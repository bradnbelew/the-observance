# IDEA — The Record Writes You In

> *you are becoming the keepers.* over the arc the record stops describing dead
> people and starts describing the **living** ones — in a dead keeper's exact hand,
> at a dead keeper's exact custom, in a dead keeper's exact cadence. profiling
> stops being a parlor trick ("it knows you mine alone") and becomes the felt body
> of FACT 15: *it has done this before, to people exactly like you, and it is
> writing you into the same column.*

Ground read: `arc/lore/canon-spine.md` (FACTs 1, 5, 9, 12, 13, 14, 15; §1 keeper
voices; §6 hard rules), `arc/lore/LORE-BIBLE.md`, `arc/corpus/journals-*.md`,
`discord/src/voice.ts`, `discord/src/showrunner/customs.ts` + `customs.run.ts`,
`plugin/.../beats/lib/BookAppearsBeat.java` + `LecternFillBeat.java`,
`plugin/.../signal/SignalSnapshot.java`, `plugin/src/main/resources/sites.yml`.

---

## 1. EXPOUND — the mechanic, the story, the mystery

### 1.1 The core object: the Hold-Book (one base lectern, mutating)

A single bound book sits on a lectern in the base — call it `stone_of_reckoning`'s
companion, the **Hold-Book**, placed Movement I by `LecternFillBeat` (its existing
`place_if_missing` path). Page 1 is the Archivist's opening (D01 `the-record-opens`,
already authored): the count of the living has begun by name.

Across the arc that book **gains pages**, out of line of sight, by re-firing
`LecternFillBeat` against the same anchor with a longer `pages[]` array. Each new
page is a record entry **about a living player**, written by the showrunner from
the player's own measured signals — but voiced as one of the six dead keepers.

The progression is the whole idea, and it is staged in three visible escalations:

1. **The record names a habit** (M1–M2, the Archivist's flat third person — the
   voice you already met). *"the one called {name} goes down alone and counts what
   he carries."* This is `voice.reportObserved` register, but written to a page,
   not the chat. Cold. True. Not yet attributed to a keeper.
2. **The record names the habit in a keeper's column** (M3). The same page is
   re-headed under a dead keeper's name — the keeper whose *fate rhymes with that
   tracked behavior* (canon-spine §1 table). The high-solo-mining hoarder's page
   moves under **Vaun**. The lone wanderer's under **Sella**. The page now reads
   in the Archivist hand but *files the living player beneath a dead one* — a
   ledger column with a dead name at its head and a living name in its rows.
3. **The keeper's own hand writes the living player** (M4→M5). The newest page
   is no longer the Archivist. It is first-person, in that keeper's inviolable
   voice (§1, §6.8), addressed *outward* — the dead keeper writing **the player**
   the way the keeper once wrote themselves. Vaun's page about a hoarding player
   is a tally with a blank give-back column. Sella's page about a wanderer is a
   reflection that gives the player's face back wrong. This is the moment the
   group realizes the journals they have been *reading* (the corpus) and the pages
   being *written about them* are the **same document, still being kept**.

### 1.2 How the per-player keeper mapping is decided (deterministic, grounded)

A new pure policy `decideKeeperEnrolment(input)` (mirrors `decideCustomReports`)
maps each living player to **at most one** keeper, using ONLY a signal the tracker
measured — never a guess. The mapping reuses the rhyme already canonized in
canon-spine §1:

| Keeper | Tracked signal (SignalSnapshot) | rhyme (canon §1) |
|---|---|---|
| **Vaun** | high `hoardedScore` + high `soloMiningRatio` | the Hoarder, kept everything |
| **Mara** | high idle/lectern time + low `blocksMined` | the Reader, map never tool |
| **Sella** | high `distanceFromGroup`, solo wandering | the Drowned, walked off alone |
| **Orin** | `the_bow` violations (passes markers uncrouched) | the Silent, would not bow |
| **Brann** | `the_dark_hours` violations (black-moon waking/sleeping) | the Night-Walker |
| **Iss** | leaned hardest on Whispers (the bond tally) | the Liar, wanted to be told |

A player is enrolled to a keeper only when that keeper's signal is the player's
**strongest measured deviation AND clears a floor** (precision over recall — §6.4).
A player whose signals are all near baseline is enrolled to **no one** and is
simply never written into a keeper column. This is the privacy/precision law in
code: a wrong "it knows you" is worse than none, so a flat player gets none.

The mapping is **a neutral colorant, not an election** (§6.3, the collective law):
it never says "you are the chosen Vaun." Multiple players can map to the same
keeper; a keeper with no matching living player simply gets no living rows this
run. The tally is never used to gate the ending.

### 1.3 The authoring module (showrunner, deterministic-first)

New showrunner module `keeper-record.ts` (pure policy) + `keeper-record.run.ts`
(I/O wrapper) — exact shape of `customs.ts` / `customs.run.ts`:

- `keeper-record.ts` (PURE): `decideKeeperEnrolment(input): KeeperRecordDecision`.
  No DB, no clock, no LLM. Reads the projected signal rows + the idempotency marks,
  returns `{ enrolments[], pages[], marks }`. Unit-tested in `keeper-record.selftest.ts`.
- `keeper-record.run.ts` (I/O): reads signals (a new `readSignalProjection()` on
  `repo.ts`, sibling to `readCustomViolations()`), runs the pure policy, then
  enqueues a `lectern_fill` (or `book_appears`) beat carrying the assembled
  `pages[]`. AUTO → `'approved'`; CONFIRM → `'pending'` (the INV-6 gate, identical
  to the customs toll path).

**The page text is assembled from deterministic templates, NOT free LLM.** Each
keeper has a small bank of **voice keys** in `voice.ts` (new `keeperPage*` family),
each a fill-in-the-blank line in that keeper's locked register with one or two
grounded numeric slots. e.g. Vaun's page is literally *"{name} has {oresMined} of
the deep and has set none of the first of it back."* — the number is measured, the
sentence is pre-written and in-voice. **The LLM is never in the page path.** If we
ever want a scalpel polish it is text-only, behind a deterministic fallback key
(the existing scalpel discipline) — but the vertical slice ships LLM-free.

### 1.4 How it plays across the 5-movement / ~2-week arc

- **Movement I (Act 1).** The Hold-Book appears (D01 page). It records the living
  group's habits in the Archivist's flat third person. No keeper attribution yet.
  The hook is FACT 1 felt as a growing document — each session, a new flat line
  about *them*. (Seed of the payoff is planted here; see §5.)
- **Movement II (Act 2 open).** The group meets the dead keepers' journals (the
  corpus, via keeper-stones). They read Vaun's tally, Mara's shelf, Sella's water.
  The Hold-Book keeps growing in the Archivist hand. The two documents look
  unrelated — *historical journals* vs *our base log*. (Inert. This is the plant.)
- **Movement III.** The escalation: the living-habit pages **move under keeper
  headings**. The hoarder's line is now filed beneath VAUN. The wanderer's beneath
  SELLA. First "oh —" beat. It still reads as the Archivist *organizing* — cold
  filing, not yet a dead hand reaching. Tied to FACT 5 (fate matches custom) and
  FACT 9 (the haunting had a biography): the page makes explicit that the living
  player's habit is the dead keeper's habit.
- **Movement IV.** The keeper's **own hand** writes the living player (first
  person, locked voice). Simultaneously the Iss-catch lands (D09→D10). The group
  re-reads the corpus and realizes the dead keepers' journals were never finished
  past-tense — the same hand is still keeping the column, and the newest rows are
  *them*. This is FACT 9 made physical and FACT 12's "kept, not departed" felt:
  the keepers did not stop writing because they did not depart.
- **Movement V (Act 3).** At the rite (D12, the Accepting), the final page writes
  itself: the living group entered into the record **as keepers**, in the keepers'
  cadence, for whoever comes next. The persistent world flips to *kept* (FACT 14 →
  15). No sentence states the induction; the *book having written them in* states it.

---

## 2. CRITIQUE — adversarial, honest

### R1 (sharpest) — PRECISION / false "it knows you". CRITICAL.
A keeper page is the single most "it knows me" surface in the whole project. A
**wrong** enrolment (filing a player under Vaun who isn't actually hoarding) is the
worst possible misfire on camera and a direct §6.4 violation. The whole effect dies
if even one page is wrong.
- **Mitigation:** enrol ONLY on the player's strongest deviation past a hard floor,
  computed from the same measured `SignalSnapshot` the customs bridge already
  trusts. A player below the floor on every axis is enrolled to NO keeper and gets
  no page — silence over a guess (INV-7). The numeric slot in every page is a raw
  measured count (`oresMined`, `distanceFromGroup`), so the page can never assert a
  habit the tracker didn't measure. Unit-test the floor in `keeper-record.selftest.ts`
  with baseline players → zero enrolments.

### R2 — COLLECTIVE law / accidental "chosen one". HIGH.
Putting a living name under a dead keeper's heading reads dangerously like electing
that player *the* Vaun — exactly the §6.3 forbidden move, and it would also wrongly
imply the ending gates on them.
- **Mitigation:** keeper columns hold **rows**, not a single name — multiple living
  players under one keeper, and the page never says "you are Vaun," only "{name}
  keeps the ways Vaun kept." The enrolment tally is explicitly excluded from any
  ending gate (it is a colorant; the rite gates on ACTIVE-player presence + the
  collective bow, unchanged). Author every page in the *plural-capable* third
  person so one or five names read identically.

### R3 — ANTI-JANK / reveal discipline + idempotency. MEDIUM.
A book that visibly gains a page while a player stares at the lectern breaks the
"never witnessed mutating" law and looks like a glitch.
- **Mitigation:** `LecternFillBeat` already mutates **only out of line of sight**
  (`mutateWhenUnwitnessed`) and is idempotent at the block level. The page set is
  re-derived deterministically each tick, and a per-(player,keeper) high-water mark
  in `showrunner_state.keeper_record` (sibling to `reported_customs`) guarantees a
  page is added once, not re-appended every cadence. A restart mid-tick re-derives
  the identical book. No LLM in the path = no slow/offline failure mode.

### R4 — ORPHAN risk / does the mechanic have a narrative home? It must.
A "book updates about you" gimmick with no story attachment is exactly the orphaned
mechanic the consistency law bans.
- **Verdict: NOT orphaned — it is the literal body of FACT 9, 12, 14, 15** (§4
  threads it). But it MUST move in lockstep: a keeper page may only exist for a
  keeper whose journal the group can also find (the corpus). Guard: never enrol to a
  keeper whose stone/journal isn't placed and reachable.

### R5 — Book length / Minecraft limits. LOW-MEDIUM.
Written books cap at 100 pages / 256 chars usable per page; `LecternFillBeat`
clamps to 1024 but MC truncates. A growing multi-player book could overflow.
- **Mitigation:** one page per enrolled player per keeper, short (the keeper
  register is terse by law). Cap the Hold-Book at the active-player count + a few
  Archivist frontispiece pages — well under 100. If it ever approaches the cap,
  spill the oldest Archivist preface, never a keeper page.

### SCALE-DOWN CALL (keep-scaled).
**CUT for the vertical slice:** the M4 *first-person keeper hand* pages and the
per-keeper voice-key banks for all six keepers. **KEEP for the slice:** the
Hold-Book existing + the M3 *keeper-column heading* move for the **two** cleanest,
highest-precision signals (Vaun/hoarding and Sella/wandering — both backed by
unambiguous numeric signals). The first-person hands and the remaining four keepers
are P1 arc-spine, authored once the slice proves the precision floor holds on a real
playtest. Shipping all six first-person voices before a playtest risks a wrong page
on camera with no chance to tune the floor.

---

## 3. DE-SLOP TEST — exemplar pages, in-voice, cold

**Archivist column-heading page (M3), filing a living hoarder under Vaun.** Plain,
ledgerlike, names the measured number, no emotion, no bow:
> the one called {name} has {oresMined} of the deep and has set back none of the
> first of it. this was Vaun's keeping also. it is set down under his name.

**Vaun's own hand (M4), writing the living player** — inventories only, never
feelings (his locked voice, §6.8):
> i had three of each. i counted them in the dark. {name} counts too, and sets back
> nothing, and calls the counting keeping. i know the column he is in. i ruled it.

**Sella's own hand (M4), the wanderer** — reflection that folds back, gives the face
back wrong (her locked voice):
> the far water gives {name} a face back and it is not the one they went in with.
> the last marker is not the last. {name} walked past it too. count again at the shore.

**The final page (M5), the rite** — the verb is *kept*, never reward; stops short of
FACT 15:
> the record does not close. {name} is set down, in the hand that set down the rest.
> there is a row under each of the six now with a living name in it. the count goes on.

*(All four: no banned phrases — no "testament", no named emotion, no thematic bow,
no three-adjective lists, no em-dash drama. Concrete number slots. The iceberg: none
says "you are becoming a keeper" — the filing does.)*

---

## 4. THREAD IT — where this lives so it is not an orphan

### Canon FACTs touched / strengthened (canon-spine §3)
- **Adds a new mechanical home for FACT 9** ("the first hauntings were a keeper's
  fate re-enacted at the group") — currently the **only spine fact with no document
  home (LORE-BIBLE TODO-3)**. The keeper-column page *names a living habit and ties
  it to a named keeper's fate*, which is exactly F9's required delivery. **This idea
  closes TODO-3 with a real artifact, not just dialogue.**
- **Strengthens FACT 5** (fate matches custom) — the page is the cross-reference made
  literal.
- **Bodies FACT 12** (kept, not departed) — the keeper hands are *still writing*,
  proving they did not depart.
- **Bodies FACT 14** (the record receives/keeps you) and is the felt-delivery vehicle
  for the **SEALED FACT 15** — the book having written the living in beneath the dead
  is the accumulation that lands 15 without a sentence stating it (§6.2).
- **Touches FACT 1** (list of the living by name) — the Hold-Book is FACT 1's body.
- **New INV / FACT-adjacent rule to record in the spine:** *the keeper-enrolment
  tally is a neutral colorant; it never elects a keeper-bearer and never gates the
  ending* (mirrors the existing Whisper-tally rule, §6.3).

### Found-documents that already foreshadow it (no new docs needed to seed)
- **D01 `the-record-opens`** — already names the living and buries "it does not close
  at the rite… it [...] you" (FACT 14). The Hold-Book IS D01, now growing. ✔ plant.
- **The journals corpus** (`journals-vaun-mara-sella.md`, `journals-orin-brann-iss.md`)
  — the past-tense voices the living pages will be written *in*. Vaun's "i counted them
  in the dark / three of each", Sella's "count again at the shore", Brann's margin "he
  is in the count now, on the side that does the counting" — these are the exact
  cadences the new pages reuse. Brann's "joined the watching" margin is the cleanest
  in-corpus foreshadow of this whole mechanic.
- **D04 `observed-warned-left-at-threshold`** — the Archivist's living-vs-dead column
  ladder; the Hold-Book's M3 filing is the same hand.

### NPC / Watcher voice lines that carry it (`voice.ts`)
- New `voice.keeperPageVaun(name, ore)`, `keeperPageSella(name)`, … one per keeper
  for the page bodies (kept in `voice.ts` per INV-1, never inlined).
- New `voice.keeperEnrolled()` — an optional Discord line when a page is added:
  *"a row is filled. the hand that fills it is not mine."* (cross-surface truth: the
  Discord surface and the in-world book never contradict; same register).
- Reuse `voice.summons()` / the rite lines unchanged at M5.

### Ciphers / puzzles that express it (reuse the 11 built ciphers)
The keeper pages are partly **enciphered in that keeper's own cipher** (canon §1 maps
each keeper to a cipher), so reading the page about yourself requires the literacy you
earned from that keeper's stone — the "it knows me" and "I had to learn its alphabet to
read it" land together:
- **Vaun page → caesar** (his cipher; "everything held back by a fixed amount").
- **Sella page → atbash / mirror** (read faced to the water).
- **Mara page → bookCipher** (page/line/word against the lectern shelf).
- **Orin page → substitution.**
- **Brann page → polybius** or beacon-sequence flavor (night-read).
- **Iss page → vigenère**, key = the living player's *own name* (the inversion: he
  keyed his to his own name; yours is keyed to yours — the trap turned on the reader).
No new cipher needed; all six are in the built set (caesar, atbash, vigenere,
substitution, bookCipher, polybius).

### Beats / listeners / tables / seeds / sites / voice keys (real symbols)
- **Beat classes (reused, no new Java needed for the slice):** `LecternFillBeat`
  (the growing Hold-Book — `place_if_missing` for M1, re-fire with longer `pages[]`
  after), `BookAppearsBeat` (optional: drop a personal keeper-page book into a
  player's ender chest for the M4 first-person hand — `dest: "ender_chest"`).
- **Showrunner (new TS):** `discord/src/showrunner/keeper-record.ts` (pure
  `decideKeeperEnrolment`) + `keeper-record.run.ts` (I/O), `keeper-record.selftest.ts`.
  Wire `runKeeperRecordPass()` into `run.ts` beside `runCustomsPass()`.
- **repo.ts:** new `readSignalProjection()` (sibling of `readCustomViolations()`)
  projecting the signal columns the enrolment needs; new `KeeperEnrolment` interface.
- **State table:** `showrunner_state.keeper_record` — per-(groupKey,keeper)
  high-water marks (idempotency), exactly like `reported_customs`.
- **voice.ts:** `keeperPage*` family + `keeperEnrolled`; extend an
  `OracleVoiceKey`-style `KeeperVoiceKey` union so a payload names a key, never English.
- **sites.yml:** reuse `stone_of_reckoning` (Hold-Book lectern anchor — already
  enabled), the six `stone_{keeper}` answer-sites (literacy gates for reading the
  enciphered pages), `the_far_water` (Sella read), `unbroken_light` (the M5 final page).
  No new site required.
- **Migration:** one column add (`keeper_record jsonb` on `showrunner_state`), in the
  next `00xx_*.sql`.

---

## 5. PLANT THE PAYOFF — "oh, that is what that was for"

- **PLANT (Movement I, inert/ambiguous).** The base Hold-Book appears (D01) and, from
  the first session, records the living group's mundane habits in the flat Archivist
  voice: *"the one called {name} goes down alone."* It reads as flavor / a quaint
  "the server is logging us" toy. Crucially it uses the **same ledger column form**
  (honored / blank give-back) that Vaun's historical tally uses — but the group has
  not yet read Vaun's tally, so the form means nothing. The journals (corpus), met in
  M2, are filed in the group's mind as *history*. Two unrelated documents.

- **AMBIGUITY HOLD (Movements II–III).** The Hold-Book keeps a column with a blank
  second field for each player. A sharp player may notice the form matches Vaun's
  tally (`counted-them-in-the-dark`, whose "last column is ruled across and empty").
  Still deniable — could be coincidence of bookkeeping style.

- **PAYOFF (Movement IV).** A keeper's own hand writes the living player, AND the
  re-read of the corpus reveals the journals were never closed — the same column,
  the same blank give-back field, the same hand, now with **the living player's name
  in the row**. The "logging toy" from M1 re-reads as the first rows of the keepers'
  ledger. *Oh — the base log was the record. The history and the log were one book.
  It was filing us into Vaun's empty column the whole time.* This re-reads Brann's
  margin ("he is in the count now, on the side that does the counting") and Mara's
  "we are not counting the same number" as having been about the reader all along.

- **No plant without payoff / no payoff without plant:** the M1 column-form is inert
  until M2 gives Vaun's tally to compare it to and M4 fills the blank field with a
  living name. The payoff is impossible without the M1 plant existing first.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Element | Movement | Priority |
|---|---|---|
| Hold-Book exists, Archivist living-habit pages (D01 growing) | M1 | **P0** vertical-slice |
| M3 keeper-**column heading** move, 2 keepers (Vaun/hoard, Sella/wander) | M3 | **P0** vertical-slice |
| `decideKeeperEnrolment` precision floor + selftest | M1→M3 | **P0** |
| `keeper-record.ts`/`.run.ts`, `readSignalProjection`, state column | M1 | **P0** |
| Enciphered pages (read via the keeper's own cipher/literacy) | M3 | **P1** arc-spine |
| M4 first-person **keeper hands**, all six keepers, `keeperPage*` banks | M4 | **P1** arc-spine |
| Iss page keyed to the living reader's own name (vigenère inversion) | M4 | **P2** depth |
| M5 final rite page (writes the group in as keepers) | M5 | **P1** arc-spine |
| `book_appears` personal keeper-book to ender chest | M4 | **P2** depth |

**Depends on:** the `SignalSnapshot` tracker + `custom_compliance` writer (live);
`LecternFillBeat` (live); the showrunner cron + beat_queue (live); the corpus
journals + keeper-stones being placed/reachable (the lockstep guard); the literacy
gates (`rune_rosetta`, keeper-stone ciphers) for the enciphered-page tier.

**Depended on by:** FACT 9's document home (closes LORE-BIBLE TODO-3); the felt
delivery of FACT 15 at M5 (it is one of the seven accumulating foreshadows — the
heaviest, because it is the only one with the living players' own names in it).

**Verdict: KEEP-SCALED.** Ship the Hold-Book + M3 two-keeper column-move in the
vertical slice (P0); gate the six first-person keeper hands behind a playtest that
confirms the precision floor never files a wrong page on camera.
