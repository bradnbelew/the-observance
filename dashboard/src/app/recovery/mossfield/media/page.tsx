import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { mediaIntakeSummary } from '@/lib/morrow-full-rehearsal';

export const metadata: Metadata = { title: 'Mossfield Media Checklist - Copperline' };
export const dynamic = 'force-static';

export default function MossfieldMediaChecklistPage() {
  const intake = mediaIntakeSummary();

  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/recovery/mossfield">Recovery Archive</Link> &raquo; <Link href="/recovery/mossfield/console">Local console</Link> &raquo; Media checklist</Breadcrumbs>
    <OldPageTitle sub="Release-blocking production media intake. This page does not serve player-facing assets.">Mossfield Media Checklist</OldPageTitle>
    <div className="old-alert"><b>Blocked:</b> all twelve media artifacts still need authored source files, release files, SHA-256 hashes, custody notes, accessibility equivalents, and safety review before launch.</div>
    <table className="old-data-table"><tbody>
      <tr><th>Total assets</th><td>{intake.totalAssets}</td></tr>
      <tr><th>Release blocking</th><td>{intake.releaseBlockingAssets}</td></tr>
      <tr><th>Release ready</th><td>{intake.releaseReadyAssets}</td></tr>
      <tr><th>Manifest</th><td><code>{intake.intakePolicy.manifest}</code></td></tr>
      <tr><th>Required fields per asset</th><td>{intake.requiredFieldCount}</td></tr>
    </tbody></table>
    <section className="old-copy">
      <h2>Required intake fields</h2>
      <ul className="old-checks">
        {intake.assets[0].requiredBeforeRelease.map((field) => <li key={field}><b>{field}</b><span>Must be present in the media manifest before release-ready can become true.</span></li>)}
      </ul>
    </section>
    <section className="old-copy">
      <h2>Asset queue</h2>
      <table className="old-data-table">
        <thead><tr><th>Asset</th><th>Gate</th><th>Files</th><th>Acceptance observation</th></tr></thead>
        <tbody>{intake.assets.map((asset) => <tr key={asset.key}>
          <td><code>{asset.key}</code><br /><small>{asset.title}</small><br /><small>{asset.media_type}</small></td>
          <td>{asset.gates.length ? asset.gates.join(', ') : 'G01 entry'}</td>
          <td><code>{asset.sourceFile}</code><br /><code>{asset.deliveryFile}</code><br /><code>{asset.accessibilityFile}</code></td>
          <td>{asset.required_observation}<br /><small>{asset.accessible_equivalent}</small></td>
        </tr>)}</tbody>
      </table>
    </section>
    <p><Link href="/api/rehearsal/morrow/media">Open media readiness JSON</Link> · <Link href="/recovery/mossfield/console">Return to console</Link></p>
  </LegacyShell>;
}
