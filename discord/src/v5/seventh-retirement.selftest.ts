import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const migration = readFileSync(resolve('supabase/migrations/0019_retire_superseded_seventh_runtime.sql'), 'utf8');
const rollback = readFileSync(resolve('supabase/rollbacks/0019_retire_superseded_seventh_runtime.rollback.sql'), 'utf8');
const v5 = readFileSync(resolve('supabase/seeds/v5_investigations.sql'), 'utf8');
const applyAll = readFileSync(resolve('supabase/apply-all.sql'), 'utf8');
const retired = ['seventh-shrine', 'seventh-unwriting', 'seventh-cause', 'seventh-choice', 'seventh-name'];
for (const key of retired) {
  assert.ok(migration.includes(`'${key}'`));
  assert.ok(rollback.includes(`'${key}'`));
}
assert.ok(migration.includes('set active = false'));
assert.ok(rollback.includes('set active = true'));
assert.ok(!v5.toLowerCase().includes('iss signed averyn out'));
assert.ok(!v5.toLowerCase().includes('subject averyn'));
assert.ok(v5.includes("('v5-ar08-averyn'"), 'P11 identity must remain active after retiring the stale category runtime');
assert.ok(v5.includes("('RP06'"), 'the physical final release must remain active');
assert.ok(applyAll.indexOf('-- FILE: discord/supabase/migrations/0019_retire_superseded_seventh_runtime.sql')
  > applyAll.indexOf('-- FILE: discord/supabase/seeds/v5_investigations.sql'),
'retirement must run after every legacy/V5 seed');

console.log('seventh-retirement.selftest OK: 5 stale category puzzles reversible; P11/RP06 preserved');
