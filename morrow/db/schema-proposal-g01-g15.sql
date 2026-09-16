-- Morrow G01-G15 local schema proposal.
-- Proposal only: do not apply to production or a validation project without separate authorization.

create schema if not exists morrow_private;

create table if not exists morrow_private.gate_definitions (
  gate_id text primary key check (gate_id ~ '^G(0[1-9]|1[0-5])$'),
  event_key text not null unique check (event_key ~ '^morrow[.]gate[.]'),
  owner_surface text not null,
  act integer not null check (act between 0 and 4),
  evidence jsonb not null check (jsonb_array_length(evidence) >= 3),
  failure_behavior text not null,
  accessible_equivalent text not null
);

create table if not exists morrow_private.events (
  event_id uuid primary key default gen_random_uuid(),
  release_id text not null,
  campaign_id uuid not null,
  event_key text not null references morrow_private.gate_definitions(event_key),
  idempotency_key text not null,
  payload jsonb not null,
  payload_sha256 text not null check (payload_sha256 ~ '^[0-9a-f]{64}$'),
  previous_receipt_sha256 text check (previous_receipt_sha256 ~ '^[0-9a-f]{64}$'),
  occurred_at timestamptz not null default now(),
  unique (release_id, campaign_id, idempotency_key)
);

create table if not exists morrow_private.event_projections (
  projection_id uuid primary key default gen_random_uuid(),
  event_id uuid not null references morrow_private.events(event_id),
  destination_surface text not null,
  status text not null default 'pending' check (status in ('pending','leased','applied','failed','dead_letter')),
  attempts integer not null default 0,
  lease_owner text,
  lease_expires_at timestamptz,
  applied_at timestamptz,
  bounded_error text,
  unique (event_id, destination_surface)
);

create table if not exists morrow_private.media_assets (
  media_key text primary key,
  release_id text,
  status text not null check (status in ('to_be_authored','authored','verified','released')),
  final_sha256 text check (final_sha256 is null or final_sha256 ~ '^[0-9a-f]{64}$'),
  accessible_equivalent text not null,
  custody_note text
);

create table if not exists morrow_private.director_actions (
  action_id uuid primary key default gen_random_uuid(),
  release_id text not null,
  campaign_id uuid not null,
  command_type text not null,
  target_id text not null,
  operator_id text not null,
  reason text not null,
  idempotency_key text not null,
  before_revision text not null,
  after_revision text not null,
  result jsonb not null,
  created_at timestamptz not null default now(),
  unique (release_id, campaign_id, idempotency_key)
);

-- Terminal silence guard to implement in the eventual ingest RPC:
-- reject new Morrow-authored messages or scare events after morrow.gate.g15_shutdown_complete.
