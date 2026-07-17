# Rebuild Phase Checkpoints

Status: **CURRENT CONTINUATION LEDGER**

This file is updated and committed at every autonomous phase boundary. A phase is not complete merely
because it appears here; its linked authorities and checks must independently prove the claim.

| Phase | Status | Durable evidence | Verification | Checkpoint |
| --- | --- | --- | --- | --- |
| Phase 0 — Spine and authority control | Complete; Brad approved 2026-07-15 | `SPINE-CONFORMANCE.md`, `PHASE-0-AUTHORITY-AUDIT.md` | V5 freshness, repository integrity, current content/predicate checks | Branch `codex/phase-1-architecture`; foundation commit `6cc1361`, approval record commit `e18d502` |
| Phase 1 — Experience architecture | Complete; Brad explicitly approved 2026-07-15 and authorized autonomous continuation | `PHASE-1-APPROVAL.md` and four Phase 1 authorities | `python tools/check_phase1_architecture.py` plus existing non-live checks | Branch `codex/phase-1-architecture`; approval and continuation checkpoint `e18d502` |
| Phase 2 / M1 — Evidence architecture | Complete under Brad's standing approval | `PHASE-2-EVIDENCE-ARCHITECTURE.md`, `PHASE-2-LEGACY-NODE-DISPOSITION.md`, `PHASE-2-CONFORMANCE-AND-MEDIA-AUDIT.md` | Phase 2, Phase 1, freshness, integrity, predicate, voice, book, scenario, content-audit, and fixture checks pass; current V5 content/layout checks expose the carried `37020…` vs `16de…` predicate-receipt mismatch recorded in the audit | Branch `codex/phase-2-evidence-architecture`; evidence commit `fae8b26`; continuation checkpoint is the commit containing this ledger row |
| M2 — Technical contracts and isolated implementation | Complete under Brad's standing approval | `design/m2/M2-TECHNICAL-CONTRACTS.md`; six generated manifests; all 82 import contracts; exact predicate byte chain; schema/rollback/forward and seven-surface parity contracts; isolated Paper/Discord implementation | M2 static gate; full plugin clean/check; Discord type/approval/voice/seed/bundle; Phase 1/2, integrity, freshness, content, 60 predicates, Hold, and 1,588-scenario checks pass. Secret-dependent and external platform receipts remain open. | Branch `codex/m2-technical-contracts`; evidence commit `2aedeca9198db36b029aaa39f364e7688fbba171`; continuation checkpoint is the commit containing this ledger row |
| M3 — Private vertical slice | Structural machinery proof complete; Brad rejected the first visual pass on 2026-07-16; revision required | `design/m3/M3-PRIVATE-VERTICAL-SLICE.md`; `BRAD-REVIEW-PACKAGE.md` exact failed decision; coordinate-native authorities; `PAPER-DISPOSABLE-RECEIPT.json`; local-primary proof; no second progression runtime | Fresh local-only Paper 1.21.11 build 132: 99,789-cell closed/open audits, restart, replay, and hashes pass. Brad's real-client walk failed palette, scale/density, water, copying-workplace legibility, entrance/exit composition, and controlled-gate credibility. Bypass, two-client/solo, player-facing UX, revised build, and visual approval remain open. | Branch `codex/m3-disposable-paper-gate`; runtime commit `16a712c08566135635fdfe383a42a85ed9320db5`; failed visual decision checkpoint is the commit containing this ledger row |
| M4 — Incremental campaign build and parity rehearsal | Closed pending remaining M3 real-client receipts and Brad's in-game approval | All required arcs/districts, services, content, assets, migrations, packages | Full non-live/live clone matrix, catch-up/replay/outage/region tests | Do not start district implementation from the structural-only M3 Paper checkpoint |
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
- A fresh local-only disposable Paper 1.21.11 build now has exact closed/open structural audits,
  stop/restart/re-audit, journal replay/idempotency, and Paper/plugin/world/package hashes. No
  authenticated Minecraft clients were available, so non-op Adventure with retained survival gear,
  event-level protected-region bypasses, two-client asymmetry, solo readback, and player-facing
  investigation UX remain external M3 gates.
- Brad walked the private P4 slice on 2026-07-16 and explicitly rejected it. The exact findings and
  `FAILED / REVISION REQUIRED` decision are in `design/m3/BRAD-REVIEW-PACKAGE.md`. Standing approval
  does not open M4 before a revised build passes another explicit visual review.

These gaps must be resolved before their owning implementation/release gates; they do not justify losing
or postponing safe work in earlier phases.
