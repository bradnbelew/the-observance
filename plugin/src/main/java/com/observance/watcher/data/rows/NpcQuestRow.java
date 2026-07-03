package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code npc_quests} table — per-player, per-quest errand state (the townsfolk
 * TRACKED QUESTS, Wave S-G). WRITE: upsert keyed on {@code (player_id, quest_key)} so an offer →
 * active → done progression is durable across sessions (a quest is a multi-session errand).
 *
 * <p><b>Keys.</b> {@code player_id} is the canonical {@code players.id} (uuid) — NOT the mc_uuid —
 * matching the FK the table declares; callers resolve mc_uuid → players.id (via
 * {@link com.observance.watcher.data.SupabaseClient#fetchPlayerByUuid}) before writing, exactly like
 * the oracle solve path keys on {@code players.id}. {@code quest_key} is the opaque quest id
 * ({@code wenna_crust} / {@code coll_lamp}); {@code status} is one of {@code offered|active|done|failed}
 * (DB check constraint).
 *
 * <p>Nulls are dropped by the client's Gson, so unset fields are simply omitted.
 */
public final class NpcQuestRow {

    /** The canonical keeper id (players.id, uuid). Required — the FK target of npc_quests. */
    @SerializedName("player_id")
    public String playerId;

    /** Opaque quest identifier, e.g. "wenna_crust", "coll_lamp". */
    @SerializedName("quest_key")
    public String questKey;

    /** One of offered | active | done | failed (DB-checked). */
    @SerializedName("status")
    public String status;

    @SerializedName("updated_at")
    public String updatedAt;

    public NpcQuestRow() { }

    public NpcQuestRow(String playerId, String questKey, String status) {
        this.playerId = playerId;
        this.questKey = questKey;
        this.status = status;
    }
}
