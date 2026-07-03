/**
 * spoken-name.ts — the voice producer for `spine-spoken-name` (§8.3): when a player SAYS the catch's
 * truth aloud, the Observer hears it and the Watcher quotes it back. This is the voice→puzzle→payoff
 * bridge the seed marked "NEEDS PLUGIN PRODUCER (Observer voice scan)"; W5's capture makes it possible,
 * so the ciphers/story web actually leads into itself: hear a spoken truth → the world answers.
 *
 * GROUNDED + SAFE by construction:
 *   - fires ONLY on the REAL spoken phrase ("the one who turned away"), matched with the SAME normalizer
 *     the oracle uses (case/space/punctuation-insensitive substring) — never a fabricated callout.
 *   - solves through the SANCTIONED resolver by submitting the puzzle's opaque token, so every guard
 *     applies: the puzzle is OPEN only post-`iss_caught` (a says-it-too-early submission is a silent
 *     miss), the solve is idempotent (says-it-twice → already-solved → silent), and it is rate-limited.
 *   - the payoff (a `lore` "it knows" beat) is posted to #the-record; the puzzle GATES NOTHING, so if the
 *     voice layer is off this simply never fires (degrade to silence — the grounding invariant).
 *   - never throws; any stumble is silence.
 */
import { resolveAnswer } from '../oracle/resolve.js';
import { normalizeAnswer } from '../oracle/normalize.js';
import { postToTheRecord } from '../showrunner/discord.js';
import { logEvent } from '../db/repo.js';
import type { Player } from '../db/types.js';

/**
 * The catch's truth spoken aloud (design/PUZZLE-DESIGNS.md §8.3). Detected in a voice transcript; on a
 * match we submit the puzzle's opaque token below. If the seed's spoken truth ever changes, change this.
 */
const SPOKEN_TRUTH = 'the one who turned away';
/**
 * The opaque accepted token for `spine-spoken-name` (discord/supabase/seeds/puzzles_seed.sql). The Observer
 * "posts" this on the real spoken phrase — the standard plugin-produced pattern. Kept in sync with the seed.
 */
const SPOKEN_TOKEN = 'q5k8 mq3w x1n7 t2d6 heard aloud';

/** Pre-normalized needle so we don't re-normalize the constant on every utterance. */
const TRUTH_NEEDLE = normalizeAnswer(SPOKEN_TRUTH);

/**
 * maybeSolveSpokenName — if a captured voice transcript contains the spoken truth, solve spine-spoken-name
 * for that player and post the Watcher's reply. A no-op (silent) on non-match, unlinked player, closed
 * puzzle, replay, or any failure. Never throws.
 */
export async function maybeSolveSpokenName(player: Player, transcript: string): Promise<void> {
  try {
    const heard = normalizeAnswer(transcript);
    if (!heard || !heard.includes(TRUTH_NEEDLE)) return; // not the real spoken phrase → nothing

    const result = await resolveAnswer(
      { player, discordId: player.discord_id ?? null },
      SPOKEN_TOKEN,
      'world',
    );
    // Only a genuinely-new solve speaks. withheld/silent (closed / replay / rate-limited) say nothing.
    if (result.kind === 'solved') {
      await postToTheRecord(result.reply);
      await logEvent('info', 'the-watcher.voice', `spoken-name: ${player.name} said it aloud — it was heard`);
    }
  } catch {
    /* a missing payoff is always safer than a broken capture — silence-is-canon */
  }
}
