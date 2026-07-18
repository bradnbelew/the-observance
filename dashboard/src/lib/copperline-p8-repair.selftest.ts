import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/2011/04/02/hold-works/page.tsx');
const index = read('src/app/community/index.php/page.tsx');

assert.ok(page.includes("hasCampaignEvent('p8.intervention_plan_accepted')"));
assert.ok(page.includes("hasCampaignEvent('p8.hold_systems_repaired')"));
assert.ok(page.includes('old fracture, unchanged heat load, empty paired watch, and late-routed closure requests'));
assert.ok(page.includes("Iss's cut widened the failure") || page.includes("Iss&apos;s cut widened the failure"));
assert.ok(page.includes('Do not erase the altered office'));
assert.ok(page.includes("group's intervention") || page.includes("group&apos;s intervention"));
assert.ok(index.includes('/community/2011/04/02/hold-works'));

console.log('Copperline P8 intervention/repair consequence selftest: PASS');
