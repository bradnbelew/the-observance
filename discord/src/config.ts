/**
 * Environment loading + validation for the Discord service.
 *
 * Loads `.env` via dotenv, validates that every required variable is present,
 * and exports a single typed, frozen `config` object. If anything is missing it
 * throws ONE clear error listing every absent key (fail fast at startup).
 */
import 'dotenv/config';

/** Channel-ID keys the bot posts to. */
export interface ChannelConfig {
  readonly theRecord: string;
}

/** Fully-validated, immutable runtime configuration. */
export interface Config {
  readonly discord: {
    readonly botToken: string;
    readonly appId: string;
    readonly guildId: string;
  };
  readonly channels: ChannelConfig;
  readonly supabase: {
    readonly url: string;
    readonly serviceRoleKey: string;
  };
  readonly morrow: {
    readonly enabled: boolean;
    readonly releaseId: string | null;
    readonly channelId: string | null;
    readonly threadId: string | null;
  };
}

/** Every required env var, mapped to where it lands in `Config`. */
const REQUIRED = [
  'DISCORD_BOT_TOKEN',
  'DISCORD_APP_ID',
  'DISCORD_GUILD_ID',
  'CHANNEL_THE_RECORD',
  'SUPABASE_URL',
  'SUPABASE_SERVICE_ROLE_KEY',
] as const;

type RequiredKey = (typeof REQUIRED)[number];

function loadConfig(): Config {
  const missing: string[] = [];
  const env = {} as Record<RequiredKey, string>;

  for (const key of REQUIRED) {
    const value = process.env[key];
    if (value === undefined || value.trim() === '') {
      missing.push(key);
    } else {
      env[key] = value.trim();
    }
  }

  if (missing.length > 0) {
    throw new Error(
      `[config] Missing required environment variable(s): ${missing.join(
        ', ',
      )}. Copy .env.example to .env and fill them in.`,
    );
  }

  const morrowEnabled = process.env.MORROW_DISCORD_ENABLED?.trim().toLowerCase() === 'true';
  const morrowReleaseId = process.env.MORROW_RELEASE_ID?.trim() || null;
  const morrowChannelId = process.env.MORROW_DISCORD_CHANNEL_ID?.trim() || null;
  const morrowThreadId = process.env.MORROW_DISCORD_THREAD_ID?.trim() || null;
  if (morrowEnabled) {
    const invalid: string[] = [];
    if (!morrowReleaseId || !/^[a-z0-9][a-z0-9._-]{6,79}$/.test(morrowReleaseId)) invalid.push('MORROW_RELEASE_ID');
    if (!morrowChannelId || !/^[0-9]{15,22}$/.test(morrowChannelId)) invalid.push('MORROW_DISCORD_CHANNEL_ID');
    if (morrowThreadId !== null && !/^[0-9]{15,22}$/.test(morrowThreadId)) invalid.push('MORROW_DISCORD_THREAD_ID');
    if (invalid.length > 0) {
      throw new Error(`[config] Morrow Discord is enabled but invalid: ${invalid.join(', ')}.`);
    }
  }

  return Object.freeze({
    discord: Object.freeze({
      botToken: env.DISCORD_BOT_TOKEN,
      appId: env.DISCORD_APP_ID,
      guildId: env.DISCORD_GUILD_ID,
    }),
    channels: Object.freeze({
      theRecord: env.CHANNEL_THE_RECORD,
    }),
    supabase: Object.freeze({
      url: env.SUPABASE_URL,
      serviceRoleKey: env.SUPABASE_SERVICE_ROLE_KEY,
    }),
    morrow: Object.freeze({
      enabled: morrowEnabled,
      releaseId: morrowReleaseId,
      channelId: morrowChannelId,
      threadId: morrowThreadId,
    }),
  });
}

/** Validated, frozen config. Importing this triggers validation (throws if invalid). */
export const config: Config = loadConfig();
