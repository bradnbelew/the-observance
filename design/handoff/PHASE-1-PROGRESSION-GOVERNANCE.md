# Phase 1 — Progression, Catch-up, Hints, and Automation Governance

Status: **PHASE 1 COMPLETE — REQUIRES BRAD APPROVAL FOR IMPLEMENTATION**

This authority defines how a free-paced group can progress safely over weeks. It deliberately avoids
choosing a database schema, node count, command syntax, or mechanism implementation.

## Progress model

Progress belongs to the group and is recorded as durable evidence, not attendance.

| Unit | Meaning | Durability rule |
| --- | --- | --- |
| Observation | A discoverable fact, object, reading, or testimony; it may be revisited without being “solved” again. | Source remains inspectable or has an exact replay/mirror. |
| Finding | A bounded conclusion supported by one or more observations. | A group receipt stores provenance, solver(s), time, and evidence references; duplicate filing is idempotent. |
| Arc conclusion | The synthesis that changes what the group believes and opens the next ordered milestone. | Commits only after every required finding for that arc exists; never by elapsed time or operator fiat. |
| Artifact/system state | A physical key, repaired system, opened route, judgment, or release input. | Local Paper state is primary for physical truth; recovery re-derives it from durable receipts without duplicating rewards. |
| Remembrance choice | Publish/unfile and Wren judgment. | Chosen by players on a protected surface, persisted before theater, immutable after commit, and replayed in coda. |

## Any-subset progression contract

- Any one or more linked players may discover, file, repair, or synthesize currently open work.
- Attendance never becomes a prerequisite flag. The system records contributors for remembrance and
  diagnostics, not eligibility or morality.
- Most group play is **better together, possible with fewer**: split observations, complementary records,
  and simultaneous discussion accelerate work, while a lone player can gather the same information in
  sequence.
- A whole-group convergence is reserved for P12's release experience. If someone cannot attend, Brad may
  approve an explicit accessibility/contribution path that preserves the same evidence and choice; the
  story never pretends the absent person acted.
- No active-roster snapshot permanently excludes a late or returning player. Roster membership is
  re-derived from durable identity links and can be refreshed without resetting progress.
- A player joining late receives only information already unlocked by the group. Catch-up never leaks a
  future revelation or grants an unearned physical artifact.

## Durable catch-up surfaces

Every arc must project the same unlocked truth through three complementary surfaces:

1. **Minecraft field archive:** a protected, in-world dossier containing discovered source replicas,
   repaired-system readbacks, open questions, and directions to the original evidence; it is the primary
   catch-up surface because Minecraft remains primary.
2. **Discord docket:** concise receipts, open findings, submission surface, and director-approved hint
   availability; it mirrors state but does not replace investigation or reveal full solutions.
3. **Copperline/public Record:** diegetic copies of earned web/media material and the persistent coda;
   it provides durable access for players who missed the moment a link appeared.

At each arc conclusion the system produces a **session brief** with five fields: what changed, evidence
that supports it, what remains disputed, which places changed, and what search space is now open. A brief
is generated only from committed receipts and approved authored summaries—never from an LLM or chat log.

## Replay and recovery

- Original evidence remains in place wherever safe; portable critical evidence has a protected canonical
  copy or exact group archive representation.
- Media remains replayable after reveal through both its earned Minecraft route and a durable web/Discord
  index. The index cannot reveal an unreached asset.
- Completed mechanisms become inspectable readbacks or safe replays. Replaying never closes a gate,
  consumes a unique artifact, changes a choice, or issues another reward.
- Open gates, repairs, findings, and choices survive disconnect, restart, database delay, and weeks of
  inactivity. No reset is tied to a date, real-time window, missed event, or session boundary.
- When Supabase or Railway is unavailable, already-open Minecraft routes and local physical truth remain
  usable; new cross-surface commits pause safely and reconcile idempotently after recovery.
- Recovery proves absence before reissuing an artifact and preserves provenance. Inventory escrow is not
  used as a campaign-mode transition; players retain survival gear and protected regions reject bypasses.

## Hint governance

All solution-bearing hints require director approval. Automation may collect a request, calculate the
next authored tier, and present context to Brad, but it may not deliver that tier until approved.

| Tier | Purpose | Delivery requirement |
| --- | --- | --- |
| H0 — orientation | Restates the open question and submission surface without narrowing the theory. | May be embedded in the durable docket because it is navigation, not a solution hint. |
| H1 — recovery | Points back to overlooked evidence, a missed location, or a previously earned callback. | Player request → pending queue → Brad approval or bounded pre-approval for that arc/session. |
| H2 — relationship | Names the comparison, transformation family, or contradiction to examine without giving the answer. | Fresh approval after Brad sees attempts, elapsed active work, and already used hints. |
| H3 — decisive nudge | Supplies the smallest missing inference or operation needed to resume progress. | Explicit one-time approval; logged with recipient/group and reason. |
| Technical correction | Repairs a broken route, lost artifact, malformed surface, or service fault without advancing the intended deduction. | Brad may issue immediately after verifying the fault; logged separately from hints. |

Approval is for an exact authored hint body, tier, group, and open finding. Approval expires when that
finding closes. A hint cannot silently advance flags, place evidence, open a gate, or rewrite an answer.
Pre-approval is allowed only for a named H1 body during a bounded session; there is no blanket “auto hints”
mode. The pre-Discord prologue uses the same pending-approval principle through its Copperline help surface.

## Automation risk classes

| Class | Examples | Default | Required controls |
| --- | --- | --- | --- |
| A0 — observation/readback | Health checks, liveness, receipt mirroring, replay indexes, director dashboards. | Automatic | Read-only or idempotent projection; no story timing, no player targeting. |
| A1 — safe ambient | Non-personal, text-free, ephemeral sound/particle/light impressions that write no world or progression state. | Automatic within a restrained drama budget | Global/per-player cooldowns, allowlist, quiet zones, accessibility suppression, no names or inferred fears. |
| A2 — personalized | A player's name, identity, history, route, isolation, inventory, or inferred attention used to target a beat—even if client-only and ephemeral. | **Approval required** | Exact authored payload, named target, director preview, expiry, audit receipt, safe cancellation. |
| A3 — social/public | Discord posts, NPC interventions, public Record changes, or beats that alter how the group interprets a person. | **Approval required** unless it is a deterministic receipt of an explicit player action | Authored payload, prerequisite proof, idempotency, rollback or correction path. |
| A4 — world/progression changing | Blocks, gates, weather/time with gameplay effect, entities, routes, evidence placement, state flags, repairs, or recovery actions. | **Approval required** unless it is the immediate deterministic result of the players completing its declared predicate | Plan/readback, bounded mutation, persistence, restart safety, rollback/repair receipt. |
| A5 — irreversible/finale | Judgment commit, name treatment, shutdown, coda transition, destructive reset. | **Dual gate: player action plus Brad/operator arm** | Durable precommit, cancel window where valid, exact branch readback, backup/rollback plan, no automation-selected choice. |

Deterministic player-earned results are not “ambient automation”: a correct filing may open its declared
gate immediately because players explicitly caused it. Curatorial timing, targeting, or added theater is
separate and follows the table.

## Current implementation gaps carried into later phases

- `AmbientBeatGenerator` currently uses attention/isolation and automatically allows `name_on_wall`;
  this is A2 personalized behavior and must become approval-gated or be removed from automatic mode.
- Current `/whisper` delivers pre-authored tiers after coarse budget checks; rebuild delivery must insert
  the pending-director-approval gate while preserving discoverability and auditability.
- Current V5 safe mode correctly suppresses legacy world-changing showrunner producers and keeps
  `WorldDriftClock` off; preserve that fail-closed boundary.
- The persistent Railway worker and recovery cron may mirror or recover deterministic work but may not
  create story progression, select a Wren/name outcome, or schedule required content.

## Acceptance tests for implementation phases

- A player absent for two arcs can reconstruct unlocked truth and resume without an operator summary.
- A solo/subset run can complete every ordinary finding and safely defer the final convergence.
- Every replay surface is spoiler-bounded and cannot duplicate rewards or mutate committed choices.
- Every H1–H3 delivery has a Brad approval receipt; no request auto-escalates with time.
- Automatic beat audits show only A0/A1 behavior; identity- or attention-targeted events never fire
  without approval.
- A restart or external outage loses no receipt, gate, repair, artifact provenance, or remembrance choice.
