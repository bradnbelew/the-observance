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
      <h2 id="package-review-heading">Record the custody decision</h2>
      <p>Choose what the evidence supports. This is a package decision, not a password or hidden phrase.</p>
      <form action={action} aria-describedby="package-review-result">
        <fieldset disabled={pending || accepted}>
          <legend>Package and route treatment</legend>
          <label><input type="radio" name="decision" value="trust-relay-and-package" /> Trust the package and the relay address because they arrived together.</label>
          <label><input type="radio" name="decision" value="reject-everything" /> Reject the package because the relay address has no retained checksum.</label>
          <label><input type="radio" name="decision" value="verify-package-quarantine-relay" /> Verify the retained world with its checksum; quarantine the later relay address as unverified.</label>
        </fieldset>
        <button type="submit" disabled={pending || accepted}>{pending ? 'Saving...' : accepted ? 'Review saved' : 'Save package review'}</button>
      </form>
      <div id="package-review-result" role="status" aria-live="polite" data-status={state.status}>
        <p><b>{state.status}.</b> {state.message}</p>
        {state.receiptId && <p><b>Receipt:</b> <code>{state.receiptId}</code></p>}
      </div>
      {accepted ? <p><Link href="/community/2011/02/08/world-backup">Open mkept&apos;s retained world post</Link>. Opening the world is the next action; no copied answer is required.</p> : null}
    </section>
  );
}
