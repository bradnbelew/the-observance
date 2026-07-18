import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = { title: 'six workspace owners - Copperline Community', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function SixWorkspacesPost() {
  const recovered = await hasCampaignEvent('p6.six_responsibilities_acknowledged');
  if (recovered !== true) {
    return (
      <LegacyShell active="community">
        <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs>
        <OldPageTitle>Archived Post Not Available</OldPageTitle>
        <div className="old-message error">This post is not in the current public index.</div>
      </LegacyShell>
    );
  }
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; mkept &raquo; March 2011</Breadcrumbs>
      <OldPageTitle sub="Posted by mkept on March 3, 2011 at 10:14 PM; attachments restored after the six-workspace comparison.">six workspace owners, not one admin</OldPageTitle>
      <article className="old-copy">
        <p>The export calls every owner <code>keeper</code>. That is a permission role, not proof of one account. The work is different enough to separate six people.</p>
        <ul>
          <li>Vaun reconciles stock and leaves private overcounts.</li>
          <li>Mara tracks editions and route substitutions.</li>
          <li>Sella teaches survey transfer through reversed copies.</li>
          <li>Orin marks material limits under the visible face.</li>
          <li>Brann keeps paired watch and redundant time.</li>
          <li>Iss argues from heat, route, and repaired surface evidence.</li>
        </ul>
        <p>Each one helped somebody. Each one also signed, copied, delayed, or hid something that made the later system harder to challenge. Keep their corrections separate.</p>
      </article>
      <section className="old-copy" aria-labelledby="workspace-replies">
        <h2 id="workspace-replies">Replies</h2>
        <div className="ticket-thread">
          <article className="customer"><header><b>ashfield</b><time>March 3, 2011 10:29 PM</time></header><p>The rooms look different too. Vaun keeps tools square to the shelf. Sella leaves charcoal on everything. Orin marked the underside of his own chair.</p></article>
          <article className="customer"><header><b>rookline</b><time>March 4, 2011 12:02 AM</time></header><p>Six owner signatures, six permission grants. The registry changes are written by another hand, but that hand never receives the keeper role.</p></article>
          <article className="customer"><header><b>wren-home</b><time>March 4, 2011 12:18 AM</time></header><p>Could the missing registry owner be a seventh keeper?</p></article>
          <article className="customer"><header><b>mkept</b><time>March 4, 2011 12:31 AM</time></header><p>Maybe a seventh person. Not a seventh holder of that role. Those are different claims.</p></article>
        </div>
      </section>
      <section className="old-copy"><h2>Archive change</h2><p>The group&apos;s six profession records restored the four replies above. Copperline retains the older collapsed export as a separate revision.</p></section>
    </LegacyShell>
  );
}
