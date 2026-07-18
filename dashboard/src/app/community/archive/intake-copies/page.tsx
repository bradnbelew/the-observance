import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { copperlineP4DirectEntries, copperlineP4Entries } from '@/lib/copperline-p4-archive';
import { P4_COPPERLINE_ROUTE } from '@/lib/copperline-p4-route';
import { RestoreArchiveForm } from './RestoreArchiveForm';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = {
  title: 'Ticket 2184 retained copy table - Copperline Community',
  robots: { index: false, follow: false },
};

export const dynamic = 'force-dynamic';

export default async function IntakeCopiesArchivePage() {
  const restored = await hasCampaignEvent('p4.mouth_revision_restored');
  const initialState = restored ? {
    status: 'accepted' as const,
    message: 'The five retained rows are restored below as a read-only copy. The older archive remains unchanged.',
    entries: [...copperlineP4DirectEntries],
  } : {
    status: 'idle' as const,
    message: 'No restore request has been sent.',
  };
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/support/index.php">Support</Link> &raquo; <Link href={P4_COPPERLINE_ROUTE.custodyTicket}>Ticket 2184</Link> &raquo; Attachments</Breadcrumbs>
      <OldPageTitle sub="Read-only cartridge and attachment custody retained with an expired game service.">
        Ticket 2184 / recovered cartridge order
      </OldPageTitle>
      <section className="old-copy" aria-labelledby="ticket-thread-heading">
        <h2 id="ticket-thread-heading">Ticket thread and retained attachments</h2>
        <p>The archive preserves author, time, reply order, and attachment state separately. A quoted or cached copy is not silently substituted for its source.</p>
        <p>Five retained rows are available. Their original upload records remain closed. Restoring them makes a read-only copy below; it does not alter the account or decide what the records mean.</p>
      </section>
      <RestoreArchiveForm initialState={initialState} />
      <section className="old-copy" aria-labelledby="context-heading">
        <h2 id="context-heading">Nearby public archive context</h2>
        <p>The archive retained {copperlineP4Entries.length - copperlineP4DirectEntries.length} nearby public listings, notices, posts, and replies from the same period.</p>
        <p><Link href={P4_COPPERLINE_ROUTE.custodyTicket}>Return to Ticket 2184</Link> · <Link href={P4_COPPERLINE_ROUTE.community}>Return to the community archive</Link></p>
      </section>
      <footer className="ticket-end">Replies, uploads, and account actions are disabled. Prior versions remain read-only.</footer>
    </LegacyShell>
  );
}
