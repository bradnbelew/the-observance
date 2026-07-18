import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = { title: 'Removed comment chronology - Copperline Archive', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function WrenModerationArchive() {
  const leakKnown = await hasCampaignEvent('p9.leak_window_proven');
  const confronted = await hasCampaignEvent('p10.wren_confronted');
  const remembered = await hasCampaignEvent('p10.wren_remembrance_committed');
  if (leakKnown !== true) return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs><OldPageTitle>Moderation Record Not Available</OldPageTitle><div className="old-message error">No retained comment chronology matches the current archive window.</div></LegacyShell>;
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive &raquo; Moderation</Breadcrumbs>
      <OldPageTitle sub="Retained database chronology. The removed comment body is not restored as a new confession.">comment 4417 / moderation history</OldPageTitle>
      <article className="old-copy"><p><b>April 27, 2011 2:12 AM:</b> wren-home posted a reply to a private-group mirror. The body hash is retained; the body itself was removed before the archive crawl.</p><p><b>2:18 AM:</b> wren-home edited the reply. The first and second bodies have different hashes.</p><p><b>2:20 AM:</b> moderator janet-c hid the reply after Rook reported that it quoted a route detail not present in the public thread.</p><p><b>2:31 AM:</b> mkept froze the thread and exported the moderation row. He did not restore either body.</p><p><b>May 1, 2011:</b> the public route attachment was uploaded. Its north-brace detail matches the second retained body hash family, but the chronology alone cannot reveal the missing words.</p></article>
      <section className="old-copy"><h2>What this record can support</h2><p>It fixes account, edit, moderation, and public-upload order. It supports access and timing. It does not, by itself, prove packet authorship or motive.</p></section>
      {confronted === true ? <section className="old-copy"><h2>Finding attached by the current group</h2><p>The moderation row is now attached to the independent packet progression and Rook counter-mark finding. The old removed body stays removed; Copperline does not manufacture a confession to fill the gap.</p></section> : <div className="old-message">The moderation chronology is available. No transmission finding has been attached.</div>}
      {remembered === true ? <section className="old-copy"><h2>Remembrance receipt attached</h2><p>The group committed a remembrance treatment in the Hold. Copperline preserves that choice beside the fixed attribution facts; it does not rewrite the moderation history to agree with the judgment.</p></section> : null}
    </LegacyShell>
  );
}
