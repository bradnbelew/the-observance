import { existsSync, readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const repo = resolve(here, '../../..');
const read = (path: string) => readFileSync(resolve(repo, path), 'utf8');
const checks: Array<[string, string, string]> = [
  ['bot persistent tick', 'discord/src/bot/index.ts', 'startPersistentShowrunner'],
  ['answer autocomplete', 'discord/src/bot/index.ts', 'handleAnswerAutocomplete'],
  ['answer selector semantics', 'discord/src/oracle/resolve.ts', 'scopeOpenPuzzles'],
  ['answer evidence receipt', 'discord/src/oracle/resolve.ts', 'recordOracleEvidence'],
  ['C01 identity command', 'discord/src/bot/commands/link.ts', "getString('code', true)"],
  ['C01 durable identity receipt', 'discord/src/db/repo.ts', 'claimIdentityHandoff'],
  ['deterministic archived observer', 'discord/src/showrunner/observer.run.ts', 'const o = decision.observation'],
  ['import-safe showrunner', 'discord/src/showrunner/run.ts', 'isDirectRun'],
  ['V5 showrunner safe mode', 'discord/src/showrunner/run.ts', 'runV5SafeHeartbeat'],
  ['V5 safe-mode detector', 'discord/src/showrunner/state.ts', 'isV5CampaignActive'],
  ['production V5 mode', 'render.yaml', 'OBSERVANCE_CAMPAIGN_VERSION'],
  ['worker cadence env', 'render.yaml', 'SHOWRUNNER_TICK_MS'],
  ['recovery cron cadence', 'render.yaml', '*/5 * * * *'],
  ['legacy puzzle retirement', 'discord/supabase/seeds/v5_investigations.sql', 'update public.puzzles set active = false'],
  ['legacy hint retirement', 'discord/supabase/seeds/v5_investigations.sql', 'update public.hints set active = false'],
  ['legacy archive retirement', 'discord/supabase/seeds/v5_investigations.sql', 'update public.thread_cards set active = false'],
  ['legacy optional row retirement', 'discord/supabase/seeds/v5_investigations.sql', 'update public.side_quests set active = false'],
  ['one-time V4 beat retirement', 'discord/supabase/seeds/v5_investigations.sql', 'v5_queue_retirement_complete'],
  ['phase cursor', 'discord/supabase/migrations/0013_v5_investigations.sql', 'phase_key'],
  ['receipt prerequisite guard', 'discord/supabase/migrations/0013_v5_investigations.sql', 'prerequisites not satisfied'],
  ['V5 resource-pack identity', 'discord/src/render/build-runepack.ts', 'The Observance V5 — field archive, Keeper testimony, and Deep Hold systems. Target: 1.21.11 (format [75,0]).'],
];
for (const [label, path, needle] of checks) {
  if (!read(path).includes(needle)) throw new Error(`V5 surface selftest FAILED: ${label} (${path})`);
}

const register = read('discord/src/bot/register.ts');
if (!register.includes(".setName('answer')") || !register.includes('.setAutocomplete(true)')) throw new Error('V5 surface selftest FAILED: /answer command is not autocomplete-scoped');
if (!register.includes(".setName('link')") || !register.includes(".setName('callback')")
  || !register.includes(".setName('code')")) throw new Error('V5 surface selftest FAILED: /link name callback code command is absent');
if (register.includes(".setName('callbackCommand')")) throw new Error('V5 surface selftest FAILED: retired standalone /callback registration remains');

const runner = read('discord/src/showrunner/run.ts');
const safeGate = runner.indexOf('if (await isV5CampaignActive())');
const legacyTick = runner.indexOf('await runTick(');
if (safeGate < 0 || legacyTick < 0 || safeGate > legacyTick) throw new Error('V5 surface selftest FAILED: legacy showrunner can run before the V5 safe-mode gate');

const bot = read('discord/src/bot/index.ts');
for (const retiredRuntime of ['GuildVoiceStates', 'startVoiceCapture', 'ensurePrologueIgnited', 'maybeCloseCoopGate', 'insertObservation', 'observerOptedOut']) {
  if (bot.includes(retiredRuntime)) throw new Error(`V5 surface selftest FAILED: bot still loads retired ambient runtime: ${retiredRuntime}`);
}
for (const retiredFile of ['discord/src/voice/receiver.ts', 'discord/src/voice/transcribe.ts', 'discord/src/voice/spoken-name.ts', 'discord/src/showrunner/observer.llm.ts']) {
  if (existsSync(resolve(repo, retiredFile))) throw new Error(`V5 surface selftest FAILED: retired external-service source remains: ${retiredFile}`);
}
const packageJson = read('discord/package.json');
for (const retiredDependency of ['@anthropic-ai/sdk', '@discordjs/voice', 'opusscript', 'prism-media', 'tweetnacl']) {
  if (packageJson.includes(`"${retiredDependency}"`)) throw new Error(`V5 surface selftest FAILED: retired dependency remains: ${retiredDependency}`);
}

const render = read('render.yaml');
if (!/OBSERVANCE_CAMPAIGN_VERSION\s*\r?\n\s*value:\s*v5/.test(render)) throw new Error('V5 surface selftest FAILED: Render does not explicitly force V5 safe mode');
const exampleEnv = read('discord/.env.example');
for (const retiredEnv of ['ANTHROPIC_API_KEY', 'DISCORD_VOICE_CHANNEL_ID', 'WHISPER_API_URL', 'WHISPER_API_KEY', 'WHISPER_MODEL', 'WHISPER_LANG', 'WHISPER_BIN']) {
  if (render.includes(retiredEnv) || exampleEnv.includes(retiredEnv)) throw new Error(`V5 surface selftest FAILED: retired production environment key remains: ${retiredEnv}`);
}
const expectedDiscordEnv = [
  'CHANNEL_THE_RECORD', 'DISCORD_APP_ID', 'DISCORD_BOT_TOKEN', 'DISCORD_GUILD_ID',
  'OBSERVANCE_CAMPAIGN_VERSION', 'SHOWRUNNER_LEASE_SECONDS', 'SHOWRUNNER_TICK_MS',
  'SUPABASE_SERVICE_ROLE_KEY', 'SUPABASE_URL',
].sort();
const exampleKeys = [...exampleEnv.matchAll(/^([A-Z][A-Z0-9_]+)=/gm)].map((match) => match[1]!).sort();
if (JSON.stringify(exampleKeys) !== JSON.stringify(expectedDiscordEnv)) throw new Error(`V5 surface selftest FAILED: Discord .env.example drift: ${exampleKeys.join(',')}`);
const expectedRenderEnv = [...expectedDiscordEnv, 'NODE_VERSION'].sort();
const renderKeys = [...render.matchAll(/^\s+- key:\s*([A-Z][A-Z0-9_]+)\s*$/gm)].map((match) => match[1]!).sort();
if (JSON.stringify(renderKeys) !== JSON.stringify(expectedRenderEnv)) throw new Error(`V5 surface selftest FAILED: Render environment drift: ${renderKeys.join(',')}`);
const discordReadme = read('discord/README.md');
for (const key of expectedDiscordEnv) if (!discordReadme.includes(key)) throw new Error(`V5 surface selftest FAILED: Discord README omits ${key}`);

console.log(`V5 surface selftest OK (${checks.length + 8} runtime/database contracts)`);
