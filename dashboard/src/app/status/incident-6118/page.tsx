import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { MORROW_SEED } from '@/lib/morrow-copperline-seed';

export const metadata: Metadata = { title: 'Incident 6118 - Copperline Status' };
export const dynamic = 'force-static';

export default async function Incident6118Page({ searchParams }: { searchParams: Promise<{ raw?: string }> }) {
  const raw = (await searchParams).raw === '1';
  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/status">Network Status</Link> &raquo; Incident 6118</Breadcrumbs>
    <OldPageTitle sub="Retained status residue for Chicago game storage.">Incident 6118</OldPageTitle>
    <div className="old-alert"><b>Mirror warning:</b> public prose changed during import. The raw uptime field below is retained unchanged.</div>
    <table className="old-data-table"><tbody>
      <tr><th>Incident</th><td>{MORROW_SEED.incident}</td></tr>
      <tr><th>Public title</th><td><s>Routine storage repair</s> Game-node recovery maintenance</td></tr>
      <tr><th>Posted</th><td><s>May 18, 2011 02:10 CST</s> May 18, 2011 03:42 CST</td></tr>
      <tr><th>Raw uptime</th><td><code>{MORROW_SEED.uptime}</code></td></tr>
      <tr><th>Archive build</th><td>{MORROW_SEED.footerBuild}</td></tr>
    </tbody></table>
    {raw ? <pre className="old-pre">incident,public_time,uptime,case
6118,2011-05-18T03:42:00-06:00,{MORROW_SEED.uptime},mossfield</pre> : null}
    <p className="old-fineprint">Recovery desk convention for this mirror uses the first four uptime digits plus the employee stem from the staff signature. The retained key is eight characters.</p>
    <p><Link href="/status/incident-6118?raw=1">Raw export</Link> · <Link href="/support/tickets/6118">Support ticket</Link> · <Link href="/recovery/mossfield">Recovery lookup</Link></p>
  </LegacyShell>;
}
