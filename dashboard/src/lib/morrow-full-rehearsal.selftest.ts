import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  allMorrowGates,
  buildFullRehearsalReceipt,
  directorLockContract,
  fullRehearsalDigest,
  gateById,
  gateReceiptById,
  mediaRequirements,
} from './morrow-full-rehearsal';

const gates = allMorrowGates();
assert.equal(gates.length, 15);
assert.deepEqual(gates.map((gate) => gate.id), Array.from({ length: 15 }, (_, index) => `G${String(index + 1).padStart(2, '0')}`));
assert.ok(gates.every((gate) => gate.evidence.length >= 3));
assert.ok(gates.every((gate) => gate.accessible_equivalent.length > 0));
assert.ok(gates.every((gate) => gate.eventKey.startsWith('morrow.gate.')));
assert.equal(gateById('g11')?.title, 'Status gap');
assert.equal(gateById('G15')?.owner, 'minecraft');

const receipt = buildFullRehearsalReceipt();
assert.equal(receipt.gateReceipts.length, 15);
assert.equal(receipt.boundaries.productionContacted, false);
assert.equal(receipt.boundaries.supabaseContacted, false);
assert.equal(receipt.boundaries.discordGatewayContacted, false);
assert.equal(receipt.directorControlsEnabled, false);
assert.equal(receipt.productionMutationsAllowed, false);
assert.equal(receipt.mediaRequired.length, 12);
assert.match(fullRehearsalDigest(), /^[0-9a-f]{64}$/);
assert.equal(gateReceiptById('g07')?.gate, 'G07');
assert.equal(mediaRequirements().length, 12);
assert.equal(mediaRequirements().filter((asset) => asset.releaseBlocking).length, 12);
assert.equal(directorLockContract().controlsEnabled, false);
assert.ok(directorLockContract().forbidden.includes('enable_production'));

for (let index = 0; index < receipt.gateReceipts.length; index += 1) {
  const row = receipt.gateReceipts[index];
  assert.equal(row.sequence, index + 1);
  assert.equal(row.previousReceiptSha256, index === 0 ? '0'.repeat(64) : receipt.gateReceipts[index - 1].receiptSha256);
}

for (const route of [
  'src/app/api/rehearsal/morrow/full/route.ts',
  'src/app/api/rehearsal/morrow/gates/[gateId]/route.ts',
  'src/app/api/rehearsal/morrow/media/route.ts',
  'src/app/api/rehearsal/morrow/readiness/route.ts',
  'src/app/api/rehearsal/morrow/director/route.ts',
  'src/app/recovery/mossfield/console/page.tsx',
  'src/app/recovery/mossfield/gates/[gateId]/page.tsx',
]) {
  const source = readFileSync(resolve(route), 'utf8');
  assert.equal(source.includes('createClient('), false, `${route} must stay local and unauthenticated`);
  assert.equal(source.includes('SUPABASE_SERVICE_ROLE_KEY'), false, `${route} must not expose service secrets`);
}

const mediaRoute = readFileSync(resolve('src/app/api/rehearsal/morrow/media/route.ts'), 'utf8');
assert.ok(mediaRoute.includes("schemaVersion: '1.1.0-morrow-media-readiness'"));
assert.ok(mediaRoute.includes("workFolder: `morrow/media/work/${asset.key}`"));
assert.ok(mediaRoute.includes("releaseReadyRequiresSafetyReview: true"));

const consoleRoute = readFileSync(resolve('src/app/recovery/mossfield/console/page.tsx'), 'utf8');
assert.ok(consoleRoute.includes('Media intake'));
assert.ok(consoleRoute.includes('needs source, final, hashes, custody, accessibility'));

console.log('MORROW FULL REHEARSAL: PASS gates=15 media=12 local-boundary hash-chain');
