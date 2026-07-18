import { MessageFlags, type ChatInputCommandInteraction } from 'discord.js';
import { getArgEventHistory, getPlayerByDiscordId, recordArgEvent } from '../../db/repo.js';
import { postToTheRecord } from '../../showrunner/discord.js';

const PHASE_LABELS: Record<string, string> = {
  p1: 'Copperline recovery', p2: 'world handoff', p3: 'settlement accounts',
  p4: 'Mouth copies', p5: 'civic services', p6: 'six Keepers',
  p7: 'Nessa finding', p8: 'Hold repair', p9: 'last company',
  p10: 'Wren', p11: 'Averyn', p12: 'release',
};

export async function handleInvestigate(interaction: ChatInputCommandInteraction): Promise<void> {
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });
  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.editReply('Link your Minecraft name first with /link.');
    return;
  }
  const action = interaction.options.getSubcommand(true);
  if (action === 'status') {
    const events = await getArgEventHistory();
    if (events.length === 0) {
      await interaction.editReply('No shared campaign action has been recorded yet.');
      return;
    }
    const latest = new Map<string, string>();
    for (const event of events) {
      const [phase = 'unknown'] = event.event_key.split('.');
      latest.set(phase, event.event_key);
    }
    const lines = [...latest.entries()].map(([phase, event]) => {
      const detail = event.split('.')[1] ?? event;
      return `- ${PHASE_LABELS[phase] ?? phase}: ${detail.replaceAll('_', ' ')}`;
    });
    await interaction.editReply(`Shared changes so far:\n${lines.join('\n')}`);
    return;
  }
  if (action === 'test-copy') {
    const method = interaction.options.getString('method', true);
    if (method !== 'barcode-and-node-clock') {
      await interaction.editReply('That test relies on timestamps or filenames written by the damaged guest. It cannot order the retained cartridges. Nothing changed.');
      return;
    }
    const result = await recordArgEvent({
      eventKey: 'p4.copy_hypothesis_tested',
      idempotencyKey: 'discord:p4:barcode-node-clock-test',
      source: 'discord',
      actorId: interaction.user.id,
      payload: { method, finding: 'copy-order-independent-of-guest-metadata' },
    });
    if (result.status === 'blocked') {
      await interaction.editReply('The retained Mouth revision is not available yet. Nothing changed.');
      return;
    }
    if (result.status === 'collision') {
      await interaction.editReply('A different copy test already owns that receipt. Nothing changed; use /investigate status.');
      return;
    }
    await interaction.editReply(result.created
      ? 'Test complete. Barcode 03 was imaged before 04, and the independent recovery-node clock stayed stable. Guest filenames and modified times cannot reverse that order.'
      : 'That custody test is already complete. Nothing was duplicated.');
    if (result.created) {
      void postToTheRecord('Copy test complete: cartridge 03 precedes 04. The recovery-node clock stayed stable; guest filenames and modified times are excluded from the order.');
    }
    return;
  }
  if (action !== 'dispatch') throw new Error('unsupported investigate action');

  const summary = interaction.options.getString('summary', true).normalize('NFKC').trim().replace(/\s+/g, ' ');
  if (summary.length < 12 || summary.length > 180) {
    await interaction.editReply('Write one plain 12-180 character summary of the disagreement you want the settlement to keep open.');
    return;
  }
  const result = await recordArgEvent({
    eventKey: 'p3.dispatch_authorized',
    idempotencyKey: 'discord:p3:settlement-dispatch',
    source: 'discord',
    actorId: interaction.user.id,
    payload: { summary, minecraft_name: player.name },
  });
  if (result.status === 'blocked') {
    await interaction.editReply('The settlement interviews are not open yet. Nothing changed.');
    return;
  }
  if (result.status === 'collision') {
    await interaction.editReply('A settlement dispatch is already on file. Use /investigate status to read the shared state.');
    return;
  }
  await interaction.editReply(result.created
    ? 'Dispatch accepted. The settlement will keep the conflicting accounts open instead of forcing one version.'
    : 'That dispatch is already on file. Nothing was duplicated.');
  if (result.created) {
    void postToTheRecord('Settlement dispatch received. Aro and Dob will keep both accounts in the public record until the Mouth copies can be compared.');
  }
}
