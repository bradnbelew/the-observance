'use client';

import { useActionState } from 'react';
import { restoreSupplierHistory, type SupplierRestoreState } from './actions';

export function SupplierRestoreForm({ restored }: { restored: boolean }) {
  const [state, action, pending] = useActionState(restoreSupplierHistory, restored
    ? { status: 'accepted', message: 'Both versions are restored side by side.' } satisfies SupplierRestoreState
    : { status: 'idle', message: 'No version-history treatment has been saved.' } satisfies SupplierRestoreState);
  const accepted = state.status === 'accepted';
  return (
    <section className="old-copy" aria-labelledby="supplier-restore-heading">
      <h2 id="supplier-restore-heading">Restore the attachment history</h2>
      <form action={action} aria-describedby="supplier-restore-result">
        <fieldset disabled={pending || accepted}>
          <legend>Version treatment</legend>
          <label><input type="radio" name="operation" value="replace-a-with-b" /> Replace Draft A with the newer Draft B.</label>
          <label><input type="radio" name="operation" value="delete-b" /> Delete Draft B as an unauthorized edit.</label>
          <label><input type="radio" name="operation" value="restore-both-versions" /> Restore Draft A and Draft B side by side with their original times.</label>
        </fieldset>
        <button type="submit" disabled={pending || accepted}>{pending ? 'Restoring...' : accepted ? 'History restored' : 'Restore selected history'}</button>
      </form>
      <div id="supplier-restore-result" role="status" aria-live="polite" data-status={state.status}><p><b>{state.status}.</b> {state.message}</p>{state.receiptId && <p><b>Receipt:</b> <code>{state.receiptId}</code></p>}</div>
    </section>
  );
}
