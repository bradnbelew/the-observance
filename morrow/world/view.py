"""First-person previews of a world: check a build or a screenshot framing without the game.

Reads Beta (McRegion) worlds and upgraded ones (Anvil .mca, e.g. build/mrrw-v2-test/Mossfield, or
its Mossfield_nether/DIM-1). Modern block states map to the Beta look where Beta had the block,
otherwise to a colour and a rough shape; item frames (and the maps in them) are drawn. Paper's
saved light is unreliable after --forceUpgrade, so an Anvil world's sky and block light are computed.

    python -m morrow.world.view <world> --save [--out f.png]          # a save's own player pose and clock
    python -m morrow.world.view <world> --cam X Y Z YAW PITCH [--time T] [--out f.png]
    python -m morrow.world.view <world> --orbit X Y Z [--radius R] [--time T] [--out f.png]

--ambient N lifts every light level to at least N, to inspect a dark room or a cave.

Y is eye height. Yaw and pitch as in Minecraft: yaw 0 faces +z (south), 90 west, 180 north,
270 east; positive pitch looks down. --orbit draws four views from the corners, 2x2.

Lighting is the chunks' own sky and block light under the clock's daylight, so a dark room renders
dark. Slabs, stairs, fences, doors, torches, ladders, rails, signs, plants, beds and chests have
rough shapes; glass and water are see-through; sheep are boxes in their wool colour. The sky is
Beta's: before Beta 1.9pre4 the sun and moon crossed north (-z) to south, and the moon is square.
Unknown block ids render magenta, so anything odd stands out. It's a check, not a screenshot.
"""

from __future__ import annotations

import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

from morrow.world import nbt
from morrow.world.anvil import AnvilWorld, section_states
from morrow.world.mcregion import World

ANVIL_TOP = 160   # an upgraded Beta world: nothing of ours is above 127, and nothing we look at below 0

# ---- block look-up tables ------------------------------------------------------------------

def _rgb(*c):
    return np.array(c, np.float32)

NID = 4096   # 0-255 are Beta ids; modern block states get ids from 256 up (see "modern" below)
COL = np.full((NID, 16, 3), (255, 0, 255), np.float32)  # unknown = magenta
def _c(i, rgb, metas=range(16)):
    for m in metas:
        COL[i, m] = rgb

for i, rgb in {
    1: (125, 125, 125), 2: (98, 156, 68), 3: (134, 96, 67), 4: (112, 112, 112), 5: (160, 130, 80),
    6: (70, 130, 40), 7: (55, 55, 55), 8: (45, 75, 190), 9: (45, 75, 190), 10: (225, 105, 25), 11: (225, 105, 25),
    12: (219, 207, 163), 13: (136, 126, 126), 14: (140, 135, 115), 15: (135, 125, 118), 16: (110, 110, 110),
    19: (205, 205, 70), 20: (225, 238, 245), 21: (110, 120, 150), 22: (35, 70, 160), 23: (110, 110, 110),
    24: (216, 203, 155), 25: (100, 66, 48), 26: (165, 40, 40), 27: (175, 145, 70), 28: (130, 120, 110),
    29: (120, 110, 90), 30: (220, 220, 220), 31: (90, 150, 60), 32: (125, 95, 45), 33: (150, 130, 90),
    37: (230, 215, 40), 38: (200, 30, 25), 39: (150, 115, 90), 40: (190, 40, 35), 41: (245, 210, 70),
    42: (220, 220, 220), 43: (168, 168, 168), 45: (150, 82, 68), 46: (200, 60, 40), 47: (120, 92, 52),
    48: (95, 118, 92), 49: (22, 18, 32), 50: (255, 214, 90), 51: (240, 150, 30), 52: (30, 50, 70),
    53: (160, 130, 80), 54: (168, 120, 46), 55: (175, 15, 10), 56: (125, 140, 140), 57: (120, 220, 215),
    58: (138, 98, 58), 59: (110, 160, 50), 60: (98, 66, 42), 61: (100, 100, 100), 62: (110, 100, 95),
    63: (160, 130, 80), 64: (140, 104, 58), 65: (132, 100, 58), 66: (130, 120, 105), 67: (112, 112, 112),
    68: (160, 130, 80), 69: (110, 90, 60), 70: (125, 125, 125), 71: (195, 195, 195), 72: (160, 130, 80),
    73: (135, 105, 105), 74: (135, 105, 105), 75: (110, 30, 20), 76: (215, 45, 20), 77: (125, 125, 125),
    78: (240, 250, 250), 79: (160, 190, 255), 80: (240, 250, 250), 81: (40, 115, 40), 82: (160, 166, 179),
    83: (125, 175, 85), 84: (108, 72, 52), 85: (160, 130, 80), 86: (210, 120, 20), 87: (110, 50, 50),
    88: (85, 65, 52), 89: (250, 222, 130), 90: (110, 40, 200), 91: (225, 140, 30), 92: (232, 222, 210),
}.items():
    _c(i, rgb)
_c(0, (0, 0, 0))
for m, rgb in enumerate([(222, 222, 222), (234, 126, 54), (190, 72, 200), (106, 140, 212), (196, 182, 30),
                         (65, 176, 54), (210, 132, 156), (66, 66, 66), (156, 162, 162), (44, 112, 136),
                         (126, 60, 182), (46, 56, 144), (80, 50, 30), (56, 76, 26), (152, 50, 46), (26, 22, 22)]):
    _c(35, rgb, [m])
for m, rgb in enumerate([(104, 82, 50), (58, 42, 26), (214, 214, 206)]):
    _c(17, rgb, [m, m + 4, m + 8, m + 12])
for m, rgb in enumerate([(62, 122, 40), (52, 88, 52), (96, 140, 62)]):
    _c(18, rgb, [m, m + 4, m + 8, m + 12])
for m, rgb in enumerate([(168, 168, 168), (216, 203, 155), (160, 130, 80), (112, 112, 112)]):
    _c(44, rgb, [m]); _c(43, rgb, [m])

FULL = np.zeros(NID, bool)
FULL[[1, 2, 3, 4, 5, 7, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 29, 33, 35, 41, 42, 43, 45, 46, 47, 48,
      49, 52, 56, 57, 58, 61, 62, 73, 74, 79, 80, 82, 84, 86, 87, 88, 89, 91]] = True
TRANSP = {20: 0.0, 8: 0.32, 9: 0.32, 79: 0.5}   # glass handled by its frame
EMIT = np.zeros(NID, np.float32)
EMIT[[10, 11, 50, 51, 89, 91]] = 1.0
EMIT[[76, 74, 90]] = 0.7
# texture families, so modern ids can share the Beta looks
PLANKY = np.zeros(NID, bool); PLANKY[[5, 53, 85, 63, 68, 72]] = True
BOOKY = np.zeros(NID, bool); BOOKY[47] = True
LOGGY = np.zeros(NID, bool); LOGGY[17] = True
LIGHTLV = np.zeros(NID, np.uint8)   # block light emitted (used when a world stores none)
LIGHTLV[[10, 11, 51, 89, 91]] = 15; LIGHTLV[50] = 14; LIGHTLV[[76, 74]] = 7; LIGHTLV[90] = 11

# shaped blocks: up to 9 boxes per (id, meta) in unit coords (x0, y0, z0, x1, y1, z1)
K = 9
BOX = np.full((NID, 16, K, 6), np.nan, np.float32)
SHAPED = np.zeros(NID, bool)

def _box(i, metas, boxes):
    SHAPED[i] = True
    for m in metas:
        for k, bx in enumerate(boxes):
            BOX[i, m, k] = bx

ALL = range(16)
_box(44, ALL, [(0, 0, 0, 1, .5, 1)])
for i in (53, 67):
    _box(i, [0], [(0, 0, 0, 1, .5, 1), (.5, .5, 0, 1, 1, 1)])
    _box(i, [1], [(0, 0, 0, 1, .5, 1), (0, .5, 0, .5, 1, 1)])
    _box(i, [2], [(0, 0, 0, 1, .5, 1), (0, .5, .5, 1, 1, 1)])
    _box(i, [3], [(0, 0, 0, 1, .5, 1), (0, .5, 0, 1, 1, .5)])
# fences: meta is replaced by a neighbour mask (1 = -x, 2 = +x, 4 = -z, 8 = +z) when the volume loads
for mask in ALL:
    bx = [(.375, 0, .375, .625, 1, .625)]
    for bit, (x0, x1, z0, z1) in ((1, (0, .375, .44, .56)), (2, (.625, 1, .44, .56)), (4, (.44, .56, 0, .375)), (8, (.44, .56, .625, 1))):
        if mask & bit:
            bx += [(x0, .375, z0, x1, .5625, z1), (x0, .75, z0, x1, .9375, z1)]
    _box(85, [mask], bx)
for i in (64, 71):   # door edge: 0 west, 1 north, 2 east, 3 south (+4 open swings it a quarter turn)
    for m in ALL:
        e = (m & 3) if not m & 4 else ((m & 3) + 1) % 4
        _box(i, [m], [[(0, 0, 0, .19, 1, 1), (0, 0, 0, 1, 1, .19), (.81, 0, 0, 1, 1, 1), (0, 0, .81, 1, 1, 1)][e]])
_box(65, [2], [(0, 0, .875, 1, 1, 1)]); _box(65, [3], [(0, 0, 0, 1, 1, .125)])
_box(65, [4], [(.875, 0, 0, 1, 1, 1)]); _box(65, [5], [(0, 0, 0, .125, 1, 1)])
for i in (50, 75, 76):
    _box(i, ALL, [(.44, 0, .44, .56, .62, .56)])
    _box(i, [1], [(0, .2, .44, .2, .82, .56)]); _box(i, [2], [(.8, .2, .44, 1, .82, .56)])
    _box(i, [3], [(.44, .2, 0, .56, .82, .2)]); _box(i, [4], [(.44, .2, .8, .56, .82, 1)])
for i in (27, 28, 66):
    _box(i, ALL, [(0, 0, 0, 1, .0625, 1)])
_box(55, ALL, [(0, 0, 0, 1, .03, 1)])
for m in ALL:   # standing sign: post + board, board turned for east/west facing
    ew = m % 8 in (3, 4, 5)
    _box(63, [m], [(.44, 0, .44, .56, .55, .56), (.44, .5, 0, .56, 1, 1) if ew else (0, .5, .44, 1, 1, .56)])
_box(68, [2], [(0, .28, .875, 1, .78, 1)]); _box(68, [3], [(0, .28, 0, 1, .78, .125)])
_box(68, [4], [(.875, .28, 0, 1, .78, 1)]); _box(68, [5], [(0, .28, 0, .125, .78, 1)])
_box(26, ALL, [(0, 0, 0, 1, .56, 1)])
_box(92, ALL, [(.06, 0, .06, .94, .5, .94)])
_box(54, ALL, [(.06, 0, .06, .94, .875, .94)])
_box(60, ALL, [(0, 0, 0, 1, .94, 1)])
_box(78, ALL, [(0, 0, 0, 1, .125, 1)])
_box(81, ALL, [(.06, 0, .06, .94, 1, .94)])
_box(69, ALL, [(.375, 0, .375, .625, .6, .625)])
_box(77, ALL, [(.375, .375, .375, .625, .625, .625)])
for i in (70, 72):
    _box(i, ALL, [(.06, 0, .06, .94, .06, .94)])
for i, h in ((6, .8), (31, .7), (32, .6), (37, .55), (38, .6), (39, .4), (40, .4), (83, 1)):
    _box(i, ALL, [(0, 0, .48, 1, h, .52), (.48, 0, 0, .52, h, 1)])
for m in range(8):
    _box(59, [m], [(0, 0, .48, 1, (m + 1) / 8, .52), (.48, 0, 0, .52, (m + 1) / 8, 1), (0, 0, .15, 1, (m + 1) / 8, .19), (.15, 0, 0, .19, (m + 1) / 8, 1)])

# ---- modern block states (Anvil worlds) ----------------------------------------------------
# A modern state maps to a Beta (id, meta) when Beta had the block, so it gets the Beta look;
# otherwise each distinct state string gets its own id with a colour and a rough shape.

DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
        "purple", "blue", "brown", "green", "red", "black"]
DYE_RGB = {d: tuple(COL[35, i].astype(int)) for i, d in enumerate(DYES)}
WOOD = {"oak": (160, 130, 80), "spruce": (115, 85, 50), "birch": (196, 178, 122), "jungle": (160, 115, 80),
        "acacia": (168, 90, 50), "dark_oak": (66, 43, 20), "mangrove": (117, 54, 48), "cherry": (226, 178, 172),
        "pale_oak": (228, 218, 216), "bamboo": (194, 173, 80), "crimson": (101, 48, 70), "warped": (43, 104, 99),
        "petrified_oak": (160, 130, 80)}
BARK = {"oak": (104, 82, 50), "spruce": (58, 42, 26), "birch": (214, 214, 206), "jungle": (85, 67, 25),
        "acacia": (103, 96, 86), "dark_oak": (60, 46, 26), "mangrove": (84, 66, 41), "cherry": (54, 33, 44),
        "pale_oak": (88, 80, 76), "crimson": (93, 26, 30), "warped": (58, 59, 78)}
STRIPPED = {"oak": (177, 144, 86), "spruce": (115, 89, 52), "birch": (196, 176, 118), "jungle": (171, 132, 84),
            "acacia": (174, 92, 59), "dark_oak": (96, 76, 49), "mangrove": (119, 54, 47), "cherry": (215, 145, 148)}

BETA_OF = {
    "stone": (1, 0), "grass_block": (2, 0), "dirt": (3, 0), "coarse_dirt": (3, 0), "cobblestone": (4, 0),
    "oak_planks": (5, 0), "bedrock": (7, 0), "water": (9, 0), "bubble_column": (9, 0), "lava": (11, 0), "sand": (12, 0),
    "gravel": (13, 0), "gold_ore": (14, 0), "iron_ore": (15, 0), "coal_ore": (16, 0), "oak_log": (17, 0),
    "spruce_log": (17, 1), "birch_log": (17, 2), "oak_wood": (17, 0), "spruce_wood": (17, 1), "birch_wood": (17, 2),
    "oak_leaves": (18, 0), "spruce_leaves": (18, 1), "birch_leaves": (18, 2), "sponge": (19, 0), "glass": (20, 0),
    "lapis_ore": (21, 0), "lapis_block": (22, 0), "dispenser": (23, 0), "sandstone": (24, 0), "note_block": (25, 0),
    "powered_rail": (27, 0), "detector_rail": (28, 0), "short_grass": (31, 1), "grass": (31, 1), "fern": (31, 2),
    "dead_bush": (32, 0), "dandelion": (37, 0), "poppy": (38, 0), "brown_mushroom": (39, 0), "red_mushroom": (40, 0),
    "gold_block": (41, 0), "iron_block": (42, 0), "smooth_stone": (43, 0), "bricks": (45, 0), "tnt": (46, 0),
    "bookshelf": (47, 0), "mossy_cobblestone": (48, 0), "obsidian": (49, 0), "torch": (50, 5), "fire": (51, 0),
    "spawner": (52, 0), "chest": (54, 0), "trapped_chest": (54, 0), "diamond_ore": (56, 0), "diamond_block": (57, 0),
    "crafting_table": (58, 0), "farmland": (60, 0), "furnace": (61, 0), "rail": (66, 0), "redstone_ore": (73, 0),
    "redstone_torch": (76, 5), "snow": (78, 0), "ice": (79, 0), "snow_block": (80, 0), "cactus": (81, 0),
    "clay": (82, 0), "sugar_cane": (83, 0), "jukebox": (84, 0), "pumpkin": (86, 0), "carved_pumpkin": (86, 0),
    "netherrack": (87, 0), "soul_sand": (88, 0), "glowstone": (89, 0), "jack_o_lantern": (91, 0), "cake": (92, 0),
    "stone_pressure_plate": (70, 0), "oak_pressure_plate": (72, 0),
}
FULL_RGB = {
    "deepslate": (80, 80, 82), "cobbled_deepslate": (77, 77, 80), "polished_deepslate": (72, 72, 73),
    "deepslate_tiles": (54, 54, 56), "deepslate_bricks": (70, 70, 72), "infested_deepslate": (80, 80, 82),
    "tuff": (108, 109, 102), "calcite": (223, 224, 220), "smooth_basalt": (72, 72, 78), "dripstone_block": (134, 107, 92),
    "sculk": (13, 30, 36), "sculk_catalyst": (40, 60, 60), "copper_block": (192, 107, 79), "exposed_copper": (161, 125, 103),
    "weathered_copper": (108, 153, 110), "oxidized_copper": (82, 162, 132), "lodestone": (147, 149, 152),
    "barrel": (130, 95, 55), "stone_bricks": (122, 121, 122), "mossy_stone_bricks": (115, 121, 105),
    "cracked_stone_bricks": (118, 117, 118), "chiseled_stone_bricks": (120, 119, 120), "andesite": (136, 136, 137),
    "diorite": (188, 188, 188), "granite": (149, 103, 85), "polished_andesite": (132, 135, 134),
    "polished_diorite": (193, 193, 195), "polished_granite": (154, 106, 89), "smooth_sandstone": (223, 214, 170),
    "red_sand": (190, 102, 33), "terracotta": (152, 94, 67), "packed_ice": (141, 180, 250), "magma_block": (142, 63, 31),
    "hay_block": (166, 139, 12), "quartz_block": (235, 229, 222), "nether_bricks": (44, 21, 26), "moss_block": (89, 109, 45),
    "podzol": (91, 63, 24), "mycelium": (111, 98, 101), "amethyst_block": (133, 97, 191), "budding_amethyst": (132, 96, 186),
    "raw_iron_block": (166, 136, 107), "coal_block": (16, 15, 15), "redstone_block": (175, 24, 5),
    "emerald_block": (42, 203, 87), "bone_block": (229, 225, 207), "copper_ore": (124, 125, 120), "emerald_ore": (117, 136, 124),
    "mud": (60, 57, 61), "mud_bricks": (137, 103, 79), "packed_mud": (142, 106, 79), "rooted_dirt": (144, 103, 76),
    "smoker": (90, 85, 80), "blast_furnace": (100, 100, 100), "dropper": (110, 110, 110), "observer": (98, 98, 98),
    "piston": (150, 130, 95), "sticky_piston": (130, 140, 95), "target": (226, 170, 157), "smithing_table": (57, 58, 70),
    "cartography_table": (103, 87, 67), "fletching_table": (197, 180, 133), "loom": (142, 119, 91), "beehive": (182, 146, 94),
    "bee_nest": (202, 160, 74), "honeycomb_block": (229, 148, 29), "dried_kelp_block": (50, 58, 38), "slime_block": (111, 192, 91),
    "prismarine": (99, 156, 151), "end_stone": (219, 222, 158), "purpur_block": (169, 125, 169), "glow_lichen": (112, 131, 121),
    "cobweb": (228, 233, 234), "white_concrete": (207, 213, 214), "black_concrete": (8, 10, 15), "gray_concrete": (54, 57, 61),
    "light_gray_concrete": (125, 125, 115), "melon": (114, 146, 30), "sea_lantern": (172, 199, 190), "shroomlight": (240, 146, 70),
    "redstone_lamp": (95, 54, 30), "crying_obsidian": (32, 10, 60), "basalt": (80, 81, 86), "blackstone": (42, 36, 41),
    "soul_soil": (75, 57, 46), "crimson_nylium": (130, 31, 31), "warped_nylium": (43, 114, 101),
    "nether_wart_block": (114, 2, 2), "warped_wart_block": (22, 119, 121), "ancient_debris": (96, 64, 56),
    "raw_copper_block": (154, 105, 79), "nether_quartz_ore": (117, 65, 62), "nether_gold_ore": (115, 54, 42),
}
EMIT_LV = {"soul_fire": 10, "lantern": 15, "soul_lantern": 10, "sea_lantern": 15, "shroomlight": 15, "redstone_wall_torch": 7,
           "wall_torch": 14, "soul_torch": 10, "soul_wall_torch": 10, "glow_lichen": 7, "magma_block": 3,
           "calibrated_sculk_sensor": 1, "sculk_sensor": 1, "amethyst_cluster": 5, "large_amethyst_bud": 4}
CROSS = {"tall_grass": (90, 150, 60, 1.0), "large_fern": (80, 135, 55, 1.0), "short_dry_grass": (180, 160, 95, .5),
         "tall_dry_grass": (185, 165, 100, .9), "oxeye_daisy": (225, 225, 210, .6), "azure_bluet": (215, 225, 230, .5),
         "allium": (175, 110, 210, .6), "blue_orchid": (40, 160, 210, .6), "cornflower": (70, 100, 200, .6),
         "lily_of_the_valley": (230, 230, 230, .6), "red_tulip": (200, 40, 30, .6), "orange_tulip": (225, 120, 30, .6),
         "white_tulip": (225, 230, 225, .6), "pink_tulip": (230, 160, 200, .6), "sweet_berry_bush": (60, 100, 50, .7),
         "seagrass": (50, 120, 40, .9), "tall_seagrass": (50, 120, 40, 1.0), "kelp": (70, 130, 40, 1.0),
         "kelp_plant": (70, 130, 40, 1.0), "lilac": (200, 150, 200, 1.0), "rose_bush": (160, 40, 40, 1.0),
         "peony": (220, 180, 220, 1.0), "sunflower": (240, 200, 40, 1.0), "carrots": (90, 160, 50, .5),
         "potatoes": (90, 160, 50, .5), "beetroots": (90, 160, 50, .5), "pointed_dripstone": (134, 107, 92, 1.0),
         "small_amethyst_bud": (160, 120, 220, .3), "medium_amethyst_bud": (160, 120, 220, .45),
         "large_amethyst_bud": (160, 120, 220, .6), "amethyst_cluster": (170, 130, 230, .75), "bush": (80, 130, 50, 1.0),
         "firefly_bush": (80, 120, 50, 1.0), "leaf_litter": (150, 110, 60, .06), "crimson_fungus": (150, 30, 30, .55),
         "warped_fungus": (20, 140, 130, .55), "crimson_roots": (125, 8, 41, .8), "warped_roots": (20, 150, 140, .8),
         "weeping_vines": (125, 8, 41, 1.0), "weeping_vines_plant": (125, 8, 41, 1.0), "twisting_vines": (20, 150, 140, 1.0),
         "twisting_vines_plant": (20, 150, 140, 1.0), "soul_fire": (60, 200, 220, .9), "nether_sprouts": (20, 150, 140, .2)}
AIRISH = {"air", "cave_air", "void_air", "light", "structure_void", "barrier", "moving_piston"}

_STATE: dict[str, tuple[int, int]] = {}
_NEXT = [256]
_FACE = {"north": (0, 0, .875, 1, 1, 1), "south": (0, 0, 0, 1, 1, .125), "west": (.875, 0, 0, 1, 1, 1), "east": (0, 0, 0, .125, 1, 1)}
_OPP = {"north": "south", "south": "north", "east": "west", "west": "east"}


def _new(rgb, full=False, boxes=None, emit=0, planky=False, booky=False, loggy=False, alpha=None) -> int:
    v = _NEXT[0]
    if v >= NID:
        return 1  # table full: stone, still visible
    _NEXT[0] += 1
    COL[v, :] = rgb
    FULL[v] = full
    if boxes:
        _box(v, ALL, boxes)
    if emit:
        LIGHTLV[v] = emit
        EMIT[v] = min(1.0, emit / 15 + .1)
    PLANKY[v], BOOKY[v], LOGGY[v] = planky, booky, loggy
    if alpha is not None:
        TRANSP[v] = alpha
    return v


def _wallbox(f, d, a0, a1, y0, y1):
    """A box on the wall a block facing f is attached to (the side opposite f), d deep."""
    return {"south": (a0, y0, 0, a1, y1, d), "north": (a0, y0, 1 - d, a1, y1, 1),
            "east": (0, y0, a0, d, y1, a1), "west": (1 - d, y0, a0, 1, y1, a1)}[f]


def _y(box, y0, y1):
    return (box[0], y0, box[2], box[3], y1, box[5])


def _arms(props, post, arm_w, y0, y1, extra=None):
    bx = [post]
    a0, a1 = .5 - arm_w, .5 + arm_w
    for side, b in (("west", (0, y0, a0, .5, y1, a1)), ("east", (.5, y0, a0, 1, y1, a1)),
                    ("north", (a0, y0, 0, a1, y1, .5)), ("south", (a0, y0, .5, a1, y1, 1))):
        if props.get(side) not in (None, "false", "none"):
            bx.append(b)
            if extra:
                bx.append(_y(b, *extra))
    return bx


def _material(name):
    for wood in sorted(WOOD, key=len, reverse=True):
        if name.startswith(wood + "_"):
            return WOOD[wood], True
    for d in sorted(DYES, key=len, reverse=True):
        if name.startswith(d + "_"):
            return DYE_RGB[d], False
    base = name
    for suf in ("_stairs", "_slab", "_wall", "_button", "_pressure_plate", "_fence_gate", "_fence", "_door", "_trapdoor"):
        if base.endswith(suf):
            base = base[: -len(suf)]
    base = {"cobblestone": "cobblestone", "stone": "stone", "smooth_stone": "smooth_stone", "brick": "bricks",
            "stone_brick": "stone_bricks", "sandstone": "sandstone", "iron": "iron_block", "nether_brick": "nether_bricks",
            "deepslate_tile": "deepslate_tiles", "deepslate_brick": "deepslate_bricks", "mossy_cobblestone": "mossy_cobblestone",
            "polished_blackstone": "blackstone", "light_weighted": "gold_block", "heavy_weighted": "iron_block"}.get(base, base)
    if base in BETA_OF:
        i, m = BETA_OF[base]
        return tuple(COL[i, m].astype(int)), False
    if base in FULL_RGB:
        return FULL_RGB[base], False
    return (255, 0, 255), False


def modern(name: str, props: dict) -> tuple[int, int]:
    """(id, meta) for a modern block state ("minecraft:" already stripped)."""
    key = name + repr(sorted(props.items()))
    if key in _STATE:
        return _STATE[key]
    _STATE[key] = r = _modern(name, props)
    return r


def _modern(n: str, p: dict) -> tuple[int, int]:
    if n in AIRISH:
        return 0, 0
    if n.startswith("waxed_"):
        n = n[6:]
    if n == "wheat":
        return 59, int(p.get("age", 7))
    if n in BETA_OF:
        return BETA_OF[n]
    for i, d in enumerate(DYES):
        if n == d + "_wool":
            return 35, i
    if n.endswith("_leaves"):
        return 18, 0
    rgb, wooden = _material(n)
    f = p.get("facing", "north")
    if n.endswith("_stairs"):
        top = p.get("half") == "top"
        step = {"east": (.5, 0, 0, 1, .5, 1), "west": (0, 0, 0, .5, .5, 1), "south": (0, 0, .5, 1, .5, 1),
                "north": (0, 0, 0, 1, .5, .5)}[f]
        slab, up = ((0, .5, 0, 1, 1, 1), step) if top else ((0, 0, 0, 1, .5, 1), _y(step, .5, 1))
        return _new(rgb, boxes=[slab, up], planky=wooden), 0
    if n.endswith("_slab"):
        t = p.get("type", "bottom")
        if t == "double":
            return _new(rgb, full=True, planky=wooden), 0
        return _new(rgb, boxes=[(0, .5, 0, 1, 1, 1) if t == "top" else (0, 0, 0, 1, .5, 1)], planky=wooden), 0
    if n.endswith("_fence"):
        return _new(rgb, boxes=_arms(p, (.375, 0, .375, .625, 1, .625), .06, .375, .5625, (.75, .9375)), planky=wooden), 0
    if n.endswith("_fence_gate"):
        ew = f in ("east", "west")
        return _new(rgb, boxes=[(.44, .3, 0, .56, .95, 1) if ew else (0, .3, .44, 1, .95, .56)], planky=wooden), 0
    if n.endswith("_wall") and not n.endswith("_wall_sign") and not n.endswith("_wall_banner"):
        return _new(rgb, boxes=_arms(p, (.25, 0, .25, .75, 1, .75), .19, 0, .875)), 0
    if n.endswith("_pane") or n in ("iron_bars", "iron_chain", "chain"):
        if n in ("iron_chain", "chain"):
            return _new((60, 64, 72), boxes=[(.44, 0, .44, .56, 1, .56)]), 0
        col = (110, 110, 115) if n == "iron_bars" else (215, 230, 235)
        return _new(col, boxes=_arms(p, (.44, 0, .44, .56, 1, .56), .06, 0, 1)), 0
    if n.endswith("_door"):
        closed = p.get("open", "false") != "true"
        right = p.get("hinge") == "right"
        side = f if closed else {"east": ("north" if right else "south"), "south": ("east" if right else "west"),
                                  "west": ("south" if right else "north"), "north": ("west" if right else "east")}[f]
        panel = {"east": (0, 0, 0, .1875, 1, 1), "west": (.8125, 0, 0, 1, 1, 1), "south": (0, 0, 0, 1, 1, .1875),
                 "north": (0, 0, .8125, 1, 1, 1)}[side]
        return _new(rgb if rgb != (255, 0, 255) else (200, 200, 200), boxes=[panel], planky=wooden), 0
    if n.endswith("_trapdoor"):
        if p.get("open") == "true":
            box = {"north": (0, 0, .8125, 1, 1, 1), "south": (0, 0, 0, 1, 1, .1875), "west": (.8125, 0, 0, 1, 1, 1),
                   "east": (0, 0, 0, .1875, 1, 1)}[f]
        else:
            box = (0, .8125, 0, 1, 1, 1) if p.get("half") == "top" else (0, 0, 0, 1, .1875, 1)
        return _new(rgb, boxes=[box], planky=wooden), 0
    if n.endswith("_wall_sign") or n.endswith("_wall_hanging_sign"):
        b = _FACE[_OPP[f]]
        return _new(rgb, boxes=[(b[0], .28, b[2], b[3], .78, b[5])], planky=True), 0
    if n.endswith("_hanging_sign"):
        return _new(rgb, boxes=[(.06, 0, .44, .94, .62, .56)], planky=True), 0
    if n.endswith("_sign"):
        ew = int(p.get("rotation", 0)) % 8 in (3, 4, 5)
        return _new(rgb, boxes=[(.44, 0, .44, .56, .55, .56), (.44, .5, 0, .56, 1, 1) if ew else (0, .5, .44, 1, 1, .56)],
                    planky=True), 0
    if n.endswith("_wall_banner"):
        b = _FACE[_OPP[f]]
        return _new(rgb, boxes=[(max(b[0], .06), 0, b[2], min(b[3], .94), 1, b[5])]), 0
    if n.endswith("_banner"):
        ew = int(p.get("rotation", 0)) % 8 in (3, 4, 5)
        return _new(rgb, boxes=[(.44, 0, .44, .56, 1, .56), (.44, .3, .06, .56, 1, .94) if ew else (.06, .3, .44, .94, 1, .56)]), 0
    if n.endswith("_carpet"):
        return _new(rgb, boxes=[(0, 0, 0, 1, .0625, 1)]), 0
    if n.endswith("_bed"):
        return _new(rgb, boxes=[(0, 0, 0, 1, .56, 1)]), 0
    if n.endswith("_button"):
        b = {"floor": (.31, 0, .375, .69, .125, .625), "ceiling": (.31, .875, .375, .69, 1, .625)}.get(p.get("face"))
        return _new(rgb, boxes=[b or _wallbox(f, .125, .31, .69, .375, .625)]), 0
    if n.endswith("_pressure_plate"):
        return _new(rgb, boxes=[(.06, 0, .06, .94, .06, .94)], planky=wooden), 0
    if n.endswith("_log") or n.endswith("_wood") or n.endswith("_stem") or n.endswith("_hyphae"):
        if n.startswith("stripped_"):
            wood = n[9:].rsplit("_", 1)[0]
            return _new(STRIPPED.get(wood, (170, 140, 90)), full=True, loggy=True), 0
        wood = n.rsplit("_", 1)[0]
        return _new(BARK.get(wood, (100, 80, 50)), full=True, loggy=True), 0
    if n.endswith("_planks"):
        return _new(rgb, full=True, planky=True), 0
    if n.endswith("_stained_glass"):
        return _new(rgb, alpha=.45), 0
    if n.endswith(("_concrete", "_terracotta", "_concrete_powder", "_glazed_terracotta")) and n.split("_")[0] in [d.split("_")[0] for d in DYES]:
        return _new(rgb, full=True), 0
    if n.endswith("_sapling") or n.endswith("_mushroom") or n.endswith("_tulip") or n in CROSS:
        r, g, b, h = CROSS.get(n, (70, 130, 40, .8))
        return _new((r, g, b), boxes=[(0, 0, .48, 1, h, .52), (.48, 0, 0, .52, h, 1)], emit=EMIT_LV.get(n, 0)), 0
    if n in ("wall_torch", "redstone_wall_torch", "soul_wall_torch"):
        b = {"east": (0, .2, .44, .2, .82, .56), "west": (.8, .2, .44, 1, .82, .56), "south": (.44, .2, 0, .56, .82, .2),
             "north": (.44, .2, .8, .56, .82, 1)}[f]
        col = (215, 45, 20) if n.startswith("redstone") else (255, 214, 90)
        return _new(col, boxes=[b], emit=EMIT_LV[n]), 0
    if n == "soul_torch":
        return _new((110, 220, 230), boxes=[(.44, 0, .44, .56, .62, .56)], emit=10), 0
    if n == "ladder":
        return _new(COL[65, 0], boxes=[_FACE[f]], planky=True), 0
    if n in ("lantern", "soul_lantern"):
        hang = p.get("hanging") == "true"
        return _new((70, 70, 75) if n == "lantern" else (60, 80, 90),
                    boxes=[(.31, .06, .31, .69, .62, .69), (.44, .62, .44, .56, 1, .56)] if hang else [(.31, 0, .31, .69, .56, .69)],
                    emit=EMIT_LV[n]), 0
    if n == "lever":
        face = p.get("face", "wall")
        if face == "floor":
            bx = [(.31, 0, .25, .69, .19, .75), (.44, .19, .44, .56, .7, .56)]
        elif face == "ceiling":
            bx = [(.31, .81, .25, .69, 1, .75), (.44, .3, .44, .56, .81, .56)]
        else:
            bx = [_wallbox(f, .19, .31, .69, .25, .75), _wallbox(f, .6, .44, .56, .45, .55)]
        return _new((110, 110, 110), boxes=bx), 0
    if n in ("redstone_wire",):
        pw = int(p.get("power", 0))
        return _new((90 + 165 * pw // 15, 8, 8), boxes=[(0, 0, 0, 1, .03, 1)]), 0
    if n.endswith("rail"):
        return _new((130, 120, 105), boxes=[(0, 0, 0, 1, .0625, 1)]), 0
    if n in ("chest", "ender_chest", "trapped_chest"):
        return _new(COL[54, 0], boxes=[(.06, 0, .06, .94, .875, .94)]), 0
    if n in ("calibrated_sculk_sensor", "sculk_sensor"):
        return _new((30, 78, 90), boxes=[(0, 0, 0, 1, .5, 1), (.2, .5, .2, .26, 1, .26), (.74, .5, .74, .8, 1, .8)], emit=1), 0
    if n == "dirt_path":
        return _new((148, 122, 65), boxes=[(0, 0, 0, 1, .9375, 1)]), 0
    if n == "chiseled_bookshelf":
        return _new((160, 130, 80), full=True, booky=True), 0
    if n == "flower_pot" or n.startswith("potted_"):
        return _new((125, 70, 50), boxes=[(.31, 0, .31, .69, .375, .69)]), 0
    if n in ("snow",):
        return _new((240, 250, 250), boxes=[(0, 0, 0, 1, int(p.get("layers", 1)) / 8, 1)]), 0
    if n == "lily_pad":
        return _new((40, 110, 40), boxes=[(0, 0, 0, 1, .03, 1)]), 0
    if n in ("vine", "glow_lichen", "sculk_vein"):
        return _new(FULL_RGB.get(n, (60, 110, 40)), boxes=[(0, 0, 0, 1, .03, 1)], emit=EMIT_LV.get(n, 0)), 0
    if n in ("cobweb",):
        return _new(FULL_RGB["cobweb"], boxes=[(0, .2, .48, 1, .8, .52), (.48, .2, 0, .52, .8, 1)]), 0
    if n.endswith("candle") or n.endswith("_candle"):
        return _new((220, 210, 180), boxes=[(.44, 0, .44, .56, .4, .56)], emit=3 if p.get("lit") == "true" else 0), 0
    if n in FULL_RGB:
        return _new(FULL_RGB[n], full=True, emit=EMIT_LV.get(n, 0)), 0
    if n.endswith("_ore"):
        base = FULL_RGB["deepslate"] if n.startswith("deepslate_") else COL[1, 0]
        return _new(base, full=True), 0
    if n.endswith("_copper") or n.startswith("cut_copper") or n.endswith("_cut_copper"):
        return _new(FULL_RGB["copper_block"], full=True), 0
    return _new((255, 0, 255), full=True), 0   # unknown: a full magenta block, so it stands out


# light: Beta's brightness table
LIGHT = np.array([(1 - f) / (f * 3 + 1) * 0.95 + 0.05 for f in (1 - np.arange(16) / 15)], np.float32)
FACE_SHADE = np.array([0.6, 0.6, 0.5, 1.0, 0.8, 0.8], np.float32)  # -x +x -y +y -z +z


def celestial(t: int) -> float:
    f = ((t % 24000) / 24000.0) - 0.25
    if f < 0:
        f += 1
    if f > 1:
        f -= 1
    g = f
    f = 1 - (math.cos(f * math.pi) + 1) / 2
    return g + (f - g) / 3


def daylight(t: int) -> tuple[float, int]:
    a = celestial(t) * 2 * math.pi
    f = min(max(math.cos(a) * 2 + 0.5, 0), 1)
    return f, int((1 - f) * 11)


# ---- the world volume ----------------------------------------------------------------------

class Volume:
    def __init__(self, w, x0: int, z0: int, x1: int, z1: int):
        cx0, cz0, cx1, cz1 = x0 >> 4, z0 >> 4, x1 >> 4, z1 >> 4
        self.ox, self.oz = cx0 * 16, cz0 * 16
        self.boxes: list[tuple[np.ndarray, np.ndarray, np.ndarray]] = []   # entity boxes: lo, hi (world), rgb 0..1
        self.sheep: list[tuple[float, float, float, int, bool]] = []
        if isinstance(w, AnvilWorld):
            self._anvil(w, cx0, cz0, cx1, cz1)
            return
        W, D = (cx1 - cx0 + 1) * 16, (cz1 - cz0 + 1) * 16
        self.ids = np.zeros((W, D, 128), np.uint8)
        self.meta = np.zeros((W, D, 128), np.uint8)
        self.sky = np.full((W, D, 128), 15, np.uint8)
        self.blk = np.zeros((W, D, 128), np.uint8)
        for cx in range(cx0, cx1 + 1):
            for cz in range(cz0, cz1 + 1):
                try:
                    lv = w.level(cx * 16, cz * 16)
                except KeyError:
                    continue
                sx, sz = cx * 16 - self.ox, cz * 16 - self.oz
                sl = (slice(sx, sx + 16), slice(sz, sz + 16))
                self.ids[sl] = np.frombuffer(bytes(lv["Blocks"].value), np.uint8).reshape(16, 16, 128)
                self.meta[sl] = _nibbles(lv["Data"].value)
                self.sky[sl] = _nibbles(lv["SkyLight"].value)
                self.blk[sl] = _nibbles(lv["BlockLight"].value)
                for e in lv["Entities"].value.items:
                    d = e.value
                    if d.get("id") and d["id"].value == "Sheep":
                        p = [t.value for t in d["Pos"].value.items]
                        self.sheep.append((p[0], p[1], p[2], int(d["Color"].value) & 15 if "Color" in d else 0,
                                           bool(d["Sheared"].value) if "Sheared" in d else False))
        f = self.ids == 85   # fence neighbour masks
        m = np.zeros(f.shape, np.uint8)
        m[1:] |= np.where(f[:-1], 1, 0).astype(np.uint8)
        m[:-1] |= np.where(f[1:], 2, 0).astype(np.uint8)
        m[:, 1:] |= np.where(f[:, :-1], 4, 0).astype(np.uint8)
        m[:, :-1] |= np.where(f[:, 1:], 8, 0).astype(np.uint8)
        self.meta[f] = m[f]
        # index as [x, y, z]
        self.ids, self.meta, self.sky, self.blk = (a.transpose(0, 2, 1).copy() for a in (self.ids, self.meta, self.sky, self.blk))
        self.ids = self.ids.astype(np.uint16)

    def _anvil(self, w, cx0, cz0, cx1, cz1, height=ANVIL_TOP):
        """Blocks from y 0 up to height (below 0 is only the deepslate the upgrade filled in). Light is
        computed here: Paper's saved light is often missing or stale (isLightOn 0 after --forceUpgrade)."""
        W, D = (cx1 - cx0 + 1) * 16, (cz1 - cz0 + 1) * 16
        self.ids = np.zeros((W, height, D), np.uint16)
        self.meta = np.zeros((W, height, D), np.uint8)
        for cx in range(cx0, cx1 + 1):
            for cz in range(cz0, cz1 + 1):
                root = w.chunk(cx, cz)
                if root is None:
                    continue
                sx, sz = cx * 16 - self.ox, cz * 16 - self.oz
                for sec in root["sections"].value.items:
                    sy = sec.value["Y"].value * 16
                    if sy < 0 or sy >= height:
                        continue
                    got = section_states(sec.value)
                    if got is None:
                        continue
                    pal, idx = got
                    lut = np.array([modern(nm.split(":", 1)[-1], pr) for nm, pr in pal], np.int32)
                    blk = lut[idx]                                  # [y, z, x, 2]
                    self.ids[sx:sx + 16, sy:sy + 16, sz:sz + 16] = blk[..., 0].transpose(2, 0, 1)
                    self.meta[sx:sx + 16, sy:sy + 16, sz:sz + 16] = blk[..., 1].transpose(2, 0, 1)
                for e in w.entities(cx, cz):
                    self._entity(e)
        self.sky, self.blk = _compute_light(self.ids)

    def _entity(self, d: dict):
        eid = d["id"].value if "id" in d else ""
        if eid not in ("minecraft:item_frame", "minecraft:glow_item_frame"):
            return
        bp = d["block_pos"].value if "block_pos" in d else [d["TileX"].value, d["TileY"].value, d["TileZ"].value]
        t = np.array(bp, np.float64)
        face = d["Facing"].value if "Facing" in d else 3
        # the frame lies on the face of the block it hangs on: the side of its cell opposite Facing
        lo, hi = np.array([.125, .125, .125]), np.array([.875, .875, .875])
        ax, back = {0: (1, 1), 1: (1, 0), 2: (2, 1), 3: (2, 0), 4: (0, 1), 5: (0, 0)}[face]
        lo[ax], hi[ax] = (1 - .0625, 1) if back else (0, .0625)
        has_map = "Item" in d and d["Item"].value.get("id") is not None and "map" in d["Item"].value["id"].value
        self.boxes.append((t + lo, t + hi, np.array((120, 85, 50)) / 255.0))
        if has_map:   # the map face, a hair proud of the frame
            lo2, hi2 = lo.copy(), hi.copy()
            lo2[[a for a in range(3) if a != ax]] += .06
            hi2[[a for a in range(3) if a != ax]] -= .06
            lo2[ax], hi2[ax] = (lo[ax] - .01, lo[ax]) if back else (hi[ax], hi[ax] + .01)
            self.boxes.append((t + lo2, t + hi2, np.array((222, 210, 170)) / 255.0))


def _compute_light(ids: np.ndarray) -> tuple[np.ndarray, np.ndarray]:
    """Sky light (15 down each open column, then spread sideways and down, -1 per step) and block
    light from emitters, both through non-opaque cells. Close enough to read a dark room as dark."""
    opaque = FULL[ids] & ~np.isin(ids, [18, 20, 79])
    blocked = np.maximum.accumulate(opaque[:, ::-1, :], axis=1)[:, ::-1, :]
    sky = np.where(blocked, 0, 15).astype(np.int16)
    blk = LIGHTLV[ids].astype(np.int16)
    for a in (sky, blk):
        for _ in range(14):
            n = a.copy()
            n[1:] = np.maximum(n[1:], a[:-1]); n[:-1] = np.maximum(n[:-1], a[1:])
            n[:, 1:] = np.maximum(n[:, 1:], a[:, :-1]); n[:, :-1] = np.maximum(n[:, :-1], a[:, 1:])
            n[:, :, 1:] = np.maximum(n[:, :, 1:], a[:, :, :-1]); n[:, :, :-1] = np.maximum(n[:, :, :-1], a[:, :, 1:])
            n -= 1
            np.maximum(a, np.where(opaque, 0, n), out=a)
    return sky.clip(0, 15).astype(np.uint8), blk.clip(0, 15).astype(np.uint8)


def _nibbles(buf) -> np.ndarray:
    d = np.frombuffer(bytes(buf), np.uint8)
    out = np.empty(d.size * 2, np.uint8)
    out[0::2] = d & 15
    out[1::2] = d >> 4
    return out.reshape(16, 16, 128)


# ---- rendering -----------------------------------------------------------------------------

def _hash(*a):
    h = np.uint32(2166136261)
    for v in a:
        h = (h ^ (np.asarray(v).astype(np.int64) & 0xFFFFFFFF).astype(np.uint32)) * np.uint32(16777619)
    h ^= h >> np.uint32(13)
    h *= np.uint32(1274126177)
    return (h & np.uint32(0xFFFF)).astype(np.float32) / 65535.0


def render(vol: Volume, cam, yaw: float, pitch: float, time: int, size=(960, 540), fov=70.0, maxd=220.0) -> Image.Image:
    Wd, Ht = size
    yr, pr = math.radians(yaw), math.radians(pitch)
    fwd = np.array([-math.sin(yr) * math.cos(pr), -math.sin(pr), math.cos(yr) * math.cos(pr)])
    right = np.cross(fwd, [0, 1, 0]); right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    th = math.tan(math.radians(fov) / 2)
    u = ((np.arange(Wd) + .5) / Wd * 2 - 1) * th * Wd / Ht
    v = (1 - (np.arange(Ht) + .5) / Ht * 2) * th
    U, V = np.meshgrid(u, v)
    D = fwd[None, None] + U[..., None] * right + V[..., None] * up
    D = (D / np.linalg.norm(D, axis=-1, keepdims=True)).reshape(-1, 3)
    N = D.shape[0]
    O = np.array([cam[0] - vol.ox, cam[1], cam[2] - vol.oz], np.float64)

    day, darken = daylight(time)
    a = celestial(time) * 2 * math.pi
    sun = np.array([0, math.cos(a), math.sin(a)])
    skycol = _sky(D, sun, day)

    out = skycol.copy()
    trans = np.ones(N, np.float32)           # remaining transmittance (glass, water)
    tint = np.zeros((N, 3), np.float32)
    hit_t = np.full(N, np.inf)

    with np.errstate(divide="ignore", invalid="ignore"):
        inv = 1.0 / D
    step = np.sign(D).astype(np.int64)
    cell = np.floor(np.broadcast_to(O, D.shape)).astype(np.int64)
    tdelta = np.abs(inv)
    tmax = np.where(step > 0, (cell + 1 - O) * inv, np.where(step < 0, (cell - O) * inv, np.inf))
    tenter = np.zeros(N)
    axis = np.full(N, -1)
    act = np.arange(N)
    X, Y, Z = vol.ids.shape

    for _ in range(int(maxd * 2.2)):
        if act.size == 0:
            break
        c = cell[act]
        inb = (c[:, 0] >= 0) & (c[:, 0] < X) & (c[:, 2] >= 0) & (c[:, 2] < Z) & (c[:, 1] >= 0) & (c[:, 1] < Y)
        gone = ~inb | (tenter[act] > maxd)
        act = act[~gone]
        c = c[~gone]
        if act.size == 0:
            break
        bid = vol.ids[c[:, 0], c[:, 1], c[:, 2]]
        bm = vol.meta[c[:, 0], c[:, 1], c[:, 2]]
        done = np.zeros(act.size, bool)

        full = FULL[bid] & (tenter[act] > 0)
        if full.any():
            r = act[full]
            ax = axis[r]
            n = np.zeros((r.size, 3)); n[np.arange(r.size), np.clip(ax, 0, 2)] = -step[r, np.clip(ax, 0, 2)]
            _shade(vol, out, trans, tint, hit_t, r, O, D, tenter[r], n, bid[full], bm[full], c[full], darken, sun, day)
            done |= full

        shp = SHAPED[bid] & ~done
        if shp.any():
            r = act[shp]
            boxes = BOX[bid[shp], bm[shp]]              # (n, K, 6)
            lo = c[shp][:, None, :] + np.nan_to_num(boxes[..., :3], nan=0)
            hi = c[shp][:, None, :] + np.nan_to_num(boxes[..., 3:], nan=-1)
            o = O[None, None, :]; iv = inv[r][:, None, :]
            with np.errstate(invalid="ignore"):
                t1 = (lo - o) * iv; t2 = (hi - o) * iv
            tn = np.nanmax(np.minimum(t1, t2), axis=-1); tf = np.nanmin(np.maximum(t1, t2), axis=-1)
            ok = (tf >= tn) & (tf > 0) & ~np.isnan(boxes[..., 0])
            tn = np.where(ok, tn, np.inf)
            k = np.argmin(tn, axis=1)
            tb = tn[np.arange(r.size), k]
            got = np.isfinite(tb)
            if got.any():
                rr = r[got]; kk = k[got]
                t1g = np.minimum(t1, t2)[got, kk]
                na = np.argmax(t1g, axis=1)
                n = np.zeros((rr.size, 3)); n[np.arange(rr.size), na] = -step[rr, na]
                cc = c[shp][got]
                _shade(vol, out, trans, tint, hit_t, rr, O, D, np.maximum(tb[got], 0), n, bid[shp][got], bm[shp][got], cc, darken, sun, day, inside=True)
                idx = np.where(shp)[0][got]
                done[idx] = True

        tr = ~done & np.isin(bid, list(TRANSP)) & (tenter[act] > 0)
        if tr.any():
            r = act[tr]
            b = bid[tr]
            p = O + D[r] * tenter[r][:, None]
            ax = np.clip(axis[r], 0, 2)
            fu, fv = _face_uv(p, ax)
            col = COL[b, 0] / 255.0
            alpha = np.where(b == 20, np.where((fu < 1 / 16) | (fu > 15 / 16) | (fv < 1 / 16) | (fv > 15 / 16)
                                                | (np.abs(fu - fv - .2) < .04) | (np.abs(fu - fv + .35) < .03), .75, .06),
                             np.vectorize(TRANSP.get)(b))
            # don't stack a second face of the same glass/water body
            prevc = c[tr] - np.eye(3, dtype=np.int64)[ax] * step[r, ax][:, None]
            pin = (prevc[:, 0] >= 0) & (prevc[:, 0] < X) & (prevc[:, 2] >= 0) & (prevc[:, 2] < Z) & (prevc[:, 1] >= 0) & (prevc[:, 1] < Y)
            same = np.zeros(r.size, bool)
            same[pin] = vol.ids[prevc[pin, 0], prevc[pin, 1], prevc[pin, 2]] == b[pin]
            alpha = np.where(same & (b != 8) & (b != 9), 0, alpha)
            lv = _light(vol, prevc, pin, darken)
            col = col * lv[:, None] * np.where(b == 20, 1.0, 1.0)[:, None]
            tint[r] += trans[r][:, None] * alpha[:, None] * col
            trans[r] *= (1 - alpha)
            water_done = trans[r] < 0.03
            if water_done.any():
                hit_t[r[water_done]] = tenter[r[water_done]]
                done[np.where(tr)[0][water_done]] = True

        act = act[~done]
        if act.size == 0:
            break
        tm = tmax[act]
        ax = np.argmin(tm, axis=1)
        ar = np.arange(act.size)
        tenter[act] = tm[ar, ax]
        cell[act, ax] += step[act, ax]
        tmax[act, ax] += tdelta[act, ax]
        axis[act] = ax

    # sheep
    for sx, sy, sz, color, sheared in vol.sheep:
        lo = np.array([sx - vol.ox - .45, sy + .25, sz - vol.oz - .45]); hi = lo + np.array([.9, 1.05, .9])
        with np.errstate(divide="ignore", invalid="ignore"):
            t1 = (lo - O) * inv; t2 = (hi - O) * inv
        tn = np.max(np.minimum(t1, t2), axis=1); tf = np.min(np.maximum(t1, t2), axis=1)
        got = (tf >= tn) & (tn > 0) & (tn < hit_t)
        if got.any():
            r = np.where(got)[0]
            p = O + D[r] * tn[r][:, None]
            cell_ = np.floor(p).astype(np.int64)
            inb = (cell_[:, 0] >= 0) & (cell_[:, 0] < X) & (cell_[:, 2] >= 0) & (cell_[:, 2] < Z)
            lv = np.full(r.size, LIGHT[15 - darken])
            lv[inb] = _light(vol, cell_[inb], np.ones(inb.sum(), bool), darken)
            base = COL[35, color] / 255.0 * (0.85 if sheared else 1.0)
            n = 0.75 + 0.25 * _hash(r // 7)
            out[r] = (trans[r][:, None] * base[None] * (lv * n)[:, None] + tint[r])
            hit_t[r] = tn[r]

    # entity boxes (item frames and their maps)
    for lo, hi, rgb in vol.boxes:
        lo_, hi_ = lo - [vol.ox, 0, vol.oz], hi - [vol.ox, 0, vol.oz]
        with np.errstate(divide="ignore", invalid="ignore"):
            t1 = (lo_ - O) * inv; t2 = (hi_ - O) * inv
        tn = np.max(np.minimum(t1, t2), axis=1); tf = np.min(np.maximum(t1, t2), axis=1)
        got = (tf >= tn) & (tn > 0) & (tn < hit_t)
        if got.any():
            r = np.where(got)[0]
            cell_ = np.floor((lo_ + hi_) / 2).astype(np.int64)[None].repeat(r.size, 0)
            inb = (cell_[:, 0] >= 0) & (cell_[:, 0] < X) & (cell_[:, 2] >= 0) & (cell_[:, 2] < Z) & (cell_[:, 1] >= 0) & (cell_[:, 1] < Y)
            lv = _light(vol, cell_, inb, darken)
            out[r] = trans[r][:, None] * rgb[None] * lv[:, None] + tint[r]
            hit_t[r] = tn[r]

    miss = ~np.isfinite(hit_t)
    out[miss] = trans[miss][:, None] * skycol[miss] + tint[miss]
    # fog toward the sky colour
    fogc = _fog(sun, day)
    f = np.clip((hit_t - maxd * .45) / (maxd * .55), 0, 1)[:, None]
    f[miss] = 0
    out = out * (1 - f) + fogc[None] * f
    img = np.clip(out.reshape(Ht, Wd, 3) * 255, 0, 255).astype(np.uint8)
    return Image.fromarray(img)


def _face_uv(p, ax):
    fr = p - np.floor(p)
    u = np.where(ax == 0, fr[:, 2], fr[:, 0])
    v = np.where(ax == 1, fr[:, 2], fr[:, 1])
    return u, v


AMBIENT = [0]   # --ambient N: a floor under every light level, to inspect dark rooms and caves


def _light(vol, c, ok, darken):
    lv = np.full(c.shape[0], LIGHT[max(15 - darken, 0)], np.float32)
    if ok.any():
        cc = c[ok]
        s = vol.sky[cc[:, 0], cc[:, 1], cc[:, 2]].astype(np.int32) - darken
        b = vol.blk[cc[:, 0], cc[:, 1], cc[:, 2]].astype(np.int32)
        lv[ok] = LIGHT[np.clip(np.maximum(np.maximum(s, b), AMBIENT[0]), 0, 15)]
    return lv


def _shade(vol, out, trans, tint, hit_t, r, O, D, t, n, bid, bm, cell, darken, sun, day, inside=False):
    p = O + D[r] * t[:, None]
    ax = np.argmax(np.abs(n), axis=1)
    sgn = n[np.arange(r.size), ax]
    face = ax * 2 + (sgn > 0)
    u, v = _face_uv(p - n * 1e-4, ax)
    tu, tv = np.floor(u * 16).astype(np.int64), np.floor(v * 16).astype(np.int64)
    col = COL[bid, bm & 15].copy() / 255.0
    noise = _hash(cell[:, 0], cell[:, 1], cell[:, 2], tu, tv, face) - .5
    amp = np.full(r.size, .08, np.float32)
    amp[np.isin(bid, [4, 13, 48, 67])] = .22
    amp[np.isin(bid, [3, 2, 60, 18])] = .14
    coarse = _hash(cell[:, 0], cell[:, 1], cell[:, 2], tu // 4, tv // 4, face) - .5
    shade = 1 + noise * amp + np.where(np.isin(bid, [4, 48, 67, 13]), coarse * .18, 0)
    side = ax != 1
    # grass: green top, dirt sides with a green fringe
    g = bid == 2
    col[g & side] = np.where((v[g & side] > .8)[:, None], COL[2, 0] / 255.0, COL[3, 0] / 255.0)
    col[g & (face == 2)] = COL[3, 0] / 255.0
    # planks-like: board lines
    plank = PLANKY[bid] | ((bid == 44) & ((bm & 7) == 2)) | ((bid == 43) & ((bm & 7) == 2))
    shade *= np.where(plank & (tv % 4 == 3), .78, 1)
    # bricks
    br = bid == 45
    mortar = br & ((tv % 4 == 3) | (((tu + (tv // 4) % 2 * 4) % 8) == 7))
    col[mortar] = np.array([.68, .64, .6])
    # logs: bark stripes on the sides, rings on the ends
    lg = LOGGY[bid]
    col[lg & ~side] = np.array([.66, .54, .34])
    shade *= np.where(lg & side & (tu % 3 == 0), .8, 1)
    # bookshelf spines
    bk = BOOKY[bid] & side & (((tv >= 1) & (tv <= 6)) | ((tv >= 9) & (tv <= 14)))
    if bk.any():
        hue = _hash(tu[bk] // 2, tv[bk] // 8, cell[bk, 0], cell[bk, 2])
        col[bk] = np.stack([.35 + .4 * hue, .2 + .3 * (1 - hue), .15 + .2 * hue], 1)
    # slabs and double slabs: the stone seam
    seam = ((bid == 43) & side & (tv == 7)) | ((bid == 44) & side & (tv == 0))
    shade *= np.where(seam, .75, 1)
    # sandstone sides: a lighter top band
    ss = (bid == 24) & side
    shade *= np.where(ss & (tv >= 12), 1.08, 1)
    # leaves: holes read darker
    lf = bid == 18
    shade *= np.where(lf & (noise > .3), .55, 1)
    # light: from the air cell in front of the face (or the cell itself for shaped blocks)
    front = cell + n.astype(np.int64) * (0 if inside else 1)
    X, Y, Z = vol.ids.shape
    ok = (front[:, 0] >= 0) & (front[:, 0] < X) & (front[:, 2] >= 0) & (front[:, 2] < Z) & (front[:, 1] >= 0) & (front[:, 1] < Y)
    lv = _light(vol, front, ok, darken)
    if inside:
        up = cell + np.array([0, 1, 0])
        ok2 = (up[:, 1] < Y) & (up[:, 0] >= 0) & (up[:, 0] < X) & (up[:, 2] >= 0) & (up[:, 2] < Z)
        lv = np.maximum(lv, _light(vol, up, ok2, darken))
    # ambient occlusion from the four neighbours of the front cell, in the face plane
    ao = np.ones(r.size, np.float32)
    if not inside:
        for tax in range(3):
            m = ax != tax
            if not m.any():
                continue
            frac = (p[:, tax] - np.floor(p[:, tax] - n[:, tax] * 1e-4))
            for d, w in ((-1, 1 - frac), (1, frac)):
                nb = front.copy(); nb[:, tax] += d
                okn = m & ok & (nb[:, 0] >= 0) & (nb[:, 0] < X) & (nb[:, 2] >= 0) & (nb[:, 2] < Z) & (nb[:, 1] >= 0) & (nb[:, 1] < Y)
                solid = np.zeros(r.size, bool)
                solid[okn] = FULL[vol.ids[nb[okn, 0], nb[okn, 1], nb[okn, 2]]]
                ao *= np.where(solid, 1 - .38 * np.clip(w, 0, 1) ** 2, 1)
    em = EMIT[bid]
    bright = np.maximum(lv * FACE_SHADE[face] * ao, em)
    c = col * (shade * bright)[:, None]
    out[r] = trans[r][:, None] * c + tint[r]
    hit_t[r] = t


def _sky(D, sun, day):
    base = np.array([.47, .65, 1.0]) * day + np.array([.01, .01, .03]) * (1 - day)
    hz = np.array([.75, .85, 1.0]) * day + np.array([.02, .02, .05]) * (1 - day)
    y = np.clip(D[:, 1], -1, 1)
    k = np.clip(1 - np.abs(y) * 3, 0, 1)[:, None]
    col = base[None] * (1 - k) + hz[None] * k
    col[y < -0.05] = hz * .6
    # sunrise / sunset glow toward the sun's horizon
    glow = max(0.0, 1 - abs(sun[1]) * 2.5)
    if glow > 0:
        towards = np.clip(D @ np.array([0, 0, np.sign(sun[2]) or 1]), 0, 1)
        g = (glow * towards ** 2 * np.clip(1 - np.abs(y) * 4, 0, 1))[:, None]
        col = col * (1 - g * .7) + np.array([1.0, .55, .25])[None] * g * .9
    # sun (30/100 square) and moon (20/100 square), square to the north-south axis
    for body, half, rgb in ((sun, .15, (1.0, .95, .7)), (-sun, .10, (.92, .94, 1.0))):
        d = D @ body
        e1 = D[:, 0]
        e2 = D @ np.array([0, -body[2], body[1]])
        on = (d > 0) & (np.abs(e1 / np.where(d > 0, d, 1)) < half) & (np.abs(e2 / np.where(d > 0, d, 1)) < half)
        col[on] = rgb
    return col.astype(np.float32)


def _fog(sun, day):
    return np.array([.75, .85, 1.0]) * day + np.array([.02, .02, .05]) * (1 - day)


# ---- entry points --------------------------------------------------------------------------

def _open(world: str):
    """A Beta (McRegion) world, or an upgraded one (Anvil .mca regions present)."""
    return AnvilWorld(world) if AnvilWorld.detect(world) else World(world)


def view(world: str, cam, yaw, pitch, time=6000, size=(960, 540), radius=150) -> Image.Image:
    w = _open(world)
    x, z = int(cam[0]), int(cam[2])
    vol = Volume(w, x - radius, z - radius, x + radius, z + radius)
    return render(vol, cam, yaw, pitch, time, size)


def save_pose(world: str):
    _, root = nbt.load_file(str(Path(world) / "level.dat"))
    d = root["Data"].value
    p = d["Player"].value
    pos = [t.value for t in p["Pos"].value.items]
    rot = [t.value for t in p["Rotation"].value.items]
    return pos, rot[0], rot[1], int(d["Time"].value) % 24000


def orbit(world: str, target, radius=24, time=6000, size=(640, 360)) -> Image.Image:
    w = _open(world)
    tx, ty, tz = target
    vol = Volume(w, int(tx) - 150, int(tz) - 150, int(tx) + 150, int(tz) + 150)
    sheet = Image.new("RGB", (size[0] * 2, size[1] * 2))
    for i, yaw in enumerate((135, 225, 315, 45)):
        yr = math.radians(yaw)
        cx, cz = tx + math.sin(yr) * radius, tz - math.cos(yr) * radius
        cy = ty + radius * .55
        ix, iz = int(math.floor(cx)) - vol.ox, int(math.floor(cz)) - vol.oz
        if 0 <= ix < vol.ids.shape[0] and 0 <= iz < vol.ids.shape[2]:
            solid = np.nonzero(FULL[vol.ids[ix, :, iz]])[0]
            if solid.size:
                cy = max(cy, solid.max() + 3)
        dx, dy, dz = tx - cx, ty - cy, tz - cz
        y = math.degrees(math.atan2(-dx, dz)) % 360
        p = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
        sheet.paste(render(vol, (cx, cy, cz), y, p, time, size), ((i % 2) * size[0], (i // 2) * size[1]))
    return sheet


def main(argv):
    if len(argv) < 2:
        print(__doc__)
        return 2
    world = argv[1]
    out = argv[argv.index("--out") + 1] if "--out" in argv else "view.png"
    t = int(argv[argv.index("--time") + 1]) if "--time" in argv else None
    if "--ambient" in argv:
        AMBIENT[0] = int(argv[argv.index("--ambient") + 1])
    if "--save" in argv:
        pos, yaw, pitch, tt = save_pose(world)
        img = view(world, pos, yaw, pitch, tt if t is None else t)
    elif "--orbit" in argv:
        i = argv.index("--orbit")
        target = tuple(float(v) for v in argv[i + 1:i + 4])
        r = float(argv[argv.index("--radius") + 1]) if "--radius" in argv else 24
        img = orbit(world, target, r, 6000 if t is None else t)
    else:
        i = argv.index("--cam")
        x, y, z, yaw, pitch = (float(v) for v in argv[i + 1:i + 6])
        img = view(world, (x, y, z), yaw, pitch, 6000 if t is None else t)
    img.save(out)
    print(out)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
