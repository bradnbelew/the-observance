package com.observance.watcher.morrow;

import java.util.Arrays;

/** Canonical relationship order. Stages may advance one step only. */
public enum MorrowStage {
    HELPFUL("helpful", MorrowCapability.STATIC_RESTORE),
    CURIOUS("curious", MorrowCapability.ENTITY_REPLAY),
    INTIMATE("intimate", MorrowCapability.LIVE_CAPTURE),
    POSSESSIVE("possessive", MorrowCapability.ACCOUNT_CONTINUITY),
    AFRAID("afraid", MorrowCapability.COLD_STORAGE_ACCESS),
    NEGOTIATED("negotiated", MorrowCapability.BRANCH_GOVERNANCE);

    private final String key;
    private final MorrowCapability capability;

    MorrowStage(String key, MorrowCapability capability) {
        this.key = key;
        this.capability = capability;
    }

    public String key() {
        return key;
    }

    public MorrowCapability capability() {
        return capability;
    }

    public boolean terminal() {
        return this == NEGOTIATED;
    }

    public MorrowStage next() {
        if (terminal()) {
            return this;
        }
        return values()[ordinal() + 1];
    }

    public static MorrowStage fromKey(String key) {
        return Arrays.stream(values())
                .filter(stage -> stage.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Morrow stage: " + key));
    }
}

