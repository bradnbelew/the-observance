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
