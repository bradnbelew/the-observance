"""Round-trip test: synthetic Beta world -> inject -> reload from disk -> decode every cipher.

    python -m morrow.world.selftest
"""

from __future__ import annotations

import shutil
import sys
import tempfile

from morrow.canon import ciphers, layout, people
from morrow.world import inject
from morrow.world.mcregion import Region, World, new_chunk, region_path


def make_world(path: str) -> None:
    needed = set()
    for (x, _y, z) in layout.ANCHORS.values():
        for dx in range(-16, 44):      # wide enough for the lived-in extras round each place
            for dz in range(-44, 46):
                needed.add(((x + dx) >> 4, (z + dz) >> 4))
    regions: dict[str, Region] = {}
    for cx, cz in needed:
        p = region_path(path, cx, cz)
        reg = regions.setdefault(p, Region(p))
        reg.chunks[(cx & 31, cz & 31)] = new_chunk(cx, cz)
        reg.mark(cx & 31, cz & 31)
    for reg in regions.values():
        reg.save()


def main() -> int:
    tmp = tempfile.mkdtemp(prefix="mossfield-selftest-")
    fails = []

    def check(ok, msg):
        print(("  ok   " if ok else "  FAIL ") + msg)
        if not ok:
            fails.append(msg)

    try:
        make_world(tmp)
        for step in inject.inject(tmp):
            print("  injected:", step)
        w = World(tmp)  # fresh instance: everything below is read back from disk
        a = layout.ANCHORS

        fx, fy, fz = a["rookery_cellar_floor"]
        # read as a player does: from the landing (south), the nearest row first, west to east then back
        grid = [[w.get(fx + c, fy, fz + layout.floor_row_z(r))[1] for c in range(ciphers.FLOOR_COLS)] for r in range(ciphers.FLOOR_ROWS)]
        check(all(w.get(fx + c, fy, fz + r)[0] == 35 for r in range(9) for c in range(18)), "floor is all wool")
        check(ciphers.read_floor(grid, ciphers.RULES_SIGNS_2011) == ciphers.FLOOR_PLAINTEXT, "floor decodes from the world, nearest row first")
        check(layout.floor_row_z(0) == 8, "row 1 is the row by the ladder")

        kx, ky, kz = a["iona_key_chest"]
        slots = []
        for half, x in ((0, kx), (27, kx + 1)):
            for t in w.tiles(x, kz):
                if t["id"].value == "Chest" and t["x"].value == x and t["y"].value == ky:
                    slots += [{"slot": it.value["Slot"].value + half, "count": it.value["Count"].value} for it in t["Items"].value.items]
        check(ciphers.read_iona_chest(slots) == ciphers.IONA_TOKEN, "Iona's chest decodes from the world")

        ax, ay, az = a["maintenance_annex"]
        lx0, ly0, lz0 = layout.ID_WALL_LEFT
        ids = [w.get(*layout.wall_run("south", (ax + lx0, ay + ly0, az + lz0), i))[0] for i in range(8)]
        check("".join(chr(i) for i in ids) == ciphers.HALT_COMMAND, "annex wall reads HALTMRRW left to right, facing south")
        check(layout.wall_run("south", (ax + lx0, ay + ly0, az + lz0), 1)[0] < ax + lx0, "the wall runs west (the reader's right)")

        px, py, pz = a["patchcord_relay_hut"]
        nl = layout.PATCH_NOTE_LEFT
        tune = []
        for i in range(len(ciphers.TUNE)):
            x, y, z = layout.wall_run("south", (px + nl[0], py + nl[1], pz + nl[2]), i)
            note = next(t["note"].value for t in w.tiles(x, z) if t["id"].value == "Music" and t["x"].value == x)
            base = "planks" if w.get(x, y - 1, z)[0] == 5 else "stone"
            tune.append((note, base))
        check(ciphers.read_tune(tune) == ciphers.TUNE_WORD, "note blocks spell BAGGAGE left to right, facing south")

        lx, ly, lz = a["rookery_ladder_room"]
        kl = layout.RESIDENTS_KEY_LEFT
        signs = []
        for i in range(len(ciphers.build_residents_key()["signs"])):
            x, y, z = layout.wall_run("south", (lx + kl[0], ly + kl[1], lz + kl[2]), i)
            t = next(t for t in w.tiles(x, z) if t["id"].value == "Sign" and t["x"].value == x and t["y"].value == y)
            signs.append([t["Text%d" % n].value for n in range(1, 5)])
        ct = "".join(l for s in signs[2:] for l in s)
        k1, k2 = ciphers.residents_keys()
        check(ciphers.read_residents_key(ct, k1, k2) == ciphers.RESIDENTS_PLAINTEXT, "Residents' Key decodes from the sign wall")

        sx, sy, sz = a["rules_signs"]
        rules = [[t["Text%d" % n].value for n in range(1, 5)] for i in range(3)
                 for t in w.tiles(sx + i, sz) if t["id"].value == "Sign" and t["x"].value == sx + i]
        check(rules == ciphers.RULES_SIGNS_2011, "spawn rules signs match the 2011 key")

        sheep = [e.value for e in w.level(a["sheep_pen"][0], a["sheep_pen"][2])["Entities"].value.items]
        check(len(sheep) == len(people.RESIDENTS), "six sheep in the census pen")

        # the lived-in set: every sign in the corpus stands in the world, each within 15 characters
        from morrow.canon.ciphers import GENUINE_SIGNS_2011
        placed = set()
        for (cx, cz) in w.generated_chunks():
            for t in w.tiles(cx * 16, cz * 16):
                if t["id"].value == "Sign":
                    placed.add(tuple(t["Text%d" % n].value for n in range(1, 5)))
        plugin_only = {"finch_station_stop", "annex_manhole"}  # placed on Paper / needs the real terrain
        missing = [k for k, v in GENUINE_SIGNS_2011.items() if k not in plugin_only and tuple((v + ["", "", "", ""])[:4]) not in placed]
        check(not missing, "every lived-in sign is in the world: missing %s" % missing)
        kx, ky, kz = a["spawn_kit_chest"]
        kit = [t for t in w.tiles(kx, kz) if t["id"].value == "Chest" and t["x"].value == kx and t["z"].value == kz]
        check(bool(kit) and len(kit[0]["Items"].value.items) == 9, "Rook's newbie chest holds the nine kit items")
        lx1, ly1, lz1 = layout.wall_run("west", a["rookery_changes_wall"], 0)
        first = next((t for t in w.tiles(lx1, lz1) if t["id"].value == "Sign" and t["x"].value == lx1 and t["y"].value == ly1), None)
        check(first is not None and first["Text1"].value == "5/19 june: roof", "Rook's ledger starts at the reader's left (facing west)")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print("\n%s" % ("all world checks passed" if not fails else "%d FAILURES" % len(fails)))
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
