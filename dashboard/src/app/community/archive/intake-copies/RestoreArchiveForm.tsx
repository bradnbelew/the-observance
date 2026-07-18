'use client';

import { useActionState } from 'react';
import type { P4RestoreState } from '@/lib/copperline-p4-restore';
import { restoreP4ArchiveAction } from './actions';

const INITIAL_P4_RESTORE_STATE: P4RestoreState = {
  status: 'idle',
  message: 'No restore request has been sent.',
};

export function RestoreArchiveForm() {
  const [state, formAction, pending] = useActionState(restoreP4ArchiveAction, INITIAL_P4_RESTORE_STATE);
  return (
    <section className="old-copy" aria-labelledby="restore-heading">
      <h2 id="restore-heading">Restore retained attachments</h2>
      <p>This action rebuilds the five retained rows as a read-only copy. It does not decide what the records mean.</p>
      <form className="archive-restore-form" action={formAction} aria-describedby="restore-help restore-result">
        <input type="hidden" name="operation" value="restore-retained-attachments" />
        <p id="restore-help">Submit restores a read-only copy. Repeating the action returns the same receipt. Leaving this page before submission changes nothing.</p>
        <button type="submit" disabled={pending}>{pending ? 'Restoring…' : 'Restore retained attachments'}</button>
      </form>
      <div className="archive-restore-result" id="restore-result" role="status" aria-live="polite" data-status={state.status}>
        <p><b>{state.status.replaceAll('_', ' ')}.</b> {state.message}</p>
        {state.receiptId && <p><b>Receipt:</b> <code>{state.receiptId}</code></p>}
      </div>
      {state.status === 'accepted' && state.entries && (
        <ol className="restored-records" aria-label="Restored Ticket 2184 chronological record">
          {state.entries.map((entry) => (
            <li key={entry.id}><article><div>
              <h3>{entry.title}</h3>
              <p className="old-post-meta"><b>{entry.author}</b> · <time dateTime={entry.date}>{new Date(entry.date).toLocaleString('en-US', { timeZone: 'America/Chicago' })}</time> · {entry.kind}</p>
              <pre style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{entry.body}</pre>
            </div></article></li>
          ))}
        </ol>
      )}
    </section>
  );
}
