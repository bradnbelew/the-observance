package com.observance.watcher.v5runtime;

public final class P9CampPredicateSelfTest {
    private P9CampPredicateSelfTest() { }

    public static void main(String[] args) {
        var people = new P9CampPredicate.People(
                "admin custody", "camera frames", "builder countermark", "route memory");
        require(P9CampPredicate.validPeople(people), "four correctly assigned people must pass");
        require(!P9CampPredicate.validPeople(new P9CampPredicate.People(
                people.ash(), people.mkept(), people.rook(), people.wren())), "swapped owners must fail");

        var window = new P9CampPredicate.Window(
                "Rook private counter-mark", "Witness Spool intake", "Copperline public upload",
                "release board complete", "inside access; sender unknown");
        require(P9CampPredicate.validWindow(window), "authenticated chain and open boundary must pass");
        require(!P9CampPredicate.validWindow(new P9CampPredicate.Window(
                window.after(), window.crossing(), window.before(), window.readiness(), window.boundary())),
                "reversed chronology must fail");
        require(!P9CampPredicate.validWindow(new P9CampPredicate.Window(
                window.before(), window.crossing(), window.after(), window.readiness(), "Wren proven")),
                "premature attribution must fail");
        require(!P9CampPredicate.validPeople(new P9CampPredicate.People(
                "x".repeat(P9CampPredicate.MAX_FIELD_LENGTH + 1),
                people.ash(), people.rook(), people.wren())), "oversized field must fail");
        System.out.println("P9CampPredicateSelfTest OK - owners, chronology, claim boundary, and bounds pass");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
