import type { Metadata } from 'next';
import Image from 'next/image';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { recordV5WebSequence } from '@/lib/v5-web-progress';
import { hasCampaignEvent, recordCampaignEvent } from '@/lib/arg-event-store';
import { CustodyDecisionForm } from './CustodyDecisionForm';

export const metadata: Metadata = {
  title: 'Archive Lookup - Copperline Community',
  robots: { index: false, follow: false },
};
export const dynamic = 'force-dynamic';

type ArchiveSearch = { service?: string; ticket?: string; locker?: string };

function MissingArchive() {
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs>
      <OldPageTitle>Archive Entry Not Found</OldPageTitle>
      <div className="old-message error">No retained attachment is indexed under that archive route.</div>
    </LegacyShell>
  );
}

async function Service1842Archive() {
  const restored = await recordCampaignEvent({
    eventKey: 'p1.attachment_history_restored',
    idempotencyKey: 'copperline:p1:service-1842-ticket-9137-history',
    source: 'copperline',
    payload: { service: '1842', ticket: '9137', treatment: 'read-only-history' },
  });
  if (restored.status !== 'committed') return <MissingArchive />;
  const custodyAccepted = await hasCampaignEvent('p1.mkept_intent_authenticated');

  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; <Link href="/support/ticket.php?id=9137">Ticket 9137</Link> &raquo; Account archive</Breadcrumbs>
      <OldPageTitle sub="Read-only account and attachment history retained with expired service 1842.">Service 1842 / recovery history</OldPageTitle>
      <div className="ticket-summary">
        <div><b>Service</b><span>1842</span></div>
        <div><b>Ticket</b><span>9137</span></div>
        <div><b>Account owner</b><span>mkept</span></div>
        <div><b>Status</b><span className="ticket-closed">Expired / retained</span></div>
      </div>
      <section className="old-copy" aria-labelledby="history-heading">
        <h2 id="history-heading">Retained history</h2>
        <p>This is an account-recovery record, not a rebuilt customer page. Copperline kept the old attachment order, checksum notes, and support replies. It did not restart the world or replace damaged files with a clean copy.</p>
        <table className="detail-table"><tbody>
          <tr><th>February 8, 2011</th><td>mkept uploads a damaged world copy and a checksum note from the off-site cartridge.</td></tr>
          <tr><th>February 9, 2011</th><td>Support records the cartridge barcode and marks the upload read-only after a partial retry.</td></tr>
          <tr><th>February 11, 2011</th><td>mkept confirms the copy opens locally and asks that player files and chat remain outside the public archive.</td></tr>
          <tr><th>November 2, 2014</th><td>Ticket 9137 reconnects the expired service, the community post, and the retained cartridge without adding a new route or file.</td></tr>
        </tbody></table>
        <p>The history supports a narrow claim: somebody using the name mkept deliberately kept a damaged server recoverable and kept its private player records out of the public copy.</p>
      </section>
      <CustodyDecisionForm alreadyAccepted={custodyAccepted === true} />
      <div className="ticket-end">Comments, replacement uploads, and account changes are disabled.</div>
    </LegacyShell>
  );
}

async function Locker13Archive() {
  const campRestored = await hasCampaignEvent('p9.company_biographies_restored');
  if (campRestored !== true) return <MissingArchive />;

  await recordV5WebSequence(
    ['A06'],
    'copperline_archive_route',
    { service: '1842', ticket: '9137', locker: '13', route: '/community/archive.php', handler: 'route_resolver' },
  );
  await recordV5WebSequence(
    ['A07'],
    'clip_01_ash_locker',
    {
      media_key: 'clip_01_ash_locker',
      image_ids: ['camp_frame_06', 'locker_detail_13'],
      handler: 'automatic_media_reveal',
    },
  );

  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; <Link href="/community/archive/ash-camp">Ash Camp</Link> &raquo; Locker 13</Breadcrumbs>
      <OldPageTitle sub="Earned from the restored camp route. The earlier service archive remains unchanged.">Field Camera Index / Locker 13</OldPageTitle>
      <div className="ticket-summary">
        <div><b>Service</b><span>1842</span></div>
        <div><b>Ticket</b><span>9137</span></div>
        <div><b>Locker</b><span>13</span></div>
        <div><b>Status</b><span className="ticket-closed">Recovered</span></div>
      </div>
      <section className="relay-result">
        <h2>One camera attachment, two retained stills</h2>
        <p>The camp route joins an old service number, a support ticket, and a locker named in Ash&apos;s shot card. None of those records alone identifies the attachment.</p>
        <div className="archive-stills">
          <figure>
            <Image src="/evidence/copperline/camp-frame-06.png" width={1448} height={1086} alt="Retained wide frame of the abandoned camp." />
            <figcaption>frame 06 / camp wide</figcaption>
          </figure>
          <figure>
            <Image src="/evidence/copperline/locker-detail-13.png" width={1448} height={1086} alt="Detail of a storage door with a painted plant and grouped tally cuts." />
            <figcaption>frame 06 / north storage enlargement</figcaption>
          </figure>
        </div>
        <a href="https://youtu.be/du-qp_clP7c" rel="nofollow noreferrer" target="_blank">Open base_check_06.mp4</a>
        <div className="relay-linking">
          <b>Archive metadata</b>
          <p>source name: <code>clip_01_prior_base.mp4</code></p>
          <p>SHA-1: <code>844c2aaf8fb51836add4b59e81abe4131c8d6d0a</code></p>
        </div>
      </section>
      <div className="ticket-end">Comments and replacement uploads are disabled.</div>
    </LegacyShell>
  );
}

export default async function PriorCompanyArchivePage({ searchParams }: { searchParams: Promise<ArchiveSearch> }) {
  const params = await searchParams;
  if (params.service !== '1842' || params.ticket !== '9137') return <MissingArchive />;
  if (params.locker === undefined) return <Service1842Archive />;
  if (params.locker === '13') return <Locker13Archive />;
  return <MissingArchive />;
}
