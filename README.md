# The Observance

**Current authority: [`morrow/v2/README.md`](morrow/v2/README.md)** (Morrow v2, ten acts, approved
2026-10-12). Everything else outside `morrow/v2/` (the G01-G15 reboot docs under `morrow/authority`,
`morrow/contracts`, `morrow/rehearsal`, the Hold / Averyn / Wren / V5 material, `tools/check_morrow_*`)
is history and is not built or tested any more.

| Need | Go to |
| --- | --- |
| What the game is, act by act | `morrow/v2/README.md`, `morrow/v2/STORYLINE.md` |
| Running it on game night | `morrow/v2/OPERATOR-RUNBOOK.md` |
| What's still yours to do before launch | `morrow/v2/HUMAN-TODO.md` |
| Testing locally | `morrow/v2/TEST-NOW.md` |
| Answer key (spoilers) | `morrow/v2/SOLUTIONS.md` |

Code: `morrow/canon` (the canon generator: `python -m morrow.canon.build`, `.verify`), `morrow/world`
(world tools and end-to-end checks), `plugin-v2` (the Paper plugin `mrrw`), `plugin-trailhead`
(`mrrw-01`, Act 0), `dashboard` (the Copperline site, Next.js), `discord-v2` (the bridge bot).
