package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code heatmap_cells} table — coarse-grid visit counts (DESIGN §2.2).
 * WRITE: upsert keyed on (world, cell_x, cell_z). Cell coords are in CELL units (block / cell-size),
 * not block coords.
 */
public final class HeatmapCellRow {

    @SerializedName("world")
    public String world;

    @SerializedName("cell_x")
    public Integer cellX;

    @SerializedName("cell_z")
    public Integer cellZ;

    @SerializedName("visits")
    public Long visits;

    @SerializedName("updated_at")
    public String updatedAt;

    public HeatmapCellRow() { }

    public HeatmapCellRow(String world, Integer cellX, Integer cellZ, Long visits, String updatedAt) {
        this.world = world;
        this.cellX = cellX;
        this.cellZ = cellZ;
        this.visits = visits;
        this.updatedAt = updatedAt;
    }
}
