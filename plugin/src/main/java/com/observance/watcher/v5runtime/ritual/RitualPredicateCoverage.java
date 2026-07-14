package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.ImplementationFamily;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.PredicateCoverageCatalog;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Fail-closed declaration of the six V5 ritual/finale predicates implemented here. */
public final class RitualPredicateCoverage {
    private static final Map<String, ImplementationFamily> EXPECTED_FAMILIES = expectedFamilies();
    private static final Set<String> IMPLEMENTED_NODE_IDS = Set.copyOf(EXPECTED_FAMILIES.keySet());

    private RitualPredicateCoverage() {
    }

    public static Set<String> implementedNodeIds() {
        assertCatalogRouting();
        return IMPLEMENTED_NODE_IDS;
    }

    public static void validateAgainst(PhysicalPredicateAuthority authority) {
        if (authority == null) {
            throw new IllegalArgumentException("authority is required");
        }
        assertCatalogRouting();
        for (String nodeId : IMPLEMENTED_NODE_IDS) {
            authority.requireNode(nodeId);
        }
        if (IMPLEMENTED_NODE_IDS.size() != 6) {
            throw new IllegalStateException("ritual coverage must contain exactly six predicates");
        }
    }

    private static void assertCatalogRouting() {
        EXPECTED_FAMILIES.forEach((nodeId, family) -> {
            ImplementationFamily actual = PredicateCoverageCatalog.require(nodeId).family();
            if (actual != family) {
                throw new IllegalStateException(
                        "V5 ritual routing drift for " + nodeId + ": expected " + family
                                + ", found " + actual);
            }
        });
    }

    private static Map<String, ImplementationFamily> expectedFamilies() {
        Map<String, ImplementationFamily> result = new LinkedHashMap<>();
        result.put("WR03", ImplementationFamily.N);
        result.put("WR05", ImplementationFamily.V);
        result.put("RP03", ImplementationFamily.V);
        result.put("RP04", ImplementationFamily.R);
        result.put("RP05", ImplementationFamily.V);
        result.put("RP06", ImplementationFamily.V);
        return Map.copyOf(result);
    }
}
