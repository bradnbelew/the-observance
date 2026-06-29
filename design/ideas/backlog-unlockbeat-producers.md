# Backlog Treatment — UnlockBeat Producer Rows (solving visibly opens the world)

> MASTER-PLAN P1.9 / R6 · immersion-blueprint §7 · resolves "dispatcher built; needs authored producer rows."
> Status of this doc: build-ready treatment. **No Java change to `UnlockBeat` itself.** One small
> producer beat (`reveal`) and three payload-contract fixes are the genuinely-unbuilt remainder.

---

## 0. ONE-PARAGRAPH FRAME

`UnlockBeat` (`plugin/.../beats/lib/UnlockBeat.java`) is a pure dispatcher: a solved puzzle's
`outcome_payload.beat = {type:"unlock", payload:{step, step_payload}}` is enqueued `status:'approved'`
by `resolve.ts → applyOutcome → enqueueOracleBeat` (`db/repo.ts:517`), and on the plugin's next poll
the dispatcher looks up `library.get(step)` and re-runs that beat with `step_payload` as its request.
This is the reward loop that makes the clue web *physical*: a glyph read at a stone doesn't just print
a Watcher line, it opens a door, drops an advancement toast in the corner, rebuilds a room, or lands a
private word. The dispatcher and four of its five target producers (`door_open`, `advancement_toast`,
`small_structure`, `private_message`) **already compile and are registered** (`BeatLibrary.java:57-89`).
The work is (a) the missing fifth producer `reveal`, (b) three payload-key contract fixes so the seed
rows actually fire instead of silently `skipped`, and (c) reconciling the existing seed's already-authored
`unlock` rows with the new web so the backlog and the new mystery are **one** thing.

---

## 1. EXPOUND — the full mechanic + story across the 5-movement / ~2-week arc

### 1.1 What the player experiences
Every other surface in this ARG *speaks* (the Watcher answers, a Discord card flips, a journal page
appears). The unlock beat is the only surface that *acts on the shared world* as a direct, earned
consequence of a solve — and it is the difference between "the bot told me I was right" and "the wall
I'd walked past for a week just opened." The five `step` kinds are a deliberate ladder of escalating
world-permanence:

| step | what the group sees | reversible? | permanence register |
|---|---|---|---|
| `private_message` | a title/action-bar word lands for the solver only | yes (fades) | the most intimate, most deniable |
| `advancement_toast` | the corner toast fires ("the record notes you") | n/a (a record entry) | acknowledgement, not a door |
| `door_open` | a door/gate/trapdoor stands open, out of sight | yes (auto/queued close) | a passage earned |
| `reveal` | a marker, slot, fragment, or sealed face changes state | yes (block-state flip) | a thing now legible that wasn't |
| `small_structure` | a set-piece is present that wasn't (FAWE A→B paste) | yes (swap back) | the heaviest — the world rebuilt |

All five obey the anti-jank reveal law inherited from `AbstractBeat.mutateWhenUnwitnessed` /
`util/Reveal.java`: **nothing is witnessed appearing.** The door is found open; the structure is found
built; the toast is the one thing allowed to be instantaneous because it is a UI corner element, not a
world mutation. The solver "earned it," so per the AUTO↔CONFIRM split (`cipher-web.md` ~§650, ~§720)
the beat fires `status:'approved'` with no human gate — the player never waits on the showrunner.

### 1.2 How it lays across the movements (tracing the EXISTING seed + the new web)
The seed already authors the producer rows; this treatment honors them and names the remainder. The
arc-shape of the unlock beats is itself a told story — the world gives back *less private, more
permanent* changes as the group earns deeper standing:

- **Movement I — The Notice.** First unlock is the gentlest: `rosetta-ring` and its runes-free twin
  `a1z26-tick-stave` both fire `advancement_toast observ:the_ring_is_whole`. Learning to *read* is the
  first thing the world acknowledges. No door yet — only the record noting you. (Seed lines 78-103,
  568-590.) Payoff register: "the record noticed that I can now read it."
- **Movement III — The Undercroft.** `undercroft-descent` fires the first true `door_open`
  (`site_id:unbroken_light`, radius 3) — the lectern door drops and the group physically leaves their
  world for the keepers'. This is the first time a solve *moves the body*, not just the record. (Seed
  313-335.) `seventh-unwriting` fires a `reveal` (`fragment:seventh_name_unsealed`) — the sealed deep
  face becomes legible, naming the Seventh.
- **Movement IV — The Reckoning.** The Liar catch (`no-wall-catch`) fires `private_message`
  `iss.dialogue.turns_cold` — Iss's voice, which had been warm, lands cold on the solver. `m4-three-hands`
  (the cross-surface co-op gate) fires `door_open` at `coop_plate` — three acts in one 20s window open
  the Threshold. `atonement-refrain` and `true-walk-arrive` fire `reveal` fragments
  (`keeper_withheld_returned`, `destination_leaves_read`). (Seed 415-439, 824-896.)
- **Movement V — The Accepting.** The heaviest unlocks land here, by design. `rite-tokens` fires a
  `reveal` (six altar slots lit, `slots:6, lit:true`); the cipher-web (~§879) specifies the rite's
  set-piece as a `small_structure` with `require_floor:true`. `accepting-crouch` (the wordless
  synchronized bow, detected in-world) fires the climactic `door_open` (`open:true`, no radius — the
  whole altar opens). `record-receives` fires the final `advancement_toast observ:the_record_receives_you`
  — the arc closes on the same UI register it opened on (the toast), now meaning everything. (Seed
  447-544.) This bookend (toast → toast) is a planted structural rhyme worth preserving.

### 1.3 The story the producer-ladder tells
The escalation is itself diegetic. The record begins by merely *noting* you (toast). It lets you *in*
(door). It makes the sealed *legible* (reveal). Finally it *rebuilds* around your rite (structure) and
*receives* you (the closing toast, which is now a verdict). A friend who pays attention will feel the
world's responses grow heavier as they earn deeper standing — without any node ever saying so. That is
the "it knows your name" thesis expressed in masonry, not text.

---

## 2. CRITIQUE — adversarial, honest; risks + mitigations

**R-A (BLOCKING) — three seed rows fire NOTHING today (silent orphans).** Verified against source:
- `advancement_toast` reads `payload.advancement` (`AdvancementToastBeat.java:42`), but every seed row
  passes `step_payload:{key:"observance:..."}`. Result: `advKey==null` → falls to fallback title →
  both fallback fields blank → `skipped("no-advancement-no-fallback")`. **The toast never fires.**
- `private_message` reads `title|subtitle|actionbar|text` (`PrivateMessageBeat.java:32, canEnact`), but
  `no-wall-catch` passes `step_payload:{key:"iss.dialogue.turns_cold"}`. `canEnact` returns false →
  the cold-Iss line never lands in-world (it only flips the Discord/flag state).
- `reveal` is **not a registered beat** (`BeatLibrary.java:57-89` has no `reveal`). All 5 `reveal` rows
  (`atonement-refrain`, `rite-tokens`, `seventh-unwriting`, `seventh-choice`, `true-walk-arrive`)
  resolve to `library.get("reveal")==null` → `UnlockBeat.doEnact` returns `skipped("unknown-step")`.
  **The single most-used step in the seed is a no-op.**
  *Mitigation (this is the build):* (1) author a thin `RevealBeat` producer and register it; (2) fix
  the three contract mismatches — either map `key`→`advancement` in the seed, or (cleaner) have
  `resolve.ts` / a `key` resolver translate a `key` into the producer's native field via `voice.ts`.
  Decision below in §4. This is the genuinely-unbuilt remainder and it is small.

**R-B — "world-write off the main thread" / idempotency.** Every unlock delegates to a built beat that
already runs `mutateWhenUnwitnessed` on the main thread and is idempotent (door re-open is a no-op;
advancement re-grant is `skipped("already-granted")`; structure swap is A→B by PDC tag). The new
`RevealBeat` MUST inherit the same discipline (extend `AbstractBeat`, use `mutateWhenUnwitnessed`,
no-op if the target state already matches). *Mitigation:* code `RevealBeat` as a block-state/marker
flip only (no FAWE; that is `small_structure`'s job), so it stays trivially main-thread + idempotent.

**R-C — orphaned-gimmick risk: `reveal` with no concrete world target.** The five `reveal` rows pass
abstract fragments (`seventh_name_unsealed`, `keeper_withheld_returned`, `slots:6`). A `reveal` that
has no *physical* anchor is exactly the "lore with no mechanic" orphan the consistency law bans.
*Mitigation:* `RevealBeat` must require a `site_id` (a real `sites.yml` anchor) and a concrete block
op (swap a sealed-face block to its legible variant; light a slot; place/relabel a marker). The
`fragment` becomes a *carve/relabel* string written onto a real block at that site, not a free-floating
abstraction. If a row can't name a real site + op, it should be `lore` (a told fragment), NOT a
`reveal` beat. Audit: `rite-tokens`/`seventh-*` already carry `site_id` (`unbroken_light`,
`the_unwriting`); `atonement-refrain` lacks one and must gain one or downgrade.

**R-D — on-camera failure mode: a door that opens with nobody there.** A `door_open` fired when the
solve happened on Discord (solver not in-world) opens a door to an empty room — anticlimactic on a
recording. *Mitigation:* this is acceptable and even good ARG texture (the world changed while you were
away; you return to find it open — the iceberg). But for the *climactic* M5 unlocks, gate the heavy
ones (`small_structure`, the altar `door_open`) on in-world detection anyway — and the seed already
does this: `accepting-crouch`/`record-receives` use opaque in-world sentinels, so the solver is
necessarily present. No change needed; just don't add Discord-solvable climactic unlocks.

**R-E — precision law: an unlock is never personalized, so it can't mis-fire "it knows you."** Correct
and safe — unlock beats target `{solver}` by earned solve, never by inferred profile. The one near-edge
is `private_message iss.dialogue.turns_cold`: it addresses the solver, but on a *measured* fact (they
solved the catch), so it passes precision. *No mitigation needed; noted as clean.*

**R-F — collective law: do heavy unlocks punish an absent member?** No spine unlock gates on a *specific*
person; the climactic ones gate on a synchronized **active-player** bow (`accepting-crouch`). The
`{solver}` target is the person who earned it, which is neutral coloring, not an elected "chosen one."
*Clean — but enforce: never author an unlock whose `step_payload` names a specific player.*

**SCALE-DOWN call — `small_structure` as an unlock step is P1-deferrable, not P0.** `SmallStructureBeat`
is itself a STUB pending the FAWE schematic branch (R5/immersion §7). Until that lands, any unlock with
`step:"small_structure"` will `skipped`/`failed`. **CUT `small_structure` from the unlock step menu for
the vertical slice;** realize the M5 rite set-piece as `reveal` (slot-lighting on a pre-placed altar)
until FAWE ships, then upgrade. The cipher-web ~§879 `small_structure` rite is the *eventual* form, not
the slice form. Keep the other four steps; they all delegate to fully-built beats.

---

## 3. DE-SLOP TEST — exemplar in-voice lines (cold, plain, concrete)

These are the strings the producers actually carry (advancement subtitle, the cold-Iss `private_message`,
a `reveal` carve). Each passes the anti-slop law — declarative, mundane, no named emotion, no bow.

> **(advancement toast subtitle, `the_ring_is_whole`)** — `the record notes you can read it now.`

> **(`private_message`, `iss.dialogue.turns_cold`, action-bar, lands on the catch-solver)** —
> `the wall was a door the wrong way round. you checked the lock. good.`

> **(`reveal` carve written at `the_unwriting` when the seal opens)** — `seven marks were cut. one was
> cut out. the gap is the name.`

> **(`door_open` has no text — it is silent by law; the Watcher line is the resolver's, not the beat's)**
> `the lectern stands open. it did not before.`  *(authored as `voice.dest`/oracle line, NOT in the beat)*

Banned-pattern check: no "testament", no "little did", no three-adjective list, no announced feeling,
no thematic bow, no em-dash drama tic, no exclamation. The Watcher counts; it does not threaten.

---

## 4. THREAD IT — exactly where this lives so it is not an orphan

### 4.1 Canon-spine FACTs / INV it carries (touches, does not invent new lore)
The unlock beat is the *mechanical expression* of FACTs already owned by the puzzle rows it fires from —
it adds **no new FACT**, which is correct (it is delivery, not lore). It is the physical carrier of:
- **FACT 3/4** (literacy) via `the_ring_is_whole` toast (`rosetta-ring`, `a1z26-tick-stave`).
- **FACT 5/13** (the descent; the missing tool is you) via `undercroft-descent` door + `rite-tokens` reveal.
- **FACT 8** (the catch) via `no-wall-catch` `private_message`.
- **FACT 10b** (the land can refuse; the Seventh) via `seventh-unwriting`/`seventh-choice` reveals.
- **FACT 14** (the record receives) via `record-receives` toast.
- **INV note to add:** a one-line spine invariant — *INV-UNLOCK: every `step` value MUST name a
  registered beat AND a `step_payload` whose keys match that beat's contract; a row failing this is a
  silent orphan.* This is the consistency guard that would have caught R-A. Add to `canon-spine.md` and
  to a `seedcheck` self-test (§4.4).

### 4.2 Found-documents / journals that must foreshadow it
The "OH that's what that was for" plant (see §5) needs document homes. Touch:
- `arc/lore/documents/learn-them-as-we-learned-them.md` (D03) — the literacy doc — should contain the
  inert line that *later* reads as foreshadowing the toast ("the record keeps a note when a hand learns
  the marks"). Already thematically present; verify the line exists.
- `arc/lore/documents/what-the-surface-keeps.md` (D06) — Sella's doc, the Seventh thread — should name
  "a sealed face below the cold hearth" so the M3 `reveal` pays off a planted object.
- The Hold-Book M4 re-read (`base-docket-reread`, seed 596) — already authored — is the document whose
  *meaning* the unlock arc retroactively recolors (the count was of hands almost in).

### 4.3 NPC / Watcher voice lines that carry it
- `discord/src/voice.ts` keys: `oracleMainBeat`, `oracleNextClue`, `oracleSideQuest` already speak the
  resolver side. NEW voice content needed only for the producer payloads themselves:
  - advancement fallback subtitle text (when the datapack advancement isn't installed) —
    `AdvancementToastBeat` `fallback_subtitle`.
  - the `private_message` cold-Iss strings keyed `iss.dialogue.turns_cold` (see §4.4 resolution).
  - the `reveal` carve strings (the legible-face text written on the block).
- These live as authored constants; per the seed header (lines 555-557) a missing voice key is *silent
  at runtime, never a build break*, so they degrade safely.

### 4.4 The cipher(s)/puzzle(s) that express it (reuse the 11 built ciphers)
The unlock beat does not *contain* a cipher — it is the *reward* of one. Every producer row hangs off a
puzzle that already uses a built cipher; the reuse is honored, not re-invented:
- `rosetta-ring` → the rune-ring (literacy) · `a1z26-tick-stave` → **a1z26** (the runes-free twin door).
- `stone-iss-wall` / `bound-word` / catch → **vigenère** (key = ISS) → fires `private_message`.
- `seventh-unwriting` → **rail-fence** (rails=6, counted in-world), REUSING Brann's taught rail-fence
  literacy → fires `reveal`.
- `prophet-wall-name` → **columnar** acrostic → re-reads cold at the catch (pairs with the Iss
  `private_message`).
- `stone-mara` → **bookCipher** → `next_clue` to `undercroft-descent` → fires `door_open`.
- `stone-sella` → **atbash** + bearing → side-quest to the Seventh reveal chain.
No NEW cipher is introduced — correct for a delivery mechanic.

### 4.5 Beat classes / listeners / tables / seed rows / sites.yml / voice keys it realizes
- **Beat classes (built, reused):** `DoorOpenBeat`, `AdvancementToastBeat`, `PrivateMessageBeat`,
  `SmallStructureBeat` (STUB — deferred per §2), all dispatched by `UnlockBeat` (built).
- **Beat class (NEW, the only new code):** `RevealBeat` (`beats/lib/RevealBeat.java`, `name()=="reveal"`,
  category `WORLD`, extends `AbstractBeat`, registered in `BeatLibrary.registerDefaults()` under `// WORLD`).
- **Tables:** `beat_queue` (writer `db/repo.ts:enqueueOracleBeat`), `puzzles.outcome_payload.beat`,
  `arc_state.flags` (the gates that flip `active=false` rows on, via `setArcFlags`).
- **Seed rows (existing, to be corrected):** `puzzles_seed.sql` — the 7 `advancement_toast`/`door_open`
  rows (key/contract fix) + the 5 `reveal` rows (now have a real delegate).
- **sites.yml anchors (all exist):** `unbroken_light`, `the_unwriting`, `coop_plate`, `the_threshold`,
  `first_marker_01`. The new `RevealBeat` requires these as anchors (R-C).
- **Voice keys:** `iss.dialogue.turns_cold` (new), `reveal` carve strings (new), advancement fallbacks.
- **Self-test (NEW, TS side):** extend the seed-coverage self-test so a `step` value that names no
  registered beat, or a `step_payload` missing the delegate's required key, FAILS the build (the
  INV-UNLOCK guard from §4.1). This is what closes R-A permanently.

### 4.6 RESOLUTION of the contract mismatches (the decision §2 R-A deferred)
**Chosen approach — fix at the seed (authoring) layer, add a thin translation only where unavoidable:**
1. `advancement_toast`: change seed `step_payload:{key:X}` → `{advancement:X, fallback_subtitle:"..."}`.
   Pure seed edit; no code. (Cleanest — the beat already reads `advancement`.)
2. `private_message`: the seed passes a voice `key`, but the beat wants literal `title/subtitle`. Add a
   `key` resolution in `resolve.ts` *before* enqueue (look up the authored line by key in `voice.ts`,
   write it into `step_payload.subtitle`), OR author the literal strings in the seed. Prefer the
   `resolve.ts` resolver so the cold-Iss line stays a single source of truth in `voice.ts`.
3. `reveal`: build `RevealBeat`; change the abstract `fragment`-only rows to carry `site_id` + a concrete
   op (`{site_id, op:"carve", text:"..."}` or `{site_id, op:"light_slots", count:6}`).

---

## 5. PLANT THE PAYOFF — the "OH, that's what that was for" seed

**The plant (Movement I, inert):** when the group first learns to read (the `rosetta-ring` /
`a1z26-tick-stave` solve), the only world response is the corner toast — `the record notes you can read
it now.` It reads, in M1, as a flavor acknowledgement: nice, the game noticed. The literacy doc D03
carries the matching inert line: *the record keeps a note when a hand learns the marks.* No door opens.
It feels like the *smallest possible* reward — almost a letdown after a hard cipher.

**The payoff (Movement V, the bookend):** the *final* world response of the entire arc —
`record-receives` — is the **same step kind**: an `advancement_toast`,
`observance:the_record_receives_you`. The arc that opened on "the record merely *notes* you" closes on
"the record *receives* you" — the identical UI register, the corner toast, now carrying the whole
weight. The player who felt the M1 toast was a thin reward re-reads it: the record was *keeping the
note the entire time*, and every door/reveal/structure between them was the record deciding whether to
move from *noting* to *receiving*. The escalation ladder (§1.2) is the hidden answer to "why was the
first reward so small." **No payoff without a plant:** the M1 toast is the plant; the M5 toast is the
payoff; the ladder between them is the argument. Tracked seed: `SEED-UNLOCK-TOAST-BOOKEND`
(plant `rosetta-ring`/M1 → payoff `record-receives`/M5).

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

| Movement | unlock beats present | step kinds | depends on | priority |
|---|---|---|---|---|
| **I** | `rosetta-ring`, `a1z26-tick-stave` | `advancement_toast` | datapack advancement OR fallback title | **P0** (slice) |
| **III** | `undercroft-descent`, `seventh-unwriting` | `door_open`, `reveal` | `RevealBeat`; `unbroken_light`/`the_unwriting` placed | **P1** |
| **IV** | `no-wall-catch`, `m4-three-hands`, `atonement-refrain`, `true-walk-arrive` | `private_message`, `door_open`, `reveal` | `key` resolver; `coop_plate` listener | **P1** |
| **V** | `rite-tokens`, `accepting-crouch`, `record-receives` | `reveal`, `door_open`, (`small_structure` deferred) | `RevealBeat`; in-world sentinels (built) | **P1** (slice-able via reveal) |

**Depends on (upstream):** P0.3/P0.4 (the oracle resolver + answer surfaces — built); the datapack
advancements for the two toasts; `sites.yml` coordinates filled (currently `null` placeholders).
**Depended on by (downstream):** the whole "world feels alive" thesis; the showrunner's CONFIRM-mode
curatorial beats reuse the same producer palette. **Net priority:**
- **P0 (vertical slice):** the M1 `advancement_toast` path end-to-end (proves the dispatcher → producer
  → world loop on camera with the smallest possible piece). Requires only the seed `key→advancement` fix.
- **P1 (arc spine):** `RevealBeat` + the `private_message` `key` resolver + the seed `reveal`-row
  rewrites (site_id + op). This unblocks M3/M4/M5.
- **P2 (depth):** upgrade the M5 rite `reveal` to a true `small_structure` once the FAWE schematic
  branch ships (R5); the `record` website / extra-surface reveals.

---

## 7. BUILD CHECKLIST (the genuinely-unbuilt remainder, ordered)
1. **Seed fix (P0):** `advancement_toast` rows → `step_payload:{advancement, fallback_subtitle}`. No code.
2. **`RevealBeat.java` (P1):** new `WORLD` beat, `name()=="reveal"`, requires `site_id`, ops
   `carve`/`relabel`/`light_slots`/`swap_state`; `mutateWhenUnwitnessed`, idempotent; register in
   `BeatLibrary`. Rewrite the 5 `reveal` seed rows to carry `site_id` + `op`.
3. **`private_message` key resolver (P1):** in `resolve.ts`, resolve `step_payload.key` → authored line
   from `voice.ts` into `subtitle` before `enqueueOracleBeat`. Author `iss.dialogue.turns_cold`.
4. **INV-UNLOCK self-test (P1):** seed-coverage check fails the build if a `step` names no registered
   beat or a `step_payload` misses the delegate's required key. Closes R-A forever.
5. **Defer (P2):** `small_structure` unlock step until FAWE schematic branch lands; ship M5 rite as
   `reveal` slot-lighting meanwhile.
