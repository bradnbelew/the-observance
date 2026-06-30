# THE OBSERVANCE — THE BUILD PLAN (start → finish, build-ready skeleton)

> **The operative doc.** Open this first next session. It is the ordered, checklist-style
> backbone for building the whole ARG to the chosen v2 direction. Strategy/why lives in
> [OVERHAUL.md](OVERHAUL.md); puzzle design in [PUZZLES.md](PUZZLES.md); integration menu in
> [INTEGRATION.md](INTEGRATION.md). This doc = WHAT to build, in WHAT order, with status.

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
no server — while the world-build and hosting happen in parallel.
