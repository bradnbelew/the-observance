"""World anchors for Mossfield (seed 'bracken', Beta 1.5).

PROVISIONAL. These coordinates are placeholders until the Beta 1.5 world is generated
on seed 'bracken' and the town is laid onto its real terrain. When that happens, edit
the numbers here and rerun the build: every LogBlock row, server.log login position,
sign and chest placement follows automatically.
"""

from __future__ import annotations

PROVISIONAL = True
SEED_TEXT = "bracken"

ANCHORS = {
    "spawn": (0, 64, 0),
    "rules_signs": (2, 65, 3),             # three signs, x+0..2
    "cairn_storehouse_door_2011": (220, 68, -38),
    "cairn_storehouse_door_morrow": (226, 68, -38),   # Morrow's grid-squared rebuild, 6 east
    "cairn_farm": (205, 68, -60),
    "rookery_house": (-30, 66, 20),
    "rookery_cellar_floor": (-36, 58, 14),  # NW corner of the 18x9 wool floor, x+ east, z+ south
    "rookery_ladder_room": (-216, 40, 168),  # = nether hub (-27, 21) x 8
    "nether_hub": (-27, 70, 21),
    "june_house": (296, 71, 84),
    "june_tower": (312, 66, 96),
    "june_old_dock": (318, 63, 80),
    "iona_key_chest": (314, 60, 80),        # 3 down, 4 toward spawn (west) from the old dock
    "iona_notes": (-72, 40, 230),          # r.-1.0.mcr c(27,14) y40
    "patchcord_relay_hut": (-72, 70, 140),
    "finch_hub": (-30, 66, -12),
    "finch_spur": (-140, 62, -260),
    "maintenance_annex": (-330, 35, -244),
    "lowpath_place": (150, 75, 40),
    "kit_fossil_base": (812, 70, -640),
    "sheep_pen": (198, 68, -52),
    # modern layer (Theo-era and Morrow-era), installed on the upgraded world by /mrrw install
    "maint_sign": (0, 65, 7),  # its own block: (6,65,3) is Rookery's 2011 warning sign
    "theo_museum": (40, 64, -20),
    "theo_reading_room": (48, 64, -20),
    "theo_map_wall": (56, 64, -20),
    "meadow_center": (128, 64, 128),
    "south_rail_end": (-30, 66, 60),
    "listening_room": (228, 68, -30),
    "dust_corridor": (-330, 35, -236),
    "kit_genuine_door": (812, 70, -641),
}
SOUTH_RAIL_DIR = (0, 1)  # +z: the south branch keeps "finishing" toward lowpath's place


def region_chunk(x: int, z: int) -> str:
    cx, cz = x >> 4, z >> 4
    return "r.%d.%d.mcr c(%d,%d)" % (cx >> 5, cz >> 5, cx & 31, cz & 31)
