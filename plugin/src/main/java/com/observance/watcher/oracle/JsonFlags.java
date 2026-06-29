package com.observance.watcher.oracle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Map;

/**
 * The gson adapter that flattens a jsonb flags / requires_flags blob ({@link JsonObject}) into the
 * flat {@code Map<String,Object>} that the dependency-free {@link FlagGate} predicate operates on.
 * Kept separate from {@link FlagGate} so the predicate stays gson-free and unit-testable with javac
 * alone. Primitive values become Boolean/Number/String (matching {@link FlagGate#truthy}); any
 * non-primitive value stringifies (a non-empty, hence truthy, object — the flags are flat by
 * contract, so this is only a defensive edge). Never throws.
 */
public final class JsonFlags {

    private JsonFlags() { }

    /** Flatten a flags/requires_flags {@link JsonObject} to a {@code Map<String,Object>} (empty if null). */
    public static Map<String, Object> toMap(JsonObject obj) {
        Map<String, Object> m = new HashMap<>();
        if (obj == null) return m;
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            JsonElement v = e.getValue();
            if (v == null || v.isJsonNull()) {
                m.put(e.getKey(), null);
            } else if (v.isJsonPrimitive()) {
                JsonPrimitive p = v.getAsJsonPrimitive();
                if (p.isBoolean()) {
                    m.put(e.getKey(), p.getAsBoolean());
                } else if (p.isNumber()) {
                    m.put(e.getKey(), p.getAsNumber());
                } else {
                    m.put(e.getKey(), p.getAsString());
                }
            } else {
                m.put(e.getKey(), v.toString());
            }
        }
        return m;
    }
}
