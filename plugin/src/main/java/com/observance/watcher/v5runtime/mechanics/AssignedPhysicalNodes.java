package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.ImplementationFamily;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PredicateCoverageCatalog;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact V5 world-adapter ownership for the S/I/F/L/R mechanics engine. */
public final class AssignedPhysicalNodes {
    private static final Map<ImplementationFamily, List<String>> BY_FAMILY = build();
    private static final Set<String> ALL = flatten();

    private AssignedPhysicalNodes() {
    }

    public static Set<String> implementedNodeIds() {
        return ALL;
    }

    public static Map<ImplementationFamily, List<String>> byFamily() {
        return BY_FAMILY;
    }

    /** Fails closed if authority or core routing changes without a matching adapter update. */
    public static void validateAgainst(PhysicalPredicateAuthority authority) {
        Set<String> authorityAssigned = new LinkedHashSet<>();
        for (PhysicalPredicateAuthority.Node node : authority.nodes()) {
            ImplementationFamily family = PredicateCoverageCatalog.require(node.nodeId()).family();
            if (BY_FAMILY.containsKey(family) && !"RP04".equals(node.nodeId())) {
                authorityAssigned.add(node.nodeId());
            }
        }
        if (!authorityAssigned.equals(ALL)) {
            Set<String> missing = new LinkedHashSet<>(authorityAssigned);
            missing.removeAll(ALL);
            Set<String> stale = new LinkedHashSet<>(ALL);
            stale.removeAll(authorityAssigned);
            throw new IllegalStateException(
                    "S/I/F/L/R adapter coverage drift; missing=" + missing + ", stale=" + stale);
        }
        for (Map.Entry<ImplementationFamily, List<String>> family : BY_FAMILY.entrySet()) {
            for (String nodeId : family.getValue()) {
                ImplementationFamily actual = PredicateCoverageCatalog.require(nodeId).family();
                if (actual != family.getKey()) {
                    throw new IllegalStateException(nodeId + " is routed to " + actual
                            + " rather than " + family.getKey());
                }
            }
        }
    }

    private static Map<ImplementationFamily, List<String>> build() {
        Map<ImplementationFamily, List<String>> result = new EnumMap<>(ImplementationFamily.class);
        result.put(ImplementationFamily.S, List.of(
                "LC02", "LC04", "A04", "WR01", "CW04", "BI05", "KV03", "KM02",
                "KS03", "KI01", "HS03", "HS04"));
        result.put(ImplementationFamily.I, List.of("LC03", "KV01", "A02", "WR02", "KI02"));
        result.put(ImplementationFamily.F, List.of(
                "LC01", "LC06", "A05", "CW03", "BI01", "BI04",
                "HS05", "KS01", "KS02", "KO02", "KB02"));
        result.put(ImplementationFamily.L, List.of(
                "AR02", "AR03", "AR04", "AR05", "AR06", "AR07", "CW01", "KM01"));
        result.put(ImplementationFamily.R, List.of(
                "WR04", "BI07", "HS06", "KM03", "KO01"));
        return Map.copyOf(result);
    }

    private static Set<String> flatten() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (ImplementationFamily family : List.of(
                ImplementationFamily.S, ImplementationFamily.I, ImplementationFamily.F,
                ImplementationFamily.L, ImplementationFamily.R)) {
            for (String nodeId : BY_FAMILY.get(family)) {
                if (!result.add(nodeId)) {
                    throw new ExceptionInInitializerError("duplicate mechanics node " + nodeId);
                }
            }
        }
        if (result.size() != 41) {
            throw new ExceptionInInitializerError(
                    "S/I/F/L/R mechanics must own 41 nodes; RP04 is ritual-owned");
        }
        return Set.copyOf(result);
    }
}
