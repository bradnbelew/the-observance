import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';

export const metadata: Metadata = { title: 'Recovered packet custody - Copperline Archive', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function RecoveredPacketPage() {
  const available = await hasCampaignEvent('p10.wren_remembrance_committed');
  const identified = await hasCampaignEvent('p11.averyn_identified');
  const unbound = await hasCampaignEvent('p11.averyn_restored_unbound');
  if (available !== true) return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs><OldPageTitle>Packet Not Available</OldPageTitle><div className="old-message error">This custody object is not in the current archive index.</div></LegacyShell>;
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive &raquo; Recovered packet</Breadcrumbs>
      <OldPageTitle sub="One ZIP and five extracted members. Review the members together; do not treat the audio as an isolated ghost file.">recovered-archive-packet</OldPageTitle>
      <article className="old-copy" id="evidence-p11-packet" data-evidence-id="p11.e01"><p><b>ZIP:</b> 62,009 bytes · SHA-1 <code>783ecde5685abdb601e4a659fc947c32964f70b3</code> · SHA-256 <code>62bb4c1b7c66f0d5415301418e45ab49f710801ddcc2c72f4cfe15a73ec2317f</code></p><p><Link href="/record/archive">Open the earned Recovery Archive delivery</Link>. Its server-owned media view applies the packet prerequisite before exposing the retained external location. Copperline keeps this wrapper separate from its extracted-member index.</p></article>
      <section className="old-copy" aria-labelledby="packet-members"><h2 id="packet-members">Extracted members</h2><table><thead><tr><th>Member</th><th>Job in the packet</th><th>SHA-1</th></tr></thead><tbody>
        <tr><td><code>README.txt</code></td><td>says to review all five members as one custody object</td><td><code>1f27f3a9edc2e348fc12916c5d72509dc6107d50</code></td></tr>
        <tr><td><code>inventory_06.txt</code></td><td>orders the intake, lamp, and field capture without naming a solution</td><td><code>83a5c75fcfd4be2072e6df46449124d9a5ccde7f</code></td></tr>
        <tr id="evidence-p11-spectrogram" data-evidence-id="p11.e02"><td><code>field_audio_03.wav</code></td><td>contains a nonverbal visual signal; the indexed spectrogram reads <b>I WAS NOT KEPT</b>. Intake authorship and field vocabulary tie the signal to the erased registrar, but the phrase alone does not identify its speaker.</td><td><code>2003f0151c1ba643c649b5ed0e19d1b31bb68319</code></td></tr>
        <tr><td><code>intake_partial.png</code></td><td>connects the packet hand to the earlier cistern correction</td><td><code>1fc1e369ba5e4314496aa66ba9afc4bfc67ae592</code></td></tr>
        <tr><td><code>lamp_roll_scan.jpg</code></td><td>connects the voice field to the repaired paired-watch circuit</td><td><code>ca9a7a3d4958fc05c9a620e452bcc1ed95ae3287</code></td></tr>
      </tbody></table></section>
      <section className="old-copy" id="evidence-p11-edition-margin" data-evidence-id="p11.e04"><h2>Edition margin and original hand</h2><p>The packet&apos;s intake image shows the same short return mark cut into the Hold&apos;s original filter register. A later council scan preserves the measurements but replaces the signer with <i>office copy</i>. Under side light, the erased margin leaves six strokes: <b>A / V / E / R / Y / N</b>, each beside a different Keeper&apos;s returned affidavit reference.</p><p>The six strokes are routes back to one removed person. They are not a seventh Keeper sequence and do not authorize filling a seventh socket.</p></section>
      <section className="old-copy" id="evidence-p11-sella-transfer" data-evidence-id="p11.e05"><h2>Sella transfer overlay</h2><p>The intake image can be aligned to Sella&apos;s map by the torn lower corner, three-reed shelf, and split sill. Read from the damp reverse, her affidavit margin exposes <b>E</b>. A pupil&apos;s tin-fish drawing survives under the crop, keeping the record tied to the school material that first taught the transfer.</p></section>
      <section className="old-copy" id="evidence-p11-iss-history" data-evidence-id="p11.e08"><h2>Iss correction history</h2><p>The first recovered line reads <i>MERCY</i> and yields M. The later history restores the longer line: <i>NAME IS NOT MERCY</i>. At the same indexed position the correction yields N. The first decode is mechanically correct and narratively wrong; both copies remain available.</p></section>
      <section className="old-copy"><h2>Earlier relations</h2><p><Link href="/community/2011/03/14/nessa-correction">The intake correction</Link> establishes the analyst&apos;s human work. <Link href="/community/2011/04/02/hold-works">The Hold works record</Link> establishes that copied official history and lived material history can diverge.</p></section>
      {identified === true ? <section className="old-copy"><h2>Identity receipt</h2><p>The six affidavit paths yield <b>AVERYN</b>. Copperline files that as a person&apos;s name, not a seventh Keeper title and not a new slot in the civic system.</p></section> : <div className="old-message">No identity receipt is attached. The packet remains available for review and replay.</div>}
      {unbound === true ? <section className="old-copy"><h2>Unbound relationship record</h2><ul><li><b>Averyn:</b> human registrar and cistern analyst.</li><li><b>Record:</b> civic monitoring and memory system that trapped her.</li><li><b>Watcher:</b> defensive Record speech through her constrained consciousness.</li><li><b>Dark:</b> related pressure or cause, still distinct and unknown.</li></ul><p>The empty Record socket remains intentionally empty. Restoring her name does not file her back into it.</p></section> : null}
    </LegacyShell>
  );
}
