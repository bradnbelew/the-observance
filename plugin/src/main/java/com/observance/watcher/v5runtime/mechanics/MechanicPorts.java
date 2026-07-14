package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.EscrowEntry;
import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.util.Set;
import java.util.UUID;

/** Narrow adapters that keep local persistence authoritative and external mirrors asynchronous. */
public final class MechanicPorts {
    private MechanicPorts() {
    }

    @FunctionalInterface
    public interface ExternalFlagSnapshot {
        boolean isTrue(String flag);
    }

    @FunctionalInterface
    public interface WorldState {
        MechanicObservation capture(
                PhysicalPredicateAuthority.Node node, UUID actor, Trigger trigger);
    }

    public interface WorldMutation {
        /** Applies latches, generated-receipt consumption, safe input return, and gate changes. */
        void applyAfterLocalCommit(
                PhysicalPredicateAuthority.Node node, UUID actor, MechanicObservation observation)
                throws Exception;

        /** Idempotently projects an already committed flag back into the Minecraft world. */
        void recoverCommitted(PhysicalPredicateAuthority.Node node) throws Exception;
    }

    public interface ArtifactDelivery {
        /** Exact tagged template including the supplied unique instance UUID. */
        MechanicItem template(String artifactId, UUID instanceId);

        /** Returns all discoverable instances across players, fixtures, and protected escrow. */
        Set<UUID> scanInstances(String artifactId);

        /** Delivers the pending entry or leaves it recoverable; never drops it into the world. */
        boolean deliverOrKeepEscrow(UUID actor, EscrowEntry pending) throws Exception;
    }

    @FunctionalInterface
    public interface AsyncMirror {
        /** Must enqueue and return; the external mirror is never in the local commit path. */
        void enqueue(PhysicalPredicateAuthority.Node node, long localRevision);
    }

    @FunctionalInterface
    public interface PlayerFeedback {
        void send(UUID actor, String safeMessage);
    }

    public enum Trigger {
        SIGN_SUBMIT,
        HANDLE,
        INVENTORY_CLOSE,
        ROUTE_COMPLETE,
        SIGHTLINE_TIMER,
        GROUP_WINDOW
    }
}
