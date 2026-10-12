"""Emit every generated record.

    python -m morrow.canon.build

Writes morrow/canon/out/ (full canon, text logs, per-act site data) and copies the
player-safe site bundle to dashboard/src/lib/canon/. The site bundle is split per act
so a page can never ship a later act's content early.
"""

from __future__ import annotations

import json
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
    """markers.json as Dynmap exported it in 2011. Positions are the 2011 ones."""
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
        "patchcord_folder": media.PATCHCORD_FOLDER if act >= 5 else [],
        "tvale_folder": media.TVALE_FOLDER if act >= 6 else [],
        "tvale_box": later.TVALE_BOX["files"] if act >= later.TVALE_BOX["act"] else [],
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
        "drops": [{"act": a, "after_hours": h, "file": f, "note": n} for a, h, f, n in later.DESK_DROPS if a <= act],
        "hints": later.HINTS.get(act, []),
    }
    if act >= 6:
        bundle["forum_rewrites"] = {str(k): {"body": b, "note": n} for k, (b, n) in later.FORUM_REWRITES.items()}
    if act >= 9:
        bundle["case_brief"] = later.CASE_BRIEF_FIELDS
        bundle["backdated_posts"] = later.BACKDATED_POSTS
        bundle["clean_md5"] = json.loads((files.OUT.parent / "hashes.json").read_text())["notrecovery-mossfield_2011-04-20.zip"]
    if act >= 10:
        bundle["usb_choices"] = {k: {"line": v[0], "voice": v[1]} for k, v in later.USB_CHOICES.items()}
        bundle["stop_log"] = later.STOP_LOG
        bundle["ready_status"] = later.READY_STATUS
        bundle["console"] = {"cmd_md5": ciphers.md5(ciphers.HALT_COMMAND), "key_md5": ciphers.md5(ciphers.IONA_TOKEN)}
    return bundle


PLUGIN = ROOT / "plugin-v2" / "src" / "main" / "resources" / "canon"


def corpus() -> list[dict]:
    """Everything Morrow ever ingested, with sources. Morrow may only reuse these words."""
    out = [{"text": people.THEO_MACRO, "source": "Theo support macro"}]
    for t in forum.THREADS:
        for p in t["posts"]:
            out.append({"text": p["body"], "source": "%s, forum, %s" % (p["author"], p["at"])})
    for day, lines in serverlog.DAYS.items():
        for t, _lvl, msg in lines:
            if msg.startswith("<"):
                who, text = msg[1:].split("> ", 1)
                out.append({"text": text, "source": "%s, chat, %s %s" % (who, day, t)})
    for at, frm, _to, text in serverlog.PRIVATE_MSGS:
        out.append({"text": text, "source": "%s, private message, %s" % (frm, at)})
    return out


def _walk(a, b, start_s, secs):
    """Straight-ish walk between two anchors, one waypoint per second."""
    (x0, y0, z0), (x1, y1, z1) = layout.ANCHORS[a], layout.ANCHORS[b]
    pts = []
    for i in range(secs + 1):
        f = i / secs
        pts.append([start_s + i, round(x0 + (x1 - x0) * f, 1), y0 + 1, round(z0 + (z1 - z0) * f, 1)])
    return pts


def incident_replay() -> list[dict]:
    """Act 5: the night of 18 May 2011, ~20 min compressed. Each figure walks and 'says' its logged lines."""
    figs = [
        {"who": "rookery", "path": _walk("spawn", "rules_signs", 0, 30),
         "lines": [[20, "everyone get off for a bit"], [140, "if it goes bad theres the floor"]]},
        {"who": "cairn", "path": _walk("cairn_farm", "cairn_storehouse_door_2011", 0, 60) + _walk("cairn_storehouse_door_2011", "cairn_farm", 61, 60),
         "lines": [[40, "theres someone at the farm"], [70, "why is there another me at the farm"]]},
        {"who": "juni_lantern", "path": _walk("june_house", "june_old_dock", 0, 40),
         "lines": [[60, "something is putting glass on my tower"], [110, "it put a roof on it"]]},
        {"who": "kitbag", "path": _walk("spawn", "spawn", 0, 10),
         "lines": [[90, "rook is by the cake"]]},
        {"who": "patchcord", "path": _walk("patchcord_relay_hut", "patchcord_relay_hut", 0, 10),
         "lines": [[150, "recording this, saving as moss_0518"], [200, serverlog_line("06:09:17")]]},
        {"who": "i_bell", "path": _walk("june_old_dock", "june_old_dock", 0, 10),
         "lines": [[170, "putting my key somewhere theo cant reach"]]},
    ]
    return figs


def serverlog_line(t: str) -> str:
    for tt, _l, msg in serverlog.DAYS["2011-05-18"]:
        if tt == t and msg.startswith("<"):
            return msg.split("> ", 1)[1]
    raise KeyError(t)


def lowpath_route() -> list[dict]:
    """Act 8: lowpath's nightly walk, stitched from everyone's habits; ends at lowpath's place."""
    legs = [("cairn_farm", "cairn_storehouse_door_2011", "cairn's farm loop"),
            ("cairn_storehouse_door_2011", "june_old_dock", "june's dock visit"),
            ("june_old_dock", "finch_hub", "finch's rail ride"),
            ("finch_hub", "spawn", "rook's spawn check"),
            ("spawn", "lowpath_place", "the low path home")]
    out, t = [], 0
    for a, b, label in legs:
        out.append({"from": a, "to": b, "habit": label, "path": _walk(a, b, t, 40)})
        t += 41
    return out


def write_plugin_canon() -> None:
    PLUGIN.mkdir(parents=True, exist_ok=True)
    data = {
        "anchors": layout.ANCHORS,
        "provisional_layout": layout.PROVISIONAL,
        "motd_by_act": {str(a): ciphers.motd_codes(t) for a, t in ciphers.MOTD_BY_ACT.items()},
        "rules_signs_2011": ciphers.RULES_SIGNS_2011,
        "rules_signs_morrow": ciphers.RULES_SIGNS_MORROW,
        "tally_book_morrow": ciphers.tally_book_morrow(),
        "sign_back_notes": {
            "rookery_house": ["rookery:", "absent since 2011", "boots off (kept)", ""],
            "rules_signs": ["rules:", "1 typo noted", "", ""],
            "cairn_storehouse_door_2011": ["cairn:", "uneven x14", "normalised", ""],
            "june_house": ["juni_lantern:", "tower plan", "completed", "2011-05-18"],
        },
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
