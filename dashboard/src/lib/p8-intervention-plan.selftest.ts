import { strict as assert } from 'node:assert';
import { P8_INTERVENTION_CANONICAL_PAYLOAD, validInterventionPlan } from './p8-intervention-plan';

assert.equal(P8_INTERVENTION_CANONICAL_PAYLOAD.observation_receipts, 0);
for (const finding of [
  {
    causes: 'Old fracture, unchanged heat load, paired-watch gap, and late routing interacted.',
    iss: 'Iss had sound surface proof, but his unreviewed route was unsafe.',
    copyBoundary: 'The altered office shows behavior. It does not identify the Dark.',
    order: 'water filter -> paired lights -> close pressure bypass -> staff route',
  },
  {
    causes: 'The existing fracture met a heat load that stayed high, an empty watch, and delayed closure.',
    iss: 'His reed sample held; the cut was unsafe.',
    copyBoundary: 'Copying is proven. The Dark remains unknown.',
    order: 'filter water, restore watch lamps, settle pressure, then open the passage',
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
    causes: 'Old fracture, unchanged heat load, watch gap, late routing.',
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

console.log('p8-intervention-plan.selftest OK: four meaning components, safe order, wrong/field-swap refusal');
