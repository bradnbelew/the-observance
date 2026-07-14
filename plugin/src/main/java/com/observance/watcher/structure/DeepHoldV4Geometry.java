package com.observance.watcher.structure;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Stairs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Authored V4 Deep Hold shell and circulation builder.
 *
 * <p>This class owns architecture only. Canonical puzzle fixtures, books, state listeners, answer
 * sites, and gate synchronization remain owned by the Observance runtime. Keeping those layers
 * separate prevents a room shell from erasing a fixture or a fixture template from clearing a
 * neighboring room.</p>
 */
public final class DeepHoldV4Geometry {

    private final World world;
    private final int ox;
    private final int oy;
    private final int oz;
    private final OperationBuffer operations;

    private DeepHoldV4Geometry(World world, Location mouth, OperationBuffer operations) {
        this.world = world;
        this.ox = mouth.getBlockX();
        this.oy = mouth.getBlockY();
        this.oz = mouth.getBlockZ();
        this.operations = operations;
    }

    public record Survey(boolean safe, List<String> issues, int minimumSurfaceY,
                         int highestAuthoredRoofY, int lowestAuthoredFoundationY) { }

    public record ChunkCoordinate(int x, int z) { }

    public record BuildResult(long changedBlocks, int rooms, int gates, int fixturesReserved) { }

    public record BatchResult(int processedOperations, int cursor, int totalOperations,
                              long changedBlocks, boolean complete) { }

    /**
     * Deterministic primitive-only shell plan.  Planning performs no block mutation; callers apply a
     * bounded number of operations per server tick and may persist/restore {@link #cursor()} after a
     * restart. Duplicate writes remain ordered because later circulation passes intentionally reopen
     * room-shell walls.
     */
    public static final class BuildPlan {
        private final World world;
        private final UUID worldId;
        private final int ox;
        private final int oy;
        private final int oz;
        private final int[] coordinates;
        private final short[] materials;
        private int cursor;
        private long changed;

        private BuildPlan(World world, int ox, int oy, int oz, OperationBuffer operations) {
            this.world = world;
            this.worldId = world.getUID();
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
            this.coordinates = operations.coordinates();
            this.materials = operations.materials();
        }

        public UUID worldId() { return worldId; }
        public int originX() { return ox; }
        public int originY() { return oy; }
        public int originZ() { return oz; }
        public int totalOperations() { return coordinates.length; }
        public int cursor() { return cursor; }
        public long changedBlocks() { return changed; }
        public boolean complete() { return cursor >= coordinates.length; }

        /** Restore a durable checkpoint. Replaying operations after an older checkpoint is safe. */
        public void restoreCursor(int savedCursor) {
            if (savedCursor < 0 || savedCursor > coordinates.length) {
                throw new IllegalArgumentException("Hold build cursor " + savedCursor
                        + " is outside 0.." + coordinates.length);
            }
            cursor = savedCursor;
        }

        /** Apply at most maxOperations and maxNanos of work on the current thread. */
        public BatchResult applyBatch(int maxOperations, long maxNanos) {
            if (maxOperations <= 0) throw new IllegalArgumentException("maxOperations must be positive");
            if (world == null || !world.getUID().equals(worldId)) {
                throw new IllegalStateException("Deep Hold build world changed while applying its plan");
            }
            long started = System.nanoTime();
            int processed = 0;
            Material[] palette = Material.values();
            while (cursor < coordinates.length && processed < maxOperations
                    && (maxNanos <= 0L || System.nanoTime() - started < maxNanos)) {
                int packed = coordinates[cursor];
                int x = (packed & 0xff) - 128;
                int y = ((packed >>> 8) & 0xff) - 128;
                int z = ((packed >>> 16) & 0x1ff) - 16;
                int ordinal = Short.toUnsignedInt(materials[cursor]);
                if (ordinal >= palette.length) throw new IllegalStateException("Unknown material ordinal " + ordinal);
                int blockX = ox + x;
                int blockZ = oz + z;
                if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                    throw new IllegalStateException("Deep Hold footprint chunk " + (blockX >> 4) + ","
                            + (blockZ >> 4) + " lost its preparation ticket during build");
                }
                Block block = world.getBlockAt(blockX, oy + y, blockZ);
                Material material = palette[ordinal];
                if (block.getType() != material) {
                    block.setType(material, false);
                    changed++;
                }
                cursor++;
                processed++;
            }
            return new BatchResult(processed, cursor, coordinates.length, changed, complete());
        }

        public BuildResult result() {
            if (!complete()) throw new IllegalStateException("Deep Hold plan is not completely applied");
            return new BuildResult(changed, DeepHoldV4Plan.ROOMS.size(), DeepHoldV4Plan.GATES.size(),
                    DeepHoldV4Plan.FIXTURES.size());
        }
    }

    private static final class OperationBuffer {
        private int[] coordinates = new int[1 << 18];
        private short[] materials = new short[1 << 18];
        private int size;

        void add(int x, int y, int z, Material material) {
            if (x < -128 || x > 127 || y < -128 || y > 127 || z < -16 || z > 495) {
                throw new IllegalStateException("Deep Hold write outside packed bounds: "
                        + x + "," + y + "," + z);
            }
            int ordinal = material.ordinal();
            if (ordinal > 0xffff) throw new IllegalStateException("Material palette exceeds packed range");
            if (size == coordinates.length) {
                int next = Math.multiplyExact(size, 2);
                coordinates = Arrays.copyOf(coordinates, next);
                materials = Arrays.copyOf(materials, next);
            }
            coordinates[size] = (x + 128) | ((y + 128) << 8) | ((z + 16) << 16);
            materials[size] = (short) ordinal;
            size++;
        }

        int[] coordinates() { return Arrays.copyOf(coordinates, size); }
        short[] materials() { return Arrays.copyOf(materials, size); }
    }

    /** Read-only survey. A failed survey must never be followed by a partial build. */
    public static Survey survey(World world, Location mouth) {
        List<String> issues = new ArrayList<>();
        if (world == null || mouth == null || mouth.getWorld() != world) {
            issues.add("surface mouth has no loaded build world");
            return new Survey(false, List.copyOf(issues), Integer.MIN_VALUE,
                    Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        List<String> planIssues = DeepHoldV4Plan.validate();
        if (!planIssues.isEmpty()) issues.addAll(planIssues);

        int mouthY = mouth.getBlockY();
        int lowest = mouthY + DeepHoldV4Plan.MIN_Y - 3;
        int bottomRequired = world.getMinHeight() + DeepHoldV4Plan.MIN_BOTTOM_BUFFER;
        if (lowest < bottomRequired) {
            issues.add("Hold foundation would reach Y " + lowest + "; requires at least " + bottomRequired);
        }

        int highestRoof = mouthY - 14; // keeper nave ceiling -16 plus its three roof layers
        int ungenerated = 0;
        int unloaded = 0;
        for (ChunkCoordinate chunk : requiredChunks(mouth)) {
            if (!world.isChunkGenerated(chunk.x(), chunk.z())) ungenerated++;
            else if (!world.isChunkLoaded(chunk.x(), chunk.z())) unloaded++;
        }
        if (ungenerated > 0 || unloaded > 0) {
            if (ungenerated > 0) issues.add(ungenerated + " Hold footprint chunks are not generated");
            if (unloaded > 0) issues.add(unloaded + " Hold footprint chunks are not loaded/ticketed");
            issues.add("run /obs placehold prepare at this exact Mouth before plan/build");
            return new Survey(false, List.copyOf(issues), Integer.MIN_VALUE, highestRoof, lowest);
        }
        int minSurface = Integer.MAX_VALUE;
        // The explicit preparation command generated and ticketed every footprint chunk. Survey
        // therefore reads real columns without ever hiding synchronous generation inside a plan.
        // Survey the authored footprint at an eight-block grid. The first 36 Z blocks are the
        // deliberate Mouth/descent earthwork; everything beyond must remain deeply buried.
        for (int x = DeepHoldV4Plan.MIN_X; x <= DeepHoldV4Plan.MAX_X; x += 8) {
            for (int z = 36; z <= DeepHoldV4Plan.MAX_Z; z += 8) {
                int surface = actualSurfaceY(world, mouth.getBlockX() + x, mouth.getBlockZ() + z);
                minSurface = Math.min(minSurface, surface);
            }
        }
        if (minSurface == Integer.MAX_VALUE) minSurface = mouthY;
        if (minSurface - highestRoof < DeepHoldV4Plan.MIN_SURFACE_COVER) {
            issues.add("minimum sampled surface Y " + minSurface + " gives only "
                    + (minSurface - highestRoof) + " blocks above the highest non-Mouth roof; requires "
                    + DeepHoldV4Plan.MIN_SURFACE_COVER);
        }

        int maxTop = world.getMaxHeight() - 2;
        if (mouthY + DeepHoldV4Plan.MAX_Y > maxTop) {
            issues.add("Surface Mouth crown would exceed build height " + maxTop);
        }
        return new Survey(issues.isEmpty(), List.copyOf(issues), minSurface, highestRoof, lowest);
    }

    /** Complete build envelope, used by the asynchronous preparation phase and read-only survey. */
    public static List<ChunkCoordinate> requiredChunks(Location mouth) {
        if (mouth == null) return List.of();
        int minChunkX = (mouth.getBlockX() + DeepHoldV4Plan.MIN_X - DeepHoldV4Plan.ENVELOPE) >> 4;
        int maxChunkX = (mouth.getBlockX() + DeepHoldV4Plan.MAX_X + DeepHoldV4Plan.ENVELOPE) >> 4;
        int minChunkZ = (mouth.getBlockZ() + DeepHoldV4Plan.MIN_Z - DeepHoldV4Plan.ENVELOPE) >> 4;
        int maxChunkZ = (mouth.getBlockZ() + DeepHoldV4Plan.MAX_Z + DeepHoldV4Plan.ENVELOPE) >> 4;
        List<ChunkCoordinate> chunks = new ArrayList<>();
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) chunks.add(new ChunkCoordinate(x, z));
        }
        return List.copyOf(chunks);
    }

    /** Heightmap caches can be stale on newly generated Paper chunks; inspect the real column. */
    private static int actualSurfaceY(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Material material = world.getBlockAt(x, y, z).getType();
            if (material.isAir() || material.name().endsWith("_LEAVES")) continue;
            return y;
        }
        return world.getMinHeight();
    }

    public static BuildPlan plan(World world, Location mouth, Consumer<String> progress) {
        Survey survey = survey(world, mouth);
        if (!survey.safe()) {
            throw new IllegalStateException("Unsafe Deep Hold V5 placement: " + String.join("; ", survey.issues()));
        }
        OperationBuffer operations = new OperationBuffer();
        DeepHoldV4Geometry builder = new DeepHoldV4Geometry(world, mouth, operations);
        builder.message(progress, "planning the single Surface Mouth and four-flight Grand Stair");
        builder.buildSurfaceMouthAndGrandStair();

        builder.message(progress, "planning 32 isolated authored room shells across three strata");
        for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) builder.buildRoomShell(room);

        builder.message(progress, "planning the reversible main route and district loops");
        builder.carveAuthoredCirculation();

        builder.message(progress, "planning room-specific civic architecture and sightline dressing");
        for (DeepHoldV4Plan.Room room : DeepHoldV4Plan.ROOMS) builder.dressRoom(room);

        builder.message(progress, "planning six main gatehouses and two controlled branch gates");
        for (DeepHoldV4Plan.Gate gate : DeepHoldV4Plan.GATES) builder.buildGatehouse(gate);

        builder.message(progress, "planning the burial envelope and surface seal");
        builder.finishSurfaceMouth();
        return new BuildPlan(world, mouth.getBlockX(), mouth.getBlockY(), mouth.getBlockZ(), operations);
    }

    /** Compatibility path for tests/tools. Production commands use {@link #plan} in bounded ticks. */
    public static BuildResult build(World world, Location mouth, Consumer<String> progress) {
        BuildPlan plan = plan(world, mouth, progress);
        while (!plan.complete()) plan.applyBatch(Integer.MAX_VALUE, 0L);
        return plan.result();
    }

    private void buildSurfaceMouthAndGrandStair() {
        // A broad civic mouth. The first 36 blocks are deliberate surface earthwork; after that the
        // stair is below the required cover and no underground roof touches daylight.
        for (int z = -6; z <= 8; z++) {
            int half = z < 0 ? 12 : 15;
            int floor = 0;
            buildVaultSlice(0, floor, z, half, 11, z <= 0);
        }
        buildFacadeArch(0, 0, 0, 15, 12);

        for (int z = 9; z <= 106; z++) {
            int drop = stairDrop(z);
            int floor = -drop;
            buildVaultSlice(0, floor, z, 8, 9, false);
            placeGrandStairTread(z, floor);
        }
        // Full lower landing opens into Orientation. It remains reversible for the whole ARG.
        corridorZ(0, -40, 102, 108, 8, 10);
        floorInlayZ(0, -40, 102, 112, 3, Material.CUT_COPPER);
    }

    private int stairDrop(int z) {
        if (z <= 18) return Math.min(10, z - 8);
        if (z <= 24) return 10;
        if (z <= 34) return 10 + (z - 24);
        if (z <= 40) return 20;
        if (z <= 60) return 20 + ((z - 40) / 2);
        if (z <= 68) return 30;
        if (z <= 88) return 30 + ((z - 68) / 2);
        return 40;
    }

    private void placeGrandStairTread(int z, int floor) {
        for (int x = -7; x <= 7; x++) {
            set(x, floor - 1, z, stairFloorMaterial(x, z));
            // The authored floor coordinate is the player's feet cell.  Earlier prototypes put
            // top-half stair blocks here, making the route look plausible while physically
            // blocking Adventure-mode traversal.  Full support blocks one cell below give the
            // same stepped descent and leave both feet and head cells provably clear.
            set(x, floor, z, Material.AIR);
            set(x, floor + 1, z, Material.AIR);
        }
        if (z == 24 || z == 40 || z == 68 || z == 88) {
            for (int x : new int[]{-6, 6}) {
                set(x, floor, z, Material.CHISELED_TUFF);
                set(x, floor + 1, z, Material.DEEPSLATE_BRICK_WALL);
                set(x, floor + 2, z, Material.SOUL_LANTERN);
            }
        }
    }

    private Material stairFloorMaterial(int x, int z) {
        if (Math.abs(x) <= 1) return Math.floorMod(z, 8) == 0
                ? Material.OXIDIZED_CUT_COPPER : Material.POLISHED_DEEPSLATE;
        return Math.floorMod(x + z, 11) == 0 ? Material.TUFF_BRICKS : Material.DEEPSLATE_TILES;
    }

    private void buildRoomShell(DeepHoldV4Plan.Room room) {
        int floor = room.floorY();
        int ceiling = room.ceilingY();
        Material wall = wallMaterial(room);
        Material accent = accentMaterial(room);
        for (int x = room.minX(); x <= room.maxX(); x++) {
            for (int z = room.minZ(); z <= room.maxZ(); z++) {
                boolean perimeter = x <= room.minX() + 2 || x >= room.maxX() - 2
                        || z <= room.minZ() + 2 || z >= room.maxZ() - 2;
                for (int y = floor - 3; y <= ceiling + 2; y++) {
                    boolean foundation = y < floor;
                    boolean roof = y >= ceiling;
                    if (foundation) {
                        set(x, y, z, y == floor - 1 ? floorMaterial(room, x, z) : Material.DEEPSLATE);
                    } else if (roof || perimeter) {
                        set(x, y, z, shellPattern(wall, accent, room, x, y, z));
                    } else {
                        set(x, y, z, Material.AIR);
                    }
                }
            }
        }
        addRoomRibs(room, accent);
        addExteriorButtresses(room, wall, accent);
    }

    private Material floorMaterial(DeepHoldV4Plan.Room room, int x, int z) {
        int hash = Math.floorMod(x * 19 + z * 31, 17);
        if (Math.abs(x - ((room.minX() + room.maxX()) / 2)) <= 2) {
            return room.floorY() <= -90 ? Material.POLISHED_BLACKSTONE_BRICKS
                    : (room.floorY() <= -60 ? Material.TUFF_BRICKS : Material.POLISHED_DEEPSLATE);
        }
        if (hash == 0) return Material.CRACKED_DEEPSLATE_TILES;
        if (hash == 1) return Material.POLISHED_BASALT;
        return room.floorY() <= -90 ? Material.POLISHED_BLACKSTONE
                : (room.floorY() <= -60 ? Material.POLISHED_TUFF : Material.DEEPSLATE_TILES);
    }

    private Material shellPattern(Material wall, Material accent, DeepHoldV4Plan.Room room,
                                  int x, int y, int z) {
        int hash = Math.floorMod((x * 17) + (y * 13) + (z * 29) + room.id().hashCode(), 43);
        if (hash == 0 || hash == 1) return accent;
        if (hash == 2) return Material.CRACKED_DEEPSLATE_BRICKS;
        return wall;
    }

    private Material wallMaterial(DeepHoldV4Plan.Room room) {
        if (room.floorY() <= -90) return Material.POLISHED_BLACKSTONE_BRICKS;
        if (room.floorY() <= -60) return Material.TUFF_BRICKS;
        return Material.DEEPSLATE_BRICKS;
    }

    private Material accentMaterial(DeepHoldV4Plan.Room room) {
        String id = room.id();
        if (id.contains("water") || id.contains("cistern")) return Material.OXIDIZED_CUT_COPPER;
        if (id.contains("warm") || id.contains("market")) return Material.WAXED_EXPOSED_COPPER;
        if (id.contains("dread") || id.contains("unwriting")) return Material.SCULK;
        if (id.contains("accepting")) return Material.CHISELED_TUFF;
        if (id.contains("keeper")) return Material.POLISHED_BASALT;
        return room.floorY() <= -90 ? Material.CHISELED_POLISHED_BLACKSTONE : Material.CHISELED_TUFF;
    }

    private void addRoomRibs(DeepHoldV4Plan.Room room, Material accent) {
        boolean alongZ = (room.maxZ() - room.minZ()) >= (room.maxX() - room.minX());
        int start = alongZ ? room.minZ() + 8 : room.minX() + 8;
        int end = alongZ ? room.maxZ() - 8 : room.maxX() - 8;
        for (int p = start; p <= end; p += 14) {
            if (alongZ) {
                for (int x = room.minX() + 3; x <= room.maxX() - 3; x++) {
                    if (reserved(room.id(), x, room.ceilingY() - 1, p, 1)) continue;
                    if (Math.abs(x - room.minX()) <= 4 || Math.abs(room.maxX() - x) <= 4
                            || Math.floorMod(x - room.minX(), 7) == 0) {
                        set(x, room.ceilingY() - 1, p, accent);
                    }
                }
            } else {
                for (int z = room.minZ() + 3; z <= room.maxZ() - 3; z++) {
                    if (reserved(room.id(), p, room.ceilingY() - 1, z, 1)) continue;
                    if (Math.abs(z - room.minZ()) <= 4 || Math.abs(room.maxZ() - z) <= 4
                            || Math.floorMod(z - room.minZ(), 7) == 0) {
                        set(p, room.ceilingY() - 1, z, accent);
                    }
                }
            }
        }
    }

    private void addExteriorButtresses(DeepHoldV4Plan.Room room, Material wall, Material accent) {
        for (int x = room.minX() + 6; x <= room.maxX() - 6; x += 12) {
            buildButtress(x, room.floorY(), room.minZ(), room.ceilingY(), wall, accent, true);
            buildButtress(x, room.floorY(), room.maxZ(), room.ceilingY(), wall, accent, true);
        }
        for (int z = room.minZ() + 6; z <= room.maxZ() - 6; z += 12) {
            buildButtress(room.minX(), room.floorY(), z, room.ceilingY(), wall, accent, false);
            buildButtress(room.maxX(), room.floorY(), z, room.ceilingY(), wall, accent, false);
        }
    }

    private void buildButtress(int x, int floor, int z, int ceiling, Material wall,
                               Material accent, boolean northSouthWall) {
        for (int y = floor; y < ceiling; y++) {
            int height = y - floor;
            int depth = height < 4 ? 2 : (height < 9 ? 1 : 0);
            for (int d = 0; d <= depth; d++) {
                int dx = northSouthWall ? 0 : d;
                int dz = northSouthWall ? d : 0;
                set(x + dx, y, z + dz, height % 6 == 0 ? accent : wall);
            }
        }
    }

    private void carveAuthoredCirculation() {
        // Upper Hold and the six Keeper bays.
        // Room shells are built after the raw Grand Stair, so reopen the deliberately owned
        // three-block Orientation threshold here.  Keeping this in the circulation pass prevents
        // the room perimeter from silently walling off the one public entrance.
        corridorZ(0, -40, 102, 110, 8, 10);
        corridorZ(0, -40, 150, 164, 8, 11);
        for (int z : new int[]{176, 206, 238}) {
            corridorX(-34, -40, z, -38, -26, 4, 9);
            corridorX(34, -40, z, 26, 38, 4, 9);
        }

        // G2 leads into a grand descending switchback, then south Archive landing. The route is
        // deliberately folded so the Hold is deep without becoming 900 blocks long.
        corridorZ(0, -40, 244, 257, 8, 11);
        buildUpperToCivicSwitchback();

        // Civic nave and two complete evidence streets. Each street reconnects at both ends and at
        // the middle, so no exhibit can block the district or become an accidental maze.
        corridorZ(0, -68, 96, 300, 8, 10);
        corridorZ(-39, -68, 118, 288, 4, 8);
        corridorZ(39, -68, 118, 288, 4, 8);
        for (int z : new int[]{120, 224, 286}) corridorX(0, -68, z, -39, 39, 4, 8);
        int[] westRoomZ = {126, 158, 191, 226, 258, 287};
        for (int z : westRoomZ) corridorX(-43, -68, z, -52, -35, 4, 8);
        int[] eastRoomZ = {126, 158, 191, 226, 258, 287};
        for (int z : eastRoomZ) corridorX(43, -68, z, 35, 52, 4, 8);
        // Horizontal corridor sidewalls are desirable between junctions, but must not run across
        // the north/south nave or either evidence street. Open every authored crossing as a real
        // plaza after both corridor axes exist.
        for (int z : new int[]{120, 224, 286}) openIntersection(0, -68, z, 8, 8, 9);
        for (int z : westRoomZ) openIntersection(-39, -68, z, 7, 7, 8);
        for (int z : eastRoomZ) openIntersection(39, -68, z, 7, 7, 8);
        openIntersection(10, -68, 258, 4, 6, 9);
        corridorZ(0, -68, 90, 104, 8, 10);

        // Puzzle Works to Lower Hold: a second reversible folded stair.
        buildCivicToLowerSwitchback();
        corridorZ(0, -96, 34, 118, 8, 11);
        openIntersection(10, -96, 34, 4, 6, 10);
        openIntersection(-10, -96, 72, 4, 6, 9);

        // Threshold branches and reconvergence.
        corridorX(-39, -96, 136, -52, -26, 4, 9);
        corridorZ(-80, -96, 150, 162, 5, 9);
        corridorX(39, -96, 136, 26, 38, 4, 9);
        corridorZ(52, -96, 150, 162, 5, 9);
        // The Vault's closed co-op ring intentionally cannot be crossed. Give its west-side
        // witness frame a dedicated aisle from the north intake instead of forcing players through
        // the ritual footprint.
        corridorZ(40, -96, 158, 184, 4, 8);
        openIntersection(46, -96, 162, 10, 6, 8);
        corridorX(72, -96, 132, 26, 78, 4, 9);
        corridorX(72, -96, 216, 26, 78, 4, 9);
        // Dread's eastbound arcade crosses the mandatory Threshold district. Its south sidewall
        // must become a real junction here, not a barrier that forces players into the optional arm.
        openIntersection(39, -96, 138, 6, 5, 8);

        // One linear route through Accepting, Coda, Release, and back the same way.
        corridorZ(0, -96, 216, 232, 9, 12);
        corridorZ(0, -96, 288, 304, 9, 12);
        corridorZ(0, -96, 346, 358, 8, 10);
    }

    private void buildUpperToCivicSwitchback() {
        // Upper landing: southward descent on west side, civic return on east side.
        corridorZ(-10, -40, 252, 258, 6, 10);
        for (int z = 258; z <= 286; z++) {
            int drop = Math.min(14, (z - 258) / 2);
            buildVaultSlice(-10, -40 - drop, z, 6, 9, false);
            placeSimpleTread(-10, -40 - drop, z, 5, BlockFace.SOUTH);
        }
        corridorX(0, -54, 286, -10, 10, 5, 9);
        for (int z = 286; z >= 258; z--) {
            int drop = Math.min(14, (286 - z) / 2);
            buildVaultSlice(10, -54 - drop, z, 6, 9, false);
            placeSimpleTread(10, -54 - drop, z, 5, BlockFace.NORTH);
        }
        // Join the civic spine only at the bottom landing. A parallel lower tunnel directly under
        // the return flight puts its roof through the stair's mid-flight headspace.
        corridorX(0, -68, 258, 0, 10, 5, 9);
    }

    private void buildCivicToLowerSwitchback() {
        corridorZ(-10, -68, 34, 42, 6, 9);
        for (int z = 34; z >= 6; z--) {
            int drop = Math.min(14, (34 - z) / 2);
            buildVaultSlice(-10, -68 - drop, z, 6, 9, false);
            placeSimpleTread(-10, -68 - drop, z, 5, BlockFace.NORTH);
        }
        corridorX(0, -82, 6, -10, 10, 5, 9);
        for (int z = 6; z <= 34; z++) {
            int drop = Math.min(14, (z - 6) / 2);
            buildVaultSlice(10, -82 - drop, z, 6, 9, false);
            placeSimpleTread(10, -82 - drop, z, 5, BlockFace.SOUTH);
        }
        // As above, connect at the completed lower landing instead of tunnelling underneath the
        // still-descending flight. The caller continues the central Lower Works spine from z=34.
        corridorX(0, -96, 34, 0, 10, 5, 9);
    }

    private void placeSimpleTread(int cx, int floor, int z, int half, BlockFace facing) {
        for (int x = cx - half; x <= cx + half; x++) {
            set(x, floor - 1, z, Material.POLISHED_BLACKSTONE_BRICKS);
            set(x, floor, z, Material.AIR);
            set(x, floor + 1, z, Material.AIR);
        }
    }

    private void dressRoom(DeepHoldV4Plan.Room room) {
        switch (room.id()) {
            case "orientation" -> dressOrientation(room);
            case "keeper_nave" -> dressKeeperNave(room);
            case "archive_nave" -> dressArchiveNave(room);
            case "puzzle_works" -> dressPuzzleWorks(room);
            case "lower_works", "lower_spine" -> dressLowerWorks(room);
            case "accepting" -> dressAccepting(room);
            case "unwriting", "release" -> dressCoda(room);
            default -> dressEvidenceRoom(room);
        }
    }

    private void dressOrientation(DeepHoldV4Plan.Room room) {
        for (int x : new int[]{-28, 0, 28}) {
            buildInteriorArch(x, room.floorY(), 116, 8, 10, BlockFace.SOUTH, Material.CHISELED_TUFF);
            floorInlayZ(x, room.floorY(), 110, 148, 1, Material.OXIDIZED_CUT_COPPER);
        }
        for (int i = -3; i <= 3; i++) {
            int x = i * 4;
            Material mark = i == 3 ? Material.LIGHT_GRAY_CONCRETE : Material.POLISHED_BASALT;
            set(x, room.floorY() - 1, 150, mark);
        }
    }

    private void dressKeeperNave(DeepHoldV4Plan.Room room) {
        for (int z = 170; z <= 242; z += 12) {
            for (int x : new int[]{-22, 22}) {
                if (reserved(room.id(), x, room.floorY(), z, 4)) continue;
                pillarLamp(x, room.floorY(), z, 6, Material.POLISHED_BASALT);
            }
        }
        floorInlayZ(0, room.floorY(), 164, 244, 2, Material.WAXED_OXIDIZED_COPPER);
        for (int z : new int[]{176, 206, 238}) {
            floorInlayX(room.floorY(), z, -28, 28, 1, Material.CHISELED_TUFF);
        }
    }

    private void dressArchiveNave(DeepHoldV4Plan.Room room) {
        for (int z = 112; z <= 290; z += 16) {
            for (int x : new int[]{-22, 22}) {
                if (!reserved(room.id(), x, room.floorY(), z, 4)) {
                    pillarLamp(x, room.floorY(), z, 5, Material.CUT_COPPER);
                }
            }
        }
        floorInlayZ(0, room.floorY(), 106, 296, 2, Material.OXIDIZED_CUT_COPPER);
        for (int z : new int[]{120, 224, 286}) {
            floorInlayX(room.floorY(), z, -28, 28, 1, Material.POLISHED_TUFF);
        }
    }

    private void dressEvidenceRoom(DeepHoldV4Plan.Room room) {
        // Fixtures own the center. Architecture identifies the place through its perimeter and
        // silhouette without scattering furniture into interaction radii.
        int floor = room.floorY();
        int cx = (room.minX() + room.maxX()) / 2;
        int cz = (room.minZ() + room.maxZ()) / 2;
        Material accent = accentMaterial(room);
        buildInteriorArch(room.minX() < 0 ? room.maxX() - 3 : room.minX() + 3,
                floor, cz, 5, 8, room.minX() < 0 ? BlockFace.EAST : BlockFace.WEST, accent);
        for (int z = room.minZ() + 6; z <= room.maxZ() - 6; z += 10) {
            int x = room.minX() < 0 ? room.minX() + 5 : room.maxX() - 5;
            if (!reserved(room.id(), x, floor, z, 4)) pillarLamp(x, floor, z, 4, accent);
        }
        floorInlayX(floor, cz, room.minX() + 4, room.maxX() - 4, 1, accent);
        if (room.id().contains("water") || room.id().contains("cistern")) {
            perimeterChannel(room, floor);
        }
    }

    private void dressPuzzleWorks(DeepHoldV4Plan.Room room) {
        for (int x = -52; x <= 52; x += 13) {
            if (!reserved(room.id(), x, room.floorY(), 52, 5)) {
                pillarLamp(x, room.floorY(), 52, 5, Material.EXPOSED_COPPER);
            }
        }
        floorInlayZ(0, room.floorY(), 44, 92, 2, Material.CUT_COPPER);
    }

    private void dressLowerWorks(DeepHoldV4Plan.Room room) {
        int cx = (room.minX() + room.maxX()) / 2;
        for (int z = room.minZ() + 8; z <= room.maxZ() - 8; z += 14) {
            for (int x : new int[]{room.minX() + 8, room.maxX() - 8}) {
                if (!reserved(room.id(), x, room.floorY(), z, 5)) {
                    pillarLamp(x, room.floorY(), z, 5, Material.CHISELED_POLISHED_BLACKSTONE);
                }
            }
        }
        floorInlayZ(cx, room.floorY(), room.minZ() + 4, room.maxZ() - 4, 2,
                Material.POLISHED_BLACKSTONE_BRICKS);
    }

    private void dressAccepting(DeepHoldV4Plan.Room room) {
        int floor = room.floorY();
        int cx = 0;
        int cz = 260;
        for (int radius : new int[]{12, 20, 30, 40}) {
            for (int step = 0; step < 64; step++) {
                double angle = (Math.PI * 2.0 * step) / 64.0;
                int x = cx + (int) Math.round(Math.cos(angle) * radius);
                int z = cz + (int) Math.round(Math.sin(angle) * radius);
                if (!reserved(room.id(), x, floor, z, 1)) set(x, floor - 1, z,
                        radius == 40 ? Material.CHISELED_TUFF : Material.POLISHED_BASALT);
            }
        }
    }

    private void dressCoda(DeepHoldV4Plan.Room room) {
        int cx = (room.minX() + room.maxX()) / 2;
        floorInlayZ(cx, room.floorY(), room.minZ() + 4, room.maxZ() - 4, 2,
                room.id().equals("unwriting") ? Material.SCULK : Material.CHISELED_TUFF);
        for (int z = room.minZ() + 10; z <= room.maxZ() - 8; z += 14) {
            for (int x : new int[]{room.minX() + 8, room.maxX() - 8}) {
                if (!reserved(room.id(), x, room.floorY(), z, 5)) {
                    pillarLamp(x, room.floorY(), z, 5, Material.POLISHED_BLACKSTONE);
                }
            }
        }
    }

    private void buildGatehouse(DeepHoldV4Plan.Gate gate) {
        int half = gate.halfAcross();
        int floor = gate.y();
        Material wall = gate.mainSequence() ? Material.REINFORCED_DEEPSLATE : Material.POLISHED_BLACKSTONE_BRICKS;
        Material accent = gate.mainSequence() ? Material.CHISELED_TUFF : Material.SCULK;
        for (int d = -3; d <= 3; d++) {
            for (int a = -half - 3; a <= half + 3; a++) {
                for (int y = floor - 2; y <= floor + gate.height() + 2; y++) {
                    boolean frame = Math.abs(a) > half - 5 || y >= floor + gate.height()
                            || y < floor;
                    int x = gate.acrossX() ? gate.x() + a : gate.x() + d;
                    int z = gate.acrossX() ? gate.z() + d : gate.z() + a;
                    if (frame) set(x, y, z, (Math.abs(a) == half || y == floor + gate.height()) ? accent : wall);
                    else if (y >= floor && y < floor + gate.height()) set(x, y, z, Material.AIR);
                }
            }
        }
        // The runtime stamps the actual sealed/open door volume after all traversal audits pass.
        floorInlayX(floor, gate.z(), gate.x() - Math.min(half, 8), gate.x() + Math.min(half, 8),
                1, accent);
    }

    private void finishSurfaceMouth() {
        // Re-seat natural-looking stone around the Mouth shoulders without hiding the entrance.
        for (int x = -22; x <= 22; x++) {
            for (int z = -6; z <= 16; z++) {
                // The Grand Stair owns the whole central throat through this landscaping band.
                // Restoring the shoulders over z=9..16 used to fill the first eight descending
                // treads with stone after they had passed their own construction checks.
                if (Math.abs(x) <= 12) continue;
                int crown = Math.max(1, 8 - (Math.abs(x) / 4) - Math.max(0, z) / 6);
                for (int y = -2; y <= crown; y++) {
                    Material material = y == crown ? Material.GRASS_BLOCK
                            : (y >= crown - 2 ? Material.DIRT : Material.STONE);
                    set(x, y, z, material);
                }
            }
        }
        for (int x : new int[]{-15, 15}) pillarLamp(x, 0, -1, 7, Material.POLISHED_DEEPSLATE);
    }

    private void buildVaultSlice(int cx, int floor, int z, int halfWidth, int clearHeight, boolean openRoof) {
        for (int x = cx - halfWidth - 3; x <= cx + halfWidth + 3; x++) {
            for (int y = floor - 3; y <= floor + clearHeight + 2; y++) {
                boolean foundation = y < floor;
                boolean wall = Math.abs(x - cx) > halfWidth;
                boolean roof = !openRoof && y >= floor + clearHeight;
                if (foundation) set(x, y, z, y == floor - 1 ? Material.DEEPSLATE_TILES : Material.DEEPSLATE);
                else if (wall || roof) set(x, y, z,
                        Math.floorMod(x + y + z, 17) == 0 ? Material.POLISHED_BASALT : Material.DEEPSLATE_BRICKS);
                else set(x, y, z, Material.AIR);
            }
        }
    }

    private void corridorZ(int cx, int floor, int z1, int z2, int halfWidth, int height) {
        int min = Math.min(z1, z2);
        int max = Math.max(z1, z2);
        for (int z = min; z <= max; z++) buildVaultSlice(cx, floor, z, halfWidth, height, false);
    }

    private void corridorX(int centerX, int floor, int centerZ, int x1, int x2, int halfWidth, int height) {
        int min = Math.min(x1, x2);
        int max = Math.max(x1, x2);
        for (int x = min; x <= max; x++) {
            for (int z = centerZ - halfWidth - 3; z <= centerZ + halfWidth + 3; z++) {
                for (int y = floor - 3; y <= floor + height + 2; y++) {
                    boolean foundation = y < floor;
                    boolean wall = Math.abs(z - centerZ) > halfWidth;
                    boolean roof = y >= floor + height;
                    if (foundation) set(x, y, z, y == floor - 1 ? Material.DEEPSLATE_TILES : Material.DEEPSLATE);
                    else if (wall || roof) set(x, y, z,
                            Math.floorMod(x + y + z, 19) == 0 ? Material.CHISELED_TUFF : Material.DEEPSLATE_BRICKS);
                    else set(x, y, z, Material.AIR);
                }
            }
        }
    }

    private void openIntersection(int cx, int floor, int cz, int halfX, int halfZ, int height) {
        for (int x = cx - halfX; x <= cx + halfX; x++) {
            for (int z = cz - halfZ; z <= cz + halfZ; z++) {
                set(x, floor - 1, z, Material.POLISHED_TUFF);
                for (int y = floor; y < floor + height; y++) set(x, y, z, Material.AIR);
            }
        }
    }

    private void buildFacadeArch(int cx, int floor, int z, int halfWidth, int height) {
        for (int x = cx - halfWidth - 3; x <= cx + halfWidth + 3; x++) {
            for (int y = floor; y <= floor + height; y++) {
                double nx = Math.abs(x - cx) / (double) halfWidth;
                double ny = (y - floor) / (double) height;
                boolean opening = nx * nx + ny * ny < 1.0 && y < floor + height - 1;
                if (!opening) set(x, y, z, Math.floorMod(x + y, 9) == 0
                        ? Material.CHISELED_TUFF : Material.POLISHED_DEEPSLATE);
                else set(x, y, z, Material.AIR);
            }
        }
    }

    private void buildInteriorArch(int cx, int floor, int cz, int halfWidth, int height,
                                   BlockFace direction, Material accent) {
        boolean acrossX = direction == BlockFace.NORTH || direction == BlockFace.SOUTH;
        for (int a = -halfWidth; a <= halfWidth; a++) {
            for (int y = 0; y <= height; y++) {
                boolean edge = Math.abs(a) >= halfWidth - 1 || y >= height - Math.max(0, halfWidth - Math.abs(a));
                if (!edge) continue;
                int x = acrossX ? cx + a : cx;
                int z = acrossX ? cz : cz + a;
                if (!reserved(null, x, floor + y, z, 1)) set(x, floor + y, z, accent);
            }
        }
    }

    private void pillarLamp(int x, int floor, int z, int height, Material accent) {
        for (int y = 0; y < height; y++) {
            set(x, floor + y, z, y == 0 || y == height - 1 ? accent : Material.DEEPSLATE_BRICK_WALL);
        }
        set(x, floor + height, z, Material.SOUL_LANTERN);
    }

    private void perimeterChannel(DeepHoldV4Plan.Room room, int floor) {
        for (int x = room.minX() + 5; x <= room.maxX() - 5; x++) {
            if (!reserved(room.id(), x, floor, room.minZ() + 5, 2)) set(x, floor - 1, room.minZ() + 5, Material.WATER);
            if (!reserved(room.id(), x, floor, room.maxZ() - 5, 2)) set(x, floor - 1, room.maxZ() - 5, Material.WATER);
        }
    }

    private void floorInlayZ(int cx, int floor, int z1, int z2, int halfWidth, Material material) {
        for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
            for (int x = cx - halfWidth; x <= cx + halfWidth; x++) {
                if (!reserved(null, x, floor, z, 1)) set(x, floor - 1, z, material);
            }
        }
    }

    private void floorInlayX(int floor, int z, int x1, int x2, int halfWidth, Material material) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                if (!reserved(null, x, floor, z + dz, 1)) set(x, floor - 1, z + dz, material);
            }
        }
    }

    private boolean reserved(String roomId, int x, int y, int z, int padding) {
        for (DeepHoldV4Plan.Fixture fixture : DeepHoldV4Plan.FIXTURES) {
            if (roomId != null && !roomId.equals(fixture.roomId())) continue;
            int horizontal = Math.max(3, Math.min(7, fixture.radius() / 2)) + padding;
            if (Math.abs(x - fixture.x()) <= horizontal && Math.abs(z - fixture.z()) <= horizontal
                    && y >= fixture.y() - 2 && y <= fixture.y() + Math.min(8, fixture.verticalRadius())) return true;
            if (Math.abs(x - fixture.standX()) <= 2 + padding
                    && Math.abs(z - fixture.standZ()) <= 2 + padding
                    && y >= fixture.standY() - 1 && y <= fixture.standY() + 4) return true;
        }
        return false;
    }

    private void set(int x, int y, int z, Material material) {
        operations.add(x, y, z, material);
    }

    private void message(Consumer<String> progress, String text) {
        if (progress != null) progress.accept(text);
    }
}
