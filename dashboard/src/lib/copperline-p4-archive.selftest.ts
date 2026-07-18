import { strict as assert } from 'node:assert';
import {
  copperlineP4DirectEntries,
  copperlineP4Entries,
  copperlineP4TextureEntries,
  P4_COPPERLINE_OFFLINE_NOTICE,
} from './copperline-p4-archive';

const ids = new Set(copperlineP4Entries.map((entry) => entry.id));
assert.equal(ids.size, copperlineP4Entries.length, 'archive ids must be unique');
assert.ok(copperlineP4TextureEntries.length >= copperlineP4DirectEntries.length * 3,
  'ordinary/mixed texture must outnumber direct clues by at least 3:1 in this review fixture');
assert.deepEqual(copperlineP4DirectEntries.map((entry) => entry.id),
  ['p4-ticket', 'p4-ticket-reply', 'p4-diff', 'p4-clock', 'p4-index']);
assert.ok(copperlineP4Entries.some((entry) => entry.author === 'mkept'));
assert.ok(new Set(copperlineP4Entries.map((entry) => entry.author)).size >= 10,
  'human texture needs distinct voices');
assert.ok(copperlineP4Entries.every((entry) => !/Averyn|Nessa|Keeper|Wren|Dark|Record/i.test(entry.body)),
  'C02 fixture must not dump later ancient or modern revelations');
assert.ok(!copperlineP4Entries.some((entry) =>
  /REFUGE BEFORE RITE|SAFETY BECAME OBEDIENCE/i.test(entry.body)),
  'final synthesis may not be printed in Copperline');
assert.match(P4_COPPERLINE_OFFLINE_NOTICE, /not a production deployment/);
console.log('Copperline P4 offline archive self-test passed');
