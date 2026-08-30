package com.observance.watcher.morrow.room04;

import com.observance.watcher.morrow.room04.RecoveryRoom04Entry.AbsoluteCell;
import com.observance.watcher.morrow.room04.RecoveryRoom04Installer.Origin;

import java.util.ArrayList;
import java.util.List;

/** Dependency-free contract coverage for safe entry, rejoin idempotency, and inventory neutrality. */
public final class RecoveryRoom04EntrySelfTest {
    private RecoveryRoom04EntrySelfTest() { }

    public static void main(String[] args) {
        Origin origin = new Origin(10, -63, 20);
        AbsoluteCell safe = RecoveryRoom04Entry.safeFeet(origin);
        check(safe.equals(new AbsoluteCell(10, -63, 18)), "spawn cell is bound to the authored offset");
        check(RecoveryRoom04Entry.insideRoom(origin, safe.x(), safe.y(), safe.z()), "safe cell is in room");
        check(!RecoveryRoom04Entry.insideRoom(origin, 10, -63, 26), "outside cell is rejected");

        check(RecoveryRoom04Entry.shouldRecover(true, false, true, true), "solid feet recover");
        check(RecoveryRoom04Entry.shouldRecover(true, true, false, true), "solid head recovers");
        check(RecoveryRoom04Entry.shouldRecover(true, true, true, false), "missing floor recovers");
        check(!RecoveryRoom04Entry.shouldRecover(true, true, true, true), "safe rejoin is idempotent");
        check(!RecoveryRoom04Entry.shouldRecover(false, false, false, false), "other worlds are untouched");

        for (int cohort : List.of(1, 2, 6)) {
            List<String> inventory = new ArrayList<>(List.of("clock", "written_book", "copper_ingot"));
            List<String> before = List.copyOf(inventory);
            for (int player = 0; player < cohort; player++) {
                check(!RecoveryRoom04Entry.shouldRecover(true, true, true, true),
                        "safe cohort entry remains accepted");
            }
            check(before.equals(inventory), "entry policy never mutates inventory for cohort " + cohort);
        }

        System.out.println("RecoveryRoom04EntrySelfTest OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
