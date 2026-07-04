/**
 * seventh-reading.ts — THE SEVENTH READING (design/THE-SEVENTH-READING.md), the capstone cipher.
 *
 * The Seventh's name (AVERYN) was cut from the record — but the six keepers each hid one letter of it
 * before they fell, and each could only hide it the one way they knew: in their OWN technique. To read
 * the name the group must return to all six and read each "in their own tongue" one final time — the
 * final exam over everything the game taught. Six DIFFERENT techniques (NOT a Vigenère rerun):
 *
 *   Vaun   — Caesar (his shift 3)                          → A
 *   Mara   — book cipher (a capstone shelf)                → V
 *   Sella  — Atbash + the reflection                       → E
 *   Orin   — substitution (his rune alphabet)              → R
 *   Brann  — rail-fence, revealed only after dark          → Y
 *   Iss    — THE CATCH one last time (a warm lie; the      → N
 *            acrostic corrects it, the prophet-wall way)
 *
 * Each fragment DECODES to a short keeper confession whose FIRST TOKEN is the letter that keeper kept
 * (so the letter is EARNED by the decode, never sight-read). Read in FALL-ORDER (Vaun, Mara, Sella,
 * Orin, Brann, Iss) the six letters spell the name. Saying it triggers the release (finale.run.ts).
 *
 * This module is the machine-checked CONTENT HOME: `readingSelfTest()` proves every fragment round-trips
 * under its keeper's real forge cipher, that Iss's acrostic corrects his lie, and that the six letters
 * in fall-order spell AVERYN — so the capstone can never silently drift from the trigger row's answer.
 * PURE: same imports as the forge core; no DB / network.
 */

import { caesar, atbash, substitution, bookCipher, railFence } from './index.js';

/** The name the reading spells (lowercased = the `seventh-name` seed accepted_answer). */
export const SEVENTH_NAME = 'averyn';

/** Brann's rail count (his fire-count; the rails his line is raked along — kept small + fair). */
const BRANN_RAILS = 3;

/** The capstone shelf Mara's book fragment indexes into (contains every word of her line + the letter V). */
export const MARA_CAPSTONE_BOOK = [
  'v is a way i marked and did not take',
  'i read it and i read it again',
  'and did not walk it to them not once',
  'walk it to them now if you still can',
].join('\n\n');

/** One keeper's hidden letter of the name. `plaintext` is what the fragment DECODES to (its first token
 *  is `letter`); the in-world carving is that plaintext ENCODED under the keeper's technique. */
export interface ReadingFragment {
  readonly keeper: 'vaun' | 'mara' | 'sella' | 'orin' | 'brann' | 'iss';
  readonly technique: 'caesar' | 'book' | 'atbash' | 'substitution' | 'railfence' | 'catch';
  /** the letter this keeper kept (uppercase). */
  readonly letter: string;
  /** the decoded confession (its first whitespace token is `letter`). */
  readonly plaintext: string;
}

/**
 * The six fragments in FALL-ORDER (Vaun, Mara, Sella, Orin, Brann, Iss). Each plaintext opens on its
 * letter as a standalone token, then the keeper's confession about the name they could not keep.
 */
export const READING_FRAGMENTS: readonly ReadingFragment[] = [
  { keeper: 'vaun',  technique: 'caesar',       letter: 'A', plaintext: 'A THE FIRST OF THEIR NAME I KEPT IT AND GAVE NONE BACK' },
  { keeper: 'mara',  technique: 'book',         letter: 'V', plaintext: 'V I READ IT AND DID NOT WALK IT TO THEM' },
  { keeper: 'sella', technique: 'atbash',       letter: 'E', plaintext: 'E I KEPT IT AT THE FAR WATER' },
  { keeper: 'orin',  technique: 'substitution', letter: 'R', plaintext: 'R I WOULD NOT BOW TO GIVE IT AND GIVE IT NOW' },
  { keeper: 'brann', technique: 'railfence',    letter: 'Y', plaintext: 'Y I KEPT IT LIT BY THE ONE FIRE' },
  { keeper: 'iss',   technique: 'catch',        letter: 'N', plaintext: 'N THE LAST OF IT I CUT AND CALLED IT M' },
];

/**
 * ISS'S LAST LIE (the catch, one final time — NOT a Vigenère). Read straight, the warm surface says the
 * last letter is M (the comfort). The truth is recovered the prophet-wall way: read the FIRST mark of
 * each line, down (the acrostic), and it spells the correction. The surface letter and the true letter
 * must differ (a real lie), and the acrostic must resolve to the true letter.
 */
export const ISS_SURFACE_LETTER: string = 'M';   // the comforting lie, read straight
export const ISS_TRUE_LETTER: string = 'N';      // the truth, read by the catch
/** Iss's four warm lines; their initials (down) spell the catch: I·T·S·N → "its n". */
export const ISS_ACROSTIC_LINES = [
  'i told you the last of it was m',
  'take the first mark of each line down',
  'see what the warm words were laid over',
  'n is the letter i cut and called m',
];

/* ------------------------------------------------------------------ */
/*  Encoders — produce the CARVED ciphertext for each fragment (what   */
/*  the world-build carves at each keeper site; the players decode it).*/
/* ------------------------------------------------------------------ */

/** The carved ciphertext for a fragment (what a player decodes with the keeper's technique). Throws for
 *  the `catch` fragment (Iss's is not a letter-cipher — it is the acrostic lines above). */
export function encodeFragment(f: ReadingFragment): string {
  switch (f.technique) {
    case 'caesar':       return caesar.encode(f.plaintext, 3);
    case 'atbash':       return atbash.encode(f.plaintext);
    case 'substitution': return substitution.encode(f.plaintext);
    case 'railfence':    return railFence.encode(f.plaintext, BRANN_RAILS);
    case 'book':         return bookCipher.format(bookCipher.encode(f.plaintext, MARA_CAPSTONE_BOOK));
    case 'catch':        throw new Error('encodeFragment: the catch fragment (iss) is the acrostic, not a cipher');
  }
}

/** Decode a carved fragment ciphertext back to its plaintext (the inverse of {@link encodeFragment}). */
export function decodeFragment(f: ReadingFragment, ciphertext: string): string {
  switch (f.technique) {
    case 'caesar':       return caesar.decode(ciphertext, 3);
    case 'atbash':       return atbash.decode(ciphertext);
    case 'substitution': return substitution.decode(ciphertext);
    case 'railfence':    return railFence.decode(ciphertext, BRANN_RAILS);
    case 'book':         return bookCipher.decode(bookCipher.parse(ciphertext), MARA_CAPSTONE_BOOK);
    case 'catch':        throw new Error('decodeFragment: the catch fragment (iss) is the acrostic, not a cipher');
  }
}

/* ------------------------------------------------------------------ */
/*  BUILD-TIME INTEGRITY — the capstone can never drift from the name. */
/* ------------------------------------------------------------------ */

/** The name the six letters spell in fall-order (must equal SEVENTH_NAME, uppercased). */
export function assembledName(): string {
  return READING_FRAGMENTS.map((f) => f.letter).join('');
}

/**
 * readingSelfTest — proves the capstone holds together:
 *   (1) each fragment's first decoded token IS its letter (earned by the decode, never sight-read);
 *   (2) each CIPHER fragment round-trips under its keeper's real forge cipher (so the carving is
 *       producible AND decodes back to the confession — no shift/key/book/rail mistake);
 *   (3) Iss's catch: the surface letter ≠ the true letter (a real lie), and the acrostic (first mark of
 *       each line, down) spells the correction ending in the true letter;
 *   (4) the six letters in FALL-ORDER spell SEVENTH_NAME (== the `seventh-name` seed answer).
 * THROWS on any violation (build fails, not the player).
 */
export function readingSelfTest(): { passed: number; cases: string[] } {
  const cases: string[] = [];

  for (const f of READING_FRAGMENTS) {
    // (1) the letter is the first token of the confession.
    const firstToken = f.plaintext.trim().split(/\s+/)[0];
    if (firstToken !== f.letter) {
      throw new Error(`readingSelfTest [${f.keeper}]: first token "${firstToken}" !== letter "${f.letter}"`);
    }

    if (f.technique === 'catch') {
      // (3) Iss's lie: surface ≠ truth, and the letter IS the truth.
      if (ISS_SURFACE_LETTER === ISS_TRUE_LETTER) {
        throw new Error('readingSelfTest [iss]: surface letter equals the truth — not a lie');
      }
      if (f.letter !== ISS_TRUE_LETTER) {
        throw new Error(`readingSelfTest [iss]: fragment letter "${f.letter}" !== true letter "${ISS_TRUE_LETTER}"`);
      }
      const acrostic = ISS_ACROSTIC_LINES.map((l) => l.trim()[0]?.toUpperCase() ?? '').join('');
      if (!acrostic.endsWith(ISS_TRUE_LETTER)) {
        throw new Error(`readingSelfTest [iss]: acrostic "${acrostic}" does not resolve to the true letter "${ISS_TRUE_LETTER}"`);
      }
      cases.push(`iss (catch): surface lie "${ISS_SURFACE_LETTER}" → acrostic "${acrostic}" corrects to "${ISS_TRUE_LETTER}"`);
      continue;
    }

    // (2) the cipher round-trips: decode(encode(plaintext)) === plaintext (uppercase, forge-canonical).
    const ct = encodeFragment(f);
    const back = decodeFragment(f, ct).toUpperCase().trim();
    const want = f.plaintext.toUpperCase().trim();
    if (back !== want) {
      throw new Error(`readingSelfTest [${f.keeper}/${f.technique}]: decode(encode) "${back}" !== plaintext "${want}"`);
    }
    cases.push(`${f.keeper} (${f.technique}): "${want.slice(0, 24)}…" carves → decodes → letter "${f.letter}"`);
  }

  // (4) fall-order assembly spells the name.
  const name = assembledName();
  if (name.toLowerCase() !== SEVENTH_NAME) {
    throw new Error(`readingSelfTest: fall-order letters spell "${name}" !== SEVENTH_NAME "${SEVENTH_NAME.toUpperCase()}"`);
  }
  cases.push(`fall-order (vaun·mara·sella·orin·brann·iss) spells "${name}" == the seventh-name answer`);

  return { passed: cases.length, cases };
}
