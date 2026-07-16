/**
 * /whisper requests an exact authored H1-H3 body. M2 classifies solution hints
 * as A3: this handler may stage a pending request, but it cannot spend, reveal,
 * record delivery, or enact a toll before an exact, unexpired approval.
 */
import { MessageFlags, type AutocompleteInteraction, type ChatInputCommandInteraction } from 'discord.js';
import {
  getPlayerByDiscordId,
  getOpenPuzzles,
  getArcAct,
  getBudget,
  countWhispersForPuzzle,
  getHint,
  searchHintedPuzzles,
  enqueueBeat,
  logEvent,
} from '../../db/repo.js';
import { voice } from '../../voice.js';
import { authoredPayloadSha256 } from '../../v5/approval-gates.js';

const SOURCE = 'the-watcher/whisper';

export async function handleWhisperAutocomplete(interaction: AutocompleteInteraction): Promise<void> {
  const focused = interaction.options.getFocused();
  const choices = await searchHintedPuzzles(typeof focused === 'string' ? focused : '');
  await interaction.respond(choices.map((choice) => ({
    name: choice.title ? `${choice.title} (${choice.puzzleKey})`.slice(0, 100) : choice.puzzleKey,
    value: choice.puzzleKey,
  })));
}

export async function handleWhisper(interaction: ChatInputCommandInteraction): Promise<void> {
  const puzzleKey = normalizePuzzleRef(interaction.options.getString('puzzle', true));
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });

  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.editReply({ content: voice.notLinked() });
    return;
  }

  const open = await getOpenPuzzles();
  if (!open.some((puzzle) => puzzle.puzzle_key === puzzleKey)) {
    await speak(interaction, voice.whisperUnknown());
    return;
  }

  const act = await getArcAct();
  const budget = await getBudget(player.id, act);
  const remaining = budget ? budget.budget + budget.earned - budget.spent : 0;
  if (!budget || remaining <= 0) {
    await speak(interaction, voice.noBudget());
    return;
  }

  const tier = await countWhispersForPuzzle(player.id, puzzleKey) + 1;
  const hint = await getHint(puzzleKey, tier);
  if (!hint) {
    await speak(interaction, voice.whisperUnknown());
    return;
  }

  const authoredPayload = {
    body: hint.body,
    player_id: player.id,
    player_uuid: player.mc_uuid,
    puzzle: puzzleKey,
    tier,
    act,
  };
  await enqueueBeat('hint_request', player.mc_uuid, {
    ...authoredPayload,
    approval_class: 'A3',
    approval_scope: `player:${player.id}/puzzle:${puzzleKey}/tier:${tier}`,
    authored_payload_sha256: authoredPayloadSha256(authoredPayload),
    idempotency_key: `discord:whisper:${interaction.id}`,
  }, 'pending');

  await logEvent(
    'info',
    SOURCE,
    `whisper approval requested: ${player.name} ${puzzleKey} t${tier} (act ${act})`,
  );
  await speak(interaction, voice.whisperPending());
}

async function speak(interaction: ChatInputCommandInteraction, content: string): Promise<void> {
  await interaction.editReply({ content });
}

function normalizePuzzleRef(raw: string): string {
  return raw
    .trim()
    .toLowerCase()
    .normalize('NFKC')
    .replace(/['’]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}
