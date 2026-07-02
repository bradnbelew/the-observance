# THE OBSERVANCE — THE OVERHAUL (single source of truth)

> Canonical STRATEGY doc (the why + the direction). Part of the canonical set (all 2026-06-29):
> **[BUILD-PLAN.md](BUILD-PLAN.md)** = the operative start→finish build skeleton + status + the
> Seventh/"kept" story repair (open that first when building); **[PUZZLES.md](PUZZLES.md)** = the
> diverse puzzle system (the cipher-monotony fix); **[INTEGRATION.md](INTEGRATION.md)** = the
> Minecraft integration catalog. Where any doc disagrees with this set, this set wins; where any
> disagrees with the **code/seeds**, the code wins (verified state is §2). Old planning docs are
> archived under `design/archive/`. The DIRECTION (§0–§4) was decided with the owner after a full
> step-back — not the original design.

---

## 0. THE CHOSEN DIRECTION — "The Observance v2"

A small veteran friend group is drawn onto a server that is **watching them** — and proves
it by knowing things a server could not know. They discover they are inside the **Deep Hold**,
a buried colony whose people kept "the seven ways" to keep the Dark companied, and who were
hollowed into a watching **record** when the keeper Iss broke faith. But one keeper — **the
Seventh, who kept everything and was cast out for nothing — is still alive**, far down past
the Deep Line, and the group's purpose is to reach them: gather the proof of Iss's lie from
the six keepers' own journals, carry it all the way down, and **correct the record** before it
closes. The reunion is the **future-dated grave that opens from the inside** — the best image
in the project, now the destination instead of a doom. Getting there costs something (to
correct the record you must enter it), so the old induction-twist survives as a *chosen,
meaningful sacrifice* rather than a passive sentence.

Five pillars carry it (each detailed in §3):
1. **Premise reframed** around the living Seventh as the active goal — agency, stakes, a
   bittersweet hard-won end. The dread, difficulty, and depth all stay.
2. **A de-linearized web** of keeper "constellations" the showrunner surfaces by **salience**,
   with emergent reassignable roles and a rotating tone so it never becomes a multi-week downer.
3. **Balanced integration** — *solo* per-player dread **and** *asymmetric-information co-op
   puzzles* (each player is shown a different piece; they must talk to combine them). The
   "it knows ME" isolation becomes the reason to lean on each other.
4. **The world is the surface; the website is "the record."** The Watcher speaks *through*
   the world; the Vercel app becomes an eerie webpage the group discovers in-game, the ledger
   filling with their names, that they write answers into. **No game-persona lives in Discord.**
5. **The Observer Engine** — the Watcher genuinely watches (in-game chat + Discord text +
   Discord **voice** via Whisper) and weaponizes *grounded* observations sparingly and
   precisely. This is the literal engine of "it knows your name," fed by reality.

North star, unchanged: **"From The Fog, but it knows your name"** — autonomous, per-player,
soft-pressure, not scripted.

**Two hard invariants that touch everything (§4):** the **dynamic roster** (player count
changes; people join late — nothing may assume a fixed N or assign a role to a fixed person),
and the **grounding + consent discipline** (the Watcher uses only *real* observed things, never
fabricated; a session-zero disclosure + opt-out ships by default).

---

## 1. THE EXPERIENCE AT ITS BEST

Session one: the friends are pulled onto the server by a cursed map that knows a number it
shouldn't. Nothing is explained. Within minutes the world does something small and wrong that
*one* of them notices and the others don't — and the argument about whether it really happened
is the first scene. They find they are underground with no surface, in a place built by hands
and then abandoned, and a record that already has their names.

Over two-to-three weeks of irregular evenings they pull on whatever thread has momentum —
there is always one, and the world (and the record website) surfaces it without railroading.
They meet the six keepers through their own degrading journals (the gold of the project):
Vaun who hoarded, Mara who read and never walked, Sella the drowned child whose copybook ends
in drawings, Orin who would not bow, Brann who slept on the black moon, and **Iss, the warm
voice who tells them the ways are a wall — and is lying.** They follow his comfort to a cold
dead hearth that pays nothing, and the dead-end itself pushes them to re-test his name as a
key, and it decodes to *the one who turned away.* The kind one lied, and the land kept the
proof. That proof is the testimony they are gathering.

The haunting is *theirs*: a block only one of them can mine yields a clue; the light dims when
they're being watched; a rune in their own name bleeds onto a wall only they can see; a
keeper's sound breadcrumbs only them. And the hardest puzzles **cannot be solved alone** —
each of them sees a different fragment and they must say it out loud and assemble it together.
And the Watcher *quotes them* — a thing one of them said in voice chat last week appears
carved in stone; a plan they made is waiting for them where they planned to go.

It ends far down, past the Deep Line, at the grave with tomorrow's date — which opens from the
inside. The Seventh has been waiting. Whether the land trusts the group enough to let them
correct the record, and what that costs them, is decided by how they kept the ways — authored
by their own conduct, witnessed and entered, true.

It is **difficult**, **immersive**, **rich** (deep optional lore, many sidequests, six voiced
keepers), and **theirs** — because it watched them become the people who finished it.

---

## 2. VERIFIED CURRENT STATE (ground truth, 2026-06-29 — trust this over the old docs)

Re-audited against HEAD by three parallel passes (DB/seeds, Java, lore) + direct reads. The
**teardown is itself partially stale** — the code drifted forward from it. This is what's real:

### The engine — what's actually broken (the "it must run" guardrail)
- **KEYSTONE: `puzzles.requires_flags` is never created by any migration** (migrations stop at
  0005). Every gate/activation `UPDATE` in the seeds is wrapped in an `if column exists` guard
  that silently no-ops → 9 back-half rows stay dark forever, and one ships open that should be
  gated. One missing file is ~80% of "it doesn't run." *The seeds already author a complete,
  internally-consistent flag graph with no dead gates — it was written to assume 0006.*
- `getOpenPuzzles` (`repo.ts`) doesn't test `requires_flags`. The Discord resolver's
  `applyOutcome` **does** already apply `set_flags`.
- **Ignition is unwired.** The "it knows your name" content exists (`voice.ts`
  `prologueNamed`/`prologueUnnamed`) and the autonomy tick reads `prologue_ignited`, but
  nothing *sets* it. (The teardown's `recordFrameBreak()` was an old function name.)
- **specscheck RED** on 11 unclassified active rows; **seedcheck GREEN** (130 answers/45 rows).
- **Java cannot touch flags at all** — `ArcStateRow` has no `flags` field; `firstMatch` ignores
  `requires_flags`; `applyOutcome` never applies `set_flags`. The world answer-sign surface is
  not equivalent to Discord for any flag-bearing puzzle. No `/obs flag` admin command.

### What's fine (keep — don't "refactor")
- Whisper/hint **infrastructure** (`whisper_budgets`/`whisper_events`/`hints` tables; spend
  module). What's missing is hint **content** (the table is empty) — an authoring task.
- The whole-string idempotency core, TS/Java normalizer agreement, the fault-isolated autonomy
  layer, and the storylet-shaped `requires_flags` design are **sound**.
- Per-player illusion primitives partly exist: `Reveal.isHiddenFrom(player,…)` and
  `PerPlayer.fakeBlock` (client-only `sendBlockChange`). The **answer-sign** world surface
  (`AnswerSignListener`) exists — in-world answer submission already works.
- The **corpus is the gold**; the over-weave is in the *machinery*, not the prose.

### Overbuilt-but-unreachable (the integration debt this overhaul pays down)
- 5 rite listeners referenced everywhere don't exist; 5 beats coded but **not registered**
  (reveal/room_swap/keeper_npc/modeled_mob/spatial_voice). The "it knows ME" reactivity relied
  on a **broken global unwitnessed-mutation model** that never fires for a co-located group.
  `RoomSwapBeat` is in-place (should teleport). `SpatialVoiceBeat` isn't positional. ModelEngine
  is unbought (cut it). Beat double-fire is possible on a crash mid-PATCH.

---

## 3. THE FIVE PILLARS (the chosen direction, in detail)

### Pillar 1 — Premise: the living Seventh as the active goal
- **Keep** the world, the record, the six keepers + their 5 ciphers, the Liar-and-the-Catch,
  the Undercroft, the divergent fates by conduct, the Accepting rite, the corpus.
- **Reframe the goal:** the group is not waiting to be judged — they are **reaching the
  Seventh** (alive, far down) to **correct the record**: gather each keeper's testimony, prove
  Iss's lie, carry it down. The grading still happens (the land/Seventh only trusts those who
  keep the ways) but it now *serves a goal the players want*.
- **The traitor-companion (Wren) — the living present-tense betrayal.** A companion NPC
  (Citizens2 — passes as a normal player-friend) joins the descent early, builds genuine trust
  over weeks (helps, warns, gifts), and is revealed to be a **Kept-in-part** who has been
  feeding the group to the Watcher to preserve his own remaining self, steering them toward the
  Seventh as his escape key. His "I was protecting you from being taken like Iss" is a half-
  believed excuse over genuine self-interest. He is a **present-tense mirror of Iss** (Iss lies
  from pride/historical; Wren lies from shame + self-preservation/living). The reckoning gives
  the group a choice (condemn / understand / free) that feeds the ending. He is **the diegetic
  reason the Watcher knows sharp, quoted, precise things** about the group — see Pillar 5 two-
  register model. Gate his reveal behind the Iss-catch (players learn the pattern first on Iss).
  See `arc/lore/documents/the-companion.md` for his full character, voice, arc, and seed flags.
- **The finale = the reunion.** The future-dated grave that opens from the inside is the
  Seventh's long wait ending. Hard-won, bittersweet, not saccharine. The induction-twist
  survives as the **cost** of correcting the record (chosen, meaningful), and the divergent
  fates are authored by the group's own conduct. The reckoning choice (Wren) feeds a branch
  on the finale fates.
- **Per-keeper agency:** each keeper gets a thing players *do*, not just read (honor the way
  they broke — give back what Vaun hoarded, walk the rite Mara only read), with a mechanical
  payoff (the record updates; a door opens; the testimony is collected).

### Pillar 2 — A de-linearized web (constellations + salience)
- Each keeper is a **constellation**: a self-contained cluster of 3–5 storylets (a location, a
  journal fragment, a cipher, an environmental change) enterable in almost any order. The only
  global precondition is "the group is in the Hold."
- **The showrunner surfaces ONE salient legal storylet at a time** (DOSSIER #A5). Salience ≈
  `recency × player-fingerprints − recent-same-tone`. The group always has one thread to pull,
  chosen *for them*, without a waypoint.
- **Replace every hard "requires all 5 others" gate with a salience BOOST**, not an
  impossibility. `requires_flags` stays for the few genuinely *sequential* gates (the Iss
  catch → the deep), but convergence is mostly *encouraged*, never *forced* — kills stalls.
- **Tone rotation:** never stay in one register >2 sessions — **Uncanny** (something's wrong),
  **Archive** (pure cipher/lore satisfaction — genuinely fun), **Warm-Grief** (a keeper's
  humanity breaks through). This is how the multi-week experience avoids bleakness-fatigue.
  **Warm-Grief also carries pacing relief (the "exhale"):** schedule an explicit relief beat
  after every climax (a kinder keeper memory, a safe hearth, a small true gift — the "ways that
  companied the Dark"). These must be diegetic, never a gamey healing chest. Relief flows from
  two sources: Wren (warmth that curdles at the reveal) and untainted keeper memories that
  survive the reckoning (Mara's kinder margin note; a Sella copybook drawing that is only joy).
  Post-reckoning, keeper-memory relief is the honest warmth that remains — never route ALL
  warmth through Wren.

### Pillar 3 — Balanced integration: solo dread + asymmetric co-op
- **Solo per-player dread** (vanilla-first, no pack required to convey the beat): a block only
  you see (`sendBlockChange`), your name in the SGA rune font on a wall only you see
  (`TextDisplay` + per-player `showEntity`), per-player particles/fog, light that dims when
  you're watched (`hasLineOfSight` → packet light), a keeper sound that breadcrumbs only you
  (`PerPlayer.soundAt`).
- **Asymmetric-information co-op puzzles (the centerpiece mechanic the owner named):** each
  player is shown a *different* fragment of one solution (different runes / block states /
  map cursors via per-player `showEntity`/`sendBlockChange`); the answer only exists when they
  **say what they each see out loud and combine it**. *Keep Talking and Nobody Explodes*, native
  to the illusion tech. **Must scale to dynamic N** (§4): the fragments are partitioned over the
  *active* roster at solve-time, and degrade to fewer-but-still-solvable with 3 or grow with 8.
  **The signature co-op vault (the Threshold) is backed by a vanilla trial-chamber vault (1.21)
  with per-player keys** — vanilla's per-player lock-and-key mechanic handles the dynamic roster
  by default. Composition: the asymmetric fragments *produce* the combination/keys (the puzzle);
  the vault is the reward container the combination opens. Fragments = puzzle; vault = payoff.
  The companion (Wren) may have been steering the group toward this convergence beat as his
  escape key — wire the reckoning to the vault if the lore pass confirms it.
- **Beats trigger on player ACTIONS**, not only a calendar (first sleep on a black moon, first
  step past the Deep Line, first refused bow) — the world reacts to what they *do*.

### Pillar 4 — The world is the surface; the website is "the record"
- **Primary surface = the world.** The Watcher speaks through books that appear, signs that
  rewrite, titles/subtitles, the rune font, per-player whispers. Answers are submitted in-world
  at the keeper stones (`AnswerSignListener`, already built).
- **Out-of-game = "the record" website** (the reframed Vercel app): not a clean puzzle site —
  a **half-corrupted archive terminal of the Hold's own record-keeping** (a "recovered system"):
  degraded, half-redacted, entries out of order, integrity warnings. It unifies four surfaces into
  one artifact: the ledger (names write in) · the hint rail (an "integrity check / error log" that
  surfaces clearer warnings the longer a thread stalls) · the Iss lie (falsified entries the group
  corrects) · the Seventh's true record (restored as flags flip). The group **discovers it via a
  URL hidden in-game** and can **write answers into it** (diegetic remote submission — inscribing
  the record, not "typing to a bot"). It also carries the between-session "the record remembers…"
  memory. Security: website reads via RLS / edge-function ONLY — never the service key in the
  browser.
- **Discord has no game-persona.** It is the friends' own chat — *which the Observer Engine
  reads* (Pillar 5), but nothing in it presents as "The Watcher." This is the TINAG fix.
  However, Discord *is* permitted to carry **corrupted artifact leaks**: on specific in-game
  triggers (entering a cursed chunk, reaching a threshold) the bot may drop a degraded artifact
  (a corrupted OGG file, a status/PFP change to something they're looking at). No dialogue, no
  persona — a grounded corrupted echo of something real (a clip of their own VC, a screenshot of
  what they're looking at). This reads as the same recovered system bleeding into their comms,
  not as a character posting. Sequence: Observer capture must exist first; sequence D1 artifact
  leaks *after* that.

### Pillar 5 — The Observer Engine (the literal "it knows your name")
- **Sources:** in-game chat (`ChatListener` → log), Discord text (bot already has MessageContent
  intent), and Discord **voice** (the bot joins the VC, captures per-user audio via
  `@discordjs/voice` receivers, transcribes with **Whisper**).
- **Archivist pass:** an LLM (Claude) filters out irrelevant banter and extracts **grounded
  observations** with provenance — names, plans, fears, claims, inside jokes, "I'd never…/I'll
  definitely…" statements — into an `observations` table. **Grounding discipline (non-
  negotiable):** only real, attributed, observed things; *never* a fabricated callout (the
  codebase's existing "precision over recall / silence-is-canon" ethic extends here).
- **Two-register "how it knows" (the T1 resolution — the companion's reveal pays this off):**
  - *Ambient register (Tier 0 = the land itself).* Grounded in the **composure signal**: a
    per-player behavior accumulator tracking time in dark, recent damage, alone-vs-grouped,
    hoarding one item, revisiting one block. Lets the Watcher speak in implication with **zero
    chat/voice/LLM** ("you keep one thing you never use"; "you haven't looked up since you came
    down"). Always-on; never names, never quotes; needs no in-fiction channel. The land notices.
  - *Sharp register (Tier 1/2 = via the companion's leak).* Quotes *real* words and plans; rare,
    precise, uncanny. The companion (see §3 Pillar 1 and the traitor-companion character) has
    been relaying real group words and plans to the Watcher. This is the diegetic channel that
    explains the *specific* scares. Ramps with `companion_trust`. After the reckoning
    (`condemn`/`free`), the sharp quotes cease — the channel is gone, the world goes quieter.
    Under `understand`, the quotes persist but the group now reads them as sorrow, not threat.
  - **Consequence to author:** after the reckoning the sharp-quote scares must *change*; losing
    the channel must be felt. Ambient Tier 0 stays always; sharp quotes ramp then resolve.
- **Weaponization:** the showrunner selects an observation and surfaces it diegetically and
  **rarely** (rate-limited — rare = uncanny; frequent = creepy spam that breaks the spell): a
  sign that quotes a real phrase, a book that references a real plan, a whisper of their own
  words, the record website "remembering" something they said. May troll and scare with real
  specifics — that's the point.
- **Operational reality (honest):** the voice layer needs the bot **hosted and always-on** (it
  must observe between sessions for the autonomous showrunner), plus a Whisper runtime and an
  LLM budget. This is the heaviest new lift; build it *after* the core loop runs (§6).

---

## 4. THE THREE INVARIANTS THAT TOUCH EVERYTHING

### Dynamic roster (player count is not fixed; people join late)
- No role is assigned to a specific person — **roles emerge and are reassignable**.
- Every quorum/gate is relative to the **active** roster (`readActiveRoster`; the plugin already
  computes `effectiveQuorum = min(config, activeRosterSize)`).
- Every asymmetric co-op puzzle **partitions over the active roster at solve-time** and works
  for any N in a sane range (≈2–10): fewer players → fewer/larger fragments; more → more.
- A late joiner is **on-boarded by the record** (the website + an in-world "a new hand is at the
  mouth" beat) without breaking anyone else's state. Audit every new mechanic against this.

### Grounding + consent discipline (because it watches real people, incl. veterans)
- **Grounded only:** the Watcher cites only *real* observed behavior/words. A wrong "it knows
  you" is worse than none — degrade to silence, never guess.
- **Disclosure + opt-out by default:** a session-zero, out-of-fiction "this watches in-game,
  in Discord, and in voice, and will use what it hears; you can opt out or dial it down anytime"
  + a working mid-experience opt-out that reduces personalization no-questions, + an end debrief.
  The owner hosts and knows the group; this is shipped on by default, not a moral gate.

### Async-first design (6+ veterans never all log on at once)
- Progress **persists and leaves traces** — the record website is the async shared brain; a
  solo player can advance a thread any night without needing the full group.
- Most puzzles are solo or "night-shift" solvable. Only **convergence beats** (the asymmetric
  co-op vault, the reckoning) need quorum — and that quorum is relative to the **active** roster.
- The showrunner salience must be **roster-aware**: never surface a convergence-required thread
  unless `activeRosterSize ≥ effectiveQuorum`; else surface a solo/night-shift thread. Dead air
  caused by mis-tuned quorum requirements is the failure mode to avoid.
- Audit every new mechanic: can a player make meaningful progress alone? If not, reconsider.

---

## 5. CUT / KEEP / REWORK LEDGER (cohesion — nothing inert may costume itself as a puzzle)

### KEEP (and grow)
- Six keepers + 5 ciphers; the journals; the Liar-and-the-Catch; the Seventh (now the goal);
  the Undercroft; the Accepting rite; the divergent fates; the sidequests (they already
  self-acknowledge: a dead lead arrives and flips to a "contradicted" card — honest, not inert);
  the UNKEPT acrostic (cheap, self-correcting, honest flavor).

### REWORK (toward the chosen direction)
- The **finale** → the reunion with the living Seventh (grave-opens-from-inside = the meeting).
- The **showrunner drip** → salience-pick one thread; **hard gates → salience boosts** (keep
  `requires_flags` only for the truly sequential ones, e.g. the catch → the deep).
- The **in-game layer** → static world-build + **per-player packet illusion**; register the
  unregistered beats; `RoomSwapBeat` → sealed-door teleport; add per-player `showEntity` +
  positional sound; **build the asymmetric co-op puzzle system** (new).
- The **surface** → world-primary + the record-website (discover-by-URL, write-answers-in);
  retire the Discord game-persona; in-game answer signs are canonical.
- **Add the Observer Engine** (new subsystem, Pillar 5).

### DEMOTE to honest flavor (keep the prose, drop the fake-puzzle framing)
- **Nether & End as live dimensions** → Undercroft shelf lore (`the-fire-is-lent` etc.). The
  Seventh's deep is reached in the Overworld/Undercroft, not a second-dimension expedition.
- **FACT 17 (word-filing axis) + the "Ear"** → one flourish on the Archivist register.
- **The dynamic-difficulty *reveal* (FACT 2b)** → invisible pacing only; keep Mara's line as
  texture, don't promise a reveal players can't perceive.
- **The "you are the seventh opening" reading** → drop (it's a third "seven"; the group is "the
  next," and "the Seventh" is reserved for the keeper).

### CUT / DEFER
- ModelEngine + worn-skin apparition (unbought; a mislabeled Warden is worse than none). The
  creature *lore* from `bestiary.md` is NOT discarded — it is salvaged into vanilla texture-swap
  reskins + display-entity (`text_display` / `block_display` / `item_display`) apparitions (per
  INTEGRATION Layer 3). The bestiary's lore is the gold; the ModelEngine rig is what was cut.
- The broken global unwitnessed-mutation model (replaced by per-player illusion).
- **Player saboteur / sleeper agent** — CUT entirely. Replaced by the traitor-companion NPC (see
  Pillar 1; betrayal is felt by the whole group together, not between friends).

### NPC FRAMEWORK — resolved (D4 hybrid, 2026-06-30)
**Citizens2** for anyone who must read as a regular person as far as players can tell: the
traitor-companion (Wren) and the surface townsfolk (Aro / Wenna / Dob / Pell). **Vanilla-uncanny**
(armor-stand / display entity + PDC + interaction entity) for everything non-human or uncanny: the
six keepers, apparitions, the Watcher, statue-things. Citizens2 is now a justified dependency; pin
its build against the same Paper 1.21.x chosen at D5 (version pin is load-bearing).

### Collisions / specscheck (still required for "it runs", but only on surviving content)
- One live answer collision (`the one who turned away` on `stone-iss-wall` + `prophet-wall-name`)
  → disambiguate (one owner) + a resolver "prefer-unsolved-among-collisions" rule for the
  legitimately-sequential re-submissions (the bound word at the M4 gate). Collapse the docket
  twin. Classify/retire the 11 unclassified rows. *(Parked architecture from the first pass —
  0006 migration, gate-aware `getOpenPuzzles`, `matchPuzzles` — is reusable here since the chosen
  direction keeps flag-gating for the sequential spine.)*

### FACT-LAYER LEDGER — WAVE R2 (motif + fact-layer, 2026-07-02)

The fact-layers players triangulate. `confirmed` = flat Archivist voice; `implied`/`sealed` = strong;
`false` = weaponized (the forged eighth way, INV-17). The **misleading** layer was the audit's weakest
(Iss's lie self-refuted in its own margins) — R2 rebuilds it into a real, held-whole false trail plus a
fair correlation herring, and unsettles F4. Changes this wave:

- **F4 — "customs learned from the land."** WAS flat-true (`learn-them-as-we-learned-them` margin: *"it
  is true, all but one word"*). NOW **plausible-but-possibly-self-serving**: added a colder later margin
  noting *"learned, not made" is the reading that costs the founders least, and it was the founders who
  wrote it* — not refuted, only made un-restful. Layer: confirmed → **confirmed-but-contested**.
- **F-mercy (NEW, misleading) — "the Seventh was spared / it was mercy."** New doc
  `the-seventh-was-spared.md`: an earnest keeper who accepts Iss's framing whole, **with NO internal
  skepticism and NO refuting margin** (its counterweights live in *other* docs). The group must hold it
  in tension against F10b/`the-fire-they-let-out`/`the-seventh-not-kept` until the Seventh speaks direct.
  Recontextualizes on the Seventh's own hand: *spared* → *refused* (same door, warm side vs cold side).
- **F-faithful (NEW, misleading→correlation) — "the faithful are kept."** New doc
  `the-faithful-are-kept.md`: an M1 (movement 1) record fragment that reads as good-conduct → being-kept
  but only ever states the **correlation, never the cause** (*"the kept had kept the ways" ≠ "the ways
  got them kept"*). Fair by Van Dine #15 (the Archivist margin flags the gap for the careful reader; the
  naïve later hand closes it wrongly and forward-points to the leaf that breaks it). Sets up **F10b** (the
  land refused a keeper who broke nothing) to land as a **reframe**, not a flat contradiction.
- **Seven-motif (threaded, ≥3 independent keeper sites).** "There was a seventh" now triangulates without
  the sella→shrine chain: **Vaun's tally** (`counted-them-in-the-dark`: keeper-count reads *vii*, row
  stands six), **Brann's fires** (`do-not-close-your-eyes-here`: seven shrines, six lit, the seventh cold
  all through), **Sella's copybook** (NEW `a-a-a-the-copybook-by-the-water`: a child counts markers to
  seven, the far water folds it). Subtle, recognizable-not-loud (leitmotif sweet spot); never spelled out.
- **Cipher-as-inversion (motif made characterization, all six keepers, in-voice not stated as a rule).**
  Each keeper's cipher is now the formal shape of their self-defeat: **Vaun** Caesar = a count read a fixed
  amount under its true value (held back the odd); **Mara** book-cipher = truth only at the crossing of
  two other hands' words, never direct (the Reader who read instead of going); **Sella** = the reflection
  in the water, everything given back mirror-wrong (NOT a letter cipher — Atbash cut in R1); **Orin**
  substitution = every letter stands for what it is not, read by putting each back to the form it refused
  to bow to; **Iss** Vigenère = key is his own name, so the lock can only give back the man, and it decodes
  to his own inverse (*the one who turned away*); **Brann** rail-fence = read only in the dark (*the day
  lies by leaving the cold one off*). A pattern-reader who notices all six share an inversion structure
  arrives at the motif unaided.

---

## 6. BUILD ORDER (run-first; guardrail priority: runs → cohesive → great)

**PHASE 1 — PROVE THE CORE LOOP RUNS ON A REAL SERVER.** Nothing else matters until this lands.
1. Solder the engine: `0006_requires_flags.sql`; gate-aware `getOpenPuzzles` (+ Java twin);
   `set_flags` on both surfaces; atomic jsonb flag merge; `/obs flag` admin command; ignition
   (`#the-record`/website post or in-world act → `prologue_ignited`). Green specscheck/seedcheck.
2. The smallest vertical slice in-world: one static keeper-stone + labeled answer lectern; one
   per-player illusion ("it knows ME"); one sealed-door reveal; ignition fires; one cipher
   solvable *with a hint*. **Run it with 3–4 friends.** Prove the loop before scaling.

**PHASE 2 — THE SURFACE + THE SAFETY RAIL.**
- The record website (discover-by-URL, ledger view, write-answers-in); retire the Discord
  game-persona. Author the hint corpus (the empty `hints` table — P0 before real play).
- The salience drip; hard-gates → salience-boosts.

**PHASE 3 — THE SIGNATURE INTEGRATION.**
- The asymmetric-info co-op puzzle system (dynamic-roster-safe). Per-player illusion library
  (`showEntity`, positional sound, packet light, per-player fog). Register/rework the beats.

**PHASE 4 — THE OBSERVER ENGINE.**
- Sources (in-game/Discord text first; voice via Whisper after); the archivist extraction
  (grounded); sparse weaponization; the consent/opt-out + debrief.

**PHASE 5 — EXPAND, RICH AND DIFFICULT.**
- Grow keeper sidequests, deepen the Undercroft, the Seventh's descent, the finale reunion,
  more per-player integration — kept cohesive (§5) and findable (§3).

---

## 7. THE HARD RULES (restate every session)
1. **Cohesive** — no orphaned/contradictory pieces; inert lore must read honestly as flavor,
   never costume itself as a puzzle.
2. **It runs** — the engine is soldered; specscheck + seedcheck green; the smallest loop is
   proven on a real server before scaling. (Nothing here has *ever* run — Phase 1 is sacred.)
3. **Genuinely great for the friend group** — immersive, difficult, *findable*; profile real
   behavior and speak in implication; never break frame.

Plus the two invariants (§4): **dynamic roster** and **grounding + consent**.

**Design gates (apply before any new addition):**

- **D6 — Known-author lens.** Our players know Ethan built this; classic "is this real?" TINAG
  is off the table. Immersion comes from (a) diegetic consistency, (b) reactivity they didn't
  expect us to build, (c) discovery deeper than they assumed we'd bother with. This governs
  every decision about surface, NPC, and mechanic.
- **D7 — Anti-datamining stance.** Assume transparency AND weaponize it. Don't build fun a
  file-read destroys — the joy is in the *doing*. Reward the datamine: the NBT-heavy item is
  meant to be inspected; leave a message for the xrayer.
- **D8 — The Golden Question.** For every puzzle: *"If they ignore my intended solution, is
  there another logical path to the truth?"* If no, the puzzle is too fragile — make the
  environment reactive to their attempts.
- **D9 — Economy of mystery.** Every answer opens a slightly bigger question until the finale;
  the Seventh reunion must pay off emotionally *and* concretely (no empty mystery box).
