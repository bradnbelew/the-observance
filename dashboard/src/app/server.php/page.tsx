import type { Metadata } from "next";
import Link from "next/link";
import { publicServers } from "@/lib/legacy-content";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Server Details - Copperline Hosting" };
export const dynamic = "force-dynamic";

export default async function ServerDetailsPage({ searchParams }: { searchParams: Promise<{ id?: string }> }) {
  const id = (await searchParams).id ?? "";
  const server = publicServers.find(row => row.id === id);
  if (!server) return <LegacyShell active="servers"><Breadcrumbs>Server List » Listing Not Found</Breadcrumbs><OldPageTitle>Listing Not Found</OldPageTitle><div className="old-message error">The requested server listing is not present in this directory cache.</div><p><Link className="small-old-link" href="/server-list.php">Return to the public server list »</Link></p></LegacyShell>;

  const observance = server.id === "1842";
  return <LegacyShell active="servers"><Breadcrumbs><Link href="/server-list.php">Public Server List</Link> » {server.name}</Breadcrumbs><OldPageTitle sub={`Minecraft server listing #${server.id}`}>{server.name}</OldPageTitle>
    <div className={`listing-expired ${observance ? "" : "generic"}`}><b>{server.status === "expired" ? "This hosting account has expired." : "This server is no longer responding."}</b><span>Information below is retained from the last public directory update.</span></div>
    <div className="server-detail-layout"><section><h2 className="old-rule-title">Server Information</h2><table className="detail-table"><tbody><tr><th>Status</th><td><span className={`old-status ${server.status}`}>{server.status}</span></td></tr><tr><th>Game</th><td>Minecraft: Java Edition</td></tr><tr><th>Last version</th><td>{server.version}</td></tr><tr><th>Player slots</th><td>{server.players.split("/")[1] ?? "unknown"}</td></tr><tr><th>Location</th><td>{server.location}</td></tr><tr><th>Server address</th><td className="struck-value">removed from directory export</td></tr>{observance ? <><tr><th>Listed by</th><td><Link href="/community/index.php?user=mkept">mkept</Link></td></tr><tr><th>Last directory check</th><td>November 2, 2014 at 9:00 PM CST</td></tr></> : null}</tbody></table>
      <h2 className="old-rule-title">Description</h2><div className="old-description">{observance ? <><p>Small private survival server. Whitelist required. The server uses a custom resource pack; players joining without it will not see all map textures.</p><p><b>MOTD:</b> count first. walk low.</p></> : <p>This customer did not leave a description in the cached directory.</p>}</div>
      {observance ? <><h2 className="old-rule-title">Related Account Activity</h2><ul className="plain-link-list"><li><Link href="/community/2011/02/08/world-backup/">World backup posted to community blog</Link><span>February 8, 2011</span></li><li><Link href="/support/ticket.php?id=1851">Archived support request #1851</Link><span>November 2, 2014</span></li></ul></> : null}
    </section><aside className="server-side-card"><h3>Query History</h3><div><b>03:00</b><span>timeout</span></div><div><b>09:00</b><span>timeout</span></div><div><b>15:00</b><span>timeout</span></div><div><b>21:00</b><span>timeout</span></div><small>Last four cached checks</small></aside></div>
  </LegacyShell>;
}
