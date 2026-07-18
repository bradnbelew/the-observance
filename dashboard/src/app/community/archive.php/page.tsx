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

export default async function PriorCompanyArchivePage({ searchParams }: { searchParams: Promise<ArchiveSearch> }) {
  const params = await searchParams;
  const exact = params.service === '1842' && params.ticket === '9137' && params.locker === '13';
  if (!exact) return <MissingArchive />;

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
  const restored = await recordCampaignEvent({
    eventKey: 'p1.attachment_history_restored',
    idempotencyKey: 'copperline:p1:service-1842-ticket-9137-locker-13',
    source: 'copperline',
    payload: { service: '1842', ticket: '9137', locker: '13', treatment: 'read-only' },
  });
  if (restored.status !== 'committed') return <MissingArchive />;
  const custodyAccepted = await hasCampaignEvent('p1.mkept_intent_authenticated');

  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive &raquo; Locker 13</Breadcrumbs>
      <OldPageTitle sub="Attachment retained with ticket 9137 under expired service 1842.">Field Camera Index / Locker 13</OldPageTitle>
      <div className="ticket-summary">
        <div><b>Service</b><span>1842</span></div>
        <div><b>Ticket</b><span>9137</span></div>
        <div><b>Locker</b><span>13</span></div>
        <div><b>Status</b><span className="ticket-closed">Recovered</span></div>
      </div>
      <section className="relay-result">
        <h2>One camera attachment, two retained stills</h2>
        <p>The host copy and its two recovery stills are available read-only. The detail came from the north storage row in frame 06.</p>
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
      <CustodyDecisionForm alreadyAccepted={custodyAccepted === true} />
      <div className="ticket-end">Comments and replacement uploads are disabled.</div>
    </LegacyShell>
  );
}
