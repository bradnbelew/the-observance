package com.observance.watcher.v5runtime.container;

import com.observance.watcher.v5runtime.ImplementationFamily;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PredicateCoverageCatalog;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Exact ownership boundary for the thirteen V5 tagged-container predicates. */
public final class ContainerPredicateCoverage {
    private static final List<String> ORDERED_IDS = List.of(
            "LS06", "A03", "A09", "RP02", "CW02", "CW07", "BI02", "BI03",
            "BI06", "HS01", "HS02", "HS07", "KV02");
    private static final Set<String> IDS = Set.copyOf(new LinkedHashSet<>(ORDERED_IDS));

    private ContainerPredicateCoverage() {
    }

    public static List<String> orderedNodeIds() {
        return ORDERED_IDS;
    }

    public static Set<String> implementedNodeIds() {
        return IDS;
    }

    public static boolean owns(String nodeId) {
        return IDS.contains(nodeId);
    }

    /** Fails closed when authority family routing changes without an adapter rewrite. */
    public static void validateAgainst(PhysicalPredicateAuthority authority) {
        Objects.requireNonNull(authority, "authority");
        LinkedHashSet<String> authorityContainerIds = new LinkedHashSet<>();
        for (PhysicalPredicateAuthority.Node node : authority.nodes()) {
            if (PredicateCoverageCatalog.require(node.nodeId()).family() == ImplementationFamily.C) {
                authorityContainerIds.add(node.nodeId());
            }
        }
        if (!authorityContainerIds.equals(new LinkedHashSet<>(ORDERED_IDS))) {
            LinkedHashSet<String> missing = new LinkedHashSet<>(authorityContainerIds);
            missing.removeAll(IDS);
            LinkedHashSet<String> stale = new LinkedHashSet<>(IDS);
            stale.removeAll(authorityContainerIds);
            throw new IllegalStateException(
                    "V5 container adapter coverage drift; missing=" + missing + ", stale=" + stale);
        }
        if (IDS.size() != 13) {
            throw new IllegalStateException("V5 container adapter must own exactly 13 nodes");
        }
        for (String nodeId : ORDERED_IDS) {
            PhysicalPredicateAuthority.Node node = authority.requireNode(nodeId);
            if (PredicateCoverageCatalog.require(nodeId).family() != ImplementationFamily.C
                    || !Set.of("plugin", "plugin_unlit").contains(node.owner())
                    || !"minecraft_local_primary".equals(node.durabilityProfile())) {
                throw new IllegalStateException("unsupported container authority route for " + nodeId);
            }
        }
    }
}
