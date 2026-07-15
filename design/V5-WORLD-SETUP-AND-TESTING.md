# The Observance V5 — world setup and testing

Status: **the Minecraft operator authority**
Target: **Paper 1.21.11 / Java 21 / Observance 0.5.0**
Story authority: `arc/WORLD-BIBLE.md`
Physical plan: `design/visuals/deep-hold-v5-blueprint.png`

This procedure starts with a real existing village world and ends with a rehearsed production copy.
The plugin builds the Deep Hold. Do not hand-build, rotate, paste, or improvise any Hold room. The only
world-specific work is selecting safe coordinates, copying/loading the Unlit world, positioning five
surface NPCs, and proving the result with a real client.

No document can choose real coordinates or certify client rendering without the actual production
world. Those facts are intentionally recorded as launch evidence instead of being guessed in source.

Version receipts: Mojang's official 1.21.11 notes specify data-pack version `94.1` and resource-pack
version `75.0`; Paper's official setup matrix specifies Java 21 for Paper 1.20 through 1.21.11.

## 1. Before touching the world

1. Stop Paper cleanly.
2. Copy the overworld, Nether, End, every custom world, and `plugins/Observance/` to a dated backup.
3. Back up the production Supabase project before applying V5 SQL.
4. Make a second complete server copy called the **rehearsal clone**. All builds, destructive tests,
   and finale branches happen there first.
5. Record the current world seed, Paper build, Java version, plugin list, and world-folder names.
6. Never test `/obs finale arm` on the production world.

Keep players whitelisted out until Section 13 passes. Use a console plus one operator account and one
non-op Adventure-mode account. Seven-client crowd testing is a launch blocker, not an optional polish
pass.

## 2. Build the selected release

From the repository root on Windows:

```powershell
python tools/check_v5_content.py --runtime
python tools/simulate_v5_scenarios.py

cd plugin
.\gradlew.bat clean check build --no-daemon
cd ..\discord
npm.cmd ci
npm.cmd run db:seed
npm.cmd run typecheck
npm.cmd run audit
npm.cmd run runtimecheck
cd ..\dashboard
npm.cmd ci
npm.cmd run lint
npm.cmd run selftest
npm.cmd run build
```

Expected plugin artifact:

```text
plugin/build/libs/observance-0.5.0.jar
```

There must be exactly one `observance-*.jar` in both `plugin/build/libs/` and the live server's
`plugins/` directory. Do not rename an old jar to look current. Build the final release with
`tools/package_launch_bundle.ps1`, validate `observance-deploy-manifest.json`, and record its SHA-256
values for the plugin, datapack, resource-pack zip, SQL bundle, gated Hold archive, and deployed Git
commit in the launch evidence.

## 3. Apply the V5 database contract

The generated authority is:

```text
discord/supabase/apply-all.sql
```

Run `npm.cmd run db:seed` immediately before applying it; never hand-edit the bundle. Apply it once in
the Supabase SQL editor or an authenticated migration job, then verify:

- 10 investigations;
- exactly 82 investigation nodes;
- 5 required media records represented by the fixed asset manifest;
- no current optional-side rows;
- legacy rows explicitly retired rather than still discoverable;
- existing accounts, Discord links, consent, attempts, and operational history retained;
- service-role access works from the plugin and Discord worker;
- anonymous website access is limited to safe projections.

Do not paste a Supabase service-role key into Discord, a screenshot, the website, or Git. The Paper
host receives it through `OBSERVANCE_SUPABASE_KEY`; Railway receives it as a secret environment value;
Vercel receives only the server-side secret plus the public anon key required by the dashboard.

## 4. Install Paper and the release artifacts

Use the exact Paper 1.21.11 server build selected in the release receipt and Java 21. Install:

- `observance-0.5.0.jar` in `plugins/`;
- `observance-datapack.zip` in the surface world's `datapacks/` folder;
- the current Observance resource pack at its public HTTPS URL;
- Multiverse-Core only if it is the chosen mechanism for keeping `observance_unlit` loaded;
- Citizens only if the human-looking NPC path has been rehearsed on this exact Paper build.

WorldEdit/FAWE, ModelEngine, Citizens, and voice plugins are optional enhancements. The ARG must pass
without them; the Observance plugin owns a deterministic vanilla fallback. Do not add an optional
plugin after rehearsal without repeating the affected tests.

Start once to create `plugins/Observance/`, stop, and configure the live copy:

```text
OBSERVANCE_SUPABASE_KEY=<service role key in the host environment>
```

In `plugins/Observance/config.yml` verify the public Supabase URL, service-key environment name,
resource-pack URL, exact lowercase SHA-1, required-pack policy, production finale policy, and V5 mode.
Never copy a test finale-state file into production.

Start Paper and run:

```text
/datapack list enabled
/data get storage observance:runtime version
/obs status
/obs sleep on
```

The V5 storage version must be `5`. `sleep on` keeps ambient/story automation quiet while placing.
Database health, version, resource-pack
configuration, and V5 authority validation must be visible. A missing database is acceptable only for
isolated geometry work; it is not acceptable for launch rehearsal.

## 5. Preserve the village-well entrance to the Unlit

The village well is the only public Unlit entrance. The Deep Hold Mouth is a different route. Do not
put a Hold stair under the well and do not add a second Unlit portal.

Create `observance_unlit` from a stopped-server copy of the surface world:

1. Stop Paper.
2. Copy the surface world folder to `observance_unlit`.
3. Remove only `observance_unlit/uid.dat` from the copy.
4. Start Paper.
5. Load the copy with the rehearsed world manager; with Multiverse-Core the usual import is:

```text
/mv import observance_unlit NORMAL
```

Confirm `/mvtp observance_unlit` reaches the copy and `/mvtp <surface-world>` returns. If the live
world has a custom name, retain that name everywhere; never silently assume `world`.

Author the expedition:

```text
/obs unlit buildmode on
```

At the real surface-well interaction cell:

```text
/obs unlit site entry
```

In `observance_unlit`, at the exact mirrored-well arrival and return cells:

```text
/obs unlit site spawn
/obs unlit site exit
```

Choose seven different readable village houses plus one clear base-mirror location. Stand where a
player should inspect each clue and run:

```text
/obs unlit clue lamp
/obs unlit clue cairn
/obs unlit clue coop
/obs unlit clue well
/obs unlit clue watch
/obs unlit clue warm
/obs unlit clue threshold
/obs unlit clue base
```

Finish the authored area:

```text
/obs unlit border 138
/obs unlit darken all 138
/obs unlit buildmode off
/obs unlit audit
/obs unlit ready
```

The audit must find entry, spawn, exit, seven distinct evidence houses, the base mirror, protected
approach cells, safe retreat, no unauthorized light, and no dependency on a second entrance. Across
restored clones, complete BI01–BI07 in at least two different orders and verify BI08 remains unavailable
until all seven receipts exist. Then complete BI08 and retreat safely from every location before
accepting the Unlit.

## 6. Place the five surface NPCs

Use normal village locations that support what each person does. Do not place them in a test row or
inside the Hold.

| NPC | Place near | Must not block |
| --- | --- | --- |
| Aro | inn, kitchen, or social center | a door, bed, or village path |
| Wenna | garden, reeds, or water route | the village well interaction cell |
| Coll | market or store | trading access and the Mouth lead route |
| Dob | masonry yard, kiln, or drainage cut | the Mouth approach |
| Old Pell | pump, bench, or cistern memory site | the well and house doors |

Stand at each final feet location, face the intended player approach, and run one command at a time:

```text
/obs townsfolk spawn aro
/obs townsfolk spawn wenna
/obs townsfolk spawn coll
/obs townsfolk spawn dob
/obs townsfolk spawn old-pell
```

A repeated spawn must relocate the one existing body, never clone it. Right-click each NPC as a non-op
at arrival, after C02/C04/C05/C07/C09 where applicable, and in every Coda treatment. Their exact text
comes from `arc/v5/npc-dialogue.json`, speaks locally without a showrunner wait, and must refer to a real
nearby well, kiln, garden, market, or route.

Wren belongs at the declared lower-Hold reckoning/confrontation site. The Hold build places the
physical chamber; after audit, stand at Wren's exact feet marker and run:

```text
/obs wren spawn
```

Test confession, condemn, understand, free, and all three Coda states on restored clones.

## 7. Choose the Deep Hold Mouth

V5 supports exactly one orientation: world **+Z**. Increasing Z is the direction the Hold extends.
Player yaw is ignored.

The full protected envelope is derived from the Mouth and printed by the plan command. Its authored
local extrema are approximately X `-118..118`, Y `-104..12`, and Z `-6..378`, with an additional
safety envelope. The command's world-space output—not mental arithmetic—is authoritative.

Choose a surface location where:

- the visible Mouth can plausibly be the drainage cut described by Dob and Aro;
- +Z points away from the village well and other builds;
- the entire roof remains buried with at least the required cover;
- the foundation stays above the world minimum with the required bottom buffer;
- no portal, protected region, player base, NPC, Unlit cell, or other ARG site touches the envelope;
- the approach is safe and return to the ordinary village is obvious.

Stand on the exact Mouth origin and generate/load the complete footprint asynchronously before the
read-only survey. Preparation changes no Hold blocks or site registrations; it keeps the exact
footprint chunks ticketed so neither plan nor build can hide synchronous chunk generation:

```text
/obs placehold prepare
```

Wait for `PREPARE PASS`, then run at the same block:

```text
/obs placehold plan
```

Console forms:

```text
/obs placehold prepare <surface-world> <mouth-x> <mouth-y> <mouth-z>
/obs placehold plan <surface-world> <mouth-x> <mouth-y> <mouth-z>
```

Preparation may take several minutes in unexplored terrain. Do not disconnect the operator/console
that issued it until `PREPARE PASS` is visible. Record the complete plan output; it must end
`PLAN PASS`. Inspect all printed bounds in spectator mode and take boundary screenshots. If anything
is doubtful, choose another site and repeat **prepare, then plan** at the new Mouth. Plan is read-only.
Any rotation argument or failed survey must change nothing.

## 8. Build the Deep Hold

Use a no-player maintenance window on the rehearsal clone. Keep the console visible.

Player form, after `PREPARE PASS` and the passing plan at the exact same Mouth in the same server
session:

```text
/obs placehold build
```

Console form:

```text
/obs placehold build <surface-world> <mouth-x> <mouth-y> <mouth-z> +z
```

Do not interrupt the build. Success requires a verified receipt, 32 rooms, 76 fixtures, eight sealed
gates, current V5 content hash, and successful atomic `sites.yml` persistence. Blocks without a receipt
are a failed build.

Then run:

```text
/obs placehold audit
/obs preflight
```

Stop and restart Paper, then run:

```text
/obs placehold prepare
```

Wait for `PREPARE PASS` at the exact persisted Mouth, then run:

```text
/obs reload
/obs status
/obs placehold audit
/obs placehold sync
/obs unlit audit
/obs dialogueaudit
/obs preflight
```

All gates begin sealed. The Mouth and Grand Stair remain accessible. Never use `placehold build` again
after live progression starts; use state-preserving fixture repair.

## 9. Inspect every physical surface

Use a non-op Adventure account with the production resource pack. Follow every row of
`design/V5-LIVE-TEST-MATRIX.csv`; all 100 rows are release blockers.

At minimum, prove:

- Mouth-to-Coda and Coda-to-Mouth traversal with two blocks of headroom and solid floor;
- every room doorway, side loop, stair, gate volume, standing cell, and backtrack route;
- seven-player crowding without traps or inaccessible interactions;
- all 44 books: title, author, order, page wrapping, glyphs, and last line;
- every sign front, backing, four-line fit, and non-input decorative treatment;
- every lectern orientation and theft protection;
- exact bookshelf slots, frame facings/rotations, container slots, and PDC items;
- no sound-only, texture-only, precision movement, randomness, real-log, or operator intervention gate;
- no blocked corridor, wall-clipped entity, reversed sign, empty required fixture, or generic sign room.

Take one overview, one approach, one standing-cell, and one interaction screenshot per critical
fixture. Keep an uncut route video for every stratum and the Unlit.

## 10. Progression and answer rehearsal

On disposable database/world checkpoints, exercise all 82 nodes in manifest order. For every input:

1. try it before prerequisites;
2. try a wrong answer or wrong item;
3. try a partial/near answer;
4. submit the exact answer/action;
5. repeat it;
6. submit concurrently from 2, 4, and 7 players;
7. disconnect and reconnect;
8. restart Paper;
9. temporarily remove Supabase access;
10. restore access and synchronize.

The correct solve happens once. Wrong deposits return. Guessed future answers are neutral. An opened
gate never closes. The exact gate sequence is:

For LS05 specifically, first confirm `server.properties` has `online-mode=true`; `/obs preflight`
must fail closed when it does not. On a disposable offline-mode clone, `/obslink` must generate no
code, make no challenge RPC, and create no database row. Restore online mode and have the exact
authenticated non-op Minecraft player run `/obslink`. Prove the returned
4-4-4 code expires after five minutes, cannot be replaced inside the 30-second issue cooldown, and
exists in Supabase only as SHA-256. An invalid callback, a valid callback before LS06 (including after
LS04 but before the Orientation filing), and a wrong,
expired, consumed, or other player's proof must leave `players.discord_id` unchanged and fail
privately. Then prove valid `/link <name> <callback> <code>`, exact replay, accidental-name correction,
and a second account's conflict are atomic and spoiler-safe. Keep both old and new identities online
during recovery: Paper must recognize the new link and revoke the old link on the next five-second
authoritative refresh (plus database latency) without reconnect or main-thread network I/O. During a
simulated Supabase outage, the last-known-good link must remain unchanged.

| Gate | Opens only when |
| --- | --- |
| G1 | C02 complete |
| G2 | all six sealed affidavits earned |
| G3 | C04 complete, BI01–BI07 complete in any order, and BI08 complete |
| G4 | C06 complete |
| Camp Ash | A01 complete |
| Dread | C07 complete |
| G5 | C08 complete with a durable Wren choice |
| G6 | C09 complete and AVERYN assembled |

After each gate: leave through the same Mouth, restart, re-enter through the Mouth, and confirm the
gate remains open while later gates remain sealed.

## 11. Artifact loss and safe repair

The exact 21 critical items are in `design/ARG-V5-ARTIFACT-MANIFEST.csv`. Recover only an earned item:

```text
/obs item recover <artifact-id> <online-player>
```

For every item, test player inventory, ender chest, loaded container, loaded dropped entity, missing
item, unearned flag, and full inventory. Load relevant chunks before recovery. The command must never
displace an item, drop a replacement, issue an unearned item, or duplicate an existing copy.

Fixture repair:

```text
/obs placehold prepare
/obs placehold repair <fixture-id>
/obs placehold audit
```

Whole-fixture repair on a clone:

```text
/obs placehold prepare
/obs placehold repair all
/obs placehold audit
```

Wait for `PREPARE PASS` before either repair. Repair preserves inventory custody, solved mechanics, artifact consumption, and every gate latch.
Restore the world backup instead of improvising if the shell or a broad region was altered.

## 12. Rehearse all endings

Rehearse all 24 rendered outcomes: three Wren outcomes times two name treatments times four conduct
verdicts (`solo`, `unanimous`, `divided`, and `persistent`). A restored clone may be reused only after
returning its database and local finale files to the same pre-choice backup. The website/database name
treatment and the intended conduct evidence must already be durable before arming the local theater.

Inspect first:

```text
/obs finale status
/obs finale markers
/obs placehold audit
```

On each restored clone, complete WR05, RP03, and RP04 with the one intended player-choice
combination, verify those receipts in `/obs finale status`, then arm the already-recorded result
with a visible cancellation window:

```text
/obs finale arm 15
```

The arm command deliberately accepts no branch argument. It must refuse if the Wren outcome, name
treatment, conduct verdict, or C10 prerequisites are absent or contradictory; an operator can never
substitute a preferred ending for what the players actually did.

Cancel rehearsal:

```text
/obs finale cancel
```

For every combination prove: durable choice before theater, district light fall, readable Averyn
goodbye, matching Discord/site Coda, complete world/player save, all players kicked, Paper shutdown,
branch-specific Coda after host restart, and refusal to fire twice. A corrupt finale-state file must
boot fault mode, never a fresh finale.

## 13. Final production acceptance

After the rehearsal clone passes:

1. Restore production from its clean pre-V5 backup.
2. Repeat Sections 3–8 using the accepted coordinates and exact release artifacts.
3. Repeat all non-destructive rows of the 100-test matrix on production.
4. Leave destructive, outage, artifact-loss, and finale tests evidenced by the byte-identical clone.
5. Back up production again after placement and after the final post-restart preflight.
6. Record current hashes, coordinates, screenshots, service URLs, SQL receipt, Paper log, and Git commit.
7. Run `/obs sleep off` only after the whitelist, Discord, website, media, NPC, database, pack, and
   world checks all pass.
8. Admit the real players. Do not use operator solve/flag commands during play except the documented
   recovery path for a proven technical failure.

The production world is ready only when there are zero unfilled evidence fields and zero failed rows
in `design/V5-LIVE-TEST-MATRIX.csv`.
