import { runShowrunnerTick, type ShowrunnerTickResult } from './run.js';

export const DEFAULT_SHOWRUNNER_TICK_MS = 12_000;
export const MIN_SHOWRUNNER_TICK_MS = 10_000;
export const MAX_SHOWRUNNER_TICK_MS = 15_000;

export function normalizeTickMs(raw: string | number | undefined): number {
  const parsed = typeof raw === 'number' ? raw : Number(raw);
  if (!Number.isFinite(parsed)) return DEFAULT_SHOWRUNNER_TICK_MS;
  return Math.max(MIN_SHOWRUNNER_TICK_MS, Math.min(MAX_SHOWRUNNER_TICK_MS, Math.trunc(parsed)));
}

export function normalizeLeaseSeconds(raw: string | number | undefined): number {
  const parsed = typeof raw === 'number' ? raw : Number(raw);
  if (!Number.isFinite(parsed)) return 300;
  return Math.max(60, Math.min(900, Math.trunc(parsed)));
}

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
