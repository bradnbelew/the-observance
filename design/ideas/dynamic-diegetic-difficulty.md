# Dynamic Difficulty as a Diegetic Force — design treatment

> IDEA: if the group is crushing it, the land **tightens** — the next door is
> withheld longer, more true-but-pointless terminals open, the Watcher grows
> colder; if stalled, **grace flows** — the door comes sooner, the noise clears,
> the voice warms. Adaptive difficulty framed as *the land grading their mastery*
> so it reads as STORY, not a slider.
>
> READ-FIRST grounding (verified 2026-06-25): `FLOW.md` §1–3 (Whispers, drama
> budget, soft-pressure), `discord/src/showrunner/decide.ts` (pure policy: stall
> auto-gift + clue drip; **already** multiplies `dripIntervalMs` by
> `reckoning.cadenceMult` and carries `tone`), `discord/src/showrunner/reckoning.ts`
> (**built** — the pure grip policy), `discord/src/showrunner/customs.ts` (carries
> `tone` through, never lets it change which rung fires), `types.ts` (`ReckoningState`,
> `Tone`, `Snapshot.reckoning`), `autonomy.run.ts` (`computeAutonomyGates` →
> `reckon()` + persisted hysteresis), `0004_oracle.sql` (the mastery substrate:
> `solves`, `answer_attempts`), `arc/lore/canon-spine.md` (FACT 2b, FACT 16, INV-15,
> §6 hard rules), `arc/lore/documents/a-closer-count-of-the-quick.md` (**the plant,
> authored**), `design/WEB-MASTER.md` §9 #12 (the plant/payoff ledger row).

---

## 0. VERDICT UP FRONT — KEEP-SCALED (and largely already built)

**KEEP, HARD-SCALED.** This is not greenfield. The spine is on disk and verified:
the pure `reckon()` policy, the `decide.ts` integration (one line: `dripIntervalMs *
cadenceMult`), the `Snapshot.reckoning` plumbing, the run-loop wiring with persisted
hysteresis, canon FACT 2b / INV-15, the M1 plant document, and the WEB-MASTER #12
ledger row all exist. This treatment's job is to (a) ratify the scaling decisions
against the laws, (b) name the **four real remaining gaps** as a build-ready
punch-list, and (c) lock the plant→payoff so the mechanic is never an orphan.

Three of the pitch's four knobs were re-pointed before they shipped; the fourth was
cut. This is correct and is now canon (INV-15) — recorded here so no future pass
re-adds the defect:

- **Whisper budget shrinking = CUT (permanent, INV-15).** Whispers are the
  player-controlled retention backstop (`FLOW.md` §3, `decide.ts` step 1).
  Difficulty must never touch the safety rail. `reckoning.ts` reads/writes **no**
  `whisper_budgets`; its self-test asserts the source is clean. *Grep guard: a hit
  for `whisper_budgets` in the difficulty module is a build defect.*
- **"Red herrings rise" = SCALED to STAGING, not spawning.** We never generate
  herrings (no LLM puzzle authoring — grounding law). Mastery raises how many
  pre-authored `dead_end` rows are `active` at once. (This knob is **designed but
  not yet wired** — see Gap C.)
- **"Watcher grows colder" = register-key SELECTION, never an LLM temperature.**
  `tone ∈ {cold, plain, warm}` selects among pre-authored voice variants. The
  selection is deterministic; it is its own fallback. (`tone` flows through
  `decide`/`customs` today, but `voice.ts` does not yet branch on it — Gap A.)
- **Clue-drip cadence = the one genuinely new, fully-live knob.** `tight` ×1.5
  (the land withholds), `even` ×1, `loose` ×0.6 (the land relents). Invisible and
  diegetic by construction; this is the highest-ROI piece and it **works today**.

The diegetic frame is the whole point and it is *earnable*: the group must be able
to **notice the land hardened** and **read why** (§1.4, §5), or it is an invisible
slider and the idea is orphaned (the consistency law). The plant/payoff converts the
mechanic into the felt FACT that the land was grading their mastery the whole time
(FACT 2b → FACT 9).

---

## 1. EXPOUND — the mechanic, the story, the mystery

### 1.1 The mastery signal (measured, never guessed) — BUILT

`reckon(ReckoningInput, ReckoningConstants): ReckoningResult` is a pure function of
data already in `0004_oracle.sql`. The live inputs (`autonomy.run.ts` `readMastery`):

- **`distinctSolvers`** — distinct *active* players who solved ≥1 node in the window.
  Breadth, so one carry can't trip `tight` (precision mitigation R2).
- **`firstTryRate`** — share of solves with no prior failed attempt. A group that
  cracks ciphers cold is "ahead of the record."
- **`whisperLean`** — avg whispers spent per solve. The *struggle* tell. This is the
  same bond tally that rhymes with Iss (canon §4) — reused, not new — and it is read
  here only as a **group aggregate**, never per-player (INV-16).

`readGrip()` resolves to three states with a **decency floor**: struggle wins ties
toward `loose` — `firstTryRate <= 0.25` OR `whisperLean >= 1.5` → `loose`; only a
group both fast (`firstTryRate >= 0.6`) AND not whisper-leaning → `tight`; else
`even`. Hysteresis (`hysteresisMs ≈ one drip cadence`) holds a state until it ages,
so the grip never strobes — the same anti-flap discipline as the customs high-water
mark.

| State | `tone` | `cadenceMult` | Diegetic frame |
|---|---|---|---|
| `loose` | `warm` | ×0.6 | the land waits, gives the door sooner, speaks warmer |
| `even` | `plain` | ×1.0 | the record holds; default cadence + register |
| `tight` | `cold` | ×1.5 | drips withheld longer, more terminals open, register cools |

> Naming note: canon/code use **`tight`/`even`/`loose`** (= the pitch's
> tight/level/grace). An earlier draft of this file used `grace/level/tight`; the
> code names win. Do not re-introduce the synonyms.

### 1.2 The three knobs the state turns

1. **Drip cadence multiplier — LIVE.** `decide.ts` line:
   `effectiveDripInterval = s.dripIntervalMs * (s.reckoning?.cadenceMult ?? 1)`.
   One scalar, one existing gate, no new branch in the hot path. Player-helpful
   gifts (the stall backstop) are computed *before* this and are unaffected — the
   cadence knob can never starve the safety rail.
2. **Dead-end staging — DESIGNED, NOT WIRED (Gap C).** A small authored pool of
   `dead_end` rows carries a `min_state` author tag. The apply pass flips their
   `active` flag: `tight` opens the spare terminals (more discernment tax), `loose`
   closes them (less noise when drowning). Pre-authored rows only — never spawn.
3. **Register selection — PARTIALLY WIRED (Gap A/B).** `tone` already rides the
   `Decision` and the `CustomReport`. The remaining work is the `voice.ts` variant
   tables and `apply.ts` passing `tone` into the `voice.*` calls.

### 1.3 How it plays across the five Movements

- **Movement I (Act 1, ~Days 1–5).** The Reckoning **computes but stays mute**:
  `reckon()` runs and persists `reckoning_state` each tick, but the engine is clamped
  to `even` in Act 1 (we never tighten on a group still learning what a custom is —
  R7). Its only M1 footprint is one planted artifact (§5) that will re-read later.
  The land is taking the same measure it took of every prior keeper (FACT 2:
  "graded by laws no one told them") — the difficulty engine *is* that grading — but
  the group cannot feel it yet. That silence is the plant.
- **Movement II (Act 2 opens, ~Days 6–9).** Reckoning goes live. A sharp veteran
  group *first notices*: they're cracking keeper-stone ciphers fast, and the next
  clue stops coming on the clock they'd half-learned. The keeper they're crushing is
  **Mara** (book-cipher, "always had the map, never the shovel") or **Vaun** (Caesar,
  the Hoarder) — and the land tightening on *speed* rhymes with Mara's fate exactly:
  mastery of reading without the work is what the land has seen before, and marked
  closest. FACT 2b surfaces here through its first door.
- **Movement III (~Days 10–11).** The Seventh Stone thread (canon §5) intersects:
  the Seventh kept all the ways and was *cast out* (FACT 10b). A group in sustained
  `tight` feels the land's patience thinning — foreshadowing FACT 10 ("acceptance is
  a choice the land makes, and it can refuse") *without* the engine ever punishing:
  `tight` is withholding + colder, never destroyed progress (INV-8).
- **Movement IV (~Day 12).** The Iss catch. A group that leaned hard on Whispers
  sits in `loose` and is spoken to *warmly* — and that warmth is the trap, because
  the warmest voice is the liar's (§4). The difficulty frame and the Liar frame
  collapse: the most-coddled group is the most Iss-like, "the one who most wanted to
  be told the comforting answer." No callout; pure colorant (INV-16).
- **Movement V (Act 3, ~Day 14).** The Reckoning is read *backward* (§5). The M1
  artifact is now legible; the Keeper re-quotes Mara's line as judgment: the land
  kept its closer count of *them*, the quick, from session one. The "slider" they
  half-suspected resolves into the spine's central truth — it was never tuning a
  game, it was watching how they kept the ways (FACT 9, "the dread had a biography").

### 1.4 The diegetic surface — how the group *reads* the tightening

The land must be **noticeable and explicable** or this is an invisible slider. Three
read-paths (web rule: ≥2 doors):

1. **The cadence itself**, once they've internalized a rhythm — "it used to answer
   by morning." Felt, not stated.
2. **A drip-line tone shift** they can quote side by side: the same report register
   that was patient now reads clipped (Gap A delivers this).
3. **The found-document** (§5, authored): the record *keeps a closer count of the
   quick.* The Rosetta line that makes a player say "wait — it's been doing that
   **to us.**"

---

## 2. CRITIQUE — adversarial, honest

**R1 — It attacks the retention rail (the sharpest risk). — RESOLVED.** The pitch's
"Whisper budget shrinks" fights `FLOW.md` §3 and the stall auto-gift. *Mitigation
(shipped):* the budget knob is CUT and the cut is canon (INV-15). `reckoning.ts`
touches no `whisper_budgets`; the gift loop in `decide.ts` runs before and
independent of the cadence multiply. **Single sharpest residual risk** is keeping
this cut true under future edits → the self-test grep guard is the standing defense.

**R2 — Precision: "ahead of the record" can be a false read.** Velocity spikes when
one strong solver carries, or an answer leaks in voice chat we can't see.
*Mitigation (built):* breadth (`distinctSolvers`, distinct *active* players) + a
struggle-wins-ties floor + hysteresis one-tick hold. And `tight` is the gentlest
expression — slower drip, cooler tone, **never an accusation** — so a false `tight`
costs a longer wait, not a false "it knows you." Recall errors are cheap by design.

**R3 — Collective-judgment law.** Whisper-lean per-player could elect a "best"
keeper. *Mitigation (built):* the Reckoning is **group-scalar only**; it stores no
per-player rank. INV-16 covered.

**R4 — Orphan risk: an invisible slider.** If the group never notices the land
hardened, the mechanic moved no story. *Mitigation:* the M1→M5 plant/payoff (§5) is
**mandatory, not optional**. The plant doc is authored; the M5 re-quote is Gap D.
Until Gap D lands, this idea is *not fully shipped* — the tuning without the payoff
is the orphan the law forbids.

**R5 — On-camera confusion.** A viewer could read a slower drip as the system being
*broken*, not the land *withholding*. *Mitigation:* `tight` must pair the withheld
drip with a *spoken* cold register beat on the normal cadence — the land is audibly
present and patient, just not *giving*. Silence is never the only signal. (This is
exactly why Gap A — the spoken cold variant — is P1-blocking, not cosmetic.) The
dashboard health panel shows `reckoning_state` to spoiler-free Ethan so a slow drip
reads as intentional (Gap E).

**R6 — Determinism / anti-jank.** *Mitigation (built):* `reckon()` is a pure
function of its input, unit-tested with no DB/clock, constants injected. Identical
input → identical state.

**R7 — Scope creep into Act 1.** Tightening a still-learning M1 group is cruel and
misfires (no baseline). *Mitigation:* clamp to `even` in Act 1. **Verify on build:**
`reckon()` itself does not currently read `currentAct` — the Act-1 clamp must be
enforced by `computeAutonomyGates`/`decide` (e.g. gate `gates.reckoning` to neutral
when `currentAct < 2`). *This clamp is a named gap (Gap F) — do not assume it; the
reckoning policy will otherwise tighten in M1.*

**Net:** keep cadence (live), finish register selection (A/B), wire dead-end staging
(C), author the M5 payoff (D), surface state on the dashboard (E), enforce the Act-1
clamp (F). Cut budget forever. Small, deterministic, load-bearing on the central
theme rather than a gimmick.

---

## 3. DE-SLOP TEST — exemplar lines (cold, plain, concrete)

The found-document that names the mechanism (Mara's hand — authored, excerpt):

> the record keeps a closer count of the quick. it gave us less, the season we
> needed less. it gave the slow ones time. i called that a cruelty for a winter. i
> do not call it that now, and i will not say what i call it.

A `tight`-state drip line (the report register, clipped — seeds `voice.drip(…,'cold')`):

> the record holds. it has answered you once. it does not answer twice.

A `loose`-state drip line (warmer, never gushing — seeds `voice.drip(…,'warm')`):

> take the time. the deep has waited longer than this for less.

The M5 Keeper re-quote (lands FACT 9 on the mechanic — seeds the §5 composer line):

> you were quick. we kept a closer count of you for it, from the first night. you
> felt the hand close. you took it for the dark being difficult.

The dashboard-internal label, in-voice for Ethan only (never player-facing):

> the deep tightens. four doors open, one true. the hand is closing.

(None name an emotion, none threaten, none editorialize. The bite is the flat
withholding and the iceberg under "i will not say what i call it.")

---

## 4. THREAD IT — where it lives so it is not an orphan

**Canon FACTs (`arc/lore/canon-spine.md` §3 / §3b) — SEALED:**

- **FACT 2b (child of FACT 2) — "The land's grip is not fixed."** M2 · REVEAL.
  Two doors: Mara's bookCipher fragment; the felt cadence cooling. → 9, 10. This is
  *the* fact this idea adds; it is already in canon §3b.
- **INV-15 — "The difficulty engine never touches the Whisper backstop."** SEALED in
  §7. The cut, made law.
- **Touches FACT 2** (the grading made tactile), **FACT 9** (the M5 backward-read),
  **FACT 10/10b** (sustained `tight` foreshadows the land's refusal without punishing).
- *Namespace note:* the integer **FACT 16** belongs to `name-where-never-been`
  (place-filing), NOT to this idea — synthesis assigned it there (`WEB-MASTER §0.1`).
  An earlier draft of this treatment mis-minted FACT 16 for difficulty; **do not.**
  This idea's fact is FACT 2b.

**Found-documents / journals — AUTHORED:**
`arc/lore/documents/a-closer-count-of-the-quick.md` (Mara's page-reference register,
`movement: 1`, `clue_bearing: true`, book-cipher, foreshadows FACT 2b). It is a real
artifact: column of page/line/word refs, a later-hand margin, `[...]` damage, a
cross-link to Sella's *what-the-surface-keeps*. Cross-referenced in
`arc/corpus/journals-vaun-mara-sella.md`.

**NPC / Watcher voice lines:** `voice.drip`, `voice.oracleDeadEnd`,
`voice.reportObserved` gain a `tone` arg selecting pre-authored cold/plain/warm
variants (the §3 exemplars seed these) — **Gap A**. The Keeper's M5 summons gains the
backward-reading re-quote (§3, §5) — **Gap D**.

**Cipher(s) / puzzle(s) (reuse the 11 built ciphers):** the plant is a
**bookCipher** node (Mara's embodied cipher — page/line/word) seeded with
`outcome_type='lore'` so it reveals FACT 2b without being a progress door. The
dead-end staging pool reuses **existing** `dead_end` rows (the seed's terminals,
across `atbash`/`columnar`/`polybius`) tagged with `min_state` — no new cipher type;
this is a *scheduling* idea, not an authoring one.

**Modules / tables / state / voice keys that realize it:**

- `discord/src/showrunner/reckoning.ts` — `reckon()` + `reckoning.selftest.ts`. **BUILT.**
- `types.ts` — `ReckoningState='tight'|'even'|'loose'`, `Tone='cold'|'plain'|'warm'`,
  `Snapshot.reckoning?`. **BUILT.**
- `decide.ts` — `dripIntervalMs * cadenceMult`; carries `tone` on the `Decision`. **BUILT.**
- `autonomy.run.ts` — `computeAutonomyGates` → `reckon()`, persists
  `reckoning_state`/`reckoning_since_ms` (hysteresis) in `showrunner_state` jsonb
  (no migration). **BUILT.**
- `customs.ts` — carries `tone` onto each `CustomReport`, never changing which rung
  fires. **BUILT.**
- `voice.ts` — `tone` arg + variant tables; new `voice.deepTightens` /
  `voice.deepIsPatient` lines. **GAP A.**
- `apply.ts` — pass `tone` into `voice.*`; flip staged `dead_end` `active`
  (Discord-side cron write, idempotent, no MC world write). **GAP B/C.**
- `puzzles_seed.sql` — `min_state` author tags on existing `dead_end` rows. **GAP C.**
- M5 ending composer (`WEB-MASTER §5` / `forks.ts`-adjacent) — the one tinted Keeper
  re-quote that reads `reckoning_state` backward. **GAP D.**
- `dashboard/src/` — health panel shows current `reckoning_state` (R5). **GAP E.**
- Act-1 clamp in `computeAutonomyGates`/`decide` (neutralize `gates.reckoning` when
  `currentAct < 2`). **GAP F.**

---

## 5. PLANT THE PAYOFF — the "OH, that's what that was for"

(WEB-MASTER §9 ledger row #12 — authoritative.)

**Plant (Movement I, inert) — AUTHORED.** In the first base journal, among ordinary
keeper entries, is one fragment in Mara's hand (bookCipher) that decodes to a flat
observation: *"the record keeps a closer count of the quick."* In M1 it is ambiguous
keeper-lore — one more dead keeper muttering about the record. `outcome_type: lore`,
so it opens no door, which is exactly why it reads as inert flavor. The mechanic it
describes is *mute* in M1 (§1.3), so nothing yet connects.

**Latent re-read (Movement II–III).** Once the Reckoning goes live and a sharp group
notices the cadence change, the fragment becomes a hypothesis they can test against
their own logs: "the closer count of the quick" — is the land answering us slower
*because we're fast?* FACT 2b surfacing through its first door.

**Payoff (Movement V) — GAP D (the line is not yet authored into the composer).** In
the Accepting, the Keeper re-quotes Mara's line back as judgment (§3): the land kept
its closer count of *them*, the quick, from the first night; the difficulty they felt
was the record grading their mastery the whole arc, the same exam every prior keeper
sat (FACT 9). The inert M1 mutter was the rule of the whole experience, hiding in
plain sight as lore.

No plant without payoff: the fragment exists only to pay off in M5. No payoff
without plant: the M5 re-quote is meaningless unless they met the line cold in M1.
**Graceful degradation:** if the engine were ever off, the plant still reads as
keeper-lore and the M5 line is simply cut — no contradiction, no dangling mechanic.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES

- **Lives in:** Movement II (engine goes live) → Movement V (backward-read payoff).
  Computes-but-mute in Movement I. Foreshadow peaks Movement III (Seventh / FACT 10
  intersection).
- **Depends on:** the showrunner spine (`decide.ts`, `snapshot.ts`/`autonomy.run.ts`,
  `state.ts`) — BUILT; the solve/whisper substrate (`0004_oracle.sql`) — BUILT; the
  voice register — extends it.
- **Depended on by:** nothing gates on it (correct — it is a colorant, not a door).
  The M5 payoff *references* it but degrades gracefully (§5).
- **Priority:** **P1 (arc-spine).** Not P0 — the vertical slice survives at ×1.0. But
  it is the cleanest expression of the FACT 2 / FACT 9 entrance-exam theme on the
  *pacing* layer, and most of it is already built. The piece that must ship *with* it
  to avoid orphaning is the spoken register + M5 re-quote (Gaps A, D) — that pairing
  is P1-atomic. The budget knob is **CUT (P-never, INV-15).**

### Build punch-list (remaining, in dependency order)

| Gap | Work | Priority |
|---|---|---|
| **A** | `voice.ts`: `tone` arg + cold/plain/warm variant tables on `drip`/`oracleDeadEnd`/`reportObserved`; new `deepTightens`/`deepIsPatient` (§3 seeds). Spoken cold beat is the R5 mitigation, so this is P1-blocking. | P1 |
| **B** | `apply.ts`: pass `Decision.tone` / `CustomReport.tone` into the `voice.*` calls (the TS-VOICE hand-off already stubbed in `customs.ts`). | P1 |
| **C** | Dead-end staging: `min_state` author tag on existing `dead_end` seed rows; `apply.ts` flips `active` by current state (idempotent, Discord-side). Lowest value of the three knobs — ship after A/B if time-boxed. | P2 |
| **D** | M5 composer: one tinted Keeper re-quote reading `reckoning_state` backward (the payoff). Without it the idea is orphaned (R4). | P1 |
| **E** | Dashboard health panel: show current `reckoning_state` (intentional-not-broken). | P2 |
| **F** | Act-1 clamp: neutralize `gates.reckoning` when `currentAct < 2` in `computeAutonomyGates`/`decide` (R7). Cheap, do first. | P1 |
