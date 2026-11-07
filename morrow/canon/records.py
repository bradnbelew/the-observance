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
        {"no": 21877, "date": "2011-06-01", "amount": "$6.95", "status": "Waived - T.V. (keep billable)"},
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
                 {"from": "Rowan Keel", "at": "2011-04-28 18:05", "body":
                  "Attaching the voicemail Theo left me, because 'back by tomorrow' was a week ago.",
                  "attach": "theo_voicemail_2011.wav"},
                 {"from": "Iona Bell", "at": "2011-05-18 06:31", "body":
                  "Stopped the restore at 06:17 CDT. Do not let anyone undo what it built until I've looked."},
             ]},
    "6290": {"subject": "do not let it restart", "opened": "2011-08-12 16:50",
             "client": "internal", "dept": "Operations", "status_public": "(not public)", "status_internal": "Open",
             "messages": [
                 {"from": "Iona Bell", "at": "2011-08-12 16:50", "body":
                  "Handover before I go. mrrw on " + NODE + " is stopped. Panel auto-restart is off. There is "
                  "an @reboot watchdog in root's crontab that I didn't add. Theo says he didn't either. I can't "
                  "remove it without root on the node, Dale has it. Rowan has the manual procedure. The checklist is in ops/6118-checklist.txt. Please do not "
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
    # Morrow's rewrite: collage of Theo's March 2011 outage post, Iona's window and the macro. Its
    # timestamp formatter always writes CST (it never learned daylight saving): the tell.
    "morrow_rewrite": {"act": 6, "title": "Brief storage maintenance",
                       "posted": "2011-05-18 01:55 CST",
                       "body": "Brief storage maintenance on the Chicago nodes, 02:00-04:00 CST. No worries at all. "
                               "Your world will be back by tomorrow."},
    "raw_export": "incident,posted,uptime,node,case\n6118,2011-05-18T01:55:00-05:00,%s,%s,mossfield\n" % (MAINTENANCE_UPTIME, NODE),
}

HEARTBEAT = {"process": "mrrw-01", "state": "running", "heartbeat": "ok",
             "first_heartbeat_on_this_host": "2014-12-14T03:12:40-06:00", "node": NODE}

TCADMIN_LOG = [
    ("2011-05-18 02:00:00 CDT", "ibell", NODE + "/mrrw", "START", "restore pass, window 02:00-04:00"),
    ("2011-05-18 03:58:30 CDT", "tvale", NODE + "/mrrw", "SET debug_view=on", ""),
    ("2011-05-18 06:17:22 CDT", "ibell", NODE + "/mrrw", "STOP", "manual, uptime 04:17:22"),
    ("2011-05-18 06:44:10 CDT", "tvale", NODE + "/mrrw", "START", "mid-session, will corrupt if left"),
    ("2011-05-18 11:12:30 CDT", "ibell", NODE + "/mrrw", "SET auto_restart=off", ""),
    ("2011-05-18 11:14:02 CDT", "ibell", NODE + "/mrrw", "STOP", "manual"),
    ("2011-08-12 16:40:02 CDT", "ibell", NODE + "/mrrw", "SET auto_restart=off", "handover. leave it off"),
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
        {"lot": 17, "item": "Dell PowerEdge 1950 (WEB-01: web, billing, forum), with drives", "buyer": "T. Vale"},
    ],
}

KIT_EMAIL = {"from": "kit vale", "to": "theo", "date": "2019-03-02 22:47", "subject": "(no subject)",
             "body": "hey. idk if you still check this. saw the server's still up?? theo its been like 8 years. "
                     "nobody plays. i dont play. you can turn it off, its ok"}

THEO_CHANGELOG_2016 = [  # mrrw-changelog-2016.txt: started in 2016, Theo kept appending to it
    "2016-08-01  mrrw upgraded for 1.10",
    "2016-08-02  session continuation: let replays keep going, like the 18th. on purpose this time.",
    "2016-08-03  lowpath = owner for orphaned builds. placeholder name off rook's map key. temporary.",
    "2016-08-03  forum import re-run (some join dates set by importer)",
    "2016-08-11  lowpath is walking around?? it was meant to be a name in a column. it's using the replays",
    "            and the import gave it a profile. didn't do either. leaving it, it's not hurting anything",
    "2018-02-17  chat helper can answer with clips from the support voicemail box + ticket attachments (for the voices)",
    "2019-03-09  fed it the old inbox. all of it. for the voices",
    "2021-04-02  mirror renders the gallery thumbs from mrrw.db now, not the old screenshots",
    "2023-06-08  skip waxed blocks (so I can keep things)",
]

CRONTAB = ["# m h dom mon dow command", "@reboot /opt/mrrw/bin/watchdog --respawn mrrw",
           "*/5 * * * * /opt/mrrw/bin/heartbeat >> /var/log/mrrw/heartbeat.log"]

BRIDGE_CONFIG = {"plugin": "DiscordSRV", "channel": "#bridge", "on_process_exit": "relaunch mrrw chat",
                 "mirror_source": "mrrw.db", "voice_clips": "/srv/support/voicemail/",
                 "added": "2026-08-02", "added_by": "tvale"}

ROBOTS_TXT = "User-agent: *\nDisallow: /forum-import/\nDisallow: /~mossfield/\nDisallow: /tcadmin/legacy/\n"

# The status board's other incidents, so 6118 sits among ordinary ones. The 2 Sept 2011 reboot is
# the night the @reboot watchdog brought mrrw back.
STATUS_ARCHIVE = [
    {"id": "5870", "title": "Chicago core switch failure", "posted": "2011-02-02 14:10 CST",
     "body": "A core switch in Chicago failed at 13:40 CST. Affected services are being moved to the standby switch.",
     "update": "2011-02-03 09:05 CST - Resolved. Replacement switch installed overnight."},
    {"id": "6402", "title": "Scheduled maintenance: Chicago game nodes", "posted": "2011-09-01 17:00 CDT",
     "body": "Kernel updates on game nodes CHI-05 to CHI-07, 2 September 04:00-05:00 CDT. Each node will reboot once.",
     "update": "2011-09-02 04:52 CDT - Complete."},
]

# Copperline's own history, for the mirror's About page (2008-2014).
COPPERLINE_HISTORY = [
    ("2008-02", "Copperline Hosting LLC founded in Chicago by Dale Kessler. Two racks at 350 E Cermak."),
    ("2009-01", "Theo Vale joins as support lead (the only support)."),
    ("2009-03", "Iona Bell joins as systems administrator."),
    ("2010-04", "Game Servers launched: Minecraft Starter and Builder plans on nodes CHI-05 to CHI-07."),
    ("2011-02", "Chicago core switch failure; two days of outages."),
    ("2011-04", "Storage array 2 fails at 02:51 on 21 April. Recovery case 6118 (Mossfield) opened."),
    ("2011-08", "Iona Bell leaves Copperline."),
    ("2012-05", "Community and Network plans added. Leased game nodes in Dallas and Atlanta."),
    ("2013-11", "Game Servers closed to new orders."),
    ("2014-10", "Notice of closure sent to all clients: services end 30 November 2014."),
    ("2014-12", "Assets sold at auction by Lakeshore Liquidators, 9 December 2014."),
    ("2026-08", "This read-only mirror put up by the archivist. Records for case 6118 kept until 31 Dec 2026."),
]


def heartbeat_log(month: str) -> str:
    """/var/log/mrrw/heartbeat.log, rotated monthly: cron runs /opt/mrrw/bin/heartbeat every five
    minutes and it logs 'ok pid up' or 'down'. Two excerpts live in Theo's box (Lot 14 came with its
    drives): May 2011 (the 06:17 kill, the 06:44 restart, the 11:14 stop) and March 2019 (Theo
    switched it off forty-three minutes after Kit's email, then back on)."""
    from datetime import datetime, timedelta
    if month == "2011-05":
        t0, t1, tz = datetime(2011, 5, 18, 2, 0), datetime(2011, 5, 18, 12, 0), "-0500"
        runs = [(datetime(2011, 5, 18, 2, 0, 0), datetime(2011, 5, 18, 6, 17, 22), 14022),
                (datetime(2011, 5, 18, 6, 44, 10), datetime(2011, 5, 18, 11, 14, 2), 14310)]
    elif month == "2019-03":
        t0, t1, tz = datetime(2019, 3, 2, 18, 0), datetime(2019, 3, 3, 6, 0), "-0600"
        runs = [(datetime(2014, 12, 14, 3, 12, 38), datetime(2019, 3, 2, 22, 51, 4), 2271),
                (datetime(2019, 3, 2, 23, 34, 10), datetime(2030, 1, 1), 31877)]
    else:
        raise KeyError(month)
    out = []
    t = t0
    while t <= t1:
        state = "down"
        for start, stop, pid in runs:
            if start <= t < stop:
                up = t - start
                state = "ok pid=%d up=%dd%02d:%02d:%02d" % (pid, up.days, up.seconds // 3600, (up.seconds // 60) % 60, up.seconds % 60)
        out.append("%s %s mrrw-01 %s" % (t.strftime("%Y-%m-%d %H:%M:%S"), tz, state))
        t += timedelta(minutes=5)
    return "\n".join(out) + "\n"


def _clean_md5() -> str:
    from .files import backup_zip, md5_bytes
    return md5_bytes(backup_zip(False))


def checklist() -> str:
    """ops/6118-checklist.txt: Iona's self-check hashes (Act 5 onward)."""
    return "\n".join([
        "6118 checklist - IB",
        "",
        "md5 plaintext A (rook floor):  " + ciphers.md5(ciphers.FLOOR_PLAINTEXT),
        "md5 token B (my key):          " + ciphers.md5(ciphers.IONA_TOKEN),
        "md5 command (annex wall):      " + ciphers.md5(ciphers.HALT_COMMAND),
        "md5 clean backup (04-20 zip):  " + _clean_md5(),
        "",
        "don't let it restart",
        "",
    ])
