# IDEA — The Counting Base-Journal

> A book in the group's base shows a number, decreasing, in a dead keeper's hand —
> counting down to nothing explained, until it is. Pairs with the record-writes-you-in
> (`keeper-record.ts`). Engine seat: `BookAppearsBeat` / `LecternFillBeat` page-swap.
>
> Status: **KEEP-SCALED.** Priority **P1 (arc-spine)** with one **P0** seam.
> Owner instant: the single Accepting instant (`canon-spine §8.2`), shared — NOT minted here.

---

## 0. THE ONE-LINE THESIS (and the trap it must avoid)

A book sits in the base. Each cadence it loses a number — *eleven marks. ten marks.* —
written in a dead mason's hand who is plainly counting down to **his own keeping**, an
event he describes with warmth and dread but never names. The group reads it as a death
clock pointed at a stranger. At V it reaches **none** on the night the Accepting fires,
and the same hand's last page rewrites to address **them**. The countdown was never the
keeper's. It was the group's appointment, kept in advance, the way the stone at the
threshold is cut before the keeper is kept.

**The trap:** the future-dated **grave** (`grave.ts`, FACT 13b) ALREADY owns "a date that
reads as a death clock, bound to the Accepting instant, that opens at V." A naive Counting
Journal is a near-duplicate of the grave and an INV-16 / privacy hazard (a second per-player
named death-clock). The whole treatment below exists to make the journal the grave's
**complement, not its twin** — same instant, opposite register, opposite surface, opposite
subject-grammar. See §2.

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 The artifact
A single written book titled **`the deep-mark book`**, author **`orin`** (a sixth-fall
mason whose day-book voice is already canon: `journals-orin-brann-iss.md` §I). It lives in
the base — destination `chest` at the home hot-cell, or, when a lectern exists at the base
report anchor, a `lectern` so the page is read in place. It is NOT a fresh apparition each
time; it is **one book whose last page is re-swapped** as the count decrements. The number
is written the keeper's own way: **depth-marks**, not a calendar (`canon-spine §6.7`, eras
not years). Orin counts courses and depth-marks; he would never write "14 days."

The decrement is **diegetic and keeper-grammared**: each page is a dated day-book entry that
happens to *open* with where he stood and *close* on a mark that is one lower than last time.
The number is buried in the prose, not a HUD digit:

> *At the eleventh mark of the deep. Set four courses on the cairn-step. Eleven left to cut
> before the stone asks me down. I have cut it eleven times. I will cut it once more.*

The reader extracts "11 → 10 → 9…" by reading successive entries — a **count assembled by
re-reading**, exactly Mara's lectern-shelf grammar but in Orin's hand. This is the mystery's
engine: the number is *real and falling* but its referent is withheld.

### 1.2 What the number is bound to (NO ORPHAN)
The count is **not a free integer**. It is `ceil(remaining_until_accepting / cadence)` —
the number of curatorial cadences left before the **single Accepting instant** (`§8.2`,
injected, owned by the showrunner; shared with the grave's `dateMs` and the website
timestamp). So the journal decrements *because real time is approaching the bound rite*, on
the same clock the grave and summons read. It cannot drift; it cannot finish early or late;
it reaches its floor on the night the rite is due. **The countdown means something measured.**

Floor handling: when the remaining count hits `0` the final page swaps to the **last entry**
(§1.5). The book never shows a negative or wraps — idempotent high-water guards that (§4).

### 1.3 The five-movement play

- **Movement I (P0 seam — the plant).** The book is already in the base when the group
  arrives, sitting among the first-report furniture. Its FIRST page is **not** a counting
  page — it is one ordinary Orin entry (mason's grumble, a bow at the markers) ending on a
  number with no stated unit: *"…twelve left."* Inert. Reads as a salvaged dead man's
  journal, atmosphere, no signal it is *live*. Crucially it does NOT yet decrement — it sits
  one cadence so the group meets it as a static found object. (This is the "oh" plant, §5.)

- **Movement II.** First decrement fires (out of LoS, page-swap). *"…eleven left."* Same hand,
  same chest. The single changed digit is the hook: a group that screenshots pages (these
  veterans will) notices the book is **counting**. Now it is a clock with no labelled
  endpoint. Cross-refs to the keeper-stone thread: this is Orin, whose stone they may already
  have met (he speaks only when crouched — `canon-spine §1`). The book and the stone are the
  same man from two distances.

- **Movement III.** Decrements continue on cadence. The entries **darken structurally**, not
  by stated emotion — courses set drop from four to two to "I set none today"; the line
  "the stone asks me down" recurs and shortens. Around here the **grave** is carved at the
  threshold (FACT 13b) with a living player's name and a future date. A sharp group now holds
  TWO clocks — the cold carved stone (a name, a date) and the warm base book (a number, no
  name). They do not yet know the two are the **same instant**. This is deliberate: the seam
  between them is a clue, not a bug (§2).

- **Movement IV (the catch hinge).** Two things converge. (a) `keeper-record.ts` has by now
  moved the group's living rows UNDER keeper headings and begun the **keeper-hand** tier —
  Orin's own hand writing a living player. (b) The journal's count is low (3, 2). A keeper
  NPC line or an exposed-Iss line lets the group set the grave's carved date beside the
  book's remaining count and see they **land on the same night**. The death clock and the
  number are one appointment. FACT 13b's misread ("a death clock counting down to a named
  person") is now legible as what it is — an appointment for the Accepting — but the journal
  still has not said *whose*.

- **Movement V (the payoff).** On the night the Accepting instant passes, the count hits its
  floor and the **last page** swaps. Orin's hand, which has counted to none, stops counting
  and **turns outward** — the only page where the day-book addresses a reader. It does not
  reward, it does not explain FACT 15; it *receives* (`canon-spine §6.5`, the verb is
  receive/keep). In the same window the grave opens from the inside (KEPT) and the
  `keeper-record` "the present hands are entered" page fires. Three surfaces, one instant,
  one voice register. The book that counted down to a stranger's keeping was counting down
  to **theirs** — planted in M1, inert for two weeks, paid in one page-swap.

### 1.4 Why it is HARD / non-linear (difficulty law)
- The number is buried in prose across successive page-swaps — extracting "it is counting"
  requires re-reading, not a glance. No HUD, no step-ladder.
- It is one in-road of several to the same realization (grave date / website timestamp /
  summons `not_before` all encode the instant). A group that never reads the book still
  lands the ending; a group that *only* reads the book gets a different door to it. No single
  missed clue blocks understanding (`canon-spine §3` web rule).
- A **red herring is built in**: because Orin's entries name *his* keeping, the obvious
  (wrong) read is "this is just Orin's biography, already over." The reveal that the count is
  *live and future* re-reads the whole book.

### 1.5 Exemplar last page — see §3 (de-slop proof).

---

## 2. CRITIQUE — adversarial, honest

### 2.1 THE SHARPEST RISK — duplication of the grave (orphan-by-redundancy)
`grave.ts` already delivers "future date → reads as death clock → bound to the Accepting
instant → opens at V." A Counting Journal that ALSO presents a per-player named death-clock
is not a new mechanic; it is the grave again on a different block, and it risks **INV-16**
(two surfaces both pointing a death-date at a player lets the group triangulate WHO).

**Mitigation (load-bearing — this is the design):** make the journal the grave's strict
complement on five axes, so they are two readings of ONE date, never two date-clocks:

| axis | grave (`grave.ts`) | base journal (this idea) |
|---|---|---|
| surface | threshold stone, public/edge | base chest/lectern, private/home |
| subject | **a named living player** | **no living name** — Orin's own first person |
| number form | a carved **date** (rune+digit glyphs) | a **count of marks**, falling, buried in prose |
| register | the record's cold third-person carve | a keeper's warm broken first-person hand |
| reveal verb at V | the stone **opens** (KEPT) | the hand **turns to address** the reader (received) |

The journal therefore **never carves a living name** (it is Orin counting himself), so it
cannot triangulate any player — it is INV-16-safe by construction, where the grave is made
safe by other means. The two share only the instant. The M3 window where the group holds
both clocks without knowing they're the same is the intended mystery, not a contradiction.

### 2.2 Path-A / anti-jank risks
- **Page-swap witnessed.** If the swap happens while a player stares into the open chest/
  lectern GUI, the number changes on camera — a reveal-discipline violation. *Mitigation:*
  `BookAppearsBeat`/`LecternFillBeat` already gate on `mutateWhenUnwitnessed`; the run wrapper
  must additionally treat **any player with the lectern's book open / chest GUI open** as
  witnessing (not just line-of-sight), deferring the swap to the next tick. This is a small
  guard, not new infra.
- **Book duplication.** A player may carry the book away, drop it, or copy it (vanilla
  written-book copy). Then the "live" book in the chest decrements while a stale copy says a
  higher number — confusion, and a player could think the clock "reset." *Mitigation:* the
  decrement targets a book identified by an NBT/PDC tag (`observance:deep_mark_book`), and the
  swap **replaces the page on the tagged book in the home container only**; copies are inert
  by design and read as "an old transcription," which is in-fiction fine (a copied day-book
  doesn't keep counting). Do NOT chase carried copies — that way lies jank.
- **Empty-chest / griefed base.** If the home container is destroyed or full, the swap can't
  land. *Mitigation:* `BookAppearsBeat` already skips on no-container / inventory-full
  (decency floor); the producer simply holds the high-water mark and re-attempts next cadence.
  No loss, no error.

### 2.3 Precision / privacy risks
- The journal is first-person Orin — it names **no living player**, so it cannot mis-fire an
  "it knows you." This is its privacy-safe advantage over the grave/keeper-record. Keep it
  that way: **never** let the last page (V) name a specific player. It addresses the group as
  *the present hands* (plural, neutral), exactly like `keeper-record`'s "the present hands are
  entered." A singular "you, [name]" here would break INV-16 and §6.3.

### 2.4 On-camera misfire risks
- **The count is invisible if the group never re-reads.** A group that reads page 1 once and
  never returns sees a static dead journal and the whole clock is lost. *Mitigation:* this is
  acceptable BECAUSE of the web — the instant has three other doors. But to raise the odds,
  the M2 first-decrement should coincide with a `clue-drip` line that points (obliquely) back
  at the Hold's records, nudging a re-read without naming the book.
- **"Reset" panic.** If a restart re-fires page 1, the count appears to jump back up. *Mit:*
  strict idempotent high-water (§4), mirroring `grave.ts`'s two-row transaction and
  `keeper-record`'s tier high-water. A restart re-derives the SAME current count, never
  rewinds.

### 2.5 What to CUT / scale down
- **CUT: any per-day decrement.** Tie strictly to the **curatorial cadence**, not real days.
  A daily tick invites drift against the bound instant and a "why didn't it move" question on
  a quiet day. One decrement per cadence, derived from `remaining/cadence`, is jank-free.
- **SCALE DOWN: the number of counting pages.** Do not author 12 distinct entries. Author a
  **small bank** of entry templates keyed by count-band (high / mid / low / floor) in
  `voice.ts`; the producer selects the band and injects the depth-mark numeral. Twelve bespoke
  pages is content-bloat and a maintenance trap; four authored bands with a numeral slot reads
  as "the same man, same hand, fewer courses each time," which is the intended structural decay.
- **KEEP small:** one book, one home container, one tagged artifact. Resist a "journals across
  the base" spread — that collides with the herd/spread mechanic and dilutes the single clock.

---

## 3. DE-SLOP TEST — exemplar lines (in Orin's hand / the record's register)

> Anti-slop check applied: no named emotions, no "testament," no thematic bow, no three-adjective
> lists, plain declarative mason's diction, the count buried not announced, decay shown by shorter
> entries and fewer courses, not stated.

**M2 mid-band counting page (numeral slot = 11):**
> *At the eleventh mark. Set two courses on the step and could not set a third; the lime is
> from the deep galleries now and will not slake. Eleven left before the stone asks me down.
> I have not told Wenna's boy. He would ask what it asks.*

**M4 low-band counting page (numeral slot = 2):**
> *Two. Set no course. I sharpened the chisel I will not use. A man cuts the date before the
> keeping, my father said, so the stone is ready and not the man. The stone is ready.*

**M5 floor page — the turn outward (the payoff; addresses the present hands, names no one):**
> *None left to cut. I am done counting and the count was never only mine. Whoever reads this
> in the warm: the marks were cut for hands that were not here yet. They are here now. Keep the
> stone. It does not argue. That is its whole virtue.*

**Record/Archivist cross-line (Discord, M5, flat third person — for `clue-drip`):**
> *the deep-mark book is closed. the count is spent. the present hands are entered.*

---

## 4. THREAD IT — exactly where this lives (no orphan)

### 4.1 Canon FACTs it touches / adds
- **Primary home: FACT 13b** (`canon-spine §3b`) — *"the stone is cut before the keeper is
  kept."* The journal is the **second door** to 13b (the grave is the first). The book's
  decrement is the death-clock misread in the keeper's OWN hand; same Accepting instant
  (`§8.2`). Adds a Path to FACT 13b, mints no new integer.
- **Touches FACT 1 / FACT 14** — the record keeping the living, the record *receives* you.
  The M5 floor-page is a FACT-14 surface ("received/kept," never "reward").
- **Touches FACT 16** (filed by place / "kept here before you") — the marks were cut for
  hands not here yet; the journal is the *temporal* sibling of `name-where-never-been`'s
  spatial carve.
- **Adds NO new canon FACT.** It is a new PATH/surface on 13b+1+14+16, which is the correct
  altitude — `canon-spine §0.1` forbids self-minting integers. (If synthesis later wants it
  numbered, propose it there; default is no new FACT.)

### 4.2 Found-documents / journals that must foreshadow it
- `arc/corpus/journals-orin-brann-iss.md` §I (Orin) — the book's voice and the recurring line
  *"a thing the dead troubled to teach you, you keep"* and *"make the stone ask"* must seed
  the day-book grammar. Add a cross-ref entry `the-deep-mark-book` to its header cross-ref list
  (alongside `i-thought-it-small`, `observed-warned-left-at-threshold`).
- `arc/corpus/journals-vaun-mara-sella.md` — Vaun's *"i counted them in the dark"* is the
  thematic rhyme of a keeper who counts; one margin line should point at "a count is a kind of
  answer" (already in Orin's M11 entry).
- `arc/corpus/official-records.md` — one Archivist abandonment-era fragment ("the marks were
  cut and the hands not yet come") gives the book a SECOND door so it isn't single-thread.
- Founder margin (`canon-spine §3b FACT 13b` path): *"we cut the names before the keeping"* —
  the journal is the in-hand expression of that founder doctrine.

### 4.3 NPC / Watcher voice lines that carry it
- A keeper-NPC (Orin himself, **crouched-only** per §6.8) or exposed-Iss line at M4 that puts
  the carved grave-date beside the book's count: *"he was counting the same night the stone was
  cut for. the same night."* (Author in `npc-and-watcher-voice.md`.)
- The flat Archivist M5 cross-line in §3 above for the `clue-drip`.

### 4.4 Ciphers / puzzles it expresses (reuse the 11)
- **`a1z26`** — the depth-mark numerals are a natural a1z26 surface: an early page can spell a
  short token by mark-numbers (e.g. the courses-set per entry read as letters), giving the book
  a *solvable* layer beyond the visible count for the obsessive solver. Low-key, optional.
- **`bookCipher`** (Mara's grammar) — the count is *assembled by re-reading successive pages*,
  the same cognitive move as Mara's page/line/word cipher; reuse the bookCipher framing in the
  clue-web entry so the "read across entries to get the number" skill transfers.
- **`polybius` / `railFence`** as the optional hidden-token layer if a1z26 is too on-the-nose —
  one band's "courses set" sequence can encode a 5–6 char `[a-z0-9]` token found at the
  threshold (INV-14: the token is the answer, the count is only a pointer). **Pick a1z26 as
  primary; reserve polybius as the fallback.**
- No NEW cipher invented.

### 4.5 Beat classes / listeners / tables / state / sites / voice keys
- **Beat (reuse, no new class):** `BookAppearsBeat` (dest `chest`, home container) as primary;
  `LecternFillBeat` when a base lectern exists (page-swap in place). Both already exist and
  already gate on `mutateWhenUnwitnessed`.
- **New producer module (mirrors `grave.ts`/`keeper-record.ts`):**
  `discord/src/showrunner/deep-mark-book.ts` — PURE: `decideDeepMarkBook(input)` →
  `{ row: { count, band, voiceKey, dest } | null, marks, notes }`. Input carries the injected
  Accepting instant, `nowMs`, cadenceMs, and the per-arc high-water count. No DB/LLM/clock in
  the pure file.
- **Run wrapper:** `deep-mark-book.run.ts` — reads the instant + cadence, computes remaining,
  fires the beat at the home anchor, persists the high-water. Adds the **GUI-open witness
  guard** (§2.2).
- **Self-test:** `deep-mark-book.selftest.ts` — pins: monotonic non-increasing count; floor at
  0 never negative; idempotent (re-run same input → no rewind); names no player.
- **State (no migration):** extend `ShowrunnerState` (`state.ts`) with
  `deep_mark_count?: number` (the last-written count high-water / low-water) and
  `deep_mark_floor_fired?: boolean` (the M5 last-page one-shot). Stored on the same `settings`
  jsonb row, exactly like `keeper_record`, `grave`, `carved_cells`.
- **sites.yml:** reuse the **home hot-cell / `first_report_lectern_01`** anchor (already in
  `sites.yml`); NO new site needed. The book lives where the first report lives (the Hold's
  records), which is also why a re-read is natural.
- **Voice keys (`voice.ts`):** add a small bank —
  `deepMarkBookHigh()`, `deepMarkBookMid()`, `deepMarkBookLow()`, `deepMarkBookFloor()`
  (the four count-bands, each with a numeral slot the producer fills with the depth-mark) +
  `deepMarkBookClosed()` (the flat Archivist Discord cross-line). KEYS only; voice.ts is the
  sole text source (mirrors `keeperPageHand_*`, `graveCarved`, `recordOpened`).
- **clue-web / cipher-web:** add a `the-deep-mark-book` node in `design/clue-web.md` and
  `design/cipher-web.md` tying the bookCipher "read-across" skill + the optional a1z26 token to
  the threshold token destination, with the grave + website as its two co-doors to the instant.

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for"

- **PLANT (Movement I, inert):** the book is in the base on arrival showing one ordinary Orin
  entry ending *"…twelve left."* No unit, no decrement yet, no signal it is live. It reads as a
  salvaged dead journal — pure atmosphere. A player who reads it dismisses it.
- **AMBIGUOUS MIDDLE (M2→M4):** it decrements. Now it is "a death clock for a dead man,
  already over" (the red-herring read) OR "a clock for something coming" (the true read,
  un-forced). The group holds it next to the grave's date without knowing they are the same
  instant.
- **PAYOFF (Movement V, exact):** on the night the **Accepting instant** passes, the count hits
  its floor and the last page swaps to the **turn-outward** entry (§3). The "twelve left" from
  M1 re-reads instantly: it was never Orin's remaining days — it was the **number of cadences
  until the group's own keeping**, written two weeks early by the same dead hand that taught
  them the marks. The plant (a number with no unit) and the payoff (the same hand, now
  addressing *them*, at zero) are one mechanism: a single page-swap re-colors a fortnight of
  inert reading. No payoff without the M1 plant; no plant left un-paid.
- **Ledger:** register this plant/payoff in `WEB-MASTER §9` (the master seed ledger) alongside
  #12 (Mara's "closer count of the quick") and FACT 13b's grave, so the three "counting" seeds
  are tracked as one chorus, not three orphans.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

- **Lives across:** Movement **I (plant, P0 seam)** → II–IV (decrement + darken) → **V (payoff)**.
- **Depends on:**
  - the **single Accepting instant** being bound (`§8.2`, showrunner-owned) — HARD dependency;
    the book cannot count without it. Until bound, it shows the M1 inert page only (mirrors
    `grave.ts` returning no row until `acceptingInstantMs != null`).
  - the curatorial **cadence** value (from `decide.ts` / `reckoning.ts` cadenceMult) to compute
    `remaining/cadence`. SOFT — if cadence shifts, the count re-derives, never rewinds.
  - `BookAppearsBeat` / `LecternFillBeat` + `mutateWhenUnwitnessed` (already built).
- **Depended on by:** nothing gates on it (it **colors, never gates** — INV-12). It is one of
  ≥4 doors to the Accepting instant; the ending composer must not read it as a selector input
  (INV-11). The M5 floor-page should fire in the same window as the grave-open and the
  `keeper-record` "present hands are entered" page for the three-surface convergence, but does
  not block them.
- **Priority:**
  - **P0** for the M1 plant page existing at vertical-slice (a static found book in the base is
    cheap and is the seed everything else re-reads). Without the plant present early, the V
    payoff cannot land — so the *plant* is P0.
  - **P1** for the live decrement engine (`deep-mark-book.ts` + run + selftest + voice bank) —
    arc-spine, lands with the grave/keeper-record cluster.
  - **P2** for the optional a1z26/polybius hidden-token layer (depth for the obsessive solver).

---

## 7. ONE-PARAGRAPH BUILD ORDER (for the synthesis phase)
Author the four-band Orin voice keys + the M1 inert plant page first (P0, cheap, in voice.ts +
one `BookAppearsBeat` seed at the home anchor). Then build `deep-mark-book.ts` as a pure
producer mirroring `grave.ts` (injected instant, `remaining/cadence` → band → row), its run
wrapper with the GUI-open witness guard, and its selftest (monotonic, floored, idempotent,
nameless). Tag the book `observance:deep_mark_book` so only the home book counts and copies go
inert. Wire the M5 floor one-shot to the same window as `grave` open + `keeper-record` hand.
Register the plant/payoff in `WEB-MASTER §9`. Add the `the-deep-mark-book` cross-ref to the
three corpus files and the clue/cipher webs. No new FACT integer, no new site, no new migration,
no new beat class.
