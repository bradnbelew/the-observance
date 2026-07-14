package com.observance.watcher.v5runtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/** Main-driven, dependency-light contract checks for the isolated V5 runtime core. */
public final class V5RuntimeCoreSelfTest {
    private V5RuntimeCoreSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        PhysicalPredicateAuthority authority = authorityAndCatalogParity();
        normalizationContract();
        fixtureTransforms();
        leaseAndMutexPrimitives();
        conductVerdictTotality();
        atomicProgressReloadCasAndCorruption(authority);
        System.out.println("V5RuntimeCoreSelfTest: PASS (60 contracts; local-primary durability verified)");
    }

    private static PhysicalPredicateAuthority authorityAndCatalogParity() throws IOException {
        PhysicalPredicateAuthority authority = PhysicalPredicateAuthorityLoader.loadDefault();
        check(authority.schemaVersion() == 1, "authority schema");
        check(authority.nodes().size() == 60, "authority node count");
        check(authority.sha256().matches("[0-9a-f]{64}"), "authority SHA-256");
        check(authority.globalRules().conductVerdict().recordedInputsPerVote().size() == 8,
                "ballot field count");
        check(authority.globalRules().conductVerdict().allowedValues().equals(
                        List.of("solo", "divided", "unanimous", "persistent")),
                "conduct values");
        PredicateCoverageCatalog.validateAgainst(authority);
        check(PredicateCoverageCatalog.entries().size() == 60, "coverage catalog count");
        check(PredicateCoverageCatalog.byFamily().keySet()
                        .equals(EnumSet.allOf(ImplementationFamily.class)),
                "all eight implementation families");
        for (PhysicalPredicateAuthority.Node node : authority.nodes()) {
            PredicateCoverageCatalog.Entry entry = PredicateCoverageCatalog.require(node.nodeId());
            check(!entry.worldInteractionImplemented(),
                    "core catalog cannot claim world implementation: " + node.nodeId());
            check(!node.owner().isBlank() && !node.handler().isBlank()
                            && !node.predicate().kind().isBlank()
                            && !node.completionFlag().isBlank()
                            && !node.prerequisites().isEmpty(),
                    "complete immutable node model: " + node.nodeId());
        }

        try (InputStream input = V5RuntimeCoreSelfTest.class.getResourceAsStream(
                PhysicalPredicateAuthorityLoader.RESOURCE_PATH)) {
            check(input != null, "authority test resource");
            String original = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String altered = original.replaceFirst(
                    "\\\"node_id\\\"\\s*:\\s*\\\"LS06\\\"", "\\\"node_id\\\":\\\"ZZ99\\\"");
            check(!altered.equals(original), "authority mutation fixture");
            expectThrows(AuthorityException.class, () -> PhysicalPredicateAuthorityLoader.load(
                    new ByteArrayInputStream(altered.getBytes(StandardCharsets.UTF_8))));
        }
        return authority;
    }

    private static void normalizationContract() {
        String source = "  \u00a7a\uff56\uff45\uff4e\uff54\u2014east\tbefore,\u0007 second\u200B bell!!  ";
        check("VENT EAST BEFORE SECOND BELL".equals(AnswerNormalizer.normalize(source)),
                "NFKC/color/control/punctuation normalization");
        check("I I".equals(AnswerNormalizer.normalize("i-i")), "Locale.ROOT uppercase");
        check("87 PEOPLE 174 BASKETS".equals(
                        AnswerNormalizer.normalize("87 people & 174 baskets")),
                "symbols preserved while punctuation and whitespace collapse");
    }

    private static void fixtureTransforms() {
        FixtureTransform.BlockPos origin = new FixtureTransform.BlockPos(10, 20, 30);
        FixtureTransform.LocalOffset offset = new FixtureTransform.LocalOffset(2, 3, 4);
        Map<FixtureTransform.Cardinal, FixtureTransform.BlockPos> expected = Map.of(
                FixtureTransform.Cardinal.NORTH, new FixtureTransform.BlockPos(12, 23, 26),
                FixtureTransform.Cardinal.EAST, new FixtureTransform.BlockPos(14, 23, 32),
                FixtureTransform.Cardinal.SOUTH, new FixtureTransform.BlockPos(8, 23, 34),
                FixtureTransform.Cardinal.WEST, new FixtureTransform.BlockPos(6, 23, 28));
        expected.forEach((front, world) -> {
            check(world.equals(FixtureTransform.toWorld(origin, front, offset)),
                    "fixture transform " + front);
            check(offset.equals(FixtureTransform.toLocal(origin, front, world)),
                    "fixture inverse " + front);
        });

        Map<FixtureTransform.Cardinal, FixtureTransform.FramePlane> framePlanes = Map.of(
                FixtureTransform.Cardinal.NORTH, new FixtureTransform.FramePlane(10.5, 20.5, 30.0),
                FixtureTransform.Cardinal.EAST, new FixtureTransform.FramePlane(11.0, 20.5, 30.5),
                FixtureTransform.Cardinal.SOUTH, new FixtureTransform.FramePlane(10.5, 20.5, 31.0),
                FixtureTransform.Cardinal.WEST, new FixtureTransform.FramePlane(10.0, 20.5, 30.5));
        framePlanes.forEach((facing, plane) -> check(
                plane.equals(FixtureTransform.framePlane(origin, facing)),
                "item-frame support plane " + facing));

        Map<FixtureTransform.Cardinal, FixtureTransform.FrameSpawnAnchor> spawnAnchors = Map.of(
                FixtureTransform.Cardinal.NORTH,
                new FixtureTransform.FrameSpawnAnchor(10.5, 20.5, 29.5),
                FixtureTransform.Cardinal.EAST,
                new FixtureTransform.FrameSpawnAnchor(11.5, 20.5, 30.5),
                FixtureTransform.Cardinal.SOUTH,
                new FixtureTransform.FrameSpawnAnchor(10.5, 20.5, 31.5),
                FixtureTransform.Cardinal.WEST,
                new FixtureTransform.FrameSpawnAnchor(9.5, 20.5, 30.5));
        spawnAnchors.forEach((facing, anchor) -> check(
                anchor.equals(FixtureTransform.frameSpawnAnchor(origin, facing)),
                "Paper hanging spawn anchor " + facing));
    }

    private static void leaseAndMutexPrimitives() throws Exception {
        AtomicLong clock = new AtomicLong(1_000);
        LeaseBook leases = new LeaseBook(clock::get);
        LeaseBook.Token first = leases.tryAcquire("answer:LC02", "player-a", Duration.ofNanos(50))
                .orElseThrow();
        check(leases.tryAcquire("answer:LC02", "player-b", Duration.ofNanos(50)).isEmpty(),
                "active lease exclusion");
        check(leases.renew(first, Duration.ofNanos(100)), "lease renewal");
        clock.addAndGet(101);
        check(leases.purgeExpired() == 1, "expired lease purge");
        check(!leases.release(first), "stale token cannot release a newer/absent lease");
        try (LeaseBook.Token ignored = leases.tryAcquire(
                "answer:LC02", "player-b", Duration.ofSeconds(1)).orElseThrow()) {
            check(leases.snapshot().size() == 1, "lease snapshot");
        }
        check(leases.snapshot().isEmpty(), "lease close release");

        SiteMutexes mutexes = new SiteMutexes();
        try (SiteMutexes.Guard guard = mutexes.tryAcquire("orientation_register", Duration.ZERO)
                .orElseThrow()) {
            check("orientation_register".equals(guard.siteId()), "site mutex identity");
        }
    }

    private static void conductVerdictTotality() {
        BallotTelemetry solo = ballot(1, 1, 1, false, 1, 0);
        check(ConductVerdictDeriver.derive(solo, solo, "free", "publish")
                        == ConductVerdict.SOLO,
                "solo precedence");

        BallotTelemetry unanimous = ballot(3, 3, 1, false, 1, 0);
        check(ConductVerdictDeriver.derive(
                        unanimous, unanimous, "understand", "release_unnamed")
                        == ConductVerdict.UNANIMOUS,
                "unanimous precedence");

        BallotTelemetry divided = ballot(3, 3, 2, true, 2, 0);
        check(ConductVerdictDeriver.derive(divided, unanimous, "condemn", "publish")
                        == ConductVerdict.DIVIDED,
                "divided precedence");

        BallotTelemetry persistent = ballot(3, 2, 1, false, 2, 1);
        check(ConductVerdictDeriver.derive(persistent, unanimous, "free", "publish")
                        == ConductVerdict.PERSISTENT,
                "persistent precedence");

        List<BallotTelemetry> validBallots = new ArrayList<>();
        for (int roster = 1; roster <= 4; roster++) {
            for (int cast = 0; cast <= roster; cast++) {
                int minimumDistinct = cast == 0 ? 0 : 1;
                for (int distinct = minimumDistinct; distinct <= Math.min(cast, 3); distinct++) {
                    for (boolean tied : List.of(false, true)) {
                        if (tied && distinct < 2) {
                            continue;
                        }
                        int rounds = cast == roster ? 1 : 2;
                        BallotTelemetry telemetry = ballot(
                                roster, cast, distinct, tied, rounds, cast == roster ? 0 : 1);
                        validBallots.add(telemetry);
                    }
                }
            }
        }
        int validPairs = 0;
        for (BallotTelemetry wr05 : validBallots) {
            for (BallotTelemetry rp03 : validBallots) {
                ConductVerdict result = ConductVerdictDeriver.derive(
                        wr05, rp03, "free", "publish");
                check(result != null, "total conduct result");
                validPairs++;
            }
        }
        check(validPairs >= 1_000, "conduct enumeration coverage");
        expectThrows(IllegalArgumentException.class,
                () -> ConductVerdictDeriver.derive(unanimous, unanimous, "unknown", "publish"));
    }

    private static BallotTelemetry ballot(
            int roster,
            int cast,
            int distinct,
            boolean tied,
            int rounds,
            int disconnects) {
        return new BallotTelemetry(
                roster, roster, roster, cast, distinct, tied, rounds, disconnects);
    }

    private static void atomicProgressReloadCasAndCorruption(
            PhysicalPredicateAuthority authority) throws Exception {
        Path directory = Files.createTempDirectory("observance-v5-runtime-");
        try {
            Path progressPath = directory.resolve("progress-v5.json");
            V5ProgressStore store = V5ProgressStore.open(progressPath, authority);
            check(Files.isRegularFile(progressPath), "initial local progress commit");
            check(store.completeIfAbsent("v5_lc02_rations"), "first completion CAS");
            check(!store.completeIfAbsent("v5_lc02_rations"), "idempotent completion CAS");

            UUID playerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
            BallotTelemetry unanimous = ballot(3, 3, 1, false, 1, 0);
            long now = System.currentTimeMillis();
            EscrowEntry entry = new EscrowEntry(
                    "escrow-orientation-key",
                    "orientation_key",
                    Optional.of(playerId),
                    "forgotten_mouth",
                    13,
                    "0".repeat(64),
                    1,
                    now,
                    now,
                    EscrowStatus.HELD,
                    Map.of("reason", "disconnect_return"));
            store.transact(editor -> {
                editor.setBooleanTrue("v5_ls05_bound");
                for (PlayerBitDomain domain : PlayerBitDomain.values()) {
                    editor.addPlayerBit(playerId, domain, "bit_" + domain.name().toLowerCase());
                }
                editor.putBranchOnce("v5_wren_outcome", "understand");
                editor.putBranchOnce("v5_name_treatment", "publish");
                editor.putBallotOnce("WR05", unanimous);
                editor.putBallotOnce("RP03", unanimous);
                check(editor.deriveAndSetConductVerdict() == ConductVerdict.UNANIMOUS,
                        "transactional conduct derivation");
                editor.putEscrowOnce(entry);
                return null;
            });

            ProgressSnapshot committed = store.snapshot();
            check(committed.revision() == 2, "single revision per transaction");
            check(committed.players().get(playerId.toString()).inspections()
                            .contains("bit_inspection"),
                    "durable player inspection bit");
            check(committed.conductVerdict().orElseThrow() == ConductVerdict.UNANIMOUS,
                    "durable conduct verdict");
            check(committed.escrow().containsKey(entry.escrowId()), "durable local escrow");

            V5ProgressStore reloaded = V5ProgressStore.open(progressPath, authority);
            check(reloaded.snapshot().equals(committed), "atomic reload equality");
            try (var files = Files.list(directory)) {
                check(files.noneMatch(path -> path.getFileName().toString().contains(".tmp-")),
                        "no orphan atomic temp file");
            }

            concurrentCas(reloaded);
            V5ProgressStore postCasReload = V5ProgressStore.open(progressPath, authority);
            check(postCasReload.snapshot().isComplete("v5_lc03_daily_life"),
                    "concurrent CAS persisted");

            String corruptBytes = "{ definitely-not-json";
            Files.writeString(progressPath, corruptBytes, StandardCharsets.UTF_8);
            CorruptProgressException failure = expectThrows(
                    CorruptProgressException.class,
                    () -> V5ProgressStore.open(progressPath, authority));
            Path recovery = failure.recoveryCopy().orElseThrow();
            check(Files.readString(progressPath, StandardCharsets.UTF_8).equals(corruptBytes),
                    "corrupt original preserved");
            check(Files.readString(recovery, StandardCharsets.UTF_8).equals(corruptBytes),
                    "corrupt recovery copy preserved");
        } finally {
            deleteTree(directory);
        }
    }

    private static void concurrentCas(V5ProgressStore store) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Boolean>> attempts = new ArrayList<>();
            for (int index = 0; index < 24; index++) {
                attempts.add(() -> store.completeIfAbsent("v5_lc03_daily_life"));
            }
            List<Future<Boolean>> results = executor.invokeAll(attempts);
            int winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    winners++;
                }
            }
            check(winners == 1, "exactly one concurrent completion CAS winner");
        } finally {
            executor.shutdownNow();
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : ordered) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static <T extends Throwable> T expectThrows(
            Class<T> expected, ThrowingAction action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                return expected.cast(failure);
            }
            throw new AssertionError("expected " + expected.getSimpleName() + " but got " + failure,
                    failure);
        }
        throw new AssertionError("expected " + expected.getSimpleName() + " but nothing was thrown");
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }
}
