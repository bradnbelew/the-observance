-- Forward-only correction: M06 proves incomplete consensus, not M05 source preservation.
-- Historical event UUIDs, payloads, idempotency keys, projections, and relationship links are retained.

begin;

do $morrow_m06_consensus_event$
declare
  v_old constant text := 'morrow.act3.contradiction_preserved';
  v_new constant text := 'morrow.act3.incomplete_consensus_proven';
  v_projection_definition text;
begin
  if exists (
    select 1 from morrow_private.event_definitions where event_key = v_old
  ) then
    if exists (
      select 1 from morrow_private.event_definitions where event_key = v_new
    ) then
      raise exception 'M06 event correction collision: both % and % exist', v_old, v_new;
    end if;

    insert into morrow_private.event_definitions(
      event_key, act, owner_surface, prerequisite_events, projection_surfaces, active
    )
    select v_new, act, owner_surface, prerequisite_events, projection_surfaces, active
    from morrow_private.event_definitions
    where event_key = v_old;

    update morrow_private.events
    set event_key = v_new
    where event_key = v_old;

    update morrow_private.event_definitions
    set prerequisite_events = array_replace(prerequisite_events, v_old, v_new)
    where prerequisite_events @> array[v_old];

    delete from morrow_private.event_definitions where event_key = v_old;
  elsif not exists (
    select 1 from morrow_private.event_definitions where event_key = v_new
  ) then
    raise exception 'M06 event correction cannot find either % or %', v_old, v_new;
  end if;

  update public.morrow_player_projection
  set projection = replace(projection::text, v_old, v_new)::jsonb,
      revision = revision + 1,
      updated_at = now()
  where projection::text like '%' || v_old || '%';

  select pg_get_functiondef(
    'public.morrow_apply_copperline_projection(uuid,text,text)'::regprocedure
  ) into strict v_projection_definition;

  if position(v_old in v_projection_definition) > 0 then
    execute replace(v_projection_definition, v_old, v_new);
  elsif position(v_new in v_projection_definition) = 0 then
    raise exception 'Copperline projection function contains neither M06 event key';
  end if;

  if exists (
    select 1 from morrow_private.events where event_key = v_old
  ) or exists (
    select 1
    from morrow_private.event_definitions
    where event_key = v_old or prerequisite_events @> array[v_old]
  ) then
    raise exception 'M06 event correction left a stale event or prerequisite';
  end if;
end;
$morrow_m06_consensus_event$;

commit;
