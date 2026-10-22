"""Media the user captures or records, defined once so the site and the shot list agree.

Screenshot filenames are the photographer's LOCAL time (Beta's F2 naming:
YYYY-MM-DD_HH.MM.SS.png). The Apache listing on the mirror shows server (Chicago) mtime,
which is how sharp players confirm June's +2 offset.
"""

from __future__ import annotations

from datetime import datetime
from zoneinfo import ZoneInfo

CHI = ZoneInfo("America/Chicago")
HFX = ZoneInfo("America/Halifax")


def june(chicago: str) -> dict:
    at = datetime.fromisoformat(chicago).replace(tzinfo=CHI)
    return {"filename": at.astimezone(HFX).strftime("%Y-%m-%d_%H.%M.%S") + ".png",
            "server_mtime": at.strftime("%d-%b-%Y %H:%M"), "chicago": chicago}


JUNE_SHOTS = [
    {**june("2011-03-02 17:20:11"), "act": 3, "shows":
     "June's first build on the new map: the little house on the point, tower base two blocks high, no top."},
    {**june("2011-03-30 18:44:05"), "act": 3, "shows":
     "The old wooden dock east of her house, sunset. Dock and shoreline clearly visible (needed for Act 7)."},
    {**june("2011-05-14 19:40:02"), "act": 3, "shows":
     "Group shot at spawn around a cake. The three ORIGINAL rules signs are readable behind them, 'b4' visible."},
    {**june("2011-05-15 20:10:30"), "act": 3, "shows":
     "Her tower, finished to the gallery, open top, no glass roof. Square full moon low in the NORTH sky."},
    {**june("2011-05-17 16:02:40"), "act": 3, "shows":
     "cairn's sheep pen from the hill, the week of 16 May: six dyed sheep, four in the inner pen (census backup)."},
    {**june("2011-05-18 04:12:40"), "act": 3, "shows":
     "Her tower with a glass roof half placed. Nobody near it. Blurry figure on the dock."},
    {**june("2011-05-18 04:14:33"), "act": 3, "shows":
     "Same angle, roof nearly done (seven panes of nine), a glowstone now in the top. The dock figure is gone."},
    {**june("2011-05-18 04:16:20"), "act": 3, "shows":
     "Roof complete. Chat visible: 'it put a roof on it'."},
    {**june("2011-05-20 18:30:00"), "act": 3, "shows":
     "The finished tower in daylight, glass roof, lamp. Her caption file: 'not mine'."},
]

FORGED_SHOTS = [  # Morrow's renders, added to the gallery in Act 6 (D10)
    {"filename": "2011-05-17_22.05.12.png", "act": 6, "shows":
     "lowpath standing on June's dock at night. Moon is a waxing CRESCENT and rises in the EAST: impossible in Beta 1.5."},
]

RECORDINGS = [
    {"file": "moss_0518.ogg", "act": 5, "owner": "patchcord", "length": "about 21 min (most of it dead air)",
     "created_local": "2011-05-18 11:58 BST", "chicago": "2011-05-18 05:58 CDT",
     "needs": "5 voices: Rookery, cairn, June, patchcord, Iona from 06:01. No Kit (logged off 04:30), no Theo."},
    {"file": "MIC-5-18-B.WAV", "act": 8, "owner": "patchcord", "length": "about 3 min",
     "created_local": "2011-05-18 12:21 BST", "chicago": "2011-05-18 06:21 CDT",
     "needs": "Room tone of patchcord's flat, faint Mumble in headphones, Theo saying 'give it till morning'."},
    {"file": "theo_voicemail_2011.wav", "act": 4, "owner": "Theo", "length": "about 50 s",
     "created_local": "2011-04-28", "chicago": "2011-04-28",
     "needs": "Record THROUGH A REAL PHONE LINE (call your own voicemail). Phone-band audio is evidence in Act 9."},
]


# Act 5: /~patchcord/mumble/ (password: patchcord's tune). Mumble only got its built-in recorder in
# 1.2.3 (19 Feb 2011), so nothing here is older than that. Files the owner records go in
# dashboard/private-media/media/patchcord/. moss_0518.ogg is listed at 0 bytes: "not retained" --
# Theo has the only copy (Archive Desk, by exact filename).
PATCHCORD_FOLDER = [
    {"file": "moss_0224.ogg", "mtime": "24-Feb-2011 21:07", "size": "1.2M", "note": "first go with the new recorder, patch and rook"},
    {"file": "moss_0313.ogg", "mtime": "13-Mar-2011 19:52", "size": "3.8M", "note": "june and cairn arguing about sheep"},
    {"file": "moss_0412.ogg", "mtime": "12-Apr-2011 20:41", "size": "2.6M", "note": "finch explaining boosters to kit"},
    {"file": "moss_0518.ogg", "mtime": "18-May-2011 06:21", "size": "0", "note": "listed, not retained"},
]

# Act 6: /~tvale/ (Theo's staff web space, linked from his forum profile).
TVALE_FOLDER = [
    {"file": "panel_ok.jpg", "mtime": "18-May-2011 06:46", "size": "61K"},
    {"file": "macros.txt", "mtime": "02-Mar-2011 10:15", "size": "2K"},
]


# Act 2: Rookery's hand-written map key, kept next to the old map tiles at /~mossfield/dynmap/.
# 2011 Dynmap had no marker API (markers arrived in September 2011), so Rook kept a text file.
# "low path" is a note in it, the most-walked path in town. That's where Morrow got the name.
def _mapkey_lines() -> list[str]:
    from . import layout
    A = layout.ANCHORS
    LOWPATH_GROUND = layout.LOWPATH_GROUND

    def row(label, key, note=""):
        x, y, z = A[key]
        return (("%s %s %s" % (label, "." * max(2, 18 - len(label)), "%d %d %d" % (x, y, z))).ljust(36) + note).rstrip()

    return ["mossfield map key (rook). coords are x y z (F3)", "",
            row("spawn", "spawn", "rules signs, cake table"), row("rook's", "rookery_house", "boots off"),
            row("storehouse", "cairn_storehouse_door_2011", "cairn's. dont touch the chests"),
            row("farms", "cairn_farm"), row("sheep", "sheep_pen"), row("june's point", "june_house"),
            row("the tower (wip)", "june_tower"), row("patch's hut", "patchcord_relay_hut"),
            row("rail hub", "finch_hub", "finch"), "",
            "low path: the track along the bottom of the hill, round about %d %d %d. everyone walks it, "
            "dont build on it" % (A["lowpath_place"][0], LOWPATH_GROUND, A["lowpath_place"][2] - 4)]  # 2011: no hill yet


MAPKEY = {"file": "mapkey.txt", "mtime": "06-Mar-2011 22:41", "size": "1K", "lines": _mapkey_lines()}

# Act 2: Rook's web folder also kept a copy of the server config. Beta's server.properties keeps the
# seed exactly as it was typed; the world itself stores the number (level.dat RandomSeed).
SERVER_PROPERTIES = "\n".join([
    "#Minecraft server properties",
    "#Tue Feb 22 23:41:09 CST 2011",
    "level-name=Mossfield",
    "view-distance=10",
    "spawn-monsters=true",
    "online-mode=true",
    "spawn-animals=true",
    "max-players=20",
    "server-ip=",
    "pvp=false",
    "level-seed=bracken",
    "server-port=25565",
    "",
])
