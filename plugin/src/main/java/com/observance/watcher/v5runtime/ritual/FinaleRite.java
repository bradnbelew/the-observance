package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.BallotTelemetry;
import com.observance.watcher.v5runtime.ConductVerdict;
import com.observance.watcher.v5runtime.ConductVerdictDeriver;
import com.observance.watcher.v5runtime.LeaseBook;
import com.observance.watcher.v5runtime.ProgressSnapshot;
import com.observance.watcher.v5runtime.SiteMutexes;
import com.observance.watcher.v5runtime.V5ProgressStore;
import com.observance.watcher.v5runtime.ritual.FinaleStateStore.Phase;
import com.observance.watcher.v5runtime.ritual.RitualChoices.EndingDimensions;
import com.observance.watcher.v5runtime.ritual.RitualChoices.NameTreatment;
import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenOutcome;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** RP05 branchless arm/player sever and RP06 durable, monotonic closing theater. */
public final class FinaleRite {
    public static final int DEFAULT_ARM_SECONDS = 120;
    public static final int MIN_ARM_SECONDS = 15;
    public static final int MAX_ARM_SECONDS = 600;
    public static final long REQUIRED_CROUCH_TICKS = 60L;

    public enum ArmStatus {
        ARMED,
        CANCELLED,
        EXPIRED,
        PREREQUISITE_MISSING,
        INVALID_DIMENSIONS,
        INVALID_WINDOW,
        ALREADY_ACTIVE,
        ALREADY_COMMITTED,
        FAULT
    }

    public enum ConfirmStatus {
        COMMITTED,
        NOT_ARMED,
        EXPIRED,
        INVALID_PLAYER_PROOF,
        PREREQUISITE_MISSING,
        ALREADY_COMMITTED,
        PERSISTENCE_FAILED,
        FAULT
    }

    public interface FinaleEffects {
        void darken(String idempotencyKey) throws Exception;

        void syntaxBreak(String idempotencyKey) throws Exception;

        void goodbye(String idempotencyKey, List<String> exactLines) throws Exception;

        void savePlayersAndWorlds(String idempotencyKey) throws Exception;

        void kickPlayers(String idempotencyKey, List<String> exactLines) throws Exception;

        void requestProductionShutdown(String idempotencyKey) throws Exception;
    }

    public record ArmResult(ArmStatus status, String detail, Optional<EndingDimensions> dimensions,
                            long cancelCutoffAt) {
        public ArmResult {
            Objects.requireNonNull(status, "status");
            detail = detail == null ? "" : detail;
            dimensions = Objects.requireNonNull(dimensions, "dimensions");
        }
    }

    public record ConfirmResult(ConfirmStatus status, String detail) {
        public ConfirmResult {
            Objects.requireNonNull(status, "status");
            detail = detail == null ? "" : detail;
        }
    }

    public record PlayerCommitProof(
            UUID playerId,
            boolean realOnlinePlayer,
            boolean linked,
            boolean spectator,
            boolean atExactConfirmCell,
            boolean operatedExactPdcControl,
            boolean continuouslyCrouching,
            long continuousCrouchTicks) {
        public PlayerCommitProof {
            Objects.requireNonNull(playerId, "playerId");
            if (continuousCrouchTicks < 0) {
                throw new IllegalArgumentException("crouch ticks cannot be negative");
            }
        }

        public boolean exact() {
            return realOnlinePlayer && linked && !spectator && atExactConfirmCell
                    && operatedExactPdcControl && continuouslyCrouching
                    && continuousCrouchTicks >= REQUIRED_CROUCH_TICKS;
        }
    }

    public record CodaReceipt(String key, List<String> exactGoodbye) {
        public CodaReceipt {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Coda key cannot be blank");
            }
            exactGoodbye = List.copyOf(exactGoodbye);
            if (exactGoodbye.isEmpty()) {
                throw new IllegalArgumentException("Coda goodbye cannot be empty");
            }
        }
    }

    private static final String FINALE_SCHEMA_KEY = "v5_finale_schema";
    private static final String FINALE_PHASE_AT_COMMIT_KEY = "v5_finale_phase_at_commit";
    private static final String FINALE_WREN_KEY = "v5_finale_wren_outcome";
    private static final String FINALE_NAME_KEY = "v5_finale_name_treatment";
    private static final String FINALE_CONDUCT_KEY = "v5_finale_conduct_verdict";
    private static final String FINALE_ARMED_BY_KEY = "v5_finale_armed_by";
    private static final String FINALE_ARMED_AT_KEY = "v5_finale_armed_at";
    private static final String FINALE_CUTOFF_KEY = "v5_finale_cancel_cutoff_at";
    private static final String FINALE_COMMITTED_BY_KEY = "v5_finale_committed_by";
    private static final String FINALE_COMMITTED_AT_KEY = "v5_finale_committed_at";
    private static final String FINALE_TERMINAL_PHASE_KEY = "v5_finale_terminal_phase";
    private static final String FINALE_CODA_KEY = "v5_finale_coda_key";
    private static final Set<String> RETIRED_INPUT_FRAGMENTS = Set.of(
            "ending_fate", "customs", "bow_window", "compliance_score");
    private static final Duration MUTEX_TIMEOUT = Duration.ofSeconds(3);

    private final RitualAuthorityContract authority;
    private final CanonicalRitualText text;
    private final V5ProgressStore progress;
    private final FinaleStateStore state;
    private final LeaseBook leases;
    private final SiteMutexes mutexes;
    private final RitualClock clock;
    private LeaseBook.Token armLease;

    public FinaleRite(
            RitualAuthorityContract authority,
            CanonicalRitualText text,
            V5ProgressStore progress,
            FinaleStateStore state,
            LeaseBook leases,
            SiteMutexes mutexes,
            RitualClock clock) throws IOException {
        this.authority = Objects.requireNonNull(authority, "authority");
        this.text = Objects.requireNonNull(text, "text");
        this.progress = Objects.requireNonNull(progress, "progress");
        this.state = Objects.requireNonNull(state, "state");
        this.leases = Objects.requireNonNull(leases, "leases");
        this.mutexes = Objects.requireNonNull(mutexes, "mutexes");
        this.clock = Objects.requireNonNull(clock, "clock");
        authority.node("RP05");
        authority.node("RP06");
        reconcileOnOpen();
    }

    /** Branchless API: branches are read exclusively from committed local player choices. */
    public synchronized ArmResult arm(String operator, Integer requestedSeconds) throws IOException {
        if (state.snapshot().phase() == Phase.FAULT) {
            return armResult(ArmStatus.FAULT, state.snapshot().faultReason(), 0);
        }
        if (progress.snapshot().isComplete(authority.completionFlag("RP05"))) {
            return armResult(ArmStatus.ALREADY_COMMITTED, "RP05 is already committed", 0);
        }
        if (state.snapshot().phase() != Phase.IDLE) {
            return armResult(ArmStatus.ALREADY_ACTIVE, "finale phase is " + state.snapshot().phase(),
                    state.snapshot().cancelCutoffAt());
        }
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("operator identity is required");
        }
        int seconds = requestedSeconds == null ? DEFAULT_ARM_SECONDS : requestedSeconds;
        if (seconds < MIN_ARM_SECONDS || seconds > MAX_ARM_SECONDS) {
            return armResult(ArmStatus.INVALID_WINDOW,
                    "arm window must be 15-600 seconds", 0);
        }
        Set<String> missing = missingLocalChain();
        if (!missing.isEmpty()) {
            return armResult(ArmStatus.PREREQUISITE_MISSING,
                    "missing local receipts " + missing, 0);
        }
        EndingDimensions dimensions;
        try {
            dimensions = exactDimensions(progress.snapshot());
        } catch (RuntimeException failure) {
            return armResult(ArmStatus.INVALID_DIMENSIONS, failure.getMessage(), 0);
        }
        long now = clock.epochMillis();
        long cutoff = Math.addExact(now, Math.multiplyExact((long) seconds, 1_000L));
        try (SiteMutexes.Guard ignored = acquireMutex()) {
            armLease = leases.tryAcquire("finale:rp05", operator,
                    Duration.ofSeconds((long) seconds + 30L)).orElse(null);
            if (armLease == null) {
                return armResult(ArmStatus.ALREADY_ACTIVE, "another finale arm lease is active", 0);
            }
            try {
                state.arm(dimensions, operator, now, cutoff);
            } catch (IOException | RuntimeException failure) {
                releaseArmLease();
                throw failure;
            }
        }
        return armResult(ArmStatus.ARMED, "awaiting one exact player sever confirmation", cutoff);
    }

    public synchronized ArmResult cancel() throws IOException {
        if (state.snapshot().phase() == Phase.FAULT) {
            return armResult(ArmStatus.FAULT, state.snapshot().faultReason(), 0);
        }
        if (state.snapshot().phase() != Phase.ARMED) {
            return armResult(progress.snapshot().isComplete(authority.completionFlag("RP05"))
                    ? ArmStatus.ALREADY_COMMITTED : ArmStatus.ALREADY_ACTIVE,
                    "only ARMED can be cancelled", state.snapshot().cancelCutoffAt());
        }
        state.cancelOrExpire();
        releaseArmLease();
        return armResult(ArmStatus.CANCELLED, "arm cancelled before commitment", 0);
    }

    public synchronized ArmResult expireIfNeeded() throws IOException {
        if (state.snapshot().phase() != Phase.ARMED
                || clock.epochMillis() < state.snapshot().cancelCutoffAt()) {
            return armResult(ArmStatus.ALREADY_ACTIVE, "arm has not expired",
                    state.snapshot().cancelCutoffAt());
        }
        state.cancelOrExpire();
        releaseArmLease();
        return armResult(ArmStatus.EXPIRED, "arm expired fail-closed to IDLE", 0);
    }

    public synchronized ConfirmResult confirm(PlayerCommitProof proof) {
        Objects.requireNonNull(proof, "proof");
        if (state.snapshot().phase() == Phase.FAULT) {
            return new ConfirmResult(ConfirmStatus.FAULT, state.snapshot().faultReason());
        }
        if (progress.snapshot().isComplete(authority.completionFlag("RP05"))) {
            return new ConfirmResult(ConfirmStatus.ALREADY_COMMITTED, "first confirmation already won");
        }
        if (state.snapshot().phase() != Phase.ARMED) {
            return new ConfirmResult(ConfirmStatus.NOT_ARMED, "operator arm is not active");
        }
        long now = clock.epochMillis();
        if (now >= state.snapshot().cancelCutoffAt()) {
            try {
                state.cancelOrExpire();
                releaseArmLease();
            } catch (IOException failure) {
                return new ConfirmResult(ConfirmStatus.PERSISTENCE_FAILED,
                        "expired arm could not be closed: " + failure.getMessage());
            }
            return new ConfirmResult(ConfirmStatus.EXPIRED, "confirmation reached the cutoff");
        }
        if (!proof.exact()) {
            return new ConfirmResult(ConfirmStatus.INVALID_PLAYER_PROOF,
                    "requires one linked online nonspectator player, exact cell/control, and 60 crouch ticks");
        }
        Set<String> missing = missingLocalChain();
        if (!missing.isEmpty()) {
            return new ConfirmResult(ConfirmStatus.PREREQUISITE_MISSING,
                    "missing local receipts " + missing);
        }
        FinaleStateStore.Snapshot armed = state.snapshot();
        try (SiteMutexes.Guard ignored = acquireMutex()) {
            final boolean[] won = {false};
            progress.transact(editor -> {
                editor.putBranchOnce(FINALE_SCHEMA_KEY, Integer.toString(FinaleStateStore.SCHEMA_VERSION));
                editor.putBranchOnce(FINALE_PHASE_AT_COMMIT_KEY, Phase.COMMITTED.name());
                editor.putBranchOnce(FINALE_WREN_KEY, armed.wrenOutcome());
                editor.putBranchOnce(FINALE_NAME_KEY, armed.nameTreatment());
                editor.putBranchOnce(FINALE_CONDUCT_KEY, armed.conductVerdict());
                editor.putBranchOnce(FINALE_ARMED_BY_KEY, armed.armedBy());
                editor.putBranchOnce(FINALE_ARMED_AT_KEY, Long.toString(armed.armedAt()));
                editor.putBranchOnce(FINALE_CUTOFF_KEY, Long.toString(armed.cancelCutoffAt()));
                editor.putBranchOnce(FINALE_COMMITTED_BY_KEY, proof.playerId().toString());
                editor.putBranchOnce(FINALE_COMMITTED_AT_KEY, Long.toString(now));
                won[0] = editor.compareAndSetCompletion(
                        authority.completionFlag("RP05"), false, true);
                return null;
            });
            if (!won[0]) {
                return new ConfirmResult(ConfirmStatus.ALREADY_COMMITTED,
                        "another player confirmation already committed");
            }
            state.commitPlayer(proof.playerId(), now);
            releaseArmLease();
            return new ConfirmResult(ConfirmStatus.COMMITTED,
                    "RP05 committed locally before theater");
        } catch (IOException | RuntimeException failure) {
            return new ConfirmResult(ConfirmStatus.PERSISTENCE_FAILED,
                    "no theater started: " + failure.getMessage());
        }
    }

    /** Advances exactly one durable RP06 phase and repairs a missing effect for that phase. */
    public synchronized Phase resumeOnePhase(FinaleEffects effects) throws Exception {
        Objects.requireNonNull(effects, "effects");
        reconcileOnOpen();
        Phase phase = state.snapshot().phase();
        if (phase == Phase.FAULT || phase == Phase.IDLE || phase == Phase.ARMED) {
            throw new IllegalStateException("RP06 cannot resume from " + phase);
        }
        CanonicalRitualText.EndingText ending = text.ending(state.snapshot().dimensions());
        return switch (phase) {
            case COMMITTED -> {
                state.transition(Phase.COMMITTED, Phase.DARKENING);
                applyOnce("rp06.darkening", () -> effects.darken("rp06.darkening"));
                yield Phase.DARKENING;
            }
            case DARKENING -> {
                applyOnce("rp06.darkening", () -> effects.darken("rp06.darkening"));
                state.transition(Phase.DARKENING, Phase.SYNTAX_BREAK);
                applyOnce("rp06.syntax_break", () -> effects.syntaxBreak("rp06.syntax_break"));
                yield Phase.SYNTAX_BREAK;
            }
            case SYNTAX_BREAK -> {
                applyOnce("rp06.syntax_break", () -> effects.syntaxBreak("rp06.syntax_break"));
                state.transition(Phase.SYNTAX_BREAK, Phase.GOODBYE);
                applyOnce("rp06.goodbye", () -> effects.goodbye(
                        "rp06.goodbye", ending.completeGoodbye()));
                yield Phase.GOODBYE;
            }
            case GOODBYE -> {
                applyOnce("rp06.goodbye", () -> effects.goodbye(
                        "rp06.goodbye", ending.completeGoodbye()));
                commitCodaReceipt(ending);
                state.transition(Phase.GOODBYE, Phase.SAVE_AND_CODA);
                runTerminalEffects(effects, ending);
                yield Phase.SAVE_AND_CODA;
            }
            case SAVE_AND_CODA -> {
                commitCodaReceipt(ending);
                runTerminalEffects(effects, ending);
                state.transition(Phase.SAVE_AND_CODA, Phase.CODA);
                yield Phase.CODA;
            }
            case CODA -> Phase.CODA;
            default -> throw new IllegalStateException("unhandled RP06 phase " + phase);
        };
    }

    public synchronized long recommendedDelayTicksBeforeNextPhase() {
        // These gaps size the finale as an EVENT the group watches, not an 8-second blip. Each value
        // is the pause before the NEXT phase's effect fires, so it must cover the theater the current
        // phase started: DARKENING schedules a ~24s light-death wave; GOODBYE drips its lines and must
        // finish before SAVE_AND_CODA kicks everyone. Durable + idempotent either way (applyOnce).
        return switch (state.snapshot().phase()) {
            case COMMITTED -> 0L;
            case DARKENING -> 520L;      // ~26s: let the light-death wave finish before the record breaks
            case SYNTAX_BREAK -> 140L;   // ~7s: the break lands, then the goodbye begins
            case GOODBYE -> 760L;        // ~38s: let the dripped goodbye play in full before the kick
            case SAVE_AND_CODA, CODA -> 0L;
            default -> throw new IllegalStateException(
                    "no RP06 schedule for " + state.snapshot().phase());
        };
    }

    public synchronized Optional<CodaReceipt> codaReceipt() {
        if (state.snapshot().phase() != Phase.CODA
                && !progress.snapshot().isComplete(authority.completionFlag("RP06"))) {
            return Optional.empty();
        }
        CanonicalRitualText.EndingText ending = text.ending(state.snapshot().dimensions());
        return Optional.of(new CodaReceipt(ending.codaKey(), ending.completeGoodbye()));
    }

    public FinaleStateStore.Snapshot snapshot() {
        return state.snapshot();
    }

    private void runTerminalEffects(FinaleEffects effects, CanonicalRitualText.EndingText ending)
            throws Exception {
        applyOnce("rp06.save", () -> effects.savePlayersAndWorlds("rp06.save"));
        applyOnce("rp06.kick", () -> effects.kickPlayers(
                "rp06.kick", ending.completeGoodbye()));
        applyOnce("rp06.production_shutdown",
                () -> effects.requestProductionShutdown("rp06.production_shutdown"));
    }

    private void commitCodaReceipt(CanonicalRitualText.EndingText ending) throws IOException {
        EndingDimensions dimensions = state.snapshot().dimensions();
        ProgressSnapshot before = progress.snapshot();
        if (before.isComplete(authority.completionFlag("RP06"))) {
            if (!ending.codaKey().equals(before.branches().get(FINALE_CODA_KEY))
                    || !Phase.CODA.name().equals(before.branches().get(FINALE_TERMINAL_PHASE_KEY))) {
                throw new IllegalStateException("durable Coda receipt does not match ending dimensions");
            }
            return;
        }
        progress.transact(editor -> {
            editor.putBranchOnce(FINALE_TERMINAL_PHASE_KEY, Phase.CODA.name());
            editor.putBranchOnce(FINALE_CODA_KEY, ending.codaKey());
            editor.setBooleanTrue("v5_finale_phase_save_and_coda");
            editor.putBranchOnce(FINALE_WREN_KEY, dimensions.wrenOutcome().wireValue());
            editor.putBranchOnce(FINALE_NAME_KEY, dimensions.nameTreatment().wireValue());
            editor.putBranchOnce(FINALE_CONDUCT_KEY, dimensions.conductVerdict().wireValue());
            editor.compareAndSetCompletion(authority.completionFlag("RP06"), false, true);
            return null;
        });
    }

    private void applyOnce(String effectKey, Effect effect) throws Exception {
        if (state.snapshot().completedEffects().contains(effectKey)) {
            return;
        }
        effect.run();
        state.markEffectComplete(effectKey);
    }

    private EndingDimensions exactDimensions(ProgressSnapshot snapshot) {
        for (String key : snapshot.branches().keySet()) {
            String lower = key.toLowerCase(Locale.ROOT);
            if (RETIRED_INPUT_FRAGMENTS.stream().anyMatch(lower::contains)) {
                throw new IllegalStateException("retired finale input present: " + key);
            }
        }
        WrenOutcome wren = WrenOutcome.fromWireValue(
                require(snapshot.branches(), "v5_wren_outcome"));
        NameTreatment name = NameTreatment.fromWireValue(
                require(snapshot.branches(), "v5_name_treatment"));
        ConductVerdict conduct = snapshot.conductVerdict().orElseThrow(
                () -> new IllegalStateException("v5_conduct_verdict is absent"));
        BallotTelemetry wr05 = Objects.requireNonNull(snapshot.ballots().get("WR05"),
                "WR05 ballot is absent");
        BallotTelemetry rp03 = Objects.requireNonNull(snapshot.ballots().get("RP03"),
                "RP03 ballot is absent");
        ConductVerdict derived = ConductVerdictDeriver.derive(
                wr05, rp03, wren.wireValue(), name.wireValue());
        if (conduct != derived) {
            throw new IllegalStateException("conduct does not match immutable first ballots");
        }
        return new EndingDimensions(wren, name, conduct);
    }

    private Set<String> missingLocalChain() {
        Set<String> required = new java.util.LinkedHashSet<>(
                authority.prerequisiteChainThrough("RP04"));
        required.add(authority.completionFlag("RP04"));
        Set<String> missing = new java.util.LinkedHashSet<>();
        for (String flag : required) {
            if (!progress.snapshot().isComplete(flag)) {
                missing.add(flag);
            }
        }
        return Set.copyOf(missing);
    }

    private void reconcileOnOpen() throws IOException {
        if (state.snapshot().phase() == Phase.FAULT) {
            return;
        }
        boolean rp05 = progress.snapshot().isComplete(authority.completionFlag("RP05"));
        if (!rp05) {
            if (state.snapshot().phase().ordinal() >= Phase.COMMITTED.ordinal()) {
                throw new IllegalStateException("finale phase exists without durable RP05 receipt");
            }
            if (state.snapshot().phase() == Phase.ARMED
                    && clock.epochMillis() >= state.snapshot().cancelCutoffAt()) {
                state.cancelOrExpire();
            }
            return;
        }
        if (state.snapshot().phase().ordinal() < Phase.COMMITTED.ordinal()) {
            ProgressSnapshot snapshot = progress.snapshot();
            EndingDimensions dimensions = exactCommittedDimensions(snapshot);
            state.recoverCommitted(
                    dimensions,
                    require(snapshot.branches(), FINALE_ARMED_BY_KEY),
                    parseLong(snapshot.branches(), FINALE_ARMED_AT_KEY),
                    parseLong(snapshot.branches(), FINALE_CUTOFF_KEY),
                    require(snapshot.branches(), FINALE_COMMITTED_BY_KEY),
                    parseLong(snapshot.branches(), FINALE_COMMITTED_AT_KEY));
        }
        if (state.snapshot().phase() == Phase.CODA
                && !progress.snapshot().isComplete(authority.completionFlag("RP06"))) {
            throw new IllegalStateException("CODA phase exists without durable C10 receipt");
        }
    }

    private static EndingDimensions exactCommittedDimensions(ProgressSnapshot snapshot) {
        if (!Integer.toString(FinaleStateStore.SCHEMA_VERSION).equals(
                snapshot.branches().get(FINALE_SCHEMA_KEY))
                || !Phase.COMMITTED.name().equals(
                snapshot.branches().get(FINALE_PHASE_AT_COMMIT_KEY))) {
            throw new IllegalStateException("durable finale schema-2 commit is incomplete");
        }
        return new EndingDimensions(
                WrenOutcome.fromWireValue(require(snapshot.branches(), FINALE_WREN_KEY)),
                NameTreatment.fromWireValue(require(snapshot.branches(), FINALE_NAME_KEY)),
                ConductVerdict.fromWireValue(require(snapshot.branches(), FINALE_CONDUCT_KEY)));
    }

    private ArmResult armResult(ArmStatus status, String detail, long cutoff) {
        return new ArmResult(status, detail, state.snapshot().optionalDimensions(), cutoff);
    }

    private SiteMutexes.Guard acquireMutex() {
        try {
            return mutexes.tryAcquire(authority.node("RP05").siteId(), MUTEX_TIMEOUT)
                    .orElseThrow(() -> new IllegalStateException("global finale mutex is busy"));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted acquiring finale mutex", interrupted);
        }
    }

    private void releaseArmLease() {
        if (armLease != null) {
            armLease.close();
            armLease = null;
        }
    }

    private static String require(Map<String, String> source, String key) {
        String value = source.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing durable finale field " + key);
        }
        return value;
    }

    private static long parseLong(Map<String, String> source, String key) {
        try {
            return Long.parseLong(require(source, key));
        } catch (NumberFormatException failure) {
            throw new IllegalStateException("invalid durable finale timestamp " + key, failure);
        }
    }

    @FunctionalInterface
    private interface Effect {
        void run() throws Exception;
    }
}
