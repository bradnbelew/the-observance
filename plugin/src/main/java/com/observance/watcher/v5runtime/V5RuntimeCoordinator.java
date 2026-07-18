package com.observance.watcher.v5runtime;

import com.google.gson.JsonObject;
import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.npc.V5DialogueCatalog;
import com.observance.watcher.structure.V5RuntimePredicateRegistry;
import com.observance.watcher.v5runtime.container.BukkitContainerCustody;
import com.observance.watcher.v5runtime.container.BukkitContainerListener;
import com.observance.watcher.v5runtime.container.BukkitContainerTriggerAudit;
import com.observance.watcher.v5runtime.container.BukkitContainerWorld;
import com.observance.watcher.v5runtime.container.ContainerAuthorityContract;
import com.observance.watcher.v5runtime.container.ContainerSolveService;
import com.observance.watcher.v5runtime.container.ContainerTriggerBindings;
import com.observance.watcher.v5runtime.container.ContainerTriggerChunkPolicy;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentCatalog;
import com.observance.watcher.v5runtime.install.V5PhysicalComponentInstaller;
import com.observance.watcher.v5runtime.mechanics.BukkitDurableItemEscrow;
import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex;
import com.observance.watcher.v5runtime.mechanics.BukkitMechanicState;
import com.observance.watcher.v5runtime.mechanics.BukkitReceiptService;
import com.observance.watcher.v5runtime.mechanics.BukkitWorldStateEvaluator;
import com.observance.watcher.v5runtime.mechanics.V5MechanicsEngine;
import com.observance.watcher.v5runtime.mechanics.V5PhysicalMechanicsListener;
import com.observance.watcher.v5runtime.ritual.BallotEvidenceJournal;
import com.observance.watcher.v5runtime.ritual.BukkitFinaleEffects;
import com.observance.watcher.v5runtime.ritual.CanonicalRitualText;
import com.observance.watcher.v5runtime.ritual.CollectivePresenceRite;
import com.observance.watcher.v5runtime.ritual.FinaleBukkitArmExpiry;
import com.observance.watcher.v5runtime.ritual.FinaleBukkitPhaseRunner;
import com.observance.watcher.v5runtime.ritual.FinaleRite;
import com.observance.watcher.v5runtime.ritual.FinaleStateStore;
import com.observance.watcher.v5runtime.ritual.RitualAuthorityContract;
import com.observance.watcher.v5runtime.ritual.RitualChoices;
import com.observance.watcher.v5runtime.ritual.RitualClock;
import com.observance.watcher.v5runtime.ritual.V5RitualWorldController;
import com.observance.watcher.v5runtime.ritual.VisibleBallotRite;
import com.observance.watcher.v5runtime.ritual.WrenDialogueBukkitListener;
import com.observance.watcher.v5runtime.ritual.WrenDialogueRite;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.Lightable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 * Sole lifecycle owner for the production V5 physical runtime.
 *
 * <p>All sixty predicates share one exact authority and one forced local progress store. The
 * network is a validated, monotonic mirror only; it can neither authorize a physical solve nor
 * close a locally opened route.</p>
 */
public final class V5RuntimeCoordinator implements Listener, AutoCloseable {
    private static final String P2_HANDOFF_EVENT = "p2.live_runtime_handoff";
    private static final String P3_ACCOUNTS_EVENT = "p3.resident_accounts_opened";
    private static final String P5_CURATION_EVENT = "p5.civic_gallery_recurated";
    private static final String P6_MODELS_EVENT = "p6.professional_models_recovered";
    private static final String P6_RESPONSIBILITY_EVENT = "p6.six_responsibilities_acknowledged";
    private static final String P7_MATERIAL_EVENT = "p7.counterfeit_material_proven";
    private static final String P8_PLAN_EVENT = "p8.intervention_plan_accepted";
    private static final String P8_UNLIT_EVENT = "p8.unlit_house_synthesis_completed";
    private static final String P8_REPAIR_EVENT = "p8.hold_systems_repaired";
    private static final String P9_BIOGRAPHIES_EVENT = "p9.company_biographies_restored";
    private static final String P9_LEAK_EVENT = "p9.leak_window_proven";
    private static final String P10_CONFRONTED_EVENT = "p10.wren_confronted";
    private static final String P10_COPY_PROOF_EVENT = "p10.player_copy_proof";
    private static final String P10_REMEMBRANCE_EVENT = "p10.wren_remembrance_committed";
    private static final String P11_IDENTIFIED_EVENT = "p11.averyn_identified";
    private static final String P11_UNBOUND_EVENT = "p11.averyn_restored_unbound";
    private static final String P12_CONFIGURATION_EVENT = "p12.release_configuration_ready";
    private static final String P12_NAME_EVENT = "p12.name_treatment_committed";
    private static final String P12_RELEASE_EVENT = "p12.record_closed_averyn_released";
    private static final List<String> P6_PROFESSIONAL_PROOFS = List.of(
            "v5_kv03_affidavit", "v5_km03_affidavit", "v5_ks03_affidavit",
            "v5_ko03_affidavit", "v5_kb03_affidavit", "v5_ki03_affidavit");
    private static final List<String> P8_UNLIT_HOUSE_PROOFS = List.of(
            "v5_bi01_lamp", "v5_bi02_cairn", "v5_bi03_coop", "v5_bi04_well",
            "v5_bi05_watch", "v5_bi06_warm", "v5_bi07_threshold");
    private final ObservancePlugin plugin;
    private final PhysicalPredicateAuthority authority;
    private final V5ProgressStore progress;
    private final V5PhysicalComponentInstaller installer;
    private final BukkitFixtureIndex fixtures;
    private final BukkitContainerCustody custody;
    private final V5RemoteStateCache remote;
    private final P5CurationRuntime p5Curation;

    private final V5MechanicsEngine mechanics;
    private final V5PhysicalMechanicsListener mechanicsListener;
    private final ContainerSolveService containers;
    private final BukkitContainerListener containerListener;

    private final FinaleRite finale;
    private final V5RitualWorldController ritualController;
    private final WrenDialogueBukkitListener wrenDialogueListener;
    private final boolean storyInputsEnabled;

    private volatile V5PhysicalComponentInstaller.Report lastBindingReport;
    private volatile V5PhysicalComponentInstaller.Report lastMapReport;
    private BukkitTask deferredRebind;
    private boolean started;
    private boolean closed;

    public V5RuntimeCoordinator(ObservancePlugin plugin) throws IOException {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.authority = PhysicalPredicateAuthorityLoader.loadDefault();
        PredicateCoverageCatalog.validateAgainst(authority);

        Path base = plugin.getDataFolder().toPath().resolve("v5");
        Files.createDirectories(base);
        this.progress = V5ProgressStore.open(base.resolve("progress.json"), authority);
        this.installer = new V5PhysicalComponentInstaller(plugin);
        if (!installer.catalog().authoritySha256().equals(authority.sha256())) {
            throw new IllegalStateException("V5 installer and predicate authorities differ");
        }
        this.fixtures = new BukkitFixtureIndex(plugin);
        this.custody = new BukkitContainerCustody(plugin, base.resolve("container-custody"));
        this.remote = new V5RemoteStateCache(plugin, authority, progress);
        this.p5Curation = new P5CurationRuntime(plugin, progress, this::onP5CurationChanged);

        RitualAuthorityContract ritualAuthority = new RitualAuthorityContract(authority);
        CanonicalRitualText ritualText = new CanonicalRitualText(ritualAuthority);
        LeaseBook leases = new LeaseBook();
        SiteMutexes mutexes = new SiteMutexes();
        RitualClock clock = RitualClock.system();
        BallotEvidenceJournal ballotJournal = BallotEvidenceJournal.open(
                base.resolve("ballots.json"), authority.sha256());
        VisibleBallotRite ballots = new VisibleBallotRite(
                ritualAuthority, progress, ballotJournal, leases, mutexes, clock);
        CollectivePresenceRite presence = new CollectivePresenceRite(
                ritualAuthority, progress, leases, mutexes, clock,
                (operator, from, to, reason) -> plugin.logEvent(
                        "warning", "v5.accessibility",
                        operator + " replaced RP04 sector " + from + " with " + to
                                + ": " + reason,
                        null));
        FinaleStateStore finaleState = FinaleStateStore.open(
                base.resolve("finale-state.json"), authority.sha256());
        this.finale = new FinaleRite(
                ritualAuthority, ritualText, progress, finaleState, leases, mutexes, clock);
        BukkitFinaleEffects finaleEffects = new BukkitFinaleEffects(
                plugin, new ExactFinaleTheater(),
                plugin.getConfig().getBoolean("finale.production-shutdown", true),
                this::projectCodaBeforeSave);
        FinaleBukkitPhaseRunner phaseRunner = new FinaleBukkitPhaseRunner(
                plugin, finale, finaleEffects);
        FinaleBukkitArmExpiry armExpiry = new FinaleBukkitArmExpiry(plugin, finale);
        this.ritualController = new V5RitualWorldController(
                plugin, progress, remote, fixtures, custody, ballots, presence,
                finale, phaseRunner, armExpiry);
        WrenDialogueRite dialogue = new WrenDialogueRite(
                ritualAuthority, ritualText, progress, leases, mutexes, clock);
        this.wrenDialogueListener = new WrenDialogueBukkitListener(
                plugin, dialogue, ritualText);

        FinaleStateStore.Phase phase = finale.snapshot().phase();
        this.storyInputsEnabled = phase == FinaleStateStore.Phase.IDLE
                || phase == FinaleStateStore.Phase.ARMED;

        if (storyInputsEnabled) {
            // One feedback port for both the mechanics engine and the container service: wrong input
            // → red actionbar (send); a completed node → a witnessed-solve cue (solved).
            com.observance.watcher.v5runtime.mechanics.MechanicPorts.PlayerFeedback playerFeedback =
                    new com.observance.watcher.v5runtime.mechanics.MechanicPorts.PlayerFeedback() {
                        @Override public void send(UUID actor, String message) { feedback(actor, message); }
                        @Override public void solved(UUID actor) { feedbackSolved(actor); }
                    };
            BukkitMechanicState live = new BukkitMechanicState();
            BukkitWorldStateEvaluator worldState = new BukkitWorldStateEvaluator(
                    plugin, fixtures, live);
            V5BukkitProjection projection = new V5BukkitProjection(
                    plugin, progress, fixtures, custody);
            this.mechanics = new V5MechanicsEngine(
                    authority, progress, remote, worldState, projection, projection,
                    remote::mirrorAsync, playerFeedback);
            BukkitDurableItemEscrow itemEscrow = new BukkitDurableItemEscrow(
                    plugin, base.resolve("mechanics-escrow"));
            BukkitReceiptService receipts = new BukkitReceiptService(
                    plugin, itemEscrow, progress, authority);
            this.mechanicsListener = new V5PhysicalMechanicsListener(
                    plugin, authority, mechanics, fixtures, live, receipts, itemEscrow);

            ContainerAuthorityContract containerAuthority = new ContainerAuthorityContract(authority);
            BukkitContainerWorld containerWorld = new BukkitContainerWorld(
                    plugin, fixtures, containerAuthority, custody);
            this.containers = new ContainerSolveService(
                    containerAuthority, progress, new SiteMutexes(), containerWorld,
                    remote, remote, rule -> projectLocalState(),
                    (rule, revision) -> remote.mirrorAsync(
                            authority.requireNode(rule.nodeId()), revision),
                    this::feedback, System::currentTimeMillis);
            this.containerListener = new BukkitContainerListener(
                    plugin, containers, fixtures, containerWorld);
        } else {
            this.mechanics = null;
            this.mechanicsListener = null;
            this.containers = null;
            this.containerListener = null;
        }
    }

    /** Must be called after the plugin has published this coordinator through its accessor. */
    public void start() {
        requireMainThread();
        if (started || closed) throw new IllegalStateException("V5 runtime cannot be started twice");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        upgradeLoadedWrenBodies();
        rebindAllLoadedFixtures();

        if (storyInputsEnabled) {
            var manager = plugin.getServer().getPluginManager();
            manager.registerEvents(mechanicsListener, plugin);
            manager.registerEvents(containerListener, plugin);
            manager.registerEvents(wrenDialogueListener, plugin);
            manager.registerEvents(ritualController, plugin);
            mechanicsListener.start();
            ritualController.start();
            p5Curation.start();

            // This is deliberately last: an active registry certifies that every exact family is live.
            V5RuntimePredicateRegistry.activate(authority);
            remote.start();
            started = true;
            recoverAndProject();
        } else if (finale.snapshot().phase() != FinaleStateStore.Phase.CODA) {
            // A crash during RP06 resumes only the durable closing runner, never ordinary inputs.
            ritualController.start();
            started = true;
        } else {
            started = true;
            plugin.getLogger().warning(
                    "Observance V5 is in durable CODA; all ordinary story inputs remain disabled.");
        }
    }

    public boolean isCoda() {
        return finale.snapshot().phase() == FinaleStateStore.Phase.CODA;
    }

    public boolean storyInputsEnabled() {
        return storyInputsEnabled;
    }

    public String authoritySha256() {
        return authority.sha256();
    }

    public ProgressSnapshot snapshot() {
        return progress.snapshot();
    }

    public enum PlanSubmission { ACCEPTED, ALREADY_ACCEPTED, NOT_READY, WRONG, FAILED }

    public boolean p8InterventionPlanAccepted() {
        return progress.snapshot().isComplete(P8_PLAN_EVENT);
    }

    /** Stable local fallback for the Copperline P8 form; raw player prose is never stored or mirrored. */
    public PlanSubmission submitP8InterventionPlan(P8InterventionPlanPredicate.Plan plan) {
        if (!storyInputsEnabled) return PlanSubmission.FAILED;
        ProgressSnapshot before = progress.snapshot();
        if (before.isComplete(P8_PLAN_EVENT)) return PlanSubmission.ALREADY_ACCEPTED;
        if (!before.isComplete("p7.nessa_publicly_cleared")) return PlanSubmission.NOT_READY;
        if (!P8InterventionPlanPredicate.valid(plan)) return PlanSubmission.WRONG;
        try {
            boolean created = progress.transact(editor -> editor.setBooleanTrue(P8_PLAN_EVENT));
            if (!created) return PlanSubmission.ALREADY_ACCEPTED;
            projectLocalState();
            mirrorP8PlanAsync();
            return PlanSubmission.ACCEPTED;
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("P8 intervention plan could not be committed locally: "
                    + failure.getMessage());
            return PlanSubmission.FAILED;
        }
    }

    /**
     * Commits the privacy-bounded surface-to-Unlit copy proof. The journal owning the physical
     * pattern contains only six allowlisted token ids; this campaign event projects its hash only.
     */
    public void commitUnlitCopyProof(String patternSha256) {
        if (!storyInputsEnabled || patternSha256 == null
                || !patternSha256.matches("[0-9a-f]{64}")) return;
        try {
            boolean created = progress.transact(editor -> editor.setBooleanTrue(P10_COPY_PROOF_EVENT));
            if (created) {
                projectLocalState();
                plugin.scheduler().runAsyncSafe("arg.p10.copy-proof.mirror", () -> {
                    if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
                    JsonObject copy = new JsonObject();
                    copy.addProperty("pattern_sha256", patternSha256);
                    copy.addProperty("cell_count", 6);
                    copy.addProperty("allowlisted_tokens_only", true);
                    copy.addProperty("personalized_input", false);
                    plugin.supabase().recordArgEvent(P10_COPY_PROOF_EVENT,
                            "minecraft:p10:bounded-unlit-copy", "minecraft", null, copy);
                });
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("P10 bounded Unlit copy proof could not be committed locally: "
                    + failure.getMessage());
        }
    }

    /** Rebuild exact coordinate/PDC bindings after construction, repair, startup, or chunk load. */
    public void rebindAllLoadedFixtures() {
        if (!Bukkit.isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin, this::rebindAllLoadedFixtures);
            return;
        }
        if (closed) return;
        retainCriticalContainerControlChunks();
        fixtures.clear();
        Location mouth = plugin.v5HoldMouth();
        lastBindingReport = installer.bindLoadedFixtures(fixtures, mouth);
        lastMapReport = installer.rebindLoadedMapViews(mouth);
        p5Curation.project();
        if (lastBindingReport.clean() && storyInputsEnabled && started) {
            recoverAndProject();
        }
    }

    /** Local monotonic facts project immediately; the external mirror is enqueue-only. */
    public void projectLocalState() {
        if (!Bukkit.isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin, this::projectLocalState);
            return;
        }
        if (closed) return;
        advanceDerivedStoryEvents();
        plugin.projectV5LocalState(progress.snapshot());
        remote.mirrorLocalSnapshotAsync();
    }

    private void onP5CurationChanged() {
        projectLocalState();
        if (progress.snapshot().isComplete(P5_CURATION_EVENT)) mirrorP5CurationAsync();
    }

    public P5CurationRuntime.Audit p5CurationAudit() {
        return p5Curation.audit();
    }

    /** Six profession-specific completions cause one biography consequence; no prose restatement is required. */
    private void advanceDerivedStoryEvents() {
        ProgressSnapshot snapshot = progress.snapshot();
        if (snapshot.isComplete(P5_CURATION_EVENT)
                && P6_PROFESSIONAL_PROOFS.stream().allMatch(snapshot::isComplete)) {
            try {
                boolean created = progress.transact(editor -> {
                    boolean models = editor.setBooleanTrue(P6_MODELS_EVENT);
                    boolean responsibility = editor.setBooleanTrue(P6_RESPONSIBILITY_EVENT);
                    return models || responsibility;
                });
                if (created) mirrorP6MilestonesAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P6 biography milestones could not be committed locally: " + failure.getMessage());
                return;
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P6_RESPONSIBILITY_EVENT)
                && snapshot.isComplete("v5_cw05_counterfeit")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P7_MATERIAL_EVENT));
                if (created) mirrorP7MaterialAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P7 material finding could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (P8_UNLIT_HOUSE_PROOFS.stream().allMatch(snapshot::isComplete)) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P8_UNLIT_EVENT));
                if (created) mirrorP8UnlitAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P8 seven-house Unlit synthesis could not be committed locally: "
                        + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P8_PLAN_EVENT)
                && snapshot.isComplete(P8_UNLIT_EVENT)
                && snapshot.isComplete("v5_case_c06_complete")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P8_REPAIR_EVENT));
                if (created) mirrorP8RepairAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P8 integrated repair could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P8_REPAIR_EVENT)
                && snapshot.isComplete("v5_a02_stations")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P9_BIOGRAPHIES_EVENT));
                if (created) mirrorP9BiographiesAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P9 company biographies could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P9_BIOGRAPHIES_EVENT)
                && snapshot.isComplete("v5_case_c07_complete")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P9_LEAK_EVENT));
                if (created) mirrorP9LeakAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P9 private leak window could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P9_LEAK_EVENT)
                && snapshot.isComplete("v5_wr03_confession")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P10_CONFRONTED_EVENT));
                if (created) mirrorP10ConfrontedAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P10 Wren finding could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P10_CONFRONTED_EVENT)
                && snapshot.isComplete("v5_case_c08_complete")
                && snapshot.branches().containsKey("v5_wren_outcome")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P10_REMEMBRANCE_EVENT));
                if (created) mirrorP10RemembranceAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P10 remembrance could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P10_REMEMBRANCE_EVENT)
                && snapshot.isComplete("v5_case_c09_complete")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P11_IDENTIFIED_EVENT));
                if (created) mirrorP11IdentityAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P11 Averyn identity could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P11_IDENTIFIED_EVENT)
                && snapshot.isComplete("v5_rp02_configured")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P11_UNBOUND_EVENT));
                if (created) mirrorP11UnboundAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P11 unbound relationship could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P11_UNBOUND_EVENT)
                && snapshot.isComplete("v5_rp02_configured")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P12_CONFIGURATION_EVENT));
                if (created) mirrorP12ConfigurationAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P12 release configuration could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P12_CONFIGURATION_EVENT)
                && snapshot.isComplete("v5_rp03_name_choice")
                && snapshot.branches().containsKey("v5_name_treatment")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P12_NAME_EVENT));
                if (created) mirrorP12NameAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P12 name treatment could not be committed locally: " + failure.getMessage());
            }
        }
        snapshot = progress.snapshot();
        if (snapshot.isComplete(P12_NAME_EVENT)
                && snapshot.isComplete("v5_case_c10_complete")) {
            try {
                boolean created = progress.transact(editor -> editor.setBooleanTrue(P12_RELEASE_EVENT));
                if (created) mirrorP12ReleaseAsync();
            } catch (IOException | RuntimeException failure) {
                plugin.getLogger().severe("P12 release event could not be committed locally: " + failure.getMessage());
            }
        }
    }

    /** Exact idempotency keys make startup/restart retries safe after a remote outage. */
    private void mirrorP5CurationAsync() {
        plugin.scheduler().runAsyncSafe("arg.p5.curation.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject chronology = new JsonObject();
            chronology.addProperty("service_cards", "public");
            chronology.addProperty("penalty_copies", "evidence-custody");
            chronology.addProperty("observation_receipts", 0);
            plugin.supabase().recordArgEvent(P5CurationRuntime.CHRONOLOGY_EVENT,
                    "minecraft:p5:service-chronology-shared", "minecraft", null, chronology);
            JsonObject curation = new JsonObject();
            curation.addProperty("gallery", "recurated");
            curation.addProperty("old_caption_preserved", true);
            curation.addProperty("runtime_exact_phrase", false);
            plugin.supabase().recordArgEvent(P5_CURATION_EVENT,
                    "minecraft:p5:civic-gallery-recurated", "minecraft", null, curation);
        });
    }

    private void mirrorP6MilestonesAsync() {
        plugin.scheduler().runAsyncSafe("arg.p6.milestones.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject models = new JsonObject();
            models.addProperty("people", "Vaun,Mara,Sella,Orin,Brann,Iss");
            models.addProperty("basis", "six-distinct-professional-proofs");
            plugin.supabase().recordArgEvent(P6_MODELS_EVENT,
                    "minecraft:p6:professional-models-recovered", "minecraft", null, models);
            JsonObject responsibility = new JsonObject();
            responsibility.addProperty("matrix", "six-distinct-people-and-culpabilities");
            responsibility.addProperty("runtime_exact_phrase", false);
            plugin.supabase().recordArgEvent(P6_RESPONSIBILITY_EVENT,
                    "minecraft:p6:six-responsibilities-acknowledged", "minecraft", null, responsibility);
        });
    }

    private void mirrorP7MaterialAsync() {
        plugin.scheduler().runAsyncSafe("arg.p7.material.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject material = new JsonObject();
            material.addProperty("failure", "counterfeit-single-warp-cloth");
            material.addProperty("location", "upstream-lower-intake");
            material.addProperty("genuine_stock", "diverted");
            plugin.supabase().recordArgEvent(P7_MATERIAL_EVENT,
                    "minecraft:p7:counterfeit-material-proven", "minecraft", null, material);
        });
    }

    private void mirrorP8PlanAsync() {
        plugin.scheduler().runAsyncSafe("arg.p8.plan.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject plan = new JsonObject();
            plan.addProperty("finding_shape", "causes-iss-copy-boundary-order-v1");
            plan.addProperty("causes", "old-fracture,unchanged-heat-load,paired-watch-gap,late-routing");
            plan.addProperty("iss", "surface-proof-valid-route-unsafe");
            plan.addProperty("copy_boundary", "copy-behavior-proven-ontology-open");
            plan.addProperty("works_order", "water-filter,paired-light,pressure-bypass,staff-route");
            plan.addProperty("observation_receipts", 0);
            plugin.supabase().recordArgEvent(P8_PLAN_EVENT,
                    "minecraft:p8:intervention-plan", "minecraft", null, plan);
        });
    }

    private void mirrorP8RepairAsync() {
        plugin.scheduler().runAsyncSafe("arg.p8.repair.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject repair = new JsonObject();
            repair.addProperty("water", "verified-filter-installed");
            repair.addProperty("light", "paired-watch-circuit-restored");
            repair.addProperty("pressure", "bypass-closed");
            repair.addProperty("route", "staff-passage-opened-last");
            repair.addProperty("altered_copy", "preserved-not-erased");
            plugin.supabase().recordArgEvent(P8_REPAIR_EVENT,
                    "minecraft:p8:hold-systems-repaired", "minecraft", null, repair);
        });
    }

    private void mirrorP8UnlitAsync() {
        plugin.scheduler().runAsyncSafe("arg.p8.unlit-synthesis.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject synthesis = new JsonObject();
            synthesis.addProperty("houses", 7);
            synthesis.addProperty("base_comparison", "complete");
            synthesis.addProperty("observation_receipts_gate_report", false);
            synthesis.addProperty("any_subset", true);
            plugin.supabase().recordArgEvent(P8_UNLIT_EVENT,
                    "minecraft:p8:seven-unlit-houses-and-base", "minecraft", null, synthesis);
        });
    }

    private void mirrorP9BiographiesAsync() {
        plugin.scheduler().runAsyncSafe("arg.p9.biographies.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject people = new JsonObject();
            people.addProperty("mkept", "admin-custody");
            people.addProperty("ash", "camera-humor");
            people.addProperty("rook", "builder-countermark");
            people.addProperty("wren", "route-companion");
            people.addProperty("restored_as", "people-not-stations");
            plugin.supabase().recordArgEvent(P9_BIOGRAPHIES_EVENT,
                    "minecraft:p9:ash-camp-owner-cards", "minecraft", null, people);
        });
    }

    private void mirrorP9LeakAsync() {
        plugin.scheduler().runAsyncSafe("arg.p9.leak.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject leak = new JsonObject();
            leak.addProperty("readiness", "release-ready");
            leak.addProperty("private_object", "rook-revision-and-identities");
            leak.addProperty("window", "private-before-public");
            leak.addProperty("boundary", "insider-unknown");
            plugin.supabase().recordArgEvent(P9_LEAK_EVENT,
                    "minecraft:p9:private-revision-window", "minecraft", null, leak);
        });
    }

    private void mirrorP10ConfrontedAsync() {
        plugin.scheduler().runAsyncSafe("arg.p10.confronted.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject finding = new JsonObject();
            finding.addProperty("sender", "wren");
            finding.addProperty("packet_payload", "names-plans-routes-fears");
            finding.addProperty("proof", "progressive-private-missing-countermark");
            finding.addProperty("motive", "fear-explains-choice-responsibility-remains");
            plugin.supabase().recordArgEvent(P10_CONFRONTED_EVENT,
                    "minecraft:p10:wren-transmission-finding", "minecraft", null, finding);
        });
    }

    private void mirrorP10RemembranceAsync() {
        plugin.scheduler().runAsyncSafe("arg.p10.remembrance.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            String outcome = progress.snapshot().branches().get("v5_wren_outcome");
            if (outcome == null || outcome.isBlank()) return;
            JsonObject remembrance = new JsonObject();
            remembrance.addProperty("treatment", outcome);
            remembrance.addProperty("facts_fixed", true);
            remembrance.addProperty("protocol_bridge_branch_independent", true);
            plugin.supabase().recordArgEvent(P10_REMEMBRANCE_EVENT,
                    "minecraft:p10:wren-remembrance", "minecraft", null, remembrance);
        });
    }

    private void mirrorP11IdentityAsync() {
        plugin.scheduler().runAsyncSafe("arg.p11.identity.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject identity = new JsonObject();
            identity.addProperty("name", "AVERYN");
            identity.addProperty("provenance", "six-distinct-affidavit-paths");
            identity.addProperty("exact_artifact", true);
            plugin.supabase().recordArgEvent(P11_IDENTIFIED_EVENT,
                    "minecraft:p11:averyn-six-affidavit-identity", "minecraft", null, identity);
        });
    }

    private void mirrorP11UnboundAsync() {
        plugin.scheduler().runAsyncSafe("arg.p11.unbound.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject relationship = new JsonObject();
            relationship.addProperty("averyn", "human-registrar-analyst");
            relationship.addProperty("record", "civic-system-trapped-her");
            relationship.addProperty("watcher", "constrained-record-voice");
            relationship.addProperty("dark", "related-distinct-unknown");
            relationship.addProperty("averyn_record_socket", "empty-unbound");
            relationship.addProperty("observation_receipts", 0);
            plugin.supabase().recordArgEvent(P11_UNBOUND_EVENT,
                    "minecraft:p11:averyn-relationship-unbound", "minecraft", null, relationship);
        });
    }

    private void mirrorP12ConfigurationAsync() {
        plugin.scheduler().runAsyncSafe("arg.p12.configuration.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject configuration = new JsonObject();
            configuration.addProperty("affidavits_returned", 6);
            configuration.addProperty("material_proofs_installed", 3);
            configuration.addProperty("averyn_record_socket", "empty-unbound");
            configuration.addProperty("active_roster_prerequisite", false);
            plugin.supabase().recordArgEvent(P12_CONFIGURATION_EVENT,
                    "minecraft:p12:release-configuration", "minecraft", null, configuration);
        });
    }

    private void mirrorP12NameAsync() {
        plugin.scheduler().runAsyncSafe("arg.p12.name.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            String treatment = progress.snapshot().branches().get("v5_name_treatment");
            if (treatment == null || treatment.isBlank()) return;
            JsonObject name = new JsonObject();
            name.addProperty("treatment", treatment);
            name.addProperty("outside_record", true);
            name.addProperty("every_treatment_releases_averyn", true);
            plugin.supabase().recordArgEvent(P12_NAME_EVENT,
                    "minecraft:p12:name-treatment", "minecraft", null, name);
        });
    }

    private void mirrorP12ReleaseAsync() {
        plugin.scheduler().runAsyncSafe("arg.p12.release.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            FinaleStateStore.Snapshot state = finale.snapshot();
            JsonObject release = new JsonObject();
            release.addProperty("phase", state.phase().name().toLowerCase(java.util.Locale.ROOT));
            release.addProperty("local_revision", state.revision());
            release.addProperty("manifest_sha256", state.manifestSha256());
            release.addProperty("name_treatment", state.nameTreatment());
            release.addProperty("wren_remembrance", state.wrenOutcome());
            release.addProperty("conduct", state.conductVerdict());
            release.addProperty("every_ending_releases_averyn", true);
            plugin.supabase().recordArgEvent(P12_RELEASE_EVENT,
                    "minecraft:p12:record-closed-averyn-released", "minecraft", null, release);
        });
    }

    public boolean handleFinaleCommand(CommandSender sender, String[] rootArgs) {
        return ritualController.handleFinaleCommand(sender, rootArgs);
    }

    /** Blocking preflight findings only; deferred unloaded chunks are intentionally not failures. */
    public List<String> readinessFindings() {
        LinkedHashSet<String> findings = new LinkedHashSet<>();
        if (!storyInputsEnabled) {
            if (finale.snapshot().phase() == FinaleStateStore.Phase.FAULT) {
                findings.add("V5 finale is locked in FAULT: " + finale.snapshot().faultReason());
            }
            return List.copyOf(findings);
        }
        if (!V5RuntimePredicateRegistry.available()
                || !authority.sha256().equals(
                V5RuntimePredicateRegistry.activeAuthoritySha256())) {
            findings.add("V5 exact-60 runtime registry is not active for the packaged authority");
        }
        if (!progress.manifestSha256().equals(authority.sha256())) {
            findings.add("V5 local progress authority hash differs from the packaged authority");
        }
        for (V5PhysicalComponentCatalog.Finding finding : installer.catalog().findings()) {
            if (finding.severity() == V5PhysicalComponentCatalog.Severity.BLOCKER) {
                findings.add(format(finding));
            }
        }
        addBlockers(findings, lastBindingReport);
        addBlockers(findings, lastMapReport);
        if (plugin.v5HoldMouth() != null) {
            findings.addAll(BukkitContainerTriggerAudit.findings(fixtures));
        }
        if (plugin.supabase() != null && plugin.supabase().isConfigured()
                && !remote.metadataValidated()) {
            findings.add("Supabase V5 campaign/authority metadata has not validated yet");
        }
        if (finale.snapshot().phase() == FinaleStateStore.Phase.FAULT) {
            findings.add("V5 finale is locked in FAULT: " + finale.snapshot().faultReason());
        }
        if (!plugin.v5DiscordHandoffConfigured()) {
            findings.add("LS06 Discord handoff URL is missing or invalid");
        }
        return List.copyOf(findings);
    }

    /**
     * CW07 and HS02 use non-tile player controls plus persistent marker entities. Keep only their
     * bounded three-block neighborhoods loaded so a distant Unlit scan cannot split a control
     * from its marker or erase it from the loaded-fixture index. Paper removes plugin tickets on
     * disable; every other fixture remains ordinary chunk-load/rebind state.
     */
    private void retainCriticalContainerControlChunks() {
        if (plugin.sites() == null) return;
        for (var binding : com.observance.watcher.structure.V5AuthorityManifest.runtimeBindings()) {
            if (!ContainerTriggerBindings.requiredSyntheticComponents().containsKey(binding.nodeId())) continue;
            var site = plugin.sites().get(binding.siteId());
            if (site == null || !site.isPlaced()) continue;
            World world = Bukkit.getWorld(site.worldName());
            if (world == null) continue;
            for (ContainerTriggerChunkPolicy.ChunkCoordinate chunk
                    : ContainerTriggerChunkPolicy.chunks(site.x(), site.z())) {
                world.getChunkAt(chunk.x(), chunk.z()).addPluginChunkTicket(plugin);
            }
        }
    }

    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        if (!closed) installer.rebindMapView(event.getMap());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (closed || deferredRebind != null) return;
        deferredRebind = plugin.getServer().getScheduler().runTask(plugin, () -> {
            deferredRebind = null;
            rebindAllLoadedFixtures();
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        commitLiveRuntimeHandoff(event.getPlayer());
        if (progress.snapshot().isComplete(P6_MODELS_EVENT)
                && progress.snapshot().isComplete(P6_RESPONSIBILITY_EVENT)) {
            mirrorP6MilestonesAsync();
        }
        if (progress.snapshot().isComplete(P5_CURATION_EVENT)) mirrorP5CurationAsync();
        if (progress.snapshot().isComplete(P7_MATERIAL_EVENT)) mirrorP7MaterialAsync();
        if (progress.snapshot().isComplete(P8_PLAN_EVENT)) mirrorP8PlanAsync();
        if (progress.snapshot().isComplete(P8_UNLIT_EVENT)) mirrorP8UnlitAsync();
        if (progress.snapshot().isComplete(P8_REPAIR_EVENT)) mirrorP8RepairAsync();
        if (progress.snapshot().isComplete(P9_BIOGRAPHIES_EVENT)) mirrorP9BiographiesAsync();
        if (progress.snapshot().isComplete(P9_LEAK_EVENT)) mirrorP9LeakAsync();
        if (progress.snapshot().isComplete(P10_CONFRONTED_EVENT)) mirrorP10ConfrontedAsync();
        if (progress.snapshot().isComplete(P10_REMEMBRANCE_EVENT)) mirrorP10RemembranceAsync();
        if (progress.snapshot().isComplete(P11_IDENTIFIED_EVENT)) mirrorP11IdentityAsync();
        if (progress.snapshot().isComplete(P11_UNBOUND_EVENT)) mirrorP11UnboundAsync();
        if (progress.snapshot().isComplete(P12_CONFIGURATION_EVENT)) mirrorP12ConfigurationAsync();
        if (progress.snapshot().isComplete(P12_NAME_EVENT)) mirrorP12NameAsync();
        if (progress.snapshot().isComplete(P12_RELEASE_EVENT)) mirrorP12ReleaseAsync();
        if (progress.snapshot().isComplete("v5_ls06_relay")) {
            plugin.getServer().getScheduler().runTaskLater(
                    plugin, () -> plugin.sendV5DiscordHandoff(event.getPlayer()), 40L);
        }
        if (!isCoda()) return;
        finale.codaReceipt().ifPresent(receipt -> event.getPlayer().sendMessage(
                joined(receipt.exactGoodbye())));
        speakCodaWrenRecord(event.getPlayer());
    }

    /** Entering the authenticated world is the P2 action; local state never waits for the network. */
    private void commitLiveRuntimeHandoff(Player player) {
        boolean created;
        try {
            created = progress.transact(editor -> editor.setBooleanTrue(P2_HANDOFF_EVENT));
            progress.transact(editor -> editor.setBooleanTrue(P3_ACCOUNTS_EVENT));
            if (created) {
                projectLocalState();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.sendMessage(
                        Component.text("The retained world is live. Its local record will keep working if Copperline or Discord goes offline.")), 40L);
            }
        } catch (IOException | RuntimeException failure) {
            plugin.getLogger().severe("P2 live handoff could not be committed locally: " + failure.getMessage());
            return;
        }
        // The web prerequisite can arrive later. Reusing this key on every join is an exact,
        // idempotent retry, never a click or source-possession gate for the local world.
        plugin.scheduler().runAsyncSafe("arg.p2.handoff.mirror", () -> {
            if (plugin.supabase() == null || !plugin.supabase().isConfigured()) return;
            JsonObject payload = new JsonObject();
            payload.addProperty("runtime", "paper-1.21.11");
            payload.addProperty("world_authority", "local-primary");
            plugin.supabase().recordArgEvent(
                    P2_HANDOFF_EVENT,
                    "minecraft:p2:live-runtime-handoff",
                    "minecraft", null, payload);
            JsonObject accounts = new JsonObject();
            accounts.addProperty("incident", "settlement-accounts-disagree");
            accounts.addProperty("availability", "replayable-local-dialogue");
            plugin.supabase().recordArgEvent(
                    P3_ACCOUNTS_EVENT,
                    "minecraft:p3:resident-accounts-opened",
                    "minecraft", null, accounts);
        });
    }

    /** Read-only Coda replay; it never re-enters WR03 or writes a choice. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCodaWrenInteract(PlayerInteractEntityEvent event) {
        if (!isCoda() || event.getHand() != EquipmentSlot.HAND) return;
        String id = event.getRightClicked().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, WrenDialogueBukkitListener.NPC_KEY),
                PersistentDataType.STRING);
        if (!WrenDialogueBukkitListener.WREN_VALUE.equals(id)) return;
        event.setCancelled(true);
        speakCodaWrenRecord(event.getPlayer());
    }

    @Override
    public void close() {
        if (closed) return;
        V5RuntimePredicateRegistry.deactivate();
        if (deferredRebind != null) {
            deferredRebind.cancel();
            deferredRebind = null;
        }
        remote.close();
        p5Curation.close();
        ritualController.close();
        if (mechanicsListener != null) mechanicsListener.close();
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(ritualController);
        HandlerList.unregisterAll(wrenDialogueListener);
        if (mechanicsListener != null) HandlerList.unregisterAll(mechanicsListener);
        if (containerListener != null) HandlerList.unregisterAll(containerListener);
        closed = true;
        started = false;
    }

    private void recoverAndProject() {
        List<String> recovery = new ArrayList<>();
        if (mechanics != null) recovery.addAll(mechanics.recoverCommittedWorld());
        if (containers != null) recovery.addAll(containers.recoverCommitted());
        projectLocalState();
        if (!recovery.isEmpty()) {
            plugin.getLogger().warning("V5 local recovery remains pending: " + recovery);
        }
    }

    private void feedback(UUID actor, String message) {
        Player player = Bukkit.getPlayer(actor);
        if (player != null && player.isOnline()) {
            player.sendActionBar(Component.text(message, NamedTextColor.RED));
        }
    }

    /** A completed node is witnessed: a soft confirming chime and a plain in-register acknowledgement. */
    private void feedbackSolved(UUID actor) {
        Player player = Bukkit.getPlayer(actor);
        if (player == null || !player.isOnline()) return;
        player.sendActionBar(Component.text("the record takes it.", NamedTextColor.GRAY));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.5f, 0.7f);
    }

    private void projectCodaBeforeSave() {
        FinaleStateStore.Snapshot state = finale.snapshot();
        RitualChoices.NameTreatment treatment = state.dimensions().nameTreatment();
        List<String> issues = plugin.applyV5CodaWorldState(treatment);
        if (!issues.isEmpty()) {
            throw new IllegalStateException("Coda world projection failed: " + issues);
        }
    }

    private void upgradeLoadedWrenBodies() {
        if (plugin.wren() == null) return;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (plugin.wren().isWren(entity)) plugin.wren().tag(entity, "");
            }
        }
    }

    private static void addBlockers(
            Set<String> target, V5PhysicalComponentInstaller.Report report) {
        if (report == null) return;
        for (V5PhysicalComponentCatalog.Finding finding : report.findings()) {
            if (finding.severity() == V5PhysicalComponentCatalog.Severity.BLOCKER) {
                target.add(format(finding));
            }
        }
    }

    private static String format(V5PhysicalComponentCatalog.Finding finding) {
        return finding.nodeId() + '/' + finding.componentId() + ": " + finding.message();
    }

    private static Component joined(List<String> lines) {
        Component message = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) message = message.append(Component.newline());
            message = message.append(Component.text(lines.get(index), NamedTextColor.WHITE));
        }
        return message;
    }

    private void speakCodaWrenRecord(Player player) {
        String outcome = finale.snapshot().wrenOutcome();
        List<String> lines = V5DialogueCatalog.wren().lines("coda_" + outcome);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.sendMessage(Component.text(
                        V5DialogueCatalog.wren().displayName(), NamedTextColor.YELLOW));
                player.sendMessage(Component.text(line, NamedTextColor.WHITE));
            }, (long) index * 24L);
        }
    }

    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("V5 runtime lifecycle must run on the server thread");
        }
    }

    /** Exact bound light groups only; no structural blocks are removed during finale theater. */
    private final class ExactFinaleTheater implements BukkitFinaleEffects.TheaterVisuals {
        @Override
        public void darkenDocumentedFixtureGroups(String idempotencyKey) {
            // Collect every currently-lit documented fixture light.
            record LitCell(World world, int x, int y, int z) { }
            List<LitCell> lit = new ArrayList<>();
            for (String nodeId : V5RuntimePredicateRegistry.implementedNodeIds()) {
                for (BukkitFixtureIndex.Binding binding : fixtures.bindingsForNode(nodeId)) {
                    if (binding.kind() != BukkitFixtureIndex.BindingKind.BLOCK) continue;
                    World world = Bukkit.getWorld(binding.worldId());
                    if (world == null) continue;
                    var block = world.getBlockAt(binding.x(), binding.y(), binding.z());
                    if (block.getBlockData() instanceof Lightable light && light.isLit()) {
                        lit.add(new LitCell(world, binding.x(), binding.y(), binding.z()));
                    }
                }
            }
            // The dark rolls outward from where the group is standing (the ritual) and climbs away
            // up the Hold, so the group watches the light leave rather than having it blink off at
            // once. With nobody online, fall back to deepest-first so it still climbs.
            final double[] origin = onlineCentroid();
            lit.sort((a, b) -> origin == null
                    ? Integer.compare(a.y(), b.y())
                    : Double.compare(dist2(a.x(), a.y(), a.z(), origin),
                                     dist2(b.x(), b.y(), b.z(), origin)));

            // One quiet in-register title as it begins — the record stating its own end, not a
            // creepypasta warning. A shallow darkness seeds the unease; the wave deepens it.
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(Title.title(
                        Component.text("the record is closing", NamedTextColor.GRAY),
                        Component.empty()));
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS, 20 * 8, 0, false, false, false));
            }

            // Roll the darkness out in twelve waves over ~24s.
            final int waves = 12;
            final int per = Math.max(1, (int) Math.ceil(lit.size() / (double) waves));
            for (int w = 0; w < waves; w++) {
                int start = w * per;
                if (start >= lit.size()) break;
                int end = Math.min(lit.size(), start + per);
                final List<LitCell> slice = new ArrayList<>(lit.subList(start, end));
                final float pitch = 0.85f - (0.5f * (w / (float) waves));
                plugin.scheduler().runLaterSafe("rp06.darken.wave." + w, (long) w * 40L, () -> {
                    for (LitCell cell : slice) {
                        var block = cell.world().getBlockAt(cell.x(), cell.y(), cell.z());
                        if (block.getBlockData() instanceof Lightable light && light.isLit()) {
                            light.setLit(false);
                            block.setBlockData(light, false);
                        }
                    }
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(),
                                Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, pitch);
                    }
                });
            }
            // The settled dark that carries through the record's break and into the goodbye.
            plugin.scheduler().runLaterSafe("rp06.darken.deepen", (long) waves * 40L, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.addPotionEffect(new PotionEffect(
                            PotionEffectType.DARKNESS, 20 * 40, 0, false, false, false));
                }
            });
        }

        @Override
        public void emitRecordSyntaxBreak(String idempotencyKey) {
            // The mask comes off: the cold record register cracks and the trapped registrar surfaces
            // for a moment before the goodbye. Lowercase, human, no meta.
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(),
                        Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.35f);
                player.spawnParticle(Particle.ASH, player.getLocation().add(0, 1, 0),
                        80, 2.0, 1.0, 2.0, 0.01);
                player.showTitle(Title.title(
                        Component.text("i am still here", NamedTextColor.WHITE),
                        Component.text("for a moment", NamedTextColor.GRAY)));
            }
        }

        /** Centre of all online players, or null when nobody is online. */
        private double[] onlineCentroid() {
            double x = 0, y = 0, z = 0;
            int n = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                Location l = p.getLocation();
                if (l == null) continue;
                x += l.getX();
                y += l.getY();
                z += l.getZ();
                n++;
            }
            return n == 0 ? null : new double[]{x / n, y / n, z / n};
        }

        private double dist2(int x, int y, int z, double[] origin) {
            double dx = x - origin[0], dy = y - origin[1], dz = z - origin[2];
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
