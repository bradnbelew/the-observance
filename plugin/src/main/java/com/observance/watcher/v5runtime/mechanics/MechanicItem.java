package com.observance.watcher.v5runtime.mechanics;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Paper-independent item image used by the predicate evaluator and recovery ledger. */
public record MechanicItem(
        String material,
        int amount,
        Map<String, String> pdc,
        Optional<UUID> artifactInstance) {
    public static final String ARTIFACT_ID = "v5_artifact_id";
    public static final String ARTIFACT_INSTANCE = "v5_artifact_instance";

    public MechanicItem {
        Objects.requireNonNull(material, "material");
        material = material.toUpperCase(Locale.ROOT);
        if (!material.matches("[A-Z0-9_]+") || amount < 1) {
            throw new IllegalArgumentException("invalid material or amount");
        }
        pdc = Map.copyOf(Objects.requireNonNull(pdc, "pdc"));
        artifactInstance = Objects.requireNonNull(artifactInstance, "artifactInstance");
        String encodedInstance = pdc.get(ARTIFACT_INSTANCE);
        if (encodedInstance != null) {
            UUID parsed = UUID.fromString(encodedInstance);
            if (artifactInstance.isPresent() && !artifactInstance.get().equals(parsed)) {
                throw new IllegalArgumentException("artifact instance disagrees with PDC");
            }
            artifactInstance = Optional.of(parsed);
        }
        if (pdc.containsKey(ARTIFACT_ID) && artifactInstance.isEmpty()) {
            throw new IllegalArgumentException("tagged artifacts require a unique instance UUID");
        }
    }

    public static MechanicItem ordinary(String material, int amount, Map<String, String> pdc) {
        return new MechanicItem(material, amount, pdc, Optional.empty());
    }

    public String fingerprintSha256() {
        StringBuilder canonical = new StringBuilder(material).append('|').append(amount);
        pdc.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> canonical
                .append('|').append(entry.getKey()).append('=').append(entry.getValue()));
        artifactInstance.ifPresent(value -> canonical.append("|instance=").append(value));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
