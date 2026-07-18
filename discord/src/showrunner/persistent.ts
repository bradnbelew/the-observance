import { runShowrunnerTick, type ShowrunnerTickResult } from './run.js';
import { normalizeLeaseSeconds, normalizeTickMs } from './persistent-policy.js';
export {
  DEFAULT_SHOWRUNNER_TICK_MS,
  MIN_SHOWRUNNER_TICK_MS,
  MAX_SHOWRUNNER_TICK_MS,
  normalizeLeaseSeconds,
  normalizeTickMs,
} from './persistent-policy.js';

export interface PersistentShowrunnerOptions {
  tickMs?: number;
  leaseSeconds?: number;
  run?: () => Promise<ShowrunnerTickResult>;
  onError?: (error: unknown) => void;
}

/**
 * Start the worker-owned showrunner pulse. The in-process guard prevents interval pileups; the SQL
 * lease prevents overlap with another worker or the recovery cron. The first pulse is immediate.
 */
export function startPersistentShowrunner(options: PersistentShowrunnerOptions = {}): () => void {
  const tickMs = normalizeTickMs(options.tickMs ?? process.env.SHOWRUNNER_TICK_MS);
  const leaseSeconds = normalizeLeaseSeconds(options.leaseSeconds ?? process.env.SHOWRUNNER_LEASE_SECONDS);
  const run = options.run ?? (() => runShowrunnerTick({ leaseSeconds }));
  let stopped = false;
  let inFlight = false;

  const pulse = async (): Promise<void> => {
    if (stopped || inFlight) return;
    inFlight = true;
    try {
      await run();
    } catch (error) {
      options.onError?.(error);
    } finally {
      inFlight = false;
    }
  };

  void pulse();
  const timer = setInterval(() => void pulse(), tickMs);
  return () => {
    stopped = true;
    clearInterval(timer);
  };
}
