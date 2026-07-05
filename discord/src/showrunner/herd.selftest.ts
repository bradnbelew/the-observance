/**
 * herd.selftest.ts — the pure self-test the herd-conversion doc (design/ideas/herd-conversion.md
 * §4.5 item 3) promises. No DB, no network, no clock: it imports herd.ts's `paceHerd` with nothing
 * and pins the LAWS the pacer exists to honor —
 *   - MONOTONE: the target never drops, call after call, movement after movement;
 *   - CAP: the target never exceeds capCount (default 16), however far past it priorPaleCount claims;
 *   - IDEMPOTENT: the same window (movement + passDoneThisWindow=true) never adds twice;
 *   - START GATE: no spread before startMovement (M1 stays the single beast only);
 *   - determinism: same input twice → identical decision.
 *
 * PRECISION LAW (design doc §2.3 / INV-13, the assertion the doc explicitly calls out): the spread
 * pass must NEVER emit a `sacred_beast` / `sacred_fork_arm` tag, only `pale_cosmetic`. `paceHerd`
 * itself is PDC-agnostic (it only computes a count), so this file also textually guards the two
 * places that DO touch PDC tags — SacredAnimalBeat.java's spread branch and the herd_spread enqueue
 * payload in autonomy.run.ts — so a regression that starts writing sacred_beast/sacred_fork_arm from
 * the spread path, or that stops forcing mode:"spread", fails the build here rather than in-world.
 *
 * Runs standalone and exits non-zero on any failed assertion so it gates the build beside decide /
 * customs / prologue / autonomy:
 *   npx tsx src/showrunner/herd.selftest.ts   (or: npm run showrunner:test:herd)
 */
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { paceHerd, HERD_DEFAULTS, type HerdInput } from './herd.js';

let failures = 0;
function check(label: string, cond: boolean): void {
  if (cond) console.log(`  ok   ${label}`);
  else { failures += 1; console.error(`  FAIL ${label}`); }
}

function inp(over: Partial<HerdInput> = {}): HerdInput {
  return { movement: 2, priorPaleCount: 0, passDoneThisWindow: false, ...over };
}

// ===========================================================================
// paceHerd — pure decision: monotone, capped, idempotent, start-gated, deterministic.
// ===========================================================================

// 1. START GATE: before startMovement, no spread — M1 is the single beast only.
{
  const d = paceHerd(inp({ movement: HERD_DEFAULTS.startMovement - 1, priorPaleCount: 1 }));
  check('before start movement → no spread, target unchanged', !d.spread && d.addThisPass === 0 && d.paleTarget === 1);
}

// 2. MONOTONE: the target only ever rises, never drops, across a run of passes.
{
  let prior = 0;
  let sawDrop = false;
  const movements = [2, 2, 3, 3, 4, 4, 5, 5];
  for (const movement of movements) {
    const d = paceHerd(inp({ movement, priorPaleCount: prior, passDoneThisWindow: false }));
    if (d.paleTarget < prior) sawDrop = true;
    prior = d.paleTarget;
  }
  check('monotone: target never drops across a sequence of passes', !sawDrop);
  check('monotone: target actually advanced over the run', prior > 0);
}

// 3. CAP: the target never exceeds capCount, even asked to add past it.
{
  const atCap = paceHerd(inp({ movement: 5, priorPaleCount: HERD_DEFAULTS.capCount }));
  check('at cap → no further spread', !atCap.spread && atCap.paleTarget === HERD_DEFAULTS.capCount);
  const overCap = paceHerd(inp({ movement: 5, priorPaleCount: HERD_DEFAULTS.capCount + 50 }));
  check('priorPaleCount past cap → clamped, never exceeds cap', overCap.paleTarget <= HERD_DEFAULTS.capCount);
  const nearCap = paceHerd(inp({ movement: 5, priorPaleCount: HERD_DEFAULTS.capCount - 1 }));
  check('one below cap → adds exactly the remainder, lands ON the cap', nearCap.paleTarget === HERD_DEFAULTS.capCount);
}

// 4. IDEMPOTENT: same window (passDoneThisWindow=true) → addThisPass 0, no re-add.
{
  const d = paceHerd(inp({ movement: 3, priorPaleCount: 4, passDoneThisWindow: true }));
  check('pass already done this window → addThisPass 0', d.addThisPass === 0);
  check('pass already done this window → no spread', !d.spread);
  check('pass already done this window → target unchanged', d.paleTarget === 4);
}

// 5. one-per-pass: a single pass never adds more than addPerPass.
{
  const d = paceHerd(inp({ movement: 3, priorPaleCount: 0, passDoneThisWindow: false }));
  check('one-per-pass: addThisPass <= addPerPass', d.addThisPass <= HERD_DEFAULTS.addPerPass);
}

// 6. determinism: same input twice → identical decision.
{
  const i = inp({ movement: 3, priorPaleCount: 2 });
  check('deterministic', JSON.stringify(paceHerd(i)) === JSON.stringify(paceHerd(i)));
}

// ===========================================================================
// PRECISION LAW (design doc §2.3 / INV-13): the spread pass is pale_cosmetic-ONLY.
// paceHerd's own decision surface never mentions the sacred/fork-arm tags — a
// regression that starts threading them through the pacer's decision shape
// would show up here as an unexpected key.
// ===========================================================================
{
  const d = paceHerd(inp({ movement: 3, priorPaleCount: 1 }));
  const keys = Object.keys(d);
  check('paceHerd decision has no sacred/fork-arm-shaped field',
    !keys.some((k) => /sacred|fork_arm|forkArm/i.test(k)));
}

// ===========================================================================
// STATIC GUARDS — the two real PDC-touching call sites stay wired the way this
// selftest's precision assumptions depend on. No server / Bukkit needed: these
// are textual checks against the source, mirroring forge/specs.selftest.ts's guard idiom.
// ===========================================================================
{
  const here = path.dirname(fileURLToPath(import.meta.url));
  const runTs = readFileSync(path.join(here, 'autonomy.run.ts'), 'utf8');
  check('autonomy.run.ts still enqueues the herd_spread beat type',
    /enqueueBeat\(\s*'herd_spread'/.test(runTs));
  check('autonomy.run.ts anchors herd_spread at the shared herd_anchor site',
    /'herd_anchor'/.test(runTs));

  const pluginRoot = path.join(here, '..', '..', '..', 'plugin', 'src', 'main', 'java', 'com',
    'observance', 'watcher', 'beats', 'lib');
  const sacredBeatJava = readFileSync(path.join(pluginRoot, 'SacredAnimalBeat.java'), 'utf8');
  // Isolate the applyPaleTag method body — the ONLY place the spread path writes a PDC tag.
  const tagStart = sacredBeatJava.indexOf('private static void applyPaleTag');
  const tagEnd = sacredBeatJava.indexOf('\n    }', tagStart);
  const applyPaleTagBody = sacredBeatJava.slice(tagStart, tagEnd);
  check('SacredAnimalBeat.applyPaleTag exists (the spread tag-writer)', tagStart !== -1);
  check('applyPaleTag sets pale_cosmetic', /"pale_cosmetic"/.test(applyPaleTagBody));
  check('applyPaleTag NEVER sets sacred_beast', !/"sacred_beast"/.test(applyPaleTagBody));
  check('applyPaleTag NEVER sets sacred_fork_arm', !/"sacred_fork_arm"/.test(applyPaleTagBody));
  check('applyPaleTag NEVER calls setGlowing(true)', !/setGlowing\(\s*true\s*\)/.test(applyPaleTagBody));

  const herdSpreadBeatJava = readFileSync(path.join(pluginRoot, 'HerdSpreadBeat.java'), 'utf8');
  check('HerdSpreadBeat forces mode:"spread"', /addProperty\(\s*"mode"\s*,\s*"spread"\s*\)/.test(herdSpreadBeatJava));
}

if (failures > 0) {
  console.error(`\nshowrunner herd: FAILED — ${failures} assertion(s)`);
  process.exit(1);
}
console.log('\nshowrunner herd: OK — all assertions passed.');
