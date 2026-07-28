# The Observance — Private Friends Launch Checklist

Status: **current operator entry point**

The Observance is made for one private group of Brad's friends. Their first full playthrough is the
real campaign. There is no separate public release, public audience, or disposable friend-group test.
Internet-facing infrastructure may still need production-grade backups and security because it holds
the group's real state, but "production" means only the systems used by this private campaign.

Target: about six friends, free-paced across several sessions, **20–30 active hours** with 24–28
preferred. Do not use the retired 15-hour launch-night estimate or the mistaken 30–40-hour test-plan
estimate.

## Current checkpoint — 2026-07-27

- Local `main` is synchronized to the frozen launch-candidate checkpoint `e272b98`.
- Source authorities, campaign projections, deterministic simulations, Discord audit/runtime tests,
  dashboard lint/self-tests/production build, all 43 Paper plugin tasks, JAR readback, asset packaging,
  media manifest, and non-live Supabase/resource-pack safeguards pass.
- Windows exact-byte checkout drift was found and corrected for the physical predicate, campaign
  projections, binding manifests, and generated SQL.
- The currently hosted resource pack matches the generated SHA-1, and all five required external
  media sources pass live readiness checks.
- The final deploy-manifest receipt remains blocked until these checklist and byte-normalization
  changes are reviewed and committed, because the release tool correctly refuses a dirty tree.
- Current Supabase/Railway/Discord state, real Minecraft coordinates, the rehearsal clone, and all
  100 real-client matrix rows remain unverified.

The next technical checkpoint is therefore: review and commit the current preparation changes, rerun
the full audit from that clean commit with live URL checks enabled, then begin service and Minecraft
preparation one backed-up system at a time.

## The only plan

| Stage | Owner | Outcome |
| --- | --- | --- |
| 1. Freeze the release | Codex | One clean commit, one artifact manifest, all automated checks green |
| 2. Prepare real services | Brad + Codex | Supabase, Vercel, Railway/Discord, media, and resource pack use the same release |
| 3. Prepare Minecraft | Brad + Codex | A backed-up Crafty/Paper world has rehearsed coordinates, Hold, Unlit, NPCs, and clean state |
| 4. Director-only smoke test | Brad | The technical path works without asking intended players to solve or preview the ARG |
| 5. Reset and admit friends | Brad | Clean starting state, whitelist restricted to the group, fiction-first handoff |
| 6. Run and support | Brad | Observe health, record incidents, use authored hints/recovery, preserve player choices |

Do not invite intended players until stages 1–4 are complete and the reset in stage 5 is verified.

## 1. Freeze and validate the release

- [ ] Work from a clean `main` checkout at the chosen release commit.
- [ ] Install locked dependencies with `npm.cmd ci` in `dashboard/` and `discord/` if needed.
- [ ] Run the full repository audit with live hosted-media and resource-pack checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1
```

- [ ] Generate the release bundle and verify its manifest:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\package_launch_bundle.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_deploy_manifest.ps1
```

- [ ] Record the Git commit and SHA-256 hashes from `observance-deploy-manifest.json`.
- [ ] Stop if the working tree becomes unexpectedly dirty or any built/deployed artifact disagrees
  with the manifest.

Offline runs using `-SkipLiveExternalMedia` or `-SkipLiveHostedResourcePack` are useful diagnostics,
but they are not the final release receipt.

## 2. Prepare the real private-group services

These are real-state changes. Take backups first and require Brad's explicit confirmation immediately
before each deployment or database mutation.

### Supabase

- [ ] Create a restorable backup or point-in-time marker.
- [ ] Validate the current migration and security-hardening SQL against a safe target.
- [ ] Apply the receipted SQL bundle to the real campaign project.
- [ ] Confirm ARG event tables, projection leases, restricted grants, current seeds, and rollback path.
- [ ] Confirm no player-private data is exposed through anonymous/public projections.

### Vercel and Copperline

- [ ] Deploy the exact release commit used by the manifest.
- [ ] Confirm the intended Copperline domain, TLS, gated Hold download, author authentication, and coda.
- [ ] Treat the site as unlisted private-campaign infrastructure even if its URL is reachable on the
  public internet; do not advertise or index the ARG.

### Railway and Discord

- [ ] Deploy the worker and recovery cron from the same release commit.
- [ ] Restrict the bot to the private campaign guild/channels; remove unnecessary Administrator access.
- [ ] Register/read back slash commands and test `/obslink` → `/link` with a disposable identity.
- [ ] Verify one event projects exactly once, survives worker restart, and reads back on all intended
  surfaces.
- [ ] Restore the clean pre-player state afterward.

### Media and resource pack

- [ ] Verify every required media URL from outside the home network.
- [ ] Verify captions, transcript, or equivalent accessibility material.
- [ ] Verify the hosted resource-pack bytes and SHA-1 match the manifest and Paper configuration.
- [ ] Do not overwrite source media during launch preparation.

## 3. Prepare the real Minecraft campaign

Use `V5-WORLD-SETUP-AND-TESTING.md` and `runbooks/V5-LAUNCH-NIGHT-GUIDE.md` for exact commands. The
requirements below decide whether to proceed.

- [ ] Paper is `1.21.11`, Java is 21, `online-mode=true`, and exactly one
  `observance-0.5.0.jar` is installed.
- [ ] The server, all worlds, `plugins/Observance/`, server configuration, and world-manager
  configuration have dated restorable backups.
- [ ] A separate rehearsal clone exists. Destructive checks and finale rehearsals happen only there.
- [ ] Real Mouth, village well, Unlit, seven houses, base mirror, five NPC, and Wren coordinates are
  recorded; no placeholder coordinates remain.
- [ ] `/obs placehold prepare` and `/obs placehold plan` pass at the chosen Mouth before building.
- [ ] The Hold and Unlit are built and visually inspected on the clone with a real non-op client.
- [ ] After a clean stop and restart, Hold, Unlit, dialogue, resource-pack, and `/obs preflight`
  checks all pass.
- [ ] All 100 rows of `V5-LIVE-TEST-MATRIX.csv` are complete.
- [ ] Mixed-order Unlit progress and BI06 escrow survive restart; BI08 remains gated until all seven
  houses are complete.
- [ ] All finale branches are rehearsed on disposable clones. The real finale remains fresh and unarmed.

## 4. Director-only smoke test

Brad may know spoilers. Intended players must not participate.

- [ ] Start from a disposable clean identity and restored clone/state.
- [ ] Follow Copperline → gated `the-hold.zip` → reconstructed server endpoint → Orientation filing →
  private Discord invitation → `/obslink` → `/link`.
- [ ] Confirm each surface is unavailable before its prerequisite and available afterward.
- [ ] Join with a normal non-op account, accept the resource pack, traverse the Mouth, use the village
  well, and interact with every surface NPC.
- [ ] Confirm backups restore the world and cross-surface state as one matching checkpoint.
- [ ] Record failures. Fix and repeat on the clone; never explain around a broken surface.

This smoke test verifies delivery and state transitions. It does not attempt to judge the intended
players' deductions or secretly run their campaign in advance.

## 5. Reset for the real run

- [ ] Stop Paper and all test actions.
- [ ] Restore the clean pre-player world and matching Supabase/cross-surface state.
- [ ] Confirm every group/case/event table has the intended fresh state.
- [ ] Confirm all Hold gates are sealed and the finale is fresh, ready, and unarmed.
- [ ] Run the full post-restart Paper preflight block from the launch-night guide.
- [ ] Confirm Vercel, Railway, Discord, Paper, SQL, media, and resource pack name the same release.
- [ ] Restrict the Minecraft whitelist and Discord access to Brad and the actual friend group.
- [ ] Retain a stopped-server backup and service restore identifiers immediately before admission.

## 6. Give players only the beginning

- [ ] Send only the Copperline root URL and ordinary Minecraft Java setup requirements.
- [ ] Do not send the server address or Discord invitation out of band; discovering them is part of C01.
- [ ] Do not provide the casebook, author console, commands, coordinates, lore summary, or solution files.
- [ ] Let the group split, revisit evidence, pause between sessions, and progress at its own pace.

## 7. During the campaign

- [ ] Keep dated backups at session boundaries.
- [ ] Monitor service health, errors, leases, queue depth, and server state—not private player reasoning.
- [ ] Use only authored hints and record when and why each hint was given.
- [ ] Never open a gate merely because the group is slow.
- [ ] Never rebuild the Hold after live progression begins.
- [ ] For a single damaged fixture, pause and use the documented state-preserving repair path.
- [ ] For world, database, or cross-surface corruption, close the whitelist and restore one complete
  matching checkpoint rather than rolling back one surface alone.
- [ ] Record player-facing confusion and technical failures for post-campaign improvements without
  rewriting their choices mid-run.

## Final go/no-go

Admit the friends only when all statements are true:

- [ ] The clean audit, bundle, and deploy manifest pass.
- [ ] Every real service uses the same receipted release.
- [ ] Backups and a tested whole-system restore path exist.
- [ ] Real coordinates and structures are built, saved, restarted, and visually inspected.
- [ ] The complete fiction-first handoff works from a clean identity.
- [ ] Hold, Unlit, dialogue, media, resource pack, and preflight checks pass after restart.
- [ ] The real player state is clean, gates are sealed, and the finale is fresh and unarmed.

One unchecked item is a blocker. It does not mean the ARG needs a public launch process; it means the
private campaign is not yet safe to begin.

## Detailed technical references

- `V5-WORLD-SETUP-AND-TESTING.md` — world placement and full live-test requirements
- `V5-PRODUCTION-LAUNCH-RUNBOOK.md` — services, backups, incidents, and finale operations
- `runbooks/V5-LAUNCH-NIGHT-GUIDE.md` — exact operator commands and player handoff
- `V5-LIVE-TEST-MATRIX.csv` — required client/world checks
- `handoff/FINAL-WHOLE-CAMPAIGN-HUMAN-TEST-PLAN.md` — optional observation rubric during the real run
- `handoff/EXTERNAL-LAUNCH-READINESS-AUDIT-2026-07-18.json` — historical external-state snapshot, not
  proof of current service readiness
