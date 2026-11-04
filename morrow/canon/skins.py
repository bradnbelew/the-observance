"""Resident skins (64x64, modern layout with the outer layer used), and lowpath's stitched one.

    python -m morrow.canon.skins

Writes morrow/v2/skins/<name>.png, a contact sheet, and morrow/v2/rework-previews/skins.png
(front and back of every figure at 10x, the way a player sees them). lowpath's skin is built from
other people's parts (people.FABRICATED["lowpath"]["skin"]): June's head, cairn's torso, Kit's
arms, finch's legs, with visible seams at every join. Players who compare skins (the forum avatar
cache, the figures) can prove it's a composite.

Each resident's look comes from the forum: finch is "steve with a red shirt lol", June made "a
lighthouse keeper one" with "a little hat", Kit wore a default skin in June's photo, Rook makes
everyone take their boots off, cairn bakes, patch plays bass and runs Mumble, Iona is staff.
Upload each PNG at mineskin.org and paste the signed texture into plugins/mrrw/config.yml.
"""

from __future__ import annotations

import hashlib
from pathlib import Path

from PIL import Image, ImageDraw

from . import people

OUT = Path(__file__).resolve().parents[2] / "morrow" / "v2" / "skins"
PREVIEW = Path(__file__).resolve().parents[2] / "morrow" / "v2" / "rework-previews"

# ---------------------------------------------------------------------------
# 64x64 layout. A box is (x, y, w, h) on the sheet; faces are named for the viewer.
# Base layer parts: head, body, right arm, left arm, right leg, left leg; the outer layer has the
# same faces offset (hat: +32 x; jacket/sleeves/pants: +16 y or their own slot).

def _faces(x, y, w, h, d):
    """Faces of a w(wide) x h(high) x d(deep) box whose unwrapped texture starts at (x, y)."""
    return {
        "top": (x + d, y, w, d), "bottom": (x + d + w, y, w, d),
        "right": (x, y + d, d, h), "front": (x + d, y + d, w, h),
        "left": (x + d + w, y + d, d, h), "back": (x + d + w + d, y + d, w, h),
    }


PARTS = {
    "head": _faces(0, 0, 8, 8, 8), "hat": _faces(32, 0, 8, 8, 8),
    "body": _faces(16, 16, 8, 12, 4), "jacket": _faces(16, 32, 8, 12, 4),
    "rarm": _faces(40, 16, 4, 12, 4), "rsleeve": _faces(40, 32, 4, 12, 4),
    "larm": _faces(32, 48, 4, 12, 4), "lsleeve": _faces(48, 48, 4, 12, 4),
    "rleg": _faces(0, 16, 4, 12, 4), "rpants": _faces(0, 32, 4, 12, 4),
    "lleg": _faces(16, 48, 4, 12, 4), "lpants": _faces(0, 48, 4, 12, 4),
}
BASE_OF = {"hat": "head", "jacket": "body", "rsleeve": "rarm", "lsleeve": "larm", "rpants": "rleg", "lpants": "lleg"}


def _shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def _grain(x, y, name, amount=10):
    h = int(hashlib.md5(("%s:%d:%d" % (name, x, y)).encode()).hexdigest()[:2], 16)
    return (h % (2 * amount + 1)) - amount


class Painter:
    """Paints one skin: fill(part, face, colour) with light per face, plus pixel details."""

    def __init__(self, name):
        self.name = name
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()

    FACE_LIGHT = {"top": 1.12, "front": 1.0, "left": 0.88, "right": 0.88, "back": 0.8, "bottom": 0.7}

    def fill(self, part, face, colour, grain=8, gradient=0.0):
        x, y, w, h = PARTS[part][face]
        k = self.FACE_LIGHT[face]
        for j in range(h):
            for i in range(w):
                g = 1.0 - gradient * (j / max(h - 1, 1))
                c = _shade(colour, k * g)
                n = _grain(x + i, y + j, self.name, grain)
                self.px[x + i, y + j] = tuple(max(0, min(255, v + n)) for v in c) + (255,)

    def fill_all(self, part, colour, **kw):
        for face in PARTS[part]:
            self.fill(part, face, colour, **kw)

    def dot(self, part, face, i, j, colour):
        x, y, w, h = PARTS[part][face]
        if 0 <= i < w and 0 <= j < h:
            self.px[x + i, y + j] = tuple(colour) + (255,)

    def rect(self, part, face, i0, j0, i1, j1, colour, grain=4):
        x, y, w, h = PARTS[part][face]
        for j in range(j0, j1):
            for i in range(i0, i1):
                if 0 <= i < w and 0 <= j < h:
                    n = _grain(x + i, y + j, self.name, grain)
                    self.px[x + i, y + j] = tuple(max(0, min(255, v + n)) for v in colour) + (255,)

    def band(self, part, j0, j1, colour, faces=("front", "left", "right", "back"), grain=4):
        for face in faces:
            w = PARTS[part][face][2]
            self.rect(part, face, 0, j0, w, j1, colour, grain)

    def clear(self, part, face):
        x, y, w, h = PARTS[part][face]
        for j in range(h):
            for i in range(w):
                self.px[x + i, y + j] = (0, 0, 0, 0)

    # ---- common pieces -----------------------------------------------------------------------

    def skin_base(self, skin):
        self.fill_all("head", skin, grain=5)
        for p in ("rarm", "larm"):
            self.fill_all(p, skin, grain=5)

    def face(self, skin, eyes, brow=None, mouth=True, eye_dx=0, glasses=None):
        white = (240, 240, 240)
        # eyes: two 2px eyes with a white and a pupil, on the front face rows 4
        for ex in (1, 5):
            self.dot("head", "front", ex + eye_dx, 4, white)
            self.dot("head", "front", ex + 1 + eye_dx, 4, eyes)
        if brow:
            for ex in (1, 2, 5, 6):
                self.dot("head", "front", ex, 3, brow)
        if mouth:
            self.rect("head", "front", 3, 6, 5, 7, _shade(skin, 0.78), grain=0)
        self.dot("head", "front", 3, 5, _shade(skin, 0.92))  # nose
        self.dot("head", "front", 4, 5, _shade(skin, 0.92))
        if glasses:
            for ex in (0, 1, 2, 5, 6, 7):
                self.dot("head", "front", ex, 5, glasses)
            self.dot("head", "front", 3, 4, glasses)
            self.dot("head", "front", 4, 4, glasses)
            for f in ("left", "right"):
                self.dot("head", f, 0, 4, glasses)
                self.dot("head", f, 1, 4, glasses)

    def hair(self, colour, fringe=2, sides=4, back=6, top=True, overlay=False):
        part = "hat" if overlay else "head"
        if top:
            self.fill(part, "top", colour, grain=6)
        self.rect(part, "front", 0, 0, 8, fringe, colour, 6)
        for f in ("left", "right"):
            self.rect(part, f, 0, 0, 8, sides, colour, 6)
        self.rect(part, "back", 0, 0, 8, back, colour, 6)

    def shirt(self, colour, collar=None, gradient=0.12):
        self.fill_all("body", colour, gradient=gradient)
        if collar:
            self.rect("body", "front", 2, 0, 6, 1, collar)
            self.rect("body", "back", 2, 0, 6, 1, collar)

    def sleeves(self, colour, length=12, part_pair=("rarm", "larm")):
        for p in part_pair:
            self.band(p, 0, length, colour, grain=6)
            self.fill(p, "top", colour)

    def pants(self, colour, shoes=None, shoe_h=2):
        for p in ("rleg", "lleg"):
            self.fill_all(p, colour, gradient=0.1)
            if shoes:
                self.band(p, 12 - shoe_h, 12, shoes, grain=3)
                self.fill(p, "bottom", _shade(shoes, 0.8))

    def belt(self, colour, buckle=None):
        self.band("body", 11, 12, colour, grain=2)
        if buckle:
            self.rect("body", "front", 3, 11, 5, 12, buckle, 0)

    def hat_cap(self, colour, brim=None):
        self.fill("hat", "top", colour, grain=5)
        for f in ("front", "left", "right", "back"):
            self.rect("hat", f, 0, 0, 8, 3, colour, 5)
        if brim:
            self.rect("hat", "front", 0, 3, 8, 4, brim, 2)

    def hood(self, colour):
        self.fill("hat", "top", colour, grain=6)
        for f in ("left", "right", "back"):
            self.rect("hat", f, 0, 0, 8, 8, colour, 6)
        self.rect("hat", "front", 0, 0, 8, 2, colour, 6)
        self.rect("hat", "front", 0, 2, 1, 8, colour, 6)
        self.rect("hat", "front", 7, 2, 8, 8, colour, 6)


# ---------------------------------------------------------------------------
# the residents

SKIN = {"pale": (236, 198, 170), "light": (224, 176, 140), "tan": (206, 150, 112), "olive": (190, 140, 100)}


def rookery(p: Painter):
    """Dark hoodie up, black jeans, the heavy brown boots, tired eyes."""
    skin = SKIN["light"]
    p.skin_base(skin)
    p.hair((40, 30, 24), fringe=3, sides=5, back=7)
    p.face(skin, (60, 80, 60), brow=(40, 30, 24))
    p.rect("head", "front", 1, 5, 3, 6, _shade(skin, 0.88), grain=0)   # the shadows under the eyes
    p.rect("head", "front", 5, 5, 7, 6, _shade(skin, 0.88), grain=0)
    hoodie = (58, 58, 64)
    p.shirt(hoodie, gradient=0.15)
    p.rect("body", "front", 1, 7, 7, 10, _shade(hoodie, 0.85))         # the pocket
    p.rect("body", "front", 3, 1, 5, 4, (200, 200, 200), 0)            # drawstrings
    p.sleeves(hoodie)
    p.hood(_shade(hoodie, 1.08))
    p.pants((34, 34, 40), shoes=(96, 64, 36), shoe_h=3)
    for leg in ("rleg", "lleg"):
        p.rect(leg, "front", 0, 9, 4, 10, (60, 40, 22), 0)              # boot tops


def finchline(p: Painter):
    """'mine is just steve with a red shirt lol': Steve's face, a red shirt, a redstone hairband."""
    skin = SKIN["tan"]
    p.skin_base(skin)
    p.hair((60, 40, 24), fringe=2, sides=4, back=6)
    p.rect("head", "front", 0, 2, 8, 3, (180, 30, 30), 0)               # hairband
    p.face(skin, (70, 60, 140), brow=(60, 40, 24))                       # Steve's purple-ish eyes
    p.rect("head", "front", 2, 6, 6, 7, (90, 60, 40), 0)                 # Steve's beard line
    red = (176, 40, 36)
    p.shirt(red, collar=(140, 30, 28))
    p.sleeves(red, length=7)                                             # rolled sleeves
    p.rect("rarm", "front", 0, 7, 4, 9, (70, 60, 50), 2)                 # grease on one sleeve
    p.pants((60, 76, 140), shoes=(90, 90, 96))
    p.belt((70, 50, 30), buckle=(200, 180, 90))


def juni_lantern(p: Painter):
    """The lighthouse keeper: yellow oilskin, a navy cap, blonde hair, a camera strap."""
    skin = SKIN["pale"]
    p.skin_base(skin)
    p.hair((222, 190, 100), fringe=2, sides=6, back=8)
    p.face(skin, (40, 100, 160), brow=(190, 160, 80))
    p.rect("head", "front", 2, 6, 6, 7, (200, 120, 110), 0)              # a smile
    coat = (232, 190, 50)
    p.shirt(coat, collar=(40, 50, 90), gradient=0.1)
    p.rect("body", "front", 3, 1, 5, 12, (180, 140, 30), 1)              # the placket
    p.rect("body", "front", 0, 2, 8, 3, (40, 40, 40), 0)                 # camera strap across the chest
    p.rect("body", "front", 5, 3, 7, 5, (40, 40, 40), 0)
    p.rect("body", "front", 5, 3, 6, 4, (170, 170, 180), 0)              # the lens
    p.sleeves(coat)
    for arm in ("rarm", "larm"):
        p.band(arm, 10, 12, (40, 50, 90))                                # cuffs
    p.hat_cap((30, 40, 80), brim=(20, 28, 60))
    p.rect("hat", "front", 3, 1, 5, 2, (220, 200, 90), 0)                # the cap badge
    p.pants((50, 70, 120), shoes=(200, 200, 200))                        # white wellies


def patchcord(p: Painter):
    """All black, headphones round the neck, a bass strap, stubble."""
    skin = SKIN["light"]
    p.skin_base(skin)
    p.hair((22, 22, 22), fringe=3, sides=5, back=7)
    p.face(skin, (50, 50, 50), brow=(22, 22, 22))
    p.rect("head", "front", 1, 6, 7, 8, _shade(skin, 0.86), grain=6)     # stubble
    black = (28, 28, 30)
    p.shirt(black, gradient=0.1)
    p.rect("body", "front", 2, 0, 3, 12, (90, 70, 50), 1)                # the bass strap
    p.sleeves(black)
    p.rect("body", "front", 0, 0, 8, 1, (60, 60, 64), 0)                 # headphone band round the neck
    p.rect("body", "front", 0, 0, 2, 3, (70, 70, 74), 0)                 # the cups
    p.rect("body", "front", 6, 0, 8, 3, (70, 70, 74), 0)
    p.pants((40, 40, 44), shoes=(20, 20, 20))


def cairn(p: Painter):
    """Baker's whites, flour on the apron, rolled sleeves, a flat cap, a beard."""
    skin = SKIN["tan"]
    p.skin_base(skin)
    p.hair((120, 84, 40), fringe=1, sides=4, back=6)
    p.face(skin, (50, 70, 120), brow=(110, 80, 40))
    p.rect("head", "front", 1, 6, 7, 8, (110, 80, 45), 5)                # beard
    p.rect("head", "front", 3, 6, 5, 7, (150, 90, 80), 0)                # mouth in the beard
    whites = (228, 226, 216)
    p.shirt((196, 120, 60), collar=(160, 95, 45))                        # orange work shirt
    p.rect("body", "front", 1, 3, 7, 12, whites, 6)                      # apron
    p.rect("body", "front", 3, 2, 5, 3, whites, 0)                       # apron strap
    for (i, j) in ((2, 5), (5, 7), (3, 9), (6, 10)):
        p.dot("body", "front", i, j, (250, 250, 250))                    # flour
    p.sleeves((196, 120, 60), length=6)
    p.hat_cap((90, 70, 50), brim=(70, 55, 40))
    p.pants((90, 80, 60), shoes=(60, 40, 30))
    p.belt((70, 50, 30))


def kitbag(p: Painter):
    """The default Steve with a green shirt and a kid's haircut (June's photo: a default skin)."""
    skin = SKIN["tan"]
    p.skin_base(skin)
    p.hair((50, 36, 24), fringe=2, sides=3, back=4)
    p.face(skin, (70, 60, 140), brow=(50, 36, 24))                       # Steve's eyes
    p.rect("head", "front", 3, 6, 5, 7, (150, 90, 80), 0)
    green = (86, 160, 86)
    p.shirt(green, gradient=0.1)
    p.rect("body", "front", 2, 3, 6, 8, (60, 120, 60), 3)                # a print on the shirt
    p.sleeves(green, length=5)                                           # short sleeves
    p.pants((60, 70, 110), shoes=(220, 60, 60))                          # red trainers
    for leg in ("rleg", "lleg"):
        p.rect(leg, "front", 0, 11, 4, 12, (240, 240, 240), 0)           # the soles


def i_bell(p: Painter):
    """Grey cardigan, glasses, a lanyard, dark hair tied back."""
    skin = SKIN["light"]
    p.skin_base(skin)
    p.hair((60, 40, 30), fringe=1, sides=3, back=8)
    p.rect("head", "back", 3, 4, 5, 8, (50, 32, 24), 3)                  # the tied-back tail
    p.face(skin, (50, 80, 70), brow=(60, 40, 30), glasses=(40, 40, 44))
    card = (126, 126, 134)
    p.shirt((200, 200, 206), collar=(180, 180, 186))                     # shirt
    p.rect("body", "front", 0, 0, 2, 12, card, 4)                        # cardigan open at the front
    p.rect("body", "front", 6, 0, 8, 12, card, 4)
    p.fill("body", "back", card, gradient=0.1)
    p.fill("body", "left", card)
    p.fill("body", "right", card)
    p.rect("body", "front", 3, 0, 4, 6, (40, 90, 160), 0)                # the lanyard
    p.rect("body", "front", 4, 0, 5, 6, (40, 90, 160), 0)
    p.rect("body", "front", 3, 6, 5, 8, (230, 230, 230), 0)              # the badge
    p.sleeves(card)
    p.pants((44, 44, 54), shoes=(30, 30, 30))


LOOKS = {"rookery": rookery, "finchline": finchline, "juni_lantern": juni_lantern, "patchcord": patchcord,
         "cairn": cairn, "kitbag": kitbag, "i_bell": i_bell}


def skin(name: str) -> Image.Image:
    p = Painter(name)
    LOOKS[name](p)
    return p.img


def lowpath() -> Image.Image:
    """June's head, cairn's torso, Kit's arms, finch's legs, with the seams showing."""
    parts = people.FABRICATED["lowpath"]["skin"]
    src = {k: skin(v) for k, v in parts.items()}
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    take = {"head": ("head", "hat"), "torso": ("body", "jacket"), "arms": ("rarm", "rsleeve", "larm", "lsleeve"),
            "legs": ("rleg", "rpants", "lleg", "lpants")}
    for part, names in take.items():
        for n in names:
            for face, (x, y, w, h) in PARTS[n].items():
                img.paste(src[part].crop((x, y, x + w, y + h)), (x, y))
    px = img.load()
    # the seams: the top row of the torso, arms and legs is one shade wrong and one pixel shifted
    for n in ("body", "rarm", "larm", "rleg", "lleg"):
        for face in ("front", "back", "left", "right"):
            x, y, w, h = PARTS[n][face]
            row = [px[x + i, y] for i in range(w)]
            for i in range(w):
                r, g, b, a = row[(i + 1) % w]
                px[x + i, y] = (min(255, r + 18), g, max(0, b - 12), a)
    # the head a shade too pale, and one eye a pixel off
    x, y, w, h = PARTS["head"]["front"]
    for j in range(h):
        for i in range(w):
            r, g, b, a = px[x + i, y + j]
            px[x + i, y + j] = (min(255, r + 14), min(255, g + 14), min(255, b + 10), a)
    px[x + 6, y + 4] = px[x + 5, y + 4]
    px[x + 5, y + 4] = px[x + 7, y + 4]
    px[x + 6, y + 5] = (40, 100, 160, 255)   # the pupil slipped a row
    return img


# ---------------------------------------------------------------------------
# previews: front and back views the way a player sees the figure

def _view(img: Image.Image, back: bool) -> Image.Image:
    f = "back" if back else "front"
    out = Image.new("RGBA", (16, 32), (0, 0, 0, 0))

    def put(part, overlay, dx, dy):
        x, y, w, h = PARTS[part][f]
        out.alpha_composite(img.crop((x, y, x + w, y + h)), (dx, dy))
        if overlay:
            x, y, w, h = PARTS[overlay][f]
            out.alpha_composite(img.crop((x, y, x + w, y + h)), (dx, dy))

    put("head", "hat", 4, 0)
    put("body", "jacket", 4, 8)
    la, ra = ("rarm", "rsleeve"), ("larm", "lsleeve")
    ll, rl = ("rleg", "rpants"), ("lleg", "lpants")
    if back:
        la, ra, ll, rl = ra, la, rl, ll
    put(la[0], la[1], 0, 8)
    put(ra[0], ra[1], 12, 8)
    put(ll[0], ll[1], 4, 20)
    put(rl[0], rl[1], 8, 20)
    return out


def preview(skins: dict[str, Image.Image], scale=10) -> Image.Image:
    names = list(skins)
    sheet = Image.new("RGBA", (len(names) * 40 * scale // 10 * 10 // 10 * 10 // 10 * 10, 0), (255, 255, 255, 255))
    cell = 40 * scale
    sheet = Image.new("RGBA", (len(names) * cell, 36 * scale + 14), (248, 248, 244, 255))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        for k, back in enumerate((False, True)):
            v = _view(skins[n], back).resize((16 * scale, 32 * scale), Image.NEAREST)
            sheet.alpha_composite(v, (i * cell + 2 * scale + k * 19 * scale, 14))
        d.text((i * cell + 2 * scale, 2), n, fill=(30, 30, 30))
    return sheet


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    PREVIEW.mkdir(parents=True, exist_ok=True)
    skins = {n: skin(n) for n in LOOKS}
    skins["lowpath"] = lowpath()
    for n, im in skins.items():
        im.save(OUT / f"{n}.png")
    sheet = Image.new("RGBA", (64 * len(skins), 64), (255, 255, 255, 255))
    for i, im in enumerate(skins.values()):
        sheet.alpha_composite(im, (64 * i, 0))
    sheet.save(OUT / "_contact-sheet.png")
    preview(skins).save(PREVIEW / "skins.png")
    print("skins written:", OUT, "preview:", PREVIEW / "skins.png")


if __name__ == "__main__":
    main()
