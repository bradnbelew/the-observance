package com.observance.watcher.morrow.coldstorage;

import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Instance;
import com.observance.watcher.morrow.coldstorage.ColdStorageAuthority.Record;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact bounded M11 archive with five custody stations and a two-instance bridge. */
public final class ColdStorageManifest {
    public static final Bounds BOUNDS = new Bounds(-15, 15, 0, 8, -11, 11);
    private final Map<Cell, String> cells;
    private final Map<Record, Cell> recordConsoles = Map.of(
            Record.THEO_PERMISSION, new Cell(-8, 1, 7),
            Record.ROOKERY_ANCHOR_GRAPH, new Cell(-4, 1, 7),
            Record.IONA_SHUTDOWN, new Cell(0, 1, 7),
            Record.MORROW_SNAPSHOT_MANIFEST, new Cell(4, 1, 7),
            Record.CAPTIONED_VOICE_ASSEMBLY, new Cell(8, 1, 7));
    private final Map<Instance, Cell> hashSources = Map.of(
            Instance.AUDIT_SNAPSHOT, new Cell(-10, 1, -7),
            Instance.CURRENT_RECOVERY, new Cell(10, 1, -7));
    private final Map<Instance, Cell> bridgeConsoles = Map.of(
            Instance.AUDIT_SNAPSHOT, new Cell(-10, 1, -2),
            Instance.CURRENT_RECOVERY, new Cell(10, 1, -2));
    private final Map<String, Cell> stateLamps = Map.ofEntries(
            Map.entry("record_1", new Cell(-8, 2, 5)), Map.entry("record_2", new Cell(-4, 2, 5)),
            Map.entry("record_3", new Cell(0, 2, 5)), Map.entry("record_4", new Cell(4, 2, 5)),
            Map.entry("record_5", new Cell(8, 2, 5)), Map.entry("audit_bridge", new Cell(-5, 2, -1)),
            Map.entry("current_bridge", new Cell(5, 2, -1)), Map.entry("reconstruction", new Cell(-2, 2, 9)),
            Map.entry("access", new Cell(2, 2, 9)));
    private final Cell reconstructionConsole = new Cell(-2, 1, 9);
    private final Cell accessConsole = new Cell(2, 1, 9);
    private final Cell resetConsole = new Cell(0, 1, 10);
    private final Cell safeReturn = new Cell(0, 1, 8);
    private final String manifestSha256;

    public ColdStorageManifest() {
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
            if (z < 3 && x < -1) return ((x + z) & 1) == 0 ? "minecraft:blue_ice" : "minecraft:packed_ice";
            if (z < 3 && x > 1) return ((x + z) & 1) == 0 ? "minecraft:oxidized_cut_copper" : "minecraft:tuff_bricks";
            return ((x + z) & 1) == 0 ? "minecraft:polished_deepslate" : "minecraft:polished_tuff";
        }
        if (recordConsoles.containsValue(cell)) return "minecraft:lectern[facing=south,has_book=false,powered=false]";
        if (hashSources.containsValue(cell)) return "minecraft:chiseled_bookshelf";
        if (bridgeConsoles.containsValue(cell)) return "minecraft:lodestone";
        if (reconstructionConsole.equals(cell)) return "minecraft:crying_obsidian";
        if (accessConsole.equals(cell)) return "minecraft:respawn_anchor[charges=0]";
        if (resetConsole.equals(cell)) return "minecraft:target";
        if (stateLamps.containsValue(cell)) return "minecraft:copper_bulb[lit=false,powered=false]";
        if (y == 8 || Math.abs(x) == 15 || Math.abs(z) == 11) {
            if (z == 11 && x == 0 && y <= 2) return "minecraft:air";
            if (x < -1 && z < 3 && y == 4) return "minecraft:packed_ice";
            if (x > 1 && z < 3 && y == 4) return "minecraft:waxed_oxidized_cut_copper";
            return "minecraft:deepslate_bricks";
        }
        boolean divider = z < 3 && (x == -1 || x == 1) && y >= 1 && y <= 7;
        if (divider) {
            if (z == 2 && y <= 2) return "minecraft:air";
            return y >= 2 && y <= 5 ? "minecraft:tinted_glass" : "minecraft:reinforced_deepslate";
        }
        boolean auditBand = z == -8 && x >= -13 && x <= -3 && y == 1;
        if (auditBand) return "minecraft:blue_ice";
        boolean recoveryBand = z == -8 && x >= 3 && x <= 13 && y == 1;
        if (recoveryBand) return "minecraft:oxidized_cut_copper";
        boolean graphRail = z == 4 && x >= -10 && x <= 10 && y == 1;
        if (graphRail) return x % 4 == 0 ? "minecraft:copper_grate" : "minecraft:polished_deepslate";
        return "minecraft:air";
    }

    public Map<Cell, String> cells() { return cells; }
    public Map<Record, Cell> recordConsoles() { return recordConsoles; }
    public Map<Instance, Cell> hashSources() { return hashSources; }
    public Map<Instance, Cell> bridgeConsoles() { return bridgeConsoles; }
    public Map<String, Cell> stateLamps() { return stateLamps; }
    public Cell reconstructionConsole() { return reconstructionConsole; }
    public Cell accessConsole() { return accessConsole; }
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
    public record Bounds(int minimumX, int maximumX, int minimumY, int maximumY, int minimumZ, int maximumZ) { }
}
