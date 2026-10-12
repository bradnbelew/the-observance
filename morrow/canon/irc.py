"""#copperline-ops, Iona's irssi log, April to August 2011. Times are Chicago local.

Theo shares this file in Act 7 ("found it on the second drive"). It is the only place
the staff speak to each other off the record. Keep it plain: tired people at work.
"""

from __future__ import annotations

# (date, [(time, nick, text)])  nick "*" = action/notice line
LOG = [
    ("2011-04-21", [
        ("02:51", "*", "ibell has joined #copperline-ops"),
        ("02:52", "ibell", "chi array 2 is throwing errors. two disks in the same set. great"),
        ("02:58", "dkessler", "how bad"),
        ("03:04", "ibell", "bad. game nodes on chi-05 to chi-07 have corrupt region files"),
        ("03:05", "ibell", "and the nightly backup ran at 02:30 so it backed up the corruption over the good copy"),
        ("03:06", "dkessler", "so no backup"),
        ("03:06", "ibell", "so no backup"),
        ("09:58", "tvale", "morning. I've got 14 tickets about missing chunks"),
        ("09:59", "tvale", "mossfield is the loud one"),
        ("10:01", "ibell", "mossfield has logblock. that's actually good news"),
        ("10:01", "tvale", "why"),
        ("10:03", "ibell", "logblock records every block anyone placed or broke with a timestamp. if I replay it onto the partial backup I can rebuild most of it"),
        ("10:04", "tvale", "you're a genius"),
        ("10:04", "ibell", "I'm tired. those are different"),
    ]),
    ("2011-04-25", [
        ("14:10", "ibell", "replay tool works on a copy. calling it mrestore"),
        ("14:11", "tvale", "that's an awful name"),
        ("14:11", "ibell", "it's a process name, not a pet"),
        ("14:13", "tvale", "call it morrow. 'your world will be back by tomorrow'"),
        ("14:13", "ibell", "you're naming it after a macro"),
        ("14:14", "tvale", "the macro is very popular"),
        ("14:16", "ibell", "fine. mrrw on the box, morrow wherever you want"),
        ("14:40", "ibell", "gaps are the problem. where logblock has nothing it has to guess"),
        ("14:41", "tvale", "guess how"),
        ("14:44", "ibell", "habits. if cairn always places chests in pairs it places pairs. if june always uses glass on top it uses glass"),
        ("14:45", "tvale", "that's kind of lovely"),
        ("14:45", "ibell", "it's kind of a lie theo. I'm marking every guess"),
    ]),
    ("2011-05-03", [
        ("22:30", "ibell", "added a debug view. it can replay a logged session as an entity so I can see who did what"),
        ("22:31", "tvale", "like ghosts?"),
        ("22:31", "ibell", "like a debug view. off by default"),
        ("22:35", "ibell", "rule for anyone touching this: never stop a restore with a session open. the region writes are half done and the files corrupt"),
        ("22:36", "tvale", "how does it know a session's done"),
        ("22:38", "ibell", "it ends a replay when they log off or sleep in a bed. no bed, no end"),
        ("22:38", "tvale", "noted"),
    ]),
    ("2011-05-06", [
        ("15:20", "tvale", "I gave morrow a chat helper. it answers !status"),
        ("15:21", "ibell", "with what"),
        ("15:23", "tvale", "markov thing. fed it the support macros and the mossfield chat logs so it talks like a person"),
        ("15:24", "ibell", "it talks like a blender full of people"),
        ("15:24", "tvale", "they love it"),
        ("15:26", "ibell", "it can only say things somebody already said. remember that when it says something clever"),
    ]),
    ("2011-05-18", [
        ("01:55", "tvale", "status page updated. window 02:00-04:00 CDT"),
        ("02:00", "ibell", "starting the pass. watching"),
        ("03:10", "ibell", "looks clean so far. cairn's area is slow, lots of log"),
        ("04:00", "ibell", "window's over. it's still going"),
        ("04:01", "tvale", "it's nearly done, cairn's area is still replaying"),
        ("04:02", "ibell", "theo it's past the window"),
        ("04:03", "ibell", "it's not \"replaying\", it's making stuff up. june's tower has a roof"),
        ("04:04", "tvale", "she posted a plan for it. it's following the plan"),
        ("04:05", "tvale", "give it till morning"),
        ("04:09", "ibell", "the debug view is on. who turned the debug view on"),
        ("04:09", "tvale", "not me"),
        ("04:12", "ibell", "it's walking replays around the map with players online. they can see them"),
        ("04:20", "tvale", "people are posting. kit's on, he's at my place, he says he saw rook by the cake"),
        ("04:21", "ibell", "kit is 14 and it is 4am"),
        ("04:21", "tvale", "he couldn't sleep"),
        ("05:40", "ibell", "it's not finishing. the queue isn't going down, it's adding jobs"),
        ("05:41", "tvale", "adding what"),
        ("05:44", "ibell", "sessions. it's continuing people. there's a job called 'cairn morning loop' that has no end"),
        ("06:10", "ibell", "I'm stopping it"),
        ("06:11", "tvale", "if you stop it mid-session the files corrupt, you said that"),
        ("06:12", "ibell", "I'd rather corrupt a region than have it walking around as a 22 year old baker"),
        ("06:17", "ibell", "killed. uptime 04:17:22. it's down"),
        ("06:19", "ibell", "going to sleep. nobody touch it"),
        ("06:44", "*", "tvale has quit (Quit: )"),
        ("07:05", "*", "tvale has joined #copperline-ops"),
        ("07:06", "tvale", "rook says it's moving again"),
        ("11:02", "ibell", "theo. the panel log says you started it at 06:44"),
        ("11:05", "tvale", "it was mid-session. you said it would corrupt. kit was asleep in the spare room and I didn't want him to wake up to it gone"),
        ("11:06", "ibell", "it IS gone theo. the world they had is gone. that thing is a guess"),
        ("11:07", "tvale", "it's a good guess"),
        ("11:10", "ibell", "I'm turning off auto-restart in the panel. do not turn it back on"),
    ]),
    ("2011-05-24", [
        ("21:30", "ibell", "rook asked me how to stop it without the panel. he doesn't trust the panel. fair"),
        ("21:31", "ibell", "told him: it reads a maint flag sign at spawn. MAINT ON, no sessions open, and the console key. three things"),
        ("21:33", "dkessler", "why can't we just delete it"),
        ("21:35", "ibell", "because the panel restarts it, cron restarts it, and theo restarts it"),
        ("21:35", "dkessler", "lol"),
        ("21:36", "ibell", "I wasn't joking"),
    ]),
    ("2011-06-02", [
        ("22:58", "ibell", "rook's building something in his cellar with wool. said he'll tell me how to read it"),
        ("23:02", "ibell", "my notes are in r.-1.0.mcr c(27,14) y40ish if anyone ever needs them"),
        ("23:10", "ibell", "put my key in a double chest under june's old dock, 3 down, 4 toward spawn. counts are b64. rook cant read it and neither can morrow lol"),
        ("23:11", "tvale", "you know I can read this channel"),
        ("23:11", "ibell", "you can read it. you can't dig"),
    ]),
    ("2011-07-08", [
        ("16:20", "ibell", "it reads signs and chat. it doesn't read blocks. so I wrote the command name in block ids in the annex wall. worldedit, 8 blocks"),
        ("16:21", "tvale", "that's insane"),
        ("16:21", "ibell", "it's a label it can't tidy"),
    ]),
    ("2011-08-12", [
        ("17:02", "ibell", "last day. thanks for everything dale"),
        ("17:03", "dkessler", "you'll be missed. take the ficus"),
        ("17:03", "ibell", "I'm buying the ficus at the auction when you go under"),
        ("17:04", "dkessler", "hilarious"),
        ("17:10", "ibell", "theo. do not let it restart"),
        ("17:25", "tvale", "I won't"),
        ("17:26", "*", "ibell has quit (Quit: bye)"),
    ]),
]


def render() -> str:
    out = []
    from datetime import date
    for day, lines in LOG:
        d = date.fromisoformat(day)
        out.append("--- Day changed %s" % d.strftime("%a %b %d %Y"))
        for t, nick, text in lines:
            out.append("[%s] %s" % (t, text) if nick == "*" else "[%s] <%s> %s" % (t, nick, text))
    return "\n".join(out) + "\n"
