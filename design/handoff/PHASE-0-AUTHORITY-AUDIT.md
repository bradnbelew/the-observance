# Phase 0 Authority and Runtime Audit

Status: **CURRENT PHASE 0 AUDIT — documentation/read-only discovery only**

This audit records contradictions between the repository's current V5 implementation authorities and
the approved ground-up rebuild contract. It does not rewrite canon, redesign cases, change schema, or
authorize deployment. `SPINE-LOCK.md` wins for story; the approved experience/runtime contract below
wins for rebuild flesh; current manifests remain accurate descriptions of the code and live data until
an approved later phase replaces them coherently.

## Approved rebuild contract

- About six friends, free-paced across several weeks, with 20–30 active hours and a 24–28 hour target.
- Any subset may progress; no content is time-gated or missable; catch-up and replay are durable.
- Minecraft is primary, while open-web research and ordinary tools are allowed.
- All substantial content is ultimately required even when framed as side investigation.
- Hints require director approval; automation may be ambient only when safe, while personalized, risky,
  or world-changing beats are approval-gated.
- Averyn is always freed; publish/unfile and Wren judgment change remembrance and coda, not release.
- The only player-facing standalone world is the gated `hold.zip` prologue used to derive the live IP.
- After onboarding, the Crafty-managed Paper `1.21.11` server on Brad's brother's PC is the only live
  campaign runtime for the surface settlement, hauntings, Mouth, Hold, altered region, camp, Release,
  and Coda.
- Any standalone vertical-slice save is private review material for Brad, never a second campaign world.
- Players retain survival gear in protected campaign regions; region enforcement prevents bypasses.
- Existing Brad-made footage must be inventoried for reuse or re-edit before any replacement decision.

## Contradictions and disposition

| Active authority or surface | Stale assumption | Phase 0 disposition |
| --- | --- | --- |
| `README.md` | Defines the product as ten cases, 82 nodes, 32 rooms, fixed media, and omits the gated-prologue/single-Crafty-runtime split. | Treat these as current V5 implementation facts only; the rebuild derives counts, layout, and media delivery after the conformance gate. |
| `design/EXPERIENCE-MANIFEST.md` | Fixes ten cases, 82 nodes, five media payloads, and about 15 active hours. | Superseded for rebuild pacing/count/media form by the 20–30 hour free-paced contract; ordered journey, mandatory substantial content, solo/subset recovery, and durable progress remain. |
| `design/ARG-V5-MASTER-PLAN.md` | Calls ten/82 and the validated 32-room shell the experience/build contract and presents six rendered endings. | Counts and shell are migration inputs, not rebuild constraints; there is one release outcome with remembrance/coda variations. |
| `design/ARG-V5-NODE-MANIFEST.csv` and predicate/runtime authorities | Encode the current exact 82-node graph and 60 physical predicates. | Preserve as current executable truth until a later approved schema/content migration; do not design the rebuild around their counts. |
| room, fixture, gate, blueprint, and setup authorities | Encode 32 rooms, 76 fixtures, eight gates, compact coordinates, and old geometry receipts. | Preserve for rollback/reference only; do not begin geometry before conformance and later plan/slice approvals. |
| `arc/WORLD-BIBLE.md` | Correctly owns spoiler truth but also freezes ten cases, 82 nodes, seven current Unlit houses, and five fixed media items as hard implementation invariants. | Preserve its locked truth; flag these flesh-level counts/forms for post-conformance reconciliation without changing the spine. |
| `arc/v5/media-manifest.json` and media checks | Treat five exact hosted payloads and their current URLs/bytes as fixed delivery. | Inventory Brad's footage first; preserve canon meanings where locked, while later approval may re-edit, rehost, or rebuild delivery and supporting media. |
| `design/handoff/README.md`, `02`, `08`, and prior master prompt | Carry forward a 15-hour target and fixed 10/82 contract, and direct an immediate Railway redeploy/KS01 implementation as Phase 0. | Phase 0 is now conformance, audit, topology documentation, and non-live checks only; deploys and implementation wait for later approval. |
| production runbooks | Assume a generic server operator flow and do not identify Crafty or the one-runtime boundary. | Later runbook work must target the Crafty-managed brother-hosted Paper server only and distinguish the prologue download and private review slice. |
| world/player-mode assumptions | Describe Adventure-mode campaign play and do not preserve ordinary survival inventory as an explicit contract. | Later protection design must permit survival gear while enforcing protected ARG regions and preventing bypasses without inventory escrow. |
| automation/showrunner authorities | Allow a persistent worker and cron to emit eligible beats without a director-approval classification for personalized/risky/world-changing effects. | Preserve safe ambient automation only; later parity work must add explicit approval gates and audit receipts for higher-risk actions. |

## Read-only service mapping (2026-07-15, America/Chicago)

No secret values were read, printed, or committed, and no service was mutated.

| Family | Exact mapping discovered | Phase 0 state |
| --- | --- | --- |
| Supabase | Project ref `fdnmhbpxnodrnbrzrlqq`, organization `yaenkbhruvrgkxqvngbk`, region `us-east-2`, Postgres `17.6.1.127`. | `ACTIVE_HEALTHY`; live rows still report 10 active required cases, 82 active required nodes, 5 active media rows, 15 active puzzles, and predicate hash `37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b`; only read-only metadata/SELECT calls were made. |
| Vercel | Team `team_2HAUKLhWF4QVYDHEt5FbNeHu` (`bradens-projects-c5e41066`), project `prj_UygHA98HGW4IBVMk6AKzXVEG6ZSQ` / `the-observance-kjxn`, domains including `copperlinehosting.com`. | Latest production deployment `dpl_26tawqUKuod6bVCqakmS2mEw1wdz` is `READY` from `main` commit `ca5416e477597b6e38f1dc82c9007c814184c980`; connector reads only. |
| Railway worker family | Repository contract: `/discord`, config `/discord/railway.worker.json`, command `npm start`, persistent restart-on-failure worker owning Discord and the fast lease-safe showrunner loop. | Live Railway project/service ID and deployment ID remain undiscovered because no Railway connector or CLI is installed and the available dashboard session is not authenticated. |
| Railway recovery family | Repository contract: `/discord`, config `/discord/railway.cron.json`, command `npm run showrunner`, schedule `*/10 * * * *`, restart `NEVER`, sharing the Supabase lease with the worker. | Live Railway project/service ID and deployment ID remain undiscovered for the same access reason; Brad's note establishes two available Railway projects, but the exact project-to-family assignment still needs authenticated read-only confirmation. |
| Minecraft production | Brother-hosted Crafty-managed Paper `1.21.11` server. | Declared by Brad as the only post-prologue campaign runtime; untouched. |
| Prologue/review worlds | Player-facing gated `hold.zip` prologue; separate standalone vertical slice only for Brad's private review. | Topology contract documented; no world files or geometry changed. |

## Later parity requirement

Every golden-standard release must include Minecraft/Crafty, Supabase, Vercel, Railway persistent
worker, and Railway recovery/cron in migration, deployment, rollback, and launch receipts, all tied to
the same release commit and artifact hashes. The two Railway projects must be named with project,
service, and deployment IDs as soon as authenticated read-only access is available.

## Gate and next phase

`SPINE-CONFORMANCE.md` was approved by Brad on 2026-07-15. The next phase is now open: experience
architecture comprising a 24–28 hour journey/case map, catch-up and replay model, content/media
inventory, authority-replacement plan, and coarse cross-surface topology. Case/evidence implementation,
schema redesign or migration, Unlit-form implementation, media replacement, Hold floorplans/geometry,
deploys, secret changes, and any action on the brother's server remain blocked behind later gates.
