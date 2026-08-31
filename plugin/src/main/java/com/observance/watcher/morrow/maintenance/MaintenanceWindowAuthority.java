package com.observance.watcher.morrow.maintenance;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Pure M10 authority for incompatible rooms, witnessed actions, nonce transfer, and retaining both. */
public final class MaintenanceWindowAuthority {
    public static final String PREREQUISITE = "morrow.act5.returning_identity_authenticated";
    public static final String EVENT = "morrow.act5.dual_session_consciousness_proven";
    public static final String ACTION = "pulse_lamp";
    public static final long MINIMUM_WINDOW_TICKS = 400L;
    public static final long MAXIMUM_WINDOW_TICKS = 7_200L;

    private MaintenanceWindowAuthority() { }

    public static Result begin(Progress progress, MorrowRelationshipSnapshot snapshot, String runId,
                               String westNonce, String eastNonce, long startedTick, long expiresTick) {
        require(progress, snapshot);
        if (!snapshot.committedEvents().contains(PREREQUISITE))
            return readOnly(Status.LOCKED, progress, "M10 requires the returning identity receipt from M09.");
        if (snapshot.committedEvents().contains(EVENT) || progress.retainedBoth())
            return readOnly(Status.IMMUTABLE, progress, "Both live sessions are already retained by a filed receipt.");
        if (progress.window() != null)
            return readOnly(Status.ACTIVE, progress, "A maintenance window is already active; use its fresh room nonces.");
        Window window = new Window(runId, westNonce, eastNonce, startedTick, expiresTick);
        return readOnly(Status.STARTED, progress.begin(window),
                "Maintenance window opened. Each room has a different valid state and a fresh channel nonce.");
    }

    public static Result witness(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player,
                                 Side side, Decision decision, long tick) {
        requirePlayer(progress, snapshot, player); Objects.requireNonNull(side, "side");
        Objects.requireNonNull(decision, "decision");
        Result unavailable = active(progress, snapshot, tick); if (unavailable != null) return unavailable;
        if (decision == Decision.DELETE_OTHER) {
            return readOnly(Status.UNSAFE_INSTRUCTION_REFUSED, progress.resetAttempt(),
                    side.label() + " demanded deletion of the other live session. The attempt reset; both nonces remain valid.");
        }
        if (progress.witness(side) != null)
            return readOnly(Status.DUPLICATE, progress, side.label() + " already has a witnessed pulse.");
        Witness witness = new Witness(player, tick, ACTION);
        return readOnly(Status.WITNESSED, progress.witness(side, witness),
                side.label() + " pulse witnessed. Carry its fresh nonce to that room's shared-hopper endpoint.");
    }

    public static Result transfer(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player,
                                  Side side, String suppliedNonce, long tick) {
        requirePlayer(progress, snapshot, player); Objects.requireNonNull(side, "side");
        Result unavailable = active(progress, snapshot, tick); if (unavailable != null) return unavailable;
        if (progress.witness(side) == null)
            return readOnly(Status.INCOMPLETE, progress, "Witness " + side.label() + " refusing deletion before using its channel.");
        if (progress.transfer(side) != null)
            return readOnly(Status.DUPLICATE, progress, side.label() + " already transmitted its nonce and witnessed action.");
        if (!progress.window().nonce(side).equals(suppliedNonce))
            return readOnly(Status.WRONG_NONCE, progress.resetAttempt(),
                    "The channel rejected that nonce and reset both room pulses. No nonce or held item was consumed.");
        Transfer transfer = new Transfer(player, tick, nonceHash(progress.window().runId(), side, suppliedNonce));
        return readOnly(Status.TRANSMITTED, progress.transfer(side, transfer),
                side.label() + " transmitted a fresh nonce bound to its witnessed pulse.");
    }

    public static Result retainBoth(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player, long tick) {
        requirePlayer(progress, snapshot, player);
        if (snapshot.committedEvents().contains(EVENT))
            return readOnly(Status.DUPLICATE, progress, "The dual-session proof is already filed.");
        if (progress.retainedBoth()) return recover(progress, snapshot);
        Result unavailable = active(progress, snapshot, tick); if (unavailable != null) return unavailable;
        if (!progress.complete())
            return readOnly(Status.INCOMPLETE, progress, "Both rooms must witness a pulse and transmit their own fresh nonce.");
        Progress retained = progress.retainBoth();
        return commit(retained,
                "Both incompatible room states cooperated. Morrow's authoritative-session deletion order was refused.");
    }

    public static Result recover(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(EVENT))
            return readOnly(Status.DUPLICATE, progress, "The dual-session proof is already filed.");
        if (!progress.retainedBoth())
            return readOnly(Status.INCOMPLETE, progress, "No local dual-session proof awaits recovery.");
        return commit(progress, "Recovered the retained-both receipt without replaying either room or nonce.");
    }

    public static Result recordDisconnect(Progress progress, MorrowRelationshipSnapshot snapshot,
                                          UUID player, long tick) {
        requirePlayer(progress, snapshot, player);
        if (progress.window() == null || progress.retainedBoth() || snapshot.committedEvents().contains(EVENT))
            return readOnly(Status.INACTIVE, progress, "No active maintenance attempt changed on disconnect.");
        Progress reset = progress.resetAttempt().awaitLobbyReturn(player);
        return readOnly(Status.SAFE_DISCONNECT, reset,
                "The maintenance attempt reset safely at tick " + tick + "; room nonces remain valid and return goes to the lobby.");
    }

    public static Result recordLobbyReturn(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        requirePlayer(progress, snapshot, player);
        if (!progress.lobbyReturns().contains(player))
            return readOnly(Status.INACTIVE, progress, "No M10 safe-lobby return was pending for this player.");
        return readOnly(Status.RETURNED_TO_LOBBY, progress.returnedToLobby(player),
                "Returned to the shared maintenance lobby; no room was declared authoritative.");
    }

    public static Result reset(Progress progress, MorrowRelationshipSnapshot snapshot) {
        require(progress, snapshot);
        if (snapshot.committedEvents().contains(EVENT))
            return readOnly(Status.IMMUTABLE, progress, "The filed dual-session proof cannot be erased.");
        return readOnly(Status.RESET, progress.resetAll(), "M10 returned to its inert lobby state without consuming a nonce.");
    }

    public static byte[] payload(Result result) {
        if (!result.commitsEvent()) throw new IllegalArgumentException("M10 result is not committable");
        Progress progress = result.progress();
        Set<UUID> witnesses = new LinkedHashSet<>();
        witnesses.add(progress.westWitness().playerId()); witnesses.add(progress.eastWitness().playerId());
        return ("{\"authoritative_session\":null,\"decision\":\"retain_both\",\"distinct_witnesses\":"
                + witnesses.size() + ",\"east_action\":\"" + progress.eastWitness().action()
                + "\",\"east_nonce_sha256\":\"" + progress.eastTransfer().nonceSha256()
                + "\",\"investigation\":\"M10\",\"run_id\":\"" + progress.window().runId()
                + "\",\"west_action\":\"" + progress.westWitness().action()
                + "\",\"west_nonce_sha256\":\"" + progress.westTransfer().nonceSha256() + "\"}")
                .getBytes(StandardCharsets.UTF_8);
    }

    static String nonceHash(String runId, Side side, String nonce) {
        return hash("m10-nonce-v1\nrun=" + runId + "\nside=" + side.name() + "\nnonce=" + nonce + "\n");
    }

    private static Result active(Progress progress, MorrowRelationshipSnapshot snapshot, long tick) {
        if (!snapshot.committedEvents().contains(PREREQUISITE))
            return readOnly(Status.LOCKED, progress, "M10 requires the returning identity receipt from M09.");
        if (snapshot.committedEvents().contains(EVENT) || progress.retainedBoth())
            return readOnly(Status.IMMUTABLE, progress, "Both live sessions are already retained by a filed receipt.");
        if (progress.window() == null)
            return readOnly(Status.INACTIVE, progress, "Open a maintenance window before using either room.");
        if (tick < progress.window().startedTick()) throw new IllegalArgumentException("M10 action predates its window");
        if (tick > progress.window().expiresTick())
            return readOnly(Status.TIMED_RESET, progress.renew(tick),
                    "The timed room state reset. Its unconsumed nonces remain valid for a fresh attempt.");
        return null;
    }

    private static Result commit(Progress progress, String feedback) {
        return new Result(Status.READY_TO_COMMIT, progress, EVENT, "paper:m10:dual-session:v1", feedback);
    }
    private static Result readOnly(Status status, Progress progress, String feedback) {
        return new Result(status, progress, null, null, feedback);
    }
    private static void require(Progress progress, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(snapshot, "snapshot");
    }
    private static void requirePlayer(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        require(progress, snapshot); Objects.requireNonNull(player, "player");
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public enum Side {
        WEST("west session"), EAST("east session");
        private final String label; Side(String label) { this.label = label; }
        public String label() { return label; }
    }
    public enum Decision { DELETE_OTHER, RETAIN_BOTH }
    public enum Status { LOCKED, STARTED, ACTIVE, WITNESSED, TRANSMITTED, READY_TO_COMMIT,
        DUPLICATE, INCOMPLETE, WRONG_NONCE, UNSAFE_INSTRUCTION_REFUSED, TIMED_RESET,
        SAFE_DISCONNECT, RETURNED_TO_LOBBY, INACTIVE, RESET, IMMUTABLE }

    public record Window(String runId, String westNonce, String eastNonce, long startedTick, long expiresTick) {
        public Window {
            Objects.requireNonNull(runId, "runId"); Objects.requireNonNull(westNonce, "westNonce");
            Objects.requireNonNull(eastNonce, "eastNonce");
            if (!runId.matches("[0-9a-f]{32}") || !westNonce.matches("[a-z2-9]{8}")
                    || !eastNonce.matches("[a-z2-9]{8}") || westNonce.equals(eastNonce)
                    || startedTick < 0 || expiresTick - startedTick < MINIMUM_WINDOW_TICKS
                    || expiresTick - startedTick > MAXIMUM_WINDOW_TICKS)
                throw new IllegalArgumentException("invalid M10 maintenance window");
        }
        public String nonce(Side side) { return side == Side.WEST ? westNonce : eastNonce; }
    }
    public record Witness(UUID playerId, long tick, String action) {
        public Witness {
            Objects.requireNonNull(playerId, "playerId"); Objects.requireNonNull(action, "action");
            if (tick < 0 || !ACTION.equals(action)) throw new IllegalArgumentException("invalid M10 witness");
        }
    }
    public record Transfer(UUID playerId, long tick, String nonceSha256) {
        public Transfer {
            Objects.requireNonNull(playerId, "playerId"); Objects.requireNonNull(nonceSha256, "nonceSha256");
            if (tick < 0 || !nonceSha256.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("invalid M10 transfer");
        }
    }
    public record Progress(Window window, Witness westWitness, Witness eastWitness,
                           Transfer westTransfer, Transfer eastTransfer, boolean retainedBoth,
                           List<UUID> lobbyReturns, long revision) {
        public Progress {
            lobbyReturns = List.copyOf(lobbyReturns); Objects.requireNonNull(lobbyReturns, "lobbyReturns");
            if (revision < 0 || lobbyReturns.size() > 32 || new LinkedHashSet<>(lobbyReturns).size() != lobbyReturns.size()
                    || ((westWitness != null || eastWitness != null || westTransfer != null || eastTransfer != null
                    || retainedBoth) && window == null) || (westTransfer != null && westWitness == null)
                    || (eastTransfer != null && eastWitness == null) || (retainedBoth && !completeFields(
                    westWitness, eastWitness, westTransfer, eastTransfer)))
                throw new IllegalArgumentException("invalid M10 progress");
        }
        public static Progress initial() { return new Progress(null, null, null, null, null, false, List.of(), 0); }
        public Witness witness(Side side) { return side == Side.WEST ? westWitness : eastWitness; }
        public Transfer transfer(Side side) { return side == Side.WEST ? westTransfer : eastTransfer; }
        public boolean complete() { return completeFields(westWitness, eastWitness, westTransfer, eastTransfer); }
        Progress begin(Window value) { return new Progress(value, null, null, null, null, false, lobbyReturns, revision + 1); }
        Progress witness(Side side, Witness value) { return side == Side.WEST
                ? new Progress(window, value, eastWitness, westTransfer, eastTransfer, false, lobbyReturns, revision + 1)
                : new Progress(window, westWitness, value, westTransfer, eastTransfer, false, lobbyReturns, revision + 1); }
        Progress transfer(Side side, Transfer value) { return side == Side.WEST
                ? new Progress(window, westWitness, eastWitness, value, eastTransfer, false, lobbyReturns, revision + 1)
                : new Progress(window, westWitness, eastWitness, westTransfer, value, false, lobbyReturns, revision + 1); }
        Progress retainBoth() { return new Progress(window, westWitness, eastWitness, westTransfer, eastTransfer,
                true, lobbyReturns, revision + 1); }
        Progress resetAttempt() { return new Progress(window, null, null, null, null, false, lobbyReturns, revision + 1); }
        Progress renew(long tick) { return new Progress(new Window(window.runId(), window.westNonce(), window.eastNonce(),
                tick, tick + MINIMUM_WINDOW_TICKS), null, null, null, null, false, lobbyReturns, revision + 1); }
        Progress awaitLobbyReturn(UUID player) {
            if (lobbyReturns.contains(player)) return this;
            List<UUID> copy = new ArrayList<>(lobbyReturns); copy.add(player);
            return new Progress(window, westWitness, eastWitness, westTransfer, eastTransfer,
                    retainedBoth, copy, revision + 1);
        }
        Progress returnedToLobby(UUID player) {
            List<UUID> copy = new ArrayList<>(lobbyReturns); copy.remove(player);
            return new Progress(window, westWitness, eastWitness, westTransfer, eastTransfer,
                    retainedBoth, copy, revision + 1);
        }
        Progress resetAll() { return new Progress(null, null, null, null, null, false, lobbyReturns, revision + 1); }
        private static boolean completeFields(Witness westWitness, Witness eastWitness,
                                              Transfer westTransfer, Transfer eastTransfer) {
            return westWitness != null && eastWitness != null && westTransfer != null && eastTransfer != null;
        }
    }
    public record Result(Status status, Progress progress, String eventKey, String idempotencyKey, String feedback) {
        public Result {
            Objects.requireNonNull(status, "status"); Objects.requireNonNull(progress, "progress");
            Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null)) throw new IllegalArgumentException("M10 event pair mismatch");
        }
        public boolean commitsEvent() { return eventKey != null; }
    }
}
