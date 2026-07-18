'use server';

import { headers } from 'next/headers';
import { recordCampaignEvent } from '@/lib/arg-event-store';

export type PackageReviewState = {
  status: 'idle' | 'accepted' | 'wrong' | 'incomplete' | 'technical_failure';
  message: string;
  receiptId?: string;
};

function sameOrigin(origin: string | null, host: string | null): boolean {
  if (!origin || !host) return true;
  try { return new URL(origin).host === host; } catch { return false; }
}

export async function confirmPackageReview(
  _previous: PackageReviewState,
  formData: FormData,
): Promise<PackageReviewState> {
  const requestHeaders = await headers();
  if (!sameOrigin(requestHeaders.get('origin'), requestHeaders.get('host'))) {
    return { status: 'technical_failure', message: 'The request came from another host. Nothing changed.' };
  }

  const decision = String(formData.get('decision') ?? '').normalize('NFKC').trim();
  if (!decision) return { status: 'incomplete', message: 'Choose how to treat the package and relay note.' };
  if (decision !== 'verify-package-quarantine-relay') {
    return {
      status: 'wrong',
      message: 'That choice gives an unverified route the same custody as the retained package. Nothing changed.',
    };
  }

  const event = await recordCampaignEvent({
    eventKey: 'p2.artifact_authenticated',
    idempotencyKey: 'copperline:p2:package-669f3fd0-relay-quarantine',
    source: 'copperline',
    payload: {
      artifact: 'the-hold.zip',
      sha1: '669f3fd00bbb6e647eeb8941e79281cc434f1e8c',
      relay_note: 'quarantined-unverified',
      decision,
    },
  });
  if (event.status === 'blocked') {
    return { status: 'incomplete', message: 'The archive treatment must be settled first. Nothing changed.' };
  }
  if (event.status !== 'committed') {
    return { status: 'technical_failure', message: 'Copperline could not save the review. Nothing changed; retry is safe.' };
  }
  return {
    status: 'accepted',
    message: 'Review saved. The retained world and checksum stay together. The later relay address is quarantined, not trusted.',
    receiptId: event.eventId,
  };
}
