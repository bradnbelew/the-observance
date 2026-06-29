# Coherence Critique — cross-idea + canon orphan/contradiction audit (BATCH 2: the backlog layer)

> LENS: coherence. Scope read this pass: **all 28 files in `design/ideas/*.md`** (the 18
> prior treatments + the 10 new `backlog-*` reconciliation specs), `arc/lore/canon-spine.md`,
> `arc/lore/LORE-BIBLE.md`, `design/COHERENCE-AUDIT.md`.
>
> **What changed since the last coherence pass.** The prior audit (preserved below as
> "APPENDIX — BATCH 1") judged 18 treatments *before integration*. Its resolutions have
> since LANDED IN CANON: the spine now carries FACT 16/10b/2b/7b/13b (§3b), INV-11..17 (§7),
> and the §8 namespace anchors (the two sixes, the single Accepting instant, the closed
> custom set). The reconciled idea files now *reference* those as frozen and explicitly say
> "do not re-mint" (`name-where-never-been`, `divergent-fates-endings`,
> `dynamic-diegetic-difficulty`, `some-laws-are-lies`, `coords-to-real-place`,
> `herd-conversion`, `collective-restraint-custom`). **Batch-1 P0-1..P0-4 / P1-x are
> RESOLVED in canon.** I re-verified each (see §RESOLVED) and found no regression.
>
> **The new danger is identical in SHAPE to Batch-1 and fresh in INSTANCE.** The 10
> `backlog-*` files are disciplined per-file and each claims to "honor what compiles," but
> they were authored as independent reconciliation specs and they re-open the SAME five
> shared namespaces the last pass closed — only this time on the *engine/wiring* layer:
> canon INV numbers, the A→B swap idempotency model, the `iss_caught` swap lane, the M5
> ending/quorum surface, and the FACT integer space. **Root A (namespace self-assignment)
> has recurred verbatim.** Severity-ranked below; each finding ends with the single coherent
> resolution.

---

## SEVERITY P0 — contradictions that break canon or a built seam if integrated as written

### P0-1 — Canon INV namespace is self-minted AGAIN: `INV-ACC-ACTIVE`, `INV-18`, `INV-UNLOCK` (three backlog files), exact recurrence of Batch-1 ROOT A
The spine froze the invariants at a **numbered sequence INV-11..17** (`canon-spine §7`), with §8 anchors. Three backlog files now each mint a new canon invariant *independently*, by a *different naming scheme*, and instruct authors to "add it to canon-spine":
- `backlog-accepting-sentinel-bridge.md` (§4, §7.1): **`INV-ACC-ACTIVE`** — "the Accepting's satisfiability quorum is computed over ACTIVE players only."
- `backlog-bestiary-spawn-bias.md` (§4): **`INV-18`** — "the apparition slot is single-arbiter (≤1 ambient apparition per drama window)."
- `backlog-unlockbeat-producers.md` (§4.1): **`INV-UNLOCK`** — "every `step` value must name a registered beat AND a payload matching its contract."

Two of the three (`INV-ACC-ACTIVE`, `INV-18`) are *real and needed* invariants that canon currently lacks — but minted with **collision-prone identifiers**: `INV-18` is the next free integer, and a future file (see P0-2) also reaches for "the next free integer," so two files both want 18. The mixed scheme (`INV-18` vs `INV-ACC-ACTIVE` vs `INV-UNLOCK`) means a reader cannot tell the canon set's size or order. This is precisely the "rule 11 ×3" defect the last pass flagged (Batch-1 P2-2 / ROOT A), recurred on a new batch that never saw that ruling.

**Resolution:** **No backlog file assigns a canon INV number or name.** Synthesis assigns INV numbers in one pass after the backlog set is frozen, continuing the integer sequence. Concretely: collect the three proposed invariants and number them **INV-18 (single-arbiter apparition slot), INV-19 (Accepting quorum is active-only), INV-20 (UnlockBeat step↔payload contract)** — or fold INV-ACC-ACTIVE into INV-11's "active players only" family as **INV-11b** since it is the same active-only principle applied to a second selector input. Whichever scheme, one owner mints them. Note in `canon-spine §7` that the invariant namespace is synthesis-owned, mirroring the §3b FACT-namespace note that already exists. (INV-UNLOCK is also a build-time self-test, not only prose — keep the self-test; just don't let the file name the canon line.)

### P0-2 — `FACT 17` is self-minted by `backlog-modeled-mob-and-voice.md`, re-opening the FACT integer namespace the spine sealed at §3b
`backlog-modeled-mob-and-voice.md §4` mints **`FACT 17` — "the record files what is *said* of the ways, not only what is done"** (the Ear / keyword-spotter), a child of FACT 1. It *does* hedge ("confirm in the synthesis namespace pass before canonizing; do not self-mint elsewhere") — but it then **uses FACT 17 as a settled identifier throughout** (§4 plant, §5 payoff, §6 table), which is exactly the self-mint it warns against. Canon's §3b namespace ends at the frozen set {16, 10b, 2b, 7b, 13b}; the spine already records that "no other file may self-mint these integers" and demoted an earlier divergent-reading "FACT 16" for the same reason. FACT 17 is a genuine, well-formed new fact (a third filing axis: name / place / **word**) and it is sealed-load-bearing (it deepens FACT 1→15) — but it must enter through synthesis, not through a P3 garnish file.

**Resolution:** Synthesis decides in one pass whether the "filed by word" fact earns a top-level integer (**FACT 17**) or becomes a child sub-fact (**FACT 1c**, alongside FACT 16's place-axis as FACT 1's name/place/word triad). Given it has a real M2→V plant/payoff and feeds FACT 15, a top-level **FACT 17** is defensible — but assigned by the namespace owner, recorded in `canon-spine §3b`, and added to the LORE-BIBLE reveal-order table with its ≥2 doors (the keeper-whisper + the M-II Archivist "what was said over it" fragment). Until then the backlog file should say "a proposed new fact (synthesis to number)," not "FACT 17."

### P0-3 — The A→B "room rebuilds itself" swap has TWO incompatible idempotency models across two backlog files
This is the sharpest *mechanical* contradiction in the batch — same set-piece, two different anti-jank designs:
- `backlog-fawe-large-setpieces.md` (GAP A, §4) makes the swap legal **by keying the durable paste-ledger on `(world, schematic, base-cell)`**: a *different* schematic at the same base has a different ledger key, so "the swap uses a *different* `schematic` at the same site, so its ledger key differs and it is allowed; a re-fire of the *same* schematic is blocked." Idempotency = "this exact schematic has been pasted at this base." It does **not** clear Room A first — it relies on `ignoreAirBlocks`/paste-over.
- `backlog-undercroft-dimension.md` (§2.1, §4) finds the *opposite* problem in the *same* code: `SmallStructureBeat.footprintClear()` requires the box to be **replaceable/air before paste**, so Room B **cannot paste over Room A's occupied footprint** — it `skipped("footprint-occupied")`. Its fix is a NEW `RoomSwapBeat` (or `swap` mode) that **clears A to air, then pastes B**, with idempotency moved to a **`swapped` PDC marker on the region anchor**, NOT the ledger.

So one file says the swap is already legal via a schematic-keyed ledger and a plain second paste; the other proves a plain second paste is *impossible* against the built `footprintClear` and demands a clear-then-paste with a *different* idempotency key (PDC `swapped` marker, not the ledger). If both integrate, the engine has two competing idempotency authorities for the one swap, and the FAWE file's "plain paste-over is allowed" claim is **false against the very `footprintClear` it elsewhere praises** — the Undercroft file is correct on the code.

**Resolution:** adopt the Undercroft file's **`RoomSwapBeat` clear-A-then-paste-B**, idempotent on a durable `swapped` marker, as the ONE swap path; demote the FAWE file's "different schematic key makes paste-over legal" to wrong-and-cut. Then reconcile the ledger: the `world_paste_ledger` (GAP A) governs **single-paste idempotency** for non-swap set-pieces (stones, cairns, the unlit deep), and its key becomes `(world, site_id, schematic, base)` for those; the A→B swap is governed by the `swapped` PDC marker and is the ONE sanctioned overwrite. Both files must state: *paste-over is impossible against `footprintClear`; the swap is the only mutate-occupied path; its idempotency is the marker, not the ledger.* This also fixes the FAWE file's own §1-table claim that the IV "room rebuilds itself" rides GAP A's ledger — it does not; it rides the swap marker.

---

## SEVERITY P1 — cross-backlog collisions and canon tensions that misread or double-build if unreconciled

### P1-1 — The `iss_caught` swap lane is designed THREE ways across three files (deterministic gate vs. showrunner flip vs. resolver node-swap)
The single most-claimed seam in the batch. Three files each own "what happens at `iss_caught`":
- `backlog-liar-engine.md` (§2 R1, §4) **recommends a deterministic in-resolver gate**: add `puzzles.requires_flag`, set `base-docket-reread.requires_flag='iss_caught', active=true`, and `getOpenPuzzles` treats `requires_flag` as part of "open" — collapsing the swap lane, the AUTO duplicate, and the showrunner flip into ONE predicate, *no showrunner SPOF*.
- `backlog-full-showrunner.md` (§1 U4) **keeps the showrunner swap lane**: a `liar.ts` producer reads `iss_caught` and emits a row-flip beat (CONFIRM `pending`), with an authored duplicate row as the AUTO/asleep fallback.
- `backlog-keeper-npc-framework.md` (§4, §5) adds a THIRD mechanism: the **resolver swaps Iss's NPC node-text cold on `iss_caught`** ("the resolver swapping Iss's node text, not by any live AI").

These are not all contradictory (node-text-swap is a different surface from row-activation), but the **base-docket-reread activation** is claimed by both the Liar file (deterministic `requires_flag`) and the showrunner file (showrunner-flipped `active`). They cannot both own the activation: a deterministic `requires_flag` row needs no showrunner flip, and a showrunner-flipped row should not also carry `requires_flag` (it would gate twice). COHERENCE-AUDIT F5 already flagged that `iss_caught` had *zero* readers and a name drift (`_known` vs `_found`); now it has *many* readers across the batch, so the drift must be fixed before any of them wires.

**Resolution:** adopt the Liar file's **deterministic `requires_flag` gate** as the single activation authority for `base-docket-reread` (it removes the showrunner SPOF on the signature move — the strongest reason). Demote `backlog-full-showrunner.md` U4 from "the swap lane" to "**optional curated re-staging** of Iss's *warm* beats as cold," explicitly NOT the activation path. The keeper-NPC node-text swap stays (different surface) but reads the SAME `iss_caught` flag. **Precondition:** fix the F5 flag-name drift to one canonical `iss_caught` (+ pick `true_coord_known` or `_found`, once) before any of the three wires — exactly Batch-1 ROOT C, still unresolved at the seed/sealed layer.

### P1-2 — The `private_message` empty-beat drift is the same defect described THREE times with three fix locations
`PrivateMessageBeat` reads `title|subtitle|actionbar|text` but the seed passes `step_payload:{key:'iss.dialogue.turns_cold'}`, so the cold-Iss line `skipped("empty")`. Three files independently "discover" and fix it:
- `backlog-liar-engine.md` §2 R2: add a key-resolution layer in `PrivateMessageBeat.java` (a `DialogueLines` map or read `voice.ts`).
- `backlog-unlockbeat-producers.md` §4.6: resolve `step_payload.key` in **`resolve.ts`** (write the looked-up line into `subtitle` before enqueue).
- `backlog-full-showrunner.md` implies it via the `iss.dialogue.turns_cold` authored line.

The fix locations **differ** (`PrivateMessageBeat.java` Java map vs. `resolve.ts` TS resolver). If two land, the key is resolved twice (the TS resolver writes `subtitle`, then the Java map also tries to resolve `key`), or the Java path shadows `voice.ts` as the single source of truth.

**Resolution:** ONE fix, in `resolve.ts` (the unlockbeat file's choice), so `voice.ts` stays the single source of truth for the cold line — exactly the "no English in payloads" / ORACLE.md discipline. `PrivateMessageBeat.java` stays unchanged (it already reads `subtitle`). The Liar file's "Java map" alternative is cut. Both files cross-reference the one fix.

### P1-3 — The Accepting quorum: `config.yml quorum: 6` vs. "ACTIVE players only" is a real contradiction, and two files specify the active-roster reader differently
`backlog-accepting-sentinel-bridge.md` R-2 correctly identifies that a static `quorum: 6` makes the climax unfireable if one of six is offline (violating the never-punish-absent law) and specifies `effectiveQuorum = min(configQuorum, activeRosterSize)` with an injected `activeRoster` supplier. `backlog-full-showrunner.md` U1 *also* specifies the active-roster reader (`readActiveRoster(windowMs)`) feeding "the Accepting satisfiability check." `backlog-bestiary-spawn-bias.md` §6 *also* needs the same active-roster reader. Three consumers, three independent specs of the same reader, no shared owner — the same shape as Batch-1 ROOT B but on the active-roster source.

**Resolution:** one `readActiveRoster(windowMs)` (owned by `backlog-full-showrunner.md` U1, the engine file) is the single source; the Accepting bridge and the spawn-bias conductor both consume it, neither re-derives it. The `min(configQuorum, activeRosterSize)` rule lands once in `AcceptingRiteListener` and is the body of the new active-only invariant (P0-1). State the shared reader in all three files' dependency lists (the bridge already says "reuse the dossier/session notion already tracked" — make it the named `readActiveRoster`).

### P1-4 — The single apparition slot (INV-18) must arbitrate FIVE lanes, but only two backlog files know it exists
`backlog-bestiary-spawn-bias.md` proposes the single-arbiter slot and names the lanes it must gate: the live conductor, `offline-skin.ts`, `name-where-never-been.ts`. But `backlog-keeper-npc-framework.md` (the six per-player apparitions) and `backlog-modeled-mob-and-voice.md` (the rigged Watcher + keeper-whisper) ALSO emit out-of-LoS apparitions/whispers in the same drama window, and neither defers to the conductor's `apparitionClaim`. If the spawn-bias conductor claims the slot but the keeper-NPC apparition and a `SpatialVoiceBeat` whisper fire the same window, the "≤1 ambient figure per window" restraint the conductor enforces is defeated by lanes that never heard of it — the same emergent-sum defect as Batch-1 P0-4 (the DIVIDED corpus), here on the drama budget.

**Resolution:** the single-arbiter invariant (P0-1's INV-18) must name **all** apparition/whisper-emitting lanes as deferrers: spawn-bias, offline-skin, name-where carve, the keeper-NPC per-player apparitions, AND the Ear's `SpatialVoiceBeat`. The conductor publishes `apparitionClaim`; every lane checks it. Add the deferral note to `backlog-keeper-npc-framework.md` and `backlog-modeled-mob-and-voice.md` (currently absent). Synthesis owns this because no single lane can.

### P1-5 — `keeper.run.ts`/`keeper.ts` (P1.14) vs. `keeper-record.ts` (P1.8) — two showrunner modules both "the FACT 9 / keeper layer," reconciled by name only
`backlog-keeper-npc-framework.md` adds `keeper.ts` + `keeper.run.ts` (the NPC dialogue resolver). `backlog-full-showrunner.md` lists `keeper-record.ts` (the Hold-Book "writes you in"). Both are FACT-9 carriers (the framework file even lists "FACT 9 Hold-Book door — NEW-D19/`keeper-record.ts`" as a sibling). The framework file *correctly* heads off one collision (R-4: do not name the M-IV module `reckoning.ts`, which is the difficulty engine), proving the namespace risk is live — but it does not check `keeper.ts` against `keeper-record.ts`. They are distinct (dialogue resolver vs. book-page producer) but share the FACT-9 surface and both run as autonomy passes; without a stated ordering they can both author a FACT-9 surface in the same M4 window (the dialogue line AND the Hold-Book re-read AND the offline-skin named line = three FACT-9 spotlights at once).

**Resolution:** record the FACT-9 delivery order: the **document doors** (`keeper-record.ts` Hold-Book re-read, offline-skin named line) and the **dialogue door** (`keeper.ts` `fact9.named`) are the spine's deliberate "≥2 doors," but they must not all fire in one window on one player — gate them so a given player meets FACT-9 through ONE surface per window (the keeper-NPC file's own precision gate + the conductor's single-arbiter slot, P1-4). This is a sequencing note for synthesis, not a cut; both modules stay.

### P1-6 — `the_unwriting` rail-fence literacy: Batch-1 resolved it as "taught at Brann, reused at the Seventh," but two backlog files state the dependency in opposite directions
Batch-1 P1-5 resolved: teach/embody railFence at **Brann's stone** (with the night gate + fire-count rail key), and have the Seventh's unwriting wall **reuse** that learned literacy. `backlog-keeper-stone-expeditions.md` §1.4 correctly authors Brann's stone as the railFence teacher ("un-orphans railFence … completes the six-verb set"). `backlog-unlockbeat-producers.md` §4.4 and `backlog-fawe-large-setpieces.md` §4 both say `seventh-unwriting` uses railFence "REUSING Brann's taught rail-fence literacy" — consistent. **But** `the-seventh-spine.md` (Batch-1) still claims rail-fence as its *primary* cipher "the right cipher here," and Batch-1 P1-5's resolution to update seventh-spine §1.3 to "gated on Brann's literacy" was a *recommendation*, not yet confirmed landed. So the dependency direction (Brann teaches → Seventh reuses) is now stated by three backlog files but may still read as "taught cold in a side dungeon" in seventh-spine.

**Resolution:** verify `the-seventh-spine.md §1.3` states the Seventh wall is **gated on Brann's railFence literacy** (reuse, not cold-teach); if it still reads "primary cipher, the right cipher here" without the Brann dependency, edit it. The three backlog files are already correct; the one older file is the lagging surface. (Confirm-only; likely a one-line edit.)

---

## SEVERITY P2 — softer tensions, redundancies, bookkeeping

### P2-1 — `RoomSwapBeat` vs. `RevealBeat` vs. "swap mode on SmallStructureBeat" — three names for overlapping world-mutation producers
`backlog-undercroft-dimension.md` proposes `RoomSwapBeat extends SmallStructureBeat` (clear-then-paste). `backlog-unlockbeat-producers.md` proposes a new `RevealBeat` (block-state flip, "no FAWE; that is small_structure's job") and *defers* `small_structure` as an unlock step until FAWE lands, shipping the M5 rite as `reveal` slot-lighting meanwhile. `backlog-fawe-large-setpieces.md` keeps the swap inside `SmallStructureBeat`'s schematic branch. So the world-mutation producer family is: `SmallStructureBeat` (paste), `RevealBeat` (block-state), `RoomSwapBeat` (clear+paste) — three classes with overlapping "change the world on solve" jobs and no stated boundary.

**Resolution:** declare the producer boundary once (in `structures.md` or the unlock file): `RevealBeat` = block-state/marker flips + slot-lighting (no FAWE, trivially idempotent); `SmallStructureBeat` = single FAWE paste onto clear footprint; `RoomSwapBeat` (the P0-3 winner) = the ONE clear-then-paste overwrite. The unlockbeat file's "ship M5 rite as `reveal` until FAWE" interim is compatible — just name `RoomSwapBeat` as the eventual swap producer so the three don't get built as four.

### P2-2 — The Ear's watchlist (FACT 17) shares the keeper-name vocabulary with the cipher plaintexts — verify one source of truth
`backlog-modeled-mob-and-voice.md §4.4` correctly makes the Ear's watchlist the **decoded keeper-name plaintexts** (`canon-spine §1`), so "a name learned at a stone and a name spoken aloud are the same token" (cross-surface-truth). Good. The only risk: the watchlist is specced as a separate checked-in seed (`voice_watchlist`), and the keeper names also live in `clue-specs.ts`/`cipher-plaintexts.md`. Two copies of the keeper-name set can drift.

**Resolution:** the `voice_watchlist` seed must *derive from* (or self-test against) the `canon-spine §1` keeper roster + the bound plaintexts, not duplicate them by hand — a one-line self-test asserting the watchlist ⊆ {decoded keeper names + authored custom names}. Low risk; note it so a re-tuned plaintext can't silently desync the Ear.

### P2-3 — `backlog-modeled-mob-and-voice.md` adds a "by word" axis to FACT 1 while `name-where-never-been` owns the "by place" axis — confirm the triad is coherent, not three competing FACT-1 children
FACT 1 now has (or will have) three children: by **name** (FACT 1 itself), by **place** (FACT 16, `name-where-never-been`), by **word** (proposed FACT 17, the Ear). This is elegant and intentional (the framework file calls it "a third filing axis"), but three independent files each treat their axis as the sharp one. Bookkeeping risk only: the M-II Archivist plant is now shared — `name-where-never-been` plants "against each name, a ground" (place); modeled-mob plants "against each name, a ground; against each ground, what was said over it" (place + word) as ONE fragment. Two files want the same Archivist fragment to carry their axis.

**Resolution:** author the M-II Archivist place/word fragment ONCE (the modeled-mob file's longer form already contains the place clause), owned by synthesis, so FACT 16's plant and FACT 17's plant are the same artifact's two clauses, not two competing fragments on D01/the Archivist. Re-run the LORE-BIBLE §6 "D01 still reads as an artifact" check (Batch-1 P2-6) after — this is one more line landing on the Archivist register.

### P2-4 — Two backlog files independently spec the `swapped`/durable-marker idempotency for the SAME Undercroft swap (Undercroft file) vs. the paste-ledger (FAWE file) — folds into P0-3 but note the migration collision
`backlog-undercroft-dimension.md` wants a PDC `swapped` marker (no new table). `backlog-fawe-large-setpieces.md` wants a `world_paste_ledger` migration. If both ship without P0-3's reconciliation, the swap is guarded by BOTH a PDC marker (Undercroft) and a ledger row (FAWE) with different keys — a double-guard that can deadlock (ledger says "different schematic = allowed," marker says "already swapped = skip") or double-fire on restart if only one persisted.

**Resolution:** P0-3's reconciliation is the fix; the bookkeeping addition is that the **migration** (`world_paste_ledger`) and the **PDC marker** are for *different* set-piece classes (single-paste vs. swap) and must not both guard the swap. State it in the migration's comment and the swap beat's javadoc.

### P2-5 — `backlog-keeper-stone-expeditions.md` meta-acrostic: confirm fall-order vs. ring-order discipline (Batch-1 P1-4) actually landed on the additive surface
The expeditions file §5 correctly relocates the `UNKEPT` meta-acrostic to an **additive surface** (the six maker-headers, NOT the frozen bound plaintexts which spell `G D S I T ·`), and reads them in **"keeper-canonical order (the Rosetta sunwise order, NOT discovery order)."** But Batch-1 P1-4 + `canon-spine §8.1` distinguish **fall-order** (Vaun..Iss, the `UNKEPT` acrostic) from **founders'-ring order** (Bow..Covering). The expeditions file says "Rosetta sunwise order" for the acrostic — which is the **ring** order (D03's Rosetta is the ring), NOT fall-order. `canon-spine §8.1` says the `UNKEPT` acrostic uses **fall-order**. This is a latent contradiction: the expeditions file would place the six header-glyphs in ring order, but canon says UNKEPT reads in fall-order.

**Resolution:** the expeditions file §5 must name **fall-order** (Vaun, Mara, Sella, Orin, Brann, Iss) for the meta-acrostic reading, matching `canon-spine §8.1`, NOT "Rosetta sunwise order" (which is the founders' ring). Per §8.1's self-correcting rule, place the six header-glyphs so they only resolve in fall-order and visibly fail in ring-order. One-line fix in the expeditions file; canon is already correct.

---

## RESOLVED — Batch-1 findings re-verified against current canon (no regression)

- **Batch-1 P0-1 (FACT 16 ×3)** — RESOLVED. Canon §3b assigns FACT 16 = place-filing (`name-where-never-been`); the divergent-reading "FACT 16" is demoted to "mechanical expression of FACT 10" (canon §3b note); difficulty's "FACT 16" is FACT 2b. The three files now say "do not re-mint." ✔ (The NEW recurrence is FACT 17 / the INV set — P0-1/P0-2 above.)
- **Batch-1 P0-2 (two eighths)** — RESOLVED. Canon §8.3 + INV-17: `the_unlit_deep` is the ONE permitted group latch; the Covering (FACT 7b) is a forged **document, not a custom**. `collective-restraint-custom.md` + `some-laws-are-lies.md` both reference this as frozen. ✔
- **Batch-1 P0-3 (two base books)** — RESOLVED. Unified Hold-Book (NEW-D19/19b, `WEB-MASTER §4`); `counting-base-journal` = M1–M3 down-count face, `record-writes-you-in` = M3–M5 keeper-column face, one anchor. ✔
- **Batch-1 P0-4 (DIVIDED vs collective law)** — RESOLVED. INV-16 added (no surface derives WHICH active player; chorus not extremes; name-carve ACTIVE-only, skin OFFLINE-only, never co-located). ✔ (The NEW analog is the apparition-slot single-arbiter across five lanes — P1-4.)
- **Batch-1 P1-1 (Iss true/false coord chain)** — RESOLVED in canon §4/§5 (catch → word → coop gate → coordinate → walk → Accepting, one chain). The backlog Liar/coop files honor it. ✔
- **Batch-1 P1-2 (cold hearth triple-claim)** — RESOLVED. Canon §5 anchor law: dead-shrine (`the_cold_hearth` surface, Iss herring) vs. seventh-shrine (`the_unwriting` deep), distinct places; `seventh_choice` + INHERITORS merged to one flag. ✔
- **Batch-1 P1-3 (M5 ending composer)** — RESOLVED. One M5 composer (`divergent-fates-endings §1.2` / `fate.ts`), bounded clauses; backlog files feed flags, never write M5 lines (the sentinel-bridge §4 explicitly defers the close to the composer). ✔
- **Batch-1 P1-6 (INV-COORD governs three)** — RESOLVED. Promoted to canon **INV-14 (INV-COORD)**; coords/grave/name-where all read, never typed. ✔
- **Batch-1 P1-7 / P2-2 (Sacred Beast fork vs herd)** — RESOLVED. **INV-13 (INV-HERD)**: only the one glowing Beast is tracked/forks; Pale never glow → fork always fairly avoidable. ✔
- **Batch-1 P2-3 (`iss_caught` readers + F5 drift)** — PARTIALLY OPEN. Many readers now exist (good), but the F5 flag-name drift (`_known` vs `_found`) is still unreconciled at the seed/sealed layer and is now a **precondition** for P1-1 above. Carry forward.

---

## CROSS-CUTTING ROOT CAUSES (this batch)

- **ROOT A (recurred) — namespace self-assignment, engine layer.** P0-1 (three INVs), P0-2 (FACT 17). The exact failure Batch-1 named and "fixed by discipline" recurred because the 10 backlog files were authored without that discipline applied to them. **Fix: extend the synthesis namespace-assignment pass to INV numbers AND to the backlog batch; add a one-line note in `canon-spine §7` (INV namespace is synthesis-owned) mirroring the §3b FACT note.** No backlog file mints an INV name/number or a FACT integer.
- **ROOT B (recurred) — shared M5/quorum/active-roster surface.** P1-3 (quorum), and the active-roster reader claimed by three files. **Fix: one `readActiveRoster` owner (the showrunner engine file); the Accepting bridge + spawn-bias consume it.**
- **ROOT C (still open from Batch-1) — `iss_caught` is the universal hinge with an unreconciled flag name.** P1-1, P1-2. **Fix: reconcile the `iss_caught`/`true_coord_*` flag name FIRST (COHERENCE-AUDIT F5), then make the deterministic `requires_flag` gate the single activation authority; demote the showrunner flip to optional re-staging; one `private_message` key-resolver in `resolve.ts`.**
- **ROOT D (new) — competing idempotency authorities for the one A→B swap.** P0-3, P2-4. **Fix: `RoomSwapBeat` clear-then-paste, idempotent on a `swapped` marker, is the ONE overwrite path; the `world_paste_ledger` governs single-paste set-pieces only; paste-over is impossible against `footprintClear` and that claim in the FAWE file is cut.**

---

## ONE-LINE VERDICT
The 10 backlog reconciliation specs are individually faithful to the built code, but as a batch they **re-open the same five shared namespaces Batch-1 closed** — INV numbers (×3), the FACT integer (FACT 17), the `iss_caught` swap lane (×3), the active-roster/quorum surface (×3), and one genuinely-contradictory pair on the A→B swap idempotency model (FAWE ledger vs. Undercroft `RoomSwapBeat`). None requires cutting a backlog item; all resolve by (1) extending the synthesis namespace-assignment pass to INV numbers + the backlog FACT 17, (2) one active-roster owner feeding quorum + spawn-bias, (3) reconciling `iss_caught` (name first) to the single deterministic `requires_flag` gate + one `private_message` resolver, and (4) one A→B swap idempotency authority (`RoomSwapBeat` marker, not the ledger). Batch-1's P0/P1 are confirmed landed in canon with no regression.

---
---

# APPENDIX — BATCH 1 (the original 18-treatment coherence pass, preserved verbatim)

> Authored before the synthesis layer landed in canon. Its resolutions are now in
> `canon-spine §3b/§7/§8` (see RESOLVED above). Preserved for traceability.

## SEVERITY P0 — hard contradictions that break canon or the seed if integrated as written

### P0-1 — FACT 16 is claimed THREE TIMES by three different ideas (namespace collision)
Three treatments each declare a *new* `FACT 16`, each a different fact:
- `name-where-never-been.md` §2a: **FACT 16 = "the record files the living by place, not only by name."**
- `divergent-fates-endings.md` §4.1: **FACT 16 = "the record's reading is not one word."**
- `dynamic-diegetic-difficulty.md` §4: **FACT 16 = "the land's grip is not fixed; it closes on those who run ahead."**

The canon spine ends at FACT 15 (the sealed reveal). All three cannot be FACT 16. **Resolution:** synthesis assigns all FACT integers in one pass; place-filing earns FACT 16, the others become FACT 10b/FACT 2b. *(LANDED: canon §3b.)*

### P0-2 — TWO different ideas seed an "eighth custom / eighth way" with opposite truth-value
`collective-restraint-custom.md` (`the_unlit_deep`, a REAL latch) vs. `some-laws-are-lies.md` (the Covering, a FORGED law whose point is there is NO eighth custom). **Resolution:** ship the forged Covering as a document; `the_unlit_deep` is the one permitted group latch reusing existing detections. *(LANDED: canon §8.3 / INV-17.)*

### P0-3 — Two ideas place a mutating base lectern book at the same anchor
`counting-base-journal` (Brann's down-count) and `record-writes-you-in` (the Hold-Book). **Resolution:** one Hold-Book; down-count = M1–M3 face, keeper-columns = M3–M5 face. *(LANDED: NEW-D19/19b.)*

### P0-4 — `DIVIDED` ending vs. collective-judgment / no-chosen-one law
Per-player surfaces (record-writes-you-in, name-where, future-dated-grave, offline-skin) could let a sharp group reconstruct the honored/violated split. **Resolution:** new INV — no surface lets the group derive WHICH active player is on which side; surfaces rhyme on a chorus, never the extremes. *(LANDED: INV-16.)*

## SEVERITY P1 (Batch 1)

### P1-1 — Iss's true/false coordinate claimed by two ideas. **Resolution:** one IV→V chain catch → word → coop gate → coordinate → walk → Accepting. *(LANDED: canon §4/§5.)*
### P1-2 — Cold hearth triple-claimed. **Resolution:** dead-shrine vs. seventh-shrine distinct; `seventh_choice` + INHERITORS one flag. *(LANDED: canon §5.)*
### P1-3 — Seventh-thread flags lack a shared M5 owner. **Resolution:** one M5 composition pass, bounded clauses. *(LANDED: `divergent-fates §1.2`.)*
### P1-4 — UNKEPT acrostic order vs. founders'-ring order. **Resolution:** the order-key names fall-order, glyphs self-correct in ring-order. *(LANDED: canon §8.1.)*
### P1-5 — Rail-fence claimed by Brann (audit) AND the Seventh spine. **Resolution:** teach at Brann, reuse at the Seventh. *(LANDED in canon note on `the_unwriting`; verify seventh-spine §1.3 — see Batch-2 P1-6.)*
### P1-6 — INV-COORD stated locally, governs three. **Resolution:** promote to canon. *(LANDED: INV-14.)*
### P1-7 — Sacred Beast fork vs herd conversion vs precision. **Resolution:** only the glowing Beast forks; Pale never glow. *(LANDED: INV-13.)*
### P1-8 — Two future-instant gates (timestamp vs grave date). **Resolution:** one Accepting instant, three surfaces. *(LANDED: canon §8.2.)*

## SEVERITY P2 (Batch 1) — see prior detail; bookkeeping items (acrostic sameyness, INV numbering, D01 plant budget, offline/active separation) all folded into canon §6/§7 or carried as authoring discipline.

## CROSS-CUTTING ROOTS (Batch 1)
- ROOT A — namespace self-assignment (FACT/INV/eighth). *(Fixed by discipline; RECURRED in Batch-2 — see above.)*
- ROOT B — shared M5 ending surface. *(One composer; LANDED.)*
- ROOT C — the Iss/M4 catch is the universal hinge; reconcile `iss_caught` name first. *(STILL OPEN — Batch-2 P1-1.)*
- ROOT D — base book + cold hearth double/triple-claimed anchors. *(LANDED.)*
