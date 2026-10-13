"""Build 2011 Mossfield into the real Beta 1.5 world: the residents' places, the rail, the dug rooms.

    python -m morrow.world.build2011 <world-dir> [--stage final|march|april|may15|half|nearly|roofed]

Run it on a COPY of the owner's world (world/beta/Mossfield-pristine is the untouched original).
Every block is a 2011 block (Beta 1.5, see BETA-WORLD-GUIDE.md). The builds are deliberately
uneven, each in its resident's own habits, because "tidy" is what Morrow does:

  rookery   lumpy cobble box, mossy patches, a dirt pillar he never took down, cellar + floor room
  cairn     long practical plank shed, log corners, six chest rows, fields, split sheep pen
  finchline the only neat thing in town: stone hub, slab roof, powered rail with torch boosters
  june      careful little sandstone-and-plank house, wooden dock, lighthouse with an open top
  patchcord cramped cobble hut by the line, mismatched glass, jukebox, the note-block wall
  i_bell    the annex room (WorldEdit-square) and her notes cell, both underground
  kitbag    nothing here: Kit's base is the owner's walk (fossil chunks need the real game)

The cipher objects themselves (floor wool, tally chests, signs, Iona's chest, tune pitches, ID
wall, sheep) are NOT placed here: inject.py places them on top, at the same layout anchors.
Stages exist for June's screenshots (tower height / roof progress at each dated shot).

It also writes build-manifest.json next to the world: every block placed, attributed to the
resident who "built" it, so the LogBlock history can be generated from the real world.
"""

from __future__ import annotations

import json
import random
import sys
from pathlib import Path

from morrow.canon import layout
from morrow.world.mcregion import World
from morrow.world.relight import relight_blocks

A = layout.ANCHORS

AIR, STONE, GRASS, DIRT, COBBLE, PLANKS = 0, 1, 2, 3, 4, 5
WATER, SAND, GRAVEL, LOG, LEAVES, GLASS = 9, 12, 13, 17, 18, 20
SANDSTONE, NOTE, BED, POWERED, WOOL = 24, 25, 26, 27, 35
DSLAB, SLAB, BRICK, BOOKSHELF, MOSSY, TORCH = 43, 44, 45, 47, 48, 50
WSTAIRS, CHEST, WORKBENCH, CROPS, FARMLAND, FURNACE = 53, 54, 58, 59, 60, 61
WDOOR, LADDER, RAIL, CSTAIRS, LEVER, IDOOR, RTORCH = 64, 65, 66, 67, 69, 71, 76
REEDS, JUKEBOX, FENCE, PUMPKIN, GLOWSTONE, CAKE = 83, 84, 85, 86, 89, 92
NATURAL_CLEAR = {LOG, LEAVES, 31, 32, 37, 38, 39, 40, 78, 81, 83, 6}  # trees, grass, flowers, snow, cactus, reeds, saplings
GROUND_SKIP = NATURAL_CLEAR | {AIR}

# door meta: which edge of the block the closed door sits on
DOOR_EDGE = {"west": 0, "north": 1, "east": 2, "south": 3}
# ladder / wall-torch meta by the direction the ladder faces (away from its wall)
LADDER_FACE = {"north": 2, "south": 3, "west": 4, "east": 5}
TORCH_FACE = {"east": 1, "west": 2, "south": 3, "north": 4, "floor": 5}


class Builder:
    def __init__(self, world: World):
        self.w = world
        self.manifest: list[dict] = []
        self.who = "rookery"
        self.box = [10**9, 10**9, -10**9, -10**9]
        self.rng = random.Random(137407650)

    # ---- primitives ------------------------------------------------------------------------
    def set(self, x, y, z, block, meta=0, log=True):
        self.w.set(x, y, z, block, meta)
        b = self.box
        b[0], b[1], b[2], b[3] = min(b[0], x), min(b[1], z), max(b[2], x), max(b[3], z)
        if log and block != AIR:
            self.manifest.append({"who": self.who, "x": x, "y": y, "z": z, "id": block, "meta": meta})

    def get(self, x, y, z):
        return self.w.get(x, y, z)[0]

    def fill(self, x0, y0, z0, x1, y1, z1, block, meta=0, log=True):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, block, meta, log)

    def ground(self, x, z) -> int:
        """Top non-tree block y at a column."""
        y = min(self.w.height(x, z), 127)
        while y > 1:
            y -= 1
            if self.get(x, y, z) not in GROUND_SKIP:
                return y
        return 63

    def clear_nature(self, x0, z0, x1, z1, y_from):
        """Remove trees/plants (and any terrain) above y_from in a footprint, without logging it."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                for y in range(y_from, 128):
                    if self.get(x, y, z) != AIR:
                        self.w.set(x, y, z, AIR)

    def clear_trees_near(self, x0, z0, x1, z1, margin=3):
        """Take out whole trees whose logs or leaves overhang a plot (leaves left floating look fake)."""
        for x in range(min(x0, x1) - margin, max(x0, x1) + margin + 1):
            for z in range(min(z0, z1) - margin, max(z0, z1) + margin + 1):
                for y in range(60, 128):
                    if self.get(x, y, z) in (LOG, LEAVES):
                        self.w.set(x, y, z, AIR)
                        if self.get(x, y - 1, z) == DIRT:
                            self.w.set(x, y - 1, z, GRASS)  # no bare dirt where the trunk stood

    def pad(self, x0, z0, x1, z1, y, top=GRASS, under=DIRT):
        """Level a plot to y: cut everything above, fill below with dirt, top with grass."""
        self.clear_trees_near(x0, z0, x1, z1)
        self.clear_nature(x0, z0, x1, z1, y + 1)
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                g = self.ground(x, z)
                for yy in range(min(g, y), y):
                    if self.get(x, yy, z) in GROUND_SKIP | {WATER, 8}:
                        self.w.set(x, yy, z, under)
                self.w.set(x, y, z, top)  # cut slopes would otherwise show bare dirt

    def walls(self, x0, y0, z0, x1, y1, z1, pick):
        """Hollow box walls; pick(x, y, z) -> (block, meta)."""
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for z in range(min(z0, z1), max(z0, z1) + 1):
                if x in (x0, x1) or z in (z0, z1):
                    for y in range(y0, y1 + 1):
                        self.set(x, y, z, *pick(x, y, z))

    def interior_air(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, AIR, log=False)

    def door(self, x, y, z, edge):
        self.set(x, y, z, WDOOR, DOOR_EDGE[edge])
        self.set(x, y + 1, z, WDOOR, DOOR_EDGE[edge] | 8)

    def torch(self, x, y, z, face="floor"):
        self.set(x, y, z, TORCH, TORCH_FACE[face])

    def ladder(self, x, y0, y1, z, face):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, LADDER, LADDER_FACE[face])

    def bed(self, x, y, z, toward):
        dx, dz, meta = {"south": (0, 1, 0), "west": (-1, 0, 1), "north": (0, -1, 2), "east": (1, 0, 3)}[toward]
        self.set(x, y, z, BED, meta)
        self.set(x + dx, y, z + dz, BED, meta | 8)

    def chance(self, p) -> bool:
        return self.rng.random() < p

    # ---- rail ------------------------------------------------------------------------------
    def rail_line(self, points, booster_every=24, powered=True):
        """Lay rail along axis-aligned legs through points (x, z), following the ground smoothly.

        Each step changes height by at most one block; cuts and fills are made as needed.
        Powered rails every booster_every blocks with a redstone torch beside them (finch's way).
        """
        path = []
        for (ax, az), (bx, bz) in zip(points, points[1:]):
            sx = (bx > ax) - (bx < ax)
            sz = (bz > az) - (bz < az)
            x, z = ax, az
            while (x, z) != (bx, bz):
                path.append((x, z))
                x, z = x + sx, z + sz
        path.append(points[-1])
        target = [self.ground(x, z) + 1 for x, z in path]
        # smooth: rails can climb one block per step; average out noise first
        ys = []
        for i in range(len(path)):
            win = target[max(0, i - 3): i + 4]
            ys.append(round(sum(win) / len(win)))
        for i in range(1, len(ys)):
            ys[i] = max(ys[i - 1] - 1, min(ys[i - 1] + 1, ys[i]))
        for i in range(len(ys) - 2, -1, -1):
            ys[i] = max(ys[i + 1] - 1, min(ys[i + 1] + 1, ys[i]))
        for i, ((x, z), y) in enumerate(zip(path, ys)):
            # bed: cut above, support below
            for yy in range(y, y + 3):
                if self.get(x, yy, z) != AIR:
                    self.w.set(x, yy, z, AIR)
            if self.get(x, y - 1, z) in GROUND_SKIP | {WATER, 8, SAND}:
                self.set(x, y - 1, z, COBBLE)
                g = y - 2
                while g > 50 and self.get(x, g, z) in GROUND_SKIP | {WATER, 8}:
                    self.set(x, g, z, COBBLE)  # pillar
                    g -= 1
            px, pz = path[i - 1] if i else path[i]
            nx, nz = path[i + 1] if i + 1 < len(path) else path[i]
            meta = self._rail_meta((px, pz), (x, z), (nx, nz), ys[i - 1] if i else y, y, ys[i + 1] if i + 1 < len(ys) else y)
            is_boost = powered and booster_every and i % booster_every == booster_every // 2 and meta < 6
            if is_boost:
                self.set(x, y, z, POWERED, meta | 8)
                tx, tz = (x + 1, z) if (nx == x) else (x, z + 1)  # torch beside the rail
                if self.get(tx, y, tz) in GROUND_SKIP:
                    self.set(tx, y, tz, RTORCH, TORCH_FACE["floor"])
            else:
                self.set(x, y, z, RAIL, meta)
        return path, ys

    @staticmethod
    def _rail_meta(prev, cur, nxt, yprev, y, ynext):
        (px, pz), (x, z), (nx, nz) = prev, cur, nxt
        if ynext > y and nx != x:
            return 2 if nx > x else 3
        if ynext > y and nz != z:
            return 5 if nz > z else 4
        if yprev > y and px != x:
            return 2 if px > x else 3
        if yprev > y and pz != z:
            return 5 if pz > z else 4
        dirs = {(px - x, pz - z), (nx - x, nz - z)} - {(0, 0)}
        if dirs <= {(1, 0), (-1, 0)}:
            return 1
        if dirs <= {(0, 1), (0, -1)}:
            return 0
        if dirs == {(0, 1), (1, 0)}:
            return 6
        if dirs == {(0, 1), (-1, 0)}:
            return 7
        if dirs == {(0, -1), (-1, 0)}:
            return 8
        if dirs == {(0, -1), (1, 0)}:
            return 9
        return 0


# ---- the residents' places -----------------------------------------------------------------

def spawn_plaza(b: Builder):
    b.who = "rookery"
    sx, sy, sz = A["spawn"]
    b.pad(sx - 5, sz - 6, sx + 5, sz + 5, sy)
    for x in range(sx - 4, sx + 5):
        for z in range(sz - 4, sz + 5):
            edge = abs(x - sx) == 4 or abs(z - sz) == 4
            b.set(x, sy, z, COBBLE if edge or b.chance(0.55) else GRAVEL)
    for x, z in ((sx - 4, sz - 4), (sx + 4, sz - 4), (sx - 4, sz + 4), (sx + 4, sz + 4)):
        b.set(x, sy + 1, z, FENCE)
        b.torch(x, sy + 2, z)
    # the cake table near spawn, where Kit stood (fence legs, wood slab top)
    tx, tz = sx + 3, sz + 2
    for dx in (0, 1):
        b.set(tx + dx, sy + 1, tz, FENCE)
        b.set(tx + dx, sy + 2, tz, SLAB, 2)
    b.set(tx, sy + 3, tz, CAKE)


def rookery_house(b: Builder):
    b.who = "rookery"
    rx, ry, rz = A["rookery_house"]           # a point on the north wall, left of the door
    x0, x1, z0, z1 = rx - 4, rx + 4, rz, rz + 7
    b.pad(x0 - 2, z0 - 3, x1 + 2, z1 + 2, ry - 1)
    b.fill(x0, ry - 1, z0, x1, ry - 1, z1, COBBLE)                      # floor
    def stone(x, y, z):
        return (MOSSY, 0) if b.chance(0.12) else (COBBLE, 0)
    b.walls(x0, ry, z0, x1, ry + 3, z1, stone)
    b.interior_air(x0 + 1, ry, z0 + 1, x1 - 1, ry + 4, z1 - 1)
    # flat plank roof with one missing corner block he never fixed
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x, z) != (x1, z1):
                b.set(x, ry + 4, z, PLANKS)
    # windows: odd sizes
    for x, y, z in ((x0 + 2, ry + 1, z0), (x0 + 6, ry + 1, z0), (x0 + 6, ry + 2, z0), (x0, ry + 1, z0 + 3), (x1, ry + 2, z0 + 5)):
        b.set(x, y, z, GLASS)
    b.door(rx + 1, ry, z0, "north")
    b.set(rx + 1, ry + 2, z0, COBBLE)
    for x, z, f in ((x0 + 1, z0 + 1, "south"), (x1 - 1, z1 - 1, "north"), (x0 + 1, z1 - 1, "north")):
        b.torch(x, ry + 2, z, f)
    b.set(x0 + 1, ry, z1 - 1, WORKBENCH)
    b.set(x0 + 2, ry, z1 - 1, FURNACE, 2)
    b.set(x0 + 3, ry, z1 - 1, CHEST)
    b.bed(x1 - 1, ry, z0 + 2, "south")
    # the dirt pillar he climbed to do the roof and never took down
    for y in range(ry, ry + 4):
        b.set(x1 + 1, y, z1 + 1, DIRT)
    # cellar: the 18x9 floor room (wool laid by inject.py) plus a one-row landing on its south side,
    # under the house's south wall, where the ladder comes down (so nothing stands on the floor).
    fx, fy, fz = A["rookery_cellar_floor"]
    lx, lz = rx + 2, z1 - 1
    assert fx <= lx <= fx + 17 and lz == fz + 9 and z1 == fz + 10, "cellar must sit under the house"
    b.fill(fx - 1, fy - 1, fz - 1, fx + 18, fy + 4, fz + 10, COBBLE, log=False)  # dug-out shell
    b.interior_air(fx, fy + 1, fz, fx + 17, fy + 3, fz + 9)
    b.fill(fx, fy, fz, fx + 17, fy, fz + 8, STONE)       # placeholder, inject.py lays the wool
    b.fill(fx, fy, fz + 9, fx + 17, fy, fz + 9, COBBLE)   # the landing
    for y in range(fy + 1, ry):
        b.set(lx, y, lz, AIR, log=False)
        b.set(lx, y, lz + 1, COBBLE)                       # the wall the ladder hangs on
    b.ladder(lx, fy + 1, ry - 1, lz, "north")
    for x in (fx + 1, fx + 8, fx + 16):
        b.torch(x, fy + 2, fz, "south")                   # on the north wall, off the floor
        b.torch(x, fy + 2, fz + 9, "north")


def ladder_room(b: Builder):
    """Rookery's stash, found from the Nether hub sign (x8). Signs go on the south wall (inject.py)."""
    b.who = "rookery"
    lx, ly, lz = A["rookery_ladder_room"]
    b.fill(lx - 2, ly - 1, lz - 5, lx + 9, ly + 4, lz + 1, STONE)
    b.interior_air(lx - 1, ly, lz - 4, lx + 8, ly + 2, lz)
    b.fill(lx - 1, ly - 1, lz - 4, lx + 8, ly - 1, lz, PLANKS)
    b.torch(lx, ly, lz - 4)
    b.torch(lx + 7, ly, lz - 4)
    b.set(lx + 3, ly, lz - 4, CHEST)
    b.set(lx + 4, ly, lz - 4, CHEST)
    # ladder shaft up to just under the surface (capped with dirt: you dig the last block)
    sx, sz = lx - 1, lz - 4
    top = b.ground(sx, sz)
    for y in range(ly, top):
        b.set(sx, y, sz, AIR, log=False)
        b.set(sx - 1, y, sz, STONE, log=False)
    b.ladder(sx, ly, top - 1, sz, "east")
    b.set(sx, top, sz, DIRT)


def finch_rail(b: Builder):
    b.who = "finchline"
    hx, hy, hz = A["finch_hub"]
    # the hub: neat stone platform, double-slab pillars, wood slab roof, a door on the shed
    b.pad(hx - 4, hz - 4, hx + 4, hz + 4, hy - 1)
    b.fill(hx - 3, hy - 1, hz - 3, hx + 3, hy - 1, hz + 3, STONE)
    for x, z in ((hx - 3, hz - 3), (hx + 3, hz - 3), (hx - 3, hz + 3), (hx + 3, hz + 3)):
        b.fill(x, hy, z, x, hy + 2, z, DSLAB)
    b.fill(hx - 3, hy + 3, hz - 3, hx + 3, hy + 3, hz + 3, SLAB, 0)
    # a little ticket shed on the west side with a real door (door register)
    b.walls(hx - 3, hy, hz - 1, hx - 2, hy + 2, hz + 1, lambda x, y, z: (PLANKS, 0))
    b.set(hx - 2, hy, hz, AIR, log=False)
    b.door(hx - 2, hy, hz, "east")
    b.torch(hx + 2, hy, hz - 2)
    b.torch(hx + 2, hy, hz + 2)
    b.set(hx - 3, hy + 1, hz, CHEST)
    for z in range(hz - 3, hz + 4):          # the line runs straight through the hub
        b.set(hx, hy, z, RAIL, 0)
    b.set(hx, hy, hz, POWERED, 8)
    b.set(hx + 1, hy, hz, RTORCH, TORCH_FACE["floor"])
    # main line north, east, north, east to the farms
    farm = A["cairn_storehouse_door_2011"]
    leg = A["patchcord_relay_hut"]
    pts = [(hx, hz - 4), (hx, leg[2] + 10), (A["finch_corner"][0], leg[2] + 10),
           (A["finch_corner"][0], A["finch_corner"][2]), (farm[0] - 24, A["finch_corner"][2])]
    path, ys = b.rail_line(pts)
    ex, ez = path[-1]                         # farm stop: a fence bumper and a torch
    b.set(ex - 1, ys[-1], ez, FENCE)
    b.torch(ex - 1, ys[-1] + 1, ez)
    # south branch: stops dead in the grass (finch: "just cobble marking for now")
    se = A["south_rail_end"]
    b.rail_line([(hx, hz + 4), (se[0], se[2])], booster_every=0)
    for dz in range(1, 6):
        b.set(se[0], b.ground(se[0], se[2] + dz) , se[2] + dz, COBBLE)  # the marking
    # the spur: heads off north from the corner and ends unfinished
    sp = A["finch_spur"]
    corner = (hx, leg[2] + 10)
    spur_end_z = sp[2] + 30
    b.rail_line([(corner[0], corner[1] - 1), (corner[0], spur_end_z)], booster_every=0)
    # tunnel from the spur's end into the hill to her hidden station (room carved by FinchLock on Paper)
    tx, ty = corner[0], b.ground(corner[0], spur_end_z) + 1
    for z in range(spur_end_z - 1, sp[2] + 12, -1):
        for y in (ty, ty + 1):
            b.set(tx, y, z, AIR, log=False)
        b.set(tx, ty - 1, z, COBBLE)
        if b.get(tx, ty + 2, z) == AIR:
            b.set(tx, ty + 2, z, COBBLE)
        if (spur_end_z - z) % 8 == 4:
            b.torch(tx, ty, z)
        ty = max(ty - 1, sp[1]) if ty > sp[1] else ty
    # station shell: stone room the lock goes into
    b.fill(sp[0] - 3, sp[1] - 1, sp[2] - 3, sp[0] + 17, sp[1] + 4, sp[2] + 13, STONE, log=False)
    b.interior_air(sp[0] - 2, sp[1], sp[2] - 2, sp[0] + 16, sp[1] + 3, sp[2] + 12)
    for y in (sp[1], sp[1] + 1):
        b.set(tx, y, sp[2] + 13, AIR, log=False)
    for x in range(sp[0] - 2, sp[0] + 17, 6):
        b.torch(x, sp[1], sp[2] + 12)


def patchcord_hut(b: Builder):
    b.who = "patchcord"
    px, py, pz = A["patchcord_relay_hut"]  # note wall origin: notes at x..x+7, z+2, y+1
    x0, x1, z0, z1 = px - 2, px + 9, pz - 1, pz + 3
    b.pad(x0 - 1, z0 - 2, x1 + 1, z1 + 1, py - 1)
    b.fill(x0, py - 1, z0, x1, py - 1, z1, PLANKS)
    def wall(x, y, z):
        if y == py + 1 and z == z0 and x in (x0 + 3, x0 + 4, x0 + 8):
            return GLASS, 0
        return (COBBLE, 0) if b.chance(0.85) else (MOSSY, 0)
    b.walls(x0, py, z0 - 0, x1, py + 3, z1 + 1, wall)
    b.interior_air(x0 + 1, py, z0 + 1, x1 - 1, py + 3, z1)
    b.fill(x0, py + 4, z0, x1, py + 4, z1 + 1, SLAB, 3)
    b.door(px, py, z0, "north")
    b.set(x0 + 1, py, z0 + 1, JUKEBOX)
    b.set(x1 - 1, py, z0 + 1, CHEST)
    b.torch(x0 + 1, py + 2, z0 + 1, "south")
    b.torch(x1 - 1, py + 2, z0 + 1, "south")


def cairn_farm(b: Builder):
    b.who = "cairn"
    cx, cy, cz = A["cairn_storehouse_door_2011"]  # door in the north wall; tally chests at x+2, z+2..z+12
    x0, x1, z0, z1 = cx - 2, cx + 6, cz, cz + 14  # interior x-1..x+5: room for the plugin's lecterns and golems
    b.pad(x0 - 2, z0 - 3, x1 + 2, z1 + 2, cy - 1)
    b.fill(x0, cy - 1, z0, x1, cy - 1, z1, PLANKS)
    def wall(x, y, z):
        corner = x in (x0, x1) and z in (z0, z1)
        if corner:
            return LOG, 0
        if y == cy + 1 and x in (x0, x1) and (z - z0) % 4 == 2:
            return GLASS, 0
        return PLANKS, 0
    b.walls(x0, cy, z0, x1, cy + 3, z1, wall)
    b.interior_air(x0 + 1, cy, z0 + 1, x1 - 1, cy + 3, z1 - 1)
    # gable-ish roof: wood stairs both sides, slab ridge
    for z in range(z0 - 1, z1 + 2):
        b.set(x0 - 1, cy + 4, z, WSTAIRS, 0)
        b.set(x1 + 1, cy + 4, z, WSTAIRS, 1)
        for x in range(x0, x1 + 1):
            b.set(x, cy + 4, z, PLANKS)
        b.set((x0 + x1) // 2, cy + 5, z, SLAB, 2)
    b.door(cx, cy, z0, "north")
    for z in range(z0 + 2, z1, 4):
        b.torch(x0 + 1, cy + 2, z, "east")
    b.set(x1 - 1, cy, z1 - 1, WORKBENCH)
    # the small shed nearby, no bed in it (the bed is gone from the shed)
    sx, sz = x1 + 5, z0 + 2
    b.pad(sx - 1, sz - 1, sx + 4, sz + 4, cy - 1)
    b.walls(sx, cy, sz, sx + 3, cy + 2, sz + 3, lambda x, y, z: (PLANKS, 0))
    b.interior_air(sx + 1, cy, sz + 1, sx + 2, cy + 2, sz + 2)
    b.fill(sx, cy + 3, sz, sx + 3, cy + 3, sz + 3, SLAB, 2)
    b.door(sx + 1, cy, sz, "north")
    b.torch(sx + 2, cy + 1, sz + 2, "north")
    # fields: farmland rows with water channels, crops at mixed growth
    fx, fy, fz = A["cairn_farm"]
    b.pad(fx - 1, fz - 1, fx + 13, fz + 9, fy - 1)
    for x in range(fx, fx + 13):
        for z in range(fz, fz + 9):
            if (x - fx) % 4 == 3:
                b.set(x, fy - 1, z, WATER)
            else:
                b.set(x, fy - 1, z, FARMLAND, 7)
                b.set(x, fy, z, CROPS, b.rng.choice((3, 5, 7, 7)))
    for x in range(fx - 1, fx + 14):
        b.set(x, fy, fz - 1, FENCE)
        b.set(x, fy, fz + 9, FENCE)
    for z in range(fz - 1, fz + 10):
        b.set(fx - 1, fy, z, FENCE)
        b.set(fx + 13, fy, z, FENCE)
    b.set(fx + 6, fy, fz - 1, AIR, log=False)  # gap he used as a gate
    # sheep pen split into inner and outer (inject.py puts the six census sheep in it)
    sx2, sy2, sz2 = A["sheep_pen"]
    b.pad(sx2 - 1, sz2 - 1, sx2 + 11, sz2 + 10, sy2)
    for x in range(sx2, sx2 + 11):
        b.set(x, sy2 + 1, sz2, FENCE)
        b.set(x, sy2 + 1, sz2 + 9, FENCE)
    for z in range(sz2, sz2 + 10):
        b.set(sx2, sy2 + 1, z, FENCE)
        b.set(sx2 + 10, sy2 + 1, z, FENCE)
        if z not in (sz2 + 4, sz2 + 5):
            b.set(sx2 + 5, sy2 + 1, z, FENCE)  # the inner/outer divider, with a gap


def june_point(b: Builder, stage: str = "final"):
    b.who = "juni_lantern"
    hx, hy, hz = A["june_house"]            # door spot on the house's east wall
    x0, x1, z0, z1 = hx - 6, hx, hz - 2, hz + 3
    b.pad(x0 - 1, z0 - 2, x1 + 1, z1 + 2, hy - 1, top=SAND, under=SAND)
    b.fill(x0, hy - 1, z0, x1, hy - 1, z1, PLANKS)
    def wall(x, y, z):
        if x in (x0, x1) and z in (z0, z1):
            return LOG, 0
        if y == hy + 1 and (z in (z0, z1) and (x - x0) % 3 == 2):
            return GLASS, 0
        return (SANDSTONE, 0) if y <= hy + 1 else (PLANKS, 0)
    b.walls(x0, hy, z0, x1, hy + 3, z1, wall)
    b.interior_air(x0 + 1, hy, z0 + 1, x1 - 1, hy + 3, z1 - 1)
    for i, z in enumerate(range(z0 - 1, z1 + 2)):
        for x in range(x0 - 1, x1 + 2):
            b.set(x, hy + 4, z, SLAB, 2 if (x + z) % 5 else 1)
    b.door(x1, hy, hz, "east")
    b.torch(x0 + 1, hy + 2, z0 + 1, "south")
    b.torch(x1 - 1, hy + 2, z1 - 1, "north")
    b.bed(x0 + 1, hy, z1 - 1, "east")
    b.set(x0 + 3, hy, z1 - 1, CHEST)
    b.set(x0 + 1, hy, z0 + 1, WORKBENCH)
    # the wooden dock, east of the house, out over the water
    dx, dy, dz = A["june_old_dock"]           # the landward end, at the shoreline
    for x in range(dx, dx + 7):
        for z in (dz, dz + 1):
            b.set(x, dy, z, PLANKS)
            if x >= dx and (x - dx) % 3 == 0:
                g = dy - 1
                while g > 40 and b.get(x, g, z) in GROUND_SKIP | {WATER, 8}:
                    b.set(x, g, z, FENCE)
                    g -= 1
    b.set(dx + 5, dy + 1, dz, FENCE)
    b.torch(dx + 5, dy + 2, dz)
    # the lighthouse: 5x5 sandstone shaft, gallery at +20/+21, open top until Morrow roofs it
    tx, ty, tz = A["june_tower"]
    height = {"march": 2, "april": 12}.get(stage, 21)
    b.pad(tx - 3, tz - 3, tx + 3, tz + 3, ty - 1, top=SAND, under=SAND)
    for y in range(ty, ty + min(height, 20)):
        for x in range(tx - 2, tx + 3):
            for z in range(tz - 2, tz + 3):
                edge = abs(x - tx) == 2 or abs(z - tz) == 2
                if edge:
                    band = ((y - ty) // 4) % 2
                    b.set(x, y, z, WOOL if band else SANDSTONE, 14 if band else 0)  # red/white bands
                else:
                    b.set(x, y, z, AIR, log=False)
    if height <= 12:
        return
    b.door(tx, ty, tz - 2, "north")
    b.ladder(tx, ty, ty + 19, tz + 1, "north")
    for x in range(tx - 3, tx + 4):          # gallery floor (ladder hole) and rail
        for z in range(tz - 3, tz + 4):
            if (x, z) == (tx, tz + 1):
                continue
            b.set(x, ty + 20, z, PLANKS)
            if abs(x - tx) == 3 or abs(z - tz) == 3:
                b.set(x, ty + 21, z, FENCE)
    b.set(tx, ty + 21, tz, FENCE)            # June's lamp stand; the lamp itself never came
    # lantern room: Morrow's glass roof + glowstone (18 May 04:11-04:16), staged for the shots
    roof = [(x, z) for x in range(tx - 1, tx + 2) for z in range(tz - 1, tz + 2)]
    placed = {"final": 9, "roofed": 9, "nearly": 7, "half": 4, "may15": 0}.get(stage, 0)
    prev = b.who
    b.who = "morrow"
    for x, z in roof[:placed]:
        b.set(x, ty + 23, z, GLASS)
    if stage in ("final", "roofed", "nearly"):
        b.set(tx, ty + 22, tz, GLOWSTONE)
    b.who = prev


def iona_rooms(b: Builder):
    """Iona's annex (July 2011, WorldEdit-square) with a stair tunnel, and her notes cell."""
    b.who = "i_bell"
    ax, ay, az = A["maintenance_annex"]
    b.fill(ax - 1, ay - 1, az - 6, ax + 12, ay + 4, az + 2, STONE, log=False)
    b.interior_air(ax, ay, az - 5, ax + 11, ay + 3, az)    # ID wall shelf sits on z = az (inject.py)
    for x in range(ax, ax + 12, 5):
        b.torch(x, ay, az - 5)
    # stair tunnel up toward the surface, heading north, sealed one block under the grass
    x, y, z = ax + 5, ay, az - 6
    while True:
        g = b.ground(x, z)
        top = min(y + 2, g - 1)
        for yy in range(y, top + 1):
            b.set(x, yy, z, AIR, log=False)
        b.set(x, y - 1, z, CSTAIRS, 3)
        if y + 2 >= g - 1:
            b.set(x, g, z, GRASS, log=False)  # she capped it; you dig the last block
            break
        y += 1
        z -= 1
    # her buried key chest (inject.py fills it): solid stone all round, so no cave shows its underside
    kx, ky, kz = A["iona_key_chest"]
    b.fill(kx - 1, ky - 2, kz - 1, kx + 2, ky + 1, kz + 1, STONE, log=False)
    # notes cell: inject.py digs it and hangs the signs; give it a stone shell so it stays a room
    nx, ny, nz = A["iona_notes"]
    b.fill(nx - 1, ny - 1, nz - 2, nx + 3, ny + 3, nz + 1, STONE, log=False)
    b.interior_air(nx, ny, nz - 1, nx + 2, ny + 2, nz)


def build(world_path: str, stage: str = "final") -> dict:
    w = World(world_path)
    b = Builder(w)
    spawn_plaza(b)
    rookery_house(b)
    ladder_room(b)
    finch_rail(b)
    patchcord_hut(b)
    cairn_farm(b)
    june_point(b, stage)
    iona_rooms(b)
    w.save()
    x0, z0, x1, z1 = b.box
    n = relight_blocks(w, x0, z0, x1, z1)
    w.save()
    out = Path(world_path) / "build-manifest.json"
    out.write_text(json.dumps(b.manifest))
    by = {}
    for m in b.manifest:
        by[m["who"]] = by.get(m["who"], 0) + 1
    return {"blocks": len(b.manifest), "by": by, "relit_chunks": n, "box": b.box}


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(2)
    st = sys.argv[sys.argv.index("--stage") + 1] if "--stage" in sys.argv else "final"
    print(build(sys.argv[1], st))
