import type { BondLedger as BondLedgerRow, Player } from "@/lib/database.types";

export type BondLedgerEntry = BondLedgerRow & {
  player: Pick<Player, "id" | "name"> | null;
};

/**
 * Bond ledger — the secret driver of the Act-3 casting. Rows arrive sorted by
 * bond_points desc (highest bond = current "front-runner" for who is kept). The
 * leader is highlighted; this is a spoiler-only surface, so it lives in Author
 * mode and is never exposed to the status view.
 */
export function BondLedger({ rows }: { rows: BondLedgerEntry[] }) {
  const leaderPoints = rows.length > 0 ? rows[0].bond_points : null;

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg text-neutral-100">Bond ledger</h2>
        <span className="font-mono text-xs text-neutral-500">casting</span>
      </div>
      <p className="mt-1 text-sm text-neutral-400">
        Highest bond is the current front-runner for the Accepting.
      </p>

      {rows.length === 0 ? (
        <p className="mt-4 text-sm text-neutral-500">No bonds recorded yet.</p>
      ) : (
        <ol className="mt-4 space-y-2">
          {rows.map((row, i) => {
            // A leader exists only if someone is strictly ahead of the field, or
            // is the sole non-zero entry; ties at the top get no crown.
            const isLeader =
              i === 0 &&
              leaderPoints !== null &&
              leaderPoints > 0 &&
              (rows.length === 1 || rows[1].bond_points < leaderPoints);

            return (
              <li
                key={row.player_id}
                className={`flex items-center gap-3 rounded-md border p-3 ${
                  isLeader
                    ? "border-amber-500/50 bg-amber-500/10"
                    : "border-neutral-800 bg-ash"
                }`}
              >
                <span className="w-6 font-mono text-sm text-neutral-500">
                  {i + 1}
                </span>
                <span className="text-sm text-neutral-100">
                  {row.player?.name ?? (
                    <span className="text-neutral-600">unknown</span>
                  )}
                </span>
                {isLeader ? (
                  <span className="rounded-full border border-amber-500/50 bg-amber-500/10 px-2 py-0.5 font-mono text-[11px] uppercase tracking-wide text-amber-300">
                    front-runner
                  </span>
                ) : null}
                <span
                  className={`ml-auto font-mono text-sm ${
                    isLeader ? "text-amber-200" : "text-neutral-300"
                  }`}
                >
                  {row.bond_points}
                </span>
              </li>
            );
          })}
        </ol>
      )}
    </section>
  );
}
