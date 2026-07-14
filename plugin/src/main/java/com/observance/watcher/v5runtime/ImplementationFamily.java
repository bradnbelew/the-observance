package com.observance.watcher.v5runtime;

/** Coarse adapter families used to route each authority node to a future Bukkit implementation. */
public enum ImplementationFamily {
    S("answer and sign submissions"),
    I("source and evidence inspection"),
    F("frames, dials, lamps, and alignments"),
    L("lectern, affidavit, and document comparisons"),
    R("bounded routes, sightlines, and group presence"),
    N("stateful NPC interactions"),
    V("votes, collective choices, and finale state"),
    C("tagged containers, artifacts, and consoles");

    private final String description;

    ImplementationFamily(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
