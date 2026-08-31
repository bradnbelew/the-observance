package com.observance.watcher.morrow.room04.replay;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.MorrowRelationshipSnapshot;
import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Action;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.BoundaryPhase;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Clip;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.ConsentBinding;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Decision;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Pose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Provenance;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Purpose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Sample;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Main-thread Paper adapter for bounded M03/M04 capture and display-only echoes. */
public final class BukkitEntityReplay implements Listener, AutoCloseable {
    private static final String OWNER = "morrow:recovery_room_04:entity_replay:v1";
    private static final Pattern CLIP_HASH = Pattern.compile("\\\"clip_hash\\\":\\\"([0-9a-f]{64})\\\"");
    private static final float LABEL_SCALE = 0.24F;

    private final JavaPlugin plugin;
    private final World world;
    private final Origin origin;
    private final MorrowLocalState state;
    private final EntityReplayConsentStore consents;
    private final EntityReplayClipStore clips;
    private final EntityReplayRecorder recorder;
    private final NamespacedKey ownerKey;
    private final NamespacedKey releaseKey;
    private final NamespacedKey kindKey;
    private final NamespacedKey provenanceKey;
    private final NamespacedKey expiresKey;
    private final Map<UUID, EnumSet<Action>> pendingActions = new HashMap<>();
    private final Map<UUID, Integer> entryGraceTicks = new HashMap<>();
    private final Map<UUID, Double> lastVerticalVelocity = new HashMap<>();
    private final List<Entity> echoes = new ArrayList<>();
    private final Map<UUID, Clip> lastClips = new HashMap<>();
    private BukkitTask samplingTask;
    private BukkitTask replayTask;
    private BukkitTask cleanupTask;
    private Interaction transferInteraction;
    private Interaction sealInteraction;
    private Interaction proofInteraction;
    private Clip replaying;
    private int replayTick;
    private boolean started;

    public BukkitEntityReplay(JavaPlugin plugin, World world, Origin origin, MorrowLocalState state, Path data)
            throws IOException {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.state = Objects.requireNonNull(state, "state");
        this.consents = new EntityReplayConsentStore(data.resolve("morrow-entity-replay-consent"), state.releaseId());
        this.clips = new EntityReplayClipStore(data.resolve("morrow-entity-replay-clips"), state.releaseId());
        this.recorder = new EntityReplayRecorder(state.releaseId(), clips);
        this.ownerKey = new NamespacedKey(plugin, "morrow_replay_owner");
        this.releaseKey = new NamespacedKey(plugin, "morrow_replay_release");
        this.kindKey = new NamespacedKey(plugin, "morrow_replay_kind");
        this.provenanceKey = new NamespacedKey(plugin, "morrow_replay_provenance");
        this.expiresKey = new NamespacedKey(plugin, "morrow_replay_expires");
        requirePrimaryThread();
    }

    public void start() throws IOException {
        requirePrimaryThread();
        if (started) return;
        cleanupOwned();
        spawnAffordances();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        samplingTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::samplingTick, 2L, 2L);
        started = true;
        catchUp();
    }

    public String optIn(Player player) throws IOException {
        requirePrimaryThread();
        if (!state.snapshot().committedEvents().contains(
                com.observance.watcher.morrow.dialog.MorrowDialogAuthority.ENTITY_REPLAY_AUTHORIZED)) {
            return "The group Entity Replay capability is not authorized. Nothing was recorded.";
        }
        Purpose purpose = currentPurpose();
        if (purpose == null) return "This replay phase has already been completed.";
        if (recorder.active(player.getUniqueId())) return "Your bounded recording is already active.";
        ConsentBinding consent = consents.grant(player.getUniqueId(), purpose);
        recorder.start(consent);
        entryGraceTicks.put(player.getUniqueId(), EntityReplayAuthority.ENTRY_GRACE_TICKS);
        return purpose == Purpose.MISSING_ROLE
                ? "Local consent revision " + consent.revision() + " is armed for 20 seconds. Enter the marked missing position; tick zero begins on entry. Target pulse 21–22; local latency grace accepts pulses 19–25."
                : "Local consent revision " + consent.revision() + " is armed for 20 seconds. Enter the marked boundary; tick zero begins on entry. Make a distinctive route, then use the illuminated in-boundary seal control.";
    }

    public String revoke(Player player) throws IOException {
        requirePrimaryThread();
        Purpose purpose = recorder.purpose(player.getUniqueId()).orElseGet(this::currentPurpose);
        if (purpose == null) {
            Clip sealed = lastClips.get(player.getUniqueId());
            if (sealed != null) purpose = sealed.purpose();
        }
        if (purpose == null) return "No replay consent is active.";
        consents.revoke(player.getUniqueId(), purpose);
        recorder.cancel(player.getUniqueId());
        pendingActions.remove(player.getUniqueId());
        entryGraceTicks.remove(player.getUniqueId());
        if (purpose == Purpose.MISSING_ROLE
                || replaying != null && replaying.playerId().equals(player.getUniqueId())) stopReplay();
        return "Consent was revoked. Your unsealed capture and any active echo were removed.";
    }

    public String cancel(Player player) {
        requirePrimaryThread();
        boolean removed = recorder.cancel(player.getUniqueId());
        pendingActions.remove(player.getUniqueId());
        entryGraceTicks.remove(player.getUniqueId());
        if (replaying != null && replaying.playerId().equals(player.getUniqueId())) stopReplay();
        return removed ? "The unsealed local clip was deleted; consent remains available for a fresh start."
                : "No unsealed local clip existed.";
    }

    public String seal(Player player) throws IOException {
        requirePrimaryThread();
        UUID playerId = player.getUniqueId();
        if (recorder.purpose(playerId).orElse(null) != Purpose.DELIBERATE_LIVE_TEST) {
            return "Only the deliberate M04 test has a manual seal control.";
        }
        int duration = recorder.sampleCount(playerId) * EntityReplayAuthority.SAMPLE_INTERVAL_TICKS;
        Optional<Clip> sealed = recorder.seal(playerId, duration);
        if (sealed.isEmpty()) return "No active clip was available to seal.";
        return evaluateLive(player, sealed.orElseThrow());
    }

    public String restartReplay(Player player) throws IOException {
        requirePrimaryThread();
        Clip clip = lastClips.get(player.getUniqueId());
        if (clip == null) clip = loadReceiptClip(EntityReplayAuthority.LIVE_TEST_RECORDED).orElse(null);
        if (clip == null) return "No locally verified deliberate clip is available.";
        ConsentBinding consent = consents.current(clip.playerId(), clip.purpose()).orElse(null);
        if (!matches(clip, consent)) return "The source player's current consent no longer permits this replay.";
        startReplay(clip, Provenance.RECONSTRUCTED);
        return "The exact sealed route is replaying as a reconstructed display echo.";
    }

    public void catchUpFor(Player player) throws IOException {
        requirePrimaryThread();
        if (!state.snapshot().committedEvents().contains(EntityReplayAuthority.LIVE_TEST_RECORDED)
                || state.snapshot().committedEvents().contains(EntityReplayAuthority.BEHAVIOR_REUSE_PROVEN)) return;
        Clip clip = loadReceiptClip(EntityReplayAuthority.LIVE_TEST_RECORDED).orElse(null);
        if (clip == null || !clip.playerId().equals(player.getUniqueId())) return;
        ConsentBinding consent = consents.current(clip.playerId(), clip.purpose()).orElse(null);
        if (matches(clip, consent)) {
            lastClips.put(clip.playerId(), clip);
            startReplay(clip, Provenance.RECONSTRUCTED);
            player.sendMessage(Component.text(
                    "Entity Replay catch-up: your verified sealed route restarted at tick zero with reconstructed provenance.",
                    NamedTextColor.GRAY));
        }
    }

    public boolean isOwnedInteraction(Entity entity) {
        requirePrimaryThread();
        return entity instanceof Interaction && owned(entity);
    }

    public int ownedEntityCount() {
        requirePrimaryThread();
        return (int) world.getEntities().stream().filter(this::owned).count();
    }

    private void samplingTick() {
        requirePrimaryThread();
        for (UUID playerId : new ArrayList<>(pendingPlayerIds())) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline() || player.getWorld() != world) {
                recorder.cancel(playerId);
                pendingActions.remove(playerId);
                entryGraceTicks.remove(playerId);
                continue;
            }
            Purpose purpose = recorder.purpose(playerId).orElse(null);
            if (purpose == null) continue;
            ConsentBinding consent;
            try {
                consent = consents.current(playerId, purpose).orElse(null);
            } catch (IOException failure) {
                cancelWithFeedback(player, "Consent audit failed; the unsealed clip was deleted.", failure);
                continue;
            }
            if (consent == null) {
                recorder.cancel(playerId);
                pendingActions.remove(playerId);
                entryGraceTicks.remove(playerId);
                player.sendMessage(Component.text("Consent was absent; the unsealed clip was deleted.", NamedTextColor.YELLOW));
                continue;
            }
            Location location = player.getLocation();
            double verticalVelocity = player.getVelocity().getY();
            Double previousVelocity = lastVerticalVelocity.put(playerId, verticalVelocity);
            if (verticalVelocity > 0.2D && (previousVelocity == null || previousVelocity <= 0.2D)) {
                addAction(player, Action.JUMP);
            }
            double x = location.getX() - origin.x();
            double y = location.getY() - origin.y();
            double z = location.getZ() - origin.z();
            int sampleCount = recorder.sampleCount(playerId);
            int grace = entryGraceTicks.getOrDefault(playerId, 0);
            BoundaryPhase boundary = EntityReplayAuthority.boundaryPhase(
                    sampleCount, grace, EntityReplayAuthority.RECORDING_BOUNDARY.contains(x, y, z));
            if (boundary == BoundaryPhase.ARMED_FOR_ENTRY) {
                entryGraceTicks.put(playerId, Math.max(0, grace - EntityReplayAuthority.SAMPLE_INTERVAL_TICKS));
                continue;
            }
            if (boundary == BoundaryPhase.CANCELLED) {
                recorder.cancel(playerId);
                pendingActions.remove(playerId);
                entryGraceTicks.remove(playerId);
                player.sendMessage(Component.text(sampleCount == 0
                        ? "The 20-second entry window expired; no samples or receipt were retained."
                        : "You left the visible recording boundary; the unsealed clip was deleted.", NamedTextColor.YELLOW));
                continue;
            }
            entryGraceTicks.remove(playerId);
            int tick = sampleCount * EntityReplayAuthority.SAMPLE_INTERVAL_TICKS;
            if (purpose == Purpose.MISSING_ROLE && tick == 0 && replayTask == null) {
                startAuthoredMissingRoleEcho();
            }
            Sample sample = new Sample(tick, x, y, z, location.getYaw(), location.getPitch(), pose(player),
                    player.getInventory().getHeldItemSlot(), drainActions(playerId));
            EntityReplayRecorder.CaptureResult result = recorder.capture(consent, sample);
            if (result.status() == EntityReplayRecorder.CaptureStatus.CANCELLED) {
                player.sendMessage(Component.text(result.feedback(), NamedTextColor.YELLOW));
            } else if (result.status() == EntityReplayRecorder.CaptureStatus.COMPLETE) {
                try {
                    Clip clip = recorder.seal(playerId, (tick + 2)).orElseThrow();
                    String feedback = purpose == Purpose.MISSING_ROLE ? evaluateMissing(player, clip) : evaluateLive(player, clip);
                    player.sendMessage(Component.text(feedback, NamedTextColor.AQUA));
                } catch (IOException | RuntimeException failure) {
                    cancelWithFeedback(player, "The local clip could not be verified; no consequence or receipt was created.", failure);
                }
            }
        }
    }

    private String evaluateMissing(Player player, Clip clip) throws IOException {
        Decision decision = EntityReplayAuthority.missingRole(state.snapshot(), clip);
        if (!decision.commitsReceipt()) {
            stopReplay();
            return decision.feedback();
        }
        if (state.snapshot().committedEvents().contains(EntityReplayAuthority.MISSING_ROLE_COMPLETED)) {
            stopReplay();
            return "The group 37-second missing-role receipt is already retained; your local clip created no duplicate.";
        }
        state.commit(decision.eventKey(), decision.idempotencyKey(), decision.payload());
        lastClips.put(player.getUniqueId(), clip);
        stopReplay();
        return decision.feedback() + " Open the terminal and opt into the deliberate movement test.";
    }

    private String evaluateLive(Player player, Clip clip) throws IOException {
        Decision decision = EntityReplayAuthority.liveTest(state.snapshot(), clip);
        if (!decision.commitsReceipt()) return decision.feedback();
        if (state.snapshot().committedEvents().contains(EntityReplayAuthority.LIVE_TEST_RECORDED)) {
            return "The group deliberate-test receipt is already retained; your local clip created no duplicate.";
        }
        state.commit(decision.eventKey(), decision.idempotencyKey(), decision.payload());
        lastClips.put(player.getUniqueId(), clip);
        startReplay(clip, Provenance.RECONSTRUCTED);
        return decision.feedback() + " Watch its reconstructed echo, then authenticate the route at the physical marker.";
    }

    private void startAuthoredMissingRoleEcho() {
        resetScene();
        BlockDisplay echo = spawnEcho(new Location(world, origin.x() + 1.4, origin.y() + 0.2, origin.z() - 1.3),
                Material.LIGHT_BLUE_STAINED_GLASS, Provenance.RECORDED, "m03_recorded_partial_avatar");
        echoes.add(echo);
        echoes.add(spawnLabel(echo.getLocation().add(0, 1.4, 0),
                "RECORDED — partial avatar / 37.0 s\nTarget 21–22; latency grace 19–25", Provenance.RECORDED));
        TextDisplay pulse = spawnLabel(new Location(world, origin.x() + 1.1, origin.y() + 2.2, origin.z() - 3.6),
                "REDSTONE PULSE LOG — 00 / 37", Provenance.RECORDED);
        echoes.add(pulse);
        echoes.add(spawnLabel(new Location(world, origin.x() + 3.7, origin.y() + 2.2, origin.z() - 3.6),
                "CAPTIONED VOICE FRAGMENT\n[archived] Position four: ready the selected manifest for pulses 21–22.",
                Provenance.RECORDED));
        final int[] tick = {0};
        replayTask = new BukkitRunnable() {
            @Override public void run() {
                tick[0] += 2;
                double angle = tick[0] / 40.0;
                echo.teleport(new Location(world, origin.x() + 1.4 + Math.sin(angle) * 0.35,
                        origin.y() + 0.2, origin.z() - 1.3 + Math.cos(angle) * 0.2));
                pulse.text(Component.text(String.format(java.util.Locale.ROOT,
                        "REDSTONE PULSE LOG — %02d / 37", Math.min(37, tick[0] / 20)), NamedTextColor.AQUA));
                if (tick[0] >= EntityReplayAuthority.MISSING_ROLE_DURATION_TICKS) cancel();
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private void startReplay(Clip clip, Provenance provenance) {
        resetScene();
        replaying = clip;
        replayTick = 0;
        Sample first = clip.samples().get(0);
        BlockDisplay echo = spawnEcho(absolute(first), Material.MAGENTA_STAINED_GLASS, provenance, "m04_echo");
        echoes.add(echo);
        echoes.add(spawnLabel(absolute(first).add(0, 1.4, 0),
                "RECONSTRUCTED — current recording hash " + clip.clipHash().substring(0, 12), provenance));
        replayTask = new BukkitRunnable() {
            @Override public void run() {
                try {
                    ConsentBinding consent = consents.current(clip.playerId(), clip.purpose()).orElse(null);
                    if (!matches(clip, consent)) {
                        stopReplay();
                        return;
                    }
                } catch (IOException failure) {
                    stopReplay();
                    return;
                }
                replayTick += 2;
                Sample target = sampleAt(clip.samples(), replayTick);
                echo.setTeleportDuration(2);
                echo.teleport(absolute(target));
                if (replayTick >= clip.durationTicks()) {
                    cancel();
                    replayTask = null;
                    spawnProofInteraction();
                    cleanupTask = plugin.getServer().getScheduler().runTaskLater(
                            plugin, BukkitEntityReplay.this::stopReplay, 600L);
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteraction(PlayerInteractEntityEvent event) {
        if (!started || event.getHand() != EquipmentSlot.HAND || !isOwnedInteraction(event.getRightClicked())) return;
        event.setCancelled(true);
        String kind = event.getRightClicked().getPersistentDataContainer().get(kindKey, PersistentDataType.STRING);
        if ("transfer".equals(kind)) {
            addAction(event.getPlayer(), Action.INTERACT);
            addAction(event.getPlayer(), Action.INVENTORY_TRANSFER);
            event.getPlayer().sendMessage(Component.text(
                    "Designated manifest transfer observed locally; no inventory item or block was changed.", NamedTextColor.AQUA));
        } else if ("seal".equals(kind)) {
            if (recorder.purpose(event.getPlayer().getUniqueId()).orElse(null) != Purpose.DELIBERATE_LIVE_TEST) {
                event.getPlayer().sendMessage(Component.text(
                        "The in-boundary seal control is available only during an active M04 deliberate route.",
                        NamedTextColor.YELLOW));
                return;
            }
            try {
                event.getPlayer().sendMessage(Component.text(seal(event.getPlayer()), NamedTextColor.AQUA));
            } catch (IOException | RuntimeException failure) {
                cancelWithFeedback(event.getPlayer(),
                        "The in-boundary seal failed safely; no receipt or echo was created.", failure);
            }
        } else if ("proof".equals(kind) && replaying != null) {
            Decision decision = EntityReplayAuthority.behaviorReuse(state.snapshot(), replaying,
                    replaying.clipHash(), replayTick);
            if (!decision.commitsReceipt()) {
                event.getPlayer().sendMessage(Component.text(decision.feedback(), NamedTextColor.YELLOW));
                return;
            }
            try {
                MorrowLocalState.CommitResult result = state.commit(
                        decision.eventKey(), decision.idempotencyKey(), decision.payload());
                event.getPlayer().sendMessage(Component.text(result.created() ? decision.feedback()
                        : "Behavior reuse is already retained; nothing duplicated.", NamedTextColor.AQUA));
                stopReplay();
            } catch (IOException | RuntimeException failure) {
                event.getPlayer().sendMessage(Component.text("The comparison receipt failed safely; retry the exact replay.", NamedTextColor.RED));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnimation(PlayerAnimationEvent event) { addAction(event.getPlayer(), Action.SWING); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) { addAction(event.getPlayer(), Action.CROUCH); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) { addAction(event.getPlayer(), Action.DROP); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.PHYSICAL) addAction(event.getPlayer(), Action.INTERACT);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Purpose purpose = recorder.purpose(event.getPlayer().getUniqueId()).orElse(null);
        recorder.cancel(event.getPlayer().getUniqueId());
        pendingActions.remove(event.getPlayer().getUniqueId());
        entryGraceTicks.remove(event.getPlayer().getUniqueId());
        lastVerticalVelocity.remove(event.getPlayer().getUniqueId());
        if (purpose == Purpose.MISSING_ROLE
                || replaying != null && replaying.playerId().equals(event.getPlayer().getUniqueId())) stopReplay();
    }

    private void addAction(Player player, Action action) {
        if (recorder.active(player.getUniqueId())) {
            pendingActions.computeIfAbsent(player.getUniqueId(), ignored -> EnumSet.noneOf(Action.class)).add(action);
        }
    }

    private EnumSet<Action> drainActions(UUID player) {
        EnumSet<Action> actions = pendingActions.remove(player);
        return actions == null ? EnumSet.noneOf(Action.class) : actions;
    }

    private List<UUID> pendingPlayerIds() {
        return new ArrayList<>(recorder.activePlayers());
    }

    private Purpose currentPurpose() {
        MorrowRelationshipSnapshot snapshot = state.snapshot();
        if (!snapshot.committedEvents().contains(EntityReplayAuthority.MISSING_ROLE_COMPLETED)) return Purpose.MISSING_ROLE;
        if (!snapshot.committedEvents().contains(EntityReplayAuthority.LIVE_TEST_RECORDED)) return Purpose.DELIBERATE_LIVE_TEST;
        return null;
    }

    private void catchUp() throws IOException {
        if (state.snapshot().committedEvents().contains(EntityReplayAuthority.LIVE_TEST_RECORDED)
                && !state.snapshot().committedEvents().contains(EntityReplayAuthority.BEHAVIOR_REUSE_PROVEN)) {
            Optional<Clip> clip = loadReceiptClip(EntityReplayAuthority.LIVE_TEST_RECORDED);
            if (clip.isPresent()) {
                lastClips.put(clip.orElseThrow().playerId(), clip.orElseThrow());
                ConsentBinding consent = consents.current(clip.orElseThrow().playerId(), clip.orElseThrow().purpose()).orElse(null);
                Player source = plugin.getServer().getPlayer(clip.orElseThrow().playerId());
                if (source != null && source.isOnline() && source.getWorld() == world
                        && matches(clip.orElseThrow(), consent)) {
                    startReplay(clip.orElseThrow(), Provenance.RECONSTRUCTED);
                }
            }
        }
    }

    private Optional<Clip> loadReceiptClip(String event) throws IOException {
        Optional<byte[]> payload = state.latestPayload(event);
        if (payload.isEmpty()) return Optional.empty();
        Matcher matcher = CLIP_HASH.matcher(new String(payload.orElseThrow(), StandardCharsets.UTF_8));
        if (!matcher.find()) throw new IOException("entity replay receipt omitted its clip hash");
        return clips.load(matcher.group(1));
    }

    private void spawnTransferInteraction() {
        Location location = new Location(world, origin.x() + 2.5, origin.y() + 1.0, origin.z() - 2.5);
        transferInteraction = world.spawn(location, Interaction.class, entity -> {
            configure(entity, "transfer", Provenance.LIVE);
            entity.setInteractionWidth(0.8F);
            entity.setInteractionHeight(1.4F);
            entity.setResponsive(true);
        });
        echoes.add(transferInteraction);
        echoes.add(spawnLabel(location.clone().add(0, 1.1, 0),
                "LIVE INPUT — missing-role manifest transfer", Provenance.LIVE));
    }

    private void spawnAffordances() {
        spawnTransferInteraction();
        spawnSealInteraction();
        double[][] corners = {{1.0, -0.6}, {3.9, -0.6}, {1.0, -3.9}, {3.9, -3.9}};
        for (int index = 0; index < corners.length; index++) {
            echoes.add(spawnLabel(new Location(world, origin.x() + corners[index][0],
                    origin.y() + 0.15, origin.z() + corners[index][1]),
                    "LIVE RECORDING BOUNDARY " + (index + 1) + "/4", Provenance.LIVE));
        }
    }

    private void spawnSealInteraction() {
        Location location = new Location(world, origin.x() + 3.45, origin.y() + 1.0, origin.z() - 0.95);
        sealInteraction = world.spawn(location, Interaction.class, entity -> {
            configure(entity, "seal", Provenance.LIVE);
            entity.setInteractionWidth(0.8F);
            entity.setInteractionHeight(1.4F);
            entity.setResponsive(true);
        });
        echoes.add(sealInteraction);
        echoes.add(spawnLabel(location.clone().add(0, 1.1, 0),
                "LIVE SEAL CONTROL — deliberate route / 6–45 s", Provenance.LIVE));
    }

    private void spawnProofInteraction() {
        Location location = new Location(world, origin.x() + 1.45, origin.y() + 1.0, origin.z() - 3.45);
        proofInteraction = world.spawn(location, Interaction.class, entity -> {
            configure(entity, "proof", Provenance.RECONSTRUCTED);
            entity.setInteractionWidth(1.0F);
            entity.setInteractionHeight(1.8F);
            entity.setResponsive(true);
        });
        echoes.add(proofInteraction);
        echoes.add(spawnLabel(location.clone().add(0, 1.3, 0),
                "RECONSTRUCTED PROOF — authenticate exact route", Provenance.RECONSTRUCTED));
    }

    private BlockDisplay spawnEcho(Location location, Material material, Provenance provenance, String kind) {
        return world.spawn(location, BlockDisplay.class, entity -> {
            configure(entity, kind, provenance);
            entity.setBlock(Bukkit.createBlockData(material));
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setInterpolationDuration(2);
            entity.setTeleportDuration(2);
            entity.setViewRange(20.0F);
        });
    }

    private TextDisplay spawnLabel(Location location, String text, Provenance provenance) {
        return world.spawn(location, TextDisplay.class, entity -> {
            configure(entity, "label", provenance);
            entity.text(Component.text(text, NamedTextColor.AQUA));
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setLineWidth(240);
            entity.setViewRange(20.0F);
            entity.setTransformation(new Transformation(
                    new Vector3f(), new AxisAngle4f(), new Vector3f(LABEL_SCALE), new AxisAngle4f()));
        });
    }

    private void configure(Entity entity, String kind, Provenance provenance) {
        entity.setPersistent(false);
        entity.setGravity(false);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(ownerKey, PersistentDataType.STRING, OWNER);
        pdc.set(releaseKey, PersistentDataType.STRING, state.releaseId());
        pdc.set(kindKey, PersistentDataType.STRING, kind);
        pdc.set(provenanceKey, PersistentDataType.STRING, provenance.name().toLowerCase(java.util.Locale.ROOT));
        pdc.set(expiresKey, PersistentDataType.LONG, System.currentTimeMillis() + 120_000L);
    }

    private Location absolute(Sample sample) {
        return new Location(world, origin.x() + sample.x(), origin.y() + sample.y(), origin.z() + sample.z(),
                sample.yaw(), sample.pitch());
    }

    private static Sample sampleAt(List<Sample> samples, int tick) {
        Sample selected = samples.get(0);
        for (Sample sample : samples) {
            if (sample.tick() > tick) break;
            selected = sample;
        }
        return selected;
    }

    private static Pose pose(Player player) {
        if (player.isGliding()) return Pose.FALL_FLYING;
        if (player.isSwimming()) return Pose.SWIMMING;
        if (player.isSneaking()) return Pose.CROUCHING;
        return Pose.STANDING;
    }

    private static boolean matches(Clip clip, ConsentBinding consent) {
        return consent != null && consent.granted() && consent.revision() == clip.consentRevision()
                && consent.consentHash().equals(clip.consentHash()) && consent.playerId().equals(clip.playerId())
                && consent.purpose() == clip.purpose() && consent.releaseId().equals(clip.releaseId());
    }

    private boolean owned(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return OWNER.equals(pdc.get(ownerKey, PersistentDataType.STRING))
                && state.releaseId().equals(pdc.get(releaseKey, PersistentDataType.STRING));
    }

    private void cancelWithFeedback(Player player, String text, Exception failure) {
        recorder.cancel(player.getUniqueId());
        pendingActions.remove(player.getUniqueId());
        entryGraceTicks.remove(player.getUniqueId());
        player.sendMessage(Component.text(text, NamedTextColor.RED));
        plugin.getLogger().warning("Morrow Entity Replay halted safely: " + safe(failure.getMessage()));
    }

    private void stopReplay() {
        requirePrimaryThread();
        if (replayTask != null) replayTask.cancel();
        replayTask = null;
        if (cleanupTask != null) cleanupTask.cancel();
        cleanupTask = null;
        replaying = null;
        replayTick = 0;
        cleanupEchoes();
        if (started) spawnAffordances();
    }

    private void resetScene() {
        if (replayTask != null) replayTask.cancel();
        replayTask = null;
        if (cleanupTask != null) cleanupTask.cancel();
        cleanupTask = null;
        replaying = null;
        replayTick = 0;
        cleanupEchoes();
        spawnAffordances();
    }

    private void cleanupEchoes() {
        for (Entity entity : new ArrayList<>(world.getEntities())) if (owned(entity)) entity.remove();
        echoes.clear();
        transferInteraction = null;
        sealInteraction = null;
        proofInteraction = null;
    }

    private void cleanupOwned() { cleanupEchoes(); }

    @Override public void close() {
        requirePrimaryThread();
        started = false;
        HandlerList.unregisterAll(this);
        if (samplingTask != null) samplingTask.cancel();
        samplingTask = null;
        if (replayTask != null) replayTask.cancel();
        replayTask = null;
        if (cleanupTask != null) cleanupTask.cancel();
        cleanupTask = null;
        recorder.cancelAll();
        pendingActions.clear();
        entryGraceTicks.clear();
        lastVerticalVelocity.clear();
        cleanupOwned();
    }

    private static String safe(String message) {
        return message == null ? "unknown" : message.replaceAll("[\\r\\n]+", " ");
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Morrow Entity Replay requires the Paper primary thread");
    }
}
