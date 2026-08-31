package com.observance.watcher.morrow.room04.replay;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Action;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Clip;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.ConsentBinding;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Decision;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Pose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Purpose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Sample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Main-driven M03/M04 bounds, privacy, consent, persistence, replay, and group matrix tests. */
public final class EntityReplayLoopSelfTest {
    private static final String RELEASE = "morrow.rehearsal.001";

    private EntityReplayLoopSelfTest() { }

    public static void main(String[] args) throws Exception {
        authorityBoundsAndCompression();
        consentRevocationAndPlayerMatrix();
        missingRoleAndLocalOutage();
        deliberateReplayRestartAndTiming();
        corruptionAndWrongRelease();
        paperAdapterContract();
        System.out.println("MORROW ENTITY REPLAY M03/M04: PASS seconds=37/45 interval=2 samples=370/450 players=1/2/6");
    }

    private static void authorityBoundsAndCompression() {
        check(EntityReplayAuthority.MISSING_ROLE_DURATION_TICKS == 740
                        && EntityReplayAuthority.MISSING_ROLE_SAMPLE_COUNT == 370,
                "M03 is authored at exactly 37 seconds and two-tick sampling");
        check(EntityReplayAuthority.TRANSFER_WINDOW_START_TICK == 380
                        && EntityReplayAuthority.TRANSFER_WINDOW_END_TICK == 500,
                "M03 keeps pulses 21–22 as its target with a bounded disclosed local-latency grace");
        check(EntityReplayAuthority.MAXIMUM_DURATION_TICKS == 900
                        && EntityReplayAuthority.MAXIMUM_SAMPLES_PER_PLAYER == 450
                        && EntityReplayAuthority.MAXIMUM_TRACKED_PLAYERS == 6
                        && EntityReplayAuthority.ENTRY_GRACE_TICKS == 400,
                "recorder hard bounds are exact");
        check(EntityReplayAuthority.boundaryPhase(0, 400, false)
                        == EntityReplayAuthority.BoundaryPhase.ARMED_FOR_ENTRY
                        && EntityReplayAuthority.boundaryPhase(0, 2, false)
                        == EntityReplayAuthority.BoundaryPhase.ARMED_FOR_ENTRY
                        && EntityReplayAuthority.boundaryPhase(0, 0, false)
                        == EntityReplayAuthority.BoundaryPhase.CANCELLED
                        && EntityReplayAuthority.boundaryPhase(1, 400, false)
                        == EntityReplayAuthority.BoundaryPhase.CANCELLED
                        && EntityReplayAuthority.boundaryPhase(0, 0, true)
                        == EntityReplayAuthority.BoundaryPhase.RECORDING,
                "terminal opt-in arms entry, tick zero begins inside, and post-start exit cancels");
        expectIllegal(() -> EntityReplayAuthority.boundaryPhase(-1, 0, false));
        List<Sample> redundant = List.of(sample(0, 1.5, -1.5, Set.of()), sample(2, 1.5, -1.5, Set.of()),
                sample(4, 1.5, -1.5, Set.of(Action.SWING)), sample(6, 1.5, -1.5, Set.of()));
        List<Sample> compressed = EntityReplayAuthority.compress(redundant);
        check(compressed.size() == 3 && compressed.get(1).actions().contains(Action.SWING)
                        && compressed.get(2).tick() == 6,
                "compression removes redundancy but retains designated action and final samples");
        expectIllegal(() -> new Sample(1, 1.5, 1, -1.5, 0, 0, Pose.STANDING, 0, Set.of()));
        expectIllegal(() -> sample(900, 1.5, -1.5, Set.of()));
        expectIllegal(() -> new Sample(0, 9, 1, -1.5, 0, 0, Pose.STANDING, 0, Set.of()));
    }

    private static void consentRevocationAndPlayerMatrix() throws Exception {
        Path root = Files.createTempDirectory("morrow-replay-consent-");
        try {
            EntityReplayConsentStore consentStore = new EntityReplayConsentStore(root.resolve("consent"), RELEASE);
            EntityReplayClipStore clipStore = new EntityReplayClipStore(root.resolve("clips"), RELEASE);
            EntityReplayRecorder recorder = new EntityReplayRecorder(RELEASE, clipStore);
            UUID noConsent = UUID.randomUUID();
            String deniedHash = EntityReplayAuthority.consentHash(RELEASE, noConsent, Purpose.MISSING_ROLE, 1, false);
            expectState(() -> recorder.start(new ConsentBinding(
                    RELEASE, noConsent, Purpose.MISSING_ROLE, 1, false, deniedHash)));

            UUID revokedPlayer = UUID.randomUUID();
            ConsentBinding granted = consentStore.grant(revokedPlayer, Purpose.MISSING_ROLE);
            recorder.start(granted);
            recorder.capture(granted, sample(0, 2.5, -2.5, Set.of()));
            ConsentBinding revoked = consentStore.revoke(revokedPlayer, Purpose.MISSING_ROLE);
            check(recorder.capture(revoked, sample(2, 2.5, -2.5, Set.of())).status()
                            == EntityReplayRecorder.CaptureStatus.CANCELLED
                            && !recorder.active(revokedPlayer),
                    "revocation immediately deletes an unsealed clip");

            List<UUID> players = new ArrayList<>();
            for (int index = 0; index < 6; index++) {
                UUID player = UUID.randomUUID();
                players.add(player);
                recorder.start(consentStore.grant(player, Purpose.DELIBERATE_LIVE_TEST));
            }
            check(recorder.activeCount() == 6, "six independently consenting players can record");
            UUID seventh = UUID.randomUUID();
            expectState(() -> recorder.start(consentStore.grant(seventh, Purpose.DELIBERATE_LIVE_TEST)));
            recorder.cancel(players.get(0));
            check(recorder.activeCount() == 5 && consentStore.current(players.get(1), Purpose.DELIBERATE_LIVE_TEST)
                            .orElseThrow().granted(),
                    "one player's cancel does not alter another player's consent");
            recorder.cancelAll();

            for (int count : new int[]{1, 2, 6}) {
                EntityReplayRecorder group = new EntityReplayRecorder(RELEASE, clipStore);
                for (int index = 0; index < count; index++) {
                    UUID player = UUID.randomUUID();
                    group.start(consentStore.grant(player, Purpose.DELIBERATE_LIVE_TEST));
                }
                check(group.activeCount() == count, count + "-player session remains isolated and bounded");
            }

            UUID timeoutPlayer = UUID.randomUUID();
            ConsentBinding timeoutConsent = consentStore.grant(timeoutPlayer, Purpose.DELIBERATE_LIVE_TEST);
            EntityReplayRecorder timeout = new EntityReplayRecorder(RELEASE, clipStore);
            timeout.start(timeoutConsent);
            EntityReplayRecorder.CaptureResult last = null;
            for (int index = 0; index < 450; index++) {
                last = timeout.capture(timeoutConsent, sample(index * 2, 1.5, -1.5, Set.of()));
            }
            check(last != null && last.status() == EntityReplayRecorder.CaptureStatus.COMPLETE
                            && timeout.seal(timeoutPlayer, 900).orElseThrow().rawSampleCount() == 450,
                    "45-second timeout seals at exactly 450 two-tick samples and cannot grow");
        } finally {
            deleteTree(root);
        }
    }

    private static void missingRoleAndLocalOutage() throws Exception {
        Path root = Files.createTempDirectory("morrow-replay-m03-");
        try {
            MorrowLocalState state = authorizedState(root.resolve("state.journal"));
            EntityReplayConsentStore consents = new EntityReplayConsentStore(root.resolve("consent"), RELEASE);
            EntityReplayClipStore clips = new EntityReplayClipStore(root.resolve("clips"), RELEASE);
            UUID player = UUID.randomUUID();
            ConsentBinding consent = consents.grant(player, Purpose.MISSING_ROLE);
            Clip partial = signedClip(clips, consent, 200, Purpose.MISSING_ROLE, true);
            check(EntityReplayAuthority.missingRole(state.snapshot(), partial).status()
                            == EntityReplayAuthority.Status.WRONG,
                    "partial M03 loop resets without proof");
            Clip correct = signedClip(clips, consent, 740, Purpose.MISSING_ROLE, true);
            Decision decision = EntityReplayAuthority.missingRole(state.snapshot(), correct);
            check(EntityReplayAuthority.missingRole(state.snapshot(),
                            signedMissingClipAt(clips, consent, 380)).commitsReceipt()
                            && EntityReplayAuthority.missingRole(state.snapshot(),
                            signedMissingClipAt(clips, consent, 500)).commitsReceipt()
                            && EntityReplayAuthority.missingRole(state.snapshot(),
                            signedMissingClipAt(clips, consent, 378)).status() == EntityReplayAuthority.Status.WRONG
                            && EntityReplayAuthority.missingRole(state.snapshot(),
                            signedMissingClipAt(clips, consent, 502)).status() == EntityReplayAuthority.Status.WRONG,
                    "M03 accepts only the disclosed pulse 19–25 latency grace, including both exact boundaries");
            check(decision.commitsReceipt() && new String(decision.payload(), StandardCharsets.UTF_8)
                            .contains("\"raw_samples_remote\":false")
                            && !new String(decision.payload(), StandardCharsets.UTF_8).contains("\"samples\""),
                    "M03 receipt projects only a hash and bounded summary during remote outage");
            MorrowLocalState.CommitResult first = state.commit(
                    decision.eventKey(), decision.idempotencyKey(), decision.payload());
            MorrowLocalState.CommitResult duplicateReceipt = state.commit(
                    decision.eventKey(), decision.idempotencyKey(), decision.payload());
            check(first.created() && !duplicateReceipt.created() && first.sequence() == duplicateReceipt.sequence(),
                    "duplicate M03 callback reuses the exact local receipt");
            check(state.pendingAfter(0).stream().anyMatch(receipt -> receipt.eventType().equals(
                            EntityReplayAuthority.MISSING_ROLE_COMPLETED)),
                    "local proof persists before any network consequence");
            MorrowLocalState restarted = MorrowLocalState.open(root.resolve("state.journal"), RELEASE);
            check(restarted.snapshot().committedEvents().contains(EntityReplayAuthority.MISSING_ROLE_COMPLETED),
                    "M03 proof catches up after restart/outage");
            Clip duplicate = clips.persist(new Clip(correct.releaseId(), correct.playerId(), correct.purpose(),
                    correct.consentRevision(), correct.consentHash(), correct.durationTicks(), correct.rawSampleCount(),
                    correct.samples(), null));
            check(duplicate.equals(correct), "duplicate content-addressed clip is idempotent");
        } finally {
            deleteTree(root);
        }
    }

    private static void deliberateReplayRestartAndTiming() throws Exception {
        Path root = Files.createTempDirectory("morrow-replay-m04-");
        try {
            MorrowLocalState state = authorizedState(root.resolve("state.journal"));
            EntityReplayConsentStore consents = new EntityReplayConsentStore(root.resolve("consent"), RELEASE);
            EntityReplayClipStore clips = new EntityReplayClipStore(root.resolve("clips"), RELEASE);
            UUID missingPlayer = UUID.randomUUID();
            Clip missing = signedClip(clips, consents.grant(missingPlayer, Purpose.MISSING_ROLE), 740,
                    Purpose.MISSING_ROLE, true);
            commit(state, EntityReplayAuthority.missingRole(state.snapshot(), missing));

            UUID player = UUID.randomUUID();
            ConsentBinding consent = consents.grant(player, Purpose.DELIBERATE_LIVE_TEST);
            Clip weak = signedClip(clips, consent, 120, Purpose.DELIBERATE_LIVE_TEST, false);
            check(EntityReplayAuthority.liveTest(state.snapshot(), weak).status() == EntityReplayAuthority.Status.WRONG,
                    "non-distinctive clip receives responsive wrong-action feedback");
            Clip live = distinctiveClip(clips, consent, 180);
            Decision liveDecision = EntityReplayAuthority.liveTest(state.snapshot(), live);
            commit(state, liveDecision);
            check(!state.snapshot().committedEvents().contains(EntityReplayAuthority.LIVE_CAPTURE_AUTHORIZED),
                    "sealing a live test never auto-authorizes later live capture");
            Decision early = EntityReplayAuthority.behaviorReuse(state.snapshot(), live, live.clipHash(), 178);
            check(early.status() == EntityReplayAuthority.Status.WRONG,
                    "reconstructed echo cannot authenticate two ticks early");
            Decision exact = EntityReplayAuthority.behaviorReuse(state.snapshot(), live, live.clipHash(), 180);
            check(exact.commitsReceipt(), "exact clip hash and deterministic duration prove reuse");
            commit(state, exact);
            check(!state.snapshot().committedEvents().contains(EntityReplayAuthority.LIVE_CAPTURE_AUTHORIZED),
                    "behavior proof still leaves live_capture_authorized to its explicit later dialog");
            MorrowLocalState restarted = MorrowLocalState.open(root.resolve("state.journal"), RELEASE);
            check(restarted.latestPayload(EntityReplayAuthority.LIVE_TEST_RECORDED).isPresent()
                            && clips.load(live.clipHash()).orElseThrow().equals(live),
                    "restart can reconstruct the exact sealed clip from receipt hash plus local store");

            EntityReplayRecorder disconnected = new EntityReplayRecorder(RELEASE, clips);
            disconnected.start(consent);
            disconnected.capture(consent, sample(0, 1.2, -1.0, Set.of()));
            check(disconnected.cancel(player) && !disconnected.active(player)
                            && consents.current(player, Purpose.DELIBERATE_LIVE_TEST).orElseThrow().granted(),
                    "disconnect deletes unsealed samples but preserves consent for explicit rejoin restart");

            List<Sample> oversized = new ArrayList<>();
            for (int index = 0; index <= 450; index++) oversized.add(sample(Math.min(index * 2, 898), 1.2, -1, Set.of()));
            expectIllegal(() -> EntityReplayAuthority.compress(oversized));
        } finally {
            deleteTree(root);
        }
    }

    private static void corruptionAndWrongRelease() throws Exception {
        Path root = Files.createTempDirectory("morrow-replay-corrupt-");
        try {
            EntityReplayConsentStore consents = new EntityReplayConsentStore(root.resolve("consent"), RELEASE);
            EntityReplayClipStore clips = new EntityReplayClipStore(root.resolve("clips"), RELEASE);
            ConsentBinding consent = consents.grant(UUID.randomUUID(), Purpose.DELIBERATE_LIVE_TEST);
            Clip clip = distinctiveClip(clips, consent, 120);
            Path original = root.resolve("clips").resolve(clip.clipHash() + ".clip");

            Path wrongDirectory = root.resolve("wrong");
            Files.createDirectories(wrongDirectory);
            Files.copy(original, wrongDirectory.resolve(clip.clipHash() + ".clip"), StandardCopyOption.REPLACE_EXISTING);
            EntityReplayClipStore wrongRelease = new EntityReplayClipStore(wrongDirectory, "morrow.rehearsal.002");
            expectIo(() -> wrongRelease.load(clip.clipHash()));

            String corrupted = Files.readString(original, StandardCharsets.UTF_8)
                    .replaceFirst("\\t1\\.050\\t", "\\t1.051\\t");
            Files.writeString(original, corrupted, StandardCharsets.UTF_8);
            expectIo(() -> clips.load(clip.clipHash()));
        } finally {
            deleteTree(root);
        }
    }

    private static void paperAdapterContract() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/room04/replay/BukkitEntityReplay.java"));
        for (String required : new String[]{"BlockDisplay", "TextDisplay", "Interaction", "setTeleportDuration(2)",
                "PersistentDataType", "Provenance.RECORDED", "Provenance.RECONSTRUCTED", "Provenance.LIVE",
                "REDSTONE PULSE LOG", "CAPTIONED VOICE FRAGMENT", "LIVE RECORDING BOUNDARY", "LIVE SEAL CONTROL",
                "RECONSTRUCTED PROOF — authenticate exact route",
                "runTaskTimer", "requirePrimaryThread", "PlayerQuitEvent", "activePlayers()",
                "LABEL_SCALE", "setTransformation"}) {
            check(source.contains(required), "Paper replay adapter missing " + required);
        }
        for (String forbidden : new String[]{"setBlockData(", "setType(", "GameProfile", "net.minecraft",
                "Bukkit.getAsyncScheduler", "PlayerInfo", "addPlayer"}) {
            check(!source.contains(forbidden), "Paper replay adapter violates bounded display-only contract: " + forbidden);
        }
    }

    private static MorrowLocalState authorizedState(Path journal) throws Exception {
        MorrowLocalState state = MorrowLocalState.open(journal, RELEASE);
        state.acceptProjection("morrow.act0.case_chain_authenticated", "copperline:case:replay-test", bytes("{}"));
        state.acceptProjection("morrow.act0.server_handoff_recovered", "copperline:handoff:replay-test", bytes("{}"));
        state.commit("morrow.act1.room04_witnessed", "paper:room04:witness:replay-test", bytes("{}"));
        state.commit("morrow.act1.static_proposal_authenticated", "paper:room04:proposal:replay-test", bytes("{}"));
        state.commit("morrow.act1.intention_error_proven", "paper:room04:error:replay-test", bytes("{}"));
        state.commit("morrow.act1.entity_replay_authorized", "paper:room04:replay:replay-test", bytes("{}"));
        return state;
    }

    private static Clip signedClip(EntityReplayClipStore store, ConsentBinding consent, int duration,
                                   Purpose purpose, boolean transfer) throws IOException {
        int count = duration / 2;
        List<Sample> raw = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int tick = index * 2;
            Set<Action> actions = transfer && tick == 420 ? Set.of(Action.INVENTORY_TRANSFER) : Set.of();
            raw.add(sample(tick, purpose == Purpose.MISSING_ROLE ? 2.5 : 1.5,
                    purpose == Purpose.MISSING_ROLE ? -2.5 : -1.5, actions));
        }
        return store.persist(new Clip(RELEASE, consent.playerId(), purpose, consent.revision(), consent.consentHash(),
                duration, count, EntityReplayAuthority.compress(raw), null));
    }

    private static Clip signedMissingClipAt(EntityReplayClipStore store, ConsentBinding consent, int transferTick)
            throws IOException {
        List<Sample> raw = new ArrayList<>();
        for (int index = 0; index < EntityReplayAuthority.MISSING_ROLE_SAMPLE_COUNT; index++) {
            int tick = index * EntityReplayAuthority.SAMPLE_INTERVAL_TICKS;
            raw.add(sample(tick, 2.5, -2.5,
                    tick == transferTick ? Set.of(Action.INVENTORY_TRANSFER) : Set.of()));
        }
        return store.persist(new Clip(RELEASE, consent.playerId(), Purpose.MISSING_ROLE, consent.revision(),
                consent.consentHash(), EntityReplayAuthority.MISSING_ROLE_DURATION_TICKS,
                EntityReplayAuthority.MISSING_ROLE_SAMPLE_COUNT, EntityReplayAuthority.compress(raw), null));
    }

    private static Clip distinctiveClip(EntityReplayClipStore store, ConsentBinding consent, int duration)
            throws IOException {
        int count = duration / 2;
        List<Sample> raw = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double fraction = index / (double) Math.max(1, count - 1);
            double x = index < count / 2 ? 1.05 + fraction * 5.6 : 3.85 - (fraction - .5) * 5.6;
            x = Math.max(1.05, Math.min(3.85, x));
            float yaw = index < count / 2 ? 0F : 90F;
            Set<Action> actions = index == 10 ? Set.of(Action.JUMP) : index == count - 10
                    ? Set.of(Action.CROUCH) : Set.of();
            raw.add(new Sample(index * 2, x, 1, -1.5, yaw, 0, Pose.STANDING, index % 9, actions));
        }
        return store.persist(new Clip(RELEASE, consent.playerId(), Purpose.DELIBERATE_LIVE_TEST,
                consent.revision(), consent.consentHash(), duration, count,
                EntityReplayAuthority.compress(raw), null));
    }

    private static Sample sample(int tick, double x, double z, Set<Action> actions) {
        return new Sample(tick, x, 1, z, 0, 0, Pose.STANDING, 0, actions);
    }

    private static void commit(MorrowLocalState state, Decision decision) throws Exception {
        check(decision.commitsReceipt(), "test predicate must commit its exact receipt");
        state.commit(decision.eventKey(), decision.idempotencyKey(), decision.payload());
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
    private static void expectIllegal(Throwing action) {
        try { action.run(); throw new AssertionError("expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static void expectState(Throwing action) {
        try { action.run(); throw new AssertionError("expected IllegalStateException"); }
        catch (IllegalStateException expected) { }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static void expectIo(Throwing action) {
        try { action.run(); throw new AssertionError("expected IOException"); }
        catch (IOException expected) { }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @FunctionalInterface private interface Throwing { void run() throws Exception; }
}
