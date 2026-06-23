package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code arc_state} table — the current act/movement of the arc. READ-only here.
 * Lore-agnostic: just an integer act + an opaque label the dashboard manages.
 */
public final class ArcStateRow {

    @SerializedName("id")
    public String id;

    /** Current act number (1..3 per FLOW; movements may map onto this). */
    @SerializedName("current_act")
    public Integer currentAct;

    /** Optional opaque label, e.g. "act2" / "movement_iii". Nullable. */
    @SerializedName("label")
    public String label;

    @SerializedName("updated_at")
    public String updatedAt;

    public ArcStateRow() { }

    /** Current act with a safe default of 1 when unset. */
    public int actOrDefault() {
        return currentAct == null ? 1 : currentAct;
    }
}
