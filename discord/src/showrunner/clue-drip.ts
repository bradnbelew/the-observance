/**
 * clue-drip.ts — forge the dripped node's Discord clue card (COHERENCE-AUDIT C1 / P0-6).
 *
 * THE GAP THIS CLOSES (SEAM C / C1): the showrunner *announced* a puzzle but never
 * *surfaced its clue* — `apply.ts` posted only the generic `voice.drip()` line, and the
 * whole `discord/src/showrunner/` tree never touched the forge or the render layer. The
 * "tide" the engine is built around never actually washed up a clue to decode.
 *
 * THE FIX: when a forgeable node drips (AUTO), build its card from the SAME authored
 * forge-spec the world-placement step uses — `forgeForPuzzle(key)` resolves the P0-1
 * registry (clue-specs.ts), so the carved stone and the Discord card render ONE plaintext
 * by construction (the X1 bind). We then frame the forged runes with one of the themed
 * templates (`renderClueDetailed`) into a PNG the cron uploads via REST.
 *
 * SCOPE + FAULT ISOLATION:
 *   - ONLY forgeable cipher nodes have a card. Non-cipher rows are excluded from the drip
 *     pool upstream (snapshot.ts `forgeable` flag, C3 / P0-7); if one ever reaches here it
 *     throws UnknownPuzzleKeyError and the caller falls back to the in-world report line.
 *   - This module does the heavy, side-effect-free work (forge + satori/resvg render). It
 *     stays OUT of the pure `decide()` so the policy remains DB/render-free + unit-testable.
 *   - No text is composed here. The card carries forged runes + the template's seeded
 *     in-character framing only; voice.ts remains the SOLE player-facing text source.
 *
 * The render spec is built from the registry entry's `toSpec()` (the cipher + params), with
 * the keeper/doc surfaced as in-character chrome — never the plaintext, never the answer.
 */
import { clueSpecFor, forgeablePuzzleKeys, UnknownPuzzleKeyError } from '../forge/clue-specs.js';
import { renderClueDetailed } from '../forge/templates/index.js';

export interface DripCard {
  /** the rendered clue-card image bytes (ready to upload). */
  png: Buffer;
  /** attachment filename: `<authored puzzle_key>.png`. */
  filename: string;
}

/**
 * Forge + render the clue card for a forgeable dripped node. Deterministic + pure-ish (no
 * DB, no network; only satori/resvg rendering). THROWS {@link UnknownPuzzleKeyError} for a
 * key with no registered forge-spec — the caller (apply.ts) catches it and routes the drip
 * to the in-world-pointing report line instead of posting a card.
 */
export async function forgeDripCard(puzzleKey: string): Promise<DripCard> {
  const entry = clueSpecFor(puzzleKey);
  if (!entry) throw new UnknownPuzzleKeyError(puzzleKey);

  // The clue spec is the authored one (same as the world-placement step → no drift, X1).
  // The framing is in-character chrome only: the keeper's hand + which record it is. The
  // template defaults deterministically off the cipher kind + puzzleKey when omitted.
  // The framing is in-character chrome only (eyebrow); the template's key-hand hint defaults to
  // the forge's own seeded `meta.keyHint` (e.g. "turn the wheel three marks") — not English we
  // author here. The template is chosen deterministically off the cipher kind + puzzleKey.
  const { png, forged } = await renderClueDetailed({
    clue: entry.toSpec(),
    eyebrow: 'the record',
  });

  return { png, filename: `${forged.puzzleKey}.png` };
}

/** True iff this key has a forgeable clue card (mirror of snapshot's pool eligibility). */
export function isForgeableDrip(puzzleKey: string): boolean {
  return forgeablePuzzleKeys().includes(puzzleKey);
}
