#!/usr/bin/env python3
"""Render and verify the nine exact V5 Minecraft map-clue textures.

The PNG stored for an item is pre-rotated against its required ItemFrame rotation. When the
player reaches the authored rotation, the on-wall view is the canonical drawing assembled here.
This keeps the visual clue and the executable predicate in one deterministic contract.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "arc" / "v5" / "map-art-manifest.json"
OUTPUT = ROOT / "arc" / "v5" / "map-art"
SHEET = ROOT / "design" / "visuals" / "v5-map-clue-sheet.png"

INK = "#16191a"
PAPER = "#d7c9a8"
PALE = "#eee4c8"
RUST = "#8a3f2d"
BRASS = "#a7833e"
WATER = "#456c72"
REED = "#526245"
BLUE = "#375d78"
RED = "#8d2d2b"
GREY = "#666962"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    names = ["DejaVuSansMono-Bold.ttf" if bold else "DejaVuSansMono.ttf", "arial.ttf"]
    for name in names:
        try:
            return ImageFont.truetype(name, size=size)
        except OSError:
            pass
    return ImageFont.load_default()


F7 = font(7)
F8 = font(8)
F9 = font(9)
F10 = font(10, True)
F13 = font(13, True)
F18 = font(18, True)


def canvas(title: str, subtitle: str = "") -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (128, 128), PAPER)
    draw = ImageDraw.Draw(image)
    draw.rectangle((2, 2, 125, 125), outline=INK, width=2)
    draw.rectangle((6, 6, 121, 121), outline=BRASS, width=1)
    draw.text((9, 8), title, fill=INK, font=F10)
    if subtitle:
        draw.text((9, 20), subtitle, fill=GREY, font=F7)
    return image, draw


def compass(draw: ImageDraw.ImageDraw, x: int = 111, y: int = 17) -> None:
    draw.line((x, y + 10, x, y - 2), fill=INK, width=2)
    draw.polygon(((x, y - 5), (x - 3, y + 1), (x + 3, y + 1)), fill=RUST)
    draw.text((x - 3, y + 11), "N", fill=INK, font=F7)


def edge_register(draw: ImageDraw.ImageDraw, side: str, y: int) -> None:
    if side == "right":
        draw.arc((116, y - 6, 128, y + 6), 90, 270, fill=RUST, width=3)
        draw.line((121, y, 127, y), fill=RUST, width=2)
    else:
        draw.arc((-1, y - 6, 11, y + 6), 270, 90, fill=RUST, width=3)
        draw.line((0, y, 6, y), fill=RUST, width=2)


def route_edge(draw: ImageDraw.ImageDraw, left_y: int, right_y: int, color: str = RED) -> None:
    draw.line((0, left_y, 127, right_y), fill=color, width=3)
    draw.ellipse((58, (left_y + right_y) // 2 - 4, 66, (left_y + right_y) // 2 + 4), fill=PALE, outline=color)


def a05_grille() -> Image.Image:
    image, draw = canvas("CARDAN GRILLE", "drain register / read cutouts")
    compass(draw)
    for x in range(16, 75, 12):
        draw.line((x, 32, x, 113), fill=BRASS, width=2)
    for y in range(34, 114, 11):
        draw.line((12, y, 78, y), fill=BRASS, width=1)
    rows = [(38, "SERVICE", "18"), (57, "TICKET", "91"), (76, "LOCKER", "1"), (95, "ROUTE", "AR")]
    for y, label, value in rows:
        draw.rectangle((82, y - 4, 126, y + 10), fill=PALE, outline=INK)
        draw.text((84, y - 3), label, fill=GREY, font=F7)
        draw.text((111, y - 3), value, fill=INK, font=F9)
    edge_register(draw, "right", 113)
    draw.text((12, 115), "ALIGN DRAIN NOTCH", fill=RUST, font=F7)
    return image


def a05_route() -> Image.Image:
    image, draw = canvas("ROUTE CORNER", "rotate north / join the register")
    compass(draw)
    rows = [(38, "42"), (57, "37"), (76, "3"), (95, "CHIVE")]
    for y, value in rows:
        draw.rectangle((2, y - 4, 50, y + 10), fill=PALE, outline=INK)
        draw.text((7, y - 3), value, fill=INK, font=F10)
        draw.line((51, y + 3, 112, y + 3), fill=RUST, width=2)
    edge_register(draw, "left", 113)
    draw.polygon(((70, 111), (102, 105), (112, 115), (75, 119)), outline=INK, fill="#b8aa89")
    draw.text((70, 97), "CAMP", fill=GREY, font=F8)
    return image


def ks_school() -> Image.Image:
    image, draw = canvas("SHORE COPY I", "fixed landmark: school roof")
    compass(draw)
    draw.polygon(((26, 56), (52, 35), (79, 56)), fill=RUST, outline=INK)
    draw.rectangle((32, 56, 73, 83), fill=PALE, outline=INK, width=2)
    draw.rectangle((48, 65, 57, 83), fill=BLUE, outline=INK)
    draw.text((29, 88), "SCHOOL", fill=INK, font=F9)
    draw.line((12, 103, 127, 91), fill=WATER, width=4)
    draw.line((12, 108, 127, 96), fill=BLUE, width=1)
    draw.ellipse((103, 86, 111, 94), fill=RUST)
    draw.text((82, 105), "north of reeds", fill=GREY, font=F7)
    return image


def ks_reeds() -> Image.Image:
    image, draw = canvas("SHORE COPY II", "long bed / copied from the back")
    compass(draw)
    draw.line((0, 91, 127, 78), fill=WATER, width=4)
    draw.line((0, 96, 127, 83), fill=BLUE, width=1)
    for x in range(18, 111, 9):
        base = 103 - (x % 18) // 3
        draw.line((x, base, x + (4 if x % 2 else -3), 52), fill=REED, width=2)
        draw.line((x, 72, x + 7, 65), fill=REED, width=1)
    draw.rectangle((42, 41, 88, 55), fill=PAPER, outline=INK)
    draw.text((46, 44), "FOLDED REEDS", fill=INK, font=F7)
    draw.line((106, 73, 127, 66), fill=RED, width=3)
    draw.text((70, 111), "transfer continues >", fill=RUST, font=F7)
    return image


def ks_intake() -> Image.Image:
    image, draw = canvas("INTAKE TRANSFER", "waterline registration")
    compass(draw)
    draw.line((0, 66, 54, 54), fill=RED, width=3)
    draw.line((54, 54, 54, 102), fill=RED, width=3)
    draw.line((54, 102, 112, 102), fill=RED, width=3)
    draw.polygon(((112, 102), (102, 97), (102, 107)), fill=RED)
    draw.rectangle((68, 43, 112, 85), fill=PALE, outline=INK, width=2)
    draw.text((75, 48), "INTAKE", fill=GREY, font=F9)
    draw.text((83, 60), "A", fill=RUST, font=F18)
    draw.text((60, 111), "behind folded reeds", fill=INK, font=F7)
    return image


def bi_village() -> Image.Image:
    image, draw = canvas("VILLAGE LOAD", "surface pressure copy")
    compass(draw)
    draw.rectangle((15, 49, 45, 78), outline=INK, fill=PALE)
    draw.polygon(((12, 49), (30, 33), (49, 49)), fill=RUST, outline=INK)
    draw.text((12, 83), "SCHOOL", fill=GREY, font=F8)
    draw.ellipse((63, 55, 82, 74), outline=BLUE, width=3)
    draw.text((57, 78), "WELL", fill=GREY, font=F8)
    draw.line((72, 65, 127, 65), fill=RED, width=4)
    draw.text((55, 98), "HEAT SHAFT >", fill=RUST, font=F8)
    return image


def bi_hold() -> Image.Image:
    image, draw = canvas("HOLD PRESSURE", "copied around the well")
    compass(draw)
    draw.line((0, 65, 52, 65), fill=RED, width=4)
    draw.rectangle((47, 42, 83, 90), outline=INK, width=3)
    draw.line((65, 42, 65, 90), fill=BRASS, width=2)
    draw.text((50, 95), "SHAFT", fill=GREY, font=F8)
    draw.line((83, 65, 127, 65), fill=RED, width=4)
    draw.polygon(((123, 65), (113, 59), (113, 71)), fill=RED)
    draw.text((12, 108), "LOAD DIVERTED", fill=INK, font=F9)
    return image


def bi_trace() -> Image.Image:
    image, draw = canvas("LOAD TRACE", "where the red line ends")
    compass(draw)
    draw.line((0, 65, 46, 65), fill=RED, width=4)
    draw.line((46, 65, 46, 93), fill=RED, width=4)
    draw.line((46, 93, 91, 93), fill=RED, width=4)
    draw.polygon(((91, 93), (81, 87), (81, 99)), fill=RED)
    draw.rectangle((72, 43, 113, 84), outline=INK, fill=PALE, width=2)
    draw.text((78, 49), "THIRD", fill=GREY, font=F10)
    draw.text((84, 64), "BAY", fill=RUST, font=F13)
    draw.text((11, 108), "heat shaft -> third bay", fill=INK, font=F7)
    return image


def ar04_intake() -> Image.Image:
    image, draw = canvas("BACKWATER INDEX", "read transfer from the back")
    compass(draw)
    draw.ellipse((27, 34, 101, 108), outline=WATER, width=4)
    draw.line((64, 36, 64, 106), fill=BRASS, width=1)
    draw.line((29, 71, 99, 71), fill=BRASS, width=1)
    draw.text((57, 44), "A", fill=GREY, font=F10)
    draw.text((84, 66), "C", fill=GREY, font=F10)
    draw.text((57, 91), "D", fill=GREY, font=F10)
    draw.text((35, 66), "B", fill=GREY, font=F10)
    draw.rectangle((49, 56, 79, 86), fill=PALE, outline=RUST, width=2)
    draw.text((58, 58), "E", fill=RUST, font=F18)
    draw.text((20, 112), "INTAKE A / EXPOSED LETTER", fill=INK, font=F7)
    return image


CANONICALS = {
    "a05_cardan_grille": a05_grille,
    "a05_route_corner": a05_route,
    "ks02_school_roof": ks_school,
    "ks02_long_reed_bed": ks_reeds,
    "ks02_intake_transfer": ks_intake,
    "bi04_village_pressure": bi_village,
    "bi04_hold_pressure": bi_hold,
    "bi04_load_trace": bi_trace,
    "ar04_intake_a": ar04_intake,
}


def stored_image(canonical: Image.Image, frame_rotation: int) -> Image.Image:
    # Item-frame rotations are clockwise in 45-degree steps. These clues intentionally use only
    # quarter turns, allowing lossless pre-rotation of the stored map pixels.
    inverse = frame_rotation % 8
    if inverse == 0:
        return canonical.copy()
    if inverse == 2:
        return canonical.transpose(Image.Transpose.ROTATE_90)
    if inverse == 4:
        return canonical.transpose(Image.Transpose.ROTATE_180)
    if inverse == 6:
        return canonical.transpose(Image.Transpose.ROTATE_270)
    raise ValueError(f"map art uses unsupported non-quarter frame rotation {frame_rotation}")


def png_bytes(image: Image.Image) -> bytes:
    stream = io.BytesIO()
    image.save(stream, format="PNG", optimize=False, compress_level=9)
    return stream.getvalue()


def render_sheet(canonical: dict[str, Image.Image]) -> bytes:
    sheet = Image.new("RGB", (432, 592), "#0d1113")
    draw = ImageDraw.Draw(sheet)
    draw.text((16, 12), "THE OBSERVANCE V5 / CANONICAL MAP-CLUE VIEWS", fill="#e7ddc3", font=font(15, True))
    draw.text((16, 35), "Stored maps are inverse-rotated; these are the views at the solved frame rotations.", fill="#9da19b", font=F8)
    groups = [
        ("A05 / CARDAN REGISTER", ["a05_cardan_grille", "a05_route_corner"]),
        ("KS02 / SHORELINE", ["ks02_school_roof", "ks02_long_reed_bed", "ks02_intake_transfer"]),
        ("BI04 / REFLECTED LOAD", ["bi04_village_pressure", "bi04_hold_pressure", "bi04_load_trace"]),
        ("AR04 / BACKWATER INDEX", ["ar04_intake_a"]),
    ]
    y = 58
    for label, ids in groups:
        draw.text((16, y), label, fill="#c49c54", font=F10)
        y += 17
        for index, art_id in enumerate(ids):
            x = 16 + index * 136
            sheet.paste(canonical[art_id], (x, y))
        y += 139
    return png_bytes(sheet)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true", help="write deterministic PNG outputs")
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    entries = manifest.get("maps", [])
    if {entry.get("id") for entry in entries} != set(CANONICALS) or len(entries) != 9:
        raise SystemExit("map-art manifest must contain exactly the nine renderer IDs")
    canonical = {art_id: factory() for art_id, factory in CANONICALS.items()}
    failures: list[str] = []
    if args.write:
        OUTPUT.mkdir(parents=True, exist_ok=True)
        SHEET.parent.mkdir(parents=True, exist_ok=True)
    for entry in entries:
        art_id = entry["id"]
        data = png_bytes(stored_image(canonical[art_id], int(entry["requiredFrameRotation"])))
        digest = hashlib.sha256(data).hexdigest()
        target = ROOT / entry["file"]
        if args.write:
            target.write_bytes(data)
        if not target.is_file() or target.read_bytes() != data:
            failures.append(f"{art_id}: PNG missing or stale; run --write")
        expected = str(entry.get("sha256", ""))
        if expected and expected != digest:
            failures.append(f"{art_id}: manifest sha256 {expected} != rendered {digest}")
        print(f"{art_id} {digest}")
    sheet_data = render_sheet(canonical)
    if args.write:
        SHEET.write_bytes(sheet_data)
    if not SHEET.is_file() or SHEET.read_bytes() != sheet_data:
        failures.append("canonical clue sheet is missing or stale")
    if failures:
        print("V5 MAP ART: FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print("V5 MAP ART: PASS (9 exact 128x128 maps + canonical solved-view sheet)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
