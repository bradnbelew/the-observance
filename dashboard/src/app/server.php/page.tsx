import type { Metadata } from 'next';
import Link from 'next/link';
import { publicServers } from '@/lib/legacy-content';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { readV5CompletionFlag, recordV5WebSequence } from '@/lib/v5-web-progress';
import { LS02_DOCKET_CIPHER, LS02_TEACHING_STRIP } from '@/lib/v5-ls02-docket';
import { ServiceDocketForm } from './ServiceDocketForm';

export const metadata: Metadata = {
  title: 'Server Details - Copperline Hosting',
  robots: { index: false, follow: false },
};
export const dynamic = 'force-dynamic';

function DamagedServiceDocket() {
  return (
    <LegacyShell active="servers">
      <Breadcrumbs><Link href="/server-list.php">Public Server List</Link> &raquo; The Observance</Breadcrumbs>
      <OldPageTitle sub="Expired Minecraft listing; account field damaged in the final directory export.">The Observance</OldPageTitle>
      <div className="listing-expired">
        <b>This hosting account has expired.</b>
        <span>The public row survived, but its service field did not.</span>
      </div>
      <div className="server-detail-layout">
        <section>
          <h2 className="old-rule-title">Directory Trace</h2>
          <table className="detail-table"><tbody>
            <tr><th>Status</th><td><span className="old-status expired">expired</span></td></tr>
            <tr><th>Provider</th><td>Copperline Hosting</td></tr>
            <tr><th>Listed by</th><td><Link href="/community/index.php?user=mkept">mkept</Link></td></tr>
            <tr><th>Service field</th><td className="struck-value">damaged during directory migration</td></tr>
            <tr><th>Server address</th><td className="struck-value">removed from directory export</td></tr>
          </tbody></table>

          <section className="service-docket" aria-labelledby="service-docket-heading">
            <h2 id="service-docket-heading">Recovered account docket</h2>
            <p>The migration kept one numeric docket line and the paper reference strip used by account staff. Restore the service field to search the retained account index.</p>
            <div className="docket-paper">
              <b>FIELD 04 / SERVICE</b>
              <code>{LS02_DOCKET_CIPHER}</code>
              <span>REFERENCE STRIP</span>
              <code>{LS02_TEACHING_STRIP}</code>
            </div>
            <ServiceDocketForm />
          </section>
        </section>
        <aside className="server-side-card"><h3>Migration Note</h3><div><b>Source</b><span>CHI-GS archive</span></div><div><b>OCR</b><span>partial</span></div><div><b>Panel</b><span>retired</span></div><small>Individual symbols cannot be checked.</small></aside>
      </div>
    </LegacyShell>
  );
}

export default async function ServerDetailsPage({ searchParams }: { searchParams: Promise<{ id?: string }> }) {
  const id = (await searchParams).id ?? '';
  const server = publicServers.find((row) => row.id === id);
  if (!server) {
    return (
      <LegacyShell active="servers">
        <Breadcrumbs>Server List &raquo; Listing Not Found</Breadcrumbs>
        <OldPageTitle>Listing Not Found</OldPageTitle>
        <div className="old-message error">The requested server listing is not present in this directory cache.</div>
        <p><Link className="small-old-link" href="/server-list.php">Return to the public server list &raquo;</Link></p>
      </LegacyShell>
    );
  }

  const observance = server.recoveredDocket === true;
  if (observance) {
    await recordV5WebSequence(
      ['LS01'],
      'copperline_traces',
      { provider: 'Copperline', uploader: 'mkept', trace: 'damaged-expired-row', handler: 'route_receipt' },
    );
    const serviceResolved = await readV5CompletionFlag('v5_ls02_service_1842');
    if (!serviceResolved.complete) return <DamagedServiceDocket />;
  }

  return (
    <LegacyShell active="servers">
      <Breadcrumbs><Link href="/server-list.php">Public Server List</Link> &raquo; {server.name}</Breadcrumbs>
      <OldPageTitle sub={observance ? 'Minecraft service listing #1842' : `Minecraft server listing #${server.id}`}>{server.name}</OldPageTitle>
      <div className={`listing-expired ${observance ? '' : 'generic'}`}>
        <b>{server.status === 'expired' ? 'This hosting account has expired.' : 'This server is no longer responding.'}</b>
        <span>Information below is retained from the last public directory update.</span>
      </div>
      <div className="server-detail-layout">
        <section>
          <h2 className="old-rule-title">Server Information</h2>
          <table className="detail-table"><tbody>
            <tr><th>Status</th><td><span className={`old-status ${server.status}`}>{server.status}</span></td></tr>
            <tr><th>Game</th><td>Minecraft: Java Edition</td></tr>
            <tr><th>Last version</th><td>{server.version}</td></tr>
            <tr><th>Player slots</th><td>{server.players.split('/')[1] ?? 'unknown'}</td></tr>
            <tr><th>Location</th><td>{server.location}</td></tr>
            <tr><th>Server address</th><td className="struck-value">removed from directory export</td></tr>
            {observance ? <><tr><th>Listed by</th><td><Link href="/community/index.php?user=mkept">mkept</Link></td></tr><tr><th>Last directory check</th><td>November 2, 2014 at 9:00 PM CST</td></tr></> : null}
          </tbody></table>
          <h2 className="old-rule-title">Description</h2>
          <div className="old-description">{observance ? <><p>Small private survival server. Whitelist required. The server uses a custom resource pack; players joining without it will not see all map textures.</p><p><b>MOTD:</b> count first. walk low.</p></> : <p>This customer did not leave a description in the cached directory.</p>}</div>
          {observance ? <><h2 className="old-rule-title">Related Account Activity</h2><ul className="plain-link-list"><li><Link href="/support/ticket.php?id=9137" prefetch={false} rel="nofollow">Archived support ticket #9137</Link><span>November 2, 2014</span></li></ul></> : null}
        </section>
        <aside className="server-side-card"><h3>Query History</h3><div><b>03:00</b><span>timeout</span></div><div><b>09:00</b><span>timeout</span></div><div><b>15:00</b><span>timeout</span></div><div><b>21:00</b><span>timeout</span></div><small>Last four cached checks</small></aside>
      </div>
    </LegacyShell>
  );
}
