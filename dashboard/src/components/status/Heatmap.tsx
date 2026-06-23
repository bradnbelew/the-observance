import type { HeatmapView } from "@/lib/database.types";

/**
 * Spoiler-free heatmap.
 *
 * Renders the `v_heatmap` view — pure spatial visit density per cell, grouped
 * by world. There is NO player identity, base ownership, or story label here:
 * the view exposes only (world, cell_x, cell_z, visits).
 *
 * Each world becomes a fixed grid spanning its min/max cell coordinates. Cell
 * opacity scales with visit count relative to that world's busiest cell.
 */

const MAX_GRID_SPAN = 48; // safety cap on rendered columns/rows per world

type WorldGroup = {
  world: string;
  cells: HeatmapView[];
  minX: number;
  maxX: number;
  minZ: number;
  maxZ: number;
  maxVisits: number;
};

function groupByWorld(cells: HeatmapView[]): WorldGroup[] {
  const byWorld = new Map<string, HeatmapView[]>();
  for (const cell of cells) {
    const list = byWorld.get(cell.world);
    if (list) list.push(cell);
    else byWorld.set(cell.world, [cell]);
  }

  const groups: WorldGroup[] = [];
  for (const [world, worldCells] of byWorld) {
    let minX = Infinity;
    let maxX = -Infinity;
    let minZ = Infinity;
    let maxZ = -Infinity;
    let maxVisits = 0;
    for (const c of worldCells) {
      if (c.cell_x < minX) minX = c.cell_x;
      if (c.cell_x > maxX) maxX = c.cell_x;
      if (c.cell_z < minZ) minZ = c.cell_z;
      if (c.cell_z > maxZ) maxZ = c.cell_z;
      if (c.visits > maxVisits) maxVisits = c.visits;
    }
    groups.push({ world, cells: worldCells, minX, maxX, minZ, maxZ, maxVisits });
  }

  // Busiest worlds first.
  groups.sort((a, b) => b.maxVisits - a.maxVisits);
  return groups;
}

/** Map a visit ratio (0..1) to a Tailwind opacity-via-rgba inline style. */
function heatColor(ratio: number): string {
  // Cold cyan → warm amber as density rises. Low floor so visited cells read.
  const clamped = Math.max(0, Math.min(1, ratio));
  const alpha = 0.12 + clamped * 0.88;
  // Interpolate hue from teal (180) to amber (38).
  const hue = 180 - clamped * 142;
  return `hsl(${hue.toFixed(0)} 80% 55% / ${alpha.toFixed(3)})`;
}

function WorldGrid({ group }: { group: WorldGroup }) {
  const spanX = group.maxX - group.minX + 1;
  const spanZ = group.maxZ - group.minZ + 1;

  // Clamp the rendered grid so a sparse-but-far-flung world can't explode.
  const cols = Math.min(spanX, MAX_GRID_SPAN);
  const rows = Math.min(spanZ, MAX_GRID_SPAN);

  // Index cells by their clamped grid position; keep the max visits per slot.
  const slot = new Map<string, number>();
  for (const c of group.cells) {
    const gx = Math.min(c.cell_x - group.minX, cols - 1);
    const gz = Math.min(c.cell_z - group.minZ, rows - 1);
    const key = `${gx}:${gz}`;
    slot.set(key, Math.max(slot.get(key) ?? 0, c.visits));
  }

  const tiles: React.ReactNode[] = [];
  for (let z = 0; z < rows; z++) {
    for (let x = 0; x < cols; x++) {
      const visits = slot.get(`${x}:${z}`) ?? 0;
      const ratio = group.maxVisits > 0 ? visits / group.maxVisits : 0;
      tiles.push(
        <div
          key={`${x}:${z}`}
          className="aspect-square rounded-[2px] border border-neutral-900/60"
          style={{
            backgroundColor:
              visits > 0 ? heatColor(ratio) : "rgba(255,255,255,0.02)",
          }}
          title={visits > 0 ? `${visits} visits` : undefined}
        />,
      );
    }
  }

  return (
    <div className="rounded-md border border-neutral-800 bg-ash/40 p-3">
      <div className="mb-2 flex items-center justify-between">
        <span className="font-mono text-xs text-neutral-300">
          {group.world}
        </span>
        <span className="font-mono text-[11px] text-neutral-500">
          {group.cells.length} cells · peak {group.maxVisits}
        </span>
      </div>
      <div
        className="grid gap-px"
        style={{ gridTemplateColumns: `repeat(${cols}, minmax(0, 1fr))` }}
      >
        {tiles}
      </div>
    </div>
  );
}

function Legend() {
  return (
    <div className="mt-3 flex items-center gap-2 text-[11px] text-neutral-500">
      <span>less</span>
      <div className="flex h-2 w-32 overflow-hidden rounded-full">
        {Array.from({ length: 12 }).map((_, i) => (
          <div
            key={i}
            className="flex-1"
            style={{ backgroundColor: heatColor(i / 11) }}
          />
        ))}
      </div>
      <span>more</span>
    </div>
  );
}

export default function Heatmap({ cells }: { cells: HeatmapView[] }) {
  const groups = groupByWorld(cells);

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-sm uppercase tracking-wide text-neutral-300">
          Traffic heatmap
        </h2>
        <span className="font-mono text-xs text-neutral-500">
          {groups.length} {groups.length === 1 ? "world" : "worlds"}
        </span>
      </div>

      {groups.length === 0 ? (
        <p className="mt-4 text-sm text-neutral-500">No traffic recorded yet.</p>
      ) : (
        <>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            {groups.map((group) => (
              <WorldGrid key={group.world} group={group} />
            ))}
          </div>
          <Legend />
        </>
      )}
    </section>
  );
}
