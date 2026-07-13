package com.observance.watcher.structure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, dependency-free construction plan for the clean-sheet Deep Hold rebuild.
 *
 * <p>The rejected V3 generator mixed room ownership, fixtures, and corridors in one command class.
 * V4 keeps the spatial contract here so it can be validated without loading Bukkit or changing a
 * world. Coordinates are local to the single public Surface Mouth: X/Z are horizontal offsets and
 * Y is the offset below the sampled mouth surface.</p>
 */
public final class DeepHoldV4Plan {

    public static final int VERSION = 4;
    public static final int MIN_SURFACE_COVER = 12;
    public static final int MIN_BOTTOM_BUFFER = 12;
    public static final int ENVELOPE = 5;
    public static final int MIN_X = -118;
    public static final int MAX_X = 118;
    public static final int MIN_Y = -104;
    public static final int MAX_Y = 12;
    public static final int MIN_Z = -6;
    public static final int MAX_Z = 378;

    private DeepHoldV4Plan() {}

    public record Room(String id, int minX, int maxX, int floorY, int ceilingY,
                       int minZ, int maxZ, String role) {
        public boolean contains(int x, int y, int z, int inset) {
            return x >= minX + inset && x <= maxX - inset
                    && z >= minZ + inset && z <= maxZ - inset
                    && y >= floorY && y <= ceilingY - 1;
        }
    }

    public record Fixture(String id, String type, String roomId,
                          int x, int y, int z, int radius, int verticalRadius,
                          String front, int standX, int standY, int standZ,
                          String contentRole) { }

    public record Gate(String id, String label, int x, int y, int z,
                       boolean acrossX, int halfAcross, int height, int depth,
                       String openCondition, boolean mainSequence) { }

    public record Link(String from, String to) { }

    public record RecordStation(String id, String roomId, int x, int y, int z,
                                String front, String title) { }

    public static final List<Room> ROOMS = List.of(
            new Room("orientation", -42, 42, -40, -20, 106, 154,
                    "literacy, first record, and customs"),
            new Room("keeper_nave", -30, 30, -40, -16, 160, 248,
                    "monumental Keeper investigation court"),
            new Room("keeper_vaun", -98, -34, -40, -22, 160, 188, "audit and sort"),
            new Room("keeper_mara", 34, 98, -40, -22, 160, 192, "compare and walk"),
            new Room("keeper_iss", -98, -34, -40, -22, 192, 220, "compare and accuse"),
            new Room("keeper_sella", 34, 98, -40, -22, 196, 224, "reflect and count"),
            new Room("keeper_brann", -98, -34, -40, -22, 224, 248, "wait and listen"),
            new Room("keeper_orin", 34, 98, -40, -22, 228, 248, "crouch and align"),

            new Room("archive_nave", -30, 30, -68, -50, 102, 300,
                    "orientation landmark and civic evidence spine"),
            new Room("archive_school", -98, -48, -68, -52, 110, 142, "school and copy trail"),
            new Room("archive_markers", -98, -48, -68, -52, 146, 170, "physical chronology"),
            new Room("archive_cistern", -98, -48, -68, -52, 174, 208, "seventh-count water proof"),
            new Room("archive_watch", -98, -48, -68, -52, 212, 242, "dark-hours watch proof"),
            new Room("archive_shelf", -98, -48, -68, -52, 246, 270, "edited-record proof"),
            new Room("archive_water", -102, -44, -68, -50, 274, 300, "far-water reflection"),
            new Room("archive_market", 48, 98, -68, -50, 110, 142, "social keeping and exchange"),
            new Room("archive_ration", 48, 98, -68, -52, 146, 170, "scarcity and debt"),
            new Room("archive_breach", 48, 98, -68, -50, 174, 208, "broken Deep Line"),
            new Room("archive_warm", 48, 102, -68, -50, 212, 242, "physical contradiction of comfort"),
            new Room("archive_stall", 48, 98, -68, -52, 246, 270, "authored failed route"),
            new Room("archive_coops", 48, 98, -68, -52, 274, 300, "preserved witness custom"),
            new Room("puzzle_works", -64, 64, -68, -48, 40, 96,
                    "cross-Keeper mechanics and deep on-ramp"),

            new Room("lower_works", -68, 68, -96, -74, 40, 108,
                    "Reckoning and group-action preparation"),
            new Room("lower_spine", -30, 30, -96, -74, 114, 220,
                    "linear Threshold convergence"),
            new Room("prior_case", -112, -48, -96, -76, 118, 154, "absence case board"),
            new Room("prior_camp", -112, -48, -96, -74, 158, 218, "prior expedition corrections"),
            new Room("lower_threshold", 34, 70, -96, -74, 118, 154, "true walk and grave"),
            new Room("lower_vault", 34, 70, -96, -74, 158, 218, "dynamic-roster vault"),
            new Room("dread", 74, 114, -96, -74, 118, 220, "controlled optional dread branch"),
            new Room("accepting", -58, 58, -96, -74, 226, 292, "group rite at Unbroken Light"),
            new Room("unwriting", -72, 72, -96, -74, 298, 350, "Seventh treatment and final reading"),
            new Room("release", -52, 52, -96, -74, 354, 378, "release receipt and reversible exit"));

    /** All 76 canonical Deep Hold fixture ids, each with one V4 owner and standing frame. */
    public static final List<Fixture> FIXTURES = List.of(
            fixture("undercroft_seal", "undercroft_seal", "orientation", -28, -40, 120, 11, 7, "NORTH", -28, -40, 114, "corroboration"),
            fixture("forgotten_mouth", "forgotten_mouth", "orientation", 28, -40, 120, 11, 7, "NORTH", 28, -40, 114, "corroboration"),
            fixture("rune_rosetta", "structure", "orientation", 0, -40, 120, 8, 7, "NORTH", 0, -40, 114, "required"),
            fixture("bow_marker_01", "bow_marker", "orientation", -28, -40, 142, 4, 4, "NORTH", -28, -40, 137, "tutorial"),
            fixture("offering_cairn_01", "offering_cairn", "orientation", 0, -40, 142, 4, 4, "NORTH", 0, -40, 137, "tutorial"),
            fixture("kept_light_home_01", "kept_light", "orientation", 28, -40, 142, 5, 4, "NORTH", 28, -40, 137, "tutorial"),

            fixture("stone_vaun", "keeper_stone", "keeper_vaun", -82, -40, 176, 8, 7, "EAST", -72, -40, 176, "required"),
            fixture("vaun_hoard_chest", "vaun_hoard_chest", "keeper_vaun", -58, -40, 168, 2, 3, "EAST", -54, -40, 168, "mechanic"),
            fixture("vaun_bookshelf", "vaun_bookshelf", "keeper_vaun", -58, -40, 180, 2, 3, "EAST", -54, -40, 180, "mechanic"),

            fixture("stone_mara", "keeper_stone", "keeper_mara", 82, -40, 176, 8, 7, "WEST", 72, -40, 176, "required"),
            fixture("mara_lectern_1", "mara_lectern", "keeper_mara", 42, -40, 168, 2, 2, "SOUTH", 42, -40, 172, "book"),
            fixture("mara_lectern_2", "mara_lectern", "keeper_mara", 52, -40, 168, 2, 2, "SOUTH", 52, -40, 172, "book"),
            fixture("mara_lectern_3", "mara_lectern", "keeper_mara", 62, -40, 168, 2, 2, "SOUTH", 62, -40, 172, "book"),
            fixture("mara_lectern_4", "mara_lectern", "keeper_mara", 72, -40, 168, 2, 2, "SOUTH", 72, -40, 172, "book"),
            fixture("mara_lectern_5", "mara_lectern", "keeper_mara", 82, -40, 168, 2, 2, "SOUTH", 82, -40, 172, "book"),
            fixture("mara_route_marker_1", "mara_route_marker", "keeper_mara", 42, -40, 184, 2, 3, "NORTH", 42, -40, 180, "mechanic"),
            fixture("mara_route_marker_2", "mara_route_marker", "keeper_mara", 54, -40, 188, 2, 3, "NORTH", 54, -40, 184, "mechanic"),
            fixture("mara_route_marker_3", "mara_route_marker", "keeper_mara", 68, -40, 188, 2, 3, "NORTH", 68, -40, 184, "mechanic"),
            fixture("mara_route_marker_4", "mara_route_marker", "keeper_mara", 82, -40, 184, 2, 3, "NORTH", 82, -40, 180, "mechanic"),
            fixture("mara_map_marker", "mara_map_marker", "keeper_mara", 62, -40, 184, 4, 4, "NORTH", 62, -40, 180, "map"),

            fixture("stone_iss", "keeper_stone", "keeper_iss", -82, -40, 206, 8, 7, "EAST", -72, -40, 206, "required"),
            fixture("the_cold_hearth", "marker", "keeper_iss", -56, -40, 206, 8, 7, "WEST", -66, -40, 206, "contradiction"),

            fixture("stone_sella", "keeper_stone", "keeper_sella", 82, -40, 210, 8, 7, "WEST", 72, -40, 210, "required"),
            fixture("sella_pool", "sella_pool", "keeper_sella", 62, -40, 212, 5, 4, "NORTH", 62, -40, 206, "mechanic"),
            fixture("sella_anchor", "sella_anchor", "keeper_sella", 46, -40, 216, 3, 5, "NORTH", 46, -40, 211, "mechanic"),
            fixture("sella_lectern_1", "sella_lectern", "keeper_sella", 42, -40, 202, 2, 2, "SOUTH", 42, -40, 206, "book"),
            fixture("sella_lectern_2", "sella_lectern", "keeper_sella", 52, -40, 202, 2, 2, "SOUTH", 52, -40, 206, "book"),
            fixture("sella_lectern_3", "sella_lectern", "keeper_sella", 62, -40, 202, 2, 2, "SOUTH", 62, -40, 206, "book"),
            fixture("sella_lectern_4", "sella_lectern", "keeper_sella", 72, -40, 202, 2, 2, "SOUTH", 72, -40, 206, "book"),
            fixture("sella_lectern_5", "sella_lectern", "keeper_sella", 82, -40, 202, 2, 2, "SOUTH", 82, -40, 206, "book"),

            fixture("stone_brann", "keeper_stone", "keeper_brann", -82, -40, 236, 8, 7, "EAST", -72, -40, 236, "required"),
            fixture("brann_toll_tower", "brann_toll_tower", "keeper_brann", -58, -40, 230, 5, 7, "WEST", -66, -40, 230, "mechanic"),
            fixture("brann_corridor_start", "brann_corridor_start", "keeper_brann", -88, -40, 242, 3, 4, "EAST", -84, -40, 242, "mechanic"),
            fixture("brann_corridor_end", "brann_corridor_end", "keeper_brann", -42, -40, 242, 3, 4, "WEST", -46, -40, 242, "mechanic"),

            fixture("stone_orin", "keeper_stone", "keeper_orin", 84, -40, 238, 8, 7, "WEST", 74, -40, 238, "required"),
            fixture("orin_marker_1", "orin_marker", "keeper_orin", 42, -40, 232, 3, 4, "SOUTH", 42, -40, 236, "mechanic"),
            fixture("orin_marker_2", "orin_marker", "keeper_orin", 54, -40, 232, 3, 4, "SOUTH", 54, -40, 236, "mechanic"),
            fixture("orin_marker_3", "orin_marker", "keeper_orin", 66, -40, 232, 3, 4, "SOUTH", 66, -40, 236, "mechanic"),
            fixture("orin_marker_4", "orin_marker", "keeper_orin", 42, -40, 240, 3, 4, "NORTH", 42, -40, 236, "mechanic"),
            fixture("orin_marker_5", "orin_marker", "keeper_orin", 54, -40, 240, 3, 4, "NORTH", 54, -40, 236, "mechanic"),
            fixture("orin_marker_6", "orin_marker", "keeper_orin", 66, -40, 240, 3, 4, "NORTH", 66, -40, 236, "mechanic"),
            fixture("orin_frame_dial_1", "orin_frame_dial", "keeper_orin", 42, -39, 245, 2, 3, "NORTH", 42, -40, 241, "mechanic"),
            fixture("orin_frame_dial_2", "orin_frame_dial", "keeper_orin", 50, -39, 245, 2, 3, "NORTH", 50, -40, 241, "mechanic"),
            fixture("orin_frame_dial_3", "orin_frame_dial", "keeper_orin", 58, -39, 245, 2, 3, "NORTH", 58, -40, 241, "mechanic"),
            fixture("orin_frame_dial_4", "orin_frame_dial", "keeper_orin", 66, -39, 245, 2, 3, "NORTH", 66, -40, 241, "mechanic"),
            fixture("orin_frame_dial_5", "orin_frame_dial", "keeper_orin", 74, -39, 245, 2, 3, "NORTH", 74, -40, 241, "mechanic"),
            fixture("orin_frame_dial_6", "orin_frame_dial", "keeper_orin", 82, -39, 245, 2, 3, "NORTH", 82, -40, 240, "mechanic"),

            fixture("school_stand", "school_stand", "archive_school", -73, -68, 126, 14, 8, "EAST", -56, -68, 126, "evidence"),
            fixture("markers_row", "markers_row", "archive_markers", -73, -68, 158, 15, 8, "EAST", -55, -68, 158, "evidence"),
            fixture("cistern_7", "cistern_7", "archive_cistern", -73, -68, 191, 15, 9, "EAST", -55, -68, 191, "evidence"),
            fixture("watch_floor", "watch_floor", "archive_watch", -73, -68, 226, 14, 9, "EAST", -56, -68, 226, "evidence"),
            fixture("set_apart_shelf", "set_apart_shelf", "archive_shelf", -73, -68, 258, 14, 8, "EAST", -56, -68, 258, "evidence"),
            fixture("the_far_water", "far_water", "archive_water", -73, -68, 287, 18, 9, "EAST", -52, -68, 287, "evidence"),
            fixture("deep_market", "deep_market", "archive_market", 73, -68, 126, 20, 10, "WEST", 52, -68, 126, "evidence"),
            fixture("ration_table", "ration_table", "archive_ration", 73, -68, 158, 13, 8, "WEST", 57, -68, 158, "evidence"),
            fixture("third_bay_breach", "third_bay_breach", "archive_breach", 73, -68, 191, 16, 9, "WEST", 54, -68, 191, "evidence"),
            fixture("warm_town_collapse", "warm_town_collapse", "archive_warm", 75, -68, 226, 18, 10, "WEST", 54, -68, 226, "evidence"),
            fixture("dead_stall", "dead_stall", "archive_stall", 73, -68, 258, 10, 7, "WEST", 60, -68, 258, "evidence"),
            fixture("deep_bird_coops", "bird_coops", "archive_coops", 73, -68, 287, 12, 7, "WEST", 58, -68, 287, "evidence"),

            fixture("lampworks_stair", "lampworks_stair", "puzzle_works", 0, -68, 70, 20, 16, "NORTH", 0, -68, 46, "mechanic"),
            fixture("third_lamp_stand", "lamp_stand", "puzzle_works", -24, -68, 84, 4, 5, "NORTH", -24, -68, 79, "mechanic"),
            fixture("painted_line", "painted_line", "puzzle_works", 0, -68, 92, 5, 5, "NORTH", 0, -68, 86, "mechanic"),

            fixture("stone_of_reckoning", "structure", "lower_works", -42, -96, 72, 10, 9, "EAST", -29, -96, 72, "required"),
            fixture("case_board", "case_board", "prior_case", -80, -96, 136, 8, 6, "EAST", -69, -96, 136, "required"),
            fixture("prior_camp", "prior_camp", "prior_camp", -80, -96, 188, 28, 12, "EAST", -52, -96, 188, "required"),
            fixture("the_threshold", "the_threshold", "lower_threshold", 52, -96, 136, 10, 9, "WEST", 39, -96, 136, "required"),
            fixture("threshold_vault", "coop_plate", "lower_vault", 52, -96, 188, 9, 8, "WEST", 40, -96, 188, "required"),
            fixture("keeper_altar", "keeper_altar", "lower_spine", -16, -96, 174, 10, 8, "EAST", -3, -96, 174, "required"),
            fixture("coop_plate", "coop_plate", "lower_spine", 16, -96, 174, 9, 7, "WEST", 4, -96, 174, "required"),
            fixture("failed_accepting", "failed_accepting", "lower_spine", 0, -96, 207, 20, 10, "NORTH", 0, -96, 184, "required"),

            fixture("dread_route_start", "dread_route", "dread", 88, -96, 132, 5, 5, "SOUTH", 88, -96, 138, "dread"),
            fixture("dread_route_elsewhere", "dread_route", "dread", 100, -96, 158, 5, 5, "WEST", 94, -96, 158, "dread"),
            fixture("dread_route_figure", "dread_route", "dread", 100, -96, 190, 5, 5, "WEST", 94, -96, 190, "dread"),
            fixture("dread_route_exit", "dread_route", "dread", 88, -96, 214, 5, 5, "NORTH", 88, -96, 208, "dread"),

            fixture("unbroken_light", "accepting_floor", "accepting", 0, -96, 260, 24, 12, "NORTH", 0, -96, 233, "required"),
            fixture("the_unwriting", "seventh_shrine", "unwriting", 0, -96, 326, 18, 10, "NORTH", 0, -96, 305, "required"));

    public static final List<Gate> GATES = List.of(
            new Gate("g1", "G1 ROSETTA", 0, -40, 157, true, 12, 20, 3,
                    "rosetta_known", true),
            new Gate("g2", "G2 INVESTIGATION", 0, -40, 251, true, 12, 20, 3,
                    "keeper_investigation_begun", true),
            new Gate("g3", "G3 UNDERCROFT", 0, -68, 99, true, 12, 18, 3,
                    "undercroft_open", true),
            new Gate("g4", "G4 DEEP", 0, -96, 111, true, 12, 20, 3,
                    "deep_gate_open", true),
            new Gate("prior", "PRIOR CAMP", -80, -96, 156, true, 8, 16, 3,
                    "prior_absence_known", false),
            new Gate("dread", "DREAD PROCESSION", 72, -96, 132, false, 7, 16, 3,
                    "iss_caught_or_seventh_suspected", false),
            new Gate("g5", "G5 ACCEPTING", 0, -96, 223, true, 14, 22, 3,
                    "prior_witness_ready_and_accepting_onramp", true),
            new Gate("g6", "G6 CODA", 0, -96, 295, true, 14, 22, 3,
                    "bowed_as_one", true));

    public static final List<Link> LINKS = List.of(
            new Link("orientation", "keeper_nave"),
            new Link("keeper_nave", "keeper_vaun"),
            new Link("keeper_nave", "keeper_mara"),
            new Link("keeper_nave", "keeper_iss"),
            new Link("keeper_nave", "keeper_sella"),
            new Link("keeper_nave", "keeper_brann"),
            new Link("keeper_nave", "keeper_orin"),
            new Link("keeper_nave", "archive_nave"),
            new Link("archive_nave", "archive_school"),
            new Link("archive_nave", "archive_markers"),
            new Link("archive_nave", "archive_cistern"),
            new Link("archive_nave", "archive_watch"),
            new Link("archive_nave", "archive_shelf"),
            new Link("archive_nave", "archive_water"),
            new Link("archive_nave", "archive_market"),
            new Link("archive_nave", "archive_ration"),
            new Link("archive_nave", "archive_breach"),
            new Link("archive_nave", "archive_warm"),
            new Link("archive_nave", "archive_stall"),
            new Link("archive_nave", "archive_coops"),
            new Link("archive_nave", "puzzle_works"),
            new Link("puzzle_works", "lower_works"),
            new Link("lower_works", "lower_spine"),
            new Link("lower_spine", "prior_case"),
            new Link("prior_case", "prior_camp"),
            new Link("lower_spine", "lower_threshold"),
            new Link("lower_threshold", "lower_vault"),
            new Link("lower_spine", "dread"),
            new Link("lower_spine", "accepting"),
            new Link("accepting", "unwriting"),
            new Link("unwriting", "release"));

    public static final List<RecordStation> RECORD_STATIONS = List.of(
            new RecordStation("orientation_register", "orientation", 12, -40, 112, "WEST", "first register"),
            new RecordStation("court_census", "keeper_nave", -18, -40, 170, "EAST", "court census"),
            new RecordStation("archive_index", "archive_nave", -18, -68, 116, "EAST", "archive index"),
            new RecordStation("archive_closure", "archive_nave", 18, -68, 222, "WEST", "closure docket"),
            new RecordStation("prior_docket", "prior_case", -60, -96, 124, "WEST", "absence docket"),
            new RecordStation("threshold_hands", "lower_spine", 18, -96, 124, "WEST", "threshold hands"),
            new RecordStation("release_record", "release", 0, -96, 366, "NORTH", "release record"));

    private static Fixture fixture(String id, String type, String roomId,
                                   int x, int y, int z, int radius, int vertical,
                                   String front, int standX, int standY, int standZ,
                                   String contentRole) {
        return new Fixture(id, type, roomId, x, y, z, radius, vertical,
                front, standX, standY, standZ, contentRole);
    }

    public static Room room(String id) {
        if (id == null) return null;
        for (Room room : ROOMS) if (id.equals(room.id())) return room;
        return null;
    }

    public static Fixture fixture(String id) {
        if (id == null) return null;
        for (Fixture fixture : FIXTURES) if (id.equals(fixture.id())) return fixture;
        return null;
    }

    /** Returns every static defect. An empty list is required before a world build may begin. */
    public static List<String> validate() {
        List<String> errors = new ArrayList<>();
        Map<String, Room> rooms = new LinkedHashMap<>();
        for (Room room : ROOMS) {
            if (rooms.put(room.id(), room) != null) errors.add("duplicate room id " + room.id());
            if (room.minX() >= room.maxX() || room.floorY() >= room.ceilingY()
                    || room.minZ() >= room.maxZ()) errors.add("invalid room bounds " + room.id());
        }

        for (int i = 0; i < ROOMS.size(); i++) {
            Room a = ROOMS.get(i);
            for (int j = i + 1; j < ROOMS.size(); j++) {
                Room b = ROOMS.get(j);
                if (overlapsOwnership(a, b)) {
                    errors.add("room ownership overlap " + a.id() + " / " + b.id());
                }
            }
        }

        Set<String> fixtureIds = new LinkedHashSet<>();
        for (Fixture fixture : FIXTURES) {
            if (!fixtureIds.add(fixture.id())) errors.add("duplicate fixture id " + fixture.id());
            Room owner = rooms.get(fixture.roomId());
            if (owner == null) {
                errors.add("fixture " + fixture.id() + " has missing owner " + fixture.roomId());
                continue;
            }
            if (!owner.contains(fixture.x(), fixture.y(), fixture.z(), 3)) {
                errors.add("fixture " + fixture.id() + " anchor outside owner " + owner.id());
            }
            if (!owner.contains(fixture.standX(), fixture.standY(), fixture.standZ(), 2)) {
                errors.add("fixture " + fixture.id() + " standing zone outside owner " + owner.id());
            }
            if (!frontMatchesStandingZone(fixture)) {
                errors.add("fixture " + fixture.id() + " front does not face standing zone");
            }
        }
        if (fixtureIds.size() != 76) {
            errors.add("expected 76 canonical fixtures, found " + fixtureIds.size());
        }

        Set<String> gateIds = new HashSet<>();
        int mainIndex = 0;
        String[] expectedMain = {"g1", "g2", "g3", "g4", "g5", "g6"};
        for (Gate gate : GATES) {
            if (!gateIds.add(gate.id())) errors.add("duplicate gate id " + gate.id());
            if (gate.mainSequence()) {
                if (mainIndex >= expectedMain.length || !expectedMain[mainIndex].equals(gate.id())) {
                    errors.add("main gate order broken at " + gate.id());
                }
                mainIndex++;
            }
        }
        if (mainIndex != expectedMain.length) errors.add("expected six main gates, found " + mainIndex);

        Map<String, Set<String>> graph = new HashMap<>();
        for (Room room : ROOMS) graph.put(room.id(), new LinkedHashSet<>());
        for (Link link : LINKS) {
            if (!graph.containsKey(link.from()) || !graph.containsKey(link.to())) {
                errors.add("link has missing room " + link.from() + " -> " + link.to());
                continue;
            }
            graph.get(link.from()).add(link.to());
            graph.get(link.to()).add(link.from());
        }
        Set<String> reached = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add("orientation");
        while (!queue.isEmpty()) {
            String id = queue.removeFirst();
            if (!reached.add(id)) continue;
            for (String next : graph.getOrDefault(id, Set.of())) if (!reached.contains(next)) queue.addLast(next);
        }
        if (reached.size() != ROOMS.size()) {
            Set<String> missing = new LinkedHashSet<>(rooms.keySet());
            missing.removeAll(reached);
            errors.add("unreachable rooms " + missing);
        }

        Set<String> stationIds = new HashSet<>();
        for (RecordStation station : RECORD_STATIONS) {
            if (!stationIds.add(station.id())) errors.add("duplicate record station " + station.id());
            Room owner = rooms.get(station.roomId());
            if (owner == null || !owner.contains(station.x(), station.y(), station.z(), 3)) {
                errors.add("record station " + station.id() + " outside owner " + station.roomId());
            }
        }
        return List.copyOf(errors);
    }

    private static boolean overlapsOwnership(Room a, Room b) {
        boolean x = a.minX() <= b.maxX() && b.minX() <= a.maxX();
        // V4 shells own three foundation layers below floor and three roof layers beginning at
        // ceilingY. Adjacent stacked shells may touch neither block range; a one-block geology gap
        // is sufficient here because the separate five-block exterior envelope is not a room owner.
        boolean y = a.floorY() - 3 <= b.ceilingY() + 2 && b.floorY() - 3 <= a.ceilingY() + 2;
        boolean z = a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ();
        return x && y && z;
    }

    private static boolean frontMatchesStandingZone(Fixture fixture) {
        int dx = Integer.compare(fixture.standX(), fixture.x());
        int dz = Integer.compare(fixture.standZ(), fixture.z());
        return switch (fixture.front()) {
            case "NORTH" -> dz < 0;
            case "SOUTH" -> dz > 0;
            case "EAST" -> dx > 0;
            case "WEST" -> dx < 0;
            default -> false;
        };
    }
}
