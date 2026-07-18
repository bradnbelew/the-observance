'use client';

import { useActionState } from 'react';
import { restoreCampBiographies, type CampBiographyState } from './actions';

const choices = [
  ['admin-custody', 'server administration, checksums, and custody'],
  ['camera-humor', 'camera work, shot logs, and visual jokes'],
  ['builder-countermark', 'structural work, counter-marks, and private revisions'],
  ['route-companion', 'route memory, companion work, and changing distance copies'],
] as const;

export function CampBiographyForm({ restored }: { restored: boolean }) {
  const [state, action, pending] = useActionState(restoreCampBiographies, restored
    ? { status: 'accepted', message: 'Four owner cards are restored.' } satisfies CampBiographyState
    : { status: 'idle', message: 'The export still treats the camp as four unlabeled stations.' } satisfies CampBiographyState);
  const accepted = state.status === 'accepted';
  return (
    <section className="old-copy" aria-labelledby="camp-owner-heading">
      <h2 id="camp-owner-heading">Restore people to the crossed work record</h2>
      <p>Use the overlapping work notes, repairs, jokes, and disagreements. This records responsibilities; it does not ask for a hidden sentence.</p>
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
