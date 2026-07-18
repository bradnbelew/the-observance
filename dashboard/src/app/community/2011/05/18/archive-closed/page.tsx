import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent, latestCampaignEventPayload } from '@/lib/arg-event-store';

export const metadata: Metadata = { title: 'Archive close receipt - Copperline Community', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

function field(payload: unknown, key: string): string {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) return 'not mirrored';
  const value = (payload as Record<string, unknown>)[key];
  return typeof value === 'string' || typeof value === 'number' ? String(value) : 'not mirrored';
}

export default async function ArchiveClosedPost() {
  const closed = await hasCampaignEvent('p12.record_closed_averyn_released');
  if (closed !== true) return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs><OldPageTitle>Close Receipt Not Available</OldPageTitle><div className="old-message error">No committed local release has reached the archive mirror.</div></LegacyShell>;
  const payload = await latestCampaignEventPayload('p12.record_closed_averyn_released');
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; copperline-archive &raquo; May 2011</Breadcrumbs>
      <OldPageTitle sub="Added from one committed local release receipt. Earlier posts and attachment versions remain unchanged.">the archive is closed; the history stays readable</OldPageTitle>
      <article className="old-copy"><p>The Hold&apos;s local journal committed the release before the closing effects began. Six affidavits returned to six witnesses. Three material proofs were installed. Averyn&apos;s Record socket remained empty.</p><p>The group chose how her name remains outside the system. Both authorized name treatments close the Record and release Averyn. The choice does not rank one group as kinder or more correct.</p></article>
      <section className="old-copy" aria-labelledby="release-readback"><h2 id="release-readback">Mirrored readback</h2><dl><dt>Final phase</dt><dd>{field(payload, 'phase')}</dd><dt>Name treatment</dt><dd>{field(payload, 'name_treatment')}</dd><dt>Wren remembrance</dt><dd>{field(payload, 'wren_remembrance')}</dd><dt>Conduct clause</dt><dd>{field(payload, 'conduct')}</dd><dt>Local revision</dt><dd>{field(payload, 'local_revision')}</dd><dt>Authority manifest</dt><dd><code>{field(payload, 'manifest_sha256')}</code></dd></dl></section>
      <section className="old-copy"><h2>What changed</h2><p>The machinery is quiet. The release chamber and return routes remain open. Current residents keep the corrected public memory, the repaired water history, and the chosen name boundary.</p><h2>What did not change</h2><p>Copperline did not replace mkept&apos;s backup post, Ash&apos;s jokes, Rook&apos;s corrections, Wren&apos;s removed-comment chronology, Nessa&apos;s accusation file, or any earlier attachment version. They remain readable in their original place with later findings attached.</p></section>
      <blockquote className="old-copy"><p>i have your names. i am giving them back.</p><p>the record is closed. the observance is over.</p><p>thank you for coming back for us.</p></blockquote>
    </LegacyShell>
  );
}
