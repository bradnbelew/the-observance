"""Prove the canon is internally consistent.

    python -m morrow.canon.verify

Fails loudly on any broken cipher, any 2011 sign over 15 characters, any wrong-zone
time in a genuine Iona post, a Theo post without his double space, or a Morrow
rewrite that forgot its tell.
"""

from __future__ import annotations

import re
import sys
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

from . import ciphers, forum, irc, later, layout, people, records, serverlog

ROOT = Path(__file__).resolve().parents[2]
CHI = ZoneInfo("America/Chicago")
FAILURES: list[str] = []


def check(cond: bool, msg: str) -> None:
    print(("  ok   " if cond else "  FAIL ") + msg)
    if not cond:
        FAILURES.append(msg)


def zone_for(day: str) -> str:
    return "CDT" if datetime.fromisoformat(day + " 12:00").replace(tzinfo=CHI).dst() else "CST"


def collage_ok(words: list[str], lines: list[str], template=lambda w: False, min_len: int = 2) -> bool:
    """Can the words be cut into consecutive fragments (min_len+ words) that each appear in the corpus?
    Template tokens (numbers, Morrow's CST stamp) may stand alone."""
    n = len(words)
    ok = [False] * (n + 1)
    ok[0] = True
    for i in range(n):
        if not ok[i]:
            continue
        if template(words[i]):
            ok[i + 1] = True
        for j in range(i + 1, n + 1):
            frag = " " + " ".join(words[i:j]) + " "
            if not any(frag in l for l in lines):
                break
            if j - i >= min_len:
                ok[j] = True
    return ok[n]


def corpus_law(lines: list[str], vocab: set[str], norm) -> None:
    """Every string Morrow writes obeys the law it teaches: chat is a collage of real fragments,
    engine output is Iona's templates plus words it has read, and nothing else."""
    handles = {r.handle for r in people.RESIDENTS} | {"lowpath", "morrow", "mrrw", "i_bell", "ibell", "theo"}
    handle_words = {w for h in handles for w in norm(h).split()}
    stamp = lambda w: w.isdigit() or w == "cst"
    voicemail = norm(later.VOICEMAIL_2011)
    for item in later.morrow_written():
        text, where = item["text"], item["where"]
        if item["kind"] == "chat":
            words = norm(text).split()
            check(collage_ok(words, lines, stamp), "corpus law: %s is a collage of real lines: %r" % (where, text))
        elif item["kind"] == "engine":
            bad = [w for w in norm(text).split()
                   if not (w.isdigit() or w in vocab or w in later.ENGINE_VOCAB or w in handle_words)]
            check(not bad, "corpus law: %s uses only templates and read words %s" % (where, bad))
        elif item["kind"] == "voicemail":
            parts = [p for p in re.split(r"\.\s+", text) if p.strip()]
            check(all(norm(p) in voicemail for p in parts), "corpus law: %s is cut from the 2011 voicemail" % where)
        elif item["kind"] == "normalised":
            old, new = text.split(" => ")
            fixed = " ".join(later.NORMALISE.get(w, w) for w in old.split())
            check(fixed == new and old != new, "corpus law: %s is only a template normalisation" % where)


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
    notes_at = layout.region_chunk(*[layout.ANCHORS["iona_notes"][i] for i in (0, 2)])
    check(notes_at in repr(irc.LOG), "C7 Iona's notes match her IRC region address (%s)" % notes_at)
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
    vocab = {w for l in lines for w in l.split()}
    for dm in later.THEO_DMS:
        unsourced = [w for w in norm(dm["text"]).split() if not any(" %s " % w in l for l in lines)]
        tells = [w for w in unsourced if w in later.THEO_TELL_WORDS]
        check(bool(tells), "Theo DM has an unsourced tell word %s" % tells)
        check(". " not in dm["text"].replace(".  ", ""), "Theo DM keeps his double spaces")
    check(not any(w in vocab for w in later.THEO_TELL_WORDS), "Theo's tell words appear nowhere in Morrow's corpus")
    check(not any(" turn it off " in l for l in lines), "'turn it off' has no source before Kit's email (Act 8)")
    print("corpus law (everything Morrow writes)")
    corpus_law(lines, vocab, norm)
    for rw in later.SIGN_REWRITES:
        check(any(len(l) > 15 for l in rw["lines"]), "Morrow sign rewrite at %s breaks the 15-char limit" % rw["anchor"])
    for pid, (body, note) in later.FORUM_REWRITES.items():
        check("CST" in note or "CST" in body, "forum rewrite %d carries the CST-in-May tell" % pid)
    book = later.TRANSCRIPTION_BOOKS[0]
    check(any(src.split(", ")[-1][:10] in later.OFFLINE_DAYS for _, src in book["lines"]), "transcription book cites an impossible (offline) day")
    for f in later.CASE_BRIEF_FIELDS:
        check(bool(f["accept"]), "case brief field %s has an answer" % f["id"])
    check(later.backdated_join("Steve") == later.backdated_join("steve"), "backdated join is stable per player")
    theo_text = ([r["reply"] for r in later.DESK_RULES] + [r["partial"] for r in later.DESK_RULES if r["partial"]]
                 + [v[1] for v in later.DESK_FILES.values()] + [d[3] for d in later.DESK_DROPS]
                 + [h for hs in later.HINTS.values() for h in hs] + [v[0] for v in later.USB_CHOICES.values()]
                 + [t for t, _v in later.BRIEF_REPLIES.values()]
                 + [later.USB_VOTE[k] for k in ("prompt", "ack", "residents_only", "closed", "undecided")]
                 + [o["cost"] for o in later.USB_VOTE["options"].values()])
    for t in theo_text:
        check(". " not in t.replace(".  ", "") and "mate" not in t.lower().split(), "2026 Theo: double spaces, no 'mate': %r" % t[:40])
    check(all(len(h) <= len(later.HINT_HOURS) for h in later.HINTS.values()), "every hint has a release time")
    check(not any(t in ("its ok", "it's ok") for r in later.DESK_RULES for t in r["tokens"]), "'its ok' is not a desk trigger")
    check("2014-12-14" in forum.MEMBERS["archivist"]["joined"]
          and forum.MEMBERS["archivist"]["posts"] == len(forum.ARCHIVIST_NOTES), "archivist joined at the first heartbeat, posts = notes")

    print("forum")
    posts = sorted((p["at"], p["id"]) for t in forum.THREADS for p in t["posts"])
    check([i for _, i in posts] == sorted(i for _, i in posts), "post IDs rise with time (%d posts)" % len(posts))
    firsts = sorted((t["posts"][0]["at"], t["id"]) for t in forum.THREADS)
    check([i for _, i in firsts] == sorted(i for _, i in firsts), "thread IDs rise with time (%d threads)" % len(firsts))
    check(len(forum.THREADS) >= 30, "the archive feels like a forum: %d threads" % len(forum.THREADS))
    for h, m in forum.MEMBERS.items():
        if h in people.STAFF or h in ("Theo [Copperline]", "archivist"):
            continue
        check(m["title"] == forum.user_title(m["posts"]), "%s's user title follows the post-count ladder" % h)
    msgs = records.TICKETS["6118"]["messages"]
    check([m["at"] for m in msgs] == sorted(m["at"] for m in msgs), "ticket 6118 messages are in time order")
    for key, sign in ciphers.GENUINE_SIGNS_2011.items():
        check(all(len(l) <= 15 for l in sign), "2011 sign fits: %s" % key)

    print("18 May 2011, minute by minute")
    from . import logblock, media
    rows = logblock.all_rows()
    at = lambda s: datetime.fromisoformat("2011-05-18 " + s)
    glass = sorted(datetime.fromisoformat(r["date"]) for r in logblock.tower_rows() if r["block"] == str(logblock.GLASS))
    glow = [datetime.fromisoformat(r["date"]) for r in logblock.tower_rows() if r["block"] == str(logblock.GLOWSTONE)][0]
    check(glass[0] < at("04:12:09"), "first glass is down before June says 'something is putting glass on my tower'")
    check(sum(g <= at("04:12:40") for g in glass) in (4, 5), "June's 04:12:40 shot: roof half placed")
    check(sum(g <= at("04:14:33") for g in glass) in (7, 8) and glow < at("04:14:33"), "June's 04:14:33 shot: nearly done, glowstone in")
    check(glass[-1] < at("04:16:02"), "roof complete before 'it put a roof on it' (04:16:02) and the 04:16:20 shot")
    rec_start = at("05:58:02")
    rs = (ROOT / "morrow" / "v2" / "RECORDING-SCRIPTS.md").read_text(encoding="utf-8")
    m = re.search(r"\[(\d\d):(\d\d)\] IONA: Done\. It's down\.", rs)
    check(bool(m) and rec_start.replace(second=0) + timedelta(minutes=int(m[1]), seconds=int(m[2])) == at("06:17:22"),
          "moss_0518: 'Done. It's down.' lands on the 06:17:22 kill")
    stamps = [int(a) * 60 + int(b) for a, b in re.findall(r"^\[(\d\d):(\d\d)\]", rs.split("## 2.")[0], re.M)]
    check(stamps == sorted(stamps) and rec_start + timedelta(seconds=stamps[-1]) < at("06:20:59"),
          "moss_0518 ends before patchcord quits (06:20:59)")
    june_bye = [int(a) * 60 + int(b) for a, b in re.findall(r"^\[(\d\d):(\d\d)\] JUNE: logging off", rs, re.M)]
    check(bool(june_bye) and rec_start + timedelta(seconds=june_bye[0]) < at("06:18:05"), "June says she's logging off before she quits")
    check(any(e[0].startswith("2011-05-18 11:14:02") and e[3] == "STOP" for e in records.TCADMIN_LOG), "Iona stops it again at 11:14 (Theo's 05-20 'it's off now' is true)")
    check(any(e[1] == "tvale" and "debug_view=on" in e[3] and e[0] < "2011-05-18 04:09" for e in records.TCADMIN_LOG), "the panel shows who turned the debug view on")
    check(any("watchdog @reboot" in msg for _, _, msg in serverlog.DAYS["2011-09-02"]), "server.log shows the watchdog bringing it back on 2 Sept")
    logins = {}
    for t, _l, msg in serverlog.DAYS["2011-05-18"]:
        w = msg.split(" ", 1)[0]
        if " logged in " in msg:
            logins[w] = [t, "23:59:59"]
        elif "lost connection" in msg:
            logins[w][1] = t
    for r in rows:
        if r["date"].startswith("2011-05-18") and r["player"] in logins:
            lo, hi = logins[r["player"]]
            check(lo <= r["date"][11:] <= hi, "18 May LogBlock row by %s sits inside their session" % r["player"])
    joined = {p.handle: p.joined for p in people.RESIDENTS}
    check(all(r["date"][:10] not in later.OFFLINE_DAYS for r in rows), "no LogBlock rows while the server was broken")
    check(all(not (r["player"] in joined and "2011-04-26" <= r["date"][:10] <= "2011-05-18" and "02" <= r["date"][11:13] < "04")
              for r in rows), "residents stay off during the 02:00-04:00 restore passes")
    check(all(r["date"] >= "2011-02-26 19:40" for r in rows if r["player"] == "kitbag"), "no kitbag rows before he joined")
    late = [r for r in rows if r["date"] >= "2011-05-21" and r["player"] in joined and r["player"] != "rookery"
            and not r["date"].startswith("2011-06-11")]
    check(len(late) < 60, "after 20 May the residents barely build (%d rows)" % len(late))
    check(any(r["player"] == "morrow" and r["block"] == str(logblock.POWERED_RAIL) for r in rows), "Morrow's south-rail rows exist")
    early = logblock.rows_for_act(4)
    wall = {(r["x"], r["y"], r["z"]) for r in logblock.id_wall_rows()}
    check(not any((r["x"], r["y"], r["z"]) in wall and r["player"] == "i_bell" for r in early), "annex wall rows hidden until Act 7")
    lock = {(r["date"], r["x"], r["y"], r["z"]) for r in logblock.finch_lock_rows()}
    check(not any((r["date"], r["x"], r["y"], r["z"]) in lock for r in early), "finch lock rows hidden until Act 5")
    check(len(logblock.rows_for_act(4)) < len(logblock.rows_for_act(5)) < len(logblock.rows_for_act(7)), "LogBlock grows by act (lock at 5, chest and wall at 7)")
    check(media.RECORDINGS[0]["created_local"].startswith("2011-05-18 11:58"), "recording metadata: 11:58 BST")
    check(all(datetime.strptime(f["mtime"], "%d-%b-%Y %H:%M") >= datetime(2011, 2, 19) for f in media.PATCHCORD_FOLDER),
          "no Mumble recording predates the built-in recorder (1.2.3, 19 Feb 2011)")
    print()
    if FAILURES:
        print("%d FAILURES" % len(FAILURES))
        return 1
    print("all checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
