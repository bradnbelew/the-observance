package com.observance.watcher.v5runtime;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.npc.V5DialogueCatalog;
import com.observance.watcher.structure.V5RuntimePredicateRegistry;
import com.observance.watcher.v5runtime.container.BukkitContainerCustody;
import com.observance.watcher.v5runtime.container.BukkitContainerListener;
import com.observance.watcher.v5runtime.container.BukkitContainerTriggerAudit;
import com.observance.watcher.v5runtime.container.BukkitContainerWorld;
import com.observance.watcher.v5runtime.container.ContainerAuthorityContract;
import com.observance.watcher.v5runtime.container.ContainerSolveService;
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
    private final ObservancePlugin plugin;
    private final PhysicalPredicateAuthority authority;
    private final V5ProgressStore progress;
    private final V5PhysicalComponentInstaller installer;
    private final BukkitFixtureIndex fixtures;
    private final BukkitContainerCustody custody;
    private final V5RemoteStateCache remote;

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
            BukkitMechanicState live = new BukkitMechanicState();
            BukkitWorldStateEvaluator worldState = new BukkitWorldStateEvaluator(
                    plugin, fixtures, live);
            V5BukkitProjection projection = new V5BukkitProjection(
                    plugin, progress, fixtures, custody);
            this.mechanics = new V5MechanicsEngine(
                    authority, progress, remote, worldState, projection, projection,
                    remote::mirrorAsync, this::feedback);
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

    /** Rebuild exact coordinate/PDC bindings after construction, repair, startup, or chunk load. */
    public void rebindAllLoadedFixtures() {
        if (!Bukkit.isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin, this::rebindAllLoadedFixtures);
            return;
        }
        if (closed) return;
        fixtures.clear();
        Location mouth = plugin.v5HoldMouth();
        lastBindingReport = installer.bindLoadedFixtures(fixtures, mouth);
        lastMapReport = installer.rebindLoadedMapViews(mouth);
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
        plugin.projectV5LocalState(progress.snapshot());
        remote.mirrorLocalSnapshotAsync();
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
        return List.copyOf(findings);
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
        if (!isCoda()) return;
        finale.codaReceipt().ifPresent(receipt -> event.getPlayer().sendMessage(
                joined(receipt.exactGoodbye())));
        speakCodaWrenRecord(event.getPlayer());
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
            for (String nodeId : V5RuntimePredicateRegistry.implementedNodeIds()) {
                for (BukkitFixtureIndex.Binding binding : fixtures.bindingsForNode(nodeId)) {
                    if (binding.kind() != BukkitFixtureIndex.BindingKind.BLOCK) continue;
                    World world = Bukkit.getWorld(binding.worldId());
                    if (world == null) continue;
                    var block = world.getBlockAt(binding.x(), binding.y(), binding.z());
                    if (block.getBlockData() instanceof Lightable light && light.isLit()) {
                        light.setLit(false);
                        block.setBlockData(light, false);
                    }
                }
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS, 20 * 10, 0, false, false, false));
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.5f);
                player.showTitle(Title.title(
                        Component.text("THE RECORD CLOSES", NamedTextColor.DARK_RED),
                        Component.text("Do not look away.", NamedTextColor.GRAY)));
            }
        }

        @Override
        public void emitRecordSyntaxBreak(String idempotencyKey) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(),
                        Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.35f);
                player.spawnParticle(Particle.ASH, player.getLocation().add(0, 1, 0),
                        80, 2.0, 1.0, 2.0, 0.01);
                player.showTitle(Title.title(
                        Component.text("RECORD // SEVERED", NamedTextColor.RED),
                        Component.text("The server is saying goodbye.", NamedTextColor.WHITE)));
            }
        }
    }
}
