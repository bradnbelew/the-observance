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
      <h2 id="restore-heading">Restore a retained attachment</h2>
      <p>Use the ticket and custody details already visible in the public thread. This form restores a copy. It does not decide what the copy means.</p>
      <form className="archive-restore-form" action={formAction} aria-describedby="restore-help restore-result">
        <p id="restore-help">All four fields are required. Submit sends the request. Your browser Back button or leaving the page cancels before submission.</p>
        <p><label htmlFor="ticket">Ticket number</label><br /><input id="ticket" name="ticket" inputMode="numeric" required maxLength={8} /></p>
        <p><label htmlFor="attachment">Attachment filename</label><br /><input id="attachment" name="attachment" required maxLength={64} /></p>
        <p><label htmlFor="order">Cartridge order</label><br /><input id="order" name="order" required maxLength={24} placeholder="earlier-later" /></p>
        <p><label htmlFor="idempotency">Your request ID</label><br /><input id="idempotency" name="idempotency" required minLength={6} maxLength={48} autoComplete="off" /></p>
        <button type="submit" disabled={pending}>{pending ? 'Restoring…' : 'Restore retained copy'}</button>
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
              <p className="old-post-meta"><b>{entry.author}</b> &middot; <time dateTime={entry.date}>{new Date(entry.date).toLocaleString('en-US', { timeZone: 'America/Chicago' })}</time> &middot; {entry.kind}</p>
              <pre style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{entry.body}</pre>
            </div></article></li>
          ))}
        </ol>
      )}
    </section>
  );
}
