# Backlog — The Accepting Rite Sentinel Bridge (the finale wiring)

> **Source idea:** MASTER-PLAN P1.15 (item 15) — "The Accepting rite (P16) sentinel bridge."
> **Pitch:** the plugin posts the opaque normalized sentinel to the shared resolver ONLY after the
> custom listeners confirm simultaneous-bow + deposits + time window, so the terminal `main_beat`
> inherits the `solves` replay-guard. Satisfiability gates on ACTIVE players only. This is the climax
> wiring the divergent-endings selector branches on.
>
> **Status of this file:** RECONCILIATION + FINISHING SPEC. The detector spine
> (`AcceptingRiteListener.java`), the seed chain (`rite-tokens` → `accepting-crouch` →
> `record-receives`), the sentinel token + self-test, the AUTO/CONFIRM split, the manual
> `AcceptingTrigger.tsx`, and the fate-NEUTRAL contract all **already compile**. This file honors
> them and specifies ONLY the genuinely-unbuilt remainder: (a) the **deposit/time/active-only**
> conditions the pitch names but the listener does not yet enforce; (b) the **readyGate wiring**;
> (c) how the bridge interlocks with the as-built divergent-endings selector
> (`design/ideas/divergent-fates-endings.md`).
>
> **Read-first done:** cipher-web §5/P16 (lines 309–327, 651–658, 716–718, 873), arg-deepening §1.6,
> immersion M-V, MASTER-PLAN item 15 + R-pass, FINAL-REPORT, `AcceptingRiteListener.java`,
> `AcceptingTrigger.tsx`, `CustomComplianceListener.java`, `puzzles_seed.sql` rows
> `rite-tokens`/`pressure-glyph-walk`/`accepting-crouch`/`record-receives`, `config.yml rites:`,
> `sites.yml accepting_floor`, `divergent-fates-endings.md`.

---

## 0. THE ONE-LINE RECONCILIATION (what was already wrong, now fixed in design)

The pitch reads "simultaneous-bow **+ deposits + time window**" as if the **AcceptingRiteListener**
must itself verify deposits and the hour. **It must not — and mostly already does not.** The as-built
web already splits these across the seed chain:

- **Deposits** are their own puzzle, `rite-tokens` (a `main_beat`, sets `tokens_laid:true`, lights the
  six altar slots). It is reached by `rite-tokens`' upstream and is independently solved when the
  components sit in their frames.
- **The bow** is `accepting-crouch` — what the listener posts.
- **The hour ("time window")** is a property of the bow detection (`world.getTime()` band), per
  arg-deepening §1.6.

So "deposits" is **not** a precondition the listener re-validates by reading item frames; it is an
**arc_state precondition** (`tokens_laid:true`) that the bow's **readyGate** consumes. This is the
single reconciliation that makes the backlog item and the seed agree: the listener stays a *thin,
pure bow detector*; the deposit-state and threshold-state ride in through one fail-closed
`Supplier<Boolean> readyGate` it already has a constructor slot for. The genuinely-unbuilt work is the
**gate predicate** (deposits ∧ threshold ∧ hour ∧ active-quorum) and the **active-only quorum**, not a
new deposit listener.

---

## 1. EXPOUND — the full mechanic + story + mystery treatment

### 1.1 What the player group actually does (Movement V, ~Days 12–14)

By M-V the spine has delivered the group to the Accepting floor (`unbroken_light` site, type
`accepting_floor`) through the single Iss IV→V chain: the three-hands coop gate opened the Threshold,
the group walked the true coordinate. The floor is a FAWE-pasted altar room: six marked deposit
places — item frames / barrels at exact coords, each labelled with a `TextDisplay` rune naming its
component — and the Keeper NPC (Citizens2), woken, presiding.

The terminal sequence is four physical acts, in this order, none typeable:

1. **Wake the Keeper** (`NPCRightClickEvent` → `KeeperNpcBeat`). Dialogue confirms the group is at the
   last hinge and names what the altar wants — without ever spelling the bow.
2. **Lay the components** — the three named items (*the deep's first heart*, *an unbroken light*,
   *salt of the old keepers*) **plus one personal token per keeper** (six, earned at the expeditions).
   When the right item sits in the right frame, the `rite-tokens` puzzle resolves: `main_beat`, sets
   `tokens_laid:true`, fires the `reveal` beat that lights the six slots. This is the **deposit**
   condition — solved, recorded, idempotent.
3. **Wait for the hour** — the altar only accepts in the kept light (a `world.getTime()` band /
   moon phase). Before the hour, a bow does nothing; the Keeper's idle line marks the wait without
   announcing the number.
4. **Bow as one** — every ACTIVE player present on the floor crouches inside a short sync window. The
   `AcceptingRiteListener` detects the simultaneous bow, confirms the readyGate (Threshold open ∧
   deposits laid ∧ in the hour) is TRUE, posts the opaque `accepting-crouch` token to the shared
   `OracleResolver`. The resolver records the solve (inheriting the `solves` replay-guard), fires the
   `record-receives` chain. The world flips to **kept**; the hidden advancement toast seals it
   ("⟡ The record receives you"); the divergent-endings selector — reading already-tracked tallies
   over **active players only** — composes which of the five fates the M5 pass speaks.

### 1.2 Why the bridge exists at all (the mystery payoff)

The whole reason the climax is a *plugin-posted sentinel* and not a typed answer is the B-5 red-team
law: **the ending cannot be spoofed at a sign or in Discord; it must be PERFORMED, together.** Across
two weeks the group has typed dozens of decoded words at signs and in Discord. The last door takes no
word. That is the "oh" of the whole arc structure — Mara's book cipher resolved to "DESCEND AND BOW AT
THE UNBROKEN LIGHT," and FACT 13 ("the missing tool is YOU") has been sitting inert since M-IV. At the
altar the group discovers the answer is not a string at all; it is six bodies bowing in the same second
in the right light. The sentinel token (`k7q2m9 x4r8p3 …`) is the engine's proof that *this specific
performance happened*, byte-matched to the seed row by a build-time self-test so the climax can never
silently fail or be guessed.

### 1.3 How it plays across the 5-movement arc

- **M-I (Days 1–5):** invisible. The bow gesture itself is taught small and ambiguous via the
  `bow_marker` sites (`CustomComplianceListener`) — players learn crouching *is honored somewhere*
  without knowing it is the finale key. Seed of the payoff (§5).
- **M-II–III:** the personal tokens are earned at the six keeper-stone expeditions; the descent into
  the Undercroft literalizes "descend." Components are gathered. None of this names the rite.
- **M-IV:** the three-hands coop gate + the true walk. `tokens_laid` is still false. The readyGate is
  fail-closed: a bow on the floor now does nothing (silently, like a miss).
- **M-V:** the four acts above. The bridge is live only here, because `accepting-crouch` /
  `record-receives` are flipped `active=true` at go-live and the readyGate's preconditions land.

---

## 2. CRITIQUE — adversarial, honest

### R-1 (HIGH) — "deposits + time window in the listener" tempts a fat, impure detector.
If we literally make `AcceptingRiteListener` read `ItemFrame`/`Barrel` contents and `world.getTime()`
inline, it becomes a stateful main-thread world-reader on a high-frequency event
(`PlayerToggleSneakEvent` fires constantly), violating the thin-pure-detector discipline and risking
TPS hitches during the filmed take.
**Mitigation (adopted):** deposits stay the `rite-tokens` puzzle (already built); the time band is read
once inside the gate predicate, not per-sneak in the hot path's early returns. The listener's hot path
stays: site-check → quorum → all-sneaking → **readyGate** → rate-limit → async post. The readyGate is a
cheap cached boolean (refreshed by the existing arc-state poll), not a live frame scan. **Net: do NOT
add a deposit/time scan to the listener. Wire them through the gate.**

### R-2 (HIGH) — quorum default 6 vs. "ACTIVE players only" is a real contradiction on camera.
`config.yml` sets `quorum: 6` (cast size). But the collective law says: never punish the group for an
absent member — gate on ACTIVE players only. If one of six is offline the night of the shoot, a
fixed quorum of 6 makes the climax **unfireable**, and worse, two stragglers could in theory meet a
low quorum. The current listener uses `present.size() < quorum` against a static config int — it has
**no concept of "active."**
**Mitigation (must build):** quorum becomes `min(configQuorum, activeRoster)` where `activeRoster` =
players seen in a recent window (reuse the dossier/session notion already tracked). The gate is:
*every ACTIVE player who is currently on the floor is bowing, AND the count of bowing-active ≥ the
active roster* — i.e. "everyone who is playing this arc, who is here, bowed." An offline sixth never
blocks; a two-person sub-group never satisfies because the active roster is > 2. This is the single
sharpest fix and it is the literal text of P1.15 ("Gate satisfiability on ACTIVE players only").

### R-3 (MED) — readyGate currently unwired → finale fireable too early.
The constructor overload for `readyGate` exists, but the registration in `ObservancePlugin.java`
(lines 328–334) calls the **legacy constructor with no gate** → `isReady()` returns `true` always.
Today, the moment `accepting-crouch` is flipped active, six people bowing **anywhere on the floor**
fires the climax even if the Threshold isn't open and nothing was deposited.
**Mitigation (must build):** wire the `readyGate` overload at registration with a supplier reading
arc_state: `threshold_open ∧ tokens_laid ∧ inHour()`. Fail-closed is already coded; we only supply the
predicate.

### R-4 (MED) — "time window" can soft-lock the shoot.
If the hour band is narrow and the group is mid-take when it closes, the bow stops working with no
tell — feels like a bug on camera.
**Mitigation:** (a) make the band generous (a multi-thousand-tick window, config-driven
`rites.accepting.hour-window`); (b) the `AcceptingTrigger.tsx` console path bypasses the hour entirely
(it already routes through the approval gate as `pending`), so the director always has a clean manual
fire for the climactic shoot. Keep the hour as *flavor + anti-trivial-early-fire*, not a hard shoot
dependency.

### R-5 (LOW) — orphan risk: the bow gesture taught in M-I could read as a gimmick if never re-used.
**Mitigation:** it IS re-used — `bow_marker` honored bows in M-I are the plant; the synchronized bow is
the payoff (§5). The COHERENCE-AUDIT requirement is satisfied: the mechanic has a narrative home in
both directions.

### Verdict on scope
**KEEP — scaled correctly by reconciliation.** Do NOT build a deposit/time listener (cut that reading
of the pitch). DO build: the **active-only quorum** and the **readyGate predicate wiring**. Everything
else already compiles and must be honored, not re-invented.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (cold, plain, concrete)

Keeper, at the altar, before the hour (idle):

> The slots are full. The light is not right yet. Wait.

Keeper, the hour having come (one line, no flourish):

> Now the light holds. Do the thing you were told and cannot read.

Watcher record line, posted to Discord after the bow resolves (register: it counts, it does not emote):

> Six bowed. One light. The record is closed.

Failed bow before the gate (NO line is spoken — silence is the tell; the iceberg). If a diegetic
near-miss line is wanted at the console only:

> Five of six. The sixth was standing.

All four: no banned constructions, no named emotions, no thematic bow, declarative, concrete count.

---

## 4. THREAD IT — exactly where this must appear (no orphans)

### Canon-spine FACTs / INV it touches or adds
- **Consumes FACT 13** ("the missing tool is YOU" — `rite-tokens` row comment) — the bridge is its
  payoff: the tool is the group's bodies, not a string.
- **Consumes FACT 11/12** (the one kept light; "the kept ones did not depart — they were kept") — the
  hour band IS "the kept light"; the world-flip IS "kept."
- **Honors INV-11** (collective judgment, no chosen one) — listener is fate-NEUTRAL; selector reads
  active-only tallies. Already enforced; the bridge must not regress it.
- **ADDS one INV** (proposed): *INV-ACC-ACTIVE* — "the Accepting's satisfiability quorum is computed
  over ACTIVE players only; an absent member never blocks and never elects." Record in canon-spine so
  any future change to quorum is checked against it.

### Found-documents / journals that must foreshadow it (already or to add)
- Mara's book cipher → "DESCEND AND BOW AT THE UNBROKEN LIGHT" (built) — the textual seed of the bow.
- Buried line F14 "we left, and the light was kept" — the "kept" verb of the flip (immersion M-V map).
- **TODO (small):** one Keeper-stone fragment may hint "the last word is no word" — only if a slot
  exists; do NOT invent a document to carry it if the dialogue node already does (COHERENCE: no
  orphaned lore). Prefer delivering it as a `KeeperNpcBeat` dialogue node (already the M-V presenter).

### NPC / Watcher voice lines (voice keys)
- `oracleMainBeat` — already the `accepting-crouch` and `record-receives` `voice_key`.
- Keeper M-V dialogue nodes — in `KeeperNpcBeat` / `voice.ts`; carry the "wait / now" hour cue (§3).
- **No new top-level voice key required** for the bridge itself; the close prose is owned by the single
  M5 composer (`divergent-fates-endings.md` §1.2), never by this listener.

### Ciphers / puzzles that express it
- **`bookCipher`** (Mara, `stone-mara` / `page-line-word`) — names the rite ("descend and bow").
- The bow itself is **`[NO CODE]` P16** — performed, not a cipher. The **two in-roads** are
  `accepting-crouch` (the true walk delivers the group) and **`pressure-glyph-walk`** (the
  "do-not-decode-walk-it" side door, already a seed row). No new cipher is consumed; the bridge's job
  is to make the *performed* answer inherit the same `solves`/replay-guard the 11 ciphers use.

### Beat classes / listeners / tables / seed rows / sites / voice keys that realize it
- **Listener:** `signal/listener/AcceptingRiteListener.java` (built; needs active-quorum + readyGate
  wiring).
- **Beat:** `record-receives` → `beats/lib` unlock beat (`door_open`); the world-flip + advancement
  toast. `KeeperNpcBeat` presides.
- **Tables/rows:** `puzzles` rows `rite-tokens`, `pressure-glyph-walk`, `accepting-crouch`,
  `record-receives` (all in `puzzles_seed.sql`); `arc_state` flags `threshold_open`, `tokens_laid`,
  `bowed_as_one`.
- **Site:** `sites.yml` `unbroken_light` (type `accepting_floor`).
- **Config:** `config.yml rites.accepting.*` (token, quorum, cooldown; ADD `hour-window`).
- **Console:** `dashboard/src/components/author/AcceptingTrigger.tsx` (built manual fire-path).
- **Self-test:** `riteTokenSelfTest` + `noLeakedSentinelSelfTest` (built; keep green).
- **Selector (downstream, separate file):** `decideFate` / `fate.ts` (per `divergent-fates-endings.md`)
  — the bridge feeds it `bowed_as_one` + active roster; it never branches here.

### NEW code symbols the unbuilt remainder needs (name them)
1. `AcceptingRiteListener`: replace static `quorum` compare with an **active-roster** computation.
   New collaborator field `Supplier<java.util.Set<java.util.UUID>> activeRoster` (or
   `IntSupplier activeCount`) injected at construction; gate becomes
   `bowingActiveOnFloor == activeOnFloor && activeOnFloor >= effectiveQuorum`, where
   `effectiveQuorum = min(configQuorum, activeRosterSize)`.
2. New private `boolean isActive(Player p)` helper (reads the injected roster), and
   `List<Player> activePlayersInSite(...)` replacing `playersInSite(...)` for the quorum test.
3. `ObservancePlugin#registerRites()` (lines 327–335): switch to the **readyGate overload**, supplying
   `Supplier<Boolean> readyGate = () -> arc.flag("threshold_open") && arc.flag("tokens_laid") && inHour()`
   and the `activeRoster` supplier from the existing session/dossier tracker.
4. New `boolean inHour()` predicate (config `rites.accepting.hour-window` low/high tick band) — a tiny
   pure util, NOT read per-sneak in the hot path (read inside the gate supplier).
5. Config additions in `config.yml`: `rites.accepting.hour-window: {low, high}` and an explicit
   `rites.accepting.active-only: true` toggle (default true) for the kill-switch ethos.
6. `arc/_SEALED` / `canon-spine`: add **INV-ACC-ACTIVE** text.

> None of the above re-invents finished code — they are additive: one constructor arg, one gate
> supplier, one config block, one INV line.

---

## 5. PLANT THE PAYOFF — the "oh, that is what that was for" seed

**Plant (M-I, inert/ambiguous):** the `bow_marker` sites. Early, a lone player crouching at a small
marked stone gets a quiet, unexplained acknowledgement (a `CUSTOM_BOW` honored signal — no message, no
toast; at most a Watcher note that "a bow was kept"). It reads as ambient flavor / one of many
"the land notices small acts" beats. Nobody is told a bow is a *key*. It is also seeded textually by
Mara's cipher ("…AND BOW…") in M-III, which at that point parses as a posture of deference, not a
mechanic.

**Payoff (M-V, the click):** at the altar, FACT 13 ("the missing tool is YOU") and the un-typeable last
door converge. The group realizes the gesture they were rewarded for alone, weeks ago, is the *only*
input that opens the finale — and only when ALL of them do it at once, in the kept light. The inert M-I
bow re-reads as training. The "oh" is structural: the arc taught its own ending in its first movement
and called it ambient.

**Ledger entry (for the seed/payoff tracker):**
`SEED bow-as-key (M-I bow_marker + M-III Mara "and bow") → PAYOFF M-V accepting-crouch synchronized
group bow. Inert until the altar. No payoff without this plant; the plant has no other payoff.`

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES

- **Lives in:** Movement V (Act 3, Days ~12–14). The plant lives in Movement I; the textual foreshadow
  in Movement III.
- **Depends on (upstream):**
  - `rite-tokens` solved → `tokens_laid:true` (the deposit precondition).
  - three-hands coop gate + true walk → `threshold_open:true`.
  - `accepting-crouch` / `record-receives` flipped `active=true` at go-live.
  - an **active-roster** source (existing session/dossier tracker) for the quorum.
  - `riteTokenSelfTest` green (token byte-match).
- **Depended on by (downstream):**
  - `record-receives` terminal beat (world-flip + advancement toast).
  - the **divergent-endings selector** (`fate.ts` / `decideFate`) — consumes `bowed_as_one` + active
    tallies; composes the M5 close. The bridge is the trigger the selector branches on.
- **Priority:** **P1 (arc-spine).** It is the climax; without it M-V does not resolve. The detector
  spine is already P0-complete (built); the *unbuilt remainder* (active-only quorum + readyGate
  wiring + hour band) is **P1** — required for a coherent, non-early-firing, fair finale. The hour band
  alone is the only P2-ish piece (pure flavor + anti-trivial; the manual trigger covers the shoot).

---

## 7. THE GENUINELY-UNBUILT REMAINDER — precise build list

1. **Active-only quorum** in `AcceptingRiteListener` (R-2): inject `activeRoster`; compute
   `effectiveQuorum = min(configQuorum, activeRosterSize)`; require every active-on-floor player bowing
   AND `activeOnFloor >= effectiveQuorum`. Add `INV-ACC-ACTIVE` to canon-spine.
2. **Wire the readyGate** in `ObservancePlugin` (R-3): use the overload; supplier =
   `threshold_open ∧ tokens_laid ∧ inHour()`, fail-closed (already coded).
3. **`inHour()` + config `hour-window`** (R-4): generous band; read inside the gate supplier, not the
   hot path. Manual `AcceptingTrigger` bypasses it.
4. **Keep self-tests green:** `riteTokenSelfTest`, `noLeakedSentinelSelfTest` — no change, just verify
   after edits.
5. **Honor, do not touch:** the seed chain, the opaque token, the fate-neutral contract, the AUTO/
   CONFIRM split, the manual trigger. These compile.

Everything else the pitch implied (a deposit-reading listener) is **explicitly CUT** as a
mis-reading — deposits are the `rite-tokens` puzzle, surfaced to the bow via the gate.
