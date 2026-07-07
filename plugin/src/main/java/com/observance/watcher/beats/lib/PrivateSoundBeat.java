package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * SENSORY — a sound only ONE player hears (deniable). Either a vanilla {@link Sound} or a named
 * resource-pack key. Optionally spatialized at an offset behind/around the player so it seems to
 * come from somewhere in the world. The cornerstone "did you hear that?" beat.
 *
 * <p>Payload:
 * <pre>{@code
 * { "sound":"AMBIENT_CAVE", "named_sound":"observance:whisper", "volume":0.6, "pitch":0.8,
 *   "behind":true, "offset":4.0 }
 * }</pre>
 */
public final class PrivateSoundBeat extends AbstractBeat {

    @Override public String name() { return "private_sound"; }
    @Override public String description() { return "A sound only one player hears, optionally from a spot behind them."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        BeatPayload p = req.payload();
        return p.has("sound") || p.has("named_sound");
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        float volume = clampVol(p.floatValue("volume", 0.6f));
        float pitch = clampPitch(p.floatValue("pitch", 1.0f));

        Location where = pl.getLocation();
        if (p.bool("behind", false)) {
            double offset = Math.max(0.5, Math.min(16.0, p.number("offset", 4.0)));
            org.bukkit.util.Vector back = pl.getLocation().getDirection().multiply(-offset);
            where = pl.getLocation().clone().add(back);
        }

        String named = p.string("named_sound", null);
        if (named != null && !named.isBlank()) {
            // Named resource-pack sounds honor the authored source location too.
            PerPlayer.namedSoundAt(pl, where, named, volume, pitch);
            return BeatResult.fired("named-sound");
        }
        Sound sound = sound(p.string("sound", null));
        if (sound == null) return BeatResult.skipped("no-sound");
        PerPlayer.soundAt(pl, where, sound, volume, pitch);
        return BeatResult.fired("sound");
    }

    private static float clampVol(float v) { return Float.isNaN(v) ? 0.6f : Math.max(0f, Math.min(10f, v)); }
    private static float clampPitch(float v) { return Float.isNaN(v) ? 1f : Math.max(0.5f, Math.min(2f, v)); }
}
