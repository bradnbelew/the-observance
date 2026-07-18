'use server';

import { headers } from 'next/headers';
import { recordCampaignEvent } from '@/lib/arg-event-store';

export type CampBiographyState = { status: 'idle' | 'accepted' | 'wrong' | 'incomplete' | 'technical_failure'; message: string; receiptId?: string };
export type LeakWindowState = CampBiographyState;

async function sameOrigin(): Promise<boolean> {
  const requestHeaders = await headers();
  const origin = requestHeaders.get('origin');
  const host = requestHeaders.get('host');
  if (!origin || !host) return true;
  try { return new URL(origin).host === host; }
  catch { return false; }
}

export async function restoreCampBiographies(_previous: CampBiographyState, formData: FormData): Promise<CampBiographyState> {
  if (!await sameOrigin()) return { status: 'technical_failure', message: 'The request came from another host. Nothing changed.' };
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

/** Preserve an authenticated version chain. This is provenance work, not a hidden sentence. */
export async function proveLeakWindow(_previous: LeakWindowState, formData: FormData): Promise<LeakWindowState> {
  if (!await sameOrigin()) return { status: 'technical_failure', message: 'The request came from another host. Nothing changed.' };
  const finding = {
    before: String(formData.get('before') ?? ''),
    crossing: String(formData.get('crossing') ?? ''),
    after: String(formData.get('after') ?? ''),
    readiness: String(formData.get('readiness') ?? ''),
    boundary: String(formData.get('boundary') ?? ''),
  };
  if (Object.values(finding).some((value) => !value)) return { status: 'incomplete', message: 'Place all three copies and set both finding limits.' };
  if (finding.before !== 'rook-private-countermark' || finding.crossing !== 'witness-spool-intake'
      || finding.after !== 'public-upload' || finding.readiness !== 'release-board-complete'
      || finding.boundary !== 'inside-access-sender-open') {
    return { status: 'wrong', message: 'That chain conflicts with the independent clocks, or claims more than the copies prove. Nothing changed.' };
  }
  const event = await recordCampaignEvent({
    eventKey: 'p9.leak_window_proven',
    idempotencyKey: 'copperline:p9:private-version-chain-v1',
    source: 'copperline',
    payload: {
      chain: ['rook-private-countermark', 'witness-spool-intake', 'public-upload'],
      readiness: 'release-board-complete',
      claim_boundary: 'inside-access-sender-open',
      observation_receipts: 0,
    },
  });
  if (event.status === 'blocked') return { status: 'incomplete', message: 'Restore the four owner cards before preserving their private version chain.' };
  if (event.status === 'collision') return { status: 'technical_failure', message: 'A different version chain already owns this receipt. Nothing changed.' };
  if (event.status !== 'committed') return { status: 'technical_failure', message: 'Copperline could not preserve the version chain. Nothing changed; retry is safe.' };
  return { status: 'accepted', message: 'Version chain preserved. It proves access inside the four-person company; it does not yet identify the sender.', receiptId: event.eventId };
}
