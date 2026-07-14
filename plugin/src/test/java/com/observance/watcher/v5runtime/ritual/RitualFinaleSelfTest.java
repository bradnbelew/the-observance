package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.BallotTelemetry;
import com.observance.watcher.v5runtime.ConductVerdict;
import com.observance.watcher.v5runtime.LeaseBook;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthorityLoader;
import com.observance.watcher.v5runtime.SiteMutexes;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.ritual.FinaleStateStore.Phase;
import com.observance.watcher.v5runtime.ritual.RitualChoices.ClosingChoice;
import com.observance.watcher.v5runtime.ritual.RitualChoices.NameTreatment;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenOutcome;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenTopic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Main-driven table tests for the isolated six-node V5 ritual/finale implementation. */
public final class RitualFinaleSelfTest {
    private RitualFinaleSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        PhysicalPredicateAuthority authority = PhysicalPredicateAuthorityLoader.loadDefault();
        RitualAuthorityContract contract = new RitualAuthorityContract(authority);
        check(RitualPredicateCoverage.implementedNodeIds().equals(
                Set.of("WR03", "WR05", "RP03", "RP04", "RP05", "RP06")),
                "exact six-node implementation coverage");
        CanonicalRitualText text = new CanonicalRitualText(contract);

        wrenRequiresFourCompletedTurns(authority, contract, text);
        ballotTables(authority, contract);
        collectivePresence(authority, contract);
        cancelledArm(authority, contract, text);
        int endings = everyEndingAndRestart(authority, contract, text);

        System.out.println("RitualFinaleSelfTest: PASS (six nodes; 1/2/7 rosters; "
                + endings + " ending/conduct combinations; restart/idempotence verified)");
    }

    private static void wrenRequiresFourCompletedTurns(
            PhysicalPredicateAuthority authority,
            RitualAuthorityContract contract,
            CanonicalRitualText text) throws Exception {
        try (Fixture fixture = Fixture.create(authority)) {
            fixture.progress.completeIfAbsent("v5_wr02_index");
            WrenDialogueRite rite = new WrenDialogueRite(
                    contract, text, fixture.progress, new LeaseBook(), new SiteMutexes(), fixture.clock);
            UUID player = uuid(1);
            check(rite.beginClosing(player, ClosingChoice.YOU_CHOSE_TO_SEND_THEM).status()
                    == WrenDialogueRite.BeginStatus.MISSING_TOPICS, "closing cannot skip topics");
            for (WrenTopic topic : WrenTopic.values()) {
                WrenDialogueRite.BeginResult begun = rite.beginTopic(player, topic);
                check(begun.status() == WrenDialogueRite.BeginStatus.STARTED, "topic starts");
                UUID turn = begun.turn().orElseThrow().id();
                check(rite.completeReply(player, turn) == WrenDialogueRite.CompleteStatus.TOO_EARLY,
                        "one click cannot complete a reply");
                fixture.clock.advanceTicks(100);
                check(rite.completeReply(player, turn)
                        == WrenDialogueRite.CompleteStatus.COMPLETED_REPLY, "reply completes later");
                check(rite.beginTopic(player, topic).status()
                        == WrenDialogueRite.BeginStatus.ALREADY_HEARD, "duplicate topic is stable");
            }
            check(!rite.isComplete(), "three topics alone do not complete WR03");
            WrenDialogueRite.BeginResult closing = rite.beginClosing(
                    player, ClosingChoice.YOU_CHOSE_TO_SEND_THEM);
            check(closing.status() == WrenDialogueRite.BeginStatus.STARTED,
                    "explicit closing choice starts canonical reply");
            check(!rite.isComplete(), "closing selection alone does not complete WR03");
            fixture.clock.advanceTicks(100);
            check(rite.completeReply(player, closing.turn().orElseThrow().id())
                    == WrenDialogueRite.CompleteStatus.COMPLETED_WR03,
                    "completed canonical reply commits WR03");
            check(rite.isComplete(), "WR03 durable flag");
        }
    }

    private static void ballotTables(
            PhysicalPredicateAuthority authority, RitualAuthorityContract contract) throws Exception {
        strictPluralityRoster(authority, contract, 1);
        tiedTwoPlayerRoster(authority, contract);
        strictPluralityRoster(authority, contract, 7);
        disconnectResnapshot(authority, contract);
    }

    private static void strictPluralityRoster(
            PhysicalPredicateAuthority authority, RitualAuthorityContract contract, int count)
            throws Exception {
        try (Fixture fixture = Fixture.create(authority)) {
            fixture.progress.completeIfAbsent("v5_wr04_bridge");
            Set<UUID> roster = roster(count, 100 + count * 10);
            UUID depositor = roster.iterator().next();
            VisibleBallotRite ballots = fixture.ballots(contract);
            ProtocolBridge bridge = bridge(200 + count);
            check(ballots.startWr05(depositor, roster, bridge).status()
                    == VisibleBallotRite.StartStatus.STARTED, "WR05 starts for " + count);
            int index = 0;
            VisibleBallotRite.VoteResult result = null;
            for (UUID player : roster) {
                String branch = count == 7
                        ? (index < 4 ? "FREE" : index < 6 ? "UNDERSTAND" : "CONDEMN")
                        : "FREE";
                result = ballots.cast(VisibleBallotRite.VoteNode.WR05, player, branch);
                if (index == 0 && count > 1) {
                    check(ballots.cast(VisibleBallotRite.VoteNode.WR05, player, branch).status()
                            == VisibleBallotRite.VoteStatus.DUPLICATE,
                            "duplicate click does not add a vote");
                }
                index++;
            }
            check(result != null && result.status() == VisibleBallotRite.VoteStatus.RESOLVED,
                    "strict plurality resolves " + count);
            check(result.resolution().orElseThrow().returnedBridge().orElseThrow().instanceId()
                    .equals(bridge.instanceId()), "same Protocol Bridge identity returned");
            check(result.resolution().orElseThrow().telemetry().initialRosterCount() == count,
                    "visible roster telemetry " + count);

            fixture.progress.completeIfAbsent("v5_rp02_configured");
            for (UUID player : roster) {
                ballots.markConsequenceBookRead(player);
            }
            check(ballots.startRp03(roster).status() == VisibleBallotRite.StartStatus.STARTED,
                    "RP03 starts only after every book receipt");
            for (UUID player : roster) {
                result = ballots.cast(VisibleBallotRite.VoteNode.RP03, player, "PUBLISH");
            }
            check(result != null && result.status() == VisibleBallotRite.VoteStatus.RESOLVED,
                    "RP03 resolves " + count);
            ConductVerdict expected = count == 1 ? ConductVerdict.SOLO
                    : count == 7 ? ConductVerdict.DIVIDED : ConductVerdict.UNANIMOUS;
            check(result.resolution().orElseThrow().conductVerdict().orElseThrow() == expected,
                    "conduct for roster " + count);
        }
    }

    private static void tiedTwoPlayerRoster(
            PhysicalPredicateAuthority authority, RitualAuthorityContract contract) throws Exception {
        try (Fixture fixture = Fixture.create(authority)) {
            fixture.progress.completeIfAbsent("v5_wr04_bridge");
            Set<UUID> roster = roster(2, 300);
            List<UUID> players = List.copyOf(roster);
            VisibleBallotRite ballots = fixture.ballots(contract);
            ProtocolBridge bridge = bridge(302);
            check(ballots.startWr05(players.get(0), roster, bridge).status()
                    == VisibleBallotRite.StartStatus.STARTED, "two-player WR05 starts");
            ballots.cast(VisibleBallotRite.VoteNode.WR05, players.get(0), "CONDEMN");
            VisibleBallotRite.VoteResult tie = ballots.cast(
                    VisibleBallotRite.VoteNode.WR05, players.get(1), "FREE");
            check(tie.status() == VisibleBallotRite.VoteStatus.TIE_OR_REVOTE,
                    "WR05 tie remains visible");
            check(tie.window().orElseThrow().tiebreaker().orElseThrow().equals(players.get(0)),
                    "depositor is deterministic tiebreaker");
            check(ballots.cast(VisibleBallotRite.VoteNode.WR05, players.get(1), "FREE").status()
                    == VisibleBallotRite.VoteStatus.NOT_TIEBREAKER,
                    "non-tiebreaker cannot resolve");
            VisibleBallotRite.VoteResult wrenResolved = ballots.cast(
                    VisibleBallotRite.VoteNode.WR05, players.get(0), "FREE");
            check(wrenResolved.status() == VisibleBallotRite.VoteStatus.RESOLVED,
                    "WR05 depositor resolves tied branch");
            check(wrenResolved.resolution().orElseThrow().telemetry().firstBallotTied(),
                    "tied first ballot preserved");

            fixture.progress.completeIfAbsent("v5_rp02_configured");
            for (UUID player : roster) {
                ballots.markConsequenceBookRead(player);
            }
            ballots.startRp03(roster);
            ballots.cast(VisibleBallotRite.VoteNode.RP03, players.get(0), "PUBLISH");
            VisibleBallotRite.VoteResult nameTie = ballots.cast(
                    VisibleBallotRite.VoteNode.RP03, players.get(1), "RELEASE_UNNAMED");
            check(nameTie.status() == VisibleBallotRite.VoteStatus.TIE_OR_REVOTE
                    && nameTie.window().orElseThrow().phase() == VisibleBallotRite.WindowPhase.REVOTE,
                    "RP03 tie opens revote and never auto-selects");
            ballots.cast(VisibleBallotRite.VoteNode.RP03, players.get(0), "PUBLISH");
            VisibleBallotRite.VoteResult nameResolved = ballots.cast(
                    VisibleBallotRite.VoteNode.RP03, players.get(1), "PUBLISH");
            check(nameResolved.status() == VisibleBallotRite.VoteStatus.RESOLVED,
                    "RP03 later strict plurality resolves");
            check(nameResolved.resolution().orElseThrow().conductVerdict().orElseThrow()
                    == ConductVerdict.DIVIDED, "first tied ballots derive DIVIDED");
        }
    }

    private static void disconnectResnapshot(
            PhysicalPredicateAuthority authority, RitualAuthorityContract contract) throws Exception {
        try (Fixture fixture = Fixture.create(authority)) {
            fixture.progress.completeIfAbsent("v5_wr04_bridge");
            Set<UUID> roster = roster(2, 400);
            List<UUID> players = List.copyOf(roster);
            VisibleBallotRite ballots = fixture.ballots(contract);
            ballots.startWr05(players.get(0), roster, bridge(402));
            ballots.cast(VisibleBallotRite.VoteNode.WR05, players.get(0), "UNDERSTAND");
            ballots.disconnected(VisibleBallotRite.VoteNode.WR05, players.get(1));
            fixture.clock.advanceTicks(399);
            check(ballots.tick(VisibleBallotRite.VoteNode.WR05).status()
                    != VisibleBallotRite.VoteStatus.RESOLVED, "disconnect grace is visible");
            fixture.clock.advanceTicks(1);
            VisibleBallotRite.VoteResult resolved = ballots.tick(
                    VisibleBallotRite.VoteNode.WR05);
            check(resolved.status() == VisibleBallotRite.VoteStatus.RESOLVED,
                    "resnapshot resolves remaining complete roster");
            BallotTelemetry telemetry = resolved.resolution().orElseThrow().telemetry();
            check(telemetry.firstBallotCastCount() == 1
                            && telemetry.firstBallotEligibleCount() == 2
                            && telemetry.resolutionRounds() == 2
                            && telemetry.disconnectResnapCount() == 1,
                    "first ballot persisted before disconnect resnapshot");
        }
    }

    private static void collectivePresence(
            PhysicalPredicateAuthority authority, RitualAuthorityContract contract) throws Exception {
        try (Fixture fixture = Fixture.create(authority)) {
            seedResolvedChoices(fixture.progress, WrenOutcome.FREE, NameTreatment.PUBLISH,
                    ConductVerdict.UNANIMOUS);
            Set<UUID> roster = roster(2, 500);
            CollectivePresenceRite rite = new CollectivePresenceRite(
                    contract, fixture.progress, new LeaseBook(), new SiteMutexes(), fixture.clock,
                    (operator, oldSector, replacement, reason) -> { });
            ProtocolBridge bridge = bridge(502).retag(WrenOutcome.FREE);
            check(rite.start(roster, bridge).status() == CollectivePresenceRite.Status.STARTED,
                    "RP04 starts");
            List<UUID> players = List.copyOf(roster);
            rite.updatePresence(players.get(0), 0, true);
            rite.updatePresence(players.get(1), 1, true);
            rite.confirmOwnSector(players.get(0), 0);
            check(rite.confirmOwnSector(players.get(1), 1).status()
                    != CollectivePresenceRite.Status.COMPLETED,
                    "sector confirmation alone cannot skip Bridge operation");
            check(rite.confirmFreeCenterToWhiteTrough(players.get(0), true, true).status()
                    == CollectivePresenceRite.Status.COMPLETED,
                    "distinct lit sectors and exact branch operation complete RP04");
            check(fixture.progress.snapshot().isComplete("v5_rp04_collective"),
                    "RP04 durable receipt");
        }
    }

    private static void cancelledArm(
            PhysicalPredicateAuthority authority,
            RitualAuthorityContract contract,
            CanonicalRitualText text) throws Exception {
        try (Fixture fixture = Fixture.create(authority)) {
            seedFinaleReady(fixture.progress, WrenOutcome.UNDERSTAND,
                    NameTreatment.RELEASE_UNNAMED, ConductVerdict.UNANIMOUS);
            FinaleStateStore store = FinaleStateStore.open(
                    fixture.directory.resolve("finale.json"), authority.sha256());
            FinaleRite finale = new FinaleRite(contract, text, fixture.progress, store,
                    new LeaseBook(), new SiteMutexes(), fixture.clock);
            check(finale.arm("operator", 14).status() == FinaleRite.ArmStatus.INVALID_WINDOW,
                    "arm rejects below 15 seconds");
            check(finale.arm("operator", null).status() == FinaleRite.ArmStatus.ARMED
                            && finale.snapshot().cancelCutoffAt() - finale.snapshot().armedAt()
                            == 120_000L,
                    "arm default is exactly 120 seconds");
            check(finale.cancel().status() == FinaleRite.ArmStatus.CANCELLED,
                    "armed finale cancels safely");
            check(finale.confirm(validProof(uuid(600))).status()
                    == FinaleRite.ConfirmStatus.NOT_ARMED, "cancelled arm cannot commit");
            check(!fixture.progress.snapshot().isComplete("v5_rp05_severed"),
                    "cancel leaves RP05 red");
        }
    }

    private static int everyEndingAndRestart(
            PhysicalPredicateAuthority authority,
            RitualAuthorityContract contract,
            CanonicalRitualText text) throws Exception {
        Map<ConductVerdict, Integer> seen = new EnumMap<>(ConductVerdict.class);
        int count = 0;
        for (WrenOutcome wren : WrenOutcome.values()) {
            for (NameTreatment name : NameTreatment.values()) {
                for (ConductVerdict conduct : ConductVerdict.values()) {
                    try (Fixture fixture = Fixture.create(authority)) {
                        seedFinaleReady(fixture.progress, wren, name, conduct);
                        Path statePath = fixture.directory.resolve("finale.json");
                        RecordingEffects effects = new RecordingEffects();
                        FinaleRite finale = openFinale(
                                contract, text, fixture, statePath, authority.sha256());
                        check(finale.arm("operator", 15).status() == FinaleRite.ArmStatus.ARMED,
                                "arm " + wren + "/" + name + "/" + conduct);
                        check(finale.confirm(new FinaleRite.PlayerCommitProof(
                                uuid(700 + count), true, true, false, true, true, true, 59))
                                .status() == FinaleRite.ConfirmStatus.INVALID_PLAYER_PROOF,
                                "59 ticks cannot sever");
                        check(finale.confirm(validProof(uuid(700 + count))).status()
                                == FinaleRite.ConfirmStatus.COMMITTED, "60 ticks commit");

                        for (Phase expected : List.of(
                                Phase.DARKENING, Phase.SYNTAX_BREAK, Phase.GOODBYE,
                                Phase.SAVE_AND_CODA, Phase.CODA)) {
                            check(finale.resumeOnePhase(effects) == expected,
                                    "phase " + expected + " for " + wren + "/" + name + "/" + conduct);
                            finale = openFinale(contract, text, fixture, statePath, authority.sha256());
                        }
                        check(finale.resumeOnePhase(effects) == Phase.CODA,
                                "CODA is terminal idempotent");
                        check(effects.calls.getOrDefault("rp06.production_shutdown", 0) == 1,
                                "shutdown exactly once");
                        check(effects.calls.values().stream().allMatch(value -> value == 1),
                                "every external effect exactly once across restarts");
                        CanonicalRitualText.EndingText exact = text.ending(
                                new RitualChoices.EndingDimensions(wren, name, conduct));
                        check(effects.goodbye.equals(exact.completeGoodbye()),
                                "exact authority goodbye composition");
                        check(finale.codaReceipt().orElseThrow().key().equals(exact.codaKey()),
                                "branch-specific Coda receipt");
                        check(fixture.progress.snapshot().isComplete("v5_case_c10_complete"),
                                "C10 locally complete before shutdown");
                        seen.merge(conduct, 1, Integer::sum);
                        count++;
                    }
                }
            }
        }
        check(count == 24 && seen.values().stream().allMatch(value -> value == 6),
                "all 24 Wren/name/conduct combinations");
        return count;
    }

    private static FinaleRite openFinale(
            RitualAuthorityContract contract,
            CanonicalRitualText text,
            Fixture fixture,
            Path statePath,
            String hash) throws Exception {
        return new FinaleRite(contract, text, fixture.progress,
                FinaleStateStore.open(statePath, hash), new LeaseBook(), new SiteMutexes(), fixture.clock);
    }

    private static void seedFinaleReady(
            V5ProgressStore progress, WrenOutcome wren, NameTreatment name, ConductVerdict conduct)
            throws IOException {
        seedResolvedChoices(progress, wren, name, conduct);
        progress.completeIfAbsent("v5_rp04_collective");
    }

    private static void seedResolvedChoices(
            V5ProgressStore progress, WrenOutcome wren, NameTreatment name, ConductVerdict conduct)
            throws IOException {
        BallotTelemetry[] ballots = ballotsFor(conduct);
        progress.completeIfAbsent("v5_rp02_configured");
        progress.transact(editor -> {
            editor.setBooleanTrue("v5_rp01_instruction");
            editor.putBranchOnce("v5_wren_outcome", wren.wireValue());
            editor.putBallotOnce("WR05", ballots[0]);
            editor.compareAndSetCompletion("v5_case_c08_complete", false, true);
            return null;
        });
        progress.transact(editor -> {
            editor.putBranchOnce("v5_name_treatment", name.wireValue());
            editor.putBallotOnce("RP03", ballots[1]);
            check(editor.deriveAndSetConductVerdict() == conduct, "seed conduct " + conduct);
            editor.compareAndSetCompletion("v5_rp03_name_choice", false, true);
            return null;
        });
    }

    private static BallotTelemetry[] ballotsFor(ConductVerdict conduct) {
        return switch (conduct) {
            case SOLO -> new BallotTelemetry[]{ballot(1, 1, 1, false, 1, 0),
                    ballot(1, 1, 1, false, 1, 0)};
            case UNANIMOUS -> new BallotTelemetry[]{ballot(2, 2, 1, false, 1, 0),
                    ballot(2, 2, 1, false, 1, 0)};
            case DIVIDED -> new BallotTelemetry[]{ballot(2, 2, 2, true, 2, 0),
                    ballot(2, 2, 1, false, 1, 0)};
            case PERSISTENT -> new BallotTelemetry[]{ballot(2, 1, 1, false, 2, 1),
                    ballot(2, 2, 1, false, 1, 0)};
        };
    }

    private static BallotTelemetry ballot(
            int roster, int cast, int distinct, boolean tied, int rounds, int resnaps) {
        return new BallotTelemetry(roster, roster, roster, cast, distinct, tied, rounds, resnaps);
    }

    private static FinaleRite.PlayerCommitProof validProof(UUID playerId) {
        return new FinaleRite.PlayerCommitProof(
                playerId, true, true, false, true, true, true, 60);
    }

    private static ProtocolBridge bridge(int value) {
        return new ProtocolBridge(uuid(value), "a".repeat(64),
                Map.of(ProtocolBridge.ARTIFACT_KEY, ProtocolBridge.ARTIFACT_VALUE));
    }

    private static Set<UUID> roster(int count, int start) {
        Set<UUID> result = new LinkedHashSet<>();
        for (int index = 0; index < count; index++) {
            result.add(uuid(start + index));
        }
        return Set.copyOf(result);
    }

    private static UUID uuid(int value) {
        return new UUID(0L, value);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class MutableClock implements RitualClock {
        private long epochMillis = 1_000_000L;
        private long tick = 1_000L;

        @Override
        public long epochMillis() {
            return epochMillis;
        }

        @Override
        public long tick() {
            return tick;
        }

        private void advanceTicks(long ticks) {
            tick += ticks;
            epochMillis += ticks * 50L;
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final Path directory;
        private final V5ProgressStore progress;
        private final MutableClock clock = new MutableClock();
        private final PhysicalPredicateAuthority authority;

        private Fixture(Path directory, V5ProgressStore progress,
                        PhysicalPredicateAuthority authority) {
            this.directory = directory;
            this.progress = progress;
            this.authority = authority;
        }

        private static Fixture create(PhysicalPredicateAuthority authority) throws IOException {
            Path directory = Files.createTempDirectory("observance-v5-ritual-");
            return new Fixture(directory,
                    V5ProgressStore.open(directory.resolve("progress.json"), authority), authority);
        }

        private VisibleBallotRite ballots(RitualAuthorityContract contract) throws IOException {
            return new VisibleBallotRite(contract, progress,
                    BallotEvidenceJournal.open(directory.resolve("ballots.json"), authority.sha256()),
                    new LeaseBook(), new SiteMutexes(), clock);
        }

        @Override
        public void close() throws IOException {
            if (!Files.exists(directory)) {
                return;
            }
            try (var paths = Files.walk(directory)) {
                paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException failure) {
                        throw new RuntimeException(failure);
                    }
                });
            } catch (RuntimeException failure) {
                if (failure.getCause() instanceof IOException io) {
                    throw io;
                }
                throw failure;
            }
        }
    }

    private static final class RecordingEffects implements FinaleRite.FinaleEffects {
        private final Map<String, Integer> calls = new LinkedHashMap<>();
        private List<String> goodbye = List.of();

        @Override
        public void darken(String key) {
            call(key);
        }

        @Override
        public void syntaxBreak(String key) {
            call(key);
        }

        @Override
        public void goodbye(String key, List<String> exactLines) {
            call(key);
            goodbye = List.copyOf(exactLines);
        }

        @Override
        public void savePlayersAndWorlds(String key) {
            call(key);
        }

        @Override
        public void kickPlayers(String key, List<String> exactLines) {
            call(key);
            check(exactLines.equals(goodbye), "kick contains the complete goodbye");
        }

        @Override
        public void requestProductionShutdown(String key) {
            call(key);
        }

        private void call(String key) {
            calls.merge(key, 1, Integer::sum);
        }
    }
}
