package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code custom_compliance} table — per-player, per-custom honored/violated tally.
 * WRITE: upsert keyed on (mc_uuid, custom_key). Lore-agnostic: {@code custom_key} is an opaque
 * identifier (e.g. "the_bow"); the human name/text lives in config/Supabase content, not here.
 */
public final class CustomComplianceRow {

    @SerializedName("mc_uuid")
    public String mcUuid;

    @SerializedName("name")
    public String name;

    /** Opaque custom identifier, e.g. "the_bow", "the_offering", "kept_light". */
    @SerializedName("custom_key")
    public String customKey;

    @SerializedName("honored_count")
    public Long honoredCount;

    @SerializedName("violated_count")
    public Long violatedCount;

    /** ISO-8601 of the most recent honor/violation. */
    @SerializedName("last_event_at")
    public String lastEventAt;

    @SerializedName("updated_at")
    public String updatedAt;

    public CustomComplianceRow() { }

    public CustomComplianceRow(String mcUuid, String name, String customKey) {
        this.mcUuid = mcUuid;
        this.name = name;
        this.customKey = customKey;
    }
}
