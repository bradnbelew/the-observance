package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code beat_queue} table — pending/approved beats authored by the bot/dashboard.
 * READ: poll for status in (pending, approved). After enacting, PATCH status='fired' + decided_at.
 *
 * <p>Lore-AGNOSTIC: the plugin reads {@code type} + a {@code payload} JSON string and enacts it;
 * any story text lives inside {@code payload}/Supabase content, never in code. Known Phase-0 types
 * include {@code "whisper_toll"} and {@code "unlock"}; unknown types are skipped gracefully.
 */
public final class BeatQueueRow {

    @SerializedName("id")
    public String id;

    /** Beat kind, e.g. "whisper_toll", "unlock", "ambient", "report". */
    @SerializedName("type")
    public String type;

    /** Workflow state: "pending" | "approved" | "fired" | "skipped" | "failed". */
    @SerializedName("status")
    public String status;

    /** Optional target player uuid (per-player beats). Nullable. */
    @SerializedName("mc_uuid")
    public String mcUuid;

    /** Optional target site id (from sites.yml). Nullable. */
    @SerializedName("site_id")
    public String siteId;

    /** Opaque JSON-string payload the enactor interprets (text, sound keys, params). */
    @SerializedName("payload")
    public String payload;

    /** Optional priority hint (higher = sooner). Nullable. */
    @SerializedName("priority")
    public Integer priority;

    @SerializedName("created_at")
    public String createdAt;

    /** Set by the plugin when it fires/skips the beat. */
    @SerializedName("decided_at")
    public String decidedAt;

    public BeatQueueRow() { }
}
