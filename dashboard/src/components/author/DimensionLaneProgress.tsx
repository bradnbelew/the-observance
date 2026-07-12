type FlagMap = Record<string, unknown>;

const DIMENSION_LANES = [
  {
    key: "nether",
    title: "Nether forge",
    storyFlag: "undercroft_open",
    storyLabel: "Undercroft opened",
    placementFlag: "nether_forge_placed",
    placementLabel: "Forge placed",
    payoffFlag: "nether_forge_found",
    payoffLabel: "Lent found",
    command: "/obs site set nether_forge, then /obs placeworld in the Nether",
  },
  {
    key: "end",
    title: "End seventh shrine",
    storyFlag: "seventh_named",
    storyLabel: "Seventh named",
    placementFlag: "end_seventh_shrine_placed",
    placementLabel: "Shrine placed",
    payoffFlag: "seventh_seen_out",
    payoffLabel: "Out-of-record read",
    command: "/obs site set end_seventh_shrine, then /obs placeworld in the End",
  },
] as const;

function truthy(value: unknown): boolean {
  return value === true || value === "true" || value === 1 || value === "1";
}

function Step({
  label,
  done,
}: {
  label: string;
  done: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-md border border-neutral-800 bg-ash px-3 py-2">
      <span className="text-sm text-neutral-300">{label}</span>
      <span
        className={
          done
            ? "font-mono text-xs text-emerald-300"
            : "font-mono text-xs text-neutral-600"
        }
      >
        {done ? "ready" : "closed"}
      </span>
    </div>
  );
}

export function DimensionLaneProgress({ flags }: { flags: FlagMap }) {
  const placed = DIMENSION_LANES.filter((lane) =>
    truthy(flags[lane.placementFlag]),
  ).length;
  const solved = DIMENSION_LANES.filter((lane) =>
    truthy(flags[lane.payoffFlag]),
  ).length;

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-mono text-lg text-neutral-100">Dimension Lanes</h2>
          <p className="mt-1 text-sm text-neutral-400">
            {placed}/{DIMENSION_LANES.length} placed, {solved}/{DIMENSION_LANES.length} payoffs found
          </p>
        </div>
        <div className="font-mono text-xs text-neutral-500">
          optional deepening
        </div>
      </div>

      <div className="mt-4 grid gap-3 lg:grid-cols-2">
        {DIMENSION_LANES.map((lane) => {
          const storyReady = truthy(flags[lane.storyFlag]);
          const lanePlaced = truthy(flags[lane.placementFlag]);
          const payoffSolved = truthy(flags[lane.payoffFlag]);
          return (
            <div
              key={lane.key}
              className="rounded-md border border-neutral-800 bg-black/20 p-3"
            >
              <div className="flex items-center justify-between gap-3">
                <h3 className="font-mono text-sm text-neutral-100">
                  {lane.title}
                </h3>
                <span
                  className={
                    storyReady && lanePlaced
                      ? "font-mono text-xs text-emerald-300"
                      : "font-mono text-xs text-amber-300"
                  }
                >
                  {storyReady && lanePlaced ? "openable" : "blocked"}
                </span>
              </div>
              <div className="mt-3 grid gap-2">
                <Step label={lane.storyLabel} done={storyReady} />
                <Step label={lane.placementLabel} done={lanePlaced} />
                <Step label={lane.payoffLabel} done={payoffSolved} />
              </div>
              {!lanePlaced ? (
                <code className="mt-3 block max-w-full break-all rounded-md border border-neutral-800 bg-black/35 px-3 py-2 font-mono text-xs text-neutral-300">
                  {lane.command}
                </code>
              ) : null}
            </div>
          );
        })}
      </div>
    </section>
  );
}
