import { createHash, randomUUID } from 'node:crypto';
import type { Client } from 'discord.js';
import { config } from '../config.js';
import {
  MORROW_GROUP_RECEIPT_TEXT,
} from './contradiction.js';
import {
  activateMorrowContradiction,
  claimMorrowDiscordProjections,
  completeMorrowDiscordProjection,
} from './repo.js';
import {
  runMorrowDiscordProjectionBatch,
  type MorrowProjectionDependencies,
} from './projection-policy.js';

function projectionNonce(eventId: string): string {
  return createHash('sha256').update(`morrow:${eventId}`, 'utf8').digest('hex').slice(0, 25);
}

function productionDependencies(client: Client<true>): MorrowProjectionDependencies {
  return {
    claim: (workerId, limit, leaseSeconds) => claimMorrowDiscordProjections({
      workerId, releaseId: config.morrow.releaseId!, limit, leaseSeconds,
    }),
    activate: (claim) => activateMorrowContradiction({
      eventId: claim.eventId,
      releaseId: claim.releaseId,
      guildId: config.discord.guildId,
      channelId: config.morrow.channelId!,
      threadId: config.morrow.threadId,
    }),
    post: async (claim) => {
      const expectedChannel = config.morrow.channelId;
      const expectedThread = config.morrow.threadId;
      if (claim.releaseId !== config.morrow.releaseId || claim.channelId !== expectedChannel
          || claim.threadId !== expectedThread) return false;
      const destination = await client.channels.fetch(expectedThread ?? expectedChannel!);
      if (!destination?.isSendable()) return false;
      await destination.send({
        content: `${MORROW_GROUP_RECEIPT_TEXT}\nRelease: ${claim.releaseId} · receipt ${claim.eventId}`,
        nonce: projectionNonce(claim.eventId),
        enforceNonce: true,
        allowedMentions: { parse: [] },
      });
      return true;
    },
    complete: (claim, workerId, applied, error) => completeMorrowDiscordProjection({
      eventId: claim.eventId,
      workerId,
      releaseId: claim.releaseId,
      applied,
      error,
    }),
  };
}

/**
 * Run one bounded batch through the same service-role RPC and Discord delivery
 * dependencies used by the persistent worker. Rehearsal tooling uses this
 * entrypoint so it can prove a complete lease/activate/post/ack cycle without
 * starting an interval or any unrelated bot worker.
 */
export async function runMorrowDiscordProjectionBatchOnce(
  client: Client<true>,
  options: { workerId?: string; limit?: number; leaseSeconds?: number } = {},
): Promise<{ claimed: number; applied: number; failed: number }> {
  if (!config.morrow.enabled || !config.morrow.releaseId || !config.morrow.channelId) {
    throw new Error('Morrow projection batch refused: runtime binding is disabled or incomplete');
  }
  return runMorrowDiscordProjectionBatch(
    options.workerId ?? randomUUID(),
    productionDependencies(client),
    options.limit ?? 10,
    options.leaseSeconds ?? 45,
  );
}

export function startMorrowDiscordProjectionWorker(client: Client<true>, options: {
  intervalMilliseconds?: number;
  onError?(error: unknown): void;
} = {}): () => void {
  if (!config.morrow.enabled) return () => undefined;
  const intervalMilliseconds = options.intervalMilliseconds ?? 10_000;
  if (!Number.isInteger(intervalMilliseconds) || intervalMilliseconds < 1_000) {
    throw new Error('invalid Morrow projection interval');
  }
  const workerId = randomUUID();
  let running = false;
  let stopped = false;
  const tick = async () => {
    if (running || stopped) return;
    running = true;
    try {
      await runMorrowDiscordProjectionBatchOnce(client, { workerId });
    } catch (error) {
      options.onError?.(error);
    } finally {
      running = false;
    }
  };
  void tick();
  const timer = setInterval(() => void tick(), intervalMilliseconds);
  timer.unref();
  return () => { stopped = true; clearInterval(timer); };
}
