import "server-only";

import { createClient as createSupabaseClient } from "@supabase/supabase-js";
import type { Database } from "@/lib/database.types";

/**
 * Service-role Supabase client (SERVER ONLY).
 *
 * Bypasses RLS — use it for privileged dashboard writes the plugin schema
 * locks behind RLS: approving/skipping beats in `beat_queue`, editing
 * `whisper_budgets`, toggling watcher-sleep in `settings`, advancing
 * `arc_state.current_act`, etc.
 *
 * NEVER import this into a Client Component or expose the key to the browser.
 * Author routes and every privileged server action must authenticate the
 * operator before constructing this client.
 * The /author route and its server-action POSTs are also guarded by proxy.ts.
 * Hosting/network access controls remain useful as a second layer.
 *
 * No session persistence / auto-refresh: this is a stateless privileged client.
 */
export function createAdminClient() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;

  if (!url || !serviceRoleKey) {
    throw new Error(
      "Missing NEXT_PUBLIC_SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY for admin client.",
    );
  }

  return createSupabaseClient<Database>(url, serviceRoleKey, {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
    },
  });
}
