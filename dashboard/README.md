# Copperline mirror (Morrow v2)

Next.js 16 site for The Observance: the preserved Copperline Hosting customer site (2011-2014), now
carrying all ten acts of Morrow v2. Design authority is `../morrow/v2/README.md`; the answer key is
`../morrow/v2/SOLUTIONS.md`; how to try it locally is `../morrow/v2/TEST-NOW.md`.

## How it works

- **Act gating.** `MORROW_ACT` (1-10) picks which generated canon bundle the site serves
  (`src/lib/canon/act-NN.json`, built by `python -m morrow.canon.build`; never hand-edit). A bundle only
  holds what a player could reach by that act, so a page can't leak a later act by accident.
- **Basic-auth folders.** `src/proxy.ts` puts residents' web folders (`/~juni_lantern`, `/~patchcord`,
  `/~tvale/box`) behind Apache-style Basic auth, from `src/lib/canon/folders.json`. A folder that
  isn't open yet answers 404. Failed logins are throttled per IP (best effort, in memory).
- **Private media.** Evidence files live in `private-media/` (tracked in git, never under `public/`)
  and are streamed by gated routes with byte-range support:
  - `private-media/files/`: generated files and Theo's drops (`/desk-file/<signed token>`, `/media/tvale*/`, `/ftp/`)
  - `private-media/media/june|patchcord/`: owner-made screenshots and recordings (`/media/...`)
  - `private-media/media/ftp/incoming/MIC-5-18-B.WAV`: the Act 8 FTP file
  - `private-media/theo/<ID>.ogg|mp3|m4a|wav`: Theo's voice notes (`/desk-voice/<signed token>`)
- **Archive Desk.** `/support/index.php` and `/viewticket.php`: Theo is prerecorded. Tickets are
  signed tokens (no database); replies are chosen by evidence tokens in what players file.
- **The bridge (optional, `MORROW_BRIDGE_URL`).** `src/lib/morrow/bridge.ts` asks the mrrw plugin for its
  state (signed with `MORROW_STOP_SECRET`, 2.5 s timeout, cached 25 s, never throws to a page). When it
  answers, the act is the later of `MORROW_ACT` and the plugin's act, so acts advance without a redeploy;
  the plugin's `act_started` drives Theo's drops and hints; hint timers (24/72/120/168 h) scale with the
  number of residents (x0.5 for two or fewer, x0.65 for three, x0.8 for four); and the Act 10 console
  only accepts the group's USB vote winner. Desk filings that move the story are POSTed to the plugin
  (`/event`: found_morrow, unmask, floor, moss_0518, restart, key, ingest, kit, brief, booking, ...).
  Unset or unreachable: everything comes from the env, as before.
- **Act 10.** `/tcadmin/legacy` (Rookery's client login, Act 6+) carries the Saturday maintenance
  booking and the stop console. The console pings the Mossfield server (`MORROW_MC_HOST`) and issues a
  stop code signed with `MORROW_STOP_SECRET`, which the mrrw plugin checks. `/status` and
  `/status/heartbeat.json` report "no heartbeat" once the process has stopped.

## Main routes

| Route | What |
| --- | --- |
| `/`, `/game-servers.php`, `/server-list.php`, `/announcements.php` | Copperline storefront (Act 1 trail starts at `/robots.txt` and the source of `/game-servers.php`) |
| `/community/index.php` | Points at the forum import |
| `/forum-import/index.php`, `forumdisplay.php`, `showthread.php`, `member.php`, `memberlist.php` | vBulletin forum import |
| `/clientarea.php`, `/pwreset.php`, `/dologin.php`, `/dopwreset.php`, `/logout.php` | Rookery's WHMCS client area |
| `/support/index.php`, `/submitticket.php`, `/viewticket.php`, `/support/brief.php`, `/dobrief.php` | Archive Desk and the Act 9 case brief |
| `/desk-file/[token]`, `/desk-voice/[token]` | Signed attachments and voice notes |
| `/~mossfield/` (`dynmap`, `mapkey.txt`, `logs`, `logblock`) | Rook's server pages |
| `/~juni_lantern/`, `/~patchcord/`, `/~tvale/` (`box/`) | Residents' folders (`/u/[user]` behind a rewrite) |
| `/media/[...path]`, `/ftp/[[...path]]` | Folder files and Rookery's FTP |
| `/status`, `/status/heartbeat.json`, `/status/incident-6118` | Network status |
| `/tcadmin/legacy` (`login`, `book`, `console`) | TCAdmin panel, Act 6+ / Act 10 |
| `/dev-act` | Local testing only (`MORROW_DEV=1`): switches the act for this browser |

## Environment

See `.env.example`. Production needs `MORROW_ACT`, `MORROW_ACT_STARTED` (with a UTC offset),
`MORROW_SESSION_SECRET`, `MORROW_STOP_SECRET`, `MORROW_MC_HOST` and `MORROW_PLAYERS`, plus
`MORROW_BRIDGE_URL` for the auto-advancing campaign (the plugin's bridge port must be reachable from Vercel). A production
server refuses to sign sessions or stop codes while either secret is unset or a dev default.

At start the server logs `MORROW stop-secret fingerprint xxxxxxxx`; it must match the fingerprint
`/mrrw status` prints in game before the finale. **Never set `MORROW_DEV` or `MORROW_DESK_FAST` in
production.**

## Build and test

```powershell
npm.cmd ci
npm.cmd run lint
$env:OBSERVANCE_NEXT_DIST_DIR=".next-v2"; $env:OBSERVANCE_TSCONFIG="tsconfig.local-build.json"; npm.cmd run build
node scripts/dev-v2.mjs        # local test preview on :3040 (act switcher, instant Theo)
```

From the repository root, with the preview running:

```powershell
python -m morrow.canon.verify
python -m morrow.world.e2e_site
```
