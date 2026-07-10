package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/** Read-only projection of {@code public.solves} for in-game director progress reports. */
public final class SolveReadRow {

    @SerializedName("puzzle_key")
    public String puzzleKey;

    @SerializedName("player_id")
    public String playerId;

    @SerializedName("mc_uuid")
    public String mcUuid;

    @SerializedName("discord_id")
    public String discordId;

    @SerializedName("attempt_count")
    public Integer attemptCount;

    @SerializedName("solved_at")
    public String solvedAt;

    public SolveReadRow() { }
}
