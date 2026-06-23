# The Observance — Technical Design (unspoiled)

This document is safe to read. It describes *how the system works*, not *what the
story is*. Story lives in `arc/` (sealed).

---

## 1. Design thesis

The wow is never "an LLM wrote this." It's **"the world reacted to *me*, specifically."**
So:

- A **deterministic haunting engine** is the spine. It can't embarrass itself.
- The **LLM is a scalpel** — rare, text-only (never geometry/coords), schema-validated,
  with a **deterministic fallback** behind every call. API down = haunting continues.
- **Restraint is the mechanic.** ~90% silence. Big beats are rare and earned.
- **Soft-pressure:** the ARG is a current, not a leash. Ignoring a thread makes it go
  quiet, not louder. No quest markers, no countdowns, no harassment.

---

## 2. Subsystems

### 2.1 Signal Tracker (the dossier) — deterministic, persistent
Plain code. Listens to events and aggregates per-player **signals**, persisted to Supabase
(and `PersistentDataContainer` as a local cache). This *is* the dossier; the "it knows me"
feeling comes from real measurement, not the model.

Tracks e.g.: solo-mining time, deaths, hoarded items, distance from group, time-of-day
patterns, chat sentiment, and **custom compliance** (see 2.4). Feeds everything downstream.

Key hooks: `BlockBreakEvent`, `EntityDeathEvent`, `PlayerDeathEvent`, `AsyncChatEvent`,
sampled `player.getLocation()` (scheduled, **not** `PlayerMoveEvent`), inventory scans.

### 2.2 Traffic & Territory Map — so beats land where they're seen
- **Heatmap:** scheduled location sampling → coarse grid cells → visit counts (persisted).
  Beats prefer hot cells.
- **Base detection:** cluster `BlockPlaceEvent` positions + bed/respawn anchors
  (`PlayerBedEnterEvent`) + container density → "this is Dana's base at X,Z."
- **Predictive placement:** sample a player's facing/velocity, place a beat ~30 blocks
  down their path, **out of line of sight**, so they *discover* it.
- **Reveal discipline:** only mutate a block when no player `hasLineOfSight()` to it.
  Discovered, never witnessed appearing. Kills 90% of "it's obviously fake."

### 2.3 Haunting Engine — pacing + the beat library
- **Drama budget** (the L4D Director, no ML): build → peak → forced quiet. Hard cooldowns
  and a per-window cap so it physically cannot spam.
- **Curated beat library** of cheap, instant, can't-glitch surfaces: signs, lectern books,
  a single named mob, per-player sounds (`player.playSound`), particles, dousing torches
  (`WALL_TORCH`→air/`REDSTONE_WALL_TORCH`), opening doors, subtitles/titles.
- Every beat is ephemeral where possible (doused torches relight, temp mobs despawn). No litter.

### 2.4 The Custom System — the ARG's "do real things" layer
The land has **customs**; the plugin detects compliance via real events/state and feeds
the tracker. Customs are **legible through the Archivist reports** (2.6), never a tutorial.
Each custom = a rule + a detection mechanic + an optional escalation-to-consequence.

Detection patterns (all real Paper):
- gesture (crouch = bow) → `PlayerToggleSneakEvent` near a marker
- offering → first-ore `BlockBreakEvent` + item left at a cairn/altar
- time/moon taboo → `PlayerBedEnterEvent` / `BlockBreakEvent` + moon phase (`fullTime/24000 % 8`)
- depth taboo → `BlockBreakEvent` + `block.getY()` threshold
- forbidden word → `AsyncChatEvent` scan
- ward / kept light → block-presence scan in the detected base
- protected mob → `EntityDeathEvent` on a tagged entity
- container/covering → `Openable` state check at dusk

> Full authored list + which escalate to real punishment lives in `arc/` (sealed).

### 2.5 The Scalpel (LLM) — rare, validated, optional
One job: when a signal is **strong and unambiguous**, personalize one beat (a report or
journal that references what a specific player actually did) using verified facts from
the tracker.

- **Structured output only.** Model returns `{lines:[...]}` / `{pages:[...]}` — never blocks,
  never coordinates.
- **Validate every byte:** length, tone, banned words (no modern slang, no "AI", no
  fourth-wall breaks). Fail → deterministic fallback line. API down → fallback. Nobody can tell.
- **Precision over recall:** only call someone out when the signal is overwhelming. A wrong
  callout is worse than a generic one.
- **Models:** local Ollama for cheap/frequent; **Claude API** for marquee beats.
- Model call runs **async** (`runTaskAsynchronously`); the resulting world write is scheduled
  back to the **main thread** (`runTask`) — world edits must be on the main tick.

### 2.6 The Archivist Reports — how customs become legible
The presence keeps a **record**. Players find reports (lecterns in-world, and in Discord)
that **name players and name violations** — and they're always *true*, because the tracker
measured them (grounding). They escalate from observation → warning → consequence.

> *"Seven days kept. The one called Marcus has not given back to the deep, not once.
> They have been told. If they will not keep the ways, the ways will not keep them."*

This is the tutorial, the social pressure, and the dread in one artifact.

### 2.7 Whispers — the Discord hint economy
Hard puzzles are safe because the backstop is player-controlled, rationed, and diegetic.

- Discord `/whisper <which puzzle>` → checks the **budget ledger** (Supabase) → returns the
  next **pre-authored hint tier** (vague → specific) in character. Pre-authored so the bot
  can't hallucinate a wrong hint.
- **Limited:** ~3 per Act, refreshing each Act; **earnable** by finding optional lore;
  **auto-gifted** if the dossier sees the group truly stalled (never a hard-stall).
- **Cost:** each spend fires an in-game **toll** (atmospheric, reversible — takes *warmth*,
  not *progress*) and ticks the asker's **bond** on the ledger.
- The bond ledger secretly drives the endgame casting. (See `arc/`.)

### 2.8 Discord Bridge — one fiction, two surfaces
- Transport is off-the-shelf (webhook/bot). Plugin emits events → bot → Discord; bot
  commands → bridge → plugin.
- **Congruence rule — cross-surface handoffs:** a clue that *starts* in one surface
  *resolves* in the other (in-game coordinate → Discord bot → cipher → back to a location).
  The crossing is what makes them feel like one world.
- TINAG: the bot never breaks character.

### 2.9 Admin Dashboard (Vercel)
- **Author mode:** full spoilers, tuning, beat preview/approval queue.
- **Spoiler-free mode:** health, heatmap, error log, "is it misfiring" — **no story.**
  Ethan runs this and plays as a genuine participant.

### 2.10 The Autonomous Showrunner (VPS, scheduled)
A Claude Agent SDK process, cron'd to wake **between** sessions: reads game state + dossiers,
authors/tunes upcoming content, deploys. Not a live puppeteer — a showrunner on the slow,
soft-pressure cadence. This is the "autonomous me, kept in the dark" layer.

---

## 3. The anti-jank contract (every beat must pass)

1. **LLM writes text only**, never geometry/coords. Worst case = an off sentence → fallback.
2. **Placement validation:** support/attachment check (no floaters), surface raycast
   (no blocks in rock/air), context gating (douse torches only where torches exist).
3. **Reveal discipline:** mutate only outside line of sight.
4. **Curated, not generative:** Phase-2 structures are pre-built, footprint-checked schematics.
5. **Graceful degradation:** engine deterministic; LLM is garnish.
6. **Precision over recall** on personalization.
7. **Admin preview mode** until the system has earned unattended trust.
8. **Cleanup + repetition guard:** ephemeral beats revert; per-beat cooldowns.
9. **Idempotent + persistent:** survives restarts; never replays/double-fires.
10. **Decency floor:** never permanently destroy hard-won progress; tolls are reversible.

---

## 4. External pieces (researched, license-checked)

- **Simple Voice Chat** plugin API — `MicrophonePacketEvent` + bundled Opus decoder (Phase 3).
- **Citizens2** — server-side NPC framework (the Keeper). (Phase 2)
- **FastAsyncWorldEdit** — async schematic pasting off the main thread. (Phase 2)
- **PacketEvents** — per-player fake surfaces (sounds/blocks only one player sees). (Phase 2)
- **From The Fog** (CC BY-NC-SA) — *design reference only* for the creepy-surface vocabulary.
- **langchain4j** — JVM LLM glue with structured output (optional; can also call Claude over plain HTTP).

---

## 5. Build setup (next artifact)

Gradle + Paper, Java 21. `plugin.yml`, a main `JavaPlugin`, the subsystem packages above.
Phase 0 has no external deps beyond Paper + a Postgres/SQLite driver. Target: a running
plugin that tracks signals, builds the heatmap, fires 3–4 ambient beats, and detects 2–3
customs with reports — deployable to PebbleHost with no AI.
