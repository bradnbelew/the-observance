package com.observance.watcher.npc;

import java.util.Set;

/** Exact V5 NPC ID/state/line-count contract. */
public final class V5DialogueCatalogSelfTest {
    private V5DialogueCatalogSelfTest() { }

    public static void main(String[] args) {
        require(V5DialogueCatalog.townsfolk().keySet().equals(
                Set.of("aro", "wenna", "coll", "dob", "old_pell")), "townsfolk IDs drifted");
        require(V5DialogueCatalog.wren().id().equals("wren"), "Wren is missing");
        require(V5DialogueCatalog.lineCount() == 89, "expected 89 exact dialogue lines");
        require(V5DialogueCatalog.townsperson("old-pell").anchorSite().equals("npc_old_pell_anchor"),
                "Old Pell anchor normalization drifted");
        for (String id : Set.of("aro", "wenna", "coll", "dob")) {
            require(V5DialogueCatalog.townsperson(id).lines("after_p5").size() == 2,
                    id + " must have two exact P5 consequence lines");
        }
        for (String id : Set.of("aro", "wenna", "coll", "dob", "old_pell")) {
            require(V5DialogueCatalog.townsperson(id).lines("after_p11").size() == 2,
                    id + " must answer the P11 identity restoration in-world");
        }
        require(V5DialogueCatalog.wren().states().size() == 10, "Wren needs ten V5 states");
        System.out.println("V5DialogueCatalogSelfTest OK - 6 NPCs, 89 exact lines");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
