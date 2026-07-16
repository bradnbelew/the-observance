do $$
begin
  if (select value #>> '{}' from public.settings where key = 'v5_physical_authority_sha256') <>
     '37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b' then
    raise exception 'M2 rollback did not restore historical predicate setting';
  end if;
  if (select count(*) from public.group_observation_receipts) <> 1
     or (select count(*) from public.group_finding_receipts) <> 1
     or (select count(*) from public.protected_choice_commits) <> 1 then
    raise exception 'M2 rollback lost a receipt or protected choice';
  end if;
  if (select count(*) from public.projection_outbox_v2) <> 1 then
    raise exception 'M2 rollback lost the prior forward effect';
  end if;
end;
$$;
