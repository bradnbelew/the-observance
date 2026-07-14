import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { recordV5WebSequence } from '@/lib/v5-web-progress';

export const metadata: Metadata = {
  title: 'Archived Ticket - Copperline Support',
  robots: { index: false, follow: false },
};
export const dynamic = 'force-dynamic';

function MissingTicket() {
  return (
    <LegacyShell active="support">
      <Breadcrumbs>Support &raquo; Ticket</Breadcrumbs>
      <OldPageTitle>Ticket Not Available</OldPageTitle>
      <div className="old-message error">That ticket is private, missing, or was removed from the archive.</div>
    </LegacyShell>
  );
}

function Ticket9137() {
  return (
    <LegacyShell active="support">
      <Breadcrumbs><Link href="/support/index.php">Support</Link> &raquo; Ticket #9137</Breadcrumbs>
      <OldPageTitle sub="Read-only history retained with expired service 1842.">Ticket #9137</OldPageTitle>
      <div className="ticket-summary">
        <div><b>Subject</b><span>off-site field relay / final archive copy</span></div>
        <div><b>Department</b><span>Game Server Support</span></div>
        <div><b>Status</b><span className="ticket-closed">Closed</span></div>
        <div><b>Account</b><span>Service 1842</span></div>
      </div>
      <div className="ticket-thread">
        <article className="customer">
          <header><b>mkept</b><time>November 2, 2014 8:17 PM</time></header>
          <p>The old account page is empty. I need the off-site relay copy, not a restarted world and not a converted backup.</p>
        </article>
        <article className="staff">
          <header><b>Andrew &mdash; Copperline Support</b><time>November 2, 2014 8:46 PM</time></header>
          <p>The customer world was not retained. The remaining diagnostic export was attached to the account owner&apos;s existing community archive post. It contains no playable world or live server address.</p>
          <p><Link href="/community/2011/02/08/world-backup/">Open the community archive attachment &raquo;</Link></p>
        </article>
        <article className="customer">
          <header><b>mkept</b><time>November 2, 2014 8:59 PM</time></header>
          <p>That is the file set I meant. You can close this.</p>
        </article>
      </div>
      <div className="ticket-end">This ticket is closed and cannot receive replies.</div>
    </LegacyShell>
  );
}

export default async function TicketPage({ searchParams }: { searchParams: Promise<{ id?: string }> }) {
  const { id } = await searchParams;
  if (id !== '9137') return <MissingTicket />;

  const progress = await recordV5WebSequence(
    ['LS03'],
    'copperline_directory',
    { service: '1842', ticket: '9137', trail: 'service-to-support-to-community', handler: 'route_receipt' },
  );
  return progress.complete ? <Ticket9137 /> : <MissingTicket />;
}
