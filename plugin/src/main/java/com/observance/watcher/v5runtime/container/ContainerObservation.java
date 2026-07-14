package com.observance.watcher.v5runtime.container;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Immutable, exact world snapshot captured while the owning site mutex is held. */
public record ContainerObservation(
        String nodeId,
        UUID actor,
        Map<String, Map<Integer, ContainerItem>> inventories,
        Map<String, Integer> frameRotations,
        Map<String, Integer> globalIdentityCounts,
        Set<String> trueFlags,
        Set<String> actorHeldEvidenceBits,
        int destinationCapacity,
        boolean actorLinked,
        boolean actorMatchesHandoff) {

    public ContainerObservation {
        requireText(nodeId, "nodeId");
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(inventories, "inventories");
        Map<String, Map<Integer, ContainerItem>> copied = new HashMap<>();
        inventories.forEach((component, slots) -> {
            requireText(component, "component");
            Objects.requireNonNull(slots, "slots");
            slots.forEach((slot, item) -> {
                if (slot == null || slot < 0 || item == null) {
                    throw new IllegalArgumentException("inventory slots/items must be valid");
                }
            });
            copied.put(component, Map.copyOf(slots));
        });
        inventories = Map.copyOf(copied);
        frameRotations = Map.copyOf(Objects.requireNonNull(frameRotations, "frameRotations"));
        globalIdentityCounts = Map.copyOf(
                Objects.requireNonNull(globalIdentityCounts, "globalIdentityCounts"));
        trueFlags = Set.copyOf(Objects.requireNonNull(trueFlags, "trueFlags"));
        actorHeldEvidenceBits = Set.copyOf(
                Objects.requireNonNull(actorHeldEvidenceBits, "actorHeldEvidenceBits"));
        if (destinationCapacity < 0) {
            throw new IllegalArgumentException("destinationCapacity cannot be negative");
        }
    }

    public Optional<ContainerItem> item(String component, int slot) {
        return Optional.ofNullable(inventories.getOrDefault(component, Map.of()).get(slot));
    }

    public int identityCount(String type, String id) {
        return globalIdentityCounts.getOrDefault(type + ':' + id, 0);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }
}
