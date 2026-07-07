-- 0009_beat_queue_failed_status.sql - allow plugin runtime beat statuses.
--
-- The plugin claims an approved beat by moving it to status='firing' before world mutation, then writes
-- a terminal status ('fired', 'skipped', or 'failed') afterward. The beat_queue table is born in the
-- dashboard lineage, while the plugin status contract is maintained in the Discord lineage, so keep this
-- dashboard-side repair in sync.

alter table if exists public.beat_queue
  drop constraint if exists beat_queue_status_check;

alter table if exists public.beat_queue
  add constraint beat_queue_status_check
  check (status in ('pending', 'approved', 'firing', 'skipped', 'fired', 'failed'));
