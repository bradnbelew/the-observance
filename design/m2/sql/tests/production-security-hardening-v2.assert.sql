-- Run after UP or forward recovery on a disposable/development target.
do $$
begin
  if exists (
    select 1 from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind in ('r','p') and not c.relrowsecurity
  ) then raise exception 'RLS missing on an exposed public table'; end if;
  if exists (
    select 1 from pg_class c join pg_namespace n on n.oid = c.relnamespace
    where n.nspname = 'public' and c.relkind = 'v'
      and coalesce(c.reloptions, '{}') @> array['security_invoker=false']
  ) then raise exception 'public SECURITY DEFINER view remains'; end if;
  if exists (
    select 1 from information_schema.role_table_grants
    where table_schema = 'public' and grantee in ('anon','authenticated')
  ) then raise exception 'unexpected browser Data API grant remains'; end if;
  if not exists (
    select 1 from pg_constraint
    where conrelid = 'public.dossiers'::regclass and contype = 'p'
  ) then raise exception 'dossiers has no primary key'; end if;
  if exists (
    select 1
    from pg_constraint fk
    join pg_class tbl on tbl.oid = fk.conrelid
    join pg_namespace ns on ns.oid = tbl.relnamespace
    where fk.contype = 'f' and ns.nspname = 'public'
      and not exists (
        select 1 from pg_index idx
        where idx.indrelid = fk.conrelid
          and (idx.indkey::smallint[])[0:cardinality(fk.conkey)-1] = fk.conkey
      )
  ) then raise exception 'an unindexed public foreign key remains'; end if;
end;
$$;
