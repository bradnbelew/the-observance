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
  });
}

/** Validated, frozen config. Importing this triggers validation (throws if invalid). */
export const config: Config = loadConfig();
