import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { MORROW_SEED, recoveryLookup } from '@/lib/morrow-copperline-seed';

export const metadata: Metadata = { title: 'Mossfield Recovery - Copperline' };
export const dynamic = 'force-static';

export default async function MossfieldRecoveryPage({ searchParams }: { searchParams: Promise<{ alias?: string; key?: string }> }) {
  const params = await searchParams;
  const lookup = recoveryLookup(params.alias, params.key);
  const open = lookup.state === 'open';
  return <LegacyShell active="support">
    <Breadcrumbs><Link href="/support/index.php">Support</Link> &raquo; Recovery Archive</Breadcrumbs>
    <OldPageTitle sub="Legacy recovery archive. Static rehearsal fixture only.">Mossfield Recovery Lookup</OldPageTitle>
    <div className={open ? 'old-alert' : 'old-message error'}><b>{open ? 'Case recovered:' : 'Lookup response:'}</b> {lookup.message}</div>
    <form method="get" className="old-filter">
      <label>Employee alias<input name="alias" defaultValue={params.alias ?? ''} aria-label="Employee alias" /></label>
      <label>Recovery key<input name="key" defaultValue={params.key ?? ''} aria-label="Recovery key" /></label>
      <button type="submit">Lookup</button>
    </form>
    {open ? <section className="old-copy">
      <h2>Mossfield handoff</h2>
      <table className="old-data-table"><tbody>
        <tr><th>Case fingerprint</th><td><code>{MORROW_SEED.caseFingerprint}</code></td></tr>
        <tr><th>Server handoff</th><td><code>{MORROW_SEED.handoffAddress}</code></td></tr>
        <tr><th>Player role</th><td>non-op rehearsal participant</td></tr>
        <tr><th>Release</th><td>{MORROW_SEED.release}</td></tr>
        <tr><th>Production</th><td>disabled; morrow-reboot.enabled remains false</td></tr>
      </tbody></table>
      <p className="old-fineprint">This static handoff satisfies the local G01-G03 route grammar only. It does not start a Minecraft server, contact Supabase, send Discord messages, or enable production.</p>
      <p><Link href="/recovery/mossfield/test-guide">Open first-touch test guide</Link> · <Link href="/recovery/mossfield/console">Open local console</Link></p>
    </section> : <section className="old-copy"><h2>Lookup hints retained by the mirror</h2><p>Alias evidence lives in the forum thread and support signature. The key is derived from incident uptime in the status residue.</p><p><Link href="/community/forum?thread=6118">Forum thread</Link> · <Link href="/support/tickets/6118">Support ticket</Link> · <Link href="/status/incident-6118">Status residue</Link></p></section>}
  </LegacyShell>;
}
