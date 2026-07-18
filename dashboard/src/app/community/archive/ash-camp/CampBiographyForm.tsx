'use client';

import { useActionState } from 'react';
import { restoreCampBiographies, type CampBiographyState } from './actions';

const choices = [
  ['admin-custody', 'kept the camera frames and build-notebook mirror stable after outages'],
  ['camera-humor', 'photographed the repaired brace face and left the shot card under supper'],
  ['builder-countermark', 'marked the brace privately and repaired the seat somebody kept using'],
  ['route-companion', 'walked the changed route with a friend and retained both distance cards'],
] as const;

export function CampBiographyForm({ restored }: { restored: boolean }) {
  const [state, action, pending] = useActionState(restoreCampBiographies, restored
    ? { status: 'accepted', message: 'Four owner cards are restored.' } satisfies CampBiographyState
    : { status: 'idle', message: 'The export still treats the camp as four unlabeled stations.' } satisfies CampBiographyState);
  const accepted = state.status === 'accepted';
  return (
    <section className="old-copy" aria-labelledby="camp-owner-heading">
      <h2 id="camp-owner-heading">Restore people to the crossed work record</h2>
      <p>Use the overlapping work notes, repairs, jokes, and disagreements. Each card must connect one person to work another person can authenticate. This records relationships, not four isolated job labels, and it does not ask for a hidden sentence.</p>
      <form action={action} aria-describedby="camp-owner-result">
        <fieldset disabled={pending || accepted}><legend>Owner cards</legend>
          {(['mkept', 'ash', 'rook', 'wren'] as const).map((person) => <label key={person}>{person}<select name={person} defaultValue="" required><option value="" disabled>Choose a work and relationship trace</option>{choices.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select></label>)}
        </fieldset>
        <button type="submit" disabled={pending || accepted}>{pending ? 'Restoring...' : accepted ? 'Owner cards restored' : 'Restore owner cards'}</button>
      </form>
      <div id="camp-owner-result" role="status" aria-live="polite" data-status={state.status}><p><b>{state.status}.</b> {state.message}</p>{state.receiptId && <p><b>Receipt:</b> <code>{state.receiptId}</code></p>}</div>
    </section>
  );
}
