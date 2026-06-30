# Teardown — PUZZLES & CIPHER DESIGN

Adversarial critique of the puzzle/cipher dimension of The Observance. Read against the
REAL files: `discord/supabase/seeds/*.sql`, `discord/src/forge/ciphers.ts` +
`clue-specs.ts` + `runes.ts`, `discord/src/oracle/resolve.ts` + `normalize.ts` +
`seedcheck.ts`, `discord/src/db/repo.ts`, `design/cipher-web.md`. The design guides
over-claim; the seed + resolver are the truth. Findings worst-first.

---

## FINDINGS (prioritized)

### 1. [FIX] `getOpenPuzzles` (repo.ts:361) never reads `requires_flags` — the ENTIRE back-half gate is a no-op
`getOpenPuzzles` filters `.eq('active', true)` and selects only 8 columns —
`requires_flags` is neither selected nor AND-tested. `metapuzzle_seed.sql §2` sets
BOTH `requires_flags` AND `active=true` on `bound-word`, `m4-three-hands`,
`threshold-coordinate`, `true-walk-arrive`, `seventh-unwriting`, `seventh-cause`,
`seventh-choice`, `base-docket-reread`, `base-docket-reread-auto`, `meta-unkept`. Result:
the moment that seed runs, **every one of those "gated" puzzles is wide open from minute
zero** — the player can solve `threshold-coordinate` / `seventh-choice` / `meta-unkept`
before catching Iss, before naming the Seventh, before anything. The sequenced Iss chain,
the Seventh restore/erase choice, and the UNKEPT meta all lose their gating. This is the
single most damaging puzzle bug in the build.
**Fix:** add `requires_flags` to the select, then filter `open` to rows where every
`requires_flags` key is truthy in `arc_state.flags` (one `getArcFlags()` read +
`Object.entries(rf).every(([k,v]) => flags[k] === v)`), exactly as `metapuzzle_seed`'s
header comment already assumes. Until this lands, treat the whole staged back half as
ungated.

### 2. [FIX] Cross-row answer collision: `'the one who turned away'` is a primary answer on THREE active rows
`stone-iss-wall` (L199), `prophet-wall-name` (L661/663), and `bound-word` (L805) all
accept `the one who turned away`. `matchPuzzle` (repo.ts:380) returns the **first** open
row by DB order — so one solve silently fires whichever row sorts first, records the solve
against the WRONG puzzle_key, speaks the wrong voice line, and sets the wrong flags
(`iss_key_turned` vs `bound_word_known`). Combined with finding #1 (bound-word open from
the start), a player who decodes Iss's stone on day one can accidentally consume the M4
`bound-word` node. `iss` also collides as a bare answer on `stone-iss-wall` (L200).
**Fix:** make the shared plaintext belong to exactly ONE row; give the others
disambiguated answers (`prophet-wall-name` → only `iss carved the wall` /
`read the first marks down`; `bound-word` → only `the bound word is his name` /
`turned away`). Never let two simultaneously-open rows share a normalized string.

### 3. [FIX] The 11 UNCLASSIFIED specscheck rows — which are actually BROKEN vs merely unregistered
`specsCoverageSelfTest` fails because 11 active rows are in neither `CLUE_SPECS` nor
`NON_CIPHER_KEYS`. Triage:
- **Genuinely non-cipher, just need a `NON_CIPHER_KEYS` line (cheap, not broken):**
  `prophet-wall-comfort`, `prophet-wall-name`, `name-where`, `difficulty-mara`,
  `record-url`, `fork-light`, `fork-name`, `forged-eighth`. These are lore/observation/
  fork/sentinel nodes with no Discord-decodable carving. Add each with a reason.
- **Claims a cipher but has NO forge spec → actually broken as a puzzle:**
  `a1z26-tick-stave` (header says "a tick-stave / a1z26") and `reckoning-rosetta`
  (digit-literacy) are sold as cipher rungs but neither is in `CLUE_SPECS`, so no carved
  artifact is ever forged for them — they exist only as typeable phrases. Either add real
  `a1z26`/digit `CLUE_SPECS` entries (the forge supports a1z26) or honestly demote them to
  `NON_CIPHER_KEYS` as "phrase-only rungs."
- **`base-docket-reread-auto`:** non-cipher lore; add to `NON_CIPHER_KEYS`. But it is the
  idempotent twin of `base-docket-reread` with an IDENTICAL answer set — see #4.
**Fix:** 9 `NON_CIPHER_KEYS` additions + a decision on the 2 fake-cipher rungs; that turns
specscheck green.

### 4. [SIMPLIFY] `base-docket-reread` ↔ `base-docket-reread-auto` are duplicate puzzles with identical answers
Both rows carry the exact same four `accepted_answers` (L600-603 ≡ L1098-1101) and both
speak `docketReread`. The design rationale (showrunner-flip vs requires_flags-auto) is an
ENGINE concern, but as PUZZLES they are indistinguishable and — once #1 is fixed —
whichever opens first eats the solve; the other can never be solved by a second player
because the answer is gone from the open set after the first. This is fake redundancy that
buys nothing a player can perceive.
**Fix:** collapse to ONE row gated by `requires_flags {iss_caught}` (the deterministic
lane), delete the showrunner-flip twin. The SPOF argument is moot once requires_flags
actually works (#1).

### 5. [FIX] `rosetta-ring` accepted answer contradicts its own teaching surface (the literacy gate is unfair)
The row accepts `bow offering kept light deep line unspoken sacred beast` (L86), but
`cipher-web.md §1 P11` and the server-icon ring teach the six customs as
`bow offering kept light deep line WARD COVERING`. The seed comment (L82-84) admits
`ward`/`covering` were "orphans" and swaps in `the_unspoken` + `the_sacred_beast` — but
nothing in the carved ring teaches those two words. This is THE literacy gate: a player
reads six glyphs off the ring, types what the ring spells, and is told silently they are
wrong because the last two words were changed in the DB but not on the stone. Pure
moon-logic; the world teaches one answer and the resolver demands another.
**Fix:** make the carved ring glyphs and the accepted answer the SAME six words. If the
canon is now `unspoken`/`sacred beast`, the GO-LIVE note (L84) must be done BEFORE this
ships, and the design doc updated; otherwise revert to `ward covering`.

### 6. [FIX] `meta-unkept` acrostic — single-word `unkept` with `max_attempts:8` is trivially brute-forced and teaches nothing in-world
The meta-acrostic's only accepted answer is `unkept` (L933). The intended solve — read the
six maker's-mark framing glyphs in FALL-ORDER (Vaun, Mara, Sella, Orin, Brann, Iss) — is
described in prose only; there is no in-game surface that says "these six marks spell a
word" or which order to read them. A friend group will never derive the fall-order reading;
they will either guess a common 6-letter dictionary word or never touch it. `max_attempts:8`
caps brute force but a thesaurus lands it. With #1 broken it is also open from day one,
long before the M4 framing that's supposed to motivate it.
**Fix:** add a real teaching surface (the cold-Iss/keeper line at the catch must actually
state the fall-order key in-world, and the six marks must be visibly grouped as an
acrostic), gate it behind `iss_caught` (needs #1), and accept the in-fiction phrasing too
(`the word each did not keep`), not just the bare answer.

### 7. [CUT] `name-where`, `m2-rhyme`, `haunting-biography`, `difficulty-mara` are "type the theme back at me" non-puzzles
These rows have no cipher, no carving, no in-world act — the "solve" is typing a sentence
that paraphrases a feeling the player is supposed to have had (`the dread had a biography`,
`their fates rhyme with the ways they broke`, `the record keeps a closer count of the
quick`). There is zero deterministic path from the world to those exact strings; the only
way to "solve" them is to read the answer in a design doc. A real friend group will never
type these unprompted. They are lore the Watcher should SPEAK on a trigger, not gated
phrases a player must guess.
**Fix:** convert to showrunner-pushed lore beats (the watcher volunteers the line at the
right moment) and remove them from the typeable puzzle set, OR give each a concrete prompt
("two stones, read together — what rhymes?") that names what to type. As authored they are
fake puzzles.

### 8. [REDESIGN] The three opaque-sentinel rows can NEVER fire because their listeners do not exist
`accepting-crouch` (L501), `record-receives` (L528), `seventh-choice` (L735),
`m4-three-hands` (L827), and `threshold-coordinate`'s gate all depend on the plugin posting
an opaque token (`k7q2m9 x4r8p3 …`) on real in-world detection. The ground-truth says the
arming listeners (`IgnitionListener`, `CoopPlateListener`, `SeventhChoiceListener`, etc.)
DO NOT EXIST. So the climax (the synchronized bow), the co-op gate, and the Seventh choice
are unsolvable by any means — no human can type the sentinel (by design, no-leaked-
sentinel), and nothing posts it. The entire Movement-V terminal is inert.
**Fix:** these are the keystone listeners; until they exist the ending cannot be reached.
Prioritize `CoopPlateListener` (the bow detector) — it is the single point of failure for
the climax. Everything else in M5 is downstream of it.

### 9. [FIX] `m4-three-hands` co-op gate: the AND-join lives in `applyOutcome` but that path can't implement a 20s 3-leg window
The seed comment (L817-823) says the three-leg AND-join (foot-plate + carve + Discord post
in a ~20s window) "lives ONCE in resolve.ts (applyOutcome of this puzzle)." But
`applyOutcome` (resolve.ts:308) only runs AFTER a single answer already matched the
puzzle's `accepted_answers` — it has no concept of three legs, a time window, or
conjunction. There is no code that counts legs or enforces the window; the row will fire on
the FIRST submission of its opaque token, making it a one-hand gate mislabeled as three.
**Fix:** the leg-counting + window logic must live in the plugin's `CoopPlateListener`
(which posts the token only once all three legs are seen in-window), NOT in `applyOutcome`.
The seed comment describes a design that the resolver architecture can't host. Decide where
the conjunction lives and write it there.

### 10. [KEEP] The five real keeper-stone ciphers (Caesar/book/atbash/subst/Vigenère) are solid and fair
`CLUE_SPECS` binds `stone-vaun` (Caesar 3), `stone-mara` (book), `stone-sella` (atbash),
`stone-orin` (substitution), `stone-iss-wall` (Vigenère key=ISS), and `specsSelfTest`
proves forge→decode→normalize→accepted_answers for each. The keys are in-corpus (Vaun's
"three of each", the lectern shelf, the water-mirror, the rune ring, Iss's own name). This
is the strongest part of the dimension. The Liar/Vigenère catch (one key, two doors:
`stone-iss-wall`→`iss-doubt` vs `iss-warm`→`iss-dead-shrine`) is genuinely clever.
**Keep**, with the caveat that the warm/cold split (iss-warm) is currently a SEPARATE
typeable phrase, not a re-application of the cipher — see #11.

### 11. [SIMPLIFY] `iss-warm` is not actually "the warm reading of the Vigenère" — it's a hand-typed phrase
`iss-warm` accepts `the ways are a wall against the watching` (L218). There is no forge
spec for it; it is NOT produced by applying key=ISS to anything. So the "two readings of
one key" — the whole point of the Liar engine — is faked: the player doesn't decode two
ways, they type two different sentences that happen to be seeded. A sharp group will never
discover that trusting Iss's own letter yields the warm reading, because the cipher doesn't
actually produce it.
**Fix:** either forge the warm plaintext as a real `vigenere` decode of Iss's own carving
(so the duality is real and discoverable) or stop advertising it as a cipher duality and
present it as the in-fiction "if you read it trustingly" framing on the SAME stone.

### 12. [FIX] `forged-eighth` / `prophet-wall-comfort` / `prophet-wall-name` claim substitution solves but have no carved ciphertext
The seed comments call these "substitution row," "true-but-empty substitution solve,"
"columnar acrostic" — but none is in `CLUE_SPECS`, so the forge never produces a carving
for them and `specsSelfTest` never proves they decode. They are typeable phrases dressed as
ciphers. `prophet-wall-name` even claims a columnar acrostic down the first letters of the
warm rungs (the engine `columnar` exists) but is wired as a plain phrase match.
**Fix:** if the columnar acrostic is real, register `prophet-wall-name` as a `columnar`
`CLUE_SPECS` entry keyed on Iss's name so the build proves it; otherwise demote all three
to `NON_CIPHER_KEYS` and stop the comments from implying a cipher that isn't carved.

### 13. [KEEP] Normalization + coordEncode handle the minus-sign trap correctly
`normalizeAnswer` strips everything but `[a-z0-9 ]`, so `-1280` → `1280` and acrostic
capitalization is destroyed. This IS a real trap, but the seed avoids it correctly: NO
coordinate row stores a signed coord; coord nodes (`iss-dead-shrine`, `stone-sella`,
`threshold-coordinate`) store DESTINATION WORDS (`west and down`, `south by the far water`,
`follow the threshold mark`) per the INV-14/INV-COORD rule. `coordEncode` is a carve-side
display cipher only; its `-`/`,` output never has to survive normalization. `seedcheck`
enforces every answer is pre-normalized. This is handled well — the one place the trap
could bite (acrostic-in-the-bound-run) was explicitly CUT for `meta-unkept` (L926-928).
**Keep.**

### 14. [SIMPLIFY] The opaque sentinels (`k7q2m9 x4r8p3 …`) are six 6-char tokens — needlessly long, and a copy-paste leak from logs would fire the climax
Each sentinel is ~40 chars of random base36. They're un-guessable (good) but if any one
ever appears in a log, a Discord audit message, or the dashboard, pasting it into `/answer`
fires the terminal beat with no in-world act. The resolver has no surface check that a
sentinel came from the plugin vs a player.
**Fix:** the resolver should accept sentinel-class answers ONLY from the plugin surface
(`surface === 'world'` or a plugin-authenticated path), never from Discord `/answer`. Right
now `resolveAnswer` treats both surfaces identically, so a leaked token is a typed shortcut
past the climax.

### 15. [FIX] `seventh-shrine` accepts `7` and `seven` — collides with any future numeric answer and is brute-trivial
`seventh-shrine` (L362-366) accepts bare `seven` and `7`. Any other puzzle whose answer
normalizes to `7`/`seven` would collide, and a player idly typing numbers hits it. It also
shares `a thing that can say no is not a wall` with `seventh-cause` (L711) — another
two-open-rows collision once #1 is fixed and both are active.
**Fix:** drop the bare `7`/`seven`; keep the phrase answers. De-dup the shared
`a thing that can say no is not a wall` line to one row.

### 16. [SIMPLIFY] `stone-orin` accepts the bare word `threshold` — a one-word guess into a lore gate
`stone-orin` (L165) lists `threshold` as an accepted answer alongside the full decoded
sentence. A substitution cipher whose plaintext is a 9-letter sentence should not also be
solvable by typing one common word a player will guess from context. It cheapens the only
crouch-to-read cipher.
**Fix:** remove `threshold`; require the decoded sentence (or the in-fiction
`the bow is the smallest of the ways`).

### 17. [CUT] `pressure-glyph-walk` and `fork-light`/`fork-name` add three "side_quest" rows that gate nothing and will be invisible
`pressure-glyph-walk` is a second in-road to `accepting-crouch` that "GATES NOTHING" (the
true walk reaches it anyway); `fork-light`/`fork-name` set color-only flags read solely by
the (nonexistent — #8) M5 composer. For a friend group that "will MISS most subtlety,"
these are pure overhead: a player who solves `walk the rune` gets a side_quest voice line
and no perceivable consequence, and the fork flags never surface because the composer that
reads them isn't built.
**Fix:** until the M5 composer exists, cut `fork-light`/`fork-name` (dead color flags) and
keep `pressure-glyph-walk` only if the floor-rune walk is actually built and detectable;
otherwise it's a phrase no one will type.

### 18. [FIX] `stone-brann-cipher` and `reckoning-rosetta` reference site_ids/advancements that may not exist; the rail-fence is staged but uncovered
`stone-brann-cipher` (L1056, active=false) is the "real" rail-fence Brann node but is
explicitly NOT in `CLUE_SPECS` and depends on TS-FORGE adding a `railFence` entry + a
night-reading gate that isn't built. `reckoning-rosetta` (L1018) is active=true, claims to
teach digit glyphs, but has no forge spec and its `advancement_toast` payload uses a
non-standard shape (`advancement`/`fallback_title` inline) the metapuzzle_seed has to
rewrite. The digit-literacy it teaches is a prerequisite the seed comments lean on
(`reckoning_known`) but no coord row actually requires it (and can't, given #1).
**Fix:** decide if digit literacy is real; if so, gate the coord rows on `reckoning_known`
(via working requires_flags) and build the Stone of Reckoning surface; if not, cut
`reckoning_known` plumbing as aspirational.

---

## IF I COULD CHANGE ONE THING

**Make `requires_flags` actually enforced in `getOpenPuzzles` (finding #1), then ruthlessly
cut the ~10 phrase-only "type-the-theme-back" rows (#7, #11, #12, #17) down to the things a
player can actually DECODE or DO.** Right now the dimension has five excellent, fair,
build-proven ciphers (the keeper stones) drowning in ~15 rows that are either (a) lore the
Watcher should just speak, or (b) gated puzzles whose gate does nothing because the resolver
ignores `requires_flags`. The build's own `specsCoverageSelfTest` is screaming that 11 rows
are unclassified — that's the symptom; the disease is that the puzzle SET grew faster than
the engine + world that make a puzzle solvable. Enforce the gate, then keep only rows that
pass the test "could a friend who reads the world, but not the design doc, ever produce this
exact answer?" Most of the back half fails that test today.
