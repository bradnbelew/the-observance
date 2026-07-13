package com.observance.watcher.signal.listener;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.ArcStateRow;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Detects both outcomes of the Unlit Deep collective-restraint trial (INV-17).
 *
 * <p>During a configured taboo-moon night, entering the Overworld at or below the configured deep
 * line arms that night's trial. An explicit player flame act breaks it; reaching the following
 * daylight without a break keeps it. Entry, break, and kept state use durable per-world/per-day
 * window keys in {@code arc_state.flags}, so reloads and restarts cannot grant a free success,
 * lose a real success, or report one night twice.
 *
 * <p>Only explicit acts count: a configured flame block placed by a player, or player-attributed
 * flint-and-steel/fire-charge ignition. Ambient spread, lava, lightning, daylight, and merely
 * holding a light never break the latch. Detection never cancels an event or names the actor in
 * chat. The actor is recorded for the private archive only.
 */
public final class UnlitDeepListener implements Listener {

    public static final String FLAG_ENTERED_WINDOW = "unlit_deep_entered_window";
    public static final String FLAG_BROKEN_AT = "unlit_deep_broken_at";
    public static final String FLAG_BROKEN_BY = "unlit_deep_broken_by";
    public static final String FLAG_BROKEN_WINDOW = "unlit_deep_broken_window";
    public static final String FLAG_KEPT_AT = "unlit_deep_kept_at";
    public static final String FLAG_KEPT_WINDOW = "unlit_deep_kept_window";

    private static final long NIGHT_START = 13_000L;
    private static final long DAWN_EVALUATION_DELAY_TICKS = 100L;

    private final SignalTracker tracker;
    private final SupabaseClient supabase;
    private final RateLimiter rateLimiter;
    private final Scheduler scheduler;
    private final Safety safety;
    private final Supplier<SitesConfig> sitesSupplier;
    private final Set<String> entryWrites = new HashSet<>();
    private final Set<String> evaluationAttempts = new HashSet<>();

    public UnlitDeepListener(SignalTracker tracker, SupabaseClient supabase,
                             RateLimiter rateLimiter, Scheduler scheduler, Safety safety,
                             Supplier<SitesConfig> sitesSupplier) {
        this.tracker = tracker;
        this.supabase = supabase;
        this.rateLimiter = rateLimiter;
        this.scheduler = scheduler;
        this.safety = safety;
        this.sitesSupplier = sitesSupplier;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        safety.run("unlit_deep.place", () -> {
            TrackerConfig cfg = tracker.config();
            if (!armed(cfg)) return;
            if (!cfg.isUnlitDeepFlameMaterial(event.getBlockPlaced().getType().name())) return;
            tryBreak(cfg, event.getBlock().getLocation(), event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        safety.run("unlit_deep.ignite", () -> {
            TrackerConfig cfg = tracker.config();
            if (!armed(cfg)) return;
            BlockIgniteEvent.IgniteCause cause = event.getCause();
            if (cause != BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL
                    && cause != BlockIgniteEvent.IgniteCause.FIREBALL) return;
            Player player = event.getPlayer();
            if (player == null) return;
            tryBreak(cfg, event.getBlock().getLocation(), player);
        });
    }

    /** Main-thread sampling hook. It reads Bukkit world/player state and schedules DB I/O async. */
    public void sampleTick() {
        TrackerConfig cfg = tracker.config();
        if (!armed(cfg)) return;

        for (World world : org.bukkit.Bukkit.getWorlds()) {
            if (world.getEnvironment() != World.Environment.NORMAL) continue;
            long day = Math.floorDiv(world.getFullTime(), 24_000L);
            long time = Math.floorMod(world.getFullTime(), 24_000L);

            if (isTrialNight(day, time, cfg)) {
                String window = windowKey(world.getName(), day);
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().getBlockY() <= cfg.unlitDeepDeepLineY()) {
                        recordEntry(window);
                        break;
                    }
                }
                continue;
            }

            // In daylight, retry the immediately preceding night's durable window. This also makes
            // a server restart after dawn safe: the persisted entry/break flags are still evaluated.
            if (time < NIGHT_START) {
                long completedDay = day - 1L;
                if (isTabooDay(completedDay, cfg)) {
                    evaluateCompletedWindow(windowKey(world.getName(), completedDay));
                }
            }
        }
    }

    private boolean armed(TrackerConfig cfg) {
        return cfg != null && cfg.unlitDeepEnabled() && cfg.restraintEnabled()
                && tracker != null && supabase != null && scheduler != null && rateLimiter != null;
    }

    private void tryBreak(TrackerConfig cfg, Location at, Player player) {
        if (at == null || player == null) return;
        World world = at.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) return;
        if (at.getBlockY() > cfg.unlitDeepDeepLineY()) return;

        long day = Math.floorDiv(world.getFullTime(), 24_000L);
        long time = Math.floorMod(world.getFullTime(), 24_000L);
        if (!isTrialNight(day, time, cfg)) return;
        if (!rateLimiter.tryCooldown("unlit_deep:latch", cfg.unlitDeepCooldownMs())) return;

        String window = windowKey(world.getName(), day);
        String name = player.getName();
        long now = System.currentTimeMillis();
        entryWrites.add(window);
        scheduler.runAsyncSafe("unlit_deep.break", () -> {
            JsonObject flags = new JsonObject();
            flags.addProperty(FLAG_ENTERED_WINDOW, window);
            flags.addProperty(FLAG_BROKEN_AT, now);
            flags.addProperty(FLAG_BROKEN_BY, name);
            flags.addProperty(FLAG_BROKEN_WINDOW, window);
            supabase.mergeArcFlags(flags);
            applyBorrowedGlow(false);
            safety.info("unlit_deep", "the deep was lit on the black moon - kept no longer (recorded, not spoken)");
        });
    }

    private void recordEntry(String window) {
        if (!entryWrites.add(window)) return;
        scheduler.runAsyncSafe("unlit_deep.enter", () -> {
            JsonObject flags = new JsonObject();
            flags.addProperty(FLAG_ENTERED_WINDOW, window);
            supabase.mergeArcFlags(flags);
        });
    }

    private void evaluateCompletedWindow(String window) {
        if (!evaluationAttempts.add(window)) return;
        scheduler.runAsyncLaterSafe("unlit_deep.kept", DAWN_EVALUATION_DELAY_TICKS, () -> {
            var result = supabase.fetchArcState();
            if (!result.ok() || result.value() == null) {
                // Permit a later sample to retry after a transient DB failure.
                scheduler.runMainSafe("unlit_deep.kept.retry", () -> evaluationAttempts.remove(window));
                return;
            }
            ArcStateRow row = result.value();
            JsonObject flags = row.flags == null ? new JsonObject() : row.flags;
            if (!window.equals(stringFlag(flags, FLAG_ENTERED_WINDOW))) return;
            if (window.equals(stringFlag(flags, FLAG_BROKEN_WINDOW))) return;
            if (window.equals(stringFlag(flags, FLAG_KEPT_WINDOW))) return;

            JsonObject update = new JsonObject();
            update.addProperty(FLAG_KEPT_AT, System.currentTimeMillis());
            update.addProperty(FLAG_KEPT_WINDOW, window);
            supabase.mergeArcFlags(update);
            applyBorrowedGlow(true);
            safety.info("unlit_deep", "the black-moon deep was crossed without flame - the restraint was kept");
        });
    }

    private static String stringFlag(JsonObject flags, String key) {
        try {
            JsonElement value = flags.get(key);
            return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                    ? value.getAsString() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /** Physical payoff: the Accepting-floor cross visibly lends or withdraws its light. */
    private void applyBorrowedGlow(boolean kept) {
        scheduler.runMainSafe("unlit_deep.glow", () -> {
            SitesConfig sites = sitesSupplier == null ? null : sitesSupplier.get();
            Site light = sites == null ? null : sites.get("unbroken_light");
            Location at = light == null ? null : light.location();
            if (at == null || at.getWorld() == null) return;
            World world = at.getWorld();
            int x = at.getBlockX(), y = at.getBlockY(), z = at.getBlockZ();
            for (int dx = -5; dx <= 5; dx++) {
                for (int dz = -5; dz <= 5; dz++) {
                    if (Math.abs(dx) > 1 && Math.abs(dz) > 1) continue;
                    world.getBlockAt(x + dx, y - 1, z + dz).setType(
                            kept ? org.bukkit.Material.SEA_LANTERN : org.bukkit.Material.POLISHED_DEEPSLATE,
                            false);
                }
            }
            world.getBlockAt(x, y, z).setType(
                    kept ? org.bukkit.Material.SEA_LANTERN : org.bukkit.Material.CRYING_OBSIDIAN, false);
        });
    }

    public static String windowKey(String worldName, long day) {
        return worldName + ":" + day;
    }

    static boolean isTrialNight(long day, long time, TrackerConfig cfg) {
        return time >= NIGHT_START && isTabooDay(day, cfg);
    }

    private static boolean isTabooDay(long day, TrackerConfig cfg) {
        int phase = (int) Math.floorMod(day, 8L);
        return cfg.isUnlitDeepTabooMoonPhase(phase);
    }
}
