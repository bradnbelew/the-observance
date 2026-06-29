# Idea Treatment — The Undercroft Custom Dimension (the fog world + the room that rebuilds)

> **Status: KEEP-SCALED (already partially canonized + partially wired — this treatment closes the build gap and fuses it with the coop gate).**
> This is **MASTER-PLAN P1.12**. The narrative, the site, and the descent gate already exist and compile.
> What is **missing**: the world itself (the Multiverse void world + the `dimension_type` fog datapack are GO-LIVE manual steps, not code), and the **A→B "room rebuilds itself" swap is NOT WIRED** — `undercroft-fog` is a pure `lore` row with **no beat** (`puzzles_seed.sql` ~339). The room "rebuilding wrong" is currently only *narrated text*, never *enacted in the world*. That is the orphan this treatment kills.
>
> **Do not re-invent:** `SmallStructureBeat` (schematic + A→B idempotency), `DoorOpenBeat`, the `unbroken_light`/`coop_plate` sites, the `undercroft-descent`→`undercroft-fog` seed chain, `oracleMainBeat`/`oracleLore` voice keys — all built. Honor them.

---

## 0. The one-line truth

The Undercroft is the **one visually distinct, oppressive, server-side space** (no client install): a Multiverse void world with a datapack `dimension_type` that has `ambient_light: 0` and Nether-grade sight-blocking fog. You enter it through the lectern-comparator **descent door**. Inside is **one kept light in a doused world**. You gather, you think you have reached the climax — and while you are not looking, **the altar room rebuilds itself into something wrong** (a second FAWE schematic, reveal-disciplined, idempotent). The floor drops out of your model of the game. The Undercroft is also the *physical home* of the coop gate (`coop_plate`) and the Accepting floor (`unbroken_light`/`accepting_floor`) — the place where Movements III, IV, and V all happen.

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 What the Undercroft physically is (three layers, all server-side)

| Layer | How | Build artifact | Path-A status |
|---|---|---|---|
| **The world** | A Multiverse-Core void world, no natural generation, the descent door teleports into it | `multiverse` world `observance_undercroft` + an import command | ✅ no client install |
| **The fog/dark** | A datapack custom `dimension_type` with `ambient_light: 0` + `effects: minecraft:the_nether` (thick fog) — OR a dark custom-biome `fog_color`/`sky_color` | `datapacks/observance/data/observance/dimension_type/undercroft.json` | ✅ datapack, not the pushed pack — no install |
| **The rooms (A and B)** | Two FAWE `.schem` set-pieces pasted by `SmallStructureBeat`: `undercroft_room_a` (the room as you first descend) and `undercroft_room_b` (the same room rebuilt wrong) | two `.schem` files in `plugins/Observance/schematics/` | ✅ FAWE server-side |

The pack only adds the **sounds/textures** inside the fog (`observance:drone_low`, `observance:stone_breath`) — those keys already exist in the atmosphere plan. The fog itself is pure datapack.

### 1.2 The descent (already wired — honor it)

`undercroft-descent` (`puzzles_seed.sql` 313) is a built `main_beat`: solving Mara's book-cipher ("descend and bow at the unbroken light") fires an `unlock`→`door_open` beat at `unbroken_light` (`DoorOpenBeat`, radius 3, reveal-disciplined, the comparator-lectern door opens *out of sight*), sets `undercroft_open=true`, and routes to `undercroft-fog`. The teleport into the Multiverse world is the **showrunner's CONFIRM-mode stage** (or a portal block at the opened door). **No new descent code is needed.** The door beat is done; the world is the GO-LIVE step.

### 1.3 The room that rebuilds — the genuinely-unbuilt remainder (A→B swap)

This is P1.12's real code work. The reversal is currently **narrated but not enacted**. Build it as a **showrunner-staged `small_structure` beat** on `undercroft-fog`:

- **Room A** is the schematic that *is the Undercroft* when they descend — six labelled slots around the one kept light, the pressure-glyph floor, the cold square (the coop plate, inert here). They gather, they bow (they think this is the finale).
- **Room B** is the *same footprint, rebuilt wrong*: slots re-ordered, the glyph re-cut into a different word, a seventh slot that was not there, the kept light now lighting nothing. Pasted by a **second `SmallStructureBeat`** with `schematic: undercroft_room_b`, `require_floor: false` (void dimension), reveal-disciplined (`mutateWhenUnwitnessed`), idempotent (once B's footprint is occupied the re-fire fails the replaceable sweep and skips).
- **The catch:** `SmallStructureBeat`'s idempotency relies on the **footprint being air/replaceable** before paste. Room B pastes *over* Room A's blocks → the footprint is **occupied** → the replaceable sweep **fails** → B never pastes. **This is a real conflict with the built code.** See §2.1 for the mitigation (the swap needs a clear-then-paste, which the current beat does not do).

### 1.4 How it plays across the 5-movement arc

- **Movement I (Days 1–5) — the locked door, seen not opened.** The lectern-comparator door is *visible* in the surface world but dead — a comparator puzzle with no signal yet. The descent is rumored in documents, never reached. The Undercroft world exists but is unreachable. *Plant: there is a way down, and it is shut.*
- **Movement II (Days 6–11) — Mara's sentence assembles.** The book-cipher across the lectern shelf resolves to *"descend and bow at the unbroken light."* They now know *where* and *what* (descend, bow) but the door is still shut until they perform the sentence at the right stone. The cold square (coop plate) is glimpsed in early reconnaissance if the door ever cracks — inert.
- **Movement III (Days 6–11, the midpoint REVERSAL) — the descent and the rebuild.** They perform Mara's sentence; the door opens; they teleport into the fog. **The first true environmental dread of the whole arc** — `ambient_light: 0`, you cannot see the far wall, one light burns somewhere ahead. They reach Room A, gather at the six slots, bow. They think: *done, that was the climax.* Then they turn, or step out and back, and **Room A is gone — Room B is there**, wrong. The Watcher posts `oracleLore` once (the existing `undercroft-fog` fragment), naming nothing. The genre flips: from "something is in the dark" to "we have misunderstood what is being asked."
- **Movement IV (≈Day 12) — the Undercroft is the gate's hour.** The same fog room now houses the **coop gate**: the cold square (`coop_plate`) is finally live, the adjacent answer-sign is the CUT leg, Discord is the WORD leg. The room they thought was the finale is revealed as the *waiting room* for the real convergence. The Threshold (a third schematic or the far door of Room B) opens off the coop gate's `threshold_open`.
- **Movement V — the Accepting.** Same world, the `unbroken_light` floor of type `accepting_floor`; `AcceptingRiteListener` watches the synchronized bow. The kept light they first saw in M-III is the rite's light. The cold square they stood on alone in M-III/IV is now the whole present group bowing together. *The Undercroft is the one room the entire back half of the arc happens inside.*

### 1.5 The mystery it carries (FACT 11 / FACT 12 / FACT 14)

The single kept light is **FACT 11** ("one fire never went out") made literal — a lit point in a doused world, beautiful and wrong. The rebuild-wrong is **FACT 12** ("the kept ones did not depart. they were kept.") made spatial: the room is not abandoned, it is *maintained* by no one — passive voice as architecture. And the room rebuilding itself with you inside it, unwitnessed, is the first felt taste of **FACT 14** ("the record does not stop at the rite — it receives, it keeps you"): the place rearranges around you the way the record will.

---

## 2. CRITIQUE — adversarial and honest

### 2.1 THE SHARPEST RISK: the A→B swap collides with `SmallStructureBeat`'s own idempotency — as currently built, Room B can never paste over Room A.
`SmallStructureBeat.footprintClear()` requires every cell of the paste box to be **replaceable** (air/passable). Room B's footprint is full of Room A's blocks → `footprint-occupied` skip → **the reversal silently never happens.** The idempotency that protects every other structure is exactly what blocks the *intended* overwrite here. This is not theoretical; it is in the code I read.

**Mitigation (the core build work, pick ONE):**
1. **Preferred — a dedicated A→B swap path, not a raw second paste.** Add a `swap` mode to `SmallStructureBeat` (or a thin `RoomSwapBeat` subclass) that, inside the single `mutateWhenUnwitnessed` block: (a) clears the known A footprint to air, then (b) pastes B. Both happen in one unwitnessed, all-or-nothing, main-thread mutation. Idempotency moves from "footprint is air" to a **durable `swapped` flag / PDC marker on the region anchor** (re-fire checks the marker, not replaceability). This keeps every anti-jank guarantee (unwitnessed, all-or-nothing, idempotent) while permitting the deliberate overwrite. **Recommend this.**
2. Alternative — paste B into a *different sub-region* of the same room (B is not literally on top of A but adjacent/below in the void), and `door_open`/teleport the group from A-spot to B-spot. Cheaper code, but weaker: the "same room, changed" gut-punch is diluted if it is visibly a different place.

**Verdict: the swap is the one piece that needs new/extended code; do not ship the narration without it (that is the orphan).**

### 2.2 Reveal-discipline on camera: the rebuild must never be witnessed mutating, but the group is standing *in the room*.
`mutateWhenUnwitnessed` fires only when the base block is hidden from all players. In a 6–8 player gather-room, *someone* is usually looking at the floor. The swap could **stall indefinitely** waiting for a clear line of sight — and on a recorded video, "we waited and nothing changed" is death.
**Mitigation:** stage it as a **CONFIRM-mode showrunner beat at a scripted moment** — Ethan moves the group out (a `door_open` ushers them through to an antechamber, or the descent-out teleport), the swap fires while the room is provably empty/unloaded, they walk *back in*. The immersion-blueprint already calls M-III a "CONFIRM-mode showcase." The swap should fire on **room-empty / chunk-unwitnessed**, which the showrunner can guarantee by routing players out for one beat. Never leave it to chance ambient timing during the climax.

### 2.3 Multiverse + datapack fog is a GO-LIVE manual step, not code — and it is a single point of failure for the whole back half.
If the world or the `dimension_type` JSON is misconfigured, Movements III–V have no home. There is no code fallback for "the dimension didn't load."
**Mitigation:** (a) the datapack `dimension_type` is ~20 lines of JSON — validate it loads (`/datapack list`) before any session; (b) provide a **degraded fallback**: if the Multiverse world is absent, host the Undercroft as a **deep, sealed, lightless room in the overworld** (deepslate box, `ambient_light` faked with no torches + the pack's dark textures). The fog is the *premium* version; the dread survives without it. Document this in TODO-GOLIVE so a datapack failure does not brick the finale.

### 2.4 Orphaned-gimmick risk: LOW — but ONLY if the swap is built. Currently MEDIUM.
The Undercroft is maximally load-bearing: it is the physical home of the descent gate, the coop gate, AND the Accepting — three spine mechanics live inside it. It cannot be an orphan space. **But** the *specific* "room rebuilds itself" mechanic is **currently orphaned**: `undercroft-fog` narrates the rebuild with no beat to enact it. A mechanic-less narration of a mechanic is precisely the orphan the consistency law forbids. **Build the swap or cut the "rebuilds wrong" claim from the fragment.** (Do not cut it — it is FACT 12's best expression. Build it.)

### 2.5 Precision-law / collective-law: clean.
The Undercroft personalizes nothing — it is a *place*, not a profiling moment. `oracleLore` names no one. The Accepting and coop gates inside it already enforce active-players-only and no-absent-punish (their own treatments). The Undercroft adds no new precision surface. ✅ Keep `oracleLore` flat; never let the rebuild "address" a player.

### 2.6 Anti-jank: the swap must be all-or-nothing, main-thread, idempotent, offline-safe.
The schematic paste is FAWE-isolated (skips cleanly if FAWE absent — already true in `SmallStructureBeat.enactSchematic`). The swap mode (§2.1.1) must keep: footprint/marker re-check at mutation time, the whole clear+paste in one `mutateWhenUnwitnessed`, idempotent on the durable marker, protected-registry on B's blocks. No LLM is in this path — it is pure deterministic FAWE. ✅ if built to the existing pattern.

### 2.7 Should anything be CUT or scaled?
- **KEEP** the world + fog + descent (built/GO-LIVE; the whole atmosphere payoff).
- **KEEP-SCALED** the A→B swap: build it as the **swap-mode beat** (§2.1.1), staged CONFIRM-mode (§2.2). Do not ship the raw second-paste — it cannot work over A.
- **DO NOT** add a second visually-distinct dimension. One oppressive space is the whole point; a second dilutes it and multiplies GO-LIVE failure surface.
- **Priority: P1 (arc-spine)** — the world/descent is needed the moment M-III opens; the swap is the M-III gut-punch. Not P0 vertical-slice (the slice can use a single static room), but the spine and finale both require the world.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (cold, plain, concrete)

The only spoken line is the existing `undercroft-fog` `oracleLore` fragment. Held flat, mechanical, passive — the keeper register that counts and records, does not emote:

> the room rebuilds itself into something wrong. one fire is kept, eternal, attended by no one. they did not depart. they were kept. the rite is not a transaction.

A found-document margin (the late record-hand), explaining the kept light as maintenance, not miracle — grief shown as a logged fact, not stated:

> the fire is kept. no one keeps it. i wrote that twice to see if it would stop being true. it did not.

A keeper journal fragment, the descent, structural decay not announced dread (spacing widens as the hand fails):

> we went down because the page said down. the light was where the page said. we bowed.
>
> when i looked up the slots were not where we left them.

(All pass: lowercase, no caps/exclaim, no named emotion, no "testament/little did/in a world where," no three-adjective list, no "not just X but Y." The room *rebuilds* and *is kept* — passive, mechanical; it does not "remember" or "wait" for them.)

---

## 4. THREAD IT — exactly where this lives (no orphan)

**Canon FACTs / INV it realizes (adds none new — it is the *spatial body* of existing ones):**
- **FACT 11** (one fire never went out) — the single kept light at the Undercroft center; the one lit point in `ambient_light: 0`.
- **FACT 12** ("the kept ones did not depart. they were kept.") — the room rebuilding *itself*, maintained by no one; passive voice as architecture. **The A→B swap IS FACT 12's enactment.**
- **FACT 14** (the record receives/keeps you) — first felt taste: the place rearranges around you, unwitnessed.
- **INV-16** — the rebuild and `oracleLore` name no player; the Undercroft derives no "which active player." ✅

**Found-documents / journals that mention or foreshadow it:**
- `arc/lore/documents/do-not-close-your-eyes-here.md` (Brann) — the corpus's strongest M-III foreshadow; "do not look away in the kept room." Plants the *don't-witness* unease the swap pays off.
- `arc/corpus/journals-vaun-mara-sella.md` — Mara reads "descend and bow at the unbroken light"; add/confirm the descent fragment (§3 line 3) — the hand that looked up and found the slots moved.
- `arc/lore/documents/learn-them-as-we-learned-them.md` — the kept-light "we left, but the light is kept" line (FACT 11 buried).
- `arc/lore/canon-spine.md` FACT 11/12 entries (already reference the Undercroft by name — keep reciprocal).

**NPC / Watcher voice keys that carry it:**
- `oracleMainBeat` (`discord/src/voice.ts`, built) — the descent door opens; "what was shut is shut no longer."
- `oracleLore` (built) — the `undercroft-fog` rebuild fragment (above). **No new voice key needed** — the Undercroft reuses both.

**Cipher(s) / puzzle(s) it expresses (reuse the 11 built ciphers):**
- **bookCipher** — Mara's lectern-shelf page/line/word → "descend and bow at the unbroken light" (`stone-mara` → `undercroft-descent`, built). This is the *key to the door.*
- The pressure-glyph floor walked in M-III/V (`pressure-glyph-walk`, built) reuses the **shared rune alphabet** (`discord/src/forge/runes.ts`) — a glyph walked on the Undercroft floor is the same glyph learnable at a stone/Discord card (cross-surface truth).
- The Room B re-cut glyph (the swap) should spell a **different word** in the same rune alphabet — readable later as a `substitution`/`a1z26` micro-clue ("the slot that was added") feeding the M-IV catch. The rebuild is not just dressing; B's wrongness *encodes* something.

**Beat classes / listeners / tables / seed rows / sites / voice keys that realize it:**
- **`DoorOpenBeat`** (built) — the descent door, already wired on `undercroft-descent`.
- **`SmallStructureBeat`** (built, schematic path) — pastes `undercroft_room_a` at GO-LIVE / first descent.
- **NEW: swap-mode on `SmallStructureBeat` (or `RoomSwapBeat extends SmallStructureBeat`)** — clear-A-then-paste-B in one `mutateWhenUnwitnessed`, idempotent on a durable `swapped` PDC/flag marker, `require_floor: false`. **This is the one genuinely-unbuilt code symbol.**
- **NEW seed wiring:** add a `beat` to `undercroft-fog` (`puzzles_seed.sql` ~339) — `type: small_structure` (swap mode), `site_id: unbroken_light`, `schematic: undercroft_room_b`, `require_floor: false`, CONFIRM-mode (`pending` curatorial, staged by showrunner). Today it has **no beat** — this is the wiring gap.
- **`unbroken_light` site** (`sites.yml` 130, type `accepting_floor`) — the room footprint; A and B paste here.
- **`coop_plate` site** (`sites.yml` 287) — lives inside the Undercroft; the cold square.
- **NEW GO-LIVE artifacts:** `multiverse` world `observance_undercroft`; datapack `data/observance/dimension_type/undercroft.json` (`ambient_light: 0`); two `.schem` files `undercroft_room_a.schem` / `undercroft_room_b.schem`. Document all three in `plugin/TODO-GOLIVE.md`.

---

## 5. PLANT THE PAYOFF — the "OH, that is what that was for" seed

- **PLANT (Movement I, inert):** the lectern-comparator **door** in the surface world — a dead comparator puzzle with no signal, and Brann's `do-not-close-your-eyes-here` document warning "do not look away in the kept room." Both read as ambient unease with no target. The Undercroft world exists, unreachable. **Ambiguous, inert.**
- **MID (Movement II):** Mara's book-cipher resolves the door's instruction; "the kept room" gets a name and a place. Brann's warning is filed as keeper-superstition.
- **PAYOFF (Movement III, the reversal):** they descend, gather in the kept room, bow — *and the room rebuilds itself while they are not looking.* Brann's "do not look away in the kept room" was **literal, not superstition** — the room only changes when unwitnessed (reveal-discipline made diegetic). The inert M-I door and the dismissed warning were the whole midpoint all along. *"OH — that is why he said don't look away."*
- **ECHO (Movements IV–V):** the same room becomes the coop gate's hour and the Accepting floor. The kept light first seen in M-III is the rite's light in M-V; the cold square stood-on alone is the whole group bowing together. The one room paid off three times.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

- **Lives in:** door/world **planted M-I**, instruction **named M-II**, **descent + rebuild fire M-III** (the midpoint reversal), **re-used as coop-gate home M-IV** and **Accepting floor M-V**. The Undercroft is the single space the entire back half inhabits.
- **Depends on:** `stone-mara` book-cipher solvable → `undercroft-descent` (built); the Multiverse world + `dimension_type` datapack (GO-LIVE); `undercroft_room_a`/`_b` `.schem` files; the swap-mode beat (UNBUILT).
- **Depended on by:** the coop gate (`coop_plate` lives here), the Accepting (`unbroken_light`/`accepting_floor` is here), `threshold_open`, the entire M-V finale. **The world is a hard precondition for the back half** — hence the overworld-fallback mitigation (§2.3) is non-negotiable insurance.
- **Priority: P1 (arc-spine).** World/descent needed at M-III open; the A→B swap is the M-III gut-punch. Build order: (1) Multiverse world + `dimension_type` datapack + the two `.schem` files (GO-LIVE), (2) **swap-mode on `SmallStructureBeat`** with durable-marker idempotency + CONFIRM-mode staging, (3) wire the `small_structure` swap beat onto `undercroft-fog` in `puzzles_seed.sql`, (4) Room B's re-cut glyph encodes the M-IV micro-clue, (5) the overworld degraded fallback documented in TODO-GOLIVE.

---

## 7. The one build gap to flag loudly

The Undercroft world and descent door are **built or GO-LIVE-ready**. The **"room rebuilds itself" reversal — the actual P1.12 payoff — is NOT enacted**: `undercroft-fog` is a beatless `lore` row, and the only structure beat that exists (`SmallStructureBeat`) **cannot paste Room B over Room A** because its own footprint-clear idempotency rejects the occupied footprint. Until a **swap-mode beat (clear-A-then-paste-B, idempotent on a durable marker, CONFIRM-staged)** is built and wired onto `undercroft-fog`, the midpoint reversal is narration with no world behind it — an orphaned mechanic by the consistency law. This is the highest-leverage unbuilt piece of P1.12.
