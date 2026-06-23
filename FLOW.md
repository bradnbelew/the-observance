# The Observance — Flow, Triggers & Progression (unspoiled)

How the pieces fire and how a ~2-week run paces. Safe to read — mechanics, not story.
(Story beats live in `arc/`.)

---

## 1. Trigger map (signal → response)

The engine never reacts to raw events — the Signal Tracker digests them, then a
**rule layer** (drama budget + cooldown + confidence) decides if/what fires. Examples:

| Trigger (digested signal) | Gate | Response (beat) |
|---|---|---|
| Player solo-mining ratio > 0.7, hoarding | high confidence + budget | personalized **report/journal** (scalpel) |
| Forbidden word in chat (`The Unspoken`) | always (cheap) | immediate ambient: lights flicker + sound, to that player |
| Base detected + dusk + no lit light (`Kept Light`) | once/night | cold beat at base (sound/particle) |
| First ore of session + no offering (`The Offering`) | once/session/player | "the deep goes dark" toll next descent |
| Pass marker without crouch (`The Bow`), repeated | escalation threshold | marker mob gains a name + "watches" |
| Group stalled on a cipher (Whisper budget unused N days) | backstop | Watcher **auto-gifts** a tiered hint |
| ≥4 of 6 fragments collected + final cipher solved | act gate | **Act 3** unlock (Keeper summons) |

**Soft-pressure rule:** an unmet gate never nags. The thread goes quiet and the hook
re-surfaces later, tied to something the group is already doing.

---

## 2. Two-week progression & cadence

Day ranges are *expected pace*, not timers — acts advance on **behavioral gates**, and
the world waits if the group is slow.

### Act 1 — Establishment (≈ Days 1–5)
- Drama budget **low**. ~1–2 tiny ambient beats per session. Deniable.
- Signal Tracker + heatmap + base detection quietly build.
- First **report** appears ~Day 3–4 (a lectern naming one player's habit).
- Discord: silent, or one cryptic line near the end ("the records have begun").
- **Gate 1→2:** a player has found the first report/journal **and** total group playtime
  threshold met.

### Act 2 — The Ways (≈ Days 6–11)
- Drama budget **rises**. ~2–4 beats/session; reports now name people + violations.
- Customs become discoverable by consequence; rune-language clues, map-art, first cipher.
- **Whispers go live** (Discord). Discord becomes the group's "list of laws" + the first
  cross-surface handoff (in-game coord → bot → cipher → back in-world).
- **Gate 2→3:** ≥4 of 6 fragments assembled **and** the final-coordinates cipher solved.

### Act 3 — The Accepting (≈ Days 12–14)
- Clues converge. Gather named components + wake the Keeper (NPC) + altar deposit.
- The ritual fires on a chosen night (~Day 14): proximity + time + group crouch.
- Discord: the Keeper's final summons. Outcome bends to the group's record — the WHOLE group is received (kept or cast out); no single "chosen" player.

### Event rhythm (so it's consistent, never spammy)
- **In-game:** ≤1 ambient beat per ~hour of play; ≤1 big personalized beat per session
  (only on strong signal); **hard cooldown ≥20 min** between beats.
- **Discord:** ≤1 Watcher-initiated post per day (a report excerpt, a fragment, or a
  cryptic line), plus on-demand Whisper replies. Restraint is enforced by the budget.

---

## 3. Whispers — the hint system, end to end

```
Discord /whisper <puzzle>
   → check ledger (Supabase): budget = 3/Act, + earnable (find optional lore),
                              + auto-gift backstop if truly stalled
   → return next PRE-AUTHORED hint tier (vague → specific)   [no hallucination]
   → fire in-game TOLL (atmospheric, reversible — takes warmth, not progress)
   (a bond tally is recorded as a NEUTRAL tracker only — no "chosen" player; the Accepting judges the whole group)
```

- Hard puzzles are safe because the backstop is player-controlled, rationed, diegetic.
- Tiers + budget per puzzle are authored in `arc/`. The bot never invents a hint.

---

## 4. Dashboard control surface

**Author mode** (full spoilers — for you while tuning, or a read-in guardian):
- View / advance / rewind the **arc act**; see act gates and progress.
- **Beat queue:** preview, approve, force, or skip queued beats (the anti-jank gate).
- Edit **Whisper budgets**; see the **bond/compliance tally** (a neutral tracker — no individual "chosen").
- See **dossiers**, **heatmap**, custom-compliance per player.
- **"Watcher sleeps"** toggle (mute everything for a sensitive session).
- Manually **trigger the Accepting** (for testing) or place a specific clue.

**Spoiler-free mode** (what Ethan runs while playing unspoiled):
- Health: last-beat time, error log, "is it misfiring", API/Whisper status.
- Heatmap + base map (no story labels).
- Player **compliance counts** shown as neutral numbers, not named customs.

---

## 5. Build plan (task order — Phase 0 first)

1. Gradle Paper plugin scaffold (Java 21, `plugin.yml`, main class, package layout).
2. Signal Tracker: event listeners + per-player counters → SQLite/Supabase.
3. Traffic & Territory map: scheduled location sampling → heatmap; base detection.
4. Drama budget + cooldown scheduler.
5. Curated beat library: signs, lectern books, named mob, per-player sound, torch swap.
6. **Reveal + placement validators** (anti-jank contract).
7. Custom detection: start with 3 (`The Bow`, `The Unspoken`, `Kept Light`) + reports.
8. → Deploy to PebbleHost, playtest the haunting with **no AI**.

*Phase 1+ (scalpel, Whispers/Discord, dashboard, showrunner, NPC/structures, voice)
expand after Phase 0 proves the world. Detailed task breakdown: next pass.*
