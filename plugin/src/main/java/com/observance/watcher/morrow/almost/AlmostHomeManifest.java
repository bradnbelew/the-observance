package com.observance.watcher.morrow.almost;

import com.observance.watcher.morrow.almost.AlmostHomeAuthority.EvidenceId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact M08 gallery containing the preserved perfect house and four-source audit stations. */
public final class AlmostHomeManifest {
    public static final Bounds BOUNDS = new Bounds(-12, 12, 0, 10, -9, 9);
    private final Map<Cell, String> cells;
    private final Map<EvidenceId, Cell> terminals;
    private final Map<EvidenceId, Cell> chronologyLamps;
    private final Cell cipherConsole = new Cell(0, 1, 7);
    private final Cell provenanceConsole = new Cell(7, 1, 7);
    private final Cell continuityConsole = new Cell(-7, 1, 7);
    private final Cell provenanceLamp = new Cell(7, 2, 7);
    private final Cell continuityLamp = new Cell(-7, 2, 7);
    private final String manifestSha256;

    public AlmostHomeManifest() {
        EnumMap<EvidenceId, Cell> authoredTerminals = new EnumMap<>(EvidenceId.class);
        authoredTerminals.put(EvidenceId.FINCH_PLAN_POST, new Cell(-9, 1, -5));
        authoredTerminals.put(EvidenceId.DATED_SCREENSHOT, new Cell(-9, 1, 5));
        authoredTerminals.put(EvidenceId.CONSTRUCTION_INVENTORY, new Cell(9, 1, 5));
        authoredTerminals.put(EvidenceId.PERFECT_CURRENT_HOUSE, new Cell(9, 1, -5));
        EnumMap<EvidenceId, Cell> lamps = new EnumMap<>(EvidenceId.class);
        lamps.put(EvidenceId.FINCH_PLAN_POST, new Cell(-3, 2, 7));
        lamps.put(EvidenceId.DATED_SCREENSHOT, new Cell(-1, 2, 7));
        lamps.put(EvidenceId.CONSTRUCTION_INVENTORY, new Cell(1, 2, 7));
        lamps.put(EvidenceId.PERFECT_CURRENT_HOUSE, new Cell(3, 2, 7));
        terminals = Map.copyOf(authoredTerminals);
        chronologyLamps = Map.copyOf(lamps);

        LinkedHashMap<Cell, String> authored = new LinkedHashMap<>();
        for (int y = BOUNDS.minimumY(); y <= BOUNDS.maximumY(); y++) {
            for (int z = BOUNDS.minimumZ(); z <= BOUNDS.maximumZ(); z++) {
                for (int x = BOUNDS.minimumX(); x <= BOUNDS.maximumX(); x++) {
                    authored.put(new Cell(x, y, z), block(x, y, z));
                }
            }
        }
        cells = Map.copyOf(authored);
        manifestSha256 = hash(authored);
    }

    private String block(int x, int y, int z) {
        if (y == 0) return ((x + z) & 1) == 0 ? "minecraft:polished_tuff" : "minecraft:tuff_bricks";
        if (y == 10 || Math.abs(x) == 12 || Math.abs(z) == 9) {
            if (z == 9 && x == 0 && y <= 2) return "minecraft:air";
            return y == 4 ? "minecraft:waxed_cut_copper" : "minecraft:deepslate_bricks";
        }
        for (Map.Entry<EvidenceId, Cell> entry : terminals.entrySet()) {
            if (entry.getValue().equals(new Cell(x, y, z))) return switch (entry.getKey()) {
                case FINCH_PLAN_POST -> "minecraft:chiseled_bookshelf";
                case DATED_SCREENSHOT -> "minecraft:lodestone";
                case CONSTRUCTION_INVENTORY -> "minecraft:barrel[facing=up,open=false]";
                case PERFECT_CURRENT_HOUSE -> "minecraft:copper_block";
            };
        }
        if (chronologyLamps.containsValue(new Cell(x, y, z))
                || provenanceLamp.equals(new Cell(x, y, z)) || continuityLamp.equals(new Cell(x, y, z)))
            return "minecraft:copper_bulb[lit=false,powered=false]";
        if ((x == -3 || x == -1 || x == 1 || x == 3) && y == 1 && z == 7)
            return "minecraft:cut_copper";
        if (cipherConsole.equals(new Cell(x, y, z))) return "minecraft:lectern[has_book=false,powered=false]";
        if (provenanceConsole.equals(new Cell(x, y, z))) return "minecraft:chiseled_copper";
        if (continuityConsole.equals(new Cell(x, y, z))) return "minecraft:oxidized_chiseled_copper";

        boolean house = Math.abs(x) <= 5 && Math.abs(z) <= 4;
        if (house && y == 1) return "minecraft:polished_deepslate";
        if (x == 4 && z == 3 && y >= 5 && y <= 8) return "minecraft:waxed_copper_block";
        if (house && y >= 2 && y <= 4 && (Math.abs(x) == 5 || Math.abs(z) == 4)) {
            if (z == 4 && x == 0 && y <= 3) return "minecraft:air";
            if (y == 3 && ((Math.abs(x) == 5 && Math.abs(z) <= 1)
                    || (Math.abs(z) == 4 && Math.abs(x) == 3))) return "minecraft:glass_pane";
            if (x == 5 && y >= 2 && y <= 4 && Math.abs(z) <= 1) return "minecraft:cartography_table";
            return "minecraft:stripped_spruce_wood[axis=y]";
        }
        if (house && y == 4 && Math.abs(x) <= 4 && z <= 0 && z >= -3) return "minecraft:spruce_planks";
        if (house && y == 5) return "minecraft:glass";
        if (y == 6 && Math.abs(x) <= 4 && Math.abs(z) <= 3) return "minecraft:tinted_glass";
        return "minecraft:air";
    }

    public Map<Cell, String> cells() { return cells; }
    public Map<EvidenceId, Cell> terminals() { return terminals; }
    public Map<EvidenceId, Cell> chronologyLamps() { return chronologyLamps; }
    public Cell cipherConsole() { return cipherConsole; }
    public Cell provenanceConsole() { return provenanceConsole; }
    public Cell continuityConsole() { return continuityConsole; }
    public Cell provenanceLamp() { return provenanceLamp; }
    public Cell continuityLamp() { return continuityLamp; }
    public String manifestSha256() { return manifestSha256; }

    private static String hash(Map<Cell, String> values) {
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<Cell, String> entry : values.entrySet()) {
            Cell cell = entry.getKey();
            canonical.append(cell.x()).append(',').append(cell.y()).append(',').append(cell.z())
                    .append('=').append(entry.getValue()).append('\n');
        }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public record Cell(int x, int y, int z) { }
    public record Bounds(int minimumX, int maximumX, int minimumY, int maximumY,
                         int minimumZ, int maximumZ) { }
}
