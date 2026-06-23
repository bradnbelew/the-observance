/**
 * index.ts — CLUE FORGE CORE entry point.
 *
 * forgeClue(spec) takes a clue specification (which cipher + the payload), runs
 * the deterministic cipher, renders the resulting ciphertext as the keepers'
 * runes (an SVG <g> fragment), and returns:
 *
 *   { svg, solution, puzzleKey, meta }
 *
 *   - svg        : an SVG <g> fragment of the carved ciphertext (NOT a full doc;
 *                  the templates/PNG agent wraps it with the brand frame).
 *   - solution   : the plaintext a player must arrive at (the "answer").
 *   - puzzleKey  : a STABLE, deterministic id for this exact clue (same spec ->
 *                  same key, forever). Used as the ledger/Whisper key so hints,
 *                  budgets, and validation all address the one puzzle.
 *   - meta       : cipher kind, the key(s) used, rendered ciphertext, sizing.
 *
 * PURE: no discord, no supabase, no I/O. Rendering to PNG is the templates
 * agent's job and is intentionally NOT done here.
 */

import {
  atbash,
  bookCipher,
  caesar,
  coordEncode,
  substitution,
  vigenere,
  runSelfTests,
} from './ciphers.js';
import type { BookRef, Coord } from './ciphers.js';
import {
  renderRunes,
  runesWidth,
  runeSelfTest,
  GLYPH_W,
  GLYPH_H,
  GLYPH_GAP,
} from './runes.js';
import type { RenderRunesOptions } from './runes.js';

// ---------------------------------------------------------------------------
// Clue specifications — a discriminated union, one variant per cipher.
// ---------------------------------------------------------------------------

export type CipherKind =
  | 'substitution'
  | 'caesar'
  | 'vigenere'
  | 'atbash'
  | 'book'
  | 'coord';

interface BaseSpec {
  /**
   * Optional stable namespace so two clues with identical payloads in different
   * acts/contexts get different keys. Folded into the puzzleKey hash.
   */
  readonly namespace?: string;
  /** Optional render tweaks passed through to renderRunes. */
  readonly render?: RenderRunesOptions;
}

export interface SubstitutionSpec extends BaseSpec {
  readonly cipher: 'substitution';
  /** plaintext clue (will be uppercased into the keepers' caseless script). */
  readonly text: string;
}

export interface CaesarSpec extends BaseSpec {
  readonly cipher: 'caesar';
  readonly text: string;
  readonly shift: number;
}

export interface VigenereSpec extends BaseSpec {
  readonly cipher: 'vigenere';
  readonly text: string;
  readonly key: string;
}

export interface AtbashSpec extends BaseSpec {
  readonly cipher: 'atbash';
  readonly text: string;
}

export interface BookSpec extends BaseSpec {
  readonly cipher: 'book';
  /** plaintext clue, addressed word-by-word into `book`. */
  readonly text: string;
  /** the book text the (page,line,word) triples index into. */
  readonly book: string;
}

export interface CoordSpec extends BaseSpec {
  readonly cipher: 'coord';
  /** the in-world coordinate to carry across to Discord as runes. */
  readonly coord: Coord;
}

export type ClueSpec =
  | SubstitutionSpec
  | CaesarSpec
  | VigenereSpec
  | AtbashSpec
  | BookSpec
  | CoordSpec;

// ---------------------------------------------------------------------------
// Output types.
// ---------------------------------------------------------------------------

export interface ClueMeta {
  readonly cipher: CipherKind;
  /** the rune-id text that was carved (post-cipher). */
  readonly cipherText: string;
  /** human-facing description of the key needed to solve (in-character safe). */
  readonly keyHint: string;
  /** machine key value(s) actually used, for the author dashboard. */
  readonly key: Readonly<Record<string, unknown>>;
  /** laid-out glyph cell metrics so the template agent can size the frame. */
  readonly layout: {
    readonly glyphW: number;
    readonly glyphH: number;
    readonly gap: number;
    readonly width: number; // total laid-out width of the rune run
    readonly height: number; // == glyphH
    readonly glyphCount: number;
  };
  /** book-cipher only: the (page,line,word) refs + their compact string. */
  readonly bookRefs?: readonly BookRef[];
  readonly bookRefString?: string;
}

export interface ForgedClue {
  readonly svg: string; // <g> fragment of carved runes
  readonly solution: string; // the plaintext answer the player must reach
  readonly puzzleKey: string; // stable id for ledger/whispers
  readonly meta: ClueMeta;
}

// ---------------------------------------------------------------------------
// forgeClue
// ---------------------------------------------------------------------------

/**
 * Forge one clue. Deterministic: identical spec -> identical output (incl. key).
 */
export function forgeClue(spec: ClueSpec): ForgedClue {
  switch (spec.cipher) {
    case 'substitution':
      return forgeRuneText(
        spec,
        substitution.encode(spec.text.toUpperCase()),
        spec.text.toUpperCase(),
        'The keepers’ script — one mark, one letter.',
        {},
      );

    case 'caesar': {
      const solution = spec.text.toUpperCase();
      const ct = caesar.encode(solution, spec.shift);
      return forgeRuneText(
        spec,
        ct,
        solution,
        `Turn the wheel ${normShift(spec.shift)} marks.`,
        { shift: normShift(spec.shift) },
      );
    }

    case 'vigenere': {
      const solution = spec.text.toUpperCase();
      const ct = vigenere.encode(solution, spec.key);
      return forgeRuneText(
        spec,
        ct,
        solution,
        'A word is the key; lay it over the marks.',
        { key: spec.key.toUpperCase().replace(/[^A-Z]/g, '') },
      );
    }

    case 'atbash': {
      const solution = spec.text.toUpperCase();
      const ct = atbash.encode(solution);
      return forgeRuneText(spec, ct, solution, 'Read the script reversed — first is last.', {});
    }

    case 'book': {
      const solution = spec.text
        .split(/\s+/)
        .map((w) => w.replace(/^[^A-Za-z0-9]+/, '').replace(/[^A-Za-z0-9]+$/, '').toUpperCase())
        .filter((w) => w.length > 0)
        .join(' ');
      const refs = bookCipher.encode(spec.text, spec.book);
      const refString = bookCipher.format(refs);
      // We carve the REF STRING as runes (digits + separators exist in the script).
      const svg = renderRunes(refString, spec.render);
      const meta = makeMeta(
        'book',
        refString,
        'Find the keepers’ book; count page, line, word.',
        { pages: 'see book' },
        refString,
        spec.render,
        refs,
        refString,
      );
      return {
        svg,
        solution,
        puzzleKey: makePuzzleKey('book', spec.namespace, refString),
        meta,
      };
    }

    case 'coord': {
      const ct = coordEncode.encode(spec.coord);
      const solution = `${spec.coord.x} ${spec.coord.z}`;
      const svg = renderRunes(ct, spec.render);
      const meta = makeMeta(
        'coord',
        ct,
        'These marks number a place — sign, then count.',
        { x: spec.coord.x, z: spec.coord.z },
        ct,
        spec.render,
      );
      return {
        svg,
        solution,
        puzzleKey: makePuzzleKey('coord', spec.namespace, ct),
        meta,
      };
    }

    default: {
      // exhaustiveness guard — `spec` is `never` here if the union is covered.
      return assertNever(spec);
    }
  }
}

// ---------------------------------------------------------------------------
// internals
// ---------------------------------------------------------------------------

/** Shared path for the letter-ciphers that carve their ciphertext directly. */
function forgeRuneText(
  spec: ClueSpec,
  cipherText: string,
  solution: string,
  keyHint: string,
  key: Record<string, unknown>,
): ForgedClue {
  const svg = renderRunes(cipherText, spec.render);
  const meta = makeMeta(spec.cipher, cipherText, keyHint, key, cipherText, spec.render);
  return {
    svg,
    solution,
    puzzleKey: makePuzzleKey(spec.cipher, spec.namespace, cipherText),
    meta,
  };
}

function makeMeta(
  cipher: CipherKind,
  cipherText: string,
  keyHint: string,
  key: Record<string, unknown>,
  layoutText: string,
  render: RenderRunesOptions | undefined,
  bookRefs?: readonly BookRef[],
  bookRefString?: string,
): ClueMeta {
  return {
    cipher,
    cipherText,
    keyHint,
    key,
    layout: {
      glyphW: GLYPH_W,
      glyphH: GLYPH_H,
      gap: GLYPH_GAP,
      width: runesWidth(layoutText, render),
      height: GLYPH_H,
      glyphCount: [...layoutText].length,
    },
    ...(bookRefs ? { bookRefs, bookRefString } : {}),
  };
}

function normShift(shift: number): number {
  return ((Math.trunc(shift) % 26) + 26) % 26;
}

/** Compile-time exhaustiveness helper; throws if ever reached at runtime. */
function assertNever(value: never): never {
  throw new Error(`forgeClue: unhandled cipher spec ${JSON.stringify(value)}`);
}

/**
 * makePuzzleKey — a STABLE, collision-resistant id for a clue. Same cipher +
 * namespace + carved text => same key on every machine, every run (a pure FNV-1a
 * 32-bit hash rendered as 8 hex chars, prefixed by the cipher kind for legibility
 * in the ledger). No randomness, no time.
 */
export function makePuzzleKey(
  cipher: CipherKind,
  namespace: string | undefined,
  cipherText: string,
): string {
  const basis = `${cipher} ${namespace ?? ''} ${cipherText}`;
  return `clue_${cipher}_${fnv1a32(basis)}`;
}

/** FNV-1a 32-bit, deterministic, returns 8 lowercase hex chars. */
function fnv1a32(str: string): string {
  let h = 0x811c9dc5;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    // h *= 16777619, kept in 32-bit unsigned space
    h = (h + ((h << 1) + (h << 4) + (h << 7) + (h << 8) + (h << 24))) >>> 0;
  }
  return h.toString(16).padStart(8, '0');
}

// ---------------------------------------------------------------------------
// Aggregate self-test runner — proves runes + all cipher round-trips, and that
// forgeClue itself produces decodable, stable output.
// ---------------------------------------------------------------------------

export function forgeSelfTest(): { passed: number; cases: string[] } {
  runeSelfTest();
  const cipherResults = runSelfTests();
  const cases = [...cipherResults.cases, 'runes: alphabet bijective + geometry valid'];

  // forgeClue stability: same spec twice -> identical puzzleKey + svg.
  const spec: ClueSpec = { cipher: 'caesar', text: 'BOW AT THE MARKER', shift: 7 };
  const a = forgeClue(spec);
  const b = forgeClue(spec);
  if (a.puzzleKey !== b.puzzleKey || a.svg !== b.svg) {
    throw new Error('forgeSelfTest: forgeClue is not deterministic');
  }
  // and the rendered ciphertext is solvable back to the solution.
  if (caesar.decode(a.meta.cipherText, 7) !== a.solution) {
    throw new Error('forgeSelfTest: caesar clue does not decode to its solution');
  }
  cases.push('forgeClue: deterministic puzzleKey + svg, decodes to solution');

  // coord handoff: forge -> read runes -> decode -> exact coord back.
  const coordClue = forgeClue({ cipher: 'coord', coord: { x: -1280, z: 64 } });
  const back = coordEncode.decode(coordClue.meta.cipherText);
  if (back.x !== -1280 || back.z !== 64) {
    throw new Error('forgeSelfTest: coord clue does not round-trip');
  }
  cases.push('forgeClue: coord cross-surface handoff round-trips');

  return { passed: cases.length, cases };
}

// Re-export the building blocks so consumers can import everything from one place.
export {
  // ciphers
  caesar,
  atbash,
  vigenere,
  substitution,
  bookCipher,
  coordEncode,
  runSelfTests,
} from './ciphers.js';
export type { BookRef, Coord } from './ciphers.js';
export {
  // runes
  renderRunes,
  glyphPath,
  glyphPathData,
  runeSelfTest,
  RUNE_MAP,
  RUNE_MAP_INVERSE,
  RUNE_LETTERS,
  SUPPORTED_CHARS,
  GLYPH_W,
  GLYPH_H,
  GLYPH_GAP,
} from './runes.js';
export type { RenderRunesOptions } from './runes.js';
