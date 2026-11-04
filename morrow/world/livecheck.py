"""Read the cipher objects back from the LIVE Paper test server and drive it through the acts.

    python -m morrow.world.livecheck world        # every cipher object as a player sees it, decoded to the canon
    python -m morrow.world.livecheck acts         # /mrrw act 2..9 with checks on what Morrow changes
    python -m morrow.world.livecheck rcon <cmd>   # one RCON command (the test server: 127.0.0.1:25598, mrrwtest)

The test server (build/mrrw-v2-test, morrow/v2/TEST-NOW.md) must be running with the plugin installed.
"world" force-loads the town first (no player online means no chunks loaded). The checks read the
world the way a player reads it: the floor from the ladder landing nearest row first, the note wall
and the ID wall facing south left to right, Iona's double chest in the order the client lists it.
"""

from __future__ import annotations

import json
import os
import re
import socket
import struct
import sys
import time

from morrow.canon import ciphers, layout, logblock

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
HOST, PORT, PASSWORD = "127.0.0.1", 25598, "mrrwtest"
PLUGIN_DIR = os.path.join(ROOT, "build", "mrrw-v2-test", "plugins", "mrrw")
A = layout.ANCHORS
FAILS: list[str] = []


# ---- RCON -----------------------------------------------------------------------------------------

def _packet(pid: int, ptype: int, body: str) -> bytes:
    data = struct.pack("<ii", pid, ptype) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(data)) + data


def _read(sock: socket.socket) -> tuple[int, int, str]:
    raw = b""
    while len(raw) < 4:
        raw += sock.recv(4 - len(raw))
    (length,) = struct.unpack("<i", raw)
    data = b""
    while len(data) < length:
        data += sock.recv(length - len(data))
    pid, ptype = struct.unpack("<ii", data[:8])
    return pid, ptype, data[8:-2].decode("utf-8", "replace")


def rcon(cmd: str, timeout: float = 10) -> str:
    s = socket.create_connection((HOST, PORT), timeout=timeout)
    s.sendall(_packet(1, 3, PASSWORD))
    pid, _, _ = _read(s)
    if pid == -1:
        raise SystemExit("rcon: bad password")
    s.sendall(_packet(2, 2, cmd))
    out = ""
    s.settimeout(timeout)
    try:
        while True:
            _, _, body = _read(s)
            out += body
            if len(body) < 4000:
                break
    except socket.timeout:
        pass
    s.close()
    return out


def check(ok: bool, msg: str) -> None:
    print(("  ok   " if ok else "  FAIL ") + msg)
    if not ok:
        FAILS.append(msg)


def is_block(x, y, z, mat: str) -> bool:
    return "passed" in rcon(f"execute if block {x} {y} {z} minecraft:{mat}").lower()


WOOL_NAMES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
              "purple", "blue", "brown", "green", "red", "black"]


def wool_at(x, y, z) -> int | None:
    for i, n in enumerate(WOOL_NAMES):
        if is_block(x, y, z, n + "_wool"):
            return i
    return None


def scalar(path_cmd: str) -> str | None:
    out = rcon(path_cmd)
    m = re.search(r"has the following block data: (.*)$", out.strip())
    return m.group(1) if m else None


def sign_lines(x, y, z, side="front") -> list[str]:
    return re.findall(r'"([^"]*)"', rcon(f"data get block {x} {y} {z} {side}_text.messages"))


def forceload_town() -> None:
    for a in ("-300 -180 -50 -61", "-300 -60 -50 59", "-300 60 -50 179", "-300 180 -50 260"):
        rcon("forceload add " + a)


def journal(kind: str) -> list[dict]:
    p = os.path.join(PLUGIN_DIR, "journal.jsonl")
    out = []
    if not os.path.exists(p):
        return out
    for line in open(p, encoding="utf-8"):
        try:
            o = json.loads(line)
        except ValueError:
            continue
        if o.get("kind") == kind:
            out.append(o)
    return out


def feed(kind: str = "recovery") -> list[str]:
    p = os.path.join(PLUGIN_DIR, "feed.jsonl")
    out = []
    if not os.path.exists(p):
        return out
    for line in open(p, encoding="utf-8"):
        try:
            o = json.loads(line)
        except ValueError:
            continue
        if o.get("kind") == kind:
            out.append(o.get("text") or "")
    return out


# ---- the world as a player reads it ---------------------------------------------------------------

def world() -> int:
    forceload_town()
    print("floor (from the ladder landing, nearest row first, boustrophedon)")
    fx, fy, fz = A["rookery_cellar_floor"]
    grid = [[wool_at(fx + c, fy, fz + layout.floor_row_z(r)) for c in range(ciphers.FLOOR_COLS)] for r in range(ciphers.FLOOR_ROWS)]
    check(all(v is not None for row in grid for v in row), "162 wool blocks found")
    original, tidied = ciphers.build_floor().grid, ciphers.build_floor().tidied
    check(grid[3:] == original[3:], "rows 4-9 are Rook's original")
    check(grid[:3] in (original[:3], tidied[:3]), "rows 1-3 are original (before Act 5) or Morrow's colour run")
    rebuilt = [row[:] for row in grid]
    rebuilt[:3] = original[:3]
    check(ciphers.read_floor(rebuilt, ciphers.RULES_SIGNS_2011) == ciphers.FLOOR_PLAINTEXT, "floor decodes (with LogBlock's rows for any tidied ones)")
    checklist = open(os.path.join(ROOT, "dashboard", "private-media", "files", "6118-checklist.txt"), encoding="utf-8").read()
    check(ciphers.md5(ciphers.FLOOR_PLAINTEXT) in checklist, "plaintext A hashes to the checklist")

    print("Iona's ID wall (facing south, left to right)")
    ax, ay, az = A["maintenance_annex"]
    lx, ly, lz = layout.ID_WALL_LEFT
    ids = {65: "ladder", 66: "rail", 69: "lever", 70: "stone_pressure_plate", 71: "iron_door", 72: "oak_pressure_plate",
           76: "redstone_torch", 77: "stone_button", 82: "clay", 84: "jukebox", 85: "oak_fence", 86: "pumpkin", 87: "netherrack"}
    got = []
    for i in range(8):
        x, y, z = layout.wall_run("south", (ax + lx, ay + ly, az + lz), i)
        got.append(next((bid for bid, m in ids.items() if is_block(x, y, z, m)), None))
    check(got == [ord(c) for c in ciphers.HALT_COMMAND], f"ID wall reads HALTMRRW: {got}")

    print("patch's notes (facing south, left to right)")
    px, py, pz = A["patchcord_relay_hut"]
    nl = layout.PATCH_NOTE_LEFT
    tune = []
    for i in range(len(ciphers.TUNE)):
        x, y, z = layout.wall_run("south", (px + nl[0], py + nl[1], pz + nl[2]), i)
        pitch = next((n for n in range(25) if is_block(x, y, z, f"note_block[note={n}]")), -1)
        tune.append((pitch, "planks" if is_block(x, y - 1, z, "oak_planks") else "stone"))
    check(ciphers.read_tune(tune) == ciphers.TUNE_WORD, f"notes spell BAGGAGE from the door: {tune}")

    print("the Residents' Key (facing south)")
    lx2, ly2, lz2 = A["rookery_ladder_room"]
    kl = layout.RESIDENTS_KEY_LEFT
    texts = [sign_lines(*layout.wall_run("south", (lx2 + kl[0], ly2 + kl[1], lz2 + kl[2]), i)) for i in range(4)]
    ct = "".join(l for s in texts[2:] for l in s)
    k1, k2 = ciphers.residents_keys()
    check(ciphers.read_residents_key(ct, k1, k2) == ciphers.RESIDENTS_PLAINTEXT, "Residents' Key decodes from the signs as read")

    print("Iona's chest (in the order the client lists it)")
    kx, ky, kz = A["iona_key_chest"]
    right_first = is_block(kx, ky, kz, "chest[type=right]")   # the client lists the chest's right half first
    halves = ((0, kx), (27, kx + 1)) if right_first else ((0, kx + 1), (27, kx))
    counts = {}
    for half, x in halves:
        for i in range(27):
            s = scalar(f"data get block {x} {ky} {kz} Items[{i}].Slot")
            c = scalar(f"data get block {x} {ky} {kz} Items[{i}].count")
            if s and c:
                counts[int(s.rstrip("b")) + half] = int(c)
    slots = [{"slot": k, "count": v} for k, v in sorted(counts.items())]
    check(len(slots) == 48 and all(v >= 2 for v in counts.values()), f"48 stacks, none of 1 ({len(slots)})")
    try:
        check(ciphers.read_iona_chest(slots) == ciphers.IONA_TOKEN, "the chest decodes to Iona's key, read top to bottom")
    except Exception as e:  # noqa: BLE001
        check(False, f"chest decode: {e}")

    print("Morrow's completions and the lived-in extras")
    dx, dy, dz = A["june_old_dock"]
    check(not is_block(dx + 3, dy, dz, "oak_planks") and is_block(dx + 1, dy, dz, "sandstone"), "June's dock is gone; a sandstone apron begins where it stood")
    jx, jy, jz = A["cairn_storehouse_jog"]
    check(is_block(jx, jy, jz, "air") and is_block(jx + 1, jy, jz - 1, "oak_planks"), "the barn corner is squared")
    hx, hy, hz = A["cairn_shed_hatch"]
    check(is_block(hx, hy, hz, "oak_trapdoor"), "the shed hatch is an oak trapdoor (the tell)")
    for key, (off, _on) in logblock.JUNCTION_LEVERS.items():
        x, y, z = A[key]
        check(is_block(x, y, z, "rail") and is_block(x + off[0], y + off[1], z + off[2], "lever"), f"{key}: a junction rail and its lever")
    tx, ty, tz = A["june_tower"]
    reps = sum(1 for ddx in range(-3, 4) for ddz in range(-11, -3) for ddy in range(-4, 7) if is_block(tx + ddx, ty + ddy, tz + ddz, "repeater"))
    check(reps == 6, f"six repeaters in finch's clock in June's yard ({reps})")
    kx3, ky3, kz3 = A["spawn_kit_chest"]
    n = sum(1 for i in range(27) if scalar(f"data get block {kx3} {ky3} {kz3} Items[{i}].Slot"))
    check(n == 9, f"Rook's newbie chest shows the nine kit items ({n})")
    mx, my, mz = A["maint_sign"]
    check(sign_lines(mx, my, mz)[:1] in (["MAINT OFF"], ["MAINT ON"]), "the MAINT sign stands at spawn")
    return done("live world")


# ---- the acts -------------------------------------------------------------------------------------

def acts() -> int:
    forceload_town()
    rx, ry, rz = A["rules_signs"]
    print("act 2")
    rcon("mrrw act 2")
    time.sleep(2)
    check(sign_lines(rx, ry, rz, "back")[:2] == ["rules:", "1 line normalised"], "Morrow's note on the back of the rules sign")
    fsx, fsy, fsz = A["rookery_floor_sign"]
    check(sign_lines(fsx, fsy, fsz, "back") in ([], ["", "", "", ""]), "the floor sign's back is blank until the tidy")
    print("act 3")
    rcon("mrrw act 3")
    time.sleep(3)
    check(sign_lines(rx + 1, ry, rz)[0] == "3 ask before you", "rules sign 2 line 1 reads 'before' (16 chars)")
    cx, cy, cz = A["spawn_creeper"]
    check(any(is_block(cx + 1, cy + 7, cz, m) for m in ("green_wool", "black_wool")), "Morrow gave the creeper its top rows")
    check(any("24 blocks restored (spawn)" in t for t in feed()), "'24 blocks restored (spawn)' went out")
    bx, by, bz = A["cairn_storehouse_door_2011"]
    check(is_block(bx + 4, by, bz + 1, "lectern"), "the tally book's lectern (beside the routine fixtures)")
    tx, ty, tz = A["june_tower"]
    check(is_block(tx, ty + 22, tz, "redstone_lamp"), "June's lamp")
    print("act 5")
    rcon("mrrw act 5")
    rcon("time set 13000")
    before = len(feed())
    rcon("mrrw replay")
    time.sleep(5)
    check(any("debug view: replay 2011-05-18 04:00" in t for t in feed()[before:]), "the replay's cue line")
    m = re.search(r"Count: (\d+)", rcon("execute if entity @e[type=minecraft:mannequin]"))
    check(m is not None and int(m.group(1)) >= 6, f"the six figures are walking ({m.group(1) if m else 0} mannequins)")
    print("act 6")
    rcon("mrrw act 6")
    rcon("time set 18100")
    time.sleep(8)
    ox, oy, oz = A["spawn_board"]
    check(sign_lines(ox + 1, oy - 1, oz)[:2] == ["the spur still", "isn't done, don't ask"], "the board's spur note is rewritten (over 15 chars)")
    lx, ly, lz = A["rookery_changes_wall"]
    check(sign_lines(lx, ly - 1, lz - 2)[:2] == ["its back on again", "im done with it"], "Rook's ledger's last line is rewritten")
    check(sign_lines(lx, ly, lz)[0] == "5/19 june: roof", "the ledger's first line is untouched")
    print("act 8")
    rcon("mrrw act 8")
    mx, my, mz = A["maint_sign"]
    rcon(f'data merge block {mx} {my} {mz} {{front_text:{{messages:["MAINT ON","","",""]}}}}')
    rcon("time set 100")
    rcon("mrrw dawn")
    time.sleep(4)
    check(sign_lines(mx, my, mz)[0] == "MAINT OFF", "an unwaxed MAINT ON is reverted at dawn")
    check(any("1 sign normalised (spawn)" in t for t in feed()), "the revert is announced")
    print("act 9")
    rcon("mrrw act 9")
    check("act 9" in rcon("mrrw status"), "act 9")
    return done("acts")


def done(what: str) -> int:
    print()
    print(f"all {what} checks passed" if not FAILS else "%d FAILURES" % len(FAILS))
    return 1 if FAILS else 0


def main(argv: list[str]) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2
    if argv[1] == "world":
        return world()
    if argv[1] == "acts":
        return acts()
    if argv[1] == "rcon":
        print(rcon(" ".join(argv[2:])))
        return 0
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))
