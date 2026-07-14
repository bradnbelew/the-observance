'use server';

import { redirect } from 'next/navigation';
import { recordV5WebSequence } from '@/lib/v5-web-progress';
import { OBSERVANCE_DIRECTORY_KEY } from '@/lib/v5-ls02-docket';
import { isCorrectLs02DocketAnswer } from '@/lib/v5-ls02-answer';

export type ServiceDocketState = {
  kind: 'idle' | 'rejected';
  message: string;
};

/** Dedicated LS02 resolver: callers cannot choose a node, flag, receipt namespace, or payload. */
export async function resolveLs02ServiceDocket(
  _previous: ServiceDocketState,
  formData: FormData,
): Promise<ServiceDocketState> {
  const submitted = String(formData.get('serviceDocket') ?? '');
  if (!isCorrectLs02DocketAnswer(submitted)) {
    return { kind: 'rejected', message: 'No matching retained service was found.' };
  }

  const progress = await recordV5WebSequence(
    ['LS02'],
    'copperline_service_1842',
    { handler: 'answer_resolver', docket_verified: true },
  );
  if (!progress.complete) {
    // Prerequisite failure and infrastructure failure intentionally share one non-oracular response.
    return { kind: 'rejected', message: 'No matching retained service was found.' };
  }

  redirect(`/server.php?id=${OBSERVANCE_DIRECTORY_KEY}`);
}
