import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/2011/02/16/service-counter/page.tsx');
const index = read('src/app/community/index.php/page.tsx');

for (const source of [page, index]) assert.ok(source.includes("hasCampaignEvent('p5.civic_gallery_recurated')"));
assert.ok(page.includes('wick shears'));
assert.ok(page.includes('school chalk'));
assert.ok(page.includes('sample rings'));
assert.ok(page.includes('older shrine caption remains in revision history'));
assert.ok(index.includes('/community/2011/02/16/service-counter'));
assert.ok(!page.includes('answer'));

console.log('Copperline P5 public-curation consequence selftest: PASS');
