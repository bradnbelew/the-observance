# Phase 1 — Brad Footage and Media Inventory

Status: **PHASE 1 INVENTORY COMPLETE — SOURCE-CUSTODY AND AV REVIEW GATE OPEN**

Purpose: preserve Brad-made work before any replacement decision. This inventory is read-only and does
not change hosting, URLs, manifests, hashes, edits, or live prerequisite gates.

## Discovery evidence

- Repository scan found no checked-in `.mp4`, `.mov`, `.mkv`, `.avi`, `.webm`, `.wav`, `.mp3`, `.flac`,
  `.ogg`, or `.m4a` source asset.
- `arc/v5/media-manifest.json` records four video sources and one WAV with canonical names, byte counts,
  SHA-1 values, current hosted identities, payloads, and narrative uses.
- `design/V5-EXTERNAL-MEDIA-RECEIPT.md` records successful public reachability on 2026-07-13.
- The connected Google Drive profile is Brad's account (`bradnbelew@gmail.com`), but read-only searches
  for all video MIME types, all audio MIME types, “Observance,” and `base_check_06` returned no files.
- The Dropbox archive receipt lists the WAV plus four supporting files; those bytes were not downloaded
  or modified in Phase 1.

## Known asset register

| Asset | Current source identity | Preserved payload / story job | Byte/hash receipt | Phase 1 disposition |
| --- | --- | --- | --- | --- |
| `clip_01_ash_locker` | Canonical `clip_01_prior_base.mp4`; hosted as `base_check_06.mp4`; YouTube ID `du-qp_clP7c`. | `ASH-13`; confirms Ash's Locker 13/archive key after the real camp investigation. | 421,443,427 bytes; SHA-1 `844c2aaf8fb51836add4b59e81abe4131c8d6d0a`. | **Preserve; reuse/re-edit candidate.** Do not replace before source custody and frame/audio review. |
| `clip_02_reeds_cache` | Canonical `clip_02_far_water_count.mp4`; hosted as `shore_copy_unlisted.mp4`; YouTube ID `iKqvPMHjR74`. | `WHERE THE REEDS FOLD BACK`; locates the original-filter cache used to clear Nessa. | 132,001,373 bytes; SHA-1 `9b979e349c7a0d7497fd0fe76d0450e744dc39d0`. | **Preserve; reuse/re-edit candidate.** Its Nessa function is load-bearing even if delivery/edit changes. |
| `clip_03_watch_correction` | Canonical `clip_03_black_moon_toll.mp4`; hosted as `watch_floor_9_lit.mp4`; YouTube ID `pSPhBYMGIRc`. | `STAY AWAKE`; supports Brann's paired-watch correction and altered timestamp. | 108,195,021 bytes; SHA-1 `9b6552e21ec01e6f046027247a689c8dd78b8ce1`. | **Preserve; reuse/re-edit candidate.** Rebuild context must keep the practical watch meaning, not supernatural sleeplessness. |
| `clip_04_release_instruction` | Canonical `clip_04_release_room_late.mp4`; hosted as `room_below_noaudio.mp4`; YouTube ID `DtZizx5QIEs`. | `SIX RETURN, ONE IS NOT KEPT`; return six affidavits without binding Averyn into a seventh slot. | 15,065,612 bytes; SHA-1 `1cb3e600d3e16e9bb1434fa65ddbdff04f512fbd`. | **Preserve; reuse/re-edit candidate.** Final delivery may change, but its release instruction must remain exact in meaning. |
| `spectrogram_averyn_voice` | Canonical `field_audio_03.wav`; currently inside the Dropbox recovered-archive packet. | `I WAS NOT KEPT`; first unambiguous evidence of a constrained human voice inside the Record. | 58,256 bytes; SHA-1 `2003f0151c1ba643c649b5ed0e19d1b31bb68319`. | **Preserve current bytes as reference.** Reuse/re-edit/rebuild delivery is open after source and accessibility review. |

## Supporting recovered-archive register

The current Dropbox receipt names `README.txt`, `inventory_06.txt`, `field_audio_03.wav`,
`intake_partial.png`, and `lamp_roll_scan.jpg`. All five are treated as a single provenance packet until
their contents and relationships are reviewed; none may be discarded merely because only the WAV is a
current required-media row.

## Preservation rules

1. Preserve the current manifest, public receipt, canonical filename, hosted identity, byte count, SHA-1,
   prerequisite, payload, and narrative job until a replacement release is fully approved.
2. Never overwrite an original. A re-edit receives a new derivative filename, cryptographic hash,
   edit-decision record, and provenance link to the source receipt.
3. No replacement may be commissioned or generated until Brad confirms source custody and reviews a
   written keep/re-edit/replace recommendation for that asset in isolation.
4. Rehosting is not replacement. A diegetic first-party delivery page may wrap or mirror preserved bytes
   while retaining the original receipt and a rollback route.
5. Every required audiovisual fact needs an accessible redundant representation that preserves
   investigation rather than simply printing the solution; sound-only information cannot gate progress.
6. Media remains prerequisite-gated, replayable after reveal, non-missable, and mirrored into the durable
   catch-up surfaces.

## Required AV/source review fields

Before Phase 2 assigns media to evidence chains, each asset receives a review row covering:

- confirmed owner/source location and best available master bytes;
- duration, resolution, frame rate, codecs, audio channels, and visible compression damage;
- exact frame/timecode carrying the payload and whether it survives ordinary playback;
- diegetic strengths worth preserving: location, prop continuity, performance, camera voice, accidents,
  texture, and authored imperfections;
- contamination to remove or mask: platform branding, stale geometry, obsolete canon, UI, usernames,
  coordinates, metadata, or accidental solution leakage;
- re-edit handles: safe trims, crops, overlays, grading, audio cleanup, subtitles/transcript, and whether
  those edits preserve found-footage credibility;
- delivery plan, fallback/mirror, accessibility path, cache policy, and rollback source;
- Brad's explicit decision: **keep as-is**, **re-edit**, or **replace**, with rationale.

## Open custody items

- Locate the four video master files or confirm the manifest byte/hash receipts are the best surviving
  sources.
- Locate the Dropbox packet's best original copy and inspect all five entries together.
- Resolve why Brad's connected Drive currently exposes no searchable video/audio files—missing upload,
  different storage location, or Drive visibility—without moving or sharing files automatically.
- Perform human audiovisual review before Phase 2 uses timing, scenery, or editability assumptions.

These custody items block media editing or replacement, but they do not block approval of the Phase 1
experience architecture because the current bytes, meanings, and rollback identities are preserved.
