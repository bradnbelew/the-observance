package com.observance.watcher.v5runtime.ritual;

import com.observance.watcher.v5runtime.ritual.RitualChoices.WrenOutcome;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Identity-preserving logical view of the exact copper Protocol Bridge item. */
public record ProtocolBridge(
        UUID instanceId,
        String fingerprintSha256,
        Map<String, String> pdc) {
    public static final String ARTIFACT_KEY = "v5_artifact_id";
    public static final String ARTIFACT_VALUE = "protocol_bridge";
    public static final String OUTCOME_KEY = "v5_wren_outcome";

    public ProtocolBridge {
        Objects.requireNonNull(instanceId, "instanceId");
        if (fingerprintSha256 == null || !fingerprintSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("bridge fingerprint must be lowercase SHA-256");
        }
        pdc = Map.copyOf(Objects.requireNonNull(pdc, "pdc"));
        if (!ARTIFACT_VALUE.equals(pdc.get(ARTIFACT_KEY))) {
            throw new IllegalArgumentException("item is not the canonical Protocol Bridge");
        }
        String outcome = pdc.get(OUTCOME_KEY);
        if (outcome != null) {
            WrenOutcome.fromWireValue(outcome);
        }
    }

    public Optional<WrenOutcome> outcome() {
        String value = pdc.get(OUTCOME_KEY);
        return value == null ? Optional.empty() : Optional.of(WrenOutcome.fromWireValue(value));
    }

    /** Returns a logical view of the same item identity with the immutable branch tag applied. */
    public ProtocolBridge retag(WrenOutcome outcome) {
        Objects.requireNonNull(outcome, "outcome");
        Optional<WrenOutcome> current = outcome();
        if (current.isPresent() && current.orElseThrow() != outcome) {
            throw new IllegalStateException("Protocol Bridge already carries " + current.orElseThrow());
        }
        Map<String, String> updated = new LinkedHashMap<>(pdc);
        updated.put(OUTCOME_KEY, outcome.wireValue());
        return new ProtocolBridge(instanceId, fingerprintSha256, updated);
    }
}
