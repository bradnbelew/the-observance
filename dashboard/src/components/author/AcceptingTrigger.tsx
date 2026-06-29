"use client";

import { useState, useTransition } from "react";
import { triggerAccepting } from "@/app/author/actions";

const CONFIRM_PHRASE = "ACCEPTING";

/**
 * The Accepting — a guarded manual trigger for the Act-3 ritual (testing only).
 *
 * Two-step: the admin must type the confirmation phrase before the button
 * enables, then it posts to the triggerAccepting server action which inserts a
 * pending beat_queue row of type 'trigger_accepting'. The action re-checks the
 * phrase server-side too, so the guard isn't merely cosmetic.
 *
 * When the Accepting resolves, the engine's fate sentinel sets the ending fate
 * (the divergent colorant) set-once — preview it, or force one for testing,
 * above in the Ending selector. This trigger only fires the rite; it elects no
 * fate and no player.
 */
export function AcceptingTrigger() {
  const [phrase, setPhrase] = useState("");
  const [result, setResult] = useState<{ ok: boolean; msg: string } | null>(
    null,
  );
  const [pending, startTransition] = useTransition();

  const armed = phrase === CONFIRM_PHRASE;

  function fire() {
    if (!armed) return;
    setResult(null);
    const formData = new FormData();
    formData.set("confirm", phrase);
    startTransition(async () => {
      const res = await triggerAccepting(formData);
      if (res.ok) {
        setResult({ ok: true, msg: "The Accepting was queued as a pending beat." });
        setPhrase("");
      } else {
        setResult({ ok: false, msg: res.error ?? "Failed to queue." });
      }
    });
  }

  return (
    <section className="rounded-lg border border-red-900/40 bg-red-950/20 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg text-red-200">Trigger the Accepting</h2>
        <span className="font-mono text-xs text-red-400/70">danger</span>
      </div>
      <p className="mt-1 max-w-prose text-sm text-neutral-400">
        Manually queue the Act-3 ritual for testing. It lands as a{" "}
        <span className="font-mono">pending</span> beat and still flows through
        the approval gate. The ending fate it resolves to is shown in the Ending
        selector above. Type{" "}
        <span className="font-mono text-red-300">{CONFIRM_PHRASE}</span> to arm.
      </p>

      <div className="mt-4 flex flex-wrap items-center gap-2">
        <input
          value={phrase}
          onChange={(e) => setPhrase(e.target.value)}
          placeholder={CONFIRM_PHRASE}
          aria-label="Confirmation phrase"
          className="w-44 rounded-md border border-neutral-800 bg-ash px-3 py-2 font-mono text-sm text-neutral-100 outline-none focus:border-red-700"
        />
        <button
          type="button"
          onClick={fire}
          disabled={!armed || pending}
          className="rounded-md border border-red-700 bg-red-900/40 px-3 py-2 text-sm text-red-100 transition-colors hover:bg-red-900/60 disabled:cursor-not-allowed disabled:opacity-40"
        >
          {pending ? "Queuing…" : "Trigger the Accepting"}
        </button>
      </div>

      {result ? (
        <p
          className={`mt-2 text-sm ${
            result.ok ? "text-emerald-400" : "text-red-400"
          }`}
        >
          {result.msg}
        </p>
      ) : null}
    </section>
  );
}
