package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Supplier;

/**
 * Vaun — {@code vaun-hoard-sorted} (design/PUZZLE-DESIGNS.md §2.1). "Give the first of the deep
 * back": the offering Vaun never made. A container (a chest) sits at the {@code vaun_hoard_chest}
 * site — the empty "given back" chest. When a keeper deposits the required first-of-the-deep items
 * into it and closes it, this producer performs a sorted-hoard content check on the container's
 * contents; if EVERY required material is present, it posts the puzzle's OPAQUE token to the shared
 * oracle. The deposit is the solve — never the typed fallback phrase (which would leak).
 *
 * <p>Detection is {@link InventoryCloseEvent}: on close, if the closed inventory belongs to a block
 * whose location sits inside a {@code vaun_hoard_chest} site, the contents are scanned. This reads
 * the SETTLED state (the player has finished arranging), so a half-filled mid-deposit never fires. A
 * per-site cooldown prevents open/close spam from hammering the oracle; the solve itself is idempotent
 * regardless (the oracle dedupes), so the offering can never be "farmed".
 *
 * <p>Fault-isolated (Safety), reveal-safe (never cancels the event, mutates the world, or messages).
 * Sites resolved live via a {@link Supplier}. All reads on the MAIN thread (event thread); only the
 * oracle resolve hops async.
 */
public final class HoardSortedListener implements Listener {

    private static final String CHEST_TYPE = "vaun_hoard_chest";
    private static final long CHECK_COOLDOWN_MS = 3_000L;

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    private final boolean enabled;
    private final String token;
    private final String puzzleKey;
    private final NamespacedKey relicKey = new NamespacedKey("observance", "vaun_first_deep");

    public HoardSortedListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                               RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                               boolean enabled, String token, String puzzleKey) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = (puzzleKey == null || puzzleKey.isBlank()) ? "vaun-hoard-sorted" : puzzleKey.trim();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        safety.run("vaun.hoard.close", () -> {
            if (!enabled || token.isBlank() || oracle == null || scheduler == null) return;
            if (!(event.getPlayer() instanceof Player p)) return;

            Inventory inv = event.getInventory();
            if (inv == null) return;
            Location loc = inv.getLocation();       // block-backed containers report a location; player invs null
            if (loc == null || loc.getWorld() == null) return;
            String world = loc.getWorld().getName();

            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;

            Site chest = nearestPlacedOfType(sites, CHEST_TYPE, world, loc.getX(), loc.getY(), loc.getZ());
            if (chest == null) return;              // not the "given back" chest

            if (!containsAuthoredRelic(inv)) return;

            // Anti-spam: one check per chest per window. The solve is idempotent regardless.
            if (!rateLimiter.tryCooldown("vaun_hoard:" + chest.id(), CHECK_COOLDOWN_MS)) return;

            final String mc = p.getUniqueId().toString();
            final String name = p.getName();
            safety.info("vaun.hoard", name + " gave the first of the deep back at " + chest.id()
                    + " — posting vaun-hoard-sorted");
            scheduler.runAsyncSafe("vaun.hoard.resolve",
                    () -> oracle.resolveWorld(mc, name, token, puzzleKey));
        });
    }

    /* ----------------------------- helpers ---------------------------- */

    private boolean containsAuthoredRelic(Inventory inv) {
        for (ItemStack it : inv.getContents()) {
            if (it == null || it.getType() == Material.AIR) continue;
            if (it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer()
                    .has(relicKey, PersistentDataType.BYTE)) return true;
        }
        return false;
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
