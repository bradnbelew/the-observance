# 06 — Group Play & the Watcher

Brad wants real group play — **multiplayer-contribution puzzles and per-player asymmetric hallucinations
("each player sees something different / must contribute")** — and a Watcher that makes the world feel
alive and responsive. The primitives exist and one example ships; this doc is how to make it systemic
across the rebuilt Hold.

---

## 1. The current state

- **Zero required-multiplayer nodes** existed across the whole 82-node arc (every collective node had a
  mandated solo path, per `EXPERIENCE-MANIFEST.md`'s "finishable with one active player" rule). No
  asymmetric-information mechanic existed between C01 and C08.
- **V5.1 added exactly one**: `BaseMirrorFragmentListener` at the Unlit base mirror — three physical
  observations, each visible only to the player standing nearest that point (uses
  `PerPlayer.showEntityTo`). That's a *proof the mechanism works*, not a systemic fix.
- **A stronger primitive is already built and proven**: `ThresholdVaultListener` — a combination split
  into per-player fragments over the *active roster*, each a rune `TextDisplay` only that one client can
  see (`PerPlayer.showEntityTo` + a deterministic dynamic-roster partition that self-heals as players
  come/go). Only by reading fragments aloud together can the group assemble the code. This is the
  template for asymmetric group play.

## 2. The constraint you must respect

`EXPERIENCE-MANIFEST.md` mandates the campaign be **finishable with one active player** (a real
constraint — friends won't always all be online). So:
- **Asymmetric moments reward and deepen group play; they must not hard-lock a solo player out.** The
  ThresholdVault and BaseMirror both satisfy this: a lone player can walk between the points/positions
  and gather all fragments themselves, just slower and less magically. Design new ones the same way —
  *better together, still possible alone.*
- **Genuine quorum is allowed only where the fiction demands it** and an accessibility path exists: the
  finale ballots (WR05/RP03/RP04) already do this (a solo eligible player resolves directly; a
  two-player hand-off has an accessibility fallback). That's the pattern for "must contribute."

## 3. Patterns to deploy across the rebuilt cases

Reuse the built primitives (`PerPlayer.showEntityTo` / `hideEntityFrom`, the dynamic-roster partition)
on the new Hold rooms (doc 1). Aim for **several asymmetric moments spread across C02–C08**, not
bookended at C01/C08/C10:

1. **Split-evidence (must-contribute-to-be-fast).** One code/reading split into per-player fragments on
   the same wall (ThresholdVault model). Rehost it on a rebuilt case node where a group naturally
   gathers.
2. **Positional asymmetry (BaseMirror model).** N fixed observation points, each showing its clue only
   to the nearest player — the group must split up and report. Good for a large room (a market floor, a
   camp) where spreading out is natural.
3. **Per-player hallucination (the Watcher, §4).** One player sees a name/mark/figure on a wall that
   the others, standing right beside them, do not. The `name_on_wall` beat *is* this — the player's own
   name in runes, seen by no one else, gone when they look away. Use it as a scare and, sparingly, as a
   clue channel ("did the wall say something to you?").
4. **Complementary readings.** Two records in two rooms that only resolve when compared — naturally
   splits a group across the Hold and forces them to talk. (This is investigation, not a mechanism —
   the cheapest "group" design and it always works.)

## 4. The Watcher — already reawakened, integrate it with the new Hold

V5.1 turned the ambient Watcher back on in production behind a safety allowlist (see
`design/V5.1-REDESIGN.md`). It is *live* but currently targets the *old* Hold's site ids for its
scare palettes. As you rebuild rooms, keep it integrated:

- **The engine:** `AmbientBeatGenerator` picks a focus player weighted by an **Attention** score
  (alone + deep + dark + night raise it), so the Watcher is drawn to the lonely/deep/uneasy player — it
  reads as "it knows *me*", not a dice roll. It fires only the curated **text-free sensory palette**
  (`private_sound`, `private_particle`, `private_darkness`, `private_time_shift`, `proximity_dim`) plus
  the `name_on_wall` signature, gated by the drama budget. A guard (`V5SafeBeatEnactor`) allowlists only
  `whisper_toll`/`hint_whisper` from the DB queue, so no retired V4 beat can fire.
- **Integrate with new rooms:** `AmbientBeatGenerator.scaryChoices(...)` switches its sound/particle
  palette on the site the player stands in (Brann tower → cold toll behind you; Sella water → whisper +
  falling-water; keeper stones → stone-breath + ash). When you rename/replace rooms, update those
  site-id matches so each new district gets its own authored dread palette. Keep it **text-free** (the
  safety rule) — the fear comes from sensation and the `name_on_wall` signature, never from authored
  lines the engine invents.
- **Cadence:** `config.yml drama.*` is tuned to a felt-but-restrained rate (window 5/30min, global 3min,
  per-player 20min). Attention keeps calm/grouped moments quiet. Don't crank it — restraint is the
  horror.
- **`/whisper` toll:** the hint toll now actually fires (the queue poller runs, guarded). Keep the hint
  bodies (`hint_whisper`) in-register and useful — they are part of "players must be able to reach the
  answer" (doc 2 §4).

## 5. Verify

- Plugin self-tests (`gradlew check`) — the beat classes and listeners compile and pass.
- A live multiplayer read (two accounts) of each new asymmetric moment: confirm each player sees only
  their fragment, a lone player can still gather all of it, and roster changes (a player leaving)
  re-partition without stranding anyone.
- Confirm the Watcher fires on a lonely/deep test account and stays quiet for a grouped one.
