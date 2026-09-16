# The Observance Dashboard

Next.js 16 public Copperline surface plus local Morrow reboot review and rehearsal APIs.

## Current Reboot Surfaces

The first playable Morrow/Copperline slice is implemented as static or local-only review routes:

- `/game-servers.php` starts the ordinary legacy-hosting trail and exposes the retired managed-world
  residue for G01.
- `/community/forum` carries the forum archaeology and Iona continuity clue for G02.
- `/support/tickets/6118` carries Theo's account ticket and Mossfield recovery motive for G03.
- `/status/incident-6118` preserves the later incident continuity surface.
- `/recovery/mossfield` is the disabled production recovery landing.
- `/recovery/mossfield/console` renders the local G01-G15 rehearsal model.
- `/recovery/mossfield/gates/[gateId]` renders a per-gate rehearsal card for `g01` through `g15`.
- `/api/rehearsal/morrow/full` returns the full local receipt model.
- `/api/rehearsal/morrow/gates/[gateId]` returns a single gate receipt.
- `/api/rehearsal/morrow/media` returns media readiness state.
- `/api/rehearsal/morrow/director` returns the locked director contract.
- `/review/morrow` remains a spoiler-full design review dossier.

All reboot review/rehearsal routes are non-mutating. They do not authenticate against production,
write Supabase, contact Discord, alter Minecraft, or unlock live director behavior.

## Historical Routes

Older Record, director, support-case, and V5-era routes may still exist as engineering references or
inactive compatibility surfaces. They are not current reboot authority and must not be treated as
release instructions unless a `morrow/` authority document explicitly remaps them to G01-G15.

## Environment

The reboot slice can build without live production secrets. Production integrations remain gated and
disabled until the readiness matrix is satisfied.

Historical environment variables may still be present for archived surfaces:

```dotenv
NEXT_PUBLIC_SUPABASE_URL=https://<project-ref>.supabase.co
NEXT_PUBLIC_SUPABASE_ANON_KEY=<anon key>
MORROW_RELEASE_ID=<current-rehearsal-release-id>
SUPABASE_SERVICE_ROLE_KEY=<service-role secret>
ADMIN_EMAILS=<comma-separated operator addresses>
AUTHOR_USERNAME=<private Basic-auth username>
AUTHOR_PASSWORD=<unique high-entropy Basic-auth password>
```

Do not add or rotate production secrets merely to test the current reboot slice.

## Build and Verification

```powershell
npm.cmd ci
npm.cmd run lint
npm.cmd run build
npm.cmd run morrow:reboot-selftest
```

From the repository root, also run:

```powershell
python tools\check_morrow_g01_g05_vertical_slice.py
python tools\check_morrow_full_rehearsal.py
python tools\check_morrow_local_contracts.py
python tools\check_morrow_production_readiness.py
```

Preview locally:

```powershell
npm.cmd run dev -- -p 3030
```

Open `http://localhost:3030/game-servers.php` and follow
`../morrow/rehearsal/FIRST-PLAYABLE-RUNBOOK.md`.
