package com.observance.watcher.morrow.room04;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact bounded block manifest for the first Recovery Room 04 revision. */
public final class RecoveryRoom04Manifest {
    public static final String AIR = "minecraft:air";
    public static final Bounds BOUNDS = new Bounds(-6, 6, -1, 6, -5, 5);
    public static final Cell SPAWN_CELL = new Cell(0, 0, -2);
    public static final Cell EXIT_CELL = new Cell(0, 0, -4);
    public static final Cell TERMINAL_CELL = new Cell(0, 0, 2);

    private final Map<Cell, String> cells;
    private final String manifestSha256;

    public RecoveryRoom04Manifest() {
        LinkedHashMap<Cell, String> authored = new LinkedHashMap<>();
        for (int y = BOUNDS.minimumY(); y <= BOUNDS.maximumY(); y++) {
            for (int z = BOUNDS.minimumZ(); z <= BOUNDS.maximumZ(); z++) {
                for (int x = BOUNDS.minimumX(); x <= BOUNDS.maximumX(); x++) {
                    Cell cell = new Cell(x, y, z);
                    authored.put(cell, blockAt(cell));
                }
            }
        }
        this.cells = Map.copyOf(authored);
        validate();
        this.manifestSha256 = hash(canonicalBytes(authored));
    }

    public Map<Cell, String> cells() {
        return cells;
    }

    public String expected(Cell cell) {
        String expected = cells.get(cell);
        if (expected == null) throw new IllegalArgumentException("cell is outside Recovery Room 04 bounds");
        return expected;
    }

    public String manifestSha256() {
        return manifestSha256;
    }

    public List<Cell> safeCells() {
        return List.of(SPAWN_CELL, EXIT_CELL);
    }

    private static String blockAt(Cell cell) {
        int x = cell.x();
        int y = cell.y();
        int z = cell.z();
        if (y == -1) {
            if ((z == -2 || z == 0) && Math.abs(x) <= 4) return "minecraft:waxed_copper_block";
            return "minecraft:smooth_stone";
        }
        if (y == 6) {
            if ((x == -3 || x == 3) && (z == -2 || z == 2)) return "minecraft:sea_lantern";
            return "minecraft:smooth_stone";
        }
        boolean boundary = x == BOUNDS.minimumX() || x == BOUNDS.maximumX()
                || z == BOUNDS.minimumZ() || z == BOUNDS.maximumZ();
        if (boundary) {
            // Three-wide, three-high physical exit aperture at the obvious approach side.
            if (z == BOUNDS.minimumZ() && Math.abs(x) <= 1 && y <= 2) return AIR;
            if (z == BOUNDS.maximumZ() && Math.abs(x) <= 2 && (y == 2 || y == 3)) {
                return "minecraft:tinted_glass";
            }
            if (y == 0 || y == 5) return "minecraft:polished_tuff";
            return "minecraft:tuff_bricks";
        }
        if (x == 0 && z == 2 && (y == 0 || y == 1)) return "minecraft:waxed_copper_block";
        if (y == 0 && z == 2 && Math.abs(x) == 1) return "minecraft:cut_copper";
        if (y == 0 && z == 3 && Math.abs(x) <= 2) return "minecraft:oxidized_cut_copper";
        return AIR;
    }

    private void validate() {
        if (cells.size() != BOUNDS.volume()) throw new IllegalStateException("room manifest is not full-volume");
        if (AIR.equals(expected(TERMINAL_CELL))) throw new IllegalStateException("room terminal is absent");
        for (Cell safe : safeCells()) {
            requireInBounds(safe);
            requireInBounds(safe.above());
            requireInBounds(safe.below());
            if (!AIR.equals(expected(safe)) || !AIR.equals(expected(safe.above()))) {
                throw new IllegalStateException("safe cell lacks two-block clearance: " + safe);
            }
            if (AIR.equals(expected(safe.below()))) {
                throw new IllegalStateException("safe cell lacks a solid floor: " + safe);
            }
        }
        for (int z = EXIT_CELL.z(); z <= SPAWN_CELL.z(); z++) {
            Cell route = new Cell(0, 0, z);
            if (!AIR.equals(expected(route)) || !AIR.equals(expected(route.above()))
                    || AIR.equals(expected(route.below()))) {
                throw new IllegalStateException("safe entry route is obstructed at " + route);
            }
        }
    }

    private static byte[] canonicalBytes(LinkedHashMap<Cell, String> source) {
        StringBuilder canonical = new StringBuilder(source.size() * 40);
        source.forEach((cell, data) -> canonical.append(cell.x()).append(',')
                .append(cell.y()).append(',').append(cell.z()).append('=')
                .append(data).append('\n'));
        return canonical.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void requireInBounds(Cell cell) {
        if (!BOUNDS.contains(cell)) throw new IllegalStateException("required room cell is out of bounds: " + cell);
    }

    public record Cell(int x, int y, int z) implements Comparable<Cell> {
        public Cell above() { return new Cell(x, y + 1, z); }
        public Cell below() { return new Cell(x, y - 1, z); }

        @Override
        public int compareTo(Cell other) {
            int byY = Integer.compare(y, other.y);
            if (byY != 0) return byY;
            int byZ = Integer.compare(z, other.z);
            return byZ != 0 ? byZ : Integer.compare(x, other.x);
        }
    }

    public record Bounds(
            int minimumX, int maximumX,
            int minimumY, int maximumY,
            int minimumZ, int maximumZ) {
        public Bounds {
            if (minimumX > maximumX || minimumY > maximumY || minimumZ > maximumZ) {
                throw new IllegalArgumentException("invalid room bounds");
            }
        }

        public boolean contains(Cell cell) {
            return cell.x() >= minimumX && cell.x() <= maximumX
                    && cell.y() >= minimumY && cell.y() <= maximumY
                    && cell.z() >= minimumZ && cell.z() <= maximumZ;
        }

        public int volume() {
            return Math.multiplyExact(
                    Math.multiplyExact(maximumX - minimumX + 1, maximumY - minimumY + 1),
                    maximumZ - minimumZ + 1);
        }
    }
}
