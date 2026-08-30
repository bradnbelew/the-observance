import { randomUUID } from 'node:crypto';
import { createClient } from '@supabase/supabase-js';

const PRODUCTION_REFS = new Set(['fndmhbpxnodrnbrzrlqq', 'fdnmhbpxnodrnbrzrlqq']);
const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
const serviceKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
const releaseId = process.env.MORROW_RELEASE_ID;
const targetRef = process.env.OBSERVANCE_MUTATION_PROJECT_REF;
const enabled = process.env.MORROW_COPPERLINE_PROJECTOR === 'enabled';
const maxBatches = Number(process.env.MORROW_PROJECTOR_MAX_BATCHES ?? 20);
const productionAcknowledged = process.env.MORROW_PROJECTOR_PRODUCTION_ACK
  === `apply:${releaseId}:${targetRef}`;
let targetUrl;
try {
  targetUrl = new URL(url ?? '');
} catch {
  targetUrl = null;
}

if (!enabled || !url || !serviceKey || !releaseId || !targetRef
    || !Number.isInteger(maxBatches) || maxBatches < 1 || maxBatches > 100
    || !/^[a-z]{20}$/.test(targetRef)
    || (PRODUCTION_REFS.has(targetRef) && !productionAcknowledged)
    || targetUrl?.protocol !== 'https:' || targetUrl.hostname !== `${targetRef}.supabase.co`) {
  throw new Error('Morrow Copperline projector is disabled or lacks an exact target authorization.');
}

const client = createClient(url, serviceKey, {
  auth: { autoRefreshToken: false, persistSession: false },
});
const workerId = randomUUID();
let applied = 0;
let failed = 0;

for (let batch = 0; batch < maxBatches; batch += 1) {
  const { data, error } = await client.rpc('morrow_claim_copperline_projections', {
    p_worker_id: workerId,
    p_release_id: releaseId,
    p_limit: 25,
    p_lease_seconds: 60,
  });
  if (error) throw new Error(`Copperline claim failed: ${error.code ?? 'unknown'}`);
  const claims = Array.isArray(data) ? data : [];
  if (claims.length === 0) break;

  for (const claim of claims) {
    const result = await client.rpc('morrow_apply_copperline_projection', {
      p_event_id: claim.event_id,
      p_worker_id: workerId,
      p_release_id: releaseId,
    });
    if (!result.error && result.data === true) {
      applied += 1;
      continue;
    }
    failed += 1;
    const failure = await client.rpc('morrow_fail_copperline_projection', {
      p_event_id: claim.event_id,
      p_worker_id: workerId,
      p_release_id: releaseId,
      p_error: result.error?.code ?? 'projection refused',
    });
    if (failure.error || failure.data !== true) {
      throw new Error(`Copperline failure receipt could not be committed for ${claim.event_id}`);
    }
  }
}

console.log(JSON.stringify({ status: failed === 0 ? 'pass' : 'retry_scheduled', applied, failed }));
if (failed > 0) process.exitCode = 1;
