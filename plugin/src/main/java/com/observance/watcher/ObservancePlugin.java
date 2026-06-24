package com.observance.watcher;

import com.observance.watcher.beat.BeatEnactor;
import com.observance.watcher.beat.BeatQueuePoller;
import com.observance.watcher.beat.NoopBeatEnactor;
import com.observance.watcher.command.ObservanceCommand;
import com.observance.watcher.config.ObservanceConfig;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.listener.PresenceListener;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.signal.InventoryScanner;
import com.observance.watcher.signal.LocationSampler;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.signal.listener.BlockBreakListener;
import com.observance.watcher.signal.listener.ChatListener;
import com.observance.watcher.signal.listener.AnswerSignListener;
import com.observance.watcher.signal.listener.CustomComplianceListener;
import com.observance.watcher.signal.listener.DarkHoursListener;
import com.observance.watcher.signal.listener.DeathListener;
import com.observance.watcher.signal.listener.TerritoryListener;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Reveal;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Observance — Phase 0 foundation. Deterministic spine: signal-tracking bridge, async
 * Supabase client, beat-queue poller, and the safety/threading utilities every subsystem builds
 * on. NO AI/LLM in Phase 0.
 *
 * <p>onEnable wiring order matters: Safety (logger only) → config → SupabaseClient → Safety's
 * remote event sink → utilities → poller + samplers + listeners + command.
 */
public final class ObservancePlugin extends JavaPlugin {

    // --- core singletons ---
    private Safety safety;
    private Scheduler scheduler;
    private ObservanceConfig config;
    private SitesConfig sites;
    private SupabaseClient supabase;

    // --- shared utilities exposed to subsystem agents ---
    private Reveal reveal;
    private RateLimiter rateLimiter;

    // --- oracle (the closed clue loop; in-world answer-sign verb) ---
    private OracleResolver oracleResolver;

    // --- signal tracker (the dossier) ---
    private TrackerConfig trackerConfig;
    private SignalTracker signalTracker;
    private LocationSampler locationSampler;
    private InventoryScanner inventoryScanner;

    // --- resource-pack load gate (MF-11): rune rendering is unsafe until the client applies the pack ---
    private com.observance.watcher.signal.ResourcePackTracker resourcePack;

    // --- beat pipeline ---
    private BeatQueuePoller poller;
    private final AtomicReference<BeatEnactor> beatEnactor = new AtomicReference<>();

    // --- haunting engine (beat library + drama budget + ambient generation) ---
    private com.observance.watcher.beats.BeatEngine beatEngine;

    // --- runtime state ---
    private volatile boolean locallyAsleep = false;          // /observance sleep on|off
    private final List<BukkitTask> scheduledTasks = new ArrayList<>();

    /* ==================================================================== */
    /*  Lifecycle                                                           */
    /* ==================================================================== */

    @Override
    public void onEnable() {
        // 1. Safety with console logger only (no remote sink yet — client doesn't exist).
        this.safety = new Safety(getLogger());
        this.scheduler = new Scheduler(this, safety);

        // 2. Config + sites (defensive: a parse failure degrades to safe defaults, never crashes).
        if (!loadConfigAndSites()) {
            getLogger().severe("Failed to load configuration — disabling Observance to avoid undefined behavior.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Supabase client.
        this.supabase = new SupabaseClient(config, getLogger());

        // 4. Now that the client exists, route Safety's error events into event_log (async).
        wireEventSink();

        // 5. Shared utilities.
        this.reveal = new Reveal(config.witnessRadius());
        this.rateLimiter = new RateLimiter();

        // 5a. Oracle resolver — the shared world-surface answer path (built before listeners).
        this.oracleResolver = new OracleResolver(config, supabase, rateLimiter, safety);

        // 5b. Signal tracker (the dossier) — pure tracking, no world effects.
        this.trackerConfig = TrackerConfig.from(getConfig());
        this.signalTracker = new SignalTracker(supabase, scheduler, safety,
                trackerConfig, config.heatmapCellSize());
        this.locationSampler = new LocationSampler(signalTracker, this::sites, rateLimiter, safety,
                config.locationSampleSeconds());
        this.inventoryScanner = new InventoryScanner(signalTracker, safety);

        // 5c. Resource-pack load gate (MF-11). Created ONCE — survives a config reload so applied-pack
        //     status isn't lost (the client won't re-report without a re-push). The sink forwards each
        //     status to event_log so the director's dashboard sees who is pack-ready before rune beats.
        if (this.resourcePack == null) {
            this.resourcePack = new com.observance.watcher.signal.ResourcePackTracker(
                    safety,
                    (uuid, name, st) -> logEvent("info", "pack", name + " resource-pack " + st, uuid.toString()));
        }

        // 6. Default (noop) beat enactor — subsystem agents replace via setBeatEnactor().
        this.beatEnactor.set(new NoopBeatEnactor(safety));

        // 7. Beat-queue poller.
        this.poller = new BeatQueuePoller(config, supabase, scheduler, safety,
                beatEnactor::get, this::isLocallyAsleep);

        // 8. Register listeners + command.
        registerListeners();
        registerCommands();

        // 9. Schedule async poller + maintenance.
        startSchedulers();

        // 9b. Activate the Haunting Engine: beat library + drama budget + ambient generation +
        //     anti-grief protection. This installs the REAL beat enactor (replacing the noop), so
        //     queued beats now realize in-world. Fully fault-isolated: a failure here degrades to
        //     "no beats" rather than crashing the server.
        this.beatEngine = new com.observance.watcher.beats.BeatEngine(this);
        beatEngine.activate();

        // 10. Boot event (async; degrades silently if DB down).
        logEvent("info", "boot", "Observance enabled (Phase 0). configured="
                + supabase.isConfigured() + " sitesPlaced=" + sites.placedCount(), null);

        getLogger().info("The Observance is watching. (Phase 0, configured=" + supabase.isConfigured()
                + ", placed sites=" + sites.placedCount() + ")");
    }

    @Override
    public void onDisable() {
        // Tear down the haunting engine first (cancels ambient task, clears transient beat state).
        if (beatEngine != null) {
            beatEngine.deactivate();
        }

        // Cancel our scheduled tasks; flush whatever offline writes we can (best effort, bounded).
        for (BukkitTask t : scheduledTasks) {
            Scheduler.cancel(t);
        }
        scheduledTasks.clear();

        if (safety != null) {
            // Stop emitting to Supabase during shutdown (scheduler is winding down).
            safety.setEventSink(null);
        }
        // Note: we intentionally do NOT block onDisable on network flushes — the server shutdown
        // path must stay fast. Queued writes are durable-by-restart only insofar as the queue is
        // in-memory; Phase 1 can add a disk spill if needed.
        getLogger().info("The Observance sleeps.");
    }

    /* ==================================================================== */
    /*  Config loading / reload                                             */
    /* ==================================================================== */

    private boolean loadConfigAndSites() {
        try {
            saveDefaultConfig();          // writes config.yml from resources if absent
            reloadConfig();
            FileConfiguration cfg = getConfig();
            this.config = ObservanceConfig.from(cfg, System::getenv);

            this.sites = loadSites();
            for (String w : sites.warnings()) {
                getLogger().warning("[sites] " + w);
            }
            return true;
        } catch (Throwable t) {
            getLogger().severe("Config load failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return false;
        }
    }

    private SitesConfig loadSites() {
        try {
            File file = new File(getDataFolder(), "sites.yml");
            if (!file.exists()) {
                saveResource("sites.yml", false);     // copy default from jar
            }
            FileConfiguration sitesCfg = YamlConfiguration.loadConfiguration(file);

            // Merge in jar defaults so missing keys still resolve.
            try (InputStream in = getResource("sites.yml")) {
                if (in != null) {
                    YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(in, StandardCharsets.UTF_8));
                    sitesCfg.setDefaults(defaults);
                }
            } catch (Exception ignored) {
                // defaults are a nicety; absence is non-fatal
            }
            return SitesConfig.from(sitesCfg);
        } catch (Throwable t) {
            getLogger().warning("sites.yml load failed (" + t.getClass().getSimpleName()
                    + ") — using empty site set.");
            return SitesConfig.empty();
        }
    }

    /** Reload config + sites at runtime. Rebuilds dependent singletons that hold config values. */
    public boolean reloadAll() {
        return safety.call("plugin.reload", () -> {
            reloadConfig();
            this.config = ObservanceConfig.from(getConfig(), System::getenv);
            this.sites = loadSites();
            for (String w : sites.warnings()) getLogger().warning("[sites] " + w);

            // Rebuild config-derived utilities + client.
            this.supabase = new SupabaseClient(config, getLogger());
            wireEventSink();
            this.reveal = new Reveal(config.witnessRadius());
            // Rebuild the oracle resolver against the new client/config (before re-registering listeners).
            this.oracleResolver = new OracleResolver(config, supabase, rateLimiter, safety);

            // Rebuild the signal tracker against the new client/config. Note: in-memory dossier
            // state for the current session is intentionally reset on a hard reload; persisted
            // dossiers in Supabase are the source of truth across restarts. (Phase 1 may rehydrate.)
            this.trackerConfig = TrackerConfig.from(getConfig());
            this.signalTracker = new SignalTracker(supabase, scheduler, safety,
                    trackerConfig, config.heatmapCellSize());
            this.locationSampler = new LocationSampler(signalTracker, this::sites, rateLimiter, safety,
                    config.locationSampleSeconds());
            this.inventoryScanner = new InventoryScanner(signalTracker, safety);
            // Re-register listeners so they point at the new tracker instance.
            org.bukkit.event.HandlerList.unregisterAll(this);
            registerListeners();

            // Rebuild the poller against the new config/client; restart schedulers.
            for (BukkitTask t : scheduledTasks) Scheduler.cancel(t);
            scheduledTasks.clear();
            this.poller = new BeatQueuePoller(config, supabase, scheduler, safety,
                    beatEnactor::get, this::isLocallyAsleep);
            startSchedulers();

            // Rebuild the haunting engine against the new config/sites. Its listeners were dropped by
            // the unregisterAll above; activate() re-registers protection + session listeners and
            // re-installs the real enactor + ambient generator.
            if (beatEngine != null) beatEngine.deactivate();
            this.beatEngine = new com.observance.watcher.beats.BeatEngine(this);
            beatEngine.activate();

            logEvent("info", "reload", "config+sites reloaded; sitesPlaced=" + sites.placedCount(), null);
            return true;
        }, false);
    }

    private void wireEventSink() {
        final SupabaseClient client = this.supabase;
        safety.setEventSink((type, context, message) -> {
            // Async, fire-and-forget, never throws back into Safety.
            if (scheduler == null || client == null) return;
            scheduler.runAsync(() -> {
                try {
                    client.insertEventLog(new EventLogRow(
                            type, context, message, null, null, SupabaseClient.timestampNow()));
                } catch (Throwable ignored) {
                    // Logging must never crash anything.
                }
            });
        });
    }

    /* ==================================================================== */
    /*  Registration + scheduling                                           */
    /* ==================================================================== */

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PresenceListener(supabase, scheduler, safety, signalTracker), this);

        // Resource-pack load gate (MF-11) — the SAME instance across reloads (its map is the truth of
        // who has the pack applied); re-registered here because reloadAll() unregisters all handlers.
        if (resourcePack != null) pm.registerEvents(resourcePack, this);

        // Signal Tracker listeners (DESIGN §2.1 + §2.2) — pure tracking, no world effects.
        pm.registerEvents(new BlockBreakListener(signalTracker, rateLimiter, safety), this);
        pm.registerEvents(new DeathListener(this, signalTracker, safety), this);
        pm.registerEvents(new ChatListener(signalTracker, safety), this);
        pm.registerEvents(new TerritoryListener(signalTracker, safety), this);
        pm.registerEvents(new CustomComplianceListener(
                signalTracker, this::sites, rateLimiter, safety), this);
        // The Dark Hours custom — sleeping on a taboo moon phase is a tracked violation
        // (rate-limited per player). Pure tracking; escalation is a downstream beat's job.
        pm.registerEvents(new DarkHoursListener(signalTracker, rateLimiter, safety), this);

        // The in-world answer verb (the closed clue loop's world surface). Sites resolved live so a
        // reload is picked up; resolver shares the same puzzles table as the Discord surface.
        pm.registerEvents(new AnswerSignListener(
                oracleResolver, this::sites, rateLimiter, scheduler, safety), this);

        // The Accepting — the TERMINAL group rite (MF-8). A synchronized group bow on the
        // accepting_floor site posts the opaque token to the same oracle (never typeable). Config-driven;
        // degrades to a no-op when disabled or the token is blank. Read live so a reload re-arms it.
        var rites = getConfig();
        if (rites.getBoolean("rites.accepting.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.AcceptingRiteListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    rites.getString("rites.accepting.token", ""),
                    rites.getString("rites.accepting.puzzle-key", "accepting-crouch"),
                    rites.getInt("rites.accepting.quorum", 6),
                    rites.getLong("rites.accepting.cooldown-seconds", 300L) * 1000L), this);
        }
    }

    private void registerCommands() {
        ObservanceCommand handler = new ObservanceCommand(this, safety);
        var cmd = getCommand("observance");
        if (cmd != null) {
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        } else {
            getLogger().warning("Command 'observance' missing from plugin.yml — admin command unavailable.");
        }
    }

    private void startSchedulers() {
        long pollTicks = Math.max(20L, config.beatPollIntervalSeconds() * 20L);
        // Beat-queue poller — ASYNC (it does network I/O; it hops to main internally to mutate).
        scheduledTasks.add(scheduler.runAsyncTimerSafe(
                "beat.poller", pollTicks, pollTicks, () -> poller.pollOnce()));

        // Offline-queue flush + rate-limiter prune — ASYNC, slower cadence.
        long maintTicks = 60L * 20L; // every 60s
        scheduledTasks.add(scheduler.runAsyncTimerSafe("maint", maintTicks, maintTicks, () -> {
            supabase.flushOfflineQueue();
            rateLimiter.prune();
        }));

        // Presence heartbeat — touches Bukkit (online players) so it runs on MAIN, then writes async.
        long hbTicks = Math.max(20L, config.presenceHeartbeatSeconds() * 20L);
        scheduledTasks.add(scheduler.runTimerSafe("presence.heartbeat", hbTicks, hbTicks,
                this::presenceHeartbeat));

        // --- Signal Tracker schedules ---

        // Location sampler — reads Bukkit (positions/lights) so MAIN thread. Heatmap + cohesion +
        // solo-mining + Kept-Light scan. NOT PlayerMoveEvent (DESIGN §2.1).
        long sampleTicks = Math.max(20L, config.locationSampleSeconds() * 20L);
        scheduledTasks.add(scheduler.runTimerSafe("sampler.location", sampleTicks, sampleTicks,
                () -> locationSampler.sampleTick()));

        // Inventory/hoard scanner — reads live inventories so MAIN thread.
        long invTicks = Math.max(20L, config.inventoryScanSeconds() * 20L);
        scheduledTasks.add(scheduler.runTimerSafe("sampler.inventory", invTicks, invTicks,
                () -> inventoryScanner.scanTick()));

        // Dossier/compliance/heatmap flush — network I/O so ASYNC. Cadence = the presence
        // heartbeat (a sensible "write back what changed" rhythm).
        scheduledTasks.add(scheduler.runAsyncTimerSafe("tracker.flush", hbTicks, hbTicks, () -> {
            SignalTracker.FlushSummary sum = signalTracker.flushOnce();
            if (config.debug() && sum.total() > 0) {
                getLogger().info("[tracker.flush] dossiers=" + sum.dossiers()
                        + " compliance=" + sum.compliance() + " cells=" + sum.cells());
            }
        }));

        // Base-detection pass — clustering build + upsert; network I/O so ASYNC, slow cadence.
        long baseTicks = Math.max(20L, config.baseDetectSeconds() * 20L);
        scheduledTasks.add(scheduler.runAsyncTimerSafe("tracker.bases", baseTicks, baseTicks,
                () -> {
                    int n = signalTracker.flushBases();
                    if (config.debug() && n > 0) {
                        getLogger().info("[tracker.bases] upserted " + n + " base(s)");
                    }
                }));
    }

    /** MAIN thread: snapshot online players, then upsert last_seen off-thread. */
    private void presenceHeartbeat() {
        var online = getServer().getOnlinePlayers();
        if (online.isEmpty()) return;
        final List<com.observance.watcher.data.rows.PlayerRow> rows = new ArrayList<>(online.size());
        String now = SupabaseClient.timestampNow();
        for (var p : online) {
            rows.add(new com.observance.watcher.data.rows.PlayerRow(
                    p.getUniqueId().toString(), p.getName(), now));
        }
        scheduler.runAsyncSafe("presence.heartbeat.write", () -> {
            for (var row : rows) supabase.upsertPlayer(row);
        });
    }

    /* ==================================================================== */
    /*  Accessors for subsystem agents                                      */
    /* ==================================================================== */

    public Safety safety() { return safety; }
    public Scheduler scheduler() { return scheduler; }
    public ObservanceConfig config() { return config; }
    public SitesConfig sites() { return sites; }
    public SupabaseClient supabase() { return supabase; }
    public Reveal reveal() { return reveal; }
    public RateLimiter rateLimiter() { return rateLimiter; }

    /** The Signal Tracker (dossier). Downstream engines read {@code SignalSnapshot}s from it. */
    public SignalTracker signalTracker() { return signalTracker; }
    public TrackerConfig trackerConfig() { return trackerConfig; }

    /** The resource-pack load gate (MF-11). Rune-bearing beats query {@code isLoaded(player)} before
     *  rendering custom glyphs, falling back to ASCII (or skipping) when the client hasn't applied it. */
    public com.observance.watcher.signal.ResourcePackTracker resourcePack() { return resourcePack; }

    /** Subsystem agents (Haunting Engine) register their real enactor here. Null-safe. */
    public void setBeatEnactor(BeatEnactor enactor) {
        if (enactor != null) this.beatEnactor.set(enactor);
    }

    /** The Haunting Engine facade (beat library + drama budget + ambient generator). Nullable pre-enable. */
    public com.observance.watcher.beats.BeatEngine beatEngine() { return beatEngine; }

    public boolean isLocallyAsleep() { return locallyAsleep; }
    public void setLocallyAsleep(boolean asleep) {
        this.locallyAsleep = asleep;
        logEvent("info", "sleep", "local watcher-sleep=" + asleep, null);
    }

    public int placedSiteCount() { return sites == null ? 0 : sites.placedCount(); }

    /** Convenience for subsystems: async event_log write that never throws. */
    public void logEvent(String type, String context, String message, String mcUuid) {
        if (scheduler == null || supabase == null) return;
        scheduler.runAsyncSafe("event.log", () -> supabase.insertEventLog(new EventLogRow(
                type, context, message, mcUuid, null, SupabaseClient.timestampNow())));
    }
}
