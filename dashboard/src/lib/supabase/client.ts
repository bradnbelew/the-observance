"use client";

import { createBrowserClient } from "@supabase/ssr";
import type { Database } from "@/lib/database.types";

/**
 * Browser-side Supabase client (anon key, RLS-enforced).
 *
 * Use inside Client Components for realtime subscriptions or reads of the
 * spoiler-free views. It can ONLY see what RLS / the anon grants allow.
 */
export function createClient() {
  return createBrowserClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
  );
}
