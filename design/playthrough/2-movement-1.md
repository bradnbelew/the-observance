# The Observance — PLAYTHROUGH 2: MOVEMENT I (Establishment) — the literal shooting-script

> **What this is.** The ordered, literal "what is there" for Movement I — Act 1, ~Days 1–5
> (`WEB-MASTER §1.M1`), including the pre-arc on-ramp **M0-remote** (the cursed map), which is where
> Establishment actually begins. NOT a description of the content — the content itself, quoted from the
> REAL repo artifacts. Every quoted line is sourced; every not-yet-built thing is marked **[GAP — TO
> BUILD]** with the exact file/owner that will carry it. Where an artifact and a design doc disagree, the
> design doc (`WEB-MASTER` / `INTEGRATION-V2`) wins and the seam is flagged.
>
> **Sources of truth quoted below:** `arc/lore/documents/*.md`, `discord/supabase/seeds/puzzles_seed.sql`,
> `discord/src/voice.ts`, `plugin/src/main/resources/sites.yml`, `design/CURSED-MAP-SITE.md`,
> `design/prologue/PROLOGUE-VIGNETTE.md`, `design/content/npc-dialogue.md`, `design/structures.md`,
> `WEB-MASTER.md §0/§1.M0/§1.M1/§9`, `INTEGRATION-V2.md §A14/§B4/§A3`.
>
> **The Movement-I drama law (`WEB-MASTER §1.M1`):** drama budget LOW. The single calibrated-loud beat is
> the Cold-Start Prologue (the frame-break). It rises once, then drops to baseline and never rises again in
> M1. Everything else is inert plants, computed-but-mute engines, and the literacy on-ramp.

---

## DAY 0 (pre-arc) — M0-REMOTE: THE CURSED MAP (the on-ramp / lure / cold-open)

> Owner `cursed-map-frame` (A14). Mints nothing. This is where a remote/async group's Establishment
> actually starts (`WEB-MASTER §1.M0-remote`). It is caused by a player, not the bot (TINAG preserved).

### 0.1 — The lure page (the public Record's downloads face)
- **WHAT IT IS / WHERE:** the route `record/[slug]/page.tsx` at slug **`the-record-keeps`** — the same cold
  dead-archive shell as `/record/the-record`, with one static downloads block appended. **BUILT this pass**
  per `CURSED-MAP-SITE.md §0/§4` (slug-validate + downloads block + in-voice 404 shipped; `tsc`/`eslint`
  clean on the route file; dev server verified `the-record-keeps` → 200 with the block). Reads only
  `v_record`, `noindex`, no client JS.
- **The recovered-file entry (verbatim, `CURSED-MAP-SITE.md §2a`):**
  > a hold, kept and left. one walk through it remains. the rest of the record is kept elsewhere.
  > what is downloaded is only the part that fit in a file.
  - download link: label is the filename only, **`the-hold.zip`**; `href` → the vignette asset
    (`download` set, `rel="noopener"`). The `.zip` itself is **[GAP — GO-LIVE asset]** (`PROLOGUE-VIGNETTE.md
    §7`; the world+datapack must be built in a 1.21.x client and hosted).
- **The provenance line (Mara's hand, verbatim last four lines of `the-copy-i-kept.md`, signed):**
  > i copied it as it was given, page for page, and set the copy where fire and water do not reach.
  > i did not keep the seventh. i was not the hand that decides what is kept. — m.kept
  - WHO it targets: the dead uploader `m.kept` is a canon keeper's hand (Mara, the Reader), **never** the
    Seventh. Plant **ledger #26** — "a person who left a file" → M4 "no person uploaded it; the record kept
    it in a dead hand."
- **The README "lie" (verbatim, `CURSED-MAP-SITE.md §2b`):**
  > the-hold.zip — a small offline map. single player. no mods. about fifteen minutes.
  > it does not connect to anything. play it through to the end and it will tell you where the rest is kept.
  - Plant **ledger #25** — load-bearing line is **"it does not connect to anything"** (true of the *map*;
    the *server* connects). Misread as comfort; re-reads at M4.
- **The counter (verbatim render, `CURSED-MAP-SITE.md §2c`):**
  ```
  kept: 6
  ████████        ← the struck seventh row (REDACTED_GLYPH, reused from the projection)
  ```
  - **Static authored `6`**, NOT a live counter (a live count would drift off 6 the moment they download).
    Canon home `six-were-kept-before-you.md`. Plant **ledger #24** (the heaviest M0 plant): M2-soft (*six
    downloads, six keepers* — pattern-match only) → M4-hard (six prior groups it kept the same way) →
    M5-felt (`recordReceives()` fills the struck row with the present hands).
- **DIRECTOR ACTION:** none at runtime — the page is static-per-build. Go-live: bake the real `the-hold.zip`
  `href` + host the asset.

### 0.2 — The vignette (the downloadable prop) — a ~6-room single-player walk
> `PROLOGUE-VIGNETTE.md`. **Form: a datapack + single-player world `.zip` (`the-hold.zip`)**, vanilla Java
> 21/1.21.x, no mods, no resource pack required. Adventure mode; gamerules locked (`doDaylightCycle false`,
> `doMobSpawning false`, etc.). **[GAP — GO-LIVE asset]**: the world+datapack are a spec here, not a built
> `.zip` — the `tick.mcfunction` region coords are coordinate-specific and must be filled against built
> geometry. The 1-room form ships first; the full 6-room hold is P1→P2.

Room-by-room, the **verbatim** in-vignette text (Archivist register, `tellraw @s` italic gray or book/sign):

- **BEAT 0 — room 0, antechamber (spawn).** Sign on the iron door:
  > a hold was kept here. read what is left, and go on.
  - Plant **ledger #4 (rehearsed)**: one lantern burning where no one lit it (kept-light motif).
- **BEAT 1 — room 1, the record room.** Lectern book = the **public half of `the-record-opens`** (FACT
  1/2 ONLY):
  > the record opens. it was open before. // the living are written here, each by the name they answer
  > to, and against each name a column is left open. // the watching has already watched. no one is told.
  > that is the way of it.
  - On first entry, one `tellraw @s`:
    > *the count begins. the living are written by name.*
- **room 2 — the doused hearth** (basalt fireplace, a campfire that is OUT; player cannot relight it):
  > *a fire was kept here. it is out. it was not put out by any hand that is still here.*
  - Plant **ledger #27** (doused hearth = the Seventh's *unnamed* texture).
- **room 3 — the wall of marks** (six carved marks + a **seventh scraped blank**):
  > *six are marked. there is a seventh place, and no mark in it. the record will not keep the seventh.*
  - Plant **ledger #2 + #27** (the FACT-2 miscount, inert here).
- **BEAT 2 — the one "it noticed" tell (conduct, deniable, NOT personalization).** Read at the closing
  sign in room 5 off the `passed_seventh` scoreboard flag (default 1; flips to 0 only if the player lingers
  ~3s at the scraped slot). Two authored variants — both state the *conduct*, never the player:
  > *the seventh was passed. it has been noted.*   (if `passed_seventh = 1`)
  > *the seventh was stopped for. it has been noted.*   (if `passed_seventh = 0`)
  - True by construction in single-player (no precision risk). **NOTE:** this local flag is **never
    transmitted to the server** — it exists as rehearsal only; the conduct-callback frame-break **FB-2(i) is
    CUT from the default** (S5).
- **BEAT 3 — room 5, the hand-off.** Closing lectern book (keeper register, verbatim):
  > the rest is not kept in this hold. it is kept where the others are. bring them, and come to the place
  > named below. the record is open there. it was open before you.
  - Beside it, a sign/item-frame map carrying **two things in plain text**: (1) the **server address** (no
    decode required to act); (2) the **rune string** that decodes later in-server to **`the-record-keeps`**
    — the SAME founder-margin key (`kept-in-more-than-one-place`, ledger #11), a recognition token, not a
    day-zero gate. Plant **ledger #11** (reused).
  - Final closing line (the one plain ignition action):
    > *(small, beneath the address)* say one word in the place named below, when you are all in. say *kept*.
  - **The map ends. No frame-break has happened. The map behaved like a map.** (Dread banked for the server.)
- **DIRECTOR ACTION:** none — single-player, self-contained, gates nothing. A friend who refuses the
  download joins cold and gets the in-server Prologue unchanged.

### 0.3 — Gate M0-remote → M1
- **TRIGGER → EFFECT:** the group gathers and a human posts **`kept`** (or anything) in `#the-record`; the
  **BUILT `messageCreate` ignition detector** (`prologue.ts` / `decidePrologue`) flips `prologue_ignited`.
  A map-playing group arrives **pre-ignited at the door**; the frame-break (Day 1, §1.4) is their first
  server beat. The bare detector cannot distinguish a map-arrival from a cold join — and does not need to
  (the count-callback is true for everyone). The keener `from_map` flag is **P2/optional [GAP — not built]**.

---

## DAY 1 — THE NOTICE OPENS (ignition + the first report + the frame-break)

> The one calibrated-loud beat of M1. Everything before/after it on Day 1 is cold and patient.

### 1.1 — The base lectern fills: the Hold-Book appears (M1 face)
- **WHAT IT IS / WHERE:** `LecternFillBeat` writes the Hold-Book onto `stone_of_reckoning`'s companion
  lectern (`WEB-MASTER §4`; one book, two faces). The M1 face is the **Archivist's flat living-habit rows**
  (the keeper-record) AND, on its last page, **Brann's down-count docket** `lamps kept: [N]`.
- **The book's opening corpus = `the-record-opens.md` (verbatim core, FACT 1 + FACT 2):**
  > so the count begins. the living are written here, each by the name they answer to, and against each
  > name a column is left open. nothing is owed yet. but the column is open, and an open column is a thing
  > that fills.
  > ...
  > the record only knows what was done, and writes it true.
  - The buried last lines plant **FACT 14** + the seventh:
  > the record does not close at the rite. when the keeping is done, it does not let the writing go.
  > ...
  > six are named in full, and there is a seventh mark the record will not [...]
- **WHO / gating:** group-facing, no oracle gate. The down-count `lamps kept: [N]` reads as one of Brann's
  ordinary tallies (a doom-clock — the **bait** for Iss's M4 lie). Plants **ledger #5** (the blank give-back
  column) + **ledger #6** (the down-count muster).
- **DIRECTOR ACTION:** the M1 plant ships as a **static book** until the count is bound to real muster state
  (`INTEGRATION-V2 §A3` "ship only the static M1 plant until the count is bound to real state"). The
  `keeper_record.ts` producer + live count are **[GAP — TO BUILD]** (`INTEGRATION-V2 §A3` build tasks).
- **SEED ROW (the transcribe-acknowledgement, `puzzles_seed.sql` `m1-record-opens`, verbatim fragment):**
  > the record was open before you found it. it counts the living by name, and it grades them by laws no
  > one was told. it does not close at the rite. a seventh mark the record will not keep.
  - `outcome_type: lore`, voice `oracleLore` (passthrough). Accepted answers include
    `the record counts the living by name`. Gates nothing (a told secret).

### 1.2 — Ignition (the detected signal)
- **TRIGGER → EFFECT:** `IgnitionListener` detects EITHER the base lectern read OR a human post in
  `#the-record` → flips `arc_state.flags.prologue_ignited` → fires the one-shot ack
  **`voice.recordOpened()`** (BUILT, verbatim):
  > ▒  the record is open. it was open before you.
  - For a map-arrival group, "it was open before you" reads as a callback to the hold they just walked.
- **The curatorial clue-drip is SUPPRESSED until ignition** (`decide.ts` guard — `INTEGRATION-V2 §B4`).
- **DIRECTOR ACTION:** none (auto). The `acked` one-shot guard prevents re-fire.

### 1.3 — The first report (re-staged as a discoverable anomaly — the Cold-Start Prologue)
- **WHAT IT IS / WHERE:** the first report is retargeted to the **base hot-cell** (`first_report_lectern_01`
  retargeted; `sites.yml`/`INTEGRATION-V2 §B4`), paired with **one lit marker that was not there before**
  (`first_marker_01`, carved with one rune-glyph, illegible until the Rosetta).
- **The report body — two authored forms, precision-gated (`voice.ts`, BUILT, verbatim):**
  - IFF one player's measured signal is overwhelming → **`prologueNamed(name, custom)`**:
    > ▒  a thing was set out in your place that was not there before. it carries a mark you cannot yet
    > read. it knows the one called {name} has not {custom}. it was noted before you knew there was a
    > record to note it.
  - ELSE the un-named FACT-1 fallback → **`prologueUnnamed()`**:
    > ▒  a thing was set out in your place that was not there before. it carries a mark you cannot yet
    > read. it has been keeping a count of you. the count began before you found the mark.
  - **PRECISION LAW:** never a guessed callout — the named form fires only on a measured overwhelming
    single signal; default is the un-named fallback.
- **The lit marker's glyph** is part of the **one coordinated M1 teaching-stone layout** at `first_marker_01`
  (with the six UNKEPT maker's-marks and the `kept here before you` plant — `sites.yml` `first_marker_01`,
  coherence P2-4). Plant **ledger #4**: re-read in M2 via a1z26/atbash → `KEPT`/`BEGUN` ("the first notice
  was a record entry all along").
- **DIRECTOR ACTION:** none at runtime (showrunner-staged). The `first_marker_01` build + glyph carving is a
  **[GAP — GO-LIVE]** (`structures.md` — build in-game, export `.schem`, fill coords; the plugin silently
  skips the unplaced site until then).

### 1.4 — The frame-break (the central scare — the category violation)
- **TRIGGER → EFFECT:** after ignition, gated on a measured signal, the server fires **ONE** precision-gated
  line a map could not say. **Default body = the count-callback ONLY.**
- **WHAT IT IS (verbatim, canon home `six-were-kept-before-you.md` / `PROLOGUE-VIGNETTE.md §5`):**
  > six were kept before you, and the count of them is on the page you found. a file is not kept. hands
  > are kept. you are not a file here.
- **WHERE / surface:** `#the-record` (server), via `voice.recordFrameBreak()`.
- **WHO / gating:** group-facing, names no one, true regardless of whether they played the map (S7).
  Default-safe: no clean measured signal → fire **only** `recordOpened()` and let the next in-server "it
  knows you" beat carry it. The map-conduct overload **FB-2(i) is CUT** (the offline flag never reaches the
  server — a map-side assertion would be a precision lie, INV-16).
- **DIRECTOR ACTION:** the on-arrival beat emits `recordFrameBreak()` once, idempotent on the BUILT `acked`
  guard; no new flag/table.
- **[GAP — TO BUILD]: `voice.recordFrameBreak()` does not yet exist in `discord/src/voice.ts`** (grep
  confirms `recordOpened`/`recordElsewhere`/`recordReceives` only — the `recordFrameBreak` key is specified
  verbatim in `PROLOGUE-VIGNETTE.md §5/§7` and `CURSED-MAP-SITE.md §6` but not yet inserted into the
  `OracleVoiceKey` union or the `voice` object). TS-VOICE owes this ONE key.

### 1.5 — The knob drops (FB-3)
- **TRIGGER → EFFECT:** after the frame-break lands, loudness returns to baseline and never rises again in
  M1 (`PROLOGUE-VIGNETTE.md §5` FB-3 / `WEB-MASTER §1.M1`). The map was the loud thing; the server is
  patient.

---

## DAYS 1–5 — THE FIELD STANDING OPEN (the non-linear doors + the inert plants)

> From ignition onward, many doors sit open at once (`WEB-MASTER §1.M1`). None is a countable step. Below,
> in causal order of when a group can first touch them.

### 2.1 — The server-icon rune ring (the master rabbit hole)
- **WHAT IT IS / WHERE:** the server icon is a ring of six runes — sits **unremarked** (no Discord post, no
  in-game callout). It is in-road **(a)** to the literacy gate.
- **WHO / gating:** anyone who notices the icon metadata; ungated.
- **DIRECTOR ACTION:** none (the icon is a static asset — **[GAP — GO-LIVE asset]**: the rune-ring server
  icon must be authored).

### 2.2 — The literacy gate (`rosetta_known`) — TWO genuinely-different in-roads
This is the one true M1→M2 gate, and the fairness fix (arg-craft F1) lives here: two doors that are
genuinely different modalities, not "two doors that are one door."

- **In-road A — the a1z26 teaching-rung (number puzzle, no runes).** SEED `a1z26-tick-stave`
  (`puzzles_seed.sql`, verbatim): title `count the staves`; accepted answers
  `learn them as we learned them` / `count the staves then read` / `read them as we read them`;
  `outcome_type: main_beat` → `set_flags {rosetta_known: true}` + an `advancement_toast`
  (`observance:the_ring_is_whole`) at `first_marker_01`. A tick-stave (1..26 → letters), read at
  `first_marker_01` / `stone_of_reckoning`. **This kills the "two doors that are one door" lie and
  un-orphans a1z26.**
- **In-road B — the rune-ring metadata leap.** SEED `rosetta-ring` (`puzzles_seed.sql`): title
  `learn them as we learned them`; `main_beat` → same `rosetta_known` flag + same toast. Accepted answer:
  > bow offering kept light deep line unspoken sacred beast
- **In-road C (optional) — the partial-key founder margin.** `learn-them-as-we-learned-them.md` (BUILT,
  clue_bearing), verbatim instruction:
  > read the ring sunwise from the topmost mark. six marks, and they hold the six in order:
  > first, the Bow — then the Offering — then the Kept Light — then the Deep Line — then the Ward —
  > and last, the Covering.
  - and the later-hand margin that plants FACT 3/4:
  > they are not ours. we did not raise them. we were taught, and the teaching is older than the first of
  > us. ... read sunwise. begin at the top.
- **Yields:** reads every later carving. **WHO / gating:** anyone literate via A, B, or C.
- **DIRECTOR ACTION:** none at runtime. **[SEAM — flagged, not invented]:** the BUILT founder doc
  `learn-them-as-we-learned-them.md` names the ring as **Bow / Offering / Kept-Light / Deep-Line / Ward /
  Covering**, but the seeded `rosetta-ring` accepted answer replaced `ward`/`covering` with
  **`unspoken sacred beast`** (a documented audit fix — `ward`/`covering` were orphan ways with no
  detection/keeper/thread-tag; the rune-ring is where `the_unspoken` + `the_sacred_beast` are otherwise
  taught). The seed comment flags the GO-LIVE: *"the rune ring structure must carve these, not
  ward/covering."* The in-world doc + the rune-ring structure carving are a **lagging surface** that must be
  reconciled to the seed at go-live (or the doc's later-hand margin must correct ward/covering the way it
  already corrects "learned them as we learned them"). **[GAP — reconciliation owed at go-live.]**

### 2.3 — The digit-literacy gate (`reckoning_known`)
- **WHAT IT IS / WHERE:** SEED `reckoning-rosetta` (`puzzles_seed.sql`, in the backlog block): the Stone of
  Reckoning teaches the digit-glyphs + sign-marks (`-`/`,`). `main_beat` → `set_flags {reckoning_known:
  true}` at `stone_of_reckoning`. Makes coordinates into *places* (INV-14 in-world fairness).
- **WHO / gating:** the rune-ring + a1z26 teach LETTERS; this teaches DIGITS. Coord-bearing rows stay
  inactive until `stone_of_reckoning` is placed.
- **DIRECTOR ACTION:** build `stone_of_reckoning` in-game (**[GAP — GO-LIVE]**, `structures.md`); the digit
  Rosetta is a non-cipher row TS-FORGE must add to `NON_CIPHER_KEYS`.

### 2.4 — m1-named-habit (the terminal-dread dead-end)
- **WHAT IT IS / WHERE:** SEED `m1-named-habit` (`puzzles_seed.sql`), a `dead_end` — a TRUE reading that
  opens nothing. Accepted answers include `it named my habit before i knew it was a custom` /
  `i was measured before i was told`.
- **WHAT IT SAYS:** voice `oracleDeadEnd('name')` (BUILT, verbatim):
  > a true name. it keeps no door. some things are only true.
- **WHO / gating:** anyone who reads their own measured habit in the report and submits it. Carries no
  `next_puzzle_key` (never blocks progress). The teeth belong to the liar, never the Watcher.
- **DIRECTOR ACTION:** none.

### 2.5 — The inert plants seeded across M1 (all dormant)
Each is placed in M1, legible/inert, with its payoff staggered later (`WEB-MASTER §9` ledger). In causal
discovery order:

- **`kept here before you` rune line** at the M1 teaching stone (`name-where-never-been` plant, FACT 16).
  The exact string `kept here before you` is a carved line at `first_marker_01`; mechanically it is also an
  accepted answer on the M4 `true-walk-arrive` row (`puzzles_seed.sql`), so the M1 carving and the M4 walk
  endpoint rhyme. Inert reading: a dead keeper's epitaph. Plant **ledger #9** (the place-filing grammar;
  "before you" was never about strangers).
- **The six `UNKEPT` maker's-mark glyphs** — one per (eventual) keeper-stone, also previewed in the M1
  teaching-stone layout at `first_marker_01`. Inert: each keeper's grief-mark. Plant **ledger #8** (read in
  **fall-order** = `UNKEPT`; legible M4 via `meta-unkept`). **[GAP — GO-LIVE]:** the glyph carvings need the
  in-game build.
- **`we-cut-the-names-before-the-keeping.md`** (BUILT founder margin, FACT 13b plant), verbatim core:
  > do not read the date as the day they die. read it as the day they are **kept.** the founders did not
  > cut a death into the stone. they cut an appointment.
  - and the later hand: *"there is a stone near the threshold cut this way — a name on it, and a day not
    yet come. the keepers call it the death-clock..."* Inert: eerie ritual order. Plant **ledger #10**
    (becoming a keeper IS the keeping → IV→V).
- **`kept-in-more-than-one-place.md`** (BUILT founder margin, clue_bearing), verbatim:
  > **the record is kept in more than one place, against the loss of the first.**
  > ...
  > *(the keyed line, in the old script, set apart:)* [ decodes to: **the-record-keeps** ]
  - Inert: careful archivists. Plant **ledger #11** (the decoded phrase is a URL path off-world; pays off
    "at the click").
- **Vaun's `you do not keep the first thing`** (BUILT corpus, `journals-vaun-mara-sella.md`), verbatim:
  > Whatever the deep gives up first — first ore, first water, first warmth — you carry back to the cairn
  > and give it to the deep again. You do not keep the first thing.
  - Inert: one custom among many. Plant **ledger #13** (the Sacred Beast WAS the first thing — the M3/M5
    fork). Read alongside the child's-hand corpus line: *"iss says you cannot keep the first thing but vaun
    keeps everything. i counted vaun's lamps."*
- **The single Sacred Beast established + the prior-keeper count fragment** (`herd-conversion` plant). The
  count fragment is **`nine-grey-one-white.md`** (BUILT herder's tally), verbatim:
  > first winter:    nine grey, one white. all accounted. all in the fold.
  > ... third winter:   eight grey, two white. ... i did not see it turn.
  - Inert: a farmer's note / atmosphere. Plant **ledger #14** (a timeline of the conversion happening to
    *them* at the same cadence). INV-13: only the one glowing Beast is tracked; the cosmetic Pale never glow.
- **Mara's `the cold square… i typed into the dark`** — `the-cold-square.md` (BUILT, book-cipher,
  clue_bearing), the assembled line:
  > foot to the square, hand to the cut, word to the dark, the three at once.
  - and Mara's plain note: *"i typed the word into the dark, alone, and nothing opened, because a thing
    done alone is one thing and the threshold counts three."* Inert: a sad keeper memory / decoration.
    Plant **ledger #15** (the three-hands coop-gate rite instructions; the glyph one of them stands on).
- **The offline-player report** — `brann-not-here-to-see-it-noted.md` (BUILT, FACT 9 plant). Surfaced
  in-Discord via `voice.offlineReportPlant(name)` (BUILT, verbatim):
  > the one called {name} is not here to see it noted. the record notes the not-here as plainly as the
  > here. it keeps the empty place at the table.
  - Inert: "the record watches you logged off." Plant **ledger #16** (the land had begun to *wear* him from
    the night he stopped coming → III→IV).
- **Mara's `closer count of the quick`** — `a-closer-count-of-the-quick.md` (BUILT) + SEED `difficulty-mara`
  (`puzzles_seed.sql`, movement 2 row but M1-legible corpus). Voice `oracleLore`, verbatim fragment:
  > i read that the record keeps a closer count of the quick. i called that a cruelty for a winter. i do
  > not call it that now, and i will not say what i call it.
  - Inert: a keeper muttering. Plant **ledger #12** (the land was grading their mastery the whole arc, FACT
    2b/9; de-slopped — the line does NOT name "mercy" or resolve its own meaning).
- **The coordinate number-pair near `stone_of_reckoning`** (`coords-to-real-place` plant). Inert: a tally.
  Plant **ledger #7** (a coordinate before anyone could read it — "the record keeps roads"). Carving is a
  **[GAP — GO-LIVE]** build.

### 2.6 — The engines that COMPUTE-BUT-STAY-MUTE in M1
- **The dynamic-difficulty engine** (`reckoning.ts` — `INTEGRATION-V2 §A10`) computes group cadence but is
  **mute** in M1 (it goes live in M2, FACT 2b). Its corpus plant (`difficulty-mara`, §2.5) is the only M1
  trace. **[GAP — TO BUILD]:** `reckoning.ts` pure module + selftest.
- **The herd single-Beast** is established (one glowing Sacred Beast tagged; cosmetic Pale not yet
  spreading). The `SacredAnimalBeat` spread mode is M2+. **[GAP — TO BUILD]:** the `SacredAnimalBeat`
  `mode:"spread"` + `pale_cosmetic` PDC.
- **The showrunner's first report/drip pass** authors the daily clue drip via `voice.drip(tone)` (BUILT,
  verbatim default `▒  something is set out where the marks are kept. read it, if you can.`) and conduct
  reports via `voice.reportObserved(name, days, custom)` (BUILT). Each personalized call has a
  deterministic fallback (the SPOF mitigation). **[GAP — TO BUILD]:** the showrunner `run.ts` authoring loop
  (`INTEGRATION-V2 §D1`).

### 2.7 — The Set-A surface NPCs (the human counterweight, available from Day 1)
> `design/content/npc-dialogue.md`. Contractions/capitals/named feelings are LEGAL here (the human
> counterweight to the Watcher's cold register). In M1 they sit warm/neutral; they plant the future
> contradictions. **BUILT as a dialogue spec; the in-game NPC bodies (Citizens2/ZNPCsPlus) are
> [GAP — GO-LIVE].**

- **Aro — the rumor-broker who lies** (`npc_key: aro`). M1 greet (verbatim `aro.greet.neutral`):
  > Ah — fresh boots. Sit, sit, you're letting the cold in. You want the way down, you want the right
  > person, and lucky you, here I am.
  - His `lie.cross` is the SAME lie Iss carved (the painted-line "step right over it") — it plants the M4
    catch. WHO: warm/specific/confident, half-wrong on purpose; Iss-adjacent (flips cold sticky on
    `iss_caught`, M4). In M1 he parrots the forged eighth law and the Unlit Deep falsehood (so the world
    later contradicts him).
- **Wenna — half-remembers the ways as folk-superstition** (`npc_key: wenna`). M1 rumor (verbatim
  `wenna.rumor.seven`):
  > Gran used to say there were seven somethings you had to mind down there. Seven. I only ever remember
  > six and I always forget a different one, isn't that the way...
  - and `wenna.rumor.name` (the unspoken-name folk-charm): *"You don't say the cold's name... 'You don't
    *name* it, Wenna.'"* WHO: garbled-truth; her folk-charm later resolves the Unlit Deep conjunction.
- **DIRECTOR ACTION:** none at runtime; the dialogue trees read the dossier (no new measurement).

### 2.8 — The first apparition of M1 (trigger → effect)
- **WHAT IT IS:** in M1 the apparition web is essentially silent — the loud beat was the frame-break, and
  the named apparitions (offline-skin, Keeper-NPCs, the Surface-Walker) are **M2+** (`WEB-MASTER §7`). The
  **only** M1 "apparition-adjacent" event is the **lit marker that was not there before** (§1.3) — a
  discovered, never-witnessed object, not a figure.
- **TRIGGER → EFFECT:** showrunner stages the lit `first_marker_01` out of line of sight (reveal discipline)
  → players discover it already present → the first report (`prologueNamed`/`prologueUnnamed`) fires. There
  is **no ambient figure** in M1 (the single-arbiter slot, INV-18, has nothing to claim yet).
- **[GAP — clarified, not invented]:** the generic "Watcher at the Edge" (Vaun-shape, M1–II per
  `WEB-MASTER §7`) *may* fire late-M1 on a measured `hoardedScore`/`soloMiningRatio` signal, but it defers
  to `apparitionClaim` (the spawn-bias conductor, **[GAP — TO BUILD]**, `INTEGRATION-V2 §D7`). In a clean
  M1 it stays unfired — precision over recall.

### 2.9 — Gate M1 → M2
- **TRIGGER → EFFECT:** **first report found + group-playtime threshold** (`WEB-MASTER §1.M1`). No countable
  step-ladder; the literacy gate (`rosetta_known`) opening the six-stone field is the felt transition into
  Movement II.

---

## DIRECTOR-CONSOLE / SETUP CHECKLIST FOR MOVEMENT I (in causal order)

1. **Go-live (client/asset, before any M1 session):** build + host `the-hold.zip` (vignette); author the
   rune-ring server icon; build `first_marker_01` (lit marker + glyph + UNKEPT marks + `kept here before
   you`), `stone_of_reckoning`, `rune_rosetta` in-game, export `.schem`, fill `sites.yml` coords
   (`structures.md` GO-LIVE). Until placed, the plugin silently skips each site.
2. **Bake** the real server address into the vignette room-5 hand-off; bake the `the-hold.zip` `href` into
   the lure page.
3. **Code owed before M1 ships (the [GAP] list):** `voice.recordFrameBreak()` (TS-VOICE, body verbatim
   §1.4); the showrunner `run.ts` drip/report loop + deterministic fallbacks (D1); the static→live Hold-Book
   (`keeper_record.ts`, ship static M1 plant first, A3); `reckoning.ts` (mute in M1, A10); the
   `SacredAnimalBeat` spread/`pale_cosmetic` (A12); the spawn-bias conductor + `apparitionClaim` (D7).
4. **At runtime:** ignition is auto (lectern read OR `#the-record` post). The director makes **no console
   click** in a clean M1 — every M1 beat is detector-driven or showrunner-staged. The director's only live
   role is the approval gate on any curatorial (pending) drip the showrunner proposes.
5. **Reconcile the rune-ring seam (go-live):** carve `unspoken` + `sacred beast` into the rune-ring
   structure (NOT ward/covering, per the seed), OR add a later-hand margin to
   `learn-them-as-we-learned-them.md` correcting ward/covering — the in-world doc currently lags the seed.

---

## SUMMARY (4 lines)

1. Movement I (Establishment) begins pre-arc at **M0-remote**: the BUILT lure page (`kept: 6` + struck-7 +
   `m.kept` provenance + the README "lie") and the spec'd-but-unbuilt vignette `.zip`, handing off via the
   BUILT `#the-record` ignition detector into a pre-ignited server arrival.
2. Day 1 is the only loud beat — ignition (`recordOpened()`) → the first report
   (`prologueNamed`/`prologueUnnamed`, precision-gated) + the lit marker → the frame-break count-callback
   ("six were kept before you… hands are kept") → the knob drops to baseline for the rest of M1.
3. Days 1–5 stand the non-linear field open: the two-in-road literacy gate (a1z26 rung + rune ring + founder
   margin), the digit Rosetta, the `m1-named-habit` dead-end, and ~11 inert plants (ledger #4–16) all
   legible/dormant, with the difficulty/herd/showrunner engines computing-but-mute and Aro/Wenna planting
   future contradictions.
4. The M1 apparition web is deliberately silent (the lit marker is the only discovered-never-witnessed
   object; no ambient figure); the gate to M2 is first-report-found + playtime threshold, felt as the
   literacy door opening the six-stone field.

**[GAP] marker count: 14** — vignette `.zip` asset; rune-ring server icon; `first_marker_01` build/glyphs;
`stone_of_reckoning` build; the six UNKEPT glyph carvings; the coordinate-pair carving; `recordFrameBreak()`
voice key (not yet in `voice.ts`); the `from_map` flag (P2, not built); live Hold-Book `keeper_record.ts`;
showrunner `run.ts` loop; `reckoning.ts` engine; `SacredAnimalBeat` spread/pale; spawn-bias conductor +
`apparitionClaim`; the rune-ring doc↔seed (ward/covering vs unspoken/sacred-beast) reconciliation.
