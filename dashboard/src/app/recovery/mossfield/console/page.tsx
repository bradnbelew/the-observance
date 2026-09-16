import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { allMorrowGates, buildFullRehearsalReceipt, fullRehearsalDigest, mediaRequirements } from '@/lib/morrow-full-rehearsal';

export const metadata: Metadata = { title: 'Mossfield Recovery Console - Copperline' };
export const dynamic = 'force-static';

export default function MossfieldConsolePage() {
  const gates = allMorrowGates();
  const media = mediaRequirements();
  const receipt = buildFullRehearsalReceipt();
  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/recovery/mossfield">Recovery Archive</Link> &raquo; Local console</Breadcrumbs>
    <OldPageTitle sub="Local full-spine rehearsal contract. No production target is bound.">Mossfield Recovery Console</OldPageTitle>
    <div className="old-alert"><b>Local only:</b> all fifteen gates are modeled below; Minecraft, Discord, Supabase, director controls, and production remain disconnected.</div>
    <table className="old-data-table"><tbody>
      <tr><th>Release</th><td>{receipt.releaseId}</td></tr>
      <tr><th>Journal head</th><td><code>{receipt.journalHead}</code></td></tr>
      <tr><th>Digest</th><td><code>{fullRehearsalDigest()}</code></td></tr>
      <tr><th>Production</th><td>blocked; no external mutation allowed</td></tr>
    </tbody></table>
    <section className="old-copy"><h2>Full gate spine</h2><table className="old-data-table"><thead><tr><th>Gate</th><th>Surface</th><th>Player action</th><th>Status</th></tr></thead><tbody>{gates.map((gate) => <tr key={gate.id}><td><Link href={`/recovery/mossfield/gates/${gate.id.toLowerCase()}`}>{gate.id}</Link><br /><small>{gate.title}</small></td><td>{gate.surfaces.join(', ')}</td><td>{gate.input}</td><td>local contract</td></tr>)}</tbody></table></section>
    <section className="old-copy"><h2>Media intake</h2><table className="old-data-table"><thead><tr><th>Asset</th><th>Type</th><th>Required observation</th><th>Status</th></tr></thead><tbody>{media.map((asset) => <tr key={asset.key}><td><code>{asset.key}</code><br /><small>{asset.title}</small></td><td>{asset.media_type}</td><td>{asset.required_observation}</td><td>needs source, final, hashes, custody, accessibility</td></tr>)}</tbody></table><p><Link href="/recovery/mossfield/media">Open media production checklist</Link></p></section>
    <section className="old-copy"><h2>Unbuilt runtime lanes</h2><ul className="old-checks"><li><b>Paper world</b><span>Mossfield geography, native clue objects, scares, and shutdown route still need implementation.</span></li><li><b>Media</b><span>All twelve handmade assets need final files, hashes, custody notes, and accessible equivalents.</span></li><li><b>Discord</b><span>G10 safe fragments and post-shutdown silence need database-bound worker implementation.</span></li><li><b>Director</b><span>Controls remain disabled until allowlists, receipts, recovery, and finale arm/start are built.</span></li></ul></section>
    <p><Link href="/recovery/mossfield/test-guide">First-touch test guide</Link> · <Link href="/api/rehearsal/morrow/full">Open JSON receipt</Link> · <Link href="/api/rehearsal/morrow/media">Media readiness API</Link> · <Link href="/api/rehearsal/morrow/readiness">Production readiness API</Link> · <Link href="/api/rehearsal/morrow/director">Director lock API</Link> · <Link href="/review/morrow">Review dossier</Link></p>
  </LegacyShell>;
}
