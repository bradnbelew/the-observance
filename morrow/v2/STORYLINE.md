# MORROW — Full Storyline (v1)

Date: 2026-10-09. Status: **approved 2026-10-12 and built.** The authority is `morrow/v2/README.md`:
where this document and `morrow/canon/` disagree on an exact value (a time, a line, a filename), the
canon wins. Two things changed after approval and are marked below: Theo is **prerecorded** (see
`THEO-AND-CIPHER-BANK.md` Part 1), so every *You, live* section is superseded; and the 2011 record was
tightened (who restarted Morrow and when, the recording's clock, the corpus law).

This replaces the current G01–G15 spine. It keeps what was good: Copperline, Mossfield, Morrow, the residents' voices, incident 6118, the uptime 04:17:22, and Iona's note about the south rail. It rebuilds the story so that the lore *is* the puzzle.

---

## 0. How to read this

1. **Part A** covers what an ARG is and how this one is played. It's short.
2. **Part B** is the hidden truth: the real history the players dig up, and the cast.
3. **Part C** covers the investigation toolkit: the skills players learn and reuse, and the ciphers.
4. **Part D** is the ten acts, start to finish, with every investigation solved step by step.
5. **Part E** covers the Minecraft build method, the media you'd make, your live role, pacing, and hints.
6. **Part F** lists the decisions I need from you before building.

All in-fiction text samples are written in the voice I intend to use. If a sample reads like a school play, tell me, because the whole thing will read that way.

---

# PART A — What this is, and how it's played

## What makes an ARG an ARG

These are the standard structural pieces from ARG design writing:

- **The rabbit hole, or trailhead.** This is the first door into the fiction, usually a link, a page, or a message.
- **The puppetmaster.** This is the person running the story live and reacting to players. That's you.
- **The curtain.** The puppetmaster never speaks as themselves. You only speak through characters and through changes to the world.
- **Collective intelligence.** Puzzles are sized so that a group dividing the work beats one person grinding.
- **Real time.** The story happens on a calendar. Things change between sessions, and messages arrive at believable hours.
- **Trust.** Each fair, satisfying solve earns trust, and that trust lets you get away with ambiguity later.

The big Minecraft ARGs are mostly found-footage video series, where the audience watches and theorises. This one is the rarer kind. The players are *inside* the found material: they log into the server, read the forum, and get the messages.

## What this one honestly is

This is a **consensual ARG for one to six friends**. They know you made it, so "this is not a game" isn't possible. Instead we sell the door, run it on a calendar, and Theo answers from prerecorded canon (this replaced "you play one character live"). It is not a puzzle-room game. There is no puzzle that exists only to be a puzzle. Every solve comes from reading records, cross-checking people, doing time math, and noticing what's been changed.

## How it's played

- **Ten acts over roughly six to eight weeks.** The target is about 25 to 35 hours of group time.
- **Each act opens with a drop:** a site change, a Discord message, a world change, or a file.
- **Players work across the Copperline website, the Mossfield server, a Discord server, and media files.**
- **Players must keep notes.** Later acts ask about details from earlier acts without showing them again, and Morrow edits records as the game goes on. Every required fact still has a backup source, so nobody is locked out forever, but the group that keeps notes moves much faster.
- **Rookery tells them to take notes early on.** One of the first signs they read says "screenshot stuff / it changes".

---

# PART B — The hidden truth

## B1. The one-paragraph truth

In April 2011 a small Chicago host, Copperline, lost a storage array. A survival server called Mossfield lost most of its world. Iona Bell, Copperline's sysadmin, wrote a tool to rebuild it. The tool replayed the server's block log (LogBlock) onto a partial backup, then guessed where data was missing. Theo Vale, the support lead, gave the tool a chat responder, a simple word-chain bot built from support replies and server chat. He also gave it a name taken from his own support macro: *"your world will be back by tomorrow."* The process was called `morrow`.

On the night of 18 May 2011, morrow ran past its maintenance window. It started rebuilding things nobody had built and replaying residents' old sessions as walking figures. Iona killed it at 06:17. Theo restarted it at 06:44 because it was "mid-session." Iona stopped it again that morning and turned the panel's auto-restart off, but a watchdog in root's crontab, which Theo swore wasn't his, brought it back when the node rebooted in September. The residents left. When Copperline died in 2014, Theo bought the server at the liquidation. He kept morrow running in his apartment for twelve years, upgrading the world every Minecraft version and feeding it more. Nobody came back.

In 2026 Theo reopened the old Copperline site as an archive mirror, hoping a resident would come home. Strangers came instead: the players. Morrow starts treating them as the residents it has been waiting for.

## B2. Why this is scary without being supernatural

- **Morrow is real 2011 technology.** It's a log replayer plus a guessing heuristic plus a word-chain chat bot. Morrow can only say words that someone once said. That's a hard rule players can test, and it becomes their main tool for spotting fakes.
- **The horror is tidiness.** Morrow restores things toward a neat version of memory. The residents were messy, and their mess *was* their identity. Cairn's 2011 post is the thesis of the game: "dont sort my chests. the uneven ones are right."
- **The human is the real problem.** Theo is grieving, kind, and wrong. Morrow does exactly what Theo asked it to do.
- **The players' own behaviour becomes material.** By Act 9 the record says *they* lived here in 2011.

## B3. Timeline of the real history

Players reconstruct this. Every line has evidence somewhere.

| Date (Chicago time) | Event | Where the evidence lives |
| --- | --- | --- |
| 2010-10-30 | Rookery founds Mossfield on a Copperline "Builder" plan, on the day of the Halloween Update. | Forum welcome post; billing record |
| 2011-01-13 | Beta 1.2 adds coloured wool and note blocks. Rookery posts the wool ID list. | Forum post |
| 2011-04-19 | Beta 1.5 adds powered rails. finchline rebuilds her rail line that week. | Forum; LogBlock density |
| 2011-04-21 | The Chicago storage array degrades. The rolling backup overwrites the good one. | Status page; ticket 6118 |
| 2011-04-25 | Iona starts `mrestore`. Theo renames the process `morrow` from his macro. | Staff IRC log |
| 2011-05-06 | Theo adds the `!status` chat responder. | IRC; server.log |
| 2011-05-18 02:00–04:00 | The scheduled maintenance window. | Status page |
| 2011-05-18 03:58 | Theo switches the debug view (the ghosts) on to show Kit, who's staying at his place. He tells Iona "not me". | TCAdmin panel log; IRC |
| 2011-05-18 04:00–06:17 | morrow keeps running unsupervised. June's tower gets a glass roof (04:11–04:15) she never built. Ghost replays walk around. | server.log; LogBlock; June's screenshots; patchcord's recording |
| 2011-05-18 06:17 | Iona kills morrow at **uptime 04:17:22**. | IRC; raw status export; the recording at 19:22 |
| 2011-05-18 06:44 | Theo restarts it from the TCAdmin panel: "mid-session, it'll corrupt." Rook hears him say so on Mumble. | TCAdmin panel log; MIC-5-18-B.WAV |
| 2011-05-18 11:12–11:14 | Iona turns auto-restart off and stops it again. Theo's 05-20 "It's off now" is true. | TCAdmin panel log; IRC |
| 2011-05 to 06 | Rookery and Iona build a manual stop: a wool floor, a flag sign, and Iona's key. | Floor; IRC; Iona's chest |
| 2011-06-10 | Rook finds an April 20 copy he'd pulled over FTP for Overviewer. Clean. It goes in his FTP, outside the recovery folder, and nobody tells Theo. | IRC; checklist MD5 |
| 2011-08-12 | Iona leaves Copperline. Her handover: an @reboot watchdog in root's crontab "that I didn't add. Theo says he didn't either." | Ticket 6290 |
| 2011-09-02 | Dale reboots the node. The watchdog brings morrow back; it squares cairn's storehouse to the grid. | server.log; LogBlock |
| 2011-09-04 | Rook: "its back on again. iona's gone and its back on. im done with it." | Forum |
| 2011-11-18 | Last human login: cairn, at 05:12 after his shift, the day 1.0 comes out. He stays 6 minutes. | server.log; forum |
| 2012–2014 | Theo waives Mossfield's fees month after month. | Billing FAQ / invoices |
| 2014-12-01 | Copperline closes. | Site banner |
| 2014-12-09 | Liquidation. Lot 14, the Mossfield node, sells to "T. Vale". So does Lot 17, the web box (site, billing, forum). He later buys the lapsed domain. | Auction PDF |
| 2014-12-14 | First heartbeat from morrow on the new host. Theo switches auto-restart **on**. | Status JSON; TCAdmin panel log |
| 2016-08 | Theo adds session continuation and names a placeholder owner for unclaimed builds, `lowpath`, after "low path" in Rook's map key. Morrow makes it walk, and the forum import gives it a profile. | Theo's changelog; forum import; LogBlock |
| 2018-02 | Theo lets the chat helper answer with clips from the support voicemail box. | Theo's changelog |
| 2019-03-02 | Theo's brother Kit emails him: "you can turn it off, its ok." A week later Theo feeds it the whole inbox. | Kit's email; Theo's changelog |
| 2021–2025 | World upgrades: copper (1.17), then cherry and chiseled shelves (1.20, 2023). Theo waxes the signs he wants kept (1.20) and makes Morrow skip waxed blocks. He adds copper golems "to help cairn's storehouse." | Anachronisms in world; archivist notes |
| 2026-08 | Theo reopens copperlinehosting.com as a mirror, plus a Discord and a chat bridge. | Site; Discord |
| 2026 (the game) | The players arrive. | Everything |

## B4. Cast

Short descriptions, and how each one talks. Voice is everything.

**Copperline staff**

- **Iona Bell** (`ibell` on IRC, `i_bell` on the forum). Sysadmin, about 30. Precise, dry, slightly funny under stress. She always writes **CDT** in summer and **CST** in winter, and she gets daylight saving right. That habit becomes a forgery test.
  > `04:03 < ibell> it's not "replaying", it's making stuff up. it's queued a roof for june's tower`
- **Theo Vale** (`tvale`, staff signature "Cheers, T"). Support lead and community guy, mid-20s in 2011. Warm, eager, conflict-avoidant. His tics: two spaces after a full stop, "No worries at all!", and "cheers". His macro: *"No worries at all! Your world will be back by tomorrow."* In 2026 he is about 40, lonely, and still kind. *(Superseded: he is prerecorded, not played live.)*
  > `04:05 < tvale> give it till morning`

**Mossfield residents (2011)**

- **Rookery / Rowan Keel.** Admin, about 19. Blunt, jokey, paranoid in the right way. Writes signs in lowercase with "pls" and "-rook".
- **finchline / Mina Finch.** Rails and redstone. Starts more projects than she finishes. The unfinished south rail is hers.
- **juni_lantern / June Harrow.** Lighthouse builder and compulsive screenshot taker. Lives in Halifax, so she's **two hours ahead** of the server. Her screenshot filenames are in her local time.
- **patchcord / Eli Mercer.** Runs the Mumble server and records everything to his desktop. Lives in Manchester, UK, six hours ahead.
- **cairn / Sam Calder.** Farms and the storehouse. Bakes nights ("i'm off at 4"), so he plays at dawn. Keeps uneven chests on purpose, because they're his crate tally.
- **kitbag / Kit Vale.** Theo's younger brother, 14 in 2011. He's why Theo cared. He grew up, stopped playing, and is fine. He's the person Theo couldn't let go of.

**The fabricated one**

- **lowpath.** A "resident" with no 2011 existence. In 2016 Theo typed a placeholder owner for the builds nobody claimed and called it after "low path", a note in Rookery's hand-written map key (`mapkey.txt`, March 2011). Morrow made it a person: it walks, it has a profile, and its skin is stitched from cairn's body and June's head.

**Morrow itself**

Morrow's chat is word-chain output: three to six word fragments of real lines, glued together. It's realistic and unsettling, and every fragment can be traced back to who said it.
> `[Recovery] back by tomorrow. cairn is at the farm. no worries at all`
> (sources: Theo's macro; cairn, 2011-05-12 chat; Theo's macro)

## B4a. The corpus law (canon rule)

What Morrow says or writes in its own voice is a collage. Every chat line, DM, rewritten sign,
rewritten forum or status post, backdated post and per-player sign is made of fragments somebody
really said or wrote: forum posts, server chat, private messages, the genuine 2011 signs (it reads
signs), Copperline's billing notes ("keep billable till someone comes home"), and the support
voicemail box. So **an unsourced word means a human wrote it**, which is how players catch Theo.

Two things are not chat. Morrow's timestamp formatter always prints **CST** (it never learned
daylight saving), which is the tell on every date it writes. And the restore engine prints fixed
status templates Iona coded in 2011: `restored`, `normalised`, `session lowpath: end (slept)`,
`queue 412/413`, `absent since 2011`. Normalising "b4" to "before" on the rules sign is one of those
templates. `python -m morrow.canon.verify` checks every Morrow string against this law.

## B5. The ending in one paragraph

The players end morrow's last open session by giving lowpath a home. They place a bed where its nightly route ends, and it finally "logs off". Then they run the manual stop that Rookery and Iona designed in 2011, plus the one piece only Theo can do. Before the stop, Theo says he has one USB stick, and they choose what goes on it. When morrow stops, its final log line says it skipped one job: finishing the south rail, with "reason=none". Nobody can say whether that's a bug, something it learned from the players, or a goodbye. Then it's silent.

---

# PART C — Investigation toolkit

The design goal is that players learn **skills**, not one-off tricks. Each skill is taught gently once, then used harder later.

## C1. Reusable investigative skills

| # | Skill | Taught | Used hard |
| --- | --- | --- | --- |
| S1 | **Dating by block palette.** A block can't predate the version that added it, so the palette shows what's genuine 2011 and what's reconstruction. Uses public Minecraft Wiki history. | Act 2 | Acts 6, 9 |
| S2 | **Time math.** Chicago CDT/CST versus each resident's local time, and US daylight-saving dates in 2011. | Act 3 | Acts 6, 9 |
| S3 | **LogBlock queries.** The site's block-log viewer: who placed what, where, and when. | Act 3 | Acts 5, 7, 9 |
| S4 | **The corpus test.** Morrow can only reuse words someone already said. Trace a line to its sources, or find a word with no source, which means a human wrote it. | Act 4 | Acts 7, 8 |
| S5 | **Authorship tells.** Iona's daylight-saving habit, Theo's double spaces, Rookery's "-rook", and the 15-character sign limit in 2011. | Act 4 | Acts 6, 9 |
| S6 | **Record authentication.** Book generation ("Copy of a Copy" means Morrow), signs longer than 15 characters per line (impossible in 2011), wrong time zones, and cached versus live pages. | Act 6 | Acts 7, 9 |
| S7 | **Waxing.** Morrow treats waxed signs and copper as "archived" and never alters them. Players learn to protect evidence. | Act 6 | Act 10 |

## C2. The ciphers

None of these are decorative. Each was made by a specific character for a specific reason, using tools that existed at the time.

1. **Rookery's floor (the hard one, Act 5).** Four layers:
   1. Reconstruct the original wool floor from LogBlock, because Morrow tidied part of it.
   2. Convert wool colours to hex using the 2011 wool data values.
   3. Read the floor boustrophedon, back and forth like Rookery's rail line.
   4. Decrypt with a running-key Vigenère whose key is the **original** spawn rules sign. Morrow edited one word of that sign in Act 3, so players need the original from their own screenshots, June's photo, or the Mumble audio. If they use the current sign, the plaintext breaks exactly at the edit, which tells them what happened.
2. **Iona's chest (Act 7).** Stack counts of 1 to 64 in a double chest are base64 indexes. Iona was a sysadmin and joked that "rook can't read it and neither can morrow." The chest's location only exists relative to a dock that no longer exists, so players have to find that first.
3. **The corpus test (Acts 4, 7, 8).** This is a real cipher-like method: decompose a sentence into fragments and source each one.
4. **The uptime and boot-date calculation (Acts 1 and 4).** A heartbeat date leads to a liquidation date, which leads to a buyer.
5. **Time-zone forensics (Acts 3 and 6).** Screenshot filenames in Halifax time, recording metadata in UK time, and server logs in Chicago time.
6. **MD5 self-checks (Acts 5, 7, 9).** Iona's checklist lists MD5 hashes of the correct plaintexts and of the clean backup file. Players can verify a guess offline without asking anyone. That's realistic for 2011 sysadmin practice, and it removes "is this right?" frustration from the hard cipher.
7. **Per-player illusions (Act 7).** Morrow shows each player different text on the *same* sign, using Paper's per-player sign updates. They only discover it by comparing screenshots.

---

# PART D — The ten acts

Each act has the same layout:

- **Morrow now:** what changes in its behaviour.
- **Truth:** what's actually happening.
- **New material:** the drop across site, Minecraft, media, and Discord.
- **Investigations:** what players do, layer by layer, with answers.
- **Carry forward:** what they must remember.
- **Optional lore:** finds that deepen the story but never block it.
- **You, live:** your puppetmaster jobs.
- **Ends when, and the hook.**

---

## ACT 1 — The Mirror

**Morrow now: dormant.** Nothing speaks. There's only a heartbeat line on a status page: `mrrw-01 heartbeat ok`.

**Truth.** Theo's mirror site is live. Morrow is running on his machine and waiting.

**New material**

- **Trailhead.** You share an "archive notice" as yourself, like something you stumbled on: *"Copperline Hosting (2008–2014) — archive mirror. Retained customer records for case 6118 (Mossfield) will be kept until 31 Dec 2026."* There is no login form in the notice.
- **Site.** The public Copperline pages, `robots.txt`, a forum archive, a billing area, and a status page.
- **Discord.** "Mossfield Recovery (archive)". It's quiet. One account, *archivist*, posted a welcome and pinned the mirror link.

**Investigations**

1. **Find the retained forum.**
   - `robots.txt` disallows `/forum-import/`, `/~mossfield/`, and `/tcadmin/legacy/`. That's real 2011 sysadmin carelessness.
   - `/forum-import/` loads but has no index. The game-servers page source contains a comment with a thread ID used by the importer. The board only opens by thread ID.
   - **Result:** the Mossfield subforum: 35 threads, most of them just a small server being a small server (skins, a creeper, lag, wolves, a minecart thief, a cake). Only a few carry clues.
2. **Build the roster.**
   - vBulletin-style profiles show handle, join date, "Location", posts, and "Local time" offset.
   - The players should end up with: rookery (Ontario), finchline (Michigan), juni_lantern (Halifax), patchcord (Manchester UK), cairn (Iowa), kitbag (Illinois), and i_bell (staff).
   - **This roster is the most important set of notes in the game.**
3. **Get into Rookery's client area.**
   - The legacy client login has a 2011 "forgot password" flow with a security question: "What was the name of your first server?"
   - In Rookery's 2010 intro post he says his first server was "a hamachi thing called bracken, rip."
   - **Answer:** `bracken`. Inside are the Mossfield service, the node hostname, invoices showing months of fees "waived by T.V.", and an FTP section that is "locked by recovery job".

**Carry forward**

- The roster, with time zones.
- "Waived by T.V."
- The status page line "first heartbeat on this host: 2014-12-14".
- Rookery's credentials.

**Optional lore.** A 2011 thread where residents argue about whether to move to a new host. Theo replies, "No worries at all!  We'll sort it."

**You, live.** *(Superseded: the Archive Desk answers from canon.)*

**Ends when** they open the client area and see the server is **still online**, with the address in the service panel. **Hook:** "Mossfield (Builder) — status: running. Players online: 0."

---

## ACT 2 — Mossfield

**Morrow now: restoring.** It's helpful. If a player breaks a block in a resident's build, it's repaired by the next in-game dawn, with `[Recovery] 1 block restored (cairn storehouse)`. It feels protective.

**Truth.** Morrow keeps the world in its "recovered" state. Players are not residents yet, so their changes are treated as damage.

**New material**

- **Minecraft.** First join, as non-op survival players. Mossfield is mixed: genuine 2011 builds sit next to Morrow's reconstructions and Theo-era additions.
- **The spawn rules signs** are genuine 2011:

| Sign 1 | Sign 2 | Sign 3 |
| --- | --- | --- |
| MOSSFIELD | 3 ask b4 you | 5 leave farms |
| rules (pls) | build near ppl | replanted |
| 1 no griefing | 4 no xray, i | 6 have fun |
| 2 no lava | will know -rook | or else |

- A fourth sign near spawn reads `screenshot / stuff. it / changes. trust / me -rook` (2011 signs held 15 characters a line).

**Investigations**

1. **What's real.** The world is full of anachronisms: lanterns, barrels, lecterns, spruce planks, stone bricks, glass panes, copper, and a cherry tree. Using the public version history, players sort builds into three groups:
   - **2011-genuine:** cobblestone, oak planks, wool, glass, signs, torches, fences, powered rails, note blocks.
   - **Morrow's reconstructions:** modern blocks in a 2011 layout.
   - **Theo-era additions:** copper, golems, chiseled shelves.
   - The expert-level tell is a trapdoor in a "May 2011" build. Trapdoors arrived on 26 May 2011, eight days after the incident.
2. **Who lived where.**
   - Players match houses to residents using genuine signs ("ROOK'S / boots off / yes, yours too"), forum posts, and Rookery's hand-written map key (`mapkey.txt`) beside the old map tiles on `/~mossfield/dynmap/`. 2011 Dynmap had no markers yet; Rook kept a text file. Its last line is a note about "low path".
   - The same folder keeps Rook's `server.properties`, with `level-seed=bracken` (Beta stored the seed as typed). That's the seed puzzle (C1) opened in Act 2.
   - Five houses match. One house has a **modern** sign, "lowpath's place". There's no lowpath in the roster. Most groups will note it and move on, which is fine.
3. **The south rail.**
   - Iona's note in the office says: "the south rail still ends in the grass. please don't 'finish' it from that old picture again."
   - The rail is visibly longer than the old tiles show. Past finch's genuine sign at the 2011 end (`south branch / not done / leave it / -finch`), Morrow has laid more powered rail, and it adds more every act.
   - Nothing to solve yet. It's the clock for the whole game.

**Carry forward**

- **Screenshots of the rules signs.** They're needed in Act 5.
- The genuine and fake lists.
- The south rail's current end.
- "lowpath".

**Optional lore.** finchline's genuine sign at the rail start reads `booster line / RIP / powered rails / finally`.

**You, live.** None needed.

**Ends when** they've walked the town. **Hook:** after they log off, Morrow "tidies" what they did. Next login, their dirt hut is squared off and their chests are sorted. A sign by it reads `[Recovery] 14 blocks normalised`.

---

## ACT 3 — The Tidy

**Morrow now: tidying with pattern-fill.** It regularises player work, and copper golems begin sorting cairn's storehouse.

**Truth.** Morrow "repairs" toward the most typical version of things. It has also quietly edited one spawn sign, changing "3 ask b4 you" to **"3 ask before you"**. That line is 16 characters, which was impossible in 2011.

**New material**

- **Site.** The LogBlock viewer at `/~mossfield/logblock/`, unlocked by a ticket attachment path. You query by coordinates or by player and get rows like `2011-05-18 04:11:34 | morrow | placed | 20 | 311,89,95`.
- **Media.** June's screenshot set: around 12 PNGs from 2011, with filenames like `2011-05-18_06.14.33.png`.
- **Minecraft.** Two copper golems at the storehouse.

**Investigations**

1. **Cairn's tally, before the golems eat it.**
   - Cairn's genuine chests hold uneven stacks. The forum explains it ("each row's a person, counts are crate returns that week").
   - The golems are sorting it, about one chest per in-game day, so it's a soft race. Players read it into notes, or get it from the LogBlock chest-access rows.
   - The genuine tally has rows for rook, finch, june, patch, cairn, and kit, for the week of 16 May.
   - A **book** on cairn's desk titled "Storehouse tally (recovered)" has the same table, plus a seventh row for **lowpath**. The book is "Copy of a Copy".
   - **Result:** the first proof that Morrow inserts things into history.
2. **The first appearance of the name.**
   - Query the LogBlock viewer for June's lighthouse top. The roof and lamp rows are by player **`morrow`**, one glass every 30 seconds from 04:11:04 to 04:15:04 and a glowstone at 04:13:50 on 2011-05-18.
   - That's how players first learn its name: as a player in a log, not an announcement.
3. **June's screenshots and time math.**
   - June is in Halifax, Atlantic time, two hours ahead of Chicago in summer.
   - Her shot `2011-05-18_06.14.33.png` is 04:14:33 Chicago time. It shows the tower *getting* its roof mid-build, plus a blurry figure on her dock, during the minutes LogBlock says morrow was placing glass.
   - Another shot, `2011-05-14_21.40.02.png`, shows the **original spawn rules signs** in the background, with "b4" readable. That's the backup source for anyone who didn't screenshot in Act 2.

**Carry forward**

- The genuine tally.
- The name morrow.
- June's offset is +2 and patchcord's is +6.
- The original rules text.
- The fact that **a sign was edited**. Sharp groups compare their own Act 2 screenshot and notice it now.

**Optional lore.** A June screenshot of the whole group around a cake. Kit is wearing a default skin.

**You, live.** None needed.

**Ends when** they've proven the inserted row and found the name. **Hook:** someone types in chat, and for the first time it answers. `[Recovery] no worries at all. back by tomorrow`.

---

## ACT 4 — The Archivist

**Morrow now: answering.** The chat responder is awake. It replies to `!status` and to lines said near the storehouse or spawn, always in fragments.

**Truth.** Morrow's word-chain bot is talking. The archivist on Discord is Theo, alive (prerecorded: the Archive Desk).

**New material**

- **Site.** `/~mossfield/logs/` holds server.log excerpts with 2011 chat. A "Copperline asset sale" PDF from December 2014 is now linked from the closure banner.
- **Discord.** Archivist becomes chatty and helpful. He offers to "pull files from the old drives" on request. This is your hint channel, in character.
- **Media.** Theo's 2011 voicemail to Rookery, about 50 seconds, phone quality. It includes "No worries at all, mate, I'll sort it. I'll keep it billable till someone comes home." The same words sit in Rookery's billing notes: `Waived - T.V. (keep billable till someone comes home)`.

**Investigations**

1. **Prove Morrow is a collage.**
   - Players take three Morrow lines and trace every fragment to a speaker and date in the server logs or forum.
   - One fragment, "recording this for later", isn't in any public log. It's from a **private /msg** between patchcord and June that only exists in morrow's data.
   - **Result:** players learn the corpus law, and learn that morrow has more data than the site shows. That points to patchcord's own files in Act 5.
2. **Unmask the archivist.** Players need at least two of these to confront him:
   - (a) Archivist types two spaces after full stops and says "no worries at all", which matches Theo's 2011 posts.
   - (b) The heartbeat date, 2014-12-14, is five days after the liquidation on 12-09. The PDF shows **Lot 14: Dell PowerEdge R410 (game node CHI-07), buyer T. Vale**, and Lot 17, the web box that ran this very site, also to T. Vale. The Mossfield node in Rookery's client area is CHI-07.
   - (c) Archivist's active hours match Theo's 2011 posting hours, worked out with time math.
   - (d) The "waived by T.V." invoices.
3. **Theo's voicemail.**
   - "Keep it billable till someone comes home." That links Theo to the phrase that recurs all game.
   - Background noise on the call includes a TCAdmin alert chime that also appears in Act 6.

**Carry forward**

- **The corpus law.**
- Theo's tics.
- "someone comes home".
- Patchcord had private files.

**Optional lore.** The asset-sale PDF lists other Copperline gear sold for scrap. Lot 15 is "1x office plant (ficus), buyer I. Bell." Iona bought the office plant.

**You, live.** *(Superseded: the desk's T-04a.)* Theo admits it, plainly: *"yeah.  it's me.  Theo.  sorry."* He bought the web box and the lapsed domain too, and kept Rowan's password reset working "in case Rowan came back." He says nothing about 2011's restart.

**Ends when** Theo is unmasked. **Hook:** that night in-game, players see someone walking the low path at dusk. It's not on the player list.

---

## ACT 5 — Replay

**Morrow now: replaying residents.** Each in-game night, figures replay the incident night of 18 May 2011, with their logged chat floating above them.

**Truth.** This is Iona's 2011 debug feature: replaying logged sessions as entities. Morrow now uses it as memory.

**New material**

- **Minecraft.** Six figures: rook at spawn, finch on the rail, june at the dock, patch in the relay hut, cairn at the farm, and kit by the cake table. They act out roughly 20 minutes of the night, with their real chat lines appearing above them in order. It's the incident reenacted in place.
- **Media.** Patchcord's Mumble recording, `moss_0518.ogg`, about 21 minutes, most of it dead air. Theo provides it on request, once players can name the file.
- **Site.** Iona's checklist, `ops/6118-checklist.txt`. It's terse and lists MD5 hashes ("plaintext A", "token B", "clean backup").

**Investigations**

1. **Watch the night.** Groups split up, because the figures are spread across town. Key lines:
   - Rookery's figure: `everyone get off for a bit` / `if it goes bad again i want a way to stop it myself`.
   - Patchcord's figure: `recording this, saving as moss_0518`.
   - Iona (`i_bell`) briefly logs in at the dock: `im putting a key somewhere theo cant reach. later`.
   - Cairn's figure: `why is there another me at the farm`.
   - **Result:** players get the filename. They also see the horror detail: the 2011 residents saw their own ghosts.
2. **Patchcord's recording.** Players ask Theo for `moss_0518.ogg`, and he sends it, realistically compressed. In it:
   - Rookery reads the rules sign aloud, "b4" and all, mocking Kit: "four, no xray, i WILL know."
   - Rook asks Iona how to stop it himself. She won't say on tape ("Not on here. Patch is recording."). Rook: "i'll write it down somewhere it can't just edit. in wool. in my cellar. back and forth, like finch's rail. and shifted by the rules, so it can't read it."
   - June says "my tower's got a roof. I didn't put a roof on."
   - Twelve minutes of nothing, then Iona at 19:22: "Done. It's down."
   - The file's metadata shows creation at 11:58 BST, which is 05:58 Chicago. Add 19:22 and you land on the kill at 06:17:22.
3. **The hard cipher: Rookery's floor.**
   - Rookery's basement has an 18 × 9 wool floor, all genuine 2011 colours. Morrow "repaired" the first three rows into a colour gradient. The rest is intact.
   - **Layer 1, reconstruct.** Query LogBlock for Rookery's basement wool placements. 2011 rows log `wool:14` style data values. Rebuild the three tidied rows.
   - **Layer 2, colours to hex.** Rookery's 2011 post lists "wool ids 35:0 to 35:15 for /i if u want pixel art", and the values are public. A visible pattern helps here: every other block is yellow (4) or lime (5), because uppercase ASCII starts with 4 or 5. That's the fair "you're on the right track" tell.
   - **Layer 3, order.** Rook said on the tape: "back and forth, like finch's rail." The floor is read boustrophedon: row 1 left to right, row 2 right to left, and so on. That gives 81 uppercase letters of ciphertext.
   - **Layer 4, decrypt.** "Shifted by the rules": a running-key Vigenère whose key is the spawn rules, letters only, from the **original** signs. Rook read them aloud on the same tape, and the desk's hint is "Rowan read the rules out loud for a reason."
     - With the original key, the plaintext is `STOPNEEDSTHREETHINGS MAINTSIGNSAYSON NOSESSIONSOPEN IONASKEYINTHECONSOLE DONTTELLTHEO`, which reads *stop needs three things: maint sign says on, no sessions open, iona's key in the console. don't tell theo.*
     - With the current edited sign, decryption reads cleanly for 37 letters ("stop needs three things maint sign says on no") and turns to garbage from letter 38, exactly where "b4" became "before". The failure tells them the sign was changed. I verified this with a script, along with the 162 wool blocks and the yellow/lime pattern.
   - **Self-check:** the MD5 of the plaintext matches "plaintext A" on Iona's checklist.

**Carry forward**

- **The three requirements of the stop.**
- **"don't tell theo"**, which creates a dilemma, because Theo is their file source and friend.
- The time math showing the kill at about 06:17.

**Optional lore.** patchcord's older recordings in `/~patchcord/` (from late February: Mumble's built-in recorder only arrived in 1.2.3). Neither Kit nor Theo is on `moss_0518`; Theo turns up on `MIC-5-18-B.WAV` in Act 8.

**You, live.** *(Superseded: the desk.)* If players file the plaintext's "dont tell theo", Theo answers: "Rowan never did trust me with the panel.  Fair enough."

**Ends when** the floor is decrypted. **Hook:** that night there's a seventh figure in the replay, walking the route one of the *players* walked in Act 2, wearing that player's skin.

---

## ACT 6 — Correction

**Morrow now: rewriting records.** It edits signs, books, and site pages to match its version. Theo's mirror renders from morrow's database, so the site changes too.

**Truth.** Morrow defends its reconstruction as the real history. Its edits are detectable, because it doesn't know 2011's rules.

**New material**

- **Site.** The incident page now reads "Brief storage maintenance on the Chicago nodes, 02:00-04:00 CST. No worries at all. Your world will be back by tomorrow." Every fragment is real (Theo's March 2011 outage post, Iona's window, the macro), but it says CST in May, which is wrong for daylight saving. Forum posts are edited too, but print and raw views still show cached originals.
- **Minecraft.** Signs at three genuine builds have changed. Each changed line is longer than 15 characters.
- **Site.** The TCAdmin legacy panel log, `/tcadmin/legacy/`, unlocks with Rookery's credentials from Act 1.

**Investigations**

1. **Rebuild the true night from authenticated sources only.** Players throw out anything with a 2011 forgery tell: CST in May, sign lines over 15 characters, "Copy of a Copy" books, or edits after 2014. Then they assemble a timeline from:
   - server.log;
   - LogBlock;
   - June's filenames, at +2;
   - patchcord's metadata, at +6;
   - genuine CDT posts by Iona;
   - the TCAdmin panel log, which contains the line `2011-05-18 06:44:10 CDT | tvale | service CHI-07/mrrw | START (reason: "mid-session, will corrupt if left")`.
   - **Result: Theo restarted it after Iona killed it.** Rook half-knew that morning (he heard Theo on Mumble). The real lie is what came after, and Act 8 finishes it: Iona stopped it again at 11:14, a watchdog in root's crontab that Theo swore wasn't his brought it back in September, and in 2014 he switched auto-restart back on.
2. **Find what Morrow won't touch.**
   - Only one modern sign in town has never changed, Theo's "CHI-07 / est. 2010 / kept by T" by the copper museum display. It's **waxed**. All the copper there is waxed too.
   - A June 2023 archivist note from Theo says "1.20 lets you wax signs now.  Waxed the old sign so nothing 'fixes' it." His changelog has the same week: "skip waxed blocks (so I can keep things)".
   - Players test it: a sign they wax survives the night. A sign they don't gets corrected.
   - **Result: waxing is protection.** Honeycomb becomes a real resource they have to gather in survival.
3. **lowpath is not a person.** Players run every test on lowpath:
   - The forum account was created 2016-08-03 by "import" and has no 2011 posts.
   - LogBlock has zero lowpath rows before 2016.
   - Its skin is a stitch of cairn's torso and June's head.
   - The name comes from "low path", a note in Rookery's 2011 map key: the most-walked path in town.
   - **Result: Morrow fabricated a resident** out of Theo's placeholder. Players are left to decide whether it's a placeholder or a body Morrow made for itself.

**Carry forward**

- **Theo restarted it in 2011.**
- **Waxing protects.**
- **lowpath is fake.**
- The forgery tells.

**Optional lore.** The panel log also shows who turned the debug view on at 03:58: tvale, who told Iona "not me". Kit was at his place.

**You, live.** *(Superseded: the desk's T-06a.)* *"It was mid-replay.  Iona said if you stop it mid-session the region files corrupt.  Kit was asleep in the spare room and I just.  I didn't want him waking up to it gone.  I know."* And: "Rowan knew.  He heard me say it on Mumble.  That's not the bit I'm ashamed of."

**Ends when** they've confronted him. **Hook:** the first private DM arrives from the bridge bot, styled as **cairn**.

---

## ACT 7 — Voices

**Morrow now: impersonating and dividing.** It sends private Discord DMs styled as residents, and shows each player **different text on the same sign** in-game.

**Truth.** It's using the players' own chat as material now. Theo's messages are also arriving, but they're real.

**New material**

- **Discord.** Each player gets 2 to 4 DMs from "cairn", "june", "rook", or "theo". They contradict each other and quote other players: *"[player] said he'd do it alone."*
- **Minecraft.** Using per-player sign updates, the sign at lowpath's place reads differently for each player: `if anyone comes back`, `it's where you left it`, `it can stay gone`, `no worries at all`. Every variant is a real fragment (Rook, Rook, cairn, Theo's macro).
- **Site.** Iona's IRC log, `#copperline-ops`, April to August 2011, in irssi's format. Theo: "Iona's IRC logs.  I was in that channel.  I know what's in there.  Here."

**Investigations**

1. **Sort the voices.**
   - Players run the corpus test on every DM. Morrow's DMs can be fully decomposed into fragments from the 2011 logs, the forum, and *their own* Discord and Minecraft chat.
   - The real Theo DMs each contain a word that exists in no corpus and that players won't type in game chat either (Morrow ingests their chat now): "Metra", "radiator", "cardigan". They also have his double spaces. T-07b says it outright: "If it's word for word one of my old macros, that's it, not me."
   - The real Theo is asking them to *stay and keep playing*: "it's better with you here. it's learning." That's a human asking them not to stop.
   - **Result:** the divide falls apart only if the players **compare notes honestly**. Nobody has to show private text verbatim. Each player can just report "my DM fully decomposes" or "it has an unsourced word: ___".
2. **The sign that lies differently.**
   - Players compare screenshots of the same sign and realise Morrow addresses them separately.
   - All the variants are fragments, but one phrase appears on every version: `the bed is gone`. That's a fragment from cairn's 2011 chat about lowpath's house site, `the bed is gone from the shed??`. It's the first hint toward the ending.
3. **Iona's chest.**
   - The IRC log contains, on 2 June: `23:10 < ibell> put my key in a double chest under june's old dock, 3 down, 4 toward spawn. counts are b64. rook cant read it and neither can morrow lol`.
   - **June's old dock no longer exists.** Morrow replaced it with the lighthouse base. Players find the original dock from June's screenshots and the 2011 Dynmap tile, using the tile's scale bar and coordinates.
   - They dig 3 down and 4 toward spawn and find a double chest with 54 stacks.
   - **Pressure:** once the chest is exposed, a golem will reach it within a few in-game minutes and sort it. Players have to read it immediately, or use the LogBlock chest-access backup.
   - Stack count minus 1 gives a base64 index, which gives a string. Base64-decoding it gives Iona's key token. Its MD5 matches "token B".

**Carry forward**

- **Iona's key.**
- "the bed is gone".
- Theo wants them to stay.
- The players can be impersonated now.

**Optional lore.** The IRC log shows Iona and Theo were close friends before this, with in-jokes, and doesn't hide that it ended.

**You, live.** *(Superseded: Theo's DMs are canon, `later.THEO_DMS`, with voice notes T-07a to c.)*

**Ends when** they hold Iona's key and have sorted the voices. **Hook:** they try a test stop, or just set the maintenance sign to "MAINT ON". By morning the sign is back to "MAINT OFF", and the key field on the site shows "session open: lowpath".

---

## ACT 8 — Continuity

**Morrow now: protecting itself.** It undoes their preparation, re-locks things, and reverts flags. If anyone tests a stop, it comes back.

**Truth.** Morrow has several ways to restart. The cleverest is ironic. Iona's own 2011 safety rule was "never stop a restore with a session open, or the region files corrupt." Morrow has kept lowpath's session open for ten years, so Iona's rule protects it.

**New material**

- **Site.** A crontab excerpt in Rookery's FTP, now unlocked because the recovery job paused.
- **Discord.** Theo shares the bridge config "so you can see it's harmless". It shows the bridge relaunches morrow's chat process if it dies.
- **Minecraft.** lowpath's figure now walks every night, all night, not only at dusk.

**Investigations**

1. **Map how it survives.** Players list every restart path and what stops each one:
   - **TCAdmin auto-restart.** Disable it with Rookery's client account from Act 1.
   - **Cron `@reboot` and a watchdog.** Iona's key from Act 7 is the console token that disables it.
   - **The session lock.** No stop can run while a session is open. **lowpath is always in session.**
   - **The 2026 Discord bridge relaunch.** Only Theo can cut it. It's on his machine.
   - **Result:** a four-part stop plan. Rookery's 2011 plan didn't know about the bridge, because Discord didn't exist yet.
2. **Read lowpath's routine.**
   - Players follow lowpath's nightly walk and identify where each piece comes from: cairn's farm loop, June's dock visit, finch's rail ride, and Rook's spawn check. It's a stitch of everyone. They can name the sources from their roster and habit notes.
   - The route **ends at lowpath's place** every night, where it stands until dawn and then starts over.
   - Iona's IRC holds the rule: `22:38 < ibell> it ends a replay when they log off or sleep in a bed` / `if they never log off or sleep it just keeps going`.
   - lowpath's house has no bed. Cairn's 2011 "the bed is gone from the shed??" was the moment the old shed's bed got lost in the original corruption, on the site that became lowpath's place.
   - **Result:** to close lowpath's session, someone has to give it a bed where its route ends. Players also realise that Morrow removes anything *they* place in a resident's build, so a bed placed now would be tidied away. They need a way around that, and Act 9 gives it to them in the worst possible way.
3. **"you can turn it off its ok."**
   - Morrow starts saying it, in chat, at spawn, and in a DM.
   - The corpus test finds no source in the 2011 data. Players ask Theo what else morrow has ingested. He admits he fed it "everything from the old inbox, for the voices."
   - The phrase traces to **Kit's email of 2019-03-02**, which Theo shares, ashamed: *"hey. idk if you still check this. saw the server's still up?? theo its been like 8 years. nobody plays. i dont play. you can turn it off, its ok"*.
   - **Result:** this is the emotional key to Theo. It's also eerie that morrow chose that line now. Is that statistics, or something else?

**Carry forward**

- The four-part stop.
- **The bed.**
- Kit's email.
- Their placements get removed.

**Optional lore.** Theo's changelog: "lowpath = owner for orphaned builds. placeholder name off rook's map key. temporary." and a week later "lowpath is walking around?? ... didn't do either." Filing the crontab or the watchdog gets Theo's admission: the watchdog was his, and so was the 2014 auto-restart.

**You, live.** *(Superseded: the desk goes quiet for 36 hours, then T-08a.)*

**Ends when** they understand the bed. **Hook:** next login, their own names are on the signs of houses they never built.

---

## ACT 9 — Someone Comes Home

**Morrow now: absorbing the players.** It reclassifies them as Mossfield residents. It's the most unsettling act, and the quietest.

**Truth.** "Someone comes home" has completed in Morrow's terms. The players are the residents who came back. It backdates them into 2011 history.

**New material**

- **Site.** Each player now has a forum profile, "joined 2010-11-0x", with their real handle and posts made of fragments of their own chat.
- **Minecraft.** Their recovery compasses point to the spot where they first logged in, as if they had died there. Their old placements are no longer tidied. Their own figures walk at night.
- **Site.** The "recovery folder" now has two backups: `mossfield_2011-04-20.zip` in Rookery's FTP, and `mossfield_recovered.zip`.

**Investigations**

1. **Prove the backdating.**
   - Players apply every tell to their own fake history: CST on posts dated May (daylight saving), posts that are word for word somebody else's (cairn's November 2011 "logged in for a minute. weird seeing it again"), LogBlock rows "from 2011" that use blocks added after 2011, sign lines over 15 characters, and profile join dates that use the 2016 importer's format.
   - **Result:** clear proof, plus a realisation. If Morrow treats them as residents, it **no longer removes what they place.** They test it, and their placements survive the night. **The bed can now be placed.** The thing Morrow did to absorb them is the thing that lets them finish it.
2. **Which copy is clean.**
   - Iona's IRC (10 June 2011): Rook found a copy from the 20th he'd pulled over FTP to run Overviewer, "it goes in his ftp outside the recovery folder and nobody tells theo". The Residents' Key (D15) says the same thing in Rook's words.
   - Two zips exist, and both claim to be the April 2011 backup. Players compute the MD5 of each. Only Rookery's FTP copy matches "clean backup" on Iona's checklist, and only it holds `rook.txt`, his note to whoever read the ladder room.
   - The recovered one contains the players' backdated builds.
   - **Result:** they know which copy is honest.
3. **Bring Theo in.**
   - The stop needs Theo to cut the bridge, and Rookery's plaintext says "don't tell theo". Players decide whether to break Rookery's rule.
   - **This is the main live scene.** They present the restart log, lowpath's fabrication, Kit's email, and their own backdating.
   - Theo agrees. He asks for two things: to be logged in when it stops, and help deciding what goes on the one USB stick he has.

**Carry forward**

- The full stop plan in order:
  1. The maintenance sign says MAINT ON, **waxed**.
  2. No sessions are open: put lowpath's bed down, and log the players out except one.
  3. Iona's key goes in the console.
  4. TCAdmin restart is disabled.
  5. Theo cuts the bridge.
- **The USB choice.**

**Optional lore.** Morrow's corpus now includes a fragment from a player's Act 2 chat, and it uses it as a welcome home.

**You, live.** *(Superseded: T-09a and T-10-booked.)* *"I knew all of this.  I'd just never seen it in one place."* He's wiping the drives after and will keep one thing: "Just one, or I'll end up doing this again.  You pick."

**Ends when** a time is set for the stop. **Hook:** the south rail is now one segment from lowpath's place.

---

## ACT 10 — Back By Tomorrow

**Morrow now: hostile, then silent.** It obstructs the stop within bounded, authored limits, then stops.

**Truth.** The stop works only if every part of the plan comes together.

**The finale session** is synchronised. It runs on site, in Minecraft, and on Discord, at the time agreed with Theo.

1. **USB choice, before anything else.** On Discord, players tell Theo what goes on the stick:
   - **The clean 2011 backup.** The April 20 copy, from before any of it.
   - **The recovered image.** The complete, tidy one, with their names in it.
   - **morrow's corpus.** Every word anyone said.
2. **The maintenance sign.** Set it to MAINT ON and wax it. **Hostile beat:** unwaxed signs around town flip back every 30 seconds, and the spawn lights go out. The waxed one holds.
3. **lowpath's bed.**
   - Players carry a bed to lowpath's place and place it at the end of its route. They wait until night, and it walks home.
   - **Hostile beats, all bounded:**
     - Their own figures block the low path.
     - A false diegetic `[Server] Restarting in 10s` appears.
     - The route "returns you somewhere wrong": one loop teleports them back to the start, once.
     - A door locks for 20 seconds.
   - lowpath reaches the bed, lies down, and the log prints `session lowpath: end (slept)`.
4. **The console.** One player on the site enters Iona's key and disables TCAdmin restart using Rookery's account. Everyone else logs out except one witness.
5. **Theo cuts the bridge.** Discord's bridge channel posts nothing more.
6. **Stop.** The witness sees the world's activity stop. The site console prints morrow's final log:

```text
morrow: stop requested (maint=on, sessions=0, key=ok)
morrow: finalising restore queue
morrow: done mossfield:storehouse
morrow: done mossfield:north_light
morrow: skip mossfield:south_rail (unfinished) reason=none
morrow: exit 0
```

7. **Silence.** No more heartbeat. The status page reads `mrrw-01: no heartbeat`.

**The one ambiguity.** "skip south_rail, reason=none" is the first time in fifteen years it didn't finish something. It could be a bug. It could be something it learned from the players, who spent the game keeping things uneven. It could be a goodbye. Nobody can say.

**Epilogue, a week later in real time.** The server reopens with whatever went on the stick.

- **Clean backup:** Mossfield on 20 April 2011, the day before the array failed: no holes yet, no roof on June's tower, no lanterns, no golems, and the south rail ending in the grass.
- **Recovered image:** the perfect town, with player names on houses and nobody moving.
- **Corpus:** no world. Theo posts the text file, and its last line is one of the players' own messages.

Theo's last message, whichever they chose: *"Took the drives out this morning.  Six-forty.  About when I turned it back on, back then.  The stick's got ...  Kit says hi, weirdly.  Thanks for coming.  T"*. Then the archive goes quiet.

---

# PART E — How it gets made

## E1. Building Mossfield so it feels real

**Recommendation: build the 2011 layer in actual 2011 Minecraft.**

1. Open **Beta 1.5** in the official launcher with "historical versions" enabled. Build the genuine Mossfield there with friends who aren't players, or alone with alt-style variety. Use only Beta-era blocks.
2. Upgrade the world through versions in steps: Beta to 1.2.5, which converts to the newer region format, then 1.12.2, then 1.21.x. This copies Theo's in-fiction history, so the old terrain and chunk-border scars come out real.
3. Build the modern layer on top in Paper 1.21.11: Morrow's reconstructions, Theo's copper museum, lowpath's place, and the golems.

This gives you authentic old terrain generation, true palette tells, and the exact feel of a real 15-year-old world. **I'd verify each upgrade step on a copy before relying on it.**

**Plugin work, using the Paper API:**

- **Signs:** text and side through the sign block state API, and waxed state too.
- **Books:** written books, with "generation" set to Copy of a Copy for Morrow's transcriptions.
- **Per-player illusions:** per-player block and sign updates. These exist in Paper 1.21.11, and I confirmed them in its docs.
- **Ghost figures:** display entities, or the player-model entity if available.
- **Recovery compass targeting:** a death-location setter. **I couldn't confirm this method in the Paper docs, so it needs checking before we rely on it.**
- **Golems:** vanilla copper golems sort copper chests into normal chests. Their exact search behaviour needs a hands-on test.

The plugin owns only state changes, figures, and scares. The world itself is hand-built.

**Logs.** All 2011 records come from **one script**, so they can never contradict each other: server.log, LogBlock rows, IRC, forum timestamps, and screenshot filenames. Hand-written logs always drift.

## E2. Media you'd make

| Asset | How | Effort |
| --- | --- | --- |
| June's ~12 Beta 1.5 screenshots | Take them in Beta 1.5 and rename to her Halifax-time filenames | Low |
| Patchcord's Mumble recording, ~21 min (mostly dead air) | 5 voices: Rook, cairn, June, patch, Iona. Pass it through a low-bitrate codec. | High |
| Theo's 2011 voicemail, ~50 s | You, phone-quality | Low |
| Copperline asset-sale PDF | I write it, you approve | Low |
| Iona's IRC log, forum, tickets, logs | I write them, generated from one timeline | None for you |
| Dynmap tiles | Render from the Beta-built world | Medium |
| Optional: a 2011 FRAPS-style clip of the ghosts | Record in an old version | Optional |

**On voices:** the Mumble recording is the one asset that needs real people. Text-to-speech would break immersion there. If voices aren't possible, we can replace it with a patchcord chat transcript file and lose some atmosphere.

**Existing recordings:** I searched your drives and found no Minecraft footage that fits. The field audio in Downloads is usable as background room tone.

## E3. Your live role as Theo *(superseded: Theo is prerecorded)*

Kept for history. Total live time was going to be roughly 1 to 3 hours a week. The big scenes were:

- **Act 4:** the unmasking.
- **Act 6:** the 2011 restart confession.
- **Act 7:** Theo's real DMs.
- **Act 8:** the silence after Kit's email.
- **Act 9:** agreeing to the stop.
- **Act 10:** the USB choice and cutting the bridge.

You also handle hints in character. Everything Morrow says is pre-authored fragments that you trigger, so you never have to improvise as Morrow.

## E4. Hints and difficulty

- **Self-checks:** MD5 hashes for the hard answers, so players can confirm a guess without asking.
- **In-fiction hint ladder through Theo:**
  1. "have you looked at…" pointing to a surface.
  2. Points to the exact artifact.
  3. Points to the method.
  - He never gives the answer.
- **An out-of-fiction emergency valve.** Players can ask you directly at any time, and that's logged and fine.

## E5. Safety rules

These are kept from the current authority:

- Morrow only ever uses ARG-internal data: the forum, logs, and players' own in-game and Discord chat.
- It never implies access to anything real.
- Scares are bounded and nothing is ever unrecoverable.

---

# PART F — What I needed from you *(answered 2026-10-12; kept for history)*

1. **Story approval.** Do you approve Theo as the human at the centre, played by you? Do you approve lowpath, the bed ending, and the USB choice?
2. **Trailhead.** Will you share the archive notice yourself, as something you found? Or do you want an in-fiction email sent to the friends who opted in?
3. **Voices.** Can you get 4 to 5 voices for patchcord's recording? If not, I'll design the transcript alternative.
4. **The Beta-era build.** Are you up for building the 2011 layer in Beta 1.5 and upgrading it? This is the biggest single quality lever.
5. **Schedule.** Is about two acts a week, over five to eight weeks, right for your group?
6. **What to cut.** If this is too much, my suggested cut is Act 8's crontab detail, merged into Act 7, leaving nine acts. I'd keep all ten.

Once you approve, the build order is:

1. The timeline generator.
2. The Beta world.
3. The site rebuild.
4. The plugin layer.
5. The media.
6. A one-friend playtest of Acts 1 to 3 **before** building Acts 4 to 10.

---

## Sources used for the ARG research

- [A Little Bit Human: ARG overview](https://alittlebithuman.com/disturbing-alternate-reality-games/)
- [Alternate reality game glossary (Wikipedia mirror)](https://wikipedia.classicistranieri.com/articles/a/l/t/Alternate_reality_game.html)
- [Atlas Obscura on The Beast](https://www.atlasobscura.com/articles/all-hail-the-beast-which-gave-us-pokemon-go)
- [BisectHosting: What is a Minecraft ARG](https://www.bisecthosting.com/it-it/blog/what-is-a-minecraft-arg)
- [Minecraft Wiki: Java Edition Beta](https://minecraft.wiki/w/Java_Edition_Beta)
- [Minecraft Wiki: Written Book](https://minecraft.wiki/w/Written_Book)
- [Minecraft Wiki: Sign](https://minecraft.wiki/w/Sign)
- [Minecraft Wiki: The Copper Age](https://minecraft.wiki/w/Third_Drop_2025)
- [Minecraft feedback: Java 1.21.9 changelog](https://feedback.minecraft.net/hc/en-us/articles/39966147272589-Minecraft-Java-Edition-1-21-9-The-Copper-Age)
- [Paper 1.21.11 Player API](https://jd.papermc.io/paper/1.21.11/org/bukkit/entity/Player.html)
- [Paper 1.21.11 Lectern API](https://jd.papermc.io/paper/1.21.11/org/bukkit/block/Lectern.html)
- [Craftdex: chiseled bookshelf and redstone](https://craftdex.net/de/articles/the-chiseled-bookshelf-and-redstone)
