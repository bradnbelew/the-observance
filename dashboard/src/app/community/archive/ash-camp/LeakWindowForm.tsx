'use client';

import { useActionState } from 'react';
import { proveLeakWindow, type LeakWindowState } from './actions';

const copies = [
  ['rook-private-countermark', 'Rook private NB-17/c counter-mark'],
  ['witness-spool-intake', 'Witness Spool NB-17/c intake'],
  ['public-upload', 'Copperline public NB-17/c upload'],
] as const;

export function LeakWindowForm({ proven }: { proven: boolean }) {
  const [state, action, pending] = useActionState(proveLeakWindow, proven
    ? { status: 'accepted', message: 'The private version chain is preserved.' } satisfies LeakWindowState
    : { status: 'idle', message: 'No private version chain has been preserved.' } satisfies LeakWindowState);
  const accepted = state.status === 'accepted';
  return <section className="old-copy" aria-labelledby="version-chain-heading">
    <h2 id="version-chain-heading">Preserve the version chain</h2>
    <p>Use the clocks printed by separate systems. Place the copy that existed first, the copy that proves a crossing, and the later public copy. Then record only what that chain can support.</p>
    <form action={action} aria-describedby="version-chain-result">
      <fieldset disabled={pending || accepted}><legend>Three-copy custody chain</legend>
        {([['before', 'Copy before the crossing'], ['crossing', 'Copy that proves the crossing'], ['after', 'Later public copy']] as const).map(([name, label]) => <label key={name}>{label}<select name={name} defaultValue="" required><option value="" disabled>Choose a copy</option>{copies.map(([value, text]) => <option value={value} key={value}>{text}</option>)}</select></label>)}
      </fieldset>
      <fieldset disabled={pending || accepted}><legend>Finding limits</legend>
        <label>Release board state<select name="readiness" defaultValue="" required><option value="" disabled>Choose the supported state</option><option value="release-board-complete">required work was checked complete before the crossing</option><option value="release-board-incomplete">required work was still missing</option></select></label>
        <label>Strongest supported claim<select name="boundary" defaultValue="" required><option value="" disabled>Choose the supported limit</option><option value="inside-access-sender-open">someone with inside access transmitted it; these copies do not name who</option><option value="wren-proven">these copies alone prove Wren sent it</option><option value="record-invented">the Record created the private revision without access</option></select></label>
      </fieldset>
      <button type="submit" disabled={pending || accepted}>{pending ? 'Checking clocks...' : accepted ? 'Version chain preserved' : 'Preserve version chain'}</button>
    </form>
    <div id="version-chain-result" role="status" aria-live="polite" data-status={state.status}><p><b>{state.status}.</b> {state.message}</p>{state.receiptId && <p><b>Receipt:</b> <code>{state.receiptId}</code></p>}</div>
  </section>;
}
