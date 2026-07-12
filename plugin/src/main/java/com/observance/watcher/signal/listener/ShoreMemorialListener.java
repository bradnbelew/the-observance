package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.function.Supplier;

/**
 * Sella — {@code sella-shore-memorial} (design/PUZZLE-DESIGNS.md §4.3). A scatter of blocks that
 * forced-perspective-resolves — from the one worn standing-stone above the pool (the
 * {@code sella_anchor} site) — into Sella's bird-over-water glyph. The vantage is the answer: a
 * keeper who STANDS at the anchor and LOOKS DOWN (into the pool, where the glyph resolves) has "seen
 * the bird". On that detected vantage this producer posts the puzzle's OPAQUE token to the shared
 * oracle. No typing — the seeing is the solve. Gated on {@code sella_overlay_read} by the seed's
 * storylet gate (the row is only OPEN once the overlay-lake puzzle set that flag).
 *
 * <p>Detection is a heavily-guarded {@link PlayerMoveEvent}: the body early-returns unless the player
 * is standing inside an anchor site AND their pitch is at/below the configured look-down threshold
 * (pitch is +90 straight down, -90 straight up in Bukkit). A per-player cooldown collapses the stream
 * of move events at the anchor into a single measured detection, so the event handler stays cheap and
 * the oracle is never hammered. The gaze is what matters, not motion — a look-only rotation while
 * standing still still fires the move event with a changed pitch, so a stationary "look down" is
 * detected.
 *
 * <p>Idempotent (oracle {@code insertSolveIfNew}), fault-isolated (Safety), reveal-safe (never
 * cancels, mutates, or messages). Sites resolved live via a {@link Supplier}.
 */
public final class ShoreMemorialListener implements Listener {

    private static final String ANCHOR_TYPE = "sella_anchor";
    /** One detection per player per this window (collapses the move-event stream at the anchor). */
    private static final long GAZE_COOLDOWN_MS = 5_000L;

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    private final String token;
    private final String puzzleKey;
    private final float lookDownMinPitch;

    public ShoreMemorialListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                                 RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                                 boolean enabled, String token, String puzzleKey,
                                 float lookDownMinPitch) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "sella-shore-memorial" : puzzleKey.trim();
        // Clamp to a sane looking-down range (0 = horizon, 90 = straight down).
        this.lookDownMinPitch = Math.max(0f, Math.min(90f, lookDownMinPitch));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Cheapest possible early-outs BEFORE entering Safety (this event is very hot).
        if (!enabled || token.isBlank()) return;
        Location to = event.getTo();
        if (to == null) return;
        // Only act when the player is actually looking down enough to "see the bird".
        if (to.getPitch() < lookDownMinPitch) return;

        safety.run("sella.memorial.move", () -> {
            Player p = event.getPlayer();
            if (p == null || oracle == null || scheduler == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            if (to.getWorld() == null) return;
            String world = to.getWorld().getName();

            Site anchor = nearestPlacedOfType(sites, ANCHOR_TYPE, world, to.getX(), to.getY(), to.getZ());
            if (anchor == null) return;                      // not standing at the worn stone
            Site pool = sites.get("sella_pool");
            Location focal = pool == null ? null : pool.location();
            if (focal == null || !gazeHitsFocalPoint(p, focal.clone().add(0.5, 0.25, 0.5))) return;

            // Collapse the move-event stream at the anchor into one measured detection per window.
            if (!rateLimiter.tryCooldown("sella_gaze:" + p.getUniqueId() + ":" + anchor.id(),
                    GAZE_COOLDOWN_MS)) return;

            final String mc = p.getUniqueId().toString();
            final String name = p.getName();
            safety.info("sella.memorial", name + " stood at " + anchor.id()
                    + " and saw the bird — posting sella-shore-memorial");
            scheduler.runAsyncSafe("sella.memorial.resolve",
                    () -> oracle.resolveWorld(mc, name, token, puzzleKey));
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    private boolean gazeHitsFocalPoint(Player player, Location focal) {
        Location eye = player.getEyeLocation();
        if (eye.getWorld() == null || focal.getWorld() == null || !eye.getWorld().equals(focal.getWorld())) return false;
        org.bukkit.util.Vector toward = focal.toVector().subtract(eye.toVector());
        double distance = toward.length();
        if (distance < 1.0 || distance > 24.0) return false;
        if (eye.getDirection().normalize().dot(toward.normalize()) < 0.975) return false;
        org.bukkit.util.RayTraceResult hit = eye.getWorld().rayTraceBlocks(
                eye, eye.getDirection(), distance + 0.75, org.bukkit.FluidCollisionMode.NEVER, true);
        return hit == null || hit.getHitBlock() == null
                || hit.getHitBlock().getLocation().distanceSquared(focal) <= 12.0;
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
