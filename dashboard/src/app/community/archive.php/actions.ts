'use server';

import { headers } from 'next/headers';
import { recordCampaignEvent } from '@/lib/arg-event-store';

export type CustodyDecisionState = {
  status: 'idle' | 'accepted' | 'wrong' | 'incomplete' | 'technical_failure';
  message: string;
  receiptId?: string;
};

export async function confirmCustodyDecision(
  _previous: CustodyDecisionState,
  formData: FormData,
): Promise<CustodyDecisionState> {
  const requestHeaders = await headers();
  const origin = requestHeaders.get('origin');
  const host = requestHeaders.get('host');
  if (origin && host) {
    try {
      if (new URL(origin).host !== host) return { status: 'technical_failure', message: 'The request came from another host. Nothing changed.' };
    } catch {
      return { status: 'technical_failure', message: 'The request origin was invalid. Nothing changed.' };
    }
  }

  const treatment = String(formData.get('treatment') ?? '').normalize('NFKC').trim();
  if (!treatment) return { status: 'incomplete', message: 'Choose an archive treatment before you confirm.' };
  if (treatment !== 'retain-damaged-redact-private') {
    return { status: 'wrong', message: 'That treatment conflicts with the retained ticket and account history. Nothing changed.' };
  }

  const event = await recordCampaignEvent({
    eventKey: 'p1.mkept_intent_authenticated',
    idempotencyKey: 'copperline:p1:mkept-custody-treatment',
    source: 'copperline',
    payload: { service: '1842', ticket: '9137', treatment, conclusion: 'deliberate-preservation' },
  });
  if (event.status === 'blocked') return { status: 'incomplete', message: 'Restore the retained attachment history before confirming its treatment.' };
  if (event.status !== 'committed') return { status: 'technical_failure', message: 'Copperline could not save that decision. Nothing changed. You can safely try again.' };
  return {
    status: 'accepted',
    message: 'Accepted. The recovered copy stays damaged and read-only. Player data and chat stay out of the public archive.',
    receiptId: event.eventId,
  };
}
