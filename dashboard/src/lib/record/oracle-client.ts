import "server-only";

import { createClient as createSupabaseClient, type SupabaseClient } from "@supabase/supabase-js";

/**
 * oracle-client.ts — the SERVER-ONLY service-role client for the oracle tables.
 *
 * WHY A SEPARATE, UNTYPED CLIENT. The oracle web (`puzzles`, `solves`, `answer_attempts`, `threads`,
 * `hints`) lives in the discord/plugin migration lane (0004–0006), which the dashboard's generated
 * `Database` type does NOT declare. Rather than fake those types into the dashboard contract, this
 * client is deliberately untyped: the record libs declare the exact row shapes they read/write and
 * cast at the boundary. The existing `createAdminClient` stays the typed control-surface client.
 *
 * SECURITY (the non-negotiable). This module is `server-only`: importing it from a Client Component
 * is a build error, so the service-role key can NEVER reach the browser. Every read/write of a
 * spoiler table (accepted_answers, hint bodies, arc flags) happens here, server-side; the browser
 * only ever receives the safe, projected JSON the record libs return. This is the RLS-bypass path the
 * risk register flags as HIGH if leaked — it is fenced behind `server-only` and never exported to a
 * client boundary.
 *
 * Fail-soft: if the env is missing (a fresh checkout, a preview without secrets) this returns null so
 * every caller can degrade to the sealed/empty baseline instead of throwing — the record is a found
 * artifact; a missing backend reads as "nothing recovered yet," never a stack trace.
 */
export function getOracleClient(): SupabaseClient | null {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!url || !serviceRoleKey) return null;

  return createSupabaseClient(url, serviceRoleKey, {
    auth: { autoRefreshToken: false, persistSession: false },
  });
}
