# Phase 1 — Cross-Surface Topology and Authority Migration

Status: **PHASE 1 COMPLETE — DESIGN ONLY, NO LIVE MUTATION AUTHORIZED**

This document fixes where the campaign runs, what each surface owns, and how the current V5 authorities
will be replaced without a big-bang cutover. It does not authorize a deployment, migration, world build,
secret change, or action on Brad's brother's server.

## One-runtime player topology

```text
Copperline on Vercel
  -> gated hold.zip prologue (only player-facing standalone save)
  -> derive the live server address from prologue evidence
  -> brother-hosted Crafty-managed Paper 1.21.11 server
       -> inhabited surface settlement and selective hauntings
       -> in-Minecraft Discord discovery and identity proof
       -> Surface Mouth, descent, Deep Hold, altered region, camp
       -> convergence, Release, shutdown, persistent Coda

Discord + Copperline + media extend and mirror the investigation.
They never create a second Minecraft campaign runtime.

Private exception: a standalone vertical-slice save may be built only for Brad's review.
It is never linked, distributed, or presented to players as campaign progression.
```

The old “Adventure-mode world” assumption does not authorize inventory replacement. Players retain
their survival gear on the Crafty server. Protected campaign regions enforce block, entity, container,
teleport, route, and gate boundaries; ordinary inventory remains theirs.

## Surface ownership

| Surface/runtime | Owns | Must not own |
| --- | --- | --- |
| `hold.zip` prologue | Gated onboarding evidence and the derivation of the one live server address. | Post-onboarding progress, Discord invite delivery, Hold/altered/camp content, release state, or coda. |
| Crafty Paper `1.21.11` | Physical world truth, protection, source evidence, NPC state, routes, repairs, gates, artifacts, local recovery, protected player choices, immediate Release theater, and local coda. | Time gates, operator-staged solutions, inventory escrow as a mode switch, or dependence on Railway for immediate theater. |
| Supabase `fdnmhbpxnodrnbrzrlqq` | Cross-surface group identity, findings/receipts, provenance, hint approvals, media reveal state, service leases, mirrored choices, and safe public projections. | Overriding local physical truth, silently clearing progress, storing plaintext proof secrets, or choosing outcomes. |
| Vercel `prj_UygHA98HGW4IBVMk6AKzXVEG6ZSQ` | Copperline archaeology, gated prologue delivery, earned media wrappers/mirrors, pre-Discord help request, public Record, director console, and persistent web coda. | A second authoritative progression engine or direct world mutation. |
| Railway persistent worker project | Discord client, identity/link commands, open docket, pending hint requests, durable receipt/media/coda mirrors, and the fast lease-safe worker loop. | Required timed story beats, automatic solution hints, personalized/world-changing theater without approval, or finale choices. |
| Railway recovery/cron project | Ten-minute recovery invocation of the same import-safe lease-protected worker tasks. | A second showrunner authority, duplicate delivery, independent schedules for required content, or a divergent release commit. |
| Media storage/delivery | Preserved source and derivative bytes with hashes, first-party wrappers, accessibility companions, and rollback copies. | Untracked replacement, platform-only availability, or unearned public indexing. |

The persistent worker is currently described by `/discord/railway.worker.json` (`npm start`), and the
separate recovery family by `/discord/railway.cron.json` (`npm run showrunner`, every ten minutes). Their
live Railway project/service/deployment IDs still require authenticated read-only discovery.

## Coarse state flow

1. A player action or evidence discovery occurs on its owning surface.
2. The owner validates the declared predicate and commits local/durable state before presentation.
3. A stable idempotency key records the finding, provenance, contributors, and source version in Supabase
   when cross-surface projection is required.
4. Railway and Vercel mirror committed state; they do not infer a solve from telemetry or presence.
5. Paper reconciles only safe external facts—identity, approved hint payload, or committed cross-surface
   receipt—and never lets a stale/false external row close a local gate.
6. Outage queues are bounded and replay idempotently. Existing local progress remains valid; new work
   pauses only at the cross-surface boundary that cannot be durably acknowledged.
7. Release persists the players' choices and local finale phase before theater, then mirrors outward.
   External delay cannot interrupt the Minecraft goodbye, save, kick, shutdown, or Coda transition.

## Authority replacement matrix

| Current V5 authority | Phase 1 status | Replacement owner / rule |
| --- | --- | --- |
| `arc/WORLD-BIBLE.md` | Split authority: locked truth remains, flesh-level 10/82/five-media/Unlit-form claims are stale for rebuild design. | Later canon-expression pass must preserve the spine and reconcile flesh language only after Brad approves the new evidence architecture. |
| `design/EXPERIENCE-MANIFEST.md` | Superseded for duration, counts, pacing, attendance, catch-up, hints, and runtime topology. | `PHASE-1-EXPERIENCE-ARCHITECTURE.md` + `PHASE-1-PROGRESSION-GOVERNANCE.md`. |
| `design/ARG-V5-MASTER-PLAN.md` | Current implementation/migration reference, not rebuild target. | Phase 2 evidence architecture followed by approved technical contracts; locked truth still comes from the spine. |
| node/runtime/predicate manifests | Current executable truth; frozen during Phase 1. | Later versioned manifests derived from evidence chains, with old IDs mapped or retired through explicit migration—never edited piecemeal against live. |
| room/fixture/gate/blueprint manifests and `DeepHoldV4Plan` | Rollback/reference only for rebuild layout. | Future coordinate-native master plan after experience/evidence approval; no geometry until floorplan and private slice gates. |
| `arc/v5/media-manifest.json` | Preserve current assets/receipts; fixed delivery and count are not rebuild constraints. | `PHASE-1-MEDIA-INVENTORY.md` plus later Brad-approved per-asset keep/re-edit/replace decisions and versioned media manifest. |
| current hints and `/whisper` | Authored bodies are useful inventory; automatic delivery contract is stale. | Pending director approval model in `PHASE-1-PROGRESSION-GOVERNANCE.md`. |
| `AmbientBeatGenerator` safe mode | A0/A1 primitives are reusable; automatic attention/name personalization is stale. | Later risk-class enforcement that auto-runs only non-personal safe ambient behavior. |
| current production runbooks | Valid only for the current V5 release. | Later Crafty-specific parity/deploy/rollback/launch runbooks generated from the approved rebuild release candidate. |

## Migration sequence

No wave advances until its predecessor has a Brad-approved artifact and rollback plan.

### M0 — Freeze and inventory (Phase 1, complete)

- Preserve current manifests, hashes, service mappings, media receipts, and old Hold as rollback evidence.
- Record locked-spine conformance, new experience contract, progression governance, media custody, and
  one-runtime topology.
- Make no live changes.

### M1 — Evidence architecture (next approved design phase)

- Define each arc's evidence chain, revelation boundary, investigative identity, target duration, group
  split, recovery, submission, and emotional function.
- Produce old-node-to-new-finding disposition: reuse ID, map/merge, or retire with reason.
- Produce media placement proposals without editing any bytes.
- Brad approves before schema or case implementation.

### M2 — Technical contracts on isolated branches/clones

- Design versioned schema/manifests, local-primary state rules, idempotency keys, hint approvals, replay,
  service parity, and rollback migrations.
- Implement only against local databases, Supabase branches, disposable Paper clones, preview Vercel,
  and non-production Railway services where approved.
- Keep current production fully operational and untouched.

### M3 — Private vertical slice

- After coarse master adjacency approval, build one private standalone review slice for Brad containing
  Mouth/descent, intake, one hallway, one fully authored room, one investigation, one gate, and one
  meaningful Watcher/asymmetric moment.
- The slice is never player-facing and never becomes a parallel campaign world.
- Brad's in-game approval is required before district implementation.

### M4 — Incremental campaign build and parity rehearsal

- Build one arc/district end-to-end on disposable Crafty/Paper clones while maintaining versioned
  Supabase, Vercel, both Railway families, Discord, media, and rollback parity.
- Verify free-paced catch-up, replay, outage, region enforcement with survival gear, and director gates.
- Do not deploy production until every required arc and service receipt is complete.

### M5 — Production cutover

- Snapshot database, Crafty world/config, current artifacts, media, and deployment identities.
- Apply reviewed incremental migrations; deploy the same commit to Vercel and both Railway projects;
  install the approved plugin/assets through Crafty; verify hashes and health before opening progression.
- Roll back as one release set if any mandatory parity/readback check fails.

## Required parity and launch receipts

| Family | Migration receipt | Deployment receipt | Rollback receipt | Launch receipt |
| --- | --- | --- | --- | --- |
| Crafty/Paper | World/config backup, plugin data/state version, region policy diff. | Crafty server identity, Paper version, selected JAR/datapack/resource-pack hashes, startup and restart logs. | Proven restore of prior world/config/artifacts on a clone. | Non-op survival-gear region test, full route/preflight, restart recovery, finale/coda readiness. |
| Supabase | Backup/PITR marker, ordered incremental migration IDs, schema/data parity, advisors. | Project ref and migration/readback timestamp; no secret values. | Down/forward recovery procedure tested on branch/clone; retained receipts never silently lost. | Counts are derived from new manifests, RLS/security checks, cross-surface idempotency and outage reconciliation. |
| Vercel | Route/env-name/domain diff and preview evidence. | Team/project/deployment ID, Git commit, domains, build status. | Known rollback deployment and route/media compatibility proof. | Copperline, gated prologue, help request, media, public Record, author console, coda, DNS/TLS. |
| Railway worker | Env-name/config/command diff and service-family mapping. | Project/service/deployment IDs, same Git commit, boot/Discord registration and lease evidence. | Known prior deployment plus command/env compatibility proof. | Link, docket, pending hint request/approval, receipt/media/coda mirror, no unauthorized beats. |
| Railway recovery/cron | Env-name/config/schedule diff and shared-lease contract. | Separate project/service/deployment IDs, same Git commit, exact cron schedule. | Known prior deployment and disabled-safe recovery path. | Worker-down rehearsal, one recovery effect, no duplicate delivery, no second authority. |
| Media | Source/derivative provenance, hashes, accessibility companion, reveal mapping. | Storage object/version and wrapper deployment IDs. | Original preserved bytes and prior manifest/route restoration. | Signed-out and earned-route playback, replay/catch-up, payload legibility, accessibility, no premature indexing. |

## Known mapping gap

Supabase and Vercel are identified exactly in `PHASE-0-AUTHORITY-AUDIT.md`. The repository proves the
two Railway service-family configs, but authenticated live project/service/deployment IDs are still
missing. Those IDs are not needed to approve Phase 1 design; they are mandatory before M2 creates parity
environments and before any deploy/rollback receipt can pass.

## Phase boundaries still closed

Phase 1 completion does not authorize schema design or mutation, case/evidence implementation, media
editing or replacement, Unlit-form implementation, Hold floorplans/geometry, service deployment, secret
changes, or access to the brother's server. Those actions proceed only through M1–M5 approvals above.
