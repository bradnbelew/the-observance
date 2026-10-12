"""Write the genuine-2011 cipher objects into the Beta 1.5 world.

    python -m morrow.world.inject <path-to-beta-world> [--dry-run]

Only 2011-valid blocks and tile entities are written here (signs, wool, chests, note
blocks, Iona's WorldEdit strip, sheep). Morrow's and Theo's modern additions are placed
later on Paper by the plugin, after the world has been upgraded.

Positions come from morrow/canon/layout.py. Run the canon build after changing them.
"""

from __future__ import annotations

import sys

from morrow.canon import ciphers, later, layout, people
from morrow.world import nbt
from morrow.world.mcregion import World

# Beta 1.5 block IDs
STONE, PLANKS, WOOL, NOTE, SIGN_POST, WALL_SIGN, CHEST = 1, 5, 35, 25, 63, 68, 54
FACING = {"north": 2, "south": 3, "west": 4, "east": 5}  # wall sign / ladder metadata
ITEM_IDS = {"cobblestone": 4, "dirt": 3, "sand": 12, "gravel": 13, "planks": 5, "torch": 50, "arrow": 262, "wood": 17}
WOOL_DYE_FOR_SHEEP = {r.handle: 15 - r.sheep_dye for r in people.RESIDENTS}  # sheep Color uses wool values


def sign_lines(lines: list[str]) -> dict:
    lines = (lines + ["", "", "", ""])[:4]
    for line in lines:
        assert len(line) <= 15, "2011 signs hold 15 characters per line: %r" % line
    return {"Text%d" % (n + 1): nbt.st(t) for n, t in enumerate(lines)}


def post_sign(w: World, x, y, z, lines, rotation=8):
    w.set(x, y, z, SIGN_POST, rotation)
    w.set_tile(x, y, z, "Sign", **sign_lines(lines))


def wall_sign(w: World, x, y, z, lines, facing="north", support=STONE):
    dz = {"north": 1, "south": -1}.get(facing, 0)
    dx = {"west": 1, "east": -1}.get(facing, 0)
    if w.get(x + dx, y, z + dz)[0] == 0:
        w.set(x + dx, y, z + dz, support)
    w.set(x, y, z, WALL_SIGN, FACING[facing])
    w.set_tile(x, y, z, "Sign", **sign_lines(lines))


def chest(w: World, x, y, z, items: list[tuple[int, str, int]]):
    """items: (slot, item name, count)."""
    w.set(x, y, z, CHEST)
    w.set_tile(x, y, z, "Chest", Items=nbt.lst(nbt.COMPOUND, [
        nbt.comp(id=nbt.s(ITEM_IDS[name]), Count=nbt.b(count), Damage=nbt.s(0), Slot=nbt.b(slot))
        for slot, name, count in items]))


def inject(world_path: str) -> list[str]:
    w = World(world_path)
    a = layout.ANCHORS
    done = []

    # Spawn: the three genuine rules signs and Rookery's warning.
    sx, sy, sz = a["rules_signs"]
    for i, sign in enumerate(ciphers.RULES_SIGNS_2011):
        post_sign(w, sx + i, sy, sz, sign)
    post_sign(w, sx + 4, sy, sz, ["screenshot", "stuff. it", "changes. trust", "me -rook"])
    done.append("spawn signs")

    # C6 Rookery's cellar floor, exactly as laid in June 2011 (Morrow's tidying happens later, on Paper).
    fx, fy, fz = a["rookery_cellar_floor"]
    floor = ciphers.build_floor()
    for r, row in enumerate(floor.grid):
        for c, wool in enumerate(row):
            w.set(fx + c, fy, fz + r, WOOL, wool)
    done.append("rookery floor 18x9")

    # C8 Iona's buried key chest: a double chest, slots 0-26 in the west half, 27-53 in the east.
    kx, ky, kz = a["iona_key_chest"]
    slots = ciphers.build_iona_chest()
    chest(w, kx, ky, kz, [(s["slot"], s["item"], s["count"]) for s in slots if s["slot"] < 27])
    chest(w, kx + 1, ky, kz, [(s["slot"] - 27, s["item"], s["count"]) for s in slots if s["slot"] >= 27])
    done.append("iona key chest (%d stacks)" % len(slots))

    # D6 Iona's block-ID strip in the annex wall: HALTMRRW as pre-1.13 block IDs.
    ax, ay, az = a["maintenance_annex"]
    for i, block in enumerate(ciphers.build_id_wall()):
        x = ax + 2 + i
        w.set(x, ay, az, STONE)          # shelf floor
        w.set(x, ay + 1, az + 1, STONE)  # back wall
        meta = {65: FACING["north"], 77: 4, 76: 5}.get(block["id"], 0)  # ladder/button face north, torch upright
        w.set(x, ay + 1, az, block["id"], meta)
    done.append("annex id wall")

    # C2 patchcord's note-block wall: pitch set by the tune; planks under = bass (the word), stone = drum.
    px, py, pz = a["patchcord_relay_hut"]
    for i, (pitch, base) in enumerate(ciphers.TUNE):
        w.set(px + i, py, pz + 2, PLANKS if base == "planks" else STONE)
        w.set(px + i, py + 1, pz + 2, NOTE)
        w.set_tile(px + i, py + 1, pz + 2, "Music", note=nbt.b(pitch))
    done.append("patchcord tune")

    # D15 Rookery's ladder room: the Residents' Key sign wall.
    lx, ly, lz = a["rookery_ladder_room"]
    for i, lines in enumerate(ciphers.build_residents_key()["signs"]):
        wall_sign(w, lx + i, ly + 1, lz, lines, "north")
    done.append("residents' key signs")

    # cairn's storehouse tally: one chest per resident in join order, one stack per week.
    cx, cy, cz = a["cairn_storehouse_door_2011"]
    for row, r in enumerate(people.join_order()):
        weeks = ciphers.TALLY[r.handle]
        items = [(slot, ciphers.TALLY_ITEM, n) for slot, n in enumerate(weeks) if n]
        chest(w, cx + 2, cy, cz + 2 + row * 2, items)
    done.append("cairn tally chests")

    # C7 Iona's notes room at r.-1.0.mcr c(27,14) y40: a dug cell with her two signs.
    nx, ny, nz = a["iona_notes"]
    for dx in range(0, 3):
        for dy in range(0, 3):
            w.set(nx + dx, ny + dy, nz, 0)
    for i, lines in enumerate(later.IONA_NOTES_SIGNS):
        wall_sign(w, nx + i, ny + 1, nz, lines, "north")
    done.append("iona notes signs")

    # Rookery's door and Kit's base.
    rx, ry, rz = a["rookery_house"]
    wall_sign(w, rx, ry + 1, rz - 1, ["ROOK'S", "boots off", "yes, yours too"], "north")
    kx2, ky2, kz2 = a["kit_fossil_base"]
    post_sign(w, kx2, ky2, kz2, ["KIT ONLY", "no theo", "no rook", "no girls"])
    post_sign(w, kx2 + 1, ky2, kz2, ["pw is my dogs", "name"])
    done.append("door and kit signs")

    # Sheep census: one dyed sheep per resident, inner pen = online the week of 16 May.
    sx2, sy2, sz2 = a["sheep_pen"]
    lv = w.level(sx2, sz2)
    ents = lv["Entities"].value
    if ents.elem == nbt.END:
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
