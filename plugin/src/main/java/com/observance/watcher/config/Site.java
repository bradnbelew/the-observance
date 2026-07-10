package com.observance.watcher.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * One named world placement from {@code sites.yml}. Immutable.
 *
 * <p>A site is "placed" only when it has a non-null x/y/z and is enabled. Until the world seed
 * is chosen the coords are placeholders (null) → {@link #isPlaced()} is false and the engine
 * silently skips it. {@link #location()} returns null if the world isn't loaded or unplaced, so
 * callers must null-check (never NPE over an unplaced/late-loaded world).
 */
public final class Site {

    private final String id;
    private final String type;
    private final String worldName;
    private final Double x;     // nullable = placeholder
    private final Double y;
    private final Double z;
    private final int radius;
    private final int verticalRadius;
    private final boolean protect;
    private final boolean enabled;
    private final String puzzleKey;   // nullable; only meaningful for answer-sign sites
    private final boolean beacon;     // Legacy parsed flag; active beacon placement is retired.

    public Site(String id, String type, String worldName,
                Double x, Double y, Double z,
                int radius, int verticalRadius, boolean protect, boolean enabled) {
        this(id, type, worldName, x, y, z, radius, verticalRadius, protect, enabled, null, false);
    }

    public Site(String id, String type, String worldName,
                Double x, Double y, Double z,
                int radius, int verticalRadius, boolean protect, boolean enabled,
                String puzzleKey) {
        this(id, type, worldName, x, y, z, radius, verticalRadius, protect, enabled, puzzleKey, false);
    }

    public Site(String id, String type, String worldName,
                Double x, Double y, Double z,
                int radius, int verticalRadius, boolean protect, boolean enabled,
                String puzzleKey, boolean beacon) {
        this.id = id;
        this.type = type == null ? "unknown" : type;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = Math.max(0, radius);
        this.verticalRadius = Math.max(0, verticalRadius);
        this.protect = protect;
        this.enabled = enabled;
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? null : puzzleKey.trim();
        this.beacon = beacon;
    }

    public String id() { return id; }
    public String type() { return type; }
    public String worldName() { return worldName; }
    public int radius() { return radius; }
    public int verticalRadius() { return verticalRadius; }
    public boolean protect() { return protect; }
    public boolean enabled() { return enabled; }

    /**
     * Optional puzzle binding for an answer-sign site: when set, the answer-sign at this site ONLY
     * resolves against that one {@code puzzle_key} (a focused gate). When null (the default for the
     * non-linear web), the sign resolves against ALL open puzzles. Nullable.
     */
    public String puzzleKey() { return puzzleKey; }

    /** Legacy parsed beacon flag. Kept for old config compatibility; placement no longer uses it. */
    public boolean beacon() { return beacon; }

    /** True if this site has real coords and is enabled — i.e. it can participate in beats. */
    public boolean isPlaced() {
        return enabled && x != null && y != null && z != null
                && worldName != null && !worldName.isBlank();
    }

    /**
     * Resolve to a Bukkit {@link Location}, or null if unplaced or the world isn't loaded.
     * MAIN thread only (touches Bukkit). Caller must null-check.
     */
    public Location location() {
        if (!isPlaced()) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z);
    }

    /**
     * Off-thread-safe proximity test against a snapshot of coordinates. Does NOT touch Bukkit,
     * so it can be called from async code if you already have a world name + coords.
     * Returns false unless the world matches and the point is within radius (horizontal) and
     * verticalRadius (vertical).
     */
    public boolean contains(String pointWorld, double px, double py, double pz) {
        if (!isPlaced()) return false;
        if (pointWorld == null || !pointWorld.equals(worldName)) return false;
        double dx = px - x;
        double dz = pz - z;
        double dy = Math.abs(py - y);
        if (dy > verticalRadius) return false;
        double horiz2 = dx * dx + dz * dz;
        return horiz2 <= (double) radius * radius;
    }

    @Override
    public String toString() {
        return "Site{" + id + " type=" + type + " world=" + worldName
                + " placed=" + isPlaced() + " r=" + radius + "}";
    }
}
