# 08 — Sequencing & Definition of Done

How to do the rebuild in an order that doesn't break, and how to know each piece is actually finished.
The golden rule: **one district/case end-to-end at a time, kept green and committed, never a big-bang.**
The last version drifted precisely because layout, content, and fillings were planned separately and
half-landed.

---

## 1. The order

**Phase 0 — conformance and authority control (current; no implementation or live mutation).**
1. Produce `SPINE-CONFORMANCE.md`, obtain Brad's explicit approval, and treat any requested locked-story
   change as an isolated spine-change request.
2. Maintain `PHASE-0-AUTHORITY-AUDIT.md`: record stale 32/10/82/fixed-media/15-hour/topology assumptions
   and the read-only Supabase, Vercel, two-Railway-project, and Crafty mappings without exposing secrets.
3. Update authority routing so future agents encounter the conformance gate and single-runtime topology
   before implementation, then run documentation/static checks only.
4. Do **not** deploy services, migrate Supabase, touch the brother's server, replace media, rewrite canon
   expression/cases, or begin a Hold floorplan or geometry before the gate is approved.

**Post-approval Phase 1 — experience architecture (next).**
Build the 20–30 hour free-paced experience map (24–28 target), subset-progress/catch-up/replay model,
required-content and Brad-footage inventory, hint/automation approval model, authority replacement plan,
and coarse cross-surface topology. This phase precedes any Hold geometry.

**Phase 1 completion — 2026-07-15:** the required artifacts are authored in
`PHASE-1-EXPERIENCE-ARCHITECTURE.md`, `PHASE-1-PROGRESSION-GOVERNANCE.md`,
`PHASE-1-MEDIA-INVENTORY.md`, and `PHASE-1-TOPOLOGY-AND-MIGRATION.md`, with the decision summary and
approval boundary in `PHASE-1-APPROVAL.md`. Brad approved Phase 1 on 2026-07-15 by requesting the
Phase 2 handoff, opening M1 evidence architecture only.

The legacy sequence below is retained as implementation history and must be re-planned against those
approved Phase 1 artifacts; its phase numbers and immediate deploy instructions are not current.

**Legacy Phase 0 — unblock and deploy what's ready (superseded; do not execute now).**
1. Run `design/CODEX-PROMPT-V5.1-DEPLOY.md` — Railway redeploy of the Discord worker for `/progress`
   (read-only; no migration). Verify `/progress` works live.
2. Resolve the **KS01 blocker** (doc 5): either fix the installer's frame-set path, or (recommended)
   let the content rebuild replace Sella's frame mechanism with a non-frame verb. The current Hold
   cannot reach a clean fresh-install receipt until this is done.

**Legacy Phase 1 — the Hold rebuild, district by district (requires resequencing and approvals).**
For each district (start with one, e.g. the Keeper library that serves C02/C03):
1. **Design the floorplan in data** (`DeepHoldV4Plan.java`) — rooms/fixtures/gates/links records +
   `validate()` proving non-overlap, standing frames, connectivity (doc 1 §3A, doc 4 §3-4).
2. **Build it dense** — per-type dressing routine in `DeepHoldV4Geometry.java` (doc 1 §3B).
3. **Rebuild the content that lives there** (doc 2) — the case's investigation chain, books/signs/items
   (doc 3 prose laws), predicate JSON + hash re-sync (doc 7), casebook update. Co-design content and
   room; they're one thing.
4. **Seed any multiplayer moment** for that district (doc 6).
5. **Verify** (see §2), commit, then move to the next district.

**Phase 2 — cross-cutting polish (after the districts are real).**
- Prose/voice sweep across everything (doc 3): purge the meta-language, break the NPC couplet meter,
  quarantine the legacy corpus, fix tier-1 whisper.
- Media de-branding + the Discord/website media mirror (doc 7 §5).
- Website: bake `the-hold.zip` into real region files; ship README+SHA-1; add the pre-Discord hint
  surface (doc 7 §6).
- Wren's body moves on the reckoning (a known small consistency fix — coda text says "his boots are
  gone" while the armor stand still stands; `WrenNpc.despawn()/spawn()` exist).

**Phase 3 — launch readiness (only when 1–2 are done).**
- `tools/audit_all.ps1` fully green (the single aggregate release gate).
- A full fresh cutover on a clean server copy reaching a **complete** receipt, then **restart +
  re-audit** clean.
- The Unlit build/bind + `obs unlit audit/ready`, `obs dialogueaudit`, `obs preflight`,
  `obs finale status` all pass, post-restart.
- The 100-row `design/V5-LIVE-TEST-MATRIX.csv` walked (a real six-player + operator pass).
- Repackage the launch kit (`build/observance-v5-launch-kit.zip`: final JAR, datapack, resourcepack,
  the-hold.zip, blueprint, operator guide, apply-all.sql for archival) with matching hashes; update
  `design/V5-PRODUCTION-LAUNCH-RUNBOOK.md` and the launch-night guide.
- A final Codex deploy of any web/Discord/DB changes, hashes re-synced.

## 2. The verification suite (run the relevant subset after every change; all of it in Phase 3)

**Static (fast, run constantly):**
```
cd plugin && ./gradlew.bat clean check build --no-daemon      # all plugin self-tests + jar
python tools/check_deep_hold_layout.py
python tools/check_deep_hold_fixture_manifest.py
python tools/check_v5_physical_predicates.py
python tools/check_v5_content.py
python tools/check_v5_freshness.py
python tools/check_deep_hold_book_manuscripts.py
python tools/check_voice_register.py
python tools/audit_deep_hold_content.py
python tools/simulate_v5_scenarios.py
cd discord && npm run typecheck && npm run v5check && npm run v5bindingcheck && npm run v5surfacecheck && npm run db:bundlecheck
```
Plus the **offline reachability sim** (doc 4 §5) after any layout change.

**Aggregate release gate:**
```
powershell -NoProfile -ExecutionPolicy Bypass -File tools/audit_all.ps1
```

**Live (per district in Phase 1, full in Phase 3):** fresh disposable server copy, unique ports, install
JAR, `obs sleep on` → `placehold prepare/plan/build` → complete receipt → `obs placehold audit` (0
findings) → `obs visualaudit` → **stop, restart, re-audit** → non-op Adventure walk.

**Never weaken an audit to get a pass.** The one legitimate exception is a *sync*, not a weakening: when
you deliberately add authored content, update the exact-count manifests to match. The specific counters
that must move with content additions are: `V5EvidenceItemAppearanceAuthority.EXPECTED_ITEMS` and its
per-node `exactCounts` map, mirrored in `tools/check_v5_content.py`'s
`EXPECTED_EVIDENCE_APPEARANCE_IDS`; and any book/node count if you change those (doc 7 §3). Fixing world
cells or authority data is always preferred over touching an audit.

## 3. Definition of Done — per piece

**A district is done when:** floorplan committed + `validate()` green; a dense per-type dresser fills it
40–70% with all aisles/standing cells walkable; the offline sim is green for it; a live build reaches a
complete receipt and survives restart+re-audit; every book/sign/item its cases reference physically
exists, correctly placed and oriented (no contradictions).

**A case is done when:** its investigation chain is designed to the layered-difficulty formula (doc 2
§2) with no naked cipher and no slot-13/handle default; its books/signs/items meet the prose laws (doc
3); its predicate JSON + Supabase hash + casebook are updated and green (doc 7); an automated run + a
real read confirm it's **hard but solvable and reasonable** with an **obvious submission point**; the
fairness engineering is intact (doc 2 §6).

**The rebuild is done when:** every district and case above is done; `audit_all.ps1` is green; a full
fresh cutover + restart audit is clean; the Unlit/dialogue/finale/preflight audits pass post-restart;
the 100-row live matrix is walked; the launch kit is repackaged with matching hashes; and a real
non-op, multi-player playthrough confirms it reads as a *world the players are investigating* — not a
puzzle game, not an "ARG" telling them so.

## 4. Don't-break-the-handoff rules

- **Preserve the dirty worktree and history.** Never `git reset --hard`, `checkout` away, `clean`, or
  broadly delete. The `design/` archive is a *working, self-enforced* supersession system
  (`tools/check_v5_freshness.py`), not cruft — do not "tidy" it by deleting quarantined history.
- **Move content and its authority together.** Fiction + predicate JSON + hash + Supabase seed + casebook
  + audits are one commit. Never land half.
- **Keep `node_id`s and `completion_flag`s stable** across a mechanism rewrite so the case graph, gates,
  and Supabase rows don't move.
- **Prove reachability offline before burning a 10-minute live build**, and never trust a pre-restart
  pass.
- **The production Minecraft server stays closed** until a clean fresh-cutover + restart audit passes.
  Brad installs the plugin by hand after that; you deploy the services.
- **When in doubt about a service change, write Brad a Codex prompt** rather than guessing — that's the
  whole reason this boundary exists.
