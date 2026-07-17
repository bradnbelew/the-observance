-- Exact non-destructive rollback for production-security-hardening-v2.up.sql.
begin;

do $$
begin
  if to_regclass('observance_migration.security_hardening_v2_grants') is null
     or to_regclass('observance_migration.security_hardening_v2_indexes') is null then
    raise exception 'security hardening rollback snapshot is missing';
  end if;
end;
$$;

revoke all privileges on all tables in schema public from anon, authenticated;
revoke usage, select on all sequences in schema public from anon, authenticated;

do $$
declare
  grant_row record;
  index_row record;
begin
  for grant_row in
    select grantee, table_schema, table_name, privilege_type
    from observance_migration.security_hardening_v2_grants
  loop
    execute format(
      'grant %s on table %I.%I to %I',
      grant_row.privilege_type, grant_row.table_schema, grant_row.table_name, grant_row.grantee
    );
  end loop;
  for index_row in
    select index_name from observance_migration.security_hardening_v2_indexes
    where not existed_before
  loop
    execute format('drop index if exists public.%I', index_row.index_name);
  end loop;
end;
$$;

alter view public.v_archive set (security_invoker = false);
alter view public.v_required_media_delivery set (security_invoker = false);
alter view public.v_case_progress set (security_invoker = false);
alter view public.v_heatmap set (security_invoker = false);
alter view public.v_compliance_counts set (security_invoker = false);
alter view public.v_health set (security_invoker = false);
alter view public.v_record set (security_invoker = false);

alter table public.dossiers drop constraint dossiers_pkey;
alter table public.dossiers drop column id;

drop table observance_migration.security_hardening_v2_indexes;
drop table observance_migration.security_hardening_v2_grants;
drop schema observance_migration;

commit;
