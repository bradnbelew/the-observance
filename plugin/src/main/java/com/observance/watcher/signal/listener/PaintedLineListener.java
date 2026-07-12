package com.observance.watcher.signal.listener;

import com.google.gson.JsonObject;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.function.Supplier;

/**
 * The painted line is a promise NPCs make out loud. Crossing it must therefore matter.
 *
 * <p>This listener keeps the consequence quiet: a private pressure cue plus an idempotent arc flag.
 * Downstream showrunner/scare content can react to {@code painted_line_crossed}; the crossing itself never
 * explains, announces, or turns into a quest banner.
 */
public final class PaintedLineListener implements Listener {

    public static final String FLAG_CROSSED = "painted_line_crossed";
    public static final String FLAG_CROSSED_BY = "painted_line_crossed_by";
    public static final String FLAG_CROSSED_AT = "painted_line_crossed_at";

    private static final String LINE_TYPE = "painted_line";
    private static final long CROSS_COOLDOWN_MS = 5 * 60_000L;

    private final SupabaseClient supabase;
    private final Supplier<SitesConfig> sitesSupplier;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;

    public PaintedLineListener(SupabaseClient supabase, Supplier<SitesConfig> sitesSupplier,
                               RateLimiter rateLimiter, Scheduler scheduler, Safety safety) {
        this.supabase = supabase;
        this.sitesSupplier = sitesSupplier;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || to.getWorld() == null || from.getWorld() == null) return;
        if (to.getBlockX() == from.getBlockX()
                && to.getBlockY() == from.getBlockY()
                && to.getBlockZ() == from.getBlockZ()) return;

        Player player = event.getPlayer();
        if (player == null || supabase == null || scheduler == null) return;

        safety.run("painted_line.move", () -> {
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            if (sites == null) return;
            String worldName = to.getWorld().getName();

            Site line = sites.get("painted_line");
            if (line == null || !line.isPlaced() || !LINE_TYPE.equals(line.type())) return;
            Location center = line.location();
            if (center == null || center.getWorld() == null || !worldName.equals(center.getWorld().getName())) return;
            double planeZ = center.getBlockZ() + 0.5;
            double fromSide = from.getZ() - planeZ;
            double toSide = to.getZ() - planeZ;
            if (fromSide == 0.0 || toSide == 0.0 || Math.signum(fromSide) == Math.signum(toSide)) return;
            double dz = to.getZ() - from.getZ();
            if (Math.abs(dz) < 0.0001) return;
            double t = (planeZ - from.getZ()) / dz;
            double crossX = from.getX() + (to.getX() - from.getX()) * t;
            double crossY = from.getY() + (to.getY() - from.getY()) * t;
            if (crossX < center.getBlockX() - 8 || crossX > center.getBlockX() + 9
                    || crossY < center.getBlockY() || crossY > center.getBlockY() + 3) return;

            String key = "painted_line:" + player.getUniqueId();
            if (rateLimiter != null && !rateLimiter.tryCooldown(key, CROSS_COOLDOWN_MS)) return;

            playPressureCue(player);

            final String name = player.getName();
            final long now = System.currentTimeMillis();
            scheduler.runAsyncSafe("painted.line.flag", () -> {
                JsonObject flags = new JsonObject();
                flags.addProperty(FLAG_CROSSED, true);
                flags.addProperty(FLAG_CROSSED_BY, name);
                flags.addProperty(FLAG_CROSSED_AT, now);
                supabase.mergeArcFlags(flags);
                safety.info("painted_line",
                        FLAG_CROSSED + " set by " + name + " at " + line.id());
            });
        });
    }

    private void playPressureCue(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.45f, 0.62f);
        } catch (Throwable t) {
            try {
                player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 0.35f, 0.7f);
            } catch (Throwable ignored) { }
        }
    }

    private Site nearestPlacedOfType(SitesConfig sites, String type,
                                     String world, double x, double y, double z) {
        Site best = null;
        double bestD2 = Double.MAX_VALUE;
        for (Site s : sites.placedOfType(type)) {
            if (!s.contains(world, x, y, z)) continue;
            Location c = s.location();
            if (c == null) {
                if (best == null) best = s;
                continue;
            }
            double dx = x - c.getX(), dy = y - c.getY(), dz = z - c.getZ();
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < bestD2) {
                bestD2 = d2;
                best = s;
            }
        }
        return best;
    }
}
