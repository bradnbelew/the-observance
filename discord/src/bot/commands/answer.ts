/**
 * /answer <text> [puzzle] — submit a solved clue's plaintext to the oracle.
 *
 * The shape of the rite (canon, ORACLE.md):
 *   1. resolve the discord user -> a bound keeper (players.discord_id).
 *      unbound -> voice.notLinked(), ephemeral. nothing is recorded as a solve
 *      (an unlinked keeper cannot be rewarded — they must /link first).
 *   2. hand the raw text to the shared resolver (oracle/resolve.ts): it
 *      normalizes, rate-limits, matches the OPEN web, guards replays, records an
 *      idempotent solve, and — on a genuinely-new correct solve — applies the
 *      outcome (sets flags, enqueues an 'approved' beat, picks the voice line).
 *   3. speak per the result:
 *        solved   -> the outcome's voice line (public — the world heard it).
 *                    if it opens a next clue, surface that key in register.
 *        withheld -> voice.oracleWithheld() (rate-limited / cap reached).
 *        silent   -> say NOTHING the player can read as a tell. a miss and a
 *                    replay are both met with the same neutral withholding so a
 *                    wrong answer never reveals closeness and a replay never
 *                    re-rewards. (ephemeral, so public chat isn't spammed.)
 *
 * The `[puzzle]` option is an author/debug convenience only: the resolver matches
 * the whole OPEN web regardless, so naming a puzzle does NOT narrow the match (it
 * is logged for context). Every player-facing string comes from voice.ts.
 */
import { MessageFlags, type ChatInputCommandInteraction } from 'discord.js';
import { getPlayerByDiscordId, logEvent } from '../../db/repo.js';
import { resolveAnswer } from '../../oracle/resolve.js';
import { voice } from '../../voice.js';

const SOURCE = 'the-watcher/answer';

export async function handleAnswer(
  interaction: ChatInputCommandInteraction,
): Promise<void> {
  const raw = interaction.options.getString('text', true);

  // 1. who are you, in the world? a solve must be a known keeper.
  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.reply({
      content: voice.notLinked(),
      flags: MessageFlags.Ephemeral,
    });
    return;
  }

  // 2. the shared resolver does ALL the work — same path the #the-record scan
  //    and the in-world sign use. We pass surface 'discord'.
  const result = await resolveAnswer(
    { player, discordId: interaction.user.id },
    raw,
    'discord',
  );

  // 3. speak per the result.
  switch (result.kind) {
    case 'solved': {
      // the watcher answers in the open — its words are public, the world heard.
      const next = result.nextPuzzleKey
        ? `\n\n${voice.oracleNextClue()}`
        : '';
      // the resolved outcome line already carries the right register; only
      // append the "way goes on" nudge if a next clue opened AND the line wasn't
      // itself the next-clue line (avoid doubling).
      const tail = result.nextPuzzleKey && result.outcomeType !== 'next_clue' ? next : '';
      await interaction.reply({ content: `${result.reply}${tail}` });
      return;
    }

    case 'withheld': {
      // rate-limited or capped — withhold, in voice, quietly.
      await interaction.reply({
        content: result.reply,
        flags: MessageFlags.Ephemeral,
      });
      return;
    }

    case 'silent': {
      // a miss or a replay. NEVER a tell. The slash command must answer SOMETHING
      // (an interaction left unacknowledged shows a red error), so we answer with
      // the same neutral withholding line, ephemerally — indistinguishable from a
      // cap. It reveals nothing about correctness or closeness.
      void logEvent('info', SOURCE, `${player.name} answered: ${result.reason}`);
      await interaction.reply({
        content: voice.oracleWithheld(),
        flags: MessageFlags.Ephemeral,
      });
      return;
    }
  }
}
