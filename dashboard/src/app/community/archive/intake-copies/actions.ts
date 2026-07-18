'use server';

import { headers } from 'next/headers';
import { validateP4Restore, type P4RestoreState } from '@/lib/copperline-p4-restore';
import { recordCampaignEvent } from '@/lib/arg-event-store';

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
  const validation = validateP4Restore(formData);
  if (validation.status !== 'accepted') return validation;
  const event = await recordCampaignEvent({
    eventKey: 'p4.mouth_revision_restored',
    idempotencyKey: 'copperline:p4:ticket-2184-retained-copy',
    source: 'copperline',
    payload: {
      ticket: '2184',
      operation: 'restore-retained-attachments',
      recordIds: validation.entries?.map((entry) => entry.id) ?? [],
      originalRowsChanged: false,
    },
  });
  if (event.status === 'blocked') {
    return { status: 'incomplete', message: 'The archive has not received the earlier dispatch record. Nothing changed.' };
  }
  if (event.status !== 'committed') {
    return { status: 'technical_failure', message: 'The archive could not save this restore. Nothing changed. You can safely try again.' };
  }
  return { ...validation, receiptId: event.eventId ?? validation.receiptId };
}
