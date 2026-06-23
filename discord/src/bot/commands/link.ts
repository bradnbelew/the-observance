/**
 * /link <name> — bind a discord voice to a name worn in the world.
 *
 * The watcher matches the offered name against the keepers it has already seen
 * (repo.linkDiscord, case-insensitive on players.name) and sets discord_id.
 *   - matched  -> voice.linked(name), confirming in the watcher's tongue.
 *   - no match -> voice.linkUnknown(name): walk the ways once where it can see
 *     you, then offer the name again.
 *
 * Replies are ephemeral — a binding is between the keeper and the record.
 * Every player-facing string comes from voice.ts.
 */
import { MessageFlags, type ChatInputCommandInteraction } from 'discord.js';
import { linkDiscord, logEvent } from '../../db/repo.js';
import { voice } from '../../voice.js';

const SOURCE = 'the-watcher/link';

export async function handleLink(
  interaction: ChatInputCommandInteraction,
): Promise<void> {
  const name = interaction.options.getString('name', true).trim();

  const player = await linkDiscord(interaction.user.id, name);

  if (!player) {
    await interaction.reply({
      content: voice.linkUnknown(name),
      flags: MessageFlags.Ephemeral,
    });
    return;
  }

  await logEvent(
    'info',
    SOURCE,
    `bound discord ${interaction.user.id} -> ${player.name} (${player.id})`,
  );

  await interaction.reply({
    content: voice.linked(player.name),
    flags: MessageFlags.Ephemeral,
  });
}
