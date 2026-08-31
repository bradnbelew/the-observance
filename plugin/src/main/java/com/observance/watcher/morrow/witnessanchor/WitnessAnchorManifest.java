package com.observance.watcher.morrow.witnessanchor;

import com.observance.watcher.morrow.witnessanchor.WitnessAnchorAuthority.CellId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact M07 chamber with side-by-side original and reconstruction six-cell grids. */
public final class WitnessAnchorManifest {
    public static final Bounds BOUNDS = new Bounds(-8, 8, 0, 6, -6, 6);
    private final Map<Cell, String> cells;
    private final Map<CellId, Cell> originalBases;
    private final Map<CellId, Cell> originalCells;
    private final Map<CellId, Cell> reconstructionCells;
    private final Cell commitConsole = new Cell(0, 1, 4);
    private final Cell commitLamp = new Cell(0, 2, 4);
    private final String manifestSha256;

    public WitnessAnchorManifest() {
        LinkedHashMap<Cell, String> authored = new LinkedHashMap<>();
        for (int y = BOUNDS.minimumY(); y <= BOUNDS.maximumY(); y++) {
            for (int z = BOUNDS.minimumZ(); z <= BOUNDS.maximumZ(); z++) {
                for (int x = BOUNDS.minimumX(); x <= BOUNDS.maximumX(); x++) {
                    authored.put(new Cell(x, y, z), block(x, y, z));
                }
            }
        }
        EnumMap<CellId, Cell> bases = new EnumMap<>(CellId.class);
        EnumMap<CellId, Cell> originals = new EnumMap<>(CellId.class);
        EnumMap<CellId, Cell> reconstructions = new EnumMap<>(CellId.class);
        int[] leftX = {-6, -4, -2, -6, -4, -2};
        int[] rightX = {2, 4, 6, 2, 4, 6};
        int[] z = {-2, -2, -2, 1, 1, 1};
        for (int index = 0; index < CellId.values().length; index++) {
            CellId id = CellId.values()[index];
            bases.put(id, new Cell(leftX[index], 1, z[index]));
            originals.put(id, new Cell(leftX[index], 2, z[index]));
            reconstructions.put(id, new Cell(rightX[index], 2, z[index]));
        }
        cells = Map.copyOf(authored);
        originalBases = Map.copyOf(bases);
        originalCells = Map.copyOf(originals);
        reconstructionCells = Map.copyOf(reconstructions);
        for (Cell base : originalBases.values()) {
            if (!"minecraft:chiseled_copper".equals(cells.get(base))) {
                throw new IllegalStateException("M07 edit base missing from manifest");
            }
        }
        manifestSha256 = hash(authored);
    }

    private static String block(int x, int y, int z) {
        if (y == 0) return ((x + z) & 1) == 0 ? "minecraft:polished_tuff" : "minecraft:tuff_bricks";
        if (y == 6 || Math.abs(x) == 8 || Math.abs(z) == 6) {
            if (z == 6 && x == 0 && y <= 2) return "minecraft:air";
            return y == 3 ? "minecraft:waxed_cut_copper" : "minecraft:deepslate_bricks";
        }
        if (y == 1 && (z == -2 || z == 1)
                && (x == -6 || x == -4 || x == -2 || x == 2 || x == 4 || x == 6)) {
            return x < 0 ? "minecraft:chiseled_copper" : "minecraft:oxidized_cut_copper";
        }
        if (x == 0 && z == 4 && y == 1) return "minecraft:chiseled_copper";
        if (x == 0 && z == 4 && y == 2) return "minecraft:copper_bulb[lit=false,powered=false]";
        if (x == 0 && y >= 1 && y <= 3 && z >= -3 && z <= 2) return "minecraft:copper_grate";
        return "minecraft:air";
    }

    public Map<Cell, String> cells() { return cells; }
    public Map<CellId, Cell> originalBases() { return originalBases; }
    public Map<CellId, Cell> originalCells() { return originalCells; }
    public Map<CellId, Cell> reconstructionCells() { return reconstructionCells; }
    public Cell commitConsole() { return commitConsole; }
    public Cell commitLamp() { return commitLamp; }
    public String manifestSha256() { return manifestSha256; }

    private static String hash(Map<Cell, String> values) {
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<Cell, String> entry : values.entrySet()) {
            Cell cell = entry.getKey();
            canonical.append(cell.x()).append(',').append(cell.y()).append(',').append(cell.z())
                    .append('=').append(entry.getValue()).append('\n');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public record Cell(int x, int y, int z) { }
    public record Bounds(int minimumX, int maximumX, int minimumY, int maximumY,
                         int minimumZ, int maximumZ) { }
}
