import { NextResponse } from 'next/server';
import { createClient } from '@/lib/supabase/server';
import { MORROW_CASE_ROUTE } from '@/lib/morrow-copperline-case';

const VALIDATION_PROJECT = 'snmqaqlagwzptzqbaiws';

/**
 * Disposable browser-rehearsal session bootstrap. It is unreachable unless the local operator sets
 * every rehearsal-only environment binding to the exact validation project. Production has no
 * password form and this route never accepts credentials or a destination from the request.
 */
export async function GET(request: Request) {
  const url = new URL(request.url);
  const targetUrl = process.env.NEXT_PUBLIC_SUPABASE_URL ?? '';
  const targetRef = process.env.OBSERVANCE_MUTATION_PROJECT_REF;
  const enabled = process.env.MORROW_BROWSER_REHEARSAL_AUTH === 'enabled';
  const email = process.env.MORROW_BROWSER_REHEARSAL_EMAIL;
  const password = process.env.MORROW_BROWSER_REHEARSAL_PASSWORD;
  if (!enabled || targetRef !== VALIDATION_PROJECT
      || !targetUrl.includes(`${VALIDATION_PROJECT}.supabase.co`) || !email || !password) {
    return new NextResponse('Not found', { status: 404 });
  }

  const client = await createClient();
  const { error } = await client.auth.signInWithPassword({ email, password });
  if (error) return new NextResponse('Isolated sign-in failed', { status: 503 });
  return NextResponse.redirect(new URL(MORROW_CASE_ROUTE, url.origin), { status: 302 });
}
