-- The Observance V5 post-migration verification. Read-only and safe to rerun.
with checks(name, expected, actual) as (
  select 'active cases', 10::bigint, count(*) from public.investigations where active and required
  union all select 'active nodes', 82, count(*) from public.investigation_nodes where active and required
  union all select 'case node budget', 82, coalesce(sum(expected_nodes), 0) from public.investigations where active and required
  union all select 'required media', 5, count(*) from public.required_media where active
  union all select 'active V5 puzzles', 15, count(*) from public.puzzles where active and puzzle_key like 'v5-%'
  union all select 'all active puzzles', 15, count(*) from public.puzzles where active
  union all select 'active hints', 45, count(*) from public.hints where active
  union all select 'active legacy thread cards', 0, count(*) from public.thread_cards where active
  union all select 'active legacy side quests', 0, count(*) from public.side_quests where active
  union all select 'retirement marker', 1, count(*) from public.settings where key = 'v5_queue_retirement_complete'
  union all
  select 'per-case budget mismatches', 0, count(*) from (
    select i.case_key
    from public.investigations i
    left join public.investigation_nodes n
      on n.case_key = i.case_key and n.active and n.required
    where i.active and i.required
    group by i.case_key, i.expected_nodes
    having count(n.node_key) <> i.expected_nodes
  ) bad
  union all
  select 'missing runtime metadata', 0, count(*)
  from public.investigation_nodes
  where active and required
    and (
      metadata->>'runtime_owner' is null
      or metadata->>'handler' is null
      or metadata->>'site_id' is null
      or metadata->>'replay_policy' is null
    )
  union all select 'evidence RPC exists', 1,
    (to_regprocedure('public.observance_record_evidence(text,text,text,text,uuid,jsonb)') is not null)::int
  union all select 'identity challenge table exists', 1,
    (to_regclass('public.identity_link_challenges') is not null)::int
  union all select 'identity issue RPC exists', 1,
    (to_regprocedure('public.observance_issue_identity_link_challenge(text,text)') is not null)::int
  union all select 'proof-bound identity RPC exists', 1,
    (to_regprocedure('public.observance_claim_identity_handoff(text,text,text,text)') is not null)::int
  union all select 'claim-first identity RPC absent', 1,
    (to_regprocedure('public.observance_claim_identity_handoff(text,text,text)') is null)::int
  union all select 'challenge plaintext columns absent', 0,
    count(*) from information_schema.columns
    where table_schema = 'public'
      and table_name = 'identity_link_challenges'
      and column_name in ('code', 'proof', 'plaintext_code')
  union all select 'record view exists', 1, (to_regclass('public.v_record') is not null)::int
  union all select 'case view exists', 1, (to_regclass('public.v_case_progress') is not null)::int
  union all select 'archive view exists', 1, (to_regclass('public.v_archive') is not null)::int
  union all select 'media view exists', 1,
    (to_regclass('public.v_required_media_delivery') is not null)::int
  union all select 'anon base-table SELECT denied', 1,
    (not has_table_privilege('anon', 'public.investigations', 'select'))::int
  union all select 'anon record-view SELECT allowed', 1,
    has_table_privilege('anon', 'public.v_record', 'select')::int
  union all select 'anon evidence-RPC EXECUTE denied', 1,
    (not has_function_privilege(
      'anon',
      'public.observance_record_evidence(text,text,text,text,uuid,jsonb)',
      'execute'
    ))::int
  union all select 'anon identity challenge SELECT denied', 1,
    (not has_table_privilege('anon', 'public.identity_link_challenges', 'select'))::int
  union all select 'authenticated identity challenge SELECT denied', 1,
    (not has_table_privilege('authenticated', 'public.identity_link_challenges', 'select'))::int
  union all select 'anon identity issue-RPC EXECUTE denied', 1,
    (not has_function_privilege(
      'anon',
      'public.observance_issue_identity_link_challenge(text,text)',
      'execute'
    ))::int
  union all select 'anon proof-bound identity-RPC EXECUTE denied', 1,
    (not has_function_privilege(
      'anon',
      'public.observance_claim_identity_handoff(text,text,text,text)',
      'execute'
    ))::int
  union all select 'service evidence-RPC EXECUTE allowed', 1,
    has_function_privilege(
      'service_role',
      'public.observance_record_evidence(text,text,text,text,uuid,jsonb)',
      'execute'
    )::int
  union all select 'service identity issue-RPC EXECUTE allowed', 1,
    has_function_privilege(
      'service_role',
      'public.observance_issue_identity_link_challenge(text,text)',
      'execute'
    )::int
  union all select 'service proof-bound identity-RPC EXECUTE allowed', 1,
    has_function_privilege(
      'service_role',
      'public.observance_claim_identity_handoff(text,text,text,text)',
      'execute'
    )::int
)
select name, expected, actual, expected = actual as ok
from checks
order by name;
