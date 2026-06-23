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
} from 'discord.js';
import { config } from '../config.js';
import { logEvent } from '../db/repo.js';
import { voice, BOT_PRESENCE } from '../voice.js';
import { registerGuildCommands } from './register.js';
import { handleWhisper } from './commands/whisper.js';
import { handleLink } from './commands/link.js';

/** Source tag for every row this process writes to event_log. */
const SOURCE = 'the-watcher';

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
