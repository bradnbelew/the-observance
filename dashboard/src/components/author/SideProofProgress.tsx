type FlagMap = Record<string, unknown>;

const PROOFS = [
  { key: "site_seen_school_stand", label: "School stand" },
  { key: "site_seen_markers_row", label: "Markers row" },
  { key: "site_seen_cistern_7", label: "Cistern 7" },
  { key: "site_seen_watch_floor", label: "Watch floor" },
  { key: "site_seen_set_apart_shelf", label: "Entry five shelf" },
  { key: "site_seen_undercroft_seal", label: "Undercroft seal" },
  { key: "site_seen_forgotten_mouth", label: "Forgotten Mouth" },
  { key: "site_seen_deep_market", label: "Deep Market" },
  { key: "site_seen_ration_table", label: "Ration table" },
  { key: "site_seen_third_bay_breach", label: "Third bay" },
  { key: "site_seen_warm_town_collapse", label: "Warm-town collapse" },
  { key: "site_seen_deep_bird_coops", label: "Bird coops" },
  { key: "npc_wenna_crust_done", label: "Wenna crust" },
  { key: "npc_coll_lamp_done", label: "Coll lamp" },
];

function truthy(v: unknown): boolean {
  return v === true || v === "true" || v === 1 || v === "1";
}

export function SideProofProgress({ flags }: { flags: FlagMap }) {
  const filed = PROOFS.filter((proof) => truthy(flags[proof.key])).length;

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-mono text-lg text-neutral-100">Side Proof</h2>
          <p className="mt-1 text-sm text-neutral-400">
            {filed}/{PROOFS.length} filed into the record
          </p>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          {filed === PROOFS.length ? "complete" : "required before Accepting"}
        </div>
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
        {PROOFS.map((proof) => {
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
