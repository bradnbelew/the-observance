package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Orin — {@code orin-frame-dials} (design/PUZZLE-DESIGNS.md §5.3). Six item frames, each an 8-position
 * rotation dial holding an arrow-marked disc, set into a sealed offering-niche. Rotating each dial to
 * POINT AT the direction each marker faces in fall-order clears the physical combination lock. This
 * producer reads the whole rotation combination — the {@link ItemFrame#getRotation()} of the frame at
 * each {@code orin_frame_dial} site, ordered by the site id's trailing index — on any frame rotation;
 * when EVERY dial shows its target rotation (0..7), it posts the puzzle's OPAQUE token to the shared
 * oracle. Gated on {@code orin_bowed} by the seed's storylet gate (you must have walked the markers
 * first, so the row is only OPEN after {@code orin-bow-fall-order} solved).
 *
 * <p>PURE detection: never cancels the rotate, never mutates the world, never messages. Checked on a
 * short settle-tick after the interact so the frame's rotation reflects the just-applied turn. A
 * per-lock cooldown collapses rapid clicking into one check; the solve is idempotent regardless.
 *
 * <p>Fault-isolated (Safety), sites resolved live via a {@link Supplier}. Entity reads on the MAIN
 * thread; only the oracle resolve hops async.
 */
public final class FrameDialsListener implements Listener {

    private static final String DIAL_TYPE = "orin_frame_dial";
    private static final long CHECK_COOLDOWN_MS = 1_500L;
    private static final long SETTLE_TICKS = 1L;
    /** How far from a dial site's center to look for its frame entity (frames sit ON a block face). */
    private static final double FRAME_SEARCH_RADIUS = 2.0;

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    private final String token;
    private final String puzzleKey;
    /** Target rotation (0..7) per dial, ordered by the dial site's trailing index (1..N). */
    private final int[] targetRotations;

    public FrameDialsListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                              RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                              boolean enabled, String token, String puzzleKey,
                              List<Integer> dialRotations) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "orin-frame-dials" : puzzleKey.trim();
        this.targetRotations = LecternLockListener.toIntArray(dialRotations);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRotate(PlayerInteractEntityEvent event) {
        if (!enabled || token.isBlank()) return;
        if (!(event.getRightClicked() instanceof ItemFrame frame)) return;   // cheap: only frame clicks

        safety.run("orin.dials.interact", () -> {
            Player p = event.getPlayer();
            if (p == null || oracle == null || scheduler == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = frame.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();
            Site here = exactDialSiteAt(sites, world, loc.getX(), loc.getY(), loc.getZ());
            if (here == null) return;                        // not one of the dial frames

            final String mc = p.getUniqueId().toString();
            final String name = p.getName();
            scheduler.runLaterSafe("orin.dials.settle", SETTLE_TICKS, () -> safety.run(
                    "orin.dials.check", () -> checkCombination(mc, name)));
        });
    }

    /** MAIN-thread: read every dial frame's rotation; on the full target combination, post the token. */
    private void checkCombination(String mcUuid, String playerName) {
        SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
        if (sites == null) return;
        if (targetRotations.length == 0) return;             // nothing to match — inert
        for (int idx = 1; idx <= targetRotations.length; idx++) {
            Site s = sites.get("orin_frame_dial_" + idx);
            if (s == null || !s.isPlaced() || !DIAL_TYPE.equals(s.type())) return;
            Integer rot = rotationOf(s);
            if (rot == null) return;                         // a dial frame not loaded → cannot clear yet
            if (rot != normalize(targetRotations[idx - 1])) return;
        }

        if (!rateLimiter.tryCooldown("orin_dials:lock", CHECK_COOLDOWN_MS)) return;
        safety.info("orin.dials", playerName + " cleared the frame dials — posting orin-frame-dials");
        scheduler.runAsyncSafe("orin.dials.resolve",
                () -> oracle.resolveWorld(mcUuid, playerName, token, puzzleKey));
    }

    /** The 0..7 rotation of the item frame at a placed dial site, or null if none is loaded there. */
    private Integer rotationOf(Site s) {
        Location loc = s.location();
        if (loc == null || loc.getWorld() == null) return null;
        ItemFrame best = null;
        double bestD2 = Double.MAX_VALUE;
        for (ItemFrame f : loc.getWorld().getNearbyEntitiesByType(
                ItemFrame.class, loc, FRAME_SEARCH_RADIUS)) {
            Location fl = f.getLocation();
            double dx = fl.getX() - loc.getX(), dy = fl.getY() - loc.getY(), dz = fl.getZ() - loc.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = f; }
        }
        if (best == null) return null;
        return best.getRotation().ordinal();                 // Rotation enum ordinal = 0..7
    }

    private static int normalize(int rot) {
        int r = rot % 8;
        return r < 0 ? r + 8 : r;
    }

    /* ----------------------------- helpers ---------------------------- */

    private Site exactDialSiteAt(SitesConfig sites, String world, double x, double y, double z) {
        for (int idx = 1; idx <= targetRotations.length; idx++) {
            Site s = sites.get("orin_frame_dial_" + idx);
            if (s == null || !s.isPlaced() || !DIAL_TYPE.equals(s.type())) continue;
            if (!s.contains(world, x, y, z)) continue;
            return s;
        }
        return null;
    }
}
