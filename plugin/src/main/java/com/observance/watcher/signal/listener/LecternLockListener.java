package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Mara — {@code mara-lectern-lock} (design/PUZZLE-DESIGNS.md §3.1). Five lecterns stand before a
 * sealed reading-alcove; each holds one of Mara's books, and each book's open page is the combination
 * digit. Turned to the pages Mara annotated (the marker-count key {@code 1 2 4 4 6}, legible from the
 * marker row), the lecterns clear a comparator page-lock. This producer reads the whole combination —
 * the open page of every {@code mara_lectern} site, ordered by the site id's trailing index — on any
 * lectern interaction; when EVERY lectern shows its marked page, it posts the puzzle's OPAQUE token to
 * the shared oracle (a redstone lock the plugin reads, the token posted on the cleared combination).
 *
 * <p>Vanilla feedback teaches wrong attempts (a lamp per correct lectern, wired in-world) so this
 * producer stays PURE detection: it never cancels the page-turn, never mutates the world, never
 * messages. The combination is checked on a short settle-tick after the interact so the block state
 * reflects the just-turned page. A per-lock cooldown collapses rapid page-flipping into one check; the
 * solve is idempotent regardless.
 *
 * <p>Fault-isolated (Safety), sites resolved live via a {@link Supplier}. All block reads on the MAIN
 * thread; only the oracle resolve hops async.
 */
public final class LecternLockListener implements Listener {

    private static final String DEFAULT_LECTERN_TYPE = "mara_lectern";
    private static final long CHECK_COOLDOWN_MS = 1_500L;
    private static final long SETTLE_TICKS = 1L;

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    private final String token;
    private final String puzzleKey;
    private final String lecternType;
    /** Target 1-based page per lectern, ordered by the lectern site's trailing index (1..N). */
    private final int[] markedPages;

    public LecternLockListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                               RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                               boolean enabled, String token, String puzzleKey,
                               List<Integer> markedPages) {
        this(sitesSupplier, oracle, rateLimiter, scheduler, safety, enabled, token, puzzleKey,
                DEFAULT_LECTERN_TYPE, markedPages);
    }

    public LecternLockListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                               RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                               boolean enabled, String token, String puzzleKey,
                               String lecternType, List<Integer> markedPages) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "mara-lectern-lock" : puzzleKey.trim();
        this.lecternType = (lecternType == null || lecternType.isBlank())
                ? DEFAULT_LECTERN_TYPE : lecternType.trim();
        this.markedPages = toIntArray(markedPages);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled || token.isBlank()) return;
        Block b = event.getClickedBlock();
        if (b == null || !(b.getState() instanceof Lectern)) return;   // cheap: only lectern clicks

        safety.run(puzzleKey + ".lectern.interact", () -> {
            Player p = event.getPlayer();
            if (p == null || oracle == null || scheduler == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = b.getLocation();
            if (loc.getWorld() == null) return;
            String world = loc.getWorld().getName();
            // Only proceed if the clicked lectern is actually one of the lock's lecterns.
            Site here = exactLockSiteAt(sites, world, loc.getX(), loc.getY(), loc.getZ());
            if (here == null) return;

            final String mc = p.getUniqueId().toString();
            final String name = p.getName();
            // Re-read the whole combination after a settle tick (the page flip applies post-event).
            scheduler.runLaterSafe(puzzleKey + ".lectern.settle", SETTLE_TICKS, () -> safety.run(
                    puzzleKey + ".lectern.check", () -> checkCombination(mc, name)));
        });
    }

    /** MAIN-thread: read every placed lectern's open page and, if the full combination matches, post. */
    private void checkCombination(String mcUuid, String playerName) {
        SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
        if (sites == null) return;
        if (markedPages.length == 0) return;                 // nothing to match against — inert

        for (int idx = 1; idx <= markedPages.length; idx++) {
            Site s = sites.get(lecternType + "_" + idx);
            if (s == null || !s.isPlaced() || !lecternType.equals(s.type())) return;
            int targetPage = markedPages[idx - 1];
            Integer openPage = openPageOf(s);
            if (openPage == null || openPage != targetPage) return;
        }

        if (!rateLimiter.tryCooldown(lecternType + ":lock", CHECK_COOLDOWN_MS)) return;
        safety.info(puzzleKey + ".lectern", playerName + " cleared the lectern lock — posting " + puzzleKey);
        scheduler.runAsyncSafe(puzzleKey + ".lectern.resolve",
                () -> oracle.resolveWorld(mcUuid, playerName, token, puzzleKey));
    }

    /** The 1-based open page of a placed lectern site's block, or null if not a loaded lectern. */
    private Integer openPageOf(Site s) {
        Location loc = s.location();
        if (loc == null || loc.getWorld() == null) return null;
        BlockState state = loc.getBlock().getState();
        if (!(state instanceof Lectern lectern)) return null;
        // Lectern.getPage() is 0-based; the combination is expressed 1-based (a "page number").
        return lectern.getPage() + 1;
    }

    /* ----------------------------- helpers ---------------------------- */

    static int[] toIntArray(List<Integer> list) {
        if (list == null || list.isEmpty()) return new int[0];
        int[] out = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Integer v = list.get(i);
            out[i] = v == null ? -1 : v;
        }
        return out;
    }

    private Site exactLockSiteAt(SitesConfig sites, String world, double x, double y, double z) {
        for (int idx = 1; idx <= markedPages.length; idx++) {
            Site s = sites.get(lecternType + "_" + idx);
            if (s == null || !s.isPlaced() || !lecternType.equals(s.type())) continue;
            if (!s.contains(world, x, y, z)) continue;
            return s;
        }
        return null;
    }
}
