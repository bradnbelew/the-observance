# Cross-Surface + Anti-Hallucination Key Integrity Audit

LENS: cross-surface-keys. READ-ONLY pass. Every voice key, flag, custom name, puzzle key,
site id, document id, sound/beat key, card id, and the stego payload was cross-referenced
across plugin Java, `discord/src/**`, `dashboard/src/**`, seeds, lore, the Record website,
and the stego layer. Ground truth was obtained by running the actual self-tests
(`specs.selftest.ts`, `stego`, `runes`, `seedcheck`) — results inline below.

## VERDICT (one line)

The deterministic spine and the stego→key chain are **solid and non-hallucinated**
(stego payload `ISS` decodes to the real `stone-iss-wall` Vigenère key; rune alphabet is a
clean bijection; 130/130 seed answers normalize). But the **newly-authored web back-half is
not wired across surfaces**: the build self-test FAILS today, the M4/Seventh activation gate
references a column and a code path that do not exist, an answer-string collision can
shadow the single most important M4 gate, and the Record website's payoff URL + live data
source are unbuilt. These are integration danglers, not story contradictions — but they are
DEFECTS that block go-live.

---

## SEVERITY-RANKED MISMATCH MATRIX

### S0 — BUILD-BREAKING (the canonical self-test fails right now)

**[S0-1] 11 active seed rows are UNCLASSIFIED → `specsCoverageSelfTest` throws.**
- Evidence (ran `npx tsx src/forge/specs.selftest.ts`):
  `specsCoverageSelfTest: 11 active seed row(s) are UNCLASSIFIED (neither a registered cipher spec nor in NON_CIPHER_KEYS): a1z26-tick-stave, forged-eighth, prophet-wall-comfort, prophet-wall-name, fork-light, fork-name, name-where, record-url, difficulty-mara, reckoning-rosetta, base-docket-reread-auto.`
- file:symbol — `discord/src/forge/clue-specs.ts` `NON_CIPHER_KEYS`
- Plus 10 STAGED rows (active=false today, flipped active by `metapuzzle_seed.sql §2`) are
  ALSO unclassified and will trip the same test the moment they activate: `bound-word`,
  `m4-three-hands`, `threshold-coordinate`, `true-walk-arrive`, `seventh-unwriting`,
  `seventh-cause`, `seventh-choice`, `base-docket-reread`, `meta-unkept` (and
  `stone-brann-cipher` which is a real cipher needing a CLUE_SPECS entry, not NON_CIPHER).
- ONE-LINE FIX: add all 20 non-cipher rows to `NON_CIPHER_KEYS` with reasons (they are
  lore/sentinel/observation/fork nodes; the seed comments at puzzles_seed.sql L556-557,
  L1001-1007 already name each as a "TS-FORGE lane must add" debt). `stone-brann-cipher`
  is the ONE that instead needs a real `railFence` entry in `CLUE_SPECS` (or stay
  active=false until it gets one — it is correctly staged today, so it is exempt while
  inactive but MUST be classified before activation).

> This is the highest-severity defect: the cross-surface coherence harness that guarantees
> "the carving and the seed can never drift" cannot pass, so it currently guarantees nothing.

---

### S1 — RUNTIME-BREAKING (referenced symbol/column/path does not exist)

**[S1-1] `puzzles.requires_flags` is referenced by two seeds but created by NO migration.**
- Referenced: `discord/supabase/seeds/metapuzzle_seed.sql` (`do $$ … requires_flags … $$`,
  L107-145) and `puzzles_seed.sql` (L1093, L1109-1111 comments).
- `grep requires_flags --include=*.sql` → matches ONLY the two seed files. No `0006_*.sql`
  exists (`discord/supabase/migrations/` has only 0003/0004/0005).
- EFFECT: the metapuzzle activation `do $$` block hits its `else` branch and **silently
  no-ops** ("activation lane skipped" — L143). The whole M4 + Seventh back-half stays
  `active=false` and **UNREACHABLE** — this is exactly the arg-craft F1 defect the file
  claims to fix, still unfixed.
- ONE-LINE FIX (SQL lane): add `discord/supabase/migrations/0006_requires_flags.sql`:
  `alter table public.puzzles add column if not exists requires_flags jsonb not null default '{}'::jsonb;`
  then re-run `metapuzzle_seed.sql`.

**[S1-2] `getOpenPuzzles` does NOT select or AND-test `requires_flags` — the gate is
inert even once the column exists.**
- file:symbol — `discord/src/db/repo.ts` `getOpenPuzzles` (L361-372): selects a fixed
  column list (no `requires_flags`) and filters only `.eq('active', true)`. No read of
  `arc_state.flags`. The `Puzzle` type (`discord/src/db/types.ts` L136-147) has no
  `requires_flags` field.
- EFFECT (compounding with S1-1): even after 0006 lands and `metapuzzle_seed §2` flips the
  back-half rows to `active=true`, every one of them becomes OPEN IMMEDIATELY with **no flag
  gate**. The sequenced Iss chain (`bound-word → m4-three-hands → threshold-coordinate →
  true-walk-arrive`) and the Seventh deep collapse into "all doors open at once, no
  prerequisites" — directly violating WEB-MASTER §0.4 temporal layering and the
  HARD/sequenced design intent.
- ONE-LINE FIX: `getOpenPuzzles` must `select('…, requires_flags')`, read
  `arc_state.flags` once, and return only rows where every key in `requires_flags` is
  truthy in flags; add `requires_flags?: Record<string, unknown>` to the `Puzzle` type.

**[S1-3] `base-docket-reread-auto` claims a deterministic flag gate it cannot get.**
- `puzzles_seed.sql` L1094-1112 ships it `active=true` and states it is "GATED BY
  requires_flags {iss_caught:true}" via the COLUMN. With S1-1 (no column) AND S1-2 (no
  AND-test), it is in fact open from world-start — the M4 "down-count re-read" fires in
  Movement I, out of sequence, spoiling FACT 9/14 before the catch. Fixed transitively by
  S1-1 + S1-2.

> S1-1/2/3 are one defect cluster: the deterministic activation lane the new ARG rides on
> was authored in SQL/seed prose but neither the migration nor the `getOpenPuzzles` code that
> must honor it was written. Until both land, the back half is either dark (column missing)
> or ungated (column present, code blind).

---

### S2 — CROSS-SURFACE LOGIC DEFECT (resolver routing / orphaned mechanic)

**[S2-1] Answer-string collision shadows the M4 `bound-word` gate.**
- Three rows accept the normalized answer `the one who turned away`:
  `stone-iss-wall` (active M2, sets `iss_key_turned`), `prophet-wall-name` (active M2,
  `dead_end`), `bound-word` (M4, sets `bound_word_known` → opens `m4-three-hands`).
  (puzzles_seed.sql L199, L661, L805.)
- `matchPuzzle` (`repo.ts` L380-389) returns the FIRST open row whose `accepted_answers`
  contains the string; `getOpenPuzzles` has **no `ORDER BY`** (L361-368), so order is
  whatever Postgres returns. The per-player replay guard (`hasSolved`) is per-KEY, so it
  does not disambiguate distinct keys.
- EFFECT: once `bound-word` is open (post-catch), a player typing the bound word may resolve
  `prophet-wall-name` (a `dead_end`, M2) instead — the Watcher speaks a dead-end line and
  `bound_word_known` is NEVER set, so `m4-three-hands` never opens. The single most
  important gate chain is non-deterministically breakable.
- ONE-LINE FIX: make the M4 token DISTINCT from the M2 readings — `bound-word` should accept
  a phrase the M2 rows do NOT (e.g. drop the bare `the one who turned away` from `bound-word`
  and keep its unique `the bound word is his name` / `turned away`), OR give
  `getOpenPuzzles` a deterministic `.order('movement')` AND have `matchPuzzle` prefer the
  highest-movement open match. (Distinct tokens is the cleaner, anti-jank fix.)

**[S2-2] `oracleDeadEnd(kind)` — the 5 kind-switched taunts are ORPHANED; the kind is never
passed.**
- `voice.ts` `oracleDeadEnd(kind?: DeadEndKind)` (L259-274) implements 5 distinct taunts
  (`name`/`count`/`place`/`known`/`prophet`); the seed sets `voice_args.kind` on 8 dead-end
  rows (e.g. `m1-named-habit` `kind:'name'`, `m2-rhyme` `kind:'count'`, `forged-eighth`
  `kind:'known'`, `prophet-wall-*` `kind:'prophet'`).
- file:symbol — `discord/src/oracle/resolve.ts` `speakOutcome` L365-366 and `defaultLineFor`
  L395 BOTH call `voice.oracleDeadEnd()` with NO argument. The `voice_args.kind` is read
  nowhere. All 8 dead-ends speak the generic line; the authored variety is dead code.
- ONE-LINE FIX: in `speakOutcome`, `case 'oracleDeadEnd': return voice.oracleDeadEnd(payload.voice_args?.kind as DeadEndKind | undefined);`
  (and the same in `defaultLineFor`'s `dead_end` case). Import `DeadEndKind` from `voice.ts`.

**[S2-3] 4 web-realization voice keys are defined + seeded but NEVER dispatched by the
resolver.**
- `speakOutcome` (`resolve.ts` L362-377) switches on only 5 keys: `oracleNextClue`,
  `oracleDeadEnd`, `oracleSideQuest`, `oracleMainBeat`, `oracleLore`. The seed references
  four MORE in `outcome_payload.voice_key`:
  - `oracleThreeHands` (m4-three-hands) — falls through to `default → oracleMainBeat()`.
    The authored line "the count is three. the threshold is open." NEVER speaks.
  - `oracleMetaUnkept` (meta-unkept), `recordElsewhere` (record-url), `docketReread`
    (base-docket-reread / -auto) — all `lore`-type, fall through to
    `default → defaultLineFor('lore') → oracleLore(loreFragment)`. The fragment passthrough
    happens to be correct (these rows carry `voice_args.fragment`), so the SPOKEN TEXT is
    acceptable — but the dedicated voice fns (`voice.oracleMetaUnkept`/`recordElsewhere`/
    `docketReread`, voice.ts L302-322) are dead, and the dispatch is accidental, not designed.
- `oracleThreeHands` is the real defect (wrong line speaks); the other three are latent
  (right text via fallback, dead fn). file:symbol — `discord/src/oracle/resolve.ts`
  `speakOutcome`.
- ONE-LINE FIX: add the four `case` arms to `speakOutcome`
  (`oracleThreeHands → voice.oracleThreeHands()`; the three lore keys →
  `voice.<key>(loreFragment(payload))`), and widen the `OracleVoiceKey` switch coverage.
  (`OracleVoiceKey` in voice.ts L624-634 already LISTS all four — the type is correct; only
  the resolver switch is missing them.)

**[S2-4] `the_unlit_deep` collective-restraint custom — voice exists, NO tracker signal.**
- `voice.ts` defines `tollUnlitDeep`/`keptUnlitDeep` (L572-578) and a `the_unlit_deep`
  entry in `CUSTOM_PHRASES` (L605). But `the_unlit_deep` is NOT in `canon.ts` `CUSTOM_KEYS`
  (L18-26, 7 keys) and NOT in `plugin/.../signal/TrackerConfig.java` (which defines 7
  `CUSTOM_*` constants, none unlit). So the A5 "Unlit Deep" group latch has a Watcher voice
  but **nothing measures it** — an orphaned mechanic (violates the no-orphan law). The
  voice.ts comment (L602-604) admits it: "threads it once TS-FORGE adds it to CUSTOM_KEYS" —
  never done.
- Because `the_unlit_deep` is absent from `CUSTOM_KEYS`, `customKeyNamespaceSelfTest`
  does NOT flag it (it only checks the 7 canonical keys), so this dangler is invisible to
  the build guard — it passes silently.
- ONE-LINE FIX (decide the intent): EITHER add `the_unlit_deep` to `CUSTOM_KEYS` +
  a `CUSTOM_UNLIT_DEEP` constant + the detector in the plugin (wire the mechanic), OR remove
  the three `the_unlit_deep` voice symbols (cut the orphan). Do not leave voice-without-signal.

---

### S3 — RECORD WEBSITE (A13) CROSS-SURFACE GAPS

**[S3-1] The decoded payoff URL does not resolve to a built route.**
- `record-url` decodes (in-world) to the path the answer-token spells: `the record keeps`
  (puzzles_seed.sql L952-966; voice "the path is the record keeps"). The dashboard has
  `dashboard/src/app/record/[slug]/page.tsx` and `record/layout.tsx` — but **no
  `record/page.tsx`**. So bare `/record` 404s, and `RecordPage()` (L110) takes no `params`
  and renders the SAME archive for ANY `[slug]` — the slug is decorative.
- DEFECT: which exact URL the founder line decodes to is specified NOWHERE in code, and the
  most natural decode (`/record`) 404s. The ARG-leaves-the-game payoff can dead-end on a 404.
- ONE-LINE FIX: add `dashboard/src/app/record/page.tsx` (or make `[slug]` validate against
  the canonical decoded slug and `notFound()` otherwise), and pin the decoded path string in
  ONE shared constant the in-world clue and the route both cite, so the seed answer and the
  URL can never drift.

**[S3-2] `v_record` view does not exist → the Record is permanently sealed.**
- `record/[slug]/page.tsx` `readSignal()` (L50-79) reads `from('v_record')`; the page admits
  "the SQL lane owns it" and "if absent … the sealed baseline." No migration creates
  `v_record` (not in 0001/0002/0003 dashboard migrations or the discord migrations). So The
  Record ALWAYS shows the empty baseline — the lockstep un-redaction (REVEAL DISCIPLINE)
  never fires. Degrades safely (no crash), but the feature is non-functional cross-surface.
- ONE-LINE FIX (SQL lane): create the SECURITY DEFINER `v_record` view exposing exactly
  `{ movement, stones_read, accepted }` from the spoiler tables, granted to anon.

**[S3-3] The Record header uses UNICODE runes that contradict the one shared alphabet.**
- `record/[slug]/page.tsx` L124 hard-codes `ᛟ ᚲ ᛖ ᛈ ᛏ` (real Unicode Elder Futhark). The
  project's rune alphabet (`discord/src/forge/runes.ts`) is explicitly an ORIGINAL,
  non-Unicode carved-stroke script ("original strokes … not copying any Unicode rune", L17-18)
  and is the declared single source of truth. A player who learned the in-world glyphs CANNOT
  read these — a false affordance and a cross-surface rune contradiction (violates the
  one-shared-rune-alphabet law). It is the ONLY place Unicode runes appear.
- ONE-LINE FIX: render the header mark with the canonical script (an SVG from
  `runes.renderRunes('KEPT')` or a neutral non-glyph seal) instead of Unicode runes; never
  present un-decodable glyphs as if they were the keepers' script.

---

### S4 — MINOR / STALE-COMMENT (no runtime effect)

**[S4-1] `thread_cards.sql` header site-list omits `coop_plate` and `the_unwriting`.**
- The header comment (L20-22) enumerates "enabled sites" but lists neither `coop_plate`
  (used at L262, `happened-three-hands`) nor `the_unwriting`. Both DO exist + are enabled in
  `sites.yml` (L309, L279), so `siteCoverageSelfTest` passes — this is a stale doc comment
  only. FIX: add the two ids to the comment list.

**[S4-2] `accepting-crouch` / `record-receives` / `seventh-choice` / `m4-three-hands`
opaque sentinel tokens — VERIFIED clean.** No leaked self-describing sentinel words
(`noLeakedSentinelSelfTest` passes); the tokens are wordless high-entropy strings the plugin
posts on detection. No action. (Listed as a positive confirmation, not a defect.)

---

## CONFIRMED-CLEAN (anti-hallucination — these cross-surface keys DO resolve)

- **STEGO → DOWNSTREAM KEY: REAL.** `stego.ts` `ISS_STEGO_PAYLOAD = 'ISS'` is exactly the
  `stone-iss-wall` Vigenère key (`clue-specs.ts` L218-228, `key:'ISS'`). `stegoSelfTest()`
  passes (6 cases) and asserts the binding can't drift. `STEGO_PUZZLE_KEY = 'stone-iss-wall'`
  is a real ACTIVE forgeable row. The stego decodes to a real key. NO hallucination.
- **RUNE ALPHABET: bijective + single-source.** `runeSelfTest()` passes; `runes.ts` is the
  one source of truth shared by the cipher (`substitution`/`stone-orin`), the stego layer,
  and the renderer. (Exception: the dashboard Unicode runes, S3-3.)
- **SEED ANSWER NORMALIZATION: clean.** `seedcheck.ts` passes — 130/130 accepted_answers
  across 45 puzzles pre-normalized + non-empty. Both surfaces share `normalizeAnswer`.
- **CLUE_SPECS ↔ seed bind: 5/5 cipher nodes round-trip** (`specsSelfTest` cases pass when
  reached): decode(forge(spec)) === plaintext === a seed accepted_answer for vaun/mara/
  sella/orin/iss. The carving and the card cannot render different plaintext.
- **THREAD CARDS ↔ voice.archive: 46/46 `cardXxx` keys referenced in `thread_cards.sql` are
  defined in `voice.archive.ts`** (1:1, verified by diff). No dangling card voice key.
- **SITE IDS: every seed-beat `site_id` exists + enabled in `sites.yml`** (`coop_plate`,
  `the_unwriting`, `first_marker_01`, `stone_brann`, `stone_of_reckoning`, `unbroken_light`,
  `the_threshold`, `the_cold_hearth` all present). `siteCoverageSelfTest` passes.
- **7 tracked customs (`the_bow`…`the_sacred_beast`) present in TrackerConfig.java AND
  voice.ts CUSTOM_PHRASES** — namespace consistent (the only custom dangler is the untracked
  `the_unlit_deep`, S2-4).

---

## WHAT THE FINALIZE INTEGRATOR MUST APPLY (ordered)

1. **[S0-1]** Add the 20 unclassified rows to `clue-specs.ts NON_CIPHER_KEYS` (19 non-cipher
   + leave `stone-brann-cipher` for a real CLUE_SPECS railFence entry or keep it inactive).
   Re-run `specs.selftest.ts` — it MUST go green. (Without this, nothing else is verifiable.)
2. **[S1-1 + S1-2]** Ship `0006_requires_flags.sql` (the column) AND teach `getOpenPuzzles`
   to select + AND-test `requires_flags` against `arc_state.flags` (and add the field to the
   `Puzzle` type). Both, together — either alone leaves the back half broken (dark or ungated).
3. **[S2-1]** Disambiguate the `the one who turned away` collision so `bound-word` can't be
   shadowed by `prophet-wall-name`/`stone-iss-wall` (distinct M4 token, the clean fix).
4. **[S2-3]** Add the four missing `case` arms to `resolve.ts speakOutcome`
   (`oracleThreeHands` is the one that currently speaks the WRONG line).
5. **[S2-2]** Pass `voice_args.kind` into `voice.oracleDeadEnd(kind)` (un-orphan the 5 taunts).
6. **[S3-1 + S3-2]** Add a real `/record` route target for the decoded path AND create the
   `v_record` view (the Record is a 404-risk + permanently-sealed without both).
7. **[S2-4]** Decide `the_unlit_deep`: wire the tracker signal OR cut the voice (no orphan).
8. **[S3-3]** Replace the dashboard's Unicode-rune header with the canonical `runes.ts` script.
9. **[S4-1]** Patch the stale `thread_cards.sql` site-list comment.

> Bars check: the spine, stego chain, and answer-loop are HARD + non-hallucinated + internally
> consistent (good). The defects above are all INTEGRATION danglers in the new back-half — fix
> them and the web is genuinely non-linear and sequenced; ship them as-is and the M4/Seventh
> half is either unreachable, un-gated, or 404s — which would read as "broken," the one thing
> the three bars cannot tolerate.
