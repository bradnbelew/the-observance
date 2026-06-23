import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";
import type { Database } from "@/lib/database.types";

/**
 * Server-side Supabase client bound to the request's auth cookies (anon key,
 * RLS-enforced, runs as the signed-in user).
 *
 * Use in Server Components, Route Handlers, and Server Actions for reads and
 * for resolving the current session. For privileged writes that must bypass
 * RLS, use the service-role client in `./admin`.
 *
 * Note: `cookies()` is async in Next.js 15, so this helper is async.
 */
export async function createClient() {
  const cookieStore = await cookies();

  return createServerClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      cookies: {
        getAll() {
          return cookieStore.getAll();
        },
        setAll(cookiesToSet) {
          try {
            cookiesToSet.forEach(({ name, value, options }) =>
              cookieStore.set(name, value, options),
            );
          } catch {
            // `setAll` was called from a Server Component. This can be ignored
            // when middleware is refreshing sessions (see src/middleware.ts).
          }
        },
      },
    },
  );
}
