/**
 * /whisper <puzzle> — ask the watcher for a hint, and pay the toll.
 *
 * The shape of the rite (canon):
 *   1. resolve the discord user -> a bound keeper (players.discord_id).
 *      unbound -> voice.notLinked(), ephemeral. nothing is spent.
 *   2. read the current act and the keeper's whisper budget for it.
 *      no whispers left -> voice.noBudget(). nothing is spent.
 *   3. tier = (whispers already given for THIS puzzle) + 1. the first whisper is
 *      a nudge; later tiers speak the keeper's seeded words more plainly.
 *      no seeded words at this tier -> voice.whisperUnknown() (in-character
 *      deferral, same as noBudget — it withholds, it does not error).
 *   4. speak: voice.whisperReply(tier, body), then voice.whisperToll().
 *   5. take the cost: spendWhisper(player, act). record the whisper_event.
 *      enqueue a 'whisper_toll' beat so the plugin enacts the in-world toll
 *      (it keeps the player's light for the night).
 *
 * The toll IS the cost. There is no casting, no "chosen" — judgment is by conduct.
 * Every player-facing string comes from voice.ts; this file writes no English.
 */
import { MessageFlags, type AutocompleteInteraction, type ChatInputCommandInteraction } from 'discord.js';
import {
  getPlayerByDiscordId,
  getArcAct,
  getBudget,
  countWhispersForPuzzle,
  getHint,
  searchHintedPuzzles,
  spendWhisper,
  recordWhisperEvent,
  enqueueBeat,
  logEvent,
} from '../../db/repo.js';
import { voice } from '../../voice.js';

const SOURCE = 'the-watcher/whisper';

export async function handleWhisperAutocomplete(
  interaction: AutocompleteInteraction,
): Promise<void> {
  const focused = interaction.options.getFocused();
  const choices = await searchHintedPuzzles(typeof focused === 'string' ? focused : '');
  await interaction.respond(choices.map((choice) => ({
    name: choice.title ? `${choice.title} (${choice.puzzleKey})`.slice(0, 100) : choice.puzzleKey,
    value: choice.puzzleKey,
  })));
}

export async function handleWhisper(
  interaction: ChatInputCommandInteraction,
): Promise<void> {
  const puzzleKey = normalizePuzzleRef(interaction.options.getString('puzzle', true));

  // CRITICAL (audit): defer ephemeral immediately — the budget/hint lookups are several Supabase
  // round-trips that can blow Discord's 3s window. All whisper replies are ephemeral, so editReply.
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });

  // 1. who are you, in the world?
  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.editReply({ content: voice.notLinked() });
    return;
  }

  // 2. what is owed this act, and what remains?
  const act = await getArcAct();
  const budget = await getBudget(player.id, act);
  const remaining = budget ? budget.budget + budget.earned - budget.spent : 0;
  if (!budget || remaining <= 0) {
    await speak(interaction, voice.noBudget());
    return;
  }

  // 3. how plainly should it speak? tier rises with each whisper for this puzzle.
  const given = await countWhispersForPuzzle(player.id, puzzleKey);
  const tier = given + 1;

  const hint = await getHint(puzzleKey, tier);
  // tier 1 is the watcher's own nudge and needs no seeded body; deeper tiers do.
  if (!hint && tier > 1) {
    await speak(interaction, voice.whisperUnknown());
    return;
  }

  // 4. take the cost first — never speak the words if the toll cannot be taken.
  const spend = await spendWhisper(player.id, act);
  if (!spend.ok) {
    // lost the race or just exhausted — withhold, in voice.
    await speak(interaction, voice.noBudget());
    return;
  }

  // record the whisper so the next tier is reachable, and the ledger is true.
  await recordWhisperEvent(player.id, puzzleKey, tier);

  // enqueue the in-world toll for the plugin to enact (the keeper's light).
  // PLAYER-EARNED → 'approved': the keeper paid the toll, the world answers now,
  // never gated behind a dashboard approval.
  await enqueueBeat('whisper_toll', player.mc_uuid, { puzzle: puzzleKey, tier }, 'approved');

  await logEvent(
    'info',
    SOURCE,
    `whisper kept: ${player.name} ${puzzleKey} t${tier} (act ${act}, ${spend.remaining} left)`,
  );

  // 5. speak the whisper, then state the toll. two lines, the watcher's cadence.
  const body = voice.whisperReply(tier, hint?.body ?? '');
  await speak(interaction, `${body}\n\n${voice.whisperToll()}`);
}

/**
 * The watcher answers in the channel where it was asked — its words are public,
 * the toll is announced, not hidden. Run /whisper anywhere (e.g. #general); there
 * is no dedicated whispers channel.
 */
async function speak(
  interaction: ChatInputCommandInteraction,
  content: string,
): Promise<void> {
  // handleWhisper defers ephemeral up front, so every result path edits that deferred reply.
  await interaction.editReply({ content });
}

function normalizePuzzleRef(raw: string): string {
  return raw
    .trim()
    .toLowerCase()
    .normalize('NFKC')
    .replace(/['']/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}
