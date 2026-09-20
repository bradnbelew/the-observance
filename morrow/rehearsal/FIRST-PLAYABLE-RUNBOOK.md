# Morrow First Playable Runbook

Status: local rehearsal package for the first ARG slice. This is ready for review and test, but not production-enabled.

## What Is Ready

- G01-G03 have local Copperline website routes.
- G01-G06 have a validated local receipt chain. G04-G06 are implemented in a coherent local Paper
  world: arrival, Cairn's storehouse routine, and Rookery's maintenance cache.
- G01-G15 have a validated full-spine local model for planning and QA.
- Media requirements are enumerated, but final handmade media files are not yet authored or hashed.
- Director controls are explicitly locked; no production mutation path is enabled.

## Website Routes To Test

Run the dashboard locally, then visit:

- `/game-servers.php`
  - First Copperline hosting brochure surface.
  - Expected clues: retired managed-world product slug, plan residue, source comment path.
- `/community/forum`
  - G02 dead forum account surface.
  - Expected clues: Iona alias continuity and damaged avatar/cache language.
- `/support/tickets/6118`
  - G03 support-ticket surface.
  - Expected clues: Theo voicemail transcript, billable-account language, Mossfield recovery handoff.
- `/status/incident-6118`
  - Incident surface for later continuity.
  - Expected clue: stable uptime value.
- `/recovery/mossfield`
  - Recovery landing surface.
  - Expected clue: local-only recovery shell with production disabled.
- `/recovery/mossfield/console`
  - Full local rehearsal console.
  - Expected result: all G01-G15 gates listed with hash-chain receipt head.
- `/recovery/mossfield/gates/g01`
  - Per-gate detail route. Replace `g01` with `g02` through `g15`.
- `/api/rehearsal/morrow/full`
  - Full JSON receipt.
- `/api/rehearsal/morrow/gates/g01`
  - Per-gate JSON receipt. Replace `g01` with any gate through `g15`.
- `/api/rehearsal/morrow/media`
  - Media readiness JSON.
- `/api/rehearsal/morrow/director`
  - Locked director contract.

## Verification Commands

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
```

Expected current result:

- Reboot selftest: pass.
- Lint: pass.
- G01-G05 local slice: pass.
- Full-spine local check: pass.
- Local contracts check: pass.

## Current Local Preview

This worktree now has local dashboard dependencies, and `npm run build` passes. To preview the first playable website locally, run:

```powershell
npm run dev -- -p 3030
```

Then open `http://localhost:3030/game-servers.php`.

The latest built-server smoke receipt is retained at `morrow/rehearsal/website-smoke/latest.json`.

## Minecraft Playtest

The rebuilt local world is now player-facing through G06. Follow
`morrow/rehearsal/MOSSFIELD-HANDS-ON.md` from first connection through the maintenance cache. The old
Copper Terminal, B06 classification prompt, and repeated recovery rooms are regression fixtures only;
they are not the rebooted player experience and must remain disabled.

## Media Needed

No finished media files are required for the current G01-G03 static web review. The routes carry text equivalents so the first pass can be tested without audio/video/PDF production.

For full release, make the twelve first-party assets listed in `morrow/media/MEDIA-PRODUCTION-PACKET.md`:

- `morrow.g01.retired_hosting_brochure`
- `morrow.g02.forum_avatar_contact_sheet`
- `morrow.g03.theo_account_voicemail`
- `morrow.g06.rookery_notebook`
- `morrow.g07.june_lighthouse_photo`
- `morrow.g08.frame_thirty_seven`
- `morrow.g09.current_session_contamination`
- `morrow.g09.patchcord_cassette_dub`
- `morrow.g11.incident_raw_export`
- `morrow.g12.iona_reversed_memo`
- `morrow.g12.spectrogram_command`
- `morrow.g15.shutdown_record`

Each final asset needs:

- Original source file.
- Final delivery file.
- SHA-256 hash for both original and delivery file.
- Custody note.
- Creation date and author identity.
- Plain-text accessibility equivalent.
- Confirmation that the asset does not use real private data, real phone numbers, or real people.

## Playtest Scope

Use this slice to test whether a player can move from ordinary Copperline residue into a believable
Mossfield and investigate it without puzzle-room framing:

1. Find the retired managed-world residue.
2. Connect the dead forum account to Iona.
3. Read Theo's support-ticket motive.
4. Reach the Mossfield recovery surface.
5. Join Mossfield at the freight stop as a non-op player.
6. Recover Cairn's storehouse routine from the place and its objects rather than an answer menu.
7. Follow the revealed coordinates to Rookery's maintenance cache.
8. Verify the local journal records G04-G06 and preserves the shutdown-only ending boundary.

G04-G06 are locally playable. G07-G15, Discord, production projection, and director controls are not
launch-ready.
