package com.observance.watcher.v5runtime;

public final class P6ResponsibilityPredicateSelfTest {
    private P6ResponsibilityPredicateSelfTest() { }

    public static void main(String[] args) {
        var valid = new P6ResponsibilityPredicate.Matrix(
                "Vaun tracked diverted cloth in the ledger, kept issue going, then delayed a challenge.",
                "Mara made the unsafe clean route edition but left the correction in her margin.",
                "Sella used teaching drawings to prove the lower intake and kept the sample sealed.",
                "Orin signed a conditional brace promise; the public copy scraped away his warning.",
                "Brann's paired lamps and Pell meal chit prove the watch timestamp was rewritten.",
                "Iss proved the surface recovered but hid route risk and the registrar objection.");
        require(P6ResponsibilityPredicate.valid(valid), "plain six-row responsibility model must pass");
        require(P6ResponsibilityPredicate.valid(new P6ResponsibilityPredicate.Matrix(
                "heat-order diversion / continuity / receipt balance",
                "substituted manual route / unsafe clean plate / margin correction",
                "survey intake / landmark transfer / lower jar sketch",
                "seam load / signed condition / brace replacement warning",
                "watch rota / bell eight rewritten / paired lamp",
                "surface reed true / unsafe route omitted / registrar warning")),
                "short professional notes must pass");
        require(!P6ResponsibilityPredicate.valid(new P6ResponsibilityPredicate.Matrix(
                valid.mara(), valid.vaun(), valid.sella(), valid.orin(), valid.brann(), valid.iss())),
                "swapped people must fail");
        require(!P6ResponsibilityPredicate.valid(new P6ResponsibilityPredicate.Matrix(
                "Vaun had a ledger", valid.mara(), valid.sella(), valid.orin(), valid.brann(), valid.iss())),
                "retrieved fact without compromise/correction must fail");
        require(!P6ResponsibilityPredicate.valid(new P6ResponsibilityPredicate.Matrix(
                "x".repeat(P6ResponsibilityPredicate.MAX_FIELD_LENGTH + 1),
                valid.mara(), valid.sella(), valid.orin(), valid.brann(), valid.iss())),
                "oversized field must fail");
        System.out.println("P6ResponsibilityPredicateSelfTest OK - paraphrase, short-note, swap, retrieval-only, and bounds paths pass");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
