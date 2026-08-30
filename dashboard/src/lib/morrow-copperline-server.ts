import 'server-only';

import { createClient } from '@/lib/supabase/server';
import {
  projectMorrowCase,
  type MorrowCaseRead,
  type MorrowProjectionRow,
} from '@/lib/morrow-copperline-case';

export type MorrowServerRead = MorrowCaseRead | { kind: 'unavailable' };

/** Authenticated, RLS-scoped read. No campaign/player identifiers are accepted from the URL. */
export async function readMorrowCase(): Promise<MorrowServerRead> {
  const releaseId = process.env.MORROW_RELEASE_ID?.trim();
  if (!releaseId || !process.env.NEXT_PUBLIC_SUPABASE_URL || !process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY) {
    return { kind: 'unavailable' };
  }
  try {
    const client = await createClient();
    const { data: auth, error: authError } = await client.auth.getUser();
    if (authError || !auth.user) return { kind: 'empty', reason: 'authentication_required' };
    const { data, error } = await client
      .from('morrow_player_projection')
      .select('campaign_id,player_id,projection_key,projection,revision,updated_at')
      .order('revision', { ascending: true });
    if (error) return { kind: 'unavailable' };
    return projectMorrowCase((data ?? []) as MorrowProjectionRow[], releaseId);
  } catch {
    return { kind: 'unavailable' };
  }
}
