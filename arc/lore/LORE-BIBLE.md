# The Observance — LORE BIBLE (operator's index to the corpus)

> Operator-facing index. Maps the 12 authored fragments in
> `arc/lore/documents/` against `canon-spine.md` and the `found-documents.md`
> brief. Use this to answer "where does fact X live?", "what does clue Y
> unlock?", and "is the corpus internally consistent?". The spine is law; this
> file only indexes and audits it.
>
> Sources of truth: `canon-spine.md` (§1 cast, §2 timeline, §3 the 15 facts,
> §4 Liar thread, §5 Seventh Stone thread, §6 hard rules) and
> `found-documents.md` (the D01–D12 brief). The 12 files on disk are the
> authored realization of D01–D12.

---

## 1. DOCUMENT INDEX (one line each)

Map of file id ↔ brief id, with kind, author/register, and movement.

| Brief | File id | Title | Kind | Author / register | Mvmt | Clue | One-line |
|---|---|---|---|---|---|---|---|
| D01 | `the-record-opens` | the record opens | record | the Archivist / the record | 1 | no | The base lectern's first report: the count of the living has begun by name (F1); it grades habits no one was told were laws (F2); closes with the buried "we left, and the light was kept" foreshadow (F14) and the "seventh mark the record will not [...]" tease. |
| D02 | `counted-them-in-the-dark` | i counted them in the dark | ledger | Vaun, the Hoarder | 2 | no | A founding-line tally-stick strongbox, inventories only, second "given-back" column blank — proves Vaun never kept the Offering (F5), and leans on "the land counts first" (F4). |
| D03 | `learn-them-as-we-learned-them` | learn them as we learned them | margin-note | the founding line | 1→2 | YES | The Rosetta seed: the ways were found not made (F4 seed, F3), written around the rune ring that matches the server icon; assembling the ring sunwise unlocks the script every later stone is written in; ends on "they were holding you" (F15 seed). |
| D04 | `observed-warned-left-at-threshold` | observed, warned, left at the threshold | record | the Archivist / the record | 2→4 | no | The cold escalation ladder written about Orin (observed→warned→left); kept/left is a real per-conduct binary (F6); finishes Orin's broken sentence; the foot-margin reframes "kept" as captivity (F12), asking which of kept/left is still free to leave. |
| D05 | `page-line-word` | page, line, word | letter | Mara, the Reader | 2 | YES | A book-cipher assembled by walking the six-book lectern shelf; her fate = the map never the tool (F5); resolves to "DESCEND AND BOW AT THE UNBROKEN LIGHT" and demands each keeper bring a piece of their own (F13). |
| D06 | `what-the-surface-keeps` | what the surface keeps | journal w/ marginalia | Sella, the Drowned | 3 | YES | Reflection-only (Atbash) journal at the shore pool; "count again… the last marker is not the last" seeds the Seventh; the steadier margin gives a bearing south to the doused shrine and hands off to `the-seventh-not-kept` (F10 seed). |
| D07 | `i-thought-it-small` | i thought it small | inscription | Orin, the Silent | 2 | no | Crouch-only threshold carving; would not bow, bowed at last to no one (F5); the sentence breaks at "i —", margin word **threshold** points to D04 for the completion (F6). |
| D08 | `do-not-close-your-eyes-here` | do not close your eyes here | journal | Brann, the Night-Walker | 3 | no | Night/black-moon-only journal; the home-fire that will not be doused (F11) read against the doused shrines; "they were kept… the same word for the people and the flame and the cold stone" (F12); points to the Seventh. |
| D09 | `the-ways-are-a-wall` | the ways are a wall | letter | Iss, the Liar | 2 | YES | The Liar's seed: warmest voice, the comforting doctrine "the ways are a wall" (F7); Vigenère key = his own name ISS, decoding to "the one who turned away"; sends you to the dead shrine; margins pre-plant the doubt. |
| D10 | `no-wall-was-ever-built-here` | no wall was ever built here | inscription | the Archivist / the record | 4 | no | The catch: a Stone-after behind a clue falsely marked "kept · solved" contradicts Iss line for line (F8: no wall — the ways are the reaching let in); refuses to say what the ways ARE; yields the true coordinate ("bring the thing only you can give"). |
| D11 | `the-seventh-not-kept` | the seventh, not kept | map-note | the cast-out keeper era (effaced) | 3 | no | The Seventh Stone payoff: an effaced map-note to a cold-hearth shrine; counts six stones then "there was a seventh," cast out *before* the threshold and unwritten; a thing that says yes to one and no to another "is not a wall… it is something with a will" (F10); ends "it has not yet said which we are." |
| D12 | `bring-the-thing-only-you-can-give` | bring the thing only you can give | letter / summons | the Keeper (the one who presides) | 5 | no | The rite instruction: the missing tool is *you*, a personal token per keeper, six (F13); the record does not close — it **receives**, it keeps (F14); the warmth-under-dread "we would keep you, if you would keep the ways," and stops short of F15. |

**Register coverage:** Archivist/record = D01, D04, D10. Founders = D03.
Keepers' own hands = D02 (Vaun), D05 (Mara), D06 (Sella), D07 (Orin),
D08 (Brann), D09 (Iss). Cast-out era (effaced, near-Archivist) = D11.
Keeper NPC = D12. All three registers + all six keepers present. ✔

---

## 2. INTERLINK MAP (adjacency)

Directed edges = `links_to` declared in each file's front-matter, annotated with
the nature of the link. `↔` marks a confirmed two-way relationship.

| From | → To | Relationship |
|---|---|---|
| D01 `the-record-opens` | D02 `counted-them-in-the-dark` | The Offering-kept claim D01 makes is contradicted by Vaun's ledger. |
| D01 `the-record-opens` | D04 `observed-warned-left-at-threshold` | Same hand, later & colder; the count → the consequence. |
| D02 `counted-them-in-the-dark` | D01 `the-record-opens` | ↔ contradiction (ledger vs record). |
| D02 `counted-them-in-the-dark` | D03 `learn-them-as-we-learned-them` | Founder origin of the customs Vaun "learned, did not make." |
| D03 `learn-them-as-we-learned-them` | *(Rosetta root)* | Teaches the script; per `found-documents.md` opens D02, D05, D06, D09. (Its own front-matter declares no out-links — it is the seed every keeper hand is written *in*, not an out-pointer. See TODO-1.) |
| D04 `observed-warned-left-at-threshold` | D07 `i-thought-it-small` | Finishes Orin's broken carving. |
| D04 `observed-warned-left-at-threshold` | D01 `the-record-opens` | ↔ same hand, the count grown cold. |
| D05 `page-line-word` | D12 `bring-the-thing-only-you-can-give` | Mara's missing tool is answered by the Keeper's summons. |
| D05 `page-line-word` | D03 `learn-them-as-we-learned-them` | Book-cipher needs the taught script. |
| D06 `what-the-surface-keeps` | D11 `the-seventh-not-kept` | Margin bearing hands off to the cast-out map-note. |
| D06 `what-the-surface-keeps` | *(no second link in front-matter)* | Brief expected → D03; file omits it. See TODO-2. |
| D07 `i-thought-it-small` | D04 `observed-warned-left-at-threshold` | ↔ "threshold" margin → the record that completes him. |
| D07 `i-thought-it-small` | D03 `learn-them-as-we-learned-them` | "learned… from the land, not made." |
| D08 `do-not-close-your-eyes-here` | D11 `the-seventh-not-kept` | Doused-vs-kept fire, same lesson two sides. |
| D09 `the-ways-are-a-wall` | D10 `no-wall-was-ever-built-here` | ↔ The lie answered by the catch. |
| D09 `the-ways-are-a-wall` | D11 `the-seventh-not-kept` | Iss's wrong fragment dead-ends at the Seventh's ruin. |
| D10 `no-wall-was-ever-built-here` | D09 `the-ways-are-a-wall` | ↔ Contradicts line for line; forces the re-walk. |
| D10 `no-wall-was-ever-built-here` | D12 `bring-the-thing-only-you-can-give` | The true coordinate it yields points to the rite. |
| D11 `the-seventh-not-kept` | D06 / D09 / D08 | Pointed-here (D06), dead-ended-here (D09), fire-lesson (D08). |
| D12 `bring-the-thing-only-you-can-give` | D05 / D01 / D04 | The tool answered (D05); the count now closed (D01); kept/left now the group's (D04). |

**Confirmed reciprocal pairs:** D01↔D02, D01↔D04, D07↔D04, D09↔D10.
**Hub nodes:** D03 (Rosetta — every keeper hand depends on it), D11 (Seventh
convergence — three inbound), D12 (rite convergence — three inbound).

---

## 3. REVEAL ORDER (which fact surfaces where, via which docs)

Per spine §3. ✔ = ≥2 document paths (web rule satisfied). Discord-oracle paths
noted where the spine assigns them in addition to documents.

| Fact | Movement | Type | Document path(s) | Paths |
|---|---|---|---|---|
| F1 — record keeps a list of the living by name | M1 | REVEAL | D01 | + Discord "the records have begun" (spine). 1 doc / 2 total ✔ |
| F2 — graded by laws no one told them | M1 | REVEAL | D01 ("the watching has already watched"); D03 ("grades the keeping of those who do not know they are kept") | 2 docs ✔ |
| F3 — there were keepers before this group | M1→M2 | REVEAL | D03 (rune ring); D01 ("six are named… a seventh mark"); D11 (the count) | 3 docs ✔ |
| F4 — customs learned *from the land*, not invented | M2 | FORESHADOW | D03 (founder fragment); D02 (Vaun "the land counts first"); D07 ("from the land, they said, not made") | 3 docs ✔ |
| F5 — each keeper's fate matches the custom they broke | M2 | REVEAL | D02 (Vaun/Offering); D07 (Orin/Bow); D05 (Mara/map-never-tool); D06 (Sella/Bow at far water) | 4 docs ✔ |
| F6 — a keeper named, warned, left at the threshold | M2→M4 | FORESHADOW | D07 (seed, breaks at "i —"); D04 (the ladder, completes it) | 2 docs ✔ |
| F7 — Iss: the ways are a *wall* (planted lie) | M2 | FORESHADOW/lie | D09 (the doctrine) | + D11 quotes it to refute. Banner-glyph wall (spine). 1 doc primary ✔ via refutations |
| F8 — Iss lied; the ways are not a wall | M2 seed→M4 | REVEAL | D10 (the catch, line-for-line); D11 ("a wall does not choose") | 2 docs ✔ |
| F9 — first hauntings were a keeper's fate re-enacted | M4 | REVEAL | **Not authored as a standalone fragment.** Implied by D04+D07 (Orin's biography) and the Iss re-walk (D09→D10). See TODO-3. | gap — see §6 |
| F10 — acceptance is a choice the land makes; it can refuse | M3→M5 | FORESHADOW | D11 (the Seventh refused); D06 (margin: "left… the land made it"); D12 ("the door swings both ways") | 3 docs ✔ |
| F11 — one fire never went out | M3 | FORESHADOW | D08 (the home-fire); D11 (inverse: the one let die); D01 ("the light was kept"); D12 (the undercroft light) | 4 docs ✔ |
| F12 — "the kept ones did not depart. they were kept." | M3→M4 | FORESHADOW (strongest) | D08 ("the same word… does not change"); D04 (foot-margin); D10 ("read the abandonment-stone"); D12 ("they did not depart… they were kept") | 4 docs ✔ |
| F13 — the rite needs a personal token per keeper | M3→M5 | REVEAL | D05 ("bring a piece you cannot read your way out of"); D12 (six tokens, the altar slots) | 2 docs ✔ |
| F14 — the record does not stop; it *receives*/keeps you | M5 | FORESHADOW→REVEAL | D01 (buried "it does not close at the rite… it [...] you"); D12 ("it receives… it would keep you") | 2 docs ✔ |
| F15 — to be accepted is to become part of the watching | M5 | SEALED (felt, never stated) | Never stated. Carried by D03 ("they were holding you"), D04, D08, D11, D12, D01, plus the world flip. No sentence names it. ✔ (see §5 audit) |

**Movement census of surfacing docs:**
M1 → D01, D03 ✔ · M2 → D02, D04, D05, D06, D07, D09 ✔ · M3 → D06, D08, D11 ✔ ·
M4 → D04, D10 ✔ · M5 → D12 ✔. Every Movement has ≥1 surfacing document.

**Two-path audit:** every spine fact except F7 and F9 has ≥2 *document* homes.
F7 is single-doc by design (one planted lie) but is reinforced by two refuting
docs. F9 has **no document home at all** — flagged below.

---

## 4. CLUE-BEARING DOCS (the Discord answer-oracle gate)

The four `clue_bearing: true` fragments and exactly what the oracle should accept
and unlock. Answers are quoted from each file's front-matter / body.

| Clue id | Cipher | Correct answer (oracle gate) | Unlocks next |
|---|---|---|---|
| D03 `learn-them-as-we-learned-them` | rune-ring Rosetta | Assemble the ring sunwise from the topmost mark in order **Bow, Offering, Kept-Light, Deep-Line, Ward, Covering** (server-icon ring). | The master script — every later keeper-stone (D02, D05, D06, D09, and the literacy-gated D04/D10/D11) becomes legible. The literacy spine of the whole arc. |
| D05 `page-line-word` | book-cipher (page/line/word ×6) | The six words assemble to **"DESCEND AND BOW AT THE UNBROKEN LIGHT."** | The instruction to go to the undercroft / unbroken light and *perform* (bow as one), and the demand for a personal token each (F13) → routes toward D12 / the rite. Oracle should accept the assembled sentence, not the raw triples. |
| D06 `what-the-surface-keeps` | Atbash / reflection + margin bearing | Read faced to the water; then **"south, by the far water where she did not come back"** to the doused shrine — handing off to `the-seventh-not-kept`. | The Seventh Stone expedition (D11). Oracle gates on the bearing/handoff, not on Sella's prose. |
| D09 `the-ways-are-a-wall` | Vigenère, key = a name | The key is **ISS** (his own name); applied against the other stones it decodes to **"the one who turned away."** | Plants the doubt that overturns F7 → routes to D10 (the catch) and exposes the dead-shrine fragment as false. Oracle confirms the *name-as-key*, never the doctrine. |

> Oracle discipline (from spine §4 / found-documents): for D09 the oracle
> confirms the **key**, not the comforting doctrine; for D03 it confirms the
> **ring solution**; for D05 it confirms the **assembled sentence** ("do the
> thing it tells you"), not a written-out answer.

---

## 5. THREAD AUDIT (Liar + Seventh Stone)

**Liar thread (Iss).** ✔ Seeded and paid off.
- Seed: **D09** `the-ways-are-a-wall` — warmest voice, the wall doctrine (F7),
  Vigenère key = his own name, sends to the dead shrine.
- Doubt: planted in D09's own margins ("we checked the lock… the one who turned
  away") and the foot line ("ask first what a wall is *for*").
- Catch / payoff: **D10** `no-wall-was-ever-built-here` — Stone-after behind a
  clue falsely marked "kept · solved," contradicts him line for line, flips warm
  → cold, yields the true coordinate. The inverse-of-the-truth discipline holds:
  D10 says only that the wall is a lie and explicitly refuses to say what the
  ways ARE ("then what are the ways for. the stone does not say"). ✔ Seeded +
  payoff doc both present.

**Seventh Stone thread.** ✔ Seeded and paid off.
- Seed: **D06** `what-the-surface-keeps` ("count again… the last marker is not
  the last") + margin bearing; reinforced by D01 ("a seventh mark the record will
  not [...]") and D08 (the cast-out one).
- Payoff: **D11** `the-seventh-not-kept` — the cast-out keeper, shrine where the
  light was let die, "a thing that can say no… is not a wall… it is something
  with a will" (F10), "it has not yet said which we are." ✔
- Distinct-from-Iss ambiguity preserved: D11 explicitly separates them ("not left
  at the threshold to be argued over, as orin was. before the threshold… no stone.
  no name") while keeping the thin wonder. Iss *came back*; the Seventh did not.
  The induction twist is untouched. ✔

**Sealed F15 discipline.** ✔ No fragment states the induction twist in plain
words. The nearest approaches all stop short: D03 "they were holding you" (the
ways, not the watching), D12 "we would keep you, if you would keep the ways" and
the withheld "*we* in my mouth that i hold back," D01's omitted word ("i will
leave the word out"). The twist is delivered by accumulation, never by a
sentence. ✔

---

## 6. CONSISTENCY CHECK

Problems found (none block play; all are authoring/wiring TODOs):

- **TODO-1 — D03 has no out-links in its front-matter.** `found-documents.md`
  declares D03 OPENS D02, D05, D06, D09 (it is the Rosetta root). The file
  `learn-them-as-we-learned-them.md` lists no `links_to`. This is defensible
  (it's the script all later hands are written *in*, an inbound dependency rather
  than an out-pointer — D02/D05/D07 all link back *to* it), but the asymmetry
  means a naive graph walk won't surface D03's downstream reach. Recommend adding
  the four reciprocal `links_to`, or documenting the inbound-only convention.

- **TODO-2 — D06 is missing its second declared link.** The brief says D06 links
  → D11 **and** D03. The file only declares → `the-seventh-not-kept`. The D03
  dependency (Atbash still needs the taught script) is real but unwired. Add
  `learn-them-as-we-learned-them` to D06's `links_to`. Minor.

- **TODO-3 — FACT 9 has no document home (the only true gap).** Spine §3 F9
  ("the first hauntings were a specific keeper's fate re-enacted at the group,"
  M4 REVEAL) is not authored as, or explicitly carried by, any of the 12
  fragments. It is *implicit* in D04+D07 (Orin's biography) and in the Iss
  re-walk, and `found-documents.md` assigns its delivery to "a keeper NPC (or
  exposed Iss) connecting a Movement-I beat to a named keeper's biography" — i.e.
  to dialogue, not a found document. **This is fine if and only if** the M4
  dialogue/Keeper-NPC content actually makes the connection explicit. As authored
  in the document corpus alone, F9 has zero document paths (vs. the spine's
  two-door web rule). Action: either (a) author one fragment that names a
  Movement-I haunting and ties it to a keeper's fate, or (b) confirm the M4
  dialogue tree carries F9 and note here that F9 is intentionally
  dialogue-delivered. Recommend (a) for web-rule parity.

- **No contradictions found.** Spot-checks all clean:
  - D01 claims the Offering "was kept faithfully" in the first winters; D02
    proves Vaun did not. This is the *intended* ledger-vs-record contradiction
    (F5 door), not an error. ✔
  - Keeper count is consistent everywhere: D11 enumerates "vaun, mara, sella,
    orin, brann, iss" = six, then "a seventh," matching D01's "six… and a
    seventh mark" and the spine §1 order. ✔
  - Keeper voices are all honored (Vaun ledger-only, Mara page-refs-only, Sella
    reflection-only, Orin crouch-only, Brann night-only, Iss plain-and-false).
    No voice break. ✔
  - F15 is never stated in plain words in any fragment (re-verified line by
    line). ✔
  - Iss/Seventh kept distinct per spine §5 default. ✔

- **No orphaned references.** Every `links_to` target resolves to an existing
  file id. All 19 declared edges point at real documents; no dangling ids.
  (D03's *missing* out-links are an omission, TODO-1 — not a dangling pointer.)

- **No movement gaps.** Every Movement M1–M5 has ≥1 surfacing document
  (M1: D01,D03 · M2: D02,D04,D05,D06,D07,D09 · M3: D06,D08,D11 · M4: D04,D10 ·
  M5: D12).

- **Note (not a defect) — F7 is intentionally single-doc.** Only D09 carries the
  planted lie, by design (one liar, one lie). It is doubly refuted (D10, D11), so
  the *refutation* is web-safe even though the *seed* is single-source.

---

### CONSISTENCY-CHECK SUMMARY

**Not clean — 1 real gap + 2 minor wiring TODOs; no contradictions, no orphans,
no movement gaps.**

- **TODO-3 (real):** FACT 9 ("hauntings = a keeper's fate re-enacted") has **no
  document home** — it relies entirely on M4 dialogue/Keeper-NPC. Author one
  fragment for it, or confirm-and-record that it is intentionally
  dialogue-delivered, to satisfy the spine's two-door web rule.
- **TODO-1 (minor):** D03 (`learn-them-as-we-learned-them`) declares no
  `links_to`; the brief makes it the Rosetta root of D02/D05/D06/D09. Add the
  reciprocal out-links or document the inbound-only convention.
- **TODO-2 (minor):** D06 (`what-the-surface-keeps`) is missing its declared
  second link to D03; add it.

Everything else is clean: both the **Liar thread** (D09 seed → D10 catch) and
the **Seventh Stone thread** (D06 → D11 payoff) are seeded and have payoff
documents; the sealed FACT 15 is never stated in words; keeper count and voices
are internally consistent; every link resolves; every Movement is covered.
