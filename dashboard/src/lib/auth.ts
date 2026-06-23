import "server-only";

import type { User } from "@supabase/supabase-js";
import { createClient } from "@/lib/supabase/server";

/**
 * Resolve the currently signed-in Supabase user from request cookies.
 * Returns null when there is no valid session.
 *
 * Uses `getUser()` (not `getSession()`) so the token is validated against the
 * Supabase auth server rather than trusted from the cookie alone.
 */
export async function getUser(): Promise<User | null> {
  const supabase = await createClient();
  const {
    data: { user },
  } = await supabase.auth.getUser();
  return user ?? null;
}

/**
 * Parse the ADMIN_EMAILS allowlist (comma-separated) into a lowercased set.
 */
function adminEmailSet(): Set<string> {
  return new Set(
    (process.env.ADMIN_EMAILS ?? "")
      .split(",")
      .map((email) => email.trim().toLowerCase())
      .filter(Boolean),
  );
}

/**
 * Whether the current session belongs to an admin (Author-mode gate).
 *
 * Pass a `user` to avoid a second round-trip when you already have one;
 * otherwise this resolves the session itself. Author-mode pages and every
 * privileged server action MUST check this before touching the admin client.
 */
export async function isAdmin(user?: User | null): Promise<boolean> {
  const resolved = user ?? (await getUser());
  const email = resolved?.email?.toLowerCase();
  if (!email) return false;
  return adminEmailSet().has(email);
}
