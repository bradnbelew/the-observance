package com.observance.watcher.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

/**
 * Placement validation (anti-jank #2): no floaters in rock or air; respect support/attachment.
 *
 * <p>All methods touch Bukkit world objects → MAIN thread only. They are READ-ONLY checks that
 * answer "would placing X here be janky?" The caller does the actual mutation after a true.
 */
public final class Placement {

    private static final BlockFace[] WALL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private Placement() { }

    /**
     * A free-standing surface block (sign post, lectern, cairn block, mob spawn floor):
     * the target cell must be replaceable (air/grass/liquid-ish) AND have a solid block
     * directly beneath it to stand on. No floating-in-air, no buried-in-rock.
     */
    public static boolean canStandOn(Block target) {
        if (target == null) return false;
        if (!isReplaceable(target)) return false;          // not buried in rock
        Block below = target.getRelative(BlockFace.DOWN);
        return isSolidSupport(below);                       // has a floor
    }

    /**
     * A wall-attached fixture (wall sign, wall torch, item frame): the target cell must be
     * replaceable AND at least one horizontal neighbor must be a solid face to attach to.
     */
    public static boolean canAttachToWall(Block target) {
        if (target == null) return false;
        if (!isReplaceable(target)) return false;
        for (BlockFace face : WALL_FACES) {
            if (isSolidSupport(target.getRelative(face))) {
                return true;
            }
        }
        return false;
    }

    /** Returns the first horizontal wall face the target can attach to, or null. */
    public static BlockFace wallSupportFace(Block target) {
        if (target == null) return null;
        if (!isReplaceable(target)) return null;
        for (BlockFace face : WALL_FACES) {
            if (isSolidSupport(target.getRelative(face))) {
                return face;
            }
        }
        return null;
    }

    /**
     * Context gate (anti-jank #2): only douse a torch where a torch actually exists.
     * Accepts standing or wall torches (and their soul/redstone variants).
     */
    public static boolean isTorch(Block block) {
        if (block == null) return false;
        Material m = block.getType();
        return m == Material.TORCH
                || m == Material.WALL_TORCH
                || m == Material.SOUL_TORCH
                || m == Material.SOUL_WALL_TORCH
                || m == Material.REDSTONE_TORCH
                || m == Material.REDSTONE_WALL_TORCH;
    }

    /**
     * Is this block "replaceable" — i.e. placing into it won't carve rock or overwrite a
     * meaningful block? Air, void, grass/foliage, fluids, and snow count as replaceable.
     */
    public static boolean isReplaceable(Block block) {
        if (block == null) return false;
        Material m = block.getType();
        if (m.isAir()) return true;
        switch (m) {
            case SHORT_GRASS:
            case TALL_GRASS:
            case FERN:
            case LARGE_FERN:
            case DEAD_BUSH:
            case SNOW:
            case WATER:
            case LAVA:
            case SEAGRASS:
            case TALL_SEAGRASS:
            case VINE:
                return true;
            default:
                return false;
        }
    }

    /**
     * A solid block a fixture can rest on / attach to: occluding + not passable + not a fluid.
     */
    public static boolean isSolidSupport(Block block) {
        if (block == null) return false;
        Material m = block.getType();
        if (m.isAir()) return false;
        if (m == Material.WATER || m == Material.LAVA) return false;
        // Material#isSolid covers full/structural blocks; isOccluding excludes glass/leaves
        // that look like gaps. Require occluding so signs/torches don't dangle off foliage.
        return m.isSolid() && m.isOccluding();
    }

    /**
     * Surface "raycast" lite: from {@code origin} step downward up to {@code maxDrop} blocks
     * and return the first air cell that sits directly on solid support — a safe spot to place
     * a free-standing fixture. Returns null if none found (e.g. open chasm / deep liquid).
     */
    public static Block findSurfaceSpot(Location origin, int maxDrop) {
        if (origin == null || origin.getWorld() == null) return null;
        World world = origin.getWorld();
        Block cursor = world.getBlockAt(origin);
        int steps = Math.max(1, maxDrop);
        for (int i = 0; i < steps; i++) {
            if (canStandOn(cursor)) {
                return cursor;
            }
            cursor = cursor.getRelative(BlockFace.DOWN);
            if (cursor.getY() < world.getMinHeight()) break;
        }
        return null;
    }

    /** True if the world coordinates are within the world's build range. */
    public static boolean isWithinBuildRange(World world, int y) {
        if (world == null) return false;
        return y >= world.getMinHeight() && y < world.getMaxHeight();
    }
}
