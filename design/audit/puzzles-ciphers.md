# AUDIT — PUZZLES + CIPHERS lens (fresh whole-project pass)

Auditor scope: puzzle/cipher DIFFICULTY, FAIRNESS, WEB-DENSITY across the whole project —
`discord/src/forge/ciphers.ts`, `clue-specs.ts`, `discord/src/oracle/{normalize,resolve,seedcheck}.ts`,
`plugin/.../AnswerNormalizer.java`, `discord/supabase/seeds/*`, `design/{cipher-web,clue-web,WEB-MASTER}.md`,
`day-one-meta-puzzle.md`. READ-ONLY. Severity: S1 (breaks the arc / unfair) → S4 (polish).

The cipher *primitives* are strong: 11 pure round-trip-self-tested transforms, coordEncode is the fixed
digit-glyph scheme with the leading-zero + 32-bit-extreme + "reads as a number" asserts, the TS and Java
normalizers are byte-for-byte identical, and the X1 forge↔decode↔seed bind is machine-checked. The defects
below are NOT in the primitives — they are in the **resolution / activation / web wiring** that sits on top,
and three of them can hard-break the spine on camera.

---

## S1 — RESOLUTION-LAYER DEFECTS (can break the arc; must-fix before any playtest)

### S1-A. `requires_flags` gate is DOCUMENTED-BUT-UNBUILT — the entire back-half activation lane is inert
**Where:** `discord/src/db/repo.ts: getOpenPuzzles()` (the `.eq('active', true)` query) vs.
`discord/supabase/seeds/metapuzzle_seed.sql §2`, `discord/src/showrunner/liar.ts:5`,
`showrunner/README.md:74`, `WEB-MASTER.md §0.5`.

The code comments and the seed both **assert** the activation gate is "the deterministic
`puzzles.requires_flags` gate in `getOpenPuzzles`" (liar.ts L5; metapuzzle_seed L16:
"getOpenPuzzles … must AND-test it"). It does not. `getOpenPuzzles` selects
`puzzle_key, title, accepted_answers, outcome_type, outcome_payload, movement, active, max_attempts`
— `requires_flags` is **not even selected**, never mind AND-tested — and the `Puzzle` interface
(`db/types.ts:136`) has no `requires_flags` field. `matchPuzzle` only checks `active` + answer membership.

Consequence: `metapuzzle_seed §2` sets `requires_flags` on the nine back-half rows AND flips them
`active=true` in the same transaction. With no enforcement, **all nine open the instant the seed runs**,
regardless of gate flags. The sequenced Iss chain (`bound-word → m4-three-hands → threshold-coordinate →
true-walk-arrive`) and the Seventh deep (`seventh-unwriting/-cause/-choice`) are meant to open link-by-link;
instead the whole web is open at once. This also makes S1-D (the meta resolving before its prerequisites)
unconditional, and it makes the `prophet-wall-name` collision in S1-B fire in Movement II as authored.

**Fix (the gate the whole design assumes):**
1. `db/types.ts` — add `requires_flags: Record<string, boolean> | null` to `interface Puzzle`.
2. `db/repo.ts: getOpenPuzzles` — add `requires_flags` to the `.select(...)` list, fetch
   `arc_state.flags` (the resolver already imports `setArcFlags`; add a `getArcFlags()` read), and
   return only rows where **every** key in `requires_flags` is truthy in `arc_state.flags`
   (null/`{}` ⇒ always open). Do the AND-test in SQL **or** in a pure helper
   `isOpen(puzzle, flags)` so it can be unit-tested. Fail-OPEN on an arc_state read error would
   re-introduce the bug — fail-CLOSED for any row that *has* a non-empty `requires_flags` (a gated
   row staying dark on an outage is safe; an ungated row is unaffected).
3. Add the missing **`activationReachabilitySelfTest`** that `metapuzzle_seed §3` already documents as
   "seedcheck NEW" but which does not exist in `oracle/seedcheck.ts` or `forge/canon.ts`: assert every
   `requires_flags` row is named by exactly one activation rule and no `active=true` row is reachable
   only through a staged predecessor.

### S1-B. CROSS-ROW ANSWER COLLISIONS among simultaneously-active rows → non-deterministic mis-resolution
**Where:** `seeds/puzzles_seed.sql` (rows below) + `db/repo.ts: matchPuzzle` (returns the FIRST open row)
+ `getOpenPuzzles` (NO `ORDER BY` → Postgres returns rows in arbitrary physical order).

`matchPuzzle` iterates `openPuzzles` and returns the first whose `accepted_answers` contains the
normalized string. `getOpenPuzzles` has no deterministic order, so when two **active** rows share an
answer, *which* row wins is undefined and can change between sessions. Shared answers across active rows
(verified by extracting every `accepted_answers` value and grouping by puzzle_key):

| shared answer | rows (active?) | failure |
|---|---|---|
| `the one who turned away` | `stone-iss-wall`(T, next_clue→iss-doubt, the catch on-ramp) **vs** `prophet-wall-name`(T, **dead_end**) | **WORST.** A player decodes the Iss wall, submits the name, and may hit the `prophet-wall-name` dead_end ("a true name; it opens nothing"). It sets no flag, opens no door, and the dead_end voice reads as "you're done here" — so they never resubmit. `stone-iss-wall`→`iss-doubt`→`no-wall-catch`→`iss_caught` is **soft-locked**, taking the whole back half (and the meta) with it. |
| `the last marker is not the last` | `stone-sella`(T, side_quest→seventh-shrine) **vs** `seventh-shrine`(T, gate) | the Sella bearing fork can be skipped / mis-credited; the Seventh on-ramp resolves to the wrong row. |
| `a thing that can say no is not a wall` | `seventh-shrine`(T) **vs** `seventh-cause`(F today) | latent: the instant `seventh-cause` activates, it collides with `seventh-shrine`. |
| 4× `base-docket-reread` answers (`the count was never of the dark…`, `the muster is read…`, `the down count is a muster…`, `not a doom clock a roll call`) | `base-docket-reread`(F→**flipped to T by metapuzzle_seed §2 L139**) **vs** `base-docket-reread-auto`(T) | the showrunner row and its "offline AUTO twin" are intentionally duplicate, but they were **never meant to be active simultaneously**. metapuzzle_seed activates `base-docket-reread` while `-auto` is already active → both twins live with 4 identical answers. One must be the single live row. |

**Fix:**
1. **Author-level (correctness):** an `accepted_answers` string must be unique across all rows that can be
   open at the same time. For the `prophet-wall-name` collision: drop `'the one who turned away'` from
   `prophet-wall-name`'s answers — its *intended* solve is the **acrostic spelled down the wall**, so keep
   only the acrostic-specific phrasings (`'iss carved the wall'`, `'read the first marks down the one who
   turned away'`) and **make the dead-end fire on the acrostic act, not the bare name** (the bare name is
   `stone-iss-wall`'s door). For Sella/seventh-shrine: give each a distinct destination-phrase
   (`stone-sella` keeps the bearing words; `seventh-shrine` keeps the count/"and one more" phrasings).
   For the docket twins: ensure exactly one is active at a time (the `requires_flags` gate from S1-A on
   `base-docket-reread`, with `base-docket-reread-auto` carrying the **complementary** gate, so they are
   mutually exclusive — never both open).
2. **Structural guard (so this can never regress):** add an `answerUniquenessSelfTest` to `forge/canon.ts`
   (wired into `specs.selftest.ts`): for every `accepted_answers` value, collect the set of rows that
   carry it; if two rows that can be co-active (active=true, OR co-activatable per their `requires_flags`)
   share a value, THROW. This is the missing sibling of `noLeakedSentinelSelfTest`.
3. **Determinism backstop:** add a stable `ORDER BY movement, puzzle_key` to `getOpenPuzzles` so even an
   *intended* overlap resolves the same way every time (defense in depth; not a substitute for #1).

### S1-C. `prophet-wall-name` is a `dead_end` carrying a `next_puzzle_key`-free payload but shares the catch's answer — verify the dead_end doctrine
**Where:** `seeds/puzzles_seed.sql: prophet-wall-name` (L658) and the §7 authoring checklist rule 4
("a `dead_end` carries `voice_key` only").

`prophet-wall-name` itself is checklist-clean (no `next_puzzle_key`, `max_attempts:6`). The defect is
purely the S1-B answer overlap. Once S1-B #1 lands (the bare name removed), this row is a fair, fine
second-acrostic herring. Flagged separately so the Finalize integrator confirms the row's *payload* is
left intact while only its answers are narrowed.

### S1-D. The meta-acrostic resolves on the wrong precondition — it does NOT require all six prerequisites
**Where:** `seeds/metapuzzle_seed.sql §2` (`meta-unkept` ← `requires_flags = {iss_caught:true}`) +
`day-one-meta-puzzle.md §1.3` (the three locks) + `WEB-MASTER.md L297`.

The brief requires the meta to resolve ONLY when its prerequisites are met. The design names three real
locks (literacy; fall-order key; two physically-gated glyphs Brann-`P`/Orin-`E`) plus the corpus order-key
(FACT 5 + FACT 9). But the seed gates `meta-unkept` on a SINGLE flag, `iss_caught`. Two problems:
1. With S1-A unbuilt, even that single gate is inert — `meta-unkept` is open the moment metapuzzle_seed
   runs, i.e. submittable in M2 with a lucky guess of a common dictionary word (`unkept`), and
   `max_attempts:8` + the global bucket is the *only* thing standing between a guesser and the marquee
   "it was there the whole time" payoff firing early and cold. That is the worst on-camera misfire for
   this specific beat.
2. Even with S1-A built, `iss_caught` alone is too weak a proxy for "the player can actually read the six
   marks." `iss_caught` is set by the catch, which does not require literacy of the maker's-mark glyphs.
   The meta should gate on the **conjunction** that maps to its real locks.

**Fix:** set `meta-unkept.requires_flags` to the AND of the prerequisite flags the design already mints,
e.g. `{ rosetta_known: true, iss_caught: true, fall_order_known: true }` — where `fall_order_known` is set
by the FACT-9 node (`haunting-biography`) that supplies the fall-order key (mint the flag on that row;
it is the order-key provenance §4 already names). `rosetta_known` covers literacy; `iss_caught` keeps it
M4-staged; `fall_order_known` is the order-key. The two physically-gated glyphs (Brann/Orin) are in-world
verbs, not flags, and stay as world-build gating. This makes "resolves only when all prerequisites met"
literally true and kills the early-guess path. (Keep `max_attempts:8` as the anti-brute backstop.)

### S1-E. `meta-unkept` will throw the build the instant it goes active — two latent wiring defects
**Where:** `day-one-meta-puzzle.md §0.5` (calls these out) — partial status as of this pass:
- **Voice key:** PARTIALLY RESOLVED. `oracleMetaUnkept(fragment)` now EXISTS (`voice.ts:302`, type union
  L632) and the seed names it (`puzzles_seed.sql:937`). BUT `resolve.ts: speakOutcome` has **no
  `case 'oracleMetaUnkept'`** (only NextClue/DeadEnd/SideQuest/MainBeat/Lore). So the dedicated key falls
  through to `default → defaultLineFor('lore') → oracleLore(loreFragment(payload))`. It *does* speak the
  fragment (because the row supplies `voice_args.fragment`), so it is not silent — but the dedicated key is
  dead code and the routing is accidental. **Fix:** add `case 'oracleMetaUnkept': return
  voice.oracleMetaUnkept(loreFragment(payload));` to `speakOutcome`, OR (simpler, per the design's own
  recommendation) change the seed `voice_key` to `'oracleLore'` and delete the unused `oracleMetaUnkept`.
  Pick one; today the symbol exists but is unreachable.
- **Coverage classification:** STILL BROKEN. `meta-unkept` is **not** in `NON_CIPHER_KEYS`
  (`clue-specs.ts:246`). It is correctly not in `CLUE_SPECS` (it is a `[NO CODE]` acrostic). The moment
  metapuzzle_seed flips it `active=true`, `specsCoverageSelfTest` throws `UNCLASSIFIED: meta-unkept`.
  **Fix:** add `'meta-unkept': "P8 acrostic across the six maker's-mark framing glyphs (reading
  convention, not a forge transform); resolves as plain lore"` to `NON_CIPHER_KEYS`.

  Same latent throw applies to the two OTHER active non-cipher rows the seed's own §1008 comment flags:
  `reckoning-rosetta` and `base-docket-reread-auto` are active but **not** in `NON_CIPHER_KEYS` and not in
  `CLUE_SPECS` → `specsCoverageSelfTest` already throws on the current seed. Add both (reckoning-rosetta =
  digit Rosetta observation; base-docket-reread-auto = offline lore re-read). The integrator must run
  `npx tsx src/forge/specs.selftest.ts` against the live seed; it does not pass today.

---

## S2 — DIFFICULTY / FAIRNESS / WEB-DENSITY (too easy / too linear / weak herring)

### S2-A. The meta's "self-correcting ring-order" fairness receipt is UNVERIFIED — it may not actually fail in ring-order
**Where:** `day-one-meta-puzzle.md §1.2`, `canon-spine.md §8.1`, `WEB-MASTER L88-92`. The fairness guarantee
is: read in fall-order → `UNKEPT`; read in the tempting ring-order
(Bow·Offering·Kept-Light·Deep-Line·Ward·Covering) → a clean non-word, so a wrong order self-rejects. But the
six glyphs are mapped to keepers (Vaun=U, Mara=N, Sella=K, Orin=E, Brann=P, Iss=T) and the keeper→ring
position mapping is **never written down**, so no one has checked that the ring-order permutation of
{U,N,K,E,P,T} is actually a non-word. If ring-order happens to spell another real/near word, the "self-
correcting" claim is false and the puzzle is unfair (two plausible orders, both "work"). **Fix:** the
Finalize integrator must (a) write the explicit keeper→ring-position table, (b) compute the ring-order
string, (c) assert it is a non-word AND not an anagram a solver would accept, and (d) record it next to the
fall-order in canon so it cannot drift. If ring-order *does* spell something, re-assign which glyph rides
which keeper's mark (the glyphs are free framing — X1-safe — so this is a pure authoring move).

### S2-B. Coordinate answers lean unsigned/destination-word (correct) — but confirm NO active row accepts a raw signed coord
**Where:** `normalize.ts`/`AnswerNormalizer.java` (drop the `-`), `cipher-web.md §4.3`, the coord rows.

This is mostly RIGHT and is a credit to the seed authors: `iss-dead-shrine` and `stone-sella` accept
unsigned/direction-word forms (`west and down`, `south by the far water`), `threshold-coordinate` resolves
to a destination WORD per INV-COORD, and `coordEncode`'s own minus-sign handling round-trips losslessly in
code (the `X-0` / `-0` rejection and 32-bit-extreme self-tests are correct). The residual risk is only that
a *future* coord row could add a raw `-1280 64` answer that can never match (the `-` is normalized away).
**Fix (cheap guard):** extend `seedcheck.ts` to flag any `accepted_answers` value that, before
normalization, contained a `-` adjacent to digits (a signed coordinate that will silently never match) —
a one-line regex pre-check, surfacing the §4.3 rule as an executable test rather than prose.

### S2-C. The two "teaching" rungs (a1z26, morse) and Caesar shift-3 are very easy — confirm they are *early* only and have a hard sibling open alongside
**Where:** `cipher-web.md §1 P1/P10b`, the literacy-door table (`WEB-MASTER §`, two in-roads).

a1z26 ("number the alphabet") and Caesar shift-3 are near-trivial. That is fine *as Movement-I teaching
rungs* and the design explicitly wants shallow entry points — but the audit's job is to confirm they never
stand alone as the *only* open door at their gate (which would make a stretch feel like a step-ladder). The
literacy door does have two genuinely different-modality in-roads (icon-ring leap + a1z26 stave), which is
good. **Action for integrator:** verify in the live `active` set that at every movement boundary at least
one HARD node (book / vigenère / rail-fence / polybius / columnar) is open simultaneously with the easy
rungs, so difficulty never collapses to "do the easy one." No code change if true; this is a staging check.

### S2-D. Vigenère key `ISS` is short (3) and guessable — the Liar engine's whole tension rests on it; make the *wrong* application the cost, not the key length
**Where:** `clue-specs.ts: stone-iss-wall` (key `ISS`), `cipher-web.md §3` (the Liar engine).

A 3-letter Vigenère key on short text is brute-forceable, and the key is literally handed in-fiction
(`D09`: "the key is my own name"). That is *intended* — the difficulty is not the key, it's realizing the
warm reading is a wall. So the cipher is fine. The fairness risk is that `stone-iss-wall` carries
`max_attempts:6` on the warm reading while the name-as-key door is uncapped — but with S1-B unfixed, the
shared answer means the cap interacts with the collision unpredictably. Once S1-B lands, re-confirm the cap
is on the warm-reading row only and the catch on-ramp stays uncapped (the design intent, cipher-web §6).

### S2-E. Web-density is genuinely strong — note for the record
The anti-ladder proof (`cipher-web.md §2.3`) gives every gate ≥2 in-roads, the `dead_end` doctrine (§2.5)
supplies four acknowledged red herrings, and the Liar engine (§3) is a real one-key-two-doors move. The
density is NOT the problem; the problem is that the activation/resolution layer (S1) doesn't yet *enforce*
the sequencing the density assumes, and two of the herrings (S1-B) collide with live doors. Fix S1 and the
web reads as designed.

---

## S3 — CORRECTNESS NITS (real, lower blast radius)

- **S3-A. `parseSeedKeys` active-detection vs `metapuzzle_seed` UPDATE-only edits.** `specsCoverageSelfTest`
  parses `active` from the **row literal** in `puzzles_seed.sql`. `metapuzzle_seed.sql` flips nine rows to
  `active=true` via `UPDATE`, which the parser never sees — so the coverage test's notion of "active" is
  the *pre-activation* seed, missing the nine back-half rows (and `base-docket-reread`). Net: the coverage
  test can pass while the LIVE DB has active, unclassified rows (`meta-unkept`, etc.). The S1-E
  classifications fix the membership; but the **test still won't catch a future back-half row** because it
  reads only the base seed. **Fix:** have `specsCoverageSelfTest` also parse the `UPDATE … set active = true
  where puzzle_key in (...)` block(s) from `metapuzzle_seed.sql` and union those keys into the active set.
- **S3-B. `bookCipher` first-occurrence indexing is fragile to book edits.** `MARA_BOOK` is inlined in
  `clue-specs.ts`; the cipher encodes to the FIRST occurrence of each word. The X1 self-test pins the
  round-trip, so a build break is loud (good) — but flag that any edit to `MARA_BOOK` prose silently
  re-points the carved refs, and the in-world lectern books MUST be kept byte-identical to `MARA_BOOK` or
  the world and the card diverge (the §5 cross-surface promise). No code defect; a content-discipline note
  the integrator should pin in the world-build checklist.
- **S3-C. `polybius`/`a1z26`/`morse` THROW on digits/punctuation by design** — correct and well-documented,
  but it means an author who puts a digit in one of those plaintexts gets a build throw, not a player error
  (good). Confirm no active seed row routed to those ciphers has a digit in its plaintext (none in
  CLUE_SPECS today; they use caesar/book/atbash/substitution/vigenère, so this is latent-only).

---

## TOP DEFECTS (terse) + MUST-FIX for the Finalize integrator

1. **[S1-A] Build the `requires_flags` gate in `getOpenPuzzles` (repo.ts) + `Puzzle` type + arc_state read.**
   It is asserted by liar.ts/README/metapuzzle_seed but does not exist; without it the entire staged
   back-half (Iss chain, Seventh deep, meta) opens at once and all sequencing is fiction. Add the missing
   `activationReachabilitySelfTest`.
2. **[S1-B] Kill the cross-row answer collisions among active rows.** Remove `'the one who turned away'`
   from `prophet-wall-name`; de-duplicate `stone-sella`↔`seventh-shrine`; make the `base-docket-reread`
   twins mutually-exclusive (one active at a time). Add an `answerUniquenessSelfTest` (canon.ts) and a
   stable `ORDER BY` in `getOpenPuzzles`. This collision can soft-lock the catch (→ `iss_caught` → back half).
3. **[S1-E] Classify the active non-cipher rows or the build throws TODAY.** Add `meta-unkept`,
   `reckoning-rosetta`, `base-docket-reread-auto` to `NON_CIPHER_KEYS`. `specs.selftest.ts` does not pass
   against the live seed until this lands.
4. **[S1-D] Gate `meta-unkept` on the conjunction of its real prerequisites**, not `iss_caught` alone
   (e.g. `{rosetta_known, iss_caught, fall_order_known}`), so the marquee re-read can't fire early/cold.
   Mint `fall_order_known` on the FACT-9 (`haunting-biography`) row.
5. **[S1-E] Wire the meta voice line deterministically** — add the `oracleMetaUnkept` case to
   `resolve.ts: speakOutcome` (or switch the seed to `oracleLore` and delete the unused symbol).
6. **[S2-A] Prove the `UNKEPT` ring-order actually fails to a non-word** — write the keeper→ring-position
   table, compute the ring permutation, assert non-word, record it in canon. The fairness receipt is
   currently claimed but unverified; re-assign glyphs (X1-safe framing) if ring-order spells anything.

**Holding well (no change needed):** the 11 forge primitives + round-trip self-tests; `coordEncode`
digit-glyph scheme incl. `-0`/leading-zero/32-bit-extreme asserts and minus-sign handling; TS↔Java
normalizer parity (byte-for-byte); the X1 forge↔decode↔seed bind (`specsSelfTest`); the anti-ladder
≥2-in-roads web shape and the `dead_end` doctrine. The danger is entirely in the activation/resolution
seam, not the ciphers.
