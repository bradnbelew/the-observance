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

The bounded V5 revision now has a fresh disposable Paper 1.21.11 validation target and a separately
prepared pristine review target. It fixes the measured client defects without simplifying the
investigation: twenty complete one-clause pages fit the conservative 114-pixel/13-line budget and expose
two equivalent click targets each; four interactive chiseled shelves have exact occupied-slot state and
matching written books; all twenty-two stair seats face classified workplace or waiting targets. Naive
and brute flows still commit zero findings, exact report plus synthesis remain required, and
closed/open/restart audits pass across 248,745 cells. Brad visual approval, human client polish,
independent interface-affordance discovery, authenticated non-op/accessibility, and optional A2 remain
open; M4 is still closed.

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
