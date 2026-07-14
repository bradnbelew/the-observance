# THE OBSERVANCE - MANUAL MEDIA STAGING RECEIPT

> **SUPERSEDED PRE-V5 ARCHIVE — NOT A CURRENT MEDIA RECEIPT.** Use `design/V5-EXTERNAL-MEDIA-RECEIPT.md` and the dated V5 rehearsal receipt.

Date recorded: 2026-07-08

This file records production, hosting, and gate state for the manual media artifacts. Hosted does not mean launch-live: each artifact still stays behind its matching readiness flag until the operator flips it intentionally at the right story gate.

Do not plant or expose player-facing clues to these artifacts just because this receipt exists. The existing flags remain the safety gate:

- `media_clip_01_ready`
- `media_clip_02_ready`
- `media_clip_03_ready`
- `media_clip_04_ready`
- `recovered_archive_ready`

## Local Staging Roots

- Found footage folder: `C:\Users\nanob\Documents\Codex\2026-07-07\hey\outputs\manual-media`
- Source Hold zip: rebuilt from the dashboard artifact by `tools\rebuild_hold_invitation.ps1`
- Dashboard Hold zip: `D:\the-observance\dashboard\public\the-hold\the-hold.zip`
- Recovered archive folder: `C:\Users\nanob\Documents\Codex\2026-07-07\hey\outputs\recovered-archive-packet`
- Recovered archive zip: `C:\Users\nanob\Documents\Codex\2026-07-07\hey\outputs\recovered-archive-packet.zip`
- Hold zip cleanliness check: 534 zip entries; no README/manifest/spoiler-style entries found on 2026-07-08
- Hosted found footage folder/set: unlisted YouTube links below
- Hosted found footage reachability check: HTTP 200 on 2026-07-08 for all four supplied URLs
- Hosted recovered archive folder: `https://www.dropbox.com/scl/fo/72dz7n8lpa1gtiymtkyjl/AMbzcJsSm0x2_TkUq1Bzkv4?rlkey=tsom0g4z87qqxv7jo6cr989v5&st=014v4y3g&dl=0`
- Hosted archive reachability check: HTTP 200 on 2026-07-08
- Operator media verification: Ethan checked the YouTube videos, recovered archive contents, and spectrogram payload on 2026-07-08. The archive folder has the correct files and `field_audio_03.wav` resolves to `I WAS NOT KEPT`.

## Produced Found-Footage Files

| Artifact | Player-facing filename | Payload | Size bytes | SHA1 | Hosted URL | Hosting state |
| --- | --- | --- | ---: | --- | --- | --- |
| `clip_01_prior_base.mp4` | `base_check_06.mp4` | `ASH-13` | 421443427 | `844c2aaf8fb51836add4b59e81abe4131c8d6d0a` | `https://youtu.be/du-qp_clP7c` | hosted on YouTube; HTTP 200; operator-checked; flag still dormant |
| `clip_02_far_water_count.mp4` | `shore_copy_unlisted.mp4` | `WHERE THE REEDS FOLD BACK` | 132001373 | `9b979e349c7a0d7497fd0fe76d0450e744dc39d0` | `https://youtu.be/iKqvPMHjR74` | hosted on YouTube; HTTP 200; operator-checked; flag still dormant |
| `clip_03_black_moon_toll.mp4` | `watch_floor_9_lit.mp4` | `STAY AWAKE` | 108195021 | `9b6552e21ec01e6f046027247a689c8dd78b8ce1` | `https://youtu.be/pSPhBYMGIRc` | hosted on YouTube; HTTP 200; operator-checked; flag still dormant |
| `clip_04_release_room_late.mp4` | `room_below_noaudio.mp4` | `SIX RETURN, ONE IS NOT KEPT` | 15065612 | `1cb3e600d3e16e9bb1434fa65ddbdff04f512fbd` | `https://youtu.be/DtZizx5QIEs` | hosted on YouTube; HTTP 200; operator-checked; flag still dormant |

## Produced Hold Download

| Artifact | Dashboard path | Payload | Size bytes | SHA1 | Hosting state |
| --- | --- | --- | ---: | --- | --- |
| `the-hold.zip` | `dashboard/public/the-hold/the-hold.zip` | six-room contained prologue; structured books; single-layer gates; cistern recovery ladders; host fragments I-IV + common-web ending + service digits 25569; no assembled raw server endpoint; kept count `6`; missing seventh; live arrival points to retired Copperline relay | 17812 | `8a4986422a4af6c65b47f76c61a1e75421b568d4` | present in dashboard public; deterministic command-built payload; Paper/Java 1.21.11; deploy route must be checked before planting live lure |

## Produced Recovered Archive Packet

Expected local contents:

- `README.txt`
- `inventory_06.txt`
- `lamp_roll_scan.jpg`
- `intake_partial.png`
- `field_audio_03.wav`

Archive zip:

| Artifact | Size bytes | SHA1 | Hosting state |
| --- | ---: | --- | --- |
| `recovered-archive-packet.zip` | 62009 | `783ecde5685abdb601e4a659fc947c32964f70b3` | local backup zip; Dropbox folder hosted |

Spectrogram audio:

| Artifact | Payload | Size bytes | SHA1 |
| --- | --- | ---: | --- |
| `field_audio_03.wav` | `I WAS NOT KEPT` | 58256 | `2003f0151c1ba643c649b5ed0e19d1b31bb68319` |

`spectrogram-key.txt` is intentionally omitted from this staged packet. The method clue was too obvious; the archive should rely on the context, filename, and solver inspection rather than a direct method file.

## Safe Wiring State

- Found-footage puzzle rows and accepted answers are already seeded.
- The Hold download is present at the dashboard path used by `/` and `/record/the-record-keeps`.
- The recovered archive answer is already seeded as `i was not kept`.
- The archive remains gated by `seventh_suspected` plus `recovered_archive_ready`.
- Video rows remain gated by their `media_clip_0N_ready` flags and context flags even though the YouTube URLs are reachable.
- No player-facing trail should point to the local filesystem paths.
- Do not flip any media-ready flag automatically. The artifacts are hosted and operator-checked, but each ready flag should be set only at its intended story gate.
- If self-hosting on the dashboard, add only stable public files under `dashboard/public/...` and update this receipt with the final URL and SHA1.

## Remaining Hosting Choices

- Found footage: YouTube URLs are supplied, HTTP-reachable, and operator-checked; set `media_clip_0N_ready` only when the matching clue should enter play.
- the-hold.zip: present in `dashboard/public/the-hold/`; verify deployed `/`, `/the-hold/the-hold.zip`, and `/record/the-record-keeps` before sending players to the web trail.
- Recovered archive: Dropbox folder URL supplied, reachable, correctly populated, and spectrogram-checked; keep this URL stable or update the receipt before launch.
- If hosted outside the dashboard, record the exact URL, final filename, size, SHA1, and incognito test result before setting any ready flag.
