package com.observance.watcher.npc;

import java.util.Set;

/** Exact V5 NPC ID/state/line-count contract. */
public final class V5DialogueCatalogSelfTest {
    private V5DialogueCatalogSelfTest() { }

    public static void main(String[] args) {
        require(V5DialogueCatalog.townsfolk().keySet().equals(
                Set.of("aro", "wenna", "coll", "dob", "old_pell")), "townsfolk IDs drifted");
        require(V5DialogueCatalog.wren().id().equals("wren"), "Wren is missing");
        require(V5DialogueCatalog.lineCount() == 69, "expected 69 exact dialogue lines");
        require(V5DialogueCatalog.townsperson("old-pell").anchorSite().equals("npc_old_pell_anchor"),
                "Old Pell anchor normalization drifted");
        require(V5DialogueCatalog.wren().states().size() == 10, "Wren needs ten V5 states");
        System.out.println("V5DialogueCatalogSelfTest OK - 6 NPCs, 69 exact lines");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
