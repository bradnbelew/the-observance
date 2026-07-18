'use client';

import { useActionState } from 'react';
import { confirmCustodyDecision, type CustodyDecisionState } from './actions';

const INITIAL: CustodyDecisionState = { status: 'idle', message: 'No archive treatment has been confirmed.' };

export function CustodyDecisionForm({ alreadyAccepted }: { alreadyAccepted: boolean }) {
  const initial = alreadyAccepted ? {
    status: 'accepted' as const,
    message: 'The recovered copy stays damaged and read-only. Player data and chat stay out of the public archive.',
  } : INITIAL;
  const [state, action, pending] = useActionState(confirmCustodyDecision, initial);
  return (
    <section className="old-copy" aria-labelledby="custody-decision-heading">
      <h2 id="custody-decision-heading">Set the public archive treatment</h2>
      <p>The ticket, account posts, and retained files do not agree on every detail. Choose what Copperline should do with the recovered copy.</p>
      <form action={action} aria-describedby="custody-result">
        <fieldset disabled={pending || state.status === 'accepted'}>
          <legend>Archive treatment</legend>
          <label><input type="radio" name="treatment" value="replace-with-newest" /> Replace the damaged copy with the newest file.</label>
          <label><input type="radio" name="treatment" value="restart-clean" /> Restart it and publish a clean world.</label>
          <label><input type="radio" name="treatment" value="retain-damaged-redact-private" /> Keep the damaged copy, remove private player records, and publish only a read-only recovery.</label>
        </fieldset>
        <button type="submit" disabled={pending || state.status === 'accepted'}>{pending ? 'Saving…' : state.status === 'accepted' ? 'Treatment saved' : 'Confirm archive treatment'}</button>
      </form>
      <div id="custody-result" role="status" aria-live="polite" data-status={state.status}>
        <p><b>{state.status}.</b> {state.message}</p>
        {state.receiptId && <p><b>Receipt:</b> <code>{state.receiptId}</code></p>}
      </div>
    </section>
  );
}
