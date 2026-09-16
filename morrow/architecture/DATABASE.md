# Database Architecture

The database stores cross-surface facts, delivery state, and operator receipts. It does not replace
Minecraft's physical authority, infer that a puzzle was solved, or decide what Morrow says.

## Creative-pivot boundary

The existing 2026-08-30 schema and migration were built for the superseded M01–M12 capability model.
They remain historical rehearsal evidence and must not be promoted to production for the G01–G15
campaign. A new additive proposal and isolated validation are required after implementation design.

## Schemas

- `morrow_private`: canonical tables and privileged functions; not exposed through the Data API.
- `public`: deliberately exposed security-invoker views and narrowly scoped owner RPCs.
- `storage`: first-party found-media objects governed by bucket and campaign policy.

Do not create or modify objects in Supabase-managed schemas except through supported product
configuration. Private Broadcast notifications are hints to refetch; Postgres remains authoritative.

## Core tables

### `campaigns`

One row per production or rehearsal campaign: release, environment, status, highest gate, Morrow state,
revision, terminal shutdown time, and clean reset identity.

### `player_identities`

Links a campaign participant to Minecraft UUID and optional Discord/Supabase identities. Public pages
never expose raw platform IDs. Linking uses one-time hashed proofs generated inside the campaign.

### `gate_definitions` / `gate_receipts`

Definitions mirror G01–G15, including owner surface, prerequisites, evidence predicate, failure/reset
behavior, and accessibility equivalent. A receipt records the concrete validated input, evidence IDs,
source event, scope, and immutable result. Only the owning surface can originate a gate receipt.

### `discovery_definitions` / `discovery_receipts`

Definitions mirror D01–D24 and mark required versus optional lore. Receipts record which campaign scope
unlocked an authored discovery. They never store a player's freeform theory as progression authority.

### `morrow_state`

One group-scoped projection using:

`support_software -> observant -> personal -> defensive -> manipulative -> emotional -> desperate -> hostile -> silent`

The state changes only from the transition events named in `relationship-states.json`. `silent` is
terminal.

### `events`

Immutable cross-surface ledger: event key, source, actor, release, campaign, scope, idempotency key,
payload, payload hash, and occurrence time.

### `event_projections`

Outbox rows keyed by event and destination surface. Each row stores status, attempts, lease owner and
expiry, next retry, applied release, and bounded error detail.

### `authored_messages` / `message_deliveries`

Allowlisted Copperline, Minecraft, and Discord lines with valid Morrow states, audience rules,
prerequisites, expiry, and accessibility copy. Deliveries bind the exact authored message to a linked
campaign identity. No generative output may create a consequential message.

### `discord_fragment_flows` / `discord_fragment_sessions`

Service-only G10 state. A flow binds campaign, release, guild/channel scope, prerequisite, expected
linked participants, and ordering rule. Sessions store only authored fragment ID, expiring nonce digest,
delivery state, and safe short-code response. Group events contain no private prose.

### `clue_objects`

Definitions for allowlisted signs, books, containers, renamed items, maps, media, and Copperline pages.
Each entry includes location or route, expected hash/state, whether it is required, permitted mutations,
and deterministic restore state.

### `scare_definitions` / `scare_runs`

Named authored scares with valid state range, world allowlist, intensity profile, reduced mode, maximum
lifetime, cleanup policy, and cooldown. Runs record trigger source, operator if any, before/after state,
entities or props owned, and cleanup receipt.

### `media_assets` / `media_deliveries`

First-party asset hashes, metadata, accessible alternatives, release binding, gate prerequisites, and
delivery receipts. Design-only catalog entries cannot be delivered until exact content hashes exist.

### `director_actions`

Append-only show-control and recovery actions: authenticated operator, role, release, campaign, scope,
command type, allowlisted target, reason, idempotency key, before/after revision, result, and recovery
receipt. Finale arm and start are distinct actions with short expiry and rollback binding.

### `shutdown_receipts`

The terminal G15 record: armed command hash, prerequisite event set, Minecraft local-journal head,
player-safe checkpoint status, Copperline confirmation, stop result, and surface acknowledgements.
After commitment, the database rejects new Morrow-authored actions and messages.

## Security rules

- Enable RLS on every exposed table/view path and revoke default grants before narrow grants.
- Use `security_invoker = true` for exposed Postgres 15+ views.
- Never authorize with user-editable metadata.
- Never expose service-role or director credentials to Minecraft clients, Discord users, or bundles.
- Service-only RPCs use a fixed `search_path`, explicit grants, payload limits, and exact scope checks.
- UPDATE policies include both `USING` and `WITH CHECK`; identity ownership is explicit.
- Public Copperline projections reveal only unlocked, spoiler-safe authored material.
- Discord RPCs validate linked identity, campaign, release, scope, purpose, nonce, expiry, and
  prerequisite.
- Director RPCs accept enums and allowlisted IDs, never arbitrary SQL, commands, paths, or message text.
- Morrow personalization stores only campaign actions and authored fictional records.
- Every mutation is rate-limited, idempotent, and receipt-producing.

## Realtime

Use private Broadcast for fast Copperline, Discord, and director refreshes. Clients refetch a scoped
projection after notification. No gate, scare, shutdown, or silence state depends on ephemeral delivery.

## Migration workflow

Before any new production schema:

1. Design an additive G01–G15 migration; do not repurpose historical event meanings.
2. Generate it through the pinned Supabase CLI and bind proposal, migration, and catalog hashes.
3. Apply it to a fresh isolated validation project.
4. Test owner-only reads, role grants, payload bounds, RLS, retries, concurrency, collisions, rollback,
   director allowlists, G15 terminal rejection, and advisor results.
5. Rehearse the full projector and linked-player Discord fragment flow.
6. Take and verify a fresh production backup.
7. Request explicit production authorization for the exact migration and release.

Schema application alone cannot activate workers, director controls, or production. Every activation
has a separate fail-closed release acknowledgement.
