package com.observance.watcher.signal.listener;

import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Resolves the six physical Keeper offerings around the Unbroken Light. */
public final class RiteTokenDepositListener implements Listener {
    public static final String FLAG_TOKENS_LAID = "tokens_laid";
    private static final int[][] OFFSETS = {{-6, 0}, {-3, 4}, {3, 4}, {6, 0}, {3, -4}, {-3, -4}};
    private static final Set<String> EXPECTED = Set.of("vaun", "mara", "sella", "orin", "brann", "iss");

    private final Supplier<SitesConfig> sitesSupplier;
    private final OracleResolver oracle;
    private final Scheduler scheduler;
    private final RateLimiter rateLimiter;
    private final Safety safety;
    private final Runnable readyCallback;
    private final String token;
    private final String puzzleKey;
    private final NamespacedKey tokenKey = new NamespacedKey("observance", "rite_token");
    private final NamespacedKey firstHeartKey = new NamespacedKey("observance", "vaun_first_deep");

    public RiteTokenDepositListener(Supplier<SitesConfig> sitesSupplier, OracleResolver oracle,
                                    Scheduler scheduler, RateLimiter rateLimiter, Safety safety,
                                    Runnable readyCallback, String token, String puzzleKey) {
        this.sitesSupplier = sitesSupplier;
        this.oracle = oracle;
        this.scheduler = scheduler;
        this.rateLimiter = rateLimiter;
        this.safety = safety;
        this.readyCallback = readyCallback;
        this.token = token == null ? "" : token.trim();
        this.puzzleKey = puzzleKey == null || puzzleKey.isBlank() ? "rite-tokens" : puzzleKey.trim();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        safety.run("accepting.tokens.close", () -> {
            if (oracle == null || scheduler == null || token.isBlank()) return;
            Location closed = event.getInventory().getLocation();
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            Site floor = sites == null ? null : sites.get("unbroken_light");
            Location center = floor == null ? null : floor.location();
            if (closed == null || center == null || closed.getWorld() == null || center.getWorld() == null
                    || !closed.getWorld().equals(center.getWorld())) return;
            if (!isSlot(center, closed)) return;
            OfferingState offering = readAllSlots(center);
            if (!offering.keepers().equals(EXPECTED) || offering.personalPages() != 6
                    || !offering.firstHeart() || center.getBlock().getType() != Material.SEA_LANTERN) return;
            if (!rateLimiter.tryCooldown("accepting:tokens_laid", 5_000L)) return;
            String uuid = event.getPlayer().getUniqueId().toString();
            String name = event.getPlayer().getName();
            scheduler.runAsyncSafe("accepting.tokens.flag", () -> {
                var result = oracle.resolveWorld(uuid, name, token, puzzleKey);
                if ((result == OracleResolver.Result.SOLVED || result == OracleResolver.Result.ALREADY_SOLVED)
                        && readyCallback != null) {
                    scheduler.runMainSafe("accepting.tokens.ready", readyCallback);
                }
                safety.info("accepting.tokens", "all six Keeper tokens were laid at the Unbroken Light");
            });
        });
    }

    private boolean isSlot(Location center, Location closed) {
        for (int[] offset : OFFSETS) {
            if (closed.getBlockX() == center.getBlockX() + offset[0]
                    && closed.getBlockY() == center.getBlockY()
                    && closed.getBlockZ() == center.getBlockZ() + offset[1]) return true;
        }
        return false;
    }

    private record OfferingState(Set<String> keepers, int personalPages, boolean firstHeart) {}

    private OfferingState readAllSlots(Location center) {
        Set<String> found = new HashSet<>();
        Set<String> personal = new HashSet<>();
        boolean firstHeart = false;
        for (int[] offset : OFFSETS) {
            Block block = center.getWorld().getBlockAt(center.getBlockX() + offset[0], center.getBlockY(),
                    center.getBlockZ() + offset[1]);
            if (!(block.getState() instanceof InventoryHolder holder)) return new OfferingState(Set.of(), 0, false);
            String token = null;
            String signedPage = null;
            for (ItemStack item : holder.getInventory().getContents()) {
                if (item == null || !item.hasItemMeta()) continue;
                var pdc = item.getItemMeta().getPersistentDataContainer();
                String value = pdc.get(tokenKey, PersistentDataType.STRING);
                if (EXPECTED.contains(value)) {
                    if (token != null) return new OfferingState(Set.of(), 0, false);
                    token = value;
                }
                if (pdc.has(firstHeartKey, PersistentDataType.BYTE)) firstHeart = true;
                if (item.getType() == Material.WRITTEN_BOOK && item.getItemMeta() instanceof BookMeta book
                        && book.getAuthor() != null && !book.getAuthor().isBlank()
                        && book.getTitle() != null && !book.getTitle().isBlank()) {
                    String fingerprint = book.getAuthor().trim() + "\n" + book.getTitle().trim();
                    if (signedPage != null) return new OfferingState(Set.of(), 0, false);
                    signedPage = fingerprint;
                }
            }
            if (token == null || signedPage == null || !found.add(token) || !personal.add(signedPage)) {
                return new OfferingState(Set.of(), 0, false);
            }
        }
        return new OfferingState(found, personal.size(), firstHeart);
    }
}
