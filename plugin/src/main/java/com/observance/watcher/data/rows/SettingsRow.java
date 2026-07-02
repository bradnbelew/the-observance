package com.observance.watcher.data.rows;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code settings} table — a flat key/value control surface (e.g. watcher_sleep).
 * READ-only here. Value interpreted by the caller (booleans via {@link #asBoolean()}).
 *
 * <p><b>Schema contract.</b> {@code settings.value} is {@code jsonb} (dashboard {@code 0001_init.sql:125})
 * and the dashboard's watcher-sleep switch stores a bare JSON <i>boolean</i> ({@code true}/{@code false}).
 * The field was previously typed {@code String}, so Gson threw deserializing a JSON boolean into a String
 * → {@code fetchSetting} failed → {@code isWatcherSleeping()} fail-opened to false and the remote toggle
 * could never mute the plugin. The field is now a {@link JsonElement} so any jsonb shape (boolean, string,
 * or number) parses without throwing; {@link #asBoolean()} extracts the boolean fail-safe.
 */
public final class SettingsRow {

    @SerializedName("key")
    public String key;

    @SerializedName("value")
    public JsonElement value;

    public SettingsRow() { }

    public SettingsRow(String key, JsonElement value) {
        this.key = key;
        this.value = value;
    }

    /** Convenience constructor from a raw string value (wrapped as a JSON string primitive). */
    public SettingsRow(String key, String value) {
        this.key = key;
        this.value = value == null ? null : new JsonPrimitive(value);
    }

    /**
     * Interpret the jsonb value as a boolean. Accepts a JSON boolean ({@code true}), or a JSON string /
     * number whose text is one of "true"/"1"/"yes"/"on". Null / any parse hiccup → false (fail-safe).
     */
    public boolean asBoolean() {
        if (value == null || value.isJsonNull()) return false;
        try {
            if (value.isJsonPrimitive()) {
                JsonPrimitive p = value.getAsJsonPrimitive();
                if (p.isBoolean()) return p.getAsBoolean();
                String v = p.getAsString().trim().toLowerCase(java.util.Locale.ROOT);
                return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("on");
            }
        } catch (Throwable ignored) {
            // Unparseable jsonb → default false (never silently mutes on a bad value).
        }
        return false;
    }
}
