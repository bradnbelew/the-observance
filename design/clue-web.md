# The Observance — THE CLUE WEB (non-linear, hard, veteran-tuned)

> Companion to `arc/lore/canon-spine.md` (law), `arc/lore/LORE-BIBLE.md` (the
> 12-doc index), `design/arg-deepening.md` (the 6 keepers + verb menu),
> `FLOW.md` (5 movements / cadence), and `discord/ORACLE.md` (the five
> `outcome_type`s the web plugs into). This file is the **map**: a movement-by-
> movement node list, an edge map, and the dead-end / side-quest / red-herring
> inventory — every node grounded in a real lore doc or beat.
>
> **Its mechanics twin is `design/cipher-web.md`** (the cipher catalog + fairness
> contract + authoring checklist). The two share ONE puzzle_key namespace: the
> authored kebab keys below (`stone-vaun`, `no-wall-catch`, …), which the canonical
> seed `discord/supabase/seeds/puzzles_seed.sql` inserts and `design/clue-web-seed-
> notes.md` grounds. The SEALED endgame rows live in `arc/cipher-web-seed.sealed.json`.
>
> **Direction this is built to:** HARD + NON-LINEAR WEB for ARG veterans. NOT a
> linear "cipher 1..6". The shape is deliberately hidden: 10 cipher families in
> play, **four independent entry paths** so no single missed clue stalls the
> group, true dead-ends (correct answers that open nothing), pure-lore payoffs,
> three side quests, and gated main-story beats. Every reveal of the core
> (FACT 15, the induction twist) is reachable by ≥2 routes and stated by none.

---

## 0. THE WEB AT A GLANCE

- **24 nodes** across 5 movements.
- **Cipher families (10):** rune-ring Rosetta · Caesar shift · book-cipher ·
  Atbash/mirror · monoalphabetic substitution · beacon colour-sequence ·
  Vigenère (keyed) · coordinate-cipher · pressure-plate glyph-walk · the
  inverted "Refrain" (solve by *not* typing the taboo word).
- **Non-cipher / physical (5):** rune-ring assembly ritual · count-the-markers
  observation · count-the-fires observation · the atonement re-walk (re-perform
  a broken custom) · the final synchronized group-crouch.
- **Entry paths (4) — no bottleneck:** (A) the base lectern report; (B) the
  **server-icon rune ring** (metadata rabbit hole — solvable Day 1, before
  anyone touches the world); (C) the **founder margin-note Rosetta** at the
  first stone; (D) any **single keeper-stone** reached by wandering. A, B, C all
  reach literacy; literacy + any one stone reaches the Movement-II spine. The
  Rosetta itself has **two doors** (icon ring B and founder note C), so even the
  literacy gate is not a single point of failure.
- **The two woven threads:** the **Liar (Iss)** seed→doubt→catch→true-coord, and
  the **Seventh Stone** seed→pursuit→payoff, each redundantly cross-referenced.

---

## 1. ASCII EDGE MAP

Legend: `═►` main-story edge · `─►` next-clue / literacy edge · `··►` optional /
side-quest edge · `✗►` dead-end (true answer, opens nothing) · `↺` re-walk /
flip. Node keys are defined in §3.

```
 ENTRY PATHS (parallel — pick any; none is a bottleneck)
 ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
 │ A record-    │   │ B server-icon│   │ C founder    │   │ D wander to  │
 │   opens (M1) │   │   rune-ring  │   │   margin-note│   │   any stone  │
 └──────┬───────┘   └──────┬───────┘   └──────┬───────┘   └──────┬───────┘
        │                  │                  │                  │
        │  (FACT1,2)       └───────┬──────────┘                  │
        ▼                          ▼                             │
   m1-named-habit            ROSETTA (rune-ring assembly) ◄───────┘ (illegible
   (lore: graded            literacy gate — TWO doors (B,C)         until literacy)
    before told)                  │  ─► reads every later stone
        │                         │
        └──────────┬──────────────┘
                   ▼
        ╔══════════════════════╗   the six stones are a WEB, not a row:
        ║ M2 KEEPER-STONE FIELD ║   reachable in any order, each its own cipher.
        ╚══════════════════════╝
   ┌─────────┬─────────┬─────────┬─────────┬─────────┬─────────┐
   ▼         ▼         ▼         ▼         ▼         ▼
 vaun     mara      sella     orin      brann     iss-wall
 (Caesar) (book)    (Atbash)  (subst/   (beacon)  (Vigenère)
   │         │       │  crouch)│  │         │  │
   │         │       │        │  │         │  │  ✗► iss-dead-shrine
   │         │       │        │  │         │  │     (DEAD-END: true coord,
   │         │       │        │  │         │  │      a grave, opens nothing)
   │         │       │··►SEVENTH│ │         │  │
   │         │       │  side    │ │         │  └─► iss-doubt (key=ISS=
   │         ═►rite  │  quest   │ │         │       "the one who turned away")
   │         │       ▼          │ │         │            │
   │         │   seventh-shrine │ │         │            ▼  (M4)
   │         │   (lore: FACT10  │ │         │      no-wall-catch ↺ flips
   │         │    land can      │ │         │      iss dialogue warm→cold
   │         │    refuse)       │ │         │            │
   │         │       │          │ ▼         │            ║ (true coord)
   │      m2-rhyme◄──┴──────────┴─orin-     │            ▼
   │      (lore: fates rhyme)    threshold  │      ═► M3 UNDERCROFT DESCENT
   │                             ↺ completes │         (book-cipher sentence:
   │                             at M4 ladder│          "descend and bow at
   │                                │        │           the unbroken light")
   │                                ▼ (M4)   │            │
   │                          haunting-      │            ▼
   │                          biography      │      undercroft-fog (lore F11/F12)
   │                          (FACT 9)       │            │
   │                                         │            ║
   └─────────────┬───────────────────────────┴────────────┘
                 ▼
        ╔════════════════════╗
        ║ M5 THE ACCEPTING   ║  gate = 6 personal tokens + components +
        ╚════════════════════╝  synchronized group-crouch at the hour
                 │
       ┌─────────┴──────────┐
       ▼                    ▼
  rite-tokens (F13)    pressure-glyph-walk (physical, optional approach)
       │                    │
       └─────────┬──────────┘
                 ▼
          accepting-crouch ═► record-receives (F14)
                 │
                 ▼
          [FACT 15 — felt, never stated: the world flips to KEPT]

 SIDE / OPTIONAL (soft-pressure, never gate progress):
   · unspoken-refrain (M2-M4): solve by NOT typing the taboo word
   · self-rewriting-journal (M1-M4): re-read across nights, lore drip
   · haunted-herd (M2-M5): protect the pale beast → boon at the Accepting
```

---

## 2. WHY IT CANNOT BOTTLENECK (web-not-chain proof)

1. **Literacy has two doors.** The rune alphabet is reachable from the
   **server-icon ring (B)** *and* the **founder margin-note (C)**. Either alone
   teaches the script. (Canon: both carry FACT 3 + the sunwise order; spine §3.)
2. **The six stones are a field, not a row.** Each is an independent cipher with
   its own verb; the group can solve them in any order. You need the *book-cipher
   sentence* (Mara) to open the Undercroft and the *Iss catch* to get the true
   final coordinate — but every *other* stone is optional-redundant lore, so
   missing one never stalls the spine.
3. **The two spine keys are each double-sourced.** The descent instruction lives
   in D05 (book-cipher) **and** is restated by D12; FACT 8 (Iss lied) lands via
   D10 (the catch) **and** D11 (the Seventh's "a wall does not choose").
4. **The Whisper backstop** (`FLOW §3`) auto-gifts a tiered hint if the group is
   truly stalled N days — so even a hard cipher cannot hard-lock the run.
5. **The core (FACT 15)** is supported by seven independent foreshadows
   (3,4,6,11,12,13,14) across all five movements; no single missed node can
   block *or* spoil the landing (spine §3 web-check).

---

## 3. MOVEMENT-BY-MOVEMENT NODE LIST

Each node: **key · cipher/verb · answer · outcome_type · what surfaces it ·
what it pays off / unlocks.** Outcome types map to `ORACLE.md §3`.

### MOVEMENT I — The Notice (entry surface)

**m1-record-opens** · non-cipher (read) · *(no oracle gate — found document)* ·
`lore` · surfaced by: the base lectern, found not delivered. Pays: FACT 1 (the
record counts the living by name) + FACT 2 (graded by laws no one told them);
buries the FACT 14 seed ("it does not close at the rite… it [...] you") and the
"seventh mark the record will not [...]" tease. → points at **rosetta-ring**,
**m1-named-habit**, and (Seventh seed) **seventh-shrine**.

**m1-named-habit** · in-world observation · answer = recognizing *your own*
measured habit named in a report before you knew it was a law · `lore` ·
surfaced by: a personalized report (the scalpel, grounded — spine §6 rule 4).
The Movement-I surprise: the world was grading before the rules were known.
Pays FACT 2 a second door. Terminal lore (no unlock) — pure dread texture.

**rosetta-ring** · **rune-ring Rosetta + assembly ritual** · answer = assemble
the ring **sunwise from the topmost mark: Bow, Offering, Kept-Light, Deep-Line,
Ward, Covering** · `main_beat` · surfaced by: the **server-icon rune ring (B)**
and the **founder margin-note D03 (C)** — *two doors*. Pays FACT 3 (keepers
before this group) + FACT 4 seed. **Unlocks the master script** — every later
keeper-stone becomes legible. This is the literacy spine of the whole arc.
(Front-margin also seeds FACT 15: "they were holding you" — never the twist
itself.)

### MOVEMENT II — The Keeper-Stone Field (the six, in any order)

> All six require literacy (rosetta-ring). They form a *field*: solve in any
> order. Two of them (mara, iss-wall) carry spine keys; the other four are
> redundant lore + the rhyming chorus.

**stone-vaun** · **Caesar shift** (everything held back by a fixed amount) /
verb: rotate a ring of item-frames · answer = the de-shifted plaintext of Vaun's
ledger line (the Caesar key) · `lore` · surfaced by: D02 *counted-them-in-the-
dark* + the cairn at the shaft-mouth. Pays FACT 5 (Vaun's fate = the Offering he
never kept; "given back" column blank) + FACT 4 ("the land counts first").
Rhymes with the hoarder/solo-miner. → feeds **m2-rhyme**; Vaun's first stone is
also the *true* descent turn named by **no-wall-catch** ("six stones to one…
where vaun's stands, turn down").

**stone-mara** · **book-cipher** (page/line/word ×6, walk the lectern shelf) ·
answer = **"DESCEND AND BOW AT THE UNBROKEN LIGHT"** (the assembled sentence —
oracle accepts the sentence, *not* the raw triples; then *do* it) · `next_clue` ·
surfaced by: D05 *page-line-word*, the six-book Kept-Light shelf. Pays FACT 5
(map never tool) + FACT 13 seed ("bring a piece you cannot read your way out
of"). **Unlocks the Undercroft descent** (→ **undercroft-descent**) and routes
toward the rite (D12). One of the two spine keys.

**stone-sella** · **Atbash / mirror** (read faced to the water) + bearing ·
answer = read the reflection, then the margin bearing **"south, by the far water
where she did not come back"** · `side_quest` · surfaced by: D06 *what-the-
surface-keeps* at the shore pool. Pays FACT 5 (Sella/Bow at far water) + seeds
the Seventh ("count again… the last marker is not the last"). **Opens the
Seventh-Stone side quest** (→ **seventh-shrine**). Rhymes with the lone wanderer.

**stone-orin** · **monoalphabetic substitution** (banner-glyph) + **crouch-only
reveal** (the carving faces the floor; a reader who won't stoop reads nothing) ·
answer = the decoded threshold line; margin word **"threshold"** · `next_clue` ·
surfaced by: D07 *i-thought-it-small*. Pays FACT 5 (Orin/Bow, bowed at last to
no one) + FACT 6 seed (the sentence breaks at "i —"). The crouch verb *is* the
Bow — you must perform the custom to read the keeper who broke it. → points to
**orin-threshold** (M4 completion).

**stone-brann** · **beacon colour-sequence** (lights read in order) + **night /
black-moon gate** + count-the-fires observation · answer = the beacon-beam colour
order; recognizing the one fire that will not be doused · `lore` · surfaced by:
D08 *do-not-close-your-eyes-here* (legible only at night). Pays FACT 11 (one fire
never went out) + FACT 12 ("the same word for the people and the flame and the
cold stone"). Rhymes with the one who slept on the Dark Hours. → cross-links to
**seventh-shrine** (doused-vs-kept, same lesson two sides).

**stone-iss-wall** · **Vigenère** (keyed on a name) · answer = **the key is ISS
(his own name); turned on the other stones it decodes to "the one who turned
away"** (oracle confirms the *key/name*, never the comforting doctrine) ·
`next_clue` · surfaced by: D09 *the-ways-are-a-wall*, the warmest, most
trustworthy voice — the trap. Pays FACT 7 (planted lie: "the ways are a wall").
**Forks two ways:** the *doubt* (→ **iss-doubt** / **no-wall-catch**) and the
*false lead* (→ **iss-dead-shrine**, a true coordinate to a grave). The Liar
seed.

**m2-rhyme** · in-world cross-reference (read any two keeper stones together) ·
answer = noticing each keeper's fate matches the custom they broke · `lore` ·
surfaced by: any two of D02/D05/D06/D07 read side by side. Pays FACT 5 fully (the
"rhyming chorus" clicks — the stones are warnings shaped like the people standing
next to you). Terminal lore; collective, never a callout (spine §6 rule 9).

### MOVEMENT II→IV — The Liar Thread (seed → doubt → DEAD-END + catch)

**iss-dead-shrine** · coordinate-cipher (Iss's fragment) · answer = the
**west-and-down dead-shrine coordinate** Iss hands you — it *works*, it leads to
a real place · **`dead_end`** · surfaced by: D09's "go there and you will have
the end of it." **THE LOAD-BEARING RED HERRING.** The coordinate is *true* — the
oracle acknowledges it (`oracleDeadEnd`: "yes. that is the true name of it. and
it opens nothing.") — but it is **a grave**, not the threshold. Opens nothing.
Trusting Iss literally walks you to a dead end. (`no-wall-catch` later names it:
"what iss sent you to was a grave.")

**iss-doubt** · re-read / Vigenère-key cross-check · answer = turning ISS's key
on the *other* stones and finding it disagrees with every honest carving →
"the one who turned away" · `next_clue` · surfaced by: D09's own hard-margin
("we checked the lock… it gives the word we keep for the one who turned away")
and the foot-line ("ask first what a wall is *for*"). Pays the *doubt* that
overturns FACT 7. → routes to **no-wall-catch**.

**no-wall-catch** · re-walk of a "solved" clue (the Stone-after) · answer =
finding the carving **behind the clue falsely marked "kept · solved"**, which
contradicts Iss line for line · `main_beat` · surfaced by: D10 *no-wall-was-
ever-built-here*. Pays FACT 8 (Iss lied; the ways are not a wall — "they were
the reaching let in"). **Flips Iss's whole dialogue tree warm→cold** (`↺`), and
**yields the TRUE final coordinate**: not the dead shrine, but back along the
keeper-row against its order, six stones to one, to **Vaun's** stone, turn down
— "bring the thing only you can give." Refuses to say what the ways *are*
(FACT 15 discipline). → unlocks the **rite** path.

### MOVEMENT III — The Undercroft + the Seventh side quest

**undercroft-descent** · lectern-comparator door + **descend** verb · answer =
*performing* Mara's sentence — descend at the unbroken light · `main_beat` ·
surfaced by: D05's resolved sentence + the Kept-Light lectern door. **Leaves the
group's world for the keepers' (a custom dimension).** Pays the threshold into
Movement III. → **undercroft-fog**.

**undercroft-fog** · in-world witness (the false-climax reversal) · answer =
witnessing the altar room rebuild itself wrong; the single lit point in a doused
world · `lore` · surfaced by: the Undercroft set-piece + D08/D01 fire lines. Pays
FACT 11 (the one kept fire, eternal, attended by no one — beautiful and wrong) +
FACT 12 ("they did not depart… they were kept"). The midpoint gut-punch that
points at the sealed truth without naming it. → feeds **rite-tokens**.

**seventh-shrine** · effaced monoalphabetic (readable only via earned literacy) +
count-the-markers observation · answer = following Sella's bearing to the
**cold-hearth shrine**; counting six stones, then "there was a seventh" ·
`lore` (`side_quest` entry, lore payoff) · surfaced by: D11 *the-seventh-not-
kept*, pointed-to by D06, cross-linked by D08 and D09. Pays FACT 10 (acceptance
is a choice the land makes — it kept six, refused the seventh; "a thing that can
say no… is not a wall… it is something with a will"). **Earns Whisper budget.**
Foreshadows the cast-out ending and the kept/left binary **without** touching the
induction twist — it answers "can the land say no?" (yes) and leaves "what does
*yes* cost?" for M5. Ignorable: it **gates nothing** ("the way goes on without
it"). Distinct-from-Iss ambiguity preserved (D11: the Seventh came *before* the
threshold; Iss came back).

### MOVEMENT IV — The Reckoning (the dossier turns; atonement; the biography)

**orin-threshold** · re-walk (completes the broken carving) + **atonement-bow** ·
answer = bringing Orin's "i —" to D04's third-person completion ("i was not kept.
i was counted, and the count was true, and it was not enough") · `lore` ·
surfaced by: D04 *observed-warned-left-at-threshold* (same hand as D01, later and
colder). Pays FACT 6 (named → warned → left at the threshold; kept/left is a real
binary) + FACT 12 foot-margin (reframes "kept" as captivity: "which of the two is
still free to leave"). → reinforces **haunting-biography**.

**haunting-biography** · keeper-NPC dialogue / exposed-Iss connection · answer =
a keeper (or the now-cold Iss) ties a Movement-I haunting to a named keeper's
fate · `lore` · surfaced by: M4 dialogue carrying FACT 9 (implicit in D04+D07 and
the Iss re-walk; spine TODO-3 routes it to the Keeper-NPC). Pays FACT 9 ("the
dread had a biography" — the first hauntings were a keeper's fate re-enacted at
the group). Terminal lore; recontextualizes Movement I.

**atonement-refrain** · *(the re-walk demanded of a transgressor)* · answer =
honoring a previously-broken custom, then returning to a keeper who withheld its
fragment · `main_beat` · surfaced by: the dossier-branching keeper NPCs
(arg-deepening §2 — "conduct is the lock, the fragment is the key"). Pays the
Movement-IV turn: the record stops being passive; fragments are gated on conduct.
Collective, grounded (only measured transgressions — spine §6 rule 4).

### MOVEMENT V — The Accepting

**rite-tokens** · **bring & deposit** (named items in exact slots) + a personal
token per keeper · answer = lay one personal token in each of six marked slots,
plus the named components (deep's first heart, unbroken light, salt of the
keepers) · `main_beat` · surfaced by: D12 *bring-the-thing-only-you-can-give* +
the altar's labelled TextDisplay slots + D05's "a piece you cannot read your way
out of." Pays FACT 13 (the missing tool is *you*; the group joins something).
→ **accepting-crouch**.

**pressure-glyph-walk** · **pressure-plate glyph-walk** (physical; trace a rune
with footsteps) · answer = the group walks the rune the altar floor names · `lore`
(optional approach to the rite, redundant with rite-tokens) · surfaced by: the
altar floor. A physical, cipher-free verb so the finale doesn't repeat earlier
puzzle shapes; ignorable if the group goes straight to tokens. Pays atmosphere +
the "do, don't decode" lesson Mara left.

**accepting-crouch** · **synchronized group ritual** (all present + right hour +
simultaneous crouch) · answer = everyone bows as one, at the hour, in the kept
light · `main_beat` · surfaced by: D12's "when all of you bow as one." Pays the
sealed turn: collective judgment, no chosen one (spine §6 rule 3). → fires
**record-receives**.

**record-receives** · *(no oracle gate — the world's response)* · answer = the
hidden advancement toast **"⟡ the record receives you"**; the world flips to
**kept** · `main_beat` · surfaced by: D12 ("it receives… it would keep you") +
D01's buried seed + the persistent world flip. Pays FACT 14 (the record does not
stop; it *receives*/keeps you — never "reward"). **The door to FACT 15.**

**FACT 15 (the sealed reveal — felt, never stated)** is delivered by *what
happens* here: the group becomes, in the persistent world, markers / Keeper-
adjacent for whoever comes next; every haunting recontextualizes as the entrance
exam. **No node states it.** Carried by the accumulated weight of rosetta-ring,
seventh-shrine, orin-threshold, undercroft-fog, rite-tokens, record-receives
(spine §3 / §6 rule 2).

### SIDE / OPTIONAL THREADS (soft-pressure, never gate)

**unspoken-refrain** · **the inverted "Refrain"** (solve by *not* typing the
taboo word — `AsyncChatEvent`) · answer = abstaining from the Unspoken when the
puzzle baits you to type it · `side_quest` · surfaced by: the custom table (The
Unspoken). The one puzzle whose solution is *restraint*; typing it is a tracked
transgression. Pays Whisper/atmosphere; never gates.

**self-rewriting-journal** · in-world observation across nights (book `pages` NBT
swaps out of sight) · answer = catching the journal evolve between sessions ·
`lore` · surfaced by: a base lectern journal. Rewards re-reading; pure creeping
lore; skippable.

**haunted-herd** · in-world observation + protection over the run (a tagged pale
mob) · answer = protecting the Sacred Beast across the run · `side_quest` ·
surfaced by: the local herd. Protecting it → a quiet boon at the Accepting;
killing it is a tracked transgression. Ambient, opt-in.

---

## 4. DEAD-END / SIDE-QUEST / RED-HERRING INVENTORY

### Dead-ends (TRUE answer, opens nothing — `outcome_type: dead_end`)
| Node | Why it's a dead-end (true, but no door) | Grounding |
|---|---|---|
| **iss-dead-shrine** | Iss's coordinate *works* and leads to a real shrine — but it is **a grave**, not the threshold. The oracle acknowledges the answer is right and advances nothing. Trusting the warm liar literally dead-ends you. | D09 ("go there and you will have the end of it"); D10 ("what iss sent you to was a grave"). |
| **m1-named-habit** | A correct in-world reading ("the record named *my* habit") that reveals dread but unlocks no path — terminal Movement-I texture. | D01. |
| **m2-rhyme** | Correctly noticing the rhyming chorus is *true* and *load-bearing emotionally* but opens no new node — it recolors what you have, it doesn't gate. | D02/D05/D06/D07. |

> Discipline (`ORACLE.md §3`): a dead-end is **heard** (the watcher acknowledges
> it is right); a genuine *miss* is **silent**. Never collapse the two.

### Side quests (optional branch — `outcome_type: side_quest`)
| Node | What it opens | Pays | Gates progress? |
|---|---|---|---|
| **seventh-shrine** (via stone-sella) | the cast-out shrine | FACT 10 (the land can refuse) + Whisper budget | **No** — "the way goes on without it." |
| **unspoken-refrain** | the restraint puzzle | atmosphere + Whisper; transgression if failed | No |
| **haunted-herd** | protect the Sacred Beast | a quiet boon at the Accepting | No |

### Pure-lore payoffs (`outcome_type: lore` — reveal only, no door)
m1-record-opens · stone-vaun · stone-brann · undercroft-fog · orin-threshold ·
haunting-biography · self-rewriting-journal · pressure-glyph-walk. (Each tells
story; none advances the spine — redundancy is the point.)

### Red herrings woven in
- **Iss the warm voice** — the most trustworthy register hands the most wrong
  lead (the dead shrine) and the FACT-7 "wall" doctrine that is the *exact
  inverse* of the truth. Disproving him walks the group up to FACT 15's edge.
- **"kept · solved"** — D10 sits behind a clue a hand falsely marked solved; the
  red herring is the *marking itself* ("the marking is wrong. read it again").
- **"six keepers"** — the reports/stones insist on six; the stray seventh glyph
  (server-icon ring, D06, D01's "seventh mark") is the bait that opens FACT 10.
- **The kept fire as comfort** — D08/D12 dangle the eternal home-fire as warmth;
  it is the strongest FACT 12/15 foreshadow ("ask who it is being kept for").

---

## 5. THE LIAR + SEVENTH THREADS (woven, redundant)

**Liar (Iss).** Seed `stone-iss-wall` (M2, warmest voice, FACT 7) → fork into the
**dead-end** `iss-dead-shrine` (true coord → a grave) *and* the **doubt**
`iss-doubt` (key=ISS="the one who turned away") → **catch** `no-wall-catch` (M4,
D10) flips his whole tree warm→cold (`↺`) and yields the *true* coordinate (back
to Vaun's stone, turn down). FACT 8 is double-sourced: D10 **and** D11 ("a wall
does not choose"). The lie is the inverse of FACT 15 — to disprove it is to step
toward the real answer without being handed it.

**Seventh Stone.** Seed at `stone-sella` (M2, "the last marker is not the last")
+ D01's "seventh mark" + D08's cast-out one → pursuit `seventh-shrine` (M3, D11,
optional, earns Whisper budget) → payoff FACT 10 (the land chooses; kept/left is
real). Kept distinct from Iss (D11: before the threshold, no stone, no name; Iss
came back). Touches the cast-out ending, **not** the induction twist.

---

## 6. GROUNDING CHECK (every node → a real doc / beat)

| Node | Grounded in |
|---|---|
| m1-record-opens | D01 |
| m1-named-habit | D01 + scalpel report (grounded, measured) |
| rosetta-ring | D03 + server-icon ring (FACT 3/4) |
| stone-vaun | D02 (Caesar / Vaun) |
| stone-mara | D05 (book-cipher; spine key) |
| stone-sella | D06 (Atbash + Seventh bearing) |
| stone-orin | D07 (substitution + crouch/Bow) |
| stone-brann | D08 (beacon + night; FACT 11/12) |
| stone-iss-wall | D09 (Vigenère; FACT 7) |
| m2-rhyme | D02/D05/D06/D07 (FACT 5 chorus) |
| iss-dead-shrine | D09→D10 (the grave; dead-end) |
| iss-doubt | D09 margins (key=ISS) |
| no-wall-catch | D10 (FACT 8; true coord; the flip) |
| undercroft-descent | D05 sentence + lectern door |
| undercroft-fog | D08/D01 fire + the reversal set-piece |
| seventh-shrine | D11 (FACT 10; Seventh) |
| orin-threshold | D04 (FACT 6; completes D07) |
| haunting-biography | D04+D07 + M4 dialogue (FACT 9; spine TODO-3) |
| atonement-refrain | dossier-branching NPCs (arg-deepening §2) |
| rite-tokens | D12 (FACT 13) |
| pressure-glyph-walk | altar floor (verb menu) |
| accepting-crouch | D12 (synchronized bow; collective) |
| record-receives | D12 + D01 seed (FACT 14) |
| FACT 15 | never a node — felt, accumulated (spine §6 rule 2) |

No node invents lore. Keeper voices honored (Vaun ledger, Mara page-refs, Sella
reflection, Orin crouch, Brann night, Iss plain-and-false). FACT 15 stated by no
node. Both threads seeded and paid off. Four entry paths, no bottleneck.
