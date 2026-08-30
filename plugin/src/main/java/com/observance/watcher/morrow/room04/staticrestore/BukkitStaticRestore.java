package com.observance.watcher.morrow.room04.staticrestore;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority;
import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Cell;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Candidate;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.CandidateId;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Evidence;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.EvidenceId;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Provenance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Main-thread Paper adapter for M02 world passes, physical markers, evidence, reset, and catch-up. */
public final class BukkitStaticRestore implements AutoCloseable {
    public static final long PASS_INTERVAL_TICKS = 20L;
    public static final long MAXIMUM_ENTITY_LIFETIME_SECONDS = 21_600L;
    public static final float CANDIDATE_LABEL_SCALE = 0.20F;
    public static final float EVIDENCE_LABEL_SCALE = 0.22F;
    private static final long MAINTENANCE_INTERVAL_TICKS = 200L;
    private static final String OWNER_VALUE = "morrow:recovery_room_04:static_restore:m02:v1";

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final String releaseId;
    private final Clock clock;
    private final StaticRestoreManifest manifest;
    private final StaticRestoreEngine engine;
    private final NamespacedKey ownerKey;
    private final NamespacedKey releaseKey;
    private final NamespacedKey kindKey;
    private final NamespacedKey expiresKey;
    private final List<Entity> entities = new ArrayList<>();

    private MorrowRelationshipSnapshot latestSnapshot;
    private long entityExpiresAtMillis;
    private boolean provenanceProvenVisible;
    private BukkitTask theaterTask;
    private BukkitTask maintenanceTask;

    public BukkitStaticRestore(
            JavaPlugin plugin,
            World world,
            Origin origin,
            String releaseId) {
        this(plugin, world, origin, releaseId, Clock.systemUTC(), new StaticRestoreManifest());
    }

    BukkitStaticRestore(
            JavaPlugin plugin,
            World world,
            Origin origin,
            String releaseId,
            Clock clock,
            StaticRestoreManifest manifest) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.ownerKey = new NamespacedKey(plugin, "morrow_static_restore_owner");
        this.releaseKey = new NamespacedKey(plugin, "morrow_static_restore_release");
        this.kindKey = new NamespacedKey(plugin, "morrow_static_restore_kind");
        this.expiresKey = new NamespacedKey(plugin, "morrow_static_restore_expires");
        this.engine = new StaticRestoreEngine(manifest, new StaticRestoreEngine.WorldPort() {
            @Override
            public String blockData(Cell relative) {
                return block(relative).getBlockData().getAsString();
            }

            @Override
            public void setBlockData(Cell relative, String blockData) {
                block(relative).setBlockData(Bukkit.createBlockData(blockData), false);
            }
        });
        requirePrimaryThread();
    }

    public void start(MorrowRelationshipSnapshot snapshot) throws IOException {
        requirePrimaryThread();
        latestSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        cleanupOwned();
        spawnPhysicalAuthority(proofCommitted(snapshot));
        reconcile(snapshot);
        maintenanceTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::maintenanceTick, MAINTENANCE_INTERVAL_TICKS, MAINTENANCE_INTERVAL_TICKS);
    }

    public void reconcile(MorrowRelationshipSnapshot snapshot) throws IOException {
        requirePrimaryThread();
        latestSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        boolean proposed = proposalCommitted(snapshot);
        if (!proposed) {
            cancelTheater();
            engine.step(false);
            return;
        }
        if (proofCommitted(snapshot) && !provenanceProvenVisible) refreshPhysicalAuthority(true);
        if (engine.complete()) {
            cancelTheater();
            return;
        }
        startTheater();
    }

    public void resetAndReplay(MorrowRelationshipSnapshot snapshot) throws IOException {
        requirePrimaryThread();
        latestSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        cancelTheater();
        engine.reset(proposalCommitted(snapshot));
        theaterCue(0, "Static Restore reset to the six-cell Room 04 baseline.", NamedTextColor.YELLOW);
        startTheater();
    }

    public boolean complete() throws IOException {
        requirePrimaryThread();
        return engine.complete();
    }

    public StaticRestoreManifest manifest() { return manifest; }

    public int ownedEntityCount() {
        requirePrimaryThread();
        return (int) world.getEntities().stream().filter(this::owned).count();
    }

    public Candidate candidate(Block block) {
        requirePrimaryThread();
        if (block == null || block.getWorld() != world) return null;
        return manifest.candidate(new Cell(
                block.getX() - origin.x(),
                block.getY() - origin.y(),
                block.getZ() - origin.z()));
    }

    public Candidate candidate(Entity entity) {
        requirePrimaryThread();
        String kind = ownedKind(entity);
        if (kind == null || !kind.startsWith("candidate:")) return null;
        try {
            return manifest.candidate(CandidateId.valueOf(kind.substring("candidate:".length())));
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    public Evidence evidence(Entity entity) {
        requirePrimaryThread();
        String kind = ownedKind(entity);
        if (kind == null || !kind.startsWith("evidence:")) return null;
        try {
            return manifest.evidence(EvidenceId.valueOf(kind.substring("evidence:".length())));
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    public void audit() throws IOException {
        requirePrimaryThread();
        if (proposalCommitted(latestSnapshot)) {
            engine.auditComplete();
        } else {
            engine.auditBaseline();
        }
        int expected = manifest.candidates().size() * 3 + manifest.evidence().size() * 2;
        long owned = world.getEntities().stream().filter(this::owned).count();
        if (owned != expected) throw new IOException("Static Restore physical authority entity count mismatch");
    }

    private void startTheater() {
        requirePrimaryThread();
        if (theaterTask != null) return;
        theaterTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::theaterTick,
                1L,
                PASS_INTERVAL_TICKS);
    }

    private void theaterTick() {
        requirePrimaryThread();
        try {
            StaticRestoreEngine.StepResult result = engine.step(proposalCommitted(latestSnapshot));
            if (result.status() == StaticRestoreEngine.StepStatus.LOCKED) {
                cancelTheater();
                return;
            }
            if (result.pass() > 0) {
                theaterCue(result.pass(),
                        "Static Restore pass " + result.pass() + "/3 audited: "
                                + result.restoredCells() + "/6 cells visible.",
                        NamedTextColor.AQUA);
            }
            if (result.status() == StaticRestoreEngine.StepStatus.COMPLETE) {
                cancelTheater();
            }
        } catch (IOException | RuntimeException failure) {
            cancelTheater();
            plugin.getLogger().severe("Morrow Static Restore halted safely: " + safe(failure.getMessage()));
            nearbyPlayers().forEach(player -> player.sendMessage(Component.text(
                    "Static Restore halted and rolled back its current pass. Use the terminal Reset control to retry.",
                    NamedTextColor.RED)));
        }
    }

    private void theaterCue(int pass, String message, NamedTextColor color) {
        Location center = new Location(world, origin.x() - 3.0, origin.y() + 1.0, origin.z() - 0.5);
        world.spawnParticle(Particle.WAX_ON, center, pass == 0 ? 24 : 12, 1.2, 0.8, 0.4, 0.01);
        world.playSound(center, Sound.BLOCK_COPPER_PLACE, SoundCategory.BLOCKS, 0.7F,
                pass == 0 ? 0.7F : 0.9F + pass * 0.1F);
        nearbyPlayers().forEach(player -> player.sendActionBar(Component.text(message, color)));
    }

    private List<Player> nearbyPlayers() {
        Location center = new Location(world, origin.x(), origin.y(), origin.z());
        return world.getNearbyPlayers(center, 16.0).stream().toList();
    }

    private void spawnPhysicalAuthority(boolean proven) {
        requirePrimaryThread();
        provenanceProvenVisible = proven;
        entityExpiresAtMillis = clock.millis() + MAXIMUM_ENTITY_LIFETIME_SECONDS * 1_000L;
        for (Candidate candidate : manifest.candidates().values()) {
            Location base = location(candidate.cell()).add(0.5, 0.5, 0.5);
            Provenance displayed = proven ? candidate.factualProvenance() : Provenance.AUTHENTICATED;
            BlockDisplay marker = world.spawn(base.clone().add(0.34, 0.34, 0.34), BlockDisplay.class, entity -> {
                configure(entity, "marker:" + candidate.id());
                entity.setBlock(Bukkit.createBlockData(displayed == Provenance.INFERRED
                        ? "minecraft:cut_copper" : "minecraft:waxed_copper_block"));
                entity.setBillboard(Display.Billboard.FIXED);
                entity.setTransformation(new Transformation(
                        new Vector3f(), new AxisAngle4f(), new Vector3f(0.22F, 0.22F, 0.22F), new AxisAngle4f()));
                entity.setViewRange(20.0F);
            });
            entities.add(marker);

            TextDisplay label = world.spawn(base.clone().add(0.0, 0.86, 0.0), TextDisplay.class, entity -> {
                configure(entity, "label:" + candidate.id());
                String suffix = proven
                        ? " // " + displayed.accessibleMark().toUpperCase(Locale.ROOT)
                        : " // MORROW CLAIM: WAXED / SQUARE";
                entity.text(Component.text(candidate.id() + suffix,
                        displayed == Provenance.INFERRED ? NamedTextColor.GOLD : NamedTextColor.AQUA));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setLineWidth(240);
                entity.setShadowed(true);
                entity.setViewRange(20.0F);
                entity.setTransformation(new Transformation(
                        new Vector3f(), new AxisAngle4f(),
                        new Vector3f(CANDIDATE_LABEL_SCALE), new AxisAngle4f()));
            });
            entities.add(label);

            Interaction interaction = world.spawn(base, Interaction.class, entity -> {
                configure(entity, "candidate:" + candidate.id());
                entity.setInteractionWidth(1.0F);
                entity.setInteractionHeight(1.0F);
                entity.setResponsive(true);
            });
            entities.add(interaction);
        }

        int index = 0;
        for (Evidence source : manifest.evidence().values().stream()
                .sorted(java.util.Comparator.comparing(Evidence::id)).toList()) {
            Location base = new Location(world, origin.x() + 4.5, origin.y() + 0.85,
                    origin.z() - 2.3 + index * 1.45);
            TextDisplay label = world.spawn(base.clone().add(0.0, 0.65, 0.0), TextDisplay.class, entity -> {
                configure(entity, "evidence-label:" + source.id());
                entity.text(Component.text("M02 SOURCE // " + source.title(), NamedTextColor.WHITE));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setLineWidth(260);
                entity.setShadowed(true);
                entity.setViewRange(20.0F);
                entity.setTransformation(new Transformation(
                        new Vector3f(), new AxisAngle4f(),
                        new Vector3f(EVIDENCE_LABEL_SCALE), new AxisAngle4f()));
            });
            entities.add(label);
            Interaction interaction = world.spawn(base, Interaction.class, entity -> {
                configure(entity, "evidence:" + source.id());
                entity.setInteractionWidth(1.6F);
                entity.setInteractionHeight(1.4F);
                entity.setResponsive(true);
            });
            entities.add(interaction);
            index++;
        }
    }

    private void refreshPhysicalAuthority(boolean proven) {
        cleanupOwned();
        spawnPhysicalAuthority(proven);
    }

    private void maintenanceTick() {
        requirePrimaryThread();
        if (clock.millis() >= entityExpiresAtMillis) {
            refreshPhysicalAuthority(proofCommitted(latestSnapshot));
        }
    }

    private void configure(Entity entity, String kind) {
        entity.setPersistent(true);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, OWNER_VALUE);
        pdc.set(releaseKey, PersistentDataType.STRING, releaseId);
        pdc.set(kindKey, PersistentDataType.STRING, kind);
        pdc.set(expiresKey, PersistentDataType.LONG, entityExpiresAtMillis);
    }

    private String ownedKind(Entity entity) {
        if (!(entity instanceof Interaction) || !owned(entity)) return null;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (!releaseId.equals(pdc.get(releaseKey, PersistentDataType.STRING))) return null;
        return pdc.get(kindKey, PersistentDataType.STRING);
    }

    private boolean owned(Entity entity) {
        return entity != null && OWNER_VALUE.equals(
                entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING));
    }

    private void cleanupOwned() {
        requirePrimaryThread();
        for (Entity entity : List.copyOf(world.getEntities())) {
            if (owned(entity)) entity.remove();
        }
        entities.clear();
    }

    private Block block(Cell relative) {
        return world.getBlockAt(origin.x() + relative.x(), origin.y() + relative.y(), origin.z() + relative.z());
    }

    private Location location(Cell relative) {
        return new Location(world, origin.x() + relative.x(), origin.y() + relative.y(), origin.z() + relative.z());
    }

    private static boolean proposalCommitted(MorrowRelationshipSnapshot snapshot) {
        return snapshot != null && snapshot.committedEvents().contains(MorrowDialogAuthority.PROPOSAL_AUTHENTICATED);
    }

    private static boolean proofCommitted(MorrowRelationshipSnapshot snapshot) {
        return snapshot != null && snapshot.committedEvents().contains(MorrowDialogAuthority.INTENTION_ERROR_PROVEN);
    }

    private void cancelTheater() {
        if (theaterTask != null) theaterTask.cancel();
        theaterTask = null;
    }

    @Override
    public void close() {
        requirePrimaryThread();
        cancelTheater();
        if (maintenanceTask != null) maintenanceTask.cancel();
        maintenanceTask = null;
        cleanupOwned();
        latestSnapshot = null;
        provenanceProvenVisible = false;
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Morrow Static Restore requires the Paper primary thread");
        }
    }
}
