import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { P4_COPPERLINE_ROUTE } from '@/lib/copperline-p4-route';

export const metadata: Metadata = { title: 'old copy opens without mods - Copperline Community' };

export default function OldCopyPostPage() {
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href={P4_COPPERLINE_ROUTE.community}>Community</Link> &raquo; mkept &raquo; February 2011</Breadcrumbs>
      <OldPageTitle sub="Posted by mkept on February 11, 2011 at 11:18 PM">old copy opens without mods</OldPageTitle>
      <article className="old-copy">
        <p>The recovered world opens in the listed Java version. I removed player data and chat. If you were on the original server and want something private removed, send me the path rather than posting it here.</p>
        <p>Both retained cartridges open to the same waiting room. The notice on the east wall is not the same in both copies. File dates from the damaged guest do not settle which came first.</p>
        <p>Copperline kept the cartridge barcode order in <Link href={P4_COPPERLINE_ROUTE.custodyTicket}>support ticket 2184</Link>. That is the order I am using. The earlier <Link href={P4_COPPERLINE_ROUTE.priorBackupPost}>backup post</Link> has the public checksum notes.</p>
      </article>
      <section className="old-copy" aria-labelledby="replies-heading">
        <h2 id="replies-heading">Replies</h2>
        <div className="ticket-thread">
          <article className="customer">
            <header><b>quietorbit</b><time>February 12, 2011 12:02 AM</time></header>
            <p>Thank you for taking the names out. I do not recognize the build, but the empty waiting room is upsetting in a way I cannot explain.</p>
          </article>
          <article className="customer">
            <header><b>ashfield</b><time>February 12, 2011 9:41 AM</time></header>
            <p>I took stills of both walls. Same camera position, different notice. I sent the contact sheet to you instead of attaching it here because one frame still had a player name.</p>
          </article>
          <article className="customer">
            <header><b>mkept</b><time>February 12, 2011 10:06 AM</time></header>
            <p>Got it. Keep the original. I will only post a redacted comparison after the cartridge order is settled.</p>
          </article>
        </div>
      </section>
      <p><Link href={P4_COPPERLINE_ROUTE.accountFilter}>More posts by mkept</Link></p>
    </LegacyShell>
  );
}
