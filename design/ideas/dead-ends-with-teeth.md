# Dead-Ends With Teeth — design treatment

> IDEA: author `dead_end` ciphers that decode to a *taunt* — the Watcher acknowledging you
> solved a thing that meant nothing — plus a **false-prophet** thread. Red herrings that
> talk back, without ever blocking progress.
>
> READ-FIRST grounding (verified against shipped code this pass):
> `discord/src/oracle/resolve.ts` (`speakOutcome` line 359; the `oracleDeadEnd` case at L365
> calls `voice.oracleDeadEnd()` **with NO arg** and never reads `voice_args`; dead_end path
> carries **voice_key only**, no `next_puzzle_key`/`beat`),
> `discord/src/voice.ts` (`oracleDeadEnd()` L177 — today ONE static line, NO `kind` param),
> `design/cipher-web.md` §2.5 (the dead_end doctrine) + §3 (the Liar engine),
> `discord/supabase/seeds/puzzles_seed.sql` (the SEED IS ALREADY AHEAD OF THE CODE — see §0.1),
> `arc/lore/documents/the-ways-are-a-wall.md` (Iss's doctrine = the prophet's wall source text).

---

## 0. VERDICT UP FRONT

**KEEP, SCALED — and there is a LIVE DRIFT DEFECT to close first.**

The taunt-variety half is a clean, low-cost, high-ROI win. The **false-prophet thread is KEPT
but is Iss-content, never a second NPC** (canon law: one liar — Iss). Both halves are already
*authored in the seed*. They are **mute** because the voice + resolver layers were never updated
to carry them. Closing that gap is the build task; the design below is mostly already-shipped seed.

The single thing to **CUT**: any taunt that *threatens*, *gloats with affect*, names the player's
feeling, or implies "you wasted your time, fool." That breaks the cold-register law (voice.ts
header) and the warmth-under-dread rule. The Watcher does not sneer. Its teeth are *accuracy*, not
malice: it states the true thing you solved, and that it is only true. The bite is the anticlimax,
delivered flat. **The teeth belong to the liar (Iss); the Watcher's only edge is that it was right.**

### 0.1 THE DRIFT (must fix — this is why the idea is currently invisible in-game)

The shipped seed `puzzles_seed.sql` **already** authors the full taunt family and the prophet
wall. Concretely, these rows already pass `voice_args.kind` to a kind-switched dead-end:

| seed row | `voice_key` | `voice_args.kind` | movement |
|---|---|---|---|
| `m1-named-habit` | `oracleDeadEnd` | `name` | I |
| `m2-rhyme` | `oracleDeadEnd` | `count` | II |
| `iss-dead-shrine` | `oracleDeadEnd` | `place` | II |
| `forged-eighth` | `oracleDeadEnd` | `known` | II |
| `prophet-wall-comfort` | `oracleDeadEnd` | `prophet` | II |
| `prophet-wall-name` | `oracleDeadEnd` | `prophet` | II |
| `name-where` | `oracleDeadEnd` | `place` | II→IV |

But the consuming code does **not** read `kind`:

- `voice.ts` L177 `oracleDeadEnd(): string` takes **no argument** and returns one static line.
- `resolve.ts` L365–366 `case 'oracleDeadEnd': return voice.oracleDeadEnd();` — passes nothing.
- `voice_args` is read **only** for `oracleLore`'s `fragment` (resolve.ts L380–385).

Result today: all seven dead_end rows speak the identical generic line. The "teeth" exist in
data and are dropped on the floor at render. **Five `kind`s = `name | count | place | known |
prophet`.** (The earlier draft of this doc listed a sixth, `rhyme`; the SHIPPED seed folded the
`m2-rhyme` row to `count` ("six. it opens nothing"), so the canonical set is FIVE. Do not
re-introduce `rhyme` — match the seed.)

There is a **parallel, separate drift** the seed also created (out of scope to fix here but
flag it): rows reference voice keys `docketReread`, `oracleThreeHands`, `oracleMetaUnkept`,
`recordElsewhere` that are **absent from voice.ts**. Per the seed's own header comment
(L554–556) "a missing voice key is silent at runtime, never a build break" — so those degrade
to silence, not a crash. This treatment fixes ONLY the dead-end family; the four others belong
to their own idea docs (three-hands gate, meta-unkept, record-url, base-docket).

---

## 1. EXPOUND — the mechanic, the story, the mystery

### 1.1 What it is, mechanically

"With teeth" = the `dead_end` acknowledgement becomes **specific to the category of thing solved**,
while staying inside the cold register and the existing schema. Two layers, both already seeded:

- **(A) A keyed taunt family.** `voice.ts` `oracleDeadEnd` becomes `oracleDeadEnd(kind?)`, switching
  over five `kind`s. The seed already names the precise variant via `voice_args.kind`. No new
  resolver *path* — `speakOutcome` already routes `voice_key → fn`; we only thread the one arg
  through, exactly as `oracleLore(fragment)` is already threaded.

- **(B) The false-prophet artifact** (the louder, story-bearing half). A forged in-world document —
  **"the prophet's wall"** — attributed (deniably) to Iss in his warm period. Its source text is
  `arc/lore/documents/the-ways-are-a-wall.md` *verbatim* ("the ways are a **wall**… keep the ten
  and you are inside the wall… the watching stays out in the cold… trust the wall"). The wall is a
  **wide** set of carvings (not a tall ladder) that each decode cleanly to a confident promise and
  each resolve to a `dead_end` taunt of kind `prophet`. Seeded as `prophet-wall-comfort` (the warm
  promise) and `prophet-wall-name` (a columnar acrostic whose first-marks spell the corpus word for
  Iss — "the one who turned away"). The wall's apparent "final answer" is the warm Iss reading
  `iss-warm` ("the ways are a wall against the watching"), which routes to the `iss-dead-shrine`
  grave (kind `place`). So the entire prophet structure is a self-consistent liar's tract whose
  every rung is a true-but-empty solve. The prophet is **Iss with a pulpit**; the dead-ends are
  his sermon. Catching it (`iss-doubt` → `no-wall-catch`, both seeded) re-reads the whole wall.

### 1.2 How it plays across the 5-movement / ~2-week arc

- **Movement I — the notice. First teeth, gentle.** `m1-named-habit`: the player decodes that the
  world named their measured habit before they knew it was a law. `dead_end`, kind `name` (a true
  name of a thing about *you*, and it opens nothing). This is the tonal thesis: solving is reading,
  and some readings are only true. The numeric "six" anticlimax also lives here in spirit (the
  count taunt) and is paid off in M3 by the Seventh.

- **Movement II — the keeper-stones. The dead-ends thicken into TEXTURE**, because this movement
  has the most parallel open doors (six stones + Seventh + carryovers) and thus the most chances to
  bounce off a wall. `m2-rhyme` (read two stones, their fates rhyme — true, opens nothing) speaks
  kind `count` ("six. it opens nothing"). `forged-eighth` (a real-looking carved "eighth way" that
  the land never measures — kind `known`) is the *honest* twin of the prophet's lie: a forgery the
  Watcher flatly declines to enforce. The **prophet's wall** is placed and legible here in Iss's
  field: a hard-ARG group finds it, decodes rung after rung, gets a `prophet` taunt each time, and
  feels *productive* (they are reading fluently) while it goes precisely nowhere. The warm `iss-warm`
  reading is the capstone → the grave (`iss-dead-shrine`, kind `place`). The prophet is, at this
  point, **trusted** — his wall is the most confidently written thing in the world.

- **Movement III — undercroft / the Seventh. Dead-ends recede as real doors open** (`undercroft-
  descent`, `seventh-shrine`). The single new beat: the Seventh shrine's recount ("six… and a
  seventh") *re-opens* the count the M2 `count` taunt dismissed. This is the first time the arc
  proves a Watcher "it opens nothing" was **not the Watcher lying — it was the Watcher being
  literal.** Six really does open nothing. *Seven* opens a side-quest. The teeth were honest; the
  player under-counted. (Load-bearing distinction: honest Watcher vs. dishonest prophet.)

- **Movement IV — the catch. The payoff movement.** `iss-doubt` → `no-wall-catch` fire; `iss_caught`
  / `true_coord_known` flip. The **prophet's wall is re-read**: every rung solved as a confident
  promise is now legible as a liar's hand. `prophet-wall-name`'s buried acrostic name is revealed
  as **Iss's own** — proof the wall was his. The dead-end `prophet` taunts collected in M2 were each
  the Watcher telling them, at the time, that the promise opened nothing — read past because the
  prophet's framing was warmer. The "OH" lands: **the Watcher was never taunting. It was reporting.**

- **Movement V — the accepting. No new dead-ends.** One optional grace note: the rite path may
  re-surface the M1 `name` answer as now-meaningful (you were named before you were told — the first
  line of your own induction into the record). Felt, never stated (FACT 15 discipline).

### 1.3 The mystery this serves — honest Watcher vs. lying prophet

The engine's whole differentiator is **the Watcher does not lie; it counts.** A naive "taunting
red herring" risks making the Watcher *seem* to lie ("ha, that was nothing"), poisoning the
precision contract — if it can jeer at a true solve, why trust its conduct reports? Resolved by
splitting the two voices the player already has:

- **The Watcher's dead-end taunt is flat and honest.** "yes. a true name. it keeps no door." It
  never says the solve was *worthless* — only that it is *true and not a door*. A fact, not a jeer.
- **The false prophet (Iss's wall) is the one who promised the dead-ends were doors.** The taunt
  the player retroactively feels is their own — aimed at the prophet who oversold the walls, not at
  the Watcher who flatly labelled them. The arc converts "the world mocked me" into "a man misled
  me, and the world told me the plain truth the whole time, and I didn't listen because his version
  was warmer." That is the Iss thread's exact theme made tactile through solving.

---

## 2. CRITIQUE — adversarial, honest

**R0 — THE DRIFT IS THE REAL RISK (and it is a shipped-but-mute defect).** Right now the seed
*claims* teeth and the runtime speaks one flat line for all seven dead-ends. On camera that is the
exact failure mode the idea exists to avoid: "and it opens nothing" three times in a row reads as a
broken puzzle, not deliberate variety. *Mitigation:* the §4 code change is **P1 and tiny** — one fn
signature, one resolver line. Until it lands, the idea is invisible. Ship it with the slice if any
dead-end is in the slice.

**R1 — PRECISION / register law: a "taunt" that gloats breaks the Watcher.** voice.ts is explicit:
the Watcher "counts and records; it does not emote or threaten." *Mitigation:* hard rule — **a
dead-end taunt states the category of truth and its inertness, nothing else.** No second-person
verb of judgment ("you wasted", "you failed"), no affect, no imperative to try again. The bite is
structural (anticlimax), not lexical (insult). The §3 exemplars are the test set; add the rule to
cipher-web.md §7 authoring checklist.

**R2 — The false prophet orphans / duplicates Iss (the one-liar law).** A standalone "false prophet"
NPC would be a second deceiver competing for Iss's narrative slot, and an orphaned mechanic (a liar
with no fate, stone, rhyme, or place in the six-keeper chorus). *Mitigation:* **the false prophet IS
Iss.** The prophet's wall is a forged artifact of his warm period; it adds no character and sets no
flag the Iss thread doesn't already own; it resolves through the existing `iss-warm` →
`iss-dead-shrine` → `iss-doubt` → `no-wall-catch` chain. Content on the existing spine, not a new spine.

**R3 — A ladder of dead-ends can read as a countable step-ladder (anti-ladder law).** *Mitigation:*
the wall's rungs are **not edges** — each prophet row is an independent `dead_end` (voice_key only,
NO `next_puzzle_key`), so solving one does not unlock the next. They are several legible carvings on
one surface, openable in any order, all walls. The real door (`iss-doubt`) is reached by turning the
key on the *other* stones, off the wall entirely. **The wall is wide, not tall** (verified: neither
`prophet-wall-comfort` nor `prophet-wall-name` carries `next_puzzle_key` in the seed).

**R4 — On camera, repeated dead-ends could read as the group "failing" / dead air.** *Mitigation:*
(a) the **variety** is the fix — five distinct taunts read as the world recognizing different things,
not one stuck string; (b) each dead-end is a *true solve the player earned*, so the editor gets a
genuine "we cracked it… oh." beat (the anticlimax is the content); (c) keep dead-end **density low
and spaced** — they cluster in M2 where many real doors are also open, so the group is never *only*
hitting walls.

**R5 — Deniability vs. fairness: will players feel cheated?** *Mitigation:* cipher-web.md §2.5 holds
— **a dead-end in a hard ARG is itself a reward** (you proved you could read it), and this crowd
*likes* true-but-inert solves as texture. The teeth make the reward legible. Unfairness only appears
if a dead-end is *required* to reach a door — forbidden by construction (dead_end rows carry no
`next_puzzle_key`).

**R6 — Brute-forcing a short dead-end answer.** A one-word answer like a name ("iss", a count) is
brute-forceable. *Mitigation:* the seed already caps the short ones — `prophet-wall-name` carries
`max_attempts: 6`; the global token bucket (resolve.ts RATE_MAX_IN_WINDOW = 8/60s) covers the rest.
Reaching a cap is in-voice withholding (`oracleWithheld`), never a hint, never a "close" tell.
**Authoring rule:** any new dead-end whose answer is a single dictionary word/name gets `max_attempts`.

**R7 — Anti-jank: no LLM, no non-determinism.** The taunt family is **hardcoded verbatim** in
voice.ts and selected by a **deterministic `kind`** in the seed. No model call, no per-request
variation, offline-safe. The dead_end path in resolve.ts is unchanged in shape.

**Net:** keep the taunt family in full (FIVE kinds matching the seed); keep the false prophet only
as Iss-content; cut all gloating/affect; cap short answers. Close the drift (§4) — that is the work.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (proof it can be done cold)

The five `oracleDeadEnd(kind)` lines. Each names the *category* of truth solved and its inertness,
flat, no affect, no second-person judgment, no slop. (Banned-list checked: no "a testament to", no
named emotion, no three-adjective list, no "not just X but Y", no exclamation, no em-dash drama.)

> **`name`** (m1-named-habit, acrostic/true-name dead-ends):
> `yes. a true name, and rightly read. it keeps no door. some names are only kept.`

> **`count`** (m2-rhyme, the even "six"):
> `six. the count is right. count is not a key. it opens nothing.`

> **`place`** (iss-dead-shrine, name-where — a true place that is a grave):
> `you read the way to it true. it is a grave. it keeps no road on.`

> **`known`** (forged-eighth — a true reading of a real carving the land never measures):
> `this is carved, and you read it true. nothing weighs it. it was added, not found.`

> **`prophet`** (the prophet's-wall rungs — the only kind with edge, still flat):
> `this is written plainly, and it is a comfort, and it is shut. read who carved it, after.`

The `prophet` line does NOT call Iss a liar (that would blurt the catch). It points — *read who
carved it, after* — a seed that only pays off once `iss_caught` flips. That is the wall's single
teeth-bearing taunt, and the teeth are an instruction to examine the author, not a jeer at the
player. The **default** `oracleDeadEnd()` (no kind) keeps its current verbatim line as fallback so
a typo never errors (mirrors `defaultLineFor`).

---

## 4. THREAD IT (consistency — exactly where this lives, no orphans)

### 4.1 Canon FACTs touched (no NEW fact — texture on existing facts)
- **FACT 2** (graded by laws no one told them) — the M1 `name` dead-end is its tactile expression.
- **FACT 5** (each keeper's fate rhymes with the custom they broke) — the `count` taunt on `m2-rhyme`.
- **FACT 7 → FACT 8** (Iss's lie that the ways are a wall → the catch) — the prophet's wall IS FACT 7
  made into a forged artifact; catching it is FACT 8. Adds **no new fact**; gives FACT 7 a louder,
  more findable surface and FACT 8 a more visceral re-read.
- **FACT 7b** (the forged eighth way / "Covering of the Hands") — `forged-eighth`, kind `known`: a
  way that was *added, not found*, which the land never weighs. The honest twin of the prophet's lie.
- **FACT 10** (the land can refuse) — indirectly: the honest "six opens nothing" `count` dead-end is
  overturned by the Seventh, modelling that the Watcher's walls are literal, the prophet's are lies.

### 4.2 Found-documents / journals that carry or foreshadow it
- `arc/lore/documents/the-ways-are-a-wall.md` (exists) — the **textual source** of the prophet's
  wall. The wall's decoded promises ARE quotations of this document ("trust the wall", "keep the ten
  and you are inside the wall", "west and a little down… the dead shrine"). The document already
  carries the later-hand margin notes ("we checked the lock… it gives the word we keep for the one
  who turned away"; "ask first what a wall is *for*. then turn his key.") that foreshadow the catch
  without naming it. **No new prose needed** — confirm the wall structure's carved rungs map 1:1 to
  this document's promise lines (a `design/structures.md` placement task, not a writing task).
- `arc/lore/documents/no-wall-was-ever-built-here.md` (exists) — the payoff document; the line-for-
  line rebuttal. Confirm its lines map 1:1 to the wall's rungs (its job already).
- `arc/lore/documents/the-eighth-way.md` (exists) — the source of `forged-eighth`'s `known` dead-end
  (the added, unweighed way). The seam-of-the-forgery showing is its whole point.
- `arc/lore/documents/the-record-opens.md` (exists) — buries the "seventh mark" + named-before-told
  seeds the M1 dead-end pays off. Confirm the "six… and a seventh" phrasing is shared by the M2
  `count` taunt and the Seventh thread.

### 4.3 NPC / Watcher voice lines that carry it (REAL symbols — the build)
- **`discord/src/voice.ts`** → the `voice` object: change `oracleDeadEnd()` (L177) to
  **`oracleDeadEnd(kind?: string): string`** that switches over the five kinds and falls through to
  the existing static line on unknown/absent kind. This is the lowest-blast-radius option and mirrors
  the already-shipped `oracleLore(fragment)` arg pattern. **No change to the `OracleVoiceKey` union**
  (the key stays `'oracleDeadEnd'`; only the arg is new). Sketch:
  ```ts
  oracleDeadEnd(kind?: string): string {
    switch (kind) {
      case 'name':    return 'yes. a true name, and rightly read. it keeps no door. some names are only kept.';
      case 'count':   return 'six. the count is right. count is not a key. it opens nothing.';
      case 'place':   return 'you read the way to it true. it is a grave. it keeps no road on.';
      case 'known':   return 'this is carved, and you read it true. nothing weighs it. it was added, not found.';
      case 'prophet': return 'this is written plainly, and it is a comfort, and it is shut. read who carved it, after.';
      default:        return 'yes. that is the true name of it. and it opens nothing. some things are only true.';
    }
  }
  ```
- **`discord/src/oracle/resolve.ts`** → `speakOutcome` (L359), the `case 'oracleDeadEnd':` (L365):
  change `return voice.oracleDeadEnd();` to `return voice.oracleDeadEnd(deadEndKind(payload));` and
  add a tiny reader mirroring `loreFragment(payload)`:
  ```ts
  function deadEndKind(payload: OutcomePayload): string | undefined {
    const k = payload.voice_args?.['kind'];
    return typeof k === 'string' && k.trim() !== '' ? k : undefined;
  }
  ```
  Also pass it in `defaultLineFor` (L388) `case 'dead_end':`. No other resolver change.
- **Tests:** add a `voice.spec`/seedcheck assertion that each of the five kinds returns a distinct,
  non-empty string and that an unknown kind returns the default — guards against future drift.

### 4.4 Cipher(s) / puzzle(s) that express it (reuse the 11 built ciphers)
- Prophet's-wall warm rungs reuse **`substitution`** (P4, the plain rune script Iss's warm wall is
  carved in); capstone reuses **`vigenere`** (P3, key=ISS) already seeded as `iss-warm`. No new cipher.
- `prophet-wall-name` reuses **`columnar`** — the wall's innocuous surface text hides the author's
  name as a column-keyed acrostic; a `prophet`-kind dead-end that, post-catch, is the proof of
  authorship (the §2.5 "hidden true name that opens nothing" doctrine example, realized).
- `m2-rhyme` is **cross-document correlation** (no transform). The count "six" is **observation** (P12).
- `forged-eighth` reuses **`substitution`** (its decoded signature "to cover one's own").

### 4.5 Beat classes / listeners / tables / seed rows / sites.yml / voice keys realized
- **Tables:** `public.puzzles` only — dead_end rows carry **voice_key + voice_args.kind ONLY**; no
  `beat_queue`, no `arc_state` flags for the pure taunts (schema comment, puzzles_seed.sql L16). The
  capstone routing rides the existing `iss-warm` row's `iss_trusted` flag; **no new flag.**
- **Seed rows — ALL ALREADY SHIPPED** (verify, don't re-author): `m1-named-habit` (`name`),
  `m2-rhyme` (`count`), `iss-dead-shrine` (`place`), `forged-eighth` (`known`), `prophet-wall-comfort`
  (`prophet`), `prophet-wall-name` (`prophet`, `max_attempts:6`), `name-where` (`place`). The only
  open seed task is confirming any NEW dead-end follows the kind taxonomy + caps short answers (§2 R6).
- **No beat class / listener / sites.yml change** — dead-ends place no structures and fire no beats by
  definition. The prophet's *wall* is a one-time in-world `small_structure` / `sign_write` placement
  in Iss's field (`design/structures.md`), NOT a beat — author the carving so its rungs quote
  `the-ways-are-a-wall.md` and its columnar surface hides "the one who turned away."
- **Voice keys / identifiers:** the five `kind` values (`name|count|place|known|prophet`) are the only
  new identifiers; they live as a switch inside voice.ts and are named by `voice_args.kind` in the seed.

---

## 5. PLANT THE PAYOFF — the "OH, that is what that was for" seed

**Plant (Movement II, inert/ambiguous):** the prophet `kind` taunt speaks `read who carved it,
after.` over every wall rung. At the time, this is the Watcher being cryptic over a true-but-inert
solve — the group has no way to know *who carved it* matters, because Iss is still the warmest,
most trusted voice. They file it as flavor. The `prophet-wall-name` columnar rung, decoded in M2,
yields a true name buried in the wall's surface text — which reads as just another "a true name,
opens nothing" beat. **Inert.**

**Payoff (Movement IV, on the catch):** when `iss-doubt` → `no-wall-catch` flips `iss_caught`, the
group re-reads the wall. The hidden name from `prophet-wall-name` is **Iss's own** — the wall was
his, and *"read who carved it, after"* was the Watcher telling them flatly, at the time, that the
author was the thing to examine. The "after" was literal: after the catch. The dead-end taunts they
collected as anticlimaxes are revealed as the Watcher's honest labels on a liar's promises. **The
Watcher never taunted; the prophet did, and the prophet was Iss.**

No payoff without a plant: the catch (`no-wall-catch`, seeded) is the payoff, already on the spine.
No plant without a payoff: every prophet rung re-reads at the catch (`comfort`, `name`) or is honest
texture the Seventh overturns (`count`). The M1 `name` dead-end gets a second, quieter payoff in M5
(your naming-before-telling was the first line of your own record — felt, never stated).

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Element | Movement | Depends on | Depended on by | Priority |
|---|---|---|---|---|
| **Close the drift:** `oracleDeadEnd(kind)` in voice.ts + `deadEndKind(payload)` in resolve.ts + test | I (lands first via `m1-named-habit`) | nothing (pure voice/resolver; offline-safe) | every dead_end row's flavor | **P1** (arc-spine polish; tiny blast radius; ships with slice) |
| Five taunt lines authored (de-slop) | I | the signature change | camera-legibility of every wall | **P1** |
| Existing dead-end rows already keyed to `kind` (verify only) | I–IV | the code change to make them audible | the whole "teeth" payoff | **P1** (already seeded) |
| Prophet's-wall structure: carve rungs quoting `the-ways-are-a-wall.md` + columnar author name | II | `rosetta-ring` literacy; the doc; Iss field placement (`structures.md`) | the M4 re-read ("OH") | **P2** (depth; the Iss catch works without it; this enriches it) |
| The re-read payoff at the catch | IV | `iss-doubt`→`no-wall-catch` (seeded) + the M2 plant | the felt honesty of the Watcher | **P2** (rides existing Iss spine) |
| M5 grace-note re-surfacing of the M1 `name` answer | V | M1 plant + the ending | nothing (optional) | **P2** |

**Vertical-slice note:** the **code change is P1** — without it the seeded teeth are mute and on
camera the dead-ends read as a stuck string (R0). It is one fn signature + one resolver line, so it
should ship with the slice if any dead-end is in the slice. The prophet's wall *structure build* is
P2 depth hanging off the already-seeded Iss thread; it can land in a later content pass without
blocking a playtest.

**One-line build summary:** make `oracleDeadEnd` take a `kind`, thread `voice_args.kind` through
resolve.ts's existing dead_end case (mirroring `oracleLore(fragment)`), author the five de-slopped
lines, add a distinct-line test — and (P2) build the prophet's-wall structure so its rungs quote
`the-ways-are-a-wall.md` and hide Iss's name in a columnar acrostic. One liar, one lie, louder mouth.
