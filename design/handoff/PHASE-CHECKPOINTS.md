# Rebuild Phase Checkpoints

Status: **CURRENT CONTINUATION LEDGER**

This file is updated and committed at every autonomous phase boundary. A phase is not complete merely
because it appears here; its linked authorities and checks must independently prove the claim.

| Phase | Status | Durable evidence | Verification | Checkpoint |
| --- | --- | --- | --- | --- |
| Phase 0 — Spine and authority control | Complete; Brad approved 2026-07-15 | `SPINE-CONFORMANCE.md`, `PHASE-0-AUTHORITY-AUDIT.md` | V5 freshness, repository integrity, current content/predicate checks | Branch `codex/phase-1-architecture`; foundation commit `6cc1361`, approval record commit `e18d502` |
| Phase 1 — Experience architecture | Complete; Brad explicitly approved 2026-07-15 and authorized autonomous continuation | `PHASE-1-APPROVAL.md` and four Phase 1 authorities | `python tools/check_phase1_architecture.py` plus existing non-live checks | Branch `codex/phase-1-architecture`; approval and continuation checkpoint `e18d502` |
| Phase 2 / M1 — Evidence architecture | Complete under Brad's standing approval | `PHASE-2-EVIDENCE-ARCHITECTURE.md`, `PHASE-2-LEGACY-NODE-DISPOSITION.md`, `PHASE-2-CONFORMANCE-AND-MEDIA-AUDIT.md` | Phase 2, Phase 1, freshness, integrity, predicate, voice, book, scenario, content-audit, and fixture checks pass; current V5 content/layout checks expose the carried `37020…` vs `16de…` predicate-receipt mismatch recorded in the audit | Branch `codex/phase-2-evidence-architecture`; evidence commit `fae8b26`; continuation checkpoint is the commit containing this ledger row |
| M2 — Technical contracts and isolated implementation | Open under standing approval after the committed Phase 2 checkpoint | Versioned schema/manifests, local-primary state, idempotency, approval gates, parity and rollback contracts; implementation on approved isolated targets | Unit/static/integration/security checks and isolated service/Paper receipts | Fresh task from exact committed Phase 2 checkpoint |
| M3 — Private vertical slice | Pending | Brad-only slice and exact review package | Automated reachability plus Brad in-game review; never player-facing | Future checkpoint |
| M4 — Incremental campaign build and parity rehearsal | Pending | All required arcs/districts, services, content, assets, migrations, packages | Full non-live/live clone matrix, catch-up/replay/outage/region tests | Future checkpoints, one coherent slice at a time |
| M5 — Production cutover and final release | Pending | GitHub/Vercel/Railway/Supabase/Crafty/media parity, final JAR/package, hashes, backups, rollback and launch receipts | Aggregate release gate plus real production readback and post-restart/coda evidence | Final checkpoint and handoff |

## Known external gaps carried forward

- Authenticated Railway project/service/deployment IDs for the worker and recovery families.
- Source custody and audiovisual review for Brad's four videos and recovered archive packet.
- Crafty/brother-host access and live-server operations remain unavailable until the relevant phase and
  platform confirmation.
- Phase 2 timing and evidence fairness are architecture findings; human playtest, source AV review, and
  live client/route/restart receipts remain for their owning later phases.
- Current committed predicate bytes hash to `16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a`
  while the seed/live receipt records `37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b`;
  M2 must reconcile exact historical bytes and the versioned hash chain on isolated targets before any
  predicate implementation, migration, or release gate.

These gaps must be resolved before their owning implementation/release gates; they do not justify losing
or postponing safe work in earlier phases.
