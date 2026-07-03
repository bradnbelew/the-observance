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
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;

import java.util.ArrayList;
import java.util.List;

/**
 * WORLD — the drift: the world AGES on its own between visits. Sculk creeps up the walls of a place
 * you left, the deep leaks a little higher, and a corner you cleared is a shade more lost when you
 * come back. This is the enactment half of the WORLD-DRIFT CLOCK: a strictly-additive dusting of
 * "aging" placed near an already-found site, out of sight, so returning feels like time passed.
 *
 * <p>Like {@link DecayCreepBeat} it never carves rock and never removes anything (the decency floor):
 * it only writes into REPLACEABLE cells (air / foliage) that already have proper support, and every
 * block it lays is registered PROTECTED so it can't be weaponized. It is HARD-CAPPED per enactment;
 * a long absence asks for a bit more creep (the clock scales {@code count}), but the beat itself
 * clamps so no gap can ever carpet an area. Reveal-safe: the {@code WorldDriftClock} only ever anchors
 * this near sites the players have ALREADY placed/found, so drift never exposes an un-found site — it
 * ages the EDGES of places you know, never points at one you don't.
 *
 * <p>What it places (all additive, into empty cells only):
 * <ul>
 *   <li>SCULK_VEIN — the signature creep, oriented onto the solid face it grows against (a
 *       {@link MultipleFacing} block, so it clings to a wall/ceiling like real veins rather than
 *       floating), and</li>
 *   <li>optionally a little floor moss/sculk on a stand-on cell (payload {@code floor_material}),
 *       the "deep leaking upward".</li>
 * </ul>
 *
 * <p>Payload:
 * <pre>{@code { "radius":4, "count":2, "vein_material":"SCULK_VEIN", "floor_material":"MOSS_CARPET" } }</pre>
 */
public final class WorldDriftBeat extends AbstractBeat {

    /** Absolute per-enactment ceiling — the clock may ask for more after a long absence, but a single
     *  drift can NEVER exceed this, so no gap of any length can carpet a place (cumulative sanity). */
    private static final int MAX_PER_ENACTMENT = 6;

    @Override public String name() { return "world_drift"; }
    @Override public String description() { return "The world ages on its own — sculk creeps into empty cells near a found site, out of sight."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return false;
        if (!a.getWorld().isChunkLoaded(a.getBlockX() >> 4, a.getBlockZ() >> 4)) return false;
        Material vein = material(req.payload().string("vein_material", "SCULK_VEIN"), Material.SCULK_VEIN);
        return vein != null && !findVeinCells(a, req.payload().integer("radius", 4), 1).isEmpty();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return BeatResult.skipped("no-anchor");
        BeatPayload p = req.payload();
        Material vein = material(p.string("vein_material", "SCULK_VEIN"), Material.SCULK_VEIN);
        if (vein == null || vein.isAir()) return BeatResult.skipped("bad-material");
        Material floor = material(p.string("floor_material", "MOSS_CARPET"), Material.MOSS_CARPET);
        int radius = Math.max(1, Math.min(8, p.integer("radius", 4)));
        // The clock scales count by elapsed time; clamp HARD here regardless of what it asks (decency).
        int count = Math.max(1, Math.min(MAX_PER_ENACTMENT, p.integer("count", 2)));

        // Reserve most of the budget for creeping veins; let at most one be a floor dusting so the
        // "deep leaking upward" stays a garnish, never a carpet.
        int floorBudget = (floor != null && !floor.isAir()) ? 1 : 0;
        List<Block> veinCells = findVeinCells(a, radius, count);
        if (veinCells.isEmpty()) return BeatResult.skipped("no-cell");

        int placed = 0;
        int floorPlaced = 0;
        for (Block cell : veinCells) {
            // Decide the aging form for THIS cell: prefer a wall/ceiling vein; only if the cell can
            // also stand a floor block and we still have floor budget, occasionally leak moss up instead.
            final BlockFace veinFace = firstSolidFace(cell);        // guaranteed non-null (finder ensured it)
            final boolean asFloor = floorBudget > 0 && floorPlaced < floorBudget
                    && Placement.canStandOn(cell);
            final Material mat = asFloor ? floor : vein;
            final Location cloc = cell.getLocation().clone();
            boolean scheduled = mutateWhenUnwitnessed(ctx, cell, () -> {
                Block b = cloc.getBlock();
                if (!Placement.isReplaceable(b)) return;            // re-check: still empty
                if (mat == vein) {
                    if (veinFace == null) return;                   // support vanished — skip, never float
                    b.setType(vein, false);
                    orientVein(b, veinFace);
                }
                else {
                    if (!Placement.canStandOn(b)) return;           // floor vanished — skip
                    b.setType(mat, false);
                }
                ctx.protectedRegistry().protect(b);
            });
            if (scheduled) {
                placed++;
                if (asFloor) floorPlaced++;
            }
        }
        return placed > 0 ? BeatResult.fired("drift=" + placed) : BeatResult.skipped("none-clear");
    }

    /**
     * Cells that can hold a wall/ceiling vein: replaceable AND adjacent to at least one solid face for
     * the vein to cling to. Prefers HIGHER cells first (creep climbs walls / hangs from ceilings), like
     * {@link DecayCreepBeat}'s ceiling-corner bias.
     */
    private static List<Block> findVeinCells(Location center, int radius, int limit) {
        List<Block> out = new ArrayList<>();
        if (center == null || center.getWorld() == null) return out;
        Block origin = center.getBlock();
        int r = Math.max(1, Math.min(8, radius));
        for (int dy = r; dy >= -r && out.size() < limit; dy--) {   // prefer higher (walls/ceilings) first
            for (int dx = -r; dx <= r && out.size() < limit; dx++) {
                for (int dz = -r; dz <= r && out.size() < limit; dz++) {
                    Block b = origin.getRelative(dx, dy, dz);
                    if (!Placement.isReplaceable(b)) continue;
                    if (firstSolidFace(b) == null) continue;       // nothing to cling to — skip
                    out.add(b);
                }
            }
        }
        return out;
    }

    /** The first face of {@code b} that abuts a solid support block (where a vein can grow), or null. */
    private static BlockFace firstSolidFace(Block b) {
        BlockFace[] faces = {
                BlockFace.DOWN, BlockFace.UP,
                BlockFace.NORTH, BlockFace.SOUTH,
                BlockFace.EAST, BlockFace.WEST
        };
        for (BlockFace f : faces) {
            if (Placement.isSolidSupport(b.getRelative(f))) return f;
        }
        return null;
    }

    /**
     * Point a placed sculk vein at the solid face it grows against — a {@link MultipleFacing} block, so
     * the vein clings to that wall/ceiling like real creep rather than sitting as a full cube. Vein
     * facing X means "attached to the block on face X", so we enable exactly the supported face. Null-safe.
     */
    private static void orientVein(Block b, BlockFace face) {
        if (b == null || face == null) return;
        BlockData data = b.getBlockData();
        if (data instanceof MultipleFacing mf && mf.getAllowedFaces().contains(face)) {
            for (BlockFace f : mf.getAllowedFaces()) mf.setFace(f, false);
            mf.setFace(face, true);
            b.setBlockData(mf, false);
        }
    }
}
