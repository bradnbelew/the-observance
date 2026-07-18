import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = { title: 'that room was a service counter - Copperline Community', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function ServiceCounterPost() {
  const recurated = await hasCampaignEvent('p5.civic_gallery_recurated');
  if (recurated !== true) {
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
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; ashfield &raquo; February 2011</Breadcrumbs>
      <OldPageTitle sub="Posted by ashfield on February 16, 2011 at 6:42 PM; archive description corrected after the service-card review.">that room was a service counter</OldPageTitle>
      <article className="old-copy">
        <p>I found the uncropped frame from the room we kept calling the shrine. The left end has soot, wick shears, and a vent hood. The low middle shelf has school chalk and a child&apos;s bird drawing. The stone basin at the right has sample rings, not candle wax.</p>
        <p>It looks like three kinds of work shared one public counter. The carved names may have survived because later clerks reused them, not because the room began as a ritual place.</p>
      </article>
      <section className="old-copy" aria-labelledby="service-replies">
        <h2 id="service-replies">Replies</h2>
        <div className="ticket-thread">
          <article className="customer"><header><b>kilnmouse</b><time>February 16, 2011 7:05 PM</time></header><p>That bird has a teacher&apos;s cap. If this was a temple, somebody was at least having a normal day in it.</p></article>
          <article className="customer"><header><b>mkept</b><time>February 16, 2011 7:31 PM</time></header><p>Keep the early work cards beside the later penalties. The same headings are not proof that the rules meant the same thing.</p></article>
          <article className="customer"><header><b>quietorbit</b><time>February 16, 2011 9:12 PM</time></header><p>The penalty plaques cover old screw holes. Whatever they were for, they were added after the counter was already in use.</p></article>
        </div>
      </section>
      <section className="old-copy">
        <h2>Archive change</h2>
        <p>The group kept the early service cards in public view and moved the later penalty copies into evidence custody. Copperline now indexes this image as <b>lamp, school, and sample service counter</b>. The older shrine caption remains in revision history.</p>
      </section>
    </LegacyShell>
  );
}
