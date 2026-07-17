package com.observance.watcher.m3runtime;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
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

/** Exact Paper projection of the authored M3 v3 private slice. */
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
            if (!lecternMatches(row.getKey(), row.getValue())) {
                findings.add(row.getKey() + " authored evidence book missing/drifted: " + row.getValue().surfaceId());
            }
            if (wordCount(row.getValue().body()) > 45) findings.add(row.getValue().surfaceId() + " evidence prose too long");
        }
        for (Map.Entry<Cell, SubmissionSurface> row : submissions.entrySet()) {
            if (!lecternMatches(row.getKey(), row.getValue().asEvidence())) {
                findings.add(row.getKey() + " filing/readback surface missing/drifted: " + row.getValue().surfaceId());
            }
        }
        for (Map.Entry<Cell, ReferenceSurface> row : references.entrySet()) {
            if (!lecternMatches(row.getKey(), row.getValue().asEvidence())) {
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
            block(cell).setType(material, false);
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
        publicFilingCounter();
        referenceSurface();
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
            clusterBlock("SUMP_GRATE", new Cell(x, -20, 78), Material.COPPER_GRATE,
                    "full-width grate marks transfer to the lower filter");
        for (int z : new int[]{63, 67, 75}) for (int x = -12; x <= -11; x++)
            clusterBlock("FLOW_BAFFLES", new Cell(x, -20, z), Material.COPPER_GRATE,
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
        expected.put(new Cell(29, -20, 65), Material.DARK_OAK_PLANKS);
        cluster("SURVEY_READING_STAND", new Cell(29, -20, 65),
                "support for the survey-revision lectern");
    }

    private void publicFilingCounter() {
        for (int x = -6; x <= -1; x++) expected.put(new Cell(x, -20, 76), Material.POLISHED_ANDESITE);
        for (int x = 1; x <= 6; x++) expected.put(new Cell(x, -20, 76), Material.POLISHED_ANDESITE);
        for (int x : new int[]{-6,-5,-4,-3,3,4,5,6}) expected.put(new Cell(x, -19, 76), Material.WHITE_CARPET);
        for (int x : new int[]{-6,-5,-4,-3,3,4,5,6}) expected.put(new Cell(x, -20, 59), Material.DARK_OAK_PLANKS);
        expected.put(new Cell(0, -20, 77), Material.DARK_OAK_PLANKS);
        cluster("SYNTHESIS_PLINTH", new Cell(0, -20, 77), "support for the final intake seal docket");
        expected.put(new Cell(-6, -20, 72), Material.DARK_OAK_PLANKS);
        cluster("PUMP_LOG_STAND", new Cell(-6, -20, 72), "support for the heat and pump log");
    }

    private void referenceSurface() {
        addReference(0, 0, 5, "south", new Cell(0, 0, 7), "INTAKE_EXAMINER_DOCKET",
                "Intake Examiner's Docket",
                "WORK ORDER 14\nThe refuge register and works plan disagree.\f"
                + "Reconcile four entries before the Commons seal can be released: public approach; works campaigns; safe berths; reason for descent.\f"
                + "Read the surviving records. At Intake, open each brass filing docket. Crouch and use it to stamp a finding after two records agree.");
    }

    private void evidenceSurfaces() {
        addEvidence(-8,0,7,"south",new Cell(-8,0,9),"DRAINAGE_MAP","P4.F1","drainage_map",
                "Drainage Survey 14-A", "The quarry and well drains end outside this Mouth. One graded intake road crosses the threshold; its return lane shares the same crown.");
        addEvidence(8,0,7,"south",new Cell(8,0,9),"CART_WEAR_LOG","P4.F1","cart_wear",
                "Cart Inspector's Log", "Loaded carts descended on the east rut. Empty carts climbed the west rut. The clerk found no second public approach.");
        addEvidence(-5,-20,59,"south",new Cell(-5,-20,61),"CIVIC_JOIN_SURVEY","P4.F2","material_join_civic",
                "Works Joint Survey", "Rough tuff ends first. Dressed civic stone keys into it. Deep tile braces cut around both. The joints record three separate campaigns.");
        addEvidence(29,-19,65,"south",new Cell(29,-20,67),"SURVEY_REVISIONS","P4.F2","survey_revisions",
                "Survey Revisions A-C", "A: storm shelter only. B: intake and record office. C: Commons spine below the controlled seal.");
        addEvidence(8,-20,59,"south",new Cell(8,-20,61),"POPULATION_BOARD","P4.F3","population_board",
                "Refuge Intake Board", "Sleeping register: 286. Work berths: 42, counted separately. Intake officers must not add the two figures.");
        addEvidence(24,-19,66,"south",new Cell(24,-20,68),"RATION_LEDGER","P4.F3","ration_ledger",
                "Seven-Day Ration Abstract", "Grain, lamp oil, and boiler draw support 294 full refuge shares. The water gauge is rated for 300.");
        addEvidence(-6,-19,72,"east",new Cell(-3,-20,72),"DESCENT_HEAT_LOG","P4.F4","descent_heat_marks",
                "Heat and Pump Log", "Below the civic landing, wall heat steadies and pump lift shortens. A lateral gallery would remain colder and farther from the sump.");
        addEvidence(24,-19,74,"south",new Cell(24,-20,77),"FOUNDING_MINUTES","P4.F4","founding_minutes",
                "Founding Works Minutes", "The committee chose the down-cut: stable cover, shorter winter haul, and direct service beside the water line.");
    }

    private void submissionSurfaces() {
        addSubmission(-4,-19,76,"north",new Cell(-4,-20,73),"FILE_PUBLIC_MOUTH","P4.F1","Mouth Finding Docket");
        addSubmission(-2,-19,76,"north",new Cell(-2,-20,73),"FILE_BUILD_PHASES","P4.F2","Works Finding Docket");
        addSubmission(2,-19,76,"north",new Cell(2,-20,73),"FILE_CAPACITY","P4.F3","Capacity Finding Docket");
        addSubmission(4,-19,76,"north",new Cell(4,-20,73),"FILE_DESCENT","P4.F4","Descent Finding Docket");
        addSubmission(0,-19,77,"north",new Cell(0,-20,74),"FILE_INTAKE_SYNTHESIS","P4.F5","Intake Seal Docket");
        addSubmission(13,-20,74,"west",new Cell(10,-20,74),"FIELD_ARCHIVE_READBACK",null,"Field Archive Index");
    }

    private void thresholdSigns() {
        addSign(0,2,1,"south",new Cell(0,2,0),new Cell(0,0,3),"MOUTH_COMMISSION_PLAQUE",
                "municipal commission identity","INTAKE WORKS","COMMISSION 14","EXAMINER DUE","");
        addSign(17,-18,66,"west",new Cell(18,-18,66),new Cell(15,-20,66),"RECORD_OFFICE_PLAQUE",
                "credible office identity","RECORD OFFICE","COPIES + SURVEYS","BELL AT DESK","");
        addSign(-6,-18,88,"north",new Cell(-6,-18,89),new Cell(-5,-20,85),"COMMONS_SEAL_PLAQUE",
                "controlled civic seal identity","COMMONS SEAL","WICKET No. 3","CLERK RELEASE","");
        addSign(-14,-18,61,"north",new Cell(-14,-18,62),new Cell(-14,-20,59),"RUNOFF_GAUGE_PLAQUE",
                "works gauge identity and rated capacity","RUNOFF WORKS","GAUGE No. 7","300 BERTH MAX","");
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
        for (int x = -5; x <= 5; x++) for (int y = -20; y <= -13; y++)
            expected.put(new Cell(x, y, 89), gateOpen ? Material.AIR : Material.COPPER_GRATE);
        expected.put(new Cell(-6, -16, 88), Material.REDSTONE_LAMP);
        expected.put(new Cell(6, -16, 88), Material.REDSTONE_LAMP);
    }

    private void decorateSurfaces() {
        evidence.forEach(this::writeBook);
        submissions.forEach((cell, surface) -> writeBook(cell, surface.asEvidence()));
        references.forEach((cell, surface) -> writeBook(cell, surface.asEvidence()));
        signs.forEach(this::writeSign);
    }

    private void writeBook(Cell cell, EvidenceSurface surface) {
        Block block = block(cell);
        if (!(block.getState() instanceof Lectern lectern)) return;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(surface.title());
        meta.setAuthor("Intake Clerk's Office");
        meta.pages(bookPages(surface.body()));
        book.setItemMeta(meta);
        // LecternInventory is live. Updating the earlier block-state snapshot after this
        // write replaces the inventory with that snapshot's empty contents on Paper.
        lectern.getInventory().setItem(0, book);
    }

    private boolean lecternMatches(Cell cell, EvidenceSurface surface) {
        if (!(block(cell).getState() instanceof Lectern lectern)) return false;
        ItemStack item = lectern.getInventory().getItem(0);
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !(item.getItemMeta() instanceof BookMeta meta))
            return false;
        return surface.title().equals(meta.getTitle()) && bookPages(surface.body()).equals(meta.pages());
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

    private void addEvidence(int x, int y, int z, String facing, Cell readerCell, String surfaceId,
            String findingId, String sourceId, String title, String body) {
        Cell cell = new Cell(x, y, z);
        directional(cell, Material.LECTERN, lecternData(facing));
        support(cell, new Cell(x, y - 1, z), "evidence lectern support");
        evidence.put(cell, new EvidenceSurface(cell, readerCell, surfaceId, findingId, sourceId, title, body));
    }

    private void addSubmission(int x, int y, int z, String facing, Cell readerCell,
            String surfaceId, String findingId, String label) {
        Cell cell = new Cell(x, y, z);
        directional(cell, Material.LECTERN, lecternData(facing));
        support(cell, new Cell(x, y - 1, z), "filing lectern support");
        submissions.put(cell, new SubmissionSurface(cell, readerCell, surfaceId, findingId, label));
    }

    private void addReference(int x, int y, int z, String facing, Cell readerCell,
            String surfaceId, String title, String body) {
        Cell cell = new Cell(x, y, z);
        directional(cell, Material.LECTERN, lecternData(facing));
        support(cell, new Cell(x, y - 1, z), "examiner docket support");
        references.put(cell, new ReferenceSurface(cell, readerCell, surfaceId, title, body));
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
        if (references.size() != 1 || submissions.size() != 6) {
            findings.add("investigation briefing/filing topology drift references=" + references.size()
                    + " submissions=" + submissions.size());
        }
        if (evidence.values().stream().anyMatch(surface -> expected.get(surface.cell()) != Material.LECTERN)) {
            findings.add("chat-only or non-book evidence surface present");
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
                "no side exit", "keep aisle clear", "file:");
        List<String> text = new ArrayList<>();
        evidence.values().forEach(surface -> text.add(surface.title() + "\n" + surface.body()));
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
                || material == Material.OAK_WALL_SIGN || material == Material.DARK_OAK_SLAB;
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
            String sourceId, String title, String body) {
        String canonical() { return String.join("|", cell.toString(), readerCell.toString(), surfaceId,
                findingId, sourceId, title, body); }
    }

    public record SubmissionSurface(Cell cell, Cell readerCell, String surfaceId, String findingId, String label) {
        EvidenceSurface asEvidence() {
            String body = findingId == null
                    ? "ARCHIVE INDEX\nOpen records are retained here. Crouch and use this index for a concise progress readback."
                    : "FINDING DOCKET\nRead the paired records for this question. Crouch and use this docket to stamp the clerk's finding.";
            return new EvidenceSurface(cell, readerCell, surfaceId, findingId == null ? "READBACK" : findingId,
                    "submission", label, body);
        }
        String canonical() { return String.join("|", cell.toString(), readerCell.toString(), surfaceId,
                findingId == null ? "READBACK" : findingId, label); }
    }

    public record ReferenceSurface(Cell cell, Cell readerCell, String surfaceId, String title, String body) {
        EvidenceSurface asEvidence() {
            return new EvidenceSurface(cell, readerCell, surfaceId, "REFERENCE", "briefing", title, body);
        }
        String canonical() { return String.join("|", cell.toString(), readerCell.toString(), surfaceId, title, body); }
    }

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
