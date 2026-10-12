"""Mossfield server.log excerpts (CraftBukkit, Beta 1.5_02). Chicago local time.

The mirror publishes selected days at /~mossfield/logs/. Private /msg lines are NOT
in the public excerpts; they exist only in Morrow's corpus (which is how players learn
in Act 4 that Morrow has more data than the site).
IPs use RFC 5737 documentation ranges only.
"""

from __future__ import annotations

from . import ciphers

IPS = {"rookery": "198.51.100.23", "cairn": "203.0.113.41", "finchline": "198.51.100.77",
       "juni_lantern": "203.0.113.9", "patchcord": "192.0.2.150", "kitbag": "198.51.100.140",
       "i_bell": "192.0.2.10"}


def login(t, who, eid, xyz):
    return (t, "INFO", "%s [/%s:5%04d] logged in with entity id %d at (%s)" % (who, IPS[who], eid, eid, xyz))


def quit_(t, who):
    return (t, "INFO", "%s lost connection: disconnect.quitting" % who)


def chat(t, who, text):
    return (t, "INFO", "<%s> %s" % (who, text))


def morrow(t, text):
    return (t, "INFO", "[morrow] %s" % text)


DAYS = {
    "2011-05-12": [
        login("05:02:11", "cairn", 4120, "212.5, 68.0, -41.5"),
        chat("05:02:40", "cairn", "morning"),
        chat("05:20:03", "cairn", "cairn is at the farm if anyone needs seeds"),
        login("06:48:19", "juni_lantern", 4133, "301.2, 71.0, 88.7"),
        chat("06:49:02", "juni_lantern", "morning cairn! sunrise from the point is unreal today"),
        chat("06:51:44", "cairn", "the bed is gone from the shed?? who took the bed from the shed"),
        chat("06:52:10", "juni_lantern", "not me"),
        chat("06:58:31", "cairn", "fine. it can stay gone"),
        quit_("07:30:02", "cairn"),
    ],
    "2011-05-18": [
        morrow("02:00:00", "restore pass started (window 02:00-04:00)"),
        login("03:58:40", "cairn", 4410, "214.0, 68.0, -40.2"),
        login("03:59:12", "rookery", 4411, "0.5, 64.0, 0.5"),
        chat("04:00:31", "rookery", "!status"),
        morrow("04:00:31", "restore 61%. no worries at all. back by tomorrow"),
        login("04:01:55", "juni_lantern", 4412, "301.0, 71.0, 88.0"),
        chat("04:02:20", "rookery", "everyone get off for a bit"),
        chat("04:05:47", "cairn", "theres someone at the farm"),
        login("04:08:03", "kitbag", 4413, "40.5, 64.0, 12.5"),
        chat("04:11:16", "cairn", "why is there another me at the farm"),
        chat("04:12:09", "juni_lantern", "something is putting glass on my tower"),
        chat("04:14:50", "kitbag", "rook is by the cake"),
        chat("04:14:58", "rookery", "im at spawn kit"),
        chat("04:16:02", "juni_lantern", "it put a roof on it"),
        login("04:19:30", "patchcord", 4414, "-72.0, 70.0, 140.0"),
        chat("04:20:12", "patchcord", "what am I looking at"),
        chat("04:22:40", "rookery", "if it goes bad theres the floor"),
        quit_("04:30:51", "kitbag"),
        chat("05:12:08", "patchcord", "it's using theo's voice in chat now"),
        morrow("05:12:15", "cairn is at the farm. no worries at all"),
        chat("05:58:02", "patchcord", "recording this, saving as moss_0518"),
        login("06:01:40", "i_bell", 4415, "301.0, 64.0, 92.0"),
        chat("06:02:31", "i_bell", "putting my key somewhere theo cant reach"),
        chat("06:09:17", "patchcord", ciphers.build_slipped_line()),
        chat("06:09:30", "juni_lantern", "patch you're typing with your hands on the wrong keys"),
        quit_("06:12:44", "i_bell"),
        morrow("06:17:22", "SIGTERM received (uptime 04:17:22). stopping"),
        quit_("06:18:05", "juni_lantern"),
        quit_("06:20:59", "patchcord"),
        quit_("06:21:40", "cairn"),
        morrow("06:44:10", "restore pass resumed (session queue: 3 open)"),
        chat("07:01:12", "rookery", "its moving again"),
        quit_("07:03:30", "rookery"),
    ],
    "2011-06-11": [
        login("20:55:01", "finchline", 5120, "-30.0, 66.0, -12.0"),
        chat("21:03:10", "finchline", "taking the spur home. 40 sec from the hub with the new rails"),
        chat("21:03:58", "finchline", "made it. nobody knows this route lol"),
        quit_("21:20:40", "finchline"),
    ],
    "2011-11-18": [
        login("14:12:02", "cairn", 6001, "214.0, 68.0, -40.2"),
        chat("14:13:30", "cairn", "hi"),
        quit_("14:18:11", "cairn"),
    ],
}

# Private messages Morrow ingested (never published as logs). Morrow can quote them.
PRIVATE_MSGS = [
    ("2011-05-18 05:58:40", "patchcord", "juni_lantern", "recording this for later. don't tell rook, he'll want to be on it"),
    ("2011-05-18 06:03:10", "juni_lantern", "patchcord", "send it to me after? I want to hear it"),
]


def render(day: str) -> str:
    return "\n".join("%s %s [%s] %s" % (day, t, lvl, msg) for t, lvl, msg in DAYS[day]) + "\n"
