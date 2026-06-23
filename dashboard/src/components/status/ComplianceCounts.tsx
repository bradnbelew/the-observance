import type { ComplianceCountsView } from "@/lib/database.types";

/**
 * Spoiler-free compliance counts.
 *
 * Reads the `v_compliance_counts` view — a NEUTRAL aggregate of total tracked
 * records and total flags across ALL players. There are deliberately NO player
 * names and NO custom_key labels here; the view exposes two integers only, so
 * nothing about the story's custom system can leak through this surface.
 */

function Stat({
  label,
  value,
  hint,
}: {
  label: string;
  value: number;
  hint: string;
}) {
  return (
    <div className="rounded-md border border-neutral-800 bg-ash/40 px-4 py-3">
      <div className="font-mono text-3xl tabular-nums text-neutral-100">
        {value.toLocaleString()}
      </div>
      <div className="mt-1 text-xs uppercase tracking-wide text-neutral-500">
        {label}
      </div>
      <div className="mt-1 text-[11px] text-neutral-600">{hint}</div>
    </div>
  );
}

export default function ComplianceCounts({
  counts,
}: {
  counts: ComplianceCountsView | null;
}) {
  const totalRecords = counts?.total_records ?? 0;
  const totalFlags = counts?.total_flags ?? 0;
  const flagRate =
    totalRecords > 0 ? Math.round((totalFlags / totalRecords) * 100) : 0;

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-sm uppercase tracking-wide text-neutral-300">
          Compliance
        </h2>
        <span className="font-mono text-xs text-neutral-500">
          {flagRate}% flagged
        </span>
      </div>

      <div className="mt-4 grid grid-cols-2 gap-3">
        <Stat
          label="Records"
          value={totalRecords}
          hint="Total tracked across all players"
        />
        <Stat
          label="Flags"
          value={totalFlags}
          hint="Total across all records"
        />
      </div>

      <p className="mt-3 text-[11px] text-neutral-600">
        Neutral aggregate only — no identities, no labels.
      </p>
    </section>
  );
}
