package com.observance.watcher.v5runtime.container;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Durable inventory/reward consequences applied only after the local completion CAS commits. */
public record ContainerCommitPlan(List<ItemDisposition> items, boolean latchInventories) {
    public ContainerCommitPlan {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
    }

    public enum Mode {
        HOLD_IN_FIXTURE,
        DELIVER_EXISTING_TO_ACTOR,
        DELIVER_NEW_ARTIFACT_TO_ACTOR
    }

    public record ItemDisposition(
            Mode mode,
            String component,
            int slot,
            String identityId,
            String material,
            Optional<ContainerItem> existingItem,
            Optional<UUID> generatedInstance,
            String escrowId) {
        public ItemDisposition {
            Objects.requireNonNull(mode, "mode");
            component = Objects.requireNonNullElse(component, "");
            if (slot < -1) {
                throw new IllegalArgumentException("slot cannot be below -1");
            }
            requireText(identityId, "identityId");
            requireText(material, "material");
            existingItem = Objects.requireNonNull(existingItem, "existingItem");
            generatedInstance = Objects.requireNonNull(generatedInstance, "generatedInstance");
            requireText(escrowId, "escrowId");
            if (mode == Mode.DELIVER_NEW_ARTIFACT_TO_ACTOR) {
                if (existingItem.isPresent() || generatedInstance.isEmpty() || slot != -1) {
                    throw new IllegalArgumentException("generated delivery shape is invalid");
                }
            } else if (existingItem.isEmpty() || generatedInstance.isPresent() || slot < 0
                    || component.isBlank()) {
                throw new IllegalArgumentException("existing item disposition shape is invalid");
            }
        }

        private static void requireText(String value, String label) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(label + " cannot be blank");
            }
        }
    }
}
