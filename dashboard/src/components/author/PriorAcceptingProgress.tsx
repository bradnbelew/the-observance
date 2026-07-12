type FlagMap = Record<string, unknown>;

const PRIOR_FLAGS = [
  { key: "prior_absence_known", label: "No witness" },
  { key: "prior_camp_read", label: "Camp refusal" },
  { key: "prior_vaun_corrected", label: "Vaun correction" },
  { key: "prior_mara_corrected", label: "Mara correction" },
  { key: "prior_sella_corrected", label: "Sella correction" },
  { key: "prior_orin_corrected", label: "Orin correction" },
  { key: "prior_brann_corrected", label: "Brann correction" },
  { key: "prior_iss_corrected", label: "Iss correction" },
  { key: "prior_witness_ready", label: "Witness before rite" },
] as const;

function truthy(v: unknown): boolean {
  return v === true || v === "true" || v === 1 || v === "1";
}

export function PriorAcceptingProgress({ flags }: { flags: FlagMap }) {
  const filed = PRIOR_FLAGS.filter((step) => truthy(flags[step.key])).length;
  const acceptingOpen =
    truthy(flags.prior_witness_ready) &&
    (truthy(flags.accepting_onramp_open) || truthy(flags.threshold_open));

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-mono text-lg text-neutral-100">Failed Accepting</h2>
          <p className="mt-1 text-sm text-neutral-400">
            {filed}/{PRIOR_FLAGS.length} prior-run files resolved
          </p>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          {acceptingOpen ? "last warm open" : "blocks rite-tokens"}
        </div>
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
        {PRIOR_FLAGS.map((step) => {
          const seen = truthy(flags[step.key]);
          return (
            <div
              key={step.key}
              className="flex items-center justify-between rounded-md border border-neutral-800 bg-ash px-3 py-2"
            >
              <span className="text-sm text-neutral-300">{step.label}</span>
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
