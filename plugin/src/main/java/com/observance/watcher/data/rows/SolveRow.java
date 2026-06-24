package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * WRITE row for {@code public.solves} — the idempotency / replay guard (ORACLE.md §1, §6).
 *
 * <p>The resolver INSERTs this with {@code Prefer: resolution=ignore-duplicates} against the
 * {@code unique(puzzle_key, player_id)} constraint BEFORE enqueuing any reward. A genuinely-new
 * insert (HTTP 201, a returned row) means "first solve" → proceed to enqueue the beat. A conflict
 * (no new row) means "already solved" → say nothing, enqueue nothing. This makes solving the same
 * puzzle twice unable to double-fire its unlock, across BOTH surfaces (in-world + Discord), because
 * both write the same table keyed on {@code player_id}.
 *
 * <p>Nulls are dropped by the client's Gson, so unset denormalized fields are simply omitted.
 */
public final class SolveRow {

    @SerializedName("puzzle_key")
    public String puzzleKey;

    /** The canonical keeper id (players.id, uuid). Required — a solve is always a known keeper. */
    @SerializedName("player_id")
    public String playerId;

    @SerializedName("mc_uuid")
    public String mcUuid;

    /** Nullable; the keeper's linked discord id, denormalized for audit. */
    @SerializedName("discord_id")
    public String discordId;

    /** How many attempts preceded the solve (audit/balance). */
    @SerializedName("attempt_count")
    public Integer attemptCount;

    public SolveRow() { }

    public SolveRow(String puzzleKey, String playerId, String mcUuid,
                    String discordId, Integer attemptCount) {
        this.puzzleKey = puzzleKey;
        this.playerId = playerId;
        this.mcUuid = mcUuid;
        this.discordId = discordId;
        this.attemptCount = attemptCount;
    }
}
