# THE OBSERVANCE - CURRENT READINESS VERDICT

> **SUPERSEDED V4 ARCHIVE.** The current V5 verdict is produced by the release checks and live
> evidence required in `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md`.

Date: 2026-07-13

> **CURRENT RELEASE-CANDIDATE OVERRIDE.** The release candidate is **repo-ready but not
> launch-ready**. Canon, puzzle/input fairness, producers/consumers, all eight Deep Hold gates,
> books/signs/items, protection, one-mouth retraceability, Keeper/Wren, Discord/showrunner, database
> bundle, dashboard, packs, and finales are implemented and automated-audit clean. Deep Hold V4 has
> been physically exercised on isolated Paper 1.21.11, including open/reseal, restart persistence,
> idempotent rebuild, collision refusal, and route/visual/dialogue audits. The configured hosted
> resource-pack bytes match the local SHA1. Unlit Deep now has durable, once-only kept and broken
> outcomes. A 2,300-case chaos suite adds 22,635 invariant checks.
>
> What remains is irreducibly production/manual: apply and receipt-check live Supabase; place V4 in
> the backed-up production world at the village well; place/proof outside-Hold sites; configure NPC
> bodies; run real-client presentation/fallback and full-route rehearsals; record session-zero consent;
> rotate credentials; complete `launch-attestations.md`; and pass
> `check_launch_manual_blockers.ps1 -Launch`. Exact order: `FINAL-LAUNCH-HANDOFF-2026-07-13.md`.
>
> Canon correction: the Undercroft is the bottom of the Hold; the Nether is the older source of the
> one lent fire, never another bottom. `sites.yml` coordinate sentinels are outside-Hold launch work,
> not evidence that V4 itself is a placeholder. The dated detail below is retained as the pre-V4
> readiness baseline; where it conflicts with this override, this override wins.

# Historical 2026-07-08 readiness baseline

Date: 2026-07-08

## 1. High-Level Verdict

The project is **repo-ready but not launch-ready**.

Automated code/content checks currently prove that the authored story graph, puzzle data, side quests,
voice/register rules, Discord runtime policies, dashboard build, plugin source/jar packaging, datapack/resource
pack packaging, media file format checks, operator docs, rehearsal packet generation, launch placement packet
generation, and manual blocker gate are wired and passing.

Launch is still blocked by work that only a real live server/client/operator pass can prove: hosted resource pack
URL/SHA1 plus hosted-byte verification, generated Deep Hold proof, real outside-Hold launch coordinates,
completed coordinate/proof CSV, completed rehearsal packet, completed `launch-attestations.md`, live Supabase
status, live Paper/client rendering, session-zero consent, and credential rotation.

## 2. Ready

- Story/puzzle graph: 68 puzzle rows, 66 puzzle keys, 170 normalized accepted answers, side-quest rows, gates,
  reachability, duplicate-answer handling, and resolver behavior are covered by automated audits.
- Lore/canon spine: Watcher/Seventh/keeper identity reconciliation is represented in current docs and protected
  by specs/register checks.
- Puzzle fairness scaffolding: every audited puzzle row has rescue/hint coverage or an explicit exemption.
- Discord runtime: gate matching, showrunner autonomy, custom reports, companion, finale/release, Observer echo,
  archive body resolution, and scenario tests pass.
- Dashboard/web: lint, selftests, and production build pass; the local `the-hold.zip` lure download is present
  under `dashboard/public/the-hold/`, and the Record route still withholds the link safely if that file is absent.
- Plugin source and package: Java 21 source compile passes for 153 files, the jar check verifies the deployable
  plugin jar contents, and the checked-in Gradle wrapper builds/tests the plugin.
- Datapack/resourcepack assets: JSON/reference/format-metadata/zip checks pass; the `observance:runes` bitmap atlas is
  pinned to A-Z/0-9, the resource-pack zip excludes docs/non-runtime files, and the current deploy hashes live in
  `observance-deploy-manifest.json`.
- Audio files: 11 OGG files are present, mono Vorbis, and duration-checked.
- Operator tooling: launch-bundle packager, resource-pack config setter, hosted resource-pack byte verifier, launch coordinate
  packet generator, coordinate quality checker, live rehearsal packet generator/validator,
  deploy-manifest writer/checker, Unlit readiness checks, manual launch blocker gate, and operator-doc drift
  checks pass.
- World template wiring: `/observance placeworld`, `placeroom`, `placeregion`, and `placedeep` route through
  the rich `StructureTemplates.keeper(...)` dispatcher, and the world-build readiness check guards against
  falling back to generic keeper stones.
- DB/plugin integration: the optional FAWE `world_paste_ledger` path is no longer half-built; migration
  `0012_world_paste_ledger.sql`, `schema-repair.sql`, and regenerated `apply-all.sql` now create the
  table used by the plugin's durable single-paste guard.
- Beat queue durability: the plugin now atomically claims `approved -> firing` before world mutation, and
  the DB status checks include `firing`, reducing the old restart/two-instance double-fire window.
- Plugin DB contracts: `tools/check_plugin_db_contracts.ps1` now guards the row/schema agreements that
  previously caused silent 400-class failures: event log columns, base upsert key, settings jsonb parsing,
  paste ledger support, and beat `firing` status.
- Signature beat registration: the plugin compile check now guards that `RevealBeat`, `RoomSwapBeat`,
  `KeeperNpcBeat`, `ModeledMobBeat`, and `SpatialVoiceBeat` remain registered in `BeatLibrary`.
- Companion/reckoning/co-op producers: Wren trust/reveal/reckoning flag producers, the two in-world
  co-op legs, the Seventh restore/erase choice producer, the Discord companion consumer, and the
  `#the-record` co-op word closer are wired and guarded by `tools/check_companion_arc_contracts.ps1`.
- Consent/observer safety in code: observer capture checks opt-out before storage on Discord and Minecraft
  chat paths, and voice capture stays behind the global off-by-default `voice_capture` switch plus the same
  opt-out floor.
- Active-source placeholder pass: live player-facing/code-delivered placeholder hits were reviewed. Stale
  structure-template "notation TBD" comments and the misleading "placeholder enactor" wording were cleaned;
  the real sigil exists at `brand/sigil.svg`; remaining active hits are intentional coordinate sentinels,
  HTML input placeholders, or internal guard/test wording.
- Minecraft text-surface static fit: `tools/check_minecraft_text_surfaces.ps1` now verifies authored structure
  sign lines, structure book titles/pages/hard-lines, direct actionbar literals, tooltip literals, and the
  runtime text-fit guards for sign/book/HUD/item beats.
- Rune alphabet cohesion: dashboard Record glyphs now self-test against the canonical Discord/resource-pack
  alphabet so the web rune marks cannot drift from the in-world font silently.
- Record terminal difficulty: the integrity/error log now keeps a cold-open terminal at tier 1 until the
  group has actually advanced the web; a guessed or early-discovered URL can no longer surface tier-3
  hint text immediately.

## 3. Not Ready

- Resource pack is built but not hosted/configured in launch config.
- `resource-pack.sha1` is blank in the plugin config until the hosted zip is chosen.
- Hosted resource-pack bytes have not been downloaded and hash-verified against the current zip.
- The generated Deep Hold has not been live-built/proofed, and outside-Hold launch-required sites still have authoring placeholder coordinates.
- No completed launch coordinate/proof CSV has been supplied.
- No completed live rehearsal packet has been supplied.
- No completed `launch-attestations.md` exists for live Supabase, exact plugin/resource-pack deploy hashes,
  real-client rendering, command audits, consent, external media, and credential rotation.
- Live Minecraft behavior is not proven until Paper 1.21.11, plugin, datapack, resource pack, and real clients are
  tested together.
- External media has been supplied and operator-checked, but it is not launch-armed: found-footage clips and
  the recovered archive remain story-gated by dormant flags, and the deployed `/the-hold/the-hold.zip` URL
  still needs live-route verification before planting the lure clue.

## 4. Contradictions Or Mismatches

- Historical docs still contain older "green/launch-ready/code-complete" language. They now have current-status
  overrides and are guarded by `tools/check_operator_docs.ps1`, so the mismatch is called out rather than silent.
- `sites.yml` intentionally contains placeholder coordinates. That is acceptable for authoring but is a launch
  blocker; `check_world_build_readiness.ps1 -Launch` and the manual blocker gate enforce the distinction.
- The resource pack zip exists and hashes correctly, but launch config does not point to a hosted copy yet and
  the hosted bytes have not been verified.
- Rehearsal and launch evidence can be generated, but no real packet proves live readiness yet.
- Optional media wiring exists and the external artifacts are now hosted/checked, but all media flags must
  remain dormant until the matching story gates.
- Record lure withholding is guarded for the current page, and the actual `the-hold.zip` artifact is now
  present locally; the deployed dashboard URL still needs verification before the in-world clue is planted.

## 5. Stale, Placeholder, Unfinished, Forgotten, Or Weak Content

- Stale launch-ready claims have been converted into historical context with overrides; any reappearance is a doc
  check failure.
- Placeholder coordinates remain for all launch-required sites and are explicitly not shippable.
- The live rehearsal packet and `launch-attestations.md` are templates until a real server/client run fills them.
- Optional external media is produced and documented, but remains unarmed until the story gates and deployed
  clue paths are verified.
- `spine-recovered-archive` is wired as an optional terminal lore puzzle, not a showrunner drip. The Dropbox
  archive and spectrogram payload now exist, but the path must stay unplanted until `recovered_archive_ready`
  is intentionally flipped at the right story gate.
- `arc/cipher-web-seed.sealed.json` still contains the sealed true-threshold staging answer `0 0`; it is
  inactive and spoiler-sealed, but should be replaced with the real built threshold coordinate during staging
  if that sealed path is activated.
- Audio is technically valid, but taste/mix/timing are live-client polish items, not proven by file checks.
- Real Minecraft formatting for books, signs, item lore, titles/actionbars, bossbars, runes, sounds, particles, and
  NPC lines remains unproven until the live client pass.
- Resource-pack rune atlas coverage is automated for A-Z/0-9, but real-client readability still needs the live
  resource-pack pass.

## 6. Systems Needing Verification Or Re-Testing

- Supabase live project after applying `discord/supabase/apply-all.sql`.
- Paper 1.21.11 plugin/datapack/resourcepack load together.
- Real client rendering of all text, glyphs, sounds, particles, bossbars, titles, actionbars, signs, books, and item
  lore.
- Static Minecraft text fit now has an automated guard, but it still needs a real client pass for font/glyph
  rendering, lighting, distance, and resource-pack fallback.
- Record/web lure behavior should be rechecked during rehearsal: the exact decoded route must expose the real
  `the-hold.zip` file from the deployed dashboard and no placeholder.
- Record terminal answer-writing and integrity hints need live route rehearsal with real Supabase state:
  verify neutral non-answer copy, no puzzle identity leak, tier-1 cold-open behavior, and 24h/72h escalation
  only after real elapsed stall.
- FAWE schematic-paste path should be rechecked if enabled live: the DB table now exists, but a real
  server with FAWE installed should still prove duplicate claims skip rather than paste twice.
- Beat queue recovery should be observed during live rehearsal: claimed beats should move from `firing` to
  `fired`/`skipped`/`failed`, and no beat should remain in `firing` after a normal run.
- Plugin DB writes should still be verified on live Supabase after applying `apply-all.sql`: event logs,
  base detection, watcher sleep, and beat-queue transitions should be watched once on the real project.
- Companion/reckoning/co-op should be rehearsed live: Wren spawn/right-click trust, Iss-caught reveal,
  reckoning marker choice, co-op plate/carve plus Discord word close, and Seventh restore/erase marker
  should each produce the expected DB flag/event and in-world or Discord payoff.
- `/observance preflight`, `/observance visualaudit`, `/observance dialogueaudit`, `/obs unlit audit`, and
  `/obs unlit ready` after live placement.
- Generated Deep Hold proof plus outside-Hold coordinate placement/proof CSV for all launch-required anchors.
- Full live rehearsal route, including first hour, side paths, scare families, Unlit, Record/web jump, and finale.
- Session-zero consent/opt-out behavior before enabling observer or voice capture.
- Credential rotation after live config is finalized.

## 7. Manual Tasks Still Required

Use `design/MANUAL-LAUNCH-PLAN.md` as the detailed operator checklist. In short:

- Apply and verify Supabase SQL.
- Deploy plugin, datapack, and hosted resource pack together, then verify the hosted pack bytes.
- Rotate exposed credentials.
- Build/proof the generated Deep Hold and place/proof all outside-Hold launch-required coordinates.
- Validate world surfaces and run live command audits.
- Stage launch beats: prologue, townsfolk, Unlit, reading fragments, and finale markers.
- Complete the live rehearsal packet and `launch-attestations.md`, including exact plugin jar and resource-pack
  SHA1 evidence.
- Verify deployed external media routes and keep story-gated media flags dormant until intentionally armed.
- Run session zero and record capture switch state plus any `observer_opt_out` choices.
- Run the final go/no-go command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check_launch_manual_blockers.ps1 -Launch -CaptureCsv <packet>\coords-capture.csv -RehearsalPacket <packet-dir>
```

## 8. Manual Task Detail

Each manual task must record:

- what it is,
- why it matters,
- where it belongs,
- how to do it,
- when to do it,
- how players actually find or experience it,
- and what evidence proves it.

The canonical detail lives in `design/MANUAL-LAUNCH-PLAN.md`; the proof artifacts live in the launch placement
packet, rehearsal packet, and `launch-attestations.md`.

## 9. Step-By-Step Manual Completion Plan

1. Generate launch placement and rehearsal packets.
2. Apply Supabase SQL and verify `/observance status`.
3. Deploy plugin/datapack/resourcepack, host/hash the pack, and run `tools\check_hosted_resource_pack.ps1`.
4. Build/proof the generated Deep Hold, then place outside-Hold launch sites with `/observance site todo|next|plan|set`.
5. Validate GeneratedProof, outside-Hold coordinates, and proof shots with the capture CSV in launch mode.
6. Stage prologue, townsfolk, Unlit, reading fragments, and finale markers.
7. Run live command audits and fix every failure.
8. Rehearse first hour, major sites, side paths, scares, Unlit, Record/web, and finale.
9. Complete `launch-attestations.md`.
10. Run session zero and set `observer_capture`, `voice_capture`, and any `observer_opt_out` choices
    according to consent.
11. Rotate credentials and re-verify live status.
12. Run the final launch blocker command in `-Launch` mode.

## 10. Launch/Readiness Checklist

- `tools\audit_all.ps1` passes.
- Hosted resource pack URL is set.
- Hosted resource pack SHA1 matches `observance-resourcepack.zip`.
- `tools\check_hosted_resource_pack.ps1` proves the hosted zip bytes match the configured SHA1.
- `check_world_build_readiness.ps1 -Launch` passes.
- `check_launch_coord_quality.ps1 -Launch -CaptureCsv <packet>\coords-capture.csv` passes.
- `check_rehearsal_packet.ps1 -PacketDir <packet-dir>` passes.
- `launch-attestations.md` ends with `decision: LAUNCH`.
- `launch-attestations.md` contains the current plugin jar SHA1 and hosted resource-pack SHA1.
- Manual blocker gate exits 0 in `-Launch` mode.
- No player-facing clue points to a missing file, route, download, or unplaced site.

## 11. Extra Suggestions

- Keep optional media withheld until it is tested from the exact clue path players will follow and its story
  gate is due.
- Keep the dashboard rune port and the resource-pack atlas in lockstep with the canonical Discord rune table;
  the added selftest should fail any future drift.
- Keep the Record terminal hard but fair: use it as a safety valve for stalled groups, not as a cold-start
  walkthrough. The new policy selftest pins that boundary.
- Treat audio mix/timing as a rehearsal item even though file format checks pass.
- Preserve the current no-handholding difficulty; fix opaque moments with retraceable in-world evidence, not
  explicit instructions.
- Commit at clear boundaries so launch-blocker hardening and unrelated creative tuning remain easy to review.

## 12. Looked Built But Was Not Truly Finished

- Resource pack: built and hashable, but not launch-configured until hosted URL/SHA1 are set and hosted bytes
  are verified.
- World build: sites exist in config, but launch-required anchors are still placeholder coordinates.
- Rehearsal process: templates and validators exist, but no real completed packet has been supplied.
- Live manual attestations: now enforceable through `launch-attestations.md`, but not completed.
- Historical launch docs: looked authoritative but overstated readiness; now overridden and guarded.
- Observer/voice capture consent: code paths are now safer, but live session-zero consent and settings still
  need proof.
- Optional recovered-archive media: the puzzle row exists and is solvable, and the external Dropbox archive plus
  spectrogram payload are now hosted/checked; the remaining work is story-gate arming and live clue-path proof.
- Record lure download: the page is correctly wired to expose `the-hold.zip` when present and withhold it when
  absent; the remaining work is deployed URL verification before the lure clue goes live.
- Sealed true-coordinate path: the sealed detail file still carries `0 0` as a staging coordinate answer; safe
  while inactive, not acceptable if that sealed row is promoted.
- FAWE paste ledger: looked partially built because the plugin had `claimPasteLedger`, but the bundled DB
  schema did not create the table. This is now fixed in migration, repair, and regenerated apply-all.
- Beat queue idempotency: looked process-safe because of the in-memory `inFlight` set, but was not durable
  across restart or a second plugin instance. It now claims in the DB before enacting.

## 13. Easy-To-Miss Items Now Explicitly Called Out

- Generated Deep Hold proof plus outside-Hold launch-required coordinate anchors.
- Four proof shots per launch-required placement.
- Real-client rendering of books, signs, item lore, titles/actionbars, bossbars, custom rune font, sounds,
  particles, NPC lines, and resource-pack fallback behavior.
- Supabase live status after applying the bundled SQL.
- Session-zero consent before observer/voice capture.
- Credential rotation before public/friend launch.
- Optional media gate discipline so players never hit a 404, placeholder artifact, or out-of-order reveal.
- Rune alphabet drift across Minecraft, Discord, and web Record surfaces is now explicitly audited.
- Record terminal hint escalation drift is now explicitly audited, including the no-prior-solve case.
- `spine-recovered-archive` specifically: do not surface its Drive/spectrogram trail until that artifact exists.
- Sealed coordinate residue: replace the `0 0` true-threshold staging answer before using the sealed row.
- `launch-attestations.md` as the human evidence file for facts automation cannot inspect.
