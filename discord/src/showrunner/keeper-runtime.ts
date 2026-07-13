/** Pure runtime helpers for keeper.run.ts (kept DB-free for build-gating tests). */
import { dominantHabit, type ObservationDossier } from './reports.js';
import type { KeeperId } from './keeper.js';

const HABIT_KEEPER = {
  hoards: 'vaun',
  reads: 'mara',
  wanders: 'sella',
  silent: 'orin',
  'night-walks': 'brann',
  'spends-words': 'iss',
} as const satisfies Readonly<Record<string, KeeperId>>;

export interface CustomCount {
  customKey: string;
  honoredCount: number;
  violatedCount: number;
}

/** Apply the same confidence floor as personalized reports, then map the chorus habit to its Keeper. */
export function keeperRhyme(dossier: ObservationDossier | null): KeeperId | null {
  if (!dossier) return null;
  const habit = dominantHabit(dossier);
  return habit == null ? null : HABIT_KEEPER[habit];
}

/** Pick one currently-unmended custom deterministically; no violation means no accusation. */
export function selectBrokenCustom(rows: readonly CustomCount[]): CustomCount | null {
  return rows
    .filter((r) => r.violatedCount > r.honoredCount && r.violatedCount > 0)
    .sort((a, b) =>
      (b.violatedCount - b.honoredCount) - (a.violatedCount - a.honoredCount)
      || b.violatedCount - a.violatedCount
      || a.customKey.localeCompare(b.customKey))[0] ?? null;
}

/** Bind the only dynamic Keeper token to a real, bounded log summary. */
export function renderKeeperLine(template: string, firstBeat: string | null): string {
  if (!template.includes('{{first_beat}}')) return template;
  const safe = (firstBeat ?? 'the first entry')
    .replace(/[\r\n\t]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 120);
  return template.replaceAll('{{first_beat}}', safe || 'the first entry');
}
