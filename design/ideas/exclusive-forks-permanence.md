# Idea Treatment — Mutually-Exclusive Forks With Permanence

> One-way doors. A choice the world will not let the group undo. Killing the
> Sacred Beast opens a transgressor thread **and** permanently shuts the shepherd
> boon — and the record writes both as fact. 2–3 real irreversible forks so the
> world holds shape under the group's hands. Veterans respect permanence.

Status: **KEEP-SCALED — and largely BUILT.** The flag-writing layer ships; two
real gaps remain (see §7). Fork A (Sacred Beast) is P0 and the engine already
arms it; Fork B (First Light) is P1 and seeded; Fork C (Spoken Name) is
P2/cuttable and seeded. **Reject any fork that gates main-arc progress or singles
out a player** (INV-12, INV-16).

Grounded 2026-06-25 against the live tree:
- `discord/src/showrunner/forks.ts` — pure `applyForks(prior, triggers)`, first-writer-wins. BUILT.
- `discord/src/showrunner/autonomy.run.ts` (L35, L185–190) — reads `arc_state.flags`, calls `applyForks`, writes leaves. BUILT.
- `discord/src/showrunner/autonomy.selftest.ts` (L173–184) — fork A/B + pale-guard tests. BUILT.
- `plugin/.../signal/listener/DeathListener.java` — `isForkArming` / `isPaleCosmetic` split; only the glowing last-tagged beast arms Fork A. BUILT.
- `plugin/.../beats/lib/SacredAnimalBeat.java` — tags `sacred_beast`; the run wrapper marks the last-tagged as fork-arming. BUILT (arm-byte wiring per DeathListener javadoc A11+A12).
- `discord/supabase/seeds/puzzles_seed.sql` (L753–788) — `fork-light` (B) and `fork-name` (C) rows, `set_flags`-only. BUILT.
- `arc/lore/canon-spine.md` §6, §7 (INV-12, INV-13, INV-16), §8.3; `design/WEB-MASTER.md` §3.4, §3 (the Accepting composer spec, L332–346, L419–422); `arc/bestiary-sealed.md` §2.5 / §2.5b.

> **What changed since the first draft of this file:** the idea is no longer a
> proposal. INV-11–17 exist; INV-12 *is* this idea's law (it does not need adding).
> `forks.ts` + the run wrapper + the seed rows are committed. The two outstanding
> items are now narrow and named: **the M5 composer that READS the flags into close
> text, and the seedcheck fork-gate assertion** — both unbuilt (§7). Everything
> below is reconciled to that reality.

---

## 0. THE ONE-LINE THESIS (what makes this not a gimmick)

The arc already lives on a sealed binary: **kept vs. left** (FACT 6 — "named,
warned, then left at the threshold"; the Seventh, FACT 10/10b, cast out). Every
other consequence in the build is a *reversible toll* — warmth, not progress
(§6.10, INV-15). A permanence fork is the **one place the world's central binary
becomes a thing the players' own hands enact and cannot take back.** It is not a
new mechanic bolted on; it is the kept/left law, finally given a lever. Without
it, kept/left only ever happened to the dead keepers. With it, the group learns
the law applies to *them* — by tripping it once, irreversibly, and reading the
record close behind their own door.

The toll/permanence split stays clean and legible:
- **Tolls** (existing): atmospheric, reverse on their own, take warmth. "The world flinched."
- **Forks** (this idea): a single recorded act that closes one thread and opens another, forever. "The world remembered." Never a punishment, never a block — a **trade**, written down.

---

## 1. EXPOUND — the mechanic + story + mystery, across the 5-movement arc

### 1.1 What a "fork" is, concretely (as built)
A fork is **one authored, irreversible leaf flag** in `arc_state.flags`. The pure
policy is `applyForks(prior: ForkFlags, triggers: ForkTriggers): ForkApplyResult`
in `forks.ts`: for each fork, if it is NOT already settled and its trigger fired,
commit the matching leaf; an already-settled fork ignores any new trigger
(`settled(a,b)` → one-way). `autonomy.run.ts` reads the prior leaves off
`arc_state.flags`, calls `applyForks`, and merges only the newly-decided leaves
back (idempotent — a re-run commits nothing). The leaf set is the typed
`ForkFlags`:

```
sacred_beast_broken?   // Fork A transgressor leaf (boon leaf is the ABSENCE of this)
light_kept? | light_taken?   // Fork B, binary
name_unspoken? | name_spoken?   // Fork C, binary
```

Each fork has three parts: a **trigger** (a tracked act, or a solved/ chosen
puzzle), a **boon leaf** (the warm thread it fed; for Fork A the boon leaf is *no
flag at all* — the absence of the break), and a **transgressor leaf** (a cold
thread: a re-read, a new node, a changed room). Both leaves carry story + clue +
lore weight. The transgressor path is **not a failure state**; it is a different,
equally authored record entry. The world does not get worse; it gets **specific
about you.**

### 1.2 The three forks (priority order — matches WEB-MASTER §3.4)

**FORK A — The Sacred Beast (the kept small life). P0. Trigger = an ACT.**
- The arc tags one glowing herd animal (`SacredAnimalBeat`, PDC `observance:sacred_beast`) and tracks its death as `CUSTOM_SACRED_BEAST` (`DeathListener`). The run wrapper additionally marks the *last-tagged* glowing beast with `observance:sacred_fork_arm`. Only that beast's kill arms the fork.
- **Trigger path (built):** `DeathListener.onEntityDeath` → `isPaleCosmetic` short-circuit (INV-13: a `pale_cosmetic` kill is decoration, never conduct) → `isSacredBeast` → `violate(CUSTOM_SACRED_BEAST)` → `isForkArming` → `safety.info("signal.sacred_beast.fork_arm", …)`. The showrunner promotes that measured, fork-arming kill to `sacred_beast_broken` **first-writer-wins** (`triggers.glowingBeastKilled` → `applyForks`).
- **Boon leaf** (default, set on no one): `shepherd_boon` stays available; at the Accepting the herd's kept life yields the quiet warmth beat promised in `bestiary-sealed §2.5`.
- **Transgressor leaf** (`sacred_beast_broken`): the shepherd boon is closed forever; a single grounded report line names the measured killer; **Vaun's stone** gains one extra readable node (the Hoarder, who "kept nothing back for the deep," now has a living rhyme — FACT 5); the herd stops watching the group and watches the *spot where it died*.
- Why permanence is honest here: you cannot un-kill an animal. Minecraft already made it one-way; the world's only choice is whether to *remember*. Remembering is the whole point.

**FORK B — The First Light (the Kept Light custom). P1. Trigger = a PUZZLE choice.**
- The Undercroft holds the one fire that never went out (FACT 11). The `fork-light` seed row (`puzzles_seed.sql` L753–770) is an M3 puzzle with **two valid plaintexts**: draw the M5 rite-token (FACT 13) **from the eternal flame** (`light_kept`) or **bank the flame** — snuff and pocket it as fuel (`light_taken`). Submitting either plaintext IS the fork. `set_flags` only; no `next_puzzle_key` that gates anything.
- **Boon leaf** (`light_kept`): the Undercroft stays lit; M5's altar lights from it; Mara's "she keeps every light" thread closes warm.
- **Transgressor leaf** (`light_taken`): the Undercroft goes dark **and stays dark** for the rest of the arc (FACT 11 inverted by the group's own hand); Mara's lectern set gains re-read page-refs that flip from grief to accusation; the M5 altar must be lit by hand, from nothing, which the Keeper notes.
- Permanence is *diegetic*, not just a flag: once `light_taken` is set there is no eternal flame to relight from. `sites.yml` needs the doused-state variant on the Undercroft (§7 gap).

**FORK C — The Spoken Name (the Unspoken custom). P2 / CUTTABLE on blurt risk.**
- The Liar thread (Iss, FACT 7/8) hinges on a name nobody should say. The `fork-name` seed row (`puzzles_seed.sql` L774–788) is a late-M4 puzzle: **withhold** Iss's true name (`name_unspoken`, the default leaf shown in the seed) or **carve** it — submit the resolved name to the answer-sign / `#the-record` (`name_spoken`, the carve act detected in-world).
- **Boon leaf** (`name_unspoken`): Iss's testimony re-reads cold but his coordinate is given freely; the Unspoken stays unspoken.
- **Transgressor leaf** (`name_spoken`): the group did what Iss did — they *told*. One flat record entry treats them, faintly, as Iss's successors. This is the closest the corpus comes to FACT 15 by the group's own action — **authored as event, never explained** (INV-16, §6.2 foreshadow-never-blurt).
- P2 because it risks over-rhyming the group with the twist. Cut it if A+B already carry the felt-permanence load (§2 RISK 4).

### 1.3 How it plays out, movement by movement
- **M1:** No fork live. The Sacred Beast is tagged, glowing, silent, watched (existing). Kept/left is seeded only as a thing that happened to dead keepers. Nobody knows a door can close.
- **M2:** Fork A is armed (the arm-byte exists; a glowing-beast kill is now permanent). Reports begin rhyming keeper fates to behavior (FACT 5). A kill here sets `sacred_beast_broken`; Vaun's extra node and the herd's shift appear within a day, *behind* the act (reveal discipline — consequence catches up, never predicts). The boon line never appears again. Precision: the report names only the measured killer (INV-16, §6.4).
- **M3:** Fork B goes live at the Undercroft via the `fork-light` puzzle. The doused-or-kept Undercroft is the most visible permanence beat — *the room*, changed, every time they return.
- **M4:** Fork C (optional) at the Liar's catch via `fork-name`. Accumulated forks re-read earlier documents: a group that broke the beast AND took the light reads Iss's "the ways are a wall" with new weight — they have been *un-keeping* by hand, exactly what frayed the last generation.
- **M5:** The Accepting composer reads every fork flag and **colors** (never gates, INV-12) the close: kept leaves → warm beats (shepherd boon fires; altar lights from the eternal flame); transgressor leaves → cold-but-equal beats (altar lit by hand; a flat Archivist report listing what was kept and what was taken). **The group is still accepted** — collective judgment (INV-11), never punish the group, never elect a chosen one. Per WEB-MASTER §3 the composer adds **≤1 tinted clause** by fixed priority (fate / seventh_choice / heaviest fork) so the close never reads as a checklist. Forks decide the *texture* of being kept, never *whether*.

---

## 2. CRITIQUE — adversarial, honest

**RISK 1 (sharpest) — the flags are written but nothing READS them into the
ending yet (a live orphan).** `autonomy.run.ts` writes `sacred_beast_broken` /
`light_*` / `name_*`, and `fate.ts` computes `ending_fate`, but **no M5 composer
consumes the fork leaves into close text.** Right now this idea is half an orphan
by its own consistency law: lore that a mechanic sets but no surface expresses.
**Mitigation (the one must-build):** ship the M5 Accepting composer that reads
`ForkFlags` off `arc_state.flags` and selects warm/cold leaf beats + the single
tinted clause (WEB-MASTER §3, L419–422). Until it exists, **forks are not
done** — they are flags nothing speaks. This is the gating sub-task for the whole
idea (see §6, §7).

**RISK 2 — a fork that gates progress breaks anti-jank + collective laws and can
soft-lock on camera.** If `sacred_beast_broken` removed a required clue, an absent
or reckless member could lock the group out. **Mitigation (INV-12, already
canon):** forks NEVER gate; the seed rows are `set_flags`-only with no gating
`next_puzzle_key`. **The promised seedcheck assertion is NOT yet built** —
`seedcheck.ts` today only checks answer-normalization parity. Add an executable
assertion: no spine puzzle's `requires` references a fork leaf (`sacred_beast_broken`,
`light_*`, `name_*`). This is the second must-build (§7).

**RISK 3 — accidental/grief permanence (a veteran kills a cow not knowing it's THE
cow).** **Mitigated by INV-13, built:** only the ONE glowing, silent, persistent,
last-tagged beast arms Fork A; the cosmetic Pale herd never glow and are
short-circuited in `DeathListener` before any sacred check. Arm Fork A only after
M2 (after the first keeper-fate report teaches "fates rhyme with conduct"), so the
act is informed. The world never says "do not kill this" (register) — the animal's
strangeness is the warning, and it predates the fork by a full movement.

**RISK 4 — permanence reads as a bug, not a feature ("the light won't relight").**
On a recording, a missing eternal flame could look like jank. **Mitigation
(INV-12 clause, must hold at authoring):** every transgressor leaf produces a
*positive new artifact* — a report line, a stone node, a clearly-authored doused
room — so absence is always paired with presence. Permanence you can *read* is
design; permanence that only subtracts looks like a fault.

**RISK 5 — Fork C over-rhymes the group with the sealed twist (FACT 15) and risks
blurting.** "You spoke the name like Iss; you are becoming the watching" is one
bad line from saying the unspoken thing. **Mitigation:** Fork C is P2/cuttable;
if kept, its transgressor artifact is ONE flat record entry and ONE re-read flip —
no narration, no "you are becoming" (INV-16, §6.2). On any authoring doubt, cut C
and let A+B carry permanence.

**RISK 6 — too many forks = a countable step-ladder (violates HARD/non-linear).**
Three labeled doors could feel like a checklist. **Mitigation (built into the
composer spec):** cap at 3, never surface them as a set, no UI / advancement toast
/ Discord announce on fork-set; the M5 composer adds **≤1** tinted fork clause
(WEB-MASTER §3 L346). The group discovers *in retrospect* that doors closed.

**RISK 7 — idempotency / double-fire.** Forks B/C inherit the resolver's
`recordSolve` ON-CONFLICT guard. Fork A's act trigger is async-flushed and could
fire twice; **mitigated by `applyForks` first-writer-wins + the last-tagged-beast-
only arm byte** — a second cow death is a no-op. Main-thread world reads enforced
in `DeathListener`. `autonomy.selftest.ts` already asserts the settled-ignores-
retrigger and pale-guard cases (L173–184).

**Verdict:** KEEP-SCALED. The most on-theme mechanic the arc has, and it is built
through the flag layer. It is **not finished** until the M5 composer reads the
flags and the seedcheck gate-assertion lands. Ship those two; A is free, B is
seeded, C is optional.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (cold, plain, concrete)

Sacred-beast transgressor report (the Archivist, flat, names only the measured):
> the pale one is down. it was nine winters watched. the herd stands where it
> stood and does not graze.

Vaun's extra node, unlocked by `sacred_beast_broken` (the Hoarder, of what he kept):
> i kept the first calf back too. i told myself the deep had enough. the deep
> counts what you keep from it. it counts well.

Undercroft, after `light_taken` (the record speaking the room, not the group):
> the fire that was kept through the empty years is out. the hand that took it
> carried it up warm. the room keeps the cold now.

Keeper at the Accepting, kept-light leaf (warmth under dread, never names the twist):
> the light came up the stair on its own. you only carried it. that is enough.
> that is how it is meant to be carried.

None states an emotion, none editorializes, none reaches for a thematic bow. Each
is a recorded fact with the warmth or cold in the *object*, not the adjective.

---

## 4. THREAD IT — where this lives so it is not an orphan (verified file map)

**Canon-spine (`arc/lore/canon-spine.md`) — already reconciled:**
- **INV-12** *is* this idea ("Permanence colors, never gates; felt at the ending, never scored; subtracts only paired with a new authored artifact"). No new INV needed — the first draft's "add rule #11" is **superseded/done**.
- **INV-13** governs Fork A's fair-avoidability (glowing = fork-arming; Pale never glow).
- **INV-16** governs the precision floor (no surface lets the group derive WHICH active player is honored vs. violated).
- Touches **FACT 5/Vaun** (Fork A's living rhyme), **FACT 6** (kept/left, player-enacted), **FACT 10/10b** (acceptance can be spent, never lost), **FACT 11** (Fork B's inversion), **FACT 7/8 §4 Liar** (Fork C).

**Found-documents / journals (verified present in `arc/lore/documents/`):**
- `counted-them-in-the-dark.md` (Vaun) — holds "you do not keep the first thing"; the **plant** for Fork A. Add the conditional `sacred_beast_broken` extra node here, fork-gated.
- `page-line-word.md` + `what-the-surface-keeps.md` (Mara) — the `light_taken` re-read page-refs.
- `the-ways-are-a-wall.md` / `no-wall-was-ever-built-here.md` (Iss thread) — Fork C's re-read.

**NPC / Watcher voice (`discord/src/voice.ts`, `arc/corpus/npc-and-watcher-voice.md`):**
- New voice keys (§7) for: sacred-beast-broken report, undercroft-doused line, kept/taken Keeper Accepting lines. **None exist in `voice.ts` yet** (grep confirms) — they are part of the composer build.

**Ciphers / puzzles (reuse of the 11 built ciphers):**
- **Fork B** (`fork-light`): **book-cipher** (Mara's embodied cipher; the two leaf words assembled from the lectern — fits her voice). Seed row built.
- **Fork C** (`fork-name`): **Vigenère** (Iss's embodied cipher; the key is his name — resolving the key IS speaking the name). Seed row built. Per INV-14 the resolved name is *read/carved*, never the puzzle's typed answer-token.
- **Fork A**: **no cipher** — trigger is a tracked act. This deliberately varies the in-roads (non-linear law): not every door is a puzzle.

**Engine symbols that realize it (verified):**
- `forks.ts` → `applyForks`, `ForkFlags`, `ForkTriggers`, `ForkApplyResult`, `settled`. BUILT.
- `autonomy.run.ts` L185–190 → builds `prior` from `arc_state.flags`, applies, writes leaves. BUILT.
- `autonomy.selftest.ts` L173–184 → fork A/B + pale-guard tests. BUILT.
- `DeathListener.java` → `isSacredBeast` / `isForkArming` / `isPaleCosmetic`; `forkArmKey = observance:sacred_fork_arm`, `paleCosmeticKey = observance:pale_cosmetic`. BUILT.
- `SacredAnimalBeat.java` → tags `observance:sacred_beast`, `glow:true`. BUILT (last-tagged arm-byte set by the run wrapper).
- `puzzles_seed.sql` L753–788 → `fork-light`, `fork-name`. BUILT.
- `resolve.ts` `applyOutcome` → `set_flags` path carries B/C. No change needed. BUILT.
- `state.ts` / `arc_state.flags` → durable home of the leaves. BUILT.

**Still UNBUILT (the §7 gaps):** the M5 Accepting composer (reads `ForkFlags` →
warm/cold beats + ≤1 tinted clause); the new voice keys; the `sites.yml`
Undercroft doused variant; the `seedcheck.ts` no-fork-gate assertion; the Vaun /
Mara conditional document nodes.

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for" seed

**Plant (M1, inert):** Vaun's line in `counted-them-in-the-dark.md`:
> "Whatever the deep gives up first — first ore, first water, first warmth — you
> carry back to the cairn and give it to the deep again. **You do not keep the
> first thing.**"
In M1 this is old custom flavor, one rule among ten. It names no animal, no fork,
no consequence. (Tracked as plant #13 in WEB-MASTER §9 ledger.)

**Also planted (M1):** the Sacred Beast is glowing, silent, watched — strange,
unexplained. The herd watching it is "atmosphere."

**Payoff (M2→M5, transgressor leaf):** when the group kills the beast, Vaun's
extra node fires — "i kept the first calf back too… the deep counts what you keep
from it." The M1 rule snaps into place: *the Sacred Beast WAS the first thing, the
kept life you were not to keep from the deep, and killing it is keeping it from the
deep most finally of all.* The glow, the silence, the watching herd — the rule
made flesh, read only after their own hand proved it. On the boon leaf, the same
plant pays off inverted at M5: the herd's kept life yields the shepherd boon, and
Vaun's line reads as the warm thing they *did* honor.

No payoff without the plant (Vaun's line + the strange beast, both M1); no plant
without the payoff (the fork node M2; the boon/cold M5 leaf).

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Fork | Lives in | Depends on | Depended on by | Priority | Build state |
|---|---|---|---|---|---|
| A — Sacred Beast | armed M2, pays M2→M5 | `SacredAnimalBeat` + `DeathListener` arm-byte (BUILT); Vaun node (UNBUILT); FACT 5 report first | M5 shepherd-boon-vs-cold leaf; Vaun's living rhyme | **P0** | flag-write BUILT; node + M5 read UNBUILT |
| B — First Light | choice M3, pays M3→M5 | `fork-light` seed (BUILT); book-cipher; `sites.yml` doused variant (UNBUILT) | M5 altar-from-flame vs by-hand; Mara re-read | **P1** | seed BUILT; sites variant + M5 read UNBUILT |
| C — Spoken Name | choice M4, pays M4→M5 | `fork-name` seed (BUILT); Vigenère / FACT 8 catch | M5 successor-of-Iss cold leaf (optional) | **P2 / cut on blurt** | seed BUILT; M5 read UNBUILT |

**Hard dependency for ALL three (the keystone):** the **M5 Accepting composer**
must read the fork flags and select warm/cold leaf beats + ≤1 tinted clause
(WEB-MASTER §3). It does **not exist yet** — `autonomy.run.ts` writes the flags;
nothing consumes them into close text. Until the composer ships, every fork is a
flag nothing speaks (RISK 1). **Ship Fork A's M5 read + the composer together, or
the whole idea is an orphan.**

**Second must-build:** the `seedcheck.ts` assertion that no spine puzzle `requires`
a fork leaf (RISK 2 / INV-12 guard) — the executable proof permanence never gates.

**What depends on this idea:** nothing gates on it (by law, INV-12). It is a
colorant the ending consumes. If cut entirely, the arc still completes — which is
the proof it obeys the collective/ungated laws.
