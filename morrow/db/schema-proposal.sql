-- Morrow reboot schema proposal.
-- NOT A PRODUCTION MIGRATION. Generate a numbered migration with the installed Supabase CLI only
-- after this passes on an isolated target, advisors, rollback, RLS, and concurrency tests.

begin;

create extension if not exists pgcrypto with schema extensions;

create schema if not exists morrow_private;
revoke all on schema morrow_private from public, anon, authenticated;
grant usage on schema morrow_private to service_role;

create table if not exists morrow_private.campaigns (
  campaign_id uuid primary key default gen_random_uuid(),
  release_id text not null check (release_id ~ '^[a-z0-9][a-z0-9._-]{6,79}$'),
  environment text not null check (environment in ('local','rehearsal','production')),
  status text not null default 'preparing' check (status in ('preparing','ready','running','paused','coda','closed')),
  current_act smallint not null default 0 check (current_act between 0 and 7),
  reset_id uuid not null default gen_random_uuid(),
  revision bigint not null default 0 check (revision >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (environment, reset_id)
);

alter table morrow_private.campaigns
  add column if not exists handoff_token_sha256 text
  check (handoff_token_sha256 is null or handoff_token_sha256 ~ '^[0-9a-f]{64}$');

create table if not exists morrow_private.player_identities (
  player_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  minecraft_uuid uuid not null,
  supabase_user_id uuid references auth.users(id) on delete set null,
  discord_user_id text check (discord_user_id is null or discord_user_id ~ '^[0-9]{15,22}$'),
  display_alias text not null check (char_length(display_alias) between 1 and 32),
  capture_consent boolean not null default false,
  consent_revision bigint not null default 0 check (consent_revision >= 0),
  joined_at timestamptz not null default now(),
  unique (campaign_id, minecraft_uuid),
  unique (campaign_id, supabase_user_id),
  unique (campaign_id, discord_user_id)
);

create table if not exists morrow_private.relationship_state (
  campaign_id uuid primary key references morrow_private.campaigns(campaign_id) on delete cascade,
  state text not null default 'helpful' check (state in ('helpful','curious','intimate','possessive','afraid','negotiated')),
  revision bigint not null default 0 check (revision >= 0),
  transition_event_id uuid,
  updated_at timestamptz not null default now()
);

create table if not exists morrow_private.event_definitions (
  event_key text primary key check (event_key ~ '^morrow[.]act[0-7][.][a-z0-9_]+$'),
  act smallint not null check (act between 0 and 7),
  owner_surface text not null check (owner_surface in ('minecraft','copperline','discord','media','director')),
  prerequisite_events text[] not null default '{}',
  projection_surfaces text[] not null,
  active boolean not null default true,
  check (cardinality(projection_surfaces) > 0),
  check (projection_surfaces <@ array['minecraft','copperline','discord','media','director']::text[])
);

insert into morrow_private.event_definitions
  (event_key, act, owner_surface, prerequisite_events, projection_surfaces)
values
  ('morrow.act0.case_chain_authenticated', 0, 'copperline', '{}', '{copperline,discord}'),
  ('morrow.act0.server_handoff_recovered', 0, 'copperline', '{morrow.act0.case_chain_authenticated}', '{minecraft,copperline,discord}'),
  ('morrow.act1.room04_witnessed', 1, 'minecraft', '{morrow.act0.server_handoff_recovered}', '{minecraft,copperline}'),
  ('morrow.act1.static_proposal_authenticated', 1, 'minecraft', '{morrow.act1.room04_witnessed}', '{minecraft,copperline,discord}'),
  ('morrow.act1.intention_error_proven', 1, 'minecraft', '{morrow.act1.static_proposal_authenticated}', '{minecraft,copperline,discord}'),
  ('morrow.act1.entity_replay_authorized', 1, 'minecraft', '{morrow.act1.intention_error_proven}', '{minecraft,copperline,discord}'),
  ('morrow.act2.missing_role_completed', 2, 'minecraft', '{morrow.act1.entity_replay_authorized}', '{minecraft,copperline}'),
  ('morrow.act2.live_test_recorded', 2, 'minecraft', '{morrow.act2.missing_role_completed}', '{minecraft,copperline,media}'),
  ('morrow.act2.behavior_reuse_proven', 2, 'minecraft', '{morrow.act2.live_test_recorded}', '{minecraft,copperline,discord}'),
  ('morrow.act2.live_capture_authorized', 2, 'minecraft', '{morrow.act2.behavior_reuse_proven}', '{minecraft,copperline,discord}'),
  ('morrow.act3.version_fragments_authenticated', 3, 'minecraft', '{morrow.act2.live_capture_authorized}', '{minecraft,copperline}'),
  ('morrow.act3.contradiction_preserved', 3, 'minecraft', '{morrow.act3.version_fragments_authenticated}', '{minecraft,copperline,discord}'),
  ('morrow.act4.witness_anchor_registered', 4, 'minecraft', '{morrow.act3.contradiction_preserved}', '{minecraft,copperline}'),
  ('morrow.act4.almost_home_proven', 4, 'minecraft', '{morrow.act4.witness_anchor_registered}', '{minecraft,copperline,media}'),
  ('morrow.act4.account_continuity_authorized', 4, 'minecraft', '{morrow.act4.almost_home_proven}', '{minecraft,copperline,discord}'),
  ('morrow.act5.continued_session_observed', 5, 'minecraft', '{morrow.act4.account_continuity_authorized}', '{minecraft,copperline,discord}'),
  ('morrow.act5.returning_identity_authenticated', 5, 'minecraft', '{morrow.act5.continued_session_observed}', '{minecraft,copperline}'),
  ('morrow.act5.dual_session_consciousness_proven', 5, 'minecraft', '{morrow.act5.returning_identity_authenticated}', '{minecraft,copperline,discord}'),
  ('morrow.act6.audit_chronology_proven', 6, 'copperline', '{morrow.act5.dual_session_consciousness_proven}', '{minecraft,copperline,discord}'),
  ('morrow.act6.current_morrow_reconstruction_proven', 6, 'minecraft', '{morrow.act6.audit_chronology_proven}', '{minecraft,copperline,discord,media}'),
  ('morrow.act6.cold_storage_access_authorized', 6, 'minecraft', '{morrow.act6.current_morrow_reconstruction_proven}', '{minecraft,copperline,discord}'),
  ('morrow.act7.rollback_anchors_committed', 7, 'minecraft', '{morrow.act6.cold_storage_access_authorized}', '{minecraft,copperline}'),
  ('morrow.act7.branch_policy_committed', 7, 'minecraft', '{morrow.act7.rollback_anchors_committed}', '{minecraft,copperline,discord,media}'),
  ('morrow.act7.branch_governance_authorized', 7, 'minecraft', '{morrow.act7.branch_policy_committed}', '{minecraft,copperline,discord,media}'),
  ('morrow.act7.coda_started', 7, 'minecraft', '{morrow.act7.branch_governance_authorized}', '{minecraft,copperline,discord}')
on conflict (event_key) do update set
  act = excluded.act,
  owner_surface = excluded.owner_surface,
  prerequisite_events = excluded.prerequisite_events,
  projection_surfaces = excluded.projection_surfaces,
  active = true;

create table if not exists morrow_private.events (
  event_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  release_id text not null,
  event_key text not null references morrow_private.event_definitions(event_key) on delete restrict,
  source_surface text not null check (source_surface in ('minecraft','copperline','discord','media','director')),
  actor_player_id uuid references morrow_private.player_identities(player_id) on delete set null,
  idempotency_key text not null check (idempotency_key ~ '^[a-z0-9][a-z0-9:._/-]{7,159}$'),
  payload jsonb not null default '{}'::jsonb,
  payload_sha256 text not null check (payload_sha256 ~ '^[0-9a-f]{64}$'),
  occurred_at timestamptz not null,
  received_at timestamptz not null default now(),
  check (octet_length(payload::text) <= 16384),
  unique (campaign_id, idempotency_key)
);

alter table morrow_private.relationship_state
  drop constraint if exists relationship_state_transition_event_id_fkey;
alter table morrow_private.relationship_state
  add constraint relationship_state_transition_event_id_fkey
  foreign key (transition_event_id) references morrow_private.events(event_id) on delete restrict;

create index if not exists morrow_events_campaign_key_time
  on morrow_private.events(campaign_id, event_key, occurred_at desc);

-- Data API boundary for signed Paper events. This is SECURITY INVOKER: only service_role has
-- table privileges, and anon/authenticated cannot execute it. Ownership, release, idempotency,
-- and projection creation are checked atomically in the database.
create or replace function public.morrow_record_minecraft_event(
  p_campaign_id uuid,
  p_release_id text,
  p_event_key text,
  p_idempotency_key text,
  p_actor_minecraft_uuid uuid,
  p_payload jsonb,
  p_payload_sha256 text,
  p_occurred_at timestamptz
)
returns table(status text, created boolean, event_id uuid)
language plpgsql
security invoker
set search_path = morrow_private, public, pg_temp
as $$
declare
  v_event_id uuid;
  v_existing morrow_private.events%rowtype;
begin
  if not exists (
    select 1 from morrow_private.campaigns campaign
    where campaign.campaign_id = p_campaign_id
      and campaign.release_id = p_release_id
      and campaign.status in ('ready', 'running')
  ) then
    return query select 'blocked'::text, false, null::uuid;
    return;
  end if;

  if exists (
    select 1
    from morrow_private.event_definitions definition,
         unnest(definition.prerequisite_events) prerequisite(event_key)
    where definition.event_key = p_event_key
      and not exists (
        select 1 from morrow_private.events prior
        where prior.campaign_id = p_campaign_id
          and prior.event_key = prerequisite.event_key
      )
  ) then
    return query select 'blocked'::text, false, null::uuid;
    return;
  end if;

  if not exists (
    select 1 from morrow_private.event_definitions definition
    where definition.event_key = p_event_key
      and definition.owner_surface = 'minecraft'
      and definition.active
  ) then
    return query select 'blocked'::text, false, null::uuid;
    return;
  end if;

  insert into morrow_private.events (
    campaign_id, release_id, event_key, source_surface, actor_player_id,
    idempotency_key, payload, payload_sha256, occurred_at
  ) values (
    p_campaign_id, p_release_id, p_event_key, 'minecraft',
    (select identity.player_id from morrow_private.player_identities identity
      where identity.campaign_id = p_campaign_id
        and identity.minecraft_uuid = p_actor_minecraft_uuid),
    p_idempotency_key, p_payload, p_payload_sha256, p_occurred_at
  )
  on conflict (campaign_id, idempotency_key) do nothing
  returning morrow_private.events.event_id into v_event_id;

  if v_event_id is null then
    select * into v_existing from morrow_private.events existing
      where existing.campaign_id = p_campaign_id
        and existing.idempotency_key = p_idempotency_key;
    if v_existing.release_id = p_release_id
       and v_existing.event_key = p_event_key
       and v_existing.source_surface = 'minecraft'
       and v_existing.payload_sha256 = p_payload_sha256 then
      return query select 'duplicate'::text, false, v_existing.event_id;
    else
      return query select 'collision'::text, false, v_existing.event_id;
    end if;
    return;
  end if;

  insert into morrow_private.event_projections(event_id, surface)
    select v_event_id, unnest(definition.projection_surfaces)
    from morrow_private.event_definitions definition
    where definition.event_key = p_event_key;

  return query select 'committed'::text, true, v_event_id;
end;
$$;

revoke all on function public.morrow_record_minecraft_event(uuid,text,text,text,uuid,jsonb,text,timestamptz)
  from public, anon, authenticated;
grant execute on function public.morrow_record_minecraft_event(uuid,text,text,text,uuid,jsonb,text,timestamptz)
  to service_role;

create table if not exists morrow_private.event_projections (
  event_id uuid not null references morrow_private.events(event_id) on delete cascade,
  surface text not null check (surface in ('minecraft','copperline','discord','media','director')),
  status text not null default 'queued' check (status in ('queued','leased','applied','failed','dead_letter')),
  attempts integer not null default 0 check (attempts >= 0),
  lease_owner text,
  lease_expires_at timestamptz,
  next_attempt_at timestamptz not null default now(),
  applied_release_id text,
  applied_at timestamptz,
  last_error text,
  updated_at timestamptz not null default now(),
  primary key (event_id, surface),
  check ((status = 'leased') = (lease_owner is not null and lease_expires_at is not null))
);

create index if not exists morrow_projection_work
  on morrow_private.event_projections(surface, status, next_attempt_at)
  where status in ('queued','failed');

-- Authenticated Copperline mutation boundary. The browser chooses neither identity nor campaign:
-- the Server Action reads its RLS projection, and this function independently rechecks auth.uid(),
-- release, ownership, prerequisite order, exact authored payload, idempotency, and the private token.
create or replace function public.morrow_record_copperline_event(
  p_campaign_id uuid,
  p_player_id uuid,
  p_release_id text,
  p_event_key text,
  p_idempotency_key text,
  p_payload jsonb,
  p_payload_sha256 text,
  p_short_token text
)
returns table(status text, created boolean, event_id uuid)
language plpgsql
security definer
set search_path = morrow_private, extensions, auth, pg_temp
as $$
declare
  v_event_id uuid;
  v_existing morrow_private.events%rowtype;
  v_token_hash text;
begin
  if auth.uid() is null
     or not morrow_private.is_linked_player(p_campaign_id, p_player_id) then
    return query select 'blocked'::text, false, null::uuid;
    return;
  end if;

  select encode(digest(upper(trim(p_short_token)), 'sha256'), 'hex')
    into v_token_hash
    where p_short_token is not null and p_short_token ~ '^[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$';

  if not exists (
    select 1 from morrow_private.campaigns campaign
    where campaign.campaign_id = p_campaign_id
      and campaign.release_id = p_release_id
      and campaign.status in ('ready','running','paused')
  ) or not exists (
    select 1 from morrow_private.event_definitions definition
    where definition.event_key = p_event_key
      and definition.owner_surface = 'copperline'
      and definition.active
      and p_event_key in (
        'morrow.act0.case_chain_authenticated',
        'morrow.act0.server_handoff_recovered'
      )
  ) or exists (
    select 1
    from unnest((select definition.prerequisite_events
                 from morrow_private.event_definitions definition
                 where definition.event_key = p_event_key)) prerequisite
    where not exists (
      select 1 from morrow_private.events prior
      where prior.campaign_id = p_campaign_id and prior.event_key = prerequisite
    )
  ) then
    return query select 'blocked'::text, false, null::uuid;
    return;
  end if;

  if p_event_key = 'morrow.act0.case_chain_authenticated' and not (
    p_payload = jsonb_build_object(
      'case_id', 'CL-RCV-04',
      'attachment_sha256', 'd7e1aa41f9d4a03ea20bdf079fa71ce7613ad66a8f347ba7f926016109817ffa',
      'custody_entries', 3,
      'operation', 'verify_attachment_custody'
    ) and p_payload_sha256 = '831b4026a5e98b263699ba337d246ad5c2c1f26b3f1d00bdef42775586707096'
  ) then
    return query select 'blocked'::text, false, null::uuid;
    return;
  end if;

  if p_event_key = 'morrow.act0.server_handoff_recovered' and (
    p_payload <> jsonb_build_object(
      'case_id', 'CL-RCV-04',
      'operation', 'recover_server_handoff',
      'token_verified', true
    ) or p_payload_sha256 <> '69ef307074c7aaf7cc4fff12c5e8b7b2423c51a05dfc7f8bb11d53b059420a6e'
      or not exists (
      select 1 from morrow_private.campaigns campaign
      where campaign.campaign_id = p_campaign_id
        and campaign.handoff_token_sha256 is not null
        and campaign.handoff_token_sha256 = v_token_hash
    )
  ) then
    return query select 'blocked'::text, false, null::uuid;
    return;
  end if;

  insert into morrow_private.events (
    campaign_id, release_id, event_key, source_surface, actor_player_id,
    idempotency_key, payload, payload_sha256, occurred_at
  ) values (
    p_campaign_id, p_release_id, p_event_key, 'copperline', p_player_id,
    p_idempotency_key, p_payload, p_payload_sha256, now()
  )
  on conflict (campaign_id, idempotency_key) do nothing
  returning morrow_private.events.event_id into v_event_id;

  if v_event_id is null then
    select * into v_existing from morrow_private.events existing
      where existing.campaign_id = p_campaign_id
        and existing.idempotency_key = p_idempotency_key;
    if v_existing.event_key = p_event_key
       and v_existing.release_id = p_release_id
       and v_existing.payload_sha256 = p_payload_sha256
       and v_existing.payload = p_payload then
      return query select 'duplicate'::text, false, v_existing.event_id;
    else
      return query select 'collision'::text, false, v_existing.event_id;
    end if;
    return;
  end if;

  insert into morrow_private.event_projections(event_id, surface)
    select v_event_id, unnest(definition.projection_surfaces)
    from morrow_private.event_definitions definition
    where definition.event_key = p_event_key;

  return query select 'committed'::text, true, v_event_id;
end;
$$;

revoke all on function public.morrow_record_copperline_event(uuid,uuid,text,text,text,jsonb,text,text)
  from public, anon, authenticated;
grant execute on function public.morrow_record_copperline_event(uuid,uuid,text,text,text,jsonb,text,text)
  to authenticated;

create table if not exists morrow_private.capability_grants (
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  capability text not null check (capability in ('static_restore','entity_replay','live_capture','account_continuity','cold_storage_access','branch_governance')),
  status text not null check (status in ('locked','offered','granted','revoked')),
  decision_event_id uuid references morrow_private.events(event_id) on delete restrict,
  revision bigint not null default 0 check (revision >= 0),
  updated_at timestamptz not null default now(),
  primary key (campaign_id, capability)
);

create table if not exists morrow_private.evidence_definitions (
  evidence_key text primary key check (evidence_key ~ '^m[0-9]{2}[.][a-z0-9_]+$'),
  investigation_id text not null check (investigation_id ~ '^M[0-9]{2}$'),
  title text not null,
  custody_surface text not null check (custody_surface in ('minecraft','copperline','discord','media')),
  claim_key text not null,
  accessibility_equivalent text not null,
  prerequisite_events text[] not null default '{}',
  content_sha256 text check (content_sha256 is null or content_sha256 ~ '^[0-9a-f]{64}$'),
  active boolean not null default true
);

create table if not exists morrow_private.evidence_receipts (
  receipt_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  player_id uuid references morrow_private.player_identities(player_id) on delete set null,
  evidence_key text not null references morrow_private.evidence_definitions(evidence_key) on delete restrict,
  receipt_kind text not null check (receipt_kind in ('observed','recovered','authenticated','preserved','rejected')),
  source_event_id uuid not null references morrow_private.events(event_id) on delete restrict,
  created_at timestamptz not null default now(),
  unique (campaign_id, player_id, evidence_key, receipt_kind)
);

create table if not exists morrow_private.dialogue_prompts (
  prompt_key text primary key check (prompt_key ~ '^morrow[.][a-z0-9_.-]+$'),
  relationship_state text not null check (relationship_state in ('helpful','curious','intimate','possessive','afraid','negotiated')),
  prompt_text text not null,
  response_options jsonb not null,
  callback_keys text[] not null default '{}',
  active boolean not null default true,
  check (jsonb_typeof(response_options) = 'array')
);

create table if not exists morrow_private.dialogue_responses (
  response_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  player_id uuid not null references morrow_private.player_identities(player_id) on delete cascade,
  prompt_key text not null references morrow_private.dialogue_prompts(prompt_key) on delete restrict,
  option_key text not null,
  source_event_id uuid not null references morrow_private.events(event_id) on delete restrict,
  created_at timestamptz not null default now(),
  unique (campaign_id, player_id, prompt_key)
);

create table if not exists morrow_private.restoration_proposals (
  proposal_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  region_key text not null check (region_key ~ '^[a-z0-9][a-z0-9_.-]{2,79}$'),
  source_version text not null,
  manifest_sha256 text not null check (manifest_sha256 ~ '^[0-9a-f]{64}$'),
  confidence numeric(5,4) not null check (confidence between 0 and 1),
  provenance text not null check (provenance in ('authenticated','inferred','conflicting','unknown')),
  decision text check (decision is null or decision in ('accepted','rejected','preserved')),
  decision_event_id uuid references morrow_private.events(event_id) on delete restrict,
  created_at timestamptz not null default now(),
  unique (campaign_id, region_key, manifest_sha256)
);

create table if not exists morrow_private.witness_anchors (
  anchor_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  creator_player_id uuid references morrow_private.player_identities(player_id) on delete set null,
  region_key text not null,
  arrangement_sha256 text not null check (arrangement_sha256 ~ '^[0-9a-f]{64}$'),
  cell_count smallint not null check (cell_count between 1 and 64),
  committed_event_id uuid not null references morrow_private.events(event_id) on delete restrict,
  supersedes_anchor_id uuid references morrow_private.witness_anchors(anchor_id) on delete restrict,
  created_at timestamptz not null default now(),
  unique (campaign_id, region_key, arrangement_sha256)
);

create table if not exists morrow_private.replay_clips (
  clip_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  owner_player_id uuid references morrow_private.player_identities(player_id) on delete set null,
  consent_revision bigint not null check (consent_revision >= 0),
  purpose text not null check (purpose in ('missing_role','deliberate_test','account_continuity')),
  duration_ticks integer not null check (duration_ticks between 1 and 900),
  sample_count integer not null check (sample_count between 1 and 450),
  local_clip_sha256 text not null check (local_clip_sha256 ~ '^[0-9a-f]{64}$'),
  derivative_key text,
  expires_at timestamptz,
  sealed_event_id uuid not null references morrow_private.events(event_id) on delete restrict,
  created_at timestamptz not null default now()
);

create table if not exists morrow_private.media_assets (
  media_key text primary key check (media_key ~ '^morrow[.][a-z0-9_.-]+$'),
  release_id text not null,
  storage_path text not null,
  sha256 text not null check (sha256 ~ '^[0-9a-f]{64}$'),
  media_type text not null check (media_type in ('image','audio','video','document','voxel_diagram')),
  accessibility_path text not null,
  prerequisite_events text[] not null default '{}',
  active boolean not null default true
);

create table if not exists morrow_private.media_deliveries (
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  media_key text not null references morrow_private.media_assets(media_key) on delete restrict,
  delivery_event_id uuid not null references morrow_private.events(event_id) on delete restrict,
  delivered_at timestamptz not null default now(),
  primary key (campaign_id, media_key)
);

create table if not exists morrow_private.director_actions (
  action_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  actor text not null,
  action_key text not null,
  reason text not null,
  before_revision bigint not null,
  after_revision bigint not null,
  receipt jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  check (after_revision >= before_revision),
  check (octet_length(receipt::text) <= 16384)
);

-- Browser-readable spoiler-filtered projection. It contains only material already authorized for the
-- linked player/campaign; raw event payloads and platform IDs never enter this table.
create table if not exists public.morrow_player_projection (
  campaign_id uuid not null,
  player_id uuid not null,
  projection_key text not null,
  projection jsonb not null,
  revision bigint not null check (revision >= 0),
  updated_at timestamptz not null default now(),
  primary key (campaign_id, player_id, projection_key),
  check (octet_length(projection::text) <= 32768)
);

alter table public.morrow_player_projection enable row level security;
revoke all on public.morrow_player_projection from public, anon, authenticated;
grant select on public.morrow_player_projection to authenticated;

create or replace function morrow_private.is_linked_player(p_campaign_id uuid, p_player_id uuid)
returns boolean
language sql
stable
security definer
set search_path = morrow_private, auth, pg_temp
as $$
  select exists (
    select 1
    from morrow_private.player_identities identity
    where identity.campaign_id = p_campaign_id
      and identity.player_id = p_player_id
      and identity.supabase_user_id = auth.uid()
  );
$$;

revoke all on function morrow_private.is_linked_player(uuid, uuid) from public, anon, authenticated;
grant execute on function morrow_private.is_linked_player(uuid, uuid) to authenticated;

drop policy if exists morrow_projection_owner_select on public.morrow_player_projection;
create policy morrow_projection_owner_select
on public.morrow_player_projection
for select
to authenticated
using (morrow_private.is_linked_player(campaign_id, player_id));

-- Private tables are fail-closed through schema isolation, revoked grants, and RLS defense in depth.
do $$
declare table_name text;
begin
  foreach table_name in array array[
    'campaigns','player_identities','relationship_state','event_definitions','events',
    'event_projections','capability_grants','evidence_definitions','evidence_receipts',
    'dialogue_prompts','dialogue_responses','restoration_proposals','witness_anchors',
    'replay_clips','media_assets','media_deliveries','director_actions'
  ] loop
    execute format('alter table morrow_private.%I enable row level security', table_name);
    execute format('revoke all on morrow_private.%I from public, anon, authenticated', table_name);
    execute format('grant select, insert, update, delete on morrow_private.%I to service_role', table_name);
  end loop;
end $$;

commit;
