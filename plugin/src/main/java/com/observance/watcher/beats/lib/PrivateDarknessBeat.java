package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * SENSORY — a brief pulse of fog/darkness for ONE player (the Darkness effect's screen-dimming
 * pulse, or blindness). Short, deniable, fully reversible — it removes warmth, never progress.
 * Capped to a sane max duration so it can never trap a player in the dark.
 *
 * <p>Payload:
 * <pre>{@code { "effect":"DARKNESS", "seconds":4, "amplifier":0 } }</pre>
 */
public final class PrivateDarknessBeat extends AbstractBeat {

    @Override public String name() { return "private_darkness"; }
    @Override public String description() { return "A brief pulse of darkness/fog for one player (reversible)."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        return req.hasTarget();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        PotionEffectType type = effectType(p.string("effect", "DARKNESS"));
        if (type == null) return BeatResult.skipped("bad-effect");
        int seconds = Math.max(1, Math.min(15, p.integer("seconds", 4)));   // hard cap 15s
        int amplifier = Math.max(0, Math.min(2, p.integer("amplifier", 0)));
        PotionEffect effect = new PotionEffect(type, seconds * 20, amplifier, true, false, false);
        pl.addPotionEffect(effect);
        return BeatResult.fired("darkness=" + seconds + "s");
    }

    private static PotionEffectType effectType(String name) {
        if (name == null || name.isBlank()) return PotionEffectType.DARKNESS;
        String n = name.trim().toLowerCase(java.util.Locale.ROOT);
        // Resolve via the effect registry (minecraft:<id>); fall back to DARKNESS.
        try {
            PotionEffectType t = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(n));
            if (t != null) return t;
        } catch (Throwable ignored) { }
        // Restrict the safe-default creep set so authoring a bad name can't apply something harmful.
        if (n.equals("blindness")) return PotionEffectType.BLINDNESS;
        return PotionEffectType.DARKNESS;
    }
}
