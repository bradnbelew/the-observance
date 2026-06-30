# THE OBSERVANCE — THE BUILD PLAN (start → finish, build-ready skeleton)

> **THE RESUME ENTRYPOINT.** If the session opens with "pick up where we left off on the Minecraft
> ARG," read this doc start-to-finish — it is the **complete** build/fix/expand guide for the whole
> thing (code · plugin · datapacks · resource pack · ciphers/puzzles · structures/world · story/lore ·
> NPCs · the bot · the record website · the Observer Engine), in order, with status. Strategy/why is
> in [OVERHAUL.md](OVERHAUL.md); puzzle design in [PUZZLES.md](PUZZLES.md); the integration menu in
> [INTEGRATION.md](INTEGRATION.md). **Trust only these 4 docs + the code/seeds.** Everything under
> `design/archive/` is superseded — do NOT build from it. §9 = content-staleness fix-list; §10 =
> stale-code fence; **§11 = the "Minimum Amazing" fallback scope; §12 = world+audio art direction;
> §13 = the Observer Engine fallback tiers; §14 = the playtest loop; §15 = the risk register;
> §16 = the open-decisions register.** This doc = WHAT to build, in WHAT order, with status, with risks.

---

## 0. THE CANONICAL DOC SET (read these; ignore the rest)

**Canonical (current):**
- [OVERHAUL.md](OVERHAUL.md) — the direction, the five pillars, the two invariants, the story.
- **BUILD-PLAN.md** (this) — the ordered build skeleton + status.
- [PUZZLES.md](PUZZLES.md) — the diverse puzzle system (the cipher-monotony fix).
- [INTEGRATION.md](INTEGRATION.md) — the Minecraft integration catalog.

**Reference (keep, not canonical):** `research/` (the 8 lane notes + DOSSIER), `TEARDOWN.md` (defect
list — partly superseded, still useful), the `arc/` corpus + lore (the gold), `content/npc-dialogue.md`,
`structures.md` (the world-build spec), `ideas/` (raw sidequest/lore source — needs triage into the
cohesive plan, §7).

**Archived/superseded → `design/archive/`** (moved this session): the old WEB-MASTER, BUILD-MANIFEST,
INTEGRATION-V2, COHERENCE-AUDIT-V2, MASTER-PLAN, PROGRESSION-LANES, PLAYTHROUGH-SCRIPT, STORY-WEB,
NEW-SESSION-PROMPT, CURSED-MAP-SITE, MINECRAFT-INGEST-PREP, OVERHAUL-BRIEF, FINAL-REPORT, the
`playthrough/ teardown/ critiques/ audit/` process dirs, and the story-map/story-web JSON. They drifted
or are folded into the four canonical docs. History is in git if anything's needed back.

---

## 1. STATUS SNAPSHOT (what is real, 2026-06-29)

- ✅ **PROVEN (Discord engine):** the storylet gate nerve — `0006_requires_flags.sql` + atomic merge
  RPC, gate-aware `getOpenPuzzles`, `oracle/gate.ts`, `matchPuzzles`+unsolved-preference, ignition
  wired, specscheck/seedcheck/gatecheck + all showrunner suites GREEN. `npm run gatecheck`.
- ⚠️ **WRITTEN, COMPILE-PENDING (Java surface parity):** `FlagGate` (predicate PROVEN via javac) +
  the OracleResolver/SupabaseClient/rows wiring + `/observance flag`. Needs the owner's `gradle build`.
- ❌ **THE GAME ITSELF — ~0% built:** no world geometry (every `sites.yml` coord is null), no `.schem`
  files, **no audio (zero OGG)**, no fog datapack, no display-entity visuals, NPCs unplaced, the
  signature v2 features (per-player illusion, asymmetric co-op, Observer Engine, the record website,
  the reunion) unbuilt. **Nothing has ever run on a real server.**

The honest shape: a strong *engine + script* for a game that **has not been built**. Phase A below is sacred.

---

## 2. HARD BLOCKERS FROM THE CRAFT AUDIT (do these or it can't run / can't land)

1. **No world.** Hand-build the Deep Hold (descent shaft, 6 keeper-stones, the two Rosettas, the
   Threshold, the Cold Hearth, the Undercroft gather-room, the shore pool). Export each `.schem`,
   fill `sites.yml` coords. ~10–20 hrs in a client. **Nothing else matters until this exists.**
2. **No audio.** Ship the 4 declared OGG events + keeper voices (mono Vorbis). A sound beat that
   plays silence is worse than no beat.
3. **No atmosphere.** Author the Undercroft fog **datapack** (dimension + ≤3 biomes + `mood_sound`).
4. **The Seventh is a void** (and the new ending needs them) — §6.
5. **Empty hint rail.** Author 2–3 diegetic hint tiers per spine puzzle (the `hints` table is empty).
6. **FAWE paste is main-thread** (tick-stall tell) — wrap async + relight.
7. **Missing flag producers** — at minimum `IgnitionListener` (the arc can't start); `/obs flag` is
   the stopgap.

> **PREP DONE 2026-06-29 (server-free head-starts on these blockers):** #4 Seventh → written
> ([the-seventh-below.md](../arc/lore/documents/the-seventh-below.md), with copy-paste voice fixes).
> #5 hint rail → DRAFTED ([`discord/supabase/seeds/hints_seed.sql`](../discord/supabase/seeds/hints_seed.sql),
> drop-in for the 5 ciphers + the catch). #3 atmosphere → fog datapack SCAFFOLDED
> ([`datapack/observance/`](../datapack/observance/), untested — verify pack_format + dimension load).
> #1 world → concrete room-by-room build plan in [WORLD-BUILD.md](WORLD-BUILD.md). Session-zero/consent →
> [SESSION-ZERO.md](SESSION-ZERO.md). Still fully yours: the manual world-build, hosting, apply 0006.

---

## 3. THE PHASES (start → finish)

### PHASE A — PROVE THE CORE LOOP ON A REAL SERVER (sacred; nothing past it counts first)
- [ ] Apply `0006` + re-run seeds on the Supabase project (lights the back half).
- [ ] `gradle build` the plugin; fix any compile errors in the Java parity wiring; fix the FAWE async bug.
- [ ] Build **one room**: one keeper-stone + labeled answer lectern, statically placed, real coords.
- [ ] One **per-player illusion** ("it knows ME": a fake block / a rune only one player sees).
- [ ] One sealed-door reveal; ignition fires; one cipher solvable **with a hint**.
- [ ] Ship the **4 OGG sounds** + a **stub fog datapack** so the room has atmosphere.
- [ ] **Run it with 3–4 friends.** Prove: ignition → gate → solve → unlock → a scare lands.

### PHASE B — THE SURFACE + THE SAFETY RAIL
- [ ] The **record website** (reframe Vercel app): discover-by-URL, ledger view, **write answers in**,
      redactions lift with flags. Retire the Discord game-persona.
- [ ] Author the **hint corpus** (diverse hints for diverse puzzles — PUZZLES §7).
- [ ] Salience drip (one live thread at a time); hard gates → salience boosts.
- [ ] Generalize the puzzle row with **`answer_kind`** (PUZZLES §4) so non-typed answers work.

### PHASE C — THE SIGNATURE INTEGRATION (defines the feel — INTEGRATION "signature 8")
- [ ] Per-player illusion library: `showEntity`, packet light (dims when watched), per-player fog.
- [ ] The **asymmetric co-op vault** (dynamic-roster fragment partition).
- [ ] The **Lens** item; the **reflection puzzle**; **display-entity** beats (floating runes, faces).
- [ ] **Desire-paths** (heatmap → worn path / grave on the most-walked route).
- [ ] RoomSwap → teleport; register the 5 unregistered beats.
- [ ] Build the **diverse puzzles** (2–3 per keeper, PUZZLES §5) + the external surfaces (Drive
      archive, 1–2 HyperFrames found-footage clips).

### PHASE D — THE OBSERVER ENGINE (the "it knows your name" payoff)
- [ ] Sources: in-game chat + Discord text first; Discord **voice (Whisper)** after.
- [ ] LLM **archivist** extracting **grounded** observations (never fabricated).
- [ ] Sparse, precise weaponization (quote real words/plans back).
- [ ] **Consent layer:** session-zero disclosure + opt-out + debrief (ships on by default).

### PHASE E — STORY DEEPENING + COHESION (the "lots of story/sidequests" the owner wants)
- [ ] Triage `ideas/` + the lore documents into the cohesive sidequest/lore web (§7).
- [ ] Reconcile the corpus to the v2 direction (§6) — the Seventh, "kept," the reunion, enrollment.
- [ ] Author the rich optional lore + sidequests (keep them honest: flavor, never fake-puzzles).
- [ ] Final **dead-weight pass**: cut anything now orphaned by the changes.

---

## 4. CROSS-CUTTING INVARIANTS (every phase)
- **Dynamic roster** — no fixed N, no role pinned to a person, quorums relative to active roster,
  asymmetric puzzles partition over who's active.
- **Grounding + consent** — the Watcher uses only REAL observed things; disclosure/opt-out by default.
- **Cohesion** — nothing inert may costume itself as a puzzle; rich standalone lore is fine as honest flavor.
- **Vanilla-first degrade** — every clue legible without the pack (illageralt / Discord mirror).

---

## 5. EXTERNAL DEPENDENCIES / DECISIONS THE OWNER OWNS
- Apply migrations + re-seed (only Ethan can reach the Supabase project).
- A **host** for the bot/showrunner/Observer (must be always-on for between-session + voice).
- The **world-build** (manual client hours) — the one thing no code can do.
- Citizens2 vs vanilla-PDC for NPCs (recommend vanilla-PDC to keep deps minimal).
- Whether to ship the **voice** layer (consent call for the veteran group).

---

## 6. STORY REPAIR — THE SEVENTH + "KEPT" (folded here; the one thing the new direction broke)

> **DRAFTED 2026-06-29:** the Seventh is now written as a living character in
> [`arc/lore/documents/the-seventh-below.md`](../arc/lore/documents/the-seventh-below.md) — their leaves,
> the "kept" reconciliation line, the reunion's first spoken line, AND a rewrite-spec for the stale
> Seventh docs + voice.ts keys + the Iss-seam. Next session: apply that rewrite-spec (wire the lines into
> voice.ts/seeds, retire the contradicted old docs). The remaining items below are that application.

The v2 "Seventh is alive / reunion finale" needs a Seventh that the corpus never wrote — they exist
only as **absence**. And **"kept"** now means two opposite things (corpus: *absorbed = horror*; v2:
*rescued/recorded-true*). Fix, in Phase E (or earlier if a seed lands):

1. **Write the Seventh's document** — a journal in their OWN hand, **undegraded** (they kept every way
   perfectly; their voice is the clearest, most direct of the seven — distinct from the six). Covers
   the casting-out (for nothing done), the long wait, what they understand now. Something the group
   physically **carries down** as proof. Without this the reunion is an abstraction.
2. **Reconcile "kept"** with one line in the Seventh's own voice that establishes a THIRD meaning,
   retroactively charging Mara's "the light keeps — it is not the same thing." Seed draft:
   > *"i kept the ways and was not kept. i know now what i did not know then — keeping and being kept
   > are not the same, and only one of them is what the record owes. you came. that is the other one."*
3. **Write the reunion as a scene** — the Seventh's **first spoken line** to the group (not Watcher
   narration about them). Seed draft:
   > *"someone came. i had stopped marking the days for someone coming. say your names — slowly — i
   > want to put them down right. the record will have you wrong; i will have you true."*
4. **Re-valence enrollment** — the `keeperPage*` keys (authored in the journal appendices but **missing
   from `voice.ts`**) must read as being *recognized*, not *consumed*; populate them into `voice.ts`.

---

## 7. THE RICH CONTENT BACKLOG (the "lots of story/sidequests" — make it cohesive, don't just pile it)
- `ideas/` (28 files) + `arc/lore/documents/` are raw material, NOT yet in the cohesive web. Triage each:
  KEEP-as-sidequest / KEEP-as-flavor-lore / CUT. Map every kept item to a thread + a surface + an
  honest label (flavor vs puzzle).
- Target: each keeper has a constellation of 3–5 storylets (PUZZLES §5) + optional deep-lore sidequests
  that reward exploration with lore/atmosphere/items (the existing 18 travel destinations already
  self-acknowledge — keep them). The Seventh, the Undercroft, and the surface town (Aro/Wenna/Dob/Pell)
  each carry their own sidequest cluster.
- Cohesion gate: before adding any lore, confirm it doesn't contradict the v2 frame (§6) and reads as
  honest flavor, not a fake game-thread.

---

## 8. NEXT SESSION, START HERE
Read §0 → §3. If a Supabase seed + a built room exist, do **Phase A** to first real run. If not, do
the **Seventh writing (§6)** and **author diverse puzzles + hints (PUZZLES)** — pure content that needs
no server — while the world-build and hosting happen in parallel. Also clear the §9 staleness ledger.

---

## 9. STALENESS LEDGER — content vs v2 (audited 2026-06-29; the cohesion fix-list)

**Verdict:** the 4 canonical docs (§0) are current + self-consistent. But ~**60–65% of player-facing
STORY/LORE content is stale**, concentrated in two places (the Seventh; "kept" as an ending), and
several old PLAN docs contradict the canon. The engine + the 6 keeper journals' *personal histories*
are fine. This is a content-rewrite job (mostly §6/§7), not an engine job.

### A. CONTENT STALE — contradicts v2, must rewrite (sources to fix)
- **The Seventh (BLOCKER).** Every dedicated doc treats them as cast-out/absent: `arc/lore/documents/
  the-seventh-not-kept.md` (frames refusal as *mercy*), `the-fire-they-let-out.md` (restore = a name in
  a ledger), `arc/corpus/letters.md` L14a, `official-records.md` R06a. **Worst: `the-name-i-cut-myself.md`
  has the Seventh go "not down… the other way," directly contradicting "alive far DOWN."** Rewrite all to
  alive-below-waiting (see §6); recontextualize "not down" (they returned to the Deep once they understood).
- **"kept" collision (SERIOUS).** The v2 third meaning (rescued/recorded-true) exists *nowhere*. Old
  horror-meaning anchors: Mara's last entry ("the light keeps — it is not the same thing" — keep it, but
  the Seventh's new line must recharge it), Brann marginalia ("joined the watching") + his M5 close
  ("i see {name} lit now"), Iss appendix ("{name} does not come back from this"). Fix the **payoffs** in
  `voice.ts`: `keeperCloseSeventhRestored()` ("kept, late" → reunion-with-living) and `graveOpened()`
  ("the hole is the deposit slot" → the Seventh opens it from inside to meet them).
- **Enrollment valence (SERIOUS).** `voice.ts` `keeperPageHeading_*` / `keeperPageHand_*` read as
  *consumed* ("mine to enter, and not given back"). Keep each keeper's grammatical fingerprint; flip the
  meaning to *recognized* ("i saw this in you and named it"). NOTE: these keys are authored in the journal
  appendices but several are **missing from `voice.ts`** — populate + re-valence together.
- **"you are the seventh" (SERIOUS).** `arc/lore/documents/six-were-kept-before-you.md` uses the DROPPED
  third-seven reading. → "you are the next." (The `kept:6` lure mechanic itself still works.)

### B. PLANS STALE — prefer the canon; flag, don't trust
- **ALL ARCHIVED 2026-06-29** (in `design/archive/`, pull only if a specific artifact is wanted):
  `cipher-web.md` / `clue-web.md` (old cipher-monotone plan + the node/edge map — PUZZLES.md is canon);
  `bestiary.md` (specced ModelEngine rigs, which are CUT — but its creature *lore* is worth salvaging
  into a v2 vanilla-reskin bestiary in Phase E); `atmosphere-stack.md` (ModelEngine-first — INTEGRATION.md
  supersedes); `SETTINGS.md` (the dead 3-setting pitch).
- `RoomSwapBeat.java` — still in-place mutation; v2 = sealed-door teleport (known; compile-pending).
- **OPEN DECISION (owner's call):** `sites.yml` + `progression_seed.sql` still carry the **Nether/End as
  live worlds** (`observance_nether`/`observance_end`, gated `active=false` so harmless now). OVERHAUL/
  INTEGRATION *demoted* them to Undercroft lore (a "land it small" call). Given the taste for rich + lots
  of content, this may be wrong — **decide: fully cut them, OR re-allow the 2 deepening lanes** (and
  update INTEGRATION.md). Don't leave the docs contradicting the seeds.

### C. THE CALLBACK WEB (spiderweb) — plants mostly solid, some payoffs broken
- **INTACT (keep):** Iss lie→catch (now *more* central — it's how players learn to distrust his account
  of the Seventh); forged eighth; prophet wall; UNKEPT acrostic; offline-skin apparition; Sella's shore →
  "the last marker is not the last" (now the *descent entry* toward the Seventh); the Unlit Deep latch;
  the fate keys; `thread_cards` structure; `recordReceives`; the Break triangle (accident/betrayal/mercy —
  *improved* under v2 as three accounts held until the reunion); `fateInheritorsCodicil` ("a hand not yet
  here" now foreshadows the Seventh).
- **BROKEN payoffs:** the future-dated grave (plant solid; payoff "deposit slot" stale → reunion); the
  Seventh restore (absorb-a-name → meet-a-living-person).
- **MISSING new connection:** catching Iss's wall-lie must *also* prompt re-reading his testimony about
  the Seventh — one oracle line + one `thread_cards` edge (`happened-no-wall` → re-open `surface-seventh-
  marker`). This is the seam that wires the solved catch into the new main quest.

### D. SURFACE
`voice.ts` content isn't text-stale, but it currently *posts through the Discord bot*. Phase B must route
the Watcher's voice to the world + record website (mechanical re-wiring, not a rewrite).

---

## 10. STALE CODE — rework before reuse (so old code can't infect the new build)

The **Discord engine is clean** (the soldered gate, resolver, oracle, showrunner — all GREEN; build on
it freely). The **Java parity wiring is new but compile-pending** (gradle build it first). The items
below are **older plugin code that must NOT be reused as-is** — each contradicts the v2 integration model
(INTEGRATION.md). Rework or replace before building on them:

- **The global "mutate-only-when-unwitnessed" model** (`Reveal.mutateWhenUnwitnessed`, relied on by
  `RevealBeat`/`SignWriteBeat`/`SmallStructureBeat`/`RoomSwapBeat`/`LecternFillBeat`) — never fires for a
  co-located group. v2 = **per-player illusion** (`showEntity`/`sendBlockChange`/packet-light). Keep the
  static world-build pastes; replace the "live reveal" reliance with per-player illusion.
- **`RoomSwapBeat`** — in-place mutation → **sealed-door + teleport-on-reentry**.
- **`ModeledMobBeat`** + the worn-skin path in `NamedMobBeat`/`offline-skin` — **ModelEngine is CUT** and a
  mislabeled Warden is worse than nothing. Use **vanilla texture-swap reskins** only.
- **`SpatialVoiceBeat`** — not actually positional (reads `behind`/`offset`, ignores them). Use
  `PerPlayer.soundAt(loc)` at a real behind-the-player location, or drop the dead params.
- **`FaweSchematicPaster`** — pastes on the **main thread** (tick-stall tell). Wrap async +
  `fastMode(true)` + `changeSetNull()` + relight.
- **5 beats coded but NOT registered** in `BeatLibrary` (`reveal`/`room_swap`/`keeper_npc`/`modeled_mob`/
  `spatial_voice`) — the enactor can't fire them. Register them only **after** the rework above.
- **`BeatQueuePoller`** double-fire window on a crash mid-PATCH — claim-then-act (flip to `firing` before
  enacting) when hardening for a real run.
- **The 5 missing flag producers** (`IgnitionListener` etc.) don't exist — build them (Ignition first; the
  arc can't start without it). `/observance flag` is the stopgap that proves gating meanwhile.

---

## 11. "MINIMUM AMAZING" — the ruthless fallback scope (build this even if everything else is cut)

The honest risk is not "it's bad" — it's "60% built, then stalls" (months of work, sole builder). So
define the smallest version that is **still genuinely great**, and treat everything past it as
enhancement. If time/energy/budget runs short, ship THIS and it's a real, finished experience:

- **One well-built region** (not the whole Deep Hold sprawl) — the descent, ~3 keeper-stones placed,
  the Cold Hearth, the shore pool, the Undercroft gather-room. Quality over extent.
- **The 6 keepers** via their journals (the gold) + **~6–8 diverse puzzles** (PUZZLES §5 palettes,
  spanning ≥4 types — NOT 6 ciphers).
- **The Iss lie → catch** (built + central) and **the Seventh reunion** (the rewritten §6 payoff).
- **Per-player illusion + the 4 audio events + the fog datapack** (the atmosphere floor).
- **The world + the record website** as the two surfaces; **text-only Observer** (Tier 0–1, §13).
- **2 playtests** (§14).

**Cut first if needed (in this order):** the external surfaces (Drive/YouTube/Voice) → the asymmetric
co-op vault (nice-to-have, hard) → the **voice** layer of the Observer → the Nether/End → most optional
sidequests → the full Undercroft sprawl. None of these are load-bearing for a great experience.

## 12. ART DIRECTION — world + audio are CRAFT, not checklist items (the amazing-determiners)

The engine can be perfect and the game still mediocre if these are phoned in. Budget real hours + a
reference board for each:
- **World / level design** (elevate `structures.md` from spec to *built craft*): built-by-hands-then-
  abandoned; **carved, never default** blocks; the **bow built into the architecture** (stones angled so
  you must stoop to read); vertical-descent dread; sightline reveals (round a corner into a thing);
  cold-hearth and shore-pool as set-pieces; a few deliberate **"wrongness"** details. Decide ONE palette +
  a lighting discipline (dark is the default; light is earned). FAWE-relight after every paste.
- **Audio direction** (the project's #1 missing layer — don't ship generic stings): **mono** OGG Vorbis,
  sparse, restrained; a sub-bass room-tone drone bed; the whisper, the cold toll, stone-breath; **6
  keeper voices, distinct + degraded**; positional always; lean on biome **`mood_sound`** so the engine
  self-generates dread. Sourcing bar: CC0 + ElevenLabs is fine ONLY if processed/curated to feel
  *recovered*, not stock. Priority: the 4 declared events first, then keeper voices.

## 13. OBSERVER ENGINE — graceful-degrade TIERS (so the north star never depends on flaky voice infra)

The "it knows your name" payoff must land WITHOUT the fragile parts. Build bottom-up; each tier is
complete on its own:
- **Tier 0 (no infra, always works, ethically cleanest):** profile **in-world behavior only** + speak in
  *implication* ("you keep one thing you never use") — vague-true-of-anyone lines grounded in real
  actions. Delivers ~80% of the magic with zero chat/voice/LLM. **The whole arc must work at Tier 0.**
- **Tier 1 (cheap):** + ingest **in-game chat + Discord text** → LLM archivist extracts grounded
  observations → sparse, precise weaponization (quote a real phrase back).
- **Tier 2 (expensive/fragile, build LAST, optional):** + Discord **voice (Whisper)**. Flag the cost
  (always-on host + Whisper + LLM $) and the dependence on the group actually being in VC.
- **Always:** grounding (only REAL observed things, never fabricated) + session-zero consent/opt-out +
  debrief. If voice proves impractical, the experience is unharmed.

## 14. THE PLAYTEST LOOP — amazing is iterated, not specced (bake it into the phases)

The gap between "the loop runs" and "they were genuinely hooked for weeks" only closes with real players.
Expect the first run to reveal it is **not amazing yet** — that's the input, not failure.
- **Playtest 1** — after Phase A (the vertical slice, 3–4 friends). Watch: findability (dead air?),
  dread (did a scare land?), friction (did they know HOW to answer?), illusion desync.
- **Playtest 2** — mid-build, one full movement. Watch: pacing over a real gap, tone fatigue, the salience
  drip picking the right thread, answer-type discoverability.
- **Playtest 3** — pre-finale, the catch → reunion. Watch: does the emotional payoff land; does "kept" read right.
- After each: capture findings → revise the plan/content/tuning. **Findability and pacing can only be
  tuned here** (the original "too dense to find" sin is solved in playtest, not on paper).

## 15. RISK REGISTER (every factor that could sink "amazing" — keep visible)

| Risk | Severity | Mitigation |
|---|---|---|
| Scope > one builder's capacity; stalls at 60% | HIGH | §11 Minimum Amazing; cut ruthlessly |
| Bus factor 1 (sole builder/host/director, burnout on the manual world-build) | HIGH | smallest world that's great; recruit a build helper if possible |
| "Amazing" unvalidated — nothing playtested | HIGH | §14 loop; first run = feedback, not verdict |
| Amazing-determiners (world/audio) least planned | HIGH | §12 art direction as first-class craft |
| Findability ("too dense to find") only testable late | HIGH | salience drip + hint rail; tune in §14; don't over-add content (§7) |
| Observer voice layer flaky/expensive | MED-HIGH | §13 tiers; arc works at Tier 0 |
| Asymmetric co-op hard to build fair across dynamic N | MED | mark nice-to-have; degrade to solo if it fights the roster |
| Answer-type discoverability (player doesn't know what KIND to submit) | MED | diegetic signposting per answer_kind; teach the verb in-world |
| Surface migration (Discord→world+website) = worse UX | MED | validate the in-world sign + website input UX in Playtest 1 |
| Per-player packet illusion desyncs (chunk reload/interaction) | MED | use for simple blocks; revert on move/timer; TextDisplay for tile-entities |
| Resource-pack decline → degraded clues | MED | vanilla-first (illageralt) + Discord/website mirror; `force=true` + reason |
| Supabase service key in the browser (website) | HIGH (security) | website reads via RLS/edge function ONLY; never ship the service key |
| Shared DB outage → both surfaces go silent (dead-looking) | MED | the code fails-safe to silence (good); add a liveness alarm |
| Hosting + LLM + Whisper recurring cost/ops | MED | budget it; Tier 0–1 keeps it near-zero |
| External surfaces (Drive/YouTube/Voice) rot + pull players out | LOW-MED | use sparingly; treat as enhancement |
| Engagement collapses after week 1 (hook didn't hold) | HIGH | ignition is strong; Playtest 1 must prove session-1 grips |
| Seventh/"kept" rewrite lands saccharine or loses the dread | MED | §6 seed lines; keep bittersweet, test in Playtest 3 |
| "Lots of content" vs "findable/cohesive" tension re-bloats it | MED | every add passes the cohesion gate (§7); salience surfaces one thread |
| Pin: exact Paper 1.21.x not chosen → font/model/packet drift | LOW | pin ONE version, author for exactly it |

## 16. OPEN DECISIONS REGISTER (undecided — do not let these get forgotten)

Owner calls / design choices still open. Resolve as they come up; none should be silently defaulted:
1. **Nether/End** — fully cut, or keep as 2 optional deepening lanes? (taste leans keep; OVERHAUL demoted
   them; seeds still carry them gated). §9.B.
2. **NPCs** — Citizens2 vs vanilla-PDC framework? (recommend vanilla-PDC for minimal deps).
3. **World layout** — the actual level design is undesigned (only a spec). Needs a real plan/reference board.
4. **Audio direction** — palette + sourcing standard (§12) not yet chosen.
5. **Hint content** — the `hints` table is empty; tiers per spine puzzle unwritten.
6. **The actual diverse puzzles** — designed at palette level (PUZZLES §5), not as concrete puzzles yet.
7. **Session-zero consent script** — DRAFTED in [`design/SESSION-ZERO.md`](SESSION-ZERO.md) (the
   out-of-fiction consent message + the cursed-map handoff + the debrief + a setup checklist). Review/adjust.
8. **Recording/YouTube layer** — mentioned, not planned (Replay Mod + shaders, Ethan-only).
9. **Hosting + cost budget** — where the always-on bot/showrunner/Observer runs, and the monthly $.
10. **Website security model** — RLS vs edge-function read path (NEVER the service key in browser).
11. **Asymmetric co-op** — core feature or nice-to-have? (recommend nice-to-have until the loop is proven).
12. **Paper version pin** — choose ONE 1.21.x and author for exactly it.
13. **Cohesion-vs-"lots" mechanism** — how much sidequest/lore, gated by what, so it stays findable.
