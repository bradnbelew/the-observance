/**
 * Service-role Supabase client (server-side only).
 *
 * Uses SUPABASE_SERVICE_ROLE_KEY, which BYPASSES Row Level Security. This is
 * safe here because the Discord bot is a trusted server process. NEVER ship this
 * key — or this client — to a browser.
 *
 * Session persistence and token auto-refresh are disabled: the service role is a
 * static key, not a user session.
 */
import { createClient, type SupabaseClient } from '@supabase/supabase-js';
import { config } from '../config.js';

export const supabase: SupabaseClient = createClient(
  config.supabase.url,
  config.supabase.serviceRoleKey,
  {
    auth: {
      persistSession: false,
      autoRefreshToken: false,
      detectSessionInUrl: false,
    },
    global: {
      headers: { 'x-application': 'the-observance-discord' },
    },
  },
);
