do $$
begin
  if (select count(*) from public.predicate_authority_versions where status = 'active'
      and raw_sha256 = '16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a') <> 1 then
    raise exception 'M2 recovery did not restore the LF predicate';
  end if;
  if (select count(*) from public.predicate_authority_versions
      where semantic_sha256 = 'd2eec35f58cf79a30f2255f429cb0d19a5c1e8b5bd7942604b3bef724272cbf6') <> 2 then
    raise exception 'M2 predicate raw/semantic chain is incomplete';
  end if;
  if (select count(*) from public.projection_outbox_v2 where idempotency_key =
      'm2:forward:10000000-0000-0000-0000-000000000001:1') <> 1 then
    raise exception 'M2 repeated forward recovery duplicated its effect';
  end if;
  if (select count(*) from public.group_observation_receipts) <> 1
     or (select count(*) from public.group_finding_receipts) <> 1
     or (select count(*) from public.protected_choice_commits) <> 1 then
    raise exception 'M2 recovery changed preserved receipt counts';
  end if;
end;
$$;
