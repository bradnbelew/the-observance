---
id: some-laws-are-lies
title: Some Laws Are Lies — the unreliable Watcher / a fabricated custom
kind: design/idea-treatment
status: FROZEN-IN-CANON (treatment reconciled to the synthesized spine; FACT 7b, INV-17, the-eighth-way.md, the ring's `covering` glyph, and the no-`the_covering` guard are all live downstream — this file is now the design rationale of record, not a proposal)
verdict: KEEP-SCALED (kept; "inert eighth custom key / null-enforcement flag" CUT; "or worse" inverted toll CUT)
priority: P1 (arc-spine) — NOT P0 vertical slice
movement: seed M2, doubt M3, catch M4, re-read M4→M5
reconciled_against:
  - arc/lore/canon-spine.md §3b FACT 7b           # FROZEN, verbatim; this idea's fact, now canon
  - arc/lore/canon-spine.md §7 INV-17             # FROZEN; the closed-set invariant this idea proposed
  - arc/lore/canon-spine.md §8.1 / §8.3           # the two sixes; ring-order vs fall-order; the_unlit_deep
  - arc/lore/documents/the-eighth-way.md          # the forged ordinance — AUTHORED, not proposed
  - arc/lore/documents/learn-them-as-we-learned-them.md  # ring froze as Bow,Offering,Kept-Light,Deep-Line,Ward,Covering
  - plugin/.../signal/listener/CustomComplianceListener.java §INV-17 javadoc  # the no-`the_covering` guard, in code
  - design/cipher-web.md (clue_substitution_rosetta_ring / clue_substitution_orin_small)  # the ring solve, ends "...ward covering"
grounded_in:
  - design/content/customs-punishment.md            # the five-stage toll loop every way obeys; INV-8
  - arc/lore/canon-spine.md                          # §1 keepers, §3 FACT 7/8, §4 the Liar thread, §6 hard rules
  - arc/lore/documents/the-ways-are-a-wall.md        # Iss's planted lie (the macro precedent for this idea)
  - arc/lore/documents/no-wall-was-ever-built-here.md# the record's flat correction (the catch shape)
  - arc/lore/documents/learn-them-as-we-learned-them.md  # the six-mark ring; "six marks" is the anchor seed
  - plugin/.../signal/listener/CustomComplianceListener.java  # the bow/offering compliance path
  - plugin/.../signal/listener/DarkHoursListener.java         # the bed-enter violation path
  - discord/src/forge/canon.ts                       # CUSTOM_KEYS (seven), KEEPERS, THREADS, the build guards
  - discord/src/forge/ciphers.ts                     # the 11 built ciphers (caesar … morse)
  - FLOW.md                                          # act gates, soft-pressure, whisper backstop
custom_keys_touched: [the_bow, the_offering, the_unspoken, the_deep_line]
ciphers_used: [substitution, vigenere, bookCipher]
new_doc: arc/lore/documents/the-eighth-way.md
---

# Some Laws Are Lies — the unreliable Watcher / a fabricated custom

> The pitch: a prior keeper fabricated a "custom" to control the others. The puzzle is
> telling which laws the land actually enforces from a dead hand's manipulation. Obeying
> a false law does *nothing* — and the *nothing* is the clue. Turns "obey the rules" into
> "interrogate the rules."

---

## 0a. RECONCILIATION — what synthesis froze (read first; supersedes any "proposed" wording below)

This treatment predates the synthesis pass. Everything it proposed is now **live canon**, with
five sharpenings the body text below should be read through:

1. **FACT 7b is frozen** (`canon-spine.md §3b`), counted in the **founders' ring (six marks),
   never fall-order** (`§8.1`). The lie is "the ring of six is really seven-and-a-half / eight,"
   not a re-ordering of who fell.
2. **INV-17 is frozen** (`§7`): the engine enforces exactly the seven `CUSTOM_KEYS` **plus one
   group latch** — so the proof of forgery is the reliable *absence* of a toll loop. The "null /
   inverted enforcement config flag" from the original LIKELY-MAPPING is **dead by invariant**;
   `CustomComplianceListener.java` now carries a javadoc forbidding any future hand from wiring
   `the_covering` as a tracked custom (doing so "destroys the falsification — a canon defect").
3. **`covering` is now a planted glyph inside the master key itself.** The ring froze as
   `Bow, Offering, Kept-Light, Deep-Line, Ward, Covering` (`learn-them-as-we-learned-them.md`;
   rosetta puzzle `clue_substitution_rosetta_ring`, accepted answer "…ward covering"). So the
   group **learns to read every later stone using a ring whose sixth mark is "covering"** — long
   before the forgery is exposed. This is a *stronger* plant than the draft's "seven and a half"
   barb: the forger keyed his confession (*cover one's own*) into the very alphabet the group
   trusts. **`covering` ≠ `the Covering of the Hands`:** the ring's sixth founding mark is a real
   founding way (a true "covering" — guarding/sealing, sibling to the Ward); the **forged eighth**
   reuses that honest word as cover. The collision is intentional and is the cipher payoff.
4. **There are now TWO "eighths," and they must never be conflated** (`§8.3`, `WEB-MASTER §3.3`):
   - the **forged Covering of the Hands** (THIS idea, FACT 7b) — a *document, not a custom*; the
     dead hand's lie about a *count*; un-tracked by construction.
   - the **`the_unlit_deep`** group latch (a *real* synthesis custom, its own `UnlitDeepListener`)
     — below the Line + on the black moon + a flame lit → borrowed warmth withdraws for all.
   They do not collide because one is forged-about-a-count and one is real-about-restraint. Any
   prose that lets the group read the forged Covering as the real latch (or vice-versa) is a defect.
5. **The "or worse" inverted toll (§1.4 below) is CUT** by INV-17 + Risk 2: an inert law cannot be
   given teeth without re-teaching it as real. The single optional concealment-reads-as-hiding flavor
   is retained only as a *pre-existing the_unspoken signal*, rare and deniable, never a Covering toll.

---

## 0. THE CORE DESIGN PROBLEM (read first — it decides the whole shape)

The seven ways are the spine of the build. The toll loop in `customs-punishment.md` is
deliberately built so that **every real way is enforced identically**: transgress →
deniable toll (stage A) → named lapse (stage B) → decipher off a surface → honor → relief.
That uniformity is the engine's load-bearing wall. If a "false eighth law" looked and
behaved like the seven up to the moment of honoring, the group would have **no fair signal**
to distinguish it — they would have to honor it, wait, observe nothing, and *infer absence*,
which is the weakest possible read on camera ("did it not work, or did we do it wrong?").

So the idea cannot be "an eighth way that is secretly inert." That is an orphan: a mechanic
whose entire content is the *lack* of the toll loop, with no positive teaching surface and a
near-unfalsifiable test. **The keepable version inverts the framing:** the false law is not a
new *way* the player must discover the engine ignores. It is a **forged document that claims
the seven are eight** — and the proof of the forgery is that the land's own enforcement only
ever measures seven. The unreliable narrator is **a dead keeper's text, never the Watcher**.
The Watcher (the record) stays true; that is its entire character (canon §6.4, "always true").
What lies is a *keeper's hand* — exactly as Iss already lies about the ways being a wall.

This makes the idea a **sibling of the Liar thread, not a competitor to it** — a second,
smaller forgery by the same broken generation, expressing the same theme (a keeper who
manipulated the others with authored law) through a different mechanism (a fabricated
*count* rather than a fabricated *purpose*). It threads cleanly into FACT 7/8 and the
six-mark ring already in canon.

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 The forged law: "the Covering" / the eighth mark

Canon already hands us the anchor. `learn-them-as-we-learned-them.md` lists the founding
ring as **six marks** read sunwise: Bow, Offering, Kept Light, Deep Line, Ward, Covering.
The seven *enforced* ways in code are kept-light, deep-line, dark-hours, offering, bow,
unspoken, sacred-beast. The ring text and the enforced set **do not line up** — "the Ward"
and "the Covering" are named in the founders' ring but are not among the seven the engine
tracks; "the dark hours," "the unspoken," and "the sacred beast" are enforced but absent
from that ring's six. This is currently a latent inconsistency. **This idea weaponizes it.**

The fabrication: a late keeper — name **Sella** is wrong (she drowned, gentle), Orin is too
silent, so use **a keeper of Iss's own generation, attributed but unnamed in canon: "the
hand that kept the count"** — added an **eighth observance, "the Covering of the Hands"**:
a daily rite of covering or hiding one's tools/inventory at dusk, claimed as the way that
"keeps the count true." It is written with the full authority of the real ordinances. It is
not in the founders' ring. The land never measured it. It was invented to make the others
**show their hands** — a control mechanism dressed as piety.

In play: the group finds a forged ordinance (`the-eighth-way.md`) that reads like R01-class
canon and adds an eighth rule. A diligent group will try to obey it. **The engine does not
track it** — there is no `the_covering` in `CUSTOM_KEYS`; no listener fires; no toll lands;
no honor clears anything. The *absence of the loop they have learned to expect from seven
real ways* is the tell. The discovery: **this one law has no land behind it.**

### 1.2 How the group proves it (the falsification path — made FAIR)

Per the web rule (canon §3) and the mercy of `customs-punishment.md`, no single surface can
soft-lock the read. Three independent falsifiers, any one sufficient:

1. **The count contradiction.** `learn-them-as-we-learned-them.md` is explicit: "six marks,
   and the order is the key… do not skip a mark to hurry." The forged ordinance asserts
   *eight*. The founders' ring is the oldest, most-authenticated source in the corpus (the
   master key that "reads every later stone"). A forged eighth cannot be a *founding* way and
   be absent from the founding ring. The arithmetic is the first crack.
2. **The enforcement contradiction (the engine itself is evidence).** The group has, by M3,
   internalized the five-stage shape from real ways: they have felt a torch gutter for an
   unkept light, heard the half-beat footstep past the Line. They *cover their hands* as the
   eighth law demands — and **nothing gutters, nothing is named, no honor-glow answers.** The
   land is silent in the one way it is never silent for a real lapse. (This is INV-8 used as a
   *diagnostic*: because real tolls are reliable and reversible, their absence is legible.)
3. **The script contradiction (cipher payoff).** The forged ordinance carries a keyed
   signature like every real ordinance — but where Iss keyed his lie on his own name as a
   dare, the forger keyed the eighth law's "authority line" on a **substitution** that, run
   against the founders' ring text, **does not resolve to any of the six founding marks.** It
   resolves to a plain word the other stones use for *to hide / to cover one's own*. The
   forgery signs itself, the way `no-wall-was-ever-built-here.md` says Iss "left the answer
   inside the lock."

### 1.3 The arc across five movements

- **M2 — plant (inert).** The forged ordinance is placed, read as just another found law.
  No flag, no fanfare. It is one more rule on the pile while the group is still learning the
  seven by punishment. Optionally a topside NPC (Aro, the established liar/rumor-monger from
  `customs-punishment.md` §A) parrots it as real — "you cover your tools at dusk, everyone
  knows that one" — so a trusting group adopts it. **The "OH" seed is laid here.**
- **M3 — friction (the nothing).** As the group starts honoring ways for real relief, the
  eighth law is the one that *never pays*. They cover their hands nightly; the cold never
  lifts because it never came. A meticulous group notices the asymmetry: seven ways have a
  felt loop; one does not. The doubt is *behavioral*, not narrated.
- **M4 — catch.** Tied to the Liar catch (FACT 8). When the group re-walks Iss and turns his
  key, the **same generational forgery pattern** is exposed on the eighth law: the record's
  flat correction names it as added-not-found. Running the substitution against the founders'
  ring confirms it signs itself "the covering" = *to hide one's own*. The eighth law collapses.
- **M4→M5 — re-read.** Catching it **recolors the seven.** The lesson is not "ignore that one
  rule." It is: *a keeper authored law to control the others, and you nearly obeyed a dead
  hand instead of the land.* This is the exact theme of the sealed spine — the ways were never
  a wall and were never even *all* the land's; some were a person's. It sharpens FACT 8 and
  feeds (never states) FACT 15: you must learn to read the land's enforcement, not a keeper's
  claim, because **you are about to become a keeper whose record the next group will read.**

### 1.4 "Or worse" (scaled DOWN, see critique)

The pitch floats "obeying a false law may do nothing, *or worse*." The worse version — a
false law that actively tolls you for *obeying* it (inverted enforcement) — is **cut** for
the headline, kept as one optional flavor: covering your hands (hiding inventory at dusk)
maps to *concealment*, and the only land-true reading of concealment is the_unspoken's
register (the dark "leans toward what is hidden"). So the **single permitted "worse" beat**:
a player who religiously covers their hands at dusk near the report-lectern may, very rarely,
draw a faint the_unspoken-class cold pulse — not because covering is a law, but because the
land reads *hiding* the way it reads the nearly-said name. This is land-true (it reuses an
existing real signal), never a fabricated inverted toll. It must be rare and deniable or it
re-teaches the false law as real (see Risk 2).

---

## 2. CRITIQUE — adversarial, honest

**Risk 1 (sharpest) — the unfalsifiable "nothing," and the on-camera dead air.**
A law whose only evidence is the *absence* of a toll is the weakest read in the build. On
camera it looks identical to "we did the real way wrong." A viewer cannot see a non-event.
*Mitigation:* never ship this as "an inert eighth *way*." Ship it as a **forged document whose
falsity is proven by three POSITIVE artifacts** — the six-mark count (a number that doesn't
add up, visible), the substitution signature (a cipher that *resolves to a word*, a positive
solve), and the record's flat correction in M4 (a readable carving). The behavioral "nothing"
is corroboration, never the sole proof. The payoff is a decode and a contradiction the camera
can *show*, not a shrug.

**Risk 2 — re-teaching the false law as real (the "worse" trap).**
If obeying the eighth law ever reliably tolls, the group learns the false law *is* enforced
and the whole thread inverts into nonsense. *Mitigation:* the only permitted toll near covering
is a **pre-existing the_unspoken/concealment signal**, rare and deniable, that reads as "the
dark notices hiding," never as "the eighth law works." If tuning this is fiddly, **cut 1.4
entirely** — the headline ("does nothing") is cleaner and fully sufficient.

**Risk 3 — orphaned-mechanic / consistency-law violation.**
A `the_covering` config flag with inverted/null enforcement is tempting but is an orphan: it
adds a *code* mechanic with no real teaching surface and no honor act, violating the lockstep
law. *Mitigation:* **add NO new custom key and NO new listener.** The fabrication lives entirely
in (a) one authored document, (b) one cipher solve reusing the built `substitution`, (c) the
existing six-mark ring contradiction, (d) the M4 record-correction beat that already exists in
shape (`no-wall-was-ever-built-here.md`). Zero new enforcement code. The "null enforcement
config flag" in the original LIKELY-MAPPING is **rejected**; see §4.

**Risk 4 — collides with / dilutes the Liar thread.**
Two "a keeper lied via authored law" threads could feel like one trick told twice, muddying
the Iss catch. *Mitigation:* make them **deliberately paired and sequenced**, not parallel.
Iss lied about *why* (purpose: the ways are a wall). The forger lied about *how many* (count:
seven are eight). The eighth-law catch is the **smaller, earlier rehearsal** that teaches the
group the *grammar of forgery* (turn the key, check the count) which they then apply to Iss.
It makes the Iss catch land harder, not softer. If synthesis finds even this too crowded,
**fold it into the Iss thread** as a single extra carving rather than a standalone doc.

**Risk 5 — Path A / precision.** No client install, no new tracking, so Path A is untouched.
Precision is *improved*, not risked: because we add no enforcement, there is no new signal that
could mis-fire an "it knows you" callout. The thread is pure authored corpus + cipher + an
existing M4 beat.

**Verdict: KEEP-SCALED.** Keep the forged-law thread as authored-document + cipher + paired
rehearsal-for-Iss. Cut the "inert eighth custom key / null-enforcement config flag." Cut or
heavily ration the "or worse" inverted toll.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (proof it can be done cold)

The forged ordinance (a keeper's hand, authoritative, written to be believed — warm-false,
the Iss register):

> the eighth is the covering of the hands. at dusk you put your tools from sight, that the
> count stays true and no hand is seen to hold more than its share. the founders set seven and
> a half; the half is the covering, and it was left for me to finish. cover, and be counted clean.

The record's flat correction (M4, the Archivist register — lowercase, exact, no emotion):

> the ring holds six marks and the seven are kept. there is no eighth. dig under the dusk-rite
> and you find no founding for it. it was added by a hand that wanted the others to show what
> they held. the land never asked for the covering. the land does not count hands. it counts
> the keeping.

A margin hand catching it (human, hard):

> we covered our tools every dusk for a winter because he wrote it down. nothing came of it.
> nothing was ever going to. — m.

(De-slop check: no "testament," no "little did they know," no named emotion, no three-adjective
list, no "not just X but Y." The forged line lies *warmly and plainly*; the record line *counts*;
the margin line is concrete and bitter by omission. "seven and a half" is the planted barb —
an ordinance precise about real ways would never carry a fraction.)

---

## 4. THREAD IT (the consistency law) — exactly where it lives, no orphans

**Canon FACTs touched / added (`arc/lore/canon-spine.md` §3):**
- Touches **FACT 7/8** (Iss's lie / its overturning) — the eighth law is the same generation's
  second forgery; the catch shares the "turn the key, re-read" mechanic.
- Touches **FACT 3** ("the customs are theirs, not new") and the founders' ring (six marks) as
  the authenticating source the forgery fails against.
- Adds **FACT 7b (proposed):** *a later keeper forged an eighth observance to control the others;
  the land never enforced it.* M2 FORESHADOW → M4 REVEAL. Paths: the forged ordinance
  (`the-eighth-way.md`); the six-mark ring count; the substitution signature; the M4 record
  correction. Sub-fact of 7/8, same theme (authored law as control), strengthens 15 (you must
  read the land's keeping, not a keeper's claim).
- Adds **INV (proposed) to `customs-punishment.md` family:** *the engine enforces exactly the
  seven `CUSTOM_KEYS`; any "law" outside that set is, by construction, fiction — falsifiable by
  the reliable absence of the toll loop.* This makes the engine's uniformity itself a clue and
  formally forbids adding a real `the_covering` key (guards Risk 3).

**Found-documents / journals that must mention or foreshadow it:**
- **NEW:** `arc/lore/documents/the-eighth-way.md` — the forged ordinance (kind: ordinance/forgery;
  author: a keeper of Iss's generation; era: the last full generation before the breaking;
  movement: 2; clue_bearing: true; links_to: learn-them-as-we-learned-them, the-ways-are-a-wall,
  no-wall-was-ever-built-here; answer: the substitution signature resolves to "to cover / hide
  one's own," absent from the six founding marks).
- **EDIT (margin only):** `no-wall-was-ever-built-here.md` — add one record line tying the eighth
  law into the M4 correction ("he was not the only hand that wrote a law it never owned").
- **EDIT (none needed, already supports):** `learn-them-as-we-learned-them.md` — its "six marks…
  do not skip a mark to hurry" is the count anchor verbatim; the forgery contradicts it untouched.

**NPC / Watcher voice lines that carry it:**
- **Aro** (SET-A surface NPC, the established liar from `customs-punishment.md` §A) parrots the
  eighth law as real in M2 — so the world later contradicts him, same pattern as his three
  existing lies. One new SET-A line (npcLines, human register, capitals allowed).
- The **Watcher/record** carries it only in M4 via the flat correction (archive register;
  lowercase; passes `registerDisciplineSelfTest`). The Watcher never asserts the law and never
  warns about it — it only *corrects the count*, in character.
- Proposed voice keys (camelCase, → voice.ts / voice.archive.ts at integration):
  `cardEighthForged` (Recovery Archive card body), `archiveEighthCorrection` (the record line).

**Cipher(s) / puzzle(s) — reuse the built 11:**
- **`substitution`** (forge/ciphers.ts:159) — the forged ordinance's authority signature; run
  against the founders' ring, resolves to the cover/hide word. Orin embodies substitution
  (canon §1) — fitting: the plainly-withheld alphabet exposes a plainly-withheld lie.
- **`bookCipher`** (ciphers.ts:230) — optional cross-check: the eighth law cites a non-existent
  "page seven and a half" of the ordinances; the page/line/word lookup dead-ends, a second
  positive falsifier (Mara's cipher, the Reader who would catch a bad citation).
- **`vigenere`** (ciphers.ts:119) — NOT re-keyed here; reserved to Iss. The eighth law uses a
  *different* cipher from Iss on purpose, so the two forgeries are siblings, not the same gag.

**Beat class(es) / listener(s) / tables / seed rows / sites.yml / voice keys that realize it:**
- **Listeners:** NONE. Explicitly no new listener; no edit to `CustomComplianceListener.java` or
  `DarkHoursListener.java`. (Guards Risk 3.)
- **Beat class:** the M4 correction reuses **`LecternFillBeat`** / **`SignWriteBeat`** (already
  built) to place the record's correction; the forged ordinance is placed by **`BookAppearsBeat`**
  or a lectern fill at M2. No new beat class.
- **Cipher node / seed row:** one new puzzle row for the substitution solve, tagged
  `teaches_custom = NULL` (it teaches *no* custom — it un-teaches one), `thread_key = 'surface'`
  (it lives in the document/record surface thread, like the_unspoken). Must pass
  `threadTagSelfTest` (thread_key ∈ THREADS; teaches_custom null is allowed/skipped).
- **sites.yml:** reuse `first_report_lectern_01` (the ordinance/report surface) — the forged law
  and its correction sit where the real ordinances sit, so the contrast is in-place. No new site.
- **Recovery Archive:** one `thread_cards` row on the **surface** thread (`cardEighthForged`),
  body_voice_key defined in voice.archive.ts so it passes `threadCardVoiceCoverageSelfTest`.
- **Rejected from the LIKELY-MAPPING:** the "custom with inverted or null enforcement (config
  flag)" — rejected as an orphan (Risk 3). The fabrication is corpus + cipher, not enforcement.

**Build-guard posture:** adds no `CUSTOM_KEYS` member (so `customKeyNamespaceSelfTest` unaffected);
the new puzzle row + thread card must satisfy `threadTagSelfTest`, `threadCardVoiceCoverageSelfTest`,
`registerDisciplineSelfTest`; the new doc cites only real sources (founders' ring, Iss docs).

---

## 5. PLANT THE PAYOFF — the "OH, that is what that was for"

**Plant — the deep one (M1→M2, frozen):** the **master-key ring itself**. The group assembles the
six-mark founders' ring (`clue_substitution_rosetta_ring`) to earn script literacy — and its sixth
mark reads **`covering`**. They adopt this as gospel: it is the alphabet every later stone is written
in. *They learn to read the world using a key whose last glyph is the word the forger will hide behind.*
Wholly inert: `covering` here is an honest founding way (guard/seal), indistinguishable from
furniture, doing nothing but its job of teaching glyphs.

**Plant — the surface one (M2, inert/ambiguous):** the forged ordinance `the-eighth-way.md` is read
as just one more found law amid the seven. Its barbs — **"the founders set seven and a half; the half
is the covering"**, the shallow-and-quick cut, the board *hung over* the old carvings rather than
among them, the missing name at its head — all read at first as archaic humility/poetry. The fraction
is the tell, dormant. The group covers their hands because Aro and the document both say to. Inert.

**Payoff (M4, when the Iss key turns):** the group runs the forger's substitution on the eighth-way
ordinance's foot-signature. It does **not** resolve to any of the six founding marks — it resolves to
**cover one's own**. The same alphabet they trusted since M1 now reads the forger's confession back at
them, *keyed into the seal so no one would check the lock* (the `the-eighth-way.md` margin, the
`no-wall` correction). In the same beat: "seven and a half" snaps into focus — there were never
seven-and-a-half ways; there were seven the land keeps and **one a man added so the others would show
their hands.** The "half" was the seam of the forgery showing; the honest sixth glyph `covering` was
the very word he hid behind. *OH — that is why nothing ever came of covering our tools; why the number
never sat right; why the board was hung over the old stones, not cut among them.* The trusted ring,
the dormant fraction, and the unrewarded nightly rite pay off together, and the catch hands the group
the **grammar of forgery** (turn the key, count the ring) they immediately turn on Iss himself.

Seed ledger:
- PLANT-deep = the master-key ring whose sixth mark is `covering` (M1→M2, learned as gospel) →
  PAYOFF = M4 the same key signs the forgery *cover one's own*.
- PLANT-surface = "seven and a half" fraction + board-hung-over-the-stones + nameless head + the
  unrewarded covering rite (M2) → PAYOFF = M4 substitution catch + `no-wall` record correction recolor
  all of them in one beat.
No plant without payoff; no payoff without plant.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES

| | |
|---|---|
| **Lives in** | M2 (plant) · M3 (behavioral doubt) · M4 (catch) · M4→M5 (re-read) |
| **Depends on** | the seven real ways being *felt* as reliable (the toll loop from `customs-punishment.md`) so the eighth's silence is legible; the founders' six-mark ring (`learn-them-as-we-learned-them.md`); the M4 Liar-catch machinery (`no-wall-was-ever-built-here.md`); the built `substitution` cipher |
| **Depended on by** | the Iss catch lands harder (this is its rehearsal); FACT 8 sharpened; FACT 15 fed (read the land's keeping, not a keeper's claim) |
| **Priority** | **P1 (arc-spine).** NOT P0 — the vertical slice must first prove the *real* toll loop reads on camera; an inert-law thread is meaningless until the reliable loop it contrasts against exists. Build the seven, then add the forged eighth as the first depth layer of M2–M4. |
| **Cut line** | if synthesis is crowded: fold into the Iss thread as one extra carving (drop the standalone doc), keep the six-mark count contradiction and the substitution barb. The "or worse" inverted toll is cut by default. |
