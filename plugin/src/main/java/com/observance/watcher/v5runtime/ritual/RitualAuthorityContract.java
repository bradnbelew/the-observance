package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Exact authority bindings used by the isolated ritual controllers. */
public final class RitualAuthorityContract {
    private static final Map<String, String> EXPECTED_KINDS = Map.of(
            "WR03", "stateful_three_topic_confrontation",
            "WR05", "visible_active_roster_three_branch_vote",
            "RP03", "visible_two_branch_active_roster_choice",
            "RP04", "all_visible_active_players_presence_and_confirmation",
            "RP05", "operator_armed_single_player_commit_confirmation",
            "RP06", "durable_phase_driven_automatic_cinematic");

    private final PhysicalPredicateAuthority authority;

    public RitualAuthorityContract(PhysicalPredicateAuthority authority) {
        this.authority = Objects.requireNonNull(authority, "authority");
        RitualPredicateCoverage.validateAgainst(authority);
        EXPECTED_KINDS.forEach((nodeId, kind) -> {
            PhysicalPredicateAuthority.Node node = authority.requireNode(nodeId);
            if (!kind.equals(node.predicate().kind())) {
                throw new IllegalStateException(
                        nodeId + " authority kind drift: expected " + kind + ", found "
                                + node.predicate().kind());
            }
        });
        P12AnySubsetAuthority.validate(authority);
    }

    public PhysicalPredicateAuthority authority() {
        return authority;
    }

    public PhysicalPredicateAuthority.Node node(String nodeId) {
        if (!RitualPredicateCoverage.implementedNodeIds().contains(nodeId)) {
            throw new IllegalArgumentException("node is outside ritual coverage: " + nodeId);
        }
        return authority.requireNode(nodeId);
    }

    public String completionFlag(String nodeId) {
        return node(nodeId).completionFlag();
    }

    /** Recursively resolves every local/external receipt required through the requested node. */
    public Set<String> prerequisiteChainThrough(String nodeId) {
        PhysicalPredicateAuthority.Node root = node(nodeId);
        Set<String> result = new LinkedHashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>(root.prerequisites());
        while (!pending.isEmpty()) {
            String flag = pending.removeFirst();
            if (!result.add(flag)) {
                continue;
            }
            authority.findByCompletionFlag(flag).ifPresent(owner -> pending.addAll(owner.prerequisites()));
        }
        return Set.copyOf(result);
    }
}
