import type { HealthView } from "@/lib/database.types";

/**
 * Spoiler-free health panel.
 *
 * Reads only the neutral `v_health` view (no story content): last fired-beat
 * time, 24h info/warn/error counts, watcher-sleep state, and the API / Whisper
 * status pulled from settings. There are NO player names, custom names, or
 * arc/story labels here by construction — the view never exposes them.
 */

/** Normalize a jsonb status value (string, boolean, number, null) to a label. */
function statusLabel(value: HealthView["api_status"]): string {
  if (value === null || value === undefined) return "unknown";
  if (typeof value === "string") return value;
  if (typeof value === "boolean") return value ? "on" : "off";
  if (typeof value === "number") return String(value);
  return "unknown";
}

/** watcher_sleep is jsonb `true`/`false` (sometimes a string). Coerce to bool. */
function isWatcherSleeping(value: HealthView["watcher_sleep"]): boolean {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") return value.toLowerCase() === "true";
  return false;
}

/** Map a free-form status string to a dot color. Neutral, no semantics leaked. */
function statusTone(label: string): string {
  const v = label.toLowerCase();
  if (["ok", "up", "online", "healthy", "on", "ready"].includes(v)) {
    return "bg-emerald-500";
  }
  if (["down", "error", "offline", "fail", "failed"].includes(v)) {
    return "bg-rose-500";
  }
  if (["degraded", "warn", "warning", "slow"].includes(v)) {
    return "bg-amber-500";
  }
  return "bg-neutral-500";
}

function relativeTime(iso: string | null): string {
  if (!iso) return "never";
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return "unknown";
  const diffMs = Date.now() - then;
  if (diffMs < 0) return "just now";
  const mins = Math.floor(diffMs / 60_000);
  if (mins < 1) return "just now";
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

function absoluteTime(iso: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toISOString().replace("T", " ").slice(0, 19) + "Z";
}

function StatusRow({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="flex items-center justify-between gap-4 py-1.5">
      <span className="text-sm text-neutral-400">{label}</span>
      <span className="flex items-center gap-2">
        <span
          className={`h-2 w-2 rounded-full ${statusTone(value)}`}
          aria-hidden
        />
        <span className="font-mono text-sm text-neutral-200">{value}</span>
      </span>
    </div>
  );
}

function CountTile({
  label,
  count,
  tone,
}: {
  label: string;
  count: number | null;
  tone: string;
}) {
  return (
    <div className="rounded-md border border-neutral-800 bg-ash/40 px-3 py-2">
      <div className={`font-mono text-2xl tabular-nums ${tone}`}>
        {count === null ? "—" : count}
      </div>
      <div className="mt-0.5 text-xs uppercase tracking-wide text-neutral-500">
        {label}
      </div>
    </div>
  );
}

export default function HealthPanel({
  health,
  unavailable = false,
}: {
  health: HealthView | null;
  unavailable?: boolean;
}) {
  const lastBeatAt = health?.last_beat_at ?? null;
  const info = unavailable ? null : (health?.info_24h ?? 0);
  const warn = unavailable ? null : (health?.warn_24h ?? 0);
  const error = unavailable ? null : (health?.error_24h ?? 0);

  const sleeping = unavailable ? null : isWatcherSleeping(health?.watcher_sleep ?? false);
  const apiStatus = statusLabel(health?.api_status ?? null);
  const whisperStatus = statusLabel(health?.whisper_status ?? null);

  const misfiring = error !== null && error > 0;
  const overall = unavailable ? "unavailable" : misfiring ? "misfiring" : "nominal";

  return (
    <section className="rounded-lg border border-neutral-800 bg-slate-850 p-5">
      <div className="flex items-center justify-between">
        <h2 className="font-mono text-sm uppercase tracking-wide text-neutral-300">
          Health
        </h2>
        <span
          className={`rounded-full px-2 py-0.5 font-mono text-xs ${
            unavailable
              ? "bg-neutral-500/15 text-neutral-300"
              : misfiring
              ? "bg-rose-500/15 text-rose-300"
              : "bg-emerald-500/15 text-emerald-300"
          }`}
        >
          {overall}
        </span>
      </div>

      {/* Last beat */}
      <div className="mt-4 rounded-md border border-neutral-800 bg-ash/40 px-3 py-3">
        <div className="text-xs uppercase tracking-wide text-neutral-500">
          Last beat fired
        </div>
        <div className="mt-1 flex items-baseline gap-2">
          <span className="font-mono text-lg text-neutral-100">
            {unavailable ? "unavailable" : relativeTime(lastBeatAt)}
          </span>
          <span className="font-mono text-xs text-neutral-500">
            {unavailable ? "—" : absoluteTime(lastBeatAt)}
          </span>
        </div>
      </div>

      {/* 24h event counts */}
      <div className="mt-4">
        <div className="mb-2 text-xs uppercase tracking-wide text-neutral-500">
          Events (last 24h)
        </div>
        <div className="grid grid-cols-3 gap-2">
          <CountTile label="info" count={info} tone="text-neutral-200" />
          <CountTile
            label="warn"
            count={warn}
            tone={warn !== null && warn > 0 ? "text-amber-300" : "text-neutral-200"}
          />
          <CountTile
            label="error"
            count={error}
            tone={error !== null && error > 0 ? "text-rose-300" : "text-neutral-200"}
          />
        </div>
      </div>

      {/* Service + watcher status */}
      <div className="mt-4 divide-y divide-neutral-800/70 border-t border-neutral-800/70">
        <StatusRow label="API" value={apiStatus} />
        <StatusRow label="Whisper" value={whisperStatus} />
        <div className="flex items-center justify-between gap-4 py-1.5">
          <span className="text-sm text-neutral-400">Watcher</span>
          <span className="flex items-center gap-2">
            <span
              className={`h-2 w-2 rounded-full ${
                sleeping === null ? "bg-neutral-500" : sleeping ? "bg-neutral-500" : "bg-emerald-500"
              }`}
              aria-hidden
            />
            <span className="font-mono text-sm text-neutral-200">
              {sleeping === null ? "unknown" : sleeping ? "sleeping" : "awake"}
            </span>
          </span>
        </div>
      </div>
    </section>
  );
}
