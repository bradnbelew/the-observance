package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code settings} table — a flat key/value control surface (e.g. watcher_sleep).
 * READ-only here. Value is stored as text and interpreted by the caller (booleans via
 * {@link #asBoolean()}).
 */
public final class SettingsRow {

    @SerializedName("key")
    public String key;

    @SerializedName("value")
    public String value;

    public SettingsRow() { }

    public SettingsRow(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /** Interpret the value as a boolean ("true"/"1"/"yes"/"on" → true). Null/blank → false. */
    public boolean asBoolean() {
        if (value == null) return false;
        String v = value.trim().toLowerCase(java.util.Locale.ROOT);
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("on");
    }
}
