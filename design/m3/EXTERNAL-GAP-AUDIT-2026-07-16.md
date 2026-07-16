# M3 external-gap audit — 2026-07-16

Status: **READ-ONLY DISCOVERY COMPLETE; M4 REMAINS CLOSED**

Checkpoint audited: `d63c48cca3b2964aea1513f704574855b4bf8a72` on
`codex/m3-private-vertical-slice`. Authority order was read through the locked spine, approved
conformance, Phase 0/1/2 authorities, M2 contracts, M3 slice authority, review package, autonomous
continuation authority, and phase ledger before discovery began.

This receipt records only state actually observed on 2026-07-16 (America/Chicago). No secret value was
read or printed. No service, deployment, file, media object, sharing rule, server, database, or runtime
was changed. Public availability probes did not join Minecraft or authenticate to Crafty.

## Repository declarations (not live platform evidence)

| Family | Repository declaration |
| --- | --- |
| Railway worker | Root `/discord`; `discord/railway.worker.json`; `npm start`; RAILPACK; restart `ON_FAILURE`, maximum 10 retries. |
| Railway recovery | Root `/discord`; `discord/railway.cron.json`; `npm run showrunner`; RAILPACK; schedule `*/10 * * * *`; restart `NEVER`. |
| Vercel | Team/project IDs recorded as `team_2HAUKLhWF4QVYDHEt5FbNeHu` / `prj_UygHA98HGW4IBVMk6AKzXVEG6ZSQ`; M2 requires an exact-release preview receipt. |
| Crafty/Paper | One brother-hosted Crafty-managed Paper `1.21.11` runtime; player endpoint reconstructed as `snoikerz.com:25569`. No Crafty panel URL or server ID is committed. |
| Media | `arc/v5/media-manifest.json`, `PHASE-1-MEDIA-INVENTORY.md`, and the dated media receipts declare four video byte/hash identities plus one five-file recovered packet. |

These declarations describe intended configuration or prior receipts. They do not establish current
Railway configuration parity, an exact-checkpoint Vercel preview, Crafty identity, Paper version, or
media-master ownership by themselves.

## Live/read-only observations

### Railway worker and recovery families

- The Railway dashboard resolved to `https://railway.com/dashboard` and displayed a login dialog.
- No existing signed-in browser tab/session was available to claim.
- Railway CLI was absent. `RAILWAY_TOKEN`, `RAILWAY_API_TOKEN`, `RAILWAY_PROJECT_ID`, and
  `RAILWAY_SERVICE_ID` were absent from the process environment; only presence was checked.
- Therefore no authenticated project, environment, service, deployment, source commit, root directory,
  start command, restart policy, cron schedule, variable-name parity, lease evidence, or worker/recovery
  family assignment was observed.

Result: **exact Railway identities and live configuration parity remain unresolved.** Repository JSON
must not be represented as a live Railway receipt.

### Vercel preview readiness

The connected Vercel account returned:

- team `team_2HAUKLhWF4QVYDHEt5FbNeHu` (`bradens-projects-c5e41066`);
- project `prj_UygHA98HGW4IBVMk6AKzXVEG6ZSQ` (`the-observance-kjxn`), Next.js, Node `24.x`;
- latest deployment `dpl_26tawqUKuod6bVCqakmS2mEw1wdz`, `READY`, target `production`, region `iad1`;
- deployment URL `the-observance-kjxn-43ssnpphm-bradens-projects-c5e41066.vercel.app`;
- Git source `main` at `ca5416e477597b6e38f1dc82c9007c814184c980`;
- aliases include `copperlinehosting.com` and the project/main aliases; `aliasError` was null.

The deployment was created at `2026-07-15 20:02:41 -05:00`, before the M2 evidence commit
`2aedeca9198db36b029aaa39f364e7688fbba171` (`21:59:03 -05:00`), the M3 evidence commit
`5d41d17203adee0249a707abeb9f7d854c578035` (`22:14:34 -05:00`), and checkpoint
`d63c48cca3b2964aea1513f704574855b4bf8a72` (`22:15:42 -05:00`). The connector's 20 newest
deployments contained no deployment for those commits; the newest item was the production deployment
above.

Result: **the project and current production identity are live evidence, but exact M2/M3 preview
readiness remains unproved.** No preview was created by this audit, and production was not touched.

### Crafty/Paper availability

- No Crafty connector, authenticated browser session, panel URL, server ID, or committed panel identity
  was available.
- Public DNS resolved `snoikerz.com` to `199.45.204.26` with an observed A-record TTL of 300 seconds.
- A direct TCP availability probe to `snoikerz.com:25569` failed on 2026-07-16; ICMP also timed out.
- Because the Minecraft status port did not answer, no protocol response, MOTD, player count, Paper
  implementation/version, plugin list, world identity, or Crafty state was observed.

Result: **only DNS presence and a failed public endpoint probe are evidenced.** This does not prove that
Crafty is absent or misconfigured; NAT, firewall, sleep/offline state, or an intentionally closed server
remain possible. Paper `1.21.11` remains a repository/Brad declaration, not a current live receipt.

### Media source custody

The authenticated Google Drive profile was `bradnbelew@gmail.com`. Exact canonical/hosted filename
searches, recovered-packet filename searches, `Observance`, and MIME-wide video/audio searches all
returned zero results. Drive therefore did not improve custody and must not be cited as a source store.

The exact local staging path recorded by the dated receipt was readable. The following files were
observed in
`C:\Users\nanob\Documents\Codex\2026-07-07\hey\outputs\manual-media` and matched the committed
receipt exactly:

| Observed file | Bytes | SHA-1 |
| --- | ---: | --- |
| `base_check_06.mp4` | 421,443,427 | `844c2aaf8fb51836add4b59e81abe4131c8d6d0a` |
| `shore_copy_unlisted.mp4` | 132,001,373 | `9b979e349c7a0d7497fd0fe76d0450e744dc39d0` |
| `watch_floor_9_lit.mp4` | 108,195,021 | `9b6552e21ec01e6f046027247a689c8dd78b8ce1` |
| `room_below_noaudio.mp4` | 15,065,612 | `1cb3e600d3e16e9bb1434fa65ddbdff04f512fbd` |

The recovered packet directory and ZIP were also present at the recorded local path. Observed receipts:

| Observed file | Bytes | SHA-1 |
| --- | ---: | --- |
| `recovered-archive-packet.zip` | 62,009 | `783ecde5685abdb601e4a659fc947c32964f70b3` |
| `README.txt` | 389 | `1f27f3a9edc2e348fc12916c5d72509dc6107d50` |
| `inventory_06.txt` | 380 | `83a5c75fcfd4be2072e6df46449124d9a5ccde7f` |
| `field_audio_03.wav` | 58,256 | `2003f0151c1ba643c649b5ed0e19d1b31bb68319` |
| `intake_partial.png` | 34,269 | `1fc1e369ba5e4314496aa66ba9afc4bfc67ae592` |
| `lamp_roll_scan.jpg` | 38,019 | `ca9a7a3d4958fc05c9a620e452bcc1ed95ae3287` |

Result: **the exact four receipted video byte sets and the full five-file recovered packet have a
current local custody location.** This closes the prior “locate these bytes” sub-gap. It does not prove
that these are the highest-quality masters, establish ownership beyond the repository's attribution,
or substitute for Brad's human frame/audio/accessibility review and explicit keep/re-edit/replace
decisions. No media file was opened, copied, moved, edited, uploaded, shared, or rehosted.

## Remaining gates

- Authenticated Railway project/environment/service/deployment identities and live worker/recovery
  configuration parity.
- A Vercel preview deployment/readiness receipt for the exact approved release tuple; the observed
  production deployment is older and does not count.
- Crafty panel/server identity and authenticated availability metadata; live Paper build/version,
  plugin/runtime, restart, non-op, and client receipts.
- Confirmation that the located media bytes are the best surviving masters, plus human AV,
  accessibility, contamination, and editability review and Brad's per-asset decision.
- Disposable Paper build/restart/non-op/asymmetry receipts and Brad's in-game M3 visual approval.

M4 district implementation remains closed. This audit records no Brad approval and authorizes no
production action.

## Validation receipt

- `python tools/check_phase1_architecture.py` — pass.
- `python tools/check_phase2_evidence_architecture.py` — pass.
- `python tools/check_m2_contracts.py` — pass.
- `python tools/check_m3_vertical_slice.py` — pass after adding explicit LF checkout rules for every
  hash-locked M3 package input. Before that rule, a Windows CRLF checkout made all seven exact-byte
  package checks fail even though LF normalization reproduced every committed manifest hash.
- `git diff --check` — pass.
