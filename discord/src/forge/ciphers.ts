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

import { RUNE_LETTERS, RUNE_MAP, RUNE_MAP_INVERSE } from './runes.js';

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
//    Scheme (deterministic, lossless for 32-bit signed ints):
//      - Map each integer to a sign letter + base-26 (A=0..Z=25) magnitude.
//      - Letters spell the magnitude; a leading 'N' marks negative, 'P' positive.
//      - Axes joined by a single space; output is pure A–Z + space, so it carves
//        cleanly with the keepers' script and is unambiguously decodable.
//
//    Example: (x=-1280, z=64) -> "NBXG PCM" (sign letter + base-26 magnitude per
//    axis). decode reverses it exactly back to {x:-1280, z:64}.
// ===========================================================================

export interface Coord {
  readonly x: number;
  readonly z: number;
}

const DIGIT26 = RUNE_LETTERS; // A..Z == 0..25, shares the rune alphabet

/** base-26 digit (0..25) -> its rune letter; throws if out of range. */
function digit26(d: number): string {
  const ch = DIGIT26[d];
  if (ch === undefined) throw new Error(`coordEncode: digit ${d} out of base-26 range`);
  return ch;
}

/** integer -> sign-prefixed base-26 letter string (e.g. -1280 -> "N" + base26). */
function intToRunes(n: number): string {
  if (!Number.isInteger(n)) throw new Error(`coordEncode: ${n} is not an integer`);
  const sign = n < 0 ? 'N' : 'P';
  let mag = Math.abs(n);
  if (mag === 0) return sign + digit26(0); // "PA" == 0
  let body = '';
  while (mag > 0) {
    body = digit26(mag % 26) + body;
    mag = Math.floor(mag / 26);
  }
  return sign + body;
}

/** inverse of intToRunes. */
function runesToInt(s: string): number {
  if (s.length < 2) throw new Error(`coordEncode: bad token "${s}"`);
  const sign = s[0];
  if (sign !== 'N' && sign !== 'P') throw new Error(`coordEncode: bad sign in "${s}"`);
  let mag = 0;
  for (let i = 1; i < s.length; i++) {
    const ch = s[i] as string; // i < length, so defined
    const d = DIGIT26.indexOf(ch);
    if (d < 0) throw new Error(`coordEncode: bad digit "${ch}" in "${s}"`);
    mag = mag * 26 + d;
  }
  return sign === 'N' ? -mag : mag;
}

export const coordEncode = {
  /** {x,z} -> rune-letter string ("<xtoken> <ztoken>"). */
  encode(coord: Coord): string {
    return `${intToRunes(coord.x)} ${intToRunes(coord.z)}`;
  },
  /** rune-letter string -> {x,z}. Exact inverse of encode. */
  decode(text: string): Coord {
    const parts = text.trim().split(/\s+/);
    const [xs, zs] = parts;
    if (parts.length !== 2 || xs === undefined || zs === undefined) {
      throw new Error(`coordEncode: expected two tokens, got "${text}"`);
    }
    return { x: runesToInt(xs), z: runesToInt(zs) };
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
    { x: 30000000, z: -30000000 }, // Minecraft world border magnitude
    { x: -2147483648, z: 2147483647 }, // 32-bit signed extremes
  ]) {
    const enc = coordEncode.encode(c);
    assertEq(coordEncode.decode(enc), c, `coordEncode (${c.x},${c.z})`);
    // output is pure A-Z + single space (carveable by the keepers' script)
    if (!/^[A-Z]+ [A-Z]+$/.test(enc)) {
      throw new Error(`coordEncode produced non-rune output: "${enc}"`);
    }
  }
  ok('coordEncode: lossless x/z round-trip incl. 32-bit extremes, rune-pure output');

  return { passed: cases.length, cases };
}
