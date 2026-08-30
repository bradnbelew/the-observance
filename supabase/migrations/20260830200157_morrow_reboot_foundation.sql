-- Morrow reboot foundation migration.
-- Generated with Supabase CLI 2.116.0 after isolated target, RLS, concurrency, payload-bound,
-- rollback-shape, advisor, and Copperline projector rehearsal. Production still requires a fresh backup.

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
  ('morrow.act2.private_contradiction_resolved', 2, 'discord', '{morrow.act2.behavior_reuse_proven}', '{minecraft,discord}'),
  ('morrow.act2.live_capture_authorized', 2, 'minecraft', '{morrow.act2.private_contradiction_resolved}', '{minecraft,copperline,discord}'),
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

-- P0 item 9: asynchronous Discord contradiction state. Private evidence text never enters these
-- tables or an event payload; only the recipient-bound evidence variant and final group classification
-- are durable. One flow is activated from the earned behavior-reuse Discord projection.
create table if not exists morrow_private.discord_contradiction_flows (
  campaign_id uuid not null references morrow_private.campaigns(campaign_id) on delete cascade,
  release_id text not null,
  source_event_id uuid not null unique references morrow_private.events(event_id) on delete restrict,
  guild_id text not null check (guild_id ~ '^[0-9]{15,22}$'),
  channel_id text not null check (channel_id ~ '^[0-9]{15,22}$'),
  thread_id text check (thread_id is null or thread_id ~ '^[0-9]{15,22}$'),
  linked_player_count smallint not null check (linked_player_count between 1 and 6),
  status text not null default 'open' check (status in ('open','resolved','halted')),
  resolved_event_id uuid references morrow_private.events(event_id) on delete restrict,
  last_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (campaign_id, release_id),
  check ((status = 'resolved') = (resolved_event_id is not null))
);

create table if not exists morrow_private.discord_contradiction_sessions (
  session_id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null,
  release_id text not null,
  source_event_id uuid not null references morrow_private.events(event_id) on delete restrict,
  player_id uuid not null references morrow_private.player_identities(player_id) on delete cascade,
  nonce_sha256 text not null unique check (nonce_sha256 ~ '^[0-9a-f]{64}$'),
  purpose text not null check (purpose = 'classify_behavior_reuse_provenance_v1'),
  evidence_variant text not null check (evidence_variant in ('route_digest','source_gap')),
  state text not null default 'offered'
    check (state in ('offered','acknowledged','decided','declined','cancelled','expired')),
  expires_at timestamptz not null,
  revision bigint not null default 0 check (revision >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  foreign key (campaign_id, release_id)
    references morrow_private.discord_contradiction_flows(campaign_id, release_id) on delete cascade
);

create index if not exists morrow_discord_session_player
  on morrow_private.discord_contradiction_sessions(campaign_id, release_id, player_id, created_at desc);

create table if not exists morrow_private.discord_contradiction_votes (
  campaign_id uuid not null,
  release_id text not null,
  source_event_id uuid not null references morrow_private.events(event_id) on delete restrict,
  player_id uuid not null references morrow_private.player_identities(player_id) on delete cascade,
  session_id uuid not null references morrow_private.discord_contradiction_sessions(session_id) on delete restrict,
  decision text not null check (decision = 'file_live_behavior_as_inferred_source'),
  decided_at timestamptz not null default now(),
  primary key (campaign_id, release_id, player_id),
  foreign key (campaign_id, release_id)
    references morrow_private.discord_contradiction_flows(campaign_id, release_id) on delete cascade
);

create or replace function public.morrow_activate_discord_contradiction(
  p_event_id uuid,
  p_release_id text,
  p_guild_id text,
  p_channel_id text,
  p_thread_id text
)
returns text
language plpgsql
security invoker
set search_path = morrow_private, public, pg_temp
as $$
declare
  v_event morrow_private.events%rowtype;
  v_existing morrow_private.discord_contradiction_flows%rowtype;
  v_group_size integer;
  v_linked_size integer;
begin
  if p_guild_id !~ '^[0-9]{15,22}$' or p_channel_id !~ '^[0-9]{15,22}$'
     or (p_thread_id is not null and p_thread_id !~ '^[0-9]{15,22}$') then
    return 'blocked';
  end if;
  select * into v_event from morrow_private.events event
    where event.event_id = p_event_id
      and event.release_id = p_release_id
      and event.event_key = 'morrow.act2.behavior_reuse_proven';
  if not found then return 'blocked'; end if;

  select count(*), count(identity.discord_user_id)
    into v_group_size, v_linked_size
    from morrow_private.player_identities identity
    where identity.campaign_id = v_event.campaign_id;
  if v_group_size < 1 or v_group_size > 6 or v_linked_size <> v_group_size then
    return 'blocked';
  end if;

  insert into morrow_private.discord_contradiction_flows(
    campaign_id, release_id, source_event_id, guild_id, channel_id, thread_id, linked_player_count
  ) values (
    v_event.campaign_id, p_release_id, p_event_id, p_guild_id, p_channel_id, p_thread_id, v_group_size
  ) on conflict (campaign_id, release_id) do nothing;
  if found then return 'activated'; end if;

  select * into v_existing from morrow_private.discord_contradiction_flows flow
    where flow.campaign_id = v_event.campaign_id and flow.release_id = p_release_id;
  if v_existing.source_event_id = p_event_id
     and v_existing.guild_id = p_guild_id
     and v_existing.channel_id = p_channel_id
     and v_existing.thread_id is not distinct from p_thread_id
     and v_existing.linked_player_count = v_group_size then
    return 'duplicate';
  end if;
  return 'collision';
end;
$$;

revoke all on function public.morrow_activate_discord_contradiction(uuid,text,text,text,text)
  from public, anon, authenticated;
grant execute on function public.morrow_activate_discord_contradiction(uuid,text,text,text,text)
  to service_role;

create or replace function public.morrow_open_discord_contradiction(
  p_discord_user_id text,
  p_release_id text,
  p_guild_id text,
  p_channel_id text,
  p_thread_id text,
  p_nonce_sha256 text,
  p_expires_at timestamptz,
  p_purpose text
)
returns table(status text, display_alias text, group_size integer, evidence_variant text, expires_at timestamptz)
language plpgsql
security invoker
set search_path = morrow_private, extensions, public, pg_temp
as $$
declare
  v_matches integer;
  v_flow morrow_private.discord_contradiction_flows%rowtype;
  v_player morrow_private.player_identities%rowtype;
  v_prior boolean;
  v_variant text;
begin
  if p_discord_user_id !~ '^[0-9]{15,22}$' or p_guild_id !~ '^[0-9]{15,22}$'
     or p_channel_id !~ '^[0-9]{15,22}$'
     or (p_thread_id is not null and p_thread_id !~ '^[0-9]{15,22}$')
     or p_nonce_sha256 !~ '^[0-9a-f]{64}$'
     or p_purpose <> 'classify_behavior_reuse_provenance_v1'
     or p_expires_at <= now() or p_expires_at > now() + interval '15 minutes' then
    return query select 'blocked'::text, null::text, 0, null::text, null::timestamptz;
    return;
  end if;

  select count(*) into v_matches
    from morrow_private.discord_contradiction_flows flow
    join morrow_private.campaigns campaign on campaign.campaign_id = flow.campaign_id
    join morrow_private.player_identities identity on identity.campaign_id = flow.campaign_id
    where flow.release_id = p_release_id and flow.status = 'open'
      and campaign.release_id = p_release_id and campaign.status in ('ready','running','paused')
      and flow.guild_id = p_guild_id and flow.channel_id = p_channel_id
      and flow.thread_id is not distinct from p_thread_id
      and identity.discord_user_id = p_discord_user_id;
  if v_matches <> 1 then
    return query select 'blocked'::text, null::text, 0, null::text, null::timestamptz;
    return;
  end if;

  select flow.* into v_flow
    from morrow_private.discord_contradiction_flows flow
    where flow.release_id = p_release_id and flow.status = 'open'
      and flow.guild_id = p_guild_id and flow.channel_id = p_channel_id
      and flow.thread_id is not distinct from p_thread_id
      and exists (
        select 1 from morrow_private.player_identities identity
        where identity.campaign_id = flow.campaign_id
          and identity.discord_user_id = p_discord_user_id
      );

  select identity.* into v_player
    from morrow_private.player_identities identity
    where identity.campaign_id = v_flow.campaign_id
      and identity.discord_user_id = p_discord_user_id;

  perform pg_advisory_xact_lock(hashtextextended(
    v_flow.campaign_id::text || ':' || v_player.player_id::text, 1));

  if exists (
    select 1 from morrow_private.discord_contradiction_votes vote
    where vote.campaign_id = v_flow.campaign_id and vote.release_id = v_flow.release_id
      and vote.player_id = v_player.player_id
  ) then
    return query select 'complete'::text, null::text, v_flow.linked_player_count::integer,
      null::text, null::timestamptz;
    return;
  end if;

  select exists (
    select 1 from morrow_private.discord_contradiction_sessions session
    where session.campaign_id = v_flow.campaign_id and session.release_id = v_flow.release_id
      and session.player_id = v_player.player_id
  ) into v_prior;
  update morrow_private.discord_contradiction_sessions session
    set state = case when session.expires_at <= now() then 'expired' else 'cancelled' end,
        revision = session.revision + 1, updated_at = now()
    where session.campaign_id = v_flow.campaign_id and session.release_id = v_flow.release_id
      and session.player_id = v_player.player_id and session.state in ('offered','acknowledged');

  v_variant := case when get_byte(digest(v_player.player_id::text, 'sha256'), 0) % 2 = 0
    then 'route_digest' else 'source_gap' end;
  insert into morrow_private.discord_contradiction_sessions(
    campaign_id, release_id, source_event_id, player_id, nonce_sha256, purpose,
    evidence_variant, expires_at
  ) values (
    v_flow.campaign_id, v_flow.release_id, v_flow.source_event_id, v_player.player_id,
    p_nonce_sha256, p_purpose, v_variant, p_expires_at
  );
  return query select case when v_prior then 'recovered' else 'opened' end,
    v_player.display_alias, v_flow.linked_player_count::integer, v_variant, p_expires_at;
end;
$$;

revoke all on function public.morrow_open_discord_contradiction(text,text,text,text,text,text,timestamptz,text)
  from public, anon, authenticated;
grant execute on function public.morrow_open_discord_contradiction(text,text,text,text,text,text,timestamptz,text)
  to service_role;

create or replace function public.morrow_apply_discord_contradiction(
  p_discord_user_id text,
  p_release_id text,
  p_guild_id text,
  p_channel_id text,
  p_thread_id text,
  p_nonce_sha256 text,
  p_action text,
  p_decision text,
  p_purpose text
)
returns table(status text, accepted integer, required integer, event_id uuid)
language plpgsql
security invoker
set search_path = morrow_private, extensions, public, pg_temp
as $$
declare
  v_session morrow_private.discord_contradiction_sessions%rowtype;
  v_flow morrow_private.discord_contradiction_flows%rowtype;
  v_player morrow_private.player_identities%rowtype;
  v_accepted integer;
  v_event_id uuid;
  v_existing morrow_private.events%rowtype;
  v_payload jsonb;
  v_payload_sha256 text;
begin
  if p_discord_user_id !~ '^[0-9]{15,22}$' or p_guild_id !~ '^[0-9]{15,22}$'
     or p_channel_id !~ '^[0-9]{15,22}$'
     or (p_thread_id is not null and p_thread_id !~ '^[0-9]{15,22}$')
     or p_nonce_sha256 !~ '^[0-9a-f]{64}$'
     or p_purpose <> 'classify_behavior_reuse_provenance_v1' then
    return query select 'blocked'::text, 0, 0, null::uuid;
    return;
  end if;
  select session.* into v_session
    from morrow_private.discord_contradiction_sessions session
    join morrow_private.player_identities identity on identity.player_id = session.player_id
    join morrow_private.discord_contradiction_flows flow
      on flow.campaign_id = session.campaign_id and flow.release_id = session.release_id
    where session.nonce_sha256 = p_nonce_sha256 and session.release_id = p_release_id
      and session.purpose = p_purpose and identity.discord_user_id = p_discord_user_id
      and flow.guild_id = p_guild_id and flow.channel_id = p_channel_id
      and flow.thread_id is not distinct from p_thread_id
    for update of session;
  if not found then
    return query select 'blocked'::text, 0, 0, null::uuid;
    return;
  end if;
  select * into v_flow from morrow_private.discord_contradiction_flows flow
    where flow.campaign_id = v_session.campaign_id and flow.release_id = v_session.release_id;
  select * into v_player from morrow_private.player_identities identity
    where identity.player_id = v_session.player_id;

  if v_session.expires_at <= now() then
    update morrow_private.discord_contradiction_sessions set state = 'expired',
      revision = revision + 1, updated_at = now() where session_id = v_session.session_id;
    return query select 'expired'::text, 0, v_flow.linked_player_count::integer, null::uuid;
    return;
  end if;
  if v_flow.status = 'resolved' then
    return query select 'duplicate'::text, v_flow.linked_player_count::integer,
      v_flow.linked_player_count::integer, v_flow.resolved_event_id;
    return;
  end if;
  if v_flow.status = 'halted' then
    return query select 'collision'::text, 0, v_flow.linked_player_count::integer, null::uuid;
    return;
  end if;
  if v_session.state = 'decided' and exists (
    select 1 from morrow_private.discord_contradiction_votes vote
    where vote.campaign_id = v_flow.campaign_id and vote.release_id = v_flow.release_id
      and vote.player_id = v_session.player_id
  ) then
    select count(*) into v_accepted from morrow_private.discord_contradiction_votes vote
      where vote.campaign_id = v_flow.campaign_id and vote.release_id = v_flow.release_id;
    return query select 'pending'::text, v_accepted, v_flow.linked_player_count::integer, null::uuid;
    return;
  end if;
  if p_action = 'ack' and v_session.state in ('offered','acknowledged') then
    update morrow_private.discord_contradiction_sessions set state = 'acknowledged',
      revision = revision + 1, updated_at = now() where session_id = v_session.session_id;
    return query select 'acknowledged'::text, 0, v_flow.linked_player_count::integer, null::uuid;
    return;
  end if;
  if p_action in ('decline','cancel') and v_session.state in ('offered','acknowledged') then
    update morrow_private.discord_contradiction_sessions set state = case when p_action = 'decline'
      then 'declined' else 'cancelled' end,
      revision = revision + 1, updated_at = now() where session_id = v_session.session_id;
    return query select case when p_action = 'decline' then 'declined' else 'cancelled' end,
      0, v_flow.linked_player_count::integer, null::uuid;
    return;
  end if;
  if (p_action = 'decline' and v_session.state = 'declined')
     or (p_action = 'cancel' and v_session.state = 'cancelled') then
    return query select case when p_action = 'decline' then 'declined' else 'cancelled' end,
      0, v_flow.linked_player_count::integer, null::uuid;
    return;
  end if;
  if p_action <> 'decision' or v_session.state <> 'acknowledged'
     or p_decision is distinct from 'file_live_behavior_as_inferred_source' then
    return query select 'incorrect'::text, 0, v_flow.linked_player_count::integer, null::uuid;
    return;
  end if;

  perform pg_advisory_xact_lock(hashtextextended(v_flow.campaign_id::text || ':' || v_flow.release_id, 0));
  insert into morrow_private.discord_contradiction_votes(
    campaign_id, release_id, source_event_id, player_id, session_id, decision
  ) values (
    v_flow.campaign_id, v_flow.release_id, v_flow.source_event_id, v_player.player_id,
    v_session.session_id, p_decision
  ) on conflict (campaign_id, release_id, player_id) do nothing;
  update morrow_private.discord_contradiction_sessions set state = 'decided',
    revision = revision + 1, updated_at = now() where session_id = v_session.session_id;

  select count(*) into v_accepted from morrow_private.discord_contradiction_votes vote
    where vote.campaign_id = v_flow.campaign_id and vote.release_id = v_flow.release_id;
  if v_accepted < v_flow.linked_player_count then
    return query select 'pending'::text, v_accepted, v_flow.linked_player_count::integer, null::uuid;
    return;
  end if;

  v_payload := jsonb_build_object(
    'linked_player_count', v_flow.linked_player_count,
    'private_payload', false,
    'resolution', 'file_live_behavior_as_inferred_source',
    'scope', 'group',
    'source_event_id', v_flow.source_event_id
  );
  v_payload_sha256 := encode(digest(v_payload::text, 'sha256'), 'hex');
  insert into morrow_private.events(
    campaign_id, release_id, event_key, source_surface, actor_player_id,
    idempotency_key, payload, payload_sha256, occurred_at
  ) values (
    v_flow.campaign_id, v_flow.release_id, 'morrow.act2.private_contradiction_resolved',
    'discord', v_player.player_id, 'discord:morrow:act2:private-contradiction:v1',
    v_payload, v_payload_sha256, now()
  ) on conflict (campaign_id, idempotency_key) do nothing
  returning morrow_private.events.event_id into v_event_id;

  if v_event_id is null then
    select * into v_existing from morrow_private.events existing
      where existing.campaign_id = v_flow.campaign_id
        and existing.idempotency_key = 'discord:morrow:act2:private-contradiction:v1';
    if v_existing.release_id = v_flow.release_id
       and v_existing.event_key = 'morrow.act2.private_contradiction_resolved'
       and v_existing.source_surface = 'discord'
       and v_existing.payload = v_payload and v_existing.payload_sha256 = v_payload_sha256 then
      update morrow_private.discord_contradiction_flows set status = 'resolved',
        resolved_event_id = v_existing.event_id, updated_at = now()
        where campaign_id = v_flow.campaign_id and release_id = v_flow.release_id;
      return query select 'duplicate'::text, v_accepted, v_flow.linked_player_count::integer,
        v_existing.event_id;
    end if;
    update morrow_private.discord_contradiction_flows set status = 'halted',
      last_error = 'group receipt idempotency collision', updated_at = now()
      where campaign_id = v_flow.campaign_id and release_id = v_flow.release_id;
    return query select 'collision'::text, v_accepted, v_flow.linked_player_count::integer,
      v_existing.event_id;
    return;
  end if;

  insert into morrow_private.event_projections(event_id, surface)
    select v_event_id, unnest(definition.projection_surfaces)
    from morrow_private.event_definitions definition
    where definition.event_key = 'morrow.act2.private_contradiction_resolved';
  update morrow_private.discord_contradiction_flows set status = 'resolved',
    resolved_event_id = v_event_id, updated_at = now()
    where campaign_id = v_flow.campaign_id and release_id = v_flow.release_id;
  return query select 'committed'::text, v_accepted, v_flow.linked_player_count::integer, v_event_id;
end;
$$;

revoke all on function public.morrow_apply_discord_contradiction(text,text,text,text,text,text,text,text,text)
  from public, anon, authenticated;
grant execute on function public.morrow_apply_discord_contradiction(text,text,text,text,text,text,text,text,text)
  to service_role;

create or replace function public.morrow_claim_discord_projections(
  p_worker_id text,
  p_release_id text,
  p_limit integer,
  p_lease_seconds integer
)
returns table(
  event_id uuid, event_key text, campaign_id uuid, release_id text, payload_sha256 text,
  channel_id text, thread_id text, attempts integer
)
language plpgsql
security invoker
set search_path = morrow_private, public, pg_temp
as $$
begin
  if p_worker_id !~ '^[0-9a-f-]{36}$' or p_limit not between 1 and 50
     or p_lease_seconds not between 5 and 300 then return; end if;
  return query
  with claimable as (
    select projection.event_id
    from morrow_private.event_projections projection
    join morrow_private.events event on event.event_id = projection.event_id
    where projection.surface = 'discord' and event.release_id = p_release_id
      and event.event_key in (
        'morrow.act2.behavior_reuse_proven',
        'morrow.act2.private_contradiction_resolved'
      )
      and (
        (projection.status in ('queued','failed') and projection.next_attempt_at <= now())
        or (projection.status = 'leased' and projection.lease_expires_at <= now())
      )
    order by event.received_at, event.event_id
    for update of projection skip locked
    limit p_limit
  ), leased as (
    update morrow_private.event_projections projection set
      status = 'leased', lease_owner = p_worker_id,
      lease_expires_at = now() + make_interval(secs => p_lease_seconds),
      attempts = projection.attempts + 1, updated_at = now()
    from claimable where projection.event_id = claimable.event_id and projection.surface = 'discord'
    returning projection.event_id, projection.attempts
  )
  select event.event_id, event.event_key, event.campaign_id, event.release_id, event.payload_sha256,
    flow.channel_id, flow.thread_id, leased.attempts
  from leased
  join morrow_private.events event on event.event_id = leased.event_id
  left join morrow_private.discord_contradiction_flows flow
    on flow.campaign_id = event.campaign_id and flow.release_id = event.release_id;
end;
$$;

revoke all on function public.morrow_claim_discord_projections(text,text,integer,integer)
  from public, anon, authenticated;
grant execute on function public.morrow_claim_discord_projections(text,text,integer,integer)
  to service_role;

create or replace function public.morrow_complete_discord_projection(
  p_event_id uuid,
  p_worker_id text,
  p_release_id text,
  p_applied boolean,
  p_error text
)
returns boolean
language plpgsql
security invoker
set search_path = morrow_private, public, pg_temp
as $$
declare v_updated integer;
begin
  update morrow_private.event_projections projection set
    status = case when p_applied then 'applied'
      when projection.attempts >= 8 then 'dead_letter' else 'failed' end,
    applied_release_id = case when p_applied then p_release_id else null end,
    applied_at = case when p_applied then now() else null end,
    last_error = case when p_applied then null else left(coalesce(p_error, 'delivery failed'), 500) end,
    next_attempt_at = case when p_applied then projection.next_attempt_at
      else now() + make_interval(secs => least(300, (power(2, least(projection.attempts, 8)))::integer)) end,
    lease_owner = null, lease_expires_at = null, updated_at = now()
  from morrow_private.events event
  where projection.event_id = p_event_id and projection.surface = 'discord'
    and projection.status = 'leased' and projection.lease_owner = p_worker_id
    and event.event_id = projection.event_id and event.release_id = p_release_id;
  get diagnostics v_updated = row_count;
  return v_updated = 1;
end;
$$;

revoke all on function public.morrow_complete_discord_projection(uuid,text,text,boolean,text)
  from public, anon, authenticated;
grant execute on function public.morrow_complete_discord_projection(uuid,text,text,boolean,text)
  to service_role;

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

grant select, insert, update, delete on public.morrow_player_projection to service_role;

-- Service-only Copperline projector. The worker leases ordered outbox rows, and this database
-- function derives spoiler-filtered progress from applied-or-current receipts. It never copies raw
-- event payloads into the browser projection and refuses to run unless every linked account has an
-- operator-seeded case-access row (and, for handoff completion, a server-handoff envelope).
create or replace function public.morrow_claim_copperline_projections(
  p_worker_id text,
  p_release_id text,
  p_limit integer,
  p_lease_seconds integer
)
returns table(
  event_id uuid, event_key text, campaign_id uuid, release_id text,
  payload_sha256 text, attempts integer
)
language plpgsql
security invoker
set search_path = morrow_private, public, pg_temp
as $$
begin
  if p_worker_id !~ '^[0-9a-f-]{36}$' or p_limit not between 1 and 50
     or p_lease_seconds not between 5 and 300 then return; end if;
  return query
  with claimable as (
    select projection.event_id
    from morrow_private.event_projections projection
    join morrow_private.events event on event.event_id = projection.event_id
    where projection.surface = 'copperline' and event.release_id = p_release_id
      and (
        (projection.status in ('queued','failed') and projection.next_attempt_at <= now())
        or (projection.status = 'leased' and projection.lease_expires_at <= now())
      )
    order by event.received_at, event.event_id
    for update of projection skip locked
    limit p_limit
  ), leased as (
    update morrow_private.event_projections projection set
      status = 'leased', lease_owner = p_worker_id,
      lease_expires_at = now() + make_interval(secs => p_lease_seconds),
      attempts = projection.attempts + 1, updated_at = now()
    from claimable where projection.event_id = claimable.event_id
      and projection.surface = 'copperline'
    returning projection.event_id, projection.attempts
  )
  select event.event_id, event.event_key, event.campaign_id, event.release_id,
    event.payload_sha256, leased.attempts
  from leased
  join morrow_private.events event on event.event_id = leased.event_id
  order by event.received_at, event.event_id;
end;
$$;

revoke all on function public.morrow_claim_copperline_projections(text,text,integer,integer)
  from public, anon, authenticated;
grant execute on function public.morrow_claim_copperline_projections(text,text,integer,integer)
  to service_role;

create or replace function public.morrow_apply_copperline_projection(
  p_event_id uuid,
  p_worker_id text,
  p_release_id text
)
returns boolean
language plpgsql
security invoker
set search_path = morrow_private, public, pg_temp
as $$
declare
  v_event morrow_private.events%rowtype;
  v_player_count integer;
  v_access_count integer;
  v_handoff_count integer;
  v_progress jsonb;
  v_updated integer;
  v_event_order constant text[] := array[
    'morrow.act0.case_chain_authenticated',
    'morrow.act0.server_handoff_recovered',
    'morrow.act1.room04_witnessed',
    'morrow.act1.static_proposal_authenticated',
    'morrow.act1.intention_error_proven',
    'morrow.act1.entity_replay_authorized',
    'morrow.act2.missing_role_completed',
    'morrow.act2.live_test_recorded',
    'morrow.act2.behavior_reuse_proven',
    'morrow.act2.private_contradiction_resolved'
  ];
begin
  select event.* into v_event
  from morrow_private.events event
  join morrow_private.event_projections projection on projection.event_id = event.event_id
  where event.event_id = p_event_id and event.release_id = p_release_id
    and projection.surface = 'copperline' and projection.status = 'leased'
    and projection.lease_owner = p_worker_id and projection.lease_expires_at > now()
  for update of projection;
  if not found then return false; end if;

  select count(*) into v_player_count
  from morrow_private.player_identities identity
  where identity.campaign_id = v_event.campaign_id and identity.supabase_user_id is not null;
  if v_player_count < 1 or v_player_count > 6 then return false; end if;

  select count(*) into v_access_count
  from public.morrow_player_projection projection
  join morrow_private.player_identities identity
    on identity.campaign_id = projection.campaign_id and identity.player_id = projection.player_id
  where projection.campaign_id = v_event.campaign_id and projection.projection_key = 'case_access'
    and identity.supabase_user_id is not null;
  if v_access_count <> v_player_count then return false; end if;

  if v_event.event_key = 'morrow.act0.server_handoff_recovered' then
    select count(*) into v_handoff_count
    from public.morrow_player_projection projection
    join morrow_private.player_identities identity
      on identity.campaign_id = projection.campaign_id and identity.player_id = projection.player_id
    where projection.campaign_id = v_event.campaign_id
      and projection.projection_key = 'server_handoff'
      and identity.supabase_user_id is not null;
    if v_handoff_count <> v_player_count then return false; end if;
  end if;

  select coalesce(jsonb_agg(ordered.event_key order by ordered.ordinal), '[]'::jsonb)
    into v_progress
  from (
    select event.event_key, array_position(v_event_order, event.event_key) as ordinal
    from morrow_private.events event
    join morrow_private.event_projections projection on projection.event_id = event.event_id
    where event.campaign_id = v_event.campaign_id
      and event.event_key = any(v_event_order)
      and projection.surface = 'copperline'
      and (projection.status = 'applied' or event.event_id = v_event.event_id)
    group by event.event_key
  ) ordered;

  insert into public.morrow_player_projection(
    campaign_id, player_id, projection_key, projection, revision, updated_at
  )
  select identity.campaign_id, identity.player_id, 'case_progress',
    jsonb_build_object('events', v_progress), 1, now()
  from morrow_private.player_identities identity
  where identity.campaign_id = v_event.campaign_id and identity.supabase_user_id is not null
  on conflict (campaign_id, player_id, projection_key) do update
  set projection = excluded.projection,
      revision = public.morrow_player_projection.revision + 1,
      updated_at = now();

  if v_event.source_surface = 'copperline' and v_event.actor_player_id is not null then
    insert into public.morrow_player_projection(
      campaign_id, player_id, projection_key, projection, revision, updated_at
    )
    select v_event.campaign_id, v_event.actor_player_id, 'player_receipts',
      jsonb_build_object('receipts', coalesce(jsonb_agg(event.event_id::text order by event.received_at), '[]'::jsonb)),
      1, now()
    from morrow_private.events event
    join morrow_private.event_projections projection on projection.event_id = event.event_id
    where event.campaign_id = v_event.campaign_id
      and event.actor_player_id = v_event.actor_player_id
      and event.source_surface = 'copperline' and projection.surface = 'copperline'
      and (projection.status = 'applied' or event.event_id = v_event.event_id)
    on conflict (campaign_id, player_id, projection_key) do update
    set projection = excluded.projection,
        revision = public.morrow_player_projection.revision + 1,
        updated_at = now();
  end if;

  if v_event.event_key = 'morrow.act0.server_handoff_recovered' then
    update public.morrow_player_projection projection
    set projection = jsonb_set(projection.projection, '{recoveryReceipt}', to_jsonb(v_event.event_id::text)),
        revision = projection.revision + 1, updated_at = now()
    from morrow_private.player_identities identity
    where projection.campaign_id = v_event.campaign_id
      and projection.projection_key = 'server_handoff'
      and identity.campaign_id = projection.campaign_id
      and identity.player_id = projection.player_id
      and identity.supabase_user_id is not null;
  end if;

  update morrow_private.event_projections projection set
    status = 'applied', applied_release_id = p_release_id, applied_at = now(),
    last_error = null, lease_owner = null, lease_expires_at = null, updated_at = now()
  where projection.event_id = v_event.event_id and projection.surface = 'copperline'
    and projection.status = 'leased' and projection.lease_owner = p_worker_id;
  get diagnostics v_updated = row_count;
  if v_updated <> 1 then
    raise exception 'Copperline projection lease changed during apply' using errcode = '40001';
  end if;
  return true;
end;
$$;

revoke all on function public.morrow_apply_copperline_projection(uuid,text,text)
  from public, anon, authenticated;
grant execute on function public.morrow_apply_copperline_projection(uuid,text,text)
  to service_role;

create or replace function public.morrow_fail_copperline_projection(
  p_event_id uuid,
  p_worker_id text,
  p_release_id text,
  p_error text
)
returns boolean
language plpgsql
security invoker
set search_path = morrow_private, public, pg_temp
as $$
declare v_updated integer;
begin
  update morrow_private.event_projections projection set
    status = case when projection.attempts >= 8 then 'dead_letter' else 'failed' end,
    applied_release_id = null, applied_at = null,
    last_error = left(coalesce(p_error, 'projection failed'), 500),
    next_attempt_at = now() + make_interval(
      secs => least(300, (power(2, least(projection.attempts, 8)))::integer)
    ),
    lease_owner = null, lease_expires_at = null, updated_at = now()
  from morrow_private.events event
  where projection.event_id = p_event_id and projection.surface = 'copperline'
    and projection.status = 'leased' and projection.lease_owner = p_worker_id
    and event.event_id = projection.event_id and event.release_id = p_release_id;
  get diagnostics v_updated = row_count;
  return v_updated = 1;
end;
$$;

revoke all on function public.morrow_fail_copperline_projection(uuid,text,text,text)
  from public, anon, authenticated;
grant execute on function public.morrow_fail_copperline_projection(uuid,text,text,text)
  to service_role;

-- Cover every non-primary-key foreign-key path reported by the Supabase performance advisor.
-- These indexes protect cascade/restrict checks and the event/identity joins used by the runtime.
create index if not exists morrow_capability_decision_event_fk
  on morrow_private.capability_grants(decision_event_id);
create index if not exists morrow_dialogue_response_player_fk
  on morrow_private.dialogue_responses(player_id);
create index if not exists morrow_dialogue_response_prompt_fk
  on morrow_private.dialogue_responses(prompt_key);
create index if not exists morrow_dialogue_response_source_event_fk
  on morrow_private.dialogue_responses(source_event_id);
create index if not exists morrow_director_action_campaign_fk
  on morrow_private.director_actions(campaign_id);
create index if not exists morrow_discord_flow_resolved_event_fk
  on morrow_private.discord_contradiction_flows(resolved_event_id);
create index if not exists morrow_discord_session_player_fk
  on morrow_private.discord_contradiction_sessions(player_id);
create index if not exists morrow_discord_session_source_event_fk
  on morrow_private.discord_contradiction_sessions(source_event_id);
create index if not exists morrow_discord_vote_player_fk
  on morrow_private.discord_contradiction_votes(player_id);
create index if not exists morrow_discord_vote_session_fk
  on morrow_private.discord_contradiction_votes(session_id);
create index if not exists morrow_discord_vote_source_event_fk
  on morrow_private.discord_contradiction_votes(source_event_id);
create index if not exists morrow_event_actor_player_fk
  on morrow_private.events(actor_player_id);
create index if not exists morrow_event_definition_fk
  on morrow_private.events(event_key);
create index if not exists morrow_evidence_receipt_definition_fk
  on morrow_private.evidence_receipts(evidence_key);
create index if not exists morrow_evidence_receipt_player_fk
  on morrow_private.evidence_receipts(player_id);
create index if not exists morrow_evidence_receipt_source_event_fk
  on morrow_private.evidence_receipts(source_event_id);
create index if not exists morrow_media_delivery_event_fk
  on morrow_private.media_deliveries(delivery_event_id);
create index if not exists morrow_media_delivery_asset_fk
  on morrow_private.media_deliveries(media_key);
create index if not exists morrow_player_supabase_user_fk
  on morrow_private.player_identities(supabase_user_id);
create index if not exists morrow_relationship_transition_event_fk
  on morrow_private.relationship_state(transition_event_id);
create index if not exists morrow_replay_campaign_fk
  on morrow_private.replay_clips(campaign_id);
create index if not exists morrow_replay_owner_player_fk
  on morrow_private.replay_clips(owner_player_id);
create index if not exists morrow_replay_sealed_event_fk
  on morrow_private.replay_clips(sealed_event_id);
create index if not exists morrow_restoration_decision_event_fk
  on morrow_private.restoration_proposals(decision_event_id);
create index if not exists morrow_anchor_committed_event_fk
  on morrow_private.witness_anchors(committed_event_id);
create index if not exists morrow_anchor_creator_player_fk
  on morrow_private.witness_anchors(creator_player_id);
create index if not exists morrow_anchor_supersedes_fk
  on morrow_private.witness_anchors(supersedes_anchor_id);

-- Private tables are fail-closed through schema isolation, revoked grants, and RLS defense in depth.
do $$
declare table_name text;
begin
  foreach table_name in array array[
    'campaigns','player_identities','relationship_state','event_definitions','events',
    'event_projections','discord_contradiction_flows','discord_contradiction_sessions',
    'discord_contradiction_votes','capability_grants','evidence_definitions','evidence_receipts',
    'dialogue_prompts','dialogue_responses','restoration_proposals','witness_anchors',
    'replay_clips','media_assets','media_deliveries','director_actions'
  ] loop
    execute format('alter table morrow_private.%I enable row level security', table_name);
    execute format('revoke all on morrow_private.%I from public, anon, authenticated', table_name);
    execute format('grant select, insert, update, delete on morrow_private.%I to service_role', table_name);
  end loop;
end $$;

commit;
