# THE OBSERVANCE — CHANGE MANIFEST (pass 1 of the fold)

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT USE FOR SETUP, STORY, OR RUNTIME.** This is change history only; current authority is indexed by `design/V5-SUPERSESSION-MAP.md`.

> **SUPERSEDED PRE-V5 ARCHIVE — DO NOT USE FOR SETUP, STORY, OR RUNTIME.** This is change history only; current authority is indexed by `design/V5-SUPERSESSION-MAP.md`.

> **CURRENT STATUS OVERRIDE — 2026-07-13.** This file is implementation history. Phrases such as
> "not yet wired" below describe the pass in which they were written, not the current release.
> The integrated runtime/DB paths, V4 Deep Hold, physical gates, content surfaces, Keeper/Wren paths,
> and release audits are now implemented. Current truth lives in
> `FINAL-LAUNCH-HANDOFF-2026-07-13.md` and `CURRENT-READINESS-VERDICT.md`.

> **What this is.** The single "what we're adding / changing / cutting" ledger produced from two
> research dossiers (Gemini's + Claude's) + the decisions locked with Ethan on **2026-06-30**.
> It is deliberately produced BEFORE touching the interconnected canon, per Ethan's rule: *one
> change pass, then one integration pass that goes over everything so nothing is orphaned.*
>
> **How to use it.** Each entry names its **touchpoints** — every place a change ripples (story /
> plugin / seeds / documents-players-find / callbacks / canon-doc-to-edit). The **integration pass**
> walks this file top to bottom and reconciles every touchpoint. Nothing here is built yet; nothing
> here edits OVERHAUL/PUZZLES/INTEGRATION/BUILD-PLAN yet. Scope tags: `[cheap]` `[medium]`
> `[expensive/park]`. Phase tags map to OVERHAUL §6 / BUILD-PLAN §3.
>
> **Pipeline:** ✅ research → ✅ pass 1: this manifest → **▶ pass 2: cohesion (§6 below)** → ⬜
> lore/character/story pass → ⬜ integration pass → ⬜ build (Phase A first, always).

---

# ✅ CONVERGED (2026-07-02) — the parallel R-fold is ADOPTED into WAVE S

> A **separate autonomous session** produced this R-fold (below) IN PARALLEL, in the same worktree, before
> the WAVE S plan was known. Both sessions independently reached the SAME core audit finding. On review
> (director pass, 2026-07-02), the two are **complementary, both green, both on-direction — so the R-fold
> is ADOPTED, not reverted.** WAVE S remains the authoritative numbering/plan; the R-work slots into it:
> - **R0 (plugin 0.3.0, green) — ADOPTED as WAVE S-B (done).** The label cull. Agrees with S-B on 9 sites.
>   The 3 that S1 had marked KEEP (Unwriting "the seal is a name", Cold Hearth, Sella "far marker") — **let
>   the cuts STAND**: the no-label ethos is more consistent, and each scene still delivers (Unwriting: scraped
>   wall + book + clean slab; Cold Hearth: the book + doused hearth + roots; Sella: pool + copybook doc +
>   carved name). S1's KEEP verdicts on those three are superseded by this call. Coexists green with S-A cribs.
> - **R2 (green) — ADOPTED (supersedes S2 "inviolable").** The journal edits are **additive margin-lines, not
>   rewrites** (+3..19 lines each): they thread the **seven-motif as a ≥3-clue web** (Vaun's tally reads vii;
>   Brann counts 7 shrines/6 lit; Sella's copybook counts 7) AND add **"cipher-as-inversion"** (each keeper's
>   cipher mirrors their flaw — Iss's Vigenère keyed on his own name "gives back only the man"; Vaun's Caesar
>   = a count held back; Orin's substitution = won't let a thing be itself; Mara's book-cipher = points, never
>   says). This makes the ciphers *characterization*, reconciling with Ethan's "pure-lore ciphers are good."
>   The **3 new docs** are FAIR RED HERRINGS (Principle 5, the *misleading* fact-layer done as whole believable
>   documents, counterweighted, never self-refuting) — which is the RIGHT way to layer facts (show-not-label,
>   canon §6.2) and exactly the immersive discovery-lore Ethan asked for. **KEEP all of R2.**
> - **R6 (green) — ADOPTED.** Placement scatter (ObservanceCommand): serves "world, not rows" — on-direction.
>
> **Net:** both sessions' work merges cleanly (verified green together: plugin jar · tsc · seedcheck ·
> gatecheck · specscheck · showrunner). WAVE S §S2 relaxes: keeper-journal *additive plants* are welcome
> (the sin is rewriting the voice or inert puzzle-costume, neither of which R2 did). The R-wave sections
> below are the build record of S-B/adopted-lore; WAVE S (§S0–S8) remains the forward plan.

# ⭐ RESHAPE FOLD (2026-07-02) — [SUPERSEDED BY WAVE S] the add/change/cut pass for the deep reshape

> Produced from the 4-surface live-state audit in `design/RESHAPE-AUDIT.md`, applied against
> `design/THE-RESHAPE.md` (plan) + `design/RESHAPE-RESEARCH.md` (evidence). Mandate: reshape ALL five
> surfaces (structures · lore · ciphers→puzzles · web · integrations) so the world stops announcing "here
> is a puzzle" and becomes a mystery you slowly learn to read. **Core audit truth: most of it is already
> good — this is surgical, not a rewrite.** Organized into BUILD WAVES (each a disjoint lane, buildable +
> green on its own). Scope tags `[cheap]`/`[medium]`/`[expensive]`. ✅=done ▶=building ⬜=queued.

## WAVE R0 — the LABEL CULL (structures) `[cheap]` — biggest feel-change per hour ✅ (0.3.0, verified green)
Every set-piece carries a waxed "label sign" that announces what it is / what to do. That single
contamination is the #1 reason it reads as a puzzle game. The spaces already read as world-mystery.
- **CUT the label entirely (no replacement):** orin ("stoop to read"), iss ("the fire is kept (it lies)"),
  unwriting ("the seal is a name… read it back"), coldHearth ("he sent you out"), unbrokenLight ("bow as one").
- **REDUCE to notation only** (world-cipher, untranslatable w/o the reckoning site): rosetta, reckoning,
  threshold (grave→date only), thresholdVault.
- **Fold cut content into a DIEGETIC surface** where it carried info: vaun's ledger line → partially-worn
  carving on the chiseled stone (final line blank = visible erasure, not an empty form); mara's margin-note →
  into the book text; the answer/submission surface stops being a blank "fill-me-in" sign (mara → the empty
  2nd lectern + bookshelf gap = "return the missing volume"; sella → partly-worn carved name).
- **Touchpoints:** `plugin/.../structure/StructureTemplates.java` (all 14 templates) · answer-sign reachability
  (keep intact) · any RUNBOOK/FIRST-PLAYTEST text that told players to read a label. LORE dependency: what
  notation replaces reduced labels comes from Wave R2 (cipher-as-inversion) — R0 can cut/blank first, R2 fills.

## WAVE R1 — LITERACY RECONSTRUCTION (the P0) `[medium]` — the highest-leverage change ⬜
Kill the alphabet tutorial; make literacy EARNED (Tunic/Fez; fair by the ~25-char frequency proof).
- **CONVERT** the three literacy-gate rows: `rosetta-ring`, `a1z26-tick-stave`, `reckoning-rosetta` stop being
  single-recitation gates. `rosetta_known` / `reckoning_known` get set by ACCRETED comprehension.
- **SCATTER glyph-referent pairings** across all 6 keeper sites — a rune carved *next to the thing it names*
  (Chants-of-Sennaar cribs): mark over the hearth/water/grave/hoard/lamp. ≥3 sites teach enough to force the rest.
- **CUT `stone-sella` Atbash** — redundant with `sella-reflection-bearing` (environmental). Stone becomes the
  observation prompt; bearing comes only from the reflection.
- **Touchpoints:** `discord/.../puzzles_seed.sql` (3 literacy rows + stone-sella) · oracle answers · the
  keeper-site templates (carve the referent-pairs — plugin, coordinates with R0) · the `rosetta_known` gate
  consumers across Java + 2× TS gate mirrors + record · re-sequence every cipher gate that depended on the old
  single literacy flag. **This re-sequences downstream — do the gate audit as part of the wave.**

## WAVE R2 — MOTIF + FACT-LAYER (lore) `[medium]` ⬜
- **Thread the number SEVEN** independently at ≥3 keeper sites (Vaun's tally, Brann's fires, Sella's copybook)
  so "there was a seventh" triangulates instead of resting on the sella→shrine chain.
- **Cipher-as-inversion motif:** author each keeper's cipher as the formal shape of their self-defeat (Caesar=
  held back a fixed amount; Vigenère key = a name that decodes to its own inverse) — ciphers become
  characterization; the pattern-reader who notices all six share the structure arrives at the motif unaided.
  (Feeds R0's reduced-label notation + R1's carvings.)
- **Fix the weak MISLEADING layer:** add ONE earnestly-believed, internally-unrefuted false account (someone who
  accepted Iss's "the Seventh was spared / mercy" framing) so the group holds two accounts in tension; plant an
  M1 fragment that *seems* to confirm good-conduct→kept (only correlation) so F10b lands as a reframe; make F4
  ("customs learned from the land") plausible-but-self-serving, not flat-true.
- **Touchpoints:** `arc/lore/documents/*.md` (new + edits) · keeper-voice content in `discord/src/voice.ts` ·
  the record copy in `record-projection.ts` · OVERHAUL.md fact ledger · `found-documents.md`.

## WAVE R3 — WEB-NOT-CHAIN + THE RECORD (web + integrations) `[medium]` ⬜
Flag graph is already partly a web; the record + drip are the linear bits.
- **Land the staged `stone-brann-cipher`** as the 2nd door into `undercroft-descent` (fixes the mara-sole-door
  fragile node — cheap, row already authored).
- **Environmental corroboration for "descend at the unbroken light"** — the single Undercroft lamp VISIBLE from
  the surface through a gap before descent (structures + a puzzle clue).
- **Fact-layers in the record:** `record-projection.ts` → add `factLayer: confirmed|implied|misleading|false`
  per entry + gate `legible` on `arc_state.flags` keys (not movement index); `record/[slug]/page.tsx` render
  per-layer styling + pass flags into `project()`; `record/terminal` show the certainty distinction.
- **Web-aware drip + certainty gradient:** `voice.drip()` + oracle lines get `certainty: certain|implied|
  false_trail`; `decide.ts` can surface a constellation / imply-without-unlock; add cross-clue edges
  (`outcome_payload.implies_keys[]/contradicts_keys[]` read in `snapshot.ts`/`types.ts`, threaded via `apply.ts`).
- **Touchpoints:** dashboard record files · `discord/src/showrunner/{decide,snapshot,clue-drip,apply}.ts` ·
  `types.ts` · `voice.ts` · puzzles_seed (brann-cipher active + edge fields) · gate mirrors stay in lockstep.

## WAVE R4 — SILENCE / CONTRADICTION environmental pass (structures + lore) `[medium]` ⬜
Make three underused assets load-bearing with zero text:
- **The erasure-smear:** a PHYSICAL blotted line / ink-run in the record-book where the Seventh's name was
  unwritten — the most disturbing thing in the first look (build the artifact; plant its lore).
- **The fire-geometry contradiction = physical proof of F10b:** Brann says the home-fire needs no hand → the
  Seventh's fire going out was a REFUSAL not neglect. Make it an observable world-state clue.
- **Sella's blank-page-as-epiphany:** the Atbash journal is blank away from water, text at the shore pool →
  weaponize "why is this page blank?" as the question that leads to the water (earned literacy, ties to R1).
- **Empty Offering cairn** (deposit-tally reads zero) makes the ledger-vs-record contradiction visible from one
  text + one silence; **Iss keyhole** that turned reveals "the one who turned away" carved behind it (not a door).
- **Touchpoints:** StructureTemplates (record-book artifact, cairn tally, iss keyhole, fire-state) · lore docs.

## WAVE R5 — ORPHAN / CALLBACK cleanup + per-player SIGNAL `[medium]` ⬜
- **Unblock per-player scares (high-leverage):** land the plugin **dossier + visited-cell reader** so
  `conductor.ts shapeRhyme` stops being hardwired `{}` → per-player apparition, offline-skin, grave-targeting,
  name-on-unvisited-wall, Observer tiers all go live (delivery already works via mc_uuid beats).
- **Close orphans:** add the missing `voice.ts` line for discovering your name carved where you've never been;
  give the asymmetric co-op vault a lore justification (the record deliberately splits knowledge); wire the
  **Wren-relocate-during-Watcher-beats** listener (else Wren present at a scare breaks the two-register model);
  add a prior-keeper document that parallels the composure-signal Tier-0 tracking; wire pale-lamb → INV-13 or
  label it cosmetic; ensure Wren's tally-page degrades gracefully if the Observer Engine hasn't populated it.
- **Touchpoints:** plugin measurement layer · `voice.ts` · lore docs · beat listeners · autonomy.run.ts wiring.

## WAVE R6 — PLACEMENT (structures) `[cheap]` ⬜
Both placeregion/placedeep produce an east-marching corridor (±4-block wobble) read as "walk in order." →
non-uniform X intervals, per-site orientation rotation, let terrain (cliff/waterway) separate sites so they
feel independently always-there. **Touchpoint:** `plugin/.../command/ObservanceCommand.java` placement helpers.

### BUILD ORDER & LANING (director's call)
R0 first (cheap, huge, self-contained plugin lane) → R1 (the P0, plugin+seed+oracle, coordinates carvings with
R0) → R2 (lore, mostly docs+voice) → R3 (web+integrations, disjoint from plugin) → R4 (structures+lore) →
R5 (plugin signal + wiring) → R6 (placement). **ONE plugin agent at a time** (R0/R1/R4/R6 all touch plugin —
serialize them); R2 (lore) and R3 (web/integrations) can run PARALLEL to a plugin lane. Verify-green after each.

---

## 0. DECISIONS LOCKED (2026-06-30) — the spine of this fold

| # | Decision | Supersedes / notes |
|---|---|---|
| D1 | **Discord = a haunted *surface*, never a persona.** Silent for weeks, then leaks *corrupted artifacts* on in-game triggers (a degraded audio file, a status/PFP change to something they're looking at). No dialogue, nothing presents as "The Watcher." | Refines OVERHAUL Pillar 4 / §3 ("Discord has no game-persona") — this *permits* the artifact-leak while keeping the TINAG fix. |
| D2 | **No player is ever an inside man.** The saboteur mechanic is CUT. Replaced by a **traitor-companion NPC** (D3). | Kills Gemini Part 5 "Saboteur." Betrayal is felt by the whole group *together*, not between friends. |
| D3 | **The traitor-companion (Option C + bad intent).** A **Kept** who has been feeding the group to the Watcher to preserve his own remaining self, and steering them toward the Seventh as *his* escape key. His "I was protecting you from being taken like Iss" is a half-believed excuse over genuine self-interest. A **present-tense mirror of Iss** (Iss lies from pride/historical; companion lies from shame+self-preservation/living). Reckoning = the group + the record decide the truth he won't say. | New load-bearing character + emotional spine. Wires the Observer Engine ("he was the channel"). Needs its own lore doc, voice arc, seeds, NPC, retroactive tells. |
| D4 | **NPC framework = hybrid.** **Citizens2** for anyone meant to read as a regular person as far as players can tell (the companion; surface townsfolk Aro/Wenna/Dob/Pell). **Vanilla-uncanny** (armor-stand/display + PDC + interaction entity) for everything non-human/uncanny (the six keepers, apparitions, the Watcher, statue-things). | Resolves BUILD-PLAN §16.2 / OVERHAUL §3 open NPC decision. Adds Citizens2 as a justified dependency. |
| D5 | **Version pin becomes load-bearing** now that Citizens2 is in. Pick ONE Paper 1.21.x and a matching Citizens2 build; author for exactly it. | Elevates BUILD-PLAN §15 / §16.12 from LOW to a real pre-build step. |
| D6 | **Governing lens = the known-author reframe.** Our players *know* Ethan built it; classic "is this real?" TINAG is off the table. Immersion comes from (a) diegetic consistency, (b) reactivity they didn't expect us to build, (c) discovery deeper than they assumed we'd bother with. This governs every fold below. | New framing for OVERHAUL §7 hard rules. Not a contradiction of TINAG — a correct application of it for this audience. |
| D-NE | **Nether + End lanes: KEEP and INTEGRATE** (2026-06-30; closes the BUILD-PLAN §16.1/§9.B open decision). Both stay **non-gating optional deepening** (INV-12/19 — never required to reach the reunion), both **zero-manual** (re-dressed vanilla end-cities/ships + bastions/soul-sand via A11/A12). **Nether = the *origin* lane** (the Kept-Light's source; the oldest keepers who went toward the fire; deepens the ways' origin + the induction). **End = the *exile* lane, RECONCILED to v2:** where the Seventh was cast out FIRST and carved "the name i cut myself" — *before* they understood and **returned down** to wait at the deep (v2's living reunion). The End is the Seventh's turning-point/testimony, NOT their current home. | Fixes the stale End contradiction (BUILD-PLAN §9 "not down… the other way" — the worst offender). Reconciliation is §9's own sanctioned fix. Turns the optional End into a reunion-pointer without gating it. See L1. |
| D7w | **World-build model = ZERO manual build for Ethan** (2026-06-30, hardened). Ethan hand-builds *nothing* — not required anywhere. The world comes from two code-driven sources only: (1) **vanilla-generated bones + a code "dresser" pass** (ancient cities / trial chambers / villages re-dressed by the plugin, no hand-overlay); (2) **procedural code-generation** for everything else (primitives AND set-pieces, built from block templates / modular jigsaw pieces by the director). Coords captured by a `site set` survey command, never hand-edited. Hand-building any space is Ethan's *option*, never a requirement. | Fully removes BUILD-PLAN §2.1 blocker #1 + neutralizes the §15 bus-factor risk. **Shifts the quality burden onto procedural-build craft (a real R&D task — A11).** Extends the codebase's "vanilla re-read in-fiction, additive-only" pattern. See A11/A12. |

---

## 1. ADD — new content/systems (each tagged with touchpoints + scope + phase)

### A1. The traitor-companion NPC — the present-tense betrayal `[medium]` (Phase B–E; reveal late)
- **What:** a companion NPC (Citizens2, passes as a normal player-friend) who joins the descent early,
  builds genuine trust over weeks (helps, warns, gifts), and is revealed to be a Kept feeding the group
  to the Watcher (D3). Reckoning gives the group a **choice** (condemn / understand / free) that feeds
  the ending.
- **Why:** replaces the cut saboteur with a shared wound; supplies the in-fiction *reason the Watcher
  knows real things*; mirrors and escalates the Iss-catch lesson.
- **Touchpoints (integration pass MUST hit all):**
  - *Story/premise:* OVERHAUL §0–1 + Pillar 1 (a living companion now walks with them); §5 KEEP ledger.
  - *Lore doc:* NEW `arc/lore/documents/the-companion.md` (his voice, the excuse, the truth, the tells) —
    author him with the same care the Seventh got (BUILD-PLAN §6 pattern).
  - *Voice:* `voice.ts` — his trust-arc lines, the betrayal reveal, the three reckoning branches; keep a
    grammatical fingerprint distinct from Iss and the six.
  - *Seeds:* new flags (`companion_introduced`, `companion_trust_N`, `companion_tells_seeded`,
    `companion_revealed`, `reckoning_condemn|understand|free`) + storylets; gate the reveal behind the
    Iss-catch (you learn the pattern on Iss first). Re-run seedcheck/specscheck/gatecheck.
  - *Plugin:* Citizens2 companion behavior; his artifact-leak hooks feed D1 (Discord) + the Observer
    narrative; retroactive-tell beats planted across earlier storylets.
  - *Observer Engine (Pillar 5):* he is the diegetic channel — reframe the "how does it know" narrative
    so his betrayal *pays off* the Observer scares retroactively.
  - *Website/record:* his entries appear in the record; his leak corrupts it.
  - *Ending:* reckoning choice → a branch on the finale/fates (OVERHAUL Pillar 1).
  - *Callbacks:* differentiate from Iss everywhere (pride/past vs shame/present); ensure the reveal is
    retroactively legible (tells planted, not invented at the end).

### A2. The Observer Engine, Tier 0 = the "composure signal" (Gemini's Stress Metric) `[cheap]` (Phase D, but Tier 0 usable earlier)
- **What:** a behavior-only per-player signal (time in dark, recent damage, alone-vs-grouped, hoarding
  one item, revisiting one block) that lets the Watcher speak in grounded *implication* with **zero
  chat/voice/LLM**. Makes "it knows me" land at zero infra + zero consent cost.
- **Why:** de-risks the whole Observer north star; the arc must work at Tier 0 (BUILD-PLAN §13).
- **Touchpoints:** OVERHAUL Pillar 5 + BUILD-PLAN §13 (flesh out Tier 0 from "profile behavior" to this
  concrete signal); plugin (a `Composure`/attention accumulator — extends the existing Attention layer);
  PUZZLES §1 "Voice-heard" cousin = a **behavior-heard** implication line; grounding invariant (§4) still
  applies (only real behavior).

### A3. New puzzle TYPEs — Minecraft-native + forensic `[cheap]` (Phase C/E)
Add to PUZZLES §1 menu + §5 palettes. All vanilla-first, veteran-flavored, cure cipher-monotony:
- **Map-art forced perspective** — an item-frame mural that resolves only from one standing block.
- **Banner heraldry cipher** — 6 keeper sigils built from banner patterns = a substitution alphabet.
- **Lectern-page redstone lock** — lecterns turned to pages (page = signal strength) complete a circuit;
  the combination is a *journal quote*.
- **Chiseled-bookshelf register** — a comparator-read 6-slot positional puzzle that opens a door.
- **Calibrated-sculk "it hears you"** — a corridor passable only in silence (sneak/muffle); a shrieker
  that answers *voice chat* (ties to Observer). Sculk = the Watcher's sensory organ made literal.
- **F3 as a diegetic instrument** — clues that are a biome name / coord / "looking-at" readout.
- **Villager-trade oracle** — a walled villager that trades *answers* for offered items (a Kept? the
  Seventh's proxy?).
- **NBT-"heavy" item stego** — a normal-looking item whose NBT hides a URL/hex; *meant* to be inspected
  (weaponizes datamining, see D7).
- **Carved-pumpkin-overlay reveal** — text invisible until a carved pumpkin is worn (vanilla overlay,
  **no shader**).
- **Recovery-compass / lodestone pointer** — the needle that settles toward the Seventh once earned.
- **Item-frame rotation dials** — 8-position physical combination locks.
- **Touchpoints:** PUZZLES §1 (TYPE menu) + §5 (assign to keeper palettes) + §4 `answer_kind` (several
  are `behavior`/`object`/`none`); INTEGRATION Layer 3–4 (listeners); hint rail per puzzle.

### A4. Display + Interaction entities as the illusion backbone `[medium]` (Phase C)
- **What:** use `text_display`/`block_display`/`item_display` (packet, per-player, transform-animated)
  for floating runes, block-built faces/figures, glitch-corruption text, "the thing in the trees" one
  player sees; **Interaction entities** for clickable diegetic buttons/NPCs without Citizens.
- **Why:** delivers the per-player illusion + replaces the CUT ModelEngine bestiary with **zero
  dependency** (removes a dep while adding capability); INTEGRATION Layer 3 already lists these as ⬜.
- **Touchpoints:** INTEGRATION Layer 3 (promote from note to backbone; fold the cut-bestiary *lore* into
  vanilla-reskin + display apparitions); PUZZLES §5 reflection/visual puzzles; the FAWE-async bug
  (INTEGRATION Layer 3 / BUILD-PLAN §2.6) must be fixed or these stutter.

### A5. The "recovered system" reframe of the record website `[medium]` (Phase B)
- **What:** the record website is not a clean puzzle site — it's a **half-corrupted archive terminal**
  of the colony's own record-keeping (Gemini's "leaked intranet, not a lore website"). This UNIFIES four
  things into one diegetic surface: the record (write names/answers in), the hint rail (as an
  **error-log / integrity checker** that clarifies the longer a thread stalls), the Iss lie (falsified
  entries you correct), and the Seventh's true record (what you restore).
- **Why:** big cohesion win, not new scope — it re-skins surfaces we already planned as one artifact.
- **Touchpoints:** OVERHAUL Pillar 4; INTEGRATION Layer 5 (the record website) + the hint rail; PUZZLES
  §2 (surface) + the hint content; the website security model (D-risk: RLS/edge-function only, never the
  service key — BUILD-PLAN §15).

### A6. Relief beats / the pacing "exhale" `[cheap]` (Phase B/E)
- **What:** explicit **Reward & Reset** beats (Gemini's L4D "Relief" phase) so the arc isn't unbroken
  dread over weeks. Home them in the existing **Warm-Grief** tone slot; keep them **diegetic** (a
  keeper's kinder memory, a safe hearth, a small true gift — the "ways that companied the Dark"), never
  a gamey healing chest.
- **Why:** dread without exhale is fatigue, not tension. Reframes an existing tone slot to also carry
  pacing relief.
- **Touchpoints:** OVERHAUL Pillar 2 (tone rotation — note Warm-Grief now also serves relief);
  showrunner salience (a relief beat is *scheduled* after a climax, not random); BUILD-PLAN §14 playtest
  (watch for fatigue).

### A7. Async-first / convergence-gated design invariant `[cheap]` (cross-cutting)
- **What:** 6+ veteran friends never all log on at once; the "everyone converges on the weekly tick"
  model breaks. Progress **persists and leaves traces** (the record website = async shared brain); most
  puzzles are solo/"night-shift"; only **convergence beats** (the co-op vault, the reckoning) need
  quorum — quorum relative to the **active** roster.
- **Why:** postmortem-grounded (scheduling + dead air kill ARGs); also reinforces the dynamic-roster
  invariant.
- **Touchpoints:** OVERHAUL §4 invariants (add async-first beside dynamic-roster + grounding/consent);
  salience tuning (a solo player can advance a thread); BUILD-PLAN §4 cross-cutting invariants.

### A8. Layered difficulty + escalating hint rail `[cheap]` (Phase B)
- **What:** every spine puzzle has a fast surface read *and* a deep true read (nobody fully blocked);
  the hint rail is the A5 error-log that escalates; wrong attempts still produce content (reactivity).
- **Why:** the two ways veterans quit — solving faster than expected, and stalling on fragile single-path
  puzzles (Portal 2 / RotMG postmortems).
- **Touchpoints:** PUZZLES §7 (hint content) + the empty `hints` table; OVERHAUL §5 (content buffer from
  `ideas/`, not new scope); the Golden Question gate (D8).

### A9. Discord haunted-surface artifact leaks `[medium/park til Phase D]` (Phase D)
- **What:** implement D1 — the bot leaks corrupted artifacts on in-game triggers (enters a cursed
  chunk → a degraded ogg drops / status changes to what they're viewing). No persona, no dialogue.
- **Touchpoints:** OVERHAUL Pillar 4 (permit the artifact-leak); the bot/hosting; the Observer Engine
  (same infra); consent disclosure (§4 — Discord is watched *and* can be written to).

### A10. Sculk / Deep-Dark as the Watcher's sensory organ (thematic thread) `[cheap]` (Phase C/E)
- **What:** lean the Undercroft/atmosphere into sculk sensors/shriekers/darkness as *the way the Watcher
  perceives* — thematically perfect and mostly vanilla; feeds A3's silence puzzles.
- **Touchpoints:** INTEGRATION Layer 2 (datapack biomes/mood_sound) + Layer 3; art direction (BUILD-PLAN
  §12); WORLD-BUILD.md (the Undercroft).

### A11. Director structure-generation system — "the director builds the world, not Ethan" `[medium/R&D]` (Phase A onward; unblocks the world-build)
- **What:** implement D7w's two code-driven sources. **(a) Vanilla-gen + code dresser:** the plugin
  overlays runes/carvings/decay/lore onto located vanilla structures (A12) — no hand-overlay. **(b)
  Procedural code-generation:** the director builds primitives (keeper stone / cairn / answer lectern /
  plate) AND set-pieces (reflection room, bookshelf-register room, threshold) from block templates +
  **modular jigsaw pieces** assembled by code. Plus a **`/observance survey` / `site set`** command to
  capture coords by walking-and-clicking. (Schematic-stamp is available if Ethan ever *opts* to author a
  piece, but it is NOT required — needs the FAWE async fix if used.)
- **THE R&D SUB-TASK (flagged explicitly — this is where "really good" lives):** learn/encode
  **cohesive procedural Minecraft building** so code-placed builds look *intentional*, not noise. The
  craft levers: a **tight block palette + strict lighting discipline** (dark default, light earned);
  **decay/wear passes** (soot, cracks, moss, rubble, half-collapse); **symmetry + modular jigsaw
  assembly** (mirror the vanilla village/trial-chamber approach — small authored-in-code pieces the
  director stitches); rule-placed **"wrongness"** details; **FAWE relight** after every write. Study
  references before the build (vanilla jigsaw system, known procedural builders). *Without this craft the
  zero-manual world looks generic — see the new risk in C9.*
- **Why:** fully removes BUILD-PLAN §2.1 blocker #1. Makes the world **relocatable/regenerable** →
  instant test worlds for the §14 playtest loop.
- **Reveal-safety:** generation happens pre-session / unwitnessed, never built toward a watching player
  (unless it's an intended per-player illusion). Follows the additive-only / never-overwrite rules in
  `sites.yml` (nether/end lanes) + OVERHAUL §5.
- **Touchpoints:** BUILD-PLAN §2.1 (blocker #1 removed), §11 (Minimum Amazing = one *code-generated*
  room proves Phase A), §12 (art direction = the procedural-craft levers above are now first-class),
  §15/§16 (risk); INTEGRATION Layer 2 (datapack `/place structure` + jigsaw) + Layer 3 (the generation
  capability + fix the FAWE main-thread paste); `sites.yml` (coords director-captured); Phase A step
  "build one room" → "generate one room."

### A12. Vanilla-generated structures as re-dressed sites `[cheap–medium]` (Phase A onward; the free bones)
- **What:** use natural Minecraft generation for the world's bones, re-dressed additively so it reads
  as ours. The standouts:
  - **Ancient City (deep dark) = the Undercroft / keeper-stone sites.** It *is* the fiction — a
    built-then-abandoned civilization drowned in sculk, dark, Warden-haunted. Saves the most expensive
    build; plugs into A10 (sculk = the Watcher's sense) + a legitimately-placed Warden as dread.
  - **Trial Chamber vault (1.21) = the asymmetric co-op vault reward mechanic** (signature #2). Vaults
    are a vanilla lock-and-key with **per-player keys** (dynamic-roster by default) — back the co-op
    vault with a real trial-chamber vault, near-zero plugin work.
  - **Village = the surface town** (Aro/Wenna/Dob/Pell), dressed + Citizens townsfolk (D4).
  - **Mineshaft / stronghold / ruined portal / ocean ruins = "recovered ruins" lore anchors.**
- **Why:** free, on-theme "built-then-abandoned" texture at a fraction of the build cost; extends the
  codebase's proven "vanilla re-read in-fiction, additive-only, never overwrite" pattern
  (`bastion_remains`/`soul_gallery`).
- **Division of labor (the rule):** generated structures for **connective tissue + dread-texture + the
  vault**; **authored/code-placed** (A11) for **load-bearing precision puzzles** (never leave a
  comparator-read bookshelf lock to raw generation).
- **Caveats:** veterans recognize vanilla raw → **must re-dress, by CODE not by hand** (the A11 dresser
  pass overlays carvings/runes/decay additively — zero manual overlay from Ethan); generation places by
  seed → `/locate` + anchor, or force-place via `/place structure`/datapack for load-bearing ones, or
  seed-select; manage vanilla mobs/spawners so they don't fight the authored beat (a legit Warden in an
  ancient-city Undercroft is *wanted* dread — but gate it so it can't TPK the whole convergence); all
  available on the pinned 1.21.x (D5).
- **Touchpoints:** OVERHAUL §0/§1 (the world's bones) + §5 (extends the re-read/additive pattern);
  INTEGRATION Layer 2 (datapack structure) + a new "vanilla structures re-dressed" note; PUZZLES §5 +
  signature vault (the co-op vault = a trial-chamber vault); BUILD-PLAN §12 (dress-not-build) + §11;
  `WORLD-BUILD.md` (rewrite around locate + dress, not build-from-scratch); `sites.yml` (`structure`-type
  sites anchored to generated structures); A1 (the co-op vault the companion's betrayal may hinge on).

---

## 2. CHANGE — modify existing canon (reconcile, don't just append)

- **C1. OVERHAUL §7 hard rules** — add D6 (known-author lens) and D7/D8 as design gates.
- **C2. OVERHAUL §4 invariants** — add **async-first** (A7) as a third invariant.
- **C3. OVERHAUL Pillar 4** — explicitly permit the **Discord artifact-leak** (D1) as distinct from a
  persona; note the record website is the **recovered system** (A5).
- **C4. OVERHAUL Pillar 5 / BUILD-PLAN §13** — flesh **Tier 0** into the concrete composure signal (A2);
  reframe the Observer's "how it knows" as (partly) **the companion's leak** (A1).
- **C5. OVERHAUL §3 / BUILD-PLAN §16.2** — replace the open NPC decision with **D4 hybrid** (Citizens2 +
  vanilla).
- **C6. OVERHAUL §5 CUT ledger** — the cut **ModelEngine bestiary** *lore* is salvaged into A4
  (vanilla-reskin + display apparitions), not discarded.
- **C7. PUZZLES §1/§5** — inject A3 types into the menu + keeper palettes; keep exactly the 5
  letter-ciphers (unchanged), now an even smaller minority.
- **C8. INTEGRATION** — Layer 3 promote display/interaction entities to the illusion backbone (A4);
  Layer 5 adopt the "one artifact, many windows" cohesion doctrine (A5/D1); add the Discord surface row.
- **C9. BUILD-PLAN §15 risk register** — add: Citizens2 dependency + version drift (D5); companion reveal
  landing flat / redundant-with-Iss (A1); async model mis-tuned → dead air (A7); **procedural
  code-generated world looks generic/bad (D7w/A11) — MED-HIGH, mitigated by the A11 craft levers +
  leaning on already-good vanilla-gen bones; validate in Playtest 1.** Also **retire the old "sole
  builder / manual world-build" HIGH risk** — the zero-manual model removes it (replaced by the
  procedural-craft risk above).
- **C10. BUILD-PLAN §16 open decisions** — close #2 (NPCs=D4), #7 already drafted; add the companion's
  exact motive-mix as a *resolved* note (D3); Nether/End (#1) still open — flag, don't touch.
- **C11. BUILD-PLAN §2.1 blocker #1 reframed** — from "hand-build the Deep Hold (~10–20 hrs), fill
  `sites.yml` coords" to **"choose a seed with good ancient-city/trial-chamber placement → director
  stamps code-placed primitives + schematic set-pieces → re-dress vanilla bones → hand-build only the
  few hero spaces → capture coords via `site set`"** (D7w/A11/A12). Correspondingly **downgrade the
  §15 bus-factor / world-build risk** from HIGH toward MED.
- **C12. INTEGRATION** — add the **structure-placement tiers** (A11) as a plugin capability (Layer 3,
  and fix the FAWE main-thread paste) and a **datapack `/place structure`** note (Layer 2); add a
  **"vanilla structures re-dressed"** row (A12) to the world/atmosphere layers.
- **C13. The signature asymmetric co-op vault** (OVERHAUL Pillar 3 / INTEGRATION signature #2 / PUZZLES
  §6 ex.4) — reframe onto a **vanilla trial-chamber vault + per-player keys** (A12). Note the possible
  tie to the companion's betrayal (A1) if the vault is a convergence beat he's been steering.
- **C14. `WORLD-BUILD.md`** — rewrite from "build these rooms by hand" to "locate + re-dress vanilla
  bones; list which sites are code-placed (Tier 1) vs schematic (Tier 2) vs hand-built (Tier 3)."
- **C15. DB / MIGRATIONS (additive-only — nothing previously applied changes).** Verified 2026-06-30:
  applied = 0001–0005; `0006_requires_flags.sql` exists but is **still PENDING apply** (keystone;
  additive+idempotent; everything gated — incl. the companion reveal — needs it). **New migrations our
  fold adds (never edits an old one):** `0007_answer_kind` (add `answer_kind` col on `puzzles`, default
  `'phrase'` so existing rows are untouched — for A3/PUZZLES §4); `0008_observations` (Phase D — Observer
  table). **No schema for companion / Nether / End** — their flags are jsonb keys in `arc_state.flags`
  (0006), i.e. seed+code only. **Seeds re-run safely** (idempotent `ON CONFLICT` upserts) after the
  companion/lane/hint/artifact edits. **Ethan's DB to-do:** apply 0006 → apply 0007 → re-run seeds →
  (later) 0008.

---

## 3. CUT / PARK — say no on the record (so they don't creep back)

| Item | Verdict | Why |
|---|---|---|
| Player saboteur / sleeper agent | **CUT** | Poisons a real friend group; replaced by A1. (D2) |
| Forced shaderpacks / GLSL screen filters | **CUT** | Hardware lottery, heavy dep, redundant with per-player illusion; salvage only the pumpkin-overlay trick (A3). |
| Geolocation-tied-to-host puzzles | **CUT** | Fiddly, semi-doxxes the host; keep only real-time black-moon (already canon). |
| Grudge-state scheduled NPC AI | **CUT (principle kept)** | The Observer + attention accumulator already delivers "the world remembers you"; a per-NPC grudge machine is sole-builder quicksand. |
| World-seed secret puzzles | **PARK** | Delicious deep layer, but post-Minimum-Amazing; seed-discipline cost. |
| Full recovered-recordings video suite; Google Voice line | **PARK** | Enhancement; Minimum Amazing ships without them (BUILD-PLAN §11). |
| Observer Tier 2 (Discord voice/Whisper) | **PARK (build last)** | BUILD-PLAN §13 — arc must work at Tier 0–1; voice is optional/expensive. |
| Ban-healing-in-chunks dread lever | **PARK (minor)** | Cheap, fine later; not load-bearing. |

---

## 4. NEW DESIGN GATES (apply to every future add — cheap, high-leverage)

- **D7. Anti-datamining stance.** Assume transparency (don't build fun a file-read destroys — the joy is
  in the *doing*, not the *not-knowing-where*) **and** weaponize the meta (reward the datamine: the
  NBT-heavy item is meant to be inspected; leave a message for the xrayer). *(→ OVERHAUL §7.)*
- **D8. The Golden Question.** For every puzzle: *"If they ignore my intended solution, is there another
  logical path to the truth?"* If no, it's too fragile — make the environment reactive to their attempts.
  *(→ PUZZLES §0 rule + OVERHAUL §7, beside the cohesion gate.)*
- **D9. Economy of mystery.** Every answer should open a slightly bigger question until the finale; the
  Seventh reunion must pay off emotionally *and* concretely (no empty mystery box). *(→ OVERHAUL §1/§6.)*

---

## 5. INTEGRATION-PASS CHECKLIST (the callback web — what must stay in lockstep)

When the integration pass runs, for EACH of A1–A10 confirm all of: **(a)** the canon doc(s) in §2 are
edited; **(b)** the story/premise still coheres; **(c)** the plugin capability exists or is on the
build list; **(d)** the seeds/flags + specscheck/seedcheck/gatecheck stay green; **(e)** any
player-facing document (lore doc, website, record entry, found media) that *references* it is updated;
**(f)** callbacks that *pay it off* later are planted, not invented at the end. The single highest
callback-density item is **A1 (the companion)** — its tells must be planted across many earlier beats,
and it must be differentiated from Iss in every place both appear.

**Nothing in this manifest is built or wired yet.**

---

## 6. COHESION PASS (2026-06-30) — folds reconciled against each other + the four canon docs

Ten real tensions surfaced; each has a resolution the **lore/story pass** and **integration pass** must
honor. None is a blocker — but each is a place two good ideas would have quietly contradicted.

### Tensions + resolutions

- **T1 — Companion-as-Observer-channel (A1) vs Observer Tier 0 behavior-only (A2).** If the companion's
  betrayal is *the* reason "it knows," what explains the Tier-0 ambient "it noticed you" before/without
  him? **Resolution: two registers.** Tier 0 = the *land/record itself* faintly noticing behavior
  (ambient, always-on, needs no in-fiction agent — the Hold watches). The companion channel explains the
  **sharp, quoted** observations (Tier 1/2 — real phrases/plans said back). The reveal recontextualizes
  the *precise* scares, not the ambient dread. **Consequence to author:** after the reckoning
  (free/condemn), the sharp-quote scares must *change* (go quiet / shift source) — losing the channel
  must be *felt*. Sequence: ambient Tier 0 from day 1; sharp quotes ramp with companion trust; both
  re-evaluated post-reckoning.

- **T2 — Async-first (A7) vs convergence beats (co-op vault / reckoning / Accepting bow).** If salience
  surfaces a quorum-gated convergence thread when the roster is small/scattered, it stalls — the exact
  dead-air A7 fixes. **Resolution:** salience must be **roster-aware** — never surface a
  convergence-required thread unless active roster ≥ its effective quorum; else surface a
  solo/night-shift thread. Extend the showrunner salience to read the existing
  `effectiveQuorum = min(config, activeRosterSize)`.

- **T3 — Trial-chamber vault (C13) vs asymmetric-info partition (Pillar 3).** The vanilla vault-open
  (per-player keys) is a *different* mechanic than asymmetric-info (each sees a fragment, combine aloud);
  conflating them loses the "talk to combine" magic. **Resolution: compose, don't conflate.** The
  asymmetric fragments *produce* the combination/keys; the vault is the reward container the combination
  opens. Fragments = puzzle; vault = payoff. Integration keeps both.

- **T4 — Zero-manual vanilla-gen (A12) vs load-bearing precision puzzles (A3).** Bookshelf-register /
  lectern-lock need exact block layouts; vanilla-gen is imprecise. **Resolution:** precision puzzles are
  **procedural code-generation** (A11 source (b) — deterministic, exact), never vanilla-gen, never
  hand-built. Vanilla-gen = atmosphere/connective tissue; code-gen = precision. Make this explicit for
  puzzle rooms.

- **T5 — Discord artifact-leak (D1/A9) vs grounding + no-persona.** A leaked artifact could read as "a
  character posting." **Resolution:** the artifact is a **corrupted echo of something real** (a clip of
  their own VC, a screenshot of what they're looking at) — grounded, no dialogue, no authored message.
  Depends on Observer capture existing (A9 ⟶ Observer infra); sequence D1 *after* capture works.

- **T6 — A5 "recovered-system" website hint rail vs the existing `hints` table + whisper infra.** No
  contradiction — same mechanism (tiered hints), two surfaces (in-world whisper + website error-log
  skin). **Resolution:** `hints` table stays the single source; render it in-world AND on the website;
  integration must ensure they don't double-deliver or desync.

- **T7 — Companion (Citizens2, walks with group) vs dynamic roster + late joiners.** One NPC for a
  changing roster. **Resolution:** the companion is **group-scoped** (one NPC, trust is a group flag
  `companion_trust_N`, betrayal is one group event). Late joiners onboarded by the record (existing
  pattern) + a companion line ("a new hand — good, we'll need it"). Author the late-joiner path.

- **T8 — Vanilla hostile mobs (Warden in ancient city) vs authored stalker/dread beats.** A real Warden
  could TPK or collide with the authored per-player stalker. **Resolution: two registers** (the codebase
  already separates them via DramaBudget): the ancient-city Warden = *ambient* dread of the deep (gated
  so it can't wipe a convergence beat); the authored stalker (per-player illusion) = the *directed*
  scare. Suppress/manage vanilla spawns where they'd fight a directed beat.

- **T9 — Nether/End lanes (open decision) vs zero-manual + A12.** *Not* a conflict — they're already
  re-dressed vanilla (end-ships/bastions), which now fits A12/zero-manual perfectly; zero-manual lowers
  the cost of "keep." Still purely Ethan's include-or-cut call (flag, don't resolve).

- **T10 — Relief beats (A6, Warm-Grief slot) vs the companion being the group's main warmth (A1).**
  Opportunity, not just tension: if relief comes mostly *from the companion*, the betrayal retroactively
  poisons every warm moment (powerful) — but risks leaving zero safe warmth after the reckoning.
  **Resolution (a lore-pass decision, flagged):** distribute relief across **both** the companion
  (warmth that later curdles) **and** untainted keeper Warm-Grief memories; post-reckoning the
  keeper-memory relief remains as the honest warmth.

### No-conflict confirmations (compatible as-is)
D6 known-author lens reinforces grounding/consent (§4); A3 Minecraft-native puzzles slot cleanly into
PUZZLES' 4 axes; A4 display/interaction entities are already ⬜ in INTEGRATION Layer 3; A8 layered
difficulty + A5 error-log hint reinforce each other; D7/D8/D9 design gates are purely additive.

### Orphans / stale language the integration pass MUST reconcile
- **Any "hand-built" world language** across canon must flip to zero-manual: **BUILD-PLAN §2.1, §11
  ("hand-build one room"), §12 ("built-by-hands"), `structures.md`, `WORLD-BUILD.md`.**
- **`structures.md` is currently a HAND-BUILD SPEC** → must become a **procedural-generation + dressing
  spec** (the biggest single doc rewrite the zero-manual decision forces).
- **`SESSION-ZERO.md` / any go-live checklist** implying Ethan builds the world → update to the
  `site set` survey + director-generation flow.
- The Observer "how it knows" narrative in OVERHAUL Pillar 5 must be reconciled with T1's two-register
  split (ambient land vs companion channel).

**Cohesion pass complete.**

---

## 7. LORE / CHARACTER / STORY PASS (2026-06-30, in progress) — narrative authoring + reconciliation

> Pass 3. Produces the story DECISIONS + draft prose; the integration pass (4) then wires them into
> every file (docs / seeds / voice / plugin / sites) checking every callback. Items here are authored,
> not yet wired.

### L1. Nether + End integration (per D-NE) — DONE (drafted)
- **Nether (origin lane):** seal the pending FACT-11 sentence so it ships — *"the kept fire was carried
  up from below the bottom; the Undercroft is the bottom of the Hold, the deep-fire its source — one
  direction, not two."* Undercroft stays the bottom; the Nether is the source, never a second bottom.
  Role unchanged: the on-site read reveals the Kept-Light origin (keeping = a *carrying* → sets up the
  induction), sets `nether_forge_found`, grants bonus Whisper budget, colorant-only (not a fate input).
  **Moves here:** the ways'-origin / oldest-keepers deep lore (decompress it out of the Undercroft).
- **End (exile lane) — RECONCILIATION (touches the sacred Seventh):** reframe from "the Seventh's home
  outside the record" → **"where the Seventh was cast out FIRST, carved their name, and waited — before
  they understood and went back DOWN."** The End shrine + `the-name-i-cut-myself` become the Seventh's
  **turning point**, not their end-state. Draft add at the carving's foot:
  > *(cut later, in the same hand, the tool gone blunt:)* i went back. not to be kept — i had my answer
  > about keeping, and it was no. i went back because a record with a lie in it is a wound that stays
  > open, and someone will come to close it, and when they do i mean to be there. i am not out here. i
  > only carved this where it could not be unwritten. **look for me below.**
  - Effect: the optional End lane now **points to the reunion** (enrichment, never a gate — skip it and
    the spine still reaches the deep; walk it and the reunion lands harder + you learn *why* the Seventh
    waits). `seventh_seen_out` stays group-scoped (not a fate input); it now also licenses a deeper
    reunion register.
- **Both lanes:** flip `sites.yml` entries to real gen anchors (A11/A12 code-gen + dresser); confirm
  non-gating; the `end_exile_hold` INV-16 binding (names no living player) still governs.
- **Touchpoints (integration pass):** `arc/WORLD-BIBLE.md` §12; `arc/lore/documents/{the-name-i-cut-
  myself, the-fire-they-let-out, the-fire-kept-me, the-seventh-not-kept, the-seventh-below}`; seeds
  (`progression_seed`/`seventh_seed`/`puzzles_seed` — nether-forge, end-seventh-out, `seventh_seen_out`,
  `nether_forge_found`); `voice.ts` (Seventh return register); `sites.yml` (enable + anchor both lanes);
  BUILD-PLAN §9 (retire the stale-End flag) + §16.1 (decision closed).

### L2. The companion — DONE
Authored: [`arc/lore/documents/the-companion.md`](../arc/lore/documents/the-companion.md) — full character
(working name **Wren**), the "kept-in-part, paying to stay a person" motive (D3), voice fingerprint
distinct from Iss + the six, trust→crack→reveal→reckoning arc, the three reckoning branches, the
post-reveal "**kept close**" found-tally artifact, and the §7 wiring notes (Observer channel, Iss
differentiation, relief split, roster/async, Citizens, finale branch, seed flags). **Canon check passed:**
a *finished* Kept can't walk/talk (WORLD-BIBLE §1) → he is a **fourth face of "kept"** (kept-in-part),
which costs nothing and earns the Observer everything.

### L3. Two-register Observer — DONE (spec)
- **Ambient register (Tier 0 = the LAND).** Grounded in the composure signal (A2); speaks in *implication*
  from behavior only; **never names, never quotes**; always-on; needs no channel/consent-cost. Voice = the
  Hold noticing ("you keep one thing you never use"; "you haven't looked up since you came down").
- **Sharp register (Tier 1/2 = via WREN).** Quotes *real* words/plans; rare, precise, uncanny; harvested
  through Wren (the "kept close" tally, L2 §6). Ramps with `companion_trust`.
- **The reckoning transition (the payoff T1 named).** `condemn`/`free` → the sharp quotes **cease** (the
  channel is gone; the world goes quieter — its own grief/relief). `understand` → they persist but the
  group now knows the source, so they read as *sorrow*, not threat. Grounding invariant holds in both.

### L4. Recovered-system reframe — DONE (spec)
- The record website = a **corrupted archive terminal of the Hold's own record-keeping** (not a clean
  puzzle site — Gemini's "leaked intranet"). Aesthetic: degraded, half-redacted, entries out of order,
  integrity warnings. **Unifies four surfaces into one artifact:** the ledger (names write in) · the hint
  rail (an **"integrity check / error log"** that surfaces clearer warnings the longer a thread stalls) ·
  the Iss lie (falsified entries the group corrects) · the Seventh's true record (restored as flags flip).
- **Error-log hint voice (diegetic, escalating):** t1 `INTEGRITY: entry [keeper] unresolved — cross-
  reference incomplete`; t3 spells the nudge nearly plain. Same `hints` table as source (T6), two surfaces.
- The **Discord artifact-leak (D1)** is the *same system bleeding into their comms* (a grounded echo — T5).
- Security (risk register): RLS / edge-function read path ONLY; never the service key in the browser.

### L5. Relief split — DONE (spec)
Warm-Grief relief flows from **two sources**: (a) **Wren** — warmth that **curdles** at the reveal
(retroactively poisons every kind moment; powerful); (b) **untainted keeper memories** that survive the
reckoning (Mara's kinder margin note; a Sella copybook drawing that is only joy; a hearth that actually
warms, once). Post-reckoning, the keeper-memory relief is the honest warmth that remains. Schedule relief
**after** climax beats (pacing exhale, A6). Never route all warmth through Wren.

### L6. Seventh / "kept" repair — SPEC (execute in integration pass, dovetailed with L1)
Apply BUILD-PLAN §6 pending items — the **third-meaning "kept"** (rescued/recorded-true) via the Seventh's
own line, the **reunion first line**, the **enrollment re-valence** (`keeperPage*` → *recognized*, not
*consumed*) — now unified with L1 (End exile→return) and L2 (Wren's kept-in-part). **"Kept" now holds FOUR
coherent meanings, each anchored to ONE character/moment so they can't muddle:** absorbed/horror (the
Kept) · the-light-keeps (Mara) · recorded-true (the Seventh's line) · kept-in-part (Wren). The Seventh's
reunion line must *charge* the third; Wren *embodies* the fourth. Files: `the-seventh-below.md`, `voice.ts`.

**Lore/story pass COMPLETE. Next: the integration pass (pass 4)** — wire every L1–L6 + §1–§4 touchpoint
into the actual files (docs / seeds / voice / plugin / sites / the 4 canon docs), reconcile every stale
line, and keep specscheck/seedcheck/gatecheck/typecheck GREEN. Establish a green baseline FIRST.

---

# WAVE R — PERPLEXITY RESEARCH INTEGRATION (2026-07-01)

Ethan supplied a Perplexity research/directive doc (ARG-design craft + "keep your head straight" audit
discipline + a client/server visual-tool brief). Triaged, then executed under his ruling: **full-send —
build/change/audit/fix, major changes authorized; a curated modpack for the vetted group is acceptable.**
Ran the established pipeline (triage → plan → modify → verify). Started from a CONFIRMED-GREEN baseline.

## R0. Triage verdict (the honest read)
- ~90% of the research **ratifies principles we already lock** (layer separation, data-driven state,
  recovery/fallback, no-orphan lockstep, silence-is-information, difficulty-from-depth). Folded as a
  *lens*, not new work.
- The **audit-asks were already satisfied** by `IMPROVEMENT-AUDIT.md` (41 grounded findings). Executed
  THAT backlog rather than re-auditing.
- Three genuine deltas: (1) modpack now allowed → **Simple Voice Chat** green-lit (the one client mod
  worth the consent budget — buys a new *sense*, not cosmetics); (2) verified server-side visual
  upgrades (**display entities + 1.21.4 `item_model` + ModelEngine**) — zero-client-mod, all degrade to
  vanilla; (3) **Iris+Photon shaders = recorder's client ONLY** for the YouTube capture.
- **Client-mod ruling:** Figura is version-blocked (no 1.21.11) and cosmetic-only-to-same-mod-users;
  CPM has a 1.21.11 build but same limitation. Spend the single modpack "consent budget" on Voice Chat's
  new capability, approximate avatars server-side (ModelEngine + display entities). Full tool report in
  session transcript (2026-07-01).

## R1. Wave-1 changes SHIPPED (all green after each change; smallest-safe diffs)
| Area | Change | File(s) |
|---|---|---|
| Plugin | Registered `ModeledMobBeat` (the only truly-missing beat) | `beats/BeatLibrary.java:92` |
| Plugin | `EventLogRow` reshaped to live `{level,source,message,created_at}`; `type`→CHECK-legal `info\|warn\|error`, uuid/detail folded into message (fixes every plugin log 400ing) | `data/rows/EventLogRow.java` |
| Plugin | `bases` upsert re-keyed `id`→`owner_uuid`; null id omitted so bigserial PK assigns (fixes base-detection 400) | `SupabaseClient.upsertBase`, `BaseDetector.java:107` |
| Plugin | `SettingsRow.value` String→`JsonElement` (dashboard watcher-sleep toggle no longer inert) | `data/rows/SettingsRow.java` |
| Plugin | `fetchArcState` → `id=eq.1` | `SupabaseClient.java` |
| Plugin | Ignition proximity trigger now requires **sneak** (incidental clicks can't ignite) | `IgnitionListener.java:120` |
| DB | `bases_owner_uuid_key` unique index (the plugin's upsert contract) | `discord/supabase/schema-repair.sql:66` |
| DB | **Seed-order ENFORCED** — new `npm run db:seed` → `build-apply-all.ts` → `apply-all.sql` (0006+0007 before seeds); guards flipped `raise notice`→**`raise exception`** (mis-order now aborts loud instead of silently leaking the 4 M4 docket answers). Caught a 2nd latent hazard: `apply-tonight.sql` omitted 0007. | `discord/package.json`, `src/db/build-apply-all.ts`, `seeds/{metapuzzle,progression}_seed.sql`, `apply-tonight.sql` |
| DB | `v_record` view authored — coarse spoiler-safe `{movement, stones_read, accepted}`, SECURITY DEFINER, anon-read only (un-wires the frozen public archive) | `dashboard/supabase/migrations/0004_v_record.sql` |
| Pack | `pack.mcmeta` → unified `min_format/max_format [75,0]` (1.21.11); 3 conflicting doc-truths reconciled | `resourcepack/pack.mcmeta`, README, `design/WORLD-BUILD.md` |
| Audio | **4 atmospheric OGGs synthesized** (cold_toll bell, drone_low bed, stone_breath rumble, whisper) — atmosphere no longer mute | `resourcepack/assets/observance/sounds/*.ogg` |
| Datapack | New `datapack/README.md` — Undercroft marked BUILT-but-DEFERRED (real noise-cavern, interior unfurnished) | `datapack/README.md` |

## R2. CRITICAL CORRECTION — the audit overstates brokenness
Verified against live code (director re-checks caught these): several audit P0s had **already drifted
fixed** and were NO-OPs — 4 of 5 "unregistered" beats were registered; keeper dispatcher already wired at
3 call sites; `pack_format` already 75; Undercroft already a valid noise-cavern. **Most important:
audit P0-C1 ("companion/reckoning/co-op = dead gates") is FLATLY WRONG** — `WrenNpcListener` (reg L403)
sets `companion_trust/companion_revealed/reckoning_{condemn,understand,free}`, `CompanionArcWatcher`
(L152) sets `companion_revealed`, `SeventhChoiceListener` (reg L542) merges finale flags, the co-op vault
runs via `ThresholdVaultListener` — all registered and writing via `mergeArcFlags`. **Wren's betrayal arc
is ALIVE.** ⟹ Do NOT plan future waves off the audit's content findings; use code truth.

## R3. Preserved deliberately (per research "treat as intentional")
Flag-flow atomicity (`observance_merge_arc_flags`), RLS deny-by-default + 0003 lockdown, dashboard
service-key server-only, idempotent seeds, the advancement icon schema, the Undercroft generator, the
existing beat/listener architecture. No refactors, no renames.

## R4. NEXT — Wave 2 (cohere) + Wave 3 (amazing), grounded in code truth not the audit
- **Wave 2:** doc reconciliation (P1-I1/I2, SESSION-ZERO over-disclosure), live answer-collision
  `the last marker is not the last` (P1-C3), dashboard read-drift (P1-D5), seedcheck hardening (P1-C9).
- **Wave 3 opening move (revised):** NOT "build missing producers" — instead a **flag-name parity audit**:
  do the exact strings the Java producers write byte-match the SQL seed gates? A 1-char drift = a silently
  dead branch. THEN: per-player illusion primitives (P1-A6), display-entity/ModelEngine world craft
  (P1-V2), salience+hints (P1-C4/C10), and the Voice Chat "Ear". Playtest is the real gate.

---

# AUTONOMOUS SESSION LOG (2026-07-01, Ethan away ~30min — self-directed)

Four waves, each verified GREEN independently by the director and committed separately.
Branch `feat/build-everything-2026-07-01`. Nothing pushed.

- **Wave 1 `6fc64e1`** — P0s: DB-write 400s (event_log/bases/settings matched to live schema),
  seed-order ENFORCED (`npm run db:seed` + fail-loud guards), `bases.owner_uuid` idx, `v_record`
  view, 4 synthesized OGGs, pack_format reconciled, Undercroft deferred, ignition sneak-gate.
- **Wave 2 `d036eca`** — wired the reckoning + finale-fork payoff (companion.ts/.run.ts,
  finale.ts/.run.ts, voice.ts, reports.ts). The already-written lore now has live consumers;
  reckoning_* + seventh_choice are player-facing. +28 self-tests.
- **Wave 3 `8a7d101`** — stone-brann-cipher hint (only genuinely-bare back-half node); P1-C3
  collision FIXED via a pure requires_flags gate (no answer/lore change).
- **Wave 4 `e298577`** — author-dashboard read-drift: two SECURITY-DEFINER reconciling views
  (v_dossiers/v_custom_compliance) join plugin mc_uuid rows to players, synthesize player_id,
  coerce drifted columns. anon-revoked. Dossiers/customs no longer render blank.

## RECURRING FINDING (carry forward): the IMPROVEMENT-AUDIT overstates brokenness
Every wave, most "broken" P0/P1 findings were ALREADY fixed in live code (audit drifted). Confirmed
false/stale: P0-A1 (4/5 beats registered), P0-V1 (dispatcher wired), P0-C1 (companion arc ALIVE),
P0-R1 (pack_format 75), P1-R3 (Undercroft is a real cavern), P1-C10 (6/7 back-half nodes had hints).
Flag-parity audit: ZERO dead gates (all 14 gated flags produced). **Trust code, not the audit.**

## OPEN — need Ethan's ruling / action (NOT done autonomously)
1. **APPLY SQL to live Supabase (operator, by hand — MCP can't reach it):** the new migrations
   `0004_v_record.sql`, `0005_reconcile_tracker_views.sql`, the `bases_owner_uuid_key` in
   `schema-repair.sql`, and the seed order via `npm run db:seed` → `apply-all.sql`. The code fixes
   ASSUME these are applied. Nothing works on the live DB until they are.
2. **Tier-3 hint philosophy (design ruling):** tier-3 "rescue floor" hints systemically hand over the
   exact typeable answer (27 rows, by documented design). For typed-CIPHER nodes (bound-word,
   true-walk-arrive) this nullifies the decode. `stone-iss-wall` t3 shows the better pattern (give the
   KEY/method, not the plaintext). Left unchanged — your call whether to align cipher-node t3s.
3. **Deferred with recommendations (design-laden, want you present):** world-craft via display
   entities/ModelEngine (P1-V2, the biggest "amazing" lever), salience/roster rewrite of decide.ts
   (P1-C4 dead-air after Mvt II), per-player illusion primitives (P1-A6), SESSION-ZERO consent-script
   alignment (P1-I2 — over-discloses dormant voice T1), and the modpack-enabled Voice Chat "Ear".

---

# WAVE S — THE RESHAPE (2026-07-02) — from cipher-chain to a mysterious world

> **This is the add/change/cut pass for the deep reshape** (mandate in `NEXT-SESSION.md`; plan in
> `THE-RESHAPE.md`; evidence in `RESHAPE-RESEARCH.md`). Produced from a full live-code audit of all
> five surfaces (five parallel Explore passes, each verdicting every layer against the direction).
> Nothing here is built yet — this is the pass Ethan reviews before any build. Cross-checked against
> `LAYER-LEDGER.md` (§S6 below); every touched layer marked **keep / migrate / rebuild / cut**.

## S0. THE DIRECTOR'S READ (the honest finding — smaller than the docs feared)

**The world is already built rich and diverse. What makes it "play like a puzzle GAME" is a THIN
layer of ANNOUNCEMENT plus ONE structural spine — not the content.** The meta-lesson held again:
`THE-RESHAPE.md`'s diagnosis was written before the diversity work shipped, and overstates how much
must be rebuilt. Live-code truth per surface:

- **Puzzles (grade A already):** ~70 rows spanning observation / behavior / object / code / coords /
  spoken / lore. Letter-ciphers are **already a minority** (8 active/staged of ~70). The "cure
  cipher-monotony" work of `PUZZLES.md` **shipped.** The reshape here is *subtraction + one web fix*,
  not "add variety."
- **Structures:** all 5 ambient mutation beats (decay/room-swap/doors/torch-gutter) are already
  silent-world-mutation — pure KEEP. The 13 static set-pieces already carry the mystery in their
  BLOCKS (significant absence, contradiction, architecture-as-grammar). Their ONE shared flaw is a
  **WAXED label sign that names the keeper and explains the mechanic** ("stoop to read", "count the
  black moons — do not sleep", and worst, Iss's **"the fire is kept (it lies)"** — which spoils the
  entire trap). The reshape is **strip/rewrite the didactic labels**, not rebuild geometry.
- **Integrations:** flag graph, oracle, normalizer are byte-identical across both surfaces — the
  reliability floor, KEEP untouched. Per-player illusion primitives are live and wired. The ONE real
  seam: the showrunner **drips a linear rank-ordered sequence, not the salient web Pillar 2 promised.**
- **Web:** the record is well-built, secure, cross-surface-consistent — but redaction is **binary
  (legible↔struck) and linear (stone 1→2→3)**, with no model for corrected/implied/absent facts.
- **Lore:** the prose is the gold; motifs (SEVEN / FIRE / SILENCE / KEEPING / PLACE) are already
  threaded; fact-layering is **shown-via-register by design** (canon §6.2 "never label it"). The
  corpus **needs finishing, not rewriting.**

**So the reshape is five surgical moves, in lockstep — not a teardown:**
1. **P0 — kill the up-front alphabet tutorial** (the single highest-leverage change; touches puzzles
   + structures + lore-cribs together).
2. **Strip the didactic structure labels** (structures).
3. **Subtract redundant ciphers + deepen the front/back into a ≥3-clue web** (puzzles).
4. **Reflect fact-layering + decay on the record** (web) — *mirror* the register-layering the prose
   already has; do NOT add didactic "this is misleading" labels.
5. **Salience-pick the drip** (integrations) — the web-not-chain fix on the showrunner side.

Plus the lockstep callbacks (lore) and the "reward the theory not the lookup" batch-confirm (the one
genuinely-new mechanic — flagged as a BIG CALL in §S7, not assumed).

---

## S1. STRUCTURES — add/change/cut

**Verdict pattern:** KEEP all geometry + all ambient beats; MIGRATE = strip/rewrite the WAXED label
(remove keeper-name, remove instruction, remove spoiler); the blocks already do the work.

| Set-piece | File:line | Verdict | The change |
|---|---|---|---|
| Rosetta | `StructureTemplates.java:159` | **MIGRATE (P0)** | Remove Roman-numeral marks (i–vi) from the way-mark signs; strip the "the rosetta — read, then answer" label. Keep dais/amethyst/ring/architecture. It becomes a *place where literacy happens*, not a labeled lesson. |
| Vaun | `:210` | **MIGRATE** | Cut the "vaun's ledger, all of it kept, none of it spent" label. The oxidized-copper decay + crammed hoard + cracked pot + cobwebs already say it. |
| Mara | `:261` | **MIGRATE** | Cut the "in the margin: 'read it back to me'" label. The empty shelf-gaps + abandoned 2nd lectern + dust are the story. |
| Sella | `:315` | **KEEP** | Label ("read what the water keeps still") is poetic process-language, not instruction. Reflecting pool is diegetic. Model to copy. |
| Orin | `:379` | **MIGRATE** | Cut "the low stone asks a bow — stoop to read." The low lintel *is* the instruction. Reduce label to one word or nothing. |
| Brann | `:439` | **MIGRATE** | Cut "count the black moons — do not sleep." Tally-marks + never-dying campfire speak. |
| Iss | `:497` | **REBUILD-label** | **Remove the "the fire is kept (it lies)" label entirely** — it spoils the trap. Keep the magma-behind-glass / soul-fire / creeping soul-soil contradiction; let the cold be *felt*. |
| Stone of Reckoning | `:560` | **MIGRATE** | Cut "count the marks, then the way — north, down, read." The 6 studs + 4 arms are a visual system to be read, not narrated. |
| Cold Hearth | `:616` | **KEEP** | Label + book are diegetic truth-of-the-place, poetic not instructional. (Optional: trim to one phrase.) |
| Unbroken Light | `:682` | **MIGRATE** | Cut "bow as one — all who are here." The fire + ring-of-lanterns + one dark 7th place invoke the rite; the listener watches the crouch. |
| The Threshold | `:743` | **MIGRATE** | Trim "the date is not yet come…" to a word/glyph. Capstone-ajar + lantern-glow tell it. |
| The Unwriting | `:805` | **KEEP** | "the seal is a name" is cryptic poetry; scrape-marks + undegraded hand + empty slot carry it. Load-bearing chamber. |
| Threshold Vault | `:877` | **MIGRATE** | Trim "each holds one rune — read them as one" → "threshold." The listener shows each player their fragment; the sign shouldn't teach the mechanic. |
| Ambient beats ×5 (DecayCreep / RoomSwap / DoorOpen / TorchGutter / SmallStructure) | `beats/lib/*` | **KEEP** | Silent world-mutation — exactly the direction. Untouched. |

**New (P0 support):** author **rune-cribs** into the keeper set-pieces — a rune-word carved *next to
the thing it names* (a mark over the hearth = the word "fire"; over the pool = "water"; over the
grave = "kept"). This is the Chants-of-Sennaar / Fez-pangram move that makes literacy *earnable* once
the rosetta stops teaching it. (Lore-authored in S5; block-placed here.)

**Touchpoints:** `StructureTemplates.java` (labels + cribs); `voice.ts` / lore (crib words must be
real decodable rune-words, S5); PUZZLES (the rosetta migrate pairs with S3's literacy change); no DB.

---

## S2. LORE — add/change/cut (finish, do not rewrite)

**Verdict pattern:** KEEP all six keeper journals + Archivist register + Keeper summons + the motif web
(inviolable — the gold). The work is FINISHING design-complete pieces + P0 cribs + lockstep callbacks.

| Layer | Verdict | The change |
|---|---|---|
| Six keeper journals (Vaun/Mara/Sella/Orin/Brann/Iss) | **KEEP** | Do not edit. Voices inviolable, fates earned, degradation complete. |
| Archivist / official records (R01–R16) + margins | **KEEP** | Register perfect; margins do the "warmth under dread." |
| Keeper summons (`bring-the-thing-only-you-can-give`) | **KEEP** | Load-bearing; does the mechanical explaining the game needs. |
| Motifs SEVEN / FIRE / SILENCE / KEEPING / PLACE | **KEEP** | Already threaded + paid off. The reshape's "recurring motif" (Principle 3) is **already present** — this is a relief. |
| Fact-layering *signals* (confirmed/implied/misleading/false) | **KEEP-as-is** | Canon §6.2 shows-not-labels (warm=Iss lie / cold=record / broken=Brann). The WEB reflects this structurally (S4); do NOT add didactic labels to the prose. |
| Rune-cribs (P0) | **ADD** | Author the real rune-words that sit beside hearth/water/grave/etc. so literacy is earned by cross-reference (S1 places them). The one genuinely-new authoring the reshape *requires*. |
| `the-seventh-below.md` | **REBUILD (author)** | Design-complete spec → diegetic first-person journal. Pays off the SEVEN motif + Iss's lie + the reunion. Serves the direction (the theory the player builds). |
| Optional subtle fact-layer plants (Vaun "it is counting me"; Mara "silence is keeping too"; Orin "watched before warned") | **ADD (optional)** | Three margin lines that deepen without blurting. Only if they don't slow the prose. |
| Wren dialogue trees + "kept close" artifact prose | **MIGRATE (finish)** | Draft lines → full `voice.ts` branches + lectern artifact. *Finishing*, not reshape — flag scope in §S7. |
| Living-keeper enrollment lines (`keeperPage*`) | **MIGRATE (finish)** | Design exemplars → full per-keeper prose. Finishing. |
| Every reshape mechanic gets its callback (lockstep) | **ADD** | Each S1–S5 change that alters a mechanic gets a one-line story callback in the fiction/record (consistency principle). Most already have one. |

**Touchpoints:** `arc/lore/documents/*`, `arc/corpus/*`, `voice.ts`, the record projection (S4).

---

## S3. CIPHERS → PUZZLES — add/change/cut (subtract + web)

**Verdict pattern:** the *system* is grade-A and diverse; reshape = remove up-front teaching, cut
redundant ciphers, deepen into a ≥3-clue web. Active letter-ciphers **8 → 4**, all earned.

| Puzzle | Verdict | The change |
|---|---|---|
| `rosetta-ring` | **MIGRATE (P0)** | Stop teaching the alphabet by typing its decode. Convert to *observation* — literacy accretes from scattered cribs (S1/S5); `rosetta_known` becomes *earned by noticing*, not a typed tutorial gate. |
| `a1z26-tick-stave` | **MIGRATE (P0)** | Redundant second door to `rosetta_known`. Convert to observation (watch the count play), or fold into the accretion model. |
| `reckoning-rosetta` (2nd literacy, digit-glyphs) | **MIGRATE** | Same treatment — earn digit-literacy by observation, or push to the deep/optional layer. |
| `stone-vaun` (Caesar) | **KEEP (as honest lore)** | *(Revised 2026-07-02 per Ethan: pure-lore ciphers are GOOD — gathering backstory you don't yet know is relevant is immersion + discovery, welcome if well done. The sin is only inert content costuming as a load-bearing puzzle; a `lore`-outcome cipher with no door is honest flavor, not that.)* A decodable Vaun carving that pays *backstory*, off-spine, no gate. Well-space it from the other Vaun beats; keep its framing honestly flavor (never pretend it's a required gate). |
| `stone-sella` (Atbash, phrase+coords) | **REBUILD** | Split/convert: the *reflection* is the read (observation), the *bearing* is the walk. Stop teaching atbash as a lookup. |
| `stone-brann` (phrase) | **MIGRATE** | → observation (beacon-count at night). The real cipher is `stone-brann-cipher` (rail-fence, staged). |
| `iss-warm` | **MIGRATE** | → clean `dead_end` (a true-feeling phrase that goes nowhere); drop the unnecessary `iss_trusted` routing. |
| `base-docket-reread` | **CUT** | Superseded by `base-docket-reread-auto` (deterministic twin). Retire. |
| Kept letter-ciphers: `stone-mara` (book) · `stone-orin` (substitution, behavior-earned) · `stone-iss-wall`+`bound-word` (Vigenère) · rail-fence ×2 (staged) | **KEEP** | All earned via behavior/observation, well-spaced. The load-bearing minority. |
| ~55 non-cipher rows (observation/behavior/object/code/coords/spoken/lore + 20 breadth quests) | **KEEP** | The diversity that already shipped. Untouched except where a label/structure changes (S1). |
| **The web (front + back)** | **REBUILD (structure)** | Front-half is a **single-point gate** (`rosetta_known` → all six stones); back-half a **tight chain** (Vigenère→substitution→co-op). Add the **≥3-clue-per-conclusion web**: redundant clues to each keeper-fate, one **cross-keeper motif combination** (a clue at keeper A + a clue at keeper B → a third thing), and one **cross-surface lock** (in-world mark + `#the-record` fragment cash out only combined). |

### S3 PIVOT (2026-07-02, post-converge) — the cipher migrations are SUPERSEDED; keep the ciphers

The converge (R2's **cipher-as-inversion** motif) + Ethan's **"pure-lore ciphers are good"** call
change S3's answer. Converting the ciphers to observation would now *orphan* the R2 lore that made
each cipher meaningful. So the three MIGRATE/REBUILD rows above are **superseded → KEEP-as-characterization:**
- **`stone-sella` (atbash)** — KEEP. R2 recast the atbash AS Sella's drowning-motif (the far water gives
  everything back mirror-wrong; `a-a-a-the-copybook` explains it). The reflection *is* the atbash; with
  cribs the reversed runes are now readable. Only ensure hints frame it as reading-the-reflection (R2's
  copybook already does). Do NOT strip the atbash.
- **`stone-brann` (rail-fence, staged)** — KEEP. R2 recast it as "reads only in the dark" (his traded
  dark-hours). Already a lore/observation node; the cipher is now characterization. No conversion.
- **`iss-warm`** — KEEP. Verified already a fair dead-end-with-teeth (the warm misreading routes to the
  cold grave that pays nothing — Principle 5). `iss_trusted` is set-but-required-by-nothing (harmless).
- **`stone-vaun` (Caesar)** — KEEP as honest lore (S3 revised entry above).
**What genuinely survives S-C:** (1) the **cross-keeper ≥3-clue web** — *already delivered* by R2's
seven-motif (Vaun vii / Brann 7-shrines / Sella copybook = ≥3 independent sites); (2) the **cross-surface
lock** (in-world mark + `#the-record` fragment, combined) — the one net-new S-C item → folded into **S-E**.
Net: S-C is essentially DONE by the converge; the reshape's "fewer letter-ciphers" goal is met by
**ciphers-that-mean-something**, a better answer than deletion.

**Touchpoints:** `puzzles_seed.sql` / `metapuzzle_seed.sql` / `progression_seed.sql` (rows + flags);
`OracleResolver.java` + `resolve.ts` (unchanged — surface-agnostic gate); `hints_seed.sql` (hints
teach the cipher *family*, not the plaintext — align cipher t3s per the OPEN #2 above); the record
web (S4) for the cross-surface lock. Re-run seedcheck/specscheck/gatecheck.

---

## S4. WEB (the record) — add/change/cut

**Verdict pattern:** KEEP the security model + cross-surface consistency; extend the projection to
*reflect* fact-layering and decay (mirror the prose's existing register-layering — no didactic labels).

| Route / layer | File | Verdict | The change |
|---|---|---|---|
| `/record/[slug]` (the-record) projection | `record-projection.ts` | **REBUILD (extend)** | Multi-state legibility: `legible / corrected / struck / silent-absence` (not just legible↔struck). Add the M4 **correction variant** (the record was told X, later writes Y). Model **silence** (a fact kept so well it is *absent*, not struck). Corruption/decay skin. |
| `/record/terminal` (ledger + threads + integrity) | `terminal/page.tsx` + `ledger.ts` + `integrity.ts` | **MIGRATE** | Keep structure; add **layered ledger history** (not just current count), **web-branching threads** (a clue → multiple nodes), recovered-note decay aesthetic. |
| `/record/the-record-keeps` (lure) | same route | **REBUILT** | Preserved user-file mirror; crude lighttpd index, no ceremonial ARG skin. |
| `layout.tsx` nav bleed | `record/layout.tsx` / `app/layout.tsx` | **FIX (bug)** | Public record still renders the admin Status/Author nav. Conditional on pathname so the archive reads as a standalone off-world artifact. |
| `/status`, `/author` | — | **KEEP** | Infrastructure / director console; not narrative surface. |
| `/` home | `page.tsx` | **MIGRATE (minor)** | Admin hub — keep, but ensure it never appears on the public archive side. |
| `voice.ts` record strings | `voice.ts` | **KEEP + EXTEND** | Add correction lines ("the record had written X; it now writes Y") + implied/pointing lines. Register locked. |
| Cross-surface lock (new, pairs with S3) | `record-projection` + seed | **ADD** | One record fragment that only resolves combined with an in-world mark (I Love Bees model). |

**Touchpoints:** `record-projection.ts` (+ its selftest), `ledger.ts`/`integrity.ts`, `layout.tsx`,
`voice.ts`, the seed row for the cross-surface lock. Keep RLS/edge-function read path only.

---

## S5. INTEGRATIONS — add/change/cut

**Verdict pattern:** KEEP the reliability floor (flag graph / oracle / normalizer / autonomy
producers); the reshape touches exactly the salience picker (+ optional illusion registry).

| Piece | File:line | Verdict | The change |
|---|---|---|---|
| Flag graph (both surfaces) | `gate.ts:23` / `OracleResolver.java:221` | **KEEP** | Byte-identical twins, fail-closed. Do not touch. |
| Oracle normalizer (TS+Java) | `normalize.ts` / `AnswerNormalizer.java` | **KEEP** | Agree today. (Nice-to-have: a CI parity check.) |
| Discord commands (`/whisper /link /answer`) + `#the-record` scan | `bot/*` | **KEEP** | Clean, rate-limited, one resolver path. |
| Showrunner spine (snapshot→decide→apply) | `showrunner/*` | **KEEP** | Deterministic, testable, fault-isolated. |
| **Drip picker** | `decide.ts:113–121` | **REBUILD** | Today: "first un-dripped by story-shape rank → forgeable → movement → key" = a **linear queue.** Reshape: **salience-weighted + roster-aware** pick (recency × player-fingerprint − recent-same-tone; never surface a convergence thread below effective quorum). This is the integration face of web-not-chain + Pillar 2. |
| Per-player illusion primitives | `PerPlayer.java` / beats | **KEEP; MIGRATE (optional)** | Live + wired. Optional: a central "what is player X seeing" registry so multiple "it knows ME" beats compose without fighting. Not load-bearing. |
| Autonomy producers (prologue/reckoning/reports/customs/companion/…) | `showrunner/*.run.ts` | **KEEP** | Grounded, idempotent, degrade gracefully. |
| Observer Engine (voice-scan / "it hears me") | spec only | **DEFER** | Matches LAYER-LEDGER forgotten #1/#2 (LLM brain + `0008_observations`) — SCOPE DECISION, not reshape scope. |

**Touchpoints:** `decide.ts` (+ selftest), `snapshot.ts` (feed salience inputs), reckoning/roster
reads; no schema change for salience (reads existing signals). Illusion registry = plugin-only.

---

## S6. LAYER-LEDGER CROSS-CHECK (nothing silently dropped)

Reconciled against `LAYER-LEDGER.md`. The reshape does **not** revive the ledger's "forgotten/unwired"
items as reshape scope — it confirms their status and stays in its lane:

- **Ledger "DONE + PROVEN" floor** (requires_flags gate · deterministic decide · clue-card render ·
  migrations 0006/0007 · seeds/flag-graph/hints · session-zero) — **all KEEP.** The reshape edits
  content *on top of* this floor; the floor is untouched (S5 keeps the gate/oracle; S3 only edits rows).
- **Ledger "WIRED BUT UNPROVEN" (the playtest list)** — the reshape's structure-label + puzzle-web +
  salience changes **feed the same playtest**; they don't add unproven surface, they *reshape existing*.
- **Ledger forgotten #1 (LLM brain) + #2 (`0008_observations`)** — **still DEFERRED scope decisions**
  (S5 Observer row). The reshape neither builds nor deletes them; it removes any dependence of the
  reshaped spine on them (the salience picker uses deterministic signals only).
- **Ledger forgotten #3 (keeper voices)** — DONE (source pack); unaffected (carry-over: resourcepack
  re-host).
- **Ledger "built not wired" (ThresholdVault roster supplier `null`)** — folds into **S5 roster-aware
  salience** (the same active-roster read the drip picker now needs). Marked: wire it as part of S5.
- **No planned layer is cut without a reason** — the only CUTs are `stone-vaun` and
  `base-docket-reread` (both redundant twins, replacement named in S3). Everything else keep/migrate/
  rebuild.

---

## S7. THE BIG CALLS FOR ETHAN (decide before build)

Four real decisions (everything else has an obvious default and is queued):

1. **P0 literacy model — how far.** (a) **Full Tunic/Fez** (recommended): never teach the alphabet;
   scatter rune-cribs; `rosetta_known` earned by the "these are letters" epiphany an hour+ in —
   highest payoff, biggest new build (cribs + reflow the opening). (b) **Soften:** keep the rosetta
   as an optional *place* (strip labels), add cribs so literacy is earnable elsewhere too. (c)
   **Reframe-only:** keep the gate, strip the announcement so it stops *feeling* like a tutorial.
2. **Record fact-layering.** Confirm **"reflect, don't annotate"** — the web mirrors the prose's
   existing register-layering (corrected/struck/silent states) but adds **no** didactic
   "misleading/false" labels (honors canon §6.2). (Recommended.)
3. **Scope boundary: reshape vs finishing.** Author the design-complete pieces that *serve the
   direction* now (rune-cribs, `the-seventh-below` journal), and **defer pure-finishing** (Wren
   dialogue trees, enrollment lines, staged rail-fence activation) unless cheap? (Recommended.)
4. **"Reward the theory, not the lookup" (Obra-Dinn batch-confirm).** The one genuinely-new mechanic:
   the record "receives" a keeper-fate only when a *cluster* is coherent (build-a-theory), vs the
   current per-cipher solve. Deep change to how "solving" feels; touches oracle + record + showrunner.
   **In-scope for this reshape, or a follow-on wave?**

## S8. DECISIONS LOCKED (2026-07-02, with Ethan) — the reshape is full-send

Ethan chose the most ambitious option on every call + reaffirmed the north star (*cohesive · difficult-
but-fair · lore/story-heavy · a **web not a chain** · deeply MC-native · weeks-long even with 6+ ·
spooky/unsettling/emotional · production-quality · not broken · lots of variety + NPC interaction*) and
granted latitude ("you are not a slave to the guide docs — flag anything outdated/forgotten/improvable").

| # | Call | DECISION |
|---|---|---|
| S7-1 | P0 literacy depth | **FULL EARNED LITERACY.** Kill the alphabet tutorial; never teach it. Scatter rune-cribs (a rune-word beside the thing it names). `rosetta_known` becomes *earned by the "these are letters" epiphany*, not a typed gate. Guardrail: every glyph decodable by cross-reference + hint rail as safety valve (difficult, never punishing / retrace-fair). |
| S7-4 | Reward-the-theory (Obra-Dinn batch-confirm) | **BUILD IN THIS RESHAPE.** The record "receives" a keeper's fate only when a *cluster* is coherent; players build+lock a theory per keeper, not type one decode. Touches oracle + record + showrunner. |
| S7-3 | NPC interaction depth | **FULL NPC LANE.** Place + wire the surface townsfolk (Aro/Wenna/Dob/Pell) with their designed dialogue trees; give the six keepers interactive presence; finish Wren's full tree. Distinct build lane (biggest content add — directly serves story-heavy + NPC-rich). |
| S7-2 | Record fact-layering | **REFLECT, DON'T ANNOTATE** (my default): mirror the prose's register-layering (corrected/struck/silent states + decay skin); no didactic "false/misleading" labels (honors canon §6.2). |

**Beyond-docs improvements greenlit into scope (Ethan invited these):**
- **NPC lane** (S7-3) — was thin vs the goal; now a first-class lane.
- **Cross-surface literacy payoff** — some record entries themselves rune-locked, legible only as
  literacy is earned in-world (P0 pays off on two surfaces). Fold into S4.
- **Fairness rail** — every earned-literacy / theory leap must pass the retrace test; the
  integrity/hint log is the always-available safety valve (difficult ≠ punishing).

**BUILD ORDER (green waves, pipeline-disciplined; baseline verified GREEN 2026-07-02 — plugin jar ·
datapack JSON · tsc · seedcheck/gatecheck/specscheck/showrunner×4 all pass):**
- **Wave S-A — P0 literacy** (plugin structure labels+cribs · rosetta/a1z26/reckoning-rosetta seed →
  observation · lore crib-words · record rune-lock). The feel-changer; do first.
- **Wave S-B — structure de-announcement** (strip the 10 didactic labels; keep Sella/ColdHearth/Unwriting).
- **Wave S-C — puzzle subtraction + web** (CUT stone-vaun + base-docket twin; REBUILD stone-sella;
  MIGRATE stone-brann/iss-warm; add ≥3-clue redundancy + cross-keeper motif combo + cross-surface lock).
- **Wave S-D — reward-the-theory batch-confirm** (oracle cluster-receive · record theory-lock · showrunner).
- **Wave S-E — record layering + decay + nav fix + voice correction lines.**
- **Wave S-F — salience drip** (decide.ts → salience-weighted + roster-aware; wire ThresholdVault roster).
- **Wave S-G — full NPC lane** (townsfolk place+wire+author · keeper presence · Wren tree finish).
- **Wave S-H — lore finish** (the-seventh-below journal · enrollment lines · lockstep callbacks · optional plants).
Each wave: cohesion + lore-callback + integration wired together (consistency principle), green after each,
commit at clean wave boundaries. `.env*` stays gitignored.

## S9. PROGRESS + CLARIFIED SCOPE (2026-07-02, verified against live code)

Building the waves surfaced the meta-lesson at full strength — **most of the reshape was ALREADY DONE by
prior waves + the R2 converge.** Verified done against live code:
- **S-A (earned literacy) — DONE + committed `6f026a4`.** runeCrib helper + 10 cribs + detuned teaching + founders' note.
- **S-B (label cull) — DONE** (adopted R0; plugin 0.3.0). Verified: the `RESHAPE R0` comments cut the labels.
- **S-C (puzzle web) — DONE/PIVOTED + committed `cf782c9`.** Ciphers KEPT-as-characterization (R2 cipher-as-
  inversion); ≥3-clue web delivered by R2 seven-motif; only the cross-surface lock survives → S-E.
- **S-E (nav-bleed) — partial DONE `cf782c9`** (SiteChrome; record full-bleed). Remainder: multi-state record
  redaction + decay skin + the cross-surface lock (fold with S-D record work).
- **S-H (lore finish) — DONE (verified).** the-seventh-below.md authored; voice.ts payoffs (graveOpened /
  keeperCloseSeventhRestored / all 6 enrollment keys re-valenced) already wired; Iss-seam wired
  (`voice.ts:310` + thread_cards edge); "you are the next" in six-were-kept-before-you.md:49. The 3 old
  Seventh docs left as honest older-era texture (R2's fact-layer reconciled them; retiring would orphan the
  intentional `the-seventh-was-spared` herring). Optional subtle plants: R2 delivered them.

**GENUINELY REMAINING (the only real new builds):**
- **S-F — salience drip.** IN PROGRESS (agent): decide.ts linear→salience-weighted + roster-aware, tests kept green.
- **S-D — reward-the-theory batch-confirm.** UNBUILT (verified: no theory/cluster mechanic exists). Design below.
- **S-G — full NPC lane.** PARTIAL: townsfolk (Aro/Wenna/Coll/Dob/Old-Pell) have authored dialogue in
  `voice.archive.ts` (SET A) + gather-event lines, but are NOT placed/wired as in-world NPCs (only Wren has a
  spawn cmd via Citizens2, `ObservanceCommand.java:526`). Build = extend the Wren spawn/interaction pattern to
  the townsfolk (spawn cmd + click-dialogue driven by the authored SET-A voice) + keeper interactive presence.

### S-D DESIGN (batch-confirm, additive — reuses the flag engine + the autonomy-producer pattern)
The record already "receives" per-stone (record-projection maps solves→legible). Batch-confirm makes it
receive a keeper's **fate** only when a *cluster* of that keeper's evidence is coherent (build-a-theory):
1. **Cluster def (seed):** each keeper's "theory" = its evidence-flag set, e.g. Vaun = {stone-vaun (lore),
   vaun-hoard-sorted (object), vaun-bookshelf-tally (code)}; threshold = author-set (e.g. ≥2 of 3, or all
   "mover" nodes). Author these clusters + thresholds in a new seed block (data, not code).
2. **Theory-lock producer (showrunner, pure/idempotent — the autonomy-producer pattern):** a deterministic
   pass over `arc_state.flags`; when a keeper's cluster meets threshold, set a DERIVED flag `<keeper>_theory`.
   Fault-isolated, no LLM, testable. (Twin the plugin side only if in-world receipt is wanted — else the
   showrunner sets it and both surfaces read it via the shared gate, no plugin change.)
3. **Record consumes theories (dashboard, dovetails S-E):** record-projection reads `<keeper>_theory` (not raw
   stonesRead) to decide which keeper fates are "received" → the record reflects assembled theories, giving
   the Obra-Dinn "commit a coherent set" payoff, not per-cipher un-redact.
4. **Lore callback (voice.ts):** one Watcher line on theory-lock ("the record receives what you have understood
   of [keeper]"). Keep register.
Additive, deterministic, retrace-fair; per-puzzle solves still set flags (the theory is a derived layer).
Touchpoints: new seed (clusters) · showrunner theory-lock producer + selftest · record-projection + selftest ·
voice.ts (+ specscheck) · no schema change (derived flags are jsonb keys). Keep all checks green.
