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

/**
 * GUARD 6 — the rite token (red-team MF-8 / B-5). The plugin posts a fixed OPAQUE token when it
 * detects the Accepting (a synchronized group bow); that token must byte-match the `accepting-crouch`
 * row's accepted_answers in the seed, or the detected climax resolves to NOTHING — the worst possible
 * silent failure (the finale just doesn't happen, on camera). This asserts config.yml's
 * rites.accepting.token === the seed token. (Both are already normalized; normalize is idempotent, so
 * equality is sufficient for the match.)
 */
export function riteTokenSelfTest(configYml: string, seedSql: string): { passed: number; cases: string[] } {
  const cfg = configYml.match(/accepting:\s*[\s\S]*?token:\s*"([^"]*)"/i);
  if (!cfg) {
    throw new Error('riteTokenSelfTest: could not find rites.accepting.token in config.yml. (MF-8)');
  }
  const configToken = cfg[1]!.trim();

  const sql = seedSql.replace(/--[^\n]*/g, '');
  // Anchor on the ROW DEFINITION `( 'accepting-crouch',` — NOT a `next_puzzle_key, 'accepting-crouch'`
  // reference (which would grab the referencing row's answer array instead).
  const seed = sql.match(/\(\s*'accepting-crouch'\s*,[\s\S]*?array\s*\[\s*'([^']*)'/i);
  if (!seed) {
    throw new Error("riteTokenSelfTest: could not find the accepting-crouch row's token in the seed. (MF-8)");
  }
  const seedToken = seed[1]!.trim();

  if (configToken !== seedToken) {
    throw new Error(
      `riteTokenSelfTest: the plugin's Accepting token disagrees with the seed — the climax would ` +
        `resolve to nothing.\n  config.yml: "${configToken}"\n  seed:       "${seedToken}"\n` +
        `Keep rites.accepting.token === the accepting-crouch accepted_answer. (MF-8)`,
    );
  }
  return { passed: 1, cases: [`rite token: config.yml Accepting token matches the seed (the climax will resolve)`] };
}

/**
 * GUARD 7 — the thread tags (content integration). thread_tags.sql sets puzzles.thread_key +
 * teaches_custom. thread_key has a DB FK to threads, but teaches_custom is a plain text column with
 * NO FK — so a typo'd custom key would silently link a node to a non-existent way. This asserts every
 * tag's thread_key ∈ THREADS and every NON-NULL teaches_custom ∈ CUSTOM_KEYS, at build time.
 */
export function threadTagSelfTest(tagsSql: string): { passed: number; cases: string[] } {
  const sql = tagsSql.replace(/--[^\n]*/g, '');
  const threadVals = [...sql.matchAll(/thread_key\s*=\s*'([a-z_]+)'/g)].map((m) => m[1]!);
  const customVals = [...sql.matchAll(/teaches_custom\s*=\s*'([a-z_]+)'/g)].map((m) => m[1]!); // quoted ⇒ skips null
  if (threadVals.length === 0) {
    throw new Error('threadTagSelfTest: parsed 0 thread_key tags — parser or thread_tags.sql broke.');
  }
  const threadSet = new Set<string>(THREADS);
  const customSet = new Set<string>(CUSTOM_KEYS);
  const badThreads = [...new Set(threadVals.filter((t) => !threadSet.has(t)))];
  if (badThreads.length > 0) {
    throw new Error(`threadTagSelfTest: thread_key value(s) not in THREADS: ${badThreads.join(', ')}.`);
  }
  const badCustoms = [...new Set(customVals.filter((c) => !customSet.has(c)))];
  if (badCustoms.length > 0) {
    throw new Error(
      `threadTagSelfTest: teaches_custom value(s) not in CUSTOM_KEYS (no FK guards this): ` +
        `${badCustoms.join(', ')}. Use a canonical the_-prefixed key.`,
    );
  }
  return {
    passed: 1,
    cases: [`thread tags: ${threadVals.length} thread_key + ${customVals.length} teaches_custom values all canonical`],
  };
}

/**
 * GUARD 9 — thread-card voice coverage (content integration). Every thread_cards row names a
 * body_voice_key (a camelCase cardXxx key); the Recovery Archive renders that key's text. A key with
 * no entry in voice.archive.ts (archive | npcLines) = a BLANK card on camera. This asserts every
 * body_voice_key the seed references is defined. `definedKeys` is Object.keys of both archive maps
 * (passed by the runner — exact, no regex on the TS).
 */
export function threadCardVoiceCoverageSelfTest(
  threadCardsSql: string,
  definedKeys: Set<string>,
): { passed: number; cases: string[] } {
  const sql = threadCardsSql.replace(/--[^\n]*/g, '');
  // body_voice_keys are camelCase 'cardXxx' literals; card_keys are hyphenated ('who-deep-market'),
  // titles are prose — neither matches, so this isolates the body keys cleanly.
  const referenced = [...new Set([...sql.matchAll(/'(card[A-Z][A-Za-z0-9]*)'/g)].map((m) => m[1]!))];
  if (referenced.length === 0) {
    throw new Error('threadCardVoiceCoverageSelfTest: parsed 0 body_voice_keys — parser or seed broke.');
  }
  const missing = referenced.filter((k) => !definedKeys.has(k));
  if (missing.length > 0) {
    throw new Error(
      `threadCardVoiceCoverageSelfTest: ${missing.length} thread_cards body_voice_key(s) have NO entry ` +
        `in voice.archive.ts (blank card on camera): ${missing.join(', ')}. Add them to archive/npcLines.`,
    );
  }
  return {
    passed: 1,
    cases: [`thread-card voice coverage: all ${referenced.length} body_voice_keys defined in voice.archive.ts`],
  };
}

/**
 * GUARD 10 — Watcher register discipline (audit, immersion). The register law (voice.ts) was
 * doc-only; this MECHANIZES it for the archive corpus: every Watcher-voice `archive` line must be
 * lowercase (no A-Z), carry no exclamation mark, and never break character with a meta-word
 * (ai/bot/game/server/minecraft/discord). Pass the `archive` map (NOT npcLines — those are SET-A
 * human voice and intentionally use capitals/contractions). Catches a register slip before camera.
 */
export function registerDisciplineSelfTest(archive: Record<string, string>): { passed: number; cases: string[] } {
  const META = ['ai', 'bot', 'game', 'server', 'minecraft', 'discord'] as const;
  const violations: string[] = [];
  const entries = Object.entries(archive);
  if (entries.length === 0) {
    throw new Error('registerDisciplineSelfTest: archive is empty — wrong import?');
  }
  for (const [key, line] of entries) {
    if (/[A-Z]/.test(line)) violations.push(`${key}: uppercase letter (register is lowercase)`);
    if (line.includes('!')) violations.push(`${key}: exclamation mark (register has none)`);
    for (const w of META) {
      if (new RegExp(`\\b${w}\\b`, 'i').test(line)) violations.push(`${key}: meta-word "${w}" breaks character`);
    }
  }
  if (violations.length > 0) {
    throw new Error(
      `registerDisciplineSelfTest: ${violations.length} Watcher register violation(s):\n  ` +
        violations.slice(0, 12).join('\n  '),
    );
  }
  return {
    passed: 1,
    cases: [`register discipline: all ${entries.length} Watcher archive lines lowercase, no exclaim, in character`],
  };
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
