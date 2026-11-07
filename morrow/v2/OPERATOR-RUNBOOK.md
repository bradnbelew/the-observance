# Operator runbook (all ten acts)

You never speak live. Theo answers through the Archive Desk on his own, the plugin runs Morrow,
and the bot runs Discord. Your jobs are:

- watch the acts advance (they do it by themselves) and step in only if one stalls;
- read the journal;
- answer an out-of-fiction "help" if a friend asks.

**The server must keep ticking while empty:** `pause-when-empty-seconds=-1` in `server.properties`
(Paper's default pauses the world with nobody online: no dawns between sessions, no timed notes, and
the site's bridge calls time out, so it falls back to `MORROW_ACT`).

**Acts advance by themselves.** The plugin moves the act when the game or the Archive Desk logs
the trigger in the table below (config `auto_advance: true` in `plugins/mrrw/config.yml`). Each move
is a journal line `act.advance` with its reason, and it records when the act began. The website and
the bot follow the plugin's act over the signed bridge (`GET /state` on port 25588), so **no Vercel
redeploy per act**: `MORROW_ACT` / `MORROW_ACT_STARTED` are only a fallback for when the bridge is
unreachable. Desk triggers fire when Theo's reply to that ticket goes out (its `reply_at`), or at
once if it has no reply time.

**Stepping in:** `/mrrw act N` in the Minecraft console always works and always wins (the
journal records it as `act.advance` with reason `operator`). Every other fix is in the command table
below ("Operator commands"). `/mrrw act N 2026-10-17T20:00:00-04:00`
also sets when the act began (use it at Act 1 to set the game night). `/mrrw status` shows the act,
when it began, and whether auto-advance is on. Set `auto_advance: false` to drive every act by hand.
Ops don't count as residents anywhere (join and online-time triggers, roster, chat corpus, stop
reason, first-join figures, quorum), so you can look around at Act 1 without starting the game
(`auto_advance_count_ops: true`, or `/mrrw residents add NAME`, to change that).

| Act | Advances to this act automatically when | What changes (automatic) |
| --- | --- | --- |
| 1 The Mirror | (you start here; send the trailhead, below) | Site: forum, client area, status. Discord: welcome pinned. Server list: fake players online. |
| 2 Mossfield | The first resident (not op) joins the server | Dynmap. Morrow's notes on sign backs. Broken blocks come back at dawn. South rail creeps. |
| 3 The Tidy | A resident has been online 45 minutes in total since Act 2, and a dawn has passed since (in-game sunrise or `/mrrw dawn`) | LogBlock and June's folder open. Rules sign says "before". Tally book with lowpath. Golems. June's lamp flashes. Morrow starts answering chat. |
| 4 The Archivist | June's folder is opened with her password (the site posts `june_folder` when `sambro` logs in) | Server logs. Auction PDF drop. Desk answers the unmasking. The creeper art at spawn was completed at the Act 3 dawn. |
| 5 Replay | Theo is unmasked (desk ref `unmask`, `unmask_lot`, `unmask_date` or `unmask_waived`) | Nightly incident replay (cue line, starts when a resident is near), then a player's own ghost. patchcord's folder. Checklist drop. FTP opens. The first dawn with a resident online tidies three rows of Rook's floor ("54 blocks normalised (rook's)"). |
| 6 Correction | The players edit the MAINT sign at spawn to `MAINT ON` (Morrow answers within a minute) and Morrow reverts it at the next dawn (journal `morrow.maint_reverted`) | Forum and status rewrites. TCAdmin opens (its console takes a key). Sign rewrites once per act, rune signs, pots, the bleached flock. Theo's folder. T-06c the night after the confession. |
| 7 Voices | They confront Theo about 06:44 (desk ref `restart` or `restart_node`) | IRC log drop. Flags tidied. Per-player signs. Bot sends the Act 7 DMs over about 15 hours; T-07d after two days. Iona's chest, once opened, is merged at the next dawn. |
| 8 Continuity | Iona's key is entered in the TCAdmin console (the site posts `console_key`; the desk refuses it) | lowpath walks every night (cue line). Sound replay, cairn's room (fixtures inside the barn), dust corridor. MAINT sign reverts unless waxed. Closet photo drop. Theo's box (with the heartbeat logs). Kit's line enters Morrow's vocabulary. A bed at lowpath's place is queued at once and removed at dawn. |
| 9 Someone Comes Home | They reach Kit's line (desk ref `kit` or `ingest`), when Theo's reply goes out | Backdated forum profiles (Location in Iona's region notation, post ids above 30112, LogBlock rows). Doors open for them. Recovery compass. Meadow redrawn. Fake Theo DM. Case brief. A placed bed is kept ("bed kept"); that night lowpath walks its last leg in and lies down. T-09c the next morning. |
| 10 Back By Tomorrow | Theo accepts the case brief (desk ref `brief`) | Booking (then Theo's ask about finch's sign: whatever a resident says in chat is the stop log's reason, else finch's own sign), console, the USB group vote, hostile beats. Then the stop: Morrow echoes the code and Kit's line, the residents' figures walk home one session at a time, the log, lights out. T-10-after a week later. |

A desk ref only moves the act from the act just before it (`unmask` moves 4 to 5, nothing else). If
one arrives early it is journalled as `act.advance_early` and does nothing; the same ref filed again
once the act has caught up applies it. Desk tickets with a Theo voice (T-04a/c/d/e, T-06a/d, T-08a,
T-09a, T-09b) also queue a `theo.reply` journal line at their reply time, which the bot posts to
#general. Theo's timed notes (T-06b, T-06c, T-07d, T-09c, hours into their act; T-10-ask after the
booking; T-10-after a week after the stop) are fed the same way by the plugin and shown on the desk.
Filing `found_morrow`, `floor`, `moss_0518` or the key at the desk gets Theo's reply but moves nothing.

## The trailhead (Act 0, at a Marked game night)

The game starts inside Marked. `mrrw-01` (`plugin-trailhead/`) sits next to Marked on that
server and does nothing until you arm it. It never touches inventories, the scoreboard, the
action bar, titles, boss bars or the world. Everything below is a command you choose to run,
from the console or as op.

| When | Command | What the players see |
| --- | --- | --- |
| Before they join, or any time | `/trailhead start` | Hovering over the server in the server list shows `cairn`, `juni_lantern` and `lowpath` online, and the count is 3 too high. Nothing in-game. |
| Mid-night, between rounds | `/trailhead knock` (or `knock cairn`) | "lowpath joined the game", then "left the game" 4 seconds later. |
| At a Huddle or the Tribunal | `/trailhead preview`, then `/trailhead speak` | Preview shows only you a `[Recovery]` line (run it again to reroll). Speak posts it to everyone. It's made only of 2011 Mossfield words, picked to echo what the table just said. |
| Last thing, everyone in the lobby | `/trailhead key all` | Each player gets a book on screen: "recovered sector 2 of 6". It shows only every 6th letter of the key, and the full text appears only when all the sectors are laid on top of each other. A copy also goes to their chat. |
| Someone missed it or joined late | `/trailhead key NAME` | Resends their sector, or gives a late player a duplicate one. |
| After the night | `/trailhead stop` | The server list goes back to normal. |

Then say, as yourself: *"huh. did anyone else get a weird book just now? that's not one of mine."* Then drop it: don't say "key", "puzzle" or "game"; let them be the ones who decide it's something.

Everyone's sectors together read: `copperline hosting / case 6118 / mossfield / records kept until /
31 dec 2026 / copperlinehosting.com`. The lines are in `plugins/mrrw-01/config.yml`.

**Set `MORROW_ACT_STARTED` to the game night's date** (Theo's timed desk drops count from it).

**Hints are yours to release.** None appear on the Archive Desk by themselves. Open
`https://copperlinehosting.com/backstage` (any user name, password = `MORROW_STOP_SECRET`): it shows
the plugin's act, who's playing, and this act's hints in order with a Release button on each. A
release shows that hint and every one before it; "Take them all back down" hides them. Each act keeps its
own released count and release times (GET /state `hints_released`, `hints_released_at`), so a note
released in one act is still there after the act moves. If the bridge is down the page says so: use `/mrrw hint N` in the
server console instead. The page is unlinked and not in robots.txt; don't share the URL.

If you'd rather skip Marked, the old trailhead still works. Send this as yourself:

> found this weird old minecraft host site, apparently someone's still running a server on it
> from 2011?? https://copperlinehosting.com
> "Retained customer records for case 6118 (Mossfield) will be kept until 31 Dec 2026."

## Control room (`/backstage`)

`https://copperlinehosting.com/backstage`, any user name, password = `MORROW_STOP_SECRET`. To a player it
looks like any password-protected folder (Apache 401). One page, server-rendered, reload it to refresh:

- **Campaign:** the plugin's act and when it began, auto-advance (with on/off buttons), the finale
  (ready / stopped and when).
- **Site act floor:** `MORROW_ACT` against the plugin's act. Green = the site follows the plugin. Red
  when the bridge isn't answering (the site is on `MORROW_ACT`, or on the last state that instance
  heard) or when `MORROW_ACT` is ahead of the plugin (lower it on Vercel or move the plugin). A
  stop-secret fingerprint mismatch shows here too.
- **Residents and the USB vote:** the roster with first-join spots, add/remove a resident, the vote count
  and winner.
- **Commands:** a button or small form for every `/mrrw` command (`status`, `flags`, `desk`, `residents`,
  `dawn`, `restore`, `replay`, `snapshot`, `figures respawn|clear`, `act`, `flag`, `handin`,
  `rollback`, `repair`, `hint`, `journal`, `say`), plus a free box that runs anything you'd type after
  `/mrrw`. They go to the plugin as a signed bridge event and the plugin's answer is shown at the top.
  `act`, `flag`, `handin`, `rollback` and `residents remove` ask "are you sure" first (the server
  refuses them unconfirmed).
- **Desk hand-ins:** the plugin's `desk` table (what the site has posted, released, applied). If one
  never arrived (bridge down when it was filed), **Re-post hand-in** sends the site's own signed event
  again, e.g. `june_folder`, `console_key`, `unmask`, `brief`. **Apply hand-in** (`handin <kind>`) makes
  the plugin apply it directly instead.
- **Hints:** release this act's hints one by one, or take them back down (as before).
- **Journal:** the last 50 entries, newest first.

If the bridge is down the page still opens and says so; use the server console. The same commands
from a terminal: `python plugin-v2/tools/bridge_client.py --url http://HOST:25588 --secret ... op act 5`
and `... journal 50`.

**Rotating `MORROW_SESSION_SECRET`:** put the old value in `MORROW_SESSION_SECRET_PREVIOUS` at the same
time, or every ticket link already posted in Discord, every "Your requests" list and every login stops
working. Testing switches (`MORROW_DEV`, `MORROW_DESK_FAST`) are ignored on the production deployment.

## The finale (Act 10), what happens

1. Players wax a sign at spawn reading `MAINT ON`, and give lowpath a bed. The server then reports
   ready over the server list.
2. On the panel they enter `HALTMRRW`, Iona's key, and their USB choice. The console checks the
   server, and gives a code like `C-1A2B-3C4D`.
3. Someone types the code in chat at the maintenance annex. The stop log plays, south_rail is
   skipped, and Morrow goes silent everywhere: chat, server list, status page, Discord.
4. **If a player's chat fails:** run `/mrrw stop <code>` in the console. It applies the same checks.

**Epilogue (a week later):** reopen the server with what they kept. Clean means the April 2011
backup world; recovered means the current world; corpus means no world, and you post the
corpus text.

## Operator commands (`/mrrw`, console or op)

Non-ops never see any of this: `/mrrw` is hidden from their tab-complete and answers "Unknown or
incomplete command"; `/plugins` and `/version` are op-only while the plugin runs. The site's operator
page runs the same text over the bridge (`POST /event {"kind":"op","cmd":"desk"}` ->
`{"ok":true|false,"output":"..."}`; `GET /journal?n=50`). Every command that changes something writes
an `operator.*` journal line; the read-only ones (`status`, `desk`, `flags`, `flag NAME`, `residents`,
`journal`, `snapshots`, `help`) write nothing.

| Command | What it does |
| --- | --- |
| `help` | Lists every command |
| `status` | Act, when it began, readiness, lowpath, auto-advance, residents and quorum, USB tally, secret fingerprint, and a red WARNING if `pause-when-empty-seconds` isn't -1 |
| `act N [ISO]` | Sets the act (always wins). The same act again re-applies it and keeps its clock. A desk ref the players already filed for the next act applies at once (the reply says so) |
| `auto on\|off` | Auto-advance, saved to config.yml. `on` applies any move held while it was off |
| `desk` | Every desk ref: received, reply time, Theo voice, released or WAITING, applied or NOT APPLIED, which act it moves |
| `handin KIND` | A hand-in as if the players did it. Desk refs (`june_folder`, `unmask*`, `restart*`, `console_key`, `kit`, `ingest`, `brief`, `booking`): filed with no reply delay, or released now if waiting (Kit's 36 h), or retried. `maint`: Act 5 = MAINT ON set and reverted (moves to 6); Act 10 = the sign set to MAINT ON and waxed. `lowpath`: lowpath's session ended (bed kept). `cairn_room`: cairn's room opens |
| `hint N` | Shows this act's first N hints on the desk (kept per act: they stay up when the act moves) |
| `flags` / `flag NAME [on\|off]` | The one-shot flags on disk. Off = that one-shot runs again at the next act change or `act N`. Warns on a name the plugin doesn't know. `flag stopped off` also clears `stop_pending` |
| `journal [n]` | The last n journal lines (default 10, max 200) |
| `residents` / `residents add\|remove NAME` / `residents quorum N` | Who counts as a resident (ops don't unless added). Remove = out of the roster and the quorum for good. Quorum 0 = residents seen in the last 14 days (at least 1) |
| `snapshot [NAME]` / `snapshots` / `rollback NAME` | Copies the state files (state, desk, roster, recovery, paths, votes, stop flag) to `plugins/mrrw/snapshots/NAME`. Every act move snapshots itself first (`before-actN-<time>`). Rollback restarts the plugin's services on the snapshot and re-applies the act; it snapshots `before-rollback-<time>` first so it can be undone; the journal and Discord feed are never rewound |
| `repair [PART\|all]` | Re-places install parts without touching progress. `all` = the safe set (museum, reading room shelves, map copies, tribute banners, dust corridor, finch's lock, nether sign, containers, south-rail sign, shed hatch, gallery wire, Theo's drafts, kit chest, visitor book). By name only: `theo_archive`, `flags` (un-tidies Act 8's order), `meadow`, `lowpath_place` (a kept bed is lost), `listening_room` (reopened if it was open), `maint_sign` (back to MAINT OFF), `dock_removed`, `barn_squared`, `iona_chest` (the unmerged key again). `map_wall` is refused (new map ids would break C11) |
| `restore [signs]` | The dawn's build restore now. `signs` also puts every build sign a player edited back to its text from before their first edit |
| `dawn` | Every dawn job now (restore, tidies, refills, the Act 2 -> 3 check) |
| `tp PLACE [PLAYER]` | To a canon anchor (`spawn`, `rookery_cellar_floor`, `iona_key_chest`, `maintenance_annex`, ...; `nether_hub` is in the Nether). From the console name a player |
| `figures clear\|respawn` | Removes every figure / starts the incident replay (Act 5+) and lowpath (Act 8+) now |
| `say TEXT` | Speaks as Morrow (`[Recovery]` in chat, relayed to Discord). Morrow only ever says 2011 words: quote the corpus |
| `replay [sound]`, `install`, `reload`, `stop CODE` | As before |

## When something goes wrong

| It happened | Do this |
| --- | --- |
| The act moved too early or wrongly | `snapshots`, then `rollback before-actN-...` (the ref that moved it is marked used; `handin REF` redoes it) |
| The group did the hand-in but nothing moved | `desk`: a ref `NOT APPLIED` with "moves 7 -> 8" while at 6 waits for its act and applies by itself; at the right act, `handin REF` |
| auto-advance was off for a while | `auto on` applies what was held |
| A puzzle object got broken, emptied or lost (books, maps, compasses, banners, shelves) | `repair all`, or `repair PART`; a broken build block comes back at dawn, or `restore` now |
| Someone overwrote a clue sign | `restore signs` |
| Iona's chest got merged before they read it | `repair iona_chest` |
| lowpath never lies down (no one online at night, bed trouble) | `handin lowpath` |
| cairn's room won't open | `handin cairn_room` |
| A player quit for good and the USB vote can't finish | `residents remove NAME` or `residents quorum N` |
| The operator's own account got counted | `residents remove YOURNAME` (do it before Act 9) |
| Figures stuck or doubled | `figures clear` (they come back at the next dusk/night) or `figures respawn` |
| The server was restarted mid-finale | Nothing: the stop stands, its record and log are written on start-up |
| `status` shows the pause-when-empty WARNING | Set `pause-when-empty-seconds=-1` in server.properties and restart: until then nothing happens between sessions |
| bridge-state.json is unreadable | It is kept as `bridge-state.json.corrupt-<time>`; `rollback` the last `before-act...` snapshot |

## When a group is stuck: which hint to release

Hints only appear when you release them (`/backstage`, or `/mrrw hint N`, which shows the act's first N).
Each is Theo noticing something in his own files; none gives an answer. Release in order; a day of
no progress on a gate is a fair moment. `/mrrw desk` shows what they've filed (a near-miss ticket means
they're close: wait).

| Gate | What they have to do | Release |
| --- | --- | --- |
| Before 1→2 | find the forum (robots.txt, page source), answer `bracken` | 1.2 (crawlers), then 1.3 (Rowan's first post) |
| 3→4 | open June's folder (`sambro`) | 3.2 (her light), 3.4 (one word for everything), 3.1 (time zones) |
| 4→5 | tell Theo they know it's him | 4.1 (say it here), 4.4 (lot numbers) |
| 5→6 | solve Rook's floor, set MAINT ON | 5.3 (rules read aloud = the key), 5.2 (wool numbers), 5.4 (hashes); 5.1 for patch's folder |
| 6→7 | ask Theo about the 06:44 restart | 6.1 (ask me about the log). The only Act 6 gate hint: release it early |
| 7→8 | Iona's key into the panel console | 7.2 (her paths), 7.3 (chest counts), 7.4 (the console box) |
| 8→9 | ask Theo where "turn it off" came from | 8.1. The plugin also has Morrow say Kit's line at the first two Act 8 dusks |
| 9→10 | the case brief (needs rook.txt's line) | 9.1 (the form), 9.2, 9.3 (hashed backups) |
| 10 | the stop: sign waxed ON, lowpath asleep, key + code at the annex | 10.2 (three things), 10.3 (waxed signs), 10.1 (in the room) |

## What Morrow protects, and what comes back

Inside the builds (each canon build anchor within 14 blocks, now also Iona's notes, finch's junction
levers, cairn's sealed room, Kit's hut, the south rail's end, the top of June's tower and Rook's
Nether hub): broken blocks and explosions come back at dawn (also over water); pistons can't move build
blocks; water and lava can't wash them away; fire doesn't burn them; signs, chests and other block
entities can't be broken by non-ops; lectern books stay on their lecterns; Iona's chest is view-only
and is merged no sooner than an hour after it was first opened; the Braille shelves can't be changed;
June's lamp is put back if broken; the genuine 2015 map, the wall-map copies and the kit chest are
refilled at dawn. Players may still edit and wax signs (Act 6 asks them to). A MAINT ON written before
Act 5 is reverted at dawn ("1 sign normalised (spawn)") and moves nothing; only one written in Act 5
is the hand-in. If the players sleep through an Act 8-9 night, lowpath walks in the day instead.

## Reading what happened

| Source | What it records |
| --- | --- |
| `plugins/mrrw/journal.jsonl` | Every change Morrow made, every chat reply with its sources, ready/stopped, and the USB choice |
| Vercel runtime logs | `MORROW_DESK` lines show every ticket and which rule it matched; `MORROW_CONSOLE` shows the finale |
| `discord-v2/state.json` | Which DMs the bot has sent |
