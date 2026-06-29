# Idea Treatment — Forced Simultaneous Cross-Surface Co-op Gate

> **Status: KEEP (already partially canonized — this treatment closes the build gap).**
> The idea is not a fresh proposal; it is a thread the web already references as the **`m4-three-hands`**
> gate / **three-hands coop gate** / **`threshold_open`** precondition. WEB-MASTER §1.M4 and §2,
> the `the-cold-square` document, BN8 voice node, the `m4-three-hands` + `bound-word` seed rows,
> the `coop_plate` site, and `AcceptingRiteListener`'s `readyGate` all assume it exists. What does
> **not** exist yet is the realizing CODE: `CoopPlateListener.java`, the `coop_gate_legs` table +
> migration, the `oracleThreeHands` voice key, and the AND-join single-writer. This document is
> the build-ready spec for that gap, plus the adversarial critique that should reshape it.

---

## 0. The one-line truth

One gate, three **acts** done inside one short shared window, on three surfaces at once:
**a foot held on the Undercroft pressure-glyph (plate), a hand carving the cut beside it (answer-sign), and the bound word typed into Discord** — within the same ~20–30s window. When all three land in-window, a plugin sentinel posts an opaque, un-typeable conjunction token to the shared oracle; the `m4-three-hands` outcome fires `main_beat`, sets `threshold_open=true`, and opens the Threshold. It is the "everybody get on the call, on three, GO" moment.

**Canon law it must obey (already written, do not break):** three **acts**, not three **people**. Active-players-only. Two players (or even one with a second device, slowly) CAN clear it — the gate counts hands at the plate-window, never seats. (WEB-MASTER §2 ledger row "Three-hands coop gate"; the collective-ending no-absent-punish rule.)

---

## 1. EXPOUND — the full mechanic + story + mystery

### 1.1 The three legs (what each surface actually measures)

| Leg | Surface | The verb | What the engine measures | Site / symbol |
|---|---|---|---|---|
| **FOOT** | Minecraft (world) | stand and *hold* on the pale square | a sustained plate-press (sneak-hold / weighted-stand) inside `coop_plate` radius, sticky for the window | `coop_plate` site (Undercroft) |
| **CUT** | Minecraft (world) | carve the cut beside the plate | an `answer_sign` submission of the bound word at the `keeper_stone` adjacent to the plate | `coop_plate`-adjacent answer-sign |
| **WORD** | Discord | type the bound word into the dark | `/answer` (or `#the-record` scan) of the bound word, surface=`discord` | the oracle Discord surface |

The **bound word** is the same token for CUT and WORD (Iss's Vigenère plaintext, "the one who turned away" / his name — `bound-word` seed row already yields it via `bound_word_known`). FOOT carries no word; it is a physical presence the plate measures. This is exactly Mara's instruction: *foot to the square, hand to the cut, word to the dark.*

### 1.2 The window and the AND-join (how it fires)

- Each leg, when satisfied, writes a **timestamped row** to `coop_gate_legs` (leg ∈ {foot, cut, word}) keyed by gate id `coop_plate` and the active actor. Legs are **reversible** — a leg expires after the window (`COOP_WINDOW_MS`, ~25–30s) so a stale press never silently completes the gate hours later (anti-jank: nothing completes un-witnessed).
- The **single AND-join lives once** (BUILD-MANIFEST §89: "coop-gate AND-join (single writer)"). **Recommendation: put the join in the PLUGIN `CoopPlateListener`, not in TS** — the plugin has the world legs live and is a single process, so there is no cross-process race; `resolve.ts` then only consumes the opaque token the plugin posts (exactly like the Accepting). On every leg-write the listener checks: *are all three distinct legs present with `at` within `COOP_WINDOW_MS` of each other, by the active set?* If yes, and not already cleared (idempotent on `threshold_open`), it posts the **opaque conjunction token** (`h3n8k1 q5m2x7 …`, the `m4-three-hands` answer array) to `OracleResolver.resolveWorld` — as `AcceptingRiteListener` posts the accepting token. The oracle's replay-guard + `recordSolve` (ON CONFLICT DO NOTHING) makes it fire exactly once.
- `m4-three-hands` outcome (already seeded) sets `threshold_open=true`, speaks `oracleThreeHands`, enqueues the `unlock`/`door_open` beat at `coop_plate`, and points `next_puzzle_key → threshold-coordinate`.

**Why opaque + plugin-posted (the whole point, B-5 anti-spoof):** the conjunction token is six random sextets; not a phrase anyone can guess, read off a wiki, or paste in Discord. Only the detector — a real same-window three-leg convergence witnessed by the server — can produce it. The gate must be *performed together*, not solved by one clever person at a sign.

### 1.3 How it plays across the 5-movement arc

- **Movement I (Days 1–5) — the inert plant.** The `coop_plate` is a pale, cold square in the Undercroft floor (the dead floor-glyph). No function, no prompt. The `the-cold-square` document is findable but reads as a sad keeper memory. Nobody knows the square does anything. *Seed laid, inert.*
- **Movement II (Days 6–11) — the rumor.** The `the-cold-square` book-cipher becomes solvable and assembles to *"foot to the square, hand to the cut, word to the dark, the three at once."* The group now *knows there is a rite* but cannot perform it: they lack the **word** (Iss is still warm; his Vigenère is unwalked). The square is a known locked door with a missing key. Standing on it alone gives at most one cold pulse — *"a thing done alone is one thing; the threshold counts three"* — never opens. This deliberate frustration makes the eventual solve land.
- **Movement IV — THE CATCH, the gate's hour (the densest hinge, ~Day 12).** The catch flips Iss cold; his stone re-carves; the **bound word** becomes earnable (`bound-word` → `bound_word_known`). Every piece now exists: the document told the choreography in MII, the catch hands the word. The realization is *"wait — we all have to do this AT THE SAME TIME."* They get on a call. One stands on the plate. One readies the carve. One has Discord open. Someone counts down. On three: foot held, cut carved, word typed. Legs land in-window; the join fires; `oracleThreeHands` speaks: *the count is three. the threshold is open.* The Threshold opens, its carving yields the **true coordinate**, they walk.
- **Movement V — the on-ramp it built.** `threshold_open` is the **fail-closed precondition** on `AcceptingRiteListener.readyGate` (already wired in code). The terminal simultaneous bow cannot fire until the coop gate opened the Threshold and the group walked the true coordinate. The coop gate is the *rehearsal* for the Accepting's bigger same-instant convergence — same muscle, raised stakes. *"OH, the bow at the end is the cold square again, but it's all of us, and it's the way in."*

---

## 2. CRITIQUE — adversarial and honest

### 2.1 The sharpest risk: **the window is a jank/feel-bad trap on camera.**
A ~20s simultaneity window across two clients + Discord, with network latency, plate-press detection lag, and humans miscounting, can produce **near-misses that feel like a bug**: "we all did it, nothing happened." On a Wifies-grade video, a gate that *looks* solved but silently fails reads as broken, not mysterious — and the silence-on-miss law (correct everywhere else) here punishes a correct attempt.
**Mitigation (do all three):**
1. **Widen and forgive the window.** `COOP_WINDOW_MS` ≥ 25–30s, and legs are **sticky**: a leg stays satisfied until window-expiry, so the three don't need the same *instant*, just the same *window*. Mara's text says "in one breath" narratively while the engine counts a tolerant window — fiction tight, tolerance generous.
2. **Diegetic per-leg confirmation that is NOT a solve-tell.** When a leg registers, give that one actor a private, in-register cold pulse (FOOT: the plate hums under you; CUT: the cut takes; WORD: the bot reacts to the bound word as the normal `next_clue` it always would). None of these says "2 of 3" — that would leak. But each actor knows *their* leg took, so a near-miss is diagnosable as "Word was late," not "the whole thing is broken." **This is the single most important mitigation.**
3. **Reusable on failure (reversible toll, not progress).** Legs expire; the group re-counts and retries. No lockout, no penalty, only a short ~5s anti-spam cooldown so the "again, on three" rhythm survives.

### 2.2 Orphaned-gimmick risk: **LOW — it is the opposite of an orphan.** It is load-bearing in lockstep: the `the-cold-square` document (story/clue), the bound word (cipher payoff), `threshold_open` (the Accepting's gate), the Threshold→true-coordinate→walk chain. Cutting it would orphan *those*. The only orphan risk is building the listener but never re-reading the document at the catch — so the MII plant must explicitly re-surface at M4 (it does: the catch hands the word the document's choreography was always waiting for).

### 2.3 Collective-law risk: **"forced 3 people" violates no-absent-punish if implemented as people.**
The pitch SAYS "3 people on 3 surfaces." Canon SAYS "3 acts, active-only, 2 can clear it." **These conflict, and canon wins.** Three distinct humans would hard-block a 2-person session — a group-punish for absence, breaking the collective-ending law and INV-16.
**Mitigation (already in canon, enforce in code):** count **distinct legs in-window**, never distinct UUIDs. One active player can hold the plate, alt-tab to carve, post in Discord across the window (slow but possible); two trivially. Quorum is on **active players present**; the leg-set is `{foot,cut,word}` not `{player1,player2,player3}`. "3 people on a call" is the *celebrated* path — never the *required* one.

### 2.4 Precision-law risk: **a personalization callout here would be a wrong "it knows you."** The gate is a mechanic, not a profiling moment. It must NOT say "you three" or name anyone — `oracleThreeHands` is flat and count-only. Do not let any per-leg confirmation name a player.

### 2.5 Path-A risk: **none new.** Plate = themed press the resource pack already supports; answer-sign + Discord `/answer` are existing verbs. No client install. ✅

### 2.6 Anti-jank / main-thread risk: plate detection and leg-writes are Bukkit reads → **main thread**; the oracle post is **async** (mirror `AcceptingRiteListener` exactly). The AND-join must be **idempotent** on `threshold_open`. Fail-closed if the legs read errors (no spurious open). ✅ if built to the `AcceptingRiteListener` pattern.

### 2.7 Should anything be CUT or scaled? **Scale the *framing*, not the mechanic.** Keep all three legs (cutting one collapses it to an ordinary answer and kills the "on three" moment). **Cut the hard-instant simultaneity** — replace with the tolerant sticky-window above. **Do not add a fourth surface** (no dashboard leg) — three is the canon count and the document's count; a fourth would orphan the `the-cold-square` triple and dilute the "three" payoff. **P0? No — P1 (arc-spine):** not in the vertical slice, but the M4 hinge the spine and finale both require.

---

## 3. DE-SLOP TEST — exemplar lines in-voice (cold, plain, concrete)

The gate's only spoken line is `oracleThreeHands`, kept flat:

> the count is three. the threshold is open.

A near-miss stays silent (no "so close", no tell). The *document* margin (the later record hand) carries the only explanation, mechanical not emotive:

> one keeper can do two and a hand short. it does not open. the count is three. the threshold is open or it is not.

Mara's page-nine fragment (grief shown structurally — failure as arithmetic, not lament):

> i had the page. i had the square under my foot. i typed the word into the dark, alone, and nothing opened, because a thing done alone is one thing and the threshold counts three.

(All pass: lowercase, no caps/exclaim, no named feeling, no "testament/little did/not just X but Y," no personified-object memory — the threshold *counts*, it does not *remember*; the prior slop "the threshold remembers three" was already corrected to a flat count, BN8 note.)

---

## 4. THREAD IT — exactly where this lives (no orphan)

**Canon FACTs / INV it touches (adds none new — it *realizes* existing ones):**
- **FACT 15** (Mara's "cold square… i typed into the dark" + the dead floor-glyph) — this gate IS FACT 15's payoff. (WEB-MASTER §2 ledger #15, M1→IV/V.)
- **`bound_word_known`** flag (the gate's need) and **`threshold_open`** flag (its product). Both already in the seed.
- **Collective ending / no-absent-punish** and **INV-16** (no surface lets the group derive WHICH active player) — enforced by counting acts not seats, and `oracleThreeHands` naming no one.
- Single Iss IV→V chain (WEB-MASTER §1.M4): catch → bound word → **this gate** → Threshold → true coordinate → walk → Accepting.

**Found-documents / journals that mention or foreshadow it:**
- `arc/lore/documents/the-cold-square.md` — the primary plant (book-cipher; assembles the choreography). Foreshadows M1; pays off M4.
- `arc/corpus/journals-vaun-mara-sella.md` — Mara's page-nine fragment + "three hands, one Going-Out" (present, ~line 304).
- `arc/corpus/letters.md` — "three hands, one Break" (present, ~373).
- `arc/lore/found-documents.md` — the cold-square entry already names this "the coop-gate plant"; links → `no-wall-was-ever-built-here` (carries the withheld word) + `bring-the-thing-only-you-can-give`.

**NPC / Watcher voice lines that carry it:**
- `oracleThreeHands` (BN8, `the count is three. the threshold is open.`) — **MUST BE ADDED to `discord/src/voice.ts`** (the seed references it; the key does not exist in voice.ts yet — grep returns nothing). Add the `OracleVoiceKey` + the `speakOutcome` case in `resolve.ts`.
- `voice.dest.threshold.arrive` (BN9, already authored) — the arrival line after the gate.

**Cipher(s) / puzzle(s) it expresses (reuse the 11 built ciphers):**
- **bookCipher** — `the-cold-square` page/line/word triples → the choreography sentence (the plant).
- **vigenère** (key = Iss's name) — yields the **bound word** that CUT and WORD both consume (`bound-word` seed row).
- A **substitution / a1z26** second keeper-stone in-road to the bound word (WEB-MASTER §2 "a second keeper-stone in-road") — non-linear redundancy so the word has two doors.
- The conjunction token itself is **not a cipher** — an opaque plugin sentinel (correct; it must be un-typeable).

**Beat classes / listeners / tables / seed rows / sites / voice keys that realize it:**
- **NEW `CoopPlateListener.java`** (`plugin/.../signal/listener/`) — watches `coop_plate` type; writes legs; runs the AND-join; posts the opaque token via `OracleResolver.resolveWorld` (mirror `AcceptingRiteListener`: Safety-wrapped, main-thread reads, async resolve, `RateLimiter` cooldown, sites via `Supplier`, idempotent on `threshold_open`). Constants `COOP_WINDOW_MS`, `COOP_COOLDOWN_MS`.
- **NEW `coop_gate_legs` table + migration** (`discord/supabase/migrations/0006_*.sql` already lists `coop_gate_legs` as owed — build it): `(gate_id, leg text check in (foot,cut,word), actor_uuid, at timestamptz)`, plus a window-query helper in `repo.ts`.
- **AND-join single-writer** — recommend in the **plugin** `CoopPlateListener` (single process, has world legs live); `resolve.ts` `applyOutcome` of `m4-three-hands` already sets flags/beat and only needs to consume the opaque token.
- **`oracleThreeHands` voice key** — add to `voice.ts` + `speakOutcome` switch in `resolve.ts`.
- **Existing seed rows (keep):** `bound-word`, `m4-three-hands`, `threshold-coordinate`, `pressure-glyph-walk`. **Add** the substitution/a1z26 second in-road row to the bound word.
- **`coop_plate` site** (sites.yml, present, type `coop_plate`) + an adjacent `keeper_stone`/`answer_sign` for the CUT leg.
- **`thread_cards.sql`** — the "three-hands" card already listed as owed (BUILD-MANIFEST §122); `body_voice_key = oracleThreeHands`.

---

## 5. PLANT THE PAYOFF — the "OH, that is what that was for" seed

- **PLANT (Movement I, inert):** the pale **cold square** in the Undercroft floor — décor, cold to stand on, no function. Standing on it alone does nothing (or one cold pulse). The `the-cold-square` document is findable but reads as a keeper's sad memory and a book-cipher with no obvious target. **Ambiguous, inert, un-actionable.**
- **MID (Movement II, named but locked):** the book-cipher solves to the choreography — *foot / cut / word / the three at once.* Now a known rite with a **missing word**. The square is a door they see but can't open. Frustration is the point.
- **PAYOFF (Movement IV, the catch):** Iss flips, the **bound word** drops. Every piece the MII document described now exists. The group realizes the square wanted three acts in one window — **they get on a call and count down.** The gate clears; the Threshold opens. The inert M1 floor-square was the IV→V hinge all along.
- **ECHO (Movement V):** the Accepting's simultaneous bow is the cold square *writ large* — same "all at once," now the whole present group, now the way in. `threshold_open` (this gate's product) is literally the fail-closed key on `AcceptingRiteListener.readyGate`. **No plant without payoff; no payoff without plant.**

---

## 6. MOVEMENT PLACEMENT + DEPENDENCIES + PRIORITY

- **Lives in:** plant in **Movement I**, named in **Movement II**, **fires in Movement IV** (the catch hour), **echoed/depended-on in Movement V**.
- **Depends on:** `bound_word_known` (← `bound-word` ← Iss Vigenère ← `iss_caught`); the `coop_plate` site placed with an adjacent answer-sign; the `the-cold-square` book-cipher solvable.
- **Depended on by:** `threshold_open` → the Threshold carving → `threshold-coordinate` (true walk) → `AcceptingRiteListener.readyGate` → the entire Movement V finale. **A single point on the critical path** — if it can't clear, the finale is unreachable (hence the forgiving-window mitigation is non-negotiable).
- **Priority: P1 (arc-spine).** Not in the P0 vertical slice, but the M4 hinge the spine and finale both require. Build order: (1) `coop_gate_legs` migration, (2) `oracleThreeHands` voice key, (3) `CoopPlateListener` with legs + AND-join + opaque post, (4) per-leg diegetic confirmation, (5) the substitution second-in-road seed row, (6) wire `AcceptingRiteListener.readyGate` to read `threshold_open` (the overload already exists — pass the supplier at registration).

---

## 7. The one build gap to flag loudly

Everything narrative and seed-level exists. **Three code artifacts are referenced but NOT built:** `CoopPlateListener.java`, the `coop_gate_legs` table/migration, and the `oracleThreeHands` voice key (+ its `speakOutcome` case). Until those land, `m4-three-hands` can only be cleared by someone typing the opaque token — impossible by design — so **the Threshold can never open and Movement V is unreachable.** This is the highest-leverage unbuilt piece on the IV→V critical path.
