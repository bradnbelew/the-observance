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
  .setDescription('request an authored hint for an open investigation.')
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
  .setDescription('submit a short exact answer when an investigation calls for one.')
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
  .setDescription('show open investigation work and its real submission surface.');

/** Shared investigation state and player-caused settlement dispatch. */
export const investigateCommand = new SlashCommandBuilder()
  .setName('investigate')
  .setDescription('review shared changes or send an authored investigation action.')
  .addSubcommand((subcommand) => subcommand
    .setName('status')
    .setDescription('show the campaign changes already caused by the group.'))
  .addSubcommand((subcommand) => subcommand
    .setName('dispatch')
    .setDescription('ask the settlement to keep two conflicting accounts open.')
    .addStringOption((option) => option
      .setName('summary')
      .setDescription('one plain sentence naming the disagreement; no hidden phrase is required.')
      .setRequired(true)
      .setMinLength(12)
      .setMaxLength(180)))
  .addSubcommand((subcommand) => subcommand
    .setName('test-copy')
    .setDescription('run one custody test against the two Mouth copies.')
    .addStringOption((option) => option
      .setName('method')
      .setDescription('choose the record that should order damaged copies.')
      .setRequired(true)
      .addChoices(
        { name: 'cartridge barcode order checked against the recovery-node clock', value: 'barcode-and-node-clock' },
        { name: 'filenames from the damaged guest', value: 'guest-filenames' },
        { name: 'modified times from the damaged guest', value: 'guest-modified-times' },
      )))
  .addSubcommand((subcommand) => subcommand
    .setName('review-nessa')
    .setDescription('open the three-part public correction form.'))
  .addSubcommand((subcommand) => subcommand
    .setName('file-nessa')
    .setDescription('keyboard fallback for the same three-part public correction.')
    .addStringOption((option) => option.setName('cause').setDescription('material cause and first failure place').setRequired(true).setMaxLength(120))
    .addStringOption((option) => option.setName('record').setDescription('what was changed in the chronology').setRequired(true).setMaxLength(120))
    .addStringOption((option) => option.setName('conduct').setDescription('what Nessa did and when').setRequired(true).setMaxLength(120)))
  .addSubcommand((subcommand) => subcommand
    .setName('confront-wren')
    .setDescription('commit the transmission finding before the remembrance choice.')
    .addStringOption((option) => option.setName('sender').setDescription('who transmitted the private packets').setRequired(true).addChoices(
      { name: 'Wren, through knowledge shared inside the company', value: 'wren' },
      { name: 'Rook, through the complete physical plan', value: 'rook' },
      { name: 'the Record, by direct passive observation alone', value: 'record-alone' },
    ))
    .addStringOption((option) => option.setName('payload').setDescription('what the packet progression carried').setRequired(true).addChoices(
      { name: 'names, operating plans, changed routes, and private fears', value: 'names-plans-routes-fears' },
      { name: 'one route distance repeated by accident', value: 'one-distance' },
      { name: 'only public Copperline posts and checksums', value: 'public-posts' },
    ))
    .addStringOption((option) => option.setName('proof').setDescription('what makes the transmission deliberate').setRequired(true).addChoices(
      { name: 'progressive packets include the private revision but omit Rook’s physical counter-mark', value: 'progressive-private-missing-countermark' },
      { name: 'Wren was nervous and sometimes changed distances', value: 'nervous-distance' },
      { name: 'the camp stopped posting after the incident', value: 'posting-stopped' },
    ))
    .addStringOption((option) => option.setName('motive').setDescription('what motive changes and does not change').setRequired(true).addChoices(
      { name: 'fear of erasure explains his choice; it does not remove responsibility', value: 'fear-explains-choice-responsibility-remains' },
      { name: 'fear proves total compulsion and removes responsibility', value: 'fear-erases-responsibility' },
      { name: 'motive is irrelevant and must be deleted from the record', value: 'delete-motive' },
    )))
  .addSubcommand((subcommand) => subcommand
    .setName('identify-averyn')
    .setDescription('restore the yielded name and keep four related terms distinct.')
    .addStringOption((option) => option.setName('name').setDescription('the six-letter artifact yielded by the six affidavits').setRequired(true).setMinLength(6).setMaxLength(12))
    .addStringOption((option) => option.setName('averyn').setDescription('Averyn’s evidenced role').setRequired(true).addChoices(
      { name: 'human registrar and cistern analyst who entered the archive', value: 'human-registrar-analyst' },
      { name: 'a themed seventh Keeper', value: 'seventh-keeper' },
      { name: 'another name for the Dark', value: 'dark-alias' },
    ))
    .addStringOption((option) => option.setName('record').setDescription('the Record’s evidenced role').setRequired(true).addChoices(
      { name: 'a civic monitoring and memory system that trapped her', value: 'civic-system-trapped-her' },
      { name: 'Averyn’s free and private diary', value: 'averyn-private-diary' },
      { name: 'the same entity as the Dark', value: 'record-is-dark' },
    ))
    .addStringOption((option) => option.setName('watcher').setDescription('the Watcher’s evidenced role').setRequired(true).addChoices(
      { name: 'defensive Record speech through her constrained consciousness', value: 'constrained-record-voice' },
      { name: 'Averyn speaking freely without system limits', value: 'averyn-free-voice' },
      { name: 'one of the six Keepers speaking anonymously', value: 'keeper-anonymous' },
    ))
    .addStringOption((option) => option.setName('dark').setDescription('the honest boundary on the Dark').setRequired(true).addChoices(
      { name: 'related pressure or cause; distinct and still unknown', value: 'related-distinct-unknown' },
      { name: 'fully identified as Averyn', value: 'dark-is-averyn' },
      { name: 'fully identified as the civic Record', value: 'dark-is-record' },
    )));

/** Every rite, in registration order. */
export const commands = [whisperCommand, linkCommand, answerCommand, progressCommand, investigateCommand] as const;

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
