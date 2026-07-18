import { strict as assert } from 'node:assert';
import { P8_INTERVENTION_CANONICAL_PAYLOAD, unsupportedInterventionParts, validInterventionPlan } from './p8-intervention-plan';

assert.equal(P8_INTERVENTION_CANONICAL_PAYLOAD.observation_receipts, 0);
for (const finding of [
  {
    causes: 'Old fracture, unchanged heat load, paired-watch gap, and late routing interacted. Nessa proved the earlier edits.',
    iss: 'Iss had sound surface proof, but his unreviewed route was unsafe.',
    copyBoundary: 'The altered office shows behavior. It does not identify the Dark.',
    order: 'water filter -> paired lights -> close pressure bypass -> staff route',
  },
  {
    causes: 'The existing fracture met a heat load that stayed high, an empty watch, and delayed closure. It was the same edit pattern as Nessa.',
    iss: 'His reed sample held; the cut was unsafe.',
    copyBoundary: 'Copying is proven. The Dark remains unknown.',
    order: 'filter water, restore watch lamps, settle pressure, then open the passage',
  },
  {
    causes: 'An earlier fracture met heat that stayed high, a coverage gap, and a delayed closure; prior falsification was already proven.',
    iss: 'The reed sample checked out, but the cut was unsafe.',
    copyBoundary: 'The copy shows behavior. We still do not know what it is.',
    order: 'Fix the filter, restore the lamps, settle the bypass, then use the passage.',
  },
]) assert.equal(validInterventionPlan(finding), true, 'natural complete plan must pass');

for (const finding of [
  {
    causes: 'Iss caused everything.',
    iss: 'The route was safe.',
    copyBoundary: 'The altered office proves the Dark is the Record.',
    order: 'route, water, pressure, lights',
  },
  {
    causes: 'Old fracture, unchanged heat load, watch gap, late routing, and earlier edited records.',
    iss: 'The surface sample was sound and the cut unsafe.',
    copyBoundary: 'Copy behavior is proven; ontology open.',
    order: 'open route, then water, paired lights, pressure',
  },
  {
    causes: 'old fracture heat load watch gap late route surface proof valid route unsafe',
    iss: 'copying is proven dark unknown',
    copyBoundary: 'water lights pressure route',
    order: 'all the words are in the wrong fields',
  },
]) assert.equal(validInterventionPlan(finding), false, 'wrong, unsafe-order, or field-swapped plan must fail');

const disconnected = {
  causes: 'Old fracture, unchanged heat load, paired-watch gap, and late routing.',
  iss: 'The surface sample was sound and the cut unsafe.',
  copyBoundary: 'Copying is proven; the Dark remains unknown.',
  order: 'water, paired lights, pressure, route',
};
assert.equal(validInterventionPlan(disconnected), false, 'P7 cannot become an unrelated closed file');
assert.deepEqual(unsupportedInterventionParts(disconnected), ['earlier record-edit pattern']);

console.log('p8-intervention-plan.selftest OK: P7 callback, four meaning components, safe order, and focused refusal');
