# Divergent Fates — 3–5 distinguishable endings, not a binary

> Design treatment, **reconciled to as-built ground truth (2026-06-25).** Read against:
> `arc/lore/canon-spine.md` (§3 FACT-web, §3b frozen-namespace, §5 Seventh, §6 rules, §7
> INV-11/16/17, §8 anchors); `design/WEB-MASTER.md` §5 (the M5 composer), §8 (divergent-endings
> logic), §9 (plant→payoff ledger); `discord/src/showrunner/{fate,state,types,decide}.ts`;
> `discord/src/oracle/resolve.ts`; `discord/src/voice.ts`;
> `plugin/.../signal/listener/AcceptingRiteListener.java`;
> `plugin/src/main/resources/sites.yml`; `discord/supabase/seeds/{puzzles_seed,seventh_seed}.sql`;
> `dashboard/src/components/author/{AcceptingTrigger,BondLedger}.tsx`.
>
> **STATUS:** the *decision* is **BUILT** — `discord/src/showrunner/fate.ts` ships a pure,
> deterministic `decideFate(FateInput): FateDecision` and synthesis has canonized this idea into
> `WEB-MASTER §5/§8` and the `canon-spine §3b` demotion note. This treatment is therefore the
> **finishing spec**: it reconciles the original idea to the as-built selector and names the
> *remaining* build gaps (selftest, the resolve.ts sentinel branch, the migration, the M5 voice
> fns + dressing enactor, the dashboard fate panel).
>
> **One-line verdict:** **KEEP, SCALED** to **3 resolvable fates (`kept`/`cast_out`/`divided`) +
> 1 secret (`refusers`) + 1 codicil (`INHERITORS`, merged into the Seventh `restore` act)**, the
> selector reading ONLY measured group tallies over active players, the divergence carried by the
> M5 composer's ≤2 tinted clauses + persistent floor dressing — never a branching cutscene, never
> a player name.

---

## 0. THE LAW THIS IDEA LIVES OR DIES BY

Canon-spine §6.3 + INV-11 are absolute: **collective judgment, no chosen one; the ending selector
reads measured group tallies over ACTIVE players only, returns an enum, names no player, and the
bond/Whisper tally is NOT a selector input.** Divergent Fates is richer than kept/cast-out without
ever (a) electing a favorite, (b) punishing the group for an absent member, or (c) handing the LLM
the climax.

The selector is a **pure deterministic function** — same shape as `decide()` — and, crucially, **a
fate is an enum, so there is nothing to "fall back" to: the determinism IS the backstop** (per the
`fate.ts` header). No DB, no clock, no LLM in the policy. The I/O that reads the spread and writes
the fate lives in the resolve.ts sentinel branch, not in the policy.

**Divergent Fates does not change how the rite FIRES. It changes how the rite RESOLVES.** The
`AcceptingRiteListener` is **fate-NEUTRAL** (confirmed in its header): it posts the one opaque,
performed `accepting-crouch` bow token byte-for-byte; *which* close composes is decided downstream
by the showrunner's single M5 composer from already-tracked signals. The plugin never branches the
ending.

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 The five fates (as-built enum + selector precedence)

The enum is **`'kept' | 'cast_out' | 'divided' | 'refusers'`** (`EndingFate` in `fate.ts`).
`INHERITORS` is **not a fifth enum value** — it is a separate `ending_codicil` boolean, set
independently upstream, that the M5 composer may append as one clause. Each fate is a *register of
the same event* ("you were received"), coloured differently. None is a win/lose screen; all are the
world reading the group it just received.

| Fate | Enum | Selector rule (as shipped in `decideFate`) | Camera-legible dressing |
|---|---|---|---|
| **Refusers** *(secret)* | `refusers` | **checked FIRST.** `quorumMet && refusalSignal`. A *positive, measured* defiance signal + the bow window empty. NEVER "quorum && !bowed". | reuses cast-out dressing, but the closing line is *chosen*, not inflicted; the Seventh's empty slot is "kept" on purpose. |
| **Kept** | `kept` | `honoredDominates && (seventhFound || issCaught) && quorumMet`. ("Dominates" = strict majority of the active honored/violated split.) | warmest read; the light holds; the markers around the floor face **outward** (they keep watch *with* the group now). |
| **Cast Out** | `cast_out` | `violatedDominates && leftAtActive >= 2` (≥2 active keepers reached the `LEFT_AT` rung). | the cold read; floor light gutters; abandonment-era passive voice ("they were not kept"); markers turn **away**. Reversible in tone (markers can turn back; light can be relit) per §6.10. |
| **Divided** | `divided` | **the floor of the enum** — any real arc that is neither all-kept nor all-cast-out, *and* the dead-even / empty arc (`decided === 0` → "the land holds on neither side"). | the world cannot read them as one; the light holds on **half the floor by geometry, never by player** (INV-16). The sharpest on-camera read. |
| **Inheritors** *(codicil)* | — (`ending_codicil` boolean) | set when `seventh_choice === 'restore'` — the deposit of a mark for a hand not yet here. Appended to *whichever* base fate fired. | the re-warmed hearth block-state (`restore`); one appended Keeper clause pointing past the arc. |

> **Scaling decision (honest, and now canon):** five fully-authored ending *cinematics* is
> over-build for a 4–6-person friend group on a 2-week run. The shape is **3 first-class resolvable
> fates** (each: own closing record-sentences + persistent floor dressing) + **`refusers` as a
> secret reusing cast-out dressing with a chosen line** + **`INHERITORS` as a one-boolean codicil**.
> This is exactly what `fate.ts` + `WEB-MASTER §8` build. It keeps "not a binary" within the
> anti-jank / build-budget reality.

### 1.2 The single M5 composer owns the prose (WEB-MASTER §5)

`fate.ts` returns an enum + a logged `reason` — **never an M5 sentence.** All ending prose is owned
by **one synthesis-owned M5 composition pass** (WEB-MASTER §5, coherence ROOT-B) that reads every
ending-colorant flag and emits a **bounded** Keeper close: the neutral close **+ at most 1–2 tinted
clauses, by fixed priority** — never all of them (the FACT-15 "delivered by accumulation, never by a
sentence" discipline breaks if M5 stacks six conditional sentences). Fixed priority:

1. `ending_fate` selects the **base** close (one of the four fate lines).
2. `seventh_choice` OR the heaviest fork (`sacred_beast_broken`, `light_kept|light_taken`,
   `name_unspoken|name_spoken`) may add **one** tinted clause.
3. `ending_codicil` (`INHERITORS`) may append the **one** "a mark is left for a hand not yet here" line.

The **persistent-world dressing** is the camera-legible delta — the sentence only confirms what the
floor already showed.

### 1.3 How it reads across the 5-movement arc

- **Movement I (Act 1, Days 1–5).** No ending is visible. The selector's *inputs* accrue silently:
  `custom_compliance` honored/violated counts and the `LEFT_AT` rung (already tracked by the customs
  bridge). **Plant:** the first base report's `observed → warned → left` escalation ladder is the
  inert demonstration that "left" is a real category. Re-read at M5 it is the `cast_out` seed.
- **Movements II–III (Act 2).** Two player-driven flags bend the fork toward `kept`:
  **`iss_caught`** (FACT 8 — catching the Vigenère lie keyed on Iss's own name) and **`seventh_found`**
  (FACT 10/10b — reading the hearth-deep `the_unwriting` carving). You cannot be meaningfully *kept*
  until you know you could have been *cast out* — finding the Seventh is what makes a good read mean
  something. Neither gates the rite; both *colour the fate*. The **`seventh_choice ∈ {restore|erase}`**
  is offered once the deep opens (post-`iss_caught` + `seventh_named`); `restore` is the
  `INHERITORS` codicil act.
- **Movement IV (Act 2 close).** The `custom_compliance` spread is legible to the author (dashboard),
  felt by players only as the warmth/cold gradient of ordinary beats. No one is told a fork exists.
- **Movement V (Act 3, the Accepting).** The bow fires (`AcceptingRiteListener`, unchanged). The
  oracle records the opaque token. The **resolve.ts set-once sentinel branch** reads the active-only
  tallies + flags through `decideFate`, writes `arc_state.ending_fate` + `arc_state.ending_codicil`
  (idempotent — a second bow does not re-roll). The M5 composer drips the fate-tinted close; the
  plugin enacts the persistent dressing. **`refusers`** is a non-event: detected on the showrunner
  cadence after the summons is set and the bow window has elapsed *with a positive defiance signal* —
  never by the listener, so a slow group is never falsely read as refusing.

### 1.4 Why it is a mystery, not a menu

The group never sees "3 endings available." The fork is **retroactively legible**: only at the
threshold does the spread of their own two weeks resolve into a sentence. The "oh" is *the record
reading us back to ourselves* — the haunting was the entrance exam (FACT 9/15), and the grade was
being computed the whole time from things they *did*, not things chosen from a list. `divided` is the
sharpest beat: the world admitting it cannot make one verdict of them — more unsettling than pass/fail
and impossible to game, because no one knows which side of the split they are on.

---

## 2. CRITIQUE — adversarial, honest (with the as-built mitigation)

**R1 — "Chosen one" leak via `divided` or the bond crown. THE SINGLE SHARPEST RISK.** `BondLedger`
renders a `most` badge for the top of the tally, and `divided` inherently means some honored, some
not. If any fate line, toast, or dressing names or *implies which* players are the honored/lapsed
side, the collective law breaks on camera — the worst failure.
**Mitigation (built + canon):** `FateInput` **has no field for the bond tally** — it is excluded *by
construction* (INV-11; verified in `fate.ts`), so the heaviest-whisperer can never decide the group's
fate. Every fate line is group second-person/passive ("you were not read as one"), never "X kept and
Y did not." The `divided` floor split is **by geometry, dressed downstream, never by player**
(`fate.ts` header + INV-16). The bond `most` crown is author-mode-only and feeds the selector nothing.

**R2 — Absent-member punishment.** A stale violated-count from a week-absent player could drag the
group to `cast_out` or fake a `divided`.
**Mitigation (built):** every count in `FateInput` is **over ACTIVE players only — the caller
filters; the module never sees an absent member** (`fate.ts` header + INV-11). Reuse the same active
window the customs bridge already uses; do not invent a new one.

**R3 — `refusers` mis-fires on a slow/AFK group.** Reading "didn't bow yet" as refusal would be the
engine inventing a transgression (violates precision-over-recall, §6.4).
**Mitigation (built):** `refusers` is checked first BUT requires `quorumMet && refusalSignal`, and
`refusalSignal` is "a POSITIVE, measured defiance signal … NEVER 'quorum && !bowed'" (`fate.ts`,
default false, set only by a plugin-detected refusal rite). A slow group falls through to the tally
read. A wrong "you refused" is worse than none.

**R4 — Orphaned-mechanic risk on `INHERITORS`.** A season-2 seed with no season-2 is a classic
orphan.
**Mitigation (canon, merged):** `INHERITORS` is **not a separate `dark_shrine` deposit** — that idea
was merged out (sites.yml line ~261: "never carry a `dark_shrine` entry"; `seventh_seed.sql` header;
INTEGRATION-V2 A1). It is **one flag = `seventh_choice = restore`**, which pays off *within this arc*
as "you marked the place for whoever comes next" (FACT 15 felt, not stated). Season-2 is upside, not
the justification.

**R5 — Five branches = five times the QA surface = cross-surface contradiction risk.**
**Mitigation (built):** one selector → one enum → written **ONCE** (set-once, idempotent) to
`arc_state`; every surface reads that one field, never re-derives it. The M5 composer's ≤2-clause cap
bounds the prose surface. `refusers` reuses cast-out dressing.

**R6 — On-camera legibility.** Three subtly different closing reads could look like "nothing
happened" in the YouTube cut.
**Mitigation (canon §5):** the **persistent-world dressing is the visible delta** (markers face
in/out, floor light whole/split/guttered, hearth re-warmed/blank) — the camera SEES the fork before
the text lands; the sentence only confirms it.

**CUT verdict:** nothing is cut outright. The only thing cut is *scope* — full cinematic parity
across five endings — and that cut is already canon. The 3+1+1 shape delivers the pitch.

---

## 3. DE-SLOP TEST — exemplar closing lines, in the record/Archivist register

Lowercase, plain, declarative, cold-with-warmth-under, no named emotion, no thematic bow, no
exclamation. Each *points at* FACT 15 without stating it. (These are exemplars; the verbatim M5
lines live with `voice.ts` per §4.3.)

**KEPT:**
> kept. the light holds. there are more hands now to tend it. walk the markers — they face out, for you.

**CAST_OUT (abandonment passive voice, grief not cruelty):**
> the count is read. they were not kept. the light is yours to carry out, if you can carry it.

**DIVIDED (cannot make one verdict — no names):**
> the record cannot write you as one. the light holds where it holds. the rest are still in the count.

**REFUSERS (a chosen non-act, the Seventh's road taken on purpose):**
> you found the floor and did not bow. so did one before you. the record keeps the empty slot, and the slot keeps you.

**INHERITORS codicil (one appended line):**
> a mark is left where a name was unwritten. it will be read by a hand that is not yet here.

---

## 4. THREAD IT — where this must appear so it is not an orphan (named files + symbols)

### 4.1 Canon-spine FACTs touched / added (`arc/lore/canon-spine.md`)
- **It is NOT a new FACT.** §3b explicitly **demotes** the divergent reading from the abortive
  "FACT 16" to *the mechanical expression of FACT 10* ("the land can refuse"), delivered by the M5
  composer (§5), not a numbered row. **Do not re-mint it.** (The real FACT 16 is "the record files by
  place"; unrelated.)
- **Touches FACT 6** (named/warned/left binary) — `cast_out` is its full-arc payoff (`leftAtActive >= 2`).
- **Touches FACT 10 + FACT 10b** (acceptance is a choice the land makes; it refused a keeper who broke
  nothing) — Divergent Fates is FACT 10's mechanical expression; `seventh_found` is a selector input;
  `seventh_choice` colors the close. Without this idea, FACT 10 is an orphaned foreshadow.
- **Touches FACT 8** (Iss lied) — `iss_caught` is a selector input.
- **Touches FACT 14/15** — the fate lines are where 14 (receive/keep) lands and 15 is *felt by what
  the floor does*.
- **§7 INV-11 / INV-16 already codify** the active-only-enum-no-player rule and the no-spatial-leak
  rule — both already present; this treatment is governed by them, adds no new invariant.

### 4.2 Found-documents / journals that must foreshadow it (`arc/lore/documents/`, `arc/lore/found-documents.md`)
- An **abandonment-era report** in the passive voice ("they were not kept") seen in M3/M4, so the
  `cast_out` line *rhymes* with a document already read.
- The **`the_unwriting` (hearth-deep) carving** that `refusers` and `INHERITORS` both re-read: the
  Seventh's unwritten name, the empty slot. (Authored as the `seventh-unwriting` / `seventh-choice`
  thread, `seventh_seed.sql` + `puzzles_seed.sql`.)
- An **Orin keeper-stone fragment** re-read at M5 to seed `divided`: a settlement *itself* split in
  the breaking-of-faith era (canon §2: "some keep them as custom, some as defense, some abandon them").

### 4.3 NPC / Watcher voice lines (`discord/src/voice.ts`)
- **NEW (gap):** five fns authored verbatim in register (no inline English generation): `voice.fateKept()`,
  `voice.fateCastOut()`, `voice.fateDivided()`, `voice.fateRefusers()`, `voice.fateInheritorsCodicil()` —
  the §3 lines. The existing `voice.summons()` (line 114) is unchanged; the fate line drips *after* the
  bow resolves, via the showrunner drip path (the fate is a *consequence*, same shape as a custom report).
- If delivered through the oracle payload, add the keys to the `OracleVoiceKey` union; **prefer the
  showrunner drip path** (consequence-shaped) to keep the listener fate-neutral.

### 4.4 Cipher(s) / puzzle(s) it expresses (reuse the 11 built ciphers — NO new cipher)
- The fork is **read, not decoded.** The flags that bend it are existing cipher payoffs:
  - `iss_caught` = the **Vigenère** keyed clue whose key is Iss's name (canon §4; Iss embodies Vigenère).
  - `seventh_found` / `seventh_choice` = reading `the_unwriting` via **rail-fence (rails = 6, reusing
    Brann's taught literacy)** — per sites.yml (~line 272) and `seventh_seed.sql` (`dest-unwriting-deep`).
    (NB: the original treatment said Atbash + a1z26/polybius for a `dark_shrine` deposit — **superseded**:
    no separate shrine, the deposit is the `restore` choice, the literacy is rail-fence.)

### 4.5 Engine realization — modules / branches / tables / seeds / sites / dashboard

| Element | File / symbol | Status |
|---|---|---|
| Pure selector | `discord/src/showrunner/fate.ts` — `decideFate(FateInput): FateDecision`, `EndingFate`, `FateInput`, `FateDecision` | **BUILT** |
| Selector selftest | `discord/src/showrunner/fate.selftest.ts` (mirror `decide.selftest.ts`/`customs.selftest.ts`) | **GAP — referenced in `fate.ts` header but absent.** Author it. |
| Resolve sentinel branch | `discord/src/oracle/resolve.ts` — on resolved `puzzleKey === 'accepting-crouch'`, build a `FateInput` from active-only `custom_compliance` + `seventh_found`/`iss_caught`/`quorumMet`/`refusalSignal`, call `decideFate`, write `arc_state.ending_fate` + `ending_codicil` **set-once / idempotent** | **GAP (TS-SHOWRUN owns; noted in `autonomy.run.ts` line ~274 as the home).** |
| Migration | `discord/supabase/migrations/00xx_*.sql` — `alter table arc_state add column ending_fate text null, add column ending_codicil boolean not null default false` (no new table) | **GAP** |
| State cache | `discord/src/showrunner/state.ts` — `ShowrunnerState.ending_fate` mirror | **BUILT** (line ~69) |
| M5 composer voice fns | `discord/src/voice.ts` — `voice.fate*()` (§4.3) | **GAP** |
| Plugin dressing enactor | new fate-dressing beat under `plugin/.../beats/` reading `arc_state.ending_fate` on the Act-3 enact: marker facing, floor-light geometry (whole/split/guttered), hearth state, advancement-toast text. **Main-thread world writes** (anti-jank); reversible in tone (§6.10) | **GAP** |
| `refusers` non-event watcher | showrunner cadence pass (`run.ts` + small `fate-watch`) after the summons flag + bow-window elapse + positive defiance signal; sets `refusalSignal`. **Never in the listener.** | **GAP** |
| Listener | `AcceptingRiteListener.java` — **UNCHANGED** (fate-neutral; posts the opaque token only) | **BUILT, no change** |
| Sites | `accepting_floor` (sites.yml line 131) + `the_cold_hearth` (248) + `the_unwriting` (270). **No `dark_shrine`** (merged out). `divided` light geometry reuses `accepting_floor` bounds. | **BUILT** |
| Seeds | `seventh_seed.sql` (`dest-unwriting-deep`) + `puzzles_seed.sql` (`seventh-unwriting`/`seventh-cause`/`seventh-choice`) | **BUILT** (verify `seventh_choice` writes flow to the resolve branch) |
| Dashboard | `AcceptingTrigger.tsx` — add a read-only **fate preview** (what `decideFate` returns right now over active players, author-only) + a manual **fate-override** select that lands as a *pending* beat through the approval gate. `BondLedger.tsx` unchanged (neutral, feeds selector nothing) | **GAP** |

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for" (WEB-MASTER §9)

- **PLANT (Movement I):** the first base report's `observed → warned → left` ladder. At the time it
  reads as one player noted for a habit — ambiguous, low-stakes. The word **"left"** is inert flavour.
- **PLANT (Movement III):** the hearth-deep `the_unwriting` — an unwritten name, an empty slot, a
  light fully out. At discovery it is backstory: someone was cast out once, unexplained.
- **PAYOFF (Movement V):** at the threshold, `decideFate` resolves the *whole active group* by exactly
  that ladder (`leftAtActive >= 2` → `cast_out`), group-wide. The M1 "left" (one habit) re-reads as the
  category the entire group can fall into; the M3 empty slot re-reads as the fate the group can *choose*
  (`refusers`) or *leave a mark at* (`INHERITORS` = `seventh_choice = restore`). **The "oh": the week-one
  ledger entry and the week-two dead shrine were the two halves of the ending the group was being graded
  toward the entire time.** Both plants pay off; neither plant states the fork. Only the threshold makes
  them mean what they meant.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES

- **Lives in:** inputs accrue across **M I–IV** (silent); the fork **resolves in M V** at the
  Accepting; the `INHERITORS`/`seventh_choice` plant sits in **M III** (`the_unwriting`).
- **Depends on:** `AcceptingRiteListener` + opaque token (BUILT); `custom_compliance` + `LEFT_AT`
  tracking (BUILT); the Seventh + Iss flags (`seventh_found`/`iss_caught`/`seventh_choice` — seeds
  BUILT, flag-wiring into the resolve branch is the GAP); `arc_state` (exists, columns are the GAP);
  the **active-player window** reused from the signal tracker.
- **Depended on by:** the felt landing of FACT 10/14/15; the YouTube cut's climax legibility; any
  future season-2 (`INHERITORS`).
- **Priority:** **P1 (arc-spine).** The single binary kept/cast-out is the P0 vertical-slice minimum;
  Divergent Fates is the spine-grade enrichment that makes M5 worth filming.
  - **P1 now:** `fate.selftest.ts`; the resolve.ts sentinel branch; the migration; the `kept`/`cast_out`/`divided`
    voice fns + dressing enactor; the dashboard fate preview. (`divided` is the highest-value, lowest-cost
    addition — sharpest mystery, dressing = floor-light split.)
  - **P2 (depth):** `refusers` non-event watcher + chosen line; the `INHERITORS` codicil clause +
    re-warmed-hearth block-state. Ship after the 3-way read is solid.
