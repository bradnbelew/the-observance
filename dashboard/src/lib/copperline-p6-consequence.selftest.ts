import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/2011/03/03/six-workspaces/page.tsx');
const index = read('src/app/community/index.php/page.tsx');

assert.ok(page.includes("hasCampaignEvent('p6.six_responsibilities_acknowledged')"));
for (const name of ['Vaun', 'Mara', 'Sella', 'Orin', 'Brann', 'Iss']) assert.ok(page.includes(name));
assert.ok(page.includes('Not a seventh holder of that role'));
assert.ok(page.includes('another hand'));
assert.ok(index.includes('/community/2011/03/03/six-workspaces'));
assert.ok(!page.includes('answer field'));

console.log('Copperline P6 biography/category consequence selftest: PASS');
