package com.observance.watcher.structure;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deterministic, bounded physical shell for the required seven-house Unlit act and base mirror.
 * The geometry provides a believable copied village and safe circulation; no mandatory deduction
 * depends on tracing exact bespoke blocks. House mechanisms remain owned by the V5 physical
 * installer and the cross-surface campaign authority.
 */
public final class UnlitVillageCandidateBuilder {
    public static final int BORDER_RADIUS = 64;
    public static final int BUILD_RADIUS = 58;

    public record House(
            String siteId,
            int x,
            int z,
            int width,
            int depth,
            BlockFace front,
            Material wall,
            Material trim,
            Material roof,
            Material floor,
            Material workBlock) {
        public House {
            if (siteId == null || !siteId.startsWith("unlit_house_")) {
                throw new IllegalArgumentException("invalid Unlit house site id");
            }
            if (width < 9 || depth < 9 || width % 2 == 0 || depth % 2 == 0) {
                throw new IllegalArgumentException("Unlit house dimensions must be odd and at least 9");
            }
            if (!Set.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST).contains(front)) {
                throw new IllegalArgumentException("Unlit house front must be cardinal");
            }
        }

        public int minX() { return x - width / 2; }
        public int maxX() { return x + width / 2; }
        public int minZ() { return z - depth / 2; }
        public int maxZ() { return z + depth / 2; }

        public int doorX() {
            return x + front.getModX() * (width / 2);
        }

        public int doorZ() {
            return z + front.getModZ() * (depth / 2);
        }
    }

    public record BuildReport(int blocksWritten, int pathCells, List<House> houses) {
        public BuildReport {
            houses = List.copyOf(houses);
        }
    }

    private static final List<House> HOUSES = List.of(
            new House("unlit_house_lamp", -28, -27, 13, 11, BlockFace.SOUTH,
                    Material.DEEPSLATE_BRICKS, Material.CUT_COPPER, Material.OXIDIZED_CUT_COPPER,
                    Material.POLISHED_DEEPSLATE, Material.BARREL),
            new House("unlit_house_cairn", 0, -39, 11, 11, BlockFace.SOUTH,
                    Material.TUFF_BRICKS, Material.POLISHED_TUFF, Material.COBBLED_DEEPSLATE,
                    Material.TUFF_BRICKS, Material.STONECUTTER),
            new House("unlit_house_coop", 29, -27, 13, 11, BlockFace.WEST,
                    Material.MUD_BRICKS, Material.DARK_OAK_LOG, Material.DARK_OAK_PLANKS,
                    Material.PACKED_MUD, Material.LOOM),
            new House("unlit_house_well", 39, 3, 11, 13, BlockFace.WEST,
                    Material.PRISMARINE_BRICKS, Material.POLISHED_TUFF, Material.DARK_PRISMARINE,
                    Material.PRISMARINE, Material.CAULDRON),
            new House("unlit_house_watch", 27, 30, 13, 11, BlockFace.NORTH,
                    Material.POLISHED_BLACKSTONE_BRICKS, Material.IRON_BLOCK, Material.DEEPSLATE_TILES,
                    Material.POLISHED_BLACKSTONE, Material.SMITHING_TABLE),
            new House("unlit_house_warm", -4, 40, 13, 11, BlockFace.NORTH,
                    Material.RED_NETHER_BRICKS, Material.PACKED_MUD, Material.BRICKS,
                    Material.MUD_BRICKS, Material.BLAST_FURNACE),
            new House("unlit_house_threshold", -36, 21, 11, 13, BlockFace.EAST,
                    Material.STONE_BRICKS, Material.CHISELED_STONE_BRICKS, Material.BLACKSTONE,
                    Material.POLISHED_ANDESITE, Material.CARTOGRAPHY_TABLE),
            new House("unlit_house_base", 0, 0, 15, 15, BlockFace.SOUTH,
                    Material.DEEPSLATE_TILES, Material.WAXED_OXIDIZED_CUT_COPPER,
                    Material.POLISHED_BLACKSTONE, Material.POLISHED_DEEPSLATE,
                    Material.CHISELED_BOOKSHELF)
    );

    static {
        validatePlan(HOUSES);
    }

    public static List<House> houses() {
        return HOUSES;
    }

    public static void validatePlan(List<House> houses) {
        if (houses.size() != 8) throw new IllegalArgumentException("seven houses plus base are required");
        Set<String> ids = new HashSet<>();
        for (House house : houses) {
            if (!ids.add(house.siteId())) throw new IllegalArgumentException("duplicate site " + house.siteId());
            int far = Math.max(Math.max(Math.abs(house.minX()), Math.abs(house.maxX())),
                    Math.max(Math.abs(house.minZ()), Math.abs(house.maxZ())));
            if (far > BUILD_RADIUS) throw new IllegalArgumentException("house outside bounded build: " + house.siteId());
        }
        Set<String> required = Set.of(
                "unlit_house_lamp", "unlit_house_cairn", "unlit_house_coop",
                "unlit_house_well", "unlit_house_watch", "unlit_house_warm",
                "unlit_house_threshold", "unlit_house_base");
        if (!ids.equals(required)) throw new IllegalArgumentException("Unlit house identities drifted");
        for (int left = 0; left < houses.size(); left++) {
            for (int right = left + 1; right < houses.size(); right++) {
                House a = houses.get(left);
                House b = houses.get(right);
                boolean separated = a.maxX() + 4 < b.minX() || b.maxX() + 4 < a.minX()
                        || a.maxZ() + 4 < b.minZ() || b.maxZ() + 4 < a.minZ();
                if (!separated) throw new IllegalArgumentException(
                        "Unlit houses overlap or lose circulation: " + a.siteId() + " / " + b.siteId());
            }
        }
    }

    public void requireFresh(World world, int originX, int baseY, int originZ) {
        for (int x = -BUILD_RADIUS; x <= BUILD_RADIUS; x++) {
            for (int z = -BUILD_RADIUS; z <= BUILD_RADIUS; z++) {
                if (x * x + z * z > BUILD_RADIUS * BUILD_RADIUS) continue;
                for (int y = baseY; y <= baseY + 8; y++) {
                    Material material = world.getBlockAt(originX + x, y, originZ + z).getType();
                    if (!material.isAir()) {
                        throw new IllegalStateException("Unlit candidate refused occupied build cell "
                                + material + " at " + (originX + x) + ',' + y + ',' + (originZ + z));
                    }
                }
            }
        }
    }

    public BuildReport buildFresh(World world, int originX, int baseY, int originZ) {
        requireFresh(world, originX, baseY, originZ);
        Counter counter = new Counter();
        buildIsland(world, originX, baseY, originZ, counter);
        int pathCells = buildPaths(world, originX, baseY, originZ, counter);
        for (House house : HOUSES) buildHouse(world, originX, baseY, originZ, house, counter);
        buildWellMouth(world, originX, baseY, originZ, counter);
        buildEntry(world, originX, baseY, originZ, counter);
        return new BuildReport(counter.blocks, pathCells, HOUSES);
    }

    public static Location siteLocation(World world, int originX, int baseY, int originZ, House house) {
        return new Location(world, originX + house.x(), baseY + 1, originZ + house.z(), yaw(house.front()), 0f);
    }

    public static float yaw(BlockFace face) {
        return switch (face) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> throw new IllegalArgumentException("non-cardinal face");
        };
    }

    private static void buildIsland(World world, int originX, int baseY, int originZ, Counter counter) {
        for (int x = -BUILD_RADIUS; x <= BUILD_RADIUS; x++) {
            for (int z = -BUILD_RADIUS; z <= BUILD_RADIUS; z++) {
                int distance = x * x + z * z;
                if (distance > BUILD_RADIUS * BUILD_RADIUS) continue;
                set(world, originX + x, baseY - 3, originZ + z, Material.DEEPSLATE, counter);
                set(world, originX + x, baseY - 2, originZ + z,
                        ((x * 17 + z * 31) & 15) == 0 ? Material.BASALT : Material.COBBLED_DEEPSLATE,
                        counter);
                Material top = ((x * 13 - z * 7) & 31) == 0
                        ? Material.SOUL_SOIL : Material.DEEPSLATE;
                set(world, originX + x, baseY - 1, originZ + z, top, counter);
            }
        }
    }

    private static int buildPaths(World world, int originX, int baseY, int originZ, Counter counter) {
        Set<Long> cells = new HashSet<>();
        for (House house : HOUSES) {
            if (house.siteId().equals("unlit_house_base")) continue;
            int x = house.doorX() + house.front().getModX();
            int z = house.doorZ() + house.front().getModZ();
            while (x != 0) {
                pathCell(world, originX, baseY, originZ, x, z, house.front(), cells, counter);
                x += x > 0 ? -1 : 1;
            }
            while (z != 0) {
                pathCell(world, originX, baseY, originZ, x, z, house.front(), cells, counter);
                z += z > 0 ? -1 : 1;
            }
        }
        for (int x = -11; x <= 11; x++) {
            for (int z = -11; z <= 11; z++) {
                if (x * x + z * z > 121) continue;
                cells.add(pack(x, z));
                set(world, originX + x, baseY - 1, originZ + z,
                        ((x + z) & 3) == 0 ? Material.POLISHED_BLACKSTONE : Material.POLISHED_DEEPSLATE,
                        counter);
            }
        }
        return cells.size();
    }

    private static void pathCell(World world, int originX, int baseY, int originZ,
                                 int x, int z, BlockFace direction, Set<Long> cells, Counter counter) {
        boolean alongX = direction == BlockFace.EAST || direction == BlockFace.WEST;
        for (int offset = -1; offset <= 1; offset++) {
            int px = alongX ? x : x + offset;
            int pz = alongX ? z + offset : z;
            cells.add(pack(px, pz));
            set(world, originX + px, baseY - 1, originZ + pz,
                    offset == 0 ? Material.POLISHED_DEEPSLATE : Material.CRACKED_DEEPSLATE_TILES,
                    counter);
        }
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static void buildHouse(World world, int originX, int baseY, int originZ,
                                   House house, Counter counter) {
        int minX = originX + house.minX();
        int maxX = originX + house.maxX();
        int minZ = originZ + house.minZ();
        int maxZ = originZ + house.maxZ();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                set(world, x, baseY, z, house.floor(), counter);
                boolean edge = x == minX || x == maxX || z == minZ || z == maxZ;
                if (!edge) continue;
                for (int y = baseY + 1; y <= baseY + 4; y++) {
                    boolean trim = y == baseY + 1 || y == baseY + 4
                            || ((x == minX || x == maxX) && (z == minZ || z == maxZ));
                    set(world, x, y, z, trim ? house.trim() : house.wall(), counter);
                }
            }
        }
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int z = minZ - 1; z <= maxZ + 1; z++) {
                set(world, x, baseY + 5, z, house.roof(), counter);
                if (x == minX - 1 || x == maxX + 1 || z == minZ - 1 || z == maxZ + 1) {
                    set(world, x, baseY + 6, z, house.trim(), counter);
                }
            }
        }
        openDoor(world, originX, baseY, originZ, house, counter);
        if (house.siteId().equals("unlit_house_base")) {
            for (BlockFace face : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                openDoor(world, originX, baseY, originZ,
                        new House(house.siteId(), house.x(), house.z(), house.width(), house.depth(), face,
                                house.wall(), house.trim(), house.roof(), house.floor(), house.workBlock()), counter);
            }
        }
        addWindows(world, originX, baseY, originZ, house, counter);
        furnish(world, originX, baseY, originZ, house, counter);
    }

    private static void openDoor(World world, int originX, int baseY, int originZ,
                                 House house, Counter counter) {
        int x = originX + house.doorX();
        int z = originZ + house.doorZ();
        for (int y = baseY + 1; y <= baseY + 3; y++) set(world, x, y, z, Material.AIR, counter);
        set(world, x, baseY, z, Material.POLISHED_BLACKSTONE, counter);
        for (int side : List.of(-1, 1)) {
            int sx = x + (house.front().getModZ() * side);
            int sz = z + (house.front().getModX() * side);
            set(world, sx, baseY + 1, sz, house.trim(), counter);
            set(world, sx, baseY + 2, sz, house.trim(), counter);
        }
    }

    private static void addWindows(World world, int originX, int baseY, int originZ,
                                   House house, Counter counter) {
        for (int direction : List.of(-1, 1)) {
            int wx = originX + house.x() + direction * Math.max(2, house.width() / 3);
            set(world, wx, baseY + 2, originZ + house.minZ(), Material.TINTED_GLASS, counter);
            set(world, wx, baseY + 3, originZ + house.minZ(), Material.TINTED_GLASS, counter);
            set(world, wx, baseY + 2, originZ + house.maxZ(), Material.TINTED_GLASS, counter);
            set(world, wx, baseY + 3, originZ + house.maxZ(), Material.TINTED_GLASS, counter);
            int zz = originZ + house.z() + direction * Math.max(2, house.depth() / 3);
            set(world, originX + house.minX(), baseY + 2, zz, Material.TINTED_GLASS, counter);
            set(world, originX + house.maxX(), baseY + 2, zz, Material.TINTED_GLASS, counter);
            set(world, originX + house.minX(), baseY + 3, zz, Material.TINTED_GLASS, counter);
            set(world, originX + house.maxX(), baseY + 3, zz, Material.TINTED_GLASS, counter);
        }
    }

    private static void furnish(World world, int originX, int baseY, int originZ,
                                House house, Counter counter) {
        int left = originX + house.minX() + 1;
        int right = originX + house.maxX() - 1;
        int back = originZ + house.minZ() + 1;
        int front = originZ + house.maxZ() - 1;
        set(world, left, baseY + 1, back, house.workBlock(), counter);
        set(world, right, baseY + 1, front, house.workBlock(), counter);
        set(world, left, baseY + 1, front, Material.BARREL, counter);
        set(world, right, baseY + 1, back, Material.CHISELED_BOOKSHELF, counter);
        faceIfDirectional(world.getBlockAt(right, baseY + 1, back), BlockFace.SOUTH);
        set(world, left + 1, baseY + 1, back, Material.OAK_STAIRS, counter);
        faceIfDirectional(world.getBlockAt(left + 1, baseY + 1, back), BlockFace.EAST);
        if (house.siteId().equals("unlit_house_coop")) {
            set(world, right - 1, baseY + 1, back, Material.HAY_BLOCK, counter);
            set(world, right - 2, baseY + 1, back, Material.OAK_FENCE, counter);
        } else if (house.siteId().equals("unlit_house_well")) {
            set(world, left + 1, baseY + 1, front, Material.WATER_CAULDRON, counter);
        } else if (house.siteId().equals("unlit_house_watch")) {
            set(world, right - 1, baseY + 2, front, Material.IRON_BARS, counter);
            set(world, right - 1, baseY + 1, front, Material.BELL, counter);
        } else if (house.siteId().equals("unlit_house_warm")) {
            set(world, left + 1, baseY + 1, front, Material.FURNACE, counter);
            faceIfDirectional(world.getBlockAt(left + 1, baseY + 1, front), BlockFace.NORTH);
        }
    }

    private static void buildWellMouth(World world, int originX, int baseY, int originZ, Counter counter) {
        int wellZ = originZ + 13;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                    set(world, originX + x, baseY, wellZ + z, Material.POLISHED_BLACKSTONE_BRICKS, counter);
                } else if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    set(world, originX + x, baseY, wellZ + z, Material.WATER, counter);
                }
            }
        }
    }

    private static void buildEntry(World world, int originX, int baseY, int originZ, Counter counter) {
        int z = originZ + 52;
        for (int x = -4; x <= 4; x++) {
            set(world, originX + x, baseY, z, Material.POLISHED_DEEPSLATE, counter);
            if (Math.abs(x) >= 3) {
                for (int y = 1; y <= 5; y++) {
                    set(world, originX + x, baseY + y, z, Material.DEEPSLATE_BRICKS, counter);
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            set(world, originX + x, baseY + 6, z, Material.DEEPSLATE_TILES, counter);
        }
    }

    private static void faceIfDirectional(Block block, BlockFace face) {
        if (!(block.getBlockData() instanceof Directional directional)) return;
        if (!directional.getFaces().contains(face)) return;
        directional.setFacing(face);
        block.setBlockData(directional, false);
    }

    private static void set(World world, int x, int y, int z, Material material, Counter counter) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == material) return;
        block.setType(material, false);
        counter.blocks++;
    }

    private static final class Counter { int blocks; }
}
