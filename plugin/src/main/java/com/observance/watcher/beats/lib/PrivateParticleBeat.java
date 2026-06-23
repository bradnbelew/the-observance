package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * SENSORY — particles only ONE player sees (deniable). A wisp of smoke in a doorway, a smear of soul
 * fire near the cairn — only for them. Placed at the anchor (or near the player) and sent per-player.
 *
 * <p>Payload:
 * <pre>{@code
 * { "particle":"SMOKE", "count":12, "spread":0.4, "speed":0.0, "height":1.0, "near_player":true }
 * }</pre>
 */
public final class PrivateParticleBeat extends AbstractBeat {

    @Override public String name() { return "private_particle"; }
    @Override public String description() { return "Particles only one player sees, at a spot in the world."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        return anchor(ctx, req) != null || req.hasTarget();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        Particle particle = particle(p.string("particle", "SMOKE"), Particle.SMOKE);
        if (particle == null) return BeatResult.skipped("no-particle");

        Location at;
        if (p.bool("near_player", false) || anchor(ctx, req) == null) {
            org.bukkit.util.Vector back = pl.getLocation().getDirection()
                    .multiply(-Math.max(1.0, p.number("offset", 3.0)));
            at = pl.getLocation().clone().add(back).add(0, p.number("height", 1.0), 0);
        } else {
            at = anchor(ctx, req).clone().add(0, p.number("height", 1.0), 0);
        }
        if (at.getWorld() == null || !at.getWorld().equals(pl.getWorld())) {
            return BeatResult.skipped("world-mismatch");
        }

        int count = Math.max(1, Math.min(200, p.integer("count", 12)));
        double spread = Math.max(0.0, Math.min(4.0, p.number("spread", 0.4)));
        double speed = Math.max(0.0, Math.min(2.0, p.number("speed", 0.0)));
        PerPlayer.particle(pl, particle, at, count, spread, spread, spread, speed);
        return BeatResult.fired("particles");
    }
}
