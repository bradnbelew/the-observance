"""The lived-in pass (2026-11 rework): what the residents left around and inside their places.

Called by build2011.build() after the places stand and before the ground settles. Everything here
is a 2011 block (Beta 1.5; the one dated exception is Rook's Nether portal, 27 May 2011, which is
obsidian and portal blocks, both in Beta 1.5's table). Signs and chests with contents are placed
by inject.py (they are tile entities); this module lays the blocks they stand on and everything
that has no tile entity: furniture, the pixel art, the torch clock, the dirt towers, the paths.

Reading order for every ordered run comes from morrow/canon/layout.py (wall_run, *_LEFT).
"""

from __future__ import annotations

import math
from collections import deque

from morrow.canon import layout
from morrow.canon.build import CREEPER_FACE, CREEPER_MISSING_ROWS
from morrow.world.build2011 import (AIR, BOOKSHELF, CAKE, COBBLE, DIRT, DSLAB, FENCE, GLASS, GRASS, GRAVEL, LADDER,
                                    LOG, MOSSY, PLANKS, PUMPKIN, RAIL, RTORCH, SAND, SLAB, SLAB_STONE, SLAB_WOOD, STONE,
                                    TORCH, WATER, WOOL, WORKBENCH, WSTAIRS, CSTAIRS, FURNACE, JUKEBOX, STAIR_UP, TORCH_FACE,
                                    LADDER_FACE, Builder)

A = layout.ANCHORS
# more Beta 1.5 ids
IRON_BLOCK, WIRE, DIAMOND_ORE, OBSIDIAN, PORTAL, REPEATER, STONE_PLATE, WOOD_PLATE, LEVER, BUTTON, CLAY, NETHERRACK = 42, 55, 56, 49, 90, 93, 70, 72, 69, 77, 82, 87
REPEATER_DIR = {"north": 0, "east": 1, "south": 2, "west": 3}
GREEN, BLACK, WHITE = 13, 15, 0


def torch_post(b: Builder, x, z, who=None):
    """A fence post with a torch on top, on the ground: 2011 servers were lit this way."""
    g = b.ground(x, z)
    if b.get(x, g, z) in (WATER, 8):
        return
    b.set(x, g + 1, z, FENCE)
    b.torch(x, g + 2, z)


def along(points, every):
    """Positions every `every` blocks along straight legs through (x, z) points."""
    out, acc = [], 0.0
    for (ax, az), (bx, bz) in zip(points, points[1:]):
        n = max(abs(bx - ax), abs(bz - az), 1)
        for i in range(n):
            if acc >= every:
                out.append((round(ax + (bx - ax) * i / n), round(az + (bz - az) * i / n)))
                acc = 0
            acc += 1
    return out


# ---- spawn ------------------------------------------------------------------------------------

def spawn_extras(b: Builder):
    b.who = "rookery"
    sx, sy, sz = A["spawn"]
    rx, ry, rz = A["rules_signs"]
    # the rules board: a cobble plinth under the three signs and Rook's warning, torch posts either side
    for x in (rx, rx + 1, rx + 2, rx + 4):
        b.set(x, ry - 1, rz, COBBLE)
    for x in (rx - 1, rx + 5):
        b.set(x, ry - 1, rz, FENCE)
        b.torch(x, ry, rz)
    # the newbie chest and the bread chest stand on the plaza's west edge; their signs on cobble beside them
    kx, ky, kz = A["spawn_kit_chest"]
    bx, by, bz = A["spawn_bread_chest"]
    for z in (kz, bz):
        b.set(kx - 1, ky - 1, z, COBBLE)
    # the bulletin board: a plank backing south of the square, the six signs hang on it (inject.py)
    ox, oy, oz = A["spawn_board"]
    for i in range(3):
        for dy in (0, 1):
            b.set(ox + i, oy - dy, oz + 1, PLANKS)
        g = b.ground(ox + i, oz + 1)
        for y in range(g + 1, oy - 1):
            b.set(ox + i, y, oz + 1, PLANKS)
        g2 = b.ground(ox + i, oz)
        for y in range(g2 + 1, oy - 1):
            b.set(ox + i, y, oz, COBBLE)  # a step under the lower row
    b.torch(ox - 1, oy, oz + 1, "west")
    b.torch(ox + 3, oy, oz + 1, "east")
    # Rook's Nether portal frame (27 May 2011), on the square's east edge, lit
    px, py, pz = A["spawn_portal"]
    for dz in range(4):
        for dy in range(5):
            edge = dy in (0, 4) or dz in (0, 3)
            b.set(px, py + dy, pz + dz, OBSIDIAN if edge else PORTAL, 0 if edge else 2)
    for dz in range(4):
        g = b.ground(px, pz + dz)
        for y in range(g + 1, py):
            b.set(px, y, pz + dz, COBBLE)
    b.set(px - 1, py - 1, pz + 2, COBBLE)  # the portal sign's footing (inject.py)
    # the Christmas-tree spot: a log and torches, the sign on it (inject.py)
    tx, tz = sx - 7, sz + 2
    g = b.ground(tx, tz)
    b.set(tx, g + 1, tz, LOG)
    b.set(tx, g + 2, tz, LOG)
    b.torch(tx + 1, g + 2, tz, "east")
    b.torch(tx - 1, g + 2, tz, "west")
    # the pixel-art creeper, north of the square: a cobble footing, the top rows laid, the bottom rows missing
    cx, cy, cz = A["spawn_creeper"]
    rows = [[BLACK if ch == "B" else GREEN for ch in row] for row in reversed(CREEPER_FACE)]  # rows[0] = bottom
    for c in range(8):
        g = b.ground(cx + c, cz)
        for y in range(g + 1, cy):
            b.set(cx + c, y, cz, COBBLE)
        b.set(cx + c, cy - 1, cz, COBBLE)
        for r in range(CREEPER_MISSING_ROWS, 8):
            b.set(cx + c, cy + r, cz, WOOL, rows[r][c])
    b.set(cx - 1, cy - 1, cz, COBBLE)      # the wool chest's footing (inject.py)
    b.set(cx + 3, cy - 1, cz + 2, COBBLE)  # the WIP sign's footing
    # the low path: the worn track along the hill foot, exactly where Rook's map key says
    lx, _, lz = A["lowpath_place"]
    b.path([(sx + 5, sz + 5), (lx - 10, lz - 4), (lx, lz - 4), (lx + 12, lz - 2), (lx + 30, lz + 6)], width=1, wear=0.7, block=DIRT)
    # the x-ray "cave" under spawn: a dead-straight tunnel down to a pocket with the diamonds nobody mined
    b.who = "patchcord"
    xx, xz = sx - 7, sz + 10
    top = b.ground(xx, xz)
    for y in range(13, top + 1):
        b.set(xx, y, xz, AIR, log=False)
        if b.get(xx + 1, y, xz) == AIR:
            b.set(xx + 1, y, xz, STONE, log=False)
    b.ladder(xx, 13, top, xz, "west")
    for z in range(xz + 1, xz + 26):
        for y in (13, 14):
            b.set(xx, y, z, AIR, log=False)
        if (z - xz) % 6 == 3:
            b.torch(xx + 1, 14, z, "east") if b.get(xx + 2, 14, z) != AIR else None
    b.fill(xx - 2, 13, xz + 26, xx + 2, 15, xz + 29, AIR, log=False)
    for (dx, dz) in ((-3, 27), (3, 28), (0, 30)):
        b.set(xx + dx, 14, xz + dz, DIAMOND_ORE)
    b.torch(xx, 13, xz + 28)
    b.set(xx - 2, 13, xz + 29, COBBLE)  # the "it was a cave" sign's footing (inject.py)


# ---- Rook's house --------------------------------------------------------------------------------

def rookery_extras(b: Builder):
    b.who = "rookery"
    rx, ry, rz = A["rookery_house"]
    x0, x1, z0, z1 = rx - 4, rx + 4, rz, rz + 7
    # the old one-room furniture goes; two rooms: front (z0+1..z0+3), a plank partition, the bedroom
    for x, z in ((x0 + 1, z1 - 1), (x0 + 2, z1 - 1), (x0 + 3, z1 - 1)):
        b.set(x, ry, z, AIR, log=False)
    for z in (z0 + 2, z0 + 3):
        b.set(x1 - 1, ry, z, AIR, log=False)  # the old bed
    part = z0 + 4
    for x in range(x0 + 1, x1):
        for y in range(ry, ry + 4):
            if x == rx + 1 and y <= ry + 1:
                continue  # the opening
            b.set(x, y, part, PLANKS)
    # front room: workbench and two furnaces along the north wall, a chest row by the partition, a table
    b.set(x0 + 1, ry, z0 + 1, WORKBENCH)
    b.set(x0 + 2, ry, z0 + 1, FURNACE, 3)
    b.set(x0 + 3, ry, z0 + 1, FURNACE, 3)
    b.set(x0 + 3, ry, z0 + 2, FENCE)
    b.set(x0 + 3, ry + 1, z0 + 2, WOOD_PLATE)
    # the boot rack inside the door, the bookshelf wall on the east
    b.set(rx + 2, ry, z0 + 1, WSTAIRS, STAIR_UP["north"])
    b.set(rx + 3, ry, z0 + 1, WSTAIRS, STAIR_UP["north"])
    for z in range(z0 + 1, part):
        for y in (ry, ry + 1):
            b.set(x1 - 1, y, z, BOOKSHELF)
    # the bedroom: bed on the west, a chest, a torch
    b.bed(x0 + 1, ry, part + 1, "south")
    b.set(x1 - 1, ry, z1 - 1, 54)
    b.torch(x0 + 1, ry + 2, z1 - 1, "north")
    # the cellar hangout: shelves in the walls, a couch and a jukebox on the landing (the wool floor is untouched)
    fx, fy, fz = A["rookery_cellar_floor"]
    for x in range(fx + 2, fx + 16, 3):
        b.set(x, fy + 1, fz - 1, BOOKSHELF)
        b.set(x, fy + 2, fz - 1, BOOKSHELF)
    for z in range(fz + 1, fz + 8, 2):
        b.set(fx - 1, fy + 1, z, BOOKSHELF)
        b.set(fx + 18, fy + 1, z, 54 if z < fz + 4 else BOOKSHELF)   # chests in the east wall's recess
    land = fz + 9
    for x in (fx + 3, fx + 4, fx + 5):
        b.set(x, fy + 1, land, WSTAIRS, STAIR_UP["south"])  # the couch, back to the south wall
    b.set(fx + 2, fy + 1, land, JUKEBOX)
    b.set(fx + 16, fy + 1, land, WORKBENCH)
    b.torch(fx + 4, fy + 3, land, "north")


# ---- cairn's places -------------------------------------------------------------------------------

def cairn_extras(b: Builder):
    b.who = "cairn"
    cx, cy, cz = A["cairn_storehouse_door_2011"]
    x0, x1, z0, z1 = cx - 2, cx + 6, cz, cz + 14
    # inside the barn: the routine fixtures along the north wall (plate inside the door, the hinge tin,
    # the returns chest, the desk lamp on a plank), the bread chest by the door, flour sacks, the oven
    b.set(cx + 5, cy, z0 + 1, PLANKS)             # the lamp's plank (the plugin puts the lever on it)
    b.set(x1 - 1, cy, z1 - 1, AIR, log=False)     # the old workbench corner becomes the oven
    for x in (x0 + 1, x0 + 2, x0 + 3):
        b.set(x, cy, z1 - 1, FURNACE, 2)
    b.set(x0 + 1, cy, z1 - 3, WOOL, WHITE)
    b.set(x0 + 1, cy + 1, z1 - 3, WOOL, WHITE)
    b.set(x0 + 2, cy, z1 - 3, WOOL, WHITE)
    b.set(x1 - 1, cy, z0 + 4, WORKBENCH)
    b.set(x1 - 1, cy, z0 + 6, FENCE)              # cairn's desk and chair
    b.set(x1 - 1, cy + 1, z0 + 6, SLAB, SLAB_WOOD)
    b.set(x1 - 2, cy, z0 + 6, WSTAIRS, STAIR_UP["east"])
    for z in (z0 + 5, z0 + 9):
        b.set(cx + 2, cy + 2, z, PUMPKIN)         # hanging under the beam
    for z in range(z0 + 3, z1, 4):
        b.torch(x1 - 1, cy + 2, z, "west")
    # the shed: the hatch (a hole, the floor plank gone; Morrow's rebuild puts a trapdoor in it), a
    # shaft to a root cellar, and the bed-shaped gap left as dirt
    hx, hy, hz = A["cairn_shed_hatch"]
    for y in range(hy - 3, hy + 1):
        b.set(hx, y, hz, AIR, log=False)
    b.ladder(hx, hy - 3, hy - 1, hz, "north")
    b.fill(hx - 2, hy - 5, hz - 2, hx + 1, hy - 4, hz + 1, STONE, log=False)
    b.fill(hx - 1, hy - 4, hz - 1, hx, hy - 4, hz, AIR, log=False)
    b.set(hx - 1, hy - 4, hz + 1, STONE, log=False)
    b.torch(hx - 1, hy - 3, hz, "west")
    b.set(hx - 1, hy, hz - 1, DIRT)
    b.set(hx - 1, hy, hz, DIRT)
    # the fields: a pumpkin patch, a scarecrow, a second gate on the west side
    fx, fy, fz = A["cairn_farm"]
    for (dx, dz) in ((15, 2), (16, 3), (15, 5), (17, 6), (16, 8)):
        g = b.ground(fx + dx, fz + dz)
        if b.get(fx + dx, g, fz + dz) in (GRASS, DIRT):
            b.set(fx + dx, g + 1, fz + dz, PUMPKIN)
    b.set(fx + 6, fy, fz + 11, FENCE)
    b.set(fx + 6, fy + 1, fz + 11, FENCE)
    b.set(fx + 6, fy + 2, fz + 11, PUMPKIN)
    b.set(fx - 1, fy, fz + 4, AIR, log=False)  # the west gate
    # cairn's cobble hut by the fields ("rebuilt it in cobble. let it try"): where he sleeps since the bed went
    hx2, hz2 = fx - 8, fz + 2
    hy2 = b.ground(hx2, hz2) + 1
    b.site(hx2, hz2, hx2 + 4, hz2 + 4, hy2 - 1, footing=COBBLE, skirt=3)
    b.walls(hx2, hy2, hz2, hx2 + 4, hy2 + 2, hz2 + 4, lambda x, y, z: (COBBLE, 0))
    b.interior_air(hx2 + 1, hy2, hz2 + 1, hx2 + 3, hy2 + 2, hz2 + 3)
    b.fill(hx2, hy2 + 3, hz2, hx2 + 4, hy2 + 3, hz2 + 4, SLAB, SLAB_COBBLE)
    b.door(hx2 + 2, hy2, hz2 + 4, "south")
    b.set(hx2 + 2, hy2 + 1, hz2, GLASS)
    b.bed(hx2 + 1, hy2, hz2 + 1, "east")
    b.set(hx2 + 3, hy2, hz2 + 3, 54)
    b.torch(hx2 + 1, hy2 + 1, hz2 + 3, "north")
    b.set(hx2 + 1, hy2 - 1, hz2 + 5, COBBLE)  # the sign's footing (inject.py)
    # the sheep pen gate's sign footing
    sx2, sy2, sz2 = A["sheep_pen"]
    b.set(sx2 + 11, sy2, sz2 + 3, COBBLE)
    # Kit's abandoned mob grinder, east of the farms: a cobble tower with a water channel, half built
    b.who = "kitbag"
    gx, gz = cx + 28, cz - 10
    gy = b.ground(gx, gz) + 1
    b.site(gx - 2, gz - 2, gx + 2, gz + 2, gy - 1, footing=COBBLE, skirt=3)
    for y in range(gy, gy + 7):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                edge = abs(dx) == 2 or abs(dz) == 2
                if edge and not (y >= gy + 5 and dx + dz > 1):   # the top rows trail off: never finished
                    b.set(gx + dx, y, gz + dz, COBBLE)
                elif not edge:
                    b.set(gx + dx, y, gz + dz, AIR, log=False)
    b.set(gx, gy + 6, gz, WATER)
    b.set(gx + 2, gy, gz, AIR, log=False)  # the collection gap
    b.set(gx + 3, gy - 1, gz + 1, COBBLE)  # the sign's footing (inject.py)


SLAB_COBBLE = 3


# ---- finch's rail ---------------------------------------------------------------------------------

def finch_extras(b: Builder):
    b.who = "finchline"
    hx, hy, hz = A["finch_hub"]
    # the timetable board north of the hub, beside the line: plank backing, three signs face south (inject.py)
    for i in range(3):
        x = hx + 1 + i
        g = b.ground(x, hz - 5)
        for y in range(g + 1, hy + 2):
            b.set(x, y, hz - 5, PLANKS)
        b.set(x, hy + 1, hz - 5, PLANKS)
    b.torch(hx + 4, hy + 2, hz - 5, "east")
    # the bench at the south branch's end, and a torch, so people stop there
    ex, ey, ez = A["south_rail_end"]
    for dz in (2, 3):
        b.set(ex - 2, b.ground(ex - 2, ez + dz) + 1, ez + dz, WSTAIRS, STAIR_UP["west"])
    torch_post(b, ex - 2, ez + 4)
    # the farm stop: a memorial stone and a bench (the sign is inject.py's)
    farm = A["cairn_storehouse_door_2011"]
    stop_x, stop_z = farm[0] - 24, A["finch_corner"][2]
    g = b.ground(stop_x, stop_z + 2)
    b.set(stop_x, g + 1, stop_z + 2, COBBLE)
    b.set(stop_x, g + 2, stop_z + 2, COBBLE)
    for dx in (1, 2):
        b.set(stop_x + dx, b.ground(stop_x + dx, stop_z + 2) + 1, stop_z + 2, WSTAIRS, STAIR_UP["south"])
    # the three junctions: a siding stub at 2 and 3 (1 is where the spur leaves), a lever pad beside each
    sidings = {"finch_jct_2": (0, -1), "finch_jct_3": (0, 1)}
    for key, (ddx, ddz) in sidings.items():
        jx, jy, jz = A[key]
        for i in range(1, 7):
            x, z = jx + ddx * i, jz + ddz * i
            yy = jy
            for yy2 in range(jy, jy + 3):
                if b.get(x, yy2, z) != AIR:
                    b.set(x, yy2, z, AIR, log=False)
            if b.get(x, yy - 1, z) in (AIR, 18, 17) or b.get(x, yy - 1, z) in (8, 9):
                b.set(x, yy - 1, z, COBBLE)
            b.set(x, yy, z, RAIL, 0 if ddz else 1)
        b.set(jx + ddx * 7, jy, jz + ddz * 7, FENCE)   # the bumper
    from morrow.canon import logblock
    for key, (off, _on) in logblock.JUNCTION_LEVERS.items():
        jx, jy, jz = A[key]
        lx, ly, lz = jx + off[0], jy + off[1], jz + off[2]
        if b.get(lx, ly - 1, lz) in (AIR, 18, 8, 9):
            b.set(lx, ly - 1, lz, COBBLE)
        b.set(lx, ly, lz, LEVER, 5)               # on the ground, off: the log says how finch left them (C3)
    # torch posts along the main line every 16 blocks
    leg = A["patchcord_relay_hut"]
    pts = [(hx + 2, hz - 4), (hx + 2, leg[2] + 12), (A["finch_corner"][0] - 2, leg[2] + 12), (A["finch_corner"][0] - 2, A["finch_corner"][2] + 2)]
    for x, z in along(pts, 16):
        torch_post(b, x, z)


# ---- June's point ---------------------------------------------------------------------------------

def june_extras(b: Builder):
    b.who = "juni_lantern"
    hx, hy, hz = A["june_house"]
    x0, x1, z0, z1 = hx - 6, hx, hz - 2, hz + 3
    b.set(x1 - 1, hy, z0 + 1, BOOKSHELF)
    b.set(x1 - 1, hy + 1, z0 + 1, BOOKSHELF)
    b.set(x0 + 2, hy, z1 - 1, FENCE)              # the lamp cabinet: a torch behind glass
    b.torch(x0 + 2, hy + 1, z1 - 1)
    b.set(x0 + 2, hy, z1 - 2, GLASS)
    b.set(x0 + 2, hy + 1, z1 - 2, GLASS)
    b.set(x0 + 4, hy, z0 + 1, 54)                 # the flag locker (inject.py fills it)
    # the tower gallery: finch's torch clock on the floor round the lantern frame: six repeaters at 4
    # ticks and one torch make a 50-tick loop, 5 s (C4's 2011 route to "Fl W 5s"). The ring is the
    # 5x5 round the frame posts; the rim's fence is one block further out.
    tx, ty, tz = A["june_tower"]
    b.who = "finchline"
    gy = ty + 21
    bx, bz = tx - 2, tz - 2
    ring = [((bx, bz), STONE, 0), ((bx + 1, bz), RTORCH, 1),
            ((bx + 2, bz), REPEATER, REPEATER_DIR["east"] + 12), ((bx + 3, bz), WIRE, 0), ((bx + 4, bz), WIRE, 0),
            ((bx + 4, bz + 1), REPEATER, REPEATER_DIR["south"] + 12), ((bx + 4, bz + 2), WIRE, 0),
            ((bx + 4, bz + 3), REPEATER, REPEATER_DIR["south"] + 12), ((bx + 4, bz + 4), WIRE, 0),
            ((bx + 3, bz + 4), REPEATER, REPEATER_DIR["west"] + 12), ((bx + 2, bz + 4), WIRE, 0),
            ((bx + 1, bz + 4), REPEATER, REPEATER_DIR["west"] + 12), ((bx, bz + 4), WIRE, 0),
            ((bx, bz + 3), REPEATER, REPEATER_DIR["north"] + 12), ((bx, bz + 2), WIRE, 0), ((bx, bz + 1), WIRE, 0)]
    for (x, z), block, meta in ring:
        b.set(x, gy, z, block, meta)
    b.who = "juni_lantern"
    b.set(tx - 1, ty - 1, tz - 3, 24)                           # the lamp sign's footing by the door (inject.py)


# ---- patchcord's studio ---------------------------------------------------------------------------

def patch_studio(b: Builder):
    b.who = "patchcord"
    px, py, pz = A["patchcord_relay_hut"]
    x0, x1, z0, z1 = px - 2, px + len(ciphers_tune()) , pz - 1, pz + 4
    # the mixer desk along the north wall, east of the door: double slabs with levers and torches on top
    for i, x in enumerate(range(px + 2, px + 8)):
        b.set(x, py, z0 + 1, DSLAB, SLAB_STONE)
        if i % 2 == 0:
            b.set(x, py + 1, z0 + 1, LEVER, 5 | (8 if i % 4 == 0 else 0))
        else:
            b.set(x, py + 1, z0 + 1, RTORCH, 5)
    b.set(px + 9, py, z0 + 1, FENCE)                 # the headphones on a hook: black wool on a post
    b.set(px + 9, py + 1, z0 + 1, WOOL, BLACK)
    b.set(px + 3, py, pz + 1, FENCE)                 # the mic stand, in front of the desk
    b.set(px + 3, py + 1, pz + 1, STONE_PLATE)


def ciphers_tune():
    from morrow.canon import ciphers
    return ciphers.TUNE


# ---- Iona's annex as an ops room ------------------------------------------------------------------

def annex_ops(b: Builder):
    b.who = "i_bell"
    ax, ay, az = A["maintenance_annex"]
    # a server rack on the west wall: iron blocks with redstone torches as LEDs
    for z in (az - 4, az - 3):
        for dy in range(3):
            b.set(ax, ay + dy, z, IRON_BLOCK)
        b.set(ax + 1, ay + 1, z, RTORCH, 1)
    # desk and chest on the north wall, a bed, the coffee corner (a furnace and a cake), a bookshelf
    b.set(ax + 4, ay, az - 5, WORKBENCH)
    b.set(ax + 5, ay, az - 5, 54)
    b.set(ax + 9, ay, az - 5, BOOKSHELF)
    b.set(ax + 9, ay + 1, az - 5, BOOKSHELF)
    b.bed(ax + 7, ay, az - 4, "east")
    b.set(ax + 1, ay, az, FURNACE, 5)               # the stove, beside the netherrack of the ID row
    b.set(ax + 1, ay, az - 1, STONE)
    b.set(ax + 1, ay + 1, az - 1, CAKE)
    # the manhole: the stair tunnel's cap becomes cobble with Iona's sign beside it (inject.py)
    if getattr(b, "manhole", None):
        mx, my, mz = b.manhole
        b.set(mx, my, mz, COBBLE)
        b.set(mx + 1, my, mz, COBBLE)
    # the ladder room: a bed and torches framing Rook's key signs
    b.who = "rookery"
    lx, ly, lz = A["rookery_ladder_room"]
    b.bed(lx + 5, ly, lz - 2, "east")
    b.torch(lx + 5, ly + 1, lz, "north")
    b.torch(lx - 1, ly + 1, lz, "north")


# ---- Kit's trail ---------------------------------------------------------------------------------

def kit_trail(b: Builder, world_path: str):
    """Dirt towers every ~80 blocks along the generated chunks from spawn to his base (the owner's
    walk), and a wolf kennel by his hut. The towers are what Rook complained about."""
    b.who = "kitbag"
    chunks = set(b.w.generated_chunks())
    sx, _, sz = A["spawn"]
    kx, ky, kz = A["kit_fossil_base"]
    start, goal = (sx >> 4, sz >> 4), (kx >> 4, kz >> 4)
    prev = {start: None}
    q = deque([start])
    while q:
        c = q.popleft()
        if c == goal:
            break
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                n = (c[0] + dx, c[1] + dz)
                if n in chunks and n not in prev:
                    prev[n] = c
                    q.append(n)
    if goal not in prev:
        return 0
    path = []
    c = goal
    while c is not None:
        path.append(c)
        c = prev[c]
    path.reverse()
    towers = 0
    for i in range(6, len(path) - 3, 5):
        cx, cz = path[i]
        x, z = cx * 16 + 8, cz * 16 + 8
        g = b.ground(x, z)
        if b.get(x, g, z) in (8, 9):
            continue
        h = 5 + (i * 7) % 4
        for y in range(g + 1, g + 1 + h):
            b.set(x, y, z, DIRT)
        b.torch(x, g + 1 + h, z)
        towers += 1
    # the kennel: a fence ring east of the hut, the dogs' name signs inside (inject.py), a chest of bones
    try:
        g = b.ground(kx + 6, kz + 2)
        for dx in range(4, 10):
            for dz in range(-2, 4):
                edge = dx in (4, 9) or dz in (-2, 3)
                if edge and not (dx == 4 and dz == 0):
                    b.set(kx + dx, b.ground(kx + dx, kz + dz) + 1, kz + dz, FENCE)
        b.set(kx + 8, b.ground(kx + 8, kz + 2) + 1, kz + 2, 54)
        for dx in (5, 6, 7):
            b.set(kx + dx, b.ground(kx + dx, kz) , kz, COBBLE)
    except KeyError:
        pass
    return towers


# ---- between the places ----------------------------------------------------------------------------

def between(b: Builder):
    """Torch posts along the gravel paths (not to lowpath's place: nobody ever walked there)."""
    b.who = "rookery"
    sx, _, sz = A["spawn"]
    rx, _, rz = A["rookery_house"]
    hx, _, hz = A["finch_hub"]
    for x, z in along([(sx - 1, sz - 6), (sx - 1, rz - 4), (rx + 2, rz - 4)], 9):
        torch_post(b, x, z)
    b.who = "finchline"
    for x, z in along([(sx + 5, sz - 2), (hx - 5, sz - 2), (hx - 5, hz)], 9):
        torch_post(b, x, z)
    b.who = "cairn"
    cx, _, cz = A["cairn_storehouse_door_2011"]
    fx, _, fz = A["cairn_farm"]
    px, _, pz = A["sheep_pen"]
    for x, z in along([(cx - 1, cz - 1), (cx - 1, cz - 5), (fx + 5, cz - 5), (fx + 5, fz + 10)], 9):
        torch_post(b, x, z)
    for x, z in along([(fx + 5, cz - 6), (px + 10, cz - 6), (px + 10, pz + 4)], 9):
        torch_post(b, x, z)


def apply(b: Builder, world_path: str) -> dict:
    spawn_extras(b)
    rookery_extras(b)
    cairn_extras(b)
    finch_extras(b)
    june_extras(b)
    patch_studio(b)
    annex_ops(b)
    towers = kit_trail(b, world_path)
    between(b)
    return {"kit_towers": towers}
