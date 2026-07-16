package com.observance.watcher.m3runtime;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact solid/air Paper projection of design/m3/vertical-slice-v1.json. */
public final class PrivateSliceWorld {
    public static final int MIN_X = -18;
    public static final int MAX_X = 18;
    public static final int MIN_Y = -20;
    public static final int MAX_Y = 8;
    public static final int MIN_Z = -8;
    public static final int MAX_Z = 84;
    public static final int EXPECTED_CLOSED_REACHABLE = 927;
    public static final int EXPECTED_OPEN_REACHABLE = 961;

    private final World world;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final Map<Cell, Material> expected = new LinkedHashMap<>();

    public PrivateSliceWorld(World world, int originX, int originY, int originZ, boolean gateOpen) {
        if (world == null) throw new IllegalArgumentException("M3 world is required");
        this.world = world;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        compile(gateOpen);
    }

    public int apply() {
        int writes = 0;
        for (Map.Entry<Cell, Material> entry : expected.entrySet()) {
            Cell c = entry.getKey();
            Block block = block(c);
            Material material = entry.getValue();
            if (block.getType() != material) {
                block.setType(material, false);
                writes++;
            }
        }
        return writes;
    }

    public Audit audit() {
        List<String> findings = new ArrayList<>();
        for (Map.Entry<Cell, Material> entry : expected.entrySet()) {
            Material actual = block(entry.getKey()).getType();
            if (actual != entry.getValue() && findings.size() < 20) {
                findings.add(entry.getKey() + " expected=" + entry.getValue() + " actual=" + actual);
            }
        }
        return new Audit(expected.size(), findings, worldHash());
    }

    public void setGate(boolean open) {
        for (int x = -3; x <= 3; x++) {
            for (int y = -16; y <= -14; y++) {
                Cell cell = new Cell(x, y, 77);
                Material material = open ? Material.AIR : Material.IRON_BARS;
                expected.put(cell, material);
                block(cell).setType(material, false);
            }
        }
    }

    public boolean inside(org.bukkit.Location location) {
        if (location == null || location.getWorld() != world) return false;
        int x = location.getBlockX() - originX;
        int y = location.getBlockY() - originY;
        int z = location.getBlockZ() - originZ;
        return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y && z >= MIN_Z && z <= MAX_Z;
    }

    public String worldHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            expected.keySet().stream().sorted(Comparator.comparingInt(Cell::x)
                            .thenComparingInt(Cell::y).thenComparingInt(Cell::z))
                    .forEach(cell -> {
                        String row = cell.x + "," + cell.y + "," + cell.z + "="
                                + block(cell).getType().getKey() + "\n";
                        digest.update(row.getBytes(StandardCharsets.UTF_8));
                    });
            return hex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void compile(boolean gateOpen) {
        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                for (int z = MIN_Z; z <= MAX_Z; z++) expected.put(new Cell(x, y, z), Material.DEEPSLATE);
            }
        }
        room(-8, 8, 0, 7, 0, 12);
        room(-12, 12, -16, -9, 45, 60);
        room(-3, 3, -16, -10, 61, 76);
        room(5, 15, -16, -10, 63, 75);
        room(-3, 3, -16, -10, 78, 83);
        for (int z = 13; z <= 44; z++) {
            int floor = -Math.floorDiv(z - 13, 2);
            for (int x = -3; x <= 3; x++) column(x, floor, z, 4);
        }
        for (int[] transition : new int[][]{{12,0},{13,0},{44,-15},{45,-16},{60,-16},
                {61,-16},{76,-16},{77,-16},{78,-16}}) {
            for (int x = -3; x <= 3; x++) door(x, transition[1], transition[0], 4);
        }
        for (int[] cell : new int[][]{{4,-16,68},{4,-16,69},{4,-16,70},{5,-16,68},
                {5,-16,69},{5,-16,70}}) door(cell[0], cell[1], cell[2], 3);
        for (int[] cell : new int[][]{{9,-16,61},{9,-16,62},{10,-16,61},{10,-16,62}})
            door(cell[0], cell[1], cell[2], 3);
        route(new int[][]{{9,-16,56},{9,-16,60},{9,-16,62},{9,-16,64},{12,-16,64}}, 2);

        furniture(Material.DARK_OAK_PLANKS, new int[][]{{8,-16,65},{9,-16,65},{10,-16,65},
                {8,-16,66},{9,-16,66},{10,-16,66},{8,-16,72},{9,-16,72},{10,-16,72},
                {8,-16,73},{9,-16,73},{10,-16,73}});
        furniture(Material.POLISHED_ANDESITE, new int[][]{{12,-16,68},{13,-16,68},{12,-16,70},
                {13,-16,70},{-4,-16,56},{-3,-16,56},{-2,-16,56},{2,-16,56},{3,-16,56},{4,-16,56}});
        furniture(Material.BARREL, new int[][]{{14,-16,64},{14,-16,65},{14,-16,66},
                {14,-16,72},{14,-16,73},{14,-16,74}});
        furniture(Material.WATER, new int[][]{{-1,-17,51},{0,-17,51},{1,-17,51},
                {-1,-17,52},{0,-17,52},{1,-17,52}});
        for (int x = -3; x <= 3; x++) for (int y = -16; y <= -14; y++)
            expected.put(new Cell(x, y, 77), gateOpen ? Material.AIR : Material.IRON_BARS);
    }

    private void room(int minX, int maxX, int floor, int ceiling, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            expected.put(new Cell(x, floor - 1, z), Material.STONE_BRICKS);
            boolean perimeter = x == minX || x == maxX || z == minZ || z == maxZ;
            for (int y = floor; y <= ceiling; y++)
                expected.put(new Cell(x, y, z), perimeter || y == ceiling ? Material.STONE_BRICKS : Material.AIR);
        }
    }

    private void column(int x, int floor, int z, int headroom) {
        expected.put(new Cell(x, floor - 1, z), Material.STONE_BRICKS);
        for (int y = floor; y < floor + headroom; y++) expected.put(new Cell(x, y, z), Material.AIR);
    }

    private void door(int x, int floor, int z, int height) {
        expected.put(new Cell(x, floor - 1, z), Material.STONE_BRICKS);
        for (int y = floor; y < floor + height; y++) expected.put(new Cell(x, y, z), Material.AIR);
    }

    private void route(int[][] points, int width) {
        int radius = Math.max(0, (width - 1) / 2);
        for (int p = 0; p + 1 < points.length; p++) {
            int[] a = points[p];
            int[] b = points[p + 1];
            int distance = Math.max(Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])), Math.abs(b[2] - a[2]));
            for (int step = 0; step <= distance; step++) {
                double ratio = distance == 0 ? 0.0 : (double) step / distance;
                int x = (int) Math.round(a[0] + (b[0] - a[0]) * ratio);
                int y = (int) Math.round(a[1] + (b[1] - a[1]) * ratio);
                int z = (int) Math.round(a[2] + (b[2] - a[2]) * ratio);
                for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++)
                    column(x + dx, y, z + dz, 4);
            }
        }
    }

    private void furniture(Material material, int[][] cells) {
        for (int[] c : cells) expected.put(new Cell(c[0], c[1], c[2]), material);
    }

    private Block block(Cell c) {
        return world.getBlockAt(originX + c.x, originY + c.y, originZ + c.z);
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) out.append(String.format("%02x", value & 0xff));
        return out.toString();
    }

    public record Audit(int cellsChecked, List<String> findings, String worldHash) {
        public boolean pass() { return findings.isEmpty(); }
    }

    private record Cell(int x, int y, int z) { }
}
