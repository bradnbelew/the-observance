# The Observance — BUILD-MANIFEST (partitioned worklist for Lore + Build)

> The file-level work partition for the Lore + Build phases. **Organized so no two workers ever touch
> the same file.** Each row: the file to CREATE or EDIT, the owning lane, and what goes in it. Authority
> for *what* each change contains is `INTEGRATION-V2.md`; authority for *how it all fits* is `WEB-MASTER.md`.
>
> **Lanes** (a lane = one worker, owns its files exclusively):
> - **LORE** — corpus prose (found-documents, journals, framing, canon-spine edits, voice line *text*).
> - **TS-FORGE** — `discord/src/forge/*` (ciphers, runes, clue-specs, templates, canon keys).
> - **TS-SHOWRUN** — `discord/src/showrunner/*` + `discord/src/oracle/*` + `discord/src/db/repo.ts`.
> - **TS-VOICE** — `discord/src/voice.ts` + `discord/src/voice.archive.ts` (the ONLY writers of these two).
> - **SQL** — `discord/supabase/migrations/*` + `discord/supabase/seeds/*`.
> - **PLUGIN** — `plugin/src/main/java/...` + `plugin/src/main/resources/*.yml`.
> - **DASH** — `dashboard/src/*` (route, projection, author panels).
> - **WEB-REC** — the Record website + stego (spans DASH route + TS-FORGE template; see §coordination).
>
> **Coordination seams (the few files multiple lanes need — serialized, never parallel):**
> `voice.ts`/`voice.archive.ts` = **TS-VOICE owns exclusively**; every other lane requests keys via a
> hand-off list, never edits these directly. `canon-spine.md` = **LORE owns**; the namespace block
> (`WEB-MASTER §0`) is applied by LORE in ONE commit before anyone reads new FACTs. `puzzles_seed.sql` =
> **SQL owns**; lanes request rows via a hand-off list. `resolve.ts` = **TS-SHOWRUN owns**; the fate
> sentinel + coop AND-join + dead-end `kind` arg all land here in one serialized pass.

---

## 0. PRECONDITION COMMITS (land FIRST, in this order, before any lane forks)

| # | File | Lane | Content |
|---|---|---|---|
| 0.1 | `arc/lore/canon-spine.md` | LORE | Apply the **frozen namespace** (`WEB-MASTER §0`): FACTs 16/10b/2b/7b/13b; INV 11–17; the two-sixes note; the shared-anchor table; demote `divergent-fates` FACT-16 to a FACT-10 child. |
| 0.2 | `discord/src/oracle/resolve.ts` + seed | TS-SHOWRUN + SQL | Reconcile **`iss_caught`** as the single canonical post-catch flag; retire `_known`/`_found`; document readers. |
| 0.3 | `discord/src/showrunner/decide.ts` | TS-SHOWRUN | Wire the **drip-ordering guard** (never opens with `dead_end`; respects outcome semantics) + `decide.selftest.ts`. |
| 0.4 | `discord/src/showrunner/` (new) | TS-SHOWRUN | Create the **M5 ending composer** skeleton (reads colorant flags, emits bounded close) before any colorant idea writes a clause. |
| 0.5 | `discord/src/forge/cipher-web.md` note (or `design/cipher-web.md`) | LORE | Document the **single Iss IV→V chain** (catch → bound word → coop gate → coordinate → walk → Accepting) before `stone_iss` is re-carved by three ideas. |
| 0.6 | `arc/lore/canon-spine.md` §7 | LORE | Apply the **backlog namespace** (`WEB-MASTER §0.2`): INV-18 (single-arbiter slot), INV-19 (Accepting active-only quorum), INV-20 (UnlockBeat step↔payload); note "INV namespace is synthesis-owned." |
| 0.7 | `arc/lore/canon-spine.md` §3b + LORE-BIBLE | LORE | Add **FACT 17** (the word-axis, child of FACT 1) with its ≥2 doors; record in the reveal-order table. |
| 0.8 | `discord/supabase/migrations/*` + `discord/src/db/repo.ts` | SQL + TS-SHOWRUN | **`puzzles.requires_flags jsonb`** + `getOpenPuzzles` filter (the activation lane, arg-craft F1); repoint `no-wall-catch` to set `iss_caught` and STOP. **Precondition for the entire back half.** |
| 0.9 | `discord/src/showrunner/run.ts` (`readActiveRoster`) | TS-SHOWRUN | The single **`readActiveRoster(windowMs)`** source consumed by the Accepting bridge, spawn-bias conductor, and keeper-NPC. **Precondition for the climax + all ambient apparitions.** |
| 0.10 | `discord/src/oracle/resolve.ts` | TS-SHOWRUN | One **`private_message` key-resolver** (look up `step_payload.key` → `subtitle`); `PrivateMessageBeat.java` unchanged. |
| 0.11 | `design/WEB-MASTER.md §9` | (synthesis, DONE) | **Ledger freeze** for the two folded ideas: cursed-map = rows 24–27, progression = 28–31; no idea file self-numbers (S1). **Applied this pass.** |
| 0.12 | `arc/lore/canon-spine.md` FACT 11 + `design/structures.md` | LORE | **Seal the FACT-11 source clause** (*"the kept fire was carried up from below the bottom; the Undercroft is the bottom of the Hold, the deep-fire its source — one direction, not two"*) BEFORE any Nether build (S11). **Precondition for the A15 Nether lane.** |

---

## 1. LORE LANE (corpus prose + canon edits)

| File | C/E | Content |
|---|---|---|
| `arc/lore/canon-spine.md` | E | §0.1 namespace; plus FACT path-edits per idea (FACT 6 forks clause, FACT 1 Hold-Book note, FACT 9 three mechanics). |
| `arc/lore/documents/the-fire-they-let-out.md` (D-new) | C | The Seventh cause-fragment (effaced hand) → FACT 10b; correlates with D11. |
| `arc/lore/documents/the-eighth-way.md` | C | The forged ordinance (anonymous lie — no "me", slop B4); substitution signature → "cover/hide one's own". |
| `arc/lore/documents/no-wall-was-ever-built-here.md` | E | One record line tying the forged eighth into the M4 correction; confirm 1:1 map to the prophet-wall rungs. |
| `arc/lore/documents/the-ways-are-a-wall.md` | E | One later-hand margin line foreshadowing the catch (prophet wall textual source). |
| `arc/lore/documents/` future-margin (grave) | C | Founder margin *"we cut the names before the keeping… the stone is ready before the keeper is."* (FACT 13b plant). |
| `arc/lore/documents/` place-filing fragment | C | Archivist water-damaged fragment: *"the list is not only of names. against each name, a ground."* (FACT 16; slop B1 — comma-fragment capped). |
| `arc/lore/documents/` Record-elsewhere fragment | C | Founder line *"the record is kept in more than one place, against the loss of the first."* (the website plant). |
| `arc/lore/documents/` difficulty fragment | C | Mara's hand (bookCipher) *"the record keeps a closer count of the quick… i will not say what i call it."* (FACT 2b; slop A2 fixed). |
| `arc/lore/documents/` herd fragments ×2 | C | Prior-keeper "nine grey, one white" count (bookCipher) + late margin "they were grey when i shut the door". |
| `arc/lore/documents/` Unlit Deep seeds | E/C | R01 eighth-line "kept by no hand… the deep keeps one. let it."; Brann torches-and-counting margin; M4 passive-voice "were kept the better for it" report. |
| `arc/lore/documents/` offline-skin | E | One marginal pointer in Brann's `do-not-close-your-eyes-here.md` (keep ambiguous); the M1 offline-player report text. |
| `arc/lore/documents/` coop-gate | C | Mara page-nine fragment *"the cold square… i typed into the dark"* + the M4 arming report. |
| `arc/corpus/cipher-plaintexts.md` | E | Six per-stone **framing** edits adding the UNKEPT maker's-mark glyphs (free prose, X1-safe, fall-order). |
| `arc/corpus/journals-*.md` | E | Vaun fork-gated extra node text; Mara `light_taken` re-read page-refs; keeper **grammatical fingerprints** held (slop B3 / WEB-MASTER §6). |
| `design/cipher-web.md`, `design/clue-web.md` | E | Document the single Iss chain; add `meta-unkept`, the prophet wall, the a1z26 literacy rung, the promoted `pressure-glyph-walk`, the kind-switched dead-ends, the second herring. |
| `arc/lore/LORE-BIBLE.md` | E | §6 FACT-web audit: add the lit-marker cross-surface seed; close TODO-3 (Hold-Book = FACT 9 home); **add FACT 17 to the reveal-order table with its ≥2 doors**; re-run after D01 budget. |
| `arc/lore/documents/` FACT-17 word-clause | C | The M2 Archivist fragment authored ONCE as place+word: *"against each name, a ground; against each ground, what was said over it."* (FACT 16 + FACT 17 share it, BP2-3.) |
| **Backlog de-slop fixes (slop Section E)** | E | Lowercase the four `backlog-bestiary-spawn-bias` Watcher `#the-record` lines (E1) + the two `backlog-keeper-stone-expeditions` Watcher/lore lines (E2); strike the named feeling in `keeper.atone.cleared` (E3); cut the "you are learning it now" bow in `keeper.fact9.named` (E4). |
| `design/the-seventh-spine` / `keeper-stone-expeditions` one-liners | E | Confirm seventh-spine §1.3 reads "gated on Brann's railFence literacy" (BP1-6); expeditions §5 names **fall-order** for UNKEPT, not "Rosetta sunwise" (BP2-5). |
| **— Two folded ideas (A14 cursed-map / A15 progression) —** | | |
| `arc/lore/canon-spine.md` FACT 11 + `design/structures.md` | E | **Seal the FACT-11 source clause** (0.12, S11 BLOCKER): *"the kept fire was carried up from below the bottom; the Undercroft is the bottom of the Hold, the deep-fire its source — one direction, not two."* No Nether build before this lands. |
| `arc/lore/documents/the-fire-is-lent.md` (D-NETHER-1) | C | Mara's hand (bearing-page, `clue_bearing:true`, M3, `requires_flags:[undercroft_open]`). **Authored in the SAME pass as the cursed-map `m.kept` provenance line** (S3 — one Mara author; she cites/defers, never acts). Points at the Nether pocket via INV-14 on-site word, never a decoded coordinate (S4). |
| `arc/lore/documents/the-fire-kept-me.md` (D-NETHER-2) | C | The pocket source-keeper's hand, decaying structurally (M3-4, `clue_bearing:false`). Anchors **soul sand = deep-time, "older than the first keeper"** (S5 — distinct from the Pale herd). Embodies FACT 15, never narrates it. |
| `arc/lore/documents/the-name-i-cut-myself.md` (D-END-1) | C | The Seventh's hand, from outside the record (M4, `clue_bearing:false`). Pairs with D11; ends on a withheld mercy ("you only came to look"). |
| `arc/lore/documents/the-seventh-not-kept.md` (D11) | — | **No edit** — already the End lane's plant; the End is its spatial payoff. |
| `arc/lore/documents/` cursed-map lure-page texts | C | Mara's `m.kept` provenance line (deferred/citeable, S3); the README "lie-that-is-true" (*"it does not connect to anything"*); the map-description entry; the vignette closing-page pointer. All cold Archivist/Mara register; the page IS `/record` (no marketing, no nav, no CTA). |
| `arc/lore/documents/the-copy-i-kept.md` (Mara) | C (OPTIONAL/cuttable) | A four-line Mara margin that *is* the lure-page provenance (un-orphans the uploader fully). Cuttable — the page can cite `kept-in-more-than-one-place` inline instead. |
| Brann's stone framing (M2 plant) | E | The Nether-lane plant line in Brann's carved *framing* (NOT a bound plaintext — does not touch X1): *"the fire we keep is not ours. it is lent… below the below."* (Brann's doubling fingerprint.) |
| `arc/lore/documents/` unwriting-wall extra line | E | The End way-out **pointer**: one extra effaced line on the `the_unwriting` chamber-2 wall, legible only at `seventh_named` — a reveal on the existing surface, NO new puzzle node (S9). |
| `arc/lore/LORE-BIBLE.md` | E | Confirm the two lanes + the on-ramp add **no new FACT/INV**; record the FACT-11 source-clause seal in the §6 FACT-web audit; note the End has no apparition lane (R3). |
| `design/cipher-web.md` | E | Add the three folded gates: map literacy (`the-record-keeps` recognition token), the Nether bearing (INV-14 on-site word), the End way-out (reveal + `end-seventh-out` row). Confirm walk-budget = 2 ground + ≤1 vertical pocket (S4). |
| **Hand-off to TS-VOICE** | — | The *text* of every new voice key (the de-slopped exemplars in `INTEGRATION-V2`), so TS-VOICE inserts verbatim. |

---

## 2. TS-FORGE LANE (`discord/src/forge/*`)

| File | C/E | Content |
|---|---|---|
| `discord/src/forge/canon.ts` | E | Add `'the_unlit_deep'` to `CUSTOM_KEYS` (forces the namespace guard to thread `TrackerConfig` + `voice.ts`). |
| `discord/src/forge/clue-specs.ts` | E | `NON_CIPHER_KEYS` entry for `meta-unkept` (P8 acrostic, plain lore) — keeps `specsCoverageSelfTest` green; **DO NOT touch X1/CLUE_SPECS**. |
| `discord/src/forge/templates/sigil.ts` (or sibling `stego.ts`) | C/E | `embedRuneLayer(svg, payload)` visual second-rune-layer (Iss card only); **round-trip self-test** that must NOT break the X1 plaintext guard. |
| `discord/src/forge/runes.ts` | E (if needed) | Add the prologue marker glyph + the UNKEPT glyphs to the taught/rendered set (resource-pack source of truth). |

---

## 3. TS-SHOWRUN LANE (`discord/src/showrunner/*`, `oracle/*`, `db/repo.ts`)

| File | C/E | Content |
|---|---|---|
| `discord/src/showrunner/fate.ts` + `fate.selftest.ts` | C | `decideFate()` pure module (INV-11 inputs, active-only enum, bond excluded). |
| `discord/src/showrunner/` M5 composer | C/E | The bounded-close composer (§0.4) reads `ending_fate`, `seventh_choice`, fork flags, `ending_codicil`, difficulty re-quote; fixed priority. |
| `discord/src/showrunner/keeper-record.ts` + `.run.ts` + `.selftest.ts` | C | Hold-Book: `decideKeeperEnrolment` (precision floor); `count_source` muster resolution; idempotent high-water. |
| `discord/src/showrunner/reckoning.ts` + `.selftest.ts` | C | Difficulty (FACT 2b); injected constants; **grep-guard: no `whisper_budgets`** (INV-15). |
| `discord/src/showrunner/grave.ts` | C | Grave producer (grounded active name, date = single Accepting instant, two-row A+B transaction, idempotent). |
| `discord/src/showrunner/name-where-never-been.ts` | C | The carve selector (group-avoided ∩ T-never-visited proof-of-absence). |
| `discord/src/showrunner/decide.ts` | E | Drip-ordering guard (0.3); `dripIntervalMs *= reckon().cadenceMult`; `no curatorial drip before prologue_ignited`; carries `tone`. |
| `discord/src/showrunner/apply.ts` | E | `tone` into voice calls; flip staged `dead_end` `active`; gated one-shot `recordOpened` ack; herd `paleTarget` pass enqueue; stego branch on Iss drip. |
| `discord/src/showrunner/snapshot.ts` | E | Muster count; distinct-solver/first-try/whisper-lean; `paleTarget` lookup; `prologueIgnited`. |
| `discord/src/showrunner/state.ts` + `types.ts` | E | `ReckoningState`/`Tone`; `reckoning_state`/`reckoning_since_ms`; fate/codicil/seventh/fork flag types. |
| `discord/src/showrunner/run.ts` | E | Wire `runKeeperRecordPass`, `runReckoning`, `fate-watch` (REFUSERS non-event), grave/herd passes beside `runCustomsPass`. |
| `discord/src/oracle/resolve.ts` | E (serialized) | Fate sentinel branch (set-once `ending_fate`/`ending_codicil`); coop-gate **AND-join** (single writer); dead-end `kind` arg pass-through; Seventh-choice sentinel; **`private_message` key-resolver** (0.10); **`requires_flags` open-check** in the match step (0.8). |
| `discord/src/db/repo.ts` | E | `readSignalProjection()`; `readGateLegs`/`writeGateLeg`; muster read; **`getOpenPuzzles` joins `arc_state.flags` and filters `requires_flags` rows** (the activation lane, 0.8); **`readActiveRoster(windowMs)`** (0.9). |
| `discord/src/showrunner/run.ts` (the authoring loop) | C/E | **THE SHOWRUNNER keystone (`backlog-full-showrunner` / D1):** the daily authoring loop — drips, fair gifts, personalized reports (scalpel + **deterministic fallback** `voice.reportObserved/Escalated`), stone/Whisper tuning, AUTO↔CONFIRM split; owns `readActiveRoster`; wires the grave/herd/keeper/reckoning passes. |
| `discord/src/showrunner/liar.ts` | C | **Optional curated re-staging ONLY** of Iss's warm beats as cold (NOT the activation path — that is the `requires_flags` gate, D4/BP1-1). CONFIRM `pending`. |
| `discord/src/showrunner/conductor.ts` (spawn-bias) | C | **`backlog-bestiary-spawn-bias` / D7:** reads `SignalSnapshot` + `readActiveRoster`, runs `selectApparition` (**probabilistic + capped**, never deterministic), publishes **`apparitionClaim`** (INV-18). All other apparition lanes defer to it. |
| `discord/src/showrunner/keeper.ts` + `keeper.run.ts` | C | **`backlog-keeper-npc-framework` / D8:** the NPC dialogue resolver (reads the dossier; `fact9.named` names the logged M-I beat). **Distinct from `keeper-record.ts`; NOT `reckoning.ts`.** FACT-9 = one surface per player per window. |
| `discord/src/showrunner/ear.ts` + `voice_watchlist` derive | C (P3) | **`backlog-modeled-mob-and-voice` / D11:** the Ear's relevance pass; `voice_watchlist` **derives from** the keeper roster + bound plaintexts (self-tested ⊆, never hand-kept). Defers to `apparitionClaim`. |

---

## 4. TS-VOICE LANE (`discord/src/voice.ts` + `voice.archive.ts` — EXCLUSIVE)

Insert verbatim from LORE's hand-off (all de-slopped per `INTEGRATION-V2`). New keys, grouped:
- **Hold-Book:** `keeperPage*` family, `docketReread`, `docketEven`, `keeperEnrolled`.
- **Fates/composer:** `fateKept`, `fateCastOut`, `fateDivided`, `fateRefusers`, `fateInheritorsCodicil`.
- **Seventh:** `keeperCloseSeventhRestored`, `keeperCloseSeventhErased`.
- **Forks:** sacred-beast-broken report, undercroft-doused, kept/taken Keeper lines.
- **Grave:** `graveCarved`, `graveOpened`, `graveReceipt`.
- **Difficulty:** `tone` arg on `drip`/`oracleDeadEnd`/`reportObserved`; `deepTightens`, `deepIsPatient`.
- **Dead-ends:** `oracleDeadEnd(kind)` family (`name|count|place|known|prophet`) — recommend single-fn `kind` arg.
- **Coop:** `oracleThreeHands` (slop A4 fixed: "the count is three. the threshold is open.").
- **Coords:** `voice.dest.coordFraming.false`, `coordReCarve`, `coldHearth.find`, `threshold.arrive`.
- **Name-where:** the place-filing clueDrip + Keeper half-veiled line.
- **Record website:** `recordElsewhere`, `recordReceives`.
- **Meta / forged / Unlit / offline:** `oracleMetaUnkept`; `cardEighthForged`, `archiveEighthCorrection`;
  `tollUnlitDeep`, `keptUnlitDeep`, `CUSTOM_PHRASES.the_unlit_deep`; the M4 FACT-9 offline line + whisper deferral.
- **Backlog — Keeper-NPC (D8):** `keeper.*` dialogue tree nodes; `keeper.fact9.named` (de-slopped, no bow — E4);
  `keeper.atone.cleared` (de-slopped, no named feeling — E3); the presiding-Keeper M-IV atonement lines.
- **Backlog — the Ear (D11, P3):** `keeperWhisper*` (FACT 17 relevance); the pack-sound fallback line.
- **Backlog — Iss cold-flip:** `iss.dialogue.turns_cold` (resolved by `resolve.ts` 0.10, not the Java beat).
- **Cursed-map (A14):** `recordFrameBreak` (ONE key; the **count-callback default body** only — *"six were
  kept before you… hands are kept"*; the map-conduct overload is **CUT**, S5). Reuses `recordOpened`,
  `recordElsewhere`, `recordReceives` (A13, no new key).
- **Progression (A15):** `nether.soulSand`, `nether.forgeArrive` (the Kept-Light-origin lore),
  `end.shrineArrive`, `end.outsideRecord`; the M5 composer clauses `fateCastOutEndRead` /
  `fateRefusersEndRead` (§3e — appended **only** when the fate fired AND `seventh_seen_out`; they **replace**
  the neutral fate clause so the ≤2-clause cap holds, not an extra clause). Reuse `fateRefusers` (the "so did
  one before you" re-read). Brann's Nether-framing line is corpus carving, **no voice key**.

> All must pass `registerDisciplineSelfTest` (the cross-surface normalizer): lowercase, no caps/exclaim/meta-word.

---

## 5. SQL LANE (`migrations/*`, `seeds/*`)

| File | C/E | Content |
|---|---|---|
| `discord/supabase/migrations/0006_*.sql` | C | `arc_state.ending_fate text`, `ending_codicil boolean`; `coop_gate_legs`; `group_restraint_state`; `player_visited_cells`; `keeper_record jsonb` on `showrunner_state`; `grave_state`; the single Accepting-instant column; `reckoning_*` (jsonb settings, may need no migration); **`puzzles.requires_flags jsonb`** (the activation lane, 0.8); **`world_paste_ledger`** `(id, world, site_id, schematic, base_x/y/z, pasted_at)` UNIQUE `(world, site_id, schematic, base_x, base_y, base_z)` (single-paste idempotency ONLY — the swap rides the PDC `swapped` marker, BP0-3); **`voice_watchlist`** (derived/self-tested, the Ear). |
| `discord/supabase/seeds/puzzles_seed.sql` | E | Rows: `a1z26` teaching-rung; `meta-unkept`; `seventh-unwriting`/`seventh-cause`/`seventh-choice`; `m4-three-hands` + two bound-word in-roads; two coord-walk rows (answers = destination words); `base-docket-reread`; `fork-light`/`fork-name`; `record-url`; `prophet-wall-comfort`/`prophet-wall-name`; the forged-eighth substitution row; re-point 3 dead-ends to `kind`s; promote `pressure-glyph-walk` (`side_quest`, `next_puzzle_key: accepting-crouch`); difficulty Mara bookCipher lore row + `min_state` tags; cap brute-forceable short answers (`max_attempts`). |
| `discord/supabase/seeds/thread_tags.sql` | E | Tag new nodes (`the_unlit_deep` → `happened`; forged eighth → `surface`; etc.). |
| `discord/supabase/seeds/thread_cards.sql` | E | Cards: forged-eighth surface, three-hands, fate, record-url (each `body_voice_key` defined). |
| `discord/supabase/seeds/side_quests.sql` | E | Seventh shrine carving text; (the `dark_shrine` row is **removed/merged** into the Seventh — P1-2). |
| `discord/src/oracle/seedcheck.ts` | E | **NEW assertions:** no spine puzzle `requires` a fork flag (INV-12); **every `active=false`/`requires_flags` row is named by exactly one activation rule and no `active=true` row is reachable only through a staged predecessor** (arg-craft F1 reachability); **every `UnlockBeat` `step` names a registered beat with a contract-matching payload** (INV-20). |
| `discord/supabase/seeds/puzzles_seed.sql` (back-half) | E | **Activation lane (arg-craft F1/D4):** set `requires_flags` on `bound-word`, `m4-three-hands`, `threshold-coordinate`, `true-walk-arrive`, `seventh-unwriting`, `seventh-cause`, `seventh-choice`, `base-docket-reread`, `meta-unkept`; repoint `no-wall-catch` (set `iss_caught`, no rite shortcut); seed the **real bound-word in-road B** (`stone-orin` stego layer + Iss card stego key, arg-craft F2). |
| `discord/supabase/seeds/side_quests.sql` (cross-wire) | E | **Arg-craft F3:** 3–4 leaves set existing flags as optional in-roads (`dest-far-water` → `seventh_suspected`; `dest-markers-row` → the `meta-unkept` key); `dest-dead-shrine` fires a catch re-read beat. Invariant preserved (removing all 18 still reconstructs the spine). |
| **— Progression (A15) —** | | |
| `discord/supabase/migrations/000X_*.sql` | C/E | `arc_state` flags **`nether_forge_found`** (bool; the proposed `FateInput.netherForgeFound`, **not** wired into `decideFate` until §8 ratified, S9) + **`seventh_seen_out`** (bool; M5-composer + `seventh_choice`-context ONLY, **never** a fate input, S2). Both gate nothing (INV-12). |
| `discord/supabase/seeds/puzzles_seed.sql` (lanes) | E | `nether-forge` (`lore`, sets `nether_forge_found` + Whisper budget, answer = on-site word, `requires_flags:[undercroft_open]`, `active:false` until the Nether world placed); `end-seventh-out` (the **arrival payoff** at `end_seventh_shrine`, `lore`, sets `seventh_seen_out`, `requires_flags:[seventh_named]`, `active:false` until the End world placed). The End way-out **pointer** is a reveal on the existing `seventh-unwriting` surface — **no row** (S9). |
| `discord/supabase/seeds/seventh_seed.sql` (breadth) | E | `dest-deep-forge` (thread `who`, entry `nether-forge`); `dest-out-of-record` (thread `who`, entry `end-seventh-out`). `gates_progress` false. Mirrors `dest-unwriting-deep`/`dest-fire-let-out`. |
| `discord/supabase/seeds/thread_cards.sql` | E | One rumor card per destination (both under `who`) → flips `verified` on arrival; each `body_voice_key` defined. |
| `discord/src/oracle/seedcheck.ts` | E | **`siteCoverageSelfTest` extension (R7):** a cross-dimension coord/destination row must NOT seed OPEN unless its `site_id` resolves to a placed + enabled site **in the named world** (`observance_nether`/`observance_end` must exist first). |
| **— Cursed-map (A14) —** | | |
| `discord/supabase/seeds/*` | E | **NONE required** for the frame-break (the `recordFrameBreak()` ack-path wiring needs no row; reuses `prologue_ignited` + the `acked` one-shot). IF emitted via a beat row instead of the ack path: ONE `puzzles` row keyed off `prologue_ignited` (`outcome_type:'lore'`, `voice_key:'recordFrameBreak'`, no door, gates nothing). **No new tables, no new flags** (the optional `from_map` flag is P2, S7). |

---

## 6. PLUGIN LANE (`plugin/src/main/java/...`, `resources/*.yml`)

| File | C/E | Content |
|---|---|---|
| `signal/listener/UnlitDeepListener.java` | C | Group latch (BlockPlace + debounced held-flame edge); main-thread; writes `group_restraint_state` via bridge. |
| `signal/listener/SeventhChoiceListener.java` | C | Rite-detect sibling; `seventh_named`-gated; sentinel → `seventh_choice`; one-site break-whitelist at `the_unwriting`; idempotent. |
| `signal/listener/CoopPlateListener.java` | C | Plate-held window; writes world legs; posts opaque conjunction token. |
| `signal/listener/IgnitionListener.java` | C | Flip `prologue_ignited` on lectern read OR `#the-record` human post. |
| `signal/listener/DeathListener.java` | E | Sacred-beast fork (last-tagged **glowing** beast kill → `sacred_beast_broken`, first-writer-wins); `paleCosmeticKey` ignored for conduct (INV-13 precision guard). |
| **offline-skin join hook** | C/E | The idea assumed `PresenceListener` (**does not exist** — `TerritoryListener` is the location source). Add a `PlayerJoinEvent`/`PlayerQuitEvent` hook (new small `PresenceListener.java` OR extend an existing listener) → `despawnApparitionsWearing(uuid)`. |
| `beats/lib/NamedMobBeat.java` | E | `skin_player` + `offline_only` payload; `applyWornSkin` (cache-first, silhouette fallback); re-check at fire; **un-targeted** retreating variant for the Seventh glimpse. |
| `beats/lib/SacredAnimalBeat.java` | E | `mode:"spread"` + `target` + `pale_cosmetic` PDC (distinct from `sacred_beast`, **never glowing**, cap 16, one-per-pass, unwitnessed, babies not auto-pale); mark last tagged beast fork-arming; collective-gaze facing pass. |
| `beats/lib/SmallStructureBeat.java` | E (reuse) | 3 Seventh `.schem` chambers + the grave mound (inline cells); reveal-disciplined. **Add the FAWE `schematic` payload branch** (`backlog-fawe-large-setpieces` / D10): load cached `.schem`, `ClipboardHolder.createPaste` inside `mutateWhenUnwitnessed`, footprint-pre-check `clipboard.getDimensions()`, `ignoreAirBlocks(true)`+`copyEntities(false)`, protect the region, tag in `world_paste_ledger`. **Single-paste onto a CLEAR footprint only.** |
| `beats/lib/RoomSwapBeat.java` | C | **`backlog-undercroft-dimension` / D5; the ONE A→B overwrite (BP0-3).** `extends SmallStructureBeat`: clear-A-then-paste-B in one `mutateWhenUnwitnessed`, `require_floor:false`, idempotent on a durable **`swapped` PDC marker** on the region anchor (NOT the ledger). |
| `beats/lib/RevealBeat.java` | C | **`backlog-unlockbeat-producers` / D6:** block-state/marker flips + slot-lighting, **no FAWE**, trivially idempotent. The M5 rite ships as `reveal` until the FAWE branch lands. (The producer boundary, BP2-1.) |
| `beats/lib/KeeperNpcBeat.java` | C | **`backlog-keeper-npc-framework` / D8:** `NPCRightClickEvent` → JSON-chat dialogue tree reading the Supabase dossier; M-IV atonement node; the apparitions **defer to `apparitionClaim`** (INV-18). Citizens2 (presiding) + ZNPCsPlus (per-player). |
| `beats/lib/ModeledMobBeat.java` | C (P2) | **`backlog-modeled-mob-and-voice` / D11:** near-clone of `NamedMobBeat` that attaches a ModelEngine R4 rig after spawn, **degrading to exactly `NamedMobBeat`** if the plugin is absent/throws. |
| `signal/listener/VoiceListener.java` + `beats/lib/SpatialVoiceBeat.java` | C (P3) | **The Ear (`backlog-modeled-mob-and-voice` / D11):** `MicrophonePacketEvent` → async Whisper STT → neutral dossier signals (FACT 17); `SpatialVoiceBeat` authored `.ogg`/TTS to one player, **falls back to `PrivateSoundBeat observance:keeper_voice`**; defers to `apparitionClaim`. |
| `signal/listener/AcceptingRiteListener.java` | E | **`backlog-accepting-sentinel-bridge` / D9:** `effectiveQuorum = min(configQuorum, activeRosterSize)` (INV-19, active-only); consume `readActiveRoster`; opaque plugin-posted sentinel inherits the `solves` replay-guard. |
| `pack/ResourcePackPusher.java` | C | `PlayerJoinEvent` → `Player#setResourcePack(url, sha1, force, prompt)`; log `ResourcePackStatusEvent` to the dashboard health panel. (Path-A one-click pack; the rune font + mono `.ogg`s.) |
| `beats/lib/SignWriteBeat.java`, `LecternFillBeat.java` | E (reuse) | Re-carve `stone_iss`; grave headstone + rewrite; Hold-Book growing pages; name-where carves; destination leaves. |
| `signal/TrackerConfig.java` | E | `CUSTOM_UNLIT_DEEP`; unlit-deep config (flame-materials, cooldown, reuse `deepLineY`/moon phases); `pale_cosmetic` key; per-player visited-cell mark from `LocationSampler`. |
| `resources/sites.yml` | E | Add `the_unwriting`, `coop_plate`, `first_marker_01`, `carve_anchor` candidates, `herd_anchor`, optional `grave_spur`; confirm `the_cold_hearth` chamber-3 anchor; **remove `dark_shrine`** (merged); the **a1z26 teaching-rung** anchor. **A15 (progression):** add `nether_forge` (`type:structure`, `world:"observance_nether"`, `enabled:true`, near-pocket keeper-grave anchor), `end_seventh_shrine` (`world:"observance_end"`, `enabled:true`, re-dressed end-ship / pre-gen island), `end_exile_hold` (`world:"observance_end"`, **`enabled:false`** until the INV-16-bound binding is built, S10). Placeholder/null coords until GO-LIVE; per-site `world:` already exists. |
| **Multiverse worlds (A15)** | C | `observance_nether` + `observance_end` (the Undercroft Multiverse pattern, `backlog-undercroft-dimension`). **No second bespoke fog-dimension** (vanilla Nether/End need only re-dressing — R8). The End shrine re-dresses an *already-generated* end-ship OR a *world-build pre-generated* island (reveal-safe, S6); the Nether keeper-remains is **placed at world-build**, never pasted toward a player. |
| `beats/lib/` producer rules (A15) | E (reuse) | End/Nether re-dressing of *occupied* vanilla = **additive pastes onto verified-clear adjacent air** via `SmallStructureBeat` on a clear sub-region (soul-lanterns/lecterns/carving-slabs beside vanilla blocks) OR `RevealBeat` flips. **NEVER** an occupied overwrite; **NEVER** `RoomSwapBeat` (Undercroft-only); bound to `world_paste_ledger` single-paste (S7). P2 Nether basalt-glimpse reuses `NamedMobBeat` re-skinned + **defers to `apparitionClaim`** (INV-18). The End has **no apparition lane** (R3). |
| `signal/listener/` (A15) | E (reuse) | **No new listener** — `nether_forge_found`/`seventh_seen_out` are set by the existing `AnswerSignListener` on-site word path, gated by the world the site is in (`LocationSampler` already keys the heatmap on `worldName` — zero new tracking infra). |
| `resources/plugin.yml` | E | Register the new listeners. |
| `resources/config.yml` | E | `tracker.forbidden-words` (the arc Unspoken); `rites.accepting.quorum` = cast size; unlit-deep + pale-spread + difficulty-state config knobs. |
| `beats/.../schematics/` | C | `seventh_hearth_01.schem`, `seventh_unwriting_02.schem`, `seventh_deep_03.schem` (deploy assets, go-live). |

---

## 7. DASH + WEB-REC LANE (`dashboard/src/*`)

| File | C/E | Content |
|---|---|---|
| `dashboard/src/app/record/[slug]/page.tsx` | C | Server component, `robots: noindex`, **no client JS / no polling** (static-per-build, anti-jank §2b). |
| `dashboard/src/lib/record-projection.ts` | C | Pure, spoiler-free-gated projection (`arc_state.flags` + per-stone solves → legible/redacted). |
| `dashboard/src/components/author/AcceptingTrigger.tsx` | E | Fate preview (author-only) + manual fate override **through the approval gate**. |
| `dashboard/src/` health panel (status) | E | Show `reckoning_state` so spoiler-free Ethan confirms a slow drip is intentional (difficulty R5). |
| **— Cursed-map (A14) —** | | |
| `dashboard/src/app/record/[slug]/page.tsx` | E (re-scope, S4) | **The route is slug-AGNOSTIC today** (`grep slug` → 0 — no `params`, no slug branch; it is NOT "reads `v_record` without validating"). Add `{ params }`; render the BASE archive for bare `/record` + the existing slug; the **downloads block ONLY for `the-record-keeps`** (static `kept: 6` + one recovered-file entry + struck-7); an **in-voice 404** for any other slug. Preserve `noindex`, no client JS, the `v_record`-only read. Still a server component (the `6` is static, no live counter — S/§2c). |
| `dashboard/src/lib/record-projection.ts` | — | **No widening** — the downloads block reads no new data (the `6` is authored, not a metric). |
| `public/the-hold/` (the vignette asset) | C | The downloadable datapack/world `.zip` (prefer a **datapack** — no world file to browse, S2f). Vanilla structures + command-blocks + ONE datapack function carrying **only** FACT-1/2 strings + the server address + the rune string. **No engine, no Supabase, no keeper name past the Archivist, no cipher key, no server flag.** Scaled: 1-room ships first → full 6-room hold is P1→P2. Not committed to the plugin/engine tree. |
| Arrival wiring (A14) | E | Route the vignette hand-off into the BUILT `messageCreate` ignition detector (`prologue.ts` — the closing page tells the group to post one plain word in `#the-record`; **no new detector code**). Emit `recordFrameBreak()` **once** on the measured signal (count-callback) via the BUILT ack path, idempotent on the `acked` guard. Default: fires for everyone (the bare detector can't distinguish map-arrivals — S7); the `from_map` flag is **P2/optional**. |

---

## 8. SPECSCHECK / GUARD FLAGS (verify after each lane lands — do not let drift fail the camera)

- **X1 plaintext round-trip** (`specs.selftest.ts`): re-run after the UNKEPT framing edits (LORE) and the
  stego layer (TS-FORGE). Both must leave bound plaintexts untouched.
- **`registerDisciplineSelfTest`** (cross-surface normalizer): re-run after every TS-VOICE insert.
- **`customKeyNamespaceSelfTest`**: `the_unlit_deep` must appear in `canon.ts` + `TrackerConfig.java` + `voice.ts`.
- **`threadTagSelfTest` / `threadCardVoiceCoverageSelfTest`**: every new puzzle row + card.
- **`noLeakedSentinelSelfTest`**: `seventh-choice`, `m4-three-hands`, Accepting sentinels stay opaque.
- **`seedcheck` NEW**: no spine puzzle requires a fork flag.
- **`specsCoverageSelfTest`**: `meta-unkept` classified in `NON_CIPHER_KEYS`.
- **`activationReachabilitySelfTest` NEW (arg-craft F1)**: every staged/`requires_flags` row has exactly one
  activation rule; no `active=true` row is reachable only through a staged predecessor. **This is the static
  test that catches the dark-back-half bug.**
- **`unlockStepContractSelfTest` NEW (INV-20)**: every `UnlockBeat` `step` names a registered beat + matching payload.
- **`watchlistSubsetSelfTest` NEW (BP2-2)**: `voice_watchlist ⊆ {decoded keeper names + authored custom names}`.
- **A→B swap idempotency**: assert the swap is guarded by the **`swapped` PDC marker only**, never also a
  `world_paste_ledger` row (BP0-3/BP2-4 — no double-guard).

---

## 9. SLICE vs SPINE vs DEPTH (build order across lanes — backlog + new-ARG interleaved)

> The backlog is the load-bearing structure; the new-ARG threads cannot be tested before the engine pieces
> they ride exist. Build order interleaves them.

- **P0 (vertical slice — validate "will they notice at all" first):**
  **Backlog engine:** `ResourcePackPusher` + the one-click pack; the FAWE `schematic` branch in
  `SmallStructureBeat`; the **`requires_flags` activation lane** (0.8) + reachability self-test; `RevealBeat` +
  the first `UnlockBeat` producer rows; the first oracle loop seeded; the Watcher vanilla apparition.
  **New-ARG:** Cold-Start Prologue; Hold-Book M1 plant + M3 two-keeper move; single Sacred Beast + Fork A;
  the a1z26 literacy in-road; the six-stone field (D2); both Rosettas (D3); the Accepting + its two in-roads.
  (One grave + one fate read are P0-cuttable for the slice.)
  **Cursed-map (A14) — the on-ramp + YouTube cold-open (P0 surface, P1 prop):** the lure page (`/record/
  the-record-keeps` re-scope + downloads block + static `6`); `recordFrameBreak` (count-callback) + arrival
  wiring into the BUILT detector. The full 6-room vignette is P1→P2 (1-room ships first). **Gates nothing** —
  a cold-joining group gets the in-server Prologue unchanged.
- **P1 (arc-spine — makes M5 worth filming):**
  **Backlog engine (the keystones):** the **showrunner authoring loop** (D1) + `readActiveRoster` (0.9); the
  **Liar engine** activation + `private_message` resolver (D4); the **Undercroft** + `RoomSwapBeat` (D5); the
  **spawn-bias conductor** + `apparitionClaim` (D7, INV-18); the **Keeper-NPC framework** (D8); the
  **Accepting sentinel bridge** + INV-19 quorum (D9); the full FAWE set-pieces (D10).
  **New-ARG:** Seventh choice + tint; divergent fates; the unified Hold-Book full; the Unlit Deep; the coop
  gate; the coord expedition; name-where; the grave; difficulty; forks A+B; herd conversion; Record website + stego.
  **Progression (A15) — the two cheap P1 lane cores** (after the FACT-11 seal, 0.12): the **Nether
  near-pocket keeper-grave** (`nether-forge` → `nether_forge_found` + Whisper + Kept-Light origin) and the
  **End Seventh shrine** (`end-seventh-out` → `seventh_seen_out`); each one set-piece + one journal/carving +
  one flag, riding built machinery; the M5 composer reads both colorant flags. `netherForgeFound` into
  `decideFate` waits on §8 ratification (S9).
- **P2 / P3 (depth — after the slice + spine are green):** UNKEPT meta; the prophet's wall; the offline-skin
  apparition; Fork C; the Seventh chambers 2–3 full vertical; the encoded-timestamp clock; the a1z26 herd
  prediction stone; the Iss-page-keyed-to-reader Hold-Book tier.
  **Two folded ideas (depth):** the full 6-room cursed-map vignette + the `from_map` flag + the conduct-FB
  overload (CUT, S5) (A14); the Nether bearing-**trek** (behind a playtest, S4), the end-city
  `cast_out`/`refusers` **place** binding (`end_exile_hold`, INV-16-bound or cut, S10), and the Nether
  **intimate apparition** beats (basalt glimpse / soul-fire bank — defer to `apparitionClaim`) (A15). **Backlog garnish:** `ModeledMobBeat` (P2,
  ModelEngine R4); **the Ear** `VoiceListener`/`SpatialVoiceBeat` + FACT 17 full payoff (P3, Simple Voice
  Chat) — FACT 17 *plants* at P2 and degrades to a pack-sound whisper if the voice layer never installs.
