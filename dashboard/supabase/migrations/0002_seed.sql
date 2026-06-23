-- The Observance — dashboard schema
-- 0002_seed.sql
--
-- Idempotent seed of control/status rows the dashboard and plugin both depend on.
-- Safe to re-run: every insert is ON CONFLICT DO NOTHING so existing live state
-- (e.g. an in-progress arc, a toggled watcher_sleep) is never clobbered.

begin;

-- Settings the spoiler-free health view + control surface read/write.
insert into public.settings (key, value) values
  ('watcher_sleep',  'false'::jsonb),
  ('api_status',     '"unknown"'::jsonb),
  ('whisper_status', '"ok"'::jsonb)
on conflict (key) do nothing;

-- The single arc_state row (id = 1, enforced by the check constraint).
insert into public.arc_state (id, current_act, gates, flags) values
  (1, 1, '{}'::jsonb, '{}'::jsonb)
on conflict (id) do nothing;

commit;
