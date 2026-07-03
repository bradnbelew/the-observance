package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * SENSORY (per-player) — "it knows ME": the light draws back from ONE player for a beat, then returns.
 *
 * <p>This wires the built-but-unwired {@link PerPlayer#dimLightAround} illusion primitive into the
 * ambient beat framework (cold-start-prologue §1.3 / §1.6 — the quiet "it knows me" first-session
 * beats). Light-emitting blocks within a small radius are sent to the target as a dark stand-in
 * (client-only via {@code sendBlockChange}; the real world is untouched), so their immediate
 * surroundings darken as if the world hushed its light around them — then it is restored a few
 * seconds later and they snap back ("did it just get darker?").
 *
 * <p><b>Reveal-safe + reversible.</b> Nothing in the server world changes; the effect is a pure
 * per-player packet, so no one else sees anything and there is no witnessed mutation. The revert is
 * scheduled unconditionally on the main thread (mirrors {@link PrivateTimeShiftBeat}); if the player
 * logs off first the client changes evaporate on their own (they were never real), so a lost revert
 * can never strand a block. Capped hard so it can never leave a player in the black.
 *
 * <p><b>Restraint.</b> AMBIENT category → every fire is paced by the {@code DramaBudget} and biased
 * by {@link com.observance.watcher.beats.Attention} toward the lonely/deep/dark player, which is
 * exactly the "it knows me" selection. {@code canEnact} additionally requires the target to be in a
 * genuinely dim spot so the dip is perceptible (a fully-lit base won't visibly change) AND requires
 * at least one dousable light nearby, so the beat stays quiet when it would do nothing.
 *
 * <p>Payload (all optional):
 * <pre>{@code { "seconds": 5 } }</pre>
 */
public final class ProximityDimBeat extends AbstractBeat {

    /** Only fire when the target's own light level is at or below this (so the dip actually reads). */
    private static final int MAX_LIGHT_TO_FIRE = 9;

    @Override public String name() { return "proximity_dim"; }
    @Override public String description() { return "The light draws back from one player for a beat, then returns (client-only)."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null || !pl.isOnline()) return false;
        Location loc = pl.getLocation();
        if (loc == null || loc.getWorld() == null) return false;
        // Only in the dark-ish: a hush of light against an already-bright room is invisible.
        try {
            if (loc.getBlock().getLightLevel() > MAX_LIGHT_TO_FIRE) return false;
        } catch (Throwable ignored) {
            return false;
        }
        return true;
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null || !pl.isOnline()) return BeatResult.skipped("no-target");

        BeatPayload p = req.payload();
        final int seconds = Math.max(1, Math.min(12, p.integer("seconds", 5)));   // hard cap 12s
        final UUID uuid = pl.getUniqueId();

        // Douse the nearby lights for this player only. If nothing dousable is in range, do nothing
        // (stay quiet) so an unspent budget reservation is refunded by the generator.
        List<Location> altered = PerPlayer.dimLightAround(pl);
        if (altered.isEmpty()) return BeatResult.skipped("no-lights");

        // Restore on the main thread after the beat, unconditionally (mirrors PrivateTimeShiftBeat).
        ctx.scheduler().runLaterSafe("beat.proxdim.restore", seconds * 20L, () -> {
            Player still = Bukkit.getPlayer(uuid);
            if (still != null && still.isOnline()) {
                PerPlayer.undimLightAround(still, altered);
            }
            // If offline, the client-only changes are already gone — nothing to restore.
        });

        return BeatResult.fired("dim=" + altered.size() + "/" + seconds + "s");
    }
}
