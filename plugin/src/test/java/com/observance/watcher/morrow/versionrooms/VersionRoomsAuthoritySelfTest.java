package com.observance.watcher.morrow.versionrooms;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.Choice;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.Progress;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.Result;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.RoomVersion;
import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Main-driven M05 route, contradiction, cohort, restart, and physical-manifest receipt. */
public final class VersionRoomsAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.m05";

    private VersionRoomsAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        manifestIsExactAndAsymmetric();
        prerequisiteAndWrongRoutesFailSafely();
        oneTwoSixPlayerRoutesCommitExactlyOnce();
        canonicalChoicesDestroyEvidenceAndNeverProgress();
        partialRouteSurvivesRestartAndDetectsCorruption();
        installerRecoversRefusesForeignAndAuditsLamps();
        paperAdapterUsesNativeBoundedInputs();
        System.out.println("MORROW VERSION ROOMS M05: PASS rooms=3 route=damaged/scaffold/completed players=1/2/6");
    }

    private static void manifestIsExactAndAsymmetric() {
        VersionRoomsManifest manifest = new VersionRoomsManifest();
        check(manifest.cells().size() == 1617, "M05 manifest has three complete bounded room volumes");
        check(manifest.manifestSha256().matches("[0-9a-f]{64}"), "M05 manifest is content-addressed");
        check(manifest.consoles().size() == 3 && manifest.lamps().size() == 3,
                "every M05 room has one console and signal lamp");
        long damaged = manifest.cells().values().stream()
                .filter("minecraft:cracked_deepslate_bricks"::equals).count();
        long completed = manifest.cells().values().stream()
                .filter("minecraft:waxed_cut_copper"::equals).count();
        long scaffold = manifest.cells().values().stream()
                .filter("minecraft:scaffolding"::equals).count();
        check(damaged > 0 && completed > 0 && scaffold > 0,
                "the three versions are physically distinguishable without a resource pack");
        for (RoomVersion version : RoomVersion.values()) {
            check(version.receiptId().matches("M05-[A-Z]{3}-[0-9A-F]{4}")
                            && version.inventory().startsWith("Inventory:")
                            && version.uniqueTruth().startsWith("Unique truth:"),
                    version + " exposes a readable custody receipt and text inventory");
        }
    }

    private static void prerequisiteAndWrongRoutesFailSafely() throws Exception {
        try (Fixture fixture = Fixture.create("m05-locked-", false)) {
            UUID player = id("locked");
            Result locked = VersionRoomsAuthority.route(
                    Progress.initial(), fixture.state.snapshot(), RoomVersion.DAMAGED, player);
            check(locked.status() == Status.LOCKED && locked.progress().revision() == 0,
                    "M05 cannot start before explicit live-capture authorization");
        }
        try (Fixture fixture = Fixture.create("m05-reset-", true)) {
            UUID player = id("reset");
            Result first = VersionRoomsAuthority.route(
                    Progress.initial(), fixture.state.snapshot(), RoomVersion.DAMAGED, player);
            Result wrong = VersionRoomsAuthority.route(
                    first.progress(), fixture.state.snapshot(), RoomVersion.COMPLETED, player);
            check(wrong.status() == Status.WRONG_RESET && wrong.progress().route().isEmpty()
                            && wrong.progress().witnesses().containsKey(RoomVersion.DAMAGED)
                            && wrong.feedback().contains("Only the three signal lamps reset"),
                    "wrong M05 routing resets only the signal and preserves observed sources");
            check(!fixture.state.snapshot().committedEvents().contains(
                    VersionRoomsAuthority.FRAGMENTS_AUTHENTICATED),
                    "wrong M05 routing fabricates no event");
        }
    }

    private static void oneTwoSixPlayerRoutesCommitExactlyOnce() throws Exception {
        for (int count : new int[]{1, 2, 6}) {
            try (Fixture fixture = Fixture.create("m05-group-" + count + "-", true)) {
                List<UUID> players = java.util.stream.IntStream.range(0, count)
                        .mapToObj(index -> id(count + "-" + index)).toList();
                Progress progress = Progress.initial();
                Result ready = null;
                for (int index = 0; index < VersionRoomsAuthority.SIGNAL_ROUTE.size(); index++) {
                    ready = VersionRoomsAuthority.route(progress, fixture.state.snapshot(),
                            VersionRoomsAuthority.SIGNAL_ROUTE.get(index), players.get(index % count));
                    progress = ready.progress();
                }
                check(ready != null && ready.status() == Status.READY_TO_COMMIT
                                && progress.witnesses().size() == 3,
                        count + "-player M05 route produces three private custody bindings");
                MorrowLocalState.CommitResult first = fixture.state.commit(
                        ready.eventKey(), ready.idempotencyKey(), VersionRoomsAuthority.payload(ready));
                MorrowLocalState.CommitResult duplicate = fixture.state.commit(
                        ready.eventKey(), ready.idempotencyKey(), VersionRoomsAuthority.payload(ready));
                check(first.created() && !duplicate.created() && first.sequence() == duplicate.sequence(),
                        count + "-player M05 route is group-idempotent");
                String payload = new String(fixture.state.latestPayload(
                        VersionRoomsAuthority.FRAGMENTS_AUTHENTICATED).orElseThrow(), StandardCharsets.UTF_8);
                for (String exact : new String[]{
                        "\"route\":[\"damaged\",\"scaffold\",\"completed\"]",
                        "M05-DMG-41C7", "M05-CMP-7A22", "M05-SCF-B903",
                        "\"world_mutation\":\"signal_lamps_only\""}) {
                    check(payload.contains(exact), count + "-player M05 payload lacks " + exact);
                }
                Result preserve = VersionRoomsAuthority.choose(
                        progress, fixture.state.snapshot(), Choice.PRESERVE_CONTRADICTION, players.get(0));
                check(preserve.status() == Status.PRESERVED && !preserve.commitsReceipt()
                                && preserve.progress().preserved(),
                        count + "-player M05 can preserve only after all fragments authenticate");
                check(!fixture.state.snapshot().committedEvents().contains(
                        "morrow.act3.contradiction_preserved"),
                        count + "-player M05 cannot skip the M06 consensus audit");
            }
        }
    }

    private static void canonicalChoicesDestroyEvidenceAndNeverProgress() throws Exception {
        try (Fixture fixture = Fixture.create("m05-choices-", true)) {
            UUID player = id("choice");
            Progress progress = Progress.initial();
            Result ready = null;
            for (RoomVersion version : VersionRoomsAuthority.SIGNAL_ROUTE) {
                ready = VersionRoomsAuthority.route(progress, fixture.state.snapshot(), version, player);
                progress = ready.progress();
            }
            fixture.state.commit(ready.eventKey(), ready.idempotencyKey(), VersionRoomsAuthority.payload(ready));
            for (Choice choice : List.of(
                    Choice.CERTIFY_DAMAGED, Choice.CERTIFY_COMPLETED, Choice.CERTIFY_SCAFFOLD)) {
                Result wrong = VersionRoomsAuthority.choose(progress, fixture.state.snapshot(), choice, player);
                check(wrong.status() == Status.WRONG_CHOICE && !wrong.commitsReceipt()
                                && wrong.feedback().contains("destroy"),
                        choice + " explains the evidence it would destroy and creates no receipt");
            }
            check(!progress.preserved(),
                    "canonical-version mistakes cannot advance M05");
        }
    }

    private static void partialRouteSurvivesRestartAndDetectsCorruption() throws Exception {
        Path directory = Files.createTempDirectory("m05-store-");
        Path path = directory.resolve("version-rooms.progress");
        try (Fixture fixture = Fixture.create("m05-store-state-", true)) {
            VersionRoomsProgressStore store = new VersionRoomsProgressStore(path, RELEASE);
            Progress partial = VersionRoomsAuthority.route(
                    Progress.initial(), fixture.state.snapshot(), RoomVersion.DAMAGED, id("store")).progress();
            store.save(partial);
            check(new VersionRoomsProgressStore(path, RELEASE).load().equals(partial),
                    "M05 partial route and custody survive restart exactly");
            Result scaffold = VersionRoomsAuthority.route(partial, fixture.state.snapshot(),
                    RoomVersion.SCAFFOLD, id("store"));
            Result ready = VersionRoomsAuthority.route(scaffold.progress(), fixture.state.snapshot(),
                    RoomVersion.COMPLETED, id("store"));
            fixture.state.commit(ready.eventKey(), ready.idempotencyKey(), VersionRoomsAuthority.payload(ready));
            Progress preserved = VersionRoomsAuthority.choose(ready.progress(), fixture.state.snapshot(),
                    Choice.PRESERVE_CONTRADICTION, id("store")).progress();
            store.save(preserved);
            check(new VersionRoomsProgressStore(path, RELEASE).load().equals(preserved)
                            && preserved.preserved(),
                    "M05 source-preservation decision survives restart without fabricating the M06 event");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(2, "revision=99");
            Files.write(path, lines, StandardCharsets.UTF_8);
            expectIo(store::load);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(directory);
        }
    }

    private static void installerRecoversRefusesForeignAndAuditsLamps() throws Exception {
        VersionRoomsManifest manifest = new VersionRoomsManifest();
        VersionRoomsInstaller.Origin origin = new VersionRoomsInstaller.Origin(32, 80, 0);
        Path directory = Files.createTempDirectory("m05-installer-");
        Path receipt = directory.resolve("m05.receipt");
        try {
            FakeWorld world = new FakeWorld();
            VersionRoomsInstaller installer = new VersionRoomsInstaller(manifest, receipt);
            VersionRoomsInstaller.Result built = installer.install(RELEASE, origin, world);
            check(built.status() == VersionRoomsInstaller.Status.BUILT
                            && built.blockCount() == manifest.cells().size(),
                    "M05 empty-target installer writes and audits the exact manifest");
            check(installer.install(RELEASE, origin, world).status()
                            == VersionRoomsInstaller.Status.ALREADY_PRESENT,
                    "M05 installer restart validates its release/world/origin receipt");
            VersionRoomsManifest.Cell lamp = manifest.lamps().get(RoomVersion.DAMAGED);
            world.setBlockData(lamp, "minecraft:copper_bulb[lit=true,powered=false]");
            installer.audit(world, true);
            try {
                installer.audit(world, false);
                throw new AssertionError("expected strict M05 lamp audit failure");
            } catch (IOException expected) {
                // expected
            }
        } finally {
            Files.deleteIfExists(receipt);
            Files.deleteIfExists(directory);
        }

        Path recoveredDirectory = Files.createTempDirectory("m05-recovered-");
        Path recoveredReceipt = recoveredDirectory.resolve("m05.receipt");
        try {
            FakeWorld recoveredWorld = new FakeWorld();
            Map.Entry<VersionRoomsManifest.Cell, String> first = manifest.cells().entrySet().stream()
                    .filter(entry -> !"minecraft:air".equals(entry.getValue())).findFirst().orElseThrow();
            recoveredWorld.setBlockData(first.getKey(), first.getValue());
            VersionRoomsInstaller.Result result = new VersionRoomsInstaller(manifest, recoveredReceipt)
                    .install(RELEASE, origin, recoveredWorld);
            check(result.status() == VersionRoomsInstaller.Status.RECOVERED_AND_BUILT,
                    "M05 safely completes an interrupted matching partial install");
        } finally {
            Files.deleteIfExists(recoveredReceipt);
            Files.deleteIfExists(recoveredDirectory);
        }

        Path foreignDirectory = Files.createTempDirectory("m05-foreign-");
        try {
            FakeWorld foreignWorld = new FakeWorld();
            VersionRoomsManifest.Cell cell = manifest.cells().keySet().iterator().next();
            foreignWorld.setBlockData(cell, "minecraft:diamond_block");
            try {
                new VersionRoomsInstaller(manifest, foreignDirectory.resolve("m05.receipt"))
                        .install(RELEASE, origin, foreignWorld);
                throw new AssertionError("expected occupied M05 target refusal");
            } catch (IOException expected) {
                check("minecraft:diamond_block".equals(foreignWorld.blockData(cell)),
                        "M05 occupied-target refusal preserves foreign blocks");
            }
        } finally {
            Files.deleteIfExists(foreignDirectory.resolve("m05.receipt"));
            Files.deleteIfExists(foreignDirectory);
        }
    }

    private static void paperAdapterUsesNativeBoundedInputs() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/versionrooms/BukkitVersionRooms.java"))
                + Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/versionrooms/BukkitVersionRoomsWorld.java"));
        String manifestSource = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/versionrooms/VersionRoomsManifest.java"));
        for (String required : new String[]{
                "PlayerInteractEvent", "RIGHT_CLICK_BLOCK", "PlayerCustomClickEvent", "DialogType.multiAction",
                "canCloseWithEscape(true)", "PRIVATE CUSTODY", "syncLamps", "PersistentDataType",
                "getBlockData().matches(expected)"}) {
            check(source.contains(required), "M05 Paper adapter missing " + required);
        }
        for (String forbidden : new String[]{
                "PlayerMoveEvent", "runTaskAsynchronously", "net.minecraft", "craftbukkit", "sendBlockChange",
                "waterlogged"}) {
            check(!source.contains(forbidden), "M05 Paper adapter crossed forbidden boundary via " + forbidden);
        }
        check(!manifestSource.contains("waterlogged"),
                "M05 manifest uses only properties accepted by the 1.21.11 copper bulb parser");
    }

    private static UUID id(String value) {
        return UUID.nameUUIDFromBytes(("m05-" + value).getBytes(StandardCharsets.UTF_8));
    }

    private static void expectIo(Throwing action) throws Exception {
        try {
            action.run();
            throw new AssertionError("expected IOException");
        } catch (IOException expected) {
            // expected
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface Throwing { void run() throws Exception; }

    private static final class Fixture implements AutoCloseable {
        private final Path directory;
        private final MorrowLocalState state;

        private Fixture(Path directory, MorrowLocalState state) {
            this.directory = directory;
            this.state = state;
        }

        static Fixture create(String prefix, boolean withPrerequisite) throws Exception {
            Path directory = Files.createTempDirectory(prefix);
            MorrowLocalState state = MorrowLocalState.open(directory.resolve("morrow.journal"), RELEASE);
            if (withPrerequisite) seedThroughPrerequisite(state);
            return new Fixture(directory, state);
        }

        private static void seedThroughPrerequisite(MorrowLocalState state) throws Exception {
            String[] events = {
                    "morrow.act0.case_chain_authenticated", "morrow.act0.server_handoff_recovered",
                    "morrow.act1.room04_witnessed", "morrow.act1.static_proposal_authenticated",
                    "morrow.act1.intention_error_proven", "morrow.act1.entity_replay_authorized",
                    "morrow.act2.missing_role_completed", "morrow.act2.live_test_recorded",
                    "morrow.act2.behavior_reuse_proven", "morrow.act2.private_contradiction_resolved",
                    VersionRoomsAuthority.PREREQUISITE,
            };
            for (int index = 0; index < events.length; index++) {
                String event = events[index];
                if (event.startsWith("morrow.act0")
                        || event.equals("morrow.act2.private_contradiction_resolved")) {
                    state.acceptProjection(event, "m05:external:" + index, bytes("{}"));
                } else {
                    state.commit(event, "m05:minecraft:" + index, bytes("{}"));
                }
            }
        }

        @Override public void close() throws IOException {
            Files.deleteIfExists(directory.resolve("morrow.journal"));
            Files.deleteIfExists(directory);
        }
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    private static final class FakeWorld implements VersionRoomsInstaller.WorldPort {
        private final Map<VersionRoomsManifest.Cell, String> blocks = new LinkedHashMap<>();
        @Override public String binding() { return "m05-test-world:00000000-0000-0000-0000-000000000005"; }
        @Override public String blockData(VersionRoomsManifest.Cell relative) {
            return blocks.getOrDefault(relative, "minecraft:air");
        }
        @Override public boolean isAir(VersionRoomsManifest.Cell relative) {
            return "minecraft:air".equals(blockData(relative));
        }
        @Override public void setBlockData(VersionRoomsManifest.Cell relative, String blockData) {
            if ("minecraft:air".equals(blockData)) blocks.remove(relative);
            else blocks.put(relative, blockData);
        }
    }
}
