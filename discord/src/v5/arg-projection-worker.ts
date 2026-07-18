import { claimArgProjections, completeArgProjection } from '../db/repo.js';
import type { ArgProjectionClaimRow } from '../db/types.js';
import { postProjectionToTheRecord } from '../showrunner/discord.js';
import { discordProjectionMessage } from './arg-projection-catalog.js';

export interface ProjectionBatchResult {
  claimed: number;
  applied: number;
  failed: number;
}

export interface ProjectionWorkerDependencies {
  claim(limit: number, leaseSeconds: number): Promise<ArgProjectionClaimRow[]>;
  post(content: string, eventId: string): Promise<boolean>;
  complete(claim: ArgProjectionClaimRow, applied: boolean, error?: string): Promise<boolean>;
}

const DEFAULT_DEPENDENCIES: ProjectionWorkerDependencies = {
  claim: (limit, leaseSeconds) => claimArgProjections('discord', limit, leaseSeconds),
  post: postProjectionToTheRecord,
  complete: (claim, applied, error) => completeArgProjection({
    eventId: claim.event_id,
    surface: 'discord',
    leaseToken: claim.lease_token,
    applied,
    error,
  }),
};

/** Deliver one bounded batch. Payloads are deliberately ignored at the player-facing edge. */
export async function runDiscordProjectionBatch(
  dependencies: ProjectionWorkerDependencies = DEFAULT_DEPENDENCIES,
  batchSize = 10,
  leaseSeconds = 45,
): Promise<ProjectionBatchResult> {
  const claims = await dependencies.claim(batchSize, leaseSeconds);
  const result: ProjectionBatchResult = { claimed: claims.length, applied: 0, failed: 0 };
  for (const claim of claims) {
    const content = discordProjectionMessage(claim.event_key);
    if (!content) {
      await dependencies.complete(claim, false, `no authored Discord projection for ${claim.event_key}`);
      result.failed += 1;
      continue;
    }
    const posted = await dependencies.post(content, claim.event_id);
    const acknowledged = await dependencies.complete(
      claim,
      posted,
      posted ? undefined : 'Discord message delivery failed',
    );
    if (posted && acknowledged) result.applied += 1;
    else result.failed += 1;
  }
  return result;
}

/** Start the long-running, overlap-safe projector owned by the private Discord worker. */
export function startArgProjectionWorker(options: {
  intervalMilliseconds?: number;
  onError?(error: unknown): void;
} = {}): () => void {
  const intervalMilliseconds = options.intervalMilliseconds ?? 10_000;
  if (!Number.isInteger(intervalMilliseconds) || intervalMilliseconds < 1_000) {
    throw new Error('invalid ARG projection worker interval');
  }
  let running = false;
  let stopped = false;
  const tick = async (): Promise<void> => {
    if (running || stopped) return;
    running = true;
    try {
      await runDiscordProjectionBatch();
    } catch (error) {
      options.onError?.(error);
    } finally {
      running = false;
    }
  };
  void tick();
  const timer = setInterval(() => void tick(), intervalMilliseconds);
  timer.unref();
  return () => {
    stopped = true;
    clearInterval(timer);
  };
}
