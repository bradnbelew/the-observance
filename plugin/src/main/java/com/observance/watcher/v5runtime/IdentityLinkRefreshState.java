package com.observance.watcher.v5runtime;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe state for asynchronous Discord-link recognition.
 *
 * <p>It deduplicates network reads per player and keeps the last authoritative identity result.
 * An indeterminate read (outage, missing row, or authority mismatch) preserves that result. A
 * successful row read with a blank {@code discord_id}, however, is authoritative evidence that an
 * atomic Discord recovery moved the account to another Minecraft identity and must revoke the old
 * hand without requiring a server restart.</p>
 */
final class IdentityLinkRefreshState {
    enum Observation {
        LINKED,
        UNLINKED,
        INDETERMINATE
    }

    private final Set<UUID> linked = ConcurrentHashMap.newKeySet();
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    boolean begin(UUID actor, boolean authorityValidated) {
        // Linked players are intentionally revalidated by the online-player poll. Otherwise an
        // atomic /link recovery can clear their database binding while this process keeps granting
        // linked-only mechanics until the next restart.
        return actor != null && authorityValidated && inFlight.add(actor);
    }

    void finish(UUID actor, Observation observation) {
        if (actor == null) return;
        try {
            if (observation == Observation.LINKED) {
                linked.add(actor);
            } else if (observation == Observation.UNLINKED) {
                linked.remove(actor);
            }
            // INDETERMINATE deliberately preserves last-known-good authority.
        } finally {
            inFlight.remove(actor);
        }
    }

    boolean linked(UUID actor) {
        return actor != null && linked.contains(actor);
    }

    Set<UUID> snapshot() {
        return Set.copyOf(linked);
    }

    boolean inFlight(UUID actor) {
        return actor != null && inFlight.contains(actor);
    }
}
