package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Automatic, private progression and scare cues for the optional Dread passage. */
public final class DreadRouteListener implements Listener {
    private static final String[] ROUTE = {
            "dread_route_start", "dread_route_elsewhere", "dread_route_figure", "dread_route_exit"
    };
    private static final long ROUTE_TIMEOUT_MS = 4 * 60_000L;

    private final Supplier<SitesConfig> sitesSupplier;
    private final SupabaseClient supabase;
    private final Scheduler scheduler;
    private final RateLimiter rateLimiter;
    private final Safety safety;
    private final Map<UUID, Integer> next = new HashMap<>();
    private final Map<UUID, Long> started = new HashMap<>();
    private final Map<UUID, String> inside = new HashMap<>();

    public DreadRouteListener(Supplier<SitesConfig> sitesSupplier, SupabaseClient supabase,
                              Scheduler scheduler, RateLimiter rateLimiter, Safety safety) {
        this.sitesSupplier = sitesSupplier;
        this.supabase = supabase;
        this.scheduler = scheduler;
        this.rateLimiter = rateLimiter;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null || to.getWorld() == null) return;
        safety.run("dread.route.move", () -> {
            Player player = event.getPlayer();
            UUID id = player.getUniqueId();
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            String entered = routeSiteAt(sites, to);
            if (entered == null) {
                inside.remove(id);
                return;
            }
            if (entered.equals(inside.put(id, entered))) return;
            int expected = next.getOrDefault(id, 0);
            Long began = started.get(id);
            if (began != null && System.currentTimeMillis() - began > ROUTE_TIMEOUT_MS) expected = 0;
            int rank = routeRank(entered);
            if (rank != expected) {
                reset(id);
                if (rank != 0) return;
            }
            if (rank == 0) started.put(id, System.currentTimeMillis());
            next.put(id, rank + 1);
            enactStage(player, rank);
            if (rank == ROUTE.length - 1) {
                reset(id);
                publishCompletion(player);
            }
        });
    }

    private void enactStage(Player player, int rank) {
        switch (rank) {
            case 0 -> {
                player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f, 0.55f);
                player.sendActionBar(Component.text("another pace answers yours", NamedTextColor.DARK_GRAY));
            }
            case 1 -> {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0, false, false, false));
                player.playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 0.25f, 0.45f);
            }
            case 2 -> {
                Location behind = player.getLocation().subtract(player.getLocation().getDirection().multiply(4));
                player.playSound(behind, Sound.ENTITY_ENDERMAN_STARE, 0.35f, 0.5f);
                player.sendActionBar(Component.text("do not supply the missing count", NamedTextColor.BLACK));
            }
            case 3 -> {
                player.removePotionEffect(PotionEffectType.DARKNESS);
                player.playSound(player.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, 0.8f, 0.5f);
            }
            default -> { }
        }
    }

    private void publishCompletion(Player player) {
        if (supabase == null || scheduler == null
                || !rateLimiter.tryCooldown("dread:complete:" + player.getUniqueId(), 60_000L)) return;
        scheduler.runAsyncSafe("dread.route.complete", () -> {
            JsonObject flags = new JsonObject();
            flags.addProperty("dread_route_survived", true);
            supabase.mergeArcFlags(flags);
            safety.info("dread.route", player.getName() + " completed the authored Dread passage");
        });
    }

    private String routeSiteAt(SitesConfig sites, Location point) {
        String world = point.getWorld().getName();
        for (String id : ROUTE) {
            Site site = sites.get(id);
            if (site != null && site.isPlaced() && site.contains(world, point.getX(), point.getY(), point.getZ())) return id;
        }
        return null;
    }

    private int routeRank(String id) {
        for (int i = 0; i < ROUTE.length; i++) if (ROUTE[i].equals(id)) return i;
        return -1;
    }

    private void reset(UUID id) {
        next.remove(id);
        started.remove(id);
        inside.remove(id);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { reset(event.getPlayer().getUniqueId()); }
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) { reset(event.getPlayer().getUniqueId()); }
}
