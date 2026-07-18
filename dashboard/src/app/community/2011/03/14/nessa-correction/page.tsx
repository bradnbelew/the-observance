import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = { title: 'correction to the cistern file - Copperline Community', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function NessaCorrectionPost() {
  const cleared = await hasCampaignEvent('p7.nessa_publicly_cleared');
  if (cleared !== true) return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs><OldPageTitle>Archived Post Not Available</OldPageTitle><div className="old-message error">This post is not in the current public index.</div></LegacyShell>;
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; mkept &raquo; March 2011</Breadcrumbs>
      <OldPageTitle sub="Posted by mkept on March 14, 2011 at 1:09 AM; public correction attached after independent material, labor, and record findings.">correction to the cistern file</OldPageTitle>
      <article className="old-copy">
        <p>The old export calls Nessa Vale an operator removed for variance. That description is wrong.</p>
        <p><b>Material:</b> the lower intake lost pressure before cloth fibers appeared. Genuine paired-warp cloth was diverted. The installed single-warp cloth shed into later samples.</p>
        <p><b>Record:</b> Toma took relief after ninth bell. The clean rota moved him to eighth and reassigned the later fiber samples to Nessa. Her complaint history was edited in the same direction.</p>
        <p><b>Conduct:</b> Nessa separated upper and lower draws, sealed them in order, and reported grit before the cloth began shedding. She followed the procedure.</p>
      </article>
      <section className="old-copy" aria-labelledby="nessa-replies"><h2 id="nessa-replies">Replies</h2><div className="ticket-thread">
        <article className="customer"><header><b>ashfield</b><time>March 14, 2011 1:24 AM</time></header><p>Put her name in the caption, not only in the attachment metadata. People should not need our folder structure to find the correction.</p></article>
        <article className="customer"><header><b>rookline</b><time>March 14, 2011 1:47 AM</time></header><p>Keep the accusation file too. Moving it into evidence is different from letting it remain the public finding.</p></article>
        <article className="customer"><header><b>wren-home</b><time>March 14, 2011 2:03 AM</time></header><p>Does correcting the name change what the lower copy shows?</p></article>
        <article className="customer"><header><b>mkept</b><time>March 14, 2011 2:11 AM</time></header><p>I do not know. It changes what we are willing to repeat.</p></article>
      </div></section>
      <section className="old-copy"><h2>Current archive note</h2><p>Nessa Vale is cleared by original exhibits. The accusation remains preserved as evidence of the edited inquiry, not as a valid finding.</p></section>
    </LegacyShell>
  );
}
