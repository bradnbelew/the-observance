# Playtest walkthrough (SPOILERS: the whole path, in order)

For testing it yourself in Chrome. Answers come from `SOLUTIONS.md`. Every step has a **Check**
line: that's what to confirm or note. Write down anything confusing, slow, broken, or too easy.
Rework of 2026-11-04: the gates, the hand-ins and the world changed; `REWORK-AUDIT.md` says how.

## Setup

| Thing | Use |
| --- | --- |
| Website | **http://localhost:3041** (stands in for copperlinehosting.com). Not the Vercel preview: that one can't follow the acts and Theo takes a whole evening to reply. |
| Minecraft | Java 1.21.11, **`localhost:25599`** (stands in for `mossfield.copperlinehosting.com`). Offline mode, so any name works. Join as **SirNan**, and **don't op yourself** (ops don't count as residents, so the game won't notice you). |
| Theo | Replies about **3 minutes** after each Archive Desk ticket (on game night it's the next evening). |
| Acts | Advance by themselves from what you do (table at the bottom). To skip a wait, ask Claude for `/mrrw dawn` or `/mrrw act N`. |

Missing media (recordings, Theo's voice notes, closet photo, forged screenshot) shows as text or is
absent. That's expected, not a bug.

---

## Act 1: The Mirror (website only)

1. Open http://localhost:3041. Read the homepage, Announcements, Network Status (the heartbeat line
   now shows the first heartbeat date, 2014-12-14), the new About page, Server List.
   **Check:** it feels like a real dead 2014 host, with nothing that looks like a game.
2. Open **http://localhost:3041/robots.txt**. It disallows `/forum-import/`, `/~mossfield/` and
   `/tcadmin/legacy/`.
3. Go to **Game Servers**, then View Source (Ctrl+U). Find the hidden link to
   `showthread.php?t=4471`. Open `/forum-import/` and browse. New threads: "skin contest", "kit's
   dirt towers", "who's on this week" (nobody answered), "bread count".
   **Check:** the forum reads like real 2010-11 people.
4. Find **Rookery's 2010 intro post**. His first server was **bracken**. Note the time zones.
5. Client Login → **Forgot password?** → `rookery` → **`bracken`** → the temporary password
   (`cl-mirror-71e4`). Log in; the Client Area shows the Mossfield service and the address.
6. Open the **Archive Desk**. Read T-01. Open ticket **6118**.

## Act 2: Mossfield (starts when you join the server)

7. Join `localhost:25599` as SirNan. **Check:** the act moves to 2 within seconds.
8. Spawn: the rules board on its plinth, Rook's "screenshot stuff" sign, the bulletin board on the
   south side, the newbie chest (take one of each and a bed: it refills at dawn), the bread chest,
   the lit Nether portal with Rook's sign, the half-built creeper face north of the square (no eyes
   yet), the Christmas-tree spot. Read every sign; Morrow's notes are on the backs of four of them
   (the rules sign, Rook's door, cairn's close-up sign, June's F2 sign).
   **Check:** it reads as a place people lived in, not a stone set.
9. `/~mossfield/`: `server.properties` (`level-seed=bracken`) and `mapkey.txt`. Walk the town with
   it: Rook's two rooms, the boot rack, his ledger on the west wall of the front room (read it
   facing west, top row first: `5/19 june: roof` ... `9/4 im done`); the cellar under the ladder
   (the floor, his "dont touch it" sign); cairn's barn (tally chests with name signs, the fixtures
   under "close-up: lot, tin chalk lamp", the oven, the flour), the shed with the dirt gap and
   "bed was here", his cobble hut by the fields, the pen gate sign; finch's station board, the
   spare carts, the three `jct` levers on the line, the bench at the south branch, the memorial at
   the farm stop; June's cottage (F2 sign, the flag locker, the lamp cabinet), finch's torch clock
   on the pad in her yard, the tower (no dock); patch's studio (the mixer desk, the mic, the
   headphones, the note wall with "play from here ->"); Kit's dirt towers on the way to his hut, the
   kennel with the dogs' signs; the x-ray cave under spawn.
   **Check:** nothing sunk or floating; the low path track runs through where lowpath's house now
   stands on its hill; the south rail creeps and never enters the house.
10. The trapdoor tell: the shed hatch is an oak trapdoor. In LogBlock (from Act 3) cairn "placed"
    it on 2011-05-12 06:50; trapdoors arrived 26 May 2011.

## Act 3: The Tidy (45 min online since Act 2, then a dawn)

11. After the dawn: the rules sign says "before"; the creeper has its eyes ("24 blocks restored
    (spawn)" in chat); golems; June's lamp flashes; Morrow answers in chat (never your own line
    back at you).
12. `/~mossfield/logblock`: player `morrow` placed glass on June's tower 04:11:04-04:15:04. Filing
    it at the desk gets "That's not anyone" but moves nothing now.
13. June's lamp flashes **Fl W 5s**; so does finch's torch clock in her yard (count the repeaters:
    six at 4 ticks, plus the torch, doubled = 50 ticks). Sambro Island Light. Open
    `/~juni_lantern/` with `sambro`. **Opening it moves you to Act 4** (the site posts
    `june_folder`).
14. The tally book has a `lowpath` row; the real chests don't. Backup: cairn's "bread count" post:
    142 - 40 - 24 - 20 = 58 = the week-of-9-May column of the real tally; with lowpath it's 60.

## Act 4: The Archivist

15. Server logs, the auction PDF. File any two of `Lot 14`, `CHI-07`, `2014-12-14`, `waived`.
    The reply follows what you filed (T-04c for the lot, T-04d for the date, T-04e for waived, else
    T-04a); one token gets "That's one thing. I'd want two". **Unmasking moves you to Act 5.**
    Notes board: "Box is up in the spare room" (2014).

## Act 5: Replay

16. At dusk, if you're near where a figure starts (spawn, the farm, June's door, patch's hut), chat
    says `[Recovery] debug view: replay 2011-05-18 04:00` and the figures begin, at walking pace,
    for about eight minutes, lines above their heads. **Check:** skins, bubbles follow, nobody
    teleports. (If nobody is near any start for three minutes it runs anyway.)
17. patchcord's folder: `patchcord` / `baggage` (read the note wall left to right from the door).
    Ask the desk for `moss_0518.ogg` (text only until recorded).
18. Rookery's floor (`-270 62 -5`): from the ladder landing, the nearest row is row 1; rows 1-3 are
    Morrow's colour run (his sign's back says `floor: 3 rows normalised`; the first dawn you're
    online after Act 5 opens announces it). Rebuild them from LogBlock, boustrophedon, the original
    rules as the key. Plaintext `STOPNEEDSTHREETHINGS...DONTTELLTHEO`; md5 in the checklist.
19. **The hand-in is in the world:** edit the MAINT sign at spawn (`-236 71 32`) to `MAINT ON`.
    Within a minute: `maint: on. sessions open: 1. restore continues`. At dawn it reverts
    (`1 sign normalised (spawn)`) and **that moves you to Act 6**. Filing "dont tell theo" gets
    Rowan's line and moves nothing.
20. C3: LogBlock on 2011-06-11 21:02 shows finch's levers (jct 1 on, 2 and 3 off). Set them, ride
    from the hub: the cart takes the spur. Her station (torch lock `10110010`) holds a chest with
    the stop in her words. Side puzzle: slipped hands → `putting it on rook's ftp`.

## Act 6: Correction

21. Forum, status, three landmark signs, the board's spur note and the last line of Rook's ledger
    are rewritten (once per act, at night). Each tell: CST in May, or a line over 15 characters.
22. TCAdmin: `rookery` / `cl-mirror-71e4`. The console takes a key (wrong keys are "rejected").
23. `panel_ok.jpg`'s EXIF thumbnail: the Start confirmation and the Mumble window behind it
    (rookery, patchcord idle, tvale, 06:44).
24. File `06:44:10` plus `mid-session` (T-06a) or plus `CHI-07/mrrw` (T-06d). **Moves you to Act 7.**
    The night after: T-06c on the desk.

## Act 7: Voices

25. Braille shelves → `sorry kit` (the visitor book on the lectern beside them). Annex: the manhole
    sign on the hill, the ops room, the ID row along the south wall read facing south → `HALTMRRW`.
    Flags → `K` + `PORCHLIGHT`.
26. Iona's chest at `-238 61 232`: no log rows; read it in the world in ONE session (the dawn after
    it is first opened Morrow merges it). Counts are base64 index + 1, top to bottom as the chest
    shows them: `mrrw:halt:case6118:2011-06-02:i_bell`.
27. **The hand-in is the console:** paste the key in TCAdmin's console → `key accepted. stop
    refused: maint=off sessions=1 (lowpath)`. **Moves you to Act 8.** Filed at the desk, Theo says
    "That's not for me. Put it where she'd put it." Two days in: T-07d.

## Act 8: Continuity

28. lowpath walks at dusk (`debug view: replay lowpath`), with stops, about five minutes, and
    stands at its place till dawn. A bed placed there: `lowpath's place: 1 block queued`, gone at
    dawn.
29. Theo's box (`tvale` / `chi07-2010`): the two heartbeat logs. March 2019: `down` 22:55-23:30,
    back at 23:35 with `up=0d00:00:50`, four minutes after Kit's email. T-08a now says so.
30. cairn's routine: inside the barn, along the north wall from the door (plate, tin, returns,
    lamp). Dust corridor → `SESSIONS:1:LOWPATH`. Filing `kit` **moves you to Act 9** (36 h).

## Act 9: Someone Comes Home

31. Your profile: a 2010 join date, Location in Iona's region notation (your first-join spot), post
    ids above 30112, LogBlock rows "from 2010" placing hay and carpet.
32. Put a bed on the west wall of lowpath's place (`-214 73 86`): `bed kept`. That night lowpath
    walks in and lies down: `session lowpath: end (slept)`.
33. `/ftp/`: the two zips; the Residents' Key; the case brief (4 of 5). **Moves you to Act 10.**
    Next morning: T-09c.

## Act 10: Back By Tomorrow

34. Book a window; Theo asks about finch's sign. Say "leave it" (or "finish it") in Mossfield chat:
    that's the stop log's reason.
35. Wax MAINT ON. Console: `HALTMRRW` + the key; the vote (Discord, or ask Claude); the code in
    chat at the annex. Morrow: the code, then `you can turn it off, its ok`. Then the residents walk
    home one by one (`session rookery: end` ...), the golems stop, the log, the last flash, lights
    out. Status page: "no heartbeat" beside "first heartbeat 2014-12-14".

---

## What moves each act

| To act | When |
| --- | --- |
| 2 | you (not an op) join the server |
| 3 | 45 min online since Act 2, then a dawn |
| 4 | June's folder opened with `sambro` |
| 5 | desk: Theo unmasked (any variant) |
| 6 | MAINT ON set at spawn, reverted by Morrow at dawn |
| 7 | desk: the 06:44 restart |
| 8 | Iona's key entered in the TCAdmin console |
| 9 | desk: Kit's line |
| 10 | desk: case brief accepted |

Filing something too early does nothing; file it again once the act catches up.
