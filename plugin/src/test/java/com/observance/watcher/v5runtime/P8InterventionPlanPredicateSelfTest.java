package com.observance.watcher.v5runtime;

public final class P8InterventionPlanPredicateSelfTest {
    private P8InterventionPlanPredicateSelfTest() { }

    public static void main(String[] args) {
        var valid = new P8InterventionPlanPredicate.Plan(
                "Earlier fracture; heat stayed high; paired watch gap; delayed closure.",
                "The water sample held, but the unreviewed route was unsafe.",
                "The altered office shows behavior. It does not identify what the Dark is.",
                "Filter, paired lamps, pressure bypass, staff route.");
        require(P8InterventionPlanPredicate.valid(valid), "plain paraphrase must pass");
        require(P8InterventionPlanPredicate.valid(new P8InterventionPlanPredicate.Plan(
                "old fracture / unchanged heat load / empty watch / late routing",
                "surface proof valid; cut unsafe", "copying is proven; ontology open",
                "water then watch light then bypass then passage")), "short technical notes must pass");
        require(!P8InterventionPlanPredicate.valid(new P8InterventionPlanPredicate.Plan(
                valid.causes(), valid.iss(), valid.copyBoundary(),
                "staff route, pressure bypass, lamps, filter")), "unsafe reversed order must fail");
        require(!P8InterventionPlanPredicate.valid(new P8InterventionPlanPredicate.Plan(
                "Iss caused everything", valid.iss(), valid.copyBoundary(), valid.order())),
                "single-cause theory must fail");
        require(!P8InterventionPlanPredicate.valid(new P8InterventionPlanPredicate.Plan(
                valid.causes(), valid.copyBoundary(), valid.iss(), valid.order())),
                "field swapping must fail");
        require(!P8InterventionPlanPredicate.valid(new P8InterventionPlanPredicate.Plan(
                "x".repeat(P8InterventionPlanPredicate.MAX_FIELD_LENGTH + 1),
                valid.iss(), valid.copyBoundary(), valid.order())), "oversized prose must fail");
        System.out.println("P8InterventionPlanPredicateSelfTest OK - short paraphrases pass; unsafe, swapped, and oversized inputs fail");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
