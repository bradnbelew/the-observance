package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Row for the {@code observations} table (0009_observations.sql) — the Observer Tier-1 "it heard you
 * say it" capture. INSERT (never upsert): every utterance is a NEW row, the verbatim thing said,
 * stored unaltered so it can later be quoted back GROUNDED.
 *
 * <p><b>Privacy discipline.</b> A row is written ONLY when both consent gates allow it (the global
 * {@code observer_capture} switch here in the plugin, and the per-player {@code players.observer_opt_out}
 * downstream). {@code text} is the message exactly as sent — never censored, never mutated.
 *
 * <p><b>Fields.</b> {@code source='chat'}, {@code mc_uuid} = the speaker's uuid, {@code text} = the
 * verbatim message, {@code context} = a short provenance string (e.g. the world name). {@code player_id}
 * is left null (the showrunner/weaponizer lane resolves mc_uuid → players.id); {@code weaponized_at} is
 * left null (set when the quote is used). Nulls are dropped by the client's Gson, so the DB defaults
 * ({@code observed_at}) apply and unset columns are simply omitted.
 */
public final class ObservationRow {

    /** The speaker's Minecraft uuid (the mc_uuid → players.id resolve happens downstream). */
    @SerializedName("mc_uuid")
    public String mcUuid;

    /** One of discord | chat | voice (DB check constraint). The plugin only ever writes 'chat'. */
    @SerializedName("source")
    public String source;

    /** The verbatim utterance — stored EXACTLY as said, never altered. */
    @SerializedName("text")
    public String text;

    /** Short provenance string (e.g. "in-game" or the world name). */
    @SerializedName("context")
    public String context;

    public ObservationRow() { }

    public ObservationRow(String mcUuid, String source, String text, String context) {
        this.mcUuid = mcUuid;
        this.source = source;
        this.text = text;
        this.context = context;
    }
}
