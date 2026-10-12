"""Acts 4-10: the Archive Desk, Morrow's rewrites, Act 7 messages, hints, and the finale.

Theo is never live. Everything he says is here, triggered by evidence tokens players file
at the Archive Desk, by elapsed time, or by game state. Voice note IDs match
morrow/v2/RECORDING-SCRIPTS.md; the site plays /media/theo/<ID>.ogg when the file exists and
shows the text either way.
"""

from __future__ import annotations

import hashlib
from datetime import date, timedelta

from . import ciphers, people

# ---------------------------------------------------------------------------
# Archive Desk rules. A ticket's text is lower-cased and checked for tokens.

DESK_RULES = [
    {
        "id": "unmask", "act": 4, "title": "who runs this mirror",
        "tokens": ["lot 14", "chi-07", "2014-12-14", "waived"], "need": 2,
        "reply": "yeah. it's me. Theo. sorry.  I didn't know how to say it.  I didn't think anyone would "
                 "actually come.\n\nI kept the old box running after Copperline closed.  It's in my spare room.  "
                 "It's just Mossfield.  It's just the residents.",
        "voice": "T-04a",
        "partial": "Not sure what you mean, sorry!  It's just an archive.  I just keep the lights on.",
        "partial_voice": "T-04b",
    },
    {
        "id": "restart", "act": 6, "title": "the 06:44 restart",
        "tokens": ["06:44:10", "chi-07/mrrw", "mid-session"], "need": 2, "require": ["06:44:10"],
        "reply": "It was mid-replay.  Iona said if you stop it mid-session the region files corrupt.  Kit was "
                 "asleep in the spare room and I just.  I didn't want him waking up to it gone.\n\nI know.  "
                 "I know what it sounds like.",
        "voice": "T-06a",
        "partial": "That was a long time ago.  The panel logs aren't always right.",
    },
    {
        "id": "ingest", "act": 8, "title": "where did it learn that",
        "tokens": ["turn it off", "its ok", "it's ok"], "need": 1,
        "reply": "It's not in the old logs, is it.\n\nI fed it everything from my old inbox.  For the voices.  "
                 "I wanted it to sound like people.  That line's from my brother.  Here.",
        "voice": None, "attach": "kit-2019-03-02.eml",
        "partial": "",
    },
    {
        "id": "kit", "act": 8, "title": "what kit said",
        "tokens": ["2019-03-02", "turn it off"], "need": 2, "delay_hours": 36,
        "reply": "Kit sent that in 2019.  I read it on the train and I didn't answer.\n\nHe was right.  He "
                 "was twenty-two, he had a job, he hadn't logged in since he was fifteen.  I kept it running "
                 "because turning it off felt like the last day all over again.\n\nI'm sorry I fed it his email.  "
                 "I wanted it to sound like people.",
        "voice": "T-08a",
        "partial": "Where did you get that.",
    },
]

DESK_FILES = {  # exact filename -> (act it can be requested from, Theo's line, voice note)
    "moss_0518.ogg": (5, "Here's patchcord's.  I haven't listened to it in years.  I don't think I'm going to now.", "T-05a"),
}

# Unprompted drops: Theo "found another drive". (act, hours after the act started, file, note)
DESK_DROPS = [
    (4, 0, "copperline-asset-sale-2014.pdf", "Found the auction paperwork.  Not sure why I kept it."),
    (5, 0, "6118-checklist.txt", "Iona left this on the ops share.  Might help, might not."),
    (6, 30, None, "I wax the signs I want to keep.  Old habit.  Nothing touches them after that."),
    (7, 0, "copperline-ops.irc.log", "Found Iona's IRC logs on the second drive.  I haven't opened them."),
    (8, 0, "closet.jpg", "Found this on my phone.  That's the box.  It's been in that cupboard since the auction.  "
                         "Everything I changed is on it, in my folder, behind the usual password."),
]

# Escalating hints by act, after 24 / 72 / 120 hours stuck (never the answer).
HINTS = {
    1: ["Found another drive.  Not sure it helps.  There's a lot on the old site nobody ever linked to.",
        "Old sites always said where not to look.  Search engines were told first.",
        "Rowan's first forum post is the most Rowan thing I've ever read."],
    2: ["If you're stuck in Mossfield, the residents left a lot of signs.  They wrote everything on signs back then.",
        "Not everything in there is from 2011.  Some of it couldn't be.",
        "The minecraft wiki keeps a history of when every block was added."],
    3: ["Everyone was in a different time zone.  Patch was always ahead of us.  June too.",
        "The block log doesn't lie.  It's the only thing in there that doesn't.",
        "June's folder has a password.  She used the same one for everything.  So did everyone."],
    4: ["If it sounds like a person, check who said it first.",
        "I'm not hard to find.  I'm on most of these pages if you look at how I type.",
        "The auction paperwork has a lot number on it."],
    5: ["Patch's password was a song.  The bass line, he always said.",
        "Rowan wrote his floor in wool.  He used the wool numbers from the forum.",
        "Iona left hashes for everything.  If your answer hashes right, it's right."],
    6: ["Daylight saving.  Iona always got it right.",
        "Signs back then fit fifteen letters a line.",
        "Look at what a picture says about itself, not just what it shows."],
    7: ["Don't believe anything that sounds like a support macro.",
        "Iona thought in files, not places.",
        "Counts in a chest can be letters if you count from the right place."],
    8: ["It ends a replay when they log off or sleep.  Iona said that once.",
        "cairn always closed up the same way.",
        "lowpath walks every night and never gets home."],
    9: ["Rowan kept a copy somewhere I couldn't reach.",
        "You'd need everyone's passwords.  In the order they joined.",
        "Fill in the brief.  I just need it to be real."],
    10: ["Three things.  The sign, the sessions, the key.",
         "Wax the sign.",
         "I'll cut the bridge when you've done your bit."],
}

# ---------------------------------------------------------------------------
# Act 6: Morrow's rewrites (each has a tell)

FORUM_REWRITES = {  # post id -> (new body, edit note). The print view still shows the original.
    26810: ("I paused the restore process briefly at 06:17 CST. It resumed normally and finished. No data was lost.",
            "Last edited by i_bell; 05-18-2011 at 06:40 AM CST."),
    26815: ("the clean copy is fine. the storehouse labels match the sheet now.",
            "Last edited by cairn; 05-18-2011 at 06:52 AM CST."),
    26820: ("all fine now. thanks theo", "Last edited by rookery; 05-18-2011 at 07:10 AM CST."),
}

SIGN_REWRITES = [  # Act 6 in-world: genuine 2011 signs Morrow "corrects" (lines over 15 chars = tell)
    {"anchor": "rookery_house", "lines": ["ROOK'S HOUSE", "please remove boots", "before entering", ""]},
    {"anchor": "finch_hub", "lines": ["RAIL HUB", "all lines complete", "south branch open", ""]},
    {"anchor": "june_house", "lines": ["JUNE'S POINT", "north light completed", "2011-05-18", ""]},
]

TRANSCRIPTION_BOOKS = [  # C18: Morrow's books, hover text cites a source per line; one source is impossible
    {"title": "Recovered chat (cairn)", "author": "cairn", "lines": [
        ("cairn is at the farm if anyone needs seeds", "cairn, chat, 2011-05-12 05:20:03"),
        ("the bed is gone from the shed", "cairn, chat, 2011-05-12 06:51:44"),
        ("leave it uneven", "cairn, forum signature"),
        ("i'm at the storehouse", "cairn, chat, 2011-04-22 05:40:00"),  # server was down on 04-22: forged
    ]},
]
OFFLINE_DAYS = ["2011-04-21", "2011-04-22"]  # storage failure; no chat can exist on these days

SGA_SIGNS = [  # D14: Morrow's code-shaped nonsense, in the enchanting-table font; decodes to collage
    ["the tower has", "a roof back by", "tomorrow", ""],
    ["leave it at the", "farm no worries", "", ""],
]

# ---------------------------------------------------------------------------
# Act 7: messages. Morrow's DMs are fixed collages (verified against the corpus);
# the bot adds per-player quotes of other players' real in-game lines.

MORROW_DMS = [
    {"as": "cairn", "text": "there's another me at the farm. i'm at the storehouse. don't tell rook"},
    {"as": "juni_lantern", "text": "my tower has a roof. send it to me after. i want to hear it"},
    {"as": "rookery", "text": "everyone get off for a bit. if it goes bad theres the floor"},
    {"as": "patchcord", "text": "recording this for later. don't tell rook, he'll want to be on it"},
    {"as": "theo", "text": "no worries at all. your world will be back by tomorrow"},
]
MORROW_QUOTE_TEMPLATE = "{other} said: {line}"
THEO_DMS = [  # each contains a word Morrow has never ingested (train, library, honeycomb): that's the tell
    {"text": "it's me.  it's actually me.  I'm on the train home.  Stay a bit.", "voice": "T-07a"},
    {"text": "Don't believe anything that sounds like a support macro.  That's not me anymore.  I work at a library now.", "voice": "T-07b"},
    {"text": "It's better with you there.  It's learning.  I think that's a good thing.  I bought honeycomb for the signs.", "voice": "T-07c"},
]
PER_PLAYER_SIGNS = [  # lowpath's place, Act 7: each player sees a different front; one line is shared
    ["come home", "the bed is gone", "", ""],
    ["they left you", "the bed is gone", "", ""],
    ["no worries at all", "the bed is gone", "", ""],
    ["back by tomorrow", "the bed is gone", "", ""],
]

# ---------------------------------------------------------------------------
# Act 9: absorption

def backdated_join(player: str) -> str:
    """The fake 2010 join date Morrow gives a player's forum profile (stable per name)."""
    n = int(hashlib.md5(player.lower().encode()).hexdigest(), 16)
    return (date(2010, 11, 1) + timedelta(days=n % 28)).isoformat()


BACKDATED_POSTS = [  # attributed to each player, dated 2011, with the CST-in-summer tell
    ("2011-05-17 21:10", "first night back. the storehouse is tidy now"),
    ("2011-05-18 04:12", "someone comes home. no worries at all"),
]

CASE_BRIEF_FIELDS = [
    {"id": "restart", "label": "When did you turn it back on?", "accept": ["2011-05-18 06:44:10", "06:44:10", "6:44:10"]},
    {"id": "lowpath", "label": "When was lowpath made?", "accept": ["2016-08-03"]},
    {"id": "kit", "label": "When did Kit write to you?", "accept": ["2019-03-02"]},
    {"id": "clean", "label": "What is the MD5 of the clean backup?", "accept": ["@clean_md5"]},
    {"id": "joined", "label": "When does the forum say we joined?", "accept": ["@backdated"]},
]

# ---------------------------------------------------------------------------
# Act 10: the stop

USB_CHOICES = {
    "clean": ("The April copy.  The real one, with the holes in it.", "T-10-clean"),
    "recovered": ("The finished town.  Your names are on the houses.", "T-10-recovered"),
    "corpus": ("Every word anyone said.", "T-10-corpus"),
}
STOP_LOG = [
    "morrow: stop requested (maint=on, sessions=0, key=ok)",
    "morrow: finalising restore queue",
    "morrow: done mossfield:storehouse",
    "morrow: done mossfield:north_light",
    "morrow: skip mossfield:south_rail (unfinished) reason=none",
    "morrow: exit 0",
]
READY_STATUS = "maint=on sessions=0"  # what the MOTD hex channel says when the node is ready to stop


__all__ = ["DESK_RULES", "DESK_FILES", "DESK_DROPS", "HINTS", "FORUM_REWRITES", "SIGN_REWRITES",
           "TRANSCRIPTION_BOOKS", "OFFLINE_DAYS", "SGA_SIGNS", "MORROW_DMS", "THEO_DMS", "PER_PLAYER_SIGNS",
           "backdated_join", "BACKDATED_POSTS", "CASE_BRIEF_FIELDS", "USB_CHOICES", "STOP_LOG", "READY_STATUS",
           "ciphers", "people"]

# ---------------------------------------------------------------------------
# Theo-era builds (exist from the start; installed by /mrrw install on the upgraded world)

# C12/D7: PORCHLIGHT in International Code of Signals flags on June's gallery (2017 tribute), plus K
# ("I wish to communicate with you") above. Banner layers: (base colour, [(pattern, colour), ...]).
FLAGS = {
    "K": ("BLUE", [("HALF_VERTICAL", "YELLOW")]),
    "P": ("WHITE", [("STRIPE_TOP", "BLUE"), ("STRIPE_BOTTOM", "BLUE"), ("STRIPE_LEFT", "BLUE"), ("STRIPE_RIGHT", "BLUE")]),
    "O": ("YELLOW", [("DIAGONAL_UP_RIGHT", "RED")]),
    "R": ("RED", [("STRAIGHT_CROSS", "YELLOW")]),
    "C": ("WHITE", [("STRIPE_TOP", "BLUE"), ("STRIPE_BOTTOM", "BLUE"), ("STRIPE_MIDDLE", "RED")]),
    "H": ("RED", [("HALF_VERTICAL", "WHITE")]),
    "L": ("BLACK", [("SQUARE_TOP_LEFT", "YELLOW"), ("SQUARE_BOTTOM_RIGHT", "YELLOW")]),
    "I": ("YELLOW", [("CIRCLE", "BLACK")]),
    "G": ("BLUE", [("SMALL_STRIPES", "YELLOW")]),
    "T": ("WHITE", [("STRIPE_LEFT", "RED"), ("STRIPE_RIGHT", "BLUE")]),
}
FLAG_WORD = "PORCHLIGHT"

# C15: Morrow's classification of uncertain reconstructions, written on decorated pots.
SHERD_MEANINGS = {  # learned from examples by players; the plugin never states them
    "SHELTER_POTTERY_SHERD": "belongs to a house", "HEART_POTTERY_SHERD": "personal object",
    "SKULL_POTTERY_SHERD": "owner gone", "MOURNER_POTTERY_SHERD": "waiting for owner",
    "FRIEND_POTTERY_SHERD": "owner present", "HEARTBREAK_POTTERY_SHERD": "sources disagree",
    "BURN_POTTERY_SHERD": "deleted", "PRIZE_POTTERY_SHERD": "accepted as fact",
    "DANGER_POTTERY_SHERD": "uncertain", "EXPLORER_POTTERY_SHERD": "from a plan",
}
POTS = [  # anchor, offset, four sherds (north, east, south, west), the suspicious block's buried item
    {"anchor": "june_tower", "offset": [2, 0, 2], "sherds": ["SHELTER", "DANGER", "EXPLORER", "PRIZE"], "buried": "GLASS"},
    {"anchor": "cairn_storehouse_door_2011", "offset": [-2, 0, 3], "sherds": ["SHELTER", "HEART", "SKULL", "PRIZE"], "buried": "WHEAT_SEEDS"},
    {"anchor": "rookery_house", "offset": [3, 0, -2], "sherds": ["SHELTER", "HEART", "MOURNER", "PRIZE"], "buried": "LEATHER_BOOTS"},
    {"anchor": "finch_hub", "offset": [2, 0, 2], "sherds": ["EXPLORER", "DANGER", "HEARTBREAK", "PRIZE"], "buried": "POWERED_RAIL"},
    {"anchor": "lowpath_place", "offset": [2, 0, 0], "sherds": ["SHELTER", "MOURNER", "HEART", "HEARTBREAK"], "buried": "RED_BED"},
]
POT_FOR_PLAYERS = ["SHELTER", "FRIEND", "HEART", "PRIZE"]  # Act 9, beside each player's first-join spot

# C11: Theo's map wall. Maps are created in THIS order (so map ids rise in this order); the
# label is what Theo (or Morrow) named it. The forged "2015" is created last, so it has the highest id.
MAP_WALL = [
    {"label": "Mossfield 2016", "lowpath": True}, {"label": "Mossfield 2017", "lowpath": True},
    {"label": "Mossfield 2018", "lowpath": True}, {"label": "Mossfield 2019", "lowpath": True},
    {"label": "Mossfield 2020", "lowpath": True}, {"label": "Mossfield 2021", "lowpath": True},
    {"label": "Mossfield 2022", "lowpath": True}, {"label": "Mossfield 2023", "lowpath": True},
    {"label": "Mossfield 2024", "lowpath": True}, {"label": "Mossfield 2025", "lowpath": True},
    {"label": "Mossfield 2015", "lowpath": True, "forged": True},
]
GENUINE_2015_MAP = {"label": "Mossfield 2015", "lowpath": False}  # created FIRST (lowest id), hidden in Theo's drafts chest

MUSEUM_SIGN = ["CHI-07", "est. 2010", "kept by T", ""]  # waxed; the only modern sign Morrow never touches
MAINT_SIGN_TEXT = "MAINT ON"

# D1: finch's torch lock. Lever i must be ON where the bit is 1 (those channels go through a torch
# inverter); the door opens only when every channel is off. Read the wiring, not the levers.
FINCH_LOCK_BITS = "10110010"
FINCH_SPUR_SIGN = ["pw for my stuff", "is booster", "rip boosters", "-finch"]

# C5: Rookery's sign at his Nether hub (2011). x8 -> the ladder room.
NETHER_HUB_SIGN = ["hub -27 / 21", "ladder room", "-rook", ""]

# C7: Iona's notes, at r.-1.0.mcr c(27,14) y40. Wall signs (2011: 15 chars a line).
IONA_NOTES_SIGNS = [
    ["stop = 3 things", "maint sign ON", "no sessions", "key in console"],
    ["cmd name is in", "the annex wall", "as block ids", "-ib"],
]

# C20: Theo's private folder on the mirror. Password = the sticky note on the case ("chi07-")
# + the year Mossfield was founded (the roster: rookery joined 2010-10-30).
TVALE_BOX = {"user": "tvale", "password": "chi07-2010", "act": 8,
             "files": ["mrrw-changelog-2016.txt", "crontab.txt", "bridge.yml"]}

# Act 9: Morrow's fake Theo voice note, spliced from the 2011 voicemail (phone-band, says "mate").
FAKE_THEO_NOTE = {"text": "no worries at all.  keep it billable till someone comes home.", "voice": "T-09-fake"}
