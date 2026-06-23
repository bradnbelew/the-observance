# The Observance — Phase 0 plugin

A deterministic, fault-isolated "soft-pressure haunting" plugin for **Paper 1.21.x** (Java 21).
It watches players (the *dossier* / Signal Tracker), and enacts sparse, restrained, reversible
"beats" pulled from a Supabase `beat_queue` that a dashboard/Discord bot fills. There is **no AI in
Phase 0** — every decision is a deterministic gate. The owner's hard rule is honored throughout:
**nothing crashes, nothing desyncs, nothing can be farmed/griefed.**

---

## 1. Build

The wrapper jar/scripts are intentionally not committed — generate them once, then build:

```bash
cd D:/the-observance/plugin

# 1) one-time: materialize the Gradle wrapper (needs a local Gradle >= 8.10 on PATH)
gradle wrapper

# 2) every build thereafter
./gradlew build        # Windows: gradlew.bat build
```

The shaded jar lands in `build/libs/observance-0.1.0.jar`. Copy that into the server's `plugins/`.

Notes:
- `paper-api` and `gson` are `compileOnly` (Paper provides both at runtime). The `shadow` plugin is
  applied but only *relocates* Gson if you flip its dependency to `implementation` — by default the
  produced jar carries no third-party classes.
- If you cannot fetch the shadow plugin offline, comment out the `id 'com.github.johnrengelman.shadow'`
  line in `build.gradle`; the plugin compiles against Paper's bundled Gson regardless.
- Target/sources are Java 21. Use a JDK 21 toolchain.

---

## 2. Configure

Two files are written to `plugins/Observance/` on first run: `config.yml` and `sites.yml`.

### 2.1 Supabase + the service key (NEVER commit the key)

`config.yml → supabase`:

```yaml
supabase:
  url: "https://<project>.supabase.co/rest/v1"
  service-key-env: "OBSERVANCE_SUPABASE_KEY"   # env var name (preferred)
  service-key: ""                              # leave EMPTY in any committed file
```

Resolution order at runtime (first non-empty wins): **environment variable** named by
`service-key-env`, then the `service-key` config value. **Set the env var on the host** — on
PebbleHost set `OBSERVANCE_SUPABASE_KEY` in the panel's *Startup / Variables* (or export it in the
start script) with the **service-role** key. The key is sent as both `apikey` and
`Authorization: Bearer …`, is held privately, and is **never logged**.

If the key/url are absent the plugin runs fine — it just degrades to "offline": reads return empty,
writes are queued (bounded) and flushed when connectivity returns. No errors ever reach players.

### 2.2 The knobs that matter

- `beat-queue.poll-interval-seconds` / `max-per-poll` — how often/how many beats per async poll.
- `drama.*` — the pacing director. `window-max-beats` per `window-minutes` is the hard ceiling on
  ALL beats; `personalized-cooldown-minutes` is floored at 20; ambient beats have a global + per-player
  cooldown. Restraint is intentional — leave these sparse.
- `reveal.witness-radius` / `retry-*` — "discovered, never witnessed appearing." Block mutations only
  fire when no player has line-of-sight within the radius; otherwise they retry then silently abandon.
- `protection.*` — beat-placed and protected-site blocks resist break/burn/explode/piston/liquid.
- `tracker.*` — lore-agnostic tuning only: forbidden words, ore set, hoard weights, the Deep Line Y,
  the Sacred-Beast PDC key, base-detection thresholds. **No story text lives here.**

### 2.3 Sleep / kill switch

- Remote: set `settings.watcher_sleep = true` in Supabase → suppresses every beat server-wide.
- Local: `/observance sleep on` → same, without touching the DB (the operator's reliable mute).
  A DB hiccup never silently mutes the game (the remote check fails *open*); the local switch is the
  guaranteed off.

---

## 3. How the dashboard / bot drive beats

The plugin is a **consumer** of `beat_queue`. Anything that inserts a row there (the dashboard, the
Discord bot, a SQL function) can drive the world. Each tick the async poller:

1. checks local + remote sleep, then fetches rows with `status in (pending, approved)` ordered by
   `priority desc, created_at asc` (capped by `max-per-poll`),
2. hops to the **main thread**, validates the row into a `BeatRequest` (resolves the target player by
   `mc_uuid`, the site by `site_id`, parses `payload` JSON), gates it through the drama budget, and
   enacts the matching beat,
3. hops **back to async** to PATCH the row `status = fired | skipped | failed` + `decided_at`.
   An unknown `type` is left **queued** (`UNHANDLED`) for a future build — never marked fired.

A row looks like:

| column | meaning |
|---|---|
| `id` | row id; the in-process + DB idempotency key |
| `type` | beat-library key, e.g. `whisper_toll`, `lectern_fill`, `door_open` |
| `status` | `pending` / `approved` → actionable |
| `mc_uuid` | target player (per-player beats); null for world/ambient |
| `site_id` | a `sites.yml` site id (world-located beats) |
| `payload` | per-beat JSON — **all story text lives here, never in code** |
| `priority` | higher fires first |

Example payloads (full catalog in the BeatLibrary manifest):

```jsonc
// whisper_toll  — reversible cold toll
{ "sound":"AMBIENT_CAVE", "pitch":0.6, "darkness_seconds":3, "torch_relight_seconds":30,
  "actionbar":"the warmth dims" }

// lectern_fill / book_appears
{ "dest":"chest", "title":"Record", "author":"the record", "pages":["…","…"] }

// unlock  — dispatcher that delegates to any other beat
{ "step":"door_open", "step_payload": { "radius":3, "open":true } }
```

The 23-beat catalog spans TEXT (lectern/book/sign/relabel/map), CLUE (chest arrange), ITEMS (swap),
WORLD (torch gutter, door, decay, small structure), MOBS (named watcher, sacred animal), per-player
SENSORY (sound/particle/message/darkness/boss-bar/fake-block/time-shift), ACK (advancement toast),
and DIRECTED specials (whisper toll, unlock). Every world beat is reveal-disciplined, placement-
validated, idempotent, and reversible/decency-floored (empty-slot-only, lossless swaps, relights,
all-or-nothing pastes).

---

## 4. Filling `sites.yml` (after the seed is chosen)

Sites ship with **placeholder (`null`) coordinates** and are silently skipped until placed. Once the
world seed is locked and you've stood at each real location in-game (F3 for coords):

```yaml
sites:
  bow_marker_01:
    type: "bow_marker"
    world: "world"
    x: 123.0        # replace the nulls with real coords
    y: 64.0
    z: -88.0
    radius: 6
    protect: true
    enabled: true
```

- `bow_marker` — The Bow (crouch near it). `offering_cairn` — The Offering (drop after first ore).
  `kept_light` — a home zone scanned for a burning light after dark. `report_lectern` — where the
  first record appears. `keeper_stone` — reserved (Phase 1), shipped `enabled: false`.
- A site is "placed" only with real x/y/z **and** `enabled: true`. Unplaced customs no-op; no errors.
- After editing, run `/observance reload` (no restart needed). The Bow/Offering customs and the
  protection snapshot pick up the new coords immediately.

---

## 5. Admin command

`/observance` (alias `/obs`, permission `observance.admin`, default op):

- `status` — Supabase configured?, last-call ok?, queued-write count, local sleep, placed sites,
  drama enabled.
- `reload` — reloads `config.yml` + `sites.yml` and rebuilds the dependent subsystems.
- `sleep <on|off>` — toggle the local mute.

---

## 6. Reliability guarantees (what hardening enforces)

- **No crash:** every listener body, scheduled task, command, and beat enact runs inside `Safety`
  (logs to console + `event_log`, then swallows). Nothing propagates to the server tick.
- **Threading:** every Supabase/network call is async; every world read/mutation is main-thread.
  The async-chat handler touches no world objects.
- **Graceful degrade:** Supabase down → reads empty, writes queued (bounded, drop-oldest), beats just
  don't fire. Players see nothing wrong.
- **No grief:** beat-placed + protected-site blocks resist break/burn/explode/piston/liquid/enderman.
- **No farm:** player-triggerable customs (Bow/Offering) are per-player+site cooldown-limited.
- **No replay:** in-process idempotency (beat id) + durable DB status (queued on failure) guard against
  double-fires within a run and across restarts.
- **No pop-in:** reveal discipline + placement validation on every world beat.

See `TODO-GOLIVE.md` for the operator-only steps that remain.
