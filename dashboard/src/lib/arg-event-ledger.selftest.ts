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
  let currentNow = new Date('2026-07-17T20:00:00.000Z');
  const now = () => new Date(currentNow);
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

  const p2 = await ledger.record({
    eventKey: 'p2.artifact_authenticated',
    idempotencyKey: 'copperline:p2:package-669f3fd0-relay-quarantine',
    source: 'copperline',
    payload: { artifact: 'the-hold.zip', relay_note: 'quarantined-unverified' },
  });
  check(p2.status === 'committed' && p2.created,
    'a provenance decision must continue the P1 to P2 causal chain');

  const restarted = new FileArgEventLedger(path, now);
  const state = await restarted.read();
  check(state.events.length === 3, 'restart must retain exactly three canonical events');
  check(state.projections.some((projection) => projection.surface === 'discord'
    && projection.status === 'queued'), 'cross-surface response must queue durably');
  check(await restarted.has('p1.attachment_history_restored'), 'event readback must survive restart');

  const claimed = await restarted.claimProjections('discord', 1, 30_000);
  check(claimed.length === 1 && claimed[0]?.projection.status === 'processing'
    && claimed[0].projection.attempts === 1 && Boolean(claimed[0].projection.leaseToken),
    'one worker must acquire one durable projection lease');
  const concurrent = await restarted.claimProjections('discord', 1, 30_000);
  check(concurrent.every((item) => item.event.eventId !== claimed[0]!.event.eventId),
    'an active lease must prevent concurrent duplicate delivery of the same event');
  check(await restarted.completeProjection({
    eventId: claimed[0]!.event.eventId,
    surface: 'discord',
    leaseToken: '00000000-0000-0000-0000-000000000000',
    applied: true,
  }) === false, 'a stale or foreign lease token must not acknowledge work');
  check(await restarted.completeProjection({
    eventId: claimed[0]!.event.eventId,
    surface: 'discord',
    leaseToken: claimed[0]!.projection.leaseToken!,
    applied: false,
    error: 'temporary outage',
  }), 'the owning worker must be able to record a retryable failure');
  const immediateRetry = await restarted.claimProjections('discord', 10, 30_000);
  check(immediateRetry.every((item) => item.event.eventId !== claimed[0]!.event.eventId),
    'a failed projection must respect its durable retry backoff');
  currentNow = new Date(currentNow.getTime() + 5_001);
  const retry = await restarted.claimProjections('discord', 10, 30_000);
  const retriedClaim = retry.find((item) => item.event.eventId === claimed[0]!.event.eventId);
  check(retriedClaim?.projection.attempts === 2,
    'a failed projection must be leased again with an incremented attempt count');
  check(await restarted.completeProjection({
    eventId: retriedClaim!.event.eventId,
    surface: 'discord',
    leaseToken: retriedClaim!.projection.leaseToken!,
    applied: true,
  }), 'successful delivery must acknowledge the owning lease');
  const afterProjectionRestart = await new FileArgEventLedger(path, now).read();
  const applied = afterProjectionRestart.projections.find((projection) =>
    projection.eventId === retriedClaim!.event.eventId && projection.surface === 'discord');
  check(applied?.status === 'applied' && applied.attempts === 2
    && applied.leaseToken === null && applied.lastError === null,
    'applied projection state must survive restart without stale lease/error state');
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

  // Exercise the complete P1-P12 graph in an order that proves optional evidence interactions do
  // not gate correct conclusions. P4 resolves before its restore/test; P7 resolves before its web
  // restore; P10 attribution resolves before the optional bounded-copy proof.
  const full = new FileArgEventLedger(join(root, 'full-ledger.json'), now);
  const completePath = [
    ['p1.attachment_history_restored', 'copperline'],
    ['p1.mkept_intent_authenticated', 'copperline'],
    ['p2.artifact_authenticated', 'copperline'],
    ['p2.live_runtime_handoff', 'minecraft'],
    ['p3.resident_accounts_opened', 'minecraft'],
    ['p3.dispatch_authorized', 'discord'],
    ['p4.control_reversal_earned', 'minecraft'],
    ['p4.mouth_revision_restored', 'copperline'],
    ['p4.copy_hypothesis_tested', 'discord'],
    ['p5.service_chronology_shared', 'minecraft'],
    ['p5.civic_gallery_recurated', 'minecraft'],
    ['p6.professional_models_recovered', 'minecraft'],
    ['p6.six_responsibilities_acknowledged', 'minecraft'],
    ['p7.counterfeit_material_proven', 'minecraft'],
    ['p7.nessa_publicly_cleared', 'discord'],
    ['p7.supplier_history_restored', 'copperline'],
    ['p8.intervention_plan_accepted', 'copperline'],
    ['p8.hold_systems_repaired', 'minecraft'],
    ['p9.company_biographies_restored', 'copperline'],
    ['p9.leak_window_proven', 'copperline'],
    ['p10.wren_confronted', 'discord'],
    ['p10.player_copy_proof', 'minecraft'],
    ['p10.wren_remembrance_committed', 'minecraft'],
    ['p11.averyn_identified', 'discord'],
    ['p11.averyn_restored_unbound', 'minecraft'],
    ['p12.release_configuration_ready', 'minecraft'],
    ['p12.name_treatment_committed', 'minecraft'],
    ['p12.record_closed_averyn_released', 'minecraft'],
  ] as const;
  for (const [index, [eventKey, source]] of completePath.entries()) {
    const result = await full.record({
      eventKey,
      idempotencyKey: `selftest:full:${String(index + 1).padStart(2, '0')}:${eventKey}`,
      source,
      payload: { canonical_test_action: eventKey, observation_receipts: 0 },
    });
    check(result.status === 'committed' && result.created, `full graph failed at ${eventKey}`);
  }
  const fullRestart = await new FileArgEventLedger(join(root, 'full-ledger.json'), now).read();
  check(fullRestart.events.length === 28, 'full P1-P12 graph must retain all 28 events after restart');
  check(fullRestart.events.every((event) => event.payload.observation_receipts === 0),
    'complete graph fixtures must prove zero-observation acceptance');
  } finally {
    await rm(root, { recursive: true, force: true });
  }

  console.log('arg-event-ledger.selftest OK: prerequisites, ownership, idempotency, collision, restart, lease, retry, projection, full P1-P12 graph');
}

void main();
