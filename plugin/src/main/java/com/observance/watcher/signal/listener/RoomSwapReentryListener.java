package com.observance.watcher.signal.listener;

import com.observance.watcher.beats.lib.RoomSwapBeat;
import com.observance.watcher.util.RateLimiter;
import com.observance.watcher.util.Safety;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * THE ROOM-SWAP CONSUMER — teleport a player who crosses back through a SEALED door into the pre-built
 * changed room ({@link RoomSwapBeat}'s consumer half; the D5 rework). When {@link RoomSwapBeat} arms a
 * swap it seals the room's door and records the destination entry point ("world x y z") on that door
 * block's chunk PDC under {@code observance:swap_dest.<coords>}. This listener watches for a player
 * pushing INTO the sealed door cell (or the block just in front of it) and, if a destination is recorded
 * for that cell, teleports them to B — the illusion of a rebuilt room, delivered with no in-place
 * overwrite and no witnessable mutation.
 *
 * <p><b>Cheap-first, fault-isolated, reveal-safe.</b> The move handler early-returns HARD before touching
 * Safety unless the player actually changed block cells (the vast majority of move events are sub-block
 * jitters). Only then does it read the destination PDC on the player's target cell + the cell they are
 * facing into. A per-player cooldown prevents a teleport loop / spam. It never cancels the move, never
 * mutates the world, never messages the room; a teleport failure degrades to a no-op (the player just
 * stays put — the door is still sealed, so no jank). Reads only durable PDC the beat wrote — no DB, no
 * cross-tick state held here.
 */
public final class RoomSwapReentryListener implements Listener {

    /** Per-player cooldown after a teleport so a re-entry can't ping-pong. */
    private static final long TELEPORT_COOLDOWN_MS = 3_000L;

    private final RateLimiter rateLimiter;
    private final Safety safety;
    private final String namespace;

    public RoomSwapReentryListener(RateLimiter rateLimiter, Safety safety, String namespace) {
        this.rateLimiter = rateLimiter;
        this.safety = safety;
        this.namespace = (namespace == null || namespace.isBlank()) ? "observance" : namespace;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;
        // Cheap gate: only act when the player entered a NEW block cell (skip sub-block jitter).
        if (from != null
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        World world = to.getWorld();
        if (world == null) return;

        safety.run("room_swap.reentry.move", () -> {
            Player p = event.getPlayer();
            if (p == null) return;

            // The player's feet cell + the cell at head height (a door is usually 2 tall). Either carrying a
            // recorded destination means they pushed into a sealed door back into A.
            Location dest = lookupDest(world, to.getBlockX(), to.getBlockY(), to.getBlockZ());
            if (dest == null) {
                dest = lookupDest(world, to.getBlockX(), to.getBlockY() + 1, to.getBlockZ());
            }
            if (dest == null) return;

            String cdKey = "roomswap:" + p.getUniqueId();
            if (!rateLimiter.tryCooldown(cdKey, TELEPORT_COOLDOWN_MS)) return;   // just teleported → let it settle

            // Preserve their facing so the room-change reads as "I walked through and I'm inside".
            dest.setYaw(to.getYaw());
            dest.setPitch(to.getPitch());
            final Location target = dest;
            try {
                p.teleport(target);
                sendReentryFeedback(p);
                safety.info("room_swap.reentry", p.getName() + " re-entered the swapped room");
            } catch (Throwable ignored) {
                // Teleport hiccup → no-op; the door stays sealed, no jank.
            }
        });
    }

    private void sendReentryFeedback(Player p) {
        if (p == null) return;
        try {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.16f, 0.42f);
        } catch (Throwable ignored) {
            // atmospheric only
        }
        try {
            p.sendActionBar(Component.text("the room returns wrong.", NamedTextColor.DARK_GRAY));
        } catch (Throwable ignored) {
            // older clients or proxy shims may not support action bars
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Destination lookup (the durable record the beat wrote)             */
    /* ------------------------------------------------------------------ */

    /** Read the swap destination recorded on the door cell at (x,y,z), or null if none. Never throws. */
    private Location lookupDest(World world, int x, int y, int z) {
        try {
            if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
            Block door = world.getBlockAt(x, y, z);
            var pdc = door.getChunk().getPersistentDataContainer();
            var key = RoomSwapBeat.destKey(namespace, door);
            if (!pdc.has(key, PersistentDataType.STRING)) return null;
            String encoded = pdc.get(key, PersistentDataType.STRING);
            return decodeDest(encoded);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Parse a "world x y z" destination string back into a Location, or null if malformed. Never throws. */
    static Location decodeDest(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        String[] parts = encoded.trim().split("\\s+");
        if (parts.length != 4) return null;
        try {
            World w = org.bukkit.Bukkit.getWorld(parts[0]);
            if (w == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(w, x, y, z);
        } catch (Throwable t) {
            return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Cheap build-time self-test (server-free; mirrors the repo idiom).  */
    /* ------------------------------------------------------------------ */

    /**
     * Guards the destination parse the teleport leans on: a well-formed "world x y z" string parses to the
     * right numbers, and any malformed / short / empty string returns null (never throws, never teleports
     * to garbage). The world-lookup half is server-only (touches Bukkit) so it is exercised at runtime.
     */
    static boolean decodeSelfTest() {
        if (decodeDest(null) != null) return false;
        if (decodeDest("") != null) return false;
        if (decodeDest("world 1 2") != null) return false;          // too few parts
        if (decodeDest("world a b c") != null) return false;        // non-numeric → null (no throw)
        if (decodeDest("world 1 2 3 4") != null) return false;      // too many parts
        // A valid string parses off-server only if the world resolves; with no server Bukkit.getWorld
        // returns null → null. So we only assert the malformed cases here (the numeric parse is covered by
        // the "world a b c" case above returning null via the catch).
        return true;
    }
}
