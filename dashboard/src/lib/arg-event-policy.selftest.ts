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
check(ARG_EVENT_DEFINITIONS['p4.control_reversal_earned'].prerequisites.length === 1
  && ARG_EVENT_DEFINITIONS['p4.control_reversal_earned'].prerequisites[0] === 'p3.dispatch_authorized',
  'a correct P4 conclusion must not require revision/test/source interactions');
check(migration.includes("('p4.control_reversal_earned','P4','{p3.dispatch_authorized}'"),
  'database P4 correctness must match zero-source-touch policy');
check(!migration.includes("('p4.control_reversal_earned','P4','{p4.copy_hypothesis_tested}'"),
  'database must not reintroduce expected-test gating');
check(ARG_EVENT_DEFINITIONS['p7.nessa_publicly_cleared'].prerequisites[0]
  === 'p6.six_responsibilities_acknowledged',
  'P7 correct exoneration must not require material/source observation events');
check(!migration.includes("('p7.nessa_publicly_cleared','P7','{p7.supplier_history_restored}'"),
  'database must not make the supplier restore a prerequisite for correct exoneration');
check(ARG_EVENT_DEFINITIONS['p8.intervention_plan_accepted'].prerequisites[0]
  === 'p7.nessa_publicly_cleared',
  'P8 correct causal model must accept zero observation/source receipts after phase entry');
check(ARG_EVENT_DEFINITIONS['p8.intervention_plan_accepted'].sourceSurfaces.includes('copperline')
  && !ARG_EVENT_DEFINITIONS['p8.intervention_plan_accepted'].sourceSurfaces.includes('discord'),
  'P8 must use the semantic Copperline plan or local Minecraft path, not a Discord answer menu');
check(ARG_EVENT_DEFINITIONS['p8.hold_systems_repaired'].prerequisites[0]
  === 'p8.intervention_plan_accepted',
  'P8 physical repair must project only after a bounded safe intervention plan');
check(ARG_EVENT_DEFINITIONS['p9.leak_window_proven'].sourceSurfaces.includes('copperline')
  && !ARG_EVENT_DEFINITIONS['p9.leak_window_proven'].sourceSurfaces.includes('discord'),
  'P9 must preserve the private version chain on Copperline or the local Minecraft path, not a Discord answer menu');
check(ARG_EVENT_DEFINITIONS['p10.player_copy_proof'].sourceSurfaces.length === 1
  && ARG_EVENT_DEFINITIONS['p10.player_copy_proof'].sourceSurfaces[0] === 'minecraft',
  'bounded player copy proof must be owned by its physical Paper predicate');
check(ARG_EVENT_DEFINITIONS['p10.player_copy_proof'].projectionSurfaces.includes('copperline'),
  'the bounded player copy must cause an authored Copperline response');
check(migration.includes("('p10.player_copy_proof','P10','{p9.leak_window_proven}','{minecraft}','{minecraft,copperline,discord,dashboard}')"),
  'database must carry the exact bounded player-copy event and projection surfaces');
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
