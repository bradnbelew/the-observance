import { externalMutationsAllowed } from './deployment-target';

if (externalMutationsAllowed({ VERCEL_ENV: 'preview' })) {
  throw new Error('an unbound Vercel preview must be read-only');
}
for (const productionRef of ['fndmhbpxnodrnbrzrlqq', 'fdnmhbpxnodrnbrzrlqq']) {
  if (externalMutationsAllowed({
    VERCEL_ENV: 'preview',
    OBSERVANCE_MUTATION_TARGET: 'verified-private-staging',
    OBSERVANCE_MUTATION_PROJECT_REF: productionRef,
  })) {
    throw new Error(`the production/legacy Supabase ref must never qualify as preview staging: ${productionRef}`);
  }
}
if (!externalMutationsAllowed({
  VERCEL_ENV: 'preview',
  OBSERVANCE_MUTATION_TARGET: 'verified-private-staging',
  OBSERVANCE_MUTATION_PROJECT_REF: 'disposable-development-ref',
})) {
  throw new Error('an exact verified non-production target should permit preview mutations');
}
if (!externalMutationsAllowed({ VERCEL_ENV: 'production' })) {
  throw new Error('production behavior remains controlled by the separate release gate');
}

console.log('deployment-target.selftest OK: previews fail closed; production ref cannot be staged');
