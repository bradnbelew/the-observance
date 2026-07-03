-- 0009_beat_queue_failed_status.sql — allow the terminal 'failed' beat status (cross-surface fix).
--
-- The plugin's BeatQueuePoller writes status='failed' when an enactor throws (a terminal status, so the
-- beat leaves the queue instead of being re-served every poll). That value was added to the beat_queue
-- status CHECK only in the DISCORD migration lineage (discord/0004_oracle.sql). The table itself is
-- created here in the dashboard lineage (0001_init.sql) WITHOUT 'failed'. The two lineages are numbered
-- independently but target ONE shared DB — so if the discord widening is skipped when provisioning, the
-- plugin's 'failed' write is rejected by the CHECK, the beat stays 'approved', and fetchActionableBeats
-- re-serves it every poll → a beat whose enactor throws re-fires forever.
--
-- This migration makes the constraint correct in the dashboard lineage too. Fully IDEMPOTENT + order-
-- independent: drop the constraint if present, re-add it with the full status set. Safe to apply whether
-- or not discord/0004 already widened it (dropping a same-named constraint and re-adding an identical one
-- is a no-op in effect), and safe on a fresh DB.

alter table if exists public.beat_queue
  drop constraint if exists beat_queue_status_check;

alter table if exists public.beat_queue
  add constraint beat_queue_status_check
  check (status in ('pending', 'approved', 'skipped', 'fired', 'failed'));
