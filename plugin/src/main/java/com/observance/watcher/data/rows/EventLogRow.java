package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code event_log} table. WRITE-only from the plugin: every notable action AND
 * every swallowed error lands here. Kept tiny + flat so inserts are cheap and never block.
 *
 * <p><b>Schema contract.</b> The live table is exactly {@code (level, source, message, created_at)}
 * where {@code level} carries a CHECK constraint of {@code ('info','warn','error')}
 * (dashboard {@code 0001_init.sql:115-121}). The plugin historically constructed rows with a rich
 * {@code (type, context, message, mc_uuid, detail)} shape whose columns do not exist — every insert
 * 400'd (PGRST204) and was silently lost. This class now serializes ONLY the four real columns while
 * keeping the original 6-arg constructor so no call site changes: the free-form {@code type} tag is
 * normalized into a constraint-legal {@code level} (and preserved verbatim inside {@code message}),
 * {@code context} maps to {@code source}, and the optional {@code mcUuid}/{@code detail} are folded
 * into {@code message} so no information is dropped even though the table has no column for them.
 */
public final class EventLogRow {

    /** Severity — MUST satisfy the table CHECK ('info','warn','error'). Serialized. */
    @SerializedName("level")
    public String level;

    /** Call-site / subsystem context, e.g. "listener.BlockBreak" or "beat.poller". Serialized as source. */
    @SerializedName("source")
    public String source;

    /** Human-readable detail (original type tag + message + optional uuid/detail folded in). Serialized. */
    @SerializedName("message")
    public String message;

    /** ISO-8601 UTC timestamp. Serialized. */
    @SerializedName("created_at")
    public String createdAt;

    public EventLogRow() { }

    /**
     * Historical 6-arg shape kept intact so no call site changes. Maps onto the real four columns:
     * {@code type}→{@code level} (normalized to info/warn/error, original tag preserved in message),
     * {@code context}→{@code source}, and {@code mcUuid}/{@code detail} folded into {@code message}.
     */
    public EventLogRow(String type, String context, String message,
                       String mcUuid, String detail, String createdAt) {
        this.level = normalizeLevel(type);
        this.source = context;
        this.message = composeMessage(type, message, mcUuid, detail);
        this.createdAt = createdAt;
    }

    /**
     * Coerce an arbitrary event {@code type} tag into a value the table's CHECK constraint accepts.
     * Recognizes obvious error/warn tags; everything else (including narrative tags like
     * "companion"/"keeper"/"finale") is {@code info}. The original tag is never lost — it is prefixed
     * into {@code message} by {@link #composeMessage}.
     */
    static String normalizeLevel(String type) {
        if (type == null) return "info";
        String t = type.trim().toLowerCase(java.util.Locale.ROOT);
        if (t.equals("error") || t.equals("err") || t.equals("severe")
                || t.equals("fatal") || t.equals("exception") || t.equals("transgression")) {
            return "error";
        }
        if (t.equals("warn") || t.equals("warning")) {
            return "warn";
        }
        return "info";
    }

    /**
     * Fold the original type tag and the optional (columnless) mcUuid/detail into the single
     * {@code message} column so nothing the caller passed is dropped. Shape:
     * {@code [type] message (uuid=..) detail}. Null-safe; empties are omitted.
     */
    static String composeMessage(String type, String message, String mcUuid, String detail) {
        StringBuilder sb = new StringBuilder();
        if (type != null && !type.isBlank()) sb.append('[').append(type.trim()).append("] ");
        if (message != null) sb.append(message);
        if (mcUuid != null && !mcUuid.isBlank()) sb.append(" (uuid=").append(mcUuid.trim()).append(')');
        if (detail != null && !detail.isBlank()) sb.append(' ').append(detail.trim());
        return sb.toString();
    }
}
