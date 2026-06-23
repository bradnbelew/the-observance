package com.observance.watcher.beats;

import com.observance.watcher.config.Site;
import com.observance.watcher.data.rows.BeatQueueRow;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * A normalized, validated request to enact one beat. Built on the MAIN thread from a
 * {@link BeatQueueRow} (queued/directed beat) or synthesized by the ambient generator. All the
 * fiddly null-handling is done once here so individual beats stay clean.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code beatId} — Supabase row id, or a synthetic id for ambient beats (audit only).</li>
 *   <li>{@code type} — the beat-library key (e.g. "lectern_fill", "torch_gutter").</li>
 *   <li>{@code category} — how it counts against the drama budget.</li>
 *   <li>{@code targetPlayer} — resolved online player for per-player beats, or null (ambient/world).</li>
 *   <li>{@code site} — resolved placed Site for world-located beats, or null.</li>
 *   <li>{@code payload} — parsed, null-safe content accessor.</li>
 * </ul>
 */
public final class BeatRequest {

    private final String beatId;
    private final String type;
    private final BeatCategory category;
    private final Player targetPlayer;     // nullable
    private final Site site;               // nullable
    private final BeatPayload payload;

    public BeatRequest(String beatId, String type, BeatCategory category,
                       Player targetPlayer, Site site, BeatPayload payload) {
        this.beatId = (beatId == null || beatId.isBlank()) ? "synthetic" : beatId;
        this.type = type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
        this.category = category == null ? BeatCategory.AMBIENT : category;
        this.targetPlayer = targetPlayer;
        this.site = site;
        this.payload = payload == null ? BeatPayload.empty() : payload;
    }

    public String beatId() { return beatId; }
    public String type() { return type; }
    public BeatCategory category() { return category; }
    public Player targetPlayer() { return targetPlayer; }
    public boolean hasTarget() { return targetPlayer != null && targetPlayer.isOnline(); }
    public Site site() { return site; }
    public boolean hasSite() { return site != null && site.isPlaced(); }
    public BeatPayload payload() { return payload; }

    public UUID targetUuid() {
        return targetPlayer == null ? null : targetPlayer.getUniqueId();
    }

    @Override
    public String toString() {
        return "BeatRequest{id=" + beatId + " type=" + type + " cat=" + category
                + " target=" + (targetPlayer == null ? "-" : targetPlayer.getName())
                + " site=" + (site == null ? "-" : site.id()) + "}";
    }
}
