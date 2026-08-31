package com.observance.watcher.morrow.continuity;

import com.observance.watcher.morrow.MorrowLocalState;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.AnchorMark;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.Progress;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.Result;
import com.observance.watcher.morrow.continuity.AccountContinuityAuthority.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Main-driven M09 privacy, disconnect, identity, reset, cohort, idempotency, and restart matrix. */
public final class AccountContinuityAuthoritySelfTest {
    private static final String RELEASE = "morrow.rehearsal.m09";
    private static final List<String> ACTIONS = List.of(
            "walk:north_4", "look:house", "crouch:1", "interact:ledger", "walk:south_2");

    private AccountContinuityAuthoritySelfTest() { }

    public static void main(String[] args) throws Exception {
        lockedAndWrongPlayerPathsAreSafe();
        oneTwoSixPlayerDisconnectAndIdentityProofs();
        wrongSecretResetAndImmutableBoundaries();
        everyCrashWindowSurvivesRestartWithoutPlaintextSecret();
        physicalManifestAndInstallerAreBounded();
        System.out.println("MORROW ACCOUNT CONTINUITY M09: PASS secret=sealed echo=bounded players=1/2/6");
    }

    private static void lockedAndWrongPlayerPathsAreSafe() throws Exception {
        try (Fixture fixture = Fixture.create("m09-locked-", false)) {
            Result locked = AccountContinuityAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                    id("volunteer"), AnchorMark.COPPER_CROSS, challenge("locked"));
            check(locked.status() == Status.LOCKED && locked.progress().revision() == 0,
                    "M09 must remain locked until Account Continuity is separately authorized");
        }
        try (Fixture fixture = Fixture.create("m09-owner-", true)) {
            UUID volunteer = id("volunteer"), other = id("other");
            Progress begun = AccountContinuityAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                    volunteer, AnchorMark.NORTH_STEP, challenge("owner")).progress();
            Result wrong = AccountContinuityAuthority.arm(begun, fixture.state.snapshot(), other);
            check(wrong.status() == Status.WRONG_PLAYER && wrong.progress().equals(begun),
                    "another account must not arm or mutate the volunteer's test");
        }
    }

    private static void oneTwoSixPlayerDisconnectAndIdentityProofs() throws Exception {
        for (int players : new int[]{1, 2, 6}) {
            try (Fixture fixture = Fixture.create("m09-group-" + players + "-", true)) {
                UUID volunteer = id(players + "-volunteer"); AnchorMark secret = AnchorMark.values()[players - 1];
                Progress progress = AccountContinuityAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                        volunteer, secret, challenge("group-" + players)).progress();
                progress = AccountContinuityAuthority.arm(progress, fixture.state.snapshot(), volunteer).progress();
                progress = AccountContinuityAuthority.recordDisconnect(progress, fixture.state.snapshot(),
                        volunteer, 1_000L + players, ACTIONS).progress();
                progress = AccountContinuityAuthority.recordReturn(progress, fixture.state.snapshot(),
                        volunteer, 1_100L + players).progress();
                UUID observer = id(players + "-observer-" + ((players - 1) % players));
                Result observed = AccountContinuityAuthority.observeContinuation(
                        progress, fixture.state.snapshot(), observer);
                check(observed.status() == Status.READY_TO_COMMIT && observed.progress().continuationObserved()
                                && observed.eventKey().equals(AccountContinuityAuthority.CONTINUED_EVENT),
                        players + "-player cohort cannot file the bounded echo observation");
                String continuedPayload = text(AccountContinuityAuthority.payload(observed));
                check(continuedPayload.contains("\"echo_action_count\":5")
                                && continuedPayload.contains("\"echo_secret_access\":false")
                                && !continuedPayload.contains(secret.name()),
                        players + "-player continuation payload leaked the secret or omitted the knowledge boundary");
                fixture.state.commit(observed.eventKey(), observed.idempotencyKey(),
                        AccountContinuityAuthority.payload(observed));
                Result identity = AccountContinuityAuthority.authenticate(
                        observed.progress(), fixture.state.snapshot(), volunteer, secret);
                check(identity.status() == Status.READY_TO_COMMIT && identity.progress().authenticated()
                                && identity.eventKey().equals(AccountContinuityAuthority.IDENTITY_EVENT),
                        players + "-player returning identity was not authenticated by excluded knowledge");
                var first = fixture.state.commit(identity.eventKey(), identity.idempotencyKey(),
                        AccountContinuityAuthority.payload(identity));
                var duplicate = fixture.state.commit(identity.eventKey(), identity.idempotencyKey(),
                        AccountContinuityAuthority.payload(identity));
                check(first.created() && !duplicate.created() && first.sequence() == duplicate.sequence(),
                        players + "-player M09 identity event is not idempotent");
                String identityPayload = text(AccountContinuityAuthority.payload(identity));
                check(identityPayload.contains("reconstructed_continuation")
                                && identityPayload.contains(volunteer.toString())
                                && !identityPayload.contains(secret.name()),
                        players + "-player identity payload overexposes the private choice or loses classification");
            }
        }
    }

    private static void wrongSecretResetAndImmutableBoundaries() throws Exception {
        try (Fixture fixture = Fixture.create("m09-wrong-", true)) {
            UUID volunteer = id("wrong-volunteer");
            Progress returned = returned(fixture, volunteer, AnchorMark.EAST_ARROW, "wrong");
            Result observed = AccountContinuityAuthority.observeContinuation(returned, fixture.state.snapshot(), volunteer);
            fixture.state.commit(observed.eventKey(), observed.idempotencyKey(), AccountContinuityAuthority.payload(observed));
            Result wrong = AccountContinuityAuthority.authenticate(observed.progress(), fixture.state.snapshot(),
                    volunteer, AnchorMark.LOW_ARCH);
            check(wrong.status() == Status.WRONG_SECRET && wrong.progress().equals(observed.progress())
                            && !fixture.state.snapshot().committedEvents().contains(AccountContinuityAuthority.IDENTITY_EVENT),
                    "wrong M09 secret must pause safely without harming either identity");
            Result immutable = AccountContinuityAuthority.reset(observed.progress(), fixture.state.snapshot(), volunteer);
            check(immutable.status() == Status.IMMUTABLE && immutable.progress().equals(observed.progress()),
                    "filed continuation evidence must not be erased by a later reset");
        }
        try (Fixture fixture = Fixture.create("m09-reset-", true)) {
            UUID volunteer = id("reset-volunteer");
            Progress armed = AccountContinuityAuthority.arm(AccountContinuityAuthority.begin(
                    Progress.initial(), fixture.state.snapshot(), volunteer, AnchorMark.DOUBLE_PILLAR,
                    challenge("reset")).progress(), fixture.state.snapshot(), volunteer).progress();
            Result reset = AccountContinuityAuthority.reset(armed, fixture.state.snapshot(), volunteer);
            check(reset.status() == Status.RESET && reset.progress().challenge() == null
                            && !fixture.state.snapshot().committedEvents().contains(AccountContinuityAuthority.CONTINUED_EVENT),
                    "M09 pre-observation reset must return everyone to a safe clean lobby");
        }
    }

    private static void everyCrashWindowSurvivesRestartWithoutPlaintextSecret() throws Exception {
        Path directory = Files.createTempDirectory("m09-store-"); Path path = directory.resolve("continuity.progress");
        try (Fixture fixture = Fixture.create("m09-store-state-", true)) {
            AccountContinuityProgressStore store = new AccountContinuityProgressStore(path, RELEASE);
            UUID volunteer = id("store-volunteer"); AnchorMark secret = AnchorMark.HOLLOW_SQUARE;
            Progress progress = AccountContinuityAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                    volunteer, secret, challenge("store")).progress();
            progress = roundTrip(store, progress, "sealed challenge");
            String stored = Files.readString(path, StandardCharsets.UTF_8);
            check(!stored.contains(secret.name()) && !stored.contains(secret.label()),
                    "M09 progress store retained the private anchor in plaintext");
            progress = roundTrip(store, AccountContinuityAuthority.arm(
                    progress, fixture.state.snapshot(), volunteer).progress(), "armed test");
            progress = roundTrip(store, AccountContinuityAuthority.recordDisconnect(
                    progress, fixture.state.snapshot(), volunteer, 2_000, ACTIONS).progress(), "disconnect receipt");
            progress = roundTrip(store, AccountContinuityAuthority.recordReturn(
                    progress, fixture.state.snapshot(), volunteer, 2_100).progress(), "return receipt");
            progress = roundTrip(store, AccountContinuityAuthority.observeContinuation(
                    progress, fixture.state.snapshot(), id("observer")).progress(), "local continuation observation");
            check(AccountContinuityAuthority.recoverContinuation(
                            progress, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT,
                    "M09 continuation crash window cannot recover without replaying disconnect");
            Result continued = AccountContinuityAuthority.recoverContinuation(progress, fixture.state.snapshot());
            fixture.state.commit(continued.eventKey(), continued.idempotencyKey(),
                    AccountContinuityAuthority.payload(continued));
            progress = roundTrip(store, AccountContinuityAuthority.authenticate(
                    progress, fixture.state.snapshot(), volunteer, secret).progress(), "local identity proof");
            check(AccountContinuityAuthority.recoverIdentity(
                            progress, fixture.state.snapshot()).status() == Status.READY_TO_COMMIT,
                    "M09 identity crash window cannot recover without exposing or replaying the secret");
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            lines.set(2, "revision=999"); Files.write(path, lines, StandardCharsets.UTF_8);
            try { store.load(); throw new AssertionError("expected M09 corruption failure"); }
            catch (IOException expected) { /* expected */ }
        } finally { Files.deleteIfExists(path); Files.deleteIfExists(directory); }
    }

    private static void physicalManifestAndInstallerAreBounded() throws Exception {
        AccountContinuityManifest manifest = new AccountContinuityManifest();
        check(manifest.cells().size() == 2_520 && manifest.stateLamps().size() == 4
                        && manifest.manifestSha256().matches("[0-9a-f]{64}")
                        && manifest.manifestSha256().equals(new AccountContinuityManifest().manifestSha256())
                        && "minecraft:air".equals(manifest.cells().get(manifest.returnCell()))
                        && "minecraft:crying_obsidian".equals(manifest.cells().get(manifest.echoDais())),
                "M09 manifest must be one stable bounded safe lobby with a distinct echo dais");
        Path directory = Files.createTempDirectory("m09-installer-"); Path receipt = directory.resolve("m09.receipt");
        AccountContinuityInstaller.Origin origin = new AccountContinuityInstaller.Origin(160, 80, 0);
        try {
            FakeWorld world = new FakeWorld(); AccountContinuityInstaller installer = new AccountContinuityInstaller(manifest, receipt);
            AccountContinuityInstaller.Result built = installer.install(RELEASE, origin, world);
            check(built.status() == AccountContinuityInstaller.Status.BUILT && built.blockCount() == 2_520,
                    "M09 empty-target installer must audit every safe-lobby cell");
            check(installer.install(RELEASE, origin, world).status() == AccountContinuityInstaller.Status.ALREADY_PRESENT,
                    "M09 installer does not validate its release/world/origin receipt after restart");
            AccountContinuityManifest.Cell lamp = manifest.stateLamps().values().iterator().next();
            world.setBlockData(lamp, "minecraft:copper_bulb[lit=true,powered=false]"); installer.audit(world, true);
            try { installer.audit(world, false); throw new AssertionError("expected strict M09 lamp audit failure"); }
            catch (IOException expected) { /* expected */ }
        } finally { Files.deleteIfExists(receipt); Files.deleteIfExists(directory); }
        Path foreignDirectory = Files.createTempDirectory("m09-foreign-");
        try {
            FakeWorld world = new FakeWorld(); AccountContinuityManifest.Cell cell = manifest.cells().keySet().iterator().next();
            world.setBlockData(cell, "minecraft:diamond_block");
            try {
                new AccountContinuityInstaller(manifest, foreignDirectory.resolve("m09.receipt")).install(RELEASE, origin, world);
                throw new AssertionError("expected M09 occupied-target refusal");
            } catch (IOException expected) {
                check("minecraft:diamond_block".equals(world.blockData(cell)),
                        "M09 occupied-target refusal must preserve foreign blocks");
            }
        } finally { Files.deleteIfExists(foreignDirectory.resolve("m09.receipt")); Files.deleteIfExists(foreignDirectory); }
        String source = Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/continuity/BukkitAccountContinuity.java"))
                + Files.readString(Path.of(
                "src/main/java/com/observance/watcher/morrow/continuity/BukkitAccountContinuityWorld.java"));
        for (String required : new String[]{"PlayerQuitEvent", "PlayerJoinEvent", "PlayerCustomClickEvent",
                "DialogType.multiAction", "DialogType.confirmation", "canCloseWithEscape(true)",
                "PRIVATE ANCHOR: INACCESSIBLE", "SECRET ACCESS: FALSE", "getBlockData().matches(expected)"}) {
            check(source.contains(required), "M09 Paper adapter missing " + required);
        }
        for (String forbidden : new String[]{"runTaskAsynchronously", "net.minecraft",
                "craftbukkit", "sendBlockChange"}) {
            check(!source.contains(forbidden), "M09 Paper adapter crossed forbidden boundary via " + forbidden);
        }
    }

    private static Progress returned(Fixture fixture, UUID volunteer, AnchorMark secret, String suffix) {
        Progress progress = AccountContinuityAuthority.begin(Progress.initial(), fixture.state.snapshot(),
                volunteer, secret, challenge(suffix)).progress();
        progress = AccountContinuityAuthority.arm(progress, fixture.state.snapshot(), volunteer).progress();
        progress = AccountContinuityAuthority.recordDisconnect(
                progress, fixture.state.snapshot(), volunteer, 500, ACTIONS).progress();
        return AccountContinuityAuthority.recordReturn(
                progress, fixture.state.snapshot(), volunteer, 600).progress();
    }
    private static Progress roundTrip(AccountContinuityProgressStore store, Progress progress, String label) throws Exception {
        store.save(progress); Progress loaded = store.load(); check(loaded.equals(progress), "M09 " + label + " did not survive restart");
        return loaded;
    }
    private static String challenge(String value) {
        return UUID.nameUUIDFromBytes(("m09-challenge-" + value).getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }
    private static UUID id(String value) { return UUID.nameUUIDFromBytes(("m09-" + value).getBytes(StandardCharsets.UTF_8)); }
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
                    "morrow.act4.almost_home_proven", AccountContinuityAuthority.PREREQUISITE};
            for (int index = 0; index < events.length; index++) {
                String event = events[index];
                if (event.startsWith("morrow.act0") || event.equals("morrow.act2.private_contradiction_resolved"))
                    state.acceptProjection(event, "m09:external:" + index, bytes("{}"));
                else state.commit(event, "m09:minecraft:" + index, bytes("{}"));
            }
        }
        @Override public void close() throws IOException {
            Files.deleteIfExists(directory.resolve("morrow.journal")); Files.deleteIfExists(directory);
        }
    }
    private static final class FakeWorld implements AccountContinuityInstaller.WorldPort {
        private final Map<AccountContinuityManifest.Cell, String> blocks = new LinkedHashMap<>();
        @Override public String binding() { return "m09-test-world:00000000-0000-0000-0000-000000000009"; }
        @Override public String blockData(AccountContinuityManifest.Cell relative) {
            return blocks.getOrDefault(relative, "minecraft:air");
        }
        @Override public boolean isAir(AccountContinuityManifest.Cell relative) { return "minecraft:air".equals(blockData(relative)); }
        @Override public void setBlockData(AccountContinuityManifest.Cell relative, String blockData) { blocks.put(relative, blockData); }
    }
    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
}
