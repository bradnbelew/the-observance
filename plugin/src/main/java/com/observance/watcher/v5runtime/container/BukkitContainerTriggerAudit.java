package com.observance.watcher.v5runtime.container;

import com.observance.watcher.v5runtime.mechanics.BukkitFixtureIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Preflight hook proving CW07/HS02 have one real exact control rather than a touch/radius fallback. */
public final class BukkitContainerTriggerAudit {
    private BukkitContainerTriggerAudit() {
    }

    public static List<String> findings(BukkitFixtureIndex fixtures) {
        return findings(fixtures, ignored -> true);
    }

    /**
     * Audit only controls whose exact site chunks are currently loaded. The fixture index is a
     * loaded-chunk index by design, so treating an intentionally unloaded distant site as a
     * destroyed control would make cross-world readiness depend on chunk eviction order.
     */
    public static List<String> findings(BukkitFixtureIndex fixtures, Predicate<String> requireNode) {
        Objects.requireNonNull(fixtures, "fixtures");
        Objects.requireNonNull(requireNode, "requireNode");
        List<String> findings = new ArrayList<>();
        for (Map.Entry<String, String> required
                : ContainerTriggerBindings.requiredSyntheticComponents().entrySet()) {
            if (!requireNode.test(required.getKey())) continue;
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

    /** Pure fail-closed policy used by runtime and its dependency-light test matrix. */
    public static boolean requireForLoadedChunk(boolean authorityBindingKnown,
                                                 boolean siteKnownAndPlaced,
                                                 boolean worldLoaded,
                                                 boolean chunkLoaded) {
        if (!authorityBindingKnown || !siteKnownAndPlaced) return true;
        return worldLoaded && chunkLoaded;
    }
}
