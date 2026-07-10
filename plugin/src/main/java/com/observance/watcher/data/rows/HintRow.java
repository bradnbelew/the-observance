package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/** Read-only row for {@code public.hints}, used by director-only progress/stuck reports. */
public final class HintRow {

    @SerializedName("puzzle_key")
    public String puzzleKey;

    @SerializedName("tier")
    public Integer tier;

    @SerializedName("body")
    public String body;

    public HintRow() { }
}
