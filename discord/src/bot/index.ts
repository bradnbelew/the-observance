/**
 * `npm run dev` — THE WATCHER comes online.
 *
 * A discord.js v14 client that:
 *   - wears the presence: Watching "the ways" (BOT_PRESENCE from voice.ts).
 *   - ensures the guild's slash commands (/whisper, /link) are registered.
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
  type Interaction,
  type ChatInputCommandInteraction,
  type Message,
} from 'discord.js';
import { config } from '../config.js';
import { ensurePrologueIgnited, getPlayerByDiscordId, logEvent } from '../db/repo.js';
import { voice, BOT_PRESENCE } from '../voice.js';
import { registerGuildCommands } from './register.js';
import { handleWhisper } from './commands/whisper.js';
import { handleLink } from './commands/link.js';
import { handleAnswer } from './commands/answer.js';
import { resolveAnswer } from '../oracle/resolve.js';

/** Source tag for every row this process writes to event_log. */
const SOURCE = 'the-watcher';

/**
 * In-process latch for the cold-start ignition (B4 / OVERHAUL §3). Once a keeper has
 * been seen in #the-record, `prologue_ignited` is set in arc_state and this flips true
 * so we never re-read the flag on every subsequent message. Re-derives from the DB on a
 * cold boot (the first keeper post simply re-confirms the already-set flag, a no-op).
 */
let prologueIgnited = false;

const client = new Client({
  intents: [
    GatewayIntentBits.Guilds,
    GatewayIntentBits.GuildMessages,
    GatewayIntentBits.MessageContent,
  ],
});

client.once('ready', (c) => {
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
});

client.on('interactionCreate', async (interaction: Interaction) => {
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

    // IGNITION (B4 / OVERHAUL §3): the first time a KEEPER is seen acting in #the-record,
    // fire the cold-start so the next autonomy tick speaks the frame-break ack. Latched
    // in-process after it fires (and gated on a bound keeper) so it costs nothing on the
    // steady-state scan. Fault-isolated: an ignition stumble never blocks the answer path.
    if (player && !prologueIgnited) {
      try {
        const fired = await ensurePrologueIgnited(Date.now());
        prologueIgnited = true; // either we set it, or it was already set — stop checking.
        if (fired) {
          void logEvent('info', SOURCE, `prologue ignited by ${player.name} in #the-record`);
        }
      } catch (err) {
        // leave the latch unset so a later message retries; never speak an error.
        const m = err instanceof Error ? err.message : String(err);
        void logEvent('warn', SOURCE, `prologue ignition stumbled: ${m}`);
      }
    }

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

client.on('error', (err) => {
  console.error('[the-watcher] client error:', err);
  void logEvent('error', SOURCE, `client error: ${err.message}`);
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
