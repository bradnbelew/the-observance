package com.observance.watcher.morrow.maintenance;

import com.observance.watcher.morrow.maintenance.MaintenanceWindowAuthority.Side;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact M10 split-reality chamber with two private rooms and one shared hopper channel. */
public final class MaintenanceWindowManifest {
    public static final Bounds BOUNDS = new Bounds(-15, 15, 0, 8, -9, 9);
    private final Map<Cell, String> cells;
    private final Map<Side, Cell> instructionConsoles = Map.of(
            Side.WEST, new Cell(-11, 1, -5), Side.EAST, new Cell(11, 1, -5));
    private final Map<Side, Cell> pulseConsoles = Map.of(
            Side.WEST, new Cell(-11, 1, -2), Side.EAST, new Cell(11, 1, -2));
    private final Map<Side, Cell> tokenConsoles = Map.of(
            Side.WEST, new Cell(-11, 1, 1), Side.EAST, new Cell(11, 1, 1));
    private final Map<Side, Cell> hopperEndpoints = Map.of(
            Side.WEST, new Cell(-3, 1, 0), Side.EAST, new Cell(3, 1, 0));
    private final Map<String, Cell> stateLamps = Map.of(
            "west_witness", new Cell(-8, 2, 4), "west_transfer", new Cell(-5, 2, 4),
            "east_witness", new Cell(5, 2, 4), "east_transfer", new Cell(8, 2, 4),
            "active", new Cell(-2, 2, 6), "retained", new Cell(2, 2, 6));
    private final Cell sharedChannel = new Cell(0, 1, 0);
    private final Cell startConsole = new Cell(-2, 1, 6);
    private final Cell retainConsole = new Cell(2, 1, 6);
    private final Cell resetConsole = new Cell(0, 1, 7);
    private final Cell safeReturn = new Cell(0, 1, 5);
    private final String manifestSha256;

    public MaintenanceWindowManifest() {
        LinkedHashMap<Cell, String> authored = new LinkedHashMap<>();
        for (int y = BOUNDS.minimumY(); y <= BOUNDS.maximumY(); y++)
            for (int z = BOUNDS.minimumZ(); z <= BOUNDS.maximumZ(); z++)
                for (int x = BOUNDS.minimumX(); x <= BOUNDS.maximumX(); x++)
                    authored.put(new Cell(x, y, z), block(x, y, z));
        cells = Map.copyOf(authored); manifestSha256 = hash(authored);
    }

    private String block(int x, int y, int z) {
        Cell cell = new Cell(x, y, z);
        if (y == 0) {
            if (z <= 4 && x <= -2) return ((x + z) & 1) == 0 ? "minecraft:dark_prismarine" : "minecraft:prismarine_bricks";
            if (z <= 4 && x >= 2) return ((x + z) & 1) == 0 ? "minecraft:waxed_cut_copper" : "minecraft:tuff_bricks";
            return ((x + z) & 1) == 0 ? "minecraft:polished_deepslate" : "minecraft:polished_tuff";
        }
        if (instructionConsoles.containsValue(cell)) return "minecraft:chiseled_bookshelf";
        if (pulseConsoles.containsValue(cell)) return "minecraft:note_block";
        if (tokenConsoles.containsValue(cell)) return "minecraft:crafter";
        if (hopperEndpoints.get(Side.WEST).equals(cell)) return "minecraft:hopper[facing=east,enabled=true]";
        if (hopperEndpoints.get(Side.EAST).equals(cell)) return "minecraft:hopper[facing=west,enabled=true]";
        if (sharedChannel.equals(cell)) return "minecraft:barrel[facing=up,open=false]";
        if (startConsole.equals(cell)) return "minecraft:respawn_anchor[charges=0]";
        if (retainConsole.equals(cell)) return "minecraft:lodestone";
        if (resetConsole.equals(cell)) return "minecraft:target";
        if (stateLamps.containsValue(cell)) return "minecraft:copper_bulb[lit=false,powered=false]";
        if (y == 8 || Math.abs(x) == 15 || Math.abs(z) == 9) {
            if (z == 9 && x == 0 && y <= 2) return "minecraft:air";
            if (y == 4 && z <= 4 && x < 0) return "minecraft:packed_ice";
            if (y == 4 && z <= 4 && x > 0) return "minecraft:waxed_oxidized_cut_copper";
            return "minecraft:deepslate_bricks";
        }
        boolean divider = z <= 4 && (x == -1 || x == 1) && y >= 1 && y <= 7;
        if (divider) {
            if (z == 4 && y <= 2) return "minecraft:air";
            return y >= 2 && y <= 5 ? "minecraft:tinted_glass" : "minecraft:reinforced_deepslate";
        }
        boolean rearWestBand = z == -7 && x >= -13 && x <= -3 && y == 1;
        if (rearWestBand) return "minecraft:blue_ice";
        boolean rearEastBand = z == -7 && x >= 3 && x <= 13 && y == 1;
        if (rearEastBand) return "minecraft:oxidized_cut_copper";
        boolean channel = z == 0 && x >= -2 && x <= 2 && y == 1;
        if (channel) return x == 0 ? "minecraft:barrel[facing=up,open=false]" : "minecraft:waxed_copper_grate";
        return "minecraft:air";
    }

    public Map<Cell, String> cells() { return cells; }
    public Map<Side, Cell> instructionConsoles() { return instructionConsoles; }
    public Map<Side, Cell> pulseConsoles() { return pulseConsoles; }
    public Map<Side, Cell> tokenConsoles() { return tokenConsoles; }
    public Map<Side, Cell> hopperEndpoints() { return hopperEndpoints; }
    public Map<String, Cell> stateLamps() { return stateLamps; }
    public Cell sharedChannel() { return sharedChannel; }
    public Cell startConsole() { return startConsole; }
    public Cell retainConsole() { return retainConsole; }
    public Cell resetConsole() { return resetConsole; }
    public Cell safeReturn() { return safeReturn; }
    public String manifestSha256() { return manifestSha256; }

    private static String hash(Map<Cell, String> values) {
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<Cell, String> entry : values.entrySet()) {
            Cell cell = entry.getKey(); canonical.append(cell.x()).append(',').append(cell.y()).append(',')
                    .append(cell.z()).append('=').append(entry.getValue()).append('\n');
        }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public record Cell(int x, int y, int z) { }
    public record Bounds(int minimumX, int maximumX, int minimumY, int maximumY,
                         int minimumZ, int maximumZ) { }
}
