"""World anchors for Mossfield (seed 'bracken', Beta 1.5).

Laid onto the real terrain of the owner's Beta 1.5_01 world on seed 'bracken' (generated
2026-10-12, spawn -233,64,30). The 2011 builds are placed by morrow/world/build2011.py and
the cipher objects by inject.py, both from these anchors; rerun the canon build after any
change: every LogBlock row, server.log login position, sign and chest placement follows.
Kit's base is the end of the owner's walk; its y is read from the generated ground.
"""

from __future__ import annotations

PROVISIONAL = False
SEED_TEXT = "bracken"

ANCHORS = {
    "spawn": (-233, 69, 30),               # plaza floor
    "rules_signs": (-236, 70, 25),         # three signs, x+0..2, facing south into the plaza
    "cairn_storehouse_door_2011": (-80, 71, -140),     # door in the north wall; tally chests at x+2
    "cairn_storehouse_door_morrow": (-74, 71, -140),   # Morrow's grid-squared rebuild, 6 east
    "cairn_farm": (-100, 72, -162),        # NW corner of the fields, crop level
    "rookery_house": (-262, 70, -2),       # north wall, left of the door; floor level
    "rookery_cellar_floor": (-270, 62, -5),  # NW corner of the 18x9 wool floor, x+ east, z+ south
    "rookery_ladder_room": (-240, 40, 64),   # = nether hub (-30, 8) x 8
    "nether_hub": (-30, 70, 8),
    "june_house": (-246, 67, 236),         # door, east wall
    "june_tower": (-243, 66, 249),         # centre of the tower base; lamp at +22, glass at +23
    "june_old_dock": (-238, 64, 236),      # landward end of the dock, at the shoreline
    "iona_key_chest": (-238, 61, 232),     # 3 down, 4 toward spawn (north) from the old dock
    "iona_notes": (-180, 40, -20),
    "patchcord_relay_hut": (-199, 69, -70),  # note wall origin (notes at x+i, y+1, z+2)
    "finch_hub": (-215, 70, 22),
    "finch_corner": (-170, 71, -120),      # where the main line turns east for the farms
    "finch_spur": (-224, 70, -116),        # her hidden station, in the hill north of town
    "maintenance_annex": (-286, 35, 28),
    "lowpath_place": (-213, 69, 86),
    "kit_fossil_base": (334, 70, -530),    # inside Kit's dirt hut (the owner's walk, 2026-10-13)
    "sheep_pen": (-112, 71, -150),
    # modern layer (Theo-era and Morrow-era), installed on the upgraded world by /mrrw install
    "maint_sign": (-236, 70, 32),
    "theo_museum": (-252, 69, -30),
    "theo_reading_room": (-244, 69, -30),
    "theo_map_wall": (-236, 69, -30),
    "meadow_center": (-180, 71, 55),
    "south_rail_end": (-215, 70, 62),
    "listening_room": (-64, 71, -132),
    "dust_corridor": (-286, 35, 36),
    "kit_genuine_door": (334, 70, -529),   # his door, south wall of the hut
}
SOUTH_RAIL_DIR = (0, 1)  # +z: the south branch keeps "finishing" toward lowpath's place


def region_chunk(x: int, z: int) -> str:
    cx, cz = x >> 4, z >> 4
    return "r.%d.%d.mcr c(%d,%d)" % (cx >> 5, cz >> 5, cx & 31, cz & 31)
