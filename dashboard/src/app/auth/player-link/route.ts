import { createServerClient } from '@supabase/ssr';
import type { CookieOptions } from '@supabase/ssr';
import { cookies } from 'next/headers';
import { NextResponse } from 'next/server';
import type { Database } from '@/lib/database.types';
import { MORROW_CASE_ROUTE } from '@/lib/morrow-copperline-case';

type PendingCookie = { name: string; value: string; options: CookieOptions };

function accountRedirect(origin: string, state: 'sent' | 'input' | 'desk') {
  const query = state === 'sent' ? 'sent=1' : `error=${state}`;
  return NextResponse.redirect(new URL(`/support/account?${query}`, origin), 303);
}

export async function POST(request: Request) {
  const requestUrl = new URL(request.url);
  const requestOrigin = request.headers.get('origin');
  if (!requestOrigin || requestOrigin !== requestUrl.origin) {
    return accountRedirect(requestUrl.origin, 'desk');
  }

  const formData = await request.formData();
  const email = String(formData.get('email') ?? '').trim().toLowerCase();
  if (email.length < 3 || email.length > 254 || !email.includes('@')) {
    return accountRedirect(requestUrl.origin, 'input');
  }

  const cookieStore = await cookies();
  const pendingCookies: PendingCookie[] = [];
  const supabase = createServerClient<Database>(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!,
    {
      cookies: {
        getAll: () => cookieStore.getAll(),
        setAll: (cookiesToSet: PendingCookie[]) => pendingCookies.push(...cookiesToSet),
      },
    },
  );

  try {
    const { error } = await supabase.auth.signInWithOtp({
      email,
      options: {
        emailRedirectTo: `${requestUrl.origin}/auth/callback?next=${encodeURIComponent(MORROW_CASE_ROUTE)}`,
        shouldCreateUser: false,
      },
    });
    if (error && (error.status ?? 500) >= 500) {
      return accountRedirect(requestUrl.origin, 'desk');
    }

    const response = accountRedirect(requestUrl.origin, 'sent');
    if (!error) {
      pendingCookies.forEach(({ name, value, options }) => response.cookies.set(name, value, options));
    }
    return response;
  } catch {
    return accountRedirect(requestUrl.origin, 'desk');
  }
}
