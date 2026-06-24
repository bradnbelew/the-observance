package com.observance.watcher.signal.listener;

import com.observance.watcher.signal.PlayerSignals;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

/**
 * The Dark Hours custom (arc bible: "do not sleep on the black moon"):
 * sleeping ({@link PlayerBedEnterEvent}) during a taboo moon phase is a tracked VIOLATION.
 *
 * <p>The moon phase is the vanilla cycle derived from the world's full-time:
 * {@code (world.getFullTime() / 24000L) % 8} → 0..7, where 0 is the full moon. The taboo set is
 * configurable ({@code tracker.dark-hours.taboo-moon-phases}; default {@code [0]} = full moon, the
 * arc's "black moon"). We only judge the moment of ENTERING the bed (the act of choosing to sleep
 * through the dark hours), and only when the bed-enter actually succeeds (MONITOR + ignoreCancelled,
 * so a denied bed — wrong time, monsters nearby, obstructed — never counts).
 *
 * PURE TRACKING + ANTI-EXPLOIT: records compliance only; never cancels the event, never mutates the
 * world, never messages the player (a downstream beat owns any escalation, which stays soft +
 * reversible). A per-player {@link RateLimiter} cooldown collapses get-in/get-out bed spam on a
 * taboo night into a single measured flag. All Bukkit reads on the MAIN thread; persistence is
 * deferred to the tracker's async flush. Body fully wrapped in Safety.
 */
public final class DarkHoursListener implements Listener {

    private final SignalTracker tracker;
    private final RateLimiter rateLimiter;
    private final Safety safety;

    public DarkHoursListener(SignalTracker tracker, RateLimiter rateLimiter, Safety safety) {
        this.tracker = tracker;
        this.rateLimiter = rateLimiter;
        this.safety = safety;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBedEnter(PlayerBedEnterEvent event) {
        safety.run("signal.DarkHours.bedEnter", () -> {
            // Only count a bed-enter that the server is actually going to honor.
            if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;

            Player p = event.getPlayer();
            if (p == null) return;

            TrackerConfig cfg = tracker.config();
            if (!cfg.enabled() || !cfg.darkHoursEnabled()) return;

            World world = p.getWorld();
            if (world == null) return;

            int phase = moonPhase(world);
            if (!cfg.isTabooMoonPhase(phase)) return;   // not the dark hours — nothing to record

            // Anti-spam: one violation per player per cooldown window (default 60s).
            String key = "dark_hours:" + p.getUniqueId();
            if (!rateLimiter.tryCooldown(key, cfg.darkHoursCooldownMs())) return;

            PlayerSignals ps = tracker.signals(p.getUniqueId(), p.getName());
            ps.violate(TrackerConfig.CUSTOM_DARK_HOURS, System.currentTimeMillis());
            safety.info("signal.dark_hours", p.getName() + " slept through the dark hours (phase=" + phase + ")");
        });
    }

    /** Vanilla moon phase 0..7 (0 = full moon) from the world's full day count. */
    private int moonPhase(World world) {
        long days = world.getFullTime() / 24000L;
        int phase = (int) (days % 8L);
        return phase < 0 ? phase + 8 : phase;   // guard against negative full-time edge cases
    }
}
