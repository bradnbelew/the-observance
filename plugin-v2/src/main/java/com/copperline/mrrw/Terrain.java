package com.copperline.mrrw;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Seating modern builds on the upgraded Beta ground (2026-10-19 audit: floors level with the grass,
 * never sunk, nothing floating, no tree growing through a roof). The same rules build2011.py uses
 * for the 2011 places: clear the plot, a footing down to the ground, the ground round it eased back
 * to the natural level, never steeper than a block per block. Only natural blocks are ever cut or
 * filled; anything built (planks, cobble, rails, a resident's chest) is left alone.
 */
final class Terrain {
    private Terrain() {
    }

    /** Leaves, logs, grass, flowers, snow layers, vines: what sits on the ground, not the ground. */
    static boolean cover(Material m) {
        if (m.isAir() || Tag.LEAVES.isTagged(m) || Tag.LOGS.isTagged(m) || Tag.FLOWERS.isTagged(m) || Tag.SAPLINGS.isTagged(m)) return true;
        if (m == Material.WATER || m == Material.LAVA) return false;
        return Tag.REPLACEABLE.isTagged(m) || m == Material.SUGAR_CANE || m == Material.BROWN_MUSHROOM || m == Material.RED_MUSHROOM
                || m == Material.CACTUS || m == Material.COCOA || m == Material.BAMBOO || m == Material.SWEET_BERRY_BUSH;
    }

    /** Natural ground the grading may cut or fill. */
    static boolean natural(Material m) {
        return switch (m) {
            case GRASS_BLOCK, DIRT, COARSE_DIRT, PODZOL, ROOTED_DIRT, MYCELIUM, STONE, GRAVEL, SAND, SANDSTONE, CLAY, ANDESITE,
                 DIORITE, GRANITE, TUFF, DEEPSLATE, COAL_ORE, IRON_ORE, COPPER_ORE, DIRT_PATH -> true;
            default -> false;
        };
    }

    /** y of the top ground block at x, z (water counts as ground; trees and plants don't). */
    static int ground(World w, int x, int z) {
        int y = w.getHighestBlockYAt(x, z, HeightMap.WORLD_SURFACE);
        while (y > w.getMinHeight() && cover(w.getBlockAt(x, y, z).getType())) y--;
        return y;
    }

    /** Make the column's ground exactly y: cut natural ground and cover above it, fill below with dirt, top it. */
    static void surface(World w, int x, int z, int y, Material top) {
        int g = ground(w, x, z);
        for (int yy = y + 1; yy <= Math.max(g, y) + 2; yy++) {
            Block b = w.getBlockAt(x, yy, z);
            if (natural(b.getType()) || cover(b.getType()) && !Tag.LOGS.isTagged(b.getType())) b.setType(Material.AIR, false);
        }
        for (int yy = Math.min(g, y); yy < y; yy++) {
            Block b = w.getBlockAt(x, yy, z);
            if (cover(b.getType()) || b.getType() == Material.WATER) b.setType(top == Material.SAND ? Material.SAND : Material.DIRT, false);
        }
        w.getBlockAt(x, y, z).setType(top, false);
    }

    /**
     * Seat a plot whose floor blocks are at floorY: fell the trees over and beside it, clear the
     * plot above the floor, build a footing from the floor down to the ground, and ease the ground
     * round it (skirt blocks) back to the natural level. The caller lays the floor itself.
     */
    static void seat(World w, int x0, int z0, int x1, int z1, int floorY, Material footing, int skirt) {
        fell(w, x0 - skirt - 2, z0 - skirt - 2, x1 + skirt + 2, z1 + skirt + 2, floorY);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            int g = ground(w, x, z);
            for (int yy = floorY + 1; yy <= Math.max(g, floorY) + 8; yy++) {
                Block b = w.getBlockAt(x, yy, z);
                if (natural(b.getType()) || cover(b.getType())) b.setType(Material.AIR, false);
            }
            for (int yy = g + 1; yy < floorY; yy++) w.getBlockAt(x, yy, z).setType(footing, false);
            if (g < floorY) w.getBlockAt(x, floorY, z).setType(footing, false);
        }
        for (int x = x0 - skirt; x <= x1 + skirt; x++) for (int z = z0 - skirt; z <= z1 + skirt; z++) {
            int dx = Math.max(Math.max(x0 - x, 0), x - x1), dz = Math.max(Math.max(z0 - z, 0), z - z1);
            if (dx == 0 && dz == 0) continue;
            double d = Math.hypot(dx, dz);
            if (d > skirt + 0.5) continue;
            int g = ground(w, x, z);
            Material at = w.getBlockAt(x, g, z).getType();
            if (!natural(at)) continue;                          // water, a path's cobble, a build: leave it
            int target = (int) Math.round(floorY + (g - floorY) * Math.min(d / (skirt + 1), 1.0));
            int reach = (int) d;
            target = Math.max(floorY - reach, Math.min(floorY + reach, target));
            if (target != g) surface(w, x, z, target, at == Material.SAND || at == Material.SANDSTONE ? Material.SAND : Material.GRASS_BLOCK);
        }
    }

    /**
     * A made hill round a seated plot: the ground is raised (never cut) to a cone that falls one block
     * every `run` blocks from the floor at the plot's edge, until it meets the natural ground. Taking the
     * higher of the cone and the ground keeps every step between neighbours to a block at most, and
     * the edge of the made ground is wherever the cone runs into the real slope, so there's no rim.
     * Grass on dirt; trees standing on the slope keep standing (their foot is buried a little).
     */
    static void mound(World w, int x0, int z0, int x1, int z1, int floorY, int run, int reach) {
        for (int x = x0 - reach; x <= x1 + reach; x++) for (int z = z0 - reach; z <= z1 + reach; z++) {
            int dx = Math.max(Math.max(x0 - x, 0), x - x1), dz = Math.max(Math.max(z0 - z, 0), z - z1);
            if (dx == 0 && dz == 0) continue;
            double d = Math.hypot(dx, dz);
            int cone = floorY - (int) Math.floor(Math.max(0, d - 0.5) / run);
            int g = ground(w, x, z);
            Material at = w.getBlockAt(x, g, z).getType();
            if (cone <= g || !(natural(at) || at == Material.WATER)) continue;
            surface(w, x, z, cone, at == Material.SAND || at == Material.SANDSTONE ? Material.SAND : Material.GRASS_BLOCK);
        }
    }

    /**
     * Fell every tree whose trunk stands in the rectangle, then let the leaves that no log holds up
     * any more decay (Beta's rule build2011.py uses: leaves more than 4 steps from a log go).
     */
    static void fell(World w, int x0, int z0, int x1, int z1, int nearY) {
        int ylo = nearY - 6, yhi = nearY + 34;
        boolean felled = false;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = ylo; y <= yhi; y++) {
            Block b = w.getBlockAt(x, y, z);
            if (!Tag.LOGS.isTagged(b.getType())) continue;
            Material under = b.getRelative(0, -1, 0).getType();
            if (!Tag.DIRT.isTagged(under) && under != Material.GRASS_BLOCK) continue;  // only a trunk's foot; a built post stands on footing
            felled |= trunk(b);
            if (under == Material.DIRT) b.getRelative(0, -1, 0).setType(Material.GRASS_BLOCK, false);
        }
        if (felled) decay(w, x0 - 5, z0 - 5, x1 + 5, z1 + 5, ylo, yhi);   // a felled crown can reach past the plot
    }

    /** One tree's logs, flooding up and sideways (branches) from the base, never further than a crown reaches. */
    private static boolean trunk(Block base) {
        ArrayDeque<Block> q = new ArrayDeque<>();
        Set<Block> seen = new HashSet<>();
        q.add(base);
        boolean any = false;
        while (!q.isEmpty() && seen.size() < 400) {
            Block b = q.poll();
            if (!seen.add(b) || !Tag.LOGS.isTagged(b.getType())) continue;
            if (Math.abs(b.getX() - base.getX()) > 7 || Math.abs(b.getZ() - base.getZ()) > 7) continue;  // a big oak's branches
            b.setType(Material.AIR, false);
            any = true;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 || dy != 0 || dz != 0) q.add(b.getRelative(dx, dy, dz));
            }
        }
        return any;
    }

    private static void decay(World w, int x0, int z0, int x1, int z1, int ylo, int yhi) {
        Map<Block, Integer> dist = new HashMap<>();
        ArrayDeque<Block> q = new ArrayDeque<>();
        Set<Block> leaves = new HashSet<>();
        for (int x = x0 - 5; x <= x1 + 5; x++) for (int z = z0 - 5; z <= z1 + 5; z++) for (int y = ylo; y <= yhi; y++) {
            Block b = w.getBlockAt(x, y, z);
            if (Tag.LOGS.isTagged(b.getType())) {
                dist.put(b, 0);
                q.add(b);
            } else if (Tag.LEAVES.isTagged(b.getType())) {
                leaves.add(b);
            }
        }
        int[][] six = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty()) {
            Block b = q.poll();
            int d = dist.get(b);
            if (d >= 4) continue;
            for (int[] o : six) {
                Block n = b.getRelative(o[0], o[1], o[2]);
                if (leaves.contains(n) && !dist.containsKey(n)) {
                    dist.put(n, d + 1);
                    q.add(n);
                }
            }
        }
        for (Block b : leaves) {
            if (!dist.containsKey(b) && b.getX() >= x0 && b.getX() <= x1 && b.getZ() >= z0 && b.getZ() <= z1) b.setType(Material.AIR, false);
        }
    }
}
