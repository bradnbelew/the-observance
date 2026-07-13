# THE UNLIT - PRE-ARG STARTUP GUIDE

This is the operator path for turning the Unlit from a coded feature into a playable ARG pillar.
Use it after uploading the fresh plugin jar and before friends enter the fiction.

## What This Pillar Must Do

- It is a dark duplicate of the spawn village, preferably an exact copy of the real village world at 0,0.
- Players enter from the real village well at `unlit_entry`.
- Every entrant receives 3 borrowed lanterns and one return token.
- Doors work. Breaking, building, carried lights, buckets, boats, pearls, beds, and route cheese do not.
- The figure stalks, vanishes, rushes exposed lanterns, breaks them, and deals small pressure damage.
- The ending cannot reach `rite-tokens` until the group has recovered the whole Unlit evidence set:
  `unlit_seen_lamp`, `unlit_seen_cairn`, `unlit_seen_coop`, `unlit_seen_well`,
  `unlit_seen_watch`, `unlit_seen_warm`, `unlit_seen_threshold`, and `unlit_seen_base`.

All eight houses are required ending evidence. The route is still non-linear, but no house is optional
scenery; each one corroborates a different part of the surface/deep/keeper web.

## One-Time Update Steps

1. Upload the latest jar:
   `plugin/build/libs/observance-0.3.29.jar`
2. Apply the latest database bundle:
   from `discord/`, run `npm run db:seed`, then paste and run `discord/supabase/apply-all.sql` in Supabase.
3. Restart the Minecraft server.
4. In game, run `/observance status`.
   You want Supabase configured, the last DB call OK, and queued writes at 0.

The resource pack does not need a new build for the current Unlit figure. The figure is display-entity
based and the resource pack SHA is unchanged unless you edit assets.

## Create The Duplicate World

Use Multiverse on the server. It belongs in the server `plugins` folder, not in your client mods folder.

Safest exact-copy path:

1. Stop the server.
2. Copy the main world folder.
   Example: copy `world` to `observance_unlit`.
3. In the copied `observance_unlit` folder, remove the copied world identity file if present:
   `uid.dat`.
4. Start the server.
5. Import the copy:
   `/mv import observance_unlit NORMAL`
6. Teleport there once to confirm it loaded:
   `/mvtp observance_unlit`

This gives the Unlit the same village layout as the real overworld because it is literally a duplicate,
not a regenerated approximation.

## Place The Unlit Anchors

Turn on build mode while authoring:

```text
/obs unlit buildmode on
```

Recommended placement order:

1. In the real overworld village well, stand at the exact entry spot and run:
   `/obs unlit site entry`
2. In `observance_unlit`, stand at the intended arrival point, a little off the real spawn for effect, and run:
   `/obs unlit site spawn`
3. In `observance_unlit`, stand at the retreat/extraction spot and run:
   `/obs unlit site exit`
4. Visit each authored house. Stand where the fixture should face/read well, face the clue direction, and run:
   `/obs unlit clue lamp`
   `/obs unlit clue cairn`
   `/obs unlit clue coop`
   `/obs unlit clue well`
   `/obs unlit clue watch`
   `/obs unlit clue warm`
   `/obs unlit clue threshold`
   `/obs unlit clue base`

The required ending evidence houses are all eight: lamp, cairn, coop, well, watch, warm, threshold,
and base. If time is short, do not launch the Unlit; use `/obs unlit pass house` to rehearse fixtures
until every house reads cleanly.

## Darken And Bound The World

Your raw Minecraft command `/worldborder set 275` means 275 blocks wide, not radius. The Observance command
uses radius, so use 138 to match that scale:

```text
/obs unlit border 138
/obs unlit darken all 138
```

Then run:

```text
/obs unlit audit
/obs unlit ready
```

You want:

- world loaded
- border OK
- stray light OK
- every required site placed
- fixture proof OK or only intentional warnings you are about to fix

Before any player-facing test:

```text
/obs unlit buildmode off
```

## Fast Playtest Checklist

Use a non-op account if possible, because op build mode can hide real restrictions.

1. Stand at the overworld well entry and right-click to enter.
2. Confirm inventory swaps to 3 borrowed lanterns plus one return token.
3. Confirm doors open.
4. Confirm normal blocks cannot be broken or placed.
5. Confirm arbitrary lights cannot be brought in or used.
6. Place a borrowed lantern and confirm it helps but does not make the whole area fully visible.
7. Stand outside safety long enough to feel darkness pressure.
8. Watch the figure stalk, vanish, move quickly, make sound, and break exposed lanterns.
9. Reach lamp, cairn, coop, well, watch, warm, threshold, and base across one or more expeditions.
10. Confirm each discovery writes an actionbar message and later appears in the Record/dashboard/archive.
11. Exit by walking to `unlit_exit` and by using the return token; inventory should restore both ways.

Useful focused passes:

```text
/obs unlit pass light
/obs unlit pass stalker
/obs unlit pass extinguish
/obs unlit pass house
/obs unlit pass extract
```

## Pre-ARG Final Gate

After the real placement pass, generate the rehearsal packet:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\new_rehearsal_packet.ps1
```

Fill the Unlit section with clips/screenshots for entry, spawn, exit, lamp use, darkness pressure,
figure behavior, clue reading, failed cheese, and extraction. Then run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_unlit_playtest_ready.ps1 -PacketDir rehearsals\<date>
```

When that prints `unlit playtest readiness: OK`, stop building and let Nano playtest.
