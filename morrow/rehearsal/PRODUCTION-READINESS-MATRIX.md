# Morrow Production Readiness Matrix

Status: not production ready. First playable review slice is ready and deployed for review.

The current package is meant to let reviewers test the opening Copperline ARG path and inspect the full G01-G15 local spine. It must not be used as a launch approval, production migration, live Discord worker, or Minecraft server activation.

## Green Right Now

- G01-G03 Copperline website surfaces exist and are deployed on the production review target.
- G01-G05 local gate/discovery receipt chain validates.
- G01-G15 full local spine validates.
- Director controls are exposed only as a locked contract.
- Media requirements are fully enumerated.
- Production mutation remains disabled.
- Vercel production deployment `dpl_HziaWHfk7paFF91UTquJeHphockE` serves commit
  `b76f6ba736420120457002a93a52cce7b8455879` at `https://copperlinehosting.com`.

## Hard Blockers

| Lane | Current state | Release exit criteria |
| --- | --- | --- |
| Media | Required assets are specified but not authored. | All 12 final assets exist, hashes and accessibility equivalents recorded, custody notes complete. |
| Minecraft | Object/world contracts exist only. | Disposable Paper rehearsal proves clue objects, scares, recovery, restart, and shutdown route. |
| Discord | G10 fragment flow contract exists only. | Database-bound worker proves authored fragments, linked-recipient isolation, and post-shutdown silence. |
| Database | Additive schema is proposal-only. | Numbered migration, isolated Supabase validation, RLS/idempotency/replay/rollback checks. |
| Director | Locked and disabled. | Receipt-bound allowlisted controls with explicit recovery and finale arm/start proof. |

## First Playable Test

Use `morrow/rehearsal/FIRST-PLAYABLE-RUNBOOK.md`.

The review goal is whether a player can move from ordinary Copperline residue into Mossfield recovery without puzzle-room language:

1. Find the retired managed-world residue.
2. Connect Iona's forum identity.
3. Read Theo's support-ticket motive.
4. Reach the Mossfield recovery surface.
5. Inspect the local gate console and confirm production remains blocked.

## Commands

From `dashboard/`:

```powershell
npm run morrow:reboot-selftest
npm run lint
```

From repo root:

```powershell
python tools\check_morrow_g01_g05_vertical_slice.py
python tools\check_morrow_full_rehearsal.py
python tools\check_morrow_local_contracts.py
python tools\check_morrow_media_intake.py
python tools\check_morrow_production_readiness.py
```

## Website Deployment Evidence

Retained production smoke: `morrow/rehearsal/website-smoke/production-main-b76f6ba.json`.

The smoke confirms:

- `main` deployment on Vercel is `READY`.
- `https://copperlinehosting.com/game-servers.php` returns the Copperline entry clues.
- `https://copperlinehosting.com/recovery/mossfield/console` returns the G01-G15 local console.
- `https://copperlinehosting.com/api/rehearsal/morrow/full` returns the local full-spine contract with
  production and director mutation disabled.
- Vercel runtime errors were clean for the checked one-hour window.

## Media Intake

When media is made, record it in `morrow/media/media-manifest.template.json` or a copy of that
structure. The manifest now covers all twelve assets with explicit work folders, acceptance checks,
source/final hash slots, accessibility files, custody notes, and safety-review booleans. Do not mark an
asset release-ready until source and final hashes are known, the accessibility equivalent exists, and
every safety-review field has been deliberately reviewed.
