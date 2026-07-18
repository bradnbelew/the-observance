package com.observance.watcher.v5runtime;

public final class P7NessaCorrectionPredicateSelfTest {
    private P7NessaCorrectionPredicateSelfTest() { }

    public static void main(String[] args) {
        var valid = new P7NessaCorrectionPredicate.Finding(
                "Genuine stock was diverted; substitute cloth first broke at the lower intake",
                "The relief shifts and complaint reports were edited",
                "Nessa followed procedure and reported it before the cloth started shedding");
        require(P7NessaCorrectionPredicate.valid(valid), "natural complete correction must pass");
        require(!P7NessaCorrectionPredicate.valid(new P7NessaCorrectionPredicate.Finding(
                valid.record(), valid.cause(), valid.conduct())), "field-swapped finding must fail");
        require(!P7NessaCorrectionPredicate.valid(new P7NessaCorrectionPredicate.Finding(
                valid.cause(), "the relief shift was edited", valid.conduct())), "missing complaint edit must fail");
        require(!P7NessaCorrectionPredicate.valid(new P7NessaCorrectionPredicate.Finding(
                "Nessa fouled the sink", valid.record(), "she reported late")), "accusation must fail");
        require(!P7NessaCorrectionPredicate.valid(new P7NessaCorrectionPredicate.Finding(
                "x".repeat(P7NessaCorrectionPredicate.MAX_FIELD_LENGTH + 1), valid.record(), valid.conduct())),
                "oversized field must fail");
        System.out.println("P7NessaCorrectionPredicateSelfTest OK - cause, edited record, conduct, swaps, and bounds pass");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
