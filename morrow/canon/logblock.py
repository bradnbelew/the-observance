"""LogBlock rows (2011 MySQL export format, numeric block IDs).

Cipher-bearing rows are derived from `ciphers`; everything else is seeded noise so the
viewer feels like a real busy log. Same seed, same rows, every build.
"""

from __future__ import annotations

import random
from datetime import datetime, timedelta

from . import ciphers, layout

WOOL_ID = 35
GLASS, GLOWSTONE, LEVER, CHEST = 20, 89, 69, 54

NOISE_BLOCKS = [(1, "stone"), (3, "dirt"), (4, "cobblestone"), (5, "wood"), (17, "log"), (12, "sand"),
                (13, "gravel"), (50, "torch"), (85, "fence"), (20, "glass"), (66, "rail")]
NOISE_PLAYERS = ["rookery", "cairn", "finchline", "juni_lantern", "patchcord", "kitbag"]


def _row(at: datetime, player: str, action: str, block_id: int, data: int, xyz, extra: str = ""):
    x, y, z = xyz
    return {"date": at.strftime("%Y-%m-%d %H:%M:%S"), "player": player, "action": action,
            "block": "%d:%d" % (block_id, data) if data else str(block_id), "x": x, "y": y, "z": z, "extra": extra}


def floor_rows() -> list[dict]:
    """Rookery laid his cellar floor over four evenings in early June 2011."""
    floor = ciphers.build_floor()
    x0, y0, z0 = layout.ANCHORS["rookery_cellar_floor"]
    start = datetime(2011, 6, 3, 19, 40, 0)
    rows = []
    for i, (r, c, wool) in enumerate(floor.placement_order):
        evening, n = divmod(i, 41)
        at = start + timedelta(days=evening, seconds=n * 9 + (n * 7) % 5)
        rows.append(_row(at, "rookery", "placed", WOOL_ID, wool, (x0 + c, y0, z0 + r)))
    return rows


def id_wall_rows() -> list[dict]:
    x0, y0, z0 = layout.ANCHORS["maintenance_annex"]
    at = datetime(2011, 7, 8, 16, 14, 2)
    return [_row(at + timedelta(seconds=i), "i_bell", "placed", w["id"], 0, (x0 + 2 + i, y0 + 1, z0))
            for i, w in enumerate(ciphers.build_id_wall())]


def tower_rows() -> list[dict]:
    """Morrow completing June's tower at 04:11-04:16 on 18 May 2011."""
    x0, y0, z0 = layout.ANCHORS["june_tower"]
    rows, at = [], datetime(2011, 5, 18, 4, 11, 4)
    top = y0 + 22
    for i, (dx, dz) in enumerate([(dx, dz) for dx in range(-1, 2) for dz in range(-1, 2)]):
        rows.append(_row(at + timedelta(seconds=i * 19), "morrow", "placed", GLASS, 0, (x0 + dx, top + 1, z0 + dz)))
    rows.append(_row(datetime(2011, 5, 18, 4, 16, 1), "morrow", "placed", GLOWSTONE, 0, (x0, top, z0)))
    return rows


def iona_chest_rows() -> list[dict]:
    xyz = layout.ANCHORS["iona_key_chest"]
    at = datetime(2011, 6, 2, 23, 4, 0)
    ids = {"cobblestone": 4, "dirt": 3, "sand": 12, "gravel": 13, "planks": 5, "torch": 50, "arrow": 262, "wood": 17}
    return [_row(at + timedelta(seconds=s["slot"] * 2), "i_bell", "chest_put", ids[s["item"]], 0, xyz,
                 "slot %d amount %d" % (s["slot"], s["count"])) for s in ciphers.build_iona_chest()]


def finch_lock_rows() -> list[dict]:
    """D1 layer 3: finch built her lock on 11 June 2011; the torch rows show which channels invert."""
    from . import later
    x0, y0, z0 = layout.ANCHORS["finch_spur"]
    at = datetime(2011, 6, 11, 19, 2, 0)
    rows = []
    for i in range(8):
        rows.append(_row(at + timedelta(seconds=i * 20), "finchline", "placed", LEVER, 5, (x0 + 2 * i, y0 + 1, z0)))
        if later.FINCH_LOCK_BITS[i] == "1":
            rows.append(_row(at + timedelta(seconds=i * 20 + 9), "finchline", "placed", 76, 3, (x0 + 2 * i, y0, z0 + 3)))
    return rows


def noise_rows(n: int = 2400) -> list[dict]:
    rng = random.Random(6118)
    t0, span = datetime(2011, 2, 23, 0, 0), int(timedelta(days=170).total_seconds())
    rows = []
    for _ in range(n):
        at = t0 + timedelta(seconds=rng.randrange(span))
        bid, _name = rng.choice(NOISE_BLOCKS)
        rows.append(_row(at, rng.choice(NOISE_PLAYERS), rng.choice(["placed", "placed", "broke"]), bid, 0,
                         (rng.randint(-300, 330), rng.randint(58, 80), rng.randint(-280, 240))))
    return rows


def all_rows() -> list[dict]:
    rows = floor_rows() + id_wall_rows() + tower_rows() + iona_chest_rows() + finch_lock_rows() + noise_rows()
    return sorted(rows, key=lambda r: r["date"])


def rows_for_act(act: int) -> list[dict]:
    """The viewer opens in Act 3; Iona's chest rows only become meaningful (and visible) from Act 7."""
    if act < 3:
        return []
    return [r for r in all_rows() if act >= 7 or r["action"] != "chest_put"]
