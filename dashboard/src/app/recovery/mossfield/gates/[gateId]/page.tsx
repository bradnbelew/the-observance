import type { Metadata } from 'next';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { gateById, gateReceiptById } from '@/lib/morrow-full-rehearsal';

export const dynamic = 'force-static';
export const metadata: Metadata = { title: 'Mossfield Gate Detail - Copperline' };

export default async function GateDetailPage({ params }: { params: Promise<{ gateId: string }> }) {
  const { gateId } = await params;
  const gate = gateById(gateId);
  if (!gate) notFound();
  const receipt = gateReceiptById(gate.id);
  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/recovery/mossfield/console">Mossfield Console</Link> &raquo; {gate.id}</Breadcrumbs>
    <OldPageTitle sub={`Act ${gate.act} · owner ${gate.owner}`}>{gate.id}: {gate.title}</OldPageTitle>
    <table className="old-data-table"><tbody>
      <tr><th>Purpose</th><td>{gate.purpose}</td></tr>
      <tr><th>Surfaces</th><td>{gate.surfaces.join(', ')}</td></tr>
      <tr><th>Mechanics</th><td>{gate.mechanics.join(', ')}</td></tr>
      <tr><th>Event</th><td><code>{gate.eventKey}</code></td></tr>
      <tr><th>Discovery</th><td>{gate.discovery ? `${gate.discovery.id}: ${gate.discovery.reveals}` : 'No required discovery'}</td></tr>
      <tr><th>Failure behavior</th><td>{gate.failure_behavior}</td></tr>
      <tr><th>Accessibility</th><td>{gate.accessible_equivalent}</td></tr>
      <tr><th>Receipt</th><td><code>{receipt?.receiptSha256}</code></td></tr>
    </tbody></table>
    <section className="old-copy"><h2>Evidence vectors</h2><ul className="old-checks">{gate.evidence.map((item) => <li key={item}><b>{item}</b><span>Required clue vector retained from authority.</span></li>)}</ul></section>
    <section className="old-copy"><h2>Local implementation note</h2><p>{gate.id === 'G01' || gate.id === 'G02' || gate.id === 'G03' ? 'This gate has a local Copperline static route in the current slice.' : 'This gate is modeled in the full local receipt and still needs runtime implementation.'}</p><p><Link href={`/api/rehearsal/morrow/gates/${gate.id.toLowerCase()}`}>Open gate JSON</Link> · <Link href="/recovery/mossfield/console">Back to full console</Link></p></section>
  </LegacyShell>;
}
