"""Mossfield server.log excerpts (CraftBukkit, Beta 1.5_02). Chicago local time.

The mirror publishes selected days at /~mossfield/logs/. Private /msg lines are NOT
in the public excerpts; they exist only in Morrow's corpus (which is how players learn
in Act 4 that Morrow has more data than the site).
IPs use RFC 5737 documentation ranges only.
"""

from __future__ import annotations

import hashlib

from . import ciphers

IPS = {"rookery": "198.51.100.23", "cairn": "203.0.113.41", "finchline": "198.51.100.77",
       "juni_lantern": "203.0.113.9", "patchcord": "192.0.2.150", "kitbag": "198.51.100.140",
       "i_bell": "192.0.2.10"}

# Client source ports are whatever the player's OS handed out: XP machines use 1025-5000, Vista/7
# and Macs use 49152-65535. Stable per (player, time) so every build prints the same log.
EPHEMERAL = {"rookery": (49152, 65535), "cairn": (1025, 5000), "finchline": (49152, 65535),
             "juni_lantern": (49152, 65535), "patchcord": (49152, 65535), "kitbag": (1025, 5000),
             "i_bell": (32768, 61000)}


def _port(who: str, t: str) -> int:
    lo, hi = EPHEMERAL[who]
    n = int(hashlib.md5(("%s %s" % (who, t)).encode()).hexdigest()[:8], 16)
    return lo + n % (hi - lo)


def login(t, who, eid, xyz):
    """eid is the server's running entity counter: it climbs with every mob and dropped item."""
    return (t, "INFO", "%s [/%s:%d] logged in with entity id %d at (%s)" % (who, IPS[who], _port(who, t), eid, xyz))


def quit_(t, who):
    return (t, "INFO", "%s lost connection: disconnect.quitting" % who)


def chat(t, who, text):
    return (t, "INFO", "<%s> %s" % (who, text))


def morrow(t, text):
    return (t, "INFO", "[morrow] %s" % text)


def info(t, text):
    return (t, "INFO", text)


DAYS = {
    "2011-05-12": [
        login("05:02:11", "cairn", 288417, "212.5, 68.0, -41.5"),
        chat("05:02:40", "cairn", "morning"),
        chat("05:20:03", "cairn", "cairn is at the farm if anyone needs seeds"),
        login("06:48:19", "juni_lantern", 291960, "301.2, 71.0, 88.7"),
        chat("06:49:02", "juni_lantern", "morning cairn! sunrise from the point is unreal today"),
        chat("06:51:44", "cairn", "the bed is gone from the shed?? who took the bed from the shed"),
        chat("06:52:10", "juni_lantern", "not me"),
        chat("06:58:31", "cairn", "fine. it can stay gone"),
        quit_("07:30:02", "cairn"),
    ],
    "2011-05-18": [
        morrow("02:00:00", "restore pass started (window 02:00-04:00)"),
        login("03:59:12", "rookery", 1203116, "0.5, 64.0, 0.5"),
        chat("04:00:31", "rookery", "!status"),
        morrow("04:00:31", "restore 61%. no worries at all. back by tomorrow"),
        login("04:01:55", "juni_lantern", 1203550, "301.0, 71.0, 88.0"),
        chat("04:02:20", "rookery", "everyone get off for a bit"),
        login("04:04:12", "cairn", 1204331, "214.0, 68.0, -40.2"),
        chat("04:05:47", "cairn", "theres someone at the farm"),
        login("04:08:03", "kitbag", 1205012, "40.5, 64.0, 12.5"),
        chat("04:11:16", "cairn", "why is there another me at the farm"),
        chat("04:12:09", "juni_lantern", "something is putting glass on my tower"),
        chat("04:14:50", "kitbag", "rook is by the cake"),
        chat("04:14:58", "rookery", "im at spawn kit"),
        chat("04:16:02", "juni_lantern", "it put a roof on it"),
        login("04:19:30", "patchcord", 1206874, "-72.0, 70.0, 140.0"),
        chat("04:20:12", "patchcord", "what am I looking at"),
        chat("04:22:40", "rookery", "if it goes bad again i want a way to stop it myself"),
        quit_("04:30:51", "kitbag"),
        chat("05:12:08", "patchcord", "it's using theo's voice in chat now"),
        morrow("05:12:15", "cairn is at the farm. no worries at all"),
        chat("05:58:02", "patchcord", "recording this, saving as moss_0518"),
        login("06:01:40", "i_bell", 1213390, "301.0, 64.0, 92.0"),
        chat("06:02:31", "i_bell", "im putting a key somewhere theo cant reach. later"),
        chat("06:09:17", "patchcord", ciphers.build_slipped_line()),
        chat("06:09:30", "juni_lantern", "patch ur hands are on the wrong keys lol"),
        quit_("06:12:44", "i_bell"),
        morrow("06:17:22", "SIGTERM received (uptime 04:17:22). stopping"),
        quit_("06:18:05", "juni_lantern"),
        quit_("06:20:59", "patchcord"),
        quit_("06:21:40", "cairn"),
        morrow("06:44:10", "restore pass resumed (session queue: 3 open)"),
        chat("07:01:12", "rookery", "its moving again"),
        quit_("07:03:30", "rookery"),
        morrow("11:14:02", "SIGTERM received (uptime 04:29:52). stopping"),
    ],
    "2011-06-11": [
        login("18:51:22", "finchline", 87420, "-138.5, 63.0, -258.5"),
        quit_("20:31:05", "finchline"),
        login("20:55:01", "finchline", 88213, "-30.0, 66.0, -12.0"),
        chat("21:03:10", "finchline", "taking the spur home. 40 sec from the hub with the new rails"),
        chat("21:03:58", "finchline", "made it. nobody knows this route lol"),
        quit_("21:20:40", "finchline"),
    ],
    "2011-09-02": [  # Dale rebooted the node for a kernel update; the @reboot watchdog came back with it
        info("04:31:07", "Starting minecraft server version Beta 1.7.3"),
        info("04:31:07", "Loading properties"),
        info("04:31:08", "Starting Minecraft server on *:25565"),
        info("04:31:08", "Preparing level \"Mossfield\""),
        info("04:31:14", "Done (6.112s)! For help, type \"help\" or \"?\""),
        morrow("04:31:40", "restore pass resumed (watchdog @reboot)"),
        morrow("04:31:41", "session queue: 3 open"),
        morrow("05:40:12", "214 blocks normalised (cairn storehouse)"),
    ],
    "2011-11-18": [  # 1.0 comes out this afternoon; cairn, off his shift, looks in first
        login("05:12:02", "cairn", 41902, "214.0, 68.0, -40.2"),
        chat("05:13:30", "cairn", "hi"),
        quit_("05:18:11", "cairn"),
    ],
}

# Private messages Morrow ingested (never published as logs). Morrow can quote them.
PRIVATE_MSGS = [
    ("2011-05-18 05:58:40", "patchcord", "juni_lantern", "recording this for later. remind me to send you a copy"),
    ("2011-05-18 06:03:10", "juni_lantern", "patchcord", "send it to me after? I want to hear it"),
]


def render(day: str) -> str:
    return "\n".join("%s %s [%s] %s" % (day, t, lvl, msg) for t, lvl, msg in DAYS[day]) + "\n"
