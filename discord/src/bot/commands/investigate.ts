import {
  ActionRowBuilder,
  MessageFlags,
  ModalBuilder,
  TextInputBuilder,
  TextInputStyle,
  type ChatInputCommandInteraction,
  type ModalSubmitInteraction,
} from 'discord.js';
import { getArgEventHistory, getPlayerByDiscordId, recordArgEvent } from '../../db/repo.js';
import {
  NESSA_CORRECTION_CANONICAL_PAYLOAD,
  validNessaCorrection,
  type NessaFinding,
} from '../../v5/nessa-correction.js';

const PHASE_LABELS: Record<string, string> = {
  p1: 'Copperline recovery', p2: 'world handoff', p3: 'settlement accounts',
  p4: 'Mouth copies', p5: 'civic services', p6: 'six Keepers',
  p7: 'Nessa finding', p8: 'Hold repair', p9: 'last company',
  p10: 'Wren', p11: 'Averyn', p12: 'release',
};

export const NESSA_CORRECTION_MODAL_ID = 'observance:p7:nessa-correction:v1';

function nessaCorrectionModal(): ModalBuilder {
  const field = (id: string, label: string, placeholder: string) => new ActionRowBuilder<TextInputBuilder>()
    .addComponents(new TextInputBuilder().setCustomId(id).setLabel(label)
      .setPlaceholder(placeholder).setStyle(TextInputStyle.Short).setRequired(true).setMaxLength(120));
  return new ModalBuilder()
    .setCustomId(NESSA_CORRECTION_MODAL_ID)
    .setTitle('Public correction review')
    .addComponents(
      field('cause', 'Material cause and first failure place', 'Short finding'),
      field('record', 'What was changed in the chronology', 'Short finding'),
      field('conduct', 'What Nessa did and when', 'Short finding'),
    );
}

async function submitNessaCorrection(
  interaction: ChatInputCommandInteraction | ModalSubmitInteraction,
  finding: NessaFinding,
): Promise<void> {
  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.editReply('Link your Minecraft name first with /link.');
    return;
  }
  if (!validNessaCorrection(finding)) {
    await interaction.editReply('Those three findings do not yet support a complete public correction. Nothing changed.');
    return;
  }
  const result = await recordArgEvent({
    eventKey: 'p7.nessa_publicly_cleared',
    idempotencyKey: 'discord:p7:nessa-public-correction',
    source: 'discord',
    actorId: interaction.user.id,
    payload: NESSA_CORRECTION_CANONICAL_PAYLOAD,
  });
  if (result.status === 'blocked') {
    await interaction.editReply('The six Keeper responsibility records are not complete yet. Nothing changed.');
    return;
  }
  if (result.status === 'collision') {
    await interaction.editReply('A different Nessa correction already owns that receipt. Nothing changed; use /investigate status.');
    return;
  }
  await interaction.editReply(result.created
    ? 'Correction filed. The material failure, changed chronology, and Nessa\'s conduct are now separate public findings.'
    : 'That public correction is already filed. Nothing was duplicated.');
}

export async function handleInvestigateModal(interaction: ModalSubmitInteraction): Promise<boolean> {
  if (interaction.customId !== NESSA_CORRECTION_MODAL_ID) return false;
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });
  await submitNessaCorrection(interaction, {
    cause: interaction.fields.getTextInputValue('cause'),
    record: interaction.fields.getTextInputValue('record'),
    conduct: interaction.fields.getTextInputValue('conduct'),
  });
  return true;
}

export async function handleInvestigate(interaction: ChatInputCommandInteraction): Promise<void> {
  const action = interaction.options.getSubcommand(true);
  if (action === 'review-nessa') {
    await interaction.showModal(nessaCorrectionModal());
    return;
  }
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });
  const player = await getPlayerByDiscordId(interaction.user.id);
  if (!player) {
    await interaction.editReply('Link your Minecraft name first with /link.');
    return;
  }
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
    return;
  }
  if (action === 'file-nessa') {
    await submitNessaCorrection(interaction, {
      cause: interaction.options.getString('cause', true),
      record: interaction.options.getString('record', true),
      conduct: interaction.options.getString('conduct', true),
    });
    return;
  }
  if (action === 'confront-wren') {
    const sender = interaction.options.getString('sender', true);
    const payload = interaction.options.getString('payload', true);
    const proof = interaction.options.getString('proof', true);
    const motive = interaction.options.getString('motive', true);
    if (sender !== 'wren' || payload !== 'names-plans-routes-fears'
        || proof !== 'progressive-private-missing-countermark'
        || motive !== 'fear-explains-choice-responsibility-remains') {
      await interaction.editReply('That finding confuses opportunity with proof, reduces four packets to one accident, or lets motive erase the act. Nothing changed.');
      return;
    }
    const result = await recordArgEvent({
      eventKey: 'p10.wren_confronted',
      idempotencyKey: 'discord:p10:wren-transmission-finding',
      source: 'discord',
      actorId: interaction.user.id,
      payload: { sender, packet_payload: payload, proof, motive, observation_receipts: 0 },
    });
    if (result.status === 'blocked') {
      await interaction.editReply('The private revision window has not been filed yet. Nothing changed.');
      return;
    }
    if (result.status === 'collision') {
      await interaction.editReply('A different Wren finding already owns that receipt. Nothing changed; use /investigate status.');
      return;
    }
    await interaction.editReply(result.created
      ? 'Finding committed. Wren deliberately transmitted the four packet classes. Fear of being erased explains his choice and does not remove responsibility. The group’s remembrance remains unchosen.'
      : 'That Wren transmission finding is already committed. Nothing was duplicated.');
    return;
  }
  if (action === 'identify-averyn') {
    const name = interaction.options.getString('name', true).normalize('NFKC').trim().toUpperCase();
    const averyn = interaction.options.getString('averyn', true);
    const record = interaction.options.getString('record', true);
    const watcher = interaction.options.getString('watcher', true);
    const dark = interaction.options.getString('dark', true);
    if (name !== 'AVERYN' || averyn !== 'human-registrar-analyst'
        || record !== 'civic-system-trapped-her' || watcher !== 'constrained-record-voice'
        || dark !== 'related-distinct-unknown') {
      await interaction.editReply('The name or relationship model collapses a distinction the evidence keeps open. Nothing changed.');
      return;
    }
    const identity = await recordArgEvent({
      eventKey: 'p11.averyn_identified',
      idempotencyKey: 'discord:p11:averyn-six-affidavit-identity',
      source: 'discord',
      actorId: interaction.user.id,
      payload: { name, provenance: 'six-distinct-affidavit-paths', observation_receipts: 0 },
    });
    if (identity.status === 'blocked') {
      await interaction.editReply('The group’s Wren remembrance has not been committed yet. Nothing changed.');
      return;
    }
    if (identity.status === 'collision') {
      await interaction.editReply('A different identity filing already owns that receipt. Nothing changed; use /investigate status.');
      return;
    }
    const relationship = await recordArgEvent({
      eventKey: 'p11.averyn_restored_unbound',
      idempotencyKey: 'discord:p11:averyn-relationship-unbound',
      source: 'discord',
      actorId: interaction.user.id,
      payload: { averyn, record, watcher, dark, empty_record_slot: true, observation_receipts: 0 },
    });
    if (relationship.status === 'blocked') {
      await interaction.editReply('The name was preserved, but the relationship filing is not ready. Retry is safe; no source clicks are required.');
      return;
    }
    if (relationship.status === 'collision') {
      await interaction.editReply('The name is preserved, but a different relationship filing owns the second receipt. Nothing was overwritten.');
      return;
    }
    await interaction.editReply(relationship.created
      ? 'Averyn restored as a person and left unbound. The Record is the civic system; the Watcher is constrained system speech through her; the Dark remains related, distinct, and unknown.'
      : 'Averyn’s identity and unbound relationship record are already committed. Nothing was duplicated.');
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
}
