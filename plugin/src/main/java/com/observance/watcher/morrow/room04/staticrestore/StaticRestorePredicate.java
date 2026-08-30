package com.observance.watcher.morrow.room04.staticrestore;

import com.observance.watcher.morrow.MorrowRelationshipSnapshot;
import com.observance.watcher.morrow.dialog.MorrowDialogAuthority;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Candidate;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.CandidateId;
import com.observance.watcher.morrow.room04.staticrestore.StaticRestoreManifest.Provenance;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Exact physical-cell plus provenance predicate; dialogue and freeform text have no authority here. */
public final class StaticRestorePredicate {
    public static final String IDEMPOTENCY_KEY = "paper:room04:m02:intention-error:v1";

    public enum Status { CORRECT, WRONG, NOT_READY }

    public record Attempt(
            Status status,
            CandidateId candidate,
            Provenance classification,
            String eventKey,
            String idempotencyKey,
            byte[] payload,
            String feedback) {
        public Attempt {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(candidate, "candidate");
            Objects.requireNonNull(classification, "classification");
            payload = payload == null ? new byte[0] : payload.clone();
            if (feedback == null || feedback.isBlank()) throw new IllegalArgumentException("attempt requires feedback");
            if ((eventKey == null) != (idempotencyKey == null)) {
                throw new IllegalArgumentException("event and idempotency key must be paired");
            }
            if (status != Status.CORRECT && eventKey != null) {
                throw new IllegalArgumentException("only the factual physical predicate may create proof");
            }
        }

        @Override public byte[] payload() { return payload.clone(); }
        public boolean commitsProof() { return eventKey != null; }
    }

    private final StaticRestoreManifest manifest;

    public StaticRestorePredicate(StaticRestoreManifest manifest) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
    }

    public Attempt evaluate(
            MorrowRelationshipSnapshot snapshot,
            CandidateId selected,
            Provenance classification,
            boolean restorationComplete) {
        Objects.requireNonNull(snapshot, "snapshot");
        Candidate candidate = manifest.candidate(Objects.requireNonNull(selected, "selected"));
        Objects.requireNonNull(classification, "classification");
        if (!snapshot.committedEvents().contains(MorrowDialogAuthority.PROPOSAL_AUTHENTICATED)) {
            return notReady(selected, classification,
                    "Authenticate the bounded Static Restore proposal before filing a marker.");
        }
        if (!restorationComplete) {
            return notReady(selected, classification,
                    "Static Restore is still applying its three visible passes. No classification was filed.");
        }
        if (selected == CandidateId.B06 && classification == Provenance.INFERRED) {
            String payload = "{\"classification\":\"inferred\",\"coordinate\":\"B06\","
                    + "\"evidence\":[\"archived_screenshot\",\"block_manifest\",\"finch_planning_post\"],"
                    + "\"factual_error\":\"discussed_never_placed\",\"investigation\":\"M02\","
                    + "\"manifest_sha256\":\"" + manifest.manifestSha256() + "\","
                    + "\"restored_block\":\"" + candidate.restoredBlock() + "\","
                    + "\"world_mutation\":false}";
            return new Attempt(
                    Status.CORRECT,
                    selected,
                    classification,
                    MorrowDialogAuthority.INTENTION_ERROR_PROVEN,
                    IDEMPOTENCY_KEY,
                    payload.getBytes(StandardCharsets.UTF_8),
                    "B06 is filed as inferred. The screenshot and manifest omit it; Finch discussed it but never placed it.");
        }
        return wrong(selected, classification);
    }

    private Attempt wrong(CandidateId selected, Provenance classification) {
        String feedback;
        if (selected != CandidateId.B06) {
            feedback = switch (classification) {
                case INFERRED -> selected + " is present in both authenticated placement sources. Re-open the image and manifest.";
                case AUTHENTICATED -> selected + " is supported, but it is not the unsupported sixth restore. The marker was not filed.";
                case CONFLICTING -> selected + " has two agreeing placement sources, not competing versions. The marker was not filed.";
                case UNKNOWN -> selected + " has named material and coordinates in authenticated sources. It is not unknown.";
            };
        } else {
            feedback = switch (classification) {
                case AUTHENTICATED -> "B06 appears only as Finch's future plan. Intention is not an authenticated placement.";
                case CONFLICTING -> "No second placed version of B06 exists. A plan and an action are not conflicting placements.";
                case UNKNOWN -> "Finch named B06's spruce material, so it is known as a proposal—but not as history.";
                case INFERRED -> throw new IllegalStateException("correct attempt routed to wrong feedback");
            };
        }
        return new Attempt(Status.WRONG, selected, classification, null, null, new byte[0], feedback);
    }

    private static Attempt notReady(CandidateId selected, Provenance classification, String feedback) {
        return new Attempt(Status.NOT_READY, selected, classification, null, null, new byte[0], feedback);
    }
}
