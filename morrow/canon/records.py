"""Copperline business records, Theo-era files, and the checklist hashes."""

from __future__ import annotations

from . import ciphers

NODE = "CHI-07"
CASE = "6118"
MAINTENANCE_UPTIME = "04:17:22"

CLIENT_ACCOUNT = {  # WHMCS client area, Act 1
    "username": "rookery",
    "email_on_file": "r.keel@example.net",
    "security_question": "What was the name of your first server?",
    "security_answer": "bracken",
    "service": {"product": "Minecraft - Builder (1 GB, 20 slots)", "label": "Mossfield", "node": NODE,
                "status": "Running", "players_online": 0, "address": "mossfield.copperlinehosting.com"},
    "invoices": [
        {"no": 21877, "date": "2011-06-01", "amount": "$6.95", "status": "Waived - T.V."},
        {"no": 22410, "date": "2011-09-01", "amount": "$6.95", "status": "Waived - T.V."},
        {"no": 25003, "date": "2012-06-01", "amount": "$6.95", "status": "Waived - T.V."},
        {"no": 28872, "date": "2013-06-01", "amount": "$6.95", "status": "Waived - T.V."},
        {"no": 31995, "date": "2014-11-01", "amount": "$6.95", "status": "Waived - T.V."},
    ],
    "ftp": {"host": "ftp.copperlinehosting.com", "user": "rookery",
            "note_act1": "World folders are locked while a recovery job is running."},
}

TICKETS = {
    "6118": {"subject": "Mossfield world damaged after storage failure", "opened": "2011-04-21 11:52",
             "client": "Rowan Keel (rookery)", "dept": "Game Server Recovery",
             "status_public": "Closed", "status_internal": "Retained",
             "messages": [
                 {"from": "Rowan Keel", "at": "2011-04-21 11:52", "body":
                  "Half of the map is missing. Spawn, the storehouse, the coast. Backup slot has the same "
                  "damage. Please tell me there is another copy somewhere."},
                 {"from": "Theo [Copperline]", "at": "2011-04-21 13:20", "body":
                  "No worries at all!  Your world will be back by tomorrow.  Iona is on it.\n\nCheers, T"},
                 {"from": "Iona Bell", "at": "2011-04-26 16:31", "body":
                  "Rowan, there is no older copy. I'm rebuilding from your LogBlock history. Attached is the "
                  "export path so you can check rows yourself if you want: /~mossfield/logblock/\n\n"
                  "Iona Bell // Copperline Recovery // alias: iona_bell"},
                 {"from": "Iona Bell", "at": "2011-05-18 06:31", "body":
                  "Stopped the restore at 06:17 CDT. Do not let anyone undo what it built until I've looked."},
             ]},
    "6290": {"subject": "do not let it restart", "opened": "2011-08-12 16:50",
             "client": "internal", "dept": "Operations", "status_public": "(not public)", "status_internal": "Open",
             "messages": [
                 {"from": "Iona Bell", "at": "2011-08-12 16:50", "body":
                  "Handover before I go. mrrw on " + NODE + " is stopped. Panel auto-restart is off. There is "
                  "an @reboot line in the crontab I could not remove without root on the node, Dale has it. "
                  "Rowan has the manual procedure. The checklist is in ops/6118-checklist.txt. Please do not "
                  "let it restart. - Iona (16:50 CDT)"},
             ]},
}

STATUS_INCIDENT = {
    "id": CASE,
    "original": {"title": "Chicago game storage - recovery maintenance",
                 "posted": "2011-05-18 01:55 CDT",
                 "body": "Scheduled recovery maintenance for affected Chicago game servers, 02:00-04:00 CDT. "
                         "Please stay off affected servers during the window.",
                 "update": "2011-05-18 06:25 CDT - Restore process stopped manually after running past the "
                           "window. Affected servers are read-only until further notice."},
    "morrow_rewrite": {"act": 6, "title": "Brief storage maintenance",
                       "posted": "2011-05-18 01:55 CST",
                       "body": "Brief storage maintenance completed 04:00 CST. No data loss."},
    "raw_export": "incident,posted,uptime,node,case\n6118,2011-05-18T01:55:00-05:00,%s,%s,mossfield\n" % (MAINTENANCE_UPTIME, NODE),
}

HEARTBEAT = {"process": "mrrw-01", "state": "running", "heartbeat": "ok",
             "first_heartbeat_on_this_host": "2014-12-14T03:12:40-06:00", "node": NODE}

TCADMIN_LOG = [
    ("2011-05-18 02:00:00 CDT", "ibell", NODE + "/mrrw", "START", "restore pass, window 02:00-04:00"),
    ("2011-05-18 06:17:22 CDT", "ibell", NODE + "/mrrw", "STOP", "manual, uptime 04:17:22"),
    ("2011-05-18 06:44:10 CDT", "tvale", NODE + "/mrrw", "START", "mid-session, will corrupt if left"),
    ("2011-05-18 11:12:30 CDT", "ibell", NODE + "/mrrw", "SET auto_restart=off", ""),
    ("2011-08-12 16:40:02 CDT", "ibell", NODE + "/mrrw", "STOP", "handover"),
    ("2014-12-14 03:12:38 CST", "tvale", NODE + "/mrrw", "START", "new host"),
    ("2014-12-14 03:13:01 CST", "tvale", NODE + "/mrrw", "SET auto_restart=on", ""),
]

ASSET_SALE = {  # December 2014 liquidation lot list (PDF on the mirror, Act 4)
    "title": "Copperline Hosting LLC - Asset Sale, Lot Results", "date": "2014-12-09",
    "lots": [
        {"lot": 11, "item": "Dell PowerEdge R410 (CHI-05), no drives", "buyer": "Lakeshore Recycling"},
        {"lot": 12, "item": "Dell PowerEdge R410 (CHI-06), no drives", "buyer": "Lakeshore Recycling"},
        {"lot": 13, "item": "APC Smart-UPS 1500 x2", "buyer": "M. Ortiz"},
        {"lot": 14, "item": "Dell PowerEdge R410 (game node %s), with drives" % NODE, "buyer": "T. Vale"},
        {"lot": 15, "item": "1x office plant (ficus)", "buyer": "I. Bell"},
        {"lot": 16, "item": "Office chairs x6", "buyer": "Lakeshore Recycling"},
    ],
}

KIT_EMAIL = {"from": "kit vale", "to": "theo", "date": "2019-03-02 22:47", "subject": "(no subject)",
             "body": "hey. idk if you still check this. saw the server's still up?? theo its been like 8 years. "
                     "nobody plays. i dont play. you can turn it off, its ok"}

THEO_CHANGELOG_2016 = [
    "2016-08-01  mrrw upgraded for 1.10 world format",
    "2016-08-02  session continuation: replays can extend a resident's routine from habit data",
    "2016-08-03  lowpath = owner for orphaned builds. temporary.",
    "2016-08-03  forum import re-run (some join dates set by importer)",
]

CRONTAB = ["# m h dom mon dow command", "@reboot /opt/mrrw/bin/watchdog --respawn mrrw",
           "*/5 * * * * /opt/mrrw/bin/heartbeat >> /var/log/mrrw/heartbeat.log"]

BRIDGE_CONFIG = {"plugin": "DiscordSRV", "channel": "#bridge", "on_process_exit": "relaunch mrrw chat",
                 "added": "2026-08-02", "added_by": "tvale"}

ROBOTS_TXT = "User-agent: *\nDisallow: /forum-import/\nDisallow: /~mossfield/\nDisallow: /tcadmin/legacy/\n"


def checklist() -> str:
    """ops/6118-checklist.txt: Iona's self-check hashes (Act 5 onward)."""
    return "\n".join([
        "6118 checklist - IB",
        "",
        "manual stop needs: maint flag sign ON, no sessions open, console key",
        "",
        "md5 plaintext A (rook floor):  " + ciphers.md5(ciphers.FLOOR_PLAINTEXT),
        "md5 token B (my key):          " + ciphers.md5(ciphers.IONA_TOKEN),
        "md5 command (annex wall):      " + ciphers.md5(ciphers.HALT_COMMAND),
        "md5 clean backup (04-20 zip):  (filled in at build, see out/backups.json)",
        "",
        "don't let it restart",
        "",
    ])
