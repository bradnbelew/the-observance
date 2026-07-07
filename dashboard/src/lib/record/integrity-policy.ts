// integrity-policy.ts - pure timing policy for the Record terminal's integrity log.
//
// Kept outside integrity.ts so the spoiler/difficulty policy can be self-tested without importing the
// server-only Supabase client. The terminal may guide a stalled group, but it must not hand out near-plain
// hints just because the archive has opened before anyone has advanced the web.

/** How long (ms) a thread must be stalled before each integrity tier is willing to surface. */
export const TIER_STALL_MS: Readonly<Record<number, number>> = {
  1: 0,
  2: 24 * 3_600_000,
  3: 72 * 3_600_000,
};

export const MAX_TIER = 3;

/** Coarse human stall label from a duration in ms. */
export function stallLabel(ms: number): string {
  if (ms <= 0) return "unresolved";
  const days = Math.floor(ms / 86_400_000);
  if (days >= 1) return `stalled ${days}d`;
  const hours = Math.floor(ms / 3_600_000);
  if (hours >= 1) return `stalled ${hours}h`;
  return "stalled <1h";
}

/** The highest tier the elapsed stall has earned (1..MAX_TIER). */
export function earnedTier(stallMs: number): number {
  let t = 1;
  for (let k = 2; k <= MAX_TIER; k++) {
    if (stallMs >= (TIER_STALL_MS[k] ?? Infinity)) t = k;
  }
  return t;
}

/**
 * Derive the stall window from the web's last advance. No prior solve is NOT a three-day stall: it is
 * just unresolved, so the terminal starts at tier 1 and only escalates after the group has made progress.
 */
export function stallMsSinceLastAdvance(lastAdvanceMs: number, now: number): number {
  if (lastAdvanceMs <= 0) return 0;
  return Math.max(0, now - lastAdvanceMs);
}
