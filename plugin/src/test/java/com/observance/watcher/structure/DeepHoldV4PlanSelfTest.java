package com.observance.watcher.structure;

import java.util.List;

/** Dependency-free executable guard for compact V5 ownership, orientation, and access invariants. */
public final class DeepHoldV4PlanSelfTest {
    public static void main(String[] args) {
        List<String> errors = DeepHoldV4Plan.validate();
        if (!errors.isEmpty()) {
            for (String error : errors) System.out.println("  FAIL " + error);
            System.out.println("DeepHoldV4PlanSelfTest: " + errors.size() + " FAILED");
            System.exit(1);
        }

        DeepHoldV4Plan.Fixture maraFive = DeepHoldV4Plan.fixture("mara_lectern_5");
        List<DeepHoldV4Plan.ApproachCell> maraLane = DeepHoldV4Plan.approachCells(maraFive);
        List<DeepHoldV4Plan.ApproachCell> expectedMaraLane = List.of(
                new DeepHoldV4Plan.ApproachCell(49, -40, 139));
        if (!expectedMaraLane.equals(maraLane)) {
            System.out.println("  FAIL mara_lectern_5 reserved lane: expected "
                    + expectedMaraLane + ", found " + maraLane);
            System.exit(1);
        }

        if (DeepHoldV4Plan.VERSION != 6 || DeepHoldV4Plan.MAX_X > 76
                || DeepHoldV4Plan.MAX_Z > 233 || DeepHoldV4Plan.MIN_X < -76) {
            System.out.println("  FAIL compact physical envelope/revision regressed");
            System.exit(1);
        }

        System.out.println("DeepHoldV4PlanSelfTest: OK - compact V5 plan, 76 fixtures, exact approach lanes, eight gates (six linear), one connected reversible graph.");
    }
}
