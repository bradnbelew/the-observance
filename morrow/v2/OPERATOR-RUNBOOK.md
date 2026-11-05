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
journal records it as `act.advance` with reason `operator`). `/mrrw act N 2026-10-17T20:00:00-04:00`
also sets when the act began (use it at Act 1 to set the game night). `/mrrw status` shows the act,
when it began, and whether auto-advance is on. Set `auto_advance: false` to drive every act by hand.
Ops don't count as residents for the join and online-time triggers, so you can look around at Act 1
without starting the game (`auto_advance_count_ops: true` to change that).

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

Then say, as yourself: *"btw, there's something in Marked I didn't put there. find the key."*

Everyone's sectors together read: `copperline hosting / case 6118 / mossfield / records kept until /
31 dec 2026 / copperlinehosting.com`. The lines are in `plugins/mrrw-01/config.yml`.

**Set `MORROW_ACT_STARTED` to the game night's date** (the Archive Desk's hint timers count from it).

If you'd rather skip Marked, the old trailhead still works. Send this as yourself:

> found this weird old minecraft host site, apparently someone's still running a server on it
> from 2011?? https://copperlinehosting.com
> "Retained customer records for case 6118 (Mossfield) will be kept until 31 Dec 2026."

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

## Reading what happened

| Source | What it records |
| --- | --- |
| `plugins/mrrw/journal.jsonl` | Every change Morrow made, every chat reply with its sources, ready/stopped, and the USB choice |
| Vercel runtime logs | `MORROW_DESK` lines show every ticket and which rule it matched; `MORROW_CONSOLE` shows the finale |
| `discord-v2/state.json` | Which DMs the bot has sent |
