package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.BallotTelemetry;
import com.observance.watcher.v5runtime.ConductVerdict;
import com.observance.watcher.v5runtime.ConductVerdictDeriver;
import com.observance.watcher.v5runtime.EscrowEntry;
import com.observance.watcher.v5runtime.EscrowStatus;
import com.observance.watcher.v5runtime.LeaseBook;
import com.observance.watcher.v5runtime.PlayerBitDomain;
import com.observance.watcher.v5runtime.SiteMutexes;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.ritual.RitualChoices.NameTreatment;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenOutcome;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** WR05 group ballot and RP03 voluntary-participant choice with immutable first evidence. */
public final class VisibleBallotRite {
    public enum VoteNode {
        WR05(Set.of("CONDEMN", "UNDERSTAND", "FREE")),
        RP03(Set.of("PUBLISH", "RELEASE_UNNAMED"));

        private final Set<String> branches;

        VoteNode(Set<String> branches) {
            this.branches = branches;
        }

        public Set<String> branches() {
            return branches;
        }
    }

    public enum WindowPhase {
        COLLECTING,
        REVOTE,
        TIEBREAK,
        RESOLVED,
        ABORTED
    }

    public enum StartStatus {
        STARTED,
        BUSY,
        EMPTY_ROSTER,
        PREREQUISITE_MISSING,
        INVALID_BRIDGE,
        ALREADY_COMPLETE,
        RECOVERY_REQUIRED
    }

    public enum VoteStatus {
        ACCEPTED,
        CHANGED,
        DUPLICATE,
        NOT_ELIGIBLE,
        INVALID_BRANCH,
        NO_ACTIVE_WINDOW,
        NOT_TIEBREAKER,
        TIEBREAK_BRANCH_REQUIRED,
        TIE_OR_REVOTE,
        RESOLVED,
        ABORTED
    }

    public record Resolution(
            VoteNode node,
            String branch,
            BallotTelemetry telemetry,
            Optional<ProtocolBridge> returnedBridge,
            Optional<ConductVerdict> conductVerdict) {
        public Resolution {
            Objects.requireNonNull(node, "node");
            branch = requireBranch(node, branch);
            Objects.requireNonNull(telemetry, "telemetry");
            returnedBridge = Objects.requireNonNull(returnedBridge, "returnedBridge");
            conductVerdict = Objects.requireNonNull(conductVerdict, "conductVerdict");
            if ((node == VoteNode.WR05) != returnedBridge.isPresent()) {
                throw new IllegalArgumentException("only WR05 returns a Protocol Bridge");
            }
            if ((node == VoteNode.RP03) != conductVerdict.isPresent()) {
                throw new IllegalArgumentException("only RP03 derives conduct");
            }
        }
    }

    public record WindowView(
            VoteNode node,
            WindowPhase phase,
            List<UUID> visibleRoster,
            int receivedVotes,
            int resolutionRound,
            int disconnectResnaps,
            Set<String> tiedBranches,
            Optional<UUID> tiebreaker,
            long closesAtTick) {
        public WindowView {
            visibleRoster = List.copyOf(visibleRoster);
            tiedBranches = Set.copyOf(tiedBranches);
            tiebreaker = Objects.requireNonNull(tiebreaker, "tiebreaker");
        }
    }

    public record StartResult(StartStatus status, Optional<WindowView> window,
                              Set<UUID> unreadPlayers) {
        public StartResult {
            Objects.requireNonNull(status, "status");
            window = Objects.requireNonNull(window, "window");
            unreadPlayers = Set.copyOf(unreadPlayers);
        }
    }

    public record VoteResult(VoteStatus status, Optional<WindowView> window,
                             Optional<Resolution> resolution) {
        public VoteResult {
            Objects.requireNonNull(status, "status");
            window = Objects.requireNonNull(window, "window");
            resolution = Objects.requireNonNull(resolution, "resolution");
        }
    }

    private static final String CONSEQUENCE_READ_BIT = "RP03:CONSEQUENCE_BOOK_READ";
    private static final long WINDOW_TICKS = 45L * 20L;
    private static final long DISCONNECT_GRACE_TICKS = 20L * 20L;
    private static final long TIEBREAK_TICKS = 30L * 20L;
    private static final Duration LEASE_DURATION = Duration.ofMinutes(10);
    private static final Duration MUTEX_TIMEOUT = Duration.ofSeconds(2);

    private static final class Window {
        private final VoteNode node;
        private final LinkedHashSet<UUID> initialRoster;
        private final LinkedHashSet<UUID> roster;
        private final UUID depositor;
        private final ProtocolBridge bridge;
        private final LeaseBook.Token lease;
        private final Map<UUID, String> votes = new LinkedHashMap<>();
        private final Map<UUID, Long> disconnectedAt = new LinkedHashMap<>();
        private WindowPhase phase = WindowPhase.COLLECTING;
        private Set<String> tiedBranches = Set.of();
        private UUID tiebreaker;
        private long closesAtTick;
        private int round = 1;
        private int resnaps;

        private Window(VoteNode node, Set<UUID> roster, UUID depositor,
                       ProtocolBridge bridge, LeaseBook.Token lease, long closesAtTick) {
            this.node = node;
            this.initialRoster = sortedRoster(roster);
            this.roster = sortedRoster(roster);
            this.depositor = depositor;
            this.bridge = bridge;
            this.lease = lease;
            this.closesAtTick = closesAtTick;
        }
    }

    private final RitualAuthorityContract authority;
    private final V5ProgressStore progress;
    private final BallotEvidenceJournal journal;
    private final LeaseBook leases;
    private final SiteMutexes mutexes;
    private final RitualClock clock;
    private final Map<VoteNode, Window> windows = new LinkedHashMap<>();

    public VisibleBallotRite(
            RitualAuthorityContract authority,
            V5ProgressStore progress,
            BallotEvidenceJournal journal,
            LeaseBook leases,
            SiteMutexes mutexes,
            RitualClock clock) {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.journal = Objects.requireNonNull(journal, "journal");
        this.leases = Objects.requireNonNull(leases, "leases");
        this.mutexes = Objects.requireNonNull(mutexes, "mutexes");
        this.clock = Objects.requireNonNull(clock, "clock");
        authority.node("WR05");
        authority.node("RP03");
    }

    public synchronized StartResult startWr05(
            UUID depositor, Set<UUID> visibleRoster, ProtocolBridge bridge) throws IOException {
        Objects.requireNonNull(depositor, "depositor");
        if (bridge == null || bridge.outcome().isPresent()) {
            return startResult(StartStatus.INVALID_BRIDGE, null, Set.of());
        }
        if (visibleRoster == null || !visibleRoster.contains(depositor)) {
            return startResult(StartStatus.EMPTY_ROSTER, null, Set.of());
        }
        StartResult validation = validateStart(VoteNode.WR05, visibleRoster);
        if (validation.status() != StartStatus.STARTED) {
            return validation;
        }
        LeaseBook.Token lease = acquireLease(VoteNode.WR05);
        if (lease == null) {
            return startResult(StartStatus.BUSY, null, Set.of());
        }
        long now = clock.epochMillis();
        EscrowEntry escrow = bridgeEscrow(bridge, depositor, now, EscrowStatus.HELD,
                Map.of("state", "ballot_open"));
        try {
            progress.transact(editor -> {
                editor.putEscrowOnce(escrow);
                return null;
            });
        } catch (IOException | RuntimeException failure) {
            lease.close();
            throw failure;
        }
        Window window = new Window(VoteNode.WR05, visibleRoster, depositor, bridge, lease,
                clock.tick() + WINDOW_TICKS);
        windows.put(VoteNode.WR05, window);
        return startResult(StartStatus.STARTED, window, Set.of());
    }

    public synchronized StartResult startRp03(Set<UUID> visibleRoster) {
        StartResult validation = validateStart(VoteNode.RP03, visibleRoster);
        if (validation.status() != StartStatus.STARTED) {
            return validation;
        }
        LeaseBook.Token lease = acquireLease(VoteNode.RP03);
        if (lease == null) {
            return startResult(StartStatus.BUSY, null, Set.of());
        }
        UUID depositor = sortedRoster(visibleRoster).iterator().next();
        Window window = new Window(VoteNode.RP03, visibleRoster, depositor, null, lease,
                clock.tick() + WINDOW_TICKS);
        windows.put(VoteNode.RP03, window);
        return startResult(StartStatus.STARTED, window, Set.of());
    }

    /** Optional provenance/catch-up observation. It is deliberately never an RP03 prerequisite. */
    public void markConsequenceBookRead(UUID playerId) throws IOException {
        Objects.requireNonNull(playerId, "playerId");
        if (!progress.snapshot().isComplete("v5_rp02_configured")) {
            throw new IllegalStateException("RP02 is not complete");
        }
        progress.transact(editor -> {
            editor.addPlayerBit(playerId, PlayerBitDomain.TOPIC, CONSEQUENCE_READ_BIT);
            return null;
        });
    }

    public synchronized VoteResult cast(VoteNode node, UUID playerId, String branch)
            throws IOException {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(playerId, "playerId");
        String normalized = normalizeBranch(branch);
        Window window = windows.get(node);
        if (window == null) {
            return voteResult(VoteStatus.NO_ACTIVE_WINDOW, null, null);
        }
        if (!node.branches().contains(normalized)) {
            return voteResult(VoteStatus.INVALID_BRANCH, window, null);
        }
        if (!window.roster.contains(playerId) || window.disconnectedAt.containsKey(playerId)) {
            return voteResult(VoteStatus.NOT_ELIGIBLE, window, null);
        }
        if (window.phase == WindowPhase.TIEBREAK) {
            if (!playerId.equals(window.tiebreaker)) {
                return voteResult(VoteStatus.NOT_TIEBREAKER, window, null);
            }
            if (!window.tiedBranches.contains(normalized)) {
                return voteResult(VoteStatus.TIEBREAK_BRANCH_REQUIRED, window, null);
            }
            return resolve(window, normalized);
        }
        if (window.phase != WindowPhase.COLLECTING && window.phase != WindowPhase.REVOTE) {
            return voteResult(VoteStatus.ABORTED, window, null);
        }
        String previous = window.votes.put(playerId, normalized);
        if (normalized.equals(previous)) {
            return voteResult(VoteStatus.DUPLICATE, window, null);
        }
        VoteStatus accepted = previous == null ? VoteStatus.ACCEPTED : VoteStatus.CHANGED;
        if (window.votes.size() != window.roster.size()) {
            return voteResult(accepted, window, null);
        }
        VoteResult evaluated = evaluateCompleteRound(window);
        return evaluated.status() == VoteStatus.RESOLVED || evaluated.status() == VoteStatus.TIE_OR_REVOTE
                ? evaluated : voteResult(accepted, window, null);
    }

    public synchronized void disconnected(VoteNode node, UUID playerId) {
        Window window = windows.get(node);
        if (window != null && window.roster.contains(playerId)) {
            window.disconnectedAt.putIfAbsent(playerId, clock.tick());
        }
    }

    public synchronized void reconnected(VoteNode node, UUID playerId) {
        Window window = windows.get(node);
        if (window != null) {
            window.disconnectedAt.remove(playerId);
        }
    }

    /** Applies grace expiries and visible deadlines; never auto-selects a narrative branch. */
    public synchronized VoteResult tick(VoteNode node) throws IOException {
        Window window = windows.get(node);
        if (window == null) {
            return voteResult(VoteStatus.NO_ACTIVE_WINDOW, null, null);
        }
        long now = clock.tick();
        List<UUID> expired = window.disconnectedAt.entrySet().stream()
                .filter(entry -> now - entry.getValue() >= DISCONNECT_GRACE_TICKS)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!expired.isEmpty()) {
            freezeIfNeeded(window, currentTied(window));
            int nextRound = window.round + 1;
            int nextResnaps = window.resnaps + 1;
            journal.advanceBeforeTransition(node.name(), nextRound, nextResnaps);
            window.round = nextRound;
            window.resnaps = nextResnaps;
            expired.forEach(player -> {
                window.roster.remove(player);
                window.votes.remove(player);
                window.disconnectedAt.remove(player);
            });
            if (window.roster.isEmpty()) {
                return abort(window);
            }
            if (window.phase == WindowPhase.TIEBREAK) {
                window.tiebreaker = lowestConnected(window).orElse(null);
            }
            if ((window.phase == WindowPhase.COLLECTING || window.phase == WindowPhase.REVOTE)
                    && window.votes.size() == window.roster.size()) {
                return evaluateCompleteRound(window);
            }
        }
        if (window.phase == WindowPhase.TIEBREAK && window.tiebreaker == null) {
            window.tiebreaker = lowestConnected(window).orElse(null);
        }
        if (now >= window.closesAtTick) {
            return abort(window);
        }
        return voteResult(VoteStatus.ACCEPTED, window, null);
    }

    public synchronized Optional<WindowView> view(VoteNode node) {
        return Optional.ofNullable(windows.get(node)).map(VisibleBallotRite::viewOf);
    }

    public synchronized VoteResult cancel(VoteNode node) throws IOException {
        Window window = windows.get(node);
        return window == null ? voteResult(VoteStatus.NO_ACTIVE_WINDOW, null, null) : abort(window);
    }

    public void acknowledgeBridgeDelivery(UUID bridgeId) throws IOException {
        Objects.requireNonNull(bridgeId, "bridgeId");
        String escrowId = bridgeEscrowId(bridgeId);
        progress.transact(editor -> {
            EscrowEntry current = progress.snapshot().escrow().get(escrowId);
            if (current == null) {
                throw new IllegalStateException("no Protocol Bridge escrow " + escrowId);
            }
            if (current.status() == EscrowStatus.DELIVERY_PENDING
                    || current.status() == EscrowStatus.RETURN_PENDING) {
                editor.transitionEscrow(escrowId, current.status(), replaceEscrowStatus(
                        current, EscrowStatus.DELIVERED, clock.epochMillis()));
            }
            return null;
        });
    }

    private StartResult validateStart(VoteNode node, Set<UUID> visibleRoster) {
        if (visibleRoster == null || visibleRoster.isEmpty()
                || visibleRoster.stream().anyMatch(Objects::isNull)) {
            return startResult(StartStatus.EMPTY_ROSTER, null, Set.of());
        }
        if (progress.snapshot().isComplete(authority.completionFlag(node.name()))) {
            return startResult(StartStatus.ALREADY_COMPLETE, null, Set.of());
        }
        String prerequisite = node == VoteNode.WR05 ? "v5_wr04_bridge" : "v5_rp02_configured";
        if (!progress.snapshot().isComplete(prerequisite)) {
            return startResult(StartStatus.PREREQUISITE_MISSING, null, Set.of());
        }
        if (windows.containsKey(node)) {
            return startResult(StartStatus.BUSY, windows.get(node), Set.of());
        }
        if (journal.snapshot().containsKey(node.name())
                && !progress.snapshot().ballots().containsKey(node.name())) {
            return startResult(StartStatus.RECOVERY_REQUIRED, null, Set.of());
        }
        return startResult(StartStatus.STARTED, null, Set.of());
    }

    private VoteResult evaluateCompleteRound(Window window) throws IOException {
        Set<String> tied = currentTied(window);
        freezeIfNeeded(window, tied);
        if (tied.size() == 1) {
            return resolve(window, tied.iterator().next());
        }
        int nextRound = window.round + 1;
        journal.advanceBeforeTransition(window.node.name(), nextRound, window.resnaps);
        window.round = nextRound;
        window.tiedBranches = tied;
        if (window.node == VoteNode.WR05) {
            window.phase = WindowPhase.TIEBREAK;
            window.tiebreaker = window.disconnectedAt.containsKey(window.depositor)
                    ? null : window.depositor;
            window.closesAtTick = clock.tick() + TIEBREAK_TICKS;
        } else {
            window.phase = WindowPhase.REVOTE;
            window.tiebreaker = null;
            window.votes.clear();
            window.closesAtTick = clock.tick() + WINDOW_TICKS;
        }
        return voteResult(VoteStatus.TIE_OR_REVOTE, window, null);
    }

    private VoteResult resolve(Window window, String branch) throws IOException {
        String canonical = requireBranch(window.node, branch);
        BallotEvidenceJournal.Evidence evidence = journal.require(window.node.name());
        BallotTelemetry telemetry = evidence.telemetry();
        try (SiteMutexes.Guard ignored = acquireMutex(window.node)) {
            if (window.node == VoteNode.WR05) {
                WrenOutcome outcome = WrenOutcome.valueOf(canonical);
                ProtocolBridge returned = window.bridge.retag(outcome);
                progress.transact(editor -> {
                    editor.putBranchOnce("v5_wren_outcome", outcome.wireValue());
                    editor.putBallotOnce("WR05", telemetry);
                    editor.compareAndSetCompletion(authority.completionFlag("WR05"), false, true);
                    EscrowEntry current = progress.snapshot().escrow().get(
                            bridgeEscrowId(window.bridge.instanceId()));
                    if (current == null || current.status() != EscrowStatus.HELD) {
                        throw new IllegalStateException("Protocol Bridge escrow is not held");
                    }
                    editor.transitionEscrow(current.escrowId(), EscrowStatus.HELD,
                            replaceEscrow(current, EscrowStatus.DELIVERY_PENDING,
                                    Map.of("state", "return_branch_tagged",
                                            "v5_wren_outcome", outcome.wireValue()),
                                    clock.epochMillis()));
                    return null;
                });
                Resolution resolution = new Resolution(VoteNode.WR05, canonical, telemetry,
                        Optional.of(returned), Optional.empty());
                finish(window, WindowPhase.RESOLVED);
                return voteResult(VoteStatus.RESOLVED, window, resolution);
            }

            NameTreatment treatment = NameTreatment.valueOf(canonical);
            BallotTelemetry wr05 = progress.snapshot().ballots().get("WR05");
            String wrenOutcome = progress.snapshot().branches().get("v5_wren_outcome");
            ConductVerdict expected = ConductVerdictDeriver.derive(
                    wr05, telemetry, wrenOutcome, treatment.wireValue());
            final ConductVerdict[] committed = {null};
            progress.transact(editor -> {
                editor.putBranchOnce("v5_name_treatment", treatment.wireValue());
                editor.putBallotOnce("RP03", telemetry);
                committed[0] = editor.deriveAndSetConductVerdict();
                if (committed[0] != expected) {
                    throw new IllegalStateException("conduct derivation changed inside transaction");
                }
                editor.compareAndSetCompletion(authority.completionFlag("RP03"), false, true);
                return null;
            });
            Resolution resolution = new Resolution(VoteNode.RP03, canonical, telemetry,
                    Optional.empty(), Optional.of(committed[0]));
            finish(window, WindowPhase.RESOLVED);
            return voteResult(VoteStatus.RESOLVED, window, resolution);
        }
    }

    private VoteResult abort(Window window) throws IOException {
        if (window.node == VoteNode.WR05) {
            String escrowId = bridgeEscrowId(window.bridge.instanceId());
            progress.transact(editor -> {
                EscrowEntry current = progress.snapshot().escrow().get(escrowId);
                if (current != null && current.status() == EscrowStatus.HELD) {
                    editor.transitionEscrow(escrowId, EscrowStatus.HELD,
                            replaceEscrow(current, EscrowStatus.RETURN_PENDING,
                                    Map.of("state", "return_uncommitted"), clock.epochMillis()));
                }
                return null;
            });
        }
        finish(window, WindowPhase.ABORTED);
        return voteResult(VoteStatus.ABORTED, window, null);
    }

    private void freezeIfNeeded(Window window, Set<String> tied) throws IOException {
        if (journal.snapshot().containsKey(window.node.name())) {
            return;
        }
        journal.freezeFirstBallot(
                window.node.name(),
                window.initialRoster,
                window.votes,
                window.initialRoster.size(),
                tied.size() > 1,
                clock.epochMillis());
    }

    private void finish(Window window, WindowPhase phase) {
        window.phase = phase;
        windows.remove(window.node);
        window.lease.close();
    }

    private LeaseBook.Token acquireLease(VoteNode node) {
        return leases.tryAcquire(
                "ritual-ballot:" + node.name().toLowerCase(Locale.ROOT),
                authority.node(node.name()).siteId(),
                LEASE_DURATION).orElse(null);
    }

    private SiteMutexes.Guard acquireMutex(VoteNode node) {
        try {
            return mutexes.tryAcquire(authority.node(node.name()).siteId(), MUTEX_TIMEOUT)
                    .orElseThrow(() -> new IllegalStateException(node + " site is busy"));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted acquiring " + node + " mutex", interrupted);
        }
    }

    private static Set<String> currentTied(Window window) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        window.votes.values().forEach(value -> counts.merge(value, 1, Integer::sum));
        int maximum = counts.values().stream().max(Integer::compareTo).orElse(0);
        Set<String> result = new LinkedHashSet<>();
        counts.entrySet().stream().filter(entry -> entry.getValue() == maximum)
                .map(Map.Entry::getKey).sorted().forEach(result::add);
        return Set.copyOf(result);
    }

    private static Optional<UUID> lowestConnected(Window window) {
        return window.roster.stream()
                .filter(player -> !window.disconnectedAt.containsKey(player))
                .min(Comparator.naturalOrder());
    }

    private static LinkedHashSet<UUID> sortedRoster(Set<UUID> source) {
        LinkedHashSet<UUID> result = new LinkedHashSet<>();
        source.stream().sorted().forEach(result::add);
        return result;
    }

    private static WindowView viewOf(Window window) {
        return new WindowView(
                window.node,
                window.phase,
                new ArrayList<>(window.roster),
                window.votes.size(),
                window.round,
                window.resnaps,
                window.tiedBranches,
                Optional.ofNullable(window.tiebreaker),
                window.closesAtTick);
    }

    private static StartResult startResult(
            StartStatus status, Window window, Set<UUID> unread) {
        return new StartResult(status, Optional.ofNullable(window).map(VisibleBallotRite::viewOf), unread);
    }

    private static VoteResult voteResult(
            VoteStatus status, Window window, Resolution resolution) {
        return new VoteResult(status, Optional.ofNullable(window).map(VisibleBallotRite::viewOf),
                Optional.ofNullable(resolution));
    }

    private static String normalizeBranch(String branch) {
        return branch == null ? "" : branch.trim().toUpperCase(Locale.ROOT);
    }

    private static String requireBranch(VoteNode node, String branch) {
        String normalized = normalizeBranch(branch);
        if (!node.branches().contains(normalized)) {
            throw new IllegalArgumentException("invalid " + node + " branch " + branch);
        }
        return normalized;
    }

    private static EscrowEntry bridgeEscrow(
            ProtocolBridge bridge, UUID player, long now, EscrowStatus status,
            Map<String, String> metadata) {
        return new EscrowEntry(
                bridgeEscrowId(bridge.instanceId()),
                ProtocolBridge.ARTIFACT_VALUE,
                Optional.of(player),
                "threshold_vault",
                13,
                bridge.fingerprintSha256(),
                1,
                now,
                now,
                status,
                metadata);
    }

    private static String bridgeEscrowId(UUID instanceId) {
        return "wr05-protocol-bridge-" + instanceId;
    }

    private static EscrowEntry replaceEscrow(
            EscrowEntry current, EscrowStatus status, Map<String, String> metadata, long now) {
        return new EscrowEntry(current.escrowId(), current.artifactId(), current.intendedPlayer(),
                current.sourceSiteId(), current.sourceSlot(), current.itemFingerprintSha256(),
                current.amount(), current.createdAtEpochMillis(), Math.max(now,
                current.updatedAtEpochMillis()), status, metadata);
    }

    private static EscrowEntry replaceEscrowStatus(
            EscrowEntry current, EscrowStatus status, long now) {
        return replaceEscrow(current, status, current.metadata(), now);
    }
}
