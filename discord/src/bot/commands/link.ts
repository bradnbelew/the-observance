/**
 * /link <name> <callback> <code> — prove the online Minecraft hand, then atomically bind it.
 *
 * The service-role RPC validates the Copperline callback and LS06 Orientation prerequisite before it can touch
 * players.discord_id. It also owns concurrency, idempotent replay, conflict privacy, and recovery
 * from this Discord account's accidental prior name. Replies remain ephemeral.
 */
import { MessageFlags, type ChatInputCommandInteraction } from 'discord.js';
import { claimIdentityHandoff, logEvent } from '../../db/repo.js';
import { voice } from '../../voice.js';

const SOURCE = 'the-watcher/link';

export async function handleLink(
  interaction: ChatInputCommandInteraction,
): Promise<void> {
  const name = interaction.options.getString('name', true).trim();
  const callback = interaction.options.getString('callback', true).trim();
  const code = interaction.options.getString('code', true).trim();

  // Defer before any network I/O so a slow database round trip cannot exceed Discord's response
  // deadline. Identity responses are private even when a name is unknown or already claimed.
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });

  try {
    const result = await claimIdentityHandoff(interaction.user.id, name, callback, code);
    if (result.state === 'invalid') {
      await interaction.editReply({ content: voice.handoffRejected() });
      return;
    }
    if (result.state === 'blocked') {
      await interaction.editReply({ content: voice.handoffBlocked() });
      return;
    }
    if (result.state === 'unknown' || result.state === 'conflict'
      || result.state === 'challenge' || !result.player) {
      // One response covers absent, ambiguous, privately claimed, expired, consumed, and wrong proof.
      await interaction.editReply({ content: voice.handoffProofRejected() });
      return;
    }

    const action = result.recovered
      ? 'recovered'
      : result.inserted
        ? 'filed'
        : 'replayed';
    await logEvent(
      'info',
      SOURCE,
      `${action} LS05 identity handoff for ${result.player.name} (${result.player.id})`,
    );
    await interaction.editReply({
      content: result.recovered
        ? voice.handoffRecovered(result.player.name)
        : voice.handoffReceipt(),
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    void logEvent('error', SOURCE, `LS05 atomic identity handoff failed: ${message}`);
    await interaction.editReply({ content: voice.handoffUnavailable() });
  }
}
