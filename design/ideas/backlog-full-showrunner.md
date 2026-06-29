# Backlog Treatment — The Between-Session Showrunner (the keystone async engine)

> MASTER-PLAN P1.8. Status going in: **far more built than the pitch implies.** The
> deterministic spine (`snapshot → decide → apply`), the difficulty grip
> (`reckoning.ts`), the customs→report bridge (`customs.ts`), the autonomy producers
> (grave / herd / forks / clock in `autonomy.run.ts`), the "record writes you in"
> scalpel-with-fallback (`keeper-record.ts`), the prologue gate (`prologue.ts`), the
> pending/approved split (`state.pending_drips`, `BeatStatus`), and the full
> scenario self-test all **compile today**. This treatment HONORS that and specifies
> only the genuinely-unbuilt remainder, reconciled against the new web.

---

## 0. What already exists (do NOT re-invent)

| Concern | File / symbol | State |
|---|---|---|
| Pure policy | `decide(Snapshot): Decision` (`decide.ts`) | DONE — stall auto-gift + cadence drip + story-shape ordering |
| Read / write halves | `buildSnapshot` (`snapshot.ts`), `applyDecision` (`apply.ts`) | DONE |
| Cron entrypoint | `run.ts` (one tick + exit, `--dry-run`) | DONE |
| Persistent state | `showrunner_state` jsonb (`state.ts`) — no migration | DONE |
| AUTO⇄CONFIRM split | `mode`, `staged`, `pending_drips`, `BeatStatus 'approved'|'pending'` | DONE |
| Difficulty dial (cadence + tone) | `reckon()` (`reckoning.ts`) + `computeAutonomyGates` | DONE |
| Customs report ladder | `decideCustomReports()` (`customs.ts`) observed→warned→left | DONE |
| Clue-card forge on drip | `forgeDripCard` (`clue-drip.ts`) | DONE |
| Record-writes-you-in scalpel | `keeper-record.ts` (authored key first, LLM optional slot) | DONE (policy) |
| Autonomy producers | grave / herd / forks / clock (`autonomy.run.ts`) | DONE |
| Self-tests gate build | `decide`/`customs`/`scenario`/`autonomy`.selftest.ts | DONE |

**The remainder is wiring + three new producers, not a rewrite.** See §1.

---

## 1. EXPOUND — the full engine, and the unbuilt remainder

The showrunner is a stateless cron that runs one tick and exits. Each tick is three
phases the existing `run.ts` already orders: **(a) compute gates** (difficulty grip
+ prologue), **(b) the pure spine** (`decide` → `apply`: gifts + curatorial drip),
**(c) the between-session producers** (customs bridge + autonomy passes). Everything
the LLM might touch sits behind a deterministic value that is already chosen before
the model is offered a constrained slot it may decline. If the model is slow, junk,
or absent, the authored value stands and the tick is byte-identical.

### The five genuinely-unbuilt pieces (the EXPOUND target)

**U1 — The cross-owner READERS (the inert-but-correct layer becomes live).**
`autonomy.run.ts` already degrades to no-op where a data source it needs isn't wired
(`readMastery` is a coarse group proxy; `keeper-record` / `name-where` / `offline-skin`
/ `fate` "slot in behind those reads with no change here"). The remainder is the small
read functions, not the policies:
- `readDossiers()` → per-player measured dominant-signal rows for `keeper-record` +
  the report scalpel (precision gate: a player with no dominant signal is read as FLAT,
  enrolled to no one).
- `readActiveRoster(windowMs)` → active-only player set, replacing the stub
  `pickGraveSubject()` "earliest-linked" pick and feeding the difficulty mastery and
  the Accepting satisfiability check (gate on ACTIVE players only — collective law).
- `readVisitedCells()` → for `name-where-never-been` (carve a glyph at a cell the
  player demonstrably has NOT walked — a measured absence, never a guess).
- `readComplianceSpread()` → per-custom honored/violated already exists as
  `readCustomViolations`; the remainder is the per-player dossier join the report
  scalpel reads so it personalizes ONLY on a measured row.

**U2 — The report SCALPEL (the rare LLM call) + its deterministic voice fallback.**
Today the daily report is fully deterministic: `decideCustomReports` emits
`voice.reportObserved` / `reportEscalated` lines. The remainder is the OPTIONAL
scalpel that, on an *overwhelming* single measured signal, may substitute ONE
constrained phrase in a report — exactly the shape `keeper-record.ts` already
defines: an authored voice key is chosen first; the LLM is offered a narrow slot
(a clause that completes a fixed template), its output is validated byte-by-byte
against a `[a-z0-9 ,.]`-only grammar and a banned-token list, and on any failure the
authored key stands. The scalpel **never invents a transgression** — it may only
re-voice a row the tracker measured. This is `reports.ts` (new): `decideReport(input)`
pure + `authorReportPhrase(dossier, fallbackKey): Promise<string>` injected, deterministic-first.

**U3 — Stone-difficulty + herring-density dial (the third reckoning lever).**
`reckon()` returns `cadenceMult` + `tone` today; the design (`reckoning.ts` header,
cipher-web §2.3) calls for a THIRD lever: **herring density** — a confident group
meets one more staged `dead_end`, a stumbling group meets fewer. The remainder:
`reckon()` returns `herringBias: -1 | 0 | +1`, and `apply.ts` flips a *staged*
`dead_end` puzzle row `active`/`inactive` (Discord-side, no MC write) by that bias,
capped + monotone-per-movement. INV-15 still holds: it never touches whisper_budgets.

**U4 — The Liar swap (one-key-two-doors re-walk; P1.11).** The showrunner's
flag-gated swap of D10's effective outcome on `flags.iss_caught`. Authored as TWO
real `puzzles` rows (`clue_vigenere_iss_warm` → dead-shrine `dead_end`;
`clue_vigenere_iss_name` → `main_beat` `set_flags{iss_caught}` + a
`private_message` `iss.dialogue.turns_cold` beat). The showrunner reads
`flags.iss_caught` each tick and, in CONFIRM, enqueues a `pending` curatorial beat
that flips which row is `active`; the AUTO/asleep fallback is an authored flag-gated
duplicate row (no showrunner needed). New: `forks.ts` already commits leaf flags
first-writer-wins — the Liar swap is a `liar.ts` producer that reads `iss_caught`
and emits the row-flip beat.

**U5 — Bestiary spawn-bias (probabilistic, capped).** Showrunner logic, no new
beats (R7). Read `SignalSnapshot` per-player (`hoardedScore`, `soloMiningRatio`,
`violationRatio`, …) and emit the rhyming creature's *existing* beat
**probabilistically** (a weighted roll, not a threshold) capped so one player is never
singled repeatedly. New: `bestiary-bias.ts` pure `decideSpawnBias(signals, history)`
+ a per-player cooldown high-water in `showrunner_state`. Vanilla fallbacks ship first.

### How it plays across the ~2-week / 5-movement arc

- **M1 (cold open / prologue).** `prologue.curatorialAllowed=false` until ignition;
  the spine drips NOTHING curatorial but gifts still fire and the record is "open
  before you." `keeper-record` writes the flat Archivist living-rows (tier
  `living_row`) — the inert plant. Difficulty starts `even`. Customs bridge is silent
  (no violations measured yet). The scalpel is dormant (no overwhelming signal).
- **M2 (the six Keeper-Stones / mastery reveal).** Drip cadence carries the stone
  ciphers. `reckon()` starts grading mastery: a group crushing the stones finds the
  next drip WITHHELD (cadence ×1.5, tone `cold`) and meets one extra herring (U3);
  a stumbling group gets it sooner (×0.6, `warm`). Mara's bookCipher plants FACT 2b
  ("the record keeps closer count of the quick"). Customs cross `observed`. The
  scalpel may first wake here on a genuinely dominant signal.
- **M3 (the Hold-Book moves / Undercroft).** `keeper-record` advances living-rows
  UNDER keeper headings (tier `keeper_heading`) — the first "oh—, that row was about
  me." Customs reach `warned` (soft reversible toll: the deep goes dark). Herd pale
  spreads pace cosmetically. The Liar's two doors are seeded but inert.
- **M4 (the keeper's own hand / the Liar bites).** `keeper-record` fires the
  `keeper_hand` page — the scalpel's one real personalized authoring slot, with the
  authored `keeperPage*` key behind it. The Liar swap (U4) flips D10's outcome on
  `iss_caught`. Customs may reach `left` (the cold turn). Bestiary bias (U5) is at
  full tilt — the rhyming creature haunts the rhyming player, capped.
- **M5 (the Accepting / resolution).** The Accepting instant is bound (`clock.ts`)
  once `iss_caught + threshold_open`; the grave opens at it. `keeper-record`'s M5
  rewrite ("the present hands are entered") re-quotes the M1 plant backward
  (the FACT 2b payoff). Difficulty relents into resolution. The collective fate is
  set ONCE (`fate.ts`), gated on ACTIVE players only.

---

## 2. CRITIQUE — adversarial, honest

**C1 — The scalpel is the single sharpest law-risk (precision / "it knows me").**
A report or keeper-page that names a transgression the tracker did not measure
shatters the whole illusion on camera. *Mitigation (already structural, must be
enforced in the new code):* the scalpel is offered ONLY a substitution slot on a
row that already passed the measured-signal gate; its output is validated
byte-by-byte against a closed grammar + the anti-slop banned-token list; ANY failure
falls to the authored key. The scalpel can re-voice a measured row; it can never
mint a claim. A FLAT player (no dominant signal) gets the deterministic line, full
stop. **Keep — but the validator is a build-gating self-test, not a runtime hope.**

**C2 — Orphan risk: U3 herring-density could be an invisible gimmick.** If the
group never notices that a confident run earns an extra dead-end, the lever is
orphaned mechanics with no narrative home. *Mitigation:* tie the herring flip to a
diegetic line already in the web — a `dead_end` that flips active drips the SAME
"something is set out... read it, if you can" `voice.drip()`, and its payoff is the
`dead_end` oracle line ("this thread ends; the record keeps it anyway"). The bias is
**story-visible** (one more thing to read) not a hidden number. If it still reads as
noise in playtest, **scale it down to ±0 in M1–M2 and only ±1 in M3–M4.**

**C3 — Bestiary bias risks reading as a deterministic callout (R7).** A creature
that ALWAYS haunts the hoarder is a wrong-precision tell the group will reverse-engineer.
*Mitigation:* it is a weighted roll, capped per-player-per-movement, with a floor
of ambient spawns so the signal is never clean. **Keep, probabilistic + capped only.**

**C4 — Liar swap (U4) is the most jank-prone (a witnessed mutation / double-fire).**
A row that flips `active` while a player is mid-decode could surface contradictory
clues. *Mitigation:* the swap is a CONFIRM-mode `pending` curatorial beat by default
(human gate), the AUTO fallback is a pre-authored duplicate row (no live mutation),
and the flip is idempotent on `iss_caught` (first-writer-wins, like `forks.ts`).
The clue card itself is forged once; the swap changes which row is `active`, never
re-renders a posted card. **Keep — but ship CONFIRM-default; AUTO only after slice.**

**C5 — `readMastery` is a coarse group proxy (firstTryRate ≈ solves/(solves+fails)).**
It can mis-grade a group that fails loudly then solves quietly. *Mitigation:* the
hysteresis (`hysteresisMs ≈ one cadence`) already damps flapping; tighten the proxy
when the whisper-spend join lands (noted in code). Acceptable for the slice; **do not
let it drive anything player-visible beyond cadence/tone until the join exists.**

**C6 — Single point of failure (MASTER-PLAN risk #1).** The whole curated layer
hangs on this cron. *Mitigation (already true):* earned beats fire `approved`
WITHOUT the showrunner; only the *curated* drip pauses if it dies. The deterministic
fallback behind every LLM call means a model outage never blocks a tick. **No change
needed; this is the design's spine and it holds.**

**Nothing should be CUT.** One thing should be **scaled**: U3 herring-density to a
conservative ±1 cap, M3+ only (C2). U4 and U5 ship CONFIRM-default / vanilla-fallback
first (C3, C4). Everything else is keep-as-specified.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (cold, plain, concrete)

> Watcher / keeper register: it counts and records; it does not emote or threaten.

- **Scalpel report (overwhelming hoard signal, measured):**
  `nine days, and the iron stays in your hands. the record keeps a column for that.`
  *(no named emotion, no bow; states a measured count + a flat consequence.)*

- **Herring `dead_end` that flips active under a confident group:**
  `this mark was cut by someone who is gone. it points nowhere. it is kept anyway.`

- **Liar swap — the warm door's dead-shrine (D10 false):**
  `the name you brought fits the lock. the lock opens on an empty room.`

- **Keeper-hand page, M4 (authored key; scalpel may re-voice the middle clause only):**
  `sella kept the low fires. you keep them now. the page is in her hand, not mine.`

---

## 4. THREAD IT — exact appearances (the consistency law)

**Canon FACTs / INV touched or added:**
- **FACT 2b** ("the land's grip is not fixed") — the difficulty dial is its mechanical
  expression. Already wired via `reckon()`; U3 adds the herring lever it names.
- **FACT 9 / 12 / 14** (the Hold-Book "writes you in") — `keeper-record` + the report
  scalpel are the producers. FACT 9 is the open content gap (R11) the authored
  `keeperPage*` keys close.
- **INV-15** (difficulty NEVER touches the Whisper backstop) — U3 must add a grep-clean
  self-test asserting `bestiary-bias.ts` / the herring flip never read `whisper_budgets`.
- **INV-16** (no surface reveals WHICH player is honored/violated) — the scalpel + bias
  speak per-player privately; the BOOK heads on a chorus signal, never the extremes.
- **INV-6** (AUTO⇄CONFIRM gate) — every new producer honors `BeatStatus`.
- **INV-8** (tolls take warmth, not progress) — the customs `warned` toll + any new toll.
- **Adds nothing new to the spine FACTs; it is the engine that EXPRESSES them.**

**Found-documents / journals that foreshadow:**
- **Mara's bookCipher fragment** (M2, "the record keeps closer count of the quick") —
  the FACT 2b plant the M5 rewrite re-quotes. (`reckoning.ts` header already names it.)
- **The Archivist's living-habit rows** (M1 Hold-Book) — the inert rows the scalpel
  later moves under headings. `arc/lore/documents/` Hold-Book pages.
- **D04** (`observed-warned-left-at-threshold.md`) — Orin's ladder the customs bridge
  voices via `voice.reportObserved/reportEscalated`.

**NPC / Watcher voice keys that carry it:**
- `voice.drip()`, `voice.reportObserved`, `voice.reportEscalated`, `customPhrase()`
  (existing). New authored keys: `voice.reportScalpel*` (the constrained-slot
  templates), `voice.keeperPage*` (M4 hand pages, the deterministic fallback),
  `voice.deadEndKept()` (the herring `dead_end` oracle line), `iss.dialogue.turns_cold`
  (the Liar `private_message`).

**Ciphers / puzzles that express it (reuse the 11 built):**
- **bookCipher** — Mara's FACT 2b plant/payoff (M2 plant, M5 backward-read).
- **vigenere** — the Liar's two D10 rows (`clue_vigenere_iss_warm` /
  `clue_vigenere_iss_name`), keyed on a keeper name.
- The six Keeper-Stones (P1.10) each teach a DIFFERENT built cipher
  (`caesar`/`book`/`atbash`/`vigenere`/`substitution`/`coordEncode`) — the drip
  cadence the showrunner paces is what surfaces each stone's forged card.
- The drip/forge path uses whatever cipher each `puzzles` row authors; the showrunner
  is cipher-agnostic by design (`clue-specs.ts` registry).

**Beat classes / listeners / tables / seed rows / sites.yml / voice keys realized:**
- Beats: `UnlockBeat`→`DoorOpenBeat`/`SmallStructureBeat`/`PrivateMessageBeat` (drip
  payoffs); `NamedMobBeat`/`SacredAnimalBeat`/`TorchGutter` (bestiary bias, existing);
  `LecternFillBeat`/`BookAppears` (keeper-record pages); the customs soft-toll beat.
- Tables: `puzzles` (Liar rows, herring `dead_end` rows), `beat_queue` (`status`
  pending/approved), `dossiers` / `custom_compliance` / `heatmap_cells` (the U1
  readers), `arc_state.flags` (`iss_caught`, `prologue_ignited`, `accepting_instant_ms`),
  `settings.showrunner_state` (all idempotency high-water marks — no migration).
- Seed rows: the two Liar `puzzles` rows; the staged herring `dead_end` rows per
  movement; the M2 Mara bookCipher row.
- `showrunner_state` new marks: `report_marks` (scalpel idempotency),
  `bestiary_bias` (per-player cooldown), `herring_active` (per-movement flip state).

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for" seed

**Primary plant (already half-wired — complete it):** In **M1**, `keeper-record`
writes each living player a FLAT Archivist living-habit row in the Hold-Book — e.g.
`"keeps the low fires lit"` — with NO heading and NO keeper attached. It reads as
ambient world-texture, inert and ambiguous (just a list of habits). The showrunner
records the same measured signal silently each tick (it is what `dossiers` accrues).

**Payoff (M3 then M4):** In **M3** the showrunner moves that exact row UNDER a keeper
heading (`keeper_heading` tier) — the first re-read: *that habit list was about ME,
and it belongs to Sella.* In **M4** the keeper's OWN hand writes the living player
(`keeper_hand` tier, the scalpel slot) — *the dead keeper is writing me in.* In **M5**
the book rewrites to "the present hands are entered," closing it.

**Second plant/payoff (FACT 2b, owned by `reckoning.ts`):** Mara's M2 bookCipher
fragment "the record keeps closer count of the quick" is inert flavor on first read.
In **M5** the COMPOSER re-quotes it backward as the reveal that the difficulty grip
was the land grading them all along. The showrunner only EXPOSES the `state` it
re-quotes from; it does not author the M5 line (clean ownership boundary).

**No orphan:** every showrunner lever has a plant and a payoff. The difficulty dial
plants FACT 2b (M2) → pays it (M5). The Hold-Book plants the flat row (M1) → pays it
(M3/M4/M5). The Liar plants two doors (M3) → pays the wrong one (M4). The herring
lever plants an extra "something to read" → pays the `dead_end` "kept anyway" line.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Piece | Lives in | Depends on | Depended on by | Priority |
|---|---|---|---|---|
| Spine (decide/apply/snapshot/run) | M1–M5 (every tick) | P0.1–P0.4 (pack, FAWE, seeds) | everything below | **DONE (P0/P1-spine)** |
| U1 cross-owner readers | M2+ (when data accrues) | `dossiers`/`custom_compliance`/`heatmap_cells` schema (SQL+PLUGIN lanes) | U2, keeper-record, bias, grave roster | **P1 (arc-spine)** |
| U2 report scalpel + fallback | M2 wake → M4 peak | U1 readers; `voice.reportScalpel*` keys | the "it knows me" payoff | **P1** |
| U3 herring-density dial | M3–M4 (scaled) | `reckon()` (done); staged `dead_end` rows | difficulty texture | **P2 (depth)** |
| U4 Liar swap | M3 seed → M4 bite | P1.11 puzzles rows; `flags.iss_caught`; `forks.ts` pattern | the re-walk payoff | **P1** |
| U5 bestiary spawn-bias | M2 → M4 | `SignalSnapshot` (done); beats (done) | per-player dread | **P2** |

**Vertical-slice (P0) needs NO showrunner** — earned beats fire `approved` alone.
Build order after the slice: **U1 (readers) → U2 (scalpel) → U4 (Liar) → U5 (bias) →
U3 (herring)**, each gated by `scenario.selftest.ts` + a per-piece self-test. The
scalpel byte-validator and the INV-15 grep-clean test are build-gating, not optional.

**Hard rule honored throughout:** every LLM call (U2 scalpel, U5 bias-flavoring if
any) chooses its deterministic authored value FIRST and offers the model a slot it may
decline; the scenario self-test must stay green with the model stubbed to always-fail.
