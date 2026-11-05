"""World anchors for Mossfield (seed 'bracken', Beta 1.5).

Laid onto the real terrain of the owner's Beta 1.5_01 world on seed 'bracken' (generated
2026-10-12, spawn -233,64,30). The 2011 builds are placed by morrow/world/build2011.py and
the cipher objects by inject.py, both from these anchors; rerun the canon build after any
change: every LogBlock row, server.log login position, sign and chest placement follows.
Kit's base is the end of the owner's walk; its y is read from the generated ground.

Heights: a building or place anchor's y is the STANDING level, the first air block above its floor
(so its floor block sits at y-1, level with the grass round it, never below it). Exceptions say so:
spawn and sheep_pen are the ground blocks themselves, cairn_farm is crop level. Floors were set
from the natural ground in a ring round each footprint (2026-10-19), so nothing is sunk.
"""

from __future__ import annotations

PROVISIONAL = False
SEED_TEXT = "bracken"

ANCHORS = {
    "spawn": (-233, 70, 30),               # plaza floor (the square's blocks; you stand at y+1)
    "rules_signs": (-236, 72, 25),         # three signs, x+0..2, facing south into the plaza, on a cobble plinth
    "spawn_kit_chest": (-237, 71, 28),     # Rook's newbie chest (double, z+0..1) on the plaza's west edge; sign at x-1
    "spawn_bread_chest": (-237, 71, 31),   # cairn's spawn bread chest
    "spawn_board": (-234, 72, 35),         # the bulletin board: 3x2 wall signs facing north, x+0..2, y-0..1
    "spawn_portal": (-226, 71, 28),        # Rook's Nether portal frame (27 May 2011), bottom NW obsidian, runs +z
    "spawn_creeper": (-227, 72, 16),       # his 8x8 wool creeper face, bottom-west corner, runs +x and +y; unfinished
    "cairn_storehouse_door_2011": (-80, 72, -140),     # door in the north wall; tally chests at x+2
    "cairn_storehouse_jog": (-82, 72, -125),   # the crooked SW corner (a one-block jog); Morrow squared it 2011-09-02
    "cairn_shed_hatch": (-67, 71, -136),       # the hole in the shed floor; Morrow's rebuild put a trapdoor in it
    "cairn_farm": (-100, 72, -162),        # NW corner of the fields, crop level
    "rookery_house": (-262, 71, -2),       # north wall, left of the door; floor level
    "rookery_cellar_floor": (-270, 62, -5),  # NW corner of the 18x9 wool floor, x+ east, z+ south (rows: floor_row_z)
    "rookery_floor_sign": (-259, 64, 4),     # Rook's "dont touch it" sign on the landing, by the ladder foot
    "rookery_changes_wall": (-265, 73, 1),   # his 2011 ledger signs: west wall of the front room, read facing west
    "rookery_ladder_room": (-240, 40, 64),   # = nether hub (-30, 8) x 8
    "nether_hub": (-30, 70, 8),
    "june_house": (-246, 68, 236),         # door, east wall
    "june_tower": (-243, 66, 249),         # centre of the tower base; lamp at +22, glass at +23
    "june_old_dock": (-238, 64, 236),      # landward end of the dock, at the shoreline
    "iona_key_chest": (-238, 61, 232),     # 3 down, 4 toward spawn (north) from the old dock
    "iona_notes": (-180, 40, -20),
    "patchcord_relay_hut": (-199, 72, -70),  # note wall origin (notes at x+i, y+1, z+2)
    "finch_hub": (-215, 71, 22),
    "finch_jct_1": (-215, 71, -60),        # the junction rail where the spur leaves the main line (lever at x+1)
    "finch_jct_2": (-170, 72, -120),       # finch's corner: a siding stub runs on north (lever at x-1)
    "finch_jct_3": (-140, 74, -120),       # the east leg: a siding stub runs south (lever at z-1)
    "finch_corner": (-170, 71, -120),      # where the main line turns east for the farms
    "finch_spur": (-224, 70, -116),        # her hidden station, in the hill north of town
    "maintenance_annex": (-286, 35, 28),
    "lowpath_place": (-213, 73, 86),       # on Morrow's hill: the seed's own ground here is LOWPATH_GROUND
    "kit_fossil_base": (334, 70, -530),    # inside Kit's dirt hut (the owner's walk, 2026-10-13)
    "sheep_pen": (-112, 73, -150),           # the pen's ground (fences at y+1)
    # modern layer (Theo-era and Morrow-era), installed on the upgraded world by /mrrw install
    "maint_sign": (-236, 71, 32),
    "theo_museum": (-252, 70, -30),
    "theo_reading_room": (-244, 70, -30),
    "theo_map_wall": (-236, 70, -30),
    "meadow_center": (-180, 71, 55),
    "south_rail_end": (-215, 71, 62),
    "listening_room": (-64, 72, -132),     # NW corner of cairn's sealed room, standing level (ground 71)
    "dust_corridor": (-286, 35, 36),       # west end, standing level = Iona's annex; runs east
    "kit_genuine_door": (334, 70, -529),   # his door, south wall of the hut
}
SOUTH_RAIL_DIR = (0, 1)  # +z: the south branch keeps "finishing" toward lowpath's place
SOUTH_RAIL_STOP_Z = 82   # Morrow's extension never passes this z: lowpath's place starts at 84 (the hill's foot)
# The natural ground under lowpath's place (seed 'bracken'), where the 2011 low path ran. Morrow's hill
# (ModernLayer) lifts the house 3 above it; only a regenerated seed shows the hill isn't nature's (C-seed).
LOWPATH_GROUND = 69
GROUND_ANCHORS = ("spawn", "sheep_pen")  # anchors whose y is the ground block, not the standing level


# ---- reading order ---------------------------------------------------------------------------
# An ordered sequence on a wall reads LEFT TO RIGHT as the reader faces it. Facing south, left is
# +x; facing north, left is -x; facing west, left is +z; facing east, left is -z. Every ordered run
# (patch's notes, the Residents' Key signs, Iona's ID wall, Rook's ledger) is laid with wall_run from
# its leftmost slot, so the world, LogBlock and the plugin agree.
LEFT_TO_RIGHT = {"south": (-1, 0), "north": (1, 0), "west": (0, -1), "east": (0, 1)}


def wall_run(reader_faces: str, leftmost: tuple[int, int, int], i: int) -> tuple[int, int, int]:
    x, y, z = leftmost
    dx, dz = LEFT_TO_RIGHT[reader_faces]
    return (x + dx * i, y, z + dz * i)


# Rook's floor: the reader comes down the ladder onto the south landing and faces north. Row 0 (the
# first row read) is the row nearest the landing; rows run away from the reader. Within a row the
# reading is boustrophedon, starting at the reader's left (west, -x), as finch's rail taught him.
FLOOR_ROWS = 9


def floor_row_z(r: int) -> int:
    """z offset of floor row r from the rookery_cellar_floor anchor (row 0 is the southmost, by the ladder)."""
    return FLOOR_ROWS - 1 - r


# Patch's note wall (reader faces south): leftmost note at x+10. Iona's ID wall (reader faces south):
# leftmost block at x+9. The Residents' Key signs (reader faces south): leftmost sign at x+3.
PATCH_NOTE_LEFT = (10, 1, 2)      # offsets from patchcord_relay_hut
ID_WALL_LEFT = (9, 1, 0)          # offsets from maintenance_annex
RESIDENTS_KEY_LEFT = (3, 1, 0)    # offsets from rookery_ladder_room


def region_chunk(x: int, z: int) -> str:
    cx, cz = x >> 4, z >> 4
    return "r.%d.%d.mcr c(%d,%d)" % (cx >> 5, cz >> 5, cx & 31, cz & 31)
