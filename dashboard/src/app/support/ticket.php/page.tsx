import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { P4_COPPERLINE_ROUTE } from '@/lib/copperline-p4-route';
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
          <p>The old account page is empty. I need the North Annex recovery copy we used for remote dispatch, not a restarted customer world.</p>
        </article>
        <article className="staff">
          <header><b>Andrew &mdash; Copperline Support</b><time>November 2, 2014 8:46 PM</time></header>
          <p>We located the single-player recovery image on the off-site cartridge. It remains attached to the account owner&apos;s community archive post. The dispatch destination was not copied into this ticket.</p>
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

function Ticket2184() {
  return (
    <LegacyShell active="support">
      <Breadcrumbs><Link href="/support/index.php">Support</Link> &raquo; Ticket #2184</Breadcrumbs>
      <OldPageTitle sub="Read-only history retained with an expired game service.">Ticket #2184</OldPageTitle>
      <div className="ticket-summary">
        <div><b>Subject</b><span>recovered cartridge order</span></div>
        <div><b>Department</b><span>Backup and Recovery</span></div>
        <div><b>Status</b><span className="ticket-closed">Closed</span></div>
        <div><b>Account</b><span>Service 1842</span></div>
      </div>
      <div className="ticket-thread">
        <article className="customer">
          <header><b>mkept</b><time>February 9, 2011 12:31 AM</time></header>
          <p>Please keep the cartridge sequence in the attachment table. The filenames and modified times came from the damaged guest and cannot order the copies. Cartridge 03 was imaged before 04; that is the only sequence I trust.</p>
        </article>
        <article className="staff">
          <header><b>Amy &mdash; Copperline Support</b><time>February 9, 2011 8:12 AM</time></header>
          <p>Sequence 03 then 04 is retained from the barcode scan. Neither image was mounted by support. The recovery node stayed within two seconds of the billing host during both reads.</p>
          <p>The original upload rows were collapsed when this account expired. The archive can rebuild a read-only copy from the retained table.</p>
          <p><Link href={P4_COPPERLINE_ROUTE.retainedAttachments}>Open the retained attachment table &raquo;</Link></p>
        </article>
        <article className="customer">
          <header><b>mkept</b><time>February 9, 2011 8:27 AM</time></header>
          <p>That settles the order. Please leave the original rows closed and make any recovery read-only.</p>
        </article>
      </div>
      <div className="ticket-end">This ticket is closed and cannot receive replies.</div>
    </LegacyShell>
  );
}

export default async function TicketPage({ searchParams }: { searchParams: Promise<{ id?: string }> }) {
  const { id } = await searchParams;
  if (id === '2184') return <Ticket2184 />;
  if (id !== '9137') return <MissingTicket />;

  const progress = await recordV5WebSequence(
    ['LS03'],
    'copperline_directory',
    { service: '1842', ticket: '9137', trail: 'service-to-support-to-community', handler: 'route_receipt' },
  );
  return progress.complete ? <Ticket9137 /> : <MissingTicket />;
}
