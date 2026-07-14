package com.observance.watcher.v5runtime.container;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/** Paper-independent exact item image. Display text is intentionally never an identity input. */
public record ContainerItem(String material, int amount, Map<String, String> pdc) {
    public static final String ARTIFACT_ID = "v5_artifact_id";
    public static final String ARTIFACT_ALIAS = "artifact_id";
    public static final String ARTIFACT_INSTANCE = "v5_artifact_instance";
    public static final String EVIDENCE_ID = "v5_evidence_id";

    public ContainerItem {
        Objects.requireNonNull(material, "material");
        material = material.toUpperCase(Locale.ROOT);
        if (!material.matches("[A-Z0-9_]+") || amount < 1) {
            throw new IllegalArgumentException("invalid material or amount");
        }
        Objects.requireNonNull(pdc, "pdc");
        Map<String, String> normalized = new LinkedHashMap<>();
        pdc.forEach((key, value) -> {
            if (key == null || key.isBlank() || value == null || value.isBlank()) {
                throw new IllegalArgumentException("PDC keys and values must be non-blank");
            }
            String previous = normalized.put(key.toLowerCase(Locale.ROOT), value);
            if (previous != null && !previous.equals(value)) {
                throw new IllegalArgumentException("duplicate normalized PDC key " + key);
            }
        });
        pdc = Map.copyOf(normalized);
        String artifact = pdc.get(ARTIFACT_ID);
        String alias = pdc.get(ARTIFACT_ALIAS);
        if (artifact != null && alias != null && !artifact.equals(alias)) {
            throw new IllegalArgumentException("artifact identity aliases disagree");
        }
        String instance = pdc.get(ARTIFACT_INSTANCE);
        if (instance != null) {
            UUID.fromString(instance);
        }
    }

    public Optional<String> artifactId() {
        return Optional.ofNullable(pdc.getOrDefault(ARTIFACT_ID, pdc.get(ARTIFACT_ALIAS)));
    }

    public Optional<UUID> artifactInstance() {
        return Optional.ofNullable(pdc.get(ARTIFACT_INSTANCE)).map(UUID::fromString);
    }

    public Optional<String> evidenceId() {
        return Optional.ofNullable(pdc.get(EVIDENCE_ID));
    }

    public Optional<String> durableIdentity() {
        return artifactId().map(value -> "artifact:" + value)
                .or(() -> evidenceId().map(value -> "evidence:" + value));
    }

    public boolean matches(String expectedMaterial, int expectedAmount, Map<String, String> expectedPdc) {
        if (!material.equals(expectedMaterial.toUpperCase(Locale.ROOT)) || amount != expectedAmount) {
            return false;
        }
        for (Map.Entry<String, String> entry : expectedPdc.entrySet()) {
            if (!entry.getValue().equals(pdc.get(entry.getKey().toLowerCase(Locale.ROOT)))) {
                return false;
            }
        }
        return true;
    }

    public boolean hasUniqueArtifactIdentity() {
        boolean artifact = artifactId().isPresent();
        boolean evidence = evidenceId().isPresent();
        return artifact != evidence && (!artifact || artifactInstance().isPresent());
    }

    public ContainerItem withPdc(String key, String value) {
        Map<String, String> changed = new LinkedHashMap<>(pdc);
        changed.put(key.toLowerCase(Locale.ROOT), value);
        return new ContainerItem(material, amount, changed);
    }

    public String fingerprintSha256() {
        StringBuilder canonical = new StringBuilder(material).append('|').append(amount);
        new TreeMap<>(pdc).forEach((key, value) -> canonical
                .append('|').append(key).append('=').append(value));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
