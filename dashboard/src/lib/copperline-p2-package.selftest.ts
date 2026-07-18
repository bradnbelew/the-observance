import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const read = (path: string) => readFileSync(resolve(path), 'utf8');
const page = read('src/app/community/archive/package-review/page.tsx');
const action = read('src/app/community/archive/package-review/actions.ts');
const form = read('src/app/community/archive/package-review/PackageReviewForm.tsx');
const download = read('src/app/the-hold/the-hold.zip/route.ts');

assert.ok(page.includes("hasCampaignEvent('p1.mkept_intent_authenticated')"));
assert.ok(page.includes('669f3fd00bbb6e647eeb8941e79281cc434f1e8c'));
assert.ok(page.includes('no cartridge barcode, no host checksum'));
assert.ok(action.includes("eventKey: 'p2.artifact_authenticated'"));
assert.ok(action.includes("decision !== 'verify-package-quarantine-relay'"));
assert.ok(action.includes("source: 'copperline'"));
assert.ok(form.includes('not a password or hidden phrase'));
assert.ok(form.includes('quarantine the later relay address as unverified'));
assert.ok(download.includes("hasCampaignEvent('p2.artifact_authenticated')"));
assert.ok(!download.includes('p2.live_runtime_handoff'));
assert.ok(!action.includes('observation'));

console.log('Copperline P2 package/provenance selftest: PASS');
