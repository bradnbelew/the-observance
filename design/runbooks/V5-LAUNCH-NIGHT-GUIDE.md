# The Observance V5 — launch-night quick guide

Status: **current operator summary**, indexed by `design/V5-SUPERSESSION-MAP.md`

Full authorities: `design/V5-WORLD-SETUP-AND-TESTING.md`,
`design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`, and `design/V5-LIVE-TEST-MATRIX.csv`.
If this summary and a full authority ever differ, stop and follow the full authority.

## The player handoff

Do not send the server address or Discord invite out of band. They are required parts of C01.

1. Players begin at the production Copperline site root.
2. The ordinary listing, damaged service record, ticket, and community trail establish service `1842`
   and ticket `9137`.
3. After LS03, the community post at `/community/2011/02/08/world-backup` exposes the gated download.
   The file is served at `/the-hold/the-hold.zip`; it must return a generic 404 before LS03.
4. Players open `the-hold.zip` as a local Java Edition 1.21.11 world. Its records reconstruct
   `snoikerz.com:25569`; no player-facing file should print that endpoint whole.
5. On the live server, players complete the first-office Orientation filing: recover the named
   Orientation Key, place it in slot 13 of the marked dispatch barrel, and use the filing handle.
   This LS06 action privately reveals the configured Discord invitation to the present group.
6. After joining Discord, each player runs `/obslink` in Minecraft, then runs
   `/link <exact Minecraft name> 9137 <4-4-4 code>` in Discord before the five-minute code expires.

The required order is therefore Copperline → local world → live server → in-game Discord invitation →
proof-bound Discord link. A direct IP post, public invite, or pre-linked test account skips required
content and is not a valid launch rehearsal.

## Exact release files

All paths below are relative to the repository root. Use hashes from a newly generated
`observance-deploy-manifest.json`; never copy a hash from this guide or an older receipt.

| Purpose | Repository file | Production destination |
| --- | --- | --- |
| Paper plugin | `plugin/build/libs/observance-0.5.0.jar` | `<paper-root>/plugins/observance-0.5.0.jar` |
| Datapack | `observance-datapack.zip` | `<paper-root>/<surface-world>/datapacks/observance-datapack.zip` |
| Resource pack | `observance-resourcepack.zip` | Public HTTPS object named by the fresh deploy manifest; configure its exact lowercase SHA-1 and require it |
| Playable local world | `dashboard/content/the-hold-v5/the-hold.zip` | Deployed with `dashboard/`; served only through `/the-hold/the-hold.zip` |
| Local-world checksum | `dashboard/content/the-hold-v5/the-hold.sha1` | Deployed beside the archive; must match the exact zip |
| Database bundle | `discord/supabase/apply-all.sql` | Apply once through the authenticated Supabase operator path after backup |
| Vercel application | `dashboard/` | Linked project `the-observance-kjxn` |
| Discord worker/recovery services | `discord/railway.worker.json`, `discord/railway.cron.json`, and `discord/` | Two Railway services rooted at `/discord`; deploy the same Git commit used by Vercel and the release receipt |
| Release receipt | `observance-deploy-manifest.json` | Retain with the launch evidence; every byte and deployed commit must match it |

There is no current `dashboard/public/the-hold/the-hold.zip`. Do not recreate that retired path: the
current route reads the checked archive from `dashboard/content/the-hold-v5/` and gates it on LS03.

From a clean checkout, produce and verify the release as one unit:

```powershell
git status --short
powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools\package_launch_bundle.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_deploy_manifest.ps1
```

The first command must print nothing. Stop if the deploy manifest predates or disagrees with any jar,
zip, SQL bundle, Hold archive, checksum, or deployed Git commit.

## Back up before placement

With Paper stopped, make a dated, restorable copy of:

- the surface world, Nether, End, and every custom world;
- `plugins/Observance/`, the selected plugin jar, server configuration, and world-manager configuration;
- the production Supabase project or point-in-time restore marker;
- the Vercel and Railway worker/cron production deployment IDs and Git commit.

Make a second complete server copy as the rehearsal clone. Build, destructive tests, outage tests, and
finale rehearsals happen there first. Never test `/obs finale arm` on production.

## Coordinates are not included

The release contains authored structures and content, not safe coordinates for an unseen server.
If the production world is new, empty terrain, or still contains placeholder/default coordinates, stop.
Launching there will give players ordinary terrain, missing NPCs, or unreachable structures.

Before building, record the exact surface-world folder name, Mouth X/Y/Z, +Z direction, village-well
entry, Unlit spawn/exit, seven evidence-house anchors, base-mirror anchor, five surface NPC anchors, and
Wren marker. Never silently assume the world is named `world`.

At the proposed Mouth on the rehearsal clone:

```text
/obs placehold prepare
/obs placehold plan
```

Wait for `PREPARE PASS`, then `PLAN PASS`. Preparation can take several minutes in unexplored terrain;
do not disconnect the issuing operator. Inspect the complete printed envelope in spectator mode. The
full Hold extends in world +Z and needs roughly X `-118..118`, Y `-104..12`, and Z `-6..378` from its
local origin plus the printed safety margin. Reject any location that touches the well, a base, portal,
NPC, protected region, another authored site, the world floor, or exposed surface.

Only after the plan passes on the clone:

```text
/obs placehold build
/obs placehold audit
/obs preflight
```

Do not interrupt the build. After live progression begins, never run `placehold build` again.

The Unlit is a stopped-server copy of the real surface world named `observance_unlit`. Remove only the
copy's `uid.dat`, load it with the rehearsed world manager, and bind:

- `entry` at the real surface-well interaction cell;
- `spawn` and `exit` at the mirrored well in `observance_unlit`;
- Lamp, Cairn, Coop, Well, Watch, Warm, and Threshold as seven independent house sites;
- Base as the separate base-mirror synthesis site.

The seven houses must all be available on arrival. Their BI numbers are filing labels, not an unlock
order. BI08 must remain unavailable until all seven receipts exist.

## Live Paper rehearsal

Use Paper 1.21.11, Java 21, `online-mode=true`, exactly one Observance 0.5.0 jar, and the exact release
datapack and required resource pack. Test with a non-op Adventure account using a real client.

On first start:

```text
/datapack list enabled
/data get storage observance:runtime version
/obs status
/obs sleep on
```

The storage version must be `5`. Then stop and restart Paper and, at the persisted Mouth, run:

```text
/obs placehold prepare
```

Wait for `PREPARE PASS`, then run:

```text
/obs reload
/obs status
/obs placehold audit
/obs placehold sync
/obs unlit audit
/obs unlit ready
/obs dialogueaudit
/obs preflight
/obs finale status
```

All checks must pass after restart. All gates begin sealed; the Mouth and Grand Stair remain usable;
the finale is fresh/ready and never armed, committed, Coda, or fault.

### Required C01 smoke test

From a clean test identity, follow the complete player handoff at the top of this guide. Confirm the
world download is unavailable before LS03, the local world opens, the reconstructed endpoint joins the
live server, LS06 reveals the Discord invite only after the filing, `/obslink` is private, and `/link`
fails before LS06 but succeeds once afterward. Restore the clean pre-player checkpoint after testing.

### Required Unlit restart test

Use disposable restored clones; do not fake these flags with operator commands.

1. Enter only through the village well.
2. Start the seven houses in a deliberately mixed order, for example BI06 → BI02 → BI05.
3. At BI06, file the exact three dated samples, disconnect the acting player as the completion commits,
   and restart Paper.
4. Rejoin and confirm BI06 remains complete, its sample set remains in protected escrow without loss or
   duplication, the other solved receipts persist, every unsolved house is still independently
   available, and `/obs unlit audit` plus `/obs preflight` pass.
5. Attempt BI08 early and confirm it remains unavailable without consuming evidence or leaking the
   conclusion.
6. Finish the remaining houses in a non-numeric order, restart again, and confirm BI08 becomes available
   exactly once after the seventh receipt.
7. On a second restored clone, use a different first and last house. Test retreat from all seven houses
   and the base mirror, then return through the same well.

If the runtime forces BI01 → BI02 → BI03 or loses/duplicates BI06 samples across restart, do not launch.

Complete all 100 rows in `design/V5-LIVE-TEST-MATRIX.csv`, including a six-player group plus one
operator/client crowd pass. Inspect every book, sign, item, holder, NPC, corridor, return route, and
resource-pack surface with the real client.

## Six-player time budget

This is about 15 active play hours, excluding setup, long breaks, or a serious technical fault.

| Case | Time |
| --- | ---: |
| C01 — The Lost Server | 1 h |
| C02 — The Long Cold | 1 h |
| C03 — Keeper Dossiers | 2.5 h |
| C04 — Cistern Winter | 1.5 h |
| C05 — Break Inquest / the Unlit | 2 h |
| C06 — Restoring the Hold | 1.25 h |
| C07 — ASH-13 Company | 2.25 h |
| C08 — Wren Betrayal | 1 h |
| C09 — Averyn / the Unwriting | 1.25 h |
| C10 — Release | 1.25 h |
| **Total** | **15 h** |

C03, C05, and C07 can be divided among six players; discussion may add time. Plan multiple sessions or
a deliberate break rather than rushing the evidence.

## Go live

Two hours before players arrive:

1. Confirm Vercel, both Railway services, Paper, SQL, and all artifacts name the same Git commit and hashes.
2. Back up Supabase and the stopped production server again.
3. Start Paper, wait for `PREPARE PASS` at the persisted Mouth, and rerun the full post-restart command
   block above.
4. Join as a non-op, accept the pack, enter and leave the Mouth, use the well, speak to every surface
   NPC, and perform the designated non-story health check.
5. Verify Copperline, the gated Hold archive, Discord worker/lease, Supabase, media, DNS, TLS, resource
   pack, and server endpoint from outside the host network.

Fifteen minutes before:

1. Stop tests and restore the clean pre-player state.
2. Confirm G1–G6 and internal gates are sealed and the finale remains fresh/ready.
3. Run `/obs sleep off`.
4. Open the whitelist only to the six players.

During play, use authored hints and documented recovery only. Never open a gate because the group is
slow, publish a skipped answer, rerun the Hold build, or improvise a state flag.

## Rollback and recovery

For one damaged fixture, pause play and use state-preserving repair only after preparation passes:

```text
/obs placehold prepare
/obs placehold repair <fixture-id>
/obs placehold audit
```

For a damaged corridor, gate, shell, world, local progress store, or database, close the whitelist and
restore the last compatible checkpoint. Restore the matching world folders, `plugins/Observance/`,
plugin/datapack/resource-pack release, Supabase snapshot, and Vercel/Railway commit as one set. Never roll
back only one surface while leaving later progress live elsewhere.

If Supabase or a cross-surface service fails, existing open gates remain open; pause new cross-surface
submissions until durable writes are verified. If the finale is armed incorrectly, `/obs finale cancel`
is safe only before its displayed cutoff. Never delete a corrupt finale-state file to force a fresh run.

Detailed incident choices are in `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md` §12. Fixture and artifact
recovery commands are in `design/V5-WORLD-SETUP-AND-TESTING.md` §11.

## Final go/no-go

Do not admit players unless every statement is true:

- the clean release gate and fresh deploy manifest pass;
- backups and matching rollback identifiers exist;
- production coordinates are real, saved, and visually inspected—not defaults or empty terrain;
- the full Copperline-to-link handoff passes from a clean identity;
- Hold, Unlit, dialogue, resource-pack, and preflight checks pass after restart;
- seven Unlit houses work in mixed order, BI06 escrow survives restart, and BI08 requires all seven;
- all 100 live-test rows pass and the final stopped-server backup exists;
- gates are initially sealed, the finale is fresh, and ambient automation is intentionally enabled.

One unchecked item is a launch blocker.
