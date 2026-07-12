import type { Metadata } from "next";
import Link from "next/link";
import { publicServers } from "@/lib/legacy-content";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Public Server List - Copperline Hosting" };

export default function ServerListPage() {
  return <LegacyShell active="servers"><Breadcrumbs>Public Server List</Breadcrumbs><OldPageTitle sub="Cached listings from customers who enabled public server advertising.">Public Server List</OldPageTitle><div className="old-filter"><label>Game <select defaultValue="Minecraft"><option>Minecraft</option></select></label><label>Status <select defaultValue="All"><option>All</option><option>Online</option></select></label><button type="button">Filter</button></div><table className="old-data-table server-table"><thead><tr><th>Status</th><th>Server Name</th><th>Version</th><th>Players</th><th>Location</th></tr></thead><tbody>{publicServers.map(server => <tr key={server.id}><td><span className={`old-status ${server.status}`}>{server.status}</span></td><td><Link href={`/server.php?id=${server.id}`}>{server.name}</Link><small>Listing #{server.id}</small></td><td>{server.version}</td><td>{server.players}</td><td>{server.location}</td></tr>)}</tbody></table><p className="old-fineprint">Listings are cached. Copperline does not verify server descriptions or availability. Expired listings may remain visible until the directory is rebuilt.</p></LegacyShell>;
}
