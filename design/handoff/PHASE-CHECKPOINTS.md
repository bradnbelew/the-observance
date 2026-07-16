# Rebuild Phase Checkpoints

Status: **CURRENT CONTINUATION LEDGER**

This file is updated and committed at every autonomous phase boundary. A phase is not complete merely
because it appears here; its linked authorities and checks must independently prove the claim.

| Phase | Status | Durable evidence | Verification | Checkpoint |
| --- | --- | --- | --- | --- |
| Phase 0 — Spine and authority control | Complete; Brad approved 2026-07-15 | `SPINE-CONFORMANCE.md`, `PHASE-0-AUTHORITY-AUDIT.md` | V5 freshness, repository integrity, current content/predicate checks | Branch `codex/phase-1-architecture`; foundation commit `6cc1361`, approval record commit `e18d502` |
| Phase 1 — Experience architecture | Complete; Brad explicitly approved 2026-07-15 and authorized autonomous continuation | `PHASE-1-APPROVAL.md` and four Phase 1 authorities | `python tools/check_phase1_architecture.py` plus existing non-live checks | Branch `codex/phase-1-architecture`; approval and continuation checkpoint `e18d502` |
| Phase 2 / M1 — Evidence architecture | Complete under Brad's standing approval | `PHASE-2-EVIDENCE-ARCHITECTURE.md`, `PHASE-2-LEGACY-NODE-DISPOSITION.md`, `PHASE-2-CONFORMANCE-AND-MEDIA-AUDIT.md` | Phase 2, Phase 1, freshness, integrity, predicate, voice, book, scenario, content-audit, and fixture checks pass; current V5 content/layout checks expose the carried `37020…` vs `16de…` predicate-receipt mismatch recorded in the audit | Branch `codex/phase-2-evidence-architecture`; evidence commit `fae8b26`; continuation checkpoint is the commit containing this ledger row |
| M2 — Technical contracts and isolated implementation | Complete under Brad's standing approval | `design/m2/M2-TECHNICAL-CONTRACTS.md`; six generated manifests; all 82 import contracts; exact predicate byte chain; schema/rollback/forward and seven-surface parity contracts; isolated Paper/Discord implementation | M2 static gate; full plugin clean/check; Discord type/approval/voice/seed/bundle; Phase 1/2, integrity, freshness, content, 60 predicates, Hold, and 1,588-scenario checks pass. Secret-dependent and external platform receipts remain open. | Branch `codex/m2-technical-contracts`; implementation checkpoint is the commit containing this ledger row |
| M3 — Private vertical slice | Open under Brad's standing approval after the committed M2 checkpoint | Coarse adjacency authority, one Brad-only slice, exact review package; no second progression runtime | Automated reachability, fresh disposable Paper build/restart/non-op checks, then Brad in-game review; never player-facing | Fresh task from exact committed M2 checkpoint |
| M4 — Incremental campaign build and parity rehearsal | Pending | All required arcs/districts, services, content, assets, migrations, packages | Full non-live/live clone matrix, catch-up/replay/outage/region tests | Future checkpoints, one coherent slice at a time |
| M5 — Production cutover and final release | Pending | GitHub/Vercel/Railway/Supabase/Crafty/media parity, final JAR/package, hashes, backups, rollback and launch receipts | Aggregate release gate plus real production readback and post-restart/coda evidence | Final checkpoint and handoff |

## Known external gaps carried forward

- Authenticated Railway project/service/deployment IDs for the worker and recovery families.
- Source custody and audiovisual review for Brad's four videos and recovered archive packet.
- Crafty/brother-host access and live-server operations remain unavailable until the relevant phase and
  platform confirmation.
- Phase 2 timing and evidence fairness are architecture findings; human playtest, source AV review, and
  live client/route/restart receipts remain for their owning later phases.
- The predicate discrepancy is resolved: `37020e...` is the exact historical mixed-EOL build byte set
  and `16de...` is its LF-normalized Git authority; both canonicalize to semantic hash `d2eec3...`.
  Production still records `37020e...` and remains untouched. A later confirmed same-release cutover
  must migrate source/package/database/runtime together to `16de...` and retain rollback bytes.
- No disposable Supabase target was available and the official CLI could not be installed in the
  restricted environment. The additive SQL remains a reviewed proposal outside the migrations folder;
  branch/local application, database tests, advisors, rollback, and forward receipts remain required.
- This isolated task had no Discord/Supabase secrets, so secret-dependent integration runners were not
  executed. Static, type, seed/bundle, approval, and full plugin checks passed without copying secrets.

These gaps must be resolved before their owning implementation/release gates; they do not justify losing
or postponing safe work in earlier phases.
