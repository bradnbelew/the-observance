# Rebuild Phase Checkpoints

Status: **CURRENT CONTINUATION LEDGER**

This file is updated and committed at every autonomous phase boundary. A phase is not complete merely
because it appears here; its linked authorities and checks must independently prove the claim.

The canonical continuation lineage is `CANONICAL-LINEAGE.json`. It preserves the original source
commits, their linear incorporation commits, exact receipt scope, unresolved gaps, supersession, all
three failed M3 review decisions, their Paper receipts, both clean-stop receipts, and the binding
cross-phase player-facing authority. `python tools/check_continuation_lineage.py` validates that chain.
The checkpoint is the commit containing that lineage file.

Brad's `OVERNIGHT-PRIVATE-LAUNCH-AUTHORITY.*` supersedes the interim M4-closed pause only for private
automated staging. M3/M4+ implementation may advance after owning automated gates pass. V5 and unseen
builds remain visually unapproved; Brad's morning end-to-end playthrough is the final human gate.
Public/production launch, production-domain promotion, Crafty/brother mutation, production Supabase
application, ambiguous targets, and fabricated receipts remain closed.

`PLAYER-FACING-EXPERIENCE-STANDARD.md` and its JSON companion apply after the locked spine to M3 v4 and
every later M4/M5 player-facing room, artifact, interaction, subtitle, label, and copy surface. Static
inventory/diversity checks are required, but human editorial and cold-read quality cannot be replaced
by quotas.

`INVESTIGATION-DRAMATURGY-STANDARD.*` and `INVESTIGATION-NOVELTY-AUDIT.json` are the binding open-ended
creative authority. Mechanism examples are not an allowlist, enum, quota, fixed catalog, or runtime
type system. Cases begin with revelation and emotional reversal and use generic composable primitives.
`COPPERLINE-COMMUNITY-ARCHIVE-STANDARD.*` expands the old hosting/community site into a recurring lived-in
modern archive while preserving C01, the ladder, immutable provenance, accessibility, and receipt honesty.

| Phase | Status | Durable evidence | Verification | Checkpoint |
| --- | --- | --- | --- | --- |
| Phase 0 — Spine and authority control | Complete; Brad approved 2026-07-15 | `SPINE-CONFORMANCE.md`, `PHASE-0-AUTHORITY-AUDIT.md` | V5 freshness, repository integrity, current content/predicate checks | Branch `codex/phase-1-architecture`; foundation commit `6cc1361`, approval record commit `e18d502` |
| Phase 1 — Experience architecture | Complete; Brad explicitly approved 2026-07-15 and authorized autonomous continuation | `PHASE-1-APPROVAL.md` and four Phase 1 authorities | `python tools/check_phase1_architecture.py` plus existing non-live checks | Branch `codex/phase-1-architecture`; approval and continuation checkpoint `e18d502` |
| Phase 2 / M1 — Evidence architecture | Complete under Brad's standing approval | `PHASE-2-EVIDENCE-ARCHITECTURE.md`, `PHASE-2-LEGACY-NODE-DISPOSITION.md`, `PHASE-2-CONFORMANCE-AND-MEDIA-AUDIT.md` | Phase 2, Phase 1, freshness, integrity, predicate, voice, book, scenario, content-audit, and fixture checks pass; current V5 content/layout checks expose the carried `37020…` vs `16de…` predicate-receipt mismatch recorded in the audit | Branch `codex/phase-2-evidence-architecture`; evidence commit `fae8b26`; continuation checkpoint is the commit containing this ledger row |
| M2 — Technical contracts and isolated implementation | Complete under Brad's standing approval | `design/m2/M2-TECHNICAL-CONTRACTS.md`; six generated manifests; all 82 import contracts; exact predicate byte chain; schema/rollback/forward and seven-surface parity contracts; isolated Paper/Discord implementation | M2 static gate; full plugin clean/check; Discord type/approval/voice/seed/bundle; Phase 1/2, integrity, freshness, content, 60 predicates, Hold, and 1,588-scenario checks pass. Secret-dependent and external platform receipts remain open. | Branch `codex/m2-technical-contracts`; evidence commit `2aedeca9198db36b029aaa39f364e7688fbba171`; continuation checkpoint is the commit containing this ledger row |
| M3 — Private vertical slice | V5 technically proven but NOT APPROVED and cleanly stopped; vNext offline redesign authorized; M4 closed | Preserved v1–v5 decisions and stop receipts; V5 authority and affordance inventory; Paper/restart/negative/UI receipts; exact block-state, seat, shelf, render, disconnect, save, and stop evidence; canonical lineage | V5 proves pagination, occupied shelves, corrected seats, state, persistence, restart, protection, waterworks, and gate behavior. Brad rejected it experientially: explicit fact retrieval and matching clauses are not P4's high-difficulty cross-media ARG investigation, and correct conclusions may never depend on source-touch receipts. The localhost target saved and stopped cleanly after confirmed disconnect; PID 10860 and port 25588 ended. | Branch `codex/m3-disposable-paper-gate`; V5 authored source `51ad3bdeedbe80b698d366e4c8c536c4cd3778ff`; open-ended authority checkpoint `8730c832b9524aa3478873c845ed68a258b58a73`; clean V5 decision/stop checkpoint is the commit containing this row |
| M4 — Incremental campaign build and parity rehearsal | Private automated staging open under Brad's overnight authority; public/production closed; final human gate deferred | All required arcs/districts, services, content, assets, migrations, packages | Full non-live/private-staging matrix, catch-up/replay/outage/region/story/novelty tests | Advance only when owning automated gates pass; do not infer or mutate ambiguous/public/production targets |
| M5 — Production cutover and final release | Pending | GitHub/Vercel/Railway/Supabase/Crafty/media parity, final JAR/package, hashes, backups, rollback and launch receipts | Aggregate release gate plus real production readback and post-restart/coda evidence | Final checkpoint and handoff |

### Completed V5 rejection and vNext gate

Brad's V5 feedback pass is complete and NOT APPROVED. After his confirmed disconnect, the disposable
server completed save-all flush and orderly Paper shutdown; PID 10860 and port 25588 ended. Approval is
null and M4 is closed. V5's client/render/affordance work remains valid immutable technical evidence,
but it does not prove
P4's intended 1–2 hour construction-phase and route archaeology experience: spatial, material,
logistical, cross-source, emotional, and lore-relevant inference must matter more than explicit fact
retrieval followed by matching-clause selection. Campaign investigation grammars must remain diverse
across P1–P12 rather than converging on that interaction.

Brad also superseded source-possession gating cross-phase. Correct answers, reports, and syntheses must
pass with zero observation receipts, including externally known or player-shared deductions. Evidence
interactions may retain provenance, contribution, replay, catch-up, hints, changed-place, accessibility,
and director value, but they may not become answer prerequisites or touch quotas. Future negative tests
must prove the zero-observation correct path, wrong-answer failure/throttling, and intact non-gating
custody. The disconnect/stop condition is now satisfied and safe offline vNext authoring is authorized.

Brad further prohibited a brittle geometry-heavy interpretation of P4. Mandatory answers may not
depend on literal cart-path tracing, block-by-block archaeology, or long coordinate-perfect material
simulations. The next slice keeps compact, achievable Minecraft architecture while treating Minecraft
as one surface in a true cross-media ARG: website/community, remembered NPC dialogue, callbacks,
footage/video, files, images, audio, metadata, recovered documents, and social activity can combine
across time. Lore evidence must be consequential, spooky, uncanny, human, emotionally relevant, and
connected to the central mystery; administrative records may remain texture but cannot dominate.

`CROSS-MEDIA-INVESTIGATION-STANDARD.*` and `M3-VNEXT-CROSS-MEDIA-PLAN.json` separate work that can be
authored and tested offline after disconnect from real future integration gates. Offline work may define
clue graphs, fair keys, accessibility equivalents, exact predicates, robust room-scale geometry, and
deterministic test fixtures. It may not claim new footage, video, image, audio, file, NPC performance,
website/community deployment, social activity, hosted unlock, or human cross-media receipt exists.

Brad's open-ended dramaturgy correction supersedes the old closed puzzle-axis/menu framing without
restoring its stale mechanics. The durable structures retained are Iss's correct-decode/wrong-read and
cold reread, delayed callbacks, situated professional grammars, multiple in-doors, contradiction and
provenance, acknowledged true-but-not-door findings, quiet human discovery, changing meanings,
asymmetric theory, moral judgment, consequence, and aftermath. P4/C02 now requires the sustained reversal
from apparent cult ruin, to ordinary rational refuge life, to the disturbing origin of Ways/Record as
practical procedure becomes control. Capacity and route facts alone cannot earn that belief.

Copperline is approved as a major recurring human ARG surface, not a thin prologue. Its directory,
accounts, support desk, community archive, releases, and reactive history carry majority ordinary lived
texture, distinct modern voices, time layers, natural provenance, callbacks, and accessible immutable
history. C01 still earns only that mkept was real and deliberately preserved a damaged server. Offline
fixtures/specifications do not prove deployment, URLs, cache behavior, responsive/accessibility review,
live unlocks, outage recovery, player visibility, or Brad approval. Production remains untouched.

## Known external gaps carried forward

- `EXTERNAL-TARGET-DISCOVERY-RECEIPT-2026-07-17.json` supersedes the older generic identity gaps. It
  verifies the exact Supabase production baseline, Vercel team/project, and both Railway production
  families. It also preserves the narrower residue: there is no distinct Railway/Discord staging guild
  or non-production database, no existing non-production Railway environment, and no Supabase
  development branch confirmed without connector permission and cost confirmation. Production remains
  read-only and the old services are not rebuild receipts.
- The exact four receipted video byte sets and full five-file recovered packet were located at the
  previously recorded local staging paths and hash-match the committed receipts. Best-master/ownership
  confirmation, human audiovisual/accessibility review, and Brad's keep/re-edit/replace decisions remain
  open; authenticated Drive searches returned no matching video, audio, or packet files.
- Vercel project `prj_UygHA98HGW4IBVMk6AKzXVEG6ZSQ` is authenticated and preview deployment is open.
  The current READY production deployment is still old commit `ca5416e...`; it is not the rebuild.
  Preview writes now fail closed unless an exact non-production Supabase ref is explicitly verified.
  The exact-checkpoint preview/readback receipt remains open; production aliases remain untouched.
- Crafty/brother-host access remains unavailable. Public DNS resolved the declared endpoint, but TCP
  `25569` did not answer, so no live Paper/Crafty version or runtime metadata was obtained.
- Phase 2 timing and evidence fairness are architecture findings; human playtest, source AV review, and
  live client/route/restart receipts remain for their owning later phases.
- The predicate discrepancy is resolved: `37020e...` is the exact historical mixed-EOL build byte set
  and `16de...` is its LF-normalized Git authority; both canonicalize to semantic hash `d2eec3...`.
  Production still records `37020e...` and remains untouched. A later confirmed same-release cutover
  must migrate source/package/database/runtime together to `16de...` and retain rollback bytes.
- Supabase production project `fndmhbpxnodrnbrzrlqq` is now the verified read-only baseline. Its real
  advisors report seven SECURITY DEFINER public views, many RLS tables without policies, disabled
  leaked-password protection, unindexed foreign keys, and a missing `dossiers` primary key. The
  repository's older `fdnm...` spelling is not a second inferred target; both forms are mutation-blocked.
  `design/m2/sql/production-security-hardening-v2.*` contains the staged security/grant/PK/index and
  exact rollback proposal. A disposable/development application, rollback, forward replay, both
  post-change advisors, and Auth leaked-password configuration receipt remain required before any
  production DDL.
- This isolated task had no Discord/Supabase secrets. The aggregate routed run honestly stopped at the
  secret-dependent Discord resolve check after every preceding source/M3/layout/scenario/SQL audit
  passed; no credentials were fabricated or copied. Discord TypeScript and pack checks, dashboard
  lint/self-tests/build, the full plugin build, deterministic packaging/readback, publisher/backup
  guards, non-live media, and repository integrity passed separately.
- The authored v3 replacement has a preserved local-only disposable Paper 1.21.11 build with exact
  closed/open structural audits, stop/restart/re-audit, journal replay/idempotency, and
  Paper/plugin/world/package hashes. No
  authenticated Minecraft clients were available, so non-op Adventure with retained survival gear,
  event-level protected-region bypasses, two-client asymmetry, solo readback, and player-facing
  investigation UX remain external M3 gates.
- Brad explicitly rejected both v1 and v2 on 2026-07-16. Their exact findings and decisions remain
  preserved. V3 addresses the combined v2 decision and has a separately prepared pristine review
  target, but no Brad approval; M4 remains closed.
- Brad completed the v3 mechanic and confirmed the functional state/persistence/gate path, but rejected
  it: a player can touch every record and filing docket to open the gate without reading, comparing, or
  deducing. His disconnect was independently confirmed, and the disposable server then logged a clean
  save/flush and orderly stop; PID 31192 and port 25582 ended. Future acceptance requires distinct
  natural filing affordance, diegetic objective/instruction, four exact content-dependent conclusions,
  combined synthesis, natural civic records, subset/replay/accessibility safety, cold-player
  comprehension, and naive-click/brute-force negative tests.
- Brad's final v3 direction is now cross-phase authority, not a local v4 note. Rooms must be believable
  workplaces with functionally justified scale, furnishing, circulation, and negative space; lecterns
  must be fewer and purpose-specific; artifacts must use fictionally earned formats and situated human
  voices; all prose must be grounded and medium-specific rather than purple, cryptic, meta, or repeated
  docket exposition. `PLAYER-FACING-EXPERIENCE-STANDARD.*` and its routed checker prevent M4/M5 regressions
  while explicitly preserving human quality judgment over quotas.
- The authored V4 candidate now has fresh disposable Paper 1.21.11 validation and a separate pristine
  closed review target. Exact audits cover 248,745 cells, two purposeful lecterns, eight situated
  evidence surfaces, 28 classified clusters, 62 ordered water cells, 103 furnishing supports, all
  reader/route cells, and gate collision `88 → 0 → 0`. Eight blind observations and bounded exhaustive
  report submissions commit zero findings; the exact four-clause report leaves the gate closed; only
  the combined synthesis opens it; throttling, journal replay, security, and state survive restart.
  This narrows the technical gap but does not satisfy the independent cold-human, authenticated client,
  accessibility, optional two-client A2, or Brad visual-approval gates. M4 remains closed.
- Brad's completed V4 review rejected the candidate. The cold read was voluntarily aborted because Brad
  was rushing and is inconclusive; it does not show that the investigation was too hard. During the guided
  pass, some four-choice native books overflowed so the correct answer was not visible/selectable; the
  intended report could not be completed through the UI even though underlying functionality otherwise
  worked. Wall-facing stair chairs, empty-looking interactive chiseled bookshelves, and non-obvious
  right-click evidence also fail. `BRAD-V4-REVIEW-DECISION.json` and the amended cross-phase standard
  require exact supported-client render budgets, fully visible/clickable options, seating-use facing,
  unmistakable physical affordances, cold affordance, and full guided-client receipts. Later M4+ may use
  larger believable areas for deep distributed investigations, but M3 remains bounded and all subset,
  replay, accessibility, catch-up, and no-missable guarantees remain mandatory.

These gaps must be resolved before their owning implementation/release gates; they do not justify losing
or postponing safe work in earlier phases.

## 2026-07-17 — Brad P4 vNext active-review experiential rejection

- Brad approval remains null. The live P4 review process at `127.0.0.1:25593`, PID `36280`, remains
  immutable while Brad is connected; no console command, rebuild, restart, or stop is authorized.
- Brad rejects the candidate's dominant experience: enter a room, read authored text, then restate or
  extract that text into an answer input. This may be an occasional beat, but it is not a sufficient core
  loop and does not make players feel inside a living ARG investigation.
- The exact controlling distinction is: story/lore/immersion focus means players feel they are **IN the
  ARG and genuinely investigating**, not consuming lore documents and filing their summaries.
- P4 vNext therefore has no experiential approval. P5-P12 packets, projections, and simulations remain
  useful content and technical scaffolding, but they do not prove that the campaign feels like an ARG and
  must be audited for the same document-read/answer-submit monoculture.
- The fresh disposable physical audit may continue and KS01/book-mount defects may be corrected, but any
  pass is technical evidence only. It cannot close the experiential gate or establish whole-campaign or
  launch readiness.
- No superficial "add more cipher types" implementation follows from this finding. The next experiential
  implementation must wait for and incorporate the control room's deep ARG-design research authority and
  campaign experience redesign. That authority is now story-first and cross-surface.

### Rejected P4 vNext server stop

- Brad later explicitly confirmed he was disconnected. The Paper log independently shows SirNan left at
  `20:54:24` and no later join occurred.
- Only disposable PID `36280` / `127.0.0.1:25593` was targeted. Paper logged plugin disable, player/world
  saves, all-dimension saves, RegionFile completion, and worker/I/O-pool shutdown at `21:41:58`.
- The post-stop readback at `21:42:13-05:00` found no PID and no port row. The exact target, package,
  plugin, Paper, final-log hashes, and the failed-before-delivery console-input attempt are preserved in
  `P4-VNEXT-REJECTED-SERVER-STOP-RECEIPT-2026-07-17.json`. Experiential rejection and Brad approval null
  are unchanged.

## 2026-07-17 — Research-based P1–P12 ARG experience redesign authority

- `ARG-EXPERIENCE-AUTHORITY.md/.json` now distinguishes lore scavenging, bounded puzzle hunts, and a
  responsive distributed ARG, citing the IGDA ARG SIG whitepaper, McGonigal, The Beast, Why So Serious?,
  and Perplex City. ARG is the campaign-level grammar; the first two may appear only as subordinate beats.
- `campaign/arg-experience-redesign.json` rewrites exact ordered P1–P12 briefs around an inciting anomaly,
  live unknown, competing hypotheses, distributed fragments, provenance, player-initiated consequential
  actions, A0/A1 authored reactivity, asymmetric collaboration, cross-surface consequence, delayed
  reinterpretation, earned belief, and qualitative novelty. It does not define a mechanism catalog.
- `campaign/arg-state-choreography.json` binds generic local-primary events to Minecraft, Copperline,
  Discord, dashboard, media, and NPC/Watcher projections with idempotent retry, replay, catch-up,
  accessibility, privacy, and the existing A2 approval boundary. Correct actions and meanings remain valid
  with zero observation or possession receipts.
- `check_arg_experience_authority.py` rejects direct source-to-restatement, single-surface, interaction-free,
  no-world-response, conclusion-printed-verbatim, and answer-box-dominant structures. It is a structural
  rejection gate, not a creativity score or human acceptance receipt.
- P4 vNext and the prior P5–P12 packets remain reusable technical/content scaffolding. They do not regain
  experiential status by passing Paper, projection, or simulation checks. No new Brad server is authorized
  until the responsive redesign is actually implemented, passes offline critique, and retains approval null.

## 2026-07-17 — Story-first expansion and real input authority

- `STORY-EXPANSION-ARG-INTEGRATION.md/.json` binds five human layers: the current settlement, Hold
  households/workers, six distinct Keeper people, Averyn/the civic Record, and the four-person last company.
  It preserves exactly three ambiguities and the fixed revelation ladder while requiring the seventh-category
  correction, Nessa–Toma–Pell line, cross-generational drainage repair, stateful resident lives, human Keeper
  relationships, last-company history, multi-expedition Unlit, lived Copperline, and controlled cross-media.
- `campaign/story-interaction-map.json` maps plants, incidents, player actions, authored responses,
  intersections, reversals, payoffs, and coda across ordered P1–P12. `story-dependency-map.json` keeps locked
  truth, approved flesh, provisional names/compositions, and implementation prerequisites distinct.
- `campaign/campaign-grammar-audit.json` rejects document-read/answer-submit monoculture without defining a
  mechanic taxonomy. Traditional ciphers and hard puzzles remain substantial and layered; plain English
  governs player-facing prose and decoded payloads unless language is intentionally the fair puzzle.
- `campaign/functional-feasibility-matrix.json` and `platform-input-feasibility-matrix.json` name physical,
  web, Discord, media, and Unlit owners, exact primitives, state, outage, accessibility, recovery, security,
  tests, and honest gaps. Lecterns are evidence readers only. Experimental Paper 1.21.11 Dialog inputs require
  exact build/client proof and server validation with the same-predicate Brigadier fallback.
- `test_arg_experience_negative_contracts.py` mutation-tests sixteen fail-closed anti-patterns, including
  optional major threads, absent-player gates, unsafe copy, fake forms, chat parsing, unavailable APIs,
  overflow, opaque plaintext, and source-click correctness prerequisites. Passing remains offline authority,
  not implementation, experience, or launch evidence.

## 2026-07-17 — P4–P5 real-input ARG vertical slice implementation

- `design/m3/P4-P5-ARG-VERTICAL-SLICE.md/.json` turns the story-first direction into an executable,
  still-unapproved disposable candidate. A Copperline custody restoration yields a fair page-line-word
  index; a six-page Minecraft read extract yields `COPY BEFORE SOURCE`, which is a provenance relationship
  to interpret rather than the final answer.
- The examiner desk uses the exact Paper 1.21.11 experimental Dialog/TextDialogInput response API with
  server-side validation and a stable namespaced Brigadier fallback. Copperline uses a semantic HTML form
  and Server Action. P5 uses two protected physical curation controls. No lectern pretends to be a textbox.
- Correct theory acceptance is explicitly independent of observations, clicks, possession, NPC contact,
  or telemetry. An accepted account opens the physical threshold; the subsequent civic curation changes
  the room and persists through the local hash-chained journal.
- `check_arg_vertical_slice.py` and the Java evidence self-test fail closed on API drift, fake forms,
  source gating, cipher-coordinate drift, or book overflow. Fresh Paper restart/client proof and Brad's
  experiential approval remain open and may not be inferred from these static checks.

### Fresh Paper 1.21.11 automated proof

- Create-only attempt 1 exited before authority confirmation and produced no readiness receipt; attempt 2
  exposed a harness event-order defect; attempt 3 correctly rejected two P5 signs outside the classified
  player-eye band. All targets remain preserved, no failed target was reused, and no audit was weakened.
- Attempt 4 at source `e2cd69fa95d5f68d4d549f7aa3945a3bd73c76b9` passed closed, open, curated,
  restart, security, wrong-theory, zero-observation-correct, and duplicate-idempotency paths. All three
  248,745-cell audits report zero findings; gate collision is `88 -> 0`; the journal has exactly four
  receipts after theory and P5 curation.
- The exact source, Paper/plugin/authority/world/journal/log hashes and all failed-attempt provenance are in
  `P4-P5-ARG-DISPOSABLE-PAPER-RECEIPT-2026-07-17.json`. Actual client Dialog, non-op physical use,
  fresh-player experience, and Brad approval remain explicitly null.

### Pristine story-first human target preparation

- After Brad's explicit execution authorization, create-only target `p4-p5-arg-review-89fafae-01` was
  prepared from source `89fafae6723ec8d5af078bfceb3f3b07efa05efc` on Paper 1.21.11 build 132.
- The target started with all four ARG flags false, zero receipts, and no journal file. Its 248,745-cell
  physical audit and localhost/non-op Adventure security audit both passed with zero findings; the closed
  gate retained 88 collision cells. Paper then saved every dimension and stopped with port 25604 closed.
- Exact target, source, Paper, plugin, authority, world, package, preparation-log, and external-receipt hashes
  are in `P4-P5-ARG-PRISTINE-REVIEW-PREPARATION-2026-07-17.json`. Starting this target is authorized only
  for the bounded real-client review. It does not claim whole-campaign readiness, launch approval, or Brad
  approval; every human receipt remains null until the walk occurs.

### Campaign-wide answer-shape correction

- Active review established that the P4 desk's long exact prose whitelist was not fairly derivable. A player
  could reach the right interpretation in ordinary language and still fail because no evidence supplied the
  server's hidden sentence. That input shape is rejected, and the live PID 32868 target remains unchanged
  as guided technical proof only.
- Exact matching is now reserved for short values that fair evidence or a transform actually yields. Every
  interpretive theory, synthesis, or judgment across P1-P12 must instead use a bounded physical action,
  clearly separated short claims, or deterministic meaning components with natural paraphrase and word-order
  coverage. The interface states the response shape without supplying the solution; this is not handholding.
- The offline P4 revision uses separate purpose/change/anomaly fields and a pipe-delimited command fallback.
  It stores one canonical meaning event rather than a player's wording. Multiple paraphrases, partial/wrong,
  contradiction, keyword-stuffing, zero-observation, idempotency, and restart tests are mandatory before a
  replacement disposable build.
- Fresh create-only target `p4-p5-arg-structured-a76e18d-01` passed that replacement chain on Paper
  1.21.11 build 132: wrong purpose left zero state, a natural paraphrase passed with zero observations,
  an alternate paraphrase remained idempotent after restart, all physical/security audits had zero findings,
  and gate collision remained `88 -> 0`. Exact hashes are in
  `P4-P5-STRUCTURED-ANSWER-PAPER-RECEIPT-2026-07-17.json`; actual client Dialog layout and human experience
  remain open, and the old live port 25604 target was not changed.

The bounded V5 revision now has a fresh disposable Paper 1.21.11 validation target and a separately
prepared pristine review target. It fixes the measured client defects without simplifying the
investigation: twenty complete one-clause pages fit the conservative 114-pixel/13-line budget and expose
two equivalent click targets each; four interactive chiseled shelves have exact occupied-slot state and
matching written books; all twenty-two stair seats face classified workplace or waiting targets. Naive
and brute flows still commit zero findings, exact report plus synthesis remain required, and
closed/open/restart audits pass across 248,745 cells. Brad visual approval, human client polish,
independent interface-affordance discovery, authenticated non-op/accessibility, and optional A2 remain
open; M4 is still closed.

## 2026-07-17 — P5–P12 whole-Hold disposable Paper technical proof

- Eleven failed create-only targets are preserved in
  `P5-P12-DISPOSABLE-PAPER-FAILED-ATTEMPTS-2026-07-17.json`. They document real configuration, terrain,
  KS01 identity, book-mount, sandbox, stacked-reader stance, and gate-label defects. No failed target was
  reused and no physical predicate, collision check, or readiness condition was weakened.
- Source `53dfe9615b10148f0a94690c0123bdbaa9988bee` passed on fresh Paper 1.21.11. The builder installed all
  32 rooms and 76 fixtures, reconciled 305 physical authority addresses and 96 protected source items,
  found no retired written book, saved and stopped, restarted, and passed an independent physical audit.
- The deterministic world package SHA-256 is
  `677421c6d5639614a504baea530ffbfab449ca595f21cac01f93c523ced6c316`. Exact Paper, plugin, cache,
  projection, binding, log, and package hashes are recorded in
  `P5-P12-DISPOSABLE-PAPER-PASS-2026-07-17.json`.
- This closes only the whole-Hold physical-install/restart/package gap. It does not make the current P5–P12
  packets experiential proof. Brad's campaign-level ARG rejection remains binding: the cases still require
  story-first cross-surface reauthoring, responsive player-caused consequences, client/player-view review,
  whole-campaign rehearsal, and human acceptance before they can support a launch claim.

## 2026-07-17 — P4 vNext private automated candidate

- Overnight authority supersedes the interim human pause only for private automated staging. V5 remains
  NOT APPROVED historical technical proof; no unseen build is recorded as Brad-approved.
- Source checkpoint `8a51f26814914e89fe857a929266e807b2c96586` replaces V5's closed four-choice
  catalog with five concise free-text findings, exact/alias meaning predicates, authored true-but-not-door
  responses, and correct-answer acceptance with zero observation receipts. Custody, contribution,
  catch-up, replay, and accessibility records remain durable but non-gating.
- The P4 case authority begins with the C02 revelation and emotional reversal: apparent occult Mouth,
  ordinary refuge life, then practical safety language becoming institutional control under an early
  unresolved copy anomaly. The bounded Minecraft build remains robust; Copperline supplies a committed
  offline comparison/provenance fixture with sixteen ordinary/mixed entries and four direct entries.
- Three failed create-only targets are preserved: sandbox-denied Mojang runtime download; a 266-character
  book page rejected by the 238-character limit; and a stale V5 format allowlist rejected after build.
  Their successor passed on fresh Paper 1.21.11 build 132: 248,745 cells, zero closed/open/restart
  findings, gate collision `88 -> 0`, five render-safe prompts, naive/brute zero-progress negatives,
  restart-persistent throttle, zero-observation correct report/synthesis, optional later custody, replay,
  and a second restart.
- A separate pristine closed review target is packaged for `127.0.0.1:25591`; its journal is absent.
  Client polish, human 1–2 hour solve, authenticated external targets, source media bytes, and Brad's
  morning acceptance remain open. Production, Crafty, public domains, and production Supabase remain
  untouched.

## 2026-07-18 — bounded player-caused Unlit copy proof

- Source `1c84e40a2adf774d381f37b24421fb7972338bf3` implements the required safe
  surface-to-Unlit copy as a real Paper 1.21.11 physical action, not an answer field. Players set six
  protected civic-role selectors (`water`, `heat`, `watch`, `record`) and send one pattern. The keyboard
  fallback `/obscopy` uses the same predicate. The local hash-chain journal stores only the six allowlisted
  ids and their hash—never free text, identity, inventory, arbitrary blocks, chat, logs, names, or fears.
- The returned Unlit register mirrors both three-cell rows and changes exactly the first copied `record`
  mark to `watch`. This is a fixed authored institutional edit: it proves copy-and-alter behavior without
  resolving what the Dark is and without claiming private surveillance.
- Create-only attempt 1 is preserved in
  `UNLIT-COPY-PROOF-DISPOSABLE-PAPER-FAILED-ATTEMPT-2026-07-18.json`. Its restricted Java process could
  not reopen the copied Mojang cache and exited before Paper/plugin/world readiness; it produced no state.
- Attempt 2 passed fresh install, blank-register audit, bounded commit, deterministic physical projection,
  save/stop, restart, independent audit, closed port, and deterministic two-world packaging. Exact source,
  Paper/plugin/cache/journal/layout/log/package hashes are in
  `UNLIT-COPY-PROOF-DISPOSABLE-PAPER-PASS-2026-07-18.json`.
- This closes the automated bounded-copy capability gap only. It does not prove the full seven-house Unlit
  multi-expedition, actual-client affordance or visual composition, player dread/recognition, Brad approval,
  external staging, production, or launch readiness.

## 2026-07-18 — integrated seven-house Unlit physical candidate

- Source `ec456d3110e089218c8aeb4ec601ac6bb57a8f8f` replaces the hand-placement-only gap with a
  deterministic, create-only candidate builder. It authors a bounded copied-village island, well arrival,
  seven distinct workplace shells, a base mirror, eight structural palettes, public circulation, 11 exact
  sites, and the existing seven house mechanics without making block tracing a mandatory deduction.
- The bounded surface-to-Unlit pattern return is installed in the same Unlit target. Minecraft commits its
  hash-only `p10.player_copy_proof` event; Copperline's Wren chronology and Discord shared status now project
  that player-caused response without receiving player text, identity, inventory, or arbitrary builds.
- Four create-only failures are preserved in
  `UNLIT-CANDIDATE-DISPOSABLE-PAPER-FAILED-ATTEMPTS-2026-07-18.json`: the first found a Threshold and border
  mismatch; the second confirmed the remaining Threshold problem; the third isolated the low lintel inside
  required crouched headroom; the fourth was a clean bind refusal on an unrelated occupied port. Every
  disposable Java process stopped and no target was reused or deleted.
- Attempt 5 on Paper 1.21.11 build 132 passed 37,991 authored block writes, 951 path cells, all eight
  houses/base, 34 exact mechanics addresses, border/light readiness, bounded copy commit, save/stop, restart,
  independent audit, occupied-rebuild refusal, closed port, and deterministic two-world packaging. Exact
  hashes are in `UNLIT-CANDIDATE-DISPOSABLE-PAPER-PASS-2026-07-18.json`.
- This is physical and restart evidence, not an experiential approval. A real client group must still prove
  multi-expedition motivation, navigation, figure pressure, inventory/death/quit extraction, cross-case
  reinterpretation, visual polish, dread, and the complete story-first ARG rhythm. Brad approval remains null.

## 2026-07-18 — routed source/package audit after Unlit integration

- Clean source `cc6cc9be98e3dc76f6886f3a8fa89f18ab296251` passed the complete routed audit after the
  seven-house/base Unlit candidate and cross-surface player-copy response were integrated.
- The audit proves the repository/canon contracts, ARG experience and negative-input contracts, 1,588
  deterministic campaign scenarios, 2,300 chaos scenarios, web and Discord builds, the exact Paper plugin
  suite, and deterministic JAR/datapack/resource-pack/SQL/deploy-manifest parity. The exact 90,238-byte log
  and every current package hash are recorded in `ROUTED-AUDIT-PASS-CC6CC9B-2026-07-18.json`.
- This is automated source/package evidence. It does not replace the still-required single combined
  Hold-plus-Unlit Paper target, actual-client and human ARG experience review, private staging, Brad approval,
  production authorization, or public launch authorization.

## 2026-07-18 — combined Hold + Unlit Paper proof

- Source `924a5495a6deee0700ae54f7ef54437030ef5375` passed one fresh, create-only Paper
  1.21.11 build 132 target containing the full 32-room/76-fixture Hold, all eight gates, the seven-house/base
  Unlit candidate, and the bounded player-caused surface-to-Unlit copy consequence.
- Five predecessor targets remain preserved as failed evidence. They exposed unloaded control bindings,
  missing exact-world flush, vanilla attachment loss, and two additional unsupported generated handles.
  Checks were strengthened; no target was reused, deleted, or converted into a pass.
- Attempt 6 passed 328 exact runtime addresses, 96 protected source items, both post-copy audits, graceful
  save/stop, restart, independent Hold/Unlit/copy audits, occupied-world rebuild refusal, closed port, and a
  deterministic two-world package. Exact hashes are in
  `COMBINED-CAMPAIGN-DISPOSABLE-PAPER-PASS-2026-07-18.json`.
- This closes the combined automated physical/restart/package gate only. It does not supersede Brad's
  experiential rejection, prove actual-client affordance or campaign quality, create approval, establish
  private external staging, authorize production, or establish launch readiness.

## 2026-07-18 — routed audit after combined physical proof

- Clean receipt checkpoint `caf2cd47b8dedaa54d30b8a9b2aa64351bf40a68` passed the complete routed
  audit after the combined Hold-plus-Unlit proof was entered into canonical lineage.
- The 91,914-byte log proves the ARG authority/negative contracts, 1,588 deterministic scenarios, 2,300
  chaos scenarios, web and Discord builds, 367-address Paper catalog, security checks, media routes, and
  deterministic plugin/datapack/resource-pack/deploy-manifest parity. Exact hashes are in
  `ROUTED-AUDIT-PASS-CAF2CD4-2026-07-18.json`.
- Automated source, package, and physical gates now agree at this checkpoint. Human experience, actual-client
  visual/use testing, private external deployment, Brad approval, production, and launch remain separate gates.

## 2026-07-18 — current private staging preflight

- The launch inventory is reconciled to the combined Paper and routed-audit receipts. Remaining Minecraft
  work is actual-client experience, not another unreceipted claim about placement.
- The exact current Vercel preview export is 154 dashboard files / 5,438,242 bytes with aggregate manifest
  `52a87fa12453b53943a01277d1153ad6c926ede321f80da4300329094502a1ef`. Export to the verified
  preview project still requires Brad's explicit informed approval; production promotion remains forbidden.
- Railway still has only the two known production environments and no distinct test guild/database identity.
  Supabase production remains read-only and a disposable/development target still requires permission/cost
  confirmation. Custody-approved media still requires human master, ownership, derivative, and hosting choices.
- The current dashboard starts locally on `127.0.0.1:3048`; automated browser reload was blocked by the
  in-app browser's localhost URL policy, so no visual receipt was fabricated. Exact residue is recorded in
  `PRIVATE-STAGING-PREFLIGHT-940F102-2026-07-18.json`.

## 2026-07-18 — crash-safe cross-surface event outbox

- Source `dc9447910ad49c07a912a6a09115e8c8d17e83fc` turns the generic P1–P12 event queue into
  a real local/deployable delivery runtime. The file ledger and Supabase proposal use exclusive expiring
  leases, exact lease-token acknowledgement, bounded exponential retry, an eight-attempt ceiling, atomic
  local commit-before-projection, and restart recovery.
- The private Discord worker owns 28 exact authored event consequences. It never grades answers, reads
  observation receipts, or echoes submitted payloads. Unknown events fail closed. A stable 25-character
  Discord nonce with `enforce_nonce` protects the post-before-ack crash window, while the canonical event
  remains locally committed during an outage.
- Dashboard self-tests/lint/production build, Discord typecheck/full audit, the worker's totality and payload
  isolation proof, and the enforced 36-file SQL bundle pass. Exact file hashes and command receipts are in
  `CROSS-SURFACE-EVENT-OUTBOX-RECEIPT-2026-07-18.json`.
- This is local runtime and deployable-package evidence only. No Supabase, Railway, Discord, production, or
  public target was mutated. An isolated non-production database, guild/channel, and Railway environment
  are still required for an external delivery/restart receipt; campaign experience and Brad approval remain
  separate human gates.

## 2026-07-18 — routed audit after event-outbox integration

- The first complete audit at source `7c228cd7e04924030beaf2cedcaf434bcc6a2e8c` failed on the exact
  Paper authority assertion that still allowed only `offline_authored_not_deployed`. The failure log is
  preserved; the assertion was not skipped or weakened.
- Source `b9fbd4248c7db20f9ddd0a71ba98b807003e44c4` strengthens that check: an implemented
  choreography status is accepted only with local commit-before-projection, a bounded eight-attempt runtime,
  a proof receipt, and `external_staging_receipt=null`. Discord command handlers also no longer post their own
  consequences, so the durable outbox is the sole delivery owner.
- The rerun passes the full routed repository, canon/ARG authority, deterministic/chaos simulation,
  dashboard, Discord, Paper, package, hosted resource-pack, and read-only live-media chain. Exact attempt
  logs and hashes are in `ROUTED-AUDIT-PASS-B9FBD42-2026-07-18.json`.
- This is automated source/package evidence. It does not prove the human ARG experience, private external
  delivery, Brad approval, production, or public launch readiness.

## 2026-07-18 — physical evidence carriers and any-subset release correction

- Source `6aa363e` binds every Minecraft/NPC-owned P5–P12 evidence record to at least one exact authored
  book, occupied fixture, NPC, or runtime carrier. Paper startup and the routed static checker both fail
  closed on JSON-only local evidence. The gate caught and corrected an omitted Sella lectern, omitted bird
  coops, and P8 repair actions mislabeled as evidence surfaces.
- Source `ab483c3` corrects the final release without rewriting historical evidence. The M2 physical
  predicate bytes remain exactly SHA-256
  `16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a`; the packaged
  `ARG-P12-ANY-SUBSET-OVERLAY.json` is the current RP03/RP04 behavior authority.
- RP03 now accepts one linked actor's protected publish/unfile action with zero consequence-book or source
  receipts. RP04 now accepts one linked participant's untimed, branch-specific Bridge action and sector
  confirmation. Nearby, online, disconnected, and absent players are not enrolled and cannot block either
  action. The existing ballot field names remain storage compatibility only.
- Focused M2, physical-authority, ARG-authority, Discord SQL/binding, and Paper tests pass, including a
  nearby-player negative test, no-attendance-timer test, restart/idempotence, and all 24 ending/conduct
  combinations. This is automated correctness evidence, not a human experience approval or launch receipt.
