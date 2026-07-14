/**
 * `npm run dev` — THE WATCHER comes online.
 *
 * A discord.js v14 client that:
 *   - wears the presence: Watching "the ways" (BOT_PRESENCE from voice.ts).
 *   - ensures the guild's slash commands (/whisper, /link, /answer) are registered.
 *   - routes interactions to the command handlers under ./commands.
 *
 * The Watcher is the record-keeping facet of the presence — the land's memory.
 * It never breaks character. Every line it utters comes from voice.ts; nothing
 * here writes English of its own. Internal stumbles are logged to the record
 * (repo.logEvent) and answered to the player only as voice.quiet().
 */
import {
  Client,
  GatewayIntentBits,
  MessageFlags,
  PermissionFlagsBits,
  type Interaction,
  type ChatInputCommandInteraction,
  type Message,
} from 'discord.js';
import { config } from '../config.js';
import { getPlayerByDiscordId, logEvent } from '../db/repo.js';
import { voice, BOT_PRESENCE } from '../voice.js';
import { registerGuildCommands } from './register.js';
import { handleWhisper, handleWhisperAutocomplete } from './commands/whisper.js';
import { handleLink } from './commands/link.js';
import { handleAnswer, handleAnswerAutocomplete } from './commands/answer.js';
import { resolveAnswer } from '../oracle/resolve.js';
import { startPersistentShowrunner } from '../showrunner/persistent.js';

/** Source tag for every row this process writes to event_log. */
const SOURCE = 'the-watcher';

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.MessageContent,
  ],
});

client.once('clientReady', (c) => {
  // wear the presence — the member list reads: The Watcher — Watching the ways.
  c.user.setPresence(BOT_PRESENCE);

  console.log(`[the-watcher] watching as ${c.user.tag}`);
  void logEvent('info', SOURCE, `the watcher is watching as ${c.user.tag}`);

  // ensure the ways are open: (re)register the guild commands on every boot.
  void registerGuildCommands().catch((err) => {
    const message = err instanceof Error ? err.message : String(err);
    console.error('[the-watcher] command registration failed:', err);
    void logEvent('error', SOURCE, `command registration failed: ${message}`);
  });

  // Production contract check: fail visibly in logs if the configured guild/channel is wrong or
  // the bot cannot read and answer there. This never posts into player-facing Discord.
  void auditDiscordSurface(c).catch((err) => {
    const message = err instanceof Error ? err.message : String(err);
    console.error('[the-watcher] discord surface audit failed:', message);
    void logEvent('error', SOURCE, `discord surface audit failed: ${message}`);
  });

  // The live showrunner belongs to the persistent worker. It pulses every 10-15 seconds, skips
  // local overlap, and still takes the SQL lease shared with the recovery cron before every mutation.
  startPersistentShowrunner({
    onError: (err) => {
      const message = err instanceof Error ? err.message : String(err);
      console.error('[the-watcher] persistent showrunner tick failed:', err);
      void logEvent('error', SOURCE, `persistent showrunner tick failed: ${message}`);
    },
  });
});

client.on('interactionCreate', async (interaction: Interaction) => {
  if (interaction.isAutocomplete()) {
    try {
      if (interaction.commandName === 'whisper') await handleWhisperAutocomplete(interaction);
      if (interaction.commandName === 'answer') await handleAnswerAutocomplete(interaction);
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err);
      console.error(`[the-watcher] ${interaction.commandName} autocomplete stumbled:`, err);
      void logEvent('warn', SOURCE, `${interaction.commandName} autocomplete failed: ${message}`);
      try { await interaction.respond([]); } catch { /* token may have lapsed */ }
    }
    return;
  }

  if (!interaction.isChatInputCommand()) return;

  try {
    switch (interaction.commandName) {
      case 'whisper':
        await handleWhisper(interaction);
        break;
      case 'link':
        await handleLink(interaction);
        break;
      case 'answer':
        await handleAnswer(interaction);
        break;
      default:
        // an unknown rite — the watcher simply goes quiet.
        await replyQuiet(interaction);
    }
  } catch (err) {
    const message = err instanceof Error ? err.message : String(err);
    console.error(`[the-watcher] ${interaction.commandName} stumbled:`, err);
    void logEvent('error', SOURCE, `${interaction.commandName} failed: ${message}`);
    await replyQuiet(interaction);
  }
});

/**
 * THE #the-record SCAN — the passive answer surface.
 *
 * Every message in the watched channel is treated as a POSSIBLE answer and run
 * through the SAME resolver the /answer command uses (no logic duplication). The
 * watcher replies ONLY when the message resolves to something to say — a solve,
 * or a withholding. On ordinary chat (a true miss / empty / replay) it stays
 * SILENT: it never spam-replies, never errors, never tells a player they were
 * close. Discipline (ORACLE.md §6):
 *   - ignore the bot's own messages and any other bot.
 *   - only react in config.channels.theRecord.
 *   - only speak when the resolver says 'solved' or 'withheld'.
 *
 * Fault-isolated: the whole body is in try/catch and logs to the record on a
 * stumble — a scan failure never crashes the process and never speaks an error.
 */
client.on('messageCreate', async (message: Message) => {
  // ignore self, other bots, webhooks, system messages, and anything outside
  // #the-record. The bot is itself a bot, so author.bot also breaks any reply
  // loop (the watcher's own answer can never re-trigger the scan).
  if (message.author.bot) return;
  if (message.webhookId) return; // webhook posts aren't keepers answering
  if (message.system) return; // join/pin/system notices are never answers
  if (message.channelId !== config.channels.theRecord) return;

  const raw = message.content;
  if (!raw || raw.trim() === '') return;
  // defensive length bound before any work (the resolver caps too, but don't even
  // hand a multi-kB paste to the resolver from the always-on scan).
  if (raw.length > 4000) return;

  try {
    // resolve the speaker -> a bound keeper (may be null; the resolver still
    // rate-limits an unlinked author by discord_id and stays silent on a hit).
    const player = await getPlayerByDiscordId(message.author.id);

    const result = await resolveAnswer(
      { player, discordId: message.author.id },
      raw,
      'discord',
    );

    switch (result.kind) {
      case 'solved':
        // a real solve, in the open — the watcher answers in the channel.
        await message.reply({ content: result.reply });
        return;
      case 'withheld':
        // rate-limited / capped — withhold in voice (still a real answerer who
        // hit a real puzzle; staying mute here would feel like a bug to them).
        await message.reply({ content: result.reply });
        return;
      case 'silent':
        // ordinary chat, a true miss, an empty line, or a replay → say NOTHING.
        return;
    }
  } catch (err) {
    const messageText = err instanceof Error ? err.message : String(err);
    console.error('[the-watcher] #the-record scan stumbled:', err);
    void logEvent('error', SOURCE, `the-record scan failed: ${messageText}`);
    // a stumble is silence too — never speak an error into the channel.
  }
});

async function auditDiscordSurface(readyClient: Client<true>): Promise<void> {
  const guild = await readyClient.guilds.fetch(config.discord.guildId);
  const channel = await guild.channels.fetch(config.channels.theRecord);
  if (!channel || !channel.isTextBased()) {
    throw new Error(`CHANNEL_THE_RECORD ${config.channels.theRecord} is missing or is not text-based`);
  }
  const me = guild.members.me ?? await guild.members.fetchMe();
  const permissions = channel.permissionsFor(me);
  const required = [
    [PermissionFlagsBits.ViewChannel, 'ViewChannel'],
    [PermissionFlagsBits.SendMessages, 'SendMessages'],
    [PermissionFlagsBits.ReadMessageHistory, 'ReadMessageHistory'],
    [PermissionFlagsBits.AttachFiles, 'AttachFiles'],
  ] as const;
  const missing = required.filter(([permission]) => !permissions?.has(permission)).map(([, name]) => name);
  if (missing.length > 0) throw new Error(`#the-record is missing bot permissions: ${missing.join(', ')}`);
  if (permissions?.has(PermissionFlagsBits.Administrator)) {
    console.warn('[the-watcher] permission warning: Administrator is unnecessary; scope the bot to its ARG channels');
    void logEvent('warn', SOURCE, 'bot has unnecessary Administrator permission');
  }
  console.log(`[the-watcher] discord surface ready: ${guild.name} / #${'name' in channel ? channel.name : config.channels.theRecord}`);
}

client.on('error', (err) => {
  console.error('[the-watcher] client error:', err);
  void logEvent('error', SOURCE, `client error: ${err.message}`);
});

/**
 * PROCESS-LEVEL BACKSTOP (2026-07-05 audit). Every handler above is already individually
 * try/catch-wrapped (the fault-isolation design intent), so these should never fire in normal
 * operation — but without them, ANY future `void somePromise()` call site whose promise rejects
 * outside its own try/catch, or a third-party library callback that throws synchronously, crashes
 * the whole Node process with no log line (Node's default behavior), taking down every surface
 * (whisper, link, answer, the-record scan) at once with zero record of why.
 *
 * unhandledRejection is logged but does NOT exit — a stray rejection is a bug to fix, not
 * necessarily evidence of a corrupted process, and the bot staying up matters more here than
 * crash-purity. uncaughtException DOES exit (Node's own guidance: continuing after a truly
 * uncaught synchronous exception risks an undefined process state) — logged first on a
 * best-effort basis, with a hard timeout so a stalled log write can never turn a crash into a
 * silent hang. A process supervisor (Railway/Render) restarts the process cleanly on exit.
 */
process.on('unhandledRejection', (reason) => {
  const message = reason instanceof Error ? reason.message : String(reason);
  console.error('[the-watcher] unhandled rejection (bot stays up):', reason);
  void logEvent('error', SOURCE, `unhandled rejection: ${message}`);
});

process.on('uncaughtException', (err) => {
  console.error('[the-watcher] uncaught exception — exiting for a clean restart:', err);
  void logEvent('error', SOURCE, `uncaught exception: ${err.message}`).finally(() => process.exit(1));
  setTimeout(() => process.exit(1), 3000).unref();
});

/**
 * Answer a stumble in-character. Whether or not the interaction was already
 * deferred or replied to, the player only ever hears voice.quiet().
 */
async function replyQuiet(interaction: ChatInputCommandInteraction): Promise<void> {
  try {
    if (interaction.deferred || interaction.replied) {
      await interaction.editReply(voice.quiet());
    } else {
      await interaction.reply({ content: voice.quiet(), flags: MessageFlags.Ephemeral });
    }
  } catch {
    // the interaction token may have lapsed; nothing more can be said.
  }
}

void logEvent('info', SOURCE, 'the watcher wakes');
void client.login(config.discord.botToken);
