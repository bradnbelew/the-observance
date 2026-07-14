# The Observance - Live Launch Runbook

> **SUPERSEDED V4 ARCHIVE — DO NOT OPERATE FROM THIS FILE.** Use
> `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`.

This is the canonical production install order. It uses the same server for validation, but it does
not turn the live world into a rehearsal grid and does not advance the live story while testing.

## Production Artifacts

- Server: Paper 1.21.11 on Java 21
- Plugin: `plugin/build/libs/observance-0.3.29.jar`
- Plugin SHA1: `eda0e218d7a074a5c150b4781161e2da01c60ead`
- Datapack: `observance-datapack.zip`
- Datapack SHA1: `e056783e9829d25a6965c7cf04b16e58afcb1969`
- Resource pack: `observance-resourcepack.zip`
- Resource-pack SHA1: `fdc15d25e1cc5811269a3405091f7b5a143ac6db`
- Database bundle: `discord/supabase/apply-all.sql`
- Database bundle SHA1: `4e0e923fc10ae7049388baa173cd706568f191ca`

Re-read `observance-deploy-manifest.json` if any artifact is rebuilt. The manifest, not this copied
receipt, wins.

## 1. Stop And Install

Keep the server stopped and whitelisted while doing this.

Given a Paper server root called `<server>` and the `level-name` from `server.properties` called
`<world>`:

1. Back up `<server>/<world>`, `<server>/<world>_nether`, `<server>/<world>_the_end`, and
   `<server>/plugins/Observance` if they already exist.
2. Remove or archive older Observance jars so exactly one Observance jar will be in
   `<server>/plugins/`.
3. Put `observance-0.3.29.jar` at `<server>/plugins/observance-0.3.29.jar` and remove the superseded Observance jar.
4. Put `observance-datapack.zip` at `<server>/<world>/datapacks/observance-datapack.zip`. Leave it as
   a zip.
5. Set the Paper process environment variable `OBSERVANCE_SUPABASE_KEY` to the rotated live Supabase
   service-role key. Do not paste it into this repository.
6. Install Multiverse-Core in `<server>/plugins/` if the Unlit is launching. Observance does not create
   or load the `observance_unlit` world by itself.

For a fresh live server, let the plugin create `<server>/plugins/Observance/config.yml` and
`sites.yml` on first boot. For an updated server, preserve the existing folder but update the live
`config.yml`; uploading a jar does not overwrite an existing live config.

The live `plugins/Observance/config.yml` must contain:

```yaml
supabase:
  url: "https://fdnmhbpxnodrnbrzrlqq.supabase.co/rest/v1"
  service-key-env: "OBSERVANCE_SUPABASE_KEY"
  service-key: ""

resource-pack:
  url: "<DIRECT HTTPS ZIP URL>"
  sha1: "fdc15d25e1cc5811269a3405091f7b5a143ac6db"
```

Use the repository helper before uploading if the hosted URL has changed:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\set_resource_pack_config.ps1 -Url "<DIRECT HTTPS ZIP URL>"
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_hosted_resource_pack.ps1
```

## 2. First Controlled Boot

1. Start Paper with the whitelist on.
2. Confirm the console says Observance 0.3.29 enabled and shows no plugin, datapack, pack-format, or
   worldgen error.
3. Join as the operator and run:

```text
/datapack list enabled
/obs sleep on
/obs reload
/obs status
```

`/datapack list enabled` must include the Observance datapack. `/obs status` must report Supabase
configured, last DB call OK, queued writes 0, and the resource-pack URL/SHA configured. Rejoin after
the pack prompt and confirm the player's pack status is loaded.

Do not proceed if the database is false/offline, writes are queued, the pack fails, or the datapack
is absent.

## 3. Build The Production Deep Hold

Choose the permanent Surface Mouth in the main overworld. The build has a fixed world orientation:
the underground body extends toward positive Z, regardless of where the player looks. Reserve the
full local envelope `X -118..118`, `Y -104..+12`, and `Z -6..378` around that Mouth. The survey requires
at least 12 blocks of surface cover over every non-Mouth roof and a 12-block bottom-world buffer; an
unsafe location aborts before changing a block.

Stand on the exact center block of the intended surface mouth and run:

```text
/obs placehold build
/obs placehold audit
/obs placehold sync
/obs site launch
```

The audit must report 76/76 Hold sites, 8/8 gates, 8/8 records, protected Hold and entry regions,
virtual-open full traversal, and zero critical findings. All eight gates start sealed; the Mouth and
Grand Stair remain open. Do not run `/obs placehold open all` on the live story.

The Hold command owns its contained sites. Never hand-survey or `placeworld` those rooms.

## 4. Place The Non-Hold Launch Anchors

### Copperline relay / Discord handoff

Near the first live-server arrival, choose level surface ground with a clear south approach. Stand
where the approach should end; the structure will be placed directly north of you. Run:

```text
/obs placerelay
/obs audit
```

Walk the result in Adventure mode. All five lecterns must face the entrance and contain readable
books. Sorting the four copper jackets oldest-to-newest must yield `9137`; submitting that callback
at `https://copperlinehosting.com/support/ticket.php?id=1842` must reveal the current Discord room.
Do not place the relay inside the Hold or beside a later story gate.

### Opening report

At the real group base, with no players watching the placement area, run:

```text
/obs placeprologue
```

Accept the result only if it reports the intended base/location and both the report lectern and lit
marker exist. If it reports witnessed, occupied, or the wrong fallback location, correct that before
launch.

### Nether forge

Enter the real vanilla Nether, choose a short safe delve just beyond a reachable portal, stand where
the ruined forge should be centered, and run:

```text
/obs placeworld
```

The command uses the exact current Y coordinate in the Nether; it does not consult the roof heightmap.

Record the `nether_forge_placed` receipt.

### End shrine

Enter the real vanilla End, choose a reachable but isolated exile site, stand at the shrine center,
and run:

```text
/obs placeworld
```

Record the `end_seventh_shrine_placed` receipt.

Then run `/obs site launch` again. Do not use `placelab`, `prepworld`, `fullrun`, or `puzzlepass gates`
in the production world.

## 5. Create And Author The Unlit

The safest production Unlit is a literal copy of the finished main spawn village.

1. Stop the server.
2. Copy `<server>/<world>` to `<server>/observance_unlit`.
3. Delete only the copied `observance_unlit/uid.dat` identity file if present.
4. Start the server and run:

```text
/mv import observance_unlit NORMAL
/mvtp observance_unlit
```

Return to the main overworld and stand at the real village-well entry:

```text
/obs unlit buildmode on
/obs unlit site entry
```

In `observance_unlit`, place the spawn, extraction point, and eight evidence fixtures while facing the
direction from which each fixture should be read:

```text
/obs unlit site spawn
/obs unlit site exit
/obs unlit clue lamp
/obs unlit clue cairn
/obs unlit clue coop
/obs unlit clue well
/obs unlit clue watch
/obs unlit clue warm
/obs unlit clue threshold
/obs unlit clue base
/obs unlit border 138
/obs unlit darken all 138
/obs unlit buildmode off
/obs unlit audit
/obs unlit ready
```

`/obs unlit ready` must print `Gate: READY`.

## 6. Safe Production Validation

Run these while the Watcher is asleep and the server is still whitelisted:

```text
/obs status
/obs preflight
/obs visualaudit
/obs dialogueaudit
/obs placehold audit
/obs unlit audit
/obs unlit ready
/obs test sound <operator-name>
```

Join once as a genuine non-op and check walking, stairs, roofs, gates, protection, lecterns/books,
chests/barrels, NPC right-click dialogue, rune rendering, sounds, Unlit entry/extraction, and return
routes.

Do not submit a correct live puzzle answer, force story flags, open all Hold gates, or run a full-path
rehearsal against the production database. Those actions write canonical solve/global state. Use a
separate staging Supabase project for a full solution-path rehearsal. Reapplying `apply-all.sql` does
not promise to erase test players, solves, attempts, dossiers, or global flags.

## 7. Discord, Showrunner, And Website

Deploy the repository-root `render.yaml`. Enter the Discord and Supabase secrets in Render; do not
commit them. The Blueprint creates the persistent Discord worker and hourly showrunner cron. Run
`npm run register` once from `discord/` with the live Discord application/guild environment configured.

Set the Vercel production secret `DISCORD_INVITE_URL` to the current invitation. Rotate only that
value when an invite expires; never edit the callback code or rebuild Minecraft for invite rotation.
Confirm the deployed website root, `/record/the-record`, and `/status` load; ticket `1842` must reject
a wrong callback and reveal the current invite only for `9137`. Keep `/author` behind a private
deployment/access wall.

Start the persistent worker and verify its boot log includes `discord surface ready` for the intended
guild/channel. In Discord, `/link` must appear. Test with a non-story account only: an unknown name
must say to join the Minecraft world once, while an already-bound voice must not be transferable to a
different player. Do not type `kept` in the live `#the-record` until the real group begins.

The bot needs View Channel, Send Messages, Read Message History, and Attach Files in `#the-record`.
It does not need Administrator. If startup prints the Administrator warning, remove that permission
from its Discord role before launch; add Connect only if the optional voice-capture tier is enabled.

Keep optional voice capture, observer capture, recovered archive, and all `media_clip_*_ready` flags
off until their consent/media checklists are complete.

## 8. Arm The Live ARG

After the non-op pass and command receipts are recorded:

```text
/obs status
/obs preflight
/obs visualaudit
/obs dialogueaudit
/obs placehold sync
/obs unlit ready
/obs flag list
/obs sleep off
```

Also confirm the remote dashboard Watcher sleep switch is off. Add only the real player accounts to
the whitelist. Do not announce the first report separately; let the placed anomaly be found.

Finally run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_launch_manual_blockers.ps1 -Launch -CaptureCsv "D:\the-observance\build\launch-placement\2026-07-12\coords-capture.csv" -RehearsalPacket "D:\the-observance\rehearsals\2026-07-12"
```

Invite players only after that passes and `launch-attestations.md` ends with `decision: LAUNCH`.
