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
    name: 'Enter Mossfield',
    route: '/recovery/mossfield/console',
    action: 'Use the recovered handoff to join the local Paper rehearsal as a normal player. Begin at the abandoned freight stop.',
    expected: 'Mossfield is a coherent ruined settlement. No Copper Terminal, B06 cell, answer menu, or investigation room appears.',
  },
  {
    phase: '07',
    name: "Cairn's storehouse",
    route: '/recovery/mossfield/console',
    action: "Read the service-office mount notice and Cairn's bench notes, then follow the work routine through the storehouse objects.",
    expected: 'The desk index appears after survey, mend, mark, and light. G05 is retained without consuming evidence.',
  },
  {
    phase: '08',
    name: 'Maintenance cache',
    route: '/recovery/mossfield/console',
    action: 'From the service-office door, travel 11 west and 24 north to survey cairn 03, then open its cache.',
    expected: "Rookery's maintenance copy establishes the first impossible restoration and points toward June's lighthouse photograph.",
  },
  {
    phase: '09',
    name: 'Media production checklist',
    route: '/recovery/mossfield/media',
    action: 'Review every release-blocking media artifact and the exact source/final/accessibility files required.',
    expected: 'All twelve assets remain release-blocking until files, hashes, custody, accessibility, and safety review exist.',
  },
  {
    phase: '10',
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
    <div className="old-alert"><b>Scope:</b> the website trail is playable through G03 and the local Minecraft rehearsal through G06. The remaining spine is inspectable contract data; production launch stays blocked.</div>
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
        <li><b>Known stop point</b><span>G04-G06 are locally playable. Do not claim G07-G15, live hosting, Discord, database projection, or director controls are launch-ready.</span></li>
      </ul>
      <p>{receiptLinks.map(([href, label], index) => <span key={href}>{index > 0 ? ' · ' : ''}<Link href={href}>{label}</Link></span>)}</p>
    </section>
  </LegacyShell>;
}
