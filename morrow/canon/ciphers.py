"""Every cipher in the campaign, as encode + decode pairs.

Each function produces the exact artifact the world, site, or media will carry.
`verify.py` round-trips all of them. Change a plaintext here and every derived
record (LogBlock rows, signs, chest stacks, checklist hashes) follows.
"""

from __future__ import annotations

import base64
import hashlib
import re
from dataclasses import dataclass

from . import people

# ---------------------------------------------------------------------------
# Shared facts

WOOL = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
DYE_TO_WOOL = {d: 15 - d for d in range(16)}  # 2011: dye damage = 15 - wool damage

PRE_FLATTENING_IDS = {  # Java pre-1.13 numeric block IDs 65-90 (A-Z in ASCII)
    65: "ladder", 66: "rail", 67: "stone_stairs", 68: "wall_sign", 69: "lever",
    70: "stone_pressure_plate", 71: "iron_door", 72: "wooden_pressure_plate",
    73: "redstone_ore", 74: "lit_redstone_ore", 75: "unlit_redstone_torch",
    76: "redstone_torch", 77: "stone_button", 78: "snow_layer", 79: "ice", 80: "snow",
    81: "cactus", 82: "clay", 83: "reeds", 84: "jukebox", 85: "fence", 86: "pumpkin",
    87: "netherrack", 88: "soul_sand", 89: "glowstone", 90: "portal",
}
UNPLACEABLE_IDS = {74, 75, 90}  # no stable placed form; messages must avoid J, K, Z


def md5(text: str) -> str:
    return hashlib.md5(text.encode("ascii")).hexdigest()


def letters(text: str) -> str:
    return re.sub("[^a-z]", "", text.lower())


def java_string_hash(text: str) -> int:
    h = 0
    for ch in text:
        h = (31 * h + ord(ch)) & 0xFFFFFFFF
    return h - (1 << 32) if h >= (1 << 31) else h


# ---------------------------------------------------------------------------
# Spawn rules signs (genuine 2011) and Morrow's Act 3 edit

RULES_SIGNS_2011 = [
    ["MOSSFIELD", "rules (pls)", "1 no griefing", "2 no lava"],
    ["3 ask b4 you", "build near ppl", "4 no xray, i", "will know -rook"],
    ["5 leave farms", "replanted", "6 have fun", "or else"],
]
RULES_SIGNS_MORROW = [list(s) for s in RULES_SIGNS_2011]
RULES_SIGNS_MORROW[1][0] = "3 ask before you"  # 16 chars: impossible on a 2011 sign (engine b4 -> before)

# The other genuine 2011 signs. Morrow reads signs, so these are in its corpus too.
# (world/inject.py places most of them; finch's south-rail sign is new: one block east of
# layout.ANCHORS["south_rail_end"], so the track Morrow lays past it never touches it.)
GENUINE_SIGNS_2011 = {
    "spawn_warning": ["screenshot", "stuff. it", "changes. trust", "me -rook"],
    "rookery_door": ["ROOK'S", "boots off", "yes, yours too", ""],
    "finch_rail_start": ["booster line", "RIP", "powered rails", "finally"],
    "finch_south_rail": ["south branch", "not done", "leave it", "-finch"],
    "kit_base_1": ["KIT ONLY", "no theo", "no rook", "no girls"],
    "kit_base_2": ["pw is my dogs", "name", "", ""],
}
FINCH_SOUTH_SIGN_OFFSET = (1, 0, 0)  # from south_rail_end


def rules_key(signs) -> str:
    return letters("".join(line for sign in signs for line in sign))


# ---------------------------------------------------------------------------
# C6 Rookery's floor: LogBlock -> wool -> hex -> boustrophedon -> running-key Vigenere

FLOOR_PLAINTEXT = "STOPNEEDSTHREETHINGSMAINTSIGNSAYSONNOSESSIONSOPENIONASKEYINTHECONSOLEDONTTELLTHEO"
FLOOR_COLS, FLOOR_ROWS = 18, 9
FLOOR_TIDIED_ROWS = (0, 1, 2)  # Morrow sorted these into a gradient


def vigenere(text: str, key: str, decrypt: bool = False) -> str:
    sign = -1 if decrypt else 1
    return "".join(chr((ord(c) - 65 + sign * (ord(k) - 97)) % 26 + 65) for c, k in zip(text, key))


@dataclass
class Floor:
    ciphertext: str
    grid: list[list[int]]  # wool damage values as built in 2011, [row][col] in world order
    tidied: list[list[int]]  # what the world shows after Morrow's repair
    placement_order: list[tuple[int, int, int]]  # (row, col, wool) in the order Rookery placed them


def build_floor() -> Floor:
    key = rules_key(RULES_SIGNS_2011)
    assert len(FLOOR_PLAINTEXT) <= len(key)
    ct = vigenere(FLOOR_PLAINTEXT, key)
    nibbles = [int(h, 16) for h in ct.encode("ascii").hex()]
    assert len(nibbles) == FLOOR_COLS * FLOOR_ROWS
    grid = []
    for r in range(FLOOR_ROWS):
        row = nibbles[r * FLOOR_COLS:(r + 1) * FLOOR_COLS]
        grid.append(row if r % 2 == 0 else list(reversed(row)))  # boustrophedon, like finch's rail
    tidied = [sorted(row) if r in FLOOR_TIDIED_ROWS else list(row) for r, row in enumerate(grid)]
    # Rookery laid the floor in reading order of his rail: row by row, alternating direction.
    order = []
    for r in range(FLOOR_ROWS):
        cols = range(FLOOR_COLS) if r % 2 == 0 else range(FLOOR_COLS - 1, -1, -1)
        order.extend((r, c, grid[r][c]) for c in cols)
    return Floor(ct, grid, tidied, order)


def read_floor(grid: list[list[int]], signs) -> str:
    nibbles = []
    for r, row in enumerate(grid):
        nibbles.extend(row if r % 2 == 0 else list(reversed(row)))
    ct = bytes(int("".join("%x" % n for n in nibbles[i:i + 2]), 16) for i in range(0, len(nibbles), 2)).decode()
    return vigenere(ct, rules_key(signs), decrypt=True)


# ---------------------------------------------------------------------------
# C8 Iona's chest: stack counts are base64 indexes (count - 1)

B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
IONA_TOKEN = "mrrw-watchdog:off:6118:ib:2011-06-02"  # 36 bytes -> 48 base64 chars, no padding
CHEST_ITEMS = ["cobblestone", "dirt", "sand", "gravel", "planks", "torch", "arrow", "wood"]


def build_iona_chest() -> list[dict]:
    encoded = base64.b64encode(IONA_TOKEN.encode("ascii")).decode()
    assert "=" not in encoded and len(encoded) <= 54
    slots = []
    for i, ch in enumerate(encoded):
        slots.append({"slot": i, "item": CHEST_ITEMS[(i * 5 + 3) % len(CHEST_ITEMS)], "count": B64.index(ch) + 1})
    return slots


def read_iona_chest(slots: list[dict]) -> str:
    s = "".join(B64[slot["count"] - 1] for slot in sorted(slots, key=lambda x: x["slot"]))
    return base64.b64decode(s).decode("ascii")


# ---------------------------------------------------------------------------
# D6 Iona's block-ID wall: HALTMRRW

HALT_COMMAND = "HALTMRRW"


def build_id_wall() -> list[dict]:
    wall = []
    for ch in HALT_COMMAND:
        block_id = ord(ch)
        assert block_id in PRE_FLATTENING_IDS and block_id not in UNPLACEABLE_IDS, ch
        wall.append({"id": block_id, "block": PRE_FLATTENING_IDS[block_id]})
    return wall


def read_id_wall(wall: list[dict]) -> str:
    return "".join(chr(w["id"]) for w in wall)


# ---------------------------------------------------------------------------
# D15 Residents' Key: double columnar transposition keyed by everyone's password

RESIDENTS_PLAINTEXT = ("THECLEANCOPYISINMYFTPFOLDERNOTRECOVERYDATEDAPRILTWENTY"
                       "THEODOESNTKNOWITSTHERESORRYGUYSROOK")
SIGN_LINE = 15


def column_order(key: str) -> list[int]:
    return [i for _, i in sorted((ch, i) for i, ch in enumerate(key))]


def columnar_encrypt(text: str, key: str) -> str:
    cols = len(key)
    return "".join(text[c::cols] for c in column_order(key))


def columnar_decrypt(text: str, key: str) -> str:
    cols, n = len(key), len(text)
    rows, extra = divmod(n, cols)
    lengths = [rows + (1 if c < extra else 0) for c in range(cols)]
    columns, pos = {}, 0
    for c in column_order(key):
        columns[c] = text[pos:pos + lengths[c]]
        pos += lengths[c]
    return "".join(columns[c][r] for r in range(rows + 1) for c in range(cols) if r < len(columns[c]))


def residents_keys() -> tuple[str, str]:
    pw = [r.lwc_password for r in people.join_order()]
    return "".join(pw[:3]), "".join(pw[3:])


def build_residents_key() -> dict:
    k1, k2 = residents_keys()
    ct = columnar_encrypt(columnar_encrypt(RESIDENTS_PLAINTEXT, k1), k2)
    lines = [ct[i:i + SIGN_LINE] for i in range(0, len(ct), SIGN_LINE)]
    preface = [["key: all of us", "in the order we", "showed up", "-rook"],
               ["us in 2 halves", "one pass each", "by columns.", "2 shuffles"]]
    body = [lines[i:i + 4] + [""] * (4 - len(lines[i:i + 4])) for i in range(0, len(lines), 4)]
    return {"key1": k1, "key2": k2, "ciphertext": ct, "signs": preface + body}


def read_residents_key(ct: str, k1: str, k2: str) -> str:
    return columnar_decrypt(columnar_decrypt(ct, k2), k1)


# ---------------------------------------------------------------------------
# D2 patchcord's slipped hands: UK QWERTY, one key to the right

UK_ROWS = ["`1234567890-=", "qwertyuiop[]", "asdfghjkl;'#", "\\zxcvbnm,./"]
US_ROWS = ["`1234567890-=", "qwertyuiop[]\\", "asdfghjkl;'", "zxcvbnm,./"]
PATCHCORD_INTENDED = "putting it on rook's ftp"


def _neighbour(rows, ch, step):
    for row in rows:
        i = row.find(ch)
        if i >= 0 and 0 <= i + step < len(row):
            return row[i + step]
    return None


def shift_line(text: str, rows, step: int) -> str | None:
    out = []
    for ch in text:
        if ch == " ":
            out.append(ch)
            continue
        n = _neighbour(rows, ch, step)
        if n is None:
            return None
        out.append(n)
    return "".join(out)


def build_slipped_line() -> str:
    line = shift_line(PATCHCORD_INTENDED, UK_ROWS, +1)
    assert line is not None
    return line


# ---------------------------------------------------------------------------
# C2 patchcord's tune: note block pitch -> note name; the bass line spells BAGGAGE

NOTE_NAMES = ["F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F"] * 2 + ["F#"]
TUNE_WORD = "BAGGAGE"
TUNE = [  # (pitch clicks, block underneath) -- wood = bass (the word), stone = drum (rhythm decoy)
    (5, "planks"), (8, "stone"), (3, "planks"), (1, "planks"), (8, "stone"), (13, "planks"),
    (15, "planks"), (8, "stone"), (13, "planks"), (10, "planks"), (8, "stone"),
]


def read_tune(tune) -> str:
    return "".join(NOTE_NAMES[p] for p, base in tune if base == "planks")


# ---------------------------------------------------------------------------
# C10 Theo's reading room: chiseled bookshelves are Braille cells turned on their side

BRAILLE = {  # Grade 1 letters, dots 1-6
    "a": {1}, "b": {1, 2}, "c": {1, 4}, "d": {1, 4, 5}, "e": {1, 5}, "f": {1, 2, 4},
    "g": {1, 2, 4, 5}, "h": {1, 2, 5}, "i": {2, 4}, "j": {2, 4, 5}, "k": {1, 3},
    "l": {1, 2, 3}, "m": {1, 3, 4}, "n": {1, 3, 4, 5}, "o": {1, 3, 5}, "p": {1, 2, 3, 4},
    "q": {1, 2, 3, 4, 5}, "r": {1, 2, 3, 5}, "s": {2, 3, 4}, "t": {2, 3, 4, 5},
    "u": {1, 3, 6}, "v": {1, 2, 3, 6}, "w": {2, 4, 5, 6}, "x": {1, 3, 4, 6},
    "y": {1, 3, 4, 5, 6}, "z": {1, 3, 5, 6}, " ": set(),
}
# Rotating the 3x2 cell a quarter-turn clockwise onto the shelf's 2x3 slots (0-2 top row L-R).
DOT_TO_SLOT = {3: 0, 2: 1, 1: 2, 6: 3, 5: 4, 4: 5}
BRAILLE_MESSAGE = "sorry kit"


def build_braille_shelves() -> list[list[bool]]:
    return [[slot in {DOT_TO_SLOT[d] for d in BRAILLE[ch]} for slot in range(6)] for ch in BRAILLE_MESSAGE]


def read_braille_shelves(shelves) -> str:
    inverse = {frozenset(v): k for k, v in BRAILLE.items()}
    slot_to_dot = {s: d for d, s in DOT_TO_SLOT.items()}
    return "".join(inverse[frozenset(slot_to_dot[i] for i, full in enumerate(sh) if full)] for sh in shelves)


# ---------------------------------------------------------------------------
# Morrow's nibble channels: MOTD colour codes, light corridor, sound replay

def to_nibbles(text: str) -> list[int]:
    return [int(h, 16) for h in text.encode("ascii").hex()]


def from_nibbles(nibbles: list[int]) -> str:
    return bytes(nibbles[i] * 16 + nibbles[i + 1] for i in range(0, len(nibbles), 2)).decode("ascii")


MOTD_BY_ACT = {1: "queue 409/413", 2: "queue 410/413", 3: "queue 411/413", 4: "queue 411/413",
               5: "queue 412/413", 6: "queue 412/413", 7: "queue 412/413", 8: "queue 412/413",
               9: "queue 412/413", 10: "skip"}


def motd_codes(text: str) -> str:
    """Each byte becomes two coloured block glyphs; the § codes carry the hex."""
    return "".join("§%x\u258c" % n for n in to_nibbles(text))


def read_motd(raw: str) -> str:
    return from_nibbles([int(c, 16) for c in re.findall("§([0-9a-f])", raw)])


LIGHT_CORRIDOR_MESSAGE = "SESSIONS:1:LOWPATH"


def build_light_corridor() -> list[int]:
    return to_nibbles(LIGHT_CORRIDOR_MESSAGE)  # one floor tile per nibble, block light 0-15


SOUND_REPLAY_FILENAME = "MIC-5-18-B.WAV"
VIBRATION_EVENTS = {  # frequency -> the sound Morrow replays (Java values to be re-verified in game)
    1: "step", 2: "hit_ground", 3: "item_interact_finish", 4: "unequip", 5: "equip",
    6: "entity_interact", 7: "entity_damage", 8: "eat", 9: "container_close", 10: "container_open",
    11: "block_change", 12: "block_destroy", 13: "block_place", 14: "entity_place", 15: "entity_die",
}


def build_sound_replay() -> list[dict]:
    nibbles = to_nibbles(SOUND_REPLAY_FILENAME)
    assert 0 not in nibbles, "frequency 0 does not exist; pick characters with non-zero nibbles"
    return [{"frequency": n, "event": VIBRATION_EVENTS[n]} for n in nibbles]


def read_sound_replay(events: list[dict]) -> str:
    return from_nibbles([e["frequency"] for e in events])


# ---------------------------------------------------------------------------
# D5 cairn's sheep census: dye IDs, not wool IDs

def sheep_census() -> list[dict]:
    rows = []
    for r in people.RESIDENTS:
        wool = DYE_TO_WOOL[r.sheep_dye]
        rows.append({"handle": r.handle, "forum_note": "351:%d" % r.sheep_dye,
                     "true_colour": WOOL[wool], "trap_colour_if_read_as_wool": WOOL[r.sheep_dye]})
    return rows


# ---------------------------------------------------------------------------
# D12 Morrow's presence register: one door per resident, join order

def door_register(act: int) -> dict:
    order = [r.handle for r in people.join_order()] + ["lowpath"]
    home = {"rookery": False, "cairn": False, "finchline": False, "juni_lantern": False,
            "patchcord": False, "kitbag": False, "lowpath": False}
    if act >= 3:
        home["cairn"] = True  # Morrow decides cairn "came back" once players restore his habit
    if act >= 8:
        home["kitbag"] = True  # after players stand in Kit's fossil base
    return {"order": order, "open": [home[h] for h in order],
            "bits": "".join("1" if home[h] else "0" for h in order),
            "players_doors_open": act >= 9, "lowpath_open": act >= 10}


# ---------------------------------------------------------------------------
# cairn's storehouse tally (genuine 2011): one chest per resident (join order),
# one stack per week, count = crates returned. Morrow's "recovered" book adds lowpath.

TALLY_WEEKS = ["2011-04-25", "2011-05-02", "2011-05-09", "2011-05-16"]
TALLY = {  # crates returned per week; 0 = no stack that week (the gaps are the point)
    "rookery": [6, 9, 4, 11], "cairn": [31, 28, 33, 26], "finchline": [3, 0, 7, 5],
    "juni_lantern": [12, 10, 14, 9], "patchcord": [0, 2, 0, 4], "kitbag": [1, 0, 0, 3],
}
TALLY_ITEM = "planks"  # cairn counted crates as planks stacks


def tally_book_morrow() -> list[str]:
    """Morrow's 'Storehouse tally (recovered)' book: tidy, complete, and one row too many."""
    rows = ["%s: %s" % (h, " ".join(str(n) for n in TALLY[h])) for h in TALLY]
    rows.append("lowpath: 2 3 2 3")
    return rows


# cairn's sheep census pens for the week of 16 May (inner = online that week)
SHEEP_INNER = ["rookery", "cairn", "juni_lantern", "kitbag"]
