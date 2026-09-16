# The Observance

The active project is the **Morrow / Copperline reboot**: a traditional layered Minecraft ARG and
found-footage mystery built around an old hosting company, a recovered survival world, Discord
fragments, authored media, and one terminal shutdown ending.

This repository no longer treats the prior Hold / Averyn / Wren / Unlit / V5 campaign as current
authority. Historical material may remain where it is useful engineering evidence, but new planning,
implementation, reviews, and deployments must follow the Morrow reboot documents below.

## Current Authority

Start here:

| Path | Role |
| --- | --- |
| `morrow/README.md` | Reboot authority order and design rules |
| `morrow/authority/ARG-GUIDEBOOK.md` | Creative promise, player contract, puzzle language, safety, ending |
| `morrow/authority/CANON.md` | Truth, characters, Morrow rules, and allowed ambiguity |
| `morrow/authority/PLAYER-JOURNEY.md` | Act order, discoveries, consequences |
| `morrow/authority/PUZZLE-LEDGER.json` | 24 discoveries and 15 required gates |
| `morrow/contracts/` | Event, relationship, media, and surface contracts |
| `morrow/architecture/` | System, database, Minecraft UX, operations, and capability decisions |
| `morrow/rehearsal/FIRST-PLAYABLE-RUNBOOK.md` | Current first playable test guide |
| `morrow/rehearsal/PRODUCTION-READINESS-MATRIX.md` | What is green and what still blocks launch |
| `morrow/media/MEDIA-PRODUCTION-PACKET.md` | Final media production list and custody requirements |

If these files disagree, earlier authority wins. Update dependent contracts in the same change as any
authority change.

## Current Build

The first playable reboot slice is implemented for local and Vercel review:

- G01-G03 Copperline web trail routes.
- G01-G05 local gate/discovery receipt chain.
- G01-G15 local planning and QA model.
- Locked director contract.
- Rehearsal API routes for gates, media, full spine, and director state.

It is review/test ready, not launch ready. Production mutation remains disabled until media,
Minecraft, Discord, Supabase, and director lanes have fresh G01-G15 proof.

## Repository Map

| Path | Current use |
| --- | --- |
| `dashboard/` | Next.js Copperline site, review surfaces, rehearsal APIs, and locked operator UI |
| `morrow/` | Reboot authority, contracts, architecture, rehearsal receipts, media plan |
| `plugin/` | Paper plugin infrastructure and reusable runtime patterns |
| `discord/` | Discord worker and database tooling to be remapped to the reboot |
| `datapack/` | Minecraft deployment assets; reboot activation remains gated |
| `resourcepack/` | Client assets pipeline; no mandatory reboot pack is enabled |
| `tools/` | Audits, rehearsals, build checks, readiness checks |
| `design/` and `arc/` | Historical design/lore unless explicitly adopted by `morrow/` authority |

## Local Test Commands

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

To preview locally:

```powershell
cd dashboard
npm run dev -- -p 3030
```

Open `http://localhost:3030/game-servers.php`.

## Deployment

Vercel builds from the GitHub integration. Pushes to `main` are the production path; feature branches
produce protected preview deployments. The production domain is expected to remain the Copperline
domain configured in Vercel.

Before promoting or treating a build as production-ready, run the commands above and test the flow in
`morrow/rehearsal/FIRST-PLAYABLE-RUNBOOK.md`.

## Historical Boundary

Old V5 runbooks, receipts, and code are not launch instructions. They may be referenced only for
implementation patterns after a new G01-G15 mapping and fresh verification. Do not relabel old proof as
new proof, and do not re-enable old progression routes as compatibility shortcuts.
