'use client';

import { useActionState } from 'react';
import { requestPlayerLink, type PlayerLoginState } from './actions';

const initial: PlayerLoginState = { kind: 'idle', message: '' };

export function PlayerLoginForm() {
  const [state, action, pending] = useActionState(requestPlayerLink, initial);
  return <form action={action} className="player-login-form">
    <label htmlFor="player-email">Linked account email</label>
    <div className="player-login-row">
      <input id="player-email" name="email" type="email" required autoComplete="email" />
      <button type="submit" disabled={pending}>{pending ? 'Requesting…' : 'Email one-time case link'}</button>
    </div>
    {state.message ? <p className={`player-login-message ${state.kind}`} role="status">{state.message}</p> : null}
  </form>;
}
