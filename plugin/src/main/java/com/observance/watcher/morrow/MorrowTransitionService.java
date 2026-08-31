package com.observance.watcher.morrow;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Pure fail-closed transition logic. Dialogue or a language model cannot bypass these predicates. */
public final class MorrowTransitionService {
    private static final Map<MorrowStage, List<String>> REQUIRED_EVENTS = Map.of(
            MorrowStage.HELPFUL, List.of(
                    "morrow.act1.static_proposal_authenticated",
                    "morrow.act1.intention_error_proven",
                    "morrow.act1.entity_replay_authorized"
            ),
            MorrowStage.CURIOUS, List.of(
                    "morrow.act2.missing_role_completed",
                    "morrow.act2.behavior_reuse_proven",
                    "morrow.act2.private_contradiction_resolved",
                    "morrow.act2.live_capture_authorized"
            ),
            MorrowStage.INTIMATE, List.of(
                    "morrow.act3.incomplete_consensus_proven",
                    "morrow.act4.witness_anchor_registered",
                    "morrow.act4.almost_home_proven",
                    "morrow.act4.account_continuity_authorized"
            ),
            MorrowStage.POSSESSIVE, List.of(
                    "morrow.act5.returning_identity_authenticated",
                    "morrow.act6.current_morrow_reconstruction_proven",
                    "morrow.act6.cold_storage_access_authorized"
            ),
            MorrowStage.AFRAID, List.of(
                    "morrow.act7.rollback_anchors_committed",
                    "morrow.act7.branch_policy_committed",
                    "morrow.act7.branch_governance_authorized"
            )
    );

    public TransitionResult attempt(MorrowRelationshipSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.stage().terminal()) {
            return new TransitionResult(TransitionStatus.TERMINAL, snapshot, Set.of());
        }

        List<String> required = REQUIRED_EVENTS.get(snapshot.stage());
        if (required == null) {
            throw new IllegalStateException("No predicate authority for stage " + snapshot.stage().key());
        }
        Set<String> missing = new LinkedHashSet<>();
        for (String event : required) {
            if (!snapshot.committedEvents().contains(event)) {
                missing.add(event);
            }
        }
        if (!missing.isEmpty()) {
            return new TransitionResult(TransitionStatus.BLOCKED, snapshot, missing);
        }
        return new TransitionResult(TransitionStatus.ADVANCED, snapshot.advanceTo(snapshot.stage().next()), Set.of());
    }

    public enum TransitionStatus {
        BLOCKED,
        ADVANCED,
        TERMINAL
    }

    public record TransitionResult(
            TransitionStatus status,
            MorrowRelationshipSnapshot snapshot,
            Set<String> missingEvents
    ) {
        public TransitionResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(snapshot, "snapshot");
            missingEvents = Set.copyOf(Objects.requireNonNull(missingEvents, "missingEvents"));
            if (status == TransitionStatus.BLOCKED && missingEvents.isEmpty()) {
                throw new IllegalArgumentException("blocked transition must report missing events");
            }
            if (status != TransitionStatus.BLOCKED && !missingEvents.isEmpty()) {
                throw new IllegalArgumentException("non-blocked transition cannot report missing events");
            }
        }
    }
}
