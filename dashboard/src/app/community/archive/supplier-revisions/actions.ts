'use server';

import { headers } from 'next/headers';
import { recordCampaignEvent } from '@/lib/arg-event-store';

export type SupplierRestoreState = { status: 'idle' | 'accepted' | 'wrong' | 'incomplete' | 'technical_failure'; message: string; receiptId?: string };

export async function restoreSupplierHistory(_previous: SupplierRestoreState, formData: FormData): Promise<SupplierRestoreState> {
  const requestHeaders = await headers();
  const origin = requestHeaders.get('origin');
  const host = requestHeaders.get('host');
  if (origin && host) {
    try { if (new URL(origin).host !== host) return { status: 'technical_failure', message: 'The request came from another host. Nothing changed.' }; }
    catch { return { status: 'technical_failure', message: 'The request origin was invalid. Nothing changed.' }; }
  }
  const operation = String(formData.get('operation') ?? '').normalize('NFKC').trim();
  if (!operation) return { status: 'incomplete', message: 'Choose a version-history treatment.' };
  if (operation !== 'restore-both-versions') {
    return { status: 'wrong', message: 'That treatment would erase one side of the supplier history. Nothing changed.' };
  }
  const event = await recordCampaignEvent({
    eventKey: 'p7.supplier_history_restored',
    idempotencyKey: 'copperline:p7:merrit-draft-a-b-restore',
    source: 'copperline',
    payload: { operation, attachment: 'cistern-cloth-delivery', originals_changed: false },
  });
  if (event.status === 'blocked') return { status: 'incomplete', message: 'The material comparison has not reached the archive. Nothing changed.' };
  if (event.status !== 'committed') return { status: 'technical_failure', message: 'The archive could not save the restore. Nothing changed; retry is safe.' };
  return { status: 'accepted', message: 'Both attachment versions are restored side by side. Neither original was overwritten.', receiptId: event.eventId };
}
