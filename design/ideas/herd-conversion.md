# Idea Treatment — The Slow Herd Conversion

> **Status going in:** ~80% already built. `paceHerd` (showrunner/herd.ts), `SacredAnimalBeat`
> (plugin), `DeathListener` precision guard, `forks.ts` Fork A, canon **FACT 15 / INV-12 / INV-13**,
> and voice keys `tollSacredBeast` / `keptSacredBeast` all exist. This treatment expounds it, audits
> it adversarially, and specifies the **glue + realization gaps** to finish it without orphans.

---

## 0. One-line

Across the run a few more ordinary animals in the group's territory go **pale** between sessions —
never witnessed mutating, never glowing, never tracked — until a whole field stands facing one way.
A cosmetic dread-barometer that **decorates** the one true (glowing, tracked) Sacred Beast and pays
off at the M5 close as the *texture* of FACT 15: the land was already turning toward whoever comes
next.

---

## 1. EXPOUND — mechanic + story + mystery across the 5-movement / ~2-week arc

### 1.1 The two populations (the spine of the whole idea — do not conflate)

This idea is two animals wearing one coat. The entire design lives or dies on keeping them apart,
which the code *already* enforces (INV-13). Stated plainly so nothing downstream blurs it:

| | **The Sacred Beast** (Quiet Herd 1.6) | **The Pale field** (this idea) |
|---|---|---|
| count | exactly **one** | grows 1 → cap 16 |
| PDC tag | `sacred_beast` (+ `sacred_fork_arm` on the glowing one) | `pale_cosmetic` |
| glows? | **yes** (the only glower) | **never** (INV-13 law) |
| tracked? | yes — kill = violation, arms **Fork A** | **never** — decoration, conduct-blind |
| narrative job | a small life you keep or break (a *choice*) | the *weather* of the watching (a *condition*) |
| pace | placed once, M1, near base | `paceHerd`, +1/pass, M2→M5 |

The Sacred Beast is the **verb** (you act on it). The Pale field is the **adjective** (it colors the
world around your one choice). The horror of the field is **behavioral, not monstrous** — the
bestiary's own thesis: *the ordinary herd turning as one to watch is scarier than any model.*

### 1.2 How it actually plays, movement by movement

- **M1 (single beast).** `paceHerd` returns `addThisPass:0` (movement < `startMovement=2`). Only the
  one Sacred Beast exists, placed once near the heatmap base by `SacredAnimalBeat`
  `{match_type, radius:12, glow:true}`. It is pale, faint-glowing, silent, persistent. The field is
  NOT yet seeded. Players meet the *singular* uncanny animal first — the thing they may grow to
  protect — before the *plural* dread starts. This ordering is the plant (§5).

- **M2 (conversion begins).** Between the M1 and M2 sessions, `herd.run.ts` reads `priorPaleCount`,
  calls `paceHerd` → `spread:true, addThisPass:1`, and fires `SacredAnimalBeat` in a new
  **`mode:"spread"`** pass that tags ONE additional nearby animal `pale_cosmetic` — whitened, **not**
  glowing, no name, persistent, silent — and turns it to the canonical facing (FACT 15 visual). The
  players log in to find a *second* pale animal beside the first. No Discord announcement. No
  step-counter. They notice or they don't.

- **M3.** +1 again (→3). Now there is a small clutch. A returning, observant player starts to feel a
  pattern: *weren't there fewer of these?* The dread is **retrospective** — it rewards the player who
  remembers last session. The pale ones all face the same compass bearing (the bearing is a *quiet*
  reuse of the orientation motif, never decoded as a puzzle here).

- **M4.** +1 (→4..n). The clutch reads as a deliberate small herd. By now the group has likely made
  (or is making) their **Fork A** choice on the *glowing* beast — protect or kill. The pale field
  is the ambient pressure under that choice, never the choice itself.

- **M5 (the field stands watching).** The field is at or near its high-water (cap 16, but realistic
  reach over 5 movements at +1/pass with multiple login windows is ~5–10). At the close, the M5
  composer reads the fork leaf (`sacred_beast_broken` vs its absence) AND the pale-field condition,
  and renders the field as the *setting* of the ending: a stretch of pale animals, all facing one
  way, watching the players the way the players were watched. The marquee leans on **orientation
  (formation), not number** — "a field that faces you" lands on camera; "there are now 9" does not.

### 1.3 The mystery / "what is happening to the animals"

There is no puzzle to *solve* here — by design it is **atmosphere with a delayed reading**, which is
the correct shape for a cosmetic. The mystery is the slow dawning that the conversion is not random:
the pale ones are the herd being *accepted into the watching* ahead of the players (FACT 15). The
"oh" is not a decode; it's a re-reading of every pale animal they walked past as **the land already
turning**, the same process that will take the group if they're accepted. The found-documents (§4)
plant the in-world frame so the conversion isn't mute decoration: a keeper *counted* his animals
going pale, the same way the Watcher counts.

---

## 2. CRITIQUE — adversarial and honest

### 2.1 The single sharpest risk: **"orphaned cosmetic" / it never gets read**
A purely cosmetic, un-tracked, un-announced spread can pass **completely unnoticed** — and an unread
cosmetic is by definition an orphaned mechanic (violates the consistency law). If no one clocks that
the count rose, the payoff at M5 has nothing to pay *off*.
- **Mitigation (required, not optional):** anchor the field to a **document seed** (§4) so the
  conversion has an in-world witness and a re-read trigger. A single found page where a dead keeper
  tallies his herd going pale converts the field from "ambient noise" into "**oh — that is what was
  happening to the animals**." The plant is the page; the mechanic is the field; neither is an
  orphan because each points at the other. This is the line item that makes the whole idea legal.

### 2.2 Reveal-discipline risk: a player witnessing an animal turn pale
If a player is looking at the animal when `SacredAnimalBeat mode:"spread"` retags it, the
mutation-in-view breaks the cardinal anti-jank law (nothing is ever witnessed mutating).
- **Mitigation:** the spread pass runs **between sessions** (server-side, `herd.run.ts`, when the
  group is offline) and additionally MUST route placement/retag through `util/Reveal.isHidden` /
  `mutateWhenUnwitnessed` — the same discipline every other beat uses. herd.ts's header already
  claims "placed out of line of sight"; the **gap is that the beat's `spread` branch must actually
  call Reveal** (see §4 code work). If no unwitnessed candidate exists this pass, **skip and carry
  the deficit** — monotone target is preserved, idempotency holds.

### 2.3 Precision-law risk: a false "it knows you"
If the pale field were ever conduct-tracked, killing a decoration animal could mis-fire a violation
or arm the irreversible fork on a player who never touched the *real* Sacred Beast — a wrong "it
knows you," which the law calls worse than none.
- **Mitigation:** already enforced in `DeathListener` — `isPaleCosmetic()` short-circuits BEFORE any
  sacred/fork check (lines 120–123). The pale field can NEVER arm Fork A. **This must stay true on
  the beat side:** the `spread` pass must tag `pale_cosmetic` and MUST NOT tag `sacred_beast` or
  `sacred_fork_arm`. Add a selftest assertion (§4).

### 2.4 Collective-law risk: punishing the group for an absent member
None — the field gates nothing and scores nothing; it's a neutral colorant. Safe by construction.

### 2.5 Path-A / performance risk
Tagging up to 16 persistent animals is cheap (PDC byte + persistence flags, already what the beat
does). The only cost is they don't despawn — bounded at cap 16, negligible. No client install. Safe.

### 2.6 What should be **CUT / kept-scaled** (already partly cut in herd.ts — keep it cut)
- **CUT: breeding / on-screen conversion / "the herd grows" Discord announcements.** herd.ts already
  marks these CUT. Re-confirm: no step-ladder, no number marquee. **KEEP CUT.**
- **SCALE DOWN: the cap.** Cap 16 is a ceiling, not a target. Over a real 2-week run the field will
  realistically reach ~5–10. Do **not** force it to 16 with extra passes — a *small* watching clutch
  reads as more uncanny on camera than a stampede, and a full 16-animal field risks looking like a
  farm bug. Leave `addPerPass:1` and let it land where it lands.
- **KEEP: orientation as the marquee signal, count as the hidden one.** Correct as written.

---

## 3. DE-SLOP exemplars (in-voice proof)

Watcher/keeper register: plain, cold, declarative, counts and records, does not emote. All pass the
anti-slop law (no named emotions, no "testament," no melodrama, concrete mundane detail, the iceberg).

> **Keeper's tally (found page, M2 plant):**
> *"Third one this week. Spring heifer. White to the hoof by morning, no mark on her, eating fine.
> She stands the way the others have started to. East. I moved the salt lick. They face east anyway."*

> **Keeper's tally (later page, degraded — M4):**
> *"Counted nine. Counted nine again to be sure. They do not graze now. I stopped bringing the
> salt."*

> **Watcher record line (M5, field condition — reuse `keptSacredBeast` register, no new key):**
> *"the pale ones face the road you came in by. they were yours. now they keep the count."*

> **Watcher record line (M5, if Fork A broken):**
> *"the kept one is down and the field still faces east. it did not need the one you took."*

---

## 4. THREAD IT — every place this must appear (no orphans)

### 4.1 Canon (`arc/lore/canon-spine.md`) — already present, this idea *realizes* them
- **FACT 15** — "To be accepted is to become part of the watching, for whoever comes next." The pale
  field is the *animal-scale rehearsal* of FACT 15. **Touches, does not add.**
- **INV-13 (INV-HERD)** — the precision split (one glowing tracked beast; pale never glow / never
  violate). **This idea is the thing INV-13 exists to protect.** Touches.
- **INV-12** — permanence colors never gates. The field colors M5, gates nothing. Touches.
- **NEW canon to ADD:** **FACT 15.a (corollary):** *"The land is accepted before its keepers are —
  the animals turn first."* One line, filed under FACT 15. This is the only net-new canon; it gives
  the cosmetic an explicit lore home so it is not orphaned at the spine level.

### 4.2 Found-documents / journals (REQUIRED — this is the anti-orphan plant)
- **NEW artifact: the keeper's tally** — a 3–4 fragment document (`arc/lore/documents/keeper-tally.md`
  in the Set-B keeper hand), counting animals going pale, degrading in spacing/legibility across
  fragments (tonal decay shown structurally, not stated). Fragments drip M2 / M3 / M4 to mirror the
  field's growth. Exemplars in §3. This is the witness that makes the field readable.
- **Existing bestiary §1.6** already documents the singular beast; add a 2-sentence "the pale field"
  note cross-referencing this idea so the bestiary and the mechanic agree.

### 4.3 NPC / Watcher voice lines
- Reuse **`tollSacredBeast` / `keptSacredBeast`** (voice.archive.ts:274–276) — header of herd.ts
  already mandates "no new key." The M5 field-condition line (§3) is composed from the `kept` register.
- No NPC spoken lines needed (the keeper is dead; he only leaves the tally). Restraint = correct.

### 4.4 Cipher / puzzle expression
- **By design this idea carries NO puzzle of its own** (a cosmetic with a forced cipher would be the
  orphan-in-reverse: a mechanic bolted to a gimmick). The pale field's east-facing bearing is the
  **same orientation motif** decoded elsewhere — it is *visible corroboration*, not a new gate.
- IF a light reward is wanted on the keeper-tally document: a single **`a1z26`** or **`atbash`** run
  on a smudged word in the last fragment (e.g. the keeper's final word decodes to `kept` or `east`) —
  reuse only, no new cipher. **Recommend keeping it cipher-free** to protect "atmosphere, not task."

### 4.5 Code symbols / beats / listeners / tables / seeds (the BUILD gaps)
Existing (done): `SacredAnimalBeat`, `DeathListener` (`isPaleCosmetic` / `isSacredBeast` /
`isForkArming`), `showrunner/herd.ts::paceHerd`, `forks.ts` Fork A, `pale_cosmetic` /
`sacred_fork_arm` NamespacedKeys.

**Missing (must build to finish — these are the real deliverables):**
1. **`SacredAnimalBeat` `mode:"spread"` branch** — payload `{mode:"spread"}` tags a fresh nearby
   `Animals` with `pale_cosmetic` (NOT `sacred_beast`, NOT `sacred_fork_arm`, NOT glowing), persistent
   + silent + faced to bearing, routed through **`util/Reveal`** (unwitnessed only; skip + carry
   deficit otherwise). Default `mode:"single"` preserves today's behavior. Idempotent: counts existing
   `pale_cosmetic` near anchor, no-op if already at target.
2. **`discord/src/showrunner/herd.run.ts`** — reads persisted `priorPaleCount` high-water + window
   flag, calls `paceHerd`, on `spread` enqueues the beat `mode:"spread"` pass and bumps the
   high-water. (herd.ts header already names this file as the runtime; it does not exist yet.)
3. **`discord/src/showrunner/herd.selftest.ts`** — asserts: monotone (target never drops), cap (≤16),
   idempotent (same window → addThisPass 0), and **never emits a `sacred`/`fork_arm` tag** (precision).
4. **Collective gaze realization** — cheap coordinated facing on `pale_cosmetic` + the one
   `sacred_beast` (face the canonical bearing, or face nearest player on approach). Per bestiary:
   light `NamedMobBeat` facing logic or a simple gaze pass — **vanilla, no per-tick AI.**
5. **`discord/seeds/sites.yml`** — a `pale_herd` anchor row at the group's territory/heatmap base so
   the spread pass has a stable anchor location (the singular beast and the field share it).
6. **Persistence column** — `arc_state.pale_count` (high-water) + a per-movement `pale_pass_done`
   window flag, read/written by `herd.run.ts`. (No new table; columns on existing `arc_state`.)

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for"

- **PLANT (inert/ambiguous), Movement I–II:**
  1. The **singular Sacred Beast** in M1 — one pale animal, read as a one-off oddity / a thing to
     maybe protect. Ambiguous: is it special, or just a glitch-colored cow?
  2. The **keeper's tally** first fragment, found M2 — a dead man counting his animals going white.
     At the time it reads as backstory flavor about a previous failed group.
  3. The **second pale animal** appearing M2 — easy to dismiss as "another one of those."
- **INERT MIDDLE, M3–M4:** the count quietly rises; the tally fragments degrade; the bearing stays
  east. Nothing announces itself. A non-observant player files it as set dressing.
- **PAYOFF, Movement V:** at the close, the field stands — a stretch of pale animals all facing the
  road in. The M5 composer's `keptSacredBeast`-register line names it: *they were yours; now they
  keep the count.* The player re-reads: the keeper's tally was **the players' own future** at
  animal-scale; the singular beast in M1 was **patient zero**; every pale animal walked past was the
  land being *accepted into the watching ahead of them* (FACT 15). The cosmetic was never decoration —
  it was the slowest clue in the run.
- **No plant without payoff / no payoff without plant:** the field (mechanic) ↔ the keeper-tally
  (document) ↔ FACT 15.a (canon) ↔ the M5 composer line (voice). Cut any one and flag the other as
  orphaned.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| | |
|---|---|
| **Lives in** | M1 (singular beast plant) → M2 conversion begins → M3–M4 inert rise → **M5 payoff (field stands watching)** |
| **Depends on** | `SacredAnimalBeat` (done), `util/Reveal` (done), `paceHerd` (done), `arc_state` persistence, sites.yml `pale_herd` anchor, keeper-tally document, FACT 15 (done) |
| **Depended on by** | nothing gates on it (INV-12). The M5 composer *reads* the field condition as a colorant alongside the Fork A leaf — soft dependency, degrades gracefully if field is small/empty |
| **Priority** | **P1 (arc-spine)** for the pacer glue (herd.run.ts + spread branch + selftest + keeper-tally) — it's the connective tissue that makes the *already-built* INV-13 split mean something on camera. **P2 (depth)** for the ModelEngine reskin + collective-gaze polish (vanilla retag is the shipping fallback, per bestiary). **NOT P0** — the vertical slice stands without the field; the singular Sacred Beast alone covers M1. |

---

## 7. Verdict

**KEEP — keep-scaled.** The core is already built and law-compliant; finish the runtime glue, add the
one document plant + one canon corollary, and hold the cap small. Cut nothing further than herd.ts
already cut (breeding / announcements / on-screen conversion stay cut). The single thing that
elevates this from "ambient noise" to "the slowest clue in the run" is the **keeper's tally**
document — build that or the cosmetic is an orphan.
