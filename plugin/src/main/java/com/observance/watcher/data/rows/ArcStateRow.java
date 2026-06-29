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

    /**
     * The storylet world-state — the flat {@code {flag:true}} blob the progression gate reads
     * (0006_requires_flags.sql; OVERHAUL.md §3). The in-world resolver AND-tests each puzzle's
     * {@code requires_flags} against this (see {@code OracleResolver.firstMatch}). Read-only here;
     * the resolver WRITES flags via the {@code observance_merge_arc_flags} RPC, never by patching this.
     */
    @SerializedName("flags")
    public com.google.gson.JsonObject flags;

    @SerializedName("updated_at")
    public String updatedAt;

    public ArcStateRow() { }

    /** The arc flags flattened to a {@code Map<String,Object>} (empty if unset). Never throws. */
    public java.util.Map<String, Object> flagsMap() {
        return com.observance.watcher.oracle.JsonFlags.toMap(flags);
    }

    /** Current act with a safe default of 1 when unset. */
    public int actOrDefault() {
        return currentAct == null ? 1 : currentAct;
    }
}
