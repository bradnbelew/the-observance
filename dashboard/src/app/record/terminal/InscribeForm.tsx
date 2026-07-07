"use client";

import { useState } from "react";
import type { RecordOutcome } from "@/lib/record/resolve";

/**
 * InscribeForm — the "write answers INTO the record" client surface (INTEGRATION Layer 5).
 *
 * This is the ONLY client component in the record terminal, and it holds NO secrets: it POSTs
 * { name, answer } to the server route (/record/terminal/inscribe) and renders the neutral line the
 * server returns. All matching, normalizing, and DB access happen server-side; the browser learns only
 * the coarse outcome kind. There is no closeness signal, no puzzle identity, no answer echo.
 *
 * The register is the archive's: a lectern you inscribe a hand + a name into. Styled as a decayed
 * terminal input, not a web form.
 */
export function InscribeForm() {
  const [name, setName] = useState("");
  const [answer, setAnswer] = useState("");
  const [outcome, setOutcome] = useState<RecordOutcome | null>(null);
  const [line, setLine] = useState<string>("");
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setOutcome(null);
    setLine("");
    try {
      const res = await fetch("/record/terminal/inscribe", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ name, answer }),
      });
      const data = (await res.json()) as { outcome: RecordOutcome; line?: string };
      setOutcome(data.outcome);
      setLine(data.line ?? "");
      if (data.outcome === "kept") setAnswer(""); // a kept mark clears the field; the name persists.
    } catch {
      setOutcome("unresolved");
      setLine("the record is not reachable.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="mt-2 font-mono text-sm">
      <div className="border border-neutral-800 bg-black/40">
        <div className="flex items-center gap-2 border-b border-neutral-900 px-3 py-2 text-[10px] uppercase tracking-[0.3em] text-neutral-600">
          <span className="text-amber-700/70">▮</span>
          <span>inscribe — lectern//record.in</span>
        </div>

        <label className="flex items-baseline gap-3 border-b border-neutral-900 px-3 py-2">
          <span className="w-16 shrink-0 text-[11px] lowercase tracking-wide text-neutral-600">hand</span>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            autoComplete="off"
            spellCheck={false}
            placeholder="the name you are known by"
            className="w-full bg-transparent text-neutral-200 placeholder:text-neutral-700 focus:outline-none"
            maxLength={64}
          />
        </label>

        <label className="flex items-baseline gap-3 px-3 py-2">
          <span className="w-16 shrink-0 text-[11px] lowercase tracking-wide text-neutral-600">mark</span>
          <input
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            autoComplete="off"
            spellCheck={false}
            placeholder="what you would inscribe"
            className="w-full bg-transparent text-neutral-200 placeholder:text-neutral-700 focus:outline-none"
            maxLength={512}
          />
        </label>
      </div>

      <div className="mt-3 flex items-center gap-4">
        <button
          type="submit"
          disabled={busy || answer.trim() === "" || name.trim() === ""}
          className="border border-neutral-700 px-4 py-1.5 text-[11px] uppercase tracking-[0.3em] text-neutral-400 transition-colors hover:border-neutral-500 hover:text-neutral-200 disabled:cursor-not-allowed disabled:opacity-40"
        >
          {busy ? "reading…" : "inscribe"}
        </button>

        {outcome && (
          <span
            role="status"
            aria-live="polite"
            className={
              outcome === "kept"
                ? "text-[12px] text-emerald-500/80"
                : "text-[12px] text-neutral-500"
            }
          >
            {line}
          </span>
        )}
      </div>
    </form>
  );
}
