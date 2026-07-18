import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { readValidatedV5HoldArchive, V5_HOLD_ARCHIVE_DOWNLOAD_PATH } from '@/lib/v5-hold-archive';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = {
  title: 'recovered relay files for the old server - Copperline Community',
  robots: { index: false, follow: false },
};
export const dynamic = 'force-dynamic';

function MissingArchive() {
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community Blog</Link> &raquo; Archive</Breadcrumbs>
      <OldPageTitle>Archive Entry Not Available</OldPageTitle>
      <div className="old-message error">That retained attachment is not available from this directory path.</div>
    </LegacyShell>
  );
}

export default async function WorldBackupPost() {
  const prerequisite = await hasCampaignEvent('p2.artifact_authenticated');
  if (prerequisite !== true) return <MissingArchive />;

  const archive = await readValidatedV5HoldArchive();
  const available = archive !== null;
  const size = archive ? `${Math.max(1, Math.round(archive.bytes.byteLength / 1024))} KB` : 'file missing';
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community Blog</Link> &raquo; recovered relay files for the old server</Breadcrumbs>
      <article className="single-old-post">
        <h1>recovered relay files for the old server</h1>
        <div className="old-post-meta">Posted by <Link href="/community/index.php?user=mkept">mkept</Link> on February 8, 2011 at 11:42 PM &middot; mirror corrected July 13, 2026</div>
        <p>I found this on the off-site cartridge listed in ticket 9137. It opens as a small Java Edition world. The customer used it to keep remote dispatch routing out of the support record.</p>
        <p>I removed the player files and chat history. The service office and its records are otherwise unchanged. Nothing in the world needs a client mod.</p>
        <div className="old-attachment"><b>Attachment retained with post</b><div><span className="zip-icon">ZIP</span><p><strong>the-hold.zip</strong><small>{size} &middot; Java Edition 1.21.11 world &middot; SHA-1 retained</small></p>{available ? <a href={V5_HOLD_ARCHIVE_DOWNLOAD_PATH} download>Download world</a> : <em>Unavailable</em>}</div></div>
        <p>Extract the folder into <code>.minecraft/saves</code> and open the world normally. The recovery image handles its own game mode.</p>
        <p>The checksum is mirrored with the old account material at <Link href="/record/the-record-keeps" className="ordinary-inline-link">mkept&apos;s static directory</Link>.</p>

        <p className="post-signature">&mdash; m.</p>
        <footer className="old-post-footer">Filed under: <Link href="/community/index.php">Minecraft</Link>, <Link href="/community/index.php">Server Administration</Link> &middot; Comments are closed.</footer>
      </article>
    </LegacyShell>
  );
}
