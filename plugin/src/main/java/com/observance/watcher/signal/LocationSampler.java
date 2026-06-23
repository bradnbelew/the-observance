package com.observance.watcher.signal;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.util.Safety;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * Scheduled location sampler (DESIGN §2.1, §2.2). Runs its body on the MAIN thread (it reads
 * Bukkit world objects) on a slow cadence; it NEVER uses {@code PlayerMoveEvent}. Each tick it,
 * per online player:
 * <ul>
 *   <li>bumps the heatmap cell for the player's position;</li>
 *   <li>computes nearest-other-player distance (group cohesion) → dossier;</li>
 *   <li>credits solo-mining time if the player mined since the last sample AND was alone;</li>
 *   <li>adds elapsed play seconds.</li>
 * </ul>
 * Plus, once per tick, it runs the <b>Kept Light</b> custom scan over {@code kept_light} sites at
 * dusk/night: if a home zone has no light-emitting block, the owner(s) present are marked in
 * violation (honored if a light burns).
 *
 * PURE TRACKING: it mutates only in-memory signals + accumulators (thread-safe). No world effects.
 * The whole body is invoked through {@code Scheduler.runTimerSafe} (already Safety-wrapped); inner
 * per-player work is additionally guarded so one player's quirk can't abort the sweep.
 */
public final class LocationSampler {

    private final SignalTracker tracker;
    private final Supplier<SitesConfig> sitesSupplier;
    private final Safety safety;
    private final int sampleIntervalSeconds;

    // "Alone" threshold: nobody else within this many blocks ⇒ solo.
    private static final double SOLO_RADIUS = 48.0;
    private static final double SOLO_RADIUS_2 = SOLO_RADIUS * SOLO_RADIUS;

    public LocationSampler(SignalTracker tracker, Supplier<SitesConfig> sitesSupplier,
                           Safety safety, int sampleIntervalSeconds) {
        this.tracker = tracker;
        this.sitesSupplier = sitesSupplier;
        this.safety = safety;
        this.sampleIntervalSeconds = Math.max(1, sampleIntervalSeconds);
    }

    /** MAIN-thread sampler tick. Wrapped by the scheduler's Safety; inner loop guarded too. */
    public void sampleTick() {
        if (!tracker.config().enabled()) return;
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        if (online.isEmpty()) return;

        long now = System.currentTimeMillis();
        for (Player p : online) {
            safety.run("sampler.location.player", () -> samplePlayer(p, online, now));
        }

        // Kept Light scan (cheap; only at dusk/night and only over placed kept_light sites).
        safety.run("sampler.keptLight", () -> scanKeptLight(online, now));
    }

    private void samplePlayer(Player p, Collection<? extends Player> online, long now) {
        if (p == null || !p.isOnline()) return;
        Location loc = p.getLocation();
        if (loc == null || loc.getWorld() == null) return;
        World world = loc.getWorld();
        String worldName = world.getName();
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();

        // Heatmap.
        tracker.heatmap().bump(worldName, bx, bz);

        PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());

        // Group cohesion: nearest other online player in the SAME world.
        double nearest2 = Double.MAX_VALUE;
        for (Player other : online) {
            if (other == null || other == p || !other.isOnline()) continue;
            Location ol = other.getLocation();
            if (ol == null || ol.getWorld() == null || !ol.getWorld().equals(world)) continue;
            double dx = ol.getX() - loc.getX();
            double dy = ol.getY() - loc.getY();
            double dz = ol.getZ() - loc.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < nearest2) nearest2 = d2;
        }
        boolean alone = nearest2 == Double.MAX_VALUE || nearest2 > SOLO_RADIUS_2;
        double nearestDist = nearest2 == Double.MAX_VALUE ? -1.0 : Math.sqrt(nearest2);

        ps.setLocationSample(worldName, bx, by, bz, nearestDist, now);
        ps.addPlaySeconds(sampleIntervalSeconds);

        // Solo-mining: if they mined since the last sample, attribute the interval + record a
        // mining sample for the solo ratio.
        if (ps.consumeMinedSinceLastSample()) {
            ps.recordMiningSample(alone);
            if (alone) ps.addSoloMiningSeconds(sampleIntervalSeconds);
        }
    }

    /* --------------------------- Kept Light --------------------------- */

    private void scanKeptLight(Collection<? extends Player> online, long now) {
        SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
        if (sites == null) return;
        List<Site> zones = sites.placedOfType("kept_light");
        if (zones.isEmpty()) return;

        for (Site zone : zones) {
            Location center = zone.location();   // null if world not loaded / unplaced
            if (center == null || center.getWorld() == null) continue;
            World world = center.getWorld();

            // Only meaningful at dusk/night (the law is "a fire must burn at home" after dark).
            if (!isNight(world)) continue;

            // Anyone present in this zone right now? (Only judge zones the owners are at.)
            boolean someonePresent = false;
            for (Player p : online) {
                Location pl = p.getLocation();
                if (pl == null || pl.getWorld() == null) continue;
                if (zone.contains(world.getName(), pl.getX(), pl.getY(), pl.getZ())) {
                    someonePresent = true;
                    break;
                }
            }
            if (!someonePresent) continue;

            boolean lit = hasLightSource(center, Math.min(zone.radius(), 16),
                    Math.min(zone.verticalRadius(), 8));

            // Credit honored/violated to every player currently in the zone.
            for (Player p : online) {
                Location pl = p.getLocation();
                if (pl == null || pl.getWorld() == null) continue;
                if (!zone.contains(world.getName(), pl.getX(), pl.getY(), pl.getZ())) continue;
                PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());
                // Rate-naturally: this only fires once per sampler tick per present player; the
                // tally grows slowly. Honored when lit, violated when dark.
                if (lit) ps.honor(TrackerConfig.CUSTOM_KEPT_LIGHT, now);
                else ps.violate(TrackerConfig.CUSTOM_KEPT_LIGHT, now);
            }
        }
    }

    /** Is it night in this world (vanilla day-time window where light matters)? */
    private boolean isNight(World world) {
        long t = world.getTime();           // 0..24000; ~13000-23000 is night
        return t >= 13000L && t <= 23000L;
    }

    /**
     * Scan a bounded box around the zone center for any light-emitting block. Bounded to keep the
     * scan cheap (≤ ~33x17x33 worst case, but radii are clamped small). Reads loaded blocks only.
     */
    private boolean hasLightSource(Location center, int radius, int vradius) {
        World world = center.getWorld();
        if (world == null) return false;
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        int r = Math.max(1, radius);
        int vr = Math.max(1, vradius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -vr; dy <= vr; dy++) {
                    int x = cx + dx, y = cy + dy, z = cz + dz;
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getLightFromBlocks() > 7) return true;   // a real light source nearby
                }
            }
        }
        return false;
    }
}
