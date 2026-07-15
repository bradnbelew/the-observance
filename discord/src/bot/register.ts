/**
 * `npm run register` — lay the ways open: deploy the watcher's slash commands to
 * the guild. Also exported as registerGuildCommands() so the bot ensures the
 * commands on every boot (guild-scoped registration is instant; global is slow).
 *
 * Four rites:
 *   /whisper  <puzzle>        — ask the watcher for a hint, and pay the toll.
 *   /link     <name> <callback> <code> — prove the Minecraft hand and file the handoff.
 *   /answer   <text> [puzzle] — submit a solved clue; the world answers, or stays silent.
 *   /progress                 — the standing docket: what stands open, and where it is taken.
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
      .setRequired(true)
      .setAutocomplete(true),
  );

/** /link <name> <callback> <code> — prove the world identity and file C01's durable handoff. */
export const linkCommand = new SlashCommandBuilder()
  .setName('link')
  .setDescription('bind your minecraft name and file the Copperline callback.')
  .addStringOption((opt) =>
    opt
      .setName('name')
      .setDescription('the name you wear in the world.')
      .setRequired(true),
  )
  .addStringOption((opt) =>
    opt
      .setName('callback')
      .setDescription('the four-digit callback printed on the Copperline ticket.')
      .setRequired(true)
      .setMaxLength(24),
  )
  .addStringOption((opt) =>
    opt
      .setName('code')
      .setDescription('the one-time code shown by /obslink in minecraft (expires in five minutes).')
      .setRequired(true)
      .setMinLength(12)
      .setMaxLength(20),
  );

/**
 * /answer <text> [puzzle] — submit a solved clue to the oracle. <text> is the
 * plaintext you reached; [puzzle] is an optional author/debug label (the resolver
 * matches the whole open web regardless of it).
 */
export const answerCommand = new SlashCommandBuilder()
  .setName('answer')
  .setDescription('give the watcher a name you have reached. it will answer, or it will not.')
  .addStringOption((opt) =>
    opt
      .setName('text')
      .setDescription('the plaintext you reached — the answer you would give.')
      .setRequired(true),
  )
  .addStringOption((opt) =>
    opt
      .setName('puzzle')
      .setDescription('the mark you worked, if you would name it. (optional)')
      .setRequired(false)
      .setAutocomplete(true),
  );

/** /progress — the standing docket of open findings and where each is taken. No options. */
export const progressCommand = new SlashCommandBuilder()
  .setName('progress')
  .setDescription('the standing docket: what stands open, and where it is taken.');

/** Every rite, in registration order. */
export const commands = [whisperCommand, linkCommand, answerCommand, progressCommand] as const;

/** JSON payloads for the REST registration call. */
export const commandsJSON = commands.map((c) => c.toJSON());

/**
 * (Re)register the three rites to DISCORD_GUILD_ID. Guild-scoped = instant. Safe to
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
