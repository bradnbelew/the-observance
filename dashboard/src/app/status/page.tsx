import type { Metadata } from "next";
import { createClient } from "@/lib/supabase/server";
import type { HealthView } from "@/lib/database.types";

export const metadata: Metadata = {
  title: "Host status — SNOIKERZ",
  description: "Archived mirror availability.",
  robots: { index: false, follow: false },
};

export const dynamic = "force-dynamic";

/**
 * Public mirror status. Operational and player telemetry belong on /author;
 * this surface exposes only a diegetic host response and last-contact time.
 */
export default async function StatusPage() {
  const supabase = await createClient();
  const healthRes = await supabase.from("v_health").select("*").maybeSingle();
  const health = healthRes.data as HealthView | null;
  const unavailable = Boolean(healthRes.error);
  const errorCount = health?.error_24h ?? 0;
  const last = health?.last_beat_at
    ? new Date(health.last_beat_at).toISOString().replace("T", " ").slice(0, 19) + "Z"
    : "unknown";
  const response = unavailable
    ? "no reply"
    : errorCount > 0
      ? "degraded"
      : "reply received";

  return (
    <div className="space-y-8">
      <header className="space-y-2 border-b border-neutral-900 pb-5">
        <p className="font-mono text-[10px] uppercase tracking-[0.2em] text-neutral-700">
          snoikerz mirror 03 / uptime cache
        </p>
        <h1 className="font-mono text-2xl uppercase text-neutral-100">
          host status
        </h1>
        <p className="max-w-prose text-sm text-neutral-400">
          The old panel kept only a public response row. Staff, traffic, player,
          and moderation records were removed with the account list.
        </p>
      </header>

      <section className="max-w-2xl border border-neutral-900 bg-black/25 font-mono text-sm">
        <div className="border-b border-neutral-900 px-4 py-2 text-[10px] uppercase tracking-wider text-neutral-700">
          cached check
        </div>
        <dl className="divide-y divide-neutral-900 px-4">
          <div className="flex justify-between gap-6 py-4">
            <dt className="text-neutral-600">ping</dt>
            <dd
              className={
                unavailable || errorCount > 0
                  ? "text-amber-700"
                  : "text-neutral-300"
              }
            >
              {response}
            </dd>
          </div>
          <div className="flex justify-between gap-6 py-4">
            <dt className="text-neutral-600">last contact</dt>
            <dd className="text-neutral-400">{last}</dd>
          </div>
          <div className="flex justify-between gap-6 py-4">
            <dt className="text-neutral-600">public row</dt>
            <dd className="text-neutral-400">unlisted / retained</dd>
          </div>
          <div className="flex justify-between gap-6 py-4">
            <dt className="text-neutral-600">staff</dt>
            <dd className="text-neutral-500">no staff listed</dd>
          </div>
        </dl>
      </section>

      <p className="max-w-2xl font-mono text-xs lowercase leading-relaxed text-neutral-700">
        A missing reply does not prove the world is gone. Mirror 03 continued
        checking after the control account expired.
      </p>
    </div>
  );
}
