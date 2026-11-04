# Playtest walkthrough (SPOILERS: the whole path, in order)

For testing it yourself in Chrome. Answers come from `SOLUTIONS.md`. Every step has a **Check**
line: that's what to confirm or note. Write down anything confusing, slow, broken, or too easy.

## Setup (already running, 2026-11-01)

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

1. Open http://localhost:3041. Read the homepage, Announcements, Network Status, Server List.
   **Check:** it feels like a real dead 2014 host, with nothing that looks like a game.
2. Open **http://localhost:3041/robots.txt**. It disallows `/forum-import/`, `/~mossfield/` and
   `/tcadmin/legacy/`.
   **Check:** would a player think to look here?
3. Go to **Game Servers**, then View Source (Ctrl+U). Find the hidden link to
   `showthread.php?t=4471`. Open `/forum-import/` and browse.
   **Check:** the forum reads like real 2010–11 people. Members list, profiles, thread 3102 (Rookery's
   "new survival server").
4. In the forum, find **Rookery's 2010 intro post**. The name of his first server is **bracken**.
   Note the roster time zones while you're there (June +2 Halifax, patchcord +6 Manchester, finch and
   Rookery +1, cairn and Kit 0).
5. Homepage → Client Login → **Forgot password?** → username **`rookery`** → the security question →
   **`bracken`**. Try a wrong answer first.
   **Check:** the wrong answer is rejected cleanly. The right one shows a temporary password
   (`cl-mirror-71e4`). Remember it, because it's reused in Act 6.
6. Log in (rookery / that password). The **Client Area** shows the Mossfield service on node
   CHI-07, and the server address.
   **Check:** the address is obvious, and it's clear you're meant to join.
7. Open the **Archive Desk** (top right). Read the welcome from Theo (T-01). Open ticket **6118**
   (`/viewticket.php`).
   **Check:** Theo's tone (quiet, says "no worries", trails off). The desk explains how filing works.

## Act 2: Mossfield (starts when you join the server)

8. Join `localhost:25599` as SirNan. Look at the server list entry first: the hover shows "players
   online", and the MOTD colour bars are a hidden hex channel.
   **Check:** the act moves to 2 within a few seconds of joining. Refresh the site and new things
   appear (Dynmap, `/~mossfield/`).
9. On the site, open **`/~mossfield/`**: it keeps the 2011 `server.properties` (`level-seed=bracken`)
   and Rookery's **`mapkey.txt`** (place coordinates, plus a "low path" note next to the map tiles).
   **Check:** the map key is usable as an in-game guide.
10. In game, walk the town using the map key: spawn (rules signs, cake table, `-233 70 30`),
    Rook's (`-262 71 -2`), storehouse/cairn's (`-80 72 -140`), farms, sheep, June's tower
    (`-243 66 249`), Theo's museum (`-252 70 -30`).
    **Check:** builds sit right on the ground, nothing looks sunk or out of place. Read sign backs
    (Morrow's notes). Break a block and see if it comes back at dawn. The south rail creeps.

## Act 3: The Tidy (45 min online since Act 2, then a dawn)

11. Play in Mossfield for 45 minutes total, then wait for an in-game sunrise (or ask Claude for
    `/mrrw dawn`). The act moves to 3.
    **Check:** what changed is noticeable. The rules sign now says "before", iron golems, June's lamp
    flashes, Morrow starts answering in chat.
12. Site: **`/~mossfield/logblock`**. Search player **`morrow`**: it placed glass on June's tower
    04:11:04–04:15:04 on 2011-05-18 (glowstone at 04:13:50).
    **Check:** the LogBlock search is easy to use, and the morrow rows feel like a find.
13. Archive Desk: file a ticket naming **`morrow`** plus a detail (`glass`, `glowstone`, `tower`,
    `04:11` or `logblock`). Theo answers in about 3 minutes ("That's not anyone...").
    **Check:** Theo's reply lands, and a partial filing gives a helpful nudge. **This moves you to
    Act 4.**
14. June's lighthouse lamp in game flashes **Fl W 5s** = Sambro Island Light. Open
    **`/~juni_lantern/`**, user `juni_lantern`, password **`sambro`**: June's screenshots.
    **Check:** the browser login box appears. June's 9 screenshots load and match the world.
15. In game, the **tally book** has a `lowpath` row, but cairn's real chests don't (storehouse).
    **Check:** the discrepancy is findable without being told.

## Act 4: The Archivist

16. New: server logs, an auction PDF drop at the desk. File any two of **`Lot 14`**, **`CHI-07`**,
    **`2014-12-14`**, **`waived`** to unmask Theo as the man who bought the node.
    **Check:** the clues to those four are discoverable. **Unmasking moves you to Act 5.**

## Act 5: Replay

17. A nightly incident replay plays in game (figures in the residents' skins, new tonight).
    **Check:** the skins show (not Steve/Alex), the figures walk their paths, chat lines appear.
18. patchcord's folder: `/~patchcord/`, user `patchcord`, password **`baggage`** (note names of
    the bass line).
19. Ask the desk for **`moss_0518.ogg`** (named in the replay and server log). *Recording not made
    yet: you'll get the text only.*
20. Rookery's wool floor (`-270 62 -5`): the key is the original spawn rules, letters only. Plaintext
    `STOPNEEDSTHREETHINGS...DONTTELLTHEO`. Filing `floor` or `moss_0518` moves you on.
    Side puzzles: "slipped hands" `[iyyomh oy pm tppl#d gy[` → `putting it on rook's ftp` (UK
    keyboard). finch's lock (`-224 70 -116`): levers ON = `10110010`.
    **Check:** the cipher is hard but fair. **This moves you to Act 6.**

## Act 6: Correction

21. Forum and status pages get rewritten (compare with what you saw in Act 1). TCAdmin opens:
    **`/tcadmin/legacy`**, `rookery` / `cl-mirror-71e4`. It shows tvale set debug_view=on at 03:58:30.
22. `panel_ok.jpg`'s EXIF thumbnail shows the "Start service" confirmation.
23. File **`06:44:10`** plus **`CHI-07/mrrw`** or **`mid-session`** to confront Theo about the restart.
    *(The forged 22.05.12 screenshot isn't made yet.)*
    **Check:** the rewrites are noticeable, and it's creepy that the history changed. **Moves you to Act 7.**

## Act 7: Voices

24. Braille shelves → **`sorry kit`**. Annex wall → **`HALTMRRW`**. Flags → `K` + `PORCHLIGHT`.
25. Iona's key chest at **(-238, 61, 232)**: key `mrrw-watchdog:off:6118:ib:2011-06-02`. Filing it
    gets "That's Iona's key" and **moves you to Act 8**.
    *(The Discord DMs need the bot: skip.)*

## Act 8: Continuity

26. lowpath walks every night (`/time set 13000`). Ask the desk where "turn it off" came from:
    Kit's email, 2019-03-02. File two of `@reboot`, `watchdog`, `crontab`, `auto_restart=on`.
27. Theo's box: `/~tvale/box/`, `tvale` / **`chi07-2010`** (the photo that gives this isn't made yet).
28. cairn's room: step on the lot plate, open and close the hinge tin, white dye on the returns
    chest, lamp lever off. Dust corridor (`-286 35 36`) → `SESSIONS:1:LOWPATH`.
29. The bed: put one inside lowpath's place at (-213, 73, 86). Before Act 9 Morrow tidies it away.
    Filing `kit` **moves you to Act 9**.

## Act 9: Someone Comes Home

30. `/ftp/` (client area login): compare the two zips. The clean backup MD5 is
    `bdd42105cf9ea6ee6f250c38ab48efba`. Residents' Key: keys `brackenhingetinbooster` then
    `sambrobaggagebiscuit`.
31. Case brief at **`/support/brief.php`**: restart `2011-05-18 06:44:10`, lowpath `2016-08-03`, Kit
    `2019-03-02`, the clean MD5 (or rook.txt's last line `dont let it finish anything`), your
    backdated join date. 4 of 5 needed. Theo accepting it **moves you to Act 10**.

## Act 10: Back By Tomorrow

32. Wax the MAINT sign at (-236, 71, 32) so it reads `MAINT ON`, and end lowpath's session (the bed).
33. Console on `/tcadmin/legacy`: command `HALTMRRW`, key = Iona's key, pick what to keep, then type
    the code in chat at the annex (-286, 35, 28). *(The USB vote needs Discord: ask Claude to stand in.)*
    **Check:** the lights-out shutdown lands, and the server says "no heartbeat".

---

## What moves each act

| To act | When |
| --- | --- |
| 2 | you (not an op) join the server |
| 3 | 45 min online since Act 2, then a dawn |
| 4 | desk: morrow / LogBlock finding |
| 5 | desk: Theo unmasked |
| 6 | desk: floor decoded / moss_0518 |
| 7 | desk: the 06:44 restart |
| 8 | desk: Iona's key |
| 9 | desk: Kit's line |
| 10 | desk: case brief accepted |

Filing something too early does nothing; file it again once the act catches up.
