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
    .setName('clear-nessa')
    .setDescription('file three separate findings in the Nessa Vale correction.')
    .addStringOption((option) => option.setName('cause').setDescription('what physically failed').setRequired(true).addChoices(
      { name: 'genuine stock diverted; counterfeit lower-intake cloth failed', value: 'diversion-counterfeit-lower-intake' },
      { name: 'Nessa contaminated the operator sample sink', value: 'operator-contamination' },
      { name: 'the genuine supplier cloth failed as delivered', value: 'genuine-cloth-failed' },
    ))
    .addStringOption((option) => option.setName('record').setDescription('what happened to the surviving chronology').setRequired(true).addChoices(
      { name: 'relief and complaint records were edited to move later samples onto Nessa', value: 'edited-relief-and-complaints' },
      { name: 'the public chronology is complete and unedited', value: 'public-record-complete' },
      { name: 'Averyn created a later false chronology', value: 'averyn-fabricated' },
    ))
    .addStringOption((option) => option.setName('conduct').setDescription('what the evidence establishes about Nessa').setRequired(true).addChoices(
      { name: 'she followed procedure and reported before the cloth began shedding', value: 'followed-and-reported-before-shedding' },
      { name: 'she noticed the failure but reported it too late', value: 'reported-late' },
      { name: 'the evidence cannot reach a conduct finding', value: 'no-conduct-finding' },
    )))
  .addSubcommand((subcommand) => subcommand
    .setName('plan-repair')
    .setDescription('publish a bounded Break model and safe works order.')
    .addStringOption((option) => option.setName('cause-model').setDescription('which causes interact').setRequired(true).addChoices(
      { name: 'old fracture + unchanged heat load + watch gap + late routing', value: 'fracture-heat-watch-routing' },
      { name: 'Iss alone caused the Break when he cut the route', value: 'iss-alone' },
      { name: 'the copied office caused every earlier material failure', value: 'copy-caused-all' },
    ))
    .addStringOption((option) => option.setName('iss-finding').setDescription('what the evidence supports about Iss').setRequired(true).addChoices(
      { name: 'the surface proof was sound; his unreviewed route was unsafe', value: 'surface-true-route-unsafe' },
      { name: 'the surface proof and route were both false', value: 'surface-and-route-false' },
      { name: 'the surface proof made the unreviewed route safe', value: 'surface-made-route-safe' },
    ))
    .addStringOption((option) => option.setName('works-order').setDescription('the safe physical intervention order').setRequired(true).addChoices(
      { name: 'water filter, paired light, pressure bypass, then staff route', value: 'water-light-pressure-route' },
      { name: 'open the route first, then diagnose the live systems', value: 'route-first' },
      { name: 'erase the copied office, then reset every gauge', value: 'erase-copy-reset' },
    )))
  .addSubcommand((subcommand) => subcommand
    .setName('file-leak-window')
    .setDescription('file what the Ash Camp chronology proves and leaves open.')
    .addStringOption((option) => option.setName('readiness').setDescription('state of the prior company’s release work').setRequired(true).addChoices(
      { name: 'needed knowledge and components were ready', value: 'release-ready' },
      { name: 'the company was missing its final answer', value: 'missing-final-answer' },
      { name: 'the company never reached the Hold systems', value: 'never-reached-systems' },
    ))
    .addStringOption((option) => option.setName('private-object').setDescription('what crossed the private boundary').setRequired(true).addChoices(
      { name: 'Rook’s north-brace revision and the team identities', value: 'rook-revision-and-identities' },
      { name: 'Ash’s public camera joke and locker number', value: 'ash-public-joke' },
      { name: 'mkept’s published backup checksum', value: 'published-checksum' },
    ))
    .addStringOption((option) => option.setName('window').setDescription('when the crossing occurred').setRequired(true).addChoices(
      { name: 'after the private counter-mark, before any public upload', value: 'private-before-public' },
      { name: 'after the public archive post', value: 'after-public' },
      { name: 'before Rook made the revision', value: 'before-revision' },
    ))
    .addStringOption((option) => option.setName('boundary').setDescription('what P9 can honestly claim').setRequired(true).addChoices(
      { name: 'an insider transmission is proven; the sender is not yet proven', value: 'insider-unknown' },
      { name: 'Wren is already proven as sender', value: 'wren-proven' },
      { name: 'the Record invented the revision without human access', value: 'record-invented' },
    )))
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
