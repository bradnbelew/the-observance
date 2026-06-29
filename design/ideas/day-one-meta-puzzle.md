# Idea — The Day-One Meta-Puzzle (planted inert, legible only late)

> **One-line:** seed a meta-answer in plain sight in Movement I that nobody can read
> until the late game re-reads it. The "it was there the whole time" beat. Pure
> authoring discipline; near-zero new engine.
>
> **STATUS (2026-06-25): PARTIALLY BUILT.** Since this idea's first treatment, the build
> moved past it and canon absorbed it. Ground truth as of this rewrite:
> - `meta-unkept` is a **live seed row** in `discord/supabase/seeds/puzzles_seed.sql`
>   (movement 4, `active:false`, `accepted_answers:['unkept']`, `outcome_type:'lore'`,
>   `max_attempts:8`), tagged `thread_key='human'` in `thread_tags.sql`.
> - canon-spine.md now **OWNS** the acrostic as law: §1 (fall-order Vaun·Mara·Sella·Orin·
>   Brann·Iss), §8.1 (the two distinct "sixes"; the `UNKEPT` acrostic uses **fall-order**,
>   its glyphs "placed so they fail in ring-order — self-correcting").
> - **TWO REAL BUILD BLOCKERS remain (see §0.5):** the seed names a `voice_key`
>   (`oracleMetaUnkept`) that **does not exist** in `voice.ts`, and `meta-unkept` is **not**
>   in `NON_CIPHER_KEYS` — so `specsCoverageSelfTest` will throw the moment this row is
>   flipped `active:true`. Both fixes are one-liners; this treatment specifies them exactly.
>
> Read against: `arc/corpus/cipher-plaintexts.md` (the X1 bind), `discord/src/forge/
> clue-specs.ts` (CLUE_SPECS + NON_CIPHER_KEYS + specsCoverageSelfTest), `discord/src/
> voice.ts` (the OracleVoiceKey set + `oracleLore`), `design/cipher-web.md` §1 P8 (acrostic
> = `[NO CODE]`), `design/clue-web.md` §3, `arc/lore/canon-spine.md` §1/§3/§8.1.

---

## 0. THE HARD CONSTRAINT (read this first — it reshapes the whole idea)

The pitch as written ("the six keeper-stones' first glyphs spell a meta-answer") **is not
buildable as stated**, and the reason is load-bearing.

The six stones' decoded plaintexts are bound by the X1 round-trip guard (`specsSelfTest` in
`clue-specs.ts`). Their first letters are already fixed by the seed's `accepted_answers`:

| stone | BOUND PLAINTEXT (forge `plaintext`) | first letter |
|---|---|---|
| stone-vaun | `GIVE THE FIRST OF THE DEEP BACK TO THE DEEP` | **G** |
| stone-mara | `DESCEND AND BOW AT THE UNBROKEN LIGHT` | **D** |
| stone-sella | `SOUTH BY THE FAR WATER WHERE SHE DID NOT COME BACK` | **S** |
| stone-orin | `I THOUGHT IT SMALL IT WAS NOT SMALL` | **I** |
| stone-iss-wall | `THE ONE WHO TURNED AWAY` | **T** |
| stone-brann | *(flat lore today; not bound — NON_CIPHER_KEYS)* | — |

That is `G D S I T —`. It spells nothing; I cannot reorder it (the resolver ignores
movement — `clue-web.md §2.2`: "the six stones are a field, not a row"); and I cannot
change a first letter without changing the bound plaintext, which fails the build
(`specsSelfTest` (1)/(3)) *and* breaks the keeper voice each plaintext carries. The naive
acrostic is **dead on arrival.** Good. Killing it forces the better version, which never
touches an X1 plaintext.

**The fix: the meta-acrostic lives on a surface X1 does not guard — the carved plain-script
FRAMING, not the ciphered run.** `cipher-plaintexts.md` is explicit: each stone has a
**bound ciphered run** (X1-guarded, untouchable) and **carved framing** ("free prose —
rewrite those all you want"). We plant one glyph per stone *inside each keeper's existing
maker's mark* — the one non-sentence element on every stone. The bound plaintexts stay
exactly as bound. `clue-specs.ts` CLUE_SPECS, `specsSelfTest`, and X1 are untouched.

### 0.5 THE TWO BUILD BLOCKERS (current, real, must-fix-before-active)

These are not design risks — they are wiring defects already in the repo, latent because
the row ships `active:false`:

1. **Dead voice key.** The seed row carries `'voice_key', 'oracleMetaUnkept'`. The valid set
   is `OracleVoiceKey = oracleNextClue | oracleLore | oracleDeadEnd | oracleSideQuest |
   oracleMainBeat` (`voice.ts`). `oracleMetaUnkept` is not in it. The resolver degrades an
   unknown key to a default — so the carefully-authored payoff line would **silently not
   speak**, which on camera is the worst possible failure for *this specific beat*.
   **FIX (minimal, no new key):** the row already supplies `voice_args.fragment`, and
   `oracleLore(fragment)` already returns exactly that fragment. Change the seed's
   `voice_key` from `'oracleMetaUnkept'` → `'oracleLore'`. Zero new voice symbols. The
   prior treatment's "one new voice key" line item is **CUT** — `oracleLore` is the home.
2. **Unclassified row → coverage test throws.** `meta-unkept` is an acrostic (`[NO CODE]`,
   not a forge transform), so it is correctly **not** in CLUE_SPECS — but it is also not yet
   in `NON_CIPHER_KEYS`. `specsCoverageSelfTest` requires every *active* row be one or the
   other; the instant the showrunner flips `meta-unkept` to `active:true` at M4, the build
   throws `UNCLASSIFIED`. **FIX:** add to `NON_CIPHER_KEYS`:
   `'meta-unkept': "P8 acrostic across the six maker's-mark framing glyphs (reading
   convention, not a forge transform); resolves as plain lore"`.

> Both fixes ship together, *before* the row can go active. They are the entire code delta
> this idea still owes (see §7).

---

## 1. EXPOUND — the mechanic, the story, the play

### 1.1 What is planted, and where

Each keeper stone already carries a **maker's mark** — the lone non-sentence element, the
part of the stone that is the person and not the rite (`cipher-plaintexts.md`, per-stone
"maker's mark"):

- **Vaun** — the second column scored deep and left empty (his unstruck "given-back" tally).
- **Mara** — the corner line, half-rubbed: *i never went down. i only ever read the way down.*
- **Sella** — the seventh-marker instruction, smallest, a child's hand.
- **Orin** — the broken final stroke at *i —*, a stave begun and not finished.
- **Brann** — the one word repeated at the stone's foot, legible only under the lit beacon.
- **Iss** — the too-smooth closing mark, his agreement signed in a hand that never fought.

We add **one carved rune to each maker's mark** — cut in that keeper's own hand, reading as
one more piece of *that keeper's grief*, never as a puzzle token. The six glyphs, read in the
**fall-order canonized in `canon-spine.md §1/§8.1`** (Vaun, Mara, Sella, Orin, Brann, Iss),
spell the meta-answer. The word is the in-corpus, in-voice **`UNKEPT`** — the inverse of
"kept," the thing the abandonment made everyone (canon-spine §2: "not slain, but *unkept*"),
the binary at the heart of FACTs 6 / 10 / 12.

| fall-order (§8.1) | keeper | glyph | reads in-mark as (its grief alibi) |
|---|---|---|---|
| 1 | Vaun | **U** | one rune scored beside the empty second column — "what is owed, un-given" |
| 2 | Mara | **N** | her corner mark — *not* down; the rune for the descent she never made |
| 3 | Sella | **K** | scratched into the seventh-marker tally — the one *kept* back |
| 4 | Orin | **E** | the unfinished stroke reads as a half-cut **E** — the mark he did not finish |
| 5 | Brann | **P** | the first rune of the foot-word he repeated, lit only under the beacon |
| 6 | Iss | **T** | the smooth closing mark — agreement signed with one rune |

`U N K E P T`. Each glyph is **individually inert**: it reads as that keeper's hand, never
flagged, no counter, no Watcher acknowledgment of partial collection.

### 1.2 The self-correcting placement (canon §8.1 — the fairness guarantee)

Canon §8.1 adds a constraint the first treatment lacked: *"the glyphs are placed so they
fail in ring-order (self-correcting)."* This is the on-camera fairness receipt. There are
two "sixes" in this world (§8.1): **fall-order** (keepers) and **founders'-ring order**
(Bow·Offering·Kept-Light·Deep-Line·Ward·Covering — the most *available* ordering, the one a
literate-but-not-yet-FACT-9 group would reach for first). The glyphs are authored so that:

- read in **fall-order** → `UNKEPT` (a word; the intended solve).
- read in **ring-order** (the tempting wrong order) → a clean non-word (e.g. the same six
  letters permuted to nonsense). The group that guesses ring-order gets garbage and *knows
  it is the wrong order* — the puzzle tells them, without a hint, that they have the wrong
  key. They are pushed back to "what other order of six is there?" → fall-order → FACT 9.

This is why the order-key is load-bearing and *fair*: a wrong order self-rejects; the right
order is the keeper-chronology the group independently establishes.

### 1.3 How it stays illegible until Movement IV

Three locks keep it inert through Movements I–III; only M4 supplies all three keys:

1. **Literacy.** The marks are in the rune script; pre-Rosetta they are decoration. So a
   sharp Day-One player who screenshots everything sees six odd marks and no way to read
   them. This is the honest "it was in your footage the whole time."
2. **Order.** The stones are a field, not a row (deliberate). Six readable-but-scattered
   glyphs spell nothing in found-order. The reading order is the late key — **fall-order** —
   which the corpus only assembles in M4 via `m2-rhyme` (FACT 5: the stones are a *sequence
   of fates*) → `haunting-biography` (FACT 9: the dread had a biography). Before you grasp
   the keepers are a chronological chorus, you have no reason to order the glyphs by fall.
3. **Two glyphs are physically gated.** Brann's `P` reads only at night under the beacon
   (the existing `stone-brann` night gate, `cipher-web.md` P13); Orin's `E` reads only from
   the crouch, where the broken stroke resolves into a half-cut letter (the existing
   `stone-orin` crouch verb, P4). Two of six ride in-world verbs already built.

### 1.4 The Movement-IV payoff (the "oh, that was what that was for")

In M4, after `no-wall-catch` flips Iss warm→cold and the group has re-walked the stones
reading them *cold*, the staged `meta-unkept` row goes `active:true`. The cold Iss / the
Keeper states **the reading order, not the answer**: "read the keepers as they fell; read
what each one kept back." The group returns to their own screenshots and re-walks the six
stones in fall-order, reads the six maker's-mark glyphs, and assembles `UNKEPT` themselves.

Submitting `unkept` resolves as **`lore`** (no door — see §2). The Watcher confirms the
word was on the stones from the first day, in the keepers' own hands. The recontextualization:
the meta-word the group spent two weeks earning was in plain sight in Movement I. Every
stone said the same word; the word is what the player is at risk of becoming (FACT 6/10/12).

### 1.5 Why `UNKEPT` and not a coordinate / a spoiler

Tempting to make the meta-answer the final coordinate or FACT 15. Both are wrong. The
coordinate is already double-sourced (the catch + the polybius grid — `cipher-web.md §2.3`);
a third path would trivialize the catch or create a countable dependency, and it would
collide with **INV-14/INV-COORD** (a coordinate is a navigation pointer read at a
destination, never a typed answer). FACT 15 is SEALED and stated by no node (rule §6.2) — an
acrostic that *spells the twist* blurts it. `UNKEPT` is the sweet spot: a real in-corpus
word, pointing hard at the sealed truth without naming it, that **opens nothing** — pure
recontextualizing texture, which is exactly what an "it was there the whole time" beat is.

---

## 2. CRITIQUE — adversarial, honest

**RISK 1 (was fatal; now mitigated by construction): the X1 guard.** The pitch's literal
form is unbuildable (§0). The acrostic lives in the framing, never the bound run; CLUE_SPECS
and the X1 plaintexts are untouched. *Verdict: naive version CUT; framing version KEPT.*

**RISK 2 (NEW, current, real): the wiring is half-done and will throw.** Covered in §0.5.
Unlike a design risk, this is a defect in the repo today. *Mitigation: the two one-line fixes
in §0.5/§7, shipped before the row goes active.* This is now **the sharpest live risk** — not
a concept flaw but an un-finished wire that fails loud the instant M4 stages the row.

**RISK 3: orphaned-gimmick smell.** A meta-acrostic can read as a crossword smuggled in (a
mechanic with no narrative home — violates the consistency law). Mitigation: each glyph is
*first* a piece of its keeper's grief and only *incidentally* a letter (§1.1's alibi column).
**Test (must pass):** delete the meta-layer and every glyph still reads as legitimate keeper
texture. It does — each maps to an existing maker's mark + existing fact. The acrostic is the
keepers, separately and unknowingly, having carved the same confession. That is a home.

**RISK 4: countable ladder ("collect 6 glyphs").** Forbidden. Mitigation: **no counter, no
progress bar, no Watcher acknowledgment of partial collection.** The glyphs are never
enumerated in UI or voice. The only node is the single terminal `meta-unkept` row. Noticing
is not staged.

**RISK 5: precision / collective law.** No player-profiling here (world-knowledge, can't
misfire a wrong "it knows you"). The *reverse* risk — punishing an un-obsessive group — is
killed by `outcome_type:'lore'`: `meta-unkept` gates **nothing**, is reachable only after the
spine is essentially done (M4), and a group that never assembles it loses only a grace note.
A reward for the obsessive, never a tax on the casual. (Also clean against INV-12: permanence/
optional depth colors, never gates.)

**RISK 6: on-camera misfire / "the author cheated."** For a Wifies-style re-read this is the
best beat *or* the worst (if the audience feels the glyphs weren't really visible). Mitigation:
glyphs genuinely on-screen in M1 footage; genuinely readable once literacy is earned; order
key *derived* from the keeper chronology the player learns (FACT 5/9), **plus** the §1.2
self-correcting ring-order failure as the fairness receipt. **CUT** any version where the order
is arbitrary or hint-gated by a Whisper — that reads as a cheat.

**RISK 7: Brann is unbound** (`stone-brann` = flat lore, no cipher — NON_CIPHER_KEYS / B2).
His `P` has no forge node. Mitigation: the glyph is a **world-build carving on the framing**,
not a forge node — it rides his flat stone's night-legible foot-word and survives whether or
not P0-5 ever re-authors Brann as the railFence/beacon node. This idea does **not** block on
the Brann re-author.

**RISK 8: redundancy with the other acrostics.** The web already has a P8 acrostic (the
hidden keeper-name in `D04`, a `dead_end`) and FACT 7b's forged-eighth count uses the *ring*.
Three "six/acrostic" shapes risk samey. Mitigation: they are deliberately different — `D04` is
*one document, a name, dead-end*; FACT 7b is a *ring-count contradiction*; this is *six stones
across the whole arc, a confession-word, lore*. Author the `meta-unkept` line to nod at the
`D04` name ("you found the name in the record; this is older than a name") so they read as
escalation. **And §1.2 explicitly forbids them sharing a glyph layout** (canon §8.1: "the two
never share a glyph layout") — the ring-order vs fall-order distinction keeps `UNKEPT` and the
forged-eighth provably separate.

**SCALE-DOWN CALL:** **KEEP-SCALED.** Keep one meta-word (`UNKEPT`), one terminal lore node,
zero new ciphers, **zero new voice keys** (reuse `oracleLore` — the prior "new key" is cut),
six carved glyphs. Do not build a second meta-layer, a stego companion (P17), or a coordinate
payoff. The discipline *is* the feature; more machinery weakens it.

---

## 3. DE-SLOP TEST — exemplar prose (in-voice, cold, concrete)

The `meta-unkept` payoff line (record register — flat, declarative, no emotion named, no bow
tied). This is the `voice_args.fragment` the seed already carries, tightened:

> the marks were cut before you came. you have read them since the first stone and called
> them grief. read the keepers in the order they fell. read what each one kept back. the
> word is unkept. it was always the word.

Vaun's planted-glyph framing (inventory, never feeling — carries the `U`):

> the second column i scored and did not fill. one mark beside it, for what is owed. i kept
> the count. i did not keep the giving.

Sella's (child's hand, flat-wrong — carries the `K`):

> i made a mark for the one kept back. it is smaller than the others. i did not want it to
> be the last one. it is not the last one.

Orin's (the broken stroke that reads as a half-cut `E` — terse, ashamed, breaks off):

> i started the next mark. it is the right mark. i did not finish it. i —

(Each line: concrete object, no adjective stacks, no "testament," no announced feeling; the
dread is in the omission. Passes the anti-slop law. Voices honored — Vaun only of what he
kept; Sella flat-wrong; Orin fewest words, breaks off — §6.8.)

---

## 4. THREAD IT (the consistency law) — exactly where this lives

**Canon FACTs touched (no new FACT minted; it re-expresses existing ones):**
- Adds nothing to the SEALED spine. Strengthens **FACT 6** (kept/left is a real binary),
  **FACT 10** (the land can refuse — `unkept`), **FACT 12** (the kept did not depart) by
  making the *inverse word* physically present on every stone from M1.
- **Already in canon (do not re-mint):** the acrostic is owned by `canon-spine.md §1` (the
  fall-order) and **§8.1** (the two sixes; `UNKEPT` uses fall-order; glyphs fail in ring-order,
  self-correcting; "the two never share a glyph layout"). The anti-orphan record exists — this
  treatment must not duplicate it, only realize it in the per-stone framing.
- **Order-key provenance:** the fall-order is planted via **FACT 5** (`m2-rhyme`) and paid off
  via **FACT 9** (`haunting-biography`) — so the *key itself* is planted-and-paid, not arbitrary.

**Found-documents / journals that foreshadow it (two doors, web rule):**
- `m1-record-opens` (D01) already buries "the seventh mark the record will not [...]"; add a
  parallel buried tease — a damaged line about marks "kept back" / "un-given" — so a re-reader
  finds the word's shadow in the very first document.
- `self-rewriting-journal` (the M1–M4 lore-drip base-lectern book) is the natural second door:
  across nights a page slowly resolves toward "each stone keeps one mark back," never stating
  the answer.

**NPC / Watcher voice that carries it:**
- The payoff is spoken via **`oracleLore`** with the seeded `voice_args.fragment` (§3 line),
  by the cold Iss / Keeper at M4. **No new voice key** (the seed's `oracleMetaUnkept` is the
  bug to fix, not a key to add — §0.5).

**Cipher(s)/puzzle(s) that express it:**
- **P8 acrostic** (`cipher-web.md §1`, `[NO CODE]` reading convention) — reused exactly:
  "lay lines/marks whose first glyphs spell the answer; decode = take initials; forged as an
  authored placement, not via `ciphers.ts`." No new cipher, no forge change, X1 untouched.
  This is the catalog's anticipated second P8 instance (the first is the `D04` name).
- `unkept` resolves through the existing `normalizeAnswer` path like any `accepted_answers`
  string — no resolver change.

**Beat class / listener / table / seed / sites / voice realizing it:**
- **Seed:** `meta-unkept` **already exists** in `puzzles_seed.sql` (movement 4, `active:false`,
  `accepted_answers:['unkept']`, `lore`, `max_attempts:8`, `thread_key='human'`). The only edit
  is the `voice_key` fix (`oracleMetaUnkept` → `oracleLore`).
- **clue-specs.ts:** add `meta-unkept` to **`NON_CIPHER_KEYS`** (the missing classification —
  §0.5). Keeps `specsCoverageSelfTest` green once active. CLUE_SPECS / `specsSelfTest` / X1
  untouched.
- **Sites / world build:** six maker's-mark glyph additions, authored as free prose in the
  `cipher-plaintexts.md` per-stone framing blocks, realized in resource-pack rune textures +
  block placement (each cut so fall-order → `UNKEPT`, ring-order → nonsense, §1.2). No
  `sites.yml` node (the glyphs ride existing stone placements).
- **Doc threading:** `clue-web.md §3` (M4 lore node "the day-one word") + §4 lore inventory;
  `cipher-web.md §1 P8` second-instance note. (canon-spine already done.)

---

## 5. PLANT THE PAYOFF — the seed → payoff ledger

| | detail |
|---|---|
| **PLANT (inert)** | Six maker's-mark glyphs (`U N K E P T`), one per keeper, cut in each keeper's hand as part of their existing grief-mark. Visible from **Movement I/II** (as each stone becomes reachable). Individually inert: reads as keeper texture, never flagged, no counter. |
| **WHY it stays inert** | (1) needs literacy to read as letters (M1/II); (2) needs **fall-order** to sequence them, assembled only at M4 via FACT 5/9 — and ring-order (the tempting order) self-rejects to nonsense (§1.2); (3) two glyphs (Brann `P`, Orin `E`) ride existing night/crouch verbs. |
| **PAYOFF (legible)** | **Movement IV**, node `meta-unkept` flips `active:true`: cold Iss / Keeper states the fall-order key (not the answer). Group re-reads the six stones in fall-order, assembles `UNKEPT`, submits `unkept` → `lore` payoff: the Watcher confirms it was carved before they came. |
| **The re-read** | every stone recolored — six keepers, separately, carved the one word they each failed to keep; the word is what the player risks becoming (FACT 6/10/12). The "oh, *that's* what those marks were." |
| **Closed pair** | the six glyphs (plant) ↔ `meta-unkept` (payoff) are a closed pair. The order-key (fall-order) is itself planted via FACT 5 and paid via FACT 9 — the *key* is also planted-and-paid, not arbitrary. No plant without payoff; no payoff without plant. |

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES

- **Lives in:** plant in **Movement I–II** (the six stone surfaces, as each becomes
  reachable); payoff in **Movement IV** (`meta-unkept`, staged `active:false` until then).
- **Depends on (must exist first):**
  - the two §0.5 wiring fixes (voice_key → `oracleLore`; `NON_CIPHER_KEYS` entry) — **before**
    the row can go active.
  - `rosetta-ring` literacy (M1/II) — to read the glyphs at all.
  - the six keeper-stone surfaces in the world build (maker's-mark framing on each).
  - FACT 5 (`m2-rhyme`) and FACT 9 (`haunting-biography`) readable — they supply the fall-order
    key (narrative dep, no code dep).
  - `stone-orin` crouch + `stone-brann` night legibility (already built) for the `E` and `P`.
- **Depends on it:** nothing. Pure leaf (`lore`, gates nothing) — by design; the collective /
  anti-ladder laws forbid the finale depending on a meta-puzzle.
- **Priority: P2 (depth).** Not vertical-slice (P0), not arc-spine (P1) — the arc is whole
  without it. Exactly the high-payoff, low-cost depth a hard-ARG group and an ARG-critic video
  live for, at near-zero engine cost. **Caveat:** the two §0.5 fixes are tiny but are
  *correctness* work, not depth — fold them into the M4 staging pass so a half-wired row never
  ships active.

---

## 7. NEW CODE SYMBOLS (the complete, minimal manifest — current)

The first treatment over-counted. Build reality: the seed row, tags, and canon already exist.
The remaining delta is **two one-line fixes + world-build glyphs + doc threading**:

- `discord/supabase/seeds/puzzles_seed.sql` — **edit** the existing `meta-unkept` row:
  `voice_key` `'oracleMetaUnkept'` → `'oracleLore'` (keep its `voice_args.fragment`). No new row.
- `discord/src/forge/clue-specs.ts` — **add** one `NON_CIPHER_KEYS` entry:
  `'meta-unkept': "P8 acrostic across the six maker's-mark framing glyphs (reading convention,
  not a forge transform); resolves as plain lore"`. (Makes `specsCoverageSelfTest` pass when
  active; X1 / CLUE_SPECS / `specsSelfTest` untouched.)
- `arc/corpus/cipher-plaintexts.md` — six per-stone framing edits adding the planted glyph to
  each maker's mark, cut so fall-order → `UNKEPT` and ring-order → nonsense (free prose, X1-safe).
- `design/clue-web.md` §3 + §4, `design/cipher-web.md` §1 P8 — doc threading.
- **NO** new voice key (`oracleLore` is the home), **NO** new cipher, **NO** `ciphers.ts`
  change, **NO** resolver change, **NO** new beat class, **NO** new listener, **NO** plugin
  change, **NO** canon-spine edit (already owns it).
