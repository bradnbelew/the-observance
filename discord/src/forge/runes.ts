/**
 * runes.ts — The Keepers' Alphabet (original deterministic runic script)
 *
 * Lore: a resource-pack rune font is the prior keepers' alphabet — a substitution
 * cipher that is ALSO world-building (see arc bible §"The rune-language"). Early
 * clues teach a few glyphs; by Act 2 the group half-reads the world.
 *
 * This module is the canonical, code-defined source of truth for that alphabet so
 * the in-game resource pack, the Discord renderer, and the cipher layer can never
 * disagree. It is PURE: no I/O, no discord/supabase imports, fully deterministic.
 *
 * Design constraints (so it stays a clean, decodable substitution AND looks
 * engraved/consistent):
 *   - Each letter A–Z maps to exactly ONE fixed glyph (bijective → decodable).
 *   - Every glyph is drawn on the SAME vertical stave (a central trunk line),
 *     plus a deterministic set of 2–4 branch strokes. This gives the family the
 *     consistent "carved on one stave" look of real futhark-style runes without
 *     copying any Unicode rune (these are original strokes).
 *   - We also define glyphs for digits 0–9 (used by coordEncode) and a small set
 *     of structural marks (word gap, group separator) — all on the same stave.
 *
 * Coordinate system for a single glyph cell (local space):
 *   width  = GLYPH_W (28)
 *   height = GLYPH_H (64)
 *   The stave (trunk) runs vertically at x = STAVE_X down the middle.
 *   Branches attach at fixed anchor heights and reach to the left/right edges.
 *
 * The stroke *vocabulary* below is what makes letters distinct yet related:
 * each letter is a fixed tuple of strokes, authored once, never random.
 */

// ---------------------------------------------------------------------------
// Geometry constants (exported so the template/PNG agent can lay out cleanly).
// ---------------------------------------------------------------------------

export const GLYPH_W = 28; // glyph cell width in SVG user units
export const GLYPH_H = 64; // glyph cell height in SVG user units
export const GLYPH_GAP = 6; // horizontal gap between adjacent glyphs
export const STROKE_W = 3; // engraved stroke weight

const STAVE_X = GLYPH_W / 2; // central trunk x
const TOP = 4; // top inset of the trunk
const BOT = GLYPH_H - 4; // bottom inset of the trunk
const LEFT = 4; // left reach of branches
const RIGHT = GLYPH_W - 4; // right reach of branches

// Three vertical anchor bands where branches attach to the stave.
const A_HIGH = TOP + (BOT - TOP) * 0.22;
const A_MID = TOP + (BOT - TOP) * 0.5;
const A_LOW = TOP + (BOT - TOP) * 0.78;

/**
 * A stroke primitive. Everything is rendered as straight line segments so the
 * result reads as carved/engraved (no curves), and so it is trivially
 * deterministic and resvg/satori-safe.
 */
type Stroke = readonly [x1: number, y1: number, x2: number, y2: number];

/**
 * Branch-stroke factory helpers. They return line segments anchored to the
 * stave so every glyph shares the same skeleton.
 */
const branchR = (anchorY: number, toY: number): Stroke => [STAVE_X, anchorY, RIGHT, toY];
const branchL = (anchorY: number, toY: number): Stroke => [STAVE_X, anchorY, LEFT, toY];
const barR = (y: number): Stroke => [STAVE_X, y, RIGHT, y];
const barL = (y: number): Stroke => [STAVE_X, y, LEFT, y];
const crossbar = (y: number): Stroke => [LEFT, y, RIGHT, y];
const fullDiag = (down: boolean): Stroke =>
  down ? [LEFT, TOP, RIGHT, BOT] : [LEFT, BOT, RIGHT, TOP];

/**
 * THE ALPHABET TABLE.
 *
 * Each entry is the set of branch strokes for that letter (the central stave is
 * added automatically by `glyphStrokes`). Authored by hand so each letter is a
 * unique, fixed silhouette — a true bijection. The patterns escalate roughly by
 * stroke count so early/simple letters feel like "starter" glyphs.
 *
 * IMPORTANT: this object is frozen and order-stable. Do not reorder; downstream
 * the keys are read via Object.keys for the substitution map, but the
 * substitution cipher keys off the letter itself, not position, so it is robust.
 */
const LETTER_BRANCHES: Readonly<Record<string, readonly Stroke[]>> = Object.freeze({
  // High branch only — the simplest carved marks.
  A: [branchR(A_HIGH, TOP)],
  B: [branchL(A_HIGH, TOP)],
  // Mid bar.
  C: [barR(A_MID)],
  D: [barL(A_MID)],
  // Low branch.
  E: [branchR(A_LOW, BOT)],
  F: [branchL(A_LOW, BOT)],
  // High + low on the same side (chevron family).
  G: [branchR(A_HIGH, TOP), branchR(A_LOW, BOT)],
  H: [branchL(A_HIGH, TOP), branchL(A_LOW, BOT)],
  // Mirror chevron (open up vs open down).
  I: [branchR(A_MID, TOP), branchR(A_MID, BOT)],
  J: [branchL(A_MID, TOP), branchL(A_MID, BOT)],
  // Two horizontal bars, same side.
  K: [barR(A_HIGH), barR(A_LOW)],
  L: [barL(A_HIGH), barL(A_LOW)],
  // Full crossbar (reaches both edges) — distinctive "tie" glyphs.
  M: [crossbar(A_HIGH)],
  N: [crossbar(A_LOW)],
  // Crossbar + a branch (compound).
  O: [crossbar(A_MID), branchR(A_HIGH, TOP)],
  P: [crossbar(A_MID), branchL(A_HIGH, TOP)],
  // Diagonal through the cell.
  Q: [fullDiag(true)],
  R: [fullDiag(false)],
  // Both diagonals = an X overlay (kept distinct from any single-diag glyph).
  S: [fullDiag(true), fullDiag(false)],
  // Branch high one side + bar mid other side.
  T: [branchR(A_HIGH, TOP), barL(A_MID)],
  U: [branchL(A_HIGH, TOP), barR(A_MID)],
  // Two branches forming a "fork" at top, plus a low bar.
  V: [branchR(A_HIGH, TOP), branchL(A_HIGH, TOP)],
  W: [branchR(A_LOW, BOT), branchL(A_LOW, BOT)],
  // Dense three-stroke marks for the rare tail letters.
  X: [branchR(A_HIGH, TOP), branchR(A_LOW, BOT), barL(A_MID)],
  Y: [branchL(A_HIGH, TOP), branchL(A_LOW, BOT), barR(A_MID)],
  Z: [crossbar(A_HIGH), crossbar(A_LOW)],
});

/**
 * Digit glyphs 0–9. Same stave family, drawn with a small "counting" motif:
 * a stack of short right-side bars whose count encodes the digit's low part,
 * plus a left tick for the "five" group — so they look related to letters but
 * are visually a separate, readable numeric run for coordinates.
 */
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

/**
 * Structural marks. These are NOT letters and never participate in the
 * substitution map; they are layout/coordinate punctuation rendered in the same
 * carved style so a clue reads as one artifact.
 *   ' ' (space)  -> blank cell (word gap, no strokes, no stave)
 *   '.'          -> a single mid dot rendered as a tiny bar (group separator)
 *   '-'          -> a sign mark for negative coordinates (low crossbar)
 *   ','          -> coordinate axis separator (a notched stave)
 */
const MARK_BRANCHES: Readonly<Record<string, readonly Stroke[]>> = Object.freeze({
  '.': [[STAVE_X - 2, A_MID, STAVE_X + 2, A_MID]],
  '-': [crossbar(A_MID)],
  ',': [barR(A_LOW), barL(A_LOW)],
});

const MARK_SET = new Set(Object.keys(MARK_BRANCHES));

/** Full set of characters this script can carve (letters + digits + marks + space). */
export const SUPPORTED_CHARS: ReadonlySet<string> = new Set([
  ...Object.keys(LETTER_BRANCHES),
  ...Object.keys(DIGIT_BRANCHES),
  ...Object.keys(MARK_BRANCHES),
  ' ',
]);

/** The ordered list of cipher letters (A–Z) — the substitution alphabet. */
export const RUNE_LETTERS: readonly string[] = Object.freeze(Object.keys(LETTER_BRANCHES));

// ---------------------------------------------------------------------------
// Substitution map (canonical letter <-> stable glyph id). This is what
// ciphers.ts consumes for the rune-substitution cipher. We expose a *stable id*
// (the letter itself, since glyphs are bijective with letters) so the cipher and
// the renderer share one ground truth and round-trips are exact.
// ---------------------------------------------------------------------------

/** letter (uppercase A–Z) -> canonical glyph id. Bijective. */
export const RUNE_MAP: Readonly<Record<string, string>> = Object.freeze(
  Object.fromEntries(RUNE_LETTERS.map((l) => [l, l])),
);

/** glyph id -> letter. The exact inverse of RUNE_MAP (guarantees decodability). */
export const RUNE_MAP_INVERSE: Readonly<Record<string, string>> = Object.freeze(
  Object.fromEntries(Object.entries(RUNE_MAP).map(([letter, glyph]) => [glyph, letter])),
);

// ---------------------------------------------------------------------------
// Glyph geometry accessors.
// ---------------------------------------------------------------------------

/** Does this char (case-insensitive for letters) have a carved glyph? */
export function hasGlyph(ch: string): boolean {
  if (ch === ' ') return true;
  const up = ch.toUpperCase();
  return (
    Object.prototype.hasOwnProperty.call(LETTER_BRANCHES, up) ||
    Object.prototype.hasOwnProperty.call(DIGIT_BRANCHES, ch) ||
    Object.prototype.hasOwnProperty.call(MARK_BRANCHES, ch)
  );
}

/** Resolve the stroke list for any supported character (without the stave). */
function branchesFor(ch: string): readonly Stroke[] | null {
  if (ch === ' ') return [];
  const up = ch.toUpperCase();
  // Bracket access returns `T | undefined` under noUncheckedIndexedAccess, so we
  // capture-and-check rather than relying on hasOwnProperty narrowing.
  const letter = LETTER_BRANCHES[up];
  if (letter !== undefined) return letter;
  const digit = DIGIT_BRANCHES[ch];
  if (digit !== undefined) return digit;
  const mark = MARK_BRANCHES[ch];
  if (mark !== undefined) return mark;
  return null;
}

/**
 * Full stroke set for a character INCLUDING the central stave when appropriate.
 * Space => no strokes. Marks => their own strokes (no full stave, they are
 * punctuation). Letters & digits => stave + branches.
 */
export function glyphStrokes(ch: string): readonly Stroke[] {
  const branches = branchesFor(ch);
  if (branches === null) {
    throw new Error(`runes: no glyph for character ${JSON.stringify(ch)}`);
  }
  if (ch === ' ') return [];
  if (MARK_SET.has(ch)) return branches; // marks render without the full trunk
  const stave: Stroke = [STAVE_X, TOP, STAVE_X, BOT];
  return [stave, ...branches];
}

/**
 * glyphPath(letter) — SVG geometry for ONE glyph as a string of <line> elements
 * (sharp/engraved). Returns an empty string for a space (an intentional gap).
 *
 * Returned fragment is local to a GLYPH_W x GLYPH_H cell; the caller positions
 * the cell. We emit <line> elements (not a single <path>) because straight
 * carved strokes are clearest and round-trip-safe in satori/resvg.
 */
export function glyphPath(letter: string): string {
  const strokes = glyphStrokes(letter);
  if (strokes.length === 0) return '';
  return strokes
    .map(
      ([x1, y1, x2, y2]) =>
        `<line x1="${round(x1)}" y1="${round(y1)}" x2="${round(x2)}" y2="${round(y2)}" ` +
        `stroke="currentColor" stroke-width="${STROKE_W}" stroke-linecap="round" />`,
    )
    .join('');
}

/**
 * glyphPathData(letter) — the same geometry expressed as an SVG path `d` string
 * (M/L commands). Handy when the template agent wants a single fillable/strokable
 * path per glyph instead of discrete lines. Empty string for a space.
 */
export function glyphPathData(letter: string): string {
  const strokes = glyphStrokes(letter);
  if (strokes.length === 0) return '';
  return strokes
    .map(([x1, y1, x2, y2]) => `M${round(x1)} ${round(y1)}L${round(x2)} ${round(y2)}`)
    .join(' ');
}

export interface RenderRunesOptions {
  /** advance per glyph cell; defaults to GLYPH_W + GLYPH_GAP */
  readonly advance?: number;
  /** left padding before the first glyph */
  readonly padX?: number;
  /** top padding */
  readonly padY?: number;
}

/**
 * renderRunes(text) — lays out the text left-to-right as carved glyphs and
 * returns an SVG `<g>` fragment (NOT a full document — the templates agent wraps
 * it with the brand frame, sizing, and the cream/navy palette).
 *
 * - Unknown characters are skipped silently EXCEPT this throws if the text
 *   contains a non-space char with no glyph, to keep clue generation honest
 *   (a clue you can't render is a bug, not a silent gap).
 * - Color uses `currentColor` so the wrapper controls ink color.
 */
export function renderRunes(text: string, opts: RenderRunesOptions = {}): string {
  const advance = opts.advance ?? GLYPH_W + GLYPH_GAP;
  const padX = opts.padX ?? 0;
  const padY = opts.padY ?? 0;

  // Validate up front: every character must be renderable.
  for (const ch of text) {
    if (!hasGlyph(ch)) {
      throw new Error(`renderRunes: character ${JSON.stringify(ch)} has no glyph`);
    }
  }

  const cells: string[] = [];
  let i = 0;
  for (const ch of text) {
    const x = padX + i * advance;
    const inner = glyphPath(ch);
    if (inner.length > 0) {
      cells.push(`<g transform="translate(${round(x)} ${round(padY)})">${inner}</g>`);
    }
    i += 1;
  }
  return `<g class="runes" fill="none">${cells.join('')}</g>`;
}

/** Total laid-out width for a string of glyphs (for the template agent's sizing). */
export function runesWidth(text: string, opts: RenderRunesOptions = {}): number {
  const advance = opts.advance ?? GLYPH_W + GLYPH_GAP;
  const padX = opts.padX ?? 0;
  const n = [...text].length;
  if (n === 0) return padX * 2;
  return padX * 2 + (n - 1) * advance + GLYPH_W;
}

function round(n: number): number {
  // keep SVG tidy + deterministic
  return Math.round(n * 100) / 100;
}

/**
 * runeSelfTest — asserts the alphabet is a clean, decodable substitution and the
 * geometry is well-formed. Throws on any violation. Pure (no I/O).
 */
export function runeSelfTest(): void {
  // 1) Exactly 26 letters, A–Z, each unique.
  if (RUNE_LETTERS.length !== 26) {
    throw new Error(`runeSelfTest: expected 26 letters, got ${RUNE_LETTERS.length}`);
  }
  const expected = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');
  for (const l of expected) {
    if (!Object.prototype.hasOwnProperty.call(RUNE_MAP, l)) {
      throw new Error(`runeSelfTest: missing letter ${l}`);
    }
  }

  // 2) Bijection: map then inverse returns the original for every letter.
  for (const l of expected) {
    const glyph = RUNE_MAP[l];
    if (glyph === undefined) {
      throw new Error(`runeSelfTest: no glyph mapped for ${l}`);
    }
    const back = RUNE_MAP_INVERSE[glyph];
    if (back !== l) {
      throw new Error(`runeSelfTest: substitution not bijective at ${l} (-> ${glyph} -> ${back})`);
    }
  }

  // 3) Glyph silhouettes are distinct (no two letters share the same stroke set).
  const sigs = new Map<string, string>();
  for (const l of expected) {
    const sig = JSON.stringify(glyphStrokes(l));
    const clash = sigs.get(sig);
    if (clash) {
      throw new Error(`runeSelfTest: letters ${clash} and ${l} render identical glyphs`);
    }
    sigs.set(sig, l);
  }

  // 4) Every letter, digit, and mark produces non-empty geometry.
  for (const ch of [...expected, ...Object.keys(DIGIT_BRANCHES), ...Object.keys(MARK_BRANCHES)]) {
    if (glyphPath(ch).length === 0) {
      throw new Error(`runeSelfTest: empty geometry for ${JSON.stringify(ch)}`);
    }
  }

  // 5) Space renders as an intentional gap (no strokes) but is "supported".
  if (glyphPath(' ').length !== 0) {
    throw new Error('runeSelfTest: space should render no strokes');
  }
  if (!hasGlyph(' ')) {
    throw new Error('runeSelfTest: space must be supported');
  }

  // 6) renderRunes emits a <g> and throws on truly unsupported chars.
  const frag = renderRunes('ABC 123');
  if (!frag.startsWith('<g')) {
    throw new Error('runeSelfTest: renderRunes must return a <g> fragment');
  }
  let threw = false;
  try {
    renderRunes('hello☃'); // snowman has no glyph
  } catch {
    threw = true;
  }
  if (!threw) {
    throw new Error('runeSelfTest: renderRunes must throw on unsupported characters');
  }
}
