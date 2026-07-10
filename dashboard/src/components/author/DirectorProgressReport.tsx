import type { AnswerAttempt, Hint, Player, Puzzle, Solve } from "@/lib/database.types";

type DirectorProgressReportProps = {
  flags: Record<string, unknown>;
  players: Player[];
  puzzles: Puzzle[];
  solves: Solve[];
  attempts: AnswerAttempt[];
  hints: Hint[];
};

function truthy(value: unknown) {
  if (value === null || value === undefined || value === false) return false;
  if (typeof value === "number") return value !== 0 && !Number.isNaN(value);
  if (typeof value === "string") return value.length > 0;
  return true;
}

function flagObject(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return value as Record<string, unknown>;
}

function gateOpen(puzzle: Puzzle, flags: Record<string, unknown>) {
  const requires = flagObject(puzzle.requires_flags);
  return Object.keys(requires).every((key) => truthy(flags[key]));
}

function compact(text: string | null | undefined, max = 96) {
  const clean = (text ?? "").replace(/\s+/g, " ").trim();
  if (!clean) return "(blank)";
  return clean.length <= max ? clean : `${clean.slice(0, max - 3)}...`;
}

function when(value: string | null | undefined) {
  if (!value) return "time unknown";
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) return value;
  return date.toISOString().slice(0, 19).replace("T", " ");
}

function shortKey(key: string) {
  return key.length <= 42 ? key : `${key.slice(0, 39)}...`;
}

export function DirectorProgressReport({
  flags,
  players,
  puzzles,
  solves,
  attempts,
  hints,
}: DirectorProgressReportProps) {
  const playerById = new Map(players.map((player) => [player.id, player]));
  const playerByUuid = new Map(players.map((player) => [player.mc_uuid, player]));
  const solvedKeys = new Set(solves.map((solve) => solve.puzzle_key));
  const missAttempts = attempts.filter((attempt) => !attempt.matched);

  const solvesByPlayer = new Map<string, number>();
  for (const solve of solves) {
    solvesByPlayer.set(solve.player_id, (solvesByPlayer.get(solve.player_id) ?? 0) + 1);
  }

  const missesByPlayer = new Map<string, number>();
  const missesByPuzzle = new Map<string, number>();
  for (const attempt of missAttempts) {
    const playerKey = attempt.player_id ?? attempt.mc_uuid ?? attempt.discord_id ?? "unknown";
    missesByPlayer.set(playerKey, (missesByPlayer.get(playerKey) ?? 0) + 1);
    if (attempt.puzzle_key) {
      missesByPuzzle.set(
        attempt.puzzle_key,
        (missesByPuzzle.get(attempt.puzzle_key) ?? 0) + 1,
      );
    }
  }

  const hintsByPuzzle = new Map<string, Hint[]>();
  for (const hint of hints) {
    const rows = hintsByPuzzle.get(hint.puzzle_key) ?? [];
    rows.push(hint);
    hintsByPuzzle.set(hint.puzzle_key, rows);
  }
  for (const rows of hintsByPuzzle.values()) {
    rows.sort((a, b) => a.tier - b.tier);
  }

  const openUnsolved = puzzles
    .filter((puzzle) => puzzle.active && gateOpen(puzzle, flags))
    .filter((puzzle) => !solvedKeys.has(puzzle.puzzle_key))
    .sort((a, b) => a.movement - b.movement || a.puzzle_key.localeCompare(b.puzzle_key));

  const stuckCandidates = openUnsolved
    .map((puzzle) => ({
      puzzle,
      hints: hintsByPuzzle.get(puzzle.puzzle_key) ?? [],
      misses: missesByPuzzle.get(puzzle.puzzle_key) ?? 0,
    }))
    .filter((row) => row.hints.length > 0)
    .sort((a, b) => b.misses - a.misses || a.puzzle.movement - b.puzzle.movement)
    .slice(0, 8);

  const recentSolves = solves.slice(0, 6);
  const recentMisses = missAttempts.slice(0, 6);

  const nextMove =
    missAttempts.length >= 6 && stuckCandidates.length > 0
      ? "Observe the current solve path before intervening; if the surface is fair, let authored hints breathe."
      : recentSolves.length === 0
        ? "Verify Hold, first report, and Rosetta are reachable before adding more late-game pressure."
        : "Keep watching pacing; use /obs director state for the next story lead.";

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs uppercase text-neutral-500">
            Director progress
          </p>
          <h2 className="mt-1 font-mono text-lg text-neutral-100">
            Player pace and stuck hints
          </h2>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          mirrors /obs director progress
        </div>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-4">
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Recent solves</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{solves.length}</p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Recent misses</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{missAttempts.length}</p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Open unsolved</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{openUnsolved.length}</p>
        </div>
        <div className="rounded-md border border-neutral-800 bg-ash p-3">
          <p className="font-mono text-[11px] uppercase text-neutral-500">Hinted candidates</p>
          <p className="mt-1 font-mono text-2xl text-neutral-100">{stuckCandidates.length}</p>
        </div>
      </div>

      <div className="mt-4 rounded-md border border-neutral-800 bg-black/20 p-4">
        <h3 className="font-mono text-sm text-neutral-100">Next operator move</h3>
        <code className="mt-3 block rounded-md border border-neutral-800 bg-black/35 px-3 py-2 font-mono text-xs text-neutral-200">
          {nextMove}
        </code>
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-2">
        <div className="rounded-md border border-neutral-800 bg-black/20 p-4">
          <h3 className="font-mono text-sm text-neutral-100">Stuck-player hint view</h3>
          <div className="mt-3 space-y-2">
            {stuckCandidates.length === 0 ? (
              <p className="text-sm text-neutral-400">
                No open unsolved puzzle with authored hints is visible under current flags.
              </p>
            ) : (
              stuckCandidates.map(({ puzzle, hints: rows, misses }) => {
                const hint = rows[0];
                return (
                  <div
                    key={puzzle.puzzle_key}
                    className="rounded border border-neutral-800 bg-ash px-3 py-2"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="font-mono text-xs text-neutral-200">
                        {shortKey(puzzle.puzzle_key)}
                      </p>
                      <p className="font-mono text-[11px] uppercase text-neutral-500">
                        M{puzzle.movement} · misses {misses} · tier {hint.tier}
                      </p>
                    </div>
                    <p className="mt-2 text-sm text-neutral-300">{compact(hint.body, 140)}</p>
                  </div>
                );
              })
            )}
          </div>
        </div>

        <div className="rounded-md border border-neutral-800 bg-black/20 p-4">
          <h3 className="font-mono text-sm text-neutral-100">Player progress summary</h3>
          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            {players.length === 0 ? (
              <p className="text-sm text-neutral-400">No players tracked yet.</p>
            ) : (
              players.map((player) => {
                const missKey = player.id;
                const uuidMissKey = player.mc_uuid;
                const misses =
                  (missesByPlayer.get(missKey) ?? 0) +
                  (missKey === uuidMissKey ? 0 : missesByPlayer.get(uuidMissKey) ?? 0);
                return (
                  <div
                    key={player.id}
                    className="rounded border border-neutral-800 bg-ash px-3 py-2"
                  >
                    <p className="font-mono text-xs text-neutral-200">{player.name}</p>
                    <p className="mt-1 text-sm text-neutral-400">
                      solves {solvesByPlayer.get(player.id) ?? 0} · misses {misses}
                    </p>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      <div className="mt-4 grid gap-4 xl:grid-cols-2">
        <div className="rounded-md border border-neutral-800 bg-black/20 p-4">
          <h3 className="font-mono text-sm text-neutral-100">Recent solves</h3>
          <div className="mt-3 space-y-2 text-sm text-neutral-300">
            {recentSolves.length === 0 ? (
              <p className="text-neutral-400">No recent solves in the dashboard window.</p>
            ) : (
              recentSolves.map((solve) => {
                const player = playerById.get(solve.player_id) ??
                  (solve.mc_uuid ? playerByUuid.get(solve.mc_uuid) : null);
                return (
                  <p key={`${solve.id}-${solve.puzzle_key}`}>
                    <span className="font-mono text-neutral-200">
                      {shortKey(solve.puzzle_key)}
                    </span>{" "}
                    by {player?.name ?? "unknown"} · {when(solve.solved_at)}
                  </p>
                );
              })
            )}
          </div>
        </div>

        <div className="rounded-md border border-neutral-800 bg-black/20 p-4">
          <h3 className="font-mono text-sm text-neutral-100">Recent wrong inputs</h3>
          <div className="mt-3 space-y-2 text-sm text-neutral-300">
            {recentMisses.length === 0 ? (
              <p className="text-neutral-400">No recent wrong inputs in the dashboard window.</p>
            ) : (
              recentMisses.map((attempt) => {
                const player = attempt.player_id
                  ? playerById.get(attempt.player_id)
                  : attempt.mc_uuid
                    ? playerByUuid.get(attempt.mc_uuid)
                    : null;
                return (
                  <p key={`${attempt.id}-${attempt.at}`}>
                    {player?.name ?? "unknown"} · {when(attempt.at)} ·{" "}
                    <span className="font-mono text-neutral-200">
                      {compact(attempt.normalized ?? attempt.raw, 72)}
                    </span>
                  </p>
                );
              })
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
