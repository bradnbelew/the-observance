# The Observance — PROGRESSION-LANES (the build spec for the Nether + End deepening lanes)

> The dimension-aware build spec for the two optional deepening lanes (`INTEGRATION-V2 A15`,
> `minecraft-progression`): the **Nether** = the deep fire-source ("below the below"), off
> Movements II→III; the **End** = exile / the Seventh's absence, off Movements IV→V. Authority
> for *what each change contains* is `INTEGRATION-V2 A15`; for *how it fits the web* is
> `WEB-MASTER §0.4 / §5 / §8`; for *what is canon* is `arc/WORLD-BIBLE.md §12` + `arc/lore/
> canon-spine.md`. This file is the **build worklist** — what gets built, where (which world),
> and in what order — and it names every REAL artifact authored this pass (no stubs).
>
> **THE TWO LAWS THAT GOVERN BOTH LANES.** (1) **They gate nothing** (INV-12, INV-19): a group
> that never lights a portal nor finds an End gateway gets the whole un-shaded Overworld arc —
> removing both lanes entirely still reconstructs the spine. (2) **No orphan**: every lane beat
> re-reads an existing flag, custom, or choice. The Nether deepens the Kept-Light custom (the
> keeping is a *carrying*) and FACT 11; the End deepens FACT 10/10b and the `seventh_choice`.
> Neither mints a FACT, an INV, a custom key, or a cipher.

---

## 0. PRECONDITIONS / BLOCKERS (must clear before the named build, in this order)

| # | Blocker | Owner (lane) | Status | What unblocks |
|---|---|---|---|---|
| 0.1 | **The FACT-11 source-clause seal** — *"the kept fire was carried up from below the bottom; the Undercroft is the bottom of the Hold, the deep-fire its source — one direction, not two"* sealed into `canon-spine` FACT 11 + `design/structures.md` (S11, `BUILD-MANIFEST §0.12`). | **LORE** (owns `canon-spine.md`) — NOT this pass's owner. | **PENDING — cross-owner hook (see §7).** | The entire Nether build. Until sealed, the Nether journals say **"below the below," never "the real bottom,"** and the lane is design-only. |
| 0.2 | **`WEB-MASTER §8` ratifies `FateInput.netherForgeFound`** (S9) — the frozen-interface edit to `decideFate`, with the self-test *`kept` fires fully without it*. | **synthesis** + **TS-SHOWRUN** (`fate.ts`). | **PENDING.** | The Nether `kept`-lean. Until ratified, `nether-forge` sets the flag and the **M5 composer** reads it for a tint; `decideFate` does **not**. |
| 0.3 | **The two Multiverse worlds exist** — `observance_nether` + `observance_end` (the Undercroft Multiverse pattern, `backlog-undercroft-dimension`). | **PLUGIN** (GO-LIVE). | **PENDING (GO-LIVE).** | `siteCoverageSelfTest` (R7) lets the cross-dimension rows seed OPEN; the payoff rows flip `active=true`. |
| 0.4 | **The `end_exile_hold` INV-16 binding is built** (S10) — chorus-only dressing, no per-player side, no spatial correspondence to any carve. | **PLUGIN** + **DASH**. | **PENDING / CUTTABLE.** | The P2 `cast_out`-made-a-place set-piece. If unmet, `end_exile_hold` stays `enabled:false` and the End ships as the Seventh shrine **alone** (the default — §5). |

> **Nothing in this lane is P0.** The slice proves the Overworld loop; both lanes are P1 (the two
> cheap cores) → P2 (the trek, the exile-hold, the intimate Nether glimpse). See `BUILD-MANIFEST §9`.

---

## 1. WHAT GETS BUILT WHERE (the dimension-aware site map)

All coords are `null` (UNPLACED) until GO-LIVE; the plugin silently skips an unplaced site. The
per-site `world:` field already exists in `sites.yml`; `LocationSampler` keys proximity/heatmap on
`worldName`, so a cross-dimension site needs **zero new tracking infra** — `Site.contains` is the
dimension-presence signal.

### 1.1 `observance_nether` (the deep fire-source)

| Site (`sites.yml`) | Type | enabled | What is built there | Reveal-safety |
|---|---|---|---|---|
| **`nether_forge`** | `answer_sign` | true | The **near pocket** — a small ruined room just past a lit portal (a *delve*, not a trek). A prior keeper's **remains** on a deepslate slab + a doused soul-lantern + the journal `the-fire-kept-me`. The on-site **word** is cut on the slab. | **Placed at world-build**, never pasted toward an approaching player. |
| **`soul_gallery`** | `marker` (texture only) | true | A wide field of **soul sand / soul fire** the group crosses on the way in — the not-kept of DEEP TIME (S5). Carries the `nether.soulSand` Set-B line nearby. Nothing built; the vanilla block IS the lore. | Static vanilla; nothing mutates. |
| **`bastion_remains`** | `structure` (texture only) | true | A **vanilla bastion / fortress** re-read in-fiction as the founders' deepest ruined delvings ("hands that had stopped being quite hands"). Discovered-never-witnessed wrong-scaled ruin. The P2 intimate glimpse may fire near here. | Additive dressing only on adjacent air; **no occupied overwrite**. |

### 1.2 `observance_end` (exile / the Seventh's absence)

| Site (`sites.yml`) | Type | enabled | What is built there | Reveal-safety |
|---|---|---|---|---|
| **`end_seventh_shrine`** | `answer_sign` | true | The **Seventh shrine** — a re-dressed **end-ship** (force-load → mutate on unwitnessed relog → unload) OR a world-build **pre-generated outer island**. Built to the Seventh's *unfinished, wrong-scaled* hand. Carries the carving `the-name-i-cut-myself`. | Re-dress an *already-generated* structure, or pre-generate at world-build — **never a lazy paste toward an approaching glider** (S6). |
| **`end_exile_hold`** | `structure` | **false** | The **`cast_out` exile-hold** (P2) — a re-dressed end-city, markers all facing *away*. Vast, static, discovered-never-witnessed. | **Disabled until the INV-16 binding is built** (S10) — else cut; the End ships as the shrine alone. |

> **The End has ZERO ambient apparition lane** — a *positive* canon choice (the End is outside the
> record; a figure there would contradict it, and would be seen winking in across open air, S6/R3).
> The **Nether** occludes, so it carries the rare intimate glimpse (P2), re-skinned to basalt and
> **deferring to the single-arbiter `apparitionClaim`** (INV-18) — the Nether has no apparition lane
> of its own.

---

## 2. DIMENSION-AWARE BEAT PLACEMENT (which built beat, in which world, fired by what)

**No new beat class, no new listener, no new cipher.** Every symbol below already ships.

| Beat / listener | Lane | Where it fires | Producer rule |
|---|---|---|---|
| `SignWriteBeat` | both | The on-site answer-word on the `nether_forge` slab; the carving on `end_seventh_shrine`. | The word is plaintext (INV-14 — the **word** answers, never a coordinate). |
| `LecternFillBeat` | Nether | The journal pages `the-fire-is-lent` (at the Undercroft, post-descent) + `the-fire-kept-me` (at the pocket). | Out-of-LoS, idempotent high-water. |
| `AnswerSignListener` | both | Reads the on-site word at `nether_forge` / `end_seventh_shrine`; sets `nether_forge_found` / `seventh_seen_out`; records the solve. | Site-scoped; gated by the world the site is in (`Site.contains`). **The flag is set by the existing on-site answer path — no new listener.** |
| `SmallStructureBeat` | both | **Additive pastes onto verified-clear ADJACENT AIR only** — soul-lanterns/lecterns/a deepslate carving-slab beside vanilla blocks. | **NEVER** an occupied overwrite; **NEVER** `RoomSwapBeat` (Undercroft-only, S7). Bound to the `world_paste_ledger` single-paste owner (`WEB-MASTER §0.5`). |
| `RevealBeat` | End | The **way-out pointer** — one extra effaced line on the existing `the_unwriting` chamber-2 wall, legible only at `seventh_named` (a reveal on an existing surface, **NO new puzzle node**, S9). | Block-state flip, no FAWE, trivially idempotent. |
| `PrivateSoundBeat` / `PrivateParticleBeat` | both | Per-presence arrival reveal at each on-site tableau. | Per-presence, reveal-disciplined. |
| `NamedMobBeat` (re-skinned) | Nether (P2 only) | The intimate basalt-corridor keeper-shape near `bastion_remains`. | **Defers to `apparitionClaim`** (INV-18). The End gets none. |

> **No `RoomSwapBeat` in either dimension.** Nothing mutates in the open End; the Nether pocket is
> static. The one A→B overwrite path is Undercroft-only.

---

## 3. THE SEEDS (what `progression_seed.sql` carries — REAL rows authored this pass)

`discord/supabase/seeds/progression_seed.sql` (NEW, this pass). Mirrors `puzzles_seed.sql` (payoff
rows, `ON CONFLICT DO UPDATE`) + `seventh_seed.sql` (breadth rows, `ON CONFLICT DO NOTHING`) +
`thread_cards.sql` (rumor cards). Every `accepted_answers` entry is pre-normalized (ORACLE.md §2:
lower, `[a-z0-9 ]` only, single-spaced, trimmed, **no apostrophe**).

| Row | File §| outcome | Sets | `requires_flags` | active | Gate? |
|---|---|---|---|---|---|---|
| **`nether-forge`** | §1 | `lore` | `nether_forge_found` + `whisper_budget_earned` + the Kept-Light origin | `{undercroft_open}` (§4, guarded) | **false** (STAGED until `observance_nether` built) | **NO** |
| **`end-seventh-out`** | §1 | `lore` | `seventh_seen_out` | `{seventh_named}` (§4, guarded) | **false** (STAGED until `observance_end` built) | **NO** |
| **`dest-deep-forge`** | §2 | breadth (`who`, entry `nether-forge`) | — | — | — | **NO** (`gates_progress` false) |
| **`dest-out-of-record`** | §2 | breadth (`who`, entry `end-seventh-out`) | — | — | — | **NO** |
| **`who-deep-forge`** | §3 | rumor card → verified on arrival (`who`, `nether_forge`) | — | revealed_by_solve `nether-forge` | — | **NO** |
| **`who-seventh-out`** | §3 | rumor card → verified on arrival (`who`, `end_seventh_shrine`) | — | revealed_by_solve `end-seventh-out` | — | **NO** |

**On-site words (INV-14, plaintext — no cipher):** Nether = the founders' word `lent` / the
keeper's account; End = the Seventh's read. The **pointer** that *sends* the group is NOT a row in
either lane — Nether = Brann's M2 carved framing line (*"the fire we keep is not ours… below the
below"*) + the `the-fire-is-lent` bearing-page; End = the `the_unwriting` wall's extra effaced line
(a reveal, S9). Plant → payoff, staggered (arg-craft F3): the Nether plants at M2 (Brann's framing),
pays at M3 (the pocket); the End plants at M3 (D11, no new plant), pays at M4→V (the shrine).

**`requires_flags` activation (the `metapuzzle_seed.sql` guarded pattern, §4 of the seed):**
`nether-forge ← undercroft_open` (found at the Undercroft post-descent); `end-seventh-out ←
seventh_named` (the way-out pointer is legible only then). The guard no-ops cleanly if the 0006
column is absent. Both stay `active=false` until GO-LIVE flips them per built world (both
`active=true` AND `requires_flags`-satisfied must hold for `getOpenPuzzles` to open the row).

---

## 4. THE Cast-Out = End LINK (the divergent ending made a place — INV-16-bound, P2/cuttable)

This is the lane's one connection to the **ending composer**, and it is the most precision-fraught
beat in either lane. It changes **no selector** and punishes **no one**.

- **`seventh_seen_out` is NOT a fate input** (S2). The fate is decided unchanged — by the
  honored/violated spread + `leftAtActive >= 2` (`cast_out`) or quorum + `refusalSignal`
  (`refusers`). `seventh_seen_out` only *licenses* the End **place** to read back to a group that
  **already earned** the fate. It touches the **M5 composer** (one tinted clause), never
  `decideFate`.
- **For `cast_out` + `seventh_seen_out`:** the re-dressed `end_exile_hold` reads as *their own*
  exile-hold — markers facing away, vast, static, discovered-never-witnessed. The M5 composer
  appends `fateCastOutEndRead` (it **replaces** the neutral fate clause for groups who went out, so
  the §5 ≤2-clause cap holds — not an extra clause).
- **For `refusers` + `seventh_seen_out`:** the shrine re-reads as *the model they followed* —
  `fateRefusersEndRead` ("*so did one before you*"), and the one before them is the Seventh whose
  shrine they stood in. (Reuses the existing `fateRefusers` "so did one before you" register.)
- **INV-16 TEETH (S10 — non-negotiable or it does not ship):** the exile-hold **names no living
  player and encodes no per-player side.** Its dressing rhymes only on a *chorus* every active player
  shares (*"you only came to look"*), **never** on the `LEFT_AT` set, and **no dressing may spatially
  correspond to any per-player carve.** If the open End cannot guarantee that, the binding is **cut**
  and the End ships as the **Seventh shrine alone** (the default — `end_exile_hold` stays disabled).
- **The Nether/End never change the `seventh_choice` mechanic** — they deepen its *meaning*. A group
  that walked the Nether (keeping = a carrying) and the End (the not-keeping = exile, the Seventh's
  own account out there) performs the same seeded `seventh-choice` row at `the_unwriting` with both
  readings in hand: `restore` = carry the Seventh's name back into the record; `erase` = complete the
  cast-out. The choice already exists (INV-12 colors-never-gates).

---

## 5. THE DEFAULT / FALLBACK LADDER (what ships if a blocker stays unmet)

| If unmet | The lane degrades to | Never breaks |
|---|---|---|
| FACT-11 seal pending (0.1) | The Nether lane is **design-only** — does not ship. Journals stay at "below the below." | The Overworld arc + the Undercroft (the bottom of the Hold) are unchanged. |
| `WEB-MASTER §8` not ratified (0.2) | `nether-forge` sets the flag; the **M5 composer** reads it for a tint; `decideFate` does not. `kept` still fires fully without it. | The fate selector is untouched; no group is punished for skipping the Nether. |
| Dimension worlds not built (0.3) | The payoff rows stay `active=false` (STAGED); `siteCoverageSelfTest` keeps the cross-dimension rows from seeding OPEN. | No unplaced-site error; the plugin silently skips. |
| `end_exile_hold` INV-16 binding not built (0.4) | `end_exile_hold` stays `enabled:false`; the End ships as the **Seventh shrine alone**. The `cast_out` fate still resolves as Overworld floor-dressing + the M5 clause. | The ending is unchanged; no per-player side is ever derivable. |
| The voice layer / dashboard never wires a key | A missing voice key is **silent** at runtime (only `thread_cards` body keys are build-guarded). | No build break. |

> Every degradation is **graceful and silent** — the anti-jank law: nothing breaks if a lane is
> half-built, world-absent, or model-absent.

---

## 6. SELF-TESTS / GUARD FLAGS THE LANES MUST KEEP GREEN

- **`siteCoverageSelfTest` (extended, R7):** a cross-dimension coord/destination row must NOT seed
  OPEN unless its `site_id` resolves to a placed + **enabled site in the named world**
  (`observance_nether` / `observance_end` must exist first). Holds by construction (rows ship
  `active=false` until GO-LIVE).
- **`activationReachabilitySelfTest` (the seed §5 ledger):** `nether-forge → undercroft_open`;
  `end-seventh-out → seventh_named`. Each named by exactly one rule; neither is a spine predecessor;
  no `active=true` row is reachable only through a staged predecessor.
- **`specsCoverageSelfTest` / `NON_CIPHER_KEYS`:** when the two payoff rows activate, TS-FORGE must
  add `'nether-forge'` + `'end-seventh-out'` to `NON_CIPHER_KEYS` (they are non-cipher lore nodes).
  While `active=false` they are exempt.
- **`threadCardVoiceCoverageSelfTest`:** the two rumor cards' `body_voice_key`s (`cardNetherForge`,
  `cardEndSeventhOut`) must exist in `voice.archive.ts` (TS-VOICE/archive owner).
- **`fate.selftest.ts` (when 0.2 lands):** `kept` fires fully **without** `netherForgeFound`.
- **`registerDisciplineSelfTest`:** every new voice key (`nether.soulSand`, `nether.forgeArrive`,
  `end.shrineArrive`, `end.outsideRecord`, `fateCastOutEndRead`, `fateRefusersEndRead`) passes
  lowercase / no-caps / no-exclaim / no-meta-word.
- **No `RoomSwapBeat`, no `world_paste_ledger` double-guard** in either dimension (the open End and
  the static Nether pocket never mutate-while-witnessed).
- **X1 plaintext round-trip:** untouched — the lanes add **no cipher**; Brann's Nether framing line
  lives in carved framing (X1-safe), never a bound plaintext.

---

## 7. CROSS-OWNER HOOKS + GO-LIVE RESIDUE (what this pass does NOT own)

**Cross-owner hooks (other lanes must land these — this pass authored canon/docs/sites/seeds/spec
only):**
- **LORE** (`canon-spine.md` + `structures.md`): **seal the FACT-11 source clause** (0.1, the P1
  Nether blocker). Until then the Nether is design-only. *This pass deliberately kept WORLD-BIBLE §12
  and the Nether journals at "below the below," never "the real bottom."*
- **synthesis + TS-SHOWRUN** (`fate.ts` / `WEB-MASTER §8`): ratify `FateInput.netherForgeFound` with
  the `kept`-without-it self-test (0.2). Until then `decideFate` does not read the flag.
- **SQL migration** (0006-era): add `arc_state` flags `nether_forge_found` + `seventh_seen_out` (both
  gate nothing) and the `puzzles.requires_flags jsonb` column (the guarded blocks in the seed no-op
  without it).
- **TS-VOICE** (`voice.ts` + `voice.archive.ts`): insert `nether.soulSand`, `nether.forgeArrive`,
  `end.shrineArrive`, `end.outsideRecord`; the M5 composer clauses `fateCastOutEndRead` /
  `fateRefusersEndRead` (replace the neutral fate clause); the card bodies `cardNetherForge` /
  `cardEndSeventhOut`. Reuse `fateRefusers` (no new key for the "so did one before you" re-read).
- **TS-FORGE** (`clue-specs.ts`): add `'nether-forge'` + `'end-seventh-out'` to `NON_CIPHER_KEYS`
  when they activate.
- **TS-SHOWRUN** (M5 composer): read `nether_forge_found` (tint) + `seventh_seen_out` (the End
  re-read, §4) within the ≤2-clause cap.
- **DASH:** **no website widening** (S8 — `record-projection.ts` is untouched; the lanes feed Discord
  Whisper + the dashboard fate-preview + the M5 composer only).

**GO-LIVE residue (needs a client / asset / in-game placement — not buildable in the repo):**
- Two **Multiverse worlds** `observance_nether` + `observance_end` (the Undercroft pattern), then
  fill the `null` coords in `sites.yml` for `nether_forge`, `soul_gallery`, `bastion_remains`,
  `end_seventh_shrine` (and `end_exile_hold` if its binding is built), and flip the two payoff rows
  `active=true`.
- The **Nether pocket** built at world-build (keeper remains on a deepslate slab + doused
  soul-lantern + the on-site word), in a short ruined room past a portal (the walk-budget: 2 ground
  walks + ≤1 short vertical pocket).
- The **End Seventh shrine** as a re-dressed end-ship (reveal-safe) OR a world-build pre-generated
  outer island, dressed to the Seventh's unfinished hand, carrying the carving.
- The **End exile-hold** (P2) only if the INV-16 binding is built (chorus-only, no per-player side) —
  else cut.
- A short authored **resource-pack** pass (optional): the soul-fire ambient / basalt re-skin for the
  P2 Nether glimpse. The lanes ship without it (the vanilla blocks already carry the lore).
