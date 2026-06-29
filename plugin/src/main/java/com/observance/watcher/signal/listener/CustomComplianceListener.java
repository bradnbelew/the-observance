package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.signal.PlayerSignals;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.function.Supplier;

/**
 * Detects compliance with the two PROXIMITY-anchored Phase-0 customs, keyed to {@code sites.yml}:
 * <ul>
 *   <li><b>The Bow</b> — crouch ({@link PlayerToggleSneakEvent}) within a {@code bow_marker}
 *       site's radius = HONORED. Rate-limited per player so holding/spamming crouch can't farm it.</li>
 *   <li><b>The Offering</b> — drop ({@link PlayerDropItemEvent}) an item within an
 *       {@code offering_cairn} site's radius, after the session's first ore = HONORED.</li>
 * </ul>
 *
 * PURE TRACKING + ANTI-EXPLOIT: this listener records compliance only; it produces NO world
 * effects and never cancels the event. A per-player cooldown (the shared {@link RateLimiter})
 * stops crouch/drop spam from inflating tallies — a real anti-weaponization guard. All Bukkit
 * reads on the MAIN thread; persistence deferred to the async flush. Bodies wrapped in Safety.
 *
 * <p>Sites are resolved live via a {@link Supplier} so a config reload (which rebuilds SitesConfig)
 * is picked up without re-registering the listener.
 *
 * <p><b>The closed-set boundary (INV-17, WEB-MASTER §3.2/§3.3 — the two "eighths"):</b> this file is a
 * PER-PLAYER, per-conduct compliance detector for the enforced customs. It must NEVER be extended to
 * track either of the arc's two "eighths", because neither is a per-player proximity custom:
 * <ul>
 *   <li>the <b>forged eighth law — the Covering</b> ({@code some-laws-are-lies}, FACT 7b) is a
 *       <i>document, not an enforcement</i>. Its whole proof-of-lie is that the land never measures it.
 *       It adds NO {@code CUSTOM_KEYS} member and NO listener. If a future hand wires "the Covering" as a
 *       tracked custom here, the falsification ("obey it, observe nothing") is destroyed — that is a
 *       canon defect, not a feature. There is deliberately no code for it.</li>
 *   <li>the <b>collective-restraint latch — the Unlit Deep</b> ({@code the_unlit_deep}, the one group
 *       latch INV-17 permits) is GROUP-scoped and negative (a thing kept by not-doing below the Line on
 *       the black moon), detected on explicit flame acts by its own {@code UnlitDeepListener} — never as a
 *       per-player honored/violated tally here. Its reward is borrowed warmth, not a compliance count.</li>
 * </ul>
 * So the engine enforces exactly the seven {@code CUSTOM_KEYS} (two of them here) plus the one group
 * latch (elsewhere); any law outside that closed set is fiction, by construction.
 */
public final class CustomComplianceListener implements Listener {

    private static final String TYPE_BOW_MARKER = "bow_marker";
    private static final String TYPE_OFFERING_CAIRN = "offering_cairn";

    // Anti-spam cooldowns (ms). A bow honored at most every 10s; an offering at most every 30s.
    private static final long BOW_COOLDOWN_MS = 10_000L;
    private static final long OFFERING_COOLDOWN_MS = 30_000L;

    private final SignalTracker tracker;
    private final Supplier<SitesConfig> sitesSupplier;
    private final RateLimiter rateLimiter;
    private final Safety safety;

    public CustomComplianceListener(SignalTracker tracker, Supplier<SitesConfig> sitesSupplier,
                                    RateLimiter rateLimiter, Safety safety) {
        this.tracker = tracker;
        this.sitesSupplier = sitesSupplier;
        this.rateLimiter = rateLimiter;
        this.safety = safety;
    }

    /* ----------------------------- The Bow ---------------------------- */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        safety.run("signal.Bow.sneak", () -> {
            if (!event.isSneaking()) return;       // only the START of a crouch
            Player p = event.getPlayer();
            if (p == null) return;
            if (!tracker.config().enabled()) return;

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = p.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            Site marker = nearestPlacedOfType(sites, TYPE_BOW_MARKER,
                    world, loc.getX(), loc.getY(), loc.getZ());
            if (marker == null) return;            // not at a bow marker — nothing to record

            // Anti-spam: only credit one bow per cooldown window per player+marker.
            String key = "bow:" + p.getUniqueId() + ":" + marker.id();
            if (!rateLimiter.tryCooldown(key, BOW_COOLDOWN_MS)) return;

            PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());
            ps.honor(TrackerConfig.CUSTOM_BOW, System.currentTimeMillis());
        });
    }

    /* --------------------------- The Offering ------------------------- */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        safety.run("signal.Offering.drop", () -> {
            Player p = event.getPlayer();
            if (p == null) return;
            if (!tracker.config().enabled()) return;

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Item dropped = event.getItemDrop();
            Location loc = dropped != null ? dropped.getLocation() : p.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            Site cairn = nearestPlacedOfType(sites, TYPE_OFFERING_CAIRN,
                    world, loc.getX(), loc.getY(), loc.getZ());
            if (cairn == null) return;             // not at a cairn

            String key = "offering:" + p.getUniqueId() + ":" + cairn.id();
            if (!rateLimiter.tryCooldown(key, OFFERING_COOLDOWN_MS)) return;

            PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());
            ps.honor(TrackerConfig.CUSTOM_OFFERING, System.currentTimeMillis());
            ps.markOfferingHonoredThisSession();
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    /**
     * Nearest placed site of {@code type} whose proximity radius contains the point, or null.
     * Uses {@link Site#contains} (snapshot coords, no Bukkit) so it is cheap and exact.
     */
    private Site nearestPlacedOfType(SitesConfig sites, String type,
                                     String world, double x, double y, double z) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(type)) {
            if (!s.contains(world, x, y, z)) continue;
            // Among containing sites, pick the closest center (deterministic tie-break by id order).
            Location center = s.location();
            if (center == null) { if (best == null) best = s; continue; }
            double dx = x - center.getX(), dy = y - center.getY(), dz = z - center.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = s; }
        }
        return best;
    }
}
