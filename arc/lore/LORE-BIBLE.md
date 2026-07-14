# The Observance — LORE BIBLE (V4 archive)

> **SUPERSEDED V4 ARCHIVE — NOT AN OPERATOR AUTHORITY.** Use `arc/WORLD-BIBLE.md`, `design/ARG-V5-MASTER-PLAN.md`, and `design/ARG-V5-NODE-MANIFEST.csv`.

> Operator-facing index. Maps the 12 authored fragments in
> `arc/lore/documents/` against `canon-spine.md` and the `found-documents.md`
> brief. Use this to answer "where does fact X live?", "what does clue Y
> unlock?", and "is the corpus internally consistent?". The spine is law; this
> file only indexes and audits it.
>
> Sources of truth: `canon-spine.md` (§1 cast, §2 timeline, §3 the 15 facts,
> §3b the synthesis facts 16/10b/2b/7b/13b/**17**, §4 Liar thread, §5 Seventh Stone
> thread, §6 hard rules, §7 invariants 11–**20**, §8 namespace anchors incl. §8.4
> the four fates + two codicils and §8.5 the `UNKEPT` meta-acrostic) and
> `found-documents.md` (the D01–D12 brief). The 12 files on disk are the
> authored realization of D01–D12.
>
> **Synthesis layer (`WEB-MASTER` + `INTEGRATION-V2`).** The web added five
> child-facts (§3b) and a set of new corpus fragments owned by the LORE lane
> (`BUILD-MANIFEST §1`). Those new fragments are indexed here as the **planned
> corpus extension** (status NEW-D## below) so the operator can answer "where
> does FACT 16 / 7b / 9 live?" before they land on disk. The 12 D01–D12 files
> remain authoritative; the NEW-D## rows are the synthesis additions that close
> the §6 TODOs.

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

## 1b. SYNTHESIS-FRAGMENT INDEX (the planned corpus extension)

The new fragments the synthesis web requires (`BUILD-MANIFEST §1`, `INTEGRATION-V2`).
Status NEW = to be authored by the LORE lane; some are *edits* (E) to existing files.
The carries-FACT column uses the §3b namespace. De-slop exemplars live in
`INTEGRATION-V2` per idea; the register normalizer (`registerDisciplineSelfTest`)
gates every Watcher/keeper line.

| New id | Title / nature | Kind | Author / register | Mvmt | Carries | Notes |
|---|---|---|---|---|---|---|
| NEW-D13 | `the-fire-they-let-out` | map-note (effaced hand) | cast-out era / near-Archivist | 3→4 | **F10b** | The Seventh cause-fragment; correlates with D11; the `restore`/`erase` fork (`seventh_choice`). Distinct *place* from D11's surface (the hearth-deep, `the_unwriting`). |
| NEW-D14 | `the-eighth-way` | forged ordinance | a later keeper (anonymous lie) | 2→4 | **F7b** | The Covering of the Hands; substitution signature → "cover/hide one's own"; counted in **founders' ring** (§8.1). No custom key, no listener (INV-17). |
| NEW-D15 | grave founder-margin | margin-note | the founding line | 2→5 | **F13b** | *"we cut the names before the keeping… the stone is ready before the keeper is."* Date == single Accepting instant (§8.2). |
| NEW-D16 | place/word-filing fragment | journal fragment (water-damaged) | the Archivist / the record | 2→V | **F16 + F17** | **One artifact, two clauses** (`WEB-MASTER §0.5`): *"the list is not only of names. against each name, a ground; against each ground, what was said over it."* The place clause carries F16 (the `name-where-never-been` plant); the word clause carries F17 (the Ear's P2 plant). Obeys INV-14/INV-16. NOT two competing fragments on the Archivist register. |
| NEW-D17 | record-elsewhere fragment | founder line | the founding line | 1→click | (texture on F1) | *"the record is kept in more than one place, against the loss of the first."* The Record-website plant; not a numbered fact. |
| NEW-D18 | difficulty fragment | letter (bookCipher) | Mara, the Reader | 1→5 | **F2b** | *"the record keeps a closer count of the quick… i will not say what i call it."* Honors Mara's page-ref voice. |
| NEW-D19 | the Hold-Book (down-count + keeper-record) | ledger→record, growing | Brann's docket + the Archivist | 1→5 | **F9**, F12/F14, F1 note | **The unified Hold-Book** (`record-writes-you-in` + `counting-base-journal`); one book, one anchor (`WEB-MASTER §4`). **This is FACT 9's document home — closes TODO-3.** See §3/§6. |
| NEW-D19b | `base-docket-reread` | record (M4 re-read) | the Archivist / the record | 4 | F9, F12 | The down-count re-reads as the muster of present hands; gated behind the Vigenère node (`iss_caught`). The de-slopped line: *"the muster is read. the count was never of the dark. it was of the hands. the hands are almost in."* |

**Edits to existing files (synthesis):**

| File | E | Carries | Change |
|---|---|---|---|
| D08 `do-not-close-your-eyes-here` | E | (F9 carrier) | One marginal pointer for the offline-skin apparition (kept ambiguous) + the M1 offline-player report text (*"brann… not here to see it noted"*) — a second door onto F9 (the worn night-walker). |
| D09 `the-ways-are-a-wall` | E | (prophet wall) | One later-hand margin line foreshadowing the catch — *"read who carved it, after"* — the prophet-wall textual source; hidden columnar name = Iss. |
| D10 `no-wall-was-ever-built-here` | E | F7b | One record line tying the forged eighth into the M4 correction; 1:1 map to the prophet-wall rungs. |

> Register/cast coverage after the extension: Mara gains NEW-D18; Brann gains the
> Hold-Book docket (NEW-D19) + the offline report (D08 edit); the Archivist gains
> NEW-D16, NEW-D19/19b; the founders gain NEW-D15, NEW-D17; the cast-out era gains
> NEW-D13. No keeper voice is broken (§6 re-checked below).

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

**The §3b synthesis facts (new doors):**

| Fact | Movement | Type | Document path(s) | Paths |
|---|---|---|---|---|
| F16 — record files the living by *place* | M2→M4 | FORESHADOW→REVEAL | NEW-D16 (place-filing fragment); the `kept here before you` teaching-stone line; the `name-where-never-been` carve (mechanical) | 2 docs + 1 mechanic ✔ |
| F10b — the land refused a keeper who broke *nothing* | M3 | SEALED sub-fact | NEW-D13 (`the-fire-they-let-out`, correlates with D11); D11 (the cast-out count) | 2 docs ✔ |
| F2b — the land's grip is not fixed | M2 | REVEAL | NEW-D18 (Mara difficulty fragment); the felt drip-cool/patience (mechanical, the difficulty engine) | 1 doc + 1 mechanic ✔ |
| F7b — a forged eighth observance; the land never enforced it | M2→M4 | FORESHADOW→REVEAL | NEW-D14 (`the-eighth-way`, the forged ordinance); D10 edit (the M4 record correction) | 2 docs ✔ |
| F13b — the stone is cut *before* the keeper is kept | M2 | FORESHADOW | NEW-D15 (grave founder-margin); the future-dated grave carve (mechanical) | 1 doc + 1 mechanic ✔ |
| F17 — the record files what is *said* of the ways | M2→V | FORESHADOW→REVEAL | NEW-D16 (the Archivist fragment's **second clause**, *"what was said over it"* — same artifact as F16); the Ear's keeper-whisper (`SpatialVoiceBeat` / pack-sound fallback, mechanical, P3) | 1 doc (P2) + 1 mechanic (P3) ✔ |

> Every §3b fact has ≥2 doors. Where a door is a **mechanic** rather than a
> document (F16 carve, F2b drip-cool, F13b grave), the mechanic is a first-class
> path — the arg-craft critique (F3d) demanded *mechanical* plants, not only
> narrative ones. `WEB-MASTER §9` is the authoritative plant→payoff ledger.

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

- **TODO-1 — RESOLVED (inbound-only convention recorded).** D03
  `learn-them-as-we-learned-them` is the **Rosetta root**: it is the script every
  later keeper hand is written *in*, not an out-pointer. The inbound dependency is
  the correct topology — D02/D05/D06/D07 link back *to* it, and `found-documents.md`'s
  "D03 OPENS …" is a literacy relation, not a `links_to` edge. **Canon convention:
  the Rosetta root declares no `links_to`; downstream reach is read from the inbound
  edges.** A graph walk that wants D03's downstream set inverts the inbound edges. No
  file edit required. (Recorded so a future audit does not re-flag the asymmetry.)

- **TODO-2 — RESOLVED (dependency recorded; one-line edit deferred to LORE lane).**
  D06 `what-the-surface-keeps` depends on D03 (Atbash still needs the taught script);
  the dependency is real and now **recorded** as an inbound edge to D03 under the
  TODO-1 convention (D06 → D03 literacy). The optional reciprocal `links_to` add on
  D06's front-matter is a cosmetic graph-completeness edit, batched into the LORE
  lane's D06 edit pass (alongside the offline-skin/D08 work) — not a blocker, not a
  contradiction.

- **TODO-3 — RESOLVED (FACT 9 now has a document home).** The synthesis layer closes
  the only true gap. FACT 9 ("the first hauntings were a specific keeper's fate
  re-enacted at the group," M4 REVEAL) now has **two document doors plus its original
  dialogue door** (`INTEGRATION-V2 A3` makes the unified Hold-Book FACT 9's home):
  - **NEW-D19 / NEW-D19b — the Hold-Book.** The down-count and the keeper-record are
    the same book (`WEB-MASTER §4`). At the M4 re-read (gated on `iss_caught`) the
    living-habit pages, already moved under keeper headings in M3, re-read as the
    group's own conduct *enrolled into a named keeper's column* — the haunting given
    a biography, in writing, in the same book as the keeper it rhymes with. This is
    the direct document carrier of F9.
  - **D08 edit (the offline-player report) → the offline-skin apparition.** The M1
    report naming a logged-off friend pays off in M3→M4 as the land *wearing* him to
    the shape of a keeper (FACT 9 spoken once, human-approved). A second door.
  - **The Iss re-walk (D09→D10), as before.** The catch re-reads the warm dialogue
    cold — the haunting's biography surfacing through the liar.
  So F9 now satisfies the two-door web rule in the document corpus (NEW-D19 + D08
  report), no longer dialogue-only. The keeper-enrolment tally is a **neutral
  colorant** (§6.3 / INV-11): it never elects, never gates, never spotlights a player
  (INV-16). Precision floor: a flat player is enrolled to **no one**.

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

### SYNTHESIS-LAYER CONSISTENCY (the §3b facts + new fragments)

Re-checked against `WEB-MASTER` + `INTEGRATION-V2` + spine §7/§8:

- **The two "sixes" do not collide (§8.1).** The `UNKEPT` meta-acrostic uses
  **fall-order** (the maker's-mark glyphs); the forged-eighth count contradiction
  (F7b / NEW-D14) uses the **founders' ring**. Recorded so no fragment conflates them.
- **The two "eighths" do not collide (§8.3 / `WEB-MASTER §3.3`).** The Unlit Deep is a
  real tracked group latch (deep + black moon + flame); the Covering (F7b) is a forged
  un-tracked law about a count. Different frames, places, times. INV-17 makes the
  boundary canon.
- **The Seventh's two places stay distinct (§5 / §8 anchor).** D11 = the surface
  cold-hearth (Iss's dead-shrine herring overlaps it); NEW-D13 = the hearth-deep
  (`the_unwriting`), sealed until `iss_caught` + `seventh_named`. No flattening fragment.
- **The single Accepting instant (§8.2)** is shared by NEW-D15 (grave date), the Record
  website timestamp, and the summons `not_before`. No fragment carves a different date.
- **Keeper voices hold across the extension (§6.8).** Mara's difficulty fragment
  (NEW-D18) stays in page-refs; the Hold-Book docket (NEW-D19) is Brann's tally hand +
  the Archivist's flat rows, both already-canon registers; the grammatical fingerprints
  (`WEB-MASTER §6`: Vaun accumulates, Brann doubles, Mara cites, etc.) are LORE-lane
  binding. No voice break introduced.
- **Sealed F15 untouched.** No §3b fact states induction; F10b deepens "the land can
  refuse" toward the cost without naming it; F7b/F16/F13b/F2b are all FACT-children that
  point, never blurt. The de-slop exemplars (`INTEGRATION-V2`, the slop A1–A4/B1–B4
  fixes) are applied at authoring — no warming, no chiasmus, no object-memory, no
  self-justifying clause.
- **Mechanics obey the invariants.** Name-carve & grave dates are *read*, never typed
  (INV-14); the difficulty engine never touches Whispers (INV-15); the keeper-enrolment
  tally never names a player (INV-11/16); no fork gates a spine puzzle (INV-12).
- **FACT 17 shares NEW-D16, never competes with F16 (§0.5).** The place clause (F16) and
  the word clause (F17) are the **same water-damaged Archivist fragment**, authored once.
  The Ear (F17's second, P3 door) defers to `apparitionClaim` (INV-18) and its
  `voice_watchlist` derives from the keeper roster + bound plaintexts (never a hand-kept
  copy), so a re-tuned plaintext cannot silently desync the spoken-word axis. F17 plants at
  P2 and degrades to a pack-sound whisper if the voice layer never installs — no single-
  thread failure point.
- **The single-arbiter slot holds across every apparition lane (INV-18).** The offline-skin
  apparition, the `name-where-never-been` carve, the six Keeper-NPC apparitions, and the
  Ear all defer to one `apparitionClaim` — at most one ambient figure/whisper per window.
  FACT-9 reaches a given player through **one surface per window**, never three spotlights
  at once (the keeper-NPC precision gate + the slot). No collision with §6.5 "warmth under
  dread": the world stays sparing with its appearings.
- **The climax never punishes an absent member (INV-19).** `effectiveQuorum =
  min(configQuorum, activeRosterSize)` over active players only — the same active-only law
  as INV-11/§6.3, applied to the Accepting. A group that shows up is never barred because a
  friend stayed logged off.
- **Every earned door visibly moves (INV-20).** Each solved-clue unlock names a registered
  world-change with a contract-matching payload, build-time enforced — no dead or malformed
  reveal. Keeps the spine's "solve a clue, the world opens" promise honest.
- **The four fates name the group, never a player (§8.4).** `kept|cast_out|divided|refusers`
  are the mechanical expression of FACT 10 (demoted from a numbered fact), composed into a
  bounded close (neutral + ≤1 tint + ≤1 codicil). `divided` splits the floor by geometry,
  never by player (INV-16); `refusers` reads only from positive defiance, never absence
  (precision); the bond/Whisper tally is excluded (§6.3). The `inheritors` codicil is the
  Seventh `restore` act (one flag), planting FACT 14 within the arc.
- **The `UNKEPT` meta-acrostic stays in the framing layer (§8.5).** The six maker's-mark
  glyphs live in carved framing, never any bound plaintext — the X1 round-trip guard is
  untouched. Read in **fall-order** (§8.1), self-correcting in ring-order; gates nothing.
- **Dynamic difficulty is diegetic (F2b / INV-15).** The land's grip is the difficulty
  engine; it cools the drip and the register but never the Whisper backstop. Felt at M2,
  re-read at V as the `inheritors`-adjacent difficulty backward-read (Mara's "closer count
  of the quick"). Group-scalar only — never a per-player rank, never a callout (§6.3).

---

### CONSISTENCY-CHECK SUMMARY

**Clean — all 3 prior TODOs resolved; no contradictions, no orphans, no movement
gaps; synthesis layer threaded.**

- **TODO-3 (was the only real gap) — RESOLVED.** FACT 9 now has a document home: the
  unified Hold-Book (NEW-D19/19b, `INTEGRATION-V2 A3`) carries it directly at the M4
  re-read, with the offline-player report (D08 edit) as a second door and the Iss
  re-walk (D09→D10) as the third. Two-door web rule satisfied in the corpus.
- **TODO-1 — RESOLVED.** D03's no-`links_to` is recorded as the **Rosetta-root
  inbound-only convention**; downstream reach is read from inbound edges. No edit needed.
- **TODO-2 — RESOLVED.** D06→D03 literacy dependency recorded under the TODO-1
  convention; the optional cosmetic `links_to` add is batched into the LORE-lane D06
  edit, non-blocking.

Both original threads remain seeded-with-payoff (Liar D09→D10; Seventh D06→D11, now
deepened by NEW-D13/F10b); the sealed FACT 15 is never stated; keeper count and voices
are consistent across the extension; the §3b facts each have ≥2 doors; the two "sixes",
the two "eighths", and the Seventh's two places are all kept distinct.

**Backlog layer threaded (Batch-2).** FACT 17 (the word-axis) shares NEW-D16 with FACT 16
as one artifact, two clauses; INV-18 (single-arbiter slot), INV-19 (Accepting active-only
quorum), and INV-20 (unlock step↔payload) are in spine §7; the four fates + two codicils
(§8.4) and the `UNKEPT` meta-acrostic (§8.5) have canon homes. The third filing axis
(name / place / word) is the same record with three columns — no contradiction with FACT 1.
