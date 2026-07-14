#!/usr/bin/env python3
"""Build the downloadable Hold prologue world from an auditable command source.

The shipped world is intentionally small on disk: its datapack constructs the
contained adventure on first load.  This builder preserves the template
level.dat and replaces every old generated function with the production map.
"""

from __future__ import annotations

raise SystemExit(
    "RETIRED PRE-V5 TOOL: build_hold_prologue.py cannot generate a production artifact. "
    "The V5 Deep Hold is built only by the Paper plugin; see design/V5-WORLD-SETUP-AND-TESTING.md."
)

import argparse
import hashlib
import json
import shutil
import tempfile
import zipfile
from pathlib import Path


UTF8 = "utf-8"


def write_text(path: Path, lines: list[str] | str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = lines if isinstance(lines, str) else "\n".join(lines) + "\n"
    path.write_text(text, encoding=UTF8, newline="\n")


def shell(
    x1: int,
    z1: int,
    x2: int,
    z2: int,
    *,
    y1: int = 238,
    y2: int = 252,
    wall: str = "minecraft:deepslate_bricks",
    floor: str = "minecraft:deepslate_tiles",
    roof: str = "minecraft:reinforced_deepslate",
) -> list[str]:
    """Return a closed rectangular shell without any oversized /fill."""
    return [
        f"fill {x1 + 1} {y1 + 1} {z1 + 1} {x2 - 1} {y2 - 1} {z2 - 1} minecraft:air",
        f"fill {x1} {y1} {z1} {x2} {y1} {z2} {floor}",
        f"fill {x1} {y2} {z1} {x2} {y2} {z2} {roof}",
        f"fill {x1} {y1 + 1} {z1} {x1} {y2 - 1} {z2} {wall}",
        f"fill {x2} {y1 + 1} {z1} {x2} {y2 - 1} {z2} {wall}",
        f"fill {x1 + 1} {y1 + 1} {z1} {x2 - 1} {y2 - 1} {z1} {wall}",
        f"fill {x1 + 1} {y1 + 1} {z2} {x2 - 1} {y2 - 1} {z2} {wall}",
    ]


def corridor(z1: int, z2: int) -> list[str]:
    commands = shell(-5, z1, 5, z2, y1=238, y2=248)
    commands.extend(
        [
            f"fill -3 239 {z1 + 1} 3 239 {z2 - 1} minecraft:polished_deepslate",
            f"fill -4 240 {z1 + 2} -4 246 {z2 - 2} minecraft:polished_basalt[axis=y]",
            f"fill 4 240 {z1 + 2} 4 246 {z2 - 2} minecraft:polished_basalt[axis=y]",
        ]
    )
    for z in range(z1 + 4, z2 - 1, 6):
        commands += [
            f"fill -4 247 {z} 4 247 {z} minecraft:polished_blackstone_bricks",
            f"setblock 0 246 {z} minecraft:iron_chain",
            f"setblock 0 245 {z} minecraft:soul_lantern[hanging=true]",
            f"setblock 0 244 {z} minecraft:light[level=10]",
        ]
    return commands


def opening(z: int, *, y1: int = 240, y2: int = 246) -> str:
    return f"fill -2 {y1} {z} 2 {y2} {z} minecraft:air"


def close_gate(z: int) -> list[str]:
    return [
        f"fill -2 240 {z} 2 245 {z} minecraft:polished_blackstone_bricks",
        f"fill -1 240 {z} 1 244 {z} minecraft:iron_bars",
    ]


def book_component(title: str, author: str, pages: list[str]) -> str:
    # 1.21.5+ text components are SNBT compounds. Quoted JSON pages render as
    # literal {"text":...}, which is the exact production bug this replaces.
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


def lectern(x: int, y: int, z: int, facing: str, title: str, author: str, pages: list[str]) -> list[str]:
    component = book_component(title, author, pages)
    stored_component = component.replace(
        "minecraft:written_book_content=", '"minecraft:written_book_content":', 1
    )
    return [
        f"setblock {x} {y} {z} minecraft:lectern[facing={facing},has_book=true]",
        f'data merge block {x} {y} {z} {{Book:{{id:"minecraft:written_book",count:1,components:{{{stored_component}}}}},Page:0}}',
    ]


def wall_sign(x: int, y: int, z: int, facing: str, lines: list[str], color: str = "gray") -> str:
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


def named_item(item: str, name: str, color: str = "gray") -> str:
    return (
        f'minecraft:{item}[minecraft:item_name={{text:{json.dumps(name)},color:"{color}",italic:false}}]'
    )


def item_book(title: str, author: str, pages: list[str]) -> str:
    return f"minecraft:written_book[{book_component(title, author, pages)}]"


def add_ribs(commands: list[str], x1: int, x2: int, z_values: list[int], top: int = 250) -> None:
    for z in z_values:
        commands += [
            f"fill {x1} 240 {z} {x1} {top - 1} {z} minecraft:polished_basalt[axis=y]",
            f"fill {x2} 240 {z} {x2} {top - 1} {z} minecraft:polished_basalt[axis=y]",
            f"fill {x1} {top} {z} {x2} {top} {z} minecraft:polished_blackstone_bricks",
        ]


def make_datapack(root: Path) -> None:
    function_root = root / "data" / "the_hold" / "function"
    minecraft_tags = root / "data" / "minecraft" / "tags" / "function"

    pack_meta = {
        "pack": {
            "min_format": 94,
            "max_format": 94,
            "description": "The Hold — production invitation prologue",
        }
    }
    write_text(root / "pack.mcmeta", json.dumps(pack_meta, indent=2) + "\n")
    write_text(minecraft_tags / "load.json", json.dumps({"values": ["the_hold:load"]}, indent=2) + "\n")
    write_text(minecraft_tags / "tick.json", json.dumps({"values": ["the_hold:tick"]}, indent=2) + "\n")

    load = [
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
        "worldborder center 0 152",
        "worldborder set 448",
        "function the_hold:build/all",
        "tag @a remove hold_started",
    ]
    write_text(function_root / "load.mcfunction", load)

    spawn = [
        "tag @s remove hold_1",
        "tag @s remove hold_2",
        "tag @s remove hold_3",
        "tag @s remove hold_4",
        "tag @s remove hold_5",
        "tag @s remove hold_done",
        "clear @s",
        "effect clear @s",
        "gamemode adventure @s",
        "tp @s 0.5 240 -23.5 0 0",
        "spawnpoint @s 0 240 -23",
        "tag @s add hold_started",
        "title @s times 20 80 40",
        'title @s subtitle {text:"follow the lit aisle. read what was left.",color:"dark_gray"}',
        'title @s title {text:"THE HOLD",color:"gray",bold:true}',
        "playsound minecraft:ambient.cave master @s 0 241 -18 0.35 0.65",
    ]
    write_text(function_root / "spawn.mcfunction", spawn)

    tick = [
        "execute as @a[tag=!hold_started] run function the_hold:spawn",
        "execute as @a[tag=hold_started] unless entity @s[x=-31,y=232,z=-33,dx=62,dy=28,dz=371] run function the_hold:spawn",
        "effect give @a[tag=hold_started] minecraft:saturation 2 0 true",
        "execute as @a[tag=hold_started,tag=!hold_1,x=-6,y=239,z=12,dx=12,dy=8,dz=12] run function the_hold:triggers/record",
        "execute as @a[tag=hold_1,tag=!hold_2,x=-7,y=239,z=73,dx=14,dy=8,dz=11] run function the_hold:triggers/domestic",
        "execute as @a[tag=hold_2,tag=!hold_3,x=-7,y=238,z=137,dx=14,dy=10,dz=10] run function the_hold:triggers/cistern",
        "execute as @a[tag=hold_3,tag=!hold_4] if block 0 240 197 minecraft:lever[powered=true] run function the_hold:triggers/lamp_change",
        "execute as @a[tag=hold_4,tag=!hold_5,x=-5,y=239,z=253,dx=10,dy=9,dz=9] run function the_hold:triggers/register",
        "execute as @a[tag=hold_5,tag=!hold_done,x=-7,y=239,z=316,dx=14,dy=9,dz=12] run function the_hold:triggers/final",
    ]
    write_text(function_root / "tick.mcfunction", tick)

    all_build = [
        "forceload add -32 -32 32 352",
        "function the_hold:build/arrival",
        "function the_hold:build/domestic",
        "function the_hold:build/cistern",
        "function the_hold:build/lampworks",
        "function the_hold:build/register",
        "function the_hold:build/dispatch",
        "function the_hold:build/passages",
        "forceload remove all",
    ]
    write_text(function_root / "build" / "all.mcfunction", all_build)

    # 1 — Archive Vestibule. Wide central sightline, all shelf faces point in.
    arrival = shell(-17, -28, 17, 28)
    arrival += [
        "fill -15 239 -26 15 239 26 minecraft:deepslate_tiles",
        "fill -3 239 -26 3 239 25 minecraft:polished_deepslate",
        "fill -2 239 -26 2 239 25 minecraft:gray_carpet",
        opening(28),
    ]
    arrival += close_gate(28)
    add_ribs(arrival, -15, 15, [-18, -6, 6, 18])
    for z in [-20, -14, -8, -2, 4, 10]:
        if z == 10:
            arrival += [
                f"setblock -16 240 {z} minecraft:chiseled_bookshelf[facing=east]",
                f"setblock 16 240 {z} minecraft:chiseled_bookshelf[facing=west]",
                f"item replace block -16 240 {z} container.0 with {item_book('WEST SHELF COPY', 'copy room', ['the opening line was copied here.\n\nthe copy ends before the seventh name.'])} 1",
                f"item replace block 16 240 {z} container.0 with {item_book('EAST SHELF COPY', 'copy room', ['the closing line was copied here.\n\nno seventh hand signed beneath it.'])} 1",
            ]
        else:
            arrival += [
                f"setblock -16 240 {z} minecraft:bookshelf",
                f"setblock 16 240 {z} minecraft:bookshelf",
            ]
        arrival += [
            f"setblock -15 241 {z} minecraft:light[level=8]",
            f"setblock 15 241 {z} minecraft:light[level=8]",
        ]
    for z in [-12, 0, 12, 24]:
        arrival += [
            f"setblock -8 249 {z} minecraft:iron_chain",
            f"setblock -8 248 {z} minecraft:soul_lantern[hanging=true]",
            f"setblock 8 249 {z} minecraft:iron_chain",
            f"setblock 8 248 {z} minecraft:soul_lantern[hanging=true]",
            f"setblock 0 247 {z} minecraft:light[level=11]",
        ]
    arrival += [
        "fill -4 239 13 4 239 21 minecraft:polished_blackstone_bricks",
        "fill -3 240 15 3 240 20 minecraft:polished_deepslate_slab[type=bottom]",
        "setblock 0 243 23 minecraft:polished_blackstone_bricks",
        wall_sign(0, 243, 22, "north", ["COPY ROOM", "REGISTER 0 / 7", "DO NOT AMEND"]),
    ]
    arrival += lectern(
        0,
        240,
        18,
        "north",
        "THE RECORD",
        "m.kept",
        [
            "the record is kept in more than one place.\n\nnot every copy is a place you can walk to.",
            "six hands carried this copy out.\n\none line was left empty.",
            "read the rooms in order.\n\nwhat repeats is true.\nwhat is missing matters.",
            "dispatch fragment i:\n\nSN",
        ],
    )
    write_text(function_root / "build" / "arrival.mcfunction", arrival)

    # 2 — Domestic Hall. Six occupied settings and a deliberate seventh blank.
    domestic = shell(-18, 43, 18, 88)
    domestic += [opening(43), opening(88)] + close_gate(88)
    domestic += [
        "fill -16 239 45 16 239 86 minecraft:deepslate_tiles",
        "fill -3 239 44 3 239 86 minecraft:polished_deepslate",
        "fill -6 239 55 6 239 70 minecraft:spruce_planks",
        "fill -5 240 57 5 240 68 minecraft:dark_oak_slab[type=top]",
        "fill -5 240 56 5 240 56 minecraft:dark_oak_stairs[facing=south]",
        "fill -5 240 69 5 240 69 minecraft:dark_oak_stairs[facing=north]",
    ]
    add_ribs(domestic, -16, 16, [50, 62, 74, 84])
    # Six complete bed alcoves, three per side; all are reachable from the aisle.
    for z in [49, 61, 73]:
        domestic += [
            f"fill -16 240 {z - 2} -11 244 {z + 4} minecraft:spruce_planks hollow",
            f"fill 11 240 {z - 2} 16 244 {z + 4} minecraft:spruce_planks hollow",
            f"fill -11 240 {z - 1} -11 243 {z + 3} minecraft:air",
            f"fill 11 240 {z - 1} 11 243 {z + 3} minecraft:air",
            f"setblock -13 240 {z} minecraft:gray_bed[facing=south,part=foot]",
            f"setblock -13 240 {z + 1} minecraft:gray_bed[facing=south,part=head]",
            f"setblock 13 240 {z} minecraft:gray_bed[facing=south,part=foot]",
            f"setblock 13 240 {z + 1} minecraft:gray_bed[facing=south,part=head]",
            f"setblock -13 243 {z + 2} minecraft:soul_lantern[hanging=true]",
            f"setblock 13 243 {z + 2} minecraft:soul_lantern[hanging=true]",
        ]
    for x, z in [(-4, 59), (0, 59), (4, 59), (-4, 66), (0, 66), (4, 66)]:
        domestic += [
            f"setblock {x} 241 {z} minecraft:flower_pot",
            f"setblock {x} 242 {z} minecraft:light[level=7]",
        ]
    domestic += [
        "fill -4 239 76 4 239 83 minecraft:polished_blackstone_bricks",
        "setblock -4 240 78 minecraft:barrel[facing=east]",
        "setblock 4 240 78 minecraft:barrel[facing=west]",
        "setblock 0 240 82 minecraft:dark_oak_slab[type=top]",
        "setblock 0 242 86 minecraft:polished_blackstone_bricks",
        wall_sign(0, 242, 85, "north", ["SEVEN PLACES", "SIX RETURNED", "ONE UNNAMED"]),
        f"item replace block -4 240 78 container.3 with {item_book('copied place ledger', 'm.kept', ['six beds were copied from the first room.', 'six bowls were set back on the table.\n\nthe seventh place has no name.', 'do not fill the blank to make the count look whole.'])} 1",
        f"item replace block 4 240 78 container.1 with {named_item('bowl', 'place seven — unissued', 'dark_gray')} 1",
    ]
    domestic += lectern(
        0,
        240,
        79,
        "north",
        "THE EMPTY PLACE",
        "m.kept",
        [
            "the room remembers six sleepers.",
            "the table was laid for seven.\n\nthe last bowl was never used.",
            "an empty place is still part of the record.",
            "dispatch fragment ii:\n\nOI",
        ],
    )
    write_text(function_root / "build" / "domestic.mcfunction", domestic)

    # 3 — Reed Cistern. The route stays dry and railed; the seventh mark is below.
    cistern = shell(-20, 103, 20, 150, y1=235, y2=253)
    cistern += [opening(103), opening(150)] + close_gate(150)
    cistern += [
        "fill -18 236 105 18 236 148 minecraft:polished_deepslate",
        "fill -17 237 109 17 237 145 minecraft:water",
        "fill -17 236 109 17 236 145 minecraft:dark_prismarine",
        "fill -3 238 104 3 238 149 minecraft:polished_deepslate",
        "fill -2 239 104 2 239 149 minecraft:gray_carpet",
        "fill -5 238 106 -4 239 148 minecraft:polished_blackstone_wall",
        "fill 4 238 106 5 239 148 minecraft:polished_blackstone_wall",
        "fill -18 238 105 -15 238 148 minecraft:deepslate_tiles",
        "fill 15 238 105 18 238 148 minecraft:deepslate_tiles",
        "setblock 0 236 132 minecraft:sea_lantern",
        "setblock 0 237 132 minecraft:blue_stained_glass",
        "setblock 0 240 134 minecraft:tuff_bricks",
        wall_sign(0, 240, 133, "north", ["VII", "ANSWERS BELOW", "NOT ABOVE"]),
    ]
    # Adventure-mode recovery points. The bridge rail is deliberately solid
    # everywhere else, but a player who investigates the submerged seventh
    # mark must always be able to climb back onto the dry route without placing
    # or breaking blocks. Each lit ladder has a clear water-side approach and
    # lands directly on the bridge edge.
    for z in [126, 140]:
        cistern += [
            f"fill -5 238 {z} -4 239 {z} minecraft:air",
            f"fill 4 238 {z} 5 239 {z} minecraft:air",
            f"setblock -4 237 {z} minecraft:sea_lantern",
            f"setblock 4 237 {z} minecraft:sea_lantern",
            f"setblock -4 238 {z} minecraft:ladder[facing=west]",
            f"setblock 4 238 {z} minecraft:ladder[facing=east]",
        ]
    for x in [-13, -8, -3, 3, 8, 13]:
        cistern += [
            f"fill {x} 238 116 {x} 246 116 minecraft:polished_basalt[axis=y]",
            f"setblock {x} 247 116 minecraft:soul_lantern[hanging=true]",
        ]
    for x in [-16, -12, 12, 16]:
        for z in [112, 122, 138, 143]:
            cistern += [
                f"setblock {x} 237 {z} minecraft:mud",
                f"setblock {x} 238 {z} minecraft:sugar_cane",
                f"setblock {x} 239 {z} minecraft:sugar_cane",
            ]
    for z in [110, 124, 138, 146]:
        cistern += [
            f"setblock -10 248 {z} minecraft:iron_chain",
            f"setblock -10 247 {z} minecraft:soul_lantern[hanging=true]",
            f"setblock 10 248 {z} minecraft:iron_chain",
            f"setblock 10 247 {z} minecraft:soul_lantern[hanging=true]",
            f"setblock 0 246 {z} minecraft:light[level=10]",
        ]
    cistern += lectern(
        0,
        239,
        142,
        "north",
        "WHERE THE REEDS FOLD",
        "m.kept",
        [
            "six posts stand where the stone stays dry.",
            "the seventh was not raised.\n\nit only answers from the water.",
            "look down before you count again.",
            "dispatch fragment iii:\n\nKER",
        ],
    )
    write_text(function_root / "build" / "cistern.mcfunction", cistern)

    # 4 — Lampworks. The one real interaction changes the room and opens the gate.
    lampworks = shell(-18, 164, 18, 207)
    lampworks += [opening(164), opening(207)] + close_gate(207)
    lampworks += [
        "fill -16 239 166 16 239 205 minecraft:deepslate_tiles",
        "fill -3 239 165 3 239 205 minecraft:polished_deepslate",
        "fill -5 239 181 5 239 192 minecraft:polished_blackstone_bricks",
        "fill -3 240 183 3 240 190 minecraft:magma_block",
        "fill -2 241 184 2 241 189 minecraft:campfire[lit=true]",
        "fill -6 240 180 -6 244 193 minecraft:polished_basalt[axis=y]",
        "fill 6 240 180 6 244 193 minecraft:polished_basalt[axis=y]",
        "setblock 0 239 197 minecraft:polished_blackstone",
        "setblock 0 240 197 minecraft:lever[face=floor,facing=south,powered=false]",
        "setblock 0 242 200 minecraft:oxidized_cut_copper",
    ]
    add_ribs(lampworks, -16, 16, [170, 180, 192, 202])
    for x, z in [(-12, 174), (0, 174), (12, 174), (-12, 199), (0, 199), (12, 199)]:
        lampworks += [
            f"setblock {x} 240 {z} minecraft:cut_copper",
            f"setblock {x} 241 {z} minecraft:oxidized_copper_bulb[lit=true,powered=true]",
            f"setblock {x} 242 {z} minecraft:light[level=10]",
        ]
    lampworks += [
        "setblock 0 245 187 minecraft:iron_chain",
        "setblock 0 244 187 minecraft:oxidized_copper_bulb[lit=false,powered=false]",
        wall_sign(0, 242, 199, "north", ["WAKE THE LAMP", "NO HAND TENDS", "DO NOT RELIGHT"]),
    ]
    write_text(function_root / "build" / "lampworks.mcfunction", lampworks)

    # 5 — Register Gallery. Every removable shelf book is complete, never filler.
    register = shell(-20, 221, 20, 265)
    register += [opening(221), opening(265)] + close_gate(265)
    register += [
        "fill -18 239 223 18 239 263 minecraft:deepslate_tiles",
        "fill -3 239 222 3 239 263 minecraft:polished_deepslate",
        "fill -5 239 248 5 239 260 minecraft:polished_blackstone_bricks",
        "setblock 0 243 262 minecraft:tuff_bricks",
        wall_sign(0, 243, 261, "north", ["THE SEVENTH LINE", "WAS NEVER FILED", "THE RECORD KEEPS"]),
    ]
    add_ribs(register, -18, 18, [227, 237, 247, 257])
    shelf_data = [
        (-19, 229, "east", "FIRST HAND", "copied the room, then crossed its own name away."),
        (-19, 239, "east", "SECOND HAND", "counted the living twice and still returned six."),
        (-19, 249, "east", "THIRD HAND", "heard water answer from below the dry stones."),
        (19, 229, "west", "FOURTH HAND", "kept the fire until the untended lamp woke."),
        (19, 239, "west", "FIFTH HAND", "filed the provider apart from the service."),
        (19, 249, "west", "SIXTH HAND", "removed the address and left the directory row."),
    ]
    service_marks = ["2", "5", "5", "6", "9", ":"]
    for index, (x, z, facing, title, page) in enumerate(shelf_data, 1):
        service_page = (
            f"service digit {index} / {service_marks[index - 1]}"
            if index <= 5
            else "service separator / :\n\nplace it before the five digits."
        )
        register += [
            f"setblock {x} 240 {z} minecraft:chiseled_bookshelf[facing={facing}]",
            f"item replace block {x} 240 {z} container.0 with {item_book(title, 'copy register', [page, 'mark ' + str(index) + ' of 6.\n\nthe next line remains blank.', service_page])} 1",
            f"setblock {x + (1 if x < 0 else -1)} 241 {z} minecraft:light[level=8]",
        ]
    register += [
        "fill -4 240 252 4 244 260 minecraft:tuff_bricks hollow",
        "fill -2 240 252 2 243 252 minecraft:air",
        "setblock 0 240 259 minecraft:chiseled_bookshelf[facing=north]",
        "setblock 0 241 258 minecraft:light[level=7]",
    ]
    register += lectern(
        0,
        240,
        252,
        "north",
        "REGISTER 0 / 7",
        "m.kept",
        [
            "six marks are present.\n\nthe seventh line is not damaged. it was never filed.",
            "do not invent a seventh hand to make the copy feel complete.",
            "margin instruction:\n\nthe record keeps",
            "the first five hands file five service digits.\n\nthe sixth files the mark placed before them.",
        ],
    )
    write_text(function_root / "build" / "register.mcfunction", register)

    # 6 — Dispatch Office. The destination is reconstructed from evidence carried through the route.
    dispatch = shell(-20, 279, 20, 333)
    dispatch += [opening(279)]
    dispatch += [
        "fill -18 239 281 18 239 331 minecraft:deepslate_tiles",
        "fill -3 239 280 3 239 330 minecraft:polished_deepslate",
        "fill -17 240 287 17 246 287 minecraft:tuff_bricks",
        "fill -15 240 288 15 244 290 minecraft:spruce_planks",
        "fill -2 240 287 2 244 290 minecraft:air",
        "setblock 0 245 288 minecraft:tuff_bricks",
        "fill -15 240 299 15 240 307 minecraft:dark_oak_planks",
        "fill -15 241 300 15 241 306 minecraft:dark_oak_slab[type=top]",
        wall_sign(0, 245, 287, "north", ["ACCOUNT DISPATCH", "RETIRED DIRECTORY", "FIELD COPIES"]),
    ]
    add_ribs(dispatch, -18, 18, [285, 297, 311, 326])
    fields = [
        (-12, "HOST I", "ARCHIVE", named_item("paper", "return to the archive fragment", "dark_aqua")),
        (-4, "HOST II", "EMPTY PLACE", named_item("paper", "return to the empty-place fragment", "dark_aqua")),
        (4, "HOST III", "BELOW WATER", named_item("paper", "return to the water fragment", "dark_aqua")),
        (12, "HOST IV", "WAKING LAMP", named_item("paper", "return to the lamp fragment", "dark_aqua")),
    ]
    for x, label, source, item in fields:
        dispatch += [
            f"setblock {x} 240 303 minecraft:barrel[facing=north]",
            wall_sign(x, 241, 302, "north", [label, source]),
            f"item replace block {x} 240 303 container.13 with {item} 1",
            f"setblock {x} 243 303 minecraft:light[level=9]",
        ]
    dispatch += [
        "setblock -9 240 320 minecraft:barrel[facing=east]",
        "setblock 9 240 320 minecraft:barrel[facing=west]",
        f"item replace block -9 240 320 container.4 with {named_item('name_tag', 'ending: common web', 'gray')} 1",
        f"item replace block 9 240 320 container.4 with {named_item('compass', 'service: sixth mark then first five hands', 'gray')} 1",
        wall_sign(-8, 242, 320, "east", ["HOST ENDING", "COMMON WEB"]),
        wall_sign(8, 242, 320, "west", ["SERVICE", "SIXTH MARK", "FIRST FIVE"]),
    ]
    dispatch += lectern(
        0,
        240,
        320,
        "north",
        "HANDOFF",
        "m.kept",
        [
            "the dead directory was the road to this copy.\n\nthe live address is kept inside the walk.",
            "four host fragments were filed in rooms one through four.\n\njoin them in numeral order without spaces.",
            "the host ending is common web.",
            "the sixth hand files the service mark.\n\nplace it before the five digits filed by hands one through five.",
            "assemble host + ending + service.\n\nbring the others there. find the retired Copperline relay near the first landing.",
            "recover its remote room. bind every field name before one of you writes kept in #the-record.",
        ],
    )
    for z in [292, 308, 324]:
        dispatch += [
            f"setblock -10 249 {z} minecraft:iron_chain",
            f"setblock -10 248 {z} minecraft:soul_lantern[hanging=true]",
            f"setblock 10 249 {z} minecraft:iron_chain",
            f"setblock 10 248 {z} minecraft:soul_lantern[hanging=true]",
            f"setblock 0 247 {z} minecraft:light[level=10]",
        ]
    write_text(function_root / "build" / "dispatch.mcfunction", dispatch)

    passages: list[str] = []
    for z1, z2, next_room_z in [
        (29, 42, 43),
        (89, 102, 103),
        (151, 163, 164),
        (208, 220, 221),
        (266, 278, 279),
    ]:
        passages += corridor(z1, z2)
        # shell() closes both ends of every corridor.  Those endcaps are not
        # progression gates: leaving them in place creates an invisible second
        # and third wall after the room gate opens.  Keep the room-side gate as
        # the only controlled barrier and permanently join the corridor to both
        # adjacent rooms.
        passages += [opening(z1), opening(z2), opening(next_room_z)]
    write_text(function_root / "build" / "passages.mcfunction", passages)

    triggers = {
        "record": [
            "tag @s add hold_1",
            opening(28),
            'title @s actionbar {text:"a lock yields beyond the register.",color:"dark_gray"}',
            "playsound minecraft:block.vault.open_shutter master @s 0 242 28 0.6 0.7",
        ],
        "domestic": [
            "tag @s add hold_2",
            opening(88),
            'title @s actionbar {text:"six places answer. the blank remains.",color:"dark_gray"}',
            "playsound minecraft:block.vault.open_shutter master @s 0 242 88 0.6 0.72",
        ],
        "cistern": [
            "tag @s add hold_3",
            opening(150),
            'title @s actionbar {text:"something below the water returns the count.",color:"dark_aqua"}',
            "playsound minecraft:block.vault.open_shutter master @s 0 242 150 0.6 0.68",
        ],
        "lamp_change": [
            "tag @s add hold_4",
            "fill -2 241 184 2 241 189 minecraft:campfire[lit=false]",
            "setblock 0 244 187 minecraft:oxidized_copper_bulb[lit=true,powered=true]",
            "setblock 0 243 187 minecraft:light[level=12]",
            wall_sign(0, 242, 199, "north", ["LAMP RETURNED", "DISPATCH IV", "Z", "FILED"]),
            opening(207),
            'title @s actionbar {text:"the untended lamp wakes. the kept fire dies.",color:"dark_aqua"}',
            "playsound minecraft:block.respawn_anchor.deplete master @s 0 242 187 0.75 0.55",
            "playsound minecraft:block.vault.open_shutter master @s 0 242 207 0.6 0.65",
        ],
        "register": [
            "tag @s add hold_5",
            opening(265),
            'title @s actionbar {text:"the unfiled line opens the dispatch office.",color:"dark_gray"}',
            "playsound minecraft:block.vault.open_shutter master @s 0 242 265 0.6 0.66",
        ],
        "final": [
            "tag @s add hold_done",
            "title @s times 20 80 40",
            'title @s subtitle {text:"the live address is complete in this copy.",color:"dark_gray"}',
            'title @s title {text:"ASSEMBLE THE HANDOFF",color:"gray",bold:true}',
            "playsound minecraft:block.respawn_anchor.deplete master @s 0 242 320 0.4 0.5",
        ],
    }
    for name, commands in triggers.items():
        write_text(function_root / "triggers" / f"{name}.mcfunction", commands)


def build(input_zip: Path, output_zip: Path, keep_backup: bool) -> tuple[int, str]:
    if not input_zip.is_file():
        raise SystemExit(f"input world not found: {input_zip}")

    with tempfile.TemporaryDirectory(prefix="observance-hold-prologue-") as temp_name:
        temp = Path(temp_name)
        with zipfile.ZipFile(input_zip, "r") as archive:
            archive.extractall(temp)

        world = temp / "the-hold"
        if not (world / "level.dat").is_file():
            raise SystemExit("the template zip must contain the-hold/level.dat")

        datapacks = world / "datapacks"
        if datapacks.exists():
            shutil.rmtree(datapacks)
        make_datapack(datapacks / "the_hold")

        for stale in ("session.lock", "uid.dat"):
            (world / stale).unlink(missing_ok=True)

        staged = temp / "the-hold-built.zip"
        with zipfile.ZipFile(staged, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
            for path in sorted(world.rglob("*")):
                if path.is_file():
                    archive_name = (Path("the-hold") / path.relative_to(world)).as_posix()
                    entry = zipfile.ZipInfo(archive_name, date_time=(2026, 7, 12, 0, 0, 0))
                    entry.compress_type = zipfile.ZIP_DEFLATED
                    entry.external_attr = 0o644 << 16
                    archive.writestr(entry, path.read_bytes(), compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)

        output_zip.parent.mkdir(parents=True, exist_ok=True)
        if keep_backup and output_zip.exists():
            shutil.copy2(output_zip, output_zip.with_suffix(output_zip.suffix + ".bak"))
        shutil.copy2(staged, output_zip)

    payload = output_zip.read_bytes()
    return len(payload), hashlib.sha1(payload).hexdigest()


def main() -> None:
    repo = Path(__file__).resolve().parents[1]
    default_zip = repo / "dashboard" / "public" / "the-hold" / "the-hold.zip"
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", type=Path, default=default_zip)
    parser.add_argument("--output", type=Path, default=default_zip)
    parser.add_argument("--backup", action="store_true")
    args = parser.parse_args()
    size, sha1 = build(args.input.resolve(), args.output.resolve(), args.backup)
    print(f"hold prologue: wrote {args.output.resolve()}")
    print(f"hold prologue: size {size}")
    print(f"hold prologue: sha1 {sha1}")


if __name__ == "__main__":
    main()
