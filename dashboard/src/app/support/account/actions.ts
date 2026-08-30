'use server';

import { headers } from 'next/headers';
import { createClient } from '@/lib/supabase/server';
import { MORROW_CASE_ROUTE } from '@/lib/morrow-copperline-case';

export type PlayerLoginState = { message: string; kind: 'idle' | 'sent' | 'error' };

export async function requestPlayerLink(
  _previous: PlayerLoginState,
  formData: FormData,
): Promise<PlayerLoginState> {
  const email = String(formData.get('email') ?? '').trim().toLowerCase();
  if (email.length < 3 || email.length > 254 || !email.includes('@')) {
    return { kind: 'error', message: 'Enter the email address linked to this recovery assignment.' };
  }

  try {
    const headerStore = await headers();
    const requestOrigin = headerStore.get('origin');
    const host = headerStore.get('host');
    if (!requestOrigin || !host) {
      return { kind: 'error', message: 'The account desk could not verify this request. Nothing changed.' };
    }
    const originUrl = new URL(requestOrigin);
    if (!['http:', 'https:'].includes(originUrl.protocol) || originUrl.host !== host) {
      return { kind: 'error', message: 'The account desk could not verify this request. Nothing changed.' };
    }
    const supabase = await createClient();
    const { error } = await supabase.auth.signInWithOtp({
      email,
      options: {
        emailRedirectTo: `${originUrl.origin}/auth/callback?next=${encodeURIComponent(MORROW_CASE_ROUTE)}`,
        shouldCreateUser: false,
      },
    });
    if (error && (error.status ?? 500) >= 500) {
      return { kind: 'error', message: 'The account desk is temporarily unavailable. Nothing changed.' };
    }
    return {
      kind: 'sent',
      message: 'If that address owns a current assignment, a one-time case link has been sent.',
    };
  } catch {
    return { kind: 'error', message: 'The account desk is temporarily unavailable. Nothing changed.' };
  }
}
