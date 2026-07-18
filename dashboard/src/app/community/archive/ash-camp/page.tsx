import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';
import { CampBiographyForm } from './CampBiographyForm';

export const metadata: Metadata = { title: 'Ash Camp owner recovery - Copperline Archive', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function AshCampArchive() {
  const worksRepaired = await hasCampaignEvent('p8.hold_systems_repaired');
  const restored = await hasCampaignEvent('p9.company_biographies_restored');
  const leakWindow = await hasCampaignEvent('p9.leak_window_proven');
  if (worksRepaired !== true) return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs><OldPageTitle>Archive Set Not Available</OldPageTitle><div className="old-message error">The linked maintenance and camp records are not in the current index.</div></LegacyShell>;
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive &raquo; Ash Camp</Breadcrumbs>
      <OldPageTitle sub="Recovered from account posts, work copies, camp objects, and retained attachment history. Originals remain read-only.">Ash Camp owner recovery</OldPageTitle>
      <article className="old-copy"><p>The old export reduced this camp to four stations. The surviving material crosses those boundaries: a shared supper sheet, repairs made for one another, borrowed camera batteries, a chair Rook kept fixing, and route notes copied between hands.</p><p>mkept maintained the server and its checksums. Ash filmed and noticed visual changes. Rook built, measured, and left counter-marks. Wren carried route knowledge, kept people talking, and later changed the distances in different copies. None of those facts alone identifies a leak.</p></article>
      <CampBiographyForm restored={restored === true} />
      {restored === true && <section className="old-copy" aria-labelledby="camp-thread"><h2 id="camp-thread">Restored ordinary thread</h2><div className="ticket-thread">
        <article className="customer"><header><b>ashfield</b><time>April 19, 2011 11:08 PM</time></header><p>Rook says the north shelf is not a shelf because Wren sits on it. I say repeated use has settled the classification.</p></article>
        <article className="customer"><header><b>rookline</b><time>April 19, 2011 11:13 PM</time></header><p>It is a brace. It is holding up the wall. Please stop helping.</p></article>
        <article className="customer"><header><b>wren-home</b><time>April 19, 2011 11:21 PM</time></header><p>I moved. The note stayed because mkept put his cables on it.</p></article>
        <article className="customer"><header><b>mkept</b><time>April 19, 2011 11:25 PM</time></header><p>I have reviewed the evidence and blame the beans.</p></article>
      </div></section>}
      {leakWindow === true
        ? <section className="old-copy" aria-labelledby="leak-window"><h2 id="leak-window">Private-window finding</h2><p>The release board was complete. Rook&apos;s north-brace revision entered the Witness Spool after his private counter-mark and before any public upload. The source was inside the four-person company. P9 does not name which person sent it.</p><p>The archive now preserves the last private version, the first spool version, and the later public version side by side.</p></section>
        : restored === true ? <div className="old-message">The owner cards are restored. The private revision window is still open for group review.</div> : null}
    </LegacyShell>
  );
}
