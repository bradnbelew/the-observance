package com.observance.watcher.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Scene-awareness for GROUP moments (red-team MF-9). The gather-events (THE COUNT, the un-lighting, the
 * Accepting) and {@code GroupBeat} need to reason about who is CONVENED — clustered near a point — so a
 * moment can target the whole gathering, and so reveal-discipline can switch to its per-player path
 * ({@link Reveal#isHiddenFrom}) when a globally-unwitnessed instant never comes because the convened
 * group is standing together.
 *
 * <p>All methods are READ-ONLY world queries → MAIN thread only. Pure (no mutation, no I/O), null-safe,
 * and world-consistent (players in another world are ignored, never mixed into a centroid).
 */
public final class SceneAwareness {

    private SceneAwareness() {}

    /** Online players in the SAME world within {@code radius} of the center. */
    public static List<Player> near(Location center, double radius) {
        List<Player> out = new ArrayList<>();
        if (center == null || center.getWorld() == null || radius <= 0) return out;
        World world = center.getWorld();
        double r2 = radius * radius;
        for (Player p : world.getPlayers()) {
            if (p == null || !p.isOnline()) continue;
            Location l = p.getLocation();
            if (l == null || l.getWorld() == null || !l.getWorld().equals(world)) continue;
            if (l.distanceSquared(center) <= r2) out.add(p);
        }
        return out;
    }

    /** True when at least {@code min} players are convened within {@code radius} of the center. */
    public static boolean convened(Location center, double radius, int min) {
        return near(center, radius).size() >= Math.max(1, min);
    }

    /** The centroid of a set of players (the gathering's center), or null if none are placeable.
     *  Anchors on the first player's world; players in another world are skipped. */
    public static Location centroid(Collection<? extends Player> players) {
        if (players == null || players.isEmpty()) return null;
        double x = 0, y = 0, z = 0;
        World world = null;
        int n = 0;
        for (Player p : players) {
            if (p == null || !p.isOnline()) continue;
            Location l = p.getLocation();
            if (l == null || l.getWorld() == null) continue;
            if (world == null) world = l.getWorld();
            else if (!world.equals(l.getWorld())) continue;   // ignore players in another world
            x += l.getX(); y += l.getY(); z += l.getZ(); n++;
        }
        return (world == null || n == 0) ? null : new Location(world, x / n, y / n, z / n);
    }

    /** True if the gathering is TIGHT — every (same-world) member within {@code spread} of the centroid.
     *  A loose spread means they aren't really "together", so a group moment may not land; the caller can
     *  wait or pick a different beat. */
    public static boolean isTight(Collection<? extends Player> players, double spread) {
        Location c = centroid(players);
        if (c == null || c.getWorld() == null) return false;
        double s2 = spread * spread;
        for (Player p : players) {
            if (p == null || !p.isOnline()) continue;
            Location l = p.getLocation();
            if (l == null || l.getWorld() == null || !l.getWorld().equals(c.getWorld())) return false;
            if (l.distanceSquared(c) > s2) return false;
        }
        return true;
    }
}
