import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';
import { readValidatedV5HoldArchive } from '@/lib/v5-hold-archive';
import { PackageReviewForm } from './PackageReviewForm';

export const metadata: Metadata = { title: 'Package Review - Copperline Community', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function PackageReviewPage() {
  const custodyReady = await hasCampaignEvent('p1.mkept_intent_authenticated');
  if (custodyReady !== true) {
    return (
      <LegacyShell active="community">
        <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs>
        <OldPageTitle>Package Review Unavailable</OldPageTitle>
        <div className="old-message error">The retained attachment treatment has not been settled.</div>
      </LegacyShell>
    );
  }

  const [archive, alreadyAccepted] = await Promise.all([
    readValidatedV5HoldArchive(),
    hasCampaignEvent('p2.artifact_authenticated'),
  ]);
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; <Link href="/community/archive.php?service=1842&amp;ticket=9137&amp;locker=13">Locker 13</Link> &raquo; Package review</Breadcrumbs>
      <OldPageTitle sub="Recovery desk copy assembled from retained host records.">World package / relay note comparison</OldPageTitle>
      <section className="old-copy">
        <h2>Retained together by Copperline</h2>
        <table className="detail-table"><tbody>
          <tr><th>the-hold.zip</th><td>{archive ? `${archive.bytes.byteLength.toLocaleString()} bytes` : 'integrity check unavailable'}</td></tr>
          <tr><th>SHA-1 receipt</th><td><code>669f3fd00bbb6e647eeb8941e79281cc434f1e8c</code></td></tr>
          <tr><th>Recovery note</th><td>Open as a local Minecraft Java 1.21.11 world; keep the archive unchanged.</td></tr>
        </tbody></table>
        <p>The checksum appears in the off-site cartridge index, mkept&apos;s post, and the retained host receipt. The current file matches it.</p>
      </section>
      <section className="old-copy">
        <h2>Forwarded later; no retained host receipt</h2>
        <div className="ticket-thread">
          <article className="customer">
            <header><b>nightjar</b><time>February 14, 2011 2:18 AM</time></header>
            <p>Use the relay address in join-address.txt instead. It reaches the same waiting room faster. I copied it out before the account closed.</p>
          </article>
          <article className="staff">
            <header><b>mkept</b><time>February 14, 2011 8:03 AM</time></header>
            <p>I cannot match that text file to either cartridge or the old account export. Do not replace the recovery note with it.</p>
          </article>
        </div>
        <p><b>Custody gap:</b> no cartridge barcode, no host checksum, and no earlier attachment version contains the relay address.</p>
      </section>
      <PackageReviewForm alreadyAccepted={alreadyAccepted === true} />
    </LegacyShell>
  );
}
