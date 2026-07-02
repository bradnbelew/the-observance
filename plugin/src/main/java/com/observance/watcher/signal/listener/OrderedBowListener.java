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
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Orin — {@code orin-bow-fall-order} (design/PUZZLE-DESIGNS.md §5.1). Six markers stand at Orin's
 * threshold, each carrying a keeper's maker's-mark. A keeper must BOW (crouch) at each marker in
 * FALL-ORDER — Vaun(1), Mara(2), Sella(3), Orin(4), Brann(5), Iss(6) (canon-spine §8.1). When a
 * keeper completes the whole ordered sequence, this detector posts the puzzle's OPAQUE token to the
 * shared oracle, recording the solve exactly like {@link AcceptingRiteListener} does for the finale.
 *
 * <p>The ordered-marker identity is carried by the site id/type suffix: sites of type
 * {@code orin_marker} whose id (or type) ends in a fall-order digit {@code 1..6}. A single
 * {@code orin_marker} placement per index; the digit IS the fall-order rank. Bowing out of order is
 * self-correcting (canon-spine §8.5, exactly as the {@code UNKEPT} acrostic): a wrong step simply
 * RESETS that keeper's progress to the correct restart (bowing at marker 1 begins again), never a
 * punishment, never a world effect.
 *
 * <p>Per-player state: each keeper walks their own sequence (embodied, solo-friendly). Progress is
 * an in-memory {@code UUID → nextIndex} map; it survives nothing (a fresh login restarts the walk,
 * which is correct — the walk is the act). Anti-spam via the shared {@link RateLimiter} cooldown per
 * player+marker so holding crouch cannot advance twice.
 *
 * <p>Idempotent: the solve is guarded by the oracle's {@code insertSolveIfNew}; a re-completed walk
 * re-posts the same token and is silently deduped. Fault-isolated (Safety), reveal-safe (never
 * cancels, never mutates the world, never messages). Sites resolved live via a {@link Supplier} so a
 * reload is picked up without re-registering.
 */
public final class OrderedBowListener implements Listener {

    private static final String MARKER_TYPE = "orin_marker";
    /** The fall-order length: Vaun, Mara, Sella, Orin, Brann, Iss. */
    private static final int SEQUENCE_LENGTH = 6;
    /** One bow credited per player+marker per this window (anti crouch-spam). */
    private static final long BOW_COOLDOWN_MS = 3_000L;

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    private final String token;
    private final String puzzleKey;

    /** Per-player progress: the fall-order index (1-based) the keeper must bow at NEXT. */
    private final Map<UUID, Integer> progress = new HashMap<>();

    public OrderedBowListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                              RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                              boolean enabled, String token, String puzzleKey) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "orin-bow-fall-order" : puzzleKey.trim();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        safety.run("orin.bow.sneak", () -> {
            if (!enabled || token.isBlank() || oracle == null || scheduler == null) return;
            if (!event.isSneaking()) return;                 // only the moment a bow BEGINS

            Player p = event.getPlayer();
            if (p == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = p.getLocation();
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            // Which fall-order marker (if any) is the keeper bowing at?
            int rank = markerRankAt(sites, world, loc.getX(), loc.getY(), loc.getZ());
            if (rank <= 0) return;                           // not at an Orin marker

            // Anti-spam: one credited bow per player+marker per cooldown.
            String rlKey = "orin_bow:" + p.getUniqueId() + ":" + rank;
            if (!rateLimiter.tryCooldown(rlKey, BOW_COOLDOWN_MS)) return;

            UUID id = p.getUniqueId();
            int expected = progress.getOrDefault(id, 1);

            if (rank != expected) {
                // Self-correcting reset (canon-spine §8.5). Bowing at marker 1 restarts the walk;
                // any other wrong step drops progress back to the start (no harm, no tell).
                progress.put(id, rank == 1 ? 2 : 1);
                if (rank == 1) return;                       // credited the fresh first step
                return;
            }

            // Correct next step in fall-order.
            if (expected < SEQUENCE_LENGTH) {
                progress.put(id, expected + 1);
                return;                                      // more markers to bow at
            }

            // The whole sequence is complete (bowed 1..6 in order). Post the opaque token ONCE; the
            // oracle's idempotency guard dedupes a re-walk. Reset progress so a later re-walk re-arms.
            progress.remove(id);
            final String mc = id.toString();
            final String name = p.getName();
            safety.info("orin.bow", name + " completed the fall-order bow — posting orin-bow-fall-order");
            scheduler.runAsyncSafe("orin.bow.resolve",
                    () -> oracle.resolveWorld(mc, name, token, puzzleKey));
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    /**
     * The fall-order rank (1..6) of the {@code orin_marker} site containing the point, or 0 if the
     * point is at no such marker. The rank is the trailing digit of the site id (preferred) or type.
     * When several markers overlap (they should not), the nearest-center wins for a stable result.
     */
    private int markerRankAt(SitesConfig sites, String world, double x, double y, double z) {
        int bestRank = 0;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(MARKER_TYPE)) {
            if (!s.contains(world, x, y, z)) continue;
            int rank = trailingRank(s.id());
            if (rank <= 0) rank = trailingRank(s.type());
            if (rank < 1 || rank > SEQUENCE_LENGTH) continue;
            Location c = s.location();
            double d2 = 0.0;
            if (c != null) {
                double dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
                d2 = dx * dx + dy * dy + dz * dz;
            }
            if (d2 < bestD2) { bestD2 = d2; bestRank = rank; }
        }
        return bestRank;
    }

    /** Trailing 1..6 fall-order digit of an id like {@code orin_marker_3}, or 0 if none/out-of-range. */
    static int trailingRank(String idOrType) {
        if (idOrType == null || idOrType.isEmpty()) return 0;
        int i = idOrType.length() - 1;
        // Skip a single trailing separator run is not needed; the digit is the last char by convention.
        char last = idOrType.charAt(i);
        if (last < '0' || last > '9') return 0;
        // Read the full trailing digit run (supports _10 etc., though the sequence is only 1..6).
        int j = i;
        while (j >= 0 && idOrType.charAt(j) >= '0' && idOrType.charAt(j) <= '9') j--;
        try {
            int n = Integer.parseInt(idOrType.substring(j + 1));
            return (n >= 1 && n <= SEQUENCE_LENGTH) ? n : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /** Pins the fall-order rank parse the whole ordered walk leans on. */
    static boolean rankParseSelfTest() {
        if (trailingRank("orin_marker_1") != 1) return false;
        if (trailingRank("orin_marker_6") != 6) return false;
        if (trailingRank("orin_marker_7") != 0) return false;   // out of fall-order range
        if (trailingRank("orin_marker") != 0) return false;     // no rank digit
        if (trailingRank(null) != 0) return false;
        if (trailingRank("orin_marker_0") != 0) return false;   // 0 is not a valid rank
        return true;
    }
}
