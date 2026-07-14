import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { recordV5WebSequence } from '@/lib/v5-web-progress';

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

  const routeProgress = await recordV5WebSequence(
    ['A06'],
    'copperline_archive_route',
    { service: '1842', ticket: '9137', locker: '13', route: '/community/archive.php', handler: 'route_resolver' },
  );
  if (!routeProgress.complete) return <MissingArchive />;
  const mediaProgress = await recordV5WebSequence(
    ['A07'],
    'clip_01_ash_locker',
    { media_key: 'clip_01_ash_locker', handler: 'automatic_media_reveal' },
  );
  if (!mediaProgress.complete) return <MissingArchive />;

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
        <h2>One camera attachment</h2>
        <p>The retained host copy is available read-only. Preserve the visible frames and time marks when reviewing it.</p>
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
