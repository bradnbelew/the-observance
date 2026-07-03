package com.observance.watcher.beats;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.config.ObservanceConfig;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade that assembles + activates the beat subsystem and registers it with the foundation. ONE
 * call wires the whole Haunting Engine:
 *
 * <ul>
 *   <li>builds the {@link BeatContext}, {@link BeatLibrary}, {@link DramaBudget},</li>
 *   <li>installs the {@link RealBeatEnactor} (so the foundation's queue poller enacts real beats),</li>
 *   <li>registers the {@link BeatProtectionListener} (anti-grief) + {@link BeatSessionListener},</li>
 *   <li>schedules the {@link AmbientBeatGenerator} on a coarse, restraint-first cadence.</li>
 * </ul>
 *
 * <p>Activation is idempotent + fully Safety-wrapped: any failure degrades to "queued beats still
 * enact, ambient generation off" rather than crashing the server.
 *
 * <p>Intended call site: {@code ObservancePlugin#onEnable} (after the foundation singletons exist),
 * e.g. {@code new BeatEngine(plugin).activate();}
 */
public final class BeatEngine {

    private final ObservancePlugin plugin;

    private BeatContext ctx;
    private BeatLibrary library;
    private DramaBudget budget;
    private Attention attention;
    private ProtectedRegistry protectedRegistry;
    private AmbientBeatGenerator ambient;
    private WorldDriftClock driftClock;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private volatile boolean active = false;

    public BeatEngine(ObservancePlugin plugin) {
        this.plugin = plugin;
    }

    /** Build + wire + schedule everything. Safe to call once on enable / after reload. */
    public boolean activate() {
        return plugin.safety().call("beat.engine.activate", () -> {
            ObservanceConfig config = plugin.config();
            this.protectedRegistry = new ProtectedRegistry();
            this.ctx = new BeatContext(
                    plugin,
                    config,
                    plugin.sites(),
                    plugin.supabase(),
                    plugin.scheduler(),
                    plugin.safety(),
                    plugin.reveal(),
                    plugin.rateLimiter(),
                    protectedRegistry,
                    plugin.lensRegistry(),
                    "observance");

            this.library = new BeatLibrary(plugin.safety());
            this.budget = new DramaBudget(config, plugin.rateLimiter());

            // 1) Install the real enactor so the foundation poller realizes queued beats.
            plugin.setBeatEnactor(new RealBeatEnactor(ctx, library, budget));

            // 2) Anti-grief protection listener for beat-placed / protected blocks.
            plugin.getServer().getPluginManager().registerEvents(
                    new BeatProtectionListener(
                            protectedRegistry, plugin.sites(), plugin.scheduler(), plugin.safety(),
                            config.cancelProtectedBreaks(), config.restoreBrokenMarkers(),
                            config.restoreDelayTicks()),
                    plugin);

            // 3) Session listener: reset per-session budget on join; record sacred-beast kills.
            plugin.getServer().getPluginManager().registerEvents(
                    new BeatSessionListener(ctx, budget, plugin.safety()), plugin);

            // 4) Ambient generator on a coarse cadence (restraint-first; budget still gates each fire).
            //    The Attention accumulator makes selection responsive (the watcher is drawn to the
            //    lonely/deep/uneasy) and adds the upstream restraint gate (calm scenes mostly stay quiet).
            this.attention = new Attention(0.82);
            this.ambient = new AmbientBeatGenerator(
                    ctx, library, budget, attention,
                    plugin::isLocallyAsleep,
                    config::dramaEnabled);

            long periodTicks = Math.max(20L, ambientPeriodTicks(config));
            // Stagger the first tick so it doesn't coincide with boot load.
            tasks.add(plugin.scheduler().runTimerSafe(
                    "beat.ambient.tick", periodTicks, periodTicks, () -> ambientTick()));

            // 5) THE WORLD-DRIFT CLOCK — the world ages on its own between visits (sculk creeps near
            //    already-found sites, scaled by real elapsed time, hard-capped so an absence never
            //    carpets a place). Mirrors the ambient cadence but is world-scoped: it drifts a little
            //    near a rotating subset of PLACED sites each tick and no-ops entirely until a site is
            //    placed. Persists its one timestamp in arc_state.flags (no new table).
            this.driftClock = new WorldDriftClock(
                    ctx, library, plugin::isLocallyAsleep, config::dramaEnabled);
            long driftTicks = Math.max(20L, driftPeriodTicks(config));
            tasks.add(plugin.scheduler().runTimerSafe(
                    "beat.world.drift", driftTicks, driftTicks, () -> driftClock.tick()));

            this.active = true;
            plugin.logEvent("info", "beat.engine",
                    "activated; beats=" + library.size(), null);
            return true;
        }, false);
    }

    /**
     * One ambient consideration, but first cheaply pre-screen the REMOTE watcher_sleep async so we
     * don't synthesize ambient beats during a "sleep" session. The local switch is checked inside
     * {@link AmbientBeatGenerator#tick()} on main.
     */
    private void ambientTick() {
        // Quick local gates first (main thread, cheap).
        if (!plugin.config().dramaEnabled() || plugin.isLocallyAsleep()) return;
        if (plugin.getServer().getOnlinePlayers().isEmpty()) return;

        // Remote sleep check is network → async; then hop to main to actually consider a beat.
        plugin.scheduler().runAsyncSafe("beat.ambient.sleepcheck", () -> {
            boolean sleeping = false;
            try {
                if (plugin.supabase().isConfigured()) {
                    sleeping = plugin.supabase().isWatcherSleeping();
                }
            } catch (Throwable ignored) {
                sleeping = false; // fail-open: a DB hiccup never silences the whole game
            }
            if (sleeping) return;
            plugin.scheduler().runMainSafe("beat.ambient.consider", () -> ambient.tick());
        });
    }

    private static long ambientPeriodTicks(ObservanceConfig config) {
        // Consider an ambient beat roughly every (ambient global cooldown / 3), floored at 60s.
        int globalMin = Math.max(1, config.ambientGlobalCooldownMinutes());
        long seconds = Math.max(60L, (globalMin * 60L) / 3L);
        return seconds * 20L;
    }

    /**
     * The world-drift cadence: a SLOW background rhythm (twice the ambient window, floored at 5 min).
     * Drift is meant to be felt on RETURN, not watched happening — the elapsed-time scaling in
     * {@link WorldDriftClock} carries the "how much aged", so the tick just needs to be unhurried.
     */
    private static long driftPeriodTicks(ObservanceConfig config) {
        long ambientSeconds = ambientPeriodTicks(config) / 20L;
        long seconds = Math.max(300L, ambientSeconds * 2L);
        return seconds * 20L;
    }

    /** Cancel scheduled tasks + clear transient state. Safe to call on disable/reload. */
    public void deactivate() {
        plugin.safety().run("beat.engine.deactivate", () -> {
            for (BukkitTask t : tasks) com.observance.watcher.util.Scheduler.cancel(t);
            tasks.clear();
            if (budget != null) budget.clear();
            if (library != null) library.clearAppliedState();
            if (protectedRegistry != null) protectedRegistry.clearAll();
            active = false;
        });
    }

    public boolean isActive() { return active; }
    public BeatLibrary library() { return library; }
    public DramaBudget budget() { return budget; }
    public ProtectedRegistry protectedRegistry() { return protectedRegistry; }
    public BeatContext context() { return ctx; }
}
