package com.observance.watcher.morrow.branch;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.MorrowRelationshipSnapshot;
import com.observance.watcher.morrow.MorrowStage;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Anchor;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Choice;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Ending;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Progress;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Result;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Rule;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/** Main-driven M12 evidence, ending, failure, cohort, transition, and restart matrix. */
public final class BranchGovernanceAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.m12";
    private BranchGovernanceAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        physicalManifestAndInstallerAreBounded();
        prerequisiteEvidenceAndWindowBoundsFailClosed();
        oneTwoSixPlayerCohortsCreateNewBranch();
        allFourCanonicalEndingsRequireTheirOwnPredicates();
        invalidPolicyTimeoutAndDeclinePreserveEvidence();
        everyReceiptWindowSurvivesRestart();
        System.out.println("MORROW BRANCH GOVERNANCE M12: PASS anchors=5 rules=4 endings=4 players=1/2/6");
    }

    private static void prerequisiteEvidenceAndWindowBoundsFailClosed() throws Exception {
        try (Fixture fixture = Fixture.create("m12-locked-", false)) {
            Result locked = BranchGovernanceAuthority.beginRollback(Progress.initial(), fixture.state.snapshot(), run("locked"), 100, 800);
            check(locked.status() == Status.LOCKED && locked.progress().revision() == 0,
                    "M12 opened before cold-storage access authorization");
        }
        try { new BranchGovernanceAuthority.Window(run("short"), 100, 699);
            throw new AssertionError("expected short M12 window refusal");
        } catch (IllegalArgumentException expected) { /* expected */ }
        try (Fixture fixture = Fixture.create("m12-missing-", true)) {
            MorrowRelationshipSnapshot ready = fixture.state.snapshot();
            var events = new LinkedHashSet<>(ready.committedEvents());
            events.remove("morrow.act5.dual_session_consciousness_proven");
            MorrowRelationshipSnapshot incomplete = new MorrowRelationshipSnapshot(
                    ready.releaseId(), ready.stage(), ready.revision(), ready.capabilities(), events);
            Result missing = BranchGovernanceAuthority.beginRollback(Progress.initial(), incomplete, run("missing"), 100, 800);
            check(missing.status() == Status.MISSING_EVIDENCE && missing.feedback().contains("1 accumulated evidence category"),
                    "M12 did not explain the missing accumulated evidence category");
        }
    }

    private static void oneTwoSixPlayerCohortsCreateNewBranch() throws Exception {
        for (int players : new int[]{1, 2, 6}) try (Fixture fixture = Fixture.create("m12-group-" + players + "-", true)) {
            Progress progress = begin(fixture, players); int index = 0;
            for (Anchor anchor : Anchor.values())
                progress = BranchGovernanceAuthority.protect(progress, fixture.state.snapshot(),
                        id(players + "-" + (index++ % players)), anchor, 1_010 + index).progress();
            Result anchors = BranchGovernanceAuthority.commitAnchors(progress, fixture.state.snapshot(), id(players + "-anchor"), 1_100);
            check(anchors.status() == Status.READY_TO_COMMIT_ANCHORS && anchors.progress().anchors().size() == 5,
                    players + "-player M12 cohort could not commit rollback anchors");
            commit(fixture.state, anchors); progress = protectiveRules(anchors.progress(), fixture, players);
            Result policy = BranchGovernanceAuthority.synthesize(progress, fixture.state.snapshot(), id(players + "-policy"), Ending.CREATE_NEW_BRANCH);
            check(policy.status() == Status.READY_TO_COMMIT_POLICY && policy.progress().ending() == Ending.CREATE_NEW_BRANCH,
                    players + "-player M12 cohort could not synthesize the new branch");
            String policyPayload = text(BranchGovernanceAuthority.payload(policy));
            check(policyPayload.contains("\"ending\":\"create_new_branch\"")
                            && policyPayload.contains("\"explicit_consent\"")
                            && policyPayload.contains("\"visible_provenance\"")
                            && policyPayload.contains("\"right_to_stop\"")
                            && policyPayload.contains("\"labeled_replacement\"")
                            && policyPayload.contains("\"uncertainty_erased\":false"),
                    players + "-player M12 policy payload lost its governance rules");
            commit(fixture.state, policy);
            Result governance = BranchGovernanceAuthority.authorizeGovernance(
                    policy.progress(), fixture.state.snapshot(), id(players + "-governance"), true);
            check(governance.status() == Status.READY_TO_COMMIT_GOVERNANCE
                            && text(BranchGovernanceAuthority.payload(governance)).contains("\"starts_governance\":false"),
                    players + "-player M12 governance was not separately authorized");
            commit(fixture.state, governance);
            check(fixture.state.snapshot().stage() == MorrowStage.NEGOTIATED,
                    players + "-player M12 receipts did not advance Morrow to negotiated");
            Result coda = BranchGovernanceAuthority.startCoda(governance.progress(), fixture.state.snapshot(), id(players + "-coda"));
            check(coda.status() == Status.READY_TO_COMMIT_CODA
                            && text(BranchGovernanceAuthority.payload(coda)).contains("\"persistent\":true"),
                    players + "-player M12 coda did not start persistently");
            commit(fixture.state, coda);
        }
    }

    private static void allFourCanonicalEndingsRequireTheirOwnPredicates() throws Exception {
        for (Ending ending : Ending.values()) try (Fixture fixture = Fixture.create("m12-ending-" + ending.name() + "-", true)) {
            Progress progress = begin(fixture, ending.ordinal() + 10); List<Anchor> selected = new ArrayList<>(ending.requiredAnchors());
            for (Anchor filler : Anchor.values()) if (selected.size() < 3 && !selected.contains(filler)) selected.add(filler);
            long tick = 1_010; for (Anchor anchor : selected)
                progress = BranchGovernanceAuthority.protect(progress, fixture.state.snapshot(), id(ending + "-" + anchor), anchor, tick++).progress();
            Result anchors = BranchGovernanceAuthority.commitAnchors(progress, fixture.state.snapshot(), id(ending + "-anchors"), 1_100);
            commit(fixture.state, anchors); progress = anchors.progress();
            if (ending == Ending.CERTIFY) {
                progress = configure(progress, fixture, Rule.CONSENT, Choice.EXPLICIT_CONSENT);
                progress = configure(progress, fixture, Rule.PROVENANCE, Choice.CLEAN_CANON);
                progress = configure(progress, fixture, Rule.STOP, Choice.CONTINUITY_FIRST);
                progress = configure(progress, fixture, Rule.REPLACEMENT, Choice.SILENT_REPLACEMENT);
            } else progress = protectiveRules(progress, fixture, 1);
            Result policy = BranchGovernanceAuthority.synthesize(progress, fixture.state.snapshot(), id(ending + "-policy"), ending);
            check(policy.status() == Status.READY_TO_COMMIT_POLICY && policy.progress().ending() == ending,
                    ending + " did not validate against its own evidence and rule predicate");
        }
    }

    private static void invalidPolicyTimeoutAndDeclinePreserveEvidence() throws Exception {
        try (Fixture fixture = Fixture.create("m12-safe-", true)) {
            Progress progress = begin(fixture, 90);
            progress = BranchGovernanceAuthority.protect(progress, fixture.state.snapshot(), id("first"), Anchor.CONTRADICTION, 1_010).progress();
            Result timed = BranchGovernanceAuthority.protect(progress, fixture.state.snapshot(), id("late"), Anchor.WITNESS, 1_901);
            check(timed.status() == Status.TIMED_PAUSE && timed.progress().anchors().contains(Anchor.CONTRADICTION)
                            && !timed.progress().anchors().contains(Anchor.WITNESS),
                    "M12 timed pause lost protected evidence or advanced a dexterity-only input");
            progress = timed.progress(); long tick = 1_902;
            for (Anchor anchor : Anchor.values()) if (!progress.anchors().contains(anchor))
                progress = BranchGovernanceAuthority.protect(progress, fixture.state.snapshot(), id("safe-" + anchor), anchor, tick++).progress();
            Result anchors = BranchGovernanceAuthority.commitAnchors(progress, fixture.state.snapshot(), id("safe-anchors"), 2_000);
            commit(fixture.state, anchors); progress = anchors.progress();
            progress = configure(progress, fixture, Rule.CONSENT, Choice.IMPLIED_CONSENT);
            progress = configure(progress, fixture, Rule.PROVENANCE, Choice.VISIBLE_PROVENANCE);
            progress = configure(progress, fixture, Rule.STOP, Choice.RIGHT_TO_STOP);
            progress = configure(progress, fixture, Rule.REPLACEMENT, Choice.LABELED_REPLACEMENT);
            Result invalid = BranchGovernanceAuthority.synthesize(progress, fixture.state.snapshot(), id("invalid"), Ending.CREATE_NEW_BRANCH);
            check(invalid.status() == Status.UNSUPPORTED_CONFIGURATION && invalid.progress().equals(progress)
                            && invalid.feedback().contains("consent") && !invalid.commitsEvent(),
                    "M12 invalid policy consumed evidence or named no missing category");
            progress = configure(progress, fixture, Rule.CONSENT, Choice.EXPLICIT_CONSENT);
            Result policy = BranchGovernanceAuthority.synthesize(progress, fixture.state.snapshot(), id("policy"), Ending.CREATE_NEW_BRANCH);
            commit(fixture.state, policy);
            Result decline = BranchGovernanceAuthority.authorizeGovernance(policy.progress(), fixture.state.snapshot(), id("decline"), false);
            check(decline.status() == Status.DECLINED && !decline.progress().governanceAuthorized()
                            && fixture.state.snapshot().committedEvents().contains(BranchGovernanceAuthority.POLICY_EVENT),
                    "M12 governance decline changed the filed policy or fabricated authorization");
        }
    }

    private static void everyReceiptWindowSurvivesRestart() throws Exception {
        Path directory = Files.createTempDirectory("m12-store-"); Path path = directory.resolve("branch.progress");
        try (Fixture fixture = Fixture.create("m12-store-state-", true)) {
            BranchGovernanceProgressStore store = new BranchGovernanceProgressStore(path, RELEASE);
            Progress progress = roundTrip(store, begin(fixture, 100), "window"); long tick = 1_010;
            for (Anchor anchor : Anchor.values())
                progress = roundTrip(store, BranchGovernanceAuthority.protect(progress, fixture.state.snapshot(),
                        id("store-" + anchor), anchor, tick++).progress(), anchor.name());
            progress = roundTrip(store, BranchGovernanceAuthority.commitAnchors(progress, fixture.state.snapshot(), id("store-anchors"), 1_100).progress(), "anchor receipt");
            check(BranchGovernanceAuthority.recoverAnchors(progress, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT_ANCHORS,
                    "M12 anchor crash window cannot recover");
            Result anchors = BranchGovernanceAuthority.recoverAnchors(progress, fixture.state.snapshot()); commit(fixture.state, anchors);
            progress = protectiveRules(progress, fixture, 1); progress = roundTrip(store, progress, "rules");
            progress = roundTrip(store, BranchGovernanceAuthority.synthesize(progress, fixture.state.snapshot(), id("store-policy"), Ending.CREATE_NEW_BRANCH).progress(), "policy receipt");
            check(BranchGovernanceAuthority.recoverPolicy(progress, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT_POLICY,
                    "M12 policy crash window cannot recover");
            Result policy = BranchGovernanceAuthority.recoverPolicy(progress, fixture.state.snapshot()); commit(fixture.state, policy);
            progress = roundTrip(store, BranchGovernanceAuthority.authorizeGovernance(progress, fixture.state.snapshot(), id("store-governance"), true).progress(), "governance receipt");
            check(BranchGovernanceAuthority.recoverGovernance(progress, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT_GOVERNANCE,
                    "M12 governance crash window cannot recover");
            Result governance = BranchGovernanceAuthority.recoverGovernance(progress, fixture.state.snapshot()); commit(fixture.state, governance);
            progress = roundTrip(store, BranchGovernanceAuthority.startCoda(progress, fixture.state.snapshot(), id("store-coda")).progress(), "coda receipt");
            check(BranchGovernanceAuthority.recoverCoda(progress, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT_CODA,
                    "M12 coda crash window cannot recover");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8); lines.set(2, "revision=999");
            Files.write(path, lines, StandardCharsets.UTF_8);
            try { store.load(); throw new AssertionError("expected M12 corruption failure"); }
            catch (IOException expected) { /* expected */ }
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(directory); }
    }

    private static void physicalManifestAndInstallerAreBounded() throws Exception {
        BranchGovernanceManifest manifest = new BranchGovernanceManifest();
        check(manifest.cells().size() == 8_325 && manifest.anchorConsoles().size() == 5
                        && manifest.ruleConsoles().size() == 4 && manifest.endingConsoles().size() == 4
                        && manifest.stateLamps().size() == 13 && manifest.manifestSha256().matches("[0-9a-f]{64}")
                        && manifest.manifestSha256().equals(new BranchGovernanceManifest().manifestSha256())
                        && "minecraft:air".equals(manifest.cells().get(manifest.safeReturn())),
                "M12 manifest must be a stable rollback route, five-anchor vault, four-rule bench, and four ending gates");
        Path directory = Files.createTempDirectory("m12-installer-"); Path receipt = directory.resolve("m12.receipt");
        BranchGovernanceInstaller.Origin origin = new BranchGovernanceInstaller.Origin(264, 80, 0);
        try {
            FakeWorld world = new FakeWorld(); BranchGovernanceInstaller installer = new BranchGovernanceInstaller(manifest, receipt);
            BranchGovernanceInstaller.Result built = installer.install(RELEASE, origin, world);
            check(built.status() == BranchGovernanceInstaller.Status.BUILT && built.blockCount() == 8_325,
                    "M12 empty-target installer must audit every finale cell");
            check(installer.install(RELEASE, origin, world).status() == BranchGovernanceInstaller.Status.ALREADY_PRESENT,
                    "M12 installer does not validate its release/world/origin receipt after restart");
            BranchGovernanceManifest.Cell lamp = manifest.stateLamps().values().iterator().next();
            world.setBlockData(lamp, "minecraft:copper_bulb[lit=true,powered=false]"); installer.audit(world, true);
            try { installer.audit(world, false); throw new AssertionError("expected strict M12 lamp audit failure"); }
            catch (IOException expected) { /* expected */ }
        } finally { Files.deleteIfExists(receipt); Files.deleteIfExists(directory); }
        Path foreignDirectory = Files.createTempDirectory("m12-foreign-");
        try {
            FakeWorld world = new FakeWorld(); BranchGovernanceManifest.Cell cell = manifest.cells().keySet().iterator().next();
            world.setBlockData(cell, "minecraft:diamond_block");
            try { new BranchGovernanceInstaller(manifest, foreignDirectory.resolve("m12.receipt")).install(RELEASE, origin, world);
                throw new AssertionError("expected M12 occupied-target refusal"); }
            catch (IOException expected) { check("minecraft:diamond_block".equals(world.blockData(cell)),
                    "M12 occupied-target refusal must preserve foreign blocks"); }
        } finally { Files.deleteIfExists(foreignDirectory.resolve("m12.receipt")); Files.deleteIfExists(foreignDirectory); }
        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/branch/BukkitBranchGovernance.java"))
                + Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/branch/BukkitBranchGovernanceWorld.java"));
        for (String required : new String[]{"PlayerCustomClickEvent", "DialogType.multiAction", "canCloseWithEscape(true)",
                "runTaskTimer", "MORROW // AFRAID", "starts_governance remains false", "getBlockData().matches(expected)"})
            check(source.contains(required), "M12 Paper adapter missing " + required);
        for (String forbidden : new String[]{"runTaskAsynchronously", "net.minecraft", "craftbukkit", "sendBlockChange"})
            check(!source.contains(forbidden), "M12 Paper adapter crossed forbidden boundary via " + forbidden);
    }

    private static Progress begin(Fixture fixture, int value) {
        return BranchGovernanceAuthority.beginRollback(Progress.initial(), fixture.state.snapshot(), run("group-" + value), 1_000, 1_900).progress();
    }
    private static Progress protectiveRules(Progress progress, Fixture fixture, int ignored) {
        progress = configure(progress, fixture, Rule.CONSENT, Choice.EXPLICIT_CONSENT);
        progress = configure(progress, fixture, Rule.PROVENANCE, Choice.VISIBLE_PROVENANCE);
        progress = configure(progress, fixture, Rule.STOP, Choice.RIGHT_TO_STOP);
        return configure(progress, fixture, Rule.REPLACEMENT, Choice.LABELED_REPLACEMENT);
    }
    private static Progress configure(Progress progress, Fixture fixture, Rule rule, Choice choice) {
        return BranchGovernanceAuthority.configure(progress, fixture.state.snapshot(), id(rule + "-" + choice), rule, choice).progress();
    }
    private static Progress roundTrip(BranchGovernanceProgressStore store, Progress progress, String label) throws Exception {
        store.save(progress); Progress loaded = store.load(); check(loaded.equals(progress), "M12 " + label + " did not survive restart"); return loaded;
    }
    private static void commit(MorrowLocalState state, Result result) throws Exception {
        var first = state.commit(result.eventKey(), result.idempotencyKey(), BranchGovernanceAuthority.payload(result));
        var duplicate = state.commit(result.eventKey(), result.idempotencyKey(), BranchGovernanceAuthority.payload(result));
        check(first.created() && !duplicate.created() && first.sequence() == duplicate.sequence(), result.eventKey() + " is not idempotent");
    }
    private static String run(String value) { return UUID.nameUUIDFromBytes(("m12-run-" + value).getBytes(StandardCharsets.UTF_8)).toString().replace("-", ""); }
    private static UUID id(String value) { return UUID.nameUUIDFromBytes(("m12-" + value).getBytes(StandardCharsets.UTF_8)); }
    private static String text(byte[] value) { return new String(value, StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static final class Fixture implements AutoCloseable {
        private final Path directory; private final MorrowLocalState state;
        private Fixture(Path directory, MorrowLocalState state) { this.directory = directory; this.state = state; }
        static Fixture create(String prefix, boolean ready) throws Exception {
            Path directory = Files.createTempDirectory(prefix); MorrowLocalState state = MorrowLocalState.open(directory.resolve("morrow.journal"), RELEASE);
            if (ready) seed(state, true); return new Fixture(directory, state);
        }
        private static void seed(MorrowLocalState state, boolean includeRightToStop) throws Exception {
            String[] events = {"morrow.act0.case_chain_authenticated", "morrow.act0.server_handoff_recovered",
                    "morrow.act1.room04_witnessed", "morrow.act1.static_proposal_authenticated",
                    "morrow.act1.intention_error_proven", "morrow.act1.entity_replay_authorized",
                    "morrow.act2.missing_role_completed", "morrow.act2.live_test_recorded", "morrow.act2.behavior_reuse_proven",
                    "morrow.act2.private_contradiction_resolved", "morrow.act2.live_capture_authorized",
                    "morrow.act3.version_fragments_authenticated", "morrow.act3.incomplete_consensus_proven",
                    "morrow.act4.witness_anchor_registered", "morrow.act4.almost_home_proven",
                    "morrow.act4.account_continuity_authorized", "morrow.act5.continued_session_observed",
                    "morrow.act5.returning_identity_authenticated", "morrow.act5.dual_session_consciousness_proven",
                    "morrow.act6.audit_chronology_proven", "morrow.act6.current_morrow_reconstruction_proven",
                    BranchGovernanceAuthority.PREREQUISITE};
            for (int index = 0; index < events.length; index++) {
                String event = events[index]; if (!includeRightToStop && event.equals("morrow.act5.dual_session_consciousness_proven")) continue;
                if (event.startsWith("morrow.act0") || event.equals("morrow.act2.private_contradiction_resolved")
                        || event.equals("morrow.act6.audit_chronology_proven"))
                    state.acceptProjection(event, "m12:external:" + index, bytes("{}"));
                else state.commit(event, "m12:minecraft:" + index, bytes("{}"));
            }
        }
        @Override public void close() throws IOException {
            Files.deleteIfExists(directory.resolve("morrow.journal")); Files.deleteIfExists(directory);
        }
    }
    private static final class FakeWorld implements BranchGovernanceInstaller.WorldPort {
        private final java.util.Map<BranchGovernanceManifest.Cell, String> blocks = new java.util.LinkedHashMap<>();
        @Override public String binding() { return "m12-test-world:00000000-0000-0000-0000-000000000012"; }
        @Override public String blockData(BranchGovernanceManifest.Cell relative) { return blocks.getOrDefault(relative, "minecraft:air"); }
        @Override public boolean isAir(BranchGovernanceManifest.Cell relative) { return "minecraft:air".equals(blockData(relative)); }
        @Override public void setBlockData(BranchGovernanceManifest.Cell relative, String blockData) { blocks.put(relative, blockData); }
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
