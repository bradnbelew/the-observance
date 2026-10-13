"""Copperline Community Forums (vBulletin 3.8), board 'Customer Servers > Mossfield'.

Times are server-local Chicago time, which is also how the forum displayed them.
Writing rules: residents type like 2010 teenagers and twenty-somethings, short and
lowercase; Theo leaves two spaces after a full stop; Iona writes CDT in summer and
CST in winter and gets it right. Anything Morrow later rewrites keeps the original
in `cached` and gets a forgery tell (wrong zone, wrong era, wrong voice).

Most threads are just a small server being a small server. Only a few carry clues.

`act` is the first act in which a post is reachable on the mirror. Post and thread IDs are
forum-wide (other boards fill the gaps), so they only ever rise with time. A post or thread
written with id None gets one interpolated from its neighbours.
"""

from __future__ import annotations

from datetime import datetime


def user_title(posts: int) -> str:
    """vBulletin 3.8 default ladder."""
    return "Junior Member" if posts < 30 else "Member" if posts < 100 else "Senior Member"


MEMBERS = {
    "rookery": {"joined": "2010-10-30", "location": "Ottawa, ON",
                "local_offset": "+1 hr", "posts": 412, "sig": "mossfield // whitelist: ask"},
    "cairn": {"joined": "2010-11-04", "location": "Ames, IA",
              "local_offset": "same as forum", "posts": 158, "sig": "leave it uneven"},
    "finchline": {"joined": "2010-11-20", "location": "Grand Rapids, MI",
                  "local_offset": "+1 hr", "posts": 233, "sig": "next stop: somewhere"},
    "juni_lantern": {"joined": "2010-12-11", "location": "Halifax, NS",
                     "local_offset": "+2 hrs", "posts": 301, "sig": "F2 F2 F2", "homepage": "/~juni_lantern/"},
    "patchcord": {"joined": "2011-01-08", "location": "Manchester, UK",
                  "local_offset": "+6 hrs", "posts": 189, "sig": "i play bass. everything else is decoration",
                  "homepage": "/~patchcord/"},
    "kitbag": {"joined": "2011-02-26", "location": "Naperville, IL",
               "local_offset": "same as forum", "posts": 47, "sig": "biscuit is a good dog",
               "interests": "minecraft, my dog biscuit, halo"},
    "i_bell": {"title": "Copperline Staff", "joined": "2009-03-02", "location": "Chicago, IL",
               "local_offset": "same as forum", "posts": 96,
               "sig": "Iona Bell // Copperline Recovery // alias: iona_bell"},
    "Theo [Copperline]": {"title": "Copperline Support", "joined": "2009-01-19", "location": "Chicago, IL",
                          "local_offset": "same as forum", "posts": 2244,
                          "sig": "Cheers, T  //  Copperline Support  //  1-877-555-0142", "homepage": "/~tvale/"},
    "lowpath": {"joined": "2010-11-02", "location": "",
                "local_offset": "same as forum", "posts": 0, "sig": "",
                "import_note": "account created 2016-08-03 by archive import (join date set by importer)"},
    "archivist": {"title": "Archive", "joined": "2014-12-14", "location": "",
                  "local_offset": "same as forum", "posts": 0, "sig": "",
                  "note": "notes board was private until the mirror opened in 2026"},
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
         "cairn. i'll do farms if nobody else wants to. i'm up at weird hours anyway"},
        {"id": 18890, "author": "rookery", "at": "2010-11-04 08:30", "body":
         "added. 5am cairn wtf"},
        {"id": 18902, "author": "cairn", "at": "2010-11-04 09:01", "body":
         "bakery. i'm off at 4. you get used to it"},
        {"id": 19117, "author": "finchline", "at": "2010-11-20 16:40", "body":
         "finchline pls! i want to do a rail line from spawn to wherever cairn's farms end up"},
        {"id": 19583, "author": "juni_lantern", "at": "2010-12-11 18:05", "body":
         "hi! juni_lantern, halifax. I mostly build and take way too many screenshots. is anyone "
         "near the water yet? I want the coast"},
        {"id": 19590, "author": "rookery", "at": "2010-12-11 18:30", "body":
         "coast is yours. east side past the gravel beach"},
        {"id": 20412, "author": "patchcord", "at": "2011-01-08 13:22", "body":
         "patchcord, manchester. heard there's a mumble going? I can host one, I've got a box doing nothing"},
        {"id": 22410, "author": "kitbag", "at": "2011-02-26 19:11", "body":
         "hi im kit. theo is my brother he said i could ask here. i have a dog called biscuit. i will not grief"},
        {"id": 22418, "author": "rookery", "at": "2011-02-26 19:40", "body":
         "added. rule 4 applies to you especially"},
    ]},
    {"id": None, "title": "creeper ate my house", "act": 1, "posts": [
        {"id": None, "author": "cairn", "at": "2010-11-12 07:40", "body":
         "came back from the fields and there's a hole where my house was. creeper. i had one chest in there"},
        {"id": None, "author": "rookery", "at": "2010-11-12 16:15", "body": "lol welcome to mossfield"},
        {"id": None, "author": "cairn", "at": "2010-11-13 06:02", "body": "rebuilt it in cobble. let it try"},
    ]},
    {"id": 3388, "title": "rail line to the farms (wip)", "act": 1, "posts": [
        {"id": 19120, "author": "finchline", "at": "2010-11-21 15:02",
         "edited": "Last edited by finchline; 11-21-2010 at 03:20 PM.", "body":
         "first bit is down from spawn. its a booster line for now because there is no other way to make "
         "them go uphill. please dont take the spare carts at the boosters\n\n"
         "south branch is just cobble marking for now, ill get to it"},
        {"id": 19166, "author": "rookery", "at": "2010-11-23 22:10", "body":
         "boosters are cursed but ok"},
    ]},
    {"id": None, "title": "free bread", "act": 1, "posts": [
        {"id": None, "author": "cairn", "at": "2010-11-27 05:31", "body":
         "chest of bread by the storehouse door. take what you want, i always end up with too much"},
        {"id": None, "author": "finchline", "at": "2010-11-27 15:48", "body": "cairn ur the best"},
        {"id": None, "author": "rookery", "at": "2010-11-27 19:02", "body": "is this a bakery joke"},
        {"id": None, "author": "cairn", "at": "2010-11-28 05:10", "body": "it's a bread joke :P"},
    ]},
    {"id": 3511, "title": "the tower (plans)", "act": 1, "posts": [
        {"id": 19601, "author": "juni_lantern", "at": "2010-12-12 11:48",
         "edited": "Last edited by juni_lantern; 12-12-2010 at 12:02 PM. Reason: spelling", "body":
         "plans for the coast build: a little house and a lighthouse on the point. glass top. I want to make "
         "the tower flash like the one at home, no idea how yet, redstone people help\n\n"
         "no roof on top, its a lighthouse lol"},
        {"id": 19610, "author": "finchline", "at": "2010-12-12 13:20", "body":
         "torch clock maybe? torches dont really make light flash tho. ill think"},
    ]},
    {"id": None, "title": "skins", "act": 1, "posts": [
        {"id": None, "author": "juni_lantern", "at": "2010-12-16 17:30", "body":
         "how do I change my skin?? I made a lighthouse keeper one"},
        {"id": None, "author": "rookery", "at": "2010-12-16 18:02", "body":
         "minecraft.net, profile, upload the png. shows up next time you log in"},
        {"id": None, "author": "juni_lantern", "at": "2010-12-16 18:40", "body": "it worked!! I have a little hat :)"},
        {"id": None, "author": "finchline", "at": "2010-12-17 16:11", "body": "mine is just steve with a red shirt lol"},
    ]},
    {"id": None, "title": "christmas tree at spawn?", "act": 1, "posts": [
        {"id": None, "author": "rookery", "at": "2010-12-22 20:45", "body":
         "anyone want to do a tree at spawn. logs and torches, bring torches pls"},
        {"id": None, "author": "finchline", "at": "2010-12-22 21:10", "body": "on it"},
        {"id": None, "author": "juni_lantern", "at": "2010-12-24 19:30", "body": "merry christmas mossfield :)"},
    ]},
    {"id": None, "title": "happy new year", "act": 1, "posts": [
        {"id": None, "author": "rookery", "at": "2011-01-01 00:02", "body": "happy new year from ottawa, an hour ago lol"},
        {"id": None, "author": "juni_lantern", "at": "2011-01-01 00:09", "body":
         "happy new year! it's been 2011 here for two hours, it's fine"},
        {"id": None, "author": "cairn", "at": "2011-01-01 05:34", "body": "happy new year. worked through it. there's still bread"},
    ]},
    {"id": 3640, "title": "mumble for mossfield", "act": 1, "posts": [
        {"id": 20420, "author": "patchcord", "at": "2011-01-09 07:15", "body":
         "mumble is up. address in your PMs. I might record sessions for the laughs if I can work out how, "
         "I'll say when. password for the channel is the song. you'll work it out"},
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
    {"id": None, "title": "lag tonight?", "act": 1, "posts": [
        {"id": None, "author": "finchline", "at": "2011-01-20 19:58", "body": "is it just me or is it lagging like crazy"},
        {"id": None, "author": "patchcord", "at": "2011-01-20 20:04", "body":
         "rubber banding here too. but I'm in manchester so I always lag"},
        {"id": None, "author": "rookery", "at": "2011-01-20 20:10", "body":
         "someone has a cobble generator running in a loop. finch??"},
        {"id": None, "author": "finchline", "at": "2011-01-20 20:11", "body": "...maybe"},
        {"id": None, "author": "Theo [Copperline]", "at": "2011-01-20 20:40", "body":
         "Hi!  We're seeing some load on the Chicago nodes tonight too.  Should settle down shortly.\n\nCheers, T"},
    ]},
    {"id": None, "title": "mumble etiquette", "act": 1, "posts": [
        {"id": None, "author": "patchcord", "at": "2011-01-25 14:20", "body":
         "push to talk please. I can hear someone's washing machine"},
        {"id": None, "author": "juni_lantern", "at": "2011-01-25 17:05", "body": "that was my dryer, sorry"},
        {"id": None, "author": "cairn", "at": "2011-01-26 05:12", "body": "i don't have a mic"},
        {"id": None, "author": "patchcord", "at": "2011-01-26 12:30", "body": "that's fine cairn. you can listen"},
    ]},
    {"id": None, "title": "move host?", "act": 1, "posts": [
        {"id": None, "author": "finchline", "at": "2011-02-02 16:50", "body":
         "server's been down twice this week. should we move? my friend's server is on another host and it "
         "never goes down"},
        {"id": None, "author": "rookery", "at": "2011-02-02 17:30", "body":
         "[QUOTE=finchline]should we move?[/QUOTE]\n\ncopperline is cheap and theo actually answers. the other "
         "ones you get a bot"},
        {"id": None, "author": "patchcord", "at": "2011-02-02 17:41", "body":
         "I could host it on my box but my upload is rubbish"},
        {"id": None, "author": "juni_lantern", "at": "2011-02-02 18:15", "body":
         "please don't move it until I finish the tower"},
        {"id": None, "author": "Theo [Copperline]", "at": "2011-02-03 09:20", "body":
         "Sorry about this week, everyone.  We had a bad switch in Chicago.  No worries at all!  We'll sort it.\n\nCheers, T"},
        {"id": None, "author": "rookery", "at": "2011-02-03 16:05", "body": "see. he answers"},
        {"id": None, "author": "finchline", "at": "2011-02-05 13:12", "body": "ok fine. staying"},
    ]},
    {"id": 3801, "title": "NEW MAP for beta 1.3!!", "act": 1, "posts": [
        {"id": 22301, "author": "rookery", "at": "2011-02-22 23:58", "body":
         "1.3 lets you pick the seed so we're starting fresh. old map is backed up if you want anything. "
         "seed's my first server lol\n\nspawn is in the same kind of spot, forest by the water. "
         "beds work now so no more dying to skip the night"},
        {"id": 22305, "author": "cairn", "at": "2011-02-23 05:02", "body": "fine. fields again"},
        {"id": 22311, "author": "juni_lantern", "at": "2011-02-23 06:31", "body":
         "noooo my coast. ok. new coast. same plan"},
    ]},
    {"id": None, "title": "beds on the server", "act": 1, "posts": [
        {"id": None, "author": "rookery", "at": "2011-02-24 20:20", "body":
         "beds work on smp but EVERYONE online has to be in bed to skip the night. so if you're on at night "
         "and not in bed, people will yell at you"},
        {"id": None, "author": "cairn", "at": "2011-02-25 05:20", "body": "i'm up when you're all asleep anyway"},
        {"id": None, "author": "juni_lantern", "at": "2011-02-25 06:40", "body":
         "[QUOTE=rookery]people will yell at you[/QUOTE]\n\nit's me. I'm people :P"},
    ]},
    {"id": None, "title": "who took my minecart", "act": 2, "posts": [
        {"id": None, "author": "finchline", "at": "2011-03-05 16:22", "body":
         "who took the spare cart from the hub. there are supposed to be TWO"},
        {"id": None, "author": "kitbag", "at": "2011-03-05 17:40", "body": "i rode it to the farms sorry"},
        {"id": None, "author": "finchline", "at": "2011-03-05 17:52", "body": "you have to bring it BACK kit"},
        {"id": None, "author": "kitbag", "at": "2011-03-06 11:03", "body": "i brought it back"},
    ]},
    {"id": None, "title": "server down?", "act": 2, "posts": [
        {"id": None, "author": "kitbag", "at": "2011-03-12 19:31", "body": "server down?"},
        {"id": None, "author": "Theo [Copperline]", "at": "2011-03-12 19:44", "body":
         "Hi!  Brief storage maintenance on the Chicago nodes tonight, should be back in twenty minutes.  "
         "Sorry for the bother!\n\nCheers, T"},
        {"id": None, "author": "kitbag", "at": "2011-03-12 20:06", "body": "its back"},
    ]},
    {"id": 3922, "title": "storehouse - how it works", "act": 2, "posts": [
        {"id": 23640, "author": "cairn", "at": "2011-03-19 05:30", "body":
         "for the people who keep asking why the chests are 'messy'.\n\n"
         "each row of chests is one of you. each stack is crates you brought back that week. "
         "i count them on sunday and start a new stack. it's not messy, it's the tally\n\n"
         "if you tidy my chests you're deleting who did the work. leave it uneven"},
        {"id": 23655, "author": "kitbag", "at": "2011-03-19 16:02", "body": "sorry cairn that was me. i thought i was helping"},
        {"id": 23660, "author": "cairn", "at": "2011-03-19 16:30", "body": "all good kit. it's fixed"},
        {"id": None, "author": "kitbag", "at": "2011-03-19 16:41", "body": "how do you close it up at night"},
        {"id": None, "author": "cairn", "at": "2011-03-20 05:15", "body":
         "close-up: walk the lot, check the hinge tin, chalk the returns, lamp off. in that order or i "
         "lose count"},
    ]},
    {"id": None, "title": "someone is xraying", "act": 2, "posts": [
        {"id": None, "author": "rookery", "at": "2011-03-25 21:05", "body":
         "someone dug a perfect tunnel straight to the diamonds under spawn. rule 4. i will know"},
        {"id": None, "author": "kitbag", "at": "2011-03-25 21:09", "body": "it wasnt me"},
        {"id": None, "author": "rookery", "at": "2011-03-25 21:12", "body":
         "i know it wasnt you kit. you would have left a dirt tower"},
        {"id": None, "author": "patchcord", "at": "2011-03-26 07:40", "body":
         "it was a cave, rook. I was there. it's a cave"},
        {"id": None, "author": "rookery", "at": "2011-03-26 10:02", "body": "...ok it's a cave"},
    ]},
    {"id": None, "title": "WOLVES", "act": 2, "posts": [
        {"id": None, "author": "kitbag", "at": "2011-04-01 16:10", "body":
         "WOLVES!!! you can tame them with bones. i have three"},
        {"id": None, "author": "cairn", "at": "2011-04-01 16:44", "body":
         "keep them away from my sheep. also cookies are in. i'm a baker and they're still wrong"},
        {"id": None, "author": "juni_lantern", "at": "2011-04-01 18:20", "body": "I named mine Captain :)"},
        {"id": None, "author": "kitbag", "at": "2011-04-01 18:31", "body": "mine are biscuit 2 biscuit 3 and biscuit 4"},
        {"id": None, "author": "rookery", "at": "2011-04-01 19:00", "body": "is this an april fools thing"},
        {"id": None, "author": "kitbag", "at": "2011-04-01 19:02", "body": "no theyre real look"},
    ]},
    {"id": None, "title": "sheep pen", "act": 3, "posts": [
        {"id": None, "author": "cairn", "at": "2011-04-04 05:11", "body":
         "nobody answers the 'who's on this week' thread so i'm doing it with sheep. one dyed sheep each, "
         "inner pen = on this week, outer pen = away. colours by dye:\n\n"
         "351:0 rook\n351:3 me\n351:11 finch\n351:12 june\n351:5 patch\n351:10 kit\n\n"
         "don't shear them"},
        {"id": None, "author": "finchline", "at": "2011-04-04 16:30", "body": "cairn this is the best and worst idea"},
    ]},
    {"id": None, "title": "anyone have spare glass", "act": 2, "posts": [
        {"id": None, "author": "juni_lantern", "at": "2011-04-09 17:12", "body":
         "I need like 3 stacks of glass for the tower. will trade fish"},
        {"id": None, "author": "finchline", "at": "2011-04-09 17:30", "body": "ill smelt some, bring sand"},
        {"id": None, "author": "juni_lantern", "at": "2011-04-09 17:33", "body": "you're the best"},
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
         "Based on what you usually do. If you always place chests in pairs, it places pairs. It's a guess. "
         "I'll mark them."},
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
    {"id": None, "title": "cake at spawn saturday", "act": 2, "posts": [
        {"id": None, "author": "juni_lantern", "at": "2011-05-12 17:40", "body":
         "cake at spawn saturday night for no reason. I'm taking a group picture so be there"},
        {"id": None, "author": "kitbag", "at": "2011-05-12 18:02", "body": "can i come"},
        {"id": None, "author": "rookery", "at": "2011-05-12 18:20", "body":
         "[QUOTE=kitbag]can i come[/QUOTE]\n\nyou live here kit"},
        {"id": None, "author": "cairn", "at": "2011-05-13 05:44", "body": "i'll bring wheat. i guess i'm making the cake"},
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
         "if anyone touches the storehouse after this: dont sort my chests. the uneven ones are right"},
        {"id": 26820, "author": "rookery", "at": "2011-05-18 07:02", "body":
         "its moving again. theo did you turn it back on"},
    ]},
    {"id": 4475, "title": "what happened last night", "act": 2, "posts": [
        {"id": 26902, "author": "rookery", "at": "2011-05-19 19:30", "body":
         "ok so. the restore thing ran 2 hours past the window, built stuff nobody built, and walked "
         "around as us. iona killed it, then it was back on by 7.\n\nim not hosting a server that does that."},
        {"id": 26911, "author": "juni_lantern", "at": "2011-05-19 20:12", "body":
         "it finished my tower from the plan I posted. it even tried to make it flash. I wanted to do that myself"},
        {"id": 26915, "author": "finchline", "at": "2011-05-19 21:02", "body":
         "it ran track on the south branch. past my cobble. three bits past where i stopped"},
        {"id": 26920, "author": "patchcord", "at": "2011-05-19 21:40", "body":
         "for what it's worth it's quite good at sounding like theo"},
        {"id": 26933, "author": "Theo [Copperline]", "at": "2011-05-20 09:15", "body":
         "I'm really sorry everyone.  It's off now and it'll only run when Iona's watching.  "
         "No worries at all, we'll get Mossfield back.\n\nCheers, T"},
        {"id": 26934, "author": "rookery", "at": "2011-05-20 09:30", "body": "'no worries at all' ok"},
    ]},
    {"id": None, "title": "nether on the server now", "act": 5, "posts": [
        {"id": None, "author": "rookery", "at": "2011-05-27 22:14", "body":
         "1.6 put the nether in multiplayer. i built a hub, coords on a sign by it. remember it's x8"},
        {"id": None, "author": "finchline", "at": "2011-05-27 22:30", "body": "TRAPDOORS"},
        {"id": None, "author": "rookery", "at": "2011-05-27 22:31", "body": "we are not putting trapdoors on everything finch"},
    ]},
    {"id": 4560, "title": "made a lock out of torches", "act": 5, "posts": [
        {"id": 27560, "author": "finchline", "at": "2011-06-11 20:40", "body":
         "spur station has a door now. 8 levers. its basically an AND of NOTs lol. wrong code = booster alarm "
         "and you wait\n\n"
         "[1][2][3][4][5][6][7][8]\n |  T  |  T  ?  T  |  |\n  \\_|__|__|__|__|__|_/\n         AND -> door\n\n"
         "(T = torch. didn't finish drawing it)"},
        {"id": 27563, "author": "rookery", "at": "2011-06-11 21:00", "body": "the ? is doing a lot of work there"},
    ]},
    {"id": None, "title": "anyone still on?", "act": 6, "posts": [
        {"id": None, "author": "juni_lantern", "at": "2011-06-20 18:10", "body": "anyone still playing? it's quiet"},
        {"id": None, "author": "cairn", "at": "2011-06-21 05:40", "body": "i log in sometimes. it's weird. i don't build"},
        {"id": None, "author": "finchline", "at": "2011-06-21 16:02", "body": "same"},
    ]},
    {"id": 4602, "title": "taking a break", "act": 6, "posts": [
        {"id": 27901, "author": "rookery", "at": "2011-07-03 13:10", "body":
         "not playing for a while. too weird logging in. server can stay up, theo says copperline won't "
         "charge for it. if you want your stuff, it's where you left it. screenshot stuff, it changes"},
    ]},
    {"id": 4733, "title": "done", "act": 8, "posts": [
        {"id": 28644, "author": "rookery", "at": "2011-09-04 01:12", "body":
         "its back on again. iona's gone and its back on. im done with it. if anyone comes back: the ladder "
         "room has my stuff. off the nether hub"},
    ]},
    {"id": 5011, "title": "1.0 comes out today", "act": 4, "posts": [
        {"id": 30112, "author": "cairn", "at": "2011-11-18 05:20", "body":
         "logged in for a minute. weird seeing it again. the storehouse was tidy"},
    ]},
]

# Posts by the 'archivist' account on the mirror's own notes board. These are Theo.
ARCHIVIST_NOTES = [
    {"id": 1, "at": "2016-08-03 23:41", "act": 4, "body":
     "Ran the forum import again.  Some accounts came through without join dates, so the importer set them.  "
     "Sorry if anything looks off."},
    {"id": 2, "at": "2020-01-14 21:02", "act": 7, "body":
     "New job at the library.  They've got me on the braille embosser, which is very satisfying."},
    {"id": 3, "at": "2023-06-09 22:15", "act": 6, "body":
     "1.20 lets you wax signs now.  Waxed the old sign so nothing 'fixes' it."},
    {"id": 4, "at": "2023-06-11 20:40", "act": 7, "body":
     "Built a reading room in Mossfield.  The new shelves are perfect for it."},
    {"id": 5, "at": "2026-08-02 20:30", "act": 1, "body":
     "This is a read-only mirror of the old Copperline site.  Records for case 6118 (Mossfield) are kept "
     "until 31 Dec 2026.  If you were a Mossfield player, the archive desk is open.  No worries at all."},
]


# ---------------------------------------------------------------------------
# Derived fields: user titles by post count, archivist post count, interpolated IDs.

def _ts(at: str) -> float:
    return datetime.fromisoformat(at).timestamp()


def _interpolate(items: list[dict], when) -> None:
    """Give every item with id None an id between its dated neighbours (ids rise with time)."""
    known = sorted((when(it), it["id"]) for it in items if it["id"] is not None)
    used = {it["id"] for it in items if it["id"] is not None}
    pending = sorted((it for it in items if it["id"] is None), key=when)
    for it in pending:
        t = when(it)
        before = [k for k in known if k[0] <= t]
        after = [k for k in known if k[0] > t]
        lo_t, lo = before[-1] if before else (t - 86400, after[0][1] - 40)
        hi_t, hi = after[0] if after else (t + 86400, lo + 40)
        n = lo + max(1, int((hi - lo) * (t - lo_t) / max(hi_t - lo_t, 1)))
        n = min(max(n, lo + 1), hi - 1)
        while n in used:
            n += 1
        assert lo < n < hi, (it, lo, hi)
        it["id"] = n
        used.add(n)
        known = sorted(known + [(t, n)])


for _m in MEMBERS.values():
    _m.setdefault("title", user_title(_m["posts"]))
MEMBERS["archivist"]["posts"] = len(ARCHIVIST_NOTES)

_interpolate([p for t in THREADS for p in t["posts"]], lambda p: _ts(p["at"]))
_interpolate(THREADS, lambda t: _ts(t["posts"][0]["at"]))
THREADS.sort(key=lambda t: t["id"])
