# Morrow Handmade Media Production Packet

Status: required for full release; not required for the current local G01-G03 route slice.
Production boundary: make these as first-party files, store them locally, then hash the final encoded outputs before they enter any delivery catalog.

Media intake is now tracked by `morrow/media/media-manifest.template.json`. That manifest contains all
twelve required assets, their work folders, acceptance checks, safety-review fields, and blank hash
slots. Copy it for a real release intake or update it in place only when actual source/final files
exist.

## Global rules

- Every asset needs an original file, a final delivery file, SHA-256 for both, custody notes, creation date, author identity, and a plain-text accessibility equivalent.
- Texture may age the artifact, but required evidence must remain readable in the accessible equivalent.
- Do not use real people, real companies, real phone numbers, private data, or real-world surveillance implications.
- Filenames should look like retained Copperline/Mossfield material, but must not contain secrets.
- Keep originals in a working folder and only catalog final exports after review.

## Required assets

1. `morrow.g01.retired_hosting_brochure`
   - Make a two-page 2011 Copperline sales PDF for managed Minecraft worlds.
   - Include obsolete plan code `cl-2011-6118` and product slug `retired-managed-worlds` in the footer.
   - Add OCR/plain text preserving footer order.

2. `morrow.g02.forum_avatar_contact_sheet`
   - Make a small contact sheet of fictional 2011 forum avatars.
   - Include `i_bell` and `iona_bell` continuity through one damaged/repeated frame or cache key.
   - Provide a named grid text equivalent.

3. `morrow.g03.theo_account_voicemail`
   - Record or synthesize a phone-bandwidth support voicemail from Theo.
   - Required observation: he says Mossfield should stay billable until "someone comes home."
   - Provide a timecoded transcript with room tone and clipping notes.

4. `morrow.g06.rookery_notebook`
   - Create scanned handwritten maintenance pages with arrows, deletions, pressure marks, and one torn/missing leaf.
   - Required observation: the missing leaf continues the route into the buried cache.
   - Provide a layout-preserving transcript.

5. `morrow.g07.june_lighthouse_photo`
   - Capture/stage a Minecraft horizon image plus old locator map.
   - Required observation: the current recovered tower is absent from the dated photograph and map.
   - Provide coordinate-grid alt text naming visible landmarks and the empty location.

6. `morrow.g08.frame_thirty_seven`
   - Make a short Minecraft video where frame 37 has one background lever change while foreground action loops.
   - Provide a frame-indexed transcript listing foreground, background, and redstone changes.

7. `morrow.g09.current_session_contamination`
   - Make a recovered resident path clip whose label claims an older date but includes a current-session movement detail.
   - Provide metadata table and ordered movement description.

8. `morrow.g09.patchcord_cassette_dub`
   - Make a cassette-style audio dub with voice-chat scraps, tape hiss, dropouts, and repeated Copperline support cadence.
   - Provide speaker-labelled transcript and noise-event notes.

9. `morrow.g11.incident_raw_export`
   - Make CSV/log export for incident 6118.
   - Required observation: uptime `04:17:22` survives while public fields change.
   - Provide accessible table with identical field order.

10. `morrow.g12.iona_reversed_memo`
    - Record Iona maintenance memo and export a reversed damaged attachment.
    - Provide independently earned reverse-order transcript and normal-order transcript after unlock.

11. `morrow.g12.spectrogram_command`
    - Make an audio carrier with restrained spectrogram text for a short command word.
    - Provide a frequency-bin table encoding the same word without audio software.

12. `morrow.g15.shutdown_record`
    - Make a sparse terminal/printable receipt with hashes, stopped process state, inert world state, and no Morrow message.
    - Required observation: silence is evidence; do not add a stinger.

## Delivery checklist per asset

- Final file opens locally.
- SHA-256 recorded after final export.
- Accessibility equivalent independently carries the required observation.
- Custody note explains who made/kept the artifact inside fiction.
- The asset does not become the sole carrier for sound-only or color-only information.
- The catalog status changes only after the exact final hash is known.

Before any asset is marked release-ready, run:

```powershell
python tools\check_morrow_media_intake.py
python tools\check_morrow_production_readiness.py
```
