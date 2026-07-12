import type { Metadata } from "next";
import Link from "next/link";
import { publicServers } from "@/lib/legacy-content";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Public Server List - Copperline Hosting" };

export default function ServerListPage() {
  return <LegacyShell active="servers"><Breadcrumbs>Public Server List</Breadcrumbs><OldPageTitle sub="Final cached directory from customers who enabled public advertising.">Public Server List</OldPageTitle><div className="directory-snapshot"><b>Directory snapshot:</b> Minecraft · all states · rebuilt November 3, 2014 02:10 CST</div><table className="old-data-table server-table"><thead><tr><th>Status</th><th>Server Name</th><th>Version</th><th>Players</th><th>Location</th></tr></thead><tbody>{publicServers.map(server => <tr key={server.id}><td><span className={`old-status ${server.status}`}>{server.status}</span></td><td><Link href={`/server.php?id=${server.id}`}>{server.name}</Link><small>Listing #{server.id}</small></td><td>{server.version}</td><td>{server.players}</td><td>{server.location}</td></tr>)}</tbody></table><p className="old-fineprint">Filters and live queries are unavailable in the preserved directory. Descriptions and expired listings are retained without verification.</p></LegacyShell>;
}
