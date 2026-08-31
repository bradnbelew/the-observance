package com.observance.watcher.morrow.branch;

import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Anchor;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Ending;
import com.observance.watcher.morrow.branch.BranchGovernanceAuthority.Rule;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact bounded M12 rollback route, evidence vault, policy bench, and four ending gates. */
public final class BranchGovernanceManifest {
    public static final Bounds BOUNDS = new Bounds(-18, 18, 0, 8, -12, 12);
    private final Map<Cell, String> cells;
    private final Map<Anchor, Cell> anchorConsoles = Map.of(
            Anchor.CONTRADICTION, new Cell(-12, 1, 5), Anchor.WITNESS, new Cell(-6, 1, 5),
            Anchor.CONSENT, new Cell(0, 1, 5), Anchor.PROVENANCE, new Cell(6, 1, 5),
            Anchor.RIGHT_TO_STOP, new Cell(12, 1, 5));
    private final Map<Rule, Cell> ruleConsoles = Map.of(
            Rule.CONSENT, new Cell(-9, 1, -2), Rule.PROVENANCE, new Cell(-3, 1, -2),
            Rule.STOP, new Cell(3, 1, -2), Rule.REPLACEMENT, new Cell(9, 1, -2));
    private final Map<Ending, Cell> endingConsoles = Map.of(
            Ending.CERTIFY, new Cell(-12, 1, -8), Ending.PRESERVE_AUDIT, new Cell(-4, 1, -8),
            Ending.CLOSE_TICKET, new Cell(4, 1, -8), Ending.CREATE_NEW_BRANCH, new Cell(12, 1, -8));
    private final Map<String, Cell> stateLamps = Map.ofEntries(
            Map.entry("anchor_contradiction", new Cell(-12, 2, 3)), Map.entry("anchor_witness", new Cell(-6, 2, 3)),
            Map.entry("anchor_consent", new Cell(0, 2, 3)), Map.entry("anchor_provenance", new Cell(6, 2, 3)),
            Map.entry("anchor_right_to_stop", new Cell(12, 2, 3)), Map.entry("rule_consent", new Cell(-9, 2, -4)),
            Map.entry("rule_provenance", new Cell(-3, 2, -4)), Map.entry("rule_stop", new Cell(3, 2, -4)),
            Map.entry("rule_replacement", new Cell(9, 2, -4)), Map.entry("anchors_filed", new Cell(-6, 2, 9)),
            Map.entry("policy_filed", new Cell(-2, 2, 9)), Map.entry("governance", new Cell(2, 2, 9)),
            Map.entry("coda", new Cell(6, 2, 9)));
    private final Cell startConsole = new Cell(0, 1, 10);
    private final Cell anchorCommitConsole = new Cell(0, 1, 2);
    private final Cell governanceConsole = new Cell(-2, 1, -10);
    private final Cell codaConsole = new Cell(2, 1, -10);
    private final Cell resetConsole = new Cell(-5, 1, 10);
    private final Cell safeReturn = new Cell(0, 1, 9);
    private final String manifestSha256;

    public BranchGovernanceManifest() {
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
            boolean rollbackRoute = (z >= 1 && Math.abs(x - routeX(z)) <= 1) || (z <= 0 && Math.abs(x) <= 2);
            if (rollbackRoute) return z % 2 == 0 ? "minecraft:waxed_exposed_cut_copper" : "minecraft:polished_blackstone";
            if (z <= -6) return ((x + z) & 1) == 0 ? "minecraft:polished_tuff" : "minecraft:polished_deepslate";
            return ((x + z) & 1) == 0 ? "minecraft:deepslate_tiles" : "minecraft:tuff_bricks";
        }
        if (anchorConsoles.containsValue(cell)) return "minecraft:chiseled_bookshelf";
        if (ruleConsoles.containsValue(cell)) return "minecraft:crafter";
        if (endingConsoles.containsValue(cell)) return "minecraft:lodestone";
        if (startConsole.equals(cell)) return "minecraft:respawn_anchor[charges=0]";
        if (anchorCommitConsole.equals(cell)) return "minecraft:crying_obsidian";
        if (governanceConsole.equals(cell)) return "minecraft:beacon";
        if (codaConsole.equals(cell)) return "minecraft:end_gateway";
        if (resetConsole.equals(cell)) return "minecraft:target";
        if (stateLamps.containsValue(cell)) return "minecraft:copper_bulb[lit=false,powered=false]";
        if (y == 8 || Math.abs(x) == 18 || Math.abs(z) == 12) {
            if (z == 12 && x == 0 && y <= 2) return "minecraft:air";
            if (z == -12 && x == 0 && y <= 2) return "minecraft:air";
            return "minecraft:deepslate_bricks";
        }
        boolean anchorRail = z == 6 && x >= -15 && x <= 15 && y == 1;
        if (anchorRail) return x % 3 == 0 ? "minecraft:copper_grate" : "minecraft:polished_deepslate";
        boolean policyRail = z == -1 && x >= -12 && x <= 12 && y == 1;
        if (policyRail) return x % 3 == 0 ? "minecraft:waxed_copper_grate" : "minecraft:polished_tuff";
        boolean endingRail = z == -9 && x >= -15 && x <= 15 && y == 1;
        if (endingRail) return x % 4 == 0 ? "minecraft:amethyst_block" : "minecraft:reinforced_deepslate";
        return "minecraft:air";
    }

    private static int routeX(int z) {
        return switch (z) { case 10, 9 -> 0; case 8, 7 -> -10; case 6, 5 -> 10; case 4, 3 -> -6; default -> 0; };
    }
    public Map<Cell, String> cells() { return cells; }
    public Map<Anchor, Cell> anchorConsoles() { return anchorConsoles; }
    public Map<Rule, Cell> ruleConsoles() { return ruleConsoles; }
    public Map<Ending, Cell> endingConsoles() { return endingConsoles; }
    public Map<String, Cell> stateLamps() { return stateLamps; }
    public Cell startConsole() { return startConsole; }
    public Cell anchorCommitConsole() { return anchorCommitConsole; }
    public Cell governanceConsole() { return governanceConsole; }
    public Cell codaConsole() { return codaConsole; }
    public Cell resetConsole() { return resetConsole; }
    public Cell safeReturn() { return safeReturn; }
    public String manifestSha256() { return manifestSha256; }
    private static String hash(Map<Cell, String> values) {
        StringBuilder canonical = new StringBuilder();
        for (Map.Entry<Cell, String> entry : values.entrySet()) { Cell cell = entry.getKey();
            canonical.append(cell.x()).append(',').append(cell.y()).append(',').append(cell.z())
                    .append('=').append(entry.getValue()).append('\n'); }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public record Cell(int x, int y, int z) { }
    public record Bounds(int minimumX, int maximumX, int minimumY, int maximumY, int minimumZ, int maximumZ) { }
}
