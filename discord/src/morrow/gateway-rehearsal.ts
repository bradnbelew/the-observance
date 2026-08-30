import { createHash, randomUUID } from 'node:crypto';
import {
  ChannelType,
  Client,
  GatewayIntentBits,
  PermissionFlagsBits,
  type Message,
  type TextChannel,
} from 'discord.js';
import 'dotenv/config';
import { MORROW_GROUP_RECEIPT_TEXT } from './contradiction.js';

const token = process.env.DISCORD_BOT_TOKEN?.trim();
const guildId = process.env.DISCORD_GUILD_ID?.trim();
const acknowledgement = process.env.MORROW_GATEWAY_REHEARSAL_ACK?.trim();
const expectedAcknowledgement = guildId ? `temporary-channel:${guildId}` : '';

if (!token || !guildId || acknowledgement !== expectedAcknowledgement) {
  throw new Error(
    'Gateway rehearsal refused: set DISCORD_BOT_TOKEN, DISCORD_GUILD_ID, and the exact '
      + 'MORROW_GATEWAY_REHEARSAL_ACK=temporary-channel:<guild-id> acknowledgement.',
  );
}

const runId = randomUUID();
const releaseId = `morrow.rehearsal.discord.${runId.replaceAll('-', '').slice(0, 12)}`;
const eventId = randomUUID();
const nonce = createHash('sha256').update(`morrow:${eventId}`, 'utf8').digest('hex').slice(0, 25);
const content = `${MORROW_GROUP_RECEIPT_TEXT}\nRelease: ${releaseId} · receipt ${eventId}`;
const client = new Client({
  intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages],
});

let channel: TextChannel | null = null;
let cleanupStatus: 'not_created' | 'deleted' | 'failed' = 'not_created';

function waitForReady(): Promise<void> {
  if (client.isReady()) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Discord Gateway ready timeout')), 20_000);
    client.once('clientReady', () => {
      clearTimeout(timer);
      resolve();
    });
  });
}

function waitForMessage(channelId: string): Promise<Message> {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      client.off('messageCreate', onMessage);
      reject(new Error('Discord Gateway message timeout'));
    }, 20_000);
    const onMessage = (message: Message) => {
      if (message.channelId !== channelId || message.author.id !== client.user?.id) return;
      clearTimeout(timer);
      client.off('messageCreate', onMessage);
      resolve(message);
    };
    client.on('messageCreate', onMessage);
  });
}

try {
  await client.login(token);
  await waitForReady();
  const guild = await client.guilds.fetch(guildId);
  const me = guild.members.me ?? await guild.members.fetchMe();
  if (!me.permissions.has(PermissionFlagsBits.Administrator)
      && !me.permissions.has(PermissionFlagsBits.ManageChannels)) {
    throw new Error('Discord bot lacks permission to create the temporary rehearsal channel');
  }

  channel = await guild.channels.create({
    name: `morrow-rehearsal-${runId.slice(0, 8)}`,
    type: ChannelType.GuildText,
    topic: 'Temporary automated Morrow Gateway rehearsal; no player delivery.',
    permissionOverwrites: [
      { id: guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
      {
        id: client.user!.id,
        allow: [
          PermissionFlagsBits.ViewChannel,
          PermissionFlagsBits.SendMessages,
          PermissionFlagsBits.ReadMessageHistory,
        ],
      },
    ],
    reason: `Morrow Gateway rehearsal ${runId}`,
  });

  const gatewayMessage = waitForMessage(channel.id);
  const sent = await channel.send({
    content,
    nonce,
    enforceNonce: true,
    allowedMentions: { parse: [] },
  });
  const observed = await gatewayMessage;
  const fetched = await channel.messages.fetch(sent.id);
  const messageShapePass = observed.id === sent.id
    && observed.content === content
    && fetched.content === content
    && fetched.author.id === client.user!.id
    && !fetched.mentions.everyone
    && fetched.mentions.users.size === 0
    && fetched.mentions.roles.size === 0;
  if (!messageShapePass) throw new Error('Discord Gateway message shape did not match the authored receipt');

  console.log(JSON.stringify({
    status: 'pass',
    lane: 'temporary_private_channel_gateway_delivery',
    production_morrow_enabled: false,
    production_database_contacted: false,
    guild_id: guild.id,
    guild_name: guild.name,
    guild_member_count: guild.memberCount,
    channel_visibility: 'everyone_denied_bot_allowed',
    gateway_dispatch_observed: true,
    message_fetch_verified: true,
    authored_receipt_sha256: createHash('sha256').update(content, 'utf8').digest('hex'),
    nonce_length: nonce.length,
    enforce_nonce: true,
    allowed_mentions_parse: [],
    mentioned_users: fetched.mentions.users.size,
    mentioned_roles: fetched.mentions.roles.size,
    mentioned_everyone: fetched.mentions.everyone,
    message_id: sent.id,
    channel_id: channel.id,
    run_id: runId,
    release_id: releaseId,
  }));
} finally {
  if (channel) {
    try {
      await channel.delete(`Morrow Gateway rehearsal cleanup ${runId}`);
      cleanupStatus = 'deleted';
    } catch {
      cleanupStatus = 'failed';
    }
  }
  client.destroy();
  console.error(JSON.stringify({ cleanup: cleanupStatus, run_id: runId }));
  if (cleanupStatus === 'failed') process.exitCode = 2;
}
