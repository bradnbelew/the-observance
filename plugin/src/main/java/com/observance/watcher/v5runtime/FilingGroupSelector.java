package com.observance.watcher.v5runtime;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Pure LS06 presence selection kept independent of Bukkit for exact contract testing. */
final class FilingGroupSelector {
    private FilingGroupSelector() {}

    static Set<UUID> select(Point desk, double radius, Iterable<Presence> candidates) {
        Objects.requireNonNull(desk, "desk");
        Objects.requireNonNull(candidates, "candidates");
        if (!Double.isFinite(radius) || radius <= 0.0) return Set.of();
        double radiusSquared = radius * radius;
        LinkedHashSet<UUID> selected = new LinkedHashSet<>();
        for (Presence candidate : candidates) {
            if (candidate == null || !candidate.online()
                    || !desk.worldId().equals(candidate.worldId())) continue;
            double dx = candidate.x() - desk.x();
            double dy = candidate.y() - desk.y();
            double dz = candidate.z() - desk.z();
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                selected.add(candidate.playerId());
            }
        }
        return Set.copyOf(selected);
    }

    record Point(UUID worldId, double x, double y, double z) {
        Point {
            Objects.requireNonNull(worldId, "worldId");
        }
    }

    record Presence(UUID playerId, UUID worldId, double x, double y, double z, boolean online) {
        Presence {
            Objects.requireNonNull(playerId, "playerId");
        }
    }
}
