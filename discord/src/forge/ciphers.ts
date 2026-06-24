/**
 * ciphers.ts — CLUE FORGE CORE: deterministic, always-decodable ciphers.
 *
 * The contract for EVERY cipher in this file:
 *
 *     decode(encode(plaintext, key), key) === plaintext
 *
 * for all plaintext drawn from the cipher's declared domain. This is enforced by
 * `runSelfTests()`, which round-trips sample inputs and THROWS on any mismatch.
 *
 * These functions are PURE: no discord, no supabase, no I/O, no Date.now(), no
 * randomness. Given the same input + key they always produce the same output —
 * which is exactly what an ARG clue layer needs (a clue must decode the same way
 * every time, on every machine, forever).
 *
 * Domain conventions
 * ------------------
 *   - The "rune alphabet" is A–Z (the keepers' script in runes.ts).
 *   - Letter ciphers (caesar/vigenere/atbash/substitution) operate on letters
 *     and PASS THROUGH every non-letter character unchanged (spaces, digits,
 *     punctuation). This keeps them reversible for arbitrary clue text.
 *   - Case handling: letter ciphers preserve case on pass-through but the
 *     keepers' script is caseless, so the canonical clue text we forge is
 *     uppercased upstream (in index.ts). The ciphers themselves still preserve
 *     case so they are general-purpose and self-tests can prove it.
 */

import { RUNE_LETTERS, RUNE_MAP, RUNE_MAP_INVERSE, SUPPORTED_CHARS } from './runes.js';

const A_CODE = 65; // 'A'
const ALPHA = 26;

// ---------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------

function isUpper(code: number): boolean {
  return code >= 65 && code <= 90;
}
function isLower(code: number): boolean {
  return code >= 97 && code <= 122;
}
/** true modulo (handles negatives), so shifts wrap cleanly both directions. */
function mod(n: number, m: number): number {
  return ((n % m) + m) % m;
}

/** Shift a single letter by `n` (positive or negative), preserving case. */
function shiftChar(ch: string, n: number): string {
  const code = ch.charCodeAt(0);
  if (isUpper(code)) return String.fromCharCode(((mod(code - 65 + n, ALPHA)) + 65));
  if (isLower(code)) return String.fromCharCode(((mod(code - 97 + n, ALPHA)) + 97));
  return ch; // pass-through for non-letters
}

/** Keep only A–Z (uppercased) from a key — used by vigenere & substitution. */
function lettersOnlyUpper(s: string): string {
  return s.toUpperCase().replace(/[^A-Z]/g, '');
}

// ===========================================================================
// 1) CAESAR
//    key: integer shift. encode shifts +key, decode shifts -key.
// ===========================================================================

export const caesar = {
  encode(text: string, key: number): string {
    const n = normalizeCaesarKey(key);
    let out = '';
    for (const ch of text) out += shiftChar(ch, n);
    return out;
  },
  decode(text: string, key: number): string {
    const n = normalizeCaesarKey(key);
    let out = '';
    for (const ch of text) out += shiftChar(ch, -n);
    return out;
  },
} as const;

function normalizeCaesarKey(key: number): number {
  if (!Number.isInteger(key)) {
    throw new Error(`caesar: key must be an integer, got ${key}`);
  }
  return mod(key, ALPHA);
}

// ===========================================================================
// 2) ATBASK  (atbash — mirror the alphabet: A<->Z, B<->Y, ...)
//    It is its own inverse: encode === decode.
// ===========================================================================

function atbashChar(ch: string): string {
  const code = ch.charCodeAt(0);
  if (isUpper(code)) return String.fromCharCode(90 - (code - 65));
  if (isLower(code)) return String.fromCharCode(122 - (code - 97));
  return ch;
}

export const atbash = {
  encode(text: string): string {
    let out = '';
    for (const ch of text) out += atbashChar(ch);
    return out;
  },
  decode(text: string): string {
    // atbash is an involution
    return atbash.encode(text);
  },
} as const;

// ===========================================================================
// 3) VIGENERE
//    key: a word (letters). Each plaintext letter is shifted by the key letter.
//    Non-letters pass through and DO NOT advance the key index (so the key
//    aligns to letters only — the standard, reversible variant).
// ===========================================================================

export const vigenere = {
  encode(text: string, key: string): string {
    return vigCore(text, key, +1);
  },
  decode(text: string, key: string): string {
    return vigCore(text, key, -1);
  },
} as const;

function vigCore(text: string, key: string, dir: 1 | -1): string {
  const k = lettersOnlyUpper(key);
  if (k.length === 0) {
    throw new Error('vigenere: key must contain at least one letter A-Z');
  }
  let out = '';
  let ki = 0;
  for (const ch of text) {
    const code = ch.charCodeAt(0);
    if (isUpper(code) || isLower(code)) {
      const shift = k.charCodeAt(ki % k.length) - A_CODE;
      out += shiftChar(ch, dir * shift);
      ki += 1;
    } else {
      out += ch; // pass-through, key index unchanged
    }
  }
  return out;
}

// ===========================================================================
// 4) SUBSTITUTION (via the keepers' rune map)
//    encode: letter -> its canonical rune glyph id (uppercased domain).
//    decode: glyph id -> letter. Non-letters pass through.
//
//    Because RUNE_MAP is bijective with RUNE_MAP_INVERSE (asserted in runes),
//    this round-trips exactly. The "ciphertext" here is the sequence of glyph
//    ids; the renderer (runes.renderRunes) turns ids into carved SVG, but the
//    cipher layer stays in id-space so it is pure text and fully reversible.
// ===========================================================================

export const substitution = {
  /** plaintext (letters + others) -> glyph-id string. Input is uppercased first. */
  encode(text: string): string {
    let out = '';
    for (const ch of text) {
      const up = ch.toUpperCase();
      const glyph = RUNE_MAP[up];
      out += glyph !== undefined ? glyph : ch; // non-letters pass through
    }
    return out;
  },
  /** glyph-id string -> plaintext letters (uppercase). Non-glyph chars pass through. */
  decode(text: string): string {
    let out = '';
    for (const ch of text) {
      const letter = RUNE_MAP_INVERSE[ch];
      out += letter !== undefined ? letter : ch;
    }
    return out;
  },
} as const;

// ===========================================================================
// 5) BOOK CIPHER
//    Encode plaintext as (page, line, word) index triples into a provided text
//    ("the book"). Decode resolves the triples back to words.
//
//    Reversibility guarantee: encode tokenizes the SAME way decode does and only
//    succeeds if every plaintext word exists somewhere in the book; otherwise it
//    THROWS (a book cipher that can't address a word is a real failure, not a
//    silent drop). decode(encode(words)) returns the normalized word sequence.
//
//    Normalization: words are compared case-insensitively and stripped of
//    surrounding punctuation, and the plaintext domain for this cipher is
//    "space-separated words", returned uppercased to match the keepers' script.
// ===========================================================================

export interface BookRef {
  readonly page: number; // 1-based
  readonly line: number; // 1-based within page
  readonly word: number; // 1-based within line
}

export interface BookCipherOptions {
  /** characters that separate pages in the book text. default: form-feed or "\n\n" */
  readonly pageBreak?: RegExp;
}

const DEFAULT_PAGE_BREAK = /\f|\n[ \t]*\n/;

/** Tokenize a book into pages -> lines -> words (lowercased, punctuation-stripped). */
function tokenizeBook(book: string, opts: BookCipherOptions = {}): string[][][] {
  const pageBreak = opts.pageBreak ?? DEFAULT_PAGE_BREAK;
  const pages = book.split(pageBreak);
  // Each page -> its lines -> that line's normalized words. Empty lines stay as
  // empty arrays so 1-based line numbering matches the source exactly.
  return pages.map((page) =>
    page.split(/\r?\n/).map((line) =>
      line
        .split(/\s+/)
        .map((w) => normalizeWord(w))
        .filter((w) => w.length > 0),
    ),
  );
}

function normalizeWord(w: string): string {
  // strip leading/trailing non-alphanumeric, lowercase
  return w.replace(/^[^A-Za-z0-9]+/, '').replace(/[^A-Za-z0-9]+$/, '').toLowerCase();
}

export const bookCipher = {
  /**
   * Encode plaintext into BookRef[] using the provided book.
   * Plaintext is treated as whitespace-separated words. Each word must be
   * findable in the book or this throws. Deterministic: always picks the FIRST
   * occurrence (lowest page, then line, then word).
   */
  encode(plaintext: string, book: string, opts: BookCipherOptions = {}): BookRef[] {
    const pages = tokenizeBook(book, opts);

    // Build a first-occurrence index: word -> BookRef.
    const index = new Map<string, BookRef>();
    for (let p = 0; p < pages.length; p++) {
      const page = pages[p] ?? [];
      for (let l = 0; l < page.length; l++) {
        const line = page[l] ?? [];
        for (let w = 0; w < line.length; w++) {
          const token = line[w];
          if (token !== undefined && !index.has(token)) {
            index.set(token, { page: p + 1, line: l + 1, word: w + 1 });
          }
        }
      }
    }

    const words = plaintext
      .split(/\s+/)
      .map((w) => normalizeWord(w))
      .filter((w) => w.length > 0);

    const refs: BookRef[] = [];
    for (const word of words) {
      const ref = index.get(word);
      if (!ref) {
        throw new Error(`bookCipher: word "${word}" not found in book — cannot encode`);
      }
      refs.push(ref);
    }
    return refs;
  },

  /** Resolve BookRef[] back into the plaintext word sequence (UPPERCASE, space-joined). */
  decode(refs: readonly BookRef[], book: string, opts: BookCipherOptions = {}): string {
    const pages = tokenizeBook(book, opts);
    const words: string[] = [];
    for (const ref of refs) {
      const page = pages[ref.page - 1];
      if (!page) throw new Error(`bookCipher: page ${ref.page} out of range`);
      const line = page[ref.line - 1];
      if (!line) throw new Error(`bookCipher: page ${ref.page} line ${ref.line} out of range`);
      const word = line[ref.word - 1];
      if (word === undefined) {
        throw new Error(
          `bookCipher: page ${ref.page} line ${ref.line} word ${ref.word} out of range`,
        );
      }
      words.push(word.toUpperCase());
    }
    return words.join(' ');
  },

  /** Render a ref list as a compact human/clue string like "3-2-7  1-1-4". */
  format(refs: readonly BookRef[]): string {
    return refs.map((r) => `${r.page}-${r.line}-${r.word}`).join('  ');
  },

  /** Parse a "p-l-w  p-l-w" string back into BookRef[] (inverse of format). */
  parse(s: string): BookRef[] {
    return s
      .trim()
      .split(/\s+/)
      .filter((t) => t.length > 0)
      .map((t) => {
        const m = t.split('-').map((x) => parseInt(x, 10));
        const [page, line, word] = m;
        if (
          m.length !== 3 ||
          page === undefined ||
          line === undefined ||
          word === undefined ||
          ![page, line, word].every((x) => Number.isInteger(x) && x >= 1)
        ) {
          throw new Error(`bookCipher: bad ref token "${t}"`);
        }
        return { page, line, word };
      });
  },
} as const;

// ===========================================================================
// 6) COORD ENCODE
//    Encode Minecraft (x, z) integer coordinates into the rune alphabet so an
//    in-game coordinate can be carried across to Discord as runes and decoded
//    back to the exact coordinate (the cross-surface handoff from DESIGN §2.8).
//
//    DESIGN INTENT (the whole point of this cipher): once a player has learned
//    the digit glyphs 0–9 and the taught sign mark, the carved coordinate must
//    read as A REAL COORDINATE — not as a word. So we spell the value in actual
//    base-10 DIGIT glyphs, exactly the way a coordinate looks in the F3 debug
//    screen, fronted by an axis label so the reader knows which axis they hold.
//
//    Scheme (deterministic, lossless for 32-bit signed ints):
//      - Each axis is "<axisLabel><sign?><decimal digits>".
//          * axisLabel: the literal letter 'X' or 'Z' (taught: "this names the
//            axis"). It is the only letter glyph in the output, and it always
//            leads, so it can never be confused with the numeric body.
//          * sign: the taught '-' sign mark, present ONLY for negative values
//            (positives carry no mark, just like in-game). Zero is unsigned.
//          * digits: the base-10 magnitude in DIGIT glyphs 0–9 (no leading
//            zeros, except the single digit '0' for zero itself).
//      - The two axes are joined by the taught ',' axis-separator mark — every
//        emitted character (X, Z, 0–9, '-', ',') has its own carved glyph in
//        runes.ts, so the whole coordinate carves cleanly and reads literally.
//
//    Example: (x=-1280, z=64) -> "X-1280,Z64". A player who knows digits + the
//    '-' sign mark reads it as exactly that. decode reverses it back to
//    {x:-1280, z:64}.
// ===========================================================================

export interface Coord {
  readonly x: number;
  readonly z: number;
}

const AXIS_SEP = ','; // taught axis-separator mark (runes.ts MARK_BRANCHES[','])
const SIGN_NEG = '-'; // taught negative sign mark (runes.ts MARK_BRANCHES['-'])

/** integer -> "<axisLabel><sign?><base-10 digits>" using DIGIT glyphs 0–9. */
function intToCoordRunes(n: number, axis: 'X' | 'Z'): string {
  if (!Number.isInteger(n)) throw new Error(`coordEncode: ${n} is not an integer`);
  // Math.abs is unsafe at -2^31? No — it is exactly representable as a double,
  // and |n| <= 2^31 fits in a Number with no precision loss, so plain base-10
  // string conversion of the magnitude is exact across the 32-bit range.
  const sign = n < 0 ? SIGN_NEG : '';
  const body = Math.abs(n).toString(10); // base-10, DIGIT glyphs 0–9
  return axis + sign + body;
}

/** inverse of intToCoordRunes for a single axis token. */
function coordRunesToInt(s: string, axis: 'X' | 'Z'): number {
  if (s.length < 2 || s[0] !== axis) {
    throw new Error(`coordEncode: token "${s}" is not a ${axis}-axis value`);
  }
  let i = 1;
  let neg = false;
  if (s[i] === SIGN_NEG) {
    neg = true;
    i += 1;
  }
  const digits = s.slice(i);
  // Must be a non-empty pure run of 0–9, with no leading zeros (except "0"),
  // so encode is the unique pre-image of every value (round-trip is exact).
  if (!/^[0-9]+$/.test(digits) || (digits.length > 1 && digits[0] === '0')) {
    throw new Error(`coordEncode: bad numeric body in "${s}"`);
  }
  if (neg && digits === '0') {
    throw new Error(`coordEncode: negative zero "-0" is not canonical in "${s}"`);
  }
  const mag = Number(digits);
  if (!Number.isSafeInteger(mag)) {
    throw new Error(`coordEncode: magnitude in "${s}" is out of safe integer range`);
  }
  return neg ? -mag : mag;
}

export const coordEncode = {
  /** {x,z} -> carved coordinate string ("X<num>,Z<num>"), digits + sign mark. */
  encode(coord: Coord): string {
    return `${intToCoordRunes(coord.x, 'X')}${AXIS_SEP}${intToCoordRunes(coord.z, 'Z')}`;
  },
  /** carved coordinate string -> {x,z}. Exact inverse of encode. */
  decode(text: string): Coord {
    const parts = text.trim().split(AXIS_SEP);
    const [xs, zs] = parts;
    if (parts.length !== 2 || xs === undefined || zs === undefined) {
      throw new Error(`coordEncode: expected two axis tokens, got "${text}"`);
    }
    return { x: coordRunesToInt(xs.trim(), 'X'), z: coordRunesToInt(zs.trim(), 'Z') };
  },
} as const;

// ===========================================================================
// 7) RAIL FENCE (zigzag transposition)
//    key: rail count (>= 1). Plaintext is written diagonally down/up across
//    `rails` rows, then read off row-by-row. PURE transposition: it permutes
//    characters, so EVERY character (letters, digits, spaces, punctuation) is
//    preserved exactly and the round-trip is lossless for arbitrary text.
//
//    Reversibility: the zig-zag visiting order is a fixed permutation of indices
//    determined only by (length, rails). encode applies it; decode inverts it by
//    placing read-order characters back at their original positions. With rails=1
//    (or rails >= length) the permutation is the identity, so it degrades safely.
//
//    Human-solvable in-world: a clue can be carved as `rails` stacked rows of
//    runes; a solver redraws the fence (draw `rails` lines, bounce the letters
//    along them) with nothing but the carving and a count of rows.
// ===========================================================================

/** The row index visited at each position of a length-n, r-rail zig-zag. */
function railPattern(n: number, rails: number): number[] {
  if (!Number.isInteger(rails) || rails < 1) {
    throw new Error(`railFence: rails must be an integer >= 1, got ${rails}`);
  }
  const rows: number[] = [];
  if (rails === 1) {
    for (let i = 0; i < n; i++) rows.push(0);
    return rows;
  }
  let row = 0;
  let dir = 1; // +1 going down, -1 going up
  for (let i = 0; i < n; i++) {
    rows.push(row);
    if (row === 0) dir = 1;
    else if (row === rails - 1) dir = -1;
    row += dir;
  }
  return rows;
}

/** The order in which encode emits original indices (row-major over the fence). */
function railReadOrder(n: number, rails: number): number[] {
  const pattern = railPattern(n, rails);
  const order: number[] = [];
  const maxRow = n === 0 ? 0 : Math.max(...pattern) + 1;
  for (let r = 0; r < maxRow; r++) {
    for (let i = 0; i < n; i++) {
      if (pattern[i] === r) order.push(i);
    }
  }
  return order;
}

export const railFence = {
  /** plaintext -> rail-read ciphertext (pure permutation, all chars preserved). */
  encode(text: string, rails: number): string {
    const chars = [...text];
    const order = railReadOrder(chars.length, rails);
    return order.map((i) => chars[i]).join('');
  },
  /** rail-read ciphertext -> plaintext. Exact inverse of encode for same rails. */
  decode(text: string, rails: number): string {
    const chars = [...text];
    const n = chars.length;
    const order = railReadOrder(n, rails);
    // order[k] is the original position of the k-th ciphertext char; invert it.
    const out: string[] = new Array(n);
    for (let k = 0; k < n; k++) {
      const dest = order[k];
      if (dest !== undefined) out[dest] = chars[k] as string;
    }
    return out.join('');
  },
} as const;

// ===========================================================================
// 8) KEYED COLUMNAR TRANSPOSITION
//    key: a word. Plaintext is written into a grid row-by-row under the key's
//    letters; columns are then read in the alphabetical order of the key letters
//    (ties broken left-to-right, the standard rule). PURE transposition.
//
//    To stay perfectly reversible for ARBITRARY length text (no padding ghosts),
//    we DON'T pad with filler — instead each source column simply holds however
//    many real characters land in it. The per-column heights are recomputed
//    identically on decode from (length, keyWidth), so the read order and counts
//    match exactly. Every character is preserved (letters, digits, spaces,
//    punctuation).
//
//    Human-solvable in-world: write the key word atop the columns, number the
//    columns by the key's alphabetical order, drop the message in by rows, then
//    read columns in that number order. A solver needs only the key word and the
//    carved run.
// ===========================================================================

/** The column read-order for a key: indices sorted by (letter, original column). */
function columnOrder(key: string): number[] {
  const k = lettersOnlyUpper(key);
  if (k.length === 0) {
    throw new Error('columnar: key must contain at least one letter A-Z');
  }
  return k
    .split('')
    .map((ch, idx) => ({ ch, idx }))
    .sort((a, b) => (a.ch < b.ch ? -1 : a.ch > b.ch ? 1 : a.idx - b.idx))
    .map((e) => e.idx);
}

export const columnar = {
  /** plaintext -> column-read ciphertext under `key` (no padding, lossless). */
  encode(text: string, key: string): string {
    const chars = [...text];
    const order = columnOrder(key);
    const width = order.length;
    const n = chars.length;
    // Read each source column top-to-bottom, columns taken in key-alpha order.
    let out = '';
    for (const col of order) {
      for (let row = col; row < n; row += width) {
        out += chars[row];
      }
    }
    return out;
  },
  /** column-read ciphertext -> plaintext. Exact inverse for the same key. */
  decode(text: string, key: string): string {
    const chars = [...text];
    const order = columnOrder(key);
    const width = order.length;
    const n = chars.length;
    // Per-column height: column c holds every original index where index % width === c.
    const heightOf = (col: number): number => {
      let h = 0;
      for (let row = col; row < n; row += width) h++;
      return h;
    };
    const out: string[] = new Array(n);
    let k = 0;
    for (const col of order) {
      const h = heightOf(col);
      for (let r = 0; r < h; r++) {
        const dest = col + r * width; // original (row-major) index
        out[dest] = chars[k] as string;
        k++;
      }
    }
    return out.join('');
  },
} as const;

// ===========================================================================
// 9) POLYBIUS SQUARE (grid cipher)
//    Each letter maps to a (row, col) pair on a 5x5 square. Classic Polybius
//    merges I/J into one cell to fit 26 letters into 25; we follow that rule
//    (J is encoded as I, and decodes back to I) so the grid stays 5x5 and a
//    solver can draw it from memory. Output is space-separated two-digit pairs
//    using digits 1–5 (e.g. "A" -> "11"). Domain: LETTERS + SPACES (word break
//    carried as the ',' mark); anything else THROWS on encode.
//
//    Reversibility caveat (declared, not silent): because J collapses to I, the
//    round-trip is exact for the I/J-merged domain — decode(encode(t)) equals t
//    with every J turned to I. This is the defining, well-known property of a
//    5x5 Polybius square, and self-tests assert it explicitly.
//
//    Human-solvable in-world: the 5x5 grid is the keepers' "tally board"; row
//    then column counts (1–5) read straight off carved tick marks. Anyone who
//    can draw a 5x5 alphabet square solves it by hand.
// ===========================================================================

// 5x5 board, I/J share a cell. Order is A..Z skipping J. Rows/cols are 1..5.
const POLYBIUS_ALPHABET = 'ABCDEFGHIKLMNOPQRSTUVWXYZ'; // 25 letters, no J

/** letter -> "rc" pair on the 5x5 square (J folded to I), or null for non-letters. */
function polybiusPair(letter: string): string | null {
  let L = letter.toUpperCase();
  if (L === 'J') L = 'I';
  const idx = POLYBIUS_ALPHABET.indexOf(L);
  if (idx < 0) return null;
  const row = Math.floor(idx / 5) + 1;
  const col = (idx % 5) + 1;
  return `${row}${col}`;
}

/** "rc" pair -> letter (inverse of polybiusPair; never yields J). */
function polybiusLetter(pair: string): string {
  const row = pair.charCodeAt(0) - 49; // '1' -> 0
  const col = pair.charCodeAt(1) - 49;
  if (row < 0 || row > 4 || col < 0 || col > 4) {
    throw new Error(`polybius: pair "${pair}" out of 1-5 range`);
  }
  const ch = POLYBIUS_ALPHABET[row * 5 + col];
  if (ch === undefined) throw new Error(`polybius: no letter at "${pair}"`);
  return ch;
}

export const polybius = {
  /**
   * plaintext -> space-separated "rc" pairs (letters). Domain is LETTERS + SPACES
   * only: the word break (space) is encoded as the ',' separator mark (carveable,
   * see runes.ts) so a single space is reserved purely as the inter-pair delimiter
   * and the stream is never ambiguous. Any other character (digits, punctuation)
   * causes a THROW — a digit would be indistinguishable from a grid count, so we
   * fail honestly rather than silently corrupt the round-trip (cf. bookCipher).
   */
  encode(text: string): string {
    const tokens: string[] = [];
    for (const ch of text) {
      if (ch === ' ') {
        tokens.push(',');
        continue;
      }
      const pair = polybiusPair(ch);
      if (pair === null) {
        throw new Error(`polybius: character ${JSON.stringify(ch)} is not a letter or space`);
      }
      tokens.push(pair);
    }
    return tokens.join(' ');
  },
  /** space-delimited "rc"/',' tokens -> plaintext (UPPERCASE, J->I). */
  decode(text: string): string {
    if (text.length === 0) return '';
    const tokens = text.split(' ');
    let out = '';
    for (const tok of tokens) {
      if (/^[1-5][1-5]$/.test(tok)) {
        out += polybiusLetter(tok);
      } else if (tok === ',') {
        out += ' '; // the encoded word break
      } else {
        throw new Error(`polybius: token ${JSON.stringify(tok)} is not a 1-5 pair or word break`);
      }
    }
    return out;
  },
} as const;

// ===========================================================================
// 10) A1Z26 (letters <-> ordinal numbers)
//     A=1, B=2, ... Z=26. Letters become their 1-based alphabet position; the
//     numbers are space-separated. Domain: LETTERS + SPACES (word break carried
//     as the ',' mark); any other character THROWS on encode (a literal digit is
//     indistinguishable from an ordinal, so we fail honestly rather than corrupt).
//
//     Human-solvable in-world: the simplest "count the letters" code — a solver
//     numbers the alphabet 1..26 and reads off. Great as an early/teaching clue;
//     tick-mark carvings (1..26) read straight off the stave.
// ===========================================================================

export const a1z26 = {
  /**
   * plaintext -> space-separated ordinals. Domain is LETTERS + SPACES only: the
   * word break (space) is encoded as the ',' separator mark (carveable, see
   * runes.ts) so a single space stays reserved purely as the inter-number
   * delimiter. Any other character (digits, punctuation) causes a THROW — a
   * literal digit would be indistinguishable from an ordinal, so we fail honestly
   * rather than silently corrupt the round-trip (cf. bookCipher).
   */
  encode(text: string): string {
    const tokens: string[] = [];
    for (const ch of text) {
      if (ch === ' ') {
        tokens.push(',');
        continue;
      }
      const code = ch.charCodeAt(0);
      if (isUpper(code)) tokens.push(String(code - 64));
      else if (isLower(code)) tokens.push(String(code - 96));
      else throw new Error(`a1z26: character ${JSON.stringify(ch)} is not a letter or space`);
    }
    return tokens.join(' ');
  },
  /** space-delimited tokens -> plaintext (UPPERCASE). Inverse over the A-Z domain. */
  decode(text: string): string {
    if (text.length === 0) return '';
    const tokens = text.split(' ');
    let out = '';
    for (const tok of tokens) {
      if (/^(?:[1-9]|1[0-9]|2[0-6])$/.test(tok)) {
        out += String.fromCharCode(64 + parseInt(tok, 10));
      } else if (tok === ',') {
        out += ' '; // the encoded word break
      } else {
        throw new Error(`a1z26: token ${JSON.stringify(tok)} is not an ordinal 1-26 or word break`);
      }
    }
    return out;
  },
} as const;

// ===========================================================================
// 11) MORSE / TAP (expressible as runes or marks)
//     Each letter/digit -> International Morse, rendered with rune-safe MARKS:
//     DOT='.', DASH='-' (both exist in the keepers' structural marks, see
//     runes.ts MARK_BRANCHES), a single SPACE between letters, and ' / ' between
//     words. This keeps the whole stream carveable in the keepers' script.
//
//     Reversibility: Morse is delimited per letter by spaces (and ' / ' between
//     words), so decode is unambiguous. Round-trip is exact over the supported
//     domain (A-Z, 0-9, word spaces); decode returns UPPERCASE. Unsupported
//     characters cause encode to THROW (an un-tappable clue is a real failure,
//     matching the bookCipher "honest failure" convention).
//
//     Human-solvable in-world: dots and dashes are the keepers' tap-code; the
//     carved '.'/'-' marks read as a Morse chart. A solver with a Morse table (a
//     common in-world prop) reads it directly.
// ===========================================================================

const MORSE_TABLE: Readonly<Record<string, string>> = Object.freeze({
  A: '.-', B: '-...', C: '-.-.', D: '-..', E: '.', F: '..-.', G: '--.',
  H: '....', I: '..', J: '.---', K: '-.-', L: '.-..', M: '--', N: '-.',
  O: '---', P: '.--.', Q: '--.-', R: '.-.', S: '...', T: '-', U: '..-',
  V: '...-', W: '.--', X: '-..-', Y: '-.--', Z: '--..',
  '0': '-----', '1': '.----', '2': '..---', '3': '...--', '4': '....-',
  '5': '.....', '6': '-....', '7': '--...', '8': '---..', '9': '----.',
});

const MORSE_INVERSE: Readonly<Record<string, string>> = Object.freeze(
  Object.fromEntries(Object.entries(MORSE_TABLE).map(([ch, code]) => [code, ch])),
);

export const morse = {
  /**
   * plaintext -> Morse using '.'/'-', single space between letters, ' / ' between
   * words. Throws on any character with no Morse code (un-tappable = a real bug).
   */
  encode(text: string): string {
    const words = text.toUpperCase().split(/\s+/).filter((w) => w.length > 0);
    return words
      .map((word) =>
        [...word]
          .map((ch) => {
            const code = MORSE_TABLE[ch];
            if (code === undefined) {
              throw new Error(`morse: character ${JSON.stringify(ch)} has no Morse code`);
            }
            return code;
          })
          .join(' '),
      )
      .join(' / ');
  },
  /** Morse ('.'/'-' + letter spaces + ' / ' words) -> plaintext (UPPERCASE). */
  decode(text: string): string {
    const trimmed = text.trim();
    if (trimmed.length === 0) return '';
    const words = trimmed.split(/\s*\/\s*/);
    return words
      .map((word) =>
        word
          .trim()
          .split(/\s+/)
          .filter((c) => c.length > 0)
          .map((code) => {
            const ch = MORSE_INVERSE[code];
            if (ch === undefined) {
              throw new Error(`morse: code ${JSON.stringify(code)} is not valid Morse`);
            }
            return ch;
          })
          .join(''),
      )
      .join(' ');
  },
} as const;

// ===========================================================================
// SELF-TESTS — assert decode(encode(t,key),key) === t for every cipher.
// Throws on the first mismatch. Pure; safe to run at import or in `sample`.
// ===========================================================================

function assertEq(actual: unknown, expected: unknown, label: string): void {
  const a = typeof actual === 'string' ? actual : JSON.stringify(actual);
  const e = typeof expected === 'string' ? expected : JSON.stringify(expected);
  if (a !== e) {
    throw new Error(`SELF-TEST FAILED [${label}]\n  expected: ${e}\n  actual:   ${a}`);
  }
}

export function runSelfTests(): { passed: number; cases: string[] } {
  const cases: string[] = [];
  const ok = (label: string) => cases.push(label);

  // ---- caesar ----
  for (const key of [1, 3, 13, 25, 26, 0, -3, 40]) {
    for (const t of ['ATTACK AT DAWN', 'Keepers Watch 7', 'zzz AAA', '']) {
      assertEq(caesar.decode(caesar.encode(t, key), key), t, `caesar key=${key} "${t}"`);
    }
  }
  ok('caesar: round-trips across keys incl. negative/overflow + pass-through');

  // ---- atbash (involution) ----
  for (const t of ['THE DEEP GOES DARK', 'Mixed Case 12', '', 'A']) {
    assertEq(atbash.decode(atbash.encode(t)), t, `atbash "${t}"`);
    assertEq(atbash.encode(t), atbash.decode(t), `atbash involution "${t}"`);
  }
  ok('atbash: round-trips and is its own inverse');

  // ---- vigenere ----
  for (const key of ['KEEPER', 'observance', 'A', 'ZZZ']) {
    for (const t of ['BOW AT THE MARKER', 'Salt of the old keepers!', 'unbroken light', '']) {
      assertEq(vigenere.decode(vigenere.encode(t, key), key), t, `vigenere key=${key} "${t}"`);
    }
  }
  ok('vigenere: round-trips across keys with non-letter pass-through');

  // ---- substitution (rune map) ----
  for (const t of ['THE KEPT LIGHT', 'wake the keeper', 'OFFER THE FIRST ORE 1']) {
    const enc = substitution.encode(t);
    assertEq(substitution.decode(enc), t.toUpperCase(), `substitution "${t}"`);
  }
  // every single letter survives the round-trip
  for (const L of RUNE_LETTERS) {
    assertEq(substitution.decode(substitution.encode(L)), L, `substitution letter ${L}`);
  }
  ok('substitution: bijective over A-Z + pass-through, uppercased domain');

  // ---- book cipher ----
  {
    const book = [
      'The keepers watch the deep and the dark.',
      'Bow at the marker or the marker bows to none.',
      '',
      'Salt of the old keepers seals the threshold.',
      'An unbroken light must burn at home.',
    ].join('\n');
    const plain = 'BOW AT THE MARKER';
    const refs = bookCipher.encode(plain, book);
    assertEq(bookCipher.decode(refs, book), plain, 'bookCipher basic round-trip');
    // format <-> parse is an inverse pair
    assertEq(bookCipher.parse(bookCipher.format(refs)), refs, 'bookCipher format/parse');
    // multi-page (page break = blank line)
    const refs2 = bookCipher.encode('SALT SEALS THE THRESHOLD', book);
    assertEq(
      bookCipher.decode(refs2, book),
      'SALT SEALS THE THRESHOLD',
      'bookCipher multi-page round-trip',
    );
    // missing word must throw
    let threw = false;
    try {
      bookCipher.encode('DRAGON', book);
    } catch {
      threw = true;
    }
    assertEq(threw, true, 'bookCipher throws on un-addressable word');
  }
  ok('bookCipher: page/line/word round-trips, format/parse inverse, throws on miss');

  // ---- coord encode ----
  for (const c of [
    { x: 0, z: 0 },
    { x: 64, z: -1280 },
    { x: -1, z: 1 },
    { x: 25, z: 26 },
    { x: 100, z: 0 }, // leading-zero trap: "100" must not be read as 1/0/0
    { x: -7, z: -7 }, // both axes negative (sign mark on both)
    { x: 30000000, z: -30000000 }, // Minecraft world border magnitude
    { x: -2147483648, z: 2147483647 }, // 32-bit signed extremes
  ]) {
    const enc = coordEncode.encode(c);
    assertEq(coordEncode.decode(enc), c, `coordEncode (${c.x},${c.z})`);
    // Output must be a REAL coordinate the script can carve: an axis label, an
    // optional taught '-' sign mark, base-10 DIGIT glyphs, the ',' separator —
    // and NO base-26 letters spelling a magnitude (the bug we fixed). Every
    // emitted char must have a carved glyph in runes.ts.
    if (!/^X-?[0-9]+,Z-?[0-9]+$/.test(enc)) {
      throw new Error(`coordEncode produced non-coordinate output: "${enc}"`);
    }
    for (const ch of enc) {
      if (!SUPPORTED_CHARS.has(ch) && !SUPPORTED_CHARS.has(ch.toUpperCase())) {
        throw new Error(`coordEncode emitted uncarvable char ${JSON.stringify(ch)} in "${enc}"`);
      }
    }
  }
  // The fix in one assertion: x=64 reads as the number 64, NOT the word "PCM".
  assertEq(coordEncode.encode({ x: 64, z: 0 }), 'X64,Z0', 'coordEncode reads as a real number');
  // Malformed / non-canonical inputs must be rejected (keeps decode honest).
  for (const bad of ['X64', 'X64,Z', 'X64,Y0', 'XAB,Z0', 'X-0,Z0', 'X01,Z0', 'X 64,Z0']) {
    let threw = false;
    try {
      coordEncode.decode(bad);
    } catch {
      threw = true;
    }
    assertEq(threw, true, `coordEncode rejects malformed "${bad}"`);
  }
  ok('coordEncode: lossless x/z round-trip incl. 32-bit extremes, reads as a real coordinate');

  // ---- rail fence ----
  for (const rails of [1, 2, 3, 4, 7, 26]) {
    for (const t of [
      'WE ARE DISCOVERED FLEE AT ONCE',
      'Keepers Watch 7!',
      'AB',
      'A',
      '',
      'zzzz aaaa 1234',
    ]) {
      assertEq(railFence.decode(railFence.encode(t, rails), rails), t, `railFence r=${rails} "${t}"`);
    }
  }
  // rails >= length and rails === 1 both degrade to identity
  assertEq(railFence.encode('HELLO', 1), 'HELLO', 'railFence rails=1 is identity');
  assertEq(railFence.encode('HI', 9), 'HI', 'railFence rails>=length is identity');
  // pure permutation: ciphertext is an anagram of the plaintext
  {
    const t = 'WE ARE DISCOVERED';
    const sortKey = (s: string) => [...s].sort().join('');
    assertEq(sortKey(railFence.encode(t, 3)), sortKey(t), 'railFence preserves the multiset of chars');
  }
  // bad rails must throw
  {
    let threw = false;
    try {
      railFence.encode('X', 0);
    } catch {
      threw = true;
    }
    assertEq(threw, true, 'railFence throws on rails < 1');
  }
  ok('railFence: round-trips across rail counts, identity edges, pure permutation, throws on bad rails');

  // ---- keyed columnar transposition ----
  for (const key of ['ZEBRA', 'KEEPER', 'A', 'observance', 'BCA']) {
    for (const t of [
      'BOW AT THE MARKER',
      'Salt of the old keepers!',
      'A',
      '',
      'attackatdawnxx',
      '1 2 3 4 5 6 7',
    ]) {
      assertEq(columnar.decode(columnar.encode(t, key), key), t, `columnar key=${key} "${t}"`);
    }
  }
  // pure permutation: ciphertext is an anagram of the plaintext
  {
    const t = 'WE ARE DISCOVERED FLEE';
    const sortKey = (s: string) => [...s].sort().join('');
    assertEq(sortKey(columnar.encode(t, 'ZEBRA')), sortKey(t), 'columnar preserves the multiset of chars');
  }
  // repeated key letters break ties left-to-right, still reversible
  assertEq(
    columnar.decode(columnar.encode('THRESHOLD', 'ABBA'), 'ABBA'),
    'THRESHOLD',
    'columnar handles repeated key letters',
  );
  // empty key must throw
  {
    let threw = false;
    try {
      columnar.encode('X', '123');
    } catch {
      threw = true;
    }
    assertEq(threw, true, 'columnar throws on key with no letters');
  }
  ok('columnar: round-trips across keys, repeated-letter ties, pure permutation, throws on empty key');

  // ---- polybius square (I/J merged domain) ----
  {
    // decode(encode(t)) == t with every J folded to I (the defining property).
    const fold = (s: string) => s.toUpperCase().replace(/J/g, 'I');
    for (const t of [
      'THE KEPT LIGHT',
      'wake the keeper',
      'JUMP THE JADE JOIST', // J-heavy: proves the declared I/J fold
      'OFFER THE FIRST ORE', // multi-word: word breaks survive via the ',' mark
      '',
      'A',
    ]) {
      assertEq(polybius.decode(polybius.encode(t)), fold(t), `polybius "${t}"`);
    }
    // every non-J letter is an exact fixed point of the fold + round-trip
    for (const L of 'ABCDEFGHIKLMNOPQRSTUVWXYZ') {
      assertEq(polybius.decode(polybius.encode(L)), L, `polybius letter ${L}`);
    }
    // every pair is two digits in 1..5
    if (!/^[1-5][1-5]$/.test(polybius.encode('A'))) {
      throw new Error('polybius: A must encode to a 1-5 digit pair');
    }
    // out-of-domain char (digit/punctuation) must throw — fail honestly.
    let threw = false;
    try {
      polybius.encode('ORE 1');
    } catch {
      threw = true;
    }
    assertEq(threw, true, 'polybius throws on non-letter/space input');
  }
  ok('polybius: round-trips over the I/J-merged 5x5 square, pairs are 1-5, throws off-domain');

  // ---- a1z26 ----
  {
    const up = (s: string) => s.toUpperCase();
    for (const t of [
      'BOW AT THE MARKER',
      'wake the keeper',
      'ZA AZ', // boundary letters 26/1
      'OFFER THREE ORE', // multi-word: word breaks survive via the ',' mark
      '',
      'A',
    ]) {
      assertEq(a1z26.decode(a1z26.encode(t)), up(t), `a1z26 "${t}"`);
    }
    // exact boundaries
    assertEq(a1z26.encode('A'), '1', 'a1z26 A=1');
    assertEq(a1z26.encode('Z'), '26', 'a1z26 Z=26');
    // every letter round-trips
    for (const L of RUNE_LETTERS) {
      assertEq(a1z26.decode(a1z26.encode(L)), L, `a1z26 letter ${L}`);
    }
    // out-of-domain char (digit/punctuation) must throw — fail honestly.
    let threw = false;
    try {
      a1z26.encode('ORE 3');
    } catch {
      threw = true;
    }
    assertEq(threw, true, 'a1z26 throws on non-letter/space input');
  }
  ok('a1z26: letter<->ordinal round-trips incl. 1/26 boundaries, throws off-domain');

  // ---- morse / tap ----
  {
    const up = (s: string) =>
      s.toUpperCase().split(/\s+/).filter((w) => w.length > 0).join(' ');
    for (const t of [
      'SOS',
      'BOW AT THE MARKER',
      'KEEPERS WATCH 7',
      'THE 1 LIGHT',
      'A',
    ]) {
      assertEq(morse.decode(morse.encode(t)), up(t), `morse "${t}"`);
    }
    // canonical SOS shape
    assertEq(morse.encode('SOS'), '... --- ...', 'morse SOS canonical');
    // output uses only carveable marks: '.', '-', ' ', '/'
    if (!/^[.\-/ ]+$/.test(morse.encode('BOW AT THE MARKER'))) {
      throw new Error('morse: output must be dots/dashes/spaces/slashes only');
    }
    // un-tappable char must throw on encode
    let threwEnc = false;
    try {
      morse.encode('HELLO!'); // '!' has no Morse code in our table
    } catch {
      threwEnc = true;
    }
    assertEq(threwEnc, true, 'morse throws on un-tappable character');
    // invalid Morse must throw on decode
    let threwDec = false;
    try {
      morse.decode('........'); // 8 dots is not a valid letter
    } catch {
      threwDec = true;
    }
    assertEq(threwDec, true, 'morse throws on invalid code');
  }
  ok('morse: round-trips over A-Z/0-9 + words, carveable marks only, throws on un-tappable/invalid');

  return { passed: cases.length, cases };
}
