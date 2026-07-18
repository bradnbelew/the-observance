import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { hasCampaignEvent } from '@/lib/arg-event-store';
import { SupplierRestoreForm } from './SupplierRestoreForm';

export const metadata: Metadata = { title: 'Cistern cloth attachment history - Copperline Community', robots: { index: false, follow: false } };
export const dynamic = 'force-dynamic';

export default async function SupplierRevisionsPage() {
  const [materialProven, restored] = await Promise.all([
    hasCampaignEvent('p7.counterfeit_material_proven'),
    hasCampaignEvent('p7.supplier_history_restored'),
  ]);
  if (materialProven !== true) return <LegacyShell active="community"><Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive</Breadcrumbs><OldPageTitle>Attachment History Unavailable</OldPageTitle><div className="old-message error">No retained material comparison is attached to this archive row.</div></LegacyShell>;
  return (
    <LegacyShell active="community">
      <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Archive &raquo; cistern-cloth-delivery</Breadcrumbs>
      <OldPageTitle sub="Two retained versions; original timestamps and hashes remain read-only.">cistern-cloth-delivery / attachment history</OldPageTitle>
      <section className="old-copy" id="evidence-p7-supplier-invoice" data-evidence-id="p7.e07"><h2>Collapsed account row</h2><p>The payment row names Merrit Textile and cistern-grade paired-warp cloth. Its trade terms use <i>paired warp</i>, <i>charcoal sized</i>, and <i>cistern return</i>. The later delivery attachment names North Cut Salvage, substitutes <i>single warp</i>, and copies Merrit&apos;s seal marks. Expiration collapsed the two files into one visible row.</p></section>
      <SupplierRestoreForm restored={restored === true} />
      {restored === true ? <section className="old-copy" id="evidence-p7-attachment-history" data-evidence-id="p7.e08"><h2>Restored comparison</h2><table className="detail-table"><tbody><tr><th>Draft A / 08:14</th><td>Merrit Textile; twelve paired-warp, charcoal-sized bolts; Vaun receiving 44.</td></tr><tr><th>Draft B / 19:52</th><td>North Cut Salvage; cheaper single-warp lot; seal image copied after receipt.</td></tr><tr><th>Seal image / 20:06</th><td>Merrit&apos;s mark pasted into Draft B after the receiving time; its compression block is identical to the old thumbnail.</td></tr><tr><th>Stores balance</th><td>Five genuine bolts moved to favored heat galleries during the same week.</td></tr></tbody></table><p>The invoice is genuine. The delivered cloth and the later attachment are not what that invoice describes.</p></section> : null}
    </LegacyShell>
  );
}
