import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { ARG_EVENT_DEFINITIONS } from './arg-event-policy';

function check(condition: boolean, message: string): void {
  if (!condition) throw new Error(`ARG event-policy self-test failed: ${message}`);
}

const repo = resolve(process.cwd(), '..');
const migration = readFileSync(resolve(repo, 'discord/supabase/migrations/0017_arg_events.sql'), 'utf8');
const rollback = readFileSync(resolve(repo, 'discord/supabase/rollbacks/0017_arg_events.rollback.sql'), 'utf8');
const seeded = new Set([...migration.matchAll(/\('(?<key>p(?:1[0-2]|[1-9])\.[a-z0-9_]+)'\s*,\s*'P/g)]
  .map((match) => match.groups?.key).filter((key): key is string => Boolean(key)));
const authored = Object.keys(ARG_EVENT_DEFINITIONS);

check(seeded.size === authored.length, `migration has ${seeded.size} events; policy has ${authored.length}`);
for (const key of authored) check(seeded.has(key), `migration is missing ${key}`);
check(!/evidence_receipts|observation_receipts|source_receipts/i.test(migration),
  'knowledge/action acceptance must not depend on source-touch receipts');
for (const table of ['arg_event_definitions', 'arg_events', 'arg_event_projections']) {
  check(migration.includes(`alter table public.${table} enable row level security`), `${table} must enable RLS`);
  check(migration.includes(`revoke all on public.${table} from public, anon, authenticated`),
    `${table} must explicitly revoke Data API roles`);
}
check(migration.includes('security definer') && migration.includes('set search_path = public, pg_temp'),
  'event RPC needs a fixed-path security definer');
check(migration.includes('to service_role') && !migration.includes('grant execute on function public.observance_record_arg_event(text,text,text,text,jsonb)\n  to authenticated'),
  'event RPC must remain server-only');
check(rollback.includes('drop function if exists public.observance_record_arg_event'),
  'staging rollback must remove the RPC first');
check(authored.every((key) => ARG_EVENT_DEFINITIONS[key as keyof typeof ARG_EVENT_DEFINITIONS].automation === 'A1'),
  'automatic authored consequences may not exceed A1');

console.log(`arg-event-policy.selftest OK: ${authored.length} open-ended story events, RLS/grants/RPC/rollback parity`);
