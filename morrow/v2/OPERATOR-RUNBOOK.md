# Operator runbook (all ten acts)

You never speak live. Theo answers through the Archive Desk on his own, the plugin runs Morrow,
and the bot runs Discord. Your jobs are:

- watch the acts advance (they do it by themselves) and step in only if one stalls;
- read the journal;
- answer an out-of-fiction "help" if a friend asks.

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
| 4 The Archivist | The desk files Morrow's name / the LogBlock finding (desk ref `logblock_morrow`, `morrow` or `found_morrow`) | Server logs. Auction PDF drop. Desk answers the unmasking. |
| 5 Replay | Theo is unmasked (desk ref `unmask`) | Nightly incident replay, then a player's own ghost. patchcord's folder. Checklist drop. FTP opens. |
| 6 Correction | The floor is decoded (desk ref `floor` or `moss_0518`) | Forum and status rewrites. TCAdmin opens. Sign rewrites, rune signs, pots, the bleached flock. Theo's folder. |
| 7 Voices | They confront Theo about 06:44 (desk ref `restart`) | IRC log drop. Flags tidied. Per-player signs. Bot sends the Act 7 DMs over about 15 hours. |
| 8 Continuity | They file Iona's key (desk ref `key`) | lowpath walks every night. Sound replay, cairn's room, dust corridor. MAINT sign reverts unless waxed. Closet photo drop. Theo's box. Kit's line enters Morrow's vocabulary. |
| 9 Someone Comes Home | They reach Kit's line (desk ref `kit` or `ingest`), when Theo's reply goes out | Backdated forum profiles. Doors open for them. Recovery compass. Meadow redrawn. Fake Theo DM. Case brief. A placed bed now stays. |
| 10 Back By Tomorrow | Theo accepts the case brief (desk ref `brief`) | Booking, console, the USB group vote, hostile beats. Then the stop. |

A desk ref only moves the act from the act just before it (`unmask` moves 4 to 5, nothing else). If
one arrives early it is journalled as `act.advance_early` and does nothing; the same ref filed again
once the act has caught up applies it. Desk tickets with a Theo voice (T-04a, T-06a, T-08a, T-09a,
T-09b) also queue a `theo.reply` journal line at their reply time, which the bot posts to #general.

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
