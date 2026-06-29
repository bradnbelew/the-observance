# IDEA — The Seventh as a Real Optional Spine (restore or complete the erasure)

> Design treatment. Spoiler-bearing (reads `arc/`-sealed material). For the synthesis
> phase + the build, not for Ethan-unspoiled.
>
> Read-grounded against: `arc/lore/documents/the-seventh-not-kept.md` (D11),
> `arc/bestiary-sealed.md §2.6` (the anti-creature), `arc/lore/canon-spine.md` (§3
> FACT 10/15, §5 the Seventh thread, §6 rules), `design/bestiary.md §2` (player-facing
> rumor), `design/cipher-web.md` (the 11 ciphers + the fairness contract),
> `plugin/.../beats/lib/SmallStructureBeat.java`, `discord/supabase/seeds/puzzles_seed.sql`
> (`seventh-shrine` already exists), `plugin/src/main/resources/sites.yml` (`the_cold_hearth`
> already sited), `discord/src/voice.ts`, `FLOW.md §3` (Whisper economy).

---

## 0. WHAT EXISTS TODAY (re-grounded 2026-06 — most of this idea is ALREADY CANON)

> **STATUS CORRECTION (read first).** An earlier draft of this very file treated the
> Seventh-spine as a thing still to be *promoted and minted*. Since then, synthesis has
> already canonized and seeded most of it. The grounding pass below is the **current
> truth on disk**; §4 and §7 are corrected to point at the *real* remaining gaps, not at
> work that is done. The verdict is therefore **keep-SCALED-to-completion**: build the few
> missing realizers, do NOT re-mint flags/FACTs that already exist.

**Already canon + seeded (do NOT re-add):**

- **The restore/erase spine is canon.** `canon-spine §3b` carries **FACT 10b** (SEALED
  child of FACT 10 — *"the land refused a keeper who broke nothing"*) and §5 carries the
  `seventh_choice ∈ {restore | erase}` resolution (the INHERITORS codicil merge, the
  two-distinct-places anchor law). **FACT 10b is already minted — do not mint it again.**
- **The puzzle rows exist** in `puzzles_seed.sql`: `seventh-shrine` (chamber-1 lore +
  `seventh_found` + `whisper_budget_earned`), **`seventh-unwriting`** (rail-fence rails=6 →
  `seventh_named`, `main_beat`, `active:false` staged), **`seventh-cause`** (correlation
  lore, FACT 10b, Whisper budget), **`seventh-choice`** (two opaque wordless tokens →
  `seventh_choice` + `ending_codicil`, `active:false`). Plus `fork-light` / `fork-name`
  siblings. **All seeded already.**
- **Breadth ledger rows exist** in `seventh_seed.sql`: `dest-unwriting-deep`,
  `dest-fire-let-out` (under thread `who`), routing the Recovery Archive cards.
- **The two places are sited** in `sites.yml`: `the_cold_hearth` (surface = Iss's
  dead-shrine grave) and **`the_unwriting`** (type `seventh_shrine`, the hearth-DEEP, with
  a one-site break-whitelist for the rite). The deep opens only post `iss_caught` +
  `seventh_named` — temporally layered, one anchor (`WEB-MASTER §0.4`).
- **The NPC node exists:** `KeeperNpcBeat` payload references `keeper.seventhChoice.offer`.
- **The bestiary entry exists** (`bestiary-sealed.md §2.6`): the Seventh is the **one keeper
  the land refused, so the one keeper that is NOT a creature** — an *anti-creature*. If
  glimpsed, *retreating*, never named.
- **D11 `the-seventh-not-kept.md` exists** — the effaced map-note, a small masterpiece of
  the iceberg ("*it has not yet said which we are.*").

**The real gaps (what this idea actually buys — see §4/§7):**

1. **`SeventhChoiceListener.java` DOES NOT EXIST.** It is referenced in `plugin.yml` and in
   the `seventh-choice` seed comments, but there is **no Java file on disk**. This is the
   single largest gap — without it the seeded `restore`/`erase` tokens can never be posted,
   so the whole choice is inert. **This is the P1 build.**
2. **No cold-shrine set-piece.** `the_unwriting` is sited with null coords and there is no
   authored `SmallStructureBeat` schematic for the unwriting chamber + doused hearth. The
   spine has a flag and a name but **no place to stand**.
3. **No D-new cause document.** `seventh-cause` / `dest-fire-let-out` reference a
   cause-fragment (`the-fire-they-let-out`) that is **not authored as a document file**.
4. **No M5 tint clauses bound.** The composer reads `seventh_choice`, but the two tinted
   Keeper clauses (restore / erase) are not authored in the keeper register.

**The promotion, restated honestly.** The *design* is done. This idea = **build the
realizers that make the canon spine FELT**, scaled by priority. It remains the canon home
for **"some laws are lies"** (FACT 8 generalized via D11's "a wall does not choose") and
**"you are becoming them"** (the felt edge of FACT 15 from the *refused* side) — both
already sanctioned by canon; the job is to make them land in the world, not to re-author
the canon.

---

## 1. EXPOUND — the full mechanic + story + mystery treatment

### 1.1 The shape: a three-chamber descent behind one cold hearth

The cold hearth is not the payoff anymore; it is the **door**. Standing in it still does
everything it does today (lore, Whisper budget, FACT 10). But a player who *reads the
hearth* (the effaced carvings, now legible only because of literacy earned elsewhere)
finds that the doused fire sits on a **sealed deep** — D11's own words: "*the deep below
it is sealed and the hearth in it is cold.*" The spine is the unsealing of that deep, in
three small chambers, each a `SmallStructureBeat` set-piece pasted out of line of sight:

1. **THE COLD HEARTH (entry).** Already built. The doused hearth, the effaced map-note
   (D11), the unanswerable count. New: a single carved line on the hearthstone that only
   resolves once the group can read the digit/sign glyphs (Stone of Reckoning literacy) —
   a **bearing down**, not across. This is the seam: today it's a leaf; now it has an edge.
2. **THE UNWRITING (middle).** A small sealed antechamber below the hearth. Its walls are
   a **record with a hole in it** — six keeper-names carved whole, and a seventh slot
   **scraped to bare stone**, the same blade-scrape as D11's own hand. The puzzle here is
   not to decode a name; it is to **reconstruct what was removed** by reading the *negative
   space* — the six are carved in a fixed order, and the gaps between them (kerning,
   really) encode the missing seventh by the **rail-fence** transposition (P7): the
   scraped letters were never gone, they were *displaced into the spacing of the others*.
   This is the literal mechanic of erasure made into a cipher: the name is still here,
   smeared across the six who were kept.
3. **THE SEVENTH'S HEARTH-DEEP (terminal).** The sealed deep itself — a tiny stone cell
   with **two things**: a cold hearth-pit (the place a fire was let die) and a **bare
   marker-socket** (the place a stone was never set). This is where the irreversible
   choice is performed (§1.4). Nothing here is a monster; the only apparition the whole
   spine permits is the §2.6 anti-creature — a single retreating glimpse, once, on
   *entry* to chamber 3, seen leaving down a passage that ends in wall.

All three are FAWE-`.schem` set-pieces (`SmallStructureBeat` schematic path,
`MAX_SCHEM_VOLUME`-bounded, footprint-validated, reveal-disciplined, idempotent). They
paste **only while unwitnessed**; the group descends into rooms that are "*already there.*"

### 1.2 How it threads across the ~2-week / 5-movement arc

- **Movement I (THE NOTICE).** The seed is *already laid* and untouched: the shore
  miscount ("six… and a seventh mark") and the first report's "*a seventh mark the record
  will not keep.*" The spine adds **nothing** to M1 — its job is to stay a rumor (§0).
- **Movement II (THE KEEPER-STONES).** Sella's Atbash bearing (`stone-sella`) hands the
  way to the cold hearth, exactly as today. The group that follows it reaches **chamber 1**
  and the `seventh-shrine` lore + Whisper budget fire (unchanged). New: chamber 1's carved
  *bearing down* is **illegible** until the group has both literacies (rune alphabet AND
  the Stone of Reckoning digits) — so the deeper spine naturally opens *after* the group
  has done real Movement-II work, without a hard gate. If they have the literacy, chamber 1
  reads "*below the cold hearth, the deep is sealed; the seal is a name.*"
- **Movement III (THE UNDERCROFT / THE SEVENTH).** This is the spine's home Movement. The
  Undercroft (the kept descent, the unbroken light) and the Seventh's hearth-deep (the
  *un*kept descent, the doused light) run as **deliberate mirrors** the group can hold
  against each other — FACT 11 (the one fire that never went out) vs the one fire that was
  *let* go out. Chamber 2 (THE UNWRITING, rail-fence) is solved here. Solving it sets
  `flags.seventh_named` and reveals the Seventh's reconstructed name (an in-corpus word,
  never a modern name — see §3) plus **why** they were erased: D-new (§4) supplies the
  cause as a *found cause*, not an explanation.
- **Movement IV (THE CATCH).** The Iss thread catches here, and the spine **rhymes** with
  it without merging. Iss said the ways are a wall (a comforting lie); the Seventh proves
  the ways are not a wall, because **a wall does not choose** (D11's own argument, verbatim
  in the doc). The catch that disproves Iss and the spine that proves the land *chooses*
  are **two doors onto "some laws are lies."** Canon keeps Iss ≠ Seventh (`canon-spine §5`):
  the spine may now (post-`iss_caught`) drop **one** ambiguous late line that a sharp group
  reads as "*were they the same hand?*" and the world never answers. Chamber 3 becomes
  **enterable** once the group has `seventh_named` — i.e. only a group that did the deep
  work reaches the choice; everyone else's Seventh thread simply rests as it does today.
- **Movement V (THE ACCEPTING).** The irreversible choice (§1.4), if made, is **already
  set** as a flag before the rite. At the Accepting it acts as a **colorant** on the
  ending the group already earns by the rite — it does not add a gate, does not elect a
  player, does not block an absent member. `restore` and `complete` each tint the
  Keeper's final register and one persistent-world detail (§1.5). A group that never went
  down gets the **neutral** ending — fully complete, missing only this one shading.

### 1.3 The puzzles, concretely (reusing the built ciphers)

| Chamber | Puzzle | Cipher (built) | Key source (in-corpus) | Outcome |
|---|---|---|---|---|
| 1 — Cold Hearth | the bearing *down* | **coordEncode** (P6) + **substitution** (P4) | Stone of Reckoning digits + rune alphabet | `next_clue` → chamber 2 (only if both literacies) |
| 2 — The Unwriting | reconstruct the scraped seventh name from the kerning | **rail-fence** (P7) | rail count = **6** (the six who were kept — a *counted in-world quantity*, the six name-carvings on the wall) | `main_beat` → `flags.seventh_named` + reveals the name (lore) |
| 2 — (correlation) | *why* erased | cross-doc (D11 × D-new §4) | hold the map-note against the cause-fragment | `lore` (FACT 10 deepened: the land can refuse, and here is the shape of a refusing) |
| 3 — Hearth-Deep | the choice | **ritual / arrangement** (P16/P15, *detected, not typed*) | the bare socket + the cold pit; two performable acts | `main_beat` (plugin-posted sentinel) → `flags.seventh_choice = restore | erase` |

Rail-fence is the **right** cipher here and it is not arbitrary: erasure *is*
transposition. The seventh's letters were not destroyed, they were **read off into the
spacing of the six** — `railFence.encode` writes letters down the rails and reads them
off row by row, which is exactly "the name smeared across the others." Rail count 6 =
the six kept keepers, a number the group literally counts on the wall (cross-ref P12).
Fair-but-hard: the *insight* (the gaps are the cipher) is the difficulty, not a wiki.

### 1.4 The irreversible choice (the heart)

Chamber 3 holds two performable acts, **neither typed**, both detected by listeners the
engine already has patterns for (the rite/arrangement modality, `AcceptingRiteListener`
sibling). There is **no menu, no "press 1 for restore."** You do one of two things in the
world:

- **RESTORE the name.** The group **sets a stone in the bare socket** (place a marker
  block) and **lights the cold pit** (a fire where the fire was let die), then **all
  present bow** at the socket (synchronized `PlayerToggleSneakEvent`, the Bow custom,
  ACTIVE players only). Mechanically: a marker placed + light + group bow at
  `the_cold_hearth` deep → `flags.seventh_choice = 'restore'`. Meaning: *the group learned
  why the seventh was erased and judged the erasure a lie — they re-keep the unkept.*
- **COMPLETE the erasure.** The group **scrapes the last six names** too — breaks the six
  carvings in chamber 2 (the only place in the entire game where breaking a protected
  marker is *the intended act*, whitelisted at this one site), leaving the record fully
  blank, and **douses any light they carried in**. Mechanically: the six name-blocks
  broken at the unwriting wall + no light left → `flags.seventh_choice = 'erase'`. Meaning:
  *the group sided with what unmade the seventh — "if the law can refuse one, the law is a
  lie, and a lie is better unwritten." They become the thing that erases.*

**Irreversibility, anti-jank-safe.** The flag is set once and the spine's chamber-3
listener self-disables for the session after either act (idempotent, `solves`-guarded via
the sentinel). The *world* changes are real but bounded: `restore` adds a marker + a lit
hearth (permanent, protected — it is a *built keeper-stone*, the only fully-warm thing the
spine produces); `erase` breaks six carved blocks at one whitelisted site and leaves bare
stone. **Neither toll touches a player's own blocks** (DESIGN §3 reversible-toll law is
about *warmth, not the players' build* — here the only things changed are the spine's own
authored set-piece). The kill-switch still mutes everything. Crucially: the choice is
**irreversible in fiction and in flag**, but it is *not* a loss condition and not a gate —
a group can refuse to choose (leave chamber 3) and lose nothing but this shading.

### 1.5 What the choice colors at the ending (the payoff that isn't a gate)

At the Accepting (FACT 14 "the record receives you"), the Keeper's final register and one
persistent detail tint by `seventh_choice`:

- **neutral (never went down):** the ending as authored. No reference to a seventh.
- **`restore`:** the Keeper's close carries one extra clause — the record now keeps a
  *seventh* mark again, said back. In the persistent world, the marker the group set in the
  hearth-deep **remains, lit**, the only re-warmed cold hearth on the map: a kept thing the
  group chose to keep when the land had refused it. Thematically: the group proved the land
  *can be argued with* — and chose mercy over law. (Felt, never stated: this is the group
  rehearsing what will be done *to them* — they just inducted a seventh; they are about to
  be inducted.)
- **`erase`:** the Keeper's close is *colder by one degree* and **shorter** — a name fewer
  to say. The persistent world keeps the unwriting wall **blank**, all seven gone. The
  group sided with unmaking. (Felt, never stated: "*you are becoming them*" lands hardest
  here — the group's first act as near-keepers was to **erase a keeper**, which is exactly
  what the watching did to the seventh. They are already doing the watching's work.)

Neither tint names FACT 15. Both are delivered by **what the Keeper says** (one clause of
register, drawn from `voice.ts`) and **one persistent block-state**, not by exposition.

---

## 2. CRITIQUE — adversarial and honest

**R1 — Orphaned-mechanic risk: the "break a protected marker" exception.** The `erase`
path requires the *one* place in the game where breaking a protected block is intended.
This is a real anti-jank smell: the whole engine protects markers; a whitelisted
"break-me" site is a special case that could (a) confuse the anti-grief listener, (b) read
as a bug ("wait, I CAN break this one?"), (c) be triggered accidentally.
**Mitigation:** scope the whitelist to a single `site_id` (`the_unwriting`) **and** gate
it behind `flags.seventh_named` AND a deliberate multi-block act (all six, within a window)
— a stray pick swing breaks one block, which **self-restores** (normal protection) unless
the group is mid-choice with the flag set. So accidental triggering is impossible: you
cannot "complete the erasure" without having already done the deep work and broken all six
on purpose. The listener logs the act distinctly so it never reads as anti-grief failure.

**R2 — Precision / "it knows me" law.** The spine is *group* conduct, not per-player
profiling — good, it can't mis-fire an "it knows you." But the §2.6 retreating apparition
on chamber-3 entry must **not** be biased to a profiled player (that would be a callout in
the one place the arc is about the *refused*, the un-profiled). **Mitigation:** the
hearth-deep glimpse is **un-targeted** — fired to whoever enters first, never selected by
dossier. It is the single apparition in the bestiary that is *deliberately not* grounded
in a measured signal, and that is **correct here**: the Seventh is the one the land did
*not* measure, did not keep, has no signal for. The absence of profiling *is* the
characterization. (This is a principled exception to anti-jank #6, documented as such.)

**R3 — Collective-ending law: does an irreversible choice punish an absent member?** No,
if built right, but the failure mode is real: if the choice required *all* members, an
absent friend blocks it. **Mitigation:** the rite is **ACTIVE players only** (same rule as
the Accepting, `cipher-web §2.2 GATE`), and refusing/missing the choice costs **only the
shading**, never progress. The neutral ending is whole. No member's absence can deny the
group the Accepting.

**R4 — On-camera confusion: "is this the Iss dead-shrine or the Seventh?"** They are the
**same cold hearth** by canon (the_cold_hearth = Iss's grave = the Seventh's shrine), and
that is a feature (one door, two readings) but a camera risk: a viewer could think the
arc contradicts itself. **Mitigation:** the layering is *temporal*, not spatial — in
Movement II it reads as Iss's dead-shrine (a grave, a `dead_end`); only after
`iss_caught` (M4) and `seventh_named` does the **deep below** open. Same place, deeper
the second time. This is the arc's signature "re-walk a solved clue" move (the Iss
engine) applied to a *location*, and it is on-theme, not a contradiction. The `arc/`
ambiguity line (R5 below) is the only place the two are allowed to touch.

**R5 — Iss/Seventh collapse risk.** Canon forbids collapsing them before M4
(`canon-spine §5`). The spine adds a post-`iss_caught` ambiguous line — that is the *one*
sanctioned flirtation. **Mitigation:** author exactly one line, place it behind the
`iss_caught` flag, and make it a *question the world refuses to answer* (de-slop §3
exemplar 4). Never a fragment that states they are the same. Keep canon default: distinct.

**R6 — Scope / "is this too much for a side thread?"** Three FAWE schematics + a rail-fence
puzzle + a correlation doc + a detected ritual + ending tints is **non-trivial content**.
If the vertical slice is at risk, this is the **first thing to scale down**, because it
gates nothing. **Scaled-down version (the honest cut):** ship **chamber 1 + the choice**
only — skip chamber 2's rail-fence and the middle, and make the choice readable directly
off the hearth-deep (set-a-stone vs leave-it-cold) once `seventh_found` is set. That keeps
the irreversible restore/erase flag and the ending tint (the *whole point*) at ~1/3 the
build cost, and the rail-fence "erasure-as-transposition" puzzle becomes a **P2-depth
add** for later. **Verdict: keep-scaled.** Build the choice + the ending tint as P1
(it is the thematic payoff); build chambers 2–3's full puzzle vertical as P2.

**R7 — "some laws are lies" must not blurt FACT 15.** The danger of giving the spine the
"you are becoming them" theme is that *erase* especially wants to say it out loud.
**Mitigation:** the theme is delivered **structurally** — the group's *action* (erasing a
keeper) is the watching's own action; nobody narrates the parallel. The Keeper's tinted
close is **register**, not exposition (one clause colder, one name fewer). Run every line
through de-slop (§3). If a draft line says "you are becoming the watching," it is wrong.

**R8 — Reveal discipline on a multi-block group act.** `restore` (place + light + bow) and
`erase` (break six) are *witnessed* acts by definition — the players do them in the light.
That is fine: the **players** act in view; the **world's** response (the flag, the ending
tint, the persistent state) is what stays disciplined. No block the *engine* sets is ever
witnessed appearing. The retreating apparition (chamber-3 entry) is the only engine spawn
and it obeys `mutateWhenUnwitnessed` / short `despawn_seconds` (`bestiary-sealed §2.6`).

---

## 3. DE-SLOP TEST — exemplar lines, in-voice, cold and concrete

Run against the ANTI-AI-SLOP LAW. No named emotions, no tidy bows, no "not just X but Y,"
plain declarative keeper/record register, the iceberg.

**(a) Chamber 2 — the unwriting wall, the record's third-person hand (lore on solve):**
> six names are cut whole here. the seventh was cut into the space between them. you have
> read it now. the record did not keep it; you did.

**(b) The cause-fragment (D-new §4), the effaced hand, why they were erased — withheld, not explained:**
> they did not break a custom. they kept all ten and were refused anyway. that is the thing
> the record could not hold and stay a record. so it let the fire out, and shut the door,
> and made the refusing into a thing that never happened.

**(c) `restore` — the Keeper's tinted close (one added clause, register only):**
> seven marks now. the seventh said back. you set a stone where the land set none.

**(d) `erase` — the Keeper's tinted close (shorter, colder by one degree):**
> the wall is bare. there is one fewer name to say. it is quieter here now.

**(e) The single sanctioned Iss/Seventh ambiguity line (behind `iss_caught`, a refused question):**
> two hands scraped this stone. the record does not say they were two.

All five: no exclamation, no adjective-stacking, no "testament," no announced feeling,
mundane concrete nouns (stone, fire, wall, name), the omission carrying the dread.

---

## 4. THREAD IT — exactly where this lives so it is not an orphan

### Canon FACTs touched / added
- **TOUCHES FACT 10** (acceptance is a choice the land makes, it can refuse) — the spine
  is FACT 10's deepest expression: it shows the *shape* of a refusing, and lets the group
  answer it. Already the `seventh-shrine` payoff; now extended.
- **TOUCHES FACT 8** (Iss lied; the ways are not a wall) — generalized to "**some laws are
  lies**": D11's "a wall does not choose" argument is the bridge. Two doors onto the theme
  (Iss catch + the Seventh's refused-despite-faithful cause-fragment, §4b).
- **TOUCHES FACT 15** (induction) from the *refused* side — felt only, via the group's
  `erase`/`restore` action mirroring the watching's own. **Never stated.**
- **ADDS (sealed, web-rule two-door) FACT 10b** — *"the land refused a keeper who broke
  nothing."* New sub-fact, home = the cause-fragment (§4b) + the unwriting wall (chamber 2).
  Two doors: the doc and the carved wall. (Record this in `canon-spine §3` as a child of
  FACT 10, sealed.)

### Found-documents / journals
- **D11 `the-seventh-not-kept.md`** — already the spine's spine. No edit needed; it already
  foreshadows "*it has not yet said which we are*" (the choice) and "*to be kept and to be
  cast out are one door*" (the restore/erase duality). **The plant is already in the file.**
- **NEW doc — `the-fire-they-let-out.md` (D-new, the cause-fragment)** — the effaced hand,
  one fragment, the *why* (§4b). `clue_bearing: true`, `movement: 3`, `links_to: [the-seventh-not-kept, the-ways-are-a-wall, no-wall-was-ever-built-here]`, `foreshadows:
  ["a refusal that kept no custom-fault (FACT 10b)"]`. Pairs with D11 as a correlation
  puzzle (hold both → the cause).
- **TOUCH `the-record-opens.md`** — already seeds "a seventh mark the record will not keep"
  (M1). No edit; it is the M1 plant.

### NPC / Watcher / Keeper voice lines
- **Keeper (M5 close):** two tinted variants (§3c, §3d) + the existing neutral close. New
  `voice.ts` keys: `keeperCloseSeventhRestored`, `keeperCloseSeventhErased` (the neutral
  close stays the default).
- **The record (chamber 2 lore):** §3a, carried by the existing `oracleLore(fragment)`
  path (fragment seeded in the puzzle row — no new voice key).
- **Iss ambiguity (M4, post-catch):** §3e, a single `lore` line gated on `iss_caught`.

### Ciphers / puzzles (reuse the 11)
- **rail-fence (P7)** — chamber 2, erasure-as-transposition, rail count 6. *Primary.*
- **coordEncode (P6) + substitution (P4)** — chamber 1's bearing-down (literacy gate).
- **correlation (meta-modality)** — D11 × D-new for the cause (FACT 10b).
- **ritual/arrangement (P16/P15, detected)** — the chamber-3 choice (set-stone+light+bow
  vs break-six+douse), plugin-posted sentinel, no typed answer.
- *(Atbash P2 already routes here via `stone-sella`; coord already teaches at Stone of
  Reckoning. No new cipher code — all 4 transforms ship today.)*

### Beats / listeners / tables / seed rows / sites / voice keys (real symbols)
- **Beats:** `SmallStructureBeat` (schematic path) × 3 chambers; `TorchGutterBeat` /
  no-light for the doused pit; `NamedMobBeat` (very short `despawn_seconds`, retreating,
  **un-targeted**) for the one §2.6 glimpse. No new beat class needed for the choice — it
  reuses the rite/arrangement detection pattern.
- **NEW listener:** `SeventhChoiceListener` (sibling of `AcceptingRiteListener`) — watches
  `the_cold_hearth` deep for the two detected acts, gated on `flags.seventh_named`,
  idempotent, posts the sentinel that sets `flags.seventh_choice`. Whitelists block-break
  at `site_id: the_unwriting` **only** while mid-choice (R1).
- **Schematics (deploy assets) — NEW:** the unwriting-chamber `.schem` (+ doused hearth)
  in `plugins/Observance/schematics/`, e.g. `seventh_unwriting.schem`. (The scaled cut, R6,
  needs only this one set-piece; the full three-chamber descent is the P2 depth add. Inline
  `blocks` fallback if FAWE is absent.)
- **arc_state flags — ALL ALREADY SEEDED:** `seventh_suspected`, `seventh_found`,
  `seventh_named`, `seventh_choice` (`'restore'|'erase'`), `ending_codicil`,
  `whisper_budget_earned`. **Do not re-mint.**
- **Seed rows (`puzzles_seed.sql`, kebab keys) — ALREADY SEEDED:** `seventh-shrine`,
  `seventh-unwriting` (rail-fence rails=6 → `seventh_named`, `active:false`), `seventh-cause`
  (correlation lore → FACT 10b), `seventh-choice` (two opaque tokens → `seventh_choice` +
  `ending_codicil`, `active:false`). Breadth rows `dest-unwriting-deep` / `dest-fire-let-out`
  in `seventh_seed.sql`. **The remaining seed task is only the `active`-flip plumbing** (the
  showrunner/`SeventhChoiceListener` flips `seventh-unwriting`/`seventh-choice` live once the
  deep opens) — not new rows.
- **sites.yml — ALREADY SITED:** `the_cold_hearth` (surface) and `the_unwriting` (type
  `seventh_shrine`, with the one-site break-whitelist). Remaining task is **placing real
  coords** + binding the set-piece to `the_unwriting`.
- **Plugin — THE REAL GAP:** **`SeventhChoiceListener.java` must be authored** (modeled on
  `signal/listener/AcceptingRiteListener.java` — same opaque-token-posts-to-oracle shape).
  It watches site-type `seventh_shrine`, arms on `seventh_named`, detects the two physical
  acts, posts the matching seeded token (`r7n4k2 m1x8p5 w3j6h9` restore / `e5t0b7 c2d4s8
  v6f1z3` erase), idempotent, reveal-disciplined permanent block-state via `ProtectedRegistry`.
- **voice keys — NEW clauses, register only:** two M5 Keeper tint clauses bound for the
  composer to select on `seventh_choice` (restore / erase). The puzzle-row lore lines reuse
  `oracleLore` / `oracleMainBeat` (already in the seed — no new oracle key). Re-scope the
  `keeper.seventhChoice.offer` node text to **non-prompting** (states the deep is open and
  cold; never presents a fork — see R1).

---

## 5. PLANT THE PAYOFF — the "OH, that is what that was for" seed

**Plant (Movement I, inert/ambiguous):** the **shore miscount** and the first report's
line "*a seventh mark the record will not keep*" (`the-record-opens.md`, already seeded).
At M1 this is pure texture — a count that doesn't come out even, easy to dismiss as a
damaged tally. **No mechanic touches it; it just sits wrong.**

**Plant 2 (Movement II, ambiguous):** D11's closing scratched hand — "*whatever it costs
to be kept — the seventh was spared it. i do not know yet whether that is mercy.*" Read in
M2 it is a melancholy aside. It is **secretly the choice prompt**: mercy (restore) vs the
cost-of-keeping refused (erase).

**Payoff (Movement III→V):** at chamber 2 the group learns the seventh's name was *never
gone* — it was transposed into the spacing of the six (rail-fence). The M1 miscount
**re-reads**: the tally never came out even because the seventh was *inside the six the
whole time*, smeared across them — exactly as the wall shows. Then at M5 the **tinted
ending** pays off D11's "*which we are*": the group's restore/erase act answers, in the
persistent world, the question the doc left open in M2. The "*spared it… whether that is
mercy*" line is the exact fork the group resolves by hand.

> Plant↔payoff ledger entry (for the seed-tracking discipline):
> - PLANT `seventh-miscount` (M1, report) → PAYOFF `seventh-unwriting` re-read (M3).
> - PLANT `d11-which-we-are` (M2, D11 close) → PAYOFF `seventh-choice` ending tint (M5).
> - No plant without payoff; no payoff without plant.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Element | Movement | Depends on | Depended on by | Priority |
|---|---|---|---|---|
| M1 miscount / "seventh mark" seed | I | — (already shipped) | the whole spine's plant | **P0** (exists) |
| `seventh-shrine` chamber-1 lore + Whisper budget | II | `stone-sella` Atbash OR miscount; cold hearth sited | chamber-1 deep edge | **P0/P1** (exists; extend) |
| Chamber-1 bearing-down (coord+subst literacy gate) | II→III | rune Rosetta + Stone of Reckoning placed | chamber 2 | **P2** |
| Chamber 2 — rail-fence unwriting → `seventh_named` | III | chamber 1; rail count 6 in-world | chamber 3; FACT 10b | **P2** |
| `seventh-cause` correlation (D11 × D-new) → FACT 10b | III | D-new authored | the theme "some laws are lies" | **P2** |
| Iss/Seventh ambiguity line | IV | `flags.iss_caught` | nothing (texture) | **P2** |
| **THE CHOICE — restore/erase → `seventh_choice`** | III(set)→V(color) | `seventh_named` (full) OR `seventh_found` (scaled, R6) | M5 ending tint | **P1** |
| `SeventhChoiceListener` + sentinel row + break-whitelist | III | `the_cold_hearth`/`the_unwriting` sited | the choice | **P1** |
| M5 ending tints (`keeperClose…` ×2) | V | `seventh_choice` | the payoff | **P1** |

**Net dependency story.** The spine **depends on**: the cold hearth being sited (done),
the two literacies (rune Rosetta + Stone of Reckoning — both sited, content-pending), and
Sella's bearing / the miscount as in-doors (both shipped). **Nothing depends on the
spine** — it gates nothing, by law; the Accepting and every other thread complete without
it. That is precisely what licenses the priority split:

- **P1 (arc-spine, build for the real run):** the irreversible **choice** + the **ending
  tint**. This is the thematic payoff ("restore/erase," "some laws are lies," "you are
  becoming them") and it is cheap in the scaled form (R6): chamber 1 (exists) → set-stone
  vs leave-cold at the hearth-deep → one flag → one tinted Keeper clause.
- **P2 (depth, build when the slice is green):** chambers 2–3 full vertical — the
  rail-fence unwriting, the cause-correlation/FACT 10b, the literacy-gated bearing, the
  Iss ambiguity line, the three discrete FAWE schematics.
- **Not P0:** nothing here belongs in the vertical slice; the slice proves the *kept*
  descent (Undercroft/Accepting), and the Seventh is its optional dark mirror.

---

## 7. ONE-SCREEN BUILD CHECKLIST (corrected to the REAL gaps — most rows already exist)

> NOT on this list because it is **already done**: FACT 10b (canon `§3b`), the flags
> (`seventh_*`, `ending_codicil`), the rows (`seventh-unwriting`/`-cause`/`-choice`,
> breadth rows), the sites (`the_cold_hearth`/`the_unwriting`), the bestiary entry, D11.

**P1 — make the choice fire and land (the spine made felt):**
1. **Author `SeventhChoiceListener.java`** (sibling of `AcceptingRiteListener`):
   site-type `seventh_shrine`, arm on `seventh_named`, detect the two physical acts
   (re-light vs scour), post the seeded opaque tokens, idempotent, reveal-disciplined
   permanent block-state, one-site break-whitelist at `the_unwriting`.
2. **Author the unwriting set-piece** — a `SmallStructureBeat` `.schem` (+ doused hearth)
   and **place real coords** for `the_unwriting`; bind the set-piece to that site.
3. **Author the two M5 Keeper tint clauses** (restore / erase — §3c/§3d) and wire the M5
   composer to select on `seventh_choice`.
4. **Re-scope `keeper.seventhChoice.offer`** to non-prompting register (§3 exemplar; R1).
5. **`active`-flip plumbing**: `seventh-unwriting` / `seventh-choice` flip live once
   `iss_caught` + `seventh_suspected` open the deep (no new rows).

**P2 — depth (build when the vertical slice is green):**
6. Author **D-new `the-fire-they-let-out.md`** (effaced hand, §4b) to back the seeded
   `seventh-cause` / `dest-fire-let-out` correlation.
7. The full three-chamber descent (bearing-down literacy gate + discrete schematics) +
   the single un-targeted retreating `NamedMobBeat` glimpse (§2.6).
8. The one sanctioned Iss/Seventh ambiguity line, gated on `iss_caught` (§3e).

**Always:** de-slop every authored line (§3); confirm **no line states FACT 15**; confirm
the choice is **wordless/unprompted** (R1) and **gates nothing** (INV-12); rail-fence
round-trip already self-tested; sentinel tokens already in normalized charset.
