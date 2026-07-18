package com.observance.watcher.v5runtime;

public final class P10WrenTransmissionPredicateSelfTest {
    private P10WrenTransmissionPredicateSelfTest() { }

    public static void main(String[] args) {
        var valid = new P10WrenTransmissionPredicate.Finding(
                "Wren knew Rook's private revision while its physical counter-mark was missing",
                "four packets grew from names to plans, routes, and fears",
                "fear of being erased explains the choice but does not excuse it; responsibility remains");
        require(P10WrenTransmissionPredicate.valid(valid), "supported attribution and motive boundary must pass");
        require(P10WrenTransmissionPredicate.valid(new P10WrenTransmissionPredicate.Finding(
                "Wren knew the north-brace private route when the physical mark was missing.",
                "Each packet knew more: people, then the build route, then a private worry.",
                "Being forgotten explains why he chose this. He is still responsible.")),
                "ordinary player-language attribution must pass");
        require(!P10WrenTransmissionPredicate.valid(new P10WrenTransmissionPredicate.Finding(
                "Wren said it", valid.pattern(), valid.motive())), "confession-only proof must fail");
        require(!P10WrenTransmissionPredicate.valid(new P10WrenTransmissionPredicate.Finding(
                valid.proof(), "one packet had a route", valid.motive())), "single-packet retrieval must fail");
        require(!P10WrenTransmissionPredicate.valid(new P10WrenTransmissionPredicate.Finding(
                valid.proof(), valid.pattern(), "fear means Wren is excused")), "absolution must fail");
        require(!P10WrenTransmissionPredicate.valid(new P10WrenTransmissionPredicate.Finding(
                "x".repeat(P10WrenTransmissionPredicate.MAX_FIELD_LENGTH + 1), valid.pattern(), valid.motive())),
                "oversized fields must fail");
        require(P10WrenTransmissionPredicate.response(new P10WrenTransmissionPredicate.Finding(
                "Wren confessed", valid.pattern(), valid.motive()))
                == P10WrenTransmissionPredicate.Response.CONFESSION_ONLY,
                "confession-only theory needs its own response");
        require(P10WrenTransmissionPredicate.response(new P10WrenTransmissionPredicate.Finding(
                "Rook knew the private revision", valid.pattern(), valid.motive()))
                == P10WrenTransmissionPredicate.Response.WRONG_SENDER,
                "wrong-sender theory needs its own response");
        require(P10WrenTransmissionPredicate.response(new P10WrenTransmissionPredicate.Finding(
                valid.proof(), "one packet had a route", valid.motive()))
                == P10WrenTransmissionPredicate.Response.SINGLE_PACKET,
                "single-packet theory needs its own response");
        require(P10WrenTransmissionPredicate.response(new P10WrenTransmissionPredicate.Finding(
                valid.proof(), valid.pattern(), "fear removes responsibility"))
                == P10WrenTransmissionPredicate.Response.ABSOLUTION,
                "fear-as-absolution theory needs its own response");
        System.out.println("P10WrenTransmissionPredicateSelfTest OK - provenance, progression, motive boundary, authored wrong theories, and bounds pass");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
