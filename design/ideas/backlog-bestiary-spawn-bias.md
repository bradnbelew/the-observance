# Backlog · Bestiary Spawn-Bias Orchestration (the 6 apparitions, fairly)

> MASTER-PLAN **P1.13** (resolves **R7**). Build-ready treatment.
> Companion reads: `design/bestiary.md` §1.1–1.6 + §3; `plugin/.../signal/SignalSnapshot.java`;
> `discord/src/showrunner/{decide,offline-skin,herd,name-where-never-been,autonomy.run}.ts`;
> `FLOW.md` §2 (drama budget); `arc/lore/canon-spine.md` (FACTs / INVs).
>
> **Scope discipline.** The six apparitions' *beat code* already compiles
> (`NamedMobBeat`, `SacredAnimalBeat`, `PrivateSound/Particle/Darkness`, `FakeBlock`,
> `ChestArrange`, `TorchGutter`, `DoorOpen`). R7 already proved that. This item is
> **one new pure showrunner module + one run wrapper** that decides *which* built beat
> fires, *for whom*, *how often*, **probabilistically and capped**. No new beat classes.
> It is the connective tissue that makes offline-skin's shape-rhyme and herd-conversion
> "precise but never a callout" — they all read the same dossier and obey the same fairness law.

---

## 0. One-line statement of the thing

A deterministic-but-stochastic **conductor** that, each between-session tick, reads every
active player's `SignalSnapshot`, scores how strongly each one *rhymes* with each of the six
apparition shapes, and — when the drama budget permits at most one — **rolls a seeded weighted
die** to emit ONE apparition beat for ONE rhyming player, refusing to single the same player
twice in a row. The rhyme is the bias; the die is the deniability; the cap is the fairness.

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 What the conductor reads (the six rhymes)

For each active player `p` it derives six **rhyme scores** in `0..1` from already-measured
`SignalSnapshot` fields. Each is a clamped, monotone read of a signal the tracker *actually owns*
(precision law — never a field we don't measure):

| Shape | `bestiary.md` | Rhyme score `r(p, shape)` derived from |
|---|---|---|
| `watcher_at_edge` | §1.1 | `max(soloMiningRatio, norm(distanceFromGroup))` — the alone one |
| `one_who_counts` | §1.2 | `norm(hoardedScore)` gated by Offering `violationRatio` (hoards AND won't give back) |
| `surface_walker` | §1.3 | `norm(distanceFromGroup)` — the wanderer who leaves the shore alone |
| `stoop` | §1.4 | Bow custom `complianceFor("bow").violationRatio()` — passes markers standing |
| `sleepless` | §1.5 | Dark-Hours + Kept-Light `violationRatio` (the one abroad / unlit after dark) |
| `quiet_herd` | §1.6 | **NOT rhyme-emitted.** Conduct-tracked only (see 1.6 below). Excluded from the die. |

`norm(x)` is a fixed, documented squash (e.g. `distanceFromGroup` blocks → `clamp(d / DISTANCE_FULL, 0, 1)`,
`DISTANCE_FULL = 128`). All squash constants live as named exports so the self-test pins them.

A player with a **flat profile** (no rhyme clears the floor) is simply not a candidate this tick —
the conductor would rather emit nothing than wear a shape on someone who didn't earn it. This is the
same `dominantShape()` precision floor `offline-skin.ts` already uses, re-expressed here for all five
ambient shapes.

### 1.2 The selection (probabilistic, never deterministic)

This is the R7 / bestiary §3 mandate made literal. The conductor does **not** pick `argmax`.

1. **Budget gate.** If the drama budget says no ambient slot is open this window (`FLOW §2`:
   ≤1 ambient/hour, ≥20-min gap), emit nothing. The conductor never *creates* drama budget; it
   *spends* an already-granted slot. (This is read off the same beat-cooldown state the rest of the
   spine respects; see 4.)
2. **Candidate set.** All active players whose top rhyme clears `minRhyme` and beats their own
   runner-up shape by `minRhymeMargin` (a *real* rhyme, not a tie) — reuse the `offline-skin`
   dominant-shape math verbatim.
3. **Fairness filter.** Drop any player who was singled in the **last `cooldownPicks` emissions**
   (default 1 → "never twice in a row") or who has hit `maxPerPlayerPerMovement` (default 2). This
   is the bestiary §3 cap: "one player is never repeatedly singled."
4. **Weighted roll.** Build a weight per surviving (player, dominant-shape) pair = `rhyme^GAMMA`
   (GAMMA≈1.5 sharpens toward stronger rhymes without ever zeroing a weaker one), then roll a
   **seeded** weighted die. The seed is `hash(movement, emissionOrdinal, arcSalt)` — so the choice is
   *reproducible* (same tick re-derives the same pick on a restart → idempotent) yet *unpredictable*
   to the players. The loner is *more likely* to feel watched; he is never *guaranteed* to, and
   occasionally the hoarder gets the Watcher instead. That wobble is the whole point: it reads as a
   land with a mind, not an `if (soloMiningRatio > 0.8)`.
5. **Emit.** Map the chosen shape to its built beat payload (table in §4) and enqueue ONE beat,
   out-of-LoS, reveal-disciplined, status by mode (`approved` in auto / `pending` in confirm).

Determinism contract matches its siblings: **same snapshot + same emission ordinal in → same beat
out.** The randomness is *seeded*, not wall-clock — so `--dry-run` shows exactly what live will do.

### 1.3 The Sacred Beast / herd is conduct-only (excluded from the die)

`quiet_herd` is **not** spawn-biased. INV-13: only the one true Sacred Beast glows and is tracked;
its appearance is established once (M1) and its only "trigger" thereafter is `mobKills` on the PDC
tag, surfaced through the reports — not a probabilistic apparition. The conductor *reads* `mobKills`
only to **suppress** unrelated escalation against a player who has already paid a transgression cost
(don't pile the Watcher on someone the herd already judged this movement) — a fairness courtesy, not
a new beat. The cosmetic pale spread stays in `herd.ts` where it belongs.

### 1.4 How it plays across the 5-movement / ~2-week arc

- **Movement I (felt, never named).** Conductor runs but at a *throttled* weight: it prefers the
  `PrivateSoundBeat` "behind you" precursor over a full figure, and `minRhyme` is set high, so most
  ticks emit nothing. A player or two meets the Watcher at the edge once. No shape is repeated. The
  group's first shared sentence is "did anyone else hear something?" — deniable by construction.
- **Movement II (the rhymes start to *fit*).** As `hoardedScore` / `distanceFromGroup` / Bow
  `violationRatio` accumulate real spread, the candidate set sharpens. The hoarder meets the One Who
  Counts at his chest wall; the wanderer sees the Surface-Walker at the far lake. Still ≤1/window,
  still capped. The group begins to *notice the fit* without being told it — "why does Brann keep
  getting the chest thing?" That dawning is the intended camera moment.
- **Movement III (the conductor interlocks with the offline lanes).** This is where the new web
  fuses. The same dominant-shape read now feeds **three** consumers in lockstep — the live conductor
  (this module), `offline-skin.ts` (wears a *rhyming offline* friend's skin over the same shape), and
  `name-where-never-been.ts` (carves by place). The **separation law** is honored centrally: the
  conductor publishes the cells/shapes it's claiming this window so the carve lane and skin lane never
  collide a worn skin + carved name at the same stone. A player can, in one movement, be *watched*
  live, find his *name* somewhere he's never been, and later see an *offline* friend wearing the
  Watcher — three surfaces, one measured truth, no contradiction (INV-16).
- **Movement IV (atonement re-weights).** When a player *honors* a custom he'd been breaking, his
  `violationRatio` falls and his rhyme to the matching shape *decays* — the apparition that singled
  him goes quiet. The conductor expresses FACT-10 mechanically: the land *relents* when you mend.
  Inversely, a brand-new violation can re-arm a shape that had gone silent. Reversible, warmth not
  progress.
- **Movement V (the conductor steps back).** Ambient apparitions taper as the Accepting rite takes
  the foreground; the conductor's `minRhyme` floor ramps up so it emits only the rarest, sharpest
  rhymes. The figures that haunted the group resolve into "the same figure, counting days" (the
  Watcher payoff) — and the group realizes, re-reading M1, that *the bias was always reading them*.

### 1.5 The mystery it serves

The deniability is the mystery. Because the conductor is *biased but stochastic*, the group can never
prove the server is profiling them — they can only *feel* it. "It always seems to be Brann" is a
hypothesis they form from a probabilistic process, which is exactly how a haunting that "knows your
name" should feel: a pattern you sense but cannot pin. The eventual reveal (the keeper-record was
counting all along, FACT 1) re-reads every apparition as a *measurement made flesh* — the
"oh, that's what that was for" lands because the bias was true the whole time, just never said.

---

## 2. CRITIQUE — adversarial, honest

**C1 — The cap can *itself* become a tell (collective / fairness law).** "Never twice in a row"
across a 5-person group means a sharp observer could back-infer "if it wasn't me last time, the odds
shift." *Risk:* the fairness mechanic leaks into a countable ladder. *Mitigation:* the cooldown is
on *being singled at all*, not per-shape, and the seed makes the next pick non-inferable; combined
with frequent **empty ticks** (most windows emit nothing) there's no clean parity to read. Keep
`cooldownPicks=1` small; do NOT escalate it into a round-robin (a round-robin IS a ladder — explicitly
rejected).

**C2 — Probabilistic singling could *still* feel unfair on camera if variance clusters.** A bad RNG
streak could give one player three apparitions in two sessions while another gets none. *Risk:* feels
buggy / persecutory; violates "never punish one player." *Mitigation:* the `maxPerPlayerPerMovement`
hard cap (default 2) bounds the worst case regardless of the roll; and because GAMMA only *biases*,
a low-rhyme player still occasionally gets chosen, spreading the load. If playtest shows clustering,
lower GAMMA toward 1.0 (flatter) rather than touching the cap.

**C3 — Orphan risk: is this a gimmick with no narrative home?** *Honest check:* if the conductor
were *only* "spawn the rhyming mob," it would be a clever-but-hollow targeting layer. *Mitigation /
why it's not an orphan:* it is threaded (§4) into FACT 1 (the list of the living), FACT 9 (the
re-enacted fate), and INV-16 (cross-surface truth), and it is the **shared brain** the offline-skin
and name-where lanes already assume exists (they both compute dominant-shape rhyme — this centralizes
it). Cutting it would leave those two lanes each re-deriving rhyme privately, risking the separation
law. So it has a real structural job beyond flavor.

**C4 — Drama-budget double-spend.** The conductor, the customs bridge, the grave lane, and the
offline-skin lane all want to enqueue out-of-LoS apparitions. *Risk:* a single window emits a
Watcher *and* a worn skin *and* a carve → over-saturation, the opposite of restraint. *Mitigation:*
the conductor must be the **single arbiter of the ambient-apparition slot** for the window — it runs
first in `runAutonomyPasses`, claims the slot if it emits, and publishes its claim so the skin/carve
lanes defer (they already accept a "carveClaims / proposedCell" handoff; extend that to an
`apparitionClaim`). One window, at most one figure. This is the sharpest integration risk.

**C5 — Confirm-mode latency breaks deniability.** In `confirm` mode the beat sits `pending` until a
human posts it; by then the player may be elsewhere and the placement (heatmap hot cell) is stale →
the figure spawns where no one is, or in LoS. *Risk:* on-camera misfire. *Mitigation:* the conductor
emits *placement intent* (cell + shape), but the **plugin re-validates LoS + re-anchors to the
player's current hot cell at spawn time** (the beat classes already do reveal-disciplined placement);
confirm-mode just approves *that this shape may fire for this player*, not a frozen coordinate.

**C6 — `mobKills` suppression could mask a deserved beat.** Suppressing escalation for a player the
herd already judged could read as the land *forgiving* a Sacred-Beast killer. *Risk:* tonal
contradiction with INV-13's gravity. *Mitigation:* scope the suppression to *one movement window* and
to *ambient* shapes only — the herd/fork consequence (forks.ts) fires regardless; the conductor just
doesn't *also* pile a Watcher on the same tick. If this feels muddy in playtest, **CUT C6 entirely** —
it's a courtesy, not load-bearing.

**Scaling verdict:** ship §1.1–1.5 and §4 threading. **Cut/optional:** the `mobKills` suppression
(C6) is P2-optional. Everything else is core.

---

## 3. DE-SLOP TEST — exemplar lines in-voice

These are the only *new* player-facing strings this item needs (the report sentence when the
conductor's pick is surfaced in `#the-record`, and the trace register). They reuse existing voice keys
where possible; where new, they pass the anti-slop law (cold, plain, concrete, counts not emotes):

> `It stood at your stores while you were down. It did not take anything. It counted.`

> `Brann mined alone for the seventh time. Something was at the edge of the light when he turned.`

> `The marker you passed standing has a name now. It faces the path.`

> `Three of you saw the figure. One of you was looked at.`

No "felt watched", no "little did he know", no adjective triplets, no em-dash drama. It records.

(Internal trace line, never player-facing, for parity with sibling modules:
`wear watcher_at_edge over Brann (rhyme 0.81 solo; capped 0/2; no carve collision)`.)

---

## 4. THREAD IT (consistency law) — exactly where this lives

### Canon FACTs / INVs it touches or adds
- **Touches FACT 1** (the record keeps a list of the living, by name) — the bias *is* the list,
  expressed as where the watching falls.
- **Touches FACT 9** (the first hauntings were a keeper's fate re-enacted at the group) — the
  conductor is the mechanism that maps a measured habit to the rhyming apparition.
- **Touches INV-13** (only the one Sacred Beast glows/tracks) — by *excluding* the herd from the die.
- **Touches INV-16** (no surface contradicts) — it is the central arbiter that prevents the
  skin/carve/apparition surfaces from colliding.
- **Adds INV-18 (proposed) — "the apparition slot is single-arbiter."** *At most one ambient
  apparition per drama window across all autonomy lanes; the conductor claims it and the skin/carve
  lanes defer.* This formalizes the C4 mitigation as an invariant the self-test can falsify.
- **Adds (lore note, not a numbered FACT):** the bias is **probabilistic** — canon-spine's risk
  ledger already names "never a deterministic callout (R7)"; this realizes it.

### Found-documents / journals that foreshadow it
- A keeper journal page (in `arc/lore/documents/`) where an earlier keeper notes, in degrading hand,
  *"it does not come for all of us the same. it comes most for the one who keeps most, and the one who
  keeps apart."* — plants the *bias* as in-world observation before the group feels it (M1→M3 payoff).
- The teaching-stone rune line at the first stone (shared with FACT 16's `kept`/place vocabulary) —
  the same rune alphabet the carve lane uses, so a glyph learned at a stone re-reads the apparition.

### NPC / Watcher voice lines that carry it
- The `#the-record` report sentences in §3 (cold keeper register). Where the pick coincides with an
  existing escalation, reuse the existing `voice.ts` key (e.g. the One Who Counts reuses the same
  light-toll/keeper line the customs bridge already owns — no new key needed for the common path).
- The M4 atonement quieting reuses the existing FACT-10 "relents" register — no new key.

### Cipher(s) / puzzle(s) it expresses (reuse the 11 built)
- **`atbash` / mirror** — already the Surface-Walker's clue verb (`arg-deepening §1.2`); the
  conductor placing the Surface-Walker at water keeps apparition + cipher on one surface.
- **`coordEncode`** — the back-pointer from a carve to the teaching stone (INV-14, pointer-only); the
  conductor shares its cell vocabulary so a coordinate clue and an apparition site reconcile.
- **`bookCipher`** — Mara's fragment (FACT 2b, "the record keeps no fixed weight") foreshadows the
  conductor's *re-weighting* on atonement (M4). No new cipher is invented; the bias rides existing ones.

### Beat classes / listeners / tables / sites.yml / voice keys it realizes
- **Beats (all built):** `NamedMobBeat` (watcher/counts/surface/stoop/sleepless figures),
  `PrivateSoundBeat` (M1 precursor), `ChestArrange`/`FakeBlockBeat`+`TorchGutterBeat` (One Who Counts),
  `PrivateParticleBeat`/`PrivateDarknessBeat` (Surface-Walker chill), `DoorOpenBeat` (Sleepless).
- **Signal (built, read-only):** `SignalSnapshot.java` — `hoardedScore`, `soloMiningRatio`,
  `distanceFromGroup`, `mobKills`, `complianceFor(custom).violationRatio()`.
- **New pure module:** `discord/src/showrunner/spawn-bias.ts` exporting
  `scoreRhymes(snapshot): ShapeRhyme`, `selectApparition(input, constants): ApparitionDecision`,
  `SPAWN_BIAS_DEFAULTS`, and the `norm*`/`GAMMA` constants — mirrors the shape of `offline-skin.ts`
  and `name-where-never-been.ts` (pure, no DB/clock/LLM, seeded RNG passed in).
- **New self-test:** `discord/src/showrunner/spawn-bias.selftest.ts` — pins the squash constants,
  the precision floor, the cap, the seeded reproducibility, and the herd-exclusion.
- **New run wrapper:** a pass inside `autonomy.run.ts` (`runAutonomyPasses`) — reads dossiers +
  drama-budget slot + active roster, runs `selectApparition`, publishes the `apparitionClaim`, and
  calls `enqueueBeat(<shape beat type>, groupKey, payload, beatStatus)`. Runs FIRST among the
  apparition-emitting lanes (C4). Degrades to no-op when the dossier read is absent (precision).
- **State (no migration):** extend `ShowrunnerState` in `state.ts` with
  `spawn_bias_last_picks?: string[]` (the cooldown ring of recent groupKeys) and
  `spawn_bias_counts?: Record<string, number>` (per-player per-movement cap, reset on movement
  change) — mirrors `worn_skins` / `carve_counts`.
- **types.ts:** add `ApparitionShape` (reuse/extend the `offline-skin` union to all six) +
  `ApparitionDecision` to the showrunner types.
- **sites.yml:** no new site type required — apparitions anchor on the existing heatmap hot cells /
  `BaseDetector` clusters / water anchors / marker anchors the beats already use.

---

## 5. PLANT THE PAYOFF — the "oh, that's what that was for" seed

- **PLANT (Movement I, inert/ambiguous):** the keeper-journal line *"it comes most for the one who
  keeps most, and the one who keeps apart"* is found early as flavor — a dead keeper's superstition.
  Simultaneously, the M1 apparitions read as *random* atmosphere (because emptiness dominates and no
  shape repeats, the group has no evidence of bias yet).
- **PAYOFF (Movement III–IV):** once the group has accumulated enough sessions to *notice* that Brann
  (the hoarder) keeps getting the chest-figure and Iss (the wanderer) keeps getting the lake-figure,
  the journal line re-reads as **literal instruction, not superstition** — the prior keeper had
  watched the exact same bias operate. The "random atmosphere" of M1 is retroactively revealed as the
  same conductor, just throttled. The reveal is *structural* (the group infers it from a probabilistic
  pattern), never blurted.
- **Final lock (Movement V):** the Watcher payoff — "the same figure, counting days" — closes it:
  the bias was the count made visible the whole time (FACT 1). No payoff without the M1 plant; the M1
  plant pays off only because the bias was true and measured, never faked.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

- **Lives in:** Movements **I–V** (throttled M1 → fused M3 → re-weighting M4 → tapering M5). Core
  body of work lands for M-II/M-III.
- **Depends on (must exist first):**
  - **P1.8** (the dossier / `SignalSnapshot` reader feeding the showrunner) — hard dep; without live
    dossiers the conductor degrades to no-op (correct, but inert).
  - The **drama-budget / cooldown slot** read (`FLOW §2`) exposed to the autonomy lane.
  - Active-roster reader (same one `name-where`/grave need; listed in the worker RETURN).
  - All six beat classes (**already built** — R7).
- **Depended on by:**
  - `offline-skin.ts` and `name-where-never-been.ts` consistency — they consume the centralized
    dominant-shape rhyme + the `apparitionClaim` separation handoff. (They function today with private
    rhyme; this *unifies* it and prevents collisions.)
  - The M3 cross-surface "three surfaces, one truth" set-piece (INV-16).
- **Priority:** **P1 (arc-spine).** It is the behaviorally-new half of R7 and the shared brain the
  offline lanes assume. *Within* P1: the **single-arbiter slot (INV-18, C4 mitigation)** is the P0-ish
  must-land-first sub-piece (without it the apparition lanes can double-spend); the `mobKills`
  suppression (C6) is **P2-optional**. The vanilla-fallback shipping of all six (R7) is already done;
  this adds no P0 vertical-slice dependency beyond P1.8.
