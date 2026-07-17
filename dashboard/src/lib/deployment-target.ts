const PRODUCTION_SUPABASE_REFS = new Set([
  'fndmhbpxnodrnbrzrlqq', // verified connected-app project ref, 2026-07-17
  'fdnmhbpxnodrnbrzrlqq', // legacy repository spelling; forbidden until reconciled
]);
const VERIFIED_PRIVATE_STAGING = 'verified-private-staging';

type DeploymentEnvironment = Record<string, string | undefined>;

/**
 * Preview deployments are read-only unless an operator binds an exact, non-production database and
 * marks it as verified private staging. The marker alone is insufficient: the production project ref
 * is always rejected. Production keeps its existing operator-controlled behavior; public promotion is
 * governed separately by the release gate.
 */
export function externalMutationsAllowed(env: DeploymentEnvironment = process.env): boolean {
  if (env.VERCEL_ENV !== 'preview') return true;
  const targetRef = env.OBSERVANCE_MUTATION_PROJECT_REF?.trim();
  return env.OBSERVANCE_MUTATION_TARGET === VERIFIED_PRIVATE_STAGING
    && Boolean(targetRef)
    && !PRODUCTION_SUPABASE_REFS.has(targetRef ?? '');
}

export function assertExternalMutationAllowed(operation: string): void {
  if (!externalMutationsAllowed()) {
    throw new Error(`External mutation disabled for this preview (${operation}).`);
  }
}
