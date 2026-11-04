import type { Metadata } from "next";
import Link from "next/link";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";
import { bundle, currentAct } from "@/lib/morrow/canon";
import { morrowStatus, ping } from "@/lib/morrow/slp";

export const metadata: Metadata = { title: "Network Status - Copperline Hosting" };
export const dynamic = "force-dynamic";

export default async function StatusPage() {
  const b = await bundle();
  let heartbeat = `heartbeat ${b.heartbeat.heartbeat}`;
  if ((await currentAct()) >= 10) {
    const status = await ping();
    if (status && morrowStatus(status) === "") heartbeat = "no heartbeat";
  }
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
          <tr><td><code>{b.heartbeat.process}</code></td><td>CHI-07</td><td>{heartbeat} · first heartbeat on this host {b.heartbeat.first_heartbeat_on_this_host?.slice(0, 10)} · <Link href="/status/heartbeat.json">json</Link></td></tr>
        </tbody>
      </table>
      <h2 className="old-rule-title">Past Incidents</h2>
      <table className="old-data-table">
        <tbody>
          {([...(b.status_archive ?? []).map((a) => ({ posted: a.posted, text: `#${a.id} ${a.title}`, update: a.update, href: "" })),
            { posted: incident.posted, text: `#6118 ${incident.title}`, href: "/status/incident-6118", update: "" },
            { posted: "2011-04-21 03:15 CDT", text: "Chicago storage array degraded (game nodes CHI-05 to CHI-07)", update: "", href: "" },
            { posted: "2012-09-08 01:00 CDT", text: "Chicago node migration", update: "", href: "" }] as { posted: string; text: string; update: string; href: string }[])
            .sort((x, y) => x.posted.localeCompare(y.posted))
            .map((row) => (
              <tr key={row.text}><td>{row.posted}</td><td>{row.href ? <Link href={row.href}>{row.text}</Link> : row.text}{row.update ? <div className="old-fineprint">{row.update}</div> : null}</td></tr>
            ))}
        </tbody>
      </table>
    </LegacyShell>
  );
}
