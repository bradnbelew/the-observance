"use client";

import { useActionState } from "react";
import { signInWithMagicLink, type LoginState } from "./actions";

const initialState: LoginState = { status: "idle" };

export default function LoginPage() {
  const [state, formAction, pending] = useActionState(
    signInWithMagicLink,
    initialState,
  );

  return (
    <div className="mx-auto max-w-sm space-y-6 pt-8">
      <div className="space-y-1">
        <h1 className="font-mono text-xl text-neutral-100">Sign in</h1>
        <p className="text-sm text-neutral-400">
          Author mode is restricted. We&apos;ll email you a sign-in link.
        </p>
      </div>

      <form action={formAction} className="space-y-3">
        <label className="block text-sm text-neutral-400" htmlFor="email">
          Email
        </label>
        <input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          required
          className="w-full rounded-md border border-neutral-800 bg-slate-850 px-3 py-2 text-sm text-neutral-100 outline-none focus:border-neutral-600"
          placeholder="you@example.com"
        />
        <button
          type="submit"
          disabled={pending}
          className="w-full rounded-md border border-neutral-700 bg-neutral-800 px-3 py-2 text-sm text-neutral-100 transition-colors hover:bg-neutral-700 disabled:opacity-50"
        >
          {pending ? "Sending…" : "Send magic link"}
        </button>
      </form>

      {state.status !== "idle" && state.message ? (
        <p
          className={
            state.status === "error"
              ? "text-sm text-red-400"
              : "text-sm text-emerald-400"
          }
        >
          {state.message}
        </p>
      ) : null}
    </div>
  );
}
