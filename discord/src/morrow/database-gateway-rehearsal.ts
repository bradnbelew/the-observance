import { randomUUID } from 'node:crypto';
import {
  ChannelType,
  Client,
  GatewayIntentBits,
  PermissionFlagsBits,
  REST,
  Routes,
  SlashCommandBuilder,
  type ButtonInteraction,
  type ChatInputCommandInteraction,
  type Interaction,
  type RESTPostAPIApplicationGuildCommandsJSONBody,
  type StringSelectMenuInteraction,
  type TextChannel,
} from 'discord.js';
import 'dotenv/config';

const VALIDATION_PROJECT_REF = 'snmqaqlagwzptzqbaiws';
const token = process.env.DISCORD_BOT_TOKEN?.trim();
const appId = process.env.DISCORD_APP_ID?.trim();
const guildId = process.env.DISCORD_GUILD_ID?.trim();
const supabaseUrl = process.env.SUPABASE_URL?.trim();
const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY?.trim();
const releaseId = process.env.MORROW_RELEASE_ID?.trim();
const acknowledgement = process.env.MORROW_DATABASE_GATEWAY_REHEARSAL_ACK?.trim();
const expectedAcknowledgement = guildId
  ? `validation-project:${VALIDATION_PROJECT_REF}:temporary-command-and-channel:${guildId}`
  : '';

const projectRef = (() => {
  try { return supabaseUrl ? new URL(supabaseUrl).hostname.split('.')[0] : ''; } catch { return ''; }
})();

if (!token || !appId || !guildId || !serviceRoleKey || projectRef !== VALIDATION_PROJECT_REF
    || !releaseId?.startsWith('morrow.rehearsal.discord.')
    || acknowledgement !== expectedAcknowledgement) {
  throw new Error(
    'Database/Gateway rehearsal refused: bind the validation project service-role client, a '
      + 'morrow.rehearsal.discord.* release, and the exact '
      + 'MORROW_DATABASE_GATEWAY_REHEARSAL_ACK=validation-project:<ref>:'
      + 'temporary-command-and-channel:<guild-id> acknowledgement.',
  );
}

const runId = randomUUID();
const workerId = randomUUID();
const commandName = `morrow-r-${runId.replaceAll('-', '').slice(0, 10)}`;
const timeoutMilliseconds = 15 * 60 * 1_000;
const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages] });
const rest = new REST({ version: '10' }).setToken(token);

let channel: TextChannel | null = null;
let commandId: string | null = null;
let initialBatch = { claimed: 0, applied: 0, failed: 0 };
let receiptBatch = { claimed: 0, applied: 0, failed: 0 };
let receiptMessageId: string | null = null;
let playerDiscordId: string | null = null;
let cleanupChannel: 'not_created' | 'deleted' | 'failed' = 'not_created';
let cleanupCommand: 'not_created' | 'deleted' | 'failed' = 'not_created';
let cleanupPromise: Promise<void> | null = null;

function waitForReady(): Promise<void> {
  if (client.isReady()) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Discord Gateway ready timeout')), 20_000);
    client.once('clientReady', () => { clearTimeout(timer); resolve(); });
  });
}

function commandPayload(): RESTPostAPIApplicationGuildCommandsJSONBody {
  return new SlashCommandBuilder()
    .setName(commandName)
    .setDescription('open the temporary Morrow validation exception.')
    .toJSON();
}

async function verifyServiceRoleBinding(): Promise<void> {
  const response = await fetch(
    `${supabaseUrl}/rest/v1/rpc/morrow_complete_discord_projection`,
    {
      method: 'POST',
      headers: {
        apikey: serviceRoleKey!,
        authorization: `Bearer ${serviceRoleKey}`,
        'content-type': 'application/json',
        'x-application': 'the-observance-discord-rehearsal-preflight',
      },
      body: JSON.stringify({
        p_event_id: randomUUID(),
        p_worker_id: randomUUID(),
        p_release_id: releaseId,
        p_applied: false,
        p_error: 'validation preflight',
      }),
    },
  );
  if (!response.ok) {
    await response.body?.cancel();
    throw new Error(`Validation service-role preflight refused with HTTP ${response.status}`);
  }
  const result: unknown = await response.json();
  if (result !== false) throw new Error('Validation service-role preflight returned an unexpected result');
}

async function cleanup(): Promise<void> {
  if (cleanupPromise) return cleanupPromise;
  cleanupPromise = (async () => {
    if (commandId) {
      try {
        await rest.delete(Routes.applicationGuildCommand(appId!, guildId!, commandId));
        cleanupCommand = 'deleted';
      } catch { cleanupCommand = 'failed'; }
    }
    if (channel) {
      try {
        await channel.delete(`Morrow database/Gateway rehearsal cleanup ${runId}`);
        cleanupChannel = 'deleted';
      } catch { cleanupChannel = 'failed'; }
    }
  })();
  return cleanupPromise;
}

async function stopForSignal(exitCode: number): Promise<void> {
  await cleanup();
  client.destroy();
  process.exit(exitCode);
}

function cleanupFailed(): boolean {
  return cleanupChannel === 'failed' || cleanupCommand === 'failed';
}

const onSigint = () => { void stopForSignal(130); };
const onSigterm = () => { void stopForSignal(143); };
process.once('SIGINT', onSigint);
process.once('SIGTERM', onSigterm);

try {
  await verifyServiceRoleBinding();
  await client.login(token);
  await waitForReady();
  const readyClient = client as Client<true>;
  const guild = await readyClient.guilds.fetch(guildId);
  const owner = await guild.members.fetch(guild.ownerId);
  if (owner.user.bot) throw new Error('Guild owner is a bot; explicit rehearsal player binding required');
  playerDiscordId = owner.id;

  channel = await guild.channels.create({
    name: `morrow-db-${runId.slice(0, 8)}`,
    type: ChannelType.GuildText,
    topic: 'Temporary Morrow database/Gateway rehearsal; removed automatically.',
    permissionOverwrites: [
      { id: guild.roles.everyone.id, deny: [PermissionFlagsBits.ViewChannel] },
      {
        id: client.user!.id,
        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory],
      },
      {
        id: owner.id,
        allow: [PermissionFlagsBits.ViewChannel, PermissionFlagsBits.SendMessages, PermissionFlagsBits.ReadMessageHistory],
      },
    ],
    reason: `Morrow database/Gateway rehearsal ${runId}`,
  });

  process.env.MORROW_DISCORD_ENABLED = 'true';
  process.env.MORROW_DISCORD_CHANNEL_ID = channel.id;
  delete process.env.MORROW_DISCORD_THREAD_ID;

  const registered = await rest.post(
    Routes.applicationGuildCommands(appId, guildId),
    { body: commandPayload() },
  ) as { id?: unknown };
  if (typeof registered.id !== 'string') throw new Error('Temporary command registration returned no command id');
  commandId = registered.id;

  const [{ handleMorrow, handleMorrowComponent }, { runMorrowDiscordProjectionBatchOnce }] = await Promise.all([
    import('../bot/commands/morrow.js'),
    import('./projection-worker.js'),
  ]);

  initialBatch = await runMorrowDiscordProjectionBatchOnce(readyClient, { workerId });
  if (initialBatch.claimed !== 1 || initialBatch.applied !== 1 || initialBatch.failed !== 0) {
    throw new Error(`Expected one activation projection, received ${JSON.stringify(initialBatch)}`);
  }

  console.log(JSON.stringify({
    status: 'interaction_ready',
    run_id: runId,
    release_id: releaseId,
    guild_id: guild.id,
    channel_id: channel.id,
    command_name: commandName,
    player_discord_id: playerDiscordId,
    initial_batch: initialBatch,
    instruction: `Open the private channel and run /${commandName}; acknowledge, then classify as inferred source.`,
  }));

  await new Promise<void>((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('Linked-player interaction timeout')), timeoutMilliseconds);
    let handling = false;
    const finish = async () => {
      const messages = await channel!.messages.fetch({ limit: 10 });
      const receipt = messages.find((message) => message.author.id === client.user!.id
        && message.content.startsWith('**Mossfield recovery · group receipt**'));
      if (!receipt) throw new Error('Projection applied without a visible group receipt');
      receiptMessageId = receipt.id;
      clearTimeout(timer);
      client.off('interactionCreate', onInteraction);
      resolve();
    };
    const onInteraction = async (interaction: Interaction) => {
      if (handling || interaction.guildId !== guildId || interaction.channelId !== channel!.id) return;
      handling = true;
      try {
        if (interaction.isChatInputCommand() && interaction.commandName === commandName) {
          await handleMorrow(interaction as ChatInputCommandInteraction);
        } else if (interaction.isButton() || interaction.isStringSelectMenu()) {
          const handled = await handleMorrowComponent(
            interaction as ButtonInteraction | StringSelectMenuInteraction,
          );
          if (handled) {
            receiptBatch = await runMorrowDiscordProjectionBatchOnce(readyClient, { workerId });
            if (receiptBatch.applied === 1) await finish();
          }
        }
      } catch (error) {
        clearTimeout(timer);
        client.off('interactionCreate', onInteraction);
        reject(error);
      } finally {
        handling = false;
      }
    };
    client.on('interactionCreate', onInteraction);
  });

  console.log(JSON.stringify({
    status: 'pass',
    lane: 'service_role_database_claim_gateway_and_linked_player_interaction',
    run_id: runId,
    release_id: releaseId,
    guild_id: guild.id,
    channel_id: channel.id,
    command_name: commandName,
    player_discord_id: playerDiscordId,
    initial_batch: initialBatch,
    receipt_batch: receiptBatch,
    receipt_message_id: receiptMessageId,
    private_payload_logged: false,
    production_project_contacted: false,
  }));
} catch (error) {
  if (error instanceof Error && 'reply' in (error as object)) {
    // Defensive marker only; interaction failures are handled without logging private reply bodies.
    console.error('Discord interaction failed safely.');
  }
  throw error;
} finally {
  process.off('SIGINT', onSigint);
  process.off('SIGTERM', onSigterm);
  await cleanup();
  client.destroy();
  console.error(JSON.stringify({
    cleanup_channel: cleanupChannel,
    cleanup_command: cleanupCommand,
    run_id: runId,
  }));
  if (cleanupFailed()) process.exitCode = 2;
}
