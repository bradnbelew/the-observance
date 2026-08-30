package com.observance.watcher.morrow;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Immutable, restart-serializable relationship projection derived from durable event receipts. */
public record MorrowRelationshipSnapshot(
        String releaseId,
        MorrowStage stage,
        long revision,
        Set<MorrowCapability> capabilities,
        Set<String> committedEvents
) {
    public MorrowRelationshipSnapshot {
        if (releaseId == null || !releaseId.matches("[a-z0-9][a-z0-9._-]{6,79}")) {
            throw new IllegalArgumentException("Invalid release id");
        }
        Objects.requireNonNull(stage, "stage");
        if (revision < 0) {
            throw new IllegalArgumentException("revision must be non-negative");
        }
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities"));
        committedEvents = Set.copyOf(Objects.requireNonNull(committedEvents, "committedEvents"));
        if (!capabilities.contains(MorrowCapability.STATIC_RESTORE)) {
            throw new IllegalArgumentException("static restore is required in every relationship state");
        }
        for (MorrowStage authored : MorrowStage.values()) {
            boolean shouldHave = authored.ordinal() <= stage.ordinal();
            boolean has = capabilities.contains(authored.capability());
            if (shouldHave != has) {
                throw new IllegalArgumentException("capability set does not match stage " + stage.key());
            }
        }
    }

    public static MorrowRelationshipSnapshot initial(String releaseId) {
        return new MorrowRelationshipSnapshot(
                releaseId,
                MorrowStage.HELPFUL,
                0,
                EnumSet.of(MorrowCapability.STATIC_RESTORE),
                Set.of()
        );
    }

    public MorrowRelationshipSnapshot withCommittedEvent(String eventKey) {
        if (eventKey == null || !eventKey.matches("morrow\\.act[0-7]\\.[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid Morrow event key: " + eventKey);
        }
        if (committedEvents.contains(eventKey)) {
            return this;
        }
        Set<String> updatedEvents = new LinkedHashSet<>(committedEvents);
        updatedEvents.add(eventKey);
        return new MorrowRelationshipSnapshot(releaseId, stage, revision, capabilities, updatedEvents);
    }

    MorrowRelationshipSnapshot advanceTo(MorrowStage target) {
        if (target != stage.next() || stage.terminal()) {
            throw new IllegalArgumentException("relationship stages may advance exactly one step");
        }
        EnumSet<MorrowCapability> updatedCapabilities = EnumSet.copyOf(capabilities);
        updatedCapabilities.add(target.capability());
        return new MorrowRelationshipSnapshot(
                releaseId,
                target,
                revision + 1,
                updatedCapabilities,
                committedEvents
        );
    }
}

