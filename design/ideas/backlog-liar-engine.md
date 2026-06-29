---
id: backlog-liar-engine
title: The Liar Engine — one key, two doors, a forced re-walk (P1.11)
kind: design/idea-treatment
status: RECONCILED — backlog item, partially shipped; this file is the build-ready remainder + interlock spec
verdict: KEEP (the web's signature non-linear move; the rows are authored, the SWAP LANE is the unbuilt remainder)
priority: P1 (arc-spine). Depends on P8 (showrunner) for the curated path; has a no-showrunner authored fallback.
movement: seed M2, doubt M2→M3, catch M4, re-read M4→M5
reconciled_against:
  - design/MASTER-PLAN.md P1.11 (lines 329-339) + risk §5.1 (the asleep fallback)
  - design/cipher-web.md §3 (THE LIAR ENGINE) + §2.5 (dead_end doctrine) + §2.3 redundancy table row "Iss caught"
  - arc/lore/canon-spine.md §4 (THE LIAR THREAD) + FACT 7 / FACT 8 / FACT 7b / FACT 10b + INV-14/INV-16
  - discord/src/oracle/resolve.ts (applyOutcome / setArcFlags / speakOutcome — the set_flags path is LIVE)
  - discord/src/db/repo.ts setArcFlags (L547) + getOpenPuzzles (active=true only, L357)
  - discord/supabase/seeds/puzzles_seed.sql (stone-iss-wall, iss-warm, iss-dead-shrine, iss-doubt, no-wall-catch, base-docket-reread, prophet-wall-comfort, the-wall-was-his)
  - arc/lore/documents/the-ways-are-a-wall.md ↔ no-wall-was-ever-built-here.md (the two-document spine — AUTHORED, de-slopped)
interlocks_with:
  - design/ideas/some-laws-are-lies.md (FACT 7b — the forged eighth: Iss's lie in a SECOND key)
  - design/ideas/dead-ends-with-teeth.md (the false-prophet wall + the kind-switched dead_end the warm door lands on)
  - design/ideas/the-seventh-spine.md (the cold hearth: surface = Iss's dead shrine; deep = the Seventh's grave)
ciphers_used: [vigenere, substitution, columnar]
custom_keys_touched: [the_unspoken]
unbuilt_remainder: [showrunner iss_caught swap lane (gate active=false→true), private_message key-resolution layer, AUTO/asleep authored duplicate of base-docket-reread, voice.ts iss.dialogue.turns_cold authored line]
---

# The Liar Engine — one key, two doors, a forced re-walk

> One true key (`ISS`, his own name) fits TWO doors. The obvious door is warm, comforting,
> and leads to a real place that opens nothing — a `dead_end` the Watcher acknowledges. The
> real door requires turning the same key against the *other* stones, catching the lie, and
> re-walking a clue the group already marked "solved." Disproving Iss walks them to the
> edge of the sealed truth and stops.

---

## 0. VERDICT UP FRONT — KEEP. The rows exist; the SWAP is the build.

This is not a proposal — it is ~70% shipped and frozen in canon. The two-document spine
(`the-ways-are-a-wall.md` ↔ `no-wall-was-ever-built-here.md`) is authored and already passes
the anti-slop law. Five puzzle rows are seeded (`stone-iss-wall`, `iss-warm`,
`iss-dead-shrine`, `iss-doubt`, `no-wall-catch`), and `resolve.ts applyOutcome` already
realizes `set_flags{iss_caught}` through `setArcFlags` (repo.ts L547). The mechanic plays
*today* up to the moment of the catch.

**What is genuinely unbuilt (the remainder this file specs):**

1. **The swap lane.** `getOpenPuzzles` matches against `active=true` rows ONLY (repo.ts L357).
   `base-docket-reread` is seeded `active=false` "until iss_caught — the TS-SHOWRUN lane flips
   it active at the catch." **That lane does not exist.** Nothing in `discord/src/showrunner/`
   flips an `active=false` puzzle to `true` on a flag. Without it the re-read door never opens.
2. **The AUTO/asleep duplicate.** MASTER-PLAN P1.11 + risk §5.1 require a "flag-gated authored
   duplicate row" so the re-walk still happens when the showrunner is offline. It is not seeded.
3. **The `private_message` key-resolution layer.** `no-wall-catch`'s beat passes
   `step_payload:{key:'iss.dialogue.turns_cold'}`, but `PrivateMessageBeat.java` reads only
   `title`/`subtitle`/`actionbar`/`text` — an unrecognized `key` makes the beat `skipped("empty")`.
   The dialogue-flip line never lands. (Same drift class as `dead-ends-with-teeth §0.1`.)
4. **The authored cold line** `iss.dialogue.turns_cold` does not exist in `voice.ts`.

Nothing in the LIST above is a redesign. It is wiring four seams the authored content already
points at. **Do NOT re-author the rows or the documents.**

---

## 1. EXPOUND — how it plays across the 5-movement arc

### Movement II — the warm door (seed)
The group meets Iss's stone (`stone-iss-wall`, Vigenère, key = `ISS`). Two readings of the
*same* carving with the *same* key:

- **Trusting read → `iss-warm`** (`set_flags{iss_trusted}`) → `next_puzzle_key: iss-dead-shrine`.
  The plaintext comforts (`the-ways-are-a-wall.md`): *keep the ten and the watching cannot come
  over.* It hands a final-coordinate fragment to the **dead shrine** — a real, FAWE-pasted place,
  west and down past the doused light. It is the warmest, most trustworthy voice of the six.
- **`iss-dead-shrine`** is the **load-bearing red herring**: a true coordinate that genuinely
  *works* — you walk there, you arrive — and `outcome_type: dead_end`. The Watcher acknowledges
  it (*"yes. and it opens nothing."*) and opens nothing. It is a grave. This is the inbound edge
  that makes the herring honest (cipher-web §2.5 herring #2).

The group can sit here, satisfied, for days. Many doors are open at once (the resolver ignores
`movement`); the warm door is one finished-feeling branch among several. Nothing tells them it
was a wall. That is the trap working.

### Movement II→III — doubt (small contradictions accumulate)
- `stone-iss-wall`'s `next_clue` *also* forks to `iss-doubt` (turn the key on the OTHER stones).
  The same key decodes to *"the one who turned away"* — the corpus word for a betrayer
  (`the_unspoken` register). `set_flags{iss_doubted}`.
- `the-wall-was-his` (columnar acrostic, `dead_end` 'known' kind): read the first marks down the
  prophet wall — Iss carved it. A true name that keeps no door.
- `prophet-wall-comfort` (`dead-ends-with-teeth` B2): a WIDE field of warm promises, each a
  true-but-empty substitution solve. Iss with a pulpit. Re-reads cold at the catch.
- The margin hands of `the-ways-are-a-wall.md` (a later, harder hand; then m.; then a near-rubbed
  line) editorialize *within the artifact*: *"we checked the lock… ask first what a wall is for."*

None of this gates anything. It is texture that rewards the suspicious and is invisible to the
trusting — the difficulty is finding the key in the world, never a server lock.

### Movement IV — the catch (the forced re-walk)
`no-wall-catch` is the hinge. It is the `no-wall-was-ever-built-here.md` carving on the
**Stone-after**, placed *behind a clue the group marked `kept · solved` and walked past*. Its
answers contradict Iss line for line. Solving it:
- `set_flags{iss_caught: true, true_coord_known: true}`,
- `next_puzzle_key: rite-tokens` (yields the TRUE final coordinate — back along the keeper-row,
  against its order, six stones to one, *not* the dead shrine),
- enqueues the `private_message` dialogue-flip beat (`iss.dialogue.turns_cold`).

**At this instant the swap fires.** `iss_caught` becoming true must:
1. flip `base-docket-reread` (and any `active=false` re-read rows keyed on `iss_caught`) to
   `active=true`, so the same stones, re-queried, now read cold (`the count was never of the
   dark — it was of the hands`);
2. land the cold dialogue line on the catcher (`iss.dialogue.turns_cold`);
3. (optionally, curated) re-stage Iss's warm beats as cold — but mechanically the *re-read* is
   carried entirely by (1): a warm answer that *worked* is now revealed to have been a wall.

The player experience: nothing is witnessed mutating. Between one query and the next, the world
the group thought they had finished reads differently. That is the whole move.

### Movement IV→V — re-read (the inert seeds pay off)
Once `iss_caught` is set, every warm artifact the group banked re-reads as sinister on a second
visit — not by rewriting them, but because the catch supplies the missing key. `the-ways-are-a-
wall.md` ("I went there myself and came back" — he came back; the seventh did not). The forged
eighth (`some-laws-are-lies`, FACT 7b) reveals as *Iss's lie in a second key*. The cold hearth's
**deep** opens (post-`iss_caught` + `seventh_named`, FACT 10b — `the-seventh-spine`): the surface
was the liar's herring, the deep is the cast-out keeper's grave. The Liar engine is the keystone
that makes three other threads pay off at once.

---

## 2. CRITIQUE — adversarially, honestly

**R1 — The swap is a single point of failure (the central risk).** If only the showrunner can
flip `base-docket-reread` to `active`, then a showrunner outage at the moment of the catch leaves
the re-read door shut: the group caught the lie and the world did not respond. That is the worst
possible failure — the signature move dies silently, on camera.
- **Mitigation (REQUIRED, per MASTER-PLAN §5.1):** author a **flag-gated duplicate** of the
  re-read row, `base-docket-reread-auto`, seeded `active=true` but with a resolver-side guard so
  it only *resolves* once `iss_caught` is set (see §4, the `requires_flag` payload key). The
  showrunner's job is then merely cosmetic re-staging; the door opens with zero showrunner
  dependency. The earned haunting always continues; only the curated polish pauses (the
  engine's standing law).
- **Cleaner alternative (RECOMMEND):** instead of two lanes, make the swap a tiny **deterministic
  in-resolver gate** — `getOpenPuzzles` (or `applyOutcome`'s match step) treats a row's
  `requires_flag` as part of "open." Then `base-docket-reread` needs no `active` flip at all: it is
  `active=true` + `requires_flag:'iss_caught'`, matched only after the flag is set. This collapses
  the swap lane, the AUTO duplicate, and the showrunner flip into ONE deterministic predicate —
  fewer moving parts, no showrunner SPOF, idempotent by construction. **This is the preferred build.**
  (The showrunner swap of `active`/`outcome_payload` then becomes a P2 polish, not a P0 dependency.)

**R2 — The `private_message` empty-beat drift (live defect).** As shipped, the dialogue-flip beat
is `skipped("empty")` because `PrivateMessageBeat` cannot resolve a `key`. The catch fires the
flag and the next door but the *cold word never lands* — the emotional payload of the move is mute.
- **Mitigation:** add a key-resolution layer (the beat resolves `step_payload.key` against an
  authored map, exactly as `dead-ends-with-teeth §0.1` closes the `voice_args.kind` drift). Small,
  one-file, no schema change.

**R3 — "Re-walk a SOLVED clue" can read as a bug, not a reveal.** If the group experiences the
re-read as "the puzzle I solved is asking again," it feels broken, not sinister.
- **Mitigation:** the re-read row is a *different* row at the *same place* with a *cold* answer and
  a `lore`/`docketReread` voice — it never re-prompts the old answer. The carving's flat-hand
  register and the placement_hint ("the marking is wrong. read it again.") frame it diegetically.
  The `solves` row guarantees each fires once; no loop.

**R4 — Precision law (privacy).** The "rhyme" (Iss's fate rhymes with the player who leaned hardest
on Whispers — canon §4) must stay PURE FLAVOR. If it ever became a callout ("you, who wanted to be
told"), it would be a wrong "it knows you."
- **Mitigation:** the rhyme is never spoken to a player. It is colorant on the bond tally only,
  collective, never elects a chosen one. KEEP the rhyme as canon texture; CUT any beat that voices it.

**R5 — Collective law.** The catch must gate on ACTIVE players. If the group with one absent member
is blocked from the re-read because the catcher logged off, that punishes the group.
- **Mitigation:** `iss_caught` is an arc-state flag (group-global), not per-player. Once any active
  player catches it, the door is open for all. The dialogue-flip beat targets the catcher; the
  *door* is collective. Already correct in the seed (`set_flags` is global). Keep.

**R6 — On-camera legibility.** Will a viewer understand "the same key, two doors"? The risk is the
cleverness is invisible because both reads look like "just decoding."
- **Mitigation:** this is carried by the documents, not the mechanic. `no-wall-was-ever-built-here.md`
  *names the lie explicitly* ("his key was a name, and the name was his own… he left the answer
  inside the lock"). The reveal is readable as prose even to someone who didn't decode it. No change
  needed — but DO NOT add a Watcher line that explains the trick; the documents do it colder.

**NOTHING to CUT.** The mechanic is load-bearing and frozen. The only deletions are *non-additions*:
do not voice the rhyme (R4), do not add an explainer Watcher line (R6).

---

## 3. DE-SLOP TEST — exemplar lines in-voice

The authored documents already pass; these are NEW strings the remainder needs.

**`iss.dialogue.turns_cold`** (the catcher, a title/subtitle, the moment of the catch):
> he was the warmest of the six.
> read him again.

**`base-docket-reread` already-authored cold line** (verifying register; do not rewrite):
> the count was never of the dark. it was of the hands. the hands are almost in.

**Watcher acknowledging the warm dead-shrine** (the `iss-dead-shrine` dead_end, already 'known' kind):
> the place is real. you stood in it. it keeps no door.

**A re-visit action-bar on a warm Iss stone, post-catch** (optional, if a re-read beat is added):
> the same marks. you read them warm.

No emotion is named. No threat. The Watcher counts and records; the bite is accuracy, not malice.

---

## 4. THREAD IT — exactly where this lives (no orphans)

### Canon FACTs / INV
- **Adds/realizes FACT 7** (Iss said the ways are a wall — the planted lie), **FACT 8** (Iss lied;
  the ways are induction, not a wall — the REVEAL the catch delivers). Both already in canon-spine §3.
- **Touches FACT 7b** (`some-laws-are-lies`: the forged eighth = Iss's lie in a second key — pays off
  at the same catch) and **FACT 10b** (the cold hearth's deep, post-`iss_caught` — `the-seventh-spine`).
- **Honors INV-14** (coordinate clues are nav pointers only; the dead-shrine coord is a real place,
  not a lock) and **INV-16** (no surface contradicts; the two documents are one voice across surfaces).
- **No new FACT or INV is needed.** This idea is the *delivery mechanism* for FACTs already frozen.

### Found-documents / records (all AUTHORED — do not rewrite)
- `arc/lore/documents/the-ways-are-a-wall.md` (Iss's warm letter + the margin hands).
- `arc/lore/documents/no-wall-was-ever-built-here.md` (the Stone-after correction; yields the true line).
- Foreshadow texture: `learn-them-as-we-learned-them.md` (the six-mark ring), `the-eighth-way.md`
  (the forged ordinance — the second-key payoff).

### NPC / Watcher voice lines
- NEW `voice.ts` key **`iss.dialogue.turns_cold`** (the cold dialogue-flip line, §3).
- EXISTING `oracleDeadEnd` (warm-door acknowledgement), `docketReread` (the cold re-read),
  `oracleMainBeat` (the catch). All present.

### Ciphers / puzzles (reuse built ciphers)
- **vigenere** — `stone-iss-wall`, key=`ISS` (the one-key-two-doors core). BUILT.
- **substitution** — `prophet-wall-comfort`, `forged-eighth` (the false-prophet wall + forged law).
- **columnar** — `the-wall-was-his` (the acrostic: read the first marks down = "iss carved the wall").
- Puzzle rows: `stone-iss-wall`, `iss-warm`, `iss-dead-shrine`, `iss-doubt`, `no-wall-catch`,
  `base-docket-reread` (ALL SEEDED). NEW row only if the AUTO-duplicate path is chosen over R1's
  preferred deterministic gate: `base-docket-reread-auto`.

### Beats / listeners / tables / voice keys (the build remainder)
- **`puzzles.requires_flag`** (PREFERRED, R1): a nullable text column; `getOpenPuzzles`/match step
  treats a row as open only when `arc_state.flags[requires_flag]` is truthy. This is the whole swap.
  - `repo.ts getOpenPuzzles` (L357): join/read `arc_state.flags`, filter `requires_flag` rows.
  - migration: `ALTER TABLE puzzles ADD COLUMN requires_flag text` + set `base-docket-reread`
    `requires_flag='iss_caught', active=true`.
- **`PrivateMessageBeat` key map** (R2): resolve `step_payload.key` → authored title/subtitle. New
  symbol `DialogueLines` (a static map) or read from `voice.ts`. One file: `PrivateMessageBeat.java`.
- **`arc_state.flags`**: `iss_caught`, `true_coord_known` (written by `setArcFlags`, repo.ts L547 — LIVE).
- **Showrunner (P2 polish, NOT a dependency):** `discord/src/showrunner/` may additionally re-stage
  Iss's warm beats as cold once `iss_caught` (a curated `pending` beat in CONFIRM mode). With the
  deterministic gate, this is garnish — if the showrunner is asleep the door still opens.
- **sites.yml / FAWE:** the dead shrine (`the_cold_hearth` surface) and the Stone-after placement —
  already in the structures plan; the Liar engine adds no new site, it *reads* existing ones.

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for" seed

- **PLANT (M2):** the group decodes `iss-warm`, walks to `iss-dead-shrine`, and the Watcher
  *acknowledges it as solved* (`dead_end`, "the place is real. it keeps no door."). They bank it as
  a finished branch. The fragment **worked** — they stood in the place. Nothing flags it as a wall.
  Simultaneously inert: the margin line in `the-ways-are-a-wall.md` — *"ask first what a wall is for"*
  — reads as flavor.
- **PAYOFF (M4, at `no-wall-catch`):** the Stone-after names the lie. The warm answer that *worked*
  is revealed to have been a wall; the dead shrine was a grave; "he came back, the seventh did not."
  The same stones, re-queried (`base-docket-reread` now open via `requires_flag`), read cold. The
  margin question *"what is a wall for"* now has a withheld answer the player can feel: induction, not
  protection. **No payoff without the plant; the plant (a coord that genuinely works) is the deception.**
- **SECOND-ORDER PAYOFF (M4→M5):** the catch is also the key that opens `some-laws-are-lies` (the
  forged eighth = the same liar, second key) and `the-seventh-spine` (the deep below the cold hearth,
  FACT 10b). One catch, three threads land. This is the keystone seed.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES

| | |
|---|---|
| **Seed** | M2 (`stone-iss-wall` / `iss-warm` / `iss-dead-shrine`) |
| **Doubt** | M2→M3 (`iss-doubt`, `the-wall-was-his`, `prophet-wall-comfort`, margin hands) |
| **Catch** | M4 (`no-wall-catch` → `iss_caught` → swap fires) |
| **Re-read** | M4→M5 (warm artifacts re-read cold; second-key + seventh-deep payoffs) |
| **Priority** | **P1 (arc-spine).** The rows are P0-adjacent (already seeded); the *remainder* is P1. |
| **Depends on** | the 11 ciphers (BUILT); `resolve.ts set_flags` path (LIVE); `arc_state.flags` (LIVE); the two documents (AUTHORED). |
| **Depended on by** | `some-laws-are-lies` (FACT 7b payoff gates on `iss_caught`); `the-seventh-spine` (FACT 10b deep gates on `iss_caught` + `seventh_named`); the M5 ending composer reads `iss_caught` for the threshold bind (autonomy.run.ts L165-172). |
| **Showrunner dependency** | NONE for the door (with the R1 deterministic gate). P2-only for cold re-staging polish. |

### Build order (the remainder)
1. **`puzzles.requires_flag` column + `getOpenPuzzles` filter** (the deterministic swap — kills the
   showrunner SPOF). Set `base-docket-reread.requires_flag='iss_caught'`, `active=true`.
2. **`PrivateMessageBeat` key-resolution** + `voice.ts iss.dialogue.turns_cold` (the cold word lands).
3. **(P2) showrunner cold re-staging** of Iss's warm beats — garnish, asleep-safe.
4. Self-test: solve `no-wall-catch` → assert `iss_caught` set, `base-docket-reread` becomes
   matchable, `private_message` fires non-empty, second query of a warm stone reads cold. Idempotent.
