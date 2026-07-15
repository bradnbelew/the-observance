#!/usr/bin/env python3
"""Build the playable Copperline service-annex prologue world.

The input is the known-good single-player archive first shipped in commit
3285f19. Its level.dat files and archive layout are preserved; only the
self-building datapack is replaced. The output is deterministic so its SHA-1
receipt can be checked into the private dashboard content bundle.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import tempfile
import zipfile
from pathlib import Path


UTF8 = "utf-8"
ZIP_TIMESTAMP = (2026, 7, 12, 0, 0, 0)
WORLD_ROOT = Path("the-hold")
PACK_ROOT = Path("datapacks") / "the_hold"


def write_text(path: Path, lines: list[str] | str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = lines if isinstance(lines, str) else "\n".join(lines) + "\n"
    path.write_text(text, encoding=UTF8, newline="\n")


def shell(x1: int, z1: int, x2: int, z2: int) -> list[str]:
    """Return a closed, low-volume utility-room shell at the proven world height."""
    return [
        f"fill {x1 + 1} 239 {z1 + 1} {x2 - 1} 249 {z2 - 1} minecraft:air",
        f"fill {x1} 238 {z1} {x2} 238 {z2} minecraft:smooth_stone",
        f"fill {x1} 250 {z1} {x2} 250 {z2} minecraft:smooth_stone",
        f"fill {x1} 239 {z1} {x1} 249 {z2} minecraft:stone_bricks",
        f"fill {x2} 239 {z1} {x2} 249 {z2} minecraft:stone_bricks",
        f"fill {x1 + 1} 239 {z1} {x2 - 1} 249 {z1} minecraft:stone_bricks",
        f"fill {x1 + 1} 239 {z2} {x2 - 1} 249 {z2} minecraft:stone_bricks",
    ]


def opening_x(x: int) -> str:
    return f"fill {x} 240 -3 {x} 246 3 minecraft:air"


def book_component(title: str, author: str, pages: list[str]) -> str:
    # 1.21.11 pages are SNBT text compounds. Quoted JSON renders literally.
    page_snbt = ",".join(
        "{text:" + json.dumps(page, ensure_ascii=False) + ',color:"black"}'
        for page in pages
    )
    return (
        "minecraft:written_book_content={title:"
        + json.dumps(title)
        + ",author:"
        + json.dumps(author)
        + ",pages:["
        + page_snbt
        + "]}"
    )


def lectern(
    x: int,
    y: int,
    z: int,
    facing: str,
    title: str,
    author: str,
    pages: list[str],
) -> list[str]:
    component = book_component(title, author, pages)
    stored_component = component.replace(
        "minecraft:written_book_content=", '"minecraft:written_book_content":', 1
    )
    return [
        f"setblock {x} {y} {z} minecraft:lectern[facing={facing},has_book=true]",
        f'data merge block {x} {y} {z} {{Book:{{id:"minecraft:written_book",count:1,components:{{{stored_component}}}}},Page:0}}',
    ]


def wall_sign(
    x: int,
    y: int,
    z: int,
    facing: str,
    lines: list[str],
    color: str = "dark_gray",
) -> str:
    padded = (lines + [""] * 4)[:4]
    messages = ",".join(
        "{text:" + json.dumps(line) + ",color:" + json.dumps(color) + "}"
        for line in padded
    )
    return (
        f"setblock {x} {y} {z} minecraft:dark_oak_wall_sign[facing={facing}]"
        + "{front_text:{messages:["
        + messages
        + "]}}"
    )


def add_ceiling_lights(commands: list[str], positions: list[tuple[int, int]]) -> None:
    for x, z in positions:
        commands += [
            f"setblock {x} 249 {z} minecraft:iron_chain",
            f"setblock {x} 248 {z} minecraft:lantern[hanging=true]",
            f"setblock {x} 247 {z} minecraft:light[level=11]",
        ]


def add_wall_bays(commands: list[str], centers: list[int]) -> None:
    for x in centers:
        commands += [
            f"fill {x - 2} 240 -13 {x + 2} 243 -13 minecraft:bookshelf",
            f"fill {x - 2} 244 -13 {x + 2} 244 -13 minecraft:dark_oak_slab[type=top]",
        ]


def make_datapack(root: Path) -> None:
    function_root = root / "data" / "the_hold" / "function"
    minecraft_tags = root / "data" / "minecraft" / "tags" / "function"

    pack_meta = {
        "pack": {
            "min_format": 94,
            "max_format": 94,
            "description": "Copperline Hosting - North Service Annex recovery copy",
        }
    }
    write_text(root / "pack.mcmeta", json.dumps(pack_meta, indent=2) + "\n")
    write_text(
        minecraft_tags / "load.json",
        json.dumps({"values": ["the_hold:load"]}, indent=2) + "\n",
    )
    write_text(
        minecraft_tags / "tick.json",
        json.dumps({"values": ["the_hold:tick"]}, indent=2) + "\n",
    )

    # These boot commands intentionally retain the mechanics of the played
    # 3285f19 archive. The target 1.21.11 build recognizes the newer gamerules.
    write_text(
        function_root / "load.mcfunction",
        [
            "gamerule advance_time false",
            "gamerule advance_weather false",
            "gamerule spawn_mobs false",
            "gamerule spawn_patrols false",
            "gamerule spawn_wandering_traders false",
            "gamerule spawn_phantoms false",
            "gamerule keep_inventory true",
            "gamerule immediate_respawn true",
            "gamerule send_command_feedback false",
            "gamerule command_block_output false",
            "difficulty peaceful",
            "time set midnight",
            "weather clear",
            "worldborder center 39 0",
            "worldborder set 224",
            "function the_hold:build/all",
            "tag @a remove hold_started",
        ],
    )

    write_text(
        function_root / "spawn.mcfunction",
        [
            "clear @s",
            "effect clear @s",
            "gamemode adventure @s",
            "tp @s -12.5 240 0.5 -90 0",
            "spawnpoint @s -12 240 0",
            "tag @s add hold_started",
            "title @s times 20 70 30",
            'title @s subtitle {text:"North service annex / closed June 2012",color:"dark_gray"}',
            'title @s title {text:"COPPERLINE HOSTING",color:"gray"}',
        ],
    )

    write_text(
        function_root / "return.mcfunction",
        [
            "gamemode adventure @s",
            "tp @s -12.5 240 0.5 -90 0",
            'title @s actionbar {text:"The exterior doors are sealed.",color:"dark_gray"}',
        ],
    )

    write_text(
        function_root / "tick.mcfunction",
        [
            "execute as @a[tag=!hold_started] run function the_hold:spawn",
            "execute as @a[tag=hold_started] unless entity @s[x=-19,y=236,z=-16,dx=116,dy=18,dz=32] run function the_hold:return",
            "gamemode adventure @a[tag=hold_started,gamemode=!adventure]",
            "effect give @a[tag=hold_started] minecraft:saturation 2 0 true",
        ],
    )

    write_text(
        function_root / "build" / "all.mcfunction",
        [
            "forceload add -32 -32 112 32",
            "function the_hold:build/receiving",
            "function the_hold:build/records",
            "function the_hold:build/dispatch",
            "function the_hold:build/passages",
            "forceload remove all",
        ],
    )

    # Room 1: an ordinary closed service office. Both books contribute facts
    # needed at dispatch, and cabinet returns A/B remain in place for revisits.
    receiving = shell(-18, -14, 14, 14)
    receiving += [
        "fill -17 239 -13 13 239 13 minecraft:polished_andesite",
        "fill -17 239 -2 13 239 2 minecraft:smooth_stone",
        opening_x(14),
        "fill -13 240 -11 -5 240 -8 minecraft:dark_oak_slab[type=top]",
        "fill 0 240 -11 8 240 -8 minecraft:dark_oak_slab[type=top]",
        "fill -14 240 7 -4 243 11 minecraft:oxidized_cut_copper hollow",
        "fill 3 240 7 11 243 11 minecraft:weathered_cut_copper hollow",
        "setblock -10 240 11 minecraft:barrel[facing=north]",
        "setblock 7 240 11 minecraft:barrel[facing=north]",
        "setblock -15 240 -11 minecraft:chest[facing=south]",
        "setblock 11 240 -11 minecraft:chest[facing=south]",
        "setblock -16 240 12 minecraft:cobweb",
        "setblock 12 240 -12 minecraft:cobweb",
        wall_sign(0, 245, -13, "south", ["RECEIVING", "NORTH ANNEX", "CLOSED 18 JUN 12"]),
        wall_sign(-10, 242, 13, "north", ["CABINET A", "INTAKE TEST", "RETURN 2", "14 JUN 2012"]),
        wall_sign(7, 242, 13, "north", ["CABINET B", "CARRIER TEST", "RETURN 5", "15 JUN 2012"]),
    ]
    receiving += lectern(
        -9,
        240,
        -7,
        "south",
        "Closure Notice",
        "Copperline Hosting",
        [
            "Copperline Hosting\nNorth Service Annex\n\nClosure notice\n18 June 2012",
            "Public routing remains under the commercial DNS class until the last managed account is moved.",
            "Keep the cabinet returns. They are the only verified service figures in this copy.",
        ],
    )
    receiving += lectern(
        4,
        240,
        -7,
        "south",
        "Shift Handover",
        "Ellis Ward",
        [
            "Mara,\n\nI left the four route tags inside the bay registers. The index card is still in the center tray.",
            "Use the card exactly as filed. Do not rewrite the tags as one host name on paper.",
            "The service number is still the cabinet returns in A-E order. Dispatch has the joining rule.\n\n- Ellis",
        ],
    )
    add_ceiling_lights(receiving, [(-10, -3), (4, -3), (-10, 6), (4, 6)])
    write_text(function_root / "build" / "receiving.mcfunction", receiving)

    # Room 2: four readable registers and one bare index card. Each register
    # has exactly six short, non-wrapping lines on its sole page.
    records = shell(20, -14, 58, 14)
    records += [
        "fill 21 239 -13 57 239 13 minecraft:polished_andesite",
        "fill 21 239 -2 57 239 2 minecraft:smooth_stone",
        opening_x(20),
        opening_x(58),
        "fill 27 240 5 51 240 9 minecraft:dark_oak_slab[type=top]",
        "setblock 39 240 11 minecraft:barrel[facing=north]",
        "fill 49 240 8 56 243 11 minecraft:exposed_cut_copper hollow",
        "setblock 53 240 11 minecraft:barrel[facing=north]",
        wall_sign(39, 245, -13, "south", ["SERVICE RECORDS", "BAYS 1-4", "FILE IN PLACE"]),
        wall_sign(53, 242, 13, "north", ["CABINET C", "MIRROR TEST", "RETURN 5", "16 JUN 2012"]),
    ]
    bay_centers = [25, 34, 43, 52]
    add_wall_bays(records, bay_centers)
    for number, x in enumerate(bay_centers, 1):
        records.append(wall_sign(x, 245, -13, "south", [str(number)]))

    register_lines = [
        [
            "Intake fan clear",
            "Rack seals intact",
            "No packet loss",
            "Relay tag OI",
            "Meter glass clean",
            "Return lead filed",
        ],
        [
            "North trunk clear",
            "Spare fuse boxed",
            "Cabinet lock reset",
            "Carrier test clean",
            "Fault moved to Z",
            "Copy was signed",
        ],
        [
            "West fan checked",
            "Dust screen changed",
            "Oil cup replaced",
            "Bearing stayed cool",
            "Route tag copied",
            "Socket SN failed",
        ],
        [
            "Patch lead fixed",
            "KER line stable",
            "Face plate clean",
            "Old key returned",
            "Night test passed",
            "Cover bolts fitted",
        ],
    ]
    for number, (x, lines) in enumerate(zip(bay_centers, register_lines), 1):
        records += lectern(
            x,
            240,
            -9,
            "south",
            f"Service Register {number}",
            "Copperline Hosting",
            ["\n".join(lines)],
        )

    records += lectern(
        39,
        241,
        7,
        "north",
        "Index Card",
        "M. Vale",
        ["3-6-2\n\n1-4-3\n\n4-2-1\n\n2-5-4"],
    )
    add_ceiling_lights(records, [(25, -3), (34, -3), (43, -3), (52, -3), (31, 7), (47, 7)])
    write_text(function_root / "build" / "records.mcfunction", records)

    # Room 3: the joining procedure and final two cabinet returns. There is no
    # input device or one-way gate; players can return to every earlier fact.
    dispatch = shell(64, -14, 96, 14)
    dispatch += [
        "fill 65 239 -13 95 239 13 minecraft:polished_andesite",
        "fill 65 239 -2 95 239 2 minecraft:smooth_stone",
        opening_x(64),
        "fill 71 240 -11 89 240 -8 minecraft:dark_oak_slab[type=top]",
        "fill 68 240 7 76 243 11 minecraft:weathered_cut_copper hollow",
        "fill 84 240 7 92 243 11 minecraft:oxidized_cut_copper hollow",
        "setblock 72 240 11 minecraft:barrel[facing=north]",
        "setblock 88 240 11 minecraft:barrel[facing=north]",
        "fill 73 240 -13 87 243 -13 minecraft:iron_block",
        "fill 75 241 -13 85 242 -13 minecraft:black_stained_glass",
        "setblock 94 240 -12 minecraft:cobweb",
        wall_sign(80, 245, -13, "south", ["DISPATCH", "REMOTE SERVICE", "ACCOUNT 1842", "RETIRED"]),
        wall_sign(72, 242, 13, "north", ["CABINET D", "LINE TEST", "RETURN 6", "17 JUN 2012"]),
        wall_sign(88, 242, 13, "north", ["CABINET E", "GATE TEST", "RETURN 9", "18 JUN 2012"]),
    ]
    dispatch += lectern(
        80,
        240,
        -7,
        "south",
        "Dispatch Procedure",
        "Copperline Hosting",
        [
            "Remote service handoff\nrevision 4\nJune 2012",
            "Recover the four route tags in the order written on the loose index card. Close the gaps before applying the account class.",
            "Use the abbreviated commercial DNS class carried on the closure notice.",
            "Read the cabinet returns from A through E. Put the standard host/service separator before those five figures.",
            "No office copy should contain the complete destination. Test the restored route and leave the records in place.",
        ],
    )
    add_ceiling_lights(dispatch, [(70, -3), (80, -3), (90, -3), (72, 6), (88, 6)])
    write_text(function_root / "build" / "dispatch.mcfunction", dispatch)

    passages: list[str] = []
    for x1, x2, left_wall, right_wall in [
        (15, 19, 14, 20),
        (59, 63, 58, 64),
    ]:
        passages += shell(x1, -5, x2, 5)
        passages += [
            f"fill {x1 + 1} 239 -4 {x2 - 1} 239 4 minecraft:smooth_stone",
            opening_x(left_wall),
            opening_x(x1),
            opening_x(x2),
            opening_x(right_wall),
            f"setblock {x1 + 2} 248 0 minecraft:lantern[hanging=true]",
            f"setblock {x1 + 2} 247 0 minecraft:light[level=11]",
        ]
    write_text(function_root / "build" / "passages.mcfunction", passages)


def validate_datapack(root: Path) -> None:
    function_root = root / "data" / "the_hold" / "function"
    function_files = sorted(function_root.rglob("*.mcfunction"))
    if len(function_files) != 9:
        raise ValueError(f"expected 9 compact functions, found {len(function_files)}")

    combined = "\n".join(path.read_text(encoding=UTF8) for path in function_files)
    required = [
        "function the_hold:build/receiving",
        "function the_hold:build/records",
        "function the_hold:build/dispatch",
        "3-6-2\\n\\n1-4-3\\n\\n4-2-1\\n\\n2-5-4",
        "Relay tag OI",
        "Fault moved to Z",
        "Socket SN failed",
        "KER line stable",
        "RETURN 2",
        "RETURN 5",
        "RETURN 6",
        "RETURN 9",
        "commercial DNS class",
        "standard host/service separator",
    ]
    for needle in required:
        if needle not in combined:
            raise ValueError(f"generated datapack is missing required evidence: {needle}")

    lowered = combined.lower()
    for forbidden in ("discord", "arg", "puzzle", "datapack", "placeholder", "todo", "tbd"):
        if forbidden in lowered:
            raise ValueError(f"generated player-facing content contains forbidden term: {forbidden}")

    if re.search(r"\b[a-z0-9.-]+\.(?:com|net|org|gg|io)\s*:\s*\d{2,5}\b", combined, re.I):
        raise ValueError("generated datapack exposes a complete destination")

    # All authored books are filled lecterns, and their front sides face open
    # floor rather than a room shell. Coordinates are deliberately explicit.
    expected_lecterns = {
        (-9, 240, -7, "south"),
        (4, 240, -7, "south"),
        (25, 240, -9, "south"),
        (34, 240, -9, "south"),
        (43, 240, -9, "south"),
        (52, 240, -9, "south"),
        (39, 241, 7, "north"),
        (80, 240, -7, "south"),
    }
    actual_lecterns: set[tuple[int, int, int, str]] = set()
    pattern = re.compile(
        r"^setblock (-?\d+) (-?\d+) (-?\d+) minecraft:lectern\[facing=(\w+),has_book=true\]$",
        re.M,
    )
    for match in pattern.finditer(combined):
        actual_lecterns.add(
            (int(match.group(1)), int(match.group(2)), int(match.group(3)), match.group(4))
        )
    if actual_lecterns != expected_lecterns:
        raise ValueError(f"lectern placement/orientation drift: {actual_lecterns}")
    if combined.count("data merge block") != len(expected_lecterns):
        raise ValueError("one or more lecterns is missing its written book")

    for x in (14, 15, 19, 20, 58, 59, 63, 64):
        if opening_x(x) not in combined:
            raise ValueError(f"two-way passage opening is missing at x={x}")

    for line in combined.splitlines():
        match = re.match(
            r"fill (-?\d+) (-?\d+) (-?\d+) (-?\d+) (-?\d+) (-?\d+) ",
            line,
        )
        if not match:
            continue
        x1, y1, z1, x2, y2, z2 = (int(value) for value in match.groups())
        volume = (abs(x2 - x1) + 1) * (abs(y2 - y1) + 1) * (abs(z2 - z1) + 1)
        if volume > 32768:
            raise ValueError(f"vanilla /fill limit exceeded ({volume}): {line}")
        if min(y1, y2) < 238 or max(y1, y2) > 250:
            raise ValueError(f"fill leaves the proven vertical envelope: {line}")


def build(input_zip: Path, output_zip: Path, sha1_path: Path) -> tuple[int, str]:
    if not input_zip.is_file():
        raise SystemExit(f"input world not found: {input_zip}")

    with tempfile.TemporaryDirectory(prefix="observance-hold-v51-") as temp_name:
        temp = Path(temp_name)
        extracted = temp / "input"
        with zipfile.ZipFile(input_zip, "r") as archive:
            archive.extractall(extracted)

        source_world = extracted / WORLD_ROOT
        level_dat = source_world / "level.dat"
        level_dat_old = source_world / "level.dat_old"
        if not level_dat.is_file() or level_dat.stat().st_size < 1000:
            raise SystemExit("the proven template must contain the-hold/level.dat")

        world = temp / "world" / WORLD_ROOT
        world.mkdir(parents=True)
        shutil.copyfile(level_dat, world / "level.dat")
        if level_dat_old.is_file():
            shutil.copyfile(level_dat_old, world / "level.dat_old")
        else:
            shutil.copyfile(level_dat, world / "level.dat_old")

        datapack = world / PACK_ROOT
        make_datapack(datapack)
        validate_datapack(datapack)

        staged = temp / "the-hold-built.zip"
        with zipfile.ZipFile(
            staged,
            "w",
            compression=zipfile.ZIP_DEFLATED,
            compresslevel=9,
        ) as archive:
            for path in sorted(world.rglob("*")):
                if not path.is_file():
                    continue
                archive_name = (WORLD_ROOT / path.relative_to(world)).as_posix()
                entry = zipfile.ZipInfo(archive_name, date_time=ZIP_TIMESTAMP)
                entry.create_system = 3
                entry.compress_type = zipfile.ZIP_DEFLATED
                entry.external_attr = 0o644 << 16
                archive.writestr(
                    entry,
                    path.read_bytes(),
                    compress_type=zipfile.ZIP_DEFLATED,
                    compresslevel=9,
                )

        output_zip.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(staged, output_zip)

    payload = output_zip.read_bytes()
    sha1 = hashlib.sha1(payload).hexdigest()
    write_text(sha1_path, f"{sha1}  the-hold.zip\n")
    return len(payload), sha1


def main() -> None:
    repo = Path(__file__).resolve().parents[1]
    default_zip = repo / "dashboard" / "content" / "the-hold-v5" / "the-hold.zip"
    parser = argparse.ArgumentParser(
        description="Build the deterministic Copperline service-annex world archive."
    )
    parser.add_argument(
        "--input",
        type=Path,
        default=default_zip,
        help="known-good world ZIP supplying level.dat (defaults to the current output)",
    )
    parser.add_argument("--output", type=Path, default=default_zip)
    parser.add_argument("--sha1", type=Path)
    args = parser.parse_args()

    output = args.output.resolve()
    sha1_path = args.sha1.resolve() if args.sha1 else output.with_suffix(".sha1")
    size, sha1 = build(args.input.resolve(), output, sha1_path)
    print(f"hold prologue: wrote {output}")
    print(f"hold prologue: size {size}")
    print(f"hold prologue: sha1 {sha1}")


if __name__ == "__main__":
    main()
