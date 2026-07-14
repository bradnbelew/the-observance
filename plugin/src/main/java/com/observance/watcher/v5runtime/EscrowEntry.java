package com.observance.watcher.v5runtime;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Local recovery record for a protected item that must never be lost or duplicated. */
public record EscrowEntry(
        String escrowId,
        String artifactId,
        Optional<UUID> intendedPlayer,
        String sourceSiteId,
        int sourceSlot,
        String itemFingerprintSha256,
        int amount,
        long createdAtEpochMillis,
        long updatedAtEpochMillis,
        EscrowStatus status,
        Map<String, String> metadata) {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    public EscrowEntry {
        requireText(escrowId, "escrowId");
        requireText(artifactId, "artifactId");
        Objects.requireNonNull(intendedPlayer, "intendedPlayer");
        requireText(sourceSiteId, "sourceSiteId");
        if (sourceSlot < -1) {
            throw new IllegalArgumentException("sourceSlot cannot be below -1");
        }
        if (itemFingerprintSha256 == null || !SHA256.matcher(itemFingerprintSha256).matches()) {
            throw new IllegalArgumentException("itemFingerprintSha256 must be lowercase SHA-256");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("escrow amount must be positive");
        }
        if (createdAtEpochMillis < 0 || updatedAtEpochMillis < createdAtEpochMillis) {
            throw new IllegalArgumentException("invalid escrow timestamps");
        }
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(metadata, "metadata");
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            requireText(entry.getKey(), "metadata key");
            requireText(entry.getValue(), "metadata value");
        }
        metadata = Map.copyOf(metadata);
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }
}
