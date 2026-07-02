package com.observance.watcher;

import com.observance.watcher.beat.BeatEnactor;
import com.observance.watcher.beat.BeatQueuePoller;
import com.observance.watcher.beat.NoopBeatEnactor;
import com.observance.watcher.command.ObservanceCommand;
import com.observance.watcher.config.ObservanceConfig;
import com.observance.watcher.config.Site;
import com.observance.watcher.config.SitesConfig;
import com.observance.watcher.data.SupabaseClient;
import com.observance.watcher.data.rows.EventLogRow;
import com.observance.watcher.listener.PresenceListener;
import com.observance.watcher.oracle.OracleResolver;
import com.observance.watcher.signal.InventoryScanner;
import com.observance.watcher.signal.LocationSampler;
import com.observance.watcher.signal.SignalTracker;
import com.observance.watcher.signal.TrackerConfig;
import com.observance.watcher.signal.listener.AnswerSignListener;
import com.observance.watcher.signal.listener.BlockBreakListener;
import com.observance.watcher.signal.listener.ChatListener;
import com.observance.watcher.signal.listener.CustomComplianceListener;
import com.observance.watcher.signal.listener.DarkHoursListener;
import com.observance.watcher.signal.listener.DeathListener;
import com.observance.watcher.signal.listener.IgnitionListener;
import com.observance.watcher.signal.listener.TerritoryListener;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Reveal;
import com.observance.watcher.util.Safety;
import com.observance.watcher.util.Scheduler;
import org.bukkit.configuration.ConfigurationSection;
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

    // --- the Lens ("second sight", INTEGRATION §SIGNATURE #3): registry of per-player gated displays
    //     + the listener that reveals/hides them as the player equips/holsters the relic. Created once
    //     and shared with the beat context so beats (e.g. reflection) can register gated runes. ---
    private com.observance.watcher.lens.LensRegistry lensRegistry;
    private com.observance.watcher.lens.LensListener lensListener;

    // --- oracle (the closed clue loop; in-world answer-sign verb) ---
    private OracleResolver oracleResolver;

    // --- the companion (Wren): the group-scoped in-world NPC + the reveal flag producer. The NPC body
    //     manager is created ONCE (survives a reload so the spawned Wren isn't orphaned); the watcher
    //     that flips companion_revealed off iss_caught is rebuilt per registration. ---
    private com.observance.watcher.npc.WrenNpc wren;
    private com.observance.watcher.npc.CompanionArcWatcher companionWatcher;
    /** The five surface townsfolk NPC body manager (Wave S-G). Created ONCE (survives a config reload). */
    private com.observance.watcher.npc.TownsfolkNpc townsfolk;

    // --- the asymmetric co-op vault (INTEGRATION signature #2): per-player fragment illusion + combination
    //     sign. Held so its per-player refresh loop can be started on enable and torn down on reload/disable
    //     (it spawns display entities that must be cleaned up). ---
    private com.observance.watcher.signal.listener.ThresholdVaultListener thresholdVault;

    // --- signal tracker (the dossier) ---
    private TrackerConfig trackerConfig;
    private SignalTracker signalTracker;
    private LocationSampler locationSampler;
    private InventoryScanner inventoryScanner;

    // --- OBSERVER TIER-0 (BUILD-PLAN §13): the behavior-only "it knows you" selector. Built from the
    //     config's tier0: block; consumed by the ComposureBeat. Rebuilt on reload. ---
    private volatile com.observance.watcher.tier0.Tier0Selector tier0Selector;

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

        // 5-lens. The Lens registry — created ONCE (survives a config reload so gated-display tracking
        //         isn't lost mid-session). The listener that flips them is (re)registered in
        //         registerListeners(); beats reach the registry via the shared BeatContext.
        if (this.lensRegistry == null) {
            this.lensRegistry = new com.observance.watcher.lens.LensRegistry();
        }

        // 5a. Oracle resolver — the shared world-surface answer path (built before listeners).
        this.oracleResolver = new OracleResolver(config, supabase, rateLimiter, safety);

        // 5a-wren. The companion (Wren). The NPC body manager is created ONCE — it holds the tracked
        //          spawned-entity id, so a config reload must not orphan a spawned Wren. The reveal
        //          watcher is (re)built each pass against the current client.
        if (this.wren == null) {
            this.wren = new com.observance.watcher.npc.WrenNpc("observance");
        }
        this.companionWatcher = new com.observance.watcher.npc.CompanionArcWatcher(supabase, safety);

        // 5a-townsfolk. The five surface townsfolk (Aro, Wenna, Coll, Dob, Old Pell). Created ONCE (like
        //               Wren) so a config reload doesn't orphan spawned bodies. Interactive via the
        //               TownsfolkNpcListener registered below; their SET-A lines are spoken in-world.
        if (this.townsfolk == null) {
            this.townsfolk = new com.observance.watcher.npc.TownsfolkNpc("observance");
        }

        // 5b. Signal tracker (the dossier) — pure tracking, no world effects.
        this.trackerConfig = TrackerConfig.from(getConfig());
        this.signalTracker = new SignalTracker(supabase, scheduler, safety,
                trackerConfig, config.heatmapCellSize());
        this.locationSampler = new LocationSampler(signalTracker, this::sites, rateLimiter, safety,
                config.locationSampleSeconds());
        this.inventoryScanner = new InventoryScanner(signalTracker, safety);

        // 5b-tier0. OBSERVER TIER-0 selector (behavior-only "it knows you"). Built from config; the
        //           ComposureBeat reads it via tier0Selector(). Fault-isolated to a default config.
        this.tier0Selector = buildTier0Selector();

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

        // Despawn the vault's per-player rune displays + stop its refresh loop (anti-orphan).
        if (thresholdVault != null) {
            thresholdVault.stop();
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
            // Rebuild the companion reveal watcher against the new client (the WrenNpc body manager is
            // preserved across reloads — created once in onEnable — so the spawned Wren is not orphaned).
            this.companionWatcher = new com.observance.watcher.npc.CompanionArcWatcher(supabase, safety);

            // Rebuild the signal tracker against the new client/config. Note: in-memory dossier
            // state for the current session is intentionally reset on a hard reload; persisted
            // dossiers in Supabase are the source of truth across restarts. (Phase 1 may rehydrate.)
            this.trackerConfig = TrackerConfig.from(getConfig());
            this.signalTracker = new SignalTracker(supabase, scheduler, safety,
                    trackerConfig, config.heatmapCellSize());
            this.locationSampler = new LocationSampler(signalTracker, this::sites, rateLimiter, safety,
                    config.locationSampleSeconds());
            this.inventoryScanner = new InventoryScanner(signalTracker, safety);
            // Rebuild the Tier-0 selector against the reloaded tier0: block.
            this.tier0Selector = buildTier0Selector();
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

        // The Lens (INTEGRATION §SIGNATURE #3) — reveals/hides gated per-player runes as the relic is
        // equipped/holstered. Rebuilt each registration so it points at the current shared registry;
        // the registry itself is preserved across reloads (created once in onEnable).
        if (lensRegistry != null) {
            this.lensListener = new com.observance.watcher.lens.LensListener(
                    this, lensRegistry, safety, "observance");
            pm.registerEvents(lensListener, this);
        }

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

        // Room-swap consumer (D5 rework): teleport a player who crosses a SEALED door (armed by
        // RoomSwapBeat) into the pre-built changed room. Reads only the durable swap_dest PDC the beat
        // wrote — no DB, no state held here. Inert until a swap is armed (no destination = no teleport).
        pm.registerEvents(new com.observance.watcher.signal.listener.RoomSwapReentryListener(
                rateLimiter, safety, "observance"), this);

        // Prologue ignition — in-world trigger that sets `prologue_ignited` (idempotent) when a player
        // reads the first-report lectern or right-clicks the rune_rosetta / stone_of_reckoning stone.
        // Keeps /observance flag as the admin stopgap; this wires the player-facing trigger. Registered
        // regardless of whether the sites are placed yet (unplaced sites are silently skipped in the
        // listener's contains() checks, so this is always safe to have registered).
        pm.registerEvents(new IgnitionListener(
                supabase, this::sites, rateLimiter, scheduler, safety), this);

        // The Companion (Wren) flag producers — a right-click on Wren advances companion_introduced +
        // companion_trust and signals the showrunner to speak his bound lines (Keeper delivery path);
        // a right-click on a reckoning marker enters ONE reckoning line, post-reveal, once. Registered
        // regardless of whether Wren is spawned yet (the listener is inert until a tagged body exists).
        pm.registerEvents(new com.observance.watcher.signal.listener.WrenNpcListener(
                supabase, wren, rateLimiter, scheduler, safety, "observance"), this);

        // The surface townsfolk (Wave S-G) — a right-click on one of the five townsfolk bodies speaks
        // that townsperson's authored SET-A lines to the clicking player, immediately and in-world
        // (greet first, then their rumor/truth/react lines cycled on repeat clicks). Ordinary human
        // register, no showrunner round-trip, touches no arc_state / flag graph / oracle. Inert until a
        // tagged townsfolk body exists (spawned via /observance townsfolk spawn).
        pm.registerEvents(new com.observance.watcher.signal.listener.TownsfolkNpcListener(
                townsfolk, rateLimiter, scheduler, safety), this);

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

        // The non-typed puzzle PRODUCERS (design/PUZZLE-DESIGNS.md §2–§6). Each detects an in-world
        // ACT and posts that puzzle's OPAQUE token to the same oracle (never typeable). All config-
        // driven; each degrades to a no-op when disabled or its token is blank. Read live so a reload
        // re-arms them. Sites resolved live via this::sites so a reload is picked up too.
        registerPuzzleProducers(pm, rites);
    }

    /** Wire the behavior/object/code/temporal producers for the non-typed puzzles. Split out to keep
     *  {@link #registerListeners()} readable; each block is independently gated + fault-isolated. */
    private void registerPuzzleProducers(org.bukkit.plugin.PluginManager pm,
                                         org.bukkit.configuration.file.FileConfiguration cfg) {
        // --- BEHAVIOR ---
        if (cfg.getBoolean("puzzles.orin-bow-fall-order.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.OrderedBowListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.orin-bow-fall-order.token", ""),
                    cfg.getString("puzzles.orin-bow-fall-order.puzzle-key", "orin-bow-fall-order")), this);
        }
        if (cfg.getBoolean("puzzles.mara-walk-the-map.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.GroupWalkListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.mara-walk-the-map.token", ""),
                    cfg.getString("puzzles.mara-walk-the-map.puzzle-key", "mara-walk-the-map"),
                    cfg.getInt("puzzles.mara-walk-the-map.quorum", 3),
                    cfg.getLong("puzzles.mara-walk-the-map.cooldown-seconds", 60L) * 1000L), this);
        }
        if (cfg.getBoolean("puzzles.sella-shore-memorial.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.ShoreMemorialListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.sella-shore-memorial.token", ""),
                    cfg.getString("puzzles.sella-shore-memorial.puzzle-key", "sella-shore-memorial"),
                    (float) cfg.getDouble("puzzles.sella-shore-memorial.look-down-min-pitch", 55.0)), this);
        }

        // --- OBJECT ---
        if (cfg.getBoolean("puzzles.vaun-hoard-sorted.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.HoardSortedListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.vaun-hoard-sorted.token", ""),
                    cfg.getString("puzzles.vaun-hoard-sorted.puzzle-key", "vaun-hoard-sorted"),
                    cfg.getStringList("puzzles.vaun-hoard-sorted.required-materials")), this);
        }

        // --- CODE / REDSTONE LOCKS ---
        if (cfg.getBoolean("puzzles.mara-lectern-lock.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.LecternLockListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.mara-lectern-lock.token", ""),
                    cfg.getString("puzzles.mara-lectern-lock.puzzle-key", "mara-lectern-lock"),
                    cfg.getIntegerList("puzzles.mara-lectern-lock.marked-pages")), this);
        }
        if (cfg.getBoolean("puzzles.vaun-bookshelf-tally.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.BookshelfTallyListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.vaun-bookshelf-tally.token", ""),
                    cfg.getString("puzzles.vaun-bookshelf-tally.puzzle-key", "vaun-bookshelf-tally"),
                    cfg.getIntegerList("puzzles.vaun-bookshelf-tally.filled-slots")), this);
        }
        if (cfg.getBoolean("puzzles.orin-frame-dials.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.FrameDialsListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.orin-frame-dials.token", ""),
                    cfg.getString("puzzles.orin-frame-dials.puzzle-key", "orin-frame-dials"),
                    cfg.getIntegerList("puzzles.orin-frame-dials.dial-rotations")), this);
        }

        // --- ASYMMETRIC CO-OP VAULT (§8.2, INTEGRATION signature #2) --------------
        // The social centerpiece: per-player fragment illusion + a combination sign at the
        // threshold_vault site. Tear down any prior instance first (reload rebuilds listeners), then
        // register + start its per-player refresh loop. The deep_gate_open reader is a live, fail-closed
        // arc-state read (the listener caches it so the event path never blocks). Sites live via this::sites.
        if (thresholdVault != null) { thresholdVault.stop(); thresholdVault = null; }
        if (cfg.getBoolean("puzzles.spine-threshold-vault.enabled", true)) {
            final SupabaseClient sbForGate = this.supabase;
            java.util.function.Supplier<Boolean> deepGateOpen = () -> {
                // Blocking arc-state read — the listener only calls this from its async gate-refresh.
                try {
                    var r = sbForGate.fetchArcState();
                    if (r == null || !r.ok() || r.value() == null) return Boolean.FALSE; // fail-closed
                    Object v = r.value().flagsMap().get("deep_gate_open");
                    return Boolean.valueOf(truthyFlag(v));
                } catch (Throwable t) {
                    return Boolean.FALSE; // any failure → gate stays closed (never leak runes early)
                }
            };
            this.thresholdVault = new com.observance.watcher.signal.listener.ThresholdVaultListener(
                    this, this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.spine-threshold-vault.token", ""),
                    cfg.getString("puzzles.spine-threshold-vault.puzzle-key", "spine-threshold-vault"),
                    splitCombination(cfg.getString("puzzles.spine-threshold-vault.combination", "")),
                    cfg.getInt("puzzles.spine-threshold-vault.quorum", 2),
                    null,               // active-roster supplier unwired → falls back to the online count
                    deepGateOpen);
            pm.registerEvents(thresholdVault, this);
            thresholdVault.start();   // MAIN thread (registerListeners runs synchronously) → safe to start the loop
        }

        // --- THE FINALE RITE — the Seventh's restore/erase choice (INTEGRATION-V2 A1, seventh-choice) ---
        // The reunion mechanic's producer: a right-click on a restore/erase choice marker at the
        // the_unwriting (seventh_shrine) wall sets the fate flags (seventh_named on restore) via
        // mergeArcFlags and posts the matching OPAQUE token to the oracle. Gated on deep_gate_open
        // (fail-CLOSED, read on the listener's async worker) so the finale can't fire before the deep opens.
        if (cfg.getBoolean("puzzles.seventh-choice.enabled", true)) {
            final SupabaseClient sbForFinale = this.supabase;
            java.util.function.Supplier<Boolean> deepGateOpenFinale = () -> {
                try {
                    var r = sbForFinale.fetchArcState();
                    if (r == null || !r.ok() || r.value() == null) return Boolean.FALSE; // fail-closed
                    Object v = r.value().flagsMap().get("deep_gate_open");
                    return Boolean.valueOf(truthyFlag(v));
                } catch (Throwable t) {
                    return Boolean.FALSE; // any failure → gate stays closed (never leak the reunion early)
                }
            };
            pm.registerEvents(new com.observance.watcher.signal.listener.SeventhChoiceListener(
                    supabase, this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    "observance", true,
                    cfg.getString("puzzles.seventh-choice.restore-token", ""),
                    cfg.getString("puzzles.seventh-choice.erase-token", ""),
                    cfg.getString("puzzles.seventh-choice.puzzle-key", "seventh-choice"),
                    deepGateOpenFinale), this);
        }

        // --- SILENCE / TEMPORAL ---
        if (cfg.getBoolean("puzzles.brann-silence-corridor.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.SilenceCorridorListener(
                    this::sites, oracleResolver, rateLimiter, scheduler, safety,
                    true,
                    cfg.getString("puzzles.brann-silence-corridor.token", ""),
                    cfg.getString("puzzles.brann-silence-corridor.puzzle-key", "brann-silence-corridor")), this);
        }
        if (cfg.getBoolean("puzzles.brann-black-moon-toll.enabled", true)) {
            pm.registerEvents(new com.observance.watcher.signal.listener.BlackMoonTollListener(
                    supabase, this::sites, rateLimiter, scheduler, safety,
                    cfg.getIntegerList("puzzles.brann-black-moon-toll.black-moon-phases")), this);
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

        // Companion reveal watcher — reads arc_state and flips companion_revealed once iss_caught (or
        // the kept-close artifact) is true. Pure DB I/O so ASYNC; slow cadence (reuses the maint rhythm).
        scheduledTasks.add(scheduler.runAsyncTimerSafe("companion.reveal", maintTicks, maintTicks, () -> {
            if (companionWatcher != null) companionWatcher.pollOnce();
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

    /** The Lens registry ("second sight"): beats register per-player gated displays here; the
     *  {@link com.observance.watcher.lens.LensListener} reveals/hides them on equip/holster. */
    public com.observance.watcher.lens.LensRegistry lensRegistry() { return lensRegistry; }

    /** The live Lens listener (for a beat to {@code refresh} a player right after registering a rune). */
    public com.observance.watcher.lens.LensListener lensListener() { return lensListener; }

    /** The Signal Tracker (dossier). Downstream engines read {@code SignalSnapshot}s from it. */
    public SignalTracker signalTracker() { return signalTracker; }
    public TrackerConfig trackerConfig() { return trackerConfig; }

    /** OBSERVER TIER-0 selector — the behavior-only "it knows you" derivation the ComposureBeat reads.
     *  Rebuilt on reload from the config {@code tier0:} block; never null after onEnable. */
    public com.observance.watcher.tier0.Tier0Selector tier0Selector() { return tier0Selector; }

    /** The resource-pack load gate (MF-11). Rune-bearing beats query {@code isLoaded(player)} before
     *  rendering custom glyphs, falling back to ASCII (or skipping) when the client hasn't applied it. */
    public com.observance.watcher.signal.ResourcePackTracker resourcePack() { return resourcePack; }

    /** Subsystem agents (Haunting Engine) register their real enactor here. Null-safe. */
    public void setBeatEnactor(BeatEnactor enactor) {
        if (enactor != null) this.beatEnactor.set(enactor);
    }

    /** The Haunting Engine facade (beat library + drama budget + ambient generator). Nullable pre-enable. */
    public com.observance.watcher.beats.BeatEngine beatEngine() { return beatEngine; }

    /** The companion (Wren) NPC body manager — spawn/despawn/tag the one group-scoped Wren. */
    public com.observance.watcher.npc.WrenNpc wren() { return wren; }

    public com.observance.watcher.npc.TownsfolkNpc townsfolk() { return townsfolk; }

    public boolean isLocallyAsleep() { return locallyAsleep; }
    public void setLocallyAsleep(boolean asleep) {
        this.locallyAsleep = asleep;
        logEvent("info", "sleep", "local watcher-sleep=" + asleep, null);
    }

    public int placedSiteCount() { return sites == null ? 0 : sites.placedCount(); }

    /**
     * Register a site into the LIVE {@link SitesConfig} at runtime (used by the {@code /observance
     * placeroom} and {@code /observance placeregion} admin commands). Swaps the {@code sites} field for
     * an immutable copy that includes the new site; because all listeners read sites via the
     * {@code this::sites} supplier, the addition is picked up immediately (e.g. {@code AnswerSignListener}
     * will resolve answers at a freshly placed keeper stone). MAIN thread only. Null-safe.
     *
     * <p>This method ALSO persists the site to {@code plugins/Observance/sites.yml} so it survives a
     * reload or restart. If the write fails it logs a warning but still registers the runtime site
     * (the in-session placement is never rolled back over a failed disk write).
     */
    public void registerRuntimeSite(Site site) {
        if (site == null || sites == null) return;
        this.sites = sites.withSite(site);
        persistSiteToYml(site);
    }

    /**
     * Write (or overwrite) a {@link Site}'s coordinates into the live
     * {@code plugins/Observance/sites.yml} file using the Bukkit {@link YamlConfiguration} API.
     * Loads the current file, sets the site's section, and saves — so a restart or
     * {@code /observance reload} will pick up the entry. Null-safe; never throws into callers.
     *
     * <p>MAIN thread only (called from the command handler, which runs synchronously).
     */
    public void persistSiteToYml(Site site) {
        if (site == null) return;
        try {
            File file = new File(getDataFolder(), "sites.yml");
            if (!file.exists()) {
                saveResource("sites.yml", false);
            }
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            String path = "sites." + site.id();
            ConfigurationSection sec = cfg.getConfigurationSection(path);
            if (sec == null) sec = cfg.createSection(path);

            sec.set("type",           site.type());
            sec.set("world",          site.worldName());
            // Write coords as plain doubles — null would be treated as unplaced (placeholder). Since we
            // are persisting a freshly placed site, coords are always non-null here.
            if (site.location() != null) {
                sec.set("x", site.location().getX());
                sec.set("y", site.location().getY());
                sec.set("z", site.location().getZ());
            }
            sec.set("radius",           site.radius());
            sec.set("vertical-radius",  site.verticalRadius());
            sec.set("protect",          site.protect());
            sec.set("enabled",          site.enabled());
            if (site.puzzleKey() != null) sec.set("puzzle-key", site.puzzleKey());

            cfg.save(file);
        } catch (Throwable t) {
            getLogger().warning("[Observance] Could not persist site '" + site.id()
                    + "' to sites.yml: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    /** Build the OBSERVER TIER-0 selector from the live config's {@code tier0:} block. Fault-isolated:
     *  any parse failure degrades to a defaults-backed selector so Tier-0 always has a working (if
     *  default-tuned) selector rather than a null. */
    private com.observance.watcher.tier0.Tier0Selector buildTier0Selector() {
        com.observance.watcher.tier0.Tier0Config cfg = safety.call("tier0.config",
                () -> com.observance.watcher.tier0.Tier0ConfigLoader.from(getConfig()),
                com.observance.watcher.tier0.Tier0Config.defaults());
        if (cfg == null) cfg = com.observance.watcher.tier0.Tier0Config.defaults();
        return new com.observance.watcher.tier0.Tier0Selector(cfg);
    }

    /** Split a space-separated combination string into fragment groups (trim, drop blanks). Null-safe. */
    private static java.util.List<String> splitCombination(String combination) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (combination == null) return out;
        for (String g : combination.trim().split("\\s+")) {
            if (g != null && !g.isBlank()) out.add(g.trim());
        }
        return out;
    }

    /** Truthy test for an arc_state flag value (Boolean true, or "true"/"1"/"yes"). Mirrors the bot/gate. */
    private static boolean truthyFlag(Object v) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0.0;
        if (v instanceof String s) {
            String t = s.trim().toLowerCase(java.util.Locale.ROOT);
            return t.equals("true") || t.equals("1") || t.equals("yes");
        }
        return false;
    }

    /** Convenience for subsystems: async event_log write that never throws. */
    public void logEvent(String type, String context, String message, String mcUuid) {
        if (scheduler == null || supabase == null) return;
        scheduler.runAsyncSafe("event.log", () -> supabase.insertEventLog(new EventLogRow(
                type, context, message, mcUuid, null, SupabaseClient.timestampNow())));
    }
}
