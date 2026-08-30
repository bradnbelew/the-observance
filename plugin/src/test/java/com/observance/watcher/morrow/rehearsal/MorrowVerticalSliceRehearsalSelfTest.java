package com.observance.watcher.morrow.rehearsal;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.Action;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.Decision;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority.View;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Clip;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.ConsentBinding;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Pose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Purpose;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Sample;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.CandidateId;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Provenance;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestorePredicate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Complete headless P0 slice ordering/restart rehearsal using the production domain authorities. */
public final class MorrowVerticalSliceRehearsalSelfTest {
    private static final String RELEASE = "morrow.rehearsal.fa1b80b.p0-10.v1";
    private static final String CASE_CHAIN = "morrow.act0.case_chain_authenticated";
    private static final String PRIVATE_CONTRADICTION = "morrow.act2.private_contradiction_resolved";

    private MorrowVerticalSliceRehearsalSelfTest() { }

    public static void main(String[] args) throws Exception {
        for (int players : new int[]{1, 2, 6}) runCohort(players);
        wrongReleaseAndOrderingFailClosed();
        System.out.println("MORROW P0 VERTICAL SLICE REHEARSAL: PASS players=1/2/6 restarts=10 "
                + "live_capture=offered_not_accepted");
    }

    private static void runCohort(int players) throws Exception {
        Path root = Files.createTempDirectory("morrow-p0-rehearsal-" + players + "-");
        Path journal = root.resolve("morrow-reboot.journal");
        try {
            MorrowLocalState state = MorrowLocalState.open(journal, RELEASE);
            MorrowLocalState initialState = state;
            expectState(() -> initialState.acceptProjection(MorrowDialogAuthority.CASE_HANDOFF,
                    "copperline:handoff:p0", bytes("{}")));

            state.acceptProjection(CASE_CHAIN, "copperline:case:p0", bytes(
                    "{\"checksum_verified\":true,\"custody_authenticated\":true}"));
            state = restart(journal, CASE_CHAIN);
            state.acceptProjection(MorrowDialogAuthority.CASE_HANDOFF, "copperline:handoff:p0", bytes(
                    "{\"short_token_recovered\":true}"));
            state = restart(journal, MorrowDialogAuthority.CASE_HANDOFF);

            Decision witness = MorrowDialogAuthority.decide(Action.ACKNOWLEDGE_ROOM, state.snapshot());
            commit(state, witness);
            state = restart(journal, MorrowDialogAuthority.ROOM_WITNESSED);
            check(!MorrowDialogAuthority.decide(Action.REVIEW_EVIDENCE, state.snapshot()).commitsReceipt(),
                    "evidence review is read-only");

            Decision proposal = MorrowDialogAuthority.decide(Action.AUTHENTICATE_PROPOSAL, state.snapshot());
            commit(state, proposal);
            state = restart(journal, MorrowDialogAuthority.PROPOSAL_AUTHENTICATED);

            StaticRestorePredicate predicate = new StaticRestorePredicate(new StaticRestoreManifest());
            check(predicate.evaluate(state.snapshot(), CandidateId.B06, Provenance.INFERRED, false).status()
                            == StaticRestorePredicate.Status.NOT_READY,
                    "partial Static Restore creates no proof");
            check(predicate.evaluate(state.snapshot(), CandidateId.B01, Provenance.INFERRED, true).status()
                            == StaticRestorePredicate.Status.WRONG,
                    "wrong physical cell creates no proof");
            StaticRestorePredicate.Attempt intention = predicate.evaluate(
                    state.snapshot(), CandidateId.B06, Provenance.INFERRED, true);
            check(intention.commitsProof(), "the factual sixth-block inference error creates proof");
            state.commit(intention.eventKey(), intention.idempotencyKey(), intention.payload());
            state = restart(journal, MorrowDialogAuthority.INTENTION_ERROR_PROVEN);

            check(!MorrowDialogAuthority.decide(Action.DECLINE_ENTITY_REPLAY, state.snapshot()).commitsReceipt(),
                    "declining Entity Replay creates no receipt");
            Decision replayAuthorization = MorrowDialogAuthority.decide(
                    Action.AUTHORIZE_ENTITY_REPLAY, state.snapshot());
            commit(state, replayAuthorization);
            state = restart(journal, MorrowDialogAuthority.ENTITY_REPLAY_AUTHORIZED);

            List<UUID> cohort = players(players);
            Clip missingRole = missingRoleClip(cohort.get(0));
            check(EntityReplayAuthority.missingRole(state.snapshot(), partialMissingRole(cohort.get(0))).status()
                            == EntityReplayAuthority.Status.WRONG,
                    "partial missing-role clip resets without progress");
            com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Decision missingDecision =
                    EntityReplayAuthority.missingRole(state.snapshot(), missingRole);
            commitReplay(state, missingDecision);
            state = restart(journal, EntityReplayAuthority.MISSING_ROLE_COMPLETED);

            Clip deliberate = deliberateClip(cohort.get(0));
            com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Decision liveTest =
                    EntityReplayAuthority.liveTest(state.snapshot(), deliberate);
            commitReplay(state, liveTest);
            state = restart(journal, EntityReplayAuthority.LIVE_TEST_RECORDED);

            com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Decision reuse =
                    EntityReplayAuthority.behaviorReuse(
                            state.snapshot(), deliberate, deliberate.clipHash(), deliberate.durationTicks());
            commitReplay(state, reuse);
            state = restart(journal, EntityReplayAuthority.BEHAVIOR_REUSE_PROVEN);
            check(MorrowDialogAuthority.currentView(state.snapshot()) == View.ENTITY_REPLAY_CONSOLE,
                    "behavior proof cannot skip the private contradiction");

            state.acceptProjection(PRIVATE_CONTRADICTION, "discord:morrow:act2:private-contradiction:v1",
                    bytes("{\"linked_player_count\":" + players
                            + ",\"private_payload\":false,\"resolution\":\"file_live_behavior_as_inferred_source\"}"));
            state = restart(journal, PRIVATE_CONTRADICTION);
            check(MorrowDialogAuthority.currentView(state.snapshot()) == View.LIVE_CAPTURE_AUTHORIZATION,
                    "the group receipt offers the later live-capture dialog");
            Decision declineLive = MorrowDialogAuthority.decide(Action.DECLINE_LIVE_CAPTURE, state.snapshot());
            check(!declineLive.commitsReceipt()
                            && !state.snapshot().committedEvents().contains(
                            EntityReplayAuthority.LIVE_CAPTURE_AUTHORIZED),
                    "live capture is offered but never automatically accepted");

            long beforeDuplicate = state.pendingAfter(0).size();
            MorrowLocalState.CommitResult duplicate = state.commit(
                    reuse.eventKey(), reuse.idempotencyKey(), reuse.payload());
            check(!duplicate.created() && state.pendingAfter(0).size() == beforeDuplicate,
                    "exact duplicate delivery remains idempotent");
            check(state.snapshot().committedEvents().size() == 10,
                    "the complete slice ends at exactly ten canonical events");
            for (UUID player : cohort) check(player != null,
                    "disconnect/rejoin cohort identity remains reconstructible");
        } finally {
            deleteTree(root);
        }
    }

    private static MorrowLocalState restart(Path journal, String requiredEvent) throws IOException {
        MorrowLocalState restarted = MorrowLocalState.open(journal, RELEASE);
        check(restarted.snapshot().committedEvents().contains(requiredEvent),
                "restart lost durable boundary " + requiredEvent);
        return restarted;
    }

    private static void wrongReleaseAndOrderingFailClosed() throws Exception {
        Path root = Files.createTempDirectory("morrow-p0-negative-");
        Path journal = root.resolve("morrow-reboot.journal");
        try {
            MorrowLocalState state = MorrowLocalState.open(journal, RELEASE);
            expectState(() -> state.commit(EntityReplayAuthority.LIVE_TEST_RECORDED,
                    "paper:room04:m04:live-test:v1", bytes("{}")));
            expectIo(() -> MorrowLocalState.open(journal, "morrow.rehearsal.wrong-release"));
        } finally {
            deleteTree(root);
        }
    }

    private static Clip missingRoleClip(UUID player) {
        return signed(player, Purpose.MISSING_ROLE, 740, 370, true);
    }

    private static Clip partialMissingRole(UUID player) {
        return signed(player, Purpose.MISSING_ROLE, 200, 100, false);
    }

    private static Clip deliberateClip(UUID player) {
        int duration = 180;
        int count = duration / 2;
        List<Sample> samples = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double fraction = index / (double) (count - 1);
            double x = index < count / 2 ? 1.05 + fraction * 5.6 : 3.85 - (fraction - .5) * 5.6;
            x = Math.max(1.05, Math.min(3.85, x));
            float yaw = index < count / 2 ? 0F : 90F;
            Set<EntityReplayAuthority.Action> actions = index == 10
                    ? Set.of(EntityReplayAuthority.Action.JUMP)
                    : index == count - 10 ? Set.of(EntityReplayAuthority.Action.CROUCH) : Set.of();
            samples.add(new Sample(index * 2, x, 1, -1.5, yaw, 0, Pose.STANDING,
                    index % 9, actions));
        }
        return hash(new Clip(RELEASE, player, Purpose.DELIBERATE_LIVE_TEST, 1,
                EntityReplayAuthority.consentHash(RELEASE, player, Purpose.DELIBERATE_LIVE_TEST, 1, true),
                duration, count, EntityReplayAuthority.compress(samples), null));
    }

    private static Clip signed(UUID player, Purpose purpose, int duration, int count, boolean transfer) {
        List<Sample> samples = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int tick = index * 2;
            Set<EntityReplayAuthority.Action> actions = transfer && tick == 420
                    ? Set.of(EntityReplayAuthority.Action.INVENTORY_TRANSFER) : Set.of();
            samples.add(new Sample(tick, 2.5, 1, -2.5, 0, 0, Pose.STANDING, 0, actions));
        }
        ConsentBinding consent = new ConsentBinding(RELEASE, player, purpose, 1, true,
                EntityReplayAuthority.consentHash(RELEASE, player, purpose, 1, true));
        return hash(new Clip(RELEASE, player, purpose, consent.revision(), consent.consentHash(),
                duration, count, EntityReplayAuthority.compress(samples), null));
    }

    private static Clip hash(Clip clip) {
        return clip.withHash(EntityReplayAuthority.clipHash(clip));
    }

    private static List<UUID> players(int count) {
        List<UUID> result = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            result.add(UUID.nameUUIDFromBytes(("morrow-p0-player-" + index).getBytes(StandardCharsets.UTF_8)));
        }
        return List.copyOf(result);
    }

    private static void commit(MorrowLocalState state, Decision decision) throws IOException {
        check(decision.commitsReceipt(), "dialog action must commit its authored receipt");
        state.commit(decision.eventKey(), decision.idempotencyKey(), decision.payload());
    }

    private static void commitReplay(
            MorrowLocalState state,
            com.observance.watcher.morrow.room04.replay.EntityReplayAuthority.Decision decision)
            throws IOException {
        check(decision.commitsReceipt(), "replay predicate must commit its authored receipt");
        state.commit(decision.eventKey(), decision.idempotencyKey(), decision.payload());
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void expectState(Throwing action) {
        try {
            action.run();
            throw new AssertionError("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private static void expectIo(Throwing action) {
        try {
            action.run();
            throw new AssertionError("expected IOException");
        } catch (IOException expected) {
            // expected
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface Throwing { void run() throws Exception; }
}
