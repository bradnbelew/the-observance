import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/archive/recovered-packet/page.tsx');
const index = read('src/app/community/index.php/page.tsx');

assert.ok(page.includes("hasCampaignEvent('p10.wren_remembrance_committed')"));
assert.ok(page.includes("hasCampaignEvent('p11.averyn_identified')"));
assert.ok(page.includes("hasCampaignEvent('p11.averyn_restored_unbound')"));
for (const hash of ['783ecde5685abdb601e4a659fc947c32964f70b3', '2003f0151c1ba643c649b5ed0e19d1b31bb68319']) assert.ok(page.includes(hash));
assert.ok(page.includes('not a seventh Keeper title'));
const beforeIdentityReceipt = page.slice(0, page.indexOf('{identified === true'));
assert.ok(beforeIdentityReceipt.includes('marked word supplies <b>V</b>'));
assert.ok(!beforeIdentityReceipt.includes('A / V / E / R / Y / N'), 'one Copperline scan must not print the whole identity');
assert.ok(!beforeIdentityReceipt.includes('<b>AVERYN</b>'), 'identity must appear only after the accepted identity event');
for (const term of ['Averyn:', 'Record:', 'Watcher:', 'Dark:']) assert.ok(page.includes(term));
assert.ok(page.includes('still distinct and unknown'));
assert.ok(index.includes('/community/archive/recovered-packet'));

console.log('Copperline P11 packet/identity selftest: PASS');
