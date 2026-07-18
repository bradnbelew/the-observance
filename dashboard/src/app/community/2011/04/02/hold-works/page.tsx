import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';
import { InterventionPlanForm } from './InterventionPlanForm';

export const metadata: Metadata = { title: 'Hold works comparison - Copperline Community', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function HoldWorksPost() {
  const [nessaCleared, planned, unlitCompared, repaired] = await Promise.all([
    hasCampaignEvent('p7.nessa_publicly_cleared'),
    hasCampaignEvent('p8.intervention_plan_accepted'),
    hasCampaignEvent('p8.unlit_house_synthesis_completed'),
    hasCampaignEvent('p8.hold_systems_repaired'),
  ]);
  if (nessaCleared !== true) return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs><OldPageTitle>Archived Post Not Available</OldPageTitle><div className="old-message error">This post is not in the current public index.</div></LegacyShell>;
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; ashfield &raquo; April 2011</Breadcrumbs>
      <OldPageTitle sub="Posted by ashfield on April 2, 2011 at 6:40 PM; current additions retain the original thread.">the Hold works: before-state and intervention</OldPageTitle>
      <article className="old-copy" aria-labelledby="before-state-heading">
        <h2 id="before-state-heading">Before-state retained by ashfield</h2>
        <p>Rook asked that this thread keep the old gauge sketch, the paired-lamp photograph, and the copied-office diff together. He did not agree that one of them explained the others.</p>
        <p>Rook kept four sets of readings because they refused a one-cause account. One begins before Iss arrived. Another changes after a watch relief. The closure stamps do not match the order in the public incident copy. Iss kept his surface samples beside a route objection, not as an answer to it. The group still has to decide what those relationships predict and what can be tested safely.</p>
      </article>
      <section className="old-copy" id="evidence-p8-allocation-gauge" data-evidence-id="p8.e04" aria-labelledby="allocation-gauge-heading">
        <h2 id="allocation-gauge-heading">Cached allocation order and gauge hand</h2>
        <p>The public order reduces pressure to the school and east dwellings. A photographed gauge card from the same issue leaves the favored upper galleries unchanged. Its return hand rises before Iss&apos;s fresh cut and remains high after the public order says the load was reduced.</p>
        <p>Rook&apos;s margin reads: <i>Do not use the clean order as the before-state. Use the hand that was physically on the return pin.</i></p>
      </section>
      <section className="old-copy" aria-labelledby="works-version-heading">
        <h2 id="works-version-heading">Local copies against Record-facing copies</h2>
        <article id="evidence-p8-closure-routing" data-evidence-id="p8.e08"><h3>Closure requests</h3><p>The local lamp, watch, and route copies are dated before the breach. The clean Record-facing copies place all three after it. Their paper, hands, and issue numbers remain independent, so the disagreement is not one bad clock.</p></article>
        <article id="evidence-p8-clean-office" data-evidence-id="p8.e09"><h3>Too-clean office image</h3><p>The copied office keeps the council&apos;s corrected dates and clean manual. It leaves out Eda&apos;s stove patch, Toma&apos;s oil stain, and the brace note added by hand. The supplied still marks those three absences without asking players to trace custom block geometry.</p></article>
        <article id="evidence-p8-copy-before-source" data-evidence-id="p8.e10"><h3>Cabinet sequence</h3><p>A retained archive row describes the copied cabinet in its later arrangement one watch before the source cabinet changed. The file does not say time ran backward. It fixes an order that a causal model must account for without naming what the Dark is.</p></article>
      </section>
      <InterventionPlanForm planned={planned === true} />
      {planned === true && <article className="old-copy">
        <p>We are keeping more than one cause on the board. The old fracture, unchanged heat load, empty paired watch, and late-routed closure requests overlap. Iss&apos;s cut widened the failure. It did not create the old fracture.</p>
        <p>His reed, water, and air samples still support the surface claim. They do not make an unreviewed route safe. That distinction stays in the incident file.</p>
        <h2>Accepted order</h2>
        <ol><li>Install the authenticated lower filter.</li><li>Restore the paired coverage lamps.</li><li>Close the mapped pressure bypass.</li><li>Open the staff route after the three system readbacks agree.</li></ol>
        <p>Do not erase the altered office. Its clean chronology is evidence of what the copy preferred.</p>
      </article>}
      {unlitCompared === true && <section className="old-copy" aria-labelledby="base-comparison-heading">
        <h2 id="base-comparison-heading">Base comparison received</h2>
        <p>The current group returned from all seven copied houses. Copperline has kept the house findings as seven separate records instead of merging them into one neat account.</p>
        <p>The official changes, household repairs, present-day shapes, and earlier company marks do not always agree. Their differences now sit beside the works before-state. Nothing in this attachment replaces the group&apos;s causal finding or makes an unsafe repair acceptable.</p>
      </section>}
      {planned === true && repaired === true ? (
        <section className="old-copy" aria-labelledby="works-readback"><h2 id="works-readback">Works readback added by the current group</h2><ul><li>Lower draw: clear, original dirty sample retained.</li><li>Coverage: both lamps live.</li><li>Pressure: return gauge at zero; bypass shut.</li><li>Route: staff gate opened last.</li></ul><p>Dob&apos;s surface patch settled after the same pressure correction. The old Hold repair, his later patch, and the group&apos;s intervention now form one material history.</p></section>
      ) : planned === true ? <div className="old-message">No repair readback has been attached. The accepted plan is public; the works remain unchanged.</div> : null}
      <section className="old-copy" aria-labelledby="works-replies"><h2 id="works-replies">Replies</h2><div className="ticket-thread">
        <article className="customer"><header><b>rookline</b><time>April 2, 2011 7:11 PM</time></header><p>Keep the before state. If the repair changes the copied office too, we need both versions and the exact order.</p></article>
        <article className="customer"><header><b>wren-home</b><time>April 2, 2011 7:19 PM</time></header><p>I can watch the archive diff while you are in the works. Tell me which change should arrive first.</p></article>
        <article className="customer"><header><b>mkept</b><time>April 2, 2011 7:31 PM</time></header><p>No. Save the diff locally before anyone predicts it in the thread.</p></article>
      </div></section>
      <p className="old-copy"><Link href="/community/2011/03/14/nessa-correction">Earlier: correction to the cistern file</Link></p>
    </LegacyShell>
  );
}
