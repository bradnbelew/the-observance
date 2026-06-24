/**
 * Seed normalization parity check — makes the ORACLE.md §2 authoring rule executable.
 *
 * The resolver matches a player's answer by normalizing their raw input and testing
 * whole-string set-membership against puzzles.accepted_answers. So EVERY accepted_answers
 * entry MUST already be stored in normalized form — if it isn't, that clue can never be
 * solved on either surface, and the failure is SILENT (the Watcher just stays quiet, which
 * is indistinguishable from a wrong guess). A single un-normalized seed string is therefore
 * a latent, un-debuggable dead clue.
 *
 * This script reads the canonical seed and asserts, for every accepted_answers entry, that
 * normalizeAnswer(entry) === entry (idempotent = already normalized) and that the entry is
 * non-empty. It exits non-zero on any violation so it can gate the seed before it goes live.
 *
 * Run:  npx tsx src/oracle/seedcheck.ts
 * (normalizeAnswer here is the SAME algorithm the Java AnswerNormalizer implements byte-for-
 * byte; keeping the seed clean on this side keeps both surfaces in lockstep.)
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { normalizeAnswer } from './normalize.js';

const here = dirname(fileURLToPath(import.meta.url));
// src/oracle -> ../../supabase/seeds/puzzles_seed.sql
const SEED = resolve(here, '../../supabase/seeds/puzzles_seed.sql');

const sql = readFileSync(SEED, 'utf8');

// Pull every `array[ ... ]` block (accepted_answers is the only array literal per row; the
// outcome_payload uses jsonb_build_object). Normalized answers are [a-z0-9 ] only, so they can
// never contain a `]` or a `'` — a simple, exact extraction for this seed.
const arrayBlocks = [...sql.matchAll(/array\[([^\]]*)\]/gi)]
  .map((m) => m[1])
  .filter((b): b is string => b !== undefined);

type Violation = { value: string; normalized: string; reason: string };
const violations: Violation[] = [];
let total = 0;

for (const block of arrayBlocks) {
  for (const m of block.matchAll(/'([^']*)'/g)) {
    const value = m[1];
    if (value === undefined) continue;
    total += 1;
    if (value.trim() === '') {
      violations.push({ value, normalized: '', reason: 'empty/blank answer (never matches)' });
      continue;
    }
    const normalized = normalizeAnswer(value);
    if (normalized !== value) {
      violations.push({
        value,
        normalized,
        reason: `not pre-normalized (would never match; store "${normalized}")`,
      });
    }
  }
}

if (total === 0) {
  console.error('seedcheck: FAILED — found 0 accepted_answers in the seed (extraction broke?).');
  process.exit(1);
}

if (violations.length > 0) {
  console.error(`seedcheck: FAILED — ${violations.length}/${total} accepted_answers are not seed-safe:`);
  for (const v of violations) {
    console.error(`  • "${v.value}"  →  ${v.reason}`);
  }
  process.exit(1);
}

console.log(`seedcheck: OK — all ${total} accepted_answers across ${arrayBlocks.length} puzzles are pre-normalized and non-empty.`);
