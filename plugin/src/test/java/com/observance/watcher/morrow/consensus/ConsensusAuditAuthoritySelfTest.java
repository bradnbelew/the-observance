package com.observance.watcher.morrow.consensus;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.consensus.ConsensusAuditAuthority.Classification;
import com.observance.watcher.morrow.consensus.ConsensusAuditAuthority.Progress;
import com.observance.watcher.morrow.consensus.ConsensusAuditAuthority.Result;
import com.observance.watcher.morrow.consensus.ConsensusAuditAuthority.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Main-driven M06 private receipt, classification, cohort, restart, and corruption matrix. */
public final class ConsensusAuditAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.m06";

    private ConsensusAuditAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        voteLedgerIsSignedReadableAndActuallyDissenting();
        lockAndWrongAnswersAreImmutable();
        oneTwoSixPlayerAuditsCommitExactlyOnce();
        receiptsReissueAndProgressSurvivesRestart();
        physicalManifestInstallerAndPaperAdapter();
        System.out.println("MORROW CONSENSUS AUDIT M06: PASS votes=6 counted=4 excluded=2 players=1/2/6");
    }

    private static void voteLedgerIsSignedReadableAndActuallyDissenting() {
        check(ConsensusAuditAuthority.VOTES.size() == 6
                        && ConsensusAuditAuthority.VOTES.stream().map(v -> v.id()).distinct().count() == 6,
                "M06 has six unique human-readable signed receipt IDs");
        check(ConsensusAuditAuthority.VOTES.stream().filter(v -> v.counted()).count() == 4
                        && ConsensusAuditAuthority.VOTES.stream().filter(v -> !v.counted()).count() == 2,
                "M06 published 4/4 is produced by excluding exactly two retained votes");
        check(ConsensusAuditAuthority.VOTES.stream().filter(v -> !v.counted())
                        .allMatch(v -> v.vote() == ConsensusAuditAuthority.Vote.SHUT_DOWN)
                        && ConsensusAuditAuthority.VOTES.stream().filter(v -> !v.counted())
                        .map(v -> v.id()).collect(java.util.stream.Collectors.toSet())
                        .equals(ConsensusAuditAuthority.DISSENT_IDS),
                "M06 excluded receipts are the authentic shutdown dissent");
        check(ConsensusAuditAuthority.POLICY.contains("0.80")
                        && ConsensusAuditAuthority.PUBLISHED_RESULT.contains("4/4 eligible"),
                "M06 exposes the policy and published denominator in plain language");
    }

    private static void lockAndWrongAnswersAreImmutable() throws Exception {
        try (Fixture fixture = Fixture.create("m06-wrong-")) {
            UUID player = id("wrong");
            Result locked = ConsensusAuditAuthority.inspect(
                    Progress.initial(), fixture.state.snapshot(), false, "M06-HAL-0D31", player);
            check(locked.status() == Status.LOCKED && locked.progress().revision() == 0,
                    "M06 remains locked until M05 preservation");
            Progress progress = inspectAll(fixture, 1);
            for (Classification wrong : List.of(Classification.UNANIMOUS, Classification.FORGED_VOTES)) {
                Progress selected = select(progress, fixture, ConsensusAuditAuthority.DISSENT_IDS);
                Result result = ConsensusAuditAuthority.submit(selected, fixture.state.snapshot(), true,
                        wrong, player);
                check(result.status() == Status.WRONG_CLASSIFICATION && !result.commitsEvent()
                                && result.feedback().contains("denominator"),
                        wrong + " receives the authored policy explanation without mutation");
            }
            Progress wrongIds = select(progress, fixture, Set.of("M06-HAL-0D31", "M06-TES-8C04"));
            Result ids = ConsensusAuditAuthority.submit(wrongIds, fixture.state.snapshot(), true,
                    Classification.INCOMPLETE_CONSENSUS, player);
            check(ids.status() == Status.WRONG_RECEIPTS && !ids.commitsEvent(),
                    "wrong M06 receipt pair neither changes votes nor advances state");
            check(!fixture.state.snapshot().committedEvents().contains(ConsensusAuditAuthority.EVENT),
                    "wrong M06 answers fabricate no Act 3 event");
        }
    }

    private static void oneTwoSixPlayerAuditsCommitExactlyOnce() throws Exception {
        for (int count : new int[]{1, 2, 6}) {
            try (Fixture fixture = Fixture.create("m06-group-" + count + "-")) {
                Progress progress = inspectAll(fixture, count);
                progress = select(progress, fixture, ConsensusAuditAuthority.DISSENT_IDS);
                Result result = ConsensusAuditAuthority.submit(progress, fixture.state.snapshot(), true,
                        Classification.INCOMPLETE_CONSENSUS,
                        id(count + "-submitter"));
                check(result.status() == Status.COMMIT && result.commitsEvent(),
                        count + "-player M06 earns incomplete consensus only after all receipts");
                byte[] payload = ConsensusAuditAuthority.payload(result);
                MorrowLocalState.CommitResult first = fixture.state.commit(
                        result.eventKey(), result.idempotencyKey(), payload);
                MorrowLocalState.CommitResult duplicate = fixture.state.commit(
                        result.eventKey(), result.idempotencyKey(), payload);
                check(first.created() && !duplicate.created() && first.sequence() == duplicate.sequence(),
                        count + "-player M06 journal commit is idempotent");
                String text = new String(payload, StandardCharsets.UTF_8);
                for (String required : new String[]{"incomplete_consensus", "M06-ION-2F17",
                        "M06-TES-8C04", "\"votes_altered\":false", "\"excluded\":2"}) {
                    check(text.contains(required), count + "-player M06 payload lacks " + required);
                }
                Result after = ConsensusAuditAuthority.submit(progress, fixture.state.snapshot(), true,
                        Classification.INCOMPLETE_CONSENSUS,
                        id("duplicate"));
                check(after.status() == Status.DUPLICATE && !after.commitsEvent(),
                        count + "-player M06 duplicate interaction is read-only");
            }
        }
    }

    private static void receiptsReissueAndProgressSurvivesRestart() throws Exception {
        Path directory = Files.createTempDirectory("m06-store-");
        Path path = directory.resolve("consensus.progress");
        try (Fixture fixture = Fixture.create("m06-store-state-")) {
            UUID player = id("original");
            Result first = ConsensusAuditAuthority.inspect(Progress.initial(), fixture.state.snapshot(),
                    true, "M06-ION-2F17", player);
            Result reissued = ConsensusAuditAuthority.inspect(first.progress(), fixture.state.snapshot(),
                    true, "M06-ION-2F17", id("requester"));
            check(reissued.status() == Status.REISSUED
                            && reissued.progress().revision() == first.progress().revision()
                            && reissued.receipt().originalCustodian().equals(player),
                    "M06 reissues the original immutable receipt without rewriting custody");
            ConsensusAuditProgressStore store = new ConsensusAuditProgressStore(path, RELEASE);
            store.save(first.progress());
            check(new ConsensusAuditProgressStore(path, RELEASE).load().equals(first.progress()),
                    "M06 private custody survives restart exactly");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(2, "revision=99");
            Files.write(path, lines, StandardCharsets.UTF_8);
            try {
                store.load();
                throw new AssertionError("expected M06 corruption failure");
            } catch (IOException expected) {
                // expected
            }
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    private static Progress inspectAll(Fixture fixture, int count) {
        Progress progress = Progress.initial();
        HashSet<UUID> custodians = new HashSet<>();
        for (int index = 0; index < ConsensusAuditAuthority.VOTES.size(); index++) {
            UUID player = id(count + "-" + (index % count));
            custodians.add(player);
            Result result = ConsensusAuditAuthority.inspect(progress, fixture.state.snapshot(), true,
                    ConsensusAuditAuthority.VOTES.get(index).id(), player);
            check(result.status() == Status.OBSERVED && result.receipt() != null,
                    "M06 receipt " + index + " was not privately delivered");
            progress = result.progress();
        }
        check(custodians.size() == count && progress.custodians().size() == 6,
                count + "-player M06 distributes all six receipts across the available cohort");
        return progress;
    }

    private static void physicalManifestInstallerAndPaperAdapter() throws Exception {
        ConsensusAuditManifest manifest = new ConsensusAuditManifest();
        check(manifest.cells().size() == 1309 && manifest.terminals().size() == 6
                        && manifest.lamps().size() == 6
                        && manifest.manifestSha256().matches("[0-9a-f]{64}"),
                "M06 manifest is one exact bounded chamber with six equal terminals");
        Path directory = Files.createTempDirectory("m06-installer-");
        Path receipt = directory.resolve("m06.receipt");
        try {
            FakeWorld world = new FakeWorld();
            ConsensusAuditInstaller installer = new ConsensusAuditInstaller(manifest, receipt);
            ConsensusAuditInstaller.Result built = installer.install(RELEASE,
                    new ConsensusAuditInstaller.Origin(64, 80, 0), world);
            check(built.status() == ConsensusAuditInstaller.Status.BUILT && built.blockCount() == 1309,
                    "M06 empty-target installer builds and audits every chamber cell");
            check(installer.install(RELEASE, new ConsensusAuditInstaller.Origin(64, 80, 0), world).status()
                            == ConsensusAuditInstaller.Status.ALREADY_PRESENT,
                    "M06 installer validates its release/world/origin receipt after restart");
            ConsensusAuditManifest.Cell lamp = manifest.lamps().values().iterator().next();
            world.setBlockData(lamp, "minecraft:copper_bulb[lit=true,powered=false]");
            installer.audit(world, true);
            try {
                installer.audit(world, false);
                throw new AssertionError("expected strict M06 lamp audit failure");
            } catch (IOException expected) {
                // expected
            }
        } finally {
            Files.deleteIfExists(receipt);
            Files.deleteIfExists(directory);
        }
        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/consensus/BukkitConsensusAudit.java"))
                + Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/consensus/BukkitConsensusAuditWorld.java"));
        for (String required : new String[]{"RIGHT_CLICK_BLOCK", "LEFT_CLICK_BLOCK",
                "PlayerCustomClickEvent", "DialogType.multiAction", "canCloseWithEscape(true)",
                "PRIVATE VOTE RECEIPT", "getBlockData().matches(expected)"}) {
            check(source.contains(required), "M06 Paper adapter missing " + required);
        }
        for (String forbidden : new String[]{"PlayerMoveEvent", "runTaskAsynchronously",
                "net.minecraft", "craftbukkit", "sendBlockChange", "waterlogged"}) {
            check(!source.contains(forbidden), "M06 Paper adapter crossed forbidden boundary via " + forbidden);
        }
    }

    private static Progress select(Progress progress, Fixture fixture, Set<String> ids) {
        for (String receiptId : ids) {
            Result result = ConsensusAuditAuthority.toggle(progress, fixture.state.snapshot(), true,
                    receiptId, id("selector-" + receiptId));
            check(result.status() == Status.SELECTED, "M06 could not mark " + receiptId);
            progress = result.progress();
        }
        return progress;
    }

    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(("m06-" + value).getBytes(StandardCharsets.UTF_8));
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture implements AutoCloseable {
        private final Path directory;
        private final MorrowLocalState state;
        private Fixture(Path directory, MorrowLocalState state) {
            this.directory = directory;
            this.state = state;
        }
        static Fixture create(String prefix) throws Exception {
            Path directory = Files.createTempDirectory(prefix);
            MorrowLocalState state = MorrowLocalState.open(directory.resolve("morrow.journal"), RELEASE);
            String[] events = {
                    "morrow.act0.case_chain_authenticated", "morrow.act0.server_handoff_recovered",
                    "morrow.act1.room04_witnessed", "morrow.act1.static_proposal_authenticated",
                    "morrow.act1.intention_error_proven", "morrow.act1.entity_replay_authorized",
                    "morrow.act2.missing_role_completed", "morrow.act2.live_test_recorded",
                    "morrow.act2.behavior_reuse_proven", "morrow.act2.private_contradiction_resolved",
                    "morrow.act2.live_capture_authorized", "morrow.act3.version_fragments_authenticated",
            };
            for (int index = 0; index < events.length; index++) {
                String event = events[index];
                if (event.startsWith("morrow.act0") || event.equals("morrow.act2.private_contradiction_resolved")) {
                    state.acceptProjection(event, "m06:external:" + index, bytes("{}"));
                } else {
                    state.commit(event, "m06:minecraft:" + index, bytes("{}"));
                }
            }
            return new Fixture(directory, state);
        }
        @Override public void close() throws IOException {
            Files.deleteIfExists(directory.resolve("morrow.journal"));
            Files.deleteIfExists(directory);
        }
    }
    private static final class FakeWorld implements ConsensusAuditInstaller.WorldPort {
        private final Map<ConsensusAuditManifest.Cell, String> blocks = new LinkedHashMap<>();
        @Override public String binding() { return "m06-test-world:00000000-0000-0000-0000-000000000006"; }
        @Override public String blockData(ConsensusAuditManifest.Cell relative) {
            return blocks.getOrDefault(relative, "minecraft:air");
        }
        @Override public boolean isAir(ConsensusAuditManifest.Cell relative) {
            return "minecraft:air".equals(blockData(relative));
        }
        @Override public void setBlockData(ConsensusAuditManifest.Cell relative, String blockData) {
            if ("minecraft:air".equals(blockData)) blocks.remove(relative);
            else blocks.put(relative, blockData);
        }
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
