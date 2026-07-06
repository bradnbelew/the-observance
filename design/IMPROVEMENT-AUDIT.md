# THE OBSERVANCE — IMPROVEMENT AUDIT (red-team + prioritization)

> A rigorous, grounded "why could this break / where is it not as good as it could be / how to
> improve" pass across the whole project, written **2026-07-01** against HEAD. Every finding cites
> real files/lines. READ-ONLY audit — nothing here is fixed; it is a prioritized backlog.
>
> **Threat model (assumed players):** veteran Minecraft + ARG players who will datamine, NBT-edit,
> xray, spectator-peek, brute-force, get stuck for hours, join late, go off-script, and try to break
> the fiction. Plus: one builder with limited time.
>
> **Priority key:** **P0** = blocks a good friends run (crash / dead loop / security hole /
> unreachable content) · **P1** = real quality gap · **P2** = polish / future.
>
> **A note on the canon docs:** several are stale relative to the code, which has drifted *forward*.
> OVERHAUL §2 says "Java cannot touch flags at all" and "IgnitionListener absent" — both are now
> FALSE (see [F1]). Where this audit and a design doc disagree, this audit was checked against the
> actual tree. The single most reliable operational doc is `design/RUNBOOK.md §6` (honest status).

---

## EXECUTIVE SUMMARY — TOP 10 FINDINGS

1. **[P0] Three plugin→Supabase writes 400 on the live schema, and `schema-repair.sql` does NOT fix
   two of them.** `event_log` inserts (every subsystem's audit/error log), `bases` upserts (base
   detection), and `settings` reads (remote watcher-sleep) all mismatch the real columns/types. The
   "it must run" guardrail is failing in ways the docs claim are resolved. (`EventLogRow.java`,
   `BaseDetector.java:107`, `SettingsRow.java:16`.)

2. **[P0] The rich per-keeper world templates and the rune-ring rosetta EXIST in code but are never
   placed** — `placeroom`/`placeregion` call the generic `keeperStone()` instead of the
   `keeper(id,…)` dispatcher, so every site is an identical 4-block pillar. The RUNBOOK tells players
   to "read the rune ring" that the tooling never builds. One-line-per-callsite fix unlocks the whole
   authored visual layer. (`ObservanceCommand.java:149,226` vs `StructureTemplates.java:64`.)

3. **[P0] Five signature beats are coded but not registered → they can never fire.** `RevealBeat`,
   `RoomSwapBeat`, `KeeperNpcBeat`, `ModeledMobBeat`, `SpatialVoiceBeat` are absent from
   `BeatLibrary.registerDefaults()`. The "it knows ME" reveal, the room-swap, and the keeper NPC are
   all dead payloads. (`BeatLibrary.java:57-90`.)

4. **[P0] The resource-pack `pack_format` (34 ≈ MC 1.21.1) does not match the pinned server
   (1.21.11).** Clients will reject or mis-load the pack; the rune font + all four sound events fail
   to apply. There are also **zero `.ogg` files** — every sound beat and the biome `mood_sound` play
   silence. (`resourcepack/pack.mcmeta:3`, `resourcepack/assets/observance/sounds/` empty.)

5. **[P0] The whole companion (Wren) arc, the reckoning branch, and the co-op plate are DEAD GATES.**
   `metapuzzle_seed.sql:204-215` documents `companion_trust / companion_revealed /
   reckoning_condemn|understand|free` as `PRODUCER: plugin` — but the producing listeners
   (`CoopPlateListener`, `SeventhChoiceListener`, a companion-trust listener) do not exist. The
   content references them; nothing sets them. (Spine to the Seventh is still reachable — this is the
   enrichment layer going inert.)

6. **[P1] The "salience showrunner" is a deterministic clue-drip, not salience.** `decide.ts` orders
   the next thread by `outcome_rank → key`, with **no** `recency × player-fingerprint − recent-tone`
   salience and no roster-aware convergence gate. The OVERHAUL's headline "one salient thread chosen
   *for them*" (Pillar 2 / T2) is only half-built; dead-air from mis-surfaced quorum threads is
   unmitigated. (`discord/src/showrunner/decide.ts`.)

7. **[P1] The record website is an un-wired read-only shell, and its one live view was never
   migrated.** `record/[slug]/page.tsx:95` reads `v_record`, which **no migration creates**, so the
   archive is permanently frozen at baseline. The headline "write answers INTO the record" feature
   does not exist. (Dashboard security itself is *solid* — service key is server-only, RLS is
   deny-by-default, the 0003 lockdown closed the real escalation hole.)

8. **[P1] The "zero-manual, amazing world" promise collides with reality.** `placeregion` stamps 7
   identical bare pillars in a straight row on the overworld surface; the Undercroft datapack
   dimension is an empty air-void with `features:false`; `WORLD-BUILD.md` still assumes Ethan
   hand-builds a rich vertical descent. The CHANGE-MANIFEST §6 itself flags these as unreconciled
   orphans. The world is the #1 driver of "amazing" and is currently the weakest surface.

9. **[P1] Per-player illusion — the literal "it knows ME" — is missing its core primitives.**
   `PerPlayer` has `fakeBlock`/`sound`/`particle`/`title` but **no `showEntity`, no packet light, no
   per-player fog**. The asymmetric co-op vault and the name-on-the-wall scare both depend on these
   and cannot be built until they land. (`util/PerPlayer.java`.)

10. **[RESOLVED] The back half used to be one wrong seed-apply from either dark or leaking.**
    `npm run db:seed` now regenerates `discord/supabase/apply-all.sql`, which owns both database
    lineages, places 0006/0007/0008 before every seed, includes the later launch migrations, and ends
    with schema repair. `npm run audit` runs `db:bundlecheck`, so drift fails loudly.

---

## AREA 1 — THE PLUGIN

### What could break / fail / race
- **[F1] Docs vs. reality (context, not a bug).** OVERHAUL §2 is stale: `IgnitionListener` exists
  and is registered (`ObservancePlugin.java:331`, `IgnitionListener.java`), the Java oracle DOES
  gate on `requires_flags` (`OracleResolver.firstMatch` → `FlagGate.satisfied`,
  `OracleResolver.java:198-214`), and DOES apply `set_flags` atomically
  (`OracleResolver.applyOutcome` → `mergeArcFlags`, `:251-254`). The engine's spine is in better
  shape than its own overview claims. Trust the code.
- **[P0-A1] Five beats never registered → dead payloads.** `RevealBeat`, `RoomSwapBeat`,
  `KeeperNpcBeat`, `ModeledMobBeat`, `SpatialVoiceBeat` are not in `BeatLibrary.registerDefaults()`
  (`BeatLibrary.java:57-90`; verified each `new X(` count = 0). Any `beat_queue` row of these types
  is fetched, hits `enactor` with no handler, and is left `UNHANDLED` (queued forever) — silent dead
  air where a reveal/room-swap/NPC was authored.
- **[P1-A2] Beat double-fire on a fast restart / second instance.** `BeatQueuePoller` claims beats
  only with an **in-JVM** `inFlight` set (`BeatQueuePoller.java:42,84`); the durable guard is the
  post-enact async `markBeatDecided` PATCH. There is no atomic DB claim (e.g. `PATCH status→'firing'
  WHERE status='approved'`). If the process dies after enacting but before the PATCH lands, the row
  is still `approved` and re-fires next boot. Two plugin instances against one DB double-fire freely.
  The `solves` unique-index protects *reward* idempotency, but a directed/ambient beat (door, mob,
  structure) will visibly re-fire. (`SupabaseClient.markBeatDecided:182-206`.)
- **[P1-A3] `RoomSwapBeat` is still in-place, not the teleport the design chose.** It clears+pastes a
  footprint (`RoomSwapBeat.java:122-138`), which OVERHAUL §5 explicitly says to rework into a
  sealed-door teleport reveal. It also hard-requires FAWE **and** a `undercroft_room_b.schem` file
  that does not ship — so even if registered it skips (`:87,91`). Compound with [P0-A1]: doubly dead.
- **[P1-A4] Ignition is easy to trip accidentally.** `IgnitionListener` fires on *any* right-click
  within the rosetta/reckoning site radius (`:119-125`, "proximity-based rather than block-face
  exact"). A player placing a torch or opening a chest near the stone silently ignites the arc before
  the group has read anything — the intended "examine the stone" gesture is indistinguishable from
  incidental interaction. For a group that will poke everything, this misfires early.
- **[P2-A5] `reloadAll()` resets in-memory dossier + drops all fake-blocks without resend.** A
  `/observance reload` rebuilds the tracker and `HandlerList.unregisterAll` (`ObservancePlugin.java:
  260`) but does not re-send active per-player `fakeBlock` illusions or clear them — a mid-beat
  reload can strand a client-only block until chunk reload. Minor; reload is an admin action.

### Not as good as it could be
- **[P1-A6] Per-player illusion primitives incomplete** (top-10 #9). `PerPlayer.java` has no
  `showEntity` (per-player entity visibility), no packet light-dimming, no per-player fog. INTEGRATION
  Layer 3 + "signature integrations #1" depend on all three. The "it knows ME" feeling is currently
  achievable only via `fakeBlock`/sound/title — a real but thin subset.
- **[P1-A7] The rune font is built but applied nowhere.** `runes.json` maps SGA glyphs onto `A-Z0-9`
  and the README says beats "only need the `observance:runes` font tag" — but no beat or
  `StructureTemplates` sign actually sets that font (grep clean). So carved clues render in plain
  ASCII; the keepers' alphabet — a core aesthetic — is invisible in-world. (Also the INTEGRATION-
  flagged PUA-codepoint collision risk is unaddressed: the font overrides base Latin, so any text
  tagged with it is runes, and mixing is impossible.)

### Concrete improvements
- Register the five beats (or explicitly cut them from the manifest so the docs stop implying they
  work). For `RoomSwapBeat`, either ship the schematic + FAWE or rebuild as a vanilla teleport reveal.
- Add an atomic DB claim to the poller: `PATCH beat_queue SET status='firing' WHERE id=? AND
  status='approved'` returning the row; only enact on a non-empty return. Kills the restart/2-instance
  double-fire.
- Gate ignition on a **specific block** (the sign/lectern face) or require sneak-right-click, so
  incidental interaction can't ignite.
- Build `PerPlayer.showEntity` + packet light + per-player fog before Phase 3.
- Apply `observance:runes` on the carved-clue components (and resolve the PUA remap so ASCII UI text
  can coexist).

---

## AREA 2 — THE DB + SCHEMA

Two Supabase lineages that **do not share history**: `dashboard/supabase/migrations` is the canonical
base and public view lineage; `discord/supabase/migrations` extends it with the oracle, threads,
observations, rate limits, and showrunner lock. The launch path is now the generated
`discord/supabase/apply-all.sql` bundle, not loose per-folder application. It includes both lineages
in dependency order and is checked by `npm run audit`.

### What could break (confirmed 400s the plugin will throw against the live schema)
- **[P0-D1] `event_log` inserts 400 on every call.** `EventLogRow.java` serializes `type, context,
  message, mc_uuid, detail, created_at`; the table has only `level, source, message, created_at`
  (`dashboard/0001_init.sql:115-121`). PGRST204 "column not found." The dashboard + bot write the
  correct `{level,source,message}` — the plugin is the lone wrong writer, at 6+ call sites
  (`ObservancePlugin.java:289,525`; `BeatSessionListener.java:85`; `KeeperNpcListener.java:119`).
  `insertEventLog` does not re-queue on failure → every plugin audit/error event is silently lost.
  **schema-repair.sql does not touch this.**
- **[P0-D2] `bases` upsert 400s — UUID string into a bigint PK.** `SupabaseClient.upsertBase`
  conflict-targets `id` (`:133-135`) and `BaseDetector.java:107` sets `id = owner UUID string`, but
  `bases.id` is bigint (`0001_init.sql:64`). schema-repair adds `owner_uuid` etc. but leaves `id`
  bigint and the conflict target unchanged → base detection keeps failing after the repair.
- **[P0-D3] `world_paste_ledger` is created by no migration.** `SupabaseClient.claimPasteLedger`
  (`:464-498`) POSTs to it; schema-repair explicitly punts it. Masked only because it is gated behind
  FAWE being installed. The moment schematic-paste is enabled, every claim 404s → set-piece skipped
  (fail-closed, at least, not double-pasted). Latent P0 for the FAWE feature.
- **[P1-D4] Remote `watcher_sleep` toggle is inert.** `SettingsRow.value` is `String`
  (`SettingsRow.java:16`) but the column is `jsonb` storing a JSON boolean; Gson throws → `fetchSetting`
  fails → `isWatcherSleeping()` fails-open to `false` (`SupabaseClient.java:229`). The dashboard's
  "watcher sleep" switch **cannot mute the plugin**; only the local config kill-switch works.

### Not as good as it could be
- **[P1-D5] dossier/compliance/base drift is repaired for WRITES but orphaned for READS.**
  schema-repair adds the plugin's flat columns so writes stop 400ing, but the dashboard still reads
  the *original* columns (`Dossiers.tsx:76-105` reads `solo_ratio/blocks_mined/group_distance/
  hoard_summary`; the plugin writes `solo_mining_seconds/hoarded_score/distance_from_group/extra`).
  Result: **every plugin-written dossier renders blank in the author dashboard** — the "it knows me"
  data the operator relies on is invisible. Same shape for `custom_compliance` and `bases`.
- **[P2-D6] `fetchArcState` reads `order=updated_at.desc&limit=1`** instead of `id=eq.1`
  (`SupabaseClient.java:237`) — harmless given the single-row check constraint, but sloppy and a
  latent bug if a second row ever appears.

### What's actually GOOD (keep — don't refactor)
- **Flag flow is atomic and race-safe.** `observance_merge_arc_flags` does `flags = flags || p_flags`
  in one statement (`0006:57-68`), `security definer`, service-role-only. `arc_state` is single-row
  (`id=1` check). Both surfaces AND-test `requires_flags` in app code with a byte-parity gate
  (TS `flagsSatisfied` ↔ Java `FlagGate`). `insertSolveIfNew` + `solves unique(puzzle_key,player_id)`
  makes concurrent solves single-reward. No lost-update on flags.
- **RLS is deny-by-default and correct.** Every table has RLS; spoiler tables have no anon policy and
  `revoke all from anon/authenticated`; only three SECURITY-DEFINER views are anon-readable and they
  expose no names/labels. `0003_lockdown.sql` closed a real hole (0001 had granted `authenticated for
  all using(true)` on all base tables). **Confirm 0003 is applied to Braden's project before go-live —
  the whole spoiler-safety argument rests on it.**
- Seeds are idempotent (`on conflict` upserts); `apply-all.sql` ordering runs the flag/kind/quorum
  migrations before the guarded activation UPDATEs, so the historical "guards no-op" bug is resolved.

### Concrete improvements
- Fix `EventLogRow` → `{level,source,message}` (or ALTER the table). Re-key `upsertBase` off
  `owner_uuid` (+ unique index) or make `bases.id` text. Change `SettingsRow.value` → `JsonElement`.
- Add a read-side mapping (a view, or update the dashboard columns) so plugin dossiers show up.
- Create `world_paste_ledger` before enabling FAWE.
- The historical `answer_kind` gap is closed by `discord/supabase/migrations/0007_answer_kind.sql`,
  and the launch bundle checks that it lands before the seeds.

---

## AREA 3 — DATAPACK + RESOURCE PACK

### What could break
- **[P0-R1] Resource-pack `pack_format` 34 ≠ server 1.21.11.** `resourcepack/pack.mcmeta:3` ships 34
  (≈1.21.1). 1.21.11 needs a much higher format (and the newer single-int format is deprecated in
  favor of a version range). Modern clients on 1.21.11 will warn/refuse or fail to apply → the rune
  font and all sounds silently don't load. `WORLD-BUILD.md` says "48/57/61", the README says 34, the
  datapack says 94 — **three different pack-format truths in the repo.** Pick one against the pinned
  server and reconcile.
- **[P0-R2] Zero audio files.** `sounds.json` declares `whisper/drone_low/stone_breath/cold_toll` but
  `resourcepack/assets/observance/sounds/*.ogg` is empty (0 files). Every `PrivateSoundBeat` and the
  three biomes' `mood_sound` play silence. Atmosphere — a headline pillar — is currently mute.
- **[P1-R3] The Undercroft dimension is an empty air-void.** `dimension/undercroft.json` is a flat
  generator with one layer of `air` and `features:false` — no floor, no structures, no keeper stones.
  Anyone teleported in falls forever. The RUNBOOK places all sites in the overworld, so the fog
  dimension is currently unused; its fog/mood exists but there is nothing to stand on or find inside.
- **[P2-R4] The reward advancements use `trigger: minecraft:impossible`** (`the_record_receives_you.
  json:19-21`) — correct pattern (they're plugin-granted via `AdvancementToastBeat`), but confirm the
  1.21.11 advancement `display.icon` schema (`{"id": "..."}`) still validates on 1.21.11; the item-
  component format tightened across 1.21.x. If it fails to parse, the datapack disables and toasts die.

### Not as good as it could be
- **[P1-R5] Only 3 biomes, all identical mood.** All three `undercroft_*` biomes point `mood_sound` at
  the same silent `observance:whisper`. Once audio lands, differentiate cold/dark/void so the descent
  *feels* like it changes.
- The rune font's ASCII mapping (Area 1 [P1-A7]) is a resource-pack-side design flaw too: it should
  live in a PUA range so it can coexist with normal UI text.

### Concrete improvements
- Regenerate the resource pack with the correct 1.21.11 `pack_format` (use the version-range form) and
  reconcile all three docs to it.
- Source the four OGGs (CC0 + TTS → `ffmpeg -ac 1` mono) — this is the single highest atmosphere ROI.
- Either build geometry inside the Undercroft (a floor + the gather-room) or keep it overworld-only and
  mark the dimension explicitly deferred so no one teleports into the void.

---

## AREA 4 — STRUCTURES / VISUALS

### The concept + how the code-placed builds fall short
- **[P0-V1] The authored per-keeper templates are never placed** (top-10 #2). `StructureTemplates`
  has a full dispatcher `keeper(id, base)` (`:64`) routing to atmospheric, sign-labeled builds —
  `rosetta()` (a 6-pillar ring, `:118`), `vaun()` hoard-ledger, `mara()` margin, `sella()` water,
  `orin()` low bow-stone, `brann()` watch, `iss()` warm-trap (`:169-480`). But `handlePlaceRoom`
  (`ObservanceCommand.java:149`) and `handlePlaceRegion` (`:226`) both call the **generic**
  `keeperStone(base)` (`:92`) — a 4-block pillar with a "keeper stone / speak your answer" sign. So
  `placeregion` builds seven identical generic pillars in a straight east-west row, and the rosetta
  ring the RUNBOOK tells players to read is never built. **This is the biggest "already-built but
  unwired" gap in the project** — a per-callsite one-line fix (`keeper(siteId, loc)`).
- **[P1-V2] The world is a bare surface row, not a descent.** Even with [P0-V1] fixed, `placeregion`
  lays sites in a flat line at the sender's Y on the overworld. `WORLD-BUILD.md` describes the
  *intended* experience — a vertical carved descent, dark-by-default, the bow built into low lintels,
  reveal-by-sightline — none of which the tooling produces. The "code-generated world looks generic"
  risk (CHANGE-MANIFEST §6, D7w/A11) is realized. For veterans who instantly recognize raw/placed
  vanilla, this is the immersion-killer.
- **[P1-V3] Reveal-safety is fine for beats but N/A for placement.** `StructureTemplates` deliberately
  places synchronously and unprotected (it's an admin build), so there's no "placed while witnessed"
  problem — but that also means the group can watch the admin stamp the world, breaking the
  "discovered, never witnessed appearing" law if placement isn't done pre-session. Document: place
  before friends join.
- **[P2-V4] Answer-sign UX is unlabeled.** The generic stone's sign reads "speak your answer"; the
  per-keeper stones' signs (when wired) are diegetic prompts. Good — but WORLD-BUILD's own note ("don't
  rely on the group guessing a sign is the input") stands: a first-timer may not realize *editing* the
  sign is how you answer. Consider a one-time in-fiction tell.

### Concrete improvements
- Wire `placeroom`/`placeregion` to `keeper(id, …)`; place the rosetta as a ring, keepers in a
  branching field (WORLD-BUILD §4), not a row.
- Invest the limited builder-time in the R&D "cohesive procedural building" sub-task (tight palette,
  dark-default lighting, decay passes) OR accept a hand-built Minimum-Amazing region and drop the
  zero-manual promise — but pick one and reconcile the docs (see Area 7).

---

## AREA 5 — CONTENT / ARC

### Reachability (the good news + the dead branches)
- **[F2] The main spine IS reachable end-to-end via typed answers.** Every spine flag has a puzzle
  producer: `rosetta_known` → keepers → `iss_caught` (`puzzles_seed.sql:293`) → `undercroft_open`
  (`:322`) → `seventh_named` (`:691`) → `threshold_open` (`:835`) → `true_coord_known` (`:867`) →
  `true_destination_reached` (`:886`) → `record_received` (`:533`). `prologue_ignited` is the single
  root. No dead gate on the critical path.
- **[P0-C1] The companion / reckoning / co-op layers are DEAD GATES** (top-10 #5).
  `metapuzzle_seed.sql:204-215` documents `companion_trust`, `companion_revealed`, and
  `reckoning_condemn|understand|free` as `PRODUCER: plugin` — but no `CoopPlateListener`,
  `SeventhChoiceListener`, or companion-trust listener exists (grep: 0 files). Content that references
  these flags never opens. Wren — "the single highest callback-density item" (CHANGE-MANIFEST §5) — is
  authored (`arc/lore/documents/the-companion.md`) but mechanically inert. `coop_plate` is only a
  `site_id`/thread tag, not a produced flag, so the asymmetric co-op vault has no gate producer either.
- **[P1-C2] `true_destination_reached` / `seventh_found` likely need a coords/arrival listener that
  isn't built.** These read as travel/behavior answers, but there is no `answer_kind` column and no
  territory-arrival producer for the finale (`TerritoryListener` tracks compliance, not puzzle
  arrival). If the finale answer is a typed phrase it works; if it's "arrive at XYZ," the spine's last
  hop may not be solvable in-world yet. Verify the finale puzzle's `answer_kind` assumption.
- **[RESOLVED P0-C6] Seed re-run ordering is now enforced.** The generated `apply-all.sql` bundle is the
  launch path, and `npm run audit` verifies its ordered file markers before a session.
- **[P1-C7] `companion-reveal` is a dangling `requires_flags` on a non-existent row.**
  `metapuzzle_seed.sql:233` sets `requires_flags={iss_caught}` on `puzzle_key='companion-reveal'`, but
  no such row exists in any seed — the UPDATE matches 0 rows (silently wrong the day a producer lands,
  and misleading in any reachability ledger). Part of the same dead-companion branch as [P0-C1].
- **[P2-C8] Literacy + token-laying are not actually enforced.** `rosetta_known` (set by the literacy
  on-ramp) and `tokens_laid` (set by `rite-tokens`) gate **no** puzzle via `requires_flags` — the six
  keeper stones are ungated `active=true`, so a player can answer a keeper stone without ever solving
  the Rosetta, and the terminal rite's ordering leans on `next_puzzle_key` not a gate. If the design
  intends "must be literate first," it's un-enforced.
- **[P1-C9] `seedcheck` validates normalization ONLY** — not reachability, collisions, hint coverage,
  or the ordering hazard. So C1/C3/C6/hint-gaps would all pass CI green. Add gate-reachability +
  collision + coverage checks to the self-test so these can't regress silently.

### Cross-answer collisions
- **[P1-C3] Live same-window collision: `the last marker is not the last`** on `stone-sella`
  (`puzzles_seed.sql:149`) **and** `seventh-shrine` (`:363`). Both can be OPEN at once (`stone-sella` is
  ungated `active=true`; `seventh-shrine` opens via Sella's next-clue). If a player types the phrase
  before solving Sella, the resolver resolves *Sella* (first unsolved in DB order) and the
  seventh-shrine payoff for that string never surfaces — a wrong-node resolution. This is the real
  unresolved collision; disambiguate one owner in the seed.
- **[F6] The historically-flagged `the one who turned away` collision is FIXED** (my initial read was
  wrong): `prophet-wall-name` no longer shares the bare phrase (it uses `iss carved the wall` /
  `read the first marks down the one who turned away`). The remaining duplicate (`stone-iss-wall` +
  `bound-word`) is the *intended sequenced pair* and is safe — `bound-word` is `active=false` until
  `iss_caught`, so the two are never open simultaneously and the resolver's unsolved-preference picks
  correctly. No action needed there.
- **[P2-C6] Narrow collision** `a thing that can say no is not a wall` on `seventh-shrine` (`:366`) +
  `seventh-cause` (`:714`) — both lore, overlap window small (`seventh-cause` gated) so payoff is
  similar; low priority.

### Hint coverage / findability
- **[F3] Hints are NOT empty** (the OVERHAUL's "empty table" claim is stale). `hints_seed.sql` covers
  the 5 keeper ciphers + the literacy on-ramps + the Iss catch (tiers 2–3; tier 1 is an ambient nudge
  in `voice.ts`). **But [P1-C10] the entire back half has ZERO hints:** `bound-word` (capped
  Vigenère re-read, a real choke point), `stone-brann-cipher`, `m4-three-hands`, `threshold-coordinate`,
  `true-walk-arrive`, `seventh-unwriting` (rail-fence), `seventh-choice`. `hints_seed.sql:13-14`
  concedes this is spine-ciphers-only. For a HARD non-linear ARG, a capped cipher with no hint tier is
  a hard-stall.
- **[P1-C4] The salience showrunner is a clue-drip, not salience, and its pool is only 5 ciphers**
  (top-10 #6). `decide.ts` orders by `OUTCOME_RANK → key`, not `recency × fingerprint − recent-tone`,
  and is **not roster-aware** (never checks `activeRosterSize ≥ effectiveQuorum` before surfacing a
  convergence thread — the exact T2 dead-air the OVERHAUL says to prevent). Worse, the drip pool is
  filtered to `p.forgeable` = the `CLUE_SPECS` registry, which has **exactly 5 entries** (the keeper
  ciphers). The entire back half (Iss catch, Undercroft, Seventh, rite, Brann-cipher, Nether/End) is in
  `NON_CIPHER_KEYS` and is **non-drippable** — so after the 5 stones are dripped, the autonomy loop
  emits "pool empty" and there is **no in-world thread pointer for the rest of the game.** Findability
  effectively stops after Movement II. (`decide.ts:91`, `clue-specs.ts:111-299`.)
- **[P2-C5] No in-world "thread pointer."** With Discord's game-persona slated for retirement (Pillar
  4) and the record website un-wired (Area 6), nothing surfaces the next thread in-world. The soft-
  pressure "there is always one thread with momentum" promise has no in-world producer yet.

### Concrete improvements
- Build the missing flag producers (`CoopPlateListener`, `SeventhChoiceListener`, companion-trust) or
  explicitly defer Wren/reckoning/co-op and strip their references so the arc doesn't dangle.
- Collapse the `the one who turned away` collision to one owner.
- Implement real salience + roster-awareness in `decide.ts`; add an in-world drip surface (the record
  block / a whisper) so world-primary findability doesn't depend on the retired Discord persona.
- Set-diff hints vs spine puzzles; author any missing tiers before real play.

---

## AREA 6 — THE WEBSITE

### Security model (solid — keep)
- Service-role key is `import "server-only"` (`dashboard/src/lib/supabase/admin.ts:1`), never in a
  client bundle; only `NEXT_PUBLIC_` URL+anon are exposed. `.env.local` holds live keys but is
  correctly gitignored. Auth uses `getUser()` (validated, not cookie-trusted). The author page + all 7
  server actions re-check `isAdmin()` per call (`author/actions.ts:35-37`). RLS is the wall for spoiler
  tables and it's a correctly-built deny-by-default wall — a curious veteran cannot pull answers/arc/
  dossiers from the browser.

### What could break / leak
- **[P1-W1] `v_record` view referenced but never migrated** (top-10 #7). `record/[slug]/page.tsx:95`
  reads `v_record`; no `.sql` creates it (only `v_health/v_heatmap/v_compliance_counts` exist). Every
  read hits the catch → the Record is **permanently frozen at the sealed baseline** and never
  un-redacts with progress. Fails safe (no leak, no 500) but the centerpiece behavior is inert.
- **[P1-W2] The "write answers into the record" feature does not exist.** The `/record` route is
  strictly read-only — no POST handler, no server action, no `createAdminClient` under `record/`.
  Remote submission happens only via Discord + in-world signs. This is a design-vs-code gap against the
  consistency principle, not a bug — but it's the website's headline feature.
- **[P1-W3] Admin nav chrome leaks onto the public archive.** The root layout wraps every route
  (including `/record`) in "Status / Author" nav; the intended pathname strip is described but not
  implemented (`record/layout.tsx`). A player who finds the URL sees dashboard chrome — immersion
  break, worsened by the known-author lens (D6).
- **[P2-W4] Stale "Open — no login required" copy** on the author page (`author/page.tsx:210-211`) +
  a `return true` dev-bypass floated in `actions.ts:33` comment + a README that still documents the
  pre-0003 "authenticated users get full read/write" model. The gate is intact today, but this is a
  footgun that invites someone to "helpfully" open the console. The lure download link
  `/the-hold/the-hold.zip` (`page.tsx:159`) 404s if the asset isn't placed in `public/`.

### Oracle duplication (low risk)
- Exactly two normalizers (`discord/src/oracle/normalize.ts` ↔ `plugin/.../AnswerNormalizer.java`),
  byte-for-byte equivalent, both carrying "change both together" warnings; the website implements no
  matching (it has no write path). Residual risk: parity is convention + selftests, not a shared CI
  test that runs both. If someone edits one regex, nothing auto-catches divergence.

### Concrete improvements
- Author the `v_record` view (the coarse, spoiler-safe projection the page already clamps through);
  build the write path as an **edge function** (never the service key in the browser). Strip nav under
  `/record`. Fix the stale copy + README + lure asset.

---

## AREA 7 — INTEGRATION COHERENCE (the CHANGE-MANIFEST cohesion gate)

The project's own §6 cohesion pass concludes **"Nothing in this manifest is built or wired yet"** and
names the orphans below as MUST-reconcile. This audit confirms they are still unreconciled:

- **[P1-I1] Zero-manual vs hand-built world (orphan, named in §6).** `RUNBOOK`/`OVERHAUL` promise
  one-command world-build; `WORLD-BUILD.md`, `structures.md`, and BUILD-PLAN §11–12 still describe a
  hand-built descent. The tooling ([P0-V1]/[P1-V2]) delivers neither the rich auto-build nor a
  hand-build workflow. **Pick a lane and rewrite the losing docs.**
- **[P1-I2] SESSION-ZERO over-discloses vs what's built.** `SESSION-ZERO.md` reads a consent script
  covering **voice tracking** and points at **the record website URL** and **`/link`** — but the
  Observer voice layer isn't wired and the record website is an un-wired shell (RUNBOOK §6 admits
  both). Disclosing capabilities that don't exist over-promises to the friend group and dates the
  script. Align the script to what actually ships tonight vs. later waves.
- **[P1-I3] Lore↔mechanics lockstep broken for the companion.** Wren's prose is the gold; his
  mechanics are dead ([P0-C1]). This is the exact "orphaned mechanic on stale ARG state" the
  consistency principle forbids — an authored character with no producer.
- **[P2-I4] Visuals↔lore lockstep broken for the rune font.** The keepers' alphabet is authored + the
  atlas is generated, but nothing in-world renders it ([P1-A7]). The cipher's *look* is decoupled from
  the cipher's *content*.
- **[F4] What DOES tie together (keep):** the oracle is one shared engine across Discord + in-world
  signs (same normalizer, same `puzzles` table, same flag gate); `hints` is a single source intended
  for both surfaces; the flag graph is internally consistent on the spine. The *machinery* coheres;
  the *surfaces and the enrichment layers* are where the orphans live.

---

## AREA 8 — THE FRIENDS-RUN EXPERIENCE

### The honest "is this amazing yet" answer
**Not yet — but the bones are unusually good.** A genuine, grounded, cross-surface *puzzle-and-lore*
loop can run tonight (ignition → gated cipher → in-world sign solve → flag flips → reward beat). That
is real and rare. But the three things that make it *"From The Fog, but it knows your name"* —
**atmosphere** (no audio, no fog geometry, generic pillars), **"it knows ME"** (per-player illusion
primitives + Observer engine unbuilt), and **the companion betrayal** (dead gates) — are all in the
next wave, not this one. Today it's an atmospheric escape-room with excellent writing; the haunting
that would make it YouTube/ARG-critic-worthy is scaffolded, not standing.

### First 30 minutes / onboarding
- **[P1-X1] Findability cliff at minute one.** With the rosetta ring unplaced ([P0-V1]) and no
  in-world thread pointer ([P2-C5]), the first thing players must do — read the rune ring to get
  `bow offering kept light deep line unspoken sacred beast` — has no artifact to read. High dead-air
  risk on the very first gate. Also the ignition can trip accidentally before they're ready ([P1-A4]).
- **[P1-X2] "How do I answer?" is unobvious.** Editing a sign as the submission verb is a novel
  mechanic; WORLD-BUILD flags it. Without the labeled per-keeper lecterns (unwired), a group can stall
  not on the puzzle but on the *interface*.

### Pacing over weeks / async
- **[P1-X3] Dead-air becomes a cliff after Movement II.** [P1-C4]: the drip surfaces the 5 keeper
  ciphers on Discord by static rank, then the pool goes empty — the entire back half has no
  autonomy-driven pointer, no hints ([P1-C10]), and (Discord persona slated for retirement) no
  world-side surface. A group that solves the stones and doesn't physically stumble onto the next site
  has nothing pulling them forward. This is the single biggest threat to the multi-week experience.
- **[F5] Consent handling is genuinely thoughtful** (`SESSION-ZERO.md`) — disclose-existence-not-
  mechanism, easy opt-out, mandatory debrief. Right instinct for a veteran group; just align it to
  reality ([P1-I2]).

### Datamining stance (veterans WILL)
- **[P2-X4] D7 "reward the datamine" is aspirational.** The design says leave a message for the xrayer
  / make the NBT item worth inspecting — none of that is built. A veteran who reads the seed sees every
  answer (RLS protects the *live* DB, not a leaked SQL file). The spoiler-safety is DB-side; the
  *design*-side "weaponize transparency" is unimplemented. Accept that a determined datamine trivializes
  the puzzle content and lean into doing/reactivity as the joy (which the D8 Golden Question already
  pushes).

### Concrete improvements
- Ship the rosetta ring + labeled lecterns before the first session (fixes the minute-one cliff).
- Wire a minimal in-world thread pointer (a whisper or the record block) so soft-pressure has a
  producer world-side.
- Source audio + the correct pack format — the cheapest, highest-impact "amazing" upgrades.
- Set expectations with the group per RUNBOOK §6: this session is the puzzle-and-lore slice; the
  signature scares come next.

---

## PRIORITIZED BACKLOG

| ID | P | Area | Finding | Fix sketch |
|----|---|------|---------|-----------|
| P0-D1 | P0 | DB | `event_log` inserts 400 (wrong columns); all plugin logs lost | `EventLogRow`→`{level,source,message}` or ALTER table |
| P0-D2 | P0 | DB | `bases` upsert 400 (UUID→bigint PK); repair doesn't fix | re-key upsert on `owner_uuid` + unique idx, or `id` text |
| P0-V1 | P0 | Structures | Rich per-keeper templates + rosetta never placed | wire `placeroom`/`placeregion` → `keeper(id,…)` |
| P0-A1 | P0 | Plugin | 5 signature beats coded but unregistered → never fire | add to `BeatLibrary.registerDefaults()` (or cut) |
| P0-R1 | P0 | Pack | `pack_format` 34 ≠ server 1.21.11; 3 conflicting truths | regen with correct format; reconcile docs |
| P0-R2 | P0 | Pack | Zero `.ogg` files → all sound/mood silent | source 4 mono OGGs (CC0/TTS + ffmpeg) |
| P0-C1 | P0 | Content | Companion/reckoning/co-op = dead gates (no producers) | build listeners or defer + strip refs |
| P0-C6 | P0 | Content | Seed re-run order footgun: dark rows OR leaked M4 answers | add db:seed runner enforcing 0006-first order |
| P0-D3 | P0 | DB | `world_paste_ledger` uncreated (latent, FAWE-gated) | create table before enabling FAWE |
| P1-A2 | P1 | Plugin | Beat double-fire on restart / 2 instances (no atomic claim) | `PATCH …WHERE status='approved'` claim |
| P1-A6 | P1 | Plugin | `PerPlayer` missing showEntity/packet-light/fog | build the 3 primitives (pre-Phase-3) |
| P1-A7 | P1 | Plugin | Rune font built but applied on no in-world surface | tag clue components `observance:runes` (+PUA remap) |
| P1-D4 | P1 | DB | Remote `watcher_sleep` inert (`SettingsRow.value` String vs jsonb) | `value`→`JsonElement` |
| P1-D5 | P1 | DB | Dossiers/compliance render blank in dashboard (read drift) | view/mapping to reconcile read columns |
| P1-V2 | P1 | Structures | World is a bare surface row, not a carved descent | procedural craft R&D, or hand-build + drop zero-manual |
| P1-W1 | P1 | Website | `v_record` view never migrated → record frozen | author the coarse view |
| P1-W2 | P1 | Website | "write answers into record" feature unbuilt | edge-function write path |
| P1-W3 | P1 | Website | Admin nav chrome leaks onto public `/record` | strip nav under `/record` |
| P1-C3 | P1 | Content | Live collision `the last marker is not the last` (sella+shrine) | disambiguate one owner in seed |
| P1-C4 | P1 | Content | Drip is static rank + pool only 5 ciphers → dead-air after Mvt II | salience + roster gate + widen drip pool |
| P1-C2 | P1 | Content | Finale hop may need an unbuilt arrival listener | verify finale `answer_kind`; add producer if travel |
| P1-C7 | P1 | Content | `companion-reveal` requires_flags on non-existent row | create row or strip (part of P0-C1) |
| P1-C9 | P1 | Content | `seedcheck` checks normalization only (not reach/collision/hints) | add gate+collision+coverage checks |
| P1-C10 | P1 | Content | Back-half decode nodes have ZERO hints (hard-stall) | author hint tiers for M4/Seventh ciphers |
| P2-C6 | P2 | Content | Narrow collision `a thing that can say no…` | low-pri disambiguate |
| P2-C8 | P2 | Content | `rosetta_known`/`tokens_laid` gate nothing (literacy unenforced) | add requires_flags if intended prereq |
| P1-A3 | P1 | Plugin | `RoomSwapBeat` in-place (not teleport) + needs missing .schem | rework to teleport reveal |
| P1-A4 | P1 | Plugin | Ignition trips on any nearby right-click | gate on specific block / sneak-click |
| P1-I1 | P1 | Coherence | Zero-manual vs hand-built world unreconciled | pick a lane, rewrite losing docs |
| P1-I2 | P1 | Coherence | SESSION-ZERO discloses unbuilt voice/website | align script to shipped scope |
| P1-I3 | P1 | Coherence | Companion prose has no mechanics (orphan) | = P0-C1 |
| P1-X1 | P1 | Friends-run | Minute-one findability cliff (no rosetta, no pointer) | = P0-V1 + in-world drip |
| P1-X2 | P1 | Friends-run | Sign-as-answer verb unobvious | labeled lecterns (via P0-V1) + one-time tell |
| P1-R3 | P1 | Datapack | Undercroft dimension is an empty air-void | add geometry or mark deferred |
| P2-D6 | P2 | DB | `fetchArcState` orders by updated_at not id=1 | pin `id=eq.1` |
| P2-A5 | P2 | Plugin | reload strands active fake-blocks | resend/clear illusions on reload |
| P2-R4 | P2 | Datapack | Advancement icon schema — verify 1.21.11 validity | validate on server |
| P2-R5 | P2 | Datapack | 3 biomes share identical (silent) mood | differentiate once audio lands |
| P2-C5 | P2 | Content | No in-world thread pointer (soft-pressure world-side) | record block / whisper drip |
| P2-W4 | P2 | Website | Stale "no login" copy, README, lure 404 | fix copy + README + place asset |
| P2-X4 | P2 | Friends-run | D7 "reward the datamine" unimplemented | leave xrayer messages; lean on doing-as-joy |

### Suggested sequencing (given a time-limited builder)
1. **Make tonight's slice not-broken:** P0-D1, P0-D2, P0-V1, P0-A1, P0-R1, P0-R2 (all small, all
   block a good first run).
2. **Make it cohere:** P1-I1/I2 (doc reconciliation, cheap), P1-C3, P1-D4/D5, P1-W1.
3. **Make it *amazing*:** P1-A6 (per-player illusion) → the co-op vault + Observer engine; P1-V2
   (world craft); then P0-C1 (companion) — the signature wave, explicitly deferred until the loop is
   proven with friends (OVERHAUL §6 Phase 1 is sacred).
