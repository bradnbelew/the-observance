'use client';

import Link from 'next/link';
import { useActionState } from 'react';
import { confirmPackageReview, type PackageReviewState } from './actions';

const INITIAL: PackageReviewState = { status: 'idle', message: 'No package review has been saved.' };

export function PackageReviewForm({ alreadyAccepted }: { alreadyAccepted: boolean }) {
  const initial: PackageReviewState = alreadyAccepted ? {
    status: 'accepted',
    message: 'The retained world and checksum stay together. The later relay address remains quarantined.',
  } : INITIAL;
  const [state, action, pending] = useActionState(confirmPackageReview, initial);
  const accepted = state.status === 'accepted';
  return (
    <section className="old-copy" aria-labelledby="package-review-heading">
      <h2 id="package-review-heading">Verify the retained package</h2>
      <p>Run the retained receipt against the actual packaged bytes. This is a real integrity check, not a password, hidden phrase, or trust choice.</p>
      <form action={action} aria-describedby="package-review-result">
        <button type="submit" disabled={pending || accepted}>{pending ? 'Hashing packaged world...' : accepted ? 'Package verified' : 'Run retained-receipt verification'}</button>
      </form>
      <div id="package-review-result" role="status" aria-live="polite" data-status={state.status}>
        <p><b>{state.status}.</b> {state.message}</p>
        {state.receiptId && <p><b>Receipt:</b> <code>{state.receiptId}</code></p>}
      </div>
      {accepted ? <p><Link href="/community/2011/02/08/world-backup">Open mkept&apos;s retained world post</Link>. The package passed the actual byte check; the unmatched relay note remains quarantined. No copied answer is required.</p> : null}
    </section>
  );
}
