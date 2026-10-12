# Morrow v2: current authority

This is the active design and build for The Observance, approved on 2026-10-12. It
supersedes `morrow/authority/*`, the G01-G15 spine, and everything older. Older files stay in
the repository as history only.

## Read in this order

| File | What it is |
| --- | --- |
| `morrow/v2/STORYLINE.md` | The approved 10-act storyline, the cast, and the hidden truth |
| `morrow/v2/THEO-AND-CIPHER-BANK.md` | The prerecorded Theo (Archive Desk) and all 35 ciphers, mapped to acts |

Where a design doc and `morrow/canon/` disagree on an exact value (a time, a filename, a coordinate), **the canon wins**. The canon was written after the docs and is verified.
| `morrow/canon/` | **The single source of truth.** Every log, post, record and cipher is generated here |
| `morrow/v2/OPERATOR-RUNBOOK.md` | How to run each act |
| `morrow/v2/BETA-WORLD-GUIDE.md` | The owner's part: building 2011 Mossfield in Beta 1.5 |
| `morrow/v2/SHOT-LIST.md` | June's screenshots |
| `morrow/v2/RECORDING-SCRIPTS.md` | Recordings and every Theo voice note |

## The rule that keeps it consistent

Never hand-edit generated files. Change `morrow/canon/*.py`, then run:

```powershell
python -m morrow.canon.build
python -m morrow.canon.verify
python -m morrow.world.selftest
```

`build` regenerates the site bundles (`dashboard/src/lib/canon/`) and the plugin canon
(`plugin-v2/src/main/resources/canon/`). `verify` proves every cipher round-trips and enforces
the 2011 writing rules. `world.selftest` proves the injector writes decodable objects into a
Beta world.

## What's built (2026-10-12)

| Piece | State |
| --- | --- |
| Canon generator and verification | Done. All cipher artifacts for all 10 acts generated and verified |
| Copperline mirror, Acts 1-3 | Done. Forum import, client area with reset, status, /~mossfield (Dynmap, LogBlock, logs), June's folder, robots, locked TCAdmin, Archive Desk acknowledgement |
| Beta world tools | Done. NBT/McRegion, terrain renderer, cipher injector, round-trip test |
| mrrw plugin, Acts 1-3 | Done. Server-list hex channel, Morrow's chat, dawn restore/normalise/tidy, Act 2-3 world changes. Tested on local Paper |
| The Beta 1.5 world | **Waiting on the owner** (BETA-WORLD-GUIDE.md) |
| June's screenshots, recordings | **Waiting on the owner** (SHOT-LIST.md, RECORDING-SCRIPTS.md) |
| Acts 4-10 (Archive Desk replies, Discord bot, replays, ciphers C/D in world, finale) | Not started. The plan is to playtest Acts 1-3 with one friend first |

## Code map

- `dashboard/`: the Copperline mirror (Next.js 16). `MORROW_ACT` env sets the act.
- `plugin-v2/`: mrrw, the Paper 1.21.11 plugin. `/mrrw act N` sets the act in game.
- `morrow/canon/`: the generator. `morrow/world/`: Beta world tools.
- `plugin/`, `discord/`, `morrow/authority/`, `design/`, `arc/`: legacy. Do not build on them.
