import type {
  CustomCompliance,
  Dossier,
  Player,
} from "@/lib/database.types";

export type DossierEntry = {
  player: Player;
  dossier: Dossier | null;
  compliance: CustomCompliance[];
};

const COMPLIANCE_STYLES: Record<string, string> = {
  keeping: "border-emerald-500/40 bg-emerald-500/10 text-emerald-300",
  warned: "border-amber-500/40 bg-amber-500/10 text-amber-300",
  violating: "border-red-500/40 bg-red-500/10 text-red-300",
  unknown: "border-neutral-600/40 bg-neutral-600/10 text-neutral-400",
};

function complianceStyle(status: string) {
  return COMPLIANCE_STYLES[status] ?? COMPLIANCE_STYLES.unknown;
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded border border-neutral-800 bg-ash px-2 py-1.5">
      <p className="font-mono text-[10px] uppercase tracking-wide text-neutral-500">
        {label}
      </p>
      <p className="font-mono text-sm text-neutral-100">{value}</p>
    </div>
  );
}

function fmtNum(n: number | null | undefined, digits = 2) {
  if (n === null || n === undefined) return "—";
  return Number.isInteger(n) ? String(n) : n.toFixed(digits);
}

/**
 * Dossiers — the full, NAMED spoiler view: each player with their measured
 * signals (the dossier the Signal Tracker writes) and their per-custom
 * compliance. This is the opposite of the status view's neutral counts; here
 * names and custom_key labels are shown deliberately.
 */
export function Dossiers({ entries }: { entries: DossierEntry[] }) {
  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg text-neutral-100">Dossiers</h2>
        <span className="font-mono text-xs text-neutral-500">
          {entries.length} {entries.length === 1 ? "player" : "players"}
        </span>
      </div>

      {entries.length === 0 ? (
        <p className="mt-4 text-sm text-neutral-500">No players tracked yet.</p>
      ) : (
        <ul className="mt-4 space-y-4">
          {entries.map(({ player, dossier, compliance }) => (
            <li
              key={player.id}
              className="rounded-md border border-neutral-800 bg-ash p-4"
            >
              <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                <span className="text-base text-neutral-100">{player.name}</span>
                <span className="font-mono text-[11px] text-neutral-600">
                  {player.mc_uuid}
                </span>
                <span className="ml-auto font-mono text-[11px] text-neutral-600">
                  last seen {new Date(player.last_seen).toLocaleString()}
                </span>
              </div>

              <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-6">
                <Stat label="Solo ratio" value={fmtNum(dossier?.solo_ratio)} />
                <Stat label="Deaths" value={fmtNum(dossier?.deaths, 0)} />
                <Stat
                  label="Blocks mined"
                  value={fmtNum(dossier?.blocks_mined, 0)}
                />
                <Stat
                  label="Grp distance"
                  value={fmtNum(dossier?.group_distance)}
                />
                <Stat
                  label="Sentiment"
                  value={fmtNum(dossier?.chat_sentiment)}
                />
                <Stat
                  label="Updated"
                  value={
                    dossier?.updated_at
                      ? new Date(dossier.updated_at).toLocaleDateString()
                      : "—"
                  }
                />
              </div>

              {dossier?.hoard_summary ? (
                <p className="mt-2 text-sm text-neutral-400">
                  <span className="font-mono text-[11px] uppercase tracking-wide text-neutral-600">
                    hoard&nbsp;
                  </span>
                  {dossier.hoard_summary}
                </p>
              ) : null}

              <div className="mt-3">
                <h3 className="mb-1.5 font-mono text-[11px] uppercase tracking-wide text-neutral-500">
                  Customs
                </h3>
                {compliance.length === 0 ? (
                  <p className="text-sm text-neutral-600">
                    No customs observed.
                  </p>
                ) : (
                  <div className="flex flex-wrap gap-2">
                    {compliance.map((c) => (
                      <span
                        key={c.id}
                        className={`rounded-md border px-2 py-1 font-mono text-[11px] ${complianceStyle(
                          c.status,
                        )}`}
                        title={
                          c.last_observed
                            ? `last observed ${new Date(
                                c.last_observed,
                              ).toLocaleString()}`
                            : "never observed"
                        }
                      >
                        {c.custom_key}
                        <span className="ml-1 opacity-70">
                          {c.status} · {c.violation_count}×
                        </span>
                      </span>
                    ))}
                  </div>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
