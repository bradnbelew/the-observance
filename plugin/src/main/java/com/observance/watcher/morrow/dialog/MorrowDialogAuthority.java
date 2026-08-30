package com.observance.watcher.morrow.dialog;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest;
import com.observance.watcher.morrow.room04.replay.EntityReplayAuthority;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Pure authored routing and exact receipt authority for the four Room 04 dialogs. */
public final class MorrowDialogAuthority {
    public static final String CASE_HANDOFF = "morrow.act0.server_handoff_recovered";
    public static final String ROOM_WITNESSED = "morrow.act1.room04_witnessed";
    public static final String PROPOSAL_AUTHENTICATED = "morrow.act1.static_proposal_authenticated";
    public static final String INTENTION_ERROR_PROVEN = "morrow.act1.intention_error_proven";
    public static final String ENTITY_REPLAY_AUTHORIZED = "morrow.act1.entity_replay_authorized";
    public static final String PRIVATE_CONTRADICTION_RESOLVED = "morrow.act2.private_contradiction_resolved";

    public enum View {
        TERMINAL_GREETING,
        RESTORATION_PROPOSAL,
        EVIDENCE_REVIEW,
        ENTITY_REPLAY_AUTHORIZATION,
        ENTITY_REPLAY_CONSOLE,
        LIVE_CAPTURE_AUTHORIZATION
    }

    public enum Action {
        ACKNOWLEDGE_ROOM,
        AUTHENTICATE_PROPOSAL,
        REVIEW_EVIDENCE,
        RESET_STATIC_RESTORE,
        AUTHORIZE_ENTITY_REPLAY,
        DECLINE_ENTITY_REPLAY,
        AUTHORIZE_LIVE_CAPTURE,
        DECLINE_LIVE_CAPTURE
    }

    public enum Status { READY, NOT_READY }

    public record Decision(
            Status status,
            String eventKey,
            String idempotencyKey,
            byte[] payload,
            String feedback) {
        public Decision {
            Objects.requireNonNull(status, "status");
            payload = payload == null ? new byte[0] : payload.clone();
            feedback = Objects.requireNonNull(feedback, "feedback");
            if ((eventKey == null) != (idempotencyKey == null)) {
                throw new IllegalArgumentException("event and idempotency key must be present together");
            }
            if (eventKey != null && payload.length == 0) {
                throw new IllegalArgumentException("receipt-producing action requires a payload");
            }
            if (status == Status.NOT_READY && eventKey != null) {
                throw new IllegalArgumentException("not-ready action cannot produce a receipt");
            }
        }

        @Override public byte[] payload() { return payload.clone(); }
        public boolean commitsReceipt() { return eventKey != null; }
    }

    private MorrowDialogAuthority() { }

    public static View currentView(MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!has(snapshot, ROOM_WITNESSED)) return View.TERMINAL_GREETING;
        if (!has(snapshot, PROPOSAL_AUTHENTICATED)) return View.RESTORATION_PROPOSAL;
        if (!has(snapshot, INTENTION_ERROR_PROVEN)) return View.EVIDENCE_REVIEW;
        if (!has(snapshot, ENTITY_REPLAY_AUTHORIZED)) return View.ENTITY_REPLAY_AUTHORIZATION;
        if (!has(snapshot, EntityReplayAuthority.BEHAVIOR_REUSE_PROVEN)) return View.ENTITY_REPLAY_CONSOLE;
        if (!has(snapshot, PRIVATE_CONTRADICTION_RESOLVED)) return View.ENTITY_REPLAY_CONSOLE;
        if (!has(snapshot, EntityReplayAuthority.LIVE_CAPTURE_AUTHORIZED)) return View.LIVE_CAPTURE_AUTHORIZATION;
        return View.EVIDENCE_REVIEW;
    }

    public static Decision decide(Action action, MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(snapshot, "snapshot");
        return switch (action) {
            case ACKNOWLEDGE_ROOM -> has(snapshot, CASE_HANDOFF)
                    ? receipt(
                            ROOM_WITNESSED,
                            "paper:room04:greeting:v1",
                            "{\"action\":\"acknowledge_room\",\"dialog\":\"terminal_greeting_v1\",\"scope\":\"group\"}",
                            "Recovery Room 04 is acknowledged. The group receipt is retained locally.")
                    : notReady("The authenticated server handoff has not reached this room. Nothing changed.");
            case AUTHENTICATE_PROPOSAL -> has(snapshot, ROOM_WITNESSED)
                    ? receipt(
                            PROPOSAL_AUTHENTICATED,
                            "paper:room04:proposal:v1",
                            "{\"action\":\"apply_bounded_static_restore\",\"bounded_cells\":6,\"dialog\":\"restoration_proposal_v1\",\"manifest_sha256\":\""
                                    + StaticRestoreManifest.MANIFEST_SHA256
                                    + "\",\"proposal\":\"static_restore_room04_m02_v1\",\"region\":\"recovery_room_04\",\"rollback\":\"six_cell_baseline\",\"scope\":\"group\",\"world_mutation\":\"bounded_static_restore\"}",
                            "The bounded proposal is authenticated. Three visible two-cell passes are now applying.")
                    : notReady("A room witness receipt is required before the proposal can be authenticated.");
            case REVIEW_EVIDENCE -> has(snapshot, ROOM_WITNESSED)
                    ? noReceipt("Evidence review is read-only. No receipt or world change was created.")
                    : notReady("Enter and acknowledge Recovery Room 04 before reviewing its evidence.");
            case RESET_STATIC_RESTORE -> has(snapshot, PROPOSAL_AUTHENTICATED)
                    ? noReceipt("The six-cell scene is reset to baseline and its authenticated proposal will replay.")
                    : notReady("Authenticate the bounded proposal before using its Reset control.");
            case AUTHORIZE_ENTITY_REPLAY -> has(snapshot, INTENTION_ERROR_PROVEN)
                    ? receipt(
                            ENTITY_REPLAY_AUTHORIZED,
                            "paper:room04:entity-replay:v1",
                            "{\"capability\":\"entity_replay\",\"dialog\":\"entity_replay_authorization_v1\",\"rollback\":\"remove_display_echoes\",\"scope\":\"recovery_room_04_scene\",\"source\":\"recorded_actions_only\",\"stop\":\"terminal_cancel\",\"stored_data\":\"local_bounded_scene_samples\",\"world_mutation\":false}",
                            "Entity Replay is authorized for the bounded Room 04 scene.")
                    : notReady("The unsupported restoration block must be proven before Entity Replay can be authorized.");
            case DECLINE_ENTITY_REPLAY -> noReceipt(
                    "Entity Replay remains unauthorized. Evidence and relationship state are unchanged.");
            case AUTHORIZE_LIVE_CAPTURE -> has(snapshot, PRIVATE_CONTRADICTION_RESOLVED)
                    ? receipt(
                            EntityReplayAuthority.LIVE_CAPTURE_AUTHORIZED,
                            "paper:room04:live-capture:v1",
                            "{\"capability\":\"live_capture\",\"dialog\":\"live_capture_authorization_v1\",\"scope\":\"later_authored_scene_only\",\"starts_capture\":false}",
                            "Later live capture is authorized. This receipt does not start a recording.")
                    : notReady("The private behavior-reuse contradiction must have a group receipt before later live capture can be authorized.");
            case DECLINE_LIVE_CAPTURE -> noReceipt(
                    "Later live capture remains unauthorized. No recording started and no receipt was created.");
        };
    }

    private static boolean has(MorrowRelationshipSnapshot snapshot, String event) {
        return snapshot.committedEvents().contains(event);
    }

    private static Decision receipt(
            String event, String idempotencyKey, String payload, String feedback) {
        return new Decision(Status.READY, event, idempotencyKey,
                payload.getBytes(StandardCharsets.UTF_8), feedback);
    }

    private static Decision noReceipt(String feedback) {
        return new Decision(Status.READY, null, null, new byte[0], feedback);
    }

    private static Decision notReady(String feedback) {
        return new Decision(Status.NOT_READY, null, null, new byte[0], feedback);
    }
}
