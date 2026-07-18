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
assert.ok(store.includes('export async function latestCampaignEventPayload'));
assert.ok(index.includes('/community/2011/05/18/archive-closed'));

console.log('Copperline P12 release/coda selftest: PASS');
