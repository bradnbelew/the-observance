package com.observance.watcher.morrow.versionrooms;

import com.observance.watcher.morrow.versionrooms.VersionRoomsAuthority.RoomVersion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact block manifest for M05's three asymmetric but safely traversable version rooms. */
public final class VersionRoomsManifest {
    public static final Bounds BOUNDS = new Bounds(-11, 11, -1, 5, -5, 5);
    private static final int[] CENTERS = {-8, 0, 8};
    private final Map<Cell, String> cells;
    private final Map<RoomVersion, Cell> consoles;
    private final Map<RoomVersion, Cell> lamps;
    private final String manifestSha256;

    public VersionRoomsManifest() {
        LinkedHashMap<Cell, String> authored = new LinkedHashMap<>();
        EnumMap<RoomVersion, Cell> consoleCells = new EnumMap<>(RoomVersion.class);
        EnumMap<RoomVersion, Cell> lampCells = new EnumMap<>(RoomVersion.class);
        RoomVersion[] versions = RoomVersion.values();
        for (int room = 0; room < versions.length; room++) {
            RoomVersion version = versions[room];
            int center = CENTERS[room];
            for (int y = -1; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        Cell cell = new Cell(center + dx, y, z);
                        authored.put(cell, blockAt(version, dx, y, z));
                    }
                }
            }
            consoleCells.put(version, new Cell(center, 0, 2));
            lampCells.put(version, new Cell(center, 1, 2));
        }
        this.cells = Map.copyOf(authored);
        this.consoles = Map.copyOf(consoleCells);
        this.lamps = Map.copyOf(lampCells);
        validate();
        this.manifestSha256 = hash(authored);
    }

    public Map<Cell, String> cells() { return cells; }
    public Map<RoomVersion, Cell> consoles() { return consoles; }
    public Map<RoomVersion, Cell> lamps() { return lamps; }
    public String manifestSha256() { return manifestSha256; }

    private static String blockAt(RoomVersion version, int x, int y, int z) {
        if (y == -1) return switch (version) {
            case DAMAGED -> "minecraft:cracked_deepslate_tiles";
            case COMPLETED -> "minecraft:polished_tuff";
            case SCAFFOLD -> "minecraft:oxidized_cut_copper";
        };
        if (y == 5) return version == RoomVersion.SCAFFOLD
                ? "minecraft:copper_grate" : "minecraft:smooth_stone";
        boolean boundary = Math.abs(x) == 3 || Math.abs(z) == 5;
        if (boundary) {
            if (z == -5 && Math.abs(x) <= 1 && y <= 2) return "minecraft:air";
            if (version == RoomVersion.DAMAGED && x == -3 && z == 1 && (y == 2 || y == 3)) {
                return "minecraft:air";
            }
            return switch (version) {
                case DAMAGED -> "minecraft:cracked_deepslate_bricks";
                case COMPLETED -> "minecraft:waxed_cut_copper";
                case SCAFFOLD -> (y == 1 || y == 3)
                        ? "minecraft:copper_grate" : "minecraft:scaffolding";
            };
        }
        if (x == 0 && z == 2 && y == 0) return "minecraft:chiseled_copper";
        if (x == 0 && z == 2 && y == 1) return "minecraft:copper_bulb[lit=false,powered=false]";
        if (y == 0 && z == 3 && Math.abs(x) <= 1) return switch (version) {
            case DAMAGED -> "minecraft:weathered_copper";
            case COMPLETED -> "minecraft:waxed_copper_block";
            case SCAFFOLD -> "minecraft:exposed_cut_copper";
        };
        return "minecraft:air";
    }

    private void validate() {
        if (cells.size() != 3 * 7 * 7 * 11) throw new IllegalStateException("M05 manifest volume drifted");
        for (RoomVersion version : RoomVersion.values()) {
            Cell console = consoles.get(version);
            Cell lamp = lamps.get(version);
            if (!"minecraft:chiseled_copper".equals(cells.get(console))
                    || cells.get(lamp) == null || !cells.get(lamp).startsWith("minecraft:copper_bulb")) {
                throw new IllegalStateException("M05 room lacks its console or signal lamp: " + version);
            }
            Cell entrance = new Cell(console.x(), 0, -5);
            if (!"minecraft:air".equals(cells.get(entrance))
                    || !"minecraft:air".equals(cells.get(entrance.above()))) {
                throw new IllegalStateException("M05 room lacks a safe entrance: " + version);
            }
        }
    }

    private static String hash(Map<Cell, String> authored) {
        StringBuilder canonical = new StringBuilder();
        authored.forEach((cell, block) -> canonical.append(cell.x()).append(',').append(cell.y())
                .append(',').append(cell.z()).append('=').append(block).append('\n'));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record Cell(int x, int y, int z) { public Cell above() { return new Cell(x, y + 1, z); } }
    public record Bounds(int minimumX, int maximumX, int minimumY, int maximumY,
                         int minimumZ, int maximumZ) { }
}
