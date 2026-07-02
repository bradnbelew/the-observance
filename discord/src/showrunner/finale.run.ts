/**
 * finale.run.ts — the I/O wrapper for the M5 close (A1/A2, WEB-MASTER §5). Posts the composed ending
 * to #the-record ONCE the Accepting instant is reached, set-once via `state.finale_posted`.
 *
 * This is the SOLE consumer that makes `seventh_choice` (and the reckoning_free finale cost)
 * player-facing: the pure composer (finale.ts) assembles the authored Watcher-register lines from the
 * ending enum + the seventh choice + the fork leaves + the free-branch cost; this wrapper reads those
 * flags, gates on the clock, posts the close, and advances the high-water.
 *
 * REGISTER (the separation law). Everything posted here is WATCHER register (voice.ts) to #the-record.
 * Wren's SET-A human last-words are delivered in-world by companion.run.ts — the two channels are never
 * crossed.
 *
 * IDEMPOTENCY. `state.finale_posted` is the single high-water; a re-run after a successful post no-ops.
 * A restart mid-tick re-derives the same single close (the composer is pure + deterministic; the fate /
 * fork / seventh flags are all set-once upstream). Fault-isolated: a failed post leaves the high-water
 * unset so the next tick retries, never a partial/duplicate close.
 */
import { logEvent } from '../db/repo.js';
import { postToTheRecord } from './discord.js';
import { instantReached } from './clock.js';
import { composeFinale, type FinaleComposeInput } from './finale.js';
import type { EndingFate } from './fate.js';
import type { ShowrunnerState } from './state.js';

/** The ending-fate values the composer accepts (mirror of fate.ts EndingFate for a safe cast). */
const FATES: ReadonlySet<string> = new Set<EndingFate>(['kept', 'cast_out', 'divided', 'refusers']);

export interface FinalePassResult {
  posted: boolean;
  /** state.finale_posted advanced (the caller persists). */
  dirty: boolean;
}

/**
 * runFinalePass — post the composed M5 close once the Accepting instant is reached. Reads the ending
 * fate + seventh choice + fork leaves + reckoning_free from the passed arc flags (already fetched by
 * runAutonomyPasses). No-ops when: already posted, the instant is unbound/not reached, or no ending
 * fate has been decided yet (the close needs at least its base opener, which is fate-driven).
 */
export async function runFinalePass(
  flags: Record<string, unknown>,
  acceptingInstantMs: number | null,
  nowMs: number,
  state: ShowrunnerState,
): Promise<FinalePassResult> {
  const result: FinalePassResult = { posted: false, dirty: false };

  if (state.finale_posted === true) return result; // set-once
  if (!instantReached(acceptingInstantMs, nowMs)) return result; // not the appointed moment yet

  // The base close is the fate opener; without a decided ending fate there is nothing to open with, so
  // the close waits (precision: never a guessed fate). ending_fate is set-once by the fate sentinel.
  const fateRaw = flags.ending_fate;
  if (typeof fateRaw !== 'string' || !FATES.has(fateRaw)) {
    await logEvent('info', 'showrunner.finale', 'accepting instant reached but ending_fate not yet decided — close waits');
    return result;
  }

  const input: FinaleComposeInput = {
    fate: fateRaw as EndingFate,
    seventhChoice:
      flags.seventh_choice === 'restore' ? 'restore'
      : flags.seventh_choice === 'erase' ? 'erase'
      : null,
    nameSpoken: flags.name_spoken === true,
    nameUnspoken: flags.name_unspoken === true,
    lightKept: flags.light_kept === true,
    lightTaken: flags.light_taken === true,
    sacredBeastBroken: flags.sacred_beast_broken === true,
    inheritorsCodicil: flags.inheritors_codicil === true || flags.seventh_named === true,
    // The reckoning is OPTIONAL content — only the FREE branch carries a finale cost (condemn/understand
    // do not reach the ending with a price; their weight is in-world + the sharp-quote shift).
    reckoningFree: flags.reckoning_free === true,
  };

  const { lines, reason } = composeFinale(input);
  // Post the whole close as ONE #the-record message (blank-line separated), so it lands as a single
  // ending beat, not a scattered drip. voice.ts owns every line; nothing is composed here.
  const ok = await postToTheRecord(lines.join('\n\n'));
  if (!ok) {
    await logEvent('warn', 'showrunner.finale', 'failed to post the M5 close — leaving high-water unset to retry');
    return result;
  }

  state.finale_posted = true;
  result.posted = true;
  result.dirty = true;
  await logEvent('info', 'showrunner.finale', `M5 close posted (${reason})`);
  return result;
}
