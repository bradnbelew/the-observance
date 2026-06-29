# Backlog — FAWE Large Set-Pieces (the schematic branch + authoring loop)

> MASTER-PLAN P0.3 / R5. Reconciliation of a prior-session build-backlog item against the sealed web.
> Scope law for this doc (from the task): EXPOUND = **what remains to build + how it fuses with the new ideas**, not a re-invention of finished code.
> Read first: `design/atmosphere-stack.md §3`, `design/structures.md`, `plugin/.../beats/lib/{SmallStructureBeat,FaweSchematicPaster,SchematicPaster,Schematics}.java`, `arc/lore/canon-spine.md`.

---

## 0. Ground truth — what is ALREADY built (honor it; do NOT re-invent)

The "is the pipeline wired?" question structures.md asks is answered **yes** by the code:

- **`SmallStructureBeat.java`** has BOTH branches live: the inline-cell path (≤256 cells, footprint-validated) AND the `enactSchematic(...)` FAWE branch. The schematic branch already does, in order: name-sanitize (anti path-traversal, `[a-z0-9_-]` only), file-exists check, `dimensions()` read, **`MAX_SCHEM_VOLUME = 32_768` size cap**, `resolveBase`, **`footprintClear(...)` pre-check** (in-range + replaceable + optional floor over the whole `[base..base+dims)` box), the **`mutateWhenUnwitnessed(...)` reveal wrapper with a re-check inside**, the paste through `ctx.safety().call("beat.structure.paste", ...)`, and **`protectBox(...)`** registering every solid pasted block with `ctx.protectedRegistry()`.
- **`FaweSchematicPaster.java`** is the only class importing `com.sk89q.*`. It caches clipboards by absolute path, computes `dest = base + (origin - regionMin)` so the **region MIN corner lands at base regardless of where the author stood at `//copy`**, and pastes with `ignoreAirBlocks(true)` + `copyEntities(false)`. Every method is `catch(Throwable) → null/false`.
- **`Schematics.java` / `SchematicPaster.java`** are the FAWE-free seam: string-probe (`Class.forName`) + plugin-enabled check, lazy instantiation, so a server without FAWE never class-loads the sk89q importer (no `NoClassDefFoundError` reaches the engine). Absent FAWE → `BeatResult.skipped("fawe-unavailable")`.
- **`sites.yml`** already carries every set-piece site, ENABLED, coords `null` (= UNPLACED → silently skipped): the six `stone_*`, `unbroken_light`, `rune_rosetta`, `stone_of_reckoning`, `the_far_water`/`the_threshold`/`the_cold_hearth`, plus the back-half `the_unwriting`/`coop_plate`/`first_marker_01`/`carve_anchor_*`/`herd_anchor`/`grave_spur`.

**Verdict on "confirm the pipeline":** the Java branch is built, reflectively isolated, footprint-checked, protected, reveal-disciplined, and size-capped. It compiles and degrades gracefully. **It does not need rewriting.** The remaining work is three precise gaps below — none of them is the paste path itself.

---

## 1. EXPOUND — the genuinely-unbuilt remainder (the 3 real gaps + how they play)

### GAP A — Durable idempotency is *claimed* but not *backed* (the one true code gap)

`atmosphere-stack §3.3` lists as an anti-jank requirement: *"Idempotent (anti-jank #9): tag the paste in Supabase so a restart never double-pastes the same stone."* The code comment in `SmallStructureBeat` instead argues idempotency comes **for free** from footprint occupancy: a re-fire fails the replaceable sweep because the structure is already there, plus the in-process applied-set + the poller's durable `status='fired'`.

That argument is **almost** right and has one real hole: **a set-piece whose footprint was cleared by a player between fires** (griefed corner, a creeper, a `protectedRegistry` miss on a passable block like a torch/sign that is NOT protected by `protectBox`'s `!isAir && !isPassable` filter). On the next poll the footprint re-reads as replaceable → it **re-pastes**. For a keeper-stone that is fine; for the **Undercroft door swap** or the **Movement-III "the room rebuilds itself"** set-piece, a partial re-paste over a half-broken room is exactly the jank the law forbids. The poller's `status='fired'` covers a *normal* restart but NOT a re-queued beat (the showrunner legitimately re-enqueues a `small_structure` for the same site in a later movement, e.g. the Threshold sealing then re-sealing).

**The fix (small, additive):** a durable **paste-ledger** keyed by `(site_id | schematic | base-cell)`. Before `enactSchematic` calls `pasteAtMinCorner`, consult the ledger; after a `true` paste, write the ledger row. A row present = skip (`BeatResult.skipped("already-pasted")`). This is the Supabase tag the doc demands, and it is the ONLY behavioral code change in this whole backlog item.

How it plays: invisible to players. It only ever prevents the rare ugly double-paste. On camera it means a keeper-stone that someone chipped at and that the protect-listener restored never silently grows a second copy.

### GAP B — The authoring loop is documented as prose, not as a closed, no-ambiguity checklist tied to the X1 guard

`atmosphere-stack §3.4` and `structures.md §3.4` describe `//copy → //schem save`. What is missing is the **interlock with the rune/cipher pipeline**: a keeper-stone schematic carries a **carved sign in the `observance:runes` font** whose text is the keeper's **bound cipher plaintext** from `clue-specs.ts` (the X1 guard round-trips it). If the author types the rune text *into the schematic at build time*, the plaintext is frozen into a binary `.schem` where the X1 guard can never see it — a silent drift the moment a plaintext is re-tuned. So the authoring loop must specify: **the schematic carries the stone geometry + an EMPTY sign/lectern/text-display; the rune text is written at runtime by `SignWriteBeat`/`LecternFillBeat` from the live plaintext.** Build the body, not the words. (This is also why `structures.md` already says "runes are signs/lecterns/text-displays … never hand-placed blocks.")

How it plays across the arc: the *stone* is FAWE-pasted (a body); the *carving* is a separate text beat fired at the same site. A re-tuned cipher updates the carving on the next fire without re-exporting a single `.schem`.

### GAP C — `require_floor` semantics for multi-storey set-pieces (a footprint edge the inline path never hit)

The Undercroft rooms and the Threshold lintel are **not floor-supported solids** — the lintel is a span you crouch *under* (air below the span), the Undercroft door sits in an existing wall. `footprintClear`'s `requireFloor` demands solid support under every `dx,dz` of the bottom layer, which a lintel/door/overhang **fails by design**. The inline cairn path never exposed this because cairns are floor-set. **No code change needed** — the payload already accepts `"require_floor": false` — but the **authoring spec must mark per-site** which set-pieces paste with `require_floor:false` (lintel, door, overhang niches) vs `true` (stones, cairns, altar floor). Shipping this as a per-site table prevents a builder floating a lintel or skipping a stone.

### The arc, concretely (which set-piece pastes when, ~2-week / 5-movement)

| Movement | FAWE set-pieces that paste (via `small_structure` schematic branch) | require_floor | New-web role |
|---|---|---|---|
| **I** | `first_marker_01` body (the teaching-stone), `rune_rosetta` dais, `stone_of_reckoning` | true | carries the prologue marker glyph + the six UNKEPT maker's-marks (fall-order) + the `kept here before you` plant (FACT 16) |
| **II** | the six `stone_*` keeper-stone bodies (Vaun→Iss), the **dead-shrine** surface at `the_cold_hearth` (Iss's false endpoint) | true | each stone = a `keeper_stone` answer-site; the rhyming chorus (FACT 5) |
| **III** | the **Undercroft rooms** at `unbroken_light` (the gather-room shell), the **doused-alcove** niches, the **seventh-shrine deep** `the_unwriting` body (sealed; pasted but unlit) | mixed | FACT 11 (the one kept fire), FACT 10/10b (the Seventh) |
| **IV** | the **Threshold lintel** at `the_threshold`, the **future-dated grave** (`grave_spur` or `the_threshold`), the **"room rebuilds itself" A→B swap** (re-paste of an altered Undercroft schematic) | **false** for lintel/grave-lid | FACT 6, FACT 13b (the death-clock misread), the Iss catch |
| **V** | the **altar deposit-slot floor** finalization at `unbroken_light` (the Accepting quorum room), the `restore`/`erase` hearth block-state at `the_cold_hearth` | true | FACT 13/14/15, `seventh_choice` |

The A→B "room rebuilds itself" (IV) is the single most demanding consumer: it is the one place a **second, intentional** paste over a region must succeed — which is exactly why GAP A's ledger must key on `(site | schematic | base)` and NOT on `(site)` alone (the swap uses a *different* `schematic` at the same site, so its ledger key differs and it is allowed; a re-fire of the *same* schematic is blocked).

---

## 2. CRITIQUE — adversarial, honest

**R1 — "It's all already built; this is a no-op backlog item." (the sharpest risk of self-deception.)** Mostly true, and that is fine to say out loud: the paste path is done. But GAP A is a **real anti-jank #9 violation hiding behind a comment that argues it away**. The footprint-occupancy argument fails precisely for the re-paste set-pieces (Undercroft swap, re-sealed Threshold). **Mitigation:** ship the Supabase ledger (GAP A). It is ~30 lines and closes the one law this item actually breaks. Do not let "it compiles" stand in for "it is idempotent under re-queue."

**R2 — Orphaned-gimmick risk: a beautiful pasted room nobody is sent to.** A FAWE set-piece is inert geometry. If a schematic exists but no seed beat references its `site_id`, or no cipher/clue routes players there, it is the textbook orphaned mechanic the consistency law forbids. **Mitigation:** every set-piece in the §1 table is anchored to a FACT and to an answer-site or listener (keeper-stones = answer-sites; `the_unwriting` = `SeventhChoiceListener`; `unbroken_light` = `AcceptingRiteListener`; `coop_plate` = `CoopPlateListener`). The THREAD section §4 makes this binding explicit. **Build no `.schem` that §4 does not name.**

**R3 — Reveal-discipline on a 32k-volume paste.** `mutateWhenUnwitnessed` checks the *base* block's visibility, then pastes a 32×32×32 box that may extend into a chunk a player *can* see even though the base corner is hidden. On camera that is a structure visibly materializing — the cardinal sin. **Mitigation (spec, not code):** author hero set-pieces to paste **fully inside unloaded/unwitnessed chunks hundreds of blocks ahead** (atmosphere-stack §3.3 step 2 already mandates this) — the discovery is the player *arriving at* a finished room, never watching it build. For any set-piece that must paste near players (the Threshold re-seal), keep its **volume small** (a lintel is ~5×3×1) so the base-visibility check is a sufficient proxy. Add a build-spec rule: *hero rooms = far + unloaded; near-player swaps = tiny.*

**R4 — Path A.** FAWE is a server-operator dependency (GPL, Java 21), zero client install. **No violation.** The ledger is a Supabase write the engine already does for every beat. Clean.

**R5 — Precision/"it knows you" law.** Not directly engaged — set-pieces are world geometry, not personalization. BUT the `name-where-never-been` carve (FACT 16) pastes the *name text* via `SignWriteBeat` at a `carve_anchor`, and the **body** of that anchor could be a FAWE stone. Risk: the stone-body schematic must NOT bake any player name (impossible — names are runtime), and the carve must obey INV-16 (active-only, never co-located with an offline-skin). **Mitigation:** carve_anchor bodies are blank stones; the name is a separate, tracker-grounded text beat. Already the §1-GAP-B law (build the body, not the words).

**R6 — Collective-judgment law.** A set-piece must never be a "chosen one" altar. The `unbroken_light` room is explicitly sized for **6–8 to bow together** and `AcceptingRiteListener` gates on **active** players. **No violation**, provided the altar schematic has **N identical deposit slots**, not one privileged plinth. **Mitigation:** spec the altar as a ring of identical slots; INV-16 forbids any slot spatially corresponding to a per-player carve.

**CUT / SCALE recommendation:** **Cut nothing from the paste path.** **Scale down** the ambition of GAP A to a single ledger table (do not build a generic "world-mutation journal"). **Scale down** the "room rebuilds itself" A→B swap to its minimum: a *second curated schematic* pasted at the same site after the first is broken/cleared out of sight — not a diffing engine. The deterministic spine stays a spine.

---

## 3. DE-SLOP TEST — exemplar in-voice lines (cold, plain, concrete)

These are the carvings/records the set-pieces carry. They must pass the anti-slop law and the canon-spine voice (lowercase-spare, the record names names, never emotes).

> *(the_cold_hearth dead-shrine surface — Iss's false endpoint, the Archivist hand):*
> `here the fire was let go. count the stones: six set, one taken out. no hand has warmed this since.`

> *(first_marker_01 teaching-stone, the planted line — inert in M1, pays off at the catch):*
> `kept here before you. the ground was filed under a name. the name is not yet cut.`

> *(the_threshold underside, Orin's broken hand — legible only crouched):*
> `i thought it small. i did not stoop. they went down. i —`

> *(the Undercroft, the one kept fire, a single found line — FACT 11):*
> `we left. the light is kept. no one was asked to keep it.`

Each: concrete object (stones, ground, fire), the iceberg (what is *not* said — who keeps the light, whose name), no named emotion, no thematic bow, no exclamation. The third degrades structurally (breaks off at "i —"), not by stating dread.

---

## 4. THREAD IT — exact non-orphan placements (the consistency law)

**Canon FACTs this idea adds / touches** (it adds none; it is the *physical body* that several existing facts are read off — which is the proof it is not an orphan):
- **FACT 11** (one fire never went out) — its *only* physical home is the Undercroft `unbroken_light` schematic (a single lit point in a doused room). No set-piece, no FACT 11.
- **FACT 16** (filed by place) — the `first_marker_01` body carries the `kept here before you` line; the `carve_anchor_*` bodies host the runtime name-carve.
- **FACT 10 / 10b** (the land can refuse / refused one who broke nothing) — the `the_cold_hearth` surface (dead-shrine) and `the_unwriting` deep (seventh-shrine) are **two schematics, one anchor**, temporally layered (canon-spine §5 anchor law). The deep is pasted-but-sealed until `iss_caught` + `seventh_named`.
- **FACT 13 / 13b / 14** (personal token / stone cut before keeper is kept / the record keeps you) — the `unbroken_light` altar deposit-slot floor + the `grave_spur`/`the_threshold` future-dated grave lid (opens from inside on its date = the single Accepting instant, canon-spine §8.2).
- **FACT 5** (fates rhyme) — the six `stone_*` bodies are the rhyming chorus.
- **INV** touched: **INV-14/INV-COORD** (a coord clue is a navigation pointer; the answer is a token *at the destination* — the destination is a set-piece), **INV-16** (carve-anchor bodies blank; no slot corresponds to a per-player carve), **INV-17** (set-pieces carry the forged *Covering of the Hands* as a **document** on a stone, never a `CUSTOM_KEYS` listener — FACT 7b's forgery is proven by the absent toll, not by withholding the carving).

**Found-documents / journals that must mention or foreshadow it:**
- `arc/lore/documents/*` — the abandonment-era report (FACT 12 passive voice) must reference the kept light *as a place* (the Undercroft set-piece), so the room is foreshadowed before it is found.
- the founder margin (`arc/corpus/cipher-plaintexts.md`) line *"we cut the names before the keeping"* (FACT 13b) must point at the future-dated grave set-piece.
- `arc/corpus/cipher-plaintexts.md` — the bound plaintexts for the six stones are the **runtime** carving source (NOT baked into `.schem`; GAP B).

**NPC / Watcher / Keeper voice lines that carry it:** the Keeper's M5 summons references the altar room (`unbroken_light`); the Archivist record lines are the dead-shrine/teaching-stone carvings (§3 exemplars). Keeper voices are read at their stones (Mara at her stone via lectern-shelf, Sella at `the_far_water` reflection, Orin at `the_threshold` crouched, Brann at night) — each keyed to a **set-piece site**.

**Ciphers / puzzles expressed (reuse the 11 built ciphers):**
- **caesar** → Vaun's stone (`stone_vaun`).
- **bookCipher** → Mara's stone (`stone_mara`, the lectern-shelf body).
- **atbash** → Sella's stone (`stone_sella`) + `the_far_water` mirror reflection.
- **substitution** → Orin's stone (`stone_orin`) + the forged *Covering of the Hands* signature (FACT 7b).
- **vigenere** → Iss's stone (`stone_iss`), key = his own name (the lie).
- **railFence** (rails=6) → `the_unwriting` rite (reuses Brann's taught literacy; canon-spine note on `the_unwriting`).
- **a1z26** → the literacy teaching-rung read at `first_marker_01`.
- **coordEncode** → every coordinate clue depends on `stone_of_reckoning` (digit-glyphs); the decoded value navigates *to a set-piece*, answer is a token there (INV-COORD).
- (beacon/colour-sequence → Brann is read at night, not a built-cipher; `polybius`/`columnar`/`morse`/`a1z26` remain available for secondary stones / the Rosetta drills.)

**Beat classes / listeners / tables / seed rows / sites.yml / voice keys that realize it:**
- Beat: **`SmallStructureBeat`** (the `schematic` branch) — the realizer. Companion text beats at the same site: **`SignWriteBeat`, `LecternFillBeat`, `BookAppearsBeat`** (the runtime rune carving; GAP B).
- Listeners already watching set-piece sites: **`AnswerSignListener`** (keeper-stones), **`AcceptingRiteListener`** (`unbroken_light`), **`SeventhChoiceListener`** (`the_unwriting`), **`CoopPlateListener`** (`coop_plate`), **`BeatProtectionListener`** (restores any protected pasted block).
- **`sites.yml`** — every entry in the §1 table (already present, ENABLED, coords null).
- **NEW table (GAP A):** `discord/supabase/migrations/*` — a `world_paste_ledger` table `(id, site_id, schematic, base_x, base_y, base_z, world, pasted_at)` with a UNIQUE constraint on `(world, schematic, base_x, base_y, base_z)`. Read/written through the existing `BeatContext` Supabase seam.
- Seed rows: the `small_structure` beats that reference each `"schematic"` name — these MUST exist or the geometry is an orphan (R2). One seed row per set-piece per movement.

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for"

**The plant (Movement I, inert/ambiguous):** `first_marker_01` is pasted in M1 carrying the line `kept here before you. the ground was filed under a name. the name is not yet cut.` Read in M1, it is a dead keeper's epitaph — archaic, a prediction about *strangers who came before*. The empty sign-slot beside it (GAP B: a blank carving the schematic ships) reads as a weathered, never-finished marker. Nobody thinks it is about them.

**The payoff (Movement IV→V):** the `name-where-never-been` carve (`SignWriteBeat` at a `carve_anchor` body) writes an **active player's own name** at a cell they have provably never visited. At that moment `first_marker_01`'s "the name is not yet cut" re-reads: the blank slot was always *theirs*; "before you" was never about strangers (FACT 16). The future-dated grave (FACT 13b) lands the same beat from the other side — its carved date, misread in M2 as a death clock, **is the Accepting instant** (canon-spine §8.2), and the grave-lid set-piece opens from the inside on that date as the rite fires. Two set-pieces, planted movements apart, pay off as one recognition: *the stone was cut before the keeper was kept, and the keeper is us.*

No plant without payoff: the blank slot → the name-carve. No payoff without plant: the carve only lands because the blank slot stood inert for two weeks.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

- **Lives in:** all five Movements (the §1 table). The **paste path itself** is needed from **Movement I** (the Rosetta + teaching-stone are M1 literacy gates).
- **Depends on:** FAWE 2.15.x present (operator install — already specced); the **resource pack live** (the rune font, so carvings render — GAP B's runtime text needs it); `stone_of_reckoning` placed before any coord-bearing set-piece is navigated to (canon-spine; coord rows inactive until then); the cipher plaintexts in `cipher-plaintexts.md` frozen by the X1 guard (GAP B).
- **Depended on by:** essentially the whole world-build — keeper-stone answer-sites (the clue web), the Undercroft climax (`AcceptingRiteListener`), the Seventh thread (`SeventhChoiceListener`), the M4 Iss catch (the dead-shrine vs deep two-place reading). If the paste path is broken, the descent has no bodies to read.
- **Priority:**
  - The **paste path** = **P0 (vertical-slice)** — already met; the slice in atmosphere-stack §6 (First Keeper-Stone + doused alcove) exercises it.
  - **GAP A (Supabase ledger)** = **P0** — it is the one open anti-jank #9 violation; cheap; required before the IV "room rebuilds itself" swap can ship safely.
  - **GAP B (authoring loop = body-not-words checklist + per-site `require_floor` table)** = **P1 (arc-spine)** — it is a documentation/authoring interlock, not a code change, but without it a builder will bake a plaintext and drift the cipher.
  - **GAP C (`require_floor:false` per-site marking)** = **P1**, folded into GAP B's table.
  - Hero rooms (Undercroft, Threshold swap, altar) as actual `.schem` files = **P1→P2 (depth)**, a go-live manual build, gated by the slice proving the look first.

---

## 7. THE CLOSE — what to actually do

1. **Ship GAP A:** add `world_paste_ledger` (migration + a check-before / write-after in `enactSchematic`, keyed `(world, schematic, base x/y/z)`). The only behavioral code change in this item.
2. **Write GAP B/C:** the authoring-loop checklist as a closed table in `structures.md §3.4` — *body-not-words* (runtime rune carving), per-site `require_floor`, far+unloaded for hero rooms / tiny for near-player swaps, ledger-key note for the A→B swap.
3. **Do NOT touch** `FaweSchematicPaster` / `Schematics` / the existing footprint+protect+reveal logic. It is correct.
4. **Build no `.schem`** that §4 does not anchor to a FACT + a listener/answer-site + a seed row.
