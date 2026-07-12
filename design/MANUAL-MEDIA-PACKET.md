# THE OBSERVANCE - MANUAL MEDIA PRODUCTION PACKET

This is the current actionable packet for Ethan-made media after the ARG remake pass. It supersedes older,
looser media-guide language when they disagree. The rule is simple: no in-world clue may point to a
missing file, and no external artifact may be the only path to a required conclusion. When an artifact is
real, wire it into the web; until then, keep the lure withheld.

The found footage should be recorded in Minecraft. It should feel like a recovered clip from a prior run,
an operator export, or a damaged player recording. Do not make it a real-life short film unless it is only
an incidental prop. The horror is that the Minecraft world has evidence in it.

## Handoff Format

Send every finished artifact with this block:

```text
Artifact name:
File or URL:
When players should see it:
What clue it contains:
Exact answer, if any:
Extraction method:
Should this be required, optional, or atmosphere only:
```

## A. Resource Pack Audio

Status: required for launch feel. The pack can technically degrade to silence, but launch should not.

Folder:

```text
resourcepack/assets/observance/sounds/
```

Required files:

```text
whisper.ogg
drone_low.ogg
stone_breath.ogg
cold_toll.ogg
keeper_voice.ogg
keeper_vaun.ogg
keeper_mara.ogg
keeper_sella.ogg
keeper_orin.ogg
keeper_brann.ogg
keeper_iss.ogg
```

Format:

- mono Vorbis `.ogg`
- 22050-48000 Hz
- 0.5-20 seconds preferred
- no music bed, no trailer hit, no obvious stock horror sting
- keep enough silence/noise around the sound that it feels spatial and physical

Creative direction:

- `whisper`: close, almost wordless, breath and cloth more than voice
- `drone_low`: room tone under pressure, not cinematic bass
- `stone_breath`: a cold exhale through masonry
- `cold_toll`: one toll or struck resonance, patient and distant
- `keeper_voice`: generic fallback, unintelligible human register
- `keeper_vaun`: dry, counted, hoarding breath
- `keeper_mara`: soft page/lectern texture, restrained
- `keeper_sella`: water-soft, childlike only if subtle
- `keeper_orin`: clipped, swallowed, almost refusing to speak
- `keeper_brann`: doubled or repeated, night-watch cadence
- `keeper_iss`: warm enough to trust, with a colder undertone

Integration:

- Run `tools/check_media_readiness.ps1` after replacing files.
- Build `observance-resourcepack.zip`.
- Host the exact zip at a direct HTTPS URL.
- Run `tools/set_resource_pack_config.ps1 -Url <hosted-https-zip-url>` so URL and SHA1 match.

Hosting:

- Good: GitHub Release asset, Cloudflare R2, S3, static CDN.
- Avoid: Google Drive for the resource pack. Minecraft needs a stable direct zip URL.

## B. the-hold.zip

Status: produced and present in the dashboard public folder. Recommended for launch if the opening should feel like an ARG before the server.

File name:

```text
the-hold.zip
```

Repo location when ready:

```text
dashboard/public/the-hold/the-hold.zip
```

Player-facing route:

```text
/
```

Current production state:

- source zip: rebuilt from the dashboard artifact by `tools\rebuild_hold_invitation.ps1`
- dashboard zip: `dashboard/public/the-hold/the-hold.zip`
- size: 17468 bytes (the datapack constructs the world on first load)
- SHA1: `69d227914501508c382952706c1f154e5e71152f`
- zip cleanliness check: 22 entries; no README/spoiler/backup entries; no pre-generated region files
- runtime check: all functions parse on Paper 1.21.11; first/final lecterns store structured written-book pages
- route check: six roofed rooms, five controlled gates, dry accessible walkways, one lever interaction
- final room handoff: copperline + hosting + common web + service 1842; no server port and no raw server endpoint

Format:

- vanilla Java 1.21.x single-player world zip
- no mods and no required resource pack
- adventure mode
- 10-15 minutes
- one route, no survival grind
- final room points to the abandoned public listing through a small reconstruction puzzle, not a raw address

Minimum build:

1. Archive Vestibule: copied records, six present hands, one missing line.
2. Domestic Hall: six accessible beds and settings, with the seventh place unissued.
3. Reed Cistern: six dry posts and the seventh answer below the water.
4. Lampworks: six tended lamps, one untended lamp, and the lever that changes the room.
5. Register Gallery: six complete depositions, one intentionally empty shelf, and `the record keeps`.
6. Dispatch Office: Copperline, Hosting, common web, service 1842, expired status, and owner mkept.

Payload:

- emotional payload: this place had ordinary people in it
- functional payload: the public listing handoff, then the live server address on the website
- recognition payload: the plain phrase `the record keeps`, not a required early cipher

Integration:

- The Record page withholds the link unless `dashboard/public/the-hold/the-hold.zip` exists.
- The community attachment and mkept static mirror both point at the same zip. Before planting the live lure, verify the deployed dashboard can serve `/`, `/the-hold/the-hold.zip`, and `/record/the-record-keeps`.
- Once present, include it in the rehearsal packet as a web-jump proof clip.

Hosting:

- Best: serve from the deployed dashboard as `/the-hold/the-hold.zip`.
- Good alternative: GitHub Release asset or static bucket.
- Avoid Discord attachments as the canonical host; links expire or become account-context dependent.

## C. Recovered Archive / Spectrogram Folder

Status: optional but high-value. It should deepen the Seventh thread, not replace it. The archive now has
a real puzzle answer: **`i was not kept`**.

Current production state:

- produced and local-staged on 2026-07-08
- staging receipt: `design/MANUAL-MEDIA-STAGING.md`
- local zip: `C:\Users\nanob\Documents\Codex\2026-07-07\hey\outputs\recovered-archive-packet.zip`
- hosted folder: `https://www.dropbox.com/scl/fo/72dz7n8lpa1gtiymtkyjl/AMbzcJsSm0x2_TkUq1Bzkv4?rlkey=tsom0g4z87qqxv7jo6cr989v5&st=014v4y3g&dl=0`
- hosting state: reachable, but not automatically launch-live
- operator verification: archive contents and spectrogram payload checked on 2026-07-08
- ready flag remains dormant until the operator intentionally flips it at the right story gate

Current gate:

```text
spine-recovered-archive requires seventh_suspected and recovered_archive_ready
```

That means the folder must not surface until:

- the group has live Sella/Seventh suspicion, and
- the operator has marked the real media ready.

Folder contents:

- `README.txt`
- `inventory_06.txt`
- `lamp_roll_scan.jpg`
- `intake_partial.png`
- `field_audio_03.ogg` or `.wav` containing the spectrogram text **I WAS NOT KEPT**
- one file that implies the Seventh was removed or rewritten

Production note:

- `spectrogram-key.txt` was intentionally omitted from the staged packet because it made the method too obvious.
- Do not treat the omission as a missing artifact unless a later playtest proves the spectrogram method is too opaque.

Required payload:

- hidden spectrogram phrase: **I WAS NOT KEPT**
- typed puzzle answer accepted by `spine-recovered-archive`: `i was not kept`
- one mundane corroboration file proving the kept were human
- one Iss-adjacent correction: he wrote mercy over refusal
- no `AVERYN` in this archive unless the group has already earned the name elsewhere

Extraction:

- Audacity spectrogram view is enough.
- Use Spectrogram view, window size 2048 or 4096, log frequency on, 0-8000 Hz visible.
- The phrase should occupy roughly 2-8 seconds of audio and be readable from a screenshot.
- The answer is self-confirming: `I WAS NOT KEPT` is a sentence the Record would hide, not random noise.
- Do not make the spectrogram the only proof of the Seventh. It is a corroborating artifact and Whisper-budget reward.

Integration:

- The exact hidden answer is already wired as `i was not kept`.
- Hosted folder is recorded in `design/MANUAL-MEDIA-STAGING.md`.
- The archive hint should point to the method without saying the answer.
- The spectrogram has been checked; keep `recovered_archive_ready` false until the story context is right.

Hosting:

- Good: Google Drive or Dropbox unlisted folder, because players open this manually.
- Better for permanence: dashboard static folder or Cloudflare R2/S3.
- If using Drive, set "anyone with the link can view" and test in an incognito window.

## D. Found Footage Recorded In Minecraft

Status: optional, but useful if it adds evidence rather than decoration.

Current production state:

- four clips produced and local-staged on 2026-07-08
- staging receipt: `design/MANUAL-MEDIA-STAGING.md`
- local folder: `C:\Users\nanob\Documents\Codex\2026-07-07\hey\outputs\manual-media`
- hosted on YouTube and HTTP-reachable on 2026-07-08:
  - `base_check_06.mp4`: `https://youtu.be/du-qp_clP7c`
  - `shore_copy_unlisted.mp4`: `https://youtu.be/iKqvPMHjR74`
  - `watch_floor_9_lit.mp4`: `https://youtu.be/pSPhBYMGIRc`
  - `room_below_noaudio.mp4`: `https://youtu.be/DtZizx5QIEs`
- hosting state: reachable, but not automatically launch-live
- operator verification: YouTube playback/videos checked on 2026-07-08
- `media_clip_01_ready`, `media_clip_02_ready`, `media_clip_03_ready`, and `media_clip_04_ready` remain false/dormant until the operator intentionally arms the matching clip at its story gate

Make 3 launch clips plus 1 late optional clip. These should be recorded in the actual server world once
the relevant structures exist, or in a private staging copy using the same structure palette.

Specs:

- 20-90 seconds each
- no intro card, no title text, no music
- practical darkness, phone/static camera, wrong timestamp if used
- one concrete clue per clip
- record at 1080p if possible, then degrade a copy; keep the clean original for debugging
- use Minecraft UI selectively: one clip may show F3 or hotbar, but most should feel like accidental play
- audio should be mostly in-game ambience/resource-pack audio, with faint Discord/mic room texture only if
  it feels natural

Video evidence grammar:

- Hotbar items are evidence. Every visible item should either be ordinary survival junk or a planted clue.
- Item names are evidence. Rename clue items in an anvil before recording if a frame-by-frame viewer should
  catch them.
- Durability/counts are evidence. Stack counts like `6`, `7`, `13`, `21`, or `64-1` can quietly carry meaning.
- Coordinates are evidence. Use F3 once or twice only; do not leave it open the whole video.
- Chat/subtitles are evidence. One system line, death message, or subtitle can do more than a sign.
- Video titles and descriptions are evidence. Keep them boring, archival, and slightly wrong.
- The description should never explain the puzzle. It can carry a timestamp, filename, or uploader note.
- The thumbnail should not spoil the clue. Use a bland still a player might ignore until later.

Launch clip list:

### D1 - `clip_01_prior_base.mp4`

When players see it:

- early, after the Record website is discovered or after the first base anomaly
- it should not reveal keeper answers

Record in Minecraft:

- first-person view inside an ordinary survival base
- a lectern or chest is opened; the player expects normal notes/items
- one page or item name changes between cuts
- the camera turns back and a lit marker is present where it was not before

Concrete clue:

- the solve token is **ASH-13**
- one brief frame shows `ASH` scratched/renamed on a map or sign
- the hotbar carries `torch x13`
- the description/timecode says the drift begins after `00:00:13`
- accepted typed answers for `media-prior-base`: `ash 13`, `ash thirteen`, `kept elsewhere ash 13`
- this is a Record-side payload, not a keeper solve and not a required main-spine gate

Hotbar:

1. `iron_pickaxe` or worn tool, normal
2. `bread`, count `6`
3. `torch`, count `13`
4. `book` renamed `first copy`
5. `map` renamed `kept elsewhere`
6. `lantern`, count `1`
7. empty
8. empty
9. `clock`, optional, if you want the time to feel wrong

On-screen evidence:

- a lectern page briefly reads `the record is kept in more than one place`
- the map name `kept elsewhere` appears in the hotbar for only a few seconds
- one damaged frame or cut frame shows only `ASH`
- a cut hides the exact moment the marker appears
- if F3 appears, crop/blur most of it but leave one real coordinate line partly readable

Upload metadata:

- title: `base_check_06.mp4`
- description: `recovered from the first copy. timecode drifts after 00:00:13.`
- filename if self-hosted: `base_check_06.mp4`

What players should get:

- the Record website is not marketing or a hint page; it is an in-world archive copy
- `the-record-keeps` is a route slug, not a coordinate
- `ASH-13` is an extractable off-world token that can be submitted when the media row is armed
- there were six kept records before the current group
- the world can change while no one is looking

Tone:

- mundane for 80 percent of the clip
- one impossible continuity break
- no chase, no monster reveal

### D2 - `clip_02_far_water_count.mp4`

When players see it:

- after Sella is suspected or when the group is circling the far-water thread
- it supports Sella, the seventh surplus, and the "people not monsters" layer

Record in Minecraft:

- shoreline/far-water area at dusk or rain
- camera looks at six visible markers
- reflected water or a map view shows a seventh mark offset from the rest
- the player pauses as if they noticed but does not explain it

Concrete clue:

- the solve phrase is **WHERE THE REEDS FOLD BACK**
- freeze-frame shows the phrase split across physical evidence:
  - `WHERE THE` on a half-visible map label or subtitle
  - `REEDS` formed by five item-frame letters/reeds at the shore
  - `FOLD BACK` in a reversed/reflected water sign
- optional audio spectrogram repeats `REEDS FOLD BACK`

Answer/payload:

- not the Seventh's name
- a place-bearing clue leading back to the far-water proof: `where the reeds fold back`
- accepted typed answers for `media-far-water`: `where the reeds fold back`, `the reeds fold back`, `reeds fold back`

Hotbar:

1. `filled_map` renamed `shore copy`
2. `compass`, normal
3. `kelp`, count `6`
4. `seagrass`, count `7`
5. `glass_bottle`, count `1`
6. `book` renamed `sella counted`
7. empty
8. empty
9. empty

On-screen evidence:

- the camera counts six visible markers slowly enough that players can count with it
- the water/reflection or map shows a seventh offset marker
- one item count contradicts another: `kelp x6`, `seagrass x7`
- the book/item name `sella counted` flashes only when selected

Upload metadata:

- title: `shore_copy_unlisted.mp4`
- description: `audio left as found. reflection not corrected.`
- filename if self-hosted: `shore_copy_unlisted.mp4`

What players should get:

- Sella's evidence is a count problem, not just a sad water scene
- the Seventh is distinct from Iss and from the six keeper evidence sites
- the far-water path deserves a return visit
- the phrase `where the reeds fold back` should point them to a physical place

### D3 - `clip_03_black_moon_toll.mp4`

When players see it:

- after Brann's watch-floor or dark-hours evidence starts surfacing
- it should make the black-moon rule feel real, not arbitrary

Record in Minecraft:

- Brann watch site or tower silhouette at night
- player stands still; bells/note blocks toll a rhythm
- the rhythm is audible enough to inspect, but not clean enough to feel like a tutorial
- if UI is visible, the time/moon state confirms this is a specific night

Concrete clue:

- the solve phrase is **STAY AWAKE**
- bell rhythm encodes `STAY AWAKE` as word lengths and toll groups:
  - `STAY` = 4 close tolls
  - pause
  - `AWAKE` = 5 close tolls
  - a late ninth toll sounds after the pause, matching the nine-light frame
- a single frame shows nine lights, one out, then the final late toll relights it
- accepted typed answers for `media-black-moon-toll`: `stay awake`, `awake`, `do not close your eyes`, `do not sleep on the black moon`

Answer/payload:

- Brann-side confirmation; does not unlock the finale by itself

Hotbar:

1. `spyglass`, normal
2. `lantern`, count `9`
3. `coal`, count `1`
4. `bell`, if available/creative staging only
5. `book` renamed `black moon`
6. empty
7. empty
8. empty
9. `clock`

On-screen evidence:

- subtitles briefly show bell or note block cues if Minecraft subtitles are enabled
- one frame shows nine lights; one is out, then later relit
- the player uses a spyglass toward the watch-floor/tower, making the site visually memorable
- optional F3/time proof appears for less than two seconds

Upload metadata:

- title: `watch_floor_9_lit.mp4`
- description: `do not normalize the audio. the ninth toll is late.`
- filename if self-hosted: `watch_floor_9_lit.mp4`

What players should get:

- Brann is about time, watch, repetition, and staying awake
- the black moon condition is fair because the clue exists outside the exact event
- `awake` / `do not close your eyes` is a confirmed Brann phrase
- nine lights matters

### D4 - `clip_04_release_room_late.mp4` (optional late clip)

When players see it:

- only after `seventh_named` or just before the final Accepting, depending on pacing

Record in Minecraft:

- static view down the Seventh chamber or release room
- no jump scare
- a distant shape is present, then absent, or one light finally goes still
- the clip ends before the name is spoken

Concrete clue:

- the late solve phrase/checksum is **SIX RETURN, ONE IS NOT KEPT**
- visible structure must show six return slots/candles and one absent/sealed seventh space
- `paper` renamed `not kept` appears only after the six slots are visible
- accepted typed answers for `media-release-room`: `six return one is not kept`, `six return and one is not kept`, `six return one not kept`

Answer/payload:

- no direct name reveal
- no `AVERYN` in the video unless the group has already earned the name
- this clip is gated behind `seventh_named`; it is a final-room approach checksum, not the way to learn the name

Hotbar:

1. `name_tag` renamed only if the group has already earned the name; otherwise leave it unnamed
2. `candle`, count `6`
3. `soul_lantern`, count `1`
4. `paper` renamed `not kept`
5. empty
6. empty
7. empty
8. empty
9. empty

On-screen evidence:

- six candles or slots are visible; the seventh space is absent, blank, or blocked
- the paper name `not kept` appears briefly
- the camera does not approach the figure; it refuses the cheap reveal
- after the group knows the name, a later version may show the name tag or renamed paper

Upload metadata:

- pre-name title: `room_below_noaudio.mp4`
- pre-name description: `last frame missing. no one says it.`
- post-name title if used: `room_below_after_name.mp4`
- post-name description if used: `the record did not keep this copy.`

What players should get:

- the finale is a place and an act, not a password prompt
- the missing/seventh space matters physically
- the release should be approached carefully and collectively
- before the name is earned, the clip withholds the name on purpose

Payload options:

- a coordinate visible for less than one second
- the same coordinate hidden in audio spectrogram
- a filename token
- EXIF timestamp or location
- a frame that shows a site players can identify later

Integration:

- Early clips should point to side-proof or keeper theory, not the finale.
- Late clips can support the Seventh, but should not reveal the name.
- If a clip becomes the cold open, the prologue sequence must be reauthored around it; do not shoot it as
  "the opening" unless we commit to that route.
- Treat each clip as evidence in the archive web. It should either open a place, corroborate an NPC claim,
  or strengthen a keeper theory. Pure atmosphere clips are cut unless they are extremely short.

Hosting:

- Good: unlisted YouTube for easy playback.
- Better ARG feel: hosted `.mp4` on dashboard/static bucket.
- Avoid explanatory video titles/descriptions.
- If using YouTube, use the exact titles/descriptions above or equally bland replacements; do not write
  ARG-style teasers in the public description.
- If self-hosting, keep the filenames exact until we wire them. The filenames themselves may be clues.

## E. Readiness Rules

- A clue to an external artifact is launch-ready only when an incognito browser can reach it.
- A download is launch-ready only when the exact filename, size, and URL are stable.
- A resource pack is launch-ready only when the server config SHA1 matches the hosted zip.
- A spectrogram clue is launch-ready only when a fresh solver can extract it with a named tool.
- A media artifact is required only if a live in-world route points at it. Otherwise it must remain withheld.
