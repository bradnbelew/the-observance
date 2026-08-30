package com.observance.watcher.morrow;

/** Authored capability ladder. A capability is a player-approved story permission, not an AI power. */
public enum MorrowCapability {
    STATIC_RESTORE("static_restore"),
    ENTITY_REPLAY("entity_replay"),
    LIVE_CAPTURE("live_capture"),
    ACCOUNT_CONTINUITY("account_continuity"),
    COLD_STORAGE_ACCESS("cold_storage_access"),
    BRANCH_GOVERNANCE("branch_governance");

    private final String key;

    MorrowCapability(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}

