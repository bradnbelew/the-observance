# Your part: everything that needs a human

Everything else is built and tested. This is the complete list of what only you can make or do,
in a sensible order. Each item says exactly where the result goes. When a file lands in the right
place, the website, plugin or bot picks it up with no code changes.

Paths are relative to `D:\the-observance\`.

## 1. Build Mossfield in Beta 1.5 (the big one)

Follow `morrow/v2/BETA-WORLD-GUIDE.md`. In short:

1. Launcher -> Installations -> new, version `old_beta b1.5_01`. Create world `Mossfield`, seed `bracken`.
2. Copy it to `world\beta\Mossfield` and tell me. I map the real terrain and send numbered plots.
3. Build the residents' places by hand in 2011 blocks only.
4. **Kit's walk:** walk (don't fly) about 800 blocks out on the route I mark, and dig a tiny dirt hut
   there. Walking generates the "fossil" chunks that puzzle D3 depends on.
5. Hand it back. I run the pipeline, which injects the ciphers and upgrades through 1.2.5, 1.12.2
   and 1.21.11:

```bash
python -m morrow.world.pipeline --beta "world/beta/Mossfield"
```

## 2. June's screenshots (in Beta, while you build)

Follow `morrow/v2/SHOT-LIST.md`. Rename each file exactly, and put them in
`dashboard/private-media/media/june/`.

## 3. Recordings

Scripts are in `morrow/v2/RECORDING-SCRIPTS.md`.

| File | Save as | Notes |
| --- | --- | --- |
| patchcord's Mumble night | `dashboard/private-media/files/moss_0518.ogg` | 4-5 voices; Theo hands it over at the Archive Desk |
| patchcord's open mic | `dashboard/private-media/media/ftp/incoming/MIC-5-18-B.WAV` | Theo faint through headphones |
| Theo's 2011 voicemail | `dashboard/private-media/files/theo_voicemail_2011.wav` | **through a real phone line**, says "mate" |
| Optional decoys | `dashboard/private-media/media/patchcord/moss_0109.ogg`, `moss_0214.ogg`, `moss_0412.ogg` | 1-3 min of chatter each |

**Then make Morrow's fake Theo note** (Act 9) from the voicemail. Note the second ranges where you
say "no worries at all" and "keep it billable till someone comes home", then run:

```bash
python -m morrow.canon.splice dashboard/private-media/files/theo_voicemail_2011.wav 1.2-2.4 9.8-12.6
```

## 4. Theo's voice notes (2026 Theo: quieter, never says "mate")

Save each as `dashboard/private-media/theo/<ID>.ogg` (or `.mp3`, `.m4a`, `.wav`). The text is in
RECORDING-SCRIPTS.md. If a note is missing, the page still shows the words.

| ID | Where it plays |
| --- | --- |
| `T-01` | Archive Desk welcome (Act 1) |
| `T-04a`, `T-04b` | unmasking reply, deflection (Act 4) |
| `T-05a` | handing over patchcord's recording (Act 5) |
| `T-06a` | the 06:44 confession (Act 6) |
| `T-07a`, `T-07b`, `T-07c` | his real Discord DMs (Act 7) |
| `T-08a` | after the 36-hour silence, about Kit (Act 8) |
| `T-09a`, `T-09b` | case brief accepted / not yet (Act 9) |
| `T-10-booked`, `T-10-cut` | window booked; cutting the bridge (Act 10) |
| `T-10-clean`, `T-10-recovered`, `T-10-corpus` | his last message, per USB choice (Act 10) |

## 5. Photos

- **The closet photo (Act 8):** an old PC in a cupboard, with a wall calendar open to **December 2014**
  and a sticky note on the case reading **`chi07-`**. Save as `dashboard/private-media/files/closet.jpg`.
  **Turn off location tagging first**, or strip the location from the file. The date the phone
  records is fine.
- **Morrow's forged screenshot (Act 6):** in modern Minecraft with the Programmer Art pack, at night,
  facing **east** with a **crescent** moon, and a figure in lowpath's skin on June's dock. Save as
  `dashboard/private-media/media/june/2011-05-17_22.05.12.png`.

## 6. Skins for the replayed figures

The PNGs are generated in `morrow/v2/skins/` (`lowpath.png` is the stitched one). For each:

1. Upload it at mineskin.org.
2. Copy the texture **value** and **signature**.
3. Paste them into `plugins/mrrw/config.yml` under `skins:` on the server.

Without this the figures wear the default skin and still work.

## 7. Discord

1. Create a server called **Mossfield Recovery (archive)** with channels `#general` and `#bridge`,
   and a role called **Resident**. Give each player that role, and ask them to set their server
   nickname to their **Minecraft name**.
2. Discord Developer Portal -> New Application -> Bot. Turn on **Server Members Intent**. Invite it
   with permission to send messages, attach files and pin messages.
3. Create `discord-v2/.env` (see `discord-v2/README.md`) with the token, server and channel IDs, and
   the path to the server's `plugins/mrrw` folder and `logs/latest.log`.
4. Run it next to the Minecraft server:

```bash
npm --prefix discord-v2 start
```

## 8. Go live

1. **Minecraft host:** upload the upgraded `Mossfield` world and `mrrw-2.0.0.jar`
   (from `plugin-v2/build/libs/`). Set `stop-secret` in `plugins/mrrw/config.yml` to a long random
   string. Then run `/mrrw install` once.
2. **Vercel** (production project), environment variables:

| Variable | Value |
| --- | --- |
| `MORROW_ACT` | `1` |
| `MORROW_ACT_STARTED` | the time you send the trailhead, ISO format |
| `MORROW_SESSION_SECRET` | a long random string |
| `MORROW_STOP_SECRET` | the same string as `stop-secret` in the plugin config |
| `MORROW_MC_HOST` | `your.server.address:25565` |
| `MORROW_PLAYERS` | comma-separated Minecraft names |

   Never set `MORROW_DEV` or `MORROW_DESK_FAST` in production.
3. **Merge the branch** `morrow-v2/storyline` into `main` when you're happy. That's what deploys
   copperlinehosting.com. The current live site is still the old broken version.

## 9. Playtest

Run Acts 1-3 with one friend before inviting the group. Watch quietly and note where they get
stuck. `morrow/v2/OPERATOR-RUNBOOK.md` covers running each act; `morrow/v2/SOLUTIONS.md` is the
answer key.
