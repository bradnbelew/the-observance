package com.observance.watcher.morrow.room04.staticrestore;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority;
import com.observance.watcher.morrow.room04.RecoveryRoom04Manifest.Cell;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Candidate;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.CandidateId;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.EvidenceId;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Provenance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Main-driven M02 authority, predicate, theater, rollback, restart, and group matrix receipt. */
public final class StaticRestoreInvestigationSelfTest {
    private static final String RELEASE = "morrow.rehearsal.001";

    private StaticRestoreInvestigationSelfTest() { }

    public static void main(String[] args) throws Exception {
        StaticRestoreManifest manifest = new StaticRestoreManifest();
        manifestAndContentAreExact(manifest);
        correctPartialAndWrongScenarios(manifest);
        restartDisconnectAndRollbackScenarios(manifest);
        resetAndCatchUpScenario(manifest);
        oneTwoSixPlayerIdempotency(manifest);
        bukkitAdapterIsBounded();
        System.out.println("MORROW STATIC RESTORE M02: PASS cells=6 passes=3 evidence=4 players=1/2/6");
        System.out.println("M02_MANIFEST_SHA256=" + manifest.manifestSha256());
    }

    private static void manifestAndContentAreExact(StaticRestoreManifest manifest) {
        check(manifest.candidates().size() == 6, "M02 has exactly six physical diff cells");
        check(StaticRestoreManifest.MANIFEST_SHA256.equals(manifest.manifestSha256()),
                "M02 manifest/content hash remains pinned");
        check(manifest.passes().size() == 3
                        && manifest.passes().stream().allMatch(pass -> pass.candidates().size() == 2),
                "M02 restoration theater is three bounded two-cell passes");
        check(manifest.evidence().size() == 4, "M02 exposes all four canonical evidence sources");
        check(manifest.factualInferenceError().id() == CandidateId.B06
                        && manifest.factualInferenceError().factualProvenance() == Provenance.INFERRED,
                "B06 is the sole factual inference error");
        check(manifest.candidates().values().stream()
                        .filter(candidate -> candidate.factualProvenance() == Provenance.INFERRED).count() == 1,
                "no other restored block is inferred");
        check(!manifest.evidence(EvidenceId.ARCHIVED_SCREENSHOT).placedCells().contains(CandidateId.B06)
                        && !manifest.evidence(EvidenceId.BLOCK_MANIFEST).placedCells().contains(CandidateId.B06),
                "two authenticated sources independently omit B06");
        check(manifest.evidence(EvidenceId.FINCH_PLANNING_POST).content().contains("not placed"),
                "Finch source states discussion without placement as factual text");
        for (var source : manifest.evidence().values()) {
            check(!source.title().isBlank() && !source.custody().isBlank() && !source.content().isBlank(),
                    source.id() + " is independently openable and accessible as text");
        }
    }

    private static void correctPartialAndWrongScenarios(StaticRestoreManifest manifest) throws Exception {
        try (Fixture fixture = Fixture.create(manifest, "m02-correct-wrong-")) {
            StaticRestoreEngine.StepResult locked = fixture.engine.step(false);
            check(locked.status() == StaticRestoreEngine.StepStatus.LOCKED && fixture.world.writes == 0,
                    "restoration cannot mutate before proposal authentication");
            fixture.authenticateProposal();
            fixture.engine.step(true);
            check(fixture.engine.completedPasses() == 1, "first visible pass restores exactly two cells");

            Map<Cell, String> beforeWrong = fixture.world.snapshot();
            StaticRestorePredicate predicate = new StaticRestorePredicate(manifest);
            StaticRestorePredicate.Attempt partial = predicate.evaluate(
                    fixture.state.snapshot(), CandidateId.B06, Provenance.INFERRED, fixture.engine.complete());
            check(partial.status() == StaticRestorePredicate.Status.NOT_READY && !partial.commitsProof(),
                    "correct theory cannot bypass an incomplete physical restore");
            check(beforeWrong.equals(fixture.world.snapshot()), "partial attempt changes no blocks");

            StaticRestorePredicate.Attempt wrongCell = predicate.evaluate(
                    fixture.state.snapshot(), CandidateId.B01, Provenance.INFERRED, true);
            check(wrongCell.status() == StaticRestorePredicate.Status.WRONG && !wrongCell.commitsProof()
                            && wrongCell.feedback().contains("authenticated placement sources"),
                    "wrong physical cell receives authored source-responsive feedback");
            StaticRestorePredicate.Attempt wrongClass = predicate.evaluate(
                    fixture.state.snapshot(), CandidateId.B06, Provenance.AUTHENTICATED, true);
            check(wrongClass.status() == StaticRestorePredicate.Status.WRONG && !wrongClass.commitsProof()
                            && wrongClass.feedback().contains("Intention is not"),
                    "wrong B06 classification receives authored intention feedback");
            check(beforeWrong.equals(fixture.world.snapshot()), "wrong predicates never mutate the physical diff");
            check(fixture.state.pendingAfter(0).size() == 2, "wrong and partial attempts create no local event");

            fixture.completeRestore();
            StaticRestorePredicate.Attempt correct = predicate.evaluate(
                    fixture.state.snapshot(), CandidateId.B06, Provenance.INFERRED, fixture.engine.complete());
            check(correct.status() == StaticRestorePredicate.Status.CORRECT && correct.commitsProof(),
                    "only physical B06 plus inferred classification proves M02");
            String payload = new String(correct.payload(), StandardCharsets.UTF_8);
            for (String exact : new String[]{
                    "\"coordinate\":\"B06\"", "\"classification\":\"inferred\"",
                    "\"factual_error\":\"discussed_never_placed\"",
                    "\"world_mutation\":false", manifest.manifestSha256()}) {
                check(payload.contains(exact), "proof payload missing " + exact);
            }
            fixture.state.commit(correct.eventKey(), correct.idempotencyKey(), correct.payload());
            check(fixture.state.snapshot().committedEvents().contains(MorrowDialogAuthority.INTENTION_ERROR_PROVEN),
                    "correct physical predicate commits intention_error_proven");
        }
    }

    private static void restartDisconnectAndRollbackScenarios(StaticRestoreManifest manifest) throws Exception {
        try (Fixture fixture = Fixture.create(manifest, "m02-restart-")) {
            fixture.authenticateProposal();
            fixture.engine.step(true);
            Candidate partial = manifest.candidate(CandidateId.B03);
            fixture.world.setBlockData(partial.cell(), partial.restoredBlock());

            StaticRestoreEngine restarted = new StaticRestoreEngine(manifest, fixture.world);
            check(restarted.completedPasses() == 1, "restart derives one complete pass despite a partial second pass");
            restarted.step(true);
            check(restarted.completedPasses() == 2, "restart transaction safely completes the partial pass");

            StaticRestoreEngine rejoined = new StaticRestoreEngine(manifest, fixture.world);
            rejoined.step(true);
            check(rejoined.complete(), "disconnect/rejoin catch-up reaches the same shared physical state");
        }

        try (Fixture fixture = Fixture.create(manifest, "m02-rollback-")) {
            fixture.authenticateProposal();
            fixture.world.dropOnceAt = manifest.candidate(CandidateId.B02).cell();
            expectIo(() -> fixture.engine.step(true));
            fixture.engine.auditBaseline();
            check(fixture.world.snapshot().isEmpty(), "failed two-cell pass rolls every changed cell to baseline");
        }

        try (Fixture fixture = Fixture.create(manifest, "m02-drift-")) {
            fixture.authenticateProposal();
            fixture.world.blocks.put(manifest.candidate(CandidateId.B04).cell(), "minecraft:diamond_block");
            int writes = fixture.world.writes;
            expectIo(() -> fixture.engine.step(true));
            check(fixture.world.writes == writes, "foreign bounded drift halts without overwriting evidence");
        }
    }

    private static void resetAndCatchUpScenario(StaticRestoreManifest manifest) throws Exception {
        try (Fixture fixture = Fixture.create(manifest, "m02-reset-")) {
            fixture.authenticateProposal();
            fixture.completeRestore();
            fixture.engine.reset(true);
            fixture.engine.auditBaseline();
            StaticRestoreEngine catchUp = new StaticRestoreEngine(manifest, fixture.world);
            while (!catchUp.complete()) catchUp.step(true);
            catchUp.auditComplete();
            check(fixture.state.pendingAfter(0).size() == 2,
                    "reset and catch-up do not duplicate proposal or fabricate proof");
        }
    }

    private static void oneTwoSixPlayerIdempotency(StaticRestoreManifest manifest) throws Exception {
        for (int players : new int[]{1, 2, 6}) {
            try (Fixture fixture = Fixture.create(manifest, "m02-group-" + players + "-")) {
                fixture.authenticateProposal();
                fixture.completeRestore();
                StaticRestorePredicate predicate = new StaticRestorePredicate(manifest);
                long firstSequence = -1;
                for (int player = 0; player < players; player++) {
                    StaticRestorePredicate.Attempt attempt = predicate.evaluate(
                            fixture.state.snapshot(), CandidateId.B06, Provenance.INFERRED, true);
                    MorrowLocalState.CommitResult result = fixture.state.commit(
                            attempt.eventKey(), attempt.idempotencyKey(), attempt.payload());
                    if (player == 0) {
                        check(result.created(), players + "-player path creates one proof");
                        firstSequence = result.sequence();
                    } else {
                        check(!result.created() && result.sequence() == firstSequence,
                                players + "-player duplicate is group-idempotent");
                    }
                }
                check(fixture.state.pendingAfter(0).size() == 3,
                        players + "-player path retains room, proposal, and one proof only");
                MorrowLocalState restarted = MorrowLocalState.open(fixture.journal, RELEASE);
                check(restarted.pendingAfter(0).size() == 3
                                && restarted.snapshot().committedEvents().contains(
                                MorrowDialogAuthority.INTENTION_ERROR_PROVEN),
                        players + "-player proof reconstructs exactly after journal restart");
            }
        }
    }

    private static void bukkitAdapterIsBounded() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/room04/staticrestore/BukkitStaticRestore.java"));
        for (String required : new String[]{
                "PASS_INTERVAL_TICKS", "spawnParticle", "playSound", "PersistentDataType",
                "resetAndReplay", "StaticRestoreEngine", "requirePrimaryThread",
                "CANDIDATE_LABEL_SCALE", "EVIDENCE_LABEL_SCALE", "setTransformation"}) {
            check(source.contains(required), "Paper M02 adapter missing " + required);
        }
        for (String forbidden : new String[]{
                "PlayerMoveEvent", "runTaskAsynchronously", "net.minecraft", "craftbukkit", "sendBlockChange"}) {
            check(!source.contains(forbidden), "P0 item 6 crossed a forbidden boundary via " + forbidden);
        }
    }

    private static void expectIo(Throwing action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected IOException");
        } catch (IOException expected) {
            // expected
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface Throwing { void run() throws Exception; }

    private static final class FakeWorld implements StaticRestoreEngine.WorldPort {
        private final Map<Cell, String> blocks = new LinkedHashMap<>();
        private int writes;
        private Cell dropOnceAt;

        @Override
        public String blockData(Cell relative) {
            return blocks.getOrDefault(relative, StaticRestoreManifest.BASELINE_BLOCK);
        }

        @Override
        public void setBlockData(Cell relative, String blockData) {
            writes++;
            if (relative.equals(dropOnceAt)) {
                dropOnceAt = null;
                return;
            }
            if (StaticRestoreManifest.BASELINE_BLOCK.equals(blockData)) blocks.remove(relative);
            else blocks.put(relative, blockData);
        }

        private Map<Cell, String> snapshot() { return Map.copyOf(blocks); }
    }

    private static final class Fixture implements AutoCloseable {
        private final Path directory;
        private final Path journal;
        private final MorrowLocalState state;
        private final FakeWorld world;
        private final StaticRestoreEngine engine;

        private Fixture(
                Path directory,
                Path journal,
                MorrowLocalState state,
                FakeWorld world,
                StaticRestoreEngine engine) {
            this.directory = directory;
            this.journal = journal;
            this.state = state;
            this.world = world;
            this.engine = engine;
        }

        private static Fixture create(StaticRestoreManifest manifest, String prefix) throws IOException {
            Path directory = Files.createTempDirectory(prefix);
            Path journal = directory.resolve("morrow.journal");
            MorrowLocalState state = MorrowLocalState.open(journal, RELEASE);
            state.acceptProjection("morrow.act0.case_chain_authenticated", "copperline:case:m02", bytes("{}"));
            state.acceptProjection(MorrowDialogAuthority.CASE_HANDOFF, "copperline:handoff:m02", bytes("{}"));
            state.commit(MorrowDialogAuthority.ROOM_WITNESSED, "paper:room04:greeting:v1", bytes("{}"));
            FakeWorld world = new FakeWorld();
            return new Fixture(directory, journal, state, world, new StaticRestoreEngine(manifest, world));
        }

        private void authenticateProposal() throws IOException {
            MorrowDialogAuthority.Decision decision = MorrowDialogAuthority.decide(
                    MorrowDialogAuthority.Action.AUTHENTICATE_PROPOSAL, state.snapshot());
            state.commit(decision.eventKey(), decision.idempotencyKey(), decision.payload());
        }

        private void completeRestore() throws IOException {
            while (!engine.complete()) engine.step(true);
            engine.auditComplete();
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(journal);
            Files.deleteIfExists(directory);
        }
    }
}
