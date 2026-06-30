# The Observance — ADVERSARIAL CRITIQUE: ARG-CRAFT & WEB-DENSITY

> LENS: arg-craft. Judges the puzzle/clue SET as a veteran-grade non-linear mystery
> web — linearity, in-roads per gate, the "oh that is what that was for" ledger,
> difficulty curve + Whisper backstop, red herrings. Read against the GROUND TRUTH
> of the **shipped seed** (`discord/supabase/seeds/puzzles_seed.sql`, now ~38 rows
> across the base + WEB-REALIZATION blocks; `side_quests.sql`'s 18 travel leaves),
> not the aspirational map. Where `WEB-MASTER.md` and the seed disagree, the SEED is
> what plays.
>
> **This is a fresh pass, not a re-run.** The prior F1–F5 (literacy chokepoint,
> linear M3→V tail, thin ledger, weak Whisper, mono-herring) were largely *taken*:
> `a1z26-tick-stave` is seeded as a real second literacy door; `pressure-glyph-walk`
> is promoted to a second Accepting in-road; the `kind`-switched dead-end family
> ships (`name|count|place|known|prophet`); WEB-MASTER §9 is a real 21-row central
> ledger; the prophet wall + its columnar name herring are seeded. Credit where due —
> the web got materially denser. This pass asks the *next* harder question: now that
> the rows exist, **does the live activation graph actually open them as a web, or
> does a missing flip-lane quietly re-linearize the whole back half?**
>
> Scope discipline: this does NOT re-litigate content-authoring holes COHERENCE-V2
> owns (unbuilt `.schem`, voice-key wiring, listener existence). It asks the narrow
> arg-craft question and flags ONE activation gap only because it silently *destroys
> non-linearity at runtime* — which is squarely this lens's job.

---

## VERDICT

The **authored web is now genuinely veteran-grade** — denser, fairer, and better
ledgered than the prior pass and than most shipping ARGs. The Movement-II stone
field, the Liar fork, the dead-end doctrine, and the new central plant→payoff ledger
are all real and strong.

But the web has a **new, sharper failure than the old linearity charge: the entire
Movement-IV true-coordinate chain and the Seventh deep ship `active=false` with no
seeded lane that flips them to `active=true`.** The resolver only matches `active=true`
rows (`repo.ts getOpenPuzzles → .eq('active', true)`). BUILD-MANIFEST §E authorizes
`apply.ts` to "flip staged **`dead_end`** `active`" — but `bound-word`, `m4-three-hands`,
`threshold-coordinate`, `true-walk-arrive`, `seventh-unwriting`, `seventh-cause`,
`seventh-choice`, `base-docket-reread`, and `meta-unkept` are **not dead-ends** and
have **no flip authority anywhere**. As built, the catch flips Iss cold and routes to
`rite-tokens` directly via `no-wall-catch.next_puzzle_key` — which means the celebrated
co-op-gate / true-walk spine is **authored, seeded, and unreachable**, and the finale
silently collapses back to the exact `catch → rite-tokens → crouch → receives` monorail
the prior critique flagged. The web's best new non-linearity is dark at runtime.

The five highest-leverage tightenings, severity-ranked, each resolvable inside the
existing schema:

---

## SEVERITY-RANKED FINDINGS

### F1 — [CRITICAL] The M4 true-coord chain and the Seventh deep are seeded `active=false` with no flip-lane — the back-half web is unreachable

**Charge.** Nine spine/colorant rows ship `active=false` and depend on a runtime flip
that **only exists for `dead_end` rows**:

| Row | outcome_type | Gate it claims | Flip authority in code? |
|---|---|---|---|
| `bound-word` | next_clue | iss_caught | **none** (not a dead_end) |
| `m4-three-hands` | main_beat | bound_word_known | **none** |
| `threshold-coordinate` | next_clue | threshold_open | **none** |
| `true-walk-arrive` | main_beat | true_coord_known | **none** |
| `seventh-unwriting` | main_beat | iss_caught+seventh_suspected | **none** |
| `seventh-cause` | lore | deep open | **none** |
| `seventh-choice` | main_beat | seventh_named | **none** |
| `base-docket-reread` | lore | iss_caught | **none** |
| `meta-unkept` | lore | iss_caught (fall-order key) | **none** |

`repo.ts` selects `.eq('active', true)`; BUILD-MANIFEST §E (`apply.ts`) reads, verbatim,
"flip staged **`dead_end`** `active`." The seed comments cheerfully assert "the
TS-SHOWRUN lane flips it active at the catch" — but **that lane is authored for one
outcome_type only.** Trace the live edges: `no-wall-catch` (the catch) has
`next_puzzle_key: rite-tokens` AND sets `iss_caught`+`true_coord_known` itself. So at
runtime the group catches Iss and is routed *straight to the rite*. The bound word, the
three-hands co-op gate, the Threshold carving, the true walk, and the entire Seventh
restore/erase deep **never become matchable.** The single most cinematic sequence in the
arc — "get on a call, three hands one word at once, then walk to a real place" — is
present in the SQL and **invisible to the solver.**

**Why it's worse than the old linearity finding.** The prior critique said the back half
was a rail *wearing* a web's clothes. This is the inverse and nastier: the design *built*
the web, then left it switched off, so the seed LOOKS dense (38 rows, beautiful ledger)
while the *playable* graph is the thin monorail. A reviewer auditing the SQL would score
it veteran-grade; a friend group actually playing it would get the catch and be teleported
to the bow with the whole middle of Act 2 dark. That is the most dangerous kind of defect:
invisible to inspection, fatal at play.

**Resolution.**
1. **Generalize the flip-lane from `dead_end`-only to flag-gated activation for ANY
   outcome_type.** Add a small declarative activation table (or a `requires_flags jsonb`
   column on `puzzles`) so each staged row names the flag(s) that open it, and have
   `apply.ts` (already the §E owner) flip `active=true` on ANY row whose `requires_flags`
   are all satisfied in `arc_state` — not just dead-ends. This is the single highest-leverage
   fix in the document; without it, F2–F5 are moot because the nodes they discuss don't run.
2. **Repoint the catch so it does NOT shortcut to the rite.** `no-wall-catch` should set
   `iss_caught` (which opens `bound-word`) and stop, NOT carry `next_puzzle_key: rite-tokens`.
   Today it does both, which is the *mechanism* by which the chain is skippable even if the
   flip existed. The rite must be reached through the chain (or the promoted `pressure-glyph-walk`),
   never handed at the catch.
3. **Add a seedcheck invariant:** every `active=false` row must be named by exactly one
   activation rule, and no `active=true` row may be reachable only through an `active=false`
   predecessor. This is the static test that would have caught F1 and prevents its recurrence.

Until (1) ships, **delete the "TS-SHOWRUN flips it active at the catch" comments** from the
nine rows — they are currently a runtime lie the seed tells itself, the §F1-shape the prior
pass killed at literacy, now recurring across the whole back half.

---

### F2 — [HIGH] `bound-word`'s "SECOND in-road" is a comment with no edge — the one true convergence gate is a single point of failure

**Charge.** The bound word is the load-bearing convergence token: it is the co-op gate's
key, so whoever cannot produce it cannot open the Threshold, cannot get the true coordinate,
cannot do the true walk. The seed comment (line 798) and WEB-MASTER §2 ("Bound word | Iss
Vigenère plaintext | a second keeper-stone (substitution/a1z26) in-road") both assert TWO
in-roads "so it is not a single point of failure." **The second in-road does not exist as a
row.** Grep confirms exactly one node yields `bound_word_known` (`bound-word` itself, fed
only by the catch). No keeper stone's `accepted_answers` or `next_puzzle_key` produces the
bound word independently. This is the *identical fairness lie* the prior pass caught at
literacy ("two doors that are one door"), now relocated to the bound word — and it's worse
here because the bound word gates the co-op gate, which is the hardest coordination beat in
the arc (a synchronized ~20s three-act window). A group that has the right idea but can't
land the Vigenère is hard-locked at the single most frustration-prone node, with no alternate.

**Why it matters.** "Non-linear front, single-thread choke at the climax" is the classic ARG
failure the design explicitly set out to avoid. The co-op gate is exactly where a real group
fractures (someone's offline, the 20s window keeps missing); making its KEY also a
single-source Vigenère stacks two failure modes on one node with zero redundancy.

**Resolution.**
1. **Actually seed in-road B.** Give one already-present keeper stone (the design names
   substitution or a1z26) a `next_clue`/`set_flags` that yields `bound_word_known` — e.g.
   `stone-orin` (substitution) carries a stego layer whose plaintext is "the one who turned
   away," normalizing to the same bound word. One row edit, and the comment becomes true.
2. **Make the stego rune-layer on the Iss Record card a real key-handoff row** (WEB-MASTER §2
   in-road B for `iss_caught` already names it). If the stego card hands the Vigenère KEY
   early, that is a genuine second modality into both `iss_caught` and `bound-word`.
3. If neither ships, **cut the "second in-road / not a single point of failure" claim** from
   the seed comment and §2 — do not let the map advertise redundancy the graph lacks.

---

### F3 — [HIGH] The 18 travel leaves are breadth, not WEB — they touch the spine at zero nodes, so they read as a parallel pamphlet, not a denser mystery

**Charge.** `side_quests.sql` ships 18 destinations, every one `entry_puzzle_key = NULL` and
`gates_progress = false` *by invariant*. That is correct anti-jank (a pooling group can't
share the walk; nothing breaks if you skip them). But for the arg-craft bar it creates a
**two-world problem**: the 18 leaves and the 38 puzzle nodes are **fully disjoint graphs**.
No travel leaf feeds a flag any puzzle reads; no puzzle points at a destination. Five leaves
are flagged "DEAD LEAD / SIDE-TRACK," which is good herring texture — but because they connect
to *nothing*, a veteran group quickly learns "the destinations never matter to the solve" and
stops walking, which collapses the entire longevity layer into optional flavor. The web's
density on paper (56 total nodes) overstates its density *as a mystery to be solved* (the 38
that actually interlock). A leaf that pays "lore/atmosphere/item/time" but never *re-reads a
puzzle you're stuck on* is breadth, not web.

This is the subtlest finding: nothing here is broken or unfair. It's that the travel layer was
built to a *longevity* spec, not a *web-density* spec, and the two were never cross-wired, so
the single biggest pile of content (18 destinations) contributes ~nothing to the "oh that's
what that was for" feeling.

**Resolution (keep the breadth invariant intact — these are *additive non-gating* links).**
1. **Wire 3–4 travel leaves as OPTIONAL second in-roads to existing flags**, never as gates.
   `dest-far-water` is already `KEYED` (Sella's mirror) and seeds FACT-10 — let arriving there
   set `seventh_suspected` (the same flag `stone-sella` sets), giving the Seventh deep a *travel*
   in-road parallel to the stone. `dest-markers-row` "seeds THE COUNT" — let it set a flag the
   `meta-unkept` fall-order key can also be reached through. Each is one optional edge; removing
   all 18 still reconstructs the spine (invariant preserved).
2. **Make at least two dead leads re-read a puzzle.** `dest-dead-shrine` is "the seventh's place,
   kept distinct from Iss" — a group that walked there in M2 should, at the catch, get a re-read
   beat ("the cold hearth you walked to was the surface of the deep you can now open"). That turns
   a flavor walk into a planted payoff — the exact mandate, currently unserved by the travel layer.
3. **Add a "you have been here before" recognizer**: if the true walk (F1) sends the group to a
   destination a player already visited as a rumor, the arrival voice acknowledges it. Cheap,
   uses existing visit state, and is the single most "it knows you" beat the travel layer could pay.

---

### F4 — [MEDIUM] The Whisper backstop still earns most of its budget downstream of the hardest walls; the difficulty curve front-loads frustration

**Charge.** The prior pass flagged that Whisper budget is earned at content gated behind the
first hard wall. The seed shows this only partly fixed: `whisper_budget_earned` is set by
`seventh-shrine` (M3, gated behind Sella's Atbash) and `seventh-cause` (M3, `active=false`).
There is **no Act-1 starting budget row and no pre-literacy earn-point in the seed.** So the
budget curve is: ~zero through the literacy gate and the entire six-stone field, then it arrives
in M3 — meaning the backstop is thinnest across M1–M2, which (literacy leap + six cold ciphers)
is precisely where a new group accumulates the most frustration. The auto-gift can't fire what
the group hasn't banked. Compounding: `max_attempts` is now set on the right nodes
(`stone-iss-wall:6`, `prophet-wall-name:6`, `bound-word:6`, `meta-unkept:8`) — genuinely good,
the brute-force inversion is fixed — but capping attempts without a solvent early Whisper budget
means an early-stuck group hits caps with no hint reservoir, which reads on camera as a dead halt.

**Resolution.**
1. **Seed a small Act-1 starting budget** (a flag or counter set at `prologue_ignited` / first
   report found) so the backstop is solvent at the literacy gate and the stone field.
2. **Add ONE pre-literacy earn-point.** The `a1z26-tick-stave` is already a M1 main_beat; have
   it grant a Whisper drip-credit in addition to `rosetta_known`, so the easier of the two
   literacy doors also *funds* the harder beats downstream. This braids the F1-fix node into
   the difficulty curve at no extra content cost.
3. **Author the 3-tier hint ladder for the 5 hardest LIVE nodes only** (rosetta-ring,
   stone-mara bookCipher, stone-iss-wall Vigenère, no-wall-catch, the co-op gate once F1 makes
   it reachable). The seed carries no hint tiers; an auto-gift today has nothing graded to give.
   Finite, bounded, and it is what makes "HARD" safe — but it is moot at the co-op gate until F1.

---

### F5 — [MEDIUM] The "oh that was for" ledger is excellent but still M4-clustered, and three of its richest rows are gated behind the F1 dead nodes

**Charge.** WEB-MASTER §9 is a real, central, 21-row plant→payoff ledger — a major upgrade and
exactly what the mandate demands. But two structural weaknesses remain. (a) **Payoff timing is
still M4-heavy:** counting the ledger's "Payoff in" column, the plurality fire at M4 (the catch
cascade). The arc still has essentially one big "OH" movement; M2 and M3 carry few re-reads of
their own. The design *says* it staggered them, but the seed's M2/M3 lore rows (`stone-*`,
`undercroft-fog`) mostly *plant* and rarely *pay off* an earlier plant. (b) **The ledger's most
mechanical, best payoffs are gated behind the F1 dead nodes:** #5 (Hold-Book give-back column →
`base-docket-reread`), #19 (prophet's-wall name → re-read), #17/#20 (Seventh fork) all depend on
rows that are `active=false` with no flip. So the ledger advertises payoffs that, at runtime, never
fire. The ledger is honest about being a go-live *gate* — but the gate isn't enforced by a test, so
"no payoff ships without a seeded plant" is currently vibes, and "no payoff ships **dead**" isn't
even a stated rule.

**Resolution.**
1. **Stagger at least two payoffs EARLIER.** Seed one M2 re-read (solving `stone-vaun`'s Caesar
   re-reads the M1 offering-toll the group already paid: "the deep went dark *because* the
   given-back column was blank") and one M3 re-read (the Undercroft's single lit point re-reads
   `stone-brann`'s "one fire never doused" plant). Both use plants that already exist; they just
   need a payoff edge.
2. **Add the missing ledger STATUS column "live-at-runtime"** (distinct from "seeded"): a payoff
   whose node is `active=false`-with-no-flip is `DEAD`, not `LIVE`. Make F1's seedcheck assert
   zero `DEAD` ledger rows at go-live. This converts the ledger from a description into an
   enforceable contract — the single process fix that keeps the mandate honest.
3. **Plant `record-receives` (FACT 14, the heaviest hinge) with one MORE early mechanical anchor.**
   The ledger claims #5/#6/#21 plant it, but #5 (`base-docket-reread`) is an F1 dead node and #21
   (grave) pays at V — so pre-V the "receiving" is thinly planted. Seed a M2-visible inert token-slot
   at the rite site that a player sees empty for the whole arc and that re-reads at V as "the thing
   only you can give." Mechanical, early, and it makes the biggest payoff the most-planted instead
   of the least.

---

## THE WEAKEST IDEA IN THE SET

**The travel layer's five "DEAD LEAD / SIDE-TRACK" destinations, as currently wired, are the
weakest content in the set** — not because dead leads are wrong (they are top-tier ARG craft) but
because, per F3, they connect to *nothing*. A dead lead is satisfying only when arriving
*contradicts a specific hope the solve gave you* and then *re-reads something*. `dest-warm-town`
("Aro's warm-town lie, wrong-scaled deep collapse, contradicted") and `dest-way-up` ("Iss's
forgotten Mouth — real but saves no one") are pure flavor culs-de-sac: you walk 2k blocks, it's
hollow, and it touches no puzzle and no flag. That is an *expensive* dead end (a long walk) paying
the *cheapest* texture (a contradicted card). Either wire them per F3.2 (let the arrival re-read a
stuck puzzle or set an optional flag) or fold the five down to two of the best — five disjoint
flavor walks dilute the load-bearing herring (Iss → the grave), which is the only herring that
*costs a journey and re-reads at the catch*. Protect that one by not surrounding it with four
cheaper imitations.

(Runner-up weak link: `prophet-wall-name` and `prophet-wall-comfort` both carry `kind: prophet`
voice, so the two prophet-wall dead-ends speak the *same* taunt — a missed chance to let the warm
rungs and the hidden-name read sound different. Minor; one voice_args change.)

---

## WHAT IS GENUINELY STRONG (protect these)

- **The two-door literacy gate is now real** — `a1z26-tick-stave` is a genuinely different-modality
  in-road to `rosetta_known` (numbers, no runes). The prior pass's #1 charge is *fixed in seed*. Do
  not regress it.
- **The Movement-II stone field** — six independent ciphers, six verbs, any order, resolver ignores
  movement. Still the spine of the non-linearity and still veteran-grade.
- **The Liar engine** (`stone-iss-wall` → `iss-warm`→grave vs `iss-doubt`→`no-wall-catch`) — one key,
  two readings, a TRUE coordinate that walks you to a real grave. Best single piece of craft in the
  set. The warm/skeptical fork is now player-driven in the seed. Touch only to add F2's second in-road.
- **The `kind`-switched dead-end family** (`name|count|place|known|prophet`) — shipped. Four walls now
  read as the world recognizing different things, not one stuck string. The prior pass's cheapest win,
  taken.
- **The opaque-sentinel rite** (`accepting-crouch` / `record-receives` carry wordless high-entropy
  tokens, never a typeable phrase) — correctly refuses brute-force at the climax. Keep exactly as is.
- **The central plant→payoff ledger** (WEB-MASTER §9) — its existence is the structural upgrade the
  prior pass demanded. F5 tightens it; it does not undo it.

---

## TOP-5 HIGHEST-LEVERAGE ADDITIONS/TIGHTENINGS (ranked)

1. **Build the general flag-gated activation lane (F1)** — flip ANY staged row (not just
   `dead_end`) when its gate-flags are met, repoint the catch off its `rite-tokens` shortcut, and
   add the seedcheck that forbids unreachable `active=true` rows. Without this the entire authored
   back-half web is dark at runtime; it dominates everything else in this doc.
2. **Seed `bound-word`'s real second in-road (F2)** — one keeper-stone edge (or the stego card key
   handoff) that yields `bound_word_known`, so the co-op-gate key is not a single-source Vigenère
   at the highest-coordination node.
3. **Cross-wire 3–4 travel leaves into the puzzle graph as optional non-gating in-roads + re-reads
   (F3)** — turn the biggest content pile from a disjoint pamphlet into web density, preserving the
   breadth invariant.
4. **Make HARD safe early (F4)** — seed an Act-1 Whisper budget + a pre-literacy earn-point on
   `a1z26-tick-stave`, and author 3-tier hints for the 5 hardest live nodes only.
5. **Stagger + enforce the ledger (F5)** — move two payoffs into M2/M3, add a "live-at-runtime"
   status column, and gate go-live on zero DEAD ledger rows; plant `record-receives` with one more
   early mechanical anchor.

---

## SEALED-DISCIPLINE CHECK

This critique names FACT 15 only by its mechanical effect (world → `world_kept`) and never spells the
induction twist. All proposed additions sit *before* that line and feed it without stating it. The F3
travel re-reads and the F5 early plants must themselves stay spoiler-free in their default surfaces
(the destinations' verified/contradicted cards reference `arc/`-sealed payoffs only by their inert
form). The F1 activation table is a pure mechanism change and carries no canon.
