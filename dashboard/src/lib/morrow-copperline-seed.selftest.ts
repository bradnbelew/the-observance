import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { MORROW_SEED, recoveryLookup } from './morrow-copperline-seed';

assert.equal(MORROW_SEED.alias, 'iona_bell');
assert.equal(MORROW_SEED.recoveryKey.length, 8);
assert.equal(MORROW_SEED.uptime, '04:17:22');
assert.equal(recoveryLookup().state, 'empty');
assert.equal(recoveryLookup('wrong', '0417iona').state, 'alias');
assert.equal(recoveryLookup('iona_bell', 'badkey').state, 'key');
assert.equal(recoveryLookup('iona_bell', '0417iona').state, 'open');

const gameServers = readFileSync(resolve('src/app/game-servers.php/page.tsx'), 'utf8');
assert.ok(gameServers.includes('archive:path='));
assert.ok(gameServers.includes('sourceCommentPath'));
assert.ok(gameServers.includes('productSlug'));
assert.ok(gameServers.includes('footerBuild'));

for (const route of [
  'src/app/community/forum/page.tsx',
  'src/app/status/incident-6118/page.tsx',
  'src/app/support/tickets/6118/page.tsx',
  'src/app/recovery/mossfield/page.tsx',
]) {
  const source = readFileSync(resolve(route), 'utf8');
  assert.equal(source.includes('createClient('), false, `${route} must not use Supabase auth`);
  assert.equal(source.includes('SUPABASE_SERVICE_ROLE_KEY'), false, `${route} must not expose service keys`);
  assert.equal(source.includes('fetch('), false, `${route} must remain local/static`);
}

const recovery = readFileSync(resolve('src/app/recovery/mossfield/page.tsx'), 'utf8');
assert.ok(recovery.includes('caseFingerprint'));
assert.ok(recovery.includes('handoffAddress'));
assert.ok(recovery.includes('morrow-reboot.enabled remains false'));

console.log('MORROW COPPERLINE SEED: PASS g01-g03 routes/local-lookup/static-boundary');


