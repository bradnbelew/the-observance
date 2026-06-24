package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * WRITE row for {@code public.answer_attempts} — the append-only audit + rate-limit substrate
 * (ORACLE.md §6). EVERY plausible attempt (matched or not) is logged, recording only a single
 * {@link #matched} boolean — NEVER which part was right or how close it was.
 *
 * <p>For the in-world surface {@link #surface} is always {@code "world"} and the player is keyed by
 * {@link #mcUuid}. An empty normalized result is NOT logged (it is not "plausibly an answer", per
 * ORACLE.md §2's gibberish guard).
 */
public final class AnswerAttemptRow {

    /** Which puzzle matched, if any. Null on a miss. */
    @SerializedName("puzzle_key")
    public String puzzleKey;

    /** The keeper id (players.id, uuid) when known; nullable. */
    @SerializedName("player_id")
    public String playerId;

    @SerializedName("mc_uuid")
    public String mcUuid;

    @SerializedName("discord_id")
    public String discordId;

    /** 'world' for the answer-sign surface, 'discord' for the bot. */
    @SerializedName("surface")
    public String surface;

    /** The player's exact input (audit). */
    @SerializedName("raw")
    public String raw;

    /** The normalized form actually matched (ORACLE.md §2). */
    @SerializedName("normalized")
    public String normalized;

    /** Did {@link #normalized} hit an accepted answer of an OPEN puzzle? */
    @SerializedName("matched")
    public Boolean matched;

    public AnswerAttemptRow() { }

    public AnswerAttemptRow(String puzzleKey, String playerId, String mcUuid, String discordId,
                            String surface, String raw, String normalized, Boolean matched) {
        this.puzzleKey = puzzleKey;
        this.playerId = playerId;
        this.mcUuid = mcUuid;
        this.discordId = discordId;
        this.surface = surface;
        this.raw = raw;
        this.normalized = normalized;
        this.matched = matched;
    }
}
