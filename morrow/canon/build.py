"""Emit every generated record.

    python -m morrow.canon.build

Writes morrow/canon/out/ (full canon, text logs, per-act site data) and copies the
player-safe site bundle to dashboard/src/lib/canon/. The site bundle is split per act
so a page can never ship a later act's content early.
"""

from __future__ import annotations

import json
import re
from pathlib import Path

from . import ciphers, files, forum, irc, later, layout, logblock, media, people, records, serverlog

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "morrow" / "canon" / "out"
SITE = ROOT / "dashboard" / "src" / "lib" / "canon"


def full_canon() -> dict:
    floor = ciphers.build_floor()
    return {
        "provisional_layout": layout.PROVISIONAL,
        "seed": {"text": layout.SEED_TEXT, "java_hash": ciphers.java_string_hash(layout.SEED_TEXT)},
        "anchors": layout.ANCHORS,
        "residents": [r.__dict__ for r in people.RESIDENTS],
        "join_order": [r.handle for r in people.join_order()],
        "staff": people.STAFF, "fabricated": people.FABRICATED,
        "ciphers": {
            "rules_signs_2011": ciphers.RULES_SIGNS_2011,
            "rules_signs_morrow": ciphers.RULES_SIGNS_MORROW,
            "floor": {"ciphertext": floor.ciphertext, "grid": floor.grid, "tidied": floor.tidied,
                      "tidied_rows": list(ciphers.FLOOR_TIDIED_ROWS)},
            "iona_chest": ciphers.build_iona_chest(),
            "id_wall": ciphers.build_id_wall(),
            "residents_key": ciphers.build_residents_key(),
            "slipped_line": ciphers.build_slipped_line(),
            "tune": [{"pitch": p, "base": b} for p, b in ciphers.TUNE],
            "braille_shelves": ciphers.build_braille_shelves(),
            "motd_by_act": {a: ciphers.motd_codes(t) for a, t in ciphers.MOTD_BY_ACT.items()},
            "light_corridor": ciphers.build_light_corridor(),
            "sound_replay": ciphers.build_sound_replay(),
            "sheep_census": ciphers.sheep_census(),
            "door_register": {a: ciphers.door_register(a) for a in range(1, 11)},
        },
        "records": {k: getattr(records, k) for k in
                    ["CLIENT_ACCOUNT", "TICKETS", "STATUS_INCIDENT", "HEARTBEAT", "TCADMIN_LOG", "ASSET_SALE",
                     "KIT_EMAIL", "THEO_CHANGELOG_2016", "CRONTAB", "BRIDGE_CONFIG"]},
        "forum": {"members": forum.MEMBERS, "threads": forum.THREADS, "archivist_notes": forum.ARCHIVIST_NOTES},
        "private_msgs": serverlog.PRIVATE_MSGS,
        "media": {"june_shots": media.JUNE_SHOTS, "forged_shots": media.FORGED_SHOTS, "recordings": media.RECORDINGS},
    }


def dynmap_markers() -> list[dict]:
    """The places in Rookery's hand-written map key (media.MAPKEY, mapkey.txt, March 2011), as rows.
    2011 Dynmap had no markers (they arrived in September 2011): the site should show mapkey.txt."""
    a = layout.ANCHORS
    pick = [("spawn", "spawn"), ("rookery_house", "rook's"), ("cairn_storehouse_door_2011", "storehouse"),
            ("cairn_farm", "the farms"), ("sheep_pen", "sheep"), ("june_house", "june's point"),
            ("june_tower", "the tower (wip)"), ("patchcord_relay_hut", "patch's hut"), ("finch_hub", "rail hub")]
    out = [{"label": label, "x": a[k][0], "y": a[k][1], "z": a[k][2], "type": "house"} for k, label in pick]
    out.append({"label": "low path", "x": 150, "y": 74, "z": 36, "type": "path"})
    return out


def site_bundle(act: int) -> dict:
    """Only what a player could reach by the given act."""
    threads = []
    for t in forum.THREADS:
        if t["act"] <= act:
            threads.append({**t, "posts": [p for p in t["posts"]]})
    status = dict(records.STATUS_INCIDENT)
    if act < 6:
        status.pop("morrow_rewrite")
    last_real_post = max(p["id"] for t in forum.THREADS for p in t["posts"])
    account = dict(records.CLIENT_ACCOUNT)
    bundle = {
        "act": act,
        "members": {k: v for k, v in forum.MEMBERS.items() if k != "archivist" or act >= 1},
        "threads": threads,
        "archivist_notes": [n for n in forum.ARCHIVIST_NOTES if n["act"] <= act],
        "client_account": account,
        "ticket_6118": records.TICKETS["6118"],
        "status_incident": status,
        "heartbeat": records.HEARTBEAT,
        "robots_txt": records.ROBOTS_TXT,
        "motd": ciphers.motd_codes(ciphers.MOTD_BY_ACT[act]),
        "server_logs": {d: serverlog.render(d) for d in serverlog.DAYS if act >= 4 or d == "2011-05-12"},
        "logblock": logblock.rows_for_act(act),
        "june_shots": [x for x in media.JUNE_SHOTS + media.FORGED_SHOTS if x["act"] <= act],
        "dynmap_markers": dynmap_markers() if act >= 2 else [],
        # mapkey.txt as Rook wrote it (the site serves it at /~mossfield/mapkey.txt)
        "mapkey": ("\n".join(media.MAPKEY["lines"]) + "\n") if act >= 2 else None,
        "mapkey_txt": ("\n".join(media.MAPKEY["lines"]) + "\n") if act >= 2 else None,
        "mossfield_files": ([{"file": "server.properties", "mtime": "22-Feb-2011 23:41", "size": "1K",
                              "body": media.SERVER_PROPERTIES}] if act >= 2 else []),
        "patchcord_folder": media.PATCHCORD_FOLDER if act >= 5 else [],
        "tvale_folder": media.TVALE_FOLDER if act >= 6 else [],
        "tvale_box": later.TVALE_BOX["files"] if act >= later.TVALE_BOX["act"] else [],
        "status_archive": records.STATUS_ARCHIVE,
        "history": records.COPPERLINE_HISTORY,
    }
    if act >= 4:
        bundle["asset_sale"] = records.ASSET_SALE
    if act >= 5:
        bundle["checklist"] = records.checklist()
    if act >= 6:
        bundle["tcadmin_log"] = records.TCADMIN_LOG
    if act >= 7:
        bundle["irc_log"] = irc.render()
    bundle["desk"] = {
        "rules": [r for r in later.DESK_RULES if r["act"] <= act],
        "files": {k: {"act": v[0], "line": v[1], "voice": v[2]} for k, v in later.DESK_FILES.items() if v[0] <= act},
        "drops": [{"act": a, "after_hours": h, "file": f, "note": n, "voice": v} for a, h, f, n, v in later.DESK_DROPS if a <= act],
        "hints": later.HINTS.get(act, []),
        "hint_hours": later.HINT_HOURS,
        "none_replies": later.NONE_REPLIES,
        "place_words": later.PLACE_WORDS,
        # Correct evidence filed early: matched (tokens only, never the reply) so Theo can say "give me a
        # bit" and answer properly once the act arrives. The ticket is re-read against the later bundle.
        "held": [{"id": r["id"], "act": r["act"], "tokens": r["tokens"], "need": r["need"], "require": r.get("require", [])}
                 for r in later.DESK_RULES if r["act"] > act],
        "held_files": {k: v[0] for k, v in later.DESK_FILES.items() if v[0] > act},
    }
    if act >= 6:
        bundle["forum_rewrites"] = {str(k): {"body": b, "note": n} for k, (b, n) in later.FORUM_REWRITES.items()}
    if act >= 9:
        bundle["case_brief"] = later.CASE_BRIEF_FIELDS
        bundle["backdated_posts"] = later.BACKDATED_POSTS
        # the importer appended the players' "2011" posts, so their ids sit above the last real one (the tell)
        bundle["backdated_post_id_base"] = last_real_post + 1
        bundle["backdated_row_templates"] = [
            {"days": d, "time": t, "block": bid, "anchor": a, "offset": list(off)} for d, t, bid, a, off in logblock.BACKDATED_ROW_TEMPLATES]
        bundle["clean_md5"] = json.loads((files.OUT.parent / "hashes.json").read_text())["notrecovery-mossfield_2011-04-20.zip"]
        bundle["brief_replies"] = {k: {"text": t, "voice": v} for k, (t, v) in later.BRIEF_REPLIES.items()}
    if act >= 10:
        bundle["usb_choices"] = {k: {"line": v[0], "voice": v[1]} for k, v in later.USB_CHOICES.items()}
        bundle["usb_vote"] = {"prompt": later.USB_VOTE["prompt"], "undecided": later.USB_VOTE["undecided"],
                              "options": later.USB_VOTE["options"]}
        bundle["stop_log"] = later.STOP_LOG
        bundle["ready_status"] = later.READY_STATUS
        bundle["console"] = {"cmd_md5": ciphers.md5(ciphers.HALT_COMMAND), "key_md5": ciphers.md5(ciphers.IONA_TOKEN)}
        bundle["theo_ask"] = later.THEO_ASK
        bundle["theo_after"] = later.THEO_AFTER
    if act >= 7:
        # the panel console takes Iona's key (Act 7's hand-in) once the players can log in (Act 6)
        bundle["console_key_md5"] = ciphers.md5(ciphers.IONA_TOKEN)
    return bundle


PLUGIN = ROOT / "plugin-v2" / "src" / "main" / "resources" / "canon"


QUOTE_BLOCK = re.compile(r"\[QUOTE(=[^\]]*)?\].*?\[/QUOTE\]\s*", re.S | re.I)


def corpus() -> list[dict]:
    """Everything Morrow ever ingested, with sources. Morrow may only reuse these words.
    It reads chat, the forum, private messages, the support macros, billing notes, the signs in the
    world, and (Theo's 2018 changelog) the support voicemail box."""
    out = [{"text": people.THEO_MACRO, "source": "Theo support macro"}]
    for t in forum.THREADS:
        for p in t["posts"]:
            body = QUOTE_BLOCK.sub("", p["body"])  # a quote is somebody else's words, already sourced
            out.append({"text": body, "source": "%s, forum, %s" % (p["author"], p["at"])})
    for inv in records.CLIENT_ACCOUNT["invoices"]:
        if "(" in inv["status"]:
            out.append({"text": inv["status"].split("(", 1)[1].rstrip(")"),
                        "source": "billing note, invoice #%d, %s" % (inv["no"], inv["date"])})
    for sign in ciphers.RULES_SIGNS_2011:
        out.append({"text": " ".join(l for l in sign if l), "source": "spawn rules sign, 2011"})
    for key, sign in ciphers.GENUINE_SIGNS_2011.items():
        out.append({"text": " ".join(l for l in sign if l), "source": "sign (%s), 2011" % key})
    out.append({"text": later.VOICEMAIL_2011, "source": "Theo's voicemail to Rowan, 2011-04-28 (support voicemail box)"})
    for day, lines in serverlog.DAYS.items():
        for t, _lvl, msg in lines:
            if msg.startswith("<"):
                who, text = msg[1:].split("> ", 1)
                out.append({"text": text, "source": "%s, chat, %s %s" % (who, day, t)})
    for at, frm, _to, text in serverlog.PRIVATE_MSGS:
        out.append({"text": text, "source": "%s, private message, %s" % (frm, at)})
    return out


def _standing(key):
    x, y, z = layout.ANCHORS[key]
    return y + 1 if key in layout.GROUND_ANCHORS else y


WALK_SPEED = 4.0   # blocks per second: a figure walks at a player's pace, in real seconds
NIGHT_SECONDS = 480   # an in-game night is 10 minutes; every replay fits inside eight


def _pt(key, dx=0, dz=0):
    x, _, z = layout.ANCHORS[key]
    return (x + dx + 0.5, _standing(key), z + dz + 0.5)


def _route(stops):
    """Waypoints [t, x, y, z] through (point, dwell seconds) stops: walk at WALK_SPEED between them, stand
    still for the dwell. The plugin interpolates between waypoints, so a figure never jumps."""
    pts, t = [], 0.0
    for i, (p, dwell) in enumerate(stops):
        if i:
            q = stops[i - 1][0]
            d = ((p[0] - q[0]) ** 2 + (p[2] - q[2]) ** 2) ** 0.5
            t += max(d / WALK_SPEED, 0.5)
        pts.append([round(t, 1), round(p[0], 1), round(p[1], 1), round(p[2], 1)])
        if dwell:
            t += dwell
            pts.append([round(t, 1), round(p[0], 1), round(p[1], 1), round(p[2], 1)])
    return pts


def incident_replay() -> list[dict]:
    """Act 5: the night of 18 May 2011 in about eight real minutes. Each figure walks its habit and
    'says' its logged lines at real intervals; a figure with 'until' leaves (logs off) at that second."""
    farm, barn = _pt("cairn_farm", 6, 4), _pt("cairn_storehouse_door_2011", 0, -2)
    cake, signs = _pt("spawn", 1, -1), _pt("rules_signs", 0, 2)
    june_door, dock = _pt("june_house", 1, 0), _pt("june_old_dock", 5, 0)
    hut = _pt("patchcord_relay_hut", 0, -3)
    figs = [
        {"who": "rookery", "path": _route([(cake, 30), (signs, 40), (cake, 60), (signs, 50), (cake, 240)]),
         "lines": [[20, "everyone get off for a bit"], [155, "im at spawn kit"], [250, serverlog_line("04:22:40")]]},
        {"who": "cairn", "path": _route([(farm, 20), (barn, 40), (farm, 40), (barn, 40), (farm, 40), (barn, 40), (farm, 30), (barn, 10)]),
         "lines": [[50, "theres someone at the farm"], [110, "why is there another me at the farm"]]},
        {"who": "juni_lantern", "path": _route([(june_door, 15), (dock, 420)]),
         "lines": [[125, "something is putting glass on my tower"], [170, "it put a roof on it"], [455, "patch ur hands are on the wrong keys lol"]]},
        {"who": "kitbag", "path": _route([(_pt("spawn", -2, 2), 20), (cake, 60), (_pt("spawn", 3, 3), 40), (cake, 80)]), "until": 190,
         "lines": [[150, "rook is by the cake"]]},
        {"who": "patchcord", "path": _route([(hut, 470)]),
         "lines": [[200, "what am I looking at"], [330, "it's using theo's voice in chat now"],
                   [380, "recording this, saving as moss_0518"], [450, serverlog_line("06:09:17")]]},
        {"who": "i_bell", "path": _route([(_pt("june_old_dock", 2, 0), 0), (dock, 60)]), "from": 395,
         "lines": [[410, serverlog_line("06:02:31")]]},
    ]
    for f in figs:
        assert f["path"][-1][0] <= NIGHT_SECONDS, (f["who"], f["path"][-1][0])
    return figs


def serverlog_line(t: str) -> str:
    for tt, _l, msg in serverlog.DAYS["2011-05-18"]:
        if tt == t and msg.startswith("<"):
            return msg.split("> ", 1)[1]
    raise KeyError(t)


LOWPATH_LEGS = [("cairn_farm", "cairn_storehouse_door_2011", "cairn's farm loop", 25),
                ("cairn_storehouse_door_2011", "june_old_dock", "june's dock visit", 30),
                ("june_old_dock", "finch_hub", "finch's rail ride", 20),
                ("finch_hub", "spawn", "rook's spawn check", 25),
                ("spawn", "lowpath_place", "the low path home", 0)]


def lowpath_route() -> list[dict]:
    """Act 8: lowpath's nightly walk, stitched from everyone's habits, at walking pace with a stop at
    each; ends at lowpath's place, where it stands until dawn. The last leg alone is the Act 9 bed
    scene (spawn to its place, about a minute)."""
    out, t = [], 0.0
    for a, b, label, dwell in LOWPATH_LEGS:
        pts = _route([(_pt(a), 0), (_pt(b), dwell)])
        pts = [[round(p[0] + t, 1), p[1], p[2], p[3]] for p in pts]
        out.append({"from": a, "to": b, "habit": label, "path": pts})
        t = pts[-1][0]
    assert t <= NIGHT_SECONDS, t
    return out


# Rook's 2011 pixel-art creeper face, 8x8 wool (green 13, black 15), bottom-west corner at the
# spawn_creeper anchor; rows run up. He laid the top five rows; Morrow "restores" the bottom three
# at the Act 3 dawn (24 blocks), which is how players first see it complete something.
CREEPER_FACE = [  # top row first, as you look at it
    "GGGGGGGG",
    "GBBGGBBG",
    "GBBGGBBG",
    "GGGBBGGG",
    "GGBBBBGG",
    "GGBBBBGG",
    "GGBGGBGG",
    "GGGGGGGG",
]
CREEPER_MISSING_ROWS = 3   # from the bottom


def creeper_art() -> dict:
    rows = [[15 if ch == "B" else 13 for ch in row] for row in reversed(CREEPER_FACE)]  # rows[0] = bottom
    return {"rows": rows, "missing_rows": CREEPER_MISSING_ROWS}


# Rook's newbie chest at spawn (2011 items; the plugin refills it at dawn: "1 chest restored (spawn)")
KIT_CHEST = [{"item": "stone_pickaxe", "count": 1}, {"item": "stone_axe", "count": 1}, {"item": "stone_shovel", "count": 1},
             {"item": "torch", "count": 16}, {"item": "bread", "count": 8}, {"item": "red_bed", "count": 1},
             {"item": "oak_log", "count": 16}, {"item": "oak_planks", "count": 8}, {"item": "crafting_table", "count": 1}]


def write_plugin_canon() -> None:
    PLUGIN.mkdir(parents=True, exist_ok=True)
    floor = ciphers.build_floor()
    data = {
        "anchors": layout.ANCHORS,
        "provisional_layout": layout.PROVISIONAL,
        "motd_by_act": {str(a): ciphers.motd_codes(t) for a, t in ciphers.MOTD_BY_ACT.items()},
        "rules_signs_2011": ciphers.RULES_SIGNS_2011,
        "rules_signs_morrow": ciphers.RULES_SIGNS_MORROW,
        "tally_book_morrow": ciphers.tally_book_morrow(),
        "sign_back_notes": {  # restore-engine templates (later.ENGINE_VOCAB), not chat
            "rookery_house": ["rookery:", "absent since 2011", "boots off (kept)", ""],
            "rules_signs": ["rules:", "1 line normalised", "", ""],
            "cairn_storehouse_door_2011": ["cairn:", "uneven x14", "normalised", ""],
            "june_house": ["juni_lantern:", "tower plan", "completed", "2011-05-18"],
            "rookery_floor_sign": ["floor:", "3 rows", "normalised", ""],
        },
        # the rework (2026-11): what the plugin needs to know about the lived-in world and the new beats
        "engine_lines": later.ENGINE_LINES,
        "skip_fallback": later.SKIP_FALLBACK,
        "theo_drafts": later.THEO_DRAFTS,
        "visitor_book": later.VISITOR_BOOK,
        "theo_timed": [{"act": a, "after_hours": h, "voice": v} for a, h, _f, _n, v in later.DESK_DROPS if v]
                      + [{"act": 10, "after_stop_hours": later.THEO_AFTER["after_stop_hours"], "voice": later.THEO_AFTER["voice"]}],
        "theo_ask_voice": later.THEO_ASK["voice"],
        "floor": {"grid": floor.grid, "tidied": floor.tidied, "tidied_rows": list(ciphers.FLOOR_TIDIED_ROWS),
                  "row_dz": [layout.floor_row_z(r) for r in range(ciphers.FLOOR_ROWS)]},
        "id_wall_left": list(layout.ID_WALL_LEFT), "patch_note_left": list(layout.PATCH_NOTE_LEFT),
        "residents_key_left": list(layout.RESIDENTS_KEY_LEFT),
        "lived_in_signs": ciphers.GENUINE_SIGNS_2011,
        "storehouse_jog": {"broke": [list(c) for c in logblock.jog_cells()[0]], "placed": [list(c) for c in logblock.jog_cells()[1]]},
        "junctions": {k: {"lever": list(off), "on": on} for k, (off, on) in logblock.JUNCTION_LEVERS.items()},
        "dock": {"anchor": "june_old_dock", "length": 7, "width": 2},
        "creeper": creeper_art(),
        "kit_chest": KIT_CHEST,
        "bread_chests": {"spawn_bread_chest": 20, "storehouse_bread": 24},
        "south_rail_stop_z": layout.SOUTH_RAIL_STOP_Z,
        "lowpath_last_leg": lowpath_route()[-1]["path"],
        "status_lines": ["restore 61%. no worries at all. back by tomorrow",
                         "restore 99%. your world will be back by tomorrow",
                         "queue 1 open. no worries at all"],
        "door_register": {str(a): ciphers.door_register(a) for a in range(1, 11)},
        "listed_players_early": ["cairn", "juni_lantern", "lowpath"],
        "sign_rewrites": later.SIGN_REWRITES,
        "transcription_books": later.TRANSCRIPTION_BOOKS,
        "sga_signs": later.SGA_SIGNS,
        "per_player_signs": later.PER_PLAYER_SIGNS,
        "stop_log": later.STOP_LOG,
        "ready_status": later.READY_STATUS,
        "motd_ready": ciphers.motd_codes(later.READY_STATUS),
        "kit_line": "you can turn it off, its ok",
        "light_corridor": ciphers.build_light_corridor(),
        "sound_replay": ciphers.build_sound_replay(),
        "braille_shelves": ciphers.build_braille_shelves(),
        "residents_join_order": [r.handle for r in people.join_order()],
        "incident_replay": incident_replay(),
        "lowpath_route": lowpath_route(),
        "flags": later.FLAGS, "flag_word": later.FLAG_WORD,
        "pots": later.POTS, "pot_for_players": later.POT_FOR_PLAYERS,
        "map_wall": later.MAP_WALL, "genuine_2015_map": later.GENUINE_2015_MAP,
        "museum_sign": later.MUSEUM_SIGN, "maint_sign_text": later.MAINT_SIGN_TEXT,
        "south_rail_dir": list(layout.SOUTH_RAIL_DIR),
        "finch_lock_bits": later.FINCH_LOCK_BITS, "finch_spur_sign": later.FINCH_SPUR_SIGN,
        "nether_hub_sign": later.NETHER_HUB_SIGN,
        "genuine_signs_2011": ciphers.GENUINE_SIGNS_2011,
        "finch_south_sign": {"anchor": "south_rail_end", "offset": list(ciphers.FINCH_SOUTH_SIGN_OFFSET),
                             "lines": ciphers.GENUINE_SIGNS_2011["finch_south_rail"]},
        "stop_secret_hint": "set stop-secret in plugins/mrrw/config.yml to the website's MORROW_STOP_SECRET",
    }
    (PLUGIN / "canon.json").write_text(json.dumps(data, indent=1), encoding="utf-8")
    (PLUGIN / "corpus.json").write_text(json.dumps(corpus(), indent=1), encoding="utf-8")


def write_discord_canon() -> None:
    """What the Discord bot says, and when. It only ever speaks authored lines."""
    d = ROOT / "discord-v2"
    d.mkdir(parents=True, exist_ok=True)
    (d / "canon.json").write_text(json.dumps({
        "morrow_dms": later.MORROW_DMS, "quote_template": later.MORROW_QUOTE_TEMPLATE,
        "theo_dms": later.THEO_DMS,
        "welcome": "This is a read-only mirror of the old Copperline site.  Records for case 6118 (Mossfield) are kept "
                   "until 31 Dec 2026.  If you were a Mossfield player, the archive desk is open: "
                   "https://copperlinehosting.com/support/index.php  No worries at all.",
        "bridge_cut": {"text": "cutting the bridge now.  bye mossfield.", "voice": "T-10-cut"},
        "fake_theo": later.FAKE_THEO_NOTE,
        # Theo's big desk replies, posted to #general when the plugin's journal releases them ("theo.reply")
        "theo_mirror": {
            "header": later.THEO_MIRROR_HEADER,
            "replies": {**{r["voice"]: r["reply"] for r in later.DESK_RULES if r.get("voice") in later.THEO_MIRROR_VOICES},
                        **{v: t for t, v in later.BRIEF_REPLIES.values() if v in later.THEO_MIRROR_VOICES},
                        **{v: n for _a, _h, _f, n, v in later.DESK_DROPS if v in later.THEO_TIMED_VOICES},
                        later.THEO_ASK["voice"]: later.THEO_ASK["text"],
                        later.THEO_AFTER["voice"]: later.THEO_AFTER["text"]},
        },
        # Act 10: the group's USB vote in #general (the plugin counts it from usb-votes.json)
        "usb_vote": later.USB_VOTE,
        # Act 7: Morrow answers DMs through the plugin's corpus (GET /corpus), signed like its DMs
        "persona_reply_template": "{as}: {text}",
        "personas": sorted({dm["as"] for dm in later.MORROW_DMS}),
    }, indent=1), encoding="utf-8")


def main() -> None:
    files.build()
    OUT.mkdir(parents=True, exist_ok=True)
    SITE.mkdir(parents=True, exist_ok=True)
    (OUT / "canon.json").write_text(json.dumps(full_canon(), indent=1), encoding="utf-8")
    (OUT / "copperline-ops.irc.log").write_text(irc.render(), encoding="utf-8")
    for day in serverlog.DAYS:
        (OUT / ("server-%s.log" % day)).write_text(serverlog.render(day), encoding="utf-8")
    (OUT / "logblock.json").write_text(json.dumps(logblock.all_rows()), encoding="utf-8")
    (OUT / "6118-checklist.txt").write_text(records.checklist(), encoding="utf-8")
    (OUT / "robots.txt").write_text(records.ROBOTS_TXT, encoding="utf-8")
    folders = {"juni_lantern": {"password": people.by_handle("juni_lantern").lwc_password, "act": 3,
                                "paths": ["/~juni_lantern", "/u/juni_lantern", "/media/june"]},
               "patchcord": {"password": people.by_handle("patchcord").lwc_password, "act": 5,
                             "paths": ["/~patchcord", "/u/patchcord", "/media/patchcord"]},
               "tvale": {"password": later.TVALE_BOX["password"], "act": later.TVALE_BOX["act"],
                         "paths": ["/~tvale/box", "/u/tvale/box", "/media/tvalebox"]}}
    (SITE / "folders.json").write_text(json.dumps(folders), encoding="utf-8")
    for act in range(1, 11):
        (SITE / ("act-%02d.json" % act)).write_text(json.dumps(site_bundle(act)), encoding="utf-8")
    write_plugin_canon()
    write_discord_canon()
    print("canon written:", OUT)
    print("site bundles written:", SITE)


if __name__ == "__main__":
    main()
