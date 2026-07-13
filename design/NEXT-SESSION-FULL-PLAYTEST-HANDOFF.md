# Next Session: Complete Setup And Playtest Handoff

Use this handoff to start a new Codex session whose job is to guide the operator through The
Observance from an installed repository to a genuinely clean, fully placed, fully tested live ARG.
The session must use plain English, give exact commands and locations, wait for real results, and
never replace a failed check with an assumption.

## Paste This Into The New Session

```text
We are in D:\the-observance. Guide me interactively through the complete production setup and a
start-to-finish playtest of The Observance. Use plain English. Do not skip steps or summarize a
group of actions as "and so on." Tell me exactly which computer, website, dashboard, folder,
Minecraft world, dimension, position, player mode, and Discord channel to use. Give every command
exactly as I should type it. After each small phase, tell me the exact expected result and wait for
me to paste the result or confirm what I saw before continuing.

Start by reading design/NEXT-SESSION-FULL-PLAYTEST-HANDOFF.md completely. Then inspect the current
code, generated artifacts, command registration, SQL schema, website routes, Discord worker, and
current git state. Treat executable code and live receipts as truth; do not assume an older design
document is accurate merely because it says something is complete.

This must cover everything: backups; clean test/live-state strategy; Supabase schema and deliberate
progression reset; website and Hold ZIP; Discord bot deployment, permissions, slash commands, and
dormant-state behavior; Paper/Java version; plugin jar; datapack; hosted resource pack; configs and
secrets; first boot; Deep Hold placement and audit; Copperline relay; opening report; every non-Hold
site; Nether and End structures; the observance_unlit world and every Unlit fixture; NPCs; books,
lecterns, signs, containers, items, sounds, textures, gates, protections, routes, recovery paths and
Adventure-mode accessibility; all puzzle inputs and outputs; all story flags; all web and Discord
handoffs; all movements from the prologue through the final release; reset-after-rehearsal; Session
Zero; final arming; and a written go/no-go receipt.

Important known incident: at about 8:00 PM on July 12, 2026, the newly connected Discord bot posted
the opening line, a SirNan custom report, and the endgame Watcher/Seventh reveal. The database had
old telemetry plus seventh_named and bowed_as_one. The later 8:40 PM onboarding commit did not itself
reset that data. Do not let players enter until the spoiler message is removed and a clean-state
audit proves the bot is dormant, late-game flags are false/absent, telemetry is intentionally clean,
and the showrunner has no stale finale high-water mismatch. Reapplying apply-all.sql is not a reset.

Do not run a full solution path against the live database and then call it launch-ready. Either use
an isolated rehearsal database/world and then validate the clean live setup non-destructively, or
perform a documented, reviewed reset after the rehearsal. Before any destructive reset, show me
exactly what will be preserved, backed up, and cleared, and get my confirmation.

Use the current observance-deploy-manifest.json hashes, not hashes copied into prose. Preserve any
unrelated or untracked user files. At every phase, use STOP and fix if the observed output differs
from the expected output. Do not advance merely because a command ran.
```

## Current Verified Orientation

- Repository: `D:\the-observance`
- Target server: Paper 1.21.11 on Java 21
- Current plugin version: 0.3.29
- Artifact truth: `observance-deploy-manifest.json`
- Database bundle: `discord/supabase/apply-all.sql`
- Public site: `https://copperlinehosting.com`
- Hold download: `https://copperlinehosting.com/the-hold/the-hold.zip`
- Hold ZIP SHA-1 at the time of this handoff: `8a4986422a4af6c65b47f76c61a1e75421b568d4`
- Hold result: `snoikerz.com:25569`
- Copperline relay callback: `9137`
- Ticket surface: `https://copperlinehosting.com/support/ticket.php?id=1842`
- Discord ignition: each player links with `/link <ExactMinecraftUsername>`, then one linked player
  types `kept` in `#the-record` only when the real group is ready.
- Current invite at the time of this handoff expires and must be checked/rotated independently of
  the stable `9137` callback.

The next session must re-check all of these before relying on them.

## Mandatory Interactive Phases

The new session must guide the operator through these phases in order. Each phase ends with a small
receipt: command output, screenshot, observed Minecraft behavior, hash, or explicit operator check.

### 0. Establish Safety And Scope

1. Read current git status and preserve unrelated/untracked files.
2. Identify the actual Paper server root, world name, hosting panel, live Supabase project, Vercel
   project/domain, Render services, Discord guild, and `#the-record` channel.
3. Confirm whether the requested run is an isolated rehearsal, the live setup, or rehearsal followed
   by a controlled live reset.
4. Back up worlds, plugin data, Supabase state, service environment variables (without printing
   secrets), and current Discord/showrunner state.
5. Keep the whitelist on and the Watcher asleep.

### 1. Build And Audit The Repository

1. Run the repository audit and package the launch bundle when required.
2. Read every current artifact path and SHA-1 from `observance-deploy-manifest.json`.
3. Verify the plugin jar, datapack ZIP, resource-pack ZIP, SQL bundle, and Hold invitation ZIP exist.
4. Stop if the generated artifacts do not correspond to the current source.

### 2. Make Progression State Safe

1. Inspect—not guess—the current rows and flags used by `arc_state`, `showrunner_state`, players,
   links, attempts, solves, dossiers, custom compliance, observations, queues, beats, gifts, site
   receipts, and settings.
2. Explain which records are authored configuration and which are run-specific state.
3. Remove the accidental Discord spoiler message before inviting players.
4. Create or use a reviewed reset that preserves authored puzzles/settings and deliberately handles
   player identity/link rows, while clearing the selected run's progression and stale automation
   state.
5. Prove `seventh_named`, `bowed_as_one`, `record_released`, ending/finale flags, stale telemetry, and
   reveal high-water cannot trigger at startup.
6. Start the bot while dormant and prove it posts nothing merely because it connected.

### 3. Verify Every External Service

1. Apply/verify the SQL bundle in the intended Supabase project; do not mistake idempotent schema
   application for progression reset.
2. Verify Vercel production and the custom domain.
3. Test every public player-facing route, the Hold download, a wrong ticket code, and callback 9137.
4. Verify the current Discord invite without exposing it on an unintended page.
5. Deploy/check the persistent Discord worker and showrunner schedule.
6. Verify bot guild/channel IDs, minimum permissions, `/link`, `/whisper`, and other registered
   commands. Remove Administrator unless a separately justified feature truly requires it.
7. Keep optional voice/observer capture off until Session Zero consent is complete.

### 4. Install The Minecraft Runtime

1. Stop Paper and back up all three vanilla dimensions plus `plugins/Observance`.
2. Ensure exactly one current Observance jar is installed.
3. Install the datapack into the exact `level-name` overworld's `datapacks` folder.
4. Host and configure the exact resource-pack bytes and SHA-1.
5. Configure Supabase through the service-key environment variable without putting the key in git.
6. Install/import Multiverse-Core if the Unlit is part of the run.
7. Start with whitelist on; check console; join as operator; verify datapack, plugin, database, pack,
   and queued-write status.

### 5. Place And Prove The Deep Hold

1. Choose and record the exact permanent surface mouth and positive-Z clearance envelope.
2. Stand on the exact mouth center and run the current production `placehold` command with the
   verified depth.
3. Run Hold audit, sync, and site-launch commands.
4. Walk the entire structure as a non-op Adventure-mode player.
5. Check every room and gate, not only the audit summary: mouth stairs, build limit, walls, roofs,
   floors, lighting, orientation, signs, lecterns, complete books, shelves, containers, items,
   evidence, puzzle surfaces, interactables, entrances/exits, return paths, and protection.
6. Confirm no overlap, buried clue, backwards display, residual gate wall, accidental maze, escape,
   softlock, unreachable elevation, placeholder text, literal JSON, or missing content.
7. Do not use `open all` in the clean live story.

### 6. Place Every Other Minecraft Surface

The session must derive the complete current list from code and `/obs site` output, then place and
prove each required surface. At minimum this includes:

1. Copperline relay near first arrival; five readable lecterns; sort jackets to 9137.
2. Opening report and lit marker at the real base, placed unwitnessed.
3. All required overworld non-Hold sites and NPC surfaces.
4. Nether forge at a reachable real Nether location.
5. End shrine at a reachable but isolated real End location.
6. Every container, item frame, book, NPC, dialogue branch, input surface, return route, and site
   receipt.

No site passes because it exists. It passes only after approach, focal object, interaction/answer,
and exit/aftermath have been checked by a normal player.

### 7. Build And Prove The Unlit

1. Stop the server before copying the intended source world.
2. Create/import `observance_unlit` without a duplicate world UID.
3. Place the main-world entry and the Unlit spawn, exit, lamp, cairn, coop, well, watch, warm,
   threshold, and base fixtures in deliberately readable orientations.
4. Apply border and darkness using the current verified radius.
5. Disable build mode; run Unlit audit and readiness checks.
6. Enter as a non-op, experience pressure/scare mechanics, traverse every clue, test monster rules,
   extraction, death/disconnect recovery, multiplayer behavior, and the return route.
7. Require the readiness gate to say `READY` and still perform the visual/manual walk.

### 8. Play The Offline Hold Invitation

1. Download the deployed ZIP as a player would and compare its hash.
2. Install it in a clean client saves folder and enter in Adventure mode.
3. Play all six rooms without operator knowledge or commands.
4. Verify rendered books, complete gate removal, water recovery, lamp interaction, six-hand register,
   route containment, and address reconstruction.
5. Confirm the player can derive `snoikerz.com:25569`, then find the live relay and derive 9137.
6. Confirm ticket 1842 reveals the current Discord invitation and clearly teaches the `/link` then
   `#the-record` then `kept` handoff without spoiling later story.

### 9. Rehearse The Entire ARG Spine

Use isolated rehearsal state for the destructive/full-solve pass. The new session must enumerate
the actual current puzzle graph and guide the tester through every movement, required clue, alternate
path, gate, answer surface, NPC state, web/Discord handoff, automation beat, director intervention,
Unlit sequence, Hold sequence, keeper case, side evidence, choice, failure/recovery path, Accepting,
Watcher/Seventh reveal, release, and ending.

For every puzzle record:

- what the player sees before solving;
- every required evidence source;
- intended reasoning and answer format;
- exact legal input method;
- expected response and state change;
- physical gate or content that becomes accessible;
- wrong-answer behavior, rate limits, hints, and recovery;
- multiplayer/quorum requirements;
- proof that it cannot be solved early or become permanently stuck.

Do not accept a playthrough that relies on the director explaining an intended clue.

### 10. Restore A Clean Live Beginning

1. Preserve rehearsal evidence, logs, screenshots, fixes, and discovered defects.
2. Apply and verify every fix, rebuild affected artifacts, and repeat affected tests.
3. Reset or replace rehearsal database/world state according to the approved clean-state plan.
4. Re-run dormant-state checks, including the Discord incident regression.
5. Verify all late-game flags and automation high-water marks are clean and all intended authored
   content remains.

### 11. Consent, Arm, And Launch

1. Complete the out-of-fiction Session Zero and record opt-outs.
2. Run final status, preflight, visual, dialogue, Hold, Unlit, website, Discord, and external-media
   checks.
3. Confirm optional media is either real and reachable at the correct gate or not planted.
4. Run the final launch blocker command with real placement and rehearsal evidence.
5. Produce a written PASS/FAIL receipt for every surface and a final `LAUNCH` or `DO NOT LAUNCH`
   decision.
6. Only after `LAUNCH`: wake the Watcher, add the real players to the whitelist, and allow the intended
   human `kept` ignition.

## Interaction Rules For The New Session

- Give no more than one small operational phase at a time.
- State prerequisites before commands.
- Put commands in copyable code blocks and say where each command is run.
- Explain placeholders such as `<server-root>` before using them.
- State what success looks like immediately after the command.
- Ask for the observed output before proceeding when the result affects safety or later commands.
- When a result fails, diagnose and fix it; do not continue down the checklist.
- Never expose secrets in chat, screenshots, shell output, commits, or Discord.
- Never clear live data, overwrite a world, open all gates, or ignite the live story without explicit
  confirmation at that exact step.
- Keep a running receipt of completed steps and unresolved defects so a context reset cannot lose the
  operator's place.

## Known Workspace Note

At preparation time, `dashboard/public/the-hold/the-hold/` was an untracked extracted directory.
Treat it as user-owned until inspected; do not delete, stage, or package it merely to make git clean.
