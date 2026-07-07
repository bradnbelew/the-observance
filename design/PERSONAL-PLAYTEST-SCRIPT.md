# THE OBSERVANCE - PERSONAL PLAYTEST SCRIPT

> Use this when Ethan does the solo/personal pre-launch playtest. The goal is not to prove every puzzle
> by brute force. The goal is to verify that the first hour feels haunted, the clue chain is retrace-fair,
> side stories are findable, and manual media slots are ready to receive real files.
>
> Evidence format lives in `design/LIVE-REHEARSAL-EVIDENCE.md`. Use this script for the route; use the
> evidence packet for screenshots, clips, blockers, and KEEP/RESHAPE/REPLACE/CUT calls.

## Before You Start

Run the checks:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\audit_all.ps1
```

From `plugin/`:

```powershell
.\gradlew.bat test build
```

Manual setup required for this playtest:

- resource pack URL/SHA1 set or intentionally skipped if testing without custom runes
- launch blocker gate reviewed with `tools\check_launch_manual_blockers.ps1`
- townsfolk spawned
- cold open staged
- keeper/deep sites placed enough for the route you are testing
- `/observance reading` run if testing final-name carvings
- `/observance finale` run if testing release markers

## What To Watch For

Keep notes in four columns:

- **felt:** what you felt as a player
- **evidence:** what clue made you think the next step was fair
- **friction:** what felt like interface/code confusion rather than mystery
- **fix:** prose, placement, pacing, or code change

If something is merely hard, keep it. If something is unclear after retracing, fix it.

Also capture the required evidence packet: approach/focal/answer/exit screenshots for major sites,
NPC line -> physical proof shots, at least six side-path proofs, one clip per scare family, and the
first-hour uncut clip. Fill the `Director Cut Scorecard` last, after watching the evidence back cold.
Any score below 4 becomes a fix, not a note for later.

## Pass 1 - The First Hour

Start as a real player. Do not rush to the stones.

1. Notice the cold-open anomaly.
2. Read the first report.
3. Talk to Aro, Wenna, Coll, Dob, and Old Pell.
4. Let at least one small haunting happen.
5. Walk toward the Lamp-works / line area.
6. Return to at least one NPC after seeing something below.

Pass criteria:

- the world feels wrong before it feels like a puzzle room
- Aro, Wenna, Coll, Dob, and Pell feel like different people
- at least one NPC line changes how you interpret a place
- no line sounds like a tutorial tooltip

## Pass 2 - Literacy

1. Find the Rosetta/rune ring.
2. Use in-world cribs to build the reading.
3. Solve:

> bow offering kept light deep line unspoken sacred beast

4. Confirm the reward/flag/record response lands.

Pass criteria:

- the ring teaches enough to be fair
- the resource pack renders correctly
- the solve feels earned, not guessed

## Pass 3 - The Record Elsewhere

1. Find the founder note about the record being kept in more than one place.
2. Read the warning that the elsewhere is not a shrine, bearing, or coordinate.
3. Decode the old-script line:

> the-record-keeps

4. Open the record route:

> /record/the-record-keeps

Pass criteria:

- the web jump is logically forced by the text
- the page feels like a place in the ARG, not a website menu
- no live lure points to missing media unless intentionally staged

## Pass 4 - Side Story Sampling

Do not test all side destinations in one sprint. Pick at least six:

- warm stair
- empty cairn
- warm town
- school stand
- bird coops
- far water
- markers row
- Cistern 7
- Deep Market
- ration table
- dead shrine
- gutter lamps

Pass criteria:

- each destination changes what you believe
- dead leads feel authored, not broken
- side content does not gate progress but still feels worth finding

## Pass 5 - Wren

1. Spawn/meet Wren.
2. Interact several times.
3. Confirm he speaks one restrained line per interaction, not a dumped packet.
4. Advance enough trust to see the companion pattern.
5. Trigger or simulate the reveal.
6. Test one reckoning branch.

Pass criteria:

- Wren feels useful before suspicious
- his fallback speech does not feel temporary
- the reveal feels like betrayal, not exposition

## Pass 6 - Iss and the Lie

1. Follow Iss's warm reading first.
2. Reach the dead shrine / cold hearth.
3. Return and find the cold reading.
4. Read the acrostic:

> no wall

5. Solve the catch:

> no wall was ever built here

Pass criteria:

- the false lead costs time but teaches truth
- the catch is retraceable from the warm lines
- Iss's warmth becomes suspicious only after evidence earns it

## Pass 7 - Final Name and Release

1. Collect or fast-forward each keeper fragment.
2. Confirm the six letters:

> A V E R Y N

3. Confirm Iss corrects M to N.
4. Perform the Accepting.
5. Speak/restore:

> AVERYN

6. Trigger the Release.

Pass criteria:

- the name feels restored, not guessed
- the group act matters
- the kick/farewell feels theatrical and final

## Media Slot Check

Before making final media, confirm each planned artifact has a payload slot:

- found footage: one frame/audio/metadata payload and one emotional payload
- Drive folder: filename/order/metadata clue plus recovered-world evidence
- spectrogram: hidden text or glyphs that confirms an existing lane
- `the-hold.zip`: a misleading readme, a count mismatch, and a return path

Do not add a media clue that introduces a new canon branch. Media should confirm or redirect existing
truths: record, kept, wall, seventh, count, light, deep, name.

## Stop Conditions

Stop and fix before inviting others if:

- the first hour feels like a puzzle course
- any NPC feels generic or interchangeable
- the record website feels like a wiki
- a clue points to missing media
- a side destination feels empty after the walk
- Wren sounds temporary
- the final name feels like password entry
- any Director Cut Scorecard axis is below 4

