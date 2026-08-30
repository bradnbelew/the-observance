import {
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  MessageFlags,
  StringSelectMenuBuilder,
  type ButtonInteraction,
  type ChatInputCommandInteraction,
  type MessageActionRowComponentBuilder,
  type StringSelectMenuInteraction,
} from 'discord.js';
import { config } from '../../config.js';
import {
  CONTRADICTION_DECISIONS,
  MORROW_CONTRADICTION_TTL_MS,
  authoredPrivateEvidence,
  contradictionCustomId,
  decisionFeedback,
  newContradictionNonce,
  parseContradictionCustomId,
  resolveInteractionScope,
  validDecision,
  type ContradictionAction,
} from '../../morrow/contradiction.js';
import { applyMorrowContradiction, openMorrowContradiction } from '../../morrow/repo.js';

const LOCKED = 'No current private recovery exception is assigned to this linked identity in this channel. Nothing changed.';
const UNAVAILABLE = 'The private recovery desk is temporarily unavailable. No receipt was changed; retry safely.';

function configuredScope() {
  if (!config.morrow.enabled || !config.morrow.releaseId || !config.morrow.channelId) return null;
  return {
    guildId: config.discord.guildId,
    channelId: config.morrow.channelId,
    threadId: config.morrow.threadId,
  };
}

function interactionScope(interaction: ChatInputCommandInteraction | ButtonInteraction | StringSelectMenuInteraction) {
  const isThread = interaction.channel?.isThread() ?? false;
  return {
    guildId: interaction.guildId,
    channelId: interaction.channelId,
    parentChannelId: isThread && interaction.channel && 'parentId' in interaction.channel
      ? interaction.channel.parentId
      : null,
    isThread,
  };
}

function offerComponents(nonce: string): ActionRowBuilder<MessageActionRowComponentBuilder>[] {
  return [new ActionRowBuilder<MessageActionRowComponentBuilder>().addComponents(
    new ButtonBuilder().setCustomId(contradictionCustomId(nonce, 'ack'))
      .setLabel('Acknowledge privately').setStyle(ButtonStyle.Primary),
    new ButtonBuilder().setCustomId(contradictionCustomId(nonce, 'decline'))
      .setLabel('Decline').setStyle(ButtonStyle.Secondary),
    new ButtonBuilder().setCustomId(contradictionCustomId(nonce, 'cancel'))
      .setLabel('Cancel').setStyle(ButtonStyle.Secondary),
  )];
}

function decisionComponents(nonce: string): ActionRowBuilder<MessageActionRowComponentBuilder>[] {
  const select = new StringSelectMenuBuilder()
    .setCustomId(contradictionCustomId(nonce, 'decision'))
    .setPlaceholder('Classify the provenance conflict')
    .setMinValues(1).setMaxValues(1)
    .addOptions(CONTRADICTION_DECISIONS.map((choice) => ({
      label: choice.label,
      value: choice.value,
      description: choice.description,
    })));
  return [
    new ActionRowBuilder<MessageActionRowComponentBuilder>().addComponents(select),
    new ActionRowBuilder<MessageActionRowComponentBuilder>().addComponents(
      new ButtonBuilder().setCustomId(contradictionCustomId(nonce, 'cancel'))
        .setLabel('Cancel without filing').setStyle(ButtonStyle.Secondary),
    ),
  ];
}

export async function handleMorrow(interaction: ChatInputCommandInteraction): Promise<void> {
  await interaction.deferReply({ flags: MessageFlags.Ephemeral });
  const configured = configuredScope();
  if (!configured) {
    await interaction.editReply(UNAVAILABLE);
    return;
  }
  const scope = resolveInteractionScope(interactionScope(interaction), configured);
  if (!scope.ok) {
    await interaction.editReply(LOCKED);
    return;
  }
  const nonce = newContradictionNonce();
  try {
    const result = await openMorrowContradiction({
      discordUserId: interaction.user.id,
      releaseId: config.morrow.releaseId!,
      guildId: configured.guildId,
      channelId: scope.channelId,
      threadId: scope.threadId,
      nonce,
      expiresAt: new Date(Date.now() + MORROW_CONTRADICTION_TTL_MS).toISOString(),
    });
    if (result.status === 'complete') {
      await interaction.editReply('Your linked-player classification is already included in the group receipt. No private payload was repeated.');
      return;
    }
    if (result.status === 'blocked' || !result.displayAlias || !result.evidenceVariant || !result.expiresAt) {
      await interaction.editReply(LOCKED);
      return;
    }
    const expiry = Math.floor(Date.parse(result.expiresAt) / 1_000);
    await interaction.editReply({
      content: `${authoredPrivateEvidence(result.displayAlias, result.evidenceVariant)}\n\n`
        + `Linked group: ${result.groupSize} player${result.groupSize === 1 ? '' : 's'} · nonce expires <t:${expiry}:R>.`,
      components: offerComponents(nonce),
      allowedMentions: { parse: [] },
    });
  } catch {
    await interaction.editReply(UNAVAILABLE);
  }
}

export async function handleMorrowComponent(
  interaction: ButtonInteraction | StringSelectMenuInteraction,
): Promise<boolean> {
  const parsed = parseContradictionCustomId(interaction.customId);
  if (!parsed) return false;
  const configured = configuredScope();
  const scope = configured ? resolveInteractionScope(interactionScope(interaction), configured) : { ok: false } as const;
  if (!configured || !scope.ok) {
    await interaction.reply({ content: LOCKED, flags: MessageFlags.Ephemeral });
    return true;
  }
  let decision = null;
  if (parsed.action === 'decision') {
    if (!interaction.isStringSelectMenu() || !interaction.values[0] || !validDecision(interaction.values[0])) {
      await interaction.reply({ content: LOCKED, flags: MessageFlags.Ephemeral });
      return true;
    }
    decision = interaction.values[0];
  } else if (!interaction.isButton()) {
    await interaction.reply({ content: LOCKED, flags: MessageFlags.Ephemeral });
    return true;
  }
  await interaction.deferUpdate();
  try {
    const result = await applyMorrowContradiction({
      discordUserId: interaction.user.id,
      releaseId: config.morrow.releaseId!,
      guildId: configured.guildId,
      channelId: scope.channelId,
      threadId: scope.threadId,
      nonce: parsed.nonce,
      action: parsed.action as ContradictionAction,
      decision,
    });
    if (result.status === 'acknowledged') {
      await interaction.editReply({
        content: 'Private custody exception acknowledged. File only the provenance classification; no prose is graded.',
        components: decisionComponents(parsed.nonce),
      });
      return true;
    }
    if (result.status === 'declined') {
      await interaction.editReply({ content: 'Private evidence declined. No vote or group receipt was created; /morrow can recover the offer later.', components: [] });
      return true;
    }
    if (result.status === 'cancelled') {
      await interaction.editReply({ content: 'Private filing cancelled. No vote or progression receipt was created.', components: [] });
      return true;
    }
    if (result.status === 'expired') {
      await interaction.editReply({ content: 'That private nonce expired without changing the case. Use /morrow to recover a fresh offer.', components: [] });
      return true;
    }
    if (result.status === 'incorrect') {
      await interaction.editReply({ content: decision ? decisionFeedback(decision) : LOCKED, components: decisionComponents(parsed.nonce) });
      return true;
    }
    if (result.status === 'pending') {
      await interaction.editReply({
        content: `${decision ? decisionFeedback(decision) : 'Classification retained.'}\n`
          + `Group receipt pending: ${result.accepted}/${result.required} linked-player classifications.`,
        components: [],
      });
      return true;
    }
    if (result.status === 'committed' || result.status === 'duplicate') {
      await interaction.editReply({
        content: `${decision ? decisionFeedback(decision) : 'Classification retained.'}\n`
          + (result.status === 'committed'
            ? 'The spoiler-free group receipt is queued for durable delivery.'
            : 'The group receipt is already committed. Nothing was duplicated.'),
        components: [],
      });
      return true;
    }
    await interaction.editReply({
      content: result.status === 'collision'
        ? 'The stored receipt differs from this interaction. Processing halted without changing the case.'
        : LOCKED,
      components: [],
    });
    return true;
  } catch {
    await interaction.editReply({ content: UNAVAILABLE, components: [] });
    return true;
  }
}
