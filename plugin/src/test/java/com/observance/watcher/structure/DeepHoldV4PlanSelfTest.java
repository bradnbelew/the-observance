package com.observance.watcher.structure;

import java.util.List;

/** Dependency-free executable guard for V4 ownership, orientation, and access invariants. */
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
                new DeepHoldV4Plan.ApproachCell(82, -40, 172),
                new DeepHoldV4Plan.ApproachCell(82, -40, 171));
        if (!expectedMaraLane.equals(maraLane)) {
            System.out.println("  FAIL mara_lectern_5 reserved lane: expected "
                    + expectedMaraLane + ", found " + maraLane);
            System.exit(1);
        }

        System.out.println("DeepHoldV4PlanSelfTest: OK - 76 fixtures, exact approach lanes, eight gates (six linear), one connected reversible graph.");
    }
}
