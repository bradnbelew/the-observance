package com.observance.watcher.v5runtime;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable local-primary V5 progress image. */
public record ProgressSnapshot(
        int schemaVersion,
        String campaignVersion,
        String manifestSha256,
        long revision,
        long updatedAtEpochMillis,
        Map<String, Boolean> booleans,
        Map<String, PlayerProgress> players,
        Map<String, String> branches,
        Map<String, BallotTelemetry> ballots,
        Map<String, EscrowEntry> escrow,
        Optional<ConductVerdict> conductVerdict) {

    public ProgressSnapshot {
        if (schemaVersion != V5ProgressStore.SCHEMA_VERSION) {
            throw new IllegalArgumentException("progress schema must be " + V5ProgressStore.SCHEMA_VERSION);
        }
        if (!PhysicalPredicateAuthority.CAMPAIGN_VERSION.equals(campaignVersion)) {
            throw new IllegalArgumentException("progress campaign must be v5");
        }
        Objects.requireNonNull(manifestSha256, "manifestSha256");
        if (!manifestSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("manifestSha256 must be lowercase SHA-256");
        }
        if (revision < 0 || updatedAtEpochMillis < 0) {
            throw new IllegalArgumentException("progress revision and timestamp cannot be negative");
        }
        booleans = Map.copyOf(Objects.requireNonNull(booleans, "booleans"));
        players = Map.copyOf(Objects.requireNonNull(players, "players"));
        branches = Map.copyOf(Objects.requireNonNull(branches, "branches"));
        ballots = Map.copyOf(Objects.requireNonNull(ballots, "ballots"));
        escrow = Map.copyOf(Objects.requireNonNull(escrow, "escrow"));
        conductVerdict = Objects.requireNonNull(conductVerdict, "conductVerdict");
    }

    public boolean isComplete(String flag) {
        return Boolean.TRUE.equals(booleans.get(flag));
    }
}
