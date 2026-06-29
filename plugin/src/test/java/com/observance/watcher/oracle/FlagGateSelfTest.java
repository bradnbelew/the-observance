package com.observance.watcher.oracle;

import java.util.HashMap;
import java.util.Map;

/**
 * Parity guard for {@link FlagGate} — proves the Java gate predicate matches the Discord
 * {@code oracle/gate.ts} {@code flagsSatisfied} (and its {@code gate.selftest.ts} cases) exactly, so
 * both answer surfaces gate identically (OVERHAUL.md §3). Dependency-free + main()-runnable, so it
 * compiles and runs with javac alone (no Paper/gson/JUnit on the classpath):
 *
 *   javac -d out plugin/.../oracle/FlagGate.java plugin/.../oracle/FlagGateSelfTest.java
 *   java  -cp out com.observance.watcher.oracle.FlagGateSelfTest
 *
 * Exits non-zero on any failed assertion.
 */
public final class FlagGateSelfTest {

    private static int failures = 0;

    private static void check(String label, boolean cond) {
        if (cond) {
            System.out.println("  ok   " + label);
        } else {
            failures++;
            System.out.println("  FAIL " + label);
        }
    }

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    public static void main(String[] args) {
        // 1. flagsSatisfied parity (same cases as gate.selftest.ts §1).
        check("ungated {} is always open", FlagGate.satisfied(map(), map()));
        check("ungated (null) is always open", FlagGate.satisfied(null, map("x", true)));
        check("gated, flag absent -> closed", !FlagGate.satisfied(map("iss_caught", true), map()));
        check("gated, flag set -> open", FlagGate.satisfied(map("iss_caught", true), map("iss_caught", true)));
        check("gated, flag falsy -> closed", !FlagGate.satisfied(map("iss_caught", true), map("iss_caught", false)));
        check("multi-key gate needs ALL truthy",
                !FlagGate.satisfied(map("a", true, "b", true), map("a", true))
                        && FlagGate.satisfied(map("a", true, "b", true), map("a", true, "b", true)));

        // 2. JS-equivalent truthiness at the edges.
        check("truthy(null) == false", !FlagGate.truthy(null));
        check("truthy(false) == false", !FlagGate.truthy(false));
        check("truthy(true) == true", FlagGate.truthy(true));
        check("truthy(0) == false", !FlagGate.truthy(0));
        check("truthy(1) == true", FlagGate.truthy(1));
        check("truthy(\"\") == false", !FlagGate.truthy(""));
        check("truthy(\"x\") == true", FlagGate.truthy("x"));

        // 3. The vertical-slice flag transition (mirror gate.selftest.ts §4): the M4 row's gate
        //    {iss_caught} is closed until the catch sets iss_caught, then open.
        Map<String, Object> boundWordGate = map("iss_caught", true);
        Map<String, Object> flags = map();
        check("slice: before the catch, the M4 gate is CLOSED", !FlagGate.satisfied(boundWordGate, flags));
        flags.put("iss_caught", true); // the catch's set_flags merge
        check("slice: after the catch, the M4 gate is OPEN", FlagGate.satisfied(boundWordGate, flags));

        if (failures > 0) {
            System.out.println("\nFlagGateSelfTest: " + failures + " FAILED");
            System.exit(1);
        }
        System.out.println("\nFlagGateSelfTest: OK — the Java gate predicate matches the TS gate exactly.");
    }
}
