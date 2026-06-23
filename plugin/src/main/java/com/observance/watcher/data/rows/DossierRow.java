package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code dossiers} table — the per-player signal aggregate (the "it knows me" data).
 * WRITE: upsert on mc_uuid. Numeric signal fields are flat for cheap upserts; richer structure
 * can live in {@code extra} as a JSON-string blob (lore-agnostic; no story text here).
 */
public final class DossierRow {

    @SerializedName("mc_uuid")
    public String mcUuid;

    @SerializedName("name")
    public String name;

    // --- core tracked signals (DESIGN §2.1). Nullable Long/Double → omitted when unset. ---

    @SerializedName("solo_mining_seconds")
    public Long soloMiningSeconds;

    @SerializedName("deaths")
    public Long deaths;

    @SerializedName("blocks_mined")
    public Long blocksMined;

    @SerializedName("hoarded_score")
    public Double hoardedScore;

    @SerializedName("distance_from_group")
    public Double distanceFromGroup;

    /** Free-form JSON string for additional signals without a schema change. */
    @SerializedName("extra")
    public String extra;

    @SerializedName("updated_at")
    public String updatedAt;

    public DossierRow() { }

    public DossierRow(String mcUuid, String name) {
        this.mcUuid = mcUuid;
        this.name = name;
    }
}
