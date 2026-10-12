"""Top-down map of a Beta world, with the canon anchors marked.

    python -m morrow.world.render <world> [out.png] [--scale 2]

Colours follow the top non-air block. Ungenerated chunks are dark. This is what we use
to lay the town onto the real 'bracken' terrain before editing morrow/canon/layout.py.
"""

from __future__ import annotations

import sys

from PIL import Image, ImageDraw

from morrow.canon import layout
from morrow.world.mcregion import World

COLOURS = {
    1: (125, 125, 125), 2: (95, 159, 53), 3: (134, 96, 67), 4: (110, 110, 110), 5: (157, 128, 79),
    8: (52, 90, 190), 9: (52, 90, 190), 12: (219, 207, 163), 13: (136, 126, 126), 17: (102, 81, 51),
    18: (55, 110, 40), 24: (216, 203, 155), 31: (95, 159, 53), 35: (230, 230, 230), 37: (240, 220, 40),
    38: (220, 40, 40), 78: (245, 250, 250), 79: (160, 190, 255), 80: (245, 250, 250), 81: (40, 120, 40),
    82: (160, 166, 179), 83: (130, 180, 90), 86: (210, 120, 20), 87: (110, 50, 50), 10: (230, 100, 20), 11: (230, 100, 20),
}


def render(world_path: str, out: str, scale: int = 2) -> str:
    w = World(world_path)
    chunks = w.generated_chunks()
    if not chunks:
        raise SystemExit("no generated chunks found in %s" % world_path)
    xs = [c[0] for c in chunks]
    zs = [c[1] for c in chunks]
    x0, z0 = min(xs) * 16, min(zs) * 16
    width, height = (max(xs) - min(xs) + 1) * 16, (max(zs) - min(zs) + 1) * 16
    img = Image.new("RGB", (width, height), (20, 20, 24))
    px = img.load()
    for cx, cz in chunks:
        lv = w.level(cx * 16, cz * 16)
        blocks, hm = lv["Blocks"].value, lv["HeightMap"].value
        for lx in range(16):
            for lz in range(16):
                y = max(min(hm[lz * 16 + lx], 127), 1)
                bid = 0
                while y > 0 and bid == 0:
                    y -= 1
                    bid = blocks[y + lz * 128 + lx * 128 * 16]
                base = COLOURS.get(bid, (150, 150, 150))
                shade = 0.75 + 0.25 * (y - 50) / 40
                px[cx * 16 + lx - x0, cz * 16 + lz - z0] = tuple(max(0, min(255, int(c * shade))) for c in base)
    img = img.resize((width * scale, height * scale), Image.NEAREST)
    d = ImageDraw.Draw(img)
    for name, (x, _y, z) in layout.ANCHORS.items():
        u, v = (x - x0) * scale, (z - z0) * scale
        if 0 <= u < img.width and 0 <= v < img.height:
            d.rectangle([u - 3, v - 3, u + 3, v + 3], outline=(255, 0, 255))
            d.text((u + 5, v - 6), name, fill=(255, 255, 255))
    d.text((4, 4), "origin x=%d z=%d  (north is up)" % (x0, z0), fill=(255, 255, 0))
    img.save(out)
    return out


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    scale = int(sys.argv[sys.argv.index("--scale") + 1]) if "--scale" in sys.argv else 2
    if not args:
        print(__doc__)
        sys.exit(2)
    print(render(args[0], args[1] if len(args) > 1 else "mossfield-map.png", scale))
