/**
 * canon.ts — THE CANON REGISTRY + COHERENCE GUARDS (red-team §5 "coherence machinery").
 *
 * Generalizes the X1 forge-spec bind (clue-specs.ts) into the closed sets everything keys off,
 * plus the build-time self-tests that turn a coherence drift into a FAILING BUILD instead of a
 * silent zero-row join on camera. Pure (no DB / no I/O): every function takes file contents as a
 * string; the runner (specs.selftest.ts) reads the files. Closes red-team blockers B-3, B-5, B-6
 * permanently.
 */
import { parseSeedKeys } from './clue-specs.js';

// ---------------------------------------------------------------------------
// THE CANON — the closed registries. Anything that keys off one of these must be a member.
// ---------------------------------------------------------------------------

/** The seven ways. CANONICAL form is `the_`-prefixed — matches TrackerConfig.java + voice.ts.
 *  (Red-team B-3: the docs drifted to unprefixed; the code is canonical, and this guards it.) */
export const CUSTOM_KEYS = [
  'the_kept_light',
  'the_deep_line',
  'the_dark_hours',
  'the_offering',
  'the_bow',
  'the_unspoken',
  'the_sacred_beast',
] as const;

/** The six keepers (the Kept), as canon names. */
export const KEEPERS = ['vaun', 'mara', 'sella', 'orin', 'brann', 'iss'] as const;

/** The five reconstruction threads (Recovery Archive). */
export const THREADS = ['who', 'place', 'happened', 'surface', 'human'] as const;

/** Nodes that MUST be reachable via a routing edge (red herrings the player is steered to).
 *  A node here with zero inbound `next_puzzle_key` edges is the B-6 orphan — a build failure. */
const REQUIRE_INBOUND = ['iss-dead-shrine'] as const;

/** Substrings an `accepted_answers` value must NEVER contain — they leak that a row is a
 *  plugin sentinel / placeholder, inviting a spoof of a detected rite (red-team B-5). */
const SENTINEL_LEAK_WORDS = ['sentinel', 'posted only by plugin', 'placeholder', 'todo'] as const;

// ---------------------------------------------------------------------------
// Parsing helpers (best-effort, comment-stripped — the seed stays source of truth).
// ---------------------------------------------------------------------------

/** Every `'next_puzzle_key', '<key>'` edge target in the seed (comments stripped first). */
function parseNextEdges(seedSql: string): string[] {
  const sql = seedSql.replace(/--[^\n]*/g, '');
  return [...sql.matchAll(/'next_puzzle_key'\s*,\s*'([a-z0-9-]+)'/g)].map((m) => m[1]!);
}

/** Every quoted string inside an `array[ ... ]` block = an accepted_answers value. */
function parseAnswerValues(seedSql: string): string[] {
  const out: string[] = [];
  for (const block of seedSql.matchAll(/array\s*\[([^\]]*)\]/gi)) {
    for (const m of block[1]!.matchAll(/'([^']*)'/g)) out.push(m[1]!);
  }
  return out;
}

// ---------------------------------------------------------------------------
// GUARD 1 — graph reachability (red-team B-6). Every edge resolves; required herrings are reachable.
// ---------------------------------------------------------------------------

export function reachabilitySelfTest(seedSql: string): { passed: number; cases: string[] } {
  const { all } = parseSeedKeys(seedSql);
  const keys = new Set(all);
  const edges = parseNextEdges(seedSql);

  // (a) no dangling edge — every next_puzzle_key points at a real row.
  const dangling = [...new Set(edges.filter((t) => !keys.has(t)))];
  if (dangling.length > 0) {
    throw new Error(
      `reachabilitySelfTest: ${dangling.length} next_puzzle_key edge(s) point at a non-existent ` +
        `row: ${dangling.join(', ')}. Wire or remove them.`,
    );
  }

  // (b) every REQUIRE_INBOUND node has >= 1 inbound edge (the B-6 orphan guard).
  const inbound = new Map<string, number>();
  for (const t of edges) inbound.set(t, (inbound.get(t) ?? 0) + 1);
  const orphans = REQUIRE_INBOUND.filter((k) => (inbound.get(k) ?? 0) === 0);
  if (orphans.length > 0) {
    throw new Error(
      `reachabilitySelfTest: red-herring node(s) with ZERO inbound edge (unreachable): ` +
        `${orphans.join(', ')}. A node nothing routes to is dead content. (B-6)`,
    );
  }
  return {
    passed: 1,
    cases: [
      `reachability: ${edges.length} edges all resolve; ${REQUIRE_INBOUND.length} required ` +
        `red-herring(s) reachable (iss-dead-shrine inbound=${inbound.get('iss-dead-shrine') ?? 0})`,
    ],
  };
}

// ---------------------------------------------------------------------------
// GUARD 2 — no leaked sentinel (red-team B-5). A detected-rite token must read as opaque.
// ---------------------------------------------------------------------------

export function noLeakedSentinelSelfTest(seedSql: string): { passed: number; cases: string[] } {
  const answers = parseAnswerValues(seedSql);
  if (answers.length === 0) {
    throw new Error('noLeakedSentinelSelfTest: parsed 0 accepted_answers — parser or seed broke.');
  }
  const leaks = answers.filter((a) =>
    SENTINEL_LEAK_WORDS.some((w) => a.toLowerCase().includes(w)),
  );
  if (leaks.length > 0) {
    throw new Error(
      `noLeakedSentinelSelfTest: ${leaks.length} accepted_answers value(s) describe themselves as ` +
        `a sentinel/placeholder (spoofable on camera): ${leaks.map((l) => `"${l}"`).join(', ')}. ` +
        `Replace with an opaque, wordless token the plugin posts on real detection. (B-5)`,
    );
  }
  return { passed: 1, cases: [`no leaked sentinels: ${answers.length} answers scanned, 0 self-describing`] };
}

// ---------------------------------------------------------------------------
// GUARD 3 — custom-key namespace (red-team B-3). The code's seven keys are the canonical set.
// ---------------------------------------------------------------------------

export function customKeyNamespaceSelfTest(
  trackerConfigJava: string,
  voiceTs: string,
): { passed: number; cases: string[] } {
  for (const key of CUSTOM_KEYS) {
    if (!trackerConfigJava.includes(key)) {
      throw new Error(
        `customKeyNamespaceSelfTest: canonical custom key "${key}" not found in TrackerConfig.java ` +
          `(namespace drift; the registry and the plugin disagree). (B-3)`,
      );
    }
    if (!voiceTs.includes(key)) {
      throw new Error(
        `customKeyNamespaceSelfTest: canonical custom key "${key}" not found in voice.ts ` +
          `(namespace drift; the registry and the bot disagree). (B-3)`,
      );
    }
  }
  // No UNPREFIXED compound form leaking as a quoted key in the code (the exact B-3 drift).
  for (const key of CUSTOM_KEYS) {
    const bare = key.slice('the_'.length); // e.g. kept_light
    if (!bare.includes('_')) continue; // skip single words (bow/offering) — too ambiguous to flag
    for (const [file, src] of [['TrackerConfig.java', trackerConfigJava], ['voice.ts', voiceTs]] as const) {
      if (new RegExp(`(?<!the_)\\b${bare}\\b`).test(src)) {
        throw new Error(
          `customKeyNamespaceSelfTest: unprefixed "${bare}" appears in ${file} — use the canonical ` +
            `"${key}". (B-3)`,
        );
      }
    }
  }
  return { passed: 1, cases: [`custom-key namespace: all ${CUSTOM_KEYS.length} the_-prefixed keys present in TrackerConfig + voice.ts; no unprefixed drift`] };
}
