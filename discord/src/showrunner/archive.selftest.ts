/**
 * archive.selftest.ts — pins the PURE archive body resolver (archive.ts). No DB, no network.
 *
 *   npm run showrunner:test:archive
 *
 * Proves: a known key resolves to its authored body; an unknown/null key is SKIPPED (never a placeholder
 * or a leaked identifier); order is preserved (deterministic); and — the load-bearing coverage check —
 * every real `body_voice_key` named by the live thread_cards seed resolves through voice.archive.ts, so
 * the materializer can never silently drop an authored card. (The SQL/spec lane's GUARD-9 checks the same
 * coverage from the canon side; this checks it from the resolver's side, end to end.)
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { buildArchiveBodies } from './archive.js';
import { archiveLine, npcLine } from '../voice.archive.js';

/** Resolve a card body through the same dual-register path the materializer uses (Watcher then SET-A). */
const resolveBody = (k: string): string | null => archiveLine(k) ?? npcLine(k);

const here = dirname(fileURLToPath(import.meta.url));
let failures = 0;
function ok(cond: boolean, label: string): void {
  console.log(`  ${cond ? 'ok  ' : 'FAIL'} ${label}`);
  if (!cond) failures++;
}

// 1. A known key resolves to a real body; an unknown key + a null key are both skipped.
const mixed = buildArchiveBodies([
  { card_key: 'k-known', body_voice_key: 'cardWhoVaunCounted' },
  { card_key: 'k-unknown', body_voice_key: 'no-such-key-xyz' },
  { card_key: 'k-null', body_voice_key: null },
]);
ok(mixed.length === 1, 'unknown + null keys are skipped, known key kept (1 of 3)');
ok(mixed[0]?.card_key === 'k-known' && mixed[0].body.length > 0, 'known key resolves to a non-empty body');
ok(mixed[0]?.body === archiveLine('cardWhoVaunCounted'), 'body matches the authored voice-archive line');

// 2. Determinism + order preservation.
const a = buildArchiveBodies([
  { card_key: 'a', body_voice_key: 'cardWhoMaraRead' },
  { card_key: 'b', body_voice_key: 'cardPlaceCameDown' },
]);
const b = buildArchiveBodies([
  { card_key: 'a', body_voice_key: 'cardWhoMaraRead' },
  { card_key: 'b', body_voice_key: 'cardPlaceCameDown' },
]);
ok(JSON.stringify(a) === JSON.stringify(b), 'deterministic — same input, same output');
ok(a[0]?.card_key === 'a' && a[1]?.card_key === 'b', 'input order preserved');

// 3. COVERAGE — every body_voice_key the live thread_cards seed names resolves. Parse the seed for the
//    body_voice_key column value in each row (the 4th positional value in the insert tuples).
const seed = readFileSync(resolve(here, '../../supabase/seeds/thread_cards.sql'), 'utf8');
// Each row is: ( 'card_key','thread_key','title','body_voice_key', ... ). Grab the 4th quoted value.
const keys: string[] = [];
const rowRe = /\(\s*'[^']*'\s*,\s*'[^']*'\s*,\s*'[^']*'\s*,\s*'([^']+)'/g;
let m: RegExpExecArray | null;
while ((m = rowRe.exec(seed)) !== null) keys.push(m[1]!);
ok(keys.length >= 40, `seed body_voice_keys parsed (${keys.length} found; expected ~42)`);
const unresolved = keys.filter((k) => resolveBody(k) == null);
ok(unresolved.length === 0, `every seed body_voice_key resolves in some register (${unresolved.length} unresolved)`);
if (unresolved.length) console.log('    unresolved:', unresolved.join(', '));

if (failures === 0) console.log('\narchive.selftest: OK — the archive body resolver + full seed coverage hold.');
else { console.error(`\narchive.selftest: ${failures} FAILURE(S)`); process.exit(1); }
