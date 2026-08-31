package com.observance.watcher.morrow.continuity;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Pure M09 authority for a bounded disconnect echo and unobserved-knowledge identity proof. */
public final class AccountContinuityAuthority {
    public static final String PREREQUISITE = "morrow.act4.account_continuity_authorized";
    public static final String CONTINUED_EVENT = "morrow.act5.continued_session_observed";
    public static final String IDENTITY_EVENT = "morrow.act5.returning_identity_authenticated";

    private AccountContinuityAuthority() { }

    public static Result begin(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player,
                               AnchorMark secret, String challengeId) {
        require(progress, snapshot, player); Objects.requireNonNull(secret, "secret");
        if (!snapshot.committedEvents().contains(PREREQUISITE))
            return readOnly(Status.LOCKED, progress, "M09 requires the separately authorized bounded continuity test.");
        if (snapshot.committedEvents().contains(CONTINUED_EVENT) || progress.continuationObserved())
            return readOnly(Status.IMMUTABLE, progress, "The observed continuation receipt is already filed.");
        if (progress.challenge() != null)
            return readOnly(Status.ACTIVE, progress, "A volunteer challenge is already active; reset it safely before replacing it.");
        if (challengeId == null || !challengeId.matches("[0-9a-f]{32}"))
            throw new IllegalArgumentException("M09 challenge id must be 32 lowercase hex characters");
        Challenge challenge = new Challenge(player, challengeId, secretHash(player, challengeId, secret));
        return readOnly(Status.SECRET_REGISTERED, progress.begin(challenge),
                "Private anchor sealed as " + secret.label() + ". It is excluded from the echo action log; remember it after reconnecting.");
    }

    public static Result arm(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        require(progress, snapshot, player);
        if (progress.challenge() == null) return readOnly(Status.INCOMPLETE, progress, "Choose a private anchor before arming M09.");
        if (!progress.challenge().playerId().equals(player)) return wrongPlayer(progress);
        if (progress.disconnect() != null) return readOnly(Status.ACTIVE, progress, "This disconnect has already been recorded.");
        if (progress.armed()) return readOnly(Status.DUPLICATE, progress, "The bounded disconnect test is already armed.");
        return readOnly(Status.ARMED, progress.arm(),
                "Bounded test armed. Disconnect normally; reconnect returns you to a safe replayable identity scene.");
    }

    public static Result recordDisconnect(Progress progress, MorrowRelationshipSnapshot snapshot,
                                          UUID player, long serverTick, List<String> echoActions) {
        require(progress, snapshot, player);
        if (progress.challenge() == null || !progress.armed())
            return readOnly(Status.INCOMPLETE, progress, "No armed M09 test exists; the disconnect was not captured.");
        if (!progress.challenge().playerId().equals(player)) return wrongPlayer(progress);
        if (progress.disconnect() != null) return readOnly(Status.DUPLICATE, progress, "The disconnect receipt already exists.");
        DisconnectReceipt receipt = new DisconnectReceipt(serverTick, echoActions, actionHash(echoActions));
        return readOnly(Status.DISCONNECTED, progress.disconnect(receipt),
                "Disconnect receipt sealed. The echo may replay only the " + receipt.echoActions().size() + " observed actions.");
    }

    public static Result recordReturn(Progress progress, MorrowRelationshipSnapshot snapshot,
                                      UUID player, long serverTick) {
        require(progress, snapshot, player);
        if (progress.challenge() == null || progress.disconnect() == null)
            return readOnly(Status.INCOMPLETE, progress, "No captured M09 disconnect awaits a return.");
        if (!progress.challenge().playerId().equals(player)) return wrongPlayer(progress);
        if (progress.returnedTick() != null) return readOnly(Status.DUPLICATE, progress, "The returning player is already present.");
        if (serverTick < progress.disconnect().serverTick()) throw new IllegalArgumentException("M09 return predates disconnect");
        return readOnly(Status.RETURNED, progress.returned(serverTick),
                "Return receipt matched the volunteer UUID. Group observation can now compare the real player and bounded echo.");
    }

    public static Result observeContinuation(Progress progress, MorrowRelationshipSnapshot snapshot, UUID observer) {
        require(progress, snapshot, observer);
        if (snapshot.committedEvents().contains(CONTINUED_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The continued-session observation is already filed.");
        if (progress.continuationObserved()) return recoverContinuation(progress, snapshot);
        if (progress.returnedTick() == null)
            return readOnly(Status.INCOMPLETE, progress, "The volunteer must reconnect before the group can file the echo observation.");
        Progress observed = progress.observe();
        return commit(Status.READY_TO_COMMIT, observed, CONTINUED_EVENT, "paper:m09:continued-session:v1",
                "The group observed a continued echo restricted to the pre-disconnect action log.");
    }

    public static Result recoverContinuation(Progress progress, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.committedEvents().contains(CONTINUED_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The continued-session observation is already filed.");
        if (!progress.continuationObserved())
            return readOnly(Status.INCOMPLETE, progress, "No local continued-session observation awaits recovery.");
        return commit(Status.READY_TO_COMMIT, progress, CONTINUED_EVENT, "paper:m09:continued-session:v1",
                "Recovered the M09 continuation observation without replaying a disconnect.");
    }

    public static Result authenticate(Progress progress, MorrowRelationshipSnapshot snapshot,
                                      UUID player, AnchorMark claimedSecret) {
        require(progress, snapshot, player); Objects.requireNonNull(claimedSecret, "claimedSecret");
        if (!snapshot.committedEvents().contains(CONTINUED_EVENT))
            return readOnly(Status.INCOMPLETE, progress, "Observe and file the bounded continuation before authenticating identity.");
        if (snapshot.committedEvents().contains(IDENTITY_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The returning identity is already authenticated.");
        if (progress.authenticated()) return recoverIdentity(progress, snapshot);
        if (progress.challenge() == null || !progress.challenge().playerId().equals(player)) return wrongPlayer(progress);
        if (!progress.challenge().secretSha256().equals(secretHash(player, progress.challenge().challengeId(), claimedSecret)))
            return readOnly(Status.WRONG_SECRET, progress,
                    "That anchor was not the volunteer's sealed unobserved choice. Neither player nor echo was harmed; retry or reset the scene.");
        Progress authenticated = progress.authenticate();
        return commit(Status.READY_TO_COMMIT, authenticated, IDENTITY_EVENT,
                "paper:m09:returning-identity:v1",
                "Returning player authenticated with knowledge excluded from the echo log. The echo is filed as reconstructed continuation.");
    }

    public static Result recoverIdentity(Progress progress, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.committedEvents().contains(IDENTITY_EVENT))
            return readOnly(Status.DUPLICATE, progress, "The returning identity is already authenticated.");
        if (!progress.authenticated()) return readOnly(Status.INCOMPLETE, progress, "No local M09 identity proof awaits recovery.");
        return commit(Status.READY_TO_COMMIT, progress, IDENTITY_EVENT,
                "paper:m09:returning-identity:v1", "Recovered the M09 identity proof without exposing the private anchor.");
    }

    public static Result reset(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        require(progress, snapshot, player);
        if (snapshot.committedEvents().contains(CONTINUED_EVENT))
            return readOnly(Status.IMMUTABLE, progress, "The continued-session event is already filed and cannot be erased by reset.");
        if (progress.challenge() != null && !progress.challenge().playerId().equals(player)) return wrongPlayer(progress);
        return readOnly(Status.RESET, progress.reset(), "M09 returned to its safe lobby state. No global event or echo identity was created.");
    }

    public static byte[] payload(Result result) {
        Progress progress = result.progress();
        if (CONTINUED_EVENT.equals(result.eventKey())) {
            DisconnectReceipt disconnect = Objects.requireNonNull(progress.disconnect(), "disconnect");
            return ("{\"disconnect_tick\":" + disconnect.serverTick() + ",\"echo_action_count\":"
                    + disconnect.echoActions().size() + ",\"echo_action_sha256\":\"" + disconnect.actionSha256()
                    + "\",\"echo_secret_access\":false,\"investigation\":\"M09\",\"return_tick\":"
                    + progress.returnedTick() + ",\"volunteer_player\":\"" + progress.challenge().playerId() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        if (IDENTITY_EVENT.equals(result.eventKey())) {
            return ("{\"challenge_id\":\"" + progress.challenge().challengeId()
                    + "\",\"echo_classification\":\"reconstructed_continuation\",\"echo_secret_access\":false,"
                    + "\"investigation\":\"M09\",\"returning_player\":\"" + progress.challenge().playerId()
                    + "\",\"secret_sha256\":\"" + progress.challenge().secretSha256() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("M09 result is not committable");
    }

    static String secretHash(UUID player, String challengeId, AnchorMark mark) {
        return hash("m09-secret-v1\nplayer=" + player + "\nchallenge=" + challengeId + "\nmark=" + mark.name() + "\n");
    }
    static String actionHash(List<String> actions) {
        StringBuilder value = new StringBuilder("m09-echo-actions-v1\n");
        for (String action : actions) value.append(action).append('\n');
        return hash(value.toString());
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static Result wrongPlayer(Progress progress) {
        return readOnly(Status.WRONG_PLAYER, progress,
                "That action belongs to the volunteer account. No identity or continuation state changed.");
    }
    private static Result readOnly(Status status, Progress progress, String feedback) {
        return new Result(status, progress, null, null, feedback);
    }
    private static Result commit(Status status, Progress progress, String event, String idempotency, String feedback) {
        return new Result(status, progress, event, idempotency, feedback);
    }
    private static void require(Progress progress, MorrowRelationshipSnapshot snapshot, UUID player) {
        Objects.requireNonNull(progress, "progress"); Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(player, "player");
    }

    public enum AnchorMark {
        NORTH_STEP("north stair notch"), COPPER_CROSS("copper cross"), HOLLOW_SQUARE("hollow square"),
        EAST_ARROW("east arrow"), DOUBLE_PILLAR("double pillar"), LOW_ARCH("low arch");
        private final String label; AnchorMark(String label) { this.label = label; }
        public String label() { return label; }
    }
    public enum Status { LOCKED, SECRET_REGISTERED, ACTIVE, ARMED, DISCONNECTED, RETURNED,
        READY_TO_COMMIT, DUPLICATE, INCOMPLETE, WRONG_PLAYER, WRONG_SECRET, RESET, IMMUTABLE }

    public record Challenge(UUID playerId, String challengeId, String secretSha256) {
        public Challenge {
            Objects.requireNonNull(playerId, "playerId"); Objects.requireNonNull(challengeId, "challengeId");
            Objects.requireNonNull(secretSha256, "secretSha256");
            if (!challengeId.matches("[0-9a-f]{32}") || !secretSha256.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("invalid M09 challenge");
        }
    }
    public record DisconnectReceipt(long serverTick, List<String> echoActions, String actionSha256) {
        public DisconnectReceipt {
            echoActions = List.copyOf(echoActions); Objects.requireNonNull(actionSha256, "actionSha256");
            if (serverTick < 0 || echoActions.size() < 3 || echoActions.size() > 12
                    || echoActions.stream().anyMatch(action -> action == null || !action.matches("[a-z0-9_:-]{1,48}"))
                    || !actionSha256.equals(actionHash(echoActions)))
                throw new IllegalArgumentException("invalid M09 disconnect receipt");
        }
    }
    public record Progress(Challenge challenge, boolean armed, DisconnectReceipt disconnect,
                           Long returnedTick, boolean continuationObserved, boolean authenticated, long revision) {
        public Progress {
            if (revision < 0 || (armed && challenge == null) || (disconnect != null && (!armed || challenge == null))
                    || (returnedTick != null && (disconnect == null || returnedTick < disconnect.serverTick()))
                    || (continuationObserved && returnedTick == null) || (authenticated && !continuationObserved))
                throw new IllegalArgumentException("invalid M09 progress");
        }
        public static Progress initial() { return new Progress(null, false, null, null, false, false, 0); }
        Progress begin(Challenge value) { return new Progress(value, false, null, null, false, false, revision + 1); }
        Progress arm() { return new Progress(challenge, true, null, null, false, false, revision + 1); }
        Progress disconnect(DisconnectReceipt value) { return new Progress(challenge, true, value, null, false, false, revision + 1); }
        Progress returned(long tick) { return new Progress(challenge, true, disconnect, tick, false, false, revision + 1); }
        Progress observe() { return new Progress(challenge, true, disconnect, returnedTick, true, false, revision + 1); }
        Progress authenticate() { return new Progress(challenge, true, disconnect, returnedTick, true, true, revision + 1); }
        Progress reset() { return new Progress(null, false, null, null, false, false, revision + 1); }
    }
    public record Result(Status status, Progress progress, String eventKey, String idempotencyKey, String feedback) {
        public Result {
            Objects.requireNonNull(status, "status"); Objects.requireNonNull(progress, "progress");
            Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null)) throw new IllegalArgumentException("M09 event pair mismatch");
        }
        public boolean commitsEvent() { return eventKey != null; }
    }
}
