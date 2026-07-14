package com.observance.watcher.v5runtime;

/** Immutable first-ballot evidence required by the V5 conduct-verdict precedence. */
public record BallotTelemetry(
        int initialRosterCount,
        int maximumVisibleRosterCount,
        int firstBallotEligibleCount,
        int firstBallotCastCount,
        int firstBallotDistinctChoices,
        boolean firstBallotTied,
        int resolutionRounds,
        int disconnectResnapCount) {

    public BallotTelemetry {
        if (initialRosterCount < 1) {
            throw new IllegalArgumentException("initialRosterCount must be at least 1");
        }
        if (maximumVisibleRosterCount < initialRosterCount) {
            throw new IllegalArgumentException(
                    "maximumVisibleRosterCount cannot be below initialRosterCount");
        }
        if (firstBallotEligibleCount < 1
                || firstBallotEligibleCount > maximumVisibleRosterCount) {
            throw new IllegalArgumentException(
                    "firstBallotEligibleCount must be within the visible roster");
        }
        if (firstBallotCastCount < 0 || firstBallotCastCount > firstBallotEligibleCount) {
            throw new IllegalArgumentException(
                    "firstBallotCastCount must be within the eligible count");
        }
        int minimumDistinct = firstBallotCastCount == 0 ? 0 : 1;
        if (firstBallotDistinctChoices < minimumDistinct
                || firstBallotDistinctChoices > firstBallotCastCount) {
            throw new IllegalArgumentException(
                    "firstBallotDistinctChoices must match the number of cast votes");
        }
        if (firstBallotTied && firstBallotDistinctChoices < 2) {
            throw new IllegalArgumentException("a tied first ballot requires at least two choices");
        }
        if (resolutionRounds < 1) {
            throw new IllegalArgumentException("a committed ballot needs at least one resolution round");
        }
        if (disconnectResnapCount < 0) {
            throw new IllegalArgumentException("disconnectResnapCount cannot be negative");
        }
        if (firstBallotCastCount < firstBallotEligibleCount
                && resolutionRounds == 1
                && disconnectResnapCount == 0) {
            throw new IllegalArgumentException(
                    "an incomplete first ballot needs a later round or disconnect resnapshot");
        }
    }

    public boolean firstBallotComplete() {
        return firstBallotCastCount == firstBallotEligibleCount;
    }
}
