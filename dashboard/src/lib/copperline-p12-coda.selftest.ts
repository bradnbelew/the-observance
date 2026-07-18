import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/2011/05/18/archive-closed/page.tsx');
const store = read('src/lib/arg-event-store.ts');
const index = read('src/app/community/index.php/page.tsx');

assert.ok(page.includes("hasCampaignEvent('p12.record_closed_averyn_released')"));
assert.ok(page.includes("latestCampaignEventPayload('p12.record_closed_averyn_released')"));
assert.ok(page.includes('Both authorized name treatments close the Record and release Averyn'));
assert.ok(page.includes('Earlier posts and attachment versions remain unchanged'));
for (const field of ['name_treatment', 'wren_remembrance', 'manifest_sha256']) assert.ok(page.includes(field));
for (const event of [
  'p4.control_reversal_earned',
  'p5.civic_gallery_recurated',
  'p7.nessa_publicly_cleared',
  'p8.hold_systems_repaired',
  'p9.company_biographies_restored',
  'p10.player_copy_proof',
  'p11.averyn_restored_unbound',
]) assert.ok(page.includes(`hasCampaignEvent('${event}')`), `coda does not read ${event}`);
for (const person of ['Aro', 'Wenna', 'Coll', 'Dob', 'Pell']) assert.ok(page.includes(person), `missing resident coda for ${person}`);
for (const outcome of ['condemn', 'understand', 'free']) assert.ok(page.includes(`value === '${outcome}'`), `missing Wren ${outcome} aftermath`);
assert.ok(page.includes('The test proves copying behavior without naming what the Dark is.'));
assert.ok(store.includes('export async function latestCampaignEventPayload'));
assert.ok(index.includes('/community/2011/05/18/archive-closed'));

console.log('Copperline P12 release/coda selftest: PASS');
