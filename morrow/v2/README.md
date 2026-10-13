# Morrow v2: current authority

This is the active design and build for The Observance, approved on 2026-10-12. It
supersedes `morrow/authority/*`, the G01-G15 spine, and everything older. Older files stay in
the repository as history only.

## Read in this order

| File | What it is |
| --- | --- |
| `morrow/v2/STORYLINE.md` | The approved 10-act storyline, the cast, and the hidden truth |
| `morrow/v2/THEO-AND-CIPHER-BANK.md` | The prerecorded Theo (Archive Desk) and all 35 ciphers, mapped to acts |
| `morrow/canon/` | **The single source of truth.** Every log, post, record and cipher is generated here |
| `morrow/v2/OPERATOR-RUNBOOK.md` | How to run each act |
| `morrow/v2/BETA-WORLD-GUIDE.md` | The owner's part: building 2011 Mossfield in Beta 1.5 |
| `morrow/v2/SHOT-LIST.md` | June's screenshots |
| `morrow/v2/RECORDING-SCRIPTS.md` | Recordings and every Theo voice note |

Where a design doc and `morrow/canon/` disagree on an exact value (a time, a filename, a
coordinate), **the canon wins**. The canon was written after the docs and is verified.

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

**All ten acts are built and tested on a stand-in world.** What's left needs a human: see
`HUMAN-TODO.md`. To try it today: `TEST-NOW.md`. Answer key: `SOLUTIONS.md` (generated).

| Piece | State | Proof |
| --- | --- | --- |
| Canon generator | Done, all acts | `python -m morrow.canon.verify` (every cipher, writing rule, anchor) |
| Copperline mirror | Done, Acts 1-10 | `python -m morrow.world.e2e_site`: 39/39 |
| World tools + upgrade chain | Done | `python -m morrow.world.selftest`; pipeline.py verified 2011 objects intact after 1.2.5 -> 1.12.2 -> 1.21.11 |
| mrrw plugin | Done, Acts 1-10 | `gradlew selftest`; live tests on local Paper (lock, lowpath, finale) |
| Act 10 across both systems | Done | `python -m morrow.world.e2e_finale`: console -> ping -> code -> stop -> no heartbeat |
| Discord bot | Done | `npm run dry` (live needs your bot token) |
| Act 0 trailhead (`plugin-trailhead/`, mrrw-01 riding on Marked) | Done | `gradlew selftest`; `e2e/trailhead.mjs` 21/21 on the Marked QA server; Marked's own `match.mjs` 18/18 with it armed |
| Beta world, screenshots, recordings, photos, skin uploads | **Yours** | `HUMAN-TODO.md` |

## Where the build differs from the design docs

- **D7 (semaphore):** I couldn't verify semaphore arm positions in text, so PORCHLIGHT is spelled
  entirely in maritime signal flags (with K above). Armor stands are not used.
- **D13 (light corridor):** light bleeds between neighbouring tiles, so the levels are carried by
  redstone dust power (0-15, visible in F3 and by brightness), not light blocks.
- **C19 (Thumbs.db):** replaced by an EXIF thumbnail. Theo's cropped JPEG still carries the
  uncropped original as its embedded thumbnail. Same forensic idea, simpler for players' tools.
- **C3 (finch's commute):** not built (rail switching needs your hand-built rail network). The
  floor plaintext is reachable without it.
- **The checklist** reaches players as a Theo drop in Act 5. The closet photo opens Theo's `box/`
  folder (`chi07-` + 2010) in Act 8, instead of the changelog being a free drop.
- **Hints** are text only. The generic H-1 to H-6 voice notes aren't used.

## Code map

- `dashboard/`: the Copperline mirror (Next.js 16). `MORROW_ACT` env sets the act. Evidence files in `private-media/`.
- `plugin-v2/`: mrrw, the Paper 1.21.11 plugin. `/mrrw act N` sets the act in game.
- `morrow/canon/`: the generator (+ files, skins, splice, solutions). `morrow/world/`: Beta tools, pipeline, e2e tests.
- `discord-v2/`: the bridge bot.
- `plugin/`, `discord/`, `morrow/authority/`, `design/`, `arc/`: legacy. Do not build on them.
