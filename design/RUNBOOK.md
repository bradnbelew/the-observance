# THE OBSERVANCE — OPERATOR'S RUNBOOK (running it with your friends)

> The single guide for setting up and running a real session. Supersedes GO-LIVE-TONIGHT.md.
> Current go/no-go status is in `design/CURRENT-READINESS-VERDICT.md`; honest system detail is in §6.
> Server = Paper **1.21.11** on Crafty (brother's PC).

---

## 1. ONE-TIME SETUP

### 1a. Database (Supabase, project `fdnmhbpxnodrnbrzrlqq`)
- From `discord/`, run **`npm run db:seed`** to regenerate **`discord/supabase/apply-all.sql`**.
- SQL Editor → paste **`discord/supabase/apply-all.sql`** (the whole file) → Run. It is the only launch
  database path: dashboard base + lockdown, Discord/oracle schema through 0011, public Record/Archive
  views through dashboard 0009, all seeds, then **schema-repair.sql**. Idempotent — safe to re-run.
- Do **not** paste loose migrations after it and do **not** use `apply-tonight.sql`. If a migration matters
  for launch, it belongs in `discord/src/db/build-apply-all.ts`; `npm run audit` verifies the generated
  bundle order with `db:bundlecheck`.
- Grab your **`service_role`** key (Project Settings → API → `service_role`, the *secret* one).

- From the repo root, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\prepare_server_test.ps1`** when your next
  goal is easier server testing rather than a launch promise. It still generates the normal friend-launch
  placement and rehearsal packets, then adds `server-test-guide.md` with exact smoke-test, vertical-slice,
  full-rehearsal, and launch-go/no-go scripts. Use `-SkipBundle` only for a fast dry packet; rebuild before
  uploading fresh server bytes. After hosting the resource pack, rerun it with
  **`-ResourcePackUrl <hosted-https-zip-url>`** so hosted pack verification and the generated guide agree.
- From the repo root, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\prepare_friend_launch.ps1`**. This is the
  fastest friend-launch prep path: it builds the deploy bundle, creates the launch placement packet, creates
  the live rehearsal packet, and runs the current launch blocker report against those exact files.
  It also writes `friend-launch-quickstart.md`, `launch-blockers.md`, `manual-media-checklist.md`,
  `supabase-apply-card.md`, `live-server-command-sheet.md`, and `friend-launch-todo.md` into the rehearsal
  packet with the exact paths, Supabase SQL SHA1, deploy hashes, media-ready flags, live command receipts
  to collect, remaining live blockers, and final go/no-go command for that run.
  If the resource pack is already hosted, run the same helper with
  **`-ResourcePackUrl <hosted-https-zip-url>`** so the URL and current zip SHA1 are written during prep.
- From the repo root, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools/audit_all.ps1`**. This is the one-button
  prep check: Discord/data story audit, dashboard build, Java 21 plugin source/jar checks, plugin DB
  contract checks, Minecraft text-surface fit, Record/web artifact withholding, Record terminal hint
  escalation, rune-font cohesion, and asset checks. Do not start a live session if this is red.
- For the final live server, also run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_world_build_readiness.ps1 -Launch`**.
  This is the outside-Hold launch-required site coordinates gate: it fails until every bespoke, surface,
  Nether, End, and Unlit-adjacent anchor has real coordinates instead of authoring placeholders. Deep
  Hold-owned sites are generated together by `/observance placehold build` and are proven in
  `coords-capture.csv` with `GeneratedProof`, audit output, and room screenshots.
- For the final no-excuses go/no-go gate, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_launch_manual_blockers.ps1 -Launch -CaptureCsv <packet>\coords-capture.csv -RehearsalPacket <packet-dir>`**.
  This combines the repo-verifiable manual blockers: hosted resource-pack URL/SHA1, outside-Hold launch
  coordinates, Deep Hold generated proof, coordinate/proof quality, and the completed live rehearsal packet. It also prints the
  remaining manual attestations that only the live Paper server/client can prove.
- Use **`design/MANUAL-LAUNCH-PLAN.md`** as the ordered human checklist. It is the source for what each
  manual task is, why it matters, where it belongs, how to do it, when to do it, how players should find it,
  and what evidence proves it. The rehearsal packet's `launch-attestations.md` is where those live-only
  proofs are recorded.
- If you only need the Discord/data half, run **`npm run audit`** from `discord/`.
- If you only need the plugin source half, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools/check_plugin_compile.ps1`**.
- For final deploy packaging, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools/package_launch_bundle.ps1`**. It rebuilds the
  plugin jar, datapack zip, resource-pack zip, validates them, and refreshes the deploy manifest.
- If you changed only plugin Java/resources and need a partial package, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools/package_plugin.ps1`** before uploading
  `plugin/build/libs/observance-0.3.22.jar`.
- If you changed only `datapack/` or `resourcepack/` and need a partial package, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools/package_assets.ps1`** before the audit.
- Those package commands refresh **`observance-deploy-manifest.json`**. Use that manifest as the deploy
  receipt for the exact plugin jar, datapack zip, and resource-pack zip bytes you are uploading/hosting.
- After hosting the resource pack, run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools/set_resource_pack_config.ps1 -Url <hosted-https-zip-url>`**
  so the URL and current zip SHA1 are written together.
- Then run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools/check_hosted_resource_pack.ps1`**
  so the hosted `.zip` bytes are proved to match the local resource-pack SHA1 before players join.
- The side-quest audit enforces the launch rule: only two blunt false/dead leads total, and each must
  have teeth. Optional content should build confidence, not teach players the ARG wastes their walks.

### 1c. Server (Crafty)
- Create a **Paper 1.21.11** server; give it Java 21 + 3–4 GB RAM.
- **Plugin:** upload `plugin/build/libs/observance-0.3.22.jar` to `plugins/` (the reshape build — earned-
  literacy rune-cribs, de-announced structure labels, terrain-following + scattered placement, townsfolk).
- **Key:** in `plugins/Observance/config.yml`, set `supabase.service-key: "<service_role key>"`
  (leave `service-key-env` alone). The `url` is already filled.
- **Datapack:** upload the `datapack/observance/` folder into the world's `datapacks/` folder
  (`world/datapacks/observance/`). This carries the Undercroft fog dimension + the reward-toast
  advancements. Run `/datapack list` in console to confirm it's enabled (or `/reload`).
- Start the server; console should show **Observance** enabled with no errors.

### 1d. Verify the DB link
- In game (op yourself first: console `op <name>`), run `/observance status`.
  Want: **`supabase configured: true`**, **`last db call ok: true`**, **`queued writes: 0`**.
  If not, the key is wrong or the SQL didn't run — fix before continuing.

## 2. BUILD THE WORLD (production hold first, rehearsal board when needed)
- Preferred production bootstrap: stand at the intended **surface mouth** and run
  **`/observance placehold build [depth]`** (default depth **392**, accepted range **340-520**), or from console run
  **`/observance placehold build <world> <x> <y> <z>`** where `y` is the Hold floor. This carves the
  controlled stone envelope, a broad encased Return Mouth stair from the surface, the grand Keeper Court,
  upper terraces, archive/cistern gallery, market/collapse wing, lampworks descent, threshold cathedral, and side-hush
  branch. It places and registers the 64 overworld/deep ARG sites that reasonably belong together inside
  one civic ruin rather than a row of isolated boxes, seeds side-evidence containers, keeps decorative chiseled bookshelves
  occupied, leaves mechanic-owned shelves under their mechanics, installs full-width physical gates,
  and registers protected Hold/entry-stair regions. Run **`/observance placehold audit`** immediately; it must
  report 64/64 hold sites, 7/7 gates, records 8/8, entry walkable, clean early-route/grand-court/terrace samples,
  protected regions, and zero critical findings. Use
  **`/observance placehold seal|open <gate|all>`** for manual operation and **`/observance placehold sync`**
  once Supabase flags are live. This does not replace the surface prologue (`first_report_lectern_01` /
  `first_marker_01`), Nether/End lanes, Unlit, NPC spawning, or the one loose `answer_sign_01` puzzle
  fixture.
- Compact rehearsal board: stand at the **southwest corner / operator edge** of a fresh intended test board
  and run **`/observance prepworld`** (default spacing **36**; optional spacing is clamped to **34-48**).
  This is a readable platform board for smoke testing, not the production setup tool. Facing east, the
  parallel rows are Lamp-works proof, surface keeper spine, deep payoff spine, side-proof row A,
  side-proof row B, and Mara books. Each compact cell is cleared before stamping so terrain does not fill
  intentional air inside structures.
  If you already generated an older overlapping rehearsal board, abandon that patch of terrain and run
  `prepworld` in a fresh area; the command lays out a new board but does not erase/move stale old platforms.
  No structure uses a beacon beam as a player waypoint; players must be moved by the authored route:
  report/marker evidence, Rosetta and reckoning literacy, keeper books/signs/items, NPC claims, dashboard
  cards, Discord/media clues, and the placement briefs' approach/focal/action/exit proof.
- For final curated world sites outside the Hold, use **`/observance site todo`** in game as the outside-Hold launch-coordinate
  checklist. Start with **`/observance site plan lanes`** so placement is handled as scene passes:
  `prologue`, `keepers`, `customs`, `human`, `deep`, `dread`, and `dimensions`. **`/observance site next <lane>`**
  names the next required site in that lane and includes its placement brief; **`/observance site plan <siteId>`**
  gives the intent, placement rule, and proof shots for any launch anchor. Stand at the real anchor and run
  **`/observance site set <siteId>`** to survey the anchor; for any placeworld or dimension row, run
  **`/observance placeworld`** from that world before it can count as placed. Hold-owned rows should instead be proofed inside the generated Deep Hold after
  `/observance placehold build` and `/observance placehold audit`. The command prints the remaining launch count after each survey so you do
  not have to keep the full launch list in your head. If a site only feels findable because an operator knows
  the coordinates, move it or add a real clue surface before testing it.
- Before that final survey sprint, generate the external worksheet:
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\new_launch_placement_packet.ps1`**.
  It writes `00-placement.md`, `launch-sites.csv`, and `coords-capture.csv` under `build\launch-placement\`.
  Keep `coords-capture.csv` open while placing: choose one lane, run `/observance site next <lane>` or
  `/observance site plan <siteId>`, survey with `/observance site set <siteId>`, stamp placeworld rows with
  `/observance placeworld`, then record the real world/X/Y/Z, `PlaceworldReceipt` where applicable, visual verdict,
  four proof shots, and cohesion notes. A row is not launch-ready just because it has numbers; it needs
  approach/focal/action/exit evidence, any required stamp receipt, and a reason it belongs in that route.
  Run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_launch_coord_quality.ps1 -CaptureCsv <packet>\coords-capture.csv`**
  during placement to catch duplicate anchors, wrong dimensions, cramped keeper evidence sites, route sprawl,
  and missing proof. For the final pass, add `-Launch` so every row must be `KEEP` with all proof fields, and Nether/End rows must record
  `nether_forge_placed` / `end_seventh_shrine_placed` in `PlaceworldReceipt`.
  If you are applying captured coordinates from the worksheet instead of relying on the server's saved
  `sites.yml`, preview them first with
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\apply_launch_coords.ps1 -CaptureCsv <packet>\coords-capture.csv`**.
  It refuses non-`KEEP` visual verdicts and only writes when rerun with `-Apply`.
- For separate passes, run **`/observance placeregion`** (starter rune-ring + keeper evidence sites) and
  **`/observance placedeep`** (deep-half payoff sites). These persist to `sites.yml` and survive
  restarts.
- **Submitting answers:** edit the **blank** sign (the *labelled* sign is waxed and won't take input,
  so flavour text can't pollute your answer). For one keeper evidence site: `/observance placeroom <keeper>`.
- **Returnability proof:** at least one live-tested puzzle must be reached before the group has enough
  evidence, left by an authored route, revisited after finding the missing clue, and solved without teleporting,
  operator hints, consumed one-use evidence, or a newly-hidden answer surface. If players cannot tell where to
  type the answer when they return, fix the local clueing before launch.
- **Structure surface spot-checks:** the Rosetta lectern must contain the "the rosetta" book and its
  rune/plaintext crib pairs; Orin's low lintel must be passable while sneaking but awkward while standing,
  and its six wall banners must render on masonry backing instead of floating or replacing the walls.
- **Proof the world matches the dialogue:** run **`/observance descentproof`** if you need to stage the
  Lamp-works stair, third lamp, painted line, dead-stall, and empty bird coops around you for fast testing.
  `placehold` places these side-proof destinations inside the production Hold. `prepworld` and `sidepass`
  also stage `school_stand`, `the_far_water`, `markers_row`, `cistern_7`,
  `watch_floor`, `set_apart_shelf`, `undercroft_seal`, `forgotten_mouth`, `deep_market`, `ration_table`, `third_bay_breach`, and
  `warm_town_collapse` for rehearsal; for bespoke final placement, survey them near the real Deep Market/
  Warrens/Deep Line route and run `/observance placeworld`.
- **The Undercroft dimension** (datapack) is a real descendable dark cavern — reach it via Multiverse:
  `/mv create undercroft NORMAL -g observance:undercroft`.
- **The Unlit dimension** is a separate mirrored village world named `observance_unlit`. Follow
  `design/UNLIT-PREARG-STARTUP.md`: duplicate the main world folder, import it with Multiverse, place the
  well entry/spawn/exit/house anchors with `/obs unlit site` and `/obs unlit clue`, then run
  `/obs unlit border 138`, `/obs unlit darken all 138`, `/obs unlit audit`, and `/obs unlit ready`.
  The required ending evidence houses are all eight: lamp, cairn, coop, well, watch, warm, threshold, and
  base. Players receive 3 borrowed lanterns each.
- **The companion:** `/observance wren spawn` places Wren (Citizens if installed, else a fallback body).
- **The townsfolk (REQUIRED — reshape S-G):** `/observance townsfolk spawn` places the 5 surface people
  (Aro/Wenna/Coll/Dob/Old Pell) with their walk-up-and-talk dialogue. `placeregion` does NOT spawn them;
  without this the world's social surface is dark.
- **Before friends join:** run **`/observance preflight`**. It runs hardware audit, visual audit,
  dialogue/world-proof audit, and coverage together. Visual failures marked `REPLACE` and dialogue
  claims without proof are blockers for a real session.
- **Before friends join:** run
  **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_world_build_readiness.ps1 -Launch`**
  from the repo root after `placehold`/curated placement has saved `sites.yml`. This catches the most
  embarrassing launch failure: a required site still exists only as a placeholder coordinate.
- **Before friends join:** complete **`design/LIVE-REHEARSAL-EVIDENCE.md`** once on the actual server.
  Static checks prove wiring; the evidence packet proves scale, readability, scares, NPC/world contracts,
  side-path value, and finale staging. Do not launch on green tooling alone.
  Start it with **`powershell -NoProfile -ExecutionPolicy Bypass -File tools\new_rehearsal_packet.ps1`**.
  The generated `launch-attestations.md` must also be completed before the final launch blocker command can
  pass with `-RehearsalPacket`.
- **Before friends join:** after operator build/audit commands pass, join once as a real non-op player account.
  Prove the player cannot freely break/build inside the Deep Hold protection region, can read books/signs/runes,
  can find answer input surfaces, can make one wrong-answer attempt, can make one correct answer/input, can
  leave and retrace/return without losing state, and can feel one Unlit pressure action with the resource pack
  loaded. Record this under `Normal Non-Op Player Pass` in `launch-attestations.md`.

## 3. SESSION ZERO (before the friends join the fiction)
- Read **`design/SESSION-ZERO.md`** — the out-of-fiction consent + onboarding script. Cover: this
  server watches (in-game behavior; later chat/voice), you can opt out anytime, there's a debrief.
  Veteran group — being watched can land hard, so this is not optional.

- Before enabling capture, handle the consent conversation and record any `observer_opt_out` choices.

## 4. RUN THE FIRST SESSION
1. **Ignite** the arc — either admin `/observance flag set prologue_ignited true`, OR in-world (real
   trigger, built): a player right-clicks a **lectern** at a report-lectern site, or deliberately
   **sneak-right-clicks** near the **rune-rosetta** / reckoning stone. Either sets `prologue_ignited`
   and the first puzzle goes live.
2. Players **read the rune ring** and submit answers by **editing an answer-sign** (clear the sign, type
   the answer, Done). First gate: the rune ring →
   `bow offering kept light deep line unspoken sacred beast`.
3. Solving the ring opens the **six keeper evidence sites** (any order). Each keeper's cipher + hint is
   seeded, but a single decoded site is only suspicion: the ending waits for keeper theories, side proof,
   and the whole Unlit evidence set. The spine runs as a web: keepers, side destinations, townsfolk,
   Unlit, Iss, descent, then the Seventh. Salience surfaces one thread at a time; the hint rail escalates
   if a thread stalls.
4. **Watch for** (this is playtest data): findability (dead air?), whether a scare/toast lands, whether
   people know *how* to answer, and pacing. Capture it — that's what turns "it runs" into "it's good."

   Use `design/LIVE-REHEARSAL-EVIDENCE.md` as the capture standard: approach/focal/answer/exit shots for
   major sites, dialogue-to-landmark proof, puzzle retrace notes, side-path value, and scare clips.

## 5. ADMIN CONTROLS + TROUBLESHOOTING
- `/observance unlit <site|clue|pass|audit|darken|border|buildmode|ready>` - build, test, darken, audit,
  and hand off the mirrored Unlit village. Use `design/UNLIT-PREARG-STARTUP.md` as the exact prep sequence.
- `/observance status` — health (db, queue, sites, reckoning).
- `/observance prepworld [spacing]` — compact playable-world bootstrap for a real rehearsal area.
- `/observance preflight` — one-command in-world readiness pass: audit + visualaudit + dialogueaudit + coverage.
- `/observance visualaudit` — catches tiny/flat/test-prop story sites before players see them.
- `/observance dialogueaudit` — lists NPC claims that must have physical or mechanical proof.
- `/observance descentproof [spacing]` — stages the Stair/third-lamp/painted-line/dead-stall/bird-coops proof chain.
- `/observance site todo|next|plan` — in-game checklist and placement brief for launch proof rows: generated Deep Hold rooms plus outside-Hold coordinate anchors.
- `/observance site set <siteId>` — survey the block you are standing on into `sites.yml`; the command
  reports how many launch-required placements remain.
- `/observance flag <set|clear|list> [key] [true|false]` — drive the storylet gate (e.g. force ignition).
- `/observance placeroom <keeper>` / `/observance placeregion` — stamp the keeper evidence sites.
- `/observance placedeep` — stamp the deep-half payoff sites (hearth, accepting floor, vault, grave,
  Seventh's chamber, reckoning stone).
- `/observance wren spawn|despawn|reckoning` — the companion NPC + his reckoning-choice markers.
- `/observance lens give [player]` — give the Lens (second-sight) item.
- `/observance reload` — reload config.yml + sites.yml.
- `/observance sleep <on|off>` — mute the watcher locally.
- **Nothing resolves on a sign** → check `status` shows db `true` + `sites placed ≥ 1`; make sure the sign
  has ONLY the answer (no placeholder text) and is inside the site radius.
- **Console red `tracker.flush.dossier` 400s** → you skipped schema-repair; re-run `discord/supabase/apply-all.sql`.
- **Toasts don't pop** → the datapack isn't installed/enabled (`/datapack list`).

## 6. HONEST STATUS — what works vs. the real ceiling (read before promising friends anything)
**Works now (built + checked this wave):** `tools/audit_all.ps1` is green: story/data audit,
showrunner runtime checks, dashboard selftests/lint/type/build, Java 21 plugin source compile,
plugin DB contract checks, operator-doc command check, Minecraft sign/book/HUD text-surface fit, plugin jar freshness/contents,
Record/web lure withholding, Record terminal hint escalation, rune alphabet cohesion, and
datapack/resourcepack JSON/reference/zip checks.
Plugin target jar is `observance-0.3.22.jar`; use
`tools/package_plugin.ps1` to rebuild it without Gradle. On the live server, verify with
`/observance status` and `/observance preflight`;
DB connected (drift fixed);
the full puzzle loop; **65 diverse puzzles** (behavior/object/code/coords/spoken + the ciphers) with
**producer listeners** so the non-typed ones are solvable in-world; **crafted per-keeper set-pieces**
plus the **deep-half structures** (Accepting floor, the future-dated grave, the Seventh's chamber, the
co-op vault) via `placeregion`/`placedeep`, persisted; **per-player illusion** (name-on-wall, the Lens,
reflection, watch-dimmed light, per-player fog); **in-world hint delivery**; the **asymmetric co-op
vault**; **Observer Tier-0** (behavior-only "it knows you" — grounded implication, rare); **Wren's
companion betrayal arc live** (NPC + producers + the reckoning choice); the **finale rite** (restore/
erase the Seventh's name); the fog **Undercroft dimension** + reward-toast advancements; the **record
website** (deployable); the reworked+registered beats (reveal→per-player, room-swap→teleport,
spatial-voice→positional, keeper-npc). The Discord audit includes seedcheck, webaudit, sidequestaudit,
specscheck, and resolvecheck; the plugin source check compiled 152 Java files under JDK 21; the jar
check verifies the deployable jar is fresh and contains plugin.yml/config/sites/classes; the asset check
verifies the resourcepack sounds/font texture, pins `observance:runes` to the A-Z/0-9 generated atlas, and
checks both zip packages; the dashboard selftest keeps the web Record rune marks geometrically identical to
the canonical Discord/resource-pack alphabet; the media readiness check verifies the 11 OGG files are mono
Vorbis with sane duration.

**Built but only the live server can *prove*:** the illusion visuals, visual scale of stamped
structures, dialogue/world proof, the co-op partition with a real group, the companion arc end-to-end,
the Undercroft generation, and the deep-site rites. In-world behavior is unproven until you run
`/observance preflight` and then complete `design/LIVE-REHEARSAL-EVIDENCE.md`.

**The honest ceiling — genuinely needs you / more infra:**
- **Audio polish** — the resource pack now ships 11 `.ogg` files and the audit verifies `sounds.json`
  references them, and that they are mono Vorbis with sane duration. What still needs live tuning is
  taste, mix, volume, and timing in actual Minecraft space.
- **Observer Tiers 1–2** (the chat/voice archivist that quotes real words back) — needs the always-on
  hosted bot + Whisper + LLM budget. Tier-0 (behavior) is built; 1–2 are the hosting lift.
- **World arrangement** — `prepworld` now gives you a cleared compact rehearsal board, while
  `placeregion`/`placedeep` still scatter and harden set-pieces. Weaving the final route into an evocative
  landscape / re-dressed ancient-city + trial-chamber is still the big
  live-server art pass. Run `visualaudit`; treat `REPLACE` findings as blockers.
- **Small polish (P2):** the desire-path grave (needs a heatmap read-path), the deep-site choice-markers
  need hand-placing, the black-moon toll's temporal *gate* (works, just not restricted), `bases` id-type.

**Bottom line:** this is no longer "an escape-room with good writing" — the **haunting layer is built**
(per-player illusion, Observer Tier-0, the companion betrayal, the co-op vault, the reunion rite). What
stands between it and a *great* run is now **live-server tuning + audio mix + hosting the voice layer**, not
missing features. The full remaining backlog is in `design/IMPROVEMENT-AUDIT.md`. Playtest it.
