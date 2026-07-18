import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/archive/ash-camp/page.tsx');
const form = read('src/app/community/archive/ash-camp/CampBiographyForm.tsx');
const leakForm = read('src/app/community/archive/ash-camp/LeakWindowForm.tsx');
const action = read('src/app/community/archive/ash-camp/actions.ts');
const index = read('src/app/community/index.php/page.tsx');

assert.ok(page.includes("hasCampaignEvent('p8.hold_systems_repaired')"));
assert.ok(page.includes("hasCampaignEvent('p9.company_biographies_restored')"));
assert.ok(page.includes("hasCampaignEvent('p9.leak_window_proven')"));
for (const person of ['mkept', 'Ash', 'Rook', 'Wren']) assert.ok(page.includes(person));
assert.ok(form.includes('<select name={person}') && form.includes('This records responsibilities'));
assert.ok(action.includes("eventKey: 'p9.company_biographies_restored'"));
assert.ok(action.includes("eventKey: 'p9.leak_window_proven'"));
assert.ok(action.includes("idempotencyKey: 'copperline:p9:private-version-chain-v1'"));
assert.ok(action.includes("['rook-private-countermark', 'witness-spool-intake', 'public-upload']"));
assert.ok(action.includes('observation_receipts: 0'));
assert.ok(leakForm.includes('<form action={action}') && leakForm.includes('aria-live="polite"'));
assert.ok(leakForm.includes('someone with inside access transmitted it; these copies do not name who'));
assert.ok(page.includes('Copies from separate clocks') && page.includes('10:19 PM') && page.includes('10:23 PM') && page.includes('11:02 PM'));
assert.ok(page.includes('P9 does not name which person sent it'));
assert.ok(index.includes('/community/archive/ash-camp'));

console.log('Copperline P9 people/version-chain selftest: PASS');
