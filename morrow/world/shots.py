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
    """Camera positions are derived from the layout anchors so they follow any re-layout."""
    tx, ty, tz = A["june_tower"]
    hx, hy, hz = A["june_house"]
    dx, dy, dz = A["june_old_dock"]
    sx, sy, sz = A["spawn"]
    rx, ry, rz = A["rules_signs"]
    px, py, pz = A["sheep_pen"]
    top = (tx + 0.5, ty + 21, tz + 0.5)
    return [
        dict(file="2011-03-02_19.20.11.png", stage="march", time=7000, cam=(dx - 2, None, hz - 5),
             look=(hx - 3, hy + 1, (hz + tz) / 2), fast=True,
             note="June's first build: house on the point, tower base two blocks high."),
        dict(file="2011-03-30_20.44.05.png", stage="april", time=12000, cam=(dx - 4, None, dz - 4),
             look=(dx + 6, dy, dz + 1), fast=True,
             note="The wooden dock at sunset, with the shoreline in frame."),
        dict(file="2011-05-17_18.02.40.png", stage="final_unroofed", time=6500, cam=(px + 15, "hill", pz + 11),
             look=(px + 5, py, pz + 4.5),
             note="cairn's sheep pen from the hill: six dyed sheep, four in the inner pen."),
        dict(file="2011-05-14_21.40.02.png", stage="may15", time=14500, cam=(sx + 1, None, sz + 7),
             look=(rx + 2, ry + 1, rz), group=True,
             note="Spawn at night around the cake; the three rules signs readable behind (b4 visible)."),
        dict(file="2011-05-15_22.10.30.png", stage="may15", time=13300, cam=(tx, "sea", tz + 17),
             look=(tx + 0.5, ty + 14, tz - 40),
             note="The tower to the gallery, open top. Look north: the square moon sits low behind it."),
        dict(file="2011-05-18_06.12.40.png", stage="half", time=23250, cam=(tx + 5, None, tz - 29), look=top,
             note="Glass roof half placed. (Optional: a friend standing on the dock.)"),
        dict(file="2011-05-18_06.14.33.png", stage="nearly", time=23450, cam=(tx + 5, None, tz - 29), look=top,
             note="Same spot: roof nearly done, glowstone lamp in the top. Dock empty."),
        dict(file="2011-05-18_06.16.20.png", stage="roofed", time=23650, cam=(tx + 5, None, tz - 29), look=top,
             chat=True, note="Same spot, roof complete. Chat visible (no F1): type  it put a roof on it"),
        dict(file="2011-05-20_20.30.00.png", stage="roofed", time=9500, cam=(dx + 5, None, dz),
             look=(tx + 0.5, ty + 16, tz + 0.5),
             note="The finished tower in daylight: glass roof and lamp."),
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
        cx, cy, cz = s["cam"]
        x, z = int(math.floor(cx)), int(math.floor(cz))
        g = build2011.Builder(w).ground(x, z)
        if cy == "sea":           # stand on one sand block out in the water
            w.set(x, 63, z, build2011.SAND)
            g = 63
        elif cy == "hill":        # a step up for a view over the fence
            w.set(x, g + 1, z, build2011.DIRT)
            g += 1
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
