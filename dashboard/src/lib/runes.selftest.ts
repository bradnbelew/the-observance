// runes.selftest.ts - keeps the web rune renderer locked to the canonical ARG alphabet.
//
// The dashboard intentionally carries a tiny geometry-only port of the Discord/resource-pack rune
// alphabet so Record headers can render with no Node-only imports. This test makes that copy honest:
// if the canonical alphabet changes, the web port must change in the same commit.

import {
  GLYPH_GAP as CANONICAL_GLYPH_GAP,
  GLYPH_H as CANONICAL_GLYPH_H,
  GLYPH_W as CANONICAL_GLYPH_W,
  SUPPORTED_CHARS,
  glyphStrokes as canonicalGlyphStrokes,
  hasGlyph as canonicalHasGlyph,
  runesWidth as canonicalRunesWidth,
} from '../../../discord/src/forge/runes';
import {
  GLYPH_GAP,
  GLYPH_H,
  GLYPH_W,
  glyphLines,
  hasGlyph,
  runesWidth,
} from './runes';

let failures = 0;

function check(name: string, cond: boolean) {
  if (!cond) {
    failures++;
    console.error(`  x ${name}`);
  } else {
    console.log(`  ok ${name}`);
  }
}

function round(n: number): number {
  return Math.round(n * 100) / 100;
}

function canonicalLines(ch: string): ReadonlyArray<{ x1: number; y1: number; x2: number; y2: number }> {
  return canonicalGlyphStrokes(ch).map(([x1, y1, x2, y2]) => ({
    x1: round(x1),
    y1: round(y1),
    x2: round(x2),
    y2: round(y2),
  }));
}

console.log('runes.selftest');

check('geometry constants match canonical alphabet', GLYPH_W === CANONICAL_GLYPH_W && GLYPH_H === CANONICAL_GLYPH_H && GLYPH_GAP === CANONICAL_GLYPH_GAP);

for (const ch of SUPPORTED_CHARS) {
  check(`dashboard supports canonical glyph ${JSON.stringify(ch)}`, hasGlyph(ch) === canonicalHasGlyph(ch));
  check(
    `dashboard geometry matches canonical glyph ${JSON.stringify(ch)}`,
    JSON.stringify(glyphLines(ch)) === JSON.stringify(canonicalLines(ch)),
  );
}

for (const ch of ['a', 'z', '?', '_', '\n']) {
  check(`support decision matches canonical for ${JSON.stringify(ch)}`, hasGlyph(ch) === canonicalHasGlyph(ch));
}

const sample = 'THE RECORD 0123456789.,-';
check('layout width matches canonical default advance', runesWidth(sample) === canonicalRunesWidth(sample));

if (failures > 0) {
  console.error(`runes.selftest FAILED - ${failures} issue(s)`);
  process.exit(1);
}

console.log('runes.selftest OK');
