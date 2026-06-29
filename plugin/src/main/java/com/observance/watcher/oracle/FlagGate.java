package com.observance.watcher.oracle;

import java.util.Map;

/**
 * FlagGate — the storylet precondition predicate, dependency-free (no gson, no Bukkit) so it can be
 * unit-tested with javac alone. It is the Java TWIN of the Discord {@code oracle/gate.ts}
 * {@code flagsSatisfied}, and MUST stay byte-for-byte equivalent in behavior: both answer surfaces
 * gate identically (OVERHAUL.md §3), so a row that is closed on Discord is closed in-world and vice
 * versa. {@code FlagGateSelfTest} pins the parity against the same cases the TS {@code gate.selftest}
 * uses.
 *
 * <p>Contract: a puzzle row is OPEN ⟺ {@code active = true} AND every key in its {@code requires_flags}
 * is TRUTHY in {@code arc_state.flags}. The caller adapts gson {@code JsonObject}s into the flat
 * {@code Map<String,?>} shape this operates on (the requires_flags / flags blobs are flat by contract).
 */
public final class FlagGate {

    private FlagGate() { }

    /**
     * True iff every key in {@code requiresFlags} is truthy in {@code flags}. An empty/null
     * {@code requiresFlags} (the default {@code {}}) is always satisfied → ungated rows are unchanged.
     * Only the KEYS of {@code requiresFlags} matter (the seeds only ever set them to {@code true}); a
     * missing or falsy live flag keeps the row closed.
     */
    public static boolean satisfied(Map<String, ?> requiresFlags, Map<String, ?> flags) {
        if (requiresFlags == null || requiresFlags.isEmpty()) {
            return true;
        }
        for (String key : requiresFlags.keySet()) {
            Object live = (flags == null) ? null : flags.get(key);
            if (!truthy(live)) {
                return false;
            }
        }
        return true;
    }

    /**
     * JS-equivalent truthiness, matching the TS gate's {@code !flags[key]} exactly for every value the
     * flags blob can hold: {@code null}/absent → false; {@code Boolean} → itself; {@code Number} →
     * non-zero and non-NaN; {@code String} → non-empty (note: the string {@code "false"} is TRUTHY in
     * JS, so it is here too); any other non-null object → true. In practice the seeds only store boolean
     * {@code true}, so the boolean branch is the live path; the rest guarantees parity at the edges.
     */
    static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            return d != 0.0 && !Double.isNaN(d);
        }
        if (v instanceof String) return !((String) v).isEmpty();
        return true;
    }
}
