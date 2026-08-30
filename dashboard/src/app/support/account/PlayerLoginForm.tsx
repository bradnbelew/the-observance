export function PlayerLoginForm({ sent }: { sent: boolean }) {
  return <form action="/auth/player-link" method="post" className="player-login-form">
    <label htmlFor="player-email">Linked account email</label>
    <div className="player-login-row">
      <input id="player-email" name="email" type="email" required autoComplete="email" />
      <button type="submit">Email one-time case link</button>
    </div>
    {sent ? <p className="player-login-message sent" role="status">
      If that address owns a current assignment, a one-time case link has been sent.
    </p> : null}
  </form>;
}
