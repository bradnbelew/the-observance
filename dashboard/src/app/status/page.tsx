import type { Metadata } from "next";
import { createClient } from "@/lib/supabase/server";
import type {
  HealthView,
  HeatmapView,
  ComplianceCountsView,
} from "@/lib/database.types";
import HealthPanel from "@/components/status/HealthPanel";
import Heatmap from "@/components/status/Heatmap";
import ComplianceCounts from "@/components/status/ComplianceCounts";

export const metadata: Metadata = {
  title: "Status — The Observance",
  description: "Spoiler-free health, traffic, and compliance status.",
  robots: { index: false, follow: false },
};

// Always read live state; never serve a stale cache of the control surface.
export const dynamic = "force-dynamic";

/**
 * Spoiler-free Status mode (PUBLIC / anon).
 *
 * This server component reads ONLY the three spoiler-free views — v_health,
 * v_heatmap, v_compliance_counts — through the request-scoped (anon-key) server
 * Supabase client. anon has no table grants and no base-table RLS policies, so
 * these views are the only thing it can physically read. There is NO story
 * content on this page: no player names, no custom names, no arc/beat labels.
 */
export default async function StatusPage() {
  const supabase = await createClient();

  // Run all three reads concurrently. Each is a single neutral view query.
  const [healthRes, heatmapRes, complianceRes] = await Promise.all([
    supabase.from("v_health").select("*").maybeSingle(),
    supabase
      .from("v_heatmap")
      .select("*")
      .order("visits", { ascending: false })
      .limit(5000),
    supabase.from("v_compliance_counts").select("*").maybeSingle(),
  ]);

  const health: HealthView | null = healthRes.data ?? null;
  const heatmapCells: HeatmapView[] = heatmapRes.data ?? [];
  const compliance: ComplianceCountsView | null = complianceRes.data ?? null;

  const errors = [
    healthRes.error?.message,
    heatmapRes.error?.message,
    complianceRes.error?.message,
  ].filter(Boolean) as string[];

  return (
    <div className="space-y-8">
      <header className="space-y-1">
        <h1 className="font-mono text-2xl text-neutral-100">Status</h1>
        <p className="max-w-prose text-sm text-neutral-400">
          Spoiler-free health view. Is it running, is it misfiring, where are
          people, and the neutral compliance counts. No story.
        </p>
      </header>

      {errors.length > 0 && (
        <div className="rounded-md border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          <p className="font-mono">Some status data could not be loaded.</p>
          <ul className="mt-1 list-disc pl-5 text-rose-300/80">
            {errors.map((e, i) => (
              <li key={i} className="font-mono text-xs">
                {e}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <HealthPanel health={health} />
        <ComplianceCounts counts={compliance} />
      </div>

      <Heatmap cells={heatmapCells} />
    </div>
  );
}
