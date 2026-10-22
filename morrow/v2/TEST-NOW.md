# Test it now

Everything below runs on your PC: the local Paper test server runs the real, upgraded Mossfield. The answers are in `morrow/v2/SOLUTIONS.md`.

## The website, all ten acts

Start the local preview. In the Claude app it's the **copperline-v2** preview; from a terminal:

```bash
node dashboard/scripts/dev-v2.mjs
```

Open http://localhost:3040. A yellow **TEST act** switcher sits bottom-right: click 1-10 to jump
between acts. Theo answers instantly in this mode, which production never does.

Things to try, in order:

1. **Act 1.** `/robots.txt`, then view the source of `/game-servers.php`, then the forum. Use
   "Forgot password?" with username `rookery`.
2. **Act 3.** `/~mossfield/logblock` (search player `morrow`). Open `/~juni_lantern/`; it asks for a
   password.
3. **Act 4.** Archive Desk (top-right "Archive Desk"): file a request mentioning `lot 14` and `CHI-07`.
4. **Act 6.** Log into `/tcadmin/legacy`. Open `/forum-import/showthread.php?t=4471`, and its
   "Printable version". Visit `/~tvale/`.
5. **Act 8.** Ask the desk where "turn it off, its ok" came from.
6. **Act 9.** Download both zips from `/ftp/` (log into the client area first) and compare MD5s.
   Submit `/support/brief.php`.
7. **Act 10.** The console on `/tcadmin/legacy`. It checks the Minecraft server, so start that first.

## The Minecraft server, with the plugin

```bash
cd build/mrrw-v2-test
java -Xmx3G -jar paper.jar --nogui
```

Join with **Minecraft Java 1.21.11** at `localhost:25599`. The server is offline-mode, so any
account works. In the server console, give yourself operator with `op YourName`. Then in game:

| Command | What it does |
| --- | --- |
| `/mrrw status` | act, finale state, lowpath |
| `/mrrw act 5` | jump to an act |
| `/mrrw replay` | play the 18 May 2011 incident now (Act 5+) |
| `/mrrw dawn` | run the dawn tidy now |
| `/time set 13000` | night: lowpath walks (Act 8+), doors open |

Places worth visiting (the real Mossfield, from `morrow/canon/layout.py`):

| Place | Coordinates |
| --- | --- |
| Spawn rules signs | `-236 71 25` |
| Rookery's wool floor | `-270 62 -5` |
| Theo's museum, reading room, map wall | `-252 70 -30`, `-244 70 -30`, `-236 70 -30` |
| June's tower and flags | `-243 66 249` (flags at height 87) |
| lowpath's place | `-213 73 86` (on Morrow's hill; the seed's own ground there is y 69) |
| finch's lock | `-224 70 -116` |
| The maintenance annex and dust corridor | `-286 35 28`, `-286 35 36` |
| The meadow (hold a map in the 128-block cell around it) | `-180 71 55` |

## The automated checks

```bash
python -m morrow.canon.verify
python -m morrow.world.selftest
python -m morrow.world.e2e_site
npm --prefix discord-v2 run dry
```

The site test needs the preview running. To rebuild the stand-in world from scratch, run
`python -m morrow.world.pipeline --synthetic`, delete `build/mrrw-v2-test/plugins/mrrw/`, then start
Paper with `--forceUpgrade` and run `/mrrw install`.
