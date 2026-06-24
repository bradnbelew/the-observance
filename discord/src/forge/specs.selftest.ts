/**
 * specs.selftest.ts — the BUILD-TIME invariant for the authored-key → forge-spec bind
 * (COHERENCE-AUDIT X1 / P0-1). Runs the same `specsSelfTest()` the forge harness runs,
 * standalone, exiting non-zero on any violation so it can gate the build / CI.
 *
 *   npx tsx src/forge/specs.selftest.ts
 *
 * What it proves, for EVERY registered cipher node (clue-specs.ts):
 *   (1) decode(forge(spec).ciphertext) === the bound plaintext  — the carved runes
 *       really decode back (no wrong shift / renamed key / mis-indexed book);
 *   (2) normalizeAnswer(plaintext) ∈ that seed row's accepted_answers — the player's
 *       decoded answer actually matches the seed (the bind that closes A3 / B5);
 *   (3) the artifact carves to a <g> fragment, and every accepted_answers mirror is
 *       pre-normalized (ORACLE.md §2 — same rule seedcheck.ts enforces on the SQL).
 *
 * If this passes, the in-world carving and the Discord card can never silently render
 * different plaintext, and a seed edit that breaks an answer fails HERE, not at a player.
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { specsSelfTest, specsCoverageSelfTest } from './clue-specs.js';

// Resolve the canonical seed relative to THIS file (src/forge/ → ../../supabase/seeds),
// so the coverage check runs from any cwd. The seed is the source of truth; we only read
// it to prove every active row is classified (no DB, no network).
const SEED_PATH = resolve(dirname(fileURLToPath(import.meta.url)), '../../supabase/seeds/puzzles_seed.sql');

try {
  const { passed, cases } = specsSelfTest();
  const seedSql = readFileSync(SEED_PATH, 'utf8');
  const cov = specsCoverageSelfTest(seedSql);
  console.log(`clue-specs self-tests passed (${passed + cov.passed}):`);
  for (const c of [...cases, ...cov.cases]) console.log(`  ok   ${c}`);
  process.exit(0);
} catch (err) {
  console.error('clue-specs self-tests FAILED:');
  console.error(`  ${err instanceof Error ? err.message : String(err)}`);
  process.exit(1);
}
