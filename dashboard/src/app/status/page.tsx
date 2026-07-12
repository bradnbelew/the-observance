import type { Metadata } from "next";
import { createClient } from "@/lib/supabase/server";
import type { HealthView } from "@/lib/database.types";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Network Status - Copperline Hosting", description: "Copperline legacy network status." };
export const dynamic = "force-dynamic";

export default async function StatusPage() {
  const supabase = await createClient();
  const result = await supabase.from("v_health").select("*").maybeSingle();
  const health = result.data as HealthView | null;
  const mirrorReachable = !result.error;
  const lastContact = health?.last_beat_at
    ? new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeStyle: "short", timeZone: "UTC" }).format(new Date(health.last_beat_at)) + " UTC"
    : "No contact retained";

  return <LegacyShell active="support"><Breadcrumbs>Network Status</Breadcrumbs><OldPageTitle sub="Last updated from the legacy monitoring cache.">Network Status</OldPageTitle>
    <div className="old-alert"><b>Legacy notice:</b> Automated monitoring has been retired. Status values below may be delayed or incomplete.</div>
    <table className="old-data-table status-old-table"><thead><tr><th>Service</th><th>Location</th><th>Status</th><th>Last Update</th></tr></thead><tbody><tr><td>Client web archive</td><td>Chicago, IL</td><td><span className={`old-status ${mirrorReachable ? "online" : "offline"}`}>{mirrorReachable ? "online" : "offline"}</span></td><td>Automatic</td></tr><tr><td>Minecraft service #1842</td><td>Chicago, IL</td><td><span className="old-status expired">expired</span></td><td>{lastContact}</td></tr><tr><td>TCAdmin 1 panel</td><td>All locations</td><td><span className="old-status offline">read only</span></td><td>Nov 3, 2014</td></tr><tr><td>New order system</td><td>All locations</td><td><span className="old-status offline">closed</span></td><td>Dec 1, 2014</td></tr></tbody></table>
    <h2 className="old-rule-title status-history-title">Incident History</h2><table className="old-data-table"><tbody><tr><td>Nov 03, 2014</td><td>Legacy game panel changed to read-only access.</td><td>Resolved</td></tr><tr><td>Jul 20, 2014</td><td>Billing database maintenance completed.</td><td>Resolved</td></tr><tr><td>Sep 08, 2012</td><td>Chicago game nodes migrated to replacement rack.</td><td>Resolved</td></tr></tbody></table>
  </LegacyShell>;
}
