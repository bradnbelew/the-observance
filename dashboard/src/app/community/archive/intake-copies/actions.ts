'use server';

import { headers } from 'next/headers';
import { validateP4Restore, type P4RestoreState } from '@/lib/copperline-p4-restore';

export async function restoreP4ArchiveAction(
  _previous: P4RestoreState,
  formData: FormData,
): Promise<P4RestoreState> {
  const requestHeaders = await headers();
  const origin = requestHeaders.get('origin');
  const host = requestHeaders.get('host');
  if (origin && host) {
    try {
      if (new URL(origin).host !== host) {
        return { status: 'technical_failure', message: 'The request origin did not match this archive. Nothing changed.' };
      }
    } catch {
      return { status: 'technical_failure', message: 'The request origin was invalid. Nothing changed.' };
    }
  }
  return validateP4Restore(formData);
}
