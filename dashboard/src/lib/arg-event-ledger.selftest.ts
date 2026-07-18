import { mkdtemp, rm } from 'node:fs/promises';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import { FileArgEventLedger, stableJson } from './arg-event-ledger';

function check(condition: boolean, message: string): void {
  if (!condition) throw new Error(`ARG event-ledger self-test failed: ${message}`);
}

async function main(): Promise<void> {
  const root = await mkdtemp(join(tmpdir(), 'observance-arg-events-'));
  const path = join(root, 'ledger.json');
  const now = () => new Date('2026-07-17T20:00:00.000Z');
  try {
  const ledger = new FileArgEventLedger(path, now);
  const blocked = await ledger.record({
    eventKey: 'p1.mkept_intent_authenticated',
    idempotencyKey: 'web:p1:intent-before-history',
    source: 'copperline',
    payload: { belief: 'deliberate preservation' },
  });
  check(blocked.status === 'blocked' && blocked.missingPrerequisites?.[0]
    === 'p1.attachment_history_restored', 'story prerequisites must fail closed');

  const first = await ledger.record({
    eventKey: 'p1.attachment_history_restored',
    idempotencyKey: 'web:p1:restore-service-1842',
    source: 'copperline',
    payload: { service: '1842', revisions: ['manifest-a', 'manifest-b'] },
  });
  check(first.status === 'committed' && first.created, 'first exact event must commit');
  const duplicate = await ledger.record({
    eventKey: 'p1.attachment_history_restored',
    idempotencyKey: 'web:p1:restore-service-1842',
    source: 'copperline',
    payload: { revisions: ['manifest-a', 'manifest-b'], service: '1842' },
  });
  check(duplicate.status === 'committed' && !duplicate.created,
    'key-order variation must return the same idempotent event');
  const collision = await ledger.record({
    eventKey: 'p1.attachment_history_restored',
    idempotencyKey: 'web:p1:restore-service-1842',
    source: 'copperline',
    payload: { service: 'different' },
  });
  check(collision.status === 'collision' && !collision.created,
    'same key with a different canonical payload must be rejected');

  const second = await ledger.record({
    eventKey: 'p1.mkept_intent_authenticated',
    idempotencyKey: 'web:p1:authenticate-mkept-intent',
    source: 'copperline',
    payload: { belief: 'mkept deliberately preserved a damaged server' },
  });
  check(second.status === 'committed' && second.created,
    'the next story event must open after its prerequisite');

  const restarted = new FileArgEventLedger(path, now);
  const state = await restarted.read();
  check(state.events.length === 2, 'restart must retain exactly two canonical events');
  check(state.projections.some((projection) => projection.surface === 'discord'
    && projection.status === 'queued'), 'cross-surface response must queue durably');
  check(await restarted.has('p1.attachment_history_restored'), 'event readback must survive restart');
  check(stableJson({ z: 1, a: ['x', true] }) === '{"a":["x",true],"z":1}',
    'payload canonicalization must be stable');

  let wrongSurface = false;
  try {
    await ledger.record({
      eventKey: 'p4.mouth_revision_restored',
      idempotencyKey: 'minecraft:p4:wrong-owner',
      source: 'minecraft',
      payload: {},
    });
  } catch {
    wrongSurface = true;
  }
  check(wrongSurface, 'a surface may not claim an event owned by another platform');
  } finally {
    await rm(root, { recursive: true, force: true });
  }

  console.log('arg-event-ledger.selftest OK: prerequisites, ownership, idempotency, collision, restart, projection');
}

void main();
