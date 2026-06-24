# The Observance — COHERENCE AUDIT (the single coherence ledger)

> **Spoiler-free.** Refers to sealed material only as "sealed (see `arc/...`)". A read-only
> audit of whether the four layers move in **lockstep** as capabilities are added:
>
> 1. **MECHANIC** — plugin beat / detection listener / forge cipher / showrunner action / dashboard control
> 2. **STORY** — lore docs (`arc/lore/documents/*`), canon (`arc/canon-spine.md`, `arc/LORE-BIBLE.md`), the keeper's fate
> 3. **CLUE** — the seed row (`discord/supabase/seeds/puzzles_seed.sql`) + its teachable in-world KEY + the clue artifact
> 4. **INTERACTION** — what the player DOES, the report/consequence, the Discord⇄in-world handoff, the Watcher's voice (`voice.ts`)
>
> The load-bearing principle: a new mechanic bolted onto stale ARG state breaks the "one world"
> illusion. This ledger finds where the layers are OUT OF SYNC — orphans and stale references —
> it does **not** add features.
>
> **This file consolidates six independent seam audits** (showrunner-drip, bestiary↔keepers,
> structures↔clues, the Liar, customs↔detection, ciphers↔puzzles↔keys) into one de-duplicated
> ledger. Where the same gap surfaced in two seams, it is merged once and cross-referenced. The
> original per-seam write-ups (verbatim, preserved) follow the consolidated ledger as an appendix.

---

## 1. VERDICT — how coherent is the system today

**The system is coherent at its core and starved at its edges.** The STORY layer (canon + 12 lore
docs + keeper roster) and the CLUE layer (the live 23-row non-linear web) are mature, internally
consistent, and double-sourced; the MECHANIC substrate the owner already shipped is real and often
*ahead* of the docs (the 11-cipher forge is self-tested, both oracle surfaces share one resolver,
the per-player beat palette is complete, the deterministic showrunner spine compiles, the seven
custom detections all write to Supabase). The dominant class of inconsistency is **not contradiction
but unwoven layers** — a mechanic (or a doc) that grew while its three partner layers stood still:

- **Authored content lags the engine.** Every set-piece has a keeper's *voice* and a clue pointing
  at it, but **no buildable body** (zero `.schem` files, almost no `sites.yml` sites, no carved
  cipher parameters). The engine can paste a stone; nothing tells it which stone, where, or what is
  carved on it.
- **Detection lags consequence.** Seven customs are *detected* and write `custom_compliance`;
  **nothing reads it** to produce a report or beat, so a player never learns they broke a custom.
- **Delivery lags announcement.** The showrunner *announces* a puzzle but never *surfaces its clue*,
  and the whole forge is orphaned from the drip engine.
- **Docs lag the seed.** The Liar catch is already player-driven in the shipped seed, but three
  design docs + the sealed JSON still prescribe the retired showrunner-flip model.
- **Taught canon ≠ live set.** The rune-ring teaches six customs; two of them (Ward, Covering) have
  no mechanic, while two real detections (Unspoken, Dark Hours) are taught nowhere.

None of this is a broken playable run *today* (most gaps are latent because the missing halves hide
each other). All of it breaks the "one world" lockstep the moment a new mechanic — the in-progress
**Dark Hours** custom, a new `.schem`, a new cipher — is added without its three partner layers. The
fix is overwhelmingly **authoring + wiring across layers in lockstep, not new features.**

---

## 2. PER-SEAM LEDGER (consistent items to protect · gaps · orphans), de-duplicated

> Gaps that surfaced in more than one seam are merged into the seam where they are most load-bearing
> and cross-referenced from the others. IDs map to the original seam write-ups in the appendix.

### SEAM A — STRUCTURES ↔ CLUES ↔ STORY (physical set-pieces) — the deepest hole

**Protect:** cipher catalog ↔ keeper voices agree everywhere; the forge (`ciphers.ts`, 11 self-tested)
+ rune alphabet (`runes.ts`, 26 glyphs + digits) are real and ahead of the docs; the 23-row web
realizes `clue-web.md` node-for-node; `SmallStructureBeat` is built and content-ready (inline +
FAWE `.schem` branch, footprint/floor/reveal/idempotency guaranteed — the FAWE reflective-isolation
work landed, see `CRITIQUE-ACTIONS` line 24). The MECHANIC is built; it is starved of content.

**Gaps:**
- **A1 (P0) — `site_id: unbroken_light` is referenced by the seed but absent from `sites.yml`.** The
  seed enqueues beats at `unbroken_light` three times (`undercroft-descent`, `rite-tokens`,
  `accepting-crouch`); `SitesConfig` cannot resolve it, so the entire Undercroft + Accepting spine
  (M3→M5 main beats) fires at a null site. *(was SEAM-3 G-S1)*
- **A2 (P0) — six named keeper-stones, but `sites.yml` has only two anonymous, disabled, unplaced
  slots.** The clue web binds clues to Vaun/Mara/Sella/Orin/Brann/Iss stones + the offering cairn,
  shore pool, threshold lintel, cold hearth; four of six stones have no slot, none is keeper-bound.
  *(was SEAM-3 G-S2)*
- **A3 (P0) — stones are "carved" in fiction but no carved-glyph content or cipher parameters are
  authored.** Only the decoded plaintext/answers exist. Vaun's Caesar **shift** is never fixed to a
  digit; Mara's six page/line/word triples are placeholders with no resolving six-book shelf; Iss's
  Vigenère **ciphertext** is unauthored (only key `ISS` + plaintext). The seed's `accepted_answers`
  must equal `decode(authored ciphertext)`, and nothing authors the ciphertext. *(merges SEAM-3 G-S3
  + SEAM-6 Caesar-shift + the per-stone half of SEAM-1 G-4; depends on the shared bind, see X1.)*
- **A4 (P0) — zero `.schem` files and no `schematics/` directory exist; no inline block list is
  authored for any structure.** `SmallStructureBeat`'s FAWE branch resolves
  `plugins/Observance/schematics/<name>.schem` → schematic-missing for every set-piece; the offering
  cairn (atmosphere-stack §6.1 calls it "the existing inline path") has no authored block list. *(merges
  SEAM-3 G-S4 + SEAM-2 G-B, which was the Seventh's-shrine special case of this.)*
- **A5 (P0) — the two Rosettas the ciphers DEPEND ON have no authored content, and one is unsited.**
  (1) the rune-letter Rosetta at the first stone (the 4–5 glyph→letter teaching sign) has no authored
  glyph subset or sign text; (2) the digit-glyph **Stone of Reckoning** (code half DONE per
  `cipher-web §4.2b`; world placement an open task — MASTER-PLAN P0.7) has no doc, no site, no
  content. Live coordinate + substitution clues are therefore an **untaught second cipher**, breaking
  the in-corpus-key fairness law (`cipher-web §0` law 2). *(merges SEAM-3 G-S5 + SEAM-6 coordEncode-Rosetta
  + `CRITIQUE-ACTIONS` friend-group #5: "verify the Stone of Reckoning is reachable BEFORE the first
  coord clue goes active.")*
- **A6 (P1) — the dead-shrine and the seventh-shrine are two DISTINCT canon places, neither sited
  nor built.** Both have seed rows (`iss-dead-shrine`, `seventh-shrine`) and are called "a real,
  placed site" in seed-notes; `sites.yml` has neither and there is no `.schem`/inline content for
  either. Both are "cold hearth" — collapse risk for two places D11 keeps distinct. The dead-end's
  whole point is physically walking there — there must be a there. *(was SEAM-3 G-S6.)*
- **A7 (P1) — coordinate answers are direction-words only; literal world coords unfilled** across
  `iss-dead-shrine`, `stone-sella`, `no-wall-catch` because `sites.yml` coords are `null`. Single fix
  tied to siting. *(was SEAM-3 G-S7 = seed-notes G5; also surfaces in SEAM-4 L-context.)*
- **A8 (P1) — FACT 9 (`haunting-biography`) has no structure or document home.** It rides entirely on
  unbuilt M4 keeper-NPC dialogue (Citizens2/ZNPCsPlus, "Phase 2, not built"); its teaching surface is
  itself an unbuilt set-piece. Either author a found-document home or gate `active=false` until the NPC
  exists. *(merges SEAM-3 G-S8 + SEAM-2's deferral of the creature↔keeper link to the M4 biography +
  LORE-BIBLE TODO-3 / MASTER-PLAN R11.)*

**Orphans:** `unbroken_light` site_id (in CLUE, missing in MECHANIC+structure); six named keeper-stones
(STORY+CLUE, missing in MECHANIC); carved ciphertext + cipher params per stone (nowhere — only
plaintext exists); any `.schem` file / `schematics/` dir (MECHANIC resolves the path, filesystem has
zero); offering-cairn inline block list (referenced, no authored blocks); first-stone rune Rosetta sign
content; Stone of Reckoning digit Rosetta (code+spec, no doc/site/content); dead-shrine & seventh-shrine
structures+sites; FACT 9 teaching surface (keeper-NPC dialogue).

### SEAM B — CIPHERS ↔ PUZZLES ↔ TEACHABLE KEYS

**Protect:** the forge ships 11 round-trip-self-tested ciphers, all wired into `forgeClue`; vigenère /
atbash / substitution / bookCipher are the **fully-coherent exemplars** (cipher, story, clue row,
teaching doc, and verb all agree). `coordEncode` is already the fixed digit-glyph scheme (MASTER-PLAN
R1/R4). The design docs are honest that some ciphers are unused — but that honesty has gone stale (B1).

**Gaps:**
- **B1 (P1) — five ciphers (railFence, columnar, polybius, a1z26, morse) are implemented + self-tested
  but used by NO active seed row and taught by NO in-world document.** The docs (`immersion-blueprint`,
  `clue-web §0`, `cipher-web §1`) claim these "carry other nodes" — those nodes do not exist in
  `puzzles_seed.sql`. The mechanic layer grew to 11; the clue layer stayed at 6 effective ciphers; the
  reconciling design claim is stale. **Lockstep:** PATH A (use them — author a seed row + placed
  teaching key per orphan cipher) OR PATH B (cut them honestly — demote the five in the docs from
  "placed" to "available in the forge, intentionally unused"). Either way MECHANIC + STORY + CLUE must
  state the same count.
- **B2 (P1) — `stone-brann` is a cipher/clue MISMATCH.** `clue-web §0` + `cipher-web §1 P7` +
  `arg-deepening §1.2` describe Brann's node as a beacon-colour-sequence and/or rail-fence puzzle gated
  to night/black-moon, rail count taught by "count the fires" (D08). The actual seed row is a flat
  `outcome_type:'lore'` accepting a prose line, with no cipher and no night gate. The most cipher-rich
  keeper node in the design is the least mechanical in the seed. **Lockstep:** either re-author
  `stone-brann` as a railfence/beacon puzzle keyed on D08's fire-count + add the night gate (this also
  un-orphans railFence and gives Dark Hours a clue home — see D2), or strike the rail-fence/beacon
  claims for Brann from the docs.
- **B3 (P1) — Caesar (`stone-vaun`) is currently UNFAIR.** The cipher needs a numeric shift, but D02
  never states a wheel value and no Rosetta wheel-sign is placed; `cipher-web §1 P1` asserts "the shift
  IS his hoarding (three of each)" but D02 only says "i had three of each" without equating it to a
  Caesar key. A fair solver cannot derive the shift in-world. **Lockstep:** author the "three of each"→
  wheel equivalence into D02 **and** build the Rosetta wheel-sign at Vaun's stone, OR change the verb so
  the shift is a counted in-world quantity; update `cipher-web §1 P1` to point at the real teaching line.
  *(This is the per-stone instance of A3 for Vaun; fix together.)*
- **B4 (P0) — `coordEncode`'s teachable key (the Stone of Reckoning) is unbuilt in the world** —
  **same gap as A5(2).** Until placed, every coordinate node is an untaught second cipher (Law-2
  violation). The CODE half landed; the WORLD half did not. Flag coord rows `active=false` until it
  exists. *(merged into A5; listed here so the cipher seam isn't silent on it.)*
- **B5 (P2) — the seed rows carry no cipher binding at all.** Every row stores only decoded plaintext;
  no field names which cipher carved the node or which document teaches it. `cipher-web §5` pushes the
  cipher↔node binding to an unwritten "authoring/placement step" that exists only as prose across three
  docs. **This is the single point where CIPHER, STORY-KEY, and CLUE are meant to meet, and it is the
  reason the orphan/stale-claim drift above accumulated silently.** *(This is the shared cross-seam
  root X1 — see §2 cross-seam.)*

**Orphans:** railFence (assigned to stone-brann in design; stone-brann is plain lore in the seed);
columnar (no row, no keyword doc); polybius (only in the sealed JSON, world marker-grid unbuilt);
a1z26 (no row, no tick-mark stave); morse (no row, no Morse-table prop); Caesar shift number / wheel
value (design says shift = Vaun's hoarding; D02 never states a number, no Rosetta wheel-sign).

### SEAM C — SHOWRUNNER DRIP ↔ CLUE CONTENT ↔ CADENCE

**Protect:** the answer side of the loop is intact — one `resolveAnswer` across all three surfaces
(`/answer`, `#the-record` scan, in-world sign) against one `puzzles` table, `solves` uniqueness guard
(INV-3); voice discipline holds (`voice.ts` is the sole text source, no hardcoded English bypass, INV-1);
player-earned fires `approved`, curated waits `pending` (INV-6); silence-is-canon honored (INV-7); the
20h drip cadence + within-session stall auto-gift are sound and deterministic (pure `decide()`). Cadence
itself is fine.

**Gaps:**
- **C1 (P1) — dripped puzzles surface NO clue content.** `immersion-blueprint §4` / `cipher-web §5` say
  the drip posts a forged clue card (`postClue → forgeClue/renderClueDetailed`); `apply.ts` posts only
  the generic `voice.drip()` line with no clue, no rune card, no puzzle reference. The entire
  `discord/src/showrunner/` tree never imports the forge or `poster.ts`. The async "tide" the engine is
  built around never actually surfaces a clue. **Lockstep:** showrunner forges/posts the dripped node's
  card via `poster.postClue` (needs X1); reconcile the docs if intent changes; the dripped node must have
  a real Discord artifact or be routed to an in-world-pointing report.
- **C2 (P1) — drip ORDER fights the story: the first-ever live drip is a `dead_end`.** `decide.ts` sorts
  the un-dripped pool by `(movement asc, key asc)`; the three active Movement-I keys sort
  `m1-named-habit` < `m1-record-opens` < `rosetta-ring`, so the opening drip is `m1-named-habit`
  (`dead_end`) — before the literacy gate or the entry document. Combined with `CRITIQUE-ACTIONS` L33
  (`dead_end` has no distinct terminal voice), this is the "wasted evening" failure. Masked because
  `decide.selftest` test 7's fixture omits `m1-named-habit`. **Lockstep:** drip ordering must respect
  outcome semantics — never open with `dead_end`/`lore` found-document rows; prefer an
  entry/`main_beat`/`next_clue` node; add a selftest case including `m1-named-habit`.
- **C3 (P1) — non-solvable "found document" / plugin-sentinel rows are in the drip pool.** `snapshot.ts`
  builds the pool from every `active=true` row with no filter on outcome_type or surface, so the drip can
  "announce, read it here" about `m1-record-opens` / `m1-named-habit` / `record-receives` — nodes with no
  Discord-decodable clue. **Lockstep:** exclude in-world-only / sentinel rows from the Discord drip pool
  (or route them to an in-world-pointing report), tied to `clue-web-seed-notes.md`.
- **C4 (P2) — `voice.drip()` is the SOLE drip line for the whole 5-movement arc.** One line for a 2-week
  escalating arc means the Watcher's between-session voice never changes register as dread rises.
  **Lockstep:** add movement-/node-aware drip lines in `voice.ts`; `apply.ts` selects by the dripped
  puzzle's movement — keeping `voice.ts` as the sole source.

**Orphans:** the clue forge (`forge/*`, built + self-tested) — no consumer in the drip engine; `poster.ts`
(`postReport`/`postClue`, "the showrunner's hand") — zero callers in `discord/src/showrunner/`;
`voice.drip()` — one line, no per-movement coverage; authored-key→forge-spec bind — asserted in
`cipher-web §5`, missing in code (= X1).

### SEAM D — CUSTOMS/TABOOS ↔ DETECTION ↔ REPORTS/CONSEQUENCE ↔ STORY

**Protect:** the plugin detects seven customs cleanly (the_bow, the_offering, the_unspoken,
the_kept_light, the_deep_line, the_sacred_beast, the_dark_hours), all writing `CustomComplianceRow` to
Supabase with anti-exploit pure tracking; the report VOICE already exists in-register
(`reportObserved`/`reportEscalated` on the Orin observe→warn→left ladder, matching D04). Bow / Offering /
Kept Light are the only fully-aligned customs (blocked from full lockstep solely by D1).

**Gaps:**
- **D1 (P0) — the customs→report/consequence bridge is entirely unbuilt.** All seven detected customs
  write `custom_compliance`; NOTHING reads it to produce a player-facing report or beat. The only caller
  of `voice.reportObserved` is a README example; the showrunner only drips clues + gifts hints.
  `immersion-blueprint §2` hook 5 (violation → soft toll → `reportObserved` → Orin ladder) is authored
  but unwired. Every custom is detected-but-no-report; a player who breaks a custom never learns it.
  **This single fix un-strands all seven customs at once.** **Lockstep:** add a showrunner/beat pass that
  reads violated counts and calls `voice.reportObserved/reportEscalated` on the observe→warn→left ladder
  and/or enqueues a soft reversible toll beat; STORY ladder already matches Orin — keep; CLUE/MECHANIC
  unchanged.
- **D2 (P0, the owner's in-progress example) — Dark Hours is detected and storied but has NO clue and NO
  consequence.** `DarkHoursListener` fires `ps.violate(CUSTOM_DARK_HOURS)` on bed-enter on a taboo moon
  phase; Brann/D08 carry the lore — but "dark hours" / "black moon" / "do not sleep" appears **nowhere**
  in `discord/src` (no voice line, no puzzle answer); no beat fires off the violation; Dark Hours is not
  in the rune-ring taught set; `stone-brann`'s answer is FACT 11, not the sleep taboo; the designed P13
  black-moon puzzle was never seeded; the blueprint's promised Brann night-apparition +
  `PrivateDarknessBeat` is unimplemented. **Ship the listener WITH its clue + consequence, not alone.**
  **Lockstep:** (CLUE) seed the P13 black-moon perform-at-time puzzle OR add Dark Hours to a taught
  surface (pairs naturally with re-authoring `stone-brann`, B2); (INTERACTION) implement the
  `immersion-blueprint §2.6` Brann night-only apparition + `PrivateDarknessBeat` downstream of the
  listener, plus the D1 report; (STORY) D08 + canon keep; (MECHANIC) already built. **Also reconcile the
  "black moon" (STORY) vs full-moon-phase-0 (MECHANIC) terminology** — gloss in `bestiary.md` that the
  black moon = the brightest full-moon night turned wrong, or change `taboo-moon-phases` + the comment.
  *(merges SEAM-5 C-5 + SEAM-2 G-C.)*
- **D3 (P1) — the Unspoken can NEVER fire.** `ChatListener` detection is built, but `config.yml` ships
  `forbidden-words: []` so `containsForbidden` is always false; the custom is storied (Iss / the
  breaking) and `unspoken-refrain` is designed but unseeded. **Lockstep:** (CONFIG) author the per-arc
  forbidden word(s) (the trigger, never a displayed string — canon's "never write the Unspoken" holds);
  (CLUE) seed `unspoken-refrain`; (INTERACTION) wire via D1.
- **D4 (P1) — three designed custom-puzzles never reached the live seed:** `unspoken-refrain`,
  `haunted-herd`, P13 (Dark-Hours perform-at-time). So three detected customs (Unspoken, Sacred Beast,
  Dark Hours) have no clue node telling the player they exist. **Lockstep:** (CLUE) seed the three rows +
  wire payoffs; pair with D1. *(D2/D3/D4 overlap on P13/unspoken-refrain — author together.)*
- **D5 (P2) — Ward and Covering are taught but do not exist as mechanics.** The rune-ring +
  `cipher-web.md` teach `bow offering kept light deep line ward covering`, but Ward and Covering have no
  detection, keeper, puzzle, or consequence. A player taught "these are the ways" finds two of six inert.
  **Lockstep (pick one):** (a) DEMOTE — replace ward/covering in the taught ring with two real customs
  (the Unspoken, Dark Hours), editing `rosetta-ring.accepted_answers`, `clue-web.md`, `cipher-web.md`,
  `learn-them-as-we-learned-them.md` together; OR (b) BUILD Ward + Covering as real mechanics + lore +
  consequence. *(Option (a) is the natural partner of D2/D4: the two customs you'd promote in are the two
  you're seeding.)*
- **D6 (P2) — Deep Line and Sacred Beast are storied thinly or not at all.** Deep Line is a taught key +
  a detection but no keeper embodies the depth taboo and no lore fragment names it; the Sacred Beast lives
  only in `arg-deepening` notes — no keeper, no lore doc — so a detected kill has nothing to read back to.
  **Lockstep:** (STORY) add a short lore beat for each so a violation has a home; (CLUE) optionally fold
  into an existing stone; keep the Sacred Beast intentionally stone-less (see SEAM E G-D).

**Orphans:** `DarkHoursListener`/`CUSTOM_DARK_HOURS`/`tracker.dark-hours` (MECHANIC; no clue, no
consequence); `reportObserved`/`reportEscalated` (INTERACTION voice; no production caller);
`the_unspoken` detection (built but inert via empty `forbidden-words`); `the_sacred_beast` detection
(built; no keeper/lore, no seeded puzzle); `unspoken-refrain`/`haunted-herd`/P13 (design; not in the live
seed); Ward / Covering (taught + named; no mechanic).

### SEAM E — BESTIARY ↔ KEEPERS ↔ STONES ↔ TRIGGERS

**Protect (exceptionally coherent):** the set {Vaun, Mara, Sella, Orin, Brann, Iss} + the Seventh is
identical across `bestiary-sealed`, `canon-spine`, `clue-web`, `LORE-BIBLE`, and all 12 lore docs; fates,
embodied customs, voices, cipher assignments match; each keeper has an authored stone row grounded in its
doc (D02/D05/D06/D07/D08/D09). Every creature trigger resolves to a REAL `SignalSnapshot` field
(`hoardedScore`, `soloMiningRatio`, `distanceFromGroup`, `mobKills`, `complianceFor(...)`) wired by real
listeners; every primary beat class exists. FACT-15 discipline holds (creatures are silent, no orphan
creature line in `voice.ts`, the creature↔keeper link correctly deferred to the M4 biography). Iss
correctly has no creature.

**Gaps:**
- **E1 (P1) — MythicMobs payload yields the WRONG fallback entity.** The bestiary writes apparition
  payloads as `{"entity":"mythicmob:watcher | WARDEN"}` and states the deterministic fallback is the
  vanilla model-rider (WARDEN/SKELETON/COW/DROWNED/STRAY). But `NamedMobBeat.entityType()` does
  `EntityType.valueOf(name.toUpperCase())` and HARD-falls back to `ZOMBIE` on any unparseable id; there is
  no MythicMobs resolution in the plugin. So `mythicmob:watcher` spawns a short green zombie, not the
  3.2-block WARDEN-silhouette the creature's read depends on (`CRITIQUE-ACTIONS` line 27 flags the unbuilt
  `ModeledMobBeat`). **Lockstep:** (MECHANIC) honor a payload-supplied `fallback_entity` instead of
  hardcoded ZOMBIE, or build `ModeledMobBeat` resolving MythicMobs and degrading to the correct skeleton;
  (STORY) until that lands, update `bestiary.md` payload examples to the plain vanilla entity the code
  actually spawns.
- **E2 (P1) — the Seventh creature + the `seventh-shrine` clue depend on a ruined-shrine `.schem` that
  does not exist** — **the bestiary-specific instance of A4.** Keep `seventh-shrine` staged/inactive per
  the seed-notes grounding rule until the doused-hearth `.schem` is authored. *(merged into A4.)*
- **E3 (P1) — "black moon" (STORY) is mechanically the FULL moon (MECHANIC)** — **same terminology drift
  folded into D2.** Reconcile in both layers; don't let a future black-moon beat assume phase 0 = a dark
  night. *(merged into D2.)*
- **E4 (P2) — the per-keeper "personal token" is described two ways and surfaced by no stone.** The rite
  (`rite-tokens`, D12) assumes "one personal token per keeper"; `arg-deepening §1.2` step 5–6 describes
  each stone vision handing a per-stone "rune-key fragment." So "what each keeper gives you" is told two
  ways and the six `stone-*` rows surface neither. **Lockstep:** decide one model — either each stone
  vision hands a token (give each `stone-*` row a `set_flags`/reveal for it) or the six tokens are a
  player-chosen M5 gather (trim `arg-deepening §1.2`'s "rune-key fragment"). The rite works either way.
- **E5 (P2) — the Sacred Beast is deliberately stone-less but that intent is undocumented in the
  player-facing bestiary**, risking a future duplicate-Mara node (creature 6 maps tonally to Mara; Mara's
  literal stone is `stone-mara`). **Lockstep:** add a note in `bestiary.md §2.5` that the Sacred Beast is
  intentionally stone-less and is Mara's tonal note only. *(pairs with D6's "keep the Sacred Beast
  stone-less.")*

**Orphans:** MythicMob entity ids (STORY payloads; no resolver in MECHANIC, degrades to ZOMBIE); the
Seventh's ruined-shrine `.schem` (= A4); per-keeper "rune-key fragment / token" (STORY arg-deepening; no
`stone-*` row surfaces it).

### SEAM F — THE LIAR (Iss)

**Verdict / protect:** **the critic's #1 charge is already addressed by the shipped seed.** The catch is
player-driven, NOT a secret showrunner flag-flip: `no-wall-catch` is `main_beat`,
`set_flags{iss_caught, true_coord_known}`, `next_puzzle_key:'rite-tokens'`, beat `private_message
iss.dialogue.turns_cold`; `resolve.ts applyOutcome` applies `set_flags` and enqueues the beat on the
player's solve (`approved`, no human gate); the showrunner never reads/writes `iss_caught`. The Vigenère
forge is real + self-tested; `dead_end` vs `main_beat` outcome types are wired right; the Seventh thread
is kept distinct from Iss; `max_attempts:6` caps the short warm reading. **MECHANIC↔CLUE lockstep is
correct; the MECHANIC moved and three other layers lagged.**

**Gaps:**
- **F1 (P1) — three design docs + the sealed JSON still describe the RETIRED "showrunner flips
  `iss_caught` offstage" model**, contradicting the shipped player-driven seed: `cipher-web §3`,
  `MASTER-PLAN` step 11, and sealed `d10-stone-after`. A builder following the docs would re-introduce the
  hollow mechanism the seed already removed. **Lockstep:** (STORY/DESIGN) rewrite all three to state the
  PLAYER solve sets `iss_caught` and enqueues the flip; demote any showrunner swap to an optional
  AUTO/asleep fallback; (CLUE/MECHANIC) none. **Re-scope `CRITIQUE-ACTIONS` ARG #1 from "rebuild the
  engine" to "reconcile the docs to the shipped player-driven seed."**
- **F2 (P1) — no document encodes a player-CHECKABLE falsifiable claim, so `no-wall-catch.accepted_answers`
  is the CONCLUSION read off D10, not a contradiction DERIVED by holding D09 against D10/D11.** The catch
  reads as "type the phrase off the answer stone," not "you caught him" — exactly the hollowness the critic
  named, relocated to the clue-authoring layer. **Lockstep:** (CLUE) author `no-wall-catch` (or a paired
  cross-document-correlation row) so its `accepted_answers` is the **derived contradiction itself**
  (reachable only by holding two docs together), keeping `outcome_payload` exactly as shipped; (STORY)
  keep D09's falsifiable boast + D10/D11's refutation, ensure the boast names something independently
  confirmable; (MECHANIC/INTERACTION) none.
- **F3 (P2) — the "warm reading → dead shrine" herring has no clue-graph edge.** D09 ("go there and you
  will have the end of it") + the design map say the warm reading hands a coordinate to `iss-dead-shrine`,
  but `stone-iss-wall.next_puzzle_key` is `iss-doubt` only; the grave is reachable only by typing a
  place-name cold. **Lockstep:** (CLUE) give the warm reading its own edge to `iss-dead-shrine`; STORY +
  the `oracleDeadEnd` voice already written.
- **F4 (P2) — the warm→cold dialogue flip has a producer but no consumer.** `no-wall-catch` enqueues
  `private_message` (`iss.dialogue.turns_cold`) and `PrivateMessageBeat` exists, but nothing reads
  `flags.iss_caught` to re-read Iss's whole dialogue TREE cold; the keeper-NPC layer is Phase-2/unbuilt.
  **Lockstep:** when the NPC layer is built, gate Iss's nodes on `flags.iss_caught`; until then, record in
  the docs that the flip is one message, not a tree re-read. *(shares the unbuilt-NPC root with A8.)*
- **F5 (P2) — `iss_caught` / `true_coord_known` flags are set but never read by any live row, and the
  name drifts** (`_known` in the seed vs `_found` in `cipher-web §2.2` + sealed `true-final-threshold`).
  The live spine routes `no-wall-catch → rite-tokens`, so nothing reads either flag; the flag-gated door
  lives only in the `active:false` sealed rows. **Lockstep:** reconcile the flag name across seed + design
  + sealed; wire a live consumer or document the flags as forward-hooks for the sealed endgame.

**Orphans:** player-driven catch (CLUE+MECHANIC; STORY/DESIGN still describe the retired model);
player-checkable falsifiable claim → submitted contradiction (STORY present; CLUE accepts the conclusion);
warm-reading → `iss-dead-shrine` edge (STORY+design map; no seed edge); Iss dialogue tree re-reads cold
(STORY + producer beat; no `iss_caught`-gated consumer); `iss_caught`/`true_coord_*` flags (set; no
reader; name drift).

### CROSS-SEAM ROOTS (one cause, many symptoms)

- **X1 — the missing authored-`puzzle_key` → forge-spec bind.** `cipher-web §5` says the
  authoring/placement step binds the carved artifact to the node's authored kebab key; **no code does
  this.** No registry maps `stone-vaun → {cipher, text, key}`. This single absence is why: A3 (no carved
  ciphertext exists to author against), C1 (the drip has nothing to forge from), and the latent
  INV-1/INV-3 risk that the in-world carving and the Discord card encode different plaintext. **Fix once:
  add a forge-spec map keyed by authored `puzzle_key`, consumed by BOTH the world-placement step and the
  showrunner drip, so a node has ONE plaintext rendered identically on both surfaces.** This is the
  precondition for A3 and C1, and the machine-checkable replacement for B5's three-prose-docs join.
- **X2 — content authoring lags the engine, everywhere.** A1/A2/A3/A4/A5/A6 + E2 + D2's clue half are all
  the same shape: a built mechanic with no authored content (`.schem`, site, carved glyph, seed row).
- **X3 — the unbuilt keeper-NPC dialogue layer** (Phase 2) is the shared blocker behind A8 (FACT 9 home)
  and F4 (Iss tree re-reads cold).

---

## 3. PRIORITIZED LOCKSTEP FIX LIST

> **P0 = breaks coherence for the live 23-puzzle web or the vertical slice.** P1 = the arc spine. P2 =
> depth/polish. For each fix, what must change in ALL affected layers together — so nothing ships
> half-woven. (Severities reconciled across seams; the owner's two named priorities — "structures need
> authored content + story" and "Dark Hours needs a narrative home" — are P0.)

### P0 — close before the vertical slice / before any new mechanic lands

**P0-1 · Add the missing forge-spec bind (X1).**
`MECHANIC` add a registry keyed by authored `puzzle_key` → `{cipher, plaintext, key/shift/book}`, consumed
by both the world-placement step and the showrunner. `CLUE` each seed row's `accepted_answers` becomes
`decode(spec.ciphertext)` by construction. `STORY` none. `INTERACTION` the drip and the carving now render
ONE plaintext. *Precondition for P0-2, P0-5, P0-6.*

**P0-2 · Author the spine's sites + carved content (A1, A2, A3, A5, B3, B4).**
`MECHANIC` add to `sites.yml`: `unbroken_light`, six named keeper-stones, the Stone of Reckoning, the
first-stone Rosetta, the shore/threshold/cold-hearth anchors (placed, enabled). `STORY` author the
Undercroft/altar physical content, the two Rosetta artifacts (rune-letter pairs + digit carving + a short
Vaun/founder fragment), and the D02 "three of each → wheel" equivalence for Caesar. `CLUE` author, per
stone, the `forgeClue` spec (via P0-1) so the carved rune string decodes to the seed answer; keep
`site_id` spellings identical; flag every coordinate row `active=false` until the Stone of Reckoning is
placed. `INTERACTION` a player can now walk to a stone, read a glyph, and decode it fairly.

**P0-3 · Author the vertical-slice `.schem`/inline content (A4, E2).**
`MECHANIC` create `plugins/Observance/schematics/` and drop the first `.schem` files; author the offering
cairn as an inline block list. `STORY` the cairn / first keeper-stone+Rosetta / doused alcove / ruined
seventh-shrine each become a real discoverable place. `CLUE` keep `seventh-shrine` (and any unsited stone)
`active=false` until its structure exists. `INTERACTION` the set-pieces are discovered, never witnessed
appearing. *(atmosphere-stack §6 vertical slice; then scale to all six stones + Undercroft + altar.)*

**P0-4 · Build the customs→report/consequence bridge (D1).**
`MECHANIC` add a showrunner/beat pass that reads `custom_compliance` violated counts.
`INTERACTION` on the observe→warn→left ladder, call `voice.reportObserved`/`reportEscalated` (already
authored, in-register) and/or enqueue a soft reversible toll beat. `STORY` the ladder already matches Orin
(D04) — keep. `CLUE` none. **Un-strands all seven detected customs at once.**

**P0-5 · Give Dark Hours its clue + consequence — the owner's example (D2, B2, E3).**
`MECHANIC` `DarkHoursListener` is built — keep; reconcile the `taboo-moon-phases` ↔ "black moon"
terminology (gloss `bestiary.md` or change the phase + comment). `CLUE` re-author `stone-brann` as the
black-moon/rail-fence node keyed on D08's fire-count (this seeds Dark Hours' clue AND un-orphans railFence,
B1 PATH A) and/or seed the P13 perform-at-time row; add Dark Hours to a taught surface. `INTERACTION`
implement the `immersion-blueprint §2.6` Brann night-only apparition + `PrivateDarknessBeat` downstream of
the listener, plus the P0-4 report. `STORY` D08 + canon keep. **Ship the listener WITH its clue +
consequence — do not land it alone.**

**P0-6 · Make the drip carry clue content (C1).**
`MECHANIC` the showrunner forges/posts the dripped node's card via `poster.postClue` (uses P0-1).
`CLUE` the dripped node must have a real Discord artifact, or be routed to an in-world-pointing report.
`STORY` reconcile `immersion-blueprint §4` / `cipher-web §5` if the intent changes. `INTERACTION` logging in
overnight now surfaces something to decode, not a teaser.

**P0-7 · Fix the drip selection (C2, C3).**
`MECHANIC` drip ordering respects outcome semantics (never open with `dead_end`/`lore`/sentinel rows;
prefer entry/`main_beat`/`next_clue`); `snapshot.ts` excludes in-world-only/sentinel rows from the Discord
pool; add a `decide.selftest` case including `m1-named-habit`. `STORY` keep the Movement-I order
(notice → literacy → field). `CLUE` row-intent stays authoritative.

### P1 — the arc spine

- **P1-1 · Reconcile the Liar docs to the shipped player-driven seed (F1).** Rewrite `cipher-web §3`,
  `MASTER-PLAN` step 11, sealed `d10-stone-after`; re-scope `CRITIQUE-ACTIONS` ARG #1. CLUE/MECHANIC none.
- **P1-2 · Make the Liar catch a derived contradiction (F2).** Re-author `no-wall-catch.accepted_answers`
  as the synthesized cross-document fact, keeping `outcome_payload` as shipped; ensure D09's boast names
  something independently confirmable.
- **P1-3 · Resolve the five orphan ciphers (B1).** PATH A (seed a row + placed key per cipher — railFence
  already lands via P0-5) or PATH B (demote in `immersion-blueprint`/`clue-web §0`/`cipher-web §1`). One
  count across MECHANIC + STORY + CLUE.
- **P1-4 · Seed the remaining custom-puzzles + fire the Unspoken (D3, D4).** Author per-arc
  `forbidden-words`; seed `unspoken-refrain` + `haunted-herd` (+ P13 if not done in P0-5); wire payoffs via
  P0-4.
- **P1-5 · Fix the MythicMobs fallback entity (E1).** Honor a payload `fallback_entity` (or build
  `ModeledMobBeat`); until then correct `bestiary.md` payloads to the vanilla entity actually spawned.
- **P1-6 · Build the two distinct shrines + backfill literal coords (A6, A7).** Two visibly-different
  shrine structures + two sites (grave vs doused hearth); add the unsigned literal coordinate to
  `iss-dead-shrine` / `stone-sella` / `no-wall-catch` once sited.
- **P1-7 · Wire the warm→dead-shrine edge (F3).**

### P2 — depth / polish

- **P2-1** · Promote the cipher↔node binding into a machine-checkable artifact (B5 — folds into P0-1's
  registry).
- **P2-2** · Movement-aware drip lines in `voice.ts` (C4).
- **P2-3** · Resolve Ward/Covering vs the live custom set (D5) — demote in the taught ring (pairs with
  P0-5/P1-4 promotions) or build them.
- **P2-4** · Thin lore homes for Deep Line + the Sacred Beast (D6); document the Sacred Beast as
  intentionally stone-less (E5).
- **P2-5** · Decide one per-keeper token model and align both layers (E4).
- **P2-6** · Gate Iss's dialogue tree on `iss_caught` when the NPC layer lands (F4); resolve FACT 9's home
  or stage it inactive (A8); reconcile the `true_coord_known`/`_found` flag name + give it a reader (F5).
  *(A8 + F4 both wait on X3, the keeper-NPC layer.)*

---

## 4. THE AUTHORING CONTRACT

> **No feature ships without its STORY, CLUE, and INTERACTION authored in the same change.** A mechanic
> alone — a listener, a beat, a cipher, a `.schem` slot, a showrunner action — is half a feature; the
> other half is the world learning it exists, a fair in-world key that teaches it, and a consequence the
> player can read back to. Concretely, before a capability is "done":
>
> 1. **MECHANIC** — the plugin/forge/showrunner/dashboard code exists and is reachable.
> 2. **STORY** — a lore/canon line introduces it; the keeper/place/fate it belongs to is named.
> 3. **CLUE** — a seed row points at it, with a *teachable in-world KEY* (the Rosetta/wheel/shelf/table
>    that makes its cipher fair) and a buildable artifact (sited + `.schem`/inline) — or the row is
>    `active=false` until that artifact exists (the seed-notes grounding rule).
> 4. **INTERACTION** — the player *does* something, gets a report/consequence, and the Discord⇄in-world
>    handoff resolves through the one resolver in the one `voice.ts` register.
>
> The owner's in-progress **Dark Hours** custom is the test case: detection is built (1) and the lore is
> strong (2), but with no clue (3) and no consequence (4) it is a detection with no narrative home — do
> not land it alone. The same rule retired the Liar's offstage flag-flip: the catch ships only when the
> player's submission of the contradiction IS the catch, and every doc, accepted-answer, and NPC gate
> says exactly that.

---
---

# APPENDIX — ORIGINAL PER-SEAM WRITE-UPS (verbatim, preserved)

> The six independent seam audits below were authored by concurrent sessions and are preserved
> unchanged for traceability. The consolidated ledger above de-duplicates and reconciles them; where an
> appendix gap ID (e.g. `G-S4`, `C-1`, `L1`) is referenced above, find its full detail here.

---

## APPENDIX SEAM 1 — SHOWRUNNER DRIP ↔ CLUE CONTENT ↔ CADENCE

### THE SEAM IN ONE LINE
The showrunner **announces a puzzle exists** but never **surfaces its clue**, and it picks *which*
puzzle by `movement asc, then puzzle_key asc` — an ordering that, on the live seed, opens with a
**dead-end** and treats non-solvable "found document" rows as drippable clues. Design
(`immersion-blueprint §4`, `cipher-web §5`) calls for the drip to carry a forged clue card; the build
emits one generic line and is wired to none of the forge.

### GAPS
- **G-1 (HIGH) — Dripped puzzles surface NO clue content.** `apply.ts` posts only `voice.drip()` with no
  clue/card/reference; the CONFIRM path stages `{puzzle_key, movement, staged_iso}` only; the entire
  `discord/src/showrunner/` tree never imports the forge or `poster.ts`. → consolidated **C1**.
- **G-2 (HIGH) — Drip ORDER fights the story: first live drip is a dead-end.** `decide.ts` sorts
  `(movement asc, key asc)`; `m1-named-habit` (`dead_end`) sorts first; selftest test 7's fixture omits it.
  → consolidated **C2**.
- **G-3 (HIGH) — Non-solvable "found document" rows are in the drip pool.** `snapshot.ts` filters nothing.
  → consolidated **C3**.
- **G-4 (MEDIUM) — No binding from an authored `puzzle_key` to a forge spec.** → consolidated **X1**.
- **G-5 (MEDIUM) — `drip()` is the SOLE drip line.** → consolidated **C4**.

### ORPHANS
The clue forge (no drip consumer); `poster.ts` (no showrunner caller); `voice.drip()` (one line);
authored-key → forge-spec bind (asserted in `cipher-web §5`, missing in code).

### CONSISTENT (do not break)
INV-1/INV-2 one voice; INV-3 one resolver; INV-6 earned fires / curated waits; INV-7 silence is canon;
cadence gating sound. *(See §2 SEAM C "Protect.")*

---

## APPENDIX SEAM 2 — BESTIARY ↔ KEEPERS ↔ STONES ↔ TRIGGERS

### VERDICT
The keeper layer is exceptionally coherent (roster, fates, customs, voices, cipher assignments identical
across all sources; every trigger → a real `SignalSnapshot` field + listener; every beat class present;
FACT-15 silence holds; Iss correctly creature-less). Gaps concentrate in the MECHANIC layer where the
custom-3D ambition outpaces shipped beat code.

### GAPS
- **G-A (HIGH) — MythicMobs payload yields the wrong fallback entity** (`NamedMobBeat.entityType()`
  hard-falls to ZOMBIE; `mythicmob:watcher` → short green zombie). → consolidated **E1**.
- **G-B (MEDIUM) — Seventh creature needs a `.schem` that does not exist.** → merged into **A4** (and
  consolidated **E2**).
- **G-C (MEDIUM) — "black moon" (STORY) is mechanically the FULL moon (MECHANIC).** → merged into **D2**.
- **G-D (LOW) — Sacred Beast is deliberately stone-less — keep it; document it.** → consolidated **E5**.
- **G-E (LOW) — Per-keeper "personal token" is leaned on but unnamed at the stones.** → consolidated **E4**.

### ORPHANS
`mythicmob:*` ids (no resolver, degrades to ZOMBIE); the Seventh's ruined-shrine `.schem` (none exist);
per-keeper "rune-key fragment / token" (no `stone-*` row surfaces it). No orphaned creature, keeper,
trigger signal, or Watcher line.

---

## APPENDIX SEAM 3 — STRUCTURES ↔ CLUES ↔ STORY (physical set-pieces)

### VERDICT
STORY + CLUE are mature and consistent, but the **STRUCTURE layer they both assume is almost entirely
unauthored and unsited.** Every clue points at a stone; every stone has a keeper's voice; no stone has a
buildable physical body. The forge + alphabet are real; `SmallStructureBeat` is built — the system is
starved of authored structure content, not broken.

### GAPS
- **G-S1 (HIGH) — `site_id: unbroken_light` referenced 3× but absent from `sites.yml`.** → **A1**.
- **G-S2 (HIGH) — six named keeper-stones, only 2 anonymous disabled slots.** → **A2**.
- **G-S3 (HIGH) — stones "carved" but no carved-glyph content / cipher parameters authored** (Vaun shift,
  Mara's six triples + shelf, Iss Vigenère ciphertext). → **A3** (+ shares **X1**).
- **G-S4 (HIGH) — zero `.schem` files / no `schematics/` dir; no inline block list authored** (incl. the
  cairn). → **A4** (generalizes SEAM-2 G-B).
- **G-S5 (HIGH) — the two Rosettas the ciphers depend on have no content, one unsited** (first-stone
  rune-letter Rosetta; Stone of Reckoning). → **A5** (+ **B4**).
- **G-S6 (MEDIUM) — dead-shrine and seventh-shrine are distinct canon places, neither sited nor built.**
  → **A6**.
- **G-S7 (MEDIUM) — coordinate answers direction-words only; literal coords unfilled** (= seed-notes G5).
  → **A7**.
- **G-S8 (LOW) — FACT 9 (`haunting-biography`) has no structure/document home** (= LORE-BIBLE TODO-3).
  → **A8**.

### ORPHANS
`unbroken_light` site; six named stones; carved ciphertext + params per stone; any `.schem`/`schematics/`;
cairn inline block list; first-stone rune Rosetta sign content; Stone of Reckoning digit Rosetta;
dead-shrine + seventh-shrine structures+sites. *(See §2 SEAM A "Orphans.")*

### RECOMMENDED LOCKSTEP ORDER (content, not features)
1. Site the spine (G-S1/G-S2/G-S6). 2. Author the two Rosettas (G-S5). 3. Author carved content + cipher
params per stone via the shared bind (G-S3 + X1). 4. Build the vertical slice (G-S4). 5. Backfill literal
coords (G-S7). 6. Resolve FACT 9 (G-S8) or stage inactive. *(Reflected in §3 P0.)*

---

## APPENDIX SEAM 4 — THE LIAR (Iss)

### VERDICT ON THE CRITIC'S CHARGE
"The catch depends on the showrunner flipping `iss_caught` OFFSTAGE" — **no longer true of the shipped
seed.** `no-wall-catch` is `main_beat`, `set_flags{iss_caught, true_coord_known}`,
`next_puzzle_key:'rite-tokens'`, beat `private_message iss.dialogue.turns_cold`; `resolve.ts applyOutcome`
applies it on the player's solve; the showrunner never touches `iss_caught`. **The player solve IS the
catch.** The MECHANIC moved; three other layers lagged.

### GAPS
- **L1 (HIGH) — three design docs + the sealed JSON still describe the RETIRED showrunner-flip model**
  (`cipher-web §3`, `MASTER-PLAN` step 11, sealed `d10-stone-after`). → **F1**.
- **L2 (HIGH) — no player-CHECKABLE falsifiable claim; the catch is read off D10, not derived.** → **F2**.
- **L3 (MEDIUM) — warm-reading → dead-shrine herring has no clue-graph edge** (`stone-iss-wall.next` is
  `iss-doubt` only). → **F3**.
- **L4 (MEDIUM) — warm→cold flip has a producer beat but no `iss_caught`-gated consumer** (NPC layer
  unbuilt). → **F4**.
- **L5 (MEDIUM) — `iss_caught`/`true_coord_known` set but never read; name drifts** (`_known` seed vs
  `_found` design/sealed). → **F5**.

### ORPHANS
Player-driven catch (docs stale); player-checkable claim → submitted contradiction (CLUE accepts the
conclusion); warm → `iss-dead-shrine` edge (no seed edge); Iss tree re-reads cold (no consumer);
`iss_caught`/`true_coord_*` flags (no reader; name drift). *(See §2 SEAM F.)*

---

## APPENDIX SEAM 5 — CUSTOMS/TABOOS ↔ DETECTION ↔ REPORTS/CONSEQUENCE ↔ STORY

### THE SEAM IN ONE LINE
The plugin **detects seven customs cleanly**, but the **CONSEQUENCE layer is unbuilt** (no production code
turns a violation into a report or beat), and three of the seven — **Dark Hours, the Unspoken, the Sacred
Beast** — also have **no clue node**; meanwhile **Ward and Covering** are **taught but never detected**.
Dark Hours (the owner's example) is built and storied but has no clue and no consequence — a detection
with no narrative home.

### CUSTOM-BY-CUSTOM MATRIX
Legend: present (Y) / absent (N) / partial (~).

| Custom | MECHANIC (detect) | STORY (lore/keeper) | CLUE (taught key / puzzle) | INTERACTION (report) |
|---|---|---|---|---|
| The Bow | Y `CustomComplianceListener` | Y Orin (D07/D04) | Y rosetta-ring; `accepting-crouch` | ~ no report caller (C-1) |
| The Offering | Y `CustomComplianceListener`+`BlockBreakListener` | Y Vaun (D02) | Y rosetta-ring; cairn sites | ~ no report caller (C-1) |
| The Kept Light | Y `LocationSampler.scanKeptLight` | Y Mara/Brann (D05/D08) | Y rosetta-ring; `kept_light` site | ~ no report caller (C-1) |
| The Deep Line | Y `BlockBreakListener` | ~ taught key only; no keeper (C-4) | Y rosetta-ring | ~ no report caller (C-1) |
| The Unspoken | Y `ChatListener` — `forbidden-words: []`, never fires (C-2) | Y Iss (canon §4) | N not taught; `unspoken-refrain` unseeded (C-3) | ~ no report caller (C-1) |
| The Sacred Beast | Y `DeathListener` (tagged mob) | ~ design only; no keeper (C-4) | N not taught; `haunted-herd` unseeded (C-3) | ~ no report caller (C-1) |
| **The Dark Hours** | Y `DarkHoursListener` (NEW) | Y Brann (D08, keeper 5) | N not taught; P13 unseeded (C-3/C-5) | N **no report/beat/voice line** (C-1/C-5) |
| Ward | N **no detection** | ~ named in ring only | Y taught in rosetta-ring | N none (C-6) |
| Covering | N **no detection** | ~ named in ring only | Y taught in rosetta-ring | N none (C-6) |

### GAPS
- **C-1 (HIGH) — customs→report/consequence bridge unbuilt** (nothing reads `custom_compliance`;
  `reportObserved`'s only caller is a README example). → **D1**.
- **C-2 (HIGH) — the Unspoken can never fire** (`forbidden-words: []`). → **D3**.
- **C-3 (HIGH) — three designed custom-puzzles never reached the live seed** (`unspoken-refrain`,
  `haunted-herd`, P13). → **D4**.
- **C-4 (MEDIUM) — Deep Line + Sacred Beast storied thinly / not at all.** → **D6**.
- **C-5 (HIGH, the owner's example) — Dark Hours has no CLUE and no INTERACTION.** → **D2**.
- **C-6 (MEDIUM) — Ward and Covering are taught but do not exist as mechanics.** → **D5**.

### ORPHANS
`DarkHoursListener`+config (no clue/consequence); `the_unspoken` detection (inert); `the_sacred_beast`
detection (no keeper/lore, no seeded puzzle); `reportObserved`/`reportEscalated` (no caller);
`unspoken-refrain`/`haunted-herd`/P13 (design only); "ward"/"covering" (taught + named, no mechanic).
*(See §2 SEAM D.)*

---

## APPENDIX SEAM 6 — CIPHERS ↔ PUZZLES ↔ TEACHABLE KEYS

### VERDICT
The forge ships 11 round-trip-self-tested ciphers, but only 6 are wired to authored seed nodes, and even
those 6 are wired by **design prose, not by the seed** (rows store decoded plaintext only — no cipher
field, no key field, no Rosetta reference). Five ciphers (railFence, columnar, polybius, a1z26, morse) are
ORPHANS in the playable web. Two of the 6 used ciphers have a fairness gap (Caesar's shift; coordEncode's
Rosetta). vigenère/atbash/substitution/book are the fully-coherent exemplars.

### GAPS
- **HIGH — five ciphers implemented + self-tested but used by no active row and taught by no document;
  the docs' "carry other nodes" claim is stale.** → **B1**.
- **MEDIUM — `stone-brann` cipher/clue mismatch** (design = beacon/rail-fence night-gated; seed = flat
  `lore`). → **B2**.
- **MEDIUM — Caesar (`stone-vaun`) is UNFAIR** (no in-world shift; no Rosetta wheel-sign). → **B3**.
- **MEDIUM — `coordEncode`'s teachable key (Stone of Reckoning) is unbuilt in the world.** → **A5/B4**.
- **LOW — seed rows carry no cipher binding at all** (the cipher↔node join exists only as prose). → **B5
  / X1**.

### ORPHANS
railFence (assigned to stone-brann in design; stone-brann is plain lore in the seed); columnar (no row, no
keyword doc); polybius (sealed-only, marker-grid unbuilt); a1z26 (no row, no tick-mark stave); morse (no
row, no Morse-table prop); Caesar shift number (design says shift = Vaun's hoarding; D02 never states a
number; no Rosetta wheel-sign). *(See §2 SEAM B.)*

---

## SEALED NOTE (whole ledger)
The endgame rows, the true final coordinate, the post-catch threshold, the parallel polybius door, the
false-coord dead-end, and the induction twist are sealed (see `arc/_SEALED_ARC_BIBLE.md`,
`arc/cipher-web-seed.sealed.json`, `arc/bestiary-sealed.md`). This audit judged coherence against them
without restating them. Two ordering caveats for when the sealed rows go live: (1) the sealed cipher-web
rows add more `unbroken_light`-targeted and coordinate beats, so **A1/A5/A7 must be resolved before they
activate** or the null-site / untaught-cipher gaps compound at the endgame; (2) the flag-name drift in F5
(`true_coord_known` vs `_found`) must be reconciled before the sealed `true-final-threshold` row reads the
flag. The spoiler-free seed's fold of the true-coordinate yield into `no-wall-catch` is correct and keeps
an unspoiled run whole.
