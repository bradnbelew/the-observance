# Rebuild Phase Checkpoints

Status: **CURRENT CONTINUATION LEDGER**

This file is updated and committed at every autonomous phase boundary. A phase is not complete merely
because it appears here; its linked authorities and checks must independently prove the claim.

| Phase | Status | Durable evidence | Verification | Checkpoint |
| --- | --- | --- | --- | --- |
| Phase 0 — Spine and authority control | Complete; Brad approved 2026-07-15 | `SPINE-CONFORMANCE.md`, `PHASE-0-AUTHORITY-AUDIT.md` | V5 freshness, repository integrity, current content/predicate checks | Branch `codex/phase-1-architecture`; commit `6cc1361` plus approval/continuation follow-up |
| Phase 1 — Experience architecture | Complete; Brad explicitly approved 2026-07-15 and authorized autonomous continuation | `PHASE-1-APPROVAL.md` and four Phase 1 authorities | `python tools/check_phase1_architecture.py` plus existing non-live checks | Branch `codex/phase-1-architecture`; approval/continuation follow-up commit pending |
| Phase 2 / M1 — Evidence architecture | Open under standing approval | Required: arc evidence chains, investigation identities, duration/group/recovery/submission/emotional design, old-node disposition, media placement proposals | Required: dedicated architecture check plus spine/revelation/count/fairness audit | Next fresh task from the committed Phase 1 branch |
| M2 — Technical contracts and isolated implementation | Pending | Versioned schema/manifests, local-primary state, idempotency, approval gates, parity and rollback contracts; implementation on approved isolated targets | Unit/static/integration/security checks and isolated service/Paper receipts | Future checkpoint |
| M3 — Private vertical slice | Pending | Brad-only slice and exact review package | Automated reachability plus Brad in-game review; never player-facing | Future checkpoint |
| M4 — Incremental campaign build and parity rehearsal | Pending | All required arcs/districts, services, content, assets, migrations, packages | Full non-live/live clone matrix, catch-up/replay/outage/region tests | Future checkpoints, one coherent slice at a time |
| M5 — Production cutover and final release | Pending | GitHub/Vercel/Railway/Supabase/Crafty/media parity, final JAR/package, hashes, backups, rollback and launch receipts | Aggregate release gate plus real production readback and post-restart/coda evidence | Final checkpoint and handoff |

## Known external gaps carried forward

- Authenticated Railway project/service/deployment IDs for the worker and recovery families.
- Source custody and audiovisual review for Brad's four videos and recovered archive packet.
- Crafty/brother-host access and live-server operations remain unavailable until the relevant phase and
  platform confirmation.

These gaps must be resolved before their owning implementation/release gates; they do not justify losing
or postponing safe work in earlier phases.
