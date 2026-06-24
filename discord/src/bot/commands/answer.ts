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
import { postToTheRecord } from '../../showrunner/discord.js';

const SOURCE = 'the-watcher/answer';

export async function handleAnswer(
  interaction: ChatInputCommandInteraction,
): Promise<void> {
  const raw = interaction.options.getString('text', true);

  // CRITICAL (audit): defer IMMEDIATELY. resolveAnswer does ~8-10 serial Supabase round-trips that
  // can exceed Discord's 3s ack window and fire a red "application did not respond" on camera. We
  // defer EPHEMERAL — a miss/withhold must never surface publicly — then editReply. A genuine SOLVE
  // is additionally posted to #the-record (below) so "the world heard it" without the timeout risk.
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });

  // 1. who are you, in the world? a solve must be a known keeper.
  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.editReply({ content: voice.notLinked() });
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
      // The resolver already picked the ONE in-register line for this outcome
      // (next_clue says "the way goes on"; side_quest says "this is not the way";
      // main_beat says "it turns"). We do NOT append a second line: appending the
      // next_clue nudge to a side_quest/main_beat would contradict its own voice,
      // and the next clue itself is surfaced in-world (the forged carving / beat),
      // never by doubling a line here. Speak the single resolved line, verbatim.
      // The ephemeral editReply is the solver's ack; the PUBLIC record of the solve goes to
      // #the-record (the shared record channel) — "the world heard it" — fire-and-forget.
      await interaction.editReply({ content: result.reply });
      void postToTheRecord(result.reply);
      return;
    }

    case 'withheld': {
      // rate-limited or capped — withhold, in voice, quietly (ephemeral via the defer).
      await interaction.editReply({ content: result.reply });
      return;
    }

    case 'silent': {
      // a miss or a replay. NEVER a tell. The slash command must answer SOMETHING
      // (an interaction left unacknowledged shows a red error), so we answer with
      // the same neutral withholding line, ephemerally — indistinguishable from a
      // cap. It reveals nothing about correctness or closeness.
      void logEvent('info', SOURCE, `${player.name} answered: ${result.reason}`);
      await interaction.editReply({ content: voice.oracleWithheld() });
      return;
    }
  }
}
