'use server';

import { headers } from 'next/headers';
import { recordCampaignEvent } from '@/lib/arg-event-store';
import { P8_INTERVENTION_CANONICAL_PAYLOAD, unsupportedInterventionParts } from '@/lib/p8-intervention-plan';

export type InterventionPlanState = {
  status: 'idle' | 'accepted' | 'wrong' | 'incomplete' | 'technical_failure';
  message: string;
  receiptId?: string;
};

export async function publishInterventionPlan(
  _previous: InterventionPlanState,
  formData: FormData,
): Promise<InterventionPlanState> {
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
  const finding = {
    causes: String(formData.get('causes') ?? ''),
    iss: String(formData.get('iss') ?? ''),
    copyBoundary: String(formData.get('copyBoundary') ?? ''),
    order: String(formData.get('order') ?? ''),
  };
  if (Object.values(finding).some((value) => !value.trim())) {
    return { status: 'incomplete', message: 'Enter all four parts of the proposed intervention. Nothing changed.' };
  }
  const unsupported = unsupportedInterventionParts(finding);
  if (unsupported.length > 0) {
    return { status: 'wrong', message: `Review: ${unsupported.join(', ')}. Nothing changed.` };
  }
  const event = await recordCampaignEvent({
    eventKey: 'p8.intervention_plan_accepted',
    idempotencyKey: 'copperline:p8:hold-intervention-plan-v1',
    source: 'copperline',
    payload: P8_INTERVENTION_CANONICAL_PAYLOAD,
  });
  if (event.status === 'blocked') return { status: 'incomplete', message: 'Nessa\'s public correction is not available yet. Nothing changed.' };
  if (event.status === 'collision') return { status: 'technical_failure', message: 'A different intervention already owns this receipt. Nothing changed.' };
  if (event.status !== 'committed') return { status: 'technical_failure', message: 'Copperline could not save the plan. Nothing changed; retry is safe.' };
  return {
    status: 'accepted',
    message: 'Plan published. The four findings are stored as a bounded causal model; the physical Hold works remain unchanged.',
    receiptId: event.eventId,
  };
}
