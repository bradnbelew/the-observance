package com.observance.watcher.v5runtime;

public final class P11AverynIdentityPredicateSelfTest {
    private P11AverynIdentityPredicateSelfTest() { }

    public static void main(String[] args) {
        require(P11AverynIdentityPredicate.valid("AVERYN"), "exact artifact must pass");
        require(P11AverynIdentityPredicate.valid("  averyn  "), "case and outer whitespace may normalize");
        require(!P11AverynIdentityPredicate.valid("Averyn the seventh"), "role prose must fail");
        require(!P11AverynIdentityPredicate.valid("AVERIN"), "wrong artifact must fail");
        require(!P11AverynIdentityPredicate.valid("x".repeat(P11AverynIdentityPredicate.MAX_LENGTH + 1)),
                "oversized input must fail");
        System.out.println("P11AverynIdentityPredicateSelfTest OK - exact artifact and bounds pass without source state");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
