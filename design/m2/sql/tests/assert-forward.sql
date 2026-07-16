do $$
begin
  if (select value #>> '{}' from public.settings where key = 'v5_physical_authority_sha256') <>
     '16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a' then
    raise exception 'M2 forward did not activate LF predicate setting';
  end if;
  if (select count(*) from public.predicate_authority_versions where status = 'active') <> 1 then
    raise exception 'M2 forward must leave exactly one active predicate';
  end if;
  if (select count(*) from public.projection_outbox_v2 where idempotency_key =
      'm2:forward:10000000-0000-0000-0000-000000000001:1') <> 1 then
    raise exception 'M2 forward must enqueue one reconciliation effect';
  end if;
end;
$$;
