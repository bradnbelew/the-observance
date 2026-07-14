package com.observance.watcher.v5runtime.mechanics;

import java.util.Objects;
import java.util.Optional;

/** Internal evaluation result; it deliberately never carries an accepted answer string. */
public record PredicateEvaluation(boolean satisfied, Optional<String> failedOperation) {
    public PredicateEvaluation {
        failedOperation = Objects.requireNonNull(failedOperation, "failedOperation");
        if (satisfied == failedOperation.isPresent()) {
            throw new IllegalArgumentException("evaluation result is inconsistent");
        }
    }

    public static PredicateEvaluation success() {
        return new PredicateEvaluation(true, Optional.empty());
    }

    public static PredicateEvaluation failure(String operation) {
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("failed operation cannot be blank");
        }
        return new PredicateEvaluation(false, Optional.of(operation));
    }
}
