package com.observance.watcher.v5runtime;

public final class P9CampPredicateSelfTest {
    private P9CampPredicateSelfTest() { }

    public static void main(String[] args) {
        var people = new P9CampPredicate.People(
                "admin custody", "camera frames", "builder countermark", "route memory");
        require(P9CampPredicate.validPeople(people), "four correctly assigned people must pass");
        require(!P9CampPredicate.validPeople(new P9CampPredicate.People(
                people.ash(), people.mkept(), people.rook(), people.wren())), "swapped owners must fail");
        require(P9CampPredicate.unsupportedPeople(new P9CampPredicate.People(
                people.ash(), people.mkept(), people.rook(), people.wren()))
                        .equals(java.util.List.of("mkept", "Ash")),
                "focused refusal must name only the swapped owner cards");
        require(P9CampPredicate.validPeople(new P9CampPredicate.People(
                "kept camera frames and notebook mirror", "photographed repaired brace face",
                "marked brace privately", "walked changed route")),
                "cross-person relationship phrasing must pass");

        var window = new P9CampPredicate.Window(
                "keep all three copies", "release board complete", "inside access; sender unknown");
        require(P9CampPredicate.validWindow(window), "preserved chain and open boundary must pass");
        require(!P9CampPredicate.validWindow(new P9CampPredicate.Window(
                "collapse to public", window.readiness(), window.boundary())),
                "destructive latest-copy treatment must fail");
        require(!P9CampPredicate.validWindow(new P9CampPredicate.Window(
                window.treatment(), window.readiness(), "Wren proven")),
                "premature attribution must fail");
        require(!P9CampPredicate.validPeople(new P9CampPredicate.People(
                "x".repeat(P9CampPredicate.MAX_FIELD_LENGTH + 1),
                people.ash(), people.rook(), people.wren())), "oversized field must fail");
        System.out.println("P9CampPredicateSelfTest OK - crossed relationships, focused owner refusal, custody treatment, claim boundary, and bounds pass");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
