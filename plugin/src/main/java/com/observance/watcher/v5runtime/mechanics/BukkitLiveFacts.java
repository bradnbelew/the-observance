package com.observance.watcher.v5runtime.mechanics;

import com.observance.watcher.v5runtime.PhysicalPredicateAuthority;
import java.util.UUID;

/** Transient route, answer, handle, view-side, and collective facts owned by Bukkit listeners. */
@FunctionalInterface
public interface BukkitLiveFacts {
    void enrich(
            PhysicalPredicateAuthority.Node node,
            UUID actor,
            MechanicObservation.Builder observation);
}
