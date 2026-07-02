# THE OBSERVANCE — OPERATOR'S RUNBOOK (running it with your friends)

> The single guide for setting up and running a real session. Supersedes GO-LIVE-TONIGHT.md.
> Honest status of every system is in §6. Server = Paper **1.21.11** on Crafty (brother's PC).

---

## 1. ONE-TIME SETUP

### 1a. Database (Supabase, project `fdnmhbpxnodrnbrzrlqq`)
- SQL Editor → paste **`discord/supabase/apply-tonight.sql`** (the whole file) → Run. It applies, in order:
  0005 (missing tables) → 0006 (the flag gate) → the 8 seeds → **schema-repair.sql** (the plugin↔DB
  drift fix that stops the tracker 400s). Idempotent — safe to re-run.
- Grab your **`service_role`** key (Project Settings → API → `service_role`, the *secret* one).

### 1b. Server (Crafty)
- Create a **Paper 1.21.11** server; give it Java 21 + 3–4 GB RAM.
- **Plugin:** upload `plugin/build/libs/observance-0.1.0.jar` to `plugins/`.
- **Key:** in `plugins/Observance/config.yml`, set `supabase.service-key: "<service_role key>"`
  (leave `service-key-env` alone). The `url` is already filled.
- **Datapack:** upload the `datapack/observance/` folder into the world's `datapacks/` folder
  (`world/datapacks/observance/`). This carries the Undercroft fog dimension + the reward-toast
  advancements. Run `/datapack list` in console to confirm it's enabled (or `/reload`).
- Start the server; console should show **Observance** enabled with no errors.

### 1c. Verify the DB link
- In game (op yourself first: console `op <name>`), run `/observance status`.
  Want: **`supabase configured: true`**, **`last db call ok: true`**, **`queued writes: 0`**.
  If not, the key is wrong or the SQL didn't run — fix before continuing.

## 2. BUILD THE WORLD (zero manual building)
- Stand at the **center** of a flat-ish open area and run **`/observance placeregion`** (default
  12-block spacing; `/observance placeregion 16` for wider). It stamps the rune-ring + six **crafted
  per-keeper set-pieces** — each themed to its keeper (Vaun's rotting copper treasury, Mara's
  reading-room, Sella's reflecting pool with a child's cairn, Orin's stoop-under-the-lintel threshold,
  Brann's watch-fire, Iss's false-warm hearth) — each a live answer site, **persisted to `sites.yml`**
  (survives restarts). IDs match the spine entries, so it fills them; no duplicates.
- **Submitting answers:** edit the **blank** sign (the *labelled* sign is waxed and won't take input,
  so flavour text can't pollute your answer). For one stone: `/observance placeroom <keeper>`.
- **The deep half:** run **`/observance placedeep`** to stamp the descent's payoff sites — the cold
  false-hearth, the Accepting floor, the co-op **vault**, the **future-dated grave that opens from
  inside**, and the **Seventh's chamber** (these host the finale rite, the co-op vault, and the
  reckoning; some choice-markers still need hand-placing).
- **The Undercroft dimension** (datapack) is a real descendable dark cavern — reach it via Multiverse:
  `/mv create undercroft NORMAL -g observance:undercroft`.
- **The companion:** `/observance wren spawn` places Wren (Citizens if installed, else a fallback body).

## 3. SESSION ZERO (before the friends join the fiction)
- Read **`design/SESSION-ZERO.md`** — the out-of-fiction consent + onboarding script. Cover: this
  server watches (in-game behavior; later chat/voice), you can opt out anytime, there's a debrief.
  Veteran group — being watched can land hard, so this is not optional.

## 4. RUN THE FIRST SESSION
1. **Ignite** the arc — either admin `/observance flag set prologue_ignited true`, OR in-world (real
   trigger, built): a player right-clicks a **lectern** at a report-lectern site, or walks up and
   right-clicks at the **rune-rosetta**. Either sets `prologue_ignited` and the first puzzle goes live.
2. Players **read the rune ring** and submit answers by **editing an answer-sign** (clear the sign, type
   the answer, Done). First gate: the rune ring →
   `bow offering kept light deep line unspoken sacred beast`.
3. Solving the ring opens the **six keeper stones** (any order). Each keeper's cipher + hint is seeded.
   The spine runs: keepers → the Iss catch → the descent → the Seventh. Salience surfaces one thread at
   a time; the hint rail escalates if a thread stalls.
4. **Watch for** (this is playtest data): findability (dead air?), whether a scare/toast lands, whether
   people know *how* to answer, and pacing. Capture it — that's what turns "it runs" into "it's good."

## 5. ADMIN CONTROLS + TROUBLESHOOTING
- `/observance status` — health (db, queue, sites, reckoning).
- `/observance flag <set|clear|list> [key] [true|false]` — drive the storylet gate (e.g. force ignition).
- `/observance placeroom <keeper>` / `/observance placeregion` — stamp the keeper spine.
- `/observance placedeep` — stamp the deep-half payoff sites (hearth, accepting floor, vault, grave,
  Seventh's chamber, reckoning stone).
- `/observance wren spawn|despawn|reckoning` — the companion NPC + his reckoning-choice markers.
- `/observance lens give [player]` — give the Lens (second-sight) item.
- `/observance reload` — reload config.yml + sites.yml.
- `/observance sleep <on|off>` — mute the watcher locally.
- **Nothing resolves on a sign** → check `status` shows db `true` + `sites placed ≥ 1`; make sure the sign
  has ONLY the answer (no placeholder text) and is inside the site radius.
- **Console red `tracker.flush.dossier` 400s** → you skipped schema-repair; re-run apply-tonight.sql.
- **Toasts don't pop** → the datapack isn't installed/enabled (`/datapack list`).

## 6. HONEST STATUS — what works vs. the real ceiling (read before promising friends anything)
**Works now (built + green this wave):** plugin builds/runs on 1.21.11; DB connected (drift fixed);
the full puzzle loop; **65 diverse puzzles** (behavior/object/code/coords/spoken + the ciphers) with
**producer listeners** so the non-typed ones are solvable in-world; **crafted per-keeper set-pieces**
plus the **deep-half structures** (Accepting floor, the future-dated grave, the Seventh's chamber, the
co-op vault) via `placeregion`/`placedeep`, persisted; **per-player illusion** (name-on-wall, the Lens,
reflection, watch-dimmed light, per-player fog); **in-world hint delivery**; the **asymmetric co-op
vault**; **Observer Tier-0** (behavior-only "it knows you" — grounded implication, rare); **Wren's
companion betrayal arc live** (NPC + producers + the reckoning choice); the **finale rite** (restore/
erase the Seventh's name); the fog **Undercroft dimension** + reward-toast advancements; the **record
website** (deployable); the reworked+registered beats (reveal→per-player, room-swap→teleport,
spatial-voice→positional, keeper-npc). **Every check green** (plugin jar · discord ×7 suites · datapack).

**Built but only the live server can *prove*:** the illusion visuals, the co-op partition with a real
group, the companion arc end-to-end, the Undercroft generation, the deep-site rites. All compile +
self-test; in-world behavior is unproven until you run it (that's what Playtest 1 is for).

**The honest ceiling — genuinely needs you / more infra:**
- **Audio** — zero OGG files; sound + biome-mood beats are silent until sound design is sourced (I can't
  generate game audio).
- **Observer Tiers 1–2** (the chat/voice archivist that quotes real words back) — needs the always-on
  hosted bot + Whisper + LLM budget. Tier-0 (behavior) is built; 1–2 are the hosting lift.
- **World arrangement** — `placeregion`/`placedeep` drop set-pieces in rows on flat ground; weaving them
  into an evocative landscape / re-dressed ancient-city + trial-chamber is hand-craft (or a later
  generator pass). The surface town (Aro/Wenna/Dob/Pell) NPCs aren't placed.
- **Small polish (P2):** the desire-path grave (needs a heatmap read-path), the deep-site choice-markers
  need hand-placing, the black-moon toll's temporal *gate* (works, just not restricted), `bases` id-type.

**Bottom line:** this is no longer "an escape-room with good writing" — the **haunting layer is built**
(per-player illusion, Observer Tier-0, the companion betrayal, the co-op vault, the reunion rite). What
stands between it and a *great* run is now **live-server tuning + audio + hosting the voice layer**, not
missing features. The full remaining backlog is in `design/IMPROVEMENT-AUDIT.md`. Playtest it.
