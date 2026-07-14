# The Observance — PLAYTHROUGH 0: DIRECTOR SETUP (everything before day one)

> The literal, ordered shooting-script of every setup action from cold repo to "ready for the
> friend group." This is the **content itself**, not a description of it — sourced from the REAL
> repo artifacts and quoted. `[GAP — TO BUILD]` marks anything not yet in the repo (never invented
> as if built). Authority: `design/WEB-MASTER.md`, `design/BUILD-MANIFEST.md`,
> `design/structures.md`, `design/PROGRESSION-LANES.md`, `plugin/src/main/resources/{sites,config}.yml`,
> `discord/.env.example`, `dashboard/.env.example`. Where this disagrees with those, they win.
>
> **Director note (the through-line):** the friends install nothing but ONE auto-pushed resource pack
> (Path A). Everything below is the operator's job — rotate keys, apply SQL, host the pack, build the
> world in-game, fill the coords, publish the lure page, deploy the dashboard, configure Discord — done
> ONCE, in order. Many steps are GO-LIVE residue that need a Minecraft client and cannot be done in the
> repo as text.

---

## STAGE A — SECRETS TO ROTATE (do this FIRST; a live key is committed)

### A1. ROTATE the Supabase service-role key (CRITICAL — it is committed in the clear)
- **WHAT IT IS:** `dashboard/.env.local` (a committed, non-ignored file) contains a **live** project URL and
  BOTH keys. Verbatim:
  ```
  NEXT_PUBLIC_SUPABASE_URL=https://fdnmhbpxnodrnbrzrlqq.supabase.co
  NEXT_PUBLIC_SUPABASE_ANON_KEY=<redacted; retired credential removed>
  SUPABASE_SERVICE_ROLE_KEY=<redacted; retired credential removed>
  ADMIN_EMAILS=bradnbelew@gmail.com
  ```
  The `service_role` key bypasses RLS. The same project ref (`fdnmhbpxnodrnbrzrlqq`) is also hardcoded in
  `plugin/src/main/resources/config.yml` line 10 (`supabase.url`).
- **WHERE:** Supabase project `fdnmhbpxnodrnbrzrlqq`.
- **DIRECTOR ACTION:** In the Supabase dashboard → **Project Settings → API → "Reset" the service_role JWT**
  (and the anon key if exposure is a concern). Then: (1) delete the new key from `dashboard/.env.local` and
  any committed file; (2) put it ONLY in untracked env per the resolution order below; (3) confirm
  `.env.local` and `discord/.env` are gitignored before any commit. The repo's own config warns this:
  `config.yml` line 12 — *"SERVICE-ROLE KEY — NEVER hardcode here, NEVER commit."*

### A2. The plugin's key resolution (where the rotated key goes for the Paper plugin)
- **WHAT IT IS:** `config.yml` lines 13–19 — the runtime resolution order:
  > Resolution order at runtime (first non-empty wins): 1. environment variable named by `service-key-env`
  > below; 2. this `service-key` value (leave EMPTY in committed config...). The key is sent as BOTH the
  > `apikey` header and `Authorization: Bearer <key>`.
  Key: `service-key-env: "OBSERVANCE_SUPABASE_KEY"`, `service-key: ""`.
- **DIRECTOR ACTION:** On the server box, set env `OBSERVANCE_SUPABASE_KEY=<rotated service-role key>`. Leave
  `config.yml` `service-key: ""` committed-empty. Never fill `service-key` except on a gitignored,
  locked-down server file.

### A3. The Discord service secrets (`discord/.env`)
- **WHAT IT IS:** `discord/.env.example` (copy to `discord/.env`, never commit) — the keys:
  `DISCORD_BOT_TOKEN`, `DISCORD_APP_ID`, `DISCORD_GUILD_ID`, `CHANNEL_THE_RECORD`, `SUPABASE_URL`,
  `SUPABASE_SERVICE_ROLE_KEY`.
- **DIRECTOR ACTION:** Create the Discord app (§J), copy `.env.example` → `.env`, fill all six with the bot
  token, app id, guild id, the `#the-record` channel id, the project URL, and the **rotated** service-role
  key. Confirm `discord/.env` is gitignored.

### A4. The dashboard service secrets (`dashboard/.env.local`)
- **WHAT IT IS:** `dashboard/.env.example` keys: `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`,
  `SUPABASE_SERVICE_ROLE_KEY`, `ADMIN_EMAILS`.
- **DIRECTOR ACTION:** Rewrite `dashboard/.env.local` (or set Vercel env vars) with the rotated keys; set
  `ADMIN_EMAILS` per §H2. The committed example's guidance: *"Comma-separated allowlist of admin emails
  permitted into Author mode. Use the email of the Supabase project owner (Braden)."*

---

## STAGE B — SQL APPLY ORDER (migrations, then the new 0006, then seeds)

> Apply against the (post-rotation) Supabase project. **Order is load-bearing** — seeds reference columns the
> migrations create, and `metapuzzle_seed.sql` + `progression_seed.sql` reference `puzzles.requires_flags`
> which lives ONLY in the **not-yet-authored 0006** migration.

### B1. Apply the dashboard migrations (the record/auth schema)
- **WHAT THEY ARE (`dashboard/supabase/migrations/`):** `0001_init.sql`, `0002_seed.sql`, `0003_lockdown.sql`
  (carries `ADMIN_EMAILS`-gated RLS lockdown + `v_record` read path the lure page reads).
- **DIRECTOR ACTION:** Apply 0001 → 0002 → 0003 in order (Supabase SQL editor or `supabase db push`).

### B2. Apply the discord/engine migrations (existing, in order)
- **WHAT THEY ARE (`discord/supabase/migrations/`):**
  1. `0003_discord.sql` — *"The Observance — Discord service schema"* (players, budgets, link).
  2. `0004_oracle.sql` — *"Oracle schema (the non-linear clue web)"* (the `puzzles` table + the answer loop).
  3. `0005_threads.sql` — *"The reconstruction layer (the Recovery Archive), the side-quest breadth, the
     discover-by-punishment custom state, and the NPC dialogue state. All additive."* (`side_quests`,
     `npc_dialogue_state`, `npc_quests`, `thread_*`).
- **DIRECTOR ACTION:** Apply 0003 → 0004 → 0005 in order.

### B3. Author + apply `0006_*.sql` — **[GAP — TO BUILD]** (the activation-lane + back-half schema)
- **WHAT IT MUST BE (`BUILD-MANIFEST §5` precondition 0.8; NO file `discord/supabase/migrations/0006*` exists
  — verified):** the migration that the staged seeds depend on. Required columns/tables, verbatim from the
  manifest:
  > `arc_state.ending_fate text`, `ending_codicil boolean`; `coop_gate_legs`; `group_restraint_state`;
  > `player_visited_cells`; `keeper_record jsonb` on `showrunner_state`; `grave_state`; the single
  > Accepting-instant column; `reckoning_*`; **`puzzles.requires_flags jsonb`** (the activation lane, 0.8);
  > **`world_paste_ledger`** `(id, world, site_id, schematic, base_x/y/z, pasted_at)` UNIQUE
  > `(world, site_id, schematic, base_x, base_y, base_z)`; **`voice_watchlist`** (derived/self-tested).
  Plus the progression flags (`BUILD-MANIFEST §5 / PROGRESSION-LANES §7`): `arc_state.nether_forge_found bool`
  + `arc_state.seventh_seen_out bool` (both gate nothing).
- **DIRECTOR ACTION:** This migration must be authored and applied BEFORE B4/B5 below. Until it exists,
  `metapuzzle_seed.sql` and `progression_seed.sql` are guarded no-ops (they say so — see B5/B6) and the
  entire back half is unreachable.

### B4. Apply the engine seeds (in dependency order)
- **WHAT THEY ARE (`discord/supabase/seeds/`):**
  1. `puzzles_seed.sql` — *"realizes design/clue-web.md in the 0004 schema. One INSERT per web node... Every
     accepted_answers entry is ALREADY NORMALIZED per ORACLE.md §2."* (the six stones, the Rosettas, the
     back-half rows, the Accepting token's matching row).
  2. `seventh_seed.sql` — the Seventh restore/erase breadth rows; *"GATE NOTHING (gates_progress defaults
     false + CHECK)."*
  3. `side_quests.sql` — *"The TRAVEL longevity layer... 18 rumor→verify destinations... GATE NOTHING. Five
     are deliberate dead leads."*
  4. `thread_tags.sql` — *"Tags each of the 24 seeded puzzle nodes to a reconstruction thread."*
  5. `thread_cards.sql` — *"42 place-anchored CARDS that cluster under the five reconstruction threads."*
- **DIRECTOR ACTION:** Apply in the order above (puzzles → seventh → side_quests → thread_tags →
  thread_cards). All are `ON CONFLICT DO UPDATE`/`DO NOTHING` idempotent, so re-running is safe.

### B5. Apply `metapuzzle_seed.sql` (the activation lane — AFTER 0006)
- **WHAT IT IS:** *"THE ACTIVATION LANE + the UnlockBeat producer-contract fixes... This file is UPDATE-ONLY.
  It carries NO `array[...]` literal."* It sets `requires_flags` on the nine staged back-half rows
  (`bound-word`, `m4-three-hands`, `threshold-coordinate`, `true-walk-arrive`, `seventh-unwriting`,
  `seventh-cause`, `seventh-choice`, `base-docket-reread`, `meta-unkept`) and repoints `no-wall-catch` to set
  `iss_caught` and STOP (no rite shortcut).
- **DIRECTOR ACTION:** Apply AFTER `0006` exists (it writes `puzzles.requires_flags`). The guard no-ops cleanly
  if the column is absent — meaning the back half silently stays dark, so do NOT skip 0006.

### B6. Apply `progression_seed.sql` (the two deepening lanes — staged OFF)
- **WHAT IT IS:** *"THE TWO DEEPENING LANES: the Nether... + the End... Both lanes GATE NOTHING (INV-12)."*
  Rows (`PROGRESSION-LANES §3`): `nether-forge` (`requires_flags:[undercroft_open]`, `active:false`),
  `end-seventh-out` (`requires_flags:[seventh_named]`, `active:false`), `dest-deep-forge`,
  `dest-out-of-record`, `who-deep-forge`, `who-seventh-out`.
- **DIRECTOR ACTION:** Apply last. The two payoff rows stay `active=false` (STAGED) and only flip `active=true`
  at GO-LIVE once the named Multiverse world exists and the site is placed (§E5, §E6). `siteCoverageSelfTest`
  keeps them from seeding OPEN until then.

### B7. `progression_seed` blocker — the FACT-11 seal (`PROGRESSION-LANES §0.1`) — **[GAP — TO BUILD]**
- **WHAT IT IS:** the canon precondition before ANY Nether build: LORE must seal into `canon-spine.md` FACT 11
  + `design/structures.md` the sentence: *"the kept fire was carried up from below the bottom; the Undercroft
  is the bottom of the Hold, the deep-fire its source — one direction, not two."*
- **DIRECTOR ACTION:** Confirm this clause is sealed in canon before flipping the Nether row live. Until then
  the Nether lane is design-only and the journals stay at "below the below."

---

## STAGE C — THE RESOURCE PACK (Path A: the friends' ONLY install)

### C1. Build the pack atlas + font (from the repo)
- **WHAT IT IS (`resourcepack/`, real tree):** `pack.mcmeta` (`pack_format: 34`, description *"The Observance
  — the keepers' alphabet, and the dark that keeps them."*), `assets/observance/font/runes.json` (bitmap
  provider → font `observance:runes`), `assets/observance/textures/font/runes.png` (the rune atlas, generated
  from `discord/src/forge/runes.ts` — the SAME source the Discord cards use, so carvings and cards can't
  disagree), `assets/observance/sounds.json` (4 events: `whisper`, `drone_low`, `stone_breath`, `cold_toll`).
- **DIRECTOR ACTION:** Run `npm run pack:build` after any `runes.ts` change; eyeball with `npm run pack:proof`
  (writes `discord/out/rune-proof.png`).

### C2. The pack audio — **[GAP — GO-LIVE asset]**
- **WHAT IT IS (`resourcepack/README.md`):** `assets/observance/sounds/*.ogg` (the four audio bodies) —
  *"⛳ go-live (binary audio can't be generated here)."* The `sounds.json` events exist; the `.ogg` files do not.
- **DIRECTOR ACTION:** Author/source the four `.ogg` files (`whisper.ogg`, `drone_low.ogg`, `stone_breath.ogg`,
  `cold_toll.ogg`) and drop them in `assets/observance/sounds/` before zipping.

### C3. Host the `.zip` and wire the push (`config.yml resource-pack`)
- **WHAT IT IS (`config.yml` lines 176–188):**
  ```
  resource-pack:
    url: ""        # Public HTTPS URL of the built pack .zip. EMPTY until hosted (go-live).
    sha1: ""       # 40-char lowercase hex SHA-1 of the .zip bytes. EMPTY ⇒ hashless push (a warn).
    required: false # Keep FALSE until the pack is live + tested.
    prompt: ""     # the client pack dialog line (lore-agnostic, keeper register).
    delay-ticks: 20
  ```
  Pusher: `plugin/src/main/java/com/observance/watcher/listener/ResourcePackPusher.java` (`PlayerJoinEvent` →
  `setResourcePack(url, sha1, force, prompt)`; logs `ResourcePackStatusEvent` to the dashboard health panel).
  While `url` is blank the push is **skipped silently** and every non-rune beat runs unchanged.
- **DIRECTOR ACTION:** (1) Zip the pack; (2) host at a public HTTPS URL; (3) compute the 40-char lowercase hex
  SHA-1 of the `.zip` bytes; (4) set `url` + `sha1`; (5) author `prompt` (a plain keeper-register line, no
  caps/exclaim — **[GAP — to author]**, currently `""`); (6) keep `required: false` until tested, then flip to
  `true` (runes are unreadable without the pack); (7) reload.

---

## STAGE D — DATAPACKS / MODS / WORLDS TO INSTALL

### D1. Server baseline
- **WHAT IT IS:** Java 21 / **Paper 1.21.x** (Path-A law). The Observance plugin `.jar` built from
  `plugin/`.
- **DIRECTOR ACTION:** Stand up Paper 1.21.x on Java 21; drop the built plugin jar in `plugins/`; first boot
  writes `config.yml` + `sites.yml` (then edit per Stage F/G).

### D2. The Undercroft fog datapack — **[GAP — TO BUILD]**
- **WHAT IT IS (`structures.md §unbroken_light` + GO-LIVE step 3):** *"the Undercroft is a **datapack**
  dimension/biome (thick fog, `ambient_light: 0`) — NOT the resource pack (Java fog isn't pack-driven). See
  `design/atmosphere-stack.md §3.5`."*
- **DIRECTOR ACTION:** Author a `dimension_type`/biome-effects datapack giving the Undercroft world thick fog
  + `ambient_light: 0`; install it on the Multiverse world that hosts `unbroken_light`.

### D3. Multiverse worlds (the Undercroft + the two deepening lanes) — **[GAP — GO-LIVE]**
- **WHAT IT IS (`PROGRESSION-LANES §0.3 / §7`, `BUILD-MANIFEST §6`):** the Multiverse plugin + three worlds —
  the Undercroft void world, **`observance_nether`**, **`observance_end`** (the Undercroft Multiverse pattern;
  no second bespoke fog dimension — vanilla Nether/End need only re-dressing, R8). `LocationSampler` already
  keys proximity on `worldName`, so a cross-dimension site needs zero new tracking infra.
- **DIRECTOR ACTION:** Install Multiverse; create the Undercroft world (+ D2 fog datapack), `observance_nether`,
  `observance_end`. These must EXIST before the cross-dimension seed rows may flip `active=true` (§B6).

### D4. NPC / apparition mods (P1/P2 — optional, graceful-degrade)
- **WHAT IT IS (`BUILD-MANIFEST §6`):** Citizens2 (the presiding Keeper) + ZNPCsPlus (per-player apparitions)
  for `KeeperNpcBeat`; ModelEngine R4 for `ModeledMobBeat` (P2, degrades to `NamedMobBeat` if absent); Simple
  Voice Chat for the Ear (P3, degrades to a pack-sound whisper). The Keeper listener reads a `keeper_npc` PDC
  tag, *never* the Citizens API — so a PDC-tagged armor-stand fallback is Path-A self-contained.
- **DIRECTOR ACTION:** Install for the full P1+ experience; the slice runs without them. None is required for
  the Overworld spine.

---

## STAGE E — IN-WORLD STRUCTURES TO BUILD + EXPORT AS `.schem`

> **All of this is `[GAP — GO-LIVE]` — it needs a Minecraft client.** No `.schem` file exists in the repo
> (verified: `find . -iname "*.schem"` → none). The paste pipeline is wired (`SmallStructureBeat` →
> `SchematicPaster` → `FaweSchematicPaster`, reflective-isolated: a missing FAWE degrades to "no paste", not a
> crash). **Theme law (`structures.md`):** carved deepslate / tuff / polished basalt / blackstone, oxidized
> copper + soul-lantern light; nothing modern/colorful; every keeper-stone is *one slab you stoop to read*
> (the bow built into the architecture); runes are signs/lecterns/text-displays in `observance:runes`, never
> hand-placed blocks (so the pack, §C, must be live before carving).

For EACH build: build it in-game per the spec, export as a `.schem`, drop the file where the engine expects
it, and fill the real x/y/z into `sites.yml` (§F). Until placed, the plugin **silently skips** the site.

### E0. The Prologue vignette world (`the-hold.zip`) — separate client-side prop — **[GAP — GO-LIVE]**
- **WHAT IT IS (`PROLOGUE-VIGNETTE.md`):** a vanilla Java 21 / 1.21.x single-player world `.zip` + datapack
  `thehold` — a linear ~6-room cold stone hold (P1→P2; the **1-room form ships first**, P0). Gamerules locked
  (`doDaylightCycle false`, `doMobSpawning false`, adventure, etc.). Rooms (verbatim §1): 0 antechamber (one
  lit lantern), 1 record room (lectern: the public half of `the-record-opens`, FACT 1/2 only), 2 doused
  hearth, 3 wall of six marks + a scraped seventh blank, 4 long walk + one lever, 5 hand-off (server address +
  the rune string → `the-record-keeps`). The datapack carries `load.mcfunction` + `tick.mcfunction` (distance
  triggers, no visible clock) + the `passed_seventh` conduct scoreboard.
- **The literal in-vignette beats (Archivist register, verbatim §3):**
  - room 1 lectern book: *"the record opens. it was open before. // the living are written here, each by the
    name they answer to, and against each name a column is left open. // the watching has already watched. no
    one is told. that is the way of it."*
  - room 1 entry tellraw: *"the count begins. the living are written by name."*
  - room 2 (hearth): *"a fire was kept here. it is out. it was not put out by any hand that is still here."*
  - room 3 (wall): *"six are marked. there is a seventh place, and no mark in it. the record will not keep the
    seventh."*
  - room 5 closing book (the pointer): *"the rest is not kept in this hold. it is kept where the others are.
    bring them, and come to the place named below. the record is open there. it was open before you."*
  - the one plain action, beneath the address: *"say one word in the place named below, when you are all in.
    say kept."*
- **DIRECTOR ACTION:** Build the world+datapack in a 1.21.x client (1-room first), fill the `tick.mcfunction`
  region coords against the built geometry, bake the real server address into the room-5 sign, render the rune
  string (bundle the rune pack inside the `.zip` OR build block-letter runes into the room-5 wall), host the
  `.zip` at the URL the lure page's `the-hold.zip` link points to (§G2).

### E1. The two Rosettas — `rune_rosetta`, `stone_of_reckoning`
- **WHAT THEY CARRY (`structures.md`):** `rune_rosetta` = the founding ring, glyph↔letter for the whole
  alphabet, read **sunwise (Bow-first)** — a circular dais, runes on the rim, center empty (teaches the script
  the whole game reads in). `stone_of_reckoning` = digit-glyphs + sign-marks (N/S/E/down); **every coordinate
  clue depends on it** (keep coord-bearing rows inactive until placed).
- **DIRECTOR ACTION:** Build both; export; place; fill coords. Place `stone_of_reckoning` before any coord
  walk is live.

### E2. The M1 teaching-stone — `first_marker_01`
- **WHAT IT CARRIES (`sites.yml` lines 319–331):** *"the one coordinated layout that carries the prologue
  marker glyph, the six UNKEPT maker's-marks (read in fall-order), and the `kept here before you` plant... The
  a1z26 literacy teaching-rung is read here too."* The prologue marker glyph re-reads in M2 via a1z26/atbash →
  `KEPT`/`BEGUN` (ledger #4).
- **DIRECTOR ACTION:** Build the teaching-stone with the lit marker glyph + the six maker's-marks + the
  `kept here before you` line + the a1z26 tick-stave; export; place.

### E3. The first report lectern — `first_report_lectern_01`
- **WHAT IT CARRIES (`structures.md` order 0 / `sites.yml`):** the first notice (Movement I), retargeted to the
  base hot-cell; the `IgnitionListener` lectern read lives here.
- **DIRECTOR ACTION:** Place the report lectern at the base hot-cell beside the teaching-stone marker.

### E4. The six keeper-stones — `stone_vaun stone_mara stone_sella stone_orin stone_brann stone_iss`
- **WHAT THEY CARRY (`structures.md` keeper-stones; `WEB-MASTER §6` fingerprints):** each is a single
  floor-set/low-canted deepslate/blackstone slab ~3×4, angled so the camera must tilt down (the bow), one
  soul-lantern at a fixed offset. Each carving is a sign/hanging-sign/text-display in `observance:runes`
  carrying that keeper's **bound plaintext from `clue-specs.ts` — NEVER edit the plaintext (the X1 guard
  round-trips it).** Per-keeper hand:
  - **Vaun** (caesar) — hammered-square + an empty second column.
  - **Mara** (bookCipher) — small-even.
  - **Sella** (atbash) — mirror-wrong child-scrawl.
  - **Orin** (substitution) — cold-perfect, breaking off at *"i —"*.
  - **Brann** (polybius/rail-fence) — legible **only at night** (place under a gutter-able torch); carries, in
    its **framing** (NOT a bound plaintext — does not touch X1), the Nether plant *"the fire we keep is not
    ours. it is lent… below the below."* (Brann's doubling fingerprint).
  - **Iss** (vigenère, key = his name) — too-smooth, frictionless; carries the false coordinate + (in its
    field) the prophet's wall, a wide set of `dead_end` rungs each decoding to a warm promise, hidden columnar
    name = Iss's own.
  The stone IS a `keeper_stone` answer-site (a sign within radius is the in-world answer verb — no extra
  build). `protect: true` restores a broken carving.
- **DIRECTOR ACTION:** Build all six per hand; export each; place; fill coords. Do NOT retype any bound
  plaintext.

### E5. The custom anchors — `kept_light_home_01`, `offering_cairn_01`, `bow_marker_01`
- **WHAT THEY CARRY (`structures.md`):** `kept_light_home_01` = a built hearth with a soul-fire, scan-zone
  radius 12 (the Kept-Light sampler checks here). `offering_cairn_01` = a small cairn at a shaft-mouth
  (first-ore dropped here = the Offering). `bow_marker_01` = a bow-stone (Orin's deep grooves; crouch within
  radius = honored).
- **DIRECTOR ACTION:** Build, export, place all three.

### E6. The named lore anchors — `the_threshold`, `the_far_water`, `the_cold_hearth`
- **WHAT THEY CARRY (`structures.md`):** `the_threshold` = Orin's low lintel forcing a crouch; Orin's broken
  *"i —"* on the underside (legible only stooped); type `the_threshold` (the presiding Keeper opens at this
  type/`keeper_altar`). `the_far_water` = a still pool in a far gallery, ceiling built to read in reflection as
  an open healed sky (the surface's lie); Sella's bearing leads here. `the_cold_hearth` = a doused hearth + a
  grave slab (Iss) + a second effaced marker (the seventh); cold palette, no light. **`the_cold_hearth` is the
  dead-shrine surface AND the seventh-shrine chamber-3 anchor** (one anchor, temporally layered — the deep
  beneath opens only post `iss_caught` + `seventh_named`).
- **DIRECTOR ACTION:** Build, export, place all three. The grave can reuse `the_threshold` by default; place
  the optional `grave_spur` (currently `enabled: false`) only if the grave needs to sit off the threshold line.

### E7. The back-half spine — `unbroken_light`, `the_unwriting`, `keeper_altar`, `coop_plate`
- **WHAT THEY CARRY (`structures.md §unbroken_light` + `sites.yml` 268–317):**
  - `unbroken_light` = the Undercroft / Accepting floor: the one fire that never went out, centered; a room
    sized for **6–8 players to gather and bow**; the lectern-comparator door `undercroft-descent` opens; the
    `pressure-glyph` rune walked on the floor; type `accepting_floor` (the `AcceptingRiteListener` watches it).
  - `the_unwriting` = the Seventh's unwriting chamber (the hearth-DEEP beneath `the_cold_hearth`); the
    `SeventhChoiceListener` watches type `seventh_shrine`; rail-fence (rails=6, reusing Brann's taught
    literacy) is the chamber-2 cipher; opens only post `iss_caught` + `seventh_named`. (The **End way-out
    pointer** is one extra effaced line revealed on this chamber-2 wall at `seventh_named` — a reveal, no new
    node.)
  - `keeper_altar` = the Undercroft altar the presiding Keeper presides at (the M-IV atonement node); the
    Keeper's body stands here (Citizens2/ZNPCsPlus or a PDC-tagged armor-stand fallback).
  - `coop_plate` = the IV→V hinge plate (foot + carve + Discord, three acts in one ~20s window; active-only).
- **DIRECTOR ACTION:** Build all four; export the three Seventh chambers as `.schem` files
  (`seventh_hearth_01.schem`, `seventh_unwriting_02.schem`, `seventh_deep_03.schem` — **[GAP — deploy
  assets]**); place; fill coords.

### E8. The carve + herd anchors — `carve_anchor_01..03`, `herd_anchor`
- **WHAT THEY CARRY (`sites.yml` 333–378):** `carve_anchor_01..03` = name-where-never-been carve cells (the
  `SignWriteBeat` carves an ACTIVE player's name at a cell they have provably never visited; several candidates
  so the selector has a group-avoided ∩ never-visited intersection; a chorus, never the divergence extremes).
  `herd_anchor` = the wide pasture (radius 16) where the cosmetic Pale conversion spreads (between-session,
  capped, unwitnessed, never glowing/tracked/breeding).
- **DIRECTOR ACTION:** Place 3 carve candidates at low-risk avoided cells; place the herd anchor over a wide
  pasture.

### E9. The Nether lane sites — `nether_forge`, `soul_gallery`, `bastion_remains` (world `observance_nether`)
- **WHAT THEY CARRY (`PROGRESSION-LANES §1.1` + `sites.yml` 411–463):** `nether_forge` = the near pocket — a
  small ruined room just past a lit portal (a delve, walk-budget = 2 ground walks + ≤1 short vertical pocket),
  a prior keeper's remains on a deepslate slab + a doused soul-lantern + the journal `the-fire-kept-me`; the
  on-site **word** (INV-14: the word answers, never the coordinate) is read off the slab and answered there,
  setting `nether_forge_found` + bonus Whisper + the Kept-Light origin; **remains placed at world-build, never
  pasted toward a player**; type `answer_sign`. `soul_gallery` = a wide soul-sand field (the not-kept of deep
  time; texture only). `bastion_remains` = a vanilla bastion re-read as the founders' deepest ruined delvings
  (texture only; the P2 basalt-glimpse may fire near here, deferring to `apparitionClaim`).
- **DIRECTOR ACTION:** Build/place in `observance_nether` only AFTER the FACT-11 seal (§B7) and after the world
  exists (§D3). Then flip `nether-forge` `active=true` (§B6).

### E10. The End lane sites — `end_seventh_shrine`, `end_exile_hold` (world `observance_end`)
- **WHAT THEY CARRY (`PROGRESSION-LANES §1.2` + `sites.yml` 465–502):** `end_seventh_shrine` = the Seventh's
  home outside the record (no kept fire, no markers, no count) — a re-dressed end-ship (force-load → mutate on
  unwitnessed relog → unload) OR a world-build pre-generated outer island, built to the Seventh's unfinished,
  wrong-scaled hand, carrying `the-name-i-cut-myself`; reaching it + answering the on-site read sets
  `seventh_seen_out` (group-scoped, NOT a fate input); type `answer_sign`; **ZERO ambient apparition lane** (a
  positive canon choice). `end_exile_hold` = the `cast_out` fate made a place (a re-dressed end-city, markers
  facing away, vast/static) — **`enabled: false`**, P2/cuttable, ships ONLY if the INV-16 binding is built
  (chorus-only dressing, no per-player side, no spatial correspondence to any carve), else the End ships as the
  shrine alone.
- **DIRECTOR ACTION:** Build/place `end_seventh_shrine` in `observance_end` after the world exists; re-dress an
  *already-generated* end-ship or a world-build island (never a lazy paste toward a glider). Flip
  `end-seventh-out` `active=true`. Leave `end_exile_hold` disabled unless the INV-16 binding is fully built
  (§B/PROGRESSION-LANES §0.4 / S10).

---

## STAGE F — `sites.yml` COORDS TO FILL

- **WHAT IT IS (`plugin/src/main/resources/sites.yml`):** every site ships with `x/y/z: null` (the UNPLACED
  sentinel) — the plugin treats null coords OR `enabled: false` as "not yet sited" and **silently skips** it,
  never erroring at a player. `default-world: "world"`; defaults `radius: 6`, `protect: true`,
  `vertical-radius: 4`.
- **DIRECTOR ACTION:** After each build/export (Stage E), fill the real x/y/z. Sites that must be filled (all
  currently null):
  - **Overworld, enabled:** `bow_marker_01`, `offering_cairn_01`, `kept_light_home_01`,
    `first_report_lectern_01`, `answer_sign_01`, `unbroken_light`, `stone_vaun`, `stone_mara`, `stone_sella`,
    `stone_orin`, `stone_brann`, `stone_iss`, `rune_rosetta`, `stone_of_reckoning`, `the_far_water`,
    `the_threshold`, `the_cold_hearth`, `the_unwriting`, `keeper_altar`, `coop_plate`, `first_marker_01`,
    `carve_anchor_01..03`, `herd_anchor`.
  - **Reserved/disabled (leave OFF unless used):** `keeper_stone_01/02` (`enabled: false`), `grave_spur`
    (`enabled: false`).
  - **`observance_nether` (after the world + FACT-11 seal):** `nether_forge`, `soul_gallery`,
    `bastion_remains`.
  - **`observance_end` (after the world):** `end_seventh_shrine`; `end_exile_hold` stays disabled unless its
    binding is built.
- **GO-LIVE rule (`sites.yml` 394–409):** a cross-dimension row must NOT seed OPEN unless its site is placed +
  enabled IN the named world — i.e. the Multiverse world must exist first.

---

## STAGE G — CONFIG TO SET (`config.yml`)

### G1. The forbidden word (the Unspoken) — **[GAP — TO AUTHOR]**
- **WHAT IT IS (`config.yml` lines 104–107):** `tracker.forbidden-words: []` — *"Authored per-arc; left EMPTY
  here so the committed config ships no story text."* Canon: `the_unspoken` = *"never speak the Dark's name"*
  (`WORLD-BIBLE §the_unspoken`). The word itself is **deliberately not in the repo** (it is the trigger, never
  a displayed string — canon's "never write the Unspoken").
- **DIRECTOR ACTION:** Set `forbidden-words` to the arc's chosen Unspoken word(s) on the locked-down server
  config (case-insensitive, word-boundary matched). This is the only place the word lives.

### G2. The Accepting quorum (`rites.accepting`)
- **WHAT IT IS (`config.yml` lines 160–168):** `token: "k7q2m9 x4r8p3 w1n6z5 t0j4h2 b8f1v7 c3d6s9"`,
  `puzzle-key: "accepting-crouch"`, `quorum: 6`, `cooldown-seconds: 300`. The token MUST byte-match the
  `accepting-crouch` row's `accepted_answers` in `puzzles_seed.sql` (the `riteTokenSelfTest` enforces it).
- **DIRECTOR ACTION:** Set `quorum` to the **cast size** (or cast−1 for safety) — default 6 so two stragglers
  can't fire the finale. Do not change the token unless the seed row changes in lockstep.

### G3. The false-law toggle (`customs.false-law`)
- **WHAT IT IS (`config.yml` lines 200–201):** `false-law.enabled: true` — *"A LIE the land never enforced...
  it NEVER enforces anything in-world (INV-17). Default on = the lie is in play."*
- **DIRECTOR ACTION:** Leave `true` (the forged eighth is part of the arc). It gates only the fiction's
  surfacing, never enforcement.

### G4. The Unlit Deep group latch (`customs.unlit-deep` / `restraint`)
- **WHAT IT IS (`config.yml` lines 209–224):** `unlit-deep.enabled: true`, `flame-materials: []` (empty ⇒
  vanilla fire/torch/lantern/campfire default), `deep-line-y: -48`, `taboo-moon-phases: []` (empty ⇒ reuse
  dark-hours), `cooldown-seconds: 300`; master kill `restraint.enabled: true`.
- **DIRECTOR ACTION:** Leave enabled (the one group custom INV-17 permits). Tune `flame-materials`/moon phases
  only if the defaults need narrowing.

### G5. Difficulty bounds (`difficulty`)
- **WHAT IT IS (`config.yml` lines 248–259):** `enabled: true`, `cadence-mult-min: 0.75`,
  `cadence-mult-max: 2.0`, `state-hold-ms: 600000` (10 min hysteresis), `stage-dead-ends: true`. NEVER touches
  the Whisper backstop (INV-15) — a `whisper_budgets` reference in the difficulty path is a bug.
- **DIRECTOR ACTION:** Keep defaults; the engine clamps within `[min,max]` from measured group skill with
  hysteresis. `state-hold-ms` is also the window the dashboard health panel reads.

### G6. The real-clock event window (`event-window`)
- **WHAT IT IS (`config.yml` lines 270–275):** `enabled: false` (⇒ always-open), `start: ""`, `end: ""` (UTC
  ISO-8601). Bounds when curatorial surfaces (Record re-render, grave `not_before`, summons) may ACT — so a
  filmed take lands in a known window. The single Accepting instant is NOT set here (the showrunner binds it in
  `arc_state`). `/observance window <on|off>` force-overrides for a live take.
- **DIRECTOR ACTION:** Leave `enabled: false` until a shoot is scheduled; then set `start`/`end` to the take
  window (UTC) and flip `enabled: true`, OR use the slash override at film time.

### G7. The other tracker knobs (sanity-confirm)
- **WHAT THEY ARE (`config.yml`):** `tracker.deep-line.y-threshold: -48`; `dark-hours.taboo-moon-phases: [0]`
  (full = "black" moon); `sacred-beast-pdc-key: "observance_sacred_beast"`; herd
  `pale-cosmetic-pdc-key: "observance_pale_cosmetic"` (DISTINCT from the sacred-beast key — killing a Pale is
  NOT a violation), `max-pale: 16`; `ore-materials/hoard-weights: []` (defaults).
- **DIRECTOR ACTION:** Confirm; leave defaults unless retuning.

---

## STAGE H — THE DASHBOARD (deploy + ADMIN_EMAILS)

### H1. Deploy
- **WHAT IT IS (`dashboard/`):** the Next.js director's console + the public Record route
  `dashboard/src/app/record/[slug]/page.tsx` (server component, `noindex`, no client JS, reads `v_record` via
  the anon client) + `record-projection.ts` (spoiler-free projection) + the author panels (behind the gate).
- **DIRECTOR ACTION:** Deploy (Vercel). Set `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`,
  `SUPABASE_SERVICE_ROLE_KEY` (rotated), `ADMIN_EMAILS` as project env vars (NOT committed `.env.local`).

### H2. ADMIN_EMAILS (Author-mode gate)
- **WHAT IT IS (`dashboard/src/lib/auth.ts`):** `parseAdminEmails()` reads the comma-separated `ADMIN_EMAILS`
  env into a lowercased set; `isAdmin(user)` gates Author mode. Current committed value (to be replaced):
  `ADMIN_EMAILS=bradnbelew@gmail.com`.
- **DIRECTOR ACTION:** Set `ADMIN_EMAILS` to the director's email(s), comma-separated, lowercase
  (e.g. `bradnbelew@gmail.com` and/or `ethan.stopperich1@gmail.com` if Ethan needs Author access — but note
  Ethan is the spoiler-free playtester; default to the director only).

---

## STAGE I — THE CURSED-MAP WEBSITE (the lure page) TO DEPLOY

- **WHAT IT IS (`CURSED-MAP-SITE.md`):** NOT a second site — it IS the Record at one slug, served by the SAME
  `record/[slug]/page.tsx` already deployed in §H. The route serves exactly two slugs (`the-record` = base
  archive; `the-record-keeps` = base archive **+ the downloads block**) and an in-voice 404 shell for anything
  else. The downloads block (rendered only for `the-record-keeps`) is **static authored content**, reads no new
  data:
  - **the map description (verbatim §2a):** *"a hold, kept and left. one walk through it remains. the rest of
    the record is kept elsewhere. what is downloaded is only the part that fit in a file."*
  - **the download link:** label `the-hold.zip`, `href` → the vignette asset URL, `download`,
    `rel="noopener"`, no button styling.
  - **the provenance (Mara's hand, signed):** *"i copied it as it was given, page for page, and set the copy
    where fire and water do not reach. i did not keep the seventh. i was not the hand that decides what is
    kept. — m.kept"*
  - **the README "lie" (verbatim §2b):** *"the-hold.zip — a small offline map. single player. no mods. about
    fifteen minutes. it does not connect to anything. play it through to the end and it will tell you where the
    rest is kept."*
  - **the counter (verbatim §2c):** static `kept: 6` (mono/tabular type) + a struck seventh row using the
    BUILT `REDACTED_GLYPH`. **NOT a live counter** (a live count would drift off 6 the moment they download).
- **DIRECTOR ACTION:** The route code is shipped (`§4` of `CURSED-MAP-SITE.md` says the slug branch + downloads
  block + 404 are already in `record/[slug]/page.tsx`). The ONLY go-live residue: set the `the-hold.zip`
  `href` to the real hosted asset URL (§E0), and confirm `/record/the-record-keeps` → 200 with the downloads
  block, junk slug → 200 in-voice 404, bare `/record` → 404. No new route, table, flag, or seed.

---

## STAGE J — THE DISCORD SERVER CONFIG

### J1. Create the app + bot
- **WHAT IT IS (`discord/README.md`):** the bot is **The Watcher** — every player-facing line comes from
  `src/voice.ts`; it posts ONLY to `#the-record`; *"nothing says bot, AI, game, server, or command to a
  player, and it never uses normal capitalization in chat."*
- **DIRECTOR ACTION:** In the Discord Developer Portal create the application + bot; copy the bot token
  (`DISCORD_BOT_TOKEN`), application id (`DISCORD_APP_ID`); enable the Message Content intent (the ignition
  detector reads `#the-record` posts).

### J2. The guild + the one channel
- **WHAT IT IS (`.env.example`):** `DISCORD_GUILD_ID` (slash commands register per-guild for instant
  availability); `CHANNEL_THE_RECORD` — *"#the-record — the Watcher's channel, and the ONLY one the bot posts
  to (reports + clue drops). /whisper is run anywhere; cipher-solving is in #general."*
- **DIRECTOR ACTION:** Turn on Developer Mode; create `#the-record` (the Watcher's voice) and `#general`
  (cipher-solving); copy the guild id and the `#the-record` channel id into `.env`.

### J3. Register the slash commands
- **WHAT IT IS (`discord/src/bot/register.ts`):** `registerGuildCommands()` REST-deploys three commands —
  `/whisper <puzzle>` (ask the Watcher for a tiered hint, pay the toll; the tier rises on its own with each
  ask), `/link <name>` (bind a Discord voice to a name worn in the world), and `/answer` (the answer command).
- **DIRECTOR ACTION:** Run the `register` entry once to deploy the guild commands; invite the bot to the guild;
  run the `dev` entry to bring the Watcher client online (presence + interaction routing). The ignition
  detector (`prologue.ts` / `decidePrologue` → `recordOpened`) flips `prologue_ignited` on a lectern read OR a
  human post in `#the-record`.

---

## STAGE K — FINAL ARM (the order to go live)

1. Rotate keys (A), env set everywhere (A2–A4).
2. SQL: dashboard 0001–0003; engine 0003–0005; **author + apply 0006 [GAP]**; engine seeds; metapuzzle;
   progression (staged OFF) (B). Confirm the FACT-11 seal before the Nether row [GAP].
3. Build + host the resource pack with SHA-1; set `config.yml resource-pack` (C). Author the pack `prompt`
   [GAP] and the four `.ogg` files [GAP].
4. Stand up Paper 1.21.x + plugin; install the Undercroft fog datapack [GAP] + Multiverse worlds [GAP].
5. Build every site in-game, export `.schem` [GAP], place, fill `sites.yml` coords (E, F). Flip the two
   progression rows `active=true` once their worlds + sites exist.
6. Set config: the Unspoken word [GAP], quorum = cast size, false-law on, difficulty bounds, event-window
   off-until-shoot (G).
7. Deploy the dashboard + set `ADMIN_EMAILS` (H); the lure page is the same route — set the `the-hold.zip`
   href to the hosted vignette [GAP] (I, E0).
8. Create the Discord app, channel, register commands, bring the Watcher online (J).
9. Flip `resource-pack.required: true` after a clean pack test; arm the event-window at film time.

---

### GAP INDEX (what is NOT yet in the repo — must be built before day one)
1. `discord/supabase/migrations/0006_*.sql` (requires_flags, world_paste_ledger, ending/grave/restraint/
   reckoning columns, the two progression flags). **No 0006 exists.**
2. The FACT-11 source-clause seal in `canon-spine.md` + `structures.md` (Nether precondition).
3. The four resource-pack `.ogg` audio bodies (`assets/observance/sounds/`).
4. The resource-pack `prompt` line (`config.yml resource-pack.prompt` is `""`).
5. The Undercroft fog datapack (dimension_type effects).
6. The three Multiverse worlds (Undercroft / `observance_nether` / `observance_end`).
7. The Prologue vignette `the-hold.zip` (world + datapack) + its hosted URL + the baked server address.
8. Every in-world build + its `.schem` export (the two Rosettas, the teaching-stone, the six keeper-stones,
   the custom anchors, the named anchors, the back-half spine, carve/herd anchors, the Nether/End sites) —
   no `.schem` file exists in the repo.
9. The three Seventh chamber `.schem` deploy assets (`seventh_hearth_01/02/03.schem`).
10. ALL `sites.yml` coords (every site is `null`/UNPLACED).
11. The forbidden Unspoken word value (`tracker.forbidden-words` is `[]`).
12. The hosted resource-pack `.zip` URL + its SHA-1 (`config.yml resource-pack.url/sha1` are `""`).
