package com.observance.watcher.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * Reveal discipline (anti-jank #3): mutate a block ONLY when no player can see it.
 *
 * <p>"Discovered, never witnessed appearing." All methods here are READ-ONLY world queries
 * and therefore MUST be called on the MAIN thread (they touch Bukkit world objects). They do
 * not mutate anything — the caller schedules the actual mutation on the main thread once a
 * target is clear.
 *
 * <p>A target is considered visible to a player if the player is within {@code witnessRadius}
 * blocks AND has line of sight to the target block's center.
 */
public final class Reveal {

    private final int witnessRadius;

    public Reveal(int witnessRadius) {
        this.witnessRadius = Math.max(1, witnessRadius);
    }

    /** True if NO online player in the world can currently witness this block. */
    public boolean isHidden(Block block) {
        if (block == null) return false;
        return isHidden(centerOf(block));
    }

    /** True if NO online player can currently witness this location. */
    public boolean isHidden(Location target) {
        if (target == null || target.getWorld() == null) return false;
        World world = target.getWorld();
        Collection<? extends Player> players = world.getPlayers();
        double r2 = (double) witnessRadius * witnessRadius;
        for (Player p : players) {
            if (p == null || !p.isOnline()) continue;
            if (!world.equals(p.getWorld())) continue;
            Location eye = p.getEyeLocation();
            if (eye.distanceSquared(target) > r2) continue; // too far to count as witness
            if (hasLineOfSight(p, target)) {
                return false; // someone can see it
            }
        }
        return true;
    }

    /** True if ANY online player can currently witness this block. Convenience inverse. */
    public boolean isWitnessed(Block block) {
        return !isHidden(block);
    }

    /**
     * True if THIS specific player cannot currently witness the target — the PER-PLAYER half of the
     * two-path reveal (MF-9). When the group is convened, a globally-hidden instant (isHidden) may
     * never come, so a world beat near them would retry and die silently. The per-player path instead
     * delivers a CLIENT-SIDE illusion (via {@code player.sendBlockChange}) to each member at the moment
     * THEY aren't looking, so each "discovers" it without seeing it appear. A player in another world,
     * out of witness range, or without line of sight counts as not-witnessing (hidden = true).
     */
    public boolean isHiddenFrom(Player player, Location target) {
        if (player == null || !player.isOnline() || target == null || target.getWorld() == null) return false;
        if (!target.getWorld().equals(player.getWorld())) return true;     // different world → can't see it
        Location eye = player.getEyeLocation();
        double r2 = (double) witnessRadius * witnessRadius;
        if (eye.distanceSquared(target) > r2) return true;                 // too far to witness
        return !hasLineOfSight(player, target);                            // in range → hidden iff no LoS
    }

    public boolean isHiddenFrom(Player player, Block block) {
        return block != null && isHiddenFrom(player, centerOf(block));
    }

    /**
     * Line-of-sight from a player's eyes to a target location, via Bukkit's ray-trace.
     * Conservative: if the ray hits a solid block before reaching the target, it's blocked.
     */
    public boolean hasLineOfSight(Player player, Location target) {
        if (player == null || target == null) return false;
        Location eye = player.getEyeLocation();
        World world = eye.getWorld();
        if (world == null || !world.equals(target.getWorld())) return false;

        Vector dir = target.toVector().subtract(eye.toVector());
        double distance = dir.length();
        if (distance < 1.0e-4) return true; // essentially same point
        dir.normalize();

        // FOV gate: only count as "seen" if roughly in front of the player. The watcher_radius
        // already bounds distance; this trims the case where the target is behind the player.
        Vector facing = eye.getDirection();
        double dot = facing.dot(dir);
        if (dot < -0.2) {
            // Target is clearly behind the player — treat as not-seen even if technically LoS.
            return false;
        }

        // Ray-trace blocks only (ignore fluids/passables); stop a touch short of the target
        // so the target block itself doesn't count as the occluder.
        double rayLen = Math.max(0.0, distance - 0.5);
        var hit = world.rayTraceBlocks(eye, dir, rayLen,
                org.bukkit.FluidCollisionMode.NEVER, true);
        // No solid block intercepted the ray before the target → line of sight is clear.
        return hit == null;
    }

    /** Center point of a block (for ray targets). */
    public static Location centerOf(Block block) {
        return block.getLocation().add(0.5, 0.5, 0.5);
    }

    public int witnessRadius() {
        return witnessRadius;
    }
}
