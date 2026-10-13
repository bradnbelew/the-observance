"""LogBlock rows (2011 MySQL export format, numeric block IDs).

Cipher-bearing rows are derived from `ciphers`; everything else is seeded noise so the
viewer feels like a real busy log. Same seed, same rows, every build.

The noise follows people, not a clock: each resident plays in their own hours (cairn after his
night shift, Kit after school and at weekends, patchcord in UK evenings, June before work and
after it), nobody has rows before they joined, nothing is logged while the server was broken
(21-22 April) or during Iona's 02:00-04:00 restore passes, and after 18 May it all but stops.
"""

from __future__ import annotations

import random
from datetime import date, datetime, timedelta

from . import ciphers, layout

WOOL_ID = 35
GLASS, GLOWSTONE, LEVER, CHEST, POWERED_RAIL, PLANKS = 20, 89, 69, 54, 27, 5

OFFLINE_DAYS = ("2011-04-21", "2011-04-22")       # storage failure (later.OFFLINE_DAYS agrees)
RESTORE_PASSES = (date(2011, 4, 26), date(2011, 5, 18))  # nightly 02:00-04:00, stay off
MAP_START = datetime(2011, 2, 23, 0, 0)          # the Beta 1.3 map (and its LogBlock db) began here
QUIET_FROM = date(2011, 5, 19)                    # after the incident nobody really builds


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
    return [_row(at + timedelta(seconds=i * 7 + (i * 3) % 4), "i_bell", "placed", w["id"], 0, (x0 + 2 + i, y0 + 1, z0))
            for i, w in enumerate(ciphers.build_id_wall())]


TOWER_GLASS_START = datetime(2011, 5, 18, 4, 11, 4)
TOWER_GLASS_EVERY = 30   # seconds between panes: 4 by June's 04:12:40 shot, 7 by 04:14:33, all 9 by 04:15:04
TOWER_GLOWSTONE_AT = datetime(2011, 5, 18, 4, 13, 50)


def tower_rows() -> list[dict]:
    """Morrow completing June's tower on 18 May 2011: a glass roof and a glowstone in the top."""
    x0, y0, z0 = layout.ANCHORS["june_tower"]
    top = y0 + 22
    rows = []
    for i, (dx, dz) in enumerate([(dx, dz) for dx in range(-1, 2) for dz in range(-1, 2)]):
        rows.append(_row(TOWER_GLASS_START + timedelta(seconds=i * TOWER_GLASS_EVERY), "morrow", "placed", GLASS, 0,
                         (x0 + dx, top + 1, z0 + dz)))
    rows.append(_row(TOWER_GLOWSTONE_AT, "morrow", "placed", GLOWSTONE, 0, (x0, top, z0)))
    return rows


def south_rail_rows() -> list[dict]:
    """Morrow ran track 'three bits past where i stopped' on finch's south branch, 18 May 2011."""
    x0, y0, z0 = layout.ANCHORS["south_rail_end"]
    dx, dz = layout.SOUTH_RAIL_DIR
    at = datetime(2011, 5, 18, 5, 21, 37)
    return [_row(at + timedelta(seconds=11 * i), "morrow", "placed", POWERED_RAIL, 0,
                 (x0 + dx * (i + 1), y0, z0 + dz * (i + 1))) for i in range(3)]


def storehouse_tidy_rows() -> list[dict]:
    """2 Sept 2011, after the watchdog brought it back: Morrow squares cairn's storehouse to the grid."""
    x0, y0, z0 = layout.ANCHORS["cairn_storehouse_door_2011"]
    x1, _, _ = layout.ANCHORS["cairn_storehouse_door_morrow"]
    at = datetime(2011, 9, 2, 5, 31, 2)
    rows = []
    for i in range(10):
        rows.append(_row(at + timedelta(seconds=4 * i), "morrow", "broke", PLANKS, 0, (x0 + (i % 3), y0 + i // 3, z0 + 1)))
        rows.append(_row(at + timedelta(seconds=4 * i + 2), "morrow", "placed", PLANKS, 0, (x1 + (i % 3), y0 + i // 3, z0 + 1)))
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


# ---------------------------------------------------------------------------
# Residents' everyday noise

# hours are Chicago time; an end past 24 runs into the small hours. (weekday windows, weekend windows)
HABITS = {
    "rookery": {"joined": datetime(2011, 2, 23, 0, 0), "p": 0.8, "home": ["rookery_house", "spawn"],
                "wd": [(16.0, 25.0)], "we": [(11.0, 26.0)],
                "blocks": [(1, 0), (4, 0), (5, 0), (3, 0), (50, 0), (65, 0), (20, 0)]},
    "cairn": {"joined": datetime(2011, 2, 23, 4, 30), "p": 0.85, "home": ["cairn_farm", "cairn_storehouse_door_2011", "sheep_pen"],
              "wd": [(4.25, 8.0)], "we": [(4.25, 8.5), (14.0, 16.5)],
              "blocks": [(3, 0), (59, 0), (17, 0), (5, 0), (85, 0), (54, 0), (50, 0)]},
    "finchline": {"joined": datetime(2011, 2, 23, 15, 0), "p": 0.6, "home": ["finch_hub", "spawn", "cairn_farm"],
                  "wd": [(15.5, 21.5)], "we": [(10.0, 22.0)],
                  "blocks": [(66, 0), (4, 0), (1, 0), (76, 0), (55, 0), (13, 0)]},
    "juni_lantern": {"joined": datetime(2011, 2, 23, 6, 0), "p": 0.7, "home": ["june_house", "june_tower", "june_old_dock"],
                     "wd": [(5.0, 6.75), (16.5, 20.5)], "we": [(8.0, 21.0)],
                     "blocks": [(20, 0), (12, 0), (4, 0), (5, 0), (50, 0), (24, 0), (35, 0)]},
    "patchcord": {"joined": datetime(2011, 2, 23, 12, 0), "p": 0.55, "home": ["patchcord_relay_hut", "spawn"],
                  "wd": [(12.0, 18.5)], "we": [(6.0, 18.5)],
                  "blocks": [(25, 0), (84, 0), (5, 0), (1, 0), (50, 0), (17, 0)]},
    "kitbag": {"joined": datetime(2011, 2, 26, 19, 40), "p": 0.7, "home": ["spawn", "cairn_storehouse_door_2011", "sheep_pen"],
               "wd": [(15.25, 20.5)], "we": [(9.5, 21.0)],
               "blocks": [(3, 0), (12, 0), (4, 0), (50, 0), (5, 0), (2, 0)]},
}


def _blocked(at: datetime) -> bool:
    d = at.date()
    if d.isoformat() in OFFLINE_DAYS:
        return True
    if RESTORE_PASSES[0] <= d <= RESTORE_PASSES[1] and 2 <= at.hour < 4:
        return True
    return d == date(2011, 5, 18)  # the incident day is written out by hand below


def _incident_day_rows() -> list[dict]:
    """18 May: people mostly stood and watched. A torch, some wheat, nothing much."""
    a = layout.ANCHORS
    return [
        _row(datetime(2011, 5, 18, 4, 3, 44), "rookery", "placed", 50, 0, (a["spawn"][0] + 2, a["spawn"][1], a["spawn"][2] - 1)),
        _row(datetime(2011, 5, 18, 4, 26, 10), "cairn", "broke", 59, 0, (a["cairn_farm"][0] + 3, a["cairn_farm"][1], a["cairn_farm"][2] + 2)),
        _row(datetime(2011, 5, 18, 4, 26, 13), "cairn", "placed", 59, 0, (a["cairn_farm"][0] + 3, a["cairn_farm"][1], a["cairn_farm"][2] + 2)),
        _row(datetime(2011, 5, 18, 4, 12, 31), "kitbag", "placed", 3, 0, (a["spawn"][0] + 41, a["spawn"][1], a["spawn"][2] + 12)),
    ]


def noise_rows() -> list[dict]:
    rng = random.Random(6118)
    rows = []
    day = MAP_START.date()
    while day <= date(2011, 6, 30):
        weekend = day.weekday() >= 5
        quiet = day >= QUIET_FROM
        shaky = date(2011, 4, 23) <= day <= date(2011, 4, 25)  # half the map missing, people poke at it
        for who, h in HABITS.items():
            p = h["p"] * (0.06 if quiet else 0.35 if shaky else 1.0)
            if rng.random() >= p:
                continue
            lo, hi = rng.choice(h["we"] if weekend else h["wd"])
            start = datetime.combine(day, datetime.min.time()) + timedelta(hours=lo + rng.random() * max(hi - lo - 0.3, 0.1))
            n = rng.randint(2, 6) if quiet else rng.randint(3, 11)
            ax, ay, az = layout.ANCHORS[rng.choice(h["home"])]
            x, y, z = ax + rng.randint(-12, 12), ay + rng.randint(-2, 3), az + rng.randint(-12, 12)
            at = start
            for _ in range(n):
                at += timedelta(seconds=rng.randint(3, 240))
                if at < h["joined"] or _blocked(at):
                    continue
                bid, data = rng.choice(h["blocks"])
                if bid == 35:
                    data = rng.choice([0, 0, 4, 11, 14])
                if who == "finchline" and bid == 66 and at >= datetime(2011, 4, 19, 19, 0) and rng.random() < 0.6:
                    bid = POWERED_RAIL
                x, y, z = x + rng.randint(-2, 2), max(58, min(84, y + rng.randint(-1, 1))), z + rng.randint(-2, 2)
                rows.append(_row(at, who, rng.choice(["placed", "placed", "broke"]), bid, data, (x, y, z)))
        day += timedelta(days=1)
    return rows + _incident_day_rows()


def all_rows() -> list[dict]:
    rows = (floor_rows() + id_wall_rows() + tower_rows() + south_rail_rows() + storehouse_tidy_rows()
            + iona_chest_rows() + finch_lock_rows() + noise_rows())
    return sorted(rows, key=lambda r: r["date"])


def rows_for_act(act: int) -> list[dict]:
    """The viewer opens in Act 3. Rows that would hand players a later act's answer stay out until then:
    finch's lock (Act 5), Iona's chest and her block-ID wall (Act 7)."""
    if act < 3:
        return []
    lock = {(r["date"], r["x"], r["y"], r["z"]) for r in finch_lock_rows()}
    wall = {(r["date"], r["x"], r["y"], r["z"]) for r in id_wall_rows()}
    out = []
    for r in all_rows():
        key = (r["date"], r["x"], r["y"], r["z"])
        if r["action"] == "chest_put" and act < 7:
            continue
        if key in wall and act < 7:
            continue
        if key in lock and act < 5:
            continue
        out.append(r)
    return out
