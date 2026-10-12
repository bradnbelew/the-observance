"""Prove the canon is internally consistent.

    python -m morrow.canon.verify

Fails loudly on any broken cipher, any 2011 sign over 15 characters, any wrong-zone
time in a genuine Iona post, a Theo post without his double space, or a Morrow
rewrite that forgot its tell.
"""

from __future__ import annotations

import re
import sys
from datetime import datetime
from zoneinfo import ZoneInfo

from . import ciphers, forum, irc, later, layout, people, records, serverlog

CHI = ZoneInfo("America/Chicago")
FAILURES: list[str] = []


def check(cond: bool, msg: str) -> None:
    print(("  ok   " if cond else "  FAIL ") + msg)
    if not cond:
        FAILURES.append(msg)


def zone_for(day: str) -> str:
    return "CDT" if datetime.fromisoformat(day + " 12:00").replace(tzinfo=CHI).dst() else "CST"


def main() -> int:
    print("ciphers")
    f = ciphers.build_floor()
    check(ciphers.read_floor(f.grid, ciphers.RULES_SIGNS_2011) == ciphers.FLOOR_PLAINTEXT, "C6 floor decodes with the 2011 rules sign")
    bad = ciphers.read_floor(f.grid, ciphers.RULES_SIGNS_MORROW)
    first = next(i for i, (a, b) in enumerate(zip(bad, ciphers.FLOOR_PLAINTEXT)) if a != b) + 1
    check(first == 38, "C6 Morrow's edited sign breaks the plaintext at letter 38 (got %d)" % first)
    check(set(ciphers.to_nibbles(f.ciphertext)[0::2]) <= {4, 5}, "C6 every other wool block is yellow or lime")
    check(len(f.grid) * len(f.grid[0]) == 162, "C6 floor is 18 x 9")
    check(ciphers.read_iona_chest(ciphers.build_iona_chest()) == ciphers.IONA_TOKEN, "C8 Iona's chest decodes")
    check(all(1 <= s["count"] <= 64 for s in ciphers.build_iona_chest()), "C8 every stack is a legal size")
    check(ciphers.read_id_wall(ciphers.build_id_wall()) == ciphers.HALT_COMMAND, "D6 block-ID wall reads HALTMRRW")
    rk = ciphers.build_residents_key()
    check(ciphers.read_residents_key(rk["ciphertext"], rk["key1"], rk["key2"]) == ciphers.RESIDENTS_PLAINTEXT, "D15 Residents' Key decodes")
    check(rk["key1"] == "brackenhingetinbooster" and rk["key2"] == "sambrobaggagebiscuit", "D15 keys follow join order")
    line = ciphers.build_slipped_line()
    check(ciphers.shift_line(line, ciphers.UK_ROWS, -1) == ciphers.PATCHCORD_INTENDED, "D2 slipped line decodes on UK layout")
    check(ciphers.shift_line(line, ciphers.US_ROWS, -1) != ciphers.PATCHCORD_INTENDED, "D2 slipped line does NOT decode on US layout")
    check(ciphers.read_tune(ciphers.TUNE) == ciphers.TUNE_WORD, "C2 bass line spells BAGGAGE")
    check(ciphers.read_braille_shelves(ciphers.build_braille_shelves()) == ciphers.BRAILLE_MESSAGE, "C10 shelves read 'sorry kit'")
    check(all(ciphers.read_motd(ciphers.motd_codes(t)) == t for t in ciphers.MOTD_BY_ACT.values()), "C14 every MOTD decodes")
    check(ciphers.from_nibbles(ciphers.build_light_corridor()) == ciphers.LIGHT_CORRIDOR_MESSAGE, "D13 light corridor decodes")
    check(ciphers.read_sound_replay(ciphers.build_sound_replay()) == ciphers.SOUND_REPLAY_FILENAME, "D11 sound replay decodes")
    check(ciphers.java_string_hash("bracken") == 137407650, "C1 seed 'bracken' hashes to 137407650")
    check(all(c["true_colour"] != c["trap_colour_if_read_as_wool"] for c in ciphers.sheep_census()), "D5 every census colour is a real trap")
    check(layout.region_chunk(*[layout.ANCHORS["iona_notes"][i] for i in (0, 2)]) == "r.-1.0.mcr c(27,14)", "C7 Iona's notes match her IRC region address")
    hub = layout.ANCHORS["nether_hub"]
    room = layout.ANCHORS["rookery_ladder_room"]
    check((hub[0] * 8, hub[2] * 8) == (room[0], room[2]), "C5 nether hub x8 lands on the ladder room")
    check(people.by_handle("rookery").lwc_password == records.CLIENT_ACCOUNT["security_answer"], "Act 1 security answer is Rookery's password")

    pts = [tuple(v) for v in layout.ANCHORS.values()]
    check(len(pts) == len(set(pts)), "no two anchors share a block")
    sx, sy, sz = layout.ANCHORS["rules_signs"]
    spawn_signs = {(sx + i, sy, sz) for i in (0, 1, 2, 4)}
    others = {tuple(v) for k, v in layout.ANCHORS.items() if k != "rules_signs"}
    check(not (spawn_signs & others), "modern anchors don't overwrite the 2011 spawn signs")
    print("2011 writing rules")
    signs = ciphers.RULES_SIGNS_2011 + [s for s in ciphers.build_residents_key()["signs"]]
    check(all(len(l) <= 15 for s in signs for l in s), "every genuine 2011 sign line is 15 characters or fewer")
    check(any(len(l) > 15 for s in ciphers.RULES_SIGNS_MORROW for l in s), "Morrow's sign edit breaks the 15-character limit")
    for t in forum.THREADS:
        for p in t["posts"]:
            if p["author"] == "Theo [Copperline]" and ". " in p["body"].replace(".  ", ""):
                check(False, "Theo post %d has a single space after a full stop" % p["id"])
            if p["author"] == "i_bell":
                for zone in re.findall(r"\b(CDT|CST)\b", p["body"]):
                    check(zone == zone_for(p["at"][:10]), "Iona post %d uses %s correctly" % (p["id"], zone))
    for msg in records.TICKETS["6290"]["messages"]:
        check("CDT" in msg["body"], "Iona's August handover says CDT")
    rewrite = records.STATUS_INCIDENT["morrow_rewrite"]
    check("CST" in rewrite["posted"] and zone_for("2011-05-18") == "CDT", "Morrow's status rewrite has the CST-in-May tell")

    for sign in later.IONA_NOTES_SIGNS + [later.FINCH_SPUR_SIGN, later.NETHER_HUB_SIGN]:
        check(all(len(l) <= 15 for l in sign), "2011 sign fits: %s" % sign[0])
    print("timeline")
    ircs = {d: lines for d, lines in irc.LOG}
    check(any("uptime 04:17:22" in t for _, _, t in ircs["2011-05-18"]), "IRC kill line carries the uptime")
    check(any(e[0].startswith("2011-05-18 06:44:10") and e[1] == "tvale" for e in records.TCADMIN_LOG), "panel log shows Theo's 06:44:10 restart")
    check(any(t == "06:17:22" and "SIGTERM" in m for t, _, m in serverlog.DAYS["2011-05-18"]), "server.log shows the 06:17:22 stop")
    start = datetime(2011, 5, 18, 2, 0, 0)
    check(str(datetime(2011, 5, 18, 6, 17, 22) - start) == "4:17:22", "02:00:00 start + 04:17:22 uptime = 06:17:22")
    june = datetime(2011, 5, 18, 4, 14, 33, tzinfo=CHI).astimezone(ZoneInfo("America/Halifax"))
    check(june.strftime("%Y-%m-%d_%H.%M.%S") == "2011-05-18_06.14.33", "June's screenshot filename is Halifax time")
    print("acts 4-10")
    from .build import corpus
    norm = lambda t: " " + " ".join(re.sub(r"[^a-z0-9' ]+", " ", t.lower()).split()) + " "
    lines = [norm(c["text"]) for c in corpus()]
    for dm in later.MORROW_DMS:
        for part in dm["text"].split(". "):
            p = norm(part)
            check(len(p.split()) >= 3 and any(p in l for l in lines), "Morrow DM fragment is in the corpus: %r" % part.strip())
    for dm in later.THEO_DMS:
        unsourced = [w for w in norm(dm["text"]).split() if not any(" %s " % w in l for l in lines)]
        check(bool(unsourced), "Theo DM has an unsourced word %s" % unsourced[:3])
    for rw in later.SIGN_REWRITES:
        check(any(len(l) > 15 for l in rw["lines"]), "Morrow sign rewrite at %s breaks the 15-char limit" % rw["anchor"])
    for pid, (body, note) in later.FORUM_REWRITES.items():
        check("CST" in note or "CST" in body, "forum rewrite %d carries the CST-in-May tell" % pid)
    book = later.TRANSCRIPTION_BOOKS[0]
    check(any(src.split(", ")[-1][:10] in later.OFFLINE_DAYS for _, src in book["lines"]), "transcription book cites an impossible (offline) day")
    for f in later.CASE_BRIEF_FIELDS:
        check(bool(f["accept"]), "case brief field %s has an answer" % f["id"])
    check(later.backdated_join("Steve") == later.backdated_join("steve"), "backdated join is stable per player")
    print()
    if FAILURES:
        print("%d FAILURES" % len(FAILURES))
        return 1
    print("all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
