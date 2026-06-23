package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.Placement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * WORLD — a small structure is just *there* (a cairn, a marker stone), pasted out of sight from an
 * authored, footprint-checked block list. Phase-0 / no-FAWE: the schematic is a list of relative
 * offsets→materials in the payload. The whole footprint is validated (every cell replaceable, a
 * solid floor under the base) BEFORE any block is set — so it never carves rock or floats in air
 * (anti-jank #2, #4). Placed blocks are registered protected. Either fully placed or not at all.
 *
 * <p>Payload:
 * <pre>{@code
 * { "blocks":[ {"dx":0,"dy":0,"dz":0,"material":"COBBLESTONE"},
 *              {"dx":0,"dy":1,"dz":0,"material":"MOSSY_COBBLESTONE"} ],
 *   "require_floor": true }
 * }</pre>
 * Offsets are relative to the chosen base block (the anchor's surface spot).
 */
public final class SmallStructureBeat extends AbstractBeat {

    @Override public String name() { return "small_structure"; }
    @Override public String description() { return "A small footprint-checked structure (cairn/marker) appears out of sight."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    private record Cell(int dx, int dy, int dz, Material material) { }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (req.payload().objectList("blocks").isEmpty()) return false;
        Location base = resolveBase(ctx, req);
        return base != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();
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
}
