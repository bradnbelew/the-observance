package com.observance.watcher.morrow.presentation;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;
import com.observance.watcher.morrow.MorrowStage;
import com.observance.watcher.morrow.presentation.MorrowBodyAuthority.Part;
import com.observance.watcher.morrow.presentation.MorrowBodyAuthority.PartSpec;
import com.observance.watcher.morrow.presentation.MorrowBodyAuthority.Pose;
import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Main-thread-only vanilla display body with deterministic PDC ownership and cleanup. */
public final class BukkitMorrowBody implements AutoCloseable {
    private static final String OWNER_VALUE = "morrow:recovery_room_04:fallback_body:v1";
    private static final float BASE_YAW = 180.0F;

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final String releaseId;
    private final Clock clock;
    private final NamespacedKey ownerKey;
    private final NamespacedKey releaseKey;
    private final NamespacedKey poseKey;
    private final NamespacedKey partKey;
    private final NamespacedKey revisionKey;
    private final NamespacedKey expiresKey;
    private final List<Entity> entities = new ArrayList<>();

    private Pose currentPose;
    private long currentRevision = -1L;
    private long expiresAtMillis;
    private BlockDisplay head;
    private Interaction interaction;
    private UUID activeSpeaker;
    private long focusUntilMillis;
    private BukkitTask trackingTask;
    private BukkitTask deferredAuditTask;

    public BukkitMorrowBody(
            JavaPlugin plugin,
            World world,
            Origin origin,
            String releaseId) {
        this(plugin, world, origin, releaseId, Clock.systemUTC());
    }

    BukkitMorrowBody(
            JavaPlugin plugin,
            World world,
            Origin origin,
            String releaseId,
            Clock clock) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.releaseId = Objects.requireNonNull(releaseId, "releaseId");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ownerKey = new NamespacedKey(plugin, "morrow_body_owner");
        this.releaseKey = new NamespacedKey(plugin, "morrow_body_release");
        this.poseKey = new NamespacedKey(plugin, "morrow_body_pose");
        this.partKey = new NamespacedKey(plugin, "morrow_body_part");
        this.revisionKey = new NamespacedKey(plugin, "morrow_body_revision");
        this.expiresKey = new NamespacedKey(plugin, "morrow_body_expires");
        requirePrimaryThread();
    }

    public void start(MorrowRelationshipSnapshot snapshot) {
        requirePrimaryThread();
        Objects.requireNonNull(snapshot, "snapshot");
        cancelDeferredAudit();
        cleanupOwned();
        spawn(snapshot.stage(), snapshot.revision());
        trackingTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::trackingTick,
                MorrowBodyAuthority.TRACKING_INTERVAL_TICKS,
                MorrowBodyAuthority.TRACKING_INTERVAL_TICKS);
    }

    public void applySnapshot(MorrowRelationshipSnapshot snapshot) {
        requirePrimaryThread();
        Objects.requireNonNull(snapshot, "snapshot");
        Pose expected = MorrowBodyAuthority.poseFor(snapshot.stage());
        if (expected == currentPose && snapshot.revision() == currentRevision) {
            try {
                audit();
                return;
            } catch (IllegalStateException drift) {
                // The body is presentation-only and fully Morrow-owned. A
                // clicked Interaction can become invalid while Paper delivers
                // the native-dialog callback; rebuild it from the durable
                // snapshot instead of misreporting a world-state failure.
                plugin.getLogger().warning(
                        "Morrow fallback body drift detected; rebuilding: " + safe(drift.getMessage()));
            }
        }
        cancelDeferredAudit();
        cleanupOwned();
        spawn(snapshot.stage(), snapshot.revision());
        long expectedRevision = snapshot.revision();
        deferredAuditTask = plugin.getServer().getScheduler().runTask(plugin, () -> {
            deferredAuditTask = null;
            if (currentRevision != expectedRevision) return;
            try {
                audit();
            } catch (RuntimeException failure) {
                plugin.getLogger().severe(
                        "Morrow fallback body deferred audit failed: " + safe(failure.getMessage()));
            }
        });
    }

    public void focus(Player player) {
        requirePrimaryThread();
        if (player == null || !player.isOnline() || player.getWorld() != world) return;
        if (player.getLocation().distanceSquared(anchor(currentPose != null && currentPose.embodied()))
                > MorrowBodyAuthority.MAXIMUM_FOCUS_DISTANCE * MorrowBodyAuthority.MAXIMUM_FOCUS_DISTANCE) {
            return;
        }
        activeSpeaker = player.getUniqueId();
        focusUntilMillis = clock.millis() + MorrowBodyAuthority.ACTIVE_SPEAKER_SECONDS * 1_000L;
    }

    public boolean isOwnedInteraction(Entity entity) {
        requirePrimaryThread();
        if (!(entity instanceof Interaction)) return false;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return OWNER_VALUE.equals(pdc.get(ownerKey, PersistentDataType.STRING))
                && releaseId.equals(pdc.get(releaseKey, PersistentDataType.STRING))
                && "interaction".equals(pdc.get(partKey, PersistentDataType.STRING));
    }

    public Pose currentPose() {
        return currentPose;
    }

    public int ownedEntityCount() {
        requirePrimaryThread();
        return (int) world.getEntities().stream().filter(this::owned).count();
    }

    public void audit() {
        audit(true);
    }

    private void audit(boolean requireValidInteraction) {
        requirePrimaryThread();
        if (currentPose == null) throw new IllegalStateException("Morrow fallback body is not started");
        int expected = currentPose.embodied() ? Part.values().length + 2 : 2;
        List<Entity> owned = world.getEntities().stream().filter(this::owned).toList();
        if (owned.size() != expected) {
            throw new IllegalStateException("Morrow fallback body entity count mismatch");
        }
        long now = clock.millis();
        for (Entity entity : owned) {
            PersistentDataContainer pdc = entity.getPersistentDataContainer();
            if (!releaseId.equals(pdc.get(releaseKey, PersistentDataType.STRING))
                    || !currentPose.key().equals(pdc.get(poseKey, PersistentDataType.STRING))
                    || !Long.valueOf(currentRevision).equals(pdc.get(revisionKey, PersistentDataType.LONG))
                    || pdc.get(partKey, PersistentDataType.STRING) == null
                    || pdc.getOrDefault(expiresKey, PersistentDataType.LONG, 0L) <= now) {
                throw new IllegalStateException("Morrow fallback body PDC audit failed");
            }
        }
        if (interaction == null
                || (requireValidInteraction && !interaction.isValid())
                || !isOwnedInteraction(interaction)) {
            throw new IllegalStateException("Morrow fallback interaction is missing");
        }
    }

    private void spawn(MorrowStage stage, long revision) {
        requirePrimaryThread();
        currentPose = MorrowBodyAuthority.poseFor(stage);
        currentRevision = revision;
        expiresAtMillis = clock.millis() + MorrowBodyAuthority.MAXIMUM_LIFETIME_SECONDS * 1_000L;
        Location anchor = anchor(currentPose.embodied());

        if (currentPose.embodied()) {
            for (PartSpec spec : MorrowBodyAuthority.parts(currentPose)) {
                Location location = anchor.clone().add(spec.x(), spec.y(), spec.z());
                BlockDisplay display = world.spawn(location, BlockDisplay.class, entity -> {
                    configure(entity, spec.part().name().toLowerCase(java.util.Locale.ROOT));
                    entity.setBlock(Bukkit.createBlockData(spec.blockData()));
                    entity.setBillboard(Display.Billboard.FIXED);
                    entity.setInterpolationDuration((int) MorrowBodyAuthority.TRACKING_INTERVAL_TICKS);
                    entity.setTeleportDuration((int) MorrowBodyAuthority.TRACKING_INTERVAL_TICKS);
                    entity.setViewRange(24.0F);
                    entity.setTransformation(new Transformation(
                            new Vector3f(),
                            new AxisAngle4f(),
                            new Vector3f(spec.scaleX(), spec.scaleY(), spec.scaleZ()),
                            new AxisAngle4f()));
                });
                entities.add(display);
                if (spec.part() == Part.HEAD) head = display;
            }
        }

        Location labelLocation = currentPose.embodied()
                ? anchor.clone().add(0.0, 2.35, 0.0)
                : anchor.clone().add(0.0, MorrowBodyAuthority.TERMINAL_LABEL_Y,
                        MorrowBodyAuthority.TERMINAL_LABEL_Z);
        TextDisplay label = world.spawn(labelLocation, TextDisplay.class, entity -> {
            configure(entity, "label");
            entity.text(Component.text(currentPose.accessibleLabel(), NamedTextColor.AQUA));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setLineWidth(180);
            entity.setShadowed(true);
            entity.setSeeThrough(false);
            entity.setViewRange(24.0F);
            entity.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(),
                    new Vector3f(MorrowBodyAuthority.LABEL_SCALE), new AxisAngle4f()));
        });
        entities.add(label);

        Location interactionLocation = currentPose.embodied()
                ? anchor.clone().add(0.0, 1.15, 0.0)
                : anchor.clone().add(0.0, MorrowBodyAuthority.TERMINAL_INTERACTION_Y,
                        MorrowBodyAuthority.TERMINAL_INTERACTION_Z);
        interaction = world.spawn(interactionLocation, Interaction.class, entity -> {
            configure(entity, "interaction");
            entity.setInteractionWidth(currentPose.embodied() ? 1.65F : 1.35F);
            entity.setInteractionHeight(currentPose.embodied() ? 2.45F : 2.15F);
            entity.setResponsive(true);
        });
        entities.add(interaction);
        // Paper can report a newly spawned Interaction as temporarily invalid
        // during a native dialog callback. PDC/count ownership is synchronous;
        // strict validity is audited on the following server tick.
        audit(false);
    }

    private void configure(Entity entity, String part) {
        entity.setPersistent(true);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, OWNER_VALUE);
        pdc.set(releaseKey, PersistentDataType.STRING, releaseId);
        pdc.set(poseKey, PersistentDataType.STRING, currentPose.key());
        pdc.set(partKey, PersistentDataType.STRING, part);
        pdc.set(revisionKey, PersistentDataType.LONG, currentRevision);
        pdc.set(expiresKey, PersistentDataType.LONG, expiresAtMillis);
    }

    private void trackingTick() {
        requirePrimaryThread();
        if (clock.millis() >= expiresAtMillis) {
            cleanupOwned();
            if (trackingTask != null) trackingTask.cancel();
            trackingTask = null;
            return;
        }
        if (head == null || !head.isValid()) return;
        if (activeSpeaker == null || clock.millis() > focusUntilMillis) {
            activeSpeaker = null;
            head.setRotation(BASE_YAW, 0.0F);
            return;
        }
        Player player = plugin.getServer().getPlayer(activeSpeaker);
        if (player == null || !player.isOnline() || player.getWorld() != world) {
            activeSpeaker = null;
            head.setRotation(BASE_YAW, 0.0F);
            return;
        }
        Location from = head.getLocation();
        Location to = player.getEyeLocation();
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal * horizontal + dy * dy
                > MorrowBodyAuthority.MAXIMUM_FOCUS_DISTANCE * MorrowBodyAuthority.MAXIMUM_FOCUS_DISTANCE) {
            activeSpeaker = null;
            head.setRotation(BASE_YAW, 0.0F);
            return;
        }
        float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float delta = normalizeYaw(desiredYaw - BASE_YAW);
        float yaw = BASE_YAW + clamp(delta, -MorrowBodyAuthority.MAXIMUM_HEAD_YAW,
                MorrowBodyAuthority.MAXIMUM_HEAD_YAW);
        float pitch = clamp((float) -Math.toDegrees(Math.atan2(dy, horizontal)),
                -MorrowBodyAuthority.MAXIMUM_HEAD_PITCH, MorrowBodyAuthority.MAXIMUM_HEAD_PITCH);
        head.setRotation(yaw, pitch);
    }

    private Location anchor(boolean embodied) {
        return embodied
                ? new Location(world, origin.x() + 0.5, origin.y(), origin.z() + 0.5, BASE_YAW, 0.0F)
                : new Location(world, origin.x() + 0.5, origin.y(), origin.z() + 2.5, BASE_YAW, 0.0F);
    }

    private boolean owned(Entity entity) {
        return OWNER_VALUE.equals(entity.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING));
    }

    private void cleanupOwned() {
        requirePrimaryThread();
        for (Entity entity : List.copyOf(world.getEntities())) {
            if (owned(entity)) entity.remove();
        }
        entities.clear();
        head = null;
        interaction = null;
        activeSpeaker = null;
        focusUntilMillis = 0L;
    }

    @Override
    public void close() {
        requirePrimaryThread();
        cancelDeferredAudit();
        if (trackingTask != null) trackingTask.cancel();
        trackingTask = null;
        cleanupOwned();
        currentPose = null;
        currentRevision = -1L;
    }

    private void cancelDeferredAudit() {
        if (deferredAuditTask != null) deferredAuditTask.cancel();
        deferredAuditTask = null;
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        if (normalized > 180.0F) normalized -= 360.0F;
        if (normalized < -180.0F) normalized += 360.0F;
        return normalized;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Morrow fallback body requires the Paper primary thread");
        }
    }
}
