# Rebuild Phase Checkpoints

Status: **CURRENT CONTINUATION LEDGER**

This file is updated and committed at every autonomous phase boundary. A phase is not complete merely
because it appears here; its linked authorities and checks must independently prove the claim.

The canonical pre-revision integration lineage is `CANONICAL-LINEAGE.json`. It preserves the original
source commits, their linear incorporation commits, exact receipt scope, unresolved gaps, supersession,
the failed v1/v2 decisions, their Paper receipts, the v2 clean-stop evidence, and the current authored
v3 Paper/restart candidate. `python tools/check_continuation_lineage.py`
validates that chain. The integration checkpoint is the commit containing that lineage file.

| Phase | Status | Durable evidence | Verification | Checkpoint |
| --- | --- | --- | --- | --- |
| Phase 0 — Spine and authority control | Complete; Brad approved 2026-07-15 | `SPINE-CONFORMANCE.md`, `PHASE-0-AUTHORITY-AUDIT.md` | V5 freshness, repository integrity, current content/predicate checks | Branch `codex/phase-1-architecture`; foundation commit `6cc1361`, approval record commit `e18d502` |
| Phase 1 — Experience architecture | Complete; Brad explicitly approved 2026-07-15 and authorized autonomous continuation | `PHASE-1-APPROVAL.md` and four Phase 1 authorities | `python tools/check_phase1_architecture.py` plus existing non-live checks | Branch `codex/phase-1-architecture`; approval and continuation checkpoint `e18d502` |
| Phase 2 / M1 — Evidence architecture | Complete under Brad's standing approval | `PHASE-2-EVIDENCE-ARCHITECTURE.md`, `PHASE-2-LEGACY-NODE-DISPOSITION.md`, `PHASE-2-CONFORMANCE-AND-MEDIA-AUDIT.md` | Phase 2, Phase 1, freshness, integrity, predicate, voice, book, scenario, content-audit, and fixture checks pass; current V5 content/layout checks expose the carried `37020…` vs `16de…` predicate-receipt mismatch recorded in the audit | Branch `codex/phase-2-evidence-architecture`; evidence commit `fae8b26`; continuation checkpoint is the commit containing this ledger row |
| M2 — Technical contracts and isolated implementation | Complete under Brad's standing approval | `design/m2/M2-TECHNICAL-CONTRACTS.md`; six generated manifests; all 82 import contracts; exact predicate byte chain; schema/rollback/forward and seven-surface parity contracts; isolated Paper/Discord implementation | M2 static gate; full plugin clean/check; Discord type/approval/voice/seed/bundle; Phase 1/2, integrity, freshness, content, 60 predicates, Hold, and 1,588-scenario checks pass. Secret-dependent and external platform receipts remain open. | Branch `codex/m2-technical-contracts`; evidence commit `2aedeca9198db36b029aaa39f364e7688fbba171`; continuation checkpoint is the commit containing this ledger row |
| M3 — Private vertical slice | V3 active review: visibly improved, but interaction affordances and investigation objective are not discoverable; NOT APPROVED | Preserved v1/v2 history; v3 structural/Paper receipts; `BRAD-V3-ACTIVE-REVIEW.json`; canonical lineage | Automated v3 receipts remain valid, but cold-player legibility currently fails: right-click versus crouch-use, evidence versus filing lecterns, crouching as filing, and the room objective require external explanation. Three mandatory future checks cover distinct filing affordance, diegetic instruction, and cold-player objective comprehension. Live server unchanged; revision paused; Brad approval null; M4 closed. | Branch `codex/m3-disposable-paper-gate`; passing v3 receipt checkpoint `a30f1301046819e88c8029d604c00703b055cc2e`; active-review checkpoint is the commit containing this row |
| M4 — Incremental campaign build and parity rehearsal | Closed pending remaining M3 real-client receipts and Brad's in-game approval | All required arcs/districts, services, content, assets, migrations, packages | Full non-live/live clone matrix, catch-up/replay/outage/region tests | Do not start district implementation from the structural-only M3 Paper checkpoint |
| M5 — Production cutover and final release | Pending | GitHub/Vercel/Railway/Supabase/Crafty/media parity, final JAR/package, hashes, backups, rollback and launch receipts | Aggregate release gate plus real production readback and post-restart/coda evidence | Final checkpoint and handoff |

## Known external gaps carried forward

- `../m3/EXTERNAL-GAP-AUDIT-2026-07-16.md` records the latest read-only discovery receipts. Railway's
  dashboard was unauthenticated and no CLI/token/IDs were available, so authenticated project,
  environment, service, deployment, and configuration-parity evidence remains open for both families.
- The exact four receipted video byte sets and full five-file recovered packet were located at the
  previously recorded local staging paths and hash-match the committed receipts. Best-master/ownership
  confirmation, human audiovisual/accessibility review, and Brad's keep/re-edit/replace decisions remain
  open; authenticated Drive searches returned no matching video, audio, or packet files.
- Vercel team/project and current production deployment identity were reconfirmed read-only, but that
  deployment predates M2/M3 and no exact-checkpoint preview exists in the 20 newest deployments. The M2
  preview readiness receipt remains open; production was untouched.
- Crafty/brother-host access remains unavailable. Public DNS resolved the declared endpoint, but TCP
  `25569` did not answer, so no live Paper/Crafty version or runtime metadata was obtained.
- Phase 2 timing and evidence fairness are architecture findings; human playtest, source AV review, and
  live client/route/restart receipts remain for their owning later phases.
- The predicate discrepancy is resolved: `37020e...` is the exact historical mixed-EOL build byte set
  and `16de...` is its LF-normalized Git authority; both canonicalize to semantic hash `d2eec3...`.
  Production still records `37020e...` and remains untouched. A later confirmed same-release cutover
  must migrate source/package/database/runtime together to `16de...` and retain rollback bytes.
- No disposable Supabase target was available and the official CLI could not be installed in the
  restricted environment. A 2026-07-16 continuation confirmed only the production project was visible,
  hard-blocked its ref, and added a CLI-scaffolded local-only lifecycle/pgTAP/advisor harness plus an
  exact blocker record in `design/m2/M2-SUPABASE-VALIDATION.md`. The additive SQL remains a reviewed
  proposal outside the migrations folder; a real local/branch application, both advisors, and recorded
  migration/rollback/forward receipts remain required.
- This isolated task had no Discord/Supabase secrets. The aggregate routed run honestly stopped at the
  secret-dependent Discord resolve check after every preceding source/M3/layout/scenario/SQL audit
  passed; no credentials were fabricated or copied. Discord TypeScript and pack checks, dashboard
  lint/self-tests/build, the full plugin build, deterministic packaging/readback, publisher/backup
  guards, non-live media, and repository integrity passed separately.
- The authored v3 replacement now has a fresh local-only disposable Paper 1.21.11 build with exact
  closed/open structural audits, stop/restart/re-audit, journal replay/idempotency, and
  Paper/plugin/world/package hashes. No
  authenticated Minecraft clients were available, so non-op Adventure with retained survival gear,
  event-level protected-region bypasses, two-client asymmetry, solo readback, and player-facing
  investigation UX remain external M3 gates.
- Brad explicitly rejected both v1 and v2 on 2026-07-16. Their exact findings and decisions remain
  preserved. V3 addresses the combined v2 decision and has a separately prepared pristine review
  target, but no Brad approval; M4 remains closed.
- Brad's active v3 walk confirms visible improvement but finds the interaction language and room
  objective undiscoverable without external explanation: right-click versus crouch-use, evidence versus
  filing lecterns, crouching as stamping, and the investigation goal all remain unclear. The live server
  stays unchanged until the pass completes; future acceptance requires distinct filing affordance,
  parsimonious diegetic instruction, cold-player objective comprehension, and Brad's explicit decision.

These gaps must be resolved before their owning implementation/release gates; they do not justify losing
or postponing safe work in earlier phases.
