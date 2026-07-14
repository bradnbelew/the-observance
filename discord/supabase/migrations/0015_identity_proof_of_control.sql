-- The Observance — short-lived Minecraft proof-of-control for Discord identity binding
-- 0015_identity_proof_of_control.sql
--
-- The four-digit Copperline callback is shared story evidence, not account authentication.  A
-- Discord user must also present a one-time code generated for their exact online Minecraft UUID.
-- Minecraft generates 60 bits of randomness and sends only its SHA-256 digest here; plaintext is
-- shown once to the player and is never stored, logged, or sent to this database.

begin;

create table if not exists public.identity_link_challenges (
  player_id uuid primary key references public.players(id) on delete cascade,
  code_hash text not null unique check (code_hash ~ '^[0-9a-f]{64}$'),
  issued_at timestamptz not null default now(),
  expires_at timestamptz not null,
  failed_attempts integer not null default 0 check (failed_attempts between 0 and 5),
  last_failed_at timestamptz,
  consumed_at timestamptz,
  consumed_by_discord_id text,
  recovered boolean not null default false,
  previous_player_id uuid references public.players(id) on delete set null,
  check (expires_at > issued_at),
  check ((consumed_at is null and consumed_by_discord_id is null)
      or (consumed_at is not null and consumed_by_discord_id is not null))
);

alter table public.identity_link_challenges enable row level security;
revoke all on public.identity_link_challenges from public, anon, authenticated;

-- Called only by the Paper plugin with its service-role credential.  One player has one current
-- challenge. Reissuing within 30 seconds leaves the still-visible previous code valid instead of
-- silently replacing it with a code the player never received.
create or replace function public.observance_issue_identity_link_challenge(
  p_mc_uuid text,
  p_code_hash text
) returns table (
  issue_state text,
  challenge_expires_at timestamptz
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_player_id uuid;
  v_existing public.identity_link_challenges%rowtype;
  v_now timestamptz := now();
  v_expires timestamptz := now() + interval '5 minutes';
begin
  if coalesce(btrim(p_mc_uuid), '') !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
     or coalesce(p_code_hash, '') !~ '^[0-9a-f]{64}$' then
    return query select 'unknown'::text, null::timestamptz;
    return;
  end if;

  -- Player row first, then challenge row: the claim RPC takes locks in the same order.
  select p.id
    into v_player_id
    from public.players p
   where lower(p.mc_uuid) = lower(btrim(p_mc_uuid))
   for update;
  if not found then
    return query select 'unknown'::text, null::timestamptz;
    return;
  end if;

  select c.*
    into v_existing
    from public.identity_link_challenges c
   where c.player_id = v_player_id
   for update;
  if found and v_existing.issued_at > v_now - interval '30 seconds' then
    return query select 'rate_limited'::text, v_existing.expires_at;
    return;
  end if;

  insert into public.identity_link_challenges (
    player_id, code_hash, issued_at, expires_at, failed_attempts, last_failed_at,
    consumed_at, consumed_by_discord_id, recovered, previous_player_id
  ) values (
    v_player_id, p_code_hash, v_now, v_expires, 0, null,
    null, null, false, null
  )
  on conflict (player_id) do update set
    code_hash = excluded.code_hash,
    issued_at = excluded.issued_at,
    expires_at = excluded.expires_at,
    failed_attempts = 0,
    last_failed_at = null,
    consumed_at = null,
    consumed_by_discord_id = null,
    recovered = false,
    previous_player_id = null;

  return query select 'issued'::text, v_expires;
end;
$$;

revoke all on function public.observance_issue_identity_link_challenge(text,text)
  from public, anon, authenticated;
grant execute on function public.observance_issue_identity_link_challenge(text,text) to service_role;

-- Remove the claim-first three-argument surface. DROP IF EXISTS succeeds in both clean installs and
-- upgrades where a draft 0014 was already applied; retaining an overload would preserve a bypass.
drop function if exists public.observance_claim_identity_handoff(text,text,text);

create or replace function public.observance_claim_identity_handoff(
  p_discord_id text,
  p_mc_name text,
  p_callback text,
  p_code_hash text
) returns table (
  claim_state text,
  player_id uuid,
  mc_uuid text,
  minecraft_name text,
  linked_discord_id text,
  receipt_inserted boolean,
  recovered boolean
)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_prerequisites text[];
  v_flags jsonb;
  v_target public.players%rowtype;
  v_previous public.players%rowtype;
  v_challenge public.identity_link_challenges%rowtype;
  v_match_count integer;
  v_had_previous boolean := false;
  v_inserted boolean := false;
  v_now timestamptz := now();
begin
  -- Callback, caller, name, and proof shape are checked before locks or writes. The story callback
  -- and LS04 gate remain independent requirements; the one-time code does not replace either.
  if coalesce(p_discord_id, '') !~ '^[0-9]{5,32}$'
     or coalesce(btrim(p_mc_name), '') !~ '^[A-Za-z0-9_]{3,16}$' then
    return query select 'unknown'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;
  if regexp_replace(coalesce(p_callback, ''), '[^0-9]', '', 'g') <> '9137' then
    return query select 'invalid'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;
  if coalesce(p_code_hash, '') !~ '^[0-9a-f]{64}$' then
    return query select 'challenge'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;

  select n.prerequisite_flags
    into v_prerequisites
    from public.investigation_nodes n
   where n.node_key = 'LS05' and n.active and n.required;
  if not found then
    raise exception 'LS05 identity binding is absent or inactive';
  end if;
  select a.flags into v_flags from public.arc_state a where a.id = 1;
  if not found or exists (
    select 1
      from unnest(v_prerequisites) as required_flag
     where not coalesce((v_flags ->> required_flag)::boolean, false)
  ) then
    return query select 'blocked'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;

  perform pg_advisory_xact_lock(hashtextextended(p_discord_id, 0));

  select count(*)::integer
    into v_match_count
    from public.players p
   where lower(p.name) = lower(btrim(p_mc_name));
  if v_match_count <> 1 then
    return query select 'unknown'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;

  select p.*
    into v_target
    from public.players p
   where lower(p.name) = lower(btrim(p_mc_name))
   for update;

  if v_target.discord_id is not null and v_target.discord_id <> p_discord_id then
    -- The bot maps conflict, unknown, and challenge to one private response.
    return query select 'conflict'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;

  select c.*
    into v_challenge
    from public.identity_link_challenges c
   where c.player_id = v_target.id
   for update;
  if not found then
    return query select 'challenge'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;

  -- Network retries of one already-committed transaction are idempotent for ten minutes, but only
  -- for the same Discord account, same target, and exact consumed digest. No second reward or bind.
  if v_challenge.consumed_at is not null then
    if v_challenge.consumed_by_discord_id = p_discord_id
       and v_challenge.code_hash = p_code_hash
       and v_target.discord_id = p_discord_id
       and v_challenge.consumed_at >= v_now - interval '10 minutes' then
      select public.observance_record_evidence(
        'discord:identity-link:LS05:' || p_discord_id || ':' || v_target.id::text,
        'LS05', 'discord',
        'discord:identity-link:LS05:' || p_discord_id || ':' || v_target.id::text,
        v_target.id,
        jsonb_build_object(
          'handler', 'identity_link', 'site_id', 'discord_link',
          'callback_verified', true, 'minecraft_proof_verified', true,
          'minecraft_name', v_target.name, 'recovered', v_challenge.recovered,
          'previous_player_id', v_challenge.previous_player_id
        )
      ) into v_inserted;
      return query select 'complete'::text, v_target.id, v_target.mc_uuid,
        v_target.name, p_discord_id, v_inserted, v_challenge.recovered;
      return;
    end if;
    return query select 'challenge'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;

  if v_challenge.expires_at <= v_now then
    return query select 'challenge'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;

  if v_challenge.code_hash <> p_code_hash then
    -- Five failures are counted per challenge. Further wrong guesses within 30 seconds remain a
    -- private no-op; an exact proof is never locked out by somebody else's guesses.
    update public.identity_link_challenges
       set failed_attempts = case
             when last_failed_at is null or last_failed_at < v_now - interval '30 seconds' then 1
             else least(5, failed_attempts + 1)
           end,
           last_failed_at = v_now
     where player_id = v_target.id
       and (failed_attempts < 5
         or last_failed_at is null
         or last_failed_at < v_now - interval '30 seconds');
    return query select 'challenge'::text, null::uuid, null::text, null::text,
      null::text, false, false;
    return;
  end if;

  -- Consume before identity mutation, in this same transaction. Any later exception rolls back the
  -- consumption, previous-row clear, target bind, and evidence receipt together.
  update public.identity_link_challenges
     set consumed_at = v_now,
         consumed_by_discord_id = p_discord_id
   where player_id = v_target.id and consumed_at is null;
  if not found then
    raise exception 'identity challenge changed while locked';
  end if;

  select p.*
    into v_previous
    from public.players p
   where p.discord_id = p_discord_id
   for update;
  v_had_previous := found;

  if v_had_previous and v_previous.id <> v_target.id then
    update public.players
       set discord_id = null
     where id = v_previous.id and discord_id = p_discord_id;
  end if;

  if v_target.discord_id is null then
    update public.players
       set discord_id = p_discord_id
     where id = v_target.id and discord_id is null
     returning * into v_target;
    if not found then
      raise exception 'identity claim changed while locked';
    end if;
  end if;

  select public.observance_record_evidence(
    'discord:identity-link:LS05:' || p_discord_id || ':' || v_target.id::text,
    'LS05', 'discord',
    'discord:identity-link:LS05:' || p_discord_id || ':' || v_target.id::text,
    v_target.id,
    jsonb_build_object(
      'handler', 'identity_link', 'site_id', 'discord_link',
      'callback_verified', true, 'minecraft_proof_verified', true,
      'minecraft_name', v_target.name,
      'recovered', v_had_previous and v_previous.id <> v_target.id,
      'previous_player_id', case
        when v_had_previous and v_previous.id <> v_target.id then v_previous.id
        else null
      end
    )
  ) into v_inserted;

  update public.identity_link_challenges
     set recovered = v_had_previous and v_previous.id <> v_target.id,
         previous_player_id = case
           when v_had_previous and v_previous.id <> v_target.id then v_previous.id
           else null
         end
   where player_id = v_target.id;

  return query select 'complete'::text, v_target.id, v_target.mc_uuid,
    v_target.name, p_discord_id, v_inserted,
    v_had_previous and v_previous.id <> v_target.id;
end;
$$;

revoke all on function public.observance_claim_identity_handoff(text,text,text,text)
  from public, anon, authenticated;
grant execute on function public.observance_claim_identity_handoff(text,text,text,text) to service_role;

commit;
