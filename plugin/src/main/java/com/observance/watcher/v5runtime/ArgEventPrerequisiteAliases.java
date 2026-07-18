package com.observance.watcher.v5runtime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-way compatibility between story-first ARG events and legacy phase gate names.
 *
 * <p>Aliases satisfy prerequisites only. They never write or counterfeit a physical completion
 * flag. RP06/C10 is intentionally absent: the final release still requires its exact local rite.</p>
 */
public final class ArgEventPrerequisiteAliases {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("v5_case_c02_complete", "p5.civic_gallery_recurated"),
            Map.entry("v5_case_c03_complete", "p6.six_responsibilities_acknowledged"),
            Map.entry("v5_case_c04_complete", "p7.nessa_publicly_cleared"),
            Map.entry("v5_case_c05_complete", "p8.intervention_plan_accepted"),
            Map.entry("v5_case_c06_complete", "p8.hold_systems_repaired"),
            Map.entry("v5_case_c07_complete", "p9.leak_window_proven"),
            Map.entry("v5_case_c08_complete", "p10.wren_remembrance_committed"),
            Map.entry("v5_case_c09_complete", "p11.averyn_restored_unbound"));

    private ArgEventPrerequisiteAliases() { }

    public static String storyEventFor(String requestedFlag) {
        return ALIASES.get(requestedFlag);
    }

    public static Map<String, Boolean> resolvedAliases(Map<String, Boolean> facts) {
        Map<String, Boolean> resolved = new LinkedHashMap<>();
        ALIASES.forEach((legacy, event) -> {
            if (Boolean.TRUE.equals(facts.get(event))) resolved.put(legacy, true);
        });
        return Map.copyOf(resolved);
    }

    public static Map<String, String> all() {
        return ALIASES;
    }
}
