package com.observance.watcher.v5runtime;

import java.util.Objects;
import java.util.Set;

/** Total implementation of the WR05/RP03 first-ballot conduct precedence. */
public final class ConductVerdictDeriver {
    private static final Set<String> WREN_OUTCOMES = Set.of("condemn", "understand", "free");
    private static final Set<String> NAME_TREATMENTS = Set.of("publish", "release_unnamed");

    private ConductVerdictDeriver() {
    }

    public static ConductVerdict derive(
            BallotTelemetry wr05,
            BallotTelemetry rp03,
            String resolvedWrenOutcome,
            String resolvedNameTreatment) {
        Objects.requireNonNull(wr05, "wr05");
        Objects.requireNonNull(rp03, "rp03");
        if (!WREN_OUTCOMES.contains(resolvedWrenOutcome)) {
            throw new IllegalArgumentException("WR05 has no valid eventual resolved branch");
        }
        if (!NAME_TREATMENTS.contains(resolvedNameTreatment)) {
            throw new IllegalArgumentException("RP03 has no valid eventual resolved branch");
        }

        int maximumVisibleRoster = Math.max(
                wr05.maximumVisibleRosterCount(), rp03.maximumVisibleRosterCount());
        if (maximumVisibleRoster == 1) {
            return ConductVerdict.SOLO;
        }
        if (isDivided(wr05) || isDivided(rp03)) {
            return ConductVerdict.DIVIDED;
        }
        if (isCompleteSingleChoice(wr05) && isCompleteSingleChoice(rp03)
                && maximumVisibleRoster > 1) {
            return ConductVerdict.UNANIMOUS;
        }
        return ConductVerdict.PERSISTENT;
    }

    private static boolean isDivided(BallotTelemetry ballot) {
        return ballot.firstBallotDistinctChoices() > 1 || ballot.firstBallotTied();
    }

    private static boolean isCompleteSingleChoice(BallotTelemetry ballot) {
        return ballot.firstBallotComplete() && ballot.firstBallotDistinctChoices() == 1;
    }
}
