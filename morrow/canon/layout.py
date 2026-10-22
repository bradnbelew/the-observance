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
    "rules_signs": (-236, 71, 25),         # three signs, x+0..2, facing south into the plaza
    "cairn_storehouse_door_2011": (-80, 72, -140),     # door in the north wall; tally chests at x+2
    "cairn_storehouse_door_morrow": (-74, 72, -140),   # Morrow's grid-squared rebuild, 6 east
    "cairn_farm": (-100, 72, -162),        # NW corner of the fields, crop level
    "rookery_house": (-262, 71, -2),       # north wall, left of the door; floor level
    "rookery_cellar_floor": (-270, 62, -5),  # NW corner of the 18x9 wool floor, x+ east, z+ south
    "rookery_ladder_room": (-240, 40, 64),   # = nether hub (-30, 8) x 8
    "nether_hub": (-30, 70, 8),
    "june_house": (-246, 68, 236),         # door, east wall
    "june_tower": (-243, 66, 249),         # centre of the tower base; lamp at +22, glass at +23
    "june_old_dock": (-238, 64, 236),      # landward end of the dock, at the shoreline
    "iona_key_chest": (-238, 61, 232),     # 3 down, 4 toward spawn (north) from the old dock
    "iona_notes": (-180, 40, -20),
    "patchcord_relay_hut": (-199, 72, -70),  # note wall origin (notes at x+i, y+1, z+2)
    "finch_hub": (-215, 71, 22),
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
# The natural ground under lowpath's place (seed 'bracken'), where the 2011 low path ran. Morrow's hill
# (ModernLayer) lifts the house 3 above it; only a regenerated seed shows the hill isn't nature's (C-seed).
LOWPATH_GROUND = 69
GROUND_ANCHORS = ("spawn", "sheep_pen")  # anchors whose y is the ground block, not the standing level


def region_chunk(x: int, z: int) -> str:
    cx, cz = x >> 4, z >> 4
    return "r.%d.%d.mcr c(%d,%d)" % (cx >> 5, cz >> 5, cx & 31, cz & 31)
