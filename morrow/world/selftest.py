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
        for dx in range(-2, 24):
            for dz in range(-2, 16):
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
        grid = [[w.get(fx + c, fy, fz + r)[1] for c in range(ciphers.FLOOR_COLS)] for r in range(ciphers.FLOOR_ROWS)]
        check(all(w.get(fx + c, fy, fz + r)[0] == 35 for r in range(9) for c in range(18)), "floor is all wool")
        check(ciphers.read_floor(grid, ciphers.RULES_SIGNS_2011) == ciphers.FLOOR_PLAINTEXT, "floor decodes from the world")

        kx, ky, kz = a["iona_key_chest"]
        slots = []
        for half, x in ((0, kx), (27, kx + 1)):
            for t in w.tiles(x, kz):
                if t["id"].value == "Chest" and t["x"].value == x and t["y"].value == ky:
                    slots += [{"slot": it.value["Slot"].value + half, "count": it.value["Count"].value} for it in t["Items"].value.items]
        check(ciphers.read_iona_chest(slots) == ciphers.IONA_TOKEN, "Iona's chest decodes from the world")

        ax, ay, az = a["maintenance_annex"]
        check("".join(chr(w.get(ax + 2 + i, ay + 1, az)[0]) for i in range(8)) == ciphers.HALT_COMMAND, "annex wall reads HALTMRRW")

        px, py, pz = a["patchcord_relay_hut"]
        tune = []
        for i in range(len(ciphers.TUNE)):
            note = next(t["note"].value for t in w.tiles(px + i, pz + 2) if t["id"].value == "Music" and t["x"].value == px + i)
            base = "planks" if w.get(px + i, py, pz + 2)[0] == 5 else "stone"
            tune.append((note, base))
        check(ciphers.read_tune(tune) == ciphers.TUNE_WORD, "note blocks spell BAGGAGE")

        lx, ly, lz = a["rookery_ladder_room"]
        signs = []
        for i in range(len(ciphers.build_residents_key()["signs"])):
            t = next(t for t in w.tiles(lx + i, lz) if t["id"].value == "Sign" and t["x"].value == lx + i)
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
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print("\n%s" % ("all world checks passed" if not fails else "%d FAILURES" % len(fails)))
    return 1 if fails else 0


if __name__ == "__main__":
    sys.exit(main())
