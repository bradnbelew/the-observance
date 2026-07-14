import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { readValidatedV5HoldArchive, V5_HOLD_ARCHIVE_DOWNLOAD_PATH } from '@/lib/v5-hold-archive';
import { readV5CompletionFlag, recordV5WebSequence } from '@/lib/v5-web-progress';

export const metadata: Metadata = {
  title: 'recovered relay files for the old server - Copperline Community',
  robots: { index: false, follow: false },
};
export const dynamic = 'force-dynamic';

type ArchiveQuery = { host?: string; callback?: string; route?: string };

function normalizeHost(raw: string): string {
  return raw.trim().toLowerCase()
    .replace(/^https?:\/\//, '')
    .replace(/^www\./, '')
    .replace(/\/+$/, '');
}

function normalizeCallback(raw: string): string {
  return raw.replace(/\D/g, '');
}

function normalizeRoute(raw: string): string {
  const trimmed = raw.trim().toLowerCase();
  if (!trimmed) return '';
  return `/${trimmed.replace(/^\/+/, '').replace(/\/+$/, '')}`;
}

function MissingArchive() {
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community Blog</Link> &raquo; Archive</Breadcrumbs>
      <OldPageTitle>Archive Entry Not Available</OldPageTitle>
      <div className="old-message error">That retained attachment is not available from this directory path.</div>
    </LegacyShell>
  );
}

export default async function WorldBackupPost({ searchParams }: { searchParams: Promise<ArchiveQuery> }) {
  const prerequisite = await readV5CompletionFlag('v5_ls03_directory_trail');
  if (!prerequisite.complete) return <MissingArchive />;

  const params = await searchParams;
  const attempted = params.host !== undefined || params.callback !== undefined || params.route !== undefined;
  const exact = attempted
    && normalizeHost(params.host ?? '') === 'copperlinehosting.com'
    && normalizeCallback(params.callback ?? '') === '9137'
    && normalizeRoute(params.route ?? '') === '/community/remote-room.php';

  const existing = await readV5CompletionFlag('v5_ls04_archive_solved');
  let archiveSolved = existing.complete;
  if (!archiveSolved && exact) {
    const progress = await recordV5WebSequence(
      ['LS04'],
      'copperline_world_backup',
      {
        handler: 'archive_resolver',
        host: 'copperlinehosting.com',
        callback_verified: true,
        route: '/community/remote-room.php',
      },
    );
    archiveSolved = progress.complete;
  }

  const archive = await readValidatedV5HoldArchive();
  const available = archive !== null;
  const size = archive ? `${Math.max(1, Math.round(archive.bytes.byteLength / 1024))} KB` : 'file missing';
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community Blog</Link> &raquo; recovered relay files for the old server</Breadcrumbs>
      <article className="single-old-post">
        <h1>recovered relay files for the old server</h1>
        <div className="old-post-meta">Posted by <Link href="/community/index.php?user=mkept">mkept</Link> on February 8, 2011 at 11:42 PM &middot; mirror corrected July 13, 2026</div>
        <p>The attachment that used to be called a world backup was mislabeled. It is the small off-site diagnostic export for service 1842. It does not contain a playable world, player files, chat, a server address, or a datapack.</p>
        <p>There are four field-relay work orders and one route image inside. Copperline told me to read them by the age stamped on each copper jacket, not by rack letter. I left the files byte-for-byte as recovered.</p>
        <div className="old-attachment"><b>Attachment retained with post</b><div><span className="zip-icon">ZIP</span><p><strong>the-hold.zip</strong><small>{size} &middot; diagnostic export &middot; SHA-1 index retained</small></p>{available ? <a href={V5_HOLD_ARCHIVE_DOWNLOAD_PATH} download>Download</a> : <em>Unavailable</em>}</div></div>
        <p>The checksum and roster copy are also on my <Link href="/record/the-record-keeps" className="ordinary-inline-link">static file mirror</Link>. Do not install this archive into Minecraft; it is ordinary support material.</p>

        <section className="archive-resolver">
          <h2>Field relay destination check</h2>
          <p>The expired panel can still verify a complete off-site destination. Enter the host, callback, and route exactly as reconstructed from the export. The checker does not validate individual fields.</p>
          <form method="get" action="/community/2011/02/08/world-backup/" className="archive-resolver-form">
            <label>Account host<input name="host" defaultValue={params.host ?? ''} autoComplete="off" spellCheck={false} /></label>
            <label>Callback<input name="callback" defaultValue={params.callback ?? ''} inputMode="numeric" autoComplete="off" spellCheck={false} /></label>
            <label>Remote route<input name="route" defaultValue={params.route ?? ''} autoComplete="off" spellCheck={false} /></label>
            <button type="submit">Check destination</button>
          </form>
          {archiveSolved ? (
            <div className="relay-result">
              <h2>Destination retained</h2>
              <p>The complete relay destination matches the archived service record.</p>
              <Link href="/community/remote-room.php" prefetch={false} rel="nofollow">Open retained field relay &raquo;</Link>
            </div>
          ) : attempted ? (
            <div className="old-message error">Archive destination not found. No individual field result is available.</div>
          ) : null}
        </section>

        <p className="post-signature">&mdash; m.</p>
        <footer className="old-post-footer">Filed under: <Link href="/community/index.php">Minecraft</Link>, <Link href="/community/index.php">Server Administration</Link> &middot; Comments are closed.</footer>
      </article>
    </LegacyShell>
  );
}
