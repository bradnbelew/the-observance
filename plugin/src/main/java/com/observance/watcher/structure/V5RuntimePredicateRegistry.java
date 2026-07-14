package com.observance.watcher.structure;

import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import com.observance.watcher.v5runtime.container.ContainerPredicateCoverage;
import com.observance.watcher.v5runtime.mechanics.AssignedPhysicalNodes;
import com.observance.watcher.v5runtime.ritual.RitualPredicateCoverage;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fail-closed seam for the separately authored, machine-readable V5 success predicates.
 *
 * <p>The bindings authority is already packaged and validated. A binding is not a success predicate:
 * treating a click as a solve would trivialize the ARG. Until the predicate authority is present and
 * every plugin-owned row has an implemented evaluator, production preflight must remain red.
 */
public final class V5RuntimePredicateRegistry {
    private static final AtomicReference<String> ACTIVE_AUTHORITY = new AtomicReference<>();

    private V5RuntimePredicateRegistry() { }

    public static boolean available() {
        return ACTIVE_AUTHORITY.get() != null;
    }

    public static List<String> validateAgainst(List<V5AuthorityManifest.RuntimeBinding> bindings) {
        List<String> issues = new ArrayList<>();
        if (bindings == null) {
            return List.of("V5 runtime bindings are unavailable");
        }
        Set<String> physical = implementedNodeIds();
        Set<String> declared = new LinkedHashSet<>();
        for (V5AuthorityManifest.RuntimeBinding binding : bindings) {
            if (!Set.of("plugin", "plugin_unlit", "plugin_finale").contains(binding.owner())) {
                continue;
            }
            if (!declared.add(binding.nodeId())) {
                issues.add("duplicate plugin runtime binding " + binding.nodeId());
            }
        }
        Set<String> missing = new LinkedHashSet<>(declared);
        missing.removeAll(physical);
        Set<String> stale = new LinkedHashSet<>(physical);
        stale.removeAll(declared);
        if (!missing.isEmpty()) issues.add("runtime adapters missing " + missing);
        if (!stale.isEmpty()) issues.add("runtime adapters stale " + stale);
        if (physical.size() != PhysicalPredicateAuthority.REQUIRED_NODE_COUNT) {
            issues.add("runtime adapter union is " + physical.size() + ", expected 60");
        }
        if (!available()) {
            issues.add("V5 runtime lifecycle is not active; no touch-to-solve fallback is allowed");
        }
        return List.copyOf(issues);
    }

    /** Called only after the lifecycle owner has constructed and registered every live adapter. */
    public static void activate(PhysicalPredicateAuthority authority) {
        if (authority == null) throw new IllegalArgumentException("authority is required");
        AssignedPhysicalNodes.validateAgainst(authority);
        ContainerPredicateCoverage.validateAgainst(authority);
        RitualPredicateCoverage.validateAgainst(authority);
        Set<String> implemented = implementedNodeIds();
        Set<String> expected = new LinkedHashSet<>(authority.nodesById().keySet());
        if (!implemented.equals(expected)) {
            Set<String> missing = new LinkedHashSet<>(expected);
            missing.removeAll(implemented);
            Set<String> stale = new LinkedHashSet<>(implemented);
            stale.removeAll(expected);
            throw new IllegalStateException("V5 live adapter coverage mismatch; missing="
                    + missing + ", stale=" + stale);
        }
        if (!ACTIVE_AUTHORITY.compareAndSet(null, authority.sha256())
                && !authority.sha256().equals(ACTIVE_AUTHORITY.get())) {
            throw new IllegalStateException("a different V5 authority is already active");
        }
    }

    public static void deactivate() {
        ACTIVE_AUTHORITY.set(null);
    }

    public static String activeAuthoritySha256() {
        return ACTIVE_AUTHORITY.get();
    }

    public static Set<String> implementedNodeIds() {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        addDisjoint(result, AssignedPhysicalNodes.implementedNodeIds(), "mechanics");
        addDisjoint(result, ContainerPredicateCoverage.implementedNodeIds(), "container");
        addDisjoint(result, RitualPredicateCoverage.implementedNodeIds(), "ritual");
        return Set.copyOf(result);
    }

    private static void addDisjoint(Set<String> target, Set<String> values, String family) {
        for (String value : values) {
            if (!target.add(value)) {
                throw new IllegalStateException("V5 node " + value
                        + " is claimed by more than one live adapter (at " + family + ")");
            }
        }
    }
}
