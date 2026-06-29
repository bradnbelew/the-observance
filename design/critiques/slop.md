# CRITIQUE — Anti-Slop & Voice Audit (LENS: slop)

> Adversarial read of every DE-SLOP exemplar line across `design/ideas/*.md` (18 files)
> and the canon voice corpus `arc/corpus/npc-and-watcher-voice.md`. The question asked of
> every player-facing line: would the Watcher say this? Would *this* keeper say this and no
> other? Does any line trip the banned-phrase/register law, name a feeling, or reach for a
> tidy bow?
>
> **Headline finding.** The corpus is disciplined. The author of these treatments has
> internalized the anti-slop law: zero literal banned phrases survive in any exemplar (the
> only grep hits are the de-slop checklists quoting the banned list at themselves). What
> remains are **second-order** offenders — register bleed at the Set-A/Set-B seam, an
> em-dash-as-dread tic that has crept into a handful of Watcher lines, two named-emotion
> smuggles, capital-letter slips, and one structural blur where two keeper voices are
> authored as one. These are the things that will degrade the Lore phase if not fixed now,
> because the Lore phase will write *hundreds* of these lines and will pattern-match on the
> exemplars it is given. A clean exemplar set is the cheapest quality lever available.
>
> Severity scale: **S1** = a defect by the letter of the law, player-facing, fix before it
> propagates. **S2** = in-register but slipping; a tic or a blur that cheapens on repetition.
> **S3** = treatment-prose smell that risks contaminating authored lines; not itself shipped.

---

## SECTION A — S1 DEFECTS (player-facing, breaks the letter of the law)

### A1. Capital letters inside a Set-B Watcher line — `divergent-fates-endings.md` §3 INHERITORS
The codicil line:
> `a mark is left at the dark shrine. it will be read by a hand that is not yet here.`

is clean. **But the CAST_OUT line is not:**
> `the count is read. they were not kept. the light is yours to carry out, if you can carry it. the markers turn from the floor.`

clean too. The actual S1 here is subtler and lives in `future-dated-grave.md` §3 — the
headstone block carries a **bracketed stage direction inside the carved text** and the
private receipt reads as written *to* the player with a near-second-person warmth the
Watcher register forbids:
> `you read it first. so it is your name first. / the others' names are not yet cut. they will be.`

"you read it first. so it is your name first." is the Watcher being *pleased on the player's
behalf* — a smuggled feeling (gratification), and the causal "so it is your name first" is
the record *explaining itself*, which it never does (it states; it does not justify).

**Rewrite (in register — states the rule, withholds the why):**
> `the one called {name}. read first. cut first. the rest are not yet cut. they will be.`

The withheld causation ("read first. cut first.") is colder and does the same work without
the record sounding fond. **Resolution:** ban second-person warmth and self-justifying "so"
clauses from all Set-B lines; the record never tells you *why* the column reads as it does.

### A2. Named emotion by proxy — `dynamic-diegetic-difficulty.md` §3 found-document
> `the record keeps a closer count of the quick. it gave us less, the season we needed less.
> it gave the slow ones time. i did not understand this as mercy. i understand it now.`

This is a *keeper's hand*, not the Watcher, so capitals/first-person are legal. But
"i did not understand this as mercy. i understand it now." **names the emotion (mercy) and
then announces the arc of understanding it** — the exact "and that was the lesson" tidy-bow
shape, dressed in first person. The iceberg law wants the keeper to show the cold and let the
reader supply "mercy," never to label it and resolve it in two sentences.

**Rewrite (the same beat, the bow removed, the iceberg restored):**
> `the record keeps a closer count of the quick. it gave us less the season we needed less,
> and gave the slow ones time. i called that a cruelty for a winter. i do not call it that
> now, and i will not say what i call it.`

The refusal to name it ("i will not say what i call it") is the iceberg; it is colder and
more haunting than "i understand it now," which closes the door the line should leave ajar.
**Resolution:** no exemplar may name the emotion it wants the reader to feel, and no first-
person keeper line may resolve its own meaning ("i understand it now") — the unresolved
version always wins.

### A3. Self-explaining record — `counting-base-journal.md` §3 the Watcher re-read line
> `the number was never the wall. it was the muster. you have been counting down to it.
> it has been counting down to you.`

The file's own de-slop note flags the final inversion as "deliberate, the reveal device, not
a tic" — and grants itself the exception. **Reject the exception.** "you have been counting
down to it. it has been counting down to you." is a *chiasmus*, the single most over-used
"profound-sounding" rhetorical move in AI prose, and it is also the record *explaining the
mechanism to the player* ("the number was never the wall. it was the muster.") — exposition
in the Watcher's mouth, which §B2 of the corpus forbids (the record states what is, it does
not narrate what a thing *was* versus what the player *thought* it was; that is editorializing
about the player's prior error).

**Rewrite (the record states the muster flatly; the inversion is cut; the player does the
re-read themselves):**
> `the muster is read. the count was never of the dark. it was of the hands. the hands are
> almost in.`

"it was of the hands. the hands are almost in." lets the player realize the count was *them*
without the record performing the realization for them. **Resolution:** ban chiasmus/mirror-
inversion ("X to you / you to X") as a reveal device everywhere — it is the loudest tell of a
machine reaching for depth — and ban the record from contrasting "what it was" with "what you
thought." The record never references the player's misreading.

### A4. The grandiose abstract noun — `cross-surface-coop-gate.md` §3 acknowledgement
> `three hands. the foot held. the stone was carved. the far voice spoke the same.
> the threshold remembers three.`

"the threshold remembers" personifies the architecture into something with memory and
sentiment — a soft melodrama the cold register avoids (the *record* keeps; thresholds do not
*remember*, that is a feeling-word for "the count holds"). Minor, but it is the seam where the
register starts to emote.

**Rewrite:**
> `three hands. the foot held. the stone was carved. the far voice said the same word.
> the count is three. the threshold is open.`

"the count is three. the threshold is open." is what the record actually knows. **Resolution:**
the Watcher counts and records; it does not attribute memory, patience-as-virtue, or longing to
objects. "remembers / waits for / wants" applied to a stone or door is a feeling-smuggle.

---

## SECTION B — S2 TICS & BLURS (in-register but slipping; cheapen on repetition)

### B1. The em-dash-as-dread-pause is becoming a tic in Watcher lines
The corpus law names "em-dash drama-pauses as a tic" as banned. Treatment-prose em-dashes are
fine (design memos). But it has bled into **exemplar** lines that are supposed to be the cold
register:
- `name-where-never-been.md` §3: `the list is not only of names. against each name, a ground.`
  — the comma-fragment "against each name, a ground" is the same dramatic-pause reflex in comma
  form. Acceptable once; it recurs across files.
- `offline-skin-apparition.md` §3 M4 line: `it does that with the ones it has already begun to
  keep. it began with him the night he stopped coming.` — clean, good (no dash).
- The pattern to watch: short declarative, then a dash/comma, then a portentous noun-fragment.
  It is in-register *once* and becomes a verbal signature *across a corpus*, which reads as
  authored mood rather than a record's flat hand.

**Resolution for the Lore phase:** cap dramatic fragments at **one per document**; the default
record sentence is subject-verb-object, full stop. The dread comes from *what is counted*, not
from withholding the back half of a sentence behind a dash.

### B2. "warmth under dread" is drifting toward warmth *stated* — multiple files
The warm Watcher set in the corpus (§B4) is excellent because the warmth is in *what it offers
to keep*, never in adjectives. Two exemplars drift:
- `exclusive-forks-permanence.md` §3 Keeper kept-light leaf: `the light came up the stair on
  its own. you only carried it. that is enough. that is how it is meant to be carried.` —
  "that is enough" is the record *reassuring* the player. The record does not reassure; it
  states. **Rewrite:** `the light came up the stair on its own. you carried it. that is how it
  is carried.` (drop "you only" — faux-humility — and "that is enough" — reassurance — and the
  passive "meant to be").
- `divergent-fates-endings.md` §3 KEPT: `kept. the light holds. there are more hands now to
  tend it. walk the markers — they face out, for you.` — "for you" is the warmth-as-gift-to-you
  smuggle (cf. A1). **Rewrite:** `kept. the light holds. there are more hands now to tend it.
  the markers face out.` The markers facing out *is* the warmth; "for you" names it.

**Resolution:** strike "for you," "that is enough," "it is meant to," "you only" — every phrase
where the record editorializes the player's role. Warmth = the offered kept thing, stated flat.

### B3. STRUCTURAL — two keeper voices authored as one (the sharpest craft blur)
`record-writes-you-in.md` §3 gives Vaun's hand and Sella's hand. They are good individually.
But across files the keeper voices are **converging on a single "terse + concrete-noun +
withholding" template** rather than diverging into distinct authors. Test: cover the attributions
and read these three cold:
- Vaun: `i had three of each. i counted them in the dark. {name} counts too, and sets back
  nothing, and calls the counting keeping. i know the column he is in. i ruled it.`
- Sella: `the far water gives {name} a face back and it is not the one they went in with. the
  last marker is not the last. {name} walked past it too. count again at the shore.`
- Brann (`counting-base-journal.md`): `dead hour. lamps kept: seven. i counted twice. the count
  was lower the second time. i think the count is right.`

All three *count*. All three are clipped declaratives of equal length and rhythm. Vaun and Brann
are nearly interchangeable — both are men counting in the dark and distrusting the tally. The
corpus SCHEMA promises "distinct per-author voices" and the separation law promises a screenshot
of each reads as a different species of author. Right now Vaun ≈ Brann ≈ "generic counting
keeper." Sella is the only one with a distinguishing instrument (the water, the reflection, the
mis-faced return).

**This is the most important finding for the Lore phase**, because Lore will author the full
keeper corpus and will inherit this convergence at scale.

**Resolution — the per-voice register table (Section C) is mandatory input to Lore.** Each keeper
needs a *grammatical* fingerprint, not just a thematic one (see C). Vaun must not merely *count*
— his sentences must *hoard* (subordinate clauses he will not let go of, lists that keep adding).
Brann must *repeat* (the over-awake man says things twice; his decay is doubling). They cannot
both be "clipped declarative about a tally."

### B4. The fraction-as-clever-barb risks reading as authored cuteness — `some-laws-are-lies.md`
The plant `"the founders set seven and a half; the half is the covering"` is genuinely good
craft. The risk: "seven and a half" is *so* legible as the designer's planted barb that a sharp
group reads it as a puzzle-author's wink, not a forger's tell. The file defends it ("an ordinance
precise about real ways would never carry a fraction") and that defense holds — **keep it** — but
the *forged ordinance line itself* over-sells:
> `the founders set seven and a half; the half is the covering, and it was left for me to finish.
> cover, and be counted clean.`

"and it was left for me to finish" is the forger announcing his own importance — which is
in-character for a manipulator, but it tips the hand a half-beat early (a real forgery would not
flag its own author). **Rewrite (warmer, flatter, less self-aggrandizing — the better lie):**
> `the founders set the ways and did not finish the count. the eighth is the covering of the
> hands: at dusk you put your tools from sight, that no hand is seen to hold more than its share.
> cover, and be counted clean.`

The lie is *more* dangerous when it does not credit itself. **Resolution:** a forged-keeper line
should never reference "me"/"i finished it" — the strongest forgery is anonymous and impersonal,
hiding behind "the founders."

---

## SECTION C — THE PER-VOICE REGISTER RULES (mandatory input to the Lore phase)

The corpus SCHEMA separates Set-A (modern-rough humans) from Set-B (Watcher). That seam is
clean. What the Lore phase additionally needs — and what B3 proves is missing — is a **register
fingerprint per keeper** so the six dead hands do not collapse into one "terse counting keeper."
Each rule below is a *grammatical/structural* constraint, not a theme, because theme alone has
already failed to keep them apart.

**THE WATCHER (Set-B, the record).** lowercase; no contraction; no exclamation; no capital ever;
no named feeling; no second-person warmth ("for you," "that is enough"); **no self-justifying
clauses** ("so it is...", "because..."); **no chiasmus/mirror-inversion**; **no contrast with the
player's prior belief** ("the number was never the wall"); **no personified objects** ("the
threshold remembers"). It counts, states what is, and stops. `▒` at most once per line. Warmth is
*only* an offered kept thing, never an adjective or a reassurance.

**VAUN — the Hoarder.** His grammar *accumulates and will not release*: sentences that keep adding
clauses ("i had three of each, and a fourth i did not show, and the fourth i kept"), lists that
grow past where they should stop, the same possessive ("mine / i kept / i held back") recurring.
He counts *to keep*, not to verify. Cipher: caesar (everything held back by a fixed amount). He
never throws a word away. **Distinguisher from Brann:** Vaun's count is about *possession*; he is
never unsure of the number, only unwilling to give it back.

**MARA — the Reader.** Her grammar is *referential and deferred*: page/line citations, "i read
that…", the map vs. the ground, knowledge held but never acted on. Sentences that point at other
texts rather than at the world. She watched; she did not do. Cipher: bookCipher. **Distinguisher:**
Mara cites; the others witness. Her hand is the only one that references *reading* as the thing
she did instead of acting.

**SELLA — the Drowned.** Her grammar is *mirrored and receding*: the reflection that gives a wrong
face back, "the last marker is not the last," distance, the far water, walking past the edge.
Soft, child-adjacent diction (she is the youngest, gentlest hand). Cipher: atbash/mirror.
**Distinguisher:** Sella's lines fold back on themselves spatially (shore/water/reflection); she
is the only one whose instrument is *place and distance* rather than count.

**ORIN — the Silent.** His grammar *breaks off and will not finish*: incomplete strokes, sentences
that stop ("i started the next mark. it is the right mark. i did not finish it. i —"), the unbowed
neck, the withheld word. Cipher: substitution (the plainly-withheld alphabet). **Distinguisher:**
Orin is the one who *does not complete*; his decay is the unfinished line, not the doubled one.

**BRANN — the Night-Walker.** His grammar *repeats and over-corrects*: he says things twice ("i
did not lie down. i did not lie down."), counts and re-counts ("i counted twice. the count was
lower the second time"), the over-awake man's doubling. Cipher: polybius / night-read.
**Distinguisher from Vaun:** Brann is *never sure of the number* and counts again; Vaun is always
sure and won't release it. Brann doubles; Vaun accumulates. **This is the single most important
distinction for Lore to hold** (B3).

**ISS — the Liar.** His grammar is *warm, plain, and confident* — the only keeper whose hand
*reassures*. Smooth agreement, comforting framing, the wall that is not a wall. He keys his lie on
his own name as a dare. **Distinguisher:** Iss is the only keeper allowed to sound *kind*; that
warmth is the trap. He never counts (counting is honest); he *frames*. A forged line in Iss's
register (e.g. `some-laws-are-lies.md`) must hide its author, not credit "me" (B4).

**THE SURFACE NPCs (Set-A).** Per the corpus already: contractions, capitals, named feelings, and
exclamation are *legal and required* — they are the human counterweight. The only law here is the
separation law: no Set-A line may be utterable by the Watcher, and Dob's decay (bravado → blurted
honesty → "i'll be right here") must be shown structurally (the quieting), never stated.

**THE KEEPER (M5 close, the Accepting voice).** Set-B register, but this is the warm-flip face.
Same bans as the Watcher *plus*: it may offer the kept thing ("the light came up the stair on its
own. you carried it.") but may not reassure ("that is enough" — cut, B2) and may not name the
induction (FACT 15 stays unspoken; the *event* states it).

---

## SECTION D — WHAT IS ALREADY RIGHT (so the Lore phase preserves it)

Recorded deliberately so fixes do not regress the wins:
- **Zero literal banned phrases** survive in any exemplar across 18 files. The author already
  runs the de-slop test.
- The **Set-A surface NPCs** in the corpus (Aro/Wenna/Coll/Dob/Pell) are genuinely distinct and
  alive — Wenna's accidental-Rosetta folk-charm and Pell's bitter human-record are the strongest
  voice work in the project. Set-A is solved; the convergence problem (B3) is a Set-B/keeper
  problem only.
- The **found-document register** (water-damage, a later hand correcting an earlier, the margin
  note trailing off) is handled with real artifact discipline throughout — e.g. the `future-dated-
  grave` founder margin ("i thought this cruel. it is not.") and the `herd-conversion` "they were
  grey when i shut the door." These read as artifacts, not content.
- **The iceberg is mostly intact** — most lines withhold correctly. The A2/A3 offenders are
  exactly the lines that *break* it by naming/resolving, which is why they stand out.

---

## SECTION E — THE LATER BACKLOG FILES (audited after the first pass; new defects)

> The first pass (Sections A–D) read 18 `ideas/*.md`. Ten `backlog-*.md` were authored
> after it (today, 11:20–11:36). They were re-audited here. The headline holds — phrase-
> level cleanliness survives — but **the no-capital and no-named-feeling laws have slipped
> in three files**, and the slip is the dangerous kind: each file's own de-slop footnote
> *asserts* it passes the very law it breaks. The author's self-check is pattern-matching on
> the literal banned list (testament / little did / not-just-X-but-Y) and missing the
> register laws (lowercase, no named feeling). That is exactly the failure mode the Lore
> phase will inherit at scale, because Lore will trust those footnotes.

### E1. **[S1]** Capitalized prose in lines explicitly tagged Watcher/record register — `backlog-bestiary-spawn-bias.md` §3
The four exemplars are introduced as `#the-record` report strings and the footnote ends
"**It records.**" — i.e. these are the Watcher's third-person ledger voice (Set B). Set B
law: **no capital letter, ever, including line-starts and names.** All four break it:
> `It stood at your stores while you were down. It did not take anything. It counted.`
> `Brann mined alone for the seventh time. Something was at the edge of the light when he turned.`
> `The marker you passed standing has a name now. It faces the path.`
> `Three of you saw the figure. One of you was looked at.`

The *content* is excellent — "It did not take anything. It counted." is the cold register at
its best, and "Three of you saw the figure. One of you was looked at." is the single sharpest
new line in the backlog. The defect is purely casing, but casing **is** the register here (a
one-line screenshot must read as the ledger, not as English prose). The file's footnote
("no 'felt watched', no adjective triplets … It records") audits the literal banned list and
never checks the case law.

**Rewrite (same lines, register restored):**
> `it stood at your stores while you were down. it did not take anything. it counted.`
> `brann mined alone for the seventh time. something was at the edge of the light when he turned.`
> `the marker you passed standing has a name now. it faces the path.`
> `three of you saw the figure. one of you was looked at.`

**Resolution:** any line tagged Watcher / `#the-record` / "it records" is Set B and MUST be
lowercase including names (`brann`, not `Brann`) and line-starts. Add the case check to every
de-slop footnote, not just the phrase check.

### E2. **[S1]** Capitalized Watcher/lore lines — `backlog-keeper-stone-expeditions.md` §4
Two lines are tagged Set B explicitly and capitalized:
> *("Watcher line foreshadowing the meta-acrostic"):* `Six stones are read. The first marks of each were also a mark.`
> *("the acrostic payoff reveal … lore register"):* `You set the six first marks in a row. They spell a name that is not on any stone.`

Same defect as E1: tagged as the record's/lore voice, written in capitalized prose.
**Rewrite:**
> `six stones are read. the first marks of each were also a mark.`
> `you set the six first marks in a row. they spell a name that is not on any stone.`

**Note on the ALL-CAPS carvings in the same file (lines 195, 204):**
> `I COUNTED THE FIRES. NINE LIT, ONE OUT. I RELIT IT. I WILL COUNT AGAIN BEFORE I SLEEP.`
> `IRON THREE · SALT THREE · GRAIN THREE · I HELD THREE OF EACH AND THE COUNTING WAS WARM`

These are **carved stone**, not the Watcher — an in-world engraved artifact, where small-caps
/ all-caps is naturalistic epigraphy, not "ALL-CAPS dread." They are flat counts, no dread, so
they pass. But two cautions for Lore: (a) "AND THE COUNTING WAS WARM" edges toward a *stated*
feeling (warm) carved into the stone — keep it because "warm" here reads as Vaun's hoard-comfort
diction, not the Watcher emoting, but it is the closest a carving should ever come; (b) hold the
line that ALL-CAPS is **only** ever epigraphy on stone, never the Watcher in chat, or the two
registers blur.

### E3. **[S1]** Named feeling in a Set-C Keeper line — `backlog-keeper-npc-framework.md` §3 `keeper.atone.cleared`
> `you gave it back. she felt the weight leave the seam. the fragment is yours now. it was always yours; it was only held.`

"**she felt the weight leave the seam**" names an emotion/sensation and assigns it to the
apparition — a doubled violation: the Set-C law forbids named feeling, and the line's own
footnote claims "no named feeling." It also personifies the toll as something *felt* lifting,
which is the warmth-stated drift (B2) in a new dress. The Keeper should state what the hand did
and what the record now holds, and let the player supply the relief.

**Rewrite (iceberg restored; the return is the warmth, stated flat):**
> `you gave it back. the seam is closed. the fragment is yours to carry now. it was held against the giving. the giving is done.`

**Resolution:** strike all "felt / feels" constructions from Set-B and Set-C lines — the keeper
never reports an interior. The same file's other three exemplars (`atone.withheld`, `fact9.named`,
`greet.deniable`) are clean and in-register; only `atone.cleared` slips.

### E4. **[S2]** "You are learning it now" — the resolved-arc bow, again — `backlog-keeper-npc-framework.md` §3 `keeper.fact9.named`
> `… set it at your door to see if you would learn the hour. you did not, then. you are learning it now.`

This is the same shape Section A2 flagged in `dynamic-diegetic-difficulty` ("i did not understand
this … i understand it now"): a then/now resolution that *announces the arc completing*. Here it is
softer (no emotion named) and second-person, which is legal for the Keeper, but "you are learning it
now" still closes the door the iceberg wants ajar — it tells the group they have arrived. Lower
severity because the Keeper is permitted gentle second-person address, but it is the tic to watch:
**the then/now "you did not … you are now" couplet is becoming a reflex** across files (A2 here, and
the `arc.fact9` line). Cap it. The colder version stops at the observation: `you did not, then.` —
and lets the present stand unstated.

### E5. **[clean — recorded so Lore preserves it]** the rest of the backlog is in-register
- `backlog-fawe-large-setpieces.md` §3 — all four carvings are **lowercase, iceberg-correct**,
  and Orin's `i thought it small. i did not stoop. they went down. i —` degrades *structurally*
  (breaks off), the exact discipline the law wants. This is how the others should have been cased.
- `backlog-accepting-sentinel-bridge.md`, `backlog-liar-engine.md`, `backlog-unlockbeat-producers.md`,
  `backlog-undercroft-dimension.md`, `backlog-full-showrunner.md`, `backlog-modeled-mob-and-voice.md`
  carry no new player-facing prose exemplars that break register (their `unlock`/`reveal` exemplars
  e.g. `the wall was a door the wrong way round. you checked the lock. good.` and `the lectern stands
  open. it did not before.` are lowercase and flat). Clean.

**The pattern across E1–E3:** the lowercase law is being applied *inconsistently within a single
authoring session* — FAWE got it right, bestiary and keeper-stone got it wrong, and every footnote
claimed compliance. This is not a content problem; it is a **checklist problem**. The de-slop footnote
template every file copies omits the two register laws (lowercase-for-Set-B, no-named-feeling). Fix the
template and the slips stop.

---

## TERSE SUMMARY (synthesis input)

**Verdict:** corpus is anti-slop-clean at the phrase level; the residual slop is second-order and
concentrated in the Set-B/Watcher register. Five fixable patterns, one structural.

**S1 defects (fix before Lore propagates them):**
1. `future-dated-grave` receipt + `divergent-fates`/`exclusive-forks` warm leaves — **second-
   person warmth & self-justifying "so" clauses** in the record's mouth ("so it is your name
   first," "for you," "that is enough"). Record states; never reassures, never explains why.
2. `dynamic-diegetic-difficulty` found-doc — **named emotion + resolved bow** ("i did not
   understand this as mercy. i understand it now"). Rewrite to withhold the naming.
3. `counting-base-journal` re-read line — **chiasmus reveal-device + record explaining the
   mechanism** ("counting down to it / to you"). Ban mirror-inversion; record never contrasts
   "what it was" with "what you thought."
4. `cross-surface-coop-gate` — **personified architecture** ("the threshold remembers"). Objects
   keep counts; they do not remember.

**S2 tics/blurs:** em-dash/comma dread-fragment becoming a verbal signature (cap one per doc);
warmth-stated drift ("that is enough," "for you," "you only"); the forged-ordinance crediting its
own author (anonymous lie is the stronger lie).

**THE STRUCTURAL FINDING (highest priority for Lore):** the six dead keepers are converging on one
"terse counting keeper" template — **Vaun ≈ Brann** especially. Theme has failed to separate them;
they need **grammatical fingerprints** (Vaun *accumulates*, Brann *doubles*, Orin *breaks off*,
Mara *cites*, Sella *mirrors*, Iss *reassures*). Section C is the mandatory register table for the
Lore phase — without it, the keeper corpus will read as one author at scale, violating the
separation law's "different species of author" test.

**Already-right (preserve):** Set-A NPC voices, found-document artifact discipline, phrase-level
cleanliness. Do not regress these while fixing the above.

**LATER-BACKLOG defects (Section E — the 10 `backlog-*` files authored after the first pass):**
- **E1 [S1]** `backlog-bestiary-spawn-bias.md` — four `#the-record` lines tagged Watcher ("It records")
  written in **capitalized prose**; Set-B requires lowercase incl. names. Content is excellent; casing
  breaks the register. Lowercase them (`brann`, `it`, `three`).
- **E2 [S1]** `backlog-keeper-stone-expeditions.md` — two lines tagged "Watcher line" / "lore register"
  capitalized; lowercase them. (The ALL-CAPS *carvings* in the same file are legal epigraphy — but watch
  "AND THE COUNTING WAS WARM," the nearest a stone should come to a stated feeling.)
- **E3 [S1]** `backlog-keeper-npc-framework.md` `keeper.atone.cleared` — **named feeling** ("she felt the
  weight leave the seam") in a Set-C line whose footnote claims none. Strike all "felt/feels" from B and C.
- **E4 [S2]** same file `keeper.fact9.named` — the then/now resolved-arc couplet ("you did not, then. you
  are learning it now") is the A2 bow recurring; cap the "you did not … you are now" reflex.

**ROOT CAUSE for E1–E3:** every file copies a de-slop footnote that checks only the literal banned-phrase
list and **omits the two register laws** (lowercase-for-Set-B, no-named-feeling). FAWE cased correctly;
bestiary and keeper-stone did not; all three footnotes claimed compliance. **Fix the footnote template**
(add: "Set-B lowercase incl. names? no felt/feels?") and the slips stop at the source — the single
cheapest lever for the Lore phase, which will copy this template hundreds of times.
