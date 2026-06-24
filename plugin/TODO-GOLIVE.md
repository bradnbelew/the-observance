# TODO — Go-Live (operator-only)

These steps cannot be done from the codebase — they need the real server, the real world seed, host
secrets, or a human decision. The plugin is built to run safely even before all of these are done
(it degrades to "offline / no beats" rather than erroring), so you can deploy incrementally.

## 1. Build + upload
- [ ] On a machine with JDK 21 + Gradle ≥ 8.10: `cd plugin && gradle wrapper && ./gradlew build`.
- [ ] Upload `build/libs/observance-0.1.0.jar` to **PebbleHost** `plugins/` (SFTP or panel file
      manager). Restart the server once so `config.yml` + `sites.yml` are written.

## 2. Secrets (host env — never commit)
- [ ] Set `OBSERVANCE_SUPABASE_KEY` to the Supabase **service-role** key in the PebbleHost panel
      (Startup → Variables) or the start script. Do **not** put it in `config.yml`.
- [ ] Confirm `supabase.url` in `config.yml` points at `https://<project>.supabase.co/rest/v1`.
- [ ] Verify with `/observance status` → `supabase configured: true`, `last db call ok: true`.

## 3. World seed → real site coords
- [ ] Lock the world seed for the live world.
- [ ] Stand at each intended site in-game, read F3 coords, and fill `sites.yml`
      (`bow_marker`, `offering_cairn`, `kept_light`, `report_lectern`; `keeper_stone` is Phase 1).
- [ ] Set each placed site `enabled: true` with real `x/y/z`. Run `/observance reload`.
- [ ] Sanity check: crouch on the bow marker, drop an item on the cairn, confirm `custom_compliance`
      rows appear in Supabase.

## 4. Supabase schema + content (the dashboard/bot side)
- [ ] Ensure the tables the plugin reads/writes exist with the expected columns:
      `players, dossiers, custom_compliance, heatmap_cells, bases, event_log` (writes);
      `beat_queue, settings, arc_state` (reads). Conflict targets the client upserts on:
      `players.mc_uuid`, `dossiers.mc_uuid`, `custom_compliance(mc_uuid,custom_key)`,
      `heatmap_cells(world,cell_x,cell_z)`, `bases.id`, `beat_queue.id`.
- [ ] Seed `settings.watcher_sleep` (boolean) — the remote master mute.
- [ ] Author all narrative payloads (book/sign/lectern pages, sounds, etc.) into `beat_queue.payload`.
      The plugin never invents story text; empty/garbled payloads just no-op.

### 4a. The Oracle (closed clue loop) — apply 0004 + seed puzzles
- [ ] **Apply `discord/supabase/migrations/0004_oracle.sql`** to the live Supabase (out-of-band:
      Supabase SQL editor or `supabase db push`). It is additive/non-breaking: it creates `puzzles`,
      `solves`, `answer_attempts`, and reconciles `beat_queue` (adds `mc_uuid` / `site_id` / `priority`,
      widens the status check to include `'failed'`, adds the actionable partial index). **Until this
      runs, the in-world answer-sign and the Discord `/answer` + `#the-record` scan can match NOTHING
      and the plugin cannot target a player** (the columns it reads/writes don't exist yet).
- [ ] **Seed `puzzles` rows** — each `puzzle_key` is the `forgeClue()` FNV-1a key; `accepted_answers`
      MUST be stored **already-normalized** (NFKC → lower → `[^a-z0-9 ]→space` → collapse → trim — see
      ORACLE.md §2). Coordinate answers lose the `-` sign, so store unsigned or include both forms.
      Set `outcome_type` + `outcome_payload` (a `voice_key` + optional `beat`/`next_puzzle_key`/`set_flags`);
      flip `active = true` only for puzzles currently open. Optionally set `max_attempts` per puzzle.
- [ ] Mark answer-sign sites in `sites.yml` with type `answer_sign` (or `keeper_stone`); optionally bind
      one puzzle to a site via `puzzle-key:` (omit to match the whole open web). Run `/observance reload`.
- [ ] Smoke test with ORACLE.md §8's 6-step script (right / wrong / dead-end / replay / spam / sign).
- [ ] Confirm `beat_queue.payload` is written as a jsonb **object** (the plugin reads it as a
      `JsonElement` via `BeatPayload.of` now — a quoted string would break it; this was the load-bearing
      fix that also rescued existing `whisper_toll` beats).

## 5. Resource pack (the rune font)
- [ ] Build/host the resource-pack that supplies the custom rune font + any `namedSound` resource-pack
      sounds the showrunner authors. Set `resource-pack` / `resource-pack-sha1` in the server's
      `server.properties` (or a forced-pack plugin). The plugin itself ships no pack; it only references
      sound keys / fonts that the pack must provide. Until then, `namedSound`/custom-font beats fall back
      gracefully (vanilla sounds / default font).

## 6. Pre-flight verification on the live server
- [ ] `/observance sleep on` then watch — confirm zero beats fire. `/observance sleep off` to resume.
- [ ] Insert a single test `beat_queue` row (e.g. `private_message`) targeting your own `mc_uuid`,
      status `approved`; confirm it fires once and the row flips to `fired`.
- [ ] Kill Supabase connectivity (bad key) briefly; confirm the server keeps running, players see
      nothing, `queued writes` climbs in `/observance status`, and it flushes on restore.
- [ ] Tail console for any `[*] swallowed:` lines after an hour of normal play — there should be none
      under ordinary conditions; each one is a logged-and-survived fault, not a crash.

## 7. Operator decisions / residual items (see the hardening notes)
- [ ] **Beat-spawned watcher mobs are persistent and not auto-swept on disable/reload.** If a long
      campaign accumulates `named_mob` watchers, cull them with a one-off command
      (`/kill @e[type=zombie,tag=…]` won't match PDC; use a Phase-1 cleanup beat or kill by custom name).
      Decide whether `named_mob` payloads should set a `despawn_seconds` ceiling by default.
- [ ] **`map_mark` allocates a new map id per fire** (vanilla Bukkit behavior). It's window-capped so
      the growth is slow, but for a multi-month world consider reusing a pool of map ids.
- [ ] **The offline write-queue is in-memory.** A crash/restart while Supabase is down loses queued
      writes (and can, in the worst case, re-fire a beat whose `fired` status never persisted). If you
      need stronger durability, add the Phase-1 disk spill. For normal operation (Supabase reachable)
      the DB status write is the durable replay guard.
- [ ] Decide final `drama.*` pacing for the live audience size before launch.
