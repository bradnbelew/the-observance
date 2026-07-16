begin;
create extension if not exists pgtap with schema extensions;
select no_plan();

select ok(
  not exists (
    select 1 from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public'
      and c.relname = any(array[
        'campaign_manifest_versions','predicate_authority_versions','campaign_groups','campaign_group_members',
        'group_observation_receipts','group_finding_receipts','finding_contributors','artifact_custody',
        'protected_choice_commits','hint_requests_v2','hint_approvals_v2','hint_deliveries_v2',
        'media_reveal_receipts_v2','projection_outbox_v2','reconciliation_receipts_v2',
        'legacy_import_receipts_v2','release_parity_receipts_v2'
      ]) and (not c.relrowsecurity or not c.relforcerowsecurity)
  ),
  'every M2 public table enables and forces RLS'
);

select ok(
  not exists (
    select 1 from unnest(array[
      'campaign_manifest_versions','predicate_authority_versions','campaign_groups','campaign_group_members',
      'group_observation_receipts','group_finding_receipts','finding_contributors','artifact_custody',
      'protected_choice_commits','hint_requests_v2','hint_approvals_v2','hint_deliveries_v2',
      'media_reveal_receipts_v2','projection_outbox_v2','reconciliation_receipts_v2',
      'legacy_import_receipts_v2','release_parity_receipts_v2'
    ]) table_name
    where has_table_privilege('anon', 'public.' || table_name, 'SELECT')
       or has_table_privilege('anon', 'public.' || table_name, 'INSERT')
       or has_table_privilege('anon', 'public.' || table_name, 'UPDATE')
       or has_table_privilege('anon', 'public.' || table_name, 'DELETE')
       or has_table_privilege('authenticated', 'public.' || table_name, 'SELECT')
       or has_table_privilege('authenticated', 'public.' || table_name, 'INSERT')
       or has_table_privilege('authenticated', 'public.' || table_name, 'UPDATE')
       or has_table_privilege('authenticated', 'public.' || table_name, 'DELETE')
  ),
  'anon and authenticated retain no M2 table privileges'
);

select ok(
  not exists (
    select 1 from unnest(array[
      'campaign_manifest_versions','predicate_authority_versions','campaign_groups','campaign_group_members',
      'group_observation_receipts','group_finding_receipts','finding_contributors','artifact_custody',
      'protected_choice_commits','hint_requests_v2','hint_approvals_v2','hint_deliveries_v2',
      'media_reveal_receipts_v2','projection_outbox_v2','reconciliation_receipts_v2',
      'legacy_import_receipts_v2','release_parity_receipts_v2'
    ]) table_name
    where not has_table_privilege('service_role', 'public.' || table_name, 'SELECT')
       or not has_table_privilege('service_role', 'public.' || table_name, 'INSERT')
       or not has_table_privilege('service_role', 'public.' || table_name, 'UPDATE')
       or has_table_privilege('service_role', 'public.' || table_name, 'DELETE')
  ),
  'service_role has select/insert/update and no delete privilege'
);

set local role anon;
select throws_ok(
  $$select * from public.group_finding_receipts$$,
  '42501', null, 'anon cannot read finding receipts'
);
reset role;

set local role authenticated;
select throws_ok(
  $$insert into public.protected_choice_commits (
      group_id, choice_id, choice_value, player_confirmation_receipt,
      local_sequence, local_state_sha256, idempotency_key, committed_at
    ) values (
      '10000000-0000-0000-0000-000000000001', 'wren_remembrance', 'free', 'forbidden',
      3, repeat('c',64), 'm2:test:forbidden', now()
    )$$,
  '42501', null, 'authenticated cannot insert protected choices'
);
reset role;

set local role service_role;
select throws_ok(
  $$update public.group_finding_receipts set local_sequence = 99
    where receipt_id = '30000000-0000-0000-0000-000000000001'$$,
  'P0001', null, 'finding receipts are append-only for service_role'
);
select throws_ok(
  $$update public.protected_choice_commits set choice_value = 'free'
    where group_id = '10000000-0000-0000-0000-000000000001'
      and choice_id = 'wren_remembrance'$$,
  'P0001', null, 'protected choices are append-only for service_role'
);
select throws_ok(
  $$insert into public.group_observation_receipts (
      group_id, observation_id, source_surface, source_version, source_receipt_key,
      provenance, idempotency_key, observed_at
    ) values (
      '10000000-0000-0000-0000-000000000001', 'P1.O.DUP', 'recovery', 'm2-local',
      'source:local:duplicate', '{}'::jsonb, 'm2:test:observation:1', now()
    )$$,
  '23505', null, 'idempotency key rejects duplicate observation effects'
);
reset role;

select is(
  (select count(*) from public.projection_outbox_v2
   where idempotency_key = 'm2:forward:10000000-0000-0000-0000-000000000001:1'),
  1::bigint,
  'rollback plus repeated forward recovery creates one projection effect'
);
select is(
  (select semantic_sha256 from public.predicate_authority_versions
   where raw_sha256 = '16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a'),
  'd2eec35f58cf79a30f2255f429cb0d19a5c1e8b5bd7942604b3bef724272cbf6',
  'LF predicate preserves the canonical semantic authority hash'
);

select * from finish();
rollback;
