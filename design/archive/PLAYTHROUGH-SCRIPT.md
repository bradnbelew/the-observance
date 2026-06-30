# THE OBSERVANCE — MASTER PLAYTHROUGH SHOOTING-SCRIPT

> The single, ordered, literal "what is there" for the whole arc — cold repo to the five divergent
> closes. Assembled from the seven segment scripts in `design/playthrough/` (`0-setup` …
> `6-finale`). Every line is sourced from a REAL repo artifact (path given) or marked
> **[GAP — TO BUILD]** (code/text owed) / **[GAP — GO-LIVE]** (in-game build/asset owed) and never
> invented as built. Where a built artifact and a design doc disagree, the design doc
> (`WEB-MASTER` / `INTEGRATION-V2`) wins and the seam is flagged. This is the continuity script the
> editor reviews; it does not invent.

---

## HOW TO READ THIS

1. **What this is.** A shooting-script, not a description. Each beat is the literal in-world / in-Discord
   / on-page content — verbatim quotes where the text is load-bearing, with the source file named. The
   editor uses it to confirm cross-surface continuity (Minecraft ↔ Discord ↔ dashboard ↔ website ↔ lore
   never contradict) and to cut a Wifies-grade ARG video.

2. **Order.** Strict causal order: **Director Setup** (one-time, before day one) → **Prologue / M0-remote**
   → **Movement I (Establishment)** → **Movement II (The Ways) + Nether lane** → **Movement III (Undercroft
   + the Liar)** → **Movement IV (Atonement, the Keeper) + End lane** → **Movement V (The Accepting) + the
   five closes**. Within a movement the field is a **non-linear web** — many doors open at once; the
   resolver ignores movement order. "Causal order" below means *the order a beat can first be touched*, not
   a step-ladder.

3. **The two registers.** The **Watcher / Archivist / Keeper** voice (Set-B/C) is plain, cold, mechanical,
   declarative — it counts and records, never emotes or threatens. The **surface NPCs** (Set-A: Aro, Wenna,
   Pell, Dob, Coll) are the human counterweight — contractions, capitals, named feelings are *legal* there.
   One shared rune alphabet (`runes.ts`) across every surface.

4. **Anchors & cross-references.** Plants and payoffs are linked by an anchor id of the form
   `{#plant-NN}` (the plant) ↔ `{#payoff-NN}` (where it pays off), keyed to the `WEB-MASTER §9` ledger
   number. "Re-reads at M4 (→ §IV)" means follow the link to the payoff beat. **No payoff without a plant;
   no plant without a payoff** — a dangling one is a defect.

5. **The laws this script is audited against** (a violation is a defect): CONSISTENCY / NO ORPHANS (every
   feature moves story + clue + lore + NPC + interaction in lockstep); ANTI-JANK (deterministic engine is
   the spine, the LLM a rare text-only scalpel with a deterministic fallback behind every call; nothing
   witnessed appearing/mutating; reversible tolls; idempotent; nothing breaks if the model is slow/offline);
   PATH A (friends install nothing but ONE auto-pushed resource pack to play the server campaign; the
   downloadable prologue map is a separate optional prop, never the delivery vehicle); PRECISION over
   recall (a wrong "it knows you" is worse than none); COLLECTIVE ending, never a chosen one, never punish
   for an absent member (active players only).

6. **GAP discipline.** `[GAP — TO BUILD]` = a code/text artifact owed. `[GAP — GO-LIVE]` = an in-game
   build / hosted asset / coord-fill owed. Every GAP is collected once in the **GAP REGISTER** at the foot
   of this file with its owner. A site with `null` coords or `enabled:false` is **silently skipped** by the
   plugin — never an error at a player.

7. **Surfaces this script crosses.** The lure **WEBSITE** (`/record/[slug]`), the downloadable **VIGNETTE**
   (`the-hold.zip`, optional prop), the **SERVER** (Paper plugin + Discord `#the-record` Watcher + the
   in-world keeper-stones/altars), the **DASHBOARD** (director's console + the public Record route), and the
   **LORE** corpus. The same truth shows on every surface, one register apart.

---

## DIRECTOR SETUP CHECKLIST (one-time, cold repo → ready for the friends)

> Consolidated and de-duplicated from `playthrough/0-setup.md` (Stages A–K) plus the per-segment go-live
> lists. Ordered; **the order is load-bearing** (seeds reference columns the migrations create; sites must
> exist before cross-dimension rows may seed open). Do this ONCE, in order. Checkbox task list.

### Stage A — Rotate secrets FIRST (a live key is committed)
- [ ] **A1.** ROTATE the Supabase `service_role` JWT (and anon key) for project `fdnmhbpxnodrnbrzrlqq` —
      it is committed in the clear in `dashboard/.env.local`. Delete the new key from any committed file;
      put it only in untracked env. Confirm `dashboard/.env.local` + `discord/.env` are gitignored before
      any commit. **[GAP — operator action; live key exposure.]**
- [ ] **A2.** On the server box set env `OBSERVANCE_SUPABASE_KEY=<rotated key>`; leave `config.yml`
      `service-key: ""` committed-empty (`config.yml` L13–19 resolution order).
- [ ] **A3.** Create `discord/.env` from `.env.example`; fill `DISCORD_BOT_TOKEN`, `DISCORD_APP_ID`,
      `DISCORD_GUILD_ID`, `CHANNEL_THE_RECORD`, `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` (rotated).
- [ ] **A4.** Set dashboard env (Vercel, not committed `.env.local`): `NEXT_PUBLIC_SUPABASE_URL`,
      `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY` (rotated), `ADMIN_EMAILS` (§H2).

### Stage B — SQL apply order (migrations → 0006 → seeds)
- [ ] **B1.** Apply dashboard migrations 0001 → 0002 → 0003 (record/auth schema + `v_record` read path).
- [ ] **B2.** Apply engine migrations 0003 → 0004 → 0005 (discord schema, oracle web, reconstruction layer).
- [ ] **B3.** **Author + apply `discord/supabase/migrations/0006_*.sql`** — **[GAP — TO BUILD]** (no 0006
      exists). Must add: `arc_state.ending_fate/ending_codicil`; `coop_gate_legs`; `group_restraint_state`;
      `player_visited_cells`; `keeper_record jsonb` on `showrunner_state`; `grave_state`; the Accepting-instant
      column; `reckoning_*`; **`puzzles.requires_flags jsonb`** (the activation lane); **`world_paste_ledger`**;
      **`voice_watchlist`**; plus `arc_state.nether_forge_found bool` + `arc_state.seventh_seen_out bool`.
- [ ] **B4.** Apply engine seeds in dependency order: `puzzles_seed.sql` → `seventh_seed.sql` →
      `side_quests.sql` → `thread_tags.sql` → `thread_cards.sql` (all idempotent).
- [ ] **B5.** Apply `metapuzzle_seed.sql` (the activation lane; UPDATE-only) **after 0006 exists** — it
      writes `puzzles.requires_flags` on the 9 staged back-half rows and repoints `no-wall-catch`.
- [ ] **B6.** Apply `progression_seed.sql` last (Nether + End lanes). The two payoff rows stay
      `active=false` (STAGED) until GO-LIVE (worlds built + sites placed).
- [ ] **B7.** **Seal the FACT-11 source-clause** into `canon-spine.md` FACT 11 + `structures.md`: *"the kept
      fire was carried up from below the bottom; the Undercroft is the bottom of the Hold, the deep-fire its
      source — one direction, not two."* — **[GAP — TO BUILD]**; precondition before ANY Nether build/flip.

### Stage C — The resource pack (Path A: the friends' ONLY install)
- [ ] **C1.** `npm run pack:build` (atlas/font from `discord/src/forge/runes.ts` — same source as the
      Discord cards); proof with `npm run pack:proof`.
- [ ] **C2.** Author/source the four `.ogg` audio bodies (`whisper`, `drone_low`, `stone_breath`,
      `cold_toll`) into `resourcepack/assets/observance/sounds/`. **[GAP — GO-LIVE asset.]**
- [ ] **C3.** Zip; host at a public HTTPS URL; compute the 40-char lowercase SHA-1; set `config.yml`
      `resource-pack.url` + `sha1`; **author the `prompt` line** (plain keeper register, currently `""`)
      **[GAP — TO AUTHOR]**; keep `required:false` until tested, then flip `true`.

### Stage D — Datapacks / mods / worlds
- [ ] **D1.** Stand up Paper 1.21.x / Java 21; drop the built plugin jar; first boot writes
      `config.yml` + `sites.yml`.
- [ ] **D2.** Author the Undercroft fog datapack (`dimension_type`/biome effects: thick fog,
      `ambient_light: 0`). **[GAP — TO BUILD.]**
- [ ] **D3.** Install Multiverse; create three worlds — the Undercroft void world (+ D2 fog),
      `observance_nether`, `observance_end`. **[GAP — GO-LIVE]** (must exist before cross-dimension rows flip).
- [ ] **D4.** (Optional, graceful-degrade) Citizens2 + ZNPCsPlus (Keeper / apparitions), ModelEngine R4,
      Simple Voice Chat. The Overworld spine runs without them (PDC-tagged armor-stand fallback for the Keeper).

### Stage E — Build every site in-game, export `.schem`, place (ALL `[GAP — GO-LIVE]`)
> Theme law (`structures.md`): carved deepslate/tuff/polished basalt/blackstone, oxidized copper +
> soul-lantern light; nothing modern/colorful; every keeper-stone is one slab you stoop to read (the bow
> built into the architecture); runes are signs/lecterns/text-displays in `observance:runes`, never
> hand-placed blocks (so the pack must be live before carving). No `.schem` exists in the repo yet.
- [ ] **E0.** Build the Prologue vignette world + `thehold` datapack (`the-hold.zip`); fill the
      `tick.mcfunction` region coords; bake the real server address into room 5; render the rune string;
      host the `.zip` at the lure page's link target.
- [ ] **E1.** Build the two Rosettas: `rune_rosetta` (founding ring, sunwise Bow-first) and
      `stone_of_reckoning` (digit Rosetta — every coord clue depends on it; place before any coord walk).
- [ ] **E2.** Build `first_marker_01` (the M1 teaching-stone: prologue marker glyph + six UNKEPT maker's-marks
      read in fall-order + `kept here before you` + the a1z26 tick-stave).
- [ ] **E3.** Place `first_report_lectern_01` at the base hot-cell beside the teaching-stone marker.
- [ ] **E4.** Build the six keeper-stones (`stone_vaun/mara/sella/orin/brann/iss`), each per its hand;
      **do NOT retype any bound plaintext** (the X1 guard round-trips it).
- [ ] **E5.** Build the custom anchors: `kept_light_home_01` (hearth + soul-fire + Mara's six-book shelf),
      `offering_cairn_01`, `bow_marker_01`.
- [ ] **E6.** Build the named anchors: `the_threshold` (Orin's low lintel), `the_far_water` (Sella's
      reflection pool), `the_cold_hearth` (doused hearth + grave + effaced seventh; the dead-shrine surface
      AND the seventh-deep anchor, temporally layered).
- [ ] **E7.** Build the back-half spine: `unbroken_light` (the Accepting floor, lectern-comparator door, one
      fire, 6–8-player bow room), `the_unwriting` (Seventh chambers; export `seventh_hearth_01/02/03.schem`),
      `keeper_altar`, `coop_plate`.
- [ ] **E8.** Place `carve_anchor_01..03` (name-where-never-been cells, low-risk avoided) + `herd_anchor`
      (wide pasture for the cosmetic Pale spread). Also build Room-A / Room-B `.schem` set-pieces (the
      Undercroft A→B swap).
- [ ] **E9.** (Nether lane, AFTER B7 seal + D3 world) Build in `observance_nether`: `nether_forge` (near
      pocket — remains on slab + doused soul-lantern + the on-site word + journal), `soul_gallery`,
      `bastion_remains`. Then flip `nether-forge` `active=true`.
- [ ] **E10.** (End lane, AFTER D3 world) Build `end_seventh_shrine` in `observance_end` (re-dressed end-ship
      or world-build island, never a paste toward a glider). Then flip `end-seventh-out` `active=true`. Leave
      `end_exile_hold` `enabled:false` unless the INV-16 binding is fully built.

### Stage F — Fill `sites.yml` coords
- [ ] **F1.** After each build/export, fill the real `x/y/z` (every site ships `null` = UNPLACED, silently
      skipped). Overworld enabled set: `bow_marker_01`, `offering_cairn_01`, `kept_light_home_01`,
      `first_report_lectern_01`, `answer_sign_01`, `unbroken_light`, the six `stone_*`, `rune_rosetta`,
      `stone_of_reckoning`, `the_far_water`, `the_threshold`, `the_cold_hearth`, `the_unwriting`,
      `keeper_altar`, `coop_plate`, `first_marker_01`, `carve_anchor_01..03`, `herd_anchor`. Leave
      `keeper_stone_01/02` + `grave_spur` disabled unless used. Cross-dimension rows must NOT seed open until
      the site is placed + enabled in the named world.

### Stage G — Config (`config.yml`)
- [ ] **G1.** Set `tracker.forbidden-words` to the chosen Unspoken word(s) on the locked-down server config
      (case-insensitive, word-boundary). The word lives ONLY here. **[GAP — TO AUTHOR.]**
- [ ] **G2.** Set `rites.accepting.quorum` to the cast size (default 6 so two stragglers can't fire the
      finale). Do not change the `token` unless the `accepting-crouch` seed row changes in lockstep
      (`riteTokenSelfTest` enforces byte-match).
- [ ] **G3.** Leave `customs.false-law.enabled: true` (the forged eighth; never enforces in-world).
- [ ] **G4.** Leave `customs.unlit-deep.enabled: true` + `restraint.enabled: true` (the one group custom).
- [ ] **G5.** Keep difficulty bounds default (`cadence-mult-min/max 0.75/2.0`, `state-hold-ms 600000`,
      `stage-dead-ends true`); it NEVER touches the Whisper backstop (INV-15).
- [ ] **G6.** Leave `event-window.enabled: false` (always-open) until a shoot; then set UTC `start`/`end`
      and flip on, OR use `/observance window on` at film time.
- [ ] **G7.** Sanity-confirm the other knobs (`deep-line.y -48`; `taboo-moon-phases [0]`; sacred-beast vs
      `pale_cosmetic` PDC keys are DISTINCT; `max-pale 16`).

### Stage H — Dashboard
- [ ] **H1.** Deploy the Next.js console + the public Record route (`record/[slug]/page.tsx`, `noindex`,
      anon-client `v_record`). Set env per A4.
- [ ] **H2.** Set `ADMIN_EMAILS` (comma-separated, lowercase) to the director's email(s) — default the
      director only (Ethan is the spoiler-free playtester).

### Stage I — The cursed-map lure page (same route, not a new site)
- [ ] **I1.** Confirm `/record/the-record-keeps` → 200 with the downloads block; junk slug → in-voice 404;
      bare `/record` → 404. Set the `the-hold.zip` `href` to the hosted vignette (E0). No new route/table/flag.

### Stage J — Discord
- [ ] **J1.** Create the app + bot (The Watcher); enable Message Content intent; copy token + app id.
- [ ] **J2.** Create `#the-record` (the only channel the bot posts to) + `#general` (cipher-solving); copy
      guild id + channel id into `.env`.
- [ ] **J3.** Run `register` once (deploy `/whisper`, `/link`, `/answer` guild commands); invite the bot;
      run `dev` to bring the Watcher online.

### Stage K — Final arm (go-live order)
- [ ] **K1.** Rotate keys + set env everywhere (A).
- [ ] **K2.** SQL: dashboard 0001–0003; engine 0003–0005; author+apply 0006; engine seeds; metapuzzle;
      progression (staged OFF) (B). Confirm the FACT-11 seal before any Nether flip.
- [ ] **K3.** Build + host the pack with SHA-1; set `config.yml resource-pack`; author the prompt + the four
      `.ogg` (C).
- [ ] **K4.** Stand up Paper + plugin; install the fog datapack + Multiverse worlds (D).
- [ ] **K5.** Build every site, export `.schem`, place, fill coords (E, F). Flip the two progression rows
      `active=true` once their worlds + sites exist.
- [ ] **K6.** Set config (Unspoken word, quorum, false-law, difficulty, event-window) (G).
- [ ] **K7.** Deploy dashboard + `ADMIN_EMAILS`; set the `the-hold.zip` href (H, I).
- [ ] **K8.** Create the Discord app/channel; register commands; bring the Watcher online (J).
- [ ] **K9.** Flip `resource-pack.required: true` after a clean pack test; arm the event-window at film time.

### Code owed before each movement ships (the cross-cutting [GAP] list — see GAP REGISTER)
- [ ] **M1:** `voice.recordFrameBreak()`; the `prologue_ignited`-SET listener; resolve the
      `recordOpenedNamed`/`prologueNamed` key-mismatch; the showrunner `run.ts` drip/report loop; the static→live
      Hold-Book (`keeper_record.ts`); `reckoning.ts` (mute in M1); `SacredAnimalBeat` spread + `pale_cosmetic`;
      the spawn-bias conductor + `apparitionClaim`; reconcile the rune-ring doc↔seed (ward/covering vs
      unspoken/sacred-beast).
- [ ] **M2:** the Whisper economy data-layer (`whisper_budgets` ledger + toll + tier) + per-puzzle `hintBody`
      strings; the four `nether.*`/`end.*` voice keys; `cardNetherForge` archive body; the live
      lockstep-unredaction Record page + the Iss-card stego payload; the `npcVoice.ts` registry; the staged
      `stone-brann-cipher` railFence activation (TS-FORGE `CLUE_SPECS` + `NON_CIPHER_KEYS`).
- [ ] **M3:** `RoomSwapBeat` + Room-A/B `.schem`; the Multiverse Undercroft world + fog datapack; the
      `resolve.ts` `private_message` key-resolver + `requires_flags` column/filter; `liar.run.ts`;
      `UnlitDeepListener.java` + `group_restraint_state`; re-author `stone-brann` as the night-beacon rail-fence;
      the SeventhChoiceListener rite + two-token sentinel branch; the `the-seventh-spine §1.3` doc fix.
- [ ] **M4:** repoint/remove `no-wall-catch` `next_puzzle_key: rite-tokens` (the F1 monorail); author
      `keeper.iss.cold` / `keeper.rhyme.*` / `keeper.presiding.neutral` bodies; de-slop `keeper.atone.cleared`
      + `keeper.fact9.named`; set `eighth_seen` on reading the forged board; the End-pointer reveal line +
      `cardEndSeventhOut`; `D-new the-fire-they-let-out.md`.
- [ ] **M5:** the M5 COMPOSER (`composer.ts`/`m5*.ts` — the ≤2-clause + codicil assembler); the arc_state
      Accepting-instant binding writer + `grave.run.ts` + `fate.run.ts`; the resolve.ts fate-sentinel + Seventh-
      choice sentinel branches; the `RefusalRiteListener` (→ REFUSERS); the `readyGate`/`activeRosterSize`
      wiring; the fate floor-dressing producer; the `observance:the_record_receives_you` advancement JSON.

---

# THE PLAYTHROUGH — PROLOGUE → FINALE

---

# SEGMENT I — THE PROLOGUE / M0-REMOTE (the cursed map + ignition)

> Source: `playthrough/1-prologue.md`, `playthrough/2-movement-1.md §Day 0`. Owner `cursed-map-frame`
> (A14). Mints nothing; caused by a player, not the bot (TINAG preserved). **Frame law (Path A):** the
> vignette is an OPTIONAL client-side prop — a friend who refuses the download joins cold and gets the
> in-server prologue unchanged. The map gates nothing.

## I.0 — THE LURE PAGE (the cold open; where the player arrives)

- **WHAT / WHERE:** the public Record at slug **`the-record-keeps`** — the same cold dead-archive shell as
  `/record/the-record`, with one static downloads block appended. **BUILT** in
  `dashboard/src/app/record/[slug]/page.tsx` (`CURSED-MAP-SITE.md §0/§4`; `the-record-keeps` → 200 with the
  block verified). Reads only `v_record`, `noindex`, no client JS. `the-record-keeps` is the slug the
  founder margin decodes to (a recognition token, never a day-zero gate). Junk slug → in-voice 404 shell;
  bare `/record` → 404. Doubles as the YouTube cold-open.
- **THE DOWNLOADS BLOCK (verbatim, `CURSED-MAP-SITE.md §2`):**
  - *recovered-file row (legible):* `a hold, kept and left. one walk through it remains. the rest of the
    record is kept elsewhere. what is downloaded is only the part that fit in a file.`
  - *download link:* label = filename only **`the-hold.zip`** (`download`, `rel="noopener"`, no button
    styling). `href` → the vignette asset. **[GAP — GO-LIVE asset URL.]**
  - *provenance (Mara's hand, signed):* `i copied it as it was given, page for page, and set the copy where
    fire and water do not reach. i did not keep the seventh. i was not the hand that decides what is kept.
    — m.kept`
  - *README "lie" (technically true):* `the-hold.zip — a small offline map. single player. no mods. about
    fifteen minutes. it does not connect to anything. play it through to the end and it will tell you where
    the rest is kept.`
  - *the counter (STATIC authored, NOT live):*
    ```
    kept: 6
    ████████        ← the struck seventh row (REDACTED_GLYPH, reused from the projection)
    ```
- **PLANTS FIRED HERE:**
  - **{#plant-24}** `kept: 6` + struck-7 — a dead file's tally → re-reads M2-soft / M4-hard / M5-felt
    (six prior keeper-generations; the group is the seventh, kept as hands). Canon home
    `six-were-kept-before-you.md`. Payoff → §V.4.3 {#payoff-24}.
  - **{#plant-25}** README "it does not connect to anything" — mundane reassurance → M4: the *map* connects
    to nothing; the *server* does (true, misread as comfort). Payoff → §V.8 {#payoff-25}.
  - **{#plant-26}** uploader `m.kept` (Mara's dead hand) — a person who left a file → M4: no person
    uploaded it; the record kept it in a dead hand. Payoff → §V.4.3 {#payoff-26}.
- **WHO / GATING:** public, anonymous, ungated; measures only coarse group progress; never addresses "you".
- **DIRECTOR ACTION:** none at runtime (static-per-build). GO-LIVE: bake the `the-hold.zip` href + host.

## I.1 — THE VIGNETTE, ROOM BY ROOM (`the-hold.zip`, single-player)

- **WHAT:** a datapack + single-player world `.zip`, vanilla Java 21 / 1.21.x, no mods, no pack required,
  ~10–15 min, a linear ~6-room walk, adventure mode, gamerules locked (`doDaylightCycle false`,
  `doMobSpawning false`, `keepInventory true`, etc.). **The 1-room form ships first**; the full 6-room hold
  is P1→P2. **STATE:** spec BUILT (`PROLOGUE-VIGNETTE.md §1–§3`); the `.zip` asset is **[GAP — GO-LIVE]**.
  Register: the Archivist's flat third person — lowercase, declarative, no warmth, no second person, no
  exclamation. The prop has **no Mara hand** (Mara is on the page, not in the map).
- **Room 0 — antechamber (spawn).** Small stone-brick cell; ONE lit lantern; closed iron door; one sign:
  > a hold was kept here. read what is left, and go on.
  Plants **{#plant-04 (rehearsed)}** kept-light (a lantern burning where no one lit one).
- **Room 1 — the record room.** Lectern book = the public half of `the-record-opens` (FACT 1/2 only):
  > the record opens. it was open before. // the living are written here, each by the name they answer to,
  > and against each name a column is left open. // the watching has already watched. no one is told. that
  > is the way of it.
  First-entry `tellraw @s`: *the count begins. the living are written by name.*
- **Room 2 — the doused hearth.** Basalt fireplace + a campfire that is OUT (player cannot relight,
  adventure mode). On entry: *a fire was kept here. it is out. it was not put out by any hand that is still
  here.* Plants **{#plant-27}** (the Seventh's *unnamed* texture).
- **Room 3 — the wall of marks.** Six carved marks + a seventh scraped blank. On entry: *six are marked.
  there is a seventh place, and no mark in it. the record will not keep the seventh.* Plants **{#plant-02}**
  (FACT-2 miscount, inert) + #27. Rooms 2 & 3 are the Seventh's only appearance — **texture, never
  attribution**; they re-read in-server at M3→M4 (→ §III.D.2, §IV.4). Spoiler surface = zero.
- **Room 4 — the long walk (the conduct).** A corridor with one lever; passing it arms a scoreboard
  conduct `hold.passed_seventh` (default **1** = "walked past the blank"; flips to **0** only on a
  deliberate ~3s linger in front of the room-3 slot, `hold.linger_7 ≥ 60 ticks`). True by construction in
  single-player — **never a name, no precision risk.** **[GAP — GO-LIVE:** `tick.mcfunction` region volumes
  are coord-specific.]
- **Room 5 — the hand-off (the pointer).** Closing lectern book (the pointer, no scare):
  > the rest is not kept in this hold. it is kept where the others are. bring them, and come to the place
  > named below. the record is open there. it was open before you.
  The conduct line (states the conduct, never the player): *the seventh was passed. it has been noted.* (if
  =1) / *the seventh was stopped for. it has been noted.* (if =0). **Rehearsal only** — never transmitted
  to the server. Beside the book, in plain text: (1) the **server address** (no decode required to act)
  **[GAP — GO-LIVE bake]**; (2) the **rune string** that decodes later in-server to **`the-record-keeps`**
  (the same founder-margin key, **{#plant-11}**, a recognition token) **[GAP — GO-LIVE render]**. The one
  plain ignition action, small, beneath the address: *say one word in the place named below, when you are
  all in. say kept.* **The map ends. No frame-break has happened. The map behaved like a map.** (Dread is
  banked for the server — the quiet-map → "the server knows you" contrast IS the scare.)
- **ANTI-METAGAME (what a cracked `.zip` reveals):** only the FACT-1/2 public lines, the `passed_seventh`
  flag logic (no name), the server address (handed out anyway), and the rune string (a token, no key). No
  FACT past 1/2, no keeper name, no cipher key, no server flag, no Supabase URL.

## I.2 — IGNITION (the frame-break trigger, `#the-record`)

- **TRIGGER → EFFECT:** the group gathers; a **human posts in `#the-record`** (the word `kept` is the
  in-fiction prompt, but any message works). The BUILT `messageCreate` scan
  (`discord/src/bot/index.ts §the-record scan`, L99–145) treats every non-bot message as a possible answer
  and runs the same resolver `/answer` uses (self/bots/webhooks ignored). The detected signal flips
  `arc_state.flags.prologue_ignited`; `decidePrologue` advances `dormant → ignited`, fires the one-shot ack
  (`postAck`), and unlocks the curatorial drip (suppressed until ignition, `decide.ts:76–83`).
- A map-playing group arrives **pre-ignited at the door**; the frame-break is their first server beat. A
  cold-join group gets the in-server prologue unchanged.
- **[GAP — TO BUILD] the literal `prologue_ignited`-SET write.** `prologue.ts` is the pure ordering POLICY
  and READS `ignited`; `autonomy.run.ts` READS the flag. The listener that WRITES `prologue_ignited = true`
  on a `#the-record` post is referenced (`plugin.yml:32`, `prologue.ts:31`) but no module performs the set —
  a wiring gap to verify at GO-LIVE.

## I.3 — THE FRAME-BREAK (the central scare, in-server)

- **FB-1 — the ack, but pointed (BUILT, `voice.ts:82` `recordOpened()`):**
  > ▒  the record is open. it was open before you.
  Fresh off the map, "it was open before you" reads as a callback to the hold they walked. Fires as the
  one-shot ack at ignition (`postAck`).
- **FB-2 — the one precision-gated frame-break line — [GAP — TO BUILD: `voice.recordFrameBreak()`].** Default
  body = the GROUP-FACING count-callback ONLY (verbatim, canon home `six-were-kept-before-you.md`):
  > six were kept before you, and the count of them is on the page you found. a file is not kept. hands are
  > kept. you are not a file here.
  States the **6** from inside the game — a thing a static map could not do. No name; true for everyone.
  **THE CUT (S5):** the map-conduct callback ("you passed the seventh *too*") is CUT from the default — the
  offline flag never reaches the server, so a server-side assertion would be a precision lie (INV-16). FB-2(i)
  is P2-only on a real `from_map` transmission the slice does not build. **DEFAULT-SAFE:** with no clean
  measured signal, fire only `recordOpened()` and let the next in-server "it knows you" beat carry it. The
  frame-break is never traded for precision. **STATE:** `recordFrameBreak()` is NOT in `voice.ts` (the voice
  object ends at `keptUnlitDeep`). Owed: one new key, body verbatim, must pass `registerDisciplineSelfTest`.
- **FB-3 — the knob drops.** After the frame-break lands, loudness returns to baseline and never rises again
  in M1. The map was the loud thing; the server is patient.
- **REVEAL DISCIPLINE:** nothing is witnessed mutating. The server lines are text the Watcher *says*, in
  register — it does not appear, does not threaten, counts and stops.

## I.4 — GATE M0-remote → M1

The group gathers and posts in `#the-record` (the BUILT detector), arriving pre-ignited. **Gate 1→2** (felt
later): first report found + group-playtime threshold.

---

# SEGMENT II — MOVEMENT I (ESTABLISHMENT)

> Source: `playthrough/2-movement-1.md`. Drama budget LOW. The single calibrated-loud beat is the Cold-Start
> Prologue frame-break (§I.3). It rises once, then drops to baseline and never rises again in M1. Everything
> else is inert plants, computed-but-mute engines, and the literacy on-ramp.

## II.1 — DAY 1: THE NOTICE OPENS

### II.1.1 — The base lectern fills: the Hold-Book appears
- `LecternFillBeat` writes the Hold-Book onto `stone_of_reckoning`'s companion lectern (one book, two faces).
  The M1 face = the Archivist's flat living-habit rows AND, on its last page, Brann's down-count docket
  `lamps kept: [N]`. Opening corpus = `the-record-opens.md` (verbatim core, FACT 1 + FACT 2):
  > so the count begins. the living are written here, each by the name they answer to, and against each name
  > a column is left open. nothing is owed yet. but the column is open, and an open column is a thing that
  > fills. … the record only knows what was done, and writes it true.
  Buried last lines (FACT 14 + the seventh):
  > the record does not close at the rite. when the keeping is done, it does not let the writing go. … six
  > are named in full, and there is a seventh mark the record will not [...]
- Plants **{#plant-05}** (the blank give-back column → §II.2.5 Vaun's stone; pays at §V) + **{#plant-06}**
  (the down-count muster — the **bait** for Iss's M4 lie; payoff → §III.C.7 / §IV.3.5 {#payoff-06}).
- **DIRECTOR ACTION:** ship as a **static book** until the count is bound to real muster state
  (`INTEGRATION-V2 §A3`). The `keeper_record.ts` producer + live count are **[GAP — TO BUILD].**
- SEED ROW `m1-record-opens` (`outcome_type: lore`, voice `oracleLore`): accepted answers include
  `the record counts the living by name`; gates nothing.

### II.1.2 — Ignition (detected signal) → ack
`IgnitionListener` detects EITHER the base lectern read OR a `#the-record` post → flips `prologue_ignited` →
fires `voice.recordOpened()` (BUILT): *▒  the record is open. it was open before you.* The curatorial drip is
suppressed until ignition. **DIRECTOR ACTION:** none (the `acked` one-shot guard prevents re-fire).

### II.1.3 — The first report (the Cold-Start Prologue anomaly)
- The first report is retargeted to the base hot-cell (`first_report_lectern_01`, `sites.yml:61`, `type:
  report_lectern`, coords null — **[GAP — UNPLACED]**), paired with one lit marker that was not there before
  (`first_marker_01`, carved with one rune-glyph, illegible until the Rosetta).
- **The report body — two precision-gated forms (BUILT, `voice.ts`):**
  - un-named FACT-1 fallback (default, `prologueUnnamed()`):
    > ▒  a thing was set out in your place that was not there before. it carries a mark you cannot yet read.
    > it has been keeping a count of you. the count began before you found the mark.
  - named form — ONLY on a single OVERWHELMING measured signal (`prologueNamed(name, custom)`):
    > ▒  a thing was set out in your place that was not there before. it carries a mark you cannot yet read.
    > it knows the one called {name} has not {custom}. it was noted before you knew there was a record to
    > note it.
  - **PRECISION LAW:** named iff `overwhelmingSignal && signalName`. **Currently `autonomy.run.ts:128`
    hardcodes `overwhelmingSignal: false`** → the named form never fires until a real measurement sets it.
- **{#plant-04}** the lit marker's glyph re-reads in M2 via a1z26/atbash → `KEPT`/`BEGUN` (→ §II.2.2 /
  §II.2.5 {#payoff-04}). **[GAP — GO-LIVE:** `first_marker_01` build/glyph.]
- **[GAP / SEAM — voice-key name mismatch]:** `prologue.ts` + `autonomy.selftest.ts` name the report keys
  `recordOpenedNamed` / `recordOpened`, but `voice.ts` exposes `prologueNamed` / `prologueUnnamed` (and
  `recordOpened` is the ACK line, not the report). The resolver that maps the decision's `reportVoiceKey` to a
  `voice.*` call is the seam to verify/build. **[GAP — TO BUILD.]**

### II.1.4 — The frame-break (the central scare)
The count-callback line fires (default-safe; CUT map-conduct overload) — full content is §I.3 FB-2. **[GAP —
TO BUILD] `voice.recordFrameBreak()`** is not yet in `voice.ts`.

### II.1.5 — The knob drops (FB-3). Loudness returns to baseline for the rest of M1.

## II.2 — DAYS 1–5: THE FIELD STANDING OPEN (non-linear doors + inert plants)

### II.2.1 — The server-icon rune ring (the master rabbit hole)
The server icon is a ring of six runes, **unremarked** (no post, no callout). In-road **(a)** to literacy.
**[GAP — GO-LIVE asset:** the rune-ring icon must be authored.]

### II.2.2 — The literacy gate (`rosetta_known`) — TWO genuinely-different in-roads
The one true M1→M2 gate (the fairness fix — two doors that are *not one door*).
- **In-road A — `a1z26-tick-stave`** (number puzzle, no runes). SEED: accepted `learn them as we learned
  them` / `count the staves then read` / `read them as we read them`; `main_beat` → `set_flags
  {rosetta_known:true}` + advancement `observance:the_ring_is_whole` at `first_marker_01`.
- **In-road B — `rosetta-ring`** (icon-ring metadata leap). Accepted: `bow offering kept light deep line
  unspoken sacred beast`; same flag + toast.
- **In-road C (optional) — the founder margin** `learn-them-as-we-learned-them.md` (BUILT):
  > read the ring sunwise from the topmost mark. six marks, and they hold the six in order: first, the Bow —
  > then the Offering — then the Kept Light — then the Deep Line — then the Ward — and last, the Covering.
  and the later-hand margin (FACT 3/4): *they are not ours. we did not raise them. we were taught, and the
  teaching is older than the first of us. … read sunwise. begin at the top.*
- **[SEAM — flagged]:** the BUILT doc names **Bow / Offering / Kept-Light / Deep-Line / Ward / Covering**,
  but the seeded `rosetta-ring` accepted answer replaced `ward`/`covering` with **`unspoken sacred beast`**
  (a documented audit fix — `ward`/`covering` were orphans). The rune-ring carving + in-world doc are a
  **lagging surface** to reconcile to the seed at go-live. **[GAP — reconciliation owed.]**

### II.2.3 — The digit-literacy gate (`reckoning_known`)
SEED `reckoning-rosetta`: the Stone of Reckoning teaches digit-glyphs + sign-marks (`-`/`,`). `main_beat` →
`set_flags {reckoning_known:true}`; advancement `observance:the_count_is_yours`. Makes coordinates into
*places* (INV-14). Coord-bearing rows stay inactive until `stone_of_reckoning` is placed. **[GAP — GO-LIVE
build;** non-cipher row TS-FORGE must add to `NON_CIPHER_KEYS`.]

### II.2.4 — `m1-named-habit` (the terminal-dread dead-end)
A `dead_end`: a TRUE reading that opens nothing. Accepted: `it named my habit before i knew it was a custom`
/ `i was measured before i was told`. Voice `oracleDeadEnd('name')` (BUILT): *a true name. it keeps no door.
some things are only true.* Carries no `next_puzzle_key`. The teeth belong to the liar, never the Watcher.

### II.2.5 — The inert plants seeded across M1 (all dormant), in causal discovery order
- **{#plant-09}** `kept here before you` rune line at `first_marker_01` (`name-where-never-been`, FACT 16) —
  also an accepted answer on the M4 `true-walk-arrive` row, so the M1 carving + M4 walk-endpoint rhyme. Inert:
  a dead keeper's epitaph. Payoff → §IV.3.4 / §V.4.4 {#payoff-09}.
- **{#plant-08}** the six `UNKEPT` maker's-mark glyphs (one per eventual stone, previewed at the M1 teaching
  stone). Inert: each keeper's grief-mark. Read in **fall-order** = `UNKEPT`; legible M4 via `meta-unkept`.
  Payoff → §III.3 / §IV.3.6 {#payoff-08}. **[GAP — GO-LIVE glyph carvings.]**
- **{#plant-10}** `we-cut-the-names-before-the-keeping.md` (BUILT, FACT 13b): *do not read the date as the
  day they die. read it as the day they are kept. the founders did not cut a death into the stone. they cut
  an appointment.* Plus the later hand naming the threshold "death-clock." Inert: eerie ritual order. Payoff →
  §V.4.1 {#payoff-10} (the grave opens from the inside).
- **{#plant-11}** `kept-in-more-than-one-place.md` (BUILT): *the record is kept in more than one place,
  against the loss of the first.* + the keyed line `[ decodes to: the-record-keeps ]`. Inert: careful
  archivists. Payoff → §II.7 (`record-url`) / §V.4.3 {#payoff-11}.
- **{#plant-13}** Vaun's `you do not keep the first thing` (BUILT, `journals-vaun-mara-sella.md`): *Whatever
  the deep gives up first … you carry back to the cairn and give it to the deep again. You do not keep the
  first thing.* Inert: one custom among many. Payoff → §VII (Sacred Beast fork) {#payoff-13}. Read alongside
  the child's-hand line: *iss says you cannot keep the first thing but vaun keeps everything. i counted
  vaun's lamps.*
- **{#plant-14}** the single Sacred Beast established + the prior-keeper count fragment
  `nine-grey-one-white.md`: *first winter: nine grey, one white. all accounted. … third winter: eight grey,
  two white. … i did not see it turn.* Inert: a farmer's note. Payoff → §VII / FACT-15 visual §V.4.4
  {#payoff-14}. INV-13: only the one glowing Beast is tracked; the cosmetic Pale never glow.
- **{#plant-15}** Mara's `the-cold-square.md` (BUILT, book-cipher): *foot to the square, hand to the cut,
  word to the dark, the three at once.* + *i typed the word into the dark, alone, and nothing opened, because
  a thing done alone is one thing and the threshold counts three.* Inert: a sad memory. Payoff → §IV.3.2
  (three-hands coop gate) {#payoff-15}.
- **{#plant-16}** `brann-not-here-to-see-it-noted.md` (BUILT, FACT 9), surfaced via
  `voice.offlineReportPlant(name)` (BUILT): *the one called {name} is not here to see it noted. the record
  notes the not-here as plainly as the here. it keeps the empty place at the table.* Inert: "the record
  watches you logged off." Payoff → §IV.2.3 (the offline haunt named) {#payoff-16}.
- **{#plant-12}** Mara's `a-closer-count-of-the-quick.md` + SEED `difficulty-mara` (voice `oracleLore`): *i
  read that the record keeps a closer count of the quick. i called that a cruelty for a winter. i do not call
  it that now, and i will not say what i call it.* Inert: a muttering. Payoff → §II.5 (the difficulty engine
  goes live in M2) {#payoff-12} (de-slopped — does NOT name "mercy").
- **{#plant-07}** the coordinate number-pair near `stone_of_reckoning` (`coords-to-real-place`). Inert: a
  tally. Payoff → coordinate readings once `reckoning_known` {#payoff-07}. **[GAP — GO-LIVE carving.]**

### II.2.6 — The engines that compute-but-stay-mute in M1
- The dynamic-difficulty engine (`reckoning.ts`) computes group cadence but is **mute** in M1 (goes live M2).
  Its only M1 trace is `difficulty-mara`. **[GAP — TO BUILD.]**
- The herd single-Beast is established (one glowing Sacred Beast tagged; cosmetic Pale not yet spreading; the
  `SacredAnimalBeat` spread mode is M2+). **[GAP — TO BUILD:** spread + `pale_cosmetic` PDC.]
- The showrunner authors the daily drip via `voice.drip(tone)` (BUILT default *▒  something is set out where
  the marks are kept. read it, if you can.*) + conduct reports via `voice.reportObserved(...)` (BUILT). Each
  personalized call has a deterministic fallback (SPOF mitigation). **[GAP — TO BUILD:** the showrunner
  `run.ts` loop.]

### II.2.7 — The Set-A surface NPCs (the human counterweight, from Day 1)
> `npc-dialogue.md`. Contractions/capitals/named feelings are LEGAL here. In M1 they sit warm/neutral and
> plant future contradictions. BUILT as a dialogue spec; the in-game NPC bodies are **[GAP — GO-LIVE].**
- **Aro — the rumor-broker who lies** (`aro`). M1 greet: *Ah — fresh boots. Sit, sit, you're letting the
  cold in. You want the way down, you want the right person, and lucky you, here I am.* His `lie.cross` is the
  same lie Iss carved (planting the M4 catch); Iss-adjacent (flips cold sticky on `iss_caught`). In M1 he
  parrots the forged eighth law + the Unlit Deep falsehood.
- **Wenna — half-remembers the ways as folk-superstition** (`wenna`). M1 rumor: *Gran used to say there were
  seven somethings you had to mind down there. Seven. I only ever remember six and I always forget a
  different one…* + the unspoken-name folk-charm.

### II.2.8 — The first apparition of M1
The apparition web is essentially silent (the loud beat was the frame-break). The only M1
"apparition-adjacent" event is the **lit marker that was not there before** (§II.1.3) — a discovered,
never-witnessed object, no figure. The generic "Watcher at the Edge" (Vaun-shape) *may* fire late-M1 on a
measured `hoardedScore`/`soloMiningRatio` signal but defers to `apparitionClaim` (**[GAP — TO BUILD]**); in a
clean M1 it stays unfired (precision over recall).

### II.2.9 — Gate M1 → M2
First report found + group-playtime threshold. No countable step-ladder; the literacy gate (`rosetta_known`)
opening the six-stone field is the felt transition into Movement II.

---

# SEGMENT III — MOVEMENT II (THE WAYS) + THE NETHER LANE

> Source: `playthrough/3-movement-2-nether.md`. The six-stone field, the two Rosettas, the meta-acrostic
> plant, the Whisper economy going live, the cross-surface handoffs — then the optional Nether lane.

## III.0 — ENTRY STATE
Carried from M1: `rosetta_known` (via `rosetta-ring` OR `a1z26-tick-stave`), `reckoning_known` (via
`reckoning-rosetta` — coord-bearing rows inactive until `stone_of_reckoning` is placed), `prologue_ignited`.
Whole active group; no per-player gate except Brann's night-gate + `reckoning_known` on coordinates.
**[GAP — TO BUILD]** the dashboard health-panel surfacing `rosetta_known`/`reckoning_known`.

## III.1 — THE SIX-STONE FIELD (a field, not a row; any order; resolver ignores movement)

Six `keeper_stone` answer-sites. Omitting `puzzle-key:` on a stone means the sign resolves against ALL open
puzzles (any solved clue answerable at any stone). Answer-verb is the same on every stone — **edit a sign
within the stone's radius**; on a miss the sign blanks (no error, no hint). All coords `null` **[GAP —
GO-LIVE].** Build palette: a single floor-set/low-canted deepslate/blackstone slab ~3×4, angled so the camera
must tilt down (the bow), one soul-lantern at a fixed offset. Carving = a sign/text-display in
`observance:runes` carrying the keeper's bound plaintext from `clue-specs.ts` — **NEVER edit the plaintext
(the X1 guard round-trips it).** Given here in **fall-order** (the meta-acrostic key order); in MII the
player may hit them in ANY order.

### III.1.1 — STONE-VAUN — caesar (shift 3) — `GIVE THE FIRST OF THE DEEP BACK TO THE DEEP`
The shift IS his hoarding ("i had three of each"). Framing (verbatim): header `IRON THREE · SALT THREE ·
GRAIN THREE · OF THE DEEP THREE · I HELD THREE OF EACH AND THE COUNTING WAS WARM`; below `I LEARNED THE COUNT
I DID NOT MAKE IT · THE LAND COUNTS FIRST · I KEEP THE TALLY AFTER`; maker's mark = *a vertical rule cut deep
with nothing carved beside it* — the blank "given-back" column = Vaun's **UNKEPT** mark {#plant-08} and the
physical plant of **{#plant-05}** (→ §V). Accepted: `give the first of the deep back to the deep` / `the land
counts first` / `i counted them in the dark and gave none back`. `outcome_type: lore`; `oracleLore`: *vaun
counted everything and gave nothing back. the given-back column of his ledger is blank. the land counts
first, and it had already counted him.* (FACT 5 + 4). Source `counted-them-in-the-dark.md`.

### III.1.2 — STONE-MARA — book cipher (the six-book shelf) — SPINE KEY → the descent
Bound plaintext: `DESCEND AND BOW AT THE UNBROKEN LIGHT`. Carved ref-string (digit glyphs): `1-1-1 2-1-1
2-1-2 2-1-5 1-1-2 3-1-2 3-1-3` (reads as a finding-list). Codebook = the six-book lectern shelf at
`kept_light_home_01`. Framing footer (the teeth): `…DO THE THING IT TELLS YOU · GO WHERE IT SENDS AND THERE
DO AS IT SAYS · NONE LEFT AT THE DOOR`; maker's mark: `I NEVER WENT DOWN · I ONLY EVER READ THE WAY DOWN`.
Accepted: the ASSEMBLED SENTENCE (not triples). `outcome_type: next_clue` → `oracleNextClue()`: *kept. the
way goes on — look where the marks were not, before.* Sets `mara_read:true`; `next_puzzle_key:
undercroft-descent` — **the spine in-road to Movement III** (→ §IV.A).

### III.1.3 — STONE-SELLA — atbash/mirror + bearing → the Seventh side-quest
Bound plaintext: `SOUTH BY THE FAR WATER WHERE SHE DID NOT COME BACK`. Read in the lake's reflection at
`the_far_water` (the reflection is the only place it resolves). The seventh-thread seed (smallest framing):
`DO NOT LET THE COUNT BE SIX ONLY · COUNT AGAIN AT THE SHORE · THE LAST MARKER IS NOT THE LAST`. Accepted:
`south by the far water where she did not come back` / `the last marker is not the last` (+ the unsigned
far-water coordinate). `outcome_type: side_quest` → `oracleSideQuest()`: *this is not the way. but it is a
way. follow it, if you would.* Sets `seventh_suspected:true`; `next_puzzle_key: seventh-shrine` — **in-road A
to the Seventh thread** (→ §IV.4).

### III.1.4 — STONE-ORIN — substitution + crouch-only reveal → the Threshold (M4)
Bound plaintext: `I THOUGHT IT SMALL IT WAS NOT SMALL`. Carving faces the floor, legible only from a crouch
under the lintel (the crouch IS the Bow). The run ends mid-thought: `…I THOUGHT IT SMALL IT WAS NOT SMALL I —`
(the stone broken away) — the broken `i —` is **{#plant-03}** (FACT 6 + the `E` glyph of UNKEPT; → §IV.3.6
{#payoff-03}). Lowest margin: `THRESHOLD` (and: FOR THE REST OF THE SENTENCE GO TO WHERE I WAS LEFT.).
Accepted: `i thought it small it was not small` / `threshold` / `the bow is the smallest of the ways`.
`outcome_type: next_clue`; `next_puzzle_key: orin-threshold` (the M4 completion node).

### III.1.5 — STONE-BRANN — flat lore today; the railFence night-stone is STAGED
Two rows, one live:
- **LIVE `stone-brann` (`lore`).** No cipher, night/black-moon framing. Accepted: `one fire was never doused`
  / `do not close your eyes here` / `the one fire that will not be doused`. `oracleLore`: *count the fires at
  night. one of them never went out, and no hand tends it. they had one word for the people and the flame and
  the cold stone. do not close your eyes here.* (FACT 11 + 12). Source `do-not-close-your-eyes-here.md` (read
  only at night).
- **THE NETHER INERT SEED rides this stone's framing — {#plant-28}:** *the fire we keep is not ours. it is
  lent… below the below.* Inert texture; pays off in the Nether lane (→ §III-Nether / §IV.E). **[GAP — TO
  BUILD]:** this framing line is specified in WEB-MASTER but **not yet carved into a seed row or
  `cipher-plaintexts.md`** — framing-only, carried at world-build; must NOT become a bound plaintext.
- **STAGED `stone-brann-cipher` (active=FALSE).** The real railFence node (rails = the fire-count;
  read-by-time, rakes visible only by lit beacon-glow after dark). Bound plaintext (when built): `count the
  fires before you sleep`. Ships staged until **TS-FORGE adds the railFence `CLUE_SPECS` entry + removes
  `stone-brann` from `NON_CIPHER_KEYS`.** `next_puzzle_key: undercroft-descent` (the second in-road to the
  descent). **[GAP — activation.]**

### III.1.6 — STONE-ISS — vigenère (key = ISS) — the warmest stone (the trap)
Bound plaintext: `THE ONE WHO TURNED AWAY`. Applied warmly to his own letter the doctrine reads comforting;
his key turned on the OTHER stones reads the word the corpus keeps for him. Framing: opening (warmest carving)
`BE EASY · …GRIEF CARVES CROOKED · LET ME SET IT STRAIGHT THE WAY A FRIEND WOULD`; the planted lie (FACT 7)
`THE WAYS ARE A WALL · KEEP THE TEN AND YOU ARE INSIDE IT · THE WATCHING STAYS OUT IN THE COLD…`; the key
handed over `THE KEY IS MY OWN NAME AS IS RIGHT AND CUSTOMARY…`; the later margin (the catch begins) `WE
CHECKED THE LOCK · TURN HIS KEY ON THE OTHER STONES AND IT DOES NOT SAY WHAT HE SAID · IT GIVES THE WORD WE
KEEP FOR THE ONE WHO TURNED AWAY · ASK FIRST WHAT A WALL IS FOR`. **Two rows:**
- **`stone-iss-wall`** (`next_clue`, `max_attempts:6`) — the skeptical name-as-key reading. Accepted: `the
  one who turned away` / `iss`. Sets `iss_key_turned:true`; `next_puzzle_key: iss-doubt` → the catch.
- **`iss-warm`** (`next_clue`) — the WARM MISREADING. Accepted: `the ways are a wall against the watching`.
  Sets `iss_trusted:true`; `next_puzzle_key: iss-dead-shrine` (routes you to HIS coordinate — a grave). **The
  false walk in-road.** Source `the-ways-are-a-wall.md`.

## III.2 — THE TWO ROSETTAS (literacy already live; re-touched here)
- `rune_rosetta` (founding ring, sunwise Bow-first; ring carves `bow offering kept light deep line unspoken
  sacred beast`; `rosetta-ring` `main_beat` sets `rosetta_known`, fires `observance:the_ring_is_whole`).
  **[GAP — coherence]:** `cipher-plaintexts.md` L364 still lists `Ward · Covering` (the old orphans); the
  seed is authoritative; the carving must follow the seed (the §II.2.2 lagging-surface drift).
- `stone_of_reckoning` (digit Rosetta; `reckoning-rosetta` `main_beat` sets `reckoning_known`; fires
  `observance:the_count_is_yours`).
- `a1z26-tick-stave` (the runes-free second literacy door at `first_marker_01` — kills the "two doors that are
  one door" lie).

## III.3 — THE META-ACROSTIC PLANT (UNKEPT) — planted MII, assembled M4 {#plant-08 detail}
Six maker's-mark glyphs (Vaun's is the empty given-back column). Read in **fall-order** they spell `UNKEPT`.
The row `meta-unkept` is **STAGED active=false**; the cold Iss/Keeper states the fall-order key at the catch
(M4) before assembly. The order-key (`the-order-the-stones-fell.md`): *vaun first, who hoarded the light and
starved in it. / then mara, who read and never did. / then sella, who walked to the far water. / then orin,
who would not bow until there was no one to bow to. / then brann, who slept on the black moon. / then iss,
who spoke the thing and lied about the wall.* The self-correcting lock: ring-order yields non-words ("the
lock telling you your key is turned the wrong way"). A practice-run rhymes with the prologue marker glyph
(KEPT/BEGUN, #04). When staged active at M4 (`oracleMetaUnkept`): *six marks, one to a stone. read them in
the order they fell. the word is the one each did not keep.* The naive "first letter of each plaintext" form
is **CUT** (X1 guard — the acrostic lives in carved FRAMING, never the bound run). **[GAP — GO-LIVE]:** the
six glyph forms (only Vaun's empty column is specified verbatim); the other five must be authored so
fall-order = UNKEPT and ring-order = nonsense. Payoff → §IV.3.6 {#payoff-08}.

## III.4 — THE LIAR THREAD (the false walk earnable in MII; catch deferred to M4)
- **`iss-dead-shrine`** (`dead_end`, active=true) — THE LOAD-BEARING RED HERRING; Iss's coordinate genuinely
  works and leads to a **grave**. Accepted: `the dead shrine` / `the cold hearth` / `nothing is kept here` /
  `west and down` / `west and down to the cold hearth`. `oracleDeadEnd('place')`: *that is the place. it was
  read true. it leads nowhere it has not already led you.* WHERE: `the_cold_hearth` (doused hearth + grave
  slab (Iss) + a second effaced marker (the seventh); cold palette, no light). **This same anchor is
  temporally layered** — the seventh-deep (`the_unwriting`) opens beneath it only post `iss_caught` +
  `seventh_named` (→ §IV.4).
- **THE PROPHET'S WALL** (placed in Iss's field in MII):
  - `prophet-wall-comfort` (`dead_end`, kind `prophet`) — wide warm promises, each a true-but-empty solve
    that opens nothing. Accepted: `keep the ten and you are inside it` / `the watching stays out in the cold
    and counts and cannot touch you` / `be easy the wall keeps the watching out`. `oracleDeadEnd('prophet')`:
    *every word of it decodes. each is true, and each opens nothing. read who carved it, after.*
  - `prophet-wall-name` (`dead_end`, kind `prophet`, `max_attempts:6`) — the hidden columnar acrostic spells
    Iss; *the wall was his.* Accepted: `the one who turned away` / `iss carved the wall` / `read the first
    marks down the one who turned away`. Re-reads at the catch as **{#plant-19}** (→ §IV.3.8 {#payoff-19}).
  - **[GAP — TO BUILD]:** the prophet's wall has **no `sites.yml` entry of its own** (lives in Iss's field
    near `stone_iss`); a site placeholder owed at GO-LIVE.
- **THE FORGED EIGHTH LAW** `forged-eighth` (`dead_end`, kind `known`, active=true; FACT 7b) — *a diligent
  group obeys it; nothing pays.* Accepted: `the eighth is the covering of the hands` / `cover and be counted
  clean` / `to cover ones own` / `the founders set the ways and did not finish the count`.
  `oracleDeadEnd('known')`: *this is carved, and you have read it true. it is not kept. a thing can be set
  down and never be a way.* Source `the-eighth-way.md` (the lie's proof: *there is no toll for not doing
  this. i kept the seven a winter and skipped the covering on purpose, every dusk, to see. nothing came.*).
  Aro parrots it as real (→ §III.7). Payoff → §IV.3.7 {#payoff-07b} (the M4 correction).
- **`iss-doubt`** (`next_clue`, active=true) — earnable the moment a group turns Iss's key on the other
  stones. Accepted: `we checked the lock` / `his key is his own name and his name is the one who turned away`
  / `ask first what a wall is for`. Sets `iss_doubted:true`; `next_puzzle_key: no-wall-catch`. **The catch
  itself (`no-wall-catch`) is M4** (→ §IV.0 / §III.C in segment IV).

## III.5 — THE NON-Iss MII PLANTS (placed inert; pay off later)
- The Seventh as rumor: `stone-sella` sets `seventh_suspected`; `seventh-shrine` (`side_quest` →
  `seventh_found, whisper_budget_earned`) earns Whisper budget. Deep stays sealed (→ §IV).
- **{#plant-21}** the future-dated grave (`future-dated-grave`, FACT 13b) carved near `the_threshold`. Reads
  as a death clock (the misread IS the mechanic). Its date == the single Accepting instant. **[GAP — TO
  BUILD]:** the carved name+date is runtime (`grave.ts`), not a static seed row. Payoff → §V.4.1 {#payoff-21}.
- First living-name carve `name-where` (`dead_end`, kind `place`, active=true; FACT 16) — Accepted: `the
  record files the living by place not only by name` / `against each name a ground` / `before you was never
  about strangers`. Produced by `name-where-never-been.ts` (real, ACTIVE-only, subjects rotate — a chorus,
  INV-16). WHERE `carve_anchor_01..03`. (The #09 plant's mechanism.)
- The herd's second Pale appears (`herd-conversion`): *weren't there one of those?* WHERE `herd_anchor`.
  Cosmetic, never glowing/tracked. Producer `herd.ts` (real).
- The coop-gate bound word becomes earnable (Iss's Vigenère resolves to it) but the Threshold stays sealed
  (`bound-word` STAGED active=false until `iss_caught`).
- The dynamic-difficulty plant `difficulty-mara` (`lore`, active=true; the engine `reckoning.ts` goes **live**
  here — a group crushing the ciphers finds the next drip withheld and the register cooled). The difficulty
  scalar NEVER touches the Whisper backstop (INV-15). Payoff of {#plant-12}.

## III.6 — THE WHISPER ECONOMY GOES LIVE (MII)
The player-controlled safety rail — ask for a hint, pay a reversible toll. Lines (BUILT, `voice.ts`):
`whisperReply(tier, hintBody)` (tier ≤1 → *look again at what repeats. it is not stone. it is sound.*);
`whisperToll()` (*i will keep something of yours while you think. your light, for tonight.* — reversible);
`noBudget()` (*there is nothing more i will say of this. not yet.*); `whisperUnknown()` (*i have no words for
that one. not the ones you are owed. not yet.*). Budget is EARNED, not free — first earn-points are MII
side-lanes (`seventh-shrine` sets `whisper_budget_earned`; `seventh-cause`/Nether `nether-forge` add additive
bonus). INV-15: the difficulty engine never removes this rail. **[GAP — TO BUILD]:** the Whisper *mechanism*
(no `whisper_budgets` table or budget-spending module in this slice) + the per-puzzle `hintBody` strings.

## III.7 — THE CROSS-SURFACE HANDOFFS IN MII
- **Minecraft ↔ Discord:** the `keeper_stone` sign + the Discord oracle resolve against the SAME normalized
  `accepted_answers`; a clue solved on either surface records once.
- **The Record website** `record-url` (`lore`, active=true): the founder line decodes to the off-world path.
  `recordElsewhere(fragment)`: *the record is kept in more than one place, against the loss of the first. the
  path is the record keeps.* The page un-redacts six entries in lockstep with stones actually read; the Iss
  card carries the stego rune-layer (a second door to the Vigenère key). **[GAP — TO BUILD]:** the Iss-card
  stego payload + the live lockstep-unredaction page (`record-projection.ts` referenced; the live page owed).
- **Discord NPC echo (Aro parrots the forged law):** `aro.lie.cross`: *The painted line? Step right over it,
  friend… That's where it gets good.* Old Pell's anti-rumor `truth.watched`: *It doesn't chase… It waits, and
  it watches, and it takes what stops being watched. So be watched.* (INV-1). These flip warm→cold only at the
  M4 catch (`iss_caught`). **[GAP — TO BUILD]:** the `npcVoice.ts` registry (proposed, not built).
- **GATE 2→3:** ≥4 of 6 fragments assembled + the final-coordinates path opened.

## III-NETHER — THE NETHER LANE (optional; gates nothing; INV-12/INV-19)
> A deepening lane off MII→MIII — the source the Undercroft's one fire was carried up from ("below the
> below"). All seeded rows ship `active=false` until (a) the dimension world is built AND (b) the upstream
> flag is set. A group that skips the map, the Nether, and the End gets a whole un-shaded Overworld arc.
- **N.0 — THE HARD BLOCKER:** LORE must seal the FACT-11 source clause into `canon-spine` BEFORE any Nether
  build (see Setup B7). Until then the lane is design-only. **[GAP — TO BUILD / BLOCKER.]**
- **N.1 — The plant + the bearing.** MII plant = Brann's framing {#plant-28} (§III.1.5). The bearing-page
  `the-fire-is-lent.md` (Mara's hand, found on the Undercroft lectern-shelf, `requires_flags:
  [undercroft_open]`): *the fire is lent. carry the coal through the burned door and walk the short way to
  where it is kept for everyone.* + Mara's margin (*…a lent thing is carried, and a carried thing is not
  owned, and a thing you do not own you do not get to keep — you only get to not let it go out.*) =
  **{#plant-30}**. INV-14: a bearing, not a coordinate.
- **N.2 — The near pocket (`nether_forge`, world `observance_nether`).** A DELVE not a trek (2 ground walks +
  ≤1 short vertical pocket). A ruined room just past a lit portal: a prior keeper's remains on a deepslate
  slab (placed at world-build, never pasted toward a player) + a doused soul-lantern + the journal
  `the-fire-kept-me.md`. Verbatim load-bearing passages: *i came down to keep the fire because up there the
  lamps were going out … orin had sealed the deep above me. i went under the seal, the last way down, to the
  source.* / *the fire is here. it does not need keeping. it never needed me… i lit a lantern off it anyway.
  habit.* / *the sand is the others… the deep kept them the wrong way…* (soul-sand = deep-time, distinct from
  the present Pale herd) / *the door back up i came through i do not think i would fit through now… i changed
  shape to get down here and i did not change back.* / *i am not letting it go out. i am the part of it that
  does not go out now. that is the keeping… carry it if you must. do not stay.* (FACT 15 felt from the
  keeper's side). Row `nether-forge` (`lore`, active=false, `requires_flags {undercroft_open}`): on-site
  WORD answers (INV-14) — `lent` / `the fire is lent` / `you do not own the fire you carry it` / `the keeping
  was always a carrying`. Reveal: *a keeper came down to keep the fire and was kept by it… the kept light
  upstairs was a coal carried up from here.* Sets `nether_forge_found` (group-scoped colorant — PROPOSED
  `FateInput.netherForgeFound`, NOT wired into `decideFate`; the M5 composer reads it for a tint) +
  `whisper_budget_earned` (additive). Voice key `nether.forgeArrive` **[GAP — TO BUILD]** (silent-at-runtime;
  the `fragment` carries the text).
- **N.3 — Texture anchors (gate nothing):** `soul_gallery` (soul sand = not-kept of deep time;
  `nether.soulSand` line **[GAP]**); `bastion_remains` (the founders' deepest ruined delvings; the P2
  basalt-corridor keeper-glimpse may fire near here, deferring to `apparitionClaim`). Breadth row
  `dest-deep-forge`; rumor card `who-deep-forge` (`cardNetherForge` body **[GAP — TO BUILD]**).
- **N.4 — Activation/reachability:** `requires_flags {undercroft_open}` holds the row closed until
  `undercroft-descent` sets it; removing both lane rows entirely still reconstructs the whole Overworld spine
  (pure deepening, no orphan, no gate).
- **N.5 — DIRECTOR ACTIONS:** seal FACT-11 (BLOCKER); build `observance_nether` + the pocket; export `.schem`;
  fill coords; flip `nether-forge` active=true. No per-session console click. Payoff colorant lands in §V.6.3.

---

# SEGMENT IV — MOVEMENT III (THE UNDERCROFT) + MOVEMENT IV (ATONEMENT, THE KEEPER) + THE END LANE

> Sources: `playthrough/4-movement-3-undercroft.md` (the descent, the A→B swap, the Liar catch, the false
> law, the Seventh, the Unlit Deep) and `playthrough/5-movement-4-end.md` (the Keeper-NPC tree, the atonement
> gate, the M4 cascade, the Seventh spine choice, becoming the keepers, the End lane). M3 is the expedition
> (decode → walk → descend); M4 is the universal hinge keyed on `iss_caught`. The Liar's catch is loud but
> COLD — a correction, not a jump-scare.

## IV.A — THE DESCENT (the lectern-comparator door → the fog dimension)

### IV.A.1 — The instruction in hand (Mara's descent sentence)
Bound plaintext of `stone-mara`: `DESCEND AND BOW AT THE UNBROKEN LIGHT`. The six lectern books read out
*descend the stair when the water is still // and bow your head at the door // the unbroken light waits at the
foot of it // do not write the way down and think it kept // do the thing the marks tell you.* The
load-bearing line `do not write the way down and think it kept` rhymes forward to `pressure-glyph-walk` at M5
and to her Nether margin. **[GAP — GO-LIVE:** `kept_light_home_01` shelf build.]

### IV.A.2 — The descent (`undercroft-descent`)
A **performed** `main_beat` (the cipher was `stone-mara`; this row is the *doing*). Accepted: `descend at the
unbroken light` / `descend through the lectern door`. Effect: `set_flags {undercroft_open: true}`,
`next_puzzle_key: undercroft-fog`, embedded `unlock` → `door_open` at `unbroken_light` (radius 3). Watcher
(`oracleMainBeat`): *what was shut is shut no longer. the record keeps the hand that opened it.* **The
mechanism:** a lectern whose book-page output drives a comparator that holds the descent door — *the record
itself is the lock.* The plugin's `door_open` is the authoritative open (idempotent); the comparator is the
diegetic skin. **[GAP — GO-LIVE:** `unbroken_light` UNPLACED — build the gather-room + comparator door.]

### IV.A.3 — The SECOND in-road to the descent (arg-craft F2)
A group that never cracks Mara reaches `undercroft_open` via Brann's stone OR the Seventh side-quest's earned
Whisper budget. **[SEAM]:** `stone-brann` ships today as flat lore (no cipher, no night-gate); the *intended*
night-beacon rail-fence is **[GAP — TO BUILD: P0-5 re-author].** Until then the actual second in-road is the
Whisper-budget path (`seventh-shrine` → `whisper_budget_earned`). The "two genuine in-roads" claim is
half-built (Mara + Whisper hold it; the night-beacon is owed).

### IV.A.4 — The fog dimension (what they descend INTO)
A separate Multiverse void world with a datapack fog (`ambient_light: 0`) — no block-light bleed, no sky; the
ONE kept fire is the only photon source (the literal staging of FACT 11). Entered only via the `door_open` at
`unbroken_light`. **[GAP — GO-LIVE / TO BUILD:** the Multiverse world + fog datapack.]

## IV.B — THE A→B ROOM-REBUILD (the midpoint gut-punch)

### IV.B.1 — Room A on first descent (ordinary) — **{#plant-23}**
The Undercroft altar room as first seen — plain, reads as *the place*, not a trap. Payoff → §IV.B.2
{#payoff-23}.

### IV.B.2 — The room rebuilds WRONG (`undercroft-fog`) {#payoff-23}
`outcome_type: lore`, a witnessed-state node (the world is the puzzle). Accepted: `the room rebuilds wrong` /
`one fire and no one to tend it` / `they did not depart they were kept`. `oracleLore` fragment: *the room
rebuilds itself into something wrong. one fire is kept, eternal, attended by no one. they did not depart. they
were kept. the rite is not a transaction.* The load-bearing reveal `they did not depart. they were kept.` is
FACT 14→15's first cold edge. **MECHANISM (`RoomSwapBeat` — the ONE sanctioned overwrite):** clear-A-then-
paste-B in one `mutateWhenUnwitnessed`, idempotent on a durable `swapped` PDC marker on the region anchor
(governed by the marker, NOT the ledger). **REVEAL DISCIPLINE:** the swap fires only when no player has the
altar region in view — they leave an ordinary room and return to a wrong one (the horror is the omission).
**[GAP — TO BUILD]:** `RoomSwapBeat extends SmallStructureBeat`. **[GAP — GO-LIVE]:** Room-A/Room-B `.schem`.
**[SEAM — resolved/CUT]:** the FAWE "different-schematic-key makes paste-over legal" claim is wrong-and-CUT;
`footprintClear` makes plain paste-over impossible, so the swap MUST go through the marker.

### IV.B.3 — The First Light fork (`fork-light`, irreversible)
A M3 `side_quest` choice at the one fire: draw the M5 token (`light_kept`) or bank it (`light_taken` → the
room stays dark for the arc). Accepted: `draw the light up the stair` / `leave the flame banked and the room
dark` / `carry the kept light` / `bank the flame`. Leaves (BUILT): `forkLightKept()`: *the light came up the
stair on its own. you carried it. that is how it is carried.*; `forkLightTaken()`: *the flame is banked, and
the room it warmed stays dark. the light that was lent is taken, and the deep is colder by it.* **GATES
NOTHING** (colors the M5 close only, INV-12). Pairs with Mara's Nether margin (light is carried, never
owned). Payoff → §V.7 (Fork B) {#payoff-fork-B}.

## IV.C — THE LIAR ENGINE (the one-key-two-doors catch)

### IV.C.1 — The one key, two doors
`stone-iss-wall` (Vigenère, key = Iss's name) yields `THE ONE WHO TURNED AWAY` two ways; the reading posture
forks the route. DOOR 1 (warm misreading) `iss-warm` → `iss-dead-shrine`, `iss_trusted`. DOOR 2 (name-as-key)
`stone-iss-wall` → `iss-doubt`, `iss_key_turned`. Same stone, same key, opposite destinations.

### IV.C.2 — The dead-end taunt (`iss-dead-shrine`) — the load-bearing red herring
Iss's coordinate genuinely works and leads to a GRAVE. `oracleDeadEnd('place')`: *that is the place. it was
read true. it leads nowhere it has not already led you.* (The Watcher states the category, never gloats — the
teeth belong to the liar.) The dead-shrine itself (`the_cold_hearth`) found-marker
(`voice.dest.coldHearth.find`): *a hearth with no fire and no name, at the end of the false road. someone
carried a letter here for one a generation drowned. below it the floor is sealed, and the seal is a name, and
the seal does not open from this side. this was the surface of a deep you cannot yet reach.* **TEMPORAL
LAYERING:** the deep beneath (`the_unwriting`) opens only post `iss_caught` + `seventh_named` (→ §IV.4). **[GAP
— GO-LIVE:** `the_cold_hearth` UNPLACED.]

### IV.C.3 — The catch (`iss-doubt` → `no-wall-catch`)
`iss-doubt`: turn Iss's key on the OTHER stones; it disagrees with every honest carving. Accepted: `we
checked the lock` / `his key is his own name and his name is the one who turned away` / `ask first what a wall
is for`. → `no-wall-catch`, `iss_doubted:true`. **THE CATCH PROPER** `no-wall-catch` (`main_beat`, FACT 8):
re-walk a clue falsely marked "kept · solved"; the Stone-after contradicts Iss line for line. Accepted: `no
wall was ever built here` / `they were the reaching let in` / `what iss sent you to was a grave` / `back to
vauns stone turn down`. **THE FLAG SWAP:** `set_flags {iss_caught: true, true_coord_known: true}`; embedded
`unlock` → `private_message`, `key: 'iss.dialogue.turns_cold'`, priority 15. The private cold-flip line
(`iss.dialogue.turns_cold`): *the one who told you the way was a wall is cold in the record now. every warm
word he set out reads the other way. he was the warmest of the six. that was the trap, and the trap is
sprung, and the warmth does not come back.* The `private_message` key-resolution lives ONCE in `resolve.ts`.
`iss_caught` is THE universal hinge (six+ threads key on it). **[GAP — TO BUILD]:** the `resolve.ts`
`private_message` key-resolver + the `requires_flags jsonb` column + `getOpenPuzzles` filter.
**[GAP — TO BUILD, F1 MONORAIL]:** the shipped seed STILL carries `next_puzzle_key: rite-tokens` on
`no-wall-catch` (`puzzles_seed.sql` L294); WEB-MASTER §2.1 BANS it — repoint/remove so the rite is reached
only through the chain (→ §V.0).

### IV.C.4 — The warm→cold re-staging (`liar.ts`, the colorant — NOT the activation lane)
The demoted optional curated re-staging of Iss's already-posted warm beats as cold once `iss_caught`. Flag-
gated, one-way (warm→cold), idempotent on a per-beat high-water, curatorial-by-default (`'pending'` for
dashboard approval unless AUTO mode). Two additional cold bodies (BUILT): `iss.dialogue.turns_cold.wall`:
*the wall he promised was a door he was opening, a course at a time, over winters. inside-the-wall was never
safe. it was the far side being let in.*; `iss.dialogue.turns_cold.easy`: *the ease he offered was the
not-counting. a thing told it is kept, and never counted, is a thing being readied to be let go.* **DIRECTOR
ACTION:** the cold re-stage enqueues `pending` by default — the approval gate fires here. **[GAP — TO BUILD]:**
`liar.run.ts` (the DB/clock wrapper; `liar.ts` pure core is BUILT).

### IV.C.5 — What the flag swap LIGHTS (the back half opens at once — non-linear)
`iss_caught` (+ co-flags) is the deterministic `requires_flags` gate that flips staged rows live, in
causal-but-not-countable order: `bound-word`, `base-docket-reread` (+ `-auto` twin), `meta-unkept`, the
Seventh deep (`the_unwriting`, once `iss_caught` + `seventh_suspected`), and the surface cold cascade
(`prophet-wall-name` cold, the Record Iss-card cold, Aro/Pell flip sticky-cold). The resolver ignores order.

### IV.C.6 — The bound word (`bound-word`)
`next_clue`, active=false until `iss_caught`, `max_attempts:6`. The Iss Vigenère plaintext IS the coop-gate's
need. Accepted: `the one who turned away` / `turned away` / `the bound word is his name`. Effect: `set_flags
{bound_word_known: true}`, `next_puzzle_key: m4-three-hands`. `oracleNextClue`: *kept. the way goes on — look
where the marks were not, before.* A SECOND in-road via `stone-orin` substitution stego normalizes to the
same word (no SPOF). **[GAP — GO-LIVE:** the stego-layer carving.]

### IV.C.7 — The Hold-Book down-count re-reads (`base-docket-reread`) {#payoff-06}
`lore`, active=false until `iss_caught`. The down-count (`lamps kept: [N]`, the doom-clock bait through
M1–M3) re-reads: never a doom-clock. Accepted: `the count was never of the dark it was of the hands` / `the
muster is read the hands are almost in` / `the down count is a muster of present hands` / `not a doom clock a
roll call`. `docketReread` fragment: *the muster is read. the count was never of the dark. it was of the
hands. the hands are almost in.* **TWIN:** `base-docket-reread-auto` ships active=true gated by `requires_flags
{iss_caught}` — the offline duplicate so the re-read fires even if the showrunner is asleep (no SPOF).

## IV.D — THE FALSE LAW + THE SEVENTH (the discoveries that recolor)

### IV.D.1 — The false-law discovery (`forged-eighth`) {#payoff-07b}
`dead_end`, kind `known`; substitution whose signature resolves to "to cover one's own" (a verb ABSENT from
the founders' ring — the seam of the forgery). `teaches_custom NULL` (fiction, INV-17). Card
(`cardEighthForged`): *the founders set the ways and did not finish the count. the eighth is the covering of
the hands. cover, and be counted clean.* (the forged line credits no "me" — the anonymous lie is stronger).
**THE M4 CORRECTION** (`archiveEighthCorrection`, fires at the catch): *the eighth was added by a later hand,
and is not in the founders' ring, and was never measured. obey it and nothing answers. that is how a forged
way is known.* No listener, no custom key (the proof is the reliable absence of a toll). **[GAP]:**
`keeper.falseLaw` + the dialogue branch read `eighth_seen`, but no shipped row sets `eighth_seen` — the
flag-set on reading the forged board is owed. The forged Covering (un-tracked) and the Unlit Deep (a real
tracked latch) never collide (different frames).

### IV.D.2 — The Seventh side-quest (`seventh-shrine`) {#payoff-02}
`side_quest`; Sella's Atbash bearing routes to the cold-hearth shrine; count six markers, then "a seventh"
(FACT 10 — the land can refuse). Earns Whisper budget. GATES NOTHING. Accepted: `there was a seventh` / `the
last marker is not the last` / `seven` / `7` / `the land kept six and refused the seventh` / `a thing that
can say no is not a wall`. Effect: `set_flags {seventh_found: true, whisper_budget_earned: true}`.
`oracleSideQuest`: *this is not the way. but it is a way. follow it, if you would.* The M1 "seventh mark the
record will not keep" (#02) surfaces here as the cast-out Seventh. **[GAP — GO-LIVE:** `the_far_water`,
`the_cold_hearth` UNPLACED.]

### IV.D.3 — The Seventh deep (chambers 2–3, staged M3→M4)
The hearth-DEEP (`the_unwriting`, beneath `the_cold_hearth`). Three staged rows, all active=false until the
deep opens (post `iss_caught` + `seventh_suspected`). Chamber 1 legible in M3 (*"below the cold hearth, the
deep is sealed; the seal is a name"*); chambers 2–3 resolve M4.
- `seventh-unwriting` (chamber 2, RAIL-FENCE rails=6, **reusing Brann's taught literacy**) — `main_beat`,
  `set_flags {seventh_named: true}`, `next_puzzle_key: seventh-choice`. The cipher logic: erasure IS
  transposition — the scraped name was displaced into the kerning of the six kept names; rail-fence reads it
  back. Chamber-2 lore on solve: *six names are cut whole here. the seventh was cut into the space between
  them. you have read it now. the record did not keep it; you did.*
- `seventh-cause` (`lore`, FACT 10b; earns Whisper; gates nothing): *the seventh kept every way and was not
  kept. the fire they let out was never theirs to lose. the land can refuse. whether that is mercy the record
  does not say.* **[GAP — TO BUILD]:** the cause-document `D-new the-fire-they-let-out.md`.
- `seventh-choice` (restore/erase, `main_beat`, in-world-detected only — two opaque tokens; → §V.6). GATES
  NOTHING.
- **[SEAM — lagging surface]:** `the-seventh-spine §1.3` must read "gated on Brann's rail-fence literacy," not
  "the right cipher here." **[GAP — doc fix.]** **[GAP — GO-LIVE:** `the_unwriting` UNPLACED.] **[GAP — TO
  BUILD:** the `SeventhChoiceListener` rite + the two-token sentinel branch.]

### IV.D.4 — The surface cascade (Iss-adjacent NPCs flip cold — sticky)
At `iss_caught`, only Aro + Old Pell turn cold **sticky** (`iss_cold`); conduct-cold is recoverable.
`aro.greet.iss_cold`: *You found what's past the line, then… I never been down there, I just say what sells…
Don't tell me about it. I don't want it in my head with the rest of the things I say.* `old-pell.greet.iss_cold`:
*So you found the dead shrine. West and down, the cold hearth. I knew a man went looking for a road up at the
bottom of a hole, and I knew what came back wearing him. … I won't ask if you came back as you. I'm watching
to see.* **[GAP — GO-LIVE:** the NPC bodies (Citizens2/ZNPCsPlus).]

### IV.D.5 — The Unlit Deep (the one group latch — `collective-restraint-custom`)
The eighth *tracked* thing — a single group-scoped negative latch, arms M3, active-only. Below the Line AND
on the black moon AND a flame is lit → the latch breaks for everyone; the borrowed glow of the never-doused
fire withdraws (reversible). Break line (`tollUnlitDeep`): *a flame was lit below the line, on the black moon,
where the deep keeps its one fire and asks for no other. the borrowed glow is drawn back. it is drawn back for
all, not for the hand that lit it.* Kept line (`keptUnlitDeep`): *no flame was carried below the line on the
black moon. the one fire that was never put out lends its glow. it is lent to all of you, while it is kept.*
Detect on EXPLICIT flame acts only (no ambient light sampling — precision). **[GAP — TO BUILD]:**
`UnlitDeepListener.java` + `group_restraint_state` table + `canon.ts` key add. Payoff at close → §IV.1
`keeper.collectiveRestraint`.

## IV.E — THE NETHER LANE (the M3 face) — see §III-NETHER
The bearing page `the-fire-is-lent` (read post-descent) and the `nether_forge` pocket (on-site word `lent`,
Kept-Light origin = keeping is a carrying) are detailed in §III-NETHER. Blocked on the LORE FACT-11 seal.
**Dob** is the human counterweight who quiets the deeper they go: *There's the line. The painted one. We're —
we're not crossing that, are we… Aro said cross it but Aro's a liar…* → much later, very quiet: *…I don't want
to go further. I'll wait here. By the lamp… I'll be right here. I'll be right here. I'll be right here.* (Dob's
"Aro's a liar" pre-echoes the Iss catch; "I'll keep this lit" rhymes with Kept-Light + Unlit Deep.) **[GAP —
GO-LIVE:** Dob's NPC body.]

## IV.0 — THE GATE INTO MOVEMENT IV (the universal hinge)
M-IV is keyed on the single canonical flag `iss_caught`, set by the player-driven catch (§IV.C.3), NOT the
showrunner. The activation cascade (`metapuzzle_seed.sql`, the deterministic `requires_flags` gate;
`getOpenPuzzles` opens a row iff every flag truthy):
```
iss_caught                     → bound-word, base-docket-reread(+auto), meta-unkept
bound_word_known               → m4-three-hands
threshold_open                 → threshold-coordinate
true_coord_known               → true-walk-arrive
iss_caught ∧ seventh_suspected → seventh-unwriting, seventh-cause
seventh_named                  → seventh-choice, end-seventh-out
```
The director does nothing to "advance" the catch; the console role is the **approval gate** + any showrunner
`active`-flips, both backstopped by `requires_flags` so a sleeping showrunner never hard-locks the back half.

## IV.1 — THE KEEPER-NPC FRAMEWORK (the dialogue tree, branching on dossier state)
The presiding Keeper (`keeper`, register 3 / SET C) presides at `the_threshold` + `keeper_altar` only (never
the Mouth where Set A lives). Branch logic = `keeper.ts` (pure resolver); text = `voice.ts` / SET C verbatim
(no English in the Java beat). **Fixed precedence** (`resolveKeeperDialogue`): 1. prior-keeper apparition not
claimed by the conductor → NO node (defer to the single-arbiter slot, INV-18; the PRESIDING Keeper is never
slot-gated); 2. M-IV atonement (broken, not atoned) → `keeper.atone.withheld`; 3. atonement honored →
`keeper.atone.cleared`; 4. FACT 9 (a logged haunt, namable, one surface/window) → `keeper.fact9.named`;
5. dossier-rhymed → `keeper.rhyme.<id>` / if `rhymesWith==='iss' && issCaught` → `keeper.iss.cold`; 6. neutral
floor → `keeper.presiding.neutral`. `iss_caught` is NOT a Keeper-skin input; he has no `truth_or_lie` tell
(the one voice that never lies).
- Verbatim greets: `keeper.greet.neutral`: *you came down. they came down too, the ones before you. i was
  nearer the front of that line than i tell. sit, or stand. the record keeps either. i keep the rite.*
  `keeper.greet.warm`: *you kept the ways coming down… i will not say who we is. you will know it, or you will
  not, and the not-knowing keeps you a while longer.* `keeper.greet.cold`: *you broke a way or two coming
  down. i am not here to scold it. i broke one myself, late, and was kept anyway… there is keeping left in it
  for you.*
- **[GAP — TO BUILD]** `keeper.iss.cold` body (only the Set-A `aro.greet.iss_cold` exists — wrong register).
- **[GAP — TO BUILD]** the per-keeper `keeper.rhyme.*` bodies.
- **[GAP — TO BUILD]** `keeper.presiding.neutral` body (the nearest authored floor is `keeper.greet.deniable`
  in the idea file — *you came to the stone. i am here, the way i am always here. ask the rite when you have a
  thing to ask it…* — keyed wrong; rename/author owed).

## IV.2 — THE M-IV ATONEMENT GATE (withhold a fragment until a broken custom is honored)
- **Puzzle half `atonement-refrain`** (`main_beat`): *the keepers turn.* Accepted: `the keepers turn` /
  `conduct is the lock the fragment is the key` / `honor the broken custom and return`. Effect: `set_flags
  {atonement_made: true}`, `next_puzzle_key: rite-tokens`, `unlock` → `reveal` `fragment:
  keeper_withheld_returned`. The player must have honored the previously-broken custom
  (`punishment_state.deciphered=true`) then return to the Keeper.
- **Dialogue half** `keeper.atone.withheld` (refusal): *the one whose mark you wear coming down kept the
  offering. you did not, the once it was asked. she will hand you nothing until the deep has had its first ore
  back from you. go up. give it. come down. the hand opens to a hand that gave.* → `keeper.atone.cleared`
  (release). **[GAP — TO BUILD]:** the de-slopped `keeper.atone.cleared` body — strike the named feeling *"she
  felt the weight leave the seam"*; let the cleared act stand.
- **FACT 9 at the same window** `keeper.fact9.named` — binds the logged M-I haunt to the keeper whose fate
  re-enacted it (one surface/window). **[GAP — TO BUILD]:** the de-slopped body — cut the *"you did not, then.
  you are learning it now"* bow. The standalone Watcher twin `haunting-biography` IS shipped: *the first
  hauntings were not random. they were one keepers fate, re-enacted at your door. the dread had a biography.*
  {#payoff-16}
- **The Hold-Book M-IV face** (the keeper's OWN hand writes the living player) — `keeper-record.ts`
  `decideKeeperEnrolment`, precision-floored (`minLeadScore 0.45`, `minLeadMargin 0.15`; a flat dossier stays
  an un-headed living row, INV-16). The keeper-hand bodies (deterministic fallback behind the optional LLM
  scalpel), per fingerprint, e.g. `keeperPageHand_orin`: *i, orin, set the one called {name} at the threshold
  and meant to cut the rest and the rest is not —*; `keeperPageHand_iss`: *i write the one called {name} and i
  tell them they are kept, and i do not count them, and the not-counting is the lie i was caught in.*

## IV.3 — THE CASCADE THAT RE-READS COLD AT THE CATCH (M-IV density), in causal order
- **3.1 `bound-word`** — see §IV.C.6.
- **3.2 `m4-three-hands`** (`main_beat`) — the cross-surface coop gate. THREE distinct ACTS in one ~20s window
  (active-only): foot on the plate + a carve + a Discord post. Accepted = a single OPAQUE conjunction token the
  plugin posts on a CLEARED gate. Effect: `set_flags {threshold_open: true}`, `next_puzzle_key:
  threshold-coordinate`, `unlock` → `door_open` at `coop_plate`. `oracleThreeHands`: *the count is three. the
  threshold is open.* In-roads: best 3 people; 2 can clear it; even 1 with a 2nd device, slowly. {#payoff-15}
- **3.3 `threshold-coordinate`** (`next_clue`) — the true coordinate (a NAVIGATION POINTER, INV-14; the typed
  answer is the clean on-site destination word, never the signed coordinate). Accepted: `follow the threshold
  mark to where it points` / `the true coordinate is a road not an answer` / `walk where the threshold sends
  you`. → `true-walk-arrive`, `true_coord_known`.
- **3.4 `true-walk-arrive`** (`main_beat`) — the destination WORD carved on leaves at the on-site tableau is
  the answer (gated to on-site presence). Accepted: **`kept here before you`** / `the road kept its word` /
  `we were already filed here`. Effect: `set_flags {true_destination_reached: true}`, `next_puzzle_key:
  rite-tokens`, `reveal` at `the_threshold`. The `kept here before you` answer is the M-IV payoff of #09 — the
  carves were never prediction; the group is already filed. {#payoff-09}
- **3.5 `base-docket-reread` (+ `-auto` twin)** — see §IV.C.7. {#payoff-06}
- **3.6 `meta-unkept`** (`lore`, gates nothing) — the six maker's-marks read in **fall-order** carry UNKEPT.
  Accepted: `unkept`. `oracleMetaUnkept`: *six marks, one to a stone. read them in the order they fell. the
  word is the one each did not keep.* active=false → opened by `requires_flags {iss_caught}` (the cold
  Iss/Keeper states the fall-order key; the glyphs fail in ring-order, self-correcting). {#payoff-08}{#payoff-03}
- **3.7 The forged eighth collapses** — see §IV.D.1. {#payoff-07b}
- **3.8 `prophet-wall-name`** (`dead_end`, kind `prophet`) — the columnar acrostic spells Iss. Accepted: `the
  one who turned away` / `iss carved the wall` / `read the first marks down the one who turned away`. Keeper
  half (`keeper.deadEndTaunt('prophet')`): *you read that true, and it is true, and it keeps no door. the one
  called iss would have told you it kept a door, and told you warm, and you would have walked it to a cold
  hearth. i tell you plain: true, and shut… read who carved it, after.* — the *"after"* was literal. {#payoff-19}
- **3.9 `name-where`** re-reads under FACT 9 (`dead_end`, kind `place`). Keeper half (`keeper.nameWhere`):
  *your name is cut where you have not been… it files the ground first and the foot after. before you was
  never strangers. it was you, before you came.*

## IV.4 — THE SEVENTH SPINE CHOICE — restore vs erase (staged; rite resolves §V.6)
- `seventh-unwriting` — name the Seventh (rail-fence rails=6, reusing Brann's literacy). `set_flags
  {seventh_named: true}`; `next_puzzle_key: seventh-choice`. (Detail §IV.D.3.)
- `seventh-cause` — why erased, FACT 10b (gates nothing; earns Whisper). (Detail §IV.D.3.) **[GAP:** `D-new
  the-fire-they-let-out.md`.]
- The Keeper lays the choice down `keeper.seventhChoice.offer` (non-prompting; no "press 1"): *below the cold
  hearth the deep is open now. the seal there was a name, and the name was scraped out, and the one it named
  kept every way and was cast out for nothing done. a name said back is a seal undone. you may write it again,
  or leave the blank. the land made its choice. you make the record's. neither opens the road. both are kept.*
- `seventh-choice` — the restore/erase rite, DETECTED IN-WORLD ONLY (two opaque tokens). RESTORE = set a
  marker block in the bare socket + light the cold pit + all present bow at `the_cold_hearth` deep (the deposit
  is ALSO the INHERITORS codicil — ONE act, ONE flag-origin). ERASE = break the six name-carvings at the
  unwriting wall (the one whitelisted break-site) + leave no carried light. Irreversible in fiction + flag
  (chamber-3 listener self-disables); not a loss condition, not a gate. **[GAP — TO BUILD]:**
  `SeventhChoiceListener.java` (the single largest gap — without it the tokens can never be posted) + the
  unwriting `.schem` + real `the_unwriting` coords. (The tint resolves §V.6.)
- The retreating anti-creature (chamber-3 entry) — a single un-targeted retreating glimpse, once, the one
  apparition NOT grounded in a measured signal (the absence of profiling IS the characterization — the Seventh
  is the one the land did not measure). **[GAP — TO BUILD]:** the `NamedMobBeat` glimpse (P2).
- The single sanctioned Iss/Seventh ambiguity line (post-`iss_caught`): *two hands scraped this stone. the
  record does not say they were two.* **[GAP — TO BUILD:** P2 row.]

## IV.5 — BECOMING THE KEEPERS (the reveal — the record writing them in)
The felt door to FACT 15, **never stated**. `keeper.becomingKeepers.neutral`: *the altar wants a thing only
each of you can give… the thing the record kept open a column for, against your name, before you came. bring
it at the dark hour. the rite does not reward. it receives. it keeps. the ones before brought theirs, and were
received, and are kept — you have read where they are kept; you are reading it now, in the same book, in the
same hand. we would keep you, if you would keep the ways.* (warm: *…we would keep you. you have made the
keeping easy.*; cold: *you broke a way or two coming to this. the altar takes a broken hand too — it took
mine, late… we would keep you, if you would keep the rest of the road.*) **THE HARD MIMIC CHECK:** every
variant STOPS at the half-veil ("we would keep you…" / "in the same hand"); a draft that finishes the
induction ("and so you become the watching") STATES FACT 15 and is a defect.

## IV.6 — THE COUNTING-JOURNAL PAYOFF (the Hold-Book, M4→M5)
The down-count + keeper-record are ONE book on ONE anchor (`stone_of_reckoning`'s companion lectern). M4
payoff = §IV.3.5 (`docketReread`). M5 close (forward-ref): `docketEven`: *the present hands are entered. the
book is even. the same book, the same hand, as all the ways above you.*; `keeperEnrolled` (neutral colorant
ack): *the one called {name} is entered under {keeper}. the heading is not a sentence. it is where the record
set them.*

## IV.7 — THE END LANE (optional deepening off M4→V; gates nothing)
> "The one place outside the record — no kept fire, no markers, no Archivist, no count." Plant D11
> **{#plant-29}** (*to be kept and to be cast out are one door, looked at from either side*). World
> `observance_end`. ZERO ambient apparition lane (a positive canon choice).
- **7.1 The pointer (a REVEAL, NO new node):** the `the_unwriting` chamber-2 wall gains one extra effaced
  line, legible only at `seventh_named`, pointing *out, past the door that is not a threshold… you will not be
  kept there; the record is not there to keep you.* A `RevealBeat` flip on the existing wall. **[GAP — TO
  BUILD]:** the authored reveal line.
- **7.2 The payoff `end-seventh-out`** (`lore`, gates nothing) — the Seventh's own account from outside the
  record; the on-site READ is the answer (INV-14) at `end_seventh_shrine`. Accepted: `i kept all the ways and
  it did not matter` / `the keeping was never the price` / `i went out past the door that is not a threshold`
  / `you only came to look`. `set_flags {seventh_seen_out: true}` (group-scoped, NEVER a fate input).
  `voice_key: end.shrineArrive`, fragment: *the seventh kept every way and was not kept, and went out past the
  door that is not a threshold, to the one place the record does not reach, and cut the name themselves. exile
  is the other side of keeping. you are not cast out. you only came to look.* Renders
  `the-name-i-cut-myself.md`. **[GAP — TO BUILD]:** `end.shrineArrive` voice key (silent-at-runtime; the
  fragment IS the body) + the `cardEndSeventhOut` archive body + `observance_end` world (GO-LIVE).
- **7.3 The breadth + rumor card:** `dest-out-of-record`; `who-seventh-out` (flips verified on arrival).
- **7.4 The set-pieces:** `end_seventh_shrine` (built to the Seventh's unfinished wrong-scaled hand;
  reveal-safe re-dressed end-ship OR world-build island, never a paste toward a glider). `end_exile_hold`
  (the `cast_out` fate made a place — a re-dressed end-city, markers facing away, vast/static) —
  **`enabled: false`**, GATED until the INV-16 binding is BUILT (the hold must name NO living player, encode
  NO per-player side, chorus-only dressing); else the End ships as the Seventh shrine ALONE. **[GAP — TO
  BUILD]:** the INV-16-safe exile-hold dressing (P2/cuttable).

---

# SEGMENT V — MOVEMENT V (THE ACCEPTING) + THE FIVE DIVERGENT ENDINGS

> Source: `playthrough/6-finale.md`. The collective rite, the world flip, the five closes. Reached ONLY
> through the single Iss chain (the catch does NOT hand it). Player-PERFORMED, not advanced from the console.

## V.0 — THE STATE THIS SEGMENT OPENS ON
The chain (causal order): catch (`iss_caught`) → `bound-word` → `m4-three-hands` (`threshold_open`) →
`threshold-coordinate` (`true_coord_known`) → `true-walk-arrive` (`true_destination_reached`) → `rite-tokens`
(`tokens_laid`) → `accepting-crouch` (`bowed_as_one`) → `record-receives` (`record_received`, `world_kept`).
Two doors to the bow: (A) the true walk → `rite-tokens` → `accepting-crouch`; (B) `pressure-glyph-walk`
(PROMOTED, a genuine second in-road that GATES NOTHING — "the do-not-read-your-way-out door").
**INHERITED GAP:** `no-wall-catch` still carries `next_puzzle_key: rite-tokens` (`puzzles_seed.sql` L294);
WEB-MASTER §2.1 BANS it — **[GAP — TO BUILD]** repoint/remove (the F1 monorail). **DIRECTOR posture:** approval
gate on Oracle beats; bind the single Accepting instant in `arc_state`; if filming, arm the `event-window` /
`/observance window on`.

### V.0.4 — The single Accepting instant (showrunner-owned)
ONE instant shared by THREE surfaces — the future-dated grave's carved date (FACT 13b), the Record website
timestamp, the summons `not_before`. No config owns it; `grave.ts` injects it (`acceptingInstantMs`). **[GAP —
TO BUILD]:** the `arc_state` binding writer + `grave.run.ts` / `fate.run.ts` I/O wrappers. DIRECTOR ACTION:
bind the instant before the rite window.

## V.1 — THE FINALE ASSEMBLY (gather the components, wake the Keeper)
- **1.1 `true-walk-arrive`** — see §IV.3.4. Watcher (`oracleMainBeat`): *what was shut is shut no longer. the
  record keeps the hand that opened it.* `requires_flags {true_coord_known}`. DIRECTOR: approve the arrival.
- **1.2 `rite-tokens`** (`main_beat`) — *bring the thing only you can give.* Lay one PERSONAL token in each of
  six slots + the named components (FACT 13 — the missing tool is YOU). Accepted: `bring the thing only you
  can give` / `deeps first heart unbroken light salt of the keepers` / `a piece you cannot read your way out
  of`. Effect: `set_flags {tokens_laid: true}`, `next_puzzle_key: accepting-crouch`, `unlock` → `reveal` at
  `unbroken_light` (6 slots lit). The Keeper's letter `bring-the-thing-only-you-can-give.md`: *bring, then, the
  thing only you can give. one token for each who kept the ways before you — six, a hand and one more. not
  coin, not ore… a thing that is yours… lay one in each marked place.* / *the record does not close when the
  rite is done… It receives… they did not depart, you have read that, they were kept…* / *lay the tokens. wake
  me fully. and when all of you bow as one, at the hour, in this light — we would keep you, if you would keep
  the ways.* / [lower margin] *do not grieve the giving. you are not losing it. you are leaving it where it
  will be found.* DIRECTOR: approve the 6-slot reveal.
- **1.3 Wake the Keeper — `becomingKeepers`** — see §IV.5 for the verbatim text + the hard mimic check.
  `KeeperNpcBeat` opens on right-click at `keeper_altar` / `the_threshold`; conduct-skinned warm/neutral/cold.
- **1.4 The summons** `summons()`: *the way is open. come — all of you — at the dark hour. bring what is
  owed.* Honors the `event-window` `not_before` == the single Accepting instant. DIRECTOR: ensure the
  `not_before` shares the bound instant; force the window if filming.

## V.2 — THE FINALE SITE `unbroken_light` (the Undercroft / Accepting floor)
`type: accepting_floor`, world `world`, `radius: 10`, `protect: true`, coords `null` **[GAP — GO-LIVE].** The
one fire never went out, centered; a room sized for 6–8 players to gather and bow; the lectern-comparator
door; the pressure-glyph rune on the floor (`pressure-glyph-walk`). Fog: a datapack dimension (`ambient_light:
0`). Carries the six lit token-slots, the never-doused fire (FACT 11), and the persistent FATE DRESSING
applied at the close (§V.5) — **[GAP — TO BUILD]** builder dressing (sites.yml has no fate-dressing field).
`keeper_altar` sits co-located but distinct (`radius: 5`) so the Keeper isn't summonable from the whole floor.

## V.3 — THE GROUP-BOW → OPAQUE SENTINEL → TERMINAL BEAT
- **3.1 The detector `AcceptingRiteListener.java`** (UNCHANGED, the spine). On `PlayerToggleSneakEvent`
  (the bow BEGINS) inside an `accepting_floor` radius with EVERY present player sneaking at once AND the
  cross-surface readiness gate open, it posts the OPAQUE wordless token (fires ONCE per cooldown per site).
  Trigger → effect: synchronized group crouch → `oracle.resolveWorld(...,"accepting-crouch")` → the climax
  beat. ACTIVE-ONLY QUORUM (INV-19): `effectiveQuorum = min(configQuorum, activeRosterSize)`, floored at 1
  (lowers the bar for a smaller active group, never raises it above the cast, never blocks an absent member).
  ACTIVE-ROSTER SOURCE: the wired `readActiveRoster(windowMs)` OR (unwired/failed)
  `Bukkit.getOnlinePlayers().size()`. READY GATE (fail-CLOSED): the Threshold must already be open
  (`threshold_open`); a null/unknown answer reads NOT ready. Fate-NEUTRAL: posts ONE token; WHICH close
  composes is decided downstream. **[GAP — TO BUILD]:** the `readActiveRoster` + `readyGate` supplier wiring
  (unwired today → fail-safe defaults).
- **3.2 The opaque token + config** (`config.yml` rites.accepting): `token: "k7q2m9 x4r8p3 w1n6z5 t0j4h2
  b8f1v7 c3d6s9"`, `quorum: 6`, `cooldown-seconds: 300`. `riteTokenSelfTest` enforces the token byte-matches
  the seed row at build time (the climax can never silently fail).
- **3.3 `accepting-crouch`** (`main_beat`, the TERMINAL rite, DETECTED in-world ONLY — never typeable).
  Accepted = the single opaque token (no human-readable phrase opens the climax). Effect: `set_flags
  {bowed_as_one: true}`, `next_puzzle_key: record-receives`, `unlock` → `door_open` at `unbroken_light`.
- **3.4 The world flips to KEPT — `record-receives`** (`main_beat`, opaque sentinel `p2w8k4 m9x1r6 z5t3j7
  h2b4f8 v1c6d3 s9q7n0`; FACT 14 — the record RECEIVES; the door to FACT 15, felt never stated). Effect:
  `set_flags {record_received: true, world_kept: true}`, `unlock` → `advancement_toast`
  `observance:the_record_receives_you` (the hidden toast *⟡ the record receives you.*). **[GAP — TO BUILD]:**
  the advancement datapack JSON. DIRECTOR: approve the toast (priority 30).

## V.4 — THE PAYOFFS THAT LAND AT V ("oh, that is what that was for")
- **4.1 The future-dated grave opens from the inside (FACT 13b)** {#payoff-21}{#payoff-10}. Plant (M2):
  `graveCarved(name)`: *the stone for the one called {name} is cut. it carries a date that has not come. the
  stone is cut before the keeper is kept.* Payoff (V): on the date == the single Accepting instant, the grave
  OPENS FROM THE INSIDE (`grave.ts decideGrave` emits `open` when `carved && !opened && nowMs >=
  acceptingInstantMs`). `graveOpened(name)`: *the stone for the one called {name} is opened from the inside.
  the date was not a death. it was an appointment. the hole is the deposit slot.* Private receipt
  (`graveReceipt`): *the one called {name}. read first. cut first. the rest are not yet cut. they will be.*
  Site `the_threshold` (optional `grave_spur` off-line). Grounds a real ACTIVE player only (nameless/inactive
  → NO grave). **[GAP — TO BUILD]:** `grave.run.ts` (the I/O; `grave.ts` policy IS built).
- **4.2 The Hold-Book's last page rewrites (FACT 14)**. `docketEven()`: *the present hands are entered. the
  book is even. the same book, the same hand, as all the ways above you.* (the group is in the SAME book as
  everyone above — FACT 15 felt). Preceded by the M4 `base-docket-reread` (§IV.3.5).
- **4.3 The cursed-map struck-seventh row fills (felt, never stated)** {#payoff-24}{#payoff-26}. Plant
  (M0-remote): the static `kept: 6` + struck row + dead uploader `m.kept`. Payoff (V): `/record` adds the
  group's own names via `recordReceives()`: *the record receives the present hands. they are entered in the
  other place too, against the loss of this one.* Verbatim payoff text (`six-were-kept-before-you.md`): *six
  were kept before you. / the count of them is kept. / you are the seventh. / a file is kept until it is
  opened. a hand is kept after.* + *…the struck row is not a row that failed to fill. it is the row that fills
  last, with present hands, when the keeping is done.* **[GAP — TO BUILD]:** the live `/record/the-record-keeps`
  page + the `/record` V-injection.
- **4.4 The herd's full pale field + the name-carves persist (FACT 15 visual)** {#payoff-14}{#payoff-09}. The
  cosmetic Pale field stands at full spread (all facing one way; only the GLOWING Beast was ever tracked,
  INV-13). The `name-where-never-been` carves persist as next-group markers (FACT 16; ACTIVE-only subjects
  rotate, a chorus, never the divergence extremes).

## V.5 — THE FIVE DIVERGENT ENDINGS (the exact selectors + literal outcomes)
> `decideFate` (`fate.ts`) returns ONE base fate enum; the M5 composer opens with it + at most one tinted
> clause + at most one codicil. It names NO player, reads the GROUP enum ONLY (INV-11, INV-16). The
> persistent floor DRESSING is the camera-legible delta; the sentence only confirms what the floor showed.
- **5.0 The selector `decideFate`** (PURE + DETERMINISTIC). Inputs (ACTIVE-only; no bond/Whisper field):
  `honoredActive`, `violatedActive`, `leftAtActive`, `seventhFound`, `issCaught`, `quorumMet`,
  `refusalSignal`. "Dominates" = a STRICT majority of the active honored/violated split (`x*2 > decided`).
  Fixed precedence: **1. REFUSERS → 2. KEPT → 3. CAST_OUT → 4. DIVIDED (the floor).** **[GAP — TO BUILD]:** the
  `resolve.ts` fate-sentinel branch + `fate.run.ts` wrapper; AND **the M5 COMPOSER itself**
  (`composer.ts`/`m5*.ts` — the bounded ≤2-clause + codicil assembler — does not exist; all the clause TEXT
  exists in `voice.ts`, the ASSEMBLER does not — the single largest finale gap).
- **5.1 KEPT** — `honoredDominates && (seventhFound || issCaught) && quorumMet`. World: markers face OUT, floor
  light whole. `fateKept`: *the hands are in, and they are kept. the markers face out. the way is open the way
  it was open before you, and will be after.* Keeper `keeper.endings.kept`: *it is done, and it is kept… you
  are the road's edge for whoever comes down next… you are received. i will not say the rest of it. you will
  keep it, the way i keep it.*
- **5.2 CAST_OUT** — `violatedDominates && leftAtActive >= 2`. World: markers face AWAY (passive voice).
  `fateCastOut`: *the count is closed and it is short. the markers face away. what was owed was not returned,
  and the record enters it so.* Keeper `keeper.endings.castOut`: *it is done. the markers face the wall. some
  of you were left at the threshold, as some were left before. i do not name which… come down again, kept, and
  stand again.* End-lane: for a `cast_out` group with `seventh_seen_out`, `end_exile_hold` reads as their own
  exile-hold — **GATED OFF by default** (`enabled: false`) until the INV-16 binding is built; else the End
  ships as the Seventh shrine alone.
- **5.3 DIVIDED (the floor)** — the default (neither honored- nor violated-dominant; or dead-even/empty).
  World: light holds on HALF the floor **BY GEOMETRY, never by player** (INV-16). `fateDivided`: *the light
  holds on one side of the floor and not the other. the record does not say which hands stood where. it says
  only that the floor is divided.* Keeper `keeper.endings.divided`: *…it did not choose by who stood there. it
  chose by what was done. the floor only shows it. you may read the floor; you may not read it onto a face.*
- **5.4 REFUSERS (secret, checked FIRST)** — `quorumMet && refusalSignal`. `refusalSignal` is a POSITIVE,
  plugin-detected defiance act, NEVER `quorum && !bowed` — a slow/absent group is never read as refusing
  (PRECISION). `fateRefusers`: *the hands were all present, and the bow was not made. that too is entered. the
  record keeps the refusal as plainly as it keeps the keeping.* Keeper `keeper.endings.refusers`: *…it is a
  refusal, and the record keeps a refused thing differently than an empty one. i do not know what it keeps it
  as. i was kept. i did not refuse.* **[GAP — TO BUILD]:** the plugin-detected refusal rite that sets
  `refusalSignal` (no `RefusalRiteListener` — until built, REFUSERS is unreachable; the arc falls through to
  KEPT/CAST_OUT/DIVIDED, safe by precision).
- **5.5 INHERITORS (a CODICIL, not a base fate)** — a boolean that may append to ANY of the four. The SAME act
  as the Seventh `restore`/deposit — ONE flag (`seventh_choice = restore` → `ending_codicil`). The +1 clause
  `fateInheritorsCodicil`: *a mark is left for a hand not yet here. the deposit slot is cut and waiting, the
  way yours was cut and waiting before you came.* (FACT 14 within this arc). Composer cap: ≤2 clauses + the
  codicil.

## V.6 — THE SEVENTH CHOICE — restore / erase (the tint + the persistent block-state)
- **6.1 `seventh-choice`** (`main_beat`, DETECTED IN-WORLD ONLY — `SeventhChoiceListener` at `the_unwriting`).
  Two opaque tokens posted on real detection: `r7n4k2 m1x8p5 w3j6h9` (restore) / `e5t0b7 c2d4s8 v6f1z3`
  (erase). `unlock` → `reveal` `fragment: seventh_choice_marked`. The resolver's Seventh-choice sentinel branch
  sets `seventh_choice` + `ending_codicil` from which token matched. active=false, `requires_flags
  {seventh_named}`. GATES NOTHING. **[GAP — TO BUILD]:** the resolver sentinel branch (+ the listener, §IV.4).
- **6.2 The tint clauses + persistent block-state:** RESTORE `keeperCloseSeventhRestored`: *the name that was
  cut out is cut back in. the hearth below the cold hearth is lit again. one that broke nothing is kept,
  late.* (block-state: the re-warmed hearth lit). ERASE `keeperCloseSeventhErased`: *the name stays out. the
  wall below the cold hearth stays blank. the record keeps the blank where the name would go, and does not
  fill it.* (block-state: the blank wall stays unwritten). Keeper faces `keeper.seventhChoice.offer /
  .restored / .erased` (the restored face pairs with `fateInheritorsCodicil`: *…a mark is left there now for a
  hand not yet here, the way a mark was left for you. that is the older keeping.*).
- **6.3 The deepening-lane tints (color the seventh clause, never change the mechanic):** the End lane
  (`seventh_seen_out`) renders `the-name-i-cut-myself.md` (§IV.7.2); the Nether lane (`nether_forge_found`)
  sets up "the keeping was a carrying" tint. Both group-scoped, active-only, names-no-player, **NEVER
  fate-selector inputs** (confirmed: `FateInput` has no such field).

## V.7 — THE FORKS (permanence colorants at the close; INV-12, color never gate; via `forks.ts`)
Each fork sets a flag at its earlier movement; the M5 composer may add ONE as the heaviest-tint clause. Forks
NEVER gate.
| Fork | Flag | Set by | Clause (verbatim) |
|---|---|---|---|
| **A — Sacred Beast** | `sacred_beast_broken` | the kill of the GLOWING Beast (INV-13) | `forkSacredBeastBroken`: *the one that glowed is down. the boon it would have lent is closed, and stays closed. the herd keeps the death-spot in its facing.* |
| **B — First Light (boon)** | `light_kept` | `fork-light` M3 | `forkLightKept`: *the light came up the stair on its own. you carried it. that is how it is carried.* |
| **B — First Light (transgressor)** | `light_taken` | `fork-light` alt | `forkLightTaken`: *the flame is banked, and the room it warmed stays dark. the light that was lent is taken, and the deep is colder by it.* |
| **C — Spoken Name (boon)** | `name_unspoken` | `fork-name` M4 | `forkNameUnspoken`: *the name was not shaped. the word stays shut, the way the sixth way is left blank in the book.* |
| **C — Spoken Name (transgressor)** | `name_spoken` | `fork-name` carve act | `forkNameSpoken`: *the name was cut into the stone. the record keeps it, and keeps a faint line under it, the way it kept the one who turned away.* |
Fork C is P2/cuttable on blurt risk. The transgressor leaves carry a persistent world delta (Undercroft dark
/ a faint carve / the herd facing the death-spot). {#payoff-fork-B}{#payoff-13}

## V.8 — THE 6-DOWNLOADS PAYOFF (the day-zero number pays off) {#payoff-24}{#payoff-25}{#payoff-26}
Restated as the FINALE re-read: the lure page's `kept: 6` (#24) + dead uploader `m.kept` (#26) + the README
"it does not connect to anything" (#25) ALL re-read at V — the `6` was six prior keeper-GENERATIONS the record
already did this to; the group is the seventh it would not keep AS A FILE; the struck row fills LAST, with
present hands. `recordReceives()` is the V-line that fills it. The "it does not connect to anything" inversion:
the MAP connects to nothing; the SERVER does (the lie was true, misread as comfort). **[GAP — TO BUILD]:** the
live lure page + V-injection.

## V.9 — THE SEASON-2 SEED (the INHERITORS / "a hand not yet here")
NOT a separate mechanism — it is the `INHERITORS` codicil made persistent (the `restore` act leaves a deposit
slot "cut and waiting"; the name-carves persist; the world flips to `world_kept`). Delivered by WHAT PERSISTS,
never by a sentence that names "season 2" or finishes the recursion (the half-veil stops at "we would keep
you"). **[GAP — TO BUILD]:** no authored season-2 corpus on disk — only the persistent world-state hooks.

---

# GAP REGISTER

> Every `[GAP — TO BUILD]` (code/text owed) and `[GAP — GO-LIVE]` (in-game build / hosted asset / coord-fill
> owed) collected once, de-duplicated, with owner. "Owner" uses the segment-doc lane codes
> (TS-VOICE / TS-FORGE / TS-SHOWRUN; A#/B#/D# = `INTEGRATION-V2` build tasks; LORE; OPERATOR; BUILDER =
> in-game construction). Where a doc named no owner, it is inferred and marked (inf.).

| # | Item | Segment(s) | What is missing | Owner |
|---|---|---|---|---|
| 1 | `discord/supabase/migrations/0006_*.sql` | Setup B3 | The whole back-half schema: `requires_flags jsonb`, `world_paste_ledger`, `voice_watchlist`, ending/grave/restraint/reckoning columns, `nether_forge_found`/`seventh_seen_out`. No 0006 exists. | DB / B-lane |
| 2 | FACT-11 source-clause seal | Setup B7; III-Nether N.0; IV.E | One sentence sealed into `canon-spine.md` FACT 11 + `structures.md` ("carried up from below the bottom… one direction, not two"). BLOCKER on any Nether build/flip. | LORE |
| 3 | Resource-pack `.ogg` audio bodies | Setup C2 | The four sound files (`whisper`, `drone_low`, `stone_breath`, `cold_toll`). | OPERATOR/BUILDER |
| 4 | Resource-pack `prompt` line | Setup C3; G | `config.yml resource-pack.prompt` is `""` (plain keeper register, no caps/exclaim). | TS-VOICE (inf.) |
| 5 | Hosted pack `.zip` URL + SHA-1 | Setup C3 | `resource-pack.url`/`sha1` are `""`. | OPERATOR |
| 6 | Undercroft fog datapack | Setup D2; IV.A.4 | `dimension_type`/biome effects (thick fog, `ambient_light: 0`). | BUILDER / D5 |
| 7 | Three Multiverse worlds | Setup D3; IV.A.4; III-Nether; IV.7 | Undercroft void world + `observance_nether` + `observance_end`. Must exist before cross-dimension rows flip. | OPERATOR/BUILDER |
| 8 | Prologue vignette `the-hold.zip` + host + baked address + rune string | Setup E0; I.0/I.1 | World + `thehold` datapack; fill `tick.mcfunction` coords; bake server address; render rune string; host; point lure href. | BUILDER |
| 9 | Every in-world build + `.schem` export | Setup E1–E10; III/IV throughout | Two Rosettas, teaching-stone, six keeper-stones, custom/named anchors, back-half spine, carve/herd anchors, Nether/End sites. No `.schem` in repo. | BUILDER |
| 10 | Three Seventh chamber `.schem` | Setup E7; IV.4 | `seventh_hearth_01/02/03.schem`. | BUILDER |
| 11 | Room-A / Room-B `.schem` (the A→B swap) | Setup E8; IV.B.2 | Large set-pieces for `RoomSwapBeat`. | BUILDER / D10 |
| 12 | ALL `sites.yml` coords | Setup F; everywhere | Every site is `null`/UNPLACED (plugin silently skips). | BUILDER/OPERATOR |
| 13 | The forbidden Unspoken word value | Setup G1 | `tracker.forbidden-words` is `[]` (lives only on the locked server config). | OPERATOR |
| 14 | Rune-ring server icon | II.2.1 | The unremarked six-rune icon asset (literacy in-road a). | BUILDER |
| 15 | `voice.recordFrameBreak()` | I.3; II.1.4 | The count-callback voice key (not in `voice.ts`); must pass `registerDisciplineSelfTest`. | TS-VOICE |
| 16 | `prologue_ignited`-SET listener | I.2 | The `IgnitionListener` write of `prologue_ignited=true` on a `#the-record` post (read paths exist; no setter found). | TS-SHOWRUN (inf.) |
| 17 | `recordOpenedNamed`/`prologueNamed` key-mismatch resolver | I.4; II.1.3 | The map from the decision's `reportVoiceKey` to the `voice.*` call (enum ≠ voice surface). | TS-VOICE/TS-SHOWRUN |
| 18 | Showrunner `run.ts` drip/report loop | II.2.6 | The authoring loop + deterministic fallbacks (D1). | TS-SHOWRUN / D1 |
| 19 | Live Hold-Book `keeper_record.ts` | II.1.1; IV.2 | Ship static M1 plant first, then bind count to real muster state (A3). | TS-SHOWRUN / A3 |
| 20 | `reckoning.ts` engine | II.2.6; III.5 | The dynamic-difficulty pure module + selftest (mute M1, live M2). | TS-SHOWRUN / A10 |
| 21 | `SacredAnimalBeat` spread + `pale_cosmetic` PDC | II.2.6 | The herd spread mode + cosmetic-Pale tag (M2+). | plugin / A12 |
| 22 | Spawn-bias conductor + `apparitionClaim` | II.2.8; III-Nether N.3 | The single-arbiter apparition conductor (INV-18). | TS-SHOWRUN / D7 |
| 23 | Rune-ring doc↔seed reconciliation | II.2.2; III.2 | Carve `unspoken`/`sacred beast` (not ward/covering), OR correct the doc later-hand margin. | BUILDER / LORE |
| 24 | Whisper economy data-layer + `hintBody` strings | III.6 | The `whisper_budgets` ledger + toll application + tier escalation; per-puzzle hint bodies (no `hint_body` column). | TS-SHOWRUN / plugin |
| 25 | The four `nether.*` / `end.*` voice keys | III-Nether N.2/N.3; IV.7.2 | `nether.forgeArrive`, `nether.soulSand`, `end.shrineArrive`, `end.outsideRecord` (silent-at-runtime; fragments carry text). | TS-VOICE |
| 26 | `cardNetherForge` + `cardEndSeventhOut` archive bodies | III-Nether N.3; IV.7.2 | Thread-card bodies in `voice.archive.ts` (`threadCardVoiceCoverageSelfTest` fails until present). | TS-VOICE/archive |
| 27 | Live lockstep-unredaction Record page + Iss-card stego payload | III.7; V.4.3 | The live `/record` page that un-redacts in lockstep + the Iss-card Vigenère-key stego layer. | dashboard / TS-FORGE |
| 28 | `npcVoice.ts` registry | II.2.7; III.7; IV.D.4 | The proposed NPC voice registry (built spec; module not built). | TS-VOICE (inf.) |
| 29 | Staged `stone-brann-cipher` railFence activation | III.1.5; IV.A.3 | TS-FORGE `CLUE_SPECS` railFence entry + remove `stone-brann` from `NON_CIPHER_KEYS`; the night-beacon re-author (P0-5, the keener descent in-road). | TS-FORGE |
| 30 | `RoomSwapBeat extends SmallStructureBeat` | IV.B.2 | The clear-A-then-paste-B beat, `swapped` PDC marker, `require_floor:false`. | plugin / D5 |
| 31 | `resolve.ts` `private_message` key-resolver + `requires_flags` column/filter | IV.C.3 | The activation lane: key→subtitle resolver + `requires_flags jsonb` + `getOpenPuzzles` filter. | TS-SHOWRUN / D4 |
| 32 | `liar.run.ts` wrapper | IV.C.4 | The DB/clock wrapper reading `iss_caught` + posted warm-beats + high-water (pure `liar.ts` is built). | TS-SHOWRUN / D4 |
| 33 | `bound-word` second in-road stego layer | IV.C.6 | The `stone-orin` substitution stego carving that normalizes to the bound word (no SPOF). | BUILDER / TS-FORGE |
| 34 | `UnlitDeepListener.java` + `group_restraint_state` + `canon.ts` key | IV.D.5 | The BlockPlace/held-flame edge listener + the latch table + canon key add (A5). | plugin / A5 |
| 35 | `the-seventh-spine §1.3` lagging-surface doc fix | IV.D.3 | Read "gated on Brann's rail-fence literacy," not "the right cipher here" (canon already correct). | LORE |
| 36 | `SeventhChoiceListener.java` + two-token sentinel branch | IV.4; V.6.1 | The largest Seventh gap — without it the restore/erase tokens can never post (the whole choice is inert) + the resolver branch token→`seventh_choice`/`ending_codicil`. | plugin / TS-SHOWRUN |
| 37 | Seventh chamber-3 `NamedMobBeat` glimpse | IV.4 | The single un-targeted retreating apparition (P2). | plugin (inf.) |
| 38 | The Iss/Seventh ambiguity row | IV.4 | *"two hands scraped this stone…"* (P2, not yet seeded). | TS-SHOWRUN (inf.) |
| 39 | `D-new the-fire-they-let-out.md` | IV.D.3 | The cause-fragment document backing `seventh-cause`/`dest-fire-let-out`. | LORE |
| 40 | `keeper.iss.cold` body | IV.1 | The Keeper's cold-Iss node text (only Set-A `aro.greet.iss_cold` exists, wrong register). | TS-VOICE / corpus |
| 41 | `keeper.rhyme.*` bodies | IV.1 | The per-keeper dossier-rhymed node texts. | TS-VOICE / corpus |
| 42 | `keeper.presiding.neutral` body | IV.1 | The floor body (the nearest authored text is keyed `greet.deniable`; rename/author). | TS-VOICE / corpus |
| 43 | De-slop `keeper.atone.cleared` | IV.2 | Strike the named feeling *"she felt the weight leave the seam"* (final string not on disk). | TS-VOICE (slop E3) |
| 44 | De-slop `keeper.fact9.named` | IV.2 | Cut the *"you did not, then. you are learning it now"* bow (final string not on disk). | TS-VOICE (slop E4) |
| 45 | `eighth_seen` flag-set | IV.D.1 | No shipped row sets `eighth_seen` (gates `keeper.falseLaw` + the dialogue branch). | TS-SHOWRUN / seed |
| 46 | The End-lane pointer reveal line | IV.7.1 | The one extra effaced `the_unwriting` chamber-2 line at `seventh_named` (a `RevealBeat`). | TS-VOICE / TS-SHOWRUN |
| 47 | `end_exile_hold` INV-16-safe binding + set-piece | IV.7.4; V.5.2 | `enabled:false`; the chorus-only dressing (no per-player side); ships as Seventh-shrine-alone until built (P2/cuttable). | BUILDER / TS-SHOWRUN |
| 48 | `no-wall-catch` `next_puzzle_key: rite-tokens` repoint | IV.C.3; V.0 | Remove the L294 shortcut (the F1 monorail) so the rite is reached only through the chain. | seed / TS-SHOWRUN |
| 49 | Arc_state Accepting-instant binding writer + `grave.run.ts` + `fate.run.ts` | V.0.4; V.4.1; V.5.0 | The instant binding + the I/O wrappers (pure `grave.ts`/`fate.ts` policies are built). | TS-SHOWRUN |
| 50 | The M5 COMPOSER (`composer.ts`/`m5*.ts`) | V.5.0 | The bounded ≤2-clause + codicil assembler (all clause TEXT exists; the assembler does not). Single largest finale gap. | TS-SHOWRUN / synthesis |
| 51 | `resolve.ts` fate-sentinel branch | V.5.0 | Reads the honored/violated spread → writes `arc_state.ending_fate` (set-once). | TS-SHOWRUN |
| 52 | `RefusalRiteListener` (sets `refusalSignal`) | V.5.4 | Without it REFUSERS is unreachable (arc falls through, safe by precision). | plugin |
| 53 | `AcceptingRiteListener` `readyGate` + `activeRosterSize` wiring | V.3.1; V.10 | Wire `readActiveRoster` + the readyGate supplier (unwired → fail-safe defaults). | TS-SHOWRUN / plugin |
| 54 | Fate floor-dressing producer | V.2; V.5 | Marker facing / floor-light geometry on `unbroken_light` (no sites.yml field — builder dressing). | BUILDER / TS-SHOWRUN |
| 55 | `observance:the_record_receives_you` advancement JSON | V.3.4 | The FACT-14 hidden-toast datapack definition (not verified on disk). | BUILDER / plugin |
| 56 | Live `/record/the-record-keeps` page + `/record` V-injection (`recordReceives`) | I.0; V.4.3; V.8 | The live lure shell + the M5 struck-row fill (`record-projection.ts` referenced; page build owed). | dashboard |
| 57 | Nether `carrying`/Kept-Light-origin V-tint voice key | V.6.3 | The `nether_forge_found` close-clause key. | TS-VOICE |
| 58 | `FateInput.netherForgeFound` wiring into `decideFate` | III-Nether N.2 | PROPOSED; NOT wired (the M5 composer reads it for a tint meanwhile) — pending WEB-MASTER §8 ratification. | TS-SHOWRUN / synthesis |
| 59 | Prophet's-wall `sites.yml` placeholder | III.4 | The prophet's wall has no site entry of its own (lives near `stone_iss`). | BUILDER |
| 60 | `from_map` transmission flag (FB-2(i)) | I.3 | P2/optional — the map-conduct callback; not built (default-safe without it). | TS-SHOWRUN (P2) |
| 61 | Fork C (Spoken Name) | V.7 | P2/cuttable on blurt risk; the `fork-name` carve act + persistent delta. | plugin / TS-SHOWRUN (P2) |
| 62 | Authored season-2 (Inheritors) corpus | V.9 | None on disk — only the persistent world-state hooks exist (by design). | LORE (deferred) |

> **Operator-action (not a build):** rotate the committed Supabase `service_role` key (Setup A1) — a live key
> is committed in the clear. Tracked here as the highest-priority pre-launch action though it is not a
> "build" gap.
