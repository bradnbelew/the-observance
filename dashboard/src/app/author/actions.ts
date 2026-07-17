'use server';

import { revalidatePath } from 'next/cache';
import { createAdminClient } from '@/lib/supabase/admin';
import { assertAuthor } from '@/lib/author-auth';
import { assertExternalMutationAllowed } from '@/lib/deployment-target';
import type { BeatStatus } from '@/lib/database.types';

export type ActionResult = { ok: boolean; error?: string };
const refresh = () => revalidatePath('/author');

async function decideBeat(id: number, status: Extract<BeatStatus, 'approved' | 'skipped'>): Promise<ActionResult> {
  await assertAuthor();
  assertExternalMutationAllowed('author.decideBeat');
  if (!Number.isSafeInteger(id) || id <= 0) return { ok: false, error: 'Invalid beat id.' };
  const supabase = createAdminClient();
  const { error } = await supabase.from('beat_queue').update({ status, decided_at: new Date().toISOString() }).eq('id', id).eq('status', 'pending');
  if (error) return { ok: false, error: error.message };
  await supabase.from('event_log').insert({ level: 'info', source: 'dashboard', message: `Beat #${id} ${status} from the V5 operations console.` });
  refresh(); return { ok: true };
}

export async function approveBeat(formData: FormData): Promise<ActionResult> { return decideBeat(Number(formData.get('id')), 'approved'); }
export async function skipBeat(formData: FormData): Promise<ActionResult> { return decideBeat(Number(formData.get('id')), 'skipped'); }
export async function approveBeatForm(formData: FormData): Promise<void> { await approveBeat(formData); }
export async function skipBeatForm(formData: FormData): Promise<void> { await skipBeat(formData); }

const nonnegativeInt = (value: FormDataEntryValue | null): number | null => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.max(0, Math.trunc(parsed)) : null;
};

export async function updateWhisperBudget(formData: FormData): Promise<ActionResult> {
  await assertAuthor();
  assertExternalMutationAllowed('author.updateWhisperBudget');
  const id = Number(formData.get('id'));
  const budget = nonnegativeInt(formData.get('budget'));
  const spent = nonnegativeInt(formData.get('spent'));
  const earned = nonnegativeInt(formData.get('earned'));
  if (!Number.isSafeInteger(id) || id <= 0 || budget === null || spent === null || earned === null) return { ok: false, error: 'Invalid whisper budget.' };
  const supabase = createAdminClient();
  const { error } = await supabase.from('whisper_budgets').update({ budget, spent, earned }).eq('id', id);
  if (error) return { ok: false, error: error.message };
  await supabase.from('event_log').insert({ level: 'info', source: 'dashboard', message: `Whisper budget #${id} updated.` });
  refresh(); return { ok: true };
}

export async function updateWhisperBudgetForm(formData: FormData): Promise<void> { await updateWhisperBudget(formData); }

export async function setWatcherSleep(asleep: boolean): Promise<ActionResult> {
  await assertAuthor();
  assertExternalMutationAllowed('author.setWatcherSleep');
  const supabase = createAdminClient();
  const { error } = await supabase.from('settings').upsert({ key: 'watcher_sleep', value: asleep, updated_at: new Date().toISOString() }, { onConflict: 'key' });
  if (error) return { ok: false, error: error.message };
  await supabase.from('event_log').insert({ level: 'warn', source: 'dashboard', message: `Persistent showrunner ${asleep ? 'paused' : 'resumed'} from the V5 operations console.` });
  refresh(); return { ok: true };
}
