# Clue-Web Seed — placement notes (puzzle_key → where it must surface)

> Companion to `discord/supabase/seeds/puzzles_seed.sql` (the INSERTs) and
> `design/clue-web.md` (the map). This file tells the **world-placement** agent
> which lore doc (D01–D12) / in-world beat must *teach* each puzzle's answer, plus
> the site binding and any unresolved gaps. The seed is the contract; this is the
> "where do players read the key" index so nothing ships fair-but-unteachable.
>
> Grounding rule (clue-web.md §6 / cipher-web.md §7 step 2): every `accepted_answers`
> string must be derivable from something in the world. If the doc/beat below is not
> placed, flip that row `active=false` until it is.

## Surfacing map

| puzzle_key | outcome_type | lore doc / in-world beat that must surface it | site / placement |
|---|---|---|---|
| `m1-record-opens` | lore | **D01** the-record-opens — found on the group's OWN base lectern | `first_report_lectern_01` |
| `m1-named-habit` | dead_end | **D01** + a grounded personalized report (the "scalpel", names only a *measured* habit — spine §6 rule 4) | base lectern; needs the per-player report generator |
| `rosetta-ring` | main_beat | **D03** founder margin-note **AND** the server-icon rune ring — TWO doors | server icon (metadata) + first keeper-stone margin; advancement `observance:the_ring_is_whole` |
| `stone-vaun` | lore | **D02** counted-them-in-the-dark + the cairn at the shaft-mouth (verb: rotate item-frame ring) | `keeper_stone` (Vaun) + `offering_cairn_01` |
| `stone-mara` | next_clue | **D05** page-line-word — the six-book Kept-Light lectern shelf, walked L→R | `keeper_stone` (Mara) + the six-book shelf at `kept_light_home_01` |
| `stone-sella` | side_quest | **D06** what-the-surface-keeps — the half-sunk lectern at the shore pool | `keeper_stone` (Sella) at a shore pool |
| `stone-orin` | next_clue | **D07** i-thought-it-small — threshold-stone, lintel forces the head **down** (crouch-to-read) | `keeper_stone` (Orin), low lintel |
| `stone-brann` | lore | **D08** do-not-close-your-eyes-here — night / black-moon-gated journal at the cold hearth | `keeper_stone` (Brann); beacon colour rig; night gate |
| `stone-iss-wall` | next_clue | **D09** the-ways-are-a-wall — the warmest, most trustworthy voice (the trap) | `keeper_stone` (Iss) |
| `m2-rhyme` | dead_end | any two of **D02/D05/D06/D07** cross-referenced (no new doc) | emergent; no dedicated site |
| `iss-dead-shrine` | dead_end | **D09** ("go there and you will have the end of it") → named by **D10** ("what iss sent you to was a grave") | the cold-hearth grave (a real, placed site) |
| `iss-doubt` | next_clue | **D09** hard-margin ("we checked the lock…") + foot-line ("ask first what a wall is for") | re-read at Iss's stone |
| `no-wall-catch` | main_beat | **D10** no-wall-was-ever-built-here — the Stone-after **behind** the falsely "kept · solved" clue | the mismarked clue site; private_message `iss.dialogue.turns_cold` |
| `undercroft-descent` | main_beat | **D05** resolved sentence + the Kept-Light lectern-comparator door | `unbroken_light` (door_open) |
| `undercroft-fog` | lore | the Undercroft reversal set-piece + **D08/D01** fire lines | inside the Undercroft (custom dimension) |
| `seventh-shrine` | side_quest | **D11** the-seventh-not-kept — pointed-to by D06, cross-linked by D08 + D09 | the cast-out cold-hearth shrine (off-spine) |
| `orin-threshold` | lore | **D04** observed-warned-left-at-threshold — same hand as D01, later & colder | base lectern (later report) + Orin's stone |
| `haunting-biography` | lore | **M4 keeper-NPC dialogue** carrying FACT 9 (implicit in D04+D07 + the Iss re-walk) | keeper-NPC; **see gap G1** |
| `atonement-refrain` | main_beat | the dossier-branching keeper NPCs (`arg-deepening §2` — conduct is the lock) | keeper-NPC dialogue gate |
| `rite-tokens` | main_beat | **D12** bring-the-thing-only-you-can-give + the altar's labelled TextDisplay slots + D05's "a piece you cannot read your way out of" | `unbroken_light` altar |
| `pressure-glyph-walk` | lore | the altar floor (pressure-plate verb menu) | `unbroken_light` altar floor |
| `accepting-crouch` | main_beat | **D12** ("when all of you bow as one") | `unbroken_light` altar; plugin sentinel |
| `record-receives` | main_beat | **D12** ("it receives… it would keep you") + D01's buried seed + the persistent world flip | world-wide; advancement `observance:the_record_receives_you`; **staged `active=false`** |

## Two-door / redundancy guarantees (web rule — clue-web.md §2)

- **Literacy (`rosetta-ring`) has two independent doors:** server-icon ring **and**
  D03 margin-note. Place at least one before Movement II opens; placing both is the
  intended no-bottleneck state.
- **Spine key `stone-mara`** double-sourced: D05 forges the descent sentence, D12
  restates it. **Spine key `no-wall-catch`** double-sourced: D10 (the catch) + D11
  (the Seventh's "a wall does not choose"). Either source alone keeps the spine alive.
- **The Seventh** is seeded three times: `stone-sella` margin, D01's "seventh mark",
  D08's cast-out one — all converge on `seventh-shrine`, which **gates nothing**.

## Beat-payload conventions used (so the plugin's UnlockBeat can dispatch)

All beats are `type:"unlock"`, `mc_uuid:"{solver}"`, enqueued `status:'approved'` by
the Oracle path (resolver-side, not in the row). `payload.step` ∈ the existing palette:
`advancement_toast`, `private_message`, `door_open`, `reveal` (matching the canonical
set already used in `design/cipher-web.md §6`). The oracle introduces **no new beat
type** — it is the producer that makes `UnlockBeat` fire (ORACLE.md §4).

## Unresolved gaps / contradictions to resolve before go-live

- **G1 — `haunting-biography` has no dedicated lore doc.** It rides on M4 keeper-NPC
  dialogue (FACT 9), flagged `spine TODO-3` in clue-web.md. Its `accepted_answers`
  are paraphrase-style; until the NPC line that *teaches* the phrasing exists, treat
  this row as soft (consider `active=false` or widen `accepted_answers` once the
  canon line is written). **Owner: the keeper-NPC / dialogue agent.**

- **G2 — `unspoken-refrain`, `self-rewriting-journal`, `haunted-herd` are NOT seeded
  as `puzzles` rows.** Per the WEB NOTES they were folded under the 24-node cap:
  `self-rewriting-journal` and `haunted-herd` are ambient/observation threads (no
  typed answer → no `puzzles` row needed), and the inverted Refrain is represented by
  `atonement-refrain`'s refrain verb + the side-quest inventory. If a *typed* gate is
  later wanted for any of them, mint a new row in this namespace. **Not a bug — a
  documented scope fold.** (Net: 23 seeded rows; 24th "node" FACT 15 is authored as
  NO node by design — felt, never stated.)

- **G3 — Sentinel answers for the M5 terminal rows are placeholders.**
  `accepting-crouch` and `record-receives` (and the spine-key handoff for the
  Accepting) carry short opaque sentinel tokens written in the normalized charset
  (`[a-z0-9 ]`, e.g. `a7f3 accepting bow sentinel posted only by plugin`). These are
  illustrative — the plugin must post the **real** long opaque token once the
  customs listeners confirm the simultaneous-bow + deposits (cipher-web.md §5 "the
  rite sentinel"). Swap in the production tokens and keep them out of any corpus.
  **Owner: the plugin / customs-listener agent.**

- **G4 — `record-receives` is staged `active=false`.** Intentional (it must not be
  answerable before M5), but it means the FACT 14 row will silently not match until a
  showrunner flips it. Confirm the M5 staging step flips `active=true` (and only then).

- **G5 — Coordinate answers are stored as direction-words + unsigned forms, not the
  literal world coords** (the world seed isn't chosen yet; sites.yml coords are still
  `null`). Once the world is sited, ADD the real unsigned coordinate strings (e.g.
  `1280 64`) to `iss-dead-shrine`, `stone-sella`, and `no-wall-catch` so a player who
  reads the literal coordinate off a carving also matches. The minus sign is dropped
  by normalize step 3, so store the **unsigned** form only. **Owner: world-seed agent.**

- **G6 — `max_attempts` is set only on `stone-iss-wall` (6).** Every other row is
  uncapped (the global token bucket still applies — ORACLE.md §8). If any other short
  answer (e.g. `seventh-shrine`'s `seven`/`7`) proves brute-forceable in playtest,
  add a per-puzzle cap there. Not a blocker; a tuning dial.
