import { SIDE_PROOF_FLAGS } from "./proofFlags";

type FlagMap = Record<string, unknown>;

function truthy(v: unknown): boolean {
  return v === true || v === "true" || v === 1 || v === "1";
}

export function SideProofProgress({ flags }: { flags: FlagMap }) {
  const filed = SIDE_PROOF_FLAGS.filter((proof) => truthy(flags[proof.key])).length;

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-mono text-lg text-neutral-100">Side Proof</h2>
          <p className="mt-1 text-sm text-neutral-400">
            {filed}/{SIDE_PROOF_FLAGS.length} filed into the record
          </p>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          {filed === SIDE_PROOF_FLAGS.length ? "complete" : "required before Accepting"}
        </div>
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        {SIDE_PROOF_FLAGS.map((proof) => {
          const seen = truthy(flags[proof.key]);
          return (
            <div
              key={proof.key}
              className="flex items-center justify-between rounded-md border border-neutral-800 bg-ash px-3 py-2"
            >
              <span className="text-sm text-neutral-300">{proof.label}</span>
              <span
                className={
                  seen
                    ? "font-mono text-xs text-emerald-300"
                    : "font-mono text-xs text-neutral-600"
                }
              >
                {seen ? "filed" : "needed"}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
