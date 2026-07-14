# V5 external media receipt — 2026-07-13

Status: current verification receipt

Scope: signed-out public reachability and current host metadata. This receipt does not replace the
required real-player prerequisite-gating/playback rehearsal in `V5-LIVE-TEST-MATRIX.csv`.

| Asset | Result | Current resolved identity |
| --- | --- | --- |
| `clip_01_ash_locker` | HTTP 200; YouTube `playabilityStatus=OK` | `du-qp_clP7c` / `base_check_06` |
| `clip_02_reeds_cache` | HTTP 200; YouTube `playabilityStatus=OK` | `iKqvPMHjR74` / `shore_copy_` |
| `clip_03_watch_correction` | HTTP 200; YouTube `playabilityStatus=OK` | `pSPhBYMGIRc` / `watch_floor_9_lit` |
| `clip_04_release_instruction` | HTTP 200; YouTube `playabilityStatus=OK` | `DtZizx5QIEs` / `room_below` |
| `spectrogram_averyn_voice` | HTTP 200; Dropbox archive downloadable | `recovered-archive-packet.zip` |

Dropbox archive entries observed:

```text
README.txt
inventory_06.txt
field_audio_03.wav
intake_partial.png
lamp_roll_scan.jpg
```

`field_audio_03.wav` live archive receipt:

```text
bytes: 58256
sha1: 2003f0151c1ba643c649b5ed0e19d1b31bb68319
```

The byte count and SHA-1 exactly match `../arc/v5/media-manifest.json`. The four YouTube sources were
verified by public landing/playability metadata; their checked-in source receipts remain immutable.
Final acceptance still requires audiovisual review through a normal signed-out player browser.
