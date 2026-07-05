import { approveBeat, skipBeat } from "@/app/author/actions";
import type { Beat, BeatStatus } from "@/lib/database.types";

const STATUS_STYLES: Record<BeatStatus, string> = {
  pending: "border-amber-500/40 bg-amber-500/10 text-amber-300",
  approved: "border-sky-500/40 bg-sky-500/10 text-sky-300",
  fired: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  skipped: "border-neutral-600/40 bg-neutral-600/10 text-neutral-400",
};

function StatusPill({ status }: { status: BeatStatus }) {
  return (
    <span
      className={`rounded-full border px-2 py-0.5 font-mono text-[11px] uppercase tracking-wide ${STATUS_STYLES[status]}`}
    >
      {status}
    </span>
  );
}

/**
 * Beat queue — the anti-jank approval gate. Lists every queued beat with its
 * payload preview, and (for pending beats) approve / force / skip buttons that
 * each post to a server action setting status + decided_at.
 */
export function BeatQueue({ beats }: { beats: Beat[] }) {
  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg text-neutral-100">Beat queue</h2>
        <span className="font-mono text-xs text-neutral-500">
          {beats.length} {beats.length === 1 ? "beat" : "beats"}
        </span>
      </div>

      {beats.length === 0 ? (
        <p className="mt-4 text-sm text-neutral-500">The queue is empty.</p>
      ) : (
        <ul className="mt-4 space-y-3">
          {beats.map((beat) => {
            const pending = beat.status === "pending";
            return (
              <li
                key={beat.id}
                className="rounded-md border border-neutral-800 bg-ash p-3"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-mono text-sm text-neutral-100">
                    {beat.type}
                  </span>
                  {beat.target ? (
                    <span className="font-mono text-xs text-neutral-500">
                      → {beat.target}
                    </span>
                  ) : null}
                  <span className="ml-auto">
                    <StatusPill status={beat.status} />
                  </span>
                </div>

                <pre className="mt-2 overflow-x-auto rounded bg-black/30 p-2 font-mono text-[11px] text-neutral-400">
                  {JSON.stringify(beat.payload, null, 2)}
                </pre>

                <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
                  <span className="font-mono text-[11px] text-neutral-600">
                    #{beat.id} · queued{" "}
                    {new Date(beat.created_at).toLocaleString()}
                    {beat.decided_at
                      ? ` · decided ${new Date(beat.decided_at).toLocaleString()}`
                      : ""}
                  </span>

                  {pending ? (
                    <div className="flex items-center gap-2">
                      <form action={async (fd: FormData) => { await approveBeat(fd); }}>
                        <input type="hidden" name="id" value={beat.id} />
                        <button
                          type="submit"
                          className="rounded-md border border-sky-700/60 bg-sky-900/30 px-2.5 py-1 text-xs text-sky-200 transition-colors hover:bg-sky-900/50"
                        >
                          Approve
                        </button>
                      </form>
                      <form action={async (fd: FormData) => { await skipBeat(fd); }}>
                        <input type="hidden" name="id" value={beat.id} />
                        <button
                          type="submit"
                          className="rounded-md border border-neutral-700 bg-neutral-800 px-2.5 py-1 text-xs text-neutral-300 transition-colors hover:bg-neutral-700"
                        >
                          Skip
                        </button>
                      </form>
                    </div>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
