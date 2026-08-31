package com.observance.watcher.morrow;

import java.util.Map;

import static java.util.Map.entry;

/** Canonical event ownership and immediate prerequisite authority used by the Paper journal. */
public final class MorrowEventAuthority {
    private static final Map<String, Definition> EVENTS = Map.ofEntries(
            event("morrow.act0.case_chain_authenticated", "copperline", null),
            event("morrow.act0.server_handoff_recovered", "copperline", "morrow.act0.case_chain_authenticated"),
            event("morrow.act1.room04_witnessed", "minecraft", "morrow.act0.server_handoff_recovered"),
            event("morrow.act1.static_proposal_authenticated", "minecraft", "morrow.act1.room04_witnessed"),
            event("morrow.act1.intention_error_proven", "minecraft", "morrow.act1.static_proposal_authenticated"),
            event("morrow.act1.entity_replay_authorized", "minecraft", "morrow.act1.intention_error_proven"),
            event("morrow.act2.missing_role_completed", "minecraft", "morrow.act1.entity_replay_authorized"),
            event("morrow.act2.live_test_recorded", "minecraft", "morrow.act2.missing_role_completed"),
            event("morrow.act2.behavior_reuse_proven", "minecraft", "morrow.act2.live_test_recorded"),
            event("morrow.act2.private_contradiction_resolved", "discord", "morrow.act2.behavior_reuse_proven"),
            event("morrow.act2.live_capture_authorized", "minecraft", "morrow.act2.private_contradiction_resolved"),
            event("morrow.act3.version_fragments_authenticated", "minecraft", "morrow.act2.live_capture_authorized"),
            event("morrow.act3.incomplete_consensus_proven", "minecraft", "morrow.act3.version_fragments_authenticated"),
            event("morrow.act4.witness_anchor_registered", "minecraft", "morrow.act3.incomplete_consensus_proven"),
            event("morrow.act4.almost_home_proven", "minecraft", "morrow.act4.witness_anchor_registered"),
            event("morrow.act4.account_continuity_authorized", "minecraft", "morrow.act4.almost_home_proven"),
            event("morrow.act5.continued_session_observed", "minecraft", "morrow.act4.account_continuity_authorized"),
            event("morrow.act5.returning_identity_authenticated", "minecraft", "morrow.act5.continued_session_observed"),
            event("morrow.act5.dual_session_consciousness_proven", "minecraft", "morrow.act5.returning_identity_authenticated"),
            event("morrow.act6.audit_chronology_proven", "copperline", "morrow.act5.dual_session_consciousness_proven"),
            event("morrow.act6.current_morrow_reconstruction_proven", "minecraft", "morrow.act6.audit_chronology_proven"),
            event("morrow.act6.cold_storage_access_authorized", "minecraft", "morrow.act6.current_morrow_reconstruction_proven"),
            event("morrow.act7.rollback_anchors_committed", "minecraft", "morrow.act6.cold_storage_access_authorized"),
            event("morrow.act7.branch_policy_committed", "minecraft", "morrow.act7.rollback_anchors_committed"),
            event("morrow.act7.branch_governance_authorized", "minecraft", "morrow.act7.branch_policy_committed"),
            event("morrow.act7.coda_started", "minecraft", "morrow.act7.branch_governance_authorized")
    );

    private MorrowEventAuthority() { }

    public static boolean canonical(String eventKey) {
        return EVENTS.containsKey(eventKey);
    }

    public static boolean minecraftOwns(String eventKey) {
        Definition definition = EVENTS.get(eventKey);
        return definition != null && "minecraft".equals(definition.owner());
    }

    public static String prerequisite(String eventKey) {
        Definition definition = EVENTS.get(eventKey);
        if (definition == null) throw new IllegalArgumentException("Unknown Morrow event: " + eventKey);
        return definition.prerequisite();
    }

    private static Map.Entry<String, Definition> event(String key, String owner, String prerequisite) {
        return entry(key, new Definition(owner, prerequisite));
    }

    private record Definition(String owner, String prerequisite) { }
}
