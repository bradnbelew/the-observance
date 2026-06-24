package com.observance.watcher.data.rows;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code beat_queue} table — story beats authored by the bot/dashboard/showrunner.
 * READ: the poller fetches {@code status='approved'} ONLY (the approval gate); {@code 'pending'}
 * beats wait for a human to approve them in the dashboard. After enacting, PATCH status='fired' +
 * decided_at. Player-earned beats (oracle unlocks, whisper tolls) are inserted already-approved.
 *
 * <p>Lore-AGNOSTIC: the plugin reads {@code type} + a {@code payload} JSON value and enacts it;
 * any story text lives inside {@code payload}/Supabase content, never in code. Known Phase-0 types
 * include {@code "whisper_toll"} and {@code "unlock"}; unknown types are skipped gracefully.
 *
 * <p><b>Payload typing (load-bearing).</b> {@code beat_queue.payload} is a Postgres {@code jsonb}
 * column, so PostgREST returns it as a JSON <i>object</i>, not a JSON string. Deserializing that
 * into a {@code String} field makes Gson throw ("Expected a string but was BEGIN_OBJECT"), which
 * the read path turns into a parse error and drops the ENTIRE beat list — silently breaking even
 * the existing {@code whisper_toll} beats. We therefore type {@code payload} as a {@link JsonElement}
 * and let {@code RealBeatEnactor} feed it straight into {@code BeatPayload.of(...)}. See
 * {@link #payloadString()} for the legacy string view.
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

    /**
     * Opaque JSON payload the enactor interprets (text, sound keys, params). Read as a
     * {@link JsonElement} because the column is {@code jsonb} (see class doc). Usually a JSON
     * object; tolerant of null / non-object via {@link #payloadObject()}.
     */
    @SerializedName("payload")
    public JsonElement payload;

    /** Optional priority hint (higher = sooner). Nullable. */
    @SerializedName("priority")
    public Integer priority;

    @SerializedName("created_at")
    public String createdAt;

    /** Set by the plugin when it fires/skips the beat. */
    @SerializedName("decided_at")
    public String decidedAt;

    public BeatQueueRow() { }

    /**
     * The payload as a {@link com.google.gson.JsonObject}, or null if absent / not an object.
     * Never throws. Callers pass this to {@code BeatPayload.of(obj)}; a null falls back to an
     * empty payload there.
     */
    public com.google.gson.JsonObject payloadObject() {
        try {
            if (payload != null && payload.isJsonObject()) {
                return payload.getAsJsonObject();
            }
        } catch (Throwable ignored) {
            // never throw out of a row accessor
        }
        return null;
    }

    /**
     * Legacy/string view of the payload (compact JSON text), or {@code "{}"} when absent. Kept so
     * any caller that still wants a string (e.g. {@code BeatPayload.parse}) keeps working.
     */
    public String payloadString() {
        try {
            return payload == null ? "{}" : payload.toString();
        } catch (Throwable ignored) {
            return "{}";
        }
    }
}
