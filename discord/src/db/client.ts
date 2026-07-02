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
import WebSocketImpl from 'ws';
import { config } from '../config.js';

// @supabase/realtime-js constructs a WebSocket at client-init and requires a
// global `WebSocket`, which Node only ships natively in 22+. On an older host
// (e.g. Node 20) createClient() throws at module load and the process crash-loops.
// Provide one so the bot boots on any Node version (harmless no-op on Node 22+).
if (typeof (globalThis as { WebSocket?: unknown }).WebSocket === 'undefined') {
  (globalThis as { WebSocket?: unknown }).WebSocket = WebSocketImpl as unknown;
}

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
