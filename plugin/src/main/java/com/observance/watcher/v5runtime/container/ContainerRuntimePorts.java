package com.observance.watcher.v5runtime.container;

import com.observance.watcher.v5runtime.ProgressSnapshot;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Narrow runtime ports; the local V5 progress file remains the only authorization source. */
public final class ContainerRuntimePorts {
    private ContainerRuntimePorts() {
    }

    public interface World {
        ContainerObservation capture(
                ContainerAuthorityContract.NodeRule rule,
                UUID actor,
                Set<String> trueFlags,
                Set<String> heldEvidenceBits,
                boolean linked,
                boolean handoffMatch) throws Exception;

        /** Applies only after the local CAS. Returned ids are already present in actor custody. */
        Set<String> applyAfterCommit(
                ContainerAuthorityContract.NodeRule rule,
                UUID actor,
                ContainerCommitPlan plan) throws Exception;

        /** Moves the exact existing source stack through protected recovery escrow. */
        boolean applyPortableClaim(
                ContainerAuthorityContract.NodeRule rule,
                UUID actor,
                String component,
                int slot,
                ContainerItem item,
                String progressEscrowId) throws Exception;

        /** Idempotently reprojects locks and pending custody after a restart. */
        Set<String> recoverCommitted(
                ContainerAuthorityContract.NodeRule rule,
                ProgressSnapshot progress) throws Exception;

        Set<String> recoverPlayer(UUID playerId) throws Exception;
    }

    public interface ActorFacts {
        boolean linked(UUID actor);

        boolean matchesHandoff(UUID actor, String sourceFlag);
    }

    /** Remote/cross-surface facts are usable only with exact campaign and authority metadata. */
    @FunctionalInterface
    public interface ExternalPrerequisites {
        Optional<ValidatedSnapshot> current();

        static ExternalPrerequisites none() {
            return Optional::empty;
        }
    }

    public record ValidatedSnapshot(
            String campaignVersion,
            String authoritySha256,
            Map<String, Boolean> flags) {
        public ValidatedSnapshot {
            if (campaignVersion == null || campaignVersion.isBlank()
                    || authoritySha256 == null || !authoritySha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("external prerequisite metadata is invalid");
            }
            flags = Map.copyOf(flags);
        }
    }

    @FunctionalInterface
    public interface CommitEffects {
        /** Must project the just-committed local flag/gate without consulting a remote mirror. */
        void applyAfterCommit(ContainerAuthorityContract.NodeRule rule) throws Exception;
    }

    @FunctionalInterface
    public interface AsyncMirror {
        void enqueue(ContainerAuthorityContract.NodeRule rule, long localRevision);
    }

    @FunctionalInterface
    public interface Feedback {
        void send(UUID actor, String safeMessage);
    }

    @FunctionalInterface
    public interface Clock {
        long epochMillis();
    }

    public static ActorFacts denyUnlinked() {
        return new ActorFacts() {
            @Override
            public boolean linked(UUID actor) {
                return false;
            }

            @Override
            public boolean matchesHandoff(UUID actor, String sourceFlag) {
                return false;
            }
        };
    }
}
