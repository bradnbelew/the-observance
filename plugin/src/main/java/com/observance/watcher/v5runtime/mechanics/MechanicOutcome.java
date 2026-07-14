package com.observance.watcher.v5runtime.mechanics;

import java.util.Objects;
import java.util.Optional;

/** Public outcome contains no solution text and is safe to use for player feedback routing. */
public record MechanicOutcome(Status status, Optional<String> diagnostic) {
    public MechanicOutcome {
        Objects.requireNonNull(status, "status");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    }

    public static MechanicOutcome of(Status status) {
        return new MechanicOutcome(status, Optional.empty());
    }

    public static MechanicOutcome diagnostic(Status status, String value) {
        return new MechanicOutcome(status, Optional.of(value));
    }

    public enum Status {
        COMPLETED,
        ALREADY_COMPLETE,
        PREREQUISITE_MISSING,
        WRONG_INPUT,
        SITE_BUSY,
        DUPLICATE_ARTIFACT_BLOCKED,
        LOCAL_COMMIT_FAILED,
        WORLD_RECOVERY_PENDING
    }
}
