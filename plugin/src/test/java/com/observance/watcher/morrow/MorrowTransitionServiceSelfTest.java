package com.observance.watcher.morrow;

import java.util.List;

/** Dependency-free contract test executed by Gradle's verification task. */
public final class MorrowTransitionServiceSelfTest {
    private MorrowTransitionServiceSelfTest() {
    }

    public static void main(String[] args) {
        MorrowTransitionService service = new MorrowTransitionService();
        MorrowRelationshipSnapshot state = MorrowRelationshipSnapshot.initial("morrow-dev-0001");

        MorrowTransitionService.TransitionResult blocked = service.attempt(state);
        require(blocked.status() == MorrowTransitionService.TransitionStatus.BLOCKED, "initial transition must block");
        require(blocked.missingEvents().size() == 3, "initial transition must name proofs and authorization");

        List<List<String>> stages = List.of(
                List.of("morrow.act1.static_proposal_authenticated", "morrow.act1.intention_error_proven", "morrow.act1.entity_replay_authorized"),
                List.of("morrow.act2.missing_role_completed", "morrow.act2.behavior_reuse_proven", "morrow.act2.live_capture_authorized"),
                List.of("morrow.act3.contradiction_preserved", "morrow.act4.witness_anchor_registered", "morrow.act4.almost_home_proven", "morrow.act4.account_continuity_authorized"),
                List.of("morrow.act5.returning_identity_authenticated", "morrow.act6.current_morrow_reconstruction_proven", "morrow.act6.cold_storage_access_authorized"),
                List.of("morrow.act7.rollback_anchors_committed", "morrow.act7.branch_policy_committed", "morrow.act7.branch_governance_authorized")
        );

        for (List<String> eventKeys : stages) {
            MorrowStage before = state.stage();
            for (String eventKey : eventKeys) {
                state = state.withCommittedEvent(eventKey);
            }
            MorrowTransitionService.TransitionResult advanced = service.attempt(state);
            require(advanced.status() == MorrowTransitionService.TransitionStatus.ADVANCED,
                    "transition should advance from " + before.key());
            state = advanced.snapshot();
            require(state.stage() == before.next(), "transition skipped or selected wrong stage");
            require(state.capabilities().contains(state.stage().capability()), "target capability missing");
        }

        require(state.stage() == MorrowStage.NEGOTIATED, "final stage must be negotiated");
        require(state.revision() == 5, "exactly five state transitions expected");
        require(state.capabilities().size() == MorrowCapability.values().length, "all capabilities expected at finale");
        require(service.attempt(state).status() == MorrowTransitionService.TransitionStatus.TERMINAL,
                "negotiated state must be terminal");

        MorrowRelationshipSnapshot same = state.withCommittedEvent("morrow.act7.branch_policy_committed");
        require(same == state, "duplicate event must be idempotent");

        boolean rejected = false;
        try {
            state.withCommittedEvent("legacy.p12.record_closed");
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "legacy event namespace must be rejected");

        System.out.println("MORROW TRANSITION SERVICE: PASS");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
