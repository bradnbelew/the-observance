package com.observance.watcher.m3runtime;

import net.kyori.adventure.text.Component;
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
import java.util.List;
import java.util.Map;

/** Exact Paper projection of the authored M3 v2 private slice. */
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
    private final Map<Cell, EvidenceSurface> evidence = new LinkedHashMap<>();
    private final Map<Cell, SubmissionSurface> submissions = new LinkedHashMap<>();
    private final Map<Cell, List<String>> signs = new LinkedHashMap<>();

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
        for (Map.Entry<Cell, EvidenceSurface> row : evidence.entrySet()) {
            if (row.getValue().material() == Material.LECTERN && !lecternMatches(row.getKey(), row.getValue())) {
                findings.add(row.getKey() + " authored evidence book missing/drifted: " + row.getValue().surfaceId());
            }
        }
        for (Map.Entry<Cell, SubmissionSurface> row : submissions.entrySet()) {
            if (!lecternMatches(row.getKey(), row.getValue().asEvidence())) {
                findings.add(row.getKey() + " filing/readback surface missing/drifted: " + row.getValue().surfaceId());
            }
        }
        for (Map.Entry<Cell, List<String>> row : signs.entrySet()) {
            if (!signMatches(row.getKey(), row.getValue())) findings.add(row.getKey() + " threshold sign drift");
        }
        int gateCollision = gateCollisionCells();
        return new Audit(expected.size(), List.copyOf(findings), worldHash(), Map.copyOf(counts),
                evidence.size(), submissions.size(), signs.size(), gateCollision);
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
                            + block(cell).getType().getKey() + "\n").getBytes(StandardCharsets.UTF_8)));
            evidence.values().forEach(surface -> digest.update((surface.canonical() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            submissions.values().forEach(surface -> digest.update((surface.canonical() + "\n")
                    .getBytes(StandardCharsets.UTF_8)));
            signs.forEach((cell, lines) -> digest.update((cell + "=" + String.join("/", lines) + "\n")
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
            if ((z - 16) % 6 == 0) {
                for (int y = floor; y < floor + 5; y++) {
                    expected.put(new Cell(-4, y, z), Material.POLISHED_BASALT);
                    expected.put(new Cell(4, y, z), Material.POLISHED_BASALT);
                }
                expected.put(new Cell(0, floor + 4, z), Material.LANTERN);
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
        doorCells(new int[][]{{12,-20,78},{13,-20,78},{14,-20,78},{15,-20,78},{16,-20,78}}, 4);
        doorCells(new int[][]{{17,-20,80},{17,-20,81},{17,-20,82},{18,-20,80},{18,-20,81},
                {18,-20,82}}, 4);
        route(new int[][]{{12,-20,76},{14,-20,78},{14,-20,81},{18,-20,81},{21,-20,81},
                {27,-20,81}}, 2, Material.CUT_COPPER);
        route(new int[][]{{0,-20,70},{12,-20,70},{18,-20,70},{21,-20,70},{26,-20,70},
                {27,-20,70}}, 3, Material.POLISHED_ANDESITE);
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
            expected.put(new Cell(-10, 8, z), Material.LANTERN);
            expected.put(new Cell(10, 8, z), Material.LANTERN);
        }
        for (int x : new int[]{-12, -6, 0, 6, 12}) {
            for (int y = -20; y <= -11; y++) {
                expected.put(new Cell(x, y, 56), Material.DEEPSLATE_TILE_WALL);
                expected.put(new Cell(x, y, 78), Material.DEEPSLATE_TILE_WALL);
            }
        }
        for (int z : new int[]{59, 65, 71, 76}) {
            expected.put(new Cell(-14, -11, z), Material.LANTERN);
            expected.put(new Cell(14, -11, z), Material.LANTERN);
        }
        for (int z : new int[]{64, 69, 74, 82}) {
            expected.put(new Cell(20, -13, z), Material.LANTERN);
            expected.put(new Cell(30, -13, z), Material.LANTERN);
        }
        for (int x = -5; x <= 5; x++) {
            expected.put(new Cell(x, -21, 79), Material.WAXED_WEATHERED_CUT_COPPER);
            expected.put(new Cell(x, -21, 88), Material.WAXED_WEATHERED_CUT_COPPER);
        }
    }

    private void waterworks() {
        volume(-14, -9, -21, -21, 63, 67, Material.WATER);
        volume(-12, -11, -21, -21, 68, 75, Material.WATER);
        volume(-14, -9, -21, -21, 76, 77, Material.WATER);
        for (int z = 62; z <= 78; z++) {
            expected.put(new Cell(-15, -20, z), Material.WEATHERED_CUT_COPPER);
            expected.put(new Cell(-8, -20, z), Material.WEATHERED_CUT_COPPER);
        }
        for (int x = -14; x <= -9; x++) {
            expected.put(new Cell(x, -20, 62), Material.WEATHERED_CUT_COPPER);
            expected.put(new Cell(x, -20, 78), Material.COPPER_GRATE);
        }
        for (int z : new int[]{64, 66, 72, 76}) {
            expected.put(new Cell(-12, -20, z), Material.COPPER_GRATE);
            expected.put(new Cell(-11, -20, z), Material.COPPER_GRATE);
        }
        expected.put(new Cell(-8, -18, 70), Material.WAXED_WEATHERED_CUT_COPPER);
    }

    private void copyOffice() {
        furniture(Material.DARK_OAK_PLANKS, new int[][]{{22,-20,65},{23,-20,65},{24,-20,65},
                {25,-20,65},{26,-20,65},{22,-20,66},{23,-20,66},{24,-20,66},{25,-20,66},
                {26,-20,66},{22,-20,74},{23,-20,74},{24,-20,74},{25,-20,74},{26,-20,74},
                {22,-20,75},{23,-20,75},{24,-20,75},{25,-20,75},{26,-20,75}});
        furniture(Material.SPRUCE_PLANKS, new int[][]{{22,-20,82},{23,-20,82},{24,-20,82},
                {25,-20,82},{26,-20,82}});
        furniture(Material.POLISHED_ANDESITE, new int[][]{{28,-20,68},{29,-20,68},{30,-20,68},
                {28,-20,69},{30,-20,69},{28,-20,70},{30,-20,70},{28,-20,71},{30,-20,71},
                {28,-20,72},{29,-20,72},{30,-20,72}});
        volume(31, 31, -20, -17, 63, 67, Material.CHISELED_BOOKSHELF);
        volume(31, 31, -20, -17, 78, 83, Material.CHISELED_BOOKSHELF);
        for (int x = 22; x <= 26; x++) {
            expected.put(new Cell(x, -19, 65), Material.WHITE_CARPET);
            expected.put(new Cell(x, -19, 74), Material.WHITE_CARPET);
        }
        for (int x = 23; x <= 25; x++) expected.put(new Cell(x, -19, 82), Material.WHITE_CARPET);
        for (int z : new int[]{67, 76}) {
            expected.put(new Cell(23, -20, z), Material.SPRUCE_STAIRS);
            expected.put(new Cell(25, -20, z), Material.SPRUCE_STAIRS);
        }
        for (int z : new int[]{64, 67, 79, 82}) expected.put(new Cell(29, -19, z), Material.BOOKSHELF);
        expected.put(new Cell(31, -18, 70), Material.WAXED_OXIDIZED_CUT_COPPER);
    }

    private void publicFilingCounter() {
        for (int x = -6; x <= -1; x++) expected.put(new Cell(x, -20, 76), Material.POLISHED_ANDESITE);
        for (int x = 1; x <= 6; x++) expected.put(new Cell(x, -20, 76), Material.POLISHED_ANDESITE);
        for (int x : new int[]{-6,-5,-4,-3,3,4,5,6}) expected.put(new Cell(x, -19, 76), Material.WHITE_CARPET);
        for (int x : new int[]{-6,-5,-4,-3,3,4,5,6}) expected.put(new Cell(x, -20, 59), Material.DARK_OAK_PLANKS);
    }

    private void evidenceSurfaces() {
        addEvidence(-8,0,7, Material.LECTERN, "DRAINAGE_MAP", "P4.F1", "drainage_map",
                "Public Works Drainage Survey", "Runoff arrows terminate at this threshold. The well and quarry drains are separately keyed; neither enters the refuge.");
        addEvidence(0,0,13, Material.POLISHED_ANDESITE, "CART_WEAR", "P4.F1", "cart_wear",
                "Threshold Wear", "Paired wheel scars descend and return on the same graded route. No second public haul line branches from the landing.");
        addEvidence(8,0,7, Material.LECTERN, "RESIDENT_MEMORY", "P4.F1", "resident_memory_copy",
                "Mira Vale, copied interview", "Families queued under the rain shed. Empty carts came back by the same stair after intake clerks counted them.");
        addEvidence(-4,-2,22, Material.CHISELED_TUFF, "SHELTER_JOIN", "P4.F2", "material_join_shelter",
                "First shelter cut", "Rough tuff courses stop at the original storm-shelter width; later ribs are keyed into their faces.");
        addEvidence(-4,-11,38, Material.CHISELED_STONE_BRICKS, "CIVIC_JOIN", "P4.F2", "material_join_civic",
                "Civic widening", "Dressed stone widens the route and adds cart recesses after the first shelter was already in use.");
        addEvidence(-4,-18,52, Material.CHISELED_DEEPSLATE, "DEEP_JOIN", "P4.F2", "material_join_deep",
                "Deep works tie-in", "Tile and basalt braces continue below the civic stair, cut around both earlier campaigns rather than beneath one plan.");
        addEvidence(-13,-20,59, Material.LECTERN, "POPULATION_BOARD", "P4.F3", "population_board",
                "Refuge intake board", "Household marks total 286 residents. Forty-two work berths are recorded separately from sleeping capacity.");
        addEvidence(-13,-20,72, Material.LECTERN, "HEAT_WATER_DIAGRAM", "P4.F3", "heat_water_capacity_diagram",
                "Heat and water capacity", "Two boilers cover 320 rationed berths. The flume limits intake to 300 until the lower filter bed opens.");
        addEvidence(-8,-18,70, Material.WAXED_WEATHERED_CUT_COPPER, "WATER_CHANNEL_GAUGE", "P4.F4", "recessed_water_channel",
                "Drainage capacity gauge", "Basin depth, flume width, and sump marks let clerks reject arrivals the drains and filters could not serve.");
        addEvidence(22,-20,66, Material.LECTERN, "RATION_LEDGER", "P4.F3", "ration_ledger",
                "Seven-day ration abstract", "Issued grain and lamp oil support 294 full shares. Surface work crews are not counted as refuge mouths.");
        addEvidence(31,-18,65, Material.CHISELED_BOOKSHELF, "SURVEY_REVISIONS", "P4.F2", "survey_revisions",
                "Survey revisions A-C", "A ends at shelter. B adds intake and copy office. C extends the spine beyond the controlled gate.");
        addEvidence(31,-18,80, Material.CHISELED_BOOKSHELF, "ROOM_USE_ROLL", "P4.F2", "room_use_roll",
                "Room-use roll", "The copy office begins after intake expansion and before deep crews receive permanent quarters.");
        addEvidence(22,-20,74, Material.LECTERN, "FOUNDING_MINUTES", "P4.F4", "founding_minutes",
                "Founding works minutes", "The committee rejects exposed lateral galleries: down-cut cover shortens winter travel and reaches stable heat beside water.");
        addEvidence(4,-18,52, Material.POLISHED_BASALT, "DESCENT_HEAT_MARKS", "P4.F4", "descent_heat_marks",
                "Heat survey marks", "Measured wall temperature steadies before intake level. Fuel-haul estimates fall at the protected depth.");
    }

    private void submissionSurfaces() {
        addSubmission(-4,-19,76, "FILE_PUBLIC_MOUTH", "P4.F1", "FILE: PUBLIC MOUTH");
        addSubmission(-2,-19,76, "FILE_BUILD_PHASES", "P4.F2", "FILE: BUILD PHASES");
        addSubmission(2,-19,76, "FILE_CAPACITY", "P4.F3", "FILE: CAPACITY");
        addSubmission(4,-19,76, "FILE_DESCENT", "P4.F4", "FILE: DESCENT MOTIVE");
        addSubmission(0,-19,77, "FILE_INTAKE_SYNTHESIS", "P4.F5", "FILE: INTAKE FINDING");
        addSubmission(13,-20,74, "FIELD_ARCHIVE_READBACK", null, "FIELD ARCHIVE / REPLAY");
    }

    private void thresholdSigns() {
        addSign(0,2,0, "PUBLIC WORKS", "REFUGE INTAKE", "DESCENT + RETURN", "KEEP AISLE CLEAR");
        addSign(0,2,15, "RETURN", "TO SURFACE", "SAME PUBLIC ROUTE", "NO SIDE EXIT");
        addSign(18,-15,70, "PUBLIC", "COPY OFFICE", "READING AISLE", "");
        addSign(18,-15,81, "STAFF RECORDS", "CART ROUTE", "RETURNS TO INTAKE", "");
        addSign(-6,-17,88, "CONTROLLED GATE", "INTAKE FINDING", "REQUIRED", "COMMONS BEYOND");
        addSign(0,-17,92, "REFUGE COMMONS", "FUTURE WORK", "SEALED HERE", "NO SIDE BYPASS");
        addSign(-16,-17,63, "RUNOFF INLET", "SETTLING BASIN", "CLEAR SILT", "BEFORE GAUGE");
        addSign(-16,-17,77, "GAUGED OUTFLOW", "LOWER FILTER", "CAPACITY LIMIT", "300 BERTHS");
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
        evidence.forEach((cell, surface) -> { if (surface.material() == Material.LECTERN) writeBook(cell, surface); });
        submissions.forEach((cell, surface) -> writeBook(cell, surface.asEvidence()));
        signs.forEach(this::writeSign);
    }

    private void writeBook(Cell cell, EvidenceSurface surface) {
        Block block = block(cell);
        if (!(block.getState() instanceof Lectern lectern)) return;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(surface.title());
        meta.setAuthor("Intake Clerk's Office");
        meta.pages(List.of(Component.text(surface.body())));
        book.setItemMeta(meta);
        lectern.getInventory().setItem(0, book);
        lectern.update(true, false);
    }

    private boolean lecternMatches(Cell cell, EvidenceSurface surface) {
        if (!(block(cell).getState() instanceof Lectern lectern)) return false;
        ItemStack item = lectern.getInventory().getItem(0);
        if (item == null || item.getType() != Material.WRITTEN_BOOK || !(item.getItemMeta() instanceof BookMeta meta))
            return false;
        return surface.title().equals(meta.getTitle()) && !meta.pages().isEmpty()
                && Component.text(surface.body()).equals(meta.pages().get(0));
    }

    private void writeSign(Cell cell, List<String> lines) {
        if (!(block(cell).getState() instanceof Sign sign)) return;
        for (int i = 0; i < 4; i++) sign.getSide(Side.FRONT).line(i, Component.text(lines.get(i)));
        sign.update(true, false);
    }

    private boolean signMatches(Cell cell, List<String> lines) {
        if (!(block(cell).getState() instanceof Sign sign)) return false;
        for (int i = 0; i < 4; i++) if (!Component.text(lines.get(i)).equals(sign.getSide(Side.FRONT).line(i))) return false;
        return true;
    }

    private void addEvidence(int x, int y, int z, Material material, String surfaceId, String findingId,
            String sourceId, String title, String body) {
        Cell cell = new Cell(x, y, z);
        expected.put(cell, material);
        evidence.put(cell, new EvidenceSurface(surfaceId, findingId, sourceId, material, title, body));
    }

    private void addSubmission(int x, int y, int z, String surfaceId, String findingId, String label) {
        Cell cell = new Cell(x, y, z);
        expected.put(cell, Material.LECTERN);
        submissions.put(cell, new SubmissionSurface(surfaceId, findingId, label));
    }

    private void addSign(int x, int y, int z, String... lines) {
        Cell cell = new Cell(x, y, z);
        expected.put(cell, Material.OAK_WALL_SIGN);
        signs.put(cell, List.of(lines));
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

    public record EvidenceSurface(String surfaceId, String findingId, String sourceId, Material material,
            String title, String body) {
        String canonical() { return String.join("|", surfaceId, findingId, sourceId, material.name(), title, body); }
    }

    public record SubmissionSurface(String surfaceId, String findingId, String label) {
        EvidenceSurface asEvidence() {
            String body = findingId == null
                    ? "Replay committed observations, findings, changed places, remaining dispute, and both accessibility descriptions."
                    : "File " + findingId + " only after inspecting its authored independent sources.";
            return new EvidenceSurface(surfaceId, findingId == null ? "READBACK" : findingId,
                    "submission", Material.LECTERN, label, body);
        }
        String canonical() { return String.join("|", surfaceId, findingId == null ? "READBACK" : findingId, label); }
    }

    public record Audit(int cellsChecked, List<String> findings, String worldHash,
            Map<Material, Integer> materialCounts, int evidenceSurfaceCount, int submissionSurfaceCount,
            int thresholdSignCount, int gateCollisionCells) {
        public boolean pass() { return findings.isEmpty(); }
        public String compositionSummary() {
            return "evidence=" + evidenceSurfaceCount + " submissions=" + submissionSurfaceCount
                    + " signs=" + thresholdSignCount + " water=" + materialCounts.getOrDefault(Material.WATER, 0)
                    + " bookshelves=" + (materialCounts.getOrDefault(Material.CHISELED_BOOKSHELF, 0)
                    + materialCounts.getOrDefault(Material.BOOKSHELF, 0)) + " gate_collision=" + gateCollisionCells;
        }
    }

    private record Cell(int x, int y, int z) { }
}
