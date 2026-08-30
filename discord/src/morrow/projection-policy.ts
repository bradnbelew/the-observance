import {
  MORROW_BEHAVIOR_REUSE_EVENT,
  MORROW_PRIVATE_CONTRADICTION_EVENT,
} from './contradiction.js';

export interface MorrowDiscordProjectionClaim {
  eventId: string;
  eventKey: string;
  campaignId: string;
  releaseId: string;
  payloadSha256: string;
  channelId: string | null;
  threadId: string | null;
  attempts: number;
}

export interface MorrowProjectionDependencies {
  claim(workerId: string, limit: number, leaseSeconds: number): Promise<MorrowDiscordProjectionClaim[]>;
  activate(claim: MorrowDiscordProjectionClaim): Promise<'activated' | 'duplicate' | 'blocked' | 'collision'>;
  post(claim: MorrowDiscordProjectionClaim): Promise<boolean>;
  complete(claim: MorrowDiscordProjectionClaim, workerId: string, applied: boolean, error?: string): Promise<boolean>;
}

export interface MorrowProjectionBatchResult { claimed: number; applied: number; failed: number }

export async function runMorrowDiscordProjectionBatch(
  workerId: string,
  dependencies: MorrowProjectionDependencies,
  limit = 10,
  leaseSeconds = 45,
): Promise<MorrowProjectionBatchResult> {
  const claims = await dependencies.claim(workerId, limit, leaseSeconds);
  const result = { claimed: claims.length, applied: 0, failed: 0 };
  for (const claim of claims) {
    try {
      let applied = false;
      let failure: string | undefined;
      if (claim.eventKey === MORROW_BEHAVIOR_REUSE_EVENT) {
        const activation = await dependencies.activate(claim);
        applied = activation === 'activated' || activation === 'duplicate';
        failure = applied ? undefined : `contradiction activation ${activation}`;
      } else if (claim.eventKey === MORROW_PRIVATE_CONTRADICTION_EVENT) {
        applied = await dependencies.post(claim);
        failure = applied ? undefined : 'group receipt delivery failed';
      } else {
        failure = 'unsupported Morrow Discord projection';
      }
      const acknowledged = await dependencies.complete(claim, workerId, applied, failure);
      if (applied && acknowledged) result.applied += 1;
      else result.failed += 1;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      try { await dependencies.complete(claim, workerId, false, message); } catch { /* lease expiry is recovery */ }
      result.failed += 1;
    }
  }
  return result;
}
