package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code bases} table — a detected player/group base (DESIGN §2.2).
 * WRITE: upsert keyed on an id or (world, center). Anchored by bed/respawn + block-place/container
 * clustering. Beats prefer to land near these.
 */
public final class BaseRow {

    /** Stable id for the base (e.g. owner uuid or a deterministic cluster id). Nullable on insert. */
    @SerializedName("id")
    public String id;

    /** The player most associated with this base, nullable. */
    @SerializedName("owner_uuid")
    public String ownerUuid;

    @SerializedName("label")
    public String label;

    @SerializedName("world")
    public String world;

    @SerializedName("center_x")
    public Integer centerX;

    @SerializedName("center_y")
    public Integer centerY;

    @SerializedName("center_z")
    public Integer centerZ;

    @SerializedName("radius")
    public Integer radius;

    /** Heuristic confidence 0..1 that this cluster is really a base. */
    @SerializedName("confidence")
    public Double confidence;

    @SerializedName("updated_at")
    public String updatedAt;

    public BaseRow() { }
}
