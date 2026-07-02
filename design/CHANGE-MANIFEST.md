# THE OBSERVANCE — CHANGE MANIFEST (pass 1 of the fold)

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
