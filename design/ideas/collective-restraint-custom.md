---
id: collective-restraint-custom
title: The Collective-Restraint Custom — the Unlit Deep (the whole group must NOT)
kind: design/treatment (idea expansion; integrates EXISTING canon, not net-new law)
status: TREATMENT — build-ready; the latch is already canon (§8.3) + half-scaffolded (config.yml, plugin.yml)
movement: III (arms) → V (pays off); planted I→II
verdict: KEEP-SCALED (one latch, one place, one toll, one warmth — resist every urge to grow it)
grounded_in:
  - arc/lore/canon-spine.md §8.3            # the_unlit_deep is the ONE group latch INV-17 permits; FACT 11
  - arc/lore/canon-spine.md §7 INV-11/12/16 # active-only, colors-never-gates, never names which player
  - design/content/customs-punishment.md    # the toll law (INV-8), stage-A/B shape, voice register
  - plugin/.../signal/listener/CustomComplianceListener.java  # the closed-set boundary doc (the two "eighths")
  - plugin/.../signal/listener/ChatListener.java + DarkHoursListener.java  # the_unspoken + dark-hours machinery reused
  - plugin/src/main/resources/config.yml §customs.unlit-deep + §restraint  # the EXISTING scaffold this realizes
  - plugin/src/main/resources/plugin.yml    # the UnlitDeepListener cross-owner hook note
  - FLOW.md                                  # Act/Movement cadence; soft-pressure rule
custom_key: the_unlit_deep          # the one group-scoped latch (NOT a CUSTOM_KEYS member; a separate latch)
arms_in: III
pays_off_in: V
priority: P1 (arc-spine) — NOT vertical-slice; do not build before the seven per-player ways prove out
---

# The Collective-Restraint Custom — the Unlit Deep

> The pitch: a law the WHOLE group must refrain from. One slip by one player breaks it for
> everyone, so the group must police itself. Abstinence, shared, is the mechanic.
>
> The finding: this is **already canon** — `the_unlit_deep`, the single group latch INV-17 permits
> (canon-spine §8.3), with a config scaffold (`customs.unlit-deep`) and a named listener hook
> (`UnlitDeepListener`) already reserved. This treatment is not an invention; it is the build-out of a
> seam the synthesis pass left open. Crucially it **braids three already-tracked per-player ways** —
> deep-line + dark-hours + kept-light — into one group condition, so it adds almost no new tracking
> surface, only a new *resolution* of existing signals. That is what keeps it from being an orphan.

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 What the law actually is (one sentence, in fiction)
*Do not light the deep on the black moon.* Below the marked depth (`deepLineY`, −48), during the
taboo "black" moon phase ({0}), **no flame may be lit by anyone**. While the group keeps this, the
one fire that never went out through the abandoned years (FACT 11, the Undercroft hearth) **lends its
glow** down into the deep — a borrowed warmth, a faint kept-light that follows the group below the
Line on the worst night. The instant any one active player lights a flame down there on that night,
the borrowed glow **withdraws — for everyone**. Reversible. It returns on the next black moon the
group keeps the deep dark, or the moment the night turns over.

### 1.2 Why these three ways, braided (not an arbitrary "don't")
The restraint is not a fourth thing bolted on; it is the **intersection** of three ways the group has
already been taught the hard way by Movement III:
- **the_deep_line** — below the marked depth (Iss's sin, the Break).
- **the_dark_hours** — on the black moon (Brann's sin, the Dark reaches the unwaking).
- **the_kept_light** — but here *inverted*: the one place keeping your own light is the transgression,
  because the deep's dark on the black moon is what the borrowed glow is *for*. You do not light it;
  you let the old fire carry you.
The fiction: down there, on that night, your small flame does not help — it **answers** the dark by
showing it where you are, and it severs you from the only light that was holding. The group has to
trust an inherited fire over its own torch. That is the whole tension: every Minecraft instinct says
*light the cave*, and the law says *don't, not here, not tonight, not any of you*.

### 1.3 How it plays across the 5-movement arc

**Movements I–II (planted, inert).** The group cannot yet trigger it (the latch is gated behind the
Undercroft fire existing, which they have not found). What they CAN find is the *language* of it,
sitting harmless among other lore:
- a founder margin in the deep-line survey (R06) that reads, of the abandoned years, "we kept the
  deep dark on the black moons and the old fire kept us. that was the last thing we kept together."
- Wenna's folk-charm half-memory (she keeps six ways, forgets a seventh) — once, she mutters "and
  you never bring your own light down on a black moon. The hold's light is enough. …no, I don't know
  why. You just don't, all of you, or none of you." (the accidental Rosetta; the *all-or-none* is the
  seed.)
These read in M I–II as ambient custom-lore. Nobody can act on them yet. **Inert plant.**

**Movement III (arms).** The group finds the Undercroft (FACT 11 — the one lit point in a doused
world). The moment that single fire is known to exist, the latch arms (`customs.unlit-deep` gated on
the Undercroft being found). Now, on the next black moon, a group that goes deep gets the borrowed
glow — a faint, sourceless warm light that is *not theirs*, that follows them below the Line. The
first time it's deniable: "huh, it's not as dark down here as it should be." Then somebody, on
instinct, places a torch — and the borrowed glow **goes out, everywhere, for all of them at once.**
Cold. No message (stage A). The group has to notice: *it went dark when one of us lit something.* The
self-policing begins here, in M III, the moment the mechanic is real.

**Movement IV (the social pressure peaks).** Reports and the catch (Iss caught) have taught the
group that the ways are not a wall (FACT 8) — and now they're keeping one *together*, by not-doing,
and watching each other do it. The Watcher names the lapse only on repetition (stage B): it records
that the deep was lit on the black moon, that the kept dark was broken, **never which of them did it**
(INV-16 — `broken_by` is recorded in state but never spoken). The group knows one of them broke it;
the world refuses to tell them who. That refusal is the horror and the bond: they have to govern
themselves, because the record won't do it for them. This is where the mechanic earns its keep — it
is the only custom in the whole arc that the *group* keeps or breaks, not an individual.

**Movement V (pays off).** The Accepting. The borrowed glow's behavior across the arc becomes legible:
the Undercroft fire was never decoration and never "free light" — it was the **first proof** of FACT
12 ("the kept ones did not depart; they were kept"). The fire kept burning because a kept group is
*how* it keeps burning; lending its glow to a group keeping the deep dark is the land already
treating them as keepers. The restraint custom was the group rehearsing, before they knew it, the
thing the Accepting asks: to be kept *together*, collectively, with no chosen one. The ending selector
(INV-11) reads it as a neutral colorant — a group that kept the Unlit Deep through M III–V tints one
Keeper clause warmer; it **gates nothing** (INV-12).

### 1.4 The mystery it carries
The "oh" is in the inversion: all arc long, light = safety, the Kept Light is the first and gravest
way. Then in one place, on one night, *your* light is the lapse and an *old, borrowed, not-yours*
light is the keeping. Read at M V, that flips the whole Kept Light way: the custom was never "make
light." It was "**be kept by the light**" — and the deepest keeping is to let an inherited fire hold
you and add nothing of your own. That is the induction twist (FACT 15) rehearsed as a mechanic,
without a word stating it.

---

## 2. CRITIQUE — adversarial, honest

**R1 — Collective-punishment law risk (the sharpest).** "One slip breaks it for everyone" can read as
*punishing the group for one player* — a near-cousin of the forbidden "punish the group for an absent
member." On camera it could feel unfair: a friend lights a torch by reflex and everyone's light dies.
- **Mitigation (decisive):** the toll is **warmth, not progress** (INV-8) and **reversible within the
  same night** — the glow returns the moment the deep goes dark again or the night turns. It never
  blocks a clue, never kills, never deletes. The "punishment" is *the borrowed gift pausing*, not a
  penalty inflicted — framed as the old fire flinching, not the group being fined. And it is keyed on
  **active players only** (INV-11/16): an offline friend's earlier torch can never break it. Critically
  it is **never the absent member** — the latch reads only flames lit *now, by present players*. This
  keeps the collective law on the right side of the "never punish for an absent member" rule.

**R2 — `broken_by` precision (the "it knows me" trap).** If the Watcher ever named *who* lit the deep,
it would (a) violate INV-16 and (b) risk a wrong callout (a flame lit at −47 vs −48; a lantern placed
a tick before the phase flipped).
- **Mitigation:** the latch detects only **explicit flame acts** (BlockPlace of a flame material, or a
  debounced held-flame edge) — **never ambient light sampling** (precision over recall; config already
  mandates this). `broken_by` is stored for the dashboard/ending math but **never surfaced** in any
  voice line. The Watcher says "the deep was lit," never "you lit it." A wrong "you" is worse than no
  "you."

**R3 — Orphaned-gimmick risk.** A bespoke group toll that touches nothing else would be a defect.
- **Mitigation:** it is the *opposite* of orphaned — it braids three existing tracked ways (no new
  per-player tracker), its warmth is the already-canon Undercroft fire (FACT 11), it expresses FACT 12
  and rehearses FACT 15, and it threads Wenna/R06 lore already authored. It adds **one** listener and
  **one** group-state row. See §4.

**R4 — Discoverability / fairness.** A group that never goes deep on a black moon never sees it; a
group that does might not connect the torch to the dark-out.
- **Mitigation:** it **colors, never gates** (INV-12) — missing it costs nothing. For groups that do
  trigger it, the link is taught from ≥2 surfaces (R06 margin + Wenna's all-or-none mutter), and the
  toll points at itself (the warmth withdrawn IS the borrowed light, so "our light died when we lit
  ours" is legible). Stage A is deniable by design; stage B names the lapse so a stuck group gets the
  nudge without an instruction.

**R5 — On-camera reveal discipline.** The borrowed glow appearing/withdrawing must never be *witnessed*
mutating (anti-jank reveal law).
- **Mitigation:** the glow is a per-player light/particle ambience driven by latch state, not a block
  swap — it fades over a tick or two on a state change, never pops a visible edit. It rides the
  existing private-light beat machinery (PrivateDarknessBeat / PrivateParticleBeat family), which is
  already reveal-safe.

**R6 — Scope creep (the real temptation).** It would be easy to grow this into "five collective laws."
- **Verdict: CUT that impulse hard.** INV-17 permits **exactly one** group latch. Abstinence-as-mechanic
  is powerful *because it is rare and singular*. A second collective law halves the weight of this one
  and doubles the unfairness surface. **One latch. One place. One night. One warmth.** Keep-scaled.

**Net verdict: KEEP-SCALED.** Build the single latch exactly as canon scoped it; do not generalize.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (proof it can be written cold)

Stage-B Watcher line (named lapse, never the actor, never an instruction; lowercase, no contraction,
no emotion, collective):

> `tollUnlitDeep`: "a flame was lit in the deep while the moon was black. the borrowed light is
> drawn back. the old fire keeps no one who will not keep the dark."

Relief line when the group keeps it again (bare, cooling — relief, not a boon; this latch is
abstinence, so it earns no "kept" reward, only the warmth resuming):

> `keptUnlitDeep`: "the deep is dark again. the old fire reaches down. it is lending, not given."

Founder margin in R06 (found, dated-feeling, a later hand, omission doing the work):

> "we kept the deep dark on the black moons and the old fire kept us. it was the last thing we kept
> together. then one of us did not, and i will not write which."

Wenna's all-or-none mutter (folk-charm, half-forgotten, the seed of the collective rule):

> "you never bring your own light down on a black moon. the hold's light is enough. all of you, or
> none of you — that part i remember. the why, no."

(Each passes the anti-slop law: no "a testament to," no named emotion, no three-adjective list, no
"not just X but Y," no exclamation. The toll is shown through the concrete withdrawn thing — the
drawn-back borrowed light — not a described feeling.)

---

## 4. THREAD IT — exactly where it lives (so it is not an orphan)

**Canon FACTs it touches / adds.**
- **No new FACT integer** (correct — §8.3 already owns this latch; minting a FACT would be drift).
- **Expresses FACT 11** (the one fire never went out) — gives that fire a *mechanic*, not just a sight.
- **Foreshadows FACT 12** (the kept ones were kept) — the fire lends to a kept group because keeping is
  how it burns.
- **Rehearses FACT 15** (induction; collective, no chosen one) — the only custom kept *as a group*.
- Obeys **INV-11** (active-only, names no player, not an ending selector input beyond a neutral tint),
  **INV-12** (colors, never gates), **INV-16** (`broken_by` recorded, never derivable as a person),
  **INV-17** (the one permitted group latch; nothing else enforced).

**Found-documents / journals that mention or foreshadow it (all existing corpus).**
- **R06 — SURVEY OF THE DEEP LINE**: add the founder-margin (§3 above). Primary plant.
- **R02 / the Undercroft journal line** ("we left, but the light is kept", FACT 11): the borrowed glow
  is this line made playable — no new doc, a re-read.
- **npc:wenna**: the all-or-none mutter (§3). Second independent door (fairness).
- (Optional, M IV) a single abandonment-era report in passive voice: "the deep was lit on the black
  moon. the dark was not kept. no hand is named here." — mirrors the Watcher's `broken_by` silence.

**NPC / Watcher voice lines that carry it (new voice keys → discord/src/voice.ts at integration).**
- `tollUnlitDeep` (stage B; §3) and `keptUnlitDeep` (relief; §3). Register-matched to SET-B / the
  existing `voice.ts` toll family in customs-punishment.md.

**Cipher(s) / puzzle(s) that express it (reuse the built 11).**
- **a1z26 + atbash** on the Undercroft hearth glyph-line, decoding to "dark / black moon / all of you"
  — the *condition*, learnable as a puzzle so a group that decodes it understands the latch before
  triggering it (turns a blind toll into a solved law). Reuse, no new cipher.
- The deep-line / Iss thread already uses **vigenere** (Iss's keyed lie) and the dark-hours uses the
  **beacon/colour-sequence** (Brann); the latch sits at the intersection of clues those ciphers
  already gate, so it needs no cipher of its own. Good: fewer moving parts.

**Beat classes / listeners / tables / config / sites it realizes (real symbols).**
- **NEW listener:** `UnlitDeepListener` (already named in `plugin.yml` + CustomComplianceListener
  Javadoc as a reserved cross-owner hook). Group-scoped; subscribes to `BlockPlaceEvent` (flame
  materials) + a debounced held-flame edge; checks `y <= deepLineY` AND `isTabooMoonPhase(phase)` AND
  Undercroft-found AND `restraint.enabled`. Flips one latch.
- **TrackerConfig:** add `CUSTOM_UNLIT_DEEP` constant + `unlitDeepEnabled()`, `flameMaterials()`,
  `unlitDeepDeepLineY()`, `unlitDeepTabooMoonPhases()`, `unlitDeepCooldownMs()` — all already specced in
  `config.yml §customs.unlit-deep`. Reuse `isTabooMoonPhase()` and the deep-line Y frame.
- **Group state (Supabase):** one new row/table — `unlit_deep_latch(broken, broken_by, broken_at,
  black_moon_window)` — group-scoped, NOT per-player `punishment_state`. `broken_by` stored, never
  surfaced.
- **Beat:** reuse `PrivateDarknessBeat` / `PrivateParticleBeat` (per-player borrowed-glow on / withdraw
  off) — reveal-safe, no block swap. No new beat class needed.
- **config.yml:** `customs.unlit-deep` + `restraint.enabled` — **already present** (lines 203–224).
- **plugin.yml:** `UnlitDeepListener` registration hook — **already noted** (line 16).
- **sites.yml:** the Undercroft hearth site (the borrowed-glow source / gate) — uses the existing
  Undercroft/FACT-11 site; no new site type.

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for" seed

- **PLANTED (inert), Movements I–II:** the Undercroft fire is found/known as a marvel — "the one fire
  that never went out." It reads as atmosphere and a FACT-11 lore beat. The R06 margin and Wenna's
  all-or-none mutter sit in the corpus as ambient custom-lore the group cannot act on. Nobody knows
  the fire *does* anything.
- **REACTIVATED, Movement III:** the latch arms. The fire's glow is suddenly *lent* down into the deep
  on the black moon — the marvel becomes a mechanic. First "huh, it's reaching us down here."
- **PAYS OFF, Movement V:** at the Accepting the group re-reads it: the fire kept burning because a
  *kept* group is how it burns; lending its glow to a group keeping the deep dark was the land already
  keeping them — FACT 12 made literal, the induction (FACT 15) rehearsed. **The oh:** "the fire that
  never went out wasn't a sight. it was the land already keeping us, and the one law we kept together
  was us learning to be kept." No plant without this payoff; no payoff un-planted.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES

- **Lives in:** **Movement III** (arms) → **IV** (social pressure / stage-B naming) → **V** (pays off).
  Planted inert in **I–II**.
- **Depends on (must exist first):** FACT 11 Undercroft fire + its `sites.yml` site (the gate + warmth
  source); the three braided trackers (`the_deep_line`, `the_dark_hours`, `the_kept_light` — all built);
  `isTabooMoonPhase()` (built, DarkHoursListener); `restraint.enabled` master kill (config, built);
  the reveal-safe private-light beat family (built).
- **Depended on by:** nothing *gates* on it (INV-12 — colors only). The M V ending composer reads its
  group-kept state as **one neutral tint** (INV-11). The "oh" payoff (§5) leans on it but does not
  break if a group never triggers it (the FACT-11 fire still pays off via other doors).
- **Priority: P1 (arc-spine).** NOT vertical-slice (P0). Do **not** build before the seven per-player
  ways prove out in playtest — the latch is a capstone that braids them, and it is only legible once a
  group already fears the deep, the black moon, and respects light. Build after Phase-0 proves the
  haunting; wire the `UnlitDeepListener` when the Undercroft (FACT 11) site lands.
