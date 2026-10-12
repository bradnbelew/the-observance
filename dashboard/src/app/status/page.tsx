import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle } from "@/lib/morrow/canon";

export const metadata: Metadata = { title: "Network Status - Copperline Hosting" };
export const dynamic = "force-dynamic";

export default async function StatusPage() {
  const b = await bundle();
  const incident = b.status_incident.morrow_rewrite ?? b.status_incident.original;
  return (
    <LegacyShell active="support">
      <Breadcrumbs>Network Status</Breadcrumbs>
      <OldPageTitle sub="Retained status board. Live checks run only for services still hosted on this mirror.">Network Status</OldPageTitle>
      <table className="old-data-table">
        <thead><tr><th>Service</th><th>Location</th><th>Status</th></tr></thead>
        <tbody>
          <tr><td>Web hosting (archive mirror)</td><td>-</td><td><span className="old-status online">online</span></td></tr>
          <tr><td>Game panel (TCAdmin 1)</td><td>Chicago</td><td><span className="old-status offline">read only</span></td></tr>
          <tr><td>Game node CHI-07</td><td>Chicago</td><td><span className="old-status online">online</span></td></tr>
          <tr><td><code>{b.heartbeat.process}</code></td><td>CHI-07</td><td>heartbeat {b.heartbeat.heartbeat} · <Link href="/status/heartbeat.json">json</Link></td></tr>
        </tbody>
      </table>
      <h2 className="old-rule-title">Past Incidents</h2>
      <table className="old-data-table">
        <tbody>
          <tr><td>{incident.posted}</td><td><Link href="/status/incident-6118">#6118 {incident.title}</Link></td></tr>
          <tr><td>2011-04-21 03:15 CDT</td><td>Chicago storage array degraded (game nodes CHI-05 to CHI-07)</td></tr>
          <tr><td>2012-09-08 01:00 CDT</td><td>Chicago node migration</td></tr>
        </tbody>
      </table>
    </LegacyShell>
  );
}
