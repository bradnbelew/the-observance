package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Marks authored side-proof sites as actually discovered.
 *
 * <p>The Recovery Archive already knows how to reveal a card from {@code flag:<name>}. This small
 * sampler closes the missing producer: if a player physically reaches a placed proof site, the site
 * writes one group-scoped arc flag. That keeps the side web honest without adding visible quest markers.
 */
public final class SiteDiscoveryListener {

    private record Discovery(String siteId, String flagKey, String message) { }

    private static final List<Discovery> DISCOVERIES = List.of(
            new Discovery("school_stand", "site_seen_school_stand", "The school copy is filed."),
            new Discovery("markers_row", "site_seen_markers_row", "The hollow after six is filed."),
            new Discovery("cistern_7", "site_seen_cistern_7", "Cistern seven is filed."),
            new Discovery("watch_floor", "site_seen_watch_floor", "The watch-floor is filed."),
            new Discovery("set_apart_shelf", "site_seen_set_apart_shelf", "Entry five is filed."),
            new Discovery("undercroft_seal", "site_seen_undercroft_seal", "The wrong-side seal is filed."),
            new Discovery("forgotten_mouth", "site_seen_forgotten_mouth", "The way-up draft is filed."),
            new Discovery("deep_market", "site_seen_deep_market", "The market is filed."),
            new Discovery("ration_table", "site_seen_ration_table", "The ration table is filed."),
            new Discovery("third_bay_breach", "site_seen_third_bay_breach", "The third bay is filed."),
            new Discovery("warm_town_collapse", "site_seen_warm_town_collapse", "The warm-town story is filed."),
            new Discovery("deep_bird_coops", "site_seen_deep_bird_coops", "The empty coops are filed.")
    );

    private final SupabaseClient supabase;
    private final Supplier<SitesConfig> sitesSupplier;
    private final Scheduler scheduler;
    private final Safety safety;
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    public SiteDiscoveryListener(SupabaseClient supabase,
                                 Supplier<SitesConfig> sitesSupplier,
                                 Scheduler scheduler,
                                 Safety safety) {
        this.supabase = supabase;
        this.sitesSupplier = sitesSupplier;
        this.scheduler = scheduler;
        this.safety = safety;
    }

    /** MAIN-thread sampler: checks online players against placed site radii, then writes flags async. */
    public void tick() {
        safety.run("site.discovery.tick", () -> {
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player == null || !player.isOnline()) continue;
                Location loc = player.getLocation();
                if (loc == null || loc.getWorld() == null) continue;
                for (Discovery discovery : DISCOVERIES) {
                    if (reported.contains(discovery.flagKey())) continue;
                    Site site = sites.get(discovery.siteId());
                    if (site == null || !site.contains(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ())) {
                        continue;
                    }
                    reported.add(discovery.flagKey());
                    mergeFlag(discovery.flagKey());
                    player.sendActionBar(Component.text(discovery.message(), NamedTextColor.DARK_GRAY));
                }
            }
        });
    }

    private void mergeFlag(String key) {
        if (supabase == null || scheduler == null || key == null || key.isBlank()) return;
        scheduler.runAsyncSafe("site.discovery.flag." + key, () -> {
            JsonObject flags = new JsonObject();
            flags.addProperty(key, true);
            supabase.mergeArcFlags(flags);
        });
    }
}
