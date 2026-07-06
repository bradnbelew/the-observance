# THE OBSERVANCE — PLANNED-LAYER LEDGER (the anti-forgetting sweep)

> **Pre-deploy snapshot — badly stale.** Written before the server/bot/dashboard were ever live; its "not
> one beat has been SEEN firing live" no longer holds. Current status: `LAUNCH-READINESS.md`. Kept for
> the layer inventory itself, which is still useful.

> Built 2026-07-01 by a full-corpus audit: **236 planned layers** enumerated from the design docs, each
> checked against live code for **built → wired → deployed → proven**. This is the promise that nothing
> was forgotten. Two director corrections are folded in (see ⚠️). Where this and a status-checker
> disagree, the code was re-checked by hand and the code wins.

## THE HONEST READ (director take)
**The project is TWO halves and only one is on the field.** The **plugin jar** (the whole Haunting
Engine, signals, illusions, oracle, every producer, structure-gen) is deployed and its self-tests pass —
**but not one beat has been SEEN firing live.** The **Discord + website half** is fully built and largely
self-tested but runs **nowhere**, because there is no host yet.

So the truest risk is **not "forgotten code"** — it's that a large amount of correct, unit-proven code has
**never been proven in the world**, and **one missing host + one un-hosted website gate almost all of it.**
Almost everything exists. The gap is **deployment + one playtest**, not months of building. That should be
a relief.

---

## 1. GENUINELY FORGOTTEN — planned, zero code (the real worry, and it's only 3 things)
1. **The LLM "AI Director" brain** (Claude Agent SDK + per-player personalization) — the headline "it's
   intelligent" promise. VERIFIED absent (no Anthropic SDK anywhere); the **deterministic `decide()` is
   quietly standing in for it.** DECISION: build a thin Agent-SDK layer *on top of* the proven
   deterministic floor, OR formally re-scope to "deterministic showrunner" and stop promising a brain.
2. **`0008_observations` table** (the Observer Engine DB) — the keystone that turns chat/voice capture into
   grounded scares. VERIFIED absent (migrations stop at 0007). Its absence silently no-ops a whole chain
   (autonomy readers, shape-rhyme apparitions, the "Discord bleeds" leaks). DECISION: author it, or cut the
   Observer Engine and delete the dead readers.
3. **Six distinct keeper voices** (per-keeper OGGs) — only 4 generic sounds exist. Claude can generate
   these (same pipeline as the 4 we already made).

## 2. BUILT BUT NOT WIRED — code exists, doesn't fire
- **ThresholdVault active-roster supplier** — passed `null` (`ObservancePlugin.java:519`) → falls back to
  online-count, breaking the dynamic-roster invariant for the co-op vault. ✅ confirmed. Owner: claude.
- **Undercroft dimension** — datapack built but no teleport/entry wired; the live gather-room is stamped on
  the overworld instead. Decide: wire an entry, or accept the overworld version as canonical.
- **Cross-owner autonomy readers** (liar warm-beats, shape-rhyme, keeper-record/name-where/offline-skin) —
  pure policies pass self-tests but sit behind un-wired reads (many depend on the unbuilt `0008`).
- ⚠️ **CORRECTION — Companion/Reckoning producers: the ledger flagged these "unbuilt." FALSE.**
  `WrenNpcListener` (registered `ObservancePlugin:403`) sets `companion_trust/revealed` and
  `reckoning_condemn/understand/free` via `mergeArcFlags`; `CompanionArcWatcher` + `SeventhChoiceListener`
  are live too. The arc **is wired.** (Smaller real item: confirm the `companion-reveal` puzzle seed row
  exists — the reveal *content* gate, separate from the producers.)

## 3. BUILT BUT NOT DEPLOYED — ready, just not on the server/host (only the jar is live)
- **Discord bot** (discord.js v14, tsc clean) + slash commands `/whisper /link /answer` → needs a host.
- **Showrunner cron** (snapshot→decide→apply, dry-run works) + clue-drip→card→`#the-record` poster → needs a scheduled cron.
- **The Record website** (Next 15: public archive, inscribe endpoint, un-redacting `v_record`, admin console) → not on Vercel.
- **Resourcepack** (rune font + 4 OGGs) + **reward-toast datapack** → not on the server, so runes fall back to ASCII and toasts can't fire.

## 4. WIRED BUT UNPROVEN — in the jar, never seen working live (the playtest list)
The 34-beat Haunting Engine · SignalTracker + 21 listeners · per-player illusions · the Lens + ignition +
oracle + answer-signs · all 9 non-typed puzzle producers + vault + Accepting + Seventh-choice · structure
generation (placeregion/placedeep) · FAWE/ModelEngine salvage paths · the companion/finale composers.
**One vertical-slice playtest proves ~15 of these at once.**

## 5. DONE + PROVEN (the reliability floor — keep)
The `requires_flags` storylet gate (self-test passes the full ignition→solve→unlock slice) · the
deterministic `decide()` salience/drip + AUTO⇄CONFIRM gate · clue-card rendering · migrations 0006/0007 ·
the puzzle seed + flag-graph + tiered hints + thread cards (applied live) · the session-zero script.

---

## THE ORDER TO CLOSE IT (proof, not compilation, is the bar)
1. **ETHAN — DB consistency check.** Regenerate/apply `discord/supabase/apply-all.sql`, then confirm the
   live DB has `puzzles.answer_kind` and the diverse rows. The generated bundle now owns both lineages
   and is checked by `npm run audit`; do not use `apply-tonight.sql`.
2. **ETHAN — stand up the bot + cron host** (Render instructions delivered) **and deploy the website**
   (Vercel). This single move **un-darkens half the game.**
3. **ETHAN — host the resourcepack + datapack** on the server, then run `/observance placeregion` + `placedeep` (0.2.2).
4. **CLAUDE + ETHAN — the FIRST vertical-slice playtest:** ignition → one rosetta/keeper solve → flag set →
   unlock beat → toast → clue drips to `#the-record`. **This is the gate.** It proves ~15 unproven layers.
5. **CLAUDE — wire the ThresholdVault roster supplier** (and confirm the companion-reveal seed row).
6. **DECIDE the 3 forgotten items** (LLM brain / `0008` Observer / keeper voices) — build or formally cut,
   but don't leave them as invisible holes.

**Rule (the consistency principle):** do not add new arc until the vertical slice is seen working
end-to-end live. The ledger's lesson is that we already have *more* built than proven — the next win is
**proof**, not more code.

---

## LOOP-TICK CORRECTIONS (2026-07-01, verified against live code)
- **Companion-reveal chain — NOT a gap.** The reveal is flag-driven: `WrenNpcListener` sets
  `companion_revealed` → `thread_cards.sql:303` surfaces a card gated on `companion:revealed`. The missing
  `companion-reveal` *puzzle row* is intentional (`metapuzzle_seed:293`: "harmless today"). No action.
- **ThresholdVault roster supplier — acceptable for week one.** There is no existing active-roster source;
  `AcceptingRiteListener` uses the same online-count fallback. For a small synchronous group, online-count
  ≈ the active roster, so this does NOT block week one. A true "active roster" (opted-in / dossier-bearing
  players) is a deferred deep-half design decision, not a bug. Downgraded from built_not_wired → deferred.

**Director status:** week-one *building* is essentially complete. The remaining path is DEPLOY (bot/cron on
Render, website on Vercel, packs on the server — all in progress) + the first VERTICAL-SLICE PLAYTEST. Per
the consistency principle, no new arc is being added until that slice is proven live.
- **Keeper voices (forgotten #3) — DONE (5cb42e4).** Generated `keeper_voice` (the SpatialVoiceBeat
  fallback that was referenced-but-missing → silent) + 6 per-keeper voices tuned to each keeper's
  identity; `sounds.json` maps them. Source-pack only; activates on the next resourcepack re-host.
  Remaining forgotten items are now just the two SCOPE DECISIONS: the LLM brain (#1) and 0008 Observer (#2).
