"use client";

import { useActionState } from "react";
import { requestAuthorLink, type LoginState } from "./actions";

const initial: LoginState = { kind: "idle", message: "" };

export function LoginForm() {
  const [state, action, pending] = useActionState(requestAuthorLink, initial);
  return (
    <form action={action} className="operator-login-form">
      <label htmlFor="operator-email">Authorized operator email</label>
      <div className="operator-login-row">
        <input id="operator-email" name="email" type="email" required autoComplete="email" />
        <button type="submit" disabled={pending}>{pending ? "Sending…" : "Send secure link"}</button>
      </div>
      {state.message ? <p className={`login-message ${state.kind}`} role="status">{state.message}</p> : null}
    </form>
  );
}
