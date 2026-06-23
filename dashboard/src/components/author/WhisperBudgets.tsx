import { updateWhisperBudget } from "@/app/author/actions";
import type { Player, WhisperBudget } from "@/lib/database.types";

export type WhisperBudgetRow = WhisperBudget & {
  player: Pick<Player, "id" | "name"> | null;
};

/**
 * Whisper budgets — the hint economy, editable per player/act. Each row is its
 * own form posting budget/spent/earned to the server action. "remaining" is
 * derived (budget + earned − spent) for at-a-glance read, matching the ledger
 * the Discord bot checks before granting a whisper.
 */
export function WhisperBudgets({ rows }: { rows: WhisperBudgetRow[] }) {
  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-lg text-neutral-100">Whisper budgets</h2>
        <span className="font-mono text-xs text-neutral-500">
          {rows.length} {rows.length === 1 ? "row" : "rows"}
        </span>
      </div>

      {rows.length === 0 ? (
        <p className="mt-4 text-sm text-neutral-500">
          No whisper budgets yet. They are seeded per player as each Act opens.
        </p>
      ) : (
        <div className="mt-4 overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left font-mono text-[11px] uppercase tracking-wide text-neutral-500">
                <th className="px-2 py-1">Player</th>
                <th className="px-2 py-1">Act</th>
                <th className="px-2 py-1">Budget</th>
                <th className="px-2 py-1">Spent</th>
                <th className="px-2 py-1">Earned</th>
                <th className="px-2 py-1">Left</th>
                <th className="px-2 py-1" />
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => {
                const remaining =
                  (row.budget ?? 0) + (row.earned ?? 0) - (row.spent ?? 0);
                return (
                  <tr
                    key={row.id}
                    className="border-t border-neutral-800 align-middle"
                  >
                    <td className="px-2 py-2 text-neutral-200">
                      {row.player?.name ?? (
                        <span className="text-neutral-600">unknown</span>
                      )}
                    </td>
                    <td className="px-2 py-2 font-mono text-neutral-400">
                      {row.act}
                    </td>
                    <td colSpan={3} className="px-0 py-1">
                      <form
                        action={updateWhisperBudget}
                        className="flex items-center gap-2"
                      >
                        <input type="hidden" name="id" value={row.id} />
                        <input
                          name="budget"
                          type="number"
                          min={0}
                          defaultValue={row.budget}
                          aria-label="Budget"
                          className="w-16 rounded border border-neutral-800 bg-ash px-2 py-1 font-mono text-xs text-neutral-100 outline-none focus:border-neutral-600"
                        />
                        <input
                          name="spent"
                          type="number"
                          min={0}
                          defaultValue={row.spent}
                          aria-label="Spent"
                          className="w-16 rounded border border-neutral-800 bg-ash px-2 py-1 font-mono text-xs text-neutral-100 outline-none focus:border-neutral-600"
                        />
                        <input
                          name="earned"
                          type="number"
                          min={0}
                          defaultValue={row.earned}
                          aria-label="Earned"
                          className="w-16 rounded border border-neutral-800 bg-ash px-2 py-1 font-mono text-xs text-neutral-100 outline-none focus:border-neutral-600"
                        />
                        <button
                          type="submit"
                          className="rounded-md border border-neutral-700 bg-neutral-800 px-2.5 py-1 text-xs text-neutral-100 transition-colors hover:bg-neutral-700"
                        >
                          Save
                        </button>
                      </form>
                    </td>
                    <td
                      className={`px-2 py-2 font-mono ${
                        remaining > 0 ? "text-emerald-300" : "text-neutral-600"
                      }`}
                    >
                      {remaining}
                    </td>
                    <td />
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
