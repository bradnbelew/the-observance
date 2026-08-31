package com.observance.watcher.morrow.witnessanchor;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.CellId;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.MaterialChoice;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.Progress;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.Result;
import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Main-driven M07 arbitrary-input, immutable version, mismatch, cohort, and restart matrix. */
public final class WitnessAnchorAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.m07";

    private WitnessAnchorAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        lockedIncompleteAndEditablePathsAreSafe();
        oneTwoSixPlayerAnchorsCommitExactlyOnce();
        wrongCellAndCommittedSourceRemainImmutable();
        draftCommitAndIdentifiedStateSurviveRestart();
        physicalManifestInstallerAndPaperAdapter();
        System.out.println("MORROW WITNESS ANCHOR M07: PASS cells=6 arbitrary=true mismatch=1 players=1/2/6");
    }

    private static void lockedIncompleteAndEditablePathsAreSafe() throws Exception {
        try (Fixture fixture = Fixture.create("m07-basic-", false)) {
            Result locked = WitnessAnchorAuthority.edit(Progress.initial(), fixture.state.snapshot(),
                    CellId.A1, MaterialChoice.COPPER_SQUARE, id("locked"));
            check(locked.status() == Status.LOCKED && locked.progress().revision() == 0,
                    "M07 cannot begin before M06 incomplete consensus");
        }
        try (Fixture fixture = Fixture.create("m07-edit-", true)) {
            Progress progress = WitnessAnchorAuthority.edit(Progress.initial(), fixture.state.snapshot(),
                    CellId.A1, MaterialChoice.COPPER_SQUARE, id("edit")).progress();
            Result incomplete = WitnessAnchorAuthority.commit(progress, fixture.state.snapshot(), id("edit"));
            check(incomplete.status() == Status.INCOMPLETE && incomplete.progress().equals(progress),
                    "M07 cannot hash a partial arrangement");
            Progress changed = WitnessAnchorAuthority.edit(progress, fixture.state.snapshot(),
                    CellId.A1, MaterialChoice.TARGET_RING, id("edit")).progress();
            Progress cleared = WitnessAnchorAuthority.edit(changed, fixture.state.snapshot(),
                    CellId.A1, null, id("edit")).progress();
            check(changed.draft().get(CellId.A1) == MaterialChoice.TARGET_RING
                            && !cleared.draft().containsKey(CellId.A1),
                    "uncommitted M07 cells can be changed and cleared freely");
        }
    }

    private static void oneTwoSixPlayerAnchorsCommitExactlyOnce() throws Exception {
        for (int count : new int[]{1, 2, 6}) {
            try (Fixture fixture = Fixture.create("m07-group-" + count + "-", true)) {
                Progress draft = completeDraft(fixture, count, count);
                Result local = WitnessAnchorAuthority.commit(draft, fixture.state.snapshot(), id("commit-" + count));
                check(local.status() == Status.COMMITTED_LOCAL && !local.commitsEvent()
                                && local.progress().anchor() != null,
                        count + "-player M07 freezes a local version before any global event");
                var anchor = local.progress().anchor();
                long differences = Arrays.stream(CellId.values())
                        .filter(cell -> anchor.original().get(cell) != anchor.reconstruction().get(cell)).count();
                check(differences == 1 && anchor.originalSha256().matches("[0-9a-f]{64}")
                                && anchor.reconstructionSha256().matches("[0-9a-f]{64}")
                                && !anchor.originalSha256().equals(anchor.reconstructionSha256()),
                        count + "-player M07 creates exactly one deterministic reconstruction error");
                Result identified = WitnessAnchorAuthority.identify(local.progress(), fixture.state.snapshot(),
                        anchor.mismatch(), id("identify-" + count));
                check(identified.status() == Status.READY_TO_COMMIT && identified.commitsEvent()
                                && identified.progress().identified(),
                        count + "-player M07 earns the event only by identifying the mismatch");
                byte[] payload = WitnessAnchorAuthority.payload(identified);
                MorrowLocalState.CommitResult first = fixture.state.commit(
                        identified.eventKey(), identified.idempotencyKey(), payload);
                MorrowLocalState.CommitResult duplicate = fixture.state.commit(
                        identified.eventKey(), identified.idempotencyKey(), payload);
                check(first.created() && !duplicate.created() && first.sequence() == duplicate.sequence(),
                        count + "-player M07 event commit is idempotent");
                String text = new String(payload, StandardCharsets.UTF_8);
                for (String required : new String[]{"\"anchor_version\":1", "\"source_overwritten\":false",
                        anchor.originalSha256(), anchor.reconstructionSha256(), anchor.mismatch().label()}) {
                    check(text.contains(required), count + "-player M07 payload lacks " + required);
                }
            }
        }
        try (Fixture fixture = Fixture.create("m07-distinct-", true)) {
            String one = WitnessAnchorAuthority.commit(completeDraft(fixture, 1, 1),
                    fixture.state.snapshot(), id("one")).progress().anchor().originalSha256();
            String two = WitnessAnchorAuthority.commit(completeDraft(fixture, 1, 2),
                    fixture.state.snapshot(), id("two")).progress().anchor().originalSha256();
            check(!one.equals(two), "different arbitrary M07 choices produce different commitments");
        }
    }

    private static void wrongCellAndCommittedSourceRemainImmutable() throws Exception {
        try (Fixture fixture = Fixture.create("m07-wrong-", true)) {
            Progress draft = completeDraft(fixture, 2, 3);
            Progress committed = WitnessAnchorAuthority.commit(
                    draft, fixture.state.snapshot(), id("commit")).progress();
            CellId mismatch = committed.anchor().mismatch();
            CellId wrong = Arrays.stream(CellId.values()).filter(cell -> cell != mismatch).findFirst().orElseThrow();
            Result wrongResult = WitnessAnchorAuthority.identify(
                    committed, fixture.state.snapshot(), wrong, id("wrong"));
            check(wrongResult.status() == Status.WRONG_CELL && wrongResult.progress().equals(committed)
                            && !wrongResult.commitsEvent(),
                    "wrong M07 mismatch selection explains the comparison without mutation");
            Result overwrite = WitnessAnchorAuthority.edit(committed, fixture.state.snapshot(),
                    CellId.A1, MaterialChoice.HONEYCOMB, id("overwrite"));
            check(overwrite.status() == Status.IMMUTABLE && overwrite.progress().equals(committed),
                    "committed M07 source cannot be overwritten by later edits");
            check(!fixture.state.snapshot().committedEvents().contains(WitnessAnchorAuthority.EVENT),
                    "wrong M07 interaction fabricates no event");
        }
    }

    private static void draftCommitAndIdentifiedStateSurviveRestart() throws Exception {
        Path directory = Files.createTempDirectory("m07-store-");
        Path path = directory.resolve("witness.progress");
        try (Fixture fixture = Fixture.create("m07-store-state-", true)) {
            WitnessAnchorProgressStore store = new WitnessAnchorProgressStore(path, RELEASE);
            Progress partial = WitnessAnchorAuthority.edit(Progress.initial(), fixture.state.snapshot(),
                    CellId.B2, MaterialChoice.CARVED_SHELF, id("partial")).progress();
            store.save(partial);
            check(new WitnessAnchorProgressStore(path, RELEASE).load().equals(partial),
                    "M07 uncommitted draft survives restart exactly");
            Progress committed = WitnessAnchorAuthority.commit(completeDraft(fixture, 3, 4),
                    fixture.state.snapshot(), id("commit")).progress();
            Progress identified = WitnessAnchorAuthority.identify(committed, fixture.state.snapshot(),
                    committed.anchor().mismatch(), id("identify")).progress();
            store.save(identified);
            Progress loaded = new WitnessAnchorProgressStore(path, RELEASE).load();
            check(loaded.equals(identified) && WitnessAnchorAuthority.recover(
                            loaded, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT,
                    "M07 committed anchor and identified mismatch recover without replay");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(2, "revision=99");
            Files.write(path, lines, StandardCharsets.UTF_8);
            try {
                store.load();
                throw new AssertionError("expected M07 corruption failure");
            } catch (IOException expected) {
                // expected
            }
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    private static void physicalManifestInstallerAndPaperAdapter() throws Exception {
        WitnessAnchorManifest manifest = new WitnessAnchorManifest();
        check(manifest.cells().size() == 1_547
                        && manifest.originalBases().size() == 6
                        && manifest.originalCells().size() == 6
                        && manifest.reconstructionCells().size() == 6
                        && manifest.manifestSha256().matches("[0-9a-f]{64}")
                        && manifest.manifestSha256().equals(new WitnessAnchorManifest().manifestSha256()),
                "M07 manifest is one stable bounded chamber with two six-cell grids");
        Path directory = Files.createTempDirectory("m07-installer-");
        Path receipt = directory.resolve("m07.receipt");
        WitnessAnchorInstaller.Origin origin = new WitnessAnchorInstaller.Origin(96, 80, 0);
        try {
            FakeWorld world = new FakeWorld();
            WitnessAnchorInstaller installer = new WitnessAnchorInstaller(manifest, receipt);
            WitnessAnchorInstaller.Result built = installer.install(RELEASE, origin, world);
            check(built.status() == WitnessAnchorInstaller.Status.BUILT && built.blockCount() == 1_547,
                    "M07 empty-target installer builds and audits every chamber cell");
            check(installer.install(RELEASE, origin, world).status()
                            == WitnessAnchorInstaller.Status.ALREADY_PRESENT,
                    "M07 installer validates its release/world/origin receipt after restart");
            WitnessAnchorManifest.Cell original = manifest.originalCells().get(CellId.A1);
            WitnessAnchorManifest.Cell reconstruction = manifest.reconstructionCells().get(CellId.B3);
            world.setBlockData(original, MaterialChoice.CARVED_SHELF.minecraftKey());
            world.setBlockData(reconstruction, MaterialChoice.WEATHERED_CUT.minecraftKey());
            world.setBlockData(manifest.commitLamp(), "minecraft:copper_bulb[lit=true,powered=false]");
            installer.audit(world, true);
            try {
                installer.audit(world, false);
                throw new AssertionError("expected strict M07 dynamic-cell audit failure");
            } catch (IOException expected) {
                // expected
            }
        } finally {
            Files.deleteIfExists(receipt);
            Files.deleteIfExists(directory);
        }

        Path foreignDirectory = Files.createTempDirectory("m07-foreign-");
        try {
            FakeWorld foreign = new FakeWorld();
            WitnessAnchorManifest.Cell cell = manifest.cells().keySet().iterator().next();
            foreign.setBlockData(cell, "minecraft:diamond_block");
            try {
                new WitnessAnchorInstaller(manifest, foreignDirectory.resolve("m07.receipt"))
                        .install(RELEASE, origin, foreign);
                throw new AssertionError("expected M07 occupied-target refusal");
            } catch (IOException expected) {
                check("minecraft:diamond_block".equals(foreign.blockData(cell)),
                        "M07 occupied-target refusal preserves foreign blocks");
            }
        } finally {
            Files.deleteIfExists(foreignDirectory.resolve("m07.receipt"));
            Files.deleteIfExists(foreignDirectory);
        }

        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/witnessanchor/BukkitWitnessAnchor.java"))
                + Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/witnessanchor/BukkitWitnessAnchorWorld.java"));
        for (String required : new String[]{"RIGHT_CLICK_BLOCK", "PlayerCustomClickEvent",
                "DialogType.multiAction", "canCloseWithEscape(true)", "COMMITTED ORIGINAL",
                "MORROW RECONSTRUCTION", "getBlockData().matches(expected)"}) {
            check(source.contains(required), "M07 Paper adapter missing " + required);
        }
        for (String forbidden : new String[]{"PlayerMoveEvent", "runTaskAsynchronously",
                "net.minecraft", "craftbukkit", "sendBlockChange", "waterlogged"}) {
            check(!source.contains(forbidden), "M07 Paper adapter crossed forbidden boundary via " + forbidden);
        }
    }

    private static Progress completeDraft(Fixture fixture, int players, int offset) {
        Progress progress = Progress.initial();
        MaterialChoice[] choices = MaterialChoice.values();
        for (int index = 0; index < CellId.values().length; index++) {
            Result result = WitnessAnchorAuthority.edit(progress, fixture.state.snapshot(),
                    CellId.values()[index], choices[(index + offset) % choices.length],
                    id(players + "-" + (index % players)));
            check(result.status() == Status.EDITED, "M07 could not edit cell " + index);
            progress = result.progress();
        }
        return progress;
    }

    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(("m07-" + value).getBytes(StandardCharsets.UTF_8));
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
        static Fixture create(String prefix, boolean ready) throws Exception {
            Path directory = Files.createTempDirectory(prefix);
            MorrowLocalState state = MorrowLocalState.open(directory.resolve("morrow.journal"), RELEASE);
            if (ready) seed(state);
            return new Fixture(directory, state);
        }
        private static void seed(MorrowLocalState state) throws Exception {
            String[] events = {
                    "morrow.act0.case_chain_authenticated", "morrow.act0.server_handoff_recovered",
                    "morrow.act1.room04_witnessed", "morrow.act1.static_proposal_authenticated",
                    "morrow.act1.intention_error_proven", "morrow.act1.entity_replay_authorized",
                    "morrow.act2.missing_role_completed", "morrow.act2.live_test_recorded",
                    "morrow.act2.behavior_reuse_proven", "morrow.act2.private_contradiction_resolved",
                    "morrow.act2.live_capture_authorized", "morrow.act3.version_fragments_authenticated",
                    WitnessAnchorAuthority.PREREQUISITE,
            };
            for (int index = 0; index < events.length; index++) {
                String event = events[index];
                if (event.startsWith("morrow.act0") || event.equals("morrow.act2.private_contradiction_resolved")) {
                    state.acceptProjection(event, "m07:external:" + index, bytes("{}"));
                } else {
                    state.commit(event, "m07:minecraft:" + index, bytes("{}"));
                }
            }
        }
        @Override public void close() throws IOException {
            Files.deleteIfExists(directory.resolve("morrow.journal"));
            Files.deleteIfExists(directory);
        }
    }
    private static final class FakeWorld implements WitnessAnchorInstaller.WorldPort {
        private final Map<WitnessAnchorManifest.Cell, String> blocks = new LinkedHashMap<>();
        @Override public String binding() { return "m07-test-world:00000000-0000-0000-0000-000000000007"; }
        @Override public String blockData(WitnessAnchorManifest.Cell relative) {
            return blocks.getOrDefault(relative, "minecraft:air");
        }
        @Override public boolean isAir(WitnessAnchorManifest.Cell relative) {
            return "minecraft:air".equals(blockData(relative));
        }
        @Override public void setBlockData(WitnessAnchorManifest.Cell relative, String blockData) {
            blocks.put(relative, blockData);
        }
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
