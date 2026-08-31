package com.observance.watcher.morrow.continuity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact bounded M09 safe lobby with volunteer, echo, observation, identity, and reset controls. */
public final class AccountContinuityManifest {
    public static final Bounds BOUNDS = new Bounds(-10, 10, 0, 7, -7, 7);
    private final Map<Cell, String> cells;
    private final Map<String, Cell> stateLamps = Map.of(
            "armed", new Cell(-3, 2, 5), "disconnected", new Cell(-1, 2, 5),
            "continued", new Cell(1, 2, 5), "authenticated", new Cell(3, 2, 5));
    private final Cell secretConsole = new Cell(-7, 1, 5);
    private final Cell armConsole = new Cell(-3, 1, 5);
    private final Cell observeConsole = new Cell(3, 1, 5);
    private final Cell identityConsole = new Cell(7, 1, 5);
    private final Cell resetConsole = new Cell(0, 1, 5);
    private final Cell echoDais = new Cell(0, 1, -3);
    private final Cell returnCell = new Cell(0, 1, 2);
    private final String manifestSha256;

    public AccountContinuityManifest() {
        LinkedHashMap<Cell, String> authored = new LinkedHashMap<>();
        for (int y = BOUNDS.minimumY(); y <= BOUNDS.maximumY(); y++)
            for (int z = BOUNDS.minimumZ(); z <= BOUNDS.maximumZ(); z++)
                for (int x = BOUNDS.minimumX(); x <= BOUNDS.maximumX(); x++)
                    authored.put(new Cell(x, y, z), block(x, y, z));
        cells = Map.copyOf(authored); manifestSha256 = hash(authored);
    }
    private String block(int x, int y, int z) {
        Cell cell = new Cell(x, y, z);
        if (y == 0) return ((x + z) & 1) == 0 ? "minecraft:polished_tuff" : "minecraft:tuff_bricks";
        if (y == 7 || Math.abs(x) == 10 || Math.abs(z) == 7) {
            if (z == 7 && x == 0 && y <= 2) return "minecraft:air";
            return y == 3 ? "minecraft:waxed_cut_copper" : "minecraft:deepslate_bricks";
        }
        if (secretConsole.equals(cell)) return "minecraft:chiseled_bookshelf";
        if (armConsole.equals(cell)) return "minecraft:copper_block";
        if (observeConsole.equals(cell)) return "minecraft:lodestone";
        if (identityConsole.equals(cell)) return "minecraft:chiseled_copper";
        if (resetConsole.equals(cell)) return "minecraft:target";
        if (stateLamps.containsValue(cell)) return "minecraft:copper_bulb[lit=false,powered=false]";
        if ((x == -1 || x == 1) && y == 1 && z == 5) return "minecraft:cut_copper";
        if (echoDais.equals(cell)) return "minecraft:crying_obsidian";
        boolean echoWall = Math.abs(x) <= 2 && z >= -5 && z <= -1 && y >= 1 && y <= 4
                && (Math.abs(x) == 2 || z == -5 || z == -1 || y == 4);
        if (echoWall) {
            if (z == -1 && x == 0 && y <= 2) return "minecraft:air";
            return "minecraft:tinted_glass";
        }
        if (y == 1 && z == -6 && Math.abs(x) <= 4) return "minecraft:oxidized_cut_copper";
        return "minecraft:air";
    }
    public Map<Cell, String> cells() { return cells; }
    public Map<String, Cell> stateLamps() { return stateLamps; }
    public Cell secretConsole() { return secretConsole; }
    public Cell armConsole() { return armConsole; }
    public Cell observeConsole() { return observeConsole; }
    public Cell identityConsole() { return identityConsole; }
    public Cell resetConsole() { return resetConsole; }
    public Cell echoDais() { return echoDais; }
    public Cell returnCell() { return returnCell; }
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
