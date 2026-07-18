import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/2011/04/02/hold-works/page.tsx');
const action = read('src/app/community/2011/04/02/hold-works/actions.ts');
const form = read('src/app/community/2011/04/02/hold-works/InterventionPlanForm.tsx');
const index = read('src/app/community/index.php/page.tsx');

assert.ok(page.includes("hasCampaignEvent('p7.nessa_publicly_cleared')"));
assert.ok(page.includes("hasCampaignEvent('p8.intervention_plan_accepted')"));
assert.ok(page.includes("hasCampaignEvent('p8.unlit_house_synthesis_completed')"));
assert.ok(page.includes("hasCampaignEvent('p8.hold_systems_repaired')"));
assert.ok(page.includes('<InterventionPlanForm planned={planned === true} />'));
assert.ok(page.includes('old fracture, unchanged heat load, empty paired watch, and late-routed closure requests'));
assert.ok(page.includes("Iss's cut widened the failure") || page.includes("Iss&apos;s cut widened the failure"));
assert.ok(page.includes('Do not erase the altered office'));
assert.ok(page.includes('Base comparison received'));
assert.ok(page.includes('seven separate records instead of merging them into one neat account'));
assert.ok(page.includes("Nothing in this attachment replaces the group's causal finding") || page.includes("Nothing in this attachment replaces the group&apos;s causal finding"));
assert.ok(page.includes("group's intervention") || page.includes("group&apos;s intervention"));
assert.ok(index.includes('/community/2011/04/02/hold-works'));
assert.ok(index.includes("hasCampaignEvent('p8.unlit_house_synthesis_completed')"));
assert.ok(index.includes('the Hold works: base comparison attached'));
assert.ok(action.includes("eventKey: 'p8.intervention_plan_accepted'"));
assert.ok(action.includes("source: 'copperline'"));
assert.ok(action.includes('P8_INTERVENTION_CANONICAL_PAYLOAD'));
assert.ok(action.includes("requestHeaders.get('origin')"));
assert.ok(form.includes('<form action={action}'));
assert.ok(form.includes('aria-live="polite"'));
assert.ok(form.includes('This form does not require one exact sentence.'));

console.log('Copperline P8 intervention/repair consequence selftest: PASS');
