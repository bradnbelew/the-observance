"""Resident skins (64x64), and lowpath's stitched one.

    python -m morrow.canon.skins

Writes morrow/v2/skins/<name>.png. lowpath's skin is built from other people's parts
(people.FABRICATED["lowpath"]["skin"]): June's head, cairn's torso, Kit's arms, finch's legs.
Players who compare skins (the forum avatar cache, the figures) can prove it's a composite.
Upload each PNG at mineskin.org and paste the signed texture into plugins/mrrw/config.yml.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

from . import people

OUT = Path(__file__).resolve().parents[2] / "morrow" / "v2" / "skins"

LOOKS = {  # skin, hair, eyes, shirt, sleeve, pants, shoes
    "rookery": ((224, 172, 136), (40, 30, 24), (60, 90, 60), (70, 70, 74), (70, 70, 74), (40, 46, 70), (30, 30, 30)),
    "cairn": ((206, 150, 112), (120, 84, 40), (50, 70, 120), (196, 120, 60), (230, 230, 220), (90, 80, 60), (60, 40, 30)),
    "finchline": ((240, 196, 160), (200, 80, 40), (40, 110, 60), (60, 120, 180), (60, 120, 180), (50, 50, 60), (120, 40, 40)),
    "juni_lantern": ((236, 190, 150), (230, 200, 90), (40, 100, 160), (240, 210, 70), (240, 210, 70), (70, 90, 140), (200, 200, 200)),
    "patchcord": ((180, 130, 100), (20, 20, 20), (40, 40, 40), (30, 30, 30), (30, 30, 30), (60, 60, 60), (20, 20, 20)),
    "kitbag": ((236, 190, 156), (110, 70, 40), (60, 120, 60), (80, 160, 80), (230, 230, 230), (60, 70, 100), (220, 60, 60)),
    "i_bell": ((214, 160, 128), (60, 40, 30), (50, 80, 70), (120, 120, 130), (120, 120, 130), (40, 40, 50), (30, 30, 30)),
}


def _rect(d: ImageDraw.ImageDraw, x0, y0, x1, y1, c):
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=c + (255,))


def _head(d, look):
    skin, hair, eyes = look[0], look[1], look[2]
    _rect(d, 0, 0, 32, 16, skin)
    _rect(d, 8, 0, 16, 8, hair)            # top
    for x0 in (0, 8, 16, 24):               # hairline round the sides/front/back
        _rect(d, x0, 8, x0 + 8, 10, hair)
    _rect(d, 24, 8, 32, 14, hair)          # back of the head
    _rect(d, 10, 12, 12, 13, eyes)         # face: eyes
    _rect(d, 14, 12, 16, 13, eyes)
    _rect(d, 11, 14, 15, 15, tuple(max(0, c - 40) for c in skin))


def _torso(d, look):
    _rect(d, 16, 16, 40, 32, look[3])


def _arms(d, look):
    skin, sleeve = look[0], look[4]
    _rect(d, 40, 16, 56, 32, skin)         # right arm
    _rect(d, 40, 16, 56, 24, sleeve)
    _rect(d, 32, 48, 48, 64, skin)         # left arm
    _rect(d, 32, 48, 48, 56, sleeve)


def _legs(d, look):
    pants, shoes = look[5], look[6]
    _rect(d, 0, 16, 16, 32, pants)         # right leg
    _rect(d, 0, 29, 16, 32, shoes)
    _rect(d, 16, 48, 32, 64, pants)        # left leg
    _rect(d, 16, 61, 32, 64, shoes)


def skin(parts: dict[str, str]) -> Image.Image:
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    _head(d, LOOKS[parts["head"]])
    _torso(d, LOOKS[parts["torso"]])
    _arms(d, LOOKS[parts["arms"]])
    _legs(d, LOOKS[parts["legs"]])
    return img


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for name in LOOKS:
        skin({"head": name, "torso": name, "arms": name, "legs": name}).save(OUT / f"{name}.png")
    skin(people.FABRICATED["lowpath"]["skin"]).save(OUT / "lowpath.png")
    sheet = Image.new("RGBA", (64 * 8, 64), (255, 255, 255, 255))
    for i, name in enumerate(list(LOOKS) + ["lowpath"]):
        sheet.alpha_composite(Image.open(OUT / f"{name}.png"), (64 * i, 0))
    sheet.save(OUT / "_contact-sheet.png")
    print("skins written:", OUT)


if __name__ == "__main__":
    main()
