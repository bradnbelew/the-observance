package com.observance.watcher.v5runtime;

import java.util.Map;

public final class ArgEventPrerequisiteAliasesSelfTest {
    private ArgEventPrerequisiteAliasesSelfTest() { }

    public static void main(String[] args) {
        require(ArgEventPrerequisiteAliases.all().size() == 8, "P5-P11 alias count");
        require("p7.nessa_publicly_cleared".equals(
                ArgEventPrerequisiteAliases.storyEventFor("v5_case_c04_complete")), "P7 alias");
        require(ArgEventPrerequisiteAliases.storyEventFor("v5_case_c10_complete") == null,
                "C10/RP06 must never be aliased");
        Map<String, Boolean> resolved = ArgEventPrerequisiteAliases.resolvedAliases(Map.of(
                "p8.intervention_plan_accepted", true,
                "p8.hold_systems_repaired", false,
                "p11.averyn_restored_unbound", true));
        require(resolved.size() == 2, "only true story facts resolve");
        require(Boolean.TRUE.equals(resolved.get("v5_case_c05_complete")), "P8 plan alias");
        require(!resolved.containsKey("v5_case_c06_complete"), "false repair does not resolve");
        require(Boolean.TRUE.equals(resolved.get("v5_case_c09_complete")), "P11 alias");
        System.out.println("ArgEventPrerequisiteAliasesSelfTest OK - 8 one-way gates; C10 remains physical");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
