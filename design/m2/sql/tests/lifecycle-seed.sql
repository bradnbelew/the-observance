insert into public.campaign_groups (group_id, stable_key, manifest_version)
values ('10000000-0000-0000-0000-000000000001', 'm2-local-validation', '2.0.0-m2');

insert into public.group_observation_receipts (
  receipt_id, group_id, observation_id, source_surface, source_version,
  source_receipt_key, provenance, idempotency_key, observed_at
) values (
  '20000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000001',
  'P1.O.LOCAL', 'recovery', 'm2-local', 'source:local:1',
  '{"target":"disposable-local"}'::jsonb, 'm2:test:observation:1', now()
);

insert into public.group_finding_receipts (
  receipt_id, group_id, finding_id, manifest_version, predicate_raw_sha256,
  observation_receipt_ids, local_sequence, local_state_sha256, idempotency_key, committed_at
) values (
  '30000000-0000-0000-0000-000000000001',
  '10000000-0000-0000-0000-000000000001',
  'P1.F.LOCAL', '2.0.0-m2',
  '16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a',
  array['20000000-0000-0000-0000-000000000001'::uuid], 1,
  repeat('a', 64), 'm2:test:finding:1', now()
);

insert into public.protected_choice_commits (
  group_id, choice_id, choice_value, player_confirmation_receipt,
  local_sequence, local_state_sha256, idempotency_key, committed_at
) values (
  '10000000-0000-0000-0000-000000000001',
  'wren_remembrance', 'understand', 'm2-local-player-confirmation',
  2, repeat('b', 64), 'm2:test:choice:1', now()
);
