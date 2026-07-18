'use client';

import { useActionState } from 'react';
import { publishInterventionPlan, type InterventionPlanState } from './actions';

export function InterventionPlanForm({ planned }: { planned: boolean }) {
  const [state, action, pending] = useActionState(publishInterventionPlan, planned
    ? { status: 'accepted', message: 'The intervention plan is already public.' } satisfies InterventionPlanState
    : { status: 'idle', message: 'No current intervention has been attached.' } satisfies InterventionPlanState);
  const accepted = state.status === 'accepted';
  return (
    <section className="old-copy" aria-labelledby="intervention-heading">
      <h2 id="intervention-heading">Attach a testable intervention</h2>
      <p>Use four short findings. Name interacting causes, separate Iss&apos;s evidence from his route, state what the altered copy does and does not prove, and put the works in a safe order. This form does not require one exact sentence.</p>
      <form action={action} aria-describedby="intervention-result">
        <fieldset disabled={pending || accepted}>
          <legend>Current group plan</legend>
          <label>Interacting causes<textarea name="causes" required maxLength={180} rows={3} /></label>
          <label>Iss: supported finding and unsafe act<textarea name="iss" required maxLength={140} rows={2} /></label>
          <label>Altered-copy evidence boundary<textarea name="copyBoundary" required maxLength={140} rows={2} /></label>
          <label>Safe works order<textarea name="order" required maxLength={140} rows={2} /></label>
        </fieldset>
        <button type="submit" disabled={pending || accepted}>{pending ? 'Publishing...' : accepted ? 'Plan attached' : 'Attach intervention plan'}</button>
      </form>
      <div id="intervention-result" role="status" aria-live="polite" data-status={state.status}>
        <p><b>{state.status}.</b> {state.message}</p>
        {state.receiptId && <p><b>Receipt:</b> <code>{state.receiptId}</code></p>}
      </div>
    </section>
  );
}
