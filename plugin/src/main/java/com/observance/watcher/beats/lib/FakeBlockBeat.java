package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * SENSORY — a CLIENT-ONLY fake block/sign shown to ONE player (the real world is never touched).
 * The purest "it knows ME / that makes no sense" beat: a sealed wall, a watching block, a sign that
 * only they see. Auto-reverts after a bounded time so it can never desync them permanently — and
 * because the server block never changed, a chunk refresh also clears it. Zero grief surface.
 *
 * <p>Payload:
 * <pre>{@code
 * { "block":"SOUL_SAND", "x":..,"y":..,"z":.., "near_player":true, "offset":3, "seconds":6 }
 * }</pre>
 * If no explicit coords, targets a block a few blocks in front of the player (their look target).
 */
public final class FakeBlockBeat extends AbstractBeat {

    @Override public String name() { return "fake_block"; }
    @Override public String description() { return "A client-only fake block appears for one player, then reverts."; }
    @Override public BeatCategory category() { return BeatCategory.AMBIENT; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        if (!req.hasTarget()) return false;
        return req.payload().has("block");
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        Player pl = target(req);
        if (pl == null) return BeatResult.skipped("no-target");
        BeatPayload p = req.payload();
        org.bukkit.Material mat = material(p.string("block", null), null);
        if (mat == null || !mat.isBlock()) return BeatResult.skipped("bad-block");

        Location at = resolveTarget(ctx, req, pl);
        if (at == null || at.getWorld() == null || !at.getWorld().equals(pl.getWorld())) {
            return BeatResult.skipped("no-target-block");
        }

        BlockData fake;
        try { fake = mat.createBlockData(); }
        catch (Throwable t) { return BeatResult.failed("bad-data"); }

        final Location loc = at.clone();
        PerPlayer.fakeBlock(pl, loc, fake);

        int seconds = Math.max(1, Math.min(30, p.integer("seconds", 6)));
        final java.util.UUID uuid = pl.getUniqueId();
        ctx.scheduler().runLaterSafe("beat.fakeblock.revert", seconds * 20L, () -> {
            Player still = Bukkit.getPlayer(uuid);
            if (still != null && still.isOnline()) {
                PerPlayer.clearFakeBlock(still, loc);  // resend real block
            }
        });
        return BeatResult.fired("fakeblock=" + seconds + "s");
    }

    private static Location resolveTarget(BeatContext ctx, BeatRequest req, Player pl) {
        BeatPayload p = req.payload();
        if (p.has("x") && p.has("y") && p.has("z")) {
            return new Location(pl.getWorld(), p.number("x", 0), p.number("y", 0), p.number("z", 0));
        }
        // a block in the player's line of sight (their look target), so they "catch" it changing
        try {
            var block = pl.getTargetBlockExact(Math.max(2, Math.min(16, p.integer("offset", 5))));
            if (block != null) return block.getLocation();
        } catch (Throwable ignored) { }
        // fallback: a few blocks ahead at foot level
        org.bukkit.util.Vector fwd = pl.getLocation().getDirection().multiply(p.number("offset", 3.0));
        return pl.getLocation().clone().add(fwd);
    }
}
