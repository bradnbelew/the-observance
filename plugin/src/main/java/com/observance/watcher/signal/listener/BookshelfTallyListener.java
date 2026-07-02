package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.function.Supplier;

/**
 * Vaun — {@code vaun-bookshelf-tally} (design/PUZZLE-DESIGNS.md §2.2). A chiseled bookshelf of 6
 * slots set into the wall of Vaun's cache. Filling the slots that reproduce Vaun's "all taken, none
 * given" tally clears the register. This producer reads the {@code vaun_bookshelf} site's 6-slot
 * fill-pattern on any interaction with the shelf; when exactly the configured slots are FILLED (and
 * the others empty), it posts the puzzle's OPAQUE token to the shared oracle (a comparator lock the
 * plugin reads, the token posted on the cleared pattern). Gated on {@code vaun_cache_open} by the
 * seed's storylet gate.
 *
 * <p>PURE detection: never cancels the place/take, never mutates the world, never messages. Checked on
 * a short settle-tick after the interact so the slot state reflects the just-changed shelf. A per-lock
 * cooldown collapses rapid book shuffling into one check; the solve is idempotent regardless.
 *
 * <p>Fault-isolated (Safety), sites resolved live via a {@link Supplier}. Block reads on the MAIN
 * thread; only the oracle resolve hops async.
 */
public final class BookshelfTallyListener implements Listener {

    private static final String SHELF_TYPE = "vaun_bookshelf";
    private static final int SLOTS = 6;                       // chiseled bookshelf = 6 slots
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
    /** Which 1-based slots must be FILLED (all others must be empty). */
    private final boolean[] targetFilled;

    public BookshelfTallyListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                                  RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                                  boolean enabled, String token, String puzzleKey,
                                  List<Integer> filledSlots) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "vaun-bookshelf-tally" : puzzleKey.trim();
        this.targetFilled = toSlotMask(filledSlots);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled || token.isBlank()) return;
        Block b = event.getClickedBlock();
        if (b == null || b.getType() != Material.CHISELED_BOOKSHELF) return;   // cheap type gate

        safety.run("vaun.shelf.interact", () -> {
            Player p = event.getPlayer();
            if (p == null || oracle == null || scheduler == null) return;
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Location loc = b.getLocation();
            if (loc.getWorld() == null) return;
            String world = loc.getWorld().getName();
            Site here = nearestPlacedOfType(sites, SHELF_TYPE, world, loc.getX(), loc.getY(), loc.getZ());
            if (here == null) return;                        // not the tally shelf

            final String mc = p.getUniqueId().toString();
            final String name = p.getName();
            scheduler.runLaterSafe("vaun.shelf.settle", SETTLE_TICKS, () -> safety.run(
                    "vaun.shelf.check", () -> checkPattern(here, mc, name)));
        });
    }

    /** MAIN-thread: read the shelf's 6-slot fill mask; on the exact target pattern, post the token. */
    private void checkPattern(Site shelf, String mcUuid, String playerName) {
        boolean[] filled = readFillMask(shelf);
        if (filled == null) return;                          // shelf not loaded / not a shelf
        for (int i = 0; i < SLOTS; i++) {
            if (filled[i] != targetFilled[i]) return;        // pattern mismatch — silent
        }

        if (!rateLimiter.tryCooldown("vaun_shelf:" + shelf.id(), CHECK_COOLDOWN_MS)) return;
        safety.info("vaun.shelf", playerName + " cleared the tally shelf — posting vaun-bookshelf-tally");
        scheduler.runAsyncSafe("vaun.shelf.resolve",
                () -> oracle.resolveWorld(mcUuid, playerName, token, puzzleKey));
    }

    /** The 6-slot filled/empty mask of a placed shelf site's block, or null if not a loaded shelf. */
    private boolean[] readFillMask(Site shelf) {
        Location loc = shelf.location();
        if (loc == null || loc.getWorld() == null) return null;
        BlockState state = loc.getBlock().getState();
        if (!(state instanceof ChiseledBookshelf cbs)) return null;
        Inventory inv = cbs.getInventory();
        boolean[] mask = new boolean[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            ItemStack it = (i < inv.getSize()) ? inv.getItem(i) : null;
            mask[i] = it != null && it.getType() != Material.AIR;
        }
        return mask;
    }

    /* ----------------------------- helpers ---------------------------- */

    /** Config 1-based slot list → a 6-length filled mask. Out-of-range indices ignored. */
    static boolean[] toSlotMask(List<Integer> slots) {
        boolean[] mask = new boolean[SLOTS];
        if (slots != null) {
            for (Integer s : slots) {
                if (s == null) continue;
                int idx = s - 1;
                if (idx >= 0 && idx < SLOTS) mask[idx] = true;
            }
        }
        return mask;
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
