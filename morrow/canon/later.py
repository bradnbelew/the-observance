"""Acts 4-10: the Archive Desk, Morrow's rewrites, Act 7 messages, hints, and the finale.

Theo is never live. Everything he says is here, triggered by evidence tokens players file
at the Archive Desk, by elapsed time, or by game state. Voice note IDs match
morrow/v2/RECORDING-SCRIPTS.md; the site plays /media/theo/<ID>.ogg when the file exists and
shows the text either way.

The corpus law (verify.py enforces it on every string below that Morrow writes):
  * Chat-voice text Morrow writes (DMs, sign rewrites, forum and status rewrites, backdated posts,
    the per-player signs, the code-shaped signs) is a collage of fragments somebody really said or
    wrote: forum posts, server chat, private messages, the genuine 2011 signs it reads, the billing
    notes, and the voicemail. An unsourced word means a human wrote it.
  * Its timestamp formatter always prints CST (it never learned daylight saving). That's the
    forgery tell on every date it writes, not a word it chose.
  * Restore-engine output is a fixed set of status templates Iona coded in 2011 ("restored",
    "normalised", "session X: end", "queue n/m", ...), filled with handles, numbers and words it has
    read. Normalising "b4" to "before" on the rules sign is one of those templates.
"""

from __future__ import annotations

import hashlib
from datetime import date, timedelta

from . import ciphers, layout, people

# ---------------------------------------------------------------------------
# Archive Desk rules. A ticket's text is lower-cased and checked for tokens.
# When two complete matches tie, the earlier rule wins, so catch-alls go last.

UNMASK_TOKENS = ["lot 14", "chi-07", "2014-12-14", "waived"]
UNMASK_PARTIAL = "That's one thing.  I'd want two before I said anything."
UNMASK_END = "\n\nIt's just Mossfield.  It's just the residents."

DESK_RULES = [
    # Act 4: three answers keyed on the strongest thing they filed (the first complete match wins, so
    # the specific ones come first), then the general one.
    {
        "id": "unmask_lot", "act": 4, "title": "who runs this mirror",
        "tokens": UNMASK_TOKENS, "need": 2, "require": ["lot 14"],
        "reply": "You found the auction paperwork.  I kept it, I don't know why.\n\nYeah.  It's me.  Theo.  Lot 14 was "
                 "the game node and Lot 17 was the web box, and nobody else bid.  I drove them home in a Corolla.  "
                 "I didn't know how to say it.  I didn't think anyone would actually come." + UNMASK_END,
        "voice": "T-04c", "partial": UNMASK_PARTIAL, "partial_voice": "T-04b",
    },
    {
        "id": "unmask_date", "act": 4, "title": "who runs this mirror",
        "tokens": UNMASK_TOKENS, "need": 2, "require": ["2014-12-14"],
        "reply": "Five days after.  Yes.\n\nIt's me.  Theo.  The auction was the ninth, I collected on the "
                 "fourteenth, and I had it running in the spare room by three in the morning.  I told myself I was "
                 "checking the drives.  I kept Rowan's password reset working.  In case Rowan came back." + UNMASK_END,
        "voice": "T-04d", "partial": UNMASK_PARTIAL, "partial_voice": "T-04b",
    },
    {
        "id": "unmask_waived", "act": 4, "title": "who runs this mirror",
        "tokens": UNMASK_TOKENS, "need": 2, "require": ["waived"],
        "reply": "Keep it billable.  I did keep it billable.\n\nIt's me.  Theo.  Every June I waived the invoice so "
                 "the account would never close and the world would never be deleted.  Then Copperline closed "
                 "instead, and I bought the box.  I didn't think anyone would actually come." + UNMASK_END,
        "voice": "T-04e", "partial": UNMASK_PARTIAL, "partial_voice": "T-04b",
    },
    {
        "id": "unmask", "act": 4, "title": "who runs this mirror",
        "tokens": UNMASK_TOKENS, "need": 2,
        "reply": "yeah.  it's me.  Theo.  sorry.  I didn't know how to say it.  I didn't think anyone would "
                 "actually come.\n\nI kept the old box running after Copperline closed.  It's in my spare room.  "
                 "I bought the web box at the same auction, and the domain when it lapsed.  I kept Rowan's "
                 "password reset working.  In case Rowan came back.\n\nIt's just Mossfield.  It's just the residents.",
        "voice": "T-04a",
        "partial": UNMASK_PARTIAL,
        "partial_voice": "T-04b",
    },
    {
        # Act 3 ends when they find Morrow's name in LogBlock (June's tower, 18 May 2011). Theo isn't
        # unmasked yet: he deflects. "morrow" must start a word (the desk won't match "tomorrow").
        "id": "found_morrow", "act": 3, "title": "a name in the block log",
        "tokens": ["morrow", "logblock", "glass", "glowstone", "04:11", "04:13", "04:15", "tower", "roof"],
        "need": 2, "require": ["morrow"],
        "reply": "That's not anyone.  It's not on the roster and it's not on the forum.  Probably a plugin, they "
                 "all had names back then.  Leave it with me.",
        "voice": None, "partial": "",
    },
    {
        # Act 5: filing the floor's last words gets Rowan's line from Theo, and moves nothing. The act
        # ends when the group does what the floor says (MAINT ON at spawn) and Morrow reverts it.
        "id": "floor", "act": 5, "title": "rowan's floor",
        "tokens": ["dont tell theo", "don't tell theo", "donttelltheo"], "need": 1,
        "reply": "Rowan never did trust me with the panel.  Fair enough.\n\nThen don't.  Whatever it says to do, "
                 "it's not me you tell.",
        "voice": None, "partial": "",
    },
    {
        "id": "restart", "act": 6, "title": "the 06:44 restart",
        "tokens": ["06:44:10", "chi-07/mrrw", "mid-session"], "need": 2, "require": ["06:44:10", "mid-session"],
        "reply": "It was mid-replay.  Iona said if you stop it mid-session the region files corrupt.  Kit was "
                 "asleep in the spare room and I just.  I didn't want him waking up to it gone.\n\nI know.  "
                 "I know what it sounds like.\n\nRowan knew.  He heard me say it on Mumble.  That's not the bit "
                 "I'm ashamed of.",
        "voice": "T-06a",
        "partial": "Which line.  There's a lot of lines in that log.  Read the reason field too.",
    },
    {
        "id": "restart_node", "act": 6, "title": "the 06:44 restart",
        "tokens": ["06:44:10", "chi-07/mrrw", "mid-session"], "need": 2, "require": ["06:44:10", "chi-07/mrrw"],
        "reply": "CHI-07/mrrw.  START.  06:44:10.  That's my login.\n\nI'd read her stop line and I'd read the "
                 "reason I typed, and I pressed it anyway.  Iona said if you stop it mid-session the region files "
                 "corrupt.  Kit was asleep in the spare room and I didn't want him waking up to it gone.\n\n"
                 "Rowan knew.  He heard me say it on Mumble.  That's not the bit I'm ashamed of.",
        "voice": "T-06d",
        "partial": "Which line.  There's a lot of lines in that log.  Read the reason field too.",
    },
    {
        "id": "sorry_kit", "act": 7, "title": "the reading room",
        "tokens": ["sorry kit"], "need": 1,
        "reply": "That's from my drafts.  I never sent it.\n\n\"Kit, sorry about the server.  And the rest of it.  T\"\n\n"
                 "That's all it is.",
        "voice": None, "partial": "",
    },
    {
        # Iona's key filed at the desk: Theo won't take it (he never had it). Matched by hash (an "md5:"
        # token is compared against the md5 of each word filed), so the key never sits in a site
        # bundle. Act 7 ends when the key goes where Iona put hers: the panel console (console_key).
        "id": "key", "act": 7, "title": "iona's key",
        "tokens": ["md5:" + ciphers.md5(ciphers.IONA_TOKEN)], "need": 1,
        "reply": "That's not for me.  Iona built it so it wasn't.  Not even me, she said, and she meant it.\n\n"
                 "Put it where she'd put it.",
        "voice": None, "partial": "",
    },
    {
        "id": "watchdog", "act": 8, "title": "the watchdog",
        "tokens": ["@reboot", "watchdog", "auto_restart=on", "crontab"], "need": 2,
        "reply": "Yes.  The watchdog's mine.  I put it in root's crontab the week Iona turned the panel restart "
                 "off, and when she asked, I said it wasn't me.  It brought it back the night Dale rebooted the "
                 "node.\n\nAnd when I set the box up in 2014 I switched auto-restart back on.  That's the bit I "
                 "never told anyone.  Not the morning.  Everything after.",
        "voice": None, "partial": "That's just housekeeping.  Every box has a crontab.",
    },
    {
        "id": "ingest", "act": 8, "title": "where did it learn that",
        "tokens": ["turn it off"], "need": 1, "delay_hours": 36,  # the same silence as 'kit': Act 9 never opens before T-08a
        "reply": "It's not in the old logs, is it.\n\nI fed it everything from my old inbox.  For the voices.  "
                 "I wanted it to sound like people.  That line's from my brother.  Here.\n\nIt picks lines that "
                 "sound like what it's hearing.  You've all been saying \"turn it off\" for a week.",
        "voice": None, "attach": "kit-2019-03-02.eml",
        "partial": "",
    },
    {
        "id": "kit", "act": 8, "title": "what kit said",
        "tokens": ["2019-03-02", "turn it off"], "need": 2, "delay_hours": 36,
        "reply": "Kit sent that in 2019.  I read it on the train and I didn't answer.\n\nHe was right.  He "
                 "was twenty-two, he had a job, he hadn't logged in since he was fifteen.  I kept it running "
                 "because.  I don't know.  Because turning it off felt like the last day all over again.\n\n"
                 "That was seven years ago.  Fifteen since any of them played.  I'm sorry I fed it his email.  "
                 "I wanted it to sound like people.\n\nI turned it off that night.  Forty minutes.  Then I put it "
                 "back on.  That's in the log too, you'll have seen.",
        "voice": "T-08a",
        "partial": "Where did you get that.",
    },
    {
        "id": "trailhead", "act": 1, "title": "the other server",
        "tokens": ["marked", "our server", "game night"], "need": 1,
        "reply": "That wasn't me.  I don't know how it got there.  Please don't put it anywhere else.",
        "voice": None, "partial": "",
    },
]

DESK_FILES = {  # exact filename -> (act it can be requested from, Theo's line, voice note)
    "moss_0518.ogg": (5, "Here's patchcord's.  Patch put it on Rowan's FTP.  I copied it off before I put the "
                         "mirror up.  I haven't listened to it in years.  I don't think I'm going to now.", "T-05a"),
}

# A ticket that matches nothing: Theo answers to what it mentions (a resident, a place, a file).
NONE_REPLIES = {
    "person": "I remember {who}.  I don't have anything of theirs that isn't on the site, sorry.  The forum's all there is.",
    "place": "I know the spot.  If there's something there, I don't have a file for it.  The block log might.",
    "file": "If you've got a filename, send me the exact one and I'll look.  I don't have an index, just folders.",
    "default": "Thanks!  I'm not sure I have anything on that, sorry.  If you've got a filename, that helps.",
}
PLACE_WORDS = ["spawn", "storehouse", "barn", "farm", "tower", "lighthouse", "dock", "hub", "rail", "spur", "cellar",
               "annex", "hut", "pen", "museum", "reading room", "ladder room", "point", "nether", "meadow", "cave"]

# Unprompted drops: Theo "found another drive", or just couldn't sleep.
# (act, hours after the act started, file, note, voice note)
DESK_DROPS = [
    (4, 0, "copperline-asset-sale-2014.pdf", "Found the auction paperwork.  Not sure why I kept it.", None),
    (5, 0, "6118-checklist.txt", "Iona left this on the ops share.  Might help, might not.", None),
    (6, 0, None, "The panel's letting people in again.  Read-only.  My old staff folder's back up too, /~tvale/.", None),
    (6, 8, None, "Couldn't sleep.  Went and looked at it.  It's just a box with a light on.  I don't know what I "
                 "expected.  Ignore this.", "T-06c"),
    (6, 30, None, "I wax the signs I want to keep.  Old habit.  Nothing touches them after that.", "T-06b"),
    (7, 0, "copperline-ops.irc.log", "Iona's IRC logs.  I was in that channel.  I know what's in there.  Here.", None),
    (7, 48, None, "If one of those wasn't me, I'm sorry.  I can't see what it sends.", "T-07d"),
    (8, 0, "closet.jpg", "Found this on my phone.  That's the box.", None),
    (9, 0, "copperline-ops.irc.2011-06-10.log", "There's a day missing from the IRC log I sent you.  I cut it out before I "
                                                 "sent it.  Here.", None),
    (9, 12, None, "Stood in the spare room for a bit this morning.  It's loud, the box.  I'd stopped hearing it.", "T-09c"),
]

# Act 10: the booking reply gains the ask (whatever a resident then says in Mossfield about finch's
# sign becomes the stop log's reason), and a week after the stop Theo writes once more.
THEO_ASK = {"text": "One more thing.  finch's sign at the south branch.  Leave it, or let it finish?  Say it in "
                    "Mossfield.  It listens to chat.", "voice": "T-10-ask"}
THEO_AFTER = {"text": "Kit came over.  We put the stick in his laptop.  He said it looked smaller than he remembered.  "
                      "Thanks for coming.\n\nT", "voice": "T-10-after", "after_stop_hours": 168}

HINT_HOURS = [24, 72, 120, 168]  # when each of an act's hints appears, hours into the act

# Escalating hints by act (never the answer).
HINTS = {
    1: ["Found another drive.  Not sure it helps.  There's a lot on the old site nobody ever linked to.",
        "Old sites always said where not to look.  Search engines were told first.",
        "Rowan's first forum post is the most Rowan thing I've ever read."],
    2: ["If you're stuck in Mossfield, the residents left a lot of signs.  They wrote everything on signs back then.",
        "Not everything in there is from 2011.  Some of it couldn't be.",
        "The minecraft wiki keeps a history of when every block was added."],
    3: ["Everyone was in a different time zone.  Patch was always ahead of us.  June too.",
        "June's light still flashes like the one at home.",
        "The block log doesn't lie.  It's the only thing in there that doesn't.",
        "June's folder has a password.  She used the same one for everything.  So did everyone."],
    4: ["If you think you know who's running this, just ask me.  I answer what's filed.",
        "If it sounds like a person, check who said it first.",
        "I'm not hard to find.  I'm on most of these pages if you look at how I type.",
        "The auction paperwork has a lot number on it."],
    5: ["Patch's password was a song.  The bass line, he always said.",
        "Rowan wrote his floor in wool.  He used the wool numbers from the forum.",
        "Rowan read the rules out loud for a reason.",
        "Iona left hashes for everything.  If your answer hashes right, it's right.  Then it's not me you tell.  "
        "It's the sign."],
    6: ["Daylight saving.  Iona always got it right.",
        "Signs back then fit fifteen letters a line.",
        "Look at what a picture says about itself, not just what it shows."],
    7: ["Don't believe anything that sounds like a support macro.",
        "Iona thought in files, not places.",
        "Counts in a chest can be letters if you count from the right place.",
        "Iona's key isn't for me.  The panel still takes a key, if you can log in."],
    8: ["It said something in chat last night I've only ever seen in one email.  Not a log.  If you've heard it "
        "too, ask me where it got it.",
        "It ends a replay when they log off or sleep.  Iona said that once.",
        "cairn always closed up the same way.",
        "lowpath walks every night and never gets home."],
    9: ["Fill in the brief.  I just need it to be real.",
        "Rowan never trusted me with anything.  If he left something, he wrote it for them, not me.",
        "Iona hashed everything.  Even the backups."],
    10: ["The console's in the annex.  Where Iona wrote its name.",
         "Three things.  The sign, the sessions, the key.",
         "Wax the sign.",
         "I'll cut the bridge when you've done your bit."],
}

# ---------------------------------------------------------------------------
# Act 6: Morrow's rewrites. Each is a collage of real lines; the tells are the CST timestamps and,
# on signs, lines longer than 2011's fifteen characters.

FORUM_REWRITES = {  # post id -> (new body, edit note). The print view still shows the original.
    26810: ("the restore process at 06:17 CST. It's fixed. Your world will be back by tomorrow.",
            "Last edited by i_bell; 05-18-2011 at 06:40 AM CST."),
    26815: ("the storehouse was tidy. it's fixed. it's not messy",
            "Last edited by cairn; 05-18-2011 at 06:52 AM CST."),
    26820: ("its moving again. no worries at all. it'll get better",
            "Last edited by rookery; 05-18-2011 at 07:10 AM CST."),
}

SIGN_REWRITES = [  # Act 6 in-world: genuine 2011 signs Morrow "corrects" (lines over 15 chars = tell)
    {"anchor": "rookery_house", "lines": ["ROOK'S", "boots off", "if anyone comes back", ""]},
    {"anchor": "finch_hub", "lines": ["the south branch", "rebuilding everything", "this week", ""]},
    {"anchor": "june_house", "lines": ["it finished my tower", "from the plan I posted", "2011-05-18", ""]},
    # the bulletin board's spur note (board_5 sits at x+1, y-1) and the last line of Rook's ledger
    {"anchor": "spawn_board", "offset": [1, -1, 0], "lines": ["the spur still", "isn't done, don't ask", "", ""]},
    {"anchor": "rookery_changes_wall", "offset": [0, -1, -2], "lines": ["its back on again", "im done with it", "", ""]},
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
    ["my tower has", "a roof back by", "tomorrow", ""],
    ["leave it at the", "farm no worries", "", ""],
]

# ---------------------------------------------------------------------------
# Act 7: messages. Morrow's DMs are fixed collages (verified against the corpus);
# the bot adds per-player quotes of other players' real in-game lines.

MORROW_DMS = [
    {"as": "cairn", "text": "there's another me at the farm. i'm at the storehouse. don't guess my chests"},
    {"as": "juni_lantern", "text": "my tower has a roof. send it to me after. i want to hear it"},
    {"as": "rookery", "text": "everyone get off for a bit. if it goes bad again"},
    {"as": "patchcord", "text": "recording this for later. remind me to send you a copy"},
    {"as": "theo", "text": "no worries at all. your world will be back by tomorrow"},
]
MORROW_QUOTE_TEMPLATE = "{other} said: {line}"
# Each real Theo DM carries a word nothing in Morrow's corpus has, and that players won't type in
# game chat either (Morrow ingests their chat from Act 7): Metra, radiator, cardigan.
THEO_TELL_WORDS = ["metra", "radiator", "cardigan"]
THEO_DMS = [
    {"text": "it's me.  it's actually me.  I'm on the Metra home.  Stay a bit.", "voice": "T-07a"},
    {"text": "If it's word for word one of my old macros, that's it, not me.  I still say no worries.  I know.  "
             "Ignore the typos, the radiator is clanking.", "voice": "T-07b"},
    {"text": "It's better with you there.  It's learning.  I think that's a good thing.  Ignore me, it's late "
             "and I'm in my cardigan.", "voice": "T-07c"},
]
PER_PLAYER_SIGNS = [  # lowpath's place, Act 7: each player sees a different front; one line is shared
    ["if anyone comes back", "the bed is gone", "", ""],
    ["it's where you left it", "the bed is gone", "", ""],
    ["it can stay gone", "the bed is gone", "", ""],
    ["no worries at all", "the bed is gone", "", ""],
]

# ---------------------------------------------------------------------------
# Act 9: absorption

def backdated_join(player: str) -> str:
    """The fake 2010 join date Morrow gives a player's forum profile (stable per name)."""
    n = int(hashlib.md5(player.lower().encode()).hexdigest(), 16)
    return (date(2010, 11, 1) + timedelta(days=n % 28)).isoformat()


BACKDATED_POSTS = [  # attributed to each player, dated 2011 (shown with CST in May: the tell)
    ("2011-05-17 21:10", "logged in for a minute. weird seeing it again. the storehouse was tidy"),
    ("2011-05-18 04:12", "someone comes home. no worries at all"),
]

# rook.txt sits in the clean April zip (files.py). He wrote it in September 2011 for whoever read the
# ladder room. The case brief accepts its last line in place of the clean copy's MD5.
ROOK_TXT = ("if you're reading this you read the ladder room, so you're one of us. not theo.\n"
            "this is mossfield on the 20th of april. before any of it. the holes are ours.\n"
            "whatever's running it now, dont let it finish anything.\n"
            "-rook\n")
ROOK_TXT_PHRASE = "dont let it finish anything"

CASE_BRIEF_FIELDS = [
    {"id": "restart", "label": "When did you turn it back on?", "accept": ["2011-05-18 06:44:10", "06:44:10", "6:44:10"]},
    {"id": "lowpath", "label": "When was lowpath made?", "accept": ["2016-08-03"]},
    {"id": "kit", "label": "When did Kit write to you?", "accept": ["2019-03-02"]},
    {"id": "clean", "label": "The clean copy: its MD5, or the line Rowan left in it",
     "accept": ["@clean_md5", ROOK_TXT_PHRASE]},
    {"id": "joined", "label": "When does the forum say we joined?", "accept": ["@backdated"]},
]

# Theo's answer to the case brief (the site's desk and the Discord mirror both read these).
BRIEF_REPLIES = {
    "accepted": ("Okay.  Okay.  I've read all of it.  I knew all of this.  I'd just never seen it in one place.  "
                 "You're right.  Let's do it properly, the way Rowan wanted.\n\nI'm wiping the drives after.  I've "
                 "got one USB stick in the drawer.  I'll keep one thing.  Just one, or I'll end up doing this again.  "
                 "You pick.\n\nThe panel will let you book a maintenance window now.", "T-09a"),
    "missing": ("I hear you.  I just need it to be real.  Show me.", "T-09b"),
}

# Theo's big replies, mirrored into Discord #general when the plugin releases them (journal "theo.reply").
THEO_MIRROR_VOICES = ["T-04a", "T-04c", "T-04d", "T-04e", "T-06a", "T-06d", "T-08a", "T-09a", "T-09b"]
# Timed notes the bot also posts (the plugin schedules them from act_started): drop voices + the ask/after.
THEO_TIMED_VOICES = ["T-06b", "T-06c", "T-07d", "T-09c", "T-10-ask", "T-10-after"]
THEO_MIRROR_HEADER = "**archivist** - Archive Desk - re: {subject}"

# ---------------------------------------------------------------------------
# Act 10: the stop

USB_CHOICES = {
    "clean": ("The April copy.  From before any of it.  June's tower's got no roof.  The south rail ends in the grass.",
              "T-10-clean"),
    "recovered": ("The finished town.  Your names are on the houses.", "T-10-recovered"),
    "corpus": ("Every word anyone said.  I read the last line.  It was one of yours.", "T-10-corpus"),
}
# Act 10: the group votes in Discord #general on what goes on the stick. Each option names its cost.
# The plugin counts the votes (a strict plurality, once at least half the residents have voted); the
# console only accepts the winner.
USB_VOTE = {
    "prompt": "One stick.  I said you pick, so you all pick.  Whatever most of you press is what I put on it.  "
              "Press another one to change your mind.",
    "options": {
        "clean": {"label": "The April copy",
                  "cost": "Mossfield on the 20th of April, before any of it.  June's tower without its roof.  The south "
                          "rail ending in the grass.  And no light flashing at June's.  Morrow built that."},
        "recovered": {"label": "The finished town",
                      "cost": "Your names on the houses.  June's light still flashing.  But everything it tidied "
                              "stays tidy."},
        "corpus": {"label": "Every word",
                   "cost": "Every word anyone said there.  The forum, the chat, the voicemail, yours too.  No world at all."},
    },
    "tally": "So far: {counts}",
    "ack": "Got it: {label}.  Press another one to change it.",
    "residents_only": "Only Mossfield residents get a say on this one, sorry.",
    "closed": "It's done.  The stick's in the drawer.",
    "undecided": "The group hasn't decided.  Ask in #general.",
}

# The skip line quotes a resident: whoever answered Theo's ask in Mossfield chat, else finch's own
# 2011 sign (never "reason=none").
SKIP_FALLBACK = 'skip mossfield:south_rail (unfinished) reason="leave it" (finchline, 2011-05-19)'
STOP_LOG = [
    "morrow: stop requested (maint=on, sessions=0, key=ok)",
    "morrow: finalising restore queue",
    "morrow: done mossfield:storehouse",
    "morrow: done mossfield:north_light",
    "morrow: " + SKIP_FALLBACK,
    "morrow: exit 0",
]
READY_STATUS = "maint=on sessions=0"  # what the MOTD hex channel says when the node is ready to stop

# Restore-engine lines the plugin prints at the moments the rework added (Iona's templates, filled).
ENGINE_LINES = {
    "replay_cue": "debug view: replay 2011-05-18 04:00",
    "lowpath_cue": "debug view: replay lowpath",
    "maint_on": "maint: on. sessions open: 1. restore continues",
    "maint_revert": "1 sign normalised (spawn)",
    "console_key": "key accepted. stop refused: maint=off sessions=1 (lowpath)",
    "bed_queued": "lowpath's place: 1 block queued",
    "bed_removed": "1 block normalised (lowpath's place)",
    "bed_kept": "lowpath's place: bed kept",
    "floor_tidy": "54 blocks normalised (rook's)",
    "kit_restored": "1 chest restored (spawn)",
    "creeper_done": "24 blocks restored (spawn)",
    "chest_normalised": "1 chest normalised (june's point)",
    "stop_requested": "stop requested: {code}",
    "session_end": "session {who}: end",
    "lowpath_slept": "session lowpath: end (slept)",
    "queue_last": "queue 413/413: lowpath (open)",
}

# Theo's unsent letters: written books in his drafts barrel under the reading room (C10's payoff).
THEO_DRAFTS = [
    {"title": "june (unsent)", "pages": [
        "June.\n\nI saw you in a photo.  A nursing board in Halifax, 2016, the staff page.  Same hat, nearly.  I had the window open for an hour.",
        "The tower's still there.  It still flashes.  I never worked out how to tell you that without the rest of it.\n\nT"]},
    {"title": "rowan (unsent)", "pages": [
        "Rowan.\n\nThe address bounced in 2013 and I didn't try another one.  I'm writing this to a drawer.",
        "You said you'd stop it yourself if it went bad again.  You never got the chance, and that's on me, not the box.\n\nIt's still running.  I know.\n\nT"]},
    {"title": "iona (unsent)", "pages": [
        "Iona.\n\nI saw a ficus in a bank lobby on Dearborn, 2022, and I thought it was yours.  I stood outside for a bit and didn't go in.",
        "You told me not to let it restart.  I set it going in my spare room three years after you said that, and I've never once told anyone why.\n\nYou'd have hashed this letter.\n\nT"]},
]
# The visitor book on the reading-room lectern. Nobody else ever signed it.
VISITOR_BOOK = {"title": "visitors", "pages": [
    "if you were a resident, write your name.",
    "theo.  2023.",
    "theo.  2024.  still here.",
    "theo.  2025.",
]}


__all__ = ["DESK_RULES", "DESK_FILES", "BRIEF_REPLIES", "THEO_MIRROR_VOICES", "THEO_MIRROR_HEADER", "USB_VOTE", "DESK_DROPS", "HINTS", "HINT_HOURS", "FORUM_REWRITES", "SIGN_REWRITES",
           "NONE_REPLIES", "PLACE_WORDS", "THEO_ASK", "THEO_AFTER", "SKIP_FALLBACK", "ENGINE_LINES", "THEO_DRAFTS", "VISITOR_BOOK",
           "TRANSCRIPTION_BOOKS", "OFFLINE_DAYS", "SGA_SIGNS", "MORROW_DMS", "THEO_DMS", "PER_PLAYER_SIGNS",
           "backdated_join", "BACKDATED_POSTS", "CASE_BRIEF_FIELDS", "USB_CHOICES", "STOP_LOG", "READY_STATUS",
           "ROOK_TXT", "ROOK_TXT_PHRASE", "morrow_written", "ENGINE_VOCAB", "ciphers", "people"]

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
# Theo made and framed one map a year (map locking only arrived in 1.14, 2019).
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
NETHER_HUB_SIGN = ["hub %d / %d" % (layout.ANCHORS["nether_hub"][0], layout.ANCHORS["nether_hub"][2]),
                   "ladder room", "-rook", ""]

# C7: Iona's notes, at r.-1.0.mcr c(27,14) y40. Wall signs (2011: 15 chars a line). Written in
# July 2011, after the annex wall (IRC 2011-07-08).
IONA_NOTES_SIGNS = [
    ["stop = 3 things", "maint sign ON", "no sessions", "key in console"],
    ["cmd name is in", "the annex wall", "as block ids", "-ib"],
    ["key: june's old", "dock. 3 down,", "4 toward spawn", "counts, b64"],
]

# C20: Theo's private folder on the mirror. Password = the sticky note on the case ("chi07-")
# + the year Mossfield was founded (the roster: rookery joined 2010-10-30).
TVALE_BOX = {"user": "tvale", "password": "chi07-2010", "act": 8,
             "files": ["mrrw-changelog-2016.txt", "crontab.txt", "bridge.yml",
                       "heartbeat.log.2011-05", "heartbeat.log.2019-03"]}  # Lot 14 came with its drives

# Act 9: Morrow's fake Theo voice note, spliced from the 2011 voicemail (phone-band, says "mate").
FAKE_THEO_NOTE = {"text": "no worries at all, mate.  keep it billable till someone comes home.", "voice": "T-09-fake"}

# The 2011 voicemail, word for word (RECORDING-SCRIPTS.md section 3). Morrow can play clips of it
# (Theo's 2018 changelog), so it's part of the corpus.
VOICEMAIL_2011 = ("Hi Rowan, it's Theo from Copperline, returning your call. Look, I know it's been a week, I know. "
                  "Iona's doing something really clever with the logs, it's going to be great, honestly. "
                  "And don't worry about the invoice, mate. No worries at all, mate, I'll sort it. "
                  "I'll keep it billable till someone comes home. I mean, till everyone's back on. Sorry, work "
                  "thing. Means we don't delete it. Anyway. Call me back. Or don't, it's fine. Cheers. Bye.")


# ---------------------------------------------------------------------------
# Everything Morrow writes, for verify.py's corpus-law check.

# Iona's status templates (2011) and the handful of labels the engine prints. Restore-engine
# strings may only use these words, words from the corpus, handles, anchors and numbers.
ENGINE_VOCAB = {
    "recovery", "recovered", "restored", "restore", "normalised", "completed", "kept", "absent", "since",
    "session", "sessions", "end", "slept", "resumed", "queue", "open", "skip", "done", "stop", "requested",
    "finalising", "exit", "reason", "none", "unfinished", "maint", "on", "off", "key", "ok", "block", "blocks",
    "line", "lines", "pass", "started", "window", "watchdog", "reboot", "sigterm", "received", "uptime",
    "stopping", "mossfield", "storehouse", "north", "light", "south", "rail", "tally", "chat", "x",
    # the rework's templates: the debug view, queued/kept placements, the chest and sign counters
    "debug", "view", "replay", "continues", "accepted", "refused", "queued", "bed", "chest", "chests", "sign",
    "place", "lowpath's", "rook's", "june's", "patch's", "point", "hub", "farms", "tower", "spawn",
}
# Template normalisations the engine applies to text it restores (2011 shorthand -> "proper" words).
NORMALISE = {"b4": "before"}


def morrow_written() -> list[dict]:
    """Every authored string Morrow writes. kind 'chat' = collage law; 'engine' = templates;
    'normalised' = a genuine line with only NORMALISE substitutions."""
    out = []

    def add(kind, where, text, act):
        out.append({"kind": kind, "where": where, "text": text, "act": act})

    for dm in MORROW_DMS:
        add("chat", "Morrow DM as %s" % dm["as"], dm["text"], 7)
    for pid, (body, _note) in FORUM_REWRITES.items():
        add("chat", "forum rewrite %d" % pid, body, 6)
    from . import records
    rw = records.STATUS_INCIDENT["morrow_rewrite"]
    add("chat", "status rewrite title", rw["title"], 6)
    add("chat", "status rewrite body", rw["body"], 6)
    for s in SIGN_REWRITES:
        add("chat", "sign rewrite at %s" % s["anchor"], " ".join(l for l in s["lines"] if l), 6)
    for i, s in enumerate(PER_PLAYER_SIGNS):
        add("chat", "per-player sign %d" % i, " ".join(l for l in s if l), 7)
    for i, s in enumerate(SGA_SIGNS):
        add("chat", "SGA sign %d" % i, " ".join(l for l in s if l), 6)
    for at, text in BACKDATED_POSTS:
        add("chat", "backdated post %s" % at, text, 9)
    for book in TRANSCRIPTION_BOOKS:
        for line, _src in book["lines"]:
            add("chat", "transcription book line", line, 6)
        add("engine", "transcription book title", book["title"], 6)
    add("voicemail", "fake Theo voice note", FAKE_THEO_NOTE["text"], 9)
    for line in STOP_LOG + [READY_STATUS, ciphers.LIGHT_CORRIDOR_MESSAGE.replace(":", " ")]:
        add("engine", "stop log / status", line, 10)
    for key, line in ENGINE_LINES.items():
        add("engine", "engine line %s" % key, line.replace("{code}", "").replace("{who}", "cairn"), 5)
    for line in ciphers.tally_book_morrow():
        add("engine", "tally book", line, 3)
    for m in MAP_WALL:
        add("engine", "map label", m["label"], 4)
    for a, t in ciphers.MOTD_BY_ACT.items():
        add("engine", "MOTD act %d" % a, t, a)
    for sign_i, sign in enumerate(ciphers.RULES_SIGNS_MORROW):
        for line_i, line in enumerate(sign):
            if line != ciphers.RULES_SIGNS_2011[sign_i][line_i]:
                add("normalised", "rules sign %d line %d" % (sign_i, line_i),
                    "%s => %s" % (ciphers.RULES_SIGNS_2011[sign_i][line_i], line), 3)
    return out
