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

commit;
