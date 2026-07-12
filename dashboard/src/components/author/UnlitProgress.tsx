type FlagMap = Record<string, unknown>;

const HOUSES = [
  { key: "unlit_seen_lamp", label: "Lamp house", required: true },
  { key: "unlit_seen_cairn", label: "Cairn house", required: true },
  { key: "unlit_seen_coop", label: "Coop house", required: true },
  { key: "unlit_seen_well", label: "Well house", required: true },
  { key: "unlit_seen_watch", label: "Watch house", required: true },
  { key: "unlit_seen_warm", label: "Warm house", required: true },
  { key: "unlit_seen_threshold", label: "Threshold house", required: true },
  { key: "unlit_seen_base", label: "Base house", required: true },
];

function truthy(v: unknown): boolean {
  return v === true || v === "true" || v === 1 || v === "1";
}

function formatStamp(v: unknown): string {
  if (typeof v !== "string" || v.length === 0) return "none";
  const date = new Date(v);
  return Number.isFinite(date.getTime()) ? date.toLocaleString() : v;
}

export function UnlitProgress({ flags }: { flags: FlagMap }) {
  const found = HOUSES.filter((h) => truthy(flags[h.key])).length;
  const requiredFound = HOUSES.filter((h) => h.required && truthy(flags[h.key])).length;
  const requiredTotal = HOUSES.filter((h) => h.required).length;

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-mono text-lg text-neutral-100">Unlit</h2>
          <p className="mt-1 text-sm text-neutral-400">
            {found}/{HOUSES.length} house discoveries recorded; {requiredFound}/{requiredTotal} required houses
          </p>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          {truthy(flags.unlit_open) ? "opened" : "sealed"}
        </div>
      </div>

      <div className="mt-4 grid gap-3 sm:grid-cols-3">
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-xs uppercase tracking-wide text-neutral-500">
            Last Entry
          </p>
          <p className="mt-1 text-sm text-neutral-200">
            {formatStamp(flags.unlit_last_entry)}
          </p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-xs uppercase tracking-wide text-neutral-500">
            Last Exit
          </p>
          <p className="mt-1 text-sm text-neutral-200">
            {formatStamp(flags.unlit_last_exit)}
          </p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-xs uppercase tracking-wide text-neutral-500">
            Mode
          </p>
          <p className="mt-1 text-sm text-neutral-200">non-linear</p>
        </div>
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        {HOUSES.map((house) => {
          const seen = truthy(flags[house.key]);
          return (
            <div
              key={house.key}
              className="flex items-center justify-between rounded-md border border-neutral-800 bg-ash px-3 py-2"
            >
              <span className="text-sm text-neutral-300">{house.label}</span>
              <span
                className={
                  seen
                    ? "font-mono text-xs text-emerald-300"
                    : "font-mono text-xs text-neutral-600"
                }
              >
                {seen ? "found" : house.required ? "needed" : "dark"}
              </span>
            </div>
          );
        })}
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-3">
        {[
          { key: "unlit_figure_seen", label: "Figure witnessed" },
          { key: "unlit_light_taken", label: "Light taken" },
          { key: "unlit_figure_hunt", label: "Hunt reached" },
        ].map((signal) => {
          const seen = truthy(flags[signal.key]);
          return (
            <div
              key={signal.key}
              className="flex items-center justify-between rounded-md border border-neutral-800 bg-black/20 px-3 py-2"
            >
              <span className="text-xs text-neutral-400">{signal.label}</span>
              <span className={seen ? "font-mono text-xs text-amber-300" : "font-mono text-xs text-neutral-700"}>
                {seen ? "yes" : "no"}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
