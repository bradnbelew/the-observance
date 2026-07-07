import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const bundle = resolve(here, '../../supabase/apply-all.sql');

const expected = [
  'dashboard/supabase/migrations/0001_init.sql',
  'dashboard/supabase/migrations/0002_seed.sql',
  'dashboard/supabase/migrations/0003_lockdown.sql',
  'discord/supabase/migrations/0003_discord.sql',
  'discord/supabase/migrations/0004_oracle.sql',
  'discord/supabase/migrations/0005_threads.sql',
  'discord/supabase/migrations/0006_requires_flags.sql',
  'discord/supabase/migrations/0007_answer_kind.sql',
  'discord/supabase/migrations/0008_requires_quorum.sql',
  'discord/supabase/migrations/0009_observations.sql',
  'discord/supabase/migrations/0010_answer_attempts_web_rate_limit.sql',
  'discord/supabase/migrations/0011_showrunner_lock.sql',
  'discord/supabase/migrations/0012_world_paste_ledger.sql',
  'dashboard/supabase/migrations/0004_v_record.sql',
  'dashboard/supabase/migrations/0005_reconcile_tracker_views.sql',
  'dashboard/supabase/migrations/0006_v_record_theories.sql',
  'dashboard/supabase/migrations/0007_v_archive.sql',
  'dashboard/supabase/migrations/0008_v_archive_flag_gate.sql',
  'dashboard/supabase/migrations/0009_beat_queue_failed_status.sql',
  'discord/supabase/seeds/puzzles_seed.sql',
  'discord/supabase/seeds/seventh_seed.sql',
  'discord/supabase/seeds/thread_tags.sql',
  'discord/supabase/seeds/thread_cards.sql',
  'discord/supabase/seeds/side_quests.sql',
  'discord/supabase/seeds/hints_seed.sql',
  'discord/supabase/seeds/metapuzzle_seed.sql',
  'discord/supabase/seeds/progression_seed.sql',
  'discord/supabase/schema-repair.sql',
];

const sql = readFileSync(bundle, 'utf8');
const markers = [...sql.matchAll(/^-- FILE: (.+)$/gm)].map((m) => m[1]);

function fail(message: string): never {
  console.error(`apply-all selftest FAILED: ${message}`);
  process.exit(1);
}

if (markers.length !== expected.length) {
  fail(`expected ${expected.length} file markers, found ${markers.length}`);
}

for (let i = 0; i < expected.length; i += 1) {
  if (markers[i] !== expected[i]) {
    fail(`marker ${i + 1} should be ${expected[i]}, found ${markers[i] ?? '<missing>'}`);
  }
}

for (const key of [
  'puzzles.requires_flags',
  'puzzles.answer_kind',
  'puzzles.requires_quorum',
  '0009_observations.sql',
  '0010_answer_attempts_web_rate_limit.sql',
  '0011_showrunner_lock.sql',
  '0012_world_paste_ledger.sql',
  'world_paste_ledger',
  '0009_beat_queue_failed_status.sql',
]) {
  if (!sql.includes(key)) {
    fail(`bundle is missing launch-critical text: ${key}`);
  }
}

console.log(`apply-all selftest OK (${markers.length} ordered files)`);
