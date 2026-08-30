# Database Architecture

The database stores cross-surface facts and receipts. It does not replace the Minecraft world's
physical authority.

## Schemas

- `morrow_private`: canonical tables and privileged functions; not exposed through the Data API.
- `public`: deliberately exposed security-invoker views and narrowly scoped RPCs only.
- `storage`: first-party media objects governed by bucket policies.

Do not create or modify objects in the Supabase-managed `realtime` schema. Current Supabase blocks
those changes. Private Broadcast authorization is implemented with allowed RLS policies and documented
helpers.

## Core tables

### `campaigns`

One row per real or rehearsal campaign: release, environment, current act, status, revision, and clean
reset identity.

### `player_identities`

Links a campaign participant to Minecraft UUID and optional Discord/Supabase identities. Public pages
never expose raw platform IDs. Linking uses one-time hashed proofs generated in Minecraft.

### `relationship_state`

One group-scoped Morrow state: `helpful`, `curious`, `intimate`, `possessive`, `afraid`, or
`negotiated`, plus revision. The state changes only after required authored events exist.

### `capability_grants`

Tracks `static_restore`, `entity_replay`, `live_capture`, `account_continuity`, and
`cold_storage_access`, and `branch_governance`. Each grant records the event and player decision that
authorized it.

### `events`

Immutable cross-surface ledger: event key, source, actor, release, campaign, idempotency key, payload,
payload hash, and occurrence time.

### `event_projections`

Outbox rows keyed by event and surface. Contains status, attempts, lease owner/expiry, next retry,
applied release, and last error.

### `discord_contradiction_flows` / `discord_contradiction_sessions` / `discord_contradiction_votes`

Private, service-role-only state for the Act 2 contradiction. The flow binds campaign, release,
Gateway guild/channel/thread scope, prerequisite event, and one-to-six expected linked participants.
Per-player sessions store an evidence-variant identifier and only a SHA-256 nonce digest; private
evidence copy never enters the event ledger. Votes are unique per linked player. Advisory and row locks
make duplicate interaction delivery and concurrent workers converge on one payload-free group receipt.
Expired leases and sessions have authored recovery paths; an altered idempotency collision halts the
flow for operator inspection.

### `evidence_definitions` / `evidence_receipts`

Definitions describe custody, modality, prerequisites, accessibility alternative, and narrative claim.
Receipts record what a player/group actually recovered or authenticated.

### `dialogue_prompts` / `dialogue_responses`

Authored Morrow questions and concrete selected responses. Later callbacks point to the original
response receipt so Morrow cannot silently rewrite what a player said.

### `restoration_proposals`

Bounded world-region proposals: claimed source version, block-manifest hash, confidence, provenance
class, and accepted/rejected/preserved status.

### `witness_anchors`

Hash and metadata for player-created bounded arrangements. Raw block details remain local until an
authored proof requires publishing them.

### `replay_clips`

Metadata only: owner, consent scope, duration, sample count, local file hash, derivative clip, and
expiry. High-frequency coordinates are not streamed to Supabase.

### `media_assets` / `media_deliveries`

First-party asset hashes, accessible alternatives, release binding, delivery prerequisites, and
receipts.

### `director_actions`

Append-only operator actions with reason, before/after revision, and recovery receipt.

## Security rules

- Enable RLS on every exposed table/view path and revoke default grants before adding narrow grants.
- Use `security_invoker = true` for exposed Postgres 15+ views.
- Never authorize with user-editable metadata; platform roles live in app metadata or private tables.
- Never expose `service_role`/secret keys to Minecraft clients, Discord users, or browser bundles.
- The Data API ingest RPC is a narrow `public` SECURITY INVOKER function with a fixed `search_path`.
  It has EXECUTE revoked from PUBLIC/anon/authenticated and is callable only with the server-side
  service role; canonical tables remain in `morrow_private`.
- UPDATE policies include both `USING` and `WITH CHECK`; identity ownership is always explicit.
- Public Copperline projections are spoiler-filtered and reveal only unlocked material.
- Discord contradiction RPCs are service-role-only, validate linked Discord/Minecraft ownership plus
  exact campaign/release/scope/purpose/nonce/prerequisite bindings, and reveal private evidence only in
  the Gateway interaction response addressed to that player.
- Every mutation is rate-limited and produces an immutable receipt.

## Realtime choice

Use private Broadcast for quick Copperline/Discord refreshes. Postgres remains the source of truth and
clients must refetch a projection after a notification. This avoids treating ephemeral messages as
durable state and follows Supabase's current recommendation to prefer Broadcast over Postgres Changes
for scalable/security-sensitive subscriptions.

## Migration workflow

Supabase CLI 2.116.0 generated
`supabase/migrations/20260830200157_morrow_reboot_foundation.sql` after the proposal passed on the
isolated `observance-validation-20260727` project. The retained receipt is
`morrow/rehearsal/database/2026-08-30-isolated-supabase.json`; the project was paused afterward.
The isolated run covered RLS, role grants, ownership, exact payloads, tokens, duplicate/collision,
concurrent duplicate, payload limits, projection creation, rollback, and both advisor classes. The
advisor's one Morrow warning is accepted and documented: the authenticated Copperline mutation is an
intentional SECURITY DEFINER RLS bridge with independent `auth.uid()` and authored-input checks.

Before production:

1. Recheck the installed CLI and the retained migration/proposal body binding.
2. Take and verify a fresh production backup.
3. Apply the numbered migration once through the approved production workflow.
4. Rerun positive, negative, rollback, RLS, idempotency, concurrency, and advisor checks.
5. Read back the deployed objects and retain migration, backup, and rollback receipts.
