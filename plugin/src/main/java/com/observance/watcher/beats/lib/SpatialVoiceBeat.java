package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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
        final boolean behind = p.bool("behind", true);
        final double offset = clampOffset(p.number("offset", 5.0));

        // A named pack sound delivered to one player only — the permanent, plugin-free floor. The spatial
        // read is now REAL: vanilla client audio pans/attenuates a POSITIONED sound by the listener's
        // facing, so playing the clip at a point a few blocks behind (or offset around) the player makes it
        // seem to come from over there — "the dark said your word back from behind you". This actually uses
        // the `behind`/`offset` params (previously ignored). A true Simple Voice Chat positional source is a
        // later upgrade that would replace only the delivery point computed here; the beat's contract is
        // unchanged and the line is never silent. MAIN thread.
        Location at = voiceSpot(pl, behind, offset);
        PerPlayer.namedSoundAt(pl, at, named, volume, pitch);
        return BeatResult.fired(named.equals(FALLBACK_NAMED_SOUND) ? "voice-fallback" : "voice");
    }

    /**
     * The point the voice appears to come from: {@code offset} blocks BEHIND the player (opposite their
     * look direction) when {@code behind}, else {@code offset} blocks IN FRONT. Horizontal only (we zero
     * the Y component of the look vector) so the source hangs at ear height, never below the floor or up in
     * the air. Falls back to the player's own location if the direction is degenerate (straight up/down).
     */
    static Location voiceSpot(Player pl, boolean behind, double offset) {
        Location eye = pl.getEyeLocation();
        if (eye.getWorld() == null) return pl.getLocation();
        Vector dir = eye.getDirection().clone();
        dir.setY(0);                                   // horizontal only — ear height, not underfoot/overhead
        if (dir.lengthSquared() < 1.0e-6) return eye;  // looking straight up/down → play at the player
        dir.normalize().multiply(behind ? -offset : offset);
        return eye.clone().add(dir);
    }

    /** Keep the source close enough to read as "right behind you", not across the room. */
    static double clampOffset(double v) {
        if (Double.isNaN(v)) return 5.0;
        return Math.max(0.5, Math.min(16.0, v));
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
        if (clampPitch(99f) != 2f || clampPitch(0.1f) != 0.5f) return false;
        // Offset (the now-used spatial param) clamps to a "close behind you" range and never NaN.
        if (clampOffset(Double.NaN) != 5.0) return false;
        if (clampOffset(999.0) != 16.0) return false;
        if (clampOffset(0.0) != 0.5) return false;
        return clampOffset(5.0) == 5.0;
    }
}
