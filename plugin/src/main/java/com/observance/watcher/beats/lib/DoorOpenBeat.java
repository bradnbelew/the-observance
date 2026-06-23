package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Openable;

import java.util.ArrayList;
import java.util.List;

/**
 * WORLD — a door (or gate/trapdoor) opens on its own, out of sight. Finds Openable blocks near the
 * anchor and toggles them open. Context-gated (only ever touches an actual Openable), reveal-
 * disciplined, and reversible (the showrunner can queue a close, or it auto-closes if authored).
 *
 * <p>Payload:
 * <pre>{@code { "radius":4, "max_doors":1, "open":true, "auto_close_seconds":0 } }</pre>
 */
public final class DoorOpenBeat extends AbstractBeat {

    @Override public String name() { return "door_open"; }
    @Override public String description() { return "A door / gate / trapdoor opens (or closes) on its own, out of sight."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return false;
        if (!a.getWorld().isChunkLoaded(a.getBlockX() >> 4, a.getBlockZ() >> 4)) return false;
        return !findOpenables(a, req.payload().integer("radius", 4), 1).isEmpty();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Location a = anchor(ctx, req);
        if (a == null || a.getWorld() == null) return BeatResult.skipped("no-anchor");
        BeatPayload p = req.payload();
        int radius = Math.max(1, Math.min(8, p.integer("radius", 4)));
        int max = Math.max(1, Math.min(8, p.integer("max_doors", 1)));
        final boolean open = p.bool("open", true);
        final long autoCloseTicks = Math.max(0L, p.integer("auto_close_seconds", 0) * 20L);

        List<Block> doors = findOpenables(a, radius, max);
        if (doors.isEmpty()) return BeatResult.skipped("no-door");

        int toggled = 0;
        for (Block door : doors) {
            final Location dloc = door.getLocation().clone();
            boolean scheduled = mutateWhenUnwitnessed(ctx, door, () -> {
                Block b = dloc.getBlock();
                if (!(b.getBlockData() instanceof Openable o)) return;
                if (o.isOpen() == open) return; // already in desired state
                o.setOpen(open);
                b.setBlockData(o, true);  // applyPhysics true so double-doors/sounds behave
                if (autoCloseTicks > 0) {
                    ctx.scheduler().runLaterSafe("beat.door.autoclose", autoCloseTicks, () -> {
                        Block now = dloc.getBlock();
                        if (now.getBlockData() instanceof Openable o2 && o2.isOpen() == open) {
                            o2.setOpen(!open);
                            now.setBlockData(o2, true);
                        }
                    });
                }
            });
            if (scheduled) toggled++;
        }
        return toggled > 0 ? BeatResult.fired("doors=" + toggled) : BeatResult.skipped("none-clear");
    }

    private static List<Block> findOpenables(Location center, int radius, int limit) {
        List<Block> out = new ArrayList<>();
        if (center == null || center.getWorld() == null) return out;
        Block origin = center.getBlock();
        int r = Math.max(1, Math.min(8, radius));
        for (int dy = -r; dy <= r && out.size() < limit; dy++) {
            for (int dx = -r; dx <= r && out.size() < limit; dx++) {
                for (int dz = -r; dz <= r && out.size() < limit; dz++) {
                    Block b = origin.getRelative(dx, dy, dz);
                    if (b.getBlockData() instanceof Openable) out.add(b);
                }
            }
        }
        return out;
    }
}
