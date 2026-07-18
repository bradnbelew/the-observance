'use server';

import { headers } from 'next/headers';
import { recordCampaignEvent } from '@/lib/arg-event-store';

export type CampBiographyState = { status: 'idle' | 'accepted' | 'wrong' | 'incomplete' | 'technical_failure'; message: string; receiptId?: string };

export async function restoreCampBiographies(_previous: CampBiographyState, formData: FormData): Promise<CampBiographyState> {
  const requestHeaders = await headers();
  const origin = requestHeaders.get('origin');
  const host = requestHeaders.get('host');
  if (origin && host) {
    try { if (new URL(origin).host !== host) return { status: 'technical_failure', message: 'The request came from another host. Nothing changed.' }; }
    catch { return { status: 'technical_failure', message: 'The request origin was invalid. Nothing changed.' }; }
  }
  const mapping = {
    mkept: String(formData.get('mkept') ?? ''),
    ash: String(formData.get('ash') ?? ''),
    rook: String(formData.get('rook') ?? ''),
    wren: String(formData.get('wren') ?? ''),
  };
  if (Object.values(mapping).some((value) => !value)) return { status: 'incomplete', message: 'Assign all four work traces before restoring the owner cards.' };
  if (mapping.mkept !== 'admin-custody' || mapping.ash !== 'camera-humor'
      || mapping.rook !== 'builder-countermark' || mapping.wren !== 'route-companion') {
    return { status: 'wrong', message: 'That assignment conflicts with the crossed work notes and shared camp objects. Nothing changed.' };
  }
  const event = await recordCampaignEvent({
    eventKey: 'p9.company_biographies_restored',
    idempotencyKey: 'copperline:p9:ash-camp-owner-cards',
    source: 'copperline',
    payload: { ...mapping, observation_receipts: 0, restored_as: 'people-not-stations' },
  });
  if (event.status === 'blocked') return { status: 'incomplete', message: 'The Hold works readback has not reached Copperline. Nothing changed.' };
  if (event.status === 'collision') return { status: 'technical_failure', message: 'A different owner-card restore already owns this receipt. Nothing changed.' };
  if (event.status !== 'committed') return { status: 'technical_failure', message: 'Copperline could not save the owner cards. Nothing changed; retry is safe.' };
  return { status: 'accepted', message: 'Four owner cards restored. Shared objects and crossed notes remain attached instead of being split into four puzzle stations.', receiptId: event.eventId };
}
