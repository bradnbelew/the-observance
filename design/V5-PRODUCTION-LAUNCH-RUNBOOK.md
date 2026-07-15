# The Observance V5 — production launch runbook

Status: **single service/deployment authority**
World procedure: `design/V5-WORLD-SETUP-AND-TESTING.md`
Live test cases: `design/V5-LIVE-TEST-MATRIX.csv`

This runbook separates code-complete from live-ready. A green repository cannot prove an unseen
Minecraft world, private credentials, Discord permissions, DNS, external media, or real-client
rendering. Each live fact receives a dated receipt. Blank evidence means **do not launch**.

## 1. Release identity

Fill this from the final clean build:

```text
Git commit:                     ________________________________
Git branch deployed:            main
Paper version/build:            ________________________________
Java version:                   ________________________________
Observance plugin:              observance-0.5.0.jar
Plugin SHA-256:                 ________________________________
Datapack SHA-256:               ________________________________
Resource-pack SHA-1:            ________________________________
Resource-pack public URL:       ________________________________
SQL bundle SHA-256:             ________________________________
Gated Hold archive SHA-256:     ________________________________
V5 authority/content hash:      ________________________________
Vercel production deployment:   ________________________________
Railway worker deployment:      ________________________________
Railway cron deployment:        ________________________________
Supabase backup/SQL receipt:    ________________________________
World backup:                   ________________________________
Rehearsal packet:               ________________________________
```

All receipts must describe the same Git commit and artifact bytes.

## 2. Clean release gate

From the final clean checkout at the repository root:

```powershell
git status --short
powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools\package_launch_bundle.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_deploy_manifest.ps1
```

The first `git status --short` must print nothing. Release fails on any warning promoted by these checks, more than one plugin jar, a stale Hold archive,
an old canon claim on a current surface, a missing V5 authority resource, or an uncommitted generated
artifact. Read plugin, datapack, resource-pack, SQL, and gated-archive hashes from
`observance-deploy-manifest.json`; do not copy them from prose. Run the same commands from a clean
checkout before deployment.

## 3. Secrets and access

Rotate exposed or reused credentials before launch. Never commit them.

### Paper host

```text
OBSERVANCE_SUPABASE_KEY
```

### Discord/Railway

```text
DISCORD_BOT_TOKEN
DISCORD_APP_ID
DISCORD_GUILD_ID
CHANNEL_THE_RECORD
SUPABASE_URL
SUPABASE_SERVICE_ROLE_KEY
OBSERVANCE_CAMPAIGN_VERSION=v5
SHOWRUNNER_TICK_MS=12000
SHOWRUNNER_LEASE_SECONDS=300
```

Voice/LLM variables are optional and must fail to silence. No required solve may depend on them.

### Vercel

```text
NEXT_PUBLIC_SUPABASE_URL
NEXT_PUBLIC_SUPABASE_ANON_KEY
SUPABASE_SERVICE_ROLE_KEY
ADMIN_EMAILS
AUTHOR_USERNAME
AUTHOR_PASSWORD
```

Use the exact final `dashboard/.env.example` as the naming authority. Preview and production values
must be set deliberately. The service-role key is server-side only; it must never be prefixed
`NEXT_PUBLIC_`.

## 4. Supabase release

1. Create a production backup or point-in-time restore marker.
2. From `discord/`, run:

```powershell
npm.cmd run db:seed
npm.cmd run db:bundlecheck
```

3. Hash `discord/supabase/apply-all.sql`.
4. Apply that exact file through an authenticated operator path.
5. Run the post-apply verification query included by the migration/checker.
6. Confirm 10 cases, 82 nodes, required media, evidence RPC, V5 `phase_key`, public projections, and
   explicit legacy retirement.
7. Confirm existing player identities and historical operational rows remain.
8. Test with three credentials: anonymous browser, authenticated admin, and service role.
9. Save the SQL job ID/time, row-count output, and backup identifier.

Rollback is restoring the database backup plus the matching pre-V5 application/world release. Never
partially roll back only one surface.

## 5. Vercel deployment

The linked project is `the-observance-kjxn`. Build locally first. From the linked repository directory:

```powershell
cmd /c vercel --prod
```

After deployment, verify the production alias rather than only the generated preview URL:

- `/` and every Copperline C01 route;
- `/community/2011/02/08/world-backup` and its current archive receipt;
- `/support/ticket.php?id=9137` and a non-9137 id returning the missing-ticket shell;
- `/record/the-record-keeps`;
- `/record/the-record`;
- `/record/archive` before and after a media prerequisite;
- `/record/terminal` as a read-only docket, and `POST /record/terminal/inscribe` returning permanent,
  non-cacheable HTTP `410` without mutating progression;
- `/status` without secrets/spoilers;
- `/author` denied while logged out and correct for the configured admin;
- all six Coda projections on restored database checkpoints;
- mobile layout, HTTPS, noindex headers on Record surfaces, and no console/server error.

Inspect deployed JavaScript, HTML, route payloads, and network responses for service-role keys, answers,
future titles, private player data, or finale branches. Save the deployment ID, Git SHA, production URL,
route results, and screenshots.

## 6. Railway and Discord deployment

Create two Railway services from the repository and deploy the same commit as Vercel. Set the root
directory of both services to `/discord`. Set the worker config-file path to
`/discord/railway.worker.json`; it uses Railpack, runs `npm start`, and restarts `ON_FAILURE` up to 10
times. Set the recovery service config-file path to `/discord/railway.cron.json`; it uses Railpack,
runs `npm run showrunner` on `*/10 * * * *`, and has restart policy `NEVER`.

1. Set every variable from `discord/.env.example` on both Railway services. Keep secrets in Railway.
2. Confirm Node 22.
3. Deploy `observance-discord`.
4. Deploy/enable `observance-showrunner` cron.
5. Run command registration once if startup did not complete it:

```powershell
cd discord
npm.cmd run register
```

6. Enable Discord Message Content Intent. Enable Guild Voice States only if the optional voice path is
   intentionally deployed.
7. Confirm the persistent tick stays between 10 and 15 seconds.
8. Confirm the cron and worker contend for the same lease and never double-deliver.
9. Stop the worker for more than ten minutes on a test state; verify cron recovery; restore worker.
10. Confirm `server.properties` has `online-mode=true` and `/obs preflight` treats offline mode as a
    blocker. On a disposable offline-mode clone, verify `/obslink` generates and transmits nothing.
    Restore online mode, then as a non-op player run `/obslink`; confirm a 4-4-4 code appears only to that player, expires after
    five minutes, cannot be replaced within 30 seconds, and plaintext is absent from Supabase/logs.
    Test `/link <name> <callback> <code>` before LS06 (including after LS04), with a bad callback, wrong/expired/reused/other-
    player code, valid first claim, exact replay, accidental-name recovery, and a privately claimed-
    name conflict. Every rejected path must leave identity rows unchanged and use the same private
    proof response where account existence could leak. Keep both Minecraft clients online during a
    recovery: Paper must recognize the new link and revoke the old link on the next five-second
    authoritative refresh (plus database latency) without reconnect; a simulated outage must revoke
    neither. Then test scoped/unscoped
    `/answer`, autocomplete, `/whisper`,
    passive Record-channel input, seven-user duplicate submission, and spoiler-safe future guesses.

Save both Railway deploy IDs, worker/cron health, Discord command screenshots, and a database event receipt.

## 7. External media

`arc/v5/media-manifest.json` is the exact source. All five assets are required:

| Node | Expected payload |
| --- | --- |
| KB01 | `STAY AWAKE` |
| CW06 | `WHERE THE REEDS FOLD BACK` |
| A07/A08 | `ASH-13` |
| AR01 | `I WAS NOT KEPT` |
| RP01 | `SIX RETURN, ONE IS NOT KEPT` |

From a signed-out browser and a normal player connection:

1. verify every configured HTTPS source opens;
2. verify expected source size/SHA-1 receipts where the host allows byte retrieval;
3. verify every prerequisite-gated website route is closed early and opens automatically when earned;
4. play the actual video/audio, not only its landing page;
5. verify captions/visual redundancy make every payload solvable without sound alone;
6. verify no asset is called optional, temporary, placeholder, or director-triggered;
7. keep an offline operator recovery copy that is never exposed early to players.

A dead, private, region-blocked, altered, or login-only media source blocks launch.

## 8. Minecraft production placement

Follow `design/V5-WORLD-SETUP-AND-TESTING.md` exactly. Required production evidence includes:

- stopped-server pre-placement backup;
- one current plugin jar and Paper/Java startup log;
- V5 datapack marker version `5` with no retired custom dimension or story state;
- resource-pack acceptance and hash receipt;
- village-well Unlit copy plus 11 anchor coordinates (entry, spawn, exit, seven evidence houses, base mirror);
- five surface NPC coordinates/facing and Wren's Hold marker;
- accepted Mouth plan transcript, world-space bounds, +Z direction, and conflict screenshots;
- Deep Hold build receipt and authority hash;
- post-restart Hold/Unlit/dialogue/preflight pass;
- 100/100 live-test rows passed;
- six ending packets from byte-identical restored clones;
- final clean production backup.

Do not count a source-level geometry test as proof of a live world. Do not count an operator flythrough
as proof of non-op Adventure traversal.

## 9. Session-zero player setup

Before Day One:

- whitelist only the intended group;
- preserve the correct survival inventories and bases;
- set ordinary server rules and consent boundaries without revealing case answers;
- confirm all players can join Paper 1.21.11 and accept the pack;
- link Discord identities without opening future cases;
- test accessibility fallbacks and time-zone expectations;
- tell players whom to contact for technical lockout without teaching them to ask for hints in normal
  admin channels;
- ensure the operator can reach host console if the plugin intentionally shuts Paper down.

No player receives the solution casebook, author console, service credentials, private media recovery
copy, test commands, or production coordinates beyond what the fiction reveals.

## 10. Launch-day order

### Two hours before

1. Confirm the final commit is still `main` and all deployed services report that commit.
2. Confirm no new Vercel/Railway deploy is pending.
3. Back up Supabase and the stopped Minecraft server.
4. Start Paper; capture the exact startup log.
5. At the exact persisted Mouth run `/obs placehold prepare` (or use the console form with its saved
   world/X/Y/Z), and wait for `PREPARE PASS`.
6. Run:

```text
/obs reload
/obs status
/obs placehold audit
/obs unlit audit
/obs unlit ready
/obs dialogueaudit
/obs preflight
/obs finale status
```

7. Join as a non-op, accept the pack, enter/leave the Mouth, use the well, right-click every surface
   NPC, and submit one designated non-story health check.
8. Confirm Vercel, Railway, Discord, Supabase, media, DNS, and TLS externally.

### Fifteen minutes before

1. Stop all testing and restore any test-only state.
2. Verify G1–G6 and internal gates are in their initial sealed state.
3. Verify finale phase is fresh/ready, never armed, committed, Coda, or fault.
4. Run `/obs sleep off`.
5. Open the whitelist only to the real group.

### During play

- Watch health, errors, queue depth, and service leases—not player answers or private deliberation.
- Never manually open a gate because players seem slow.
- Use authored hint tiers, not improvised explanations.
- On a technical loss, prove completion/custody and use exact artifact recovery.
- On a damaged fixture, use targeted state-preserving repair during a pause.
- On a broad world or database fault, close the whitelist and restore the last compatible checkpoint.
- Never rerun the Hold build.

## 11. Finale operation

The player choices must already be durable. The operator verifies status and explicitly arms the
recorded outcome only when RP01–RP04 are complete:

```text
/obs finale status
/obs finale markers
/obs finale arm 15
```

No branch argument is accepted: Wren outcome, name treatment, and conduct verdict are read from the
players' durable receipts. Any missing, multiple, or contradictory choice keeps the finale closed.

The cancellation window is the last safe intervention:

```text
/obs finale cancel
```

After commit, do not kill the process. The plugin persists the ending before theater, saves players
and worlds, gives the branch-specific Averyn goodbye, kicks everyone, and cleanly shuts Paper down.
The host must permit application-initiated shutdown.

Confirm matching Coda state reaches Supabase, Discord, and Vercel. External delay must not stall local
Minecraft theater. Restart Paper only after the shutdown completes and the world backup is safe. The
plugin must boot branch-specific terminal Coda and refuse a second finale.

## 12. Incident decisions

| Failure | Safe response |
| --- | --- |
| Vercel or media unavailable before its node | pause before the handoff; do not reveal the answer |
| Discord worker down | keep Minecraft safe; restore worker/cron and verify lease before resuming |
| Supabase unavailable | opened gates remain open; pause new cross-surface solves until durable writes are verified |
| one lost critical item | load relevant chunks, prove earned/missing, run exact recovery command |
| one damaged fixture | pause players, run targeted repair and audit |
| corridor/gate/shell damaged | close whitelist and restore compatible world/plugin-data backup |
| stale/duplicate jar discovered | stop, remove all Observance jars, install the selected one, restart and preflight |
| finale armed incorrectly | cancel before safe cutoff |
| corrupt finale state | keep server closed in fault mode and inspect offline; never delete it to force fresh |
| secret exposed | stop affected service, rotate secret, redeploy, inspect logs/history, then resume |

Never solve a narrative problem with an operator command. Operator intervention exists only for a
demonstrable technical failure.

## 13. Final decision

Launch only when all statements are true:

```text
[ ] final main commit deployed to Vercel and both Railway services
[ ] production Supabase backup and V5 apply receipt complete
[ ] Discord commands/lease/recovery verified
[ ] all required media reachable and correctly gated
[ ] Paper 1.21.11 + Java 21 + exactly one Observance 0.5.0 jar
[ ] resource pack URL/hash/client rendering verified
[ ] well-based Unlit, seven evidence houses in any order, and base mirror pass
[ ] five surface NPCs and Wren pass exact V5 dialogue states
[ ] Deep Hold plan/build/post-restart audits pass
[ ] 1,588 model scenarios pass
[ ] 100/100 live test rows pass
[ ] all 24 rendered ending/conduct combinations rehearsed on restored clones
[ ] production finale fresh and unarmed
[ ] final stopped-server backup complete
[ ] no blank evidence or temporary asset remains
```

Anything unchecked is a blocker, not a future nice-to-have.
