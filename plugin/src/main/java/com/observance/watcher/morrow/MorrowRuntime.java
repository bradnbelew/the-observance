package com.observance.watcher.morrow;

import com.observance.watcher.ObservancePlugin;
import com.observance.watcher.morrow.room04.BukkitRecoveryRoom04World;
import com.observance.watcher.morrow.room04.BukkitRecoveryRoom04Entry;
import com.observance.watcher.morrow.room04.RecoveryRoom04Installer;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest;
import com.observance.watcher.morrow.dialog.BukkitMorrowDialogs;
import com.observance.watcher.morrow.presentation.BukkitMorrowBody;
import com.observance.watcher.morrow.room04.staticrestore.BukkitStaticRestore;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest;
import com.observance.watcher.morrow.room04.replay.BukkitEntityReplay;
import com.observance.watcher.morrow.versionrooms.BukkitVersionRooms;
import com.observance.watcher.morrow.versionrooms.BukkitVersionRoomsWorld;
import com.observance.watcher.morrow.versionrooms.VersionRoomsInstaller;
import com.observance.watcher.morrow.versionrooms.VersionRoomsManifest;
import com.observance.watcher.morrow.consensus.BukkitConsensusAudit;
import com.observance.watcher.morrow.consensus.BukkitConsensusAuditWorld;
import com.observance.watcher.morrow.consensus.ConsensusAuditInstaller;
import com.observance.watcher.morrow.consensus.ConsensusAuditManifest;
import com.observance.watcher.listener.ResourcePackPusher;
import com.observance.watcher.signal.ResourcePackTracker;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Fail-closed Paper lifecycle for the disabled-by-default Morrow reboot runtime. */
public final class MorrowRuntime implements AutoCloseable {
    private static final String JOURNAL_NAME = "morrow-reboot.journal";
    private static final String CURSOR_NAME = "morrow-reboot.projector.cursor";
    private static final String ROOM04_SNAPSHOT_NAME = "morrow-room04.rollback.snapshot";
    private static final String ROOM04_RECEIPT_NAME = "morrow-room04.install.receipt";
    private static final String VERSION_ROOMS_RECEIPT_NAME = "morrow-version-rooms.install.receipt";
    private static final String CONSENSUS_AUDIT_RECEIPT_NAME = "morrow-consensus-audit.install.receipt";

    private final MorrowRuntimeSettings settings;
    private final MorrowLocalState localState;
    private final MorrowEventProjector projector;
    private final RecoveryRoom04Installer.Result room04Result;
    private final VersionRoomsInstaller.Result versionRoomsResult;
    private final ConsensusAuditInstaller.Result consensusAuditResult;
    private final BukkitMorrowBody body;
    private final BukkitStaticRestore staticRestore;
    private final BukkitEntityReplay entityReplay;
    private final BukkitMorrowDialogs dialogs;
    private final BukkitRecoveryRoom04Entry playerEntry;
    private final BukkitVersionRooms versionRooms;
    private final BukkitConsensusAudit consensusAudit;
    private final ResourcePackTracker resourcePackTracker;
    private final ResourcePackPusher resourcePackPusher;

    private MorrowRuntime(
            MorrowRuntimeSettings settings,
            MorrowLocalState localState,
            MorrowEventProjector projector,
            RecoveryRoom04Installer.Result room04Result,
            VersionRoomsInstaller.Result versionRoomsResult,
            ConsensusAuditInstaller.Result consensusAuditResult,
            BukkitMorrowBody body,
            BukkitStaticRestore staticRestore,
            BukkitEntityReplay entityReplay,
            BukkitMorrowDialogs dialogs,
            BukkitRecoveryRoom04Entry playerEntry,
            BukkitVersionRooms versionRooms,
            BukkitConsensusAudit consensusAudit,
            ResourcePackTracker resourcePackTracker,
            ResourcePackPusher resourcePackPusher) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.localState = Objects.requireNonNull(localState, "localState");
        this.projector = Objects.requireNonNull(projector, "projector");
        this.room04Result = room04Result;
        this.versionRoomsResult = versionRoomsResult;
        this.consensusAuditResult = consensusAuditResult;
        this.body = body;
        this.staticRestore = staticRestore;
        this.entityReplay = entityReplay;
        this.dialogs = dialogs;
        this.playerEntry = playerEntry;
        this.versionRooms = versionRooms;
        this.consensusAudit = consensusAudit;
        this.resourcePackTracker = resourcePackTracker;
        this.resourcePackPusher = resourcePackPusher;
    }

    /** Returns {@code null} while the shipped disabled gate is closed. */
    public static MorrowRuntime start(ObservancePlugin plugin) throws IOException {
        Objects.requireNonNull(plugin, "plugin");
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("morrow-reboot");
        if (section == null || !section.getBoolean("enabled", false)) return null;

        String secret = resolveSecret(section);
        MorrowRuntimeSettings settings = MorrowRuntimeSettings.create(
                section.getString("release-id", ""),
                section.getString("campaign-id", ""),
                section.getString("world", ""),
                section.getString("ingest-url", ""),
                secret,
                section.getLong("projector.connect-timeout-ms", 5_000L),
                section.getLong("projector.request-timeout-ms", 10_000L),
                section.getLong("projector.initial-retry-ms", 500L),
                section.getLong("projector.maximum-retry-ms", 30_000L));
        ConfigurationSection packSection = section.getConfigurationSection("resource-pack");
        boolean packEnabled = packSection != null && packSection.getBoolean("enabled", false);
        boolean packRequired = packSection != null && packSection.getBoolean("required", false);
        MorrowResourcePackPolicy.Plan packPlan = MorrowResourcePackPolicy.create(
                packEnabled,
                packRequired,
                plugin.config().resourcePackUrl(),
                plugin.config().resourcePackSha1(),
                plugin.config().resourcePackPrompt(),
                plugin.config().resourcePackDelayTicks());

        // This is the only world lookup in the lifecycle. It happens on Paper's enable thread before
        // the projector exists; no Bukkit object is retained by the worker.
        World world = plugin.getServer().getWorld(settings.worldName());
        if (world == null || !settings.worldName().equals(world.getName())) {
            throw new IllegalArgumentException("configured Morrow world is not loaded");
        }

        Path data = plugin.getDataFolder().toPath();
        MorrowLocalState state = MorrowLocalState.open(data.resolve(JOURNAL_NAME), settings.releaseId());
        RecoveryRoom04Installer.Result room04Result = null;
        RecoveryRoom04Installer.Origin room04Origin = null;
        if (section.getBoolean("room04.build-enabled", false)) {
            room04Origin = new RecoveryRoom04Installer.Origin(
                    section.getInt("room04.origin-x"),
                    section.getInt("room04.origin-y", 80),
                    section.getInt("room04.origin-z"));
            RecoveryRoom04Manifest.Bounds bounds = RecoveryRoom04Manifest.BOUNDS;
            if (room04Origin.y() + bounds.minimumY() < world.getMinHeight()
                    || room04Origin.y() + bounds.maximumY() >= world.getMaxHeight()) {
                throw new IllegalArgumentException("Recovery Room 04 origin exceeds the configured world height");
            }
            RecoveryRoom04Installer installer = new RecoveryRoom04Installer(
                    new RecoveryRoom04Manifest(),
                    data.resolve(ROOM04_SNAPSHOT_NAME),
                    data.resolve(ROOM04_RECEIPT_NAME));
            Set<RecoveryRoom04Manifest.Cell> mutableCells = state.snapshot().committedEvents().contains(
                    "morrow.act1.static_proposal_authenticated")
                    ? new StaticRestoreManifest().mutableCells()
                    : Set.of();
            room04Result = installer.install(
                    settings.releaseId(), room04Origin,
                    new BukkitRecoveryRoom04World(world, room04Origin),
                    mutableCells);
        }
        VersionRoomsInstaller.Result versionRoomsResult = null;
        VersionRoomsInstaller.Origin versionRoomsOrigin = null;
        if (section.getBoolean("version-rooms.build-enabled", false)) {
            versionRoomsOrigin = new VersionRoomsInstaller.Origin(
                    section.getInt("version-rooms.origin-x", 32),
                    section.getInt("version-rooms.origin-y", 80),
                    section.getInt("version-rooms.origin-z"));
            VersionRoomsManifest.Bounds bounds = VersionRoomsManifest.BOUNDS;
            if (versionRoomsOrigin.y() + bounds.minimumY() < world.getMinHeight()
                    || versionRoomsOrigin.y() + bounds.maximumY() >= world.getMaxHeight()) {
                throw new IllegalArgumentException("M05 version-room origin exceeds the configured world height");
            }
            VersionRoomsManifest manifest = new VersionRoomsManifest();
            versionRoomsResult = new VersionRoomsInstaller(
                    manifest, data.resolve(VERSION_ROOMS_RECEIPT_NAME)).install(
                    settings.releaseId(), versionRoomsOrigin,
                    new BukkitVersionRoomsWorld(world, versionRoomsOrigin));
        }
        ConsensusAuditInstaller.Result consensusAuditResult = null;
        ConsensusAuditInstaller.Origin consensusAuditOrigin = null;
        if (section.getBoolean("consensus-audit.build-enabled", false)) {
            if (versionRoomsResult == null) {
                throw new IllegalArgumentException("M06 consensus audit requires the M05 version rooms");
            }
            consensusAuditOrigin = new ConsensusAuditInstaller.Origin(
                    section.getInt("consensus-audit.origin-x", 64),
                    section.getInt("consensus-audit.origin-y", 80),
                    section.getInt("consensus-audit.origin-z"));
            ConsensusAuditManifest.Bounds bounds = ConsensusAuditManifest.BOUNDS;
            if (consensusAuditOrigin.y() + bounds.minimumY() < world.getMinHeight()
                    || consensusAuditOrigin.y() + bounds.maximumY() >= world.getMaxHeight()) {
                throw new IllegalArgumentException("M06 consensus-audit origin exceeds the configured world height");
            }
            ConsensusAuditManifest manifest = new ConsensusAuditManifest();
            consensusAuditResult = new ConsensusAuditInstaller(
                    manifest, data.resolve(CONSENSUS_AUDIT_RECEIPT_NAME)).install(
                    settings.releaseId(), consensusAuditOrigin,
                    new BukkitConsensusAuditWorld(world, consensusAuditOrigin));
        }
        MorrowEventProjector projector = null;
        BukkitMorrowBody body = null;
        BukkitStaticRestore staticRestore = null;
        BukkitEntityReplay entityReplay = null;
        BukkitMorrowDialogs dialogs = null;
        BukkitRecoveryRoom04Entry playerEntry = null;
        BukkitVersionRooms versionRooms = null;
        BukkitConsensusAudit consensusAudit = null;
        ResourcePackTracker resourcePackTracker = null;
        ResourcePackPusher resourcePackPusher = null;
        try {
            projector = MorrowEventProjector.open(
                    state,
                    settings,
                    data.resolve(CURSOR_NAME),
                    plugin.getLogger());
            projector.start();
            if (packPlan.enabled()) {
                resourcePackTracker = new ResourcePackTracker(
                        plugin.safety(),
                        (uuid, name, status) -> plugin.getLogger().info(
                                "MORROW_RESOURCE_PACK player=" + name + " status=" + status));
                resourcePackPusher = new ResourcePackPusher(
                        plugin.scheduler(), plugin.safety(), packPlan.url(), packPlan.sha1(),
                        packPlan.required(), packPlan.prompt(), packPlan.delayTicks());
                plugin.getServer().getPluginManager().registerEvents(resourcePackTracker, plugin);
                plugin.getServer().getPluginManager().registerEvents(resourcePackPusher, plugin);
            }
            if (room04Result != null && room04Origin != null) {
                playerEntry = new BukkitRecoveryRoom04Entry(plugin, world, room04Origin);
                playerEntry.start();
                staticRestore = new BukkitStaticRestore(plugin, world, room04Origin, settings.releaseId());
                staticRestore.start(state.snapshot());
                body = new BukkitMorrowBody(plugin, world, room04Origin, settings.releaseId());
                body.start(state.snapshot());
                entityReplay = new BukkitEntityReplay(plugin, world, room04Origin, state, data);
                entityReplay.start();
                dialogs = new BukkitMorrowDialogs(plugin, world, room04Origin, state, body, staticRestore, entityReplay);
                dialogs.start();
                plugin.getLogger().info("MORROW_ROOM04_READY status=" + room04Result.status()
                        + " manifest=" + room04Result.manifestSha256()
                        + " snapshot=" + room04Result.snapshotSha256()
                        + " blocks=" + room04Result.blockCount());
            }
            if (versionRoomsResult != null && versionRoomsOrigin != null) {
                versionRooms = new BukkitVersionRooms(
                        plugin, world, versionRoomsOrigin, settings.releaseId(), state, data);
                versionRooms.start();
                plugin.getLogger().info("MORROW_VERSION_ROOMS_READY status=" + versionRoomsResult.status()
                        + " manifest=" + versionRoomsResult.manifestSha256()
                        + " blocks=" + versionRoomsResult.blockCount());
            }
            if (consensusAuditResult != null && consensusAuditOrigin != null && versionRooms != null) {
                BukkitVersionRooms requiredVersionRooms = versionRooms;
                consensusAudit = new BukkitConsensusAudit(
                        plugin, world, consensusAuditOrigin, settings.releaseId(), state,
                        requiredVersionRooms::sourcesPreserved, data);
                consensusAudit.start();
                plugin.getLogger().info("MORROW_CONSENSUS_AUDIT_READY status=" + consensusAuditResult.status()
                        + " manifest=" + consensusAuditResult.manifestSha256()
                        + " blocks=" + consensusAuditResult.blockCount());
            }
            plugin.getLogger().info("MORROW_RUNTIME_READY release=" + settings.releaseId()
                    + " journal_events=" + state.snapshot().committedEvents().size()
                    + " projector_state=" + projector.snapshot().state()
                    + " resource_pack=" + (packPlan.enabled() ? "optional" : "disabled")
                    + " body_entities=" + (body == null ? 0 : body.ownedEntityCount())
                    + " static_entities=" + (staticRestore == null ? 0 : staticRestore.ownedEntityCount())
                    + " replay_entities=" + (entityReplay == null ? 0 : entityReplay.ownedEntityCount())
                    + " version_entities=" + (versionRooms == null ? 0 : versionRooms.ownedEntityCount())
                    + " consensus_entities=" + (consensusAudit == null ? 0 : consensusAudit.ownedEntityCount()));
            return new MorrowRuntime(
                    settings, state, projector, room04Result, versionRoomsResult, consensusAuditResult,
                    body, staticRestore, entityReplay, dialogs,
                    playerEntry, versionRooms, consensusAudit, resourcePackTracker, resourcePackPusher);
        } catch (IOException | RuntimeException | LinkageError failure) {
            if (consensusAudit != null) consensusAudit.close();
            if (versionRooms != null) versionRooms.close();
            if (dialogs != null) dialogs.close();
            if (entityReplay != null) entityReplay.close();
            if (staticRestore != null) staticRestore.close();
            if (body != null) body.close();
            if (playerEntry != null) playerEntry.close();
            if (resourcePackPusher != null) HandlerList.unregisterAll(resourcePackPusher);
            if (resourcePackTracker != null) HandlerList.unregisterAll(resourcePackTracker);
            if (projector != null) projector.close();
            throw failure;
        }
    }

    public MorrowRuntimeSettings settings() {
        return settings;
    }

    public MorrowLocalState localState() {
        return localState;
    }

    public MorrowEventProjector.Snapshot projectorSnapshot() {
        return projector.snapshot();
    }

    public Optional<RecoveryRoom04Installer.Result> room04Result() {
        return Optional.ofNullable(room04Result);
    }

    public Optional<VersionRoomsInstaller.Result> versionRoomsResult() {
        return Optional.ofNullable(versionRoomsResult);
    }

    public Optional<ConsensusAuditInstaller.Result> consensusAuditResult() {
        return Optional.ofNullable(consensusAuditResult);
    }

    @Override
    public void close() {
        if (consensusAudit != null) consensusAudit.close();
        if (versionRooms != null) versionRooms.close();
        if (dialogs != null) dialogs.close();
        if (entityReplay != null) entityReplay.close();
        if (staticRestore != null) staticRestore.close();
        if (body != null) body.close();
        if (playerEntry != null) playerEntry.close();
        if (resourcePackPusher != null) HandlerList.unregisterAll(resourcePackPusher);
        if (resourcePackTracker != null) HandlerList.unregisterAll(resourcePackTracker);
        projector.close();
    }

    private static String resolveSecret(ConfigurationSection section) {
        String direct = section.getString("ingest-secret", "");
        if (direct != null && !direct.isBlank()) return direct.trim();
        String environmentName = section.getString(
                "ingest-secret-env", "MORROW_MINECRAFT_INGEST_SECRET");
        if (environmentName == null || !environmentName.matches("[A-Z][A-Z0-9_]{1,63}")) {
            throw new IllegalArgumentException("morrow-reboot.ingest-secret-env has invalid grammar");
        }
        String environmentValue = System.getenv(environmentName);
        return environmentValue == null ? "" : environmentValue.trim();
    }
}
