package com.observance.watcher.v5runtime;

import java.util.Objects;
import java.util.Set;

/** Durable, per-linked-player evidence bits. */
public record PlayerProgress(
        Set<String> inspections,
        Set<String> topics,
        Set<String> routes,
        Set<String> sessionBits) {
    public PlayerProgress {
        inspections = immutableBits(inspections, "inspections");
        topics = immutableBits(topics, "topics");
        routes = immutableBits(routes, "routes");
        sessionBits = immutableBits(sessionBits, "sessionBits");
    }

    public static PlayerProgress empty() {
        return new PlayerProgress(Set.of(), Set.of(), Set.of(), Set.of());
    }

    public Set<String> bits(PlayerBitDomain domain) {
        Objects.requireNonNull(domain, "domain");
        return switch (domain) {
            case INSPECTION -> inspections;
            case TOPIC -> topics;
            case ROUTE -> routes;
            case SESSION -> sessionBits;
        };
    }

    private static Set<String> immutableBits(Set<String> values, String label) {
        Objects.requireNonNull(values, label);
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(label + " cannot contain a blank bit");
            }
        }
        return Set.copyOf(values);
    }
}
