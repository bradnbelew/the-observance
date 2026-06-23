package com.observance.watcher.beats;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anti-grief registry (the owner's #1 demand): a block placed by a beat (a sign-stone, a lectern, a
 * cairn block, a written marker) is recorded here with its intended {@link BlockData}. A companion
 * listener ({@code BeatProtectionListener}) cancels or restores any attempt to break/burn/explode
 * it, so players can't weaponize or destroy authored content even if they figure out it's "fake".
 *
 * <p>Keying is by a compact world+coords string so it's cheap and survives chunk reloads. The map
 * is bounded by pruning when the world is unloaded (handled by the listener) and is thread-safe.
 *
 * <p>Persistence note: this in-memory registry is rebuilt from Supabase-authored sites + active
 * beat state on enable; the durable anti-grief guarantee for site fixtures also comes from the
 * {@code sites.yml protect} flag. Ephemeral beats deregister themselves on cleanup.
 */
public final class ProtectedRegistry {

    /** key → intended block data string (e.g. "minecraft:lectern[facing=north]"). */
    private final Map<String, String> protectedBlocks = new ConcurrentHashMap<>();

    /** Register a placed block as protected with its intended data. MAIN thread (reads BlockData). */
    public void protect(Block block) {
        if (block == null) return;
        protectedBlocks.put(key(block.getLocation()), block.getBlockData().getAsString());
    }

    /** Register a location with an explicit intended data string (when you know the target form). */
    public void protect(Location loc, BlockData data) {
        if (loc == null || data == null) return;
        protectedBlocks.put(key(loc), data.getAsString());
    }

    /** Stop protecting a location (ephemeral beat cleaned up, or fixture intentionally removed). */
    public void release(Location loc) {
        if (loc == null) return;
        protectedBlocks.remove(key(loc));
    }

    public boolean isProtected(Location loc) {
        return loc != null && protectedBlocks.containsKey(key(loc));
    }

    /** The intended block-data string for a protected location, or null. */
    public String intendedData(Location loc) {
        return loc == null ? null : protectedBlocks.get(key(loc));
    }

    public int size() {
        return protectedBlocks.size();
    }

    /** Drop all protections for a world (e.g. on world unload) to bound memory. */
    public void clearWorld(String worldName) {
        if (worldName == null) return;
        String prefix = worldName + ":";
        protectedBlocks.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public void clearAll() {
        protectedBlocks.clear();
    }

    private static String key(Location loc) {
        World w = loc.getWorld();
        String wn = w == null ? "?" : w.getName();
        return wn + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }
}
