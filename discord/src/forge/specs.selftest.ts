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
import {
  reachabilitySelfTest,
  noLeakedSentinelSelfTest,
  customKeyNamespaceSelfTest,
  threadRegistrySelfTest,
  siteCoverageSelfTest,
  riteTokenSelfTest,
  threadTagSelfTest,
  threadCardVoiceCoverageSelfTest,
} from './canon.js';
import { archive, npcLines } from '../voice.archive.js';

// Resolve canon files relative to THIS file (src/forge/), so the checks run from any cwd.
// The seed/plugin/voice are the sources of truth; we only READ them to prove coherence.
const here = dirname(fileURLToPath(import.meta.url));
const SEED_PATH = resolve(here, '../../supabase/seeds/puzzles_seed.sql');
const VOICE_PATH = resolve(here, '../voice.ts');
const TRACKER_PATH = resolve(
  here,
  '../../../plugin/src/main/java/com/observance/watcher/signal/TrackerConfig.java',
);
const MIGRATION_0005_PATH = resolve(here, '../../supabase/migrations/0005_threads.sql');
const SITES_PATH = resolve(here, '../../../plugin/src/main/resources/sites.yml');
const CONFIG_PATH = resolve(here, '../../../plugin/src/main/resources/config.yml');
const THREAD_TAGS_PATH = resolve(here, '../../supabase/seeds/thread_tags.sql');
const THREAD_CARDS_PATH = resolve(here, '../../supabase/seeds/thread_cards.sql');

try {
  const { cases } = specsSelfTest();
  const seedSql = readFileSync(SEED_PATH, 'utf8');
  const cov = specsCoverageSelfTest(seedSql);
  // Canon coherence guards (red-team §5): reachability (B-6), no-leaked-sentinel (B-5),
  // custom-key namespace (B-3). Each throws on drift → the build fails, not the camera.
  const reach = reachabilitySelfTest(seedSql);
  const sentinel = noLeakedSentinelSelfTest(seedSql);
  const namespace = customKeyNamespaceSelfTest(
    readFileSync(TRACKER_PATH, 'utf8'),
    readFileSync(VOICE_PATH, 'utf8'),
  );
  const threads = threadRegistrySelfTest(readFileSync(MIGRATION_0005_PATH, 'utf8'));
  const sites = siteCoverageSelfTest(seedSql, readFileSync(SITES_PATH, 'utf8'));
  const rite = riteTokenSelfTest(readFileSync(CONFIG_PATH, 'utf8'), seedSql);
  const tags = threadTagSelfTest(readFileSync(THREAD_TAGS_PATH, 'utf8'));
  const definedVoiceKeys = new Set<string>([...Object.keys(archive), ...Object.keys(npcLines)]);
  const cards = threadCardVoiceCoverageSelfTest(readFileSync(THREAD_CARDS_PATH, 'utf8'), definedVoiceKeys);
  const all = [
    ...cases, ...cov.cases, ...reach.cases, ...sentinel.cases, ...namespace.cases,
    ...threads.cases, ...sites.cases, ...rite.cases, ...tags.cases, ...cards.cases,
  ];
  console.log(`clue-specs + canon self-tests passed (${all.length}):`);
  for (const c of all) console.log(`  ok   ${c}`);
  process.exit(0);
} catch (err) {
  console.error('clue-specs + canon self-tests FAILED:');
  console.error(`  ${err instanceof Error ? err.message : String(err)}`);
  process.exit(1);
}
