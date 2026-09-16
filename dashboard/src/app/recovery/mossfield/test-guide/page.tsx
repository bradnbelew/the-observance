import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';

export const metadata: Metadata = { title: 'Mossfield First-Touch Test Guide - Copperline' };
export const dynamic = 'force-static';

const testSteps = [
  {
    phase: '01',
    name: 'Ordinary host landing',
    route: '/game-servers.php',
    action: 'Start from the old Copperline hosting listing and follow normal page links before touching the recovery archive.',
    expected: 'The page feels like a mundane 2011 host listing and does not expose production controls.',
  },
  {
    phase: '02',
    name: 'Forum residue',
    route: '/community/forum?thread=6118',
    action: 'Read the staff/community thread for alias continuity and support residue.',
    expected: 'Players can derive the employee alias path without needing hidden browser tools.',
  },
  {
    phase: '03',
    name: 'Support ticket',
    route: '/support/tickets/6118',
    action: 'Compare the ticket language with the forum thread and status residue.',
    expected: 'The Copperline support trail points toward the same incident and recovery lookup.',
  },
  {
    phase: '04',
    name: 'Status residue',
    route: '/status/incident-6118',
    action: 'Use the stable uptime value instead of the edited public timestamp.',
    expected: 'The lookup key can be inferred from visible page content.',
  },
  {
    phase: '05',
    name: 'Recovery lookup',
    route: '/recovery/mossfield?alias=iona&key=041722',
    action: 'Submit the recovered alias and key through the Mossfield lookup page.',
    expected: 'The static handoff opens and reports production disabled.',
  },
  {
    phase: '06',
    name: 'Local spine console',
    route: '/recovery/mossfield/console',
    action: 'Inspect all fifteen modeled gates, local receipt hash, and disabled runtime lanes.',
    expected: 'G01-G15 are modeled as local contract data; Minecraft, Discord, Supabase, and director actions stay disconnected.',
  },
  {
    phase: '07',
    name: 'Media production checklist',
    route: '/recovery/mossfield/media',
    action: 'Review every release-blocking media artifact and the exact source/final/accessibility files required.',
    expected: 'All twelve assets remain release-blocking until files, hashes, custody, accessibility, and safety review exist.',
  },
  {
    phase: '08',
    name: 'Machine receipts',
    route: '/api/rehearsal/morrow/readiness',
    action: 'Open the readiness JSON, then compare the media and full-spine JSON receipts.',
    expected: 'The APIs report first-playable review ready, production not ready, productionMutation false, and five hard blockers.',
  },
] as const;

const receiptLinks = [
  ['/api/rehearsal/morrow/full', 'Full-spine receipt'],
  ['/api/rehearsal/morrow/media', 'Media readiness'],
  ['/api/rehearsal/morrow/readiness', 'Production readiness'],
  ['/api/rehearsal/morrow/director', 'Director lock'],
] as const;

export default function MossfieldTestGuidePage() {
  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/support/index.php">Support</Link> &raquo; <Link href="/recovery/mossfield">Recovery Archive</Link> &raquo; First-touch test guide</Breadcrumbs>
    <OldPageTitle sub="Built-so-far walkthrough for reviewers. No production target is mutated.">Mossfield First-Touch Test Guide</OldPageTitle>
    <div className="old-alert"><b>Scope:</b> the website trail is playable through the static G01-G03 handoff. The full G01-G15 spine is inspectable as local contract data, with production launch still blocked.</div>
    <section className="old-copy">
      <h2>Walkthrough</h2>
      <table className="old-data-table">
        <thead><tr><th>Step</th><th>Open</th><th>Tester action</th><th>Expected result</th></tr></thead>
        <tbody>{testSteps.map((step) => <tr key={step.phase}>
          <td><b>{step.phase}</b><br /><small>{step.name}</small></td>
          <td><Link href={step.route}><code>{step.route}</code></Link></td>
          <td>{step.action}</td>
          <td>{step.expected}</td>
        </tr>)}</tbody>
      </table>
    </section>
    <section className="old-copy">
      <h2>Receipt checks</h2>
      <ul className="old-checks">
        <li><b>Production boundary</b><span>Every receipt should keep productionMutation false or production_enabled false.</span></li>
        <li><b>Director lock</b><span>The director endpoint should stay locked, with production enablement forbidden.</span></li>
        <li><b>Media gate</b><span>The media endpoint should list 12 assets and 0 release-ready assets.</span></li>
        <li><b>Known stop point</b><span>Do not claim the Minecraft, Discord, database, or director runtime lanes are launch-ready.</span></li>
      </ul>
      <p>{receiptLinks.map(([href, label], index) => <span key={href}>{index > 0 ? ' · ' : ''}<Link href={href}>{label}</Link></span>)}</p>
    </section>
  </LegacyShell>;
}
