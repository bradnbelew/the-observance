"use client";

import { useState, useTransition } from "react";
import { setWatcherSleep } from "@/app/author/actions";

/**
 * Watcher-sleep toggle: manual mode for a sensitive session.
 */
export function WatcherSleepToggle({ asleep }: { asleep: boolean }) {
  const [isAsleep, setIsAsleep] = useState(asleep);
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  function toggle() {
    const next = !isAsleep;
    setError(null);
    setIsAsleep(next);
    startTransition(async () => {
      const res = await setWatcherSleep(next);
      if (!res.ok) {
        setIsAsleep(!next);
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
              ? "Manual mode is on. All automatic beats are muted."
              : "Automatic mode is on. Approved beats fire on the normal cadence."}
          </p>
        </div>

        <div className="flex items-center gap-3">
          <span className="font-mono text-xs uppercase tracking-wide text-neutral-500">
            {isAsleep ? "Manual" : "Auto"}
          </span>
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
      </div>

      {error ? <p className="mt-2 text-sm text-red-400">{error}</p> : null}
    </section>
  );
}
