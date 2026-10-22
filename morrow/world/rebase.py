"""Put untouched terrain back under the town, keeping everything else from a played world.

    python -m morrow.world.rebase <pristine> <played> <out> [--box X0 Z0 X1 Z1]

Copies <played> (the owner's walked world: Kit's hut, the chunks the walk generated, level.dat)
to <out>, then replaces every chunk in the box that also exists in <pristine> with the pristine
chunk. Build the 2011 town on the result (build2011.py), so a rebuild never stacks on an old build.
The default box is the whole 2011 town with a margin; Kit's base is far outside it.
"""

from __future__ import annotations

import shutil
import sys
from pathlib import Path

from morrow.world.mcregion import World

TOWN_BOX = (-340, -230, -20, 300)


def rebase(pristine: str, played: str, out: str, box=TOWN_BOX) -> int:
    if Path(out).exists():
        shutil.rmtree(out)
    shutil.copytree(played, out)
    for leftover in ("build-manifest.json",):
        (Path(out) / leftover).unlink(missing_ok=True)
    src, dst = World(pristine), World(out)
    x0, z0, x1, z1 = box
    n = 0
    for cx in range(x0 >> 4, (x1 >> 4) + 1):
        for cz in range(z0 >> 4, (z1 >> 4) + 1):
            sreg = src.region_for(cx * 16, cz * 16)
            lx, lz = cx & 31, cz & 31
            if not sreg.has(lx, lz):
                continue
            dreg = dst.region_for(cx * 16, cz * 16)
            dreg.chunks[(lx, lz)] = sreg.chunk(lx, lz)
            dreg.mark(lx, lz)
            n += 1
    dst.save()
    return n


if __name__ == "__main__":
    if len(sys.argv) < 4:
        print(__doc__)
        sys.exit(2)
    box = tuple(int(v) for v in sys.argv[sys.argv.index("--box") + 1:][:4]) if "--box" in sys.argv else TOWN_BOX
    print("restored", rebase(sys.argv[1], sys.argv[2], sys.argv[3], box), "pristine chunks")
