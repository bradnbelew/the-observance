/**
 * `npm run register` — lay the ways open: deploy the watcher's slash commands to
 * the guild. Also exported as registerGuildCommands() so the bot ensures the
 * commands on every boot (guild-scoped registration is instant; global is slow).
 *
 * Two rites only:
 *   /whisper <puzzle>  — ask the watcher for a hint, and pay the toll.
 *   /link    <name>    — bind your discord voice to a name worn in the world.
 *
 * Command names + option names are machine identifiers (discord requires
 * lowercase a-z); the descriptions are the only player-visible text here and are
 * written in the watcher's register.
 */
import { pathToFileURL } from 'node:url';
import { REST, Routes, SlashCommandBuilder } from 'discord.js';
import { config } from '../config.js';

/** /whisper <puzzle> — no tier option; the tier rises on its own with each ask. */
export const whisperCommand = new SlashCommandBuilder()
  .setName('whisper')
  .setDescription('ask the watcher. it will keep something of yours while you think.')
  .addStringOption((opt) =>
    opt
      .setName('puzzle')
      .setDescription('the mark you are working — the puzzle you ask after.')
      .setRequired(true),
  );

/** /link <name> — bind your discord voice to your name in the world. */
export const linkCommand = new SlashCommandBuilder()
  .setName('link')
  .setDescription('tell the watcher the name you wear in the world.')
  .addStringOption((opt) =>
    opt
      .setName('name')
      .setDescription('the name you wear in the world.')
      .setRequired(true),
  );

/** Every rite, in registration order. */
export const commands = [whisperCommand, linkCommand] as const;

/** JSON payloads for the REST registration call. */
export const commandsJSON = commands.map((c) => c.toJSON());

/**
 * (Re)register the two rites to DISCORD_GUILD_ID. Guild-scoped = instant. Safe to
 * call on every boot — `put` overwrites the full set idempotently.
 */
export async function registerGuildCommands(): Promise<void> {
  const rest = new REST({ version: '10' }).setToken(config.discord.botToken);
  await rest.put(
    Routes.applicationGuildCommands(config.discord.appId, config.discord.guildId),
    { body: commandsJSON },
  );
}

/** When run directly (`npm run register`), deploy and report. */
const isDirectRun =
  process.argv[1] !== undefined &&
  import.meta.url === pathToFileURL(process.argv[1]).href;

if (isDirectRun) {
  registerGuildCommands()
    .then(() => {
      console.log(
        `[the-watcher] ${commandsJSON.length} rite(s) laid open to guild ${config.discord.guildId}`,
      );
    })
    .catch((err) => {
      console.error('[the-watcher] failed to lay the ways open:', err);
      process.exit(1);
    });
}
