"""Prepare one Beta save per June screenshot: right build stage, time of day, camera in place.

    python -m morrow.world.shots <pristine-world> <saves-dir> [--only N]

For each shot this copies the untouched Beta world, builds 2011 Mossfield at the right stage
(June's tower height and roof progress), injects the cipher objects (signs, sheep), relights,
stops the weather, sets the clock, and puts the player exactly where the camera should be,
facing the subject. The owner opens the save in old_beta b1.5_01, presses F1 (unless the shot
needs chat) and F2, quits, and renames the screenshot to the filename given.

Beta sky: before Beta 1.9 pre-release 4 the sun and moon rose in the NORTH (-z) and had no
phases (a square moon). So "moon low in the north" is just after dusk, looking toward -z.
Yaw: 0 faces south (+z), 90 west, 180 north, 270 east. Pitch: positive looks down.
"""

from __future__ import annotations

import math
import shutil
import sys
from pathlib import Path

from morrow.canon import layout
from morrow.world import build2011, inject, nbt
from morrow.world.mcregion import World
from morrow.world.relight import relight_blocks

A = layout.ANCHORS


def _aim(cam, target):
    dx, dy, dz = target[0] - cam[0], target[1] - (cam[1] + 1.62), target[2] - cam[2]
    yaw = math.degrees(math.atan2(-dx, dz)) % 360
    pitch = -math.degrees(math.atan2(dy, math.hypot(dx, dz)))
    return yaw, pitch


def shots() -> list[dict]:
    """In date order (shot N = row N of SHOT-LIST.md). Cameras derive from the layout anchors.

    June had a spot: the hill north-east of the point, the highest ground with a clear line to the
    whole lighthouse (she keeps the trees cut along it). The roof shots are all from there, so they
    agree with each other. Every stand was checked for a clear line of sight to its subject on the
    built world (morrow/world/view.py renders what each save sees). stand = ("ground", x, z) stands on
    the ground there; ("sea", x, z) on one sand block at sea level; ("at", x, y, z) on a given block.
    """
    tx, ty, tz = A["june_tower"]
    hx, hy, hz = A["june_house"]
    dx, dy, dz = A["june_old_dock"]
    sx, sy, sz = A["spawn"]
    rx, ry, rz = A["rules_signs"]
    px, py, pz = A["sheep_pen"]
    spot = ("ground", tx + 18, tz - 27)                     # June's hill, 33 blocks NE of the tower
    top = (tx + 0.5, ty + 15, tz + 0.5)
    return [
        dict(file="2011-03-02_19.20.11.png", stage="march", time=7000, stand=("ground", tx + 10, tz - 19),
             look=(hx - 2, hy, (hz + tz) / 2 - 1), fast=True,
             note="June's first build: the house on the point and the tower base, two blocks high. Daytime."),
        dict(file="2011-03-30_20.44.05.png", stage="april", time=12000, stand=("ground", tx + 2, tz - 7),
             look=(dx + 4, dy, dz), fast=True,
             note="The wooden dock at sunset, the shoreline either side of it."),
        dict(file="2011-05-14_21.40.02.png", stage="may15", time=11500, stand=("ground", sx - 1, sz),
             look=(rx + 2, ry + 0.6, rz + 1.5), group=True,
             note="Spawn: the cake on its table, the three rules signs readable behind (b4 visible)."),
        dict(file="2011-05-15_22.10.30.png", stage="may15", time=14400, stand=("sea", tx - 10, tz + 45),
             look=(tx - 3, ty + 18, tz),
             note="The tower to the gallery, open top. Night: the square moon to the left of it, in the north."),
        dict(file="2011-05-17_18.02.40.png", stage="final_unroofed", time=9000, stand=("at", px - 19, 92, pz + 2),  # the hilltop; ("ground", px-19, pz+5) hit a grass overhang lip at y 88 you fall off
             look=(px + 5, py, pz + 4.5),
             note="cairn's sheep pen from the hill: six dyed sheep, four in the inner pen."),
        dict(file="2011-05-18_06.12.40.png", stage="half", time=23250, stand=spot, look=top,
             note="Glass roof half placed: 4 of 9 panes. (Optional: a friend standing on the dock.)"),
        dict(file="2011-05-18_06.14.33.png", stage="nearly", time=23450, stand=spot, look=top,
             note="Same spot: roof nearly done (7 of 9), the glowstone in the top. Dock empty."),
        dict(file="2011-05-18_06.16.20.png", stage="roofed", time=23650, stand=spot, look=top,
             chat=True, note="Same spot, roof complete. Chat visible (no F1): type  it put a roof on it"),
        dict(file="2011-05-20_20.30.00.png", stage="roofed", time=9500, stand=("ground", tx + 54, tz + 3),
             look=(tx + 0.5, ty + 12, tz + 0.5),
             note="The finished tower in daylight from across the bay: glass roof and lamp."),
    ]


def _player(level: dict, pos, yaw, pitch):
    p = level["Data"].value["Player"].value
    p["Pos"] = nbt.lst(nbt.DOUBLE, [nbt.Tag(nbt.DOUBLE, v) for v in (pos[0], pos[1] + 1.62, pos[2])])
    p["Rotation"] = nbt.lst(nbt.FLOAT, [nbt.Tag(nbt.FLOAT, float(yaw)), nbt.Tag(nbt.FLOAT, float(pitch))])
    p["Motion"] = nbt.lst(nbt.DOUBLE, [nbt.Tag(nbt.DOUBLE, 0.0)] * 3)
    p["FallDistance"] = nbt.Tag(nbt.FLOAT, 0.0)
    p["OnGround"] = nbt.b(1)


def prepare(pristine: str, saves: str, only: int | None = None) -> list[str]:
    out = []
    for n, s in enumerate(shots(), 1):
        if only and n != only:
            continue
        folder = Path(saves) / ("MossfieldShot%02d" % n)
        if folder.exists():
            shutil.rmtree(folder)
        shutil.copytree(pristine, folder)
        (folder / "build-manifest.json").unlink(missing_ok=True)
        stage = s["stage"]
        build2011.build(str(folder), "may15" if stage == "final_unroofed" else stage)
        inject.inject(str(folder))
        w = World(str(folder))
        kind, *where = s["stand"]
        if kind == "at":
            x, g, z = where
        else:
            x, z = where
            g = build2011.Builder(w).ground(x, z)
            if kind == "sea":         # stand on one sand block out in the water
                g = 63
                w.set(x, g, z, build2011.SAND)
        for yy in (g + 1, g + 2):
            w.set(x, yy, z, 0)
        relight_blocks(w, x, z, x, z, margin=2)
        w.save()
        lp = folder / "level.dat"
        name, root = nbt.load_file(str(lp))
        d = root["Data"].value
        d["Time"] = nbt.Tag(nbt.LONG, 24000 * 10 + s["time"])
        d["raining"], d["thundering"] = nbt.b(0), nbt.b(0)
        d["rainTime"], d["thunderTime"] = nbt.i(10_000_000), nbt.i(10_000_000)
        d["LevelName"] = nbt.st("Mossfield shot %d (%s)" % (n, s["file"][:-4]))
        yaw, pitch = _aim((x + 0.5, g + 1, z + 0.5), s["look"])
        _player(root, (x + 0.5, g + 1, z + 0.5), yaw, pitch)
        nbt.save_file(str(lp), name, root)
        flags = (" FAST graphics" if s.get("fast") else "") + (" CHAT visible" if s.get("chat") else " F1")
        out.append("shot %d  %-28s %s |%s" % (n, s["file"], s["note"], flags))
    return out


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    only = int(sys.argv[sys.argv.index("--only") + 1]) if "--only" in sys.argv else None
    for line in prepare(sys.argv[1], sys.argv[2], only):
        print(line)
