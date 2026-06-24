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
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Accepting — the TERMINAL group rite (red-team MF-8 / seed {@code accepting-crouch}). When EVERY
 * player present on the Accepting floor (an {@code accepting_floor} site) crouches AT ONCE — "bow as
 * one" — the plugin posts the rite's OPAQUE, wordless token to the shared oracle, recording the solve
 * and firing the climax beat. The token is NEVER typeable (it is not a phrase a player could guess or
 * read off a wiki); only THIS detector — a real synchronized group bow, witnessed by the server — can
 * produce it. That is the entire point of the opaque token (B-5): the climax cannot be spoofed at a
 * sign or in Discord; it must be PERFORMED, together. The detector posting the same token the seed
 * stores is enforced byte-for-byte by a build-time self-test (riteTokenSelfTest).
 *
 * <p>Detection (anti-cheese):
 * <ul>
 *   <li>fires only inside an {@code accepting_floor} site's radius;</li>
 *   <li>requires a quorum (≥ {@code min} players present) AND that ALL present are sneaking at once;</li>
 *   <li>rate-limited per site (one attempt per cooldown) so crouch-spam can't hammer the oracle;</li>
 *   <li>resolves ASYNC (network) — the toggling player is the nominal solver; the solve is shared.</li>
 * </ul>
 *
 * <p>Pure: never cancels the event, never mutates the world, never messages players. Body in Safety.
 * Sites resolved live via a {@link Supplier} so a config reload is picked up without re-registering.
 */
public final class AcceptingRiteListener implements Listener {

    private static final String RITE_SITE_TYPE = "accepting_floor";

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

    public AcceptingRiteListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
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
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "accepting-crouch" : puzzleKey.trim();
        this.quorum = Math.max(1, quorum);
        this.cooldownMs = Math.max(1_000L, cooldownMs);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        safety.run("rite.accepting.sneak", () -> {
            if (!enabled || token.isBlank() || oracle == null || scheduler == null) return;
            if (!event.isSneaking()) return;                 // only the moment a bow BEGINS

            Player toggler = event.getPlayer();
            if (toggler == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = toggler.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            Site floor = nearestPlacedOfType(sites, RITE_SITE_TYPE, world, loc.getX(), loc.getY(), loc.getZ());
            if (floor == null) return;                       // not on the Accepting floor

            // Everyone currently within the floor. The toggler counts as sneaking — the event IS their bow.
            List<Player> present = playersInSite(floor, world);
            if (present.size() < quorum) return;             // not the whole present group yet
            for (Player p : present) {
                boolean sneaking = p.equals(toggler) || p.isSneaking();
                if (!sneaking) return;                       // someone present is NOT bowing → not yet
            }

            // All present are bowing as one. Fire ONCE per cooldown window for this site.
            if (!rateLimiter.tryCooldown("accepting:" + floor.id(), cooldownMs)) return;

            final String mc = toggler.getUniqueId().toString();
            final String name = toggler.getName();
            safety.info("rite.accepting", name + " + " + (present.size() - 1)
                    + " bowed as one on " + floor.id() + " — posting the Accepting");
            // resolveWorld does network I/O → async. The opaque token is bound to the rite's puzzle key.
            scheduler.runAsyncSafe("rite.accepting.resolve",
                    () -> oracle.resolveWorld(mc, name, token, puzzleKey));
        });
    }

    /* ----------------------------- helpers ---------------------------- */

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
