package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/** Read-only projection of {@code public.answer_attempts} for director-only stuck/progress reports. */
public final class AnswerAttemptReadRow {

    @SerializedName("puzzle_key")
    public String puzzleKey;

    @SerializedName("player_id")
    public String playerId;

    @SerializedName("mc_uuid")
    public String mcUuid;

    @SerializedName("discord_id")
    public String discordId;

    @SerializedName("surface")
    public String surface;

    @SerializedName("raw")
    public String raw;

    @SerializedName("normalized")
    public String normalized;

    @SerializedName("matched")
    public Boolean matched;

    @SerializedName("at")
    public String at;

    public AnswerAttemptReadRow() { }
}
