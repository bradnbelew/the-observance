/**
 * runes.ts — THE KEEPERS' ALPHABET, ported for the dashboard's client-renderable header marks.
 *
 * This is a deliberate, geometry-only PORT of `discord/src/forge/runes.ts` (the single source of
 * truth for the game's own invented rune alphabet — the one also baked into the Minecraft resource
 * pack font at resourcepack/assets/observance/font/runes.json). The dashboard has no shared package
 * with `discord/`, so rather than link a fake Unicode Elder Futhark wordmark previously hard-coded in
 * the record pages (a "false affordance": a player who learns the REAL in-game glyphs cannot read that
 * decorative stand-in), we mirror the exact same stroke table here so the web header marks are
 * carved from the identical alphabet as the in-world runes and the Discord clue cards.
 *
 * Keep this file's LETTER_BRANCHES / geometry constants byte-identical to the canonical copy in
 * discord/src/forge/runes.ts if that file ever changes the alphabet.
 *
 * PURE: no fs/node imports, safe in a Next.js Server or Client Component.
 */

export const GLYPH_W = 28;
export const GLYPH_H = 64;
export const GLYPH_GAP = 6;
export const STROKE_W = 3;

const STAVE_X = GLYPH_W / 2;
const TOP = 4;
const BOT = GLYPH_H - 4;
const LEFT = 4;
const RIGHT = GLYPH_W - 4;

const A_HIGH = TOP + (BOT - TOP) * 0.22;
const A_MID = TOP + (BOT - TOP) * 0.5;
const A_LOW = TOP + (BOT - TOP) * 0.78;

type Stroke = readonly [x1: number, y1: number, x2: number, y2: number];

const branchR = (anchorY: number, toY: number): Stroke => [STAVE_X, anchorY, RIGHT, toY];
const branchL = (anchorY: number, toY: number): Stroke => [STAVE_X, anchorY, LEFT, toY];
const barR = (y: number): Stroke => [STAVE_X, y, RIGHT, y];
const barL = (y: number): Stroke => [STAVE_X, y, LEFT, y];
const crossbar = (y: number): Stroke => [LEFT, y, RIGHT, y];
const fullDiag = (down: boolean): Stroke =>
  down ? [LEFT, TOP, RIGHT, BOT] : [LEFT, BOT, RIGHT, TOP];

const LETTER_BRANCHES: Readonly<Record<string, readonly Stroke[]>> = Object.freeze({
  A: [branchR(A_HIGH, TOP)],
  B: [branchL(A_HIGH, TOP)],
  C: [barR(A_MID)],
  D: [barL(A_MID)],
  E: [branchR(A_LOW, BOT)],
  F: [branchL(A_LOW, BOT)],
  G: [branchR(A_HIGH, TOP), branchR(A_LOW, BOT)],
  H: [branchL(A_HIGH, TOP), branchL(A_LOW, BOT)],
  I: [branchR(A_MID, TOP), branchR(A_MID, BOT)],
  J: [branchL(A_MID, TOP), branchL(A_MID, BOT)],
  K: [barR(A_HIGH), barR(A_LOW)],
  L: [barL(A_HIGH), barL(A_LOW)],
  M: [crossbar(A_HIGH)],
  N: [crossbar(A_LOW)],
  O: [crossbar(A_MID), branchR(A_HIGH, TOP)],
  P: [crossbar(A_MID), branchL(A_HIGH, TOP)],
  Q: [fullDiag(true)],
  R: [fullDiag(false)],
  S: [fullDiag(true), fullDiag(false)],
  T: [branchR(A_HIGH, TOP), barL(A_MID)],
  U: [branchL(A_HIGH, TOP), barR(A_MID)],
  V: [branchR(A_HIGH, TOP), branchL(A_HIGH, TOP)],
  W: [branchR(A_LOW, BOT), branchL(A_LOW, BOT)],
  X: [branchR(A_HIGH, TOP), branchR(A_LOW, BOT), barL(A_MID)],
  Y: [branchL(A_HIGH, TOP), branchL(A_LOW, BOT), barR(A_MID)],
  Z: [crossbar(A_HIGH), crossbar(A_LOW)],
});

const DIGIT_BRANCHES: Readonly<Record<string, readonly Stroke[]>> = Object.freeze({
  '0': [barL(A_MID)],
  '1': [barR(A_HIGH)],
  '2': [barR(A_HIGH), barR(A_MID)],
  '3': [barR(A_HIGH), barR(A_MID), barR(A_LOW)],
  '4': [barR(A_HIGH), barR(A_MID), barR(A_LOW), barL(A_MID)],
  '5': [barL(A_HIGH)],
  '6': [barL(A_HIGH), barR(A_LOW)],
  '7': [barL(A_HIGH), barR(A_MID), barR(A_LOW)],
  '8': [barL(A_HIGH), barR(A_HIGH), barR(A_MID), barR(A_LOW)],
  '9': [barL(A_HIGH), barL(A_LOW), barR(A_HIGH), barR(A_MID), barR(A_LOW)],
});

const MARK_BRANCHES: Readonly<Record<string, readonly Stroke[]>> = Object.freeze({
  '.': [[STAVE_X - 2, A_MID, STAVE_X + 2, A_MID]],
  '-': [crossbar(A_MID)],
  ',': [barR(A_LOW), barL(A_LOW)],
});

const MARK_SET = new Set(Object.keys(MARK_BRANCHES));

export function hasGlyph(ch: string): boolean {
  if (ch === ' ') return true;
  const up = ch.toUpperCase();
  return (
    Object.prototype.hasOwnProperty.call(LETTER_BRANCHES, up) ||
    Object.prototype.hasOwnProperty.call(DIGIT_BRANCHES, ch) ||
    Object.prototype.hasOwnProperty.call(MARK_BRANCHES, ch)
  );
}

function branchesFor(ch: string): readonly Stroke[] | null {
  if (ch === ' ') return [];
  const up = ch.toUpperCase();
  const letter = LETTER_BRANCHES[up];
  if (letter !== undefined) return letter;
  const digit = DIGIT_BRANCHES[ch];
  if (digit !== undefined) return digit;
  const mark = MARK_BRANCHES[ch];
  if (mark !== undefined) return mark;
  return null;
}

export function glyphStrokes(ch: string): readonly Stroke[] {
  const branches = branchesFor(ch);
  if (branches === null) {
    throw new Error(`runes: no glyph for character ${JSON.stringify(ch)}`);
  }
  if (ch === ' ') return [];
  if (MARK_SET.has(ch)) return branches;
  const stave: Stroke = [STAVE_X, TOP, STAVE_X, BOT];
  return [stave, ...branches];
}

function round(n: number): number {
  return Math.round(n * 100) / 100;
}

/** Per-glyph line segments as plain data (for a React/SVG renderer — no string templating). */
export function glyphLines(ch: string): ReadonlyArray<{ x1: number; y1: number; x2: number; y2: number }> {
  return glyphStrokes(ch).map(([x1, y1, x2, y2]) => ({
    x1: round(x1),
    y1: round(y1),
    x2: round(x2),
    y2: round(y2),
  }));
}

/** Total laid-out width for a string of glyphs, matching the canonical advance/padding rules. */
export function runesWidth(text: string, advance = GLYPH_W + GLYPH_GAP, padX = 0): number {
  const n = [...text].length;
  if (n === 0) return padX * 2;
  return padX * 2 + (n - 1) * advance + GLYPH_W;
}

export { GLYPH_W as RUNE_GLYPH_W, GLYPH_H as RUNE_GLYPH_H };
