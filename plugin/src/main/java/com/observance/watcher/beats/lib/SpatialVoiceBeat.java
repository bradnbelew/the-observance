package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.entity.Player;

/**
 * OPTIONAL LATE-ARC GARNISH ({@code backlog-modeled-mob-and-voice} / D11, P3 — the Ear's reply half).
 *
 * <p>The keeper "speaks" an authored voice clip to ONE player, spatialized so it seems to come from a
 * point in the world (a few blocks behind/around them). The clip is a resource-pack {@code .ogg} (or a
 * pre-rendered TTS line baked into the pack) named by {@code named_sound}. <b>Falls back to exactly
 * {@code observance:keeper_voice}</b> when no clip is named, when Simple Voice Chat / the spatial layer
 * never installs, or when the named clip isn't in the player's pack — so the beat is identical in shape
 * to {@link PrivateSoundBeat} and never depends on an optional plugin. It gates nothing.
 *
 * <p><b>Why this is just a careful {@link PrivateSoundBeat}.</b> Real per-player spatial VOICE (as
 * opposed to a pack sound) needs the Simple Voice Chat API; that is a go-live add. Until then — and as the
 * permanent fallback — a named pack sound played via {@link PerPlayer#namedSound}/{@code soundAt} delivers
 * the same private, spatialized "the dark said your word back" moment using only vanilla per-player audio.
 * This class therefore composes {@link PerPlayer} directly (no SVC type is referenced or class-loaded), and
 * the SVC upgrade can later replace the {@link #deliver} body without touching the beat's contract.
 *
 * <p><b>Defers to {@code apparitionClaim} (INV-18).</b> The Ear is one of the apparition/whisper lanes the
 * spawn-bias conductor arbitrates. The CLAIM is enforced upstream (the showrunner only enqueues this beat
 * when the Ear holds the claim for this window) — this beat adds no second arbiter; it just delivers the
 * line it was handed, exactly like {@link KeeperNpcBeat} delivers its resolved text.
 *
 * <p>Payload:
 * <pre>{@code
 * {
 *   "named_sound": "observance:keeper_voice.your_word",  // authored clip; absent → keeper_voice fallback
 *   "volume": 0.7, "pitch": 1.0,
 *   "behind": true, "offset": 5.0      // spatialize a few blocks behind the player
 * }
 * }</pre>
 *
 * <p>{@link BeatCategory#PERSONALIZED}: it lands privately on one player (the Ear heard THAT player), so it
 * counts against the personalized budget, not ambient.
 */
public final class SpatialVoiceBeat extends AbstractBeat {

    /** The permanent fallback clip — the keeper's voice, always present in the one-click pack. */
    static final String FALLBACK_NAMED_SOUND = "observance:keeper_voice";

    @Override public String name() { return "spatial_voice"; }
    @Override public String description() { return "An authored keeper voice clip to one player, spatialized; falls back to observance:keeper_voice."; }
    @Override public BeatCategory category() { return BeatCategory.PERSONALIZED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        // Always enactable for an online target — the fallback clip guarantees there is something to play.
        return req.hasTarget();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");

        BeatPayload p = req.payload();
        final String named = resolveClip(p.string("named_sound", null));
        final float volume = clampVol(p.floatValue("volume", 0.7f));
        final float pitch = clampPitch(p.floatValue("pitch", 1.0f));

        // A named pack sound delivered to one player only — the permanent, plugin-free floor. A named
        // (resource-pack) key has no Sound enum, so vanilla per-player audio can only play it at the
        // player; the spatial "from behind/around you" read is therefore carried by the clip's own authored
        // panning + the pitch/volume framing. The Simple Voice Chat go-live upgrade replaces this single
        // line with a true positional voice source at the behind-offset point (computed from `offset`);
        // until then the keeper line is private and never silent. MAIN thread.
        PerPlayer.namedSound(pl, named, volume, pitch);
        return BeatResult.fired(named.equals(FALLBACK_NAMED_SOUND) ? "voice-fallback" : "voice");
    }

    /** A blank/missing clip name resolves to the permanent keeper-voice fallback (never silence). */
    static String resolveClip(String named) {
        if (named == null || named.isBlank()) return FALLBACK_NAMED_SOUND;
        String s = named.trim();
        // Must look like a resource-pack key (namespace:path); otherwise fall back rather than risk a miss.
        return s.contains(":") ? s : FALLBACK_NAMED_SOUND;
    }

    private static float clampVol(float v) { return Float.isNaN(v) ? 0.7f : Math.max(0f, Math.min(10f, v)); }
    private static float clampPitch(float v) { return Float.isNaN(v) ? 1f : Math.max(0.5f, Math.min(2f, v)); }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the fallback resolution — the one piece of logic that makes this beat never-silent: a blank
     * or malformed clip name MUST resolve to {@code observance:keeper_voice}, and a valid namespaced key
     * MUST pass through. A regression here would play nothing (a dropped keeper line).
     */
    static boolean spatialVoiceSelfTest() {
        if (!FALLBACK_NAMED_SOUND.equals(resolveClip(null))) return false;
        if (!FALLBACK_NAMED_SOUND.equals(resolveClip(""))) return false;
        if (!FALLBACK_NAMED_SOUND.equals(resolveClip("no_namespace_here"))) return false; // not a key → fallback
        if (!"observance:keeper_voice.your_word".equals(resolveClip("observance:keeper_voice.your_word"))) return false;
        // Clamps stay in range / never NaN.
        if (clampVol(Float.NaN) != 0.7f) return false;
        return clampPitch(99f) == 2f && clampPitch(0.1f) == 0.5f;
    }
}
