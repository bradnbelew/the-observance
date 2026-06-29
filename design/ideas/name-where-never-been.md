# Your Name Where You Have Never Been

> Build-ready design treatment. One idea, fully threaded.
> Status: **KEEP-SCALED** (P1 arc-spine; one mechanic, two staged payoffs).

**PITCH:** A player's name, carved in the shared rune alphabet, in a location that
player has *never visited* (verified by the heatmap), and *discovered by a different
player*. Pure dossier flex: the land had your name before you walked there.

> **AS-BUILT RECONCILIATION (read first — this doc predates the code).** The pure
> WHO/WHERE selector is **already built and self-tested**:
> `discord/src/showrunner/name-where-never-been.ts` exports `selectCarve()`, with
> `name-where-never-been.selftest.ts` green. It ALREADY implements: the
> proof-of-absence gate (fires ONLY when T has a flushed visited-set that does NOT
> contain the anchor cell — "no proof → no carve"); chorus rotation (fewest-carved
> first, ties by name — never the divergence extremes, INV-16); ACTIVE-only; the
> `coordEncode` back-pointer as a navigation POINTER not an answer (INV-14); and
> consumes `groupVisitedCells` (group-avoided) ∩ per-player `visitedCells`
> (T-never-visited) exactly as §2e option B prescribes. `sites.yml` already has
> `carve_anchor_01..03` (placeholder coords). So §2e's "new selection logic" and
> §4 RISK-1's "the precision data does not exist" are **partially superseded**: the
> selector exists; what remains is the *data path + plumbing*, not the algorithm.
> The remaining build gaps are: (1) `LocationSampler`/`PlayerSignals` must WRITE a
> coarse per-player visited-cell set (today neither `PlayerSignals` nor
> `SignalSnapshot` carries one) → flush to a NEW `player_visited_cells` table (new
> migration; current `migrations/` holds only 0003–0005); (2) the run wrapper
> `name-where-never-been.run.ts` (NOT YET PRESENT — sibling pattern = `offline-skin.ts`
> + `autonomy.run.ts` A9 grave block) that reads the visited-sets, calls
> `selectCarve`, fires `SignWriteBeat`, records `name_carves`, and passes the
> offline-skin-occupied + `divided`-floor cells into the exclusion set (INV-16
> co-location); (3) voice keys + the M1/M4 documents. The body below still stands as
> the story/mystery/threading treatment; treat §2e/§4 as the *intent* the as-built
> selector now realizes.

**LIKELY ENGINE MAPPING (confirmed against code):** the showrunner picks an
**un-visited cell** from the heatmap for target T; `SignWriteBeat` carves T's name in
runes at an anchor inside that cell; precision-gated — only a name the tracker truly
holds, only a cell the heatmap proves T has zero visits to, only an anchor a
*different* player is statistically likely to reach.

---

## 1. EXPOUND — the mechanic, story, and mystery

### 1a. What the player experiences

Mid-arc, a player — call her the **finder** — is ranging somewhere she does not
usually go: a cave mouth two cells off the worn paths, the back wall of an
abandoned mineshaft, a stone at the edge of the map. On a face she did not carve,
in the same angular runes the group has been learning at the teaching stones since
Movement I, is a short string. She runs it through the rune key (the shared alphabet,
`discord/src/forge/runes.ts`). It is not a coordinate. It is not a keeper's name.

It is **another living player's name** — say, the name of the loner who never comes
this far. Below it, in the keeper-record register, one line in plain glyphs:

> kept here before you. — *(rendered in runes; decodes to those four words)*

The finder brings it back. The named player swears they have never set foot there —
and they are *right*. The dashboard heatmap confirms it: that cell's `visits` for
that player is **zero**. The land wrote a true name in a place its owner has never
been, and let someone else find it first.

### 1b. Why it lands (the three bars)

- **"It knows me" max:** the callout is *true* and *verifiable by the players
  themselves* — they can check the heatmap-shaped evidence in-fiction (the named
  player's own memory) and out (dashboard). This is the precision law paying rent:
  the carve only ever uses a name the tracker holds and a cell the tracker proved
  empty, so it can never be wrong.
- **Non-linear web:** the finder is not the subject. The clue *travels between
  players* — the person who can verify it is not the person who found it. That forces
  a conversation, which is where veteran ARG groups live.
- **Camera-worthy:** a clean two-beat on video — "wait, that's *your* name" / "I've
  literally never been here." No jank, no UI, just stone and a decode.

### 1c. How it plays across the 5-movement arc

This is **not** a one-shot gimmick. It is staged so the same mechanic re-reads
differently three times — the "oh, that's what that was for" engine.

- **Movement I (plant, inert):** at one of the early teaching/Rosetta stones, among
  ordinary rune-practice lines, one carved line is the rune string `kept here before
  you`. At M1 the group cannot yet decode cleanly and reads it as flavor — a keeper's
  epitaph. No name attached. This is the **seed**: the *grammar* of the eventual
  callout ("kept … before you") seen first as old, dead text about strangers.

- **Movement II (first true carve — group-scale, low stakes):** once the rune key is
  largely solved, the first *living-name* carve appears. The showrunner picks the
  cell with the **lowest aggregate visits across ALL active players** (a place the
  whole group has avoided) and carves a name there — but it picks a name that is
  *low-risk to be wrong*: the player whose heatmap most cleanly proves zero visits to
  that cell. Discovered by whoever first ranges out that far. Register: still
  ambiguous — could be coincidence, could be a keeper who shared a name.

- **Movement III (the precise hit):** now it is unmistakably personal. The cell is
  chosen specifically to be one **the named player has zero visits to but is
  meaningful** — e.g. near a stone tied to the keeper whose *fate rhymes with that
  player's tracked behavior* (Sella's far-water for the high-distance wanderer;
  Vaun's hoard-cave for the high-solo-ratio hoarder). The carve is no longer just a
  name; it is a name placed where the matching keeper ended. The group starts to feel
  the stones are "warnings shaped like the people standing next to them" (canon §1)
  — but the land never says so.

- **Movement IV (the re-read / weaponized by the reveal):** when FACT 9 lands ("the
  first hauntings were a keeper's fate re-enacted at the group"), the M2–M3 carves
  recontextualize: the land was not *predicting* where you'd go — it was telling you
  **you are already in the record, filed under a place, the way the keepers are.**
  The grammar from M1 (`kept here before you`) reads a third time: *before you* was
  never about strangers. It is the induction (sealed spine: customs are induction
  into the watching).

- **Movement V (the close):** in the kept/persistent world, the accepting group's
  own names persist carved in places they never went — they have become the markers
  for the next group (FACT 15, felt not stated). The mechanic that flexed "it knows
  you" is revealed to have been the *enrollment paperwork* the whole time.

### 1d. Precision gate (the non-negotiable spine)

The showrunner emits this beat **only** when ALL hold for the chosen (target T,
cell C, anchor A):
1. T's name is held by the tracker (`SignalSnapshot.name()` non-empty; T is an
   active, /link-bound player).
2. The heatmap proves T has **zero visits** to cell C: there is no `heatmap_cells`
   row for `(world, C.x, C.z)` attributable to T. (See §2d for the per-player
   accounting note — this is the one real code gap.)
3. C is **reachable** and a *different* player D is likely to find it: C is adjacent
   to a path D's heatmap shows traffic on, OR C contains a known site/anchor. The
   carve anchor A is a sign or sign-capable support inside C.
4. T ≠ D (finder is not subject). Enforced at selection, not at carve time.

If any fail, the beat does not fire and the cadence falls back to a generic
lore-drip — no jank, no wrong "it knows you."

---

## 2. THREAD IT (consistency law — exact appearances)

### 2a. Canon FACTs

- **ADDS — FACT 16 (new): "The record files the living by place, not only by name."**
  M2→M4 · FORESHADOW→REVEAL. Paths: the living-name carves in un-visited cells; the M1
  `kept here before you` grammar. Sits under FACT 1 (the record lists the living by
  name) and FACT 15 (to be accepted is to become a marker for whoever comes next).
  Rule: never stated; delivered by carve + the named player's own "I've never been
  here." Add to `arc/lore/canon-spine.md` §3 with the standard `→` web links to
  1, 9, 12, 15.
- **TOUCHES — FACT 1** (record lists the living by name): this mechanic is FACT 1's
  most literal, spatial expression.
- **TOUCHES — FACT 9** (first hauntings = a keeper's fate re-enacted at the group):
  M3 placement near the rhyming keeper's stone is the bridge; M4 reveal recontextualizes
  the carves through 9.
- **TOUCHES — FACT 15** (acceptance = becoming a marker for the next group): M5 makes
  the group's own un-visited carves persist. This is the payoff (see §3).

### 2b. Found-documents / records that must mention or foreshadow it

- `arc/lore/found-documents.md` / `arc/lore/documents/*`: add ONE Archivist-register
  fragment (water-damaged) that uses the **place-filing grammar** so the carve has a
  documentary home. Draft in §3 de-slop block. It must predate the player ever seeing
  a living-name carve.
- The **Mara (Reader) book-cipher** lectern shelf can hold one assembled line that
  reads, when walked, as the place-filing idea ("the map had names on it for ground no
  one had walked") — reusing the existing book-cipher mechanic, not a new one.

### 2c. NPC / Watcher / keeper voice lines

- **Discord Watcher** (`discord/src/voice.ts`): add a `clueDrip`-adjacent key for when
  a living-name carve has been logged — cryptic, names no one, points outward. (Draft
  §3.) Lives in the existing voice module, not inline.
- **The Keeper NPC** (Citizens2/ZNPCsPlus dialogue tree): one half-veiled line, late
  (M4), in the "we" register — the only NPC acknowledgment, never explanation.
- **Archivist record register**: the carve's own second line *is* the record speaking;
  it must obey the §0 record voice.

### 2d. Cipher / puzzle expression (reuse the 11)

- **Primary: the shared rune alphabet** (`runes.ts`) — the carve is rune-encoded; the
  group already learned the key at the teaching stones, so this is pure reuse, zero new
  cipher. A glyph learned at a stone unlocks this carve and vice-versa (cross-surface
  rune law).
- **M3 hardening (optional, reuse `atbash`):** for the precise-hit carve, mirror the
  name with **atbash** before rune-encoding — thematically exact for Sella's
  mirror/reflection keeper and the "your name reflected back at you" read. No new code:
  `atbash` is one of the 11 built ciphers (`discord/src/forge/ciphers.ts`).
- **M2 site link (reuse `coordEncode`):** the second carved line can encode, in
  `coordEncode`, the coordinate of the *teaching stone where the M1 grammar first
  appeared* — so decoding the carve sends the finder back to re-read the inert M1 seed.
  This is the literal "oh, that's what that was for" loop, built from an existing cipher.

### 2e. Beat class / listener / table / seed / sites / voice keys

- **Beat:** `SignWriteBeat` (`plugin/.../beats/lib/SignWriteBeat.java`) — **reused
  as-is.** Payload `lines` = the rune-encoded strings; `place_if_missing:true`,
  `material:"OAK_SIGN"`, `glowing:false`. Carve is reveal-disciplined already
  (`mutateWhenUnwitnessed`) and waxed/protected. **No new beat class needed.**
- **Showrunner selection (new logic, not new beat):** a selector in
  `discord/src/showrunner/decide.ts` (or a small `name-where-never-been.ts` helper it
  calls) that:
  - reads `heatmap_cells`,
  - per active player computes the set of visited cells,
  - finds T, C, A satisfying the §1d gate,
  - emits a `sign_write` `main_beat` with anchor A and rune-encoded lines.
- **New signal accounting (THE ONE REAL CODE CHANGE — see §4 critique):**
  `HeatmapAccumulator`/`heatmap_cells` is currently **aggregate, not per-player**
  (`bump(world, x, z)` has no UUID). The precision gate needs *per-player* "never been
  here." Two options, pick **B**:
  - (A) add `uuid` to the heatmap grain — heavy, multiplies cell rows by player count.
  - (B) **add a separate coarse per-player visited-cell set** keyed by UUID at a
    *coarser* cell size (e.g. 64-block), written from `LocationSampler.samplePlayer`
    via a new `tracker.visitedCells().mark(uuid, world, cx, cz)`, flushed to a new
    `player_visited_cells(uuid, world, cell_x, cell_z)` table. Cheap (a set per player,
    bounded), and it is the *correct* signal for "T has never been in C." The aggregate
    heatmap stays for "where the GROUP avoids." Selection uses both: group-avoided cell
    (aggregate) ∩ T-never-visited (per-player).
- **Table/migration:** new `player_visited_cells` (`discord/supabase/migrations/*`),
  upsert merge-duplicates like `heatmap_cells`. Idempotent absolute-presence rows.
- **Sites (`plugin/src/main/resources/sites.yml`):** add a small set of
  `type: "carve_anchor"` candidate anchors in low-traffic regions (or let selection
  place free-standing signs via `place_if_missing`). Optional but gives art control
  over WHERE carves can appear; all start `enabled:true` with placeholder coords.
- **Voice keys:** add to `voice.ts` the clueDrip line (§2c). Resolver maps
  `voice_key` → fn as today; no English in the showrunner.

---

## 3. DE-SLOP exemplar prose (in-voice, cold, concrete)

The M1 inert seed (carved, rune-encoded; decodes to):

> kept here before you.

The Archivist found-document fragment (place-filing grammar, water-damaged):

> the list is not only of names. against each name, a ground.
> some grounds the foot has not yet found. the record does not wait for the foot. [...]
> we marked Sella to the far water a season before she walked to it.

The carve's own second line (record register, under the living name):

> no visits here. the name is older than the path to it.

The Discord Watcher drip (names no one, points outward):

> a name is cut where its owner has not stood. someone has seen it who is not it. ask whose.

The Keeper NPC, half-veiled, M4 (the only spoken acknowledgment):

> we did not guess where you would go. we filed where you already are.

*(De-slop check: no banned phrases; no named emotions; concrete nouns — ground, foot,
far water, path; the iceberg holds — nothing explains the induction; record voice
counts and files, does not threaten.)*

---

## 4. CRITIQUE — adversarial, honest

**RISK 1 (sharpest) — the precision data does not exist yet.** The aggregate heatmap
cannot answer "has *this player* ever been in *this cell*." Shipping the carve on
aggregate data risks a **false "it knows you"** (carving T's name in a cell T actually
visited but the group rarely does) — a direct precision-law violation, the worst
failure mode.
**MITIGATION:** §2e option B — the new per-player `player_visited_cells` set is a hard
prerequisite. The selector requires a *positive proof of absence* (T has a flushed
visited-set and C is not in it), not merely "no aggregate row." If the per-player set
is unavailable/stale for T, the beat does not fire. **This is a P-gate, not a nicety.**

**RISK 2 — discovery is not guaranteed; the carve may never be found.** A sign in a
low-traffic cell can sit unseen, and an undiscovered flex is a dead mechanic
(orphan risk).
**MITIGATION:** selection clause §1d.3 requires C be *adjacent to a path a different
player actually travels* (per that player's heatmap), so the carve sits just off a
real route. Add a soft backstop: if a carve is un-found after N days, the Watcher
drip (§2c) nudges "someone has seen it" framing only AFTER first discovery — never
hand the location. As a last resort the carve can be re-sited to a freshly-traveled
edge cell next cadence (it is idempotent; old sign stays as ambient lore).

**RISK 3 — collective-judgment law: this singles out a named individual.** Carving ONE
person's name flirts with electing a "chosen one" / punishing an absentee.
**MITIGATION:** (a) gate on **active players only** (canon law); never carve an
offline/absent player's name. (b) Rotate subjects across the arc so every active
player gets carved at least once — it is a chorus, not a spotlight. (c) The carve is
**neutral** (a filing, not a verdict): no honored/violated weight rides on it; it never
changes the bond/compliance tally. It colors, it does not elect.

**RISK 4 — "coincidence" deflation on camera.** A skeptic reads it as the writer
cheating ("you just placed it"). 
**MITIGATION:** the verifiability *is* the defense — the named player's lived memory +
the dashboard zero-visit row make it un-fakeable in-group. Lean into it: the M3 precise
hit (name at the *rhyming keeper's* stone) is too specific to wave off.

**RISK 5 — over-frequency cheapens it.** Carve it weekly and it's noise.
**MITIGATION:** cap to **one carve per movement** (≈3–4 across the arc), each a staged
re-read per §1c. This is also why it's KEEP-**SCALED**, not keep-as-pitched: the pitch
implies a repeatable flex; the craft demands rationing.

**WHAT TO CUT:** do **not** build per-name procedural taunts or any LLM-authored carve
text. The carve strings are short, authored, deterministic, rune/cipher-encoded — the
LLM has no role here. Keep the scalpel out of this entirely (anti-jank: deterministic
spine).

---

## 5. PLANT THE PAYOFF

- **PLANT (Movement I, inert):** the rune line `kept here before you` carved among
  ordinary practice glyphs at a teaching stone. Reads as a dead keeper's epitaph; no
  name; ignorable. The M2 carve's `coordEncode` second line points *back* to this exact
  stone.
- **PAYOFF 1 (Movement II–III):** the *same grammar* returns attached to a **living
  player's** name in a place they have never been. "kept … before you" stops being
  about the dead. First "oh — that line was about *us*."
- **PAYOFF 2 (Movement IV, the big re-read):** under FACT 9, the carves flip from
  "uncanny prediction" to "you were already filed." `before you` was never temporal
  about strangers — it is the induction grammar (sealed spine). Nothing is said; the
  weight does it.
- **PAYOFF 3 (Movement V, felt):** the group's own names persist carved in unvisited
  ground in the kept world — they are now the `kept here before you` for the next group
  (FACT 15). The plant from M1, three movements later, is revealed as the enrollment
  form. **No payoff without the M1 plant; the M1 plant exists only to pay this off.**

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Movement | Role | Depends on | Feeds |
|---|---|---|---|
| **I** | Plant inert grammar at teaching stone | rune key taught (M1 Rosetta), `SignWriteBeat` | M2 carve back-link |
| **II** | First living-name carve, group-avoided cell, low-risk subject | `player_visited_cells` table+signal; rune key solved | M3 precision, FACT 16 |
| **III** | Precise hit at rhyming-keeper stone; optional atbash mirror | keeper-stone sites placed (M2 stones); `atbash` | M4 reveal, FACT 9 bridge |
| **IV** | Re-read under FACT 9; Keeper half-veiled "we" line | FACT 9 reveal beat | FACT 15 hinge |
| **V** | Group's names persist as next-group markers | the Accepting / kept-world flip | FACT 15 (felt) |

**Depends on:** (1) per-player visited-cell signal — **the one new code prerequisite**;
(2) the shared rune key already taught; (3) `SignWriteBeat` (built); (4) keeper-stone
sites placed for the M3 precise hit.

**Depended on by:** FACT 16; the FACT 9 → 15 re-read chain (one of the seven
independent foreshadows of the sealed truth, so a missed carve neither blocks nor
spoils the landing — web-redundant by design).

**PRIORITY: P1 (arc-spine).** Not P0 vertical-slice (it needs the per-player heatmap
work and the rune key taught first; it is not the smallest playable loop). Not P2 depth
(it carries a real FACT and a three-stage payoff into the sealed reveal). Build the
`player_visited_cells` signal in the same pass that builds the slice's heatmap so this
unlocks cleanly in M2.
