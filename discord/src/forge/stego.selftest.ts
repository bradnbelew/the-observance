/**
 * stego.selftest.ts — the BUILD-TIME invariant for P17 clue-card steganography
 * (WEB-MASTER §1.M2 / §2 GATE "Iss caught" in-road B). Runs `stegoSelfTest()`
 * standalone, exiting non-zero on any violation so it can gate the build / CI.
 *
 *   npx tsx src/forge/stego.selftest.ts
 *
 * What it proves:
 *   (1) the hidden Iss payload is exactly his name = the seed's Vigenère key
 *       ('ISS' on stone-iss-wall) — the stego "second door" can never drift from
 *       the cipher it opens;
 *   (2) the PREFERRED faint second-rune-layer embeds → extracts losslessly, is
 *       faint (0 < opacity < 1), and decodes to null on a clean card (no false
 *       "it knows you");
 *   (3) the LSB FALLBACK embeds → extracts on raw RGBA, leaves alpha untouched,
 *       fails closed on a clean/tampered card, and SURVIVES a lossless PNG
 *       encode→decode pass (Discord's PNG handling);
 *   (4) the capacity guard throws on an undersized card.
 *
 * It does NOT touch ciphers.ts or the X1 plaintext bind — it only hides the key.
 */
import { stegoSelfTest } from './stego.js';

try {
  const { cases } = stegoSelfTest();
  console.log(`stego self-tests passed (${cases.length}):`);
  for (const c of cases) console.log(`  ok   ${c}`);
  process.exit(0);
} catch (err) {
  console.error('stego self-tests FAILED:');
  console.error(`  ${err instanceof Error ? err.message : String(err)}`);
  process.exit(1);
}
