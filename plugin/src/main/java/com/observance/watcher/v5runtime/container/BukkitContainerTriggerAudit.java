package com.observance.watcher.v5runtime.container;

import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Preflight hook proving CW07/HS02 have one real exact control rather than a touch/radius fallback. */
public final class BukkitContainerTriggerAudit {
    private BukkitContainerTriggerAudit() {
    }

    public static List<String> findings(BukkitFixtureIndex fixtures) {
        Objects.requireNonNull(fixtures, "fixtures");
        List<String> findings = new ArrayList<>();
        for (Map.Entry<String, String> required
                : ContainerTriggerBindings.requiredSyntheticComponents().entrySet()) {
            // A non-tile control is indexed twice by design: once as the clickable block and
            // once as its invisible tagged marker used for restart-safe identity. Count only
            // the player-operable block here so one lever is not reported as two controls.
            int exact = (int) fixtures.bindings(required.getKey(), required.getValue()).stream()
                    .filter(binding -> binding.kind() == BukkitFixtureIndex.BindingKind.BLOCK).count();
            int compatibility = (int) fixtures.bindings(required.getKey(), "evaluation_handle").stream()
                    .filter(binding -> binding.kind() == BukkitFixtureIndex.BindingKind.BLOCK).count();
            if (exact + compatibility != 1) {
                findings.add(required.getKey() + " requires exactly one bound "
                        + required.getValue() + "/evaluation_handle control, found "
                        + (exact + compatibility));
            }
        }
        return List.copyOf(findings);
    }

    public static void requireReady(BukkitFixtureIndex fixtures) {
        List<String> findings = findings(fixtures);
        if (!findings.isEmpty()) {
            throw new IllegalStateException("V5 container trigger preflight failed: " + findings);
        }
    }

}
