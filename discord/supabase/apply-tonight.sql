-- ============================================================================
-- THE OBSERVANCE - apply-tonight.sql
-- DEPRECATED - DO NOT USE
-- ============================================================================
--
-- This old hand-assembled launch path was intentionally replaced by the generated
-- all-in-one bundle:
--
--   discord/supabase/apply-all.sql
--
-- Regenerate that file with:
--
--   cd discord
--   npm run db:seed
--
-- Then apply only apply-all.sql as service_role. The generated bundle carries the
-- enforced migration/seed order, the current side-quest ledger, dashboard lockdown,
-- public views, and schema repair. Keeping historical launch SQL here is more
-- dangerous than useful, because stale optional-path rows and outdated seed order
-- can leak back into prep.

do $$
begin
  raise exception
    'apply-tonight.sql is deprecated. Regenerate and apply discord/supabase/apply-all.sql instead.';
end $$;
