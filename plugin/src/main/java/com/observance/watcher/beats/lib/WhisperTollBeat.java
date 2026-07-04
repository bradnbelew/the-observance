package com.observance.watcher.beats.lib;

import com.observance.watcher.beats.BeatCategory;
import com.observance.watcher.beats.BeatContext;
import com.observance.watcher.beats.BeatPayload;
import com.observance.watcher.beats.BeatRequest;
import com.observance.watcher.beats.BeatResult;
import com.observance.watcher.util.PerPlayer;
import com.observance.watcher.util.Placement;
import com.observance.watcher.util.TextFit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * DIRECTED — the {@code whisper_toll} beat (FLOW §3 hint economy). When a player spends a Whisper,
 * the bot enqueues this toll: an atmospheric, REVERSIBLE cost that takes *warmth, not progress*
 * (decency floor #10). It composes a few cold cues — gutter the nearest torch (relit shortly), a
 * low cold sound, a brief dim — aimed at the toll's target (or all online players near the site).
 *
 * <p>Lore-agnostic: any toll text/sound is authored in the payload; the mechanical shape (douse →
 * relight, cold pulse) is fixed and safe.
 *
 * <p>Payload:
 * <pre>{@code
 * { "sound":"AMBIENT_CAVE", "pitch":0.6, "darkness_seconds":3, "torch_relight_seconds":30,
 *   "actionbar":"the warmth dims" }
 * }</pre>
 */
public final class WhisperTollBeat extends AbstractBeat {

    @Override public String name() { return "whisper_toll"; }
    @Override public String description() { return "A reversible cold toll (warmth, not progress) for spending a Whisper."; }
    @Override public BeatCategory category() { return BeatCategory.DIRECTED; }

    @Override
    public boolean canEnact(BeatContext ctx, BeatRequest req) {
        // Always enactable in some form (it degrades to just a sound/dim if no torch is near).
        return req.hasTarget() || req.hasSite();
    }

    @Override
    protected BeatResult doEnact(BeatContext ctx, BeatRequest req) {
        BeatPayload p = req.payload();
        Player pl = target(req);
        Location anchor = anchor(ctx, req);
        boolean didSomething = false;

        // 1) Cold sound + dim, per-player (deniable, immediate).
        if (pl != null) {
            Sound s = sound(p.string("sound", "AMBIENT_CAVE"));
            if (s != null) {
                PerPlayer.soundAt(pl, pl.getLocation(), s, 0.7f,
                        Math.max(0.5f, Math.min(2f, p.floatValue("pitch", 0.6f))));
                didSomething = true;
            }
            int dim = Math.max(0, Math.min(10, p.integer("darkness_seconds", 3)));
            if (dim > 0) {
                try {
                    pl.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.DARKNESS, dim * 20, 0, true, false, false));
                    didSomething = true;
                } catch (Throwable ignored) { }
            }
            String ab = p.string("actionbar", null);
            if (ab != null && !ab.isBlank()) {
                // The action bar is ONE non-wrapping line — 256 characters would run well past what
                // a client actually shows, clipped silently on the client, not the server.
                PerPlayer.actionBar(pl, TextFit.clampLine(ab, TextFit.HUD_LINE_CHARS));
                didSomething = true;
            }
        }

        // 2) Gutter the nearest torch near the anchor, relight shortly (reversible).
        Location gutterAt = anchor != null ? anchor : (pl != null ? pl.getLocation() : null);
        if (gutterAt != null && gutterAt.getWorld() != null
                && gutterAt.getWorld().isChunkLoaded(gutterAt.getBlockX() >> 4, gutterAt.getBlockZ() >> 4)) {
            List<Block> torches = nearestTorches(gutterAt, 6, 1);
            final long relight = Math.max(20L, p.integer("torch_relight_seconds", 30) * 20L);
            for (Block torch : torches) {
                final org.bukkit.block.data.BlockData original = torch.getBlockData().clone();
                final Location tloc = torch.getLocation().clone();
                mutateWhenUnwitnessed(ctx, torch, () -> {
                    Block b = tloc.getBlock();
                    if (!Placement.isTorch(b)) return;
                    b.setType(Material.AIR, false);
                    ctx.scheduler().runLaterSafe("beat.toll.relight", relight, () -> {
                        Block now = tloc.getBlock();
                        if (now.getType().isAir()) {
                            relightWhenUnwitnessed(ctx, now, original);
                        }
                    });
                });
                didSomething = true;
            }
        }

        return didSomething ? BeatResult.fired("toll") : BeatResult.skipped("nothing-to-toll");
    }

    private void relightWhenUnwitnessed(BeatContext ctx, Block block, org.bukkit.block.data.BlockData original) {
        boolean hidden = ctx.safety().call("beat.toll.relight.check",
                () -> ctx.reveal().isHidden(block), Boolean.TRUE);
        if (Boolean.TRUE.equals(hidden)) {
            if (block.getType().isAir()) {
                ctx.safety().run("beat.toll.relight.apply", () -> block.setBlockData(original, false));
            }
        } else {
            ctx.scheduler().runLaterSafe("beat.toll.relight.retry",
                    ctx.config().revealRetryDelayTicks(),
                    () -> relightWhenUnwitnessed(ctx, block, original));
        }
    }

    private static List<Block> nearestTorches(Location center, int radius, int limit) {
        List<Block> out = new ArrayList<>();
        Block origin = center.getBlock();
        int r = Math.max(1, Math.min(10, radius));
        for (int dy = -r; dy <= r && out.size() < limit; dy++) {
            for (int dx = -r; dx <= r && out.size() < limit; dx++) {
                for (int dz = -r; dz <= r && out.size() < limit; dz++) {
                    Block b = origin.getRelative(dx, dy, dz);
                    if (Placement.isTorch(b)
                            && b.getType() != Material.REDSTONE_TORCH
                            && b.getType() != Material.REDSTONE_WALL_TORCH) {
                        out.add(b);
                    }
                }
            }
        }
        return out;
    }
}
