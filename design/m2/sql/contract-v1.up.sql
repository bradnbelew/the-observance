-- M2 REVIEWED PROPOSAL ONLY. Never apply directly to production.
-- Scaffold the executable migration with the current Supabase CLI, then test on a disposable target.
begin;

create table if not exists public.campaign_manifest_versions (
  manifest_version text primary key,
  release_id text not null unique,
  authority_sha256 text not null check (authority_sha256 ~ '^[0-9a-f]{64}$'),
  schema_version text not null,
  status text not null check (status in ('staged','active','retired','rolled_back')),
  predecessor_version text references public.campaign_manifest_versions(manifest_version),
  created_at timestamptz not null default now(),
  activated_at timestamptz,
  check ((status = 'active' and activated_at is not null) or status <> 'active')
);
create unique index if not exists one_active_campaign_manifest
  on public.campaign_manifest_versions ((status)) where status = 'active';

create table if not exists public.predicate_authority_versions (
  raw_sha256 text primary key check (raw_sha256 ~ '^[0-9a-f]{64}$'),
  semantic_sha256 text not null check (semantic_sha256 ~ '^[0-9a-f]{64}$'),
  version_id text not null unique,
  byte_length bigint not null check (byte_length > 0),
  predecessor_raw_sha256 text references public.predicate_authority_versions(raw_sha256),
  rollback_raw_sha256 text references public.predicate_authority_versions(raw_sha256),
  manifest_version text not null references public.campaign_manifest_versions(manifest_version),
  status text not null check (status in ('staged','active','retired','rolled_back')),
  created_at timestamptz not null default now(),
  activated_at timestamptz
);
create unique index if not exists one_active_predicate_authority
  on public.predicate_authority_versions ((status)) where status = 'active';

create table if not exists public.campaign_groups (
  group_id uuid primary key default gen_random_uuid(),
  stable_key text not null unique,
  manifest_version text not null references public.campaign_manifest_versions(manifest_version),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.campaign_group_members (
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  player_id uuid not null references public.players(id) on delete restrict,
  joined_at timestamptz not null default now(),
  provenance jsonb not null,
  primary key (group_id, player_id)
);

create table if not exists public.group_observation_receipts (
  receipt_id uuid primary key default gen_random_uuid(),
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  observation_id text not null,
  source_surface text not null check (source_surface in ('paper','discord','web','media','operator','legacy_import','recovery')),
  source_version text not null,
  source_receipt_key text not null,
  provenance jsonb not null,
  idempotency_key text not null unique,
  observed_at timestamptz not null,
  committed_at timestamptz not null default now(),
  unique (group_id, observation_id, source_receipt_key)
);

create table if not exists public.group_finding_receipts (
  receipt_id uuid primary key default gen_random_uuid(),
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  finding_id text not null,
  manifest_version text not null references public.campaign_manifest_versions(manifest_version),
  predicate_raw_sha256 text not null references public.predicate_authority_versions(raw_sha256),
  observation_receipt_ids uuid[] not null check (cardinality(observation_receipt_ids) > 0),
  local_sequence bigint not null check (local_sequence > 0),
  local_state_sha256 text not null check (local_state_sha256 ~ '^[0-9a-f]{64}$'),
  idempotency_key text not null unique,
  committed_at timestamptz not null,
  mirrored_at timestamptz not null default now(),
  unique (group_id, finding_id, manifest_version)
);

create table if not exists public.finding_contributors (
  finding_receipt_id uuid not null references public.group_finding_receipts(receipt_id) on delete restrict,
  player_id uuid not null references public.players(id) on delete restrict,
  contribution_kind text not null check (contribution_kind in ('observed','filed','repaired','synthesized','confirmed','accessibility')),
  contributed_at timestamptz not null,
  primary key (finding_receipt_id, player_id, contribution_kind)
);

create table if not exists public.artifact_custody (
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  artifact_id text not null,
  instance_id uuid not null,
  state text not null check (state in ('issued','installed','returned','consumed_by_release','missing_pending_proof','reissued')),
  source_finding_receipt_id uuid references public.group_finding_receipts(receipt_id) on delete restrict,
  local_sequence bigint not null check (local_sequence > 0),
  provenance jsonb not null,
  idempotency_key text not null unique,
  recorded_at timestamptz not null default now(),
  primary key (group_id, artifact_id, instance_id)
);

create table if not exists public.protected_choice_commits (
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  choice_id text not null check (choice_id in ('wren_remembrance','averyn_name_treatment')),
  choice_value text not null,
  player_confirmation_receipt text not null,
  operator_arm_receipt text,
  local_sequence bigint not null check (local_sequence > 0),
  local_state_sha256 text not null check (local_state_sha256 ~ '^[0-9a-f]{64}$'),
  idempotency_key text not null unique,
  committed_at timestamptz not null,
  primary key (group_id, choice_id),
  check ((choice_id = 'wren_remembrance' and choice_value in ('condemn','understand','free'))
      or (choice_id = 'averyn_name_treatment' and choice_value in ('publish','deliberately_unfile'))),
  check (choice_id <> 'averyn_name_treatment' or operator_arm_receipt is not null)
);

create table if not exists public.hint_requests_v2 (
  request_id uuid primary key default gen_random_uuid(),
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  finding_id text not null,
  requested_by uuid references public.players(id) on delete restrict,
  requested_tier int not null check (requested_tier between 1 and 3),
  attempts_snapshot jsonb not null,
  status text not null default 'pending' check (status in ('pending','approved','denied','expired','delivered','closed')),
  idempotency_key text not null unique,
  requested_at timestamptz not null default now(),
  closes_at timestamptz
);

create table if not exists public.hint_approvals_v2 (
  approval_id uuid primary key default gen_random_uuid(),
  request_id uuid not null unique references public.hint_requests_v2(request_id) on delete restrict,
  authored_body text not null,
  authored_body_sha256 text not null check (authored_body_sha256 ~ '^[0-9a-f]{64}$'),
  approved_tier int not null check (approved_tier between 1 and 3),
  approved_by text not null,
  approved_at timestamptz not null default now(),
  expires_at timestamptz not null,
  scope jsonb not null,
  check (expires_at > approved_at)
);

create table if not exists public.hint_deliveries_v2 (
  delivery_id uuid primary key default gen_random_uuid(),
  approval_id uuid not null unique references public.hint_approvals_v2(approval_id) on delete restrict,
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  delivered_body_sha256 text not null check (delivered_body_sha256 ~ '^[0-9a-f]{64}$'),
  idempotency_key text not null unique,
  delivered_at timestamptz not null default now(),
  check (delivered_body_sha256 <> repeat('0',64))
);

create table if not exists public.media_reveal_receipts_v2 (
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  media_key text not null,
  manifest_version text not null references public.campaign_manifest_versions(manifest_version),
  prerequisite_finding_receipt_ids uuid[] not null,
  source_or_derivative_sha256 text not null check (source_or_derivative_sha256 ~ '^[0-9a-f]{64}$'),
  accessibility_companion_id text not null,
  idempotency_key text not null unique,
  revealed_at timestamptz not null,
  primary key (group_id, media_key, manifest_version)
);

create table if not exists public.projection_outbox_v2 (
  event_id uuid primary key default gen_random_uuid(),
  group_id uuid references public.campaign_groups(group_id) on delete restrict,
  event_type text not null,
  payload jsonb not null,
  idempotency_key text not null unique,
  status text not null default 'pending' check (status in ('pending','leased','delivered','failed','superseded')),
  attempt_count int not null default 0 check (attempt_count >= 0),
  lease_owner text,
  lease_expires_at timestamptz,
  created_at timestamptz not null default now(),
  delivered_at timestamptz
);

create table if not exists public.reconciliation_receipts_v2 (
  reconciliation_id uuid primary key default gen_random_uuid(),
  group_id uuid references public.campaign_groups(group_id) on delete restrict,
  surface text not null,
  from_sequence bigint,
  through_sequence bigint,
  inserted_count int not null check (inserted_count >= 0),
  already_present_count int not null check (already_present_count >= 0),
  conflict_count int not null check (conflict_count >= 0),
  idempotency_key text not null unique,
  details jsonb not null,
  reconciled_at timestamptz not null default now()
);

create table if not exists public.legacy_import_receipts_v2 (
  group_id uuid not null references public.campaign_groups(group_id) on delete restrict,
  legacy_node_id text not null,
  legacy_completion_flag text not null,
  disposition text not null check (disposition in ('reuse','map_merge','retire')),
  target_finding_ids text[] not null,
  source_receipt jsonb not null,
  promoted boolean not null default false,
  idempotency_key text not null unique,
  imported_at timestamptz not null default now(),
  primary key (group_id, legacy_node_id)
);

create table if not exists public.release_parity_receipts_v2 (
  surface text not null,
  environment text not null,
  git_commit text not null check (git_commit ~ '^[0-9a-f]{40}$'),
  manifest_version text not null references public.campaign_manifest_versions(manifest_version),
  predicate_raw_sha256 text not null references public.predicate_authority_versions(raw_sha256),
  schema_version text not null,
  artifact_hashes jsonb not null,
  platform_receipt jsonb not null,
  observed_at timestamptz not null,
  primary key (surface, environment, git_commit)
);

-- PostgreSQL does not add indexes for referencing columns. These cover joins,
-- reconciliation scans, and every FK whose leading column is not already a PK.
create index if not exists campaign_groups_manifest_version_idx on public.campaign_groups (manifest_version);
create index if not exists campaign_group_members_player_id_idx on public.campaign_group_members (player_id);
create index if not exists group_observations_group_id_idx on public.group_observation_receipts (group_id);
create index if not exists group_findings_group_id_idx on public.group_finding_receipts (group_id);
create index if not exists group_findings_manifest_version_idx on public.group_finding_receipts (manifest_version);
create index if not exists group_findings_predicate_sha_idx on public.group_finding_receipts (predicate_raw_sha256);
create index if not exists finding_contributors_player_id_idx on public.finding_contributors (player_id);
create index if not exists artifact_custody_source_receipt_idx on public.artifact_custody (source_finding_receipt_id);
create index if not exists hint_requests_group_id_idx on public.hint_requests_v2 (group_id);
create index if not exists hint_requests_requested_by_idx on public.hint_requests_v2 (requested_by);
create index if not exists hint_deliveries_approval_id_idx on public.hint_deliveries_v2 (approval_id);
create index if not exists hint_deliveries_group_id_idx on public.hint_deliveries_v2 (group_id);
create index if not exists projection_outbox_pending_idx on public.projection_outbox_v2 (created_at)
  where status in ('pending','failed');

create or replace function public.observance_m2_reject_mutation()
returns trigger language plpgsql security invoker set search_path = public as $$
begin
  raise exception 'M2 append-only authority: % on % is forbidden', tg_op, tg_table_name;
end;
$$;

drop trigger if exists protect_m2_choice_commits on public.protected_choice_commits;
create trigger protect_m2_choice_commits before update or delete on public.protected_choice_commits
for each row execute function public.observance_m2_reject_mutation();
drop trigger if exists protect_m2_finding_receipts on public.group_finding_receipts;
create trigger protect_m2_finding_receipts before update or delete on public.group_finding_receipts
for each row execute function public.observance_m2_reject_mutation();
drop trigger if exists protect_m2_contributors on public.finding_contributors;
create trigger protect_m2_contributors before update or delete on public.finding_contributors
for each row execute function public.observance_m2_reject_mutation();

do $$
declare table_name text;
begin
  foreach table_name in array array[
    'campaign_manifest_versions','predicate_authority_versions','campaign_groups','campaign_group_members',
    'group_observation_receipts','group_finding_receipts','finding_contributors','artifact_custody',
    'protected_choice_commits','hint_requests_v2','hint_approvals_v2','hint_deliveries_v2',
    'media_reveal_receipts_v2','projection_outbox_v2','reconciliation_receipts_v2',
    'legacy_import_receipts_v2','release_parity_receipts_v2'
  ] loop
    execute format('alter table public.%I enable row level security', table_name);
    execute format('alter table public.%I force row level security', table_name);
    execute format('revoke all on table public.%I from public, anon, authenticated', table_name);
    execute format('grant select, insert, update on table public.%I to service_role', table_name);
  end loop;
end;
$$;

revoke all on function public.observance_m2_reject_mutation() from public, anon, authenticated;
grant execute on function public.observance_m2_reject_mutation() to service_role;

insert into public.campaign_manifest_versions
  (manifest_version, release_id, authority_sha256, schema_version, status, activated_at)
values ('2.0.0-m2', 'm2-isolated-contract',
  'f3fc93dd2886c83b31f4b27c2538a0b5b268da5703f35ce40c9240ab019cd410', '1.0.0', 'staged', null)
on conflict (manifest_version) do nothing;

insert into public.predicate_authority_versions
  (raw_sha256, semantic_sha256, version_id, byte_length, manifest_version, status)
values
  ('37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b',
   'd2eec35f58cf79a30f2255f429cb0d19a5c1e8b5bd7942604b3bef724272cbf6',
   'v5-live-receipt-2026-07-14', 138349, '2.0.0-m2', 'retired'),
  ('16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a',
   'd2eec35f58cf79a30f2255f429cb0d19a5c1e8b5bd7942604b3bef724272cbf6',
   'v5-git-lf-3ef5486', 136859, '2.0.0-m2', 'staged')
on conflict (raw_sha256) do nothing;

update public.predicate_authority_versions set
  predecessor_raw_sha256 = '37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b',
  rollback_raw_sha256 = '37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b'
where raw_sha256 = '16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a';

commit;
