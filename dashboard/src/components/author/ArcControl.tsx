import { advanceArc, rewindArc } from "@/app/author/actions";
import type { ArcState, Json } from "@/lib/database.types";

const ACT_NAMES: Record<number, string> = {
  1: "Establishment",
  2: "The Ways",
  3: "The Accepting",
};

/**
 * Render a JSONB object (gates / flags) as a compact key→value list. Anything
 * that isn't a plain object falls back to a code dump.
 */
function JsonEntries({ value }: { value: Json }) {
  if (value && typeof value === "object" && !Array.isArray(value)) {
    const entries = Object.entries(value as Record<string, Json>);
    if (entries.length === 0) {
      return <p className="text-sm text-neutral-600">— none —</p>;
    }
    return (
      <ul className="space-y-1">
        {entries.map(([k, v]) => (
          <li
            key={k}
            className="flex items-baseline justify-between gap-3 font-mono text-xs"
          >
            <span className="text-neutral-400">{k}</span>
            <span className="text-neutral-200">{JSON.stringify(v)}</span>
          </li>
        ))}
      </ul>
    );
  }
  return (
    <pre className="overflow-x-auto font-mono text-xs text-neutral-300">
      {JSON.stringify(value, null, 2)}
    </pre>
  );
}

/**
 * Arc control — shows the current act and its gates/flags, with server-action
 * buttons to advance or rewind. The act number is carried in a hidden field so
 * the action needs no extra read.
 */
export function ArcControl({ arc }: { arc: ArcState | null }) {
  const current = arc?.current_act ?? 1;
  const name = ACT_NAMES[current] ?? "Unknown";

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg text-neutral-100">Arc</h2>
        <span className="font-mono text-xs text-neutral-500">arc_state</span>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-4">
        <div>
          <p className="font-mono text-3xl text-neutral-100">Act {current}</p>
          <p className="text-sm text-neutral-400">{name}</p>
        </div>

        <div className="ml-auto flex items-center gap-2">
          <form action={rewindArc}>
            <input type="hidden" name="current_act" value={current} />
            <button
              type="submit"
              disabled={current <= 1}
              className="rounded-md border border-neutral-700 bg-neutral-800 px-3 py-1.5 text-sm text-neutral-100 transition-colors hover:bg-neutral-700 disabled:cursor-not-allowed disabled:opacity-40"
            >
              ← Rewind
            </button>
          </form>
          <form action={advanceArc}>
            <input type="hidden" name="current_act" value={current} />
            <button
              type="submit"
              disabled={current >= 3}
              className="rounded-md border border-neutral-700 bg-neutral-800 px-3 py-1.5 text-sm text-neutral-100 transition-colors hover:bg-neutral-700 disabled:cursor-not-allowed disabled:opacity-40"
            >
              Advance →
            </button>
          </form>
        </div>
      </div>

      <div className="mt-5 grid gap-4 sm:grid-cols-2">
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <h3 className="mb-2 font-mono text-xs uppercase tracking-wide text-neutral-500">
            Gates
          </h3>
          <JsonEntries value={arc?.gates ?? {}} />
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <h3 className="mb-2 font-mono text-xs uppercase tracking-wide text-neutral-500">
            Flags
          </h3>
          <JsonEntries value={arc?.flags ?? {}} />
        </div>
      </div>

      {arc?.updated_at ? (
        <p className="mt-3 font-mono text-xs text-neutral-600">
          updated {new Date(arc.updated_at).toLocaleString()}
        </p>
      ) : null}
    </section>
  );
}
