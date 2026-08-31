package com.observance.watcher.morrow.consensus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact vanilla-fallback-safe M06 vote chamber: six equal terminals and one audit console. */
public final class ConsensusAuditManifest {
    public static final Bounds BOUNDS = new Bounds(-8, 8, 0, 6, -5, 5);
    private final Map<Cell, String> cells;
    private final Map<String, Cell> terminals;
    private final Map<String, Cell> lamps;
    private final Cell auditConsole = new Cell(0, 1, 3);
    private final Cell auditLamp = new Cell(0, 2, 3);
    private final String manifestSha256;

    public ConsensusAuditManifest() {
        LinkedHashMap<Cell, String> built = new LinkedHashMap<>();
        for (int y = BOUNDS.minimumY(); y <= BOUNDS.maximumY(); y++) {
            for (int z = BOUNDS.minimumZ(); z <= BOUNDS.maximumZ(); z++) {
                for (int x = BOUNDS.minimumX(); x <= BOUNDS.maximumX(); x++) {
                    built.put(new Cell(x, y, z), block(x, y, z));
                }
            }
        }
        LinkedHashMap<String, Cell> terminalsBuilt = new LinkedHashMap<>();
        LinkedHashMap<String, Cell> lampsBuilt = new LinkedHashMap<>();
        List<Integer> xs = List.of(-6, -4, -2, 2, 4, 6);
        for (int index = 0; index < ConsensusAuditAuthority.VOTES.size(); index++) {
            String id = ConsensusAuditAuthority.VOTES.get(index).id();
            terminalsBuilt.put(id, new Cell(xs.get(index), 1, -3));
            lampsBuilt.put(id, new Cell(xs.get(index), 2, -3));
        }
        cells = Map.copyOf(built);
        terminals = Map.copyOf(terminalsBuilt);
        lamps = Map.copyOf(lampsBuilt);
        for (Cell terminal : terminals.values()) {
            if (!"minecraft:chiseled_copper".equals(cells.get(terminal))) {
                throw new IllegalStateException("M06 terminal missing from manifest");
            }
        }
        manifestSha256 = hash(cells);
    }

    private static String block(int x, int y, int z) {
        if (y == 0) return ((x + z) & 1) == 0 ? "minecraft:polished_deepslate" : "minecraft:deepslate_tiles";
        if (y == 6 || Math.abs(x) == 8 || Math.abs(z) == 5) {
            if (z == 5 && x == 0 && y <= 2) return "minecraft:air";
            return (y == 3 && (Math.abs(x) == 8 || Math.abs(z) == 5))
                    ? "minecraft:oxidized_cut_copper" : "minecraft:tuff_bricks";
        }
        if (z == -3 && y == 1 && List.of(-6, -4, -2, 2, 4, 6).contains(x)) {
            return "minecraft:chiseled_copper";
        }
        if (z == -3 && y == 2 && List.of(-6, -4, -2, 2, 4, 6).contains(x)) {
            return "minecraft:copper_bulb[lit=false,powered=false]";
        }
        if (x == 0 && z == 3 && y == 1) return "minecraft:chiseled_copper";
        if (x == 0 && z == 3 && y == 2) return "minecraft:copper_bulb[lit=false,powered=false]";
        if (y == 1 && z == -2 && Math.abs(x) <= 6) return "minecraft:waxed_cut_copper";
        if (y == 1 && z == 1 && Math.abs(x) <= 2) return "minecraft:oxidized_copper";
        return "minecraft:air";
    }

    public Map<Cell, String> cells() { return cells; }
    public Map<String, Cell> terminals() { return terminals; }
    public Map<String, Cell> lamps() { return lamps; }
    public Cell auditConsole() { return auditConsole; }
    public Cell auditLamp() { return auditLamp; }
    public String manifestSha256() { return manifestSha256; }

    private static String hash(Map<Cell, String> cells) {
        StringBuilder value = new StringBuilder();
        for (Map.Entry<Cell, String> entry : cells.entrySet()) {
            Cell cell = entry.getKey();
            value.append(cell.x()).append(',').append(cell.y()).append(',').append(cell.z())
                    .append('=').append(entry.getValue()).append('\n');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record Cell(int x, int y, int z) { }
    public record Bounds(int minimumX, int maximumX, int minimumY, int maximumY,
                         int minimumZ, int maximumZ) { }
}
