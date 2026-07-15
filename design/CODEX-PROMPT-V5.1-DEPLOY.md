# Codex prompt — deploy V5.1 redesign changes

Status: current — one-time deploy instructions for the V5.1 redesign pass (2026-07-15). Superseded by
nothing yet; re-check against `git log` if this file is read long after that date.

Paste this to Codex (it holds the Supabase/Railway connections; Claude does not touch them directly).

---

I've just landed a redesign pass on The Observance (branch/commit: see `git log -1` after pulling).
Here's exactly what changed and what needs to ship. Please verify each claim against the repo before
acting — don't take my counts on faith.

## 1. Supabase — verify NO migration is needed (do not apply anything blind)

None of this pass touched `design/ARG-V5-PHYSICAL-PREDICATES.json`, so the live
`settings.v5_physical_authority_sha256` should still equal the current file's SHA-256. Please:

1. Recompute the SHA-256 of `design/ARG-V5-PHYSICAL-PREDICATES.json` and confirm it still matches
   `settings.v5_physical_authority_sha256` in the live project (fdnmhbpxnodrnbrzrlqq). If it matches,
   **no migration is needed** — stop here for Supabase.
2. If it does NOT match, something in this pass touched the predicates file unexpectedly — stop and
   flag it back to me before writing anything; do not paper over a mismatch with a hash update.
3. Sanity-check row counts are unchanged: `investigations` (10 active+required), `investigation_nodes`
   (82 active+required), `required_media` (5 active), `puzzles` (15 active). This pass added zero rows
   to any of these tables — it only ADDED CONTENT inside the plugin's bundled JSON files
   (`arc/v5/evidence-item-appearance.json`, `arc/v5/evidence-item-text.json`) and a new **read-only**
   Discord command querying existing columns. If any count differs from the above, stop and flag it.

## 2. Railway — redeploy the Discord bot (this IS needed)

`discord/src/` changed: a new slash command `/progress` was added (shows players the live docket of
open findings and which of three channels — world / Discord / Copperline site — each is submitted
through). Files touched: `discord/src/bot/register.ts`, `discord/src/bot/index.ts`,
`discord/src/bot/commands/progress.ts` (new), `discord/src/db/repo.ts` (two new read-only functions:
`getOpenInvestigationNodes`, `getCaseTitles`), `discord/src/voice.ts` (three new in-register lines).

No schema changes — both new repo functions only SELECT from existing `investigation_nodes` and
`investigations` columns (`input_surface`, `title`, `prerequisite_flags`, `completion_flag`), already
live. Locally verified: `npm run typecheck` clean, and `npm run v5check` / `v5bindingcheck` /
`v5surfacecheck` all pass unchanged (82 nodes / 60 plugin / 16 Discord / 6 website surfaces, exactly as
before this pass).

Please:
1. Pull the latest commit into the Railway-linked branch (or redeploy from the current HEAD if Railway
   tracks a branch already).
2. Redeploy the **worker** service (the one that was `904bdad1-c107-44c2-9ea3-296ced8f54ba` at last
   check — confirm the current service id, IDs can rotate on redeploy).
3. The bot registers guild slash commands on every boot (`registerGuildCommands()` runs in
   `bot/index.ts`'s startup path), so `/progress` should appear automatically within the deploy — no
   separate `npm run register` step needed unless you want to force it manually.
4. After deploy, confirm in Discord: `/progress` appears in the command list, and running it (as a
   linked player) returns either "nothing stands open" or a real docket — not an error. If it errors,
   check Railway logs for the failure (most likely cause: a column name mismatch if the live schema
   drifted from what's in `discord/supabase/migrations/`0013_v5_investigations.sql` — compare
   `input_surface`/`prerequisite_flags`/`completion_flag` column names against what's actually live).
5. The **cron/recovery** service does not need a redeploy for this pass (nothing in `showrunner/` or
   `customs.run.ts` changed) — confirm that assumption by diffing `discord/src/showrunner/` against
   what's already deployed; only redeploy it if you find it's also behind for unrelated reasons.

## 3. Do NOT deploy the Minecraft plugin from here

The plugin JAR (`plugin/build/libs/observance-0.5.0.jar`) is installed manually on the Minecraft server
by Brad after a final smoke test — not part of this Railway/Supabase deploy. Leave it alone.

## 4. What changed, for your own context (no action needed on these)

- The ambient "Watcher" haunting system was re-enabled in production (previously fully disabled) with a
  curated, text-free, no-write sensory palette — plugin-only, no live-service touch.
- The finale sequence was stretched from ~8 seconds to ~70 seconds with a distance-ordered light-death
  wave and a dripped goodbye message — plugin-only.
- The Unlit's seven house puzzles were rewritten so answers are derived from evidence rather than
  printed on the item — plugin + bundled JSON content only, no Supabase touch (verified above).
- A new asymmetric multiplayer moment was added at the Unlit's base-mirror site (BI08) — pure world
  dressing, no predicate/gate change, no Supabase touch.
- `design/V5.1-REDESIGN.md` is now the source-of-truth for this pass; it's registered in
  `tools/check_v5_freshness.py`'s current-docs allowlist so the freshness audit still passes.

If anything above doesn't match what you see in the repo, trust the repo and tell Brad — this prompt
describes intent, the code is the truth.
