# Idea — The Cold-Start Prologue (week-zero ignition)

> **One-line:** before anyone knows there is a game, seed ONE anomaly juicy enough
> that a friend screenshots it to Discord *unprompted*. Make the FIRST notice
> deliberately findable; let everything after it be as subtle as the design wants.
> That first screenshot is ignition — it converts a quiet haunting into a group
> investigation.
>
> **Status (2026-06-25): KEEP — the spine is BUILT; finish four small gaps.** This idea
> is no longer a proposal. Its pure ordering policy ships as `discord/src/showrunner/prologue.ts`
> (`decidePrologue`), its gate is folded onto the snapshot in `run.ts`, and `decide.ts`
> already suppresses the curatorial drip until ignition. This doc is now the *build-aware*
> treatment: what plays, what is wired, and the exact remaining seams.
>
> Read against: `FLOW.md` (Act 1 / Movement I), `design/immersion-blueprint.md` §1/§4/§5,
> `design/WEB-MASTER.md` §1.M1 + seed-ledger rows 4/8, `arc/lore/documents/the-record-opens.md`
> (FACT 1/2/14), `arc/lore/canon-spine.md` §3/§6, `discord/src/voice.ts` (`recordOpened`),
> `discord/src/showrunner/{prologue,decide,run,types}.ts`, `plugin/.../beats/lib/LecternFillBeat.java`,
> `plugin/src/main/resources/sites.yml` (`first_report_lectern_01`, `first_marker_01`).

---

## 0. THE PROBLEM THIS SOLVES (and the one it must not create)

The whole engine is built on **restraint** — INV-7 ("silence is canon"), the ~90%-quiet
default, soft-pressure that goes *quiet* not *louder*. That restraint is the single biggest
reason the arc reads as authentic to an ARG critic. But restraint has one failure mode the
rest of the design does not cover: **week zero**. If the world is 90% quiet from session one,
and the friends do not yet know there *is* a world to investigate, a perfectly-built Movement I
can be missed entirely. A lectern report that "names a habit nobody told them was a law" only
lands if someone *reads the lectern* and *thinks it strange enough to mention*. In a veteran
group mid-build-a-base, a single book on a single lectern can sit unread for a week.

The engagement risk is asymmetric: **everything downstream depends on the group deciding,
once, on their own, that something is happening.** No clue-drip, no Whisper economy, no
keeper-stone web matters until that first "wait — did you see this?" fires in Discord.

So the Cold-Start Prologue is a **single, deliberately-loud-by-our-standards ignition beat**,
calibrated to be *findable and screenshot-worthy* without breaking the world's voice or
spoiling anything. It is the one place we turn the loudness knob up. After it fires and is
detected as landed (§1.4), the knob returns to baseline and never rises again in Movement I.

The danger it must NOT create: **becoming the orphaned gimmick** — a loud opening stunt that
doesn't connect to the FACT web, reads louder than the Watcher's register, or trains the group
to expect spectacle (which would poison the restraint the rest of the arc runs on). §4 is the
threading that prevents that; §2 is the honest critique of where it nearly fails.

---

## 1. EXPOUND — the full mechanic + story + mystery treatment

### 1.1 What the prologue actually IS

The Cold-Start Prologue is **the first report, re-staged as a discoverable anomaly** — the
*same* `the-record-opens.md` content (FACT 1 + FACT 2), but placed and framed so it is found
within the first one or two sessions rather than whenever someone happens to read a base
lectern. One notice, delivered with three calibrated "loudness" affordances stacked so at
least one trips even an inattentive group:

1. **Placement on the path, not in a corner.** The first report does not wait on a pre-placed
   `first_report_lectern_01` at null coords. The prologue retargets it to the group's actual
   detected base anchor (`bases`, via `BaseDetector`) — specifically the **most-trafficked
   block the group passes through** (the bed/door threshold, highest `heatmap_cells` count
   inside the base radius). It is placed where they *must walk*, not where they might wander.

2. **A pre-placement tell that survives a dead world.** A lectern alone is silent furniture.
   So the prologue pairs the report with **one** ambient affordance that draws the eye to it
   on first approach, fired *once*, reveal-disciplined (placed out of sight, witnessed only as
   an after-state): the companion **lit marker** (`first_marker_01`) — one small carved stone
   with a single light source (candle/lantern) that "was not there yesterday," beside the
   lectern, and the book is **already open to the named page** (`Lectern.setPage`). The group
   walks past a place they know cold, and one new lit object is sitting in it. That
   incongruity — *something is in my base that I did not put here* — is the screenshot trigger,
   stronger than any text.

3. **A name they cannot deny.** The report's marquee line names a **real measured habit**
   (FACT 2, grounded per canon §6 rule 4) — but only when the precision gate passes (§2.2).
   The one player whose `dossiers`/`custom_compliance` signal is *overwhelming* by the end of
   session one (the clearest Offering-keeper or Bow-skipper) is named for the thing they
   actually did. The anomaly stops being "spooky book"; it becomes "**spooky book that knows
   what I specifically did last night.**" That is the unprompted-screenshot payload.

### 1.2 The Discord half — the catch, not the cast

The prologue is **silent on Discord by default.** Counter-intuitive and load-bearing: the
ignition must come *from a player*, not from the bot. If the Watcher posts "the record is open"
before anyone has found the lectern, we have *told* them there is a game — which kills TINAG
and the entire "they discovered it themselves" authenticity the YouTube cut depends on
(immersion-blueprint §5).

The Watcher's only week-zero presence is **passive and ambient** (already built, no new work):
the bot is in the server, member-list status reads "The Watcher — Watching the ways"
(`BOT_PRESENCE`), and the **server icon is the rune ring** (the master rabbit hole,
`learn-them-as-we-learned-them.md`). These sit there, unremarked, *before* ignition.

When a friend screenshots the lectern into `#the-record` and asks "did one of you do this?",
the Watcher does exactly one thing, **once, and only after a human posts about it**:
`voice.recordOpened()` — *"▒ the record is open. it was open before you."* It does not explain.
It confirms the channel is part of the thing, in a line that implies it *predates the question*.
This is the one-shot ack `prologue.ts` returns via `postAck: true` on the ignition tick.

### 1.3 The world after ignition — the knob goes back down

The moment ignition is confirmed, the prologue is **over** and the engine reverts to its
designed low budget (`FLOW §2`: ~1–2 tiny ambient beats/session, deniable). The contrast IS
the point: the prologue is the *only* loud thing in Movement I. Everything after — private
sounds, torch-gutters, the second report — is quieter than the prologue by design, so the
group's own attention does the work. We spent the loudness once, to buy a group that is now
*looking*. The rest of M-I rewards looking; it never shouts again.

### 1.4 How "did it land?" is measured (no guessing, no nagging)

Ignition is a **detected signal**, not a timer and not Ethan's eyeball. Two independent
detectors, either of which sets `arc_state.flags.prologue_ignited`:

- **In-world:** any player interacts with the prologue lectern (`PlayerInteractEvent` on the
  protected `first_report_lectern_01` block) — they *read it*. Proof of discovery.
- **Cross-surface:** a human message lands in `#the-record` within the ignition window (the
  `messageCreate` scan that already feeds the Oracle). A human posting in that channel at all,
  pre-ignition, is the screenshot landing.

The showrunner reads this flag onto the tick as `prologue.curatorialAllowed` (`types.ts`
`PrologueGate`; computed by `decidePrologue`). While `!ignited`, `decide.ts` line 76–84
suppresses the curatorial clue-drip (gifts still apply — player-helpful, never gated).

If **neither** detector fires after an expected-pace window (~2–3 sessions, tuned in the
snapshot), the showrunner does the soft-pressure thing — **not** louder, but *closer*: it
re-targets the next ambient beat to the hottest current `heatmap_cells` cell on the group's
path, and, if still nothing by the backstop threshold, gently escalates the prologue's
pre-placement tell *once* (a second lit object; a `PrivateSoundBeat` to the named player on
next login). It never posts on Discord to announce the game. The hook resurfaces tied to what
they are already doing (`FLOW §1`). The prologue can be *patient*; it cannot be *skipped* — but
patience, not volume, is how it waits.

### 1.5 Story register — what the prologue MEANS in canon

In-fiction the prologue is not a special event at all — it is **the record doing the one thing
it always does first: the first naming** (canon §2 timeline, "~the 7th winter — the first
naming"; `the-record-opens.md` FACT 2). Every prior keeper generation got this exact notice.
The group sits the same entrance exam Orin's generation sat. The prologue's *loudness* is a
production choice (calibrating discoverability); the *content* is the oldest, most canonical
beat in the timeline. This is what keeps it from being a gimmick: it is not an added mechanic,
it is the **existing Movement-I notice, tuned for ignition**.

### 1.6 Across the ~2-week / 5-movement arc

- **Movement I (Days 1–2, not "~Day 3–4"):** the prologue IS M-I's opening beat. It fires,
  lands, the knob drops. The rest of M-I proceeds as designed (the quiet "it knows me" beats,
  immersion-blueprint §2).
- **Movement II:** the prologue is *re-read*. Once the Rosetta teaches the rune script, the lit
  marker beside the first lectern is revealed to have carried a **rune glyph all along**
  (seed-ledger row 4; §5). The first notice was already in the script; nobody could read it on
  day one.
- **Movements III–V:** the named habit threads forward exactly as the normal first report does
  — it re-reads in Orin's biography (M-IV, `observed-warned-left-at-threshold`) and its FACT-14
  buried line ("the record does not close at the rite… it receives you") pays off at the
  Accepting. The prologue inherits all of `the-record-opens.md`'s existing forward-links; it
  adds **one new physical seed** (the lit marker, seed-ledger row 4).

---

## 2. CRITIQUE — adversarial and honest

### 2.1 The sharpest risk: it trains the group to expect spectacle, poisoning restraint

**Risk.** The arc's authenticity rests on restraint (INV-7, ~90% quiet). A loud, findable, "it
knows your name" opening could set the wrong expectation — the group now expects the world to
*perform*, and the designed silence of Days 3–14 reads as the game "going quiet / being broken"
rather than as dread. This is the single most likely way the prologue backfires on camera: a
great opening, then a perceived anticlimax.

**Mitigation.** (a) The prologue's loudness is *one lit object + one named line*, not a
spectacle — loud only relative to a near-zero baseline, and **still in register** (no
jump-scare, no announcement). (b) The named line itself models the restraint: the Watcher
*states and goes quiet*. The first thing the group learns about the presence is that it is
patient — which reframes subsequent silence as the presence *watching*, not *absent*. (c) The
drop is immediate and total: after ignition the knob is at baseline (the `decide.ts` gate
flips, nothing escalates), so there is no spectacle to chase. The group is trained to *look*,
not to *expect a show*.

### 2.2 Precision risk — naming the wrong player at ignition (the sharpest, mitigated in code)

**Risk.** The payload is "it knows what *I specifically* did." If session one has not produced
an *overwhelming* signal, naming a player is a precision violation (canon §6 rule 4 — a wrong
callout is worse than none) and ignition misfires into "huh, that's not even true."

**Mitigation (already encoded in `prologue.ts`).** The named line is **conditional and degrades
safely**. `decidePrologue` returns `reportVoiceKey: 'recordOpenedNamed'` ONLY when
`overwhelmingSignal && signalName != null`; otherwise it returns `'recordOpened'`, the un-named
FACT-1 fallback. The named path requires an explicit `overwhelmingSignal` flag the caller sets
**only on a confident single-dominant measurement** — never a guess. If no signal is
overwhelming after session one, the prologue falls back to the un-named framing (the
`the-record-opens.md` collective marquee: "the living are written here, each by the name they
answer to"). The lit-marker incongruity still ignites ("something is in my base"); the name
just waits for the second report when the signal is real. **Precision is never traded for
ignition.** (GAP: the `recordOpenedNamed` voice key the decider names does not yet exist in
`voice.ts` — see §4 / §7. Until it does, the named branch has no string to emit; the un-named
branch is safe and ships first.)

### 2.3 Anti-jank — placement / reveal / idempotency

**Risk.** A pre-placement tell that "draws the eye" tempts a witnessed mutation (the cardinal
Reveal sin). Retargeting to a live base means writing to a possibly-loaded chunk near players.

**Mitigation.** Everything routes through `LecternFillBeat` + `mutateWhenUnwitnessed`
(enforced in the class — line 70) and the existing `Reveal`/`Placement` validators. The lit
marker is a `SmallStructureBeat`/block-set under the same unwitnessed gate. The decider is pure
and the emission idempotent (`status:'fired'` guard, INV-6; the one-shot ack guarded by
`acked`); if the chunk is witnessed it waits for the next unwitnessed window. No new anti-jank
surface — the prologue uses only existing, validated vehicles.

### 2.4 Path A — does it cost the friends an install? No.

Lecterns, vanilla light blocks, vanilla book NBT, the existing Discord bot. **Zero client
install.** The lit-object glow is vanilla light, not a shader. Path A holds. (The richer
fog/shader version is Ethan's solo B-roll path only — immersion-blueprint §5.)

### 2.5 Collective-law — does naming one player elect a "chosen one"? No, but watch it.

**Risk.** Naming a single player *first* could read as electing them.

**Mitigation.** The prologue names a **conduct**, not a favorite (canon §6 rule 3), and names
*whoever* the signal points at — across the run, different players get named in different
reports. The first naming is "the one who did X," identical in form to every keeper-stone's
rhyme; it confers nothing and gates nothing. The ignition payload is the incongruity, which is
group-facing ("our base changed"); the name is texture. The §2.2 un-named fallback is the
default-safe form and loses almost no ignition power, so if even this feels too singular for
the group's taste it degrades to collective for free.

### 2.6 On-camera failure modes

- **It fires when no one is recording.** Mitigation: in CONFIRM mode the *named* report is a
  `pending` beat Ethan releases when POVs are rolling and the group is heading into base; the
  ambient lit-object can pre-place AUTO. The ignition is real (players don't know); only the
  *timing* is staged (immersion-blueprint §5 rule: control timing, not content).
- **One player finds it and says nothing.** Mitigation: placement is on the shared path + the
  lit object is group-visible, so discovery is likely multi-witness; and the in-world
  read-detector flips ignition regardless of whether they post — the engine knows it landed.

### 2.7 VERDICT on scope

**KEEP — scaled to "re-stage the existing first report; do not invent a second mechanic."**
The one part to **CUT**: any temptation to make the prologue a *bespoke* set-piece (a custom
structure, a unique mob, a special Discord broadcast). That would be the orphaned gimmick. The
prologue is the existing M-I notice + one physical seed (the lit marker) + one detection flag +
one gated `recordOpened` ack. Everything else is reuse. Keep it small; its power is calibration,
not novelty.

---

## 3. DE-SLOP TEST — exemplar lines in-voice

The prologue authors **no new corpus prose** — it reuses `the-record-opens.md`. The only
genuinely-new player-facing strings are the ignition ack (canonical) and, if the precision gate
passes, the named first-naming inflection. Proof the seams write cold and in-register:

> ▒  the record is open. it was open before you.

*(the ignition ack — verbatim `voice.recordOpened()`, unchanged. confirms; does not explain;
implies it predates the question.)*

> one day kept. the one called {name} has not given back to the deep. it has been noted.

*(the named marquee — the `recordOpenedNamed` inflection, modeled on `voice.reportObserved`.
states a count and a fact. no "the darkness watched you," no "you should be afraid" — it says
what is true and stops.)*

> the living are written here, each by the name they answer to, and against each name a column
> is left open.

*(the un-named fallback — verbatim from `the-record-opens.md`. found-artifact register, passive,
no editorializing.)*

> nothing burned here when you left it. a small light is kept by the lectern now.

*(an OPTIONAL backstop tell line if a private message is ever attached to the second lit object
— flat, locative, the incongruity stated by the world's own silence around it. note the seed:
"is kept," the Kept-Light verb, planted inert.)*

What is **banned and absent here:** no "little did they know there was a watcher"; no "an eerie
glow"; no announced fear; no thematic bow; no three-adjective list. The lit candle is described
to players by *its existence in a place they know*, not by an adjective.

---

## 4. THREAD IT — where this lives so it is not an orphan

### Canon FACTs it adds / touches
- **Adds no new FACT.** It is the *delivery* of existing **FACT 1** (the record keeps a list of
  the living) and **FACT 2** (graded by laws no one was told), per `canon-spine §3`.
- **Touches/foreshadows FACT 14** (the buried "the record does not close at the rite… receives
  you" line) — inherited from `the-record-opens.md`, paid off in M-V.
- **One cross-surface artifact to log in `LORE-BIBLE.md §6` audit (INV-5):** the *physical lit
  marker* (`first_marker_01`) beside the first lectern — a world object that becomes
  Discord/rune-readable once the Rosetta is learned. This is **seed-ledger row 4** in
  `WEB-MASTER.md` (LIVE in the ledger; the audit-log entry is the open item). It is a second
  door onto FACT 3 (the rune script predates the group).

### Found-documents / records that carry it
- **`arc/lore/documents/the-record-opens.md`** — IS the prologue's text (the prologue is its
  *staging*, not a rewrite). Its `foreshadows`/`links_to` front-matter (FACT 14;
  `observed-warned-left-at-threshold`, `counted-them-in-the-dark`) are the prologue's
  forward-thread.
- **`learn-them-as-we-learned-them.md`** (the Rosetta) — must include the one glyph the lit
  marker is carved with, so M-II re-reads the marker (§5 payoff).
- **`observed-warned-left-at-threshold.md`** (Orin) — already the M-IV payoff of the first
  naming; the prologue inherits this link unchanged.

### NPC / Watcher voice lines
- `voice.recordOpened()` — the ignition ack (**built**, verbatim).
- `voice.recordOpenedNamed(name, …)` — the named first-naming inflection (**GAP — referenced
  by `prologue.ts` `reportVoiceKey` but ABSENT from `voice.ts`**; add in register, modeled on
  `reportObserved`).
- `voice.reportObserved(name, days, customPhrase(key))` — the fallback shape for the named
  clause (**built**).

### Cipher(s) / puzzle(s) it expresses
- The prologue itself is **pre-literate** by design — readable as *incongruity* before any
  cipher is learned (that is what makes it ignition). It carries **no solvable cipher on day
  one.**
- Its lit-marker seed is carved in the **rune alphabet** (`forge/runes.ts`), unreadable until
  the Rosetta. When re-read in M-II it resolves via the simplest transform — **`a1z26`** (or
  **`atbash`**) on a one-word glyph (the marker spells `KEPT` or `BEGUN`) — reusing an existing
  forge cipher, no new transform. Chosen so the re-read is a quick "oh," not a fresh hard puzzle.

### Beat classes / listeners / tables / seed rows / sites.yml / voice keys
- **Beat classes (all existing):** `LecternFillBeat` (the report), `SmallStructureBeat` or a
  single block-set for the lit marker, `PrivateSoundBeat`/`TorchGutterBeat` (the §1.4 backstop).
  All under `Reveal`/`mutateWhenUnwitnessed`.
- **Listeners:** `BaseDetector` + heatmap accumulation (retarget placement to the hot path-cell
  in the base); `PlayerInteractEvent` on the protected lectern (read-detector); the existing
  `messageCreate` scan (Discord ignition detector). **One new narrow Java listener:**
  `IgnitionListener` that sets `arc_state.flags.prologue_ignited` on either detector
  (**GAP — not built; no `*Ignition*` class exists in `plugin/`**).
- **sites.yml:** `first_report_lectern_01` (**built**, gains runtime retarget to base hot-cell)
  and `first_marker_01` (**built** — already present as `type: structure`, enabled, null coords,
  carrying the prologue marker glyph + the six UNKEPT maker's-marks + the `kept here before you`
  plant per its comment).
- **Showrunner (TS):**
  - `prologue.ts` `decidePrologue` (**built** — the pure ignition policy + precision gate + one-
    shot ack key).
  - `types.ts` `PrologueGate { curatorialAllowed }` folded onto `Snapshot.prologue?` (**built**).
  - `decide.ts` curatorial-drip suppression while `!curatorialAllowed` (**built**, lines 76–84).
  - `run.ts` folds `gates.prologue` onto the snapshot and persists `prologue` (**built**,
    lines 23/29/58).
  - **GAPS:** (a) `snapshot.ts` does not yet surface `prologueIgnited`/`acked`/`overwhelmingSignal`
    inputs — the run wrapper must read the flag + the one-shot-ack state + the overwhelming-signal
    measurement and feed `decidePrologue`; (b) the one-shot `recordOpened` ack emission (on
    `postAck`) is not yet wired into `apply.ts`/the bot; (c) `prologue.selftest.ts` does not exist
    (the doc-string says it imports `prologue.ts` with nothing — add the self-test).
- **Tables / flags:** `arc_state.flags.prologue_ignited` (boolean, read by showrunner + plugin);
  an `acked` one-shot guard (a second flag or a `beat_queue`/`event_log` check); the overwhelming-
  signal read off `dossiers`/`custom_compliance`.
- **Voice keys:** `recordOpened` (built) + `recordOpenedNamed` (GAP).

### Consistency invariants it must satisfy (immersion-blueprint §3)
- **INV-1/INV-2:** ignition ack is verbatim `voice.ts`; the lectern payload is the existing
  corpus register; nothing breaks character.
- **INV-3/INV-5:** the lit marker is a cross-surface seed (world → Discord re-read in M-II);
  logged in the FACT-web audit as a second door (the one open audit item).
- **INV-6:** the named report is `pending` (curatorial) in CONFIRM; the ambient seed is
  `approved`; the timing difference is invisible to players.
- **INV-7:** after ignition, *silence resumes* — the prologue is the one calibrated exception,
  then the world goes quiet again (the `curatorialAllowed` flip is the mechanical proof).

---

## 5. PLANT THE PAYOFF — the "oh, that's what that was for" seed

**Plant (Movement I, day one — inert/ambiguous):** beside the first report lectern sits **one
lit object that was not there before** — the `first_marker_01` carved stone with a single light
source, bearing a single rune-glyph nobody can read yet. On day one it reads only as
*incongruity*: "something is in my base." It is the ignition trigger. It is **not legible**; it
carries no decodable meaning to a pre-literate group. It is also, quietly, the first appearance
of the **Kept Light** motif — a fire that burns where no one lit one. (This is **WEB-MASTER
seed-ledger row 4**, status LIVE.)

**Payoff (Movement II, after the Rosetta — `learn-them-as-we-learned-them.md`):** once the group
learns the rune alphabet, **the marker becomes readable.** Its single glyph resolves (via
`a1z26`/`atbash`) to one word — `KEPT` (or `BEGUN`). The group realizes the very first anomaly,
the thing that ignited the whole investigation, was **already written in the script** they have
only now learned — *"the first notice was a record entry all along; the land was speaking to us
in week zero, we just couldn't read it."* This binds the prologue to FACT 3 (there were keepers
before; the customs/script are theirs) and to the Kept Light thread that re-pays in M-III (the
Undercroft's eternal light) and M-V (the rite's light, the world-flip to *kept*).

**Web integrity:** no payoff without a plant (the marker is placed in M-I); no plant without a
payoff (it pays off in M-II re-read, and its Kept-Light motif re-pays in M-III/V). The single
glyph must be added to the Rosetta's taught set so the re-read is *earnable*, not a dead artifact.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

**Lives in:** **Movement I** (the opening beat, Days 1–2), with a **re-read tail in Movement II**
(the marker becomes legible) and inherited forward-links into **M-IV** (Orin biography) and
**M-V** (FACT 14 / Kept Light).

**Depends on (must exist first):**
- `BaseDetector` + heatmap accumulation (retarget placement to the live base hot-cell) — *built*.
- `LecternFillBeat` + `Reveal`/`mutateWhenUnwitnessed` + `Placement` validators — *built*.
- `dossiers`/`custom_compliance` overwhelming-signal check (for the conditional name) — *data
  built*; the showrunner consumer that sets `overwhelmingSignal`/`signalName` — **pending (GAP)**.
- `voice.recordOpened` + `#the-record` `messageCreate` scan — *built*.
- `prologue.ts` decider + `decide.ts` guard + `run.ts` fold + `types.ts PrologueGate` — *built*.
- `IgnitionListener` (Java) + the snapshot ignition inputs + the one-shot ack emission +
  `prologue.selftest.ts` + the `recordOpenedNamed` voice key — **the four+ small GAPS.**
- The Rosetta carrying the marker's glyph — *exists, needs one glyph added* (M-II payoff only).

**Depended on by:**
- **The entire downstream engine's engagement** — clue-drip, Whisper economy, keeper-stone web
  all assume a group that is *already investigating*. The prologue produces that group; it is
  the engagement precondition for Movement II onward. (Mechanically enforced: `decide.ts` will
  not drip a curatorial clue until `prologue.curatorialAllowed`.)
- The **YouTube cut's opening** (immersion-blueprint §5 "first-naming reaction," gold #1) — the
  prologue is literally the first reaction the edit hunts for.

**Priority:** **P0 — vertical slice.** Not depth or arc-spine garnish; it is the ignition
without which the slice has no audience. Cheapest high-leverage P0 in the design (almost entirely
reuse + the spine already ships). It must be in the *first* playtest, because the first thing to
validate is not "is Movement III good" but "**will they notice at all.**" If the prologue does
not ignite, nothing else gets tested.

---

## 7. BUILD STATE + REMAINING SEAMS (reuse-first)

**BUILT (spine ships):**
- `discord/src/showrunner/prologue.ts` — `decidePrologue`: pure ignition policy, precision gate,
  one-shot ack key.
- `discord/src/showrunner/types.ts` — `PrologueGate` + `Snapshot.prologue?`.
- `discord/src/showrunner/decide.ts` — curatorial-drip suppression while `!curatorialAllowed`
  (lines 76–84); gifts unaffected.
- `discord/src/showrunner/run.ts` — folds `gates.prologue` onto the snapshot + persists it.
- `plugin/.../sites.yml` — `first_report_lectern_01` + `first_marker_01` (enabled, null coords).
- `voice.recordOpened()`, `voice.reportObserved(...)`.

**REMAINING SEAMS (small, ordered):**
1. **`voice.recordOpenedNamed(name, …)`** — add to `voice.ts` in register (modeled on
   `reportObserved`). Without it the decider's named branch has no string. (The un-named branch
   ships safely first.)
2. **`IgnitionListener` (Java)** — new narrow listener: set `arc_state.flags.prologue_ignited`
   on `PlayerInteractEvent`(first lectern) OR human `#the-record` post. No `*Ignition*` class
   exists today.
3. **Snapshot ignition inputs** — surface `prologueIgnited`, the one-shot `acked` state, and the
   `overwhelmingSignal`/`signalName` measurement (off `dossiers`/`custom_compliance`) so the run
   wrapper can feed `decidePrologue` its full `PrologueInput` (today only `curatorialAllowed` is
   folded).
4. **One-shot ack emission** — wire `postAck` → post `voice.recordOpened()` once in `apply.ts`/
   the bot, gated on a human `#the-record` post; idempotent via the `acked` guard.
5. **`prologue.selftest.ts`** — add the pure self-test the doc-string promises (dormant → ignited
   → acknowledged; named vs un-named precision gate).
6. **In-world lit-marker placement** — place `first_marker_01` via existing block-set/
   `SmallStructureBeat` under `Reveal`; carve it with one `forge/runes.ts` glyph; retarget
   `first_report_lectern_01` to the base hot-cell at placement time.
7. **Rosetta glyph** — add the marker's glyph to `learn-them-as-we-learned-them.md`'s taught set
   (M-II payoff legibility).
8. **FACT-web audit log** — record the `first_marker_01` cross-surface seed in `LORE-BIBLE.md §6`
   (INV-5); it is WEB-MASTER seed-ledger row 4.
