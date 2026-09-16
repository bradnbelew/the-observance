import type { Metadata } from 'next';
import Link from 'next/link';
import { Breadcrumbs, LegacyShell, OldPageTitle } from '@/components/legacy/LegacyShell';
import { MORROW_SEED, mossfieldForumPosts } from '@/lib/morrow-copperline-seed';

export const metadata: Metadata = { title: 'Community Forum - Copperline Hosting' };
export const dynamic = 'force-static';

export default async function ForumPage({ searchParams }: { searchParams: Promise<{ thread?: string; view?: string }> }) {
  const params = await searchParams;
  const threadOpen = params.thread === MORROW_SEED.forumThread;
  const printView = params.view === 'print';
  return <LegacyShell active="community">
    <Breadcrumbs><Link href="/community/index.php">Community</Link> &raquo; Forum Archive</Breadcrumbs>
    <OldPageTitle sub="Read-only import from the retired customer forum.">Community Forum Archive</OldPageTitle>
    {!threadOpen ? <div className="old-alert"><b>Archive index:</b> Most threads were not imported. Thread {MORROW_SEED.forumThread} survives through a support-ticket reference.</div> : null}
    <section className="old-copy">
      <h2>{threadOpen ? 'Thread 6118: Mossfield restore window' : 'Imported forum fragments'}</h2>
      <p className="old-fineprint">Mode: {printView ? 'print/raw text view' : 'screen view'} · quoted edits and missing avatars are preserved as text labels.</p>
      {threadOpen ? <table className="old-data-table"><tbody>{mossfieldForumPosts.map((post) => <tr key={post.id}><td><b>{post.author}</b><br /><small>{post.date}</small></td><td><b>{post.title}</b><p>{post.body}</p>{post.edited ? <small>{post.edited}</small> : null}</td></tr>)}</tbody></table> : <p>Use a retained thread number from an archived product-page comment or support attachment.</p>}
      {threadOpen ? <div className="old-alert"><b>Moderator import note:</b> display handle <code>i_bell</code> belongs to the staff signature indexed as <code>{MORROW_SEED.alias}</code>. See <Link href="/support/tickets/6118">ticket 6118</Link>.</div> : null}
      <p><Link href="/community/forum?thread=6118&view=print">Open print view</Link> · <Link href="/status/incident-6118">Incident status residue</Link></p>
    </section>
  </LegacyShell>;
}
