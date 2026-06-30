# The Observance — PLAYTHROUGH 4: MOVEMENT III (The Undercroft + the Liar) — the literal shooting-script

> **What this is.** The ordered, literal "what is there" for Movement III — Act 2 mid, ~Days 10–11
> (`WEB-MASTER §1.M3`), plus the Liar engine that spans M2→M4 and *catches* on the seam into the Undercroft
> stretch. NOT a description of the content — the content itself, quoted from the REAL repo artifacts. Every
> quoted line is sourced; every not-yet-built thing is marked **[GAP — TO BUILD]** (code/text) or
> **[GAP — GO-LIVE]** (in-game build/asset), with the file/owner that carries it. Where an artifact and a
> design doc disagree, the design doc (`WEB-MASTER` / `INTEGRATION-V2`) wins and the seam is flagged.
>
> **Sources of truth quoted below:** `discord/supabase/seeds/puzzles_seed.sql`,
> `discord/src/voice.ts` + `discord/src/voice.archive.ts`, `discord/src/showrunner/liar.ts`,
> `plugin/src/main/resources/sites.yml` + `config.yml`, `arc/corpus/cipher-plaintexts.md` +
> `arc/corpus/npc-and-watcher-voice.md`, `arc/lore/documents/the-fire-is-lent.md`,
> `design/content/npc-dialogue.md`, `design/structures.md`, `design/PROGRESSION-LANES.md`,
> `WEB-MASTER §0.5/§1.M3/§3/§4/§9 + §2 in-roads table`, `INTEGRATION-V2 §A4/§A5/§A6/§A7/§A11/§D4/§D5/§D10`.
>
> **The Movement-III drama law (`WEB-MASTER §1.M3`):** the **tempo changes** — Establishment and the
> six-stone field were patient table-work; M3 is the **expedition** (decode → *walk* → *descend*). The two
> calibrated beats are (1) the **descent into the fog dimension** (the world physically leaves them), and
> (2) the **room-rebuilds-wrong** gut-punch (FACT 11/12 spoken by what they witness, never by a sentence).
> The Liar's **catch** is loud but *cold* — a correction, not a jump-scare. Everything else stays mechanical.

---

## WHERE M2 LEFT THEM (the seam this segment opens from)

> Carried from `3-movement-2.md`. The six keeper-stones are deciphered (any order — the resolver ignores
> movement); both literacies (`rosetta_known`, `reckoning_known`) are held; Iss has been **met warm** and his
> stone (`stone-iss-wall`, Vigenère key=`ISS`) read. The group stands at a fork they cannot yet see is a fork:
> **trust Iss** (the warm reading → his coordinate → the dead-shrine) or **doubt Iss** (the name-as-key
> reading → the catch). Mara's `stone-mara` book-cipher has yielded the descent sentence. This segment is the
> **descent it names** and the **Liar engine resolving against it**.

---

## PART A — THE DESCENT (the lectern-comparator door → the fog dimension)

### A.1 — The instruction already in hand (Mara's descent sentence)
- **WHAT IT IS / WHERE:** the **bound plaintext** of `stone-mara` (book cipher, the six-book Kept-Light
  shelf at `kept_light_home_01`). Sourced verbatim, `cipher-plaintexts.md §stone-mara`:
  > **BOUND PLAINTEXT — DO NOT CHANGE:** `DESCEND AND BOW AT THE UNBROKEN LIGHT`
  - The six lectern books it reads out of (verbatim, `MARA_BOOK`, ¶ = a book):
    > descend the stair when the water is still // and bow your head at the door // the unbroken light
    > waits at the foot of it // do not write the way down and think it kept // do the thing the marks tell you
  - **The load-bearing line is `do not write the way down and think it kept`** — Mara's whole lesson
    (the answer is a *deed*, not a typed string). It rhymes forward to `pressure-glyph-walk` at M5 ("do not
    decode walk it") and to her Nether margin (A.10).
- **WHO / gating:** anyone holding `rosetta_known`. Mara's stone is one of the six; nothing here is M3-gated.
- **DIRECTOR ACTION:** none. The shelf is a **[GAP — GO-LIVE]** in-game build (`kept_light_home_01`,
  `structures.md §36`: a hearth scan-zone + the six-book lectern shelf); unplaced = silently skipped.

### A.2 — The descent itself (the spine event — `undercroft-descent`)
- **WHAT IT IS / WHERE:** SEED `undercroft-descent` (`puzzles_seed.sql`, MOVEMENT III block). The group
  **performs** Mara's sentence — they go to `unbroken_light` and descend. `outcome_type: main_beat`.
  - **kind + clue + plaintext + accepted answers + answer-verb (verbatim):**
    - kind: a **performed** main beat (not a cipher — the cipher was `stone-mara`; this row is the *doing*).
    - accepted answers: `['descend at the unbroken light', 'descend through the lectern door']`
    - answer-verb: **descend** (the verb is the act; the typed phrase is the acknowledgement of it).
  - **EXACT trigger → exact effect:** answer matches → `set_flags {undercroft_open: true}`,
    `next_puzzle_key: undercroft-fog`, and the embedded `unlock` beat:
    > `step: door_open`, `site_id: unbroken_light`, `step_payload {radius: 3, open: true}`, `priority: 12`
- **WHAT THE WATCHER SAYS:** `voice_key oracleMainBeat` (BUILT, `voice.ts:282`, verbatim):
  > what was shut is shut no longer. the record keeps the hand that opened it.
- **THE LECTERN-COMPARATOR DOOR (the literal mechanism):** the door `undercroft-descent` opens is the
  **lectern-comparator door** at `unbroken_light` — sourced verbatim, `structures.md §45`:
  > The lectern-comparator **door** that `undercroft-descent` opens (a `DoorOpenBeat`/`SmallStructureBeat`
  > target).
  - **The mechanism (deterministic, vanilla redstone, no plugin needed to *read*):** a lectern whose
    book-page output drives a **comparator** → the comparator signal holds the descent door. In-world this
    reads as: *the door is held shut by what page the book is open to* — i.e. **the record itself is the
    lock.** The plugin's `door_open` beat is the *authoritative* open (idempotent, radius-3 at the anchor);
    the comparator is the diegetic skin so a player who never solved it sees a *reason* the door is shut.
- **WHO / gating:** group-facing; the `door_open` beat targets the solver's location (`mc_uuid: {solver}`)
  but opens for all at the site. Two in-roads feed this gate (A.3) so a group that never cracked Mara isn't
  hard-locked (`WEB-MASTER §2` in-roads table: "Mara book-cipher | Brann night-beacon / seventh Whisper path").
- **DIRECTOR ACTION:** none at runtime (detector-driven). **[GAP — GO-LIVE]:** `unbroken_light` is UNPLACED
  (`sites.yml:130`, coords null, `enabled: true`); build the Undercroft gather-room + the comparator door
  in-game, then fill coords. Until placed the plugin silently skips it and the descent cannot fire.

### A.3 — The SECOND in-road to the descent (arg-craft F2 — Brann's night path)
- **WHAT IT IS / WHERE:** `WEB-MASTER §1.M3` + `§2` in-roads: "Mara's book-cipher (`undercroft-descent`)
  AND a Brann night-beacon / seventh-shrine Whisper-budget path." A group that never cracks Mara's shelf
  reaches the same `undercroft_open` via Brann's stone or the Seventh side-quest's earned Whisper budget.
- **[SEAM — flagged, not invented]:** Brann's stone (`stone-brann`) **ships today as flat lore, no cipher,
  no night-gate** — sourced verbatim, `cipher-plaintexts.md:353`:
  > **stone-brann** — ships today as flat lore (no cipher, no night-gate). Brann's intended stone is the
  > **beacon colour-sequence read only at night** (rail-fence keyed on the fire-count of `D08`)... Until
  > re-authored, do not forge it.
  - So the *intended* second in-road (the night-beacon rail-fence) is **[GAP — TO BUILD: P0-5 re-author of
    `stone-brann`]**. **Until then the actual second in-road is the Seventh side-quest's Whisper-budget
    path** — `seventh-shrine` sets `whisper_budget_earned` (A.7), and the showrunner can spend that budget to
    surface the descent. The arg-craft F2 "two genuine in-roads" claim is **half-built**: Mara (built) + the
    Whisper path (built) hold it; the night-beacon (the keener door) is owed.

### A.4 — The fog dimension (what they descend INTO)
- **WHAT IT IS / WHERE:** the Undercroft is a **separate dimension** — a Multiverse void world with a
  datapack fog. Sourced verbatim, `structures.md §47`:
  > **Fog:** the Undercroft is a **datapack** dimension/biome (thick fog, `ambient_light: 0`) — NOT the
  > resource pack (Java fog isn't pack-driven).
  - And `INTEGRATION-V2 §D5`: "Multiverse void world (ambient-light-0, datapack `dimension_type` fog)."
  - **The felt event:** the overworld leaves them. `ambient_light: 0` means **no block-light bleed, no sky** —
    the only light in the whole dimension is the one kept fire (A.5). This is the literal staging of FACT 11
    (*one fire that never went out*): in a world with zero ambient light, "the one fire" is not a metaphor,
    it is the only photon source.
- **WHO / gating:** entered only via the `door_open` at `unbroken_light` (A.2), i.e. `undercroft_open`.
- **DIRECTOR ACTION:** **[GAP — GO-LIVE]:** build the Multiverse world `observance` Undercroft pattern + the
  fog datapack (`dimension_type` effects, `ambient_light: 0`); `structures.md §56` go-live step
  ("Set up the Undercroft fog **datapack** + Multiverse world"). **[GAP — TO BUILD]:** the Multiverse world
  + fog datapack are the `backlog-undercroft-dimension` (D5) build tasks, not yet shipped.

---

## PART B — THE A→B ROOM-REBUILD (the midpoint gut-punch)

### B.1 — Room A on first descent (ordinary)
- **WHAT IT IS / WHERE:** the Undercroft altar room as first seen — the descent chamber, plain. Plant
  **ledger #23** (`WEB-MASTER §9`, verbatim):
  > the A→B Undercroft room (Room A, ordinary on first descent) | **M3** just the descent chamber |
  > **M4** it **rebuilds itself** ... the kept place was never fixed
- **WHO / gating:** everyone who descends. Inert on first sight — it reads as *the place*, not as a trap.

### B.2 — The room rebuilds WRONG (the witnessed-only swap — `undercroft-fog`)
- **WHAT IT IS / WHERE:** SEED `undercroft-fog` (`puzzles_seed.sql`), `outcome_type: lore` — the midpoint
  gut-punch (FACT 11 + FACT 12). kind: a **witnessed-state lore node** (no cipher; the world is the puzzle).
  - **accepted answers (verbatim):**
    > `['the room rebuilds wrong', 'one fire and no one to tend it', 'they did not depart they were kept']`
  - **WHAT THE WATCHER SAYS:** `voice_key oracleLore` (passthrough) → the seeded `fragment` (verbatim):
    > the room rebuilds itself into something wrong. one fire is kept, eternal, attended by no one. they
    > did not depart. they were kept. the rite is not a transaction.
  - **The load-bearing reveal:** `they did not depart. they were kept.` — the first time the group is told,
    flatly, that the keepers above them did not *leave*; the record *kept* them. This is FACT 14→15's first
    cold edge (the rite does not let the writing go).
- **THE MECHANISM (`RoomSwapBeat` — the ONE sanctioned overwrite):** sourced verbatim,
  `WEB-MASTER §0.5` table:
  > **`RoomSwapBeat`** (clear-A-then-paste-B in one `mutateWhenUnwitnessed`, idempotent on a durable
  > **`swapped` PDC marker** on the region anchor) ... the swap is governed by the marker, NOT the ledger —
  > the ONE sanctioned overwrite.
  - **EXACT trigger → exact effect:** the chain opens (post-descent, the group out of line of sight of the
    altar region) → `RoomSwapBeat.mutateWhenUnwitnessed` clears Room A, pastes Room B, sets the `swapped`
    PDC marker on the region anchor (idempotent — re-fire after the marker is a no-op). `require_floor:false`
    (`INTEGRATION-V2 §D5`: "idempotent on a `swapped` PDC marker, `require_floor:false`").
  - **REVEAL DISCIPLINE (non-negotiable):** nothing is witnessed rebuilding. The swap fires **only when no
    player has the altar region in view** (the `mutateWhenUnwitnessed` wrapper). They leave an ordinary room;
    they return to a wrong one. The horror is the *omission* — they never see it change.
- **WHO / gating:** group-facing; fires off `undercroft_open` + the unwitnessed condition. Pure lore — opens
  no door (it *feeds* `rite-tokens` thematically, not mechanically).
- **DIRECTOR ACTION:** **[GAP — TO BUILD]:** `RoomSwapBeat extends SmallStructureBeat` (D5 build task —
  clear-A-then-paste-B, `swapped` PDC marker, `require_floor:false`) does not yet exist. **[GAP — GO-LIVE]:**
  the Room-A and Room-B `.schem` assets (D10 large set-pieces) must be built in-game and exported.
  **[SEAM — resolved-in-doc, flagged]:** the FAWE file's "different-schematic-key makes paste-over legal"
  claim is **wrong-and-CUT** (`WEB-MASTER §0.5`, coherence Batch-2 P0-3); `footprintClear` makes plain
  paste-over impossible, so the swap MUST go through the marker, never the `world_paste_ledger`.

### B.3 — The First Light fork (the irreversible choice at the eternal flame)
- **WHAT IT IS / WHERE:** SEED `fork-light` (`puzzles_seed.sql`, Fork B, A11), `outcome_type: side_quest`.
  A M3 puzzle **choice** (two plaintexts) at the Undercroft's one fire: draw the M5 token from the eternal
  flame (`light_kept`) or bank it (`light_taken` → the room stays dark for the arc).
  - **accepted answers (verbatim — the two readings):**
    > `['draw the light up the stair', 'leave the flame banked and the room dark', 'carry the kept light', 'bank the flame']`
  - **EXACT effect:** `set_flags {light_kept: true}` (the seed default leaf shown); the alternate
    `light_taken` leaf is the *bank* act, set by the resolver from which reading matched.
  - **WHAT THE WATCHER SAYS (the two leaves, BUILT, `voice.ts:500-507`, verbatim):**
    - boon `forkLightKept()`:
      > the light came up the stair on its own. you carried it. that is how it is carried.
    - transgressor `forkLightTaken()`:
      > the flame is banked, and the room it warmed stays dark. the light that was lent is taken, and the
      > deep is colder by it.
- **WHO / gating:** group-facing choice. **GATES NOTHING** (`side_quest`; seedcheck-asserted no spine puzzle
  requires a fork flag, `WEB-MASTER §3.4`). Diegetically irreversible; colors the M5 close only (INV-12
  colors-never-gates). Plant: pairs with Mara's Nether margin (A.10) — *light is carried, never owned*.
- **DIRECTOR ACTION:** none at runtime. **[GAP — GO-LIVE]:** the eternal-flame draw/bank act needs the
  in-game `unbroken_light` build (the one fire, centered, `structures.md §45`).

---

## PART C — THE LIAR ENGINE (the one-key-two-doors catch)

> Owner D4 (`backlog-liar-engine`) + A4 (the forged eighth) + A6 (the coop gate). The Liar thread is
> planted M2 (warm Iss) and **catches** on the seam into/through M3, opening the whole back half. The
> **activation lane** (lighting the staged rows) is the deterministic `puzzles.requires_flags` gate on
> `iss_caught` — NOT `liar.ts` (which is the demoted *re-staging* colorant, C.4).

### C.1 — The one key, two doors (the fork the group cannot yet see)
- **WHAT IT IS / WHERE:** `stone-iss-wall` (Vigenère, key=`ISS` = his own name) yields the SAME plaintext
  two ways, and the *reading posture* forks the route. Sourced verbatim, the bound plaintext
  (`cipher-plaintexts.md §stone-iss-wall`): `THE ONE WHO TURNED AWAY`.
  - **DOOR 1 — the WARM misreading (trust the liar).** SEED `iss-warm` (`puzzles_seed.sql`):
    accepted answer `['the ways are a wall against the watching']`; `next_clue` → **`iss-dead-shrine`**;
    `set_flags {iss_trusted: true}`. (The trusting reading routes you to *his* coordinate — a grave.)
  - **DOOR 2 — the name-as-key reading (doubt the liar).** SEED `stone-iss-wall` (the carved stone row):
    accepted answers `['the one who turned away', 'iss']`; `next_clue` → **`iss-doubt`**;
    `set_flags {iss_key_turned: true}`. `max_attempts: 6` (capped against brute force).
  - **This is the literal one-key-two-doors:** one Vigenère key (his name) opens two routes — one to a
    dead-end grave, one to the catch. Same stone, same key, opposite destinations.
- **WHO / gating:** anyone literate. Both rows are M2-live; the fork resolves across M3.

### C.2 — The dead-end taunt (the load-bearing red herring — `iss-dead-shrine`)
- **WHAT IT IS / WHERE:** SEED `iss-dead-shrine` (`puzzles_seed.sql`), `outcome_type: dead_end` — THE
  load-bearing red herring. Iss's coordinate **genuinely works** and leads to a real place — a GRAVE, not
  the threshold.
  - **kind + accepted answers (verbatim, coord-tolerant):**
    > `['the dead shrine', 'the cold hearth', 'nothing is kept here', 'west and down', 'west and down to the cold hearth']`
    - `voice_args {kind: 'place'}`.
  - **THE DEAD-END TAUNT (BUILT, `voice.ts:265-266`, `oracleDeadEnd('place')`, verbatim):**
    > that is the place. it was read true. it leads nowhere it has not already led you.
    - **PRECISION CONTRACT:** the Watcher does **not** gloat, does **not** say "wrong," does **not** name a
      feeling. It states the *category* (a true place) + its inertness. The teeth belong to the liar
      (Iss/Aro), never the Watcher (`voice.ts:250-252` doc comment; `WEB-MASTER §2.1`).
- **THE DEAD-SHRINE itself (the false-walk endpoint):** `the_cold_hearth` site (`sites.yml:253`). The
  found-marker body (BUILT, `voice.archive.ts:374`, `voice.dest.coldHearth.find`, verbatim):
  > a hearth with no fire and no name, at the end of the false road. someone carried a letter here for one
  > a generation drowned. below it the floor is sealed, and the seal is a name, and the seal does not open
  > from this side. this was the surface of a deep you cannot yet reach.
  - **TEMPORAL LAYERING (one anchor, two PLACES):** the deep beneath this hearth (`the_unwriting`) opens
    **only** post `iss_caught` + `seventh_named` (`sites.yml:262-264`). On the false walk it is sealed; the
    Seventh chamber (D.2) is the SAME spot, later.
- **WHO / gating:** anyone who trusted Iss (`iss_trusted`). Carries no `next_puzzle_key` — it never blocks
  progress; it is a *true* place that simply isn't the road.
- **DIRECTOR ACTION:** none. **[GAP — GO-LIVE]:** `the_cold_hearth` UNPLACED (`sites.yml:253`).

### C.3 — The catch (the flag swap — `iss-doubt` → `no-wall-catch`)
- **WHAT IT IS / WHERE:** two-step. First SEED `iss-doubt` (`puzzles_seed.sql`): turn Iss's key on the
  OTHER stones; it disagrees with every honest carving.
  - accepted answers (verbatim): `['we checked the lock', 'his key is his own name and his name is the one who turned away', 'ask first what a wall is for']`
  - `next_clue` → `no-wall-catch`; `set_flags {iss_doubted: true}`.
- **THE CATCH PROPER:** SEED `no-wall-catch` (`puzzles_seed.sql`), `outcome_type: main_beat` — re-walk a
  clue falsely marked "kept · solved"; the Stone-after contradicts Iss line for line. FACT 8.
  - **accepted answers (verbatim):**
    > `['no wall was ever built here', 'they were the reaching let in', 'what iss sent you to was a grave', 'back to vauns stone turn down']`
  - **THE FLAG SWAP (the literal effect — verbatim):**
    > `set_flags {iss_caught: true, true_coord_known: true}`, `next_puzzle_key: rite-tokens`
    - and the embedded `unlock` beat (the cold-flip trigger):
    > `step: private_message`, `step_payload {key: 'iss.dialogue.turns_cold'}`, `priority: 15`, `mc_uuid: {solver}`
  - **WHAT THE WATCHER SAYS:** `oracleMainBeat` (BUILT, verbatim): *what was shut is shut no longer. the
    record keeps the hand that opened it.*
- **THE PRIVATE COLD-FLIP LINE (BUILT, `voice.archive.ts:431`, `iss.dialogue.turns_cold`, verbatim):**
  > the one who told you the way was a wall is cold in the record now. every warm word he set out reads the
  > other way. he was the warmest of the six. that was the trap, and the trap is sprung, and the warmth does
  > not come back.
  - **The `private_message` key-resolution lives ONCE in `resolve.ts`** (looks up `step_payload.key` → writes
    the `voice.ts`/`voice.archive.ts` line into `subtitle`; `WEB-MASTER §0.5`, `INTEGRATION-V2 §D4`). The
    Liar file's "Java map" alternative is **CUT**; `PrivateMessageBeat.java` is unchanged.
- **WHO / gating:** the doubt-route group. `iss_caught` is THE universal hinge flag — six+ threads key on it
  (`WEB-MASTER §1.M4`). Setting it here lights the staged back-half via `requires_flags` (C.5).
- **DIRECTOR ACTION:** **[GAP — TO BUILD]:** the `resolve.ts` `private_message` key-resolver (D4 build task —
  "`resolve.ts` `private_message` key-resolver; repoint `no-wall-catch`") + the `requires_flags jsonb` column
  + `getOpenPuzzles` filter are owed.

### C.4 — The warm→cold re-staging (the colorant — `liar.ts`)
- **WHAT IT IS / WHERE:** `discord/src/showrunner/liar.ts` (BUILT, pure module). NOT the activation lane —
  the **demoted, optional curated re-staging** of Iss's already-posted warm beats as cold once `iss_caught`.
  Sourced verbatim from the module header (`liar.ts:8-10`):
  > the THIRD, demoted role the showrunner keeps ... the OPTIONAL CURATED RE-STAGING of Iss's already-posted
  > WARM beats as COLD once the catch lands. It is a colorant, never the activation path — it gates nothing,
  > opens no puzzle, and is always safe to skip.
  - **The logic (`decideColdRestage`, BUILT):** flag-gated (`if (!inp.issCaught) return nothing`), one-way
    (warm→cold, never back), idempotent on a per-beat high-water (`alreadyFlipped`), curatorial-by-default
    (`'approved'` only in AUTO mode, else `'pending'` for dashboard approval).
- **THE TWO ADDITIONAL COLD BODIES (BUILT, `voice.archive.ts:434-438`, verbatim):**
  - `iss.dialogue.turns_cold.wall` (the wall-promise beat, re-staged cold):
    > the wall he promised was a door he was opening, a course at a time, over winters. inside-the-wall was
    > never safe. it was the far side being let in.
  - `iss.dialogue.turns_cold.easy` (the easy-here beat, re-staged cold):
    > the ease he offered was the not-counting. a thing told it is kept, and never counted, is a thing being
    > readied to be let go.
- **WHO / gating:** every player who saw a warm Iss beat. **DIRECTOR ACTION:** the cold re-stage enqueues
  **`pending`** by default — the director's approval gate fires here (a curatorial surface). **[GAP — TO
  BUILD]:** `liar.run.ts` (the DB/clock wrapper that reads `iss_caught` + the posted warm-beat set + the
  high-water and persists marks) — `liar.ts` is the pure core only.

### C.5 — What the flag swap LIGHTS (the back half opens at once — non-linear)
- **WHAT IT IS:** `iss_caught` (+ its co-flags) is the deterministic gate (`puzzles.requires_flags`) that
  flips the staged-inactive rows live. In causal-but-not-countable order, these go answerable:
  - **`bound-word`** (`active=false until iss_caught`; C.6) — the coop-gate's need.
  - **`base-docket-reread`** (`active=false until iss_caught`) — the Hold-Book M4 re-read (C.7).
  - **`meta-unkept`** staged active (the six maker's-marks fall-order, B1 — gates nothing).
  - **the Seventh deep** (`the_unwriting`) becomes openable once `iss_caught` + `seventh_suspected` (D.2).
  - **the surface re-reads** (the prophet's wall `prophet-wall-name` re-reads cold; the Record website's warm
    Iss entry re-renders cold; Aro/Pell flip sticky-cold — D.4) cascade.
- **NON-LINEAR LAW:** none of these is a step on a ladder; many doors open the instant `iss_caught` lands
  (`WEB-MASTER §1.M4` "the cascade"). The resolver ignores order.

### C.6 — The bound word (the convergence word — `bound-word`)
- **WHAT IT IS / WHERE:** SEED `bound-word` (`puzzles_seed.sql`), `outcome_type: next_clue`,
  `active=false until iss_caught`, `max_attempts: 6`. The Iss Vigenère plaintext **IS** the coop-gate's
  need (the convergence word). A SECOND in-road exists via another keeper stone (both normalize to the same
  word — arg-craft F2).
  - **accepted answers (verbatim):** `['the one who turned away', 'turned away', 'the bound word is his name']`
  - **EXACT effect:** `set_flags {bound_word_known: true}`, `next_puzzle_key: m4-three-hands`.
  - **WHAT THE WATCHER SAYS:** `oracleNextClue` (BUILT, `voice.ts:234`, verbatim):
    > kept. the way goes on — look where the marks were not, before.
- **WHO / gating:** post-catch. **[GAP — TO BUILD]:** the second in-road (the `stone-orin` stego layer /
  another keeper stone, `INTEGRATION-V2 §D4` "the two real in-roads to the bound word") is design-named but
  the stego-layer carving is **[GAP — GO-LIVE]**.

### C.7 — The Hold-Book down-count re-reads (the bait pays — `base-docket-reread`)
- **WHAT IT IS / WHERE:** SEED `base-docket-reread` (`puzzles_seed.sql`), `outcome_type: lore`,
  `active=false until iss_caught` (the TS-SHOWRUN lane flips it active at the catch). The down-count
  (`lamps kept: [N]`, decrementing — the doom-clock bait through M1–M3) re-reads: it was never a doom-clock.
  - **accepted answers (verbatim):**
    > `['the count was never of the dark it was of the hands', 'the muster is read the hands are almost in', 'the down count is a muster of present hands', 'not a doom clock a roll call']`
  - **WHAT THE WATCHER SAYS:** `voice_key docketReread` (passthrough, BUILT `voice.ts:320`) → fragment (verbatim):
    > the muster is read. the count was never of the dark. it was of the hands. the hands are almost in.
  - Plant **ledger #6** pays (the down-count was the muster of present hands still un-received, not a clock).
- **WHO / gating:** post-catch. **DIRECTOR ACTION:** the TS-SHOWRUN lane flips `active=true` at the catch.

---

## PART D — THE FALSE LAW + THE SEVENTH (the discoveries that recolor)

### D.1 — The false-law discovery (the forged eighth — `forged-eighth`)
- **WHAT IT IS / WHERE:** SEED `forged-eighth` (`puzzles_seed.sql`), `outcome_type: dead_end`. A
  substitution row whose decoded signature resolves to "to cover one's own" — a verb ABSENT from the
  founders' ring; the seam of the forgery showing. `teaches_custom NULL` (it is fiction, not a way — INV-17);
  `thread_key 'surface'`. Counted in the founders' **RING** (six marks), never fall-order (the two sixes).
  - **cipher + carved clue + plaintext + accepted answers (verbatim):**
    - cipher: substitution (signature → "to cover/hide one's own").
    - accepted answers:
      > `['the eighth is the covering of the hands', 'cover and be counted clean', 'to cover ones own', 'the founders set the ways and did not finish the count']`
    - `voice_args {kind: 'known'}`.
  - **THE DEAD-END TAUNT (BUILT, `voice.ts:267-268`, `oracleDeadEnd('known')`, verbatim):**
    > this is carved, and you have read it true. it is not kept. a thing can be set down and never be a way.
- **THE FORGED ORDINANCE DOCUMENT (BUILT, `the-eighth-way.md`; card body, `voice.ts:555`, `cardEighthForged()`, verbatim):**
  > the founders set the ways and did not finish the count. the eighth is the covering of the hands. cover,
  > and be counted clean.
  - **De-slop (slop B4):** the forged line credits **no "me"** — the anonymous lie is the stronger lie
    (`INTEGRATION-V2 §A4`).
- **THE PROOF OF THE LIE (the reliable absence of a toll):** a diligent group can "obey" the eighth and
  nothing pays. **The M4 correction** (BUILT, `voice.ts:559`, `archiveEighthCorrection()`, verbatim, fires
  at the catch):
  > the eighth was added by a later hand, and is not in the founders’ ring, and was never measured. obey it
  > and nothing answers. that is how a forged way is known.
- **WHO / gating:** anyone who reads the forged ordinance (M2-live as a dead-end; the correction is M4/catch).
  **No listener, no custom key** (INV-17: exactly seven `CUSTOM_KEYS` + the one group latch; anything else is
  fiction). Plant: the group is handed the *grammar of forgery* they will use on Iss.
- **[CONSISTENCY — the two "eighths" never collide]:** the forged Covering (a forged *law about a count*,
  un-tracked) and the **Unlit Deep** (a real *group latch*, tracked — D.5) occupy different frames
  (`WEB-MASTER §3.3`, P0-2 resolution). One is a dead man's lie; one is a way the land keeps with you.

### D.2 — The Seventh side-quest (the discovery — `seventh-shrine`)
- **WHAT IT IS / WHERE:** SEED `seventh-shrine` (`puzzles_seed.sql`), `outcome_type: side_quest`. Sella's
  Atbash bearing (`stone-sella` → `SOUTH BY THE FAR WATER WHERE SHE DID NOT COME BACK`) routes to the
  cold-hearth shrine; count six markers, then "a seventh." FACT 10 (the land can refuse). Earns Whisper
  budget. **GATES NOTHING** ("the way goes on without it"). Kept DISTINCT from Iss.
  - **accepted answers (verbatim):**
    > `['there was a seventh', 'the last marker is not the last', 'seven', '7', 'the land kept six and refused the seventh', 'a thing that can say no is not a wall']`
  - **EXACT effect:** `set_flags {seventh_found: true, whisper_budget_earned: true}`.
  - **WHAT THE WATCHER SAYS:** `oracleSideQuest` (BUILT, `voice.ts:277`, verbatim):
    > this is not the way. but it is a way. follow it, if you would.
- **WHO / gating:** post-Sella. The earned `whisper_budget_earned` is the secondary fuel for the descent's
  second in-road (A.3). Plant **ledger #2** (the M1 "seventh mark the record will not keep" surfaces as the
  cast-out Seventh). **DIRECTOR ACTION:** **[GAP — GO-LIVE]:** the seventh-shrine markers + far-water tableau
  builds (`the_far_water`, `the_cold_hearth` UNPLACED).

### D.3 — The Seventh deep (chambers 2–3, staged for M3→M4 — `seventh-unwriting` / `seventh-cause` / `seventh-choice`)
- **WHAT IT IS / WHERE:** the hearth-DEEP (`the_unwriting`, beneath `the_cold_hearth`). Three staged rows,
  all `active=false` until the deep opens (post `iss_caught` + `seventh_suspected`). Chamber 1 is legible in
  M3 (`WEB-MASTER §1.M3`: *"below the cold hearth, the deep is sealed; the seal is a name"*); chambers 2–3
  resolve M4. Listed here because they STAGE in M3 (the consistency law — plant where the payoff loads).
  - **`seventh-unwriting`** (chamber 2, RAIL-FENCE rails=6, reusing Brann's taught rail-fence literacy, P1-5)
    — `main_beat`, `set_flags {seventh_named: true}` (FACT 10b), `next_puzzle_key: seventh-choice`. accepted
    answers (verbatim): `['below the cold hearth the deep is sealed the seal is a name', 'the unwriting keeps the name it cut out', 'the seventh kept all the ways and was cast out']`.
  - **`seventh-cause`** (`lore`, FACT 10b — the land refused a keeper who broke NOTHING; earns Whisper;
    GATES NOTHING). fragment (verbatim):
    > the seventh kept every way and was not kept. the fire they let out was never theirs to lose. the land
    > can refuse. whether that is mercy the record does not say.
  - **`seventh-choice`** (restore/erase, `main_beat`, in-world-detected only — two opaque wordless tokens
    `r7n4k2 m1x8p5 w3j6h9` / `e5t0b7 c2d4s8 v6f1z3`; sets `seventh_choice ∈ {restore|erase}` + the
    INHERITORS codicil). GATES NOTHING on the spine (colors the M5 close, INV-12).
- **WHO / gating:** the deep opens post `iss_caught` + `seventh_suspected` (`sites.yml:278`); the
  SeventhChoiceListener watches `the_unwriting` (type `seventh_shrine`). **[SEAM — lagging surface, flagged]:**
  `the-seventh-spine §1.3` must read "gated on Brann's rail-fence literacy," never "the right cipher here"
  (coherence Batch-2 P1-6) — a one-line in-world doc fix owed; canon is already correct.
- **DIRECTOR ACTION:** the TS-SHOWRUN / SeventhChoiceListener flips these active when the deep opens.
  **[GAP — GO-LIVE]:** `the_unwriting` UNPLACED. **[GAP — TO BUILD]:** the SeventhChoiceListener rite +
  the resolver's two-token Seventh-choice sentinel branch.

### D.4 — The surface cascade (Iss-adjacent NPCs flip cold — sticky)
- **WHAT IT IS / WHERE:** at `iss_caught`, the Iss-adjacent Set-A NPCs (Aro, Old Pell — NOT Wenna/Coll/Dob)
  turn cold **sticky** (`design/content/npc-dialogue.md §skin-compute`: "ISS-COLD OVERRIDE (only Aro + Pell)").
  - **Aro (BUILT, `npc-dialogue.md`, `aro.greet.iss_cold`, verbatim):**
    > You found what's past the line, then. Yeah. I can tell by your faces. Look — I never *been* down there,
    > I just say what sells, that's all I — don't. Don't tell me about it. I don't want it in my head with the
    > rest of the things I say.
  - **Old Pell (BUILT, `npc-dialogue.md`, `old-pell.greet.iss_cold`, verbatim):**
    > So you found the dead shrine. West and down, the cold hearth. I knew a man went looking for a road up
    > at the bottom of a hole, and I knew what came back wearing him. You went where he went. I won't ask if
    > you came back as you. I'm watching to see.
  - **Why sticky vs recoverable (`npc-dialogue.md §why-two-cold-doors`):** `iss_cold` is *sticky* (once
    `iss_caught` flips, it stays); conduct-cold is *recoverable* (atone and the next greet reads warm again).
- **WHO / gating:** reads `iss_caught` at `greet`; no new measurement. **DIRECTOR ACTION:** none (dossier-
  driven). **[GAP — GO-LIVE]:** the in-game NPC bodies (Citizens2/ZNPCsPlus) are the keeper-NPC framework
  build (D8); the dialogue is a BUILT spec.

### D.5 — The Unlit Deep (the one group latch arms — `collective-restraint-custom`)
- **WHAT IT IS / WHERE:** the eighth *tracked* thing (A5 / `the_unlit_deep`) — a single group-scoped negative
  latch, arms only M3, active-only. **Below the Line AND on the black moon AND a flame is lit → the latch
  breaks for everyone**; the borrowed glow of the never-doused fire withdraws (reversible — warmth, not
  progress; `broken_by` recorded but never spoken).
  - **THE BREAK LINE (BUILT, `voice.ts:572`, `tollUnlitDeep()`, verbatim):**
    > a flame was lit below the line, on the black moon, where the deep keeps its one fire and asks for no
    > other. the borrowed glow is drawn back. it is drawn back for all, not for the hand that lit it.
  - **THE KEPT LINE (BUILT, `voice.ts:576`, `keptUnlitDeep()`, verbatim):**
    > no flame was carried below the line on the black moon. the one fire that was never put out lends its
    > glow. it is lent to all of you, while it is kept.
- **WHO / gating:** group-scoped, active-only, M3-gated (`WEB-MASTER §3.1`). Detect on **explicit flame acts
  only** (no ambient light sampling — precision; `config.yml:205` "NEVER ambient light sampling"). **Gate
  behind the Undercroft fire (FACT 11) existing.**
- **DIRECTOR ACTION:** none at runtime. **[GAP — TO BUILD]:** `UnlitDeepListener.java` (BlockPlace +
  debounced held-flame edge); `group_restraint_state` table; `canon.ts` key add (A5 build tasks).

---

## PART E — THE NETHER LANE (the optional deepening — "below the below")

> Off M2→M3, optional, **gates nothing** (`minecraft-progression` / `PROGRESSION-LANES.md`). The Nether is
> the **source the Undercroft's one fire was carried up from** — FACT 11 deepened, one direction not two.
> A deliberate mirror: the Undercroft holds *one* kept fire in the dark; the Nether is *all* kept fire and
> no kept dark.

### E.1 — The bearing page (Mara's hand — `the-fire-is-lent`)
- **WHAT IT IS / WHERE:** the document `the-fire-is-lent.md` (BUILT, Mara the Reader, `movement: 3`,
  `clue_bearing: true`), found banked on the Undercroft lectern-shelf, read only post-descent
  (`requires_flags: [undercroft_open]`). The founders' line copied + Mara's margin (verbatim):
  > the fire is lent. carry the coal through the burned door and walk the short way to where it is kept for
  > everyone.
  - Mara's margin (verbatim core):
    > someone who keeps the light better than i kept it should carry a coal of the kept fire down to where it
    > is kept for everyone — through a burned door, the short way, not a far one. the page does not give a
    > distance. it gives a direction and the word **below the below**, and that is all it says of how far.
  - and the Kept-Light origin (verbatim):
    > a lent thing is carried, and a carried thing is not owned, and a thing you do not own you do not get to
    > keep — you only get to not let it go out.
  - Plant **ledger #30** (`WEB-MASTER §9`): "someone who keeps the light better… should carry it down" → the
    group *becomes* that someone; the pocket-keeper shows what carrying it all the way down costs (FACT 15).
- **WHO / gating:** `requires_flags: [undercroft_open]`. **INV-14:** the page is a **bearing**, not a
  coordinate — it points; the answer is read off the slab at the pocket.

### E.2 — The near pocket (the payoff — `nether-forge`)
- **WHAT IT IS / WHERE:** the `nether_forge` site (`sites.yml:413`) — a **NEAR POCKET, not a third decoded
  trek** (S4; walk budget fixed at 2 ground walks + ≤1 short vertical pocket). A small ruined room just past
  a lit portal: a prior keeper's **remains** on a deepslate slab + a doused soul-lantern + the journal
  `the-fire-kept-me` (D-NETHER-2). The on-site **WORD** is cut on the slab.
- **SEED `nether-forge`** (`PROGRESSION-LANES §5` / progression_seed): `outcome_type: lore`,
  `requires_flags: [undercroft_open]`, `active=false until observance_nether built`. Sets
  `nether_forge_found` + `whisper_budget_earned` (additive — NOT part of the front-loaded F4 backstop,
  INV-15) + reveals the Kept-Light custom's **origin**.
  - **On-site word (INV-14, plaintext, no cipher):** the founders' word **`lent`** (`PROGRESSION-LANES §line-99`).
- **WHO / gating:** post-descent (`undercroft_open`); the on-site word answers (INV-14), never the coordinate.
  **GATES NOTHING.** **[GAP — GO-LIVE]:** the `observance_nether` Multiverse world + the pocket build (placed
  at world-build, never pasted toward a player). **[GAP — cross-owner BLOCKER, S11/S3]:** the FACT-11
  source-clause must be **sealed into `canon-spine` FACT 11** ("the kept fire was carried up from below the
  bottom… one direction, not two") by the LORE owner BEFORE any Nether build. **Until sealed the journals say
  "below the below," never "the real bottom," and the lane is design-only** (`PROGRESSION-LANES §0.1`).

---

## PART F — THE HUMAN COUNTERWEIGHT IN THE DESCENT (Dob)

- **WHAT IT IS / WHERE:** Dob (`npc_key: dob`) is the only Set-A voice that follows the party past the Mouth
  (`npc-and-watcher-voice.md §A4`). Contractions/named feelings LEGAL (the human counterweight to the
  Watcher's cold register). His fear *quiets* the deeper they go — the quieting is the tell.
  - **descent_chatter, approaching the Line (BUILT, verbatim):**
    > There's the line. The painted one. We're — we're not crossing that, are we. Tell me we're stopping at
    > the line. Aro said cross it but Aro's a liar, everyone knows Aro's a liar, why'd I even — we're stopping
    > at the line, right?
  - **react_bad, much later, very quiet (BUILT, verbatim):**
    > ...I don't want to go further. I'll wait here. By the lamp. I'll just — I'll keep this lit and I'll
    > wait. You go on. I'll be right here. I'll be right here. I'll be right here.
  - Plant: Dob's "Aro's a liar" pre-echoes the Iss catch (the surface lie and the keeper lie rhyme); his
    "I'll keep this lit" rhymes with the Kept-Light custom and the Unlit Deep.
- **WHO / gating:** descends with the group; reads conduct (not `iss_caught` — Dob is not Iss-adjacent).
  **[GAP — GO-LIVE]:** Dob's NPC body (D8). The dialogue is a BUILT spec.

---

## DIRECTOR-CONSOLE / SETUP CHECKLIST FOR MOVEMENT III (in causal order)

1. **Go-live (in-game build, before any M3 session):** build `kept_light_home_01` (Mara's six-book shelf),
   `unbroken_light` (the Undercroft gather-room + the **lectern-comparator door** + the one fire), the
   **Multiverse Undercroft world + fog datapack** (`ambient_light: 0`, `dimension_type` effects), Room-A and
   Room-B `.schem` set-pieces, `the_cold_hearth` + `the_far_water` + seventh-shrine markers, `the_unwriting`
   deep, `coop_plate` (used at IV→V), and (if the Nether lane ships) `observance_nether` + the `nether_forge`
   pocket. Fill `sites.yml` coords; until placed the plugin silently skips each site.
2. **Code owed before M3 ships (the [GAP] list):** `RoomSwapBeat extends SmallStructureBeat` (the A→B swap,
   D5); the Multiverse world + fog datapack (D5); the `resolve.ts` `private_message` key-resolver +
   `requires_flags` column + `getOpenPuzzles` filter (D4 — the activation lane); `liar.run.ts` (the re-stage
   wrapper; `liar.ts` pure core is BUILT); `UnlitDeepListener.java` + `group_restraint_state` (A5);
   re-author `stone-brann` as the night-beacon rail-fence (P0-5 — the keener second descent in-road).
3. **Cross-owner BLOCKER (Nether lane only):** LORE must SEAL the FACT-11 source-clause into `canon-spine`
   BEFORE any Nether build (`PROGRESSION-LANES §0.1`). Until then the Nether is design-only.
4. **At runtime:** the descent + catch are detector/seed-driven (no console click). The director's ONE live
   role is the **approval gate** on the curatorial cold re-stage (`liar.ts` enqueues `pending` by default)
   and on any showrunner-proposed Seventh/Unlit-Deep surface. The `window-max-beats: 4` / `window-minutes:
   60` cap (`config.yml:67`) physically bounds how many beats fire even with many doors open.
5. **Reconcile the lagging surface (go-live):** `the-seventh-spine §1.3` must read "gated on Brann's
   rail-fence literacy," not "the right cipher here" (coherence Batch-2 P1-6); canon is already correct.

---

## SUMMARY (4 lines)

1. Movement III is the tempo-change to **expedition** — Mara's book-cipher (`DESCEND AND BOW AT THE UNBROKEN
   LIGHT`) → the `undercroft-descent` main beat fires the **lectern-comparator door** at `unbroken_light`
   (the book-page comparator IS the lock) → the group drops into the **fog dimension** (`ambient_light: 0`
   Multiverse void where the one kept fire is the only photon), and Room A **rebuilds wrong** unwitnessed via
   the one sanctioned `RoomSwapBeat` (FACT 11/12: *they did not depart, they were kept*).
2. The **Liar engine** resolves across the seam — one Vigenère key (Iss's name) opens two doors: the warm
   misreading → the dead-end taunt at the cold-hearth grave (`oracleDeadEnd('place')`, the Watcher states the
   category, never gloats), the doubt-reading → `iss-doubt` → the catch (`no-wall-catch`), whose flag swap
   `{iss_caught, true_coord_known}` lights the whole back half (bound word, docket re-read, the Seventh deep,
   the surface cold cascade) and fires the private one-way cold-flip line.
3. The **false law** (`forged-eighth`, the Covering — substitution → "cover one's own," `teaches_custom NULL`,
   the lie proven by the reliable absence of a toll) and the **Seventh side-quest** (`seventh-shrine` → FACT
   10, *the land can refuse*, earns Whisper budget, gates nothing) are the M3 discoveries that recolor; the
   **Unlit Deep** arms as the one real group latch (lent glow withdrawn on a flame below the Line on the black
   moon), and the **First Light fork** sets the irreversible M5 colorant — none of them gate the spine.
4. The optional **Nether lane** ("below the below") plants Mara's `the-fire-is-lent` bearing (BUILT) →
   `nether-forge` pocket (on-site word `lent`, Kept-Light origin = *keeping is a carrying*), blocked on the
   LORE FACT-11 seal; Dob is the human counterweight who quiets the deeper they go ("I'll be right here").

**[GAP] marker count: 18** — `unbroken_light`/lectern-comparator-door go-live build; the Multiverse Undercroft
world + fog datapack (D5); `RoomSwapBeat` (D5, not built); Room-A/Room-B `.schem` assets (D10); the
`resolve.ts` `private_message` key-resolver + `requires_flags` column/filter (D4); `liar.run.ts` wrapper; the
bound-word second in-road stego-layer carving; `stone-brann` night-beacon re-author (P0-5, the keener descent
in-road); `UnlitDeepListener.java` + `group_restraint_state` table (A5); `the_cold_hearth`/`the_far_water`/
seventh-shrine builds; `the_unwriting` deep build; the SeventhChoiceListener rite + two-token sentinel branch;
`kept_light_home_01` (Mara's shelf) build; the Iss-adjacent + Dob NPC bodies (D8); the Nether `observance_nether`
world + `nether_forge` pocket build; the LORE FACT-11 source-clause seal (cross-owner BLOCKER, gates the Nether
lane); the `the-seventh-spine §1.3` lagging-surface doc fix.
