package com.observance.watcher.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * Per-player targeting (anti-jank: "it knows ME"). Sends sounds/particles/titles/fake-blocks
 * to ONE player only, so a beat lands as a private experience, not a server-wide announcement.
 *
 * <p>All methods touch Bukkit objects → MAIN thread only. Every call is null-/online-guarded
 * and self-contained; a failure on one player never affects another (callers should still wrap
 * batches in {@link Safety}). Phase 0 uses only vanilla Player#... APIs — no PacketEvents yet;
 * {@link #fakeBlock} uses {@code Player#sendBlockChange}, which is genuinely client-only.
 */
public final class PerPlayer {

    private PerPlayer() { }

    /* ----------------------------- sound ------------------------------ */

    /** Play a sound only this player hears, at their own location. */
    public static void sound(Player player, Sound sound, float volume, float pitch) {
        if (!usable(player) || sound == null) return;
        player.playSound(player.getLocation(), sound, clampVol(volume), clampPitch(pitch));
    }

    /** Play a sound only this player hears, at a world location (spatialized for them). */
    public static void soundAt(Player player, Location at, Sound sound, float volume, float pitch) {
        if (!usable(player) || sound == null || at == null) return;
        player.playSound(at, sound, SoundCategory.AMBIENT, clampVol(volume), clampPitch(pitch));
    }

    /** Play a named (resource-pack) sound only this player hears. */
    public static void namedSound(Player player, String soundKey, float volume, float pitch) {
        if (!usable(player) || soundKey == null || soundKey.isBlank()) return;
        player.playSound(player.getLocation(), soundKey, SoundCategory.AMBIENT,
                clampVol(volume), clampPitch(pitch));
    }

    /* ---------------------------- particle ---------------------------- */

    /** Spawn particles visible only to this player. */
    public static void particle(Player player, Particle particle, Location at, int count,
                                double offX, double offY, double offZ, double speed) {
        if (!usable(player) || particle == null || at == null) return;
        player.spawnParticle(particle, at, Math.max(0, count), offX, offY, offZ, speed);
    }

    /** Single-point particle convenience. */
    public static void particle(Player player, Particle particle, Location at, int count) {
        particle(player, particle, at, count, 0.0, 0.0, 0.0, 0.0);
    }

    /* ----------------------------- title ------------------------------ */

    /**
     * Show a title/subtitle to one player. Times are in TICKS (20 = 1s). Null text → empty line.
     */
    public static void title(Player player, String title, String subtitle,
                             int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (!usable(player)) return;
        player.sendTitle(
                title == null ? "" : title,
                subtitle == null ? "" : subtitle,
                Math.max(0, fadeInTicks), Math.max(0, stayTicks), Math.max(0, fadeOutTicks));
    }

    /** Send an action-bar message to one player (transient, above the hotbar). */
    public static void actionBar(Player player, String message) {
        if (!usable(player) || message == null) return;
        player.sendActionBar(message);
    }

    /* --------------------------- fake block --------------------------- */

    /**
     * Show a CLIENT-ONLY block change to one player (the real world is untouched). Use for
     * "did that just... change?" beats. Caller is responsible for {@link #clearFakeBlock}
     * (resending the real block) when the beat ends or the chunk reloads.
     */
    public static void fakeBlock(Player player, Location at, BlockData fakeData) {
        if (!usable(player) || at == null || fakeData == null) return;
        if (at.getWorld() == null || !at.getWorld().equals(player.getWorld())) return;
        player.sendBlockChange(at, fakeData);
    }

    /** Revert a fake block back to the server's real block for this player. */
    public static void clearFakeBlock(Player player, Location at) {
        if (!usable(player) || at == null) return;
        if (at.getWorld() == null || !at.getWorld().equals(player.getWorld())) return;
        BlockData real = at.getBlock().getBlockData();
        player.sendBlockChange(at, real);
    }

    /* ----------------------------- helpers ---------------------------- */

    private static boolean usable(Player p) {
        return p != null && p.isOnline();
    }

    private static float clampVol(float v) {
        if (Float.isNaN(v)) return 0f;
        return Math.max(0f, Math.min(10f, v));
    }

    private static float clampPitch(float p) {
        if (Float.isNaN(p)) return 1f;
        return Math.max(0.5f, Math.min(2.0f, p));
    }
}
