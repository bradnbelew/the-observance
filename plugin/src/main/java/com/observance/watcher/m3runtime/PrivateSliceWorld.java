package com.observance.watcher.m3runtime;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exact Paper projection of the authored M3 v4 private slice. */
public final class PrivateSliceWorld {
    public static final int MIN_X = -34;
    public static final int MAX_X = 34;
    public static final int MIN_Y = -24;
    public static final int MAX_Y = 10;
    public static final int MIN_Z = -10;
    public static final int MAX_Z = 92;
    public static final int GATE_CLOSED_COLLISION_CELLS = 88;

    private final World world;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final Map<Cell, Material> expected = new LinkedHashMap<>();
    private final Map<Cell, String> expectedBlockData = new LinkedHashMap<>();
    private final Map<Cell, EvidenceSurface> evidence = new LinkedHashMap<>();
    private final Map<Cell, SubmissionSurface> submissions = new LinkedHashMap<>();
    private final Map<Cell, ReferenceSurface> references = new LinkedHashMap<>();
    private final Map<Cell, SignSurface> signs = new LinkedHashMap<>();
    private final Map<Cell, SupportSpec> supports = new LinkedHashMap<>();
    private final Map<String, DecorativeCluster> clusters = new LinkedHashMap<>();

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
            Block block = block(entry.getKey());
            if (block.getType() != entry.getValue()) {
                block.setType(entry.getValue(), false);
                writes++;
            }
        }
        for (Map.Entry<Cell, String> entry : expectedBlockData.entrySet()) {
            Block block = block(entry.getKey());
            if (!entry.getValue().equals(block.getBlockData().getAsString())) {
                block.setBlockData(Bukkit.createBlockData(entry.getValue()), false);
                writes++;
            }
        }
        decorateSurfaces();
        return writes;
    }

    public Audit audit() {
        List<String> findings = new ArrayList<>();
        Map<Material, Integer> counts = new EnumMap<>(Material.class);
        for (Map.Entry<Cell, Material> entry : expected.entrySet()) {
            Material actual = block(entry.getKey()).getType();
            counts.merge(actual, 1, Integer::sum);
            if (actual != entry.getValue() && findings.size() < 40) {
                findings.add(entry.getKey() + " expected=" + entry.getValue() + " actual=" + actual);
            }
        }
        for (Map.Entry<Cell, String> entry : expectedBlockData.entrySet()) {
            String actual = block(entry.getKey()).getBlockData().getAsString();
            if (!entry.getValue().equals(actual) && findings.size() < 80) {
                findings.add(entry.getKey() + " block_data expected=" + entry.getValue() + " actual=" + actual);
            }
        }
        for (Map.Entry<Cell, EvidenceSurface> row : evidence.entrySet()) {
            EvidenceSurface surface = row.getValue();
            if (expected.get(row.getKey()) != surface.physicalMaterial()) {
                findings.add(row.getKey() + " evidence medium/material drift: " + surface.surfaceId());
            }
            if (surface.presentation() == Presentation.NATIVE_BOOK
                    && (surface.body().isBlank() || bookPages(surface.body()).isEmpty())) {
                findings.add(surface.surfaceId() + " native-book evidence missing authored pages");
            }
            if (surface.presentation() == Presentation.VISIBLE_ENVIRONMENTAL_RECORD
                    && (surface.physicalMaterial() != Material.OAK_WALL_SIGN || !signs.containsKey(surface.cell()))) {
                findings.add(surface.surfaceId() + " environmental record is not visibly authored in-world");
            }
            if (wordCount(surface.body()) > 115) findings.add(surface.surfaceId() + " evidence prose too long for medium");
        }
        for (Map.Entry<Cell, SubmissionSurface> row : submissions.entrySet()) {
            if (!lecternMatches(row.getKey(), row.getValue().asEvidence())) {
                findings.add(row.getKey() + " filing/readback surface missing/drifted: " + row.getValue().surfaceId());
            }
        }
        for (Map.Entry<Cell, ReferenceSurface> row : references.entrySet()) {
            if (row.getValue().physicalMaterial() == Material.LECTERN
                    && !lecternMatches(row.getKey(), row.getValue().asEvidence())) {
                findings.add(row.getKey() + " reference book missing/drifted: " + row.getValue().surfaceId());
            }
        }
        for (Map.Entry<Cell, SignSurface> row : signs.entrySet()) {
            if (!signMatches(row.getKey(), row.getValue())) findings.add(row.getKey() + " authored sign drift");
        }
        for (Map.Entry<Cell, SupportSpec> row : supports.entrySet()) {
            if (block(row.getValue().supportCell()).getType().isAir()) {
                findings.add(row.getKey() + " unsupported furnishing purpose=" + row.getValue().purpose());
            }
        }
        for (EvidenceSurface surface : evidence.values()) {
            checkReader(findings, surface.cell(), surface.readerCell(), surface.surfaceId());
        }
        for (SubmissionSurface surface : submissions.values()) {
            checkReader(findings, surface.cell(), surface.readerCell(), surface.surfaceId());
        }
        for (ReferenceSurface surface : references.values()) {
            checkReader(findings, surface.cell(), surface.readerCell(), surface.surfaceId());
        }
        for (SignSurface surface : signs.values()) {
            checkReader(findings, surface.cell(), surface.readerCell(), surface.surfaceId());
            double eyeDelta = Math.abs((surface.cell().y + 0.5) - (surface.readerCell().y + 1.62));
            if (eyeDelta > 1.25) findings.add(surface.surfaceId() + " sign outside player-eye band: " + eyeDelta);
        }
        for (DecorativeCluster cluster : clusters.values()) {
            if (cluster.purpose().isBlank() || cluster.cells().isEmpty()) {
                findings.add("unclassified decorative cluster: " + cluster.clusterId());
            }
        }
        for (Map.Entry<Cell, Material> row : expected.entrySet()) {
            Cell cell = row.getKey();
            if (requiresSupport(row.getValue()) && block(new Cell(cell.x, cell.y - 1, cell.z)).getType().isAir()
                    && !supports.containsKey(cell)) {
                findings.add(cell + " unclassified floating furnishing material=" + row.getValue());
            }
        }
        checkInvestigationTopology(findings);
        checkWaterworks(findings, counts.getOrDefault(Material.WATER, 0));
        checkCorridor(findings, 12, 27, 69, 71, "public record-office corridor");
        checkCorridor(findings, 12, 27, 80, 82, "staff record corridor");
        checkImmersiveText(findings);
        int gateCollision = gateCollisionCells();
        return new Audit(expected.size(), List.copyOf(findings), worldHash(), Map.copyOf(counts),
                evidence.size(), submissions.size(), references.size(), signs.size(), supports.size(), clusters.size(), gateCollision);
    }

    public void setGate(boolean open) {
        for (int x = -5; x <= 5; x++) for (int y = -20; y <= -13; y++) {
            Cell cell = new Cell(x, y, 89);
            Material material = open ? Material.AIR : Material.COPPER_GRATE;
            expected.put(cell, material);
            if (open) {
                expectedBlockData.remove(cell);
                block(cell).setType(Material.AIR, false);
            } else {
                String data = Bukkit.createBlockData("minecraft:copper_grate[waterlogged=false]").getAsString();
                expectedBlockData.put(cell, data);
                block(cell).setBlockData(Bukkit.createBlockData(data), false);
            }
        }
    }

    public boolean inside(Location location) {
        if (location == null || location.getWorld() != world) return false;
        Cell cell = relative(location);
        return cell.x >= MIN_X && cell.x <= MAX_X && cell.y >= MIN_Y && cell.y <= MAX_Y
                && cell.z >= MIN_Z && cell.z <= MAX_Z;
    }

    public boolean beyondClosedGate(Location location) {
        if (!inside(location)) return false;
        Cell cell = relative(location);
        return cell.z >= 89 && cell.x >= -6 && cell.x <= 6 && cell.y >= -21 && cell.y <= -12;
    }

    public EvidenceSurface evidenceAt(Location location) {
        return location == null ? null : evidence.get(relative(location));
    }

    public SubmissionSurface submissionAt(Location location) {
        return location == null ? null : submissions.get(relative(location));
    }

    public ReferenceSurface referenceAt(Location location) {
        return location == null ? null : references.get(relative(location));
    }

    public Location absolute(int x, int y, int z) {
        return new Location(world, originX + x, originY + y, originZ + z);
    }

    public boolean inWatcherWestZone(Location location) {
        Cell c = relative(location);
        return c.y == -20 && c.x >= 22 && c.x <= 26 && c.z >= 68 && c.z <= 72;
    }

    public boolean inWatcherEastZone(Location location) {
        Cell c = relative(location);
        return c.y == -20 && c.x >= 28 && c.x <= 30 && c.z >= 68 && c.z <= 72;
    }

    public int gateCollisionCells() {
        int count = 0;
        for (int x = -5; x <= 5; x++) for (int y = -20; y <= -13; y++) {
            if (block(new Cell(x, y, 89)).getType().isSolid()) count++;
        }
        return count;
    }

    public String worldHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            expected.keySet().stream().sorted(Comparator.comparingInt(Cell::x)
                            .thenComparingInt(Cell::y).thenComparingInt(Cell::z))
                    .forEach(cell -> digest.update((cell.x + "," + cell.y + "," + cell.z + "="
                            + block(cell).getType().getKey() + "|" + block(cell).getBlockData().getAsString()
                            + "\n").getBytes(StandardCharsets.UTF_8)));
            evidence.values().forEach(surface -> digest.update((surface.canonical() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            submissions.values().forEach(surface -> digest.update((surface.canonical() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            references.values().forEach(surface -> digest.update((surface.canonical() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            signs.values().forEach(surface -> digest.update((surface.canonical() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            supports.forEach((cell, support) -> digest.update((cell + "=" + support.canonical() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            clusters.values().forEach(cluster -> digest.update((cluster.canonical() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            return hex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private void compile(boolean gateOpen) {
        fillEnvelope();
        room(-12, 12, 0, 9, 0, 15, Material.POLISHED_ANDESITE, Material.TUFF_BRICKS,
                Material.DARK_OAK_PLANKS);
        room(-16, 16, -20, -10, 56, 78, Material.POLISHED_ANDESITE, Material.TUFF_BRICKS,
                Material.DEEPSLATE_TILES);
        room(-6, 6, -20, -12, 79, 89, Material.DEEPSLATE_TILES, Material.TUFF_BRICKS,
                Material.POLISHED_BLACKSTONE_BRICKS);
        room(18, 32, -20, -12, 62, 84, Material.POLISHED_ANDESITE, Material.STONE_BRICKS,
                Material.DARK_OAK_PLANKS);
        room(12, 21, -20, -14, 79, 83, Material.CUT_COPPER, Material.STONE_BRICKS,
                Material.DEEPSLATE_TILES);
        room(-6, 6, -20, -12, 90, 92, Material.DEEPSLATE_TILES, Material.POLISHED_BLACKSTONE_BRICKS,
                Material.POLISHED_BLACKSTONE_BRICKS);
        descent();
        transitionsAndRoutes();
        architecturalDetail();
        waterworks();
        copyOffice();
        intakeClerkWorkplace();
        publicFilingCounter();
        referenceSurfaces();
        evidenceSurfaces();
        submissionSurfaces();
        thresholdSigns();
        gateFrame(gateOpen);
    }

    private void fillEnvelope() {
        for (int x = MIN_X; x <= MAX_X; x++) for (int y = MIN_Y; y <= MAX_Y; y++)
            for (int z = MIN_Z; z <= MAX_Z; z++) expected.put(new Cell(x, y, z), Material.DEEPSLATE);
    }

    private void descent() {
        for (int z = 16; z <= 55; z++) {
            int floor = -Math.floorDiv(z - 16, 2);
            Material floorMaterial = z <= 28 ? Material.COBBLED_DEEPSLATE
                    : z <= 42 ? Material.POLISHED_ANDESITE : Material.DEEPSLATE_TILES;
            Material wallMaterial = z <= 28 ? Material.TUFF_BRICKS
                    : z <= 42 ? Material.STONE_BRICKS : Material.DEEPSLATE_BRICKS;
            for (int x = -4; x <= 4; x++) column(x, floor, z, 5, floorMaterial);
            for (int y = floor; y < floor + 5; y++) {
                expected.put(new Cell(-5, y, z), wallMaterial);
                expected.put(new Cell(5, y, z), wallMaterial);
            }
            for (int x = -5; x <= 5; x++) expected.put(new Cell(x, floor + 5, z), wallMaterial);
            if (z != 16 && (z - 16) % 6 == 0) {
                for (int y = floor; y < floor + 5; y++) {
                    expected.put(new Cell(-4, y, z), Material.POLISHED_BASALT);
                    expected.put(new Cell(4, y, z), Material.POLISHED_BASALT);
                }
                hangingLantern(0, floor + 4, z, "descent rib light");
            }
        }
    }

    private void transitionsAndRoutes() {
        for (int[] transition : new int[][]{{15,0},{16,0},{55,-19},{56,-20},{78,-20},{79,-20},
                {88,-20},{89,-20},{90,-20}}) {
            for (int x = -4; x <= 4; x++) door(x, transition[1], transition[0], 5,
                    Material.POLISHED_ANDESITE);
        }
        doorCells(new int[][]{{17,-20,68},{17,-20,69},{17,-20,70},{17,-20,71},{17,-20,72},
                {18,-20,68},{18,-20,69},{18,-20,70},{18,-20,71},{18,-20,72}}, 4);
        doorCells(new int[][]{{17,-20,80},{17,-20,81},{17,-20,82},{18,-20,80},{18,-20,81},
                {18,-20,82}}, 4);
        route(new int[][]{{12,-20,81},{27,-20,81}}, 3, Material.CUT_COPPER);
        route(new int[][]{{0,-20,70},{27,-20,70}}, 3, Material.POLISHED_ANDESITE);
        route(new int[][]{{20,-20,65},{20,-20,70},{20,-20,76},{20,-20,82}}, 2,
                Material.POLISHED_ANDESITE);
    }

    private void architecturalDetail() {
        for (int x = -11; x <= 11; x += 5) {
            for (int y = 0; y <= 8; y++) {
                expected.put(new Cell(x, y, 0), Material.MOSSY_STONE_BRICKS);
                expected.put(new Cell(x, y, 15), Material.MOSSY_STONE_BRICKS);
            }
        }
        for (int z : new int[]{3, 8, 13}) {
            hangingLantern(-10, 8, z, "Mouth bay light");
            hangingLantern(10, 8, z, "Mouth bay light");
        }
        for (int z = 2; z <= 14; z++) {
            clusterBlock("MOUTH_LOADED_RUT", new Cell(3, -1, z), Material.PACKED_MUD,
                    "east loaded-cart rut worn into the shared public road");
            clusterBlock("MOUTH_RETURN_RUT", new Cell(-3, -1, z), Material.COARSE_DIRT,
                    "west empty-cart return rut worn into the same road crown");
        }
        for (int x : new int[]{-10, 10}) for (int z : new int[]{5, 6, 10, 11})
            directionalCluster("MOUTH_WAITING_BENCHES", new Cell(x, 0, z), Material.DARK_OAK_STAIRS,
                    "minecraft:dark_oak_stairs[facing=" + (x < 0 ? "east" : "west")
                            + ",half=bottom,shape=straight,waterlogged=false]",
                    "wall-backed benches for hauliers waiting on the examiner");
        for (int x = -1; x <= 1; x++) for (int z = 4; z <= 5; z++)
            clusterBlock("MOUTH_EXAMINER_DESK", new Cell(x, 0, z), Material.DARK_OAK_PLANKS,
                    "public examiner desk beside the shared cart crown");
        for (int x : new int[]{-12, -6, 0, 6, 12}) {
            for (int y = -20; y <= -11; y++) {
                expected.put(new Cell(x, y, 56), Material.DEEPSLATE_TILE_WALL);
                expected.put(new Cell(x, y, 78), Material.DEEPSLATE_TILE_WALL);
            }
        }
        for (int z : new int[]{59, 65, 71, 76}) {
            hangingLantern(-14, -11, z, "intake west service light");
            hangingLantern(14, -11, z, "intake east service light");
        }
        for (int z : new int[]{64, 69, 74, 82}) {
            hangingLantern(20, -13, z, "record-office task light");
            hangingLantern(30, -13, z, "record-office task light");
        }
        for (int x = -5; x <= 5; x++) {
            expected.put(new Cell(x, -21, 79), Material.WAXED_WEATHERED_CUT_COPPER);
            expected.put(new Cell(x, -21, 88), Material.WAXED_WEATHERED_CUT_COPPER);
        }
    }

    private void waterworks() {
        waterComponent("INLET_TROUGH", -14, -13, 59, 62,
                "receive surface and quarry runoff");
        waterComponent("SETTLING_BASIN", -14, -9, 63, 67,
                "drop silt before gauging");
        waterComponent("GAUGING_FLUME", -12, -11, 68, 75,
                "constrain measurable flow against the 300-berth limit");
        waterComponent("GRATED_SUMP", -13, -10, 76, 77,
                "collect measured flow for the lower filter");
        for (int z = 59; z <= 77; z++) {
            int west = z <= 67 ? -15 : z <= 75 ? -13 : -14;
            int east = z <= 62 ? -12 : z <= 67 ? -8 : z <= 75 ? -10 : -9;
            clusterBlock("RUNOFF_CURBS", new Cell(west, -20, z), Material.WEATHERED_CUT_COPPER,
                    "continuous curbs reveal the ordered inlet-to-sump path");
            clusterBlock("RUNOFF_CURBS", new Cell(east, -20, z), Material.WEATHERED_CUT_COPPER,
                    "continuous curbs reveal the ordered inlet-to-sump path");
        }
        for (int x = -14; x <= -13; x++)
            clusterBlock("INLET_HEADWALL", new Cell(x, -20, 58), Material.WEATHERED_CUT_COPPER,
                    "headwall makes the runoff entry unambiguous");
        for (int x = -13; x <= -10; x++)
            directionalCluster("SUMP_GRATE", new Cell(x, -20, 78), Material.COPPER_GRATE,
                    "minecraft:copper_grate[waterlogged=false]",
                    "full-width grate marks transfer to the lower filter");
        for (int z : new int[]{63, 67, 75}) for (int x = -12; x <= -11; x++)
            directionalCluster("FLOW_BAFFLES", new Cell(x, -20, z), Material.COPPER_GRATE,
                    "minecraft:copper_grate[waterlogged=false]",
                    "baffles separate settling, gauging, and sump stages");
        clusterBlock("RUNOFF_GAUGE_SUPPORT", new Cell(-14, -18, 62), Material.WEATHERED_CUT_COPPER,
                "masonry support for the rated-capacity plaque");
        for (int y = -20; y <= -18; y++)
            clusterBlock("RUNOFF_GAUGE_SUPPORT", new Cell(-14, y, 62), Material.WEATHERED_CUT_COPPER,
                    "masonry support for the rated-capacity plaque");
    }

    private void copyOffice() {
        furniture(Material.DARK_OAK_PLANKS, new int[][]{{22,-20,65},{23,-20,65},{24,-20,65},
                {25,-20,65},{26,-20,65},{22,-20,66},{23,-20,66},{24,-20,66},{25,-20,66},
                {26,-20,66},{22,-20,74},{23,-20,74},{24,-20,74},{25,-20,74},{26,-20,74},
                {22,-20,75},{23,-20,75},{24,-20,75},{25,-20,75},{26,-20,75}});
        for (int x : new int[]{28,30}) for (int z : new int[]{69,71})
            clusterBlock("BINDING_ISLAND", new Cell(x, -20, z), Material.STRIPPED_SPRUCE_LOG,
                    "supported binding and collation island");
        for (int x = 28; x <= 30; x++) for (int z = 69; z <= 71; z++) {
            Cell cell = new Cell(x, -19, z);
            directional(cell, Material.DARK_OAK_SLAB,
                    "minecraft:dark_oak_slab[type=top,waterlogged=false]");
            support(cell, new Cell(x == 29 ? 28 : x, -20, z == 70 ? 69 : z),
                    "binding worktop support");
            cluster("BINDING_ISLAND", cell, "supported binding and collation island");
        }
        bookshelfRank(31, 63, 67, "SURVEY_CABINET_RANK");
        bookshelfRank(31, 78, 83, "ROOM_ROLL_CABINET_RANK");
        for (int x = 22; x <= 26; x++) {
            expected.put(new Cell(x, -19, 65), Material.WHITE_CARPET);
            expected.put(new Cell(x, -19, 74), Material.WHITE_CARPET);
        }
        for (int z : new int[]{67, 76}) {
            directional(new Cell(23, -20, z), Material.SPRUCE_STAIRS,
                    "minecraft:spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
            directional(new Cell(25, -20, z), Material.SPRUCE_STAIRS,
                    "minecraft:spruce_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]");
        }
        for (int z : new int[]{64, 75, 79, 82}) {
            Cell lower = new Cell(29, -20, z);
            Cell upper = new Cell(29, -19, z);
            expected.put(lower, Material.BOOKSHELF);
            expected.put(upper, Material.BOOKSHELF);
            support(lower, new Cell(29, -21, z), "floor-supported reference shelf");
            support(upper, lower, "stacked reference shelf");
            cluster("REFERENCE_SHELVES", lower, "supported frequently-used reference volumes");
            cluster("REFERENCE_SHELVES", upper, "supported frequently-used reference volumes");
        }
        for (int z = 62; z <= 84; z += 11) for (int x = 20; x <= 30; x += 5)
            clusterBlock("OFFICE_FLOOR_INLAYS", new Cell(x, -21, z), Material.WAXED_CUT_COPPER,
                    "floor studs mark copy bays and cabinet rank intervals");
    }

    private void intakeClerkWorkplace() {
        for (int x = 8; x <= 14; x++) for (int z : new int[]{60, 61})
            clusterBlock("INTAKE_PUBLIC_COUNTER", new Cell(x, -20, z), Material.DARK_OAK_PLANKS,
                    "public counter for registering arrivals before records move to the copy office");
        for (int x : new int[]{9, 12}) for (int z : new int[]{64, 65, 75, 76})
            clusterBlock("INTAKE_CLERK_DESKS", new Cell(x, -20, z), Material.SPRUCE_PLANKS,
                    "paired clerk desks for berth and supply reconciliation");
        for (int x : new int[]{10, 13}) for (int z : new int[]{63, 74})
            directional(new Cell(x, -20, z), Material.SPRUCE_STAIRS,
                    "minecraft:spruce_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]");
        for (int z : new int[]{64, 76}) {
            clusterBlock("INTAKE_ACTIVE_FILES", new Cell(15, -20, z), Material.BOOKSHELF,
                    "supported active berth and supply files beside the clerk desks");
            clusterBlock("INTAKE_ACTIVE_FILES", new Cell(15, -19, z), Material.CHISELED_BOOKSHELF,
                    "supported active berth and supply files beside the clerk desks");
            support(new Cell(15, -20, z), new Cell(15, -21, z), "floor-supported intake file shelf");
            support(new Cell(15, -19, z), new Cell(15, -20, z), "stacked active-file shelf");
            directional(new Cell(15, -19, z), Material.CHISELED_BOOKSHELF,
                    "minecraft:chiseled_bookshelf[facing=west]");
        }
        for (int x = 8; x <= 14; x += 3) for (int z : new int[]{57, 77})
            directionalCluster("INTAKE_WAITING_BENCHES", new Cell(x, -20, z), Material.DARK_OAK_STAIRS,
                    "minecraft:dark_oak_stairs[facing=" + (z < 70 ? "south" : "north")
                            + ",half=bottom,shape=straight,waterlogged=false]",
                    "public waiting benches kept outside the hydraulic maintenance walk");
    }

    private void publicFilingCounter() {
        for (int x = -6; x <= 6; x++) {
            clusterBlock("EXAMINER_FILING_COUNTER", new Cell(x, -20, 76),
                    x == -6 || x == 6 ? Material.WAXED_WEATHERED_CUT_COPPER : Material.POLISHED_ANDESITE,
                    "single public counter where one reconciled report is lodged");
            if (x != 0) clusterBlock("COUNTER_WORKING_PAPERS", new Cell(x, -19, 76),
                    x % 3 == 0 ? Material.LIGHT_GRAY_CARPET : Material.WHITE_CARPET,
                    "composed stacks of working copies around the central findings ledger");
        }
        clusterBlock("EXAMINER_SIDE_TRAYS", new Cell(5, -20, 75), Material.DARK_OAK_PLANKS,
                "paired side tables carrying the examiner's seal and ink tools");
        clusterBlock("EXAMINER_SIDE_TRAYS", new Cell(-5, -20, 75), Material.DARK_OAK_PLANKS,
                "paired side tables carrying the examiner's seal and ink tools");
        directionalCluster("EXAMINER_SEAL_PRESS", new Cell(5, -19, 75), Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
                "minecraft:heavy_weighted_pressure_plate[power=0]",
                "weighted civic seal press beside the findings ledger");
        clusterBlock("EXAMINER_INK_TRAY", new Cell(-5, -19, 75), Material.BLACK_CARPET,
                "ink and blotting tray for the examiner's endorsed report");
    }

    private void referenceSurfaces() {
        addReference(0, 0, 5, "south", new Cell(0, 0, 7), "INTAKE_EXAMINER_DOCKET",
                "Examiner Mara Venn", "official work order", "Intake Examiner's Docket",
                "COMMISSION 14 — MARA VENN\nThe refuge register does not agree with the works file. Before I release the Commons seal, establish the public road, the building campaigns, the supported refuge places, and the reason for the down-cut.\f"
                + "I left one findings ledger on the Intake counter. Enter one clause under each heading, then lodge the whole report. Returned papers are not marked by section. Once endorsed, add the account that all four findings support.",
                Material.LECTERN, Presentation.NATIVE_BOOK, "briefing");
        addReferenceArtifact(31, -19, 83, "west", new Cell(28, -20, 83), "FIELD_ARCHIVE_READBACK",
                "Intake record office", "bound field archive", "Examiner's Field Archive", "archive",
                Material.CHISELED_BOOKSHELF, Presentation.NATIVE_BOOK);
    }

    private void evidenceSurfaces() {
        addEvidenceArtifact(-8,0,6,"south",new Cell(-8,0,8),"DRAINAGE_PLAN_TABLE","P4.F1","drainage_plan",
                "Neri Holt, works surveyor", "annotated drainage diagram", "Drainage Sheet 14-A",
                "NORTH: quarry drain — stops at headwall\nSOUTH: well drain — stops outside crown\nCENTER: one graded road through the Mouth\f"
                + "Holt's red pencil: 'Keep both gutters off the cart crown. Loaded side east; empty return west. Do not cut a second approach.'",
                Material.CARTOGRAPHY_TABLE, Presentation.NATIVE_BOOK);
        addEvidenceSign(8,2,1,"south",new Cell(8,2,0),new Cell(8,0,3),"CART_RUT_INSPECTION_TAG",
                "P4.F1","cart_rut_tag","Orris Pell, road inspector","field tag fixed beside the physical ruts",
                "ROAD CHECK 18","EAST: LOAD","WEST: RETURN","ONE CROWN");
        addEvidenceSign(-5,-18,56,"south",new Cell(-5,-18,55),new Cell(-5,-20,59),"THREE_CAMPAIGN_MASON_MARK",
                "P4.F2","mason_mark","Toma Rusk, municipal mason","chalk annotation across the physical construction seam",
                "RUSK — JOINT","A TUFF SHELL","B CIVIC STONE","C DEEP TILE");
        addEvidenceArtifact(31,-19,65,"west",new Cell(28,-20,65),"SURVEY_REVISION_LETTER","P4.F2","revision_letter",
                "Eda Sorn to Mara Venn", "personal cover letter with office marginalia", "Eda's Revision Cover",
                "Mara—\nI found the three rolls you asked for. A ends at the storm shell. B adds Intake and the copy office. C carries the Commons spine beneath the seal.\f"
                + "In the margin, in Mara's hand: 'Not repairs. Three appropriations, three crews, one continuing plan.'",
                Material.CHISELED_BOOKSHELF, Presentation.NATIVE_BOOK);
        addEvidenceArtifact(24,-19,66,"south",new Cell(24,-20,68),"BERTH_REGISTER","P4.F3","berth_register",
                "Lio Marr, refuge registrar", "official refuge register", "Refuge Register — East Leaf",
                "Assigned sleeping places ........ 286\nInfirmary reserve cots ............. 8\nSupported refuge places .......... 294\f"
                + "Work berths in pump, copy, and watch rooms: 42. These are shifts, not sleeping places, and are excluded from the refuge total.",
                Material.CHISELED_BOOKSHELF, Presentation.NATIVE_BOOK);
        addEvidenceSign(19,-18,75,"east",new Cell(18,-18,75),new Cell(21,-20,75),"RATION_TALLY_NOTICE",
                "P4.F3","ration_tally","Sela Orr, quartermaster","posted seven-day supply tally",
                "7-DAY TALLY","294 FULL SHARES","WATER GAUGE 300","42 SHIFTS OMIT");
        expected.put(new Cell(-15, -18, 72), Material.WEATHERED_CUT_COPPER);
        addEvidenceSign(-14,-18,72,"east",new Cell(-15,-18,72),new Cell(-11,-20,72),"PUMP_GAUGE_CARD",
                "P4.F4","pump_gauge","Iven Quill, pump engineer","maintenance gauge card beside the working hydraulic train",
                "PUMP CARD 7","LOWER: 11 LIFT","WARM + STEADY","WINTER ROUTE");
        addEvidenceArtifact(29,-19,79,"west",new Cell(26,-20,79),"FOUNDING_ENGINEER_LETTER","P4.F4","engineer_letter",
                "Iven Quill to Councillor Meve", "personal engineering letter retained with works minutes", "Quill's Winter Letter",
                "Councillor—\nA level gallery saves digging but costs us every winter: longer pipe, colder wall, and another exposed haul.\f"
                + "Drive down beside the sump. The cover is stable, the pump lift shortens, and stores reach the Commons without crossing the runoff walk. —I. Quill",
                Material.CHISELED_BOOKSHELF, Presentation.NATIVE_BOOK);
    }

    private void submissionSurfaces() {
        addSubmission(0,-19,76,"north",new Cell(0,-20,73),"EXAMINER_FINDINGS_LEDGER","Examiner's Findings Ledger");
    }

    private void thresholdSigns() {
        addSign(0,2,1,"south",new Cell(0,2,0),new Cell(0,0,3),"MOUTH_COMMISSION_PLAQUE",
                "municipal commission identity","INTAKE WORKS","COMMISSION 14","MARA VENN","");
        addSign(19,-18,66,"east",new Cell(18,-18,66),new Cell(21,-20,66),"RECORD_OFFICE_PLAQUE",
                "credible office identity","RECORD OFFICE","SURVEYS + COPIES","BELL AT DESK","");
    }

    private void gateFrame(boolean gateOpen) {
        for (int x = -6; x <= 6; x++) {
            expected.put(new Cell(x, -21, 89), Material.POLISHED_BLACKSTONE_BRICKS);
            expected.put(new Cell(x, -12, 89), Material.POLISHED_BLACKSTONE_BRICKS);
        }
        for (int y = -20; y <= -13; y++) {
            expected.put(new Cell(-6, y, 89), Material.POLISHED_BLACKSTONE_BRICKS);
            expected.put(new Cell(6, y, 89), Material.POLISHED_BLACKSTONE_BRICKS);
        }
        for (int x = -5; x <= 5; x++) for (int y = -20; y <= -13; y++) {
            Cell cell = new Cell(x, y, 89);
            if (gateOpen) expected.put(cell, Material.AIR);
            else directional(cell, Material.COPPER_GRATE, "minecraft:copper_grate[waterlogged=false]");
        }
        expected.put(new Cell(-6, -16, 88), Material.REDSTONE_LAMP);
        expected.put(new Cell(6, -16, 88), Material.REDSTONE_LAMP);
    }

    private void decorateSurfaces() {
        submissions.forEach((cell, surface) -> writeBook(cell, surface.asEvidence()));
        references.forEach((cell, surface) -> {
            if (surface.physicalMaterial() == Material.LECTERN) writeBook(cell, surface.asEvidence());
        });
        signs.forEach(this::writeSign);
    }

    private void writeBook(Cell cell, EvidenceSurface surface) {
        Block block = block(cell);
        if (!(block.getState() instanceof Lectern lectern)) return;
        ItemStack book = authoredBook(surface.title(), surface.author(), bookPages(surface.body()));
        // LecternInventory is live. Updating the earlier block-state snapshot after this
        // write replaces the inventory with that snapshot's empty contents on Paper.
        lectern.getInventory().setItem(0, book);
    }

    private boolean lecternMatches(Cell cell, EvidenceSurface surface) {
        if (!(block(cell).getState() instanceof Lectern lectern)) return false;
        ItemStack item = lectern.getInventory().getItem(0);
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !(item.getItemMeta() instanceof BookMeta meta))
            return false;
        return surface.title().equals(meta.getTitle()) && surface.author().equals(meta.getAuthor())
                && bookPages(surface.body()).equals(meta.pages());
    }

    public void openEvidenceBook(Player player, EvidenceSurface surface) {
        if (surface.presentation() != Presentation.NATIVE_BOOK) return;
        player.openBook(authoredBook(surface.title(), surface.author(), bookPages(surface.body())));
    }

    public void openReferenceBook(Player player, ReferenceSurface surface, PrivateSliceState state) {
        if ("archive".equals(surface.kind())) {
            List<Component> pages = new ArrayList<>();
            List<String> entered = new ArrayList<>();
            for (String finding : PrivateSliceState.BASE_FINDINGS) {
                entered.add(finding + ": " + state.observedSources(finding).size() + "/2 records; finding "
                        + (state.findingCommitted(finding) ? "endorsed" : "open"));
            }
            pages.add(Component.text("FIELD ARCHIVE\n\n" + String.join("\n", entered)));
            pages.add(Component.text("Changed place\n\nCommons seal: " + (state.gateOpen() ? "released" : "held")
                    + "\n\nThe filed report is durable. Unentered drafts are working paper and are not retained."));
            pages.add(Component.text("ACCESS COPY\n\nWest view: one capacity digit appears freshly overwritten.\n\n"
                    + "East view: the same digit remains worn.\n\nNeither view is evidence for the report."));
            player.openBook(authoredBook(surface.title(), surface.author(), pages));
            return;
        }
        player.openBook(authoredBook(surface.title(), surface.author(), bookPages(surface.body())));
    }

    public void openFilingLedger(Player player, PrivateSliceState state) {
        String contributor = player.getUniqueId().toString();
        Map<String, String> draft = state.draft(contributor);
        List<Component> pages = new ArrayList<>();
        pages.add(Component.text("EXAMINER'S FINDINGS\n\nOne clause must be marked under each heading. The whole report is examined together; returned papers are not marked by section."));
        pages.add(choicePage("PUBLIC ROAD", "P4.F1", draft, List.of(
                choice("Two separate public roads served the Mouth.", "two_separate_public_roads"),
                choice("One road carried loaded descent and empty return.", "one_road_loaded_down_empty_return"),
                choice("The road served drainage crews only.", "drainage_crews_only"),
                choice("No cart road crossed the threshold.", "no_cart_road_crossed"))));
        pages.add(choicePage("WORKS CAMPAIGNS", "P4.F2", draft, List.of(
                choice("One emergency build produced every room.", "single_emergency_build"),
                choice("Shelter and office were the only campaigns.", "two_campaigns_shelter_and_office"),
                choice("Four unrelated repairs made the visible joints.", "four_unrelated_repairs"),
                choice("Shelter, Intake, and Commons were three campaigns.", "three_campaigns_shelter_intake_commons"))));
        pages.add(choicePage("SUPPORTED PLACES", "P4.F3", draft, List.of(
                choice("328 people, including every work berth.", "328_people_including_work_berths"),
                choice("294 refuge places; work berths are excluded.", "294_refuge_places_work_berths_excluded"),
                choice("286 people; infirmary cots do not count.", "286_people_no_infirmary"),
                choice("300 people, equal to the water gauge.", "300_people_equal_to_water_gauge"))));
        pages.add(choicePage("THE DOWN-CUT", "P4.F4", draft, List.of(
                choice("It concealed the works from the public road.", "concealment_from_public_road"),
                choice("It reached a second entrance.", "access_to_a_second_entrance"),
                choice("Stable cover and shorter winter service required it.", "downcut_for_stable_cover_shorter_winter_service"),
                choice("It followed an accidental quarry break.", "accidental_quarry_breakthrough"))));
        Component lodge = Component.text("LODGE FOUR-CLAUSE REPORT")
                .clickEvent(ClickEvent.runCommand("/obsfile lodge"));
        pages.add(Component.text("REPORT ENDORSEMENT\n\nWhen all four clauses are marked, lodge the report at this counter.\n\n")
                .append(lodge));
        if (PrivateSliceState.BASE_FINDINGS.stream().allMatch(state::findingCommitted)) {
            pages.add(choicePage("ACCOUNT SUPPORTED", "P4.F5", draft, List.of(
                    choice("A temporary quarry shelter abandoned after one winter.", "temporary_quarry_shelter_abandoned_after_one_winter"),
                    choice("A planned civic intake for 294, not one emergency shelter.", "planned_civic_intake_for_294_not_single_emergency_shelter"),
                    choice("A private archive with no refuge role.", "private_archive_with_no_public_refuge_role"),
                    choice("A natural cave mistaken for civic works.", "natural_cave_later_mistaken_for_civic_works"))));
            pages.add(Component.text("COMMONS SEAL\n\nThe four endorsed findings support one account of the Mouth. Mark it, then lodge the seal endorsement.\n\n")
                    .append(Component.text("LODGE SEAL ENDORSEMENT")
                            .clickEvent(ClickEvent.runCommand("/obsfile seal"))));
        }
        player.openBook(authoredBook("Examiner's Findings", "Mara Venn", pages));
    }

    public boolean nearFilingLedger(Location location) {
        if (!inside(location)) return false;
        Cell c = relative(location);
        return Math.abs(c.x) <= 4 && c.y >= -20 && c.y <= -18 && c.z >= 72 && c.z <= 77;
    }

    private static Choice choice(String label, String id) { return new Choice(label, id); }

    private static Component choicePage(String heading, String finding, Map<String, String> draft,
            List<Choice> choices) {
        Component page = Component.text(heading + "\n\n");
        String selected = draft.get(finding);
        for (Choice choice : choices) {
            String marker = choice.id().equals(selected) ? "[X] " : "[ ] ";
            page = page.append(Component.text(marker + choice.label())
                    .clickEvent(ClickEvent.runCommand("/obsfile mark " + finding + " " + choice.id())))
                    .append(Component.text("\n\n"));
        }
        return page;
    }

    private static ItemStack authoredBook(String title, String author, List<Component> pages) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(title);
        meta.setAuthor(author);
        meta.pages(pages);
        book.setItemMeta(meta);
        return book;
    }

    private void writeSign(Cell cell, SignSurface surface) {
        if (!(block(cell).getState() instanceof Sign sign)) return;
        for (int i = 0; i < 4; i++) sign.getSide(Side.FRONT).line(i, Component.text(surface.lines().get(i)));
        sign.update(true, false);
    }

    private boolean signMatches(Cell cell, SignSurface surface) {
        if (!(block(cell).getState() instanceof Sign sign)) return false;
        for (int i = 0; i < 4; i++)
            if (!Component.text(surface.lines().get(i)).equals(sign.getSide(Side.FRONT).line(i))) return false;
        return true;
    }

    private void addEvidenceArtifact(int x, int y, int z, String facing, Cell readerCell, String surfaceId,
            String findingId, String sourceId, String author, String format, String title, String body,
            Material physicalMaterial, Presentation presentation) {
        Cell cell = new Cell(x, y, z);
        if (physicalMaterial == Material.CHISELED_BOOKSHELF) {
            directional(cell, physicalMaterial, "minecraft:chiseled_bookshelf[facing=" + facing + "]");
        } else {
            expected.put(cell, physicalMaterial);
        }
        support(cell, new Cell(x, y - 1, z), "supported " + format);
        evidence.put(cell, new EvidenceSurface(cell, readerCell, surfaceId, findingId, sourceId,
                author, format, title, body, physicalMaterial, presentation));
    }

    private void addEvidenceSign(int x, int y, int z, String facing, Cell supportCell, Cell readerCell,
            String surfaceId, String findingId, String sourceId, String author, String format,
            String... lines) {
        addSign(x, y, z, facing, supportCell, readerCell, surfaceId, format, lines);
        Cell cell = new Cell(x, y, z);
        evidence.put(cell, new EvidenceSurface(cell, readerCell, surfaceId, findingId, sourceId,
                author, format, String.join(" ", lines), String.join("\n", lines), Material.OAK_WALL_SIGN,
                Presentation.VISIBLE_ENVIRONMENTAL_RECORD));
    }

    private void addSubmission(int x, int y, int z, String facing, Cell readerCell,
            String surfaceId, String label) {
        Cell cell = new Cell(x, y, z);
        directional(cell, Material.LECTERN, lecternData(facing));
        support(cell, new Cell(x, y - 1, z), "central findings-ledger support in the public filing counter");
        submissions.put(cell, new SubmissionSurface(cell, readerCell, surfaceId, label));
    }

    private void addReference(int x, int y, int z, String facing, Cell readerCell,
            String surfaceId, String author, String format, String title, String body,
            Material physicalMaterial, Presentation presentation, String kind) {
        Cell cell = new Cell(x, y, z);
        directional(cell, physicalMaterial, lecternData(facing));
        support(cell, new Cell(x, y - 1, z), "supported " + format);
        references.put(cell, new ReferenceSurface(cell, readerCell, surfaceId, author, format, title,
                body, physicalMaterial, presentation, kind));
    }

    private void addReferenceArtifact(int x, int y, int z, String facing, Cell readerCell,
            String surfaceId, String author, String format, String title, String kind,
            Material physicalMaterial, Presentation presentation) {
        Cell cell = new Cell(x, y, z);
        directional(cell, physicalMaterial, "minecraft:chiseled_bookshelf[facing=" + facing + "]");
        support(cell, new Cell(x, y - 1, z), "supported " + format);
        references.put(cell, new ReferenceSurface(cell, readerCell, surfaceId, author, format, title,
                "Dynamic local-primary archive readback", physicalMaterial, presentation, kind));
    }

    private void addSign(int x, int y, int z, String facing, Cell supportCell, Cell readerCell,
            String surfaceId, String purpose, String... lines) {
        if (lines.length != 4) throw new IllegalArgumentException("authored signs require four exact lines");
        Cell cell = new Cell(x, y, z);
        directional(cell, Material.OAK_WALL_SIGN,
                "minecraft:oak_wall_sign[facing=" + facing + ",waterlogged=false]");
        support(cell, supportCell, purpose);
        signs.put(cell, new SignSurface(cell, supportCell, readerCell, surfaceId, purpose, List.of(lines)));
    }

    private void directional(Cell cell, Material material, String blockData) {
        expected.put(cell, material);
        String canonical = Bukkit.createBlockData(blockData).getAsString();
        if (Bukkit.createBlockData(canonical).getMaterial() != material) {
            throw new IllegalArgumentException("block data/material mismatch at " + cell);
        }
        expectedBlockData.put(cell, canonical);
    }

    private void support(Cell cell, Cell supportCell, String purpose) {
        supports.put(cell, new SupportSpec(supportCell, purpose));
    }

    private void cluster(String clusterId, Cell cell, String purpose) {
        DecorativeCluster cluster = clusters.computeIfAbsent(clusterId,
                ignored -> new DecorativeCluster(clusterId, purpose, new LinkedHashSet<>()));
        if (!cluster.purpose().equals(purpose)) throw new IllegalArgumentException("cluster purpose drift: " + clusterId);
        cluster.cells().add(cell);
    }

    private void clusterBlock(String clusterId, Cell cell, Material material, String purpose) {
        expected.put(cell, material);
        cluster(clusterId, cell, purpose);
    }

    private void directionalCluster(String clusterId, Cell cell, Material material, String blockData,
            String purpose) {
        directional(cell, material, blockData);
        cluster(clusterId, cell, purpose);
    }

    private void waterComponent(String clusterId, int minX, int maxX, int minZ, int maxZ, String purpose) {
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            Cell cell = new Cell(x, -21, z);
            directional(cell, Material.WATER, "minecraft:water[level=0]");
            cluster(clusterId, cell, purpose);
        }
    }

    private void bookshelfRank(int x, int minZ, int maxZ, String clusterId) {
        for (int z = minZ; z <= maxZ; z++) for (int y = -20; y <= -17; y++) {
            Cell cell = new Cell(x, y, z);
            directional(cell, Material.CHISELED_BOOKSHELF, "minecraft:chiseled_bookshelf[facing=west]");
            support(cell, new Cell(x, y - 1, z), "west-facing supported cabinet rank");
            cluster(clusterId, cell, "west-facing supported archival cabinet rank");
        }
    }

    private void hangingLantern(int x, int y, int z, String purpose) {
        Cell cell = new Cell(x, y, z);
        directional(cell, Material.LANTERN, "minecraft:lantern[hanging=true,waterlogged=false]");
        support(cell, new Cell(x, y + 1, z), purpose);
        cluster("TASK_LIGHTS", cell, "ceiling-attached civic task lighting");
    }

    private void checkReader(List<String> findings, Cell surface, Cell reader, String surfaceId) {
        if (!block(reader).getType().isAir() || !block(new Cell(reader.x, reader.y + 1, reader.z)).getType().isAir()) {
            findings.add(surfaceId + " reader standing cell blocked: " + reader);
        }
        int horizontal = Math.abs(surface.x - reader.x) + Math.abs(surface.z - reader.z);
        if (horizontal < 1 || horizontal > 4) findings.add(surfaceId + " reader distance invalid: " + horizontal);
        String data = expectedBlockData.get(surface);
        if (data != null && !facesReader(surface, reader, data)) {
            findings.add(surfaceId + " directional face does not address reader " + reader + ": " + data);
        }
        checkSightline(findings, surface, reader, surfaceId);
    }

    private void checkSightline(List<String> findings, Cell surface, Cell reader, String surfaceId) {
        double startX = reader.x + 0.5;
        double startY = reader.y + 1.62;
        double startZ = reader.z + 0.5;
        double endX = surface.x + 0.5;
        double endY = surface.y + 0.65;
        double endZ = surface.z + 0.5;
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(
                Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)
                        + Math.pow(endZ - startZ, 2)) * 8));
        for (int step = 1; step < steps; step++) {
            double ratio = (double) step / steps;
            Cell sample = new Cell((int) Math.floor(startX + (endX - startX) * ratio),
                    (int) Math.floor(startY + (endY - startY) * ratio),
                    (int) Math.floor(startZ + (endZ - startZ) * ratio));
            if (!sample.equals(reader) && !sample.equals(surface) && block(sample).getType().isSolid()) {
                findings.add(surfaceId + " reader sightline blocked at " + sample);
                return;
            }
        }
    }

    private void checkInvestigationTopology(List<String> findings) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        evidence.values().forEach(surface -> counts.merge(surface.findingId(), 1, Integer::sum));
        if (!counts.equals(Map.of("P4.F1", 2, "P4.F2", 2, "P4.F3", 2, "P4.F4", 2))) {
            findings.add("investigation evidence topology must be exact paired records: " + counts);
        }
        if (references.size() != 2 || submissions.size() != 1) {
            findings.add("investigation briefing/filing topology drift references=" + references.size()
                    + " submissions=" + submissions.size());
        }
        long lecterns = expected.values().stream().filter(material -> material == Material.LECTERN).count();
        if (lecterns != 2) findings.add("v4 requires exactly two purpose-specific lecterns actual=" + lecterns);
        Set<String> formats = new LinkedHashSet<>();
        evidence.values().forEach(surface -> formats.add(surface.format()));
        Set<String> exactFormats = Set.of(
                "annotated drainage diagram",
                "field tag fixed beside the physical ruts",
                "chalk annotation across the physical construction seam",
                "personal cover letter with office marginalia",
                "official refuge register",
                "posted seven-day supply tally",
                "maintenance gauge card beside the working hydraulic train",
                "personal engineering letter retained with works minutes");
        if (!formats.equals(exactFormats)) {
            findings.add("authored evidence format inventory drift expected=" + exactFormats + " actual=" + formats);
        }
        long nativeBooks = evidence.values().stream()
                .filter(surface -> surface.presentation() == Presentation.NATIVE_BOOK).count();
        long environmental = evidence.size() - nativeBooks;
        if (nativeBooks != 4 || environmental != 4) {
            findings.add("evidence medium balance drift native_books=" + nativeBooks + " environmental=" + environmental);
        }
    }

    private void checkWaterworks(List<String> findings, int waterCount) {
        Map<String, Integer> expectedSizes = Map.of(
                "INLET_TROUGH", 8, "SETTLING_BASIN", 30, "GAUGING_FLUME", 16, "GRATED_SUMP", 8);
        for (Map.Entry<String, Integer> row : expectedSizes.entrySet()) {
            DecorativeCluster cluster = clusters.get(row.getKey());
            if (cluster == null || cluster.cells().size() != row.getValue()
                    || cluster.cells().stream().anyMatch(cell -> expected.get(cell) != Material.WATER)) {
                findings.add("waterworks component drift " + row.getKey());
            }
        }
        if (waterCount != 62) findings.add("waterworks exact capacity path expected 62 cells actual=" + waterCount);
        Set<Cell> classifiedWater = new LinkedHashSet<>();
        expectedSizes.keySet().forEach(id -> {
            DecorativeCluster cluster = clusters.get(id);
            if (cluster != null) classifiedWater.addAll(cluster.cells());
        });
        long allWater = expected.entrySet().stream().filter(row -> row.getValue() == Material.WATER).count();
        if (classifiedWater.size() != allWater) findings.add("unclassified water or orphan hydraulic component");
    }

    private void checkCorridor(List<String> findings, int minX, int maxX, int minZ, int maxZ, String label) {
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
            for (int y = -20; y <= -16; y++) if (!block(new Cell(x, y, z)).getType().isAir()) {
                findings.add(label + " blocked at " + new Cell(x, y, z));
                return;
            }
    }

    private void checkImmersiveText(List<String> findings) {
        List<String> banned = List.of("this is the entrance", "this is the exit", "same public route",
                "no side exit", "keep aisle clear", "file:", "right-click", "crouch-use",
                "solve the puzzle", "submit here", "mysterious", "ancient secret");
        List<String> text = new ArrayList<>();
        evidence.values().forEach(surface -> text.add(surface.author() + "\n" + surface.format()
                + "\n" + surface.title() + "\n" + surface.body()));
        references.values().forEach(surface -> text.add(surface.title() + "\n" + surface.body()));
        signs.values().forEach(surface -> text.add(String.join(" ", surface.lines())));
        for (String value : text) for (String phrase : banned) if (value.toLowerCase().contains(phrase))
            findings.add("immersion-breaking authored phrase retained: " + phrase);
        if (new LinkedHashSet<>(text).size() != text.size()) findings.add("duplicated authored evidence/sign text");
    }

    private static boolean facesReader(Cell surface, Cell reader, String data) {
        if (data.contains("facing=north")) return reader.z < surface.z;
        if (data.contains("facing=south")) return reader.z > surface.z;
        if (data.contains("facing=east")) return reader.x > surface.x;
        if (data.contains("facing=west")) return reader.x < surface.x;
        return true;
    }

    private static boolean requiresSupport(Material material) {
        return material == Material.LECTERN || material == Material.CHISELED_BOOKSHELF
                || material == Material.BOOKSHELF || material == Material.LANTERN
                || material == Material.OAK_WALL_SIGN || material == Material.DARK_OAK_SLAB
                || material == Material.CARTOGRAPHY_TABLE || material == Material.HEAVY_WEIGHTED_PRESSURE_PLATE
                || material.name().endsWith("_STAIRS") || material.name().endsWith("_CARPET");
    }

    private static String lecternData(String facing) {
        return "minecraft:lectern[facing=" + facing + ",has_book=true,powered=false]";
    }

    private static List<Component> bookPages(String body) {
        List<Component> pages = new ArrayList<>();
        for (String page : body.split("\\f", -1)) {
            if (page.length() > 238) throw new IllegalArgumentException("authored book page exceeds 238 characters");
            pages.add(Component.text(page));
        }
        return List.copyOf(pages);
    }

    private static int wordCount(String text) {
        return text.isBlank() ? 0 : text.trim().split("\\s+").length;
    }

    private void room(int minX, int maxX, int floor, int ceiling, int minZ, int maxZ,
            Material floorMaterial, Material wallMaterial, Material ceilingMaterial) {
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            expected.put(new Cell(x, floor - 1, z), floorMaterial);
            boolean perimeter = x == minX || x == maxX || z == minZ || z == maxZ;
            for (int y = floor; y <= ceiling; y++) {
                expected.put(new Cell(x, y, z), y == ceiling ? ceilingMaterial : perimeter ? wallMaterial : Material.AIR);
            }
        }
    }

    private void column(int x, int floor, int z, int headroom, Material floorMaterial) {
        expected.put(new Cell(x, floor - 1, z), floorMaterial);
        for (int y = floor; y < floor + headroom; y++) expected.put(new Cell(x, y, z), Material.AIR);
    }

    private void door(int x, int floor, int z, int height, Material floorMaterial) {
        expected.put(new Cell(x, floor - 1, z), floorMaterial);
        for (int y = floor; y < floor + height; y++) expected.put(new Cell(x, y, z), Material.AIR);
    }

    private void doorCells(int[][] cells, int height) {
        for (int[] c : cells) door(c[0], c[1], c[2], height, Material.POLISHED_ANDESITE);
    }

    private void route(int[][] points, int width, Material floorMaterial) {
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
                    column(x + dx, y, z + dz, 5, floorMaterial);
            }
        }
    }

    private void volume(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, Material material) {
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++) expected.put(new Cell(x, y, z), material);
    }

    private void furniture(Material material, int[][] cells) {
        for (int[] c : cells) expected.put(new Cell(c[0], c[1], c[2]), material);
    }

    private Cell relative(Location location) {
        return new Cell(location.getBlockX() - originX, location.getBlockY() - originY,
                location.getBlockZ() - originZ);
    }

    private Block block(Cell cell) {
        return world.getBlockAt(originX + cell.x, originY + cell.y, originZ + cell.z);
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) out.append(String.format("%02x", value & 0xff));
        return out.toString();
    }

    public record EvidenceSurface(Cell cell, Cell readerCell, String surfaceId, String findingId,
            String sourceId, String author, String format, String title, String body,
            Material physicalMaterial, Presentation presentation) {
        String canonical() { return String.join("|", cell.toString(), readerCell.toString(), surfaceId,
                findingId, sourceId, author, format, title, body, physicalMaterial.name(), presentation.name()); }
    }

    public record SubmissionSurface(Cell cell, Cell readerCell, String surfaceId, String label) {
        EvidenceSurface asEvidence() {
            String body = "EXAMINER'S FINDINGS\nMark one supported clause under each heading. The whole report is examined together; returned papers are not marked by section.";
            return new EvidenceSurface(cell, readerCell, surfaceId, "REPORT", "submission",
                    "Mara Venn", "bound findings ledger", label, body, Material.LECTERN,
                    Presentation.NATIVE_BOOK);
        }
        String canonical() { return String.join("|", cell.toString(), readerCell.toString(), surfaceId,
                label); }
    }

    public record ReferenceSurface(Cell cell, Cell readerCell, String surfaceId, String author, String format,
            String title, String body, Material physicalMaterial, Presentation presentation, String kind) {
        EvidenceSurface asEvidence() {
            return new EvidenceSurface(cell, readerCell, surfaceId, "REFERENCE", kind, author, format,
                    title, body, physicalMaterial, presentation);
        }
        String canonical() { return String.join("|", cell.toString(), readerCell.toString(), surfaceId,
                author, format, title, body, physicalMaterial.name(), presentation.name(), kind); }
    }

    public enum Presentation { NATIVE_BOOK, VISIBLE_ENVIRONMENTAL_RECORD }

    private record SignSurface(Cell cell, Cell supportCell, Cell readerCell, String surfaceId,
            String purpose, List<String> lines) {
        String canonical() { return String.join("|", cell.toString(), supportCell.toString(), readerCell.toString(),
                surfaceId, purpose, String.join("/", lines)); }
    }

    private record SupportSpec(Cell supportCell, String purpose) {
        String canonical() { return supportCell + "|" + purpose; }
    }

    private record DecorativeCluster(String clusterId, String purpose, Set<Cell> cells) {
        String canonical() { return clusterId + "|" + purpose + "|" + cells.stream()
                .sorted(Comparator.comparingInt(Cell::x).thenComparingInt(Cell::y).thenComparingInt(Cell::z))
                .map(Cell::toString).toList(); }
    }

    private record Choice(String label, String id) { }

    public record Audit(int cellsChecked, List<String> findings, String worldHash,
            Map<Material, Integer> materialCounts, int evidenceSurfaceCount, int submissionSurfaceCount,
            int referenceSurfaceCount, int thresholdSignCount, int supportCount, int clusterCount,
            int gateCollisionCells) {
        public boolean pass() { return findings.isEmpty(); }
        public String compositionSummary() {
            return "evidence=" + evidenceSurfaceCount + " submissions=" + submissionSurfaceCount
                    + " references=" + referenceSurfaceCount
                    + " signs=" + thresholdSignCount + " water=" + materialCounts.getOrDefault(Material.WATER, 0)
                    + " bookshelves=" + (materialCounts.getOrDefault(Material.CHISELED_BOOKSHELF, 0)
                    + materialCounts.getOrDefault(Material.BOOKSHELF, 0)) + " supports=" + supportCount
                    + " clusters=" + clusterCount + " gate_collision=" + gateCollisionCells;
        }
    }

    private record Cell(int x, int y, int z) { }
}
