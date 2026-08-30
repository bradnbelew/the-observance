package com.observance.watcher.morrow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Dependency-free outage, retry, duplicate, collision, cursor, release, restart, and shutdown tests.
 */
public final class MorrowEventProjectorSelfTest {
    private static final String RELEASE = "morrow.rehearsal.001";
    private static final String CAMPAIGN = "8e0b1a62-d2dd-4e86-91f1-4b07af5e2922";
    private static final String SECRET = "selftest-secret-with-at-least-32-bytes";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-30T12:00:00Z"), ZoneOffset.UTC);

    private MorrowEventProjectorSelfTest() { }

    public static void main(String[] args) throws Exception {
        outageRetryOrderingAndRestart();
        duplicateCursorRecovery();
        collisionHaltsTheQueue();
        wrongReleaseHaltsAndCursorIsReleaseBound();
        cleanShutdownInterruptsIdleWorker();
        System.out.println("MORROW EVENT PROJECTOR: PASS");
    }

    private static void outageRetryOrderingAndRestart() throws Exception {
        Path directory = Files.createTempDirectory("morrow-projector-retry-");
        Path journal = directory.resolve("morrow.journal");
        Path cursor = directory.resolve("morrow.cursor");
        try {
            MorrowLocalState state = stateWithTwoLocalEvents(journal);
            FakeTransport transport = new FakeTransport();
            transport.outage();
            transport.respond(503, "{\"status\":\"unavailable\",\"releaseId\":\"" + RELEASE + "\"}");
            transport.respond(200, success("committed", RELEASE));
            transport.respond(200, success("committed", RELEASE));
            List<Long> delays = new ArrayList<>();

            MorrowEventProjector projector = MorrowEventProjector.forTest(
                    state, settings(), cursor, transport, CLOCK, duration -> delays.add(duration.toMillis()));
            projector.driveForTest(5);
            check(projector.snapshot().state() == MorrowEventProjector.State.IDLE, "queue drains to idle");
            check(projector.snapshot().projectedSequence() == 5, "cursor advances through both local events");
            check(delays.equals(List.of(10L, 20L)), "retry backoff doubles in order");
            check(transport.requests.size() == 4, "outage and remote retry were both retried");
            check(transport.requests.get(0).sequence() == 4
                    && transport.requests.get(1).sequence() == 4
                    && transport.requests.get(2).sequence() == 4
                    && transport.requests.get(3).sequence() == 5,
                    "later event never overtakes failed event");
            check(transport.requests.get(0).body().equals(transport.requests.get(2).body()),
                    "retry preserves the signed event envelope");
            assertSigned(transport.requests, SECRET);
            projector.close();

            FakeTransport restartedTransport = new FakeTransport();
            MorrowEventProjector restarted = MorrowEventProjector.forTest(
                    MorrowLocalState.open(journal, RELEASE), settings(), cursor,
                    restartedTransport, CLOCK, duration -> { });
            restarted.driveForTest(1);
            check(restarted.snapshot().state() == MorrowEventProjector.State.IDLE,
                    "restart resumes at durable cursor");
            check(restartedTransport.requests.isEmpty(), "restart does not resend acknowledged events");
            restarted.close();
        } finally {
            delete(directory, cursor, journal);
        }
    }

    private static void duplicateCursorRecovery() throws Exception {
        Path directory = Files.createTempDirectory("morrow-projector-duplicate-");
        Path journal = directory.resolve("morrow.journal");
        Path cursor = directory.resolve("morrow.cursor");
        try {
            MorrowLocalState state = stateWithTwoLocalEvents(journal);
            FakeTransport transport = new FakeTransport();
            // Models a crash after the remote commit but before the local cursor replacement.
            transport.respond(200, success("duplicate", RELEASE));
            transport.respond(200, success("committed", RELEASE));
            MorrowEventProjector projector = MorrowEventProjector.forTest(
                    state, settings(), cursor, transport, CLOCK, duration -> { });
            projector.driveForTest(3);
            check(projector.snapshot().projectedSequence() == 5,
                    "duplicate safely recovers a missing cursor then advances");
            projector.close();

            String durable = Files.readString(cursor, StandardCharsets.UTF_8);
            check(durable.contains("release=" + RELEASE), "cursor binds release");
            check(durable.contains("sequence=5"), "recovered cursor is durable");
        } finally {
            delete(directory, cursor, journal);
        }
    }

    private static void collisionHaltsTheQueue() throws Exception {
        Path directory = Files.createTempDirectory("morrow-projector-collision-");
        Path journal = directory.resolve("morrow.journal");
        Path cursor = directory.resolve("morrow.cursor");
        try {
            MorrowLocalState state = stateWithTwoLocalEvents(journal);
            FakeTransport transport = new FakeTransport();
            transport.respond(409, "{\"status\":\"collision\",\"releaseId\":\"" + RELEASE + "\"}");
            transport.respond(200, success("committed", RELEASE));
            MorrowEventProjector projector = MorrowEventProjector.forTest(
                    state, settings(), cursor, transport, CLOCK, duration -> { });
            projector.driveForTest(4);
            check(projector.snapshot().state() == MorrowEventProjector.State.HALTED, "collision halts projector");
            check(projector.snapshot().haltReason() == MorrowEventProjector.HaltReason.COLLISION,
                    "collision has explicit halt reason");
            check(projector.snapshot().projectedSequence() == 0, "collision never advances cursor");
            check(transport.requests.size() == 1, "collision prevents later event transmission");
            projector.close();
        } finally {
            delete(directory, cursor, journal);
        }
    }

    private static void wrongReleaseHaltsAndCursorIsReleaseBound() throws Exception {
        Path directory = Files.createTempDirectory("morrow-projector-release-");
        Path journal = directory.resolve("morrow.journal");
        Path cursor = directory.resolve("morrow.cursor");
        try {
            MorrowLocalState state = stateWithTwoLocalEvents(journal);
            FakeTransport transport = new FakeTransport();
            transport.respond(200, success("committed", "morrow.rehearsal.999"));
            MorrowEventProjector projector = MorrowEventProjector.forTest(
                    state, settings(), cursor, transport, CLOCK, duration -> { });
            projector.driveForTest(2);
            check(projector.snapshot().haltReason() == MorrowEventProjector.HaltReason.WRONG_RELEASE,
                    "wrong remote release halts projector");
            check(projector.snapshot().projectedSequence() == 0, "wrong release never advances cursor");
            projector.close();

            FakeTransport accepted = new FakeTransport();
            accepted.respond(200, success("committed", RELEASE));
            MorrowEventProjector persisted = MorrowEventProjector.forTest(
                    state, settings(), cursor, accepted, CLOCK, duration -> { });
            persisted.driveForTest(1);
            persisted.close();
            String changed = Files.readString(cursor, StandardCharsets.UTF_8)
                    .replace("release=" + RELEASE, "release=morrow.rehearsal.999");
            Files.writeString(cursor, changed, StandardCharsets.UTF_8);
            expectIo(() -> MorrowEventProjector.forTest(
                    state, settings(), cursor, new FakeTransport(), CLOCK, duration -> { }));
            expectIo(() -> MorrowLocalState.open(journal, "morrow.rehearsal.999"));
        } finally {
            delete(directory, cursor, journal);
        }
    }

    private static void cleanShutdownInterruptsIdleWorker() throws Exception {
        Path directory = Files.createTempDirectory("morrow-projector-shutdown-");
        Path journal = directory.resolve("morrow.journal");
        Path cursor = directory.resolve("morrow.cursor");
        try {
            MorrowLocalState state = MorrowLocalState.open(journal, RELEASE);
            CountDownLatch enteredDelay = new CountDownLatch(1);
            MorrowEventProjector projector = MorrowEventProjector.forTest(
                    state, settings(), cursor, new FakeTransport(), CLOCK, duration -> {
                        enteredDelay.countDown();
                        Thread.sleep(30_000);
                    });
            projector.start();
            check(enteredDelay.await(2, TimeUnit.SECONDS), "background worker reaches idle wait");
            projector.close();
            check(projector.snapshot().state() == MorrowEventProjector.State.CLOSED,
                    "clean shutdown interrupts and joins worker");
        } finally {
            delete(directory, cursor, journal);
        }
    }

    private static MorrowLocalState stateWithTwoLocalEvents(Path journal) throws Exception {
        MorrowLocalState state = MorrowLocalState.open(journal, RELEASE);
        state.acceptProjection("morrow.act0.case_chain_authenticated", "copperline:case:001", bytes("{}"));
        state.acceptProjection("morrow.act0.server_handoff_recovered", "copperline:handoff:001", bytes("{}"));
        state.commit("morrow.act1.room04_witnessed", "paper:room04:witness:001", bytes("{\"room\":\"recovery_04\"}"));
        state.commit("morrow.act1.static_proposal_authenticated", "paper:room04:proposal:001", bytes("{\"proposal\":\"sixth-block\"}"));
        return state;
    }

    private static MorrowRuntimeSettings settings() {
        return MorrowRuntimeSettings.create(
                RELEASE, CAMPAIGN, "morrow_rehearsal", "https://example.test/api/morrow", SECRET,
                1_000, 2_000, 10, 80);
    }

    private static String success(String status, String release) {
        return "{\"status\":\"" + status + "\",\"created\":true,\"releaseId\":\"" + release + "\"}";
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void assertSigned(List<MorrowEventProjector.ProjectionRequest> requests, String secret) {
        for (MorrowEventProjector.ProjectionRequest request : requests) {
            String expected = "v1=" + MorrowEventProjector.hmac(
                    secret, request.timestamp() + "." + request.body());
            check(expected.equals(request.signature()), "HMAC signature binds timestamp and exact body");
            check(request.body().contains("\"releaseId\":\"" + RELEASE + "\""),
                    "body is release-bound");
            check(request.body().contains("\"campaignId\":\"" + CAMPAIGN + "\""),
                    "body is campaign-bound");
        }
    }

    private static void delete(Path directory, Path cursor, Path journal) throws IOException {
        Files.deleteIfExists(cursor);
        Files.deleteIfExists(journal);
        Files.deleteIfExists(directory);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void expectIo(Throwing action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected IOException");
        } catch (IOException expected) {
            // expected
        }
    }

    @FunctionalInterface
    private interface Throwing { void run() throws Exception; }

    private static final class FakeTransport implements MorrowEventProjector.Transport {
        private final Deque<Object> outcomes = new ArrayDeque<>();
        private final List<MorrowEventProjector.ProjectionRequest> requests = new ArrayList<>();

        private void outage() {
            outcomes.addLast(new IOException("simulated outage"));
        }

        private void respond(int statusCode, String body) {
            outcomes.addLast(new MorrowEventProjector.ProjectionResponse(statusCode, body));
        }

        @Override
        public MorrowEventProjector.ProjectionResponse send(MorrowEventProjector.ProjectionRequest request)
                throws IOException {
            requests.add(request);
            Object outcome = outcomes.pollFirst();
            if (outcome == null) throw new AssertionError("unexpected projector request");
            if (outcome instanceof IOException failure) throw failure;
            return (MorrowEventProjector.ProjectionResponse) outcome;
        }
    }
}
