"""Emit every generated record.

    python -m morrow.canon.build

Writes morrow/canon/out/ (full canon, text logs, per-act site data) and copies the
player-safe site bundle to dashboard/src/lib/canon/. The site bundle is split per act
so a page can never ship a later act's content early.
"""

from __future__ import annotations

import json
from pathlib import Path

from . import ciphers, forum, irc, layout, logblock, people, records, serverlog

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
    }


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
    }
    if act >= 4:
        bundle["asset_sale"] = records.ASSET_SALE
    if act >= 5:
        bundle["checklist"] = records.checklist()
    if act >= 6:
        bundle["tcadmin_log"] = records.TCADMIN_LOG
    if act >= 7:
        bundle["irc_log"] = irc.render()
    return bundle


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    SITE.mkdir(parents=True, exist_ok=True)
    (OUT / "canon.json").write_text(json.dumps(full_canon(), indent=1), encoding="utf-8")
    (OUT / "copperline-ops.irc.log").write_text(irc.render(), encoding="utf-8")
    for day in serverlog.DAYS:
        (OUT / ("server-%s.log" % day)).write_text(serverlog.render(day), encoding="utf-8")
    (OUT / "logblock.json").write_text(json.dumps(logblock.all_rows()), encoding="utf-8")
    (OUT / "6118-checklist.txt").write_text(records.checklist(), encoding="utf-8")
    (OUT / "robots.txt").write_text(records.ROBOTS_TXT, encoding="utf-8")
    for act in range(1, 11):
        (SITE / ("act-%02d.json" % act)).write_text(json.dumps(site_bundle(act)), encoding="utf-8")
    print("canon written:", OUT)
    print("site bundles written:", SITE)


if __name__ == "__main__":
    main()
