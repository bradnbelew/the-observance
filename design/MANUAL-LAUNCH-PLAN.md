# THE OBSERVANCE - MANUAL LAUNCH PLAN

> **SUPERSEDED V4 ARCHIVE.** All remaining real-world actions are now enumerated in
> `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`.

> Current verdict: the repo automation can be green while launch is still not approved. This plan is the
> handoff for the work that only a live server, real client, hosted files, and Ethan/operator judgment can
> prove. The final go/no-go command is:
>
> ```powershell
> powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_launch_manual_blockers.ps1 -Launch -CaptureCsv <packet>\coords-capture.csv -RehearsalPacket <packet-dir>
> ```
>
> Do not invite players until that command passes and every manual attestation below is true on the live
> Paper server.
>
> The current high-level verdict is maintained in `design/CURRENT-READINESS-VERDICT.md`.

## Evidence Packet

For easier server testing before a launch promise, generate the normal launch/rehearsal packets plus one
operator-facing test guide:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\prepare_server_test.ps1
```

That command writes `server-test-guide.md` into the rehearsal packet with exact smoke-test, vertical-slice,
full-rehearsal, and launch go/no-go scripts. It still uses the same placement packet, rehearsal packet,
blocker report, media checklist, command sheet, Supabase apply card, and launch attestations as the launch
helper. Use `-SkipBundle` only for a fast dry packet; rebuild before uploading fresh server bytes. After
`observance-resourcepack.zip` is hosted, rerun it with `-ResourcePackUrl <hosted-https-zip-url>` so the
resource-pack config, hosted-byte verification, and generated test guide agree.

Create one folder for the launch rehearsal and keep every artifact there:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\prepare_friend_launch.ps1
```

That command builds the deploy bundle, creates the placement packet, creates the rehearsal packet, writes
the quickstart/blocker/media/command-sheet/todo handoff files, and runs the blocker report. After
`observance-resourcepack.zip` is hosted, rerun it with
`-ResourcePackUrl <hosted-https-zip-url>` so `resource-pack.url` and the current zip SHA1 are written
together. To rerun only the packet generators:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\new_launch_placement_packet.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools\new_rehearsal_packet.ps1
```

The packet must contain:

- `friend-launch-quickstart.md` with the exact generated paths, Supabase SQL SHA1, deploy hashes, hosted
  pack status, and final go/no-go command for this run.
- `launch-blockers.md` with the current machine-readable NOT READY/READY report for this packet.
- `manual-media-checklist.md` with found-footage/archive payloads, ready flags, gates, and withhold rules.
- `supabase-apply-card.md` with the live project id, exact `apply-all.sql` SHA1, ordered bundle count, and
  `/observance status` receipts to copy into attestations after applying SQL.
- `live-server-command-sheet.md` with the exact `/observance` and `/obs unlit` receipts to collect during
  placement, rehearsal, media flagging, and final live audits.
- `friend-launch-todo.md` with the plain ordered live setup checklist and exact receipts to record.
- `coords-capture.csv` filled from live `/observance site set <siteId>` placement.
- `launch-attestations.md` completed from the real Paper/client pass, including the current Supabase SQL SHA1,
  plugin jar SHA1, hosted resource-pack SHA1, and Normal Non-Op Player Pass proof.
- Four proof shots per launch-required site: approach, focal object, answer/action surface, exit.
- Completed `LIVE-REHEARSAL-EVIDENCE.md` fields, with only `KEEP` verdicts for launch surfaces.
- Clips for first hour, scare families, Unlit route, Record/web jump, and finale.
- `fixes.md` containing either no unresolved blockers or links to the replacement proof after each fix.

## Manual Tasks

| Order | Task | Why it matters | Where it belongs | How to do it | When | Player-discovery requirement | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Regenerate and apply Supabase SQL | Discord, web Record/Archive, puzzle gates, solves, observations, and operator status all depend on the live schema matching the repo. | Live Supabase project `fdnmhbpxnodrnbrzrlqq`; plugin `/observance status`. | From `discord/`, run `npm run db:seed`; paste all of `discord/supabase/apply-all.sql` into Supabase SQL Editor; run it once; do not paste loose migrations or `apply-tonight.sql`. | Before plugin live testing. | Players should only see diegetic progression; if DB drift exists, signs, reports, Record pages, and flags can silently miss. | `/observance status` shows `supabase configured: true`, `last db call ok: true`, `queued writes: 0`. |
| 2 | Build and deploy plugin/datapack/resource pack together | The ARG is cross-surface; testing only one layer can hide broken runes, sounds, dimensions, commands, and producers. | Paper 1.21.11 server, `plugins/`, world `datapacks/`, server resource pack config. | Run `tools\package_launch_bundle.ps1`, then run the repo audit. Upload `plugin/build/libs/observance-0.3.29.jar`, record its SHA1 from `observance-deploy-manifest.json`, install the datapack, host `observance-resourcepack.zip`, run `tools\set_resource_pack_config.ps1 -Url <hosted-https-zip-url>` so `resource-pack.url` and `resource-pack.sha1` move together, run `tools\check_hosted_resource_pack.ps1` to prove the hosted bytes match, then run `/observance status` after rehearsal clients join. | Before any in-client proof. | Players must load the exact current plugin and custom font/sounds as part of the world, not as operator explanation. | Server console has no plugin/datapack/pack errors; `/observance status` shows `pack readiness` with every rehearsal client `LOADED`; `launch-attestations.md` records the exact plugin/resource-pack SHA1 values from the current deploy manifest; `check_launch_manual_blockers.ps1` accepts URL/SHA1 and hosted byte verification. |
| 3 | Rotate exposed credentials | A friend launch should not run on keys that may have appeared in local files, logs, screenshots, or chat. | Supabase service role, Discord bot token, Render/Vercel env, local plugin config. | Rotate the service role and bot token; update live env/config; restart services; verify status. | After deploy config is settled, before players join. | Invisible to players; protects the live fiction and data. | Fresh keys in live systems; no old key works; `/observance status` still passes. |
| 4 | Build/proof the Deep Hold and place outside-Hold launch anchors | Authoring placeholders are safe no-ops, but launch needs the generated Hold proven in situ and every bespoke outside-Hold route anchored at real walkable locations with sightlines and body language. | Live world `plugins/Observance/sites.yml`; proof packet `coords-capture.csv`. | In game, stand at the intended surface mouth and run `/observance placehold build`, `/observance placehold audit`, and `/observance placehold sync`; record `GeneratedProof` for Hold-owned rows. Then use `/observance site todo` and `/observance site plan lanes` for the remaining outside-Hold lanes. Work one lane at a time with `/observance site next prologue|dimensions` or inspect a specific outside-Hold row with `/observance site plan <siteId>`, then stand at the real anchor and run `/observance site set <siteId>`. Export/fill the lane-aware capture CSV and validate it. | After the world route exists, before rehearsal. | Every site must be discoverable from the clue or surrounding world; no floating marker, test pad, or labeled route may be required. | `check_world_build_readiness.ps1 -Launch` passes; `check_launch_coord_quality.ps1 -Launch -CaptureCsv <packet>\coords-capture.csv` passes. |
| 5 | Build and validate major world surfaces | Placement alone is not enough; books, signs, item lore, structures, runes, light, sound, protection, and NPC claims must read in Minecraft. | Major sites, keeper stones, townsfolk area, Deep Hold, Undercroft, Unlit, finale chamber. | Use the operator account for build/audit commands, then join as a real non-op player account. Inspect every major surface in `LIVE-REHEARSAL-EVIDENCE.md`; run `/observance preflight`, `/observance visualaudit`, `/observance dialogueaudit`, `/obs unlit audit`, `/obs unlit ready`. Prove the player cannot freely break/build inside the Deep Hold protection region, can locate answer input surfaces, can attempt one wrong answer, can submit one correct answer/input, can retrace/return without state loss, and can feel one Unlit pressure action. | After site placement, before inviting friends. | Players should infer the next action from the build, line, object, or sound itself; no out-of-fiction explanation. | All in-game audits pass; packet screenshots show legible approach/focal/action/exit surfaces; `launch-attestations.md` includes `Normal Non-Op Player Pass` evidence. |
| 6 | Stage launch beats | The built mechanics still need live staging: prologue, townsfolk, Unlit, reading fragments, and finale markers. | Live world and operator commands. | Run `/observance placeprologue`; spawn townsfolk; follow `UNLIT-PREARG-STARTUP.md`; run `/observance reading` after keeper sites are placed; run `/observance finale` in the Seventh chamber. | After placement, before the rehearsal route reaches each beat. | Players should meet the beats as events in the world, not as admin-spawned props. | Rehearsal clips show the cold open, social surface, Unlit handoff, reading fragments, and finale markers in context. |
| 7 | Complete live rehearsal packet | Static checks prove wiring; rehearsal proves scale, pacing, fairness, dread, retraceability, and live-only readiness. | `rehearsals/<date>/`, `design/LIVE-REHEARSAL-EVIDENCE.md`, and packet `launch-attestations.md`. | Run the route as a player would. Capture required screenshots/clips, mark every surface `KEEP`, complete live attestations, and fix/retest every `RESHAPE`, `REPLACE`, or `CUT`. Validate with `check_rehearsal_packet.ps1`. | After staging, before session zero/final launch decision. | The first hour must feel haunted before it feels like a puzzle course; side paths must change belief, create dread, or confirm a motif. | `check_rehearsal_packet.ps1 -PacketDir <packet-dir>` passes; no unresolved `fixes.md` blockers; `launch-attestations.md` decision is `LAUNCH`. |
| 8 | Verify external media choices | Some artifacts are optional enrichment, but live clues to missing downloads are not acceptable. | Dashboard public files, Record pages, Discord/archive rows, in-world lure clues, `design/MANUAL-MEDIA-PACKET.md`, `design/MANUAL-MEDIA-STAGING.md`. | B1 found-footage is produced, hosted on YouTube, reachable, and operator-checked, but still gated by the `media_clip_0N_ready` flags until each matching story gate. B2 `the-hold.zip` is present at `dashboard/public/the-hold/the-hold.zip`; verify the deployed `/the-hold/the-hold.zip` URL before planting the in-world lure clue, and do not plant the in-world lure clue until that deployed URL works. B3 recovered archive is hosted on Dropbox, reachable, correctly populated, and spectrogram-checked, but still gated by `recovered_archive_ready`. The launch media payloads are fixed: `ASH-13`, `where the reeds fold back`, `stay awake`, `six return one is not kept`, and spectrogram `i was not kept`. | Before any clue that sends players outside Minecraft. | A player following a clue should find an artifact, not a placeholder or a 404. | Web route works from the clue path; missing optional media is either withheld or clearly not planted; every external artifact has a hosted URL, exact payload, extraction method, intended gate, and incognito reachability proof recorded. |
| 9 | Run session zero and consent | Observer and voice systems are part of the fiction, but consent is not optional. | Out-of-fiction pre-game conversation; `design/SESSION-ZERO.md`. | Read the session-zero script; explain behavior/chat/voice observation, opt-out, debrief, and optional tiers. Keep `observer_capture` and `voice_capture` off until you have handled consent; set `players.observer_opt_out = true` for anyone you choose to exclude from capture. | Immediately before friends join the fiction. | The fiction can feel watched only after the humans understand the boundary. | Consent/opt-out notes recorded; observer/voice settings match the group decision. |
| 10 | Run final go/no-go | This prevents a green repo audit from being mistaken for launch approval. | Repo root and live server. | Run `tools\audit_all.ps1`, then run the launch blocker command with the real capture CSV and rehearsal packet. If it fails, fix the named blocker and rerun. | Last action before launch. | Players only enter after every repo-verifiable and live-attested blocker is cleared. | `check_launch_manual_blockers.ps1 -Launch ...` exits 0 and printed manual attestations are true. |

## Stop Conditions

Stop and fix before launch if any of these are true:

- Any outside-Hold launch-required site still has placeholder coordinates, or any Deep Hold row lacks generated-room proof.
- Any coordinate row is not `KEEP` or lacks proof shots.
- Any major site needs operator explanation to identify the important object.
- Any NPC line names a place, object, route, or custom that is not physically or mechanically proven.
- Any book, sign, item lore, title, actionbar, bossbar, rune glyph, sound, or particle is illegible or missing in a real client.
- A normal non-op player can freely break/build inside the Deep Hold, cannot find answer input, cannot
  recover from a wrong-answer attempt, cannot make a correct answer/input, cannot retrace/return, or never
  experiences an Unlit pressure action.
- Any in-world clue points to a missing web/download artifact.
- Any required command audit fails on the live server.
- Any consent/opt-out state is unclear before enabling observation or voice capture.
- Any previously exposed credential remains active.

## Launch Verdict Template

Use this exact language in the final operator note:

```text
Launch verdict:
- Repo automation: PASS/FAIL
- Launch blocker command: PASS/FAIL
- Supabase live status: PASS/FAIL
- Resource pack hosted and hash-matched: PASS/FAIL
- Generated Hold proof plus outside-Hold launch coordinates proofed: PASS/FAIL
- Live Minecraft client rendering/audio/text check: PASS/FAIL (`launch-attestations.md`)
- Normal non-op player pass: PASS/FAIL (`launch-attestations.md`)
- Plugin jar SHA1 matches current repo package: PASS/FAIL (`launch-attestations.md`)
- Resource pack SHA1 matches hosted zip: PASS/FAIL (`launch-attestations.md`)
- Rehearsal packet: PASS/FAIL
- Session-zero consent/opt-out: PASS/FAIL (`launch-attestations.md`)
- Credential rotation: PASS/FAIL (`launch-attestations.md`)

Decision: LAUNCH / DO NOT LAUNCH
Reason:
```
