"use client";

import { useState, useTransition } from "react";
import { setWatcherSleep } from "@/app/author/actions";

/**
 * Watcher-sleep toggle — mute every beat for a sensitive session.
 *
 * Client component so the switch reflects pending state immediately. It calls
 * the setWatcherSleep server action (which re-checks isAdmin) and reflects the
 * authoritative result; on error it reverts the optimistic flip.
 */
export function WatcherSleepToggle({ asleep }: { asleep: boolean }) {
  const [isAsleep, setIsAsleep] = useState(asleep);
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  function toggle() {
    const next = !isAsleep;
    setError(null);
    setIsAsleep(next); // optimistic
    startTransition(async () => {
      const res = await setWatcherSleep(next);
      if (!res.ok) {
        setIsAsleep(!next); // revert
        setError(res.error ?? "Failed to update.");
      }
    });
  }

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h2 className="font-mono text-lg text-neutral-100">Watcher sleep</h2>
          <p className="mt-1 max-w-prose text-sm text-neutral-400">
            {isAsleep
              ? "The Watcher is asleep — all beats are muted."
              : "The Watcher is awake — beats fire on the normal cadence."}
          </p>
        </div>

        <button
          type="button"
          role="switch"
          aria-checked={isAsleep}
          onClick={toggle}
          disabled={pending}
          className={`relative inline-flex h-7 w-12 shrink-0 items-center rounded-full border transition-colors disabled:opacity-50 ${
            isAsleep
              ? "border-amber-500/60 bg-amber-500/30"
              : "border-neutral-700 bg-neutral-800"
          }`}
        >
          <span
            className={`inline-block h-5 w-5 transform rounded-full bg-neutral-100 transition-transform ${
              isAsleep ? "translate-x-6" : "translate-x-1"
            }`}
          />
        </button>
      </div>

      {error ? <p className="mt-2 text-sm text-red-400">{error}</p> : null}
    </section>
  );
}
