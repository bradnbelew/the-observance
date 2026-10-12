import type { Metadata } from "next";
import { Breadcrumbs, LegacyShell, OldPageTitle } from "@/components/legacy/LegacyShell";

export const metadata: Metadata = { title: "Game Server Hosting - Copperline Hosting" };

/**
 * G01. The forum was never linked after the 2013 redesign, but the importer's
 * cache note survived in this template. robots.txt names the directory; this comment
 * names a thread inside it.
 */
const IMPORTER_NOTE =
  "<!-- forum-import cache: kept threads referenced by open support tickets. " +
  "e.g. /forum-import/showthread.php?t=4471 (ticket 6118). TODO remove after migration -->";

export default function GameServersPage() {
  return (
    <LegacyShell active="games">
      <Breadcrumbs>Game Servers</Breadcrumbs>
      <OldPageTitle sub="Affordable game servers on our US network.">Game Server Hosting</OldPageTitle>
      <div className="old-alert"><b>Archived product sheet.</b> Prices and features below describe the retired 2014 service and cannot be ordered.</div>
      <span hidden dangerouslySetInnerHTML={{ __html: IMPORTER_NOTE }} />
      <section className="old-copy">
        <h2>Minecraft Servers</h2>
        <p>Run your own private or public Minecraft server without configuring a VPS. Every plan includes our TCAdmin control panel, FTP access and one daily backup slot.</p>
        <table className="old-data-table plan-table">
          <thead><tr><th>Plan</th><th>Memory</th><th>Players</th><th>Disk</th><th>Monthly</th></tr></thead>
          <tbody>
            <tr><td>Starter</td><td>512 MB</td><td>10</td><td>5 GB</td><td>$3.95</td></tr>
            <tr><td>Builder</td><td>1 GB</td><td>20</td><td>10 GB</td><td>$6.95</td></tr>
            <tr><td>Community</td><td>2 GB</td><td>40</td><td>20 GB</td><td>$11.95</td></tr>
            <tr><td>Network</td><td>4 GB</td><td>80</td><td>35 GB</td><td>$19.95</td></tr>
          </tbody>
        </table>
        <h2>Included with every plan</h2>
        <div className="feature-grid">
          <div><b>TCAdmin Panel</b><p>Start, stop, restart and view the live console.</p></div>
          <div><b>FTP Access</b><p>Upload worlds, plugins and configuration files.</p></div>
          <div><b>Daily Backups</b><p>One rolling backup stored on a separate disk.</p></div>
          <div><b>Bukkit Support</b><p>Use our installer or upload your own server jar.</p></div>
        </div>
        <p className="old-fineprint">Free web space: every game plan includes a personal page at copperlinehosting.com/~yourname for maps, rules and screenshots.</p>
        <p className="old-fineprint">Minecraft server software and third-party plugins are provided without warranty. Copperline Hosting is not affiliated with Mojang AB.</p>
      </section>
    </LegacyShell>
  );
}
