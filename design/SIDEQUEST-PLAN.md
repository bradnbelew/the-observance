# THE OBSERVANCE — THE SIDE-QUEST WEB (triage + designs)

> **DESIGN doc, not a seed.** Triages the raw backlog in [`design/ideas/`](ideas/) (28 files) and
> designs the KEEP-as-sidequest ones as cohesive side-quests. Obeys the canon set (OVERHAUL.md,
> PUZZLES.md, WORLD-BIBLE.md, canon-spine.md, the-seventh-below.md, the-companion.md). Honors the
> cohesion gate (OVERHAUL §5, §7.1 / canon-spine §6): **nothing inert may costume itself as a
> puzzle** — every entry below is labelled HONESTLY as **[real puzzle]**, **[flavor / atmosphere]**,
> or **[reactive drama]** (character/story that reacts but has no submit-answer).
>
> **Invariant floor (canon INV-12 / OVERHAUL §4):** every side-quest **gates nothing** and nothing
> spine-critical gates on it. Removing all of them leaves the reconstruction intact (matches the
> existing `side_quests.sql` breadth invariant: `gates_progress = false`). Async-first: a solo
> player on a quiet night can advance any of these (except the two flagged convergence beats).

---

## 1. THE TRIAGE TABLE (all 28 ideas/ files)

Three verdicts:
- **KEEP-as-sidequest** — a discrete off-spine quest with entry/pay-off; designed in §2–§5.
- **KEEP-as-flavor-lore** — real content, but it is atmosphere/mechanic/canon-machinery, not a
  quest with an entry and a reward. Kept, not designed as a quest here.
- **CUT** — superseded, out-of-scope for content, or already fully absorbed into canon.

| # | ideas/ file | Verdict | One-line reason |
|---|---|---|---|
| 1 | `arg-leaves-the-game.md` | KEEP-as-sidequest | The record-website + Drive archive + real-clock is the external-surface quest web (OVERHAUL Pillar 4) → **SQ-REC** (the record) + feeds `spine-recovered-archive`. |
| 2 | `backlog-accepting-sentinel-bridge.md` | KEEP-as-flavor-lore | Finale *wiring* (the sentinel bridge), not a side-quest — engine machinery for the Accepting. |
| 3 | `backlog-bestiary-spawn-bias.md` | KEEP-as-flavor-lore | The 6 apparitions' fair spawn — atmosphere machinery (single-arbiter conductor, INV-18), not a quest. |
| 4 | `backlog-desire-paths.md` | CUT | Explicitly "not a commitment, an idea on the shelf" (Roads-More-Traveled riff); Pillar-5 phase, no content owed. |
| 5 | `backlog-fawe-large-setpieces.md` | KEEP-as-flavor-lore | Schematic authoring loop for big builds — the *how* the sites get built, not a quest. |
| 6 | `backlog-full-showrunner.md` | KEEP-as-flavor-lore | The async salience engine (OVERHAUL Pillar 2) — the delivery brain, not a quest. |
| 7 | `backlog-keeper-npc-framework.md` | KEEP-as-flavor-lore | Citizens2 Keeper + per-player apparitions — NPC framework the quests use, not itself a quest. |
| 8 | `backlog-keeper-stone-expeditions.md` | KEEP-as-sidequest | The six keeper-stone constellations ARE the keeper side-quests → drives §2 (the six keeper clusters). |
| 9 | `backlog-liar-engine.md` | KEEP-as-flavor-lore | One-key-two-doors re-walk = the Iss catch machinery (PUZZLE-DESIGNS §7), spine not side. |
| 10 | `backlog-modeled-mob-and-voice.md` | CUT | ModelEngine unbought (OVERHAUL §5 CUT); the Ear/voice is P3 Observer, folded into `spine-spoken-name`. Bestiary *lore* salvaged (§5.4). |
| 11 | `backlog-undercroft-dimension.md` | KEEP-as-sidequest | The fog world + the room that rebuilds wrong → **SQ-UNDER** (the Undercroft cluster), §3. |
| 12 | `backlog-unlockbeat-producers.md` | KEEP-as-flavor-lore | "Solve → world visibly opens" producer rows (INV-20) — the payoff wiring every quest reuses. |
| 13 | `cold-start-prologue.md` | KEEP-as-sidequest | Week-zero ignition anomaly (the screenshot-to-Discord lure) → **SQ-COLD** (the cold start), §5.1. |
| 14 | `collective-restraint-custom.md` | KEEP-as-flavor-lore | The Unlit Deep group latch (canon-spine §8.3) — a custom, already canonized, not a quest. |
| 15 | `coords-to-real-place.md` | KEEP-as-sidequest | The two walks (false + true) → **SQ-WALKS**, §5.2 (the expedition, WORLD-BIBLE §11.2). |
| 16 | `counting-base-journal.md` | KEEP-as-sidequest | The base book counting down in a dead hand → **SQ-COUNT**, §5.3 (pairs record-writes-you-in). |
| 17 | `cross-surface-coop-gate.md` | KEEP-as-flavor-lore | The `m4-three-hands` coop gate = spine convergence (PUZZLE-DESIGNS `spine-threshold-vault`), not side. |
| 18 | `cursed-map-frame.md` | KEEP-as-sidequest | The cursed adventure-map prologue vignette + frame-break → part of **SQ-COLD**, §5.1. |
| 19 | `day-one-meta-puzzle.md` | KEEP-as-flavor-lore | The planted-inert meta = the `unkept` acrostic (PUZZLE-DESIGNS `spine-unkept-acrostic`), spine texture. |
| 20 | `dead-ends-with-teeth.md` | KEEP-as-sidequest | Red-herrings that talk back + the false-prophet thread → **SQ-HERRING**, §5.4. |
| 21 | `divergent-fates-endings.md` | KEEP-as-flavor-lore | The 4 fates + 2 codicils (canon-spine §8.4) — the M5 composer, canonized, not a quest. |
| 22 | `dynamic-diegetic-difficulty.md` | KEEP-as-flavor-lore | FACT 2b, the land tightens/loosens — invisible pacing (OVERHAUL §5 DEMOTE), never a quest. |
| 23 | `exclusive-forks-permanence.md` | KEEP-as-flavor-lore | One-way forks (Sacred Beast etc.) — permanence *colors* (INV-12), a mechanic, not a quest. |
| 24 | `future-dated-grave.md` | KEEP-as-flavor-lore | The grave with tomorrow's date = the reunion destination (OVERHAUL §0, FACT 13b) — spine set-piece. |
| 25 | `herd-conversion.md` | KEEP-as-sidequest | The slow Pale-herd conversion + the one true deep-bird → **SQ-BIRD**, §5.5 (the deep-bird vigil). |
| 26 | `minecraft-progression.md` | KEEP-as-sidequest | The Nether (fire-source) + End (exile) optional lanes → **SQ-NETHER** + **SQ-END**, §5.6 (WORLD-BIBLE §12). |
| 27 | `name-where-never-been.md` | KEEP-as-flavor-lore | Your name carved where you've never been — per-player apparition beat (FACT 16, INV-16), atmosphere. |
| 28 | `offline-skin-apparition.md` | KEEP-as-flavor-lore | Apparition in an offline friend's skin — per-player scare beat (INV-16/18), atmosphere. |
| + | `record-writes-you-in.md` | KEEP-as-flavor-lore | The record starts describing the living in a dead keeper's hand — the profiling atmosphere spine, not a quest. |
| + | `some-laws-are-lies.md` | KEEP-as-flavor-lore | The forged eighth law (FACT 7b, the Covering) — a document/deduction folded into the Iss catch, canonized. |
| + | `the-seventh-spine.md` | KEEP-as-sidequest | The Seventh restore/erase optional spine → **SQ-SEVENTH**, §4 (the Seventh cluster). |

**Tally:** 31 files triaged (28 in the listing + 3 that appear beyond the first 28 alphabetically).
- **KEEP-as-sidequest:** 12 → grouped into the clusters designed in §2–§5.
- **KEEP-as-flavor-lore:** 17 → kept as canon machinery/atmosphere (not re-designed here; listed so
  none is lost).
- **CUT:** 2 (`backlog-desire-paths` shelf-only; `backlog-modeled-mob-and-voice` ModelEngine cut,
  voice absorbed into the Observer, bestiary lore salvaged).

---

## 2. THE SIX KEEPER CLUSTERS (constellations — each a self-contained side-quest web)

Per OVERHAUL Pillar 2, each keeper is a **constellation** of 3–5 storylets enterable in almost any
order (the only global precondition: the group is in the Hold). Each cluster's puzzles are designed
in [PUZZLE-DESIGNS.md](PUZZLE-DESIGNS.md). Here is each as a *quest*: entry, pay, thread, honest label.

### SQ-VAUN — the hoarder's cache **[real puzzle]**
- **Entry.** A room of full chests found in the Warrens; the empty labelled `given back` column
  (`vaun-hoard-sorted`).
- **Pays.** Lore (Vaun's tragedy, FACT 5) + a door (his Caesar stone) + the mercy beat (the group
  gives back what he wouldn't — per-keeper agency, OVERHAUL Pillar 1).
- **Thread / keeper** `who` / Vaun. **Storylets:** `vaun-hoard-sorted`, `vaun-bookshelf-tally`,
  the Caesar stone. **Tone:** Archive → small Warm-Grief.

### SQ-MARA — the reader who never walked **[real puzzle]**
- **Entry.** The Kept-Light lectern shelf (`page-line-word`, built) or the five annotated lecterns
  (`mara-lectern-lock`).
- **Pays.** Lore (Mara, FACT 5 + FACT 13 seed) + the "walk what you read" embodied beat + the
  untainted relief of her kinder margin note (new doc `the-margin-she-kept.md`).
- **Thread / keeper** `who` / Mara. **Storylets:** book-cipher, `mara-lectern-lock`,
  `mara-walk-the-map`. **Tone:** Archive → Warm-Grief (the exhale beat).

### SQ-SELLA — the drowned child **[real puzzle]**
- **Entry.** The blank shore keeper-stone + the pool (`sella-reflection-bearing`) or the far-water
  copybook (`dest-far-water`, built).
- **Pays.** Lore (Sella, FACT 10 seed) + a wordless joy drawing (untainted relief) + coords into the
  Undercroft path.
- **Thread / keeper** `who` (+ `surface` deep-bird) / Sella. **Storylets:** atbash stone,
  `sella-reflection-bearing`, `sella-overlay-lake`, `sella-shore-memorial`. **Tone:** Uncanny →
  Warm-Grief.

### SQ-ORIN — the mason who would not bow **[real puzzle]**
- **Entry.** The threshold-stone legible only from the crouch (`i-thought-it-small`, built) or the
  marker row (`orin-bow-fall-order`).
- **Pays.** Lore (Orin, FACT 5 + FACT 6) + his atonement made playable (the group bows where he
  wouldn't) + a maker's-mark glyph toward `unkept`.
- **Thread / keeper** `who` / `happened` / Orin. **Storylets:** substitution stone,
  `orin-bow-fall-order`, `orin-banner-heraldry`, `orin-frame-dials`. **Tone:** Uncanny → Archive.

### SQ-BRANN — the watchman on the black moon **[real puzzle]**
- **Entry.** His night-only journal (`do-not-close-your-eyes-here`, built) or the silent watch-walk
  (`brann-silence-corridor`).
- **Pays.** Lore (Brann, FACT 5 + FACT 11 + FACT 12) + the black-moon toll + the sculk-as-Watcher's-
  ear foreshadow.
- **Thread / keeper** `who` / `surface` / Brann. **Storylets:** beacon/colour stone,
  `brann-black-moon-toll`, `brann-silence-corridor`. **Tone:** Uncanny (temporal dread).

### SQ-ISS — the liar **[real puzzle + reactive drama]**
- **Entry.** His warm wall-doctrine (`the-ways-are-a-wall`, built) + the contradicting later stone.
- **Pays.** The catch (`iss_caught`) — re-reads his whole tree cold, re-opens the Seventh thread,
  gates the companion reveal.
- **Thread / keeper** `iss` / spine / Iss. **Storylets:** Vigenère stone, `iss-which-is-true`,
  `iss-nbt-falsified-entry`, `iss-bound-word-callback`. **Tone:** Archive → cold Uncanny (the flip).

---

## 3. THE UNDERCROFT CLUSTER — the fog world + the room that rebuilds wrong

### SQ-UNDER — the descent into the fog **[real puzzle + flavor]**
> Source: `backlog-undercroft-dimension.md`. The narrative/site/descent gate already exist; the
> orphan this closes is the "room rebuilds itself" swap (currently only narrated, never enacted —
> `undercroft-fog` is a pure `lore` row with no beat). Honest fix: either wire the room-swap beat
> **or** demote the narration to flavor. Designed here as a *real* reactive beat.
- **Entry.** Past the Deep Line, the descent to the sealed Undercroft (Orin sealed it, WORLD-BIBLE
  §2). The `undercroft-fog` world (a Multiverse void world + fog datapack — a GO-LIVE manual step).
- **The core beat (the orphan killed).** The group passes through a room; when they return (or when
  they cross a threshold), the room has **rebuilt itself wrong** — a `RoomSwapBeat` teleport to a
  wrong-scaled twin (OVERHAUL §5: `RoomSwapBeat` → sealed-door teleport, not in-place). This makes
  "the deep is older + wrong-scaled" (WORLD-BIBLE §1) a *felt* enacted beat, not narration.
  **[real puzzle]** — the answer is comprehension (the group notices the room changed; the noticing
  is detected) + a solo per-player illusion layer (a block only one of them sees, OVERHAUL Pillar 3).
- **Pays.** The single kept fire (FACT 11) — the one lit point in a doused world; the "the kept ones
  did not depart, they were kept" beat (FACT 12); the on-ramp to the Accepting.
- **Thread** `place` / spine. **Tone:** deep Uncanny. **Async note:** the fog world persists; a solo
  player can descend and advance the noticing beat. **Dynamic-roster:** the wrong-scaling and the
  single fire are group-agnostic; no role assigned to a person.
- **Relief split (OVERHAUL Pillar 2):** the single kept fire is a *diegetic* safe point (never a
  gamey healing chest) — the honest "ways that companied the Dark" exhale.

---

## 4. THE SEVENTH CLUSTER — the cast-out keeper, restore or erase

### SQ-SEVENTH — the seventh below **[real puzzle + reactive drama]**
> Source: `the-seventh-spine.md` + canon-spine §5 + the-seventh-below.md (v2). The Seventh is now
> the **active goal** (OVERHAUL §0), not just an optional absence. This cluster is the spine's heart,
> but its *entry* and much of its body read as an optional side-mystery until the catch pulls it in.
- **Entry (M1→M2, soft).** A stray glyph implies a **seventh** where the record says six (canon-spine
  §5); the dead-shrine (the false walk, `spine-cold-hearth-shadow`) yields the *question*, not an
  answer.
- **Body (post-`iss_caught` + `seventh_named`).** The hearth-deep below the cold hearth opens
  (`the_unwriting`, WORLD-BIBLE §11.1): three chambers — the seal-is-a-name, the unwriting wall
  (rail-fence rails=6, recovers what was unwritten, sets `seventh_named`), the unfinished hearth-deep.
- **The choice (`seventh_choice ∈ {restore | erase}`).** Restore the Seventh's name (re-warm the
  hearth, the `inheritors` codicil) or leave it erased. **Colors, never gates** (canon-spine §5,
  INV-12): one tinted M5 clause + one persistent block-state.
- **The reunion (finale).** The future-dated grave that opens from the inside — the Seventh, alive,
  waiting (OVERHAUL §0, the-seventh-below.md §"the reunion"). The group carries the six keepers'
  testimony + Iss's proof down and **corrects the record**. **[reactive drama]** — the finale is
  authored dialogue + a chosen sacrifice, not a submit-answer.
- **Pays.** The whole emotional payoff (D9: economy of mystery — every answer a bigger question until
  the reunion pays off emotionally AND concretely).
- **Thread** `seventh` / spine. **Storylets:** the stray-glyph seed, `spine-cold-hearth-shadow`
  (false walk, dead-end-with-teeth), the three `the_unwriting` chambers, the reunion.
- **New/updated lore:** the-seventh-below.md (built, v2); the End shrine `the-name-i-cut-myself.md`
  (built, reconciled); new doc `the-first-day-i-stopped-counting.md` (§6) deepens the Seventh's wait.

---

## 5. THE OTHER CLUSTERS (external surfaces, town, cold-start, herrings, bird, dimensions)

### 5.1 SQ-COLD — the cold-start ignition **[flavor / atmosphere → lure]**
> Sources: `cold-start-prologue.md` + `cursed-map-frame.md`.
- **Entry.** Week-zero, before anyone knows there's a game: ONE anomaly juicy enough to screenshot
  to Discord unprompted — the **cursed adventure-map frame** (a map item that knows a number it
  shouldn't; the frame-break vignette). The FIRST notice is deliberately findable (cold-start §1).
- **Pays.** Ignition (`prologue_ignited` — the OVERHAUL Phase-1 ignition the engine currently lacks).
  Atmosphere, not a submitted answer. **[flavor / atmosphere]** — it lures, it does not gate.
- **Thread** spine (M1). **Grounding/consent (OVERHAUL §4):** the "knows a number it shouldn't" is a
  *real* observed value (the group's own world seed / a real count), never fabricated. Session-zero
  disclosure precedes it.
- **Async/roster:** any one friend can trip the anomaly; it announces to all.

### 5.2 SQ-WALKS — the two expeditions **[real puzzle]**
> Source: `coords-to-real-place.md` + WORLD-BIBLE §11.2 / §11.1.
- **The false walk (pre-catch).** Iss's first coordinate delivers the group to the dead-shrine — a
  cold hearth at the end of a grown-over path (the warm lie's dead end). Answer = the on-site word
  (INV-14), outcome `dead_end` with teeth. **[real puzzle]** but honestly a red-herring destination.
- **The true walk (post-catch).** The Threshold's carving yields the true coordinate to the
  Accepting on-ramp — a door standing open, markers facing inward. Answer = the on-site word.
- **Pays.** The matched-pair lesson (salvation was never *out*; the liar sent them out, the catch
  sends them down) + the on-ramp. **Thread** `place` / spine.
- **Async/roster:** the walk is the longevity (a group shares a rumor in a second but not the
  1–3k-block walk — matches `side_quests.sql` design). Solo-walkable.

### 5.3 SQ-COUNT — the base book that counts down **[flavor / atmosphere]**
> Source: `counting-base-journal.md` (pairs `record-writes-you-in.md`).
- **Entry.** A book in the group's OWN base shows a number, decreasing, in a dead keeper's hand,
  counting down to nothing explained (engine: `BookAppearsBeat` / `LecternFillBeat` page-swap).
- **Pays.** Dread + the FACT 13b payoff (the count is the appointment for the Accepting; the grave's
  date). **[flavor / atmosphere]** — there is nothing to submit; it *reacts* (the number ticks as
  flags flip). Honest: not a puzzle.
- **Thread** spine (M1→M5). **Grounding:** the number is derived from real progress, never faked.

### 5.4 SQ-HERRING — the dead-ends that talk back + the false prophet **[reactive drama]**
> Source: `dead-ends-with-teeth.md` (bestiary *lore* salvaged from the CUT
> `backlog-modeled-mob-and-voice.md`, OVERHAUL §5).
- **Entry.** A cipher/thread that decodes to a **taunt** — the Watcher acknowledging you solved a
  thing that meant nothing — plus a false-prophet voice (a Wenna-style folk-charm rumor that is
  confidently wrong, Aro's lie made into a thread). These self-acknowledge (OVERHAUL §5 KEEP: a dead
  lead arrives and flips to a "contradicted" card — honest, not inert).
- **Pays.** Atmosphere + the anti-speedrun tax + character (Aro the liar-broker, `npc-and-watcher-
  voice.md` A1). **[reactive drama]** — honest red-herrings that never block (D8: wrong attempts
  still produce content).
- **Thread** any (`place` / `happened`). **Existing seed hook:** the five deliberate dead leads in
  `side_quests.sql` (`dest-warm-town` = Aro's lie, already built).

### 5.5 SQ-BIRD — the deep-bird vigil **[real puzzle + flavor]**
> Source: `herd-conversion.md` (~80% built: `paceHerd`, `SacredAnimalBeat`, FACT 15/INV-12/INV-13).
- **Entry.** The bird-coops (`dest-bird-coops`, built) + the one **true glowing deep-bird** among a
  cosmetic Pale herd (INV-13: only the glowing one is conduct-tracked and the only one that glows;
  the herd never glows and is never a violation).
- **Pays.** The `the_sacred_beast` custom taught + a seed-cake item + the slow herd-conversion
  atmosphere (the Pale herd = this group's present-tense conversion of the living, WORLD-BIBLE §12.1,
  distinct from the soul-sand deep-time dead). Killing the true bird is a permanence fork (INV-12,
  colors the ending). **[real puzzle]** (keep the glowing one) **+ [flavor]** (the herd).
- **Thread** `surface` / Sella (she kept the deep-bird). **Fairness:** the fork-arming Beast is
  always the glowing one, so the irreversible fork is always avoidable (INV-13).

### 5.6 SQ-NETHER + SQ-END — the two optional deepening lanes **[flavor / atmosphere]**
> Source: `minecraft-progression.md` + WORLD-BIBLE §12. Both **gate nothing** (INV-12); skip them
> and the whole Overworld arc stands.
- **SQ-NETHER (the fire-source, "below the below").** A lit portal → a near pocket with a prior
  keeper kept *as* the fire. Vanilla blocks ARE the lore (soul sand = deep-time dead older than the
  first keeper; bastions/fortresses = founders who went furthest and came back wrong-scaled). Pays:
  the Kept-Light custom's ORIGIN (keeping was always a *carrying*) + `nether_forge_found` +
  Whisper budget. **[flavor / atmosphere]** — reading blocks, not submitting answers. **BUILD GATE:**
  blocked on the FACT-11 source-clause (now SEALED, canon-spine §3 FACT 11) — clear to build.
- **SQ-END (exile / the Seventh's absence).** The one place outside the record. Two static set-pieces:
  the Seventh shrine (`end_seventh_shrine`, sets `seventh_seen_out`) and the exile-hold
  (`end_exile_hold`, disabled until its INV-16 binding is built). Pays: *why* the Seventh chose exile,
  deepening the reunion (WORLD-BIBLE §12.2 reconciliation: the exile is the Seventh's PAST; they came
  back down). **[flavor / atmosphere]** — vistas + one carving; zero apparition lane (the End is
  outside the record). **Roster/INV-16:** the exile-hold names no living player, rhymes only on a
  chorus all share.

### 5.7 SQ-REC — the record (the discovered website) **[real puzzle + reactive drama]**
> Source: `arg-leaves-the-game.md` + OVERHAUL Pillar 4.
- **Entry.** A URL hidden in-game (a carved sign, an NBT item field) → the record website, a
  half-corrupted archive terminal of the Hold's own record-keeping (degraded, half-redacted, entries
  out of order, integrity warnings).
- **Pays.** Four surfaces in one artifact (OVERHAUL Pillar 4): the ledger (the group's names write
  in), the hint rail (an "integrity check / error log" that clarifies as a thread stalls), the Iss
  lie (falsified entries the group **corrects** — the `iss-nbt-falsified-entry` payoff), the Seventh's
  true record (restored as flags flip). Between-session "the record remembers…" memory.
- **Thread** spine (all movements). **[real puzzle]** (the answer-input field = diegetic remote
  submission) **+ [reactive drama]** (the ledger/hint-rail react without a submit). **Security
  (OVERHAUL Pillar 4):** reads via RLS / edge-function only; never the service key in the browser.
- **Discord note:** no game-persona in Discord; permitted only **corrupted artifact leaks** on
  in-game triggers (a corrupted OGG of the group's own VC, a PFP of what they're looking at) —
  grounded echoes of the recovered system, never a character posting (OVERHAUL Pillar 4). Sequence:
  Observer capture must exist first.

---

## 6. NEW LORE / SIDE-STORY DOCUMENTS WRITTEN (this pass)

Authored into `arc/lore/documents/` in the corpus register (lowercase, sparse, in-character,
matching each author's voice), deepening the world without contradicting canon:

1. **`the-margin-she-kept.md`** (Mara) — the untainted keeper-memory relief beat for SQ-MARA (the
   kinder margin note that survives the reckoning, OVERHAUL Pillar 2). Voice: page-refs, cool, lonely.
2. **`the-hoard-that-was-a-door.md`** (Vaun) — the cache lore behind `vaun-hoard-sorted` /
   `vaun-bookshelf-tally`; Vaun speaking only of what he kept, the door his refusal became.
3. **`the-first-day-i-stopped-counting.md`** (the Seventh) — deepens the Seventh's long wait for
   SQ-SEVENTH; the clearest, most direct of the seven hands (the-seventh-below.md register), a small
   scene of the day they stopped marking the days.
4. **`stay-close-a-page-torn-out.md`** (Wren, post-reveal) — a single torn leaf from the "kept close"
   tally (the-companion.md §6), his warm hand turning forensic; **surfaces only post-`companion_
   revealed`**, honest [reactive drama], never a puzzle.

Each pays a canon debt (WORLD-BIBLE §8 fragment→revelation ledger) and lists its thread + facts in
frontmatter. See the docs themselves for the debt tags.
