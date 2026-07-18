import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = { title: 'Hold works comparison - Copperline Community', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function HoldWorksPost() {
  const planned = await hasCampaignEvent('p8.intervention_plan_accepted');
  const repaired = await hasCampaignEvent('p8.hold_systems_repaired');
  if (planned !== true) return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs><OldPageTitle>Archived Post Not Available</OldPageTitle><div className="old-message error">This post is not in the current public index.</div></LegacyShell>;
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; ashfield &raquo; April 2011</Breadcrumbs>
      <OldPageTitle sub="Posted by ashfield on April 2, 2011 at 6:40 PM; revised without replacing the original plan.">the Hold works: plan and readback</OldPageTitle>
      <article className="old-copy">
        <p>We are keeping more than one cause on the board. The old fracture, unchanged heat load, empty paired watch, and late-routed closure requests overlap. Iss&apos;s cut widened the failure. It did not create the old fracture.</p>
        <p>His reed, water, and air samples still support the surface claim. They do not make an unreviewed route safe. That distinction stays in the incident file.</p>
        <h2>Accepted order</h2>
        <ol><li>Install the authenticated lower filter.</li><li>Restore the paired coverage lamps.</li><li>Close the mapped pressure bypass.</li><li>Open the staff route after the three system readbacks agree.</li></ol>
        <p>Do not erase the altered office. Its clean chronology is evidence of what the copy preferred.</p>
      </article>
      {repaired === true ? (
        <section className="old-copy" aria-labelledby="works-readback"><h2 id="works-readback">Works readback added by the current group</h2><ul><li>Lower draw: clear, original dirty sample retained.</li><li>Coverage: both lamps live.</li><li>Pressure: return gauge at zero; bypass shut.</li><li>Route: staff gate opened last.</li></ul><p>Dob&apos;s surface patch settled after the same pressure correction. The old Hold repair, his later patch, and the group&apos;s intervention now form one material history.</p></section>
      ) : <div className="old-message">No repair readback has been attached. The accepted plan is public; the works remain unchanged.</div>}
      <section className="old-copy" aria-labelledby="works-replies"><h2 id="works-replies">Replies</h2><div className="ticket-thread">
        <article className="customer"><header><b>rookline</b><time>April 2, 2011 7:11 PM</time></header><p>Keep the before state. If the repair changes the copied office too, we need both versions and the exact order.</p></article>
        <article className="customer"><header><b>wren-home</b><time>April 2, 2011 7:19 PM</time></header><p>I can watch the archive diff while you are in the works. Tell me which change should arrive first.</p></article>
        <article className="customer"><header><b>mkept</b><time>April 2, 2011 7:31 PM</time></header><p>No. Save the diff locally before anyone predicts it in the thread.</p></article>
      </div></section>
      <p className="old-copy"><Link href="/community/2011/03/14/nessa-correction">Earlier: correction to the cistern file</Link></p>
    </LegacyShell>
  );
}
