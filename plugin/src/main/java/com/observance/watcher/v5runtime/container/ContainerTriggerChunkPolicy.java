package com.observance.watcher.v5runtime.container;

import java.util.ArrayList;
import java.util.List;

/** Exact bounded chunk neighborhood retained for the two critical synthetic controls. */
public final class ContainerTriggerChunkPolicy {
    public static final int CONTROL_RADIUS = 3;

    private ContainerTriggerChunkPolicy() {
    }

    public static List<ChunkCoordinate> chunks(double x, double z) {
        int minX = floorBlock(x) - CONTROL_RADIUS;
        int maxX = floorBlock(x) + CONTROL_RADIUS;
        int minZ = floorBlock(z) - CONTROL_RADIUS;
        int maxZ = floorBlock(z) + CONTROL_RADIUS;
        List<ChunkCoordinate> result = new ArrayList<>(4);
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                result.add(new ChunkCoordinate(chunkX, chunkZ));
            }
        }
        return List.copyOf(result);
    }

    private static int floorBlock(double coordinate) {
        int truncated = (int) coordinate;
        return coordinate < truncated ? truncated - 1 : truncated;
    }

    public record ChunkCoordinate(int x, int z) {
    }
}
