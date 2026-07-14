# The Observance — Final Launch Handoff (2026-07-13)

> **SUPERSEDED V4 ARCHIVE.** This handoff predates the V5 rewrite. Use
> `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`.

This is the current operator handoff. The repository, deploy artifacts, Deep Hold V4 generator, and isolated
Paper rehearsal are green. A public/friend launch is intentionally still gated on live-world placement,
live Supabase application, a real non-op client rehearsal, consent, and credential rotation. Those receipts
cannot be manufactured from the repository and must be entered in the generated 2026-07-13 packet.

## Frozen deploy artifacts

Target: Paper 1.21.11, Java 21, Observance 0.3.29.

| Artifact | SHA-1 |
| --- | --- |
| `plugin/build/libs/observance-0.3.29.jar` | `eda0e218d7a074a5c150b4781161e2da01c60ead` |
| `observance-datapack.zip` | `e056783e9829d25a6965c7cf04b16e58afcb1969` |
| `observance-resourcepack.zip` | `fdc15d25e1cc5811269a3405091f7b5a143ac6db` |
| `discord/supabase/apply-all.sql` | `4e0e923fc10ae7049388baa173cd706568f191ca` |

The configured public resource-pack URL was downloaded and byte-verified against the resource-pack SHA-1
on 2026-07-13. Re-run `tools\check_hosted_resource_pack.ps1` if the URL or file changes.

## Completed machine and world proof

- Root story/data/showrunner/dashboard/plugin/datapack/resource-pack checks pass.
- Dashboard lint and production build pass; all canonical Record surfaces carry the invented alphabet mark.
- Eleven OGG assets pass existence, Vorbis, mono, and duration checks.
- Keeper NPC clicks, Wren history, Iss warm-to-cold restaging, dossier targeting, FACT9, and atonement use the
  real four-column plugin event format and pass TypeScript compilation plus showrunner selftests.
- In an isolated Paper 1.21.11 world, Deep Hold V4 built 32 rooms, 76 canonical fixtures, eight gates, and
  seven district records plus the covered entrance copy at Surface Mouth `(1000, 55, 0)`.
- The Hold audit reported 76/76 sites, 8/8 gates, 8/8 records, a protected entry stair/Hold, virtual-open
  full traversal, and zero critical findings.
- Every gate was opened and audited, then resealed and audited. The result remained zero-critical.
- Exact-coordinate rebuild was idempotent. An offset build whose envelope intersected the first Hold was
  refused before block placement. Restart persistence and the final sealed audit remained green.
- Visual audit reported 64 KEEP, 0 RESHAPE, 0 REPLACE.
- The Unlit Deep now persists per-world/per-night entry, broken, and kept windows across restart; it
  reports either outcome once, never names the breaker publicly, and physically withdraws/returns the
  protected Accepting-floor glow.

The isolated test is strong generator/runtime proof. It does not substitute for walking the final live
terrain, NPCs, dimensions, Unlit, and resource-pack rendering with a non-op client.

## Live setup order

1. Back up the live world and database. Use the mouth coordinates chosen in the village well; the entrance
   to Unlit remains there because existing NPC dialogue depends on it.
2. Apply only `discord/supabase/apply-all.sql` to the live Supabase project. Record its SHA-1 in
   `rehearsals/2026-07-13/launch-attestations.md`.
3. Upload the frozen plugin jar to `plugins/`, install the datapack in the live world's `datapacks/`, and
   keep the verified resource-pack zip at the configured direct HTTPS URL.
4. Configure `plugins/Observance/config.yml` with the live Supabase service-role secret. Restart Paper; do
   not use `/reload` for plugin/config changes.
5. From the intended Surface Mouth, run `/observance placehold build`, then `/observance placehold audit`.
   Do not continue unless it reports 76/76 sites, 8/8 gates, 8/8 records, walkable entry, full virtual-open
   traversal, and zero critical findings.
6. Place/prove the remaining outside-Hold anchors using the exact order and commands in
   `rehearsals/2026-07-13/live-server-command-sheet.md`. A survey from `/obs site set` is not a stamped
   placeworld receipt; use `/obs placeworld` for those rows. Stamp `nether_forge` in the Nether and
   `end_seventh_shrine` in the End.
7. Keep the Unlit entrance in the village well. Build and validate the mirrored expedition with
   `/obs unlit audit`, `/obs unlit ready`, and all five `/obs unlit pass ...` checks from the command sheet.
8. Spawn and inspect the required NPCs. Citizens2 is preferred; the Keeper/Wren armor-stand fallback exists,
   but the final client rehearsal must prove the chosen production mode and every required dialogue surface.
9. As a non-op player, run the complete day-one-to-finale rehearsal: resource pack, prologue discovery,
   answer normalization, leave-and-return routes, eight Hold gates, Keeper callbacks, Failed Accepting six-file
   repair, Nether/End evidence, Unlit, Record/Discord handoffs, Accepting rite, and both finale choices.
10. Fill `build/launch-placement/2026-07-13/coords-capture.csv`, all screenshots/clips, rehearsal notes, fixes,
    and launch attestations with real evidence. Complete `design/SESSION-ZERO.md` and rotate exposed secrets.
11. Run the final command below. Invite players only when it exits successfully and the attestation decision
    is `LAUNCH`.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_launch_manual_blockers.ps1 -Launch -CaptureCsv "D:\the-observance\build\launch-placement\2026-07-13\coords-capture.csv" -RehearsalPacket "D:\the-observance\rehearsals\2026-07-13"
```

## Operator packet

- `rehearsals/2026-07-13/friend-launch-quickstart.md` — concise install and go/no-go order.
- `rehearsals/2026-07-13/live-server-command-sheet.md` — exact Minecraft commands and receipts.
- `rehearsals/2026-07-13/server-test-guide.md` — smoke, vertical-slice, and full-rehearsal procedure.
- `rehearsals/2026-07-13/friend-launch-todo.md` — remaining live work in order.
- `rehearsals/2026-07-13/launch-blockers.md` — current machine-readable manual blocker result.
- `build/launch-placement/2026-07-13/coords-capture.csv` — required live placement/visual proof ledger.
- `design/DEEP-HOLD-V4-SERVER-SETUP.md` — Deep Hold-specific build, safety, and recovery reference.

Do not copy proof from the isolated world into the live packet. The packet is the final-world receipt, and
its remaining unchecked fields are deliberate launch interlocks.
