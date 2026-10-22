# Solo playtest (no spoilers)

You're playing it cold, as a resident would. Don't open `SOLUTIONS.md`, the canon, or anything
under `morrow/` while you play. Keep a notepad open and write down the time whenever you get stuck,
guess wrong, or nearly quit. That list is the point of the playtest.

## What's running

| Thing | Where |
| --- | --- |
| The website | http://localhost:3041 (the **copperline-playtest** preview). Stands in for copperlinehosting.com. |
| The Minecraft server | `localhost:25599`, Java 1.21.11. Stands in for `mossfield.copperlinehosting.com`. |

Both run the real campaign: the act moves on by itself from what you do, there's no act switcher,
and hints arrive on the real clock. The one change from game night is that **Theo answers in about
3 minutes** instead of the next evening.

**Don't op yourself.** Operators don't count as residents, so the game wouldn't notice you.

## How it starts

Pretend a friend sent you this:

> found this weird old minecraft host site, apparently someone's still running a server on it
> from 2011?? http://localhost:3041
> "Retained customer records for case 6118 (Mossfield) will be kept until 31 Dec 2026."

Wherever the game says `copperlinehosting.com`, use `localhost:3041`. Wherever it says
`mossfield.copperlinehosting.com`, use `localhost:25599`.

## When you're done

Tell Claude where you stopped and share your notes. To run it again from scratch, Claude restores
`build/mrrw-v2-test-pre-playtest` over the test server and resets the act clock.
