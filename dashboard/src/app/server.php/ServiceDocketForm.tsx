'use client';

import { useActionState } from 'react';
import {
  resolveLs02ServiceDocket,
  type ServiceDocketState,
} from './actions';

const initialState: ServiceDocketState = { kind: 'idle', message: '' };

export function ServiceDocketForm() {
  const [state, action, pending] = useActionState(resolveLs02ServiceDocket, initialState);
  return (
    <form action={action} className="docket-resolver-form">
      <label htmlFor="service-docket-answer">
        Recovered service field
        <input
          id="service-docket-answer"
          name="serviceDocket"
          type="text"
          required
          autoComplete="off"
          spellCheck={false}
          aria-describedby="docket-submit-result"
        />
      </label>
      <button type="submit" disabled={pending}>{pending ? 'Searching...' : 'Search archive'}</button>
      <p id="docket-submit-result" className={`docket-submit-result ${state.kind}`} role="status" aria-live="polite">
        {state.message}
      </p>
    </form>
  );
}
