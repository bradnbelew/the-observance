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
 * WORLD — subtle decay creeps in: a cobweb in a ceiling corner, moss on stone, a vine. Places a few
 * authored decay blocks into REPLACEABLE cells (air/foliage) that have proper support, out of sight,
 * so the base slowly feels less yours. Strictly additive into empty cells — never destroys a real
 * block (decency floor). The placed blocks are registered protected so they can't be weaponized, and
 * can be cleaned up by a reverse beat.
 *
 * <p>Payload:
 * <pre>{@code { "radius":4, "count":2, "material":"COBWEB", "needs_support":true } }</pre>
 */
public final class DecayCreepBeat extends AbstractBeat {

    @Override public String name() { return "decay_creep"; }
    @Override public String description() { return "Subtle decay creeps in (cobweb / moss / vine) into empty cells, out of sight."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return false;
        if (!a.getWorld().isChunkLoaded(a.getBlockX() >> 4, a.getBlockZ() >> 4)) return false;
        Material mat = material(req.payload().string("material", "COBWEB"), Material.COBWEB);
        return !findCells(a, req.payload().integer("radius", 4),
                req.payload().bool("needs_support", true), 1).isEmpty() && mat != null;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return BeatResult.skipped("no-anchor");
        BeatPayload p = req.payload();
        Material mat = material(p.string("material", "COBWEB"), Material.COBWEB);
        if (mat == null || mat.isAir()) return BeatResult.skipped("bad-material");
        int radius = Math.max(1, Math.min(8, p.integer("radius", 4)));
        int count = Math.max(1, Math.min(8, p.integer("count", 2)));
        boolean needsSupport = p.bool("needs_support", true);

        List<Block> cells = findCells(a, radius, needsSupport, count);
        if (cells.isEmpty()) return BeatResult.skipped("no-cell");

        int placed = 0;
        for (Block cell : cells) {
            final Location cloc = cell.getLocation().clone();
            boolean scheduled = mutateWhenUnwitnessed(ctx, cell, () -> {
                Block b = cloc.getBlock();
                if (!Placement.isReplaceable(b)) return;      // re-check: still empty
                b.setType(mat, false);
                ctx.protectedRegistry().protect(b);
            });
            if (scheduled) placed++;
        }
        return placed > 0 ? BeatResult.fired("decay=" + placed) : BeatResult.skipped("none-clear");
    }

    private static List<Block> findCells(Location center, int radius, boolean needsSupport, int limit) {
        List<Block> out = new ArrayList<>();
        if (center == null || center.getWorld() == null) return out;
        Block origin = center.getBlock();
        int r = Math.max(1, Math.min(8, radius));
        for (int dy = r; dy >= -r && out.size() < limit; dy--) {   // prefer higher (ceiling corners) first
            for (int dx = -r; dx <= r && out.size() < limit; dx++) {
                for (int dz = -r; dz <= r && out.size() < limit; dz++) {
                    Block b = origin.getRelative(dx, dy, dz);
                    if (!Placement.isReplaceable(b)) continue;
                    if (needsSupport) {
                        // adjacent to at least one solid block so it doesn't float oddly
                        if (!hasAnySolidNeighbor(b)) continue;
                    }
                    out.add(b);
                }
            }
        }
        return out;
    }

    private static boolean hasAnySolidNeighbor(Block b) {
        org.bukkit.block.BlockFace[] faces = {
                org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN,
                org.bukkit.block.BlockFace.NORTH, org.bukkit.block.BlockFace.SOUTH,
                org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST
        };
        for (org.bukkit.block.BlockFace f : faces) {
            if (Placement.isSolidSupport(b.getRelative(f))) return true;
        }
        return false;
    }
}
