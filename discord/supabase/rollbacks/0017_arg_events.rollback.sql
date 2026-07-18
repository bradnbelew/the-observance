-- Development/staging rollback only. Do not run against production without an approved preflight.
begin;
drop function if exists public.observance_record_arg_event(text,text,text,text,jsonb);
drop table if exists public.arg_event_projections;
drop table if exists public.arg_events;
drop table if exists public.arg_event_definitions;
commit;
