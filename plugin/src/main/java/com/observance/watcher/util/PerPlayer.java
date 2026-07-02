package com.observance.watcher.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Play a named (resource-pack) sound only this player hears, at a WORLD location — spatialized for
     * them, so the clip seems to come from that point (e.g. a few blocks behind the player). Vanilla
     * client audio pans/attenuates a positioned sound by the listener's facing, so this is the genuine
     * "the dark said your word back from over there" delivery (as opposed to {@link #namedSound}, which
     * plays flat at the player). Null-/online-guarded; falls back to the player's own location if
     * {@code at} is null so the line is never silent.
     */
    public static void namedSoundAt(Player player, Location at, String soundKey, float volume, float pitch) {
        if (!usable(player) || soundKey == null || soundKey.isBlank()) return;
        Location where = (at != null && at.getWorld() != null && at.getWorld().equals(player.getWorld()))
                ? at : player.getLocation();
        player.playSound(where, soundKey, SoundCategory.AMBIENT, clampVol(volume), clampPitch(pitch));
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

    /* ----------------------- per-player entity ------------------------ */

    /**
     * Make {@code entity} visible to ONLY this player (the "it knows ME" apparition — a display entity
     * or mob that no one else can see). Uses Paper's genuinely client-only {@code Player#showEntity};
     * the entity itself is a real server entity, so spawn it hidden-from-everyone and reveal it to the
     * one target. Null-/online-guarded; never throws (a stale entity handle can't crash a batch).
     *
     * @param plugin the owning plugin (required by the visibility API's tracking).
     */
    public static void showEntityTo(Plugin plugin, Player player, Entity entity) {
        if (plugin == null || !usable(player) || entity == null) return;
        try {
            player.showEntity(plugin, entity);
        } catch (Throwable ignored) { }
    }

    /**
     * Hide {@code entity} from ONLY this player (the rest of the server still sees it, if anyone does).
     * The inverse of {@link #showEntityTo}. Null-/online-guarded; never throws.
     */
    public static void hideEntityFrom(Plugin plugin, Player player, Entity entity) {
        if (plugin == null || !usable(player) || entity == null) return;
        try {
            player.hideEntity(plugin, entity);
        } catch (Throwable ignored) { }
    }

    /** True if this player can currently see the entity (best-effort; false on any failure). */
    public static boolean canSeeEntity(Player player, Entity entity) {
        if (!usable(player) || entity == null) return false;
        try {
            return player.canSee(entity);
        } catch (Throwable t) {
            return false;
        }
    }

    /* --------------------- per-player light dimming -------------------- */

    /**
     * Radius (blocks) around the player scanned for light-emitting blocks to douse. Kept small so the
     * effect is a local "the light shrank back from me" hush, not a whole-room blackout, and so the
     * revert set stays bounded.
     */
    private static final int DIM_RADIUS = 6;

    /** Light-affecting blocks we swap to a dark, same-shape stand-in for the watched player only. */
    private static Material darkStandIn(Material lit) {
        return switch (lit) {
            case TORCH, REDSTONE_TORCH, SOUL_TORCH -> Material.AIR;             // the flame goes out
            case WALL_TORCH, REDSTONE_WALL_TORCH, SOUL_WALL_TORCH -> Material.AIR;
            case LANTERN, SOUL_LANTERN -> Material.AIR;
            case GLOWSTONE, SHROOMLIGHT, OCHRE_FROGLIGHT,
                 VERDANT_FROGLIGHT, PEARLESCENT_FROGLIGHT -> Material.COAL_BLOCK; // an opaque dark block
            case SEA_LANTERN -> Material.COAL_BLOCK;
            case JACK_O_LANTERN -> Material.CARVED_PUMPKIN;                      // lit → unlit twin
            case CAMPFIRE -> Material.AIR;
            case SOUL_CAMPFIRE -> Material.AIR;
            case END_ROD -> Material.AIR;
            default -> null;
        };
    }

    /**
     * Dim the light around a player CLIENT-ONLY: light-emitting blocks within {@link #DIM_RADIUS} are
     * sent to that player as a dark stand-in, so their immediate surroundings darken as if the world
     * drew back its light from them — while everyone else's world is untouched. Pure
     * {@code Player#sendBlockChange}; the server world never changes.
     *
     * <p>Returns the list of locations that were altered — the caller MUST keep this and pass it to
     * {@link #undimLightAround} (or {@link #revertBlocks}) to restore the real blocks when the player
     * is no longer watched. If the player logs off before revert, the changes evaporate on their own
     * (they were client-only), so a lost list can never strand a real block.
     *
     * @return altered locations (possibly empty); never null.
     */
    public static List<Location> dimLightAround(Player player) {
        List<Location> altered = new ArrayList<>();
        if (!usable(player)) return altered;
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) return altered;
        int cx = center.getBlockX(), cy = center.getBlockY(), cz = center.getBlockZ();
        for (int dx = -DIM_RADIUS; dx <= DIM_RADIUS; dx++) {
            for (int dy = -DIM_RADIUS; dy <= DIM_RADIUS; dy++) {
                for (int dz = -DIM_RADIUS; dz <= DIM_RADIUS; dz++) {
                    int x = cx + dx, y = cy + dy, z = cz + dz;
                    if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
                    Block b = world.getBlockAt(x, y, z);
                    Material stand = darkStandIn(b.getType());
                    if (stand == null) continue;
                    try {
                        player.sendBlockChange(b.getLocation(), stand.createBlockData());
                        altered.add(b.getLocation());
                    } catch (Throwable ignored) { }
                }
            }
        }
        return altered;
    }

    /** Restore the real light-blocks for this player (the revert half of {@link #dimLightAround}). */
    public static void undimLightAround(Player player, List<Location> altered) {
        revertBlocks(player, altered);
    }

    /** Resend the server's real block to this player for every location (generic fake-block revert). */
    public static void revertBlocks(Player player, List<Location> locations) {
        if (!usable(player) || locations == null) return;
        for (Location at : locations) {
            if (at == null || at.getWorld() == null) continue;
            if (!at.getWorld().equals(player.getWorld())) continue;
            try {
                player.sendBlockChange(at, at.getBlock().getBlockData());
            } catch (Throwable ignored) { }
        }
    }

    /* ---------------------- per-player fog / darkness ------------------ */

    /**
     * Apply a per-player fog/darkness pressure — the vanilla Darkness effect's pulsing screen-dim, seen
     * by this player alone. Fully reversible via {@link #clearFog}; capped so it can never trap a player
     * in the black. Ambient (no particles, no HUD icon) so it reads as the world dimming, not a debuff.
     *
     * @param seconds   duration in seconds (clamped 1..30).
     * @param amplifier effect strength (clamped 0..2).
     */
    public static void fog(Player player, int seconds, int amplifier) {
        if (!usable(player)) return;
        int secs = Math.max(1, Math.min(30, seconds));
        int amp = Math.max(0, Math.min(2, amplifier));
        PotionEffectType darkness = darknessType();
        if (darkness == null) return;                       // registry unavailable → no-op, never throw
        try {
            player.addPotionEffect(new PotionEffect(darkness, secs * 20, amp, true, false, false));
        } catch (Throwable ignored) { }
    }

    /** Clear the per-player fog/darkness early (the revert half of {@link #fog}). */
    public static void clearFog(Player player) {
        if (!usable(player)) return;
        PotionEffectType darkness = darknessType();
        if (darkness == null) return;
        try {
            player.removePotionEffect(darkness);
        } catch (Throwable ignored) { }
    }

    /** Resolve the DARKNESS effect via the registry (it is no longer a plain enum). Never throws. */
    private static PotionEffectType darknessType() {
        try {
            PotionEffectType t = org.bukkit.Registry.EFFECT.get(
                    org.bukkit.NamespacedKey.minecraft("darkness"));
            if (t != null) return t;
        } catch (Throwable ignored) { }
        try {
            return PotionEffectType.DARKNESS;
        } catch (Throwable t) {
            return null;
        }
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
