package com.observance.watcher.data.rows;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code puzzles} table (the oracle's clue web) — see
 * {@code discord/supabase/migrations/0004_oracle.sql} and {@code discord/ORACLE.md}.
 *
 * <p>READ-ONLY from the plugin: the in-world answer-sign verb fetches the OPEN puzzles
 * ({@code active = true}) and matches a normalized answer by exact set-membership against
 * {@link #acceptedAnswers}. The plugin NEVER writes this table; authoring is the bot/dashboard's job.
 *
 * <p>Lore/spoiler hygiene: {@link #acceptedAnswers} are spoilers and live ONLY in service-role
 * memory for the duration of a match; they are never spoken, logged, or echoed to players.
 */
public final class PuzzleRow {

    /** The forgeClue() FNV-1a key — the stable join point for the whole loop. */
    @SerializedName("puzzle_key")
    public String puzzleKey;

    /** Author-facing label (spoiler; never shown to players). */
    @SerializedName("title")
    public String title;

    /**
     * Normalized accepted solutions (already normalized per ORACLE.md §2). MULTIPLE supported.
     * Matching is exact whole-string set-membership of the normalized player input against this.
     */
    @SerializedName("accepted_answers")
    public String[] acceptedAnswers;

    /** next_clue | lore | dead_end | side_quest | main_beat (ORACLE.md §3). */
    @SerializedName("outcome_type")
    public String outcomeType;

    /**
     * The resolution recipe (jsonb → {@link JsonElement}). For the world surface the load-bearing
     * key is {@code beat} (the in-world reward to enqueue); {@code voice_key}/{@code voice_args}
     * drive the Discord echo only. Read via {@link #outcomeObject()}.
     */
    @SerializedName("outcome_payload")
    public JsonElement outcomePayload;

    /** Author position on the web (act/rung). Ordering/visualization only; not gated on. */
    @SerializedName("movement")
    public Integer movement;

    /** OPEN for answers? The resolver only matches rows where this is true. */
    @SerializedName("active")
    public Boolean active;

    /** Optional per-puzzle attempt cap (anti-brute-force). Null = no per-puzzle cap. */
    @SerializedName("max_attempts")
    public Integer maxAttempts;

    /**
     * The storylet gate (0006_requires_flags.sql; OVERHAUL.md §3): a flat {@code {flag:true}} object;
     * the row is OPEN iff {@code active = true} AND every key here is truthy in {@code arc_state.flags}.
     * Empty {@code {}} (the default) = ungated. AND-tested by {@code OracleResolver.firstMatch} via
     * {@link com.observance.watcher.oracle.FlagGate}, the byte-for-byte twin of the Discord gate.
     */
    @SerializedName("requires_flags")
    public JsonElement requiresFlags;

    public PuzzleRow() { }

    /** The {@code requires_flags} as a flat {@code Map<String,Object>} (empty if absent). Never throws. */
    public java.util.Map<String, Object> requiresFlagsMap() {
        try {
            if (requiresFlags != null && requiresFlags.isJsonObject()) {
                return com.observance.watcher.oracle.JsonFlags.toMap(requiresFlags.getAsJsonObject());
            }
        } catch (Throwable ignored) {
            // never throw out of a row accessor
        }
        return java.util.Collections.emptyMap();
    }

    /**
     * The {@code outcome_payload} as a {@link com.google.gson.JsonObject}, or null if absent / not
     * an object. Never throws. Pass to {@code BeatPayload.of(obj)}.
     */
    public com.google.gson.JsonObject outcomeObject() {
        try {
            if (outcomePayload != null && outcomePayload.isJsonObject()) {
                return outcomePayload.getAsJsonObject();
            }
        } catch (Throwable ignored) {
            // never throw out of a row accessor
        }
        return null;
    }
}
