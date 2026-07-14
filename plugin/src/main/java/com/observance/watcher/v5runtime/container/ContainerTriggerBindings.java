package com.observance.watcher.v5runtime.container;

import java.util.Map;
import java.util.Set;

/**
 * Explicit trigger components required in addition to authored inventory components. The installer
 * must bind these as exact protected controls; the listener never substitutes an arbitrary click.
 */
public final class ContainerTriggerBindings {
    private static final Map<String, String> REQUIRED_SYNTHETIC = Map.of(
            "CW07", "cache_seal",
            "HS02", "housing_latch");

    private ContainerTriggerBindings() {
    }

    public static Map<String, String> requiredSyntheticComponents() {
        return REQUIRED_SYNTHETIC;
    }

    public static Set<String> acceptedComponents(String nodeId, boolean authoredHandle) {
        String required = REQUIRED_SYNTHETIC.get(nodeId);
        if (required != null) {
            // evaluation_handle is retained as an exact compatibility alias for a catalog-generated lever.
            return Set.of(required, "evaluation_handle");
        }
        return Set.of(authoredHandle ? "handle" : "evaluation_handle");
    }
}
