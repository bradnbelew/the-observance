-- M2 FORWARD-RECOVERY PROPOSAL. Re-activate retained rows; never duplicate effects.
begin;
do $$
begin
  if not exists (select 1 from public.predicate_authority_versions
    where raw_sha256 = '16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a'
      and semantic_sha256 = 'd2eec35f58cf79a30f2255f429cb0d19a5c1e8b5bd7942604b3bef724272cbf6') then
    raise exception 'M2 predicate bytes/semantic receipt missing';
  end if;
  if exists (select 1 from public.protected_choice_commits group by group_id, choice_id having count(*) > 1) then
    raise exception 'duplicate protected choice receipt';
  end if;
end;
$$;
update public.predicate_authority_versions set status = 'retired'
where raw_sha256 = '37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b';
update public.predicate_authority_versions set status = 'active', activated_at = now()
where raw_sha256 = '16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a';
update public.settings set
  value = to_jsonb('16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a'::text),
  updated_at = now()
where key = 'v5_physical_authority_sha256';
insert into public.projection_outbox_v2 (group_id, event_type, payload, idempotency_key)
select group_id, 'forward_reconcile', jsonb_build_object('through_sequence', max(local_sequence)),
       'm2:forward:' || group_id::text || ':' || max(local_sequence)::text
from public.group_finding_receipts group by group_id
on conflict (idempotency_key) do nothing;
commit;
