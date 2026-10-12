"""Copperline Community Forums (vBulletin 3.8), board 'Customer Servers > Mossfield'.

Times are server-local Chicago time, which is also how the forum displayed them.
Writing rules: residents type like 2010 teenagers and twenty-somethings, short and
lowercase; Theo leaves two spaces after a full stop; Iona writes CDT in summer and
CST in winter and gets it right. Anything Morrow later rewrites keeps the original
in `cached` and gets a forgery tell (wrong zone, wrong era, wrong voice).

`act` is the first act in which a post is reachable on the mirror.
"""

from __future__ import annotations

MEMBERS = {
    "rookery": {"title": "Member", "joined": "2010-10-30", "location": "Ottawa, ON",
                "local_offset": "+1 hr", "posts": 412, "sig": "mossfield // whitelist: ask"},
    "cairn": {"title": "Member", "joined": "2010-11-04", "location": "Ames, IA",
              "local_offset": "same as forum", "posts": 158, "sig": "leave it uneven"},
    "finchline": {"title": "Member", "joined": "2010-11-20", "location": "Grand Rapids, MI",
                  "local_offset": "+1 hr", "posts": 233, "sig": "next stop: somewhere"},
    "juni_lantern": {"title": "Member", "joined": "2010-12-11", "location": "Halifax, NS",
                     "local_offset": "+2 hrs", "posts": 301, "sig": "F2 F2 F2"},
    "patchcord": {"title": "Member", "joined": "2011-01-08", "location": "Manchester, UK",
                  "local_offset": "+6 hrs", "posts": 189, "sig": "♪ only the bass line matters ♪"},
    "kitbag": {"title": "Junior Member", "joined": "2011-02-26", "location": "Naperville, IL",
               "local_offset": "same as forum", "posts": 47, "sig": "biscuit is a good dog",
               "interests": "minecraft, my dog biscuit, halo"},
    "i_bell": {"title": "Copperline Staff", "joined": "2009-03-02", "location": "Chicago, IL",
               "local_offset": "same as forum", "posts": 96,
               "sig": "Iona Bell // Copperline Recovery // alias: iona_bell"},
    "Theo [Copperline]": {"title": "Copperline Support", "joined": "2009-01-19", "location": "Chicago, IL",
                          "local_offset": "same as forum", "posts": 2244,
                          "sig": "Cheers, T  //  Copperline Support  //  1-877-555-0142", "homepage": "/~tvale/"},
    "lowpath": {"title": "Member", "joined": "2010-11-02", "location": "",
                "local_offset": "same as forum", "posts": 0, "sig": "",
                "import_note": "account created 2016-08-03 by archive import (join date set by importer)"},
    "archivist": {"title": "Archive", "joined": "2026-08-02", "location": "",
                  "local_offset": "same as forum", "posts": 9, "sig": ""},
}

THREADS = [
    {"id": 3102, "title": "Mossfield - new survival server, whitelist open", "act": 1, "posts": [
        {"id": 18877, "author": "rookery", "at": "2010-10-30 21:14", "body":
         "halloween update is out so i finally set up the server i keep talking about.\n\n"
         "survival, no plugins except the basics, whitelist so we dont get griefed into the ground. "
         "its on copperline's builder plan so it should actually stay up this time. first server i ran was "
         "a hamachi thing called bracken, rip, it died when my laptop did.\n\n"
         "post your mc name and ill add you. rules are on the signs at spawn, read them pls"},
        {"id": 18881, "author": "cairn", "at": "2010-11-04 05:52", "body":
         "cairn. i'll do farms if nobody else wants to. i'm up early anyway"},
        {"id": 18890, "author": "rookery", "at": "2010-11-04 08:30", "body":
         "added. 5am cairn wtf"},
        {"id": 18902, "author": "cairn", "at": "2010-11-04 09:01", "body":
         "bakery. 4am start. you get used to it"},
        {"id": 19117, "author": "finchline", "at": "2010-11-20 16:40", "body":
         "finchline pls! i want to do a rail line from spawn to wherever cairn's farms end up"},
        {"id": 19583, "author": "juni_lantern", "at": "2010-12-11 18:05", "body":
         "hi! juni_lantern, halifax. I mostly build and take way too many screenshots. is anyone "
         "near the water yet? I want the coast"},
        {"id": 19590, "author": "rookery", "at": "2010-12-11 18:30", "body":
         "coast is yours. east side past the gravel beach"},
        {"id": 20412, "author": "patchcord", "at": "2011-01-08 13:22", "body":
         "patchcord, manchester. heard there's a mumble going? I can host one, I've got a box doing nothing"},
        {"id": 21944, "author": "kitbag", "at": "2011-02-26 19:11", "body":
         "hi im kit. theo is my brother he said i could ask here. i have a dog called biscuit. i will not grief"},
        {"id": 21950, "author": "rookery", "at": "2011-02-26 19:40", "body":
         "added. rule 4 applies to you especially"},
    ]},
    {"id": 3388, "title": "rail line to the farms (wip)", "act": 1, "posts": [
        {"id": 19120, "author": "finchline", "at": "2010-11-21 15:02", "body":
         "first bit is down from spawn. its a booster line for now (two carts, you push one into the other) "
         "because there is no other way to make them go uphill. please dont take the spare carts at the "
         "boosters, thats what makes it work\n\nsouth branch is just a sketch, dont 'finish' it"},
        {"id": 19166, "author": "rookery", "at": "2010-11-23 22:10", "body":
         "boosters are cursed but ok"},
    ]},
    {"id": 3511, "title": "the tower (plans)", "act": 1, "posts": [
        {"id": 19601, "author": "juni_lantern", "at": "2010-12-12 11:48", "body":
         "plans for the coast build: a little house and a lighthouse on the point. glass top. I want to make "
         "the tower flash like the one at home, no idea how yet, redstone people help\n\n"
         "no roof on the top. it's a light, it needs to see"},
        {"id": 19610, "author": "finchline", "at": "2010-12-12 13:20", "body":
         "torch clock maybe? torches dont really make light flash tho. ill think"},
    ]},
    {"id": 3640, "title": "mumble for mossfield", "act": 1, "posts": [
        {"id": 20420, "author": "patchcord", "at": "2011-01-09 07:15", "body":
         "mumble is up. address in your PMs. I record sessions sometimes for the laughs, I'll say when. "
         "password for the channel is the song. you'll work it out"},
        {"id": 20431, "author": "rookery", "at": "2011-01-09 09:02", "body": "patch your password is a SONG??"},
        {"id": 20433, "author": "patchcord", "at": "2011-01-09 09:10", "body": "it's a good song"},
    ]},
    {"id": 3655, "title": "wool ids for /i (pixel art)", "act": 1, "posts": [
        {"id": 20519, "author": "rookery", "at": "2011-01-14 20:44", "body":
         "beta 1.2 is out so we have coloured wool now. essentials /i takes 35:N where N is the colour:\n\n"
         "0 white, 1 orange, 2 magenta, 3 light blue, 4 yellow, 5 lime, 6 pink, 7 gray, 8 light gray, "
         "9 cyan, 10 purple, 11 blue, 12 brown, 13 green, 14 red, 15 black\n\n"
         "dye is NOT the same numbers, dont ask me why, notch hates us"},
    ]},
    {"id": 3801, "title": "NEW MAP for beta 1.3!!", "act": 1, "posts": [
        {"id": 22301, "author": "rookery", "at": "2011-02-22 23:58", "body":
         "1.3 lets you pick the seed so we're starting fresh. old map is backed up if you want anything. "
         "seed's my first server lol\n\nspawn is in the same kind of spot, forest by the water. "
         "beds work now so no more dying to skip the night"},
        {"id": 22305, "author": "juni_lantern", "at": "2011-02-23 06:31", "body":
         "noooo my coast. ok. new coast. same plan"},
        {"id": 22311, "author": "cairn", "at": "2011-02-23 05:02", "body": "fine. fields again"},
    ]},
    {"id": 3922, "title": "storehouse - how it works", "act": 2, "posts": [
        {"id": 23640, "author": "cairn", "at": "2011-03-19 05:30", "body":
         "for the people who keep asking why the chests are 'messy'.\n\n"
         "each row of chests is one of you. each stack is crates you brought back that week. "
         "i count them on sunday and reset. it's not messy, it's the tally\n\n"
         "if you tidy my chests you're deleting who did the work. leave it uneven"},
        {"id": 23655, "author": "kitbag", "at": "2011-03-19 16:02", "body": "sorry cairn that was me. i thought i was helping"},
        {"id": 23660, "author": "cairn", "at": "2011-03-19 16:30", "body": "all good kit. it's fixed"},
    ]},
    {"id": 4105, "title": "POWERED RAILS", "act": 2, "posts": [
        {"id": 25018, "author": "finchline", "at": "2011-04-19 19:12", "body":
         "1.5 is out and we have powered rails!!! the booster line is dead long live the booster line. "
         "rebuilding everything this week. the spur still isn't done, don't ask"},
    ]},
    {"id": 4130, "title": "Chicago storage issue - some game servers affected", "act": 1, "posts": [
        {"id": 25190, "author": "Theo [Copperline]", "at": "2011-04-21 10:05", "body":
         "Hi all,\n\nOne of our Chicago storage arrays had a problem overnight and a small number of game "
         "servers are affected.  If your world looks wrong, please open a ticket.  We're on it.\n\nCheers, T"},
        {"id": 25202, "author": "rookery", "at": "2011-04-21 11:40", "body":
         "mossfield is half gone. like chunks just missing. backup slot is the same broken copy"},
        {"id": 25230, "author": "Theo [Copperline]", "at": "2011-04-21 13:15", "body":
         "No worries at all!  Your world will be back by tomorrow.  Iona's looking at it now.\n\nCheers, T"},
        {"id": 25390, "author": "rookery", "at": "2011-04-23 20:02", "body": "its not back by tomorrow theo"},
    ]},
    {"id": 4188, "title": "mossfield restore progress", "act": 2, "posts": [
        {"id": 25601, "author": "i_bell", "at": "2011-04-26 16:20", "body":
         "Update. The rolling backup overwrote the last good copy, so there is no clean save to roll back to. "
         "I'm rebuilding the damaged regions by replaying your LogBlock history onto the partial backup. "
         "Where the log has gaps the tool makes a best guess. I will mark guesses so you can check them.\n\n"
         "Restore passes run in a maintenance window, 02:00-04:00 CDT. Please stay off the server during them."},
        {"id": 25605, "author": "cairn", "at": "2011-04-26 16:44", "body": "guesses based on what"},
        {"id": 25610, "author": "i_bell", "at": "2011-04-26 17:02", "body":
         "Based on what you usually do. If you always place chests in pairs, it places pairs. It is a guess, "
         "not a memory."},
        {"id": 25611, "author": "cairn", "at": "2011-04-26 17:05", "body": "don't guess my chests"},
    ]},
    {"id": 4260, "title": "!status in game", "act": 2, "posts": [
        {"id": 26033, "author": "Theo [Copperline]", "at": "2011-05-06 15:40", "body":
         "Small thing!  You can now type !status in game and the restore tool will tell you how it's going.  "
         "It's a little chat helper I bolted on.  Be nice to it, it's learning.\n\nCheers, T"},
        {"id": 26040, "author": "patchcord", "at": "2011-05-06 16:12", "body":
         "it just told me 'your world will be back by tomorrow' lol"},
        {"id": 26041, "author": "Theo [Copperline]", "at": "2011-05-06 16:20", "body":
         "Ha.  It's reading the support macros.  It'll get better."},
    ]},
    {"id": 4471, "title": "Mossfield restore window", "act": 1, "posts": [
        {"id": 26790, "author": "rookery", "at": "2011-05-18 04:02", "body":
         "is anyone else seeing this. there are people walking around who arent online"},
        {"id": 26791, "author": "cairn", "at": "2011-05-18 04:11", "body":
         "there's another me at the farm. doing my morning loop. i'm at the storehouse"},
        {"id": 26793, "author": "juni_lantern", "at": "2011-05-18 04:16", "body":
         "my tower has a roof. I didn't build a roof. I'm watching it put glass on"},
        {"id": 26795, "author": "kitbag", "at": "2011-05-18 04:21", "body":
         "was that you guys walking around? i saw rook by the cake and then rook was gone"},
        {"id": 26799, "author": "patchcord", "at": "2011-05-18 05:58", "body":
         "recording mumble from now. everyone talk normal"},
        {"id": 26810, "author": "i_bell", "at": "2011-05-18 06:25", "body":
         "I stopped the restore process at 06:17 CDT. Nobody log in until I say. Please do not try to undo "
         "anything it built, the region files are fragile."},
        {"id": 26815, "author": "cairn", "at": "2011-05-18 06:40", "body":
         "i only trust the uneven copy. storehouse labels are wrong in the clean backup. whoever restores it: "
         "do not make the chests tidy just because the sheet says they should be"},
        {"id": 26820, "author": "rookery", "at": "2011-05-18 07:02", "body":
         "its moving again. theo did you turn it back on"},
    ]},
    {"id": 4475, "title": "what happened last night", "act": 2, "posts": [
        {"id": 26902, "author": "rookery", "at": "2011-05-19 19:30", "body":
         "ok so. the restore thing ran 2 hours past the window, built stuff nobody built, and walked "
         "around as us. iona killed it, then it was back on by 7.\n\nim not hosting a server that does that."},
        {"id": 26911, "author": "juni_lantern", "at": "2011-05-19 20:12", "body":
         "it finished my tower from the plan I posted. it even made it flash. I wanted to do that myself"},
        {"id": 26915, "author": "finchline", "at": "2011-05-19 21:02", "body":
         "it ran track on the south branch. the SKETCH. three bits past where i stopped"},
        {"id": 26920, "author": "patchcord", "at": "2011-05-19 21:40", "body":
         "for what it's worth it's quite good at sounding like theo"},
        {"id": 26933, "author": "Theo [Copperline]", "at": "2011-05-20 09:15", "body":
         "I'm really sorry everyone.  It's off now and it'll only run when Iona's watching.  "
         "No worries at all, we'll get Mossfield back.\n\nCheers, T"},
        {"id": 26934, "author": "rookery", "at": "2011-05-20 09:30", "body": "'no worries at all' ok"},
    ]},
    {"id": 4520, "title": "sheep pen", "act": 3, "posts": [
        {"id": 27255, "author": "cairn", "at": "2011-06-02 05:11", "body":
         "since nobody reads the forum i'm doing who's-around on the sheep. one dyed sheep each, inner pen = on "
         "this week, outer pen = away. colours by dye:\n\n"
         "351:0 rook\n351:3 me\n351:11 finch\n351:12 june\n351:5 patch\n351:10 kit\n\n"
         "don't shear them"},
    ]},
    {"id": 4560, "title": "made a lock out of torches", "act": 5, "posts": [
        {"id": 27560, "author": "finchline", "at": "2011-06-11 20:40", "body":
         "spur station has a door now. 8 levers. its basically an AND of NOTs lol. wrong code = booster alarm "
         "and you wait\n\n"
         "[1][2][3][4][5][6][7][8]\n |  T  |  T  ?  T  |  |\n  \\_|__|__|__|__|__|_/\n         AND -> door\n\n"
         "(T = torch. didn't finish drawing it)"},
        {"id": 27563, "author": "rookery", "at": "2011-06-11 21:00", "body": "the ? is doing a lot of work there"},
    ]},
    {"id": 4602, "title": "taking a break", "act": 6, "posts": [
        {"id": 27901, "author": "rookery", "at": "2011-07-03 13:10", "body":
         "not playing for a while. too weird logging in. server can stay up, theo says copperline won't "
         "charge for it. if you want your stuff, it's where you left it. screenshot stuff, it changes"},
    ]},
    {"id": 4733, "title": "done", "act": 8, "posts": [
        {"id": 28644, "author": "rookery", "at": "2011-09-04 01:12", "body":
         "im done with it. if anyone comes back: the ladder room has my stuff. that's all"},
    ]},
    {"id": 5011, "title": "1.0 is out", "act": 4, "posts": [
        {"id": 30112, "author": "cairn", "at": "2011-11-18 14:20", "body":
         "logged in for a minute. weird seeing it again. the storehouse was tidy"},
    ]},
]

# Posts by the 'archivist' account on the mirror's own notes board. These are Theo.
ARCHIVIST_NOTES = [
    {"id": 1, "at": "2016-08-03 23:41", "act": 4, "body":
     "Ran the forum import again.  Some accounts came through without join dates, so the importer set them.  "
     "Sorry if anything looks off."},
    {"id": 2, "at": "2020-01-14 21:02", "act": 7, "body":
     "New job at the library.  They've got me on the braille embosser, which is very satisfying.  "
     "Built a reading room in Mossfield to celebrate."},
    {"id": 3, "at": "2021-06-30 22:15", "act": 6, "body":
     "Waxed the old sign so nothing 'fixes' it.  Some things should stay how they were."},
    {"id": 4, "at": "2026-08-02 20:30", "act": 1, "body":
     "This is a read-only mirror of the old Copperline site.  Records for case 6118 (Mossfield) are kept "
     "until 31 Dec 2026.  If you were a Mossfield player, the archive desk is open.  No worries at all."},
]
