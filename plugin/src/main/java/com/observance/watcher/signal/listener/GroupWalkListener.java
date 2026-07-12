package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Mara — {@code mara-walk-the-map} (design/PUZZLE-DESIGNS.md §3.2). "Walk the rite you have only
 * read": the active roster physically travels to the {@code mara_map_marker} site and bows TOGETHER
 * (a synchronized group crouch at the marker), enacting the rite Mara only ever read. When enough of
 * the present group are bowing at once, this detector posts the puzzle's OPAQUE token to the shared
 * oracle — the same shape as {@link AcceptingRiteListener}, but a smaller convergence than the finale
 * and gated on {@code mara_alcove_open} by the seed's storylet gate (the row is only OPEN once the
 * lectern lock set that flag, so a solve before the alcove opens cannot land — the oracle's flag gate
 * silently skips the closed row).
 *
 * <p>Active-only quorum (INV-19): the configured quorum is clamped to the count of players currently
 * online — the real active set at the instant of the bow — so an absent cast member can never block
 * the walk. The bow requires that EVERY present player on the marker is bowing at once (a real "we
 * did it together", not two people while others idle).
 *
 * <p>Idempotent (oracle {@code insertSolveIfNew}), rate-limited per site, fault-isolated (Safety),
 * reveal-safe (never cancels, mutates, or messages). Sites resolved live via a {@link Supplier}.
 */
public final class GroupWalkListener implements Listener {

    private static final String MARKER_TYPE = "mara_map_marker";
    private static final String ROUTE_TYPE = "mara_route_marker";
    private static final int ROUTE_MARKERS = 4;

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    private final String token;
    private final String puzzleKey;
    private final int quorum;
    private final long cooldownMs;
    /** Next route rank each player must reach; 5 means the full four-marker walk is complete. */
    private final Map<UUID, Integer> routeProgress = new ConcurrentHashMap<>();

    public GroupWalkListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                             RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                             boolean enabled, String token, String puzzleKey,
                             int quorum, long cooldownMs) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "mara-walk-the-map" : puzzleKey.trim();
        this.quorum = Math.max(1, quorum);
        this.cooldownMs = Math.max(1_000L, cooldownMs);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled || event.getTo() == null || event.getFrom() == null) return;
        Location to = event.getTo();
        if (to.getWorld() == null) return;
        if (event.getFrom().getBlockX() == to.getBlockX()
                && event.getFrom().getBlockY() == to.getBlockY()
                && event.getFrom().getBlockZ() == to.getBlockZ()) return;
        safety.run("mara.walk.route", () -> {
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            Site marker = nearestPlacedOfType(sites, ROUTE_TYPE, to.getWorld().getName(),
                    to.getX(), to.getY(), to.getZ());
            if (marker == null) return;
            int rank = OrderedBowListener.trailingRank(marker.id());
            if (rank < 1 || rank > ROUTE_MARKERS) return;
            UUID id = event.getPlayer().getUniqueId();
            int expected = routeProgress.getOrDefault(id, 1);
            if (rank == expected) routeProgress.put(id, expected + 1);
            else if (rank == 1) routeProgress.put(id, 2);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        safety.run("mara.walk.sneak", () -> {
            if (!enabled || token.isBlank() || oracle == null || scheduler == null) return;
            if (!event.isSneaking()) return;                 // only the moment a bow BEGINS

            Player toggler = event.getPlayer();
            if (toggler == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = toggler.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            Site marker = nearestPlacedOfType(sites, MARKER_TYPE, world, loc.getX(), loc.getY(), loc.getZ());
            if (marker == null) return;                      // not at the map marker

            List<Player> present = playersInSite(marker, world);
            int effectiveQuorum = AcceptingRiteListener.clampQuorum(quorum, onlineCount());
            if (present.size() < effectiveQuorum) return;    // not the whole present (active) group yet
            for (Player p : present) {
                if (routeProgress.getOrDefault(p.getUniqueId(), 1) <= ROUTE_MARKERS) return;
                boolean sneaking = p.equals(toggler) || p.isSneaking();
                if (!sneaking) return;                       // someone present is NOT bowing → not yet
            }

            // The group walked and bowed together. Fire ONCE per cooldown window for this marker.
            if (!rateLimiter.tryCooldown("mara_walk:" + marker.id(), cooldownMs)) return;

            final String mc = toggler.getUniqueId().toString();
            final String name = toggler.getName();
            safety.info("mara.walk", name + " + " + (present.size() - 1)
                    + " walked the rite on " + marker.id() + " — posting mara-walk-the-map");
            scheduler.runAsyncSafe("mara.walk.resolve",
                    () -> oracle.resolveWorld(mc, name, token, puzzleKey));
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    private int onlineCount() {
        try {
            return Bukkit.getOnlinePlayers().size();
        } catch (Throwable t) {
            return 1;
        }
    }

    private List<Player> playersInSite(Site site, String world) {
        List<Player> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Location l = p.getLocation();
            if (l == null || l.getWorld() == null) continue;
            if (!l.getWorld().getName().equals(world)) continue;
            if (site.contains(world, l.getX(), l.getY(), l.getZ())) out.add(p);
        }
        return out;
    }

    private Site nearestPlacedOfType(SitesConfig sites, String type,
                                     String world, double x, double y, double z) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(type)) {
            if (!s.contains(world, x, y, z)) continue;
            Location c = s.location();
            if (c == null) { if (best == null) best = s; continue; }
            double dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = s; }
        }
        return best;
    }
}
