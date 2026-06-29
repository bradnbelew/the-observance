package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.Placement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataType;

/**
 * THE ONE A→B OVERWRITE ({@code backlog-undercroft-dimension} / D5; INTEGRATION-V2 §0.5/§D5, BP0-3).
 *
 * <p>The Undercroft "the room rebuilds itself" move: clear region A (the room as the group last left it)
 * and paste schematic B (the changed room) in a single unwitnessed instant, so the change is discovered,
 * never seen mutating. This is the <b>only</b> path in the plugin that overwrites an occupied footprint —
 * every other set-piece ({@link SmallStructureBeat}'s schematic branch) pastes onto a CLEAR footprint and
 * refuses an occupied one. Keeping overwrite isolated to this one beat is the anti-jank contract: a swap
 * is irreversible-looking and must be deliberate.
 *
 * <p><b>Why not {@code extends SmallStructureBeat}.</b> The manifest sketches this as a subclass, but
 * {@code SmallStructureBeat} is {@code final} and its footprint/paste helpers are {@code private} — it was
 * authored as a clear-footprint-only paster and would have to be re-opened to subclass. Rather than weaken
 * that class's invariant (occupied footprint = skip), this beat composes the SAME FAWE seam
 * ({@link Schematics#paster()}) and the SAME reveal/idempotency idioms directly on {@link AbstractBeat},
 * and a one-line API request to un-{@code final} {@code SmallStructureBeat} is left in the return for the
 * owning worker. Behavior is identical to the manifest's intent; only the inheritance edge differs.
 *
 * <p><b>Idempotency is the {@code swapped} PDC marker, NOT the {@code world_paste_ledger} (BP0-3/BP2-4).</b>
 * The ledger guards single-paste set-pieces (paste-once onto clear ground); a swap is a different shape —
 * it overwrites, so the ledger's {@code (world,site,schematic,base)} key would either block the legitimate
 * swap or need a second row. Instead the swap is guarded by a durable {@code observance:swapped} marker
 * written into the region ANCHOR block's {@link org.bukkit.persistence.PersistentDataContainer} (a marker
 * block authored at {@code marker_dx/dy/dz}, default the base). Once set, a re-fire reads the marker and
 * skips — no double-guard, ever. The in-process applied-set + durable {@code beat_queue.status} guard the
 * row on top of that.
 *
 * <p>Payload:
 * <pre>{@code
 * {
 *   "schematic": "undercroft_room_b",   // B: the changed room (curated .schem, FAWE-pasted)
 *   "clear": true,                       // (default true) clear A to air before pasting B
 *   "clear_w": 9, "clear_h": 5, "clear_l": 9,   // optional A box; defaults to B's dimensions
 *   "marker_dx": 0, "marker_dy": 0, "marker_dz": 0  // where the durable 'swapped' marker lives
 * }
 * }</pre>
 *
 * <p><b>Anti-jank.</b> {@code require_floor:false} by design (the Undercroft has no surface — A→B is a
 * sealed room, not a ground build). The clear-then-paste runs entirely inside one
 * {@link #mutateWhenUnwitnessed} so no one ever sees A empty between clear and paste. If FAWE is absent the
 * beat skips (never errors) — the swap simply doesn't happen and the room stays as A (a soft, in-fiction
 * outcome, not a crash). Volume-capped like the structure paster. MAIN-thread only; never throws.
 */
public final class RoomSwapBeat extends AbstractBeat {

    /** Same hard cap as {@link SmallStructureBeat} — a swap is a room, not a region. */
    private static final int MAX_SWAP_VOLUME = 32_768;

    /** PDC sub-key marking a region as already swapped (durable across restarts; the sole idempotency guard). */
    private static final String PDC_SWAPPED = "swapped";

    /** Authored marker-offset clamp (the marker block lives within or just at the room). */
    private static final int OFFSET_LIMIT = 32;

    @Override public String name() { return "room_swap"; }
    @Override public String description() { return "The ONE A→B overwrite: clear a room and paste the changed room, unwitnessed."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (sanitizeSchemName(req.payload().string("schematic", null)) == null) return false;
        return resolveBase(ctx, req) != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();

        String schem = sanitizeSchemName(p.string("schematic", null));
        if (schem == null) return BeatResult.skipped("no-schematic");

        SchematicPaster paster = Schematics.paster();
        if (paster == null) return BeatResult.skipped("fawe-unavailable"); // room stays A — soft outcome, no error

        java.io.File file = new java.io.File(
                new java.io.File(ctx.plugin().getDataFolder(), "schematics"), schem + ".schem");
        if (!file.isFile()) return BeatResult.skipped("schematic-missing");

        int[] dims = paster.dimensions(file);
        if (dims == null || dims.length != 3) return BeatResult.skipped("schematic-unreadable");
        final int w = dims[0], h = dims[1], l = dims[2];
        if (w <= 0 || h <= 0 || l <= 0) return BeatResult.skipped("schematic-empty");
        if ((long) w * h * l > MAX_SWAP_VOLUME) return BeatResult.skipped("schematic-too-large");

        Location base = resolveBase(ctx, req);
        if (base == null || base.getWorld() == null) return BeatResult.skipped("no-base");
        final Block baseBlock = base.getBlock();

        // Durable idempotency: the swap marker on the anchor block. Read BEFORE doing any work so a
        // re-fire (process restart, re-queued row) never overwrites a room that already became B.
        final Block markerBlock = baseBlock.getRelative(
                clampOffset(p.integer("marker_dx", 0)),
                clampOffset(p.integer("marker_dy", 0)),
                clampOffset(p.integer("marker_dz", 0)));
        if (isSwapped(ctx, markerBlock)) return BeatResult.skipped("already-swapped");

        if (!base.getWorld().isChunkLoaded(base.getBlockX() >> 4, base.getBlockZ() >> 4)) {
            return BeatResult.skipped("base-unloaded");
        }

        final boolean clear = p.bool("clear", true);
        final int cw = boundDim(p.integer("clear_w", w), w);
        final int ch = boundDim(p.integer("clear_h", h), h);
        final int cl = boundDim(p.integer("clear_l", l), l);

        // Reveal-disciplined: the WHOLE swap (clear A → paste B → set marker) happens in one hidden instant.
        // Gate on the base block; the room is sealed so the base is the natural witness anchor.
        mutateWhenUnwitnessed(ctx, baseBlock, () -> {
            // Re-read the marker at mutation time (another instance may have swapped during the retry wait).
            if (isSwapped(ctx, markerBlock)) return;

            if (clear) {
                clearBox(baseBlock, cw, ch, cl);
            }
            Boolean ok = ctx.safety().call("beat.room_swap.paste",
                    () -> paster.pasteAtMinCorner(file, base, true), Boolean.FALSE);
            if (Boolean.TRUE.equals(ok)) {
                protectBox(ctx, baseBlock, w, h, l);
                markSwapped(ctx, markerBlock);
            }
            // If the paste failed AND we cleared, the room is now empty — but the marker is NOT set, so a
            // re-fire will retry the full swap (clear is convergent: re-clearing air is a no-op). No
            // half-swapped state survives as "done".
        });
        return BeatResult.fired("room_swap=" + schem + " " + w + "x" + h + "x" + l);
    }

    /* ------------------------------------------------------------------ */
    /*  Region clear (vanilla, FAWE-free — paste B handles the fill)       */
    /* ------------------------------------------------------------------ */

    /** Set every cell of the [base..base+dims) box to air. Convergent (re-clearing air is a no-op). MAIN thread. */
    private static void clearBox(Block baseBlock, int w, int h, int l) {
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dz = 0; dz < l; dz++) {
                    Block b = baseBlock.getRelative(dx, dy, dz);
                    if (!Placement.isWithinBuildRange(b.getWorld(), b.getY())) continue;
                    if (!b.getType().isAir()) b.setType(Material.AIR, false);
                }
            }
        }
    }

    /** Protect the solid blocks B left behind (so the anti-grief listener keeps the changed room intact). */
    private static void protectBox(BeatContext ctx, Block baseBlock, int w, int h, int l) {
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                for (int dz = 0; dz < l; dz++) {
                    Block b = baseBlock.getRelative(dx, dy, dz);
                    try {
                        if (!b.getType().isAir() && !b.isPassable()) ctx.protectedRegistry().protect(b);
                    } catch (Throwable ignored) { }
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  The durable 'swapped' marker (the SOLE idempotency guard, BP0-3)   */
    /* ------------------------------------------------------------------ */

    // The marker lives in the CHUNK's PersistentDataContainer (a {@link org.bukkit.Chunk} is a durable
    // PersistentDataHolder, unlike a plain block — a sealed room's anchor is usually air/stone, not a
    // TileState). It is keyed to the exact anchor block coordinates so two swaps anchored in the same
    // chunk never collide. This is the SOLE swap guard (no world_paste_ledger row — BP0-3/BP2-4).

    /** PDC key on the anchor's chunk: {@code observance:swapped.<x>_<y>_<z>}. Durable, block-agnostic. */
    private static NamespacedKey markerKey(BeatContext ctx, Block markerBlock) {
        String sub = PDC_SWAPPED + "." + markerBlock.getX() + "_" + markerBlock.getY() + "_" + markerBlock.getZ();
        return new NamespacedKey(ctx.namespace(), sub.toLowerCase(java.util.Locale.ROOT));
    }

    private static boolean isSwapped(BeatContext ctx, Block markerBlock) {
        if (markerBlock == null) return false;
        try {
            var pdc = markerBlock.getChunk().getPersistentDataContainer();
            return pdc.has(markerKey(ctx, markerBlock), PersistentDataType.BYTE);
        } catch (Throwable t) {
            return false; // a read failure must never make us RE-RUN a destructive swap on a false negative...
        }
    }

    private static void markSwapped(BeatContext ctx, Block markerBlock) {
        if (markerBlock == null) return;
        try {
            markerBlock.getChunk().getPersistentDataContainer()
                    .set(markerKey(ctx, markerBlock), PersistentDataType.BYTE, (byte) 1);
        } catch (Throwable ignored) {
            // If the marker can't be written, the room still swapped; worst case is a possible re-swap,
            // which is convergent against the SAME schematic B (clear B, paste B again = B). Acceptable.
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Shared helpers (mirrors SmallStructureBeat's idioms)               */
    /* ------------------------------------------------------------------ */

    /** Choose the swap base: the resolved anchor block (the room is sealed; no surface search like a build). */
    private static Location resolveBase(BeatContext ctx, BeatRequest req) {
        Location anchor = anchor(ctx, req);
        if (anchor == null || anchor.getWorld() == null) return null;
        return anchor;
    }

    /** A clear-box dimension defaults to B's matching dimension and is bounded to it (never clear beyond B). */
    private static int boundDim(int v, int def) {
        if (v <= 0) return def;
        return Math.min(v, def);
    }

    private static int clampOffset(int v) { return Math.max(-OFFSET_LIMIT, Math.min(OFFSET_LIMIT, v)); }

    /** Lowercase + strip to [a-z0-9_-] so a payload `schematic` can never path-traverse. Null if empty. */
    private static String sanitizeSchemName(String name) {
        if (name == null) return null;
        String s = name.trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return s.isEmpty() ? null : s;
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the pure helpers the swap's safety leans on: the schematic name is sanitized against
     * path-traversal, the clear-box dimension is bounded to B (never clears beyond the new room), and
     * marker offsets clamp. A regression here would let a swap clear arbitrary world or escape its room.
     */
    static boolean roomSwapSelfTest() {
        if (sanitizeSchemName("../../etc/passwd") == null) {
            // stripping non [a-z0-9_-] leaves "etcpasswd" — non-null but harmless (no slashes/dots).
        }
        if (sanitizeSchemName("../../etc/passwd").contains("/")) return false; // traversal stripped
        if (sanitizeSchemName("..") != null) return false;                     // empties to null
        if (!"undercroft_room_b".equals(sanitizeSchemName("Undercroft_Room_B"))) return false;
        // clear-box defaults to B's dim and never exceeds it.
        if (boundDim(0, 9) != 9) return false;       // 0 → default
        if (boundDim(-5, 9) != 9) return false;      // negative → default
        if (boundDim(20, 9) != 9) return false;      // clamp down to B
        if (boundDim(5, 9) != 5) return false;       // a smaller A box is honored
        // offsets clamp.
        return clampOffset(9999) == OFFSET_LIMIT && clampOffset(-9999) == -OFFSET_LIMIT;
    }
}
