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

/**
 * GUARD 5 — site coverage (red-team B-4). Every `site_id` a seed beat targets must exist AND
 * be `enabled: true` in sites.yml, or the beat pastes to a null site and silently no-ops on
 * camera. (Coords may still be placeholder/null — that's a go-live in-game step the plugin
 * skips safely; this guards EXISTENCE + enabled, the part authorable now.)
 */
export function siteCoverageSelfTest(seedSql: string, sitesYml: string): { passed: number; cases: string[] } {
  // Site ids the seed's beats target.
  const noComments = seedSql.replace(/--[^\n]*/g, '');
  const referenced = [
    ...new Set([...noComments.matchAll(/'site_id'\s*,\s*'([a-z0-9_]+)'/g)].map((m) => m[1]!)),
  ];

  // Enabled site keys: scan from `sites:` onward; a site key is a 2-space `key:`; its block's
  // `enabled: true/false` (4-space) records the flag.
  const fromSites = sitesYml.slice(Math.max(0, sitesYml.search(/^sites:/m)));
  const enabled = new Set<string>();
  const defined = new Set<string>();
  let current: string | null = null;
  for (const line of fromSites.split(/\r?\n/)) {
    const siteKey = line.match(/^ {2}([a-z0-9_]+):\s*$/);
    if (siteKey) {
      current = siteKey[1]!;
      defined.add(current);
      continue;
    }
    const en = line.match(/^ {4}enabled:\s*(true|false)/);
    if (en && current && en[1] === 'true') enabled.add(current);
  }

  const missing = referenced.filter((s) => !defined.has(s));
  if (missing.length > 0) {
    throw new Error(
      `siteCoverageSelfTest: ${missing.length} seed site_id(s) have NO entry in sites.yml: ` +
        `${missing.join(', ')}. Add each (placeholder coords ok) or the beat no-ops. (B-4)`,
    );
  }
  const disabled = referenced.filter((s) => defined.has(s) && !enabled.has(s));
  if (disabled.length > 0) {
    throw new Error(
      `siteCoverageSelfTest: ${disabled.length} seed site_id(s) exist but are enabled:false: ` +
        `${disabled.join(', ')}. Enable them. (B-4)`,
    );
  }
  return {
    passed: 1,
    cases: [`site coverage: all ${referenced.length} seed site_id(s) exist + enabled in sites.yml (${referenced.join(', ')})`],
  };
}

/**
 * GUARD 4 — the thread layer (red-team B-7). The code's THREADS registry must equal the
 * five threads SEEDED in migration 0005, so the Recovery Archive's columns can't drift from
 * the canon. (The DB FK on puzzles.thread_key / thread_cards.thread_key enforces canonicality
 * at runtime; this enforces the code↔migration agreement at build time, BEFORE any thread
 * content is authored.) Pass the 0005 migration SQL.
 */
export function threadRegistrySelfTest(migration0005Sql: string): { passed: number; cases: string[] } {
  const sql = migration0005Sql.replace(/--[^\n]*/g, '');
  // The seed block: insert into public.threads (...) values ('who', …), ('place', …) …
  const m = sql.match(/insert\s+into\s+public\.threads[\s\S]*?values([\s\S]*?)on\s+conflict/i);
  if (!m) {
    throw new Error('threadRegistrySelfTest: could not find the threads seed in 0005 (migration changed?).');
  }
  // First quoted string of each `( '<key>', …)` tuple is the thread_key.
  const seeded = [...m[1]!.matchAll(/\(\s*'([a-z_]+)'\s*,/g)].map((x) => x[1]!);
  const seededSet = new Set(seeded);
  const canon = new Set<string>(THREADS);
  const missing = [...canon].filter((t) => !seededSet.has(t));
  const extra = [...seededSet].filter((t) => !canon.has(t));
  if (missing.length || extra.length) {
    throw new Error(
      `threadRegistrySelfTest: THREADS registry vs 0005 seed mismatch — ` +
        `missing in migration: [${missing.join(', ')}]; extra in migration: [${extra.join(', ')}]. ` +
        `Keep forge/canon.ts THREADS and the 0005 threads seed identical. (B-7)`,
    );
  }
  return { passed: 1, cases: [`thread registry: all ${THREADS.length} threads match the 0005 seed (who/place/happened/surface/human)`] };
}

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
