# 07 — Services & Deploy (Codex's domain)

> **PHASE 0 OVERRIDE — NO DEPLOYMENT.** `PHASE-0-AUTHORITY-AUDIT.md` records the read-only mapping of
> Supabase, Vercel, both Railway service families, and the Crafty-managed Minecraft runtime. Do not
> redeploy, migrate, rotate secrets, or change live state until the relevant post-conformance phase is
> approved. The eventual release must carry parity, migration, deployment, rollback, and launch receipts
> for Supabase, Vercel, the Railway persistent worker, the Railway recovery/cron service, and the
> brother-hosted Crafty Paper server.

Railway (Discord bot), Supabase (database), Google Drive (media), and Vercel (website) are **yours,
Codex** — Brad runs the plugin on the Minecraft server by hand, but every service change goes through
you. This doc is the discipline that keeps the four layers in sync so a content/puzzle change doesn't
desync production.

**Never expose the service-role key or any secret** in a commit, a log, or a prompt. Read env var
*names* from `discord/.env` if you must; never their values.

---

## 1. The four layers and who owns them

| Layer | What | Owner | Deploy |
|---|---|---|---|
| Minecraft plugin | `plugin/build/libs/observance-0.5.0.jar` | Brad/Crafty on brother-hosted Paper | after an approved clean fresh-cutover + restart audit; only live post-prologue Minecraft runtime |
| Discord persistent worker | `discord/` + `railway.worker.json` | **you (worker-family Railway project; exact live ID pending authenticated read-only discovery)** | persistent `npm start`; slash commands and fast lease-safe showrunner loop |
| Discord recovery service | `discord/` + `railway.cron.json` | **you (recovery-family Railway project; exact live ID pending authenticated read-only discovery)** | `npm run showrunner` every ten minutes; recovery, not a second authority |
| Database | Supabase project `fdnmhbpxnodrnbrzrlqq` | **you (Supabase)** | incremental migrations only; never re-apply the whole bundle |
| Website | `dashboard/` (Next.js) | **you (Vercel)** — project `the-observance-kjxn`, domain `copperlinehosting.com` | git push → Vercel builds |
| Media | video/audio payloads | **you (Google Drive)** | host + link from the DB `required_media` / dashboard |

## 2. The predicate-hash chain — the #1 way to desync production

This is the most important thing in this doc. Four things must always agree:

```
design/ARG-V5-PHYSICAL-PREDICATES.json
   └─ SHA-256 of its raw bytes
        └─ discord/supabase/seeds/v5_investigations.sql  →  settings.v5_physical_authority_sha256
             └─ (live)  settings row in Supabase project fdnmhbpxnodrnbrzrlqq
                  └─ the plugin's PhysicalPredicateAuthority contract at runtime
```

At handoff, all four are
`37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b` (verified: the committed file's
SHA-256 equals the value in the seed and the live setting). **The V5.1 pass did not change this file, so
no migration is needed for anything shipped so far.**

**When the content rebuild (doc 2) changes a node's predicate, you MUST, as one unit:**
1. Edit `design/ARG-V5-PHYSICAL-PREDICATES.json`.
2. Recompute its SHA-256 (`sha256sum design/ARG-V5-PHYSICAL-PREDICATES.json`).
3. Update `discord/supabase/seeds/v5_investigations.sql` so `v5_physical_authority_sha256` = the new
   hash.
4. Regenerate the Supabase bundle: `cd discord && npm run db:seed` (builds `apply-all.sql` from the
   seeds) then `npm run db:bundlecheck` (the self-test that verifies the bundle).
5. Apply the change to live Supabase as an **incremental migration** (a new
   `discord/supabase/migrations/00NN_*.sql` that updates the setting + any changed
   `investigations`/`investigation_nodes` rows) — **not** by re-running the entire `apply-all.sql`. The
   security_grants migration (`0016_security_grants.sql`) is already live; do not re-apply it.
6. Re-run `python tools/check_v5_physical_predicates.py` and the plugin self-tests (`cd plugin &&
   ./gradlew.bat check`) so the runtime side agrees.
7. Update `arc/v5/SOLUTION-CASEBOOK.md` (the solutions authority) to match the new puzzle.

If you land a predicate edit without steps 3–6, the plugin will refuse to start (it verifies the hash)
or the Discord/website answer surfaces will gate the wrong nodes. This is the desync to avoid.

## 3. Node/case counts are contractual

`investigations` (10 active+required), `investigation_nodes` (82 active+required), `required_media` (5),
`puzzles` (15 active Oracle). These are asserted by `check_v5_content.py`, the plugin self-tests, and
the Discord `v5check`/`v5bindingcheck`/`v5surfacecheck`. A rebuild that keeps `node_id`s and
`completion_flag`s stable (doc 2 §7) leaves these counts untouched — preferred. If you genuinely
add/remove a node, every counter moves together and it's a schema change; surface it to Brad first.

## 4. Verifying live services (read-only, safe)

- **Supabase:** the connected MCP account at review time was the *wrong* org and couldn't read this
  project. Use the service-role creds in `discord/.env` (names only, never log values) against the REST
  API, or the Supabase dashboard, to confirm counts and the hash setting. Confirmed live at handoff:
  project `ACTIVE_HEALTHY` on Postgres 17.6.1; counts 10/82/5/15 exact; hash matches; migrations through
  `0016_security_grants` applied.
- **Vercel:** the public site is healthy (`/`, `/community/archive.php`,
  `/community/2011/02/08/world-backup`, `/community/remote-room.php`, `/support/ticket.php` all 200;
  `/the-hold/the-hold.zip` correctly 404 pre-LS03). The MCP account at review time was the wrong team
  scope — verify deployment state from Brad's Vercel scope (`team_2HAUKLhWF4QVYDHEt5FbNeHu`).
- **Railway:** no CLI/token locally. Confirm the Discord bot is online, `/link` and `/answer` work, the
  showrunner heartbeat/lease runs, and slash commands are registered, via the Railway dashboard or bot
  behavior. The bot re-registers commands on every boot.

## 5. The immediate deploy already teed up

> **Superseded sequencing:** do not run this deployment during Phase 0. Retain the prompt as provenance
> and reassess it after Spine Conformance approval and an authenticated Railway mapping.

`design/CODEX-PROMPT-V5.1-DEPLOY.md` is a ready-to-run prompt for the one service change the V5.1 pass
needs: a **Railway redeploy of the Discord worker** to ship the new `/progress` command (read-only, no
schema change — it queries existing columns). It includes the verification that no Supabase migration is
required. Run that first; it's independent of the Hold rebuild.

## 6. Website realism (Brad's rule 2 applies to the site too)

The Copperline site is the strongest layer already (an era-correct defunct-host pastiche). Two web items
carry into the rebuild:
- The gated `the-hold.zip` is currently a datapack build-script with **no `region/` terrain** and
  plaintext books — it confesses the moment a Minecraft-literate friend opens it in a text editor. Bake
  the annex into **real region files** so the download is a believable world save, not a stage prop.
  (This is a dashboard/content change — yours.)
- Ship the README + SHA-1 the post promises, and give the pre-Discord phase a hint surface (the only
  campaign phase with no `/whisper` available). Details in the web/onboarding critic notes referenced by
  `design/V5.1-REDESIGN.md`.
