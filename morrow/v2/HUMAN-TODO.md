# Your part: everything that needs a human

Everything else is built and tested. This is the complete list of what only you can make or do,
in a sensible order. Each item says exactly where the result goes. When a file lands in the right
place, the website, plugin or bot picks it up with no code changes.

Paths are relative to `D:\the-observance\`.

## 1. Mossfield in Beta 1.5

1. ~~Create world `Mossfield`, seed `bracken`, copy to `world\beta\Mossfield`.~~ **Done 2026-10-12.**
2. ~~Claude builds the residents' places in 2011 blocks.~~ **Done 2026-10-12** (`morrow/world/build2011.py`;
   map in `morrow/v2/BETA-WORLD-GUIDE.md`).
3. **Kit's walk (yours, about 5 minutes):** Singleplayer -> **Mossfield (Kit walk)**, walk to
   x 330, z -530, build a dirt hut with the door you're carrying. Steps in BETA-WORLD-GUIDE.md.
4. Claude runs the pipeline, which injects the ciphers and upgrades through 1.2.5, 1.12.2 and
   1.21.11:

```bash
python -m morrow.world.pipeline --beta "world/beta/Mossfield"
```

## 2. June's screenshots (in Beta, while you build)

Claude prepares one save per shot (build at the right stage, right time of day, you already
standing on a spot with a clear view, facing the subject). Each has a preview of what the frame
should look like in `morrow/v2/shot-previews/`. Load it, press F1 then F2, quit; take them 1 to 9
in order and Claude renames and files them (details in `morrow/v2/SHOT-LIST.md`).

## 3. Recordings

Scripts are in `morrow/v2/RECORDING-SCRIPTS.md`.

| File | Save as | Notes |
| --- | --- | --- |
| patchcord's Mumble night | `dashboard/private-media/files/moss_0518.ogg` | 4-5 voices; Theo hands it over at the Archive Desk |
| patchcord's open mic | `dashboard/private-media/media/ftp/incoming/MIC-5-18-B.WAV` | Theo faint through headphones |
| Theo's 2011 voicemail | `dashboard/private-media/files/theo_voicemail_2011.wav` | **through a real phone line**, says "mate" |
| Optional decoys | `dashboard/private-media/media/patchcord/moss_0224.ogg`, `moss_0313.ogg`, `moss_0412.ogg` | 1-3 min of chatter each |

The Mumble night is now about **21 minutes** (mostly dead air between 04:30 and 16:30, so the
kill lands at 06:17:22 like every other record), and Theo isn't on it.

**Then make Morrow's fake Theo note** (Act 9) from the voicemail. Note the second ranges where you
say "no worries at all, mate" and "keep it billable till someone comes home", then run this with
**your own** ranges (the numbers below are only an example):

```bash
python -m morrow.canon.splice dashboard/private-media/files/theo_voicemail_2011.wav 1.2-2.4 9.8-12.6
```

**You're voicing Theo yourself, so disguise it.** Your friends know your voice; the Kit
confession in particular dies if they recognise you.

- Use a different register and pace from how you talk to them: 2011 Theo fast and chirpy, 2026
  Theo slow, soft, a little hoarse. Pick one physical change and keep it for every take (sit back
  from the phone, talk slightly lower, smile on the 2011 lines).
- Drop your own verbal habits (your usual "like", "dude", laugh). Theo's tics are "honestly",
  "no worries" and trailing off.
- Don't pitch-shift or add effects beyond the real phone line for the voicemail. Processing sounds
  fake, and real Theo is the one voice the game says is authentic.
- Play one take to someone who isn't in the game before recording the rest.
- The Mumble night needs 4-5 voices. Recruit people who aren't playing for Rook, cairn, June
  and Iona; you take patchcord (he barely talks).

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
3. The bot runs on **Railway** and reaches the Minecraft server through the plugin's bridge, so
   nothing runs on the server machine besides Minecraft. Ask whoever runs that machine to open
   **TCP 25588** (the site needs it too). Then follow "Railway" in `discord-v2/README.md`: deploy the
   repo with config file `/discord-v2/railway.json`, paste the seven variables (token, server, channel
   and role IDs, `MORROW_BRIDGE_URL=http://<minecraft host>:25588`, `MORROW_STOP_SECRET`, the same as
   the site and plugin), and add a volume at `/data`.
4. Ask players to allow DMs from server members: Morrow's and Theo's DMs, and the "is it really
   Theo?" corpus replies in Acts 7-9, go there. The Act 10 USB vote happens with buttons in #general,
   and Theo's big replies get posted there too. To check the bot without Discord:

```bash
npm --prefix discord-v2 run dry
```

## 8. Go live

1. **Minecraft host:** build the plugin first (`plugin-v2\gradlew.bat build`), then upload the
   upgraded `Mossfield` world and the jar from `plugin-v2/build/libs/`. Set `stop-secret` in
   `plugins/mrrw/config.yml` to a long random string (the plugin refuses the finale while it's the
   dev default), restart, then run `/mrrw install` once. In `server.properties` set
   `online-mode=true` and `white-list=true`, and `/whitelist add` each player. The address is
   printed on the site, so without a whitelist strangers and scanners would join, become
   "residents" and feed Morrow's corpus.
2. **DNS:** the client area tells players the server is `mossfield.copperlinehosting.com`. Add an
   A record for `mossfield` pointing at the Minecraft host's IP. If the port isn't 25565, also add
   an SRV record `_minecraft._tcp.mossfield` pointing at it. Test by adding that address in the
   Minecraft server list.
3. **Vercel** (production project), environment variables:

| Variable | Value |
| --- | --- |
| `MORROW_ACT` | `1` |
| `MORROW_ACT_STARTED` | game night, ISO format **with your offset**, e.g. `2026-11-14T21:00:00-05:00` |
| `MORROW_SESSION_SECRET` | a long random string |
| `MORROW_STOP_SECRET` | the same string as `stop-secret` in the plugin config |
| `MORROW_MC_HOST` | the real `host:port` (not the SRV name; the site's ping doesn't follow SRV) |
| `MORROW_PLAYERS` | comma-separated Minecraft names |
| `MORROW_BRIDGE_URL` | `http://<minecraft host>:25588` (the plugin's bridge; see below) |

   **The bridge:** the plugin now runs the campaign. Acts advance by themselves from what players
   do and file, and the site follows the plugin's act through the bridge, so you don't change
   `MORROW_ACT` or redeploy per act (it stays as a floor; `/mrrw act N` still overrides). Open
   **TCP 25588** on the Minecraft host's firewall. Vercel has no fixed IPs, so the bridge is
   protected only by the shared stop secret and a 2-minute timestamp window: keep the host's clock
   synced. Check it any time with `python plugin-v2/tools/bridge_client.py state --config <path to plugins/mrrw/config.yml>`.

   **Redeploy after every env change** (Vercel only reads them at deploy). Never set `MORROW_DEV`
   or `MORROW_DESK_FAST` in production; the site now refuses to start with the default secrets.
   Before the finale, check `/mrrw status` shows the same secret fingerprint as the site's deploy log.
4. **Merge the branch** `morrow-v2/storyline` into `main` when you're happy. That's what deploys
   copperlinehosting.com. The current live site is still the old broken version.
5. **Marked server:** build the trailhead (`plugin-trailhead\gradlew.bat build`) and drop the jar
   from `plugin-trailhead/build/libs/` into its `plugins/` folder next to Marked, then restart. It
   stays dormant until you run `/trailhead start`. Players can see it with `/plugins`; that's
   intended (it's "mrrw-01", in-fiction). Log in as op so you don't get handed a key sector.

## 9. Playtest

Run Acts 1-3 with one friend before inviting the group. Watch quietly and note where they get
stuck. `morrow/v2/OPERATOR-RUNBOOK.md` covers running each act; `morrow/v2/SOLUTIONS.md` is the
answer key. Ideally pick someone who won't be at game night, because the playtest spoils Act 1.

## 10. Game night (the real start)

Everything above must be done first. Act 1 ends with the Mossfield server's address, so the
world, site and Discord all need to be live before anyone holds the key.

Play Marked normally. Then run the `/trailhead` beats from the runbook ("The trailhead"), whenever
you like. Hand out the key last, set `MORROW_ACT_STARTED` on Vercel to that night, and tell them
there's something in Marked you didn't put there.
