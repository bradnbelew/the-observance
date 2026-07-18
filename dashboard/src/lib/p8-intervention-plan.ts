export type InterventionPlanFinding = {
  causes: string;
  iss: string;
  copyBoundary: string;
  order: string;
};

export const P8_INTERVENTION_CANONICAL_PAYLOAD = Object.freeze({
  finding_shape: 'causes-iss-copy-boundary-order-v1',
  causes: ['old-fracture', 'unchanged-heat-load', 'paired-watch-gap', 'late-routing'],
  iss: 'surface-proof-valid-route-unsafe',
  copy_boundary: 'copy-behavior-proven-ontology-open',
  works_order: ['water-filter', 'paired-light', 'pressure-bypass', 'staff-route'],
  observation_receipts: 0,
});

const fold = (value: string): string => value.normalize('NFKC').toLocaleLowerCase('en-US')
  .replace(/[^a-z0-9]+/g, ' ').trim().replace(/\s+/g, ' ');
const hasAny = (value: string, terms: readonly string[]): boolean => terms.some((term) => value.includes(term));
const first = (value: string, terms: readonly string[]): number => {
  const found = terms.map((term) => value.indexOf(term)).filter((index) => index >= 0);
  return found.length === 0 ? -1 : Math.min(...found);
};

/** Four independent meaning components; no long hidden sentence and no source-receipt predicate. */
export function validInterventionPlan(finding: InterventionPlanFinding): boolean {
  const causes = fold(finding.causes);
  const iss = fold(finding.iss);
  const boundary = fold(finding.copyBoundary);
  const order = fold(finding.order);
  const positions = [
    first(order, ['water', 'filter']),
    first(order, ['paired light', 'lamp', 'watch light']),
    first(order, ['pressure', 'bypass']),
    first(order, ['staff route', 'route', 'passage']),
  ];
  return hasAny(causes, ['old fracture', 'existing fracture', 'earlier fracture'])
    && hasAny(causes, ['heat load', 'unchanged heat', 'heat stayed', 'stayed high'])
    && hasAny(causes, ['watch gap', 'paired watch', 'empty watch', 'coverage gap'])
    && hasAny(causes, ['late route', 'late routing', 'closure delay', 'delayed closure'])
    && hasAny(iss, ['surface proof', 'surface sample', 'reed sample', 'water sample'])
    && hasAny(iss, ['valid', 'sound', 'true', 'held'])
    && hasAny(iss, ['route unsafe', 'cut unsafe', 'cut was unsafe', 'unreviewed route', 'unsafe cut'])
    && hasAny(boundary, ['copy', 'altered office', 'record behavior'])
    && hasAny(boundary, ['proves behavior', 'shows behavior', 'copying is proven', 'alteration is proven'])
    && hasAny(boundary, ['dark unknown', 'dark remains unknown', 'ontology open', 'does not identify', 'not what the dark is'])
    && positions.every((position) => position >= 0)
    && positions.every((position, index) => index === 0 || positions[index - 1]! < position);
}
