package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/** Service-role RPC result for one short-lived Minecraft identity proof. */
public final class IdentityLinkChallengeRow {
    @SerializedName("issue_state")
    public String issueState;

    @SerializedName("challenge_expires_at")
    public String challengeExpiresAt;

    public IdentityLinkChallengeRow() { }
}
