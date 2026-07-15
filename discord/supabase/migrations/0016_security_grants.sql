begin;

-- Trigger helpers run through their owning triggers and should not be callable
-- directly by public API roles. A fixed search path also prevents object-shadowing.
alter function public.set_updated_at() set search_path = public, pg_temp;
revoke execute on function public.set_updated_at() from public, anon, authenticated;

-- These functions mutate campaign-wide state. Only the server-side Discord
-- runtime may invoke them; players use the narrower evidence/identity RPCs.
revoke execute on function public.observance_merge_arc_flags(jsonb)
  from public, anon, authenticated;
revoke execute on function public.showrunner_try_acquire_lock(integer)
  from public, anon, authenticated;
revoke execute on function public.showrunner_release_lock()
  from public, anon, authenticated;

grant execute on function public.observance_merge_arc_flags(jsonb)
  to service_role;
grant execute on function public.showrunner_try_acquire_lock(integer)
  to service_role;
grant execute on function public.showrunner_release_lock()
  to service_role;

commit;
