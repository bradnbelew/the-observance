# Operator runbook (all ten acts)

You never speak live. Theo answers through the Archive Desk on his own, the plugin runs Morrow,
and the bot runs Discord. Your jobs are:

- advance the act at the right moment, on **both** sides;
- read the journal;
- answer an out-of-fiction "help" if a friend asks.

**Advancing an act, every time:**

1. In the Minecraft console: `/mrrw act N`.
2. On Vercel: set `MORROW_ACT=N` and `MORROW_ACT_STARTED=<now, ISO>`, then redeploy. The start time
   drives Theo's drops and the 24/72/120-hour hints.

The bot reads the act from the plugin by itself.

| Act | Advance when | What changes (automatic) |
| --- | --- | --- |
| 1 The Mirror | You send the trailhead (below) | Site: forum, client area, status. Discord: welcome pinned. Server list: fake players online. |
| 2 Mossfield | Someone joins the server | Dynmap. Morrow's notes on sign backs. Broken blocks come back at dawn. South rail creeps. |
| 3 The Tidy | After their first real session | LogBlock and June's folder open. Rules sign says "before". Tally book with lowpath. Golems. June's lamp flashes. Morrow starts answering chat. |
| 4 The Archivist | They've found Morrow's name in LogBlock | Server logs. Auction PDF drop. Desk answers the unmasking. |
| 5 Replay | Theo is unmasked (desk log: `MORROW_DESK ... ref=unmask`) | Nightly incident replay, then a player's own ghost. patchcord's folder. Checklist drop. FTP opens. |
| 6 Correction | The floor is decoded (they'll tell you, or they ask for moss_0518) | Forum and status rewrites. TCAdmin opens. Sign rewrites, rune signs, pots, the bleached flock. Theo's folder. |
| 7 Voices | They've confronted Theo about 06:44 | IRC log drop. Flags tidied. Per-player signs. Bot sends the Act 7 DMs over about 15 hours. |
| 8 Continuity | They hold Iona's key | lowpath walks every night. Sound replay, cairn's room, dust corridor. MAINT sign reverts unless waxed. Closet photo drop. Theo's box. Kit's line enters Morrow's vocabulary. |
| 9 Someone Comes Home | They understand the bed | Backdated forum profiles. Doors open for them. Recovery compass. Meadow redrawn. Fake Theo DM. Case brief. A placed bed now stays. |
| 10 Back By Tomorrow | Theo accepts the case brief | Booking, console, the USB choice, hostile beats. Then the stop. |

## The trailhead (Act 1)

Send this to the group as yourself, like something you found:

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
