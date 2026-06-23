package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code event_log} table. WRITE-only from the plugin: every notable action AND
 * every swallowed error lands here. Kept tiny + flat so inserts are cheap and never block.
 */
public final class EventLogRow {

    /** Machine tag: "error", "warn", "info", "beat_fired", "compliance", etc. */
    @SerializedName("type")
    public String type;

    /** Call-site / subsystem context, e.g. "listener.BlockBreak" or "beat.poller". */
    @SerializedName("context")
    public String context;

    /** Human-readable detail. Bounded length upstream. */
    @SerializedName("message")
    public String message;

    /** Optional player this event concerns (uuid string), nullable. */
    @SerializedName("mc_uuid")
    public String mcUuid;

    /** Optional arbitrary JSON-as-string detail blob, nullable. */
    @SerializedName("detail")
    public String detail;

    /** ISO-8601 UTC timestamp. */
    @SerializedName("created_at")
    public String createdAt;

    public EventLogRow() { }

    public EventLogRow(String type, String context, String message,
                       String mcUuid, String detail, String createdAt) {
        this.type = type;
        this.context = context;
        this.message = message;
        this.mcUuid = mcUuid;
        this.detail = detail;
        this.createdAt = createdAt;
    }
}
