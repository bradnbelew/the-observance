import { createServerClient } from "@supabase/ssr";
import type { CookieOptions } from "@supabase/ssr";
import { cookies } from "next/headers";
import type { Database } from "@/lib/database.types";

/**
 * Server-side Supabase client using the anon key. Public/status surfaces read
 * only RLS-approved views through this client.
 *
 * Use in Server Components, Route Handlers, and Server Actions for public reads.
 * For privileged Author-mode reads/writes that must bypass RLS, use the
 * service-role client in `./admin`.
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
        setAll(cookiesToSet: { name: string; value: string; options: CookieOptions }[]) {
          try {
            cookiesToSet.forEach(({ name, value, options }) =>
              cookieStore.set(name, value, options),
            );
          } catch {
            // `setAll` was called from a Server Component. Public dashboard reads
            // do not rely on cookie mutation.
          }
        },
      },
    },
  );
}
