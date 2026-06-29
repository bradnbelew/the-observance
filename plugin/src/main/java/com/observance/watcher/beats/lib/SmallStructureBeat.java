package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.Placement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * WORLD — a small structure is just *there* (a cairn, a marker stone, a keeper-stone shrine), pasted
 * out of sight. Two paths, same anti-jank guarantees (footprint validated before ANY block is set,
 * reveal-disciplined, all-or-nothing, placed blocks protected):
 *
 * <ul>
 *   <li><b>inline</b> — an authored list of relative offsets→materials (≤256 cells) in the payload.
 *       Always available, no dependencies. Good for cairns/markers.</li>
 *   <li><b>schematic</b> — a curated {@code .schem} from {@code plugins/Observance/schematics/}, pasted
 *       via FastAsyncWorldEdit for the larger set-pieces the inline list can't hold (keeper-stones,
 *       alcoves, the Undercroft rooms). OPTIONAL: isolated behind {@link Schematics}/{@link
 *       FaweSchematicPaster}; if FAWE is absent the beat skips this path (never errors), and every
 *       other beat is unaffected.</li>
 * </ul>
 *
 * <p>Payload (one of):
 * <pre>{@code
 * { "blocks":[ {"dx":0,"dy":0,"dz":0,"material":"COBBLESTONE"} ], "require_floor": true }
 * { "schematic":"stone_01_caesar", "require_floor": true }
 * }</pre>
 * The schematic's region MIN corner lands at the resolved base; the footprint box [base..base+dims)
 * is swept for replaceability + floor support exactly like the inline path. Idempotency: once placed,
 * the footprint is occupied, so a re-fire fails the replaceable sweep and skips (plus the in-process
 * applied-set + the poller's durable status='fired').
 */
public final class SmallStructureBeat extends AbstractBeat {

    @Override public String name() { return "small_structure"; }
    @Override public String description() { return "A small footprint-checked structure (cairn/marker) appears out of sight."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    private record Cell(int dx, int dy, int dz, Material material) { }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();
        boolean hasSchematic = sanitizeSchemName(p.string("schematic", null)) != null;
        boolean hasBlocks = !p.objectList("blocks").isEmpty();
        if (!hasSchematic && !hasBlocks) return false;
        return resolveBase(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();

        // Schematic branch first (FAWE) — falls through to the inline path if no `schematic` field.
        String schem = sanitizeSchemName(p.string("schematic", null));
        if (schem != null) {
            return enactSchematic(ctx, req, p, schem);
        }

        List<Cell> cells = parseCells(p);
        if (cells.isEmpty()) return BeatResult.skipped("no-blocks");

        Location base = resolveBase(ctx, req);
        if (base == null || base.getWorld() == null) return BeatResult.skipped("no-base");
        boolean requireFloor = p.bool("require_floor", true);

        // Footprint validation FIRST — all-or-nothing.
        Block baseBlock = base.getBlock();
        for (Cell c : cells) {
            Block b = baseBlock.getRelative(c.dx, c.dy, c.dz);
            if (!Placement.isWithinBuildRange(b.getWorld(), b.getY())) return BeatResult.skipped("out-of-range");
            if (!Placement.isReplaceable(b)) return BeatResult.skipped("footprint-occupied");
        }
        if (requireFloor) {
            // every lowest-layer cell must have solid support beneath
            int minDy = cells.stream().mapToInt(Cell::dy).min().orElse(0);
            for (Cell c : cells) {
                if (c.dy != minDy) continue;
                Block under = baseBlock.getRelative(c.dx, c.dy - 1, c.dz);
                if (!Placement.isSolidSupport(under)) return BeatResult.skipped("no-floor");
            }
        }

        // Reveal-disciplined: place the whole thing only when the BASE is hidden.
        mutateWhenUnwitnessed(ctx, baseBlock, () -> {
            // Re-validate footprint at mutation time (world may have changed).
            for (Cell c : cells) {
                Block b = baseBlock.getRelative(c.dx, c.dy, c.dz);
                if (!Placement.isReplaceable(b)) return; // abort silently, leave nothing half-built
            }
            for (Cell c : cells) {
                Block b = baseBlock.getRelative(c.dx, c.dy, c.dz);
                if (c.material == null || c.material.isAir()) continue;
                b.setType(c.material, false);
                ctx.protectedRegistry().protect(b);
            }
        });
        return BeatResult.fired("structure=" + cells.size());
    }

    private static List<Cell> parseCells(BeatPayload p) {
        List<Cell> out = new ArrayList<>();
        for (BeatPayload bp : p.objectList("blocks")) {
            Material m = matOrNull(bp.string("material", null));
            if (m == null) continue;
            int dx = clamp(bp.integer("dx", 0));
            int dy = clamp(bp.integer("dy", 0));
            int dz = clamp(bp.integer("dz", 0));
            out.add(new Cell(dx, dy, dz, m));
            if (out.size() >= 256) break;   // bound structure size (anti-lag, anti-grief)
        }
        return out;
    }

    private static Material matOrNull(String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return Material.matchMaterial(name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Throwable t) { return null; }
    }

    /** Choose the base block: a valid surface spot under/at the anchor. MAIN thread. */
    private static Location resolveBase(BeatContext ctx, BeatRequest req) {
        Location anchor = anchor(ctx, req);
        if (anchor == null || anchor.getWorld() == null) return null;
        if (!anchor.getWorld().isChunkLoaded(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) return null;
        // Find an air cell on solid floor near the anchor (search downward).
        Block spot = Placement.findSurfaceSpot(anchor.clone().add(0, 2, 0), 8);
        return spot == null ? null : spot.getLocation();
    }

    private static int clamp(int v) { return Math.max(-32, Math.min(32, v)); }

    /* ------------------------------------------------------------------ */
    /* Schematic (FAWE) path                                              */
    /* ------------------------------------------------------------------ */

    /**
     * Bound the schematic bounding-box volume. Caps the main-thread cost of the footprint sweep + the
     * protect pass + the paste itself, and keeps a runaway/oversized schematic from lagging a tick.
     * Curated keeper-stones/alcoves/rooms fit comfortably; truly large set-pieces (whole dimensions)
     * are pre-generated at deploy, not runtime-pasted.
     */
    private static final int MAX_SCHEM_VOLUME = 32_768;

    private BeatResult enactSchematic(BeatContext ctx, BeatRequest req, BeatPayload p, String schem) {
        SchematicPaster paster = Schematics.paster();
        if (paster == null) return BeatResult.skipped("fawe-unavailable");

        java.io.File file = new java.io.File(
                new java.io.File(ctx.plugin().getDataFolder(), "schematics"), schem + ".schem");
        if (!file.isFile()) return BeatResult.skipped("schematic-missing");

        int[] dims = paster.dimensions(file);
        if (dims == null || dims.length != 3) return BeatResult.skipped("schematic-unreadable");
        final int w = dims[0], h = dims[1], l = dims[2];
        if (w <= 0 || h <= 0 || l <= 0) return BeatResult.skipped("schematic-empty");
        if ((long) w * h * l > MAX_SCHEM_VOLUME) return BeatResult.skipped("schematic-too-large");

        Location base = resolveBase(ctx, req);
        if (base == null || base.getWorld() == null) return BeatResult.skipped("no-base");
        final boolean requireFloor = p.bool("require_floor", true);
        final Block baseBlock = base.getBlock();

        // Footprint pre-check over the schematic's [base .. base+dims) box — never carve/float. MAIN thread.
        if (!footprintClear(baseBlock, w, h, l, requireFloor)) {
            return BeatResult.skipped("footprint-occupied");
        }

        // The actual paste + protect, reveal-disciplined, re-checking the footprint at mutation time.
        // Factored so both the ledger-guarded and the no-ledger paths schedule the same main-thread work.
        final Runnable doPaste = () -> mutateWhenUnwitnessed(ctx, baseBlock, () -> {
            if (!footprintClear(baseBlock, w, h, l, requireFloor)) return; // world changed — abort silently
            Boolean ok = ctx.safety().call("beat.structure.paste",
                    () -> paster.pasteAtMinCorner(file, base, true), Boolean.FALSE);
            if (Boolean.TRUE.equals(ok)) {
                protectBox(ctx, baseBlock, w, h, l);
            }
        });

        // Durable single-paste idempotency (backlog D10): claim the (world,site,schematic,base) tuple in
        // world_paste_ledger BEFORE pasting, so a set-piece never re-pastes across a restart even if the
        // footprint sweep is somehow fooled. The claim is blocking network I/O → it must run OFF the main
        // thread; on a fresh claim (NEW) we hop the world write back onto main inside mutateWhenUnwitnessed.
        // A DUPLICATE means it was already pasted here (skip). A FAILED means we couldn't durably guard —
        // skip rather than risk a double set-piece (the in-process applied-set already covers the same-run
        // re-fire; this is purely the cross-restart backstop). The ledger governs SINGLE-PASTE only — the
        // A→B swap is RoomSwapBeat's `swapped` PDC marker's job, never this table (no double-guard, BP0-3).
        final SupabaseClient supabase = ctx.supabase();
        final String world = base.getWorld().getName();
        final String siteId = req.hasSite() ? req.site().id() : "";
        final int bx = baseBlock.getX(), by = baseBlock.getY(), bz = baseBlock.getZ();
        if (supabase == null) {
            // No durable store wired (test/standalone) → footprint sweep is the sole guard, paste directly.
            doPaste.run();
            return BeatResult.fired("schematic=" + schem + " " + w + "x" + h + "x" + l);
        }
        ctx.scheduler().asyncThenMain("beat.structure.ledger",
                () -> supabase.claimPasteLedger(world, siteId, schem, bx, by, bz),
                claim -> {
                    if (claim == SupabaseClient.PasteClaim.NEW) {
                        doPaste.run();
                    }
                    // DUPLICATE / FAILED → do nothing (already pasted, or couldn't guard → never double-paste).
                });
        // We initiated the guarded paste (it resolves async). Report fired so the poller marks the row done;
        // a genuine DUPLICATE/FAILED simply leaves the world untouched (idempotent, no half-state).
        return BeatResult.fired("schematic=" + schem + " " + w + "x" + h + "x" + l + " (ledger-guarded)");
    }

    /** Every cell of the [base..base+dims) box must be in-range + replaceable; floor optional. MAIN thread. */
    private static boolean footprintClear(Block baseBlock, int w, int h, int l, boolean requireFloor) {
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dz = 0; dz < l; dz++) {
                    Block b = baseBlock.getRelative(dx, dy, dz);
                    if (!Placement.isWithinBuildRange(b.getWorld(), b.getY())) return false;
                    if (!Placement.isReplaceable(b)) return false;
                }
            }
        }
        if (requireFloor) {
            for (int dx = 0; dx < w; dx++) {
                for (int dz = 0; dz < l; dz++) {
                    if (!Placement.isSolidSupport(baseBlock.getRelative(dx, -1, dz))) return false;
                }
            }
        }
        return true;
    }

    /** Protect the solid blocks the paste left in the box (the structure), for the anti-grief listener. */
    private static void protectBox(BeatContext ctx, Block baseBlock, int w, int h, int l) {
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dz = 0; dz < l; dz++) {
                    Block b = baseBlock.getRelative(dx, dy, dz);
                    if (!b.getType().isAir() && !b.isPassable()) ctx.protectedRegistry().protect(b);
                }
            }
        }
    }

    /** Lowercase + strip to [a-z0-9_-] so a payload `schematic` can never path-traverse. Null if empty. */
    private static String sanitizeSchemName(String name) {
        if (name == null) return null;
        String s = name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return s.isEmpty() ? null : s;
    }
}
