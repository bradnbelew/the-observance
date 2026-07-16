# The Observance

> **Ground-up rebuild Phase 0 (2026-07-15):** Start with
> `design/handoff/SPINE-LOCK.md`, `design/handoff/SPINE-CONFORMANCE.md`, and
> `design/handoff/PHASE-0-AUTHORITY-AUDIT.md`. The spine is locked and the conformance statement requires
> Brad approved it on 2026-07-15, opening experience architecture while later implementation, live-state,
> and geometry gates remain closed. The approved rebuild
> is free-paced for about six friends over 20–30 active hours (24–28 target). Existing 10-case, 82-node,
> 32-room, and five-media counts below describe the current V5 implementation, not rebuild constraints.

> After the gated player-facing `hold.zip` prologue, the brother-hosted Crafty-managed Paper `1.21.11`
> server is the only live Minecraft campaign runtime. A standalone vertical slice is Brad-only review;
> players keep survival gear in protected ARG regions, with bypass prevention enforced by region rules.

The Observance is a production Minecraft ARG for Paper `1.21.11`: a ten-case, 82-node investigation spanning a protected command-built Deep Hold, the village-well Unlit, a legacy-hosting website, Discord, fixed found footage, and a branch-specific cinematic server close.

The campaign is designed for ordinary Minecraft clients in Adventure mode. Players do not manually build ARG structures, break puzzle blocks, inspect real server logs, install client mods, or depend on an operator moving rooms during play.

## Current V5 premise

During the Long Cold, a real community built the Hold as a refuge. Six distinct officials later hid evidence of a falsified civic disaster and the erased registrar Averyn, whose consciousness became trapped as the human interface of the Record. Four modern investigators reached the same archive and failed after Wren betrayed their routes. The current group must reconstruct both histories, repair the Hold, decide Wren's fate, and close the Record without binding Averyn into it again.

The story is not a six-plus-one repetition or a “six were secretly one” retcon. Every substantial case and every fixed media asset is required.

## Repository map

| Path | Authority |
| --- | --- |
| `arc/WORLD-BIBLE.md` | only spoiler-truth authority |
| `design/ARG-V5-NODE-MANIFEST.csv` | exactly 82 required nodes and their graph |
| `design/EXPERIENCE-MANIFEST.md` | player order and surface responsibilities |
| `arc/v5/` | exact books, NPC dialogue, media receipts, solutions, and hints |
| `design/ARG-V5-BOOK-PLACEMENT.csv` | exact holder, mount, facing, unlock, and PDC for every book |
| `design/ARG-V5-ROOM-ASSIGNMENTS.csv` | 32-room case ownership |
| `design/ARG-V5-FIXTURE-OWNERSHIP.csv` | 76-fixture node ownership |
| `design/ARG-V5-ARTIFACT-MANIFEST.csv` | 21 critical item/recovery contracts |
| `plugin/` | Paper plugin and deterministic world builder/runtime |
| `discord/` | Discord bot, showrunner worker, Supabase migrations/seeds |
| `dashboard/` | Copperline website, public Record, and author console |
| `datapack/` | inert V5 deployment/version marker; no story state or retired dimension |
| `resourcepack/` | auto-pushed client assets |
| `tools/` | content, build, deployment, and rehearsal checks |

Every pre-V5 design/lore document carries an explicit archive header and is retained only as development history. It is not an implementation, setup, or readiness source.

## Runtime architecture

- **Minecraft / Paper 1.21.11 / Java 21:** authoritative physical gameplay, protection, gates, NPC responses, recovery, and immediate finale.
- **Supabase:** durable group/node/evidence/finale state and safe public projections.
- **Discord worker:** identity linking, answers, hints, mirrored receipts, and a fast lease-safe showrunner tick.
- **Vercel dashboard/site:** Copperline trail, required media routes, public Record, author controls, and coda.
- **Railway:** persistent Discord worker plus ten-minute recovery cron.

External services enhance delivery but never hold Minecraft theater hostage. Critical NPC replies and the finale run locally and synchronously.

## Build

Windows production candidate (live media is checked by default):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1
```

Install Node dependencies with `npm.cmd ci` in `discord/` and `dashboard/` before the first clean run. The exact active/retired tool boundary is documented in `tools/README.md`. `-SkipLiveExternalMedia` is for offline development only and cannot produce a launch receipt.

The release pipeline must select one plugin jar, remove/refuse stale Observance jars, rebuild the SQL bundle rather than hand-editing it, and record hashes for every deployed artifact.

## World and launch setup

Use `design/V5-WORLD-SETUP-AND-TESTING.md` and `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`. Do not use an older rehearsal/status document as a live command authority.

High-level order:

1. snapshot production data/world and rehearse on a clone;
2. apply the V5 migration and explicit stale-row retirement;
3. install exactly one V5 plugin jar plus current datapack/resource pack;
4. bind the real village well and individual NPC anchors;
5. preview, survey, build, persist, and audit the Deep Hold at a fresh site;
6. verify all external media, Discord commands, Railway worker/cron, Vercel routes, and DNS;
7. complete non-op and failure/restart rehearsals;
8. arm the finale only for the real run.

## Release standard

“Build succeeded” is not play readiness. Release requires 82/82 content validation, exact books/signs/items/NPC parity, full Hold/Unlit traversal, every gate/restart/recovery scenario, clean Paper `1.21.11` evidence, all three Wren branches, both name treatments, real-client typography/hitbox checks, production service verification, and a branch-specific save/goodbye/kick/shutdown/Coda sequence.
