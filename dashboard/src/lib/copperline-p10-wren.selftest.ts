import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/archive/wren-moderation/page.tsx');
const index = read('src/app/community/index.php/page.tsx');

assert.ok(page.includes("hasCampaignEvent('p9.leak_window_proven')"));
assert.ok(page.includes("hasCampaignEvent('p10.player_copy_proof')"));
assert.ok(page.includes("hasCampaignEvent('p10.wren_confronted')"));
assert.ok(page.includes("hasCampaignEvent('p10.wren_remembrance_committed')"));
assert.ok(page.includes('removed comment body is not restored as a new confession'));
assert.ok(page.includes('cannot reveal the missing words'));
assert.ok(page.includes('does not, by itself, prove packet authorship or motive'));
assert.ok(page.includes('stores only the pattern receipt and the before-and-after difference'));
assert.ok(page.includes('received no player text, names, inventory, or private build data'));
assert.ok(page.includes('does not explain who made it'));
assert.ok(page.includes('retained account response'));
assert.ok(page.includes('This response follows the independent proof'));
assert.ok(index.includes('/community/archive/wren-moderation'));
assert.ok(index.includes("hasCampaignEvent('p10.player_copy_proof')"));

console.log('Copperline P10 moderation/aftermath selftest: PASS');
