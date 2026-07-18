package com.observance.watcher.v5runtime;

public final class P3SettlementDispatchPredicateSelfTest {
    private P3SettlementDispatchPredicateSelfTest() { }

    public static void main(String[] args) {
        require(P3SettlementDispatchPredicate.valid(
                "Aro and Pell disagree about the mark date; keep both accounts open"),
                "plain open disagreement must pass");
        require(P3SettlementDispatchPredicate.valid(
                "The accounts conflict on where the work mark stood. Record both without choosing an official version"),
                "natural equivalent must pass");
        require(P3SettlementDispatchPredicate.valid(
                "Their dates for the repair mark contradict each other. We cannot settle this yet."),
                "ordinary uncertainty wording must pass");
        require(!P3SettlementDispatchPredicate.valid("They disagree, and Aro is clearly right"),
                "premature official choice must fail");
        require(!P3SettlementDispatchPredicate.valid("Keep the mark open"),
                "missing disagreement must fail");
        require(!P3SettlementDispatchPredicate.valid("x".repeat(P3SettlementDispatchPredicate.MAX_LENGTH + 1)),
                "oversized input must fail");
        System.out.println("P3SettlementDispatchPredicateSelfTest OK - disagreement, open boundary, and bounds pass");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
