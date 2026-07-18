import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const supplierPage = read('src/app/community/archive/supplier-revisions/page.tsx');
const supplierAction = read('src/app/community/archive/supplier-revisions/actions.ts');
const correction = read('src/app/community/2011/03/14/nessa-correction/page.tsx');
const index = read('src/app/community/index.php/page.tsx');

assert.ok(supplierPage.includes("hasCampaignEvent('p7.counterfeit_material_proven')"));
assert.ok(supplierAction.includes("eventKey: 'p7.supplier_history_restored'"));
assert.ok(supplierAction.includes("operation !== 'restore-both-versions'"));
assert.ok(supplierPage.includes('Draft A / 08:14') && supplierPage.includes('Draft B / 19:52'));
assert.ok(correction.includes("hasCampaignEvent('p7.nessa_publicly_cleared')"));
for (const heading of ['Material:', 'Record:', 'Conduct:']) assert.ok(correction.includes(heading));
assert.ok(correction.includes('accusation remains preserved as evidence'));
assert.ok(index.includes('/community/archive/supplier-revisions'));
assert.ok(index.includes('/community/2011/03/14/nessa-correction'));

console.log('Copperline P7 supplier/exoneration selftest: PASS');
