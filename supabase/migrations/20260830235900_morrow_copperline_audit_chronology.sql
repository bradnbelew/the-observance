-- Additive Morrow Act 6 Copperline authority. This forward migration is locally
-- verified and intentionally remains unapplied pending a fresh isolated rehearsal.
begin;

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
        'morrow.act0.server_handoff_recovered',
        'morrow.act6.audit_chronology_proven'
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

  if p_event_key = 'morrow.act6.audit_chronology_proven' and not (
    p_payload = jsonb_build_object(
      'case_id', 'CL-RCV-04',
      'investigation', 'M11',
      'operation', 'prove_audit_chronology',
      'record_order', jsonb_build_array(
        'theo_live_capture_permission',
        'rookery_anchor_graph',
        'iona_shutdown_order',
        'morrow_snapshot_manifest',
        'captioned_current_voice_assembly'
      ),
      'continuity_claim', null
    ) and p_payload_sha256 = 'cbf40a46335441d5d164d4e8f8f6afd02a97503ade2163ad3fd86e9063fc51e5'
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
  v_media jsonb;
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
    'morrow.act2.live_capture_authorized',
    'morrow.act3.version_fragments_authenticated',
    'morrow.act3.contradiction_preserved',
    'morrow.act4.witness_anchor_registered',
    'morrow.act4.almost_home_proven',
    'morrow.act4.account_continuity_authorized',
    'morrow.act5.continued_session_observed',
    'morrow.act5.returning_identity_authenticated',
    'morrow.act5.dual_session_consciousness_proven',
    'morrow.act6.audit_chronology_proven',
    'morrow.act6.current_morrow_reconstruction_proven',
    'morrow.act6.cold_storage_access_authorized',
    'morrow.act7.rollback_anchors_committed',
    'morrow.act7.branch_policy_committed',
    'morrow.act7.branch_governance_authorized',
    'morrow.act7.coda_started'
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

  select coalesce(jsonb_agg(earned.media_key order by earned.ordinal), '[]'::jsonb)
    into v_media
  from (
    select asset.media_key, asset.ordinal
    from (values
      ('morrow.m03.captioned_voice_fragment', 'morrow.act1.entity_replay_authorized', 1),
      ('morrow.m04.route_derivative', 'morrow.act2.live_test_recorded', 2),
      ('morrow.m08.almost_home_comparison', 'morrow.act4.almost_home_proven', 3),
      ('morrow.m11.current_voice_assembly', 'morrow.act6.audit_chronology_proven', 4)
    ) asset(media_key, prerequisite_event, ordinal)
    where exists (
      select 1 from morrow_private.events event
      join morrow_private.event_projections projection on projection.event_id = event.event_id
      where event.campaign_id = v_event.campaign_id
        and event.event_key = asset.prerequisite_event
        and projection.surface = 'copperline'
        and (projection.status = 'applied' or event.event_id = v_event.event_id)
    )
    union all
    select case policy.payload->>'ending'
        when 'certify' then 'morrow.m12.coda.certify'
        when 'preserve_audit' then 'morrow.m12.coda.preserve_audit'
        when 'close_ticket' then 'morrow.m12.coda.close_ticket'
        when 'create_new_branch' then 'morrow.m12.coda.create_new_branch'
      end as media_key,
      case policy.payload->>'ending'
        when 'certify' then 5 when 'preserve_audit' then 6
        when 'close_ticket' then 7 when 'create_new_branch' then 8
      end as ordinal
    from morrow_private.events policy
    join morrow_private.event_projections policy_projection on policy_projection.event_id = policy.event_id
    where policy.campaign_id = v_event.campaign_id
      and policy.event_key = 'morrow.act7.branch_policy_committed'
      and policy.payload->>'ending' in ('certify','preserve_audit','close_ticket','create_new_branch')
      and policy_projection.surface = 'copperline' and policy_projection.status = 'applied'
      and exists (
        select 1 from morrow_private.events coda
        join morrow_private.event_projections coda_projection on coda_projection.event_id = coda.event_id
        where coda.campaign_id = v_event.campaign_id and coda.event_key = 'morrow.act7.coda_started'
          and coda_projection.surface = 'copperline'
          and (coda_projection.status = 'applied' or coda.event_id = v_event.event_id)
      )
  ) earned
  where earned.media_key is not null;

  insert into public.morrow_player_projection(
    campaign_id, player_id, projection_key, projection, revision, updated_at
  )
  select identity.campaign_id, identity.player_id, 'case_media',
    jsonb_build_object('keys', v_media), 1, now()
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

insert into morrow_private.media_assets(
  media_key, release_id, storage_path, sha256, media_type,
  accessibility_path, prerequisite_events, active
) values
  ('morrow.m03.captioned_voice_fragment', 'morrow.reboot.media.v1',
   '/support/cases/mossfield-recovery/media/morrow.m03.captioned_voice_fragment',
   '42a7135186463bbd6d0543430e4bd4415e8a4b59f1799e011761eba8684f5dbf', 'audio',
   '/support/cases/mossfield-recovery/media/morrow.m03.captioned_voice_fragment',
   '{morrow.act1.entity_replay_authorized}', true),
  ('morrow.m04.route_derivative', 'morrow.reboot.media.v1',
   '/support/cases/mossfield-recovery/media/morrow.m04.route_derivative',
   '4e020cc9deba442d8c203b68cd90b45f37bc73e3c53ad6838aa3811811697d62', 'voxel_diagram',
   '/support/cases/mossfield-recovery/media/morrow.m04.route_derivative',
   '{morrow.act2.live_test_recorded}', true),
  ('morrow.m08.almost_home_comparison', 'morrow.reboot.media.v1',
   '/support/cases/mossfield-recovery/media/morrow.m08.almost_home_comparison',
   '49454a55b1c4f6d670c8ca53c5be344755e1f5032c5148573b05eca808e3ca37', 'voxel_diagram',
   '/support/cases/mossfield-recovery/media/morrow.m08.almost_home_comparison',
   '{morrow.act4.almost_home_proven}', true),
  ('morrow.m11.current_voice_assembly', 'morrow.reboot.media.v1',
   '/support/cases/mossfield-recovery/media/morrow.m11.current_voice_assembly',
   '83aadd5cb2330b6db1534477cfefd93093a039670a2059b707c55f97ae5cde03', 'audio',
   '/support/cases/mossfield-recovery/media/morrow.m11.current_voice_assembly',
   '{morrow.act6.audit_chronology_proven}', true),
  ('morrow.m12.coda.certify', 'morrow.reboot.media.v1',
   '/support/cases/mossfield-recovery/media/morrow.m12.coda.certify',
   '6df9f16822a6bd8569c8b2f4f7d810b6690aeeb6537adea1aced76a43a88ed7a', 'document',
   '/support/cases/mossfield-recovery/media/morrow.m12.coda.certify',
   '{morrow.act7.coda_started}', true),
  ('morrow.m12.coda.preserve_audit', 'morrow.reboot.media.v1',
   '/support/cases/mossfield-recovery/media/morrow.m12.coda.preserve_audit',
   'd5e4ec93f2bd76b9e81afb3844f1d5d5c0f2aa2c10e5cca8f9e35a5db30d1684', 'document',
   '/support/cases/mossfield-recovery/media/morrow.m12.coda.preserve_audit',
   '{morrow.act7.coda_started}', true),
  ('morrow.m12.coda.close_ticket', 'morrow.reboot.media.v1',
   '/support/cases/mossfield-recovery/media/morrow.m12.coda.close_ticket',
   'f40b98189a43203f190fe44403edc4040f935395f6b324380d044cc8999c69ad', 'document',
   '/support/cases/mossfield-recovery/media/morrow.m12.coda.close_ticket',
   '{morrow.act7.coda_started}', true),
  ('morrow.m12.coda.create_new_branch', 'morrow.reboot.media.v1',
   '/support/cases/mossfield-recovery/media/morrow.m12.coda.create_new_branch',
   'a609327ce8ae1c8032800265acd1e9127fa2172e35c18482744bf263f724648e', 'document',
   '/support/cases/mossfield-recovery/media/morrow.m12.coda.create_new_branch',
   '{morrow.act7.coda_started}', true)
on conflict (media_key) do update set
  release_id = excluded.release_id,
  storage_path = excluded.storage_path,
  sha256 = excluded.sha256,
  media_type = excluded.media_type,
  accessibility_path = excluded.accessibility_path,
  prerequisite_events = excluded.prerequisite_events,
  active = excluded.active;

commit;
