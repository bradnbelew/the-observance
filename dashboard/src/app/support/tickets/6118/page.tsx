import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { supportTicket6118 } from '@/lib/morrow-copperline-seed';

export const metadata: Metadata = { title: 'Ticket 6118 - Copperline Support' };
export const dynamic = 'force-static';

export default function Ticket6118Page() {
  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/support/index.php">Support</Link> &raquo; Ticket 6118</Breadcrumbs>
    <OldPageTitle sub="Read-only support ticket imported from the retired helpdesk.">Ticket 6118</OldPageTitle>
    <table className="old-data-table"><tbody>
      <tr><th>Opened</th><td>{supportTicket6118.opened}</td></tr>
      <tr><th>Queue</th><td>{supportTicket6118.queue}</td></tr>
      <tr><th>Customer</th><td>{supportTicket6118.customer}</td></tr>
      <tr><th>Status</th><td>{supportTicket6118.status}</td></tr>
      <tr><th>Attachment</th><td>{supportTicket6118.attachment}</td></tr>
    </tbody></table>
    <div className="old-alert"><b>Internal note:</b> {supportTicket6118.note}</div>
    <blockquote className="old-testimonial"><p>{supportTicket6118.signature}</p><span>signature block retained from staff reply</span></blockquote>
    <p><Link href="/community/forum?thread=6118">Forum alias trail</Link> · <Link href="/status/incident-6118">Status residue</Link> · <Link href="/recovery/mossfield">Recovery lookup</Link></p>
  </LegacyShell>;
}
