"""REFERENCE offline block-level reachability sim for the Deep Hold.

  ================================================================================
  READ design/handoff/04-MINECRAFT-SAFETY.md §5 BEFORE using or trusting this.
  ================================================================================

  THIS MODELS THE *OLD* COMPACT GEOMETRY (v5-compact-natural-2026-07-15) that the
  full rebuild REPLACES. It is committed only as a proven pattern to adapt, NOT as a
  current authority. During this session it correctly caught the archive_nave
  unreachability, the self-walling stair junctions, and (with material probes) the
  water-flood bugs — so the *approach* works; the *data below is stale by design*.

  THE REUSABLE PATTERN (rebuild this against the new coordinate-native plan):
    1. Model the world as solid[(x,y,z)] = bool, default solid (buried rock).
    2. Re-implement every builder primitive as SOLID/AIR writes only (ignore
       materials), and run them in the EXACT SAME ORDER as DeepHoldV4Geometry.
       This is what catches order-dependent self-walling — a later primitive
       re-walling a junction an earlier one opened.
    3. Model gate door cells as passable (mirror holdGateSpan / isV4GateDoorCell).
    4. BFS from the Mouth interior floor seed; a step is legal iff the target cell
       is standable (solid below, air at feet+head) or a gate door cell.
    5. ASSERT: every room, every fixture standing cell, and every record station is
       in the visited set, AND visited never escapes the authored envelope
       (a runaway flood == an accidental exit).

  Keep the primitive replay BYTE-FOR-BYTE faithful to the builder; update this in
  the SAME commit as any carve/dresser change, or it silently lies. A green sim is
  necessary but not sufficient (it does not model item-frame installs or water
  physics — those still need the live audit + restart).

  Original (stale) purpose: replays DeepHoldV4Geometry compact primitives in order,
  then runs the same walk audit as ObservanceCommand.auditV4OpenRoute.
"""
from collections import deque

SOLID = {}  # (x,y,z) -> True solid / False air; default: solid (buried rock / old world)

def get(x, y, z):
    return SOLID.get((x, y, z), True)

def set_b(x, y, z, solid):
    SOLID[(x, y, z)] = solid

# ---------------- plan data (compact transforms) ----------------
def compactX(x): return round(x * 0.60)
def compactD(d): return max(1, round(d * 0.60))
def compactZ(floorY, z):
    if floorY >= -54:
        return 106 + round((z - 106) * 0.50)
    if floorY >= -82:
        return 40 + round((z - 40) * 0.55) if z <= 99 else 75 + round((z - 102) * 0.50)
    if z <= 111: return 40 + round((z - 40) * 0.55)
    if z <= 223: return 82 + round((z - 114) * 0.55)
    if z <= 295: return 145 + round((z - 226) * 0.55)
    if z <= 350: return 186 + round((z - 298) * 0.55)
    return 219 + round((z - 354) * 0.55)

# Python round() is banker's; Java Math.round is floor(x+0.5). Emulate Java:
import math
def jround(v): return math.floor(v + 0.5)
def compactX(x): return jround(x * 0.60)
def compactD(d): return max(1, jround(d * 0.60))
def compactZ(floorY, z):
    if floorY >= -54:
        return 106 + jround((z - 106) * 0.50)
    if floorY >= -82:
        return 40 + jround((z - 40) * 0.55) if z <= 99 else 75 + jround((z - 102) * 0.50)
    if z <= 111: return 40 + jround((z - 40) * 0.55)
    if z <= 223: return 82 + jround((z - 114) * 0.55)
    if z <= 295: return 145 + jround((z - 226) * 0.55)
    if z <= 350: return 186 + jround((z - 298) * 0.55)
    return 219 + jround((z - 354) * 0.55)

ROOMS_RAW = [
    ("orientation", -42, 42, -40, -20, 106, 154),
    ("keeper_nave", -30, 30, -40, -16, 160, 248),
    ("keeper_vaun", -98, -34, -40, -22, 160, 188),
    ("keeper_mara", 34, 98, -40, -22, 160, 192),
    ("keeper_iss", -98, -34, -40, -22, 192, 220),
    ("keeper_sella", 34, 98, -40, -22, 196, 224),
    ("keeper_brann", -98, -34, -40, -22, 224, 248),
    ("keeper_orin", 34, 98, -40, -22, 228, 248),
    ("archive_nave", -30, 30, -68, -50, 102, 300),
    ("archive_school", -98, -48, -68, -52, 110, 142),
    ("archive_markers", -98, -48, -68, -52, 146, 170),
    ("archive_cistern", -98, -48, -68, -52, 174, 208),
    ("archive_watch", -98, -48, -68, -52, 212, 242),
    ("archive_shelf", -98, -48, -68, -52, 246, 270),
    ("archive_water", -102, -44, -68, -50, 274, 300),
    ("archive_market", 48, 98, -68, -50, 110, 142),
    ("archive_ration", 48, 98, -68, -52, 146, 170),
    ("archive_breach", 48, 98, -68, -50, 174, 208),
    ("archive_warm", 48, 102, -68, -50, 212, 242),
    ("archive_stall", 48, 98, -68, -52, 246, 270),
    ("archive_coops", 48, 98, -68, -52, 274, 300),
    ("puzzle_works", -64, 64, -68, -48, 40, 96),
    ("lower_works", -68, 68, -96, -74, 40, 108),
    ("lower_spine", -30, 30, -96, -74, 114, 220),
    ("prior_case", -112, -48, -96, -76, 118, 154),
    ("prior_camp", -112, -48, -96, -74, 158, 218),
    ("lower_threshold", 34, 70, -96, -74, 118, 154),
    ("lower_vault", 34, 70, -96, -74, 158, 218),
    ("dread", 74, 114, -96, -74, 118, 220),
    ("accepting", -58, 58, -96, -74, 226, 292),
    ("unwriting", -72, 72, -96, -74, 298, 350),
    ("release", -52, 52, -96, -74, 354, 378),
]
ROOMS = []
for rid, x1, x2, fy, cy, z1, z2 in ROOMS_RAW:
    ROOMS.append((rid, compactX(x1), compactX(x2), fy, cy, compactZ(fy, z1), compactZ(fy, z2)))

FIXTURES_RAW = [
    # id, x, y, z, radius, standX, standY, standZ
    ("undercroft_seal", -28, -40, 120, -28, -40, 114),
    ("forgotten_mouth", 28, -40, 120, 28, -40, 114),
    ("rune_rosetta", 0, -40, 120, 0, -40, 114),
    ("bow_marker_01", -28, -40, 142, -28, -40, 137),
    ("offering_cairn_01", 0, -40, 142, 0, -40, 137),
    ("kept_light_home_01", 28, -40, 142, 28, -40, 137),
    ("stone_vaun", -82, -40, 176, -72, -40, 176),
    ("vaun_hoard_chest", -58, -40, 168, -54, -40, 168),
    ("vaun_bookshelf", -58, -40, 180, -54, -40, 180),
    ("stone_mara", 82, -40, 176, 72, -40, 176),
    ("mara_lectern_1", 42, -40, 168, 42, -40, 172),
    ("mara_lectern_2", 52, -40, 168, 52, -40, 172),
    ("mara_lectern_3", 62, -40, 168, 62, -40, 172),
    ("mara_lectern_4", 72, -40, 168, 72, -40, 172),
    ("mara_lectern_5", 82, -40, 168, 82, -40, 172),
    ("mara_route_marker_1", 42, -40, 184, 42, -40, 180),
    ("mara_route_marker_2", 54, -40, 188, 54, -40, 184),
    ("mara_route_marker_3", 68, -40, 188, 68, -40, 184),
    ("mara_route_marker_4", 82, -40, 184, 82, -40, 180),
    ("mara_map_marker", 62, -40, 184, 62, -40, 180),
    ("stone_iss", -82, -40, 206, -72, -40, 206),
    ("the_cold_hearth", -56, -40, 206, -66, -40, 206),
    ("stone_sella", 82, -40, 210, 72, -40, 210),
    ("sella_pool", 62, -40, 212, 62, -40, 206),
    ("sella_anchor", 46, -40, 216, 46, -40, 211),
    ("sella_lectern_1", 42, -40, 202, 42, -40, 206),
    ("sella_lectern_2", 52, -40, 202, 52, -40, 206),
    ("sella_lectern_3", 62, -40, 202, 62, -40, 206),
    ("sella_lectern_4", 72, -40, 202, 72, -40, 206),
    ("sella_lectern_5", 82, -40, 202, 82, -40, 206),
    ("stone_brann", -82, -40, 236, -72, -40, 236),
    ("brann_toll_tower", -58, -40, 230, -66, -40, 230),
    ("brann_corridor_start", -88, -40, 242, -84, -40, 242),
    ("brann_corridor_end", -42, -40, 242, -46, -40, 242),
    ("stone_orin", 84, -40, 238, 74, -40, 238),
    ("orin_marker_1", 42, -40, 232, 42, -40, 236),
    ("orin_marker_2", 54, -40, 232, 54, -40, 236),
    ("orin_marker_3", 66, -40, 232, 66, -40, 236),
    ("orin_marker_4", 42, -40, 240, 42, -40, 236),
    ("orin_marker_5", 54, -40, 240, 54, -40, 236),
    ("orin_marker_6", 66, -40, 240, 66, -40, 236),
    ("orin_frame_dial_1", 42, -39, 245, 42, -40, 241),
    ("orin_frame_dial_2", 50, -39, 245, 50, -40, 241),
    ("orin_frame_dial_3", 58, -39, 245, 58, -40, 241),
    ("orin_frame_dial_4", 66, -39, 245, 66, -40, 241),
    ("orin_frame_dial_5", 74, -39, 245, 74, -40, 241),
    ("orin_frame_dial_6", 82, -39, 245, 82, -40, 240),
    ("school_stand", -73, -68, 126, -56, -68, 126),
    ("markers_row", -73, -68, 158, -55, -68, 158),
    ("cistern_7", -73, -68, 191, -55, -68, 191),
    ("watch_floor", -73, -68, 226, -56, -68, 226),
    ("set_apart_shelf", -73, -68, 258, -56, -68, 258),
    ("the_far_water", -73, -68, 287, -52, -68, 287),
    ("deep_market", 73, -68, 126, 52, -68, 126),
    ("ration_table", 73, -68, 158, 57, -68, 158),
    ("third_bay_breach", 73, -68, 191, 54, -68, 191),
    ("warm_town_collapse", 75, -68, 226, 54, -68, 226),
    ("dead_stall", 73, -68, 258, 60, -68, 258),
    ("deep_bird_coops", 73, -68, 287, 58, -68, 287),
    ("lampworks_stair", 0, -68, 70, 0, -68, 46),
    ("third_lamp_stand", -24, -68, 84, -24, -68, 79),
    ("painted_line", 0, -68, 92, 0, -68, 86),
    ("stone_of_reckoning", -42, -96, 72, -29, -96, 72),
    ("case_board", -80, -96, 136, -69, -96, 136),
    ("prior_camp", -80, -96, 188, -52, -96, 188),
    ("the_threshold", 52, -96, 136, 39, -96, 136),
    ("threshold_vault", 52, -96, 188, 40, -96, 188),
    ("keeper_altar", -16, -96, 174, -3, -96, 174),
    ("coop_plate", 16, -96, 174, 4, -96, 174),
    ("failed_accepting", 0, -96, 207, 0, -96, 184),
    ("dread_route_start", 88, -96, 132, 88, -96, 138),
    ("dread_route_elsewhere", 100, -96, 158, 94, -96, 158),
    ("dread_route_figure", 100, -96, 190, 94, -96, 190),
    ("dread_route_exit", 88, -96, 214, 88, -96, 208),
]
def fixture_adjust(fid, x, y, z, sx, sy, sz):
    x = compactX(x); z = compactZ(y, z); sx = compactX(sx); sz = compactZ(sy, sz)
    if fid in ("mara_route_marker_2", "mara_route_marker_3"): z -= 1
    if fid.startswith("orin_marker_") and z < 170: z = 170
    if fid.startswith("orin_frame_dial_") and z > 174: z = 174
    if fid.startswith("orin_frame_dial_") and sz >= z: sz = z - 1
    if fid == "painted_line" and z > 68: z = 68
    return x, y, z, sx, sy, sz
FIXTURES = [ (f[0],) + fixture_adjust(f[0], *f[1:]) for f in FIXTURES_RAW ]

GATES_RAW = [
    ("g1", 0, -40, 157, True, 12, 20, 3),
    ("g2", 0, -40, 251, True, 12, 20, 3),
    ("g3", 0, -68, 99, True, 12, 18, 3),
    ("g4", 0, -96, 111, True, 12, 20, 3),
    ("prior", -80, -96, 156, True, 8, 16, 3),
    ("dread", 72, -96, 132, False, 7, 16, 3),
    ("g5", 0, -96, 223, True, 14, 22, 3),
    ("g6", 0, -96, 295, True, 14, 22, 3),
]
GATES = []
for gid, x, y, z, ax, half, h, d in GATES_RAW:
    GATES.append((gid, compactX(x), y, compactZ(y, z), ax, max(5, compactD(half)), h, d))

RECORDS_RAW = [
    ("orientation_register", 12, -40, 112),
    ("court_census", -18, -40, 170),
    ("archive_index", -18, -68, 116),
    ("archive_closure", 18, -68, 222),
    ("prior_docket", -60, -96, 124),
    ("threshold_hands", 18, -96, 124),
    ("release_record", 0, -96, 366),
]
RECORDS = [ (r[0], compactX(r[1]), r[2], compactZ(r[2], r[3])) for r in RECORDS_RAW ]

ZONES = [
    ("keeper_archive", -62, 62, -40, -16, 132, 179),
    ("service_works", -40, 40, -68, -48, 38, 73),
    ("civic_library", -66, 66, -68, -48, 75, 176),
    ("lower_workroom", -42, 42, -96, -74, 38, 80),
    ("lower_spine", -20, 20, -96, -74, 82, 143),
    ("prior_camp_wing", -76, -20, -96, -74, 82, 141),
    ("threshold_wing", 20, 43, -96, -74, 82, 141),
    ("dread_service", 44, 70, -96, -74, 82, 141),
    ("accepting_archive", -36, 36, -96, -74, 145, 181),
    ("unwriting_chamber", -44, 44, -96, -74, 186, 215),
    ("release_office", -32, 32, -96, -74, 219, 233),
]

# ---------------- primitives ----------------
def build_vault_slice(cx, floor, z, half, height, open_roof):
    for x in range(cx - half - 3, cx + half + 4):
        for y in range(floor - 3, floor + height + 3):
            foundation = y < floor
            wall = abs(x - cx) > half
            roof = (not open_roof) and y >= floor + height
            if foundation: set_b(x, y, z, True)
            elif wall or roof: set_b(x, y, z, True)
            else: set_b(x, y, z, False)

def corridor_z(cx, floor, z1, z2, half, height):
    for z in range(min(z1, z2), max(z1, z2) + 1):
        build_vault_slice(cx, floor, z, half, height, False)

def corridor_x(_cx, floor, cz, x1, x2, half, height):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(cz - half - 3, cz + half + 4):
            for y in range(floor - 3, floor + height + 3):
                foundation = y < floor
                wall = abs(z - cz) > half
                roof = y >= floor + height
                if foundation: set_b(x, y, z, True)
                elif wall or roof: set_b(x, y, z, True)
                else: set_b(x, y, z, False)

def open_intersection(cx, floor, cz, hx, hz, height):
    for x in range(cx - hx, cx + hx + 1):
        for z in range(cz - hz, cz + hz + 1):
            set_b(x, floor - 1, z, True)
            for y in range(floor, floor + height):
                set_b(x, y, z, False)

def place_grand_tread(z, floor):
    for x in range(-7, 8):
        set_b(x, floor - 1, z, True)
        set_b(x, floor, z, False)
        set_b(x, floor + 1, z, False)
    if z in (24, 40, 68, 88):
        for x in (-6, 6):
            set_b(x, floor, z, True)
            set_b(x, floor + 1, z, True)   # wall block (solid-ish; treat solid)
            # soul lantern above: passable; ignore

def stair_drop(z):
    if z <= 18: return min(10, z - 8)
    if z <= 24: return 10
    if z <= 34: return 10 + (z - 24)
    if z <= 40: return 20
    if z <= 60: return 20 + ((z - 40) // 2)
    if z <= 68: return 30
    if z <= 88: return 30 + ((z - 68) // 2)
    return 40

def place_simple_tread(cx, floor, z, half):
    for x in range(cx - half, cx + half + 1):
        set_b(x, floor - 1, z, True)
        set_b(x, floor, z, False)
        set_b(x, floor + 1, z, False)

def build_facade_arch(cx, floor, z, half, height):
    for x in range(cx - half - 3, cx + half + 4):
        for y in range(floor, floor + height + 1):
            nx = abs(x - cx) / half
            ny = (y - floor) / height
            opening = nx * nx + ny * ny < 1.0 and y < floor + height - 1
            set_b(x, y, z, not opening)

def build_room_shell(x1, x2, fy, cy, z1, z2):
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            perimeter = x <= x1 + 2 or x >= x2 - 2 or z <= z1 + 2 or z >= z2 - 2
            for y in range(fy - 3, cy + 3):
                if y < fy: set_b(x, y, z, True)
                elif y >= cy or perimeter: set_b(x, y, z, True)
                else: set_b(x, y, z, False)

def add_room_ribs(x1, x2, fy, cy, z1, z2):
    along_z = (z2 - z1) >= (x2 - x1)
    start = (z1 + 8) if along_z else (x1 + 8)
    end = (z2 - 8) if along_z else (x2 - 8)
    p = start
    while p <= end:
        if along_z:
            for x in range(x1 + 3, x2 - 2):
                if abs(x - x1) <= 4 or abs(x2 - x) <= 4 or (x - x1) % 7 == 0:
                    set_b(x, cy - 1, p, True)
        else:
            for z in range(z1 + 3, z2 - 2):
                if abs(z - z1) <= 4 or abs(z2 - z) <= 4 or (z - z1) % 7 == 0:
                    set_b(p, cy - 1, z, True)
        p += 14

def add_buttresses(x1, x2, fy, cy, z1, z2):
    for x in range(x1 + 6, x2 - 5, 12):
        for zz in (z1, z2):
            for y in range(fy, cy):
                h = y - fy
                depth = 2 if h < 4 else (1 if h < 9 else 0)
                for d in range(depth + 1):
                    set_b(x, y, zz + d, True)
    for z in range(z1 + 6, z2 - 5, 12):
        for xx in (x1, x2):
            for y in range(fy, cy):
                h = y - fy
                depth = 2 if h < 4 else (1 if h < 9 else 0)
                for d in range(depth + 1):
                    set_b(xx + d, y, z, True)

def build_partition(x1, x2, floor, z, height):
    for x in range(x1, x2 + 1):
        for y in range(floor, floor + height + 1):
            set_b(x, y, z, True)

RESERVED = []  # fixture reservations: (x,y,z,radius) approximate: skip decor writes near fixtures
for fid, fx, fy, fz, sx, sy, sz in FIXTURES:
    RESERVED.append((fx, fy, fz))

def reserved(x, y, z, pad):
    # approximation of Geometry.reserved(): skip if within pad of any fixture anchor (radius varies)
    for (fx, fy, fz) in RESERVED:
        if abs(x - fx) <= pad + 4 and abs(z - fz) <= pad + 4 and abs(y - fy) <= 6:
            return True
    return False

# ---------------- build sequence ----------------
def neutralize():
    pass  # bands fill solid; default state is already solid

def mouth_and_grand_stair():
    for z in range(-6, 9):
        half = 12 if z < 0 else 15
        build_vault_slice(0, 0, z, half, 11, z <= 0)
    build_facade_arch(0, 0, 0, 15, 12)
    for z in range(9, 107):
        drop = stair_drop(z)
        floor = -drop
        build_vault_slice(0, floor, z, 8, 9, False)
        place_grand_tread(z, floor)
    corridor_z(0, -40, 102, 108, 8, 10)

def build_zone(x1, x2, fy, cy, z1, z2):
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            perimeter = x <= x1 + 2 or x >= x2 - 2 or z <= z1 + 2 or z >= z2 - 2
            for y in range(fy - 3, cy + 3):
                if y < fy: set_b(x, y, z, True)
                elif y >= cy or perimeter: set_b(x, y, z, True)
                else: set_b(x, y, z, False)

def carve_circulation():
    corridor_z(0, -40, 102, 110, 8, 10)
    corridor_z(0, -40, 128, 134, 6, 10)
    corridor_z(0, -40, 176, 183, 6, 10)
    corridor_x(-5, -40, 183, 0, -10, 5, 9)
    for z in range(183, 212):
        drop = min(14, (z - 183) // 2)
        build_vault_slice(-10, -40 - drop, z, 6, 9, False)
        place_simple_tread(-10, -40 - drop, z, 5)
    corridor_x(0, -54, 211, -10, 10, 5, 9)
    for z in range(211, 182, -1):
        drop = min(14, (211 - z) // 2)
        build_vault_slice(10, -54 - drop, z, 6, 9, False)
        place_simple_tread(10, -54 - drop, z, 5)
    corridor_z(0, -68, 174, 183, 6, 9)
    corridor_x(0, -68, 183, 0, 10, 5, 9)
    corridor_z(0, -68, 68, 78, 6, 9)
    corridor_z(-10, -68, 34, 42, 6, 9)
    for z in range(38, 9, -1):
        drop = min(14, (38 - z) // 2)
        build_vault_slice(-10, -68 - drop, z, 6, 9, False)
        place_simple_tread(-10, -68 - drop, z, 5)
    corridor_x(0, -82, 10, -10, 10, 5, 9)
    for z in range(10, 39):
        drop = min(14, (z - 10) // 2)
        build_vault_slice(10, -82 - drop, z, 6, 9, False)
        place_simple_tread(10, -82 - drop, z, 5)
    corridor_z(0, -96, 38, 84, 6, 9)
    corridor_x(0, -96, 38, 0, 10, 5, 9)
    corridor_x(0, -96, 94, -30, 30, 5, 8)
    corridor_x(0, -96, 130, -30, 30, 5, 8)
    corridor_z(-48, -96, 82, 141, 5, 8)
    corridor_z(31, -96, 82, 141, 5, 8)
    corridor_x(43, -96, 92, 36, 49, 5, 8)
    build_partition(-76, -20, -96, 105, 10)
    corridor_z(-48, -96, 101, 109, 5, 8)
    corridor_z(0, -96, 139, 148, 8, 10)
    corridor_z(0, -96, 179, 188, 8, 10)
    corridor_z(0, -96, 212, 221, 7, 9)
    for z in (94, 130):
        open_intersection(0, -96, z, 7, 9, 8)
        open_intersection(-48, -96, z, 9, 5, 8)
        open_intersection(31, -96, z, 9, 5, 8)
    open_intersection(-10, -96, 72, 4, 6, 9)
    open_intersection(-41, -96, 125, 4, 5, 8)
    open_intersection(24, -96, 123, 3, 3, 8)
    repair_stairs()

def repair_stairs():
    def clear_body(x1, x2, y1, y2, z1, z2):
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                for y in range(y1, y2 + 1):
                    set_b(x, y, z, False)
    def tread(x1, x2, feet, z):
        for x in range(x1, x2 + 1):
            set_b(x, feet - 1, z, True)
            for y in range(feet, feet + 6):
                set_b(x, y, z, False)
    clear_body(-3, -1, -40, -35, 183, 183)
    for z in range(203, 211):
        tread(-13, -7, -40 - min(14, (z - 183) // 2), z)
    clear_body(1, 3, -54, -49, 206, 211)
    for z in range(184, 192):
        tread(7, 13, -54 - min(14, (211 - z) // 2), z)
    clear_body(0, 2, -68, -63, 175, 177)
    for z in range(10, 19):
        tread(-13, -7, -68 - min(14, (38 - z) // 2), z)
    clear_body(1, 3, -82, -77, 10, 15)
    for z in range(30, 39):
        tread(7, 13, -82 - min(14, (z - 10) // 2), z)
    clear_body(0, 2, -96, -91, 44, 46)

def dress_districts():
    # bookshelf columns in civic library
    for z in range(84, 169, 14):
        for x in (-61, 61):
            if reserved(x, -68, z, 2): continue
            for y in range(-68, -64):
                set_b(x, y, z, True)
    # pillar lamps (solid columns)
    for z in (48, 64):
        for x in (-34, 34):
            if not reserved(x, -68, z, 3):
                for y in range(-68, -63):
                    set_b(x, y, z, True)
    for x in (-24, 24):
        for y in range(-40, -34):
            set_b(x, y, 126, True)
    # floor inlays are floor-level only; skip (harmless)

def build_gatehouse(gid, gx, gy, gz, across_x, half, height, depth):
    for d in range(-3, 4):
        for a in range(-half - 3, half + 4):
            for y in range(gy - 2, gy + height + 3):
                frame = abs(a) > half - 5 or y >= gy + height or y < gy
                x = gx + a if across_x else gx + d
                z = gz + d if across_x else gz + a
                if frame:
                    set_b(x, y, z, True)
                elif gy <= y < gy + height:
                    set_b(x, y, z, False)

def finish_surface():
    for x in range(-22, 23):
        for z in range(-6, 17):
            if abs(x) <= 12: continue
            crown = max(1, 8 - (abs(x) // 4) - max(0, z) // 6)
            for y in range(-2, crown + 1):
                set_b(x, y, z, True)

# run build
mouth_and_grand_stair()
o = ROOMS[0]
build_room_shell(o[1], o[2], o[3], o[4], o[5], o[6])
add_room_ribs(o[1], o[2], o[3], o[4], o[5], o[6])
add_buttresses(o[1], o[2], o[3], o[4], o[5], o[6])
for zn in ZONES:
    build_zone(zn[1], zn[2], zn[3], zn[4], zn[5], zn[6])
carve_circulation()
dress_districts()
for g in GATES:
    build_gatehouse(*g)
finish_surface()

# ---------------- audit ----------------
def gate_door_cell(x, y, z):
    for gid, gx, gy, gz, across_x, half, height, depth in GATES:
        door_half = max(3, min(6, half // 2))
        across = (x - gx) if across_x else (z - gz)
        dep = (z - gz) if across_x else (x - gx)
        dy = y - gy
        door_h = min(height - 3, max(6, door_half + 1))
        if abs(across) <= door_half and 0 <= dep <= depth and 0 <= dy <= door_h:
            return True
    return False

def standable(x, y, z):
    if get(x, y - 1, z) is False: return False
    feet_ok = (not get(x, y, z)) or gate_door_cell(x, y, z)
    head_ok = (not get(x, y + 1, z)) or gate_door_cell(x, y + 1, z)
    return feet_ok and head_ok

def plain_standable(x, y, z):
    return get(x, y - 1, z) and not get(x, y, z) and not get(x, y + 1, z)

seed = None
if plain_standable(0, 0, 2): seed = (0, 0, 2)
else:
    for z in range(0, 9):
        for dy in (0, -1, 1, -2):
            if plain_standable(0, dy, z):
                seed = (0, dy, z); break
        if seed: break
print("seed:", seed)
visited = set()
q = deque([seed])
while q:
    node = q.popleft()
    if node in visited: continue
    visited.add(node)
    x, y, z = node
    for dx, dz in ((1,0),(-1,0),(0,1),(0,-1)):
        nx, nz = x + dx, z + dz
        if nx < -80 or nx > 80 or nz < -6 or nz > 237: continue
        for dy in (0, 1, -1):
            ny = y + dy
            if ny < -108 or ny > 16: continue
            cand = (nx, ny, nz)
            if cand not in visited and standable(nx, ny, nz):
                q.append(cand)
                break
print("visited:", len(visited))

fails = []
for rid, x1, x2, fy, cy, z1, z2 in ROOMS:
    ok = any((x, fy, z) in visited for x in range(x1+3, x2-2) for z in range(z1+3, z2-2))
    if not ok: fails.append("ROOM " + rid)
for fid, fx, fy, fz, sx, sy, sz in FIXTURES:
    if (sx, sy, sz) not in visited:
        fails.append(f"FIXTURE {fid} stand {sx},{sy},{sz}")
for rid, x, y, z in RECORDS:
    ok = any((xx, yy, zz) in visited
             for xx in range(x-6, x+7) for zz in range(z-6, z+7) for yy in range(y-2, y+3))
    if not ok: fails.append(f"RECORD {rid} @ {x},{y},{z}")

if fails:
    print("FAILURES:")
    for f in fails: print("  " + f)
else:
    print("ALL ROOMS/FIXTURES/RECORDS REACHABLE")
