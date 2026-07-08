type FlagMap = Record<string, unknown>;

const KEEPERS = [
  { key: "vaun_theory", label: "Vaun" },
  { key: "mara_theory", label: "Mara" },
  { key: "sella_theory", label: "Sella" },
  { key: "orin_theory", label: "Orin" },
  { key: "brann_theory", label: "Brann" },
  { key: "iss_theory", label: "Iss" },
];

function truthy(v: unknown): boolean {
  return v === true || v === "true" || v === 1 || v === "1";
}

export function KeeperTheoryProgress({ flags }: { flags: FlagMap }) {
  const received = KEEPERS.filter((keeper) => truthy(flags[keeper.key])).length;

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-mono text-lg text-neutral-100">
            Keeper Theories
          </h2>
          <p className="mt-1 text-sm text-neutral-400">
            {received}/{KEEPERS.length} received by the record
          </p>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          {received === KEEPERS.length ? "complete" : "required before Accepting"}
        </div>
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {KEEPERS.map((keeper) => {
          const seen = truthy(flags[keeper.key]);
          return (
            <div
              key={keeper.key}
              className="flex items-center justify-between rounded-md border border-neutral-800 bg-ash px-3 py-2"
            >
              <span className="text-sm text-neutral-300">{keeper.label}</span>
              <span
                className={
                  seen
                    ? "font-mono text-xs text-emerald-300"
                    : "font-mono text-xs text-neutral-600"
                }
              >
                {seen ? "received" : "needed"}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
