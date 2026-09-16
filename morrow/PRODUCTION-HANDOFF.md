# Morrow Production Handoff

Status: first playable reboot slice is review/test ready. It is **not** production-launch ready.

This file supersedes the long historical M01-M12 handoff that previously lived here. That earlier
handoff described old implementation evidence and must not be used as current launch, migration,
world-build, or director-operation authority.

## Current Checkpoint

Branch work landed on `codex/morrow-reboot-architecture` and is intended to become `main`.

Implemented and verified locally:

- G01-G03 Copperline website trail.
- G01-G05 local gate/discovery receipt chain.
- G01-G15 full local spine for planning and QA.
- Locked director contract with no production mutation path.
- Rehearsal APIs for full spine, per-gate state, media readiness, and director state.
- Vercel-compatible local JSON rehearsal data packaged under `dashboard/`.
- Production-readiness checker that keeps launch blocked until remaining lanes are real.

## Required Review Entry Points

- `morrow/README.md`
- `morrow/rehearsal/FIRST-PLAYABLE-RUNBOOK.md`
- `morrow/rehearsal/PRODUCTION-READINESS-MATRIX.md`
- `morrow/media/MEDIA-PRODUCTION-PACKET.md`
- `dashboard/README.md`

## Current Verification Commands

From `dashboard/`:

```powershell
npm run lint
npm run build
npm run morrow:reboot-selftest
```

From the repository root:

```powershell
python tools\check_morrow_g01_g05_vertical_slice.py
python tools\check_morrow_full_rehearsal.py
python tools\check_morrow_local_contracts.py
python tools\check_morrow_production_readiness.py
```

## Launch Blockers

The current package must not be used as a live player launch until these lanes have fresh G01-G15
proof:

- Final media files are authored, hashed, custody-recorded, and accessibility-backed.
- Minecraft world/Paper gameplay proves G04-G15 clue objects, bounded scares, restart recovery, and
  terminal shutdown.
- Discord flow proves linked-player fragments, isolation, authored responses, and terminal silence.
- Supabase migration proves RLS, idempotency, replay refusal, rollback, and public projections.
- Director controls prove allowlisted, receipt-bound actions and explicit recovery.
- Full one-, two-, and six-player rehearsal passes are retained under a new release ID.

## Historical Boundary

Old M01-M12 receipts, V5 route copy, and prior world evidence are historical only. They can inform
engineering choices, but they cannot satisfy reboot launch gates and should not be copied into player
copy or operator instructions without an explicit G01-G15 remap and fresh tests.
