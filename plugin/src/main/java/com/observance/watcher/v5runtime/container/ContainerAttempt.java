package com.observance.watcher.v5runtime.container;

import java.util.Objects;
import java.util.Optional;

/** Safe outcome returned to Bukkit; it never contains a hidden answer or expected PDC value. */
public record ContainerAttempt(Status status, String message, Optional<ContainerCommitPlan> plan) {
    public ContainerAttempt {
        Objects.requireNonNull(status, "status");
        message = Objects.requireNonNullElse(message, "");
        plan = Objects.requireNonNull(plan, "plan");
        if ((status == Status.READY) != plan.isPresent()) {
            throw new IllegalArgumentException("only READY attempts carry a commit plan");
        }
    }

    public enum Status {
        READY,
        LOCKED,
        WRONG,
        BUSY,
        ALREADY_COMPLETE,
        COMMITTED,
        RECOVERY_PENDING,
        ERROR
    }

    public static ContainerAttempt ready(ContainerCommitPlan plan) {
        return new ContainerAttempt(Status.READY, "The filing accepts the complete arrangement.",
                Optional.of(plan));
    }

    public static ContainerAttempt of(Status status, String message) {
        return new ContainerAttempt(status, message, Optional.empty());
    }
}
