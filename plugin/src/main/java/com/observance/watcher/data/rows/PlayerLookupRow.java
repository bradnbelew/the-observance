package com.observance.watcher.data.rows;

import com.google.gson.annotations.SerializedName;

/**
 * Read-only projection of {@code public.players} used to resolve a Minecraft {@code mc_uuid} to the
 * canonical {@code players.id} (uuid). The oracle's {@code solves} row keys on {@code player_id}, so
 * the in-world answer-sign verb must turn the solving player's {@code mc_uuid} into their id first.
 *
 * <p>A solve is always a known keeper (the plugin upserts {@code players} on join), so this lookup
 * normally hits. If it misses (a never-joined uuid, or DB outage), the resolver withholds silently —
 * no reward is granted without a real player_id, preserving the {@code solves} integrity guard.
 */
public final class PlayerLookupRow {

    @SerializedName("id")
    public String id;            // players.id (uuid)

    @SerializedName("mc_uuid")
    public String mcUuid;

    @SerializedName("discord_id")
    public String discordId;     // nullable; denormalized into solves for audit

    @SerializedName("observer_opt_out")
    public Boolean observerOptOut; // nullable; fail-closed callers treat null as opted out

    public PlayerLookupRow() { }
}
