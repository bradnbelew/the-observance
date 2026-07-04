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
import { logEvent, enqueueBeat } from '../db/repo.js';
import { postToTheRecord } from './discord.js';
import { instantReached } from './clock.js';
import { composeFinale, composeRelease, type FinaleComposeInput, type ReleaseComposeInput } from './finale.js';
import type { EndingFate } from './fate.js';
import { readSetting, type ShowrunnerState } from './state.js';
import type { BeatStatus } from '../db/types.js';

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

/**
 * runReleasePass — THE RELEASE (design/FINALE-THE-RELEASE.md). The FINAL beat, after the Accepting close.
 * Fires ONCE when the group has performed the release act (`record_released` set by the plugin's
 * ReleaseRiteListener), composing the mask-off farewell + the disconnect-screen kick line from the SAME
 * measured state the fate close reads, then:
 *   1. posts the farewell to #the-record (Watcher register, the one earned break — voice.ts owns it), and
 *   2. enqueues the `the_closing` beat (status 'approved' so it fires on the plugin's next poll) carrying
 *      the composed `kick_line` — the plugin runs the server-wide death theater + kicks every player.
 *
 * SET-ONCE via `state.release_posted`. Gated: needs `record_released` truthy AND a decided `ending_fate`
 * (the release tone is fate-driven; never a guessed fate). Fault-isolated: a failed post/enqueue leaves
 * the high-water unset so the next tick retries — never a partial/duplicate close, never a stranded kick.
 */
export async function runReleasePass(
  flags: Record<string, unknown>,
  state: ShowrunnerState,
): Promise<FinalePassResult> {
  const result: FinalePassResult = { posted: false, dirty: false };

  if (state.release_posted === true) return result; // set-once — the world ends exactly once
  if (flags.record_released !== true) return result; // the group has not performed the release act yet

  const fateRaw = flags.ending_fate;
  if (typeof fateRaw !== 'string' || !FATES.has(fateRaw)) {
    await logEvent('info', 'showrunner.finale', 'release act performed but ending_fate not yet decided — release waits');
    return result;
  }

  // The Seventh's restored name, if a canon one is set (arc flag `seventh_name` or the `seventh_name`
  // setting). Null → the release signs off as its reclaimed title ("the seventh, kept no longer"), so the
  // finale is complete WITHOUT inventing a name — Ethan can drop one in later with zero code change.
  const seventhName =
    typeof flags.seventh_name === 'string' && flags.seventh_name.trim() !== ''
      ? (flags.seventh_name as string)
      : await readSetting<string | null>('seventh_name', null);

  const input: ReleaseComposeInput = {
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
    reckoningFree: flags.reckoning_free === true,
    reckoningUnderstand: flags.reckoning_understand === true,
    reckoningCondemn: flags.reckoning_condemn === true,
    seventhName,
  };

  const { lines, kickLine, reason } = composeRelease(input);

  // 1. post the mask-off farewell to #the-record (ONE message, blank-line separated).
  const ok = await postToTheRecord(lines.join('\n\n'));
  if (!ok) {
    await logEvent('warn', 'showrunner.finale', 'failed to post the release farewell — leaving high-water unset to retry');
    return result;
  }

  // 2. enqueue the death/kick beat carrying the composed kick line (lore-agnostic on the plugin side).
  //    'approved' → fires on the plugin's next poll, no human gate (the players earned the ending).
  try {
    await enqueueBeat('the_closing', null, { kick_line: kickLine }, 'approved' satisfies BeatStatus, null);
  } catch (e) {
    // The farewell already posted; a failed enqueue must not double-post it. Mark posted so we don't
    // re-post the farewell, but log loudly — the operator can re-fire `the_closing` from the dashboard.
    await logEvent('error', 'showrunner.finale', `release farewell posted but the_closing enqueue FAILED (${e instanceof Error ? e.message : String(e)}) — re-enqueue the_closing manually`);
  }

  state.release_posted = true;
  result.posted = true;
  result.dirty = true;
  await logEvent('info', 'showrunner.finale', `THE RELEASE fired (${reason})`);
  return result;
}
