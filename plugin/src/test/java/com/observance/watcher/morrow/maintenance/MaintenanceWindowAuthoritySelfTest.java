package com.observance.watcher.morrow.maintenance;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Decision;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Progress;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Result;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Side;
import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Main-driven M10 asymmetric-coop, nonce, refusal, timeout, disconnect, and restart matrix. */
public final class MaintenanceWindowAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.m10";
    private MaintenanceWindowAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        prerequisiteAndWindowBoundsFailClosed();
        oneTwoSixPlayerCohortsRetainBothSessions();
        unsafeInstructionsWrongNoncesTimeoutsAndDisconnectsResetSafely();
        everyReceiptWindowSurvivesRestart();
        System.out.println("MORROW MAINTENANCE WINDOW M10: PASS sessions=2 nonce=fresh players=1/2/6 deletion=refused");
    }

    private static void prerequisiteAndWindowBoundsFailClosed() throws Exception {
        try (Fixture fixture = Fixture.create("m10-locked-", false)) {
            Result locked = MaintenanceWindowAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                    run("locked"), nonce("west-locked"), nonce("east-locked"), 100, 600);
            check(locked.status() == Status.LOCKED && locked.progress().revision() == 0,
                    "M10 opened before returning identity authentication");
        }
        try {
            new MaintenanceWindowAuthority.Window(run("bad"), "aaaaaaaa", "aaaaaaaa", 100, 600);
            throw new AssertionError("expected identical M10 nonce refusal");
        } catch (IllegalArgumentException expected) { /* expected */ }
    }

    private static void oneTwoSixPlayerCohortsRetainBothSessions() throws Exception {
        for (int players : new int[]{1, 2, 6}) {
            try (Fixture fixture = Fixture.create("m10-group-" + players + "-", true)) {
                String westNonce = nonce(players + "-west"), eastNonce = nonce(players + "-east");
                Progress progress = MaintenanceWindowAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                        run("group-" + players), westNonce, eastNonce, 1_000, 1_800).progress();
                UUID west = id(players + "-0"); UUID east = id(players + "-" + (players == 1 ? 0 : 1));
                progress = MaintenanceWindowAuthority.witness(progress, fixture.state.snapshot(), west,
                        Side.WEST, Decision.RETAIN_BOTH, 1_010).progress();
                progress = MaintenanceWindowAuthority.witness(progress, fixture.state.snapshot(), east,
                        Side.EAST, Decision.RETAIN_BOTH, 1_011).progress();
                progress = MaintenanceWindowAuthority.transfer(progress, fixture.state.snapshot(), west,
                        Side.WEST, westNonce, 1_020).progress();
                progress = MaintenanceWindowAuthority.transfer(progress, fixture.state.snapshot(), east,
                        Side.EAST, eastNonce, 1_021).progress();
                Result retained = MaintenanceWindowAuthority.retainBoth(
                        progress, fixture.state.snapshot(), id(players + "-observer"), 1_030);
                check(retained.status() == Status.READY_TO_COMMIT && retained.progress().retainedBoth()
                                && retained.eventKey().equals(MaintenanceWindowAuthority.EVENT),
                        players + "-player M10 cohort could not retain both sessions");
                String payload = text(MaintenanceWindowAuthority.payload(retained));
                int witnesses = players == 1 ? 1 : 2;
                check(payload.contains("\"decision\":\"retain_both\"")
                                && payload.contains("\"authoritative_session\":null")
                                && payload.contains("\"distinct_witnesses\":" + witnesses)
                                && payload.contains("\"west_action\":\"pulse_lamp\"")
                                && payload.contains(MaintenanceWindowAuthority.nonceHash(
                                retained.progress().window().runId(), Side.WEST, westNonce))
                                && !payload.contains(westNonce) && !payload.contains(eastNonce),
                        players + "-player M10 payload lost its proof or exposed a fresh nonce");
                var first = fixture.state.commit(retained.eventKey(), retained.idempotencyKey(),
                        MaintenanceWindowAuthority.payload(retained));
                var duplicate = fixture.state.commit(retained.eventKey(), retained.idempotencyKey(),
                        MaintenanceWindowAuthority.payload(retained));
                check(first.created() && !duplicate.created() && first.sequence() == duplicate.sequence(),
                        players + "-player M10 event is not idempotent");
            }
        }
    }

    private static void unsafeInstructionsWrongNoncesTimeoutsAndDisconnectsResetSafely() throws Exception {
        try (Fixture fixture = Fixture.create("m10-safe-", true)) {
            String westNonce = nonce("safe-west"), eastNonce = nonce("safe-east");
            Progress progress = MaintenanceWindowAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                    run("safe"), westNonce, eastNonce, 2_000, 2_600).progress();
            progress = MaintenanceWindowAuthority.witness(progress, fixture.state.snapshot(), id("west"),
                    Side.WEST, Decision.RETAIN_BOTH, 2_010).progress();
            Result unsafe = MaintenanceWindowAuthority.witness(progress, fixture.state.snapshot(), id("east"),
                    Side.EAST, Decision.DELETE_OTHER, 2_011);
            check(unsafe.status() == Status.UNSAFE_INSTRUCTION_REFUSED
                            && unsafe.progress().westWitness() == null
                            && unsafe.progress().window().westNonce().equals(westNonce),
                    "M10 forced-deletion path did not reset safely while preserving the nonce");
            progress = MaintenanceWindowAuthority.witness(unsafe.progress(), fixture.state.snapshot(), id("west"),
                    Side.WEST, Decision.RETAIN_BOTH, 2_020).progress();
            Result wrong = MaintenanceWindowAuthority.transfer(progress, fixture.state.snapshot(), id("west"),
                    Side.WEST, "zzzzzzzz", 2_021);
            check(wrong.status() == Status.WRONG_NONCE && wrong.progress().westWitness() == null
                            && wrong.progress().window().westNonce().equals(westNonce),
                    "M10 wrong nonce consumed or replaced the active nonce");
            Result timed = MaintenanceWindowAuthority.witness(wrong.progress(), fixture.state.snapshot(), id("west"),
                    Side.WEST, Decision.RETAIN_BOTH, 2_601);
            check(timed.status() == Status.TIMED_RESET && timed.progress().window().westNonce().equals(westNonce)
                            && timed.progress().window().eastNonce().equals(eastNonce),
                    "M10 timeout did not preserve both unconsumed nonces");
            progress = MaintenanceWindowAuthority.witness(timed.progress(), fixture.state.snapshot(), id("west"),
                    Side.WEST, Decision.RETAIN_BOTH, 2_602).progress();
            Result disconnected = MaintenanceWindowAuthority.recordDisconnect(
                    progress, fixture.state.snapshot(), id("west"), 2_603);
            check(disconnected.status() == Status.SAFE_DISCONNECT && disconnected.progress().westWitness() == null
                            && disconnected.progress().lobbyReturns().contains(id("west")),
                    "M10 disconnect did not reset the attempt and schedule a safe lobby return");
            Result returned = MaintenanceWindowAuthority.recordLobbyReturn(
                    disconnected.progress(), fixture.state.snapshot(), id("west"));
            check(returned.status() == Status.RETURNED_TO_LOBBY && returned.progress().lobbyReturns().isEmpty(),
                    "M10 safe lobby return was not cleared idempotently");
        }
    }

    private static void everyReceiptWindowSurvivesRestart() throws Exception {
        Path directory = Files.createTempDirectory("m10-store-"); Path path = directory.resolve("maintenance.progress");
        try (Fixture fixture = Fixture.create("m10-store-state-", true)) {
            MaintenanceWindowProgressStore store = new MaintenanceWindowProgressStore(path, RELEASE);
            String westNonce = nonce("store-west"), eastNonce = nonce("store-east");
            Progress progress = MaintenanceWindowAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                    run("store"), westNonce, eastNonce, 3_000, 3_800).progress();
            progress = roundTrip(store, progress, "window");
            progress = roundTrip(store, MaintenanceWindowAuthority.witness(progress, fixture.state.snapshot(),
                    id("store-west"), Side.WEST, Decision.RETAIN_BOTH, 3_010).progress(), "west witness");
            progress = roundTrip(store, MaintenanceWindowAuthority.transfer(progress, fixture.state.snapshot(),
                    id("store-west"), Side.WEST, westNonce, 3_020).progress(), "west transfer");
            progress = roundTrip(store, MaintenanceWindowAuthority.witness(progress, fixture.state.snapshot(),
                    id("store-east"), Side.EAST, Decision.RETAIN_BOTH, 3_030).progress(), "east witness");
            progress = roundTrip(store, MaintenanceWindowAuthority.transfer(progress, fixture.state.snapshot(),
                    id("store-east"), Side.EAST, eastNonce, 3_040).progress(), "east transfer");
            progress = roundTrip(store, MaintenanceWindowAuthority.retainBoth(progress, fixture.state.snapshot(),
                    id("store-observer"), 3_050).progress(), "local retained-both receipt");
            check(MaintenanceWindowAuthority.recover(progress, fixture.state.snapshot()).status()
                            == Status.READY_TO_COMMIT,
                    "M10 retained-both crash window cannot recover without replaying room inputs");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(2, "revision=999"); Files.write(path, lines, StandardCharsets.UTF_8);
            try { store.load(); throw new AssertionError("expected M10 corruption failure"); }
            catch (IOException expected) { /* expected */ }
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(directory); }
    }

    private static Progress roundTrip(MaintenanceWindowProgressStore store, Progress progress, String label)
            throws Exception {
        store.save(progress); Progress loaded = store.load();
        check(loaded.equals(progress), "M10 " + label + " did not survive restart"); return loaded;
    }
    private static String run(String value) {
        return UUID.nameUUIDFromBytes(("m10-run-" + value).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }
    private static String nonce(String value) {
        String hex = UUID.nameUUIDFromBytes(("m10-nonce-" + value).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        return hex.substring(0, 8).replace('0', 'a').replace('1', 'b');
    }
    private static UUID id(String value) { return UUID.nameUUIDFromBytes(("m10-" + value).getBytes(StandardCharsets.UTF_8)); }
    private static String text(byte[] value) { return new String(value, StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static final class Fixture implements AutoCloseable {
        private final Path directory; private final MorrowLocalState state;
        private Fixture(Path directory, MorrowLocalState state) { this.directory = directory; this.state = state; }
        static Fixture create(String prefix, boolean ready) throws Exception {
            Path directory = Files.createTempDirectory(prefix);
            MorrowLocalState state = MorrowLocalState.open(directory.resolve("morrow.journal"), RELEASE);
            if (ready) seed(state); return new Fixture(directory, state);
        }
        private static void seed(MorrowLocalState state) throws Exception {
            String[] events = {"morrow.act0.case_chain_authenticated", "morrow.act0.server_handoff_recovered",
                    "morrow.act1.room04_witnessed", "morrow.act1.static_proposal_authenticated",
                    "morrow.act1.intention_error_proven", "morrow.act1.entity_replay_authorized",
                    "morrow.act2.missing_role_completed", "morrow.act2.live_test_recorded",
                    "morrow.act2.behavior_reuse_proven", "morrow.act2.private_contradiction_resolved",
                    "morrow.act2.live_capture_authorized", "morrow.act3.version_fragments_authenticated",
                    "morrow.act3.contradiction_preserved", "morrow.act4.witness_anchor_registered",
                    "morrow.act4.almost_home_proven", "morrow.act4.account_continuity_authorized",
                    "morrow.act5.continued_session_observed", MaintenanceWindowAuthority.PREREQUISITE};
            for (int index = 0; index < events.length; index++) {
                String event = events[index];
                if (event.startsWith("morrow.act0") || event.equals("morrow.act2.private_contradiction_resolved"))
                    state.acceptProjection(event, "m10:external:" + index, bytes("{}"));
                else state.commit(event, "m10:minecraft:" + index, bytes("{}"));
            }
        }
        @Override public void close() throws IOException {
            Files.deleteIfExists(directory.resolve("morrow.journal")); Files.deleteIfExists(directory);
        }
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
