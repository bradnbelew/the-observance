package com.observance.watcher.morrow.coldstorage;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Instance;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Progress;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Result;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Main-driven M11 custody-chain, two-instance bridge, consent, cohort, and restart matrix. */
public final class ColdStorageAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.m11";
    private ColdStorageAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        physicalManifestAndInstallerAreBounded();
        prerequisiteAndBrokenEdgesFailClosed();
        oneTwoSixPlayerCohortsProveRecovery();
        wrongHashesAndAccessConsentRemainSafe();
        everyReceiptWindowSurvivesRestart();
        System.out.println("MORROW COLD STORAGE M11: PASS records=5 bridge=2 players=1/2/6 continuity=unresolved");
    }

    private static void prerequisiteAndBrokenEdgesFailClosed() throws Exception {
        try (Fixture fixture = Fixture.create("m11-locked-", false)) {
            Result locked = ColdStorageAuthority.append(Progress.initial(), fixture.state.snapshot(), id("locked"),
                    ColdStorageAuthority.Record.THEO_PERMISSION);
            check(locked.status() == Status.LOCKED && locked.progress().revision() == 0,
                    "M11 opened before Copperline chronology projection");
        }
        try (Fixture fixture = Fixture.create("m11-edge-", true)) {
            Result broken = ColdStorageAuthority.append(Progress.initial(), fixture.state.snapshot(), id("edge"),
                    ColdStorageAuthority.Record.IONA_SHUTDOWN);
            check(broken.status() == Status.BROKEN_CUSTODY_EDGE && broken.progress().chain().isEmpty()
                            && broken.feedback().contains("edge 0") && broken.feedback().contains("Theo Vale"),
                    "M11 wrong chronology did not identify the first broken custody edge");
        }
    }

    private static void oneTwoSixPlayerCohortsProveRecovery() throws Exception {
        for (int players : new int[]{1, 2, 6}) try (Fixture fixture = Fixture.create("m11-group-" + players + "-", true)) {
            Progress progress = chain(fixture, players);
            UUID auditCourier = id(players + "-0"); UUID currentCourier = id(players + "-" + (players == 1 ? 0 : 1));
            progress = ColdStorageAuthority.deliver(progress, fixture.state.snapshot(), auditCourier,
                    Instance.AUDIT_SNAPSHOT, ColdStorageAuthority.CURRENT_RECOVERY_HASH).progress();
            progress = ColdStorageAuthority.deliver(progress, fixture.state.snapshot(), currentCourier,
                    Instance.CURRENT_RECOVERY, ColdStorageAuthority.AUDIT_SNAPSHOT_HASH).progress();
            Result proof = ColdStorageAuthority.proveReconstruction(progress, fixture.state.snapshot(), id(players + "-observer"));
            check(proof.status() == Status.READY_TO_COMMIT_RECONSTRUCTION && proof.progress().reconstructionProven(),
                    players + "-player M11 cohort could not prove reconstruction");
            String payload = text(ColdStorageAuthority.payload(proof));
            check(payload.contains("\"current_process\":\"reconstructed\"")
                            && payload.contains("\"original_process\":\"closed\"")
                            && payload.contains("\"continuity_claim\":null")
                            && payload.contains(ColdStorageAuthority.chainHash(ColdStorageAuthority.CANONICAL_CHAIN)),
                    players + "-player M11 payload lost provenance or overclaimed continuity");
            var first = fixture.state.commit(proof.eventKey(), proof.idempotencyKey(), ColdStorageAuthority.payload(proof));
            var duplicate = fixture.state.commit(proof.eventKey(), proof.idempotencyKey(), ColdStorageAuthority.payload(proof));
            check(first.created() && !duplicate.created() && first.sequence() == duplicate.sequence(),
                    players + "-player M11 reconstruction event is not idempotent");
            Result access = ColdStorageAuthority.authorizeAccess(proof.progress(), fixture.state.snapshot(), id(players + "-access"), true);
            check(access.status() == Status.READY_TO_COMMIT_ACCESS && access.progress().accessAuthorized()
                            && text(ColdStorageAuthority.payload(access)).contains("\"starts_access\":false"),
                    players + "-player M11 access was not separately and safely authorized");
        }
    }

    private static void wrongHashesAndAccessConsentRemainSafe() throws Exception {
        try (Fixture fixture = Fixture.create("m11-safe-", true)) {
            Progress progress = chain(fixture, 2);
            Result wrong = ColdStorageAuthority.deliver(progress, fixture.state.snapshot(), id("wrong"),
                    Instance.AUDIT_SNAPSHOT, "0".repeat(64));
            check(wrong.status() == Status.WRONG_SNAPSHOT_HASH && !wrong.progress().auditReceivedCurrent(),
                    "M11 wrong peer hash advanced an instance");
            Result early = ColdStorageAuthority.authorizeAccess(progress, fixture.state.snapshot(), id("early"), true);
            check(early.status() == Status.LOCKED && !early.progress().accessAuthorized(),
                    "M11 access opened before reconstruction was filed");
            progress = ColdStorageAuthority.deliver(progress, fixture.state.snapshot(), id("audit"),
                    Instance.AUDIT_SNAPSHOT, ColdStorageAuthority.CURRENT_RECOVERY_HASH).progress();
            progress = ColdStorageAuthority.deliver(progress, fixture.state.snapshot(), id("current"),
                    Instance.CURRENT_RECOVERY, ColdStorageAuthority.AUDIT_SNAPSHOT_HASH).progress();
            Result proof = ColdStorageAuthority.proveReconstruction(progress, fixture.state.snapshot(), id("proof"));
            fixture.state.commit(proof.eventKey(), proof.idempotencyKey(), ColdStorageAuthority.payload(proof));
            Result decline = ColdStorageAuthority.authorizeAccess(proof.progress(), fixture.state.snapshot(), id("decline"), false);
            check(decline.status() == Status.DECLINED && !decline.progress().accessAuthorized()
                            && fixture.state.snapshot().committedEvents().contains(ColdStorageAuthority.RECONSTRUCTION_EVENT),
                    "M11 access decline changed evidence or fabricated access");
        }
    }

    private static void everyReceiptWindowSurvivesRestart() throws Exception {
        Path directory = Files.createTempDirectory("m11-store-"); Path path = directory.resolve("cold-storage.progress");
        try (Fixture fixture = Fixture.create("m11-store-state-", true)) {
            ColdStorageProgressStore store = new ColdStorageProgressStore(path, RELEASE); Progress progress = Progress.initial();
            for (ColdStorageAuthority.Record record : ColdStorageAuthority.CANONICAL_CHAIN) {
                progress = ColdStorageAuthority.append(progress, fixture.state.snapshot(), id("record-" + record.name()), record).progress();
                progress = roundTrip(store, progress, record.name());
            }
            progress = roundTrip(store, ColdStorageAuthority.deliver(progress, fixture.state.snapshot(), id("audit"),
                    Instance.AUDIT_SNAPSHOT, ColdStorageAuthority.CURRENT_RECOVERY_HASH).progress(), "audit delivery");
            progress = roundTrip(store, ColdStorageAuthority.deliver(progress, fixture.state.snapshot(), id("current"),
                    Instance.CURRENT_RECOVERY, ColdStorageAuthority.AUDIT_SNAPSHOT_HASH).progress(), "current delivery");
            progress = roundTrip(store, ColdStorageAuthority.proveReconstruction(progress, fixture.state.snapshot(), id("proof")).progress(),
                    "local reconstruction receipt");
            check(ColdStorageAuthority.recoverReconstruction(progress, fixture.state.snapshot()).status()
                            == Status.READY_TO_COMMIT_RECONSTRUCTION,
                    "M11 reconstruction crash window cannot recover without replaying inputs");
            Result proof = ColdStorageAuthority.recoverReconstruction(progress, fixture.state.snapshot());
            fixture.state.commit(proof.eventKey(), proof.idempotencyKey(), ColdStorageAuthority.payload(proof));
            progress = roundTrip(store, ColdStorageAuthority.authorizeAccess(progress, fixture.state.snapshot(), id("access"), true).progress(),
                    "local access receipt");
            check(ColdStorageAuthority.recoverAccess(progress, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT_ACCESS,
                    "M11 access crash window cannot recover without replaying consent");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8); lines.set(2, "revision=999");
            Files.write(path, lines, StandardCharsets.UTF_8);
            try { store.load(); throw new AssertionError("expected M11 corruption failure"); }
            catch (IOException expected) { /* expected */ }
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(directory); }
    }

    private static void physicalManifestAndInstallerAreBounded() throws Exception {
        ColdStorageManifest manifest = new ColdStorageManifest();
        check(manifest.cells().size() == 6_417 && manifest.recordConsoles().size() == 5
                        && manifest.bridgeConsoles().size() == 2 && manifest.hashSources().size() == 2
                        && manifest.stateLamps().size() == 9 && manifest.manifestSha256().matches("[0-9a-f]{64}")
                        && manifest.manifestSha256().equals(new ColdStorageManifest().manifestSha256())
                        && "minecraft:air".equals(manifest.cells().get(manifest.safeReturn())),
                "M11 manifest must be a stable five-record archive with two instance endpoints and a safe return");
        Path directory = Files.createTempDirectory("m11-installer-"); Path receipt = directory.resolve("m11.receipt");
        ColdStorageInstaller.Origin origin = new ColdStorageInstaller.Origin(224, 80, 0);
        try {
            FakeWorld world = new FakeWorld(); ColdStorageInstaller installer = new ColdStorageInstaller(manifest, receipt);
            ColdStorageInstaller.Result built = installer.install(RELEASE, origin, world);
            check(built.status() == ColdStorageInstaller.Status.BUILT && built.blockCount() == 6_417,
                    "M11 empty-target installer must audit every archive cell");
            check(installer.install(RELEASE, origin, world).status() == ColdStorageInstaller.Status.ALREADY_PRESENT,
                    "M11 installer does not validate its release/world/origin receipt after restart");
            ColdStorageManifest.Cell lamp = manifest.stateLamps().values().iterator().next();
            world.setBlockData(lamp, "minecraft:copper_bulb[lit=true,powered=false]"); installer.audit(world, true);
            try { installer.audit(world, false); throw new AssertionError("expected strict M11 lamp audit failure"); }
            catch (IOException expected) { /* expected */ }
        } finally { Files.deleteIfExists(receipt); Files.deleteIfExists(directory); }
        Path foreignDirectory = Files.createTempDirectory("m11-foreign-");
        try {
            FakeWorld world = new FakeWorld(); ColdStorageManifest.Cell cell = manifest.cells().keySet().iterator().next();
            world.setBlockData(cell, "minecraft:diamond_block");
            try {
                new ColdStorageInstaller(manifest, foreignDirectory.resolve("m11.receipt")).install(RELEASE, origin, world);
                throw new AssertionError("expected M11 occupied-target refusal");
            } catch (IOException expected) {
                check("minecraft:diamond_block".equals(world.blockData(cell)),
                        "M11 occupied-target refusal must preserve foreign blocks");
            }
        } finally { Files.deleteIfExists(foreignDirectory.resolve("m11.receipt")); Files.deleteIfExists(foreignDirectory); }
        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/coldstorage/BukkitColdStorage.java"))
                + Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/coldstorage/BukkitColdStorageWorld.java"));
        for (String required : new String[]{"PlayerCustomClickEvent", "DialogType.multiAction",
                "canCloseWithEscape(true)", "Material.PAPER", "PersistentDataType.STRING",
                "Failed deliveries remain unconsumed", "IDENTITY: UNRESOLVED", "getBlockData().matches(expected)"})
            check(source.contains(required), "M11 Paper adapter missing " + required);
        for (String forbidden : new String[]{"runTaskAsynchronously", "net.minecraft", "craftbukkit", "sendBlockChange"})
            check(!source.contains(forbidden), "M11 Paper adapter crossed forbidden boundary via " + forbidden);
    }

    private static Progress chain(Fixture fixture, int players) {
        Progress progress = Progress.initial(); int index = 0;
        for (ColdStorageAuthority.Record record : ColdStorageAuthority.CANONICAL_CHAIN)
            progress = ColdStorageAuthority.append(progress, fixture.state.snapshot(), id(players + "-" + index++), record).progress();
        return progress;
    }
    private static Progress roundTrip(ColdStorageProgressStore store, Progress progress, String label) throws Exception {
        store.save(progress); Progress loaded = store.load(); check(loaded.equals(progress), "M11 " + label + " did not survive restart"); return loaded;
    }
    private static UUID id(String value) { return UUID.nameUUIDFromBytes(("m11-" + value).getBytes(StandardCharsets.UTF_8)); }
    private static String text(byte[] value) { return new String(value, StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static final class Fixture implements AutoCloseable {
        private final Path directory; private final MorrowLocalState state;
        private Fixture(Path directory, MorrowLocalState state) { this.directory = directory; this.state = state; }
        static Fixture create(String prefix, boolean ready) throws Exception {
            Path directory = Files.createTempDirectory(prefix); MorrowLocalState state = MorrowLocalState.open(directory.resolve("morrow.journal"), RELEASE);
            if (ready) seed(state); return new Fixture(directory, state);
        }
        private static void seed(MorrowLocalState state) throws Exception {
            String[] events = {"morrow.act0.case_chain_authenticated", "morrow.act0.server_handoff_recovered",
                    "morrow.act1.room04_witnessed", "morrow.act1.static_proposal_authenticated",
                    "morrow.act1.intention_error_proven", "morrow.act1.entity_replay_authorized",
                    "morrow.act2.missing_role_completed", "morrow.act2.live_test_recorded", "morrow.act2.behavior_reuse_proven",
                    "morrow.act2.private_contradiction_resolved", "morrow.act2.live_capture_authorized",
                    "morrow.act3.version_fragments_authenticated", "morrow.act3.incomplete_consensus_proven",
                    "morrow.act4.witness_anchor_registered", "morrow.act4.almost_home_proven",
                    "morrow.act4.account_continuity_authorized", "morrow.act5.continued_session_observed",
                    "morrow.act5.returning_identity_authenticated", "morrow.act5.dual_session_consciousness_proven",
                    ColdStorageAuthority.PREREQUISITE};
            for (int index = 0; index < events.length; index++) {
                String event = events[index];
                if (event.startsWith("morrow.act0") || event.equals("morrow.act2.private_contradiction_resolved")
                        || event.equals(ColdStorageAuthority.PREREQUISITE))
                    state.acceptProjection(event, "m11:external:" + index, bytes("{}"));
                else state.commit(event, "m11:minecraft:" + index, bytes("{}"));
            }
        }
        @Override public void close() throws IOException { Files.deleteIfExists(directory.resolve("morrow.journal")); Files.deleteIfExists(directory); }
    }
    private static final class FakeWorld implements ColdStorageInstaller.WorldPort {
        private final java.util.Map<ColdStorageManifest.Cell, String> blocks = new java.util.LinkedHashMap<>();
        @Override public String binding() { return "m11-test-world:00000000-0000-0000-0000-000000000011"; }
        @Override public String blockData(ColdStorageManifest.Cell relative) { return blocks.getOrDefault(relative, "minecraft:air"); }
        @Override public boolean isAir(ColdStorageManifest.Cell relative) { return "minecraft:air".equals(blockData(relative)); }
        @Override public void setBlockData(ColdStorageManifest.Cell relative, String blockData) { blocks.put(relative, blockData); }
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
