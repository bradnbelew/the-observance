# Operator runbook (Acts 1-3)

You are the puppetmaster, but you never speak live. Your jobs are to flip the act at the right
moment, keep the world and site in step, and read the journal.

**Keep the act in step on both sides.** The website reads `MORROW_ACT` (a Vercel environment
variable). The server reads `/mrrw act N`. Set both every time the act changes. On Vercel,
changing an environment variable needs a redeploy to take effect.

## Before Act 1

- [ ] The Beta world is built, injected, upgraded, and loaded on Paper with `mrrw` installed.
- [ ] June's screenshots are in `dashboard/public/media/june/`.
- [ ] Vercel production has `MORROW_ACT=1` and a random `MORROW_SESSION_SECRET`.
- [ ] Discord: create a server named **Mossfield Recovery (archive)** with one channel, `#general`.
      Make a second account called `archivist` (Theo) and pin the welcome note:
      *"This is a read-only mirror of the old Copperline site. Records for case 6118 (Mossfield) are
      kept until 31 Dec 2026. If you were a Mossfield player, the archive desk is open. No worries
      at all."* The Discord bot arrives in Act 4. For Acts 1-3, Discord is just atmosphere.
- [ ] In game: `/mrrw act 1`. The server MOTD shows the queue band, and the hover list shows
      cairn, juni_lantern and lowpath.

## Act 1: The Mirror

**Trailhead.** Send this to the group as yourself, like something you stumbled on. Don't
explain it.

> found this weird old minecraft host site, apparently someone's still running a server on it
> from 2011?? https://copperlinehosting.com
> "Retained customer records for case 6118 (Mossfield) will be kept until 31 Dec 2026."

**What players do:** robots.txt, then the importer comment, then the forum, the roster, the
security question (`bracken`), and Rookery's client area with the live server address.

**Advance to Act 2** when someone has the server address and joins Mossfield.

## Act 2: Mossfield

`/mrrw act 2`, and set `MORROW_ACT=2`. Dynmap opens. Morrow's notes appear on the backs of the
residents' signs. Blocks players break in resident builds come back at dawn.

**Advance to Act 3** after their first real play session. Morrow should "tidy" what they built
the night they log off.

## Act 3: The Tidy

`/mrrw act 3`, and set `MORROW_ACT=3`. The LogBlock viewer and June's folder (password `sambro`)
open. The rules sign changes to "before". The tally book with lowpath appears, golems arrive,
and June's lamp starts flashing every 5 seconds.

**The Act 3 hook** is Morrow answering chat for the first time, which happens on its own.

**Stop here for the playtest.** Note what the friend did, where they got stuck, and how long
each step took.

## Reading the journal

`plugins/mrrw/journal.jsonl` records every change Morrow makes, plus every chat reply with its
sources and `obeys_corpus_law: true`. If a player ever claims Morrow "said something new", this
file proves where each fragment came from.
