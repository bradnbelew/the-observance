import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';
import { CampBiographyForm } from './CampBiographyForm';
import { LeakWindowForm } from './LeakWindowForm';

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
      <article className="old-copy" id="evidence-p9-maintenance-trail" data-evidence-id="p9.e02"><p>The old export reduced this camp to four stations. The surviving material crosses those boundaries: a shared supper sheet, repairs made for one another, borrowed camera batteries, a chair that was fixed twice, and route notes copied between hands.</p><p>mkept&apos;s maintenance trail stays plain: restarts, corrupt chunks, mirror checks, and attachment hashes. One unsent draft says, <i>If I vanish from the account, keep the checksums. A person can lose a login without becoming imaginary.</i></p><p>The export did not retain an owner field. Restore the people from their work, habits, replies, and the objects they shared. The later leak question stays open.</p></article>
      <section className="old-copy" aria-labelledby="crossed-traces"><h2 id="crossed-traces">Crossed traces</h2><div className="ticket-thread">
        <article className="staff"><header><b>mkept</b><time>April 18, 2011 8:42 PM</time></header><p>I ran the four hashes again after the outage. The world copy and Rook&apos;s notebook image still match yesterday&apos;s retained set. Please stop renaming the backup folder while it is open.</p></article>
        <article className="customer"><header><b>ashfield</b><time>April 18, 2011 8:51 PM</time></header><p>Battery two is mine. Frame 64 has the lamp twice and Rook only once, which is probably the right ratio. I left the shot card under the beans.</p></article>
        <article className="customer"><header><b>rookline</b><time>April 18, 2011 9:03 PM</time></header><p>NB-17/c is a brace, not a shelf. The two cuts under the joint are my check. If the drawing comes back without them, it is a copy of the plan and not the repaired face.</p></article>
        <article className="customer"><header><b>wren-home</b><time>April 18, 2011 9:16 PM</time></header><p>The east card says twenty-six steps. I wrote twenty-three after walking it with Ash because the lower turn moved. Keep both cards; one is the old path, not a lie.</p></article>
      </div></section>
      <CampBiographyForm restored={restored === true} />
      {restored === true && <section className="old-copy" aria-labelledby="camp-thread"><h2 id="camp-thread">Restored ordinary thread</h2><div className="ticket-thread">
        <article className="customer"><header><b>ashfield</b><time>April 19, 2011 11:08 PM</time></header><p>Rook says the north shelf is not a shelf because Wren sits on it. I say repeated use has settled the classification.</p></article>
        <article className="customer"><header><b>rookline</b><time>April 19, 2011 11:13 PM</time></header><p>It is a brace. It is holding up the wall. Please stop helping.</p></article>
        <article className="customer"><header><b>wren-home</b><time>April 19, 2011 11:21 PM</time></header><p>I moved. The note stayed because mkept put his cables on it.</p></article>
        <article className="customer"><header><b>mkept</b><time>April 19, 2011 11:25 PM</time></header><p>I have reviewed the evidence and blame the beans.</p></article>
      </div></section>}
      {restored === true && <section className="old-copy" aria-labelledby="private-copies"><h2 id="private-copies">Copies from separate clocks</h2>
        <div className="ticket-thread">
          <article className="staff"><header><b>release board / local Polaroid</b><time>April 21, 2011 10:14 PM</time></header><p>Water return, paired lamps, pressure bypass, and staff passage are all marked complete. The marker strokes cross the same coffee ring.</p></article>
          <article className="customer"><header><b>Rook / private build notebook</b><time>April 21, 2011 10:19 PM</time></header><p>North brace NB-17/c. Counter-mark: two short cuts under the lower joint. Shared with the four of us. Do not upload until Ash photographs the repaired face.</p></article>
          <article className="staff"><header><b>Witness Spool / intake controller</b><time>April 21, 2011 10:23 PM</time></header><p>Object NB-17/c accepted with four account labels. The diagram includes the revised upper angle but not Rook&apos;s two physical cuts.</p></article>
          <article className="customer"><header><b>Copperline attachment history</b><time>April 21, 2011 11:02 PM</time></header><p><code>north-brace-NB17c.png</code> uploaded by rookline. First public copy. Ash added the repaired-face photograph at 11:06 PM.</p></article>
        </div>
      </section>}
      {restored === true && <section className="old-copy" aria-labelledby="camp-route-and-board"><h2 id="camp-route-and-board">Route index and near-release board</h2>
        <article id="evidence-p9-copperline-route" data-evidence-id="p9.e06"><h3>Recovered navigation chain</h3><p>The camp notebook says <b>ACCOUNT / COMMUNITY / POST / TICKET / ARCHIVE</b>. Copperline account history supplies service 1842; the support mirror supplies ticket 9137; Ash&apos;s contact sheet supplies locker 13. These are provenance joins across ordinary records, not a numbered clue menu.</p><p><Link href="/community/archive.php?service=1842&amp;ticket=9137&amp;locker=13">Open the earned Locker 13 archive route &raquo;</Link></p></article>
        <article id="evidence-p9-near-release" data-evidence-id="p9.e08"><h3>Board and Witness Spool</h3><p>The board marks the cistern finding, four system repairs, six separate affidavits, the Bridge plan, and an intentionally empty Record position as understood. The Witness Spool contains Rook&apos;s private north-brace revision but omits his two physical counter-cuts. That makes the camp close to release and proves a private transfer without naming its sender.</p></article>
      </section>}
      {restored === true && <LeakWindowForm proven={leakWindow === true} />}
      {leakWindow === true
        ? <section className="old-copy" aria-labelledby="leak-window"><h2 id="leak-window">Private-window finding</h2><p>The release board was complete. Rook&apos;s north-brace revision entered the Witness Spool after his private counter-mark and before any public upload. The source was inside the four-person company. P9 does not name which person sent it.</p><p>The archive now preserves the last private version, the first spool version, and the later public version side by side.</p></section>
        : restored === true ? <div className="old-message">The owner cards are restored. The private revision window is still open for group review.</div> : null}
    </LegacyShell>
  );
}
