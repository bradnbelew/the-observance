import { strict as assert } from 'node:assert';
import { P4_COPPERLINE_ROUTE, P4_COPPERLINE_ROUTE_STEPS } from './copperline-p4-route';

assert.equal(P4_COPPERLINE_ROUTE_STEPS.length, 5);
assert.equal(P4_COPPERLINE_ROUTE_STEPS[0]?.path, P4_COPPERLINE_ROUTE.community);
assert.equal(P4_COPPERLINE_ROUTE_STEPS.at(-1)?.path, P4_COPPERLINE_ROUTE.retainedAttachments);
assert.equal(new Set(P4_COPPERLINE_ROUTE_STEPS.map((step) => step.path)).size, P4_COPPERLINE_ROUTE_STEPS.length);
assert.ok(P4_COPPERLINE_ROUTE.priorBackupPost.includes('world-backup'));
assert.ok(P4_COPPERLINE_ROUTE.custodyTicket.endsWith('id=2184'));
console.log('Copperline P4 connected-route self-test passed');
