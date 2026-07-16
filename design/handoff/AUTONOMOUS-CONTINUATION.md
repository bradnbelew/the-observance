# Autonomous Phase Continuation Authority

Status: **APPROVED BY BRAD — 2026-07-15**

Brad authorized Codex to continue the approved rebuild through every remaining phase without stopping
for routine intermediate design approval. At each phase boundary, Codex may mark the gate approved and
start the next phase only after the current phase's explicit deliverables and verification are genuinely
complete.

## Required loop

1. Read the locked spine, approved conformance statement, current Phase authorities, and
   `PHASE-CHECKPOINTS.md` completely.
2. Work only within the current phase's scope and preserve unrelated/user changes.
3. Run a requirement-by-requirement completion audit using authoritative files, checks, rendered or
   runtime evidence, and external receipts appropriate to that phase.
4. Fix every failed or weakly evidenced requirement; never weaken a check to obtain a pass.
5. Update `PHASE-CHECKPOINTS.md` with deliverables, checks, remaining external gaps, branch, and commit.
6. Commit every completed phase to Git on the continuing `codex/` branch. A clean committed checkpoint
   is mandatory before handoff; uncommitted work may not be the only copy.
7. Apply Brad's standing approval to open the next phase, while retaining all narrower safety and
   production gates defined by the architecture.
8. Create a fresh Codex task from the exact checkpoint branch/commit, provide the full next-phase scope
   and terminal objective, and instruct that task to repeat this loop.
9. Continue until the complete rebuild, verification, package, migration/deploy/rollback receipts, and
   final handoff are proven—not merely planned.

## What standing approval means

- Codex does not need to pause for ordinary phase-design approval when the documented gate is green.
- Codex may implement, test, version, package, and prepare the approved rebuild in the sequence defined
  by `PHASE-1-TOPOLOGY-AND-MIGRATION.md` and later authorities.
- When the final release phase is reached, Brad's instruction authorizes the normal GitHub/Vercel/service
  publication workflow and final JAR/package preparation after all required backups, parity, rollback,
  security, live-environment, and launch receipts pass.

## What standing approval does not waive

- The locked spine, exactly three ambiguities, one release outcome, single Crafty runtime, media
  preservation, survival-gear region policy, or any other approved invariant.
- Tool/platform confirmations required for external writes, deployments, credentials, costs, permissions,
  destructive actions, production database mutation, or access to Brad's brother's server.
- Secret handling, backups, incremental migrations, rollback proof, branch protection, external service
  parity, non-op tests, or real-client/live-server evidence.
- The requirement to stop and report a genuine external blocker that cannot be safely resolved with the
  available authority or access.
- Permission to fabricate receipts, treat preview/local evidence as production evidence, or call the
  campaign finished while any required phase or acceptance test is incomplete.

## Preservation rule

The continuing Git branch and `PHASE-CHECKPOINTS.md` are the durable chain of custody. Every generated
authority, source change, migration, asset manifest, test, package hash, deployment ID, rollback marker,
and launch receipt must be committed or recorded in a committed authority before the next task starts.
Large source media and secrets stay in their approved external stores; Git records identities, hashes,
provenance, and recovery instructions rather than secret values or inappropriate binaries.
