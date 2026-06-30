# AUDIT — STORY / LORE / TWIST / DRAMA + ANTI-SLOP + VOICE (whole-corpus pass)

> Read-only audit. Fresh full pass over `arc/**` (sealed + corpus + documents),
> `design/**` (critiques, ideas, bibles), and the new authored work, through the
> story-lore-drama lens. Scope read: `canon-spine.md`, `_SEALED_ARC_BIBLE.md`,
> `bestiary-sealed.md`, `WORLD-BIBLE.md`, `LORE-BIBLE.md`, `found-documents.md`,
> all three `corpus/journals-*` + `corpus/letters.md` + `corpus/official-records.md`
> + `corpus/npc-and-watcher-voice.md` + `corpus/cipher-plaintexts.md`, the highest-risk
> 9 `lore/documents/*`, `design/critiques/slop.md`, `design/ideas/the-seventh-spine.md`.
>
> **HEADLINE.** This is top-tier ARG corpus. The voice discipline is real and held; the
> iceberg works; the keeper grammatical fingerprints (Vaun accumulates / Brann doubles /
> Orin breaks off / Mara cites / Sella mirrors / Iss reassures) are now installed in the
> sealed appendices and HOLD at the line level; the slop-critique's S1 fixes (A1 grave
> receipt, A2 "i will not say what i call it", A3 muster-not-chiasmus, A4 "the count is
> three") all actually LANDED in the shipped files. Sella's copybook (drawings-only
> Going-Out), the R-series "were they human" engine (heads counted as jars, R15's report
> observing its own author), and the L09/L10/L11 three-account Break are genuinely
> Wifies-grade. Phrase-level slop: **zero** banned phrases survive outside the rules-files
> quoting the banned list at themselves.
>
> What follows are the **residual** defects a ruthless pass still finds. One is a real
> timeline contradiction (S1). The rest are S2/S3 — a couple of voice-seam slips, a
> drama-flatness risk, and clarity hazards in the most-overloaded thread (the Seventh).

---

## SEVERITY-RANKED FINDINGS

### S1-1 — TIMELINE CONTRADICTION: the six keepers are "six generations" in the spine but **all contemporaries at the Break** in the corpus.
**Files:** `arc/lore/canon-spine.md §1` (the cast table) + `§2` (timeline) **vs.**
`arc/corpus/journals-orin-brann-iss.md`, `arc/corpus/journals-vaun-mara-sella.md`,
`arc/corpus/letters.md`.

**The defect.** `canon-spine §1` assigns each keeper a distinct generation:
Vaun = founding line, Mara = 2nd gen, Sella = 3rd gen, Orin = 4th gen, Brann = 5th gen,
Iss = "last full generation." That is **~6 generations** of separation. But the shipped
corpus stages **all six alive and interacting in the same Break/Going-Out window**:
- Iss writes letters *to* Orin (L06), Mara (L08), Brann (gate refusal, journal), and a
  younger "lad" (L08a) — and they answer (L07, L11) in the *same* era ("the last full
  generation").
- Vaun's day-book has **Iss come to be his second** ("fifth winter… Iss came to be my
  second"; `journals-vaun-mara-sella` Vaun arc) and Vaun answers Iss's market-talk with
  his ledger (Iss L-arc "to Vaun, who answers my market-talk").
- Brann's watch-dockets and Orin's day-book both narrate the **same Break** Iss caused,
  in real time, alongside Mara relighting lamps.
- L16 (the record's last hand) lists all six as kept **together** at one abandonment.

A founding-line warden (Vaun) cannot be Iss's working second AND four-to-six generations
his senior. The journals quietly collapse "generations" into "contemporaries who all fell
at the going-out," which is the *better* story (it is what makes the grief land — they kept
each other's lamps) — but it **directly contradicts** the spine's generation column and the
cipher-plaintexts hands ("four generations gone," L05's "vaun is of the founding line, four
generations gone" while Orin writes him). The corpus has already chosen; the spine row is
the stale artifact.

> Note this is NOT the same as the deliberate eras-not-years rule — eras are fine. The
> contradiction is that two canon files assert two **different topologies** of the same six
> people: serial (spine) vs. concurrent-at-the-end (corpus).

**Why it matters for the bars.** A hard, obsessive friend-group reconstructing the WHO
thread will build a generation chart from the letters' "the third generation / the fourth
generation / the last full generation" datelines — and it will not close. L05 says Vaun is
"four generations gone" while Orin corresponds about the cairn; L04/L12 put Sella "a
generation dead" while Mara still writes her at the Break; yet Vaun, Mara, Sella, Orin,
Brann, Iss are all "kept" at the *one* going-out (L16). The careful reader hits a wall that
reads as an authoring error, not a mystery.

**EXACT FIX (Finalize integrator).** Pick ONE topology and make the spine serve the corpus
(the corpus is shipped and is the stronger telling):
1. **In `canon-spine.md §1`, change the "Era" column** from per-generation ranks to a
   single shared frame: the six are the **last keepers**, of overlapping late generations,
   who kept the ways *together* through the Doubt → Break → Going-Out. Replace "second
   generation / third generation / fourth generation / fifth generation" with relative
   anchors that allow overlap (e.g. "an elder of the last keepers / the lampwright of the
   last keepers / the child of the last keepers / the mason / the watchman / the one who
   broke it") — keeping Vaun as the eldest/founding-descended, NOT founding-line-itself.
2. **Reconcile `§2` timeline** so "six keepers' generations rise and end" reads "the last
   keepers' lives overlap and end together at the going-out."
3. **Sweep `letters.md` datelines** for the two that assert hard generational distance from
   contemporaries: L05's "(vaun is of the founding line, four generations gone)" and L12's
   "a girl a generation dead." Either soften "four generations" → "long gone, of the first
   delvings" (Vaun as founding-*descended* elder, not literally four gens before Orin), or
   accept Sella's death as recent-within-the-same-collapse (L04/L12 already treat her as
   freshly grievable). The cleanest single edit: redefine Vaun as "of the founding **line**"
   = *descended from* the founders, the eldest still living, so he can be both the keeper of
   the oldest customs AND Iss's contemporary second.

This is the one defect that fails the "internally consistent, never broken" bar. Everything
below is polish.

---

### S2-1 — VOICE SEAM SLIP: D12 (the Keeper) puts a **named feeling** in Vaun's mouth, breaking Vaun's inviolable "inventories, never feelings" law.
**File:** `arc/lore/documents/bring-the-thing-only-you-can-give.md`, ¶ beginning
"bring, then, the thing only you can give."

**The line:**
> "vaun, who kept everything and gave nothing, **would tell you, if he could be made to
> speak of feeling**: it is the giving that was the whole of it…"

`canon-spine §1` and §6.8 make Vaun's voice **inviolable**: "speaks plainly, but **only of
what he kept** — inventories, never feelings." The slop appendix in
`journals-vaun-mara-sella` restates it: "He inventories. He does **not name a feeling**." The
Keeper here narrates Vaun *speaking of feeling* — and worse, hands the reader a tidy moral
("it is the giving that was the whole of it… he learned it too late") that is exactly the
resolved-bow shape the slop critique (A2/E4) banned. It also slightly editorializes Vaun's
fate as a lesson, which the cold register avoids.

**Why it slips through.** It is in the *Keeper's* mouth, not Vaun's, so a literal voice-check
passes ("Vaun isn't speaking"). But the Keeper is *attributing* feeling-speech to Vaun and
moralizing it — which thins the very thing that makes Vaun haunting (a man who could only
ever count).

**EXACT FIX.** Cut the feeling-attribution and the bow. Rewrite to state what Vaun *did*
(the un-struck second column) and let the group draw the moral:
> "vaun kept everything and set nothing back. his ledger runs one column and the second —
> given-back — he ruled and never struck. he is still counting it. lay the token in the
> place his giving was owed."

Keeps Vaun an inventory to the last; removes "speak of feeling" and "learned it too late."

---

### S2-2 — DRAMA FLATNESS: the four divergent fates are mechanically clean but **`divided` and `refusers` are under-dramatized** relative to `kept`/`cast_out`.
**Files:** `arc/corpus/npc-and-watcher-voice.md` BN2 (`fateDivided`, `fateRefusers`) + SET C
`keeper.endings.divided` / `.refusers`; `canon-spine §8.4`.

**The issue.** `kept` and `cast_out` land hard (markers face out / face the wall; the
abandonment in passive voice). But `divided` resolves to "the light holds on one side of the
floor" — which, with INV-16 forbidding any per-player reading, risks playing on-camera as
*arbitrary* ("half the floor is lit, we don't know who or why"). And `refusers` is gated on a
"positive defiance signal… the bow withheld on purpose" that the corpus never concretely
defines as an *act* — so the single most dramatic possible ending (the group is offered the
keeping and turns from it) is the thinnest-specified one. For a group that earns `divided` or
`refusers`, the climax is the payoff of the whole arc; these two cannot be the flattest.

**Why it matters for the bars.** YouTube-worthiness and friend-group-worthiness both peak at
the ending. A `divided` ending that reads as "the floor is half-lit for reasons we can't
parse" is a anticlimax, not a gut-punch. The fix is not more text (the bounded-close law is
right) — it is **legible dressing** the floor already carries.

**EXACT FIX.**
1. `divided`: make the geometry *narratively* legible without naming a player — split the
   floor light **along the keeper-row axis** (the side toward the markers stays lit, the side
   toward the unwriting goes dark), so the camera reads "the kept ways held the warm side; the
   broken ways took the cold side" as a *thematic* split, not a coin-flip. The Keeper clause
   already says "it chose by what was done, the floor only shows it" — give the floor a
   *direction* that means something.
2. `refusers`: define the "positive defiance signal" as a concrete diegetic act in
   `canon-spine §8.4` so it can be authored and recognized — e.g. *quorum present at the
   altar, the tokens laid, and then the group leaves the bow window empty by walking out
   together before the hour* (a walk-out, not an absence). One sentence of definition turns the
   thinnest ending into the sharpest. (Cross-check INV-19: still active-only.)

This is a drama/clarity upgrade, not a contradiction — but it is the difference between four
endings that all land and two that fizzle.

---

### S2-3 — CLARITY HAZARD: the **"seventh marker" (Sella's count) and the "seventh keeper" (the cast-out one)** are two different sevens that the corpus lets brush too close.
**Files:** `arc/corpus/journals-vaun-mara-sella.md` (Sella copybook + L04) and
`arc/lore/documents/what-the-surface-keeps.md` **vs.** `the-seventh-not-kept.md` /
`the-fire-they-let-out.md` (the cast-out Seventh, FACT 10b).

**The tangle.** There are, by design, multiple "sevenths" the canon keeps *deliberately*
distinct (Iss called "the seventh" colloquially = #6 in fall-order; the cast-out Seventh =
an unnamed distinct keeper; the surplus seventh *mark* in R10's muster). The Seventh-spine
design (`design/ideas/the-seventh-spine.md`) defends the Iss↔cast-out ambiguity well and it
is sanctioned. **But Sella introduces a fourth thread that is genuinely muddy:** Sella's
copybook and L04 fixate on "a **seventh marker** out past the six… one more place where a
marker should go" at the far water, and D06's margin says *"we set no seventh stone for HER,
because the lake had already set one."* This frames **Sella herself** as occupying the
seventh-stone slot — yet Sella is canonically keeper **#3, fully kept** (she is in the "six
are kept" list, L16/R06a). So a sharp reader gets:
- six keepers, six stones (Sella is one of them),
- a "seventh marker" Sella counts past the six (a *place*, the far water),
- the cast-out **Seventh keeper** (a *person*, unnamed, FACT 10b),

and the corpus never cleanly distinguishes "the seventh *marker* Sella is pointing at" from
"the Seventh *keeper* who was cast out." D06's margin ("the lake had already set one [stone]
for her") makes it sound like **Sella's stone IS the seventh** — which would make Sella the
cast-out Seventh, contradicting her membership in the kept six.

**Is it intentional?** Partly. The intended reading (confirmed by `the-seventh-not-kept.md`
and the spine §5) is: Sella's far-water *marker-count anomaly* is the **breadcrumb that points
to** the cast-out Seventh's shrine — they are a clue-chain, not the same entity. But the
shipped Sella text over-identifies her *with* the seventh slot ("the lake set one for her")
rather than her *pointing past* the six toward a seventh that is someone else. That is a
clarity defect, not a sanctioned ambiguity — it muddies the FACT 10b payoff.

**EXACT FIX.** In `what-the-surface-keeps.md` margin and the Sella copybook leaf, separate
**Sella's stone** (one of the kept six, set by the lake where she drowned) from **the seventh
she counts toward** (the cast-out one's, further out). Change the margin from "we set no
seventh stone for her, because the lake had already set one" to language that keeps Sella as
one of the six and makes her the *finger pointing past* it:
> "she is one of the six; the lake set her stone where it took her. but she counted one more
> past the six and we did not believe her. follow her count, not her grave — the marker she
> meant is further out, and no one set it."

This preserves Sella's drowning-as-Bow-fate AND the seventh-she-points-to as the distinct
cast-out keeper. Keep the Iss↔cast-out ambiguity (that one is sanctioned); fix only the
Sella↔seventh-marker over-identification.

---

### S3-1 — VOICE PRESSURE: Iss's warm letters risk reading as the **author's** cleverness, not a forger's, because the corpus flags his lie's *shape* so explicitly.
**Files:** `arc/corpus/letters.md` L08a (the warm circle) + its editorial footnote; D22
brief (`the-wall-of-warm-words`).

**The note (S3, not a defect — a caution for the Finalize lane and any further authoring).**
L08a is excellent craft — a circular argument where "each line is true and the circle shuts
on nothing." But its **footnote** ("read it again and find the gate. there is none… a lie in
the shape — a wall of true things with no door in it") *explains the trick to the reader*.
In-world that footnote is "the record" / a later hand, which is permissible, but it does the
group's catch *for* them. The whole Iss engine's payoff is the group **discovering** the
circle is a circle. A corpus footnote that announces "this is a circular non-argument" is the
designer winking — it lowers the difficulty and tips the hand. Same risk in the L08b lad-reply
footnote ("the lad asks the one question that cracks the circle").

**Caution (not a hard fix).** These footnotes are writer-facing scaffolding that read as
in-world marginalia. For the *player-facing* surfaces (the carved board-wall D22, the in-game
Iss letter), ensure the "find the gate, there is none" analysis is **never** surfaced to the
group as a hint — it lives only in the design doc / sealed appendix. The board-wall the group
reads must carry the warm promises and the columnar-name-read-after, and NOT a margin that
says "this opens nothing." Let `oracleDeadEnd(kind='prophet')` ("that is a warm thing… it opens
nothing. read who carved it, after") be the *only* nudge, earned by solving — not a free label
on the artifact. Flag for Finalize: confirm no player-facing Iss surface inherits the
explanatory footnotes.

---

### S3-2 — MINOR: `the-eighth-way.md` forged ordinance still half-credits its author once, against the slop-B4 "anonymous lie is stronger" rule.
**File:** `arc/lore/documents/the-eighth-way.md`, body.

**The line:** "the founders left the seventh half-cut and the eighth uncarved, trusting the
keepers-after to keep what they could not stay to set down. **I have stayed. I set it down.**"

The slop critique (B4) established the forged ordinance is a stronger lie when it hides behind
"the founders" and never credits "me/I." The forged board mostly obeys (no name at its head),
but "I have stayed. I set it down." is the forger stepping forward to claim the work — the
exact self-credit B4 flagged. The keyed signature ("cover one's own") is *meant* to be the
only place his self is hidden; this plain-text "I set it down" tips it in prose before the
cipher does.

**EXACT FIX.** Drop the first-person claim; keep it impersonal:
> "the founders left the seventh half-cut and the eighth uncarved, trusting the keepers-after
> to keep what they could not stay to set down. it is set down now, as they meant."

Removes the "I have stayed / I set it down" self-credit; the only trace of the author stays
where it belongs — keyed into the signature glyph.

---

## CROSS-CHECKS THAT PASSED (recorded so Finalize does not re-flag)

- **FACT 15 never stated in plain words** — re-verified line by line across all read
  documents + voice banks. The nearest approaches (D04 "ask which is still free to leave," D08
  "it does keep you. that is not the comfort you take it for," D12 "we would keep you," Brann's
  "in the count, on the side that does the counting") all stop at the half-veil. ✔
- **Keeper voices inviolable** — held everywhere EXCEPT the S2-1 Vaun slip in D12. Mara
  page-refs only, Sella reflection only, Orin crouch/break-off, Brann night/double, Iss
  plain-and-false: all clean. ✔ (one exception flagged above)
- **The two "sixes" (fall-order vs founders'-ring)** kept distinct; the two "eighths" (Unlit
  Deep latch vs forged Covering) kept distinct; the Seventh's two *places* (cold-hearth
  surface vs hearth-deep) kept distinct. ✔
- **Iss ↔ cast-out Seventh ambiguity** is sanctioned and correctly preserved (distinct by
  default, one ambiguity line behind `iss_caught`). NOT a defect. ✔
- **Slop S1 fixes from `critiques/slop.md` actually landed** in shipped files: A2 ("i called
  that a cruelty… i will not say what i call it" — `a-closer-count-of-the-quick`), A3
  ("the muster is read… it was of the hands" — `npc-and-watcher-voice` docketReread), A1 grave
  receipt, A4 "the count is three." All present and correct. ✔
- **Phrase-level slop: zero** banned phrases outside the rules-files. ✔
- **The L09/L10/L11 three-account Break** (accident / betrayal / mercy) is a deliberate,
  well-built unresolved contradiction — NOT a defect; it is flagged as intentional in the
  front-matter `contradiction_set`. ✔

---

## MUST-FIX LIST FOR THE FINALIZE INTEGRATOR (ranked)

1. **[S1-1] Resolve the keeper-generation contradiction.** Edit `canon-spine.md §1` Era
   column + §2 timeline so the six are overlapping *last keepers* who fell together (matching
   the shipped corpus), not six serial generations. Soften L05's "four generations gone" and
   reconcile L12. THE ONE INTERNAL-CONSISTENCY DEFECT — fix first.
2. **[S2-1] Fix the Vaun voice break in D12** (`bring-the-thing-only-you-can-give.md`): cut
   "would tell you, if he could be made to speak of feeling" + the "learned it too late" bow;
   restate Vaun as an un-struck-second-column inventory.
3. **[S2-3] De-muddle Sella vs the seventh marker** (`what-the-surface-keeps.md` margin +
   Sella copybook leaf): make Sella one of the kept six *pointing past* it, not the occupant
   of the seventh slot. Preserve the (separate, sanctioned) Iss↔cast-out ambiguity.
4. **[S2-2] Strengthen the `divided` and `refusers` endings** (`canon-spine §8.4` +
   `npc-and-watcher-voice` SET C): give `divided`'s floor-split a thematic axis (warm side =
   markers, cold side = unwriting); define `refusers`' "positive defiance" as a concrete
   walk-out act so the sharpest ending is authorable.
5. **[S3-2] Drop the "I have stayed. I set it down." self-credit** in `the-eighth-way.md`
   (B4 — anonymous lie is stronger).
6. **[S3-1] Confirm no player-facing Iss surface inherits the explanatory footnotes** (L08a/
   L08b "find the gate, there is none") — those stay sealed/writer-facing; the only player nudge
   is the earned `oracleDeadEnd(kind='prophet')`.

Items 1–3 are the load-bearing ones (one true contradiction, one voice break, one clarity
defect). 4 is a drama upgrade. 5–6 are polish. Nothing else read in this pass fails the bars.
