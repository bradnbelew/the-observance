package com.observance.watcher.morrow.almost;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.ContinuityDecision;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.DecodeChoice;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.EvidenceId;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.Progress;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.ProvenanceChoice;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.Result;
import com.observance.watcher.morrow.almost.AlmostHomeAuthority.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Main-driven M08 chronology, negative-evidence, consent, cohort, and restart matrix. */
public final class AlmostHomeAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.m08";

    private AlmostHomeAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        cipherAndLockedBoundaryAreExact();
        oneTwoSixPlayerChronologyAndNegativeEvidence();
        wrongDecodeAndProvenanceNeverDestroyTheHouse();
        almostHomeAndBoundedContinuityCommitExactlyOnce();
        partialAndProvenProgressSurviveRestart();
        physicalManifestAndInstallerAreBounded();
        System.out.println("MORROW ALMOST HOME M08: PASS sources=4 cipher=vigenere house=preserved players=1/2/6");
    }

    private static void cipherAndLockedBoundaryAreExact() throws Exception {
        check(AlmostHomeAuthority.decodeVigenere(
                        AlmostHomeAuthority.CIPHERTEXT, AlmostHomeAuthority.OLD_MOTD)
                        .equals("LABELCOMPLETIONINFERRED"),
                "M08 Vigenere note does not decode under the displayed old MOTD");
        try (Fixture fixture = Fixture.create("m08-locked-", false)) {
            Result locked = AlmostHomeAuthority.inspect(Progress.initial(), fixture.state.snapshot(),
                    EvidenceId.FINCH_PLAN_POST, id("locked"));
            check(locked.status() == Status.LOCKED && locked.progress().revision() == 0,
                    "M08 cannot start before M07 authenticates a witness anchor");
        }
    }

    private static void oneTwoSixPlayerChronologyAndNegativeEvidence() throws Exception {
        for (int players : new int[]{1, 2, 6}) {
            try (Fixture fixture = Fixture.create("m08-group-" + players + "-", true)) {
                Progress progress = observeAll(fixture, players);
                check(progress.observed().size() == 4, players + "-player M08 did not retain all source reads");
                Result wrong = AlmostHomeAuthority.order(progress, fixture.state.snapshot(),
                        EvidenceId.PERFECT_CURRENT_HOUSE, id("wrong-order-" + players));
                check(wrong.status() == Status.WRONG_ORDER && wrong.progress().chronology().isEmpty()
                                && wrong.feedback().contains("house") && wrong.feedback().contains("intact"),
                        players + "-player wrong chronology must reset safely without destroying evidence");
                progress = orderAll(wrong.progress(), fixture);
                check(progress.chronology().equals(AlmostHomeAuthority.CORRECT_CHRONOLOGY),
                        players + "-player chronology cannot order plan, screenshot, inventory, and current house");
                String inventory = EvidenceId.CONSTRUCTION_INVENTORY.receipt().finding();
                check(inventory.contains("lacks") && inventory.contains("required"),
                        "M08 construction inventory does not expose legible negative evidence");
            }
        }
    }

    private static void wrongDecodeAndProvenanceNeverDestroyTheHouse() throws Exception {
        try (Fixture fixture = Fixture.create("m08-wrong-", true)) {
            Progress ordered = orderAll(observeAll(fixture, 2), fixture);
            Result wrongNote = AlmostHomeAuthority.decode(ordered, fixture.state.snapshot(),
                    DecodeChoice.CERTIFY_PLAN_AS_HISTORY, id("wrong-note"));
            check(wrongNote.status() == Status.WRONG_NOTE && wrongNote.progress().equals(ordered),
                    "wrong M08 plaintext must not mutate chronology or evidence");
            Progress decoded = AlmostHomeAuthority.decode(ordered, fixture.state.snapshot(),
                    DecodeChoice.LABEL_COMPLETION_INFERRED, id("decode")).progress();
            Result authenticated = AlmostHomeAuthority.classify(decoded, fixture.state.snapshot(),
                    ProvenanceChoice.AUTHENTICATED, id("wrong-provenance"));
            Result unknown = AlmostHomeAuthority.classify(decoded, fixture.state.snapshot(),
                    ProvenanceChoice.UNKNOWN, id("unknown-provenance"));
            check(authenticated.status() == Status.WRONG_PROVENANCE
                            && unknown.status() == Status.WRONG_PROVENANCE
                            && authenticated.progress().equals(decoded) && unknown.progress().equals(decoded)
                            && !fixture.state.snapshot().committedEvents().contains(AlmostHomeAuthority.ALMOST_HOME_EVENT),
                    "wrong M08 labels must leave the beautiful false restoration intact and unfiled");
        }
    }

    private static void almostHomeAndBoundedContinuityCommitExactlyOnce() throws Exception {
        try (Fixture fixture = Fixture.create("m08-commit-", true)) {
            Progress decoded = AlmostHomeAuthority.decode(
                    orderAll(observeAll(fixture, 6), fixture), fixture.state.snapshot(),
                    DecodeChoice.LABEL_COMPLETION_INFERRED, id("decode")).progress();
            Result proven = AlmostHomeAuthority.classify(decoded, fixture.state.snapshot(),
                    ProvenanceChoice.INFERRED, id("provenance"));
            check(proven.status() == Status.READY_TO_COMMIT && proven.progress().proven()
                            && proven.eventKey().equals(AlmostHomeAuthority.ALMOST_HOME_EVENT),
                    "M08 inferred label must locally persist before journal commit");
            String almostPayload = text(AlmostHomeAuthority.payload(proven));
            for (String required : new String[]{"\"house_destroyed\":false", "\"provenance\":\"inferred\"",
                    AlmostHomeAuthority.CIPHERTEXT, "finch_never_built_complete_house"}) {
                check(almostPayload.contains(required), "M08 Almost Home payload lacks " + required);
            }
            var first = fixture.state.commit(proven.eventKey(), proven.idempotencyKey(), AlmostHomeAuthority.payload(proven));
            var duplicate = fixture.state.commit(proven.eventKey(), proven.idempotencyKey(), AlmostHomeAuthority.payload(proven));
            check(first.created() && !duplicate.created() && first.sequence() == duplicate.sequence(),
                    "M08 Almost Home event is not idempotent");

            Result decline = AlmostHomeAuthority.decideContinuity(proven.progress(), fixture.state.snapshot(),
                    ContinuityDecision.NOT_NOW, id("decline"));
            check(decline.status() == Status.DECLINED && !decline.commitsEvent()
                            && !fixture.state.snapshot().committedEvents().contains(AlmostHomeAuthority.CONTINUITY_EVENT),
                    "declining M08 Account Continuity must create no receipt and start nothing");
            Result authorize = AlmostHomeAuthority.decideContinuity(proven.progress(), fixture.state.snapshot(),
                    ContinuityDecision.AUTHORIZE_BOUNDED_TEST, id("authorize"));
            String consentPayload = text(AlmostHomeAuthority.payload(authorize));
            check(authorize.commitsEvent() && authorize.eventKey().equals(AlmostHomeAuthority.CONTINUITY_EVENT)
                            && consentPayload.contains("\"starts_continuation\":false")
                            && consentPayload.contains("bounded_disconnect_continuation_test_only"),
                    "M08 authorization must be bounded and must not start continuation immediately");
            fixture.state.commit(authorize.eventKey(), authorize.idempotencyKey(), AlmostHomeAuthority.payload(authorize));
            check(AlmostHomeAuthority.decideContinuity(proven.progress(), fixture.state.snapshot(),
                            ContinuityDecision.AUTHORIZE_BOUNDED_TEST, id("again")).status() == Status.DUPLICATE,
                    "M08 Account Continuity authorization must be idempotent");
        }
    }

    private static void partialAndProvenProgressSurviveRestart() throws Exception {
        Path directory = Files.createTempDirectory("m08-store-");
        Path path = directory.resolve("almost-home.progress");
        try (Fixture fixture = Fixture.create("m08-store-state-", true)) {
            AlmostHomeProgressStore store = new AlmostHomeProgressStore(path, RELEASE);
            Progress partial = AlmostHomeAuthority.inspect(Progress.initial(), fixture.state.snapshot(),
                    EvidenceId.DATED_SCREENSHOT, id("partial")).progress();
            store.save(partial);
            check(new AlmostHomeProgressStore(path, RELEASE).load().equals(partial),
                    "M08 partial observation does not survive restart");
            Progress decoded = AlmostHomeAuthority.decode(orderAll(observeAll(fixture, 3), fixture),
                    fixture.state.snapshot(), DecodeChoice.LABEL_COMPLETION_INFERRED, id("decode")).progress();
            Progress proven = AlmostHomeAuthority.classify(decoded, fixture.state.snapshot(),
                    ProvenanceChoice.INFERRED, id("prove")).progress();
            store.save(proven);
            Progress loaded = new AlmostHomeProgressStore(path, RELEASE).load();
            check(loaded.equals(proven) && AlmostHomeAuthority.recoverAlmostHome(
                            loaded, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT,
                    "M08 proven provenance cannot recover across a crash window");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(2, "revision=900");
            Files.write(path, lines, StandardCharsets.UTF_8);
            try { store.load(); throw new AssertionError("expected M08 corruption failure"); }
            catch (IOException expected) { /* expected */ }
        } finally {
            Files.deleteIfExists(path); Files.deleteIfExists(directory);
        }
    }

    private static void physicalManifestAndInstallerAreBounded() throws Exception {
        AlmostHomeManifest manifest = new AlmostHomeManifest();
        check(manifest.cells().size() == 5_225 && manifest.terminals().size() == 4
                        && manifest.chronologyLamps().size() == 4
                        && manifest.manifestSha256().matches("[0-9a-f]{64}")
                        && manifest.manifestSha256().equals(new AlmostHomeManifest().manifestSha256()),
                "M08 manifest must be one stable bounded house gallery");
        check(manifest.cells().values().stream().anyMatch("minecraft:glass"::equals)
                        && manifest.cells().values().stream().anyMatch("minecraft:spruce_planks"::equals)
                        && manifest.cells().values().stream().anyMatch("minecraft:waxed_copper_block"::equals)
                        && manifest.cells().values().stream().anyMatch("minecraft:cartography_table"::equals),
                "M08 perfect house does not physically combine Finch's four planned features");
        Path directory = Files.createTempDirectory("m08-installer-");
        Path receipt = directory.resolve("m08.receipt");
        AlmostHomeInstaller.Origin origin = new AlmostHomeInstaller.Origin(128, 80, 0);
        try {
            FakeWorld world = new FakeWorld();
            AlmostHomeInstaller installer = new AlmostHomeInstaller(manifest, receipt);
            AlmostHomeInstaller.Result built = installer.install(RELEASE, origin, world);
            check(built.status() == AlmostHomeInstaller.Status.BUILT && built.blockCount() == 5_225,
                    "M08 empty-target installer must audit every gallery cell");
            check(installer.install(RELEASE, origin, world).status() == AlmostHomeInstaller.Status.ALREADY_PRESENT,
                    "M08 installer does not validate its release/world/origin receipt after restart");
            AlmostHomeManifest.Cell lamp = manifest.chronologyLamps().values().iterator().next();
            world.setBlockData(lamp, "minecraft:copper_bulb[lit=true,powered=false]");
            installer.audit(world, true);
            try { installer.audit(world, false); throw new AssertionError("expected strict M08 lamp audit failure"); }
            catch (IOException expected) { /* expected */ }
        } finally {
            Files.deleteIfExists(receipt); Files.deleteIfExists(directory);
        }
        Path foreignDirectory = Files.createTempDirectory("m08-foreign-");
        try {
            FakeWorld world = new FakeWorld();
            AlmostHomeManifest.Cell cell = manifest.cells().keySet().iterator().next();
            world.setBlockData(cell, "minecraft:diamond_block");
            try {
                new AlmostHomeInstaller(manifest, foreignDirectory.resolve("m08.receipt")).install(RELEASE, origin, world);
                throw new AssertionError("expected M08 occupied-target refusal");
            } catch (IOException expected) {
                check("minecraft:diamond_block".equals(world.blockData(cell)),
                        "M08 occupied-target refusal must preserve foreign blocks");
            }
        } finally {
            Files.deleteIfExists(foreignDirectory.resolve("m08.receipt")); Files.deleteIfExists(foreignDirectory);
        }
        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/almost/BukkitAlmostHome.java"))
                + Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/almost/BukkitAlmostHomeWorld.java"));
        for (String required : new String[]{"RIGHT_CLICK_BLOCK", "LEFT_CLICK_BLOCK",
                "PlayerCustomClickEvent", "DialogType.multiAction", "DialogType.confirmation",
                "canCloseWithEscape(true)", "SELECTABLE CIPHER", "getBlockData().matches(expected)"}) {
            check(source.contains(required), "M08 Paper adapter missing " + required);
        }
        for (String forbidden : new String[]{"PlayerMoveEvent", "runTaskAsynchronously",
                "net.minecraft", "craftbukkit", "sendBlockChange"}) {
            check(!source.contains(forbidden), "M08 Paper adapter crossed forbidden boundary via " + forbidden);
        }
    }

    private static Progress observeAll(Fixture fixture, int players) {
        Progress progress = Progress.initial();
        for (int index = 0; index < EvidenceId.values().length; index++) {
            Result result = AlmostHomeAuthority.inspect(progress, fixture.state.snapshot(),
                    EvidenceId.values()[index], id(players + "-" + (index % players)));
            check(result.status() == Status.OBSERVED && result.receipt() != null,
                    "M08 source " + index + " was not privately readable");
            progress = result.progress();
        }
        return progress;
    }
    private static Progress orderAll(Progress progress, Fixture fixture) {
        for (EvidenceId evidence : AlmostHomeAuthority.CORRECT_CHRONOLOGY) {
            Result result = AlmostHomeAuthority.order(progress, fixture.state.snapshot(), evidence, id("order-" + evidence));
            check(result.status() == Status.ORDERED, "M08 could not place " + evidence);
            progress = result.progress();
        }
        return progress;
    }
    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(("m08-" + value).getBytes(StandardCharsets.UTF_8));
    }
    private static String text(byte[] value) { return new String(value, StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static final class Fixture implements AutoCloseable {
        private final Path directory; private final MorrowLocalState state;
        private Fixture(Path directory, MorrowLocalState state) { this.directory = directory; this.state = state; }
        static Fixture create(String prefix, boolean ready) throws Exception {
            Path directory = Files.createTempDirectory(prefix);
            MorrowLocalState state = MorrowLocalState.open(directory.resolve("morrow.journal"), RELEASE);
            if (ready) seed(state);
            return new Fixture(directory, state);
        }
        private static void seed(MorrowLocalState state) throws Exception {
            String[] events = {"morrow.act0.case_chain_authenticated", "morrow.act0.server_handoff_recovered",
                    "morrow.act1.room04_witnessed", "morrow.act1.static_proposal_authenticated",
                    "morrow.act1.intention_error_proven", "morrow.act1.entity_replay_authorized",
                    "morrow.act2.missing_role_completed", "morrow.act2.live_test_recorded",
                    "morrow.act2.behavior_reuse_proven", "morrow.act2.private_contradiction_resolved",
                    "morrow.act2.live_capture_authorized", "morrow.act3.version_fragments_authenticated",
                    "morrow.act3.incomplete_consensus_proven", AlmostHomeAuthority.PREREQUISITE};
            for (int index = 0; index < events.length; index++) {
                String event = events[index];
                if (event.startsWith("morrow.act0") || event.equals("morrow.act2.private_contradiction_resolved"))
                    state.acceptProjection(event, "m08:external:" + index, bytes("{}"));
                else state.commit(event, "m08:minecraft:" + index, bytes("{}"));
            }
        }
        @Override public void close() throws IOException {
            Files.deleteIfExists(directory.resolve("morrow.journal")); Files.deleteIfExists(directory);
        }
    }
    private static final class FakeWorld implements AlmostHomeInstaller.WorldPort {
        private final Map<AlmostHomeManifest.Cell, String> blocks = new LinkedHashMap<>();
        @Override public String binding() { return "m08-test-world:00000000-0000-0000-0000-000000000008"; }
        @Override public String blockData(AlmostHomeManifest.Cell relative) {
            return blocks.getOrDefault(relative, "minecraft:air");
        }
        @Override public boolean isAir(AlmostHomeManifest.Cell relative) {
            return "minecraft:air".equals(blockData(relative));
        }
        @Override public void setBlockData(AlmostHomeManifest.Cell relative, String blockData) {
            blocks.put(relative, blockData);
        }
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
