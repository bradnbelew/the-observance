package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code players} table. WRITE: upsert on join (mc_uuid, name, last_seen).
 * Field names map to PostgREST/JSON column names via @SerializedName. Extra DB columns are
 * ignored on read; unset fields are omitted on write by the client's Gson (nulls dropped).
 */
public final class PlayerRow {

    @SerializedName("mc_uuid")
    public String mcUuid;

    @SerializedName("name")
    public String name;

    /** ISO-8601 timestamp string (UTC). Server may also default this; we set it explicitly. */
    @SerializedName("last_seen")
    public String lastSeen;

    public PlayerRow() { }

    public PlayerRow(String mcUuid, String name, String lastSeen) {
        this.mcUuid = mcUuid;
        this.name = name;
        this.lastSeen = lastSeen;
    }
}
