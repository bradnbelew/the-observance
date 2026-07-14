package com.observance.watcher.v5runtime.mechanics;

import java.util.Objects;
import java.util.Set;

/** Read-only durable facts supplied to a single pure predicate evaluation. */
public record EvaluationEnvironment(Set<String> trueFlags, Set<String> inspectionBits) {
    public EvaluationEnvironment {
        trueFlags = Set.copyOf(Objects.requireNonNull(trueFlags, "trueFlags"));
        inspectionBits = Set.copyOf(Objects.requireNonNull(inspectionBits, "inspectionBits"));
    }

    public static EvaluationEnvironment empty() {
        return new EvaluationEnvironment(Set.of(), Set.of());
    }
}
