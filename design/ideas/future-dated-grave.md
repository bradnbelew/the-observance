# Idea Treatment — The Future-Dated Grave

> A grave bearing a living player's username and a future date, discovered by the
> group. Maximally unsettling, near-zero cost. Engine mapping: `SmallStructureBeat`
> (the mound + headstone) + `SignWriteBeat` (the carved name + date). **The carved
> date is not flavor — it is the single Accepting instant (WEB-MASTER §8.2), shared
> with the Record-website timestamp and the summons `not_before`. The grave is a
> promise the engine keeps, never an empty threat.**

Status: **KEEP (scaled — one grave, not a graveyard).** Priority: **P1 (arc-spine),
with a P0 single-grave vertical-slice cut.** Lives Movement II (plant) → Movement V
(payoff). FACT 13b is **canon** (no longer a candidate); the producer `grave.ts` and
the founder margin doc already exist. This treatment reconciles the idea to the
synthesized spine and names the exact remaining build work.

---

## 0. The one-sentence spine

The world carves a grave with your name and a date. On that date, nothing dies —
the grave **opens from the inside**: it is revealed to have been your *deposit slot*
in the rite all along (FACT 13), the headstone restated as the keeper-stone you will
become (FACT 15). The threat re-reads, in one move, as an invitation that was never
a threat. "Oh — that is what that was for."

A death-flavored object that pays off as **induction**, not death. It is the sealed
twist (FACT 15) expressed as a single physical artifact, foreshadowed in plain sight
(FACT 13b: *the stone is cut before the keeper is kept*), resolved by event.

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 What the group physically finds (Movement II)

Off the main descent path, near `the_threshold` (Orin's lintel) — a place the group
already has reason to walk — a low mound and a single headstone stand where there was
bare floor the session before. Reveal-disciplined: discovered, never witnessed
appearing (`SmallStructureBeat` only pastes when the base is unwitnessed;
`SignWriteBeat` only writes when the sign block is hidden; both re-validate the
footprint at mutation time and abort silently if the world changed).

The structure (inline cells, ~9 blocks, well under the 256 cap — no FAWE dependency,
Path-A safe):
- a 2×1 mound of `COARSE_DIRT` / `PODZOL`,
- a `COBBLED_DEEPSLATE_WALL` foot-stub,
- one `OAK_SIGN` (waxed, protected) as the headstone face.

The headstone is **carved in the rune alphabet** (`runes.ts` / forge), not Latin
letters — so on first read it is *illegible*. It is one more carved thing in a world
full of carved things. A group that has earned the `rune_rosetta` literacy can decode
the name; the date's digits use the **digit-glyphs** the reckoning stone teaches, so a
group must have earned **both** literacies to fully read it (INV-14: the date is
*read*, never typed as an answer). A group that hasn't will photograph it and move on,
unsettled but not yet understanding. This is the first mercy of the design: **the
grave does not announce itself.** It waits to be read, like everything the record keeps.

When decoded, line for line, the sign reads (the record's hand, FACT 13b register):

```
[ keeper-rune mark ]
the one called <NAME>      ← one living, ACTIVE player's exact username, rune-carved
kept — not yet
< DATE >                   ← the single Accepting instant, as digit-glyphs
```

The name is **always** a real, currently-active player (precision law: pulled from a
`SignalSnapshot.name()`-grounded read of a player the tracker is actively measuring;
`grave.ts`'s `GraveCandidate.active` gate — a nameless or inactive subject → **no
grave**, it requeues). The date is **not authored and not random**: it is the single
Accepting instant injected by the showrunner (`GraveInput.acceptingInstantMs`), the
same instant the summons `not_before` and the Record website timestamp share. The
grave is silently telling the truth about *when the arc resolves*, days before the
group could know that.

### 1.2 Why it is not an empty threat — the engine contract (load-bearing)

The grave is a **two-row idempotent transaction** (`grave.ts` `decideGrave`),
authored so the plant can never exist without its payoff:

1. **Row A — the carve, fires now.** `small_structure` + `sign_write`, targeting the
   grave site. Carves name + the Accepting-instant date. Marks `{carved:true}`.
2. **Row B — the open, fires on the carved date.** A `sign_write` rewrite (+ a slot
   reveal) whose firing time **is** the Accepting instant. `decideGrave` only emits
   `open` when `carved && !opened && nowMs >= acceptingInstantMs`. Marks `{opened:true}`.

The carve and the open read the **same subject** (`grave.ts` passes the carved subject
through to the open row). The date on the stone and the open's due-time are the same
injected value — they cannot drift by construction.

> **ENGINEERING GATE — STILL UNWIRED, CLEAR BEFORE BUILD (verified 2026-06-25):**
> `SupabaseClient.fetchActionableBeats` (line 168) fetches `status=eq.approved` only —
> it does **NOT** filter `not_before<=now()`. So a Row B inserted as
> `approved, not_before=date` would fire **immediately**, not on its date. Pick one:
> - **(preferred) add `&not_before=lte.{now-iso}`** to the `fetchActionableBeats`
>   query (one line; makes every queued beat honor its date — generally useful). Row B
>   inserts as `status='approved', not_before=acceptingInstant`.
> - **(no-engine-change fallback)** insert Row B as `status='queued'`; let the
>   showrunner tick flip it to `approved` when `now >= acceptingInstantMs`. The
>   showrunner already owns time-gated drips. This is in fact what `grave.run.ts`
>   would do natively (it re-runs `decideGrave` each tick and only emits `open` when
>   due), so the **fallback needs no engine change at all** — the producer is the gate.
>
> **Do not author the grave's open row until this is settled.** An un-honored date is
> the exact orphan this idea is built to avoid. Ship `grave.selftest.ts` asserting:
> (a) no `open` row before the instant, (b) exactly one `open` row at/after it,
> (c) restart mid-arc re-derives the same single grave (high-water idempotency).

### 1.3 The decoy reading the group will (correctly, then wrongly) reach

A veteran ARG group reads "grave + my name + a date a week out" as **a death clock**.
Good. That fear is the engine of Movements III–IV: they over-keep the customs, never
sleep on the black moon, never wander alone — exactly the behavior the customs are
*for*. The grave converts dread into real keeping, not busywork. **The misread is the
mechanic.** (Warmth-under-dread law: the world is not lying to scare them; it states
the date of their acceptance plainly and lets them assume the worst, because that is
what people do. The founder margin literally argues this with itself in the corpus —
"the keepers call it the death-clock… it is not a clock counting down to a death.")

### 1.4 The payoff (Movement V) — the grave opens from the inside

On the Accepting instant, Row B fires at the grave site while unwitnessed. Three small
things, no death, no jumpscare:

1. The headstone **rewrites itself** (`sign_write`, same sign block) from
   `kept — not yet` to **`kept`**, and adds a deposit instruction in rune. The
   "not yet" was always a clock counting *toward keeping*, not toward dying.
2. The mound **opens**: the top `COARSE_DIRT` cell becomes a labelled deposit slot —
   a `BARREL` with a `TextDisplay` rune label (the same FACT 13 deposit-slot mechanic
   the altar uses). The grave **is** that player's rite-slot. The thing they feared was
   a hole for their body is a hole for their **offering**.
3. (Optional, P1) a private, precise receipt to the named player only
   (`PrivateMessageBeat`, grounded — only because the tracker measured this name). Not
   a taunt. A deposit instruction.

The recontextualization, stated nowhere, felt everywhere: the grave was the rite all
along; the date was the appointment; "kept" means **kept**, as the abandonment-era
reports say of the people who "did not depart — they were kept" (FACT 12). The
headstone with a living name *is* the keeper-stone they are about to become (FACT 15) —
a marker for whoever comes next. **The grave date == the rite date** is literal
(WEB-MASTER §8.2): when the summons comes "at the dark hour" and it is the date on the
stone, the plant clicks for the whole group at once.

### 1.5 The collective guard (non-negotiable)

Exactly **one** grave, **one** name — chosen by a neutral, explained rule, never a
"chosen one." Default rule: the **first player to earn the `rune_rosetta` literacy**
gets their name on the stone — because they are the one who *can read it*; a
self-justifying, non-judgmental pick. Fallback if read-order is unknowable: longest
active session this Movement (most present, not most virtuous). The accompanying lore
makes explicit that **all** active players will be kept — the one name is the **first**
name in a list the record is still writing (the receipt line carries this), never the
only one (INV-16: the carve must not let the group derive who is honored vs. violated).

> **Scaling decision (HOLD THE CUT):** the pitch invites a *graveyard* (every name).
> **Cut it.** A graveyard reads as a body-count threat (anti-collective, anti-warmth)
> and multiplies the precision risk N-fold. One grave + an explicit "still being
> written" list = same chill, none of the law-breaks. `grave.ts` already enforces one
> subject; do not regress this.

---

## 2. CRITIQUE — adversarial, honest

**R1 — Precision: wrong name = catastrophe.** If the carved name belongs to an absent
or mis-measured player, the most personal beat in the arc becomes a bug with their name
on it. *Mitigation (already coded):* `grave.ts` refuses to emit a `carve` row unless
`subject.active && subject.name` (`'no grounded active subject for the grave — never
guess one'`). The run wrapper must read a fresh `SignalSnapshot` (positive
`sessionPlaySeconds`, recent `lastLocationMs`) and copy the name **verbatim**. Same
grounding rule reports obey (canon §6.4).

**R2 — Un-honored date (the orphan risk) — LIVE.** Confirmed: `fetchActionableBeats`
does not gate on `not_before`. If Row B is inserted `approved` it fires at carve time,
collapsing the whole idea. *Mitigation:* §1.2 — wire the predicate **or** use the
showrunner-flip fallback (the producer is the gate); ship `grave.selftest.ts`. This is
the single highest-risk item; treat as P0 blocker for the open row.

**R3 — Reveal jank on a high-traffic spot.** A grave near `the_threshold` where 4–6
friends cluster may never find a globally-unwitnessed instant, so it silently never
appears (the known reveal-starvation problem). *Mitigation:* the disabled `grave_spur`
site already exists in `sites.yml` for exactly this — a side spur 20–40 blocks off the
threshold the group passes but doesn't camp. Flip `enabled: true` and set coords if the
threshold proves too busy in playtest. Footprint is 9 cells, so an unwitnessed instant
is cheap. Per-player reveal (`Reveal.isHiddenFrom` + `sendBlockChange`) is the further
fallback.

**R4 — The misread never corrects (worst-case on camera).** If a group is spooked
enough to *avoid* the grave entirely, they may never witness the open rewrite, and the
death-clock fear calcifies into "the ARG threatened us and nothing happened."
*Mitigation:* the payoff is **also** reachable by a second door — the Keeper NPC's
Movement-V node references "the stone that waits with your name," and `#the-record`
posts a dated rune-card of the same name+date (cross-surface truth). Resolution arrives
even if they avoid the spot. (Web rule: two doors.)

**R5 — Gimmick risk if the date points nowhere specific.** A date that just lands
"sometime in Movement V" is mushy. *Mitigation (already canon):* the date **is** the
single Accepting instant (WEB-MASTER §8.2), injected by the showrunner and shared with
the summons + website. It is the most-anchored date in the arc, not a free-floating one.

**R6 — Anti-slop on the carved lines.** "kept — not yet" risks reading as movie-poster
dread. *Mitigation:* it passes — it is a **ledger state**, not an emotion (the record
marks a not-yet-true fact the way it marks winters kept), and it stays lowercase, not
dread-caps. Guard the private receipt hard against taunting (§3).

**R7 — Latin-vs-rune legibility.** Carved in Latin it is a cheap jumpscare that spoils
itself instantly; the "waits to be read" mercy collapses. *Mitigation:* **rune-only**
name (`runes.ts`), digit-glyph date — consuming BOTH literacy gates, threading the
grave into the existing Rosetta structure instead of bolting on.

**What to CUT:** the graveyard (every name) — §1.5. **What to scale:** keep the date
window tight enough that the clock has real tension but resolves inside the ~2-week
arc; the Accepting instant already enforces this.

---

## 3. DE-SLOP — exemplar lines (in-voice, cold, concrete)

The headstone, decoded — the record's hand, ledgerlike, states a not-yet-true fact the
way it states winters:

```
the one called <NAME>.
kept — not yet.
when the dark hour comes round: <date>.
```

The open rewrite, on the date — one word changes; the iceberg does the work:

```
the one called <NAME>.
kept.
set down here what is yours to set down.
```

The private receipt to the named player — grounded, no threat, no emotion named, a
deposit instruction, not a death sentence:

```
you read it first. so it is your name first.
the others' names are not yet cut. they will be.
```

The founder margin already in the corpus (`we-cut-the-names-before-the-keeping.md`,
M1 plant) — a later hand correcting an earlier, damage gap, no melodrama:

```
do not read the date as the day they die. read it as the day they are kept.
the founders did not cut a death into the stone. they cut an appointment. [...]
```

(None state the twist; none emote; none use a banned construction. "kept — not yet" is
a record state. The later hand's "i cannot decide which is worse, only that they are
not the same thing" reads as a real artifact argument, not a thematic bow.)

---

## 4. THREAD IT — exactly where this lives (no orphans)

**Canon FACTs touched/added (canon-spine §3 / §3b):**
- **FACT 13b** (*the stone is cut before the keeper is kept*) — **the grave IS the
  primary surface of this canon fact.** Already registered (§3b); the founder margin is
  its second door. The misread *is* the mechanic; the date == the Accepting instant.
- **FACT 13** (rite needs a personal token per keeper) — the open is the mechanical
  first instance of the deposit slot.
- **FACT 14** (the record receives/keeps you) — the `kept` rewrite is FACT 14 in one word.
- **FACT 15** (acceptance = becoming a marker for whoever comes next) — the headstone
  with a living name *is* the next keeper-stone. Felt, never stated.
- **FACT 12** (the kept ones did not depart — they were kept) — the grave is the
  pre-cut stone FACT 12 grieves; the founder margin is a path to FACT 12.
- **FACT 10** (acceptance is a choice the land can refuse) — the Seventh's *cast-out*
  unwriting (the_unwriting) is the **inverse object**: a name *erased*, never kept.
  The future-dated grave (kept, dated) and the Seventh's grave (cast out, unwritten)
  rhyme — one promised, one refused. No new fact; strengthens FACT 10's web.

**Invariants it must obey:** INV-14 (date read, never typed), INV-16 (no per-player
honored/violated derivation — the single carve rhymes on "who could read first," never
a virtue ranking; must not co-locate with the `offline-skin` worn-skin apparition for
the same player/window), INV-12 (colors, never gates).

**Found-documents / records (exist or one-line additions):**
- `arc/lore/documents/we-cut-the-names-before-the-keeping.md` — **exists**, M1 plant.
- one abandonment-era report (passive voice, FACT 12) referencing "stones cut and
  waiting" — strengthen an existing FACT-12 report, no new doc needed.

**NPC / Watcher voice lines (`voice.ts` — DO NOT EXIST YET, must add):**
- `graveCarved(name, date)` and `graveOpened(name)` — the exact two `voiceKey`s
  `grave.ts` `GraveRow` already references. **`voice.ts` currently has neither** —
  adding them is required build work. Cold/ledger register, §3.
- `graveReceipt(name)` — optional private line (P1).
- the **Keeper** NPC gains a Movement-V node "the stone that waits with your name"
  (second door for the payoff, R4) — dialogue tree, not a new file.
- Discord: a dated rune-card in `#the-record` via `runes.ts` (cross-surface truth, R4).

**Cipher(s)/puzzle(s) — reuse, do not invent:**
- **rune substitution** (`runes.ts` alphabet) — the name is carved in it; reading
  requires the `rune_rosetta` literacy. No new cipher.
- the **date digits** use the digit-glyphs the reckoning stone teaches — the grave
  consumes BOTH existing literacy gates.
- (optional flavor) lightly **caesar**-shift the date digits (Vaun's cipher — "held
  back by a fixed amount") so the group must also recall the founding line held things
  back, tying the grave to Vaun (first keeper, first stone). Caesar is one of the 11
  built ciphers; no new code. INV-14 still holds (the date is read, not typed).

**Beat classes / listeners / tables / seed rows / sites.yml / producer:**
- Beats (all exist): `small_structure` (`SmallStructureBeat`, inline cells),
  `sign_write` (`SignWriteBeat`, rune lines), optional `private_message`
  (`PrivateMessageBeat`). The open reuses `sign_write` (rewrite + slot). **No new beat
  class.**
- Producer `discord/src/showrunner/grave.ts` — **exists** (pure `decideGrave`, two-row
  idempotent, grounded-active gate, INV-14 note). **Still to write:** `grave.run.ts`
  (I/O wrapper: pick active name, read injected Accepting instant, fire the beats, emit
  the rune-card, persist `{carved,opened}` marks) and `grave.selftest.ts` (imports
  `grave.ts` with nothing; asserts the R2 properties).
- `sites.yml` — `the_threshold` (enabled, P0 anchor) is the default; **`grave_spur`**
  (type `grave`, currently `enabled:false`) is the off-spur escape hatch for R3.
- table: a one-row grave state (subject name/uuid, carved bool, opened bool,
  accepting_instant) — or fold into existing showrunner state — for idempotency + the
  self-test high-water.

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for"

- **Plant (Movement I, inert/ambiguous):** the founder margin `we cut the names before
  the keeping` — "the stone is ready before the keeper is… do not read the date as the
  day they die." Read in M1 it is one more eerie founder line about ritual order. Names
  nothing, threatens nothing. It is *about gravestones* and no one knows it yet.
- **Plant (Movement II, the object):** the grave appears with a living name + the
  Accepting-instant date. The margin re-reads once (*oh — they cut ours*) but the group
  reads it as a death omen. Ambiguous on purpose.
- **Payoff (Movement V, on the date):** the grave opens as a deposit slot;
  `kept — not yet` → `kept`. The margin re-reads a **final** time and lands true: the
  stone was ready before the keeper because **becoming a keeper is the keeping** — the
  grave was never a grave, it was their place in the record (FACT 15). The date was the
  appointment for the Accepting, told days early, plainly, and disbelieved.

Three-beat chain, each re-reading the last: the margin (M1) ↔ the carve (M2) ↔ the open
(M5). No plant without payoff; no payoff without plant.

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES

- **Movement I:** founder margin doc (plant). *P2 (depth — the grave works without it,
  but it is the iceberg that makes the payoff land). Already authored.*
- **Movement II:** the grave is carved (Row A). *P1.* **Depends on:** `rune_rosetta` +
  reckoning digit-glyphs placed (literacy to read it); a grounded active player
  (`grave.ts` subject gate); the Accepting instant bound (`acceptingInstantMs != null`).
- **Movements III–IV:** the grave does nothing mechanically — it works on the group's
  *behavior* (the misread drives over-keeping). The Discord rune-card + Keeper node keep
  the second door open (R4).
- **Movement V:** the grave opens (Row B fires on the Accepting instant). *P1.*
  **Depends on:** the `not_before` gate settled (§1.2, R2 — the P0 blocker);
  `nowMs >= acceptingInstantMs`. **Depended on by:** FACT 13's deposit-slot mechanic
  gets its first and most personal instance; the FACT 15 landing is partly carried here.

**Vertical-slice cut (P0):** one grave, one grounded name, the Accepting instant set
2–3 days out (slice timescale), Row A now, Row B rewrite on the date. Proves the engine
contract (date honored), reveal discipline, grounding, and the re-read — the whole idea
minus the founder margin (already done, P2) and the Discord/Keeper second doors (P1).

---

## 7. Build order (current ground truth, 2026-06-25)

1. **Settle the `not_before` gate** (R2/§1.2): add `&not_before=lte.{now}` to
   `fetchActionableBeats`, OR confirm the showrunner-flip path and let `grave.run.ts`
   be the gate. Ship `grave.selftest.ts` asserting the three R2 properties.
2. **Add `voice.ts` keys** `graveCarved`, `graveOpened` (and optional `graveReceipt`) —
   the keys `grave.ts` already names but `voice.ts` lacks.
3. **Write `grave.run.ts`** (grounded active name, inject the Accepting instant, two-row
   transaction via `decideGrave`, emit the `#the-record` rune-card, persist marks).
4. Add the Keeper Movement-V node ("the stone that waits with your name") — second door.
5. P0 slice playtest (instant 2–3 days out) before committing the full date window.

Already done, do not redo: `grave.ts` (decider), FACT 13b (canon-spine §3b), the
founder margin doc, `grave_spur` site stub, `the_threshold` anchor.
