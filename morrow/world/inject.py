"""Write the genuine-2011 cipher objects and the lived-in signs and chests into the Beta 1.5 world.

    python -m morrow.world.inject <path-to-beta-world> [--dry-run]

Only 2011-valid blocks and tile entities are written here (signs, wool, chests, note blocks,
Iona's WorldEdit strip, sheep). Morrow's and Theo's modern additions are placed later on Paper
by the plugin, after the world has been upgraded.

Positions come from morrow/canon/layout.py; every ordered run (patch's notes, the Residents' Key,
Iona's ID wall, Rook's ledger, the floor's rows) is laid in READING ORDER with layout.wall_run and
layout.floor_row_z, so a player reads it left to right from where they stand. The lived-in signs
are the ones in ciphers.GENUINE_SIGNS_2011 (Morrow reads them: they are in its corpus). Run the
canon build after changing any of it.
"""

from __future__ import annotations

import sys

from morrow.canon import ciphers, later, layout, logblock, people
from morrow.world import nbt
from morrow.world.mcregion import World

# Beta 1.5 block IDs
STONE, PLANKS, WOOL, NOTE, SIGN_POST, WALL_SIGN, CHEST, COBBLE, LEVER = 1, 5, 35, 25, 63, 68, 54, 4, 69
FACING = {"north": 2, "south": 3, "west": 4, "east": 5}  # wall sign / ladder metadata
ROT = {"south": 0, "west": 4, "north": 8, "east": 12}     # sign post: the way the text faces
ITEM_IDS = {"cobblestone": 4, "dirt": 3, "sand": 12, "gravel": 13, "planks": 5, "torch": 50, "arrow": 262, "wood": 17,
            "bread": 297, "minecart": 328, "paper": 339, "bone": 352, "cookie": 357, "stone_pickaxe": 274, "stone_axe": 275,
            "stone_shovel": 273, "bed": 355, "workbench": 58, "glass": 20, "wool": 35, "iron_ingot": 265, "compass": 345,
            "redstone": 331, "book": 340, "wheat": 296, "seeds": 295, "rail": 66, "redstone_torch": 76, "powered_rail": 27,
            "coal": 263, "string": 287, "sapling": 6, "lever": 69}
WOOL_DYE_FOR_SHEEP = {r.handle: 15 - r.sheep_dye for r in people.RESIDENTS}  # sheep Color uses wool values
GROUND_SKIP = {0, 6, 17, 18, 31, 32, 37, 38, 39, 40, 78, 81, 83}


def sign_lines(lines: list[str]) -> dict:
    lines = (list(lines) + ["", "", "", ""])[:4]
    for line in lines:
        assert len(line) <= 15, "2011 signs hold 15 characters per line: %r" % line
    return {"Text%d" % (n + 1): nbt.st(t) for n, t in enumerate(lines)}


def post_sign(w: World, x, y, z, lines, rotation=8):
    if w.get(x, y - 1, z)[0] in GROUND_SKIP:
        w.set(x, y - 1, z, COBBLE)
    w.set(x, y, z, SIGN_POST, rotation)
    w.set_tile(x, y, z, "Sign", **sign_lines(lines))


def wall_sign(w: World, x, y, z, lines, facing="north", support=STONE):
    dz = {"north": 1, "south": -1}.get(facing, 0)
    dx = {"west": 1, "east": -1}.get(facing, 0)
    if w.get(x + dx, y, z + dz)[0] == 0:
        w.set(x + dx, y, z + dz, support)
    w.set(x, y, z, WALL_SIGN, FACING[facing])
    w.set_tile(x, y, z, "Sign", **sign_lines(lines))


def chest(w: World, x, y, z, items: list[tuple[int, str, int]], damage: dict | None = None):
    """items: (slot, item name, count); damage: slot -> damage value (wool colours)."""
    damage = damage or {}
    w.set(x, y, z, CHEST)
    w.set_tile(x, y, z, "Chest", Items=nbt.lst(nbt.COMPOUND, [
        nbt.comp(id=nbt.s(ITEM_IDS[name]), Count=nbt.b(count), Damage=nbt.s(damage.get(slot, 0)), Slot=nbt.b(slot))
        for slot, name, count in items]))


def ground(w: World, x, z) -> int:
    y = min(w.height(x, z), 127)
    while y > 1:
        y -= 1
        if w.get(x, y, z)[0] not in GROUND_SKIP:
            return y
    return 63


def inject(world_path: str) -> list[str]:
    w = World(world_path)
    a = layout.ANCHORS
    S = ciphers.GENUINE_SIGNS_2011
    done = []

    # Spawn: the three genuine rules signs and Rookery's warning, on their plinth.
    sx, sy, sz = a["rules_signs"]
    for i, sign in enumerate(ciphers.RULES_SIGNS_2011):
        post_sign(w, sx + i, sy, sz, sign, ROT["south"])
    post_sign(w, sx + 4, sy, sz, S["spawn_warning"], ROT["south"])
    # finch's genuine sign at the end of the south branch (Morrow keeps building past it)
    ex, ey, ez = a["south_rail_end"]
    ox, oy, oz = ciphers.FINCH_SOUTH_SIGN_OFFSET
    post_sign(w, ex + ox, ey + oy, ez + oz, S["finch_south_rail"], ROT["south"])
    done.append("spawn signs")

    # The newbie chest and the bread chest, their signs; the bulletin board; the portal, creeper and tree signs.
    kx, ky, kz = a["spawn_kit_chest"]
    kit = [(i, n, c) for i, (n, c) in enumerate([("stone_pickaxe", 1), ("stone_axe", 1), ("stone_shovel", 1), ("torch", 16),
                                                 ("bread", 8), ("bed", 1), ("wood", 16), ("planks", 8), ("workbench", 1)])]
    chest(w, kx, ky, kz, kit)
    chest(w, kx, ky, kz + 1, [])
    post_sign(w, kx - 1, ky, kz, S["spawn_kit_chest"], ROT["east"])
    bx, by, bz = a["spawn_bread_chest"]
    chest(w, bx, by, bz, [(0, "bread", 20)])
    post_sign(w, bx - 1, by, bz, S["spawn_bread"], ROT["east"])
    ox2, oy2, oz2 = a["spawn_board"]
    for k in range(6):
        wall_sign(w, ox2 + k % 3, oy2 - k // 3, oz2, S["board_%d" % (k + 1)], "north", support=PLANKS)
    px, py, pz = a["spawn_portal"]
    post_sign(w, px - 1, py, pz + 2, S["spawn_portal"], ROT["west"])
    cx, cy, cz = a["spawn_creeper"]
    post_sign(w, cx + 3, cy, cz + 2, S["spawn_creeper"], ROT["south"])
    chest(w, cx - 1, cy, cz, [(0, "wool", 20), (1, "wool", 12), (2, "wool", 3)], {0: 13, 1: 15, 2: 5})
    spx, spy, spz = a["spawn"]
    tx, tz = spx - 7, spz + 2
    post_sign(w, tx, ground(w, tx, tz) + 3, tz, S["spawn_tree"], ROT["east"])
    done.append("spawn chests, board, portal and creeper signs")

    # Rook's house: door sign, boots sign, the pillar's note, the ledger (read facing west, top row first), front-room chests.
    rx, ry, rz = a["rookery_house"]
    x0, x1, z0, z1 = rx - 4, rx + 4, rz, rz + 7
    wall_sign(w, rx, ry + 1, rz - 1, S["rookery_door"], "north")
    wall_sign(w, rx + 2, ry + 1, z0 + 1, S["rookery_boots"], "south", support=COBBLE)
    wall_sign(w, x1, ry + 1, z1 + 1, S["rookery_pillar"], "west", support=3)
    lx0, ly0, lz0 = a["rookery_changes_wall"]
    for i in range(6):
        x, y, z = layout.wall_run("west", (lx0, ly0 - i // 3, lz0), i % 3)
        wall_sign(w, x, y, z, S["ledger_%d" % (i + 1)], "east", support=COBBLE)
    chest(w, x0 + 1, ry, z0 + 3, [(0, "cobblestone", 64), (1, "cobblestone", 40), (2, "dirt", 31), (3, "sapling", 4), (4, "torch", 9)])
    chest(w, x0 + 2, ry, z0 + 3, [(0, "coal", 22), (1, "string", 7), (2, "bread", 2)])
    done.append("rook's signs and chests")

    # C6 Rookery's cellar floor, exactly as laid in June 2011: row r sits at z + floor_row_z(r), nearest the ladder first.
    fx, fy, fz = a["rookery_cellar_floor"]
    floor = ciphers.build_floor()
    for r, row in enumerate(floor.grid):
        for c, wool in enumerate(row):
            w.set(fx + c, fy, fz + layout.floor_row_z(r), WOOL, wool)
    sgx, sgy, sgz = a["rookery_floor_sign"]
    wall_sign(w, sgx, sgy, sgz, S["rookery_floor"], "north", support=COBBLE)
    done.append("rookery floor 18x9 (rows nearest the ladder first)")

    # C8 Iona's buried key chest: a double chest, slots 0-26 in the west half, 27-53 in the east.
    kx2, ky2, kz2 = a["iona_key_chest"]
    slots = ciphers.build_iona_chest()
    chest(w, kx2, ky2, kz2, [(s["slot"], s["item"], s["count"]) for s in slots if s["slot"] < 27])
    chest(w, kx2 + 1, ky2, kz2, [(s["slot"] - 27, s["item"], s["count"]) for s in slots if s["slot"] >= 27])
    done.append("iona key chest (%d stacks)" % len(slots))

    # D6 Iona's block-ID strip along the annex's south wall: HALTMRRW as pre-1.13 block IDs, read facing south.
    ax, ay, az = a["maintenance_annex"]
    lx, ly, lz = layout.ID_WALL_LEFT
    for i, block in enumerate(ciphers.build_id_wall()):
        x, y, z = layout.wall_run("south", (ax + lx, ay + ly, az + lz), i)
        w.set(x, y - 1, z, STONE)          # shelf floor
        w.set(x, y, z + 1, STONE)          # back wall
        meta = {65: FACING["north"], 77: 4, 76: 5}.get(block["id"], 0)  # ladder/button face north, torch upright
        w.set(x, y, z, block["id"], meta)
    chest(w, ax + 5, ay, az - 5, [(0, "redstone", 30), (1, "paper", 8), (2, "book", 2), (3, "torch", 14)])
    done.append("annex id wall (reading order) and desk chest")

    # C2 patchcord's note-block wall, read facing south from the door: note i at x + 10 - i; planks under = bass.
    px2, py2, pz2 = a["patchcord_relay_hut"]
    nlx, nly, nlz = layout.PATCH_NOTE_LEFT
    for i, (pitch, base) in enumerate(ciphers.TUNE):
        x, y, z = layout.wall_run("south", (px2 + nlx, py2 + nly, pz2 + nlz), i)
        w.set(x, y - 1, z, PLANKS if base == "planks" else STONE)
        w.set(x, y, z, NOTE)
        w.set_tile(x, y, z, "Music", note=nbt.b(pitch))
    first = layout.wall_run("south", (px2 + nlx, py2 + nly, pz2 + nlz), 0)
    post_sign(w, first[0], first[1] + 1, first[2], S["patch_play"], ROT["north"])
    wall_sign(w, px2 + 4, py2 + 1, pz2, S["patch_mumble"], "south", support=COBBLE)
    chest(w, px2 + len(ciphers.TUNE) - 1, py2, pz2, [(0, "paper", 12), (1, "redstone", 6)])
    done.append("patchcord tune (reading order), studio signs, tapes")

    # D15 Rookery's ladder room: the Residents' Key sign wall, read facing south (preface signs first).
    lx2, ly2, lz2 = a["rookery_ladder_room"]
    klx, kly, klz = layout.RESIDENTS_KEY_LEFT
    for i, lines in enumerate(ciphers.build_residents_key()["signs"]):
        x, y, z = layout.wall_run("south", (lx2 + klx, ly2 + kly, lz2 + klz), i)
        wall_sign(w, x, y, z, lines, "north")
    chest(w, lx2 + 3, ly2, lz2 - 4, [(0, "cobblestone", 64), (1, "torch", 23), (2, "bread", 3), (3, "iron_ingot", 5), (4, "compass", 1)])
    chest(w, lx2 + 4, ly2, lz2 - 4, [(0, "planks", 48), (1, "rail", 9), (2, "wool", 6)], {2: 14})
    done.append("residents' key signs (reading order)")

    # cairn's storehouse: tally chests in join order with his name signs, the fixtures' sign, the bread chest.
    cx2, cy2, cz2 = a["cairn_storehouse_door_2011"]
    short = {"rookery": "rook", "cairn": "cairn", "finchline": "finch", "juni_lantern": "june", "patchcord": "patch", "kitbag": "kit"}
    for row, r in enumerate(people.join_order()):
        weeks = ciphers.TALLY[r.handle]
        items = [(slot, ciphers.TALLY_ITEM, n) for slot, n in enumerate(weeks) if n]
        chest(w, cx2 + 2, cy2, cz2 + 2 + row * 2, items)
        post_sign(w, cx2 + 2, cy2 + 1, cz2 + 2 + row * 2, S["barn_name_" + short[r.handle]], ROT["west"])
    wall_sign(w, cx2 + 2, cy2 + 1, cz2 + 1, S["barn_closeup"], "south", support=PLANKS)
    chest(w, cx2 - 1, cy2, cz2 + 1, [(0, "bread", 24)])
    # the shed's bed gap, cairn's hut, the pen gate, the grinder, the farm stop
    hx, hy, hz = a["cairn_shed_hatch"]
    wall_sign(w, hx - 1, hy + 1, hz - 1, S["shed_bed"], "south", support=PLANKS)
    fx2, fy2, fz2 = a["cairn_farm"]
    hx2, hz2 = fx2 - 8, fz2 + 2
    post_sign(w, hx2 + 1, ground(w, hx2 + 1, hz2 + 5) + 1, hz2 + 5, S["crater"], ROT["south"])
    sx2, sy2, sz2 = a["sheep_pen"]
    post_sign(w, sx2 + 11, sy2 + 1, sz2 + 3, S["pen_gate"], ROT["east"])
    gx, gz = cx2 + 28, cz2 - 10
    post_sign(w, gx + 3, ground(w, gx + 3, gz + 1) + 1, gz + 1, S["grinder"], ROT["west"])
    stop_x, stop_z = cx2 - 24, a["finch_corner"][2]
    wall_sign(w, stop_x, ground(w, stop_x, stop_z + 2) + 2, stop_z + 1, S["farm_stop"], "north", support=COBBLE)
    done.append("cairn tally chests, name signs, bread, shed, hut, pen, grinder, farm stop")

    # finch's hub: the timetable, the spare carts, the first-booster sign, the junction signs.
    hx3, hy3, hz3 = a["finch_hub"]
    for i in range(3):
        wall_sign(w, hx3 + 1 + i, hy3 + 1, hz3 - 4, S["hub_timetable_%d" % (i + 1)], "south", support=PLANKS)
    chest(w, hx3 - 3, hy3 + 1, hz3 + 1, [(0, "minecart", 2), (1, "rail", 30), (2, "redstone_torch", 4)])
    post_sign(w, hx3 - 1, hy3, hz3 - 1, S["finch_rail_start"], ROT["east"])
    for k, (key, (off, _on)) in enumerate(logblock.JUNCTION_LEVERS.items()):
        jx, jy, jz = a[key]
        if off[0]:
            post_sign(w, jx + off[0], jy, jz + 1, S["jct_%d" % (k + 1)], ROT["south"])
        else:
            post_sign(w, jx - 1, jy, jz + off[2], S["jct_%d" % (k + 1)], ROT["south"])
    done.append("finch's station signs, carts, junction signs")

    # June's cottage and tower: F2, the flag locker, the lamp sign.
    jx2, jy2, jz2 = a["june_house"]
    wall_sign(w, jx2 - 3, jy2 + 1, jz2 - 1, S["june_f2"], "south", support=PLANKS)
    chest(w, jx2 - 2, jy2, jz2 - 1, [(i, "wool", 8) for i in range(6)], {0: 14, 1: 0, 2: 11, 3: 4, 4: 15, 5: 5})
    chest(w, jx2 - 3, jy2, jz2 + 2, [(0, "glass", 64), (1, "glass", 40), (2, "sand", 32)])
    tx2, ty2, tz2 = a["june_tower"]
    post_sign(w, tx2 - 1, ty2, tz2 - 3, S["june_lamp"], ROT["north"])
    done.append("june's signs and chests")

    # C7 Iona's notes room at r.-1.0.mcr c(27,14) y40: a dug cell with her two signs (the first on the reader's left).
    nx, ny, nz = a["iona_notes"]
    for dx in range(0, 3):
        for dy in range(0, 3):
            w.set(nx + dx, ny + dy, nz, 0)
    for i, lines in enumerate(later.IONA_NOTES_SIGNS):
        wall_sign(w, nx + 1 - i, ny + 1, nz, lines, "north")
    done.append("iona notes signs")

    # The annex manhole: the tunnel's cap, marked (build2011 records where the stair tunnel surfaces).
    try:
        import json
        from pathlib import Path
        man = Path(world_path) / "build-manifest.json"
        meta = json.loads((Path(world_path) / "build-extras.json").read_text()) if (Path(world_path) / "build-extras.json").exists() else {}
        if "manhole" in meta:
            mx, my, mz = meta["manhole"]
            post_sign(w, mx + 1, my + 1, mz, S["annex_manhole"], ROT["south"])
            done.append("annex manhole sign")
        _ = man
    except (OSError, ValueError):
        pass

    # Kit's base: his signs and the kennel's name signs and chest; the x-ray cave's sign.
    kx3, ky3, kz3 = a["kit_genuine_door"]
    try:
        wall_sign(w, kx3 - 1, ky3 + 1, kz3 + 1, S["kit_base_1"], "south", support=3)
        wall_sign(w, kx3 + 1, ky3 + 1, kz3 + 1, S["kit_base_2"], "south", support=3)
        bx3, by3, bz3 = a["kit_fossil_base"]
        for n, dx in enumerate((5, 6, 7)):
            post_sign(w, bx3 + dx, ground(w, bx3 + dx, bz3) + 1, bz3, S["kit_biscuit_%d" % (n + 2)], ROT["south"])
        g = ground(w, bx3 + 8, bz3 + 2)
        chest(w, bx3 + 8, g + 1, bz3 + 2, [(0, "bone", 7), (1, "cookie", 4), (2, "string", 3)])
        done.append("kit's signs, kennel")
    except KeyError:
        print("  warning: Kit's base chunk isn't generated yet (the owner's walk); skipped his signs")
        done.append("kit's base pending the walk")
    xx, xz = spx - 7, spz + 10
    post_sign(w, xx - 2, 14, xz + 29, S["xray_cave"], ROT["north"])
    done.append("x-ray cave sign")

    # Sheep census: one dyed sheep per resident, inner pen = online the week of 16 May.
    lv = w.level(sx2, sz2)
    ents = lv["Entities"].value
    if ents.elem != nbt.COMPOUND:  # Beta writes empty lists typed as BYTE, not END
        ents.elem = nbt.COMPOUND
    for i, r in enumerate(people.join_order()):
        inner = r.handle in ciphers.SHEEP_INNER
        x = sx2 + (2 + i % 2 if inner else 7 + i % 2) + 0.5
        z = sz2 + 2 + i + 0.5
        ents.items.append(nbt.comp(
            id=nbt.st("Sheep"), Pos=nbt.lst(nbt.DOUBLE, [nbt.Tag(nbt.DOUBLE, v) for v in (x, sy2 + 1.0, z)]),
            Motion=nbt.lst(nbt.DOUBLE, [nbt.Tag(nbt.DOUBLE, 0.0)] * 3),
            Rotation=nbt.lst(nbt.FLOAT, [nbt.Tag(nbt.FLOAT, 0.0)] * 2),
            FallDistance=nbt.Tag(nbt.FLOAT, 0.0), Fire=nbt.s(-1), Air=nbt.s(300), OnGround=nbt.b(1),
            Health=nbt.s(8), HurtTime=nbt.s(0), DeathTime=nbt.s(0), AttackTime=nbt.s(0),
            Sheared=nbt.b(0), Color=nbt.b(WOOL_DYE_FOR_SHEEP[r.handle])))
    w._mark(sx2, sz2)
    done.append("sheep census")

    w.save()
    return done


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2
    if layout.PROVISIONAL:
        print("warning: layout anchors are still PROVISIONAL (see morrow/canon/layout.py)")
    for step in inject(argv[1]):
        print("  injected:", step)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
