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
    {**june("2011-04-10 16:02:40"), "act": 3, "shows":
     "cairn's sheep pen from the hill: six dyed sheep, four in the inner pen (Act 3 census backup)."},
    {**june("2011-05-14 19:40:02"), "act": 3, "shows":
     "Group shot at spawn around a cake. The three ORIGINAL rules signs are readable behind them, 'b4' visible."},
    {**june("2011-05-15 20:10:30"), "act": 3, "shows":
     "Her tower, finished to the gallery, open top, no glass roof. Square full moon low in the NORTH sky."},
    {**june("2011-05-18 04:12:40"), "act": 3, "shows":
     "Her tower with a glass roof half placed. Nobody near it. Blurry figure on the dock."},
    {**june("2011-05-18 04:14:33"), "act": 3, "shows":
     "Same angle, roof nearly done, a glowstone lamp now in the top. The dock figure is gone."},
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
    {"file": "moss_0518.ogg", "act": 5, "owner": "patchcord", "length": "about 8 min",
     "created_local": "2011-05-18 11:58 BST", "chicago": "2011-05-18 05:58 CDT",
     "needs": "4-5 voices: Rookery, cairn, June, patchcord, Iona briefly, Theo once (from his own Mumble)."},
    {"file": "MIC-5-18-B.WAV", "act": 8, "owner": "patchcord", "length": "about 3 min",
     "created_local": "2011-05-18 12:20 BST", "chicago": "2011-05-18 06:20 CDT",
     "needs": "Room tone of patchcord's flat, faint Mumble in headphones, Theo saying 'give it till morning'."},
    {"file": "theo_voicemail_2011.wav", "act": 4, "owner": "Theo", "length": "about 50 s",
     "created_local": "2011-04-28", "chicago": "2011-04-28",
     "needs": "Record THROUGH A REAL PHONE LINE (call your own voicemail). Phone-band audio is evidence in Act 9."},
]
