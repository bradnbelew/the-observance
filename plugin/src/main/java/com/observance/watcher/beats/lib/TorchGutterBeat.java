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
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.List;

/**
 * WORLD — lights gutter and relight. The signature "the deep goes dark" / Kept-Light toll: douses
 * nearby torches (WALL_TORCH/TORCH → air) out of line of sight, then RELIGHTS them after a delay
 * (reversible — takes warmth, not progress; decency floor #10). Context-gated: only ever touches a
 * block that actually IS a torch (no carving rock, no floaters).
 *
 * <p>Payload:
 * <pre>{@code { "radius":6, "max_torches":3, "relight_seconds":40, "permanent":false } }</pre>
 */
public final class TorchGutterBeat extends AbstractBeat {

    @Override public String name() { return "torch_gutter"; }
    @Override public String description() { return "Nearby torches gutter out (and relight) — a reversible cold toll."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return false;
        if (!a.getWorld().isChunkLoaded(a.getBlockX() >> 4, a.getBlockZ() >> 4)) return false;
        return !findTorches(a, req.payload().integer("radius", 6), 1).isEmpty();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return BeatResult.skipped("no-anchor");
        BeatPayload p = req.payload();
        int radius = Math.max(1, Math.min(16, p.integer("radius", 6)));
        int maxTorches = Math.max(1, Math.min(12, p.integer("max_torches", 3)));
        final boolean permanent = p.bool("permanent", false);
        final long relightTicks = Math.max(20L, p.integer("relight_seconds", 40) * 20L);

        List<Block> torches = findTorches(a, radius, maxTorches);
        if (torches.isEmpty()) return BeatResult.skipped("no-torch");

        int doused = 0;
        for (Block torch : torches) {
            final BlockData original = torch.getBlockData().clone();
            final Location tloc = torch.getLocation().clone();
            boolean scheduled = mutateWhenUnwitnessed(ctx, torch, () -> {
                Block b = tloc.getBlock();
                if (!Placement.isTorch(b)) return;           // re-check at mutation time
                b.setType(Material.AIR, false);
                if (!permanent) {
                    ctx.scheduler().runLaterSafe("beat.torch.relight", relightTicks, () -> {
                        Block now = tloc.getBlock();
                        // only relight if still air (don't overwrite player rebuild)
                        if (now.getType().isAir()) {
                            relightWhenUnwitnessed(ctx, now, original);
                        }
                    });
                }
            });
            if (scheduled) doused++;
        }
        return doused > 0 ? BeatResult.fired("guttered=" + doused) : BeatResult.skipped("none-clear");
    }

    private void relightWhenUnwitnessed(BeatContext ctx, Block block, BlockData original) {
        // Relight out of sight too so it "comes back" unwitnessed.
        boolean hidden = ctx.safety().call("beat.torch.relight.check",
                () -> ctx.reveal().isHidden(block), Boolean.TRUE);
        if (Boolean.TRUE.equals(hidden)) {
            if (block.getType().isAir()) {
                ctx.safety().run("beat.torch.relight.apply", () -> block.setBlockData(original, false));
            }
        } else {
            int delay = ctx.config().revealRetryDelayTicks();
            ctx.scheduler().runLaterSafe("beat.torch.relight.retry", delay,
                    () -> relightWhenUnwitnessed(ctx, block, original));
        }
    }

    private static List<Block> findTorches(Location center, int radius, int limit) {
        List<Block> out = new ArrayList<>();
        if (center == null || center.getWorld() == null) return out;
        Block origin = center.getBlock();
        int r = Math.max(1, Math.min(16, radius));
        for (int dy = -r; dy <= r && out.size() < limit; dy++) {
            for (int dx = -r; dx <= r && out.size() < limit; dx++) {
                for (int dz = -r; dz <= r && out.size() < limit; dz++) {
                    Block b = origin.getRelative(dx, dy, dz);
                    if (Placement.isTorch(b)
                            && b.getType() != Material.REDSTONE_TORCH
                            && b.getType() != Material.REDSTONE_WALL_TORCH) {
                        out.add(b);
                    }
                }
            }
        }
        return out;
    }
}
