package com.observance.watcher.v5runtime.container;

/** Dependency-light matrix for loaded-chunk versus corrupt-control readiness. */
public final class BukkitContainerTriggerAuditSelfTest {
    private BukkitContainerTriggerAuditSelfTest() {
    }

    public static void main(String[] args) {
        check(BukkitContainerTriggerAudit.requireForLoadedChunk(false, false, false, false),
                "unknown authority must fail closed");
        check(BukkitContainerTriggerAudit.requireForLoadedChunk(true, false, false, false),
                "missing authored site must fail closed");
        check(!BukkitContainerTriggerAudit.requireForLoadedChunk(true, true, false, false),
                "unloaded world is deferred");
        check(!BukkitContainerTriggerAudit.requireForLoadedChunk(true, true, true, false),
                "unloaded distant chunk is deferred");
        check(BukkitContainerTriggerAudit.requireForLoadedChunk(true, true, true, true),
                "loaded exact chunk must be audited");
        System.out.println("BukkitContainerTriggerAuditSelfTest PASS - unloaded chunks defer; loaded controls remain strict");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
}
