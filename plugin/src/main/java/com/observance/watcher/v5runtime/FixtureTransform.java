package com.observance.watcher.v5runtime;

import java.util.Objects;

/** Pure block-coordinate transform for fixture offsets ordered as right, up, front. */
public final class FixtureTransform {
    private FixtureTransform() {
    }

    public enum Cardinal {
        NORTH(0, -1),
        EAST(1, 0),
        SOUTH(0, 1),
        WEST(-1, 0);

        private final int x;
        private final int z;

        Cardinal(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public int x() {
            return x;
        }

        public int z() {
            return z;
        }

        public Cardinal clockwise() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Cardinal opposite() {
            return values()[(ordinal() + 2) % values().length];
        }
    }

    public record BlockPos(int x, int y, int z) {
        public BlockPos add(int deltaX, int deltaY, int deltaZ) {
            return new BlockPos(x + deltaX, y + deltaY, z + deltaZ);
        }
    }

    public record LocalOffset(int right, int up, int front) {
    }

    /**
     * Exact center of a wall-mounted item frame on the named face of a solid supporting block.
     * Keeping this geometry in the dependency-free transform prevents builders and runtime audits
     * from disagreeing about which side of the backing block owns the hanging entity.
     */
    public record FramePlane(double x, double y, double z) {
    }

    /**
     * Bukkit/Paper spawn coordinate for a hanging entity. Paper floors this location to the
     * adjacent air cell, then discovers the solid backing in a neighboring block. This is not the
     * same coordinate as the entity's rendered face plane.
     */
    public record FrameSpawnAnchor(double x, double y, double z) {
    }

    public static BlockPos toWorld(BlockPos origin, Cardinal expectedFront, LocalOffset offset) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(expectedFront, "expectedFront");
        Objects.requireNonNull(offset, "offset");
        int rightX = -expectedFront.z();
        int rightZ = expectedFront.x();
        int x = origin.x() + offset.right() * rightX + offset.front() * expectedFront.x();
        int z = origin.z() + offset.right() * rightZ + offset.front() * expectedFront.z();
        return new BlockPos(x, origin.y() + offset.up(), z);
    }

    public static LocalOffset toLocal(BlockPos origin, Cardinal expectedFront, BlockPos world) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(expectedFront, "expectedFront");
        Objects.requireNonNull(world, "world");
        int deltaX = world.x() - origin.x();
        int deltaZ = world.z() - origin.z();
        int rightX = -expectedFront.z();
        int rightZ = expectedFront.x();
        int right = deltaX * rightX + deltaZ * rightZ;
        int front = deltaX * expectedFront.x() + deltaZ * expectedFront.z();
        return new LocalOffset(right, world.y() - origin.y(), front);
    }

    public static FramePlane framePlane(BlockPos supportingBlock, Cardinal facing) {
        Objects.requireNonNull(supportingBlock, "supportingBlock");
        Objects.requireNonNull(facing, "facing");
        return new FramePlane(
                supportingBlock.x() + 0.5 + facing.x() * 0.5,
                supportingBlock.y() + 0.5,
                supportingBlock.z() + 0.5 + facing.z() * 0.5);
    }

    public static FrameSpawnAnchor frameSpawnAnchor(BlockPos supportingBlock, Cardinal facing) {
        Objects.requireNonNull(supportingBlock, "supportingBlock");
        Objects.requireNonNull(facing, "facing");
        return new FrameSpawnAnchor(
                supportingBlock.x() + 0.5 + facing.x(),
                supportingBlock.y() + 0.5,
                supportingBlock.z() + 0.5 + facing.z());
    }
}
