package com.observance.watcher.data.rows;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * WRITE row for inserting a new {@code beat_queue} beat — the oracle is the PRODUCER that makes
 * {@code UnlockBeat} (until now dead code) fire. Mirrors the bot's {@code enqueueBeat} contract
 * (ORACLE.md §4) so a world-surface solve enqueues exactly what a Discord solve would.
 *
 * <p><b>status defaults to 'approved'</b>: a reward a player just earned must NEVER wait on a human
 * gate. The future showrunner's CONFIRM path uses 'pending'; the oracle's own player-earned unlocks
 * are 'approved' and fire on the very next poll. (The plugin enacting its own enqueued beat is fine —
 * the {@code BeatQueuePoller} picks it up on the next cycle and {@code RealBeatEnactor} dispatches it.)
 *
 * <p>Nulls are dropped by the client's Gson, so an omitted {@code site_id}/{@code mc_uuid}/{@code
 * priority} simply isn't sent.
 */
public final class BeatQueueInsertRow {

    /** Beat kind; "unlock" → the UnlockBeat dispatcher. */
    @SerializedName("type")
    public String type;

    /** Workflow state. Player-earned unlocks are 'approved' (fire immediately). */
    @SerializedName("status")
    public String status;

    /** Target player uuid (the solving keeper), nullable for world/ambient beats. */
    @SerializedName("mc_uuid")
    public String mcUuid;

    /** Optional target site id (from sites.yml). Nullable. */
    @SerializedName("site_id")
    public String siteId;

    /** Optional ordering hint (higher = sooner). Nullable. */
    @SerializedName("priority")
    public Integer priority;

    /** The jsonb payload (the UnlockBeat dispatcher recipe). Sent as a JSON value, not a string. */
    @SerializedName("payload")
    public JsonElement payload;

    public BeatQueueInsertRow() { }

    public BeatQueueInsertRow(String type, String status, String mcUuid, String siteId,
                              Integer priority, JsonElement payload) {
        this.type = type;
        this.status = status;
        this.mcUuid = mcUuid;
        this.siteId = siteId;
        this.priority = priority;
        this.payload = payload;
    }
}
