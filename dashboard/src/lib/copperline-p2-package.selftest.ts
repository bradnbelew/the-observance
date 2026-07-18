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
assert.ok(action.includes('readValidatedV5HoldArchive'));
assert.ok(action.includes('sha1: archive.sha1'));
assert.ok(action.includes("decision: 'verified-package-quarantined-unmatched-relay'"));
assert.ok(action.includes("source: 'copperline'"));
assert.ok(form.includes('actual packaged bytes'));
assert.ok(form.includes('Run retained-receipt verification'));
assert.ok(!form.includes('type="radio"'));
assert.ok(form.includes('unmatched relay note remains quarantined'));
assert.ok(download.includes("hasCampaignEvent('p2.artifact_authenticated')"));
assert.ok(!download.includes('p2.live_runtime_handoff'));
assert.ok(!action.includes('observation'));

console.log('Copperline P2 package/provenance selftest: PASS');
